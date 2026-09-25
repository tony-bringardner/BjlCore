package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseObject;
import us.bringardner.core.BaseThread;
import us.bringardner.core.BjlLogger;
import us.bringardner.core.ILogger;
import us.bringardner.core.ILogger.Level;
import us.bringardner.core.JulLogger;
import us.bringardner.core.SecureBaseObject;
import us.bringardner.core.util.AbstractCoreServer;
import us.bringardner.core.util.LogHelper;
import us.bringardner.core.util.SearchableClassLoader;

/**
 * Tests for the reliability fixes (logger defaults and caching, thread visibility,
 * property cache concurrency, property parsing, class loader search).
 */
public class TestReliability {

	@Test
	public void testDefaultLoggerLogsErrors() {
		BjlLogger logger = new BjlLogger();
		logger.init("test.default.level");
		assertEquals(BjlLogger.DEFAULT_LEVEL, logger.getLevel());
		assertTrue(logger.isErrorEnabled(), "Errors must be logged by default");
		assertFalse(logger.isDebugEnabled());
	}

	@Test
	public void testParseLevel() {
		assertEquals(Level.DEBUG, BjlLogger.parseLevel("debug", Level.ERROR));
		assertEquals(Level.WARN, BjlLogger.parseLevel(" Warning ", Level.ERROR));
		assertEquals(Level.ERROR, BjlLogger.parseLevel("FATAL", Level.NONE));
		assertEquals(Level.NONE, BjlLogger.parseLevel("off", Level.ERROR));
		//  An invalid value must not throw (that would break every BaseObject)
		assertEquals(Level.INFO, BjlLogger.parseLevel("not-a-level", Level.INFO));
		assertEquals(Level.INFO, BjlLogger.parseLevel(null, Level.INFO));
	}

	//  Test classes extend TestCoreBase (not BaseObject directly) so TestCore.testClassLoader, 
	//  which searches the class path for direct subclasses of BaseObject, is not affected.
	static class LoggerUser extends TestCoreBase {}

	@Test
	public void testLoggersAreSharedByName() {
		LoggerUser a = new LoggerUser();
		LoggerUser b = new LoggerUser();
		assertSame(a.getLogger(), b.getLogger(), "Objects of the same class should share one ILogger");
		assertSame(BaseObject.findLogger(LoggerUser.class.getName()), a.getLogger());

		LogHelper h1 = new LogHelper("shared.helper.name");
		LogHelper h2 = new LogHelper("shared.helper.name");
		assertSame(h1.getLogger(), h2.getLogger());
	}

	@Test
	public void testLazyMessageIsNotBuiltWhenDisabled() {
		BjlLogger logger = new BjlLogger();
		logger.init("test.lazy");
		logger.setLevel(Level.ERROR);
		AtomicInteger calls = new AtomicInteger();
		logger.debug(() -> "debug " + calls.incrementAndGet());
		assertEquals(0, calls.get(), "The message supplier must not be called when debug is disabled");

		PrintStream out = System.out;
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		logger.setOut(new PrintStream(bo, true));
		try {
			logger.error(() -> "error " + calls.incrementAndGet());
		} finally {
			logger.setOut(null);
			System.setOut(out);
		}
		assertEquals(1, calls.get());
		assertTrue(new String(bo.toByteArray()).contains("ERROR test.lazy - error 1"));
	}

