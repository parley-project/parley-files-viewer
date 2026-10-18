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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import us.bringardner.parley.files.FileSource;

/**
 * Looks through a folder and everything below it for the files that match, with no user
 * interface. A folder that can't be listed is counted and skipped, so one unreadable folder
 * doesn't end the search.
 */
final class FileSearch {

	private final SearchCriteria criteria;
	private final BooleanSupplier canceled;
	private final Consumer<FileSource> found;
	private int skipped;
	private int examined;

	FileSearch(SearchCriteria criteria, BooleanSupplier canceled, Consumer<FileSource> found) {
		this.criteria = criteria;
		this.canceled = canceled;
		this.found = found;
	}

	/** Searches dir, below it too. Returns when done or canceled. */
	void run(FileSource dir) {
		if( canceled.getAsBoolean()) {
			return;
		}
		FileSource[] list;
		try {
			list = dir.listFiles();
		} catch (IOException | RuntimeException e) {
			skipped++;
			return;
		}
		if( list == null ) {
			skipped++;
			return;
		}
		for(FileSource f : list) {
			if( canceled.getAsBoolean()) {
				return;
			}
			examined++;
			try {
				if( criteria.matches(f)) {
					found.accept(f);
				}
			} catch (IOException | RuntimeException e) {
				skipped++;
			}
		}
		for(FileSource f : list) {
			if( canceled.getAsBoolean()) {
				return;
			}
			try {
				if( f.isDirectory()) {
					run(f);
				}
			} catch (IOException | RuntimeException e) {
				skipped++;
			}
		}
	}

	/** How many folders or files couldn't be read. */
	int getSkipped() {
		return skipped;
	}

	/** How many files and folders were looked at. */
	int getExamined() {
		return examined;
	}
}
