package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.Test;

import us.bringardner.core.SecureBaseObject;

/**
 * Covers the SecureBaseObject setters and the key store error handling.
 * Key stores are created in code (an empty PKCS12 store) so no keytool is required.
 */
public class TestSecureBaseObjectCoverage {

	private static final String PASSWORD = "changeit";

	static class Secure extends SecureBaseObject {
	}

	static class AcceptAll implements X509TrustManager {
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
		}
		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}
		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}

	static File createEmptyKeyStore(String password) throws Exception {
		File file = File.createTempFile("bjlcore-test", ".p12");
		file.deleteOnExit();
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(null, password.toCharArray());
		try(OutputStream out = new FileOutputStream(file)) {
			ks.store(out, password.toCharArray());
		}
		return file;
	}

	static Secure newSecure(File keyStore, String password) {
		Secure obj = new Secure();
		obj.setKeyStoreType("PKCS12");
		obj.setAlgorithm(KeyManagerFactory.getDefaultAlgorithm());
		obj.setKeyStoreFileName(keyStore == null ? null : keyStore.getAbsolutePath());
		obj.setKeyStorePassword(password);
		return obj;
	}

	@Test
	public void testInitReadsSecureProperty() {
		String key = Secure.class.getName()+"."+SecureBaseObject.PROPERTY_SECURE;
		System.setProperty(key, " TRUE ");
		try {
			Secure obj = new Secure();
			assertTrue(obj.isSecure());
		} finally {
			System.clearProperty(key);
		}
		Secure obj = new Secure();
		assertFalse(obj.isSecure(), "Not secure unless configured");
	}

	@Test
	public void testSslContextFromKeyStore() throws Exception {
		File file = createEmptyKeyStore(PASSWORD);
		Secure obj = newSecure(file, PASSWORD);
		obj.setProtocol("TLS");
		obj.setTrustManagers(new TrustManager[] {new AcceptAll()});

		SSLContext ctx = obj.getSSLContext();
		assertNotNull(ctx);
		assertSame(ctx, obj.getSSLContext(), "The context is cached");
		assertEquals("TLS", obj.getProtocol());
		assertEquals("PKCS12", obj.getKeyStoreType());
		assertEquals(file.getAbsolutePath(), obj.getKeyStoreFileName());
		assertEquals(PASSWORD, obj.getKeyStorePassword());
		assertNotNull(obj.getKeyStore(PASSWORD.toCharArray()));
		assertNotNull(obj.getKeyManagers());

		//  every setter resets the cached context
		obj.setSecureRandom(new SecureRandom());
		assertNotNull(obj.getSecureRandom());
		SSLContext ctx2 = obj.getSSLContext();
		assertTrue(ctx != ctx2, "setSecureRandom should reset the SSLContext");

		obj.setKeyManagers(obj.getKeyManagers());
		assertTrue(ctx2 != obj.getSSLContext(), "setKeyManagers should reset the SSLContext");
	}

	@Test
	public void testDefaultAlgorithmAndKeyStoreType() throws Exception {
		Secure obj = new Secure();
		assertEquals(KeyManagerFactory.getDefaultAlgorithm(), obj.getAlgorithm(), "The JVM default algorithm is used when none is configured");
		assertEquals(KeyStore.getDefaultType(), obj.getKeyStoreType(), "The JVM default key store type is used when none is configured");

		//  a key store file and password are all that is needed for a server context
		Secure server = new Secure();
		server.setKeyStoreFileName(createEmptyKeyStore(PASSWORD).getAbsolutePath());
		server.setKeyStorePassword(PASSWORD);
		assertNotNull(server.getKeyManagers());
		assertNotNull(server.getSSLContext());

		//  a configured value still wins
		String key = Secure.class.getName()+"."+SecureBaseObject.PROPERTY_ALGORITHM;
		System.setProperty(key, "SunX509");
		try {
			assertEquals("SunX509", new Secure().getAlgorithm());
		} finally {
			System.clearProperty(key);
		}
	}

	@Test
	public void testSetters() throws Exception {
		Secure obj = new Secure();
		SSLContext ctx = SSLContext.getInstance("TLS");
		obj.setSSLContext(ctx);
		assertSame(ctx, obj.getSSLContext());

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		obj.setKeyManagerFactory(kmf);
		assertSame(kmf, obj.getKeyManagerFactory());

		KeyManager[] managers = new KeyManager[0];
		obj.setKeyManagers(managers);
		assertSame(managers, obj.getKeyManagers());

		obj.setAlgorithm("SunX509");
		assertEquals("SunX509", obj.getAlgorithm());
		obj.setProtocol("TLSv1.2");
		assertEquals("TLSv1.2", obj.getProtocol());

		obj.setSecure(true);
		assertTrue(obj.isSecure());
		obj.setSecure(false);
		assertFalse(obj.isSecure());
	}

	@Test
	public void testDefaultTrustManagers() {
		TrustManager[] original = SecureBaseObject.getDefaultTrustManagers();
		TrustManager[] mgrs = {new AcceptAll()};
		try {
			SecureBaseObject.setDefaultTrustManagers(mgrs);
			assertSame(mgrs, SecureBaseObject.getDefaultTrustManagers());
			assertSame(mgrs, new Secure().getTrustManagers(), "New objects use the default trust managers");
		} finally {
			SecureBaseObject.setDefaultTrustManagers(original);
		}
		assertSame(original, new Secure().getTrustManagers());
	}

	@Test
	public void testNoPasswordMeansNoKeyManagers() throws Exception {
		Secure obj = newSecure(null, null);
		assertNull(obj.getKeyStorePassword());
		assertNull(obj.getKeyManagers(), "No key managers without a password (normal for a client)");
		//  a client context can still be created
		assertNotNull(obj.getSSLContext());
	}

	@Test
	public void testKeyStoreFileNotDefined() {
		Secure obj = newSecure(null, PASSWORD);
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> obj.getKeyStore(PASSWORD.toCharArray()));
		assertTrue(e.getMessage().contains(SecureBaseObject.PROPERTY_KEY_STORE_NAME));
	}

	@Test
	public void testKeyStoreFileNotFound() {
		Secure obj = newSecure(new File("no-such-dir/no-such-keystore.p12"), PASSWORD);
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> obj.getKeyStore(PASSWORD.toCharArray()));
		assertTrue(e.getMessage().contains("not found"));
	}

	@Test
	public void testInvalidKeyStoreType() throws Exception {
		Secure obj = newSecure(createEmptyKeyStore(PASSWORD), PASSWORD);
		obj.setKeyStoreType("NoSuchKeyStoreType");
		assertThrows(IOException.class, () -> obj.getKeyStore(PASSWORD.toCharArray()));
	}

	@Test
	public void testWrongPassword() throws Exception {
		Secure obj = newSecure(createEmptyKeyStore(PASSWORD), "wrong-password");
		assertThrows(IOException.class, () -> obj.getKeyStore("wrong-password".toCharArray()));
	}

	@Test
	public void testInvalidProtocol() {
		Secure obj = newSecure(null, null);
		obj.setProtocol("NoSuchProtocol");
		IOException e = assertThrows(IOException.class, obj::getSSLContext);
		assertNotNull(e.getCause(), "The original error should be the cause");
	}
}
