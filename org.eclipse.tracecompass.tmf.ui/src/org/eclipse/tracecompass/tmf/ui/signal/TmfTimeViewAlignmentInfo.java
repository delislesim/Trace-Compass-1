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
import org.eclipse.swt.widgets.Shell;

/**
 * Information necessary to perform proper alignment of view.
 * @see TmfTimeViewAlignmentSignal
 *
 * @since 1.0
 */
public class TmfTimeViewAlignmentInfo {
    private final Point fViewLocation;
    private final int fTimeAxisOffset;
    private final Shell fShell;

    /**
     * Constructs a new TmfTimeViewAlignmentInfo
     *
     * @param shell used to determine whether or not views should be aligned together
     * @param viewLocation location of the view, used to determine whether or not views should be aligned together
     * @param timeAxisOffset offset relative to the view. This offset will be communicated to the other views
     */
    public TmfTimeViewAlignmentInfo(Shell shell, Point viewLocation, int timeAxisOffset) {
        fViewLocation = viewLocation;
        fTimeAxisOffset = timeAxisOffset;
        fShell = shell;
    }

    /**
     *
     * @return
     */
    public Point getViewLocation() {
        return fViewLocation;
    }

    /**
     * Offset relative to the view
     *
     * @return
     */
    public int getTimeAxisOffset() {
        return fTimeAxisOffset;
    }

    public Shell getShell() {
        return fShell;
    }
}