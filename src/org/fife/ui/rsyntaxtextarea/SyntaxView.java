/*
 * 02/24/2004
 *
 * SyntaxView.java - The View object used by RSyntaxTextArea when word wrap is
 * disabled.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;


/**
 * The <code>javax.swing.text.View</code> object used by {@link RSyntaxTextArea}
 * when word wrap is disabled.  It implements syntax highlighting for
 * programming languages using the colors and font styles specified by the
 * <code>RSyntaxTextArea</code>.<p>
 *
 * You don't really have to do anything to use this class, as
 * {@link RSyntaxTextAreaUI} automatically sets the text area's view to be
 * an instance of this class if word wrap is disabled.<p>
 *
 * The tokens that specify how to paint the syntax-highlighted text are gleaned
 * from the text area's {@link RSyntaxDocument}.
 *
 * @author Robert Futrell
 * @version 0.3
 */
public class SyntaxView extends View implements TabExpander,
											TokenOrientedView, RSTAView {

	/**
	 * The default font used by the text area.  If this changes we need to
	 * recalculate the longest line.
	 */
	Font font;

	/**
	 * Font metrics for the current font.
	 */
	protected FontMetrics metrics;

	/**
	 * The current longest line.  This is used to calculate the preferred width
	 * of the view.  Since the calculation is potentially expensive, we try to
	 * avoid it by stashing which line is currently the longest.
	 */
	Element longLine;
	float longLineWidth;

	private int tabSize;
	protected int tabBase;
    

	/**
	 * Cached for each paint() call so each drawLine() call has access to it.
	 */
	private RSyntaxTextArea host;

	/**
	 * Cached values to speed up the painting a tad.
	 */
	private int lineHeight = 0;
	private int ascent;
	private int clipStart;
	private int clipEnd;

//	/**
//	 * The end-of-line marker.
//	 */
//	private static final char[] eolMarker = { '.' };


	/**
	 * Constructs a new <code>SyntaxView</code> wrapped around an element.
	 *
	 * @param elem The element representing the text to display.
	 */
	public SyntaxView(Element elem) {
		super(elem);
	}


	/**
	 * Iterate over the lines represented by the child elements
	 * of the element this view represents, looking for the line
	 * that is the longest.  The <em>longLine</em> variable is updated to
	 * represent the longest line contained.  The <em>font</em> variable
	 * is updated to indicate the font used to calculate the 
	 * longest line.
	 */
	void calculateLongestLine() {
		Component c = getContainer();
		font = c.getFont();
		metrics = c.getFontMetrics(font);
		tabSize = getTabSize() * metrics.charWidth(' ');
		Element lines = getElement();
		int n = lines.getElementCount();
		for (int i=0; i<n; i++) {
			Element line = lines.getElement(i);
			float w = getLineWidth(i);
			if (w > longLineWidth) {
				longLineWidth = w;
				longLine = line;
			}
		}
	}


	/**
	 * Gives notification from the document that attributes were changed
	 * in a location that this view is responsible for.
	 *
	 * @param changes the change information from the associated document
	 * @param a the current allocation of the view
	 * @param f the factory to use to rebuild if the view has children
	 * @see View#changedUpdate
	 */
	public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
		updateDamage(changes, a, f);
	}


	/**
	 * Repaint the given line range.
	 *
	 * @param line0 The starting line number to repaint.  This must
	 *   be a valid line number in the model.
	 * @param line1 The ending line number to repaint.  This must
	 *   be a valid line number in the model.
	 * @param a  The region allocated for the view to render into.
	 * @param host The component hosting the view (used to call repaint).
	 */
	protected void damageLineRange(int line0, int line1, Shape a,
												Component host) {
		if (a != null) {
			Rectangle area0 = lineToRect(a, line0);
			Rectangle area1 = lineToRect(a, line1);
			if ((area0 != null) && (area1 != null)) {
				Rectangle dmg = area0.union(area1); // damage.
				host.repaint(dmg.x, dmg.y, dmg.width, dmg.height);
			}
			else
				host.repaint();
		}
	}


	/**
	 * Draws the passed-in text using syntax highlighting for the current
	 * language.  The tokens used to decide how to paint the syntax
	 * highlighting are grabbed from the text area's document.
	 *
	 * @param token The list of tokens to draw.
	 * @param g The graphics context in which to draw.
	 * @param x The x-coordinate at which to draw.
	 * @param y The y-coordinate at which to draw.
	 * @return The x-coordinate representing the end of the painted text.
	 */
	public float drawLine(Token token, Graphics2D g, float x, float y) {

		float nextX = x;	// The x-value at the end of our text.

		while (token!=null && token.isPaintable() && nextX<clipEnd) {
			nextX = token.paint(g, nextX,y, host, this, clipStart);
			token = token.getNextToken();
		}

		// NOTE: We should re-use code from Token (paintBackground()) here,
		// but don't because I'm just too lazy.
		if (host.getEOLMarkersVisible()) {
			g.setColor(host.getForegroundForTokenType(Token.WHITESPACE));
			g.setFont(host.getFontForTokenType(Token.WHITESPACE));
			g.drawString("\u00B6", nextX, y);
		}

		// Return the x-coordinate at the end of the painted text.
		return nextX;

	}


	/**
	 * Calculates the width of the line represented by the given element.
	 *
	 * @param line The line for which to get the length.
	 * @param lineNumber The line number of the specified line in the document.
	 * @return The width of the line.
	 */
	private float getLineWidth(int lineNumber) {
		Token tokenList = ((RSyntaxDocument)getDocument()).
									getTokenListForLine(lineNumber);
		return RSyntaxUtilities.getTokenListWidth(tokenList,
								(RSyntaxTextArea)getContainer(),
								this);
	}


	/**
	 * Provides a way to determine the next visually represented model 
	 * location that one might place a caret.  Some views may not be visible,
	 * they might not be in the same order found in the model, or they just
	 * might not allow access to some of the locations in the model.
	 *
	 * @param pos the position to convert >= 0
	 * @param a the allocated region to render into
	 * @param direction the direction from the current position that can
	 *  be thought of as the arrow keys typically found on a keyboard.
	 *  This may be SwingConstants.WEST, SwingConstants.EAST, 
	 *  SwingConstants.NORTH, or SwingConstants.SOUTH.  
	 * @return the location within the model that best represents the next
	 *  location visual position.
	 * @exception BadLocationException
	 * @exception IllegalArgumentException for an invalid direction
	 */
	public int getNextVisualPositionFrom(int pos, Position.Bias b, Shape a, 
							int direction, Position.Bias[] biasRet)
							throws BadLocationException {
		return RSyntaxUtilities.getNextVisualPositionFrom(pos, b, a,
										direction, biasRet, this);
	}


	/**
	 * Determines the preferred span for this view along an
	 * axis.
	 *
	 * @param axis may be either View.X_AXIS or View.Y_AXIS
	 * @return   the span the view would like to be rendered into >= 0.
	 *           Typically the view is told to render into the span
	 *           that is returned, although there is no guarantee.  
	 *           The parent may choose to resize or break the view.
	 * @exception IllegalArgumentException for an invalid axis
	 */
	public float getPreferredSpan(int axis) {
		updateMetrics();
		switch (axis) {
			case View.X_AXIS:
				float span = longLineWidth + 10; // "fudge factor."
				if (host.getEOLMarkersVisible()) {
					span += metrics.charWidth('\u00B6');
				}
				return span;
			case View.Y_AXIS:
				// We update lineHeight here as when this method is first
				// called, lineHeight isn't initialized.  If we don't do it
				// here, we get no vertical scrollbar (as lineHeight==0).
				lineHeight = host!=null ? host.getLineHeight() : lineHeight;
//				return getElement().getElementCount() * lineHeight;
int visibleLineCount = getElement().getElementCount();
if (host.isCodeFoldingEnabled()) {
	visibleLineCount -= host.getFoldManager().getHiddenLineCount();
}
return visibleLineCount * lineHeight;
			default:
				throw new IllegalArgumentException("Invalid axis: " + axis);
		}
	}


	/**
	 * Returns the tab size set for the document, defaulting to 5.
	 *
	 * @return The tab size.
	 */
	protected int getTabSize() {
		Integer i = (Integer)getDocument().getProperty(
									PlainDocument.tabSizeAttribute);
		int size = (i != null) ? i.intValue() : 5;
		return size;
	}


	/**
	 * Returns a token list for the <i>physical</i> line above the physical
	 * line containing the specified offset into the document.  Note that for
	 * this plain (non-wrapped) view, this is simply the token list for the
	 * logical line above the line containing <code>offset</code>, since lines
	 * are not wrapped.
	 *
	 * @param offset The offset in question.
	 * @return A token list for the physical (and in this view, logical) line
	 *         before this one.  If <code>offset</code> is in the first line in
	 *         the document, <code>null</code> is returned.
	 */
	public Token getTokenListForPhysicalLineAbove(int offset) {
		RSyntaxDocument document = (RSyntaxDocument)getDocument();
		Element map = document.getDefaultRootElement();
int line = map.getElementIndex(offset);
FoldManager fm = host.getFoldManager();
if (fm==null) {
	line--;
	if (line>=0) {
		return document.getTokenListForLine(line);
	}
}
else {
	line = fm.getVisibleLineAbove(line);
	if (line>=0) {
		return document.getTokenListForLine(line);
	}
}
//		int line = map.getElementIndex(offset) - 1;
//		if (line>=0)
//			return document.getTokenListForLine(line);
		return null;
	}


	/**
	 * Returns a token list for the <i>physical</i> line below the physical
	 * line containing the specified offset into the document.  Note that for
	 * this plain (non-wrapped) view, this is simply the token list for the
	 * logical line below the line containing <code>offset</code>, since lines
	 * are not wrapped.
	 *
	 * @param offset The offset in question.
	 * @return A token list for the physical (and in this view, logical) line
	 *         after this one.  If <code>offset</code> is in the last physical
	 *         line in the document, <code>null</code> is returned.
	 */
	public Token getTokenListForPhysicalLineBelow(int offset) {
		RSyntaxDocument document = (RSyntaxDocument)getDocument();
		Element map = document.getDefaultRootElement();
		int lineCount = map.getElementCount();
int line = map.getElementIndex(offset);
if (!host.isCodeFoldingEnabled()) {
	if (line<lineCount-1) {
		return document.getTokenListForLine(line+1);
	}
}
else {
	FoldManager fm = host.getFoldManager();
	line = fm.getVisibleLineBelow(line);
	if (line>=0 && line<lineCount) {
		return document.getTokenListForLine(line);
	}
}
//		int line = map.getElementIndex(offset);
//		int lineCount = map.getElementCount();
//		if (line<lineCount-1)
//			return document.getTokenListForLine(line+1);
		return null;
	}


	/**
	 * Gives notification that something was inserted into the document
	 * in a location that this view is responsible for.
	 *
	 * @param changes The change information from the associated document.
	 * @param a The current allocation of the view.
	 * @param f The factory to use to rebuild if the view has children.
	 */
	public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
		updateDamage(changes, a, f);
	}


	/**
	 * Determine the rectangle that represents the given line.
	 *
	 * @param a The region allocated for the view to render into
	 * @param line The line number to find the region of.  This must
	 *        be a valid line number in the model.
	 */
	protected Rectangle lineToRect(Shape a, int line) {
		Rectangle r = null;
		updateMetrics();
		if (metrics != null) {
			Rectangle alloc = a.getBounds();
			// NOTE:  lineHeight is not initially set here, leading to the
			// current line not being highlighted when a document is first
			// opened.  So, we set it here just in case.
			lineHeight = host!=null ? host.getLineHeight() : lineHeight;
if (host.isCodeFoldingEnabled()) {
	FoldManager fm = host.getFoldManager();
	int hiddenCount = fm.getHiddenLineCountAbove(line);
	line -= hiddenCount;
}
			r = new Rectangle(alloc.x, alloc.y + line*lineHeight,
									alloc.width, lineHeight);
		}
		return r;
	}


	/**
	 * Provides a mapping from the document model coordinate space
	 * to the coordinate space of the view mapped to it.
	 *
	 * @param pos the position to convert >= 0
	 * @param a the allocated region to render into
	 * @return the bounding box of the given position
	 * @exception BadLocationException  if the given position does not
	 *   represent a valid location in the associated document
	 * @see View#modelToView
	 */
	public Shape modelToView(int pos, Shape a, Position.Bias b)
										throws BadLocationException {

		// line coordinates
		Element map = getElement();
		RSyntaxDocument doc = (RSyntaxDocument)getDocument();
		int lineIndex = map.getElementIndex(pos);
		Token tokenList = doc.getTokenListForLine(lineIndex);
		Rectangle lineArea = lineToRect(a, lineIndex);
		tabBase = lineArea.x; // Used by listOffsetToView().

		//int x = (int)RSyntaxUtilities.getTokenListWidthUpTo(tokenList,
		//							(RSyntaxTextArea)getContainer(),
		//							this, 0, pos);
		// We use this method instead as it returns the actual bounding box,
		// not just the x-coordinate.
		lineArea = tokenList.listOffsetToView(
						(RSyntaxTextArea)getContainer(), this, pos,
						tabBase, lineArea);

		return lineArea;

	}


	/**
	 * Provides a mapping, for a given region, from the document model
	 * coordinate space to the view coordinate space. The specified region is
	 * created as a union of the first and last character positions.<p>
	 *
	 * This is implemented to subtract the width of the second character, as
	 * this view's <code>modelToView</code> actually returns the width of the
	 * character instead of "1" or "0" like the View implementations in
	 * <code>javax.swing.text</code>.  Thus, if we don't override this method,
	 * the <code>View</code> implementation will return one character's width
	 * too much for its consumers (implementations of
	 * <code>javax.swing.text.Highlighter</code>).
	 *
	 * @param p0 the position of the first character (>=0)
	 * @param b0 The bias of the first character position, toward the previous
	 *        character or the next character represented by the offset, in
	 *        case the position is a boundary of two views; <code>b0</code>
	 *        will have one of these values:
	 * <ul>
	 *    <li> <code>Position.Bias.Forward</code>
	 *    <li> <code>Position.Bias.Backward</code>
	 * </ul>
	 * @param p1 the position of the last character (>=0)
	 * @param b1 the bias for the second character position, defined
	 *		one of the legal values shown above
	 * @param a the area of the view, which encompasses the requested region
	 * @return the bounding box which is a union of the region specified
	 *		by the first and last character positions
	 * @exception BadLocationException  if the given position does
	 *   not represent a valid location in the associated document
	 * @exception IllegalArgumentException if <code>b0</code> or
	 *		<code>b1</code> are not one of the
	 *		legal <code>Position.Bias</code> values listed above
	 * @see View#viewToModel
	 */
	public Shape modelToView(int p0, Position.Bias b0,
							int p1, Position.Bias b1,
							Shape a) throws BadLocationException {

		Shape s0 = modelToView(p0, a, b0);
		Shape s1;
		if (p1 ==getEndOffset()) {
			try {
				s1 = modelToView(p1, a, b1);
			} catch (BadLocationException ble) {
				s1 = null;
			}
			if (s1 == null) {
				// Assume extends left to right.
				Rectangle alloc = (a instanceof Rectangle) ? (Rectangle)a :
								a.getBounds();
				s1 = new Rectangle(alloc.x + alloc.width - 1, alloc.y,
								1, alloc.height);
			}
		}
		else {
			s1 = modelToView(p1, a, b1);
		}
		Rectangle r0 = s0 instanceof Rectangle ? (Rectangle)s0 : s0.getBounds();
		Rectangle r1 = s1 instanceof Rectangle ? (Rectangle)s1 : s1.getBounds();
		if (r0.y != r1.y) {
			// If it spans lines, force it to be the width of the view.
			Rectangle alloc = (a instanceof Rectangle) ? (Rectangle)a :
								a.getBounds();
			r0.x = alloc.x;
			r0.width = alloc.width;
		}

		r0.add(r1);
		// The next line is the only difference between this method and
		// View's implementation.  We're subtracting the width of the second
		// character.  This is because this method is used by Highlighter
		// implementations to get the area to "highlight", and if we don't do
		// this, one character too many is highlighted thanks to our
		// modelToView() implementation returning the actual width of the
		// character requested!
		if (p1>p0) r0.width -= r1.width;

		return r0;

	}


	/**
	 * Returns the next tab stop position after a given reference position.
	 * This implementation does not support things like centering so it
	 * ignores the tabOffset argument.
	 *
	 * @param x the current position >= 0
	 * @param tabOffset the position within the text stream
	 *   that the tab occurred at >= 0.
	 * @return the tab stop, measured in points >= 0
	 */
	public float nextTabStop(float x, int tabOffset) {
		if (tabSize == 0)
			return x;
		int ntabs = (((int)x) - tabBase) / tabSize;
		return tabBase + ((ntabs + 1) * tabSize);
	}


	/**
	 * Actually paints the text area.  Only lines that have been damaged are
	 * repainted.
	 *
	 * @param g The graphics context with which to paint.
	 * @param a The allocated region in which to render.
	 * @see #drawLine
	 */
	public void paint(Graphics g, Shape a) {

		RSyntaxDocument document = (RSyntaxDocument)getDocument();

		Rectangle alloc = a.getBounds();

		tabBase = alloc.x;
		host = (RSyntaxTextArea)getContainer();

		Rectangle clip = g.getClipBounds();
		// An attempt to speed things up for files with long lines.  Note that
		// this will actually slow things down a tad for the common case of
		// regular-length lines, but I don't think it'll make a difference
		// visually.  We'll see...
		clipStart = clip.x;
		clipEnd = clipStart + clip.width;

		lineHeight = host.getLineHeight();
		ascent = host.getMaxAscent();//metrics.getAscent();
		int heightAbove = clip.y - alloc.y;
		int linesAbove = Math.max(0, heightAbove / lineHeight);

		FoldManager fm = host.getFoldManager();
		linesAbove += fm.getHiddenLineCountAbove(linesAbove, true);
		Rectangle lineArea = lineToRect(a, linesAbove);
		int y = lineArea.y + ascent;
		int x = lineArea.x;
		Element map = getElement();
		int lineCount = map.getElementCount();

		RSyntaxTextAreaHighlighter h =
					(RSyntaxTextAreaHighlighter)host.getHighlighter();

		Graphics2D g2d = (Graphics2D)g;
		Token token;
		//System.err.println("Painting lines: " + linesAbove + " to " + (endLine-1));


		int line = linesAbove;
//int count = 0;
		while (y<clip.y+clip.height+lineHeight && line<lineCount) {

			Fold fold = fm.getFoldForLine(line);
			Element lineElement = map.getElement(line);
			int startOffset = lineElement.getStartOffset();
			//int endOffset = (line==lineCount ? lineElement.getEndOffset()-1 :
			//							lineElement.getEndOffset()-1);
			int endOffset = lineElement.getEndOffset()-1; // Why always "-1"?
			h.paintLayeredHighlights(g2d, startOffset, endOffset,
								a, host, this);
	
			// Paint a line of text.
			token = document.getTokenListForLine(line);
			drawLine(token, g2d, x,y);

			if (fold!=null && fold.isCollapsed()) {

				// Visible indicator of collapsed lines
				Color c = RSyntaxUtilities.getFoldedLineBottomColor(host);
				if (c!=null) {
					g.setColor(c);
					g.drawLine(x,y+lineHeight-ascent-1,
							alloc.width,y+lineHeight-ascent-1);
				}

				// Skip to next line to paint, taking extra care for lines with
				// block ends and begins together, e.g. "} else {"
				do {
					int hiddenLineCount = fold.getLineCount();
					if (hiddenLineCount==0) {
						// Fold parser identified a zero-line fold region.
						// This is really a bug, but we'll be graceful here
						// and avoid an infinite loop.
						break;
					}
					line += hiddenLineCount;
					fold = fm.getFoldForLine(line);
				} while (fold!=null && fold.isCollapsed());

			}

			y += lineHeight;
			line++;
			//count++;

		}

		//System.out.println("SyntaxView: lines painted=" + count);

	}


	/**
	 * If the passed-in line is longer than the current longest line, then
	 * the longest line is updated.
	 *
	 * @param line The line to test against the current longest.
	 * @param lineNumber The line number of the passed-in line.
	 * @return <code>true</code> iff the current longest line was updated.
	 */
	protected boolean possiblyUpdateLongLine(Element line, int lineNumber) {
		float w = getLineWidth(lineNumber);
		if (w > longLineWidth) {
			longLineWidth = w;
			longLine = line;
			return true;
		}
		return false;
	}


	/**
	 * Gives notification that something was removed from the document
	 * in a location that this view is responsible for.
	 *
	 * @param changes the change information from the associated document
	 * @param a the current allocation of the view
	 * @param f the factory to use to rebuild if the view has children
	 */
	public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
		updateDamage(changes, a, f);
	}


	public void setSize(float width, float height) {
		super.setSize(width, height);
		updateMetrics();
	}


	/**
	 * Repaint the region of change covered by the given document
	 * event.  Damages the line that begins the range to cover
	 * the case when the insert/remove is only on one line.  
	 * If lines are added or removed, damages the whole 
	 * view.  The longest line is checked to see if it has 
	 * changed.
	 */
	protected void updateDamage(DocumentEvent changes, Shape a, ViewFactory f) {
		Component host = getContainer();
		updateMetrics();
		Element elem = getElement();
		DocumentEvent.ElementChange ec = changes.getChange(elem);
		Element[] added = (ec != null) ? ec.getChildrenAdded() : null;
		Element[] removed = (ec != null) ? ec.getChildrenRemoved() : null;
		if (((added != null) && (added.length > 0)) || 
			((removed != null) && (removed.length > 0))) {
			// lines were added or removed...
			if (added != null) {
				int addedAt = ec.getIndex(); // FIXME: Is this correct?????
				for (int i = 0; i < added.length; i++)
					possiblyUpdateLongLine(added[i], addedAt+i);
			}
			if (removed != null) {
				for (int i = 0; i < removed.length; i++) {
					if (removed[i] == longLine) {
						longLineWidth = -1; // Must do this!!
						calculateLongestLine();
						break;
					}
				}
			}
			preferenceChanged(null, true, true);
			host.repaint();
		}

		// This occurs when syntax highlighting only changes on lines
		// (i.e. beginning a multiline comment).
		else if (changes.getType()==DocumentEvent.EventType.CHANGE) {
			//System.err.println("Updating the damage due to a CHANGE event...");
			int startLine = changes.getOffset();
			int endLine = changes.getLength();
			damageLineRange(startLine,endLine, a, host);
		}

		else {
			Element map = getElement();
			int line = map.getElementIndex(changes.getOffset());
			damageLineRange(line, line, a, host);
			if (changes.getType() == DocumentEvent.EventType.INSERT) {
				// check to see if the line is longer than current
				// longest line.
				Element e = map.getElement(line);
				if (e == longLine) {
					// We must recalculate longest line's width here
					// because it has gotten longer.
					longLineWidth = getLineWidth(line);
					preferenceChanged(null, true, false);
				}
				else {
					// If long line gets updated, update the status bars too.
					if (possiblyUpdateLongLine(e, line))
						preferenceChanged(null, true, false);
				}
			}
			else if (changes.getType() == DocumentEvent.EventType.REMOVE) {
				if (map.getElement(line) == longLine) {
					// removed from longest line... recalc
					longLineWidth = -1; // Must do this!
					calculateLongestLine();
					preferenceChanged(null, true, false);
				}			
			}
		}
	}


	/**
	 * Checks to see if the font metrics and longest line are up-to-date.
	 */
	protected void updateMetrics() {
		host = (RSyntaxTextArea)getContainer();
		Font f = host.getFont();
		if (font != f) {
			// The font changed, we need to recalculate the longest line!
			// This also updates cached font and tab size.
			calculateLongestLine();
		}
	}


	/**
	 * Provides a mapping from the view coordinate space to the logical
	 * coordinate space of the model.
	 *
	 * @param fx the X coordinate >= 0
	 * @param fy the Y coordinate >= 0
	 * @param a the allocated region to render into
	 * @return the location within the model that best represents the
	 *  given point in the view >= 0
	 */
	public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {

		bias[0] = Position.Bias.Forward;

		Rectangle alloc = a.getBounds();
		RSyntaxDocument doc = (RSyntaxDocument)getDocument();
		int x = (int) fx;
		int y = (int) fy;

		// If they're asking about a view position above the area covered by
		// this view, then the position is assumed to be the starting position
		// of this view.
		if (y < alloc.y) {
			return getStartOffset();
		}

		// If they're asking about a position below this view, the position
		// is assumed to be the ending position of this view.
		else if (y > alloc.y + alloc.height) {
			return host.getLastVisibleOffset();
		}

		// They're asking about a position within the coverage of this view
		// vertically.  So, we figure out which line the point corresponds to.
		// If the line is greater than the number of lines contained, then
		// simply use the last line as it represents the last possible place
		// we can position to.
		else {

			Element map = doc.getDefaultRootElement();
			int lineIndex = Math.abs((y - alloc.y) / lineHeight);//metrics.getHeight() );
FoldManager fm = host.getFoldManager();
//System.out.print("--- " + lineIndex);
lineIndex += fm.getHiddenLineCountAbove(lineIndex, true);
//System.out.println(" => " + lineIndex);
			if (lineIndex >= map.getElementCount()) {
				return host.getLastVisibleOffset();
			}

			Element line = map.getElement(lineIndex);

			// If the point is to the left of the line...
			if (x < alloc.x)
				return line.getStartOffset();

			// If the point is to the right of the line...
			else if (x > alloc.x + alloc.width)
				return line.getEndOffset() - 1;

			else {
				// Determine the offset into the text
				int p0 = line.getStartOffset();
				Token tokenList = doc.getTokenListForLine(lineIndex);
				tabBase = alloc.x;
				int offs = tokenList.getListOffset(
									(RSyntaxTextArea)getContainer(),
									this, tabBase, x);
				return offs!=-1 ? offs : p0;
			}

		} // End of else.

	} 


	/**
	 * {@inheritDoc}
	 */
	public int yForLine(Rectangle alloc, int line) throws BadLocationException {

		//Rectangle lineArea = lineToRect(alloc, lineIndex);
		updateMetrics();
		if (metrics != null) {
			// NOTE:  lineHeight is not initially set here, leading to the
			// current line not being highlighted when a document is first
			// opened.  So, we set it here just in case.
			lineHeight = host!=null ? host.getLineHeight() : lineHeight;
			FoldManager fm = host.getFoldManager();
			if (!fm.isLineHidden(line)) {
				line -= fm.getHiddenLineCountAbove(line);
				return alloc.y + line*lineHeight;
			}
		}

		return -1;

	}


	/**
	 * {@inheritDoc}
	 */
	public int yForLineContaining(Rectangle alloc, int offs)
							throws BadLocationException {
		Element map = getElement();
		int line = map.getElementIndex(offs);
		return yForLine(alloc, line);
	}


}