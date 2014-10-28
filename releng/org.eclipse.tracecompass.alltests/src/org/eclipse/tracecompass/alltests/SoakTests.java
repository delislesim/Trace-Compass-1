package org.eclipse.tracecompass.alltests;

import org.eclipse.tracecompass.alltests.swtbot.RunAllSWTBotTests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;

/**
 * Test suite that runs everything many times.
 */
public class SoakTests extends TestSuite {
    public static TestSuite suite() {
        TestSuite s = new TestSuite();
        for (int i = 0; i < 100; i++) {
            s.addTest(new JUnit4TestAdapter(RunAllCoreTests.class));
            s.addTest(new JUnit4TestAdapter(RunAllSWTBotTests.class));
            s.addTest(new JUnit4TestAdapter(SWTLeakTest.class));
        }
        return s;
    }
}