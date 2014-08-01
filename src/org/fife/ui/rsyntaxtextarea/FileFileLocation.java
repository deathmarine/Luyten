/*
 * 11/13/2008
 *
 * FileFileLocation.java - The location of a local file.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * The location of a local file.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class FileFileLocation extends FileLocation {

	/**
	 * The file.  This may or may not actually exist.
	 */
	private File file;


	/**
	 * Constructor.
	 *
	 * @param file The local file.
	 */
	public FileFileLocation(File file) {
		try {
			// Useful on Windows and OS X.
			this.file = file.getCanonicalFile();
		} catch (IOException ioe) {
			this.file = file;
		}
	}


	/**
	 * {@inheritDoc}
	 */
	protected long getActualLastModified() {
		return file.lastModified();
	}


	/**
	 * Returns the full path to the file.
	 *
	 * @return The full path to the file.
	 * @see #getFileName()
	 */
	public String getFileFullPath() {
		return file.getAbsolutePath();
	}


	/**
	 * {@inheritDoc}
	 */
	public String getFileName() {
		return file.getName();
	}


	/**
	 * {@inheritDoc}
	 */
	protected InputStream getInputStream() throws IOException {
		return new FileInputStream(file);
	}


	/**
	 * {@inheritDoc}
	 */
	protected OutputStream getOutputStream() throws IOException {
		return new FileOutputStream(file);
	}


	/**
	 * Always returns <code>true</code>.
	 *
	 * @return <code>true</code> always.
	 * @see #isLocalAndExists()
	 */
	public boolean isLocal() {
		return true;
	}


	/**
	 * Since file locations of this type are guaranteed to be local, this
	 * method returns whether the file exists.
	 *
	 * @return Whether this local file actually exists.
	 * @see #isLocal()
	 */
	public boolean isLocalAndExists() {
		return file.exists();
	}


}