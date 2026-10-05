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
 * ~version~V000.01.18-
 * 
 * The viewer's version of BjlFileSystem's PropertyPanel, with a different look and feel.
 * It used to have the same full name as BjlFileSystem's class, so inside the viewer it
 * silently replaced it everywhere (BjlFileSystem's own dialogs included).
 *  
 */
package us.bringardner.io.filesource.viewer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import us.bringardner.io.filesource.FileSourceFactory;
import us.bringardner.io.filesource.IConnectionPropertiesEditor;
import us.bringardner.swing.PasswordPanel;
import us.bringardner.swing.TextFieldPanel;

public class PropertyPanel extends JPanel implements IConnectionPropertiesEditor{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Properties properties = new Properties();
	private Map<String,Component> map = new HashMap<String, Component>();

	private FileSourceFactory factory;


	public Properties getProperties() {
		//  get current values
		for (Entry<Object, Object> e : properties.entrySet()) {
			String nm = e.getKey().toString();
			Component fld = map.get(nm);
			//  The fields are the viewer's own components. This used to look only for JPasswordField
			//  and JTextField, which they aren't, so edits were never read back.
			if (fld instanceof PasswordPanel) {
				properties.setProperty(nm, ((PasswordPanel) fld).getPassword());
			} else if (fld instanceof TextFieldPanel) {
				properties.setProperty(nm, ((TextFieldPanel) fld).getText());
			} else if (fld instanceof JPasswordField) {
				properties.setProperty(nm, new String(((JPasswordField) fld).getPassword()));
			} else if (fld instanceof JTextField) {
				properties.setProperty(nm, ((JTextField) fld).getText());
			}
		}

		return properties;
	}



	public void setProperties(Properties properties) {
		Properties old = properties;
		if(properties == null ) {
			properties = new Properties();
		}

		this.properties = properties;
		removeAll();
		map.clear();
		for (Entry<Object, Object> e : properties.entrySet()) {
			final String name = e.getKey().toString();
			String val  = e.getValue().toString();
			if( old != null ) {
				val = old.getProperty(name,val);
			}
			String displayName = name;
			if( factory != null) {
				String id = factory.getTypeId();
				if( name.toLowerCase().startsWith(id.toLowerCase())) {
					displayName = name.substring(id.length());
				}
			}
			

			Component fld = new TextFieldPanel(displayName,val);

			//  Every secret the factory knows of (a key passphrase, a token ...), not only names with
			//  "password" in them, as BjlFileSystem's PropertyPanel does
			if( factory != null ? factory.isSecretProperty(name) : FileSourceFactory.looksLikeSecret(name)) {
				fld = new PasswordPanel(val);
				
			}
			fld.setPreferredSize(new Dimension(300, 30));
			map.put(name,fld);
			JPanel rowPanel = new JPanel();
			
			rowPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			rowPanel.add(new JLabel(displayName));
			rowPanel.add(fld);
			add(rowPanel);
		}
		int flds = getComponentCount();
		int h = flds * 50;
		setSize(360, h);
		
	}




	/**
	 * Create the panel.
	 */
	public PropertyPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}



	public void setProperties(FileSourceFactory factory) {
		this.factory = factory; 
		setProperties(factory.getConnectProperties());
	}

}
