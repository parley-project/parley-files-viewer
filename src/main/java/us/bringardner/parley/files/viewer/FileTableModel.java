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
 * ~version~V000.01.39-V000.01.32-V000.01.V21_5-V000.01.21-V000.01.15-V000.01.12-V000.01.08-V000.01.05-V000.01.04-V000.01.00-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.viewer.IRegistry.CommandType;
import us.bringardner.parley.files.viewer.IRegistry.RegData;
import us.bringardner.swing.dialog.MessageDialog;

public class FileTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private FileSource[] files = new FileSource[0];
	private FileSource dir;

	private boolean showHidden = true;
	private boolean showExtention = true;
	public static final Icon dirIcon = new ImageIcon(FileTableModel.class.getResource(FileSourceViewer.ICON_LOCATION+"/folder_closed_20.png"));
	public static final Icon dirIconNoRead = new ImageIcon(FileTableModel.class.getResource(FileSourceViewer.ICON_LOCATION+"/folder_closed_not_readable_20.png"));

	//public static final Icon fileIcon = new ImageIcon(FileTableModel.class.getResource(FileSourceViewer.ICON_LOCATION+"/fileicon_8mm_10mm.png"));
	public static final Icon fileIcon = new ImageIcon(FileTableModel.class.getResource(FileSourceViewer.ICON_LOCATION+"/fileicon_20.png"));
	public static final Icon fileIconNoRead = new ImageIcon(FileTableModel.class.getResource(FileSourceViewer.ICON_LOCATION+"/fileicon_not_readable_20.png"));


	//  Only used by viewer.setup
	public FileTableModel(FileSource root1,boolean showHidden,boolean showExtention) {
		this.showHidden = showHidden;
		this.showExtention = showExtention;
		setDir(root1);
	}

	

	public boolean isShowHidden() {
		return showHidden;
	}

	public void setShowHidden(boolean showHidden) {
		this.showHidden = showHidden;
	}

	public boolean isShowExtention() {
		return showExtention;
	}

	public void setShowExtention(boolean showExtention) {
		this.showExtention = showExtention;				
	}

	public FileSource getDir() {
		return dir;
	}

	public void setDir(FileSource dir) {
		this.dir = dir;

		try {
			FileSource[] tmp = 
					dir != null?dir.listFiles():
						new FileSource[0];
			if( tmp == null ) {
				tmp = new FileSource[0];
			}
			List<FileSource> list = new ArrayList<FileSource>();
			for (FileSource f : tmp) {
				if( showHidden 
						|| 
						!isHidden(f) ) {
					list.add(f);
				}
			}

			Collections.sort(list, new Comparator<FileSource>() {

				public int compare(FileSource arg0, FileSource arg1) {
					return arg0.getName().compareToIgnoreCase(arg1.getName());
				}
			});
			files = list.toArray(new FileSource[list.size()]);
			fireTableDataChanged();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isHidden(FileSource f) throws IOException {
		return f.isHidden() || f.getName().startsWith(".");
	}


	public int getColumnCount() {
		return colNames.length;
	}


	public int getRowCount() {
		return files.length;
	}

	public FileSource getFile(int row) {
		return files[row];
	}


	public Object getValueAt(int row, int col) {
		Object ret = null;
		try {
			FileSource f = files[row];
			
			String name = f.getName().trim();

			FileSource l;
			boolean readable = FileTableCellRenderer.isReadable(f);

			switch (col) {
			case 1:

				if( !showExtention) {
					int idx = name.lastIndexOf(".");
					if( idx > 0 ) {
						name = name.substring(0, idx);
					}
				}
				ret = name;

				break;
			case 2:
				ret = new Date(f.lastModified());
				break;
			case 3:
				ret = f.isDirectory() ? "Dir" : "File";
				l = f.getLinkedTo();
				if( l != null ) {
					ret = "->"+l.getCanonicalPath();
					ret = "Link";
				}


				break;
			case 4:
				ret =  f.isDirectory() ? -1:f.length();
				break;

			case 0:

				ret = f.isDirectory()?
						readable? 
								dirIcon
								:
									dirIconNoRead
									:
										readable?
												getFileIconFor(f)
												:
													fileIconNoRead;				
			default:
				break;
			}
		} catch (IOException e) {
			e.printStackTrace();
			ret = e.getMessage();
		}

		return ret;
	}



	private Icon getFileIconFor(FileSource f) {
		Icon ret = fileIcon;
		List<RegData> list = IRegistry.getRegistry().getRegisteredHandler(f.getAbsolutePath(), CommandType.Any);
		if( list != null && list.size()>0) {
			try {
				Icon tmp = list.get(0).getIcon(16, 16);
				if( tmp != null ) {
					ret = tmp;
				}
			} catch (IOException e) {
			}
		}
		return ret;
	}



	public Class<?> getColumnClass(int col) {
		Class<?> ret = String.class;
		switch (col) {
		case 0: ret = Icon.class;break;
		case 1:	ret = String.class;break;
		case 2:	ret = Date.class;break;
		case 3:	ret = String.class;break;
		case 4:	ret = Long.class;break;

		default:
			break;
		}

		return ret;
	}

	private static String [] colNames = {"","Name","Date","Type","Size"};
	public String getColumnName(int col) {
		return colNames[col];
	}


	public boolean isCellEditable(int row, int col) {
		//  user can rename via pop up menu
		return false;
	}


	public void setValueAt(Object newName, int row, int col) {
		if( col == 1 ) {
			FileSource f = files[row];
			if( !newName.equals(f.getName())) {
				FileSource newFile;
				try {
					newFile = f.getFileSourceFactory().createFileSource(newName.toString());
					boolean ok = f.renameTo(newFile);
					if( !ok ) {
						MessageDialog.showErrorDialog("Could not rename "+f.getName()+" to "+newName, null);
						
					} else {
						fireTableCellUpdated(row, col);					
					}
				} catch (IOException e) {
					MessageDialog.showErrorDialog("Could not rename "+f.getName()+" to "+newName, e.toString());
				}

			}
		}
	}

	public void resetIFile() throws IOException {
		dir.refresh();
		setDir(dir);
		fireTableStructureChanged();
	}
}
