package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseThread;
import us.bringardner.core.BjlLogger;
import us.bringardner.core.SecureBaseObject;
import us.bringardner.core.swing.DateTimeCombo;

/**
 * BJL-53: public API that bjl_core 1.0.0 had, 1.1.0 removed and other BJL projects still use
 * (bjl_net_framework, BjlNetFtp, BjlFileSystem, BjlFileSystemViewer, BjlNetworkCore).
 * This class compiling is most of the test.
 */
@SuppressWarnings("deprecation")
public class TestApiCompatibility {

	@Test
	public void forceTlsVersionPropertyName() {
		assertEquals("ForceTlsVersion", SecureBaseObject.PROPERTY_FORCE_TLS_VERSION);
	}

	/** Like BjlNetworkCore's ProxyServer: overrides init() and calls super.init(). */
	static class InitOverride extends SecureBaseObject {
		boolean called;

		@Override
		protected void init() {
			super.init();
			called = true;
		}
	}

	@Test
	public void initCanBeOverridden() {
		InitOverride o = new InitOverride();
		o.init();
		assertTrue(o.called);
		assertFalse(o.isSecure(), "settings are still read lazily");
	}

	static class Worker extends BaseThread {
		@Override
		public void run() {
		}
	}

	@Test
	public void stopOnErrorAndErrorSleepTime() {
		Worker w = new Worker();
		assertFalse(w.isStopOnError());
		assertEquals(BaseThread.DEFAULT_ERROR_SLEEP_TIME, w.getErrorSleepTime());
		assertEquals(60000, BaseThread.DEFAULT_ERROR_SLEEP_TIME);
		w.setStopOnError(true);
		w.setErrorSleepTime(5);
		assertTrue(w.isStopOnError());
		assertEquals(5, w.getErrorSleepTime());
	}

	@Test
	public void loggerFormat() {
		assertNotNull(BjlLogger.format);
		assertEquals(19 + 4, BjlLogger.format.format(new Date(0)).length(), "MM-dd-yyyy HH:mm:ss.SSS");
	}

	@Test
	public void dateTimeComboSetdate() {
		DateTimeCombo combo = new DateTimeCombo();
		Date d = new Date(1_700_000_000_000L);
		combo.setdate(d);
		assertEquals(d, combo.getDate());
	}
}
