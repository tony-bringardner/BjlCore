package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseObject;
import us.bringardner.core.util.LogHelper;

/**
 * Properties files for a class in a separate class loader (a plugin) are found even when the
 * object doing the lookup (here a LogHelper, which is in bjl_core) can't see that loader.
 */
public class TestPluginProperties {

	private static final String TARGET = IsolatedPropertyTarget.class.getName();
	private static final String PROPERTIES = TARGET.replace('.', '/')+".properties";

	/** Defines its own copy of the target class and is the only loader with its properties file. */
	static class PluginLoader extends ClassLoader {
		PluginLoader(ClassLoader parent) {
			super(parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if( name.equals(TARGET) ) {
				synchronized (getClassLoadingLock(name)) {
					Class<?> ret = findLoadedClass(name);
					if( ret == null ) {
						try (InputStream in = getParent().getResourceAsStream(TARGET.replace('.', '/')+".class")) {
							byte[] code = in.readAllBytes();
							ret = defineClass(name, code, 0, code.length);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}
					return ret;
				}
			}
			return super.loadClass(name, resolve);
		}

		@Override
		public InputStream getResourceAsStream(String name) {
			if( name.equals(PROPERTIES) ) {
				return new ByteArrayInputStream("Greeting=hello from the plugin\n".getBytes(StandardCharsets.ISO_8859_1));
			}
			return super.getResourceAsStream(name);
		}
	}

	@Test
	public void testPropertiesOfAClassInAnotherLoader() throws Exception {
		BaseObject.clearPropertyCache();
		try {
			Class<?> plugin = new PluginLoader(getClass().getClassLoader()).loadClass(TARGET);
			assertNotSame(IsolatedPropertyTarget.class, plugin, "The plugin class should come from its own loader");

			//  Before the fix the file was only searched for through LogHelper's class loader, so this was null.
			assertEquals("hello from the plugin", new LogHelper(plugin).getProperty("Greeting"));

			//  The class on the normal class path has no properties file, and must not get the plugin's.
			BaseObject.clearPropertyCache();
			assertNull(new LogHelper(IsolatedPropertyTarget.class).getProperty("Greeting"));
		} finally {
			BaseObject.clearPropertyCache();
		}
	}
}
