/*
 * 02/10/2009
 *
 * LineHighlightManager - Manages line highlights.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;


/**
 * Manages line highlights in an <code>RTextArea</code>.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class LineHighlightManager {

	private RTextArea textArea;
	private List lineHighlights;


	/**
	 * Constructor.
	 *
	 * @param textArea The parent text area.
	 */
	public LineHighlightManager(RTextArea textArea) {
		this.textArea = textArea;
	}


	/**
	 * Highlights the specified line.
	 *
	 * @param line The line to highlight.
	 * @param color The color to highlight with.
	 * @return A tag for the highlight.
	 * @throws BadLocationException If <code>line</code> is not a valid line
	 *         number.
	 * @see #removeLineHighlight(Object)
	 */
	public Object addLineHighlight(int line, Color color)
									throws BadLocationException {
		int offs = textArea.getLineStartOffset(line);
		LineHighlightInfo lhi = new LineHighlightInfo(
						textArea.getDocument().createPosition(offs), color);
		if (lineHighlights==null) {
			lineHighlights = new ArrayList(1);
		}
		int index = Collections.binarySearch(lineHighlights, lhi);
		if (index<0) { // Common case
			index = -(index+1);
		}
		lineHighlights.add(index, lhi);
		repaintLine(lhi);
		return lhi;
	}


	/**
	 * Paints any highlighted lines in the specified line range.
	 *
	 * @param g The graphics context.
	 */
	public void paintLineHighlights(Graphics g) {

		int count = lineHighlights==null ? 0 : lineHighlights.size();
		if (count>0) {

			int docLen = textArea.getDocument().getLength();
			Rectangle vr = textArea.getVisibleRect();
			int lineHeight = textArea.getLineHeight();

			try {

				for (int i=0; i<count; i++) {
					LineHighlightInfo lhi =(LineHighlightInfo)
													lineHighlights.get(i);
					int offs = lhi.getOffset();
					if (offs>=0 && offs<=docLen) {
						int y = textArea.yForLineContaining(offs);
						if (y>vr.y-lineHeight) {
							if (y<vr.y+vr.height) {
								g.setColor(lhi.getColor());
								g.fillRect(0,y, textArea.getWidth(),lineHeight);
							}
							else {
								break; // Out of visible rect
							}
						}
					}
				}

			} catch (BadLocationException ble) { // Never happens
				ble.printStackTrace();
			}
		}

	}


	/**
	 * Removes all line highlights.
	 *
	 * @see #removeLineHighlight(Object)
	 */
	public void removeAllLineHighlights() {
		if (lineHighlights!=null) {
			lineHighlights.clear();
			textArea.repaint();
		}
	}


	/**
	 * Removes a line highlight.
	 *
	 * @param tag The tag of the line highlight to remove.
	 * @see #addLineHighlight(int, Color)
	 */
	public void removeLineHighlight(Object tag) {
		if (tag instanceof LineHighlightInfo) {
			lineHighlights.remove(tag);
			repaintLine((LineHighlightInfo)tag);
		}
	}


	/**
	 * Repaints the line pointed to by the specified highlight information.
	 *
	 * @param lhi The highlight information.
	 */
	private void repaintLine(LineHighlightInfo lhi) {
		int offs = lhi.getOffset();
		// May be > length if they deleted text including the highlight
		if (offs>=0 && offs<=textArea.getDocument().getLength()) {
			try {
				int y = textArea.yForLineContaining(offs);
				if (y>-1) {
					textArea.repaint(0, y,
								textArea.getWidth(), textArea.getLineHeight());
				}
			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}
	}


	/**
	 * Information about a line highlight.
	 */
	private static class LineHighlightInfo implements Comparable {

		private Position offs;
		private Color color;

		public LineHighlightInfo(Position offs, Color c) {
			this.offs = offs;
			this.color = c;
		}

		public int compareTo(Object o) {
			if (o instanceof LineHighlightInfo) {
				return offs.getOffset() - ((LineHighlightInfo)o).getOffset();
			}
			return -1;
		}

		public boolean equals(Object o) {
			if (o==this) {
				return true;
			}
			if (o instanceof LineHighlightInfo) {
				return offs.getOffset()==((LineHighlightInfo)o).getOffset();
			}
			return false;
		}

		public Color getColor() {
			return color;
		}

		public int getOffset() {
			return offs.getOffset();
		}

		public int hashCode() {
			return getOffset();
		}

	}


}