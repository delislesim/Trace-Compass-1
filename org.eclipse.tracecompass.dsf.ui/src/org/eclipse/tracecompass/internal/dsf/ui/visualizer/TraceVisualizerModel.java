/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * Contributors:
 *     Marc Dumais - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.dsf.ui.visualizer;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.model.VisualizerModel;


@SuppressWarnings("restriction")
public class TraceVisualizerModel extends VisualizerModel {

    /** Whether the color highlight mode is on or off */
    protected String m_colorHighlightMode;


    /**
     * @param sessionId unique session id
     */
    public TraceVisualizerModel(final String sessionId) {
        super(sessionId);
    }

    public void setColorHighlightMode (String mode) {
        m_colorHighlightMode = mode;
    }

    public String getColorHighlightMode () {
        return m_colorHighlightMode;
    }


}
