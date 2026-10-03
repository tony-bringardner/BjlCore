package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.net.ServerSocketFactory;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseObject;
import us.bringardner.core.BjlLogger;
import us.bringardner.core.ILogger;
import us.bringardner.core.SecureBaseObject;
import us.bringardner.core.util.AbstractCoreServer;
import us.bringardner.core.util.LogHelper;
import us.bringardner.core.util.LruMap;
import us.bringardner.core.util.SearchableClassLoader;
import us.bringardner.core.util.SocketClient;
import us.bringardner.core.util.ThreadSafeDateFormat;

/**
 * Covers the util package code the original tests did not reach.
 */
public class TestUtilCoverage {

	static class Client extends SocketClient {
		Client() {
			super();
		}
	}

	/** A server that never runs, used to test configuration. */
	static class Server extends AbstractCoreServer {
		Server() {
			super();
		}
		Server(boolean secure) {
			super(secure);
		}
		@Override
		public void run() {
		}
	}

	// ---------------- SocketClient ----------------

	@Test
	public void testSocketClientDefaults() {
		SocketClient client = new SocketClient();
		assertFalse(client.isSecure());
		assertFalse(client.isSoLinger());
		assertEquals(SocketClient.DEFAULT_SO_LINGER, client.getLingerTime());
		assertEquals(SocketClient.DEFAULT_SOCKET_TIMEOUT, client.getSocketTimeout());
	}

