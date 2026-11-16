/**
 * <PRE>
 *
 * Copyright Tony Bringarder 1998, 2026 <A href="http://bringardner.com/tony">Tony Bringardner</A>
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
 *	@author Tony Bringardner
 */
package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Properties;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.sftp.SftpFileSourceFactory;

/** The last remote session's settings: remembered, restored, and never a typed password. */
public class LastConnectionSettingsTest {

	private Preferences node;
	private LastConnectionSettings last;

	@BeforeEach
	void setUp() {
		// a node of its own, removed afterwards, so the real preferences aren't touched
		node = Preferences.userRoot().node(FileSourceViewer.PREFERENCES_NODE+"/test-"+UUID.randomUUID());
		last = new LastConnectionSettings(node);
	}

	@AfterEach
	void tearDown() throws BackingStoreException {
		node.removeNode();
		node.flush();
	}

	private static FileSourceFactory sftp(String password) {
		FileSourceFactory f = FileSourceFactory.getFileSourceFactory(SftpFileSourceFactory.FACTORY_ID);
		assertNotNull(f, "sftp factory");
		Properties p = new Properties();
		p.setProperty(SftpFileSourceFactory.PROP_HOST, "example.com");
		p.setProperty(SftpFileSourceFactory.PROP_PORT, "2222");
		p.setProperty(SftpFileSourceFactory.PROP_USER, "tony");
		p.setProperty(SftpFileSourceFactory.PROP_PASSWORD, password);
		f.setConnectionProperties(p);
		return f;
	}

	@Test
	void nothingRemembered() {
		assertNull(last.restore());
	}

	@Test
	void keychainReferenceComesBack() {
		last.remember(sftp("@keychain:parley/sftp/tony@example.com"));

		FileSourceFactory f = last.restore();
		assertNotNull(f);
		assertEquals(SftpFileSourceFactory.FACTORY_ID, f.getTypeId());
		Properties p = f.getConnectProperties();
		assertEquals("example.com", p.getProperty(SftpFileSourceFactory.PROP_HOST));
		assertEquals("2222", p.getProperty(SftpFileSourceFactory.PROP_PORT));
		assertEquals("tony", p.getProperty(SftpFileSourceFactory.PROP_USER));
		assertEquals("@keychain:parley/sftp/tony@example.com", p.getProperty(SftpFileSourceFactory.PROP_PASSWORD));
	}

	@Test
	void typedPasswordIsNotWritten() throws BackingStoreException {
		last.remember(sftp("hunter2"));

		Preferences settings = node.node(SftpFileSourceFactory.FACTORY_ID);
		assertNull(settings.get(SftpFileSourceFactory.PROP_PASSWORD, null));
		assertEquals("tony", settings.get(SftpFileSourceFactory.PROP_USER, null));

		FileSourceFactory f = last.restore();
		assertNotNull(f);
		String pw = f.getConnectProperties().getProperty(SftpFileSourceFactory.PROP_PASSWORD, "");
		assertEquals("", pw);
	}

	@Test
	void undescribedSecretNamesAreDropped() {
		Properties in = new Properties();
		in.setProperty("host", "h");
		in.setProperty("password", "typed");
		in.setProperty("apiToken", "@env:TOKEN");
		Properties out = LastConnectionSettings.withoutTypedSecrets(null, in);
		assertEquals("h", out.getProperty("host"));
		assertNull(out.getProperty("password"));
		assertEquals("@env:TOKEN", out.getProperty("apiToken"));
	}
}
