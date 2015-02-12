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

import java.util.Enumeration;
import java.util.List;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.MulticoreVisualizerUIPlugin;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.model.VisualizerCPU;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.model.VisualizerCore;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.model.VisualizerThread;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerCPU;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerCanvas;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerCore;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerLoadMeter;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizerThread;
import org.eclipse.cdt.visualizer.ui.util.GUIUtils;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

/** */
@SuppressWarnings("restriction")
public class TraceMulticoreVisualizerCanvas extends MulticoreVisualizerCanvas {

    private ColorChoser m_colorChoser = new ColorChoser();

    /** Current trace visualizer model we're displaying.  */
//    protected TraceVisualizerModel m_model = null;

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



    // TODO: we could make a change in the open-source visualizer to factor-out the object creation of
    // cores/CPUs from the code below. Then we could avoid overriding this whole method and instead just
    // override, for example, "createCoreObject()".

    /** Recache persistent objects (tiles, etc.) for new monitor */
    // synchronized so we don't change recache flags while doing a recache
    @Override
    public synchronized void recache() {
        if (! m_recache)
         {
            return; // nothing to do, free the lock quickly
        }

        if (m_recacheState) {

            // clear all grid view objects
            clear();

            // clear cached state
            m_cpus.clear();
            m_cores.clear();
            m_threads.clear();
            m_cpuMap.clear();
            m_coreMap.clear();
            m_threadMap.clear();

            if (m_model != null) {
                for (VisualizerCPU cpu : m_model.getCPUs()) {
                    // current filter permits displaying this CPU?
                    if (m_canvasFilterManager.displayObject(cpu)) {
                        MulticoreVisualizerCPU mcpu = new MulticoreVisualizerCPU(cpu.getID());
                        m_cpus.add(mcpu);
                        m_cpuMap.put(cpu, mcpu);
                        for (VisualizerCore core : cpu.getCores()) {
                            // current filter permits displaying this core?
                            if(m_canvasFilterManager.displayObject(core)) {
                                // **** change compared to open-source code ****
                                MulticoreVisualizerCore mcore = new TraceMulticoreVisualizerCore(mcpu, core.getID());
                                // **** end of change compared to open-source code ****
                                m_cores.add(mcore);
                                m_coreMap.put(core, mcore);
                            }
                        }
                    }
                }
            }

            // we've recached state, which implies recacheing sizes and load meters
            m_recacheState = false;
            m_recacheLoadMeters = true;
            m_recacheSizes = true;
        }

        if (m_recacheLoadMeters) {
            // refresh the visualizer CPU and core load meters
            if (m_model != null) {
                Enumeration<VisualizerCPU> modelCpus = m_cpuMap.keys();
                while (modelCpus.hasMoreElements()) {
                    VisualizerCPU modelCpu = modelCpus.nextElement();
                    MulticoreVisualizerCPU visualizerCpu = m_cpuMap.get(modelCpu);
                    // when filtering is active, not all objects might be in the map
                    if (visualizerCpu != null) {
                        // update CPUs load meter
                        MulticoreVisualizerLoadMeter meter = visualizerCpu.getLoadMeter();
                        meter.setEnabled(getCPULoadEnabled());
                        meter.setLoad(modelCpu.getLoad());
                        meter.setHighLoadWatermark(modelCpu.getHighLoadWatermark());

                        for (VisualizerCore modelCore : modelCpu.getCores()) {
                            MulticoreVisualizerCore visualizerCore = m_coreMap.get(modelCore);
                            // when filtering is active, not all objects might be in the map
                            if (visualizerCore != null) {
                                // update cores load meter
                                meter = visualizerCore.getLoadMeter();
                                meter.setEnabled(m_model.getLoadMetersEnabled());
                                meter.setLoad(modelCore.getLoad());
                                meter.setHighLoadWatermark(modelCore.getHighLoadWatermark());
                            }
                        }
                    }
                }
            }

            m_recacheSizes = true;
            m_recacheLoadMeters = false;
        }

        if (m_recacheSizes) {
            // avoid doing resize calculations if the model is not ready
            if (m_model == null ) {
                m_recacheSizes = false;
                return;
            }
            // update cached size information

            // General margin/spacing constants.
            int cpu_margin = 8;       // margin around edges of CPU grid
            int cpu_separation = 6;   // spacing between CPUS
            int statusBarHeight;

            statusBarHeight = 20;

            // make room when load meters are present, else use a more compact layout
            int core_margin = getCPULoadEnabled() ? 20 : 12;      // margin around cores in a CPU
            int core_separation = 4;  // spacing between cores

            int loadMeterWidth = core_margin*3/5;
            int loadMeterHMargin = core_margin/5;
            int loadMeterHCoreMargin = loadMeterHMargin + 5;

            // Get overall area we have for laying out content.
            Rectangle bounds = getClientArea();
            GUIUtils.inset(bounds, cpu_margin);

            // Figure out area to allocate to each CPU box.
            int ncpus  = m_cpus.size();
            int width  = bounds.width  + cpu_separation;
            int height = bounds.height + cpu_separation - statusBarHeight;

            // put status bar at the bottom of the canvas area
            m_statusBar.setBounds(cpu_margin,
                    bounds.y + bounds.height - 2 * cpu_margin,
                    width ,
                    statusBarHeight);

            int cpu_edge = fitSquareItems(ncpus, width, height);
            int cpu_size = cpu_edge - cpu_separation;
            if (cpu_size < 0) {
                cpu_size = 0;
            }

            // Calculate area on each CPU for placing cores.
            int ncores = 0;
            // find the greatest number of cores on a given CPU and use
            // that number for size calculations for all CPUs - this way
            // we avoid displaying cores of varying sizes, in different
            // CPUs.
            for (MulticoreVisualizerCPU cpu : m_cpus) {
                int n = cpu.getCores().size();
                if (n > ncores) {
                    ncores = n;
                }
            }
            int cpu_width  = cpu_size - core_margin * 2 + core_separation;
            int cpu_height = cpu_size - core_margin * 2 + core_separation;
            int core_edge  = fitSquareItems(ncores, cpu_width, cpu_height);
            int core_size  = core_edge - core_separation;
            if (core_size < 0) {
                core_size = 0;
            }

            int x = bounds.x, y = bounds.y;
            for (MulticoreVisualizerCPU cpu : m_cpus) {
                cpu.setBounds(x, y, cpu_size-1, cpu_size-1);
                // put cpu meter in the right margin of the CPU
                cpu.getLoadMeter().setBounds(x + cpu_size - 2*cpu_margin,
                        y + 2*core_margin,
                        loadMeterWidth,
                        cpu_size-3*core_margin);

                int left = x + core_margin;
                int cx = left, cy = y + core_margin;
                for (MulticoreVisualizerCore core : cpu.getCores())
                {
                    core.setBounds(cx, cy, core_size, core_size);

                    core.getLoadMeter().setBounds(
                            cx + core_size - loadMeterHCoreMargin - loadMeterWidth,
                            cy + core_size * 1 / 3,
                            loadMeterWidth,
                            core_size * 2 / 3 - loadMeterHCoreMargin
                            );

                    cx += core_size + core_separation;
                    if (cx + core_size + core_margin > x + cpu_size) {
                        cx = left;
                        cy += core_size + core_separation;
                    }
                }

                x += cpu_size + cpu_separation;
                if (x + cpu_size > bounds.x + width) {
                    x = bounds.x;
                    y += cpu_size + cpu_separation;
                }
            }

            m_recacheSizes = false;
        }
        m_recache = false;
    }


