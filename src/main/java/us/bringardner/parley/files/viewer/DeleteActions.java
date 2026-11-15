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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;

/**
 * The viewer's Delete, for files and whole folders, done in the background.
 * <p>
 * It used to delete one file at a time, asking the file system first whether each was a folder.
 * Over SFTP every delete makes the file system forget the details its listings had read (any
 * change might have changed them), so each file cost three round trips to the server: a 400-file
 * folder took 40-50 seconds. Now:
 * <ol>
 * <li>the folders are walked first ({@link #plan}): each listing's entries are sorted into files,
 * links and folders straight after it is read, while what it read is fresh (no round trip each);</li>
 * <li>the files are deleted several at a time ({@link #workersFor}): over SFTP the connection has
 * a pool of channels for this; on a local disk too;</li>
 * <li>then the folders, the deepest first.</li>
 * </ol>
 * A link is deleted as a link: never followed. (Deleting a folder used to go into a link to a
 * folder in it and delete what the link's target held.)
 */
class DeleteActions {

	/** How many files are deleted at a time where the file system allows it. */
	static final int PARALLEL_DELETES = 8;

	private DeleteActions() {
	}

	/** What a Delete removes: the files and links first (any order), then the folders, each after what was in it. */
	static class Plan {
		final List<FileSource> files = new ArrayList<>();
		final List<FileSource> folders = new ArrayList<>();

		int size() {
			return files.size()+folders.size();
		}
	}

	/** Walks the targets: lists each folder once (as walking goes, before anything is deleted). */
	static Plan plan(List<FileSource> targets) throws IOException {
		Plan plan = new Plan();
		for(FileSource t : targets) {
			if( isFolder(t) ) {
				addFolder(t, plan);
			} else {
				plan.files.add(t);
			}
		}
		return plan;
	}

	/** A folder that is not a link (a link to a folder is deleted as a link). */
	private static boolean isFolder(FileSource f) throws IOException {
		return f.isDirectory() && f.getLinkedTo() == null;
	}

	private static void addFolder(FileSource folder, Plan plan) throws IOException {
		FileSource[] kids = folder.listFiles();
		List<FileSource> subfolders = new ArrayList<>();
		if( kids != null ) {
			// sorted now, from the listing just read; a subfolder's own listing comes after
			for(FileSource k : kids) {
				if( isFolder(k) ) {
					subfolders.add(k);
				} else {
					plan.files.add(k);
				}
			}
		}
		for(FileSource s : subfolders) {
			addFolder(s, plan);
		}
		plan.folders.add(folder);
	}

	/**
	 * How many files to delete at a time on factory's file system: {@link #PARALLEL_DELETES} for
	 * SFTP (each request takes a channel of its own from the connection's pool) and a local disk;
	 * one for the rest, whose factories are not made to be used from several threads at once.
	 */
	static int workersFor(FileSourceFactory factory) {
		String type = factory == null ? "" : factory.getTypeId();
		return "sftp".equals(type) || "fileproxy".equals(type) ? PARALLEL_DELETES : 1;
	}

	/**
	 * Deletes what plan holds, reporting to progress (the total first, then each item). Stops at
	 * the first that can't be deleted.
	 *
	 * @param workers how many files to delete at a time
	 * @throws IOException naming the first that couldn't be deleted
	 */
	static void delete(Plan plan, int workers, ArchiveActions.Progress progress) throws IOException {
		progress.total(plan.size());
		AtomicInteger done = new AtomicInteger();
		AtomicReference<IOException> failed = new AtomicReference<>();
		if( workers <= 1 || plan.files.size() <= 1 ) {
			for(FileSource f : plan.files) {
				deleteOne(f, done, progress);
			}
		} else {
			ExecutorService pool = Executors.newFixedThreadPool(Math.min(workers, plan.files.size()), r->{
				Thread t = new Thread(r, "viewer-delete");
				t.setDaemon(true);
				return t;
			});
			try {
				for(FileSource f : plan.files) {
					pool.execute(()->{
						if( failed.get() != null ) {
							return;   // stop at the first that fails
						}
						try {
							deleteOne(f, done, progress);
						} catch (IOException | RuntimeException e) {
							failed.compareAndSet(null, e instanceof IOException ? (IOException) e : new IOException(e));
						}
					});
				}
			} finally {
				pool.shutdown();
				try {
					while( !pool.awaitTermination(1, TimeUnit.MINUTES) ) {
						// (a slow server: keep waiting for the deletes under way)
					}
				} catch (InterruptedException e) {
					pool.shutdownNow();
					Thread.currentThread().interrupt();
					throw new IOException("Delete interrupted", e);
				}
			}
			if( failed.get() != null ) {
				throw failed.get();
			}
		}
		for(FileSource f : plan.folders) {
			deleteOne(f, done, progress);
		}
	}

	private static void deleteOne(FileSource f, AtomicInteger done, ArchiveActions.Progress progress) throws IOException {
		if( !f.delete() ) {
			throw new IOException("Could not delete "+f.getAbsolutePath());
		}
		progress.item(done.incrementAndGet(), f.getName());
	}
}
