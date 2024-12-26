// ~version~V000.01.11-V000.01.02-V000.00.01-V000.00.00-


package us.bringardner.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
	private static LruMap<String, Properties> properties = new LruMap<String, Properties>(DEFAULT_MAX_PROPERTIES);
	private boolean supportPrefixProperty = true;

	/**
	 * @param maxSize for the LruMap used for properties.
	 */
	public static void setMaxProperties(int maxSize) {
		properties.setMaxSize(maxSize);
	}

	/**
	 * Clear the properties cache.
	 * 
	 * Properties are cache to improve performance when accessing them.
	 * If the application uses properties only during initialization,
	 * the cache may be cleared to reduce the memory footprint.  
	 */
	public synchronized static void clearPropertyCache() {
		properties.clear();
	}

	/**
	 * propertyPrefix provides a "search path" for properties.
	 */
	private String propertyPrefix;

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
	private synchronized Properties getPropertyEntry(String name) {
		Properties ret = properties.get(name);
		if( ret == null ) {
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
				properties.put(name, ret);				
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
	 * Determine the class to use to implement the ILogger api.
	 * The objective the this class is to implement logging without creating 
	 * runtime dependencies to third party libraries. 
	 * 
	 * 1)  The System.property for "ILogger" is defined, that class is used.
	 * 2)  If the log4j library is in the class path, that is used.
	 * 3)  The default is used (java.util).
	 * 
	 * @return 
	 */
	protected static Class<?> getLoggerClass() {
		if( loggerClass == null ) {
			synchronized (BaseObject.class) {
				if( loggerClass == null ) {
					String  tmp = System.getProperty(PROPERTY_LOGGER);
					if( tmp != null ) {
						try {
							loggerClass = Class.forName(tmp);
						} catch (ClassNotFoundException e) {
							System.err.println("Defined Logger is not availible. loggerClass="+loggerClass);
						}
					}
					if( loggerClass == null ) 	{						
						try {
							Class.forName("org.apache.log4j.Level");
							loggerClass = Log4JLogger.class;
						} catch (ClassNotFoundException e) {
							loggerClass = BjlLogger.class;
						} 
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
		if( logger == null ) {
			synchronized (this) {
				if( logger == null ) {
					logger = getLogger(getClass().getName());		
				}
			}			
		}

		return logger;
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
	 * @param msg The message to log if Info logging is enabled
	 */
	public void logInfo(String msg) {
		getLogger().
		info(msg);		
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
	 * @return true is Info logging is enabled
	 */

	public boolean isInfoEnabled() {
		return getLogger().isInfoEnabled();
	}



	/**
	 * Find the ILogger for his name.  
	 * 
	 * @param name
	 * @return the ILogger associated with the given name.
	 */
	protected ILogger getLogger(String name) {
		if( logger == null ) {
			synchronized (this) {
				if( logger == null ) {
					Class<?> loggerClass = getLoggerClass();
					try {
						ILogger tmp = (ILogger) loggerClass.getDeclaredConstructor().newInstance();
						tmp.init(name);
						logger = tmp;
					} catch (Exception e) {
						throw new IllegalStateException("Fatal error occured attempting to create ILogger. loggerClass="+loggerClass,e);
					}
				}
			}
		}
		return logger;
	}



}
