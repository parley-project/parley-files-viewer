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
package us.bringardner.parley.files.viewer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.viewer.FileSourceViewer;
import us.bringardner.swing.menu.StandardMenus;

/**
 * The viewer's File, Edit and View menus.
 */
public class TestViewerMenus {

	/** File, Edit, View and Go ("-" a separator; Print only where the system prints). */
	public static final List<String> FILE = List.of("New Window", "New Remote Session...", "New Terminal", "New Folder...",
			"-", "Open", "Open With...", "Edit", "Edit With...", "Print", "-", "Start Here Next Time",
			"Set Start Directory...", "-", "Close");
	public static final List<String> EDIT = List.of("Copy", "Paste", "-", "Rename...", "Delete", "-", "Find...");
	public static final List<String> VIEW = List.of("Show Hidden Files", "Show File Extensions", "-", "Refresh");
	public static final List<String> GO = List.of("Back", "Forward", "Up", "-", "Recent Files", "-", "Go to Folder...");

	/** A menu's items as the lists above have them, without what StandardMenus adds off macOS. */
	public static List<String> names(List<String> items, boolean canPrint) {
		List<String> ret = new ArrayList<>(items);
		if( !canPrint ) {
			ret.remove("Print");
		}
		return ret;
	}

	@Test
	public void theFileEditAndViewMenus() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		FileSourceViewer viewer = new FileSourceViewer();
		try {
			List<List<String>> menus = new ArrayList<>();
			SwingUtilities.invokeAndWait(()->{
				viewer.setup(FileSourceFactory.fileProxyFactory, true);
				JMenuBar bar = viewer.getFrame().getJMenuBar();
				for(int i = 0; i < bar.getMenuCount() && i < 4; i++) {
					JMenu m = bar.getMenu(i);
					List<String> items = new ArrayList<>();
					for(int j = 0; j < m.getItemCount(); j++) {
						JMenuItem it = m.getItem(j);
						items.add(it == null ? "-" : it.getText());
					}
					menus.add(items);
				}
			});
			boolean canPrint = us.bringardner.parley.files.viewer.Printing.isAvailable();
			List<String> file = new ArrayList<>(menus.get(0));
			if( !StandardMenus.isMac() ) {
				// Settings... and Exit, which StandardMenus puts at the end of File off macOS
				file = file.subList(0, file.indexOf("Close") + 1);
			}
			assertEquals(names(FILE, canPrint), file);
			assertEquals(EDIT, menus.get(1));
			assertEquals(VIEW, menus.get(2));
			assertEquals(GO, menus.get(3));
		} finally {
			if( viewer.getFrame() != null ) {
				viewer.getFrame().dispose();
			}
		}
	}
}
