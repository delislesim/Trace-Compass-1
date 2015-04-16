package org.eclipse.tracecompass.internal.tmf.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.tracecompass.tmf.core.component.ITmfComponent;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentInfo;
import org.eclipse.tracecompass.tmf.ui.signal.TmfTimeViewAlignmentSignal;

/**
 * @since 1.0
 */
public class TmfAlignThrottler {

    private final long fDelay;
    private final Timer fTimer;
    private TimerTask fCurrentTask;
    private List<TmfTimeViewAlignmentInfo> pendingAlignments = Collections.synchronizedList(new ArrayList<TmfTimeViewAlignmentInfo>());

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

    public synchronized void queue(TmfTimeViewAlignmentInfo info) {
        fCurrentTask.cancel();
        for (TmfTimeViewAlignmentInfo pendingInfo : pendingAlignments) {
            if (pendingInfo.isViewLocationNear(info.getViewLocation())) {
                pendingAlignments.remove(pendingInfo);
                break;
            }
        }
        pendingAlignments.add(info);
        fCurrentTask = new AlignRequest();
        fTimer.schedule(fCurrentTask, fDelay);
    }

    private class AlignRequest extends TimerTask {
        @Override
        public void run() {
            List<TmfTimeViewAlignmentInfo> copy;
            synchronized (TmfAlignThrottler.this) {
                copy = new ArrayList<>(pendingAlignments);
                pendingAlignments.clear();
            }
            for (TmfTimeViewAlignmentInfo info : copy) {
                TmfSignalManager.dispatchSignal(new TmfTimeViewAlignmentSignal(null, info));
            }
        }
    }
}