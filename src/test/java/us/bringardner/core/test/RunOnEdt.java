package us.bringardner.core.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Runs each test method, and the class's @BeforeEach and @AfterEach methods, on the Swing event
 * dispatch thread. Swing components must only be created and used on that thread; tests that used
 * them from the JUnit thread could fail now and then for no visible reason.
 * <p>
 * Use with {@code @ExtendWith(RunOnEdt.class)}. Assertion failures and exceptions are reported as if
 * the test had run on the JUnit thread. A modal dialog shown by a test runs its own event loop, so
 * Swing timers (see {@link SwingTestUtil#clickWhenShowing}) still fire while it is open.
 */
public class RunOnEdt implements InvocationInterceptor {

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		onEdt(invocation);
	}

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		onEdt(invocation);
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		onEdt(invocation);
	}

	private static void onEdt(Invocation<Void> invocation) throws Throwable {
		if( SwingUtilities.isEventDispatchThread() ) {
			invocation.proceed();
			return;
		}
		Throwable[] error = new Throwable[1];
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					invocation.proceed();
				} catch (Throwable e) {
					error[0] = e;
				}
			});
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
		if( error[0] != null ) {
			throw error[0];
		}
	}
}
