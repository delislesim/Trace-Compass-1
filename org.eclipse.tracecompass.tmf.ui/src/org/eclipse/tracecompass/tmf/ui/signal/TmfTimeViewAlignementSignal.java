/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.signal;

import org.eclipse.swt.graphics.Point;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;

/**
 * @since 1.0
 */
public class TmfTimeViewAlignementSignal extends TmfSignal {

    private int fTimeAxisOffset;
    private Point fViewLocation;
    private boolean fExecute;

    public TmfTimeViewAlignementSignal(Object source, Point viewLocation, int timeAxisOffset, boolean execute) {
        super(source);
        fViewLocation = viewLocation;
        fTimeAxisOffset = timeAxisOffset;
        fExecute = execute;
    }

    public int getTimeAxisOffset() {
        return fTimeAxisOffset;
    }

    public Point getViewLocation() {
        return fViewLocation;
    }

    public boolean isExecute() {
        return fExecute;
    }

    @Override
    public String toString() {
        return "[TmfTimeViewAlignementSignal (" + fViewLocation + ", " + fViewLocation + ")]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
