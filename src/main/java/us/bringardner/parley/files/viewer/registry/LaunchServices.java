package us.bringardner.parley.files.viewer.registry;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

/**
 * The applications that handle a file or a file extension, as Launch Services knows them,
 * through JNA. It replaces a JNI library (macos/GetLaunchData) that was built with Xcode.
 * CoreFoundation and Launch Services are C APIs, so nothing is compiled: no Xcode project, no
 * per-architecture dylib to ship.
 *
 * Every Core Foundation object that a Copy or Create function returns is released here (the C
 * version released none of them, and released a NULL once).
 */
public final class LaunchServices {

	/** an application that handles a type */
	public static final class App {
		public final String name;
		public final String path;
		/** the .icns file of its bundle, or "" */
		public final String iconPath;
		/** what opens the file when it is double-clicked */
		public boolean isDefault;
		/** CFBundleIdentifier: two copies of one application have the same one */
		public String bundleId = "";
		/** CFBundleShortVersionString, else CFBundleVersion, or "" */
		public String version = "";
		/** the other installed copies of this application that dedupe left out (best first) */
		public final List<App> alternates = new ArrayList<>();

		App(String name, String path, String iconPath) {
			this.name = name;
			this.path = path;
			this.iconPath = iconPath;
		}

		@Override
		public String toString() {
			return name+","+path+","+iconPath;
		}
	}

	/** what Launch Services says about an extension */
	public static final class Result {
		/** the type's identifier (public.plain-text), "" if there is none */
		public final String uti;
		public final List<App> apps = new ArrayList<>();
		/** "file": asked about the file itself; "extension": asked about its extension */
		public String route = "extension";
		/** what went wrong for one handler or the whole answer; the rest is still the answer */
		public final List<String> problems = new ArrayList<>();

		Result(String uti) {
			this.uti = uti;
		}
	}

	/** the roles an application can have for a type (LSRolesMask) */
	public enum Role {
		NONE(0x1), VIEWER(0x2), EDITOR(0x4), SHELL(0x8), ALL(0xFFFFFFFF);

		final int mask;

		Role(int mask) {
			this.mask = mask;
		}
	}

	interface CF extends Library {
		Pointer CFStringCreateWithCString(Pointer allocator, String cString, int encoding);
		byte CFStringGetCString(Pointer string, byte [] buffer, long bufferSize, int encoding);
		long CFStringGetTypeID();
		long CFGetTypeID(Pointer cf);
		void CFRelease(Pointer cf);
		long CFArrayGetCount(Pointer array);
		Pointer CFArrayGetValueAtIndex(Pointer array, long index);
		byte CFURLGetFileSystemRepresentation(Pointer url, byte resolveAgainstBase, byte [] buffer, long maxBufLen);
		Pointer CFBundleCreate(Pointer allocator, Pointer url);
		Pointer CFBundleGetValueForInfoDictionaryKey(Pointer bundle, Pointer key);
		Pointer CFBundleCopyResourcesDirectoryURL(Pointer bundle);
		Pointer CFErrorCopyDescription(Pointer error);
		Pointer CFURLCreateFromFileSystemRepresentation(Pointer allocator, byte [] buffer, long bufLen, byte isDirectory);
	}

	interface LS extends Library {
		Pointer LSCopyAllRoleHandlersForContentType(Pointer contentType, int roles);
		Pointer LSCopyApplicationURLsForBundleIdentifier(Pointer bundleIdentifier, PointerByReference error);
		Pointer UTTypeCreatePreferredIdentifierForTag(Pointer tagClass, Pointer tag, Pointer conformingToType);
		Pointer LSCopyApplicationURLsForURL(Pointer url, int roles);
		Pointer LSCopyDefaultApplicationURLForURL(Pointer url, int roles, PointerByReference error);
	}

	private static final int UTF8 = 0x08000100;
	private static final int PATH_MAX = 1024;

	private static final CF cf;
	private static final LS ls;
	private static final Pointer filenameExtensionTag;

