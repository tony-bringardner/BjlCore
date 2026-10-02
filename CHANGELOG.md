# Changelog

## 1.2.0 (unreleased)

### Fixed

- An `AbstractCoreServer` could not be started again after it stopped: `getServerSocket()` returned
  the cached socket even after it had been closed, so `accept()` failed. A closed socket is now replaced.
- `AbstractCoreServer.stop()` only set `stopping`, so the server kept running until `accept()` timed out
  (60 seconds by default). It now also closes the listening socket so `accept()` returns at once.
  Connections that were already accepted are not affected.
- `SocketClient.getSocket()` had no connect timeout, so an unreachable host blocked for as long as the
  operating system kept trying (about 75 seconds on macOS, minutes on Linux). It also left the socket
  open if `configure()` threw. The socket is now closed on any failure.

### Changed (may need a code change)

- Because `stop()` closes the server socket, a run method blocked in `accept()` now gets a
  `SocketException` when the server is stopped. Check `stopping` before treating it as an error.
- `SocketClient.getSocket()` gives up after 60 seconds by default (the `ConnectTimeout` property,
  in milliseconds; 0 means no limit).

### Added

- `AbstractCoreServer.closeServerSocket()`.
- `SocketClient.getConnectTimeout()`/`setConnectTimeout(int)`, `PROPERTY_CONNECT_TIMEOUT` and
  `DEFAULT_CONNECT_TIMEOUT`.
- `BaseThread` can run on virtual threads on Java 21+ (multi-release jar; see `BaseThread.VIRTUAL_THREADS_PROPERTY`).
- `BaseThread.isAlive()`.

### Restored

- `BaseThread.setStopOnError`/`isStopOnError`, `setErrorSleepTime`/`getErrorSleepTime`,
  `DEFAULT_ERROR_SLEEP_TIME`, `SecureBaseObject.init()`, `SecureBaseObject.PROPERTY_FORCE_TLS_VERSION`,
  `BjlLogger.format` and `DateTimeCombo.setdate(Date)` were removed or renamed in 1.1.0 and are back
  (deprecated) because other BJL projects still use them (BJL-53).

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