	@Test
	public void testSocketClientGetSocketIsConfigured() throws Exception {
		try(ServerSocket server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress())) {
			SocketClient client = new SocketClient();
			client.setSocketTimeout(1234);
			client.setSoLinger(true);
			client.setLingerTime(5);
			try(Socket socket = client.getSocket(InetAddress.getLoopbackAddress().getHostAddress(), server.getLocalPort())) {
				assertEquals(1234, socket.getSoTimeout());
				assertEquals(5, socket.getSoLinger());
			}

			client.setSoLinger(false);
			try(Socket socket = client.getSocket(InetAddress.getLoopbackAddress().getHostAddress(), server.getLocalPort())) {
				assertEquals(-1, socket.getSoLinger(), "SO_LINGER is not set unless requested");
			}
		}
	}

	@Test
	public void testSocketClientInit() {
		String key = Client.class.getName()+"."+SocketClient.PROPERTY_IS_SO_LINGER;
		System.setProperty(key, "true");
		try {
			Client client = new Client();
			assertTrue(client.isSoLinger());
		} finally {
			System.clearProperty(key);
		}
		Client client = new Client();
		assertFalse(client.isSoLinger());
	}

	// ---------------- AbstractCoreServer ----------------

	@Test
	public void testServerDefaults() {
		Server svr = new Server();
		assertEquals(AbstractCoreServer.DEFAULT_PORT, svr.getPort());
		assertEquals(AbstractCoreServer.DEFAULT_ACCEPT_TIMEOUT, svr.getAcceptTimeout());
		assertEquals(AbstractCoreServer.DEFAULT_SOCKET_TIMEOUT, svr.getSocketTimeout());
		assertEquals(AbstractCoreServer.DEFAULT_BACKLOG, svr.getBacklog());
		assertNull(svr.getBindAddr());
		assertFalse(svr.isNeedClientAuth());

		assertTrue(new Server(true).isSecure());
		assertFalse(new Server(false).isSecure());
	}

	@Test
	public void testServerSetters() {
		Server svr = new Server();
		svr.setBacklog(7);
		assertEquals(7, svr.getBacklog());
		svr.setLingerTime(3);
		assertEquals(3, svr.getLingerTime());
		svr.setSocketTimeout(99);
		assertEquals(99, svr.getSocketTimeout());
		svr.setSoLinger(true);
		assertTrue(svr.isSoLinger());
		svr.setNeedClientAuth(true);
		assertTrue(svr.isNeedClientAuth());
		InetAddress loopback = InetAddress.getLoopbackAddress();
		svr.setBindAddr(loopback);
		assertSame(loopback, svr.getBindAddr());

		Map<String, Object> attrs = new HashMap<>();
		attrs.put("key", "value");
		svr.setAttributes(attrs);
		assertSame(attrs, svr.getAttributes());
		assertEquals("value", svr.getAttribute("key"));
		assertEquals("value", svr.removeAttribute("key"));
		assertNull(svr.removeAttribute(null));

		//  no-op hook for sub classes
		svr.connectionClosed(new Object());
	}

	@Test
	public void testServerConfigureSocket() throws Exception {
		Server svr = new Server();
		svr.setSocketTimeout(4321);
		svr.setSoLinger(true);
		svr.setLingerTime(2);
		try(Socket socket = new Socket()) {
			svr.configure(socket);
			assertEquals(4321, socket.getSoTimeout());
			assertEquals(2, socket.getSoLinger());
		}
		svr.setSoLinger(false);
		try(Socket socket = new Socket()) {
			svr.configure(socket);
			assertEquals(-1, socket.getSoLinger());
		}
	}

	@Test
	public void testServerSocketFromFactoryAndBindAddress() throws Exception {
		Server svr = new Server();
		svr.setPort(0);
		svr.setAcceptTimeout(50);
		svr.setBindAddr(InetAddress.getLoopbackAddress());
		ServerSocketFactory factory = ServerSocketFactory.getDefault();
		svr.setServerSocketFactory(factory);
		assertSame(factory, svr.getServerSocketFactory());
		ServerSocket ss = svr.getServerSocket();
		try {
			assertSame(ss, svr.getServerSocket(), "The server socket is cached");
			assertEquals(InetAddress.getLoopbackAddress(), ss.getInetAddress());
			assertEquals(50, ss.getSoTimeout());
		} finally {
			ss.close();
		}
	}

	@Test
	public void testServerBindAddressProperty() {
		String key = Server.class.getName()+"."+AbstractCoreServer.PROPERTY_BIND_ADDRESS;
		System.setProperty(key, " 127.0.0.1 ");
		try {
			assertEquals("127.0.0.1", new Server().getBindAddr().getHostAddress());
			System.setProperty(key, "[not-a-valid-address");
			Server svr = new Server();
			svr.setLogger(new RecordingLogger());
			assertThrows(IllegalStateException.class, svr::getBindAddr);
		} finally {
			System.clearProperty(key);
		}
	}

	@Test
	public void testServerInit() {
		String linger = Server.class.getName()+"."+AbstractCoreServer.PROPERTY_IS_SO_LINGER;
		String bind = Server.class.getName()+"."+AbstractCoreServer.PROPERTY_BIND_ADDRESS;
		try {
			System.setProperty(linger, "TRUE");
			System.setProperty(bind, "127.0.0.1");
			Server svr = new Server();
			assertTrue(svr.isSoLinger());
			assertEquals("127.0.0.1", svr.getBindAddr().getHostAddress());

			System.setProperty(bind, "[not-a-valid-address");
			Server bad = new Server();
			RecordingLogger rec = new RecordingLogger();
			bad.setLogger(rec);
			assertThrows(IllegalStateException.class, bad::getBindAddr);
			assertTrue(rec.messages.get(0).startsWith("ERROR Cannot create BindAddress"), "The error should be logged");
		} finally {
			System.clearProperty(linger);
			System.clearProperty(bind);
		}
		Server svr = new Server();
		assertFalse(svr.isSoLinger());
		assertNull(svr.getBindAddr());
	}

	// ---------------- SearchableClassLoader ----------------

	private static File testFile(String path) throws IOException {
		return new File(path).getCanonicalFile();
	}

	@Test
	public void testClassLoaderWithNoPaths() throws IOException {
		try(SearchableClassLoader loader = SearchableClassLoader.getLoader(null)) {
			assertTrue(loader.findTarget(BaseObject.class).isEmpty());
		}
		try(SearchableClassLoader loader = SearchableClassLoader.getLoader(Arrays.asList("", null))) {
			assertEquals(0, loader.getURLs().length, "Empty and null paths are ignored");
		}
	}

	@Test
	public void testClassLoaderSingleClassFile() throws IOException {
		File cls = testFile("TestFiles/us/bringardner/core/BjlLogger.class");
		try(SearchableClassLoader loader = SearchableClassLoader.getLoader(Arrays.asList(cls.getPath()))) {
			List<Class<?>> list = loader.findTarget(BaseObject.class);
			assertEquals(Arrays.asList(BjlLogger.class), list);
		}
	}

	@Test
	public void testClassLoaderInterfaceTarget() throws IOException {
		File jar = testFile("TestFiles/TestSearchableClassLoader.jar");
		try(SearchableClassLoader loader = SearchableClassLoader.getLoader(Arrays.asList(jar.getPath()))) {
			List<Class<?>> list = loader.findTarget(ILogger.class);
			assertTrue(list.contains(BjlLogger.class), "Classes that implement the interface directly match");
			assertFalse(list.contains(BaseObject.class));
		}
	}

	@Test
	public void testClassLoaderDirectoryContainingJar() throws IOException {
		//  TestFiles holds both the class files (under us/) and a jar
		File dir = testFile("TestFiles");
		try(SearchableClassLoader loader = SearchableClassLoader.getLoader(Arrays.asList(dir.getPath()))) {
			List<Class<?>> list = loader.findTarget(SecureBaseObject.class);
			assertTrue(list.contains(SecureBaseObject.class));
			assertEquals(list.size(), list.stream().distinct().count(), "A class found twice is only listed once");
		}
	}

	@Test
	public void testClassLoaderIgnoresBadUrls() throws IOException {
		try(SearchableClassLoader loader = SearchableClassLoader.getLoader(null)) {
			//  not a file URL
			loader.addUrl(new URL("http://localhost/not-searched.jar"));
			//  a file URL that is not a valid URI (the space) and does not exist
			loader.addUrl(new URL("file:/no such dir/missing.jar"));
			assertTrue(loader.findTarget(BaseObject.class).isEmpty());
		}
	}

	// ---------------- LogHelper, ThreadSafeDateFormat, LruMap ----------------

	@Test
	public void testLogHelperForClass() {
		LogHelper helper = new LogHelper(TestUtilCoverage.class);
		assertSame(BaseObject.findLogger(TestUtilCoverage.class.getName()), helper.getLogger());
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testThreadSafeDateFormatParse() throws ParseException {
		ThreadSafeDateFormat fmt = new ThreadSafeDateFormat("yyyy-MM-dd HH:mm:ss");
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = fmt.parse("1970-01-02 00:00:01");
		assertEquals(86401000L, date.getTime());
		assertEquals("1970-01-02 00:00:01", fmt.format(date));
		assertThrows(ParseException.class, () -> fmt.parse("not a date"));
	}

	@Test
	public void testLruMapDefaultAndUnbounded() {
		LruMap<Integer, Integer> map = new LruMap<>();
		assertEquals(10000, map.getMaxSize());

		LruMap<Integer, Integer> unbounded = new LruMap<>(0);
		for(int idx = 0; idx < 100; idx++) {
			unbounded.put(idx, idx);
		}
		assertEquals(100, unbounded.size(), "A max size of 0 means no limit");
	}

	@Test
	public void testLruMapSetMaxSizeZeroMeansNoLimit() {
		LruMap<Integer, Integer> map = new LruMap<>(5);
		for(int idx = 0; idx < 5; idx++) {
			map.put(idx, idx);
		}
		map.setMaxSize(0);
		assertEquals(5, map.size(), "Removing the limit must not remove entries");
		for(int idx = 5; idx < 50; idx++) {
			map.put(idx, idx);
		}
		assertEquals(50, map.size());
		map.setMaxSize(-1);
		assertEquals(50, map.size());
	}

	@Test
	public void testLogHelperProperties() {
		//  From the properties file of the class (us/bringardner/core/test/TestCore.properties)
		LogHelper forClass = new LogHelper(TestCore.class);
		assertEquals("1", forClass.getProperty("Value01"));

		//  System properties with the class name (or the name) as the prefix
		String key = TestUtilCoverage.class.getName()+".LogHelperValue";
		System.setProperty(key, "fromPrefix");
		try {
			assertEquals("fromPrefix", new LogHelper(TestUtilCoverage.class).getProperty("LogHelperValue"));
			assertEquals("fromPrefix", new LogHelper(TestUtilCoverage.class.getName()).getProperty("LogHelperValue"));
		} finally {
			System.clearProperty(key);
		}
		assertNull(new LogHelper("no.such.Name").getProperty("LogHelperValue"));
	}

	@Test
	public void testLruMapShrink() {
		LruMap<String, String> map = new LruMap<>(5);
		for(String key : new String[] {"a","b","c","d","e"}) {
			map.put(key, key);
		}
		map.get("a");	//  a is now the most recently used
		map.setMaxSize(2);
		assertEquals(2, map.size());
		assertTrue(map.containsKey("a"));
		assertTrue(map.containsKey("e"));
	}
}
