/*
 * 02/21/2004
 *
 * Token.java - A token used in syntax highlighting.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.text.TabExpander;


/**
 * A generic token that functions as a node in a linked list of syntax
 * highlighted tokens for some language.<p>
 *
 * A <code>Token</code> is a piece of text representing some logical token in
 * source code for a programming language.  For example, the line of C code:<p>
 * <pre>
 * int i = 0;
 * </pre>
 * would be broken into 8 <code>Token</code>s: the first representing
 * <code>int</code>, the second whitespace, the third <code>i</code>, the fourth
 * whitespace, the fifth <code>=</code>, etc.<p>
 *
 * @author Robert Futrell
 * @version 0.3
 */
public abstract class Token implements TokenTypes {

	/**
	 * The text this token represents.  This is implemented as a segment so we
	 * can point directly to the text in the document without having to make a
	 * copy of it.
	 */
	public char[] text;
	public int textOffset;
	public int textCount;
	
	/**
	 * The offset into the document at which this token resides.
	 */
	public int offset;

	/**
	 * The type of token this is; for example, {@link #FUNCTION}.
	 */
	public int type;

	/**
	 * Whether this token is a hyperlink.
	 */
	private boolean hyperlink;

	/**
	 * The next token in this linked list.
	 */
	private Token nextToken;

	/**
	 * The language this token is in, <code>&gt;= 0</code>.
	 */
	private int languageIndex;

	/**
	 * Rectangle used for filling token backgrounds.
	 */
	private Rectangle2D.Float bgRect;

	/**
	 * Micro-optimization; buffer used to compute tab width.  If the width is
	 * correct it's not re-allocated, to prevent lots of very small garbage.
	 * Only used when painting tab lines.
	 */
	private static char[] tabBuf;


