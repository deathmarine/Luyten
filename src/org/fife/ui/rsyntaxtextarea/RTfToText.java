/*
 * 07/28/2008
 *
 * RtfToText.java - Returns the plain text version of RTF documents.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.io.*;


/**
 * Gets the plain text version of RTF documents.<p>
 *
 * This is used by <code>RtfTransferable</code> to return the plain text
 * version of the transferable when the receiver does not support RTF.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class RtfToText {

	private Reader r;
	private StringBuffer sb;
	private StringBuffer controlWord;
	private int blockCount;
	private boolean inControlWord;


	/**
	 * Private constructor.
	 *
	 * @param r The reader to read RTF text from.
	 */
	private RtfToText(Reader r) {
		this.r = r;
		sb = new StringBuffer();
		controlWord = new StringBuffer();
		blockCount = 0;
		inControlWord = false;
	}


	/**
	 * Converts the RTF text read from this converter's <code>Reader</code>
	 * into plain text.  It is the caller's responsibility to close the
	 * reader after this method is called.
	 *
	 * @return The plain text.
	 * @throws IOException If an IO error occurs.
	 */
	private String convert() throws IOException {

		// Skip over first curly brace as the whole file is in '{' and '}'
		int i = r.read();
		if (i!='{') {
			throw new IOException("Invalid RTF file");
		}

		while ((i=r.read())!=-1) {

			char ch = (char)i;
			switch (ch) {
				case '{':
					if (inControlWord && controlWord.length()==0) { // "\{"
						sb.append('{');
						controlWord.setLength(0);
						inControlWord = false;
					}
					else {
						blockCount++;
					}
					break;
				case '}':
					if (inControlWord && controlWord.length()==0) { // "\}"
						sb.append('}');
						controlWord.setLength(0);
						inControlWord = false;
					}
					else {
						blockCount--;
					}
					break;
				case '\\':
					if (blockCount==0) {
						if (inControlWord) {
							if (controlWord.length()==0) { // "\\"
								sb.append('\\');
								controlWord.setLength(0);
								inControlWord = false;
							}
							else {
								endControlWord();
							}
						}
						inControlWord = true;
					}
					break;
				case ' ':
					if (blockCount==0) {
						if (inControlWord) {
							endControlWord();
						}
						else {
							sb.append(' ');
						}
					}
					break;
				case '\r':
				case '\n':
					if (blockCount==0) {
						if (inControlWord) {
							endControlWord();
						}
						// Otherwise, ignore
					}
					break;
				default:
					if (blockCount==0) {
						if (inControlWord) {
							controlWord.append(ch);
						}
						else {
							sb.append(ch);
						}
					}
					break;
			}

		}

		return sb.toString();

	}


	/**
	 * Ends a control word.  Checks whether it is a common one that affects
	 * the plain text output (such as "<code>par</code>" or "<code>tab</code>")
	 * and updates the text buffer accordingly.
	 */
	private void endControlWord() {
		String word = controlWord.toString();
		if ("par".equals(word)) {
			sb.append('\n');
		}
		else if ("tab".equals(word)) {
			sb.append('\t');
		}
		controlWord.setLength(0);
		inControlWord = false;
	}


	/**
	 * Converts the contents of the specified byte array representing
	 * an RTF document into plain text.
	 *
	 * @param rtf The byte array representing an RTF document.
	 * @return The contents of the RTF document, in plain text.
	 * @throws IOException If an IO error occurs.
	 */
	public static String getPlainText(byte[] rtf) throws IOException {
		return getPlainText(new ByteArrayInputStream(rtf));
	}


	/**
	 * Converts the contents of the specified RTF file to plain text.
	 *
	 * @param file The RTF file to convert.
	 * @return The contents of the file, in plain text.
	 * @throws IOException If an IO error occurs.
	 */
	public static String getPlainText(File file) throws IOException {
		return getPlainText(new BufferedReader(new FileReader(file)));
	}


	/**
	 * Converts the contents of the specified input stream to plain text.
	 * The input stream will be closed when this method returns.
	 *
	 * @param in The input stream to convert.
	 * @return The contents of the stream, in plain text.
	 * @throws IOException If an IO error occurs.
	 */
	public static String getPlainText(InputStream in) throws IOException {
		return getPlainText(new InputStreamReader(in, "US-ASCII"));
	}


	/**
	 * Converts the contents of the specified <code>Reader</code> to plain text.
	 *
	 * @param r The <code>Reader</code>.
	 * @return The contents of the <code>Reader</code>, in plain text.
	 * @throws IOException If an IO error occurs.
	 */
	private static String getPlainText(Reader r) throws IOException {
		try {
			RtfToText  converter = new RtfToText(r);
			return converter.convert();
		} finally {
			r.close();
		}
	}


	/**
	 * Converts the contents of the specified String to plain text.
	 *
	 * @param rtf A string whose contents represent an RTF document.
	 * @return The contents of the String, in plain text.
	 * @throws IOException If an IO error occurs.
	 */
	public static String getPlainText(String rtf) throws IOException {
		return getPlainText(new StringReader(rtf));
	}


}