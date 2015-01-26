/*******************************************************************************
 * Copyright (c) 2015 Ericsson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alvaro Sanchez-Leon (Ericsson) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.dsf.core.service;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IProcesses;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;


/**
 * Process Service proxy to the DSF Trace model
 *
 */
public class TraceProcessesService extends AbstractDsfTraceService implements IProcesses {


    /**
     * @param session - Session where an instance of this service will participate
     * @throws CoreException -
     */
    public TraceProcessesService(@NonNull DsfSession session) throws CoreException {
        super(session);
    }

    /**
     * This method initializes this service after our superclass's initialize()
     * method succeeds.
     *
     * @param requestMonitor
     *            The call-back object to notify when this service's
     *            initialization is done.
     */
    @Override
    protected void doInitialize(RequestMonitor requestMonitor) {
        // Register this service.
        register(new String[] { IProcesses.class.getName(),
                TraceProcessesService.class.getName() },
                new Hashtable<String, String>());

        requestMonitor.done();
    }


    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#getProcessesBeingDebugged(org.eclipse.cdt.dsf.datamodel.IDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void getProcessesBeingDebugged(IDMContext dmc, DataRequestMonitor<IDMContext[]> rm) {
        rm.done(fModelService.getProcessesBeingDebugged(dmc));
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#getExecutionData(org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void getExecutionData(IThreadDMContext dmc, DataRequestMonitor<IThreadDMData> rm) {
        rm.done(fModelService.getExecutionData(dmc));
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#getDebuggingContext(org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void getDebuggingContext(IThreadDMContext dmc, DataRequestMonitor<IDMContext> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#getRunningProcesses(org.eclipse.cdt.dsf.datamodel.IDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void getRunningProcesses(IDMContext dmc, DataRequestMonitor<IProcessDMContext[]> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#isDebuggerAttachSupported(org.eclipse.cdt.dsf.datamodel.IDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void isDebuggerAttachSupported(IDMContext dmc, DataRequestMonitor<Boolean> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#attachDebuggerToProcess(org.eclipse.cdt.dsf.debug.service.IProcesses.IProcessDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void attachDebuggerToProcess(IProcessDMContext procCtx, DataRequestMonitor<IDMContext> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#canDetachDebuggerFromProcess(org.eclipse.cdt.dsf.datamodel.IDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void canDetachDebuggerFromProcess(IDMContext dmc, DataRequestMonitor<Boolean> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#detachDebuggerFromProcess(org.eclipse.cdt.dsf.datamodel.IDMContext, org.eclipse.cdt.dsf.concurrent.RequestMonitor)
     */
    @Override
    public void detachDebuggerFromProcess(IDMContext dmc, RequestMonitor rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#isRunNewProcessSupported(org.eclipse.cdt.dsf.datamodel.IDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void isRunNewProcessSupported(IDMContext dmc, DataRequestMonitor<Boolean> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#runNewProcess(org.eclipse.cdt.dsf.datamodel.IDMContext, java.lang.String, java.util.Map, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void runNewProcess(IDMContext dmc, String file, Map<String, Object> attributes, DataRequestMonitor<IProcessDMContext> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#isDebugNewProcessSupported(org.eclipse.cdt.dsf.datamodel.IDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void isDebugNewProcessSupported(IDMContext dmc, DataRequestMonitor<Boolean> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#debugNewProcess(org.eclipse.cdt.dsf.datamodel.IDMContext, java.lang.String, java.util.Map, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void debugNewProcess(IDMContext dmc, String file, Map<String, Object> attributes, DataRequestMonitor<IDMContext> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#canTerminate(org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void canTerminate(IThreadDMContext thread, DataRequestMonitor<Boolean> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IProcesses#terminate(org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMContext, org.eclipse.cdt.dsf.concurrent.RequestMonitor)
     */
    @Override
    public void terminate(IThreadDMContext thread, RequestMonitor rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }
}
