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

import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;
import org.osgi.framework.BundleContext;


/**
 * Common and abstract methods to all DSF Trace Services
 *
 */
public abstract class AbstractDsfTraceService extends AbstractDsfService {
    final protected IDsfTraceModelService fModelService;

    /**
     * @param session - Debug session
     * @throws CoreException - Exception notification at the construction of this instance
     */
    public AbstractDsfTraceService(@NonNull DsfSession session) throws CoreException {
        super(session);
        DsfServicesTracker tracker = new DsfServicesTracker(DsfTraceCorePlugin.getBundleContext(), session.getId());
        fModelService = tracker.getService(IDsfTraceModelService.class);
        tracker.dispose();
        if (fModelService == null) {
            throw new CoreException(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, REQUEST_FAILED, "Unable to resolve the Trace Model Service", null)); //$NON-NLS-1$
        }
    }

    /**
     * This method initializes this service.
     *
     * @param requestMonitor
     *            The request monitor indicating the operation is finished
     */
    @Override
    public void initialize(final RequestMonitor requestMonitor) {
        super.initialize(new RequestMonitor(ImmediateExecutor.getInstance(), requestMonitor) {
            @Override
            protected void handleSuccess() {
                doInitialize(requestMonitor);
            }
        });
    }

    /**
     * Service initialization e.g. Registration to a debug session
     * @param requestMonitor - asynchronous call back request monitor
     */
    protected abstract void doInitialize(RequestMonitor requestMonitor);


    @Override
    public void shutdown(RequestMonitor requestMonitor) {
        unregister();
        super.shutdown(requestMonitor);
    }

    @Override
    protected BundleContext getBundleContext() {
        return DsfTraceCorePlugin.getBundleContext();
    }

}
