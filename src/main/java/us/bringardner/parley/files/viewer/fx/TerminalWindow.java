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

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import us.bringardner.fsh.Console;
import us.bringardner.fsh.ConsoleSignal;
import us.bringardner.fx.menu.StandardMenus;
import us.bringardner.parley.files.FileSource;

/**
 * File > New Terminal: an fsh shell in a window of its own, in a folder of any file system the
 * viewer shows (local or remote), as the Swing viewer's terminal. "exit" closes the window; closing
 * the window ends the shell.
 */
class TerminalWindow {

	private final Stage stage = new Stage();
	private final TerminalView view = new TerminalView();
	private final Console console = new Console();

	/**
	 * @param dir the folder the shell starts in, or null for the default one
	 */
	TerminalWindow(FileSource dir) throws IOException {
		// "exit" ends this shell, not the viewer
		Console.exitJvm = false;
		console.setKeyboardReader(view.getIo());
		console.setStdIn(view.getStdIn());
		console.setStdOut(view.getStdOut());
		console.setStdErr(view.getStdErr());
		view.getIo().setConsole(console);
		if( dir != null ) {
			console.setCurrentDirectory(dir);
		}

		StandardMenus menus = new StandardMenus(FxLook.APP_NAME).about(()->ViewerWindow.showAbout(stage))
				.settings(()->ViewerWindow.showSettings(stage)).quit(ViewerWindow::okToQuit);
		menus.file.getItems().add(StandardMenus.item("Close", "Shortcut+W", stage::close));
		menus.edit.getItems().addAll(
				StandardMenus.item("Copy", "Shortcut+C", ()->view.getArea().copy()),
				StandardMenus.item("Paste", "Shortcut+V", ()->{
					view.getArea().moveTo(view.getArea().getLength());
					view.getArea().paste();
				}),
				StandardMenus.item("Select All", "Shortcut+A", ()->view.getArea().selectAll()), new SeparatorMenuItem(),
				StandardMenus.item("Clear", "Shortcut+K", view::clear));
		// a fixed-width font: the first of these this computer has (CSS can't list them in JavaFX)
		view.getArea().setStyle("-fx-font-family: \"" + monospaced() + "\"; -fx-font-size: 13px;");
		BorderPane root = new BorderPane(view);
		root.setTop(menus.build(stage, new Menu[0]));
		Scene scene = new Scene(root, 760, 460);
		scene.getStylesheets().add(TerminalWindow.class.getResource("terminal.css").toExternalForm());
		stage.setScene(FxLook.apply(stage, scene));
		stage.setTitle("Terminal - " + (dir == null ? "fsh" : dir.getAbsolutePath()));
		stage.setOnHidden(e->{
			// the window is gone: so is its shell
			if( console.isAlive() ) {
				view.getIo().signal(ConsoleSignal.Hup);
				Console.exit(console, 129);
			}
		});
	}

	/** Shows the window and starts the shell; the window closes when the shell ends. */
	void show() {
		stage.show();

		// (as the Swing viewer starts its terminal: no options)
		console.setName("terminal " + stage.getTitle());
		console.setDaemon(true);
		console.start();
		Thread watch = new Thread(()->{
			try {
				while( console.isAlive() ) {
					console.join(500);
				}
			} catch (InterruptedException e) {
				return;
			}
			Platform.runLater(()->{
				if( stage.isShowing() ) {
					stage.close();
				}
			});
		}, "terminal watch");
		watch.setDaemon(true);
		watch.start();
		view.getArea().requestFocus();
	}

	/** The fixed-width font for the terminal: Menlo (macOS), Consolas (Windows), DejaVu Sans Mono (Linux). */
	static String monospaced() {
		java.util.List<String> have = javafx.scene.text.Font.getFamilies();
		for(String f : new String[] {"Menlo", "Consolas", "DejaVu Sans Mono", "Liberation Mono"}) {
			if( have.contains(f) ) {
				return f;
			}
		}
		return "Monospaced";
	}

		Stage getStage() {
		return stage;
	}

	TerminalView getView() {
		return view;
	}

	Console getConsole() {
		return console;
	}
}
