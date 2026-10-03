package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import us.bringardner.core.swing.Clock;
import us.bringardner.core.swing.DayPanel;

/**
 * DayPanel in locales where the week starts on Monday, and Clock painting without a display,
 * at zero size and at 2x (Retina) scale. None of these need a display.
 */
public class TestSwingFixes {

	// ---------------- DayPanel ----------------

	interface LocaleTest {
		void run() throws Exception;
	}

	private static void withLocale(Locale locale, LocaleTest test) throws Exception {
		Locale saved = Locale.getDefault();
		Locale savedFormat = Locale.getDefault(Locale.Category.FORMAT);
		Locale savedDisplay = Locale.getDefault(Locale.Category.DISPLAY);
		Locale.setDefault(locale);
		try {
			test.run();
		} finally {
			Locale.setDefault(saved);
			Locale.setDefault(Locale.Category.FORMAT, savedFormat);
			Locale.setDefault(Locale.Category.DISPLAY, savedDisplay);
		}
	}

	private static Date feb15th2020() {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		//  February 1st 2020 was a Saturday
		cal.set(2020, Calendar.FEBRUARY, 15, 12, 0);
		return cal.getTime();
	}

	private static List<JTextField> days(DayPanel panel) {
		return SwingTestUtil.field(panel, "days");
	}

	private static String text(Component c) {
		return ((JLabel) c).getText().trim();
	}

	private static String dayName(DayOfWeek day, Locale locale) {
		return day.getDisplayName(TextStyle.NARROW, locale);
	}

	private static void click(JTextField field) {
		MouseEvent e = new MouseEvent(field, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 1, 1, 1, false);
		for(MouseListener l : field.getMouseListeners()) {
			if( l.getClass().getName().startsWith(DayPanel.class.getName())) {
				l.mouseClicked(e);
			}
		}
	}

	@Test
	public void testDayPanelMondayFirst() throws Exception {
		withLocale(Locale.GERMANY, () -> {
			DayPanel panel = new DayPanel(feb15th2020());
			List<JTextField> days = days(panel);

			//  Before the fix the month started on the 2nd (day 1 was missing) and clicks selected the wrong day
			assertEquals(29, days.size(), "February 2020 has 29 days");
			assertEquals("01", days.get(0).getText());
			assertEquals("29", days.get(28).getText());
			assertEquals(Color.blue, days.get(14).getBackground(), "The 15th is selected");
			assertEquals("15", days.get(14).getText());

			//  The header starts on Monday and ends on Sunday
			assertEquals(dayName(DayOfWeek.MONDAY, Locale.GERMANY), text(panel.getComponent(0)));
			assertEquals(dayName(DayOfWeek.SATURDAY, Locale.GERMANY), text(panel.getComponent(5)));
			assertEquals(dayName(DayOfWeek.SUNDAY, Locale.GERMANY), text(panel.getComponent(6)));

			//  Saturday the 1st is in the 6th column, after Monday 27 ... Friday 31 January
			assertEquals("27", text(panel.getComponent(7)));
			assertEquals("31", text(panel.getComponent(11)));
			assertSame(days.get(0), panel.getComponent(12));
			assertEquals(0, panel.getComponentCount() % 7, "Every week is complete");

			List<PropertyChangeEvent> events = new ArrayList<>();
			panel.addPropertyChangeListener(DayPanel.PROP_DAY, events::add);
			click(days.get(19));
			assertEquals(1, events.size());
			assertEquals(Integer.valueOf(20), events.get(0).getNewValue());
			assertEquals(Color.blue, days.get(19).getBackground());
			assertEquals(Color.white, days.get(14).getBackground());
		});
	}

