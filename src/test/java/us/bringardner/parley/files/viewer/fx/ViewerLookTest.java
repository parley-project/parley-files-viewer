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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import us.bringardner.fx.gradient.GradientLook;
import us.bringardner.fx.menu.StandardMenus;
import us.bringardner.parley.files.FileSourceFactory;

/** The JavaFX viewer's look and menus: those of every Parley application. */
public class ViewerLookTest {

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

	@Test
	public void theWindowHasTheLookAndTheStandardMenus() throws Exception {
		FileSourceFactory local = FileSourceFactory.getDefaultFactory();
		List<String> menus = onFx(()->{
			Stage stage = new Stage();
			new ViewerWindow(local, local.createFileSource(dir.toString()), stage);
			assertTrue(GradientLook.isApplied(stage), "the gradient look");
			assertFalse(stage.getIcons().isEmpty(), "the Parley icon");
			MenuBar bar = (MenuBar) stage.getScene().getRoot().lookup(".menu-bar");
			List<String> ret = new ArrayList<>();
			for(Menu m : bar.getMenus()) {
				StringBuilder b = new StringBuilder(m.getText()).append(":");
				for(MenuItem i : m.getItems()) {
					if( i.getText() != null ) {
						b.append(" ").append(i.getText()).append(",");
					}
				}
				ret.add(b.toString());
			}
			return ret;
		});
		assertEquals(5, menus.size(), menus.toString());
		assertTrue(menus.get(0).startsWith("File: New Window, New Remote Session..."), menus.get(0));
		assertTrue(menus.get(1).startsWith("Edit: Copy, Paste,") && menus.get(1).contains("Find..."), menus.get(1));
		assertTrue(menus.get(2).startsWith("View: Show Hidden Files, Show File Extensions, Refresh"), menus.get(2));
		assertTrue(menus.get(3).startsWith("Window: Minimize"), menus.get(3));
		if( !StandardMenus.isMac() ) {
			assertTrue(menus.get(0).endsWith("Settings..., Exit,"), menus.get(0));
			assertEquals("Help: About " + FxLook.APP_NAME + ",", menus.get(4));
		}
	}

	@Test
	public void sharesItsSettingsWithTheSwingViewer() {
		assertEquals("/us/bringardner/parley/files/viewer", FxLook.prefs().absolutePath());
		assertEquals(java.util.prefs.Preferences.userNodeForPackage(us.bringardner.parley.files.viewer.RootFile.class)
				.absolutePath(), FxLook.prefs().absolutePath());
	}

	@Test
	public void quitsWhenNothingIsBeingCopied() throws Exception {
		Assumptions.assumeFalse(CopyQueue.shared().isBusy());
		assertTrue(onFx(ViewerWindow::okToQuit));
		assertFalse(onFx(FxLook::appIcons).isEmpty());
	}
}
