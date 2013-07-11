/*
 * 08/11/2009
 *
 * DefaultParserNotice.java - Base implementation of a parser notice.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.parser;

import java.awt.Color;


/**
 * Base implementation of a parser notice.  Most <code>Parser</code>
 * implementations can return instances of this in their parse result.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see Parser
 * @see ParseResult
 */
public class DefaultParserNotice implements ParserNotice {

	private Parser parser;
	private int level;
	private int line;
	private int offset;
	private int length;
	private boolean showInEditor;
	private Color color;
	private String message;
	private String toolTipText;

	private static final Color[] DEFAULT_COLORS = {
		new Color(255, 0, 128),		// Error
		new Color(244, 200, 45),	// Warning
		Color.gray,					// Info
	};


	/**
	 * Constructor.
	 *
	 * @param parser The parser that created this notice.
	 * @param msg The text of the message.
	 * @param line The line number for the message.
	 */
	public DefaultParserNotice(Parser parser, String msg, int line) {
		this(parser, msg, line, -1, -1);
	}


	/**
	 * Constructor.
	 *
	 * @param parser The parser that created this notice.
	 * @param message The message.
	 * @param line The line number corresponding to the message.
	 * @param offset The offset in the input stream of the code the
	 *        message is concerned with, or <code>-1</code> if unknown.
	 * @param length The length of the code the message is concerned with,
	 *        or <code>-1</code> if unknown.
	 */
	public DefaultParserNotice(Parser parser, String message, int line,
						int offset, int length) {
		this.parser = parser;
		this.message = message;
		this.line = line;
		this.offset = offset;
		this.length = length;
		setLevel(ERROR);
		setShowInEditor(true);
	}


	/**
	 * Compares this parser notice to another.
	 *
	 * @param obj Another parser notice.
	 * @return How the two parser notices should be sorted relative to one
	 *         another.
	 */
	public int compareTo(Object obj) {
		int diff = -1;
		if (obj instanceof ParserNotice) {
			ParserNotice p2 = (ParserNotice)obj;
			diff = level - p2.getLevel();
			if (diff==0) {
				diff = line - p2.getLine();
				if (diff==0) {
					diff = message.compareTo(p2.getMessage());
				}
			}
		}
		return diff;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean containsPosition(int pos) {
		return offset<=pos && pos<(offset+length);
	}


	/**
	 * Returns whether this parser notice is equal to another one.
	 *
	 * @param obj Another parser notice.
	 * @return Whether the two notices are equal.
	 */
	public boolean equals(Object obj) {
		return compareTo(obj)==0;
	}


	/**
	 * {@inheritDoc}
	 */
	public Color getColor() {
		Color c = color; // User-defined
		if (c==null) {
			c = DEFAULT_COLORS[getLevel()];
		}
		return c;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getLength() {
		return length;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getLevel() {
		return level;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getLine() {
		return line;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getMessage() {
		return message;
	}


	/**
	 * {@inheritDoc}
	 */
	public int getOffset() {
		return offset;
	}


	/**
	 * {@inheritDoc}
	 */
	public Parser getParser() {
		return parser;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean getShowInEditor() {
		return showInEditor;
	}


	/**
	 * {@inheritDoc}
	 */
	public String getToolTipText() {
		return toolTipText!=null ? toolTipText : getMessage();
	}


	/**
	 * Returns the hash code for this notice.
	 *
	 * @return The hash code.
	 */
	public int hashCode() {
		return (line<<16) | offset;
	}


	/**
	 * Sets the color to use when painting this notice.
	 *
	 * @param color The color to use.
	 * @see #getColor()
	 */
	public void setColor(Color color) {
		this.color = color;
	}


	/**
	 * Sets the level of this notice.
	 *
	 * @param level The new level.
	 * @see #getLevel()
	 */
	public void setLevel(int level) {
		if (level>INFO) {
			level = INFO;
		}
		else if (level<ERROR) {
			level = ERROR;
		}
		this.level = level;
	}


	/**
	 * Sets whether a squiggle underline should be drawn in the editor for
	 * this notice.
	 *
	 * @param show Whether to draw a squiggle underline.
	 * @see #getShowInEditor()
	 */
	public void setShowInEditor(boolean show) {
		showInEditor = show;
	}


	/**
	 * Sets the tooltip text to display for this notice.
	 *
	 * @param text The new tooltip text.  This can be HTML.  If this is
	 *        <code>null</code>, then tooltips will return the same text as
	 *        {@link #getMessage()}.
	 * @see #getToolTipText()
	 */
	public void setToolTipText(String text) {
		this.toolTipText = text;
	}


	/**
	 * Returns a string representation of this parser notice.
	 *
	 * @return This parser notice as a string.
	 */
	public String toString() {
		return "Line " + getLine() + ": " + getMessage();
	}


}