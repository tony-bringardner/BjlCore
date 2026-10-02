package us.bringardner.core;

/**
 * Creates the threads {@link BaseThread} runs on (BJL-6).
 * <p>
 * This is the Java 11 version: virtual threads don't exist, so every thread is a platform
 * thread. bjl_core is a multi-release jar: on Java 21 and later the JVM loads the copy in
 * META-INF/versions/21 instead (source in src/main/java21), which can create virtual threads.
 * Both copies must keep the same methods.
 */
final class Threads {

	private Threads() {
	}

	/** @return true if this JVM can create virtual threads (Java 21+). */
	static boolean virtualSupported() {
		return false;
	}

	/**
	 * @param task what the thread runs
	 * @param virtual ignored here: there are no virtual threads before Java 21
	 * @return a new, unstarted platform thread
	 */
	static Thread create(Runnable task, boolean virtual) {
		return new Thread(task);
	}

	/** @return true if the thread is a virtual thread (never, before Java 21) */
	static boolean isVirtual(Thread thread) {
		return false;
	}
}
