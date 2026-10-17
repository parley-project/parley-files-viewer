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
 * ~version~V000.01.28-V000.01.26-V000.01.21-V000.01.20-V000.01.17-V000.01.16-V000.01.15-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import us.bringardner.swing.gradient.GradientColors;
import java.awt.Component;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import us.bringardner.parley.files.FileSource;

public class FileTableCellRenderer implements TableCellRenderer {
	public static long KB = 1024;
	public static long MB = KB * KB;
	public static long GB = MB * KB;
	public static long TB = GB * KB;

	private SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");

	  JLabel comp = new JLabel();
      String val= new String();
      
		

	public FileTableCellRenderer() {
		comp.setOpaque(true);
		
	}

	public static boolean isReadable(FileSource dir) {
		boolean ret = true;
		try {
			ret = dir.canRead();
			if( ret) {
				FileSource link = dir.getLinkedTo();
				if( link != null) {
					ret = !link.isChildOfMine(dir);				
				}
			}
		} catch (Throwable e) {
		}
		
		return ret;
	}

	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
		comp.setFont(javax.swing.UIManager.getDefaults().getFont("Label.font"));
		comp.setHorizontalAlignment(SwingConstants.LEFT);
		comp.setIcon(null);
		comp.setText("");
		comp.setToolTipText("");
		comp.setEnabled(true);
		
		if (value instanceof Icon) {
			comp.setIcon((Icon)value);
		} else if (value instanceof Date) {
			comp.setText(fmt.format((Date) value));			
		} else if (value instanceof Long) {
			comp.setText(fmt(((Long)value).longValue()));
			comp.setHorizontalAlignment(SwingConstants.RIGHT);	
		} else {
			if( value == null ) {
				value = "null";
			}
			comp.setText(value.toString());
		}
		
		if( isSelected ) {
			comp.setBackground(GradientColors.getStart());
		}  else {
			comp.setBackground(FileSourceViewer.TRANSPARENT);
		}
		
		
		TableModel tm = table.getModel();
		if (tm instanceof FileTableModel) {
			FileTableModel ftm = (FileTableModel) tm;
			FileSource f = ftm.getFile(table.convertRowIndexToModel(row));
			
			try {
				FileSource l = f.getLinkedTo();
				if( l != null ) {
					comp.setToolTipText(l.getCanonicalPath());
				} else {
					comp.setToolTipText(f.getCanonicalPath());
				}
			} catch (IOException e) {
			}						
		}
		
		return comp;
	}

	
	private String fmt(long val) {
		String ret = null;
		String lb = null;

		if( val < 0 ) {
			ret = "";
			lb = "";
		} else if( val > TB) {
		
			ret = ""+((double)val / (double)TB);
			lb = " TB";
		} else if( val > GB) {
			ret = ""+((double)val / (double)GB);
			lb = " GB";
		} else if( val > MB) {
			ret = ""+((double)val / (double)MB);
			lb = " MB";
		} else if( val > KB) {
			ret = ""+((double)val / (double)KB);
			lb = " KB";
		} else {
			ret = ""+val;
			lb = "";
		}
		int idx = ret.indexOf('.');
		if( idx > 0 && ret.length() > idx+3) {
			ret = ret.substring(0,idx+3);
		}

		return ret+lb;

	}
	
	
}
