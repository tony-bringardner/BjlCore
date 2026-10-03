// ~version~V000.01.02-V000.00.01-V000.00.00-
package us.bringardner.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;


/**
 * <PRE>
 * This is a very basic logger that can be used when nothing else is available.
 * It is NOT intended for production use.
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
 *   
 *	@author Tony Bringardner   
 *
 *
 */
public class BjlLogger extends BaseObject implements ILogger {

	public static final String PROPERTY_LOG_LEVEL = "LogLevel";
	public static final String PROPERTY_LOG_FILE = "LogFile";
	/**
	 * When the log file would grow past this size it is renamed (to LogFile.1, the old LogFile.1 to LogFile.2 ...)
	 * and a new one is started. Bytes, or with a K, M or G suffix ("10M"). The default (0) is no limit.
	 */
	public static final String PROPERTY_LOG_FILE_MAX_SIZE = "LogFileMaxSize";
	/**
	 * How many old log files are kept when {@link #PROPERTY_LOG_FILE_MAX_SIZE} is set, the default is
	 * {@link #DEFAULT_LOG_FILE_COUNT}. With 0 the log file is started again from empty.
	 */
	public static final String PROPERTY_LOG_FILE_COUNT = "LogFileCount";
	public static final int DEFAULT_LOG_FILE_COUNT = 5;
	/**
	 * The default level is ERROR so that errors are never silently discarded.
	 * Set the LogLevel property (NONE, ERROR, WARN, INFO, DEBUG) to change it.
	 */
	public static final Level DEFAULT_LEVEL = Level.ERROR;
	

	/**
	 * @deprecated no longer used for formatting log entries (it serialized all logging threads).
	 * Removed in 1.1.0 and restored in 1.2.0 for compatibility (BJL-53).
	 */
	@Deprecated
	public static final us.bringardner.core.util.ThreadSafeDateFormat format = new us.bringardner.core.util.ThreadSafeDateFormat("MM-dd-yyyy HH:mm:ss.SSS");

	//  DateTimeFormatter is immutable and thread safe, so no locking is required.
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss.SSS");

	//  One PrintStream per log file, shared by all BjlLoggers, so the file is opened once (in append mode).
	private static final ConcurrentHashMap<String, PrintStream> logFiles = new ConcurrentHashMap<>();
	//  The file streams under those PrintStreams, for closeLogFiles()
	private static final ConcurrentHashMap<String, LogFileStream> logFileStreams = new ConcurrentHashMap<>();

	/**
	 * Flush every log file and close it, so another program can move or delete it (Windows doesn't allow
	 * that while the file is open). Logging can go on: a file is opened again by the next entry written to it.
	 * Call it, for example, before the files are archived, or when the application shuts down.
	 */
	public static void closeLogFiles() {
		for(PrintStream ps : logFiles.values()) {
			ps.flush();
		}
		for(LogFileStream stream : logFileStreams.values()) {
			stream.release();
		}
	}

	private volatile ILogger.Level level = DEFAULT_LEVEL;
	private String name;
	private volatile PrintStream _out;
	private volatile PrintStream _err;
	
	
	public void debug(String msg) {
		
		if( isDebugEnabled() ) {
			logMessage(Level.DEBUG,msg,getOut());
		}
	}

	public PrintStream getOut() {
		PrintStream ret = _out;
		return ret !=null ? ret : System.out;
	}

	public void setOut(PrintStream out) {
		_out = out;
	}
	
	/**
	 * @return the stream used for entries that include a stack trace. 
	 * Unless set, this is the same as getOut() so all entries stay in order in one place.
	 */
	public PrintStream getErr() {
		PrintStream ret = _err;
		return ret !=null ? ret : getOut();
	}

	public void setErr(PrintStream err) {
		_err = err;
	}
	
	public void debug(String msg, Throwable error) {
		
		if( isDebugEnabled() ) {
			logMessage(Level.DEBUG,msg,error,getErr());
		}
	}

	public void error(String msg) {
		if( isErrorEnabled() ) {
			logMessage(Level.ERROR,msg,getErr());
		}

	}

