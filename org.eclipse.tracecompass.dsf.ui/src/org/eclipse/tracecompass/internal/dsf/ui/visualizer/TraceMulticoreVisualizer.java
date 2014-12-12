/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc Khouzam (Ericsson) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.Messages;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizer;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.visualizer.ui.canvas.GraphicCanvas;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceSessionManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/** */
@SuppressWarnings("restriction")
public class TraceMulticoreVisualizer extends MulticoreVisualizer {

    /**
     *
     */
    public TraceMulticoreVisualizer() {
        TmfSignalManager.register(this);
    }

    /** Returns non-localized unique name for this visualizer. */
    @Override
    public String getName() {
        return "tracecompass"; //$NON-NLS-1$
    }

    /** Returns localized name to display for this visualizer. */
    @Override
    public String getDisplayName() {
        return "Trace Visualizer";
    }

    /** Returns localized tooltip text to display for this visualizer. */
    @Override
    public String getDescription() {
        return Messages.MulticoreVisualizer_tooltip;
    }

    @Override
    public void dispose() {
        super.dispose();
        TmfSignalManager.deregister(this);
    }

    @Override
    public GraphicCanvas createCanvas(Composite parent) {
        m_canvas = new TraceMulticoreVisualizerCanvas(parent);
        m_canvas.addSelectionChangedListener(this);
        return m_canvas;
    }

    @Override
    public boolean updateDebugContext() {
        // is the visualizer pinned? Then inhibit context change
        if (isPinned()) {
            return false;
        }

        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            DsfSession session = DsfTraceSessionManager.getSessionId(trace);
            if (session != null) {
                return setDebugSession(session.getId());
            }
        }

        return false;
    }

    @Override
    public int handlesSelection(ISelection selection) {
        int result = 0;

        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            result = 1;
        }
        else {
            result = 0;
        }

        updateDebugViewListener();

        return result;
    }

    /**
     * Enable Meters and request the cpu load from the service
     * @param signal -
     */
    @TmfSignalHandler
    public void timeSelected(TmfTimeSynchSignal signal) {
        setLoadMetersEnabled(true);
        updateLoads();
    }

    /**
     * Enable Meters and request the cpu load from the service
     * @param signal -
     */
    @TmfSignalHandler
    public void timeRangeSelected(TmfRangeSynchSignal signal) {
        setLoadMetersEnabled(true);
        updateLoads();
    }

    @Override
    public void setLoadMetersEnabled(boolean enabled) {
        if (m_loadMetersEnabled == enabled) {
            return;
        }
        m_loadMetersEnabled = enabled;
        // save load meter enablement in model
        fDataModel.setLoadMetersEnabled(m_loadMetersEnabled);
        disposeLoadMeterTimer();
        // No polling timers for Tracing
        // initializeLoadMeterTimer();
    }

}
