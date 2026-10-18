/**
 * Copyright Tony Bringardner 1998, 2026 <a href="http://bringardner.com/tony">Tony Bringardner</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.bringardner.parley.files.viewer.fx;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * What a user needs to decide whether to trust a server's certificate, worked out from the
 * certificate: who it's for, who vouches for it, when it's good, its fingerprints, and what
 * is wrong with it that can be seen without the chain (expired, self-signed, wrong host).
 */
final class CertificateInfo {

	private final String host;
	private final String subject;
	private final String issuer;
	private final Date notBefore;
	private final Date notAfter;
	private final String serial;
	private final String sha256;
	private final String sha1;
	private final boolean selfSigned;
	private final boolean expired;
	private final boolean notYetValid;
	private final boolean hostMismatch;

	CertificateInfo(X509Certificate cert, String host, Date now) {
		this.host = host;
		subject = cert.getSubjectX500Principal().getName();
		issuer = cert.getIssuerX500Principal().getName();
		notBefore = cert.getNotBefore();
		notAfter = cert.getNotAfter();
		serial = cert.getSerialNumber().toString(16).toUpperCase();
		sha256 = fingerprint(cert, "SHA-256");
		sha1 = fingerprint(cert, "SHA-1");
		selfSigned = cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
		expired = now.after(notAfter);
		notYetValid = now.before(notBefore);
		hostMismatch = host != null && !host.isEmpty() && !namesCover(cert, host);
	}

	/** Colon separated upper case hex of the certificate's digest, or "" if it can't be made. */
	static String fingerprint(X509Certificate cert, String algorithm) {
		try {
			byte[] digest = MessageDigest.getInstance(algorithm).digest(cert.getEncoded());
			StringBuilder buf = new StringBuilder();
			for(int i=0; i < digest.length; i++) {
				if( i > 0 ) {
					buf.append(':');
				}
				buf.append(String.format("%02X", digest[i] & 0xff));
			}
			return buf.toString();
		} catch (NoSuchAlgorithmException | CertificateEncodingException e) {
			return "";
		}
	}

	/** True if a name in the certificate (its alternative names, else its common name) covers host. */
	static boolean namesCover(X509Certificate cert, String host) {
		List<String> names = new ArrayList<>();
		try {
			java.util.Collection<List<?>> alt = cert.getSubjectAlternativeNames();
			if( alt != null ) {
				for(List<?> entry : alt) {
					// 2 is a DNS name, 7 an IP address
					if( entry.size() >= 2 && (Integer.valueOf(2).equals(entry.get(0)) || Integer.valueOf(7).equals(entry.get(0)))) {
						names.add(String.valueOf(entry.get(1)));
					}
				}
			}
		} catch (java.security.cert.CertificateParsingException e) {
			// fall through to the common name
		}
		if( names.isEmpty()) {
			for(String part : cert.getSubjectX500Principal().getName().split(",")) {
				String p = part.trim();
				if( p.regionMatches(true, 0, "CN=", 0, 3)) {
					names.add(p.substring(3));
				}
			}
		}
		String h = host.toLowerCase(java.util.Locale.ROOT);
		for(String n : names) {
			String name = n.toLowerCase(java.util.Locale.ROOT);
			if( name.equals(h)) {
				return true;
			}
			if( name.startsWith("*.") && h.indexOf('.') > 0 && h.substring(h.indexOf('.')).equals(name.substring(1))) {
				return true;
			}
		}
		return false;
	}

	String getHost() { return host; }
	String getSubject() { return subject; }
	String getIssuer() { return issuer; }
	Date getNotBefore() { return notBefore; }
	Date getNotAfter() { return notAfter; }
	String getSerial() { return serial; }
	String getSha256() { return sha256; }
	String getSha1() { return sha1; }
	boolean isSelfSigned() { return selfSigned; }
	boolean isExpired() { return expired; }
	boolean isNotYetValid() { return notYetValid; }
	boolean isHostMismatch() { return hostMismatch; }

	/** What's wrong that the user should weigh, in plain words; empty if nothing stands out. */
	List<String> warnings() {
		List<String> ret = new ArrayList<>();
		if( expired ) {
			ret.add("This certificate expired on "+notAfter+".");
		}
		if( notYetValid ) {
			ret.add("This certificate isn't valid until "+notBefore+".");
		}
		if( hostMismatch ) {
			ret.add("This certificate isn't for "+host+": it names a different server.");
		}
		if( selfSigned ) {
			ret.add("This certificate is signed by the server itself, not by an authority you trust.");
		}
		return ret;
	}
}
