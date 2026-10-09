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
package us.bringardner.parley.files.viewer.registry;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import us.bringardner.parley.files.viewer.IRegistry;

public class WindowsRegistry implements IRegistry {

	public List<IRegistry.RegData> getRegisteredEditor(String file) {
		List<IRegistry.RegData> list = getRegisteredEditor2(file,"edit");
		list.addAll(getRegisteredEditor1(file,"edit"));
		if(list.size() == 0 ) {
			list = getRegisteredEditor2(file,"open");
			list.addAll(getRegisteredEditor1(file,"open"));			
		}
		
		List<IRegistry.RegData> ret = new ArrayList<>();
		/*
		for (IRegistry.RegData tmp : list) {
			String tmp2 = macro(tmp.path);
			if( !ret.contains(tmp2) ) {
				// TODO:  ret.add(tmp2);
			}
		}
		*/
		
		return ret;
	}
	@SuppressWarnings("unused")
	private String macro(String tmp) {
		int idx = tmp.indexOf('%');
		while(idx >=0) {
			int idx2 = tmp.indexOf('%', idx+1);
			if( idx2 < idx) {
				break;// error ??
			}
			String nm = tmp.substring(idx+1,idx2);
			String nm2 = System.getenv(nm);
			if(nm2 == null ) {
				break; // Error ??
			}
			String left = "";
			if( idx > 0 ) {
				left = tmp.substring(0,idx);
			}
			String right = tmp.substring(idx2+1);
			tmp = left+nm2+right;
			idx = tmp.indexOf('%');
		}
		
		return tmp;
	}
	private List<IRegistry.RegData> getRegisteredEditor1(String file, String cmdType) {
		List<IRegistry.RegData> ret = new ArrayList<>();
		String ext = getExt(file);
		if( ext != null ) {
			String tmp = readRegistry(String.format("HKCR\\%s\\OpenWithProgIDs", ext));
			if( tmp != null && !tmp.startsWith("ERR")) {
				String [] list = parseResults(tmp);
				if( list != null ) {
					for (String p : list) {
						String list2 [] = p.trim().split("[ ]");
						if(list2.length > 0 ) {
							String tmp2 = readRegistry(String.format("HKCR\\%s\\shell\\%s\\command", list2[0],cmdType));
							if( tmp2 != null && !tmp2.startsWith("ERR")) {
								String [] list3 = parseResults(tmp2);
								if( list3 != null ) {
									for (String s : list3) {
										//(Default)    REG_SZ    "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" -- "%1"
										int idx = s.indexOf('"');
										if( idx > 0 ) {
											int idx2 = s.indexOf('"',idx+1);
											if( idx2 > 0 ) {
												//ret.add(new IRegistry.RegData("",s.substring(idx+1,idx2)));
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return ret;
	}

	private static String [] parseResults(String str) {

		String parts [] = str.trim().split("[\n]");
		String [] ret = new String[parts.length - 1];
		for(int idx=1; idx < parts.length ; idx++ ) {
			ret[idx-1] = parts[idx].trim();
		}

		return ret;
	}


	public static String getExt(String file) {
		String ret = null;
		int idx = file.lastIndexOf("\\");
		if( idx < 0 ) {
			idx = file.lastIndexOf("/");
		}
		
		String nm = file;
		if( idx >=0 ) {
			nm = file.substring(idx);
		}
		idx= nm.lastIndexOf('.');
		if( idx > 0 ) {
			ret = nm.substring(idx);
		}

		return ret;
	}


	/**
	 * 
	 * @param location path in the registry
	 * @param key registry key
	 * @return registry value or null if not found
	 */
	public static final String readRegistry(String location){
		try {
			// Run reg query, then read output with StreamReader (internal class)
			String cmd = "reg query "+location;
			//System.out.println("cmd="+cmd);
			Process process = Runtime.getRuntime().exec(cmd);

			StreamReader reader = new StreamReader(process.getInputStream());
			reader.start();
			process.waitFor();
			reader.join();
			String output = reader.getResult();
			return output;
		}
		catch (Exception e) {
			return null;
		}

	}

	static class StreamReader extends Thread {
		private InputStream is;
		private StringWriter sw= new StringWriter();

		public StreamReader(InputStream is) {
			this.is = is;
		}

		public void run() {
			try {
				int c;
				while ((c = is.read()) != -1)
					sw.write(c);
			}
			catch (IOException e) { 
			}
		}

		public String getResult() {
			return sw.toString();
		}
	}

	public static void main(String[] args) {
		IRegistry reg = new WindowsRegistry();
		String files [] = { "Test.txt", "Test.html","Test.png","Test.jar","Test.x.y.xml","Test",".test"};
		for (String f : files) {

			List<IRegistry.RegData> list = reg.getRegisteredHandler(f,CommandType.Editor);
			
			System.out.println(f+" sz="+list.size());
			for (RegData str : list) {
				System.out.println("\t str="+str);
			}
		}

		/*
		 * 
		 * 
HKEY_CLASSES_ROOT\.txt
    PerceivedType    REG_SZ    text
    (Default)    REG_SZ    txtfile
    Content Type    REG_SZ    text/plain


HKEY_CLASSES_ROOT\txtfile\shell\open\command
    (Default)    REG_EXPAND_SZ    %SystemRoot%\system32\NOTEPAD.EXE %1

		String value = WindowsRegistry2.readRegistry("HKCR\\Opera.HTML\\shell\\open\\command");
		System.out.println(value);

		//HKCR\.html\OpenWithProgIDs
		value = WindowsRegistry2.readRegistry("HKCR\\.html\\OpenWithProgIDs");
		System.out.println(value);
		 */
	}

	private  List<IRegistry.RegData> getRegisteredEditor2(String file, String cmdType) {
		List<IRegistry.RegData> ret = new ArrayList<>();
		String ext = getExt(file);
		if( ext != null ) {
			/*
 HKEY_CLASSES_ROOT\.txt
    PerceivedType    REG_SZ    text
    (Default)    REG_SZ    txtfile
    Content Type    REG_SZ    text/plain			
			 */
			String tmp = readRegistry(String.format("HKCR\\%s", ext));
			if( tmp != null && !tmp.startsWith("ERR")) {
				String  type = getDefault(tmp);
				if( type != null ) {
					/*
 HKEY_CLASSES_ROOT\txtfile\shell\open\command
    (Default)    REG_EXPAND_SZ    %SystemRoot%\system32\NOTEPAD.EXE %1

					 */
					String tmp2 = readRegistry(String.format("HKCR\\%s\\shell\\%s\\command", type,cmdType));
					if( tmp2 != null && !tmp2.startsWith("ERR")) {
						String [] list3 = parseResults(tmp2);
						if( list3 != null ) {
							for (String s : list3) {
								//(Default)    REG_SZ    "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" -- "%1"
								
								int idx = s.indexOf('"');
								if( idx > 0 ) {
									int idx2 = s.indexOf('"',idx+1);
									if( idx2 > 0 ) {
										//ret.add(new IRegistry.RegData("",s.substring(idx+1,idx2)));
									}
								} else {
									//(Default)    REG_EXPAND_SZ    %SystemRoot%\system32\NOTEPAD.EXE %1
									//String [] list4 = s.split("[ ]+");
									//ret.add(new IRegistry.RegData("",list4[2]));
								}
							}
						}
					}
				}
			}
		}

		return ret;
	}

	private static String getDefault(String tmp) {
		String ret = null;
		String[] list = parseResults(tmp);
		if( list != null ) {
			for (String s : list) {
				if( s.startsWith("(Default)")) {
					String parts[] = s.split("[ ]");
					ret = parts[parts.length-1].trim();
					break;
				}
			}
		}

		return ret;
	}
	@Override
	public List<RegData> getRegisteredHandler(String path,CommandType type) {
		// TODO Auto-generated method stub
		return null;
	}
}
