// ~version~V000.01.02-V000.00.01-V000.00.00-

package us.bringardner.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * <PRE>
 * An implementation of ILogger to wrap the log4j framework.
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.us/tony">Tony Bringardner</A>
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
	 * All reflection is done once, when this class is first used, and shared by every Log4JLogger.
	 * log4j2 is accessed by reflection so this library has no compile or runtime dependency on it.
	 */
	private static final class Log4j {
		static final boolean available;
		static final Throwable initError;
		static final Method getLoggerByName;
		static final Method debug1, debug2, info1, info2, warn1, warn2, error1, error2;
		static final Method isDebug, isInfo, isWarn, isError, getLevel;
		//  Optional: only available with log4j-core
		static final Method configuratorSetLevel;
		static final Map<String,Object> levels = new HashMap<>();
		// setLevel methods found on logger implementation classes (log4j-core Logger has one, the API does not)
		static final Map<Class<?>,Method> setLevelMethods = new java.util.concurrent.ConcurrentHashMap<>();
		static final Method NO_METHOD;
		static final Class<?> levelClass;

		static {
			boolean ok = false;
			Throwable err = null;
			Method gl=null, d1=null, d2=null, i1=null, i2=null, w1=null, w2=null, e1=null, e2=null;
			Method isD=null, isI=null, isW=null, isE=null, getL=null, confSet=null, none=null;
			Class<?> lc = null;
			try {
				none = Object.class.getMethod("hashCode");
				ClassLoader cl = Log4JLogger.class.getClassLoader();
				lc = Class.forName("org.apache.logging.log4j.Level", true, cl);
				for(String name : new String[] {"OFF","FATAL","ERROR","WARN","INFO","DEBUG","TRACE","ALL"}) {
					levels.put(name, lc.getField(name).get(null));
				}
				Class<?> logMgrClass = Class.forName("org.apache.logging.log4j.LogManager", true, cl);
				Class<?> api = Class.forName("org.apache.logging.log4j.Logger", true, cl);
				gl = logMgrClass.getMethod("getLogger", String.class);
				d1 = api.getMethod("debug", Object.class);
				d2 = api.getMethod("debug", Object.class, Throwable.class);
				i1 = api.getMethod("info", Object.class);
				i2 = api.getMethod("info", Object.class, Throwable.class);
				w1 = api.getMethod("warn", Object.class);
				w2 = api.getMethod("warn", Object.class, Throwable.class);
				e1 = api.getMethod("error", Object.class);
				e2 = api.getMethod("error", Object.class, Throwable.class);
				isD = api.getMethod("isDebugEnabled");
				isI = api.getMethod("isInfoEnabled");
				isW = api.getMethod("isWarnEnabled");
				isE = api.getMethod("isErrorEnabled");
				getL = api.getMethod("getLevel");
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
			getLoggerByName=gl; debug1=d1; debug2=d2; info1=i1; info2=i2; warn1=w1; warn2=w2; error1=e1; error2=e2;
			isDebug=isD; isInfo=isI; isWarn=isW; isError=isE; getLevel=getL; configuratorSetLevel=confSet;
			NO_METHOD = none;
			levelClass = lc;
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

	private volatile Object _logger;
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
	
	private boolean invokeBoolean(Method m) {
		Object ret = invoke(m);
		return ret instanceof Boolean && ((Boolean) ret).booleanValue();
	}

	public void debug(String msg) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.debug(msg);
		} else {
			invoke(Log4j.debug1, msg);
		}
	}


	public void debug(String msg, Throwable error) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.debug(msg,error);
		} else {
			invoke(Log4j.debug2, msg, error);
		}
	}

	public void info(String msg) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.info(msg);
		} else {
			invoke(Log4j.info1, msg);
		}
	}

	public void info(String msg, Throwable error) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.info(msg,error);
		} else {
			invoke(Log4j.info2, msg, error);
		}
	}

	public void error(String msg) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.error(msg);
		} else {
			invoke(Log4j.error1, msg);
		}
	}

	public void error(String msg, Throwable error) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.error(msg,error);
		} else {
			invoke(Log4j.error2, msg, error);
		}
	}

	public void warn(String msg) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.warn(msg);
		} else {
			invoke(Log4j.warn1, msg);
		}
	}

	public void warn(String msg, Throwable error) {
		BjlLogger fb = fallback;
		if( fb != null ) {
			fb.warn(msg,error);
		} else {
			invoke(Log4j.warn2, msg, error);
		}
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
			_logger = Log4j.getLoggerByName.invoke(null, this.name);
		} catch (Exception e) {
			useFallback(this.name, e instanceof InvocationTargetException ? e.getCause() : e);
		}
	}

	public boolean isDebugEnabled() {
		BjlLogger fb = fallback;
		return fb != null ? fb.isDebugEnabled() : invokeBoolean(Log4j.isDebug);
	}

	public boolean isWarnEnabled() {
		BjlLogger fb = fallback;
		return fb != null ? fb.isWarnEnabled() : invokeBoolean(Log4j.isWarn);
	}

	public boolean isErrorEnabled() {
		BjlLogger fb = fallback;
		return fb != null ? fb.isErrorEnabled() : invokeBoolean(Log4j.isError);
	}

	public boolean isInfoEnabled() {		
		BjlLogger fb = fallback;
		return fb != null ? fb.isInfoEnabled() : invokeBoolean(Log4j.isInfo);
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
