package us.bringardner.parley.files.viewer;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.fileproxy.FileProxy;

/**
 * Which directories the viewer shows as applications and not as folders: a macOS application
 * bundle (Calculator.app is a folder that opens as one program).
 *
 * The registry decides (IRegistry.isApplication), because only it knows whether a folder named
 * X.app is a valid application: a folder that is only named so, with nothing in it, is not. The
 * name is checked first, so no ordinary folder is ever asked about, and what the registry says is
 * kept for each bundle until the bundle changes (it is asked about every .app in a listing).
 *
 * Only a folder on this computer can be one: Launch Services answers for local paths. A bundle on
 * another machine stays a folder.
 */
public final class AppBundles {

	/** how many answers are kept */
	private static final int CACHE_SIZE = 1024;

	/** answers by path and the time the bundle changed, so a bundle that is replaced is asked about again */
	private static final Map<String,Boolean> cache = Collections.synchronizedMap(
			new LinkedHashMap<String,Boolean>(64, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<String,Boolean> eldest) {
					return size() > CACHE_SIZE;
				}
			});

	/** the registry asked (a test puts its own in the place of the system's) */
	static Supplier<IRegistry> registry = IRegistry::getRegistry;

	private AppBundles() {
	}

	/** @return true if a name ends as an application bundle's does (.app, in any case) */
	public static boolean hasAppName(String name) {
		return name != null && name.toLowerCase(Locale.ROOT).endsWith(".app");
	}

	/**
	 * @return true if the file is a directory on this computer that the registry says is an
	 *         application (always false where the registry does not know applications)
	 */
	public static boolean isAppBundle(FileSource file) {
		if( file == null || !hasAppName(file.getName()) || !(file instanceof FileProxy) ) {
			return false;
		}
		try {
			if( !file.isDirectory() ) {
				return false;
			}
			String path = ((FileProxy) file).getTarget().getAbsolutePath();
			String key = path+"|"+file.lastModified();
			Boolean ret = cache.get(key);
			if( ret == null ) {
				ret = registry.get().isApplication(path);
				cache.put(key, ret);
			}
			return ret;
		} catch (IOException e) {
			return false;
		}
	}

	/** Forgets what was said (the registry's answers changed: an application was installed). */
	public static void clear() {
		cache.clear();
	}
}
