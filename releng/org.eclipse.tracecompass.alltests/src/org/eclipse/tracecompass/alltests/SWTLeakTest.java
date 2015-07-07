/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *   Marc-Andre Laperle
 *   Patrick Tasse - Add support for folder elements
 *******************************************************************************/

package org.eclipse.tracecompass.alltests;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.controlflow.ControlFlowView;
import org.eclipse.tracecompass.analysis.os.linux.ui.views.resources.ResourcesView;
import org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace.ImportTraceWizard;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ui.editors.TmfEventsEditor;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.LeakRunner;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.views.histogram.HistogramView;
import org.eclipse.tracecompass.tmf.ui.views.statistics.TmfStatisticsView;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Smoke test for LTTng Kernel UI.
 *
 * @author Matthew Khouzam
 */
@RunWith(SWTBotJunit4ClassRunner.class)
@SuppressWarnings("restriction")
public class SWTLeakTest {

    private static final String TRACE_TYPE = "org.eclipse.linuxtools.lttng2.kernel.tracetype";
    private static final String KERNEL_PERSPECTIVE_ID = "org.eclipse.linuxtools.lttng2.kernel.ui.perspective";
    private static final String TRACE_PROJECT_NAME = "test";
    private static final CtfTmfTestTrace CTT = CtfTmfTestTrace.SYNTHETIC_TRACE;

    private static SWTWorkbenchBot fBot;
    private ITmfEvent fDesired1;
    private ITmfEvent fDesired2;

    /** The Log4j logger instance. */
    private static final Logger fLogger = Logger.getRootLogger();

    /**
     * Test Class setup
     */
    @BeforeClass
    public static void init() {
        SWTBotUtils.failIfUIThread();

        /* set up for swtbot */
        SWTBotPreferences.TIMEOUT = 20000; /* 20 second timeout */
        fLogger.addAppender(new NullAppender());
        fBot = new SWTWorkbenchBot();

        final List<SWTBotView> openViews = fBot.views();
        for (SWTBotView view : openViews) {
            if (view.getTitle().equals("Welcome")) {
                view.close();
                fBot.waitUntil(ConditionHelpers.ViewIsClosed(view));
            }
        }
        /* Switch perspectives */
        SWTBotUtils.switchToPerspective(KERNEL_PERSPECTIVE_ID);
        /* Finish waiting for eclipse to load */
        SWTBotUtils.waitForJobs();
    }

    /**
     * Tear down class
     */
    @AfterClass
    public static void afterClass() {
        // Print used memory. To help track if we're close to running out since
        // this test might run many times during soak testing.
        Runtime runtime = Runtime.getRuntime();
        final int mb = 1024 * 1024;
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mb;
        System.out.println("Used Memory:" + usedMemory);
    }

    /**
     * Main test case
     */
    @Test
    public void test() {
        new LeakRunner() {
            @Override
            protected void run() {
                SWTBotUtils.createProject(TRACE_PROJECT_NAME);
                SWTBotUtils.openTrace(TRACE_PROJECT_NAME, CTT.getPath(), TRACE_TYPE);
                openEditor();

                final SWTBotTable tableBot = fBot.activeEditor().bot().table();
                tableBot.getTableItem(1).click();

                tableBot.contextMenu("Copy to Clipboard").click();
                testHV();

                fBot.closeAllEditors();
                SWTBotUtils.deleteProject(TRACE_PROJECT_NAME, fBot);
            }
        }.run();
    }

    /**
     * Test Histogram
     */
    @Test
    public void testHistogram() {
        testView(HistogramView.ID);
    }

    /**
     * Test Control Flow view
     */
    @Test
    @Ignore
    public void testControlFlowView() {
        testView(ControlFlowView.ID);
    }

    /**
     * Test Resources view
     */
    @Test
    @Ignore
    public void testResourcesView() {
        testView(ResourcesView.ID);
    }

    /**
     * Test Statistics view
     */
    @Test
    public void testStatisticsView() {
        testView(TmfStatisticsView.ID);
    }

    /**
     * Test project wizard
     */
    @Test
    public void testProjectWizard() {
        LeakRunner r = new LeakRunner() {
            @Override
            protected void run() {
                SWTBotUtils.createProjectFromUI(TRACE_PROJECT_NAME, fBot);
                SWTBotUtils.deleteProject(TRACE_PROJECT_NAME, fBot);
            }
        };
        r.start();
    }

