/*
 * 10/16/2004
 *
 * RSyntaxDocument.java - A document capable of syntax highlighting, used by
 * RSyntaxTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.Action;
import javax.swing.event.*;
import javax.swing.text.*;

import org.fife.ui.rsyntaxtextarea.modes.AbstractMarkupTokenMaker;
import org.fife.ui.rtextarea.RDocument;
import org.fife.util.DynamicIntArray;


/**
 * The document used by {@link org.fife.ui.rsyntaxtextarea.RSyntaxTextArea}.
 * This document is like <code>javax.swing.text.PlainDocument</code> except that
 * it also keeps track of syntax highlighting in the document.  It has a "style"
 * attribute associated with it that determines how syntax highlighting is done
 * (i.e., what language is being highlighted).<p>
 *
 * Instances of <code>RSyntaxTextArea</code> will only accept instances of
 * <code>RSyntaxDocument</code>, since it is this document that keeps
 * track of syntax highlighting.  All others will cause an exception to be
 * thrown.<p>
 *
 * To change the language being syntax highlighted at any time, you merely have
 * to call {@link #setSyntaxStyle}.  Other than that, this document can be
 * treated like any other save one caveat:  all <code>DocumentEvent</code>s of
 * type <code>CHANGE</code> use their offset and length values to represent the
 * first and last lines, respectively, that have had their syntax coloring
 * change.  This is really a hack to increase the speed of the painting code
 * and should really be corrected, but oh well. 
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class RSyntaxDocument extends RDocument implements SyntaxConstants {

	/**
	 * Creates a {@link TokenMaker} appropriate for a given programming
	 * language.
	 */
	private transient TokenMakerFactory tokenMakerFactory;

	/**
	 * Splits text into tokens for the current programming language.
	 */
	private transient TokenMaker tokenMaker;

	/**
	 * The current syntax style.  Only cached to keep this class serializable.
	 */
	private String syntaxStyle;

	/**
	 * Array of values representing the "last token type" on each line.  This
	 * is used in cases such as multi-line comments:  if the previous line
	 * ended with an (unclosed) multi-line comment, we can use this knowledge
	 * and start the current line's syntax highlighting in multi-line comment
	 * state.
	 */
	protected transient DynamicIntArray lastTokensOnLines;

	private transient Segment s;


	/**
	 * Constructs a plain text document.  A default root element is created,
	 * and the tab size set to 5.
	 *
	 * @param syntaxStyle The syntax highlighting scheme to use.
	 */
	public RSyntaxDocument(String syntaxStyle) {
		this(null, syntaxStyle);
	}


	/**
	 * Constructs a plain text document.  A default root element is created,
	 * and the tab size set to 5.
	 *
	 * @param tmf The <code>TokenMakerFactory</code> for this document.  If
	 *        this is <code>null</code>, a default factory is used.
	 * @param syntaxStyle The syntax highlighting scheme to use.
	 */
	public RSyntaxDocument(TokenMakerFactory tmf, String syntaxStyle) {
		putProperty(tabSizeAttribute, new Integer(5));
		lastTokensOnLines = new DynamicIntArray(400);
		lastTokensOnLines.add(Token.NULL); // Initial (empty) line.
		s = new Segment();
		setTokenMakerFactory(tmf);
		setSyntaxStyle(syntaxStyle);
	}


	/**
	 * Alerts all listeners to this document of an insertion.  This is
	 * overridden so we can update our syntax highlighting stuff.<p>
	 * The syntax highlighting stuff has to be here instead of in
	 * <code>insertUpdate</code> because <code>insertUpdate</code> is not
	 * called by the undo/redo actions, but this method is.
	 *
	 * @param e The change.
	 */
	protected void fireInsertUpdate(DocumentEvent e) {

		/*
		 * Now that the text is actually inserted into the content and
		 * element structure, we can update our token elements and "last
		 * tokens on lines" structure.
		 */

		Element lineMap = getDefaultRootElement();
		DocumentEvent.ElementChange change = e.getChange(lineMap);
		Element[] added = change==null ? null : change.getChildrenAdded();

		int numLines = lineMap.getElementCount();
		int line = lineMap.getElementIndex(e.getOffset());
		int previousLine = line - 1;
		int previousTokenType = (previousLine>-1 ?
					lastTokensOnLines.get(previousLine) : Token.NULL);

		// If entire lines were added...
		if (added!=null && added.length>0) {

			Element[] removed = change.getChildrenRemoved();
			int numRemoved = removed!=null ? removed.length : 0;

			int endBefore = line + added.length - numRemoved;
			//System.err.println("... adding lines: " + line + " - " + (endBefore-1));
			//System.err.println("... ... added: " + added.length + ", removed:" + numRemoved);
			for (int i=line; i<endBefore; i++) {

				setSharedSegment(i); // Loads line i's text into s.

				int tokenType = tokenMaker.getLastTokenTypeOnLine(s, previousTokenType);
				lastTokensOnLines.add(i, tokenType);
				//System.err.println("--------- lastTokensOnLines.size() == " + lastTokensOnLines.getSize());

				previousTokenType = tokenType;

			} // End of for (int i=line; i<endBefore; i++).

			// Update last tokens for lines below until they stop changing.
			updateLastTokensBelow(endBefore, numLines, previousTokenType);

		} // End of if (added!=null && added.length>0).

		// Otherwise, text was inserted on a single line...
		else {

			// Update last tokens for lines below until they stop changing.
			updateLastTokensBelow(line, numLines, previousTokenType);

		} // End of else.

		// Let all listeners know about the insertion.
		super.fireInsertUpdate(e);

	}


	/**
	 * This method is called AFTER the content has been inserted into the
	 * document and the element structure has been updated.<p>
	 * The syntax-highlighting updates need to be done here (as opposed to
	 * an override of <code>postRemoveUpdate</code>) as this method is called
	 * in response to undo/redo events, whereas <code>postRemoveUpdate</code>
	 * is not.<p>
	 * Now that the text is actually inserted into the content and element
	 * structure, we can update our token elements and "last tokens on
	 * lines" structure.
	 *
	 * @param chng The change that occurred.
	 * @see #removeUpdate
	 */
	protected void fireRemoveUpdate(DocumentEvent chng) {

		Element lineMap = getDefaultRootElement();
		int numLines = lineMap.getElementCount();

		DocumentEvent.ElementChange change = chng.getChange(lineMap);
		Element[] removed = change==null ? null : change.getChildrenRemoved();

		// If entire lines were removed...
		if (removed!=null && removed.length>0) {

			int line = change.getIndex();	// First line entirely removed.
			int previousLine = line - 1;	// Line before that.
			int previousTokenType = (previousLine>-1 ?
					lastTokensOnLines.get(previousLine) : Token.NULL);

			Element[] added = change.getChildrenAdded();
			int numAdded = added==null ? 0 : added.length;

			// Remove the cached last-token values for the removed lines.
			int endBefore = line + removed.length - numAdded;
			//System.err.println("... removing lines: " + line + " - " + (endBefore-1));
			//System.err.println("... added: " + numAdded + ", removed: " + removed.length);

			lastTokensOnLines.removeRange(line, endBefore); // Removing values for lines [line-(endBefore-1)].
			//System.err.println("--------- lastTokensOnLines.size() == " + lastTokensOnLines.getSize());

			// Update last tokens for lines below until they've stopped changing.
			updateLastTokensBelow(line, numLines, previousTokenType);

		} // End of if (removed!=null && removed.size()>0).

		// Otherwise, text was removed from just one line...
		else {

			int line = lineMap.getElementIndex(chng.getOffset());
			if (line>=lastTokensOnLines.getSize())
				return;	// If we're editing the last line in a document...

			int previousLine = line - 1;
			int previousTokenType = (previousLine>-1 ?
					lastTokensOnLines.get(previousLine) : Token.NULL);
			//System.err.println("previousTokenType for line : " + previousLine + " is " + previousTokenType);
			// Update last tokens for lines below until they've stopped changing.
			updateLastTokensBelow(line, numLines, previousTokenType);

		}

		// Let all of our listeners know about the removal.
		super.fireRemoveUpdate(chng);

	}


	/**
	 * Returns whether closing markup tags should be automatically completed.
	 * This method only returns <code>true</code> if
	 * {@link #getLanguageIsMarkup()} also returns <code>true</code>.
	 *
	 * @return Whether markup closing tags should be automatically completed.
	 * @see #getLanguageIsMarkup()
	 */
	public boolean getCompleteMarkupCloseTags() {
		// TODO: Remove terrible dependency on AbstractMarkupTokenMaker
		return getLanguageIsMarkup() &&
				((AbstractMarkupTokenMaker)tokenMaker).getCompleteCloseTags();
	}


	/**
	 * Returns whether the current programming language uses curly braces
	 * ('<tt>{</tt>' and '<tt>}</tt>') to denote code blocks.
	 *
	 * @return Whether curly braces denote code blocks.
	 */
	public boolean getCurlyBracesDenoteCodeBlocks() {
		return tokenMaker.getCurlyBracesDenoteCodeBlocks();
	}


	/**
	 * Returns whether the current language is a markup language, such as
	 * HTML, XML or PHP.
	 *
	 * @return Whether the current language is a markup language.
	 */
	public boolean getLanguageIsMarkup() {
		return tokenMaker.isMarkupLanguage();
	}


	/**
	 * Returns the token type of the last token on the given line.
	 *
	 * @param line The line to inspect.
	 * @return The token type of the last token on the specified line.  If
	 *         the line is invalid, an exception is thrown.
	 */
	public int getLastTokenTypeOnLine(int line) {
		return lastTokensOnLines.get(line);
	}


	/**
	 * Returns the text to place at the beginning and end of a
	 * line to "comment" it in the current programming language.
	 *
	 * @return The start and end strings to add to a line to "comment"
	 *         it out.  A <code>null</code> value for either means there
	 *         is no string to add for that part.  A value of
	 *         <code>null</code> for the array means this language
	 *         does not support commenting/uncommenting lines.
	 */
	public String[] getLineCommentStartAndEnd() {
		return tokenMaker.getLineCommentStartAndEnd();
	}


	/**
	 * Returns whether tokens of the specified type should have "mark
	 * occurrences" enabled for the current programming language.
	 *
	 * @param type The token type.
	 * @return Whether tokens of this type should have "mark occurrences"
	 *         enabled.
	 */
	boolean getMarkOccurrencesOfTokenType(int type) {
		return tokenMaker.getMarkOccurrencesOfTokenType(type);
	}


	/**
	 * This method returns whether auto indentation should be done if Enter
	 * is pressed at the end of the specified line.
	 *
	 * @param line The line to check.
	 * @return Whether an extra indentation should be done.
	 */
	public boolean getShouldIndentNextLine(int line) {
		Token t = getTokenListForLine(line);
		t = t.getLastNonCommentNonWhitespaceToken();
		return tokenMaker.getShouldIndentNextLineAfter(t);
	}


	/**
	 * Returns a token list for the specified segment of text representing
	 * the specified line number.  This method is basically a wrapper for
	 * <code>tokenMaker.getTokenList</code> that takes into account the last
	 * token on the previous line to assure token accuracy.
	 *
	 * @param line The line number of <code>text</code> in the document, >= 0.
	 * @return A token list representing the specified line.
	 */
	public final Token getTokenListForLine(int line) {
		Element map = getDefaultRootElement();
		Element elem = map.getElement(line);
		int startOffset = elem.getStartOffset();
		//int endOffset = (line==map.getElementCount()-1 ? elem.getEndOffset() - 1:
		//									elem.getEndOffset() - 1);
		int endOffset = elem.getEndOffset() - 1; // Why always "-1"?
		try {
			getText(startOffset,endOffset-startOffset, s);
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			return null;
		}
		int initialTokenType = line==0 ? Token.NULL :
								getLastTokenTypeOnLine(line-1);
		return tokenMaker.getTokenList(s, initialTokenType, startOffset);
	}


	boolean insertBreakSpecialHandling(ActionEvent e) {
		Action a = tokenMaker.getInsertBreakAction();
		if (a!=null) {
			a.actionPerformed(e);
			return true;
		}
		return false;
	}


	/**
	 * Returns whether whitespace is visible.
	 *
	 * @return Whether whitespace is visible.
	 * @see #setWhitespaceVisible(boolean)
	 */
	public boolean isWhitespaceVisible() {
		return tokenMaker==null ? false : tokenMaker.isWhitespaceVisible();
	}


	/**
	 * Deserializes a document.
	 *
	 * @param in The stream to read from.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream in)
						throws ClassNotFoundException, IOException {

		in.defaultReadObject();

		// Install default TokenMakerFactory.  To support custom TokenMakers,
		// both JVM's should install default TokenMakerFactories that support
		// the language they want to use beforehand.
		setTokenMakerFactory(null);

		// Handle other transient stuff
		this.s = new Segment();
		int lineCount = getDefaultRootElement().getElementCount();
		lastTokensOnLines = new DynamicIntArray(lineCount);
		setSyntaxStyle(syntaxStyle); // Actually install (transient) TokenMaker
		setWhitespaceVisible(in.readBoolean()); // Do after setSyntaxStyle()

	}


	/**
	 * Makes our private <code>Segment s</code> point to the text in our
	 * document referenced by the specified element.  Note that
	 * <code>line</code> MUST be a valid line number in the document.
	 *
	 * @param line The line number you want to get.
	 */
	private final void setSharedSegment(int line) {

		Element map = getDefaultRootElement();
		//int numLines = map.getElementCount();

		Element element = map.getElement(line);
		if (element==null)
			throw new InternalError("Invalid line number: " + line);
		int startOffset = element.getStartOffset();
		//int endOffset = (line==numLines-1 ?
		//			element.getEndOffset()-1 : element.getEndOffset() - 1);
		int endOffset = element.getEndOffset()-1; // Why always "-1"?
		try {
			getText(startOffset, endOffset-startOffset, s);
		} catch (BadLocationException ble) {
			throw new InternalError("Text range not in document: " +
								startOffset + "-" + endOffset);
		}

	}


	/**
	 * Sets the syntax style being used for syntax highlighting in this
	 * document.  What styles are supported by a document is determined by its
	 * {@link TokenMakerFactory}.  By default, all <code>RSyntaxDocument</code>s
	 * support all languages built into <code>RSyntaxTextArea</code>. 
	 *
	 * @param styleKey The new style to use, such as
	 *        {@link SyntaxConstants#SYNTAX_STYLE_JAVA}.  If this style is not
	 *        known or supported by this document, then
	 *        {@link SyntaxConstants#SYNTAX_STYLE_NONE} is used.
	 */
	public void setSyntaxStyle(String styleKey) {
		boolean wsVisible = isWhitespaceVisible();
		tokenMaker = tokenMakerFactory.getTokenMaker(styleKey);
		tokenMaker.setWhitespaceVisible(wsVisible);
		updateSyntaxHighlightingInformation();
		this.syntaxStyle = styleKey;
	}


	/**
	 * Sets the syntax style being used for syntax highlighting in this
	 * document.  You should call this method if you've created a custom token
	 * maker for a language not normally supported by
	 * <code>RSyntaxTextArea</code>.
	 *
	 * @param tokenMaker The new token maker to use.
	 */
	public void setSyntaxStyle(TokenMaker tokenMaker) {
		tokenMaker.setWhitespaceVisible(isWhitespaceVisible());
		this.tokenMaker = tokenMaker;
		updateSyntaxHighlightingInformation();
	}


	/**
	 * Sets the token maker factory used by this document.
	 *
	 * @param tmf The <code>TokenMakerFactory</code> for this document.  If
	 *        this is <code>null</code>, a default factory is used.
	 */
	public void setTokenMakerFactory(TokenMakerFactory tmf) {
		tokenMakerFactory = tmf!=null ? tmf :
			TokenMakerFactory.getDefaultInstance();
	}


	/**
	 * Sets whether whitespace is visible.  This property is actually setting
	 * whether the tokens generated from this document "paint" something when
	 * they represent whitespace.
	 *
	 * @param visible Whether whitespace should be visible.
	 * @see #isWhitespaceVisible()
	 */
	public void setWhitespaceVisible(boolean visible) {
		tokenMaker.setWhitespaceVisible(visible);
	}


	/**
	 * Loops through the last-tokens-on-lines array from a specified point
	 * onward, updating last-token values until they stop changing.  This
	 * should be called when lines are updated/inserted/removed, as doing
	 * so may cause lines below to change color.
	 *
	 * @param line The first line to check for a change in last-token value.
	 * @param numLines The number of lines in the document.
	 * @param previousTokenType The last-token value of the line just before
	 *        <code>line</code>.
	 * @return The last line that needs repainting.
	 */
	private int updateLastTokensBelow(int line, int numLines, int previousTokenType) {

		int firstLine = line;

		// Loop through all lines past our starting point.  Update even the last
		// line's info, even though there aren't any lines after it that depend
		// on it changing for them to be changed, as its state may be used
		// elsewhere in the library.
		int end = numLines;
		//System.err.println("--- end==" + end + " (numLines==" + numLines + ")");
		while (line<end) {

			setSharedSegment(line); // Sets s's text to that of line 'line' in the document.

			int oldTokenType = lastTokensOnLines.get(line);
			int newTokenType = tokenMaker.getLastTokenTypeOnLine(s, previousTokenType);
			//System.err.println("---------------- line " + line + "; oldTokenType==" + oldTokenType + ", newTokenType==" + newTokenType + ", s=='" + s + "'");

			// If this line's end-token value didn't change, stop here.  Note
			// that we're saying this line needs repainting; this is because
			// the beginning of this line did indeed change color, but the
			// end didn't.
			if (oldTokenType==newTokenType) {
				//System.err.println("... ... ... repainting lines " + firstLine + "-" + line);
				fireChangedUpdate(new DefaultDocumentEvent(firstLine, line, DocumentEvent.EventType.CHANGE));
				return line;
			}

			// If the line's end-token value did change, update it and
			// keep going.
			// NOTE: "setUnsafe" is okay here as the bounds checking was
			// already done in lastTokensOnLines.get(line) above.
			lastTokensOnLines.setUnsafe(line, newTokenType);
			previousTokenType = newTokenType;
			line++;

		} // End of while (line<numLines).

		// If any lines had their token types changed, fire a changed update
		// for them.  The view will repaint the area covered by the lines.
		// FIXME:  We currently cheat and send the line range that needs to be
		// repainted as the "offset and length" of the change, since this is
		// what the view needs.  We really should send the actual offset and
		// length.
		if (line>firstLine) {
			//System.err.println("... ... ... repainting lines " + firstLine + "-" + line);
			fireChangedUpdate(new DefaultDocumentEvent(firstLine, line,
								DocumentEvent.EventType.CHANGE));
		}

		return line;

	}


	/**
	 * Updates internal state information; e.g. the "last tokens on lines"
	 * data.  After this, a changed update is fired to let listeners know that
	 * the document's structure has changed.<p>
	 *
	 * This is called internally whenever the syntax style changes.
	 */
	protected void updateSyntaxHighlightingInformation() {

		// Reinitialize the "last token on each line" array.  Note that since
		// the actual text in the document isn't changing, the number of lines
		// is the same.
		Element map = getDefaultRootElement();
		int numLines = map.getElementCount();
		int lastTokenType = Token.NULL;
		for (int i=0; i<numLines; i++) {
			setSharedSegment(i);
			lastTokenType = tokenMaker.getLastTokenTypeOnLine(s, lastTokenType);
			lastTokensOnLines.set(i, lastTokenType);
		}

		// Let everybody know that syntax styles have (probably) changed.
		fireChangedUpdate(new DefaultDocumentEvent(
						0, numLines-1, DocumentEvent.EventType.CHANGE));

	}


	/**
	 * Overridden for custom serialization purposes.
	 *
	 * @param out The stream to write to.
	 * @throws IOException If an IO error occurs.
	 */
	private void writeObject(ObjectOutputStream out)throws IOException {
		out.defaultWriteObject();
		out.writeBoolean(isWhitespaceVisible());
	}


}