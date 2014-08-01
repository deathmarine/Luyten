/*
 * 08/26/2004
 *
 * TokenMakerBase.java - A base class for token makers.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import javax.swing.Action;
import javax.swing.text.Segment;


/**
 * Base class for token makers.
 *
 * @author Robert Futrell
 * @version 1.0
 */
abstract class TokenMakerBase implements TokenMaker {

	/**
	 * The first token in the returned linked list.
	 */
	protected Token firstToken;
	
	/**
	 * Used in the creation of the linked list.
	 */
	protected Token currentToken;
	
	/**
	 * Used in the creation of the linked list.
	 */
	protected Token previousToken;

	/**
	 * The factory that gives us our tokens to use.
	 */
	private TokenFactory tokenFactory;

	/**
	 * "0" implies this is the "main" language being highlighted.  Positive
	 * values imply various "secondary" or "embedded" languages, such as CSS
	 * or JavaScript in HTML.  While this value is non-zero, tokens will be
	 * generated with this language index so they can (possibly) be painted
	 * differently, so "embedded" languages can be rendered with a special
	 * background.
	 */
	private int languageIndex;


	/**
	 * Constructor.
	 */
	public TokenMakerBase() {
		firstToken = currentToken = previousToken = null;
		tokenFactory = new DefaultTokenFactory();
	}


	/**
	 * {@inheritDoc}
	 */
	public void addNullToken() {
		if (firstToken==null) {
			firstToken = tokenFactory.createToken();
			currentToken = firstToken;
		}
		else {
			currentToken.setNextToken(tokenFactory.createToken());
			previousToken = currentToken;
			currentToken = currentToken.getNextToken();
		}
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param segment <code>Segment</code> to get text from.
	 * @param start Start offset in <code>segment</code> of token.
	 * @param end End offset in <code>segment</code> of token.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *        occurs.
	 */
	public void addToken(Segment segment, int start, int end, int tokenType,
							int startOffset) {
		addToken(segment.array, start,end, tokenType, startOffset);
	}


	/**
	 * {@inheritDoc}
	 */
	public void addToken(char[] array, int start, int end, int tokenType,
						int startOffset) {
		addToken(array, start, end, tokenType, startOffset, false);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param array The character array.
	 * @param start The starting offset in the array.
	 * @param end The ending offset in the array.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *        occurs.
	 * @param hyperlink Whether this token is a hyperlink.
	 */
	public void addToken(char[] array, int start, int end, int tokenType,
						int startOffset, boolean hyperlink) {

		if (firstToken==null) {
			firstToken = tokenFactory.createToken(array, start, end,
									startOffset, tokenType);
			currentToken = firstToken; // previous token is still null.
		}
		else {
			currentToken.setNextToken(tokenFactory.createToken(array,
								start,end, startOffset, tokenType));
			previousToken = currentToken;
			currentToken = currentToken.getNextToken();
		}

		currentToken.setLanguageIndex(languageIndex);
		currentToken.setHyperlink(hyperlink);

	}


	/**
	 * Returns whether this programming language uses curly braces
	 * ('<tt>{</tt>' and '<tt>}</tt>') to denote code blocks.<p>
	 *
	 * The default implementation returns <code>false</code>; subclasses can
	 * override this method if necessary.
	 *
	 * @return Whether curly braces denote code blocks.
	 */
	public boolean getCurlyBracesDenoteCodeBlocks() {
		return false;
	}


	/**
	 * Returns an action to handle "insert break" key presses (i.e. Enter).
	 * The default implementation returns <code>null</code>.  Subclasses
	 * can override.
	 *
	 * @return The default implementation always returns <code>null</code>.
	 */
	public Action getInsertBreakAction() {
		return null;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getLastTokenTypeOnLine(Segment text, int initialTokenType) {

		// Last parameter doesn't matter if we're not painting.
		Token t = getTokenList(text, initialTokenType, 0);

		while (t.getNextToken()!=null)
			t = t.getNextToken();

		return t.type;

	}


	/**
	 * {@inheritDoc}
	 */
	public String[] getLineCommentStartAndEnd() {
		return null;
	}


	/**
	 * Returns whether tokens of the specified type should have "mark
	 * occurrences" enabled for the current programming language.  The default
	 * implementation returns true if <tt>type</tt> is {@link Token#IDENTIFIER}.
	 * Subclasses can override this method to support other token types, such
	 * as {@link Token#VARIABLE}.
	 *
	 * @param type The token type.
	 * @return Whether tokens of this type should have "mark occurrences"
	 *         enabled.
	 */
	public boolean getMarkOccurrencesOfTokenType(int type) {
		return type==Token.IDENTIFIER;
	}


	/**
	 * The default implementation returns <code>false</code> always.  Languages
	 * that wish to better support auto-indentation can override this method.
	 *
	 * @param token The token the previous line ends with.
	 * @return Whether the next line should be indented.
	 */
	public boolean getShouldIndentNextLineAfter(Token token) {
		return false;
	}


	/**
	 * The default implementation returns <code>false</code> always.
	 * Subclasses that are highlighting a markup language should override this
	 * method to return <code>true</code>.
	 *
	 * @return <code>false</code> always.
	 */
	public boolean isMarkupLanguage() {
		return false;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isWhitespaceVisible() {
		return tokenFactory instanceof VisibleWhitespaceTokenFactory;
	}


	/**
	 * Deletes the linked list of tokens so we can begin anew.  This should
	 * never have to be called by the programmer, as it is automatically
	 * called whenever the user calls
	 * {@link #getLastTokenTypeOnLine(Segment, int)} or
	 * {@link #getTokenList(Segment, int, int)}.
	 */
	protected void resetTokenList() {
		firstToken = currentToken = previousToken = null;
		tokenFactory.resetAllTokens();
	}


	/**
	 * Sets the language index to assign to tokens moving forward.  This
	 * property is used to designate tokens as being in "secondary" languages
	 * (such as CSS or JavaScript in HTML).
	 *
	 * @param languageIndex The new language index.  A value of
	 *        <code>0</code> denotes the "main" language, any positive value
	 *        denotes a specific secondary language.  Negative values will
	 *        be treated as <code>0</code>.
	 */
	public void setLanguageIndex(int languageIndex) {
		this.languageIndex = Math.max(0, languageIndex);
	}


	/**
	 * {@inheritDoc}
	 */
	public void setWhitespaceVisible(boolean visible) {
		// FIXME:  Initialize with the proper sizes.
		tokenFactory = visible ? new VisibleWhitespaceTokenFactory() :
									new DefaultTokenFactory();
	}


}