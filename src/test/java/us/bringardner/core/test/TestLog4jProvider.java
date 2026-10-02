package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import us.bringardner.core.BaseObject;
import us.bringardner.core.BjlLogger;
import us.bringardner.core.Log4JLogger;

/**
 * BaseObject only uses Log4JLogger by default when a log4j2 implementation (provider) is available.
 * With the log4j2 API alone, log4j2 prints only errors (after a warning) and ignores our LogLevel and LogFile.
 */
public class TestLog4jProvider {

	private static URL jarOf(String className) throws Exception {
		return Class.forName(className).getProtectionDomain().getCodeSource().getLocation();
	}

	private static boolean hasProvider(URL... urls) throws Exception {
		Method m = Log4JLogger.class.getDeclaredMethod("hasProvider", ClassLoader.class);
		m.setAccessible(true);
		try (URLClassLoader cl = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader())) {
			return (Boolean) m.invoke(null, cl);
		}
	}

	@Test
	public void testProviderDetection() throws Exception {
		URL api = jarOf("org.apache.logging.log4j.spi.Provider");
		URL core = jarOf("org.apache.logging.log4j.core.LoggerContext");

		assertFalse(hasProvider(), "No log4j at all");
		assertFalse(hasProvider(api), "The log4j API alone has no provider");
		assertTrue(hasProvider(api, core), "log4j-core is a provider");
	}

	@Test
	public void testTestClassPathHasAProvider() {
		//  log4j-core is a test dependency, so the default is still Log4JLogger here
		assertTrue(Log4JLogger.isLog4jProviderAvailable());
	}

	@Test
	public void testApiOnlyClassPathUsesBjlLogger() throws Exception {
		File coreClasses = new File(BaseObject.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		File testClasses = new File(PrintDefaultLogger.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		File api = new File(jarOf("org.apache.logging.log4j.spi.Provider").toURI());
		String cp = coreClasses+File.pathSeparator+testClasses+File.pathSeparator+api;

		String java = System.getProperty("java.home")+File.separator+"bin"+File.separator+"java";
		ProcessBuilder pb = new ProcessBuilder(java, "-cp", cp, PrintDefaultLogger.class.getName());
		pb.redirectErrorStream(true);
		Process p = pb.start();
		String out = new String(p.getInputStream().readAllBytes());
		assertTrue(p.waitFor(60, TimeUnit.SECONDS), "The JVM did not finish");
		assertEquals(0, p.exitValue(), out);

		assertTrue(out.contains("LOGGER="+BjlLogger.class.getName()), "Expected BjlLogger, got: "+out);
		assertFalse(out.contains("could not find a logging provider"), "log4j should not have been initialized: "+out);
	}
}
