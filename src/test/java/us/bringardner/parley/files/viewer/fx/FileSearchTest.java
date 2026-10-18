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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;

class FileSearchTest {

	@TempDir
	File root;

	private FileSource fs(File f) throws IOException {
		return FileSourceFactory.getDefaultFactory().createFileSource(f.getAbsolutePath());
	}

	private void write(String path, int bytes) throws IOException {
		File f = new File(root, path);
		assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
		Files.write(f.toPath(), new String(new char[bytes]).replace('\0', 'x').getBytes(StandardCharsets.UTF_8));
	}

	private List<String> search(SearchCriteria c) throws IOException {
		TreeSet<String> found = new TreeSet<>();
		new FileSearch(c, ()->false, f->found.add(f.getName())).run(fs(root));
		return new ArrayList<>(found);
	}

	private void tree() throws IOException {
		write("a.txt", 10);
		write("B.TXT", 2000);
		write("sub/c.log", 5000);
		write("sub/deep/d.txt", 1);
		write("a.txt.bak", 3);
	}

	@Test
	void nameMatchesWholeNameWithWildcardsIgnoringCaseByDefault() throws Exception {
		tree();
		assertEquals(List.of("B.TXT", "a.txt", "d.txt"), search(new SearchCriteria().name("*.txt", false)));
		assertEquals(List.of("a.txt", "d.txt"), search(new SearchCriteria().name("*.txt", true)));
		assertEquals(List.of("B.TXT"), search(new SearchCriteria().name("?.TXT", true)));
		// a dot is a dot, not any character, and the whole name must match
		assertEquals(List.of("a.txt"), search(new SearchCriteria().name("a.txt", false)));
		assertFalse(search(new SearchCriteria().name("a.txt", false)).contains("a.txt.bak"));
	}

	@Test
	void sizeAndTypeNarrowTheResult() throws Exception {
		tree();
		assertEquals(List.of("B.TXT", "c.log"),
				search(new SearchCriteria().size(SearchCriteria.Operator.AT_LEAST, 1000, 0)));
		assertEquals(List.of("a.txt"),
				search(new SearchCriteria().size(SearchCriteria.Operator.BETWEEN, 5, 10)));
		assertEquals(List.of("deep", "sub"),
				search(new SearchCriteria().type(SearchCriteria.Type.DIRECTORY)));
		assertEquals(List.of("B.TXT"), search(new SearchCriteria().name("*.txt", false)
				.size(SearchCriteria.Operator.AT_LEAST, 1000, 0)));
	}

	@Test
	void dateRangeIsFromInclusiveToExclusive() throws Exception {
		tree();
		File a = new File(root, "a.txt");
		assertTrue(a.setLastModified(1_000_000_000_000L));
		assertEquals(List.of("a.txt"), search(new SearchCriteria().modified(1_000_000_000_000L, 1_000_000_001_000L)));
		assertFalse(search(new SearchCriteria().modified(1_000_000_001_000L, null)).contains("a.txt"));
		assertFalse(search(new SearchCriteria().modified(null, 1_000_000_000_000L)).contains("a.txt"));
	}

	@Test
	void canceledSearchStops() throws Exception {
		tree();
		List<FileSource> found = new ArrayList<>();
		new FileSearch(new SearchCriteria().name("*", false), ()->true, found::add).run(fs(root));
		assertTrue(found.isEmpty());
		assertTrue(new SearchCriteria().isEmpty());
	}
}