	public void error(String msg, Throwable error) {
		if( isErrorEnabled() ) {
			logMessage(Level.ERROR,msg,error,getErr());
		}

	}

	public void info(String msg) {
		if( isInfoEnabled() ) {
			logMessage(Level.INFO,msg,getOut());
		}

	}

	public void info(String msg, Throwable error) {
		if( isInfoEnabled() ) {
			logMessage(Level.INFO,msg,error,getErr());
		}

	}

	/**
	 * Initialize this logger.
	 * 
	 * The level is taken from the first of these properties that is defined:
	 * 	name.LogLevel  (specific to this logger name)
	 * 	LogLevel       
	 * 
	 * The value is not case sensitive. An invalid value is reported and the default (ERROR) is used.
	 *  
	 * LogFile may be System.out, System.err or a file name.  Log files are opened once, in append mode,
	 * and shared by all loggers that use the same file.
	 */
	public void init(String name) {
		this.name = name;
		String tmp = null;
		if( name != null && !name.isEmpty()) {
			tmp = getProperty(name+"."+PROPERTY_LOG_LEVEL);
		}
		if( tmp == null ) {
			tmp = getProperty(PROPERTY_LOG_LEVEL);
		}
		level = parseLevel(tmp, DEFAULT_LEVEL);
		
		if( (tmp=getProperty(PROPERTY_LOG_FILE)) != null) {
			tmp = tmp.trim();
			if( tmp.equals("System.out")) {
				_out = null;
				_err = null;
			} else if( tmp.equals("System.err")) {
				_out = System.err;
				_err = System.err;
			}  else {
				long maxSize = parseSize(getProperty(PROPERTY_LOG_FILE_MAX_SIZE));
				int count = getIntProperty(PROPERTY_LOG_FILE_COUNT, DEFAULT_LOG_FILE_COUNT);
				PrintStream ps = openLogFile(tmp, maxSize, count);
				if( ps != null ) {
					_out = ps;
					_err = ps;
				}
			}
		}
	}
	
	/**
	 * Convert a String to a Level (not case sensitive). 
	 * @param value
	 * @param defaultLevel returned if value is null or invalid
	 * @return the Level
	 */
	public static Level parseLevel(String value, Level defaultLevel) {
		if( value == null || value.trim().isEmpty()) {
			return defaultLevel;
		}
		String tmp = value.trim().toUpperCase(Locale.ROOT);
		switch (tmp) {
			// common aliases used by other logging frameworks
			case "OFF": return Level.NONE;
			case "SEVERE": 
			case "FATAL": return Level.ERROR;
			case "WARNING": return Level.WARN;
			case "TRACE": 
			case "ALL": 
			case "FINE": 
			case "FINER": 
			case "FINEST": return Level.DEBUG;
			default:
				try {
					return Level.valueOf(tmp);
				} catch (IllegalArgumentException e) {
					System.err.println("Invalid "+PROPERTY_LOG_LEVEL+" ("+value+"). Using "+defaultLevel);
					return defaultLevel;
				}
		}
	}

	/**
	 * Open (or find) the stream for a log file. The first logger to open a file decides its maximum
	 * size and how many old files are kept; loggers that name the same file share the stream.
	 */
	private static PrintStream openLogFile(String fileName, long maxSize, int count) {
		File file = new File(fileName).getAbsoluteFile();
		String key = file.getPath();
		try {
			key = file.getCanonicalPath();
		} catch (IOException e) {
			// use the absolute path
		}
		PrintStream ret = logFiles.get(key);
		if( ret == null ) {
			synchronized (logFiles) {
				ret = logFiles.get(key);
				if( ret == null ) {
					try {
						File dir = file.getParentFile();
						if( dir != null && !dir.exists() ) {
							dir.mkdirs();
						}
						//  Buffered, and flushed at the end of every log entry (autoflush), so an entry is
						//  written with one write instead of one per line of a stack trace.
						LogFileStream stream = new LogFileStream(file, maxSize, count);
						ret = new PrintStream(new BufferedOutputStream(stream, 8192), true);
						logFileStreams.put(key, stream);
						logFiles.put(key, ret);
					} catch (IOException e) {
						System.err.println("Error opening log file "+file+ " e="+e+". Logging to System.out");
						ret = null;
					}
				}
			}
		}
		return ret;
	}

