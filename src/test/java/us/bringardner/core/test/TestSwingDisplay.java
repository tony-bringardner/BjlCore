package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static us.bringardner.core.test.SwingTestUtil.assumeDisplay;
import static us.bringardner.core.test.SwingTestUtil.button;
import static us.bringardner.core.test.SwingTestUtil.clickWhenShowing;
import static us.bringardner.core.test.SwingTestUtil.field;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.bringardner.core.swing.Clock;
import us.bringardner.core.swing.DateAndTimeDialog;
import us.bringardner.core.swing.DateDialog;
import us.bringardner.core.swing.DateTimeCombo;
import us.bringardner.core.swing.TimeDialog;
import us.bringardner.core.swing.TimePanel;

/**
 * Swing tests that need a display: painting the Clock, dragging its hands, and the
 * modal dialogs (which are closed automatically by clicking their buttons).
 * They are skipped when java.awt.headless=true.
 */
public class TestSwingDisplay {

	private final List<JFrame> frames = new ArrayList<>();
	private boolean military;

	@BeforeEach
	public void setUp() {
		military = TimePanel.isMilitary();
		TimePanel.setMilitary(false);
	}

	@AfterEach
	public void tearDown() {
		TimePanel.setMilitary(military);
		for(JFrame f : frames) {
			f.dispose();
		}
	}

	static Date date(int year, int month, int day, int hour, int minute) {
		return TestSwingPanels.date(year, month, day, hour, minute, 0, 0);
	}

	static Calendar cal(Date date) {
		Calendar ret = Calendar.getInstance();
		ret.setTime(date);
		return ret;
	}

	// ---------------- Clock painting and mouse handling ----------------

	/** Put the clock in a (not visible) frame so it can create its off screen image. */
	private Clock displayableClock() {
		Clock clock = new Clock();
		JFrame frame = new JFrame();
		frames.add(frame);
		frame.add(clock);
		frame.pack();
		return clock;
	}

