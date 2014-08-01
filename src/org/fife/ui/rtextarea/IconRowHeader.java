/*
 * 02/17/2009
 *
 * IconRowHeader.java - Renders icons in the gutter.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;

import org.fife.ui.rsyntaxtextarea.FoldingAwareIconRowHeader;


/**
 * Renders icons in the {@link Gutter}.  This can be used to visually mark
 * lines containing syntax errors, lines with breakpoints set on them, etc.<p>
 *
 * This component has built-in support for displaying icons representing
 * "bookmarks;" that is, lines a user can cycle through via F2 and Shift+F2.
 * Bookmarked lines are toggled via Ctrl+F2, or by clicking in the icon area
 * at the line to bookmark.  In order to enable bookmarking, you must first
 * assign an icon to represent a bookmarked line, then actually enable the
 * feature.  This is actually done on the parent {@link Gutter} component:<p>
 * 
 * <pre>
 * Gutter gutter = scrollPane.getGutter();
 * gutter.setBookmarkIcon(new ImageIcon("bookmark.png"));
 * gutter.setBookmarkingEnabled(true);
 * </pre>
 *
 * @author Robert Futrell
 * @version 1.0
 * @see FoldingAwareIconRowHeader
 */
public class IconRowHeader extends AbstractGutterComponent implements MouseListener {

	/**
	 * The icons to render.
	 */
	protected List trackingIcons;

	/**
	 * The width of this component.
	 */
	protected int width;

	/**
	 * Whether this component listens for mouse clicks and toggles "bookmark"
	 * icons on them.
	 */
	private boolean bookmarkingEnabled;

	/**
	 * The icon to use for bookmarks.
	 */
	private Icon bookmarkIcon;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on
	 * each paint.
	 */
	protected Rectangle visibleRect;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on
	 * each paint.
	 */
	protected Insets textAreaInsets;

	/**
	 * The first line in the active line range.
	 */
	protected int activeLineRangeStart;

	/**
	 * The end line in the active line range.
	 */
	protected int activeLineRangeEnd;

	/**
	 * The color used to highlight the active code block.
	 */
	private Color activeLineRangeColor;


	/**
	 * Constructor.
	 *
	 * @param textArea The parent text area.
	 */
	public IconRowHeader(RTextArea textArea) {

		super(textArea);
		visibleRect = new Rectangle();
		width = 16;
		addMouseListener(this);
		activeLineRangeStart = activeLineRangeEnd = -1;
		setActiveLineRangeColor(null);

		// Must explicitly set our background color, otherwise we inherit that
		// of the parent Gutter.
		updateBackground();

		ToolTipManager.sharedInstance().registerComponent(this);

	}


	/**
	 * Adds an icon that tracks an offset in the document, and is displayed
	 * adjacent to the line numbers.  This is useful for marking things such
	 * as source code errors.
	 *
	 * @param offs The offset to track.
	 * @param icon The icon to display.  This should be small (say 16x16).
	 * @return A tag for this icon.
	 * @throws BadLocationException If <code>offs</code> is an invalid offset
	 *         into the text area.
	 * @see #removeTrackingIcon(Object)
	 */
	public GutterIconInfo addOffsetTrackingIcon(int offs, Icon icon)
												throws BadLocationException {
		return addOffsetTrackingIcon(offs, icon, null);
	}


	/**
	 * Adds an icon that tracks an offset in the document, and is displayed
	 * adjacent to the line numbers.  This is useful for marking things such
	 * as source code errors.
	 *
	 * @param offs The offset to track.
	 * @param icon The icon to display.  This should be small (say 16x16).
	 * @param tip A tool tip for the icon.
	 * @return A tag for this icon.
	 * @throws BadLocationException If <code>offs</code> is an invalid offset
	 *         into the text area.
	 * @see #removeTrackingIcon(Object)
	 */
	public GutterIconInfo addOffsetTrackingIcon(int offs, Icon icon, String tip)
												throws BadLocationException {
		Position pos = textArea.getDocument().createPosition(offs);
		GutterIconImpl ti = new GutterIconImpl(icon, pos, tip);
		if (trackingIcons==null) {
			trackingIcons = new ArrayList(1); // Usually small
		}
		int index = Collections.binarySearch(trackingIcons, ti);
		if (index<0) {
			index = -(index+1);
		}
		trackingIcons.add(index, ti);
		repaint();
		return ti;
	}


