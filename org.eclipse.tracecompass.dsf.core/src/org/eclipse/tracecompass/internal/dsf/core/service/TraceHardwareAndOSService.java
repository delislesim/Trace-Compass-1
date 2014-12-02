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
import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ImmediateExecutor;
import org.eclipse.cdt.dsf.concurrent.Immutable;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.AbstractDMContext;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.datamodel.IDMData;
import org.eclipse.cdt.dsf.debug.service.ICachingService;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS2;
import org.eclipse.cdt.dsf.service.AbstractDsfService;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tracecompass.internal.dsf.core.DsfTraceCorePlugin;
import org.osgi.framework.BundleContext;

/** */
public class TraceHardwareAndOSService extends AbstractDsfService implements IGDBHardwareAndOS2, ICachingService {

	@Immutable
	private static class GDBCPUDMC extends AbstractDMContext
	implements ICPUDMContext
	{
		/**
		 * String ID that is used to identify the thread in the GDB/MI protocol.
		 */
		private final String fId;

		/**
		 * @param sessionId The session
		 * @param targetDmc The target
		 * @param id the CPU id
		 */
        protected GDBCPUDMC(String sessionId, IHardwareTargetDMContext targetDmc, String id) {
            super(sessionId, targetDmc == null ? new IDMContext[0] : new IDMContext[] { targetDmc });
            fId = id;
        }

		@Override
		public String getId(){
			return fId;
		}

		@Override
		public String toString() { return baseToString() + ".CPU[" + fId + "]"; }  //$NON-NLS-1$ //$NON-NLS-2$

		@Override
		public boolean equals(Object obj) {
			return baseEquals(obj) && ((GDBCPUDMC)obj).fId.equals(fId);
		}

		@Override
		public int hashCode() { return baseHashCode() ^ fId.hashCode(); }
	}

    @Immutable
    private static class GDBCoreDMC extends AbstractDMContext
	implements ICoreDMContext
	{
		private final String fId;

		public GDBCoreDMC(String sessionId, ICPUDMContext CPUDmc, String id) {
			super(sessionId, CPUDmc == null ? new IDMContext[0] : new IDMContext[] { CPUDmc });
			fId = id;
		}

		@Override
		public String getId(){ return fId; }

		@Override
		public String toString() { return baseToString() + ".core[" + fId + "]"; }  //$NON-NLS-1$ //$NON-NLS-2$

		@Override
		public boolean equals(Object obj) {
			return baseEquals(obj) &&
			       (((GDBCoreDMC)obj).fId == null ? fId == null : ((GDBCoreDMC)obj).fId.equals(fId));
		}

		@Override
		public int hashCode() { return baseHashCode() ^ (fId == null ? 0 : fId.hashCode()); }
	}

//    @Immutable
//    private static class GDBCPUDMData implements ICPUDMData {
//    	final int fNumCores;
//
//    	public GDBCPUDMData(int num) {
//    		fNumCores = num;
//    	}
//
//		@Override
//		public int getNumCores() { return fNumCores; }
//    }
//
//    @Immutable
//    private static class GDBCoreDMData implements ICoreDMData {
//    	final String fPhysicalId;
//
//    	public GDBCoreDMData(String id) {
//    		fPhysicalId = id;
//    	}
//
//		@Override
//		public String getPhysicalId() { return fPhysicalId; }
//    }


    @Immutable
    private class GDBLoadInfo implements ILoadInfo {
    	private String fLoad;
    	private Map<String,String> fDetailedLoad;

    	public GDBLoadInfo(String load, Map<String,String> detailedLoad) {
    		fLoad = load;
   			fDetailedLoad = detailedLoad;
    	}
    	public GDBLoadInfo(String load) {
    		this(load, null);
    	}
		@Override
		public String getLoad() {
			return fLoad;
		}
		@Override
		public Map<String,String> getDetailedLoad() {
			return fDetailedLoad;
		}
    }

    /**
     * @param session The DSF session for this service.
     */
    public TraceHardwareAndOSService(DsfSession session) {
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
        // Register this service.
		register(new String[] { IGDBHardwareAndOS.class.getName(),
								IGDBHardwareAndOS2.class.getName(),
				                TraceHardwareAndOSService.class.getName() },
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
	public void getCPUs(final IHardwareTargetDMContext dmc, final DataRequestMonitor<ICPUDMContext[]> rm) {
        // TODO
        ICPUDMContext[] cpuDmcs = new ICPUDMContext[4];
        for (int i = 0; i < cpuDmcs.length; i++) {
            cpuDmcs[i] = createCPUContext(dmc, Integer.toString(i));
        }

        rm.done(cpuDmcs);
	}

	@Override
	public void getCores(IDMContext dmc, final DataRequestMonitor<ICoreDMContext[]> rm) {
		ICPUDMContext cpuDmc = DMContexts.getAncestorOfType(dmc, ICPUDMContext.class);

		// TODO
		if (cpuDmc == null) {
		    IHardwareTargetDMContext targetDmc = DMContexts.getAncestorOfType(dmc, IHardwareTargetDMContext.class);
		    cpuDmc = createCPUContext(targetDmc, "1"); //$NON-NLS-1$
		}
		ICoreDMContext[] coreDmcs = new ICoreDMContext[1];
        coreDmcs[0] = createCoreContext(cpuDmc, cpuDmc.getId());

        rm.done(coreDmcs);
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
		return new GDBCPUDMC(getSession().getId(), targetDmc, CPUId);
	}

	@Override
	public ICoreDMContext createCoreContext(ICPUDMContext cpuDmc, String coreId) {
		return new GDBCoreDMC(getSession().getId(), cpuDmc, coreId);
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

 	@Override
	public void getLoadInfo(final IDMContext context, final DataRequestMonitor<ILoadInfo> rm) {
		if (!(context instanceof ICoreDMContext) && !(context instanceof ICPUDMContext)) {
			// we only support getting the load for a CPU or a core
			rm.done(new Status(IStatus.ERROR, DsfTraceCorePlugin.PLUGIN_ID, INVALID_HANDLE, "Load information not supported for this context type", null)); //$NON-NLS-1$
			return;
		}

		// TODO
		rm.done(new GDBLoadInfo("50")); //$NON-NLS-1$
    }
}
