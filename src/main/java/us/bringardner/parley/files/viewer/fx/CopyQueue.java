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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceCopy;

/**
 * Copies files one after another, in the background, with no user interface: the engine of the
 * copy manager. Files are added in batches (see {@link CopyPlanner}); each is a {@link Job} that
 * can be watched, canceled, and kept as history. The queue can be paused, and every job stopped.
 * <p>
 * Listeners are called on the copying thread, and often (progress), so a user interface must
 * not do much in them: ask for an update later and return.
 */
final class CopyQueue {

	enum State { WAITING, RUNNING, DONE, CANCELED, FAILED }

	/** One file to copy. */
	static final class Job {
		private final FileSource source;
		private final FileSource dest;
		private volatile State state = State.WAITING;
		private volatile long expected;
		private volatile long copied;
		private volatile long startTime;
		private volatile long endTime;
		private volatile Throwable error;
		private volatile boolean cancelRequested;

		Job(FileSource source, FileSource dest) {
			this.source = source;
			this.dest = dest;
		}

		FileSource getSource() { return source; }
		FileSource getDest() { return dest; }
		State getState() { return state; }
		/** The size of the source, once started. */
		long getExpected() { return expected; }
		long getCopied() { return copied; }
		long getStartTime() { return startTime; }
		long getEndTime() { return endTime; }
		Throwable getError() { return error; }

		/** 0 to 1; a file with nothing in it is done as soon as it's started. */
		double getFraction() {
			if( state == State.DONE ) {
				return 1;
			}
			return expected <= 0 ? 0 : Math.min(1, (double) copied / expected);
		}

		boolean isFinished() {
			return state == State.DONE || state == State.CANCELED || state == State.FAILED;
		}
	}

	interface Listener {
		/** A job was added, started, finished or made progress. */
		void changed();

		/**
		 * The queue has nothing left to do.
		 *
		 * @param foldersMade true if folders were made since it was last idle, so views of the
		 *        folder tree are out of date
		 */
		void idle(boolean foldersMade);
	}

	private static final CopyQueue SHARED = new CopyQueue();

	/** The queue every window uses, so a copy goes on whichever window it was started from. */
	static CopyQueue shared() {
		return SHARED;
	}

	private final Object lock = new Object();
	private final List<Job> jobs = new ArrayList<>();
	private final Deque<Job> waiting = new ArrayDeque<>();
	private final List<Listener> listeners = new CopyOnWriteArrayList<>();
	private int batchStart;
	private boolean working;
	private volatile boolean paused;
	private volatile Job current;
	private volatile boolean foldersMade;

	void addListener(Listener l) {
		listeners.add(l);
	}

	void removeListener(Listener l) {
		listeners.remove(l);
	}

	/** Adds the jobs to the end of the queue and starts copying if it isn't already. */
	void enqueue(List<Job> added) {
		if( added.isEmpty()) {
			return;
		}
		synchronized (lock) {
			if( !working && waiting.isEmpty()) {
				// a new batch: the overall progress starts again
				batchStart = jobs.size();
			}
			jobs.addAll(added);
			waiting.addAll(added);
			if( !working ) {
				working = true;
				Thread t = new Thread(this::work, "parley-viewer-fx-copy");
				t.setDaemon(true);
				t.start();
			}
		}
		fireChanged();
	}

	/**
	 * Plans a copy off the calling thread, then queues it.
	 *
	 * @param onPlanned told how many files were queued and whether folders were made (on the
	 *        planning thread); null for nothing
	 * @param onError told why planning failed (a cancel isn't an error), on the planning thread
	 */
	void copy(List<FileSource> sources, FileSource dest, CopyPlanner.Resolver resolver,
			Consumer<CopyPlanner.Plan> onPlanned, Consumer<Throwable> onError) {
		List<FileSource> copy = new ArrayList<>(sources);
		TransferWindow.EXECUTOR.execute(()->{
			try {
				CopyPlanner.Plan plan = CopyPlanner.plan(copy, dest, resolver);
				if( onPlanned != null ) {
					onPlanned.accept(plan);
				}
				if( plan.isCreatedFolders()) {
					foldersMade = true;
				}
				enqueue(plan.getJobs());
				if( plan.getJobs().isEmpty()) {
					// nothing to copy, but folders may have been made
					fireIdle();
				}
			} catch (java.util.concurrent.CancellationException e) {
				// the user stopped it
			} catch (IOException | RuntimeException e) {
				if( onError != null ) {
					onError.accept(e);
				}
			}
		});
	}

	// ---------------------------------------------------------------- control

	boolean isPaused() {
		return paused;
	}

	void setPaused(boolean paused) {
		this.paused = paused;
		fireChanged();
	}

	/** Stops the file being copied and drops every file waiting. */
	void cancelAll() {
		List<Job> dropped;
		synchronized (lock) {
			dropped = new ArrayList<>(waiting);
			waiting.clear();
		}
		long now = System.currentTimeMillis();
		for(Job j : dropped) {
			j.cancelRequested = true;
			j.endTime = now;
			j.state = State.CANCELED;
		}
		Job c = current;
		if( c != null ) {
			c.cancelRequested = true;
		}
		fireChanged();
	}

