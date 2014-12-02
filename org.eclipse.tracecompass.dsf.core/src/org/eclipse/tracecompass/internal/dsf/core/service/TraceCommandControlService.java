/*******************************************************************************
 * Copyright (c) 2014 Ericsson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc Khouzam (Ericsson) - initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.dsf.core.service;

import java.util.Hashtable;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.AbstractDMContext;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService;
import org.eclipse.cdt.dsf.debug.service.command.ICommandListener;
import org.eclipse.cdt.dsf.debug.service.command.ICommandResult;
import org.eclipse.cdt.dsf.debug.service.command.ICommandToken;
import org.eclipse.cdt.dsf.debug.service.command.IEventListener;
import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;
import org.osgi.framework.BundleContext;

/**
 * TEMPORARY STUB
 * We should not need this service just to obtain the context.
 * The base multicore visualizer should use a different way to get
 * the root context
 */
public class TraceCommandControlService extends AbstractDsfService implements ICommandControlService {

    /** */
    private class TraceCommandControlDMContext extends AbstractDMContext
    implements ICommandControlDMContext {

        public TraceCommandControlDMContext(String sessionId) {
            super(sessionId, DMContexts.EMPTY_CONTEXTS_ARRAY);
        }

        @Override
        public boolean equals(Object obj) {
            return baseEquals(obj);
        }

        @Override
        public int hashCode() {
            return baseHashCode();
        }

        @Override
        public String toString() {
            return baseToString();
        }

        @Override
        public String getCommandControlId() {
            return "0"; //$NON-NLS-1$
        }
    }

    private ICommandControlDMContext fContext;

    /**
     * @param session The DSF session for this service.
     */
    public TraceCommandControlService(DsfSession session) {
        super(session);
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
     * This method initializes this service after our superclass's initialize()
     * method succeeds.
     *
     * @param requestMonitor
     *            The call-back object to notify when this service's
     *            initialization is done.
     */
    private void doInitialize(RequestMonitor requestMonitor) {
        fContext = new TraceCommandControlDMContext(getSession().getId());

        // Register this service.
        register(new String[] { ICommandControlService.class.getName() },
                 new Hashtable<String, String>());

        requestMonitor.done();
    }

    @Override
    public void shutdown(RequestMonitor requestMonitor) {
        unregister();
        super.shutdown(requestMonitor);
    }

    /**
     * @return The bundle context of the plug-in to which this service belongs.
     */
    @Override
    protected BundleContext getBundleContext() {
        return DsfTraceCorePlugin.getBundleContext();
    }

    @Override
    public ICommandControlDMContext getContext() {
        return fContext;
    }


    @Override
    public <V extends ICommandResult> ICommandToken queueCommand(ICommand<V> command, DataRequestMonitor<V> rm) {
        return null;
    }

    @Override
    public void removeCommand(ICommandToken token) {
    }

    @Override
    public void addCommandListener(ICommandListener listener) {
    }

    @Override
    public void removeCommandListener(ICommandListener listener) {
    }

    @Override
    public void addEventListener(IEventListener listener) {
    }

    @Override
    public void removeEventListener(IEventListener listener) {
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
