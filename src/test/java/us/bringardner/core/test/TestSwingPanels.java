package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static us.bringardner.core.test.SwingTestUtil.named;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import us.bringardner.core.swing.Clock;
import us.bringardner.core.swing.DatePanel;
import us.bringardner.core.swing.DateTimeCombo;
import us.bringardner.core.swing.DayPanel;
import us.bringardner.core.swing.TimePanel;

/**
 * Tests for the Swing panels that do not need a display (they also run with java.awt.headless=true).
 */
@ExtendWith(RunOnEdt.class)
public class TestSwingPanels {

	private boolean military;

	@BeforeEach
	public void saveMilitary() {
		military = TimePanel.isMilitary();
		TimePanel.setMilitary(false);
	}

	@AfterEach
	public void restoreMilitary() {
		TimePanel.setMilitary(military);
	}

	static Date date(int year, int month, int day, int hour, int minute, int second, int millis) {
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(year, month, day, hour, minute, second);
		cal.set(Calendar.MILLISECOND, millis);
		return cal.getTime();
	}

	static int intValue(JSpinner spinner) {
		return ((Integer) spinner.getValue()).intValue();
	}

	// ---------------- Clock ----------------

	@Test
	public void testClockProperties() {
		Clock clock = new Clock(date(2020, Calendar.JANUARY, 1, 0, 7, 8, 9).getTime());
		assertEquals(12, clock.getHour(), "Midnight is shown as 12");
		assertEquals(0, clock.getHourOfDay());
		assertEquals(7, clock.getMinute());
		assertEquals(8, clock.getSeconds());
		assertEquals(9, clock.getMilliSeconds());

		clock.setHour(9);
		clock.setMinute(45);
		assertEquals(9, clock.getHour());
		assertEquals(9, clock.getHourOfDay());
		assertEquals(45, clock.getMinute());

		clock.setHour(12);
		assertEquals(12, clock.getHour());
		assertEquals(12, clock.getHourOfDay());

		assertTrue(clock.isShowText());
		clock.setShowText(false);
		assertFalse(clock.isShowText());

		clock.setHourColor(Color.ORANGE);
		clock.setMinuteColor(Color.PINK);
		clock.setClockBackground(Color.WHITE);
		clock.setClockForground(Color.BLUE);
		clock.setShadowColor(Color.GRAY);
		assertEquals(Color.ORANGE, clock.getHourColor());
		assertEquals(Color.PINK, clock.getMinuteColor());
		assertEquals(Color.WHITE, clock.getClockBackground());
		assertEquals(Color.BLUE, clock.getClockForground());
		assertEquals(Color.GRAY, clock.getShadowColor());


		//  the default constructor uses the current time
		assertTrue(new Clock().getHour() >= 1);
	}

	@Test
	public void testClockHourOfDayAfternoon() {
		Clock clock = new Clock();
		clock.setHour(15);
		assertEquals(3, clock.getHour());
		assertEquals(15, clock.getHourOfDay());
	}

	@Test
	public void testClockHourOfDayFromConstructor() {
		Clock clock = new Clock(date(2020, Calendar.JANUARY, 1, 15, 0, 0, 0).getTime());
		assertEquals(15, clock.getHourOfDay());
	}

	// ---------------- TimePanel ----------------

	@Test
	public void testTimePanelTwelveHour() {
		TimePanel panel = new TimePanel(date(2020, Calendar.JANUARY, 1, 14, 25, 36, 789).getTime());
		JSpinner hour = named(panel, "hourSpinner", JSpinner.class);
		JRadioButton pm = named(panel, "pmRadio", JRadioButton.class);
		JRadioButton am = named(panel, "amRadio", JRadioButton.class);

		assertEquals(2, intValue(hour));
		assertTrue(pm.isSelected());
		assertEquals(14, panel.getHour());
		assertEquals(25, panel.getMinute());

		panel.setHour(0);
		assertEquals(12, intValue(hour), "Midnight is 12 AM");
		assertTrue(am.isSelected());
		assertEquals(0, panel.getHour());

		panel.setHour(12);
		assertEquals(12, intValue(hour), "Noon is 12 PM");
		assertTrue(pm.isSelected());
		assertEquals(12, panel.getHour());

		panel.setHour(99);
		assertEquals(23, panel.getHour(), "Hours are limited to 23");

	}

	@Test
	public void testTimePanelMilitary() {
		TimePanel.setMilitary(true);
		assertTrue(TimePanel.isMilitary());
		TimePanel panel = new TimePanel(date(2020, Calendar.JANUARY, 1, 9, 5, 0, 0).getTime());
		JSpinner hour = named(panel, "hourSpinner", JSpinner.class);
		JCheckBox box = named(panel, "militaryTimeCheckbox", JCheckBox.class);
		JRadioButton pm = named(panel, "pmRadio", JRadioButton.class);
		assertTrue(box.isSelected());
		assertFalse(pm.isVisible(), "AM/PM is hidden in 24 hour mode");

		panel.setHour(17);
		assertEquals(17, intValue(hour));
		assertEquals(17, panel.getHour());
		assertTrue(pm.isSelected());
		panel.setHour(3);
		assertEquals(3, panel.getHour());
	}

