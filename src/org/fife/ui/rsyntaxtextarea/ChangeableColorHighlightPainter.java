/*
 * 07/23/2009
 *
 * ChangeableColorHighlightPainter.java - A highlighter whose color you can
 * change.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;


/**
 * A highlighter whose color you can change.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class ChangeableColorHighlightPainter extends DefaultHighlightPainter {

	/**
	 * DefaultHighlightPainter doesn't allow changing color, so we must cache
	 * ours here.
	 */
	private Color color;


	/**
	 * Constructor.
	 *
	 * @param color The initial color to use.  This cannot be <code>null</code>.
	 */
	public ChangeableColorHighlightPainter(Color color) {
		super(color);
		setColor(color);
	}


	/**
	 * Returns the color to paint with.
	 *
	 * @return The color.
	 * @see #setColor(Color)
	 */
	public Color getColor() {
		return color;
	}


	/**
	 * Sets the color to paint the bounding boxes with.
	 *
	 * @param color The new color.  This cannot be <code>null</code>.
	 * @see #getColor()
	 */
	public void setColor(Color color) {
		if (color==null) {
			throw new IllegalArgumentException("color cannot be null");
		}
		this.color = color;
	}


}