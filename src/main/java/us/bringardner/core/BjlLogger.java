// ~version~V000.01.02-V000.00.01-V000.00.00-
package us.bringardner.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import us.bringardner.core.util.ThreadSafeDateFormat;

/**
 * <PRE>
 * This is a very basic logger that can be used when nothing else is available.
 * It is NOT intended for production use.
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
 *   
 *	@author Tony Bringardner   
 *
 *
 */
public class BjlLogger extends BaseObject implements ILogger {

	public static final String PROPERTY_LOG_LEVEL = "LogLevel";
	public static final String PROPERTY_LOG_FILE = "LogFile";
	/**
	 * The default level is ERROR so that errors are never silently discarded.
	 * Set the LogLevel property (NONE, ERROR, WARN, INFO, DEBUG) to change it.
	 */
	public static final Level DEFAULT_LEVEL = Level.ERROR;
	
	/**
	 * @deprecated no longer used for formatting log entries (it serialized all logging threads). 
	 * Kept for compatibility.
	 */
	@Deprecated
	public static final ThreadSafeDateFormat format = new ThreadSafeDateFormat("MM-dd-yyyy HH:mm:ss.SSS");

	//  DateTimeFormatter is immutable and thread safe, so no locking is required.
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss.SSS");

	//  One PrintStream per log file, shared by all BjlLoggers, so the file is opened once (in append mode).
	private static final ConcurrentHashMap<String, PrintStream> logFiles = new ConcurrentHashMap<>();

	private volatile ILogger.Level level = DEFAULT_LEVEL;
	private String name;
	private volatile PrintStream _out;
	private volatile PrintStream _err;
	
	
	public void debug(String msg) {
		
		if( isDebugEnabled() ) {
			logMessage(Level.DEBUG,msg,getOut());
		}
	}

	public PrintStream getOut() {
		PrintStream ret = _out;
		return ret !=null ? ret : System.out;
	}

	public void setOut(PrintStream out) {
		_out = out;
	}
	
	/**
	 * @return the stream used for entries that include a stack trace. 
	 * Unless set, this is the same as getOut() so all entries stay in order in one place.
	 */
	public PrintStream getErr() {
		PrintStream ret = _err;
		return ret !=null ? ret : getOut();
	}

	public void setErr(PrintStream err) {
		_err = err;
	}
	
	public void debug(String msg, Throwable error) {
		
		if( isDebugEnabled() ) {
			logMessage(Level.DEBUG,msg,error,getErr());
		}
	}

	public void error(String msg) {
		if( isErrorEnabled() ) {
			logMessage(Level.ERROR,msg,getErr());
		}

	}

	public void error(String msg, Throwable error) {
		if( isErrorEnabled() ) {
			logMessage(Level.ERROR,msg,error,getErr());
		}

	}

	public void info(String msg) {
		if( isInfoEnabled() ) {
			logMessage(Level.INFO,msg,getOut());
		}

	}

	public void info(String msg, Throwable error) {
		if( isInfoEnabled() ) {
			logMessage(Level.INFO,msg,error,getErr());
		}

	}

	/**
	 * Initialize this logger.
	 * 
	 * The level is taken from the first of these properties that is defined:
	 * 	name.LogLevel  (specific to this logger name)
	 * 	LogLevel       
	 * 
	 * The value is not case sensitive. An invalid value is reported and the default (ERROR) is used.
	 *  
	 * LogFile may be System.out, System.err or a file name.  Log files are opened once, in append mode,
	 * and shared by all loggers that use the same file.
	 */
	public void init(String name) {
		this.name = name;
		String tmp = null;
		if( name != null && !name.isEmpty()) {
			tmp = getProperty(name+"."+PROPERTY_LOG_LEVEL);
		}
		if( tmp == null ) {
			tmp = getProperty(PROPERTY_LOG_LEVEL);
		}
		level = parseLevel(tmp, DEFAULT_LEVEL);
		
		if( (tmp=getProperty(PROPERTY_LOG_FILE)) != null) {
			tmp = tmp.trim();
			if( tmp.equals("System.out")) {
				_out = null;
				_err = null;
			} else if( tmp.equals("System.err")) {
				_out = System.err;
				_err = System.err;
			}  else {
				PrintStream ps = openLogFile(tmp);
				if( ps != null ) {
					_out = ps;
					_err = ps;
				}
			}
		}
	}
	
