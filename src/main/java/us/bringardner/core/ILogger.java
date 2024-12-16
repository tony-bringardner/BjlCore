// ~version~V000.00.01-V000.00.00-
/**
 * Copyright 1998-2009 Tony Bringardner
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *   
 *   
 *	@author Tony Bringardner   
 */

package us.bringardner.core;


/**
 * 
 * <PRE>
 *  This is the interface to all Loggers used by the core package.
 *  The objective is to define an implementation neutral API that will
 *  provide the core functionality and may be implemented by any
 *  logging framework.  Specifically, log4j & java.util.logging.
 *    
 *  The desire is that higher level components that use this library
 *  need not know or care what logging framework is used at runtime.
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
 */
public interface ILogger {
	/*
	int l0Off = org.apache.logging.log4j.Level.OFF.intLevel();
		int l100Fatal = org.apache.logging.log4j.Level.FATAL.intLevel();
		int l200Error = org.apache.logging.log4j.Level.ERROR.intLevel();
		int l300Warn = org.apache.logging.log4j.Level.WARN.intLevel();
		int l400Info = org.apache.logging.log4j.Level.INFO.intLevel();
		int l500Debug = org.apache.logging.log4j.Level.DEBUG.intLevel();
		int l600Trace = org.apache.logging.log4j.Level.TRACE.intLevel();
		
		int l2147483647All = org.apache.logging.log4j.Level.ALL.intLevel(); 
	 */
	enum Level  {
		NONE,
		ERROR,
		WARN,
		INFO,
		DEBUG
		};
	
	/**
	 * Initialize the ILogger 
	 * @param name
	 */
	public void init(String name);
	
	/**
	 * Set the Level associated with the ILogger.
	 * @param level
	 */
	public void setLevel(Level level);
	
	/**
	 * @return the Level currently associated with the ILogger.
	 */
	public Level getLevel();
	
	/**
	 * generate a log entry if isInof() returns true.
	 * @param msg
	 */
	public void info(String msg);
	
	/**
	 * generate a log entry if isInof() returns true.
	 * 
	 * @param msg
	 * @param error
	 */
	public void info(String msg, Throwable error);
	
	/**
	 * @return true if getLevel() returns a value &gt;= Level.Info
	 */

	public boolean isInfoEnabled();
	
	/**
	 * generate a log entry if isError() returns true.
	 * 
	 * @param msg
	 */
	public void error(String msg);
	
	/**
	 * generate a log entry if isError() returns true.
	 * 
	 * @param msg
	 * @param error
	 */
	public void error(String msg, Throwable error);
	
	/**
	 * @return true if getLevel() returns a value &gt;= Level.Error
	 */
	public boolean isErrorEnabled();
	
	/**
	 * generate a log entry if isDebug() returns true.
	 * @param msg
	 */
	public void debug(String msg);
	
	/**
	 * generate a log entry if isDebug() returns true.
	 * 
	 * @param msg
	 * @param error
	 */
	public void debug(String msg, Throwable error);
	
	/**
	 * @return true if getLevel() returns a value &gt;= Level.Debug
	 */

	public boolean isDebugEnabled();

	
	
	/**
	 * generate a log entry if isWarn() returns true.
	 * @param msg
	 */
	public void warn(String msg);
	
	/**
	 * generate a log entry if isWarn() returns true.
	 * 
	 * @param msg
	 * @param error
	 */
	public void warn(String msg, Throwable error);
	
	/**
	 * @return true if getLevel() returns a value &gt;= Level.Warn
	 */

	public boolean isWarnEnabled();

}
