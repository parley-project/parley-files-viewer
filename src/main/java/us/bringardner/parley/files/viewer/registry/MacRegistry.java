package us.bringardner.parley.files.viewer.registry;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import us.bringardner.parley.files.viewer.IRegistry;

/**
 * The applications Launch Services lists for a file (see {@link LaunchServices}): through JNA,
 * so there is no library to build or to find on java.library.path.
 */
public class MacRegistry implements IRegistry {

	private class RegDataImpl extends IRegistry.RegData {


		private App app;

		public RegDataImpl(App app) {
			super(app.name, app.path);
			this.app = app;
		}

		@Override
		public List<Icon> getIcons() throws IOException {
			List<Icon> ret = new ArrayList<>();
			ret.addAll(app.getIcons());			
			return ret;
		}

		@Override
		public Icon getIcon(int width, int height) throws IOException {
			return app.getIcon(width, height);
		}

		String execPath;

		@Override
		public String getCommand() {
			if( path.endsWith(".app")) {
				if( execPath == null ) {
					File file = new File(path+"/Contents/Info.plist");
					try(InputStream in = new FileInputStream(file)) {
						String tmp = new String(in.readAllBytes());
						// <key>CFBundleExecutable</key>
						//<string>firefox</string>
						String key = "<key>CFBundleExecutable</key>";
						int idx = tmp.indexOf(key);
						if( idx > 0 ) {
							tmp = tmp.substring( idx+key.length());
							idx = tmp.indexOf('>');
							tmp = tmp.substring(idx+1);
							idx = tmp.indexOf('<');
							tmp = tmp.substring(0,idx);
							execPath = file.getParentFile().getAbsolutePath()+"/MacOS/"+tmp;
						}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return "open -Wn "+path;
				//return path;
			}
			return path;
		}

	}
	/** answers by path and role (a file's own type, so not by extension): the last 512 */
	private static final int CACHE_SIZE = 512;
	private static final Map<String,UtiInstance> map = java.util.Collections.synchronizedMap(
			new java.util.LinkedHashMap<String,UtiInstance>(64, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<String,UtiInstance> eldest) {
					return size() > CACHE_SIZE;
				}
			});

	public static ImageIcon resize(ImageIcon icon, int newW, int newH) {

		Image tmp = icon.getImage().getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return new ImageIcon(dimg);
	}  

	/**
	 * https://en.wikipedia.org/wiki/Apple_Icon_Image_format
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static List<BufferedImage> importIcons(String path) throws IOException {
		List<BufferedImage> ret = new ArrayList<>();
		File file = new File(path);
		try(DataInputStream in = new DataInputStream( new FileInputStream(path))) {
			if(!(in.read()=='i' && in.read()=='c' && in.read()=='n' && in.read()=='s')) {
				throw new IOException("Invalid icns file");
			}
			int len = in.readInt();
			if( len != file.length()) {
				throw new IOException("Invalid icns file");
			}
			while(in.available()>0) {
				in.readInt();
				len = in.readInt();
				if( len > 0 ) {
					byte [] data = new byte[len-8];
					in.readFully(data);
					{
						ByteArrayInputStream ips = new ByteArrayInputStream(data);
						BufferedImage image = ImageIO.read(ips);
						if( image != null ) {
							ret.add(image);
						}
					}
				}
			}
		}

		return ret;
	}

	private static Map<String,List<ImageIcon>> iconMap = new TreeMap<>();

	public static class App {
		String name;
		String path;
		String iconPath;


		/** the other installed copies of this application (LaunchServices.dedupe), best first */
		List<App> alternates = new ArrayList<>();

		public App(String[] parts) {
			name = parts[0];
			path = parts[1];
			iconPath = parts[2];
		}

		App(LaunchServices.App app) {
			name = app.name;
			path = app.path;
			iconPath = app.iconPath;
			for(LaunchServices.App other : app.alternates) {
				alternates.add(new App(other));
			}
		}

		/** the other installed copies of this application, best first (empty: there is one) */
		public List<App> getAlternates() {
			return alternates;
		}

