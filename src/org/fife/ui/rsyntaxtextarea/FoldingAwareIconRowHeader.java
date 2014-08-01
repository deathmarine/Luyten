/*
 * 03/07/2012
 *
 * FoldingAwareIconRowHeader - Icon row header that paints itself correctly
 * even when code folding is enabled.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import javax.swing.Icon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.IconRowHeader;


/**
 * A row header component that takes code folding into account when painting
 * itself.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FoldingAwareIconRowHeader extends IconRowHeader {


	/**
	 * Constructor.
	 *
	 * @param textArea The parent text area.
	 */
	public FoldingAwareIconRowHeader(RSyntaxTextArea textArea) {
		super(textArea);
	}


	/**
	 * {@inheritDoc}
	 */
	protected void paintComponent(Graphics g) {

		// When line wrap is not enabled, take the faster code path.
		if (textArea==null) {
			return;
		}
		RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
		FoldManager fm = rsta.getFoldManager();
		if (!fm.isCodeFoldingSupportedAndEnabled()) {
			super.paintComponent(g);
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

		// Get the first line to paint.
		int cellHeight = textArea.getLineHeight();
		int topLine = (visibleRect.y-textAreaInsets.top)/cellHeight;

		// Get where to start painting (top of the row).
		// We need to be "scrolled up" up just enough for the missing part of
		// the first line.
		int y = topLine*cellHeight + textAreaInsets.top;

		// AFTER calculating visual offset to paint at, account for folding.
		topLine += fm.getHiddenLineCountAbove(topLine, true);

		// Paint the active line range.
		if (activeLineRangeStart>-1 && activeLineRangeEnd>-1) {
			Color activeLineRangeColor = getActiveLineRangeColor();
			g.setColor(activeLineRangeColor);
			try {

				int y1 = rsta.yForLine(activeLineRangeStart);
				if (y1>-1) { // Not in a collapsed fold...

					int y2 = rsta.yForLine(activeLineRangeEnd);
					if (y2==-1) { // In a collapsed fold
						y2 = y1;
					}
					y2 += cellHeight - 1;

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

					if (y1>=y && y1<y+visibleRect.height) {
						g.drawLine(0,y1, getWidth(),y1);
					}
					if (y2>=y && y2<y+visibleRect.height) {
						g.drawLine(0,y2, getWidth(),y2);
					}

				}

			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}

		// Paint icons
		if (trackingIcons!=null) {
			int lastLine = textArea.getLineCount() - 1;
			for (int i=trackingIcons.size()-1; i>=0; i--) { // Last to first
				GutterIconInfo ti = getTrackingIcon(i);
				int offs = ti.getMarkedOffset();
				if (offs>=0 && offs<=doc.getLength()) {
					int line = root.getElementIndex(offs);
					if (line<=lastLine && line>=topLine) {
						try {
							Icon icon = ti.getIcon();
							if (icon!=null) {
								int lineY = rsta.yForLine(line);
								if (lineY>=y && lineY<=visibleRect.y+visibleRect.height) {
									int y2 = lineY + (cellHeight-icon.getIconHeight())/2;
									icon.paintIcon(this, g, 0, y2);
									lastLine = line-1; // Paint only 1 icon per line
								}
							}
						} catch (BadLocationException ble) {
							ble.printStackTrace(); // Never happens
						}
					}
					else if (line<topLine) {
						break; // All other lines are above us, so quit now
					}
				}
			}
		}

	}


	/**
	 * Paints icons when line wrapping is enabled.  Note that this does not
	 * override the parent class's implementation to avoid this version being
	 * called when line wrapping is disabled.
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

		// We avoid using modelToView/viewToModel where possible, as these
		// methods trigger a parsing of the line into syntax tokens, which is
		// costly.  It's cheaper to just grab the child views' bounds.

		RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
//		boolean currentLineHighlighted = textArea.getHighlightCurrentLine();
		Document doc = textArea.getDocument();
		Element root = doc.getDefaultRootElement();
		int topPosition = textArea.viewToModel(
								new Point(visibleRect.x,visibleRect.y));
		int topLine = root.getElementIndex(topPosition);

		int topY = visibleRect.y;
		int bottomY = visibleRect.y + visibleRect.height;
		int cellHeight = textArea.getLineHeight();

		// Paint icons
		if (trackingIcons!=null) {
			int lastLine = textArea.getLineCount() - 1;
			for (int i=trackingIcons.size()-1; i>=0; i--) { // Last to first
				GutterIconInfo ti = getTrackingIcon(i);
				int offs = ti.getMarkedOffset();
				if (offs>=0 && offs<=doc.getLength()) {
					int line = root.getElementIndex(offs);
					if (line<=lastLine && line>=topLine) {
						try {
							int lineY = rsta.yForLine(line);
							if (lineY>=topY && lineY<bottomY) {
								Icon icon = ti.getIcon();
								if (icon!=null) {
									int y2 = lineY + (cellHeight-icon.getIconHeight())/2;
									ti.getIcon().paintIcon(this, g, 0, y2);
									lastLine = line-1; // Paint only 1 icon per line
								}
							}
						} catch (BadLocationException ble) {
							ble.printStackTrace(); // Never happens
						}
					}
					else if (line<topLine) {
						break; // All other lines are above us, so quit now
					}
				}
			}
		}

	}


}