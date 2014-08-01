/*
 * 10/08/2011
 *
 * CurlyFoldParser.java - Fold parser for languages with C-style syntax.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;


/**
 * A basic fold parser that can be used for languages such as C, that use
 * curly braces to denote code blocks.  This parser searches for curly brace
 * pairs and creates code folds out of them.  It can also optionally find
 * C-style multi-line comments ("<code>/* ... *&#47;</code>") and make them
 * foldable as well.<p>
 * 
 * This parser knows nothing about language semantics; it uses
 * <code>RSyntaxTextArea</code>'s syntax highlighting tokens to identify
 * curly braces.  By default, it looks for single-char tokens of type
 * {@link Token#SEPARATOR}, with lexemes '<code>{</code>' or '<code>}</code>'.
 * If your {@link TokenMaker} uses a different token type for curly braces, you
 * should override the {@link #isLeftCurly(Token)} and
 * {@link #isRightCurly(Token)} methods with your own definitions.  In theory,
 * you could extend this fold parser to parse languages that use completely
 * different tokens than curly braces to denote foldable regions by overriding
 * those two methods.<p>
 *
 * Note also that this class may impose somewhat of a performance penalty on
 * large source files, since it re-parses the entire document each time folds
 * are reevaluated.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class CurlyFoldParser implements FoldParser {

	/**
	 * Whether to scan for C-style multi-line comments and make them foldable.
	 */
	private boolean foldableMultiLineComments;

	/**
	 * Whether this parser is folding Java.
	 */
	private final boolean java;

	/**
	 * Used to find import statements when folding Java code.
	 */
	private static final char[] KEYWORD_IMPORT = "import".toCharArray();

	/**
	 * Ending of a multi-line comment in C, C++, Java, etc.
	 */
	protected static final char[] C_MLC_END = "*/".toCharArray();


	/**
	 * Creates a fold parser that identifies foldable regions via curly braces
	 * as well as C-style multi-line comments.
	 */
	public CurlyFoldParser() {
		this(true, false);
	}


	/**
	 * Constructor.
	 *
	 * @param cStyleMultiLineComments Whether to scan for C-style multi-line
	 *        comments and make them foldable.
	 * @param java Whether this parser is folding Java.  This adds extra
	 *        parsing rules, such as grouping all import statements into a
	 *        fold section.
	 */
	public CurlyFoldParser(boolean cStyleMultiLineComments, boolean java) {
		this.foldableMultiLineComments = cStyleMultiLineComments;
		this.java = java;
	}


	/**
	 * Returns whether multi-line comments are foldable with this parser.
	 *
	 * @return Whether multi-line comments are foldable.
	 * @see #setFoldableMultiLineComments(boolean)
	 */
	public boolean getFoldableMultiLineComments() {
		return foldableMultiLineComments;
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
		int importStartLine = -1;
		int lastSeenImportLine = -1;
		int importGroupStartOffs = -1;
		int importGroupEndOffs = -1;

		try {

			for (int line=0; line<lineCount; line++) {

				Token t = textArea.getTokenListForLine(line);
				while (t!=null && t.isPaintable()) {

					if (getFoldableMultiLineComments() && t.isComment()) {

						// Java-specific stuff
						if (java) {

							if (importStartLine>-1) {
								if (lastSeenImportLine>importStartLine) {
									Fold fold = null;
									// Any imports found *should* be a top-level fold,
									// but we're extra lenient here and allow groups
									// of them anywhere to keep our parser better-behaved
									// if they have random "imports" throughout code.
									if (currentFold==null) {
										fold = new Fold(FoldType.IMPORTS,
												textArea, importGroupStartOffs);
										folds.add(fold);
									}
									else {
										fold = currentFold.createChild(FoldType.IMPORTS,
												importGroupStartOffs);
									}
									fold.setEndOffset(importGroupEndOffs);
								}
								importStartLine = lastSeenImportLine =
								importGroupStartOffs = importGroupEndOffs = -1;
							}

						}

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

					else if (isLeftCurly(t)) {

						// Java-specific stuff
						if (java) {

							if (importStartLine>-1) {
								if (lastSeenImportLine>importStartLine) {
									Fold fold = null;
									// Any imports found *should* be a top-level fold,
									// but we're extra lenient here and allow groups
									// of them anywhere to keep our parser better-behaved
									// if they have random "imports" throughout code.
									if (currentFold==null) {
										fold = new Fold(FoldType.IMPORTS,
												textArea, importGroupStartOffs);
										folds.add(fold);
									}
									else {
										fold = currentFold.createChild(FoldType.IMPORTS,
												importGroupStartOffs);
									}
									fold.setEndOffset(importGroupEndOffs);
								}
								importStartLine = lastSeenImportLine =
								importGroupStartOffs = importGroupEndOffs = -1;
							}

						}

						if (currentFold==null) {
							currentFold = new Fold(FoldType.CODE, textArea, t.offset);
							folds.add(currentFold);
						}
						else {
							currentFold = currentFold.createChild(FoldType.CODE, t.offset);
						}

					}

					else if (isRightCurly(t)) {

						if (currentFold!=null) {
							currentFold.setEndOffset(t.offset);
							Fold parentFold = currentFold.getParent();
							//System.out.println("... Adding regular fold at " + t.offset + ", parent==" + parentFold);
							// Don't add fold markers for single-line blocks
							if (currentFold.isOnSingleLine()) {
								if (!currentFold.removeFromParent()) {
									folds.remove(folds.size()-1);
								}
							}
							currentFold = parentFold;
						}

					}

					// Java-specific folding rules
					else if (java) {

						if (t.is(Token.RESERVED_WORD, KEYWORD_IMPORT)) {
							if (importStartLine==-1) {
								importStartLine = line;
								importGroupStartOffs = t.offset;
								importGroupEndOffs = t.offset;
							}
							lastSeenImportLine = line;
						}

						else if (importStartLine>-1 &&
								t.isIdentifier() &&//SEPARATOR &&
								t.isSingleChar(';')) {
							importGroupEndOffs = t.offset;
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


	/**
	 * Returns whether the token is a left curly brace.  This method exists
	 * so subclasses can provide their own curly brace definition.
	 *
	 * @param t The token.
	 * @return Whether it is a left curly brace.
	 * @see #isRightCurly(Token)
	 */
	public boolean isLeftCurly(Token t) {
		return t.isLeftCurly();
	}


	/**
	 * Returns whether the token is a right curly brace.  This method exists
	 * so subclasses can provide their own curly brace definition.
	 *
	 * @param t The token.
	 * @return Whether it is a right curly brace.
	 * @see #isLeftCurly(Token)
	 */
	public boolean isRightCurly(Token t) {
		return t.isRightCurly();
	}


	/**
	 * Sets whether multi-line comments are foldable with this parser.
	 *
	 * @param foldable Whether multi-line comments are foldable.
	 * @see #getFoldableMultiLineComments()
	 */
	public void setFoldableMultiLineComments(boolean foldable) {
		this.foldableMultiLineComments = foldable;
	}


}