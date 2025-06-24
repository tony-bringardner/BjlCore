/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.us/tony">Tony Bringardner</A>
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
 * ~version~000.01.09-V000.01.06-V000.01.02-V000.00.02-V000.00.01-V000.00.00-
 */
package us.bringardner.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import us.bringardner.core.BaseObject;
import us.bringardner.core.BaseThread;
import us.bringardner.core.BjlLogger;
import us.bringardner.core.ILogger;
import us.bringardner.core.JulLogger;
import us.bringardner.core.Log4JLogger;
import us.bringardner.core.SecureBaseObject;
import us.bringardner.core.ILogger.Level;
import us.bringardner.core.swing.DatePanel;
import us.bringardner.core.util.AbstractCoreServer;
import us.bringardner.core.util.LogHelper;
import us.bringardner.core.util.LruMap;
import us.bringardner.core.util.SearchableClassLoader;
import us.bringardner.core.util.SocketClient;
import us.bringardner.core.util.ThreadSafeDateFormat;



public class TestCore extends TestCoreBase {

	Map<Level,String> expecedLogging = new TreeMap<ILogger.Level, String>();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		/*
Generating 2,048 bit RSA key pair and self-signed certificate (SHA256withRSA) with a validity of 90 days
	for: CN=bringardner.us, OU=AA, O=BBB, L=Bringardner, ST=CCCC, C=DD

		 */
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {



		expecedLogging.put(Level.NONE, "Start level NONE\n"
				+ "isErrorEnabled false\n"
				+ "isDebugEnabled false\n"
				+ "isInfoEnabled false\n"
				+ "isWarnEnabled false\n"
				+ "End level NONE\n"
				+ "");
		//enum Level  {DEBUG,WARN,INFO,ERROR,NONE};
		expecedLogging.put(Level.ERROR, "Start level ERROR\n"
				+ "isErrorEnabled true\n"
				+ "isDebugEnabled false\n"
				+ "isInfoEnabled false\n"
				+ "isWarnEnabled false\n"
				+ "11-01-2023 10:27:54.940 [main] ERROR us.bringardner.core.Log4JLogger - Test error no exception\n"
				+ "11-01-2023 10:27:54.940 [main] ERROR us.bringardner.core.Log4JLogger - Test error with exception\n"
				+ "java.io.IOException: Test error with exception\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:471) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:491) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "End level ERROR\n"
				+ "\n"
				+ "");
		expecedLogging.put(Level.INFO, "Start level INFO\n"
				+ "isErrorEnabled true\n"
				+ "isDebugEnabled false\n"
				+ "isInfoEnabled true\n"
				+ "isWarnEnabled true\n"
				+ "11-01-2023 10:27:54.940 [main] INFO  us.bringardner.core.Log4JLogger - Logging info no error\n"
				+ "11-01-2023 10:27:54.940 [main] INFO  us.bringardner.core.Log4JLogger - Logging info with error\n"
				+ "java.io.IOException: Test info log with error\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:467) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:492) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "11-01-2023 10:27:54.940 [main] WARN  us.bringardner.core.Log4JLogger - Logging warn no error\n"
				+ "11-01-2023 10:27:54.940 [main] WARN  us.bringardner.core.Log4JLogger - Logging warn with error\n"
				+ "java.io.IOException: Test warn log with error\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:469) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:492) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "11-01-2023 10:27:54.940 [main] ERROR us.bringardner.core.Log4JLogger - Test error no exception\n"
				+ "11-01-2023 10:27:54.940 [main] ERROR us.bringardner.core.Log4JLogger - Test error with exception\n"
				+ "java.io.IOException: Test error with exception\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:471) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:492) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "End level INFO\n"
				+ "\n"
				+ "");
		expecedLogging.put(Level.WARN, "Start level WARN\n"
				+ "isErrorEnabled true\n"
				+ "isDebugEnabled false\n"
				+ "isInfoEnabled false\n"
				+ "isWarnEnabled true\n"
				+ "11-01-2023 10:27:54.940 [main] WARN  us.bringardner.core.Log4JLogger - Logging warn no error\n"
				+ "11-01-2023 10:27:54.940 [main] WARN  us.bringardner.core.Log4JLogger - Logging warn with error\n"
				+ "java.io.IOException: Test warn log with error\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:469) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:493) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "11-01-2023 10:27:54.940 [main] ERROR us.bringardner.core.Log4JLogger - Test error no exception\n"
				+ "11-01-2023 10:27:54.940 [main] ERROR us.bringardner.core.Log4JLogger - Test error with exception\n"
				+ "java.io.IOException: Test error with exception\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:471) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:493) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "End level WARN\n"
				+ "\n"
				+ "");
		expecedLogging.put(Level.DEBUG, "Start level DEBUG\n"
				+ "isErrorEnabled true\n"
				+ "isDebugEnabled true\n"
				+ "isInfoEnabled true\n"
				+ "isWarnEnabled true\n"
				+ "11-01-2023 10:27:54.940 [main] DEBUG us.bringardner.core.Log4JLogger - Logging debug no error\n"
				+ "11-01-2023 10:27:54.940 [main] DEBUG us.bringardner.core.Log4JLogger - Logging debug with error\n"
				+ "java.io.IOException: Test debug log with error\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:465) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:494) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "11-01-2023 10:27:54.940 [main] INFO  us.bringardner.core.Log4JLogger - Logging info no error\n"
				+ "11-01-2023 10:27:54.940 [main] INFO  us.bringardner.core.Log4JLogger - Logging info with error\n"
				+ "java.io.IOException: Test info log with error\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:467) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:494) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "11-01-2023 10:27:54.940 [main] WARN  us.bringardner.core.Log4JLogger - Logging warn no error\n"
				+ "11-01-2023 10:27:54.940 [main] WARN  us.bringardner.core.Log4JLogger - Logging warn with error\n"
				+ "java.io.IOException: Test warn log with error\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:469) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:494) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "11-01-2023 10:27:54.940 [main] ERROR us.bringardner.core.Log4JLogger - Test error no exception\n"
				+ "11-01-2023 10:27:54.940 [main] ERROR us.bringardner.core.Log4JLogger - Test error with exception\n"
				+ "java.io.IOException: Test error with exception\n"
				+ "	at us.bringardner.core.test.TestCore.runTests(TestCore.java:471) [test-classes/:?]\n"
				+ "	at us.bringardner.core.test.TestCore.testLog4JLogger(TestCore.java:494) [test-classes/:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:?]\n"
				+ "	at jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[?:?]\n"
				+ "	at jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:?]\n"
				+ "	at java.lang.reflect.Method.invoke(Method.java:568) ~[?:?]\n"
				+ "	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.junit.runners.ParentRunner.run(ParentRunner.java:413) [org.junit_4.13.2.v20230809-1000.jar:4.13.2]\n"
				+ "	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:530) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:758) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:453) [.cp/:?]\n"
				+ "	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:211) [.cp/:?]\n"
				+ "End level DEBUG\n"
				+ "\n"
				+ "");



	}