    /**
     * Test project wizard
     */
    @Test
    public void testImportWizard() {
        LeakRunner r = new LeakRunner() {
            @Override
            protected void run() {
                SWTBotUtils.createProject(TRACE_PROJECT_NAME);
                final ImportTraceWizard wizard = new ImportTraceWizard();

                UIThreadRunnable.asyncExec(new VoidResult() {
                    @Override
                    public void run() {
                        final IWorkbench workbench = PlatformUI.getWorkbench();
                        // Fire the Import Trace Wizard
                        if (workbench != null) {
                            final IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
                            Shell shell = activeWorkbenchWindow.getShell();
                            assertNotNull(shell);
                            wizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
                            WizardDialog dialog = new WizardDialog(shell, wizard);
                            dialog.open();
                        }
                    }
                });

                fBot.waitUntil(ConditionHelpers.isWizardReady(wizard));

                SWTBotRadio button = fBot.radio("Select roo&t directory:");
                button.click();

                SWTBotCombo sourceCombo = fBot.comboBox();
                final String TRACE_FOLDER_PARENT_PATH = CtfTmfTestTrace.SYNC_DEST.getPath() + File.separator + ".." + File.separator + ".." + File.separator;
                File traceFolderParent = new File(TRACE_FOLDER_PARENT_PATH);
                sourceCombo.setText(traceFolderParent.getAbsolutePath());
                SWTBotText text = fBot.text();
                text.setFocus();

                SWTBotShell shell = fBot.activeShell();
                final SWTBotButton finishButton = fBot.button("Cancel");
                finishButton.click();
                fBot.waitUntil(Conditions.shellCloses(shell));
                SWTBotUtils.deleteProject(TRACE_PROJECT_NAME, fBot);
            }
        };
        r.start();
    }

    private void testView(final String viewId) {
        SWTBotUtils.createProject(TRACE_PROJECT_NAME);
        SWTBotUtils.openTrace(TRACE_PROJECT_NAME, CTT.getPath(), TRACE_TYPE);
        openEditor();

        LeakRunner r = new LeakRunner() {

            @Override
            protected void run() {
                SWTBotUtils.openView(viewId);
                SWTBotUtils.delay(2000);
                SWTBotUtils.closeViewById(viewId, fBot);
            }

        };
        r.start();

        fBot.closeAllEditors();
        SWTBotUtils.deleteProject(TRACE_PROJECT_NAME, fBot);
    }

    private void openEditor() {
        Matcher<IEditorReference> matcher = WidgetMatcherFactory.withPartName(CTT.getTrace().getName());
        IEditorPart iep = fBot.editor(matcher).getReference().getEditor(true);
        fDesired1 = getEvent(100);
        fDesired2 = getEvent(10000);
        final TmfEventsEditor tmfEd = (TmfEventsEditor) iep;
        UIThreadRunnable.syncExec(new VoidResult() {
            @Override
            public void run() {
                tmfEd.setFocus();
                tmfEd.selectionChanged(new SelectionChangedEvent(tmfEd, new StructuredSelection(fDesired1)));
            }
        });

        SWTBotUtils.waitForJobs();
        SWTBotUtils.delay(1000);
        assertNotNull(tmfEd);
    }

    private void testHV() {
        SWTBotUtils.openView(HistogramView.ID);
        SWTBotView hvBot = (new SWTWorkbenchBot()).viewById(HistogramView.ID);
        IWorkbenchPart part = hvBot.getViewReference().getPart(false);
        List<SWTBotToolbarButton> hvTools = hvBot.getToolbarButtons();
        for (SWTBotToolbarButton hvTool : hvTools) {
            if (hvTool.getToolTipText().toLowerCase().contains("lost")) {
                hvTool.click();
            }
        }
        HistogramView hv = (HistogramView) part;
        final TmfSelectionRangeUpdatedSignal signal = new TmfSelectionRangeUpdatedSignal(hv, fDesired1.getTimestamp());
        final TmfSelectionRangeUpdatedSignal signal2 = new TmfSelectionRangeUpdatedSignal(hv, fDesired2.getTimestamp());
        hv.updateTimeRange(100000);
        SWTBotUtils.waitForJobs();
        hv.selectionRangeUpdated(signal);
        hv.broadcast(signal);
        SWTBotUtils.waitForJobs();
        SWTBotUtils.delay(1000);

        hv.updateTimeRange(1000000000);
        SWTBotUtils.waitForJobs();
        hv.selectionRangeUpdated(signal2);
        hv.broadcast(signal2);
        SWTBotUtils.waitForJobs();
        SWTBotUtils.delay(1000);
        assertNotNull(hv);
    }

    private static CtfTmfEvent getEvent(int rank) {
        try (CtfTmfTrace trace = CtfTmfTestTrace.SYNTHETIC_TRACE.getTrace()) {
            ITmfContext ctx = trace.seekEvent(0);
            for (int i = 0; i < rank; i++) {
                trace.getNext(ctx);
            }
            return trace.getNext(ctx);
        }

    }
}
