/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2026 <A href="http://bringardner.com/tony">Tony Bringardner</A>
 * 
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE>
 *   
 *	@author Tony Bringardner   
 */
package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.memory.MemoryFileSourceFactory;

/** The viewer's Delete (DeleteActions): walked first, files several at a time, links never followed. */
public class DeleteActionsTest {

	@TempDir
	Path dir;

	private FileSource fs(Path p) throws IOException {
		return FileSourceFactory.getDefaultFactory().createFileSource(p.toString());
	}

	private Path write(String rel, String text) throws IOException {
		Path p = dir.resolve(rel);
		Files.createDirectories(p.getParent());
		Files.writeString(p, text);
		return p;
	}

	/** What a Delete reported. */
	static class Recorded implements ArchiveActions.Progress {
		int total = -1;
		final List<Integer> counts = java.util.Collections.synchronizedList(new ArrayList<>());

		@Override
		public void total(int count) {
			total = count;
		}

		@Override
		public void item(int done, String name) {
			counts.add(done);
		}
	}

	@Test
	public void aFolderAndAllInItSeveralAtATime() throws Exception {
		for(int i = 0; i < 60; i++) {
			write("docs/f"+i+".txt", "x");
			write("docs/sub/deeper/g"+i+".txt", "y");
		}
		write("keep.txt", "k");
		FileSource docs = fs(dir.resolve("docs"));
		DeleteActions.Plan plan = DeleteActions.plan(List.of(docs));
		assertEquals(120, plan.files.size());
		// each folder after what is in it: deeper, sub, docs
		assertEquals(List.of("deeper", "sub", "docs"), plan.folders.stream().map(FileSource::getName).collect(java.util.stream.Collectors.toList()));

		Recorded progress = new Recorded();
		DeleteActions.delete(plan, DeleteActions.workersFor(docs.getFileSourceFactory()), progress);
		assertFalse(Files.exists(dir.resolve("docs")));
		assertTrue(Files.exists(dir.resolve("keep.txt")));
		assertEquals(123, progress.total);
		assertEquals(123, progress.counts.size());
		assertEquals(123, progress.counts.stream().mapToInt(Integer::intValue).max().getAsInt());
	}

	@Test
	public void aLinkIsDeletedNotFollowed() throws Exception {
		Path outside = Files.createDirectories(dir.resolve("outside"));
		Files.writeString(outside.resolve("precious.txt"), "p");
		write("docs/a.txt", "a");
		Files.createSymbolicLink(dir.resolve("docs/link-to-outside"), outside);
		FileSource docs = fs(dir.resolve("docs"));
		DeleteActions.delete(DeleteActions.plan(List.of(docs)), DeleteActions.PARALLEL_DELETES, new Recorded());
		assertFalse(Files.exists(dir.resolve("docs"), java.nio.file.LinkOption.NOFOLLOW_LINKS));
		// what the link pointed to is untouched
		assertEquals("p", Files.readString(outside.resolve("precious.txt")));
	}

	@Test
	public void filesAndFoldersTogether() throws Exception {
		write("a.txt", "a");
		write("b/c.txt", "c");
		DeleteActions.delete(DeleteActions.plan(List.of(fs(dir.resolve("a.txt")), fs(dir.resolve("b")))), 1, new Recorded());
		assertFalse(Files.exists(dir.resolve("a.txt")));
		assertFalse(Files.exists(dir.resolve("b")));
	}

	@Test
	public void onAFileSystemThatIsNotLocal() throws Exception {
		MemoryFileSourceFactory memory = new MemoryFileSourceFactory();
		memory.connect();
		try {
			FileSource home = memory.createFileSource("/home/tony");
			for(String n : List.of("docs/a.txt", "docs/sub/b.txt", "other.txt")) {
				FileSource f = home.getChild(n);
				f.getParentFile().mkdirs();
				try (OutputStream out = f.getOutputStream()) {
					out.write(1);
				}
			}
			// not a factory made to be used from several threads: one at a time
			assertEquals(1, DeleteActions.workersFor(memory));
			Recorded progress = new Recorded();
			DeleteActions.delete(DeleteActions.plan(List.of(home.getChild("docs"))), 1, progress);
			assertFalse(home.getChild("docs").exists());
			assertTrue(home.getChild("other.txt").exists());
			assertEquals(4, progress.total);
		} finally {
			memory.disConnect();
		}
	}

	@Test
	public void howManyAtATime() {
		assertEquals(DeleteActions.PARALLEL_DELETES, DeleteActions.workersFor(FileSourceFactory.getDefaultFactory()));
	}
}
