package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseThread;

/**
 * Covers the BaseThread constructors, the properties applied to the thread at start,
 * and stop/join edge cases.
 */
public class TestBaseThreadCoverage {

	/** A thread that runs until stopped and remembers the Thread it ran on. */
	static class Worker extends BaseThread {
		final AtomicReference<Thread> ranOn = new AtomicReference<>();
		final CountDownLatch startedLatch = new CountDownLatch(1);

		Worker() {
			super();
		}
		Worker(String name) {
			super(name);
		}
		Worker(boolean daemon) {
			super(daemon);
		}
		Worker(String name, boolean daemon) {
			super(name, daemon);
		}

		@Override
		public void run() {
			ranOn.set(Thread.currentThread());
			started = running = true;
			startedLatch.countDown();
			while(!stopping) {
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
				}
			}
			running = false;
		}

		void awaitStart() throws InterruptedException {
			assertTrue(startedLatch.await(5, TimeUnit.SECONDS), "Thread did not start");
		}
	}

	@Test
	public void testConstructors() {
		Worker w1 = new Worker("worker-one");
		assertEquals("worker-one", w1.getName());
		assertTrue(w1.isDaemon(), "Threads are daemons by default");

		Worker w2 = new Worker(false);
		assertNull(w2.getName());
		assertFalse(w2.isDaemon());

		Worker w3 = new Worker("worker-three", false);
		assertEquals("worker-three", w3.getName());
		assertFalse(w3.isDaemon());
	}

	@Test
	public void testSettersBeforeStart() {
		Worker w = new Worker();
		assertFalse(w.isStopping());
		assertEquals(-1, w.getPriority());
	}

	@Test
	public void testPropertiesAreAppliedAtStart() throws Exception {
		ClassLoader loader = new URLClassLoader(new URL[0]);
		UncaughtExceptionHandler handler = (t, e) -> {};

		Worker w = new Worker("configured-worker", false);
		w.setPriority(Thread.MIN_PRIORITY);
		w.setContextClassLoader(loader);
		w.setUncaughtExceptionHandler(handler);
		w.start();
		try {
			w.awaitStart();
			Thread t = w.ranOn.get();
			assertEquals("configured-worker", t.getName());
			assertFalse(t.isDaemon());
			assertEquals(Thread.MIN_PRIORITY, t.getPriority());
			assertSame(loader, t.getContextClassLoader());
			assertSame(handler, t.getUncaughtExceptionHandler());
		} finally {
			assertTrue(w.stop(5000, true));
		}
	}

	@Test
	public void testSettersWhileRunningUpdateTheThread() throws Exception {
		Worker w = new Worker();
		w.start();
		try {
			w.awaitStart();
			Thread t = w.ranOn.get();
			assertEquals(t.getName(), w.getName(), "The thread name is used when no name was set");

			w.setName("renamed");
			assertEquals("renamed", t.getName());

			w.setPriority(Thread.MAX_PRIORITY - 1);
			assertEquals(Thread.MAX_PRIORITY - 1, t.getPriority());

			ClassLoader loader = new URLClassLoader(new URL[0]);
			w.setContextClassLoader(loader);
			assertSame(loader, t.getContextClassLoader());

			UncaughtExceptionHandler handler = (th, e) -> {};
			w.setUncaughtExceptionHandler(handler);
			assertSame(handler, t.getUncaughtExceptionHandler());
		} finally {
			assertTrue(w.stop(5000, true));
		}
		//  the thread has ended, so the daemon flag can be changed again
		w.setDaemon(false);
		assertFalse(w.isDaemon());
	}

	@Test
	public void testStopAndJoinBeforeStart() throws Exception {
		Worker w = new Worker();
		assertTrue(w.stop(100, false), "Stopping a thread that was never started succeeds");
		assertTrue(w.isStopping());
		//  join on a thread that was never started returns immediately
		w.join(100);
	}

	@Test
	public void testJoin() throws Exception {
		Worker w = new Worker();
		w.start();
		w.awaitStart();
		w.stop();
		w.join(5000);
		assertFalse(w.ranOn.get().isAlive());
		assertFalse(w.isRunning());
	}

	@Test
	public void testStopFromOwnThread() throws Exception {
		AtomicBoolean result = new AtomicBoolean(true);
		CountDownLatch done = new CountDownLatch(1);
		BaseThread thread = new BaseThread() {
			@Override
			public void run() {
				started = running = true;
				try {
					//  must not wait for (join) itself
					result.set(stop(60000, false));
				} catch (InterruptedException e) {
				}
				running = false;
				done.countDown();
			}
		};
		thread.start();
		assertTrue(done.await(5, TimeUnit.SECONDS), "stop() from the thread itself must not block");
		assertFalse(result.get(), "The thread is still alive while it calls stop on itself");
	}
}
