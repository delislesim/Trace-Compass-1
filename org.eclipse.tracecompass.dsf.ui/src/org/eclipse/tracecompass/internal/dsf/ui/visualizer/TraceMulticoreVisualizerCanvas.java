/*******************************************************************************
 * Copyright (c) 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc Khouzam (Ericsson) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerCanvas;
import org.eclipse.swt.widgets.Composite;

/** */
@SuppressWarnings("restriction")
public class TraceMulticoreVisualizerCanvas extends MulticoreVisualizerCanvas {

    /**
     *
     * @param parent p
     */
    public TraceMulticoreVisualizerCanvas(Composite parent) {
        super(parent);
    }

    @Override
    protected boolean getCPULoadEnabled() {
        // For Trace Compass, we only show one core per CPU
        // so there is no point in showing the CPU load
        return false;
    }
}