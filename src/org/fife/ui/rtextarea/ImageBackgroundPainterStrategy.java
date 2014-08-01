/*
 * 01/22/2005
 *
 * ImageBackgroundPainterStrategy.java - Renders an RTextAreaBase's
 * background as an image.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;


/**
 * A strategy for painting the background of an <code>RTextAreaBase</code>
 * as an image.  The image is always stretched to completely fill the
 * <code>RTextAreaBase</code>.<p>
 *
 * You can set the scaling hint used when stretching/skewing the image
 * to fit in the <code>RTextAreaBase</code>'s background via the
 * <code>setScalingHint</code> method, but keep in mind the more
 * accurate the scaling hint, the less responsive your application will
 * be when stretching the window (as that's the only time the image's
 * size is recalculated).
 *
 * @author Robert Futrell
 * @version 0.1
 * @see org.fife.ui.rtextarea.BufferedImageBackgroundPainterStrategy
 * @see org.fife.ui.rtextarea.VolatileImageBackgroundPainterStrategy
 */
public abstract class ImageBackgroundPainterStrategy
					implements BackgroundPainterStrategy {

	protected MediaTracker tracker;

	private RTextAreaBase textArea;
	private Image master;
	private int oldWidth, oldHeight;
	private int scalingHint;


	/**
	 * Constructor.
	 *
	 * @param textArea The text area using this image as its background.
	 */
	public ImageBackgroundPainterStrategy(RTextAreaBase textArea) {
		this.textArea = textArea;
		tracker = new MediaTracker(textArea);
		scalingHint = Image.SCALE_FAST;
	}


	/**
	 * Returns the text area using this strategy.
	 *
	 * @return The text area.
	 */
	public RTextAreaBase getRTextAreaBase() {
		return textArea;
	}


	/**
	 * Returns the "master" image; that is, the original, unscaled image.
	 * When the image needs to be rescaled, scaling should be done from
	 * this image, to prevent repeated scaling from distorting the image.
	 *
	 * @return The master image.
	 */
	public Image getMasterImage() {
		return master;
	}


	/**
	 * Returns the scaling hint being used.
	 *
	 * @return The scaling hint to use when scaling an image.
	 * @see #setScalingHint
	 */
	public int getScalingHint() {
		return scalingHint;
	}


	/**
	 * Paints the image at the specified location and at the specified size.
	 *
	 * @param g The graphics context.
	 * @param bounds The bounds in which to paint the image.  The image
	 *        will be scaled to fit exactly in these bounds if necessary.
	 */
	public final void paint(Graphics g, Rectangle bounds) {
		if (bounds.width!=oldWidth || bounds.height!=oldHeight) {
			rescaleImage(bounds.width, bounds.height, getScalingHint());
			oldWidth = bounds.width;
			oldHeight = bounds.height;
		}
		paintImage(g, bounds.x,bounds.y);
	}


	/**
	 * Paints the image at the specified location.  This method assumes
	 * scaling has already been done, and simply paints the background
	 * image "as-is."
	 *
	 * @param g The graphics context.
	 * @param x The x-coordinate at which to paint.
	 * @param y The y-coordinate at which to paint.
	 */
	protected abstract void paintImage(Graphics g, int x, int y);


	/**
	 * Rescales the displayed image to be the specified size.
	 *
	 * @param width The new width of the image.
	 * @param height The new height of the image.
	 * @param hint The scaling hint to use.
	 */
	protected abstract void rescaleImage(int width, int height,
								int hint);


	/**
	 * Sets the image this background painter displays.
	 *
	 * @param imageURL URL of a file containing the image to display.
	 */
	public void setImage(URL imageURL) {
		BufferedImage image = null;
		try {
			image = ImageIO.read(imageURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		setImage(image);
	}

	/**
	 * Sets the image this background painter displays.
	 *
	 * @param image The new image to use for the background.
	 */
	public void setImage(Image image) {
		master = image;
		oldWidth = -1; // To trick us into fixing bgImage.
	}


	/**
	 * Sets the scaling hint to use when scaling the image.
	 *
	 * @param hint The hint to apply; e.g. <code>Image.SCALE_DEFAULT</code>.
	 * @see #getScalingHint
	 */
	public void setScalingHint(int hint) {
		this.scalingHint = hint;
	}


}