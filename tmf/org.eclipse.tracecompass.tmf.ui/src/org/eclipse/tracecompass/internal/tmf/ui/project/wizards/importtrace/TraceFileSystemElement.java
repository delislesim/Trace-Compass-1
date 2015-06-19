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

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.dialogs.FileSystemElement;
import org.eclipse.ui.model.AdaptableList;

/**
 * The <code>TraceFileSystemElement</code> is a <code>FileSystemElement</code>
 * that knows if it has been populated or not.
 */
public class TraceFileSystemElement extends FileSystemElement {

    private boolean fIsPopulated = false;
    private String fLabel = null;
    private IPath fDestinationContainerPath;
    private FileSystemObjectImportStructureProvider fProvider;
    private String fSourceLocation;

    public TraceFileSystemElement(String name, FileSystemElement parent, boolean isDirectory, FileSystemObjectImportStructureProvider provider) {
        super(name, parent, isDirectory);
        fProvider = provider;
    }

    public void setDestinationContainerPath(IPath destinationContainerPath) {
        fDestinationContainerPath = destinationContainerPath;
    }

    public void setPopulated() {
        fIsPopulated = true;
    }

    public boolean isPopulated() {
        return fIsPopulated;
    }

    @Override
    public AdaptableList getFiles() {
        if (!fIsPopulated) {
            populateElementChildren();
        }
        return super.getFiles();
    }

    @Override
    public AdaptableList getFolders() {
        if (!fIsPopulated) {
            populateElementChildren();
        }
        return super.getFolders();
    }

    /**
     * Sets the label for the trace to be used when importing at trace.
     *
     * @param name
     *            the label for the trace
     */
    public void setLabel(String name) {
        fLabel = name;
    }

    /**
     * Returns the label for the trace to be used when importing at trace.
     *
     * @return the label of trace resource
     */
    public String getLabel() {
        if (fLabel == null) {
            return getFileSystemObject().getLabel();
        }
        return fLabel;
    }

    /**
     * The full path to the container that will contain the trace
     *
     * @return the destination container path
     */
    public IPath getDestinationContainerPath() {
        return fDestinationContainerPath;
    }

    /**
     * Populates the children of the specified parent
     * <code>FileSystemElement</code>
     */
    private void populateElementChildren() {
        List<IFileSystemObject> allchildren = fProvider.getChildren(this.getFileSystemObject());
        Object child = null;
        TraceFileSystemElement newelement = null;
        Iterator<IFileSystemObject> iter = allchildren.iterator();
        while (iter.hasNext()) {
            child = iter.next();
            newelement = new TraceFileSystemElement(fProvider.getLabel(child), this, fProvider.isFolder(child), fProvider);
            newelement.setFileSystemObject(child);
        }
        setPopulated();
    }

    public FileSystemObjectImportStructureProvider getProvider() {
        return fProvider;
    }

    @Override
    public IFileSystemObject getFileSystemObject() {
        Object fileSystemObject = super.getFileSystemObject();
        return (IFileSystemObject) fileSystemObject;
    }

    public String getSourceLocation() {
        if (fSourceLocation == null) {
            fSourceLocation = getFileSystemObject().getSourceLocation();
        }
        return fSourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        fSourceLocation = sourceLocation;
    }
}