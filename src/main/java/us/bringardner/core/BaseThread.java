// ~version~V000.01.02-V000.00.01-V000.00.00-
package us.bringardner.core;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * <PRE>
 * 
 * A general purpose BaseThread class. 
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
public abstract class BaseThread extends SecureBaseObject implements Runnable {

	public static final int DEFAULT_ERROR_SLEEP_TIME = 60000;
	
	//  the run method must set the running field to true
	protected boolean running;
	protected boolean stopping;
	protected boolean started=false;
	
	private String name;
	private boolean daemon=true;
	private int priority = -1;
	private boolean stopOnError = false;
	
	private Thread thread;
	private int errorSleepTime = DEFAULT_ERROR_SLEEP_TIME;

	private ClassLoader contextClassLoader;

	private UncaughtExceptionHandler uncaughtExceptionHandler;
	
	public BaseThread() {
		super();
	}

	public BaseThread(String name) {
		this();
		setName(name);
	}
	
	public BaseThread(boolean isDaemon) {
		this();
		setDaemon(isDaemon);
	}
	
	public BaseThread(String name,boolean isDaemon) {
		super();
		setName(name);
		setDaemon(isDaemon);
	}
	
	/**
	 * Set the uncaughtExceptionHandler used by this thread.
	 * 
	 * @param handler
	 * @see java.lang.Thread#setUncaughtExceptionHandler(UncaughtExceptionHandler handler)
	 */
	public 	void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
		this.uncaughtExceptionHandler = handler;
		if( thread != null ) {
			thread.setUncaughtExceptionHandler(handler);
		}
	}
	
	
	/**
	 * @return the uncaughtExceptionHandler used by this thread.
	 * @see java.lang.Thread#getUncaughtExceptionHandler()
	 */
	public UncaughtExceptionHandler getUncaughtExceptionHandler() {
		return uncaughtExceptionHandler;
	}

	/**
	 * Set the contextClassLoader used by this thread.
	 * 
	 * @param contextClassLoader
	 * @see java.lang.Thread#setContextClassLoader(ClassLoader contextClassLoader)
	 */
	public void setContextClassLoader(ClassLoader contextClassLoader) {
		this.contextClassLoader = contextClassLoader;
		if( thread != null ) {
			thread.setContextClassLoader(contextClassLoader);
		}
	}
	
	
	/**
	 * @return the contextClassLoader used by this thread.
	 * @see java.lang.Thread#getContextClassLoader() 
	 */
	public ClassLoader getContextClassLoader() {
		return contextClassLoader;
	}

	/**
	 * @return the amount of time to sleep if we encounter an error isStopOnError() is false..  
	 */
	public int getErrorSleepTime() {
		return errorSleepTime;
	}

	/**
	 * @return true is this thread is still running (until the run method terminates).
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * @return true if the stop method has been called and the run method has not exited.
	 */
	public boolean isStopping() {
		return stopping;
	}

	/**
	 * @return the name of this thread.
	 * @see java.lang.Thread#getName()
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of this thread.
	 * 
	 * @param name
	 * @see java.lang.Thread#setName(String name)
	 */
	public void setName(String name) {
		this.name = name;
		if( thread != null ) {
			thread.setName(name);
		}
	}

	/**
	 * @return true if this thread should run as a daemon.  
	 * The JVM will terminate when on 'non-daemon' threads have terminated. 
	 * @see java.lang.Thread#isDaemon() 
	 */
	public boolean isDaemon() {
		return daemon;
	}

	/**
	 * Set the daemon flag.  
	 * The JVM will terminate when on 'non-daemon' threads have terminated.
	 * 
	 * @param daemon
	 * @see java.lang.Thread#setDaemon(boolean daemon) 
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
		if( thread != null ) {
			this.thread.setDaemon(daemon);
		}
	}

	/**
	 * @return the priority of this thread.
	 * @see java.lang.Thread#getPriority()
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Set the priority of this thread.
	 * 
	 * @param priority
	 * @see java.lang.Thread#setPriority(int priority)
	 */
	public void setPriority(int priority) {
		this.priority = priority;
		if( thread != null ) {
			thread.setPriority(priority);
		}
	}

		
	/**
	 * 
	 * @return true is the thread should stop when an error is encountered.
	 */
	public boolean isStopOnError() {
		return stopOnError;
	}

	/**
	 * Set to true is the thread should stop when an error is encountered.
	 * 
	 * @param stopOnError
	 */
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}


	/**
	 * Start the thread processing by creating and initializing 
	 * a Thread and calling its start method. This has no impact on 
	 * the running field which should be set to true during the run method.
	 *   
	 */
	public void start() {
		if( !running ) {
			thread = new Thread(this);
			String name = getName();
			if( name != null ) {
				thread.setName(name);
			} else {
				setName(thread.getName());
			}
			
			thread.setDaemon(isDaemon());
			int priority = getPriority();
			
			if( priority != -1 ) {
				thread.setPriority(priority);
			} 
			
			ClassLoader loader = getContextClassLoader();
			if( loader != null ) {
				thread.setContextClassLoader(loader);
			} 
			
			UncaughtExceptionHandler eh = getUncaughtExceptionHandler();
			if( eh != null ) {
				thread.setUncaughtExceptionHandler(eh);
			}
			
			thread.start();
		}
	}
	
	/**
	 * Stop the thread processing.  Calling this method will change the 'stopping' 
	 * field to true.  The 'running' field will remain true until the run method has terminated.
	 *  
	 */
	public void stop() {
		stopping = true;
	}
	
	public boolean hasStarted() {
		return started;
	}

	/**
	 * When implementing the run method, make sure to use the variable started, running and stopping 
	 * to coordinate activities like this.
	 *  
	 *  public void run() {
	 *  	started = running = true;
	 *  	while(!stopping ) {
	 *  		...
	 *  	}
	 *  	running = false;
	 *  }
	 *  
	 */
	public abstract void run() ;

}
