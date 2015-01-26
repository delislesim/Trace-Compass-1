/*******************************************************************************
 * Copyright (c) 2015 Ericsson.
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
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.datamodel.IDMData;
import org.eclipse.cdt.dsf.debug.service.ICachingService;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS2;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;

public class TraceHardwareAndOSService extends AbstractDsfTraceService implements IGDBHardwareAndOS2, ICachingService {
    /**
     * @param session
     *            The DSF session for this service.
     * @param trace
     *            The trace associated to this service
     * @throws CoreException
     */
    public TraceHardwareAndOSService(@NonNull DsfSession session) throws CoreException {
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
        register(new String[] { IGDBHardwareAndOS.class.getName(),
                IGDBHardwareAndOS2.class.getName(),
                TraceHardwareAndOSService.class.getName() },
                new Hashtable<String, String>());

        requestMonitor.done();
    }

    @Override
    public void getCPUs(final IHardwareTargetDMContext dmc, final DataRequestMonitor<ICPUDMContext[]> rm) {
        rm.done(fModelService.getCPUs(dmc));
    }

    @Override
    public void getCores(IDMContext dmc, final DataRequestMonitor<ICoreDMContext[]> rm) {
        rm.done(fModelService.getCores(dmc));
    }

    @Override
    public void getExecutionData(IDMContext dmc, DataRequestMonitor<IDMData> rm) {
        if (dmc instanceof ICoreDMContext) {
            rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Not done yet", null)); //$NON-NLS-1$
        } else if (dmc instanceof ICPUDMContext) {
            rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Not done yet", null)); //$NON-NLS-1$
        } else {
            rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Invalid DMC type", null)); //$NON-NLS-1$
        }
    }

    @Override
    public ICPUDMContext createCPUContext(IHardwareTargetDMContext targetDmc, String CPUId) {
        return fModelService.createCPUContext(targetDmc, CPUId);
    }

    @Override
    public void getLoadInfo(final IDMContext context, final DataRequestMonitor<ILoadInfo> rm) {
        try {
            rm.done(fModelService.getLoadInfo(context));
        } catch (CoreException e) {
            rm.done(e.getStatus());
        }
    }

    @Override
    public ICoreDMContext createCoreContext(ICPUDMContext cpuDmc, String coreId) {
        // FIXME: Not being used at the moment, but need to discuss and improve
        // the API as we need an additional element i.e. the coreNode as seen in
        // the private method below
        assert false;
        return null;
    }

    @Override
    public void flushCache(IDMContext context) {
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void getResourceClasses(IDMContext dmc,
            DataRequestMonitor<IResourceClass[]> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

    @Override
    public void getResourcesInformation(IDMContext dmc, String resourceClassId,
            DataRequestMonitor<IResourcesInformation> rm) {
        rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, NOT_SUPPORTED, "Operation not supported", null)); //$NON-NLS-1$
    }

}
