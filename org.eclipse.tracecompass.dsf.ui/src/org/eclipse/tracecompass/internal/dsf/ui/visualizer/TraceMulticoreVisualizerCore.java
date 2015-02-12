/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * Contributors:
 *     Marc Dumais - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerCPU;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerCore;
import org.eclipse.cdt.visualizer.ui.util.Colors;
import org.eclipse.swt.graphics.Color;

@SuppressWarnings("restriction")
public class TraceMulticoreVisualizerCore extends MulticoreVisualizerCore {

    public TraceMulticoreVisualizerCore(MulticoreVisualizerCPU cpu, int id) {
        super(cpu, id);
    }

    /** Returns core color for current state. */
    @Override
    protected Color getCoreStateColor(boolean foreground) {
        Color color;

        if (foreground) {
            color = Colors.BLACK;
        }
        else {
            color = Colors.BLACK;
        }
        return color;
    }

}
