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

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.fx.FileIcons;

/**
 * Search a folder and everything below it by name (with * and ?), and optionally by size, date
 * modified and type. Results appear as they are found; the search can be canceled.
 * Double-clicking a result opens it; "Show in Folder" goes to its folder in the viewer.
 */
public class SearchWindow {

	/** What the window asks of the viewer it was opened from. */
	interface Host {
		/** Opens a file with its application. */
		void open(FileSource file);

		/** Shows the file's folder in the viewer and selects the file (a folder is shown itself). */
		void reveal(FileSource file);
	}

	private final Stage stage = new Stage();
	private final FileSource dir;
	private final Host host;
	private final TextField name = new TextField();
	private final CheckBox caseSensitive = new CheckBox("Case sensitive");
	private final CheckBox bySize = new CheckBox("Size");
	private final ComboBox<String> sizeOp = new ComboBox<>();
	private final TextField size1 = new TextField();
	private final TextField size2 = new TextField();
	private final ComboBox<String> sizeUnit = new ComboBox<>();
	private final CheckBox byDate = new CheckBox("Modified");
	private final ComboBox<String> dateOp = new ComboBox<>();
	private final DatePicker date1 = new DatePicker(LocalDate.now());
	private final DatePicker date2 = new DatePicker(LocalDate.now());
	private final CheckBox byType = new CheckBox("Type");
	private final ComboBox<String> type = new ComboBox<>();
	private final Button go = new Button("Search");
	private final Label status = new Label("Enter what to look for.");
	private final ObservableList<FileSource> results = FXCollections.observableArrayList();
	private final TableView<FileSource> table = new TableView<>(results);
	private AtomicBoolean cancel;

