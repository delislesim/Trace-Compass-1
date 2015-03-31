/*******************************************************************************
 * Copyright (c) 2014, 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 *   Marc-Andre Laperle - Added tests for extracting archives during import
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.ctf.ui.swtbot.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCheckBox;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotRadio;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace.ImportTraceWizard;
import org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace.ImportTraceWizardPage;
import org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace.Messages;
import org.eclipse.tracecompass.tmf.core.TmfCommonConstants;
import org.eclipse.tracecompass.tmf.ui.editors.TmfEventsEditor;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTraceFolder;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfTracesFolder;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * SWTBot Smoke test using ImportTraceWizard.
 *
 * @author Bernd Hufmann
 */
@RunWith(SWTBotJunit4ClassRunner.class)
public class StandardImportAndReadSmokeTest extends AbstractImportAndReadSmokeTest {

    private static final String TRACE_FOLDER_PARENT_PATH = fTrace.getPath() + File.separator + ".." + File.separator + ".." + File.separator;
    private static final String ARCHIVE_FILE_NAME = "synctraces.tar.gz";
    private static final String TRACE_ARCHIVE_PATH = TRACE_FOLDER_PARENT_PATH + ARCHIVE_FILE_NAME;
    private static final String TRACE_FOLDER_PARENT_NAME = "traces";
    private static final String TRACE_PROJECT_NAME = "Tracing";

    private static final String ARCHIVE_ROOT_ELEMENT_NAME = "/";
    private static final String GENERATED_ARCHIVE_NAME = "testtraces.zip";
    private static final String URI_SEPARATOR = "/";
    private static final String URI_FILE_SCHEME = "file:";
    private static final String URI_JAR_FILE_SCHEME = "jar:file:";

    /**
     * Test import from directory
     */
    @Test
    public void testImportFromDirectory() {
        testImport(0, false, false);
    }

    /**
     * Test import from directory, create links
     */
    @Test
    public void testImportFromDirectoryLinks() {
        testImport(ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE, false, false);
    }

    /**
     * Test import from directory, preserve folder structure
     */
    @Test
    public void testImportFromDirectoryPreserveFolder() {
        testImport(ImportTraceWizardPage.OPTION_PRESERVE_FOLDER_STRUCTURE, false, false);
    }

    /**
     * Test import from directory, create links, preserve folder structure
     */
    @Test
    public void testImportFromDirectoryLinksPreserveFolder() {
        int options = ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE | ImportTraceWizardPage.OPTION_PRESERVE_FOLDER_STRUCTURE;
        testImport(options, false, false);
    }

    /**
     * Test import from directory, overwrite all
     */
    @Test
    public void testImportFromDirectoryOverwrite() {
        testImport(0, false, false);
        testImport(ImportTraceWizardPage.OPTION_OVERWRITE_EXISTING_RESOURCES, false, false);
    }

    /**
     * Test import from archive
     */
    @Test
    public void testImportFromArchive() {
        testImport(ImportTraceWizardPage.OPTION_PRESERVE_FOLDER_STRUCTURE, true, true);
    }

    /**
     * Test import from directory, preserve folder structure
     */
    @Test
    public void testImportFromArchivePreserveFolder() {
        testImport(ImportTraceWizardPage.OPTION_PRESERVE_FOLDER_STRUCTURE, false, true);
    }

    /**
     * Test import from directory, overwrite all
     */
    @Test
    public void testImportFromArchiveOverwrite() {
        testImport(0, false, true);
        testImport(ImportTraceWizardPage.OPTION_OVERWRITE_EXISTING_RESOURCES, false, true);
    }

