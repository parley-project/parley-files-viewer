/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.com/tony">Tony Bringardner</A>
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
 *   
 *	@author Tony Bringardner   
 *
 *
 * ~version~V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.memory.MemoryFileSourceFactory;

/** The viewer is told when the folder it shows changes (D90, D94). */
public class FolderWatcherTest {

	private String savedInterval;
	private String savedWatch;
	private MemoryFileSourceFactory mem;
	private FolderWatcher watcher;
	private final BlockingQueue<String> told = new LinkedBlockingQueue<String>();

	@BeforeEach
	public void setUp() throws Exception {
		savedInterval = System.getProperty("parley.files.watch.pollInterval");
		savedWatch = System.getProperty(FolderWatcher.PROPERTY_WATCH);
		System.setProperty("parley.files.watch.pollInterval", "50");
		System.clearProperty(FolderWatcher.PROPERTY_WATCH);
		mem = new MemoryFileSourceFactory();
		mem.connect(new Properties());
		watcher = new FolderWatcher(f -> told.add(f.getAbsolutePath()), Runnable::run);
	}

	@AfterEach
	public void tearDown() throws Exception {
		watcher.close();
		mem.disConnect();
		restore("parley.files.watch.pollInterval", savedInterval);
		restore(FolderWatcher.PROPERTY_WATCH, savedWatch);
	}

	private static void restore(String name, String value) {
		if( value == null ) {
			System.clearProperty(name);
		} else {
			System.setProperty(name, value);
		}
	}

	private FileSource folder(String path) throws Exception {
		FileSource ret = mem.createFileSource(path);
		assertTrue(ret.mkdirs());
		return ret;
	}

	/** the watch is registered in a thread of its own: wait for it before changing the folder */
	private void watching(FileSource dir) throws Exception {
		watcher.watch(dir);
		long end = System.currentTimeMillis()+5000;
		while( !dir.equals(watcher.getFolder()) && System.currentTimeMillis() < end ) {
			Thread.sleep(10);
		}
		assertEquals(dir, watcher.getFolder());
	}

	@Test
	public void anEntryAddedOrRemovedIsTold() throws Exception {
		FileSource dir = folder("/shown");
		watching(dir);

		assertTrue(mem.createFileSource("/shown/a").createNewFile());
		assertEquals("/shown", told.poll(5, TimeUnit.SECONDS));

		assertTrue(mem.createFileSource("/shown/a").delete());
		assertEquals("/shown", told.poll(5, TimeUnit.SECONDS));
	}

	@Test
	public void aBurstIsToldOnce() throws Exception {
		FileSource dir = folder("/burst");
		watching(dir);
		for (int i = 0; i < 20; i++) {
			assertTrue(mem.createFileSource("/burst/f"+i).createNewFile());
		}
		assertEquals("/burst", told.poll(5, TimeUnit.SECONDS));
		assertNull(told.poll(FolderWatcher.QUIET_MILLIS*4, TimeUnit.MILLISECONDS), "one change, told once");
	}

	@Test
	public void watchingAnotherFolderLeavesTheFirst() throws Exception {
		FileSource one = folder("/one");
		FileSource two = folder("/two");
		watching(one);
		watching(two);

		assertTrue(mem.createFileSource("/one/a").createNewFile());
		assertNull(told.poll(500, TimeUnit.MILLISECONDS), "the first folder is not watched any more");
		assertTrue(mem.createFileSource("/two/a").createNewFile());
		assertEquals("/two", told.poll(5, TimeUnit.SECONDS));
	}

	@Test
	public void nothingIsToldAfterClose() throws Exception {
		FileSource dir = folder("/closed");
		watching(dir);
		watcher.close();
		assertTrue(mem.createFileSource("/closed/a").createNewFile());
		assertNull(told.poll(500, TimeUnit.MILLISECONDS));
		assertNull(watcher.getFolder());
		watcher.watch(dir);
		Thread.sleep(100);
		assertNull(watcher.getFolder(), "a closed watcher watches nothing");
	}

	@Test
	public void aFolderThatCantBeWatchedIsJustNotWatched() throws Exception {
		FileSource file = mem.createFileSource("/just-a-file");
		assertTrue(file.createNewFile());
		watcher.watch(file);
		Thread.sleep(200);
		assertNull(watcher.getFolder());
		watcher.watch(null);
	}

	@Test
	public void theSettingTurnsItOff() throws Exception {
		System.setProperty(FolderWatcher.PROPERTY_WATCH, "false");
		FileSource dir = folder("/off");
		watcher.watch(dir);
		Thread.sleep(200);
		assertNull(watcher.getFolder());
	}
}
