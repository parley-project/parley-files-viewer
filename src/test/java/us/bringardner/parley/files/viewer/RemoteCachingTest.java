package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The viewer's default attribute cache time for remote file systems, and that a given one is kept. */
public class RemoteCachingTest {

	private final String before = System.getProperty(RemoteCaching.FTP_ATTRIBUTE_CACHE_TTL);

	@AfterEach
	void restore() {
		if( before == null ) {
			System.clearProperty(RemoteCaching.FTP_ATTRIBUTE_CACHE_TTL);
		} else {
			System.setProperty(RemoteCaching.FTP_ATTRIBUTE_CACHE_TTL, before);
		}
	}

	@Test
	void setsTheViewersDefault() {
		System.clearProperty(RemoteCaching.FTP_ATTRIBUTE_CACHE_TTL);
		RemoteCaching.install();
		assertEquals(RemoteCaching.VIEWER_TTL, System.getProperty(RemoteCaching.FTP_ATTRIBUTE_CACHE_TTL));
	}

	@Test
	void keepsAGivenValue() {
		System.setProperty(RemoteCaching.FTP_ATTRIBUTE_CACHE_TTL, "5s");
		RemoteCaching.install();
		assertEquals("5s", System.getProperty(RemoteCaching.FTP_ATTRIBUTE_CACHE_TTL));
	}
}