	/**
	 * @param value a size in bytes, or with a K, M or G suffix (an optional B after it is allowed: "10MB")
	 * @return the size in bytes, 0 for null, empty or invalid values
	 */
	static long parseSize(String value) {
		if( value == null || value.trim().isEmpty() ) {
			return 0;
		}
		String tmp = value.trim().toUpperCase(Locale.ROOT);
		if( tmp.length() > 1 && tmp.endsWith("B") ) {
			tmp = tmp.substring(0, tmp.length()-1).trim();
		}
		long unit = 1;
		if( tmp.endsWith("K") ) {
			unit = 1024;
		} else if( tmp.endsWith("M") ) {
			unit = 1024*1024;
		} else if( tmp.endsWith("G") ) {
			unit = 1024L*1024*1024;
		}
		if( unit > 1 ) {
			tmp = tmp.substring(0, tmp.length()-1).trim();
		}
		try {
			long ret = Long.parseLong(tmp)*unit;
			if( ret >= 0 ) {
				return ret;
			}
		} catch (NumberFormatException e) {
		}
		System.err.println("Invalid "+PROPERTY_LOG_FILE_MAX_SIZE+" ("+value+"). The log file will not be rotated.");
		return 0;
	}

	/**
	 * Appends to a log file, starts a new one when it would grow past the maximum size, and reports
	 * write errors (a full disk, say) on System.err. PrintStream hides them, so before this, log
	 * entries could be lost without any sign.
	 * <p>
	 * At most once every {@link #CHECK_MILLIS} it also checks that the file is still there: if another
	 * program (logrotate, say) has moved or deleted it, a new file is started under the same name.
	 * Before, entries went on being written to the moved (or deleted) file, and the new one stayed empty.
	 */
	static final class LogFileStream extends OutputStream {
		/** How often (milliseconds) a write checks that the file hasn't been moved or deleted. */
		static final long CHECK_MILLIS = 1000;

		private final File file;
		private final long maxSize;
		private final int count;
		//  null when the file isn't open (see release()); the next write opens it
		private FileOutputStream out;
		private long size;
		//  The file system's identity of the open file (null where there isn't one, Windows for example)
		private Object fileKey;
		private long lastCheck;
		//  true from the first failed write until a write works again, so each failure is reported once
		private boolean failing;

		LogFileStream(File file, long maxSize, int count) throws IOException {
			this.file = file;
			this.maxSize = maxSize;
			this.count = Math.max(count, 0);
			open();
		}

		private void open() throws IOException {
			out = new FileOutputStream(file, true);
			size = file.length();
			fileKey = fileKey();
			lastCheck = System.nanoTime();
		}

		private Object fileKey() {
			try {
				return Files.readAttributes(file.toPath(), BasicFileAttributes.class).fileKey();
			} catch (IOException | RuntimeException e) {
				return null;
			}
		}

		/**
		 * Start a new file if the open one has been moved or deleted (checked at most every CHECK_MILLIS).
		 * A file that was truncated in place (logrotate's copytruncate) is still written to, from its new end.
		 */
		private void checkFile() throws IOException {
			long now = System.nanoTime();
			if( now-lastCheck < CHECK_MILLIS*1_000_000L ) {
				return;
			}
			lastCheck = now;
			Object key = fileKey();
			if( key == null && !file.exists() ) {
				//  Deleted, or moved and not replaced
				reopen();
			} else if( key != null && fileKey != null && !key.equals(fileKey) ) {
				//  Moved, and a new file created under the name
				reopen();
			} else {
				long length = file.length();
				if( length < size ) {
					//  Truncated: count from the new size so rotation by size still works
					size = length;
				}
			}
		}

		private void reopen() throws IOException {
			try {
				out.close();
			} catch (IOException e) {
			}
			out = null;
			open();
		}

