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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.SwingWorker;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import us.bringardner.parley.files.FileSource;

/**
 * One folder of the path bar: its name, and a drop-down of the other
 * folders beside it. The folders are listed when the drop-down is first
 * opened (see FileComboBoxPanel), in the background. Each folder of the path
 * used to be listed as soon as the path bar was made, every time a folder was
 * selected: a listing per level of the path, which for a remote folder is a
 * round trip to the server or more each.
 */
public class FileComboBoxModel implements ComboBoxModel {

	List<ListDataListener> listners = new ArrayList<ListDataListener>();
	Object selected ;
	private FileSource dir;
	/** dir's sub folders, once listed; event thread only. */
	private List<FileSource> list = new ArrayList<FileSource>();
	private ActionListener al;
	private boolean loading;
	private boolean loaded;

	public FileComboBoxModel(FileSource dir, String selectedName, ActionListener al) {
		this.dir = dir;
		this.al = al;
		selected = selectedName;
	}

	/** Whether the sub folders have been listed. */
	public boolean isLoaded() {
		return loaded;
	}

	/**
	 * Lists the sub folders in the background, once; then, on the event
	 * thread, adds them and runs 'then'. Event thread.
	 */
	public void load(Runnable then) {
		if( loaded || loading ) {
			return;
		}
		loading = true;
		new SwingWorker<List<FileSource>, Object>() {
			@Override
			protected List<FileSource> doInBackground() throws Exception {
				List<FileSource> ret = new ArrayList<>();
				FileSource[] list2 = dir.listFiles();
				if ( list2 != null ) {
					for (FileSource f : list2) {
						if(f.isDirectory()) {
							ret.add(f);
						}
					}
				}
				return ret;
			}

			@Override
			protected void done() {
				loading = false;
				loaded = true;
				try {
					list = get();
				} catch (Exception e) {
					// as before: no folders to choose from
				}
				if( list.size() > 0 ) {
					ListDataEvent event = new ListDataEvent(FileComboBoxModel.this, ListDataEvent.INTERVAL_ADDED, 0, list.size()-1) ;
					for (ListDataListener l : new ArrayList<>(listners)) {
						l.intervalAdded(event);
					}
				}
				if( then != null ) {
					then.run();
				}
			}
		}.execute();
	}

	public void removeListDataListener(ListDataListener arg0) {
		listners.remove(arg0);
	}

	public int getSize() {
		return list.size();
	}

	public Object getElementAt(int index) {
		return list.get(index).getName();
	}

	public void addListDataListener(ListDataListener arg0) {
		listners.add(arg0);							
	}

	/** A folder chosen from the drop-down: the one listed with that name (it used to list the folder again to find it). */
	public void setSelectedItem(Object arg0) {
		selected = arg0;				
		if( al != null && arg0 != null ) {
			String nm = arg0.toString();
			for(FileSource f : list) {
				if( f.getName().equals(nm)) {
					al.actionPerformed(new ActionEvent(f, 0, "Selected"));
					break;
				}
			}
		}
	}

	public Object getSelectedItem() {
		return selected;
	}

}