	/**
	 * Convert a String to a Level (not case sensitive). 
	 * @param value
	 * @param defaultLevel returned if value is null or invalid
	 * @return the Level
	 */
	public static Level parseLevel(String value, Level defaultLevel) {
		if( value == null || value.trim().isEmpty()) {
			return defaultLevel;
		}
		String tmp = value.trim().toUpperCase(Locale.ROOT);
		switch (tmp) {
			// common aliases used by other logging frameworks
			case "OFF": return Level.NONE;
			case "SEVERE": 
			case "FATAL": return Level.ERROR;
			case "WARNING": return Level.WARN;
			case "TRACE": 
			case "ALL": 
			case "FINE": 
			case "FINER": 
			case "FINEST": return Level.DEBUG;
			default:
				try {
					return Level.valueOf(tmp);
				} catch (IllegalArgumentException e) {
					System.err.println("Invalid "+PROPERTY_LOG_LEVEL+" ("+value+"). Using "+defaultLevel);
					return defaultLevel;
				}
		}
	}

	private static PrintStream openLogFile(String fileName) {
		File file = new File(fileName).getAbsoluteFile();
		String key = file.getPath();
		try {
			key = file.getCanonicalPath();
		} catch (IOException e) {
			// use the absolute path
		}
		PrintStream ret = logFiles.get(key);
		if( ret == null ) {
			synchronized (logFiles) {
				ret = logFiles.get(key);
				if( ret == null ) {
					try {
						File dir = file.getParentFile();
						if( dir != null && !dir.exists() ) {
							dir.mkdirs();
						}
						ret = new PrintStream(new FileOutputStream(file, true), true);
						logFiles.put(key, ret);
					} catch (IOException e) {
						System.err.println("Error opening log file "+file+ " e="+e+". Logging to System.out");
						ret = null;
					}
				}
			}
		}
		return ret;
	}

	protected void logMessage(Level level,String msg,PrintStream out) {
		out.println(formatMessage(level, msg));
	}
	
	private void logMessage(Level level,String msg,Throwable error, PrintStream out) {
		String line = formatMessage(level, msg);
		if( error == null ) {
			out.println(line);
		} else {
			// Keep the message and stack trace together when several threads are logging.
			synchronized (out) {
				out.println(line);
				error.printStackTrace(out);
			}
		}
	}

	private String formatMessage(Level level,String msg) {
		return TIME_FORMAT.format(LocalDateTime.now())+" ["+Thread.currentThread().getName()+"] "+level+" "+name+" - "+ msg;
	}

	public boolean isDebugEnabled() {		
		return isEnabled(ILogger.Level.DEBUG);
	}

	public boolean isErrorEnabled() {
		return isEnabled(ILogger.Level.ERROR);
	}

	public boolean isInfoEnabled() {		
		return isEnabled(ILogger.Level.INFO);
	}
	
	public boolean isWarnEnabled() {	
		return isEnabled(ILogger.Level.WARN);
	}

	private boolean isEnabled(ILogger.Level target) {
		Level current = level;
		return current != ILogger.Level.NONE && current.ordinal() >= target.ordinal();
	}

	public void setLevel(Level level) {
		if( level == null ) {
			this.level = Level.NONE;
		} else {
			this.level = level;
		}
	}

	public Level getLevel() {
		return level;
	}

	

	public void warn(String msg) {
		if( isWarnEnabled() ) {
			logMessage(Level.WARN,msg,getOut());
		}		
	}

	public void warn(String msg, Throwable error) {

		if( isWarnEnabled() ) {
			logMessage(Level.WARN,msg,error,getErr());
		}		
	}

}