		/**
		 * Close the file (see {@link BjlLogger#closeLogFiles()}); the next write opens it again.
		 */
		synchronized void release() {
			if( out != null ) {
				try {
					out.close();
				} catch (IOException e) {
				}
				out = null;
			}
		}

		@Override
		public synchronized void write(int b) throws IOException {
			write(new byte[] {(byte) b}, 0, 1);
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			try {
				if( out == null ) {
					//  Closed by release()
					open();
				} else {
					checkFile();
				}
				if( maxSize > 0 && size > 0 && size+len > maxSize ) {
					rotate();
				}
				out.write(b, off, len);
				size += len;
				if( failing ) {
					failing = false;
					System.err.println("BjlLogger: writing to "+file+" works again.");
				}
			} catch (IOException e) {
				if( !failing ) {
					failing = true;
					System.err.println("BjlLogger: can't write to "+file+" ("+e+"). Log entries are being lost.");
				}
				throw e;
			}
		}

		/** file.1 is the newest old file, file.&lt;count&gt; the oldest */
		private File old(int i) {
			return new File(file.getPath()+"."+i);
		}

		private void rotate() throws IOException {
			try {
				out.close();
			} catch (IOException e) {
			}
			try {
				if( count > 0 ) {
					for(int i=count-1; i > 0; i-- ) {
						File from = old(i);
						if( from.exists() ) {
							Files.move(from.toPath(), old(i+1).toPath(), StandardCopyOption.REPLACE_EXISTING);
						}
					}
					Files.move(file.toPath(), old(1).toPath(), StandardCopyOption.REPLACE_EXISTING);
				} else {
					Files.delete(file.toPath());
				}
			} catch (IOException e) {
				System.err.println("BjlLogger: can't start a new log file, still appending to "+file+" ("+e+")");
				open();
				//  Don't try again until the file has grown by another maxSize
				size = 0;
				return;
			}
			open();
		}

		@Override
		public synchronized void flush() throws IOException {
			if( out != null ) {
				out.flush();
			}
		}

		@Override
		public synchronized void close() throws IOException {
			if( out != null ) {
				out.close();
				out = null;
			}
		}
	}

	protected void logMessage(Level level,String msg,PrintStream out) {
		out.println(formatMessage(level, msg));
	}

	private void logMessage(Level level,String msg,Throwable error, PrintStream out) {
		String line = formatMessage(level, msg);
		if( error == null ) {
			out.println(line);
		} else {
			//  One print, so the message and stack trace stay together when several threads are logging
			//  and a log file gets them in one write (printStackTrace writes and flushes every line).
			StringWriter text = new StringWriter();
			PrintWriter pw = new PrintWriter(text);
			pw.println(line);
			error.printStackTrace(pw);
			pw.flush();
			out.print(text.toString());
			out.flush();
		}
	}

	private String formatMessage(Level level,String msg) {
		return TIME_FORMAT.format(LocalDateTime.now())+" ["+Thread.currentThread().getName()+"] "+level+" "+name+" - "+ msg;
	}

	public boolean isDebugEnabled() {		
		return isEnabled(ILogger.Level.DEBUG);
	}

	public boolean isErrorEnabled() {
		return isEnabled(ILogger.Level.ERROR);
	}

	public boolean isInfoEnabled() {		
		return isEnabled(ILogger.Level.INFO);
	}
	
	public boolean isWarnEnabled() {	
		return isEnabled(ILogger.Level.WARN);
	}

	private boolean isEnabled(ILogger.Level target) {
		Level current = level;
		return current != ILogger.Level.NONE && current.ordinal() >= target.ordinal();
	}

	public void setLevel(Level level) {
		if( level == null ) {
			this.level = Level.NONE;
		} else {
			this.level = level;
		}
	}

	public Level getLevel() {
		return level;
	}

	

	public void warn(String msg) {
		if( isWarnEnabled() ) {
			logMessage(Level.WARN,msg,getOut());
		}		
	}

	public void warn(String msg, Throwable error) {

		if( isWarnEnabled() ) {
			logMessage(Level.WARN,msg,error,getErr());
		}		
	}

}