	/**
	 * Clears the active line range.
	 *
	 * @see #setActiveLineRange(int, int)
	 */
	public void clearActiveLineRange() {
		if (activeLineRangeStart!=-1 || activeLineRangeEnd!=-1) {
			activeLineRangeStart = activeLineRangeEnd = -1;
			repaint();
		}
	}


	/**
	 * Returns the color used to paint the active line range, if any.
	 *
	 * @return The color.
	 * @see #setActiveLineRangeColor(Color)
	 */
	public Color getActiveLineRangeColor() {
		return activeLineRangeColor;
	}


	/**
	 * Returns the icon to use for bookmarks.
	 *
	 * @return The icon to use for bookmarks.  If this is <code>null</code>,
	 *         bookmarking is effectively disabled.
	 * @see #setBookmarkIcon(Icon)
	 * @see #isBookmarkingEnabled()
	 */
	public Icon getBookmarkIcon() {
		return bookmarkIcon;
	}


	/**
	 * Returns the bookmarks known to this gutter.
	 *
	 * @return The bookmarks.  If there are no bookmarks, an empty array is
	 *         returned.
	 */
	public GutterIconInfo[] getBookmarks() {

		List retVal = new ArrayList(1);

		if (trackingIcons!=null) {
			for (int i=0; i<trackingIcons.size(); i++) {
				GutterIconImpl ti = getTrackingIcon(i);
				if (ti.getIcon()==bookmarkIcon) {
					retVal.add(ti);
				}
			}
		}

		GutterIconInfo[] array = new GutterIconInfo[retVal.size()];
		return (GutterIconInfo[])retVal.toArray(array);

	}


