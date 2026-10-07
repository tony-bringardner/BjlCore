package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import us.bringardner.core.SecureBaseObject;
import us.bringardner.core.util.SocketClient;
import us.bringardner.core.util.TlsSockets;
import us.bringardner.core.util.TrustAllCertificates;

/**
 * TlsSockets and SocketClient.startTls: TLS put on a connected socket (STARTTLS style), with the
 * host name check. The servers accept a plain connection and then start TLS on it, as a mail or
 * FTP server does. The client trusts every certificate, so only the host name check can reject one.
 */
public class TestTlsSockets {

	private static final String PASSWORD = "test-only";

	/** Accepts plain connections, starts TLS (server side) and echoes one byte. */
	static class StartTlsServer extends Thread {
		final ServerSocket plain;
		final SSLContext ctx;

		StartTlsServer(SSLContext ctx) throws IOException {
			this.ctx = ctx;
			this.plain = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
			setDaemon(true);
			start();
		}

		int port() {
			return plain.getLocalPort();
		}

		@Override
		public void run() {
			while( !plain.isClosed() ) {
				try (Socket s = plain.accept()) {
					s.setSoTimeout(5000);
					SSLSocket tls = TlsSockets.layer(ctx, s, null, false, false, true);
					tls.startHandshake();
					int i = tls.getInputStream().read();
					tls.getOutputStream().write(i);
					tls.getOutputStream().flush();
				} catch (IOException e) {
					// a client that rejected our certificate, or the socket was closed
				}
			}
		}
	}

	private static File dir;
	private static StartTlsServer localhostServer;
	private static StartTlsServer otherNameServer;

	private static SSLContext serverContext(String name, String dname, String san) throws Exception {
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

		SecureBaseObject sbo = new SecureBaseObject();
		sbo.setKeyStoreFileName(ks.getAbsolutePath());
		sbo.setKeyStorePassword(PASSWORD);
		sbo.setKeyStoreType("PKCS12");
		return sbo.getSSLContext();
	}

	@BeforeAll
	public static void setUp() throws Exception {
		dir = Files.createTempDirectory("bjl-starttls").toFile();
		localhostServer = new StartTlsServer(serverContext("localhost.p12", "CN=localhost", "dns:localhost,ip:127.0.0.1"));
		otherNameServer = new StartTlsServer(serverContext("other.p12", "CN=bringardner.us", null));
	}

	@AfterAll
	public static void tearDown() throws IOException {
		if( localhostServer != null ) {
			localhostServer.plain.close();
		}
		if( otherNameServer != null ) {
			otherNameServer.plain.close();
		}
		if( dir != null ) {
			for (File f : dir.listFiles()) {
				f.delete();
			}
			dir.delete();
		}
	}

	private static Socket connect(StartTlsServer server) throws IOException {
		Socket s = new Socket(InetAddress.getLoopbackAddress(), server.port());
		s.setSoTimeout(5000);
		return s;
	}

	private static void echo(SSLSocket s) throws IOException {
		s.startHandshake();
		s.getOutputStream().write(7);
		s.getOutputStream().flush();
		assertEquals(7, s.getInputStream().read());
	}

	private static SSLContext trustAll() throws Exception {
		return TrustAllCertificates.sslContext("TLS");
	}

	@Test
	public void testLayerVerifiesTheHostName() throws Exception {
		try (Socket plain = connect(localhostServer);
				SSLSocket tls = TlsSockets.layer(trustAll(), plain, "localhost", true, true, true)) {
			echo(tls);
		}
		//  The certificate is for bringardner.us, not localhost
		try (Socket plain = connect(otherNameServer);
				SSLSocket tls = TlsSockets.layer(trustAll(), plain, "localhost", true, true, true)) {
			assertThrows(SSLException.class, () -> echo(tls));
		}
	}

	@Test
	public void testLayerByAddress() throws Exception {
		//  The certificate has the address in its subject alternative names; no SNI is sent for an address
		try (Socket plain = connect(localhostServer);
				SSLSocket tls = TlsSockets.layer(trustAll(), plain, "127.0.0.1", true, true, true)) {
			echo(tls);
		}
	}

