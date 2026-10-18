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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.fx.FolderTree;

class TreeFollowerTest {

	@TempDir
	static File root;

	@BeforeAll
	static void startFx() throws Exception {
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

	private static <T> T onFx(Supplier<T> code) throws Exception {
		CompletableFuture<T> ret = new CompletableFuture<>();
		Platform.runLater(()->{
			try {
				ret.complete(code.get());
			} catch (Throwable e) {
				ret.completeExceptionally(e);
			}
		});
		return ret.get(30, TimeUnit.SECONDS);
	}

	private static FileSource fs(File f) throws Exception {
		return FileSourceFactory.getDefaultFactory().createFileSource(f.getAbsolutePath());
	}

	private static File selectedPathWithin(FolderTree tree) throws Exception {
		FileSource sel = onFx(tree::getSelectedFolder);
		return sel == null ? null : new File(sel.getAbsolutePath());
	}

	private static File waitForSelection(FolderTree tree, File want) throws Exception {
		long end = System.currentTimeMillis()+10_000;
		File got = null;
		while( System.currentTimeMillis() < end ) {
			got = selectedPathWithin(tree);
			if( want.equals(got)) {
				return got;
			}
			Thread.sleep(50);
		}
		return got;
	}

	@Test
	void selectsAFolderBelowTheTopFolderAndExpandsTheWay() throws Exception {
		File c = new File(root, "a/b/c");
		assertTrue(c.mkdirs());
		assertTrue(new File(root, "a/other").mkdirs());
		FolderTree tree = onFx(FolderTree::new);
		TreeFollower follower = onFx(()->new TreeFollower(tree));
		onFx(()->{
			try {
				tree.setTopFolders(List.of(fs(root)));
				follower.reveal(fs(c));
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			return null;
		});

		assertEquals(c, waitForSelection(tree, c));
		// the folders above it are open
		assertTrue(onFx(()->tree.getRoot().getChildren().get(0).isExpanded()));
		assertFalse(follower.isSelecting());
	}

	@Test
	void aLaterRequestWins() throws Exception {
		File x = new File(root, "x/y");
		File z = new File(root, "z/w");
		assertTrue(x.mkdirs());
		assertTrue(z.mkdirs());
		FolderTree tree = onFx(FolderTree::new);
		TreeFollower follower = onFx(()->new TreeFollower(tree));
		onFx(()->{
			try {
				tree.setTopFolders(List.of(fs(root)));
				follower.reveal(fs(x));
				follower.reveal(fs(z));
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			return null;
		});
		assertEquals(z, waitForSelection(tree, z));
		Thread.sleep(300);
		assertEquals(z, selectedPathWithin(tree));
		assertNotNull(onFx(tree::getSelectedFolder));
	}
}
