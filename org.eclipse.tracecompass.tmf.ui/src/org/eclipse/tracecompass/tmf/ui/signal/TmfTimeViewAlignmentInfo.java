package org.eclipse.tracecompass.tmf.ui.signal;

import org.eclipse.swt.graphics.Point;

/**
 * @since 1.0
 */
public class TmfTimeViewAlignmentInfo {
    private Point fViewLocation;
    private int fTimeAxisOffset;
    private boolean fApply;
    private int fWidth;

    public TmfTimeViewAlignmentInfo(Point viewLocation, int timeAxisOffset, int width, boolean apply) {
        fViewLocation = viewLocation;
        fTimeAxisOffset = timeAxisOffset;
        fWidth = width;
        fApply = apply;
    }

    public Point getViewLocation() {
        return fViewLocation;
    }

    public int getTimeAxisOffset() {
        return fTimeAxisOffset;
    }

    public int getWidth() {
        return fWidth;
    }

    public boolean isApply() {
        return fApply;
    }
}