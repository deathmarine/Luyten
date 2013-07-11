/*
 * 11/29/2008
 *
 * CodeTemplate.java - A "template" (macro) for commonly-typed code.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.templates;

import java.io.Serializable;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


/**
 * A "code template" is a kind of macro for commonly-typed code.  It
 * associates a short identifier with a longer code snippet, then when the
 * code template is enabled and the short identifier is typed, it is
 * replaced with the longer code snippet.<p>
 *
 * For example, you can associate the identifier <code>forb</code>
 * (short for "for-block") with the following code:<p>
 *
 * <pre>
 *   for (&lt;caret&gt;) {
 *
 *   }
 * </pre>
 *
 * Then, whenever you type <code>forb</code> followed by a trigger
 * (e.g., a space) into a text area with this <code>CodeTemplate</code>,
 * the code snippet is added in place of <code>forb</code>.  Further,
 * the caret is placed at the position denoted by <code>&lt;caret&gt;</code>.<p>
 *
 * Static text replacements are done with {@link StaticCodeTemplate}.  Dynamic
 * templates can also be created and used.
 *
 * @author Robert Futrell
 * @version 0.1
 * @see StaticCodeTemplate
 */
public interface CodeTemplate extends Cloneable, Comparable, Serializable {


	/**
	 * Creates a deep copy of this template.
	 *
	 * @return A deep copy of this template.
	 */
	public Object clone();


	/**
	 * Returns the ID of this code template.
	 *
	 * @return The template's ID.
	 */
	public String getID();


	/**
	 * Invokes this code template.  The changes are made to the given text
	 * area.
	 *
	 * @param textArea The text area to operate on.
	 * @throws BadLocationException If something bad happens.
	 */
	public void invoke(RSyntaxTextArea textArea) throws BadLocationException;


}