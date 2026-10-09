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

public class MacRegistry implements IRegistry {
	private native String getLaunchData(String ext);
	static {
		System.loadLibrary("GetLaunchData");        
	}

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
	private static MacRegistry singleton = new MacRegistry();
	private static Map<String,UtiInstance> map = new TreeMap<>();

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


		public App(String[] parts) {
			name = parts[0];
			path = parts[1];
			iconPath = parts[2];
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

	public static UtiInstance getInstance(String extention,IRegistry.CommandType type) {
		int i = extention.lastIndexOf('.');
		if( i>=0 ) {
			extention = extention.substring(i+1);
		}

		String id = extention.trim() +","+type;

		UtiInstance ret = map.get(id);
		if( ret == null ) {
			String response = singleton.getLaunchData(id);
			if( response != null ) {
				response = response.trim();
				if( response.startsWith("UTI for") 
						&& 
						response.endsWith("errors=0")) {
					String [] lines = response.split("\n");
					String uti = lines[0].substring(lines[0].indexOf('='));
					List<App> apps = new ArrayList<>();
					for (int idx = 1; idx < lines.length-1; idx++) {
						String [] parts = lines[idx].split("[,]");
						if( parts.length == 3) {
							if( !parts[0].equals("System Information")) {
								///Applications/Xcode.app/Contents/Applications/Instruments.app
								if( parts[1].indexOf(".app") == parts[1].lastIndexOf(".app")) {
									apps.add(new App(parts));
								}							
							}
						}
					}
					ret = new UtiInstance(id,uti,apps);
					map.put(id, ret);
				}
			}
		}

		return ret;
	}



	public static void main(String[] args) throws IOException {
		//System.out.println(new MacGetLaunchData().getLaunchData("bash,Editor"));

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
