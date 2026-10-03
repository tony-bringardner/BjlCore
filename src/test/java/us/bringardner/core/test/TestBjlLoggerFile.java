package us.bringardner.core.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import us.bringardner.core.BjlLogger;

/**
 * BjlLogger log files: rotation by size, stack traces written whole, and write errors reported.
 */
public class TestBjlLoggerFile {

	private static final String PREFIX = "us.bringardner.core.BjlLogger.";

	/** Create a logger with the given properties (cleared again afterwards). */
	private static BjlLogger logger(String name, Map<String, String> props) {
		for (Map.Entry<String, String> e : props.entrySet()) {
			System.setProperty(PREFIX+e.getKey(), e.getValue());
		}
		try {
			BjlLogger ret = new BjlLogger();
			ret.init(name);
			return ret;
		} finally {
			for (String key : props.keySet()) {
				System.clearProperty(PREFIX+key);
			}
		}
	}

	private static Map<String, String> props(String ... nameValue) {
		Map<String, String> ret = new HashMap<>();
		for(int i=0; i < nameValue.length; i+=2 ) {
			ret.put(nameValue[i], nameValue[i+1]);
		}
		return ret;
	}

	private static List<String> lines(File file) throws IOException {
		return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
	}

	private static int number(String line) {
		return Integer.parseInt(line.substring(line.lastIndexOf(' ')+1));
	}

	@Test
	public void testRotation() throws IOException {
		File dir = Files.createTempDirectory("bjl-rotate").toFile();
		File log = new File(dir, "app.log");
		BjlLogger logger = logger("test.rotate", props(
				BjlLogger.PROPERTY_LOG_FILE, log.getPath(),
				BjlLogger.PROPERTY_LOG_FILE_MAX_SIZE, "1K",
				BjlLogger.PROPERTY_LOG_FILE_COUNT, "2"));
		for(int i=0; i < 200; i++ ) {
			logger.error("message number "+i);
		}

		assertTrue(log.exists());
		File one = new File(dir, "app.log.1");
		File two = new File(dir, "app.log.2");
		assertTrue(one.exists() && two.exists(), "Two old files are kept");
		assertFalse(new File(dir, "app.log.3").exists(), "Older files are deleted");
		for(File f : new File[] {log, one, two}) {
			assertTrue(f.length() <= 1024, f+" is "+f.length()+" bytes");
		}

		//  The files hold consecutive messages, newest in app.log, and none were lost between them
		List<String> all = new ArrayList<>(lines(two));
		all.addAll(lines(one));
		all.addAll(lines(log));
		assertEquals(199, number(all.get(all.size()-1)));
		for(int i=1; i < all.size(); i++ ) {
			assertEquals(number(all.get(i-1))+1, number(all.get(i)), "Message missing before: "+all.get(i));
		}
	}

	@Test
	public void testNoRotationByDefault() throws IOException {
		File dir = Files.createTempDirectory("bjl-norotate").toFile();
		File log = new File(dir, "app.log");
		BjlLogger logger = logger("test.norotate", props(BjlLogger.PROPERTY_LOG_FILE, log.getPath()));
		for(int i=0; i < 200; i++ ) {
			logger.error("message number "+i);
		}
		assertTrue(log.length() > 10000);
		assertEquals(1, dir.list().length, "Only the log file");
		//  Every entry is flushed as it is logged (nothing is waiting in the buffer)
		assertEquals(200, lines(log).size());
	}

	@Test
	public void testOversizedFileIsRotatedOnFirstWrite() throws IOException {
		File dir = Files.createTempDirectory("bjl-oversized").toFile();
		File log = new File(dir, "app.log");
		Files.write(log.toPath(), new byte[5000]);
		BjlLogger logger = logger("test.oversized", props(
				BjlLogger.PROPERTY_LOG_FILE, log.getPath(),
				BjlLogger.PROPERTY_LOG_FILE_MAX_SIZE, "4KB",
				BjlLogger.PROPERTY_LOG_FILE_COUNT, "0"));
		logger.error("first");
		//  With LogFileCount 0 no old file is kept
		assertEquals(1, dir.list().length);
		assertTrue(log.length() < 1000, "The old content was removed, the file is "+log.length()+" bytes");
		assertEquals(1, lines(log).size());
		assertTrue(lines(log).get(0).endsWith("test.oversized - first"));
	}

	@Test
	public void testStackTracesStayWithTheirMessage() throws Exception {
		File dir = Files.createTempDirectory("bjl-traces").toFile();
		File log = new File(dir, "app.log");
		BjlLogger logger = logger("test.traces", props(BjlLogger.PROPERTY_LOG_FILE, log.getPath()));

		int threads = 4, each = 50;
		CountDownLatch start = new CountDownLatch(1);
		List<Thread> list = new ArrayList<>();
		for(int t=0; t < threads; t++ ) {
			final int id = t;
			Thread th = new Thread(() -> {
				try {
					start.await();
				} catch (InterruptedException e) {
					return;
				}
				for(int i=0; i < each; i++ ) {
					logger.error("entry "+id+"-"+i, new Exception("trace "+id+"-"+i));
				}
			});
			th.start();
			list.add(th);
		}
		start.countDown();
		for (Thread th : list) {
			th.join();
		}

		List<String> lines = lines(log);
		int entries = 0;
		for(int i=0; i < lines.size(); i++ ) {
			String line = lines.get(i);
			if( line.contains("test.traces - entry ") ) {
				entries++;
				String id = line.substring(line.indexOf("entry ")+6);
				assertEquals("java.lang.Exception: trace "+id, lines.get(i+1), "The stack trace must follow its message");
				assertTrue(lines.get(i+2).trim().startsWith("at "), lines.get(i+2));
			}
		}
		assertEquals(threads*each, entries);
	}

	@Test
	public void testWriteErrorsAreReportedOnce() {
		//  Writing to /dev/full always fails with "No space left on device" (Linux)
		File full = new File("/dev/full");
		Assumptions.assumeTrue(full.exists(), "Needs /dev/full");
		PrintStream err = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		System.setErr(new PrintStream(captured, true));
		try {
			BjlLogger logger = logger("test.full", props(BjlLogger.PROPERTY_LOG_FILE, full.getPath()));
			for(int i=0; i < 5; i++ ) {
				logger.error("lost "+i);
			}
		} finally {
			System.setErr(err);
		}
		String text = captured.toString();
		int first = text.indexOf("can't write to /dev/full");
		assertTrue(first >= 0, "The failure is reported: "+text);
		assertEquals(-1, text.indexOf("can't write to /dev/full", first+1), "Only once: "+text);
	}
}
