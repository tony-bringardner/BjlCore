package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * RunOnEdt runs tests and their set up and tear down on the Swing event dispatch thread.
 */
@ExtendWith(RunOnEdt.class)
public class TestRunOnEdt {

	private boolean beforeOnEdt;

	@BeforeEach
	public void setUp() {
		beforeOnEdt = SwingUtilities.isEventDispatchThread();
	}

	@AfterEach
	public void tearDown() {
		assertTrue(SwingUtilities.isEventDispatchThread(), "@AfterEach should run on the event thread");
	}

	@Test
	public void testRunsOnEdt() {
		assertTrue(beforeOnEdt, "@BeforeEach should run on the event thread");
		assertTrue(SwingUtilities.isEventDispatchThread(), "The test should run on the event thread");
	}
}
