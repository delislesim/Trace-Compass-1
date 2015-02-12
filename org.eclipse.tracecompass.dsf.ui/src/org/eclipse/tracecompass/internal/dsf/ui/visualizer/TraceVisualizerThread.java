/*******************************************************************************
 * Copyright (c) 2015 Ericsson AB
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alvaro Sanchez-Leon (Ericsson) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import org.eclipse.cdt.dsf.debug.service.IStack.IFrameDMData;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.model.VisualizerCore;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.model.VisualizerExecutionState;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.model.VisualizerThread;


public class TraceVisualizerThread extends VisualizerThread {

    private int fKernelState;

    public TraceVisualizerThread(VisualizerCore core, int pid, int tid, int gdbtid, VisualizerExecutionState state, IFrameDMData frame, int kernelState) {
        super(core, pid, tid, gdbtid, state, frame);
        fKernelState = kernelState;
    }

    public int getKernelState() {
        return fKernelState;
    }

}
