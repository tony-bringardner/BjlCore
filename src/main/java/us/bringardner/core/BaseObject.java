// ~version~V000.01.11-V000.01.02-V000.00.01-V000.00.00-


package us.bringardner.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import us.bringardner.core.util.LruMap;


/**
 * 
 * <PRE>
 * The objective of the BaseObject class is to provide a common foundation for all, non trivial classes.
 * It provides for the simplest and most fundamental functions required by most non trivial classes 
 * such as logging and property management.
 *  
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
public class BaseObject { 




	//  Used to initialize the LruMap for properties.  Use setMaxProperties to change it at run time. 
	public static final int DEFAULT_MAX_PROPERTIES = 200;
	public static final String PROPERTY_LOGGER = "ILogger";
	private static volatile Class<?>   loggerClass = null;

	//  The properties map is global so we use a LruMap to manage the memory footprint.
	//  LruMap is an access-ordered LinkedHashMap (even get() modifies it) so EVERY access
	//  must be guarded by the same lock.  We use the map itself as the lock.
	private static final LruMap<String, Properties> properties = new LruMap<String, Properties>(DEFAULT_MAX_PROPERTIES);

	//  Loggers are shared by name (like log4j and java.util.logging) so we don't create
	//  (and initialize) a new ILogger for every object instance.
	private static final ConcurrentHashMap<String, ILogger> loggers = new ConcurrentHashMap<>();

	private volatile boolean supportPrefixProperty = true;

	/**
	 * @param maxSize for the LruMap used for properties.
	 */
	public static void setMaxProperties(int maxSize) {
		synchronized (properties) {
			properties.setMaxSize(maxSize);
		}
	}

	/**
	 * Remove all cached ILoggers. The next call to getLogger will create new ones.
	 * Objects that have already obtained a logger will continue to use it.
	 */
	public static void clearLoggerCache() {
		loggers.clear();
	}

	/**
	 * Clear the properties cache.
	 * 
	 * Properties are cache to improve performance when accessing them.
	 * If the application uses properties only during initialization,
	 * the cache may be cleared to reduce the memory footprint.  
	 */
	public static void clearPropertyCache() {
		synchronized (properties) {
			properties.clear();
		}
	}

	/**
	 * propertyPrefix provides a "search path" for properties.
	 */
	private volatile String propertyPrefix;

	private volatile ILogger logger ;


	/**
	 * BaseObject constructor  
	 */
	public BaseObject()  {
		super();
	}



	/**
	 * The propertyPrefix is used when searching for properties.
	 * The default is the Class name of this Object.
	 *  
	 * @return the propertyPrefix for this Object
	 * @see #getProperty(String)
	 * @see #getProperty(String, String)
	 * @See {@link #isSupportPrefixProperty()}
	 */
	protected String getPropertyPrefix() {
		if( !isSupportPrefixProperty()) {
			return null;
		}
		
		if( propertyPrefix == null ) {
			synchronized (this) {
				if( propertyPrefix == null ) {
					 propertyPrefix = getClass().getName();
				}
			}
		}
		
		return propertyPrefix;
	}

	/**
	 * Set the propertyPrefix used when searching for properties.
	 * The default is the Class name of this Object.
	 *   
	 * @param propertyPrefix
	 * @see BaseObject#getProperty(String)
	 * @see #getProperty(String, String)
	 * @See {@link #isSupportPrefixProperty()}
	 */
	protected void setPropertyPrefix(String propertyPrefix) {
		this.propertyPrefix = propertyPrefix;
	}



	/**
	 * Find the Properties of the given name.
	 * 
	 * The objective is to allow each level, or 'name' to externals
	 * values into property files that can be easily overridden at run time.
	 * 
	 * The BaseObject will maintain a map of Entries for each name as an indicator
	 * the properties have been loaded, thus eliminating the need to search for the properties 
	 * every time the getProperteis method is called.
	 * 
	 *  
	 * @param name
	 * @return the Properties associated with the given name.
	 */
	private Properties getPropertyEntry(String name) {
		Properties ret;
		synchronized (properties) {
			ret = properties.get(name);
		}
		if( ret == null ) {
			//  Load outside the lock so a slow class path search does not block every other thread.
			//  If two threads load the same file at the same time, the last one wins (the content is the same).
			ret = new Properties();
			try {
				// First, see it we can find a file name "name.properties"
				String path = null;
				if( name.length() > 0 ) {
					path = "/"+name.replace('.', '/');
				} else {
					path = name;
				}

				String fn = path+".properties";
				/*
				 * If you run a bug detector this will show up as a bug...
				 * Calling this.getClass().getResource(...) could give results other than expected if this class is extended by a class in another package
				 * In this case we want that behavior.  It allows a property file to be replaced or overwritten by the extending class. 
				 */
				try(InputStream in = getClass().getResourceAsStream(fn)){
					if( in != null ) {
						ret  = new Properties();
						ret.load(in);
					}
				}
				
			} catch(IOException e) {
				// We cannot call the normal logging functions here because it could potentially cause a deadlock.
				System.err.println("Error reading properties for "+name+" e=("+e+")");
				e.printStackTrace(System.err);

			} finally {
				synchronized (properties) {
					properties.put(name, ret);
				}
			}
		}

		return ret;
	}

	/**
	 * <PRE>
	 * Properties can be defined at multiple levels and can be controlled 
	 * both by the property name and the location or property file. 
	 * 
	 * First, the property name is prepended with the fully qualified class name.
	 * 
	 *  The a search is initiated by iterating through the 'dot' notation of the name.
	 *  Example:  In the class us.bringardner.TestClass, a call to getProperties("propertyName")
	 *  			would start the search looking for "us.bringardner.Test.propertyName".
	 *  			The search will then iterate through the 'dot' names until a property is is found
	 *  			or the end is reached.
	 *  
	 *    			Every iteration will search first in the System.properteis.  If the property
	 *    			is not found, we look for a property file with the appropriate name.  If such a file exists,
	 *    			the file is checked for a property with that name. 
	 *  	
	 *  In our example any of these properties could be used to set the value.
	 *  	us.bringardner.TestClass.propertyName
	 *  	us.bringardner.propertyName
	 *  	com.propertyName
	 *  	propertyName
	 *  
	 *  Using this method you can configure some properties at a very broad level where many classes would share 
	 *  the same configuration or set a property the is very specific for one class.  
	 *    
	 * At each level of the naming structure (dot notation) a properties file may exist 
	 * in the class path.  If so, it will be included in the search.  
	 *   
	 * In our example, the following locations could be defined (and searched in this order);
	 * 		System.properties
	 *  	us.bringardner.TestClass.properties
	 *  	us.bringardner.properties
	 *  	com.properties
	 *  </PRE>
	 *  
	 * @param propertyName Name of the property
	 * @return the value associated with the given name in the properties for this Object, or null.
	 */
	public String getProperty(String propertyName) {
		return getProperty(propertyName,null);
	}


	/**
	 * 
	 * @return
	 */
	public boolean isSupportPrefixProperty() {
		return supportPrefixProperty;
	}

	/**
	 * Enable property logic
	 * @param supportPrefixProperty
	 */
	public void setSupportPrefixProperty(boolean supportPrefixProperty) {
		this.supportPrefixProperty = supportPrefixProperty;
	}


	public String getProperty(String propertyName,String defaultValue) {
		String ret = null;
		
		String prefix = getPropertyPrefix();

		if( prefix!=null ) {
			ret = System.getProperty(prefix+"."+propertyName);
		}
		
		if( ret == null ) {
			ret = System.getProperty(propertyName);
		}

		if( ret == null ) {

			Class<?> cls = getClass();
			while(ret == null && cls != BaseObject.class) {
				String path = cls.getName();
				int idx = path.indexOf('$');
				if( idx > 0) {
					path = path.substring(0,idx);
				}
				Properties p = getPropertyEntry(path);
				if( p != null ) {
					if( prefix!=null ) {
						ret = p.getProperty(prefix+"."+propertyName);
					}
					if(ret==null) {
						ret = p.getProperty(propertyName);
					}
				}
				cls = cls.getSuperclass();
			}
		}

		if( ret == null ) {
			ret = defaultValue;
		}
		return ret;
	}



	/**
	 * Get an integer property. If the property is not defined, or is not a valid integer,
	 * the default value is returned (an invalid value is reported to System.err).
	 *  
	 * @param propertyName Name of the property
	 * @param defaultValue value to use if the property is not defined or is invalid
	 * @return the integer value of the property
	 * @see #getProperty(String, String)
	 */
	public int getIntProperty(String propertyName, int defaultValue) {
		String tmp = getProperty(propertyName);
		if( tmp == null ) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(tmp.trim());
		} catch (NumberFormatException e) {
			// Don't use the logger here, getProperty is used while loggers are being created.
			System.err.println("Invalid integer value for property "+propertyName+" ("+tmp+") in "+getClass().getName()+". Using default "+defaultValue);
			return defaultValue;
		}
	}

	/**
	 * Get a boolean property ("true" in any case is true, anything else is false).
	 * 
	 * @param propertyName Name of the property
	 * @param defaultValue value to use if the property is not defined
	 * @return the boolean value of the property
	 */
	public boolean getBooleanProperty(String propertyName, boolean defaultValue) {
		String tmp = getProperty(propertyName);
		if( tmp == null ) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(tmp.trim());
	}

	/**
	 * Determine the class to use to implement the ILogger api.
	 * The objective the this class is to implement logging without creating 
	 * runtime dependencies to third party libraries. 
	 * 
	 * 1)  The System.property for "ILogger" is defined, that class is used.
	 * 2)  If the log4j2 API (org.apache.logging.log4j) is in the class path, Log4JLogger is used.
	 * 3)  The default is BjlLogger.
	 * 
	 * @return the Class used to create ILoggers
	 */
	protected static Class<?> getLoggerClass() {
		if( loggerClass == null ) {
			synchronized (BaseObject.class) {
				if( loggerClass == null ) {
					String  tmp = System.getProperty(PROPERTY_LOGGER);
					if( tmp != null ) {
						try {
							Class<?> cls = Class.forName(tmp);
							if( ILogger.class.isAssignableFrom(cls)) {
								loggerClass = cls;
							} else {
								System.err.println("Defined Logger does not implement "+ILogger.class.getName()+". "+PROPERTY_LOGGER+"="+tmp);
							}
						} catch (ClassNotFoundException e) {
							System.err.println("Defined Logger is not availible. "+PROPERTY_LOGGER+"="+tmp);
						}
					}
					if( loggerClass == null ) 	{						
						loggerClass = Log4JLogger.isLog4jAvailable() ? Log4JLogger.class : BjlLogger.class;
					}
				}
			}
		}

		return loggerClass;
	}


	/**
	 * @return ILogger for this Object
	 */
	public ILogger getLogger() {
		ILogger ret = logger;
		if( ret == null ) {
			ret = getLogger(getClass().getName());
			logger = ret;
		}

		return ret;
	}

	/**
	 * @param logger ILogger for this object. 
	 */
	public void setLogger(ILogger logger) {
		this.logger = logger;
	}

	/**
	 * @param msg The message to log if Debug logging is enabled
	 */
	public void logDebug(String msg) {
		getLogger().debug(msg);		
	}

	/**
	 * The message is only created if Debug logging is enabled.
	 * Example: logDebug(() -> "value="+expensiveCall());
	 * 
	 * @param msg Supplies the message to log if Debug logging is enabled
	 */
	public void logDebug(Supplier<String> msg) {
		getLogger().debug(msg);		
	}

	/**
	 * @param msg The message to log if Debug logging is enabled
	 * @param error The stack trace of the error is logged if Debug is enabled 
	 */
	public void logDebug(String msg, Throwable error) {
		getLogger().debug(msg,error);		
	}

	/**
	 * @param msg The message to log if Error logging is enabled
	 */
	public void logError(String msg) {
		getLogger().error(msg);

	}

	/**
	 * @param msg The message to log if Error logging is enabled
	 * @param error The stack trace of the error is logged if Error is enabled 
	 */
	public void logError(String msg, Throwable error) {
		getLogger().error(msg,error);

	}

	/**
	 * @param msg The message to log if Warn logging is enabled
	 */
	public void logWarn(String msg) {
		getLogger().warn(msg);
	}

	/**
	 * @param msg The message to log if Warn logging is enabled
	 * @param error The stack trace of the error is logged if Warn is enabled 
	 */
	public void logWarn(String msg, Throwable error) {
		getLogger().warn(msg,error);
	}

	/**
	 * @param msg The message to log if Info logging is enabled
	 */
	public void logInfo(String msg) {
		getLogger().info(msg);		
	}

	/**
	 * The message is only created if Info logging is enabled.
	 * 
	 * @param msg Supplies the message to log if Info logging is enabled
	 */
	public void logInfo(Supplier<String> msg) {
		getLogger().info(msg);		
	}

	/**
	 * @param msg The message to log if Info logging is enabled
	 * @param error log the stack trace of the error if Info is enabled 
	 */
	public void logInfo(String msg, Throwable error) {
		getLogger().info(msg,error);		
	}


	/**
	 * @return true is Debug logging is enabled
	 */
	public boolean isDebugEnabled() {
		return getLogger().isDebugEnabled();
	}

	/**
	 * @return true is Error logging is enabled
	 */

	public boolean isErrorEnabled() {
		return getLogger().isErrorEnabled();
	}

	/**
	 * @return true is Warn logging is enabled
	 */
	public boolean isWarnEnabled() {
		return getLogger().isWarnEnabled();
	}

	/**
	 * @return true is Info logging is enabled
	 */

	public boolean isInfoEnabled() {
		return getLogger().isInfoEnabled();
	}



	/**
	 * Find the ILogger for this name.  
	 * ILoggers are cached by name and shared by all objects that use the same name.
	 * 
	 * @param name
	 * @return the ILogger associated with the given name.
	 */
	protected ILogger getLogger(String name) {
		return findLogger(name);
	}

	/**
	 * Find (or create) the shared ILogger for this name.
	 * 
	 * @param name
	 * @return the ILogger associated with the given name.
	 */
	public static ILogger findLogger(String name) {
		if( name == null ) {
			name = "";
		}
		ILogger ret = loggers.get(name);
		if( ret == null ) {
			// Create outside of any lock (ILogger.init may read properties or configuration files).
			// Not using computeIfAbsent because creating a logger could recursively request another logger.
			Class<?> loggerClass = getLoggerClass();
			try {
				ILogger tmp = (ILogger) loggerClass.getDeclaredConstructor().newInstance();
				tmp.init(name);
				ILogger prev = loggers.putIfAbsent(name, tmp);
				ret = prev == null ? tmp : prev;
			} catch (Exception e) {
				throw new IllegalStateException("Fatal error occured attempting to create ILogger. loggerClass="+loggerClass,e);
			}
		}
		return ret;
	}

}