	@Test
	public void testDayPanelSundayFirstIsUnchanged() throws Exception {
		withLocale(Locale.US, () -> {
			DayPanel panel = new DayPanel(feb15th2020());
			List<JTextField> days = days(panel);
			assertEquals(29, days.size());

			String[] header = new String[7];
			for(int i=0; i < 7; i++ ) {
				header[i] = text(panel.getComponent(i));
			}
			assertEquals("S M T W T F S", String.join(" ", header));

			//  Saturday the 1st is in the last column, after Sunday 26 ... Friday 31 January
			assertEquals("26", text(panel.getComponent(7)));
			assertSame(days.get(0), panel.getComponent(13));
			assertEquals(0, panel.getComponentCount() % 7, "Every week is complete");
		});
	}

	@Test
	public void testDayPanelMonthStartingOnTheFirstDayOfTheWeek() throws Exception {
		withLocale(Locale.US, () -> {
			Calendar cal = Calendar.getInstance();
			cal.clear();
			//  March 1st 2020 was a Sunday
			cal.set(2020, Calendar.MARCH, 10);
			DayPanel panel = new DayPanel(cal.getTime());
			assertSame(days(panel).get(0), panel.getComponent(7), "No days of the previous month");
			assertEquals(31, days(panel).size());
			assertEquals(0, panel.getComponentCount() % 7);
		});
	}

	// ---------------- Clock ----------------

	private static Clock clock(int size) {
		Clock clock = new Clock();
		clock.setHour(3);
		clock.setMinute(0);
		clock.setSize(size, size);
		return clock;
	}

	private static BufferedImage paint(Clock clock, int imageSize, double scale) {
		BufferedImage img = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		try {
			g.scale(scale, scale);
			clock.paint(g);
		} finally {
			g.dispose();
		}
		return img;
	}

	@Test
	public void testClockPaintsWithoutADisplay() {
		//  Before the fix paint() threw a NullPointerException: createImage() returns null until the clock is in a frame
		Clock clock = clock(200);
		BufferedImage img = paint(clock, 200, 1);
		assertTrue((img.getRGB(100, 100) >>> 24) != 0, "The clock face was painted");
		//  The hour hand points at 3 o'clock, the minute hand at 12
		assertTrue(img.getRGB(100+20, 100) != img.getRGB(100-20, 100), "The hour hand was painted");
		assertTrue(img.getRGB(100, 100-30) != img.getRGB(100, 100+30), "The minute hand was painted");
	}

	@Test
	public void testClockWithNoSize() {
		Clock clock = clock(0);
		paint(clock, 10, 1);
		clock.setSize(1, 1);
		paint(clock, 10, 1);
	}

	@Test
	public void testClockPressBeforePaint() {
		//  Before the fix the hand positions were null until the first paint
		Clock clock = clock(200);
		clock.mousePressed(new MouseEvent(clock, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 100, 100, 1, false));
		clock.mouseReleased(new MouseEvent(clock, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, 100, 100, 1, false));
		assertEquals(3, clock.getHour());
		assertEquals(0, clock.getMinute());
	}

	@Test
	public void testClockIsSharpAtDoubleScale() {
		//  On a Retina display Swing paints with a 2x scale. The clock used to draw a 1x image and let it
		//  be stretched, so every 2x2 block of pixels was the same. Drawn at 2x, the edges have detail.
		Clock clock = clock(200);
		BufferedImage img = paint(clock, 400, 2);
		int detailed = 0;
		for(int y=0; y < 400; y+=2 ) {
			for(int x=0; x < 400; x+=2 ) {
				int p = img.getRGB(x, y);
				if( p != img.getRGB(x+1, y) || p != img.getRGB(x, y+1) || p != img.getRGB(x+1, y+1) ) {
					detailed++;
				}
			}
		}
		assertTrue(detailed > 200, "Only "+detailed+" 2x2 blocks have more than one color");
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testDeprecatedCreateGraphics2D() {
		Clock clock = clock(0);
		Graphics2D g = clock.createGraphics2D(0, 0);
		try {
			g.drawLine(0, 0, 1, 1);
		} finally {
			g.dispose();
		}
	}
}
