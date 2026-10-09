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
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import us.bringardner.parley.files.FileSource;

public class SearchFileTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private List<FileSource> files = new ArrayList<FileSource>();

	public SearchFileTableModel() {
		super();
		//add(new FileProxy(new File("C:/MyTemp")));
	}

	@Override
	public int getColumnCount() {
		return 4;
	}

	@Override
	public int getRowCount() {
		return files.size();
	}

	public FileSource getFile(int row) {
		return files.get(row);
	}

	@Override
	public Object getValueAt(int row, int col) {
		FileSource f = files.get(row);
		Object ret = null;

		
		switch (col) {
		case 0:ret = f.getAbsolutePath();
		break;
		case 1:try {
				ret = new Date(f.lastModified());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		break;
		case 2:try {
				ret = f.isDirectory() ? "Dir" : "File";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		break;
		case 3:
			try {
				ret =  f.isDirectory() ? 0:f.length();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		default:
			break;
		}

		return ret;
	}


	@Override
	public Class<?> getColumnClass(int col) {
		Class<?> ret = null;
		switch (col) {
		case 0:	ret = String.class;
		case 1:	ret = Date.class;
		case 2:	ret = String.class;
		case 3:	ret = Long.class;
		default:
			break;
		}

		return ret;
	}

	@Override
	public String getColumnName(int col) {
		String ret = null;
		switch (col) {
		case 0:	ret = "Path";break;
		case 1:	ret = "Date";break;
		case 2:	ret = "Type";break;
		case 3:	ret = "Size";break;
		default:
			break;
		}

		return ret;
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}

	@Override
	public void setValueAt(Object newName, int row, int col) {

	}

	public void add(FileSource file) {
		int sz = files.size();
		files.add(file);
		fireTableRowsInserted(sz, sz);
	}

	public void clear() {
		
		if(files.size() > 0 ) {
			files.clear();
			fireTableStructureChanged();
		}
	}
}
