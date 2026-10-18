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
package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;

/** Renaming keeps a file in its own folder (the Swing viewer moved it to the factory's current one). */
public class FileRenameTest {

	@TempDir
	Path dir;

	private FileSource file(String name) throws IOException {
		Path p = dir.resolve(name);
		Files.writeString(p, "x");
		return FileSourceFactory.getDefaultFactory().createFileSource(p.toString());
	}

	@Test
	public void staysInItsFolder() throws Exception {
		FileSource f = file("old.txt");
		FileSource renamed = FileRename.rename(f, " new.txt ");
		assertEquals("new.txt", renamed.getName());
		assertTrue(new File(dir.toFile(), "new.txt").exists());
		assertFalse(new File(dir.toFile(), "old.txt").exists());
		assertFalse(new File("new.txt").exists(), "not moved to the current folder");
	}

	@Test
	public void aNameOnly() throws Exception {
		FileSource f = file("a.txt");
		file("taken.txt");
		assertThrows(IOException.class, ()->FileRename.rename(f, ""));
		assertThrows(IOException.class, ()->FileRename.rename(f, "sub/b.txt"));
		assertThrows(IOException.class, ()->FileRename.rename(f, "taken.txt"));
		assertTrue(new File(dir.toFile(), "a.txt").exists(), "still there");
	}
}
