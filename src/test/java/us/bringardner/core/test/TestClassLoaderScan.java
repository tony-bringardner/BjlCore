package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.jupiter.api.Test;

import us.bringardner.core.util.SearchableClassLoader;

/**
 * SearchableClassLoader only loads the classes that can match, and still finds every one that does.
 * The log4j jars (about 2000 classes) are searched in a loader of their own, so their classes are
 * defined (and counted) by that loader.
 */
public class TestClassLoaderScan {

	/** Counts the classes this loader is asked for */
	static class CountingLoader extends SearchableClassLoader {
		final AtomicInteger loads = new AtomicInteger();

		CountingLoader(URL[] urls) {
			super(urls, ClassLoader.getPlatformClassLoader());
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			loads.incrementAndGet();
			return super.loadClass(name);
		}
	}

	private static File jarOf(String className) throws Exception {
		return new File(Class.forName(className).getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	private static File[] log4jJars() throws Exception {
		return new File[] {
				jarOf("org.apache.logging.log4j.core.LoggerContext"),
				jarOf("org.apache.logging.log4j.LogManager")
		};
	}

	private static URL[] urls(File[] jars) throws Exception {
		URL[] ret = new URL[jars.length];
		for(int i=0; i < jars.length; i++ ) {
			ret[i] = jars[i].toURI().toURL();
		}
		return ret;
	}

	/** What the old SearchableClassLoader did: load every class and check it */
	private static List<Class<?>> loadEverything(File[] jars, Class<?> target, boolean indirect, int[] classCount) throws Exception {
		Set<Class<?>> ret = new LinkedHashSet<>();
		try (URLClassLoader2 loader = new URLClassLoader2(urls(jars))) {
			for (File jar : jars) {
				try (ZipFile zip = new ZipFile(jar)) {
					Enumeration<? extends ZipEntry> e = zip.entries();
					while( e.hasMoreElements() ) {
						String entry = e.nextElement().getName();
						if( !entry.endsWith(".class") || entry.endsWith("module-info.class") || entry.startsWith("META-INF/") ) {
							continue;
						}
						classCount[0]++;
						try {
							Class<?> cls = loader.loadClass(entry.substring(0, entry.length()-6).replace('/', '.'));
							boolean match = cls == target
									|| (indirect ? target.isAssignableFrom(cls)
											: cls.getSuperclass() == target || List.of(cls.getInterfaces()).contains(target));
							if( match ) {
								ret.add(cls);
							}
						} catch (ClassNotFoundException | LinkageError ex) {
							// optional dependencies of log4j-core
						}
					}
				}
			}
		}
		return new ArrayList<>(ret);
	}

	static class URLClassLoader2 extends java.net.URLClassLoader {
		URLClassLoader2(URL[] urls) {
			super(urls, ClassLoader.getPlatformClassLoader());
		}
	}

	private static List<String> names(List<Class<?>> list) {
		List<String> ret = new ArrayList<>();
		for (Class<?> c : list) {
			ret.add(c.getName());
		}
		return ret;
	}

	private void check(Class<?> target, boolean indirect) throws Exception {
		File[] jars = log4jJars();
		int[] classCount = new int[1];
		List<Class<?>> expected = loadEverything(jars, target, indirect, classCount);

		List<Class<?>> found;
		int loads;
		try (CountingLoader loader = new CountingLoader(urls(jars))) {
			found = loader.findTarget(target, indirect);
			loads = loader.loads.get();
		}

		assertFalse(expected.isEmpty(), "The test needs some matches");
		//  The same classes, in the same order (classes from different loaders, so compare names)
		assertEquals(names(expected), names(found));
		//  Before, every class was loaded (about 2000). Now only the matches (and what they need) are.
		assertTrue(loads < classCount[0]/4, "Loaded "+loads+" classes to find "+found.size()+" of "+classCount[0]);
	}

	@Test
	public void testDirectImplementations() throws Exception {
		check(Runnable.class, false);
	}

	@Test
	public void testIndirectImplementations() throws Exception {
		check(Runnable.class, true);
	}

	@Test
	public void testIndirectInterfaceWithManyImplementations() throws Exception {
		check(Closeable.class, true);
	}

	@Test
	public void testDirectSubclasses() throws Exception {
		check(Thread.class, false);
	}

	@Test
	public void testParallelCapable() throws Exception {
		try(SearchableClassLoader loader = new SearchableClassLoader(new URL[0], getClass().getClassLoader())) {
			assertTrue(loader.isRegisteredAsParallelCapable(), "loadClass should not lock the whole loader");
		}
	}
}
