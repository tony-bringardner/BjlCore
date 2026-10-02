// ~version~V000.01.02-V000.00.01-V000.00.00-
package us.bringardner.core;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * <PRE>
 * 
 * A general purpose BaseThread class. 
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
public abstract class BaseThread extends SecureBaseObject implements Runnable {

	
	//  the run method must set the running field to true
	//  These fields are read and written by different threads so they MUST be volatile, 
	//  otherwise the run loop may never see stopping change.
	protected volatile boolean running;
	protected volatile boolean stopping;
	protected volatile boolean started=false;
	protected volatile Thread thread;
	
	private volatile String name;
	private volatile boolean daemon=true;
	private volatile int priority = -1;

	/**
	 * @deprecated not used by BaseThread; restored in 1.2.0 for compatibility (BJL-53).
	 */
	@Deprecated
	public static final int DEFAULT_ERROR_SLEEP_TIME = 60000;
	private volatile boolean stopOnError = false;
	private volatile int errorSleepTime = DEFAULT_ERROR_SLEEP_TIME;
	
	

	private volatile ClassLoader contextClassLoader;

	/**
	 * System property for the default of {@link #setVirtual(Boolean)}: "false" (the default,
	 * so upgrading bjl_core changes nothing), "true", or "auto" (virtual threads on Java 24
	 * and later), see {@link #isVirtualDefault()}.
	 */
	public static final String VIRTUAL_THREADS_PROPERTY = "us.bringardner.core.virtualThreads";

	/** null = the default ({@link #isVirtualDefault()}), else what the caller asked for. */
	private volatile Boolean virtual;

