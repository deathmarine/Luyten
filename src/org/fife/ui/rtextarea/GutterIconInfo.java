/*
 * 02/19/2009
 *
 * GutterIconInfo.java - Information about an Icon in a Gutter.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import javax.swing.Icon;


/**
 * Information about an icon displayed in a {@link Gutter}.  Instances of this
 * class are returned by {@link Gutter#addLineTrackingIcon(int, Icon)} and
 * {@link Gutter#addOffsetTrackingIcon(int, Icon)}.  They can later be used
 * in calls to {@link Gutter#removeTrackingIcon(GutterIconInfo)} to be
 * individually removed.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see Gutter
 */
public interface GutterIconInfo {


	/**
	 * Returns the icon being rendered.
	 *
	 * @return The icon being rendered.
	 */
	public Icon getIcon();


	/**
	 * Returns the offset that is being tracked.  The line of this offset is
	 * where the icon is rendered.  This offset may change as the user types
	 * to track the new location of the marked offset.
	 *
	 * @return The offset being tracked.
	 */
	public int getMarkedOffset();


	/**
	 * Returns the tool tip to display when the mouse hovers over this icon.
	 *
	 * @return The tool tip to display.
	 */
	public String getToolTip();

}