	@After
	public void tearDown() throws Exception {

	}

	/**
	 * Run through testing all functions. The result will depend on the current level
	 * @param test
	 * @param level 
	 * @return
	 */
	private String runTests(ILogger test, Level level) {

		PrintStream out = System.out;		
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		System.setOut(new PrintStream(bo));

		/*
		PrintStream err = System.err;
		ByteArrayOutputStream be = new ByteArrayOutputStream();
		System.setErr(new PrintStream(be));
		 */

		test.setLevel(level);
		assertEquals(test.getLevel(), level);

		System.out.println("Start level "+test.getLevel());
		System.out.println("isErrorEnabled "+test.isErrorEnabled());
		System.out.println("isDebugEnabled "+test.isDebugEnabled());

		System.out.println("isInfoEnabled "+test.isInfoEnabled());
		System.out.println("isWarnEnabled "+test.isWarnEnabled());

		test.debug("Logging debug no error");
		test.debug("Logging debug with error",new IOException("Test debug log with error"));
		test.info("Logging info no error");
		test.info("Logging info with error",new IOException("Test info log with error"));
		test.warn("Logging warn no error");
		test.warn("Logging warn with error",new IOException("Test warn log with error"));		
		test.error("Test error no exception");
		test.error(
				"Test error with exception",
				new IOException("Test error with exception")
				);

		System.out.println("End level "+test.getLevel());		
		System.err.flush();		
		System.out.flush();

		System.setOut(out);
		//System.setErr(err);
		String outStr = new String(bo.toByteArray());
		//String errStr = new String(be.toByteArray());
		String ret = outStr;//+errStr;
		String expected = expecedLogging.get(level);
		compare(expected,ret);
		return ret;

	}



