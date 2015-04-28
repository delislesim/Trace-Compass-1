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

package org.eclipse.tracecompass.internal.tmf.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfUIPreferences;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.tracecompass.tmf.ui.views.ITmfTimeAligned;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * @since 1.0
 */
public class TmfAlignmentSynchronizer {

    private static final long fDelay = 500;
    private final Timer fTimer;
    private TimerTask fCurrentTask;
    private List<TmfTimeViewAlignmentSignal> pendingAlignments = Collections.synchronizedList(new ArrayList<TmfTimeViewAlignmentSignal>());
    private IPreferenceChangeListener fAlignViewPrefListener;

    /**
     * Constructor
     *
     */
    public TmfAlignmentSynchronizer() {
        TmfSignalManager.register(this);
        fTimer = new Timer();
        fAlignViewPrefListener = new IPreferenceChangeListener() {

            @Override
            public void preferenceChange(PreferenceChangeEvent event) {
                if (event.getKey().equals(ITmfUIPreferences.PREF_ALIGN_VIEWS)) {
                    Object oldValue = event.getOldValue();
                    Object newValue = event.getNewValue();
                    if (Boolean.toString(false).equals(oldValue) && Boolean.toString(true).equals(newValue)) {
                        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
                            for (IWorkbenchPage page : window.getPages()) {
                                IViewReference[] viewReferences = page.getViewReferences();
                                for (IViewReference ref : viewReferences) {
                                    IViewPart view = ref.getView(false);
                                    if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                                        realignViews((TmfView) view);
                                    }
                                }
                            }
                        }
                    } else if (Boolean.toString(true).equals(oldValue) && Boolean.toString(false).equals(newValue)) {
                        restoreViews();
                    }
                }
            }
        };
        InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(fAlignViewPrefListener);

        fCurrentTask = new TimerTask() { @Override public void run() {} };
    }

    private synchronized void queue(TmfTimeViewAlignmentSignal signal) {
        fCurrentTask.cancel();
        for (TmfTimeViewAlignmentSignal pendingSignal : pendingAlignments) {
            if (isSameAlignment(signal, pendingSignal)) {
                pendingAlignments.remove(pendingSignal);
                break;
            }
        }
        pendingAlignments.add(signal);
        fCurrentTask = new AlignRequest();
        fTimer.schedule(fCurrentTask, fDelay);
    }

    private static boolean isSameAlignment(TmfTimeViewAlignmentSignal signal, TmfTimeViewAlignmentSignal pendingInfo) {
        if (signal.getSource() == pendingInfo.getSource()) {
            return true;
        }

        if (!(signal.getSource() instanceof TmfView) || !(pendingInfo.getSource() instanceof TmfView)) {
            return false;
        }

        if (isViewLocationNear(getViewLocation(signal), getViewLocation(pendingInfo))) {
            return true;
        }

        return false;
    }

    private static Point getViewLocation(TmfTimeViewAlignmentSignal signal) {
        Point pendingLocation = ((TmfView) signal.getSource()).getParentComposite().toDisplay(0, 0);
        return pendingLocation;
    }

    private class AlignRequest extends TimerTask {

        @Override
        public void run() {
            final List<TmfTimeViewAlignmentSignal> fcopy;
            synchronized (TmfAlignmentSynchronizer.this) {
                fcopy = new ArrayList<>(pendingAlignments);
                pendingAlignments.clear();
            }
            System.out.println("Dispatching " + fcopy.size());
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    for (final TmfTimeViewAlignmentSignal info : fcopy) {

                        TmfTimeViewAlignmentInfo timeViewAlignmentInfo = info.getTimeViewAlignmentInfo();
                        int timeAxisOffset = timeViewAlignmentInfo.getTimeAxisOffset();
                        System.out.println();
                        TmfView aview = getSmallestView(((TmfView) info.getSource()).getSite().getPage(), getViewLocation(info), timeAxisOffset);
                        if (aview == null) {
                            // No valid view found for this alignment
                            continue;
                        }
                        ITmfTimeAligned iTmfTimeAligned = (ITmfTimeAligned) aview;
                        int smallestWidth = iTmfTimeAligned.getAvailableWidth(timeAxisOffset);
                        IViewReference[] viewReferences = ((TmfView) info.getSource()).getSite().getPage().getViewReferences();
                        for (IViewReference ref : viewReferences) {
                            IViewPart view = ref.getView(false);
                            if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                                TmfView tmfView = (TmfView) view;
                                ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                                if (isViewLocationNear(tmfView.getParentComposite().toDisplay(0, 0), aview.getParentComposite().toDisplay(0, 0))) {
                                    alignedView.performAlign(timeAxisOffset, smallestWidth);
                                }
                            }
                        }
                    }
                }
            });
        }
    }
    private static final int NEAR_THRESHOLD = 10;
    public static boolean isViewLocationNear(Point location1, Point location2) {
        int distance = Math.abs(location1.x - location2.x);
        return distance < NEAR_THRESHOLD;
    }


    public void realignViews(TmfView triggerView) {
        IWorkbenchPage page = triggerView.getSite().getPage();
        TmfTimeViewAlignmentInfo curInfo = ((ITmfTimeAligned) triggerView).getTimeViewAlignmentInfo();
        if (curInfo == null) {
            return;
        }

        // Look for a visible view that could be used as a reference to trigger the realign. Prioritize view with lowest offset because m
        // most of the interesting data should be in the time widget.
        int lowestOffset = Integer.MAX_VALUE;
        ITmfTimeAligned referenceView = null;
        IViewReference[] viewReferences = page.getViewReferences();
        for (IViewReference ref : viewReferences) {
            IViewPart view = ref.getView(false);
            // Don't use self as reference view. Otherwise, a view that was just
            // opened might use itself as a reference but we want to
            // keep the existing alignment. This also has the nice side
            // effect of only aligning when there are more than one
            // ITmfTimeAligned.
            if (view != triggerView && view instanceof TmfView && view instanceof ITmfTimeAligned) {
                TmfView tmfView = (TmfView) view;
                ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                Composite parentComposite = tmfView.getParentComposite();
                TmfTimeViewAlignmentInfo timeViewAlignmentInfo = alignedView.getTimeViewAlignmentInfo();
                if (parentComposite != null && parentComposite.isVisible() && timeViewAlignmentInfo != null && isViewLocationNear(triggerView.getParentComposite().toDisplay(0, 0), parentComposite.toDisplay(0, 0))
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
                if (parentComposite != null && parentComposite.isVisible() && timeViewAlignmentInfo != null && isViewLocationNear(parentComposite.toDisplay(0, 0), viewLocation) && availableWidth < smallestWidth && availableWidth > 0) {
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
     * @noreference This method is not intended to be referenced by clients.
     */
    @TmfSignalHandler
    public void timeViewAlignmentUpdatedInfo(@SuppressWarnings("javadoc") TmfTimeViewAlignmentSignal signal) {
        TmfTimeViewAlignmentInfo timeViewAlignmentInfo = signal.getTimeViewAlignmentInfo();
        timeViewAlignmentUpdatedInfo(timeViewAlignmentInfo);
    }

    private static IWorkbenchWindow getWorkbenchWindow(Shell shell) {
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            if (window.getShell().equals(shell)) {
                return window;
            }
        }

        return null;
    }

    private static boolean isAlignViewsPreferenceEnabled() {
        return InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).getBoolean(ITmfUIPreferences.PREF_ALIGN_VIEWS, true);
    }

    private void timeViewAlignmentUpdatedInfo(TmfTimeViewAlignmentInfo timeViewAlignmentInfo) {
        if (isAlignViewsPreferenceEnabled()) {
            IWorkbenchWindow workbenchWindow = getWorkbenchWindow(timeViewAlignmentInfo.getShell());
            if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
                // Only time aligned views that are part of a workbench window are supported
                return;
            }
            TmfView view = getSmallestView(workbenchWindow.getActivePage(), timeViewAlignmentInfo.getViewLocation(), timeViewAlignmentInfo.getTimeAxisOffset());
            if (view == null) {
                // No valid view found for this alignment
                return;
            }
            TmfTimeViewAlignmentInfo timeViewAlignment = new TmfTimeViewAlignmentInfo(timeViewAlignmentInfo.getShell(), timeViewAlignmentInfo.getViewLocation(), timeViewAlignmentInfo.getTimeAxisOffset());
            queue(new TmfTimeViewAlignmentSignal(view, timeViewAlignment));
        }
    }

    public void restoreViews() {
        // Reset views to their respective maximum widths.
        // We set the width to Integer.MAX_VALUE so that the
        // views remove any "filler" space they might have.

        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                IViewReference[] viewReferences = page.getViewReferences();
                for (IViewReference ref : viewReferences) {
                    IViewPart view = ref.getView(false);
                    if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                        ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                        alignedView.performAlign(alignedView.getTimeViewAlignmentInfo().getTimeAxisOffset(), Integer.MAX_VALUE);
                    }
                }
            }
        }
    }
}