		public List<ImageIcon> getIcons() throws IOException {
			List<ImageIcon> icons = iconMap.get(name);
			if( icons == null ) {
				synchronized (iconMap) {
					icons = iconMap.get(name);
					if( icons == null ) {
						List<ImageIcon> tmp = new ArrayList<>();
						if( iconPath !=null && !iconPath.isEmpty()) {
							List<BufferedImage> imgs = importIcons(iconPath);
							if( imgs !=null ) {
								for(BufferedImage img : imgs) {
									tmp.add(new ImageIcon(img));
								}
							}
							icons = tmp;
							iconMap.put(name, icons);
						}						
					}
				}
			}

			return icons;
		}

		public Icon getIcon(int width, int height) throws IOException {

			List<ImageIcon> list = getIcons();
			if( list == null || list.size()==0) {
				return null;
			}
			ImageIcon close=null;
			for(ImageIcon image : list) {
				if( image.getIconWidth()==width && image.getIconHeight()==height) {
					return image;
				}

				if( close == null ) {
					close = image;
				} else {
					if(close.getIconHeight()-height<image.getIconHeight()-height) {
						close = image;
					}
				}
			}

			ImageIcon ret = resize(close, width, height);
			list.add(ret);
			return ret;
		}


		@Override
		public String toString() {
			return name+","+path+","+iconPath;
		}
	}

	public static class UtiInstance {
		String ext;
		String uti;
		List<App> apps;

		public UtiInstance(String ext2, String uti2, List<App> apps2) {
			ext = ext2;
			uti = uti2;
			apps = apps2;
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder(ext+"="+uti+"\n");
			for(App app : apps) {
				ret.append(app.toString());
				ret.append('\n');
			}

			return ret.toString();
		}
	}

	private static LaunchServices.Role role(IRegistry.CommandType type) {
		switch (type) {
		case Viewer: return LaunchServices.Role.VIEWER;
		case Editor: return LaunchServices.Role.EDITOR;
		case None: return LaunchServices.Role.NONE;
		default: return LaunchServices.Role.ALL;
		}
	}

	/**
	 * The applications that handle a file, the default first: Launch Services is asked about the
	 * file itself where it exists (so a file with no extension and a package are answered as
	 * Finder answers them), and about its extension where it does not. An application installed
	 * more than once is listed once, as its best copy, with the others in its alternates.
	 *
	 * @param path a file's path, or just its name or extension (txt, .txt, x.txt)
	 * @return null if Launch Services is not available
	 */
	public static UtiInstance getInstance(String path, IRegistry.CommandType type) {
		if( !LaunchServices.isAvailable() ) {
			return null;
		}
		String id = path.trim()+","+type;
		UtiInstance ret = map.get(id);
		if( ret == null ) {
			// (a path that is not there is asked about by its extension: .txt, txt and x.txt are one)
			LaunchServices.Result found = LaunchServices.dedupe(LaunchServices.handlersForPath(path.trim(), role(type)));
			List<App> apps = new ArrayList<>();
			for(LaunchServices.App app : found.apps) {
				// (a claim, not a use: it is a poor viewer of anything; and an application inside
				// another, such as Xcode's Instruments)
				if( app.name.equals("System Information") ) {
					continue;
				}
				if( app.path.indexOf(".app") != app.path.lastIndexOf(".app") ) {
					continue;
				}
				apps.add(new App(app));
			}
			ret = new UtiInstance(id, found.uti, apps);
			map.put(id, ret);
		}

		return ret;
	}



	public static void main(String[] args) throws IOException {

		IRegistry.CommandType type = CommandType.Any;

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String ext = in.readLine();

		while( ext != null ) {
			if( ext.equalsIgnoreCase("v")) {
				type = CommandType.Viewer;
				System.out.println("type = "+type);
			} else if( ext.equalsIgnoreCase("e")) {
				type = CommandType.Editor;
				System.out.println("type = "+type);
			} else if( ext.equalsIgnoreCase("a")) {
				type = CommandType.Any;
				System.out.println("type = "+type);
			} else {
				UtiInstance uti = getInstance(ext,type);
				System.out.println(uti);				
			}
			ext = in.readLine();
		}

	}

	@Override
	public List<RegData> getRegisteredHandler(String path, CommandType type) {
		List<RegData> ret = new ArrayList<>();
		UtiInstance uti = getInstance(path, type);
		if( uti != null ) {
			for(App app : uti.apps) {
				ret.add(new RegDataImpl(app));
			}
		}

		return ret;
	}

}
