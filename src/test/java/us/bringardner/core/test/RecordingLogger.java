package us.bringardner.core.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import us.bringardner.core.ILogger;

/**
 * An ILogger that records every message it is asked to log (respecting its level)
 * so tests can verify what was logged without parsing console output.
 * Only the abstract methods are implemented, so the ILogger default (Supplier) methods are exercised as-is.
 */
public class RecordingLogger implements ILogger {

	public final List<String> messages = Collections.synchronizedList(new ArrayList<>());
	private volatile Level level = Level.DEBUG;
	private volatile String name;

	public String getName() {
		return name;
	}

	private void record(Level target, String msg, Throwable error) {
		if( isEnabled(target)) {
			messages.add(target+" "+msg+(error == null ? "" : " "+error.getMessage()));
		}
	}

	private boolean isEnabled(Level target) {
		return level != Level.NONE && level.ordinal() >= target.ordinal();
	}

	@Override
	public void init(String name) {
		this.name = name;
	}

	@Override
	public void setLevel(Level level) {
		this.level = level == null ? Level.NONE : level;
	}

	@Override
	public Level getLevel() {
		return level;
	}

	@Override
	public void info(String msg) {
		record(Level.INFO, msg, null);
	}

	@Override
	public void info(String msg, Throwable error) {
		record(Level.INFO, msg, error);
	}

	@Override
	public boolean isInfoEnabled() {
		return isEnabled(Level.INFO);
	}

	@Override
	public void error(String msg) {
		record(Level.ERROR, msg, null);
	}

	@Override
	public void error(String msg, Throwable error) {
		record(Level.ERROR, msg, error);
	}

	@Override
	public boolean isErrorEnabled() {
		return isEnabled(Level.ERROR);
	}

	@Override
	public void debug(String msg) {
		record(Level.DEBUG, msg, null);
	}

	@Override
	public void debug(String msg, Throwable error) {
		record(Level.DEBUG, msg, error);
	}

	@Override
	public boolean isDebugEnabled() {
		return isEnabled(Level.DEBUG);
	}

	@Override
	public void warn(String msg) {
		record(Level.WARN, msg, null);
	}

	@Override
	public void warn(String msg, Throwable error) {
		record(Level.WARN, msg, error);
	}

	@Override
	public boolean isWarnEnabled() {
		return isEnabled(Level.WARN);
	}
}
