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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import us.bringardner.parley.files.FileSource;

@SuppressWarnings("rawtypes")
public class FileComboBoxModel implements ComboBoxModel {

	List<ListDataListener> listners = new ArrayList<ListDataListener>();
	Object selected ;
	private FileSource dir;
	private List<FileSource> list = new ArrayList<FileSource>();
	private ActionListener al;

	public FileComboBoxModel(FileSource dir, String selectedName, ActionListener al) {
		this.dir = dir;
		this.al = al;
		selected = selectedName;

		new Thread(new Runnable() {

			
			public void run() {
				try {
					final FileSource[] list2 = FileComboBoxModel.this.dir.listFiles();
					if ( list2 != null &&  list2.length > 0 ) {
						for (FileSource f : list2) {
							if(f.isDirectory()) {
								FileComboBoxModel.this.list.add(f);
							}
						}
						if( FileComboBoxModel.this.list.size() > 0 ) {
							ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, 0, FileComboBoxModel.this.list.size()-1) ;
							for (ListDataListener l : listners) {
								l.intervalAdded(event);
							}
						}
					}
				} catch(Throwable e) {

				}
			}
		}).start();

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

	
	public void setSelectedItem(Object arg0) {
		selected = arg0;				
		if( al != null ) {
			try {
				String nm = arg0.toString();
				for(FileSource f : dir.listFiles()) {
					if( f.getName().equals(nm)) {
						al.actionPerformed(new ActionEvent(f, 0, "Selected"));
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	
	public Object getSelectedItem() {
		return selected;
	}

}
