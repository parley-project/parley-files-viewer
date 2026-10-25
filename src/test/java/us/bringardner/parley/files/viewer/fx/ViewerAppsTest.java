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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.viewer.IRegistry.RegData;

/** The JavaFX viewer's terminal and Open With..., as the Swing viewer has them. */
public class ViewerAppsTest {

	@TempDir
	Path dir;

	@BeforeAll
	public static void startJavaFx() throws Exception {
		CountDownLatch up = new CountDownLatch(1);
		boolean ok;
		try {
			Platform.startup(up::countDown);
			ok = up.await(10, TimeUnit.SECONDS);
			Platform.setImplicitExit(false);
		} catch (IllegalStateException alreadyRunning) {
			ok = true;
		} catch (Throwable e) {
			ok = false;
		}
		Assumptions.assumeTrue(ok, "JavaFX can't start here");
	}

	private static <T> T onFx(Callable<T> code) throws Exception {
		CompletableFuture<T> ret = new CompletableFuture<>();
		Platform.runLater(()->{
			try {
				ret.complete(code.call());
			} catch (Throwable e) {
				ret.completeExceptionally(e);
			}
		});
		return ret.get(30, TimeUnit.SECONDS);
	}

	private static void waitFor(Callable<Boolean> condition) throws Exception {
		long end = System.currentTimeMillis() + 20_000;
		while( !onFx(condition) ) {
			if( System.currentTimeMillis() > end ) {
				throw new AssertionError("timed out");
			}
			Thread.sleep(50);
		}
	}

	@Test
	public void theTerminalRunsAShellInTheFolder() throws Exception {
		Files.writeString(dir.resolve("marker.txt"), "x");
		FileSourceFactory local = FileSourceFactory.getDefaultFactory();
		TerminalWindow t = onFx(()->{
			TerminalWindow w = new TerminalWindow(local.createFileSource(dir.toString()));
			w.show();
			return w;
		});
		try {
			assertTrue(onFx(()->t.getStage().getTitle().contains(dir.toString())));
			// a prompt, then a command typed and run
			waitFor(()->t.getView().getIo().isReadingLine());
			onFx(()->{
				t.getView().getArea().appendText("ls");
				t.getView().submit();
				return null;
			});
			waitFor(()->t.getView().getArea().getText().contains("marker.txt"));
			// exit ends the shell, and the window closes
			waitFor(()->t.getView().getIo().isReadingLine());
			onFx(()->{
				t.getView().getArea().appendText("exit");
				t.getView().submit();
				return null;
			});
			waitFor(()->!t.getStage().isShowing());
			assertFalse(t.getConsole().isAlive() && t.getConsole().isRunning());
		} finally {
			onFx(()->{
				t.getStage().close();
				return null;
			});
		}
	}

	/** An application of the registry, for the dialog. */
	static class App extends RegData {
		App(String name, String path) {
			super(name, path);
		}

		@Override
		public List<Icon> getIcons() throws IOException {
			return new ArrayList<>();
		}

		@Override
		public Icon getIcon(int width, int height) throws IOException {
			java.awt.image.BufferedImage b = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = b.createGraphics();
			g.setColor(java.awt.Color.RED);
			g.fillRect(0, 0, width, height);
			g.dispose();
			return new javax.swing.ImageIcon(b);
		}

		@Override
		public String getCommand() {
			return path + " --open";
		}
	}

	@Test
	public void openWithListsTheApplicationsAndMakesTheCommand() throws Exception {
		App editor = new App("Editor", "/opt/editor");
		App viewer = new App("Viewer", "/opt/viewer");
		OpenWithDialog d = onFx(()->new OpenWithDialog(null, "notes.txt", List.of(editor, viewer)));
		assertEquals(2, onFx(()->d.getList().getItems().size()));
		assertEquals(editor, onFx(()->d.getList().getSelectionModel().getSelectedItem()), "the first is chosen");
		onFx(()->{
			d.getList().getSelectionModel().select(1);
			d.ok();
			return null;
		});
		OpenWithDialog.Choice c = d.getChosen();
		assertEquals("Viewer", c.toString());
		assertEquals(List.of("/opt/viewer", "--open", "/tmp/notes.txt"), c.command("/tmp/notes.txt"));

		// a program chosen from the disk
		OpenWithDialog.Choice program = new OpenWithDialog.Choice(null, new File("/usr/bin/less"));
		if( !System.getProperty("os.name", "").toLowerCase().startsWith("mac") ) {
			// (the program's absolute path: with a drive and \ on Windows)
			assertEquals(List.of(new File("/usr/bin/less").getAbsolutePath(), "/tmp/a b.txt"), program.command("/tmp/a b.txt"));
		}
		assertEquals("less", program.toString());

		OpenWithDialog none = onFx(()->new OpenWithDialog(null, "x", List.of()));
		onFx(()->{
			none.ok();
			none.cancel();
			return null;
		});
		assertNull(none.getChosen());

		// the icons: a Swing icon as a JavaFX image
		Image i = OpenWithDialog.image(editor.getIcon(8, 8));
		assertNotNull(i);
		assertEquals(8, i.getWidth());
		assertEquals(0xFFFF0000, i.getPixelReader().getArgb(3, 3));
	}

	@Test
	public void theMenusHaveOpenWithAndNewTerminal() throws Exception {
		FileSourceFactory local = FileSourceFactory.getDefaultFactory();
		List<String> items = onFx(()->{
			Stage stage = new Stage();
			new ViewerWindow(local, local.createFileSource(dir.toString()), stage);
			MenuBar bar = (MenuBar) stage.getScene().getRoot().lookup(".menu-bar");
			List<String> ret = new ArrayList<>();
			for(Menu m : bar.getMenus()) {
				for(MenuItem i : m.getItems()) {
					ret.add(m.getText() + " > " + i.getText());
				}
			}
			return ret;
		});
		assertTrue(items.contains("File > New Terminal"), items.toString());
		assertTrue(items.contains("File > Open With..."), items.toString());
	}
}
