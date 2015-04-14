package org.eclipse.tracecompass.internal.tmf.ui.project.wizards.importtrace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

public class GzipFile {

    private File file;
    private GzipEntry fEntry;
    private GzipEntry curEntry;

    private InputStream internalEntryStream;

    /**
     * Create a new TarFile for the given file.
     *
     * @param file
     * @throws IOException
     */
    public GzipFile(File file) throws IOException {
        this.file = file;

        InputStream in = new FileInputStream(file);
        // Check if it's a GZIPInputStream.
        internalEntryStream = new GZIPInputStream(in);
        String name = file.getName();
        fEntry = new GzipEntry(name.substring(0, name.lastIndexOf(".gz")));
        curEntry = fEntry;
    }

    /**
     * Close the tar file input stream.
     *
     * @throws IOException if the file cannot be successfully closed
     */
    public void close() throws IOException {
        if (internalEntryStream != null) {
            internalEntryStream.close();
        }
    }

    /**
     * Create a new GzipFile for the given path name.
     *
     * @param filename
     * @throws IOException
     */
    public GzipFile(String filename) throws IOException {
        this(new File(filename));
    }

    /**
     * Returns an enumeration cataloguing the tar archive.
     *
     * @return enumeration of all files in the archive
     */
    public Enumeration entries() {
        return new Enumeration() {
            @Override
            public boolean hasMoreElements() {
                return (curEntry != null);
            }

            @Override
            public Object nextElement() {
                GzipEntry oldEntry = curEntry;
                curEntry = null;
                return oldEntry;
            }
        };
    }

    /**
     * Returns a new InputStream for the given file in the tar archive.
     *
     * @param entry
     * @return an input stream for the given file
     * @throws IOException
     */
    public InputStream getInputStream(GzipEntry entry) throws IOException {
        if (entry != fEntry) {
            throw new IllegalArgumentException();
        }
        return internalEntryStream;
    }

    /**
     * Returns the path name of the file this archive represents.
     *
     * @return path
     */
    public String getName() {
        return file.getPath();
    }

    /* (non-Javadoc)
     * @see java.util.zip.ZipFile#finalize()
     *
     */
    @Override
    protected void finalize() throws Throwable {
        close();
    }

}
