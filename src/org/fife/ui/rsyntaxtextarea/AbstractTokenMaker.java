/*
 * 11/07/2004
 *
 * AbstractTokenMaker.java - An abstract implementation of TokenMaker.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;


/**
 * An abstract implementation of the
 * {@link org.fife.ui.rsyntaxtextarea.TokenMaker} interface.  It should
 * be overridden for every language for which you want to provide
 * syntax highlighting.<p>
 *
 * @see Token
 *
 * @author Robert Futrell
 * @version 0.2
 */
public abstract class AbstractTokenMaker extends TokenMakerBase {

	/**
	 * Hash table of words to highlight and what token type they are.
	 * The keys are the words to highlight, and their values are the
	 * token types, for example, <code>Token.RESERVED_WORD</code> or
	 * <code>Token.FUNCTION</code>.
	 */
	protected TokenMap wordsToHighlight;


	/**
	 * Constructor.
	 */
	public AbstractTokenMaker() {
		wordsToHighlight = getWordsToHighlight();
	}


	/**
	 * Returns the words to highlight for this programming language.
	 *
	 * @return A <code>TokenMap</code> containing the words to highlight for
	 *         this programming language.
	 */
	public abstract TokenMap getWordsToHighlight();


	/**
	 * Removes the token last added from the linked list of tokens.  The
	 * programmer should never have to call this directly; it can be called
	 * by subclasses of <code>TokenMaker</code> if necessary.
	 */
	public void removeLastToken() {
		if (previousToken==null) {
			firstToken = currentToken = null;
		}
		else {
			currentToken = previousToken;
			currentToken.setNextToken(null);
		}
	}


}