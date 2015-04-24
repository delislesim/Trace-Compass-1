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

package org.eclipse.tracecompass.tmf.ui.views;

import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;

/**
 * @since 1.0
 */
public interface ITmfTimeAligned {

    TmfTimeViewAlignmentInfo getTimeViewAlignmentInfo();
    int getAvailableWidth(int requestedOffset);
}
