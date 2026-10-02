package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import us.bringardner.core.util.AbstractCoreServer;
import us.bringardner.core.util.SocketClient;

/**
 * SocketClient checks that a TLS server's certificate was issued for the host it connects to.
 * The trust manager accepts every certificate, so the only thing that can reject one is the host name check.
 */
public class TestTlsHostname {

	private static final String PASSWORD = "test-only";

	/** A TLS server that echoes one byte per connection. */
	static class TlsEchoServer extends AbstractCoreServer {
		TlsEchoServer(File keyStore) {
			super(0, true);
			setKeyStoreFileName(keyStore.getAbsolutePath());
			setKeyStorePassword(PASSWORD);
			setKeyStoreType("PKCS12");
			setAcceptTimeout(60000);
		}

		@Override
		public void run() {
			ServerSocket ss;
			try {
				ss = getServerSocket();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			started = running = true;
			try {
				while( !stopping && !ss.isClosed() ) {
					try (Socket s = ss.accept()) {
						s.setSoTimeout(5000);
						int i = s.getInputStream().read();
						s.getOutputStream().write(i);
						s.getOutputStream().flush();
					} catch (SocketTimeoutException e) {
						// check stopping
					} catch (IOException e) {
						// a client that rejected our certificate, or stop() closed the socket
					}
				}
			} finally {
				running = false;
			}
		}
	}

	static class TrustEverything implements X509TrustManager {
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

	private static File dir;
	private static TlsEchoServer localhostServer;
	private static TlsEchoServer otherNameServer;

	private static File createKeyStore(String name, String dname, String san) throws Exception {
		File ks = new File(dir, name);
		String keytool = System.getProperty("java.home")+File.separator+"bin"+File.separator+"keytool";
		ProcessBuilder pb = new ProcessBuilder(keytool, "-genkeypair", "-noprompt",
				"-alias", "server", "-dname", dname,
				"-keystore", ks.getAbsolutePath(), "-storetype", "PKCS12",
				"-storepass", PASSWORD, "-keypass", PASSWORD,
				"-keyalg", "RSA", "-keysize", "2048", "-validity", "2");
		if( san != null ) {
			pb.command().add("-ext");
			pb.command().add("SAN="+san);
		}
		pb.redirectErrorStream(true);
		Process p = pb.start();
		String out = new String(p.getInputStream().readAllBytes());
		assertTrue(p.waitFor(60, TimeUnit.SECONDS), "keytool did not finish");
		assertEquals(0, p.exitValue(), "keytool failed: "+out);
		return ks;
	}

	private static TlsEchoServer start(File keyStore) throws InterruptedException {
		TlsEchoServer svr = new TlsEchoServer(keyStore);
		svr.start();
		long end = System.currentTimeMillis()+10000;
		while( !svr.hasStarted() && System.currentTimeMillis() < end ) {
			Thread.sleep(10);
		}
		assertTrue(svr.isRunning(), "TLS server did not start");
		return svr;
	}

	@BeforeAll
	public static void setUp() throws Exception {
		dir = Files.createTempDirectory("bjl-tls").toFile();
		localhostServer = start(createKeyStore("localhost.p12", "CN=localhost", "dns:localhost,ip:127.0.0.1"));
		otherNameServer = start(createKeyStore("other.p12", "CN=bringardner.us", null));
	}

	@AfterAll
	public static void tearDown() throws InterruptedException {
		if( localhostServer != null ) {
			localhostServer.stop(10000, false);
		}
		if( otherNameServer != null ) {
			otherNameServer.stop(10000, false);
		}
		if( dir != null ) {
			for (File f : dir.listFiles()) {
				f.delete();
			}
			dir.delete();
		}
	}

	private static SocketClient client() {
		SocketClient client = new SocketClient(true);
		client.setTrustManagers(new TrustManager[] {new TrustEverything()});
		client.setConnectTimeout(5000);
		client.setSocketTimeout(5000);
		return client;
	}

	private static void echo(Socket s) throws IOException {
		((SSLSocket) s).startHandshake();
		s.getOutputStream().write(7);
		s.getOutputStream().flush();
		assertEquals(7, s.getInputStream().read());
	}

	@Test
	public void testVerifyHostnameIsOnByDefault() {
		SocketClient client = new SocketClient(true);
		assertTrue(client.isVerifyHostname());
		client.setVerifyHostname(false);
		assertFalse(client.isVerifyHostname());
	}

	@Test
	public void testMatchingCertificateIsAccepted() throws Exception {
		try (Socket s = client().getSocket("localhost", localhostServer.getServerSocket().getLocalPort())) {
			echo(s);
		}
	}

	@Test
	public void testCertificateForAnotherHostIsRejected() throws Exception {
		try (Socket s = client().getSocket("localhost", otherNameServer.getServerSocket().getLocalPort())) {
			assertThrows(SSLException.class, () -> echo(s), "A certificate for bringardner.us must not be accepted for localhost");
		}
	}

	@Test
	public void testFactorySocketsAreCheckedToo() throws Exception {
		//  Sockets from getSocketFactory() (not only getSocket()) must be checked
		SocketFactory factory = client().getSocketFactory();
		try (Socket s = factory.createSocket("localhost", otherNameServer.getServerSocket().getLocalPort())) {
			s.setSoTimeout(5000);
			assertThrows(SSLException.class, () -> echo(s));
		}
	}

	@Test
	public void testCheckCanBeTurnedOff() throws Exception {
		SocketClient client = client();
		client.setVerifyHostname(false);
		try (Socket s = client.getSocket("localhost", otherNameServer.getServerSocket().getLocalPort())) {
			echo(s);
		}
	}
}
