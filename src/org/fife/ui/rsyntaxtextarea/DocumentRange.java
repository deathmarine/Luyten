/*
 * 08/11/2009
 *
 * DocumentRange.java - A range of text in a document.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;


/**
 * A range of text in a document.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class DocumentRange {

	private int startOffs;
	private int endOffs;


	public DocumentRange(int startOffs, int endOffs) {
		this.startOffs = startOffs;
		this.endOffs = endOffs;
	}


	/**
	 * Gets the end offset of the range.
	 *
	 * @return The end offset.
	 * @see #getStartOffset()
	 */
	public int getEndOffset() {
		return endOffs;
	}


	/**
	 * Gets the starting offset of the range.
	 *
	 * @return The starting offset.
	 * @see #getEndOffset()
	 */
	public int getStartOffset() {
		return startOffs;
	}


	/**
	 * Returns a string representation of this object.
	 *
	 * @return A string representation of this object.
	 */
	public String toString() {
		return "[DocumentRange: " + startOffs + "-" + endOffs + "]";
	}


}