    // TODO: we could make a change in the open-source visualizer to factor-out the object creation of
    // threads from the code below. Then we could avoid overriding this whole method and instead just
    // override, for example, "createThreadObject()"

    /** Invoked when canvas repaint event is raised.
     *  Default implementation clears canvas to background color.
     */
    @Override
    public void paintCanvas(GC gc) {
        assert m_model instanceof TraceVisualizerModel;
        TraceVisualizerModel model = (TraceVisualizerModel) m_model;

        if (m_model == null) {
            return;
        }

        // NOTE: We have a little setup to do first,
        // so we delay clearing/redrawing the canvas until needed,
        // to minimize any potential visual flickering.

        // recache/resize tiles & shims if needed
        recache();

        // do any "per frame" updating/replacement of graphic objects

        // recalculate process/thread graphic objects on the fly
        // TODO: can we cache/pool these and just move them around?
        for (MulticoreVisualizerCore core : m_cores) {
            core.removeAllThreads();
        }
        m_threads.clear();
        m_threadMap.clear();

        // update based on current processes/threads
        if (m_model != null) {

            // NOTE: we assume that we've already created and sized the graphic
            // objects for cpus/cores in recache() above,
            // so we can use these to determine the size/location of more dynamic elements
            // like processes and threads

            for (VisualizerThread thread : model.getThreads()) {
                TraceVisualizerModelThread modelThread = (TraceVisualizerModelThread) thread;

                // current filter permits displaying this thread?
                if(m_canvasFilterManager.displayObject(modelThread)) {
                    VisualizerCore core = thread.getCore();
                    MulticoreVisualizerCore mcore = m_coreMap.get(core);
                    if (mcore != null) {
                        MulticoreVisualizerThread mthread =
                                new TraceMulticoreVisualizerThread(m_colorChoser, model.getColorHighlightMode(), mcore, modelThread);
                        mcore.addThread(mthread);
                        m_threads.add(mthread);
                        m_threadMap.put(thread, mthread);
                    }
                }
            }

            // now set sizes of processes/threads for each tile
            for (MulticoreVisualizerCore core : m_cores) {
                Rectangle bounds = core.getBounds();

                // how we lay out threads depends on how many there are
                List<MulticoreVisualizerThread> threads = core.getThreads();
                int threadspotsize = MulticoreVisualizerThread.THREAD_SPOT_SIZE;
                int threadheight = threadspotsize + THREAD_SPACING;
                int count = threads.size();
                int tileheight = bounds.height - 4;
                int tx = bounds.x + 2;
                int ty = bounds.y + 2;
                int dty = (count < 1) ? 0 : tileheight / count;
                if (dty > threadheight) {
                    dty = threadheight;
                }
                if (count > 0 && dty * count <= tileheight) {
                    ty = bounds.y + 2 + (tileheight - (dty * count)) / 2;
                    if (ty < bounds.y + 2) {
                        ty = bounds.y + 2;
                    }
                }
                else if (count > 0) {
                    dty = tileheight / count;
                    if (dty > threadheight) {
                        dty = threadheight;
                    }
                }
                int t = 0;
                for (MulticoreVisualizerThread threadobj : threads) {
                    int y = ty + dty * (t++);
                    threadobj.setBounds(tx, y, threadspotsize, threadspotsize);
                }
            }
        }

        // restore canvas object highlighting from model object selection
        restoreSelection();

        // FIXME: enable secondary highlight for threads that are
        // part of a selected process.
        m_selectedPIDs.clear();
        for (MulticoreVisualizerThread mthread : m_threads) {
            if (mthread.isSelected()) {
                m_selectedPIDs.add(mthread.getPID());
            }
        }
        for (MulticoreVisualizerThread mthread : m_threads) {
            mthread.setProcessSelected(m_selectedPIDs.contains(mthread.getPID()));
        }

        // NOW we can clear the background
        clearCanvas(gc);

        // Make sure color/font resources are properly initialized.
        MulticoreVisualizerUIPlugin.getResources();

        // paint cpus
        for (MulticoreVisualizerCPU cpu : m_cpus) {
            cpu.paintContent(gc);
            cpu.getLoadMeter().paintContent(gc);
            cpu.getLoadMeter().paintDecorations(gc);
        }

        // paint cores
        for (MulticoreVisualizerCore core : m_cores) {
            core.paintContent(gc);
            core.getLoadMeter().paintContent(gc);
            core.getLoadMeter().paintDecorations(gc);
        }

        // paint cpus IDs on top of cores
        for (MulticoreVisualizerCPU cpu : m_cpus) {
            cpu.paintDecorations(gc);
        }

        // paint threads on top of cores
        for (MulticoreVisualizerThread thread : m_threads) {
            thread.paintContent(gc);
        }

        String highlightMode = model.getColorHighlightMode();
        if (highlightMode != null) {
            m_statusBar.setMessage("Color Highlight mode: " + highlightMode);
        }
        else {
            m_statusBar.setMessage("Color Highlight mode: none");
        }
//        Font oldFont = m_textFont;
//        Font newFont = CDTVisualizerUIPlugin.getResources().getFont("Luxi Sans", 18); //$NON-NLS-1$
//        setFont(newFont);
        // paint status bar
        m_statusBar.paintContent(gc);
//        setFont(oldFont);

        // paint drag-selection marquee last, so it's on top.
        m_marquee.paintContent(gc);
    }
}