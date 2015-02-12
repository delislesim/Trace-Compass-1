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

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.debug.service.IRunControl;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;


/**
 * Run Control Service proxy to the DSF Trace model
 *
 */
public class TraceRunControlService extends AbstractDsfTraceService implements IRunControl {

    /**
     * @param session - Session where an instance of this service will participate
     * @throws CoreException -
     */
    public TraceRunControlService(@NonNull DsfSession session) throws CoreException {
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
        register(new String[] { IRunControl.class.getName(),
                TraceRunControlService.class.getName() },
                new Hashtable<String, String>());

        requestMonitor.done();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#isSuspended(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext)
     */
    @Override
    public boolean isSuspended(IExecutionDMContext context) {
        return fModelService.isSuspended(context);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#getExecutionData(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void getExecutionData(IExecutionDMContext dmc, DataRequestMonitor<IExecutionDMData> rm) {
        rm.done(fModelService.getExecutionData(dmc));
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#getExecutionContexts(org.eclipse.cdt.dsf.debug.service.IRunControl.IContainerDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void getExecutionContexts(IContainerDMContext c, DataRequestMonitor<IExecutionDMContext[]> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#canResume(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void canResume(IExecutionDMContext context, DataRequestMonitor<Boolean> rm) {
        rm.done(false);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#canSuspend(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void canSuspend(IExecutionDMContext context, DataRequestMonitor<Boolean> rm) {
        rm.done(false);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#resume(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext, org.eclipse.cdt.dsf.concurrent.RequestMonitor)
     */
    @Override
    public void resume(IExecutionDMContext context, RequestMonitor rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#suspend(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext, org.eclipse.cdt.dsf.concurrent.RequestMonitor)
     */
    @Override
    public void suspend(IExecutionDMContext context, RequestMonitor rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#isStepping(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext)
     */
    @Override
    public boolean isStepping(IExecutionDMContext context) {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#canStep(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext, org.eclipse.cdt.dsf.debug.service.IRunControl.StepType, org.eclipse.cdt.dsf.concurrent.DataRequestMonitor)
     */
    @Override
    public void canStep(IExecutionDMContext context, StepType stepType, DataRequestMonitor<Boolean> rm) {
        rm.setData(false);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.dsf.debug.service.IRunControl#step(org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext, org.eclipse.cdt.dsf.debug.service.IRunControl.StepType, org.eclipse.cdt.dsf.concurrent.RequestMonitor)
     */
    @Override
    public void step(IExecutionDMContext context, StepType stepType, RequestMonitor rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }
}
