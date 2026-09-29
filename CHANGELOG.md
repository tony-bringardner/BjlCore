# Changelog

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