	private void compare(String expected, String actual) {
		// "11-01-2023 10:27:54.940 [main] 11/01/2024 07:52 ERROR us.bringardner.core.Log4JLogger - Test error no exception";
		// "11-01-2023 10:27:54.940 [main] 11/01/2024 07:52 SEVER us.bringardner.core.Log4JLogger - Test error no exception";
		String [] elines = clean(expected);
		String [] alines = clean(actual);
		Pattern reDate = Pattern.compile("([0-9]{2})[-]([0-9]{2})[-]([0-9]{4})");
		Pattern reTime = Pattern.compile("([0-9]{1,2})[:]([0-9]{2})[:]([0-9]{2})[.]([0-9]{3})");

		assertTrue("Number of lines are not good=",elines.length>=2 && alines.length>=2);

		for (int lidx = 0; lidx < elines.length; lidx++) {
			String e [] = elines[lidx].split("[ ]");
			String a [] = alines[lidx].split("[ ]");

			assertEquals("Length of results don't match on line "+lidx,e.length, a.length);

			for (int idx = 0; idx < a.length; idx++) {
				String one = a[idx];
				String two = e[idx];
				if(!levelMatches(one, two) ) {
					if( !e[idx].equals(a[idx])) {
						Matcher em = reDate.matcher(e[idx]);
						Matcher am = reDate.matcher(a[idx]);

						if( !(em.matches() && am.matches())) {
							em = reTime.matcher(e[idx]);
							am = reTime.matcher(a[idx]);

							if( !(em.matches() && am.matches())) {
								if(!(one.startsWith("us.bringardner.core.") && two.startsWith("us.bringardner.core."))) {
									assertTrue("Field "+idx+" on line "+lidx+" don't match and are not date or time w="+e[idx]+" a="+a[idx],false);
								}
							}
						}
					}
				}
			}	
		}
	}

	public boolean levelMatches(String two,String one) {
		boolean ret = false;
		if( one.equals("NONE") && two.equals("OFF")) {
			ret = true;
		} else if( one.equals("DEBUG") && two.equals("FINEST")) {
			ret = true;
		} else if( one.equals("ERROR") && two.equals("SEVERE")) {
			ret = true;
		} else if( one.equals("INFO") && two.equals("INFO")) {
			ret = true;
		} else if( one.equals("WARN") && two.equals("WARNING")) {
			ret = true;
		} else if( one.equals("NONE") && two.equals("OFF")) {
			ret = true;
		} else if( one.equals("NONE") && two.equals("OFF")) {
			ret = true;
		}
		return ret;
	}

	private String[] clean(String expected) {
		List<String> ret = new ArrayList<String>();
		int idx = expected.indexOf("  ");
		while( idx >=0) {
			expected = expected.replaceAll("  ", " ");
			idx = expected.indexOf("  ");
		}
		String[] lines = expected.split("\n");
		for(String line : lines) {
			line = line.trim();
			if(!line.isEmpty() && !line.startsWith("at ")) {
				ret.add(line);
			}
		}

		return ret.toArray(new String[ret.size()]);
	}

	@Test
	public void testLog4JLogger() {
		Log4JLogger test = new Log4JLogger();

		test.init(getClass().getName());
		runTests(test,ILogger.Level.NONE);
		runTests(test,ILogger.Level.ERROR);
		runTests(test,ILogger.Level.INFO);
		runTests(test,ILogger.Level.WARN);
		runTests(test,ILogger.Level.DEBUG);




	}

