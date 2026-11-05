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

/**
 * The files of one folder, for the viewer's table.
 * <p>
 * Each file's details (date, type, size, readable, link) are read once, when
 * the folder is loaded, and kept in a {@link Row}; painting the table only
 * reads them back. For a remote folder (SFTP) every detail can be a round trip
 * to the server, and the table paints each cell many times (scrolling,
 * hovering, selecting), so asking the file again on every paint made a
 * 40-file folder cost over a thousand requests per repaint.
 * <p>
 * A row's tooltip (its canonical path, which for SFTP takes a request per
 * element of the path) is worked out the first time the mouse rests on it.
 * <p>
 * {@link #load} does the reading and may be slow; call it off the event
 * thread and give the rows to {@link #FileTableModel(FileSource, List, boolean, boolean)}.
 */
public class FileTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	/** One file's details, as they were when the folder was loaded. */
	public static final class Row {
		final FileSource file;
		final String name;
		final long lastModified;
		final boolean directory;
		final long length;
		final boolean readable;
		final boolean link;
		/** Why the details couldn't be read, or null. */
		final String error;
		/** Worked out on first use, on the event thread. */
		private String toolTip;
		private Icon icon;

		Row(FileSource file) {
			this.file = file;
			this.name = file.getName().trim();
			long lm = 0, len = 0;
			boolean dir = false, rd = false, lk = false;
			String err = null;
			try {
				dir = file.isDirectory();
				lm = file.lastModified();
				len = dir ? -1 : file.length();
				lk = file.getLinkedTo() != null;
				rd = FileTableCellRenderer.isReadable(file);
			} catch (IOException e) {
				err = e.getMessage();
			}
			this.lastModified = lm;
			this.directory = dir;
			this.length = len;
			this.link = lk;
			this.readable = rd;
			this.error = err;
		}

		public FileSource getFile() {
			return file;
		}
	}

	private Row[] rows = new Row[0];
	private FileSource dir;
	private boolean showHidden = true;
	private boolean showExtention = true;

	/** Folders in the theme's colors (swing-widgets' FolderIcon), as in the JavaFX viewer. */
	public static final Icon dirIcon = new us.bringardner.swing.gradient.FolderIcon(20, us.bringardner.swing.gradient.FolderIcon.Kind.CLOSED);
	public static final Icon dirIconNoRead = new us.bringardner.swing.gradient.FolderIcon(20, us.bringardner.swing.gradient.FolderIcon.Kind.NOT_READABLE);
	//public static final Icon fileIcon = new ImageIcon(FileTableModel.class.getResource(FileSourceViewer.ICON_LOCATION+"/fileicon_8mm_10mm.png"));
	public static final Icon fileIcon = new ImageIcon(FileTableModel.class.getResource(FileSourceViewer.ICON_LOCATION+"/fileicon_20.png"));
	public static final Icon fileIconNoRead = new ImageIcon(FileTableModel.class.getResource(FileSourceViewer.ICON_LOCATION+"/fileicon_not_readable_20.png"));

	/** Lists 'dir' and reads its files' details now, on this thread. */
	public FileTableModel(FileSource root1,boolean showHidden,boolean showExtention) {
		this.showHidden = showHidden;
		this.showExtention = showExtention;
		setDir(root1);
	}

	/** Rows already read by {@link #load}; touches nothing on the server. */
	public FileTableModel(FileSource dir, List<Row> rows, boolean showHidden, boolean showExtention) {
		this.dir = dir;
		this.showHidden = showHidden;
		this.showExtention = showExtention;
		this.rows = rows.toArray(new Row[rows.size()]);
	}

	/**
	 * Reads the rows for 'dir': its files, sorted by name, without the hidden
	 * ones unless showHidden. Talks to the server; call it off the event thread.
	 *
	 * @param listing dir's files if they have just been listed (so they
	 *        aren't listed again), or null to list them here
	 */
	public static List<Row> load(FileSource dir, FileSource[] listing, boolean showHidden) throws IOException {
		FileSource[] tmp = listing;
		if( tmp == null ) {
			tmp = dir != null ? dir.listFiles() : null;
		}
		List<Row> ret = new ArrayList<>();
		if( tmp == null ) {
			return ret;
		}
		List<FileSource> list = new ArrayList<FileSource>();
		for (FileSource f : tmp) {
			if( showHidden || !isHidden(f) ) {
				list.add(f);
			}
		}
		Collections.sort(list, new Comparator<FileSource>() {
			public int compare(FileSource arg0, FileSource arg1) {
				return arg0.getName().compareToIgnoreCase(arg1.getName());
			}
		});
		for (FileSource f : list) {
			ret.add(new Row(f));
		}
		return ret;
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

	/** Lists 'dir' and reads its files' details now, on this thread. */
	public void setDir(FileSource dir) {
		this.dir = dir;
		try {
			List<Row> list = load(dir, null, showHidden);
			rows = list.toArray(new Row[list.size()]);
			fireTableDataChanged();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean isHidden(FileSource f) throws IOException {
		return f.isHidden() || f.getName().startsWith(".");
	}

	public int getColumnCount() {
		return colNames.length;
	}

	public int getRowCount() {
		return rows.length;
	}

	public FileSource getFile(int row) {
		return rows[row].file;
	}

	/**
	 * The tooltip for a row (model index): the link's target, or the file,
	 * as a canonical path. Worked out the first time it's asked for.
	 */
	public String getToolTip(int row) {
		if( row < 0 || row >= rows.length ) {
			return null;
		}
		Row r = rows[row];
		if( r.toolTip == null ) {
			String tip = "";
			try {
				FileSource l = r.link ? r.file.getLinkedTo() : null;
				tip = l != null ? l.getCanonicalPath() : r.file.getCanonicalPath();
			} catch (IOException e) {
				// no tooltip, as before
			}
			r.toolTip = tip;
		}
		return r.toolTip.isEmpty() ? null : r.toolTip;
	}

	public Object getValueAt(int row, int col) {
		Object ret = null;
		Row r = rows[row];
		if( r.error != null && col >= 2 ) {
			return r.error;
		}
		switch (col) {
		case 1:
			String name = r.name;
			if( !showExtention) {
				int idx = name.lastIndexOf(".");
				if( idx > 0 ) {
					name = name.substring(0, idx);
				}
			}
			ret = name;
			break;
		case 2:
			ret = new Date(r.lastModified);
			break;
		case 3:
			ret = r.link ? "Link" : r.directory ? "Dir" : "File";
			break;
		case 4:
			ret = r.directory ? -1 : r.length;
			break;
		case 0:
			ret = r.directory ?
					(r.readable ? dirIcon : dirIconNoRead)
					:
					(r.readable ? iconFor(r) : fileIconNoRead);
		default:
			break;
		}
		return ret;
	}

	private Icon iconFor(Row r) {
		if( r.icon == null ) {
			r.icon = getFileIconFor(r.file);
		}
		return r.icon;
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
			FileSource f = rows[row].file;
			if( !newName.equals(f.getName())) {
				try {
					// in its own folder, as the rename menu item does (not editable now, but kept right)
					FileRename.rename(f, newName.toString());
					fireTableCellUpdated(row, col);
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
