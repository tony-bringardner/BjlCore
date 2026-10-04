package us.bringardner.core;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <PRE>
 * A ThreadFactory for executors that names its threads and makes them daemon threads (by default),
 * so an executor's threads show up by name in thread dumps and don't keep the JVM running.
 *
 * new NamedThreadFactory("ZoneNotifier")        threads "ZoneNotifier", "ZoneNotifier-2", ...
 * NamedThreadFactory.numbered("TCPConn")        threads "TCPConn1", "TCPConn2", ...
 * new NamedThreadFactory("Worker").daemon(false)  non-daemon threads
 * new NamedThreadFactory("Worker").virtual(true)  virtual threads on Java 21+ (platform threads before)
 *
 * Instances are immutable and thread safe; daemon() and virtual() return a new factory.
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
 *  @author Tony Bringardner
 */
public final class NamedThreadFactory implements ThreadFactory {

	private final String name;
	//  true: name+1, name+2 ...; false: name, name-2, name-3 ...
	private final boolean numbered;
	private final boolean daemon;
	private final boolean virtual;
	private final AtomicInteger count;

	/**
	 * Daemon threads named name, then name-2, name-3 ... if more than one is made (so a single
	 * thread executor's thread is simply called name).
	 *
	 * @param name the thread name
	 */
	public NamedThreadFactory(String name) {
		this(name, false, true, false, new AtomicInteger());
	}

	private NamedThreadFactory(String name, boolean numbered, boolean daemon, boolean virtual, AtomicInteger count) {
		this.name = Objects.requireNonNull(name, "name is required");
		this.numbered = numbered;
		this.daemon = daemon;
		this.virtual = virtual;
		this.count = count;
	}

	/**
	 * @param prefix the start of every thread's name
	 * @return a factory for daemon threads named prefix1, prefix2 ...
	 */
	public static NamedThreadFactory numbered(String prefix) {
		return new NamedThreadFactory(prefix, true, true, false, new AtomicInteger());
	}

	/**
	 * @param daemon false for threads that keep the JVM running (ignored for virtual threads,
	 *  which are always daemon threads)
	 * @return a factory like this one that makes daemon (or non-daemon) threads
	 */
	public NamedThreadFactory daemon(boolean daemon) {
		return new NamedThreadFactory(name, numbered, daemon, virtual, count);
	}

	/**
	 * @param virtual true for virtual threads where the JVM has them (Java 21+); platform threads otherwise
	 * @return a factory like this one that makes virtual (or platform) threads
	 */
	public NamedThreadFactory virtual(boolean virtual) {
		return new NamedThreadFactory(name, numbered, daemon, virtual, count);
	}

	@Override
	public Thread newThread(Runnable task) {
		int n = count.incrementAndGet();
		Thread ret = Threads.create(task, virtual);
		ret.setName(numbered ? name+n : (n == 1 ? name : name+"-"+n));
		if( !Threads.isVirtual(ret) ) {
			ret.setDaemon(daemon);
		}
		return ret;
	}

	@Override
	public String toString() {
		return "NamedThreadFactory["+name+(numbered ? "#" : "")+(daemon ? ", daemon" : "")+(virtual ? ", virtual" : "")+"]";
	}
}