	@Test
	public void testLogFileIsSharedAndAppended() throws IOException {
		File file = File.createTempFile("bjlcore", ".log");
		file.deleteOnExit();
		Files.write(file.toPath(), "existing line\n".getBytes());

		String key = "us.bringardner.core.BjlLogger." + BjlLogger.PROPERTY_LOG_FILE;
		System.setProperty(key, file.getAbsolutePath());
		try {
			BjlLogger one = new BjlLogger();
			one.init("file.one");
			BjlLogger two = new BjlLogger();
			two.init("file.two");
			assertSame(one.getOut(), two.getOut(), "Loggers writing to the same file should share one stream");
			one.error("from one");
			two.error("from two", new IOException("test exception"));
		} finally {
			System.clearProperty(key);
		}

		String content = new String(Files.readAllBytes(file.toPath()));
		assertTrue(content.startsWith("existing line"), "The log file must be appended, not truncated");
		assertTrue(content.contains("file.one - from one"));
		assertTrue(content.contains("file.two - from two"));
		//  errors (and stack traces) go to the log file, not System.out
		assertTrue(content.contains("java.io.IOException: test exception"));
	}

	@Test
	public void testJulLoggerDoesNotResetConfiguration() {
		java.util.logging.Logger app = java.util.logging.Logger.getLogger("test.jul.app");
		app.setLevel(java.util.logging.Level.FINE);
		java.util.logging.Handler h = new java.util.logging.ConsoleHandler();
		app.addHandler(h);
		try {
			for (int idx = 0; idx < 5; idx++) {
				new JulLogger().init("test.jul.other" + idx);
			}
			assertEquals(java.util.logging.Level.FINE, app.getLevel(), "Creating a JulLogger must not reset other loggers");
			assertTrue(Arrays.asList(app.getHandlers()).contains(h), "Creating a JulLogger must not remove handlers");
		} finally {
			app.removeHandler(h);
		}
	}

	@Test
	public void testJulLoggerInheritedLevel() {
		JulLogger logger = new JulLogger();
		logger.init("test.jul.inherited.child");
		java.util.logging.Logger.getLogger("test.jul.inherited.child").setLevel(null);
		//  must not throw a NullPointerException when the level is inherited
		ILogger.Level level = logger.getLevel();
		assertTrue(level != null);
	}

	@Test
	public void testBaseThreadStopRestartAndJoin() throws InterruptedException {
		AtomicInteger runs = new AtomicInteger();
		BaseThread thread = new BaseThread() {
			@Override
			public void run() {
				started = running = true;
				runs.incrementAndGet();
				//  Busy loop (no sleep) - only works if 'stopping' is visible to this thread
				while (!stopping) {
					Thread.onSpinWait();
				}
				running = false;
			}
		};

		thread.start();
		waitFor(thread::hasStarted, 5000);
		assertTrue(thread.isRunning());
		//  a second start while running must not create a second thread
		thread.start();
		assertTrue(thread.stop(5000, false), "Thread did not stop");
		assertFalse(thread.isRunning());

		//  restart after stop
		thread.start();
		waitFor(thread::hasStarted, 5000);
		assertTrue(thread.isRunning(), "Thread should run again after a restart");
		assertTrue(thread.stop(5000, false), "Thread did not stop after restart");
		assertEquals(2, runs.get());
	}

	@Test
	public void testBaseThreadInterruptWakesSleepingThread() throws InterruptedException {
		BaseThread thread = new BaseThread() {
			@Override
			public void run() {
				started = running = true;
				while (!stopping) {
					try {
						Thread.sleep(60000);
					} catch (InterruptedException e) {
					}
				}
				running = false;
			}
		};
		thread.start();
		waitFor(thread::hasStarted, 5000);
		long start = System.currentTimeMillis();
		assertTrue(thread.stop(5000, true), "Interrupt should wake the sleeping thread");
		assertTrue(System.currentTimeMillis() - start < 5000);
	}

	static class PropertyUser1 extends TestCoreBase {}
	static class PropertyUser2 extends TestCoreBase {}
	static class PropertyUser3 extends TestCoreBase {}
	static class PropertyUser4 extends TestCoreBase {}