    /**
     * Test import from directory containing archives
     */
    @Test
    public void testExtractArchivesFromDirectory() {
        createProject();

        openImportWizard();
        selectImportFromDirectory(TRACE_FOLDER_PARENT_PATH);
        selectFile(ARCHIVE_FILE_NAME, TRACE_FOLDER_PARENT_NAME);

        int options = ImportTraceWizardPage.OPTION_OVERWRITE_EXISTING_RESOURCES;
        setOptions(options, ImportTraceWizardPage.TRACE_TYPE_AUTO_DETECT);
        importFinish();

        IPath expectedElementPath = new Path(TRACE_NAME);
        int expectedOptions = options & ~ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE;
        try {
            checkOptions(expectedOptions, false, URI_FILE_SCHEME + new File(TRACE_FOLDER_PARENT_PATH).getCanonicalPath() + File.separator + ARCHIVE_FILE_NAME + File.separator + TRACE_FOLDER + File.separator + TRACE_NAME + File.separator, expectedElementPath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        TmfEventsEditor editor = SWTBotUtils.openEditor(fBot, TRACE_PROJECT_NAME, expectedElementPath);
        testViews(editor);

        SWTBotUtils.deleteProject(getProjectName(), fBot);
    }

    /**
     * Test import from directory containing archives, create links
     */
    @Test
    public void testExtractArchivesFromDirectoryLinks() {
        createProject();

        openImportWizard();
        selectImportFromDirectory(TRACE_FOLDER_PARENT_PATH);
        selectFile(ARCHIVE_FILE_NAME, TRACE_FOLDER_PARENT_NAME);

        int options = ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE | ImportTraceWizardPage.OPTION_OVERWRITE_EXISTING_RESOURCES;
        setOptions(options, ImportTraceWizardPage.TRACE_TYPE_AUTO_DETECT);
        importFinish();

        IPath expectedElementPath = new Path(TRACE_NAME);
        int expectedOptions = options & ~ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE;
        try {
            checkOptions(expectedOptions, false, URI_FILE_SCHEME + new File(TRACE_FOLDER_PARENT_PATH).getCanonicalPath() + File.separator + ARCHIVE_FILE_NAME + File.separator + TRACE_FOLDER + File.separator + TRACE_NAME + File.separator, expectedElementPath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        TmfEventsEditor editor = SWTBotUtils.openEditor(fBot, TRACE_PROJECT_NAME, expectedElementPath);
        testViews(editor);

        SWTBotUtils.deleteProject(getProjectName(), fBot);
    }

    /**
     * Test import from directory containing archives, create links, preserve folder structure
     */
    @Test
    public void testExtractArchivesFromDirectoryLinksPreserveStruture() {
        createProject();

        openImportWizard();
        selectImportFromDirectory(TRACE_FOLDER_PARENT_PATH);
        selectFile(ARCHIVE_FILE_NAME, TRACE_FOLDER_PARENT_NAME);

        int options = ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE | ImportTraceWizardPage.OPTION_OVERWRITE_EXISTING_RESOURCES | ImportTraceWizardPage.OPTION_PRESERVE_FOLDER_STRUCTURE;
        setOptions(options, ImportTraceWizardPage.TRACE_TYPE_AUTO_DETECT);
        importFinish();

        IPath expectedElementPath = new Path(ARCHIVE_FILE_NAME).append(TRACE_FOLDER).append(TRACE_NAME);
        int expectedOptions = options & ~ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE;
        try {
            checkOptions(expectedOptions, false, URI_FILE_SCHEME + new File(TRACE_FOLDER_PARENT_PATH).getCanonicalPath() + File.separator + ARCHIVE_FILE_NAME + File.separator + TRACE_FOLDER + File.separator + TRACE_NAME + File.separator, expectedElementPath);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        TmfEventsEditor editor = SWTBotUtils.openEditor(fBot, TRACE_PROJECT_NAME, expectedElementPath);
        testViews(editor);

        SWTBotUtils.deleteProject(getProjectName(), fBot);
    }

    /**
     * Test import from archive containing archives
     *
     * @throws Exception
     *             on error
     */
    @Test
    public void testExtractArchivesFromArchive() throws Exception {
        createProject();
        final String TEST_ARCHIVE_PATH = createArchive();

        openImportWizard();
        selectImportFromArchive(TEST_ARCHIVE_PATH);
        selectFile(ARCHIVE_FILE_NAME, ARCHIVE_ROOT_ELEMENT_NAME, TRACE_PROJECT_NAME, TRACE_FOLDER_PARENT_NAME);

        int options = ImportTraceWizardPage.OPTION_OVERWRITE_EXISTING_RESOURCES;
        setOptions(options, ImportTraceWizardPage.TRACE_TYPE_AUTO_DETECT);
        importFinish();

        String expectedSourceLocation = URI_JAR_FILE_SCHEME + new File(TEST_ARCHIVE_PATH).getCanonicalFile().toString() + "!" + URI_SEPARATOR + TRACE_PROJECT_NAME + URI_SEPARATOR + TRACE_FOLDER_PARENT_NAME + URI_SEPARATOR + ARCHIVE_FILE_NAME + URI_SEPARATOR
              + TRACE_FOLDER + URI_SEPARATOR + TRACE_NAME + URI_SEPARATOR;
        Path expectedElementPath = new Path(TRACE_NAME);
        checkOptions(options, true, expectedSourceLocation, expectedElementPath);

        TmfEventsEditor editor = SWTBotUtils.openEditor(fBot, TRACE_PROJECT_NAME, expectedElementPath);
        testViews(editor);

        SWTBotUtils.deleteProject(getProjectName(), fBot);
    }

    /**
     * Test import from archive containing archives, preserve folder structure
     *
     * @throws Exception
     *             on error
     */
    @Test
    public void testExtractArchivesFromArchivePreserveFolder() throws Exception {
        createProject();

        final String TEST_ARCHIVE_PATH = createArchive();

        openImportWizard();

        selectImportFromArchive(TEST_ARCHIVE_PATH);
        selectFile(ARCHIVE_FILE_NAME, ARCHIVE_ROOT_ELEMENT_NAME, TRACE_PROJECT_NAME, TRACE_FOLDER_PARENT_NAME);

        int options = ImportTraceWizardPage.OPTION_OVERWRITE_EXISTING_RESOURCES | ImportTraceWizardPage.OPTION_PRESERVE_FOLDER_STRUCTURE;
        setOptions(options, ImportTraceWizardPage.TRACE_TYPE_AUTO_DETECT);
        importFinish();
        String expectedSourceLocation = URI_JAR_FILE_SCHEME + new File(TEST_ARCHIVE_PATH).getCanonicalFile().toString() + "!" + URI_SEPARATOR + TRACE_PROJECT_NAME + URI_SEPARATOR + TRACE_FOLDER_PARENT_NAME + URI_SEPARATOR + ARCHIVE_FILE_NAME + URI_SEPARATOR
                + TRACE_FOLDER + URI_SEPARATOR + TRACE_NAME + URI_SEPARATOR;
        IPath expectedElementPath = new Path(TRACE_PROJECT_NAME).append(TRACE_FOLDER_PARENT_NAME).append(ARCHIVE_FILE_NAME).append(TRACE_FOLDER).append(TRACE_NAME);
        checkOptions(options, true, expectedSourceLocation, expectedElementPath);

        TmfEventsEditor editor = SWTBotUtils.openEditor(fBot, TRACE_PROJECT_NAME, expectedElementPath);
        testViews(editor);


        SWTBotUtils.deleteProject(getProjectName(), fBot);
    }

    private IProject getProjectResource() {
        return ResourcesPlugin.getWorkspace().getRoot().getProject(getProjectName());
    }

    private String createArchive() throws URISyntaxException, CoreException, IOException {

        // Link to the test traces folder. We use a link so that we can safely
        // delete the entire project when we are done.
        IProject project = getProjectResource();
        String canonicalPath = new File(TRACE_FOLDER_PARENT_PATH).getCanonicalPath();
        IFolder folder = project.getFolder(TRACE_FOLDER_PARENT_NAME);
        folder.createLink(new Path(canonicalPath), IResource.REPLACE, null);

        SWTBotTreeItem traceFilesProject = SWTBotUtils.selectProject(fBot, TRACE_PROJECT_NAME);
        traceFilesProject.contextMenu("Export...").click();

        fBot.waitUntil(Conditions.shellIsActive("Export"));
        SWTBotShell activeShell = fBot.activeShell();
        SWTBotTree exportWizardsTree = fBot.tree();
        SWTBotTreeItem treeItem = SWTBotUtils.getTreeItem(fBot, exportWizardsTree, "General", "Archive File");
        treeItem.select();
        fBot.button("Next >").click();
        fBot.button("&Deselect All").click();
        selectFile(ARCHIVE_FILE_NAME, TRACE_PROJECT_NAME, TRACE_FOLDER_PARENT_NAME);

        String workspacePath = URIUtil.toFile(URIUtil.fromString(System.getProperty("osgi.instance.area"))).getAbsolutePath();
        final String archiveDestinationPath = workspacePath + File.separator + TRACE_PROJECT_NAME + File.separator + GENERATED_ARCHIVE_NAME;
        fBot.comboBox().setText(archiveDestinationPath);
        fBot.button("&Finish").click();
        fBot.waitUntil(Conditions.shellCloses(activeShell));
        return archiveDestinationPath;
    }

    private void testImport(int options, boolean testViews, boolean fromArchive) {
        createProject();
        String expectedSourceLocation = null;
        openImportWizard();
        if (fromArchive) {
            try {
                expectedSourceLocation = URI_JAR_FILE_SCHEME + new File(TRACE_ARCHIVE_PATH).getCanonicalFile().toString() + "!" + URI_SEPARATOR + TRACE_FOLDER + URI_SEPARATOR + TRACE_NAME + URI_SEPARATOR;
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
            selectImportFromArchive(TRACE_ARCHIVE_PATH);
            selectFolder(ARCHIVE_ROOT_ELEMENT_NAME);
            SWTBotCheckBox checkBox = fBot.checkBox(Messages.ImportTraceWizard_CreateLinksInWorkspace);
            assertFalse(checkBox.isEnabled());
        } else {
            String sourcePath = TRACE_FOLDER_PARENT_PATH + File.separator + TRACE_FOLDER + File.separator + TRACE_NAME;
            try {
                expectedSourceLocation = URI_FILE_SCHEME + new File(sourcePath).getCanonicalFile().toString() + URI_SEPARATOR;
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
            selectImportFromDirectory(TRACE_FOLDER_PARENT_PATH);
            selectFolder(new String [] {TRACE_FOLDER_PARENT_NAME, TRACE_FOLDER });
        }

        setOptions(options, ImportTraceWizardPage.TRACE_TYPE_AUTO_DETECT);
        importFinish();

        IPath expectedElementPath = getTraceElementPath(options);
        checkOptions(options, fromArchive, expectedSourceLocation, expectedElementPath);
        TmfEventsEditor tmfEd = SWTBotUtils.openEditor(fBot, getProjectName(), expectedElementPath);
        if (testViews) {
            testViews(tmfEd);
        }

        fBot.closeAllEditors();

        SWTBotUtils.deleteProject(getProjectName(), fBot);
    }

    private void testViews(TmfEventsEditor editor) {
        testHistogramView(getViewPart("Histogram"), editor);
        testPropertyView(getViewPart("Properties"));
        testStatisticsView(getViewPart("Statistics"));
    }

    private static void openImportWizard() {
        fWizard = new ImportTraceWizard();

        UIThreadRunnable.asyncExec(new VoidResult() {
            @Override
            public void run() {
                final IWorkbench workbench = PlatformUI.getWorkbench();
                // Fire the Import Trace Wizard
                if (workbench != null) {
                    final IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
                    Shell shell = activeWorkbenchWindow.getShell();
                    assertNotNull(shell);
                    ((ImportTraceWizard) fWizard).init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
                    WizardDialog dialog = new WizardDialog(shell, fWizard);
                    dialog.open();
                }
            }
        });

        fBot.waitUntil(ConditionHelpers.isWizardReady(fWizard));
    }

    private static void selectImportFromDirectory(String directoryPath) {
        SWTBotRadio button = fBot.radio("Select roo&t directory:");
        button.click();

        SWTBotCombo sourceCombo = fBot.comboBox();
        File traceFolderParent = new File(directoryPath);
        sourceCombo.setText(traceFolderParent.getAbsolutePath());

        SWTBotText text = fBot.text();
        text.setFocus();
    }

    private static void selectImportFromArchive(String archivePath) {
        SWTBotRadio button = fBot.radio("Select &archive file:");
        button.click();

        SWTBotCombo sourceCombo = fBot.comboBox(1);

        sourceCombo.setText(new File(archivePath).getAbsolutePath());

        SWTBotText text = fBot.text();
        text.setFocus();
    }

    private static void selectFolder(String... treePath) {
        SWTBotTree tree = fBot.tree();
        fBot.waitUntil(Conditions.widgetIsEnabled(tree));
        SWTBotTreeItem folderNode = SWTBotUtils.getTreeItem(fBot, tree, treePath);
        folderNode.check();
    }

    private static void selectFile(String fileName, String... folderTreePath) {
        SWTBotTree folderTree = fBot.tree();
        fBot.waitUntil(Conditions.widgetIsEnabled(folderTree));
        SWTBotTreeItem folderNode = SWTBotUtils.getTreeItem(fBot, folderTree, folderTreePath);
        folderNode.select();

        SWTBotTable fileTable = fBot.table();
        fBot.waitUntil(Conditions.widgetIsEnabled(fileTable));
        fBot.waitUntil(ConditionHelpers.isTableItemAvailable(fileName, fileTable));
        SWTBotTableItem tableItem = fileTable.getTableItem(fileName);
        tableItem.check();
    }

    private static void setOptions(int optionFlags, String traceTypeName) {
        SWTBotCheckBox checkBox = fBot.checkBox(Messages.ImportTraceWizard_CreateLinksInWorkspace);
        if (checkBox.isEnabled()) {
            if ((optionFlags & ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE) != 0) {
                checkBox.select();
            } else {
                checkBox.deselect();
            }
        }

        checkBox = fBot.checkBox(Messages.ImportTraceWizard_PreserveFolderStructure);
        if ((optionFlags & ImportTraceWizardPage.OPTION_PRESERVE_FOLDER_STRUCTURE) != 0) {
            checkBox.select();
        } else {
            checkBox.deselect();
        }

        checkBox = fBot.checkBox(Messages.ImportTraceWizard_ImportUnrecognized);
        if ((optionFlags & ImportTraceWizardPage.OPTION_IMPORT_UNRECOGNIZED_TRACES) != 0) {
            checkBox.select();
        } else {
            checkBox.deselect();
        }

        checkBox = fBot.checkBox(Messages.ImportTraceWizard_OverwriteExistingTrace);
        if ((optionFlags & ImportTraceWizardPage.OPTION_OVERWRITE_EXISTING_RESOURCES) != 0) {
            checkBox.select();
        } else {
            checkBox.deselect();
        }

        SWTBotCombo comboBox = fBot.comboBoxWithLabel(Messages.ImportTraceWizard_TraceType);
        if (traceTypeName != null && !traceTypeName.isEmpty()) {
            comboBox.setSelection(traceTypeName);
        } else {
            comboBox.setSelection(ImportTraceWizardPage.TRACE_TYPE_AUTO_DETECT);
        }
    }

    private void checkOptions(int optionFlags, boolean fromArchive, String expectedSourceLocation, IPath expectedElementPath) {
        IProject project = getProjectResource();
        assertTrue(project.exists());
        TmfProjectElement tmfProject = TmfProjectRegistry.getProject(project, true);
        assertNotNull(tmfProject);
        TmfTraceFolder tracesFolder = tmfProject.getTracesFolder();
        assertNotNull(tracesFolder);
        List<TmfTraceElement> traces = tracesFolder.getTraces();
        assertFalse(traces.isEmpty());
        Collections.sort(traces, new Comparator<TmfTraceElement>() {
            @Override
            public int compare(TmfTraceElement arg0, TmfTraceElement arg1) {
                return arg0.getElementPath().compareTo(arg1.getElementPath());
            }
        });

        TmfTraceElement tmfTraceElement = traces.get(0);
        IResource traceResource = tmfTraceElement.getResource();

        assertEquals((optionFlags & ImportTraceWizardPage.OPTION_CREATE_LINKS_IN_WORKSPACE) != 0, traceResource.isLinked());

        // i.e. /Tracing/Traces
        IPath expectedPath = Path.ROOT.append(new Path(TRACE_PROJECT_NAME)).append(TmfTracesFolder.TRACES_FOLDER_NAME);
        expectedPath = expectedPath.append(expectedElementPath);
        assertEquals(expectedPath, traceResource.getFullPath());

        String sourceLocation = null;
        try {
            sourceLocation = traceResource.getPersistentProperty(TmfCommonConstants.SOURCE_LOCATION);
        } catch (CoreException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        assertNotNull(sourceLocation);
        assertEquals(expectedSourceLocation, sourceLocation);
    }

    private static IPath getTraceElementPath(int optionFlags) {
        IPath traceElementPath = new Path("");
        if ((optionFlags & ImportTraceWizardPage.OPTION_PRESERVE_FOLDER_STRUCTURE) != 0) {
            traceElementPath = traceElementPath.append(TRACE_FOLDER);
        }
        return traceElementPath.append(TRACE_NAME);
    }

    @Override
    protected String getProjectName() {
        return TRACE_PROJECT_NAME;
    }
}
