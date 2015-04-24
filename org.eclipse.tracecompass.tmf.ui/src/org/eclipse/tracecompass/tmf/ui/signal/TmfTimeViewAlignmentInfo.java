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

/**
 * @since 1.0
 */
public class TmfTimeViewAlignmentInfo {
    final private Point fViewLocation;
    final private int fTimeAxisOffset;
    final private boolean fApply;
    final private int fWidth;

    private static final int NEAR_THRESHOLD = 10;

    /**
     * @param viewLocation location of the view
     * @param timeAxisOffset Offset relative to the view
     * @param width available width at the specified offset
     */
    public TmfTimeViewAlignmentInfo(Point viewLocation, int timeAxisOffset, int width) {
        fViewLocation = viewLocation;
        fTimeAxisOffset = timeAxisOffset;
        fWidth = width;
        fApply = true;
    }

    /**
     * @param viewLocation location of the view
     * @param timeAxisOffset Offset relative to the view
     * @param width available width at the specified offset
     * @param apply
     */
    public TmfTimeViewAlignmentInfo(Point viewLocation, int timeAxisOffset) {
        fViewLocation = viewLocation;
        fTimeAxisOffset = timeAxisOffset;
        fWidth = -1;
        fApply = false;
    }

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

    public int getWidth() {
        return fWidth;
    }

    public boolean isApply() {
        return fApply;
    }

    public boolean isViewLocationNear(Point location) {
        int distance = Math.abs(location.x - fViewLocation.x);
        return distance < NEAR_THRESHOLD;
    }
}