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
 * A signal to inform about the state of time alignment. Typically, the emitter
 * will inform the receivers about the position of a sash that separates the
 * time axis on right side extra information on the left side.
 *
 * @since 1.0
 */
public class TmfTimeViewAlignmentSignal extends TmfSignal {

    private int fTimeAxisOffset;
    private Point fViewLocation;
    private boolean fApply;

    /**
     * Creates a new TmfTimeViewAlignmentSignal
     *
     * @param source
     *            the source of the signal
     * @param viewLocation
     *            the location of the view
     * @param timeAxisOffset
     *            the offset of the time axis, typically the position of a sash
     * @param apply
     *            whether or not to apply this new alignment. Views emitting
     *            this signal should set this to false. This only informs the
     *            synchronizer of a new alignment. The synchronizer is
     *            responsible of emitting the signal that will apply the
     *            alignment.
     */
    public TmfTimeViewAlignmentSignal(Object source, Point viewLocation, int timeAxisOffset, boolean apply) {
        super(source);
        fViewLocation = viewLocation;
        fTimeAxisOffset = timeAxisOffset;
        fApply = apply;
    }

    /**
     * Get the offset of the time axis, typically the position of a sash
     *
     * @return the offset of the time axis
     */
    public int getTimeAxisOffset() {
        return fTimeAxisOffset;
    }

    /**
     * The location of the view emitting the signal, or null
     *
     * @return the view location or null
     */
    public Point getViewLocation() {
        return fViewLocation;
    }

    /**
     * Whether or not to apply the new alignment. Views should only react to the signal when this is true.
     *
     * @return Whether or not to apply the new alignment.
     */
    public boolean isApply() {
        return fApply;
    }

    @Override
    public String toString() {
        return "[TmfTimeViewAlignmentSignal (" + fViewLocation + ", " + fViewLocation + ", " + fApply + ")]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}
