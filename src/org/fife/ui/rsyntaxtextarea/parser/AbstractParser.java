/*
 * 07/31/2009
 *
 * AbstractParser.java - A base implementation for parsers.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.parser;

import java.net.URL;

import org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip;


/**
 * A base class for {@link Parser} implementations.  Most <code>Parser</code>s
 * should be able to extend this class.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class AbstractParser implements Parser {

	/**
	 * Whether this parser is enabled.  If this is <code>false</code>, then
	 * this parser will not be run.
	 */
	private boolean enabled;

	/**
	 * Listens for events from {@link FocusableTip}s generated from this
	 * parser's notices.
	 */
	private ExtendedHyperlinkListener linkListener;


	/**
	 * Constructor.
	 */
	protected AbstractParser() {
		setEnabled(true);
	}


	/**
	 * {@inheritDoc}
	 */
	public ExtendedHyperlinkListener getHyperlinkListener() {
		return linkListener;
	}


	/**
	 * Returns <code>null</code>.  Parsers that wish to show images in their
	 * tool tips should override this method to return the image base URL.
	 *
	 * @return <code>null</code> always.
	 */
	public URL getImageBase() {
		return null;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean isEnabled() {
		return enabled;
	}


	/**
	 * Toggles whether this parser is enabled.
	 *
	 * @param enabled Whether this parser is enabled.
	 * @see #isEnabled()
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


	/**
	 * Returns the listener for this parser.
	 *
	 * @param listener The new listener.
	 * @see #getHyperlinkListener()
	 */
	public void setHyperlinkListener(ExtendedHyperlinkListener listener) {
		linkListener = listener;
	}


}