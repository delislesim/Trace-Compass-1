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
    private final List<AlignmentOperation> fPendingOperations = Collections.synchronizedList(new ArrayList<AlignmentOperation>());

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
                        realignViews();
                    } else if (Boolean.toString(true).equals(oldValue) && Boolean.toString(false).equals(newValue)) {
                        restoreViews();
                    }
                }
            }
        };
        InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(listener);
        return listener;
    }

    private class AlignmentOperation {
        final TmfView fView;
        final TmfTimeViewAlignmentInfo fAlignmentInfo;

        public AlignmentOperation(TmfView view, TmfTimeViewAlignmentInfo timeViewAlignmentInfo) {
            fView = view;
            fAlignmentInfo = timeViewAlignmentInfo;
        }
    }

    /**
     * Queue the operation for processing. If an operation is considered the
     * same alignment (shell, location) as a previously queued one, it will
     * replace the old one. This way, only one up-to-date alignment operation is
     * kept per set of time-axis aligned views. The processing of the operation
     * is also throttled (TimerTask).
     *
     * @param operation
     *            the operation to queue
     */
    private void queue(AlignmentOperation operation) {
        synchronized(fPendingOperations) {
            fCurrentTask.cancel();
            for (AlignmentOperation pendingOperation : fPendingOperations) {
                if (isSameAlignment(operation, pendingOperation)) {
                    fPendingOperations.remove(pendingOperation);
                    break;
                }
            }
            fPendingOperations.add(operation);
            fCurrentTask = new AlignTask();
            fTimer.schedule(fCurrentTask, THROTTLE_DELAY);
        }
    }

    /**
     * Two operations are considered to be for the same set of time-axis aligned
     * views if they are on the same Shell and near the same location.
     */
    private static boolean isSameAlignment(AlignmentOperation operation1, AlignmentOperation operation2) {
        if (operation1.fView == operation2.fView) {
            return true;
        }

        if (operation1.fAlignmentInfo.getShell() != operation2.fAlignmentInfo.getShell()) {
            return false;
        }

        if (isViewLocationNear(getViewLocation(operation1.fView), getViewLocation(operation2.fView))) {
            return true;
        }

        return false;
    }

    private static Point getViewLocation(TmfView view) {
        return view.getParentComposite().toDisplay(0, 0);
    }

    private class AlignTask extends TimerTask {

        @Override
        public void run() {
            final List<AlignmentOperation> fcopy;
            synchronized (fPendingOperations) {
                fcopy = new ArrayList<>(fPendingOperations);
                fPendingOperations.clear();
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
    private static void performAllAlignments(final List<AlignmentOperation> alignments) {
        for (final AlignmentOperation info : alignments) {

            TmfView referenceView = info.fView;

            TmfTimeViewAlignmentInfo alignmentInfo = info.fAlignmentInfo;
            // The location of the view might have changed (resize, etc). Update the alignment info.
            alignmentInfo = new TmfTimeViewAlignmentInfo(alignmentInfo.getShell(), getViewLocation(referenceView), alignmentInfo.getTimeAxisOffset());

            TmfView narrowestView = getNarrowestView(alignmentInfo);
            if (narrowestView == null) {
                // No valid view found for this alignment. This could mean that the views for this alignment are now too narrow (width == 0).
                continue;
            }

            int narrowestWidth = ((ITmfTimeAligned) narrowestView).getAvailableWidth(alignmentInfo.getTimeAxisOffset());
            IViewReference[] viewReferences = referenceView.getSite().getPage().getViewReferences();
            for (IViewReference ref : viewReferences) {
                IViewPart view = ref.getView(false);
                if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                    TmfView tmfView = (TmfView) view;
                    ITmfTimeAligned alignedView = (ITmfTimeAligned) view;
                    if (isViewLocationNear(getViewLocation(tmfView), alignmentInfo.getViewLocation())) {
                        alignedView.performAlign(alignmentInfo.getTimeAxisOffset(), narrowestWidth);
                    }
                }
            }
        }
    }

    private static boolean isViewLocationNear(Point location1, Point location2) {
        return Math.abs(location1.x - location2.x) < NEAR_THRESHOLD;
    }

    /**
     * Handle a view that was just created.
     *
     * @param view
     *            the view that was created
     */
    public void handleViewCreated(TmfView view) {
        TmfTimeViewAlignmentInfo alignmentInfo = ((ITmfTimeAligned) view).getTimeViewAlignmentInfo();
        if (alignmentInfo == null) {
            return;
        }

        // Don't use a view that was just created as a reference view.
        // Otherwise, a view that was just
        // created might use itself as a reference but we want to
        // keep the existing alignment from the other views.
        ITmfTimeAligned referenceView = getReferenceView(alignmentInfo, view);
        if (referenceView != null) {
            queueAlignment(referenceView.getTimeViewAlignmentInfo());
        }
    }

    /**
     * Handle a view that was just resized.
     *
     * @param view
     *            the view that was resized
     */
    public void handleViewResized(TmfView view) {
        realignViews(view.getSite().getPage());
    }

    /**
     * Realign all views
     */
    private void realignViews() {
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                realignViews(page);
            }
        }
    }

    /**
     * Realign views inside a given page
     *
     * @param page
     *            the workbench page
     */
    private void realignViews(IWorkbenchPage page) {
        IViewReference[] viewReferences = page.getViewReferences();
        for (IViewReference ref : viewReferences) {
            IViewPart view = ref.getView(false);
            if (view instanceof TmfView && view instanceof ITmfTimeAligned) {
                queueAlignment(((ITmfTimeAligned) view).getTimeViewAlignmentInfo());
            }
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
                if (parentComposite != null && parentComposite.isVisible() && timeViewAlignmentInfo != null && isViewLocationNear(alignmentInfo.getViewLocation(), getViewLocation(tmfView))
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

            queue(new AlignmentOperation(view, timeViewAlignmentInfo));
        }
    }

    /**
     * Restore the views to their respective maximum widths
     */
    private static void restoreViews() {
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
