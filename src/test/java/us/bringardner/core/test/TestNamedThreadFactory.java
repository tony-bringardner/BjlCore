package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import us.bringardner.core.NamedThreadFactory;

public class TestNamedThreadFactory {

	private static final Runnable NOTHING = () -> { };

	@Test
	public void testNames() {
		NamedThreadFactory f = new NamedThreadFactory("Worker");
		assertEquals("Worker", f.newThread(NOTHING).getName(), "The first thread has just the name");
		assertEquals("Worker-2", f.newThread(NOTHING).getName());
		assertEquals("Worker-3", f.newThread(NOTHING).getName());

		NamedThreadFactory n = NamedThreadFactory.numbered("TCPConn");
		assertEquals("TCPConn1", n.newThread(NOTHING).getName());
		assertEquals("TCPConn2", n.newThread(NOTHING).getName());
	}

	@Test
	public void testDaemon() {
		NamedThreadFactory f = new NamedThreadFactory("D");
		assertTrue(f.newThread(NOTHING).isDaemon(), "Daemon by default");
		assertFalse(f.daemon(false).newThread(NOTHING).isDaemon());
		assertTrue(f.daemon(false).daemon(true).newThread(NOTHING).isDaemon());
		assertThrows(NullPointerException.class, () -> new NamedThreadFactory(null));
	}

	@Test
	public void testWithAnExecutor() throws Exception {
		ExecutorService ex = Executors.newSingleThreadExecutor(new NamedThreadFactory("SingleOne"));
		try {
			assertEquals("SingleOne", ex.submit(() -> Thread.currentThread().getName()).get(5, TimeUnit.SECONDS));
			assertTrue(ex.submit(() -> Thread.currentThread().isDaemon()).get(5, TimeUnit.SECONDS));
		} finally {
			ex.shutdownNow();
		}
	}

	@Test
	public void testVirtual() throws Exception {
		//  A virtual thread on Java 21+ (from the jar), a platform daemon thread otherwise; it runs either way
		Thread t = new NamedThreadFactory("V").virtual(true).newThread(NOTHING);
		assertEquals("V", t.getName());
		assertTrue(t.isDaemon());
		t.start();
		t.join(5000);
		assertFalse(t.isAlive());
	}
}
