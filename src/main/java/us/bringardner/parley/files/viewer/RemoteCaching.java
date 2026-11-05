package us.bringardner.parley.files.viewer;

/**
 * How long the viewer trusts what a remote file system told it about a file or a folder before
 * it asks again. The FTP file system's own default (2 seconds) suits a program that reads and
 * writes as it goes; in a viewer it meant that going back to a folder, or opening one shown a
 * few seconds earlier, listed it again (on a server across the internet, a listing over FTPS
 * takes a few hundred milliseconds). A change made in the viewer is seen at once whatever this
 * is, and Refresh reads the folder shown again; this only says how soon a change someone else
 * made shows up by itself.
 * <p>
 * Only a default: a value the user or an administrator gives on the command line
 * (-Dparley.files.ftp.attributeCacheTtl=5s) is kept.
 */
public final class RemoteCaching {

	/** parley-files-ftp's system property for its attribute cache time (a duration) */
	public static final String FTP_ATTRIBUTE_CACHE_TTL = "parley.files.ftp.attributeCacheTtl";
	/** The viewer's default for it */
	public static final String VIEWER_TTL = "30s";

	private RemoteCaching() {
	}

	/** Sets the viewer's default, unless one was given. Call before connecting to anything. */
	public static void install() {
		if( System.getProperty(FTP_ATTRIBUTE_CACHE_TTL) == null ) {
			System.setProperty(FTP_ATTRIBUTE_CACHE_TTL, VIEWER_TTL);
		}
	}
}
