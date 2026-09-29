# BjlCore

The core of the **Bringardner Java Library (BJL)**: small, dependency-free building blocks that
the other BJL projects share. It gives you property lookup and logging for any class, threads
that can be stopped cleanly, a TCP/SSL server and client base, a few utilities, and a set of
Swing date and time pickers.

- **Java 11** or later
- **No runtime dependencies.** log4j 2 is used when it is on the class path, but it is never required.
- Apache License 2.0

## Contents

- [Getting started](#getting-started)
- [What's inside](#whats-inside)
- [Properties](#properties)
- [Logging](#logging)
- [Threads and servers](#threads-and-servers)
- [SSL](#ssl)
- [Swing components](#swing-components)
- [Building and testing](#building-and-testing)
- [Releasing](#releasing)
- [License](#license)

## Getting started

BjlCore is published to GitHub Packages:

```xml
<dependency>
    <groupId>us.bringardner</groupId>
    <artifactId>bjl_core</artifactId>
    <version>0.1.2</version>
</dependency>
```

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/tony-bringardner/BjlCore</url>
    </repository>
</repositories>
```

GitHub Packages needs a token even for public packages. Add a `<server>` with the id `github`
to `~/.m2/settings.xml` with your GitHub user name and a token that has the `read:packages` scope.

A class gets properties and logging by extending `BaseObject`:

```java
public class Mailer extends BaseObject {

    public void send(String to) {
        String host = getProperty("SmtpHost", "localhost");
        int port = getIntProperty("SmtpPort", 25);
        logDebug(() -> "Sending to " + to + " via " + host + ":" + port);
        ...
    }
}
```

## "What's inside"

| Package | Class | Purpose |
|---|---|---|
| `us.bringardner.core` | `BaseObject` | Property lookup and logging for any class. |
| | `SecureBaseObject` | Adds key store, trust manager and `SSLContext` handling. |
| | `BaseThread` | A `Runnable` that can be started, stopped and restarted. |
| | `ILogger` | The logging interface used everywhere in BJL. |
| | `BjlLogger`, `Log4JLogger`, `JulLogger` | `ILogger` implementations: built in, log4j 2, `java.util.logging`. |
| `us.bringardner.core.util` | `AbstractCoreServer` | Base class for a TCP or SSL server. |
| | `SocketClient` | Creates configured plain or SSL client sockets. |
| | `LruMap` | A `LinkedHashMap` that drops the least recently used entry at a size limit. |
| | `SearchableClassLoader` | Finds the direct (or all) sub classes / implementations of a type in jars and folders. |
| | `ThreadSafeDateFormat` | A synchronized `SimpleDateFormat`. |
| | `LogHelper` | Logging for code that can't extend `BaseObject`. |
| `us.bringardner.core.swing` | `DatePanel`, `DayPanel`, `TimePanel`, `Clock` | Date and time picker panels. |
| | `DateDialog`, `TimeDialog`, `DateAndTimeDialog` | Modal dialogs built from the panels. |
| | `DateTimeCombo` | A date/time spinner with a button that opens `DateAndTimeDialog`. |

## Properties

`getProperty(name)` looks in these places, in order, and returns the first value it finds:

1. The system property `<class name>.<name>`, e.g. `-Dcom.example.Mailer.SmtpHost=mail.example.com`
2. The system property `<name>`, e.g. `-DSmtpHost=mail.example.com`
3. A properties file on the class path named after the class, `/com/example/Mailer.properties`,
   checking `<class name>.<name>` and then `<name>`. The files of each super class are checked
   next, up to (not including) `BaseObject`. Inner classes use their outer class's file.
4. The default value passed to `getProperty(name, default)`, or `null`

So a property can be set for one class (`com.example.Mailer.SmtpHost`) or for everything (`SmtpHost`),
and a sub class inherits its parents' properties files.

- `getIntProperty(name, default)` and `getBooleanProperty(name, default)` convert the value.
  An invalid number prints a warning to `System.err` and returns the default. Only `true` (in any case) is true.
- `setSupportPrefixProperty(false)` turns off the `<class name>.` lookups for one object.
  `setPropertyPrefix(...)` (protected) uses a different prefix.
- Properties files are cached (up to 200 by default). Use `BaseObject.setMaxProperties(n)`
  to change the limit, and `BaseObject.clearPropertyCache()` to re-read them.

## Logging

Every `BaseObject` has `logDebug`, `logInfo`, `logWarn` and `logError` methods (each with a
`Throwable` variant), plus `isDebugEnabled()` and so on. The `Supplier` versions, such as
`logDebug(() -> "...")`, only build the message when that level is enabled.

The logger is chosen once, the first time one is needed:

1. The class named by the system property `ILogger`, if it implements `ILogger`
2. `Log4JLogger`, if log4j 2 (`log4j-api`) is on the class path
3. `BjlLogger` otherwise

Loggers are shared by name (the class name by default), so objects of the same class use one
logger. `BaseObject.findLogger(name)` returns the logger for any name, and
`setLogger(...)` replaces it for one object. Levels are `DEBUG`, `INFO`, `WARN`, `ERROR` and `NONE`.

### BjlLogger

The built-in logger writes `MM-dd-yyyy HH:mm:ss.SSS [thread] LEVEL name - message` lines.
It is configured with properties (see [Properties](#properties); the class name prefix is
`us.bringardner.core.BjlLogger`):

| Property | Meaning |
|---|---|
| `<logger name>.LogLevel` | The level for one logger, e.g. `-Dcom.example.Mailer.LogLevel=DEBUG` |
| `LogLevel` | The level for all loggers. The default is `ERROR`. |
| `LogFile` | A file to append to, or `System.out` (the default) or `System.err`. Loggers that name the same file share it. |

Level names from other frameworks also work: `OFF`, `FATAL`, `SEVERE`, `WARNING`, `TRACE`,
`ALL`, `FINE`, `FINER` and `FINEST`. An invalid level is reported and the default is used.

### log4j 2 and java.util.logging

`Log4JLogger` calls log4j through reflection, so BjlCore does not depend on it. Add
`log4j-api` and `log4j-core` to your own project and configure log4j as usual
(`resources/log4j2.xml` is a working example). Setting a level from code needs `log4j-core`.

`JulLogger` uses `java.util.logging`. It reads the file named by `java.util.logging.config.file`,
or `/JulLogging.properties` from the class path. This happens once, when the first
`JulLogger` is created. `resources/JulLogging.properties` is an example.

## Threads and servers

`BaseThread` wraps a `Thread` that you can start, stop and start again. Your `run()` sets the
`started` and `running` flags and returns when `stopping` is set:

```java
public class Poller extends BaseThread {
    @Override
    public void run() {
        started = running = true;
        while (!stopping) {
            poll();
        }
        running = false;
    }
}

Poller poller = new Poller();
poller.setName("poller");
poller.start();
...
poller.stop(5000, true);   // ask it to stop, interrupt it, wait up to 5 seconds
```

Name, priority, daemon, context class loader and uncaught exception handler can be set before
`start()`. Threads are daemons by default. A running thread's daemon flag can't change, so
`setDaemon` takes effect the next time the thread starts.

`AbstractCoreServer` is a `BaseThread` for servers. `getServerSocket()` creates a plain or SSL
server socket from these properties (or the matching setters):

| Property | Default | Meaning |
|---|---|---|
| `Port` | `9200` | Port to listen on (`0` = any free port) |
| `BindAddress` | all addresses | Address to listen on |
| `Backlog` | `0` (system default) | Connection queue length |
| `AcceptTimeout` | `60000` ms | `accept()` timeout, so the loop can check `stopping` |
| `SocketTimeout` | `60000` ms | Read timeout set by `configure(socket)` |
| `IsSoLinger` / `SoLinger` | `false` / `10` s | SO_LINGER set by `configure(socket)` |
| `secure` | `false` | Use SSL (see [SSL](#ssl)) |

```java
AbstractCoreServer server = new AbstractCoreServer(8080) {
    @Override
    public void run() {
        try (ServerSocket ss = getServerSocket()) {
            started = running = true;
            while (!stopping) {
                try (Socket socket = ss.accept()) {
                    configure(socket);
                    handle(socket);
                } catch (SocketTimeoutException e) {
                    // check stopping again
                }
            }
        } catch (IOException e) {
            logError("Server failed", e);
        }
        running = false;
    }
};
server.start();
```

`SocketClient` is the client side: `new SocketClient(useSSL).getSocket(host, port)` returns a
socket configured with the same `SocketTimeout`, `IsSoLinger` and `SoLinger` properties.

## SSL

`SecureBaseObject` (and so `BaseThread`, `AbstractCoreServer` and `SocketClient`) builds an
`SSLContext` from these properties, or the matching setters:

| Property | Meaning |
|---|---|
| `KeyStoreName` | A class path resource (e.g. `/keys/server.p12`) or a file |
| `KeyStorePassword` | Without it no key managers are created, which is fine for a client |
| `KeyStoreType` | e.g. `PKCS12`. The default is the JVM default type (`PKCS12` since Java 9). |
| `Algorithm` | Key manager algorithm, e.g. `SunX509`. The default is the JVM default (`KeyManagerFactory.getDefaultAlgorithm()`). |
| `Protocol` | `SSLContext` protocol. The default is `TLS`. |

Trust managers can be set per object with `setTrustManagers(...)` or for all new objects with
`SecureBaseObject.setDefaultTrustManagers(...)`. `makecert.sh` creates a self-signed key store
for testing.

## Swing components

The date and time components work together:

```java
Date picked = new DateAndTimeDialog().showDialog(new Date(), "Start time");

DateTimeCombo combo = new DateTimeCombo(new Date());   // spinner plus "^" button
panel.add(combo);
...
Date value = combo.getDate();
```

`DateDialog`, `TimeDialog` and `DateAndTimeDialog` are modal. `showDialog` returns the chosen
value, or the date you passed in if the user cancels (`isCanceled()` tells you which).
`TimePanel` shows 12 or 24 hour time (set the default with `-DMilitaryTime=true`), can hide the
analog `Clock`, and can show seconds and milliseconds. The clock's hands can be dragged.

## Building and testing

```bash
mvn verify
```

This compiles the library, runs the tests and writes a [JaCoCo](https://www.jacoco.org/)
coverage report to `target/site/jacoco/index.html`.

- The Swing tests that paint or open dialogs need a display. They are skipped automatically
  when there is none (for example on a CI server), and briefly open windows when there is.
- The SSL server test uses `keytool` (from the JDK) to create a test key store if needed.
- log4j is a test dependency, so the `Log4JLogger` tests run against the real library.

## Releasing

Set the new version in `pom.xml`, then run `./release.sh`. It checks for GitHub credentials,
runs `mvn clean deploy` to GitHub Packages, commits any changes, tags `v<version>` and pushes.
Use `./release.sh -n` for a dry run. See the comments at the top of the script for details.

## License

Copyright 1998-2026 Tony Bringardner. Licensed under the [Apache License, Version 2.0](LICENSE).
