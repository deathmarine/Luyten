/*
 * 02/10/2009
 *
 * RSTAView.java - An <code>RSyntaxTextArea</code> view.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Rectangle;

import javax.swing.text.BadLocationException;


/**
 * Utility methods for RSyntaxTextArea's views.
 *
 * @author Robert Futrell
 * @version 1.0
 */
interface RSTAView {


	/**
	 * Returns the y-coordinate of the specified line.<p>
	 *
	 * This method is quicker than using traditional
	 * <code>modelToView(int)</code> calls, as the entire bounding box isn't
	 * computed.
	 *
	 * @param alloc The area the text area can render into.
	 * @param line The line number.
	 * @return The y-coordinate of the top of the line, or <code>-1</code> if
	 *         this text area doesn't yet have a positive size or the line is
	 *         hidden (i.e. from folding).
	 * @throws BadLocationException If <code>line</code> isn't a valid line
	 *         number for this document.
	 */
	public int yForLine(Rectangle alloc, int line) throws BadLocationException;


	/**
	 * Returns the y-coordinate of the line containing a specified offset.<p>
	 *
	 * This method is quicker than using traditional
	 * <code>modelToView(int)</code> calls, as the entire bounding box isn't
	 * computed.
	 *
	 * @param alloc The area the text area can render into.
	 * @param offs The offset info the document.
	 * @return The y-coordinate of the top of the offset, or <code>-1</code> if
	 *         this text area doesn't yet have a positive size or the line is
	 *         hidden (i.e. from folding).
	 * @throws BadLocationException If <code>offs</code> isn't a valid offset
	 *         into the document.
	 */
	public int yForLineContaining(Rectangle alloc, int offs)
											throws BadLocationException;


}