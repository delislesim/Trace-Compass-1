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

import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMContext;
import org.eclipse.cdt.dsf.debug.service.IProcesses.IThreadDMData;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMData;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICPUDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.ICoreDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS.IHardwareTargetDMContext;
import org.eclipse.cdt.dsf.gdb.service.IGDBHardwareAndOS2.ILoadInfo;
import org.eclipse.core.runtime.CoreException;

/**
 * Consolidating the API Services needed by the Multi-core visualizer
 *
 */
public interface IDsfTraceModelService {

    /*********************/
    /* Initialization */
    /*********************/
    /**
     * @param immediateRequestMonitor -
     */
    public void initialize(RequestMonitor requestMonitor);

    /*********************/
    /* HW OS related API */
    /*********************/

    /**
     * @param dmc -
     * @return - All known CPU's at the currently selected time on the associated trace
     */
    public ICPUDMContext[] getCPUs(final IHardwareTargetDMContext dmc);

    /**
     * @param dmc -
     * @return - All known Cores at the currently selected time on the associated trace
     */
    public ICoreDMContext[] getCores(IDMContext dmc);

    /**
     * @param targetDmc - Parent context
     * @param CPUId -
     * @return - Created instance
     */
    public ICPUDMContext createCPUContext(IHardwareTargetDMContext targetDmc, String CPUId);

    /**
     * @param context - Specific context for the resolution of the load
     * @return - The resolved load information
     * @throws CoreException -
     */
    public ILoadInfo getLoadInfo(IDMContext context) throws CoreException;

    /*************************/
    /* Processes related API */
    /*************************/
    /**
     * @param dmc - Processes known under the given context
     * @return - known processes contexts associated to the given context
     */
    public IDMContext[] getProcessesBeingDebugged(IDMContext dmc);

    /**
     * @param dmc -
     * @return - The thread data corresponding to the given context
     */
    public IThreadDMData getExecutionData(IThreadDMContext dmc);

    /***************************/
    /* IRunControl related API */
    /***************************/
    /**
     * @param dmc -
     * @return - The execution data associated to the given context
     */
    public IExecutionDMData getExecutionData(IExecutionDMContext dmc);
}
