/**
 * Copyright Tony Bringardner 1998, 2026 <a href="http://bringardner.com/tony">Tony Bringardner</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.bringardner.parley.files.viewer.fx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import us.bringardner.fx.menu.StandardMenus;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.browse.FileEntry;
import us.bringardner.parley.files.browse.NavigationHistory;
import us.bringardner.parley.files.fx.ConnectionDialog;
import us.bringardner.parley.files.fx.FileTable;
import us.bringardner.parley.files.fx.FolderTree;
import us.bringardner.parley.files.fx.PathBar;
import us.bringardner.parley.files.fx.RecentFileMenu;

/**
 * One viewer window: a folder tree beside the files of the current folder, with the actions of
 * the Swing viewer's core (open, edit, copy, paste, rename, delete, new folder), a menu of
 * recent files that reopens remote ones, and "Connect to..." for other file systems.
 * <p>
 * Everything that touches a file system runs off the JavaFX thread, so a slow server never
 * freezes the window.
 */
public class ViewerWindow {

	static final String PREF_SHOW_HIDDEN = "ShowHidden";
	static final String PREF_SHOW_EXTENSIONS = "ShowExtensions";
	static final String PREF_START = "StartLocation";

	/**
	 * The folder a kind of file system starts in, shared with the Swing viewer (its preferences and
	 * key). One saved by an earlier JavaFX viewer, in its own preferences, is moved there.
	 */
	static String startLocation(String typeId) {
		String key = us.bringardner.parley.files.viewer.FileSourceViewer.PREF_START_LOCATION + "_" + typeId;
		String ret = FxLook.prefs().get(key, null);
		String old = PREFS.get(PREF_START + "." + typeId, null);
		if( old != null ) {
			if( ret == null ) {
				FxLook.prefs().put(key, old);
				ret = old;
			}
			PREFS.remove(PREF_START + "." + typeId);
		}
		return ret;
	}

	/** Sets the folder a kind of file system starts in, for both viewers. */
	static void setStartLocation(String typeId, FileSource dir) {
		FxLook.prefs().put(us.bringardner.parley.files.viewer.FileSourceViewer.PREF_START_LOCATION + "_" + typeId,
				dir.getAbsolutePath());
	}

	private static final Preferences PREFS = Preferences.userNodeForPackage(ViewerWindow.class);
	private static final DataFormat DRAG_FORMAT = new DataFormat("application/x-parley-file-sources");

	// shared by every window, so a copy can be pasted into another window, even one on another server
	private static List<FileSource> clipboard = new ArrayList<>();
	private static List<String> clipboardLocalPaths = new ArrayList<>();
	private static List<FileSource> dragged;
	private static int openWindows;

	private final Stage stage;
	private final FileSourceFactory factory;
	private final NavigationHistory history = new NavigationHistory();
	private final FileTable table = new FileTable();
	private final FolderTree tree = new FolderTree();
	private final TreeFollower follower = new TreeFollower(tree);
	private final CopyManagerPane copyManager = new CopyManagerPane(CopyQueue.shared());
	private final CopyQueue.Listener refresher = new CopyQueue.Listener() {
		@Override public void changed() { }

		@Override public void idle(boolean foldersMade) {
			Platform.runLater(()->{
				refresh();
				if( foldersMade ) {
					refreshTree();
				}
			});
		}
	};
	private final PathBar pathBar = new PathBar();
	private final Button back = new Button("‹");
	private final Button forward = new Button("›");
	private final Button up = new Button("↑");
	private final Label status = new Label();
	private final MenuItem pasteItem = new MenuItem("Paste");
	private boolean navigating;
	private RecentFileMenu recent;

