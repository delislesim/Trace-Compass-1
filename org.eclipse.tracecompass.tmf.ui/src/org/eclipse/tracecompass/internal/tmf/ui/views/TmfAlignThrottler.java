package org.eclipse.tracecompass.internal.tmf.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.tmf.core.component.ITmfComponent;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;
import org.eclipse.tracecompass.tmf.ui.views.ITmfTimeAligned;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;

/**
 * @since 1.0
 */
public class TmfAlignThrottler {

    private final long fDelay;
    private final Timer fTimer;
    private TimerTask fCurrentTask;
    private List<TmfTimeViewAlignmentSignal> pendingAlignments = Collections.synchronizedList(new ArrayList<TmfTimeViewAlignmentSignal>());

    /**
     * Constructor
     *
     * @param component
     *            The source component of the signals
     * @param delay
     *            Time to wait before actually sending signals (in ms)
     */
    public TmfAlignThrottler(ITmfComponent component, long delay) {
        this.fDelay = delay;
        this.fTimer = new Timer();
        fCurrentTask = new TimerTask() { @Override public void run() {} };
    }

    public synchronized void queue(TmfTimeViewAlignmentSignal signal) {
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

    private boolean isSameAlignment(TmfTimeViewAlignmentSignal signal, TmfTimeViewAlignmentSignal pendingInfo) {
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
        private static final boolean DO_XY_CHART_HACK = true;

        @Override
        public void run() {
            final List<TmfTimeViewAlignmentSignal> copy;
            synchronized (TmfAlignThrottler.this) {
                copy = new ArrayList<>(pendingAlignments);
                pendingAlignments.clear();
            }
            System.out.println("Dispatching " + copy.size());
            if (DO_XY_CHART_HACK) {
                // FIXME HACK: recomputes the available width because some views
                // like CPU usage don't have their widgets resized on a
                // SWT.Resize. I.e. we don't know the new width of the chart
                // just after a SWT.Resize.
                Display.getDefault().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        Display.getDefault().timerExec(50, new Runnable() {

                            @Override
                            public void run() {
                                for (final TmfTimeViewAlignmentSignal info : copy) {
                                    TmfView view = TmfView.getSmallestView(((TmfView)info.getSource()).getSite().getPage(), info.getTimeViewAlignmentInfo().getViewLocation(), info.getTimeViewAlignmentInfo().getTimeAxisOffset());
                                    TmfTimeViewAlignmentInfo timeViewAlignment = new TmfTimeViewAlignmentInfo(info.getTimeViewAlignmentInfo().getViewLocation(), info.getTimeViewAlignmentInfo().getTimeAxisOffset(), ((ITmfTimeAligned)view).getAvailableWidth(info.getTimeViewAlignmentInfo().getTimeAxisOffset()));
                                    System.out.println("dispatchSignal offset: " + timeViewAlignment.getTimeAxisOffset()  + " width: " + timeViewAlignment.getWidth());
                                    TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(view, timeViewAlignment));
                                }

                            }
                        });
                    }
                });

            } else {
                for (final TmfTimeViewAlignmentSignal info : copy) {
                    System.out.println("dispatchSignal offset: " + info.getTimeViewAlignmentInfo().getTimeAxisOffset()  + " width: " + info.getTimeViewAlignmentInfo().getWidth());
                    TmfSignalManager.dispatchSignal(info);
                }
            }
        }
    }
    private static final int NEAR_THRESHOLD = 10;
    public boolean isViewLocationNear(Point location1, Point location2) {
        int distance = Math.abs(location1.x - location2.x);
        return distance < NEAR_THRESHOLD;
    }
}