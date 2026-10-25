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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.viewer.fx.CopyQueue.Job;
import us.bringardner.parley.files.viewer.fx.CopyQueue.State;

class CopyQueueTest {

	@TempDir
	File root;

	private FileSource fs(File f) throws IOException {
		return FileSourceFactory.getDefaultFactory().createFileSource(f.getAbsolutePath());
	}

	private File write(String path, String text) throws IOException {
		File f = new File(root, path);
		assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
		Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	private String read(String path) throws IOException {
		return new String(Files.readAllBytes(new File(root, path).toPath()), StandardCharsets.UTF_8);
	}

	/** Runs the jobs and waits until the queue is idle again. */
	private void runAll(CopyQueue q, List<Job> jobs) throws Exception {
		CountDownLatch idle = new CountDownLatch(1);
		q.addListener(new CopyQueue.Listener() {
			@Override public void changed() { }
			@Override public void idle(boolean foldersMade) { idle.countDown(); }
		});
		q.enqueue(jobs);
		assertTrue(idle.await(20, TimeUnit.SECONDS), "the queue didn't finish");
	}

	@Test
	void copiesAFolderWithEverythingInIt() throws Exception {
		write("src/a.txt", "alpha");
		write("src/sub/b.txt", "bravo");
		assertTrue(new File(root, "dest").mkdir());

		CopyPlanner.Plan plan = CopyPlanner.plan(List.of(fs(new File(root, "src"))), fs(new File(root, "dest")),
				CopyPlanner.REPLACE_ALL);
		assertEquals(2, plan.getJobs().size());
		assertTrue(plan.isCreatedFolders());
		CopyQueue q = new CopyQueue();
		runAll(q, plan.getJobs());

		assertEquals("alpha", read("dest/src/a.txt"));
		assertEquals("bravo", read("dest/src/sub/b.txt"));
		assertEquals(2, q.getBatchTotal());
		assertEquals(2, q.getBatchDone());
		assertEquals(0, q.getBatchFailed());
		for(Job j : q.getJobs()) {
			assertEquals(State.DONE, j.getState());
			assertEquals(1.0, j.getFraction());
		}
		assertFalse(q.isBusy());
	}

	@Test
	void anExistingFileIsReplacedOrSkippedAsTheResolverSays() throws Exception {
		write("src/a.txt", "new");
		write("dest/a.txt", "old");
		FileSource src = fs(new File(root, "src/a.txt"));
		FileSource dest = fs(new File(root, "dest"));

		CopyPlanner.Plan skipped = CopyPlanner.plan(List.of(src), dest, e->CopyPlanner.Answer.SKIP);
		assertTrue(skipped.getJobs().isEmpty());
		assertEquals("old", read("dest/a.txt"));

		CopyPlanner.Plan replaced = CopyPlanner.plan(List.of(src), dest, e->CopyPlanner.Answer.REPLACE);
		runAll(new CopyQueue(), replaced.getJobs());
		assertEquals("new", read("dest/a.txt"));

		assertThrows(CancellationException.class, ()->CopyPlanner.plan(List.of(src), dest, e->CopyPlanner.Answer.CANCEL));
	}

	@Test
	void anAnswerForAllIsAskedOnce() throws Exception {
		write("src/a.txt", "1");
		write("src/b.txt", "2");
		write("dest/a.txt", "x");
		write("dest/b.txt", "y");
		int[] asked = { 0 };
		CopyPlanner.Plan plan = CopyPlanner.plan(
				List.of(fs(new File(root, "src/a.txt")), fs(new File(root, "src/b.txt"))), fs(new File(root, "dest")),
				e->{
					asked[0]++;
					return CopyPlanner.Answer.SKIP_ALL;
				});
		assertEquals(1, asked[0]);
		assertTrue(plan.getJobs().isEmpty());
	}

	@Test
	void refusesAFolderIntoItselfAndSkipsAFileAlreadyThere() throws Exception {
		write("src/a.txt", "alpha");
		write("src/sub/b.txt", "bravo");
		assertThrows(IOException.class, ()->CopyPlanner.plan(List.of(fs(new File(root, "src"))),
				fs(new File(root, "src/sub")), CopyPlanner.REPLACE_ALL));
		assertThrows(IOException.class, ()->CopyPlanner.plan(List.of(fs(new File(root, "src"))),
				fs(new File(root, "src")), CopyPlanner.REPLACE_ALL));
		assertTrue(CopyPlanner.plan(List.of(fs(new File(root, "src/a.txt"))), fs(new File(root, "src")),
				CopyPlanner.REPLACE_ALL).getJobs().isEmpty());
	}

	@Test
	void aFailedFileIsRecordedAndTheRestGoOn() throws Exception {
		write("src/a.txt", "alpha");
		write("src/b.txt", "bravo");
		assertTrue(new File(root, "dest/a.txt").mkdirs());
		// a.txt can't be written: a folder is in the way
		Job bad = new Job(fs(new File(root, "src/a.txt")), fs(new File(root, "dest/a.txt")));
		Job good = new Job(fs(new File(root, "src/b.txt")), fs(new File(root, "dest/b.txt")));
		CopyQueue q = new CopyQueue();
		runAll(q, List.of(bad, good));

		assertEquals(State.FAILED, bad.getState());
		assertNotNull(bad.getError());
		assertEquals(State.DONE, good.getState());
		assertEquals("bravo", read("dest/b.txt"));
		assertEquals(1, q.getBatchFailed());
		assertTrue(CopyManagerPane.statusText(bad).startsWith("Failed"));
	}

	@Test
	void stopAllDropsWhatWaitsAndRemovesAPartialFile() throws Exception {
		StringBuilder big = new StringBuilder();
		for(int i=0; i < 100_000; i++) {
			big.append("0123456789");
		}
		write("src/a.bin", big.toString());
		write("src/b.bin", "b");
		write("src/c.bin", "c");
		assertTrue(new File(root, "dest").mkdir());
		CopyQueue q = new CopyQueue();
		q.setPaused(true);
		Job a = new Job(fs(new File(root, "src/a.bin")), fs(new File(root, "dest/a.bin")));
		Job b = new Job(fs(new File(root, "src/b.bin")), fs(new File(root, "dest/b.bin")));
		Job c = new Job(fs(new File(root, "src/c.bin")), fs(new File(root, "dest/c.bin")));

		CountDownLatch idle = new CountDownLatch(1);
		AtomicBoolean sawRunning = new AtomicBoolean();
		q.addListener(new CopyQueue.Listener() {
			@Override public void changed() {
				if( a.getState() == State.RUNNING ) {
					sawRunning.set(true);
				}
			}
			@Override public void idle(boolean foldersMade) { idle.countDown(); }
		});
		q.enqueue(List.of(a, b, c));
		long end = System.currentTimeMillis()+10_000;
		while( !sawRunning.get() && System.currentTimeMillis() < end ) {
			Thread.sleep(10);
		}
		assertTrue(sawRunning.get());
		q.cancelAll();
		q.setPaused(false);
		assertTrue(idle.await(20, TimeUnit.SECONDS));

		assertEquals(State.CANCELED, a.getState());
		assertEquals(State.CANCELED, b.getState());
		assertEquals(State.CANCELED, c.getState());
		assertFalse(new File(root, "dest/a.bin").exists());
		assertFalse(new File(root, "dest/b.bin").exists());

		q.clearHistory();
		assertTrue(q.getJobs().isEmpty());
		assertEquals(0, q.getBatchTotal());
	}

	@Test
	void anOverwriteLeavesNoTemporaryFile() throws Exception {
		write("src/a.txt", "new");
		write("dest/a.txt", "old");
		Job j = new Job(fs(new File(root, "src/a.txt")), fs(new File(root, "dest/a.txt")));
		runAll(new CopyQueue(), List.of(j));
		assertEquals(State.DONE, j.getState());
		assertEquals("new", read("dest/a.txt"));
		assertEquals(List.of("a.txt"), List.of(new File(root, "dest").list()));
	}

	@Test
	void aFailedOverwriteKeepsTheOldFile() throws Exception {
		write("dest/a.txt", "old");
		// the source is gone by the time its turn comes
		Job j = new Job(fs(new File(root, "src/a.txt")), fs(new File(root, "dest/a.txt")));
		runAll(new CopyQueue(), List.of(j));
		assertEquals(State.FAILED, j.getState());
		assertEquals("old", read("dest/a.txt"));
		assertEquals(List.of("a.txt"), List.of(new File(root, "dest").list()));
	}

	@Test
	void aCanceledOverwriteKeepsTheOldFile() throws Exception {
		StringBuilder big = new StringBuilder();
		for(int i=0; i < 100_000; i++) {
			big.append("0123456789");
		}
		write("src/a.bin", big.toString());
		write("dest/a.bin", "old");
		CopyQueue q = new CopyQueue();
		q.setPaused(true);
		Job a = new Job(fs(new File(root, "src/a.bin")), fs(new File(root, "dest/a.bin")));
		CountDownLatch idle = new CountDownLatch(1);
		AtomicBoolean sawRunning = new AtomicBoolean();
		q.addListener(new CopyQueue.Listener() {
			@Override public void changed() {
				if( a.getState() == State.RUNNING ) {
					sawRunning.set(true);
				}
			}
			@Override public void idle(boolean foldersMade) { idle.countDown(); }
		});
		q.enqueue(List.of(a));
		long end = System.currentTimeMillis()+10_000;
		while( !sawRunning.get() && System.currentTimeMillis() < end ) {
			Thread.sleep(10);
		}
		assertTrue(sawRunning.get());
		q.cancel(a);
		q.setPaused(false);
		assertTrue(idle.await(20, TimeUnit.SECONDS));
		assertEquals(State.CANCELED, a.getState());
		assertEquals("old", read("dest/a.bin"));
		assertEquals(List.of("a.bin"), List.of(new File(root, "dest").list()));
	}
}
