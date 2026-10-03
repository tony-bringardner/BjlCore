package us.bringardner.core.test;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractButton;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.junit.jupiter.api.Assumptions;

/**
 * Helpers for the Swing tests.
 */
public class SwingTestUtil {

	/** Skip the calling test when there is no display (e.g. a headless CI build). */
	public static void assumeDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Requires a display (java.awt.headless=true)");
	}

	/**
	 * Read a private field (searching super classes). Only for state that isn't a component,
	 * such as the Clock's drag handles; find components with {@link #named}.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T field(Object target, String name) {
		for(Class<?> cls = target.getClass(); cls != null; cls = cls.getSuperclass()) {
			try {
				Field f = cls.getDeclaredField(name);
				f.setAccessible(true);
				return (T) f.get(target);
			} catch (NoSuchFieldException e) {
				//  try the super class
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
		throw new IllegalArgumentException("No field "+name+" in "+target.getClass());
	}

	/** Find all components of the given type in a container (depth first). */
	public static <T> List<T> find(Container root, Class<T> type) {
		List<T> ret = new ArrayList<>();
		for(Component c : root.getComponents()) {
			if( type.isInstance(c)) {
				ret.add(type.cast(c));
			}
			if( c instanceof Container) {
				ret.addAll(find((Container) c, type));
			}
		}
		return ret;
	}

	/**
	 * Find a component by its name (see {@link Component#setName(String)}).
	 *
	 * @throws IllegalArgumentException if there is no such component, or it isn't of the given type
	 */
	public static <T extends Component> T named(Container root, String name, Class<T> type) {
		for(Component c : find(root, Component.class)) {
			if( name.equals(c.getName())) {
				if( !type.isInstance(c)) {
					throw new IllegalArgumentException("'"+name+"' is a "+c.getClass().getName()+", not a "+type.getName());
				}
				return type.cast(c);
			}
		}
		throw new IllegalArgumentException("No component named '"+name+"' in "+root.getClass().getName());
	}

	/** The day of the month fields of a DayPanel (named day1, day2 ...), in order. */
	public static List<JTextField> days(Container dayPanel) {
		List<JTextField> ret = new ArrayList<>();
		for(int day=1; ; day++ ) {
			try {
				ret.add(named(dayPanel, "day"+day, JTextField.class));
			} catch (IllegalArgumentException e) {
				return ret;
			}
		}
	}

	/** Find a button by its text. */
	public static AbstractButton button(Container root, String text) {
		for(AbstractButton b : find(root, AbstractButton.class)) {
			if( text.equals(b.getText())) {
				return b;
			}
		}
		throw new IllegalArgumentException("No button '"+text+"'");
	}

	/**
	 * Start a Swing timer that waits for a visible window of the given type and then clicks
	 * the given buttons (one per tick, in order). Use before calling a method that shows a
	 * modal dialog. If the window does not show up (or a button is missing) the window is
	 * disposed after about 10 seconds so a broken test can't hang the build.
	 */
	public static Timer clickWhenShowing(Class<? extends Window> type, String ... buttons) {
		List<String> todo = new ArrayList<>(Arrays.asList(buttons));
		AtomicInteger ticks = new AtomicInteger();
		Timer timer = new Timer(100, null);
		timer.addActionListener(e -> {
			for(Window w : Window.getWindows()) {
				if( type.isInstance(w) && w.isShowing()) {
					if( ticks.incrementAndGet() > 100 || todo.isEmpty()) {
						w.dispose();
						timer.stop();
					} else {
						try {
							button(w, todo.remove(0)).doClick();
						} catch (IllegalArgumentException ex) {
							ex.printStackTrace();
							w.dispose();
							timer.stop();
						}
						if( todo.isEmpty()) {
							timer.stop();
						}
					}
					return;
				}
			}
			if( ticks.incrementAndGet() > 100) {
				timer.stop();
			}
		});
		timer.start();
		return timer;
	}
}
