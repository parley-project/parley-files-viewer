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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import us.bringardner.parley.files.viewer.IRegistry;

/**
 * The applications Windows lists for a file, the one that opens it first: through JNA, as
 * {@link MacRegistry} does with Launch Services, so there is nothing to build.
 *
 * <ul>
 * <li>the application that opens (or edits) a file is what the shell itself answers to
 * AssocQueryString, so a user's own choice, ProgID indirection and %SystemRoot% are all
 * resolved by Windows and not by reading the registry here;</li>
 * <li>the others are the shell's "recommended" handlers for the extension
 * (SHAssocEnumHandlers), the list the Open with menu starts from.</li>
 * </ul>
 *
 * Windows has no viewer or editor roles for a type, so a request for an editor asks for the
 * extension's "edit" verb and falls back to what opens the file.
 *
 * An application that is not a file, such as a packaged (Store) application, cannot be started
 * from a command line with a file, so it is not listed.
 */
public class WindowsRegistry implements IRegistry {

	private static final int ASSOCF_NOTRUNCATE = 0x20;
	private static final int ASSOCF_IGNOREUNKNOWN = 0x400;
	private static final int ASSOCSTR_COMMAND = 1;
	private static final int ASSOCSTR_EXECUTABLE = 2;
	private static final int ASSOCSTR_FRIENDLYAPPNAME = 4;
	private static final int ASSOC_FILTER_RECOMMENDED = 1;
	private static final int COINIT_APARTMENTTHREADED = 2;
	private static final int E_POINTER = 0x8007007A;
	private static final int CACHE_SIZE = 256;

	interface Shlwapi extends Library {
		int AssocQueryStringW(int flags, int str, WString assoc, WString extra, char [] out, IntByReference len);
	}

	interface Shell32 extends Library {
		int SHAssocEnumHandlers(WString extension, int filter, PointerByReference enumerator);
	}

	interface Ole32 extends Library {
		int CoInitializeEx(Pointer reserved, int flags);
		void CoUninitialize();
		void CoTaskMemFree(Pointer p);
	}

	private static final Shlwapi shlwapi;
	private static final Shell32 shell32;
	private static final Ole32 ole32;

	static {
		Shlwapi s = null;
		Shell32 sh = null;
		Ole32 o = null;
		if( System.getProperty("os.name", "").startsWith("Win") ) {
			try {
				Map<String,Object> options = new HashMap<>();
				if( Native.POINTER_SIZE == 4 ) {
					// (stdcall is the calling convention of a 32 bit process only; on x64 there is one)
					options.put(Library.OPTION_CALLING_CONVENTION, Function.ALT_CONVENTION);
				}
				s = Native.load("shlwapi", Shlwapi.class, options);
				sh = Native.load("shell32", Shell32.class, options);
				o = Native.load("ole32", Ole32.class, options);
			} catch (Throwable e) {
				s = null;
				sh = null;
				o = null;
			}
		}
		shlwapi = s;
		shell32 = sh;
		ole32 = o;
	}

	/** an application, as the shell describes it */
	private static class Handler {
		String name;
		String exe;
		/** the verb's command line with %1 for the file, or null when only the program is known */
		String template;
	}

	private class RegDataImpl extends IRegistry.RegData {

		private final String template;
		private Icon icon;

		RegDataImpl(Handler h) {
			super(h.name, h.exe);
			this.template = h.template;
		}

		@Override
		public List<Icon> getIcons() throws IOException {
			List<Icon> ret = new ArrayList<>();
			Icon i = systemIcon();
			if( i != null ) {
				ret.add(i);
			}
			return ret;
		}

		private synchronized Icon systemIcon() {
			if( icon == null ) {
				icon = FileSystemView.getFileSystemView().getSystemIcon(new File(path));
			}
			return icon;
		}

		@Override
		public Icon getIcon(int width, int height) throws IOException {
			Icon i = systemIcon();
			if( i == null || (i.getIconWidth() == width && i.getIconHeight() == height) ) {
				return i;
			}
			BufferedImage src = new BufferedImage(i.getIconWidth(), i.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = src.createGraphics();
			i.paintIcon(null, g, 0, 0);
			g.dispose();
			Image scaled = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);
			BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = out.createGraphics();
			g2.drawImage(scaled, 0, 0, null);
			g2.dispose();
			return new ImageIcon(out);
		}

		@Override
		public String getCommand() {
			return path.indexOf(' ') >= 0 ? "\""+path+"\"" : path;
		}

		@Override
		public List<String> getCommandLine(String file) {
			if( template == null ) {
				return super.getCommandLine(file);
			}
			return commandLine(template, file);
		}
	}

