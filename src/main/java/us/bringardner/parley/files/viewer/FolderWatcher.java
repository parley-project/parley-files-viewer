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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import us.bringardner.parley.core.BaseObject;
import us.bringardner.parley.core.NamedExecutors;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceWatchEvent;
import us.bringardner.parley.files.FileSourceWatchKey;
import us.bringardner.parley.files.FileSourceWatchService;

/**
 * Tells the viewer that the folder it shows has changed (an entry added, removed or renamed, by anyone), so the
 * window can show it without the user choosing Refresh. It watches one folder: {@link #watch(FileSource)} replaces
 * the folder it watched, {@link #close()} ends it. The service is the folder's factory's own
 * ({@code FileSourceFactory.newWatchService()}: the operating system for local folders, the change attribute for NFS,
 * a poll for the others).
 *
 * <p>The change is reported to {@code changed} in the {@code executor} (the event thread in the viewer), once for
 * a burst of events: it is told when the events have stopped coming for {@link #QUIET_MILLIS} milliseconds. A folder that
 * can't be watched is just not watched; the user still has Refresh. The system property
 * {@value #PROPERTY_WATCH} = false turns the watching off.
 */
class FolderWatcher extends BaseObject implements AutoCloseable {

	static final String PROPERTY_WATCH = "parley.files.viewer.watch";
	static final long QUIET_MILLIS = 150;

	private final Consumer<FileSource> changed;
	private final Executor executor;
	/** registering lists the folder, which can be far away: not on the thread that asks */
	private final java.util.concurrent.ExecutorService starter = NamedExecutors.singleThread("FileViewerWatchStart");
	private Watch current;
	private boolean closed;

	/**
	 * @param changed told, with the folder, when it changed
	 * @param executor where {@code changed} runs
	 */
	FolderWatcher(Consumer<FileSource> changed, Executor executor) {
		this.changed = changed;
		this.executor = executor;
	}

	static boolean isEnabled() {
		return Boolean.parseBoolean(System.getProperty(PROPERTY_WATCH, "true"));
	}

	/** @return the folder that is watched, null for none */
	synchronized FileSource getFolder() {
		return current == null ? null : current.folder;
	}

	/**
	 * Watch this folder and no other; the same folder again changes nothing. Returns at once (the folder is registered
	 * in a thread of its own), and never throws.
	 */
	void watch(FileSource folder) {
		if( closed || !isEnabled() ) {
			return;
		}
		try {
			starter.execute(() -> switchTo(folder));
		} catch (java.util.concurrent.RejectedExecutionException e) {
			// closed
		}
	}

	private synchronized void switchTo(FileSource folder) {
		if( closed ) {
			return;
		}
		if( current != null && current.folder.equals(folder) && current.isRunning() ) {
			return;
		}
		stopCurrent();
		if( folder == null ) {
			return;
		}
		try {
			current = new Watch(folder);
		} catch (IOException | RuntimeException e) {
			logDebug("Not watching "+folder+": "+e);
		}
	}

	private void stopCurrent() {
		Watch w = current;
		current = null;
		if( w != null ) {
			w.stop();
		}
	}

	@Override
	public void close() {
		closed = true;
		starter.shutdownNow();
		synchronized (this) {
			stopCurrent();
		}
	}

	private final class Watch {
		final FileSource folder;
		private final FileSourceWatchService service;
		private final FileSourceWatchKey key;
		private volatile boolean running = true;

		Watch(FileSource folder) throws IOException {
			this.folder = folder;
			FileSourceWatchService s = folder.getFileSourceFactory().newWatchService();
			try {
				key = s.register(folder);
			} catch (IOException | RuntimeException e) {
				s.close();
				throw e;
			}
			service = s;
			NamedExecutors.start("FileViewerWatch", this::run);
		}

		boolean isRunning() {
			return running;
		}

		void stop() {
			running = false;
			try {
				service.close();
			} catch (IOException e) {
				// stopping
			}
		}

		private void run() {
			try {
				while( running ) {
					FileSourceWatchKey k = service.take();
					boolean any = take(k);
					// wait until it is quiet, then tell once
					while( running && any ) {
						FileSourceWatchKey more = service.poll(QUIET_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS);
						if( more == null ) {
							break;
						}
						take(more);
					}
					if( running && any ) {
						executor.execute(() -> {
							if( running ) {
								changed.accept(folder);
							}
						});
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (java.nio.file.ClosedWatchServiceException e) {
				// stopped
			} finally {
				running = false;
			}
		}

		/** @return whether the key had events; the key is read and reset */
		private boolean take(FileSourceWatchKey k) {
			List<FileSourceWatchEvent> events = k.pollEvents();
			k.reset();
			return !events.isEmpty();
		}
	}
}
