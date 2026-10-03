package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseObject;
import us.bringardner.core.ILogger;
import us.bringardner.core.Log4JLogger;

/**
 * Log4j layouts that show the caller (%C %M %L %l) must show the code that logged,
 * not Log4JLogger or BaseObject.
 */
public class TestLog4jCaller {

	private static final String LOGGER = "us.bringardner.core.test.TestLog4jCaller.events";

	/** Keeps every event with the caller location log4j calculated for it. */
	static class Capture extends AbstractAppender {
		final List<LogEvent> events = new CopyOnWriteArrayList<>();
		final List<StackTraceElement> sources = new CopyOnWriteArrayList<>();

		Capture() {
			super("capture", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			//  The source must be read now, the event object may be reused
			sources.add(event.getSource());
			events.add(event.toImmutable());
		}
	}

	private Capture capture;
	private Log4JLogger logger;

	@BeforeEach
	public void setUp() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		capture = new Capture();
		capture.start();
		config.addAppender(capture);
		LoggerConfig lc = LoggerConfig.newBuilder()
				.withLoggerName(LOGGER)
				.withLevel(org.apache.logging.log4j.Level.INFO)
				.withAdditivity(false)
				.withIncludeLocation("true")
				.withConfig(config)
				.build();
		lc.addAppender(capture, null, null);
		config.addLogger(LOGGER, lc);
		ctx.updateLoggers();

		logger = new Log4JLogger();
		logger.init(LOGGER);
	}

	@AfterEach
	public void tearDown() {
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.getConfiguration().removeLogger(LOGGER);
		ctx.updateLoggers();
		capture.stop();
	}

	private void assertCaller(String method) {
		assertEquals(1, capture.sources.size(), "One event");
		StackTraceElement source = capture.sources.get(0);
		assertNotNull(source, "No caller location");
		assertEquals(TestLog4jCaller.class.getName(), source.getClassName(), "Caller class (%C), was "+source);
		assertEquals(method, source.getMethodName(), "Caller method (%M), was "+source);
		assertTrue(source.getLineNumber() > 0, "Caller line (%L), was "+source);
	}

	@Test
	public void testDirectCall() {
		logger.error("direct");
		assertCaller("testDirectCall");
		assertEquals("direct", capture.events.get(0).getMessage().getFormattedMessage());
		assertEquals(org.apache.logging.log4j.Level.ERROR, capture.events.get(0).getLevel());
	}

	@Test
	public void testDirectCallWithThrowable() {
		Exception e = new Exception("boom");
		logger.warn("with error", e);
		assertCaller("testDirectCallWithThrowable");
		assertSame(e, capture.events.get(0).getThrown());
	}

	@Test
	public void testSupplier() {
		logger.info(() -> "lazy");
		assertCaller("testSupplier");
		assertEquals("lazy", capture.events.get(0).getMessage().getFormattedMessage());
	}

	@Test
	public void testThroughBaseObject() {
		BaseObject obj = new BaseObject();
		obj.setLogger(logger);
		obj.logError("from base object");
		assertCaller("testThroughBaseObject");
	}

	@Test
	public void testThroughBaseObjectWithThrowableAndSupplier() {
		BaseObject obj = new BaseObject();
		obj.setLogger(logger);
		Exception e = new Exception("boom");
		obj.logWarn("warn", e);
		assertCaller("testThroughBaseObjectWithThrowableAndSupplier");
		assertSame(e, capture.events.get(0).getThrown());

		capture.sources.clear();
		capture.events.clear();
		obj.logInfo(() -> "lazy info");
		assertCaller("testThroughBaseObjectWithThrowableAndSupplier");
	}

	@Test
	public void testLoggerThatIsNotAnExtendedLogger() throws Exception {
		//  Every log4j2 implementation is an ExtendedLogger, but the plain Logger API path must still work
		java.lang.reflect.Field f = Log4JLogger.class.getDeclaredField("extended");
		f.setAccessible(true);
		f.setBoolean(logger, false);
		Exception e = new Exception("boom");
		logger.error("plain", e);
		logger.debug("not logged");
		assertEquals(1, capture.events.size());
		assertEquals("plain", capture.events.get(0).getMessage().getFormattedMessage());
		assertSame(e, capture.events.get(0).getThrown());
	}

	@Test
	public void testDisabledLevel() {
		//  The logger level is INFO
		assertFalse(logger.isDebugEnabled());
		assertTrue(logger.isInfoEnabled());
		assertEquals(ILogger.Level.INFO, logger.getLevel());
		logger.debug("not logged");
		logger.debug(() -> { throw new AssertionError("The message must not be built"); });
		assertTrue(capture.events.isEmpty());
	}
}
