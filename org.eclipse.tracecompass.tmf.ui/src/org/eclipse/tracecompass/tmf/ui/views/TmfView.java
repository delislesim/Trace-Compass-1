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

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfUIPreferences;
import org.eclipse.tracecompass.internal.tmf.ui.views.AlignViewsAction;
import org.eclipse.tracecompass.internal.tmf.ui.views.TmfAlignThrottler;
import org.eclipse.tracecompass.tmf.core.component.ITmfComponent;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
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
    /** This allows us to keep track of the view sizes */
    private Composite fParentComposite;
    private static final TmfAlignThrottler fTimeAlignmentThrottle = new TmfAlignThrottler(null, 500);

    /**
     * Action class for pinning of TmfView.
     */
    protected PinTmfViewAction fPinAction;
    private static AlignViewsAction fAlignViewsAction;
    private IPreferenceChangeListener fAlignViewPrefListener;

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

        if (fAlignViewPrefListener != null) {
            InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).removePreferenceChangeListener(fAlignViewPrefListener);
            fAlignViewPrefListener = null;
        }

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

    @Override
    public void createPartControl(Composite parent) {
        fParentComposite = parent;
        if (this instanceof ITmfTimeAligned) {
            if (fAlignViewsAction == null) {
                fAlignViewsAction = new AlignViewsAction();
            }

            fAlignViewPrefListener = new IPreferenceChangeListener() {

                @Override
                public void preferenceChange(PreferenceChangeEvent event) {
                    if (event.getKey().equals(ITmfUIPreferences.PREF_ALIGN_VIEWS)) {
                        Object oldValue = event.getOldValue();
                        Object newValue = event.getNewValue();
                        if (Boolean.toString(false).equals(oldValue) && Boolean.toString(true).equals(newValue)) {
                            realignViews();
                        } else if (Boolean.toString(true).equals(oldValue) && Boolean.toString(false).equals(newValue)) {
                            // Reset views to their respective maximum widths.
                            // We set the width to Integer.MAX_VALUE so that the
                            // views remove any "filler" space they might have.
                            TmfTimeViewAlignmentInfo currentAlignmentInfo = ((ITmfTimeAligned) TmfView.this).getTimeViewAlignmentInfo();
                            TmfTimeViewAlignmentInfo newTimeViewAlignment = new TmfTimeViewAlignmentInfo(currentAlignmentInfo.getViewLocation(), currentAlignmentInfo.getTimeAxisOffset(), Integer.MAX_VALUE);
                            TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(this, newTimeViewAlignment));
                        }
                    }
                }
            };
            InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(fAlignViewPrefListener);

            IToolBarManager toolBarManager = getViewSite().getActionBars()
                    .getToolBarManager();
            toolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
            toolBarManager.add(fAlignViewsAction);

            parent.addControlListener(new ControlListener() {

                @Override
                public void controlResized(ControlEvent e) {
                    ITmfTimeAligned aligned = (ITmfTimeAligned)TmfView.this;
                    int availableWidth = aligned.getAvailableWidth(aligned.getTimeViewAlignmentInfo().getTimeAxisOffset());
                    System.out.println(getTitle() + " resized to " + getParentComposite().getSize().x + " available: " + availableWidth);
                    realignViews();
                }

                @Override
                public void controlMoved(ControlEvent e) {
                }
            });
            realignViews();
        }
    }

    private void realignViews() {
        TmfTimeViewAlignmentInfo curInfo = ((ITmfTimeAligned) this).getTimeViewAlignmentInfo();
        if (curInfo == null) {
            return;
        }

        // Look for a visible view that could be used as a reference to trigger the realign. Prioritize view with lowest offset because m
        // most of the interesting data should be in the time widget.
        int lowestOffset = Integer.MAX_VALUE;
        ITmfTimeAligned referenceView = null;
        IViewReference[] viewReferences = TmfView.this.getSite().getPage().getViewReferences();
        for (IViewReference ref : viewReferences) {
            IViewPart view = ref.getView(false);
            // Don't use self as reference view. Otherwise, a view that was just
            // opened might use itself as a reference but we want to
            // keep the existing alignment. This also has the nice side
            // effect of only aligning when there are more than one
            // ITmfTimeAligned.
            if (view != this && view instanceof TmfView && view instanceof ITmfTimeAligned) {
                TmfView tmfView = (TmfView) view;
                ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                Composite parentComposite = tmfView.getParentComposite();
                TmfTimeViewAlignmentInfo timeViewAlignmentInfo = alignedView.getTimeViewAlignmentInfo();
                if (parentComposite != null && parentComposite.isVisible() && timeViewAlignmentInfo != null && timeViewAlignmentInfo.isViewLocationNear(curInfo.getViewLocation())
                        && alignedView.getAvailableWidth(timeViewAlignmentInfo.getTimeAxisOffset()) > 0 && timeViewAlignmentInfo.getTimeAxisOffset() < lowestOffset) {
                    referenceView = (ITmfTimeAligned) view;
                    lowestOffset = timeViewAlignmentInfo.getTimeAxisOffset();
                    break;
                }
            }
        }
        if (referenceView != null) {
            TmfView tmfView = (TmfView) referenceView;
            Composite parentComposite = tmfView.getParentComposite();
            System.out.println("TmfView.realignViews() using reference view " + tmfView.getTitle() + " at " + referenceView.getTimeViewAlignmentInfo().getTimeAxisOffset() + " width:" + parentComposite.getSize().x + " available: " + referenceView.getAvailableWidth(referenceView.getTimeViewAlignmentInfo().getTimeAxisOffset()));
            timeViewAlignmentUpdatedInfo(referenceView.getTimeViewAlignmentInfo());
        } else {
            System.out.println("realign canceled, not enough visible views");
        }
    }

    /**
     * @since 1.0
     */
    public static TmfView getSmallestView(IWorkbenchPage page, Point viewLocation, int requestedOffset) {
        int smallestWidth = Integer.MAX_VALUE;
        TmfView smallest = null;
        IViewReference[] viewReferences = page.getViewReferences();
        for (IViewReference ref : viewReferences) {
            IViewPart view = ref.getView(false);
            if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                TmfView tmfView = (TmfView) view;
                ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                Composite parentComposite = tmfView.getParentComposite();
                TmfTimeViewAlignmentInfo timeViewAlignmentInfo = alignedView.getTimeViewAlignmentInfo();
                int availableWidth = alignedView.getAvailableWidth(requestedOffset);
                if (parentComposite != null && parentComposite.isVisible() && timeViewAlignmentInfo != null && timeViewAlignmentInfo.isViewLocationNear(viewLocation) && availableWidth < smallestWidth && availableWidth > 0) {
                    smallestWidth = availableWidth;
                    smallest = tmfView;
                }
            }

        }
        if (smallest != null) {
            System.out.println("smallest is " + smallest.getTitle() + " (" + smallestWidth + ")");
        }

        return smallest;
    }

    /**
     * @since 1.0
     */
    public Composite getParentComposite() {
        return fParentComposite;
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     */
    @TmfSignalHandler
    public void timeViewAlignmentUpdatedInfo(@SuppressWarnings("javadoc") TmfTimeViewAlignmentSignal signal) {
        TmfTimeViewAlignmentInfo timeViewAlignmentInfo = signal.getTimeViewAlignmentInfo();
        timeViewAlignmentUpdatedInfo(timeViewAlignmentInfo);
    }

    private void timeViewAlignmentUpdatedInfo(TmfTimeViewAlignmentInfo timeViewAlignmentInfo) {
        if (timeViewAlignmentInfo.isApply() == false && fAlignViewsAction.isChecked()) {
            TmfView view = getSmallestView(TmfView.this.getSite().getPage(), timeViewAlignmentInfo.getViewLocation(), timeViewAlignmentInfo.getTimeAxisOffset());
            TmfTimeViewAlignmentInfo timeViewAlignment = new TmfTimeViewAlignmentInfo(timeViewAlignmentInfo.getViewLocation(), timeViewAlignmentInfo.getTimeAxisOffset(), ((ITmfTimeAligned)view).getAvailableWidth(timeViewAlignmentInfo.getTimeAxisOffset()));
            fTimeAlignmentThrottle.queue(new TmfTimeViewAlignmentSignal(view, timeViewAlignment));
        }
    }
}
