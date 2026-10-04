# Changelog

## 1.3.1 (unreleased)

### Fixed

- `SecureBaseObject.getSSLContext()` could return null when a setter (`setTrustManagers`, say) ran while
  the context was being built: the setter waited for the build, then cleared the cached context before the
  getter returned it. `getKeyManagers()`, `getKeyManagerFactory()`, `getKeyStore()`,
  `SocketClient.getSocketFactory()` and `AbstractCoreServer.getServerSocketFactory()` had the same race.
  They now return the value they built or found. `testSetterDuringBuildIsNotLost` failed about half the
  time because of this.
- `SecureBaseObject.getKeyManagers()` left the key store password in a `char[]` that was never cleared.
  It is now cleared once the key store is loaded and the key managers are made, or loading fails.
- When `KeyStorePassword` or `KeyStoreName` wasn't set, every call to `getKeyStorePassword()` or
  `getKeyStoreFileName()` looked the property up again (taking the object's lock) and logged it again
  at debug level. Each is now looked up once.

### Changed (may need a code change)

- A `KeyStorePassword` or `KeyStoreName` property that wasn't set when it was first needed is not looked
  for again, so setting it later (as a system property, say) has no effect on that object. Call
  `setKeyStorePassword` / `setKeyStoreFileName`; setting null looks the property up again.

### Removed

- `us.bringardner.core.swing.ComponentId`, and `getComponentName()` / `getComponentId()` on `Clock`,
  `DatePanel`, `DayPanel` and `TimePanel`. It only gave the Swing components a fixed name and number for
  tests, and nothing else used it; tests find components by `Component.getName()`, which the components
  have set since 1.2.0 (for example `hourSpinner`, `todayButton`, `day1`). Strictly an incompatible change,
  made in a patch release because no BJL project uses it: code that called these methods should use
  `getClass()` or `getName()` instead.

## 1.3.0

### Added

- `util.AddressMatcher`: lists of IP addresses and networks (CIDR) for allow lists, moved here from
  bjl_dns so other projects can use it (bjl_email's relay networks do). Only address literals are
  accepted, never a host name, so parsing never does a DNS lookup: IPv4 is parsed here, because
  `InetAddress.getByName` looks an invalid literal such as `1.2.3.999` up as a host name. A prefix
  length must fit the address (0-32, 0-128). Also `isAddressLiteral(String)` and `and(AddressMatcher)`.
- `util.TlsSockets`: `layer()` puts TLS on a connected socket (STARTTLS, FTP's AUTH TLS),
  `configureClient()` sets SNI and the HTTPS host name check on a client `SSLSocket`, and
  `hostnameVerifying()` wraps a socket factory so its sockets check the host name. bjl_email,
  bjl_net_framework and bjl_net_ftp each had their own version (bjl_net_ftp's had no host name check).
- `util.TrustAllCertificates`: the one, clearly named, trust manager that accepts every certificate,
  for opportunistic TLS and tests, with `trustManagers()` and `sslContext(protocol)`.
- `SocketClient.startTls(Socket, String)`: TLS on a connected socket with the client's `SSLContext`
  and host name check; the handshake is done and the socket closed if it fails.

## 1.2.0

### Fixed

- An `AbstractCoreServer` could not be started again after it stopped: `getServerSocket()` returned
  the cached socket even after it had been closed, so `accept()` failed. A closed socket is now replaced.
- `AbstractCoreServer.stop()` only set `stopping`, so the server kept running until `accept()` timed out
  (60 seconds by default). It now also closes the listening socket so `accept()` returns at once.
  Connections that were already accepted are not affected.
- `SocketClient.getSocket()` had no connect timeout, so an unreachable host blocked for as long as the
  operating system kept trying (about 75 seconds on macOS, minutes on Linux). It also left the socket
  open if `configure()` threw. The socket is now closed on any failure.
- A properties file for a class in another class loader (a plugin, say) was not found when the lookup
  was made by an object whose class can't see that loader, such as `new LogHelper(PluginClass.class)`
  or a `getPropertyClass()` override. If the file isn't found through the object's own class it is now
  also looked for through the class being searched.
- With only the log4j2 API on the class path (no log4j-core or other implementation), `BaseObject` still
  chose `Log4JLogger`. log4j2 then printed a warning, logged only errors to the console and ignored the
  `LogLevel` and `LogFile` properties. `BjlLogger` is now the default unless a log4j2 implementation is
  found, and log4j2 isn't started just to find out. Naming `Log4JLogger` in the `ILogger` property still
  uses it.
- `DayPanel` was wrong where the week starts on Monday (most of Europe): the month started on the 2nd,
  so the 1st was missing and clicking a day selected the next one. It now follows the locale's first
  day of the week, and the day names in the header come from the locale (`S M T W T F S` in the US,
  as before).
- `Clock` drew into a 1x image and copied it to the screen, so it was blurry on high resolution
  (Retina) displays, leaked a `Graphics2D` on every repaint, and threw a `NullPointerException` when
  painted before it was displayable or with no size. It now draws directly in `paintComponent`.
  Pressing the mouse on a clock before its first paint also threw a `NullPointerException`.
- With `Log4JLogger`, log4j layouts that show the caller (`%C`, `%M`, `%L`, `%l`) showed
  `Log4JLogger.invoke` for every message. They now show the code that logged, whether it called the
  logger directly or through `BaseObject.logError` (etc.), including the `Supplier` versions.
- A server could keep its port after it had stopped. If `stop()` was called before the run method
  asked for its socket (a `start()` followed at once by `stop()`, say), `getServerSocket()` opened a
  new socket that nothing closed. While the server thread is stopping it now throws a `SocketException`
  instead; once the thread has ended it works as before.
- A `BaseThread` whose run method threw could not be started again: `running` stayed true, so
  `start()` did nothing. `running` is now cleared when the run method ends, however it ends, and
  `start()` checks whether the thread is alive.
- `JulLogger` logged debug messages at `FINEST`, so with the java.util.logging level at `FINE` or
  `FINER`, `getLevel()` said `DEBUG` but debug messages weren't logged. They are now logged at `FINE`,
  and at `CONFIG` `getLevel()` says `INFO`.
- `JulLogger` reported `JulLogger.log` as the source class and method of every message. It now
  reports the code that logged, whether it called the logger directly or through `BaseObject.logError`
  (etc.), including the `Supplier` versions.
- `SearchableClassLoader.findTarget` followed symbolic links to directories without noticing it had been
  there before. A link back to a parent directory made it search the same files again and again; with
  two such links the search never finished. Each directory and jar is now searched once.
- `SocketClient.getSocket()` returned secure sockets before the TLS handshake, so a bad certificate,
  a host name that doesn't match it or a server that doesn't answer TLS failed on the caller's first
  read or write, and the socket was left for the caller to close. The handshake is now done in
  `getSocket()` (with the socket timeout), and the socket is closed if it fails.
- Changing a security setting on a `SecureBaseObject` (`setKeyStorePassword`, `setTrustManagers`,
  `setProtocol` ...) while another thread was in `getSSLContext()` could be lost: the context being
  built from the old settings was kept after the setter had cleared it, and used from then on. The
  setters (and `resetSecurityContext()`, also in `AbstractCoreServer` and `SocketClient`) are now
  synchronized like the getters, so a setter waits for a build in progress and then clears it.
- `SecureBaseObject.getSSLContext()` caught `Throwable`, so an `Error` such as `OutOfMemoryError` came
  back as an `IOException`. It now wraps only `GeneralSecurityException`, `IOException` and
  `RuntimeException`.
- The properties file cache was shared by every class loader, so in plugin or application server setups
  the first loader searched decided the result for all of them: a plugin's file could be returned for a
  class with the same name in another loader, or a cached "not found" could hide it. The cache is now
  kept per class loader, and doesn't keep a loader from being garbage collected.
- `BjlLogger` kept writing to its log file after another program (logrotate, say) had moved or deleted
  it, so new entries went to the moved file, or were lost, and the new file stayed empty. At most once a
  second a write now checks the file and starts a new one under the same name if it has gone. A file
  emptied in place (logrotate's `copytruncate`) is written to from its new end, and `LogFileMaxSize`
  counts from its new size; before, it was rotated much too soon.

### Performance

- `Log4JLogger` calls log4j through cached `MethodHandle`s instead of `Method.invoke`. A disabled
  `debug()` call (the most common kind) takes about half the time on Java 21 and two thirds on Java 11.
- `BjlLogger` writes a message and its stack trace to a log file in one write, instead of one write
  per line of the trace: logging an error with a stack trace takes about half the time.
- `SearchableClassLoader.findTarget` loaded every class it looked at, and they stayed loaded. It now
  reads the class file header (the class, super class and interfaces) and only loads classes that can
  match. Searching the log4j jars (about 1400 classes) for implementations of `Runnable` loaded 1683
  classes; it now loads 17 (115 to include indirect implementations). The results are the same.
  A class found twice is no longer checked with a linear search of the results.
- Looking up a property no longer takes a global lock: the properties file cache is a
  `ConcurrentHashMap` instead of an `LruMap`. A lookup takes about a third of the time with one
  thread, and much less than that when several threads look up properties at once.
- `DEFAULT_MAX_PROPERTIES` is 1000 (was 200), so an application with a few hundred `BaseObject`
  classes doesn't read the same properties files again and again. Over the limit, the files dropped
  from the cache are no longer the least recently used ones.
- `SearchableClassLoader` is registered as parallel capable, so threads loading different classes through
  it no longer wait for each other.

### BjlLogger log files

- New `LogFileMaxSize` and `LogFileCount` properties start a new log file when it reaches a size and
  keep a number of old ones (`app.log.1`, `app.log.2` ...). Off by default, so log files grow as before.
- A failure writing the log file (a full disk, say) was silently ignored and log entries were lost
  without any sign. It is now reported once on `System.err`, and again when writing works again.
- New `BjlLogger.closeLogFiles()` flushes and closes every log file, so it can be moved or deleted
  (Windows doesn't allow that while it is open). Logging can go on: the next entry opens the file again.

### Changed (may need a code change)

- `Clock` no longer overrides `paint(Graphics)` and `update(Graphics)`; it draws in `paintComponent`.
  A subclass that overrides `paint` and calls `super.paint` still gets the clock drawn.
  `createGraphics2D(int, int)` is deprecated and unused; it now returns a `Graphics2D` for a new image.

- Because `stop()` closes the server socket, a run method blocked in `accept()` now gets a
  `SocketException` when the server is stopped. Check `stopping` before treating it as an error.
- `SocketClient.getSocket()` gives up after 60 seconds by default (the `ConnectTimeout` property,
  in milliseconds; 0 means no limit).
- Secure `SocketClient` connections now check that the server's certificate was issued for the host
  being connected to (as HTTPS does). Before, any certificate the trust managers accepted was accepted
  for every host. This applies to sockets from `getSocketFactory()` as well as `getSocket()`. A server
  whose certificate doesn't match the name used to reach it (a test certificate, or connecting by IP
  address to a certificate without that address) now fails the TLS handshake; set the `VerifyHostname`
  property to false, or call `setVerifyHostname(false)`, for those.
- `AbstractCoreServer.getServerSocket()` throws a `SocketException` while the server thread is
  stopping and its socket has been closed. A run method should check `stopping` when it gets one.
- `JulLogger` logs debug messages at `FINE` instead of `FINEST`. A java.util.logging filter or
  configuration that picks out debug messages by the `FINEST` level needs to use `FINE`.
- Secure sockets from `SocketClient.getSocket()` have finished the TLS handshake, so TLS settings made on
  the returned socket (enabled protocols, cipher suites) no longer apply. Make them in an override of
  `configure(Socket)`, which runs before the handshake. Sockets from `getSocketFactory()` are unchanged.
- The `SecureBaseObject` setters are synchronized, so a setter called while another thread is in
  `getSSLContext()` (loading a key store, say) waits for it to finish.
- An `Error` thrown while `getSSLContext()` builds the context is no longer wrapped in an `IOException`.
- `MaxProperties` (`setMaxProperties`, default 1000) is now the most properties files cached for each
  class loader, not for all of them together. With one class loader nothing changes.

### Added

- `AbstractCoreServer.closeServerSocket()`.
- `SocketClient.getConnectTimeout()`/`setConnectTimeout(int)`, `PROPERTY_CONNECT_TIMEOUT` and
  `DEFAULT_CONNECT_TIMEOUT`.
- `SocketClient.isVerifyHostname()`/`setVerifyHostname(boolean)` and `PROPERTY_VERIFY_HOSTNAME`.
- `Log4JLogger.isLog4jProviderAvailable()`.
- `BaseThread` can run on virtual threads on Java 21+ (multi-release jar; see `BaseThread.VIRTUAL_THREADS_PROPERTY`).
- `BaseThread.isAlive()`.
- `KeepAlive` and `TcpNoDelay` properties (and `isKeepAlive()`/`setKeepAlive(boolean)`,
  `isTcpNoDelay()`/`setTcpNoDelay(boolean)`) in `AbstractCoreServer` and `SocketClient`, applied to
  sockets in `configure(Socket)`. Both are off by default.
- An opt-in connection limit for `AbstractCoreServer` accept loops: the `MaxConnections` property
  (`getMaxConnections()`/`setMaxConnections(int)`, default 0, no limit), `tryAcquireConnection()`,
  `releaseConnection()` and `getActiveConnections()`. The server doesn't call them itself; see the
  `tryAcquireConnection()` javadoc for how an accept loop uses them.
- The sub-components of `TimePanel`, `DatePanel`, `DayPanel` and `DateTimeCombo` have names
  (`Component.setName`), such as `hourSpinner`, `todayButton`, `btnBrowse` and `day1` to `day31`,
  so tests and GUI tools can find them.

### Deprecated

- `ThreadSafeDateFormat`: every call takes the same lock. Use `java.time.format.DateTimeFormatter`,
  which is thread safe without one. It will be removed in a future major version.

### Restored

- `BaseThread.setStopOnError`/`isStopOnError`, `setErrorSleepTime`/`getErrorSleepTime`,
  `DEFAULT_ERROR_SLEEP_TIME`, `SecureBaseObject.init()`, `SecureBaseObject.PROPERTY_FORCE_TLS_VERSION`,
  `BjlLogger.format` and `DateTimeCombo.setdate(Date)` were removed or renamed in 1.1.0 and are back
  (deprecated) because other BJL projects still use them (BJL-53).

### Documentation

- `BaseObject.findLogger(String)` and `LogHelper(String)`: loggers are kept for as long as the application
  runs, so logger names should come from a fixed set (class names), not from changing data such as a
  user or request id.

## 1.1.0

### Fixed

- `LruMap.setMaxSize(0)` (or a negative size) removed every entry. It now means "no limit",
  the same as the constructor.
- `LogHelper` did not find properties for the class or name it was created with: it looked them up
  under its own class name. `new LogHelper(Mailer.class).getProperty("SmtpHost")` now finds
  `com.example.Mailer.SmtpHost` and the `Mailer.properties` file, as if `Mailer` extended `BaseObject`.
- `Clock` printed "Bad hr" to `System.out`.

### Changed (may need a code change)

- `DateTimeCombo.setdate(Date)` is renamed `setDate(Date)`, to match `getDate()`.
- `AbstractCoreServer.PROPERTY_PORT` is now public, like the other property names.

### Removed (may need a code change)

- `BaseThread.setStopOnError`/`isStopOnError`, `setErrorSleepTime`/`getErrorSleepTime` and
  `DEFAULT_ERROR_SLEEP_TIME`. `BaseThread` never used them; a subclass that needs these settings
  should keep its own.
- The protected `init()` methods of `SecureBaseObject`, `SocketClient` and `AbstractCoreServer`.
  Nothing called them; the same properties are read the first time each value is needed.
- `SecureBaseObject.PROPERTY_FORCE_TLS_VERSION`, which was never used.
- The deprecated `BjlLogger.format`. `ThreadSafeDateFormat` itself is unchanged.
- The `main()` test drivers in `LruMap` and the Swing classes.

### Added

- `BaseObject.logWarn(Supplier<String>)` and `logError(Supplier<String>)`, like `logDebug` and `logInfo`:
  the message is only built when that level is enabled.
- `BaseObject.getPropertyClass()`: a subclass can choose which class's properties files are searched.
- The jar declares the module name `us.bringardner.core` (`Automatic-Module-Name`).
- License and SCM information in the POM.

### Other

- The `getProperty` Javadoc now describes the search that is actually done (the class, then its
  super classes), and the Javadoc builds without errors.
- `maven-deploy-plugin` updated to 3.1.4; the unused `site-maven-plugin` configuration was removed.
- Two `Clock` tests that were disabled for bugs that had already been fixed are enabled again.

## 1.0.0

First stable release. See the git history for the changes since 0.1.2.
