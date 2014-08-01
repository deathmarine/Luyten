/*
 * 10/28/2004
 *
 * VisibleWhitespaceTokenFactory.java - Visible whitespace token factory.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;


/**
 * Token factory that generates tokens that display special symbols for the
 * whitespace characters space and tab.<p>
 *
 * NOTE:  This class should only be used by {@link TokenMaker}; nobody else
 * needs it!
 *
 * @author Robert Futrell
 * @version 0.1
 */
class VisibleWhitespaceTokenFactory extends DefaultTokenFactory {


	/**
	 * Cosnstructor.
	 */
	public VisibleWhitespaceTokenFactory() {
		this(DEFAULT_START_SIZE, DEFAULT_INCREMENT);
	}


	/**
	 * Constructor.
	 *
	 * @param size The initial number of tokens in this factory.
	 * @param increment How many tokens to increment by when the stack gets
	 *        empty.
	 */
	public VisibleWhitespaceTokenFactory(int size, int increment) {
		super(size, increment);
	}

	/**
	 * Creates a token for use internally by this token factory.  This method
	 * should NOT be called externally; only by this class and possibly
	 * subclasses.
	 *
	 * @return A token to add to this token factory's internal stack.
	 */
	protected Token createInternalUseOnlyToken() {
		return new VisibleWhitespaceToken();
	}


}