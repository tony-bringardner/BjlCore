package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseObject;
import us.bringardner.core.BjlLogger;
import us.bringardner.core.ILogger;
import us.bringardner.core.ILogger.Level;
import us.bringardner.core.Log4JLogger;

/**
 * Covers the parts of BaseObject the original tests did not reach: the logging
 * convenience methods, property prefix handling, boolean properties, the cache
 * reset methods and the ILogger class selection.
 *
 * NOTE: test classes must not extend BaseObject directly (TestCore.testClassLoader
 * expects a fixed list of direct sub classes on the class path), so TestCoreBase is used.
 */
public class TestBaseObjectCoverage {

	/** Exposes the protected prefix setter. */
	static class PrefixUser extends TestCoreBase {
		void prefix(String prefix) {
			setPropertyPrefix(prefix);
		}
		String prefix() {
			return getPropertyPrefix();
		}
	}

	/** An ILogger that can't be created by BaseObject.findLogger (no default constructor). */
	public static class NoDefaultConstructorLogger extends RecordingLogger {
		public NoDefaultConstructorLogger(String unused) {
		}
	}

	@Test
	public void testLoggingConvenienceMethods() {
		RecordingLogger rec = new RecordingLogger();
		BaseObject obj = new BaseObject();
		obj.setLogger(rec);
		assertSame(rec, obj.getLogger());

		IOException err = new IOException("boom");
		obj.logDebug("d1");
		obj.logDebug(() -> "d2");
		obj.logDebug("d3", err);
		obj.logInfo("i1");
		obj.logInfo(() -> "i2");
		obj.logInfo("i3", err);
		obj.logWarn("w1");
		obj.logWarn("w2", err);
		obj.logError("e1");
		obj.logError("e2", err);

		assertEquals(Arrays.asList(
				"DEBUG d1", "DEBUG d2", "DEBUG d3 boom",
				"INFO i1", "INFO i2", "INFO i3 boom",
				"WARN w1", "WARN w2 boom",
				"ERROR e1", "ERROR e2 boom"), rec.messages);

		assertTrue(obj.isDebugEnabled());
		assertTrue(obj.isInfoEnabled());
		assertTrue(obj.isWarnEnabled());
		assertTrue(obj.isErrorEnabled());

		rec.setLevel(Level.WARN);
		assertFalse(obj.isDebugEnabled());
		assertFalse(obj.isInfoEnabled());
		assertTrue(obj.isWarnEnabled());
		assertTrue(obj.isErrorEnabled());

		//  Suppliers must not be evaluated when the level is disabled
		rec.messages.clear();
		obj.logDebug(() -> { throw new IllegalStateException("must not be called"); });
		obj.logInfo(() -> { throw new IllegalStateException("must not be called"); });
		assertTrue(rec.messages.isEmpty());
	}

	@Test
	public void testPropertyPrefix() {
		PrefixUser obj = new PrefixUser();
		assertEquals(PrefixUser.class.getName(), obj.prefix(), "The default prefix is the class name");

		System.setProperty("test.custom.prefix.Color", "red");
		System.setProperty("Color", "blue");
		try {
			obj.prefix("test.custom.prefix");
			assertEquals("test.custom.prefix", obj.prefix());
			assertEquals("red", obj.getProperty("Color"), "A prefixed property should win");

			obj.setSupportPrefixProperty(false);
			assertFalse(obj.isSupportPrefixProperty());
			assertNull(obj.prefix(), "No prefix when prefix support is off");
			assertEquals("blue", obj.getProperty("Color"), "Prefixed properties are ignored when prefix support is off");
			//  property files are still searched (TestCoreBase.properties defines Value02)
			assertEquals("2", obj.getProperty("Value02"));
		} finally {
			System.clearProperty("test.custom.prefix.Color");
			System.clearProperty("Color");
		}
	}

	@Test
	public void testBooleanProperty() {
		BaseObject obj = new BaseObject();
		System.setProperty("TestBooleanTrue", "  TRUE ");
		System.setProperty("TestBooleanOther", "yes");
		try {
			assertTrue(obj.getBooleanProperty("TestBooleanTrue", false));
			assertFalse(obj.getBooleanProperty("TestBooleanOther", true), "Only 'true' is true");
			assertTrue(obj.getBooleanProperty("TestBooleanMissing", true));
			assertFalse(obj.getBooleanProperty("TestBooleanMissing", false));
		} finally {
			System.clearProperty("TestBooleanTrue");
			System.clearProperty("TestBooleanOther");
		}
	}

	@Test
	public void testClearCaches() {
		TestCoreBase obj = new TestCoreBase();
		assertEquals("2", obj.getProperty("Value02"));
		BaseObject.clearPropertyCache();
		assertEquals("2", obj.getProperty("Value02"), "Properties are re-read after the cache is cleared");

		ILogger before = BaseObject.findLogger("test.clear.logger.cache");
		assertSame(before, BaseObject.findLogger("test.clear.logger.cache"));
		BaseObject.clearLoggerCache();
		ILogger after = BaseObject.findLogger("test.clear.logger.cache");
		assertNotNull(after);
		assertTrue(before != after, "A new logger is created after the cache is cleared");
	}

	@Test
	public void testFindLoggerWithNullName() {
		ILogger logger = BaseObject.findLogger(null);
		assertNotNull(logger);
		assertSame(logger, BaseObject.findLogger(""), "A null name is treated as the empty name");
	}

	@Test
	public void testLoggerClassSelection() throws Exception {
		Field field = BaseObject.class.getDeclaredField("loggerClass");
		field.setAccessible(true);
		Object original = field.get(null);
		String originalProperty = System.getProperty(BaseObject.PROPERTY_LOGGER);
		Class<?> defaultClass = Log4JLogger.isLog4jAvailable() ? Log4JLogger.class : BjlLogger.class;
		try {
			//  a valid ILogger implementation
			field.set(null, null);
			System.setProperty(BaseObject.PROPERTY_LOGGER, RecordingLogger.class.getName());
			ILogger logger = BaseObject.findLogger("test.logger.class.valid");
			assertEquals(RecordingLogger.class, logger.getClass());
			assertEquals("test.logger.class.valid", ((RecordingLogger) logger).getName(), "init(name) must be called");

			//  a class that is not an ILogger falls back to the default
			field.set(null, null);
			System.setProperty(BaseObject.PROPERTY_LOGGER, String.class.getName());
			assertEquals(defaultClass, BaseObject.findLogger("test.logger.class.not.ilogger").getClass());

			//  a class that does not exist falls back to the default
			field.set(null, null);
			System.setProperty(BaseObject.PROPERTY_LOGGER, "us.bringardner.no.such.Logger");
			assertEquals(defaultClass, BaseObject.findLogger("test.logger.class.missing").getClass());

			//  a logger that can't be created is a fatal error
			field.set(null, NoDefaultConstructorLogger.class);
			IllegalStateException e = assertThrows(IllegalStateException.class,
					() -> BaseObject.findLogger("test.logger.class.no.constructor"));
			assertTrue(e.getMessage().contains(NoDefaultConstructorLogger.class.getName()));
		} finally {
			field.set(null, original);
			if( originalProperty == null ) {
				System.clearProperty(BaseObject.PROPERTY_LOGGER);
			} else {
				System.setProperty(BaseObject.PROPERTY_LOGGER, originalProperty);
			}
			BaseObject.clearLoggerCache();
		}
	}
}
