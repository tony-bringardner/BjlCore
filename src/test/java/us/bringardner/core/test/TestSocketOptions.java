package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import us.bringardner.core.util.AbstractCoreServer;
import us.bringardner.core.util.SocketClient;

/**
 * The KeepAlive and TcpNoDelay settings (server and client) and the server's connection limit.
 */
public class TestSocketOptions {

	static class Server extends AbstractCoreServer {
		@Override
		public void run() {
		}
	}

	/** A server-side socket and the client socket connected to it */
	static Socket[] connectedPair(ServerSocket ss) throws Exception {
		Socket client = new Socket(InetAddress.getLoopbackAddress(), ss.getLocalPort());
		Socket server = ss.accept();
		return new Socket[] {server, client};
	}

	static void close(Socket[] pair) throws Exception {
		for(Socket s : pair) {
			s.close();
		}
	}

	@Test
	public void testServerDefaultsLeaveOptionsOff() throws Exception {
		Server server = new Server();
		assertFalse(server.isKeepAlive());
		assertFalse(server.isTcpNoDelay());
		try(ServerSocket ss = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
			Socket[] pair = connectedPair(ss);
			try {
				server.configure(pair[0]);
				assertFalse(pair[0].getKeepAlive());
				assertFalse(pair[0].getTcpNoDelay());
			} finally {
				close(pair);
			}
		}
	}

	@Test
	public void testServerOptionsApplied() throws Exception {
		Server server = new Server();
		server.setKeepAlive(true);
		server.setTcpNoDelay(true);
		try(ServerSocket ss = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
			Socket[] pair = connectedPair(ss);
			try {
				server.configure(pair[0]);
				assertTrue(pair[0].getKeepAlive());
				assertTrue(pair[0].getTcpNoDelay());
			} finally {
				close(pair);
			}
		}
	}

	@Test
	public void testServerOptionsFromProperties() {
		String prefix = Server.class.getName()+".";
		System.setProperty(prefix+AbstractCoreServer.PROPERTY_KEEP_ALIVE, "true");
		System.setProperty(prefix+AbstractCoreServer.PROPERTY_TCP_NO_DELAY, "TRUE");
		System.setProperty(prefix+AbstractCoreServer.PROPERTY_MAX_CONNECTIONS, "3");
		try {
			Server server = new Server();
			assertTrue(server.isKeepAlive());
			assertTrue(server.isTcpNoDelay());
			assertEquals(3, server.getMaxConnections());
		} finally {
			System.clearProperty(prefix+AbstractCoreServer.PROPERTY_KEEP_ALIVE);
			System.clearProperty(prefix+AbstractCoreServer.PROPERTY_TCP_NO_DELAY);
			System.clearProperty(prefix+AbstractCoreServer.PROPERTY_MAX_CONNECTIONS);
		}
	}

	@Test
	public void testClientOptionsApplied() throws Exception {
		SocketClient client = new SocketClient();
		assertFalse(client.isKeepAlive());
		assertFalse(client.isTcpNoDelay());
		client.setKeepAlive(true);
		client.setTcpNoDelay(true);
		try(ServerSocket ss = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
			try(Socket s = client.getSocket(InetAddress.getLoopbackAddress().getHostAddress(), ss.getLocalPort());
					Socket accepted = ss.accept()) {
				assertTrue(s.getKeepAlive());
				assertTrue(s.getTcpNoDelay());
			}
		}
	}

	@Test
	public void testNoConnectionLimitByDefault() {
		Server server = new Server();
		assertEquals(0, server.getMaxConnections());
		for(int i=0; i < 100; i++ ) {
			assertTrue(server.tryAcquireConnection());
		}
		assertEquals(100, server.getActiveConnections());
	}

	@Test
	public void testConnectionLimit() {
		Server server = new Server();
		server.setMaxConnections(2);
		assertTrue(server.tryAcquireConnection());
		assertTrue(server.tryAcquireConnection());
		assertFalse(server.tryAcquireConnection(), "The third connection should be refused");
		assertEquals(2, server.getActiveConnections());

		server.releaseConnection();
		assertTrue(server.tryAcquireConnection(), "A released place can be used again");

		server.releaseConnection();
		server.releaseConnection();
		server.releaseConnection();
		assertEquals(0, server.getActiveConnections(), "Extra releases don't make the count negative");

		server.setMaxConnections(-5);
		assertEquals(0, server.getMaxConnections(), "A negative limit means no limit");
	}

	@Test
	public void testConnectionLimitUnderContention() throws Exception {
		Server server = new Server();
		int max = 5;
		server.setMaxConnections(max);
		int threads = 16;
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger acquired = new AtomicInteger();
		List<Thread> list = new ArrayList<>();
		for(int i=0; i < threads; i++ ) {
			Thread t = new Thread(() -> {
				try {
					start.await(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					return;
				}
				for(int j=0; j < 1000; j++ ) {
					if( server.tryAcquireConnection() ) {
						acquired.incrementAndGet();
					}
				}
			});
			t.start();
			list.add(t);
		}
		start.countDown();
		for(Thread t : list) {
			t.join(10000);
		}
		assertEquals(max, acquired.get(), "Exactly the limit should be granted");
		assertEquals(max, server.getActiveConnections());
	}
}
