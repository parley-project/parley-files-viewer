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
import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.browse.DirectoryListing;
import us.bringardner.parley.files.fx.FolderTree;

/**
 * Makes a {@link FolderTree} show a folder: expands each folder on the way down from the top
 * and selects it. A folder's children are listed in the background the first time it's expanded,
 * so each step waits for its listing. A newer request replaces one still waiting. A folder the
 * tree doesn't show (hidden, or not under a top folder) is left alone.
 * <p>
 * Selecting is done with {@link #isSelecting()} true, so a selection listener can tell it from
 * the user's. Use it on the JavaFX application thread.
 */
final class TreeFollower {

	private final FolderTree tree;
	private int request;
	private boolean selecting;

	TreeFollower(FolderTree tree) {
		this.tree = tree;
	}

	/** True while the tree is being selected by this follower rather than by the user. */
	boolean isSelecting() {
		return selecting;
	}

	/** Expands down to dir and selects it. */
	void reveal(FileSource dir) {
		request++;
		FileSource current = tree.getSelectedFolder();
		if( dir == null || (current != null && DirectoryListing.samePath(current, dir))) {
			return;
		}
		List<FileSource> path;
		try {
			path = DirectoryListing.ancestors(dir);
		} catch (IOException e) {
			return;
		}
		// the tree starts at its top folders (a file system's roots, say, or favorites), which
		// may be below the top of the path: start at the first folder on the path that is one
		int start = 0;
		search:
		for(; start < path.size(); start++) {
			for(TreeItem<FileSource> top : tree.getRoot().getChildren()) {
				if( top.getValue() != null && DirectoryListing.samePath(top.getValue(), path.get(start))) {
					break search;
				}
			}
		}
		if( start < path.size()) {
			step(tree.getRoot(), path, start, request);
		}
	}

	private void step(TreeItem<FileSource> parent, List<FileSource> path, int idx, int id) {
		if( id != request ) {
			return;
		}
		TreeItem<FileSource> match = null;
		for(TreeItem<FileSource> kid : parent.getChildren()) {
			if( kid.getValue() != null && DirectoryListing.samePath(kid.getValue(), path.get(idx))) {
				match = kid;
				break;
			}
		}
		if( match != null ) {
			if( idx == path.size()-1 ) {
				select(match);
			} else {
				match.setExpanded(true);
				step(match, path, idx+1, id);
			}
			return;
		}
		if( isUnlisted(parent)) {
			// the listing is on its way: its children replace the placeholder
			ListChangeListener<TreeItem<FileSource>> once = new ListChangeListener<TreeItem<FileSource>>() {
				@Override
				public void onChanged(Change<? extends TreeItem<FileSource>> c) {
					parent.getChildren().removeListener(this);
					step(parent, path, idx, id);
				}
			};
			parent.getChildren().addListener(once);
			parent.setExpanded(true);
		}
	}

	/** True if the item still holds only the placeholder shown before its folder is listed. */
	private static boolean isUnlisted(TreeItem<FileSource> item) {
		return item.getValue() != null && item.getChildren().size() == 1 && item.getChildren().get(0).getValue() == null;
	}

	private void select(TreeItem<FileSource> item) {
		selecting = true;
		try {
			tree.getSelectionModel().select(item);
			int row = tree.getRow(item);
			if( row >= 0 ) {
				tree.scrollTo(row);
			}
		} finally {
			selecting = false;
		}
	}
}
