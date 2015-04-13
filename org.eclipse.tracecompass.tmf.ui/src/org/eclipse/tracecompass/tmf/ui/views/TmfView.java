/*******************************************************************************
 * Copyright (c) 2009, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *   Bernd Hufmann - Added possibility to pin view
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ui.views;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.tmf.core.component.ITmfComponent;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalThrottler;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignementSignal;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

/**
 * Basic abstract TMF view class implementation.
 *
 * It registers any sub class to the signal manager for receiving and sending
 * TMF signals.
 *
 * @author Francois Chouinard
 */
public abstract class TmfView extends ViewPart implements ITmfComponent {

    private final String fName;
    private Composite fParentComposite;
    private static final TmfSignalThrottler fTimeAlignmentThrottle = new TmfSignalThrottler(null, 200);

    /**
     * @since 1.0
     */
    public Composite getParentComposite() {
        return fParentComposite;
    }

    @Override
    public void createPartControl(Composite parent) {
        fParentComposite = parent;
        if (this instanceof ITmfTimeAligned) {
            parent.addControlListener(new ControlListener() {

                @Override
                public void controlResized(ControlEvent e) {
                    System.out.println(getTitle() + " resized");
                    realignViews(null);
                }

                @Override
                public void controlMoved(ControlEvent e) {
                    // TODO Auto-generated method stub

                }
            });
            realignViews(this);
        }
    }

    private void realignViews(TmfView skipView) {
        ITmfTimeAligned referenceView = null;
        int numTimeAlignedView = 0;
        for (IViewReference ref : TmfView.this.getSite().getPage().getViewReferences()) {
            IViewPart view = ref.getView(false);
            if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                TmfView tmfView = (TmfView) view;
                Composite parentComposite = tmfView.getParentComposite();
                if (parentComposite != null && parentComposite.isVisible()) {
                    numTimeAlignedView++;
                    System.out.println(tmfView.getTitle() + " " + Boolean.toString(tmfView.getParentComposite().isVisible()));
                    if (view != skipView) {
                        referenceView = (ITmfTimeAligned) view;
                    }
                }
            }
        }
        if (numTimeAlignedView > 1 && referenceView != null) {
            System.out.println("realignTimeView");
            referenceView.realignTimeView();
        }
    }

    /**
     * Action class for pinning of TmfView.
     */
    protected PinTmfViewAction fPinAction;



//    private static final Map<Shell, ControlListener> fShellResizeListeners = new WeakHashMap<>();

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor. Creates a TMF view and registers to the signal manager.
     *
     * @param viewName
     *            A view name
     */
    public TmfView(String viewName) {
        super();
        fName = viewName;
        TmfSignalManager.register(this);
    }

    /**
     * Disposes this view and de-registers itself from the signal manager
     */
    @Override
    public void dispose() {
        TmfSignalManager.deregister(this);
        super.dispose();
    }

    // ------------------------------------------------------------------------
    // ITmfComponent
    // ------------------------------------------------------------------------

    @Override
    public String getName() {
        return fName;
    }

    @Override
    public void broadcast(TmfSignal signal) {
        TmfSignalManager.dispatchSignal(signal);
    }

    @Override
    public void broadcastAsync(TmfSignal signal) {
        TmfSignalManager.dispatchSignalAsync(signal);
    }

    // ------------------------------------------------------------------------
    // View pinning support
    // ------------------------------------------------------------------------

    /**
     * Returns whether the pin flag is set.
     * For example, this flag can be used to ignore time synchronization signals from other TmfViews.
     *
     * @return pin flag
     */
    public boolean isPinned() {
        return ((fPinAction != null) && (fPinAction.isChecked()));
    }

    @Override
    public void init(final IViewSite site) throws PartInitException {
        super.init(site);

//        final Shell shell = site.getPage().getWorkbenchWindow().getShell();
//        ControlListener listener = new ControlListener() {
//
//            @Override
//            public void controlResized(ControlEvent e) {
//                System.out.println("Shell resized: " + shell.getSize().x); //$NON-NLS-1$
//                for (IViewReference ref : site.getPage().getViewReferences()) {
//                    IViewPart view = ref.getView(false);
//                    if (view instanceof ITmfTimeAligned) {
//                        ITmfTimeAligned tmfView = (ITmfTimeAligned) view;
//                        tmfView.realignTimeView();
//                        return;
//                    }
//                }
//            }
//
//            @Override
//            public void controlMoved(ControlEvent e) {
//            }
//        };
//        shell.addControlListener(listener);
//        fShellResizeListeners.put(shell, listener);
    }

    /**
     * @since 1.0
     */
    @TmfSignalHandler
    public void timeViewAlignementUpdatedInfo(TmfTimeViewAlignementSignal signal) {
        if (signal.isExecute() == false) {
            fTimeAlignmentThrottle.queue(new TmfTimeViewAlignementSignal(this, null, signal.getTimeAxisOffset(), true));
            //broadcast();
        }
    }

    /**
     * Method adds a pin action to the TmfView. The pin action allows to toggle the <code>fIsPinned</code> flag.
     * For example, this flag can be used to ignore time synchronization signals from other TmfViews.
     */
    protected void contributePinActionToToolBar() {
        if (fPinAction == null) {
            fPinAction = new PinTmfViewAction();

            IToolBarManager toolBarManager = getViewSite().getActionBars()
                    .getToolBarManager();
            toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            toolBarManager.add(fPinAction);
        }
    }
}