	/**
	 * {@inheritDoc}
	 */
	void handleDocumentEvent(DocumentEvent e) {
		int newLineCount = textArea.getLineCount();
		if (newLineCount!=currentLineCount) {
			currentLineCount = newLineCount;
			repaint();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	public Dimension getPreferredSize() {
		int h = textArea!=null ? textArea.getHeight() : 100; // Arbitrary
		return new Dimension(width, h);
	}


	/**
	 * Overridden to display the tool tip of any icons on this line.
	 *
	 * @param e The location the mouse is hovering over.
	 */
	public String getToolTipText(MouseEvent e) {
		try {
			int line = viewToModelLine(e.getPoint());
			if (line>-1) {
				GutterIconInfo[] infos = getTrackingIcons(line);
				if (infos.length>0) {
					// TODO: Display all messages?
					return infos[infos.length-1].getToolTip();
				}
			}
		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Never happens
		}
		return null;
	}


	protected GutterIconImpl getTrackingIcon(int index) {
		return (GutterIconImpl)trackingIcons.get(index);
	}


	/**
	 * Returns the tracking icons at the specified line.
	 *
	 * @param line The line.
	 * @return The tracking icons at that line.  If there are no tracking
	 *         icons there, this will be an empty array.
	 * @throws BadLocationException If <code>line</code> is invalid.
	 */
	public GutterIconInfo[] getTrackingIcons(int line)
								throws BadLocationException {

		List retVal = new ArrayList(1);

		if (trackingIcons!=null) {
			int start = textArea.getLineStartOffset(line);
			int end = textArea.getLineEndOffset(line);
			if (line==textArea.getLineCount()-1) {
				end++; // Hack
			}
			for (int i=0; i<trackingIcons.size(); i++) {
				GutterIconImpl ti = getTrackingIcon(i);
				int offs = ti.getMarkedOffset();
				if (offs>=start && offs<end) {
					retVal.add(ti);
				}
				else if (offs>=end) {
					break; // Quit early
				}
			}
		}

		GutterIconInfo[] array = new GutterIconInfo[retVal.size()];
		return (GutterIconInfo[])retVal.toArray(array);

	}


	/**
	 * Returns whether bookmarking is enabled.
	 *
	 * @return Whether bookmarking is enabled.
	 * @see #setBookmarkingEnabled(boolean)
	 */
	public boolean isBookmarkingEnabled() {
		return bookmarkingEnabled;
	}


	/**
	 * {@inheritDoc}
	 */
	void lineHeightsChanged() {
		repaint();
	}


	public void mouseClicked(MouseEvent e) {
	}


	public void mouseEntered(MouseEvent e) {
	}


	public void mouseExited(MouseEvent e) {
	}


	public void mousePressed(MouseEvent e) {
		if (bookmarkingEnabled && bookmarkIcon!=null) {
			try {
				int line = viewToModelLine(e.getPoint());
				if (line>-1) {
					toggleBookmark(line);
				}
			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}
	}


	public void mouseReleased(MouseEvent e) {
	}


	/**
	 * {@inheritDoc}
	 */
	protected void paintComponent(Graphics g) {

		if (textArea==null) {
			return;
		}

		visibleRect = g.getClipBounds(visibleRect);
		if (visibleRect==null) { // ???
			visibleRect = getVisibleRect();
		}
		//System.out.println("IconRowHeader repainting: " + visibleRect);
		if (visibleRect==null) {
			return;
		}

		g.setColor(getBackground());
		g.fillRect(0,visibleRect.y, width,visibleRect.height);

		if (textArea.getLineWrap()) {
			paintComponentWrapped(g);
			return;
		}

		Document doc = textArea.getDocument();
		Element root = doc.getDefaultRootElement();
		textAreaInsets = textArea.getInsets(textAreaInsets);

		// Get the first and last lines to paint.
		int cellHeight = textArea.getLineHeight();
		int topLine = (visibleRect.y-textAreaInsets.top)/cellHeight;
		int bottomLine = Math.min(topLine+visibleRect.height/cellHeight+1,
							root.getElementCount());

		// Get where to start painting (top of the row).
		// We need to be "scrolled up" up just enough for the missing part of
		// the first line.
		int y = topLine*cellHeight + textAreaInsets.top;

		if ((activeLineRangeStart>=topLine&&activeLineRangeStart<=bottomLine) ||
			(activeLineRangeEnd>=topLine && activeLineRangeEnd<=bottomLine) ||
			(activeLineRangeStart<=topLine && activeLineRangeEnd>=bottomLine)) {

			g.setColor(activeLineRangeColor);
			int firstLine = Math.max(activeLineRangeStart, topLine);
			int y1 = firstLine * cellHeight + textAreaInsets.top;
			int lastLine = Math.min(activeLineRangeEnd, bottomLine);
			int y2 = (lastLine+1) * cellHeight + textAreaInsets.top - 1;

			int j = y1;
			while (j<=y2) {
				int yEnd = Math.min(y2, j+getWidth());
				int xEnd = yEnd-j;
				g.drawLine(0,j, xEnd,yEnd);
				j += 2;
			}

			int i = 2;
			while (i<getWidth()) {
				int yEnd = y1 + getWidth() - i;
				g.drawLine(i,y1, getWidth(),yEnd);
				i += 2;
			}

			if (firstLine==activeLineRangeStart) {
				g.drawLine(0,y1, getWidth(),y1);
			}
			if (lastLine==activeLineRangeEnd) {
				g.drawLine(0,y2, getWidth(),y2);
			}

		}

		if (trackingIcons!=null) {
			int lastLine = bottomLine;
			for (int i=trackingIcons.size()-1; i>=0; i--) { // Last to first
				GutterIconInfo ti = getTrackingIcon(i);
				int offs = ti.getMarkedOffset();
				if (offs>=0 && offs<=doc.getLength()) {
					int line = root.getElementIndex(offs);
					if (line<=lastLine && line>=topLine) {
						Icon icon = ti.getIcon();
						if (icon!=null) {
							int y2 = y + (line-topLine)*cellHeight;
							y2 += (cellHeight-icon.getIconHeight())/2;
							ti.getIcon().paintIcon(this, g, 0, y2);
							lastLine = line-1; // Paint only 1 icon per line
						}
					}
					else if (line<topLine) {
						break;
					}
				}
			}
		}

	}


	/**
	 * Paints icons when line wrapping is enabled.
	 *
	 * @param g The graphics context.
	 */
	private void paintComponentWrapped(Graphics g) {

		// The variables we use are as follows:
		// - visibleRect is the "visible" area of the text area; e.g.
		// [0,100, 300,100+(lineCount*cellHeight)-1].
		// actualTop.y is the topmost-pixel in the first logical line we
		// paint.  Note that we may well not paint this part of the logical
		// line, as it may be broken into many physical lines, with the first
		// few physical lines scrolled past.  Note also that this is NOT the
		// visible rect of this line number list; this line number list has
		// visible rect == [0,0, insets.left-1,visibleRect.height-1].
		// - offset (<=0) is the y-coordinate at which we begin painting when
		// we begin painting with the first logical line.  This can be
		// negative, signifying that we've scrolled past the actual topmost
		// part of this line.

		// The algorithm is as follows:
		// - Get the starting y-coordinate at which to paint.  This may be
		//   above the first visible y-coordinate as we're in line-wrapping
		//   mode, but we always paint entire logical lines.
		// - Paint that line's line number and highlight, if appropriate.
		//   Increment y to be just below the are we just painted (i.e., the
		//   beginning of the next logical line's view area).
		// - Get the ending visual position for that line.  We can now loop
		//   back, paint this line, and continue until our y-coordinate is
		//   past the last visible y-value.

		// We avoid using modelToView/viewToModel where possible, as these
		// methods trigger a parsing of the line into syntax tokens, which is
		// costly.  It's cheaper to just grab the child views' bounds.

		RTextAreaUI ui = (RTextAreaUI)textArea.getUI();
		View v = ui.getRootView(textArea).getView(0);
//		boolean currentLineHighlighted = textArea.getHighlightCurrentLine();
		Document doc = textArea.getDocument();
		Element root = doc.getDefaultRootElement();
		int lineCount = root.getElementCount();
		int topPosition = textArea.viewToModel(
								new Point(visibleRect.x,visibleRect.y));
		int topLine = root.getElementIndex(topPosition);

		// Compute the y at which to begin painting text, taking into account
		// that 1 logical line => at least 1 physical line, so it may be that
		// y<0.  The computed y-value is the y-value of the top of the first
		// (possibly) partially-visible view.
		Rectangle visibleEditorRect = ui.getVisibleEditorRect();
		Rectangle r = IconRowHeader.getChildViewBounds(v, topLine,
												visibleEditorRect);
		int y = r.y;

		int visibleBottom = visibleRect.y + visibleRect.height;

		// Get the first possibly visible icon index.
		int currentIcon = -1;
		if (trackingIcons!=null) {
			for (int i=0; i<trackingIcons.size(); i++) {
				GutterIconImpl icon = getTrackingIcon(i);
				int offs = icon.getMarkedOffset();
				if (offs>=0 && offs<=doc.getLength()) {
					int line = root.getElementIndex(offs);
					if (line>=topLine) {
						currentIcon = i;
						break;
					}
				}
			}
		}

		// Keep painting lines until our y-coordinate is past the visible
		// end of the text area.
		g.setColor(getForeground());
		int cellHeight = textArea.getLineHeight();
		while (y < visibleBottom) {

			r = getChildViewBounds(v, topLine, visibleEditorRect);
//			int lineEndY = r.y+r.height;

			/*
			// Highlight the current line's line number, if desired.
			if (currentLineHighlighted && topLine==currentLine) {
				g.setColor(textArea.getCurrentLineHighlightColor());
				g.fillRect(0,y, width,lineEndY-y);
				g.setColor(getForeground());
			}
			*/

			// Possibly paint an icon.
			if (currentIcon>-1) {
				// We want to paint the last icon added for this line.
				GutterIconImpl toPaint = null;
				while (currentIcon<trackingIcons.size()) {
					GutterIconImpl ti = getTrackingIcon(currentIcon);
					int offs = ti.getMarkedOffset();
					if (offs>=0 && offs<=doc.getLength()) {
						int line = root.getElementIndex(offs);
						if (line==topLine) {
							toPaint = ti;
						}
						else if (line>topLine) {
							break;
						}
					}
					currentIcon++;
				}
				if (toPaint!=null) {
					Icon icon = toPaint.getIcon();
					if (icon!=null) {
						int y2 = y + (cellHeight-icon.getIconHeight())/2;
						icon.paintIcon(this, g, 0, y2);
					}
				}
			}

			// The next possible y-coordinate is just after the last line
			// painted.
			y += r.height;

			// Update topLine (we're actually using it for our "current line"
			// variable now).
			topLine++;
			if (topLine>=lineCount)
				break;

		}

	}


	/**
	 * Removes the specified tracking icon.
	 *
	 * @param tag A tag for a tracking icon.
	 * @see #removeAllTrackingIcons()
	 * @see #addOffsetTrackingIcon(int, Icon)
	 */
	public void removeTrackingIcon(Object tag) {
		if (trackingIcons!=null && trackingIcons.remove(tag)) {
			repaint();
		}
	}


	/**
	 * Removes all tracking icons.
	 *
	 * @see #removeTrackingIcon(Object)
	 * @see #addOffsetTrackingIcon(int, Icon)
	 */
	public void removeAllTrackingIcons() {
		if (trackingIcons!=null && trackingIcons.size()>0) {
			trackingIcons.clear();
			repaint();
		}
	}


	/**
	 * Removes all bookmark tracking icons.
	 */
	private void removeBookmarkTrackingIcons() {
		if (trackingIcons!=null) {
			for (Iterator i=trackingIcons.iterator(); i.hasNext(); ) {
				GutterIconImpl ti = (GutterIconImpl)i.next();
				if (ti.getIcon()==bookmarkIcon) {
					i.remove();
				}
			}
		}
	}


	/**
	 * Highlights a range of lines in the icon area.
	 *
	 * @param startLine The start of the line range.
	 * @param endLine The end of the line range.
	 * @see #clearActiveLineRange()
	 */
	public void setActiveLineRange(int startLine, int endLine) {
		if (startLine!=activeLineRangeStart ||
				endLine!=activeLineRangeEnd) {
			activeLineRangeStart = startLine;
			activeLineRangeEnd = endLine;
			repaint();
		}
	}


	/**
	 * Sets the color to use to render active line ranges.
	 *
	 * @param color The color to use.  If this is null, then the default
	 *        color is used.
	 * @see #getActiveLineRangeColor()
	 * @see Gutter#DEFAULT_ACTIVE_LINE_RANGE_COLOR
	 */
	public void setActiveLineRangeColor(Color color) {
		if (color==null) {
			color = Gutter.DEFAULT_ACTIVE_LINE_RANGE_COLOR;
		}
		if (!color.equals(activeLineRangeColor)) {
			activeLineRangeColor = color;
			repaint();
		}
	}


	/**
	 * Sets the icon to use for bookmarks.  Any previous bookmark icons
	 * are removed.
	 *
	 * @param icon The new bookmark icon.  If this is <code>null</code>,
	 *        bookmarking is effectively disabled.
	 * @see #getBookmarkIcon()
	 * @see #isBookmarkingEnabled()
	 */
	public void setBookmarkIcon(Icon icon) {
		removeBookmarkTrackingIcons();
		bookmarkIcon = icon;
		repaint();
	}


	/**
	 * Sets whether bookmarking is enabled.  Note that a bookmarking icon
	 * must be set via {@link #setBookmarkIcon(Icon)} before bookmarks are
	 * truly enabled.
	 *
	 * @param enabled Whether bookmarking is enabled.  If this is
	 *        <code>false</code>, any bookmark icons are removed.
	 * @see #isBookmarkingEnabled()
	 * @see #setBookmarkIcon(Icon)
	 */
	public void setBookmarkingEnabled(boolean enabled) {
		if (enabled!=bookmarkingEnabled) {
			bookmarkingEnabled = enabled;
			if (!enabled) {
				removeBookmarkTrackingIcons();
			}
			repaint();
		}
	}


	/**
	 * Sets the text area being displayed.  This will clear any tracking
	 * icons currently displayed.
	 *
	 * @param textArea The text area.
	 */
	public void setTextArea(RTextArea textArea) {
		removeAllTrackingIcons();
		super.setTextArea(textArea);
	}


	/**
	 * Programatically toggles whether there is a bookmark for the specified
	 * line.  If bookmarking is not enabled, this method does nothing.
	 *
	 * @param line The line.
	 * @return Whether a bookmark is now at the specified line.
	 * @throws BadLocationException If <code>line</code> is an invalid line
	 *         number in the text area.
	 */
	public boolean toggleBookmark(int line) throws BadLocationException {

		if (!isBookmarkingEnabled() || getBookmarkIcon()==null) {
			return false;
		}

		GutterIconInfo[] icons = getTrackingIcons(line);
		if (icons.length==0) {
			int offs = textArea.getLineStartOffset(line);
			addOffsetTrackingIcon(offs, bookmarkIcon);
			return true;
		}

		boolean found = false;
		for (int i=0; i<icons.length; i++) {
			if (icons[i].getIcon()==bookmarkIcon) {
				removeTrackingIcon(icons[i]);
				found = true;
				// Don't quit, in case they manipulate the document so > 1
				// bookmark is on a single line (kind of flaky, but it
				// works...).  If they delete all chars in the document,
				// AbstractDocument gets a little flaky with the returned line
				// number for viewToModel(), so this is just us trying to save
				// face a little.
			}
		}
		if (!found) {
			int offs = textArea.getLineStartOffset(line);
			addOffsetTrackingIcon(offs, bookmarkIcon);
		}

		return !found;

	}


	/**
	 * Sets our background color to that of standard "panels" in this
	 * LookAndFeel.  This is necessary because, otherwise, we'd inherit the
	 * background color of our parent component (the Gutter).
	 */
	private void updateBackground() {
		Color bg = UIManager.getColor("Panel.background");
		if (bg==null) { // UIManager properties aren't guaranteed to exist
			bg = new JPanel().getBackground();
		}
		setBackground(bg);
	}


	/**
	 * {@inheritDoc}
	 */
	public void updateUI() {
		super.updateUI(); // Does nothing
		updateBackground();
	}


	/**
	 * Returns the line rendered at the specified location.
	 *
	 * @param p The location in this row header.
	 * @return The corresponding line in the editor.
	 * @throws BadLocationException ble If an error occurs.
	 */
	private int viewToModelLine(Point p) throws BadLocationException {
		int offs = textArea.viewToModel(p);
		return offs>-1 ? textArea.getLineOfOffset(offs) : -1;
	}


	/**
	 * Implementation of the icons rendered.
	 */
	private static class GutterIconImpl implements GutterIconInfo, Comparable {

		private Icon icon;
		private Position pos;
		private String toolTip;

		public GutterIconImpl(Icon icon, Position pos, String toolTip) {
			this.icon = icon;
			this.pos = pos;
			this.toolTip = toolTip;
		}

		public int compareTo(Object o) {
			if (o instanceof GutterIconInfo) {
				return pos.getOffset() - ((GutterIconInfo)o).getMarkedOffset();
			}
			return -1;
		}

		public boolean equals(Object o) {
			return o==this;
		}

		public Icon getIcon() {
			return icon;
		}

		public int getMarkedOffset() {
			return pos.getOffset();
		}

		public String getToolTip() {
			return toolTip;
		}

		public int hashCode() {
			return icon.hashCode(); // FindBugs
		}

	}


}