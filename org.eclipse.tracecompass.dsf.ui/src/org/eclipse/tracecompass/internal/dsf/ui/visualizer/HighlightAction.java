/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * Contributors:
 *     Marc Dumais - Initial API and implementation
 *******************************************************************************/
package org.eclipse.tracecompass.internal.dsf.ui.visualizer;


import org.eclipse.cdt.visualizer.ui.VisualizerAction;

@SuppressWarnings("javadoc")
public class HighlightAction extends VisualizerAction {
    // --- members ---

    /** Visualizer instance we're associated with. */
    private TraceMulticoreVisualizer m_visualizer = null;
    private final String m_highlightMode;


    // --- constructors/destructors ---

    /** Constructor. */
    public HighlightAction(String mode)
    {
        m_highlightMode = mode;
        setText("Highlight mode: " + mode); //$NON-NLS-1$
        setDescription("Highlight mode of the visualizer's threads"); //$NON-NLS-1$
    }

    /** Dispose method. */
    @Override
    public void dispose()
    {
        m_visualizer = null;
        super.dispose();
    }


    // --- init methods ---

    /** Initializes this action for the specified view. */
    public void init(TraceMulticoreVisualizer visualizer)
    {
        m_visualizer = visualizer;
    }


    // --- methods ---

    /** Invoked when action is triggered. */
    @Override
    public void run() {
        if (m_visualizer != null) {
            m_visualizer.setHighlightEnabled(m_highlightMode);
        }
    }
}
