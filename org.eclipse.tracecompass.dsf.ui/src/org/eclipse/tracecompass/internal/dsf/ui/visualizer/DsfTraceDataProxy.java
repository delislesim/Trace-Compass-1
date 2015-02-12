/*******************************************************************************
 * Copyright (c) 2015 Ericsson AB and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alvaro Sanchez-Leon (Ericsson) - Bug 459114 - override construction of the data model
 *******************************************************************************/

package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import org.eclipse.cdt.dsf.concurrent.ConfinedToDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateDataRequestMonitor;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IProcesses;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.utils.DSFDebugModel;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.utils.DSFSessionState;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICPUDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICoreDMContext;

/**
 */
public class DsfTraceDataProxy extends DSFDebugModel {
    @Override
    @ConfinedToDsfExecutor("null")
    public void getThreads(DSFSessionState sessionState,
            final ICPUDMContext cpuContext,
            final ICoreDMContext coreContext,
            final DataRequestMonitor<IDMContext[]> rm)
    {
        final IProcesses procService = sessionState.getService(IProcesses.class);
        if (procService == null) {
            rm.done(new IDMContext[0]);
            return;
        }

        procService.getProcessesBeingDebugged(coreContext,
                new ImmediateDataRequestMonitor<IDMContext[]>() {
                    @Override
                    protected void handleCompleted() {
                        IDMContext[] processContexts = getData();

                        if (!isSuccess() || processContexts == null || processContexts.length < 1) {
                            // Unable to get any process data for this core
                            // Is this an issue? A core may have no processes/threads, right?
                            rm.done(new IDMContext[0]);
                            return;
                        }

                        rm.done(processContexts);
                    }
                });
    }

}
