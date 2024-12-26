// ~version~V000.01.02-V000.00.01-V000.00.00-

package us.bringardner.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

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

	private static Object globalLogger;
	private static Class<?> loggerClass;

	private Method error1ArgMethod;
	private Method error2ArgMethod;

	private Method info1ArgMethod;
	private Method info2ArgMethod;

	private Method warn1ArgMethod;
	private Method warn2ArgMethod;

	private Method debug1ArgMethod;
	private Method debug2ArgMethod;

	private Method isDebug;
	private Method isError;
	private Method isInfo;
	private Method isWarn;
	private Method getLevel;
	private Method setLevel;
	
	private Object _logger;

	private static Map<String,Object> levels = new TreeMap<>();


	public Log4JLogger() {
		if( levels.isEmpty()) {
			try {
				Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
				Field[] flds = levelClass.getFields();
				for(Field f : flds) {
					Object val =  f.get(null);
					levels.put(f.getName(),val);
				}

				Class<?> logMgrClass = Class.forName("org.apache.logging.log4j.LogManager");
				Method m =logMgrClass.getDeclaredMethod("getLogger", Class.class);
				if( m !=null && Modifier.isStatic(m.getModifiers())) {
					globalLogger = m.invoke(null, Log4JLogger.class);
					loggerClass = globalLogger.getClass();
				}
			} catch (Throwable e) {			
				System.out.println("Log4JLogger error "+e);
			}
		}
	}
	
	private Object getLoggerObject() {
		return _logger == null ? globalLogger:_logger; 
	}

	public void debug(String msg) {
		try {
			if( debug1ArgMethod == null ) {
				debug1ArgMethod = loggerClass.getMethod("debug", Object.class);
				debug1ArgMethod.setAccessible(true);
			}
			debug1ArgMethod.invoke(getLoggerObject(), msg);
		} catch (Exception e) {
			System.err.println(msg);
			e.printStackTrace();
		}				
	}


	public void debug(String msg, Throwable error) {
		try {
			if( debug2ArgMethod == null ) {
				debug2ArgMethod = loggerClass.getMethod("debug", Object.class,Throwable.class);
				debug2ArgMethod.setAccessible(true);
			}
			debug2ArgMethod.invoke(getLoggerObject(), msg,error);
		} catch (Exception e) {
			System.err.println(msg);
			e.printStackTrace();
		}				

	}

	public void info(String msg) {
		try {
			if( info1ArgMethod == null ) {
				info1ArgMethod = loggerClass.getMethod("info", Object.class);
				info1ArgMethod.setAccessible(true);
			}
			info1ArgMethod.invoke(getLoggerObject(), msg);
		} catch (Exception e) {
			System.err.println(msg);
			e.printStackTrace();
		}					
	}

	public void info(String msg, Throwable error) {
		try {
			if( info2ArgMethod == null ) {
				info2ArgMethod = loggerClass.getMethod("info", Object.class,Throwable.class);
				info2ArgMethod.setAccessible(true);
			}
			info2ArgMethod.invoke(getLoggerObject(), msg,error);
		} catch (Exception e) {
			System.err.println(msg);
			e.printStackTrace();
		}				
	}

	public void error(String msg) {
		try {
			if( error1ArgMethod == null ) {
				error1ArgMethod = loggerClass.getMethod("error", Object.class);
				error1ArgMethod.setAccessible(true);
			}
			error1ArgMethod.invoke(getLoggerObject(), msg);
		} catch (Exception e) {
			System.err.println(msg);
			e.printStackTrace();
		}				
	}

	public void error(String msg, Throwable error) {
		try {
			if( error2ArgMethod == null ) {
				error2ArgMethod = loggerClass.getMethod("error", Object.class,Throwable.class);
				error2ArgMethod.setAccessible(true);
			}
			error2ArgMethod.invoke(getLoggerObject(), msg,error);
		} catch (Exception e) {
			System.err.println(msg);
			e.printStackTrace();
		}				

	}

	public void warn(String msg) {
		try {
			if( warn1ArgMethod == null ) {
				warn1ArgMethod = loggerClass.getMethod("warn", Object.class);
				warn1ArgMethod.setAccessible(true);
			}
			warn1ArgMethod.invoke(getLoggerObject(), msg);
		} catch (Exception e) {
			System.err.println(msg);
			e.printStackTrace();
		}				
	}

	public void warn(String msg, Throwable error) {
		try {
			if( warn2ArgMethod == null ) {
				warn2ArgMethod = loggerClass.getMethod("warn", Object.class,Throwable.class);
				warn2ArgMethod.setAccessible(true);
			}
			warn2ArgMethod.invoke(getLoggerObject(), msg,error);
		} catch (Exception e) {
			System.err.println(msg);
			e.printStackTrace();
		}				
	}

	public void init(String name) {
		if( _logger == null ) {
			try {
				Class<?> logMgrClass = Class.forName("org.apache.logging.log4j.LogManager");
				Method m =logMgrClass.getDeclaredMethod("getLogger", String.class);
				if( m !=null && Modifier.isStatic(m.getModifiers())) {
					Object tmp = m.invoke(null, name);
					Level level = getLevel();
					//  just in case the global level got changed...
					_logger = tmp;
					setLevel(level);
				}
			} catch (Exception e) {
				System.out.println("Can't create log4J logger. Using BjlLogger instead.  Error="+e);
				_logger = new BjlLogger();				
			}
		}	
	}

	public boolean isDebugEnabled() {
		boolean ret = false;
		try {
			if( isDebug == null ) {
				isDebug = loggerClass.getMethod("isDebugEnabled");
				isDebug.setAccessible(true);
			}
			ret = (boolean) isDebug.invoke(getLoggerObject());
		} catch (Exception e) {			
			e.printStackTrace();
		}				
		return ret;
	}

	public boolean isWarnEnabled() {
		boolean ret = false;
		try {
			if( isWarn == null ) {
				isWarn = loggerClass.getMethod("isWarnEnabled");
				isWarn.setAccessible(true);
			}
			ret = (boolean) isWarn.invoke(getLoggerObject());
		} catch (Exception e) {			
			e.printStackTrace();
		}				
		return ret;
	}

	public boolean isErrorEnabled() {
		boolean ret = false;
		try {
			if( isError == null ) {
				isError = loggerClass.getMethod("isErrorEnabled");
				isError.setAccessible(true);
			}
			ret = (boolean) isError.invoke(getLoggerObject());
		} catch (Exception e) {			
			e.printStackTrace();
		}				
		return ret;
	}

	public boolean isInfoEnabled() {		
		boolean ret = false;
		try {
			if( isInfo == null ) {
				isInfo = loggerClass.getMethod("isInfoEnabled");
				isInfo.setAccessible(true);
			}
			ret = (boolean) isInfo.invoke(getLoggerObject());
		} catch (Exception e) {			
			e.printStackTrace();
		}				
		return ret;
	}

	public void setLevel(Level level) {
		try {
			Object alevel = levels.get("OFF");
			switch (level) {
			case ERROR:alevel = levels.get("ERROR");break;
			case WARN:alevel = levels.get("WARN");break;
			case NONE:alevel = levels.get("OFF");break;
			case INFO:alevel = levels.get("INFO");break;
			case DEBUG:alevel = levels.get("DEBUG");break;
			default:
				throw new IllegalArgumentException(level+" is not a valid level");			
			}
			if( setLevel == null ) {
				setLevel = loggerClass.getMethod("setLevel",alevel.getClass());
				setLevel.setAccessible(true);				
			}
			setLevel.invoke(getLoggerObject(), alevel);
		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	public Level getLevel() {	
		Level ret = Level.NONE;

		try {
			if( getLevel == null ) {
				getLevel = loggerClass.getMethod("getLevel");
				getLevel.setAccessible(true);
			}
			Object val =  getLevel.invoke(getLoggerObject());
			if( val !=null ) {
				String name = val.toString();
				if( "OFF".equals(name)) {
					ret = Level.NONE;
				} else if( "ALL".equals(name)) {
					ret = Level.DEBUG;
				} else if( "DEBUG".equals(name)) {
					ret = Level.DEBUG;
				} else if( "ERROR".equals(name)) {
					ret = Level.ERROR;
				} else if( "FATAL".equals(name)) {
					ret = Level.DEBUG;
				} else if( "INFO".equals(name)) {
					ret = Level.INFO;
				} else if( "WARN".equals(name)) {
					ret = Level.WARN;
				} else if( "TRACE".equals(name)) {
					ret = Level.DEBUG;
				}
			}
		} catch (Exception e) {			
			e.printStackTrace();
		}				
		return ret;
	}



}
