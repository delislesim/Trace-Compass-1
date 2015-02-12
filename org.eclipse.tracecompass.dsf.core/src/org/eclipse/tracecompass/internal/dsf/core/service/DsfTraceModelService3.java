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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICPUDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.IHardwareTargetDMContext;
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
     * List of threads associated to a core independent of the thread state
     */
    protected Map<GDBCoreDMC, List<TraceExecutionDMC>> fMapCoreToThreads = new HashMap<>();

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
    protected void reset() {
        super.reset();
        fMapCoreToThreads.clear();
    }


    @Override
    protected ICPUDMContext[] resolveCPUContexts(IHardwareTargetDMContext dmc) {
        ICPUDMContext[] cpus = super.resolveCPUContexts(dmc);

        //Resolve all threads on the target system besides running threads
        resolveTargetThreads();
        return cpus;
    }

    /**
     * Resolve all the threads running in the target
     */
    protected void resolveTargetThreads() {

        // Resolve the thread ids for the active threads
        Collection<Integer> activeThreadsId = new ArrayList<>();
        for (GDBCoreDMC core : fMapCoreToExecution.keySet()) {
            // Keep track of active threads
            TraceExecutionDMC threadDmc = fMapCoreToExecution.get(core);
            activeThreadsId.add(Integer.valueOf(threadDmc.getThreadId()));

            // Fill in local map with the active threads
            List<TraceExecutionDMC> threadsPerCore = fMapCoreToThreads.get(core);
            if (threadsPerCore == null) {
                threadsPerCore = new ArrayList<>();
                fMapCoreToThreads.put(core, threadsPerCore);
            }
            threadsPerCore.add(threadDmc);
        }

        // Resolve all known threads
        Collection<TraceThreadDMData> allThreads = getThreads();

        // Get Parent core context to associate to existing threads
        List<GDBCoreDMC> coresList = new ArrayList<>();
        coresList.addAll(fMapCoreToThreads.keySet());

        int numberOfCores = coresList.size();
        if (allThreads.size() < 1 || numberOfCores < 1) {
            return;
        }

      // Create a thread execution context with its proper core
        int threadsAdded = 0;
        for (TraceThreadDMData threadDMData : allThreads) {
            Integer threadId = Integer.valueOf(threadDMData.getId());
            // Add a new thread but skip active threads as they are already present
            if (!activeThreadsId.contains(threadId)) {
                // These threads are actually not executing in any core
                // Lets have a simple balanced distribution among the available cores
                GDBCoreDMC core = coresList.get(threadsAdded++ % numberOfCores);
                threadDMData.addCore(core.getId());

                // Add thread to this data structure
                fMapCoreToThreads.get(core).add(new TraceExecutionDMC(getSession(), core, threadDMData));
            }
        }
    }

    protected Collection<TraceThreadDMData> getThreads() {
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
                if (state > 1) {
                    TraceThreadDMData thread = new TraceThreadDMData(threadId, executableName, ppid, state);
                    System.out.println("********************* New thread added ******************");
                    System.out.println("Quark: " + quark + ", threadId: " + threadId + ", executableName: " + executableName + ", coreId: " + coreId + ", ppid: " + ppid + ", state: " + state);
                    threadsData.add(thread);
                }

            }

            return threadsData;
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        }

        return NonNullUtils.checkNotNull(Collections.EMPTY_SET);
    }

    @Override
    public IDMContext[] getProcessesBeingDebugged(IDMContext dmc) {
        Collection<IDMContext> execDmcs = new ArrayList<>();
        if (dmc instanceof TraceExecutionDMC) {
            // Returning a single execution context
            execDmcs.add(dmc);
        } else if (dmc instanceof GDBCoreDMC) {
            // Returning all thread execution contexts for a core
            execDmcs.addAll(fMapCoreToThreads.get(dmc));
        }

        return execDmcs.toArray(new TraceExecutionDMC[execDmcs.size()]);
    }

    @Override
    public boolean isSuspended(IExecutionDMContext context) {
        assert context instanceof TraceExecutionDMC;
        int threadId = ((TraceExecutionDMC) context).getThreadId();

        for (TraceExecutionDMC activeThread : fMapCoreToExecution.values()) {
            int  activeThreadId = activeThread.getThreadId();
            if (threadId == activeThreadId) {
                return false;
            }
        }

        return true;
    }



}
