/*
 * 08/08/2012
 *
 * LispFoldParser.java - Fold parser for Lisp and related languages.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import org.fife.ui.rsyntaxtextarea.Token;


/**
 * Fold parser for Lisp and related languages.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class LispFoldParser extends CurlyFoldParser {


	public boolean isLeftCurly(Token t) {
		return t.isSingleChar(Token.SEPARATOR, '(');
	}


	public boolean isRightCurly(Token t) {
		return t.isSingleChar(Token.SEPARATOR, ')');
	}


}