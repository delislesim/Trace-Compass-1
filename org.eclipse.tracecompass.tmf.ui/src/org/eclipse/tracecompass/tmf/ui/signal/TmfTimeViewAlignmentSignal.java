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

import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;

/**
 * A signal to inform about the state of time alignment. Typically, the emitter
 * will inform the receivers about the position of a sash that separates the
 * time axis on right side extra information on the left side.
 *
 * @since 1.0
 */
public class TmfTimeViewAlignmentSignal extends TmfSignal {

    private TmfTimeViewAlignmentInfo fTimeViewAlignmentInfo;

    /**
     * Creates a new TmfTimeViewAlignmentSignal
     *
     * @param source
     *            the source of the signal
     * @param parameterObject TODO
     */
    public TmfTimeViewAlignmentSignal(Object source, TmfTimeViewAlignmentInfo parameterObject) {
        super(source);
        fTimeViewAlignmentInfo = parameterObject;
    }

    public TmfTimeViewAlignmentInfo getTimeViewAlignmentInfo() {
        return fTimeViewAlignmentInfo;
    }

    @Override
    public String toString() {
        return "[TmfTimeViewAlignmentSignal (" + fTimeViewAlignmentInfo.toString() + ")]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
}