	@Test
	public void testTimePanelToggleMilitaryKeepsTheHour() {
		TimePanel panel = new TimePanel(date(2020, Calendar.JANUARY, 1, 14, 0, 0, 0).getTime());
		JSpinner hour = named(panel, "hourSpinner", JSpinner.class);
		JCheckBox box = named(panel, "militaryTimeCheckbox", JCheckBox.class);

		box.doClick();
		assertTrue(TimePanel.isMilitary());
		assertEquals(14, intValue(hour));
		assertEquals(14, panel.getHour());

		box.doClick();
		assertFalse(TimePanel.isMilitary());
		assertEquals(2, intValue(hour));
		assertEquals(14, panel.getHour());
	}

	@Test
	public void testTimePanelSetAndGetTime() {
		TimePanel panel = new TimePanel(date(2020, Calendar.JANUARY, 1, 14, 25, 36, 789).getTime());

		//  seconds and milliseconds are only returned when they are editable
		Calendar cal = panel.getTime();
		assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(25, cal.get(Calendar.MINUTE));
		assertEquals(0, cal.get(Calendar.SECOND));
		assertEquals(0, cal.get(Calendar.MILLISECOND));

		panel.setEditSeconds(true);
		panel.setEditMilliSeconds(true);
		assertTrue(panel.isEditSeconds());
		assertTrue(panel.isEditMilliSeconds());
		cal = panel.getTime();
		assertEquals(36, cal.get(Calendar.SECOND));
		assertEquals(789, cal.get(Calendar.MILLISECOND));
		panel.setSeconds(10);
		panel.setMilliSeconds(20);
		assertEquals(10, panel.getSecond());
		assertEquals(20, panel.getMillisSecond());

		panel.setTime(date(2021, Calendar.MARCH, 3, 8, 15, 0, 0));
		assertEquals(8, panel.getHour());
		assertEquals(15, panel.getMinute());

		panel.setTime(date(2021, Calendar.MARCH, 3, 23, 59, 0, 0).getTime());
		assertEquals(23, panel.getHour());
		assertEquals(59, panel.getMinute());

		Calendar c = Calendar.getInstance();
		c.setTime(date(2021, Calendar.MARCH, 3, 6, 1, 0, 0));
		panel.setTime(c);
		assertEquals(6, panel.getHour());
		assertEquals(1, panel.getMinute());
	}

	@Test
	public void testTimePanelPreferredSize() {
		TimePanel panel = new TimePanel();
		assertTrue(panel.isShowAnalog());
		int full = panel.getPreferredSize().height;
		int narrow = panel.getPreferredSize().width;

		panel.setShowAnalog(false);
		assertFalse(panel.isShowAnalog());
		assertTrue(panel.getPreferredSize().height < full, "Hiding the clock makes the panel shorter");

		panel.setEditSeconds(true);
		panel.setEditMilliSeconds(true);
		assertTrue(panel.getPreferredSize().width > narrow, "Editing seconds and milliseconds makes the panel wider");
		panel.setEditSeconds(false);
		panel.setEditMilliSeconds(false);
		assertEquals(narrow, panel.getPreferredSize().width);
	}

	@Test
	public void testTimePanelNowButton() {
		TimePanel panel = new TimePanel(date(2000, Calendar.JANUARY, 1, 0, 0, 0, 0).getTime());
		Calendar before = Calendar.getInstance();
		named(panel, "btnCurrentTime", JButton.class).doClick();
		Calendar after = Calendar.getInstance();
		int hour = panel.getHour();
		assertTrue(hour == before.get(Calendar.HOUR_OF_DAY) || hour == after.get(Calendar.HOUR_OF_DAY));
		int minute = panel.getMinute();
		assertTrue(minute == before.get(Calendar.MINUTE) || minute == after.get(Calendar.MINUTE));
	}

	@Test
	public void testTimePanelSpinnersAndClockStayInSync() {
		TimePanel panel = new TimePanel(date(2020, Calendar.JANUARY, 1, 10, 30, 0, 0).getTime());
		Clock clock = named(panel, "clock", Clock.class);
		JSpinner hour = named(panel, "hourSpinner", JSpinner.class);
		JSpinner minute = named(panel, "minuteSpinner", JSpinner.class);
		List<PropertyChangeEvent> events = new ArrayList<>();
		panel.addPropertyChangeListener(e -> {
			if( "".equals(e.getPropertyName())) {
				events.add(e);
			}
		});

		//  spinner -> clock
		minute.setValue(45);
		assertEquals(45, clock.getMinute());
		assertEquals(1, events.size(), "A minute change fires a property change");
		assertEquals(Integer.valueOf(45), events.get(0).getNewValue());
		hour.setValue(4);
		assertEquals(4, clock.getHour());

		//  clock -> spinner (as when the user drags a clock hand)
		clock.setHour(7);
		clock.firePropertyChange(Clock.PROP_HOUR_CHANGED, 4, 7);
		assertEquals(7, intValue(hour));
		clock.setMinute(12);
		clock.firePropertyChange(Clock.PROP_MIN_CHANGED, 45, 12);
		assertEquals(12, intValue(minute));

		TimePanel.setMilitary(true);
		clock.setHour(8);
		clock.firePropertyChange(Clock.PROP_HOUR_CHANGED, 7, 8);
		assertEquals(clock.getHourOfDay(), intValue(hour), "In 24 hour mode the hour of day is used");
	}

