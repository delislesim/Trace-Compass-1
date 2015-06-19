/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc-Andre Laperle - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.TarException;
import org.eclipse.ui.internal.wizards.datatransfer.TarFile;
import org.eclipse.ui.internal.wizards.datatransfer.TarLeveledStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;

@SuppressWarnings({"restriction" })
public class Util {

    @SuppressWarnings("resource")
    public static Pair<IFileSystemObject, FileSystemObjectImportStructureProvider> getRootObjectAndProvider(File sourceFile, Shell shell) {
        if (sourceFile == null) {
            return null;
        }

        IFileSystemObject rootElement = null;
        FileSystemObjectImportStructureProvider importStructureProvider = null;

        // Import from directory
        if (!isArchiveFile(sourceFile)) {
            importStructureProvider = new FileSystemObjectImportStructureProvider(FileSystemStructureProvider.INSTANCE, null);
            rootElement = importStructureProvider.getIFileSystemObject(sourceFile);
        } else {
            // Import from archive
            FileSystemObjectLeveledImportStructureProvider leveledImportStructureProvider = null;
            String archivePath = sourceFile.getAbsolutePath();
            if (isTarFile(archivePath)) {
                if (ensureTarSourceIsValid(archivePath, shell)) {
                    // We close the file when we dispose the import provider,
                    // see disposeSelectionGroupRoot
                    TarFile tarFile = getSpecifiedTarSourceFile(archivePath);
                    leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new TarLeveledStructureProvider(tarFile), archivePath);
                }
            } else if (ensureZipSourceIsValid(archivePath, shell)) {
                // We close the file when we dispose the import provider, see
                // disposeSelectionGroupRoot
                ZipFile zipFile = getSpecifiedZipSourceFile(archivePath);
                leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new ZipLeveledStructureProvider(zipFile), archivePath);
            } else if (ensureGzipSourceIsValid(archivePath)) {
                // We close the file when we dispose the import provider, see
                // disposeSelectionGroupRoot
                GzipFile zipFile = null;
                try {
                    zipFile = new GzipFile(archivePath);
                    leveledImportStructureProvider = new FileSystemObjectLeveledImportStructureProvider(new GzipLeveledStructureProvider(zipFile), archivePath);
                } catch (IOException e) {
                    // do nothing
                }
            }
            if (leveledImportStructureProvider == null) {
                return null;
            }
            rootElement = leveledImportStructureProvider.getRoot();
            importStructureProvider = leveledImportStructureProvider;
        }

        if (rootElement == null) {
            return null;
        }

        return new Pair<>(rootElement, importStructureProvider);
    }

    public static TraceFileSystemElement createRootTraceFileElement(IFileSystemObject element,
            FileSystemObjectImportStructureProvider provider) {
        boolean isContainer = provider.isFolder(element);
        String elementLabel = provider.getLabel(element);

        // Use an empty label so that display of the element's full name
        // doesn't include a confusing label
        TraceFileSystemElement dummyParent = new TraceFileSystemElement("", null, true, provider);//$NON-NLS-1$
        Object dummyParentFileSystemObject = element;
        Object rawFileSystemObject = element.getRawFileSystemObject();
        if (rawFileSystemObject instanceof File) {
            dummyParentFileSystemObject = provider.getIFileSystemObject(((File) rawFileSystemObject).getParentFile());
        }
        dummyParent.setFileSystemObject(dummyParentFileSystemObject);
        dummyParent.setPopulated();
        TraceFileSystemElement result = new TraceFileSystemElement(
                elementLabel, dummyParent, isContainer, provider);
        result.setFileSystemObject(element);

        // Get the files for the element so as to build the first level
        result.getFiles();

        return dummyParent;
    }

    public static boolean isArchiveFile(File sourceFile) {
        String absolutePath = sourceFile.getAbsolutePath();
        return isTarFile(absolutePath) || ArchiveFileManipulations.isZipFile(absolutePath) || isGzipFile(absolutePath);
    }

    private static boolean isTarFile(String fileName) {
        TarFile specifiedTarSourceFile = getSpecifiedTarSourceFile(fileName);
        if (specifiedTarSourceFile != null) {
            try {
                specifiedTarSourceFile.close();
                return true;
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    private static boolean isGzipFile(String fileName) {
        if (!fileName.isEmpty()) {
            try (GzipFile specifiedTarSourceFile = new GzipFile(fileName);) {
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }

    private static ZipFile getSpecifiedZipSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        try {
            return new ZipFile(fileName);
        } catch (ZipException e) {
            // ignore
        } catch (IOException e) {
            // ignore
        }

        return null;
    }

    private static TarFile getSpecifiedTarSourceFile(String fileName) {
        if (fileName.length() == 0) {
            return null;
        }

        // FIXME: Work around Bug 463633. Remove this block once we move to Eclipse 4.5.
        if (new File(fileName).length() < 512) {
            return null;
        }

        try {
            return new TarFile(fileName);
        } catch (TarException | IOException e) {
            // ignore
        }

        return null;
    }

    /**
     * Get all the TraceFileSystemElements recursively.
     *
     * @param result
     *            the list accumulating the result
     * @param rootElement
     *            the root element of the file system to be imported
     */
    public static void getAllChildren(List<TraceFileSystemElement> result, TraceFileSystemElement rootElement) {
        AdaptableList files = rootElement.getFiles();
        for (Object file : files.getChildren()) {
            result.add((TraceFileSystemElement) file);
        }

        AdaptableList folders = rootElement.getFolders();
        for (Object folder : folders.getChildren()) {
            getAllChildren(result, (TraceFileSystemElement) folder);
        }
    }

    @SuppressWarnings("resource")
    static boolean ensureZipSourceIsValid(String archivePath, Shell shell) {
        ZipFile specifiedFile = getSpecifiedZipSourceFile(archivePath);
        if (specifiedFile == null) {
            return false;
        }
        return ArchiveFileManipulations.closeZipFile(specifiedFile, shell);
    }

    static boolean ensureTarSourceIsValid(String archivePath, Shell shell) {
        TarFile specifiedFile = getSpecifiedTarSourceFile(archivePath);
        if (specifiedFile == null) {
            return false;
        }
        return ArchiveFileManipulations.closeTarFile(specifiedFile, shell);
    }


    static boolean ensureGzipSourceIsValid(String archivePath) {
        return isGzipFile(archivePath);
    }
}