	private static void paint(Clock clock) {
		BufferedImage img = new BufferedImage(clock.getWidth(), clock.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try {
			clock.paint(g);
		} finally {
			g.dispose();
		}
	}

	/** The point on the clock face for a position in minutes (0-60). */
	private static Point at(Clock clock, double minutes) {
		double a = minutes * 2 * Math.PI / 60;
		int cx = clock.getWidth()/2;
		int cy = clock.getHeight()/2;
		int r = Math.min(cx, cy)/2;
		return new Point((int)Math.round(cx + r * Math.sin(a)), (int)Math.round(cy - r * Math.cos(a)));
	}

	private static MouseEvent event(Clock clock, int id, Point p) {
		return new MouseEvent(clock, id, System.currentTimeMillis(), 0, p.x, p.y, 1, false);
	}

	private static Point center(Ellipse2D shape) {
		return new Point((int)shape.getCenterX(), (int)shape.getCenterY());
	}

	/** Press on a hand, drag through the given positions and release on the last one. */
	private static void drag(Clock clock, Ellipse2D hand, double ... minutes) {
		//  entering shows the drag points, pressing selects (fills) one of them
		clock.mouseEntered(event(clock, MouseEvent.MOUSE_ENTERED, center(hand)));
		clock.mousePressed(event(clock, MouseEvent.MOUSE_PRESSED, center(hand)));
		paint(clock);
		Point p = null;
		for(double m : minutes) {
			p = at(clock, m);
			clock.mouseDragged(event(clock, MouseEvent.MOUSE_DRAGGED, p));
		}
		clock.mouseReleased(event(clock, MouseEvent.MOUSE_RELEASED, p));
	}

	@Test
	public void testClockPaint() {
		assumeDisplay();
		Clock clock = displayableClock();
		clock.setHour(3);
		clock.setMinute(0);
		paint(clock);
		//  show the drag points and paint again (re-uses the off screen image)
		clock.mouseEntered(event(clock, MouseEvent.MOUSE_ENTERED, new Point(0, 0)));
		BufferedImage img = new BufferedImage(clock.getWidth(), clock.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		clock.update(g);
		g.dispose();
		clock.setShowText(false);
		paint(clock);
		clock.mouseExited(event(clock, MouseEvent.MOUSE_EXITED, new Point(0, 0)));
		clock.mouseClicked(event(clock, MouseEvent.MOUSE_CLICKED, new Point(0, 0)));
		clock.mouseMoved(event(clock, MouseEvent.MOUSE_MOVED, new Point(0, 0)));
		//  the center of the clock face is not transparent
		assertTrue((img.getRGB(clock.getWidth()/2, clock.getHeight()/2) >>> 24) != 0);
	}

	@Test
	public void testClockDragMinuteHand() {
		assumeDisplay();
		Clock clock = displayableClock();
		clock.setHour(3);
		clock.setMinute(20);
		List<PropertyChangeEvent> events = new ArrayList<>();
		clock.addPropertyChangeListener(Clock.PROP_MIN_CHANGED, events::add);
		clock.addPropertyChangeListener(Clock.PROP_HOUR_CHANGED, events::add);

		paint(clock);
		//  drag through all four quadrants and the exact center
		clock.mousePressed(event(clock, MouseEvent.MOUSE_PRESSED, center(field(clock, "miPoint"))));
		for(double m : new double[] {25, 35, 50, 5}) {
			clock.mouseDragged(event(clock, MouseEvent.MOUSE_DRAGGED, at(clock, m)));
		}
		clock.mouseDragged(event(clock, MouseEvent.MOUSE_DRAGGED, new Point(clock.getWidth()/2, clock.getHeight()/2)));
		clock.mouseReleased(event(clock, MouseEvent.MOUSE_RELEASED, at(clock, 40)));

		assertTrue(Math.abs(clock.getMinute() - 40) <= 1, "minute="+clock.getMinute());
		assertEquals(3, clock.getHour());
		assertEquals(1, events.size());
		assertEquals(Clock.PROP_MIN_CHANGED, events.get(0).getPropertyName());
		assertEquals(Integer.valueOf(20), events.get(0).getOldValue());
	}

	@Test
	public void testClockMinuteHandCrossesTheHour() {
		assumeDisplay();
		Clock clock = displayableClock();
		clock.setHour(3);
		clock.setMinute(55);
		paint(clock);
		drag(clock, field(clock, "miPoint"), 5);
		assertEquals(4, clock.getHour(), "Moving past 12 advances the hour");
		assertTrue(clock.getMinute() <= 6);

		paint(clock);
		drag(clock, field(clock, "miPoint"), 55);
		assertEquals(3, clock.getHour(), "Moving back past 12 goes back an hour");

		//  12 -> 1 and 1 -> 12 switch between AM and PM
		clock.setHour(12);
		clock.setMinute(55);
		paint(clock);
		drag(clock, field(clock, "miPoint"), 5);
		assertEquals(1, clock.getHour());
		assertEquals(13, clock.getHourOfDay());
		paint(clock);
		drag(clock, field(clock, "miPoint"), 55);
		assertEquals(12, clock.getHour());
	}

	@Test
	public void testClockDragHourHand() {
		assumeDisplay();
		Clock clock = displayableClock();
		clock.setHour(3);
		clock.setMinute(0);
		List<PropertyChangeEvent> events = new ArrayList<>();
		clock.addPropertyChangeListener(Clock.PROP_HOUR_CHANGED, events::add);
		paint(clock);
		drag(clock, field(clock, "hrPoint"), 9 * 5);
		assertEquals(9, clock.getHour());
		assertEquals(1, events.size());

		paint(clock);
		drag(clock, field(clock, "hrPoint"), 0);
		assertEquals(12, clock.getHour());
	}

	@Test
	public void testClockPressOutsideTheHands() {
		assumeDisplay();
		Clock clock = displayableClock();
		clock.setHour(3);
		clock.setMinute(0);
		paint(clock);
		clock.mousePressed(event(clock, MouseEvent.MOUSE_PRESSED, new Point(1, 1)));
		clock.mouseDragged(event(clock, MouseEvent.MOUSE_DRAGGED, at(clock, 30)));
		assertEquals(0, clock.getMinute(), "Dragging does nothing unless a hand was pressed");
		assertEquals(3, clock.getHour());
	}

	// ---------------- Dialogs ----------------

	@Test
	public void testTimeDialogOk() {
		assumeDisplay();
		TimeDialog dialog = new TimeDialog();
		dialog.setEditSecond(true);
		dialog.setEditMilliSecond(true);
		assertTrue(dialog.isEditSecond());
		assertTrue(dialog.isEditMilliSecond());
		dialog.setEditSecond(false);
		dialog.setEditMilliSecond(false);
		assertFalse(dialog.isEditSecond());
		assertFalse(dialog.isEditMilliSecond());

		Date date = date(2020, Calendar.MAY, 5, 16, 45);
		clickWhenShowing(TimeDialog.class, "OK");
		Date ret = dialog.showDialog(date, "Time", new Point(10, 10));
		assertFalse(dialog.isCanceled());
		Calendar cal = cal(ret);
		assertEquals(16, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(45, cal.get(Calendar.MINUTE));
	}

	@Test
	public void testTimeDialogCancel() {
		assumeDisplay();
		JFrame owner = new JFrame();
		frames.add(owner);
		TimeDialog dialog = new TimeDialog(owner, true);
		Date date = date(2020, Calendar.MAY, 5, 16, 45);
		clickWhenShowing(TimeDialog.class, "Cancel");
		assertSame(date, dialog.showDialog(date, "Time"), "Cancel returns the original date");
		assertTrue(dialog.isCanceled());
	}

	@Test
	public void testDateDialog() {
		assumeDisplay();
		Date date = date(2019, Calendar.OCTOBER, 12, 0, 0);

		DateDialog ok = new DateDialog();
		clickWhenShowing(DateDialog.class, "OK");
		Date ret = ok.showDialog(date, "Date", new Point(5, 5));
		assertFalse(ok.isCanceled());
		assertEquals(date, ret);
		assertNotSame(date, ret);

		DateDialog cancel = new DateDialog();
		clickWhenShowing(DateDialog.class, "Cancel");
		assertSame(date, cancel.showDialog(date, "Date"));
		assertTrue(cancel.isCanceled());
	}

	@Test
	public void testDateAndTimeDialogOk() {
		assumeDisplay();
		DateAndTimeDialog dialog = new DateAndTimeDialog();
		dialog.setEditSecond(true);
		dialog.setEditMilliSecond(true);
		assertTrue(dialog.isEditSecond());
		assertTrue(dialog.isEditMilliSecond());
		dialog.setShowDate(true);
		dialog.setShowTime(true);

		Date date = date(2018, Calendar.JUNE, 30, 21, 10);
		clickWhenShowing(DateAndTimeDialog.class, "OK");
		Calendar ret = cal(dialog.showDialog(date, "Label", new Point(0, 0)));
		assertFalse(dialog.isCanceled());
		assertEquals(2018, ret.get(Calendar.YEAR));
		assertEquals(Calendar.JUNE, ret.get(Calendar.MONTH));
		assertEquals(30, ret.get(Calendar.DAY_OF_MONTH));
		assertEquals(21, ret.get(Calendar.HOUR_OF_DAY));
		assertEquals(10, ret.get(Calendar.MINUTE));
	}

	@Test
	public void testDateAndTimeDialogNowAndCancel() {
		assumeDisplay();
		Date date = date(2018, Calendar.JUNE, 30, 21, 10);

		DateAndTimeDialog now = new DateAndTimeDialog();
		clickWhenShowing(DateAndTimeDialog.class, "Now", "OK");
		Calendar ret = cal(now.showDate(date, new Point(400, 400)));
		assertEquals(Calendar.getInstance().get(Calendar.YEAR), ret.get(Calendar.YEAR), "Now sets today's date");

		DateAndTimeDialog cancel = new DateAndTimeDialog();
		cancel.setShowTime(false);
		cancel.setShowDate(false);
		clickWhenShowing(DateAndTimeDialog.class, "Cancel");
		assertSame(date, cancel.showDialog(date, "Label"));
		assertTrue(cancel.isCanceled());
	}

	@Test
	public void testDateTimeComboBrowse() throws Exception {
		assumeDisplay();
		Date date = date(2017, Calendar.MARCH, 1, 8, 0);
		DateTimeCombo combo = new DateTimeCombo(date);
		combo.setLabel("Start");
		JFrame frame = new JFrame();
		frames.add(frame);
		frame.add(combo);
		frame.pack();
		SwingUtilities.invokeAndWait(() -> frame.setVisible(true));
		long end = System.currentTimeMillis() + 5000;
		while(!combo.isShowing() && System.currentTimeMillis() < end) {
			Thread.sleep(20);
		}

		//  OK keeps the date and time that were in the combo
		clickWhenShowing(DateAndTimeDialog.class, "OK");
		button(combo, "^").doClick();
		Calendar cal = cal(combo.getDate());
		assertEquals(2017, cal.get(Calendar.YEAR));
		assertEquals(8, cal.get(Calendar.HOUR_OF_DAY));

		//  Now + OK changes it to the current date
		clickWhenShowing(DateAndTimeDialog.class, "Now", "OK");
		button(combo, "^").doClick();
		assertEquals(Calendar.getInstance().get(Calendar.YEAR), cal(combo.getDate()).get(Calendar.YEAR));
	}
}
