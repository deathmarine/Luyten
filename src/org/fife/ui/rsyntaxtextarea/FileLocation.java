/*
 * 11/13/2008
 *
 * FileLocation.java - Holds the location of a local or remote file.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;


/**
 * Holds the location of a local or remote file.  This provides a common way
 * to read, write, and check properties of both local and remote files.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class FileLocation {


	/**
	 * Creates a {@link FileLocation} instance for the specified local file.
	 *
	 * @param fileFullPath The full path to a local file.
	 * @return The file's location.
	 */
	public static FileLocation create(String fileFullPath) {
		return new FileFileLocation(new File(fileFullPath));
	}


	/**
	 * Creates a {@link FileLocation} instance for the specified local file.
	 *
	 * @param file A local file.
	 * @return The file's location.
	 */
	public static FileLocation create(File file) {
		return new FileFileLocation(file);
	}


	/**
	 * Creates a {@link FileLocation} instance for the specified file.
	 *
	 * @param url The URL of a file.
	 * @return The file's location.
	 */
	public static FileLocation create(URL url) {
		if ("file".equalsIgnoreCase(url.getProtocol())) {
			return new FileFileLocation(new File(url.getPath()));
		}
		return new URLFileLocation(url);
	}


	/**
	 * Returns the last time this file was modified, or
	 * {@link TextEditorPane#LAST_MODIFIED_UNKNOWN} if this value cannot be
	 * computed (such as for a remote file).
	 *
	 * @return The last time this file was modified.
	 */
	protected abstract long getActualLastModified();


	/**
	 * Returns the full path to the file.  This will be stripped of
	 * sensitive information such as passwords for remote files.
	 *
	 * @return The full path to the file.
	 * @see #getFileName()
	 */
	public abstract String getFileFullPath();


	/**
	 * Returns the name of the file.
	 *
	 * @return The name of the file.
	 * @see #getFileFullPath()
	 */
	public abstract String getFileName();


	/**
	 * Opens an input stream for reading from this file.
	 *
	 * @return The input stream.
	 * @throws IOException If the file does not exist, or some other IO error
	 *         occurs.
	 */
	protected abstract InputStream getInputStream() throws IOException;


	/**
	 * Opens an output stream for writing this file.
	 *
	 * @return An output stream.
	 * @throws IOException If an IO error occurs.
	 */
	protected abstract OutputStream getOutputStream() throws IOException;


	/**
	 * Returns whether this file location is a local file.
	 *
	 * @return Whether this is a local file.
	 * @see #isLocalAndExists()
	 */
	public abstract boolean isLocal();


	/**
	 * Returns whether this file location is a local file that already
	 * exists.
	 *
	 * @return Whether this file is local and actually exists.
	 * @see #isLocal()
	 */
	public abstract boolean isLocalAndExists();


	/**
	 * Returns whether this file location is a remote location.
	 *
	 * @return Whether this is a remote file location.
	 */
	public boolean isRemote() {
		return !isLocal();
	}


}