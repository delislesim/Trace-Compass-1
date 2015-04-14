package org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;

@SuppressWarnings("restriction")
public class GzipLeveledStructureProvider implements ILeveledImportStructureProvider {

    private GzipFile file;
    private GzipEntry root = new GzipEntry("/");//$NON-NLS-1$
    private GzipEntry fEntry;

    /**
     * Creates a <code>GzipFileStructureProvider</code>, which will operate on
     * the passed tar file.
     *
     * @param sourceFile
     *            the source GzipFile
     */
    public GzipLeveledStructureProvider(GzipFile sourceFile) {
        super();
        file = sourceFile;
        root.setFileType(GzipEntry.DIRECTORY);
        fEntry = (GzipEntry) sourceFile.entries().nextElement();
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    @Override
    public List getChildren(Object element) {
        ArrayList<Object> children = new ArrayList<>();
        if (element == root) {
            children.add(fEntry);
        }
        return children;
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    @Override
    public InputStream getContents(Object element) {
        try {
            return file.getInputStream((GzipEntry) element);
        } catch (IOException e) {
            Activator.getDefault().logError(e.getLocalizedMessage(), e);
            return null;
        }
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    @Override
    public String getFullPath(Object element) {
        return ((GzipEntry) element).getName();
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    @Override
    public String getLabel(Object element) {
        if (element != root && element != fEntry) {
            throw new IllegalArgumentException();
        }
        return ((GzipEntry) element).getName();
    }

    /**
     * Returns the entry that this importer uses as the root sentinel.
     *
     * @return GzipEntry entry
     */
    @Override
    public Object getRoot() {
        return root;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.internal.wizards.datatransfer.
     * ILeveledImportStructureProvider#closeArchive()
     */
    @Override
    public boolean closeArchive() {
        try {
            file.close();
        } catch (IOException e) {
            Activator.getDefault().logError(DataTransferMessages.ZipImport_couldNotClose
                    + file.getName(), e);
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc) Method declared on IImportStructureProvider
     */
    @Override
    public boolean isFolder(Object element) {
        return ((GzipEntry) element).getFileType() == GzipEntry.DIRECTORY;
    }

    @Override
    public void setStrip(int level) {
    }

    @Override
    public int getStrip() {
        return 0;
    }
}
