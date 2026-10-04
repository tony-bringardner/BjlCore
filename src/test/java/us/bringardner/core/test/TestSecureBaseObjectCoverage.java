package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

	static class TestError extends Error {
		private static final long serialVersionUID = 1L;
	}

	@Test
	public void testErrorIsNotWrapped() {
		Secure obj = new Secure() {
			@Override
			public SecureRandom getSecureRandom() {
				throw new TestError();
			}
		};
		assertThrows(TestError.class, obj::getSSLContext, "An Error should not become an IOException");
	}

	/**
	 * A setter called while getSSLContext() is building from the old settings must not leave
	 * that stale context cached: the setter waits for the build and then clears it.
	 */
	@Test
	public void testSetterDuringBuildIsNotLost() throws Exception {
		CountDownLatch building = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Secure obj = new Secure() {
			@Override
			public SecureRandom getSecureRandom() {
				building.countDown();
				try {
					release.await(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return null;
			}
		};
		obj.setKeyStorePassword(null);
		AtomicReference<SSLContext> built = new AtomicReference<>();
		Thread getter = new Thread(() -> {
			try {
				built.set(obj.getSSLContext());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		});
		getter.start();
		assertTrue(building.await(10, TimeUnit.SECONDS));

		Thread setter = new Thread(() -> obj.setTrustManagers(new TrustManager[] {new AcceptAll()}));
		setter.start();
		//  With the fix the setter waits for the getter's lock; give it time to get there
		long end = System.currentTimeMillis()+2000;
		while( setter.isAlive() && setter.getState() != Thread.State.BLOCKED && System.currentTimeMillis() < end ) {
			Thread.sleep(10);
		}
		release.countDown();
		getter.join(10000);
		setter.join(10000);

		assertNotNull(built.get());
		assertNotSame(built.get(), obj.getSSLContext(), "The context built from the old settings should not be kept");
	}

	/** Counts the lookups of one property and keeps the char[] passed to getKeyStore. */
	static class Counting extends SecureBaseObject {
		final java.util.concurrent.atomic.AtomicInteger lookups = new java.util.concurrent.atomic.AtomicInteger();
		final String counted;
		volatile char[] passphrase;

		Counting(String counted) {
			this.counted = counted;
		}

		@Override
		public String getProperty(String propertyName, String defaultValue) {
			if( propertyName.equals(counted) ) {
				lookups.incrementAndGet();
			}
			return super.getProperty(propertyName, defaultValue);
		}

		@Override
		public KeyStore getKeyStore(char[] passphrase) throws IOException {
			this.passphrase = passphrase;
			return super.getKeyStore(passphrase);
		}
	}

	@Test
	public void testUnsetPasswordIsLookedUpOnce() {
		Counting obj = new Counting(SecureBaseObject.PROPERTY_PASS_PHRASE);
		for (int i = 0; i < 5; i++) {
			assertNull(obj.getKeyStorePassword());
		}
		assertEquals(1, obj.lookups.get(), "A password that isn't set should be looked up once");

		//  Setting null means "use the property" again, as before
		obj.setKeyStorePassword(null);
		assertNull(obj.getKeyStorePassword());
		assertEquals(2, obj.lookups.get());

		obj.setKeyStorePassword(PASSWORD);
		assertEquals(PASSWORD, obj.getKeyStorePassword());
		assertEquals(2, obj.lookups.get(), "A password that was set is not looked up");
	}

	@Test
	public void testUnsetKeyStoreNameIsLookedUpOnce() {
		Counting obj = new Counting(SecureBaseObject.PROPERTY_KEY_STORE_NAME);
		for (int i = 0; i < 5; i++) {
			assertNull(obj.getKeyStoreFileName());
		}
		assertEquals(1, obj.lookups.get(), "A key store name that isn't set should be looked up once");
		obj.setKeyStoreFileName(null);
		assertNull(obj.getKeyStoreFileName());
		assertEquals(2, obj.lookups.get());
	}

	@Test
	public void testPasswordFromPropertyIsStillRead() {
		String key = Counting.class.getName()+"."+SecureBaseObject.PROPERTY_PASS_PHRASE;
		System.setProperty(key, PASSWORD);
		try {
			Counting obj = new Counting(SecureBaseObject.PROPERTY_PASS_PHRASE);
			assertEquals(PASSWORD, obj.getKeyStorePassword());
			assertEquals(PASSWORD, obj.getKeyStorePassword());
			assertEquals(1, obj.lookups.get());
		} finally {
			System.clearProperty(key);
		}
	}

	@Test
	public void testPasswordCharsAreClearedAfterUse() throws Exception {
		File file = createEmptyKeyStore(PASSWORD);
		Counting obj = new Counting("none");
		obj.setKeyStoreType("PKCS12");
		obj.setKeyStoreFileName(file.getAbsolutePath());
		obj.setKeyStorePassword(PASSWORD);
		assertNotNull(obj.getKeyManagers());
		char[] used = obj.passphrase;
		assertNotNull(used);
		for (char c : used) {
			assertEquals('\0', c, "The password chars should be cleared once the key managers are made");
		}
		//  Still usable: the context is built from the key managers already made
		assertNotNull(obj.getSSLContext());
	}

	@Test
	public void testPasswordCharsAreClearedWhenLoadingFails() throws Exception {
		File file = createEmptyKeyStore(PASSWORD);
		Counting obj = new Counting("none");
		obj.setKeyStoreType("PKCS12");
		obj.setKeyStoreFileName(file.getAbsolutePath());
		obj.setKeyStorePassword("wrong password");
		assertThrows(IOException.class, () -> obj.getKeyManagers());
		for (char c : obj.passphrase) {
			assertEquals('\0', c);
		}
	}
}
