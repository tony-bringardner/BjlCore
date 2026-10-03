// ~version~V000.01.02-V000.00.01-V000.00.00-

package us.bringardner.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * <PRE>
 * An implementation of ILogger to wrap the log4j framework.
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
 *  </PRE>   
 *   
 *	@author Tony Bringardner   
 *
 */
public class Log4JLogger implements ILogger {

	/**
	 * The class name log4j is given as the logging "wrapper" so that layouts showing the caller
	 * (%C %M %L %l) show the code that called this logger, not this class.
	 */
	private static final String FQCN = Log4JLogger.class.getName();

	/** Passed as the Throwable by the methods that don't take one (it is never logged). */
	static final Throwable NO_ERROR = new Throwable("no error", null, false, false) {
		private static final long serialVersionUID = 1L;
	};

	/**
	 * All the log4j2 methods are looked up once, when this class is first used, and shared by every Log4JLogger.
	 * log4j2 is accessed this way so this library has no compile or runtime dependency on it.
	 * The logging calls use MethodHandles in static final fields, which the JIT can inline. A disabled
	 * debug() call takes about half to two thirds of the time it did with Method.invoke (Java 11 and 21).
	 */
	private static final class Log4j {
		static final boolean available;
		static final Throwable initError;
		static final Method getLoggerByName;
		static final Method getLevel;
		//  Optional: only available with log4j-core
		static final Method configuratorSetLevel;
		static final Map<String,Object> levels = new HashMap<>();
		// setLevel methods found on logger implementation classes (log4j-core Logger has one, the API does not)
		static final Map<Class<?>,Method> setLevelMethods = new java.util.concurrent.ConcurrentHashMap<>();
		static final Method NO_METHOD;
		static final Class<?> levelClass;
		/** org.apache.logging.log4j.spi.ExtendedLogger, which every log4j2 logger implementation implements */
		static final Class<?> extendedLoggerClass;

		/** ExtendedLogger.logIfEnabled(String fqcn, Level, Marker, Object message, Throwable) as (Object,String,Object,Object,Object,Throwable)void */
		static final MethodHandle logIfEnabled;
		/** Logger.log(Level, Object message, Throwable) as (Object,Object,Object,Throwable)void, for a logger that is not an ExtendedLogger */
		static final MethodHandle log;
		/** Logger.isEnabled(Level) as (Object,Object)boolean */
		static final MethodHandle isEnabled;
		/** The log4j2 Level for each ILogger.Level, by ordinal */
		static final Object[] levelFor;

		static {
			boolean ok = false;
			Throwable err = null;
			Method gl=null, getL=null, confSet=null, none=null;
			MethodHandle lie=null, lg=null, ie=null;
			Class<?> lc = null, ext = null;
			Object[] lf = new Object[ILogger.Level.values().length];
			try {
				none = Object.class.getMethod("hashCode");
				ClassLoader cl = Log4JLogger.class.getClassLoader();
				lc = Class.forName("org.apache.logging.log4j.Level", true, cl);
				for(String name : new String[] {"OFF","FATAL","ERROR","WARN","INFO","DEBUG","TRACE","ALL"}) {
					levels.put(name, lc.getField(name).get(null));
				}
				lf[ILogger.Level.NONE.ordinal()] = levels.get("OFF");
				lf[ILogger.Level.ERROR.ordinal()] = levels.get("ERROR");
				lf[ILogger.Level.WARN.ordinal()] = levels.get("WARN");
				lf[ILogger.Level.INFO.ordinal()] = levels.get("INFO");
				lf[ILogger.Level.DEBUG.ordinal()] = levels.get("DEBUG");

				Class<?> logMgrClass = Class.forName("org.apache.logging.log4j.LogManager", true, cl);
				Class<?> api = Class.forName("org.apache.logging.log4j.Logger", true, cl);
				Class<?> marker = Class.forName("org.apache.logging.log4j.Marker", true, cl);
				ext = Class.forName("org.apache.logging.log4j.spi.ExtendedLogger", true, cl);
				gl = logMgrClass.getMethod("getLogger", String.class);
				getL = api.getMethod("getLevel");

				MethodHandles.Lookup lookup = MethodHandles.publicLookup();
				lie = lookup.findVirtual(ext, "logIfEnabled",
						MethodType.methodType(void.class, String.class, lc, marker, Object.class, Throwable.class))
						.asType(MethodType.methodType(void.class, Object.class, String.class, Object.class, Object.class, Object.class, Throwable.class));
				lg = lookup.findVirtual(api, "log",
						MethodType.methodType(void.class, lc, Object.class, Throwable.class))
						.asType(MethodType.methodType(void.class, Object.class, Object.class, Object.class, Throwable.class));
				ie = lookup.findVirtual(api, "isEnabled", MethodType.methodType(boolean.class, lc))
						.asType(MethodType.methodType(boolean.class, Object.class, Object.class));
				try {
					Class<?> conf = Class.forName("org.apache.logging.log4j.core.config.Configurator", true, cl);
					confSet = conf.getMethod("setLevel", String.class, lc);
				} catch (ReflectiveOperationException | LinkageError e) {
					// log4j-core is not available, setLevel will only work if the logger implements it.
				}
				ok = true;
			} catch (Throwable e) {
				err = e;
			}
			available = ok;
			initError = err;
			getLoggerByName=gl; getLevel=getL; configuratorSetLevel=confSet;
			NO_METHOD = none;
			levelClass = lc;
			extendedLoggerClass = ext;
			logIfEnabled = lie;
			log = lg;
			isEnabled = ie;
			levelFor = lf;
		}
	}

