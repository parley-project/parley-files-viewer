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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import us.bringardner.parley.files.viewer.fx.FileOperations.Progress;

/**
 * Runs a long file operation in the background with a small window that shows how far it is and
 * can cancel it. The window appears only if the operation takes longer than a moment.
 */
final class TransferWindow {

	static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r->{
		Thread t = new Thread(r, "parley-viewer-fx");
		t.setDaemon(true);
		return t;
	});

	/** The work: it reports to and obeys the progress. */
	interface Work<T> {
		T run(Progress progress) throws Exception;
	}

	private TransferWindow() {
	}

	/**
	 * Runs work off the JavaFX thread. onDone gets the result on the JavaFX thread; onFailed gets
	 * the error (a cancel is not reported as one).
	 */
	static <T> void run(Window owner, String title, Work<T> work, Consumer<T> onDone, Consumer<Throwable> onFailed) {
		Label what = new Label(" ");
		ProgressBar bar = new ProgressBar(ProgressBar.INDETERMINATE_PROGRESS);
		bar.setPrefWidth(340);
		Task<T> task = new Task<T>() {
			@Override
			protected T call() throws Exception {
				Task<T> self = this;
				return work.run(new Progress() {
					@Override
					public void step(String text, int done, int total) {
						Platform.runLater(()->what.setText(text));
						if( total > 0 ) {
							updateProgress(done, total);
						}
					}

					@Override
					public boolean isCanceled() {
						return self.isCancelled();
					}
				});
			}
		};
		ReadOnlyDoubleProperty p = task.progressProperty();
		p.addListener((o, was, is)->bar.setProgress(is.doubleValue() < 0 ? ProgressBar.INDETERMINATE_PROGRESS : is.doubleValue()));

		Stage stage = new Stage();
		if( owner != null ) {
			stage.initOwner(owner);
		}
		stage.setTitle(title);
		Button cancel = new Button("Cancel");
		cancel.setOnAction(e->task.cancel(true));
		Region gap = new Region();
		HBox.setHgrow(gap, Priority.ALWAYS);
		VBox box = new VBox(8, what, bar, new HBox(gap, cancel));
		box.setPadding(new Insets(12));
		stage.setScene(FxLook.apply(stage, new Scene(box)));
		stage.setOnCloseRequest(e->task.cancel(true));

		// shown only if it takes a while
		Thread delay = new Thread(()->{
			try {
				Thread.sleep(800);
			} catch (InterruptedException e) {
				return;
			}
			Platform.runLater(()->{
				if( task.isRunning()) {
					stage.show();
				}
			});
		}, "parley-viewer-fx-delay");
		delay.setDaemon(true);

		task.setOnSucceeded(e->{
			stage.close();
			if( onDone != null ) {
				onDone.accept(task.getValue());
			}
		});
		task.setOnCancelled(e->{
			stage.close();
			if( onFailed != null ) {
				onFailed.accept(null);
			}
		});
		task.setOnFailed(e->{
			stage.close();
			Throwable t = task.getException();
			if( t instanceof java.util.concurrent.CancellationException ) {
				t = null;
			}
			if( onFailed != null ) {
				onFailed.accept(t);
			}
		});
		EXECUTOR.execute(task);
		delay.start();
	}

	static void showError(Window owner, String title, Throwable e) {
		Alert alert = new Alert(AlertType.ERROR, e == null || e.getMessage() == null ? String.valueOf(e) : e.getMessage());
		alert.setHeaderText(title);
		if( owner != null ) {
			alert.initOwner(owner);
		}
		FxLook.apply(alert);
		alert.showAndWait();
	}
}