	private volatile UncaughtExceptionHandler uncaughtExceptionHandler;
	
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
	 * A running thread's daemon status can't be changed, so the value is used
	 * the next time the thread is started.
	 * 
	 * @param daemon
	 * @see java.lang.Thread#setDaemon(boolean daemon) 
	 */
	public void setDaemon(boolean daemon) {
		this.daemon = daemon;
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
	 * @return true if this JVM can run a BaseThread on a virtual thread (Java 21 and later).
	 * On older JVMs {@link #setVirtual(Boolean)} is accepted and ignored.
	 */
	public static boolean isVirtualSupported() {
		return Threads.virtualSupported();
	}

	/**
	 * Whether a BaseThread uses a virtual thread when {@link #setVirtual(Boolean)} wasn't
	 * called (or was called with null), from the {@value #VIRTUAL_THREADS_PROPERTY} system
	 * property: "false" (also when it isn't set), "true", or "auto" =
	 * {@link #isVirtualRecommended()}. Even then a thread set to non-daemon stays a
	 * platform thread unless setVirtual(true) is called (see {@link #isVirtual()}).
	 * 
	 * @return the default; always false if virtual threads aren't supported.
	 */
	public static boolean isVirtualDefault() {
		if( !isVirtualSupported() ) {
			return false;
		}
		String value = System.getProperty(VIRTUAL_THREADS_PROPERTY, "false").trim();
		if( value.equalsIgnoreCase("true") ) {
			return true;
		} else if( value.equalsIgnoreCase("auto") ) {
			return isVirtualRecommended();
		}
		return false;
	}

	/**
	 * Virtual threads are worth using for blocking I/O on Java 24 and later. On Java 21-23 a
	 * virtual thread that blocks (for example on socket I/O) inside a synchronized block holds
	 * on to its carrier thread, which removes most of the benefit; Java 24 fixed that (JEP 491).
	 * Callers can use this for their own "auto" setting.
	 * 
	 * @return true on Java 24 and later
	 */
	public static boolean isVirtualRecommended() {
		return isVirtualSupported() && Runtime.version().feature() >= 24;
	}

	/**
	 * Run this thread on a virtual thread (true), a platform thread (false) or decide with
	 * {@link #isVirtualDefault()} (null, the default). Used the next time the thread is started.
	 * Ignored where virtual threads aren't supported (before Java 21).
	 * A virtual thread is always a daemon thread and has normal priority, so
	 * {@link #setDaemon(boolean)} and {@link #setPriority(int)} don't apply to it.
	 * 
	 * @param virtual true, false, or null for the default
	 */
	public void setVirtual(Boolean virtual) {
		this.virtual = virtual;
	}

	/**
	 * @return what was set with {@link #setVirtual(Boolean)}: true, false or null (the default)
	 */
	public Boolean getVirtual() {
		return virtual;
	}

	/**
	 * A virtual thread is always a daemon thread, so with the default (null) a thread set to
	 * non-daemon (to keep the JVM running) stays a platform thread; setVirtual(true) overrides that.
	 * 
	 * @return true if the next start will use a virtual thread
	 */
	public boolean isVirtual() {
		if( !isVirtualSupported() ) {
			return false;
		}
		Boolean v = virtual;
		return v == null ? isVirtualDefault() && isDaemon() : v.booleanValue();
	}

	/**
	 * @return true if the thread this object started (or is running on) is a virtual thread
	 */
	public boolean isVirtualThread() {
		return Threads.isVirtual(thread);
	}

	/**
	 * Creates the (unstarted) thread that runs this object; called by {@link #start()}.
	 * Subclasses can override it to supply their own thread.
	 * 
	 * @param virtual {@link #isVirtual()}
	 * @return a new thread that runs this object
	 */
	protected Thread createThread(boolean virtual) {
		return Threads.create(this, virtual);
	}

	/**
	 * @return the time (milliseconds) a subclass may sleep after an error. BaseThread itself
	 * doesn't use it.
	 * @deprecated not used by BaseThread; removed in 1.1.0 and restored in 1.2.0 for
	 * compatibility (BJL-53). Subclasses that need it should keep their own setting.
	 */
	@Deprecated
	public int getErrorSleepTime() {
		return errorSleepTime;
	}

	/**
	 * @param errorSleepTime the time (milliseconds) a subclass may sleep after an error
	 * @deprecated see {@link #getErrorSleepTime()}
	 */
	@Deprecated
	public void setErrorSleepTime(int errorSleepTime) {
		this.errorSleepTime = errorSleepTime;
	}

	/**
	 * @return true if a subclass should stop when it hits an error. BaseThread itself
	 * doesn't use it.
	 * @deprecated not used by BaseThread; removed in 1.1.0 and restored in 1.2.0 for
	 * compatibility (BJL-53). Subclasses that need it should keep their own setting.
	 */
	@Deprecated
	public boolean isStopOnError() {
		return stopOnError;
	}

	/**
	 * @param stopOnError true if a subclass should stop when it hits an error
	 * @deprecated see {@link #isStopOnError()}
	 */
	@Deprecated
	public void setStopOnError(boolean stopOnError) {
		this.stopOnError = stopOnError;
	}

	/**
	 * Start the thread processing by creating and initializing 
	 * a Thread and calling its start method. This has no impact on 
	 * the running field which should be set to true during the run method.
	 * The thread is a virtual thread if {@link #isVirtual()} is true.
	 *   
	 */
	public synchronized void start() {
		Thread current = thread;
		//  Don't start a second thread while the first one is starting or still running.
		if( !running && (current == null || !current.isAlive()) ) {
			stopping = false;
			started = false;
			thread = createThread(isVirtual());
			String name = getName();
			if( name != null ) {
				thread.setName(name);
			} else {
				setName(thread.getName());
			}
			
			// A virtual thread is always a daemon (setDaemon(false) would throw) and its
			// priority can't be changed (BJL-6)
			if( !Threads.isVirtual(thread) ) {
				thread.setDaemon(isDaemon());
				int priority = getPriority();

				if( priority != -1 ) {
					thread.setPriority(priority);
				}
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
	
	/**
	 * Stop the thread processing and wait (up to timeoutMillis) for the thread to terminate.
	 * 
	 * @param timeoutMillis maximum time to wait. Zero means don't wait. 
	 * @param interrupt if true, the thread is also interrupted (to wake it from sleep, wait, etc.).
	 * @return true if the thread has terminated. 
	 * @throws InterruptedException if the calling thread is interrupted while waiting.
	 */
	public boolean stop(long timeoutMillis, boolean interrupt) throws InterruptedException {
		stop();
		Thread current = thread;
		if( current == null ) {
			return !running;
		}
		if( interrupt ) {
			current.interrupt();
		}
		if( timeoutMillis > 0 && current != Thread.currentThread()) {
			current.join(timeoutMillis);
		}
		return !current.isAlive();
	}
	
	/**
	 * Wait for the thread to terminate.
	 * 
	 * @param timeoutMillis maximum time to wait (0 means wait forever).
	 * @throws InterruptedException
	 * @see Thread#join(long)
	 */
	public void join(long timeoutMillis) throws InterruptedException {
		Thread current = thread;
		if( current != null ) {
			current.join(timeoutMillis);
		}
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