	/**
	 * @return true if the log4j2 API is in the class path and could be initialized.
	 */
	public static boolean isLog4jAvailable() {
		try {
			return Log4j.available;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * Without a log4j2 implementation (a provider, such as log4j-core) the log4j2 API only prints
	 * errors to the console, after a warning that it found no provider. So BaseObject only uses
	 * Log4JLogger by default when this is true, and BjlLogger (which follows the LogLevel and LogFile
	 * properties) otherwise.
	 *
	 * @return true if the log4j2 API and a log4j2 implementation are both in the class path.
	 */
	public static boolean isLog4jProviderAvailable() {
		//  Check for a provider first: isLog4jAvailable() initializes log4j's LogManager, which prints the warning.
		return ProviderCheck.AVAILABLE && isLog4jAvailable();
	}

	private static final class ProviderCheck {
		static final boolean AVAILABLE = hasProvider(Log4JLogger.class.getClassLoader());
	}

	/**
	 * Look for a log4j2 provider the way log4j2 does, without initializing log4j2.
	 *
	 * @param cl the class loader to search
	 * @return true if a provider is configured or declared in the class path of cl
	 */
	static boolean hasProvider(ClassLoader cl) {
		//  A provider (or context factory) named by a system property
		for (String name : new String[] {"log4j.provider", "log4j2.provider", "log4j2.loggerContextFactory", "log4j2.LoggerContextFactory"}) {
			if( System.getProperty(name) != null ) {
				return true;
			}
		}

		Class<?> providerClass;
		try {
			providerClass = Class.forName("org.apache.logging.log4j.spi.Provider", false, cl);
		} catch (ClassNotFoundException | LinkageError e) {
			//  No log4j2 API
			return false;
		}
		try {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Iterator<?> it = ServiceLoader.load((Class) providerClass, cl).iterator();
			if( it.hasNext() ) {
				return true;
			}
		} catch (ServiceConfigurationError | RuntimeException | LinkageError e) {
			//  A provider is declared but broken, let log4j2 report it (as it did before this check)
			return true;
		}
		//  Providers declared the way log4j2 versions before 2.10 did
		return cl != null && cl.getResource("META-INF/log4j-provider.properties") != null;
	}

	private volatile Object _logger;
	//  true if _logger is an ExtendedLogger (set before _logger)
	private volatile boolean extended;
	private volatile String name = Log4JLogger.class.getName();
	//  Used if log4j is not available (or can't create the logger) so logging is never lost.
	private volatile BjlLogger fallback;


	public Log4JLogger() {
		if( !isLog4jAvailable() ) {
			useFallback(name, Log4j.initError);
		}
	}
	
	private void useFallback(String name, Throwable error) {
		System.err.println("Can't create log4J logger. Using BjlLogger instead.  Error="+error);
		BjlLogger tmp = new BjlLogger();
		tmp.init(name);
		fallback = tmp;
	}

	private Object getLoggerObject() {
		Object ret = _logger;
		if( ret == null ) {
			// init was not called, use a logger named for this class
			init(name);
			ret = _logger;
		}
		return ret; 
	}

	/*
	 * Invoke a log4j method. Logging must never throw, so errors are reported to System.err.
	 */
	private Object invoke(Method m, Object ... args) {
		try {
			return m.invoke(getLoggerObject(), args);
		} catch (InvocationTargetException e) {
			System.err.println("Log4JLogger error calling "+m.getName()+" e="+e.getCause());
		} catch (Exception e) {
			System.err.println("Log4JLogger error calling "+m.getName()+" e="+e);
		}
		return null;
	}
	
	/*
	 * Logging must never throw, so errors from log4j are reported to System.err.
	 */
	private static void report(String what, Throwable e) {
		if( e instanceof VirtualMachineError ) {
			throw (VirtualMachineError) e;
		}
		System.err.println("Log4JLogger error calling "+what+" e="+e);
	}

	/**
	 * Log a message.
	 *
	 * @param fqcn the class whose frames log4j skips to find the caller (this class, or BaseObject when it is called by BaseObject)
	 * @param level
	 * @param msg
	 * @param error may be null, {@link #NO_ERROR} when the caller used a method without a Throwable
	 */
	void log(String fqcn, Level level, String msg, Throwable error) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			logTo(fb, level, msg, error);
			return;
		}
		if( error == NO_ERROR ) {
			error = null;
		}
		Object target = getLoggerObject();
		Object l4jLevel = Log4j.levelFor[level.ordinal()];
		try {
			if( extended ) {
				Log4j.logIfEnabled.invokeExact(target, fqcn, l4jLevel, (Object) null, (Object) msg, error);
			} else {
				Log4j.log.invokeExact(target, l4jLevel, (Object) msg, error);
			}
		} catch (Throwable e) {
			report("log", e);
		}
	}

