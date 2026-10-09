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
 * ~version~V000.01.50-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public class CopyFileTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private List<CopyTransaction> files = new ArrayList<CopyTransaction>();
	private static String [] fieldNames = {"Source","Destination","Start Time","Status"};
	
	public CopyFileTableModel() {
		super();
	}

	


	
	public int getColumnCount() {
		//  source dest startTime status
		return 4;
	}

	
	public int getRowCount() {
		return files.size();
	}

	
	
	public Object getValueAt(int row, int col) {
		CopyTransaction tx = files.get(row);
		String ret = null;

		switch (col) {
		case 0:ret = tx.getSource().getAbsolutePath();
		break;
		case 1:ret = tx.getDest().getAbsolutePath();
		break;
		case 2:ret = tx.getStartLabel();
		break;
		case 3: ret = tx.getStatusLabel();
			break;

		default:
			break;
		}
		if( ret.startsWith("//")) {
			System.out.println("Why");
		}
		return ret;
	}

	

	
	public String getColumnName(int col) {
		String ret = fieldNames[col];

		return ret;
	}

	
	public boolean isCellEditable(int row, int col) {
		if( col == 0 ) {
			return true;
		}
		return false;
	}




	public void addCopy(CopyTransaction tx) {
		int sz = files.size();
		files.add(tx);	
		
		fireTableRowsInserted(sz, sz);
	}




	public CopyTransaction getTxForRow(int idx) {
		return files.get(idx);
	}

	


}
