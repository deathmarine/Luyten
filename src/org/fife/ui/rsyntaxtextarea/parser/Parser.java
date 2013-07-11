/*
 * 09/23/2005
 *
 * Parser.java - An interface for a parser for RSyntaxTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.parser;

import java.net.URL;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip;


/**
 * An interface for a parser for content in an {@link RSyntaxTextArea}.
 * A <code>Parser</code> returns a list of issues it finds in the text area's
 * content, which the text area can flag (e.g. squiggle underline).  It can
 * also return descriptions of the issues, to be used in tool tips.<p>
 * 
 * To install a <code>Parser</code>, simply call
 * {@link RSyntaxTextArea#addParser(Parser)}.
 *
 * @author Robert Futrell
 * @version 0.5
 * @see AbstractParser
 */
public interface Parser {


	/**
	 * Returns the listener for hyperlink events from {@link FocusableTip}s,
	 * or <code>null</code> if none.
	 *
	 * @return The listener.
	 */
	public ExtendedHyperlinkListener getHyperlinkListener();


	/**
	 * Returns the base URL for any images displayed in returned
	 * {@link ParserNotice} HTML text.  Note that if a parser notice's text
	 * is not HTML, this URL is not used.
	 *
	 * @return The URL.  This may be <code>null</code>.
	 */
	public URL getImageBase();


	/**
	 * Returns whether this parser is enabled.  If this returns
	 * <code>false</code>, it will not be run.
	 *
	 * @return Whether this parser is enabled.
	 */
	public boolean isEnabled();


	/**
	 * Parses input from the specified document.
	 *
	 * @param doc The document to parse.  This document is in a read lock,
	 *        so it cannot be modified while parsing is occurring.
	 * @param style The language being rendered, such as
	 *        {@link SyntaxConstants#SYNTAX_STYLE_JAVA}.
	 * @return An object describing the section of the document parsed and the
	 *         results.  This is guaranteed to be non-<code>null</code>.
	 */
	public ParseResult parse(RSyntaxDocument doc, String style);


}