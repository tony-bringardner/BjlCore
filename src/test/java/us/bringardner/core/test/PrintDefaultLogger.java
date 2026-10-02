package us.bringardner.core.test;

import us.bringardner.core.BaseObject;

/**
 * Run in a separate JVM by TestLog4jProvider: prints the class of the logger BaseObject chooses by default.
 */
public class PrintDefaultLogger {
	public static void main(String[] args) {
		System.out.println("LOGGER="+BaseObject.findLogger(PrintDefaultLogger.class.getName()).getClass().getName());
	}
}
