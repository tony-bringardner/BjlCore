// ~version~V000.01.02-V000.00.01-V000.00.00-
package us.bringardner.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Date;

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
	public static final Level DEFAULT_LEVEL = Level.NONE;
	
	public static final ThreadSafeDateFormat format = new ThreadSafeDateFormat("MM-dd-yyyy HH:mm:ss.SSS");
	private ILogger.Level level;
	private String name;
	private PrintStream _out;
	private PrintStream _err;
	
	
	public void debug(String msg) {
		
		if( isDebugEnabled() ) {
			logMessage(Level.DEBUG,msg,getOut());
		}
	}

	public PrintStream getOut() {
		return _out !=null
				?_out
						:System.out;
	}

	public void setOut(PrintStream out) {
		_out = out;
	}
	
	public PrintStream getErr() {
		return _err !=null?_err:System.out;
	}

	public void setErr(PrintStream err) {
		_err = err;
	}
	
	public void debug(String msg, Throwable error) {
		
		if( isDebugEnabled() ) {
			logMessage(Level.DEBUG,msg,getErr());
			if( error != null ) {
				error.printStackTrace(getErr());
			}
		}
	}

	public void error(String msg) {
		if( isErrorEnabled() ) {
			logMessage(Level.ERROR,msg,getErr());
		}

	}

	public void error(String msg, Throwable error) {
		if( isErrorEnabled() ) {
			logMessage(Level.ERROR,msg,getErr());
			if( error != null ) {
				error.printStackTrace(getErr());
			}
		}

	}

	public void info(String msg) {
		if( isInfoEnabled() ) {
			logMessage(Level.INFO,msg,getOut());
		}

	}

	public void info(String msg, Throwable error) {
		if( isInfoEnabled() ) {
			logMessage(Level.INFO,msg,getErr());
			if( error != null ) {
				error.printStackTrace(getErr());
			}
		}

	}

	public void init(String name) {
		this.name = name;
		String tmp = getProperty(PROPERTY_LOG_LEVEL);
		if( tmp != null ) {
			level = Level.valueOf(tmp);
		}
		
		if ( level == null ) {
			level = DEFAULT_LEVEL;
		}
		
		if( (tmp=getProperty(PROPERTY_LOG_FILE)) != null) {
			
			if( tmp.equals("System.out")) {
				_out = null;
			} else if( tmp.equals("System.err")) {
				_err = null;
			}  else {
				File file = new File(tmp);
				File dir = file.getParentFile();
				if( !dir.exists() ) {
					dir.mkdirs();
				}
				try {
					PrintStream ps = new PrintStream(file);
					_out = ps;
				} catch (FileNotFoundException e) {
					System.out.println("Error opening log file "+file+ " e="+e);
					e.printStackTrace(System.out);
					_out = null;
				}
			}
		}
	}
	
	protected void logMessage(Level level,String msg,PrintStream out) {
		out.println(format.format(new Date())+" [main]"+" "+level+" "+name+" - "+ msg);
	}

	public boolean isDebugEnabled() {		
		return level != ILogger.Level.NONE && level.ordinal() >= ILogger.Level.DEBUG.ordinal();
	}

	public boolean isErrorEnabled() {
		return  level != ILogger.Level.NONE && level.ordinal()>= ILogger.Level.ERROR.ordinal();
	}

	public boolean isInfoEnabled() {		
		return level != ILogger.Level.NONE &&  level.ordinal()>= ILogger.Level.INFO.ordinal();
	}
	
	public boolean isWarnEnabled() {	
		return level != ILogger.Level.NONE &&  level.ordinal()>= ILogger.Level.WARN.ordinal();
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
			logMessage(Level.WARN,msg,getErr());
			if( error != null ) {
				error.printStackTrace(getErr());
			}
		}		
	}

}
