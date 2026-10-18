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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import us.bringardner.parley.files.viewer.fx.CopyQueue.Job;
import us.bringardner.parley.files.viewer.fx.CopyQueue.State;

/**
 * The copy manager: shows what the {@link CopyQueue} is doing along the bottom of a window. It
 * stays out of sight until something is copied. It shows the file being copied and how far the
 * whole batch is, lets the user pause, stop the file or everything, and (under Details) lists
 * every file with its times and result. Use it on the JavaFX application thread.
 */
final class CopyManagerPane extends VBox implements CopyQueue.Listener {

	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm:ss a").withZone(ZoneId.systemDefault());

	private final CopyQueue queue;
	private final Label what = new Label();
	private final ProgressBar fileBar = new ProgressBar(0);
	private final ProgressBar filesBar = new ProgressBar(0);
	private final Label filesLabel = new Label();
	private final ToggleButton pause = new ToggleButton("Pause");
	private final Button cancelFile = new Button("Stop This File");
	private final Button cancelAll = new Button("Stop All");
	private final Button clear = new Button("Clear");
	private final Button hide = new Button("Hide");
	private final ToggleButton details = new ToggleButton("Details");
	private final ObservableList<Job> rows = FXCollections.observableArrayList();
	private final TableView<Job> table = new TableView<>(rows);
	private final AtomicBoolean updateQueued = new AtomicBoolean();

	CopyManagerPane(CopyQueue queue) {
		this.queue = queue;
		setSpacing(4);
		setPadding(new Insets(6, 8, 6, 8));
		setStyle("-fx-border-color: -fx-box-border transparent transparent transparent;");

		what.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(what, Priority.ALWAYS);
		pause.setOnAction(e->queue.setPaused(pause.isSelected()));
		cancelFile.setOnAction(e->queue.cancelCurrent());
		cancelAll.setOnAction(e->queue.cancelAll());
		clear.setOnAction(e->queue.clearHistory());
		hide.setOnAction(e->{
			details.setSelected(false);
			setVisible(false);
		});
		HBox top = new HBox(8, what, pause, cancelFile, cancelAll, clear, hide);
		top.setStyle("-fx-alignment: center-left;");

		fileBar.setMaxWidth(Double.MAX_VALUE);
		filesBar.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(filesBar, Priority.ALWAYS);
		Region gap = new Region();
		gap.setMinWidth(0);
		HBox bars = new HBox(8, filesLabel, filesBar, details);
		bars.setStyle("-fx-alignment: center-left;");

		TableColumn<Job,Job> dest = new TableColumn<>("Destination");
		dest.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue()));
		dest.setCellFactory(c->cell(j->j.getDest().getAbsolutePath()));
		dest.setPrefWidth(480);
		TableColumn<Job,Job> start = new TableColumn<>("Started");
		start.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue()));
		start.setCellFactory(c->cell(j->j.getStartTime() == 0 ? "" : TIME.format(Instant.ofEpochMilli(j.getStartTime()))));
		start.setPrefWidth(100);
		TableColumn<Job,Job> end = new TableColumn<>("Finished");
		end.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue()));
		end.setCellFactory(c->cell(j->j.isFinished() && j.getEndTime() != 0 ? TIME.format(Instant.ofEpochMilli(j.getEndTime())) : ""));
		end.setPrefWidth(100);
		TableColumn<Job,Job> status = new TableColumn<>("Status");
		status.setCellValueFactory(c->new ReadOnlyObjectWrapper<>(c.getValue()));
		status.setCellFactory(c->cell(CopyManagerPane::statusText));
		status.setPrefWidth(220);
		table.getColumns().add(dest);
		table.getColumns().add(start);
		table.getColumns().add(end);
		table.getColumns().add(status);
		table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		table.setPlaceholder(new Label("Nothing copied yet"));
		table.setPrefHeight(150);
		MenuItem stop = new MenuItem("Stop");
		stop.setOnAction(e->{
			for(Job j : table.getSelectionModel().getSelectedItems()) {
				if( j != null && !j.isFinished()) {
					queue.cancel(j);
				}
			}
		});
		table.setContextMenu(new ContextMenu(stop));
		table.visibleProperty().bind(details.selectedProperty());
		table.managedProperty().bind(details.selectedProperty());

		getChildren().addAll(top, fileBar, bars, table);
		setVisible(false);
		managedProperty().bind(visibleProperty());
		queue.addListener(this);
		update();
	}

	/** Stops listening: for when the window is closed. */
	void dispose() {
		queue.removeListener(this);
	}

	private static TableCell<Job,Job> cell(java.util.function.Function<Job,String> text) {
		return new TableCell<>() {
			@Override
			protected void updateItem(Job j, boolean empty) {
				super.updateItem(j, empty);
				setText(empty || j == null ? null : text.apply(j));
			}
		};
	}

	static String statusText(Job j) {
		switch( j.getState()) {
		case WAITING:
			return "Waiting";
		case RUNNING:
			return Math.round(j.getFraction()*100)+"%";
		case DONE:
			return "Done";
		case CANCELED:
			return "Stopped";
		case FAILED:
			Throwable e = j.getError();
			return "Failed: "+(e == null || e.getMessage() == null ? String.valueOf(e) : e.getMessage());
		default:
			return "";
		}
	}

	// ---------------------------------------------------------------- queue listener

	@Override
	public void changed() {
		if( updateQueued.compareAndSet(false, true)) {
			Platform.runLater(()->{
				updateQueued.set(false);
				update();
			});
		}
	}

	@Override
	public void idle(boolean foldersMade) {
		// windows refresh their own views; the pane only shows the end state
		changed();
	}

	private void update() {
		List<Job> jobs = queue.getJobs();
		boolean busy = queue.isBusy();
		if( !jobs.isEmpty() && (busy || !isVisible() && jobs.stream().anyMatch(j->!j.isFinished()))) {
			setVisible(true);
		}
		if( rows.size() != jobs.size() || details.isSelected()) {
			rows.setAll(jobs);
		}
		Job current = queue.getCurrent();
		int total = queue.getBatchTotal();
		int done = queue.getBatchDone();
		int failed = queue.getBatchFailed();

		pause.setSelected(queue.isPaused());
		cancelFile.setVisible(busy);
		cancelAll.setVisible(busy);
		clear.setVisible(!busy);
		hide.setVisible(!busy);
		cancelFile.setManaged(busy);
		cancelAll.setManaged(busy);
		clear.setManaged(!busy);
		hide.setManaged(!busy);
		pause.setVisible(busy);
		pause.setManaged(busy);

		if( busy && current != null ) {
			what.setText((queue.isPaused() ? "Paused: " : "Copying ")+current.getDest().getAbsolutePath());
			fileBar.setProgress(current.getExpected() <= 0 && current.getState() == State.RUNNING
					? ProgressBar.INDETERMINATE_PROGRESS : current.getFraction());
		} else if( busy ) {
			what.setText("Copying...");
			fileBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
		} else {
			what.setText(failed > 0 ? failed+(failed == 1 ? " file" : " files")+" couldn't be copied: see Details"
					: total == 0 ? "" : "Copied "+done+(done == 1 ? " file" : " files"));
			fileBar.setProgress(total == 0 ? 0 : 1);
			if( failed > 0 && !details.isSelected()) {
				details.setSelected(true);
			}
		}
		filesBar.setProgress(total == 0 ? 0 : (double) done / total);
		filesLabel.setText(done+" of "+total+(total == 1 ? " file" : " files"));
	}
}
