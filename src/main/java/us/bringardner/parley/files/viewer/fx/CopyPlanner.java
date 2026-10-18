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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.browse.DirectoryListing;
import us.bringardner.parley.files.viewer.fx.CopyQueue.Job;

/**
 * Works out what a copy has to do: makes the folders (they're quick, and the copy needs them),
 * and gives back a job for each file. Where a file would replace another, the {@link Resolver}
 * decides. It lists folders, so it must not run on the JavaFX thread.
 */
final class CopyPlanner {

	/** What to do about a file that's already there. */
	enum Answer { REPLACE, REPLACE_ALL, SKIP, SKIP_ALL, CANCEL }

	/** Decides for a file that already exists; may block (it's asked off the JavaFX thread). */
	interface Resolver {
		Answer resolve(FileSource existing);
	}

	/** Replace everything. */
	static final Resolver REPLACE_ALL = existing->Answer.REPLACE_ALL;

	/** The files to copy, and whether any folder was made. */
	static final class Plan {
		private final List<Job> jobs = new ArrayList<>();
		private boolean createdFolders;

		List<Job> getJobs() { return jobs; }
		boolean isCreatedFolders() { return createdFolders; }
	}

	private CopyPlanner() {
	}

	/**
	 * @throws CancellationException if the resolver canceled
	 * @throws IOException if something can't be copied (a folder into itself, a folder over a file)
	 */
	static Plan plan(List<FileSource> sources, FileSource dest, Resolver resolver) throws IOException {
		Plan plan = new Plan();
		Answer[] sticky = { null };
		for(FileSource f : sources) {
			planOne(f, dest, resolver, sticky, plan);
		}
		return plan;
	}

	private static void planOne(FileSource source, FileSource dir, Resolver resolver, Answer[] sticky, Plan plan) throws IOException {
		if( !source.exists()) {
			return;
		}
		FileSource parent = source.getParentFile();
		if( parent != null && sameFolder(parent, dir)) {
			// it's already there
			return;
		}
		FileSource target = dir.getChild(source.getName());
		if( source.isDirectory()) {
			if( DirectoryListing.samePath(source, dir) || source.isChildOfMine(dir)) {
				throw new IOException("Can't copy "+source.getAbsolutePath()+" into itself ("+dir.getAbsolutePath()+")");
			}
			if( target.exists() && !target.isDirectory()) {
				throw new IOException("Can't copy the folder "+source.getName()+": "+target.getAbsolutePath()+" is a file");
			}
			if( !target.exists()) {
				if( !target.mkdirs()) {
					throw new IOException("Can't create the folder "+target.getAbsolutePath());
				}
				plan.createdFolders = true;
			}
			FileSource[] kids = source.listFiles();
			if( kids != null ) {
				for(FileSource kid : kids) {
					planOne(kid, target, resolver, sticky, plan);
				}
			}
		} else {
			if( target.exists()) {
				if( target.isDirectory()) {
					throw new IOException("Can't copy "+source.getName()+": "+target.getAbsolutePath()+" is a folder");
				}
				Answer answer = sticky[0] != null ? sticky[0] : resolver.resolve(target);
				if( answer == Answer.CANCEL ) {
					throw new CancellationException("Copy canceled");
				}
				if( answer == Answer.REPLACE_ALL || answer == Answer.SKIP_ALL ) {
					sticky[0] = answer;
				}
				if( answer == Answer.SKIP || answer == Answer.SKIP_ALL ) {
					return;
				}
			}
			plan.jobs.add(new Job(source, target));
		}
	}

	private static boolean sameFolder(FileSource a, FileSource b) throws IOException {
		return a.getFileSourceFactory().isSameFileSystem(b.getFileSourceFactory())
				&& a.getCanonicalPath().equals(b.getCanonicalPath());
	}
}
