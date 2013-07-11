/*
 * 02/11/2009
 *
 * LineNumberList.java - Renders line numbers in an RTextScrollPane.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.MouseInputListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.View;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;


/**
 * Renders line numbers in the gutter.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class LineNumberList extends AbstractGutterComponent
								implements MouseInputListener {

	private int currentLine;	// The last line the caret was on.
	private int lastY = -1;		// Used to check if caret changes lines when line wrap is enabled.
	private int lastVisibleLine;// Last line index painted.

	private int cellHeight;		// Height of a line number "cell" when word wrap is off.
	private int cellWidth;		// The width used for all line number cells.
	private int ascent;			// The ascent to use when painting line numbers.

	private Map aaHints;

	private int mouseDragStartOffset;

	/**
	 * Listens for events from the current text area.
	 */
	private Listener l;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on
	 * each paint.
	 */
	private Insets textAreaInsets;

	/**
	 * Used in {@link #paintComponent(Graphics)} to prevent reallocation on
	 * each paint.
	 */
	private Rectangle visibleRect;

	/**
	 * The index at which line numbering should start.  The default value is
	 * <code>1</code>, but applications can change this if, for example, they
	 * are displaying a subset of lines in a file.
	 */
	private int lineNumberingStartIndex;


	/**
	 * Constructs a new <code>LineNumberList</code> using default values for
	 * line number color (gray) and highlighting the current line.
	 *
	 * @param textArea The text component for which line numbers will be
	 *        displayed.
	 */
	public LineNumberList(RTextArea textArea) {
		this(textArea, null);
	}


	/**
	 * Constructs a new <code>LineNumberList</code>.
	 *
	 * @param textArea The text component for which line numbers will be
	 *        displayed.
	 * @param numberColor The color to use for the line numbers.  If this is
	 *        <code>null</code>, gray will be used.
	 */
	public LineNumberList(RTextArea textArea, Color numberColor) {

		super(textArea);

		if (numberColor!=null) {
			setForeground(numberColor);
		}
		else {
			setForeground(Color.GRAY);
		}

		// Initialize currentLine; otherwise, the current line won't start
		// off as highlighted.
		currentLine = 0;
		setLineNumberingStartIndex(1);

		visibleRect = new Rectangle(); // Must be initialized

		addMouseListener(this);
		addMouseMotionListener(this);

		aaHints = RSyntaxUtilities.getDesktopAntiAliasHints();

	}


	/**
	 * Overridden to set width of this component correctly when we are first
	 * displayed (as keying off of the RTextArea gives us (0,0) when it isn't
	 * yet displayed.
	 */
	public void addNotify() {
		super.addNotify();
		if (textArea!=null) {
			l.install(textArea); // Won't double-install
		}
		updateCellWidths();
		updateCellHeights();
	}


	/**
	 * Calculates the last line number index painted in this component.
	 *
	 * @return The last line number index painted in this component.
	 */
	private int calculateLastVisibleLineNumber() {
		int lastLine = 0;
		if (textArea!=null) {
			lastLine = textArea.getLineCount()+getLineNumberingStartIndex()-1;
		}
		return lastLine;
	}


	/**
	 * Returns the starting line's line number.  The default value is
	 * <code>1</code>.
	 *
	 * @return The index
	 * @see #setLineNumberingStartIndex(int)
	 */
	public int getLineNumberingStartIndex() {
		return lineNumberingStartIndex;
	}


	/**
	 * {@inheritDoc}
	 */
	public Dimension getPreferredSize() {
		int h = textArea!=null ? textArea.getHeight() : 100; // Arbitrary
		return new Dimension(cellWidth, h);
	}


	/**
	 * Returns the width of the empty border on this component's right-hand
	 * side (or left-hand side, if the orientation is RTL).
	 *
	 * @return The border width.
	 */
	private int getRhsBorderWidth() {
		int w = 4;
		if (textArea instanceof RSyntaxTextArea) {
			if (((RSyntaxTextArea)textArea).isCodeFoldingEnabled()) {
				w = 0;
			}
		}
		return w;
	}


	/**
	 * {@inheritDoc}
	 */
	void handleDocumentEvent(DocumentEvent e) {
		int newLastLine = calculateLastVisibleLineNumber();
		if (newLastLine!=lastVisibleLine) {
			// Adjust the amount of space the line numbers take up,
			// if necessary.
			if (newLastLine/10 != lastVisibleLine/10) {
				updateCellWidths();
			}
			lastVisibleLine = newLastLine;
			repaint();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	void lineHeightsChanged() {
		updateCellHeights();
	}


	public void mouseClicked(MouseEvent e) {
	}


	public void mouseDragged(MouseEvent e) {
		if (mouseDragStartOffset>-1) {
			int pos = textArea.viewToModel(new Point(0, e.getY()));
			if (pos>=0) { // Not -1
				textArea.setCaretPosition(mouseDragStartOffset);
				textArea.moveCaretPosition(pos);
			}
		}
	}


	public void mouseEntered(MouseEvent e) {
	}


	public void mouseExited(MouseEvent e) {
	}


	public void mouseMoved(MouseEvent e) {
	}


	public void mousePressed(MouseEvent e) {
		if (textArea==null) {
			return;
		}
		if (e.getButton()==MouseEvent.BUTTON1) {
			int pos = textArea.viewToModel(new Point(0, e.getY()));
			if (pos>=0) { // Not -1
				textArea.setCaretPosition(pos);
			}
			mouseDragStartOffset = pos;
		}
		else {
			mouseDragStartOffset = -1;
		}
	}


	public void mouseReleased(MouseEvent e) {
	}


	/**
	 * Paints this component.
	 *
	 * @param g The graphics context.
	 */
	protected void paintComponent(Graphics g) {

		if (textArea==null) {
			return;
		}

		visibleRect = g.getClipBounds(visibleRect);
		if (visibleRect==null) { // ???
			visibleRect = getVisibleRect();
		}
		//System.out.println("LineNumberList repainting: " + visibleRect);
		if (visibleRect==null) {
			return;
		}

		Color bg = getBackground();
		if (getGutter()!=null) { // Should always be true
			bg = getGutter().getBackground();
		}
		g.setColor(bg);
		g.fillRect(0,visibleRect.y, cellWidth,visibleRect.height);
		g.setFont(getFont());
		if (aaHints!=null) {
			((Graphics2D)g).addRenderingHints(aaHints);
		}

		if (textArea.getLineWrap()) {
			paintWrappedLineNumbers(g, visibleRect);
			return;
		}

		// Get where to start painting (top of the row), and where to paint
		// the line number (drawString expects y==baseline).
		// We need to be "scrolled up" just enough for the missing part of
		// the first line.
		int topLine = visibleRect.y/cellHeight;
		int actualTopY = topLine*cellHeight;
		textAreaInsets = textArea.getInsets(textAreaInsets);
		actualTopY += textAreaInsets.top;
		int y = actualTopY + ascent;

		// Get the actual first line to paint, taking into account folding.
		FoldManager fm = null;
		if (textArea instanceof RSyntaxTextArea) {
			fm = ((RSyntaxTextArea)textArea).getFoldManager();
			topLine += fm.getHiddenLineCountAbove(topLine, true);
		}
		final int RHS_BORDER_WIDTH = getRhsBorderWidth();

/*
		// Highlight the current line's line number, if desired.
		if (textArea.getHighlightCurrentLine() && currentLine>=topLine &&
				currentLine<=bottomLine) {
			g.setColor(textArea.getCurrentLineHighlightColor());
			g.fillRect(0,actualTopY+(currentLine-topLine)*cellHeight,
						cellWidth,cellHeight);
		}
*/

		// Paint line numbers
		g.setColor(getForeground());
		boolean ltr = getComponentOrientation().isLeftToRight();
		if (ltr) {
			FontMetrics metrics = g.getFontMetrics();
			int rhs = getWidth() - RHS_BORDER_WIDTH;
			int line = topLine + 1;
			while (y<visibleRect.y+visibleRect.height+ascent && line<=textArea.getLineCount()) {
				String number = Integer.toString(line + getLineNumberingStartIndex() - 1);
				int width = metrics.stringWidth(number);
				g.drawString(number, rhs-width,y);
				y += cellHeight;
				Fold fold = fm.getFoldForLine(line-1);
				// Skip to next line to paint, taking extra care for lines with
				// block ends and begins together, e.g. "} else {"
				while (fold!=null && fold.isCollapsed()) {
					int hiddenLineCount = fold.getLineCount();
					if (hiddenLineCount==0) {
						// Fold parser identified a 0-line fold region... This
						// is really a bug, but we'll handle it gracefully.
						break;
					}
					line += hiddenLineCount;
					fold = fm.getFoldForLine(line-1);
				}
				line++;
			}
		}
		else { // rtl
			int line = topLine + 1;
			while (y<visibleRect.y+visibleRect.height && line<textArea.getLineCount()) {
				String number = Integer.toString(line + getLineNumberingStartIndex() - 1);
				g.drawString(number, RHS_BORDER_WIDTH, y);
				y += cellHeight;
				Fold fold = fm.getFoldForLine(line-1);
				// Skip to next line to paint, taking extra care for lines with
				// block ends and begins together, e.g. "} else {"
				while (fold!=null && fold.isCollapsed()) {
					line += fold.getLineCount();
					fold = fm.getFoldForLine(line);
				}
				line++;
			}
		}

	}


	/**
	 * Paints line numbers for text areas with line wrap enabled.
	 *
	 * @param g The graphics context.
	 * @param visibleRect The visible rectangle of these line numbers.
	 */
	private void paintWrappedLineNumbers(Graphics g, Rectangle visibleRect) {

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

		// Some variables we'll be using.
		int width = getWidth();

		RTextAreaUI ui = (RTextAreaUI)textArea.getUI();
		View v = ui.getRootView(textArea).getView(0);
		//boolean currentLineHighlighted = textArea.getHighlightCurrentLine();
		Document doc = textArea.getDocument();
		Element root = doc.getDefaultRootElement();
		int lineCount = root.getElementCount();
		int topPosition = textArea.viewToModel(
								new Point(visibleRect.x,visibleRect.y));
		int topLine = root.getElementIndex(topPosition);
		FoldManager fm = ((RSyntaxTextArea)textArea).getFoldManager();

		// Compute the y at which to begin painting text, taking into account
		// that 1 logical line => at least 1 physical line, so it may be that
		// y<0.  The computed y-value is the y-value of the top of the first
		// (possibly) partially-visible view.
		Rectangle visibleEditorRect = ui.getVisibleEditorRect();
		Rectangle r = LineNumberList.getChildViewBounds(v, topLine,
												visibleEditorRect);
		int y = r.y;
		final int RHS_BORDER_WIDTH = getRhsBorderWidth();
		int rhs;
		boolean ltr = getComponentOrientation().isLeftToRight();
		if (ltr) {
			rhs = width - RHS_BORDER_WIDTH;
		}
		else { // rtl
			rhs = RHS_BORDER_WIDTH;
		}
		int visibleBottom = visibleRect.y + visibleRect.height;
		FontMetrics metrics = g.getFontMetrics();

		// Keep painting lines until our y-coordinate is past the visible
		// end of the text area.
		g.setColor(getForeground());

		while (y < visibleBottom) {

			r = LineNumberList.getChildViewBounds(v, topLine, visibleEditorRect);

			/*
			// Highlight the current line's line number, if desired.
			if (currentLineHighlighted && topLine==currentLine) {
				g.setColor(textArea.getCurrentLineHighlightColor());
				g.fillRect(0,y, width,(r.y+r.height)-y);
				g.setColor(getForeground());
			}
			*/

			// Paint the line number.
			int index = (topLine+1) + getLineNumberingStartIndex() - 1;
			String number = Integer.toString(index);
			if (ltr) {
				int strWidth = metrics.stringWidth(number);
				g.drawString(number, rhs-strWidth,y+ascent);
			}
			else {
				int x = RHS_BORDER_WIDTH;
				g.drawString(number, x, y+ascent);
			}

			// The next possible y-coordinate is just after the last line
			// painted.
			y += r.height;

			// Update topLine (we're actually using it for our "current line"
			// variable now).
			Fold fold = fm.getFoldForLine(topLine);
			if (fold!=null && fold.isCollapsed()) {
				topLine += fold.getCollapsedLineCount();
			}
			topLine++;
			if (topLine>=lineCount)
				break;

		}

	}


	/**
	 * Called when this component is removed from the view hierarchy.
	 */
	public void removeNotify() {
		super.removeNotify();
		if (textArea!=null) {
			l.uninstall(textArea);
		}
	}


	/**
	 * Repaints a single line in this list.
	 *
	 * @param line The line to repaint.
	 */
	private void repaintLine(int line) {
		int y = textArea.getInsets().top;
		y += line*cellHeight;
		repaint(0,y, cellWidth,cellHeight);
	}


	/**
	 * Overridden to ensure line number cell sizes are updated with the
	 * font size change.
	 *
	 * @param font The new font to use for line numbers.
	 */
	public void setFont(Font font) {
		super.setFont(font);
		updateCellWidths();
		updateCellHeights();
	}


	/**
	 * Sets the starting line's line number.  The default value is
	 * <code>1</code>.  Applications can call this method to change this value
	 * if they are displaying a subset of lines in a file, for example.
	 *
	 * @param index The new index.
	 * @see #getLineNumberingStartIndex()
	 */
	public void setLineNumberingStartIndex(int index) {
		if (index!=lineNumberingStartIndex) {
			lineNumberingStartIndex = index;
			updateCellWidths();
			repaint();
		}
	}


	/**
	 * Sets the text area being displayed.
	 *
	 * @param textArea The text area.
	 */
	public void setTextArea(RTextArea textArea) {

		if (l==null) {
			l = new Listener();
		}

		if (this.textArea!=null) {
			l.uninstall(textArea);
		}

		super.setTextArea(textArea);
		lastVisibleLine = calculateLastVisibleLineNumber();

		if (textArea!=null) {
			l.install(textArea); // Won't double-install
			updateCellHeights();
			updateCellWidths();
		}

	}


	/**
	 * Changes the height of the cells in the JList so that they are as tall as
	 * font. This function should be called whenever the user changes the Font
	 * of <code>textArea</code>.
	 */
	private void updateCellHeights() {
		if (textArea!=null) {
			cellHeight = textArea.getLineHeight();
			ascent = textArea.getMaxAscent();
		}
		else {
			cellHeight = 20; // Arbitrary number.
			ascent = 5; // Also arbitrary
		}
		repaint();
	}


	/**
	 * Changes the width of the cells in the JList so you can see every digit
	 * of each.
	 */
	void updateCellWidths() {

		int oldCellWidth = cellWidth;
		cellWidth = getRhsBorderWidth();

		// Adjust the amount of space the line numbers take up, if necessary.
		if (textArea!=null) {
			Font font = getFont();
			if (font!=null) {
				FontMetrics fontMetrics = getFontMetrics(font);
				int count = 0;
				int lineCount = textArea.getLineCount() +
						getLineNumberingStartIndex() - 1;
				do {
					lineCount = lineCount/10;
					count++;
				} while (lineCount >= 10);
				cellWidth += fontMetrics.charWidth('9')*(count+1) + 3;
			}
		}

		if (cellWidth!=oldCellWidth) { // Always true
			revalidate();
		}

	}


	/**
	 * Listens for events in the text area we're interested in.
	 */
	private class Listener implements CaretListener, PropertyChangeListener {

		private boolean installed;

		public void caretUpdate(CaretEvent e) {

			int dot = textArea.getCaretPosition();

			// We separate the line wrap/no line wrap cases because word wrap
			// can make a single line from the model (document) be on multiple
			// lines on the screen (in the view); thus, we have to enhance the
			// logic for that case a bit - we check the actual y-coordinate of
			// the caret when line wrap is enabled.  For the no-line-wrap case,
			// getting the line number of the caret suffices.  This increases
			// efficiency in the no-line-wrap case.

			if (textArea.getLineWrap()==false) {
				int line = textArea.getDocument().getDefaultRootElement().
										getElementIndex(dot);
				if (currentLine!=line) {
					repaintLine(line);
					repaintLine(currentLine);
					currentLine = line;
				}
			}
			else { // lineWrap enabled; must check actual y position of caret
				try {
					int y = textArea.yForLineContaining(dot);
					if (y!=lastY) {
						lastY = y;
						currentLine = textArea.getDocument().
								getDefaultRootElement().getElementIndex(dot);
						repaint(); // *Could* be optimized...
					}
				} catch (BadLocationException ble) {
					ble.printStackTrace();
				}
			}

		}

		public void install(RTextArea textArea) {
			if (!installed) {
				//System.out.println("Installing");
				textArea.addCaretListener(this);
				textArea.addPropertyChangeListener(this);
				caretUpdate(null); // Force current line highlight repaint
				installed = true;
			}
		}

		public void propertyChange(PropertyChangeEvent e) {

			String name = e.getPropertyName();

			// If they change the current line highlight in any way...
			if (RTextArea.HIGHLIGHT_CURRENT_LINE_PROPERTY.equals(name) ||
				RTextArea.CURRENT_LINE_HIGHLIGHT_COLOR_PROPERTY.equals(name)) {
				repaintLine(currentLine);
			}

		}

		public void uninstall(RTextArea textArea) {
			if (installed) {
				//System.out.println("Uninstalling");
				textArea.removeCaretListener(this);
				textArea.removePropertyChangeListener(this);
				installed = false;
			}
		}

	}


}