	@Test
	public void testClassLoader() throws IOException {
		Class<?>[] expected3 = {
				TestCoreBase.class,BaseObject.class,BjlLogger.class,SecureBaseObject.class,LogHelper.class
		};
		Class<?>[] expected2 = {
				SecureBaseObject.class,BjlLogger.class,BaseObject.class,LogHelper.class,TestCoreBase.class
		};

		Class<?>[] expected1 = {
				BaseObject.class,BjlLogger.class,SecureBaseObject.class,LogHelper.class
		};


		File file2 = new File("TestFiles/TestSearchableClassLoader.jar").getCanonicalFile();

		SearchableClassLoader l2 = SearchableClassLoader.getLoader(Arrays.asList(file2.getAbsolutePath()));
		List<Class<?>> list2 = l2.findTarget(BaseObject.class);
		assertEquals("jar flile List sizes do not match",expected2.length,list2.size());
		for (int idx = 0; idx < expected2.length; idx++) {
			assertEquals("jar file Class does not match", list2.get(idx), expected2[idx]);
		}

		File file = new File("TestFiles/us").getCanonicalFile();		
		SearchableClassLoader l1 = SearchableClassLoader.getLoader(Arrays.asList(file.getAbsolutePath()));
		List<Class<?>> list1 = l1.findTarget(BaseObject.class);
		assertEquals("directory List sizes do not match",list1.size(),expected1.length);
		for (int idx = 0; idx < expected1.length; idx++) {
			assertEquals("directory Class does not match", list1.get(idx), expected1[idx]);
		}



		SearchableClassLoader l = SearchableClassLoader.getClassPathLoader();
		List<Class<?>> list = l.findTarget(BaseObject.class);
		assertTrue("class path List sizes do not match",list.size()==expected3.length);
		for (int idx = 0; idx < expected3.length; idx++) {
			assertEquals("class path Class does not match", list.get(idx), expected3[idx]);
		}

	}

	@Test
	public void testBjlLogger() {
		BjlLogger test = new BjlLogger();
		test.init("us.bringardner.core.Log4JLogger");

		runTests(test,ILogger.Level.NONE);
		runTests(test,ILogger.Level.ERROR);
		runTests(test,ILogger.Level.INFO);
		runTests(test,ILogger.Level.WARN);
		runTests(test,ILogger.Level.DEBUG);

	}

	@Test
	public void testJulLogger() {
		//The java.util.logging package is not real done well in my opinion.
		System.setProperty("java.util.logging.config.file", "/Volumes/Data/eclipse-workspace-jmail/BjlCore/resources/JulLogging.properties");

		JulLogger test = new JulLogger();
		test.init("us.bringardner.core.Log4JLogger");

		Logger jul = test.getLogger();
		Handler h = new StreamHandler(System.out,new SimpleFormatter() ) {
			@Override
			public synchronized void publish(LogRecord record) {
				String msg = getFormatter().
						format(record);
				System.out.print(msg);
			}
		};
		h.setLevel(java.util.logging.Level.SEVERE);
		jul.addHandler(h);
		jul.setUseParentHandlers(false);

		runTests(test,ILogger.Level.ERROR);
		runTests(test,ILogger.Level.NONE);		
		runTests(test,ILogger.Level.INFO);
		runTests(test,ILogger.Level.WARN);
		runTests(test,ILogger.Level.DEBUG);
	}

	@Test
	public void testProperty01() {
		String [] expected = {
				"1", //BaseClass property logic finds it in us.bringardner.core.test.TestCore.properties
				"1",//BaseClass property logic finds it in us.bringardner.core.test.TestCore.properties
				"2",//BaseClass property logic overrides with com.TestCoreBase.properties
				"1",//BaseClass property logic finds it in us.bringardner.core.test.TestCore.properties
				"System", //BaseClass property logic overrides with System.properties
				"Default6" // No property exists use default
		};
		System.setProperty("Value05", "System");

		for(int idx=1; idx <= 6; idx++) {
			assertEquals("idx="+idx, expected[idx-1],getProperty("Value0"+idx,"Default"+idx));
		}
	}

