package us.bringardner.core;

/**
 * Creates the threads {@link BaseThread} runs on (BJL-6).
 * <p>
 * This is the Java 21 version, packaged in META-INF/versions/21 of the multi-release jar and
 * used only by Java 21+ JVMs; Java 11-20 use src/main/java/us/bringardner/core/Threads.java.
 * Both copies must keep the same methods.
 */
final class Threads {

	private Threads() {
	}

	/** @return true: this JVM can create virtual threads. */
	static boolean virtualSupported() {
		return true;
	}

	/**
	 * @param task what the thread runs
	 * @param virtual true for a virtual thread, false for a platform thread
	 * @return a new, unstarted thread
	 */
	static Thread create(Runnable task, boolean virtual) {
		return virtual ? Thread.ofVirtual().unstarted(task) : new Thread(task);
	}

	/** @return true if the thread is a virtual thread */
	static boolean isVirtual(Thread thread) {
		return thread != null && thread.isVirtual();
	}
}
