package org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.IntResult;
import org.eclipse.swtbot.swt.finder.results.StringResult;

public abstract class LeakRunner {

    public static boolean PRINT_NATIVE_OBJECT_COUNT = true;
    public static final int NUM_ITER = 2;
    private int fObjectCount = 0;

    public void start() {
        for (int i = 0; i < NUM_ITER; i++) {
            run();

            int oldObjectCount = fObjectCount;
            fObjectCount = getNativeObjectCount();
            if (PRINT_NATIVE_OBJECT_COUNT) {
                System.out.println("Diff object count: " + (fObjectCount - oldObjectCount) + " (total:" + fObjectCount + ")");
            }

            // Only consider the total after the first time to consider images
            // using the image registry, etc.
            if (oldObjectCount == 0) {
                oldObjectCount = fObjectCount;
            }

            String lastNativeObjectStack = getLastNativeObjectStack();
            assertEquals("Leaked object count: " + (fObjectCount - oldObjectCount) + ", last stack: " + lastNativeObjectStack, oldObjectCount, fObjectCount);
        }
    }

    private static int getNativeObjectCount() {
        return UIThreadRunnable.syncExec(new IntResult() {
            @Override
            public Integer run() {
                Display display = Display.getDefault();
                DeviceData info = display.getDeviceData();
                if (!info.tracking) {
                    fail("Warning: Device is not tracking resource allocation. Make sure you are running with Sleak tracing options, see http://www.eclipse.org/swt/tools.php");
                }
                return info.objects.length;
            }
        });
    }

    private static String getLastNativeObjectStack() {
        return UIThreadRunnable.syncExec(new StringResult() {
            @Override
            public String run() {
                Display display = Display.getDefault();
                DeviceData info = display.getDeviceData();
                if (!info.tracking) {
                    fail("Warning: Device is not tracking resource allocation. Make sure you are running with Sleak tracing options, see http://www.eclipse.org/swt/tools.php");
                }

                String stackMessage = null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                info.errors[info.objects.length - 1].printStackTrace(ps);
                ps.flush();
                try {
                    baos.flush();
                    stackMessage = baos.toString();
                } catch (IOException e) {
                }

                return stackMessage;
            }
        });
    }

    protected abstract void run();
}