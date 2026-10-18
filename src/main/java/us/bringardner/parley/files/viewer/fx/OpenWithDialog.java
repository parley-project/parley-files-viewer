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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import us.bringardner.parley.files.viewer.IRegistry;
import us.bringardner.parley.files.viewer.IRegistry.RegData;

/**
 * Open With...: the applications this computer has for a file (the Swing viewer's registry: Launch
 * Services on macOS, the registry on Windows), each with its icon, or any other application chosen
 * from the disk.
 */
class OpenWithDialog {

	/** What was chosen: an application of the list, or another program. */
	static final class Choice {
		final RegData app;
		final File program;

		Choice(RegData app, File program) {
			this.app = app;
			this.program = program;
		}

		/** The command that opens a file with it. */
		List<String> command(String file) {
			if( app != null ) {
				return app.getCommandLine(file);
			}
			List<String> ret = new ArrayList<>();
			if( isMac() && program.getName().endsWith(".app") ) {
				// an application bundle; -W: the command lasts as long as the application does
				ret.add("open");
				ret.add("-W");
				ret.add("-a");
			}
			ret.add(program.getAbsolutePath());
			ret.add(file);
			return ret;
		}

		@Override
		public String toString() {
			return app != null ? app.name : program.getName();
		}
	}

	private final Stage stage = new Stage();
	private final ListView<RegData> list = new ListView<>();
	private final Map<String, Image> icons = new ConcurrentHashMap<>();
	private Choice chosen;

	OpenWithDialog(Window owner, String fileName, List<RegData> apps) {
		this(owner, fileName, apps, "Open");
	}

	/**
	 * @param verb "Open" (Open With...) or "Edit" (Edit With...)
	 */
	OpenWithDialog(Window owner, String fileName, List<RegData> apps, String verb) {
		stage.setTitle(verb + " With");
		stage.initModality(Modality.WINDOW_MODAL);
		if( owner != null ) {
			stage.initOwner(owner);
		}
		list.getItems().setAll(apps);
		list.setCellFactory(l->new ListCell<>() {
			private final ImageView icon = new ImageView();

			@Override
			protected void updateItem(RegData item, boolean empty) {
				super.updateItem(item, empty);
				if( empty || item == null ) {
					setText(null);
					setGraphic(null);
				} else {
					setText(item.name);
					icon.setImage(icons.get(item.path));
					icon.setFitWidth(24);
					icon.setFitHeight(24);
					setGraphic(icon);
				}
			}
		});
		list.setPlaceholder(new Label("No applications are registered for this kind of file.\nChoose one with Other Application..."));
		list.setPrefSize(380, 300);
		list.setOnMouseClicked(e->{
			if( e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && list.getSelectionModel().getSelectedItem() != null ) {
				ok();
			}
		});
		if( !apps.isEmpty() ) {
			list.getSelectionModel().select(0);
		}

		Label heading = new Label(verb + " \"" + fileName + "\" with:");
		Button other = new Button("Other Application...");
		other.setOnAction(e->chooseOther());
		Button cancel = new Button("Cancel");
		cancel.setCancelButton(true);
		cancel.setOnAction(e->cancel());
		Button open = new Button(verb);
		open.setDefaultButton(true);
		open.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
		open.setOnAction(e->ok());
		Region spring = new Region();
		HBox.setHgrow(spring, Priority.ALWAYS);
		HBox buttons = new HBox(8, other, spring, cancel, open);
		buttons.setAlignment(Pos.CENTER_LEFT);

		BorderPane content = new BorderPane(list);
		content.setTop(heading);
		BorderPane.setMargin(heading, new Insets(0, 0, 8, 0));
		content.setBottom(buttons);
		BorderPane.setMargin(buttons, new Insets(10, 0, 0, 0));
		content.setPadding(new Insets(14, 16, 12, 16));
		stage.setScene(FxLook.apply(stage, new Scene(new VBox(content))));
		loadIcons(apps);
	}

	/**
	 * Shows the dialog and waits.
	 *
	 * @return what was chosen, or null if canceled
	 */
	static Choice choose(Window owner, String fileName, List<RegData> apps) {
		return choose(owner, fileName, apps, "Open");
	}

	/** As above, for "Open" (Open With...) or "Edit" (Edit With...). */
	static Choice choose(Window owner, String fileName, List<RegData> apps, String verb) {
		OpenWithDialog d = new OpenWithDialog(owner, fileName, apps, verb);
		d.stage.showAndWait();
		return d.chosen;
	}

	/** The applications registered for a file (by its name), or none where there's no registry. */
	static List<RegData> appsFor(String fileName) {
		return appsFor(fileName, IRegistry.CommandType.Any);
	}

	/**
	 * The applications of a kind registered for a file: editors for Edit With... (or, with none
	 * registered as such, any that open it).
	 */
	static List<RegData> appsFor(String fileName, IRegistry.CommandType type) {
		try {
			List<RegData> ret = IRegistry.getRegistry().getRegisteredHandler(fileName, type);
			if( (ret == null || ret.isEmpty()) && type != IRegistry.CommandType.Any ) {
				ret = IRegistry.getRegistry().getRegisteredHandler(fileName, IRegistry.CommandType.Any);
			}
			return ret == null ? new ArrayList<>() : new ArrayList<>(ret);
		} catch (RuntimeException | LinkageError e) {
			return new ArrayList<>();
		}
	}

	void ok() {
		RegData app = list.getSelectionModel().getSelectedItem();
		if( app != null ) {
			chosen = new Choice(app, null);
			stage.close();
		}
	}

	void cancel() {
		chosen = null;
		stage.close();
	}

	/** Other Application...: a program (on macOS, an application) chosen from the disk. */
	private void chooseOther() {
		FileChooser fc = new FileChooser();
		fc.setTitle("Choose an Application");
		File apps = new File(isMac() ? "/Applications" : System.getProperty("user.home"));
		if( apps.isDirectory() ) {
			fc.setInitialDirectory(apps);
		}
		File f = fc.showOpenDialog(stage);
		if( f != null ) {
			chosen = new Choice(null, f);
			stage.close();
		}
	}

	/** The applications' icons, loaded in the background (Launch Services can be slow). */
	private void loadIcons(List<RegData> apps) {
		Thread t = new Thread(()->{
			for(RegData app : apps) {
				try {
					javax.swing.Icon icon = app.getIcon(32, 32);
					Image image = icon == null ? null : image(icon);
					if( image != null ) {
						icons.put(app.path, image);
						Platform.runLater(list::refresh);
					}
				} catch (Exception | LinkageError e) {
					// no icon: the name is enough
				}
			}
		}, "Open With icons");
		t.setDaemon(true);
		t.start();
	}

	/** A Swing icon as a JavaFX image. */
	static Image image(javax.swing.Icon icon) {
		int w = icon.getIconWidth();
		int h = icon.getIconHeight();
		if( w <= 0 || h <= 0 ) {
			return null;
		}
		BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = b.createGraphics();
		icon.paintIcon(null, g, 0, 0);
		g.dispose();
		WritableImage ret = new WritableImage(w, h);
		for(int y = 0; y < h; y++) {
			for(int x = 0; x < w; x++) {
				ret.getPixelWriter().setArgb(x, y, b.getRGB(x, y));
			}
		}
		return ret;
	}

	private static boolean isMac() {
		return System.getProperty("os.name", "").toLowerCase().startsWith("mac");
	}

	Stage getStage() {
		return stage;
	}

	ListView<RegData> getList() {
		return list;
	}

	Choice getChosen() {
		return chosen;
	}
}
