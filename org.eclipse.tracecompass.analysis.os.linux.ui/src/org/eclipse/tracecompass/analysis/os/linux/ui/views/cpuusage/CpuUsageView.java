/*******************************************************************************
 * Copyright (c) 2014, 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.ui.views.cpuusage;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignementSignal;
import org.eclipse.tracecompass.tmf.ui.views.ITmfTimeAligned;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * CPU usage view. It contains 2 viewers: one tree viewer showing all the
 * threads who were on the CPU in the time range, and one XY chart viewer
 * plotting the total time spent on CPU and the time of the threads selected in
 * the tree viewer.
 *
 * @author Geneviève Bastien
 */
public class CpuUsageView extends TmfView implements ITmfTimeAligned {

    /** ID string */
    public static final String ID = "org.eclipse.tracecompass.analysis.os.linux.views.cpuusage"; //$NON-NLS-1$

    private CpuUsageComposite fTreeViewer = null;
    private CpuUsageXYViewer fXYViewer = null;

    private SashForm fSashForm;

    private Listener fSashDragListener;
    //private TmfSignalThrottler fTimeAlignmentThrottle = new TmfSignalThrottler(null, 200);

    /**
     * Constructor
     */
    public CpuUsageView() {
        super(Messages.CpuUsageView_Title);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        fSashForm = new SashForm(parent, SWT.NONE);

        fTreeViewer = new CpuUsageComposite(fSashForm);

        /* Build the XY chart part of the view */
        fXYViewer = new CpuUsageXYViewer(fSashForm);

        /* Add selection listener to tree viewer */
        fTreeViewer.addSelectionChangeListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    Object structSelection = ((IStructuredSelection) selection).getFirstElement();
                    if (structSelection instanceof CpuUsageEntry) {
                        CpuUsageEntry entry = (CpuUsageEntry) structSelection;
                        fTreeViewer.setSelectedThread(entry.getTid());
                        fXYViewer.setSelectedThread(Long.valueOf(entry.getTid()));
                    }
                }
            }
        });

        fSashForm.setLayout(new FillLayout());
        fSashForm.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                // Sashes in a SashForm are being created on layout so add the
                // drag listener here
                if (fSashDragListener == null) {
                    for (Control control : fSashForm.getChildren()) {
                        if (control instanceof Sash) {
                            fSashDragListener = new Listener() {

                                @Override
                                public void handleEvent(Event event) {
                                    sendTimeViewAlignmentChanged();
                                }
                            };
                            control.addListener(SWT.Selection, fSashDragListener);
                            // There should be only one sash
                            break;
                        }
                    }
                }
            }
        });

        /* Initialize the viewers with the currently selected trace */
        ITmfTrace trace = TmfTraceManager.getInstance().getActiveTrace();
        if (trace != null) {
            TmfTraceSelectedSignal signal = new TmfTraceSelectedSignal(this, trace);
            fTreeViewer.traceSelected(signal);
            fXYViewer.traceSelected(signal);
        }
//        fSashForm.addControlListener(new ControlListener() {
//
//            @Override
//            public void controlResized(ControlEvent e) {
//                sendTimeViewAlignmentChanged();
//            }
//
//            @Override
//            public void controlMoved(ControlEvent e) {
//            }
//        });
    }

    private void sendTimeViewAlignmentChanged() {
        int width = (int)((float)fSashForm.getWeights()[0] / 1000 * fSashForm.getBounds().width);
        TmfSignalManager.dispatchSignal(new TmfTimeViewAlignementSignal(fSashForm, fSashForm.getLocation(), width + fSashForm.getSashWidth(), false));
        //fTimeAlignmentThrottle.queue(new TmfTimeViewAlignementSignal(fSashForm, fSashForm.getLocation(), width + fSashForm.getSashWidth(), false));
        System.out.println("Cpu usage:" + width); //$NON-NLS-1$
    }

    @Override
    public void setFocus() {
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fTreeViewer != null) {
            fTreeViewer.dispose();
        }
        if (fXYViewer != null) {
            fXYViewer.dispose();
        }
    }

    /**
     * Handler for the window range signal.
     *
     * @param signal
     *            The signal that's received
     * @since 1.0
     */
    @TmfSignalHandler
    public void timeViewAlignementUpdated(final TmfTimeViewAlignementSignal signal) {
        if (!signal.isExecute()) {
            return;
        }
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (signal.getSource() != fSashForm) {
                    int total = fSashForm.getBounds().width;
                    int width1 = (int)(signal.getTimeAxisOffset() / (float)total * 1000) ;
                    int width2 = (int)((total - signal.getTimeAxisOffset()) / (float)total * 1000);
                    fSashForm.setWeights(new int[] { width1, width2 });
                    fSashForm.layout(); //nedded?
                }
            }
        });
    }

    /**
     * @since 1.0
     */
    @Override
    public void realignTimeView() {
        sendTimeViewAlignmentChanged();
    }

}
