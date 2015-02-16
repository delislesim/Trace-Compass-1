/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * Contributors:
 *     Marc Dumais - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.IMulticoreVisualizerConstants;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerCore;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerThread;
import org.eclipse.cdt.visualizer.ui.util.Colors;
import org.eclipse.cdt.visualizer.ui.util.GUIUtils;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;



@SuppressWarnings("restriction")
public class TraceMulticoreVisualizerThread extends MulticoreVisualizerThread {

    private static final String PROCESS_REGEXP = ".*Executable:\\s*(\\S*)\\s.*"; //$NON-NLS-1$

    private ColorChoser m_colorChoser;
    private final Pattern m_processPattern;
    private String m_highlight;

    public TraceMulticoreVisualizerThread(ColorChoser colorChoser, String highlight, MulticoreVisualizerCore core, TraceVisualizerModelThread thread) {
        super(core, thread);
        m_colorChoser = colorChoser;
        m_highlight = highlight;
        m_processPattern = Pattern.compile(PROCESS_REGEXP, Pattern.DOTALL);
    }

    /** Gets thread state. */
    public int getKernelState() {
        assert m_thread instanceof TraceVisualizerModelThread;

        TraceVisualizerModelThread thread;
        thread = (TraceVisualizerModelThread) m_thread;
        return thread.getKernelState();
    }

    /** Gets thread state. */
    public String getKernelStateLabel() {
        assert m_thread instanceof TraceVisualizerModelThread;

        TraceVisualizerModelThread thread;
        thread = (TraceVisualizerModelThread) m_thread;
        return thread.getKernelStateLabel();
    }

    @Override
    protected Color getThreadStateColor() {
        Color defaultColor = Colors.WHITE;

        // Depending on which "highlight mode" is on, return
        // color for current thread
        if (m_highlight != null) {
            switch (m_highlight) {
            case "Kernel State": //$NON-NLS-1$
                return getColorKernelStateHighlight();
            case "Process Name": //$NON-NLS-1$
                return getColorProcessHighlight();
            case "None" : //$NON-NLS-1$
                return defaultColor;
            default:
                return defaultColor;
                // return getThreadStateColorKernelStateHighlight();
            }
        }
        return defaultColor;
    }

    /** Gets thread color based on current state. */
    protected Color getColorKernelStateHighlight() {
        switch (getKernelState()) {
        case 0:
            // UNKNOWN
            return Colors.getColor(100,100,100);
        case 1:
            // WAIT_BLOCKED
            return Colors.getColor(200,200,0);
        case 2:
            // USERMODE
            return Colors.getColor(0,200,0);
        case 3:
            // SYSCALL
            return Colors.getColor(0,0,200);
        case 4:
            // INTERRUPTED
            return Colors.getColor(200,0,100);
        case 5:
            // WAIT_FOR_CPU
            return Colors.getColor(200,100,0);
        default:
            return Colors.getColor(0,0,0);
        }
    }

//    @Override
    protected Color getColorProcessHighlight() {
        String exec = getExecutable();
        return m_colorChoser.getColor(exec);
    }

    /** Returns the executable name, for the current thread, or "unknown" is unable to find it */
    private String getExecutable() {
        String exec = null;

        Matcher matcher = m_processPattern.matcher(m_thread.getLocationInfo());
        if (matcher.matches()) {
            exec = matcher.group(1);
        }
        else {
            exec = "unknown"; //$NON-NLS-1$
        }

        return exec;
    }

 // --- paint methods ---

    /** Invoked to allow element to paint itself on the viewer canvas */
    @Override
    public void paintContent(GC gc) {
        if (m_core.getWidth() >= MIN_PARENT_WIDTH) {
            gc.setBackground(getThreadStateColor());

            int x = m_bounds.x;
            int y = m_bounds.y;
            int w = THREAD_SPOT_SIZE;
            int h = THREAD_SPOT_SIZE;

            gc.fillOval(x, y, w, h);

            // special case: for the "process" thread, draw an enclosing circle
            if (m_thread.isProcessThread()) {
                // Subtract one from the width and height
                // in the case of drawOval because that method
                // adds a pixel to each value for some reason
                gc.setForeground(IMulticoreVisualizerConstants.COLOR_PROCESS_THREAD);
                gc.drawOval(x,y,w-1,h-1);
            }

            // draw text annotations
            gc.setBackground(IMulticoreVisualizerConstants.COLOR_THREAD_TEXT_BG);
            gc.setForeground(IMulticoreVisualizerConstants.COLOR_THREAD_TEXT_FG);

            // draw TID
            String displayTID = Integer.toString(m_thread.getTID());
            displayTID += " - " + getExecutable() + " - (" + getKernelStateLabel() + ")";
            GUIUtils.drawText(gc, displayTID, x + w + 4, y + 2);

            // draw selection marker, if any
            if (m_selected)
            {
                gc.setForeground(IMulticoreVisualizerConstants.COLOR_SELECTED);
                gc.drawOval(x-2,y-2,w+3,h+3);
                gc.drawOval(x-3,y-3,w+5,h+5);
            }
        }
    }

}