	@Test
	public void testLayerWithoutVerification() throws Exception {
		//  Opportunistic TLS: any certificate, for any host
		try (Socket plain = connect(otherNameServer);
				SSLSocket tls = TlsSockets.layer(trustAll(), plain, "localhost", true, false, true)) {
			echo(tls);
		}
	}

	@Test
	public void testConfigureClientNeedsAHostToVerify() throws Exception {
		try (Socket plain = connect(localhostServer);
				SSLSocket tls = (SSLSocket) trustAll().getSocketFactory().createSocket(plain, null, plain.getPort(), true)) {
			tls.setUseClientMode(true);
			assertThrows(IllegalArgumentException.class, () -> TlsSockets.configureClient(tls, null, true));
			//  Without the check a missing host is fine
			TlsSockets.configureClient(tls, null, false);
		}
	}

	@Test
	public void testClientEngine() throws Exception {
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(null, null, null);

		SSLEngine engine = TlsSockets.clientEngine(ctx, " localhost ", 443, true);
		assertTrue(engine.getUseClientMode());
		assertEquals("localhost", engine.getPeerHost());
		assertEquals(443, engine.getPeerPort());
		assertEquals("HTTPS", engine.getSSLParameters().getEndpointIdentificationAlgorithm());
		assertEquals(new SNIHostName("localhost"), engine.getSSLParameters().getServerNames().get(0));

		// An address: the host name check, but no SNI (RFC 6066 doesn't allow addresses)
		engine = TlsSockets.clientEngine(ctx, "127.0.0.1", 443, true);
		assertEquals("HTTPS", engine.getSSLParameters().getEndpointIdentificationAlgorithm());
		assertTrue(engine.getSSLParameters().getServerNames() == null || engine.getSSLParameters().getServerNames().isEmpty());

		engine = TlsSockets.clientEngine(ctx, "localhost", 443, false);
		assertNull(engine.getSSLParameters().getEndpointIdentificationAlgorithm());

		assertThrows(IllegalArgumentException.class, () -> TlsSockets.clientEngine(ctx, null, 443, true));
		engine = TlsSockets.clientEngine(ctx, null, 443, false);
		assertNull(engine.getPeerHost());
		assertFalse(engine.getNeedClientAuth());
	}

	@Test
	public void testSocketClientStartTls() throws Exception {
		SocketClient client = new SocketClient();
		client.setTrustManagers(TrustAllCertificates.trustManagers());
		try (Socket plain = connect(localhostServer);
				SSLSocket tls = client.startTls(plain, "localhost")) {
			tls.getOutputStream().write(9);
			tls.getOutputStream().flush();
			assertEquals(9, tls.getInputStream().read());
		}

		Socket plain = connect(otherNameServer);
		assertThrows(SSLException.class, () -> client.startTls(plain, "localhost"));
		assertTrue(plain.isClosed(), "The socket is closed when the handshake fails");

		client.setVerifyHostname(false);
		try (Socket plain2 = connect(otherNameServer);
				SSLSocket tls = client.startTls(plain2, "localhost")) {
			tls.getOutputStream().write(5);
			tls.getOutputStream().flush();
			assertEquals(5, tls.getInputStream().read());
		}
	}

	@Test
	public void testHostnameVerifyingIsNotWrappedTwice() throws Exception {
		SSLSocketFactory f = trustAll().getSocketFactory();
		SSLSocketFactory once = TlsSockets.hostnameVerifying(f);
		assertNotSame(f, once);
		assertSame(once, TlsSockets.hostnameVerifying(once));
	}

	@Test
	public void testTrustAllCertificates() {
		assertNotSame(TrustAllCertificates.trustManagers(), TrustAllCertificates.trustManagers(), "A new array each time");
		assertSame(TrustAllCertificates.INSTANCE, TrustAllCertificates.trustManagers()[0]);
		assertEquals(0, TrustAllCertificates.INSTANCE.getAcceptedIssuers().length);
		TrustAllCertificates.INSTANCE.checkServerTrusted(null, "RSA");
		TrustAllCertificates.INSTANCE.checkClientTrusted(null, "RSA");
	}
}
