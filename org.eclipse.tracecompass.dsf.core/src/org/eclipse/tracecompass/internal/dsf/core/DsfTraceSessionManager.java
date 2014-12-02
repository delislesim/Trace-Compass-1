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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DefaultDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.ImmediateRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Query;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.tracecompass.internal.dsf.core.service.TraceCommandControlService;
import org.eclipse.tracecompass.internal.dsf.core.service.TraceHardwareAndOSService;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/** */
public class DsfTraceSessionManager {

    /** */
    public final static String TRACE_DEBUG_MODEL_ID = "org.eclipse.tracecompass.dsf"; //$NON-NLS-1$


    /**
     * Create a DSF session f
     * @return The DSF session created.
     */
    public static DsfSession startDsfSession() {
        final DefaultDsfExecutor dsfExecutor = new DefaultDsfExecutor(TRACE_DEBUG_MODEL_ID);
        dsfExecutor.prestartCoreThread();
        DsfSession session = DsfSession.startSession(dsfExecutor, TRACE_DEBUG_MODEL_ID);

        startServices(session);

        return session;
    }

    /**
     * @param trace a
     * @return a
     */
    public static DsfSession getSessionId(ITmfTrace trace) {
        //TODO
        DsfSession[] sessions = DsfSession.getActiveSessions();
        if (sessions != null && sessions.length > 0) {
            return sessions[0];
        }
        return null;
    }

    private static void startServices(final DsfSession session) {
        Query<Object> query = new Query<Object>() {
            @Override
            protected void execute(final DataRequestMonitor<Object> rm) {
                new TraceCommandControlService(session).initialize(new ImmediateRequestMonitor(rm) {
                    @Override
                    protected void handleSuccess() {
                        new TraceHardwareAndOSService(session).initialize(new ImmediateRequestMonitor(rm));
                    }
                });
            }
        };

        session.getExecutor().execute(query);
        try {
            query.get();
        } catch (InterruptedException e1) {
        } catch (ExecutionException e1) {
        } catch (CancellationException e1) {
        } finally {
        }
    }
}
