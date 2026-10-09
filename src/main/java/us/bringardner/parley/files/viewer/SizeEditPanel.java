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

import javax.swing.JPanel;

import java.awt.FlowLayout;

import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingConstants;

public class SizeEditPanel extends JPanel {

	private static final long KB = 1024;
	private static final long MB = KB*KB;
	private static final long GB = MB*KB;
	
	private static final long serialVersionUID = 1L;
	private JTextField textField;
	private JComboBox<String> comboBox;

	public long getValue() {
		long ret = 0l;
		String tmp = textField.getText().trim();
		if(tmp.length() > 0 ) {
			try {
				ret = Long.parseLong(tmp);
				String type = comboBox.getSelectedItem().toString();
				if( type.equals("KB")) {
					ret *= KB;
				} else if( type.equals("MB")) {
					ret *= MB;
				} else if( type.equals("GB")) {
					ret *= GB;
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return ret;
	}
	/**
	 * Create the panel.
	 */
	public SizeEditPanel() {
		FlowLayout flowLayout = (FlowLayout) getLayout();
		flowLayout.setVgap(0);
		flowLayout.setHgap(0);
		flowLayout.setAlignment(FlowLayout.LEFT);
		
		textField = new JTextField();
		textField.setHorizontalAlignment(SwingConstants.RIGHT);
		textField.setText("");
		add(textField);
		textField.setColumns(10);
		
		comboBox = new JComboBox<String>();
		comboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"B", "KB", "MB", "GB"}));
		add(comboBox);
		setBackground(textField.getBackground());

	}

}
