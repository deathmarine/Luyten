/*
 * 10/07/2012
 *
 * NsisFoldParser.java - Fold parser for NSIS.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;


/**
 * A fold parser NSIS.<p>
 *
 * Note that this class may impose somewhat of a performance penalty on large
 * source files, since it re-parses the entire document each time folds are
 * reevaluated.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class NsisFoldParser implements FoldParser {

	private static final char[] KEYWORD_FUNCTION		= "Function".toCharArray();
	private static final char[] KEYWORD_FUNCTION_END	= "FunctionEnd".toCharArray();
	private static final char[] KEYWORD_SECTION			= "Section".toCharArray();
	private static final char[] KEYWORD_SECTION_END		= "SectionEnd".toCharArray();

	protected static final char[] C_MLC_END = "*/".toCharArray();


	private static final boolean foundEndKeyword(char[] keyword, Token t,
			Stack endWordStack) {
		return t.is(Token.RESERVED_WORD, keyword) && !endWordStack.isEmpty() &&
			keyword==endWordStack.peek();
	}


	/**
	 * {@inheritDoc}
	 */
	public List getFolds(RSyntaxTextArea textArea) {

		List folds = new ArrayList();

		Fold currentFold = null;
		int lineCount = textArea.getLineCount();
		boolean inMLC = false;
		int mlcStart = 0;
		Stack endWordStack = new Stack();

		try {

			for (int line=0; line<lineCount; line++) {

				Token t = textArea.getTokenListForLine(line);
				while (t!=null && t.isPaintable()) {

					if (t.isComment()) {

						if (inMLC) {
							// If we found the end of an MLC that started
							// on a previous line...
							if (t.endsWith(C_MLC_END)) {
								int mlcEnd = t.offset + t.textCount - 1;
								if (currentFold==null) {
									currentFold = new Fold(FoldType.COMMENT, textArea, mlcStart);
									currentFold.setEndOffset(mlcEnd);
									folds.add(currentFold);
									currentFold = null;
								}
								else {
									currentFold = currentFold.createChild(FoldType.COMMENT, mlcStart);
									currentFold.setEndOffset(mlcEnd);
									currentFold = currentFold.getParent();
								}
								//System.out.println("Ending MLC at: " + mlcEnd + ", parent==" + currentFold);
								inMLC = false;
								mlcStart = 0;
							}
							// Otherwise, this MLC is continuing on to yet
							// another line.
						}
						else {
							// If we're an MLC that ends on a later line...
							if (t.type!=Token.COMMENT_EOL && !t.endsWith(C_MLC_END)) {
								//System.out.println("Starting MLC at: " + t.offset);
								inMLC = true;
								mlcStart = t.offset;
							}
						}

					}

					else if (t.is(Token.RESERVED_WORD, KEYWORD_SECTION)) {
						if (currentFold==null) {
							currentFold = new Fold(FoldType.CODE, textArea, t.offset);
							folds.add(currentFold);
						}
						else {
							currentFold = currentFold.createChild(FoldType.CODE, t.offset);
						}
						endWordStack.push(KEYWORD_SECTION_END);
					}

					else if (t.is(Token.RESERVED_WORD, KEYWORD_FUNCTION)) {
						if (currentFold==null) {
							currentFold = new Fold(FoldType.CODE, textArea, t.offset);
							folds.add(currentFold);
						}
						else {
							currentFold = currentFold.createChild(FoldType.CODE, t.offset);
						}
						endWordStack.push(KEYWORD_FUNCTION_END);
					}

					else if (foundEndKeyword(KEYWORD_SECTION_END, t, endWordStack) ||
							foundEndKeyword(KEYWORD_FUNCTION_END, t, endWordStack)) {
						if (currentFold!=null) {
							currentFold.setEndOffset(t.offset);
							Fold parentFold = currentFold.getParent();
							endWordStack.pop();
							// Don't add fold markers for single-line blocks
							if (currentFold.isOnSingleLine()) {
								if (!currentFold.removeFromParent()) {
									folds.remove(folds.size()-1);
								}
							}
							currentFold = parentFold;
						}
					}

					t = t.getNextToken();

				}

			}

		} catch (BadLocationException ble) { // Should never happen
			ble.printStackTrace();
		}

		return folds;

	}


}