	@Test
	public void testPropertyCacheConcurrency() throws Exception {
		//  a tiny cache forces constant eviction while many threads use it
		BaseObject.setMaxProperties(2);
		ExecutorService ex = Executors.newFixedThreadPool(8);
		List<Future<?>> futures = new ArrayList<>();
		try {
			for (int t = 0; t < 8; t++) {
				futures.add(ex.submit(() -> {
					BaseObject[] objects = { new PropertyUser1(), new PropertyUser2(), new PropertyUser3(),
							new PropertyUser4(), new TestCoreBase() };
					for (int idx = 0; idx < 5000; idx++) {
						BaseObject obj = objects[idx % objects.length];
						assertNull(obj.getProperty("NoSuchProperty"));
					}
					assertEquals("2", new TestCoreBase().getProperty("Value02"));
				}));
			}
			for (Future<?> f : futures) {
				f.get(60, TimeUnit.SECONDS);
			}
		} finally {
			ex.shutdownNow();
			BaseObject.setMaxProperties(BaseObject.DEFAULT_MAX_PROPERTIES);
		}
	}

	@Test
	public void testIntPropertyParsing() {
		BaseObject obj = new BaseObject();
		System.setProperty("TestIntValue", "  42 ");
		System.setProperty("TestBadIntValue", "forty-two");
		try {
			assertEquals(42, obj.getIntProperty("TestIntValue", 7));
			assertEquals(7, obj.getIntProperty("TestBadIntValue", 7), "An invalid value should use the default, not throw");
			assertEquals(7, obj.getIntProperty("TestMissingIntValue", 7));
		} finally {
			System.clearProperty("TestIntValue");
			System.clearProperty("TestBadIntValue");
		}
	}

	@Test
	public void testServerAttributesAndLinger() {
		AbstractCoreServer svr = new AbstractCoreServer(0) {
			@Override
			public void run() {
			}
		};
		svr.setAttribute("a", "value");
		assertEquals("value", svr.getAttribute("a"));
		//  null values are allowed (and remove the attribute)
		svr.setAttribute("a", null);
		assertNull(svr.getAttribute("a"));
		assertNull(svr.getAttribute(null));
		//  SO_LINGER is in seconds
		assertTrue(svr.getLingerTime() <= 60, "Default linger time should be seconds, not milliseconds");
		assertFalse(svr.isSoLinger());
		assertFalse(svr.isSecure());
	}

	@Test
	public void testClassLoaderDoesNotReturnSuperTypes() throws IOException {
		File jar = new File("TestFiles/TestSearchableClassLoader.jar").getCanonicalFile();
		try (SearchableClassLoader loader = SearchableClassLoader.getLoader(Arrays.asList(jar.getAbsolutePath()))) {
			List<Class<?>> list = loader.findTarget(SecureBaseObject.class);
			assertFalse(list.contains(BaseObject.class), "A super class of the target is not a match");
			assertTrue(list.contains(SecureBaseObject.class));
			assertTrue(list.contains(BaseThread.class));

			List<Class<?>> all = loader.findTarget(BaseObject.class, true);
			assertTrue(all.contains(BaseThread.class), "Indirect subclasses are included when requested");
			assertTrue(all.contains(AbstractCoreServer.class));
		}
	}

	@Test
	public void testClassLoaderPathWithSpaces() throws IOException {
		File dir = Files.createTempDirectory("bjl core with spaces").toFile();
		File jar = new File(dir, "test jar.jar");
		Files.copy(new File("TestFiles/TestSearchableClassLoader.jar").toPath(), jar.toPath());
		try (SearchableClassLoader loader = SearchableClassLoader.getLoader(Arrays.asList(jar.getAbsolutePath()))) {
			assertFalse(loader.findTarget(BaseObject.class).isEmpty(), "A path with spaces should be searched");
		} finally {
			jar.delete();
			dir.delete();
		}
	}

	private static void waitFor(java.util.function.BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
		long end = System.currentTimeMillis() + timeoutMillis;
		while (!condition.getAsBoolean() && System.currentTimeMillis() < end) {
			Thread.sleep(10);
		}
	}
}
