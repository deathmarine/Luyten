/*
 * 01/22/2005
 *
 * ColorBackgroundPainterStrategy.java - Renders an RTextAreaBase's background
 * as a single color.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;


/**
 * A strategy for painting the background of an <code>RTextAreaBase</code>
 * as a solid color.  The default background for <code>RTextAreaBase</code>s
 * is this strategy using the color white.
 *
 * @author Robert Futrell
 * @version 0.1
 * @see org.fife.ui.rtextarea.ImageBackgroundPainterStrategy
 */
public class ColorBackgroundPainterStrategy
				implements BackgroundPainterStrategy {

	private Color color;


	/**
	 * Constructor.
	 *
	 * @param color The color to use when painting the background.
	 */
	public ColorBackgroundPainterStrategy(Color color) {
		setColor(color);
	}


	/**
	 * Returns whether or not the specified object is equivalent to
	 * this one.
	 *
	 * @param o2 The object to which to compare.
	 * @return Whether <code>o2</code> is another
	 *         <code>ColorBackgroundPainterStrategy</code> representing
	 *         the same color as this one.
	 */
	public boolean equals(Object o2) {
		return o2!=null &&
			(o2 instanceof ColorBackgroundPainterStrategy) &&
			this.color.equals(
				((ColorBackgroundPainterStrategy)o2).getColor());
	}


	/**
	 * Returns the color used to paint the background.
	 *
	 * @return The color.
	 * @see #setColor
	 */
	public Color getColor() {
		return color;
	}


	/**
	 * Returns the hash code to use when placing an object of this type into
	 * hash maps.  This method is implemented since we overrode
	 * {@link #equals(Object)}, to keep FindBugs happy.
	 *
	 * @return The hash code.
	 */
	public int hashCode() {
		return color.hashCode();
	}


	/**
	 * Paints the background.
	 *
	 * @param g The graphics context.
	 * @param bounds The bounds of the object whose backgrouns we're
	 *        painting.
	 */
	public void paint(Graphics g, Rectangle bounds) {
		Color temp = g.getColor();
		g.setColor(color);
		g.fillRect(bounds.x,bounds.y, bounds.width,bounds.height);
		g.setColor(temp);
	}


	/**
	 * Sets the color used to paint the background.
	 *
	 * @param color The color to use.
	 * @see #getColor
	 */
	public void setColor(Color color) {
		this.color = color;
	}


}