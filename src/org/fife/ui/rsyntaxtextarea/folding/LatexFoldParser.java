/*
 * 04/24/2012
 *
 * LatexFoldParser.java - Fold parser for LaTeX.
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
 * A fold parser for LaTeX documents.  This is likely incomplete and/or not
 * quite right; feedback is appreciated.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class LatexFoldParser implements FoldParser {

	private static final char[] BEGIN = "\\begin".toCharArray();
	private static final char[] END = "\\end".toCharArray();


	/**
	 * {@inheritDoc}
	 */
	public List getFolds(RSyntaxTextArea textArea) {

		List folds = new ArrayList();
		Stack expectedStack = new Stack();

		Fold currentFold = null;
		int lineCount = textArea.getLineCount();

		try {

			for (int line=0; line<lineCount; line++) {

				Token t = textArea.getTokenListForLine(line);
				while (t!=null && t.isPaintable()) {

					if (t.is(Token.RESERVED_WORD, BEGIN)) {
						Token temp = t.getNextToken();
						if (temp!=null && temp.isLeftCurly()) {
							temp = temp.getNextToken();
							if (temp!=null && temp.type==Token.RESERVED_WORD) {
								if (currentFold==null) {
									currentFold = new Fold(FoldType.CODE, textArea, t.offset);
									folds.add(currentFold);
								}
								else {
									currentFold = currentFold.createChild(FoldType.CODE, t.offset);
								}
								expectedStack.push(temp.getLexeme());
								t = temp;
							}
						}
					}

					else if (t.is(Token.RESERVED_WORD, END) &&
							currentFold!=null && !expectedStack.isEmpty()) {
						Token temp = t.getNextToken();
						if (temp!=null && temp.isLeftCurly()) {
							temp = temp.getNextToken();
							if (temp!=null && temp.type==Token.RESERVED_WORD) {
								String value = temp.getLexeme();
								if (expectedStack.peek().equals(value)) {
									expectedStack.pop();
									currentFold.setEndOffset(t.offset);
									Fold parentFold = currentFold.getParent();
									// Don't add fold markers for single-line blocks
									if (currentFold.isOnSingleLine()) {
										if (!currentFold.removeFromParent()) {
											folds.remove(folds.size()-1);
										}
									}
									t = temp;
									currentFold = parentFold;
								}
							}
						}
					}

					t = t.getNextToken();

				}

			}

		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}

		return folds;

	}


}