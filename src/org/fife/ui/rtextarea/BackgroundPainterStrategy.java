/*
 * 01/22/2005
 *
 * BackgroundPainterStrategy.java - Renders an RTextAreaBase's background
 * using some strategy.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Graphics;
import java.awt.Rectangle;


/**
 * Interface for classes that paint the background of an
 * <code>RTextAreaBase</code>.  The Strategy pattern is used for this
 * object because the background can be painted as a solid color, as
 * an image, and possibly other ways (gradients, animated images, etc.).
 * When a method to change the background of an <code>RTextAreaBase</code>
 * instance is called (such as <code>setBackground</code>,
 * <code>setBackgroundImage</code> or <code>setBackgoundObject</code>),
 * the correct strategy is then created and used to paint its background.
 *
 * @author Robert Futrell
 * @version 0.1
 * @see org.fife.ui.rtextarea.ImageBackgroundPainterStrategy
 * @see org.fife.ui.rtextarea.ColorBackgroundPainterStrategy
 */
public interface BackgroundPainterStrategy {


	/**
	 * Paints the background.
	 *
	 * @param g The graphics context.
	 * @param bounds The bounds of the object whose backgrouns we're
	 *        painting.
	 */
	public void paint(Graphics g, Rectangle bounds);


}