	/** Stops one job: a waiting one is dropped, the running one is stopped. */
	void cancel(Job job) {
		synchronized (lock) {
			if( waiting.remove(job)) {
				job.cancelRequested = true;
				job.endTime = System.currentTimeMillis();
				job.state = State.CANCELED;
			} else {
				job.cancelRequested = true;
			}
		}
		fireChanged();
	}

	/** Stops the file being copied and goes on with the next. */
	void cancelCurrent() {
		Job c = current;
		if( c != null ) {
			cancel(c);
		}
	}

	/** Forgets the jobs that are finished. */
	void clearHistory() {
		synchronized (lock) {
			int removedBefore = 0;
			for(int i=0; i < batchStart && i < jobs.size(); i++) {
				if( jobs.get(i).isFinished()) {
					removedBefore++;
				}
			}
			jobs.removeIf(Job::isFinished);
			batchStart = Math.max(0, batchStart-removedBefore);
		}
		fireChanged();
	}

	// ---------------------------------------------------------------- what's there

	/** Every job still remembered, oldest first. */
	List<Job> getJobs() {
		synchronized (lock) {
			return new ArrayList<>(jobs);
		}
	}

	/** The job being copied, or null. */
	Job getCurrent() {
		return current;
	}

	/** True while there is anything running or waiting. */
	boolean isBusy() {
		synchronized (lock) {
			return working;
		}
	}

	/** How many files the current batch has, finished or not. */
	int getBatchTotal() {
		synchronized (lock) {
			return Math.max(0, jobs.size()-batchStart);
		}
	}

	/** How many files of the current batch are finished. */
	int getBatchDone() {
		synchronized (lock) {
			int n = 0;
			for(int i=batchStart; i < jobs.size(); i++) {
				if( jobs.get(i).isFinished()) {
					n++;
				}
			}
			return n;
		}
	}

	/** How many files of the current batch failed. */
	int getBatchFailed() {
		synchronized (lock) {
			int n = 0;
			for(int i=batchStart; i < jobs.size(); i++) {
				if( jobs.get(i).state == State.FAILED ) {
					n++;
				}
			}
			return n;
		}
	}

	// ---------------------------------------------------------------- the worker

	private void work() {
		while( true ) {
			Job job;
			synchronized (lock) {
				job = waiting.poll();
				if( job == null ) {
					working = false;
					current = null;
					break;
				}
				current = job;
			}
			run(job);
		}
		fireChanged();
		fireIdle();
	}

	/**
	 * Copies one job. A file already at the destination is replaced only once the copy is
	 * complete: the copy is written to a hidden name next to it and then renamed over it, so a
	 * failed or canceled copy leaves the old file as it was. Only what this copy wrote is ever
	 * deleted.
	 */
	private void run(Job job) {
		if( job.cancelRequested ) {
			job.state = State.CANCELED;
			job.endTime = System.currentTimeMillis();
			fireChanged();
			return;
		}
		job.startTime = System.currentTimeMillis();
		job.state = State.RUNNING;
		fireChanged();
		FileSource written = null;
		boolean partial = false;
		try {
			job.expected = job.source.length();
			int block = FileSourceCopy.blockSizeFor(job.source, job.dest);
			byte[] buffer = new byte[block];
			boolean replacing = job.dest.exists();
			written = replacing ? tempFor(job.dest) : job.dest;
			long last = 0;
			try( InputStream in = job.source.getInputStream(FileSourceCopy.optionsFor(block))) {
				try( OutputStream out = written.getOutputStream(false, FileSourceCopy.optionsFor(block))) {
					partial = true;
					long done = 0;
					while( !job.cancelRequested ) {
						while( paused && !job.cancelRequested ) {
							Thread.sleep(20);
						}
						int got = in.read(buffer);
						if( got < 0 ) {
							break;
						}
						out.write(buffer, 0, got);
						done += got;
						job.copied = done;
						long now = System.currentTimeMillis();
						if( now-last > 100 ) {
							last = now;
							fireChanged();
						}
					}
				}
			}
			if( job.cancelRequested ) {
				job.state = State.CANCELED;
			} else {
				if( replacing && !written.renameTo(job.dest, true)) {
					throw new IOException("Can't replace "+job.dest.getAbsolutePath());
				}
				partial = false;
				job.state = State.DONE;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			job.state = State.CANCELED;
		} catch (IOException | RuntimeException e) {
			job.error = e;
			job.state = State.FAILED;
		}
		if( partial && written != null ) {
			// only the start of the file (never a file that was there before)
			try {
				written.delete();
			} catch (IOException | RuntimeException e) {
				// nothing more to do
			}
		}
		job.endTime = System.currentTimeMillis();
		fireChanged();
	}

	/** A hidden name next to {@code target} for a copy that will replace it. */
	static FileSource tempFor(FileSource target) throws IOException {
		FileSource parent = target.getParentFile();
		if( parent == null ) {
			throw new IOException("Can't replace "+target.getAbsolutePath()+": it has no parent directory");
		}
		return parent.getChild("."+target.getName()+"."+Long.toHexString(System.nanoTime())+".copying");
	}

	private void fireChanged() {
		for(Listener l : listeners) {
			l.changed();
		}
	}

	private void fireIdle() {
		boolean made = foldersMade;
		foldersMade = false;
		for(Listener l : listeners) {
			l.idle(made);
		}
	}
}