	static {
		CF c = null;
		LS l = null;
		Pointer tag = null;
		if( System.getProperty("os.name", "").startsWith("Mac") ) {
			try {
				c = Native.load("CoreFoundation", CF.class);
				l = Native.load("CoreServices", LS.class);
				// (kUTTagClassFilenameExtension is a global CFStringRef: the variable's value)
				tag = NativeLibrary.getInstance("CoreServices").getGlobalVariableAddress("kUTTagClassFilenameExtension").getPointer(0);
			} catch (Throwable e) {
				c = null;
				l = null;
			}
		}
		cf = c;
		ls = l;
		filenameExtensionTag = tag;
	}

	private LaunchServices() {
	}

	/** Core Foundation and Launch Services could be loaded (a Mac) */
	public static boolean isAvailable() {
		return cf != null && ls != null && filenameExtensionTag != null;
	}

	private static String string(Pointer cfString) {
		if( cfString == null || cf.CFGetTypeID(cfString) != cf.CFStringGetTypeID() ) {
			return null;
		}
		// (up to four UTF-8 bytes a character: a name longer than the buffer is cut, as in C)
		byte [] buf = new byte[1024];
		if( cf.CFStringGetCString(cfString, buf, buf.length, UTF8) == 0 ) {
			return null;
		}
		int n = 0;
		while( n < buf.length && buf[n] != 0 ) {
			n++;
		}
		return new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
	}

