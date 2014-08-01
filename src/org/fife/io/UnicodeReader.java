/*
 * 09/23/2004
 *
 * UnicodeReader.java - A reader for Unicode input streams that is capable of
 * discerning which particular encoding is being used via the BOM.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.Reader;


/**
 * A reader capable of identifying Unicode streams by their BOMs.  This class
 * will recognize the following encodings:
 * <ul>
 *   <li>UTF-8
 *   <li>UTF-16LE
 *   <li>UTF-16BE
 *   <li>UTF-32LE
 *   <li>UTF-32BE
 * </ul>
 * If the stream is not found to be any of the above, then a default encoding
 * is used for reading.  The user can specify this default encoding, or a system
 * default will be used.<p>
 *
 * For optimum performance, it is recommended that you wrap all instances of
 * <code>UnicodeReader</code> with a <code>java.io.BufferedReader</code>.<p>
 *
 * This class is mostly ripped off from the workaround in the description of
 * Java Bug 4508058.
 *
 * @author Robert Futrell
 * @version 0.9
 */
public class UnicodeReader extends Reader {

	/**
	 * The input stream from which we're really reading.
	 */
	private InputStreamReader internalIn = null;

	/**
	 * The encoding being used.  We keep our own instead of using the string
	 * returned by <code>java.io.InputStreamReader</code> since that class
	 * does not return user-friendly names.
	 */
	private String encoding;

	/**
	 * The size of a BOM.
	 */
	private static final int BOM_SIZE = 4;


	/**
	 * This utility constructor is here because you will usually use a
	 * <code>UnicodeReader</code> on files.<p>
	 * Creates a reader using the encoding specified by the BOM in the file;
	 * if there is no recognized BOM, then a system default encoding is used.
	 *
	 * @param file The file from which you want to read.
	 * @throws IOException If an error occurs when checking for/reading the
	 *         BOM.
	 * @throws FileNotFoundException If the file does not exist, is a
	 *         directory, or cannot be opened for reading.
	 * @throws SecurityException If a security manager exists and its
	 *         checkRead method denies read access to the file.
	 */
	public UnicodeReader(String file) throws IOException,
							FileNotFoundException, SecurityException {
		this(new File(file));
	}


	/**
	 * This utility constructor is here because you will usually use a
	 * <code>UnicodeReader</code> on files.<p>
	 * Creates a reader using the encoding specified by the BOM in the file;
	 * if there is no recognized BOM, then a system default encoding is used.
	 *
	 * @param file The file from which you want to read.
	 * @throws IOException If an error occurs when checking for/reading the
	 *         BOM.
	 * @throws FileNotFoundException If the file does not exist, is a
	 *         directory, or cannot be opened for reading.
	 * @throws SecurityException If a security manager exists and its
	 *         checkRead method denies read access to the file.
	 */
	public UnicodeReader(File file) throws IOException, FileNotFoundException,
									SecurityException {
		this(new FileInputStream(file));
	}


	/**
	 * This utility constructor is here because you will usually use a
	 * <code>UnicodeReader</code> on files.<p>
	 * Creates a reader using the encoding specified by the BOM in the file;
	 * if there is no recognized BOM, then a specified default encoding is
	 * used.
	 *
	 * @param file The file from which you want to read.
	 * @param defaultEncoding The encoding to use if no BOM is found.  If
	 *        this value is <code>null</code>, a system default is used.
	 * @throws IOException If an error occurs when checking for/reading the
	 *         BOM.
	 * @throws FileNotFoundException If the file does not exist, is a
	 *         directory, or cannot be opened for reading.
	 * @throws SecurityException If a security manager exists and its
	 *         checkRead method denies read access to the file.
	 */
	public UnicodeReader(File file, String defaultEncoding)
						throws IOException, FileNotFoundException,
								SecurityException {
		this(new FileInputStream(file), defaultEncoding);
	}


	/**
	 * Creates a reader using the encoding specified by the BOM in the file;
	 * if there is no recognized BOM, then a system default encoding is used.
	 *
	 * @param in The input stream from which to read.
	 * @throws IOException If an error occurs when checking for/reading the
	 *         BOM.
	 */
	public UnicodeReader(InputStream in) throws IOException {
		this(in, null);
	}


