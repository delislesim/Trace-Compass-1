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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICPUDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICoreDMContext;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.Attributes;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelThreadInformationProvider;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;


/** */
public class DsfTraceModelService3 extends DsfTraceModelService {

    /**
     * Used to interface with utility class KernelThreadInformationProvider
     */
    final protected KernelAnalysis fKernelModule;

    /**
     * @param session -
     * @param trace -
     * @throws CoreException -
     */
    public DsfTraceModelService3(@NonNull DsfSession session, @NonNull ITmfTrace trace) throws CoreException {
        super(session, trace);

        fKernelModule = TmfTraceUtils.getAnalysisModuleOfClass(fTrace, KernelAnalysis.class, KernelAnalysis.ID);
        if (fKernelModule == null) {
            // Notify of incorrect initialization
            throw new CoreException(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Unable to resolve the Kernel Analysis module from trace: " + trace, null)); //$NON-NLS-1$
        }

        fKernelModule.schedule();
        fKernelModule.waitForInitialization();
    }

    @Override
    protected ICoreDMContext createCoreContext(ICPUDMContext cpuDmc, Integer coreNode, String coreId) {
        getThreads();

        return super.createCoreContext(cpuDmc, coreNode, coreId);
    }

    public Collection<TraceThreadDMData> getThreads() {
        ITmfStateSystem ss = TmfStateSystemAnalysisModule.getStateSystem(fTrace, KernelAnalysis.ID);

        if (ss == null) {
            return NonNullUtils.checkNotNull(Collections.EMPTY_SET);
        }

        List<TraceThreadDMData> threadsData = new ArrayList<>();

        int threadsQuark;
        try {
            threadsQuark = ss.getQuarkAbsolute(Attributes.THREADS);
            for (Integer quark : ss.getSubAttributes(threadsQuark, false)) {
                // Resolve the threaid
                int threadId = Integer.parseInt(ss.getAttributeName(quark));
                // Resolve the executable name
                String executableName = KernelThreadInformationProvider.getExecutableName(fKernelModule, threadId);
                // TODO: Provide core id for the executable thread only
                String coreId = "";
                // Resolve the ppid
                Integer ppid = getIntegerThreadAttribute(threadId, Attributes.PPID);

                // Resolve the state
                Integer oState = getIntegerThreadAttribute(threadId, Attributes.STATUS);
                int state = oState == null ? 0 : oState.intValue();

                // Build the thread data instance
                TraceThreadDMData thread = new TraceThreadDMData(threadId, executableName, ppid, state);
                if (state > 1) {
                    System.out.println("********************* New thread added ******************");
                    System.out.println("Quark: " + quark + ", threadId: " + threadId + ", executableName: " + executableName + ", coreId: " + coreId + ", ppid: " + ppid + ", state: " + state);
                }

                threadsData.add(thread);
            }

            return threadsData;
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        }

        return NonNullUtils.checkNotNull(Collections.EMPTY_SET);
    }
}