	private static String path(Pointer url) {
		if( url == null ) {
			return null;
		}
		byte [] buf = new byte[PATH_MAX];
		if( cf.CFURLGetFileSystemRepresentation(url, (byte) 1, buf, buf.length) == 0 ) {
			return null;
		}
		int n = 0;
		while( n < buf.length && buf[n] != 0 ) {
			n++;
		}
		return new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8);
	}

	/** the .icns file of bundle (CFBundleIconFile, else CFBundleIconName), or "" */
	private static String iconPath(Pointer bundle) {
		String icon = info(bundle, "CFBundleIconFile");
		if( icon == null ) {
			icon = info(bundle, "CFBundleIconName");
		}
		if( icon == null ) {
			return "";
		}
		if( !icon.contains(".icns") ) {
			icon += ".icns";
		}
		Pointer resources = cf.CFBundleCopyResourcesDirectoryURL(bundle);
		if( resources == null ) {
			return "";
		}
		try {
			String dir = path(resources);
			return dir == null ? "" : dir+"/"+icon;
		} finally {
			cf.CFRelease(resources);
		}
	}

	/** an Info.plist string of bundle, or null */
	private static String info(Pointer bundle, String key) {
		Pointer k = cf.CFStringCreateWithCString(null, key, UTF8);
		try {
			// (a Get function: owned by the bundle, not released)
			return string(cf.CFBundleGetValueForInfoDictionaryKey(bundle, k));
		} finally {
			cf.CFRelease(k);
		}
	}

	/** an App for the application at url (a bundle), or null with the reason added to problems */
	private static App appAt(Pointer url, Result into) {
		String path = path(url);
		if( path == null ) {
			into.problems.add("Failed to get file system representation");
			return null;
		}
		Pointer bundle = cf.CFBundleCreate(null, url);
		if( bundle == null ) {
			into.problems.add("Failed to create bundle from URL "+path);
			return null;
		}
		try {
			String name = info(bundle, "CFBundleDisplayName");
			if( name == null ) {
				name = info(bundle, "CFBundleName");
			}
			if( name == null ) {
				into.problems.add("Application name not found in Info.plist of "+path);
				return null;
			}
			App app = new App(name, path, iconPath(bundle));
			String id = info(bundle, "CFBundleIdentifier");
			app.bundleId = id == null ? "" : id;
			String v = info(bundle, "CFBundleShortVersionString");
			if( v == null || v.isEmpty() ) {
				v = info(bundle, "CFBundleVersion");
			}
			app.version = v == null ? "" : v;
			return app;
		} finally {
			cf.CFRelease(bundle);
		}
	}

	/**
	 * The applications that can open path, as Finder's Open With lists them, the default first.
	 *
	 * It asks about the file itself, so it sees what an extension cannot: a file with none, a
	 * package (a folder that opens as one document) and what the file's content says it is. When
	 * there is no such file it asks about the extension (handlersFor).
	 *
	 * What it returns are the applications that <em>declare</em> they open this kind of file in
	 * the role: Launch Services has no way to know that one can, so a list can hold an application
	 * that is a poor viewer (System Information, Instruments).
	 */
	public static Result handlersForPath(String path, Role role) {
		if( !isAvailable() ) {
			throw new UnsupportedOperationException("Launch Services is not available");
		}
		java.io.File file = new java.io.File(path);
		if( !file.exists() ) {
			String name = file.getName();
			int dot = name.lastIndexOf('.');
			return handlersFor(dot < 0 ? name : name.substring(dot+1), role);
		}
		Result ret = new Result("");
		ret.route = "file";
		byte [] bytes = path.getBytes(java.nio.charset.StandardCharsets.UTF_8);
		Pointer url = cf.CFURLCreateFromFileSystemRepresentation(null, bytes, bytes.length, (byte) (file.isDirectory() ? 1 : 0));
		if( url == null ) {
			ret.problems.add("Could not make a URL of "+path);
			return ret;
		}
		try {
			java.util.Set<String> seen = new java.util.HashSet<>();
			// the default first: what double-clicking the file opens
			Pointer def = ls.LSCopyDefaultApplicationURLForURL(url, role.mask, null);
			if( def != null ) {
				try {
					App app = appAt(def, ret);
					if( app != null && seen.add(app.path) ) {
						app.isDefault = true;
						ret.apps.add(app);
					}
				} finally {
					cf.CFRelease(def);
				}
			}
			Pointer urls = ls.LSCopyApplicationURLsForURL(url, role.mask);
			if( urls == null ) {
				ret.problems.add("No applications found for "+path);
				return ret;
			}
			try {
				for(long i = 0, n = cf.CFArrayGetCount(urls); i < n; i++) {
					App app = appAt(cf.CFArrayGetValueAtIndex(urls, i), ret);
					// (the same application can be listed twice: by its bundle and by an alias)
					if( app != null && seen.add(app.path) ) {
						ret.apps.add(app);
					}
				}
			} finally {
				cf.CFRelease(urls);
			}
		} finally {
			cf.CFRelease(url);
		}
		return ret;
	}

	/** 2.31226.1 against 10.4.8: by the numbers in it, from the left; a missing number is 0 */
	static int compareVersions(String a, String b) {
		String [] x = a.split("[^0-9]+");
		String [] y = b.split("[^0-9]+");
		for(int i = 0; i < Math.max(x.length, y.length); i++) {
			long p = i < x.length && !x[i].isEmpty() ? Long.parseLong(x[i]) : 0;
			long q = i < y.length && !y[i].isEmpty() ? Long.parseLong(y[i]) : 0;
			if( p != q ) {
				return Long.compare(p, q);
			}
		}
		return 0;
	}

	/** where an application is installed, best first: 0 /Applications, 1 ~/Applications, 2 /System, 3 elsewhere */
	private static int place(String path) {
		if( path.startsWith("/Applications/") ) {
			return 0;
		}
		if( path.startsWith(System.getProperty("user.home", "/nonexistent")+"/Applications/") ) {
			return 1;
		}
		return path.startsWith("/System/Applications/") ? 2 : 3;
	}

	/** how many folders deep: /Applications/X.app is 2 */
	private static int depth(String path) {
		int n = 0;
		for(int i = 0; i < path.length(); i++) {
			if( path.charAt(i) == '/' ) {
				n++;
			}
		}
		return n;
	}

	/** is a a better copy of the application than b? */
	private static boolean better(App a, App b) {
		if( a.isDefault != b.isDefault ) {
			// (what Launch Services opens the file with)
			return a.isDefault;
		}
		int v = compareVersions(a.version, b.version);
		if( v != 0 ) {
			// (the newest: the one that was installed last, or updated)
			return v > 0;
		}
		if( place(a.path) != place(b.path) ) {
			return place(a.path) < place(b.path);
		}
		if( depth(a.path) != depth(b.path) ) {
			return depth(a.path) < depth(b.path);
		}
		return a.path.compareTo(b.path) < 0;
	}

	/**
	 * One entry for each application, where it is installed more than once: the best copy
	 * (the one Launch Services opens the file with, else the newest, else the one in the
	 * better place), with the others in its alternates, so that nothing is lost. The order of
	 * the list is kept: an application is where its first copy was.
	 */
	public static Result dedupe(Result in) {
		Result out = new Result(in.uti);
		out.route = in.route;
		out.problems.addAll(in.problems);
		java.util.Map<String,App> chosen = new java.util.LinkedHashMap<>();
		for(App app : in.apps) {
			// (no bundle identifier: nothing says it is a copy of another)
			String key = app.bundleId.isEmpty() ? "path:"+app.path : app.bundleId;
			App best = chosen.get(key);
			if( best == null ) {
				chosen.put(key, app);
			} else if( better(app, best) ) {
				app.alternates.add(best);
				app.alternates.addAll(best.alternates);
				best.alternates.clear();
				chosen.put(key, app);
			} else {
				best.alternates.add(app);
			}
		}
		out.apps.addAll(chosen.values());
		return out;
	}

	/**
	 * @param extension without the dot (txt)
	 * @return what handles it in role, in Launch Services' order
	 */
	public static Result handlersFor(String extension, Role role) {
		if( !isAvailable() ) {
			throw new UnsupportedOperationException("Launch Services is not available");
		}
		String utiName = "";
		Pointer ext = cf.CFStringCreateWithCString(null, extension, UTF8);
		// (what UTType typeWithFilenameExtension: asks for: files, not folders or packages)
		Pointer conformTo = cf.CFStringCreateWithCString(null, "public.data", UTF8);
		if( ext != null ) {
			try {
				Pointer uti = ls.UTTypeCreatePreferredIdentifierForTag(filenameExtensionTag, ext, conformTo);
				if( uti != null ) {
					try {
						String s = string(uti);
						utiName = s == null ? "" : s;
					} finally {
						cf.CFRelease(uti);
					}
				}
			} finally {
				cf.CFRelease(ext);
				if( conformTo != null ) {
					cf.CFRelease(conformTo);
				}
			}
		}
		Result ret = new Result(utiName);
		if( utiName.isEmpty() ) {
			ret.problems.add("Could not determine UTI for extension.");
			return ret;
		}

		Pointer type = cf.CFStringCreateWithCString(null, utiName, UTF8);
		Pointer handlers = ls.LSCopyAllRoleHandlersForContentType(type, role.mask);
		cf.CFRelease(type);
		if( handlers == null ) {
			ret.problems.add("No handlers found ext="+extension);
			return ret;
		}
		try {
			for(long i = 0, n = cf.CFArrayGetCount(handlers); i < n; i++) {
				Pointer bundleId = cf.CFArrayGetValueAtIndex(handlers, i);
				PointerByReference error = new PointerByReference();
				Pointer urls = ls.LSCopyApplicationURLsForBundleIdentifier(bundleId, error);
				try {
					if( urls == null || cf.CFArrayGetCount(urls) == 0 ) {
						String why = "No application found for bundle identifier "+string(bundleId);
						Pointer e = error.getValue();
						if( e != null ) {
							Pointer d = cf.CFErrorCopyDescription(e);
							if( d != null ) {
								why += ": "+string(d);
								cf.CFRelease(d);
							}
							cf.CFRelease(e);
						}
						ret.problems.add(why);
						continue;
					}
					// every installed copy (Get functions: owned by the array): which one is
					// listed is dedupe's to say, and the first is not always the right one
					for(long k = 0, m = cf.CFArrayGetCount(urls); k < m; k++) {
						App app = appAt(cf.CFArrayGetValueAtIndex(urls, k), ret);
						if( app != null ) {
							ret.apps.add(app);
						}
					}
				} finally {
					if( urls != null ) {
						cf.CFRelease(urls);
					}
				}
			}
		} finally {
			cf.CFRelease(handlers);
		}
		return ret;
	}
}