	SearchWindow(Window owner, FileSource dir, Host host) {
		this.dir = dir;
		this.host = host;
		if( owner != null ) {
			stage.initOwner(owner);
		}
		stage.setTitle("Search "+dir.getAbsolutePath());

		name.setPromptText("Name, like *.txt or report?.doc");
		name.setPrefColumnCount(30);
		name.setOnAction(e->startOrCancel());
		go.setDefaultButton(true);
		go.setOnAction(e->startOrCancel());
		HBox nameRow = new HBox(8, new Label("Name:"), name, caseSensitive, go);
		nameRow.setStyle("-fx-alignment: center-left;");

		sizeOp.getItems().addAll("at least", "at most", "exactly", "between");
		sizeOp.getSelectionModel().select(0);
		sizeUnit.getItems().addAll("bytes", "KB", "MB", "GB");
		sizeUnit.getSelectionModel().select(1);
		size1.setPrefColumnCount(7);
		size2.setPrefColumnCount(7);
		Label and = new Label("and");
		and.visibleProperty().bind(sizeOp.valueProperty().isEqualTo("between"));
		size2.visibleProperty().bind(sizeOp.valueProperty().isEqualTo("between"));
		HBox sizeRow = new HBox(8, bySize, sizeOp, size1, and, size2, sizeUnit);
		sizeRow.setStyle("-fx-alignment: center-left;");

		dateOp.getItems().addAll("on", "before", "after", "between");
		dateOp.getSelectionModel().select(0);
		Label and2 = new Label("and");
		and2.visibleProperty().bind(dateOp.valueProperty().isEqualTo("between"));
		date2.visibleProperty().bind(dateOp.valueProperty().isEqualTo("between"));
		HBox dateRow = new HBox(8, byDate, dateOp, date1, and2, date2);
		dateRow.setStyle("-fx-alignment: center-left;");

		type.getItems().addAll("Files", "Folders");
		type.getSelectionModel().select(0);
		HBox typeRow = new HBox(8, byType, type);
		typeRow.setStyle("-fx-alignment: center-left;");

		VBox more = new VBox(8, sizeRow, dateRow, typeRow);
		more.setPadding(new Insets(6));
		TitledPane advanced = new TitledPane("More choices", more);
		advanced.setExpanded(false);

		TableColumn<FileSource,FileSource> path = new TableColumn<>("Path");
		path.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue()));
		path.setCellFactory(c->text(f->f.getAbsolutePath(), true));
		path.setPrefWidth(520);
		TableColumn<FileSource,FileSource> date = new TableColumn<>("Date Modified");
		date.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue()));
		date.setCellFactory(c->text(f->{
			try {
				return FileIcons.date(f.lastModified());
			} catch (IOException e) {
				return "";
			}
		}, false));
		date.setPrefWidth(160);
		TableColumn<FileSource,FileSource> kind = new TableColumn<>("Type");
		kind.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue()));
		kind.setCellFactory(c->text(f->isDirectory(f) ? "Folder" : "File", false));
		kind.setPrefWidth(70);
		TableColumn<FileSource,FileSource> size = new TableColumn<>("Size");
		size.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue()));
		size.setCellFactory(c->text(f->{
			try {
				return isDirectory(f) ? "--" : FileIcons.size(f.length());
			} catch (IOException e) {
				return "";
			}
		}, false));
		size.setPrefWidth(90);
		size.setStyle("-fx-alignment: CENTER-RIGHT;");
		table.getColumns().add(path);
		table.getColumns().add(date);
		table.getColumns().add(kind);
		table.getColumns().add(size);
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.setPlaceholder(new Label("No results"));
		table.setRowFactory(t->{
			javafx.scene.control.TableRow<FileSource> row = new javafx.scene.control.TableRow<>();
			row.setOnMouseClicked(m->{
				if( m.getButton() == MouseButton.PRIMARY && m.getClickCount() == 2 && !row.isEmpty()) {
					open(row.getItem());
				}
			});
			return row;
		});
		MenuItem openItem = new MenuItem("Open");
		openItem.setOnAction(e->selected().forEach(this::open));
		MenuItem revealItem = new MenuItem("Show in Folder");
		revealItem.setOnAction(e->selected().forEach(this.host::reveal));
		table.setContextMenu(new ContextMenu(openItem, revealItem));

		VBox top = new VBox(8, nameRow, advanced);
		top.setPadding(new Insets(8));
		HBox bottom = new HBox(status);
		bottom.setPadding(new Insets(4, 8, 4, 8));
		BorderPane root = new BorderPane(table);
		root.setTop(top);
		root.setBottom(bottom);
		stage.setScene(new Scene(root, 860, 520));
		stage.setOnHidden(e->{
			if( cancel != null ) {
				cancel.set(true);
			}
		});
	}

	private void open(FileSource f) {
		if( isDirectory(f)) {
			host.reveal(f);
		} else {
			host.open(f);
		}
	}

	private List<FileSource> selected() {
		return new ArrayList<>(table.getSelectionModel().getSelectedItems());
	}

	private static boolean isDirectory(FileSource f) {
		try {
			return f.isDirectory();
		} catch (IOException e) {
			return false;
		}
	}

	private static TableCell<FileSource,FileSource> text(java.util.function.Function<FileSource,String> text, boolean folderIcon) {
		return new TableCell<>() {
			@Override
			protected void updateItem(FileSource f, boolean empty) {
				super.updateItem(f, empty);
				if( empty || f == null ) {
					setText(null);
					setGraphic(null);
				} else {
					setText(text.apply(f));
					setGraphic(folderIcon ? FileIcons.icon(isDirectory(f)) : null);
				}
			}
		};
	}

	public void show() {
		stage.show();
		name.requestFocus();
	}

	Stage getStage() {
		return stage;
	}

	// ---------------------------------------------------------------- searching

	private void startOrCancel() {
		if( cancel != null ) {
			cancel.set(true);
			return;
		}
		SearchCriteria criteria;
		try {
			criteria = criteria();
		} catch (IllegalArgumentException e) {
			status.setText(e.getMessage());
			return;
		}
		if( criteria.isEmpty()) {
			status.setText("Enter a name, or choose a size, date or type.");
			return;
		}
		results.clear();
		AtomicBoolean stop = new AtomicBoolean();
		cancel = stop;
		go.setText("Cancel");
		status.setText("Searching...");

		List<FileSource> batch = new ArrayList<>();
		boolean[] flushing = { false };
		Consumer<FileSource> found = f->{
			synchronized (batch) {
				batch.add(f);
				if( !flushing[0] ) {
					flushing[0] = true;
					Platform.runLater(()->{
						List<FileSource> now;
						synchronized (batch) {
							now = new ArrayList<>(batch);
							batch.clear();
							flushing[0] = false;
						}
						results.addAll(now);
						status.setText("Searching... "+results.size()+" found");
					});
				}
			}
		};
		FileSearch search = new FileSearch(criteria, stop::get, found);
		TransferWindow.EXECUTOR.execute(()->{
			search.run(dir);
			Platform.runLater(()->{
				// results posted before this are already added: runLater runs in order
				cancel = null;
				go.setText("Search");
				String text = results.size()+(results.size() == 1 ? " item" : " items")+" found";
				if( stop.get()) {
					text = "Canceled. "+text;
				}
				if( search.getSkipped() > 0 ) {
					text += " ("+search.getSkipped()+" couldn't be read)";
				}
				status.setText(text);
			});
		});
	}

	/** What the fields ask for; throws IllegalArgumentException with a message to show. */
	SearchCriteria criteria() {
		SearchCriteria c = new SearchCriteria().name(name.getText(), caseSensitive.isSelected());
		if( bySize.isSelected()) {
			long unit = 1;
			switch( sizeUnit.getValue()) {
			case "KB": unit = 1024L; break;
			case "MB": unit = 1024L*1024; break;
			case "GB": unit = 1024L*1024*1024; break;
			default: break;
			}
			long a = number(size1, "the size") * unit;
			SearchCriteria.Operator op;
			long b = 0;
			switch( sizeOp.getValue()) {
			case "at most": op = SearchCriteria.Operator.AT_MOST; break;
			case "exactly": op = SearchCriteria.Operator.EXACTLY; break;
			case "between":
				op = SearchCriteria.Operator.BETWEEN;
				b = number(size2, "the second size") * unit;
				break;
			default: op = SearchCriteria.Operator.AT_LEAST; break;
			}
			c.size(op, a, b);
		}
		if( byDate.isSelected()) {
			LocalDate d1 = date1.getValue();
			if( d1 == null ) {
				throw new IllegalArgumentException("Choose the date.");
			}
			long start = startOfDay(d1);
			long next = startOfDay(d1.plusDays(1));
			switch( dateOp.getValue()) {
			case "before":
				c.modified(null, start);
				break;
			case "after":
				c.modified(next, null);
				break;
			case "between":
				LocalDate d2 = date2.getValue();
				if( d2 == null ) {
					throw new IllegalArgumentException("Choose the second date.");
				}
				c.modified(Math.min(start, startOfDay(d2)), Math.max(next, startOfDay(d2.plusDays(1))));
				break;
			default:
				c.modified(start, next);
				break;
			}
		}
		if( byType.isSelected()) {
			c.type("Folders".equals(type.getValue()) ? SearchCriteria.Type.DIRECTORY : SearchCriteria.Type.FILE);
		}
		return c;
	}

	private static long startOfDay(LocalDate d) {
		return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	private static long number(TextField field, String what) {
		try {
			long v = Long.parseLong(field.getText().trim());
			if( v < 0 ) {
				throw new NumberFormatException();
			}
			return v;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Enter "+what+" as a whole number.");
		}
	}
}
