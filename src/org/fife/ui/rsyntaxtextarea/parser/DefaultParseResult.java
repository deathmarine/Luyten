/*
 * 07/27/2009
 *
 * DefaultParseResult.java - A basic implementation of a ParseResult.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.parser;

import java.util.ArrayList;
import java.util.List;


/**
 * A basic implementation of {@link ParseResult}.  Most, if not all,
 * <code>Parser</code>s can return instances of this class.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see Parser
 */
public class DefaultParseResult implements ParseResult {

	private Parser parser;
	private int firstLineParsed;
	private int lastLineParsed;
	private List notices;
	private long parseTime;
	private Exception error;


	public DefaultParseResult(Parser parser) {
		this.parser = parser;
		notices = new ArrayList();
	}


	/**
	 * Adds a parser notice.
	 *
	 * @param notice The new notice.
	 * @see #clearNotices()
	 */
	public void addNotice(ParserNotice notice) {
		notices.add(notice);
	}


	/**
	 * Clears any parser notices in this result.
	 *
	 * @see #addNotice(ParserNotice)
	 */
	public void clearNotices() {
		notices.clear();
	}


	/**
	 * {@inheritDoc}
	 */
	public Exception getError() {
		return error;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getFirstLineParsed() {
		return firstLineParsed;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getLastLineParsed() {
		return lastLineParsed;
	}


	/**
	 * {@inheritDoc}
	 */
	public List getNotices() {
		return notices;
	}


	/**
	 * {@inheritDoc}
	 */
	public long getParseTime() {
		return parseTime;
	}


	/**
	 * {@inheritDoc}
	 */
	public Parser getParser() {
		return parser;
	}


	/**
	 * Sets the error that occurred when last parsing the document, if
	 * any.
	 *
	 * @param e The error that occurred, or <code>null</code> if no error
	 *         occurred.
	 */
	public void setError(Exception e) {
		this.error = e;
	}


	/**
	 * Sets the amount of time it took for this parser to parse the document.
	 *
	 * @param time The amount of time, in milliseconds.
	 * @see #getParseTime()
	 */
	public void setParseTime(long time) {
		parseTime = time;
	}


	/**
	 * Sets the line range parsed.
	 *
	 * @param first The first line parsed, inclusive.
	 * @param last The last line parsed, inclusive.
	 * @see #getFirstLineParsed()
	 * @see #getLastLineParsed()
	 */
	public void setParsedLines(int first, int last) {
		firstLineParsed = first;
		lastLineParsed = last;
	}


}