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
package us.bringardner.parley.files.viewer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.browse.RecentFile;
import us.bringardner.parley.files.viewer.Favorites;

/**
 * The Favorites list: add, remove, order, and that it is kept for the next run.
 */
public class TestFavorites {

	@TempDir
	File tmp;

	private Preferences prefs;

	@BeforeEach
	public void setUp() {
		// a node of its own, removed afterwards so nothing is left in the user's preferences
		prefs = Preferences.userRoot().node("us/bringardner/parley/files/viewer/test/favorites-" + UUID.randomUUID());
	}

	@AfterEach
	public void tearDown() throws Exception {
		prefs.removeNode();
		prefs.flush();
	}

	private FileSource folder(String name) throws Exception {
		File f = new File(tmp, name);
		assertTrue(f.mkdirs() || f.isDirectory());
		return FileSourceFactory.fileProxyFactory.createFileSource(f.getAbsolutePath());
	}

	@Test
	public void addKeepsOrderAndIgnoresDuplicates() throws Exception {
		Favorites favorites = new Favorites(prefs);
		FileSource a = folder("a");
		FileSource b = folder("b");
		assertTrue(favorites.list().isEmpty());
		favorites.add(b);
		favorites.add(a);
		favorites.add(b);
		List<RecentFile> list = favorites.list();
		assertEquals(2, list.size());
		assertEquals(b.getCanonicalPath(), list.get(0).getPath());
		assertEquals(a.getCanonicalPath(), list.get(1).getPath());
		assertTrue(favorites.contains(a));
	}

	@Test
	public void removeTakesOnlyThatFolder() throws Exception {
		Favorites favorites = new Favorites(prefs);
		FileSource a = folder("a");
		FileSource b = folder("b");
		favorites.add(a);
		favorites.add(b);
		favorites.remove(a);
		assertFalse(favorites.contains(a));
		assertTrue(favorites.contains(b));
		assertEquals(1, favorites.list().size());
		// removing what isn't there changes nothing
		favorites.remove(a);
		assertEquals(1, favorites.list().size());
	}

	@Test
	public void theListIsKeptForTheNextRun() throws Exception {
		FileSource a = folder("a");
		new Favorites(prefs).add(a);
		Favorites next = new Favorites(prefs);
		assertTrue(next.contains(a));
		assertEquals(1, next.list().size());
	}

	@Test
	public void anEntryThatCantBeReadIsSkipped() throws Exception {
		FileSource a = folder("a");
		Favorites favorites = new Favorites(prefs);
		favorites.add(a);
		prefs.node("Favorites").put("f1", "not an entry");
		assertEquals(1, favorites.list().size());
	}
}
