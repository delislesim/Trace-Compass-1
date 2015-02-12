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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.Immutable;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.AbstractDMContext;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMContext;
import org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMData;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IContainerDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMData;
import org.eclipse.cdt.dsf.debug.service.IRunControl.StateChangeReason;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICPUDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICoreDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.IHardwareTargetDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS2.ILoadInfo;
import org.eclipse.cdt.dsf.gdb.service.IGDBProcesses.IGdbThreadDMData;
import org.eclipse.cdt.dsf.mi.service.IMIExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIProcessDMContext;
import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.cpuusage.KernelCpuUsageAnalysis;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.Attributes;
import org.eclipse.tracecompass.analysis.os.linux.core.kernelanalysis.KernelAnalysis;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateValueTypeException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.tracecompass.tmf.core.signal.TmfRangeSynchSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTimeSynchSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.osgi.framework.BundleContext;

/** */
public class DsfTraceModelService extends AbstractDsfService implements IDsfTraceModelService {

    final protected ITmfTrace fTrace;
    final protected KernelCpuUsageAnalysis fCPUModule;

    final protected ITmfStateSystem fStateSys;
    final protected Map<ICPUDMContext, GDBCoreDMC[]> fMapCPUToCores = new HashMap<>();
    protected long fStartTime;
    protected long fEndTime;
    protected ICommandControlDMContext fCommandControlContext;
    protected Map<GDBCoreDMC, TraceExecutionDMC> fMapCoreToExecution = new HashMap<>();
    protected boolean fTraceActive = true;

    @Immutable
    protected class GDBCPUDMC extends AbstractDMContext implements ICPUDMContext
    {
        /**
         * String ID that is used to identify the thread in the GDB/MI protocol.
         */
        private final String fId;

        /**
         * @param sessionId
         *            The session
         * @param targetDmc
         *            The target
         * @param id
         *            the CPU id
         */
        protected GDBCPUDMC(String sessionId, IHardwareTargetDMContext targetDmc, String id) {
            super(sessionId, targetDmc == null ? new IDMContext[] { fCommandControlContext } : new IDMContext[] { fCommandControlContext, targetDmc });
            fId = id;
        }

        @Override
        public String getId() {
            return fId;
        }

        @Override
        public String toString() {
            return baseToString() + ".CPU[" + fId + "]";} //$NON-NLS-1$ //$NON-NLS-2$

        @Override
        public boolean equals(Object obj) {
            return baseEquals(obj) && ((GDBCPUDMC) obj).fId.equals(fId);
        }

        @Override
        public int hashCode() {
            return baseHashCode() ^ fId.hashCode();
        }
    }

    @Immutable
    protected static class GDBCoreDMC extends AbstractDMContext
            implements ICoreDMContext
    {
        /**
         * E.g. The name given to the State system node representing a cpu core
         */
        private final String fId;
        /**
         * The node in the Tmftrace State system,
         */
        private final Integer fNode;

        public GDBCoreDMC(String sessionId, ICPUDMContext CPUDmc, Integer coreNode, String id) {
            super(sessionId, CPUDmc == null ? new IDMContext[0] : new IDMContext[] { CPUDmc });
            fId = id;
            fNode = coreNode;
        }

        @Override
        public String getId() {
            return fId;
        }

        // The State system node is the key to access any additional
        // information, however the value does not represent the actual core id
        public Integer getNode() {
            return fNode;
        }

        @Override
        public String toString() {
            return baseToString() + ".core[" + fId + "]";} //$NON-NLS-1$ //$NON-NLS-2$

        @Override
        public boolean equals(Object obj) {
            return baseEquals(obj) &&
                    (((GDBCoreDMC) obj).fId == null ? fId == null : ((GDBCoreDMC) obj).fId.equals(fId));
        }

        @Override
        public int hashCode() {
            return baseHashCode() ^ (fId == null ? 0 : fId.hashCode());
        }
    }

    protected static class TraceExecutionDMC extends AbstractDMContext implements IContainerDMContext, IMIProcessDMContext, IMIExecutionDMContext {
        TraceThreadDMData fThreadData;

