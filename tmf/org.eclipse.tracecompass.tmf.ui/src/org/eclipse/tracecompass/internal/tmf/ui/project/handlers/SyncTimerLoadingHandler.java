/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial API and implementation.
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.ui.project.handlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.tmf.ui.project.dialogs.SyncTimeLoadingSelectionDialog;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceCompleteness;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class SyncTimerLoadingHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return null;
        }

        // Get the selection
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        final Iterator<Object> iterator = ((IStructuredSelection) selection).iterator();

        final List<TmfTraceElement> elements = new ArrayList<>();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (element instanceof TmfTraceElement) {
                TmfTraceElement trace = (TmfTraceElement) element;
                elements.add(trace);

            } else if (element instanceof TmfTraceFolder) {
                TmfTraceFolder tmfTraceFolder = (TmfTraceFolder) element;
                for (TmfTraceElement e : tmfTraceFolder.getTraces()) {
                    elements.add(e);
                }
            }
        }

        if (elements.isEmpty()) {
            return null;
        }

        SyncTimeLoadingSelectionDialog dialog = new SyncTimeLoadingSelectionDialog(window.getShell());
        dialog.open();
        if (dialog.getReturnCode() == Window.OK) {
            // TODO: do stuff with dialog.getResult();
            // new Object[] { fTimerText.getText(), fRefreshAllButton.getSelection() }
            Object[] result = dialog.getResult();
            final int timer = (int) result[0];
            boolean refreshAll = (boolean) result[1];
            if (!refreshAll) {
                throw new UnsupportedOperationException();
            }

            Display.getDefault().timerExec(timer, new TimerLoading(elements, timer));
        }
        return null;
    }

    private static class TimerLoading implements Runnable {

        private List<TmfTraceElement> fElements;
        private int fTimer;

        private TimerLoading(List<TmfTraceElement> elements, int timer) {
            fElements = elements;
            fTimer = timer;

        }

        @Override
        public void run() {
            ITmfTrace trace = fElements.get(0).getTrace();
            if (trace instanceof ITmfTraceCompleteness) {
                ITmfTraceCompleteness complete = (ITmfTraceCompleteness) trace;
                complete.setComplete(false);
                Display.getDefault().timerExec(fTimer, this);
            }
        }
    }
}
