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

package org.eclipse.tracecompass.internal.dsf.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.DefaultDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.ImmediateRequestMonitor;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tracecompass.internal.dsf.core.service.TraceCommandControlService;
import org.eclipse.tracecompass.internal.dsf.core.service.TraceHardwareAndOSService;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/** */
public class DsfTraceSessionManager {

    /** */
    public final static String TRACE_DEBUG_MODEL_ID = "org.eclipse.tracecompass.dsf"; //$NON-NLS-1$
    private final static Map<ITmfTrace, DsfSession> fTraceToSessionMap = new HashMap<>();

    /**
     *
     */
    public DsfTraceSessionManager() {
        TmfSignalManager.register(this);
    }

    /**
     *
     */
    public void dispose() {
        TmfSignalManager.deregister(this);
    }

    /**
     * @param signal -
     */
    @TmfSignalHandler
    public void traceOpened(TmfTraceOpenedSignal signal) {
        startDsfSession(signal.getTrace());
    }

    /**
     * Create a DSF session f
     * @return The DSF session created.
     */
    public static DsfSession startDsfSession(ITmfTrace trace) {
        final DefaultDsfExecutor dsfExecutor = new DefaultDsfExecutor(TRACE_DEBUG_MODEL_ID);
        dsfExecutor.prestartCoreThread();
        DsfSession session = DsfSession.startSession(dsfExecutor, TRACE_DEBUG_MODEL_ID);

        startServices(session, trace);
        fTraceToSessionMap.put(trace, session);

        return session;
    }

    /**
     * @param trace a
     * @return a
     */
    public static DsfSession getSessionId(ITmfTrace trace) {
        return fTraceToSessionMap.get(trace);
    }

    private static void startServices(final DsfSession session, final ITmfTrace trace) {
         Runnable task = new Runnable() {

            @Override
            public void run() {
                new TraceCommandControlService(session).initialize(new ImmediateRequestMonitor(null) {
                    @Override
                    protected void handleSuccess() {
                        try {
                            new TraceHardwareAndOSService(session, trace).initialize(new ImmediateRequestMonitor(null));
                        } catch (CoreException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                });
            }
        };

        session.getExecutor().execute(task);
    }
}