        public TraceExecutionDMC(DsfSession session, GDBCoreDMC parent, TraceThreadDMData threadData) {
            super(session, new IDMContext[] { parent });
            fThreadData = threadData;
        }

        public IThreadDMData getExecutionData() {
            return fThreadData;
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
        public int getThreadId() {
            return Integer.valueOf(fThreadData.getId());
        }

        @Override
        public String getProcId() {
            Integer pid = fThreadData.getPpid();
            pid = (pid == null ? Integer.valueOf(getThreadId()) : pid);
            return String.valueOf(pid.intValue());
        }
    }

    @Immutable
    protected class GDBLoadInfo implements ILoadInfo {
        private int fILoad;
        private String fLoad;
        private Map<String, String> fDetailedLoad;

        public GDBLoadInfo(String load, Map<String, String> detailedLoad) {
            fLoad = load;
            fILoad = Integer.valueOf(load);
            fDetailedLoad = detailedLoad;
        }

        public GDBLoadInfo(int load) {
            this(String.valueOf(load), null);
            fILoad = load;
        }

        public int getILoad() {
            return fILoad;
        }

        @Override
        public String getLoad() {
            return fLoad;
        }

        @Override
        public Map<String, String> getDetailedLoad() {
            return fDetailedLoad;
        }
    }

    @Immutable
    public static class TraceThreadDMData implements IGdbThreadDMData {
        private int fThreadId;
        private String fExecutableName;
        private final List<String> fCoreId = new ArrayList<>();
        private Integer fPpid;
        private int fState;

        public TraceThreadDMData(int threadId, String executableName, Integer ppid, int state) {
            fThreadId = threadId;
            fExecutableName = executableName;
            fPpid = ppid;
            fState = state;
        }

        @Override
        public String getName() {
            return fExecutableName;
        }

        @Override
        public String getId() {
            return String.valueOf(fThreadId);
        }

        @Override
        public boolean isDebuggerAttached() {
            return false;
        }

        @Override
        public String[] getCores() {
            return fCoreId.toArray(new String[fCoreId.size()]);
        }

        public void addCore(@NonNull String coreId) {
            fCoreId.add(coreId);
        }

        public void removeCore(@NonNull String coreId) {
            fCoreId.remove(coreId);
        }

        @Override
        public String getOwner() {
            // TODO resolve owner
            return "Owner ?"; //$NON-NLS-1$
        }

        public Integer getPpid() {
            return fPpid;
        }

        public int getState() {
            return fState;
        }

    }

