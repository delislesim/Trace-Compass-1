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

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceCompleteness;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.ui.handlers.HandlerUtil;

public class SyncRefreshAllHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        // Get the selection
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        final Iterator<Object> iterator = ((IStructuredSelection) selection).iterator();

        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (element instanceof TmfTraceElement) {
                TmfTraceElement trace = (TmfTraceElement) element;
                refreshTrace(trace);

            } else if (element instanceof TmfTraceFolder) {
                TmfTraceFolder tmfTraceFolder = (TmfTraceFolder) element;
                for (TmfTraceElement e : tmfTraceFolder.getTraces()) {
                    refreshTrace(e);
                }
            }
        }
        return null;
    }

    private static void refreshTrace(TmfTraceElement t) {
        final TmfTraceElement traceElement = t.getElementUnderTraceFolder();
//        trace.closeEditors();
//        trace.deleteSupplementaryFolder();
//        TmfOpenTraceHelper.openTraceFromElement(trace);
        ITmfTrace trace = traceElement.getTrace();
        if (trace instanceof ITmfTraceCompleteness) {
            ITmfTraceCompleteness complete = (ITmfTraceCompleteness) trace;
            complete.setComplete(false);
        }
    }

}
