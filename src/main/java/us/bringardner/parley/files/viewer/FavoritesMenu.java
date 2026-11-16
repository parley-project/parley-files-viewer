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
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.browse.RecentFile;

/**
 * The Go &gt; Favorites menu: one item per favorite folder, then Add Current Folder and a
 * Remove submenu. The list is read each time the menu opens, so a favorite added or removed in
 * one window shows in every other.
 */
public class FavoritesMenu extends JMenu {

	private static final long serialVersionUID = 1L;

	private final transient Favorites favorites;
	private final transient Supplier<FileSource> current;
	private final transient Consumer<RecentFile> opener;
	private final transient Consumer<IOException> errors;

	/**
	 * @param favorites the list
	 * @param current the folder shown (null if none), which Add Current Folder adds
	 * @param opener shows a favorite
	 * @param errors told of a list that can't be saved
	 */
	public FavoritesMenu(Favorites favorites, Supplier<FileSource> current, Consumer<RecentFile> opener,
			Consumer<IOException> errors) {
		super("Favorites");
		this.favorites = favorites;
		this.current = current;
		this.opener = opener;
		this.errors = errors;
		addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent e) {
				build();
			}
			public void menuDeselected(MenuEvent e) {
			}
			public void menuCanceled(MenuEvent e) {
			}
		});
		build();
	}

	private void build() {
		removeAll();
		List<RecentFile> list = favorites.list();
		for(RecentFile entry : list) {
			JMenuItem item = new JMenuItem(entry.getLabel());
			item.addActionListener(e->opener.accept(entry));
			add(item);
		}
		if( !list.isEmpty() ) {
			addSeparator();
		}
		FileSource shown = current.get();
		JMenuItem add = new JMenuItem("Add Current Folder");
		add.setEnabled(shown != null);
		add.addActionListener(e->{
			try {
				favorites.add(shown);
			} catch (IOException ex) {
				errors.accept(ex);
			}
		});
		add(add);
		JMenu remove = new JMenu("Remove");
		remove.setEnabled(!list.isEmpty());
		for(RecentFile entry : list) {
			JMenuItem item = new JMenuItem(entry.getLabel());
			item.addActionListener(e->{
				try {
					favorites.remove(entry);
				} catch (IOException ex) {
					errors.accept(ex);
				}
			});
			remove.add(item);
		}
		add(remove);
	}
}