	/**
	 * Log a message that is only built if the level is enabled.
	 *
	 * @param fqcn see {@link #log(String, Level, String, Throwable)}
	 * @param level
	 * @param msg
	 */
	void log(String fqcn, Level level, Supplier<String> msg) {
		if( isEnabled(level) ) {
			log(fqcn, level, msg.get(), NO_ERROR);
		}
	}

	/*
	 * Call the same method on the fallback logger as the caller called on this one 
	 * (BjlLogger writes debug(msg) and debug(msg,null) to different streams).
	 */
	private static void logTo(ILogger logger, Level level, String msg, Throwable error) {
		boolean one = error == NO_ERROR;
		switch (level) {
		case DEBUG: if( one ) logger.debug(msg); else logger.debug(msg, error); break;
		case INFO:  if( one ) logger.info(msg);  else logger.info(msg, error);  break;
		case WARN:  if( one ) logger.warn(msg);  else logger.warn(msg, error);  break;
		case ERROR: if( one ) logger.error(msg); else logger.error(msg, error); break;
		default: break;
		}
	}

	boolean isEnabled(Level level) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			switch (level) {
			case DEBUG: return fb.isDebugEnabled();
			case INFO: return fb.isInfoEnabled();
			case WARN: return fb.isWarnEnabled();
			case ERROR: return fb.isErrorEnabled();
			default: return false;
			}
		}
		Object target = getLoggerObject();
		try {
			return (boolean) Log4j.isEnabled.invokeExact(target, Log4j.levelFor[level.ordinal()]);
		} catch (Throwable e) {
			report("isEnabled", e);
			return false;
		}
	}

	public void debug(String msg) {
		log(FQCN, Level.DEBUG, msg, NO_ERROR);
	}

	public void debug(String msg, Throwable error) {
		log(FQCN, Level.DEBUG, msg, error);
	}

	public void info(String msg) {
		log(FQCN, Level.INFO, msg, NO_ERROR);
	}

	public void info(String msg, Throwable error) {
		log(FQCN, Level.INFO, msg, error);
	}

	public void error(String msg) {
		log(FQCN, Level.ERROR, msg, NO_ERROR);
	}

	public void error(String msg, Throwable error) {
		log(FQCN, Level.ERROR, msg, error);
	}

	public void warn(String msg) {
		log(FQCN, Level.WARN, msg, NO_ERROR);
	}

	public void warn(String msg, Throwable error) {
		log(FQCN, Level.WARN, msg, error);
	}

	//  These override the ILogger defaults so the caller log4j reports isn't the ILogger interface

	@Override
	public void debug(Supplier<String> msg) {
		log(FQCN, Level.DEBUG, msg);
	}

	@Override
	public void info(Supplier<String> msg) {
		log(FQCN, Level.INFO, msg);
	}

	@Override
	public void warn(Supplier<String> msg) {
		log(FQCN, Level.WARN, msg);
	}

	@Override
	public void error(Supplier<String> msg) {
		log(FQCN, Level.ERROR, msg);
	}

	/**
	 * Create the log4j Logger for this name.  
	 * The level is NOT changed, so the levels in the log4j configuration are respected.
	 */
	public void init(String name) {
		if( name != null && !name.isEmpty()) {
			this.name = name;
		}
		if( fallback != null ) {
			fallback.init(this.name);
			return;
		}
		try {
			Object tmp = Log4j.getLoggerByName.invoke(null, this.name);
			//  Every log4j2 logger implementation is an ExtendedLogger, which lets us tell log4j who the caller is
			extended = Log4j.extendedLoggerClass.isInstance(tmp);
			_logger = tmp;
		} catch (Exception e) {
			useFallback(this.name, e instanceof InvocationTargetException ? e.getCause() : e);
		}
	}

	public boolean isDebugEnabled() {
		return isEnabled(Level.DEBUG);
	}

	public boolean isWarnEnabled() {
		return isEnabled(Level.WARN);
	}

	public boolean isErrorEnabled() {
		return isEnabled(Level.ERROR);
	}

	public boolean isInfoEnabled() {
		return isEnabled(Level.INFO);
	}

	public void setLevel(Level level) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.setLevel(level);
			return;
		}
		
		String levelName = "OFF";
		if( level != null ) {
			switch (level) {
			case ERROR:levelName = "ERROR";break;
			case WARN:levelName = "WARN";break;
			case NONE:levelName = "OFF";break;
			case INFO:levelName = "INFO";break;
			case DEBUG:levelName = "DEBUG";break;
			}
		}
		Object alevel = Log4j.levels.get(levelName);
		Object target = getLoggerObject();
		
		// log4j-core Loggers implement setLevel, the log4j API does not.
		Method setLevel = Log4j.setLevelMethods.computeIfAbsent(target.getClass(), cls -> {
			try {
				Method m = cls.getMethod("setLevel", Log4j.levelClass);
				try {
					//  The logger implementation class may not be public
					m.setAccessible(true);
				} catch (RuntimeException e) {
					// InaccessibleObjectException (modules), invoke may still work if the class is public 
				}
				return m;
			} catch (NoSuchMethodException e) {
				return Log4j.NO_METHOD;
			}
		});
		
		try {
			if( setLevel != Log4j.NO_METHOD ) {
				try {
					setLevel.invoke(target, alevel);
					return;
				} catch (IllegalAccessException e) {
					// fall through and try the Configurator
				}
			} 
			if( Log4j.configuratorSetLevel != null ) {
				Log4j.configuratorSetLevel.invoke(null, name, alevel);
			} else {
				System.err.println("Log4JLogger can't set the level of "+name+" (log4j-core is not available).");
			}
		} catch (Exception e) {
			System.err.println("Log4JLogger error setting level of "+name+" e="+e);
		}

	}

	public Level getLevel() {	
		BjlLogger fb = fallback;
		if( fb != null ) {
			return fb.getLevel();
		}
		Level ret = Level.NONE;

		Object val =  invoke(Log4j.getLevel);
		if( val !=null ) {
			String name = val.toString();
			if( "OFF".equals(name)) {
				ret = Level.NONE;
			} else if( "FATAL".equals(name)) {
				ret = Level.ERROR;
			} else if( "ERROR".equals(name)) {
				ret = Level.ERROR;
			} else if( "WARN".equals(name)) {
				ret = Level.WARN;
			} else if( "INFO".equals(name)) {
				ret = Level.INFO;
			} else if( "DEBUG".equals(name) || "TRACE".equals(name) || "ALL".equals(name)) {
				ret = Level.DEBUG;
			} 
		}
		return ret;
	}

}
