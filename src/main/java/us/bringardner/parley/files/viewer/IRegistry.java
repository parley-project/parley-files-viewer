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
 * ~version~V000.01.10-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import us.bringardner.parley.files.viewer.registry.MacRegistry;
import us.bringardner.parley.files.viewer.registry.WindowsRegistry;

/**
 * The sole purpose of this interface is to identify an executable that can 'Edit'
 * a particular file.  
 * 
 * @author Tony Bringardner
 *
 */
public interface IRegistry {
	public enum CommandType{Any,Editor,Viewer,None}
	
	public abstract class RegData {
		public String name;
		public String path;
		public abstract List<Icon> getIcons() throws IOException;
		public abstract Icon getIcon(int width, int height) throws IOException;
		public abstract String getCommand();

		/**
		 * The command that opens a file with this application, one argument to a string (a
		 * ProcessBuilder takes a single string as the name of the program, so the program, its
		 * options and the file's path, which may have spaces, cannot be one).
		 */
		public List<String> getCommandLine(String file) {
			List<String> ret = splitCommand(getCommand());
			ret.add(file);
			return ret;
		}

		/** Splits a command line into its arguments, where a double quote holds spaces. */
		public static List<String> splitCommand(String command) {
			List<String> ret = new ArrayList<>();
			StringBuilder b = new StringBuilder();
			boolean quoted = false;
			boolean any = false;
			for(int idx = 0; idx < command.length(); idx++) {
				char c = command.charAt(idx);
				if( c == '"' ) {
					quoted = !quoted;
					any = true;
				} else if( Character.isWhitespace(c) && !quoted ) {
					if( any ) {
						ret.add(b.toString());
						b.setLength(0);
						any = false;
					}
				} else {
					b.append(c);
					any = true;
				}
			}
			if( any ) {
				ret.add(b.toString());
			}
			return ret;
		}

		public RegData(String name, String path) {
			this.name= name;
			this.path = path;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof RegData) {
				RegData rd = (RegData) obj;
				return rd.path.equals(path);
			}
			return false;
		}
	}

	static IRegistry registry = System.getProperty("os.name").startsWith("Mac")?new MacRegistry():
		System.getProperty("os.name").startsWith("Win")?new WindowsRegistry():
			new IRegistry() {
			List<IRegistry.RegData> empty = new ArrayList<>();
			
			@Override
			public List<RegData> getRegisteredHandler(String path,CommandType type) {
				return empty;
			}
		};
	
	public static IRegistry getRegistry() {
		
		return registry;
	}

	List<RegData> getRegisteredHandler(String path,CommandType type);

	
}
