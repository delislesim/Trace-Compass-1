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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.Messages;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizer;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.visualizer.ui.canvas.GraphicCanvas;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelAnalysis;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceSessionManager;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/** */
@SuppressWarnings("restriction")
public class TraceMulticoreVisualizer extends MulticoreVisualizer {
    // Timeout between updates in the build thread in ms
    private static final long BUILD_UPDATE_TIMEOUT = 500;
    // Map to track the polling threads for trace parsing completion
    private final Map<ITmfTrace, BuildThread> fBuildThreadMap = new HashMap<>();

    /**
     *
     */
    public TraceMulticoreVisualizer() {
        TmfSignalManager.register(this);
    }

    /** Returns non-localized unique name for this visualizer. */
    @Override
    public @NonNull String getName() {
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
     * @param signal -
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        ITmfTrace trace = signal.getTrace();
        if (trace == null) {
            return;
        }

        // Create a polling thread to know when the trace has been parsed
        BuildThread buildThread = new BuildThread(trace, getName());

        synchronized (fBuildThreadMap) {
            fBuildThreadMap.put(trace, buildThread);
        }

        buildThread.start();
    }

    /**
     * @param signal -
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        ITmfTrace trace = signal.getTrace();

        // Remove the polling thread from the tracking list
        BuildThread thread = null;
        synchronized (fBuildThreadMap) {
            thread = fBuildThreadMap.remove(trace);
        }

        if (thread != null) {
            thread.cancel();
        }
    }

    private class BuildThread extends Thread {
        private final @NonNull ITmfTrace fBuildTrace;
        private final @NonNull IProgressMonitor fMonitor;

        public BuildThread(final @NonNull ITmfTrace trace, final @NonNull String name) {
            super(name + " build"); //$NON-NLS-1$
            fBuildTrace = trace;
            fMonitor = new NullProgressMonitor();
        }

        @Override
        public void run() {
            buildEventList(fBuildTrace, fMonitor);
            // trace build is done, remove it from the tracking list
            synchronized (fBuildThreadMap) {
                fBuildThreadMap.remove(fBuildTrace);
            }
        }

        public void cancel() {
            fMonitor.setCanceled(true);
        }
    }

    private void buildEventList(@NonNull ITmfTrace trace, @NonNull IProgressMonitor monitor) {
        ITmfStateSystem ssq = TmfStateSystemAnalysisModule.getStateSystem(trace, KernelAnalysis.ID);
        if (ssq == null) {
            return;
        }

        boolean complete = false;

        // Poll until build completion of cancellation
        while (!complete) {
            if (monitor.isCanceled()) {
                return;
            }

            complete = ssq.waitUntilBuilt(BUILD_UPDATE_TIMEOUT);
            if (ssq.isCancelled()) {
                return;
            }
        }

        // The trace is now fully parsed
        update();
    }

    /**
     * Enable Meters and request the cpu load from the service
     * @param signal -
     */
    @TmfSignalHandler
    public void timeSelected(TmfTimeSynchSignal signal) {
        setLoadMetersEnabled(true);
        // Refresh the data model
        update();
    }

    /**
     * Enable Meters and request the cpu load from the service
     * @param signal -
     */
    @TmfSignalHandler
    public void timeRangeSelected(TmfRangeSynchSignal signal) {
        setLoadMetersEnabled(true);
        // Refresh the data model
        update();
    }

    /**
     * @param signal -
     */
    @TmfSignalHandler
    public void timeTraceSelected(TmfTraceSelectedSignal signal) {
        setLoadMetersEnabled(false);
    }

    @Override
    public void setLoadMetersEnabled(boolean enabled) {
        if (fDataModel != null) {
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

}
