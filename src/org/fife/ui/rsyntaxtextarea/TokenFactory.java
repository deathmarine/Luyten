/*
 * 10/28/2004
 *
 * TokenFactory.java - Interface for a class that generates tokens of some type.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import javax.swing.text.Segment;


/**
 * Interface for a class that generates tokens somehow.
 *
 * @author Robert Futrell
 * @version 0.1
 */
interface TokenFactory {


	/**
	 * Returns a null token.
	 *
	 * @return A null token.
	 */
	public Token createToken();


	/**
	 * Returns a token.
	 *
	 * @param line The segment from which to get the token's text.
	 * @param beg The starting offset of the token's text in the segment.
	 * @param end The ending offset of the token's text in the segment.
	 * @param startOffset The offset in the document of the token.
	 * @param type The type of token.
	 * @return The token.
	 */
	public Token createToken(final Segment line, final int beg,
					final int end, final int startOffset, final int type);


	/**
	 * Returns a token.
	 *
	 * @param line The char array from which to get the token's text.
	 * @param beg The starting offset of the token's text in the char array.
	 * @param end The ending offset of the token's text in the char array.
	 * @param startOffset The offset in the document of the token.
	 * @param type The type of token.
	 * @return The token.
	 */
	public Token createToken(final char[] line, final int beg,
					final int end, final int startOffset, final int type);


	/**
	 * Resets the state of this token maker, if necessary.
	 * FIXME:  Improve documentation.
	 */
	public void resetAllTokens();


}