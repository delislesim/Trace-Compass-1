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
 * Receives the various notifications for realignment and
 * performs the alignment on the appropriate views.
 *
 * @since 1.0
 */
public class TmfAlignmentSynchronizer {

    private static final long THROTTLE_DELAY = 500;
    private static final int NEAR_THRESHOLD = 10;
    private final Timer fTimer;
    private final List<TmfTimeViewAlignmentSignal> fPendingAlignments = Collections.synchronizedList(new ArrayList<TmfTimeViewAlignmentSignal>());

    private TimerTask fCurrentTask;

    /**
     * Constructor
     */
    public TmfAlignmentSynchronizer() {
        TmfSignalManager.register(this);
        fTimer = new Timer();
        createPreferenceListener();
        fCurrentTask = new TimerTask() { @Override public void run() {} };
    }

    private IPreferenceChangeListener createPreferenceListener() {
        IPreferenceChangeListener listener = new IPreferenceChangeListener() {

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
                                        queueAlignment(((ITmfTimeAligned) view).getTimeViewAlignmentInfo());
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
        InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(listener);
        return listener;
    }

    /**
     * Queue the signal for processing. If a signal is considered the same
     * alignment (shell, location) as a previously queued one, it will replace
     * the old one. This way, only one up-to-date alignment signal is kept per
     * set of time-axis aligned views and the signals. The processing of the
     * signals is also throttled (TimerTask).
     *
     * @param signal the signal to queue
     */
    private void queue(TmfTimeViewAlignmentSignal signal) {
        synchronized(fPendingAlignments) {
            fCurrentTask.cancel();
            for (TmfTimeViewAlignmentSignal pendingSignal : fPendingAlignments) {
                if (isSameAlignment(signal, pendingSignal)) {
                    fPendingAlignments.remove(pendingSignal);
                    break;
                }
            }
            fPendingAlignments.add(signal);
            fCurrentTask = new AlignTask();
            fTimer.schedule(fCurrentTask, THROTTLE_DELAY);
        }
    }

    /**
     * Two signals are considered to be for the same set of time-axis aligned
     * views if they are on the same Shell and near the same location.
     */
    private static boolean isSameAlignment(TmfTimeViewAlignmentSignal signal, TmfTimeViewAlignmentSignal pendingInfo) {
        if (signal.getSource() == pendingInfo.getSource()) {
            return true;
        }

        if (!(signal.getSource() instanceof TmfView) || !(pendingInfo.getSource() instanceof TmfView)) {
            return false;
        }

        if (signal.getTimeViewAlignmentInfo().getShell() != pendingInfo.getTimeViewAlignmentInfo().getShell()) {
            return false;
        }

        if (isViewLocationNear(getCurrentViewLocation((TmfView) signal.getSource()), getCurrentViewLocation((TmfView) pendingInfo.getSource()))) {
            return true;
        }

        return false;
    }

    private static Point getCurrentViewLocation(TmfView view) {
        Point pendingLocation = view.getParentComposite().toDisplay(0, 0);
        return pendingLocation;
    }

    private class AlignTask extends TimerTask {

        @Override
        public void run() {
            final List<TmfTimeViewAlignmentSignal> fcopy;
            synchronized (fPendingAlignments) {
                fcopy = new ArrayList<>(fPendingAlignments);
                fPendingAlignments.clear();
            }
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    performAllAlignments(fcopy);
                }
            });
        }
    }

    /**
     * Perform all alignment operations for the specified alignment
     * informations.
     *
     * <pre>
     * - The alignment algorithm chooses the narrowest width to accommodate all views.
     * - View positions are recomputed for extra accuracy since the views could have been moved or resized.
     * - Based on the up-to-date view positions, only views that are near and aligned with each other
     * </pre>
     */
    private static void performAllAlignments(final List<TmfTimeViewAlignmentSignal> alignments) {
        for (final TmfTimeViewAlignmentSignal info : alignments) {

            TmfView referenceView = (TmfView) info.getSource();

            TmfTimeViewAlignmentInfo timeViewAlignmentInfo = info.getTimeViewAlignmentInfo();
            // The location of the view might have changed (resize, etc). Update the alignment info.
            timeViewAlignmentInfo = new TmfTimeViewAlignmentInfo(info.getTimeViewAlignmentInfo().getShell(), getCurrentViewLocation(referenceView), timeViewAlignmentInfo.getTimeAxisOffset());

            TmfView narrowestView = getNarrowestView(timeViewAlignmentInfo);
            if (narrowestView == null) {
                // No valid view found for this alignment. This could mean that the views for this alignment are now too narrow (width == 0).
                continue;
            }

            int narrowestWidth = ((ITmfTimeAligned) narrowestView).getAvailableWidth(timeViewAlignmentInfo.getTimeAxisOffset());
            IViewReference[] viewReferences = referenceView.getSite().getPage().getViewReferences();
            for (IViewReference ref : viewReferences) {
                IViewPart view = ref.getView(false);
                if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                    TmfView tmfView = (TmfView) view;
                    ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                    if (isViewLocationNear(getCurrentViewLocation(tmfView), timeViewAlignmentInfo.getViewLocation())) {
                        alignedView.performAlign(timeViewAlignmentInfo.getTimeAxisOffset(), narrowestWidth);
                    }
                }
            }
        }
    }

    private static boolean isViewLocationNear(Point location1, Point location2) {
        return Math.abs(location1.x - location2.x) < NEAR_THRESHOLD;
    }

    //FIXME: Remove or clarify sementics
    public void realignViews(TmfView triggerView) {
        TmfTimeViewAlignmentInfo alignmentInfo = ((ITmfTimeAligned) triggerView).getTimeViewAlignmentInfo();
        if (alignmentInfo == null) {
            return;
        }
        // Don't use self as reference view. Otherwise, a view that was just
        // opened might use itself as a reference but we want to
        // keep the existing alignment. This also has the nice side
        // effect of only aligning when there are more than one
        // ITmfTimeAligned.
        ITmfTimeAligned referenceView = getReferenceView(alignmentInfo, triggerView);
        if (referenceView != null) {
            queueAlignment(referenceView.getTimeViewAlignmentInfo());
        }
    }

    /**
     * Get a view that corresponds to the alignment information. The view is
     * meant to be used as a "reference" for other views to align on. Heuristics
     * are applied to choose the best view. For example, the view has to be
     * visible. It also will prioritize the view with lowest time axis offset
     * because most of the interesting data should be in the time widget.
     *
     * @param alignmentInfo
     *            alignment information
     * @param blackListedView
     *            an optional black listed view that will not be used as
     *            reference (useful for a view that just got created)
     * @return the reference view
     */
    private static ITmfTimeAligned getReferenceView(TmfTimeViewAlignmentInfo alignmentInfo, TmfView blackListedView) {
        IWorkbenchPage page = getWorkbenchWindow(alignmentInfo.getShell()).getActivePage();

        int lowestTimeAxisOffset = Integer.MAX_VALUE;
        ITmfTimeAligned referenceView = null;
        for (IViewReference ref : page.getViewReferences()) {
            IViewPart view = ref.getView(false);
            if (view != blackListedView && view instanceof TmfView && view instanceof ITmfTimeAligned) {
                TmfView tmfView = (TmfView) view;
                ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                Composite parentComposite = tmfView.getParentComposite();
                TmfTimeViewAlignmentInfo timeViewAlignmentInfo = alignedView.getTimeViewAlignmentInfo();
                if (parentComposite != null && parentComposite.isVisible() && timeViewAlignmentInfo != null && isViewLocationNear(alignmentInfo.getViewLocation(), getCurrentViewLocation(tmfView))
                        && alignedView.getAvailableWidth(timeViewAlignmentInfo.getTimeAxisOffset()) > 0 && timeViewAlignmentInfo.getTimeAxisOffset() < lowestTimeAxisOffset) {
                    referenceView = (ITmfTimeAligned) view;
                    lowestTimeAxisOffset = timeViewAlignmentInfo.getTimeAxisOffset();
                    break;
                }
            }
        }
        return referenceView;
    }

    /**
     * Get the narrowest view that corresponds to the given alignment information.
     */
    private static TmfView getNarrowestView(TmfTimeViewAlignmentInfo alignmentInfo) {
        IWorkbenchPage page = getWorkbenchWindow(alignmentInfo.getShell()).getActivePage();

        int smallestWidth = Integer.MAX_VALUE;
        TmfView smallest = null;
        for (IViewReference ref : page.getViewReferences()) {
            IViewPart view = ref.getView(false);
            if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                TmfView tmfView = (TmfView) view;
                ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                Composite parentComposite = tmfView.getParentComposite();
                TmfTimeViewAlignmentInfo timeViewAlignmentInfo = alignedView.getTimeViewAlignmentInfo();
                int availableWidth = alignedView.getAvailableWidth(alignmentInfo.getTimeAxisOffset());
                if (parentComposite != null && parentComposite.isVisible() && timeViewAlignmentInfo != null && isViewLocationNear(parentComposite.toDisplay(0, 0), alignmentInfo.getViewLocation()) && availableWidth < smallestWidth && availableWidth > 0) {
                    smallestWidth = availableWidth;
                    smallest = tmfView;
                }
            }
        }

        return smallest;
    }

    /**
     * Process signal for alignment
     *
     * @param signal the alignment signal
     */
    @TmfSignalHandler
    public void timeViewAlignmentUpdated(TmfTimeViewAlignmentSignal signal) {
        queueAlignment(signal.getTimeViewAlignmentInfo());
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

    private void queueAlignment(TmfTimeViewAlignmentInfo timeViewAlignmentInfo) {
        if (isAlignViewsPreferenceEnabled()) {
            IWorkbenchWindow workbenchWindow = getWorkbenchWindow(timeViewAlignmentInfo.getShell());
            if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
                // Only time aligned views that are part of a workbench window are supported
                return;
            }

            // We need a view so that we can compute position right as we are
            // about to realign the views. The view could have been resized,
            // moved, etc.
            TmfView view = (TmfView) getReferenceView(timeViewAlignmentInfo, null);
            if (view == null) {
                // No valid view found for this alignment
                return;
            }

            queue(new TmfTimeViewAlignmentSignal(view, timeViewAlignmentInfo));
        }
    }

    /**
     * Restore the views to their respective maximum widths
     */
    public void restoreViews() {
        // We set the width to Integer.MAX_VALUE so that the
        // views remove any "filler" space they might have.
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference ref : page.getViewReferences()) {
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