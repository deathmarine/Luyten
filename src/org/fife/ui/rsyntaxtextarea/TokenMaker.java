/*
 * 02/24/2004
 *
 * TokenMaker.java - An object that can take a chunk of text and return a
 * linked list of <code>Token</code>s representing it.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import javax.swing.Action;
import javax.swing.text.Segment;


/**
 * An implementation of <code>TokenMaker</code> is a class that turns text into
 * a linked list of <code>Token</code>s for syntax highlighting
 * in a particular language.
 *
 * @see Token
 * @see AbstractTokenMaker
 *
 * @author Robert Futrell
 * @version 0.2
 */
public interface TokenMaker {


	/**
	 * Adds a null token to the end of the current linked list of tokens.
	 * This should be put at the end of the linked list whenever the last
	 * token on the current line is NOT a multi-line token.
	 */
	public void addNullToken();


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param array The character array from which to get the text.
	 * @param start Start offset in <code>segment</code> of token.
	 * @param end End offset in <code>segment</code> of token.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *        occurs.
	 */
	public void addToken(char[] array, int start, int end, int tokenType,
							int startOffset);


	/**
	 * Returns whether this programming language uses curly braces
	 * ('<tt>{</tt>' and '<tt>}</tt>') to denote code blocks.
	 *
	 * @return Whether curly braces denote code blocks.
	 */
	public boolean getCurlyBracesDenoteCodeBlocks();


	/**
	 * Returns the last token on this line's type if the token is "unfinished",
	 * or {@link Token#NULL} if it was finished.  For example, if C-style
	 * syntax highlighting is being implemented, and <code>text</code>
	 * contained a line of code that contained the beginning of a comment but
	 * no end-comment marker ("*\/"), then this method would return
	 * {@link Token#COMMENT_MULTILINE} for that line.  This is useful
	 * for doing syntax highlighting.
	 *
	 * @param text The line of tokens to examine.
	 * @param initialTokenType The token type to start with (i.e., the value
	 *        of <code>getLastTokenTypeOnLine</code> for the line before
	 *        <code>text</code>).
	 * @return The last token on this line's type, or {@link Token#NULL}
	 *         if the line was completed.
	 */
	public int getLastTokenTypeOnLine(Segment text, int initialTokenType);


	/**
	 * Returns the text to place at the beginning and end of a
	 * line to "comment" it in a this programming language.
	 *
	 * @return The start and end strings to add to a line to "comment"
	 *         it out.  A <code>null</code> value for either means there
	 *         is no string to add for that part.  A value of
	 *         <code>null</code> for the array means this language
	 *         does not support commenting/uncommenting lines.
	 */
	public String[] getLineCommentStartAndEnd();


	/**
	 * Returns an action to handle "insert break" key presses (i.e. Enter).
	 *
	 * @return The action, or <code>null</code> if the default action should
	 *         be used.
	 */
	public Action getInsertBreakAction();


	/**
	 * Returns whether tokens of the specified type should have "mark
	 * occurrences" enabled for the current programming language.
	 *
	 * @param type The token type.
	 * @return Whether tokens of this type should have "mark occurrences"
	 *         enabled.
	 */
	public boolean getMarkOccurrencesOfTokenType(int type);


	/**
	 * If a line ends in the specified token, this method returns whether
	 * a new line inserted after that line should be indented.
	 *
	 * @param token The token the previous line ends with.
	 * @return Whether the next line should be indented.
	 */
	public boolean getShouldIndentNextLineAfter(Token token);


	/**
	 * Returns the first token in the linked list of tokens generated
	 * from <code>text</code>.  This method must be implemented by
	 * subclasses so they can correctly implement syntax highlighting.
	 *
	 * @param text The text from which to get tokens.
	 * @param initialTokenType The token type we should start with.
	 * @param startOffset The offset into the document at which
	 *        <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public Token getTokenList(Segment text, int initialTokenType,
											int startOffset);


	/**
	 * Returns whether this language is a markup language.
	 *
	 * @return Whether this language is markup.
	 */
	public boolean isMarkupLanguage();


	/**
	 * Returns whether whitespace is visible.
	 *
	 * @return Whether whitespace is visible.
	 * @see #setWhitespaceVisible(boolean)
	 */
	public boolean isWhitespaceVisible();


	/**
	 * Sets whether tokens are generated that "show" whitespace.
	 *
	 * @param visible Whether whitespace should be visible.
	 * @see #isWhitespaceVisible()
	 */
	public void setWhitespaceVisible(boolean visible);


}