	public ViewerWindow(FileSourceFactory factory, FileSource start, Stage stage) {
		this.factory = factory;
		this.stage = stage == null ? new Stage() : stage;

		String type = factory.getTypeId();
		table.showHiddenProperty().set(PREFS.getBoolean(PREF_SHOW_HIDDEN+"."+type, false));
		table.showExtensionsProperty().set(PREFS.getBoolean(PREF_SHOW_EXTENSIONS+"."+type, true));
		tree.showHiddenProperty().bind(table.showHiddenProperty());
		table.showHiddenProperty().addListener((o, was, is)->PREFS.putBoolean(PREF_SHOW_HIDDEN+"."+type, is));
		table.showExtensionsProperty().addListener((o, was, is)->PREFS.putBoolean(PREF_SHOW_EXTENSIONS+"."+type, is));

		BorderPane root = new BorderPane();
		root.setTop(new VBox(buildMenuBar(), buildToolbar()));
		SplitPane split = new SplitPane(tree, table);
		split.setDividerPositions(0.25);
		SplitPane.setResizableWithParent(tree, false);
		root.setCenter(split);
		HBox statusBar = new HBox(status);
		statusBar.setPadding(new Insets(3, 8, 3, 8));
		root.setBottom(new VBox(copyManager, statusBar));

		wireNavigation();
		wireTable();

		this.stage.setScene(FxLook.apply(this.stage, new Scene(root, 1000, 640)));
		CopyQueue.shared().addListener(refresher);
		this.stage.setOnHidden(e->{
			CopyQueue.shared().removeListener(refresher);
			copyManager.dispose();
			openWindows--;
			if( openWindows <= 0 ) {
				Platform.exit();
			}
		});

		try {
			FileSource[] roots = factory.listRoots();
			tree.setTopFolders(roots == null ? new ArrayList<>() : Arrays.asList(roots));
		} catch (IOException e) {
			TransferWindow.showError(this.stage, "Can't list the roots of "+factory.getTitle(), e);
		}
		navigate(start != null ? start : startDirectory());
		updateTitle();
	}

	// ---------------------------------------------------------------- building

