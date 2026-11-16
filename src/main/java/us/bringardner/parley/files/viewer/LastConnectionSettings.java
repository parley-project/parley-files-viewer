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

import java.util.List;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import us.bringardner.parley.files.ConnectionSetting;
import us.bringardner.parley.files.ConnectionSettings;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.credentials.CredentialRef;

/**
 * The settings of the last remote session that connected, so the next New Remote Session starts
 * with them instead of a blank form.
 * <p>
 * Kept in the viewer's preferences, under {@code connections}: {@code lastType} names the
 * kind of file system, and a child node of that name holds its settings. A secret is kept only
 * as a reference to a credential store (such as {@code @keychain:parley/sftp/tony@host}, which
 * "Save to keychain" puts in the field); a typed password is never written.
 * @since 2.0.0
 */
public final class LastConnectionSettings {

	/** The node under the viewer's preferences */
	public static final String NODE = "connections";
	/** The type id of the last file system connected to */
	public static final String KEY_LAST_TYPE = "lastType";

	private final Preferences node;

	/** In the viewer's preferences ({@link FileSourceViewer#preferences()}) */
	public LastConnectionSettings() {
		this(FileSourceViewer.preferences().node(NODE));
	}

	/** In node (tests use their own) */
	public LastConnectionSettings(Preferences node) {
		this.node = node;
	}

	/**
	 * Remembers factory's type and settings as the last connection; secrets only as references.
	 * A failure to write the preferences is ignored: it only means the next form starts blank.
	 */
	public void remember(FileSourceFactory factory) {
		String type = factory.getTypeId();
		try {
			Preferences settings = node.node(type);
			settings.clear();
			Properties keep = withoutTypedSecrets(factory.getConnectionSettings(), factory.getConnectProperties());
			for(String key : keep.stringPropertyNames()) {
				String value = keep.getProperty(key);
				if( key.length() <= Preferences.MAX_KEY_LENGTH && value.length() <= Preferences.MAX_VALUE_LENGTH ) {
					settings.put(key, value);
				}
			}
			node.put(KEY_LAST_TYPE, type);
			node.flush();
		} catch (BackingStoreException | IllegalArgumentException | IllegalStateException e) {
			// not remembered
		}
	}

	/** A factory of the last type connected to, set to its remembered settings; null if there's none. */
	public FileSourceFactory restore() {
		String type = node.get(KEY_LAST_TYPE, null);
		if( type == null || !FileSourceFactory.isRegisteredFactory(type)) {
			return null;
		}
		FileSourceFactory ret = FileSourceFactory.getFileSourceFactory(type);
		if( ret == null ) {
			return null;
		}
		try {
			if( node.nodeExists(type)) {
				Preferences settings = node.node(type);
				Properties values = ret.getConnectProperties();
				if( values == null ) {
					values = new Properties();
				}
				for(String key : settings.keys()) {
					String value = settings.get(key, null);
					if( value != null ) {
						values.setProperty(key, value);
					}
				}
				ret.setConnectionProperties(values);
			}
		} catch (BackingStoreException | IllegalStateException e) {
			// the type without its settings
		}
		return ret;
	}

	/**
	 * values without the secrets that aren't references (a typed password). A setting is a secret
	 * if settings says so, or, for a value settings doesn't describe, if its name looks like one.
	 */
	static Properties withoutTypedSecrets(List<ConnectionSetting> settings, Properties values) {
		Properties ret = new Properties();
		if( values == null ) {
			return ret;
		}
		for(String key : values.stringPropertyNames()) {
			String value = values.getProperty(key);
			ConnectionSetting s = settings == null ? null : ConnectionSettings.find(settings, key);
			boolean secret = s == null ? FileSourceFactory.looksLikeSecret(key) : ConnectionSettings.isSecret(s);
			if( !secret || CredentialRef.isReference(value)) {
				ret.setProperty(key, value);
			}
		}
		return ret;
	}
}