    /**
     * @param session
     *            The DSF session for this service.
     * @param trace
     *            The trace associated to this service
     * @throws CoreException
     *             -
     */
    public DsfTraceModelService(@NonNull DsfSession session, @NonNull ITmfTrace trace) throws CoreException {
        super(session);

        // Use VIP to make sure the time selection is first updated in this
        // service i.e. before the UI queries for the load
        TmfSignalManager.registerVIP(this);

        fTrace = trace;

        // initialize cpu data source
        fCPUModule = TmfTraceUtils.getAnalysisModuleOfClass(fTrace, KernelCpuUsageAnalysis.class, KernelCpuUsageAnalysis.ID);
        if (fCPUModule == null) {
            // Notify of incorrect initialization
            throw new CoreException(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Unable to resolve the CPU Usage Analysis module from trace: " + trace, null)); //$NON-NLS-1$
        }

        fCPUModule.schedule();
        fCPUModule.waitForInitialization();
        fStateSys = fCPUModule.getStateSystem();
        if (fStateSys == null) {
            // Notify of incorrect initialization
            throw new CoreException(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Unable to resolve the State System from trace: " + trace, null)); //$NON-NLS-1$
        }

        fStartTime = fStateSys.getStartTime();
        fEndTime = fStateSys.getCurrentEndTime();

        // Resolve the root context from the command control service
        DsfServicesTracker tracker = new DsfServicesTracker(DsfTraceCorePlugin.getBundleContext(), session.getId());
        ICommandControlService controlService = tracker.getService(ICommandControlService.class);
        tracker.dispose();
        if (controlService == null || controlService.getContext() == null) {
            throw new CoreException(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, REQUEST_FAILED, "Unable to resolve the ICommandControlService: " + trace, null)); //$NON-NLS-1$
        }

        fCommandControlContext = controlService.getContext();
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
    protected void doInitialize(RequestMonitor requestMonitor) {
        // Register this service.
        register(new String[] { IDsfTraceModelService.class.getName(),
                DsfTraceModelService.class.getName() },
                new Hashtable<String, String>());

        requestMonitor.done();
    }

    @Override
    public void shutdown(RequestMonitor requestMonitor) {
        TmfSignalManager.deregister(this);
        unregister();
        reset();
        super.shutdown(requestMonitor);
    }

    /**
     * Resolve the time interval to use for the calculation of load
     *
     * @param signal
     *            -
     */
    @TmfSignalHandler
    public void timeSelected(TmfTimeSynchSignal signal) {
        if (!fTraceActive) {
            return;
        }

        // Broadcasted in nano seconds
        long beginTime = signal.getBeginTime().getValue();
        long endTime = signal.getEndTime().getValue();
        fEndTime = endTime;
        if (beginTime == endTime) {
            // calculate load period over previous 1/2000 of the trace's time
            // range
            long delta = (fStateSys.getCurrentEndTime() - fStateSys.getStartTime()) / 2000;
            fStartTime = fEndTime - delta;
        } else {
            fStartTime = beginTime;
        }

        assert fEndTime > fStartTime;
        System.out.println("Current end time: " + fStateSys.getCurrentEndTime());
        System.out.println("Time Selected: " + fStartTime + "->" + fEndTime + ": " + (fEndTime - fStartTime));

        reset();
    }

    // initialize model, ready to be refresh upon a query
    protected void reset() {
        fMapCPUToCores.clear();
        fMapCoreToExecution.clear();
    }

    /**
     * @param signal -
     */
    @TmfSignalHandler
    public void timeRangeSelected(TmfRangeSynchSignal signal) {
        if (!fTraceActive) {
            return;
        }

        // Broadcasted in nano seconds
        TmfTimeRange timeRange = signal.getCurrentRange();
        fStartTime = timeRange.getStartTime().getValue();
        fEndTime = timeRange.getStartTime().getValue();
        System.out.println("Time Range selected: " + fStartTime + "->" + fEndTime);
        reset();
    }

    /**
     * @param signal -
     */
    @TmfSignalHandler
    public void traceSelected(TmfTraceSelectedSignal signal) {
        ITmfTrace activeTrace = signal.getTrace();
        if (activeTrace == fTrace) {
            fTraceActive = true;
        } else {
            fTraceActive = false;
        }
    }


    /**
     * @return The bundle context of the plug-in to which this service belongs.
     */
    @Override
    protected BundleContext getBundleContext() {
        return DsfTraceCorePlugin.getBundleContext();
    }

    /**
     * Return the CPU context resolved for the associated trace
     *
     * @param dmc - parent context
     * @return - All resolved CPUs in the associated trace
     */
    @Override
    public ICPUDMContext[] getCPUs(final IHardwareTargetDMContext dmc) {
        if (fMapCPUToCores.keySet().size() > 0) {
            // CPU's already resolved for the associated trace
            ICPUDMContext[] cpus = fMapCPUToCores.keySet().toArray(new ICPUDMContext[fMapCPUToCores.size()]);
            System.out.println("getCPUs, returning cpus from map: " + cpus.length);
            return cpus;
        }

        return resolveCPUContexts(dmc);
    }

    @Override
    public ICoreDMContext[] getCores(IDMContext dmc) {
        ICPUDMContext cpuDmc = DMContexts.getAncestorOfType(dmc, ICPUDMContext.class);

        if (cpuDmc == null) {
            // Retrieve all available cores
            Set<ICPUDMContext> cpus = fMapCPUToCores.keySet();
            if (cpus.size() < 1) {
                // Most likely not yet initialized, try to resolve the CPUs
                resolveCPUContexts(null);
            }

            // Now retrieve all available cores
            return getAllCores();
        }

        // Check if the ICoreDMContexts exist in our cpu map
        ICoreDMContext[] coreDmcs = fMapCPUToCores.get(cpuDmc);
        if (coreDmcs != null) {
            // We have previously resolved the cores, so lets return them
            return coreDmcs;
        }

        // Not available but cpu context is not null, not expected but we can
        // assume there are no core context on this trace
        return new ICoreDMContext[0];
    }

    protected ICoreDMContext[] getAllCores() {
        List<ICoreDMContext> cores = new ArrayList<>();
        Set<ICPUDMContext> cpus = fMapCPUToCores.keySet();
        if (cpus.size() > 0) {
            for (ICPUDMContext cpu : cpus) {
                ICoreDMContext[] coreDmcs = fMapCPUToCores.get(cpu);
                if (coreDmcs != null) {
                    for (ICoreDMContext coreDmc : coreDmcs) {
                        cores.add(coreDmc);
                    }
                } else {
                    System.out.println("No cores associated to cpu: " + cpu.toString());
                }
            }
        }

        System.out.println("getAllCores, returning " + cores.size() + ", cores");
        return new ICoreDMContext[cores.size()];
    }

    @Override
    public ICPUDMContext createCPUContext(IHardwareTargetDMContext targetDmc, String CPUId) {
        ICPUDMContext cpuDmc = new GDBCPUDMC(getSession().getId(), targetDmc, CPUId);
        return cpuDmc;
    }

    protected GDBCoreDMC createCoreContext(ICPUDMContext cpuDmc, Integer coreNode, String coreId) {
        GDBCoreDMC core = new GDBCoreDMC(getSession().getId(), cpuDmc, coreNode, coreId);
        // Make it possible to resolve the execution context from a core or vice
        // versa
        fMapCoreToExecution.put(core, new TraceExecutionDMC(getSession(), core, getActiveThread(core)));
        return core;
    }

    /**
     * Need to resolve the associated CPU's and Cores
     *
     * TODO: Trace compass does not seem to divide core and cpu today, For now
     * lets go with one CORE per CPU, and use the Trace compass CPU's as cores
     *
     * @param dmc
     * @return
     */
    protected ICPUDMContext[] resolveCPUContexts(IHardwareTargetDMContext dmc) {
        // For now treat a TRACE-CPU as Core and associate it with a local CPU
        // context
        try {
            int coreSSNode = fStateSys.getQuarkAbsolute(Attributes.CPUS);
            List<Integer> coreNodes = fStateSys.getSubAttributes(coreSSNode, false);

            // Represent it as one core per CPU
            ICPUDMContext[] cpuDmcs = new ICPUDMContext[coreNodes.size()];
            int i = 0;
            for (Integer coreNode : coreNodes) {
                String coreName = fStateSys.getAttributeName(coreNode);
                ICPUDMContext cpuDmc = cpuDmcs[i] = createCPUContext(dmc, String.valueOf(i));
                GDBCoreDMC[] coreDmcs = new GDBCoreDMC[] { createCoreContext(cpuDmcs[i], coreNode, coreName) };
                fMapCPUToCores.put(cpuDmc, coreDmcs);
                System.out.println("resolved core #" + coreName);
                i++;
            }

            // Normal scenario, returning contexts for each CPU
            System.out.println("resolved " + cpuDmcs.length + " CPUs"); //$NON-NLS-1$ //$NON-NLS-2$

            return cpuDmcs;
        } catch (AttributeNotFoundException e) {
            // No core attributes found
            System.out.println("State system, AttributeNotfound");
            return new ICPUDMContext[0];
        }
    }

    @Override
    public ILoadInfo getLoadInfo(final IDMContext context) throws CoreException {
        if (context instanceof ICoreDMContext) {
            System.out.println("********** CORE **************");
            return getCoreLoadInfo((ICoreDMContext) context);
        }

        if (context instanceof ICPUDMContext) {
            System.out.println("*********** CPU *************");
            // Resolve the load for given cpu context
            return getCPULoadInfo((ICPUDMContext) context);
        }

        // we only support getting the load for a CPU or a core
        throw new CoreException(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Load information not supported for this context type", null)); //$NON-NLS-1$
    }

    protected TraceThreadDMData getActiveThread(ICoreDMContext coreDmc) {
        // Validate context, and provide a handle to the internal class
        // implementation
        assert (coreDmc instanceof GDBCoreDMC);
        String execName = ""; //$NON-NLS-1$
        int currentThreadId = 0;

        GDBCoreDMC context = (GDBCoreDMC) coreDmc;

        ITmfStateSystem ss = TmfStateSystemAnalysisModule.getStateSystem(fTrace, KernelAnalysis.ID);
        if (ss != null) {

            try {
                int cpuQuark = resolveKernelCpuQuark(ss, context.fId);
                int currentThreadQuark = ss.getQuarkRelative(cpuQuark, Attributes.CURRENT_THREAD);
                ITmfStateInterval interval = ss.querySingleState(fEndTime, currentThreadQuark);
                if (!interval.getStateValue().isNull()) {
                    ITmfStateValue value = interval.getStateValue();
                    currentThreadId = value.unboxInt();

                    int execNameQuark = ss.getQuarkAbsolute(Attributes.THREADS, Integer.toString(currentThreadId), Attributes.EXEC_NAME);
                    interval = ss.querySingleState(fEndTime, execNameQuark);
                    if (!interval.getStateValue().isNull()) {
                        value = interval.getStateValue();
                        execName = value.unboxStr();
                    }
                }

                System.out.println("Current Thread id: " + currentThreadId + "\nCurrent executable: " + execName); //$NON-NLS-1$
            } catch (AttributeNotFoundException | TimeRangeException | StateValueTypeException e) {
                System.out.println("Error resolving active thread"); //$NON-NLS-1$
                e.printStackTrace();
            } catch (StateSystemDisposedException e) {
                /* Ignored */
            }
        }

        Integer oState = getIntegerThreadAttribute(currentThreadId, Attributes.STATUS);
        int state = oState == null ? 0 : oState.intValue();

        // can be null
        Integer ppid = getIntegerThreadAttribute(currentThreadId, Attributes.PPID);

        TraceThreadDMData thread = new TraceThreadDMData(currentThreadId, execName, ppid, state);
        thread.addCore(coreDmc.getId());

        return thread;
    }

    protected static int resolveKernelCpuQuark(ITmfStateSystem ss, String coreName) throws AttributeNotFoundException {
        int cpusNode = ss.getQuarkAbsolute(Attributes.CPUS);
        List<Integer> cpuNodes = ss.getSubAttributes(cpusNode, false);

        // Get the names to match against the name on the cpu state system

        int ssNode = -1;
        // Resolve the selected cpu node quark on the kernel ss
        for (int cpuNode : cpuNodes) {
            String ssCpuName = ss.getAttributeName(cpuNode);
            if (ssCpuName.equals(coreName)) {
                ssNode = cpuNode;
                break;
            }
        }

        return ssNode;
    }

    protected ILoadInfo getCoreLoadInfo(ICoreDMContext coreDmc) {
        long startTime = fStartTime;
        long endTime = fEndTime;

        double duration = endTime - startTime;

        // validate time ranges and prevent division by zero
        if (duration < 1) {
            assert false;
            throw new TimeRangeException();
        }

        // Validate context, and provide a handle to the internal class
        // implementation
        assert (coreDmc instanceof GDBCoreDMC);
        GDBCoreDMC context = (GDBCoreDMC) coreDmc;

        Integer[] coreNode = new Integer[] { context.getNode() };

        Map<String, Long> tidToCPUTime = fCPUModule.getCpuUsageInRange(coreNode, startTime, endTime);

        // The map from thread to time spent includes a grand total identified
        // with the key "total"
        double totCpuTime = tidToCPUTime.get("total"); //$NON-NLS-1$
        String cpuName = fStateSys.getAttributeName(context.getNode());
        // The idle time is represented by a thread id of zero over the
        // following key=value format "cpuname/threadId=value"
        double idleCpuTime = tidToCPUTime.get(cpuName + "/0"); //$NON-NLS-1$
        totCpuTime = totCpuTime - idleCpuTime;

        double loadPercent = (totCpuTime / duration) * 100;
        System.out.println("Core " + context.getId() + ", load: " + (int) loadPercent);

        return new GDBLoadInfo((int) loadPercent);
    }

    protected ILoadInfo getCPULoadInfo(ICPUDMContext cpuDmc) {
        int loadInfo = 0;

        // Resolve the context for the cores
        ICoreDMContext[] coreDmcs = fMapCPUToCores.get(cpuDmc);

        if (coreDmcs != null && coreDmcs.length > 0) {
            for (ICoreDMContext core : coreDmcs) {
                ILoadInfo coreLoadInfo = getCoreLoadInfo(core);
                assert (coreLoadInfo instanceof GDBLoadInfo);
                loadInfo += ((GDBLoadInfo) coreLoadInfo).getILoad();
            }

            // Take average load of all cores
            loadInfo = loadInfo / coreDmcs.length;
        }

        System.out.println("CPU load: " + cpuDmc.getId() + "->" + loadInfo + "\n"); //$NON-NLS-1$

        return new GDBLoadInfo(loadInfo);
    }

    // Processes related API
    @Override
    public IDMContext[] getProcessesBeingDebugged(IDMContext dmc) {
        Collection<IDMContext> execDmcs = new ArrayList<>();
        if (dmc instanceof TraceExecutionDMC) {
            // Returning a single execution context
            execDmcs.add(dmc);
        } else if (dmc instanceof GDBCoreDMC) {
            // Returning all thread execution contexts for a core
            execDmcs.add(fMapCoreToExecution.get(dmc));
        }

        return execDmcs.toArray(new TraceExecutionDMC[execDmcs.size()]);
    }

    @Override
    public IThreadDMData getExecutionData(IThreadDMContext dmc) {
        assert (dmc != null && dmc instanceof TraceExecutionDMC);

        return ((TraceExecutionDMC) dmc).getExecutionData();
    }

    // IRunControl related API
    @Override
    public IExecutionDMData getExecutionData(IExecutionDMContext dmc) {
        // TODO: no mapping between thread cpu stopped mode and the actual debug reason
        // assert dmc instanceof TraceExecutionDMC;
        // TraceExecutionDMC executionCtx = (TraceExecutionDMC) dmc;
        // int threadState = executionCtx.fThreadData.getState();

        return new IExecutionDMData() {

            @Override
            public StateChangeReason getStateChangeReason() {
                return StateChangeReason.UNKNOWN;
            }
        };
    }

    // TODO: This is based on KernelThreadInformationProvider.getParentPid,
    // this can be merged by e.g. adding a method in KernelThreadInformationProvider to retrieve the thread STATUS
    protected @Nullable Integer getIntegerThreadAttribute(Integer threadId, String attribute) {
        Integer intRetResult = null;
        ITmfStateSystem ss = TmfStateSystemAnalysisModule.getStateSystem(fTrace, KernelAnalysis.ID);

        if (ss == null) {
            return intRetResult;
        }

        Integer node;
        try {
            node = ss.getQuarkAbsolute(Attributes.THREADS, threadId.toString(), attribute);
            ITmfStateInterval nodeInterval = ss.querySingleState(fEndTime, node);
            ITmfStateValue value = nodeInterval.getStateValue();

            switch (value.getType()) {
            case INTEGER:
                intRetResult = NonNullUtils.checkNotNull(Integer.valueOf(value.unboxInt()));
                break;
            case DOUBLE:
            case LONG:
            case NULL:
            case STRING:
            default:
                break;
            }
        } catch (AttributeNotFoundException | StateSystemDisposedException | TimeRangeException e) {
        }
        return intRetResult;
    }

    @Override
    public boolean isSuspended(IExecutionDMContext context) {
        // The execution context provided in this implementation
        // are for the active threads only.
        return false;
    }
}
