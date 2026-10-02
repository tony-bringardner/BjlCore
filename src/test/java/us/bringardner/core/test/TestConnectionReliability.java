package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.SocketFactory;

import org.junit.jupiter.api.Test;

import us.bringardner.core.util.AbstractCoreServer;
import us.bringardner.core.util.SocketClient;

/**
 * Tests for stopping and restarting AbstractCoreServer, and for SocketClient's
 * connect timeout and socket cleanup.
 */
public class TestConnectionReliability {

	/** Echoes one byte per connection. */
	static class EchoServer extends AbstractCoreServer {
		final AtomicInteger runs = new AtomicInteger();
		volatile Throwable error;

		EchoServer() {
			//  port 0 = any free port
			super(0, false);
			//  Long enough that the tests would fail if stop() had to wait for it
			setAcceptTimeout(60000);
		}

		@Override
		public void run() {
			runs.incrementAndGet();
			ServerSocket ss;
			try {
				ss = getServerSocket();
			} catch (IOException e) {
				error = e;
				return;
			}
			started = running = true;
			try {
				while( !stopping && !ss.isClosed() ) {
					try (Socket s = ss.accept()) {
						InputStream in = s.getInputStream();
						OutputStream out = s.getOutputStream();
						out.write(in.read());
						out.flush();
					} catch (SocketTimeoutException e) {
						// check stopping
					} catch (IOException e) {
						if( !stopping ) {
							error = e;
						}
					}
				}
			} finally {
				running = false;
			}
		}
	}

	private static void waitForStart(AbstractCoreServer svr) throws InterruptedException {
		long end = System.currentTimeMillis()+10000;
		while( !svr.hasStarted() && System.currentTimeMillis() < end ) {
			Thread.sleep(10);
		}
		assertTrue(svr.hasStarted(), "Server did not start");
	}

	private static void echo(int port) throws Exception {
		SocketClient client = new SocketClient();
		client.setConnectTimeout(5000);
		client.setSocketTimeout(5000);
		try (Socket s = client.getSocket("localhost", port)) {
			s.getOutputStream().write(42);
			s.getOutputStream().flush();
			assertEquals(42, s.getInputStream().read());
		}
	}

	@Test
	public void testServerStopIsImmediateAndRestartable() throws Exception {
		EchoServer svr = new EchoServer();
		svr.start();
		waitForStart(svr);
		echo(svr.getServerSocket().getLocalPort());

		//  stop() closes the listening socket, so accept() returns at once instead of after the 60 second accept timeout.
		long start = System.currentTimeMillis();
		assertTrue(svr.stop(10000, false), "Server thread did not end");
		long elapsed = System.currentTimeMillis()-start;
		assertTrue(elapsed < 5000, "stop took "+elapsed+" ms");
		assertNull(svr.error, "Stopping should not be reported as an error");

		//  Restart: before the fix getServerSocket() returned the closed socket and accept() failed.
		svr.start();
		waitForStart(svr);
		ServerSocket ss = svr.getServerSocket();
		assertFalse(ss.isClosed());
		echo(ss.getLocalPort());
		assertTrue(svr.stop(10000, false), "Server thread did not end");
		assertNull(svr.error);
		assertEquals(2, svr.runs.get());
	}

	@Test
	public void testGetServerSocketReplacesClosedSocket() throws IOException {
		EchoServer svr = new EchoServer();
		ServerSocket ss1 = svr.getServerSocket();
		assertSame(ss1, svr.getServerSocket(), "An open socket is reused");

		//  A run method that closes its own socket (the old pattern) must not break a restart
		ss1.close();
		ServerSocket ss2 = svr.getServerSocket();
		assertNotSame(ss1, ss2);
		assertFalse(ss2.isClosed());

		svr.closeServerSocket();
		assertTrue(ss2.isClosed());
		//  closing twice is harmless
		svr.closeServerSocket();
	}

	@Test
	public void testConnectTimeoutDefaultAndSetter() {
		SocketClient client = new SocketClient();
		assertEquals(SocketClient.DEFAULT_CONNECT_TIMEOUT, client.getConnectTimeout());
		client.setConnectTimeout(1234);
		assertEquals(1234, client.getConnectTimeout());
	}

	@Test
	public void testConnectToUnreachableHostGivesUp() {
		SocketClient client = new SocketClient();
		client.setConnectTimeout(500);
		long start = System.currentTimeMillis();
		//  A non-routable address: the connect either times out or the network rejects it at once.
		//  Before the fix this could block for 75 seconds or more.
		assertThrows(IOException.class, () -> client.getSocket("10.255.255.1", 9));
		long elapsed = System.currentTimeMillis()-start;
		assertTrue(elapsed < 5000, "connect took "+elapsed+" ms");
	}

	@Test
	public void testSocketIsClosedWhenConfigureFails() throws Exception {
		try (ServerSocket ss = new ServerSocket(0)) {
			Socket[] captured = new Socket[1];
			SocketClient client = new SocketClient() {
				@Override
				public void configure(Socket socket) throws SocketException {
					captured[0] = socket;
					throw new SocketException("configure failed");
				}
			};
			client.setConnectTimeout(5000);
			assertThrows(SocketException.class, () -> client.getSocket("localhost", ss.getLocalPort()));
			assertTrue(captured[0] != null && captured[0].isClosed(), "The socket was not closed");
		}
	}

	/** A factory that only makes connected sockets (like some older custom factories). */
	static class ConnectedOnlyFactory extends SocketFactory {
		@Override
		public Socket createSocket(String host, int port) throws IOException {
			return new Socket(host, port);
		}
		@Override
		public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
			return new Socket(host, port, localHost, localPort);
		}
		@Override
		public Socket createSocket(InetAddress host, int port) throws IOException {
			return new Socket(host, port);
		}
		@Override
		public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
			return new Socket(address, port, localAddress, localPort);
		}
	}

	@Test
	public void testFactoryWithoutUnconnectedSockets() throws Exception {
		EchoServer svr = new EchoServer();
		svr.start();
		try {
			waitForStart(svr);
			SocketClient client = new SocketClient() {
				@Override
				public SocketFactory getSocketFactory() {
					return new ConnectedOnlyFactory();
				}
			};
			try (Socket s = client.getSocket("localhost", svr.getServerSocket().getLocalPort())) {
				assertTrue(s.isConnected());
				assertEquals(client.getSocketTimeout(), s.getSoTimeout(), "The socket was configured");
			}
		} finally {
			svr.stop(10000, false);
		}
	}
}
