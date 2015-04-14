/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Marc-Andre Laperle - Initial API and implementation. Inspired from TarEntry.
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace;

public class GzipEntry {
    private String name;
    private long mode, time, size;
    private int type;
    int filepos;

    /**
     * Entry type for normal files. This is the only valid type for Gzip entries.
     */
    public static final int FILE = '0';

    /**
     * Entry type for directories. This doesn't really exist in a Gzip but it's
     * useful to represent the root of the archive.
     */
    public static final int DIRECTORY = '5';

    /**
     * Create a new GzipEntry for a file of the given name at the
     * given position in the file.
     *
     * @param name filename
     * @param pos position in the file in bytes
     */
    GzipEntry(String name, int pos) {
        this.name = name;
        mode = 0644;
        type = FILE;
        filepos = pos;
        time = System.currentTimeMillis() / 1000;
    }

    /**
     * Create a new TarEntry for a file of the given name.
     *
     * @param name filename
     */
    public GzipEntry(String name) {
        this(name, -1);
    }

    /**
     * Returns the type of this file, can only be FILE for a real Gzip entry.
     * DIRECTORY can be specified to represent a "dummy root" in the archive.
     *
     * @return file type
     */
    public int getFileType() {
        return type;
    }

    /**
     * Returns the mode of the file in UNIX permissions format.
     *
     * @return file mode
     */
    public long getMode() {
        return mode;
    }

    /**
     * Returns the name of the file.
     *
     * @return filename
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the size of the file in bytes.
     *
     * @return filesize
     */
    public long getSize() {
        return size;
    }

    /**
     * Returns the modification time of the file in seconds since January
     * 1st 1970.
     *
     * @return time
     */
    public long getTime() {
        return time;
    }

    /**
     * Sets the type of the file, can only be FILE for a real Gzip entry.
     * DIRECTORY can be specified to represent a "dummy root" in the archive.
     *
     * @param type
     */
    public void setFileType(int type) {
        if (type != FILE && type != DIRECTORY) {
            throw new IllegalArgumentException();
        }
        this.type = type;
    }

    /**
     * Sets the mode of the file in UNIX permissions format.
     *
     * @param mode
     */
    public void setMode(long mode) {
        this.mode = mode;
    }

    /**
     * Sets the size of the file in bytes.
     *
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Sets the modification time of the file in seconds since January
     * 1st 1970.
     *
     * @param time
     */
    public void setTime(long time) {
        this.time = time;
    }
}
