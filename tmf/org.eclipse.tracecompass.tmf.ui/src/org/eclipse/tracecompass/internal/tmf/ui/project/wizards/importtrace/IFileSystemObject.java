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

/**
 * This interface abstracts the differences between different kinds of
 * FileSystemObjects such as File, TarEntry, ZipEntry, etc. This allows clients
 * (TraceFileSystemElement, TraceValidateAndImportOperation) to handle all the
 * types transparently.
 */
public interface IFileSystemObject {
    String getLabel();

    String getName();

    String getAbsolutePath(String parentContainerPath);

    String getSourceLocation();

    Object getRawFileSystemObject();

    boolean exists();
}