	// ---------------- DayPanel / DatePanel ----------------

	private static List<JTextField> days(DayPanel panel) {
		return SwingTestUtil.days(panel);
	}

	/** Deliver a mouse event to the DayPanel listener of a day field. */
	private static void mouse(JTextField field, int id) {
		MouseEvent e = new MouseEvent(field, id, System.currentTimeMillis(), 0, 1, 1, 1, false);
		for(MouseListener l : field.getMouseListeners()) {
			if( l.getClass().getName().startsWith(DayPanel.class.getName())) {
				switch (id) {
				case MouseEvent.MOUSE_CLICKED: l.mouseClicked(e); break;
				case MouseEvent.MOUSE_ENTERED: l.mouseEntered(e); break;
				case MouseEvent.MOUSE_EXITED: l.mouseExited(e); break;
				default: throw new IllegalArgumentException("id="+id);
				}
			}
		}
	}

	@Test
	public void testDayPanelSelectDay() {
		DayPanel panel = new DayPanel(date(2020, Calendar.FEBRUARY, 15, 12, 0, 0, 0));
		List<JTextField> days = days(panel);
		assertEquals(29, days.size(), "February 2020 has 29 days");
		assertEquals(Color.blue, days.get(14).getBackground(), "The selected day is highlighted");

		List<PropertyChangeEvent> events = new ArrayList<>();
		panel.addPropertyChangeListener(DayPanel.PROP_DAY, events::add);

		mouse(days.get(14), MouseEvent.MOUSE_CLICKED);
		assertTrue(events.isEmpty(), "Clicking the selected day does nothing");

		mouse(days.get(19), MouseEvent.MOUSE_CLICKED);
		assertEquals(1, events.size());
		assertEquals(Integer.valueOf(15), events.get(0).getOldValue());
		assertEquals(Integer.valueOf(20), events.get(0).getNewValue());
		assertEquals(Color.blue, days.get(19).getBackground());
		assertEquals(Color.white, days.get(14).getBackground());

		mouse(days.get(3), MouseEvent.MOUSE_ENTERED);
		assertTrue(days.get(3).getBorder() instanceof LineBorder, "Hovering shows a border");
		mouse(days.get(3), MouseEvent.MOUSE_EXITED);
		assertNull(days.get(3).getBorder());

	}

	@Test
	public void testDatePanelDayChangeIsForwarded() {
		DatePanel panel = new DatePanel(date(2020, Calendar.FEBRUARY, 15, 12, 0, 0, 0));
		List<PropertyChangeEvent> events = new ArrayList<>();
		panel.addPropertyChangeListener(DatePanel.PROP_DATE_CHANGED, events::add);

		DayPanel dayPanel = named(panel, "dayPanel1", DayPanel.class);
		mouse(days(dayPanel).get(19), MouseEvent.MOUSE_CLICKED);

		assertEquals(1, events.size());
		Calendar cal = Calendar.getInstance();
		cal.setTime(panel.getDate());
		assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));

	}

	@Test
	public void testDatePanelTodayButton() {
		DatePanel panel = new DatePanel();
		panel.setDate(date(1999, Calendar.DECEMBER, 31, 0, 0, 0, 0));
		named(panel, "todayButton", JButton.class).doClick();
		Calendar today = Calendar.getInstance();
		Calendar cal = Calendar.getInstance();
		cal.setTime(panel.getDate());
		assertEquals(today.get(Calendar.YEAR), cal.get(Calendar.YEAR));
		assertEquals(today.get(Calendar.DAY_OF_YEAR), cal.get(Calendar.DAY_OF_YEAR));
	}

	// ---------------- DateTimeCombo ----------------

	@Test
	public void testDateTimeCombo() {
		Date date = date(2020, Calendar.JULY, 4, 12, 30, 0, 0);
		assertEquals(date, new DateTimeCombo(date).getDate());
		assertEquals(date, new DateTimeCombo(date.getTime()).getDate());

		DateTimeCombo combo = new DateTimeCombo();
		assertEquals(new Date(1392699600000L), combo.getDate());
		combo.setDate(date);
		assertEquals(date, combo.getDate());
		assertNull(combo.getLabel());
		combo.setLabel("When");
		assertEquals("When", combo.getLabel());
	}
}