	/**
	 * Creates a reader using the encoding specified by the BOM in the file;
	 * if there is no recognized BOM, then <code>defaultEncoding</code> is
	 * used.
	 *
	 * @param in The input stream from which to read.
	 * @param defaultEncoding The encoding to use if no recognized BOM is
	 *        found.  If this value is <code>null</code>, a system default
	 *        is used.
	 * @throws IOException If an error occurs when checking for/reading the
	 *         BOM.
	 */
	public UnicodeReader(InputStream in, String defaultEncoding)
									throws IOException {
		init(in, defaultEncoding);
	}


	/**
	 * Closes this reader.
	 */
	public void close() throws IOException {
		internalIn.close();
	}


	/**
	 * Returns the encoding being used to read this input stream (i.e., the
	 * encoding of the file).  If a BOM was recognized, then the specific
	 * Unicode type is returned; otherwise, either the default encoding passed
	 * into the constructor or the system default is returned.
	 *
	 * @return The encoding of the stream.
	 */
	public String getEncoding() {
		return encoding;
	}


	/**
	 * Read-ahead four bytes and check for BOM marks. Extra bytes are
	 * unread back to the stream, only BOM bytes are skipped.
	 *
	 * @param defaultEncoding The encoding to use if no BOM was recognized.  If
	 *        this value is <code>null</code>, then a system default is used.
	 * @throws IOException If an error occurs when trying to read a BOM.
	 */
	protected void init(InputStream in, String defaultEncoding)
											throws IOException {

		PushbackInputStream tempIn = new PushbackInputStream(in, BOM_SIZE);

		byte bom[] = new byte[BOM_SIZE];
		int n, unread;
		n = tempIn.read(bom, 0, bom.length);

		if ((bom[0]==(byte)0x00) && (bom[1]==(byte)0x00) &&
				(bom[2]==(byte)0xFE) && (bom[3]==(byte)0xFF)) {
			encoding = "UTF-32BE";
			unread = n - 4;
		}

		else if (n==BOM_SIZE && // Last 2 bytes are 0; could be an empty UTF-16
				(bom[0]==(byte)0xFF) && (bom[1]==(byte)0xFE) &&
				(bom[2]==(byte)0x00) && (bom[3]==(byte)0x00)) {
			encoding = "UTF-32LE";
			unread = n - 4;
		}
		
		else if ((bom[0]==(byte)0xEF) &&
			(bom[1]==(byte)0xBB) &&
			(bom[2]==(byte)0xBF)) {
			encoding = "UTF-8";
			unread = n - 3;
		}

		else if ((bom[0]==(byte)0xFE) && (bom[1] == (byte)0xFF)) {
			encoding = "UTF-16BE";
			unread = n - 2;
		}

		else if ((bom[0]==(byte)0xFF) && (bom[1]== (byte)0xFE)) {
			encoding = "UTF-16LE";
			unread = n - 2;
		}

		else {
			// Unicode BOM mark not found, unread all bytes
			encoding = defaultEncoding;
			unread = n;
		}

		if (unread > 0)
			tempIn.unread(bom, (n - unread), unread);
		else if (unread < -1)
			tempIn.unread(bom, 0, 0);

		// Use given encoding
		if (encoding == null) {
			internalIn = new InputStreamReader(tempIn);
			encoding = internalIn.getEncoding(); // Get the default.
		}
		else {
			internalIn = new InputStreamReader(tempIn, encoding);
		}

	}


	/**
	 * Read characters into a portion of an array. This method will block until
	 * some input is available, an I/O error occurs, or the end of the stream
	 * is reached.
	 *
	 * @param cbuf The buffer into which to read.
	 * @param off The offset at which to start storing characters.
	 * @param len The maximum number of characters to read.
	 *
	 * @return The number of characters read, or <code>-1</code> if the end
	 *         of the stream has been reached.
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		return internalIn.read(cbuf, off, len);
	}


}