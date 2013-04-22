package org.testng.eclipse.pde.runtime;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;

/**
 * The {@link ITestHarness} implementation used to execute Platform UI tests.
 */
public class PlatformUITestHarness implements ITestHarness {

	private TestableObject fTestableObject;
	private final boolean fRunTestsInSeparateThread;

	/**
	 * Creates a new instance.
	 * @param testableObject the testable object
	 */
	public PlatformUITestHarness(Object testableObject, boolean runTestsInSeparateThread) {
		this.fRunTestsInSeparateThread = runTestsInSeparateThread;
		fTestableObject = (TestableObject) testableObject;
		fTestableObject.setTestHarness(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.testing.ITestHarness#runTests()
	 */
	public void runTests() {
		try {
			// signal starting
			fTestableObject.testingStarting();

			// the test runner runnable
			Runnable testsRunner = new Runnable() {
				public void run() {
					RemotePluginTestRunner.main(stripTestNGPluginArgs(Platform.getCommandLineArgs()));
				}
			};

			if (fRunTestsInSeparateThread) {
				// wrap into separate thread and run from there
				final Thread testRunnerThread = new Thread(testsRunner, "Plug-in Tests Runner"); //$NON-NLS-1$
				fTestableObject.runTest(new Runnable() {
					public void run() {
						testRunnerThread.start();
					}
				});

				// wait for tests to finish
				// note, this has do be done outside #runTest method to not lock the UI
				try {
					testRunnerThread.join();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			} else {
				// run directly
				fTestableObject.runTest(testsRunner);
			}
		} finally {
			// signal shutdown
			fTestableObject.testingFinished();
		}
	}

	protected String[] stripTestNGPluginArgs(String[] applicationArgs) {
		List<String> argList = new LinkedList<String>(Arrays.asList(applicationArgs));
		for (Iterator<String> iter = argList.iterator(); iter.hasNext();) {
			String arg = iter.next();
			if (arg.equals("-testApplication")) { //$NON-NLS-1$
				iter.remove();
				if(iter.hasNext()) {
					iter.next();
					iter.remove();
				}
			}
		}
		return argList.toArray(new String[argList.size()]);
	}

}
