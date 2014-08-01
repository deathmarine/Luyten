/*
 * 10/28/2004
 *
 * VisibleWhitespaceToken.java - Token that paints special symbols for its
 * whitespace characters (space and tab).
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;


/**
 * This token class paints spaces and tabs with special symbols so the user
 * can see the whitespace in his document.  Rendering hints are honored.<p>
 *
 * The current implementation paints as follows:
 * <ul>
 *   <li>The first tab or space, if any, is found in the token.</li>
 *   <li>If a tab was found, all characters up to it are painted as a
 *       group.</li>
 *   <li>If a space was found, all characters up to and including it are
 *       painted (it is painted with a special symbol to denote it as
 *       a space).</li>
 *   <li>If neither a tab nor a whitespace was found, all characters in the
 *       token are painted.</li>
 *   <li>Repeat until all characters are painted.</li>
 * </ul>
 * This means that rendering hints are applied to all groups of characters
 * within a token, excluding whitespace and tabs.<p>
 *
 * A problem with this implementation is that FontMetrics.charsWidth() is still
 * used to calculate the width of a group of chars painted.  Thus, the group of
 * characters will be painted with the rendering hints specified, but the
 * following tab (or group of characters if the current group was the end of a
 * token) will not necessarily be painted at the proper x-coordinate (as
 * FontMetrics.charsWidth() returns an <code>int</code> and not a
 * <code>float</code>).  The way around this would be to calculate the token's
 * width in such a way that a float is returned (Font.getStringBounds()?).
 *
 * @author Robert Futrell
 * @version 0.5
 * @see Token
 * @see DefaultToken
 */
public class VisibleWhitespaceToken extends DefaultToken {

	private Rectangle2D.Float dotRect;


	/**
	 * Creates a "null token."  The token itself is not null; rather, it
	 * signifies that it is the last token in a linked list of tokens and
	 * that it is not part of a "multi-line token."
	 */
	public VisibleWhitespaceToken() {
		super();
		dotRect = new Rectangle2D.Float(0,0, 1,1);
	}


	/**
	 * Constructor.
	 *
	 * @param line The segment from which to get the token.
	 * @param beg The first character's position in <code>line</code>.
	 * @param end The last character's position in <code>line</code>.
	 * @param startOffset The offset into the document at which this
	 *        token begins.
	 * @param type A token type listed as "generic" above.
	 */
	public VisibleWhitespaceToken(final Segment line, final int beg,
					final int end, final int startOffset, final int type) {
		this(line.array, beg,end, startOffset, type);
	}


	/**
	 * Constructor.
	 *
	 * @param line The segment from which to get the token.
	 * @param beg The first character's position in <code>line</code>.
	 * @param end The last character's position in <code>line</code>.
	 * @param startOffset The offset into the document at which this
	 *        token begins.
	 * @param type A token type listed as "generic" above.
	 */
	public VisibleWhitespaceToken(final char[] line, final int beg,
					final int end, final int startOffset, final int type) {
		super(line, beg,end, startOffset, type);
	}


	/**
	 * Paints this token, using special symbols for whitespace characters.
	 *
	 * @param g The graphics context in which to paint.
	 * @param x The x-coordinate at which to paint.
	 * @param y The y-coordinate at which to paint.
	 * @param host The text area this token is in.
	 * @param e How to expand tabs.
	 * @param clipStart The left boundary of the clip rectangle in which we're
	 *        painting.  This optimizes painting by allowing us to not paint
	 *        not paint when this token is "to the left" of the clip rectangle.
	 * @return The x-coordinate representing the end of the painted text.
	 */
	public final float paint(Graphics2D g, float x, float y,
						RSyntaxTextArea host, TabExpander e,
						float clipStart) {

		int origX = (int)x;
		int end = textOffset + textCount;
		float nextX = x;
		int flushLen = 0;
		int flushIndex = textOffset;
		Color fg = host.getForegroundForToken(this);
		Color bg = host.getBackgroundForToken(this);
		g.setFont(host.getFontForTokenType(type));
		FontMetrics fm = host.getFontMetricsForTokenType(type);

		int ascent = fm.getAscent();
		int height = fm.getHeight();

		for (int i=textOffset; i<end; i++) {

			switch (text[i]) {

				case '\t':

					// Fill in background.
					nextX = x+fm.charsWidth(text, flushIndex,flushLen);
					float nextNextX = e.nextTabStop(nextX, 0);
					if (bg!=null) {
						paintBackground(x,y, nextNextX-x,height, g,
										ascent, host, bg);
					}
					g.setColor(fg);

					// Paint chars cached before the tab.
					if (flushLen > 0) {
						g.drawChars(text, flushIndex, flushLen, (int)x,(int)y);
						flushLen = 0;
					}
					flushIndex = i + 1;

					// Draw an arrow representing the tab.
					int halfHeight = height / 2;
					int quarterHeight = halfHeight / 2;
					int ymid = (int)y - ascent + halfHeight;
					g.drawLine((int)nextX,ymid, (int)nextNextX,ymid);
					g.drawLine((int)nextNextX,ymid, (int)nextNextX-4,ymid-quarterHeight);
					g.drawLine((int)nextNextX,ymid, (int)nextNextX-4,ymid+quarterHeight);

					x = nextNextX;
					break;

				case ' ':

					// NOTE:  There is a little bit of a "fudge factor"
					// here when "smooth text" is enabled, as "width"
					// below may well not be the width given to the space
					// by fm.charsWidth() (it depends on how it places the
					// space with respect to the preceding character).
					// But, we assume the approximation is close enough for
					// our drawing a dot for the space.

					// "flushLen+1" ensures text is aligned correctly (or,
					// aligned the same as in getWidth()).
					nextX = x+fm.charsWidth(text, flushIndex,flushLen+1);
					int width = fm.charWidth(' ');

					// Paint background.
					if (bg!=null) {
						paintBackground(x,y, nextX-x,height, g,
										ascent, host, bg);
					}
					g.setColor(fg);

					// Paint chars before space.
					if (flushLen>0) {
						g.drawChars(text, flushIndex, flushLen, (int)x,(int)y);
						flushLen = 0;
					}

					// Paint a dot representing the space.
					dotRect.x = nextX - width/2.0f; // "2.0f" for FindBugs
					dotRect.y = y - ascent + height/2.0f; // Ditto
					g.fill(dotRect);
					flushIndex = i + 1;
					x = nextX;
					break;


				case '\f':
					// ???
					// fall-through for now.

				default:
					flushLen += 1;
					break;

			}
		}

		nextX = x+fm.charsWidth(text, flushIndex,flushLen);

		if (flushLen>0 && nextX>=clipStart) {
			if (bg!=null) {
				paintBackground(x,y, nextX-x,height, g,
							ascent, host, bg);
			}
			g.setColor(fg);
			g.drawChars(text, flushIndex, flushLen, (int)x,(int)y);
		}

		if (host.getUnderlineForToken(this)) {
			g.setColor(fg);
			int y2 = (int)(y+1);
			g.drawLine(origX,y2, (int)nextX,y2);
		}

		// Don't check if it's whitespace - some TokenMakers may return types
		// other than Token.WHITESPACE for spaces (such as Token.IDENTIFIER).
		// This also allows us to paint tab lines for MLC's.
		if (host.getPaintTabLines() && origX==host.getMargin().left) {// && isWhitespace()) {
			paintTabLines(origX, (int)y, (int)nextX, g, e, host);
		}

		return nextX;

	}


}