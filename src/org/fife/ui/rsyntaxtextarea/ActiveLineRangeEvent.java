/*
 * 02/06/2011
 *
 * ActiveLineRangeEvent.java - Notifies listeners of an "active line range"
 * change in an RSyntaxTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.EventObject;


/**
 * The event fired by {@link RSyntaxTextArea}s when the active line range
 * changes.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class ActiveLineRangeEvent extends EventObject {

	private int min;
	private int max;


	/**
	 * Constructor.
	 *
	 * @param source The text area.
	 * @param min The first line in the active line range, or
	 *        <code>-1</code> if the line range is being cleared.
	 * @param max The last line in the active line range, or
	 *        <code>-1</code> if the line range is being cleared.
	 */
	public ActiveLineRangeEvent(RSyntaxTextArea source, int min, int max) {
		super(source);
		this.min = min;
		this.max = max;
	}


	/**
	 * Returns the last line in the active line range.
	 *
	 * @return The last line, or <code>-1</code> if the range is being
	 *         cleared.
	 * @see #getMin()
	 */
	public int getMax() {
		return max;
	}


	/**
	 * Returns the first line in the active line range.
	 *
	 * @return The first line, or <code>-1</code> if the range is being
	 *         cleared.
	 * @see #getMax()
	 */
	public int getMin() {
		return min;
	}


}