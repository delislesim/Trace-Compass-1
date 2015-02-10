/*******************************************************************************
 * Copyright (c) 2015 Ericsson
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
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.tracecompass.internal.dsf.core.service.DsfTraceModelService;
import org.eclipse.tracecompass.internal.dsf.core.service.DsfTraceModelService3;
import org.eclipse.tracecompass.internal.dsf.core.service.IDsfTraceModelService;
import org.eclipse.tracecompass.internal.dsf.core.service.TraceCommandControlService;
import org.eclipse.tracecompass.internal.dsf.core.service.TraceHardwareAndOSService;
import org.eclipse.tracecompass.internal.dsf.core.service.TraceProcessesService;
import org.eclipse.tracecompass.internal.dsf.core.service.TraceRunControlService;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/** */
public class DsfTraceSessionManager {

    //TODO: Temporary preference of prototype implementation, to choose a thread presentation option
    private enum Mode{RUNNING_THREAD,  NON_SLEEPING_THREADS, THREAD_GROUPS_BY_PROCESS, THREAD_GROUPS_BY_STATE}
    private static Mode mode = Mode.NON_SLEEPING_THREADS;

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
     * Remove the resources used with the trace being closed
     * @param signal -
     */
    @TmfSignalHandler
    public void traceClosed(TmfTraceClosedSignal signal) {
        endSession(signal.getTrace());
    }

    /**
     * @param trace - Remove the session associated to the given trace
     */
    public static void endSession(ITmfTrace trace) {
        DsfSession session =  fTraceToSessionMap.remove(trace);
        // Check if the session is still tracked / active
        if (session == null) {
            return;
        }

        // create a services tracker
        DsfServicesTracker tracker = new DsfServicesTracker(DsfTraceCorePlugin.getBundleContext(), session.getId());


        // remove the associated services
        TraceRunControlService runControlService = tracker.getService(TraceRunControlService.class);
        if (runControlService != null) {
            runControlService.shutdown(new RequestMonitor(session.getExecutor(), null));
        }

        TraceProcessesService processesService = tracker.getService(TraceProcessesService.class);
        if (processesService != null) {
            processesService.shutdown(new RequestMonitor(session.getExecutor(), null));
        }

        TraceHardwareAndOSService traceHWService = tracker.getService(TraceHardwareAndOSService.class);
        if (traceHWService != null) {
            traceHWService.shutdown(new RequestMonitor(session.getExecutor(), null));
        }

        TraceCommandControlService commandService = tracker.getService(TraceCommandControlService.class);
        if (commandService != null) {
            commandService.shutdown(new RequestMonitor(session.getExecutor(), null));
        }

        DsfTraceModelService modelService = tracker.getService(DsfTraceModelService.class);
        if (modelService != null) {
            modelService.shutdown(new RequestMonitor(session.getExecutor(), null));
        }

        DsfSession.endSession(session);
    }

    /**
     * Create a DSF session f
     * @param trace trace that needs to be associated to a new session
     * @return The DSF session created.
     */
    public static DsfSession startDsfSession(ITmfTrace trace) {
        // Check if a session has already been started for the given trace
        DsfSession session =  fTraceToSessionMap.get(trace);
        if (session != null) {
            return session;
        }

        // New session needed for this trace
        final DefaultDsfExecutor dsfExecutor = new DefaultDsfExecutor(TRACE_DEBUG_MODEL_ID);
        dsfExecutor.prestartCoreThread();
        session = DsfSession.startSession(dsfExecutor, TRACE_DEBUG_MODEL_ID);

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
                            getTraceModelService(session, trace).initialize(new ImmediateRequestMonitor(null) {
                                @Override
                                protected void handleSuccess() {
                                    try {
                                        new TraceHardwareAndOSService(session).initialize(new ImmediateRequestMonitor(null));
                                        new TraceProcessesService(session).initialize(new ImmediateRequestMonitor(null));
                                        new TraceRunControlService(session).initialize(new ImmediateRequestMonitor(null));
                                    } catch (CoreException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                }
                            });
                        } catch (CoreException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                });
            }
        };

        session.getExecutor().execute(task);
    }

    private static IDsfTraceModelService getTraceModelService(DsfSession session, ITmfTrace trace) throws CoreException {
        IDsfTraceModelService service = new DsfTraceModelService(session, trace);

        switch (mode) {
        case NON_SLEEPING_THREADS:
            service = new DsfTraceModelService3(session, trace);
            break;
        case THREAD_GROUPS_BY_PROCESS:
        	//TODO: 
        case THREAD_GROUPS_BY_STATE:
        	//TODO:
        case RUNNING_THREAD:
        	//TODO:
        default:
            service = new DsfTraceModelService(session, trace);
        }

        return service;
    }
}
