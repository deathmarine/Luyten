/*
 * 06/30/2012
 *
 * RDocumentCharSequence.java - Iterator over a portion of an RTextArea's
 * document.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import javax.swing.text.BadLocationException;


/**
 * Allows iterating over a portion of an <code>RDocument</code>.  This is of
 * course not thread-safe, so should only be used on the EDT or with external
 * synchronization.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class RDocumentCharSequence implements CharSequence {

	private RDocument doc;
	private int start;
	private int end;


	/**
	 * Creates a <code>CharSequence</code> representing the text in a document
	 * from the specified offset to the end of that document.
	 *
	 * @param doc The document.
	 * @param start The starting offset in the document, inclusive.
	 */
	public RDocumentCharSequence(RDocument doc, int start) {
		this(doc, start, doc.getLength());
	}


	/**
	 * Constructor.
	 *
	 * @param doc The document.
	 * @param start The starting offset in the document, inclusive.
	 * @param end the ending offset in the document, exclusive.
	 */
	public RDocumentCharSequence(RDocument doc, int start, int end) {
		this.doc = doc;
		this.start = start;
		this.end = end;
	}


	/**
	 * {@inheritDoc}
	 */
	public char charAt(int index) {
		if (index<0 || index>=length()) {
			throw new IndexOutOfBoundsException("Index " + index +
					" is not in range [0-" + length() + ")");
		}
		try {
			return doc.charAt(start+index);
		} catch (BadLocationException ble) {
			throw new IndexOutOfBoundsException(ble.toString());
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public int length() {
		return end - start;
	}


	/**
	 * {@inheritDoc}
	 */
	public CharSequence subSequence(int start, int end) {
		if (start<0) {
			throw new IndexOutOfBoundsException("start must be >= 0 (" +
					start + ")");
		}
		else if (end<0) {
			throw new IndexOutOfBoundsException("end must be >= 0 (" +
					end + ")");
		}
		else if (end>length()) {
			throw new IndexOutOfBoundsException("end must be <= " +
					length() + " (" + end + ")");
		}
		else if (start>end) {
			throw new IndexOutOfBoundsException("start (" + start +
					") cannot be > end (" + end + ")");
		}
		int newStart = this.start + start;
		int newEnd = this.start + end;
		return new RDocumentCharSequence(doc, newStart, newEnd);
	}


	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		try {
			return doc.getText(start, length());
		} catch (BadLocationException ble) { // Never happens
			ble.printStackTrace();
			return "";
		}
	}

}