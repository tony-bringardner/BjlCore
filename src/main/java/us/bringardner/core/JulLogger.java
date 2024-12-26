// ~version~V000.00.01-V000.00.00-
package us.bringardner.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * <PRE>
 *  An implementation of ILogger that uses JUL (java.util.Logger).
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
 *  </PRE> *   
 *   
 *	@author Tony Bringardner   
 *
 */
public class JulLogger implements ILogger {

	private static final String JUL_LOGGING_PROPERTIES = "/JulLogging.properties";
	private Logger logger;
	
	
	public Logger getLogger() {
		if( logger == null ) {
			init(null);
		}
		
		return logger;
	}
	
	public void debug(String msg) {		
			log(java.util.logging.Level.FINEST, msg, null);	
	}

	public void debug(String msg, Throwable error) {
		log(java.util.logging.Level.FINEST, msg, error);
	}

	private void log(java.util.logging.Level level, String msg, Throwable error) {
		
		 if( logger == null ) {
			 init(null);
		 }
		 if( logger == null ) {
			 if( isEnabled(level) ) {
				 System.err.println("No logger availible for");
				 System.err.println(msg);
				 if( error != null ) {
					 error.printStackTrace(System.err);
				 }
			 }
		 } else {
			 logger.log(level, msg,error);
		 }
	}

	private boolean isEnabled(java.util.logging.Level level) {
		boolean ret = false;
		
		if( level.intValue() == java.util.logging.Level.FINEST.intValue()) {
			ret = isDebugEnabled();
		} else if( level.intValue() == java.util.logging.Level.INFO.intValue()) {
			ret = isInfoEnabled();
		} else if( level.intValue() == java.util.logging.Level.SEVERE.intValue()) {
			ret = isErrorEnabled();
		} else if( level.intValue() == java.util.logging.Level.WARNING.intValue()) {
			ret = isWarnEnabled();
		} else if( level.intValue() == java.util.logging.Level.OFF.intValue()) {
			ret = false;
		} else if( level.intValue() == java.util.logging.Level.ALL.intValue()) {
			ret = true;
		}
		
		return ret;
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
		return logger.isLoggable(java.util.logging.Level.FINEST);
	}

	public boolean isErrorEnabled() {
		return logger.isLoggable(java.util.logging.Level.SEVERE);
	}

	
	public boolean isInfoEnabled() {		
		return logger.isLoggable(java.util.logging.Level.INFO);
	}

	public boolean isWarnEnabled() {		
		return logger.isLoggable(java.util.logging.Level.WARNING);
	}
	
	/**
	 * Implementation for JUL (java.util.logging).
	 * Level.INFO =  java.util.logging.Level.INFO
	 * Level.DEBUG = java.util.logging.Level.FINEST
	 * Level.ERROR = java.util.logging.Level.SEVERE;
	 * 
	 * @see us.bringardner.core.ILogger#setLevel(us.bringardner.core.ILogger.Level)
	 **/
	public void setLevel(Level level) {
		//  OFF, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, ALL
		/*
	NONE,
		ERROR,
		WARN,
		INFO,
		DEBUG
		 */
		java.util.logging.Level ret = java.util.logging.Level.OFF;
		
		switch (level) {
		case NONE:ret = java.util.logging.Level.OFF;break;
		case ERROR:ret = java.util.logging.Level.SEVERE;break;
		case WARN:ret = java.util.logging.Level.WARNING;break;
		case INFO:ret = java.util.logging.Level.INFO;break;
		case DEBUG:ret = java.util.logging.Level.FINEST;break;
		
		}
		
		logger.setLevel(ret);

	}

	public void init(String name) {	
		
		if(name == null || name.isEmpty()) {
			name = getClass().getName();
		}
		
		// java.util.logging.LogMAnager should take care of this but it does not seem reliable
		String configFile = System.getProperty("java.util.logging.config.file");
		if( configFile == null ) {
			configFile = JUL_LOGGING_PROPERTIES;
		}
		InputStream in = null;
		File file = new File(configFile);
		if( file.exists()) {
			try {
				in = new FileInputStream(file);
			} catch (IOException e) {
			}
		} 
		
		if(in == null) {
			in = getClass().getResourceAsStream(JUL_LOGGING_PROPERTIES);	
		}
		
		if( in != null ) {
				try {
					java.util.logging.LogManager.getLogManager().readConfiguration(in);
				} catch (Exception e) {
				} finally {
					try {
						in.close();
					} catch (Exception e2) {
					}
				}			
		}
		
		try {
			logger = java.util.logging.Logger.getLogger(name);	
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public Level getLevel() {
		Level ret = Level.NONE;
		java.util.logging.Level l  = logger.getLevel();

		if(l.equals(java.util.logging.Level.INFO)) {
			ret = Level.INFO;
		} else if(l.equals(java.util.logging.Level.ALL)) {
			ret = Level.DEBUG;
		} else if(l.equals(java.util.logging.Level.CONFIG)) {
			ret = Level.DEBUG;
		} else if(l.equals(java.util.logging.Level. FINE)) {
			ret = Level.DEBUG;
		} else if(l.equals(java.util.logging.Level.FINEST)) {
			ret = Level.DEBUG;
		} else if(l.equals(java.util.logging.Level.OFF)) {
			ret = Level.NONE;
		} else if(l.equals(java.util.logging.Level.SEVERE)) {
			ret = Level.ERROR;
		} else if(l.equals(java.util.logging.Level.WARNING)) {
			ret = Level.WARN;
		}
		
		return ret;
	}

}
