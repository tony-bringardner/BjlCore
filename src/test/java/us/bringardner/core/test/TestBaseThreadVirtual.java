package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseThread;

/**
 * BJL-6: the virtual thread setting and the createThread hook. Surefire runs these against
 * target/classes, i.e. the Java 11 classes, on any JDK; BaseThreadVirtualIT tests the jar
 * (and so the Java 21 classes on JDK 21+).
 */
public class TestBaseThreadVirtual {

	static class Worker extends BaseThread {
		final AtomicReference<Thread> ranOn = new AtomicReference<>();
		final CountDownLatch ran = new CountDownLatch(1);

		@Override
		public void run() {
			started = running = true;
			ranOn.set(Thread.currentThread());
			ran.countDown();
			running = false;
		}
	}

	@Test
	public void settingDefaultsToNullAndRoundTrips() {
		Worker w = new Worker();
		assertNull(w.getVirtual(), "null means the default");
		w.setVirtual(Boolean.TRUE);
		assertEquals(Boolean.TRUE, w.getVirtual());
		assertEquals(BaseThread.isVirtualSupported(), w.isVirtual(), "true only where supported");
		w.setVirtual(Boolean.FALSE);
		assertFalse(w.isVirtual());
		w.setVirtual(null);
		assertEquals(BaseThread.isVirtualDefault(), w.isVirtual());
	}

	@Test
	public void systemPropertySetsTheDefault() {
		String old = System.getProperty(BaseThread.VIRTUAL_THREADS_PROPERTY);
		try {
			System.clearProperty(BaseThread.VIRTUAL_THREADS_PROPERTY);
			assertFalse(BaseThread.isVirtualDefault(), "off unless asked for");
			System.setProperty(BaseThread.VIRTUAL_THREADS_PROPERTY, "false");
			assertFalse(BaseThread.isVirtualDefault());
			System.setProperty(BaseThread.VIRTUAL_THREADS_PROPERTY, " TRUE ");
			assertEquals(BaseThread.isVirtualSupported(), BaseThread.isVirtualDefault());
			Worker daemon = new Worker();
			assertEquals(BaseThread.isVirtualSupported(), daemon.isVirtual());
			Worker nonDaemon = new Worker();
			nonDaemon.setDaemon(false);
			assertFalse(nonDaemon.isVirtual(), "the default never turns a non-daemon thread virtual");
			nonDaemon.setVirtual(Boolean.TRUE);
			assertEquals(BaseThread.isVirtualSupported(), nonDaemon.isVirtual(), "unless asked");
			System.setProperty(BaseThread.VIRTUAL_THREADS_PROPERTY, "auto");
			assertEquals(BaseThread.isVirtualRecommended(), BaseThread.isVirtualDefault());
			assertEquals(BaseThread.isVirtualSupported() && Runtime.version().feature() >= 24,
					BaseThread.isVirtualRecommended());
		} finally {
			if (old == null) {
				System.clearProperty(BaseThread.VIRTUAL_THREADS_PROPERTY);
			} else {
				System.setProperty(BaseThread.VIRTUAL_THREADS_PROPERTY, old);
			}
		}
	}

	@Test
	public void platformThreadKeepsDaemonAndPriority() throws Exception {
		Worker w = new Worker();
		w.setVirtual(Boolean.FALSE);
		w.setDaemon(false);
		w.setPriority(Thread.MAX_PRIORITY);
		w.setName("platform-worker");
		w.start();
		assertTrue(w.ran.await(5, TimeUnit.SECONDS));
		Thread t = w.ranOn.get();
		assertFalse(t.isDaemon());
		assertEquals(Thread.MAX_PRIORITY, t.getPriority());
		assertEquals("platform-worker", t.getName());
		assertFalse(w.isVirtualThread());
		w.join(5000);
	}

	@Test
	public void unsupportedVirtualRequestFallsBackToAPlatformThread() throws Exception {
		if (BaseThread.isVirtualSupported()) {
			return; // covered by BaseThreadVirtualIT
		}
		Worker w = new Worker();
		w.setVirtual(Boolean.TRUE);
		w.setDaemon(false);
		w.start();
		assertTrue(w.ran.await(5, TimeUnit.SECONDS));
		assertFalse(w.ranOn.get().isDaemon(), "a platform thread keeps the daemon setting");
		assertFalse(w.isVirtualThread());
		w.join(5000);
	}

	@Test
	public void createThreadHookSuppliesTheThread() throws Exception {
		AtomicInteger calls = new AtomicInteger();
		AtomicReference<Boolean> asked = new AtomicReference<>();
		AtomicReference<Thread> made = new AtomicReference<>();
		Worker w = new Worker() {
			@Override
			protected Thread createThread(boolean virtual) {
				calls.incrementAndGet();
				asked.set(virtual);
				Thread t = new Thread(this, "from-hook");
				made.set(t);
				return t;
			}
		};
		w.setVirtual(Boolean.FALSE);
		w.start();
		assertTrue(w.ran.await(5, TimeUnit.SECONDS));
		w.join(5000);
		assertEquals(1, calls.get());
		assertEquals(Boolean.FALSE, asked.get());
		assertSame(made.get(), w.ranOn.get());
		assertEquals("from-hook", w.getName(), "an unnamed BaseThread takes the thread's name");
	}
}
