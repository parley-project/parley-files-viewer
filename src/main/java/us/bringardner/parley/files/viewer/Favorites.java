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
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.browse.RecentFile;

/**
 * The folders the user keeps as favorites, in the order they were added, saved with
 * java.util.prefs.Preferences (one entry per key, so the list has no size limit).
 * <p>
 * An entry is a {@link RecentFile}: it keeps its file system's connection properties so a
 * remote favorite can be opened again, and never saves a secret value. Two entries are the
 * same favorite when their saved forms match, so the same path on two servers is two favorites.
 */
public class Favorites {

	/** The preferences node, under the one given, that holds the list. */
	static final String NODE = "Favorites";

	private final Preferences node;

	/** Saves the list in a child of the given preferences node. */
	public Favorites(Preferences prefs) {
		this.node = prefs.node(NODE);
	}

	/** @return the favorites, in the order they were added. A saved entry that can't be read is left out. */
	public synchronized List<RecentFile> list() {
		List<RecentFile> ret = new ArrayList<>();
		for(int idx = 0; ; idx++) {
			String line = node.get(key(idx), null);
			if( line == null ) {
				break;
			}
			try {
				ret.add(new RecentFile(line));
			} catch (IllegalArgumentException e) {
				// not an entry: skipped
			}
		}
		return ret;
	}

	/** @return true if the folder is a favorite */
	public boolean contains(FileSource dir) throws IOException {
		return indexOf(list(), new RecentFile(dir)) >= 0;
	}

	/** Adds a folder to the end of the list, unless it is already there. */
	public synchronized void add(FileSource dir) throws IOException {
		RecentFile entry = new RecentFile(dir);
		List<RecentFile> list = list();
		if( indexOf(list, entry) < 0 ) {
			list.add(entry);
			save(list);
		}
	}

	/** Removes a folder from the list. */
	public synchronized void remove(FileSource dir) throws IOException {
		remove(new RecentFile(dir));
	}

	/** Removes an entry from the list. */
	public synchronized void remove(RecentFile entry) throws IOException {
		List<RecentFile> list = list();
		int at = indexOf(list, entry);
		if( at >= 0 ) {
			list.remove(at);
			save(list);
		}
	}

	private static int indexOf(List<RecentFile> list, RecentFile entry) {
		String saved = entry.toString();
		for(int idx = 0; idx < list.size(); idx++) {
			if( list.get(idx).toString().equals(saved) ) {
				return idx;
			}
		}
		return -1;
	}

	private void save(List<RecentFile> list) throws IOException {
		try {
			node.clear();
			for(int idx = 0; idx < list.size(); idx++) {
				node.put(key(idx), list.get(idx).toString());
			}
			node.flush();
		} catch (BackingStoreException e) {
			throw new IOException("Can't save the favorites", e);
		}
	}

	private static String key(int idx) {
		return "f" + idx;
	}
}
