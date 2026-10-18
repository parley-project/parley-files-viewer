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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.Test;

import us.bringardner.parley.net.client.DynamicTrustManager;
import us.bringardner.parley.net.client.DynamicTrustManager.CertificateValidator.ManageAs;

class CertificateValidationTest {

	/** A real (self-signed root) certificate, from the JVM's own trust store. */
	private static X509Certificate someCertificate() throws Exception {
		TrustManagerFactory f = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		f.init((KeyStore) null);
		for(TrustManager tm : f.getTrustManagers()) {
			if( tm instanceof X509TrustManager ) {
				X509Certificate[] roots = ((X509TrustManager) tm).getAcceptedIssuers();
				if( roots.length > 0 ) {
					return roots[0];
				}
			}
		}
		throw new IllegalStateException("no certificates in the trust store");
	}

	@Test
	void describesTheCertificateAndItsProblems() throws Exception {
		X509Certificate cert = someCertificate();
		Date before = new Date(cert.getNotBefore().getTime()-1000);
		Date after = new Date(cert.getNotAfter().getTime()+1000);

		CertificateInfo ok = new CertificateInfo(cert, null, new Date(cert.getNotBefore().getTime()+1000));
		assertEquals(cert.getSubjectX500Principal().getName(), ok.getSubject());
		assertTrue(ok.isSelfSigned());
		assertFalse(ok.isExpired());
		assertFalse(ok.isNotYetValid());
		assertFalse(ok.isHostMismatch(), "with no host there is nothing to mismatch");
		// SHA-256 as 32 colon separated bytes
		assertEquals(32*3-1, ok.getSha256().length());
		assertTrue(ok.getSha256().matches("([0-9A-F]{2}:){31}[0-9A-F]{2}"));
		assertEquals(20*3-1, ok.getSha1().length());

		assertTrue(new CertificateInfo(cert, null, after).isExpired());
		assertTrue(new CertificateInfo(cert, null, before).isNotYetValid());
		CertificateInfo other = new CertificateInfo(cert, "not-this-server.invalid", new Date(cert.getNotBefore().getTime()+1000));
		assertTrue(other.isHostMismatch());
		assertTrue(other.warnings().stream().anyMatch(w->w.contains("different server")));
		assertTrue(other.warnings().stream().anyMatch(w->w.contains("signed by the server itself")));
		assertTrue(new CertificateInfo(cert, null, after).warnings().stream().anyMatch(w->w.contains("expired")));
	}

	@Test
	void anAcceptedAnswerIsRememberedForTheSameServerAndARejectIsNot() throws Exception {
		X509Certificate cert = someCertificate();
		AtomicInteger asked = new AtomicInteger();
		ManageAs[] next = { ManageAs.ACCEPT_ONCE };
		FxCertificateValidator v = new FxCertificateValidator(info->{
			asked.incrementAndGet();
			return next[0];
		});

		assertEquals(ManageAs.ACCEPT_ONCE, v.validate(cert, "a.example"));
		assertEquals(ManageAs.ACCEPT_ONCE, v.validate(cert, "A.example"));
		assertEquals(1, asked.get());
		// another server is asked about separately
		assertEquals(ManageAs.ACCEPT_ONCE, v.validate(cert, "b.example"));
		assertEquals(2, asked.get());

		next[0] = ManageAs.REJECT;
		assertEquals(ManageAs.REJECT, v.validate(cert, "c.example"));
		assertEquals(ManageAs.REJECT, v.validate(cert, "c.example"));
		assertEquals(4, asked.get());
	}

	@Test
	void aFailingOrMissingAnswerIsARejection() throws Exception {
		X509Certificate cert = someCertificate();
		assertEquals(ManageAs.REJECT, new FxCertificateValidator(info->{
			throw new IllegalStateException("no screen");
		}).validate(cert, "d.example"));
		assertEquals(ManageAs.REJECT, new FxCertificateValidator(info->null).validate(cert, "e.example"));
	}

	@Test
	void theTrustManagerAsksTheValidatorAndHonorsItsAnswer() throws Exception {
		X509Certificate cert = someCertificate();
		X509Certificate[] chain = { cert };
		// no JVM trust store behind it, so only the validator can vouch for the certificate
		DynamicTrustManager rejecting = new DynamicTrustManager(new TrustManager[0],
				new FxCertificateValidator(info->ManageAs.REJECT));
		assertThrows(CertificateException.class, ()->rejecting.checkServerTrusted(chain, "RSA"));

		DynamicTrustManager accepting = new DynamicTrustManager(new TrustManager[0],
				new FxCertificateValidator(info->ManageAs.ACCEPT_ONCE));
		accepting.checkServerTrusted(chain, "RSA");
	}
}
