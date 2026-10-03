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

	/** Longer than how often BjlLogger checks that its file hasn't been moved (1 second) */
	private static void waitForFileCheck() throws InterruptedException {
		Thread.sleep(1200);
	}

	private static List<String> messages(File file) throws IOException {
		List<String> ret = new ArrayList<>();
		for(String line : lines(file)) {
			ret.add(line.substring(line.lastIndexOf(" - ")+3));
		}
		return ret;
	}

	@Test
	public void testMovedFileIsReplaced() throws Exception {
		File dir = Files.createTempDirectory("bjl-moved").toFile();
		File log = new File(dir, "app.log");
		BjlLogger logger = logger("test.moved", props(BjlLogger.PROPERTY_LOG_FILE, log.getPath()));
		logger.error("before");

		//  What logrotate does by default: move the file, then create a new empty one
		File moved = new File(dir, "app.log.old");
		Files.move(log.toPath(), moved.toPath());
		Files.createFile(log.toPath());
		waitForFileCheck();
		logger.error("after");

		assertEquals(List.of("before"), messages(moved));
		assertEquals(List.of("after"), messages(log), "New entries should go to the new file");
	}

	@Test
	public void testDeletedFileIsRecreated() throws Exception {
		File dir = Files.createTempDirectory("bjl-deleted").toFile();
		File log = new File(dir, "app.log");
		BjlLogger logger = logger("test.deleted", props(BjlLogger.PROPERTY_LOG_FILE, log.getPath()));
		logger.error("before");

		Files.delete(log.toPath());
		waitForFileCheck();
		logger.error("after");

		assertTrue(log.exists(), "The log file should be created again");
		assertEquals(List.of("after"), messages(log));
	}

	@Test
	public void testTruncatedFileKeepsRotating() throws Exception {
		File dir = Files.createTempDirectory("bjl-truncated").toFile();
		File log = new File(dir, "app.log");
		BjlLogger logger = logger("test.truncated", props(
				BjlLogger.PROPERTY_LOG_FILE, log.getPath(),
				BjlLogger.PROPERTY_LOG_FILE_MAX_SIZE, "4K",
				BjlLogger.PROPERTY_LOG_FILE_COUNT, "1"));
		for(int i=0; i < 50; i++ ) {
			logger.error("filler "+i);
		}
		assertFalse(new File(dir, "app.log.1").exists(), "Still under 4K");
		long before = log.length();

		//  logrotate's copytruncate: the file is emptied in place
		Files.write(log.toPath(), new byte[0]);
		waitForFileCheck();
		List<String> expected = new ArrayList<>();
		for(int i=0; i < 20; i++ ) {
			logger.error("after "+i);
			expected.add("after "+i);
		}

		assertEquals(expected, messages(log));
		//  Counted from the new (empty) size. Counting the old size too would pass 4K and rotate now.
		assertTrue(before+log.length() > 4096, "The test should write enough to pass 4K counting the old size");
		assertFalse(new File(dir, "app.log.1").exists(), "The file should not have been rotated");
	}

	@Test
	public void testCloseLogFiles() throws Exception {
		File dir = Files.createTempDirectory("bjl-close").toFile();
		File log = new File(dir, "app.log");
		BjlLogger logger = logger("test.close", props(BjlLogger.PROPERTY_LOG_FILE, log.getPath()));
		logger.error("before");

		BjlLogger.closeLogFiles();
		assertEquals(List.of("before"), messages(log), "Everything logged is in the file when it is closed");

		//  Closed, so it can be moved away (Windows refuses while it is open)
		File archived = new File(dir, "app.log.archived");
		Files.move(log.toPath(), archived.toPath());

		//  No wait needed: the next entry opens the file again
		logger.error("after");
		assertEquals(List.of("before"), messages(archived));
		assertEquals(List.of("after"), messages(log));
	}
}
