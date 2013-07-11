/*
 * 10/08/2011
 *
 * FoldParser.java - Locates folds in an RSyntaxTextArea instance.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.util.List;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


/**
 * Locates folds in a document.  If you are implementing a language that has
 * sections of source code that can be logically "folded," you can create an
 * instance of this interface that locates those regions and represents them
 * as {@link Fold}s.  <code>RSyntaxTextArea</code> knows how to take it from
 * there and implement code folding in the editor.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see CurlyFoldParser
 * @see XmlFoldParser
 */
public interface FoldParser {


	/**
	 * Returns a list of all folds in the text area.
	 *
	 * @param textArea The text area whose contents should be analyzed.
	 * @return The list of folds.  If this method returns <code>null</code>,
	 *         it is treated as if no folds were found.
	 */
	public List getFolds(RSyntaxTextArea textArea);


}