	/**
	 * The menus, laid out as in every Parley application (fx-widgets' StandardMenus) and as in the
	 * Swing viewer: File, Edit, View, Window and Help, with About, Settings... and Quit in the
	 * application menu on macOS and in File and Help elsewhere.
	 */
	private MenuBar buildMenuBar() {
		StandardMenus menus = new StandardMenus(FxLook.APP_NAME).about(()->showAbout(stage)).settings(()->showSettings(stage))
				.quit(ViewerWindow::okToQuit);
		Menu file = menus.file;
		MenuItem newWindow = item("New Window", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::newWindow);
		MenuItem connect = item("New Remote Session...", null, this::connectTo);
		MenuItem open = item("Open", new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), this::openSelected);
		MenuItem openWith = item("Open With...", null, this::openWithSelected);
		MenuItem editWith = item("Edit With...", null, this::editWithSelected);
		MenuItem print = item("Print", null, this::printSelected);
		MenuItem newFolder = item("New Folder...", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), this::newFolder);
		MenuItem setStart = item("Set Start Directory...", null, this::chooseStartLocation);
		MenuItem edit = item("Edit", new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN), this::editSelected);
		MenuItem terminal = item("New Terminal", new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN,
				KeyCombination.ALT_DOWN), ()->newTerminal(history.current()));
		MenuItem search = item("Find...", new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), this::search);
		MenuItem startHere = item("Start Here Next Time", null, this::setStartLocation);
		MenuItem close = item("Close", new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN), stage::close);
		// as in the Swing viewer
		file.getItems().addAll(newWindow, connect, terminal, newFolder, new SeparatorMenuItem(), open, openWith, edit, editWith);
		if( canPrint() ) {
			file.getItems().add(print);
		}
		try {
			recent = new RecentFileMenu(ViewerWindow.class);
			recent.setOnOpened(this::openRecent);
			file.getItems().add(recent);
		} catch (IOException e) {
			// the menu is a convenience: the viewer works without it
		}
		file.getItems().addAll(new SeparatorMenuItem(), startHere, setStart, new SeparatorMenuItem(), close);

		Menu editMenu = menus.edit;
		pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
		pasteItem.setOnAction(e->paste());
		editMenu.getItems().addAll(
				item("Copy", new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN), this::copySelected),
				pasteItem,
				new SeparatorMenuItem(),
				item("Rename...", new KeyCodeCombination(KeyCode.F2), this::renameSelected),
				item("Delete", new KeyCodeCombination(KeyCode.DELETE), this::deleteSelected),
				new SeparatorMenuItem(), search);

		Menu view = menus.view;
		CheckMenuItem hidden = new CheckMenuItem("Show Hidden Files");
		hidden.selectedProperty().bindBidirectional(table.showHiddenProperty());
		CheckMenuItem extensions = new CheckMenuItem("Show File Extensions");
		extensions.selectedProperty().bindBidirectional(table.showExtensionsProperty());
		view.getItems().addAll(hidden, extensions, new SeparatorMenuItem(),
				item("Refresh", new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), this::refresh));
		return menus.build(stage);
	}

	/** Help > About (the application menu on macOS), from any of the viewer's windows. */
	static void showAbout(Window owner) {
		Package p = ViewerWindow.class.getPackage();
		String version = p == null ? null : p.getImplementationVersion();
		Alert a = new Alert(AlertType.INFORMATION, "Browse, copy, search and edit files on any parley-files file system:"
				+ " local, memory, FTP, SFTP and JDBC.\n\nAn example of what Parley can help you build."
				+ "\nhttps://github.com/parley-project", ButtonType.OK);
		a.initOwner(owner);
		a.setTitle("About " + FxLook.APP_NAME);
		a.setHeaderText(FxLook.APP_NAME + (version == null ? "" : " " + version));
		List<javafx.scene.image.Image> icons = FxLook.appIcons();
		if( icons.size() > 3 ) {
			javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(icons.get(3));
			logo.setFitWidth(64);
			logo.setFitHeight(64);
			a.setGraphic(logo);
		}
		FxLook.apply(a);
		a.showAndWait();
	}

	/** Settings...: the theme and font, shared with the Swing viewer and the other Parley applications. */
	static void showSettings(Window owner) {
		us.bringardner.fx.dialog.SettingsDialog.showDialog(owner, FxLook.prefs());
	}

	/**
	 * Quit: asks first while files are being copied (stopping would leave them half copied), then
	 * closes every window.
	 */
	static boolean okToQuit() {
		if( CopyQueue.shared().isBusy() ) {
			Alert a = new Alert(AlertType.CONFIRMATION, "Files are still being copied. Quit and stop copying them?",
					ButtonType.CANCEL, ButtonType.OK);
			a.setHeaderText("Quit " + FxLook.APP_NAME + "?");
			FxLook.apply(a);
			if( a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK ) {
				return false;
			}
			CopyQueue.shared().cancelAll();
		}
		return true;
	}

	private static MenuItem item(String text, KeyCombination accelerator, Runnable action) {
		MenuItem ret = new MenuItem(text);
		if( accelerator != null ) {
			ret.setAccelerator(accelerator);
		}
		ret.setOnAction(e->action.run());
		return ret;
	}

	private Node buildToolbar() {
		back.setTooltip(new Tooltip("Back"));
		forward.setTooltip(new Tooltip("Forward"));
		up.setTooltip(new Tooltip("Enclosing folder"));
		back.setOnAction(e->show(history.back()));
		forward.setOnAction(e->show(history.forward()));
		up.setOnAction(e->goUp());
		HBox bar = new HBox(4, back, forward, up, pathBar);
		bar.setPadding(new Insets(4));
		bar.setStyle("-fx-alignment: center-left;");
		return bar;
	}

	private void wireNavigation() {
		pathBar.directoryProperty().addListener((o, was, is)->{
			if( !navigating && is != null ) {
				navigate(is);
			}
		});
		tree.getSelectionModel().selectedItemProperty().addListener((o, was, is)->{
			if( !navigating && !follower.isSelecting() && is != null && is.getValue() != null ) {
				navigate(is.getValue());
			}
		});
		table.setOnOpen(entry->{
			if( entry.isDirectory()) {
				navigate(entry.getFile());
			} else {
				open(entry.getFile());
			}
		});
	}

	private void wireTable() {
		ContextMenu menu = new ContextMenu(
				item("Open", null, this::openSelected),
				item("Open With...", null, this::openWithSelected),
				item("Edit", null, this::editSelected),
				item("Edit With...", null, this::editWithSelected));
		if( canPrint() ) {
			menu.getItems().add(item("Print", null, this::printSelected));
		}
		menu.getItems().addAll(
				item("New Terminal Here", null, this::terminalHere),
				item("Find...", null, this::search),
				new SeparatorMenuItem(),
				item("Copy", null, this::copySelected),
				item("Paste", null, this::paste),
				new SeparatorMenuItem(),
				item("New Folder...", null, this::newFolder),
				item("Rename...", null, this::renameSelected),
				item("Delete", null, this::deleteSelected),
				new SeparatorMenuItem(),
				item("Refresh", null, this::refresh));
		table.getTableView().setContextMenu(menu);
		table.getTableView().getSelectionModel().getSelectedItems().addListener(
				(javafx.collections.ListChangeListener<FileEntry>) c->updateStatus());
		table.loadingProperty().addListener((o, was, is)->updateStatus());

		// drag files out (to another window, or to Finder when they're local) and in
		table.getTableView().setOnDragDetected(e->{
			List<FileEntry> selected = table.getSelectedEntries();
			if( selected.isEmpty()) {
				return;
			}
			List<FileSource> files = new ArrayList<>();
			for(FileEntry f : selected) {
				files.add(f.getFile());
			}
			dragged = files;
			Dragboard db = table.getTableView().startDragAndDrop(TransferMode.COPY);
			ClipboardContent content = new ClipboardContent();
			content.put(DRAG_FORMAT, "parley");
			List<File> local = localFiles(files);
			if( !local.isEmpty()) {
				content.putFiles(local);
			}
			db.setContent(content);
			e.consume();
		});
		table.getTableView().setOnDragOver(e->{
			if( dragged != null || e.getDragboard().hasFiles()) {
				e.acceptTransferModes(TransferMode.COPY);
			}
			e.consume();
		});
		table.getTableView().setOnDragDropped(e->{
			List<FileSource> files = dragged;
			if( files == null && e.getDragboard().hasFiles()) {
				files = fromSystemFiles(e.getDragboard().getFiles());
			}
			FileSource target = dropTarget(e.getPickResult().getIntersectedNode());
			boolean ok = files != null && target != null;
			if( ok ) {
				copyInto(target, files);
			}
			dragged = null;
			e.setDropCompleted(ok);
			e.consume();
		});
		table.getTableView().setOnDragDone(e->dragged = null);
	}

	/** The folder under the pointer, else the folder shown. */
	private FileSource dropTarget(Node node) {
		for(Node n = node; n != null; n = n.getParent()) {
			if( n instanceof TableRow ) {
				Object item = ((TableRow<?>) n).getItem();
				if( item instanceof FileEntry && ((FileEntry) item).isDirectory()) {
					return ((FileEntry) item).getFile();
				}
				break;
			}
		}
		return history.current();
	}

	// ---------------------------------------------------------------- navigation

	/** The folder the user chose to start in, else the factory's current one, else its first root. */
	private FileSource startDirectory() {
		String saved = startLocation(factory.getTypeId());
		try {
			if( saved != null ) {
				FileSource dir = factory.createFileSource(saved);
				if( dir.isDirectory()) {
					return dir;
				}
			}
			FileSource cwd = factory.getCurrentDirectory();
			if( cwd != null ) {
				return cwd;
			}
			FileSource[] roots = factory.listRoots();
			return roots != null && roots.length > 0 ? roots[0] : null;
		} catch (IOException e) {
			return null;
		}
	}

	void navigate(FileSource dir) {
		if( dir == null ) {
			return;
		}
		history.go(dir);
		show(history.current());
	}

	private void show(FileSource dir) {
		if( dir == null ) {
			return;
		}
		navigating = true;
		try {
			table.setDirectory(dir);
			pathBar.setDirectory(dir);
		} finally {
			navigating = false;
		}
		follower.reveal(dir);
		back.setDisable(!history.canGoBack());
		forward.setDisable(!history.canGoForward());
		boolean hasParent = false;
		try {
			hasParent = dir.getParentFile() != null;
		} catch (IOException e) {
		}
		up.setDisable(!hasParent);
		updateTitle();
		updateStatus();
	}

	private void goUp() {
		FileSource from = history.current();
		try {
			FileSource parent = from == null ? null : from.getParentFile();
			if( parent != null ) {
				navigate(parent);
				table.select(from);
			}
		} catch (IOException e) {
			TransferWindow.showError(stage, "Can't go up", e);
		}
	}

	private void refresh() {
		table.refresh();
	}

	private void updateTitle() {
		FileSource dir = history.current();
		stage.setTitle(factory.getTitle()+(dir == null ? "" : " - "+dir.getAbsolutePath()));
	}

	private void updateStatus() {
		int shown = table.getShownEntries().size();
		int selected = table.getSelectedEntries().size();
		if( table.isLoading()) {
			status.setText("Loading...");
		} else {
			status.setText(shown+(shown == 1 ? " item" : " items")+(selected > 0 ? ", "+selected+" selected" : ""));
		}
	}

	public void show() {
		openWindows++;
		stage.show();
		if( history.current() != null && recent != null ) {
			try {
				recent.addRecent(history.current());
			} catch (IOException e) {
				// the list is a convenience
			}
		}
	}

	// ---------------------------------------------------------------- windows

	private void newWindow() {
		FileSourceFactory f = FileSourceFactory.getDefaultFactory();
		new ViewerWindow(f, null, null).show();
	}

	private void connectTo() {
		Optional<FileSourceFactory> connected = ConnectionDialog.connect(stage);
		connected.ifPresent(f->{
			try {
				new ViewerWindow(f, null, null).show();
			} catch (RuntimeException e) {
				TransferWindow.showError(stage, "Can't show "+f.getTitle(), e);
			}
		});
	}

	/** A recent file: a folder opens in a window of its own file system; a file is shown in its folder. */
	private void openRecent(Object opened) {
		if( !(opened instanceof FileSource) ) {
			return;
		}
		FileSource file = (FileSource) opened;
		try {
			FileSourceFactory f = file.getFileSourceFactory();
			if( file.isDirectory()) {
				new ViewerWindow(f, file, null).show();
			} else {
				ViewerWindow w = new ViewerWindow(f, file.getParentFile(), null);
				w.show();
				w.table.select(file);
			}
		} catch (IOException e) {
			TransferWindow.showError(stage, "Can't open "+file.getAbsolutePath(), e);
		}
	}

	/** Searches the folder shown, and below it, in a window of its own. */
	private void search() {
		FileSource dir = history.current();
		if( dir == null ) {
			return;
		}
		new SearchWindow(stage, dir, new SearchWindow.Host() {
			@Override
			public void open(FileSource file) {
				ViewerWindow.this.open(file);
			}

			@Override
			public void reveal(FileSource file) {
				try {
					if( file.isDirectory()) {
						navigate(file);
					} else {
						navigate(file.getParentFile());
						table.select(file);
					}
					stage.toFront();
				} catch (IOException e) {
					TransferWindow.showError(stage, "Can't show "+file.getAbsolutePath(), e);
				}
			}
		}).show();
	}

	private void setStartLocation() {
		FileSource dir = history.current();
		if( dir != null ) {
			setStartLocation(factory.getTypeId(), dir);
		}
	}

	// ---------------------------------------------------------------- opening and editing

	private List<FileSource> selectedFiles() {
		List<FileSource> ret = new ArrayList<>();
		for(FileEntry e : table.getSelectedEntries()) {
			ret.add(e.getFile());
		}
		return ret;
	}

	private void openSelected() {
		for(FileEntry e : table.getSelectedEntries()) {
			if( e.isDirectory()) {
				navigate(e.getFile());
				return;
			}
			open(e.getFile());
		}
	}

	/** Opens a file with the application of this machine that handles it; remote files are downloaded first. */
	void open(FileSource file) {
		TransferWindow.<File>run(stage, "Download "+file.getName(),
				progress->{
					progress.step(file.getName(), 0, 0);
					return FileOperations.localCopy(file);
				},
				local->{
					try {
						if( !FileOperations.isLocal(file)) {
							// a copy of a remote file: opening it must not change it
							local.setReadOnly();
						}
						ExternalApps.open(local);
					} catch (IOException e) {
						TransferWindow.showError(stage, "Can't open "+file.getName(), e);
					}
				},
				error->{
					if( error != null ) {
						TransferWindow.showError(stage, "Can't open "+file.getName(), error);
					}
				});
	}

	/** Edit With... on the selected file. */
	private void editWithSelected() {
		for(FileEntry e : table.getSelectedEntries()) {
			if( !e.isDirectory()) {
				chooseApplication(e.getFile(), us.bringardner.parley.files.viewer.IRegistry.CommandType.Editor, "Edit");
				return;
			}
		}
	}

	/** Print, where the system can: each file selected (a remote one is downloaded first). */
	private void printSelected() {
		for(FileEntry e : table.getSelectedEntries()) {
			if( !e.isDirectory()) {
				print(e.getFile());
			}
		}
	}

	/** True if the system can print files (the same test as the Swing viewer's). */
	static boolean canPrint() {
		return us.bringardner.parley.files.viewer.Printing.isAvailable();
	}

	/** Prints a file with the application the system has for it, as the Swing viewer does. */
	void print(FileSource file) {
		TransferWindow.<File>run(stage, "Download "+file.getName(),
				progress->{
					File local = FileOperations.localCopy(file);
					us.bringardner.parley.files.viewer.Printing.print(local);
					return local;
				},
				local->{},
				error->{
					if( error != null ) {
						TransferWindow.showError(stage, "Can't print "+file.getName(), error);
					}
				});
	}

	/** File > Set Start Directory...: a folder chosen, on this window's file system, to start in next time. */
	private void chooseStartLocation() {
		us.bringardner.parley.files.fx.FileSourceChooser chooser = new us.bringardner.parley.files.fx.FileSourceChooser();
		chooser.setTitle("Set Start Directory");
		chooser.setSelectionMode(us.bringardner.parley.files.browse.SelectionMode.DIRECTORIES);
		FileSource dir = history.current();
		if( dir != null ) {
			chooser.setInitialDirectory(dir);
		}
		FileSource chosen = chooser.showOpenDialog(stage);
		if( chosen != null ) {
			setStartLocation(factory.getTypeId(), chosen);
		}
	}

	/** Open With... on the selected file. */
	private void openWithSelected() {
		for(FileEntry e : table.getSelectedEntries()) {
			if( !e.isDirectory()) {
				openWith(e.getFile());
				return;
			}
		}
	}

	/**
	 * Open With...: the applications this computer has for the file, or another; a remote file is
	 * downloaded, and changes to the copy are offered to be sent back (as Edit does).
	 */
	void openWith(FileSource file) {
		chooseApplication(file, us.bringardner.parley.files.viewer.IRegistry.CommandType.Any, "Open");
	}

	/** Open With... and Edit With...: the applications of a kind for a file, one chosen, the file opened with it. */
	private void chooseApplication(FileSource file, us.bringardner.parley.files.viewer.IRegistry.CommandType type, String verb) {
		TransferWindow.<List<us.bringardner.parley.files.viewer.IRegistry.RegData>>run(stage, verb+" With",
				progress->OpenWithDialog.appsFor(file.getName(), type),
				apps->{
					OpenWithDialog.Choice choice = OpenWithDialog.choose(stage, file.getName(), apps, verb);
					if( choice != null ) {
						openWith(file, choice);
					}
				},
				error->{
					if( error != null ) {
						TransferWindow.showError(stage, "Can't open "+file.getName(), error);
					}
				});
	}

	private void openWith(FileSource file, OpenWithDialog.Choice choice) {
		TransferWindow.<File>run(stage, "Download "+file.getName(),
				progress->FileOperations.localCopy(file),
				local->{
					try {
						Process p = new ProcessBuilder(choice.command(local.getAbsolutePath())).redirectErrorStream(true).start();
						if( !FileOperations.isLocal(file)) {
							monitor(file, local, p);
						}
					} catch (IOException e) {
						TransferWindow.showError(stage, "Can't open "+file.getName()+" with "+choice, e);
					}
				},
				error->{
					if( error != null ) {
						TransferWindow.showError(stage, "Can't open "+file.getName(), error);
					}
				});
	}

	/** New Terminal Here: a terminal in the selected folder, or the one shown. */
	private void terminalHere() {
		for(FileEntry e : table.getSelectedEntries()) {
			if( e.isDirectory()) {
				newTerminal(e.getFile());
				return;
			}
		}
		newTerminal(history.current());
	}

	/** File > New Terminal: an fsh shell in a folder (of any file system), in its own window. */
	void newTerminal(FileSource dir) {
		try {
			new TerminalWindow(dir).show();
		} catch (IOException | RuntimeException e) {
			TransferWindow.showError(stage, "Can't open a terminal", e);
		}
	}

	private void editSelected() {
		for(FileEntry e : table.getSelectedEntries()) {
			if( !e.isDirectory()) {
				edit(e.getFile());
			}
		}
	}

	/**
	 * Edits a file in a text editor. A remote file is edited as a copy; when the copy changes
	 * the user is asked whether to send it back to the original.
	 */
	void edit(FileSource file) {
		TransferWindow.<File>run(stage, "Download "+file.getName(),
				progress->FileOperations.localCopy(file),
				local->{
					try {
						Process p = ExternalApps.edit(local);
						if( !FileOperations.isLocal(file)) {
							monitor(file, local, p);
						}
					} catch (IOException e) {
						TransferWindow.showError(stage, "Can't edit "+file.getName(), e);
					}
				},
				error->{
					if( error != null ) {
						TransferWindow.showError(stage, "Can't edit "+file.getName(), error);
					}
				});
	}

	/** Watches the copy while its editor runs, and offers to upload each change. */
	private void monitor(FileSource remote, File local, Process process) {
		Thread t = new Thread(()->{
			long known = local.lastModified();
			// editors that return at once (xdg-open) are watched for a while after
			long giveUp = System.currentTimeMillis() + 2L*60*60*1000;
			boolean ended = false;
			while( !ended && System.currentTimeMillis() < giveUp ) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					return;
				}
				ended = !process.isAlive() && !returnsAtOnce(process);
				long now = local.lastModified();
				if( now != known ) {
					known = now;
					uploadIfWanted(remote, local);
				}
			}
		}, "Edit "+remote.getName());
		t.setDaemon(true);
		t.start();
	}

	// xdg-open returns as soon as it has started an editor, so its end says nothing
	private static boolean returnsAtOnce(Process p) {
		return p.info().command().map(c->new File(c).getName().equals("xdg-open")).orElse(false);
	}

	private void uploadIfWanted(FileSource remote, File local) {
		java.util.concurrent.CompletableFuture<Boolean> answer = new java.util.concurrent.CompletableFuture<>();
		Platform.runLater(()->{
			Alert a = new Alert(AlertType.CONFIRMATION,
					"The copy of "+remote.getName()+" has changed.\nReplace the original file?",
					ButtonType.YES, ButtonType.NO);
			a.initOwner(stage);
			FxLook.apply(a);
			answer.complete(a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES);
		});
		try {
			if( answer.get() ) {
				FileSource src = FileSourceFactory.getDefaultFactory().createFileSource(local.getAbsolutePath());
				us.bringardner.parley.files.FileSourceCopy.copy(src, remote);
				Platform.runLater(this::refresh);
			}
		} catch (Exception e) {
			Platform.runLater(()->TransferWindow.showError(stage, "Can't replace "+remote.getName(), e));
		}
	}

	// ---------------------------------------------------------------- copy and paste

	/**
	 * The text field with the focus, if one has it: Copy, Paste and Delete act on its text then,
	 * not on files (on macOS the menu bar sees the keys first).
	 */
	private javafx.scene.control.TextInputControl focusedText() {
		Node f = stage.getScene() == null ? null : stage.getScene().getFocusOwner();
		return f instanceof javafx.scene.control.TextInputControl ? (javafx.scene.control.TextInputControl) f : null;
	}

	private void copySelected() {
		if( focusedText() != null ) {
			focusedText().copy();
			return;
		}
		List<FileSource> files = selectedFiles();
		if( files.isEmpty()) {
			return;
		}
		clipboard = files;
		List<File> local = localFiles(files);
		clipboardLocalPaths = new ArrayList<>();
		for(File f : local) {
			clipboardLocalPaths.add(f.getAbsolutePath());
		}
		if( !local.isEmpty()) {
			ClipboardContent content = new ClipboardContent();
			content.putFiles(local);
			Clipboard.getSystemClipboard().setContent(content);
		}
	}

	private void paste() {
		if( focusedText() != null ) {
			focusedText().paste();
			return;
		}
		FileSource dir = history.current();
		if( dir == null ) {
			return;
		}
		List<FileSource> files = clipboard;
		Clipboard system = Clipboard.getSystemClipboard();
		if( system.hasFiles()) {
			List<String> paths = new ArrayList<>();
			for(File f : system.getFiles()) {
				paths.add(f.getAbsolutePath());
			}
			// files copied in another application since the viewer's own copy
			if( !paths.equals(clipboardLocalPaths)) {
				files = fromSystemFiles(system.getFiles());
			}
		}
		if( files == null || files.isEmpty()) {
			return;
		}
		copyInto(dir, files);
	}

	/** Queues a copy of files into dir: it goes on in the background, shown in the copy manager. */
	private void copyInto(FileSource dir, List<FileSource> files) {
		CopyQueue.shared().copy(files, dir, this::askReplace, null,
				error->Platform.runLater(()->TransferWindow.showError(stage, "Copy failed", error)));
	}

	/** Asks, off the JavaFX thread, what to do about a file that's already there. */
	private CopyPlanner.Answer askReplace(FileSource existing) {
		java.util.concurrent.CompletableFuture<CopyPlanner.Answer> answer = new java.util.concurrent.CompletableFuture<>();
		Platform.runLater(()->{
			ButtonType replace = new ButtonType("Replace");
			ButtonType replaceAll = new ButtonType("Replace All");
			ButtonType skip = new ButtonType("Skip");
			ButtonType skipAll = new ButtonType("Skip All");
			Alert a = new Alert(AlertType.CONFIRMATION, existing.getAbsolutePath()+" already exists.",
					replace, replaceAll, skip, skipAll, ButtonType.CANCEL);
			a.setHeaderText("Replace it?");
			a.initOwner(stage);
			FxLook.apply(a);
			ButtonType r = a.showAndWait().orElse(ButtonType.CANCEL);
			answer.complete(r == replace ? CopyPlanner.Answer.REPLACE
					: r == replaceAll ? CopyPlanner.Answer.REPLACE_ALL
					: r == skip ? CopyPlanner.Answer.SKIP
					: r == skipAll ? CopyPlanner.Answer.SKIP_ALL
					: CopyPlanner.Answer.CANCEL);
		});
		try {
			return answer.get();
		} catch (Exception e) {
			return CopyPlanner.Answer.CANCEL;
		}
	}

	/** Lists the tree again from its top, and finds the folder shown in it. */
	private void refreshTree() {
		try {
			FileSource[] roots = factory.listRoots();
			tree.setTopFolders(roots == null ? new ArrayList<>() : Arrays.asList(roots));
			follower.reveal(history.current());
		} catch (IOException e) {
			// the tree keeps what it had
		}
	}

	private static List<File> localFiles(List<FileSource> files) {
		List<File> ret = new ArrayList<>();
		for(FileSource f : files) {
			if( f instanceof us.bringardner.parley.files.fileproxy.FileProxy ) {
				ret.add(((us.bringardner.parley.files.fileproxy.FileProxy) f).getTarget());
			}
		}
		return ret;
	}

	private List<FileSource> fromSystemFiles(List<File> files) {
		List<FileSource> ret = new ArrayList<>();
		try {
			for(File f : files) {
				ret.add(FileSourceFactory.getDefaultFactory().createFileSource(f.getAbsolutePath()));
			}
		} catch (IOException e) {
			TransferWindow.showError(stage, "Can't read the copied files", e);
		}
		return ret;
	}

	// ---------------------------------------------------------------- change

	private void newFolder() {
		FileSource dir = history.current();
		if( dir == null ) {
			return;
		}
		TextInputDialog d = new TextInputDialog("New Folder");
		d.initOwner(stage);
		FxLook.apply(d);
		d.setTitle("New Folder");
		d.setHeaderText("Name of the new folder in "+dir.getAbsolutePath());
		d.showAndWait().ifPresent(name->TransferWindow.<FileSource>run(stage, "New Folder",
				p->FileOperations.newFolder(dir, name),
				created->{
					refresh();
					refreshTree();
					table.select(created);
				},
				error->{
					if( error != null ) {
						TransferWindow.showError(stage, "Can't create "+name, error);
					}
				}));
	}

	private void renameSelected() {
		List<FileSource> files = selectedFiles();
		if( files.size() != 1 ) {
			return;
		}
		FileSource file = files.get(0);
		TextInputDialog d = new TextInputDialog(file.getName());
		d.initOwner(stage);
		FxLook.apply(d);
		d.setTitle("Rename");
		d.setHeaderText("New name for "+file.getName());
		d.showAndWait().ifPresent(name->{
			if( name.trim().equals(file.getName())) {
				return;
			}
			TransferWindow.<FileSource>run(stage, "Rename",
					p->FileOperations.rename(file, name),
					renamed->{
						refresh();
						table.select(renamed);
					},
					error->{
						if( error != null ) {
							TransferWindow.showError(stage, "Can't rename "+file.getName(), error);
						}
					});
		});
	}

	private void deleteSelected() {
		if( focusedText() != null ) {
			focusedText().deleteNextChar();
			return;
		}
		List<FileSource> files = selectedFiles();
		if( files.isEmpty()) {
			return;
		}
		String what = files.size() == 1
				? (isDirectory(files.get(0)) ? "the folder \"" : "\"")+files.get(0).getName()+"\""+(isDirectory(files.get(0)) ? " and everything in it" : "")
				: files.size()+" items (and everything in any folder)";
		Alert a = new Alert(AlertType.CONFIRMATION, "Delete "+what+"?", ButtonType.YES, ButtonType.NO);
		a.initOwner(stage);
		FxLook.apply(a);
		a.setHeaderText("Delete");
		if( a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES ) {
			return;
		}
		TransferWindow.<Integer>run(stage, "Delete",
				progress->{
					for(FileSource f : files) {
						FileOperations.deleteAll(f, progress);
					}
					return files.size();
				},
				n->refresh(),
				error->{
					refresh();
					if( error != null ) {
						TransferWindow.showError(stage, "Delete failed", error);
					}
				});
	}

	private static boolean isDirectory(FileSource f) {
		try {
			return f.isDirectory();
		} catch (IOException e) {
			return false;
		}
	}

	Stage getStage() {
		return stage;
	}

	FileTable getTable() {
		return table;
	}
}