	@Test
	public void testBaseThread() {

		BaseThread thread = new BaseThread() {

			@Override
			public void run() {
				started = running = true;
				while( !stopping ) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
				}
				running = false;
			}
		};
		thread.start();
		int timeout = 1000;
		long startTime = System.currentTimeMillis();
		while( System.currentTimeMillis()-startTime<= timeout && !thread.hasStarted()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}	
		}
		assertTrue("Thread is not running",thread.isRunning());

		thread.stop();		
		startTime = System.currentTimeMillis();
		while( System.currentTimeMillis()-startTime<= timeout && thread.isRunning()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}	
		}

		assertTrue("Thread did not stop",!thread.isRunning());


	}

	@Test
	public void testAbstractServer01() throws IOException {
		// test insecure server
		runAbstractServer01(false);
		//  test secure server
		runAbstractServer01(true);

	}

	private void runAbstractServer01(boolean useSSL) throws IOException {
		if( useSSL) {
			String ksname= getProperty("KeyStoreName");
			File file = new File(ksname);
			file = file.getCanonicalFile();
			if( !file.exists()) {
				createCert();
			}
		}

		class Tm implements X509TrustManager {

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				throw new CertificateException("This object is not intended for clinet processing.");					
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				//normally the trust manager will throw an exception here if it does not like the certificate.
				//  for testing just accept anything.					

			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

		};
		TrustManager[] mgr = {
				new Tm(),
		};

		int port = 7778;
		AbstractCoreServer svr = new AbstractCoreServer(port,useSSL) {
			Socket socket = null;
			InputStream in = null;
			OutputStream out = null;
			ServerSocket ss=null;

			@Override
			public void run() {
				try {
					ss = getServerSocket();
					//  wait until ServerSocket is created before claiming to have started
					started = running = true;
					while(!stopping) {
						try {
							socket = ss.accept();
							if( socket != null ) {
								in = socket.getInputStream();
								out = socket.getOutputStream();
								int i = in.read();
								while(i >=0 ) {
									out.write(i);
									i = in.read();
								}
								try {in.close();} catch (Exception e) {	}
								try {out.close();} catch (Exception e) {	}							
								try {socket.close();} catch (Exception e) {	}
								socket=null;
							}
						} catch (SocketTimeoutException e) {
						}
					}
				} catch (Throwable e) {					
					e.printStackTrace();
				}
				if( socket != null ) {
					try {in.close();} catch (Exception e) {	}
					try {out.close();} catch (Exception e) {	}							
					try {socket.close();} catch (Exception e) {}
				}
				try {ss.close();} catch (Exception e) {					}
				running = false;
			}
		};

		svr.setAcceptTimeout(100);
		if( useSSL) {
			svr.setTrustManagers(mgr);
		}
		svr.start();
		int timeout = 60000;
		long startTime = System.currentTimeMillis();
		while( System.currentTimeMillis()-startTime<= timeout && !svr.hasStarted()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}	
		}
		assertTrue("Server is not running",svr.isRunning());
		String clientMEssage = "Client is here\n";
		try {
			SocketClient client = new SocketClient(useSSL);
			client.setTrustManagers(mgr);
			SocketFactory factory = client.getSocketFactory();

			Socket socket = factory.createSocket("localhost", port);
			InputStream in = socket.getInputStream();
			OutputStream out = socket.getOutputStream();
			out.write(clientMEssage.getBytes());
			byte [] data = new byte[clientMEssage.length()];
			int got = in.read(data);
			while(got < data.length) {
				got += in.read(data, got, data.length-got);
			}

			String response = new String(data);
			assertEquals("Server response is not what we expected",clientMEssage, response);
			try {in.close();} catch (Exception e) {	}
			try {out.close();} catch (Exception e) {	}							
			try {socket.close();} catch (Exception e) {}
			svr.stop();
			startTime = System.currentTimeMillis();
			while( System.currentTimeMillis()-startTime<= timeout && svr.isRunning()) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}	
			}
			assertTrue("Server is did not stop",!svr.isRunning());
		} catch (Throwable e) {
			e.printStackTrace();
			assertTrue("Could not process client code. e="+e,true);			
		}
		if( useSSL) {
			String ksname= getProperty("KeyStoreName");
			File file = new File(ksname);
			file = file.getCanonicalFile();
			if( file.exists()) {
				file.delete();
			}
		}
	}

	@Test
	public void testLruMap() {

		int maxSize = 10;

		// Create the map
		LruMap<String, String> map = new LruMap<>(maxSize);
		// file the map with one more value than it has room for
		for(int idx = 0,sz=map.getMaxSize()+1; idx <sz; idx++  ) {
			map.put("Key"+idx, "Value"+idx);
		}

		assertEquals("Map is the wrong size", maxSize, map.size());
		//  Now the map should have eliminate the first entry Key0...
		assertNull("Key0 was not removed from lru map", map.get("Key0"));
		// this makes Key3 the MRU entry
		assertNotNull("Key3 is not in the lru map", map.get("Key3"));
		// and another entry will cause "Key1" to be removed
		map.put("Key11", "Value11");
		assertEquals("Map is the wrong size", maxSize, map.size());
		//  Now the map should have eliminate the first entry Key0...
		assertNull("Key1 was not removed from lru map", map.get("Key1"));
	}

	@Test
	public void testThreadSafeDateFormat() {
		class DateFormatThread extends BaseThread {
			private Date date;
			private ThreadSafeDateFormat fmt;
			DateFormatThread (Date date,ThreadSafeDateFormat fmt){
				this.date = date;
				this.fmt = fmt;
			}

			List<String> results = new ArrayList<String>();
			@Override
			public void run() {
				started = running = true;
				while(!stopping) {
					//  use the fmt object as fast as possible
					results.add(fmt.format(date));
				}				
				running = false;
			}
		}
		//  I really can't understand why SimpleDateFormat is NOT tread safe
		ThreadSafeDateFormat fmt = new ThreadSafeDateFormat("MM-dd-yyyy HH:mm:ss.SSS");
		final Date date = new Date();
		final String expect = fmt.format(date);		
		DateFormatThread[] threads = new DateFormatThread[10];

		for(int idx = 0; idx < 10; idx++) {
			threads[idx] = new DateFormatThread(date, fmt);
			threads[idx].start();
		}

		try {
			//  Let them run and compete with each other for 2 seconds
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		// Stop them all
		for(DateFormatThread thread : threads) {
			thread.stop();			
		}

		try {
			//  give them time to stop
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		// check the results
		for(DateFormatThread thread : threads) {
			for(String val : thread.results) {
				assertEquals("Formatted date is not correct", val,expect);
			}
		}
	}




	public void createCert() throws IOException {

		//excuteCommand("./makecert.sh");
		ProcessBuilder pb =new ProcessBuilder("keytool"
				, "-genkey"
				, "-noprompt"
				, "-alias"
				, "serverkey"
				, "-dname"
				, "CN=bringardner.us, OU=AA, O=BBB, L=Bringardner, S=CCCC, C=DD"
				, "-keystore"
				, "serverkeystore.p12"
				, "-storepass"
				, "peekab00"
				, "-keypass"
				, "peekab00"
				, "-keyalg"
				, "RSA"
				, "-keysize"
				, "2048"
				, "-sigalg"
				, "SHA256withRSA"
				);

		Process p = pb.start();
		try {
			int i = p.waitFor();
			System.out.println("i="+i);
		} catch (InterruptedException e) {
			throw new IOException(e);
		}

	}


	public static void excuteCommand(String filePath) throws IOException{
		boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0;
		Process p = null;
		if(isWindows){
			p = Runtime.getRuntime().exec("cmd /c start " + filePath);	        
		}else {
			p = Runtime.getRuntime().exec(new String[] {filePath}, null);
			//p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-c", filePath}, null);
		}
		try {
			p.waitFor(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		int exit = p.exitValue();
		assertEquals("Invalid exit code",0,exit);
		String expect = "Generating 2,048 bit RSA key pair and self-signed certificate (SHA256withRSA) with a validity of 90 days\n"
				+ "	for: CN=bringardner.us, OU=AA, O=BBB, L=Bringardner, ST=CCCC, C=DD";
		String tmp =  readAllBytes(p.getInputStream())+ readAllBytes(p.getErrorStream());
		assertEquals("Invalid response text ",expect.trim(),tmp.trim());

	}

	private static String readAllBytes(InputStream in) throws IOException {
		byte[] ret = new byte[in.available()];
		int got = in.read(ret);
		while( got >=0 && got < ret.length) {
			int tmp = in.read(ret, got, ret.length-got);
			if( tmp < 0 ) {
				throw new IOException("Unexpected EOF");
			}
		}
		return new String(ret);
	}

	@Test 
	public void testDayPanel () throws Exception {
		final SimpleDateFormat fmt = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss.SSS");
		final Calendar startDate = Calendar.getInstance();

		startDate.setTime( fmt.parse("02-15-2000 13:42:20.333"));
		startDate.setTimeZone(TimeZone.getTimeZone("EST"));

		DatePanel panel = new DatePanel(startDate.getTime());
		Date date2 = panel.getDate();
		assertEquals("", startDate.getTime(), date2);

	}

}
