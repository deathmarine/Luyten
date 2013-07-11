/*
 * 08/06/2004
 *
 * WrappedSyntaxView.java - Test implementation of WrappedSyntaxView that
 * is also aware of RSyntaxTextArea's different fonts per token type.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.*;
import javax.swing.text.*;
import javax.swing.text.Position.Bias;
import javax.swing.event.*;

import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rtextarea.Gutter;


/**
 * The view used by {@link RSyntaxTextArea} when word wrap is enabled.
 *
 * @author Robert Futrell
 * @version 0.2
 */
public class WrappedSyntaxView extends BoxView implements TabExpander,
												RSTAView {

	boolean widthChanging;
	int tabBase;
	int tabSize;
    
	/**
	 * This is reused to keep from allocating/deallocating.
	 */
	private Segment s, drawSeg;
	
	/**
	 * Another variable initialized once to keep from allocating/deallocating.
	 */
	private Rectangle tempRect;

	/**
	 * Cached for each paint() call so each drawView() call has access to it.
	 */
	private RSyntaxTextArea host;
	private FontMetrics metrics;

//	/**
//	 * The end-of-line marker.
//	 */
//	private static final char[] eolMarker = { '.' };

	/**
	 * The width of this view cannot be below this amount, as if the width
	 * is ever 0 (really a bug), we'll go into an infinite loop.
	 */
	private static final int MIN_WIDTH		= 20;


	/**
	 * Creates a new WrappedSyntaxView.  Lines will be wrapped
	 * on character boundaries.
	 *
	 * @param elem the element underlying the view
	 */
	public WrappedSyntaxView(Element elem) {
		super(elem, Y_AXIS);
		s = new Segment();
		drawSeg = new Segment();
		tempRect = new Rectangle();
	}



	/**
	 * This is called by the nested wrapped line
	 * views to determine the break location.  This can
	 * be reimplemented to alter the breaking behavior.
	 * It will either break at word or character boundaries
	 * depending upon the break argument given at
	 * construction.
	 */
	protected int calculateBreakPosition(int p0, Token tokenList, float x0) {
//System.err.println("------ beginning calculateBreakPosition() --------");
		int p = p0;
		RSyntaxTextArea textArea = (RSyntaxTextArea)getContainer();
		float currentWidth = getWidth();
		if (currentWidth==Integer.MAX_VALUE)
			currentWidth = getPreferredSpan(X_AXIS);
		// Make sure width>0; this is a huge hack to fix a bug where
		// loading text into an RTextArea before it is visible if word wrap
		// is enabled causes an infinite loop in calculateBreakPosition()
		// because of the 0-width!  We cannot simply check in setSize()
		// because the width is set to 0 somewhere else too somehow...
		currentWidth = Math.max(currentWidth, MIN_WIDTH);
		Token t = tokenList;
		while (t!=null && t.isPaintable()) {
// FIXME:  Replace the code below with the commented-out line below.  This will
// allow long tokens to be broken at embedded spaces (such as MLC's).  But it
// currently throws BadLocationExceptions sometimes...
			float tokenWidth = t.getWidth(textArea, this, x0);
			if (tokenWidth>currentWidth) {
				// If the current token alone is too long for this line,
				// break at a character boundary.
				if (p==p0) {
					return t.getOffsetBeforeX(textArea, this, 0, currentWidth);
				}
				// Return the first non-whitespace char (i.e., don't start
				// off the continuation of a wrapped line with whitespace).
				return t.isWhitespace() ? p+t.textCount : p;
//return getBreakLocation(t, fm, x0, currentWidth, this);
			}
			currentWidth -= tokenWidth;
			x0 += tokenWidth;
			p += t.textCount;
//System.err.println("*** *** *** token fit entirely (width==" + tokenWidth + "), adding " + t.textCount + " to p, now p==" + p);
			t = t.getNextToken();
		}
//System.err.println("... ... whole line fits; returning p==" + p);
//System.err.println("------ ending calculateBreakPosition() --------");

//		return p;
return p + 1;
	}

//private int getBreakLocation(Token t, FontMetrics fm, int x0, int x,
//								TabExpander e) {
//	Segment s = new Segment();
//	s.array = t.text;
//	s.offset = t.textOffset;
//	s.count = t.textCount;
//	return t.offset + Utilities.getBreakLocation(s, fm, x0, x, e, t.offset);
//}

	/**
	 * Gives notification from the document that attributes were changed
	 * in a location that this view is responsible for.
	 *
	 * @param e the change information from the associated document
	 * @param a the current allocation of the view
	 * @param f the factory to use to rebuild if the view has children
	 * @see View#changedUpdate
	 */
	public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
		updateChildren(e, a);
	}


	/**
	 * Sets the allocation rectangle for a given line's view, but sets the
	 * y value to the passed-in value.  This should be used instead of
	 * {@link #childAllocation(int, Rectangle)} since it allows you to account
	 * for hidden lines in collapsed fold regions.
	 *
	 * @param line
	 * @param y
	 * @param alloc
	 */
	private void childAllocation2(int line, int y, Rectangle alloc) {
		alloc.x += getOffset(X_AXIS, line);
		alloc.y += y;
		alloc.width = getSpan(X_AXIS, line);
		alloc.height = getSpan(Y_AXIS, line);
	}


	/**
	 * Draws a single view (i.e., a line of text for a wrapped view),
	 * wrapping the text onto multiple lines if necessary.
	 *
	 * @param g The graphics context in which to paint.
	 * @param r The rectangle in which to paint.
	 * @param view The <code>View</code> to paint.
	 * @param fontHeight The height of the font being used.
	 * @param y The y-coordinate at which to begin painting.
	 */
	protected void drawView(Graphics2D g, Rectangle r, View view,
						int fontHeight, int y) {

		float x = r.x;

		LayeredHighlighter h = (LayeredHighlighter)host.getHighlighter();

		RSyntaxDocument document = (RSyntaxDocument)getDocument();
		Element map = getElement();

		int p0 = view.getStartOffset();
		int lineNumber = map.getElementIndex(p0);
		int p1 = view.getEndOffset();// - 1;

		setSegment(p0,p1-1, document, drawSeg);
		//System.err.println("drawSeg=='" + drawSeg + "' (p0/p1==" + p0 + "/" + p1 + ")");
		int start = p0 - drawSeg.offset;
		Token token = document.getTokenListForLine(lineNumber);

		// If this line is an empty line, then the token list is simply a
		// null token.  In this case, the line highlight will be skipped in
		// the loop below, so unfortunately we must manually do it here.
		if (token!=null && token.type==Token.NULL) {
			h.paintLayeredHighlights(g, p0,p1, r, host, this);
			return;
		}

		// Loop through all tokens in this view and paint them!
		while (token!=null && token.isPaintable()) {

			int p = calculateBreakPosition(p0, token, x);
			x = r.x;

			h.paintLayeredHighlights(g, p0,p, r, host, this);

			while (token!=null && token.isPaintable() && token.offset+token.textCount-1<p) {//<=p) {
				x = token.paint(g, x,y, host, this);
				token = token.getNextToken();
			}
			
			if (token!=null && token.isPaintable() && token.offset<p) {
				int tokenOffset = token.offset;
				Token temp = new DefaultToken(drawSeg, tokenOffset-start,
									p-1-start, tokenOffset,
									token.type);
				temp.paint(g, x,y, host, this);
				temp = null;
				token.makeStartAt(p);
			}

			p0 = (p==p0) ? p1 : p;
			y += fontHeight;
			
		} // End of while (token!=null && token.isPaintable()).

		// NOTE: We should re-use code from Token (paintBackground()) here,
		// but don't because I'm just too lazy.
		if (host.getEOLMarkersVisible()) {
			g.setColor(host.getForegroundForTokenType(Token.WHITESPACE));
			g.setFont(host.getFontForTokenType(Token.WHITESPACE));
			g.drawString("\u00B6", x, y-fontHeight);
		}

	}


	/**
	 * Fetches the allocation for the given child view.<p>
	 * Overridden to account for code folding.
	 * 
	 * @param index The index of the child, >= 0 && < getViewCount().
	 * @param a The allocation to this view
	 * @return The allocation to the child; or <code>null</code> if
	 *         <code>a</code> is <code>null</code>; or <code>null</code> if the
	 *         layout is invalid
	 */
	public Shape getChildAllocation(int index, Shape a) {
		if (a != null) {
			Shape ca = getChildAllocationImpl(index, a);
			if ((ca != null) && (!isAllocationValid())) {
				// The child allocation may not have been set yet.
				Rectangle r = (ca instanceof Rectangle) ? (Rectangle) ca : ca
						.getBounds();
				if ((r.width == 0) && (r.height == 0)) {
					return null;
				}
			}
			return ca;
		}
		return null;
	}

	/**
	 * Fetches the allocation for the given child view to render into.<p>
	 * Overridden to account for lines hidden by collapsed folded regions.
	 * 
	 * @param line The index of the child, >= 0 && < getViewCount()
	 * @param a The allocation to this view
	 * @return The allocation to the child
	 */
	public Shape getChildAllocationImpl(int line, Shape a) {

		Rectangle alloc = getInsideAllocation(a);
		host = (RSyntaxTextArea)getContainer();
		FoldManager fm = host.getFoldManager();
		int y = alloc.y;

		// TODO: Make cached getOffset() calls for Y_AXIS valid even for
		// folding, to speed this up!
		for (int i=0; i<line; i++) {
			y += getSpan(Y_AXIS, i);
			Fold fold = fm.getFoldForLine(i);
			if (fold!=null && fold.isCollapsed()) {
				i += fold.getCollapsedLineCount();
			}
		}

		childAllocation2(line, y, alloc);
		return alloc;

	}


	/**
	 * Determines the maximum span for this view along an
	 * axis.  This is implemented to provide the superclass
	 * behavior after first making sure that the current font
	 * metrics are cached (for the nested lines which use
	 * the metrics to determine the height of the potentially
	 * wrapped lines).
	 *
	 * @param axis may be either View.X_AXIS or View.Y_AXIS
	 * @return  the span the view would like to be rendered into.
	 *           Typically the view is told to render into the span
	 *           that is returned, although there is no guarantee.  
	 *           The parent may choose to resize or break the view.
	 * @see View#getMaximumSpan
	 */
	public float getMaximumSpan(int axis) {
		updateMetrics();
		float span = super.getPreferredSpan(axis);
		if (axis==View.X_AXIS) { // EOL marker
			span += metrics.charWidth('\u00b6'); // metrics set in updateMetrics
		}
		return span;
	}


	/**
	 * Determines the minimum span for this view along an
	 * axis.  This is implemented to provide the superclass
	 * behavior after first making sure that the current font
	 * metrics are cached (for the nested lines which use
	 * the metrics to determine the height of the potentially
	 * wrapped lines).
	 *
	 * @param axis may be either View.X_AXIS or View.Y_AXIS
	 * @return  the span the view would like to be rendered into.
	 *           Typically the view is told to render into the span
	 *           that is returned, although there is no guarantee.  
	 *           The parent may choose to resize or break the view.
	 * @see View#getMinimumSpan
	 */
	public float getMinimumSpan(int axis) {
		updateMetrics();
		float span = super.getPreferredSpan(axis);
		if (axis==View.X_AXIS) { // EOL marker
			span += metrics.charWidth('\u00b6'); // metrics set in updateMetrics
		}
		return span;
	}


	/**
	 * Determines the preferred span for this view along an
	 * axis.  This is implemented to provide the superclass
	 * behavior after first making sure that the current font
	 * metrics are cached (for the nested lines which use
	 * the metrics to determine the height of the potentially
	 * wrapped lines).
	 *
	 * @param axis may be either View.X_AXIS or View.Y_AXIS
	 * @return  the span the view would like to be rendered into.
	 *           Typically the view is told to render into the span
	 *           that is returned, although there is no guarantee.  
	 *           The parent may choose to resize or break the view.
	 * @see View#getPreferredSpan
	 */
	public float getPreferredSpan(int axis) {
		updateMetrics();
		float span = 0;
		if (axis==View.X_AXIS) { // Add EOL marker
			span = super.getPreferredSpan(axis);
			span += metrics.charWidth('\u00b6'); // metrics set in updateMetrics
		}
		else {
			span = super.getPreferredSpan(axis);
			host = (RSyntaxTextArea)getContainer();
			if (host.isCodeFoldingEnabled()) {
				// TODO: Cache y-offsets again to speed this up
				//System.out.println("y-axis baby");
				int lineCount = host.getLineCount();
				FoldManager fm = host.getFoldManager();
				for (int i=0; i<lineCount; i++) {
					if (fm.isLineHidden(i)) {
						span -= getSpan(View.Y_AXIS, i);
					}
				}
			}
		}
		return span;
	}


	/**
	 * Returns the tab size set for the document, defaulting to 5.
	 *
	 * @return the tab size
	 */
	protected int getTabSize() {
		Integer i = (Integer) getDocument().
							getProperty(PlainDocument.tabSizeAttribute);
		int size = (i != null) ? i.intValue() : 5;
		return size;
	}


	/**
	 * Overridden to allow for folded regions.
	 */
	protected View getViewAtPoint(int x, int y, Rectangle alloc) {

		int lineCount = getViewCount();
		int curY = alloc.y + getOffset(Y_AXIS, 0); // Always at least 1 line
		host = (RSyntaxTextArea)getContainer();
		FoldManager fm = host.getFoldManager();

		for (int line=1; line<lineCount; line++) {
			int span = getSpan(Y_AXIS, line-1);
			if (y<curY+span) {
				childAllocation2(line-1, curY, alloc);
				return getView(line-1);
			}
			curY += span;
			Fold fold = fm.getFoldForLine(line-1);
			if (fold!=null && fold.isCollapsed()) {
				line += fold.getCollapsedLineCount();
			}
		}

		// Not found - return last line's view.
		childAllocation2(lineCount - 1, curY, alloc);
		return getView(lineCount - 1);

	}


	/**
	 * Gives notification that something was inserted into the 
	 * document in a location that this view is responsible for.
	 * This is implemented to simply update the children.
	 *
	 * @param changes The change information from the associated document.
	 * @param a the current allocation of the view
	 * @param f the factory to use to rebuild if the view has children
	 * @see View#insertUpdate
	 */
	public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
		updateChildren(changes, a);
		Rectangle alloc = ((a != null) && isAllocationValid()) ? 
									getInsideAllocation(a) : null;
		int pos = changes.getOffset();
		View v = getViewAtPosition(pos, alloc);
		if (v != null)
			v.insertUpdate(changes, alloc, f);
	}


	/**
	 * Loads all of the children to initialize the view.
	 * This is called by the <code>setParent</code> method.
	 * Subclasses can re-implement this to initialize their
	 * child views in a different manner.  The default
	 * implementation creates a child view for each 
	 * child element.
	 *
	 * @param f the view factory
	 */
	protected void loadChildren(ViewFactory f) {
		Element e = getElement();
		int n = e.getElementCount();
		if (n > 0) {
			View[] added = new View[n];
			for (int i = 0; i < n; i++)
				added[i] = new WrappedLine(e.getElement(i));
			replace(0, 0, added);
		}
	}


	public Shape modelToView(int pos, Shape a, Position.Bias b)
			throws BadLocationException {

		if (! isAllocationValid()) {
			Rectangle alloc = a.getBounds();
			setSize(alloc.width, alloc.height);
		}

		boolean isBackward = (b == Position.Bias.Backward);
		int testPos = (isBackward) ? Math.max(0, pos - 1) : pos;
		if(isBackward && testPos < getStartOffset()) {
			return null;
		}

		int vIndex = getViewIndexAtPosition(testPos);
		if ((vIndex != -1) && (vIndex < getViewCount())) {
			View v = getView(vIndex);
			if(v != null && testPos >= v.getStartOffset() &&
					testPos < v.getEndOffset()) {
				Shape childShape = getChildAllocation(vIndex, a);
				if (childShape == null) {
					// We are likely invalid, fail.
					return null;
				}
				Shape retShape = v.modelToView(pos, childShape, b);
				if(retShape == null && v.getEndOffset() == pos) {
					if(++vIndex < getViewCount()) {
						v = getView(vIndex);
						retShape = v.modelToView(pos, getChildAllocation(vIndex, a), b);
					}
				}
				return retShape;
			}
		}

		throw new BadLocationException("Position not represented by view", pos);

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
		Rectangle r0 = s0.getBounds();
		Rectangle r1 = (s1 instanceof Rectangle) ? (Rectangle) s1 :
													s1.getBounds();
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
		int ntabs = ((int) x - tabBase) / tabSize;
		return tabBase + ((ntabs + 1) * tabSize);
	}


	/**
	 * Paints the word-wrapped text.
	 *
	 * @param g The graphics context in which to paint.
	 * @param a The shape (usually a rectangle) in which to paint.
	 */
	public void paint(Graphics g, Shape a) {

		Rectangle alloc = (a instanceof Rectangle) ?
							(Rectangle)a : a.getBounds();
		tabBase = alloc.x;

		Graphics2D g2d = (Graphics2D)g;
		host = (RSyntaxTextArea)getContainer();
		int ascent = host.getMaxAscent();
		int fontHeight = host.getLineHeight();
		FoldManager fm = host.getFoldManager();

		int n = getViewCount();	// Number of lines.
		int x = alloc.x + getLeftInset();
		tempRect.y = alloc.y + getTopInset();
		Rectangle clip = g.getClipBounds();
		for (int i = 0; i < n; i++) {
			tempRect.x = x + getOffset(X_AXIS, i);
			//tempRect.y = y + getOffset(Y_AXIS, i);
			tempRect.width = getSpan(X_AXIS, i);
			tempRect.height = getSpan(Y_AXIS, i);
			//System.err.println("For line " + i + ": tempRect==" + tempRect);
			if (tempRect.intersects(clip)) {
				View view = getView(i);
				drawView(g2d, alloc, view, fontHeight, tempRect.y+ascent);
			}
			tempRect.y += tempRect.height;
			Fold possibleFold = fm.getFoldForLine(i);
			if (possibleFold!=null && possibleFold.isCollapsed()) {
				i += possibleFold.getCollapsedLineCount();
				// Visible indicator of collapsed lines
				Color c = RSyntaxUtilities.getFoldedLineBottomColor(host);
				if (c!=null) {
					g.setColor(c);
					g.drawLine(x,tempRect.y-1, alloc.width,tempRect.y-1);
				}
			}
		}

	}


	/**
	 * Gives notification that something was removed from the 
	 * document in a location that this view is responsible for.
	 * This is implemented to simply update the children.
	 *
	 * @param changes The change information from the associated document.
	 * @param a the current allocation of the view
	 * @param f the factory to use to rebuild if the view has children
	 * @see View#removeUpdate
	 */
	public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {

		updateChildren(changes, a);

		Rectangle alloc = ((a != null) && isAllocationValid()) ? 
									getInsideAllocation(a) : null;
		int pos = changes.getOffset();
		View v = getViewAtPosition(pos, alloc);
		if (v != null)
            v.removeUpdate(changes, alloc, f);

	}


	/**
	 * Makes a <code>Segment</code> point to the text in our
	 * document between the given positions.  Note that the positions MUST be
	 * valid positions in the document.
	 *
	 * @param p0 The first position in the document.
	 * @param p1 The second position in the document.
	 * @param document The document from which you want to get the text.
	 * @param seg The segment in which to load the text.
	 */
	private void setSegment(int p0, int p1, Document document,
							Segment seg) {
		try {
//System.err.println("... in setSharedSegment, p0/p1==" + p0 + "/" + p1);
			document.getText(p0, p1-p0, seg);
			//System.err.println("... in setSharedSegment: s=='" + s + "'; line/numLines==" + line + "/" + numLines);
		} catch (BadLocationException ble) { // Never happens
			ble.printStackTrace();
		}
	}


	/**
	 * Sets the size of the view.  This should cause layout of the view along
	 * the given axis, if it has any layout duties.
	 *
	 * @param width the width >= 0
	 * @param height the height >= 0
	 */
	public void setSize(float width, float height) {
		updateMetrics();
		if ((int) width != getWidth()) {
			// invalidate the view itself since the childrens
			// desired widths will be based upon this views width.
			preferenceChanged(null, true, true);
			widthChanging = true;
		}
		super.setSize(width, height);
		widthChanging = false;
	}


	/**
	 * Update the child views in response to a 
	 * document event.
	 */
	void updateChildren(DocumentEvent e, Shape a) {

		Element elem = getElement();
		DocumentEvent.ElementChange ec = e.getChange(elem);

		// This occurs when syntax highlighting only changes on lines
		// (i.e. beginning a multiline comment).
		if (e.getType()==DocumentEvent.EventType.CHANGE) {
			//System.err.println("Updating the damage due to a CHANGE event...");
			// FIXME:  Make me repaint more intelligently.
			getContainer().repaint();
			//damageLineRange(startLine,endLine, a, host);
		}

		else if (ec != null) {

			// the structure of this element changed.
			Element[] removedElems = ec.getChildrenRemoved();
			Element[] addedElems = ec.getChildrenAdded();
			View[] added = new View[addedElems.length];

			for (int i = 0; i < addedElems.length; i++)
				added[i] = new WrappedLine(addedElems[i]);
			//System.err.println("Replacing " + removedElems.length +
			// " children with " + addedElems.length);
			replace(ec.getIndex(), removedElems.length, added);

			// should damge a little more intelligently.
			if (a != null) {
				preferenceChanged(null, true, true);
				getContainer().repaint();
			}

		}

		// update font metrics which may be used by the child views
		updateMetrics();

	}


	final void updateMetrics() {
		Component host = getContainer();
		Font f = host.getFont();
		metrics = host.getFontMetrics(f); // Metrics for the default font.
		tabSize = getTabSize() * metrics.charWidth('m');
	}


	public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {

		int offs = -1;

		if (! isAllocationValid()) {
			Rectangle alloc = a.getBounds();
			setSize(alloc.width, alloc.height);
		}

		// Get the child view for the line at (x,y), and ask it for the
		// specific offset.
		Rectangle alloc = getInsideAllocation(a);
		View v = getViewAtPoint((int) x, (int) y, alloc);
		if (v != null) {
			offs = v.viewToModel(x, y, alloc, bias);
		}

		// Code folding may have hidden the last line.  If so, return the last
		// visible offset instead of the last offset.
		if (host.isCodeFoldingEnabled() && v==getView(getViewCount()-1) &&
				offs==v.getEndOffset()-1) {
			offs = host.getLastVisibleOffset();
		}

		return offs;

	}


	/**
	 * {@inheritDoc}
	 */
	public int yForLine(Rectangle alloc, int line) throws BadLocationException {
		return yForLineContaining(alloc,
				getElement().getElement(line).getStartOffset());
//return alloc.y + getOffset(Y_AXIS, line);
	}


	/**
	 * {@inheritDoc}
	 */
	public int yForLineContaining(Rectangle alloc, int offs)
								throws BadLocationException {
		if (isAllocationValid()) {
			// TODO: make cached Y_AXIS offsets valid even with folding enabled
			// to speed this back up!
			Rectangle r = (Rectangle)modelToView(offs, alloc, Bias.Forward);
			if (r!=null) {
				if (host.isCodeFoldingEnabled()) {
					int line = host.getLineOfOffset(offs);
					FoldManager fm = host.getFoldManager();
					if (fm.isLineHidden(line)) {
						return -1;
					}
				}
				return r.y;
			}
		}
		return -1;
	}


	/**
	 * Simple view of a line that wraps if it doesn't
	 * fit within the horizontal space allocated.
	 * This class tries to be lightweight by carrying little 
	 * state of it's own and sharing the state of the outer class 
	 * with it's siblings.
	 */
	class WrappedLine extends View {

		int nlines;

		WrappedLine(Element elem) {
			super(elem);
		}

		/**
		 * Calculate the number of lines that will be rendered
		 * by logical line when it is wrapped.
		 */
		final int calculateLineCount() {

			int nlines = 0;
			int startOffset = getStartOffset();
			int p1 = getEndOffset();

			// Get the token list for this line so we don't have to keep
			// recomputing it if this logical line spans multiple physical
			// lines.
			RSyntaxTextArea textArea = (RSyntaxTextArea)getContainer();
			RSyntaxDocument doc = (RSyntaxDocument)getDocument();
			Element map = doc.getDefaultRootElement();
			int line = map.getElementIndex(startOffset);
			Token tokenList = doc.getTokenListForLine(line);
			float x0 = 0;// FIXME:  should be alloc.x!! alloc.x;//0;


//System.err.println(">>> calculateLineCount: " + startOffset + "-" + p1);
			for (int p0=startOffset; p0<p1; ) {
//System.err.println("... ... " + p0 + ", " + p1);
				nlines += 1;
				x0 = RSyntaxUtilities.makeTokenListStartAt(tokenList, p0,
							WrappedSyntaxView.this, textArea, x0);
				int p = calculateBreakPosition(p0, tokenList, x0);

//System.err.println("... ... ... break position p==" + p);
				p0 = (p == p0) ? ++p : p; // this is the fix of #4410243
									// we check on situation when
									// width is too small and
									// break position is calculated
									// incorrectly.
//System.err.println("... ... ... new p0==" + p0);
			}
/*
int numLines = 0;
try {
	numLines = textArea.getLineCount();
} catch (BadLocationException ble) {
	ble.printStackTrace();
}
System.err.println(">>> >>> calculated number of lines for this view (line " + line + "/" + numLines + ": " + nlines);
*/
			return nlines;
		}

		/**
		 * Determines the preferred span for this view along an
		 * axis.
		 *
		 * @param axis may be either X_AXIS or Y_AXIS
		 * @return   the span the view would like to be rendered into.
		 *           Typically the view is told to render into the span
		 *           that is returned, although there is no guarantee.  
		 *           The parent may choose to resize or break the view.
		 * @see View#getPreferredSpan
		 */
		public float getPreferredSpan(int axis) {
			switch (axis) {
				case View.X_AXIS:
					float width = getWidth();
					if (width == Integer.MAX_VALUE) {
						// We have been initially set to MAX_VALUE, but we don't
						// want this as our preferred.
						return 100f;
					}
					return width;
				case View.Y_AXIS:
					if (nlines == 0 || widthChanging)
						nlines = calculateLineCount();
					int h = nlines * ((RSyntaxTextArea)getContainer()).getLineHeight();
					return h;
				default:
					throw new IllegalArgumentException("Invalid axis: " + axis);
			}
		}

		/**
		 * Renders using the given rendering surface and area on that
		 * surface.  The view may need to do layout and create child
		 * views to enable itself to render into the given allocation.
		 *
		 * @param g the rendering surface to use
		 * @param a the allocated region to render into
		 * @see View#paint
		 */
		public void paint(Graphics g, Shape a) {
			// This is done by drawView() above.
		}

		/**
		 * Provides a mapping from the document model coordinate space
		 * to the coordinate space of the view mapped to it.
		 *
		 * @param pos the position to convert
		 * @param a the allocated region to render into
		 * @return the bounding box of the given position is returned
		 * @exception BadLocationException  if the given position does not
		 *            represent a valid location in the associated document.
		 */
		public Shape modelToView(int pos, Shape a, Position.Bias b)
										throws BadLocationException {

			//System.err.println("--- begin modelToView ---");
			Rectangle alloc = a.getBounds();
			RSyntaxTextArea textArea = (RSyntaxTextArea)getContainer();
			alloc.height = textArea.getLineHeight();//metrics.getHeight();
			alloc.width = 1;
			int p0 = getStartOffset();
			int p1 = getEndOffset();
			int testP = (b == Position.Bias.Forward) ? pos :
											Math.max(p0, pos - 1);

			// Get the token list for this line so we don't have to keep
			// recomputing it if this logical line spans multiple physical
			// lines.
			RSyntaxDocument doc = (RSyntaxDocument)getDocument();
			Element map = doc.getDefaultRootElement();
			int line = map.getElementIndex(p0);
			Token tokenList = doc.getTokenListForLine(line);
			float x0 = alloc.x;//0;

			while (p0 < p1) {
				x0 = RSyntaxUtilities.makeTokenListStartAt(tokenList, p0,
							WrappedSyntaxView.this, textArea, x0);
				int p = calculateBreakPosition(p0, tokenList, x0);
				if ((pos >= p0) && (testP<p)) {//pos < p)) {
					// it's in this line
					alloc = RSyntaxUtilities.getLineWidthUpTo(
									textArea, s, p0, pos,
									WrappedSyntaxView.this,
									alloc, alloc.x);
					//System.err.println("--- end modelToView ---");
					return alloc;
				}
				//if (p == p1 && pos == p1) {
				if (p==p1-1 && pos==p1-1) {
					// Wants end.
					if (pos > p0) {
						alloc = RSyntaxUtilities.getLineWidthUpTo(
									textArea, s, p0, pos,
									WrappedSyntaxView.this,
									alloc, alloc.x);
					}
					//System.err.println("--- end modelToView ---");
					return alloc;
				}

				p0 = (p == p0) ? p1 : p;
				//System.err.println("... ... Incrementing y");
				alloc.y += alloc.height;

			}

			throw new BadLocationException(null, pos);

		}

		/**
		 * Provides a mapping from the view coordinate space to the logical
		 * coordinate space of the model.
		 *
		 * @param fx the X coordinate
		 * @param fy the Y coordinate
		 * @param a the allocated region to render into
		 * @return the location within the model that best represents the
		 *  given point in the view
		 * @see View#viewToModel
		 */
		public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {

			// PENDING(prinz) implement bias properly
			bias[0] = Position.Bias.Forward;

			Rectangle alloc = (Rectangle) a;
			RSyntaxDocument doc = (RSyntaxDocument)getDocument();
			int x = (int) fx;
			int y = (int) fy;
			if (y < alloc.y) {
				// above the area covered by this icon, so the the position
				// is assumed to be the start of the coverage for this view.
				return getStartOffset();
			}
			else if (y > alloc.y + alloc.height) {
				// below the area covered by this icon, so the the position
				// is assumed to be the end of the coverage for this view.
				return getEndOffset() - 1;
			}
			else {

				// positioned within the coverage of this view vertically,
				// so we figure out which line the point corresponds to.
				// if the line is greater than the number of lines
				// contained, then simply use the last line as it represents
				// the last possible place we can position to.

				RSyntaxTextArea textArea = (RSyntaxTextArea)getContainer();
				alloc.height = textArea.getLineHeight();
				int p1 = getEndOffset();

				// Get the token list for this line so we don't have to keep
				// recomputing it if this logical line spans multiple
				// physical lines.
				Element map = doc.getDefaultRootElement();
				int p0 = getStartOffset();
				int line = map.getElementIndex(p0);
				Token tlist = doc.getTokenListForLine(line);

				// Look at each physical line-chunk of this logical line.
				while (p0<p1) {

					// We can always use alloc.x since we always break
					// lines so they start at the beginning of a physical
					// line.
					RSyntaxUtilities.makeTokenListStartAt(tlist, p0,
						WrappedSyntaxView.this, textArea, alloc.x);
					int p = calculateBreakPosition(p0, tlist, alloc.x);

					// If desired view position is in this physical chunk.
					if ((y>=alloc.y) && (y<(alloc.y+alloc.height))) {

						// Point is to the left of the line
						if (x < alloc.x) {
							return p0;
						}

						// Point is to the right of the line
						else if (x > alloc.x + alloc.width) {
							return p - 1;
						}

						// Point is in this physical line!
						else {

							// Start at alloc.x since this chunk starts
							// at the beginning of a physical line.
							int n = tlist.getListOffset(textArea,
										WrappedSyntaxView.this,
										alloc.x, x);

							// NOTE:  We needed to add the max() with
							// p0 as getTokenListForLine returns -1
							// for empty lines (just a null token).
							// How did this work before?
							// FIXME:  Have null tokens have their
							// offset but a -1 length.
							return Math.max(Math.min(n, p1-1), p0);

						}  // End of else.

					} // End of if ((y>=alloc.y) && ...

					p0 = (p == p0) ? p1 : p;
					alloc.y += alloc.height;

				} // End of while (p0<p1).

				return getEndOffset() - 1;

			} // End of else.

		}

		private void handleDocumentEvent(DocumentEvent e, Shape a,
											ViewFactory f) {
			int n = calculateLineCount();
			if (this.nlines != n) {
				this.nlines = n;
				WrappedSyntaxView.this.preferenceChanged(this, false, true);
				// have to repaint any views after the receiver.
				RSyntaxTextArea textArea = (RSyntaxTextArea)getContainer();
				textArea.repaint();
				// Must also revalidate container so gutter components, such
				// as line numbers, get updated for this line's new height
				Gutter gutter = RSyntaxUtilities.getGutter(textArea);
				if (gutter!=null) {
					gutter.revalidate();
					gutter.repaint();
				}
			}
			else if (a != null) {
				Component c = getContainer();
				Rectangle alloc = (Rectangle) a;
				c.repaint(alloc.x, alloc.y, alloc.width, alloc.height);
			}
		}

		public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
			handleDocumentEvent(e, a, f);
		}

		public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
			handleDocumentEvent(e, a, f);
		}

	}


}