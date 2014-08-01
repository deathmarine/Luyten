/*
 * 02/05/2009
 *
 * ToolTipSupplier.java - Can provide tool tips to RTextAreas without the need
 * for subclassing.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.event.MouseEvent;


/**
 * A <tt>ToolTipSupplier</tt> can create tool tip text for an <tt>RTextArea</tt>
 * on its behalf.  A text area will check its <tt>ToolTipSupplier</tt> for a
 * tool tip before calling the super class's implementation of
 * {@link RTextArea#getToolTipText()}.  This allows
 * applications to intercept tool tip events and provide the text for a tool
 * tip without subclassing <tt>RTextArea</tt>.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface ToolTipSupplier {


	/**
	 * Returns the tool tip text to display for a given mouse event.
	 *
	 * @param textArea The text area.
	 * @param e The mouse event.
	 * @return The tool tip, or <code>null</code> if none.
	 */
	public String getToolTipText(RTextArea textArea, MouseEvent e);


}