	/**
	 * Creates a "null token."  The token itself is not null; rather, it
	 * signifies that it is the last token in a linked list of tokens and
	 * that it is not part of a "multi-line token."
	 */
	public Token() {
		this.text = null;
		this.textOffset = -1;
		this.textCount = -1;
		this.type = NULL;
		offset = -1;
		hyperlink = false;
		nextToken = null;
		bgRect = new Rectangle2D.Float();
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
	public Token(final char[] line, final int beg, final int end,
							final int startOffset, final int type) {
		this();
		set(line, beg,end, startOffset, type);
	}


	/**
	 * Creates this token as a deep copy of the passed-in token.
	 *
	 * @param t2 The token from which to make a copy.
	 */
	public Token(Token t2) {
		this();
		copyFrom(t2);
	}


	/**
	 * Appends HTML code for painting this token, using the given text area's
	 * color scheme.
	 *
	 * @param sb The buffer to append to.
	 * @param textArea The text area whose color scheme to use.
	 * @param fontFamily Whether to include the font family in the HTML for
	 *        this token.  You can pass <code>false</code> for this parameter
	 *        if, for example, you are making all your HTML be monospaced,
	 *        and don't want any crazy fonts being used in the editor to be
	 *        reflected in your HTML.
	 * @return The buffer appended to.
	 * @see #getHTMLRepresentation(RSyntaxTextArea)
	 */
	public StringBuffer appendHTMLRepresentation(StringBuffer sb,
											RSyntaxTextArea textArea,
											boolean fontFamily) {
		return appendHTMLRepresentation(sb, textArea, fontFamily, false);
	}


	/**
	 * Appends HTML code for painting this token, using the given text area's
	 * color scheme.
	 *
	 * @param sb The buffer to append to.
	 * @param textArea The text area whose color scheme to use.
	 * @param fontFamily Whether to include the font family in the HTML for
	 *        this token.  You can pass <code>false</code> for this parameter
	 *        if, for example, you are making all your HTML be monospaced,
	 *        and don't want any crazy fonts being used in the editor to be
	 *        reflected in your HTML.
	 * @param tabsToSpaces Whether to convert tabs into spaces.
	 * @return The buffer appended to.
	 * @see #getHTMLRepresentation(RSyntaxTextArea)
	 */
	public StringBuffer appendHTMLRepresentation(StringBuffer sb,
								RSyntaxTextArea textArea, boolean fontFamily,
								boolean tabsToSpaces) {

		SyntaxScheme colorScheme = textArea.getSyntaxScheme();
		Style scheme = colorScheme.getStyle(type);
		Font font = textArea.getFontForTokenType(type);//scheme.font;

		if (font.isBold()) sb.append("<b>");
		if (font.isItalic()) sb.append("<em>");
		if (scheme.underline || isHyperlink()) sb.append("<u>");

		sb.append("<font");
		if (fontFamily) {
			sb.append(" face=\"").append(font.getFamily()).append("\"");
		}
		sb.append(" color=\"").
			append(getHTMLFormatForColor(scheme.foreground)).
			append("\">");

		// NOTE: Don't use getLexeme().trim() because whitespace tokens will
		// be turned into NOTHING.
		appendHtmlLexeme(textArea, sb, tabsToSpaces);

		sb.append("</font>");
		if (scheme.underline || isHyperlink()) sb.append("</u>");
		if (font.isItalic()) sb.append("</em>");
		if (font.isBold()) sb.append("</b>");

		return sb;

	}


	/**
	 * Appends an HTML version of the lexeme of this token (i.e. no style
	 * HTML, but replacing chars such as <code>\t</code>, <code>&lt;</code>
	 * and <code>&gt;</code> with their escapes).
	 *
	 * @param textArea The text area.
	 * @param sb The buffer to append to.
	 * @param tabsToSpaces Whether to convert tabs into spaces.
	 * @return The same buffer.
	 */
	private final StringBuffer appendHtmlLexeme(RSyntaxTextArea textArea,
								StringBuffer sb, boolean tabsToSpaces) {

		boolean lastWasSpace = false;
		int i = textOffset;
		int lastI = i;
		String tabStr = null;

		while (i<textOffset+textCount) {
			char ch = text[i];
			switch (ch) {
				case ' ':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append(lastWasSpace ? "&nbsp;" : " ");
					lastWasSpace = true;
					break;
				case '\t':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					if (tabsToSpaces && tabStr==null) {
						tabStr = "";
						for (int j=0; j<textArea.getTabSize(); j++) {
							tabStr += "&nbsp;";
						}
					}
					sb.append(tabsToSpaces ? tabStr : "&#09;");
					lastWasSpace = false;
					break;
				case '<':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append("&lt;");
					lastWasSpace = false;
					break;
				case '>':
					sb.append(text, lastI, i-lastI);
					lastI = i+1;
					sb.append("&gt;");
					lastWasSpace = false;
					break;
				default:
					lastWasSpace = false;
					break;
			}
			i++;
		}
		if (lastI<textOffset+textCount) {
			sb.append(text, lastI, textOffset+textCount-lastI);
		}
		return sb;
	}


	/**
	 * Returns whether the token straddles the specified position in the
	 * document.
	 *
	 * @param pos The position in the document to check.
	 * @return Whether the specified position is straddled by this token.
	 */
	public boolean containsPosition(int pos) {
		return pos>=offset && pos<offset+textCount;
	}


	/**
	 * Makes one token point to the same text segment, and have the same value
	 * as another token.
	 *
	 * @param t2 The token from which to copy.
	 */
	public void copyFrom(Token t2) {
		text = t2.text;
		textOffset = t2.textOffset;
		textCount = t2.textCount;
		offset = t2.offset;
		type = t2.type;
		languageIndex = t2.languageIndex;
		nextToken = t2.nextToken;
	}


	/**
	 * Returns the position in the token's internal char array corresponding
	 * to the specified document position.<p>
	 * Note that this method does NOT do any bounds checking; you can pass in
	 * a document position that does not correspond to a position in this
	 * token, and you will not receive an Exception or any other notification;
	 * it is up to the caller to ensure valid input.
	 *
	 * @param pos A position in the document that is represented by this token.
	 * @return The corresponding token position >= <code>textOffset</code> and
	 *         < <code>textOffset+textCount</code>.
	 * @see #tokenToDocument
	 */
	public int documentToToken(int pos) {
		return pos + (textOffset-offset);
	}


	/**
	 * Returns whether this token's lexeme ends with the specified characters.
	 *
	 * @param ch The characters.
	 * @return Whether this token's lexeme ends with the specified characters.
	 * @see #startsWith(char[])
	 */
	public boolean endsWith(char[] ch) {
		if (ch==null || ch.length>textCount) {
			return false;
		}
		final int start = textOffset + textCount - ch.length;
		for (int i=0; i<ch.length; i++) {
			if (text[start+i]!=ch[i]) {
				return false;
			}
		}
		return true;
	}


	/**
	 * Returns a <code>String</code> of the form "#xxxxxx" good for use
	 * in HTML, representing the given color.
	 *
	 * @param color The color to get a string for.
	 * @return The HTML form of the color.  If <code>color</code> is
	 *         <code>null</code>, <code>#000000</code> is returned.
	 */
	private static final String getHTMLFormatForColor(Color color) {
		if (color==null) {
			return "black";
		}
		String hexRed = Integer.toHexString(color.getRed());
		if (hexRed.length()==1)
			hexRed = "0" + hexRed;
		String hexGreen = Integer.toHexString(color.getGreen());
		if (hexGreen.length()==1)
			hexGreen = "0" + hexGreen;
		String hexBlue = Integer.toHexString(color.getBlue());
		if (hexBlue.length()==1)
			hexBlue = "0" + hexBlue;
		return "#" + hexRed + hexGreen + hexBlue;
	}


	/**
	 * Returns a <code>String</code> containing HTML code for painting this
	 * token, using the given text area's color scheme.
	 *
	 * @param textArea The text area whose color scheme to use.
	 * @return The HTML representation of the token.
	 * @see #appendHTMLRepresentation(StringBuffer, RSyntaxTextArea, boolean)
	 */
	public String getHTMLRepresentation(RSyntaxTextArea textArea) {
		StringBuffer buf = new StringBuffer();
		appendHTMLRepresentation(buf, textArea, true);
		return buf.toString();
	}


	/**
	 * Returns the language index of this token.
	 *
	 * @return The language index.  A value of <code>0</code> denotes the
	 *        "main" language, any positive value denotes a specific secondary
	 *        language.
	 * @see #setLanguageIndex(int)
	 */
	public int getLanguageIndex() {
		return languageIndex;
	}


	/**
	 * Returns the last token in this list that is not whitespace or a
	 * comment.
	 *
	 * @return The last non-comment, non-whitespace token, or <code>null</code>
	 *         if there isn't one.
	 */
	public Token getLastNonCommentNonWhitespaceToken() {

		Token last = null;

		for (Token t=this; t!=null && t.isPaintable(); t=t.nextToken) {
			switch (t.type) {
				case COMMENT_DOCUMENTATION:
				case COMMENT_EOL:
				case COMMENT_MULTILINE:
				case WHITESPACE:
					break;
				default:
					last = t;
					break;
			}
		}

		return last;

	}


	/**
	 * Returns the last paintable token in this token list, or <code>null</code>
	 * if there is no paintable token.
	 *
	 * @return The last paintable token in this token list.
	 */
	public Token getLastPaintableToken() {
		Token t = this;
		while (t.isPaintable()) {
			if (t.nextToken==null || !t.nextToken.isPaintable()) {
				return t;
			}
			t = t.nextToken;
		}
		return null;
	}


	/**
	 * Returns the text of this token, as a string.<p>
	 *
	 * Note that this method isn't used much by the
	 * <code>rsyntaxtextarea</code> package internally, as it tries to limit
	 * memory allocation.
	 *
	 * @return The text of this token.
	 */
	public String getLexeme() {
		return new String(text, textOffset, textCount);
	}


	/**
	 * Determines the offset into this token list (i.e., into the
	 * document) that covers pixel location <code>x</code> if the token list
	 * starts at pixel location <code>x0</code><p>.
	 * This method will return the document position "closest" to the
	 * x-coordinate (i.e., if they click on the "right-half" of the
	 * <code>w</code> in <code>awe</code>, the caret will be placed in
	 * between the <code>w</code> and <code>e</code>; similarly, clicking on
	 * the left-half places the caret between the <code>a</code> and
	 * <code>w</code>).  This makes it useful for methods such as
	 * <code>viewToModel</code> found in <code>javax.swing.text.View</code>
	 * subclasses.<p>
	 *
	 * This method is abstract so subclasses who paint themselves differently
	 * (i.e., {@link VisibleWhitespaceToken} is painted a tad differently than
	 * {@link DefaultToken} when rendering hints are enabled) can still return
	 * accurate results.
	 *
	 * @param textArea The text area from which the token list was derived.
	 * @param e How to expand tabs.
	 * @param x0 The pixel x-location that is the beginning of
	 *        <code>tokenList</code>.
	 * @param x The pixel-position for which you want to get the corresponding
	 *        offset.
	 * @return The position (in the document, NOT into the token list!) that
	 *         covers the pixel location.  If <code>tokenList</code> is
	 *         <code>null</code> or has type <code>Token.NULL</code>, then
	 *         <code>-1</code is returned; the caller should recognize this and
	 *         return the actual end position of the (empty) line.
	 */
	public abstract int getListOffset(RSyntaxTextArea textArea, TabExpander e,
								float x0, float x);


	/**
	 * Returns the token after this one in the linked list.
	 *
	 * @return The next token.
	 * @see #setNextToken
	 */
	public Token getNextToken() {
		return nextToken;
	}


	/**
	 * Returns the position in the document that represents the last character
	 * in the token that will fit into <code>endBeforeX-startX</code> pixels.
	 * For example, if you're using a monospaced 8-pixel-per-character font,
	 * have the token "while" and <code>startX</code> is <code>0</code> and
	 * <code>endBeforeX</code> is <code>30</code>, this method will return the
	 * document position of the "i" in "while", because the "i" ends at pixel
	 * <code>24</code>, while the "l" ends at <code>32</code>.  If not even the
	 * first character fits in <code>endBeforeX-startX</code>, the first
	 * character's position is still returned so calling methods don't go into
	 * infinite loops.
	 *
	 * @param textArea The text area in which this token is being painted.
	 * @param e How to expand tabs.
	 * @param startX The x-coordinate at which the token will be painted.  This
	 *        is needed because of tabs.
	 * @param endBeforeX The x-coordinate for which you want to find the last
	 *        character of <code>t</code> which comes before it.
	 * @return The last document position that will fit in the specified amount
	 *         of pixels.
	 */
	/*
	 * @see #getTokenListOffsetBeforeX
	 * FIXME:  This method does not compute correctly!  It needs to be abstract
	 * and implemented by subclasses.
	 */
	public int getOffsetBeforeX(RSyntaxTextArea textArea, TabExpander e,
							float startX, float endBeforeX) {

		FontMetrics fm = textArea.getFontMetricsForTokenType(type);
		int i = textOffset;
		int stop = i + textCount;
		float x = startX;

		while (i<stop) {
			if (text[i]=='\t')
				x = e.nextTabStop(x, 0);
			else
				x += fm.charWidth(text[i]);
			if (x>endBeforeX) {
				// If not even the first character fits into the space, go
				// ahead and say the first char does fit so we don't go into
				// an infinite loop.
				int intoToken = Math.max(i-textOffset, 1);
				return offset + intoToken;
			}
			i++;
		}

		// If we got here, the whole token fit in (endBeforeX-startX) pixels.
		return offset + textCount - 1;

	}


	/**
	 * Returns the width of this token given the specified parameters.
	 *
	 * @param textArea The text area in which the token is being painted.
	 * @param e Describes how to expand tabs.  This parameter cannot be
	 *        <code>null</code>.
	 * @param x0 The pixel-location at which the token begins.  This is needed
	 *        because of tabs.
	 * @return The width of the token, in pixels.
	 * @see #getWidthUpTo
	 */
	public float getWidth(RSyntaxTextArea textArea, TabExpander e, float x0) {
		return getWidthUpTo(textCount, textArea, e, x0);
	}


	/**
	 * Returns the width of a specified number of characters in this token.
	 * For example, for the token "while", specifying a value of <code>3</code>
	 * here returns the width of the "whi" portion of the token.<p>
	 *
	 * This method is abstract so subclasses who paint themselves differently
	 * (i.e., {@link VisibleWhitespaceToken} is painted a tad differently than
	 * {@link DefaultToken} when rendering hints are enabled) can still return
	 * accurate results.
	 *
	 * @param numChars The number of characters for which to get the width.
	 * @param textArea The text area in which the token is being painted.
	 * @param e How to expand tabs.  This value cannot be <code>null</code>.
	 * @param x0 The pixel-location at which this token begins.  This is needed
	 *        because of tabs.
	 * @return The width of the specified number of characters in this token.
	 * @see #getWidth
	 */
	public abstract float getWidthUpTo(int numChars, RSyntaxTextArea textArea,
										TabExpander e, float x0);


	/**
	 * Returns whether this token is of the specified type, with the specified
	 * lexeme.<p>
	 * This method is preferred over the other overload in performance-critical
	 * code where this operation may be called frequently, since it does not
	 * involve any String allocations.
	 *
	 * @param type The type to check for.
	 * @param lexeme The lexeme to check for.
	 * @return Whether this token has that type and lexeme.
	 * @see #is(int, String)
	 * @see #isSingleChar(int, char)
	 * @see #startsWith(char[])
	 */
	public boolean is(int type, char[] lexeme) {
		if (this.type==type && textCount==lexeme.length) {
			for (int i=0; i<textCount; i++) {
				if (text[textOffset+i]!=lexeme[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


	/**
	 * Returns whether this token is of the specified type, with the specified
	 * lexeme.<p>
	 * The other overload of this method is preferred over this one in
	 * performance-critical code, as this one involves a String allocation
	 * while the other does not.
	 *
	 * @param type The type to check for.
	 * @param lexeme The lexeme to check for.
	 * @return Whether this token has that type and lexeme.
	 * @see #is(int, char[])
	 * @see #isSingleChar(int, char)
	 * @see #startsWith(char[])
	 */
	public boolean is(int type, String lexeme) {
		return this.type==type && textCount==lexeme.length() &&
				lexeme.equals(getLexeme());
	}


	/**
	 * Returns whether this token is a comment.
	 *
	 * @return Whether this token is a comment.
	 * @see #isWhitespace()
	 */
	public boolean isComment() {
		return type>=Token.COMMENT_EOL && type<=Token.COMMENT_MARKUP;
	}


	/**
	 * Returns whether this token is a hyperlink.
	 *
	 * @return Whether this token is a hyperlink.
	 * @see #setHyperlink(boolean)
	 */
	public boolean isHyperlink() {
		return hyperlink;
	}


	/**
	 * Returns whether this token is an identifier.
	 *
	 * @return Whether this token is an identifier.
	 */
	public boolean isIdentifier() {
		return type==IDENTIFIER;
	}


	/**
	 * Returns whether this token is a {@link #SEPARATOR} representing a single
	 * left curly brace.
	 *
	 * @return Whether this token is a left curly brace.
	 * @see #isRightCurly()
	 */
	public boolean isLeftCurly() {
		return type==SEPARATOR && isSingleChar('{');
	}


	/**
	 * Returns whether this token is a {@link #SEPARATOR} representing a single
	 * right curly brace.
	 *
	 * @return Whether this token is a right curly brace.
	 * @see #isLeftCurly()
	 */
	public boolean isRightCurly() {
		return type==SEPARATOR && isSingleChar('}');
	}


	/**
	 * Returns whether or not this token is "paintable;" i.e., whether or not
	 * the type of this token is one such that it has an associated syntax
	 * style.  What this boils down to is whether the token type is greater
	 * than <code>Token.NULL</code>.
	 *
	 * @return Whether or not this token is paintable.
	 */
	public boolean isPaintable() {
		return type>Token.NULL;
	}


	/**
	 * Returns whether this token is the specified single character.
	 *
	 * @param ch The character to check for.
	 * @return Whether this token's lexeme is the single character specified.
	 * @see #isSingleChar(int, char)
	 */
	public boolean isSingleChar(char ch) {
		return textCount==1 && text[textOffset]==ch;
	}


	/**
	 * Returns whether this token is the specified single character, and of a
	 * specific type.
	 *
	 * @param type The token type.
	 * @param ch The character to check for.
	 * @return Whether this token is of the specified type, and with a lexeme
	 *         Equaling the single character specified.
	 * @see #isSingleChar(char)
	 */
	public boolean isSingleChar(int type, char ch) {
		return this.type==type && isSingleChar(ch);
	}


	/**
	 * Returns whether or not this token is whitespace.
	 *
	 * @return <code>true</code> iff this token is whitespace.
	 * @see #isComment()
	 */
	public boolean isWhitespace() {
		return type==WHITESPACE;
	}


	/**
	 * Returns the bounding box for the specified document location.  The
	 * location must be in the specified token list; if it isn't,
	 * <code>null</code> is returned.
	 *
	 * @param textArea The text area from which the token list was derived.
	 * @param e How to expand tabs.
	 * @param pos The position in the document for which to get the bounding
	 *        box in the view.
	 * @param x0 The pixel x-location that is the beginning of
	 *        <code>tokenList</code>.
	 * @param rect The rectangle in which we'll be returning the results.  This
	 *        object is reused to keep from frequent memory allocations.
	 * @return The bounding box for the specified position in the model.
	 */
	public abstract Rectangle listOffsetToView(RSyntaxTextArea textArea,
								TabExpander e, int pos, int x0,
								Rectangle rect);


	/**
	 * Makes this token start at the specified offset into the document.
	 *
	 * @param pos The offset into the document this token should start at.
	 *        Note that this token must already contain this position; if
	 *        it doesn't, an exception is thrown.
	 * @throws IllegalArgumentException If pos is not already contained by
	 *         this token.
	 * @see #moveOffset(int)
	 */
	public void makeStartAt(int pos) {
		if (pos<offset || pos>=(offset+textCount)) {
			throw new IllegalArgumentException("pos " + pos +
				" is not in range " + offset + "-" + (offset+textCount-1));
		}
		int shift = pos - offset;
		offset = pos;
		textOffset += shift;
		textCount -= shift;
	}


	/**
	 * Moves the starting offset of this token.
	 *
	 * @param amt The amount to move the starting offset.  This should be
	 *        between <code>0</code> and <code>textCount</code>, inclusive.
	 * @throws IllegalArgumentException If <code>amt</code> is an invalid value.
	 * @see #makeStartAt(int)
	 */
	public void moveOffset(int amt) {
		if (amt<0 || amt>textCount) {
			throw new IllegalArgumentException("amt " + amt +
					" is not in range 0-" + textCount);
		}
		offset += amt;
		textOffset += amt;
		textCount -= amt;
	}


	/**
	 * Paints this token.
	 *
	 * @param g The graphics context in which to paint.
	 * @param x The x-coordinate at which to paint.
	 * @param y The y-coordinate at which to paint.
	 * @param host The text area this token is in.
	 * @param e How to expand tabs.
	 * @return The x-coordinate representing the end of the painted text.
	 */
	public final float paint(Graphics2D g, float x, float y,
						RSyntaxTextArea host, TabExpander e) {
		return paint(g, x,y, host, e, 0);
	}


	/**
	 * Paints this token.
	 *
	 * @param g The graphics context in which to paint.
	 * @param x The x-coordinate at which to paint.
	 * @param y The y-coordinate at which to paint.
	 * @param host The text area this token is in.
	 * @param e How to expand tabs.
	 * @param clipStart The left boundary of the clip rectangle in which we're
	 *        painting.  This optimizes painting by allowing us to not paint
	 *        paint when this token is "to the left" of the clip rectangle.
	 * @return The x-coordinate representing the end of the painted text.
	 */
	public abstract float paint(Graphics2D g, float x, float y,
							RSyntaxTextArea host,
							TabExpander e, float clipStart);


	/**
	 * Paints the background of a token.
	 *
	 * @param x The x-coordinate of the token.
	 * @param y The y-coordinate of the token.
	 * @param width The width of the token (actually, the width of the part of
	 *        the token to paint).
	 * @param height The height of the token.
	 * @param g The graphics context with which to paint.
	 * @param fontAscent The ascent of the token's font.
	 * @param host The text area.
	 * @param color The color with which to paint.
	 */
	protected void paintBackground(float x, float y, float width, float height,
							Graphics2D g, int fontAscent,
							RSyntaxTextArea host, Color color) {
		// RSyntaxTextArea's bg can be null, so we must check for this.
		Color temp = host.getBackground();
		g.setXORMode(temp!=null ? temp : Color.WHITE);
		g.setColor(color);
		bgRect.setRect(x,y-fontAscent, width,height);
		//g.fill(bgRect);
		g.fillRect((int)x, (int)(y-fontAscent), (int)width, (int)height);
		g.setPaintMode();
	}


	/**
	 * Paints dotted "tab" lines; that is, lines that show where your caret
	 * would go to on the line if you hit "tab".  This visual effect is usually
	 * done in the leading whitespace token(s) of lines.
	 *
	 * @param x The starting x-offset of this token.  It is assumed that this
	 *        is the left margin of the text area (may be non-zero due to
	 *        insets), since tab lines are only painted for leading whitespace.
	 * @param y The baseline where this token was painted.
	 * @param endX The ending x-offset of this token.
	 * @param g The graphics context.
	 * @param e Used to expand tabs.
	 * @param host The text area.
	 */
	protected void paintTabLines(int x, int y, int endX, Graphics2D g,
								TabExpander e, RSyntaxTextArea host) {

		// We allow tab lines to be painted in more than just Token.WHITESPACE,
		// i.e. for MLC's and Token.IDENTIFIERS (for TokenMakers that return
		// whitespace as identifiers for performance).  But we only paint tab
		// lines for the leading whitespace in the token.  So, if this isn't a
		// WHITESPACE token, figure out the leading whitespace's length.
		if (type!=Token.WHITESPACE) {
			int offs = textOffset;
			for (; offs<textOffset+textCount; offs++) {
				if (!RSyntaxUtilities.isWhitespace(text[offs])) {
					break; // MLC text, etc.
				}
			}
			int len = offs - textOffset;
			if (len<2) { // Must be at least two whitespaces to see tab line
				return;
			}
			//endX = x + (int)getWidthUpTo(len, host, e, x);
			endX = (int)getWidthUpTo(len, host, e, 0);
		}

		// Get the length of a tab.
		FontMetrics fm = host.getFontMetricsForTokenType(type);
		int tabSize = host.getTabSize();
		if (tabBuf==null || tabBuf.length<tabSize) {
			tabBuf = new char[tabSize];
			for (int i=0; i<tabSize; i++) {
				tabBuf[i] = ' ';
			}
		}
		// Note different token types (MLC's, whitespace) could possibly be
		// using different fonts, which means we can't cache the actual width
		// of a tab as it may be different per-token-type.  We could keep a
		// per-token-type cache, but we'd have to clear it whenever they
		// modified token styles.
		int tabW = fm.charsWidth(tabBuf, 0, tabSize);

		// Draw any tab lines.  Here we're assuming that "x" is the left
		// margin of the editor.
		g.setColor(host.getTabLineColor());
		int x0 = x + tabW;
		int y0 = y - fm.getAscent();
		if ((y0&1)>0) {
			// Only paint on even y-pixels to prevent doubling up between lines
			y0++;
		}
		while (x0<endX) {
			int y1 = y0;
			int y2 = y0 + host.getLineHeight();
			while (y1<y2) {
				g.drawLine(x0, y1, x0, y1);
				y1 += 2;
			}
			//g.drawLine(x0,y0, x0,y0+host.getLineHeight());
			x0 += tabW;
		}

	}


	/**
	 * Sets the value of this token to a particular segment of a document.
	 * The "next token" value is cleared.
	 *
	 * @param line The segment from which to get the token.
	 * @param beg The first character's position in <code>line</code>.
	 * @param end The last character's position in <code>line</code>.
	 * @param offset The offset into the document at which this token begins.
	 * @param type A token type listed as "generic" above.
	 */
	public void set(final char[] line, final int beg, final int end,
							final int offset, final int type) {
		this.text = line;
		this.textOffset = beg;
		this.textCount = end - beg + 1;
		this.type = type;
		this.offset = offset;
		nextToken = null;
	}


	/**
	 * Sets whether this token is a hyperlink.
	 *
	 * @param hyperlink Whether this token is a hyperlink.
	 * @see #isHyperlink()
	 */
	public void setHyperlink(boolean hyperlink) {
		this.hyperlink = hyperlink;
	}


	/**
	 * Sets the language index for this token.  If this value is positive, it
	 * denotes a specific "secondary" language this token represents (such as
	 * JavaScript code or CSS embedded in an HTML file).  If this value is
	 * <code>0</code>, this token is in the "main" language being edited.
	 * Negative values are invalid and treated as <code>0</code>.
	 *
	 * @param languageIndex The new language index.  A value of
	 *        <code>0</code> denotes the "main" language, any positive value
	 *        denotes a specific secondary language.  Negative values will
	 *        be treated as <code>0</code>.
	 * @see #getLanguageIndex()
	 */
	public void setLanguageIndex(int languageIndex) {
		this.languageIndex = languageIndex;
	}


	/**
	 * Sets the "next token" pointer of this token to point to the specified
	 * token.
	 *
	 * @param nextToken The new next token.
	 * @see #getNextToken()
	 */
	public void setNextToken(Token nextToken) {
		this.nextToken = nextToken;
	}


	/**
	 * Returns whether this token starts with the specified characters.
	 *
	 * @param chars The characters.
	 * @return Whether this token starts with those characters.
	 * @see #endsWith(char[])
	 * @see #is(int, char[])
	 */
	public boolean startsWith(char[] chars) {
		if (chars.length<=textCount){
			for (int i=0; i<chars.length; i++) {
				if (text[textOffset+i]!=chars[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}


	/**
	 * Returns the position in the document corresponding to the specified
	 * position in this token's internal char array (<code>textOffset</code> -
	 * <code>textOffset+textCount-1</code>).<p>
	 * Note that this method does NOT do any bounds checking; you can pass in
	 * an invalid token position, and you will not receive an Exception or any
	 * other indication that the returned document position is invalid.  It is
	 * up to the user to ensure valid input.
	 *
	 * @param pos A position in the token's internal char array
	 *        (<code>textOffset</code> - <code>textOffset+textCount</code>).
	 * @return The corresponding position in the document.
	 * @see #documentToToken(int)
	 */
	public int tokenToDocument(int pos) {
		return pos + (offset-textOffset);
	}


	/**
	 * Returns this token as a <code>String</code>, which is useful for
	 * debugging.
	 *
	 * @return A string describing this token.
	 */
	public String toString() {
		return "[Token: " +
			(type==Token.NULL ? "<null token>" :
				"text: '" +
					(text==null ? "<null>" : getLexeme() + "'; " +
	       		"offset: " + offset + "; type: " + type + "; " +
		   		"isPaintable: " + isPaintable() +
		   		"; nextToken==null: " + (nextToken==null))) +
		   "]";
	}


}