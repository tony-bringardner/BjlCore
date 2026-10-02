package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseThread;

/**
 * BJL-6, run by failsafe in 'mvn verify' against the built multi-release jar: on JDK 21+
 * the Java 21 classes in META-INF/versions/21 are used and BaseThread can run on a virtual
 * thread; on older JDKs everything stays a platform thread.
 */
public class BaseThreadVirtualIT {

	static class Worker extends BaseThread {
		final AtomicReference<Thread> ranOn = new AtomicReference<>();
		/** Read while running: a terminated thread no longer has its handler. */
		final AtomicReference<UncaughtExceptionHandler> handlerWhileRunning = new AtomicReference<>();
		final CountDownLatch ran = new CountDownLatch(1);

		@Override
		public void run() {
			started = running = true;
			ranOn.set(Thread.currentThread());
			handlerWhileRunning.set(Thread.currentThread().getUncaughtExceptionHandler());
			ran.countDown();
			running = false;
		}
	}

	/** Thread.isVirtual() by reflection, as this test is compiled for Java 11. */
	private static boolean isVirtual(Thread t) throws Exception {
		if (Runtime.version().feature() < 21) {
			return false;
		}
		return (Boolean) Thread.class.getMethod("isVirtual").invoke(t);
	}

	@Test
	public void testsTheJar() {
		URL url = BaseThread.class.getResource("BaseThread.class");
		assertEquals("jar", url.getProtocol(), "should run against the jar, not " + url);
	}

	@Test
	public void supportFollowsTheJavaVersion() {
		int feature = Runtime.version().feature();
		assertEquals(feature >= 21, BaseThread.isVirtualSupported());
		assertEquals(feature >= 24, BaseThread.isVirtualRecommended());
		if (System.getProperty(BaseThread.VIRTUAL_THREADS_PROPERTY) == null) {
			assertFalse(BaseThread.isVirtualDefault(), "upgrading bjl_core changes nothing by default");
			assertFalse(new Worker().isVirtual());
		}
	}

	@Test
	public void virtualThreadWhenAskedAndSupported() throws Exception {
		ClassLoader loader = new URLClassLoader(new URL[0]);
		UncaughtExceptionHandler handler = (t, e) -> { };
		Worker w = new Worker();
		w.setVirtual(Boolean.TRUE);
		// Not applicable to a virtual thread: must not break it (setDaemon(false) would throw)
		w.setDaemon(false);
		w.setPriority(Thread.MAX_PRIORITY);
		w.setName("virtual-worker");
		w.setContextClassLoader(loader);
		w.setUncaughtExceptionHandler(handler);
		w.start();
		assertTrue(w.ran.await(5, TimeUnit.SECONDS), "did not run");
		w.join(5000);
		Thread t = w.ranOn.get();
		assertEquals("virtual-worker", t.getName());
		assertSame(loader, t.getContextClassLoader());
		assertSame(handler, w.handlerWhileRunning.get());
		if (BaseThread.isVirtualSupported()) {
			assertTrue(isVirtual(t), "should be a virtual thread");
			assertTrue(w.isVirtualThread());
			assertTrue(t.isDaemon(), "virtual threads are daemons");
		} else {
			assertFalse(w.isVirtualThread());
			assertFalse(t.isDaemon(), "platform thread keeps the daemon setting");
			assertEquals(Thread.MAX_PRIORITY, t.getPriority());
		}
	}

	@Test
	public void platformThreadWhenNotAsked() throws Exception {
		Worker w = new Worker();
		w.setVirtual(Boolean.FALSE);
		w.start();
		assertTrue(w.ran.await(5, TimeUnit.SECONDS));
		w.join(5000);
		assertFalse(isVirtual(w.ranOn.get()));
		assertFalse(w.isVirtualThread());
	}

	@Test
	public void restartUsesTheNewSetting() throws Exception {
		Worker w = new Worker();
		w.setVirtual(Boolean.FALSE);
		w.start();
		assertTrue(w.ran.await(5, TimeUnit.SECONDS));
		w.join(5000);
		assertFalse(isVirtual(w.ranOn.get()));

		Worker again = new Worker();
		again.setVirtual(Boolean.TRUE);
		again.start();
		assertTrue(again.ran.await(5, TimeUnit.SECONDS));
		again.join(5000);
		assertEquals(BaseThread.isVirtualSupported(), isVirtual(again.ranOn.get()));
	}
}
