/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2026 <A href="http://bringardner.com/tony">Tony Bringardner</A>
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
 *	@author Tony Bringardner   
 */
package us.bringardner.parley.files.viewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import us.bringardner.swing.gradient.Theme;

/**
 * Paints the file table's column headers in the theme's header colors: the header color behind,
 * the header text color on it, with the sort arrow on the sorted column.
 * <p>
 * The look and feel's own header renderer (Aqua on macOS) paints its light, native background and
 * ignores the theme's, but still takes the theme's text color, so a theme with white header text
 * gave white text on a nearly white header. This renderer paints the whole cell itself, as the
 * theme designer's preview does. The colors are read from {@link Theme#current()} on every paint,
 * so a change of theme shows at once.
 * <p>
 * Every header cell is as tall as the taller of the sort arrow and a line of the table's font, plus
 * {@link #PADDING} above and below, whether or not its column is sorted, so the header keeps one
 * height (the table header takes the tallest cell's preferred height).
 */
class ThemedHeaderRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;

	/** Space above and below the header's text, in pixels. */
	static final int PADDING = 6;

	ThemedHeaderRenderer() {
		setOpaque(true);
		setHorizontalTextPosition(SwingConstants.LEADING);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		Theme theme = Theme.current();
		setText(value == null ? "" : value.toString());
		setBackground(theme.getHeader());
		setForeground(theme.getHeaderText());
		setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 1, theme.getAccent()),
				BorderFactory.createEmptyBorder(PADDING, 4, PADDING, 4)));
		// the table's font, so the header follows the font chosen in Settings as the rows do
		Font font = table.getFont();
		if( font != null ) {
			setFont(font.deriveFont(Font.BOLD));
		}
		setIcon(sortIcon(table, column));
		return this;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension size = super.getPreferredSize();
		int content = Math.max(getFontMetrics(getFont()).getHeight(), arrowHeight());
		// the matte border's 1 pixel line along the bottom, then the padding above and below
		size.height = Math.max(size.height, content + 2 * PADDING + 1);
		return size;
	}

	/** The height of the taller sort arrow, so a column keeps its height when it is sorted. */
	private static int arrowHeight() {
		int h = 0;
		for(String key : new String[] {"Table.ascendingSortIcon", "Table.descendingSortIcon"}) {
			Icon icon = UIManager.getIcon(key);
			if( icon != null ) {
				h = Math.max(h, icon.getIconHeight());
			}
		}
		return h;
	}

	/** The arrow for the column the table is sorted on, or null. */
	private static Icon sortIcon(JTable table, int column) {
		if( table.getRowSorter() == null ) {
			return null;
		}
		List<? extends SortKey> keys = table.getRowSorter().getSortKeys();
		if( !keys.isEmpty() && keys.get(0).getColumn() == table.convertColumnIndexToModel(column) ) {
			SortOrder order = keys.get(0).getSortOrder();
			if( order == SortOrder.ASCENDING ) {
				return UIManager.getIcon("Table.ascendingSortIcon");
			} else if( order == SortOrder.DESCENDING ) {
				return UIManager.getIcon("Table.descendingSortIcon");
			}
		}
		return null;
	}
}