	/**
	 * A verb's command line (WINWORD.EXE /n "%1" /o "%u") as the arguments to start it with for
	 * a file. %1 and %L are the file's path. A placeholder that has no value here (%u, %I) is
	 * left out, and so is the switch in front of it. If the template has no place for the file it
	 * is added last.
	 */
	static List<String> commandLine(String template, String file) {
		List<String> ret = new ArrayList<>();
		for(String arg : RegData.splitCommand(template)) {
			String tmp = arg;
			if( tmp.contains("%1") || tmp.contains("%L") || tmp.contains("%l") || tmp.contains("%*") ) {
				tmp = tmp.replace("%1", file).replace("%L", file).replace("%l", file).replace("%*", file);
			}
			if( tmp.replace(file, "").matches("(?s).*%[A-Za-z0-9].*") ) {
				int last = ret.size()-1;
				if( last > 0 && (ret.get(last).startsWith("/") || ret.get(last).startsWith("-")) ) {
					ret.remove(last);
				}
				continue;
			}
			ret.add(tmp);
		}
		boolean placed = false;
		for(String a : ret) {
			placed |= a.contains(file);
		}
		if( !placed ) {
			ret.add(file);
		}
		return ret;
	}

	/** answers by extension and role: the last 256 */
	private final Map<String,List<Handler>> cache = Collections.synchronizedMap(
			new LinkedHashMap<String,List<Handler>>(64, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<String,List<Handler>> eldest) {
					return size() > CACHE_SIZE;
				}
			});

	/** the shell's association functions could be loaded (Windows) */
	public static boolean isAvailable() {
		return shlwapi != null && shell32 != null && ole32 != null;
	}

	/**
	 * The extension of a file's name, with its dot (.txt), or null if it has none. A name that is
	 * only an extension (.test) is the extension.
	 */
	public static String getExt(String file) {
		if( file == null ) {
			return null;
		}
		int slash = Math.max(file.lastIndexOf('\\'), file.lastIndexOf('/'));
		String name = file.substring(slash+1);
		int dot = name.lastIndexOf('.');
		if( dot < 0 || dot == name.length()-1 ) {
			return null;
		}
		return name.substring(dot);
	}

	/** %NAME% replaced by the environment's value; one that is not set is left as it is */
	static String expand(String text) {
		StringBuilder b = new StringBuilder();
		int idx = 0;
		while( idx < text.length() ) {
			int start = text.indexOf('%', idx);
			if( start < 0 ) {
				break;
			}
			int end = text.indexOf('%', start+1);
			if( end < 0 ) {
				break;
			}
			String value = System.getenv(text.substring(start+1, end));
			if( value == null || end == start+1 ) {
				b.append(text, idx, end);
				idx = end;
			} else {
				b.append(text, idx, start).append(value);
				idx = end+1;
			}
		}
		b.append(text.substring(idx));
		return b.toString();
	}

	/** one string the shell has for an association, or null (there is none, or no verb of that name) */
	private static String query(int what, String ext, String verb) {
		char [] buf = new char[1024];
		IntByReference len = new IntByReference(buf.length);
		int flags = ASSOCF_NOTRUNCATE | ASSOCF_IGNOREUNKNOWN;
		WString v = verb == null ? null : new WString(verb);
		int hr = shlwapi.AssocQueryStringW(flags, what, new WString(ext), v, buf, len);
		if( hr == E_POINTER && len.getValue() > buf.length ) {
			buf = new char[len.getValue()];
			len.setValue(buf.length);
			hr = shlwapi.AssocQueryStringW(flags, what, new WString(ext), v, buf, len);
		}
		if( hr != 0 ) {
			return null;
		}
		int n = 0;
		while( n < buf.length && buf[n] != 0 ) {
			n++;
		}
		return n == 0 ? null : new String(buf, 0, n);
	}

	private static Handler defaultHandler(String ext, String verb) {
		String exe = query(ASSOCSTR_EXECUTABLE, ext, verb);
		if( exe == null || !new File(exe).isFile() ) {
			return null;
		}
		Handler ret = new Handler();
		ret.exe = exe;
		ret.name = query(ASSOCSTR_FRIENDLYAPPNAME, ext, verb);
		String command = query(ASSOCSTR_COMMAND, ext, verb);
		if( command != null ) {
			ret.template = expand(command);
		}
		if( ret.name == null ) {
			ret.name = baseName(exe);
		}
		return ret;
	}

	private static String baseName(String exe) {
		String n = new File(exe).getName();
		int dot = n.lastIndexOf('.');
		return dot > 0 ? n.substring(0, dot) : n;
	}

	/** a method of a COM object, by its place in the object's table of functions */
	private static int call(Pointer object, int index, Object... args) {
		Pointer table = object.getPointer(0);
		Pointer fn = table.getPointer((long)index * Native.POINTER_SIZE);
		Function f = Function.getFunction(fn, Native.POINTER_SIZE == 4 ? Function.ALT_CONVENTION : Function.C_CONVENTION);
		Object [] all = new Object[args.length+1];
		all[0] = object;
		System.arraycopy(args, 0, all, 1, args.length);
		return f.invokeInt(all);
	}

	private static void release(Pointer object) {
		call(object, 2);
	}

	/** a string a COM method allocated with CoTaskMemAlloc */
	private static String taskString(PointerByReference ref) {
		Pointer p = ref.getValue();
		if( p == null ) {
			return null;
		}
		try {
			return p.getWideString(0);
		} finally {
			ole32.CoTaskMemFree(p);
		}
	}

	/** the shell's recommended handlers for an extension (SHAssocEnumHandlers) */
	private static List<Handler> recommended(String ext) {
		List<Handler> ret = new ArrayList<>();
		int init = ole32.CoInitializeEx(null, COINIT_APARTMENTTHREADED);
		try {
			PointerByReference en = new PointerByReference();
			if( shell32.SHAssocEnumHandlers(new WString(ext), ASSOC_FILTER_RECOMMENDED, en) != 0 || en.getValue() == null ) {
				return ret;
			}
			Pointer enumerator = en.getValue();
			try {
				while( true ) {
					PointerByReference h = new PointerByReference();
					IntByReference got = new IntByReference();
					// IEnumAssocHandlers::Next is the first method after IUnknown's three
					int hr = call(enumerator, 3, 1, h, got);
					if( hr != 0 || got.getValue() != 1 || h.getValue() == null ) {
						break;
					}
					Pointer handler = h.getValue();
					try {
						// IAssocHandler: GetName (3), GetUIName (4)
						PointerByReference name = new PointerByReference();
						PointerByReference ui = new PointerByReference();
						Handler item = new Handler();
						if( call(handler, 3, name) == 0 ) {
							item.exe = taskString(name);
						}
						if( call(handler, 4, ui) == 0 ) {
							item.name = taskString(ui);
						}
						// (a packaged application has a name that is not a file: it can't be started with one)
						if( item.exe != null && new File(item.exe).isFile() ) {
							if( item.name == null ) {
								item.name = baseName(item.exe);
							}
							ret.add(item);
						}
					} finally {
						release(handler);
					}
				}
			} finally {
				release(enumerator);
			}
		} catch (Throwable e) {
			// (what was found is the answer)
		} finally {
			if( init >= 0 ) {
				ole32.CoUninitialize();
			}
		}
		return ret;
	}

	private List<Handler> handlers(String ext, CommandType type) {
		String id = ext.toLowerCase(Locale.ROOT)+","+type;
		List<Handler> ret = cache.get(id);
		if( ret == null ) {
			ret = new ArrayList<>();
			try {
				Handler first = null;
				if( type == CommandType.Editor ) {
					first = defaultHandler(ext, "edit");
				}
				if( first == null ) {
					first = defaultHandler(ext, null);
				}
				if( first != null ) {
					ret.add(first);
				}
				for(Handler h : recommended(ext)) {
					boolean listed = false;
					for(Handler tmp : ret) {
						if( tmp.exe.equalsIgnoreCase(h.exe) ) {
							listed = true;
							break;
						}
					}
					if( !listed ) {
						ret.add(h);
					}
				}
			} catch (Throwable e) {
				// (not available: nothing is registered)
			}
			cache.put(id, ret);
		}
		return ret;
	}

	/**
	 * The applications that handle a file, the one that opens it first.
	 *
	 * @param path a file's path or name, or just its extension (.txt)
	 * @return the applications, empty if the file has no extension or none is registered
	 */
	@Override
	public List<RegData> getRegisteredHandler(String path, CommandType type) {
		List<RegData> ret = new ArrayList<>();
		if( !isAvailable() || path == null ) {
			return ret;
		}
		String ext = getExt(path.trim());
		if( ext == null ) {
			return ret;
		}
		for(Handler h : handlers(ext, type)) {
			ret.add(new RegDataImpl(h));
		}
		return ret;
	}

	public static void main(String[] args) {
		IRegistry reg = new WindowsRegistry();
		String files [] = args.length > 0 ? args : new String[] { "Test.txt", "Test.html", "Test.png", "Test.jar", "Test.x.y.xml", "Test", ".test" };
		for (String f : files) {
			List<RegData> list = reg.getRegisteredHandler(f, CommandType.Any);
			System.out.println(f+" sz="+list.size());
			for (RegData d : list) {
				System.out.println("\t"+d.name+"  "+d.path+"  "+d.getCommandLine("FILE"));
			}
		}
	}
}
