// ~version~V000.00.01-V000.00.00-
package us.bringardner.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * <PRE>
 *  An implementation of ILogger that uses JUL (java.util.Logger).
 *  
 * Copyright 1998-2026 <A href="http://bringardner.us/tony">Tony Bringardner</A>
 * 
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE> *   
 *   
 *	@author Tony Bringardner   
 *
 */
public class JulLogger implements ILogger {

	private static final String JUL_LOGGING_PROPERTIES = "/JulLogging.properties";
	
	//  The configuration file is only read once per JVM (not every time a logger is created).
	private static final AtomicBoolean configured = new AtomicBoolean(false);
	
	private volatile Logger logger;
	
	
	public Logger getLogger() {
		if( logger == null ) {
			init(null);
		}
		
		return logger;
	}
	
	/**
	 * The java.util.logging level debug messages are logged at. It was FINEST, so with the level
	 * set to FINE (or FINER) {@link #getLevel()} said DEBUG but debug messages were not logged.
	 */
	private static final java.util.logging.Level DEBUG_LEVEL = java.util.logging.Level.FINE;

	//  Stack frames skipped when finding the code that logged (logging "wrapper" classes)
	private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	public void debug(String msg) {		
		log(DEBUG_LEVEL, msg, null);	
	}

	public void debug(String msg, Throwable error) {
		log(DEBUG_LEVEL, msg, error);
	}

	private void log(java.util.logging.Level level, String msg, Throwable error) {
		Logger tmp = getLogger();
		if( tmp == null ) {
			System.err.println("No logger availible for "+msg);
			if( error != null ) {
				error.printStackTrace(System.err);
			}
		} else if( tmp.isLoggable(level) ) {
			//  Name the code that logged as the source. Without it java.util.logging works out the
			//  source itself, and reported JulLogger.log for every message.
			StackWalker.StackFrame caller = findCaller();
			if( caller != null ) {
				tmp.logp(level, caller.getClassName(), caller.getMethodName(), msg, error);
			} else {
				tmp.log(level, msg, error);
			}
		}
	}

	/**
	 * @return the first stack frame that isn't in a logging class (this one, a subclass of it,
	 * BaseObject's logError etc. or the ILogger Supplier methods), or null if there is none.
	 */
	private static StackWalker.StackFrame findCaller() {
		try {
			return WALKER.walk(frames -> frames
					.filter(f -> !isLoggingClass(f.getDeclaringClass()))
					.findFirst()
					.orElse(null));
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static boolean isLoggingClass(Class<?> cls) {
		return JulLogger.class.isAssignableFrom(cls) || cls == BaseObject.class || cls == ILogger.class;
	}

	private boolean isLoggable(java.util.logging.Level level) {
		Logger tmp = getLogger();
		return tmp != null && tmp.isLoggable(level);
	}

	public void warn(String msg) {
		log(java.util.logging.Level.WARNING, msg, null);
	}

	public void warn(String msg, Throwable error) {
		log(java.util.logging.Level.WARNING, msg, error);
	}

	public void error(String msg) {
		log(java.util.logging.Level.SEVERE, msg, null);
	}

	public void error(String msg, Throwable error) {
		log(java.util.logging.Level.SEVERE, msg, error);
	}

	public void info(String msg) {
		log(java.util.logging.Level.INFO, msg, null);
	}

	public void info(String msg, Throwable error) {
		log(java.util.logging.Level.INFO, msg, error);
	}

	public boolean isDebugEnabled() {
		return isLoggable(DEBUG_LEVEL);
	}

	public boolean isErrorEnabled() {
		return isLoggable(java.util.logging.Level.SEVERE);
	}

	
	public boolean isInfoEnabled() {		
		return isLoggable(java.util.logging.Level.INFO);
	}

	public boolean isWarnEnabled() {		
		return isLoggable(java.util.logging.Level.WARNING);
	}
	
	/**
	 * Map ILogger levels to java.util.logging levels 
	 *	NONE  -&gt; OFF
	 *	ERROR -&gt; SEVERE
	 *	WARN  -&gt; WARNING
	 *	INFO  -&gt; INFO
	 *	DEBUG -&gt; FINEST (debug messages are logged at FINE, so FINEST shows them and any FINER and FINEST
	 *	         messages logged directly with java.util.logging)
	 */
	public void setLevel(Level level) {
		java.util.logging.Level ret = java.util.logging.Level.OFF;
		
		if( level != null ) {
			switch (level) {
			case NONE:ret = java.util.logging.Level.OFF;break;
			case ERROR:ret = java.util.logging.Level.SEVERE;break;
			case WARN:ret = java.util.logging.Level.WARNING;break;
			case INFO:ret = java.util.logging.Level.INFO;break;
			case DEBUG:ret = java.util.logging.Level.FINEST;break;
			}
		}
		
		getLogger().setLevel(ret);

	}

	/**
	 * Read the java.util.logging configuration (once per JVM). 
	 * The file named by the java.util.logging.config.file system property is used if it exists, 
	 * otherwise /JulLogging.properties from the class path (if available).
	 */
	private void configure() {
		if( !configured.compareAndSet(false, true)) {
			return;
		}
		
		// java.util.logging.LogMAnager should take care of this but it does not seem reliable
		String configFile = System.getProperty("java.util.logging.config.file");
		InputStream in = null;
		try {
			if( configFile != null ) {
				File file = new File(configFile);
				if( file.exists()) {
					in = new FileInputStream(file);
				}
			} 

			if(in == null) {
				in = getClass().getResourceAsStream(JUL_LOGGING_PROPERTIES);	
			}

			if( in != null ) {
				//  updateConfiguration (unlike readConfiguration) does not reset every logger and handler in the JVM,
				//  it only changes the loggers and handlers named in the file.
				java.util.logging.LogManager.getLogManager().updateConfiguration(in, null);
			}
		} catch (IOException | RuntimeException e) {
			System.err.println("Error reading java.util.logging configuration. e="+e);
		} finally {
			if( in != null ) {
				try {
					in.close();
				} catch (IOException e2) {
				}
			}
		}
	}

	public void init(String name) {	
		
		if(name == null || name.isEmpty()) {
			name = getClass().getName();
		}
		
		configure();
		
		logger = java.util.logging.Logger.getLogger(name);	
	}

	public Level getLevel() {
		Level ret = Level.NONE;
		
		//  The level is null when it's inherited from a parent logger
		java.util.logging.Level l  = null;
		for(Logger tmp = getLogger(); l == null && tmp != null; tmp = tmp.getParent()) {
			l = tmp.getLevel();
		}
		
		if( l == null ) {
			l = java.util.logging.Level.INFO;
		}
		
		int val = l.intValue();
		if( val == java.util.logging.Level.OFF.intValue()) {
			ret = Level.NONE;
		} else if( val >= java.util.logging.Level.SEVERE.intValue()) {
			ret = Level.ERROR;
		} else if( val >= java.util.logging.Level.WARNING.intValue()) {
			ret = Level.WARN;
		} else if( val > DEBUG_LEVEL.intValue()) {
			//  INFO and CONFIG (debug messages, at FINE, are not logged)
			ret = Level.INFO;
		} else {
			//  FINE, FINER, FINEST and ALL
			ret = Level.DEBUG;
		}
		
		return ret;
	}

}
