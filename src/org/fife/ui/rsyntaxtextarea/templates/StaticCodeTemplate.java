/*
 * 02/21/2005
 *
 * CodeTemplate.java - A "template" (macro) for commonly-typed code.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.templates;

import java.io.IOException;
import java.io.ObjectInputStream;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;


/**
 * A code template that inserts static text before and after the caret.<p>
 *
 * For example, you can associate the identifier <code>forb</code>
 * (short for "for-block") with the following code:<p>
 *
 * <pre>
 *   for (&lt;caret&gt;) {
 *
 *   }
 * </pre>
 *
 * Then, whenever you type <code>forb</code> followed by a trigger
 * (e.g., a space) into a text area with this <code>CodeTemplate</code>,
 * the code snippet is added in place of <code>forb</code>.  Further,
 * the caret is placed at the position denoted by <code>&lt;caret&gt;</code>.
 *
 * @author Robert Futrell
 * @version 0.1
 * @see CodeTemplate
 */
public class StaticCodeTemplate extends AbstractCodeTemplate {

	private static final long serialVersionUID = 1;

	/**
	 * The code inserted before the caret position.
	 */
	private String beforeCaret;

	/**
	 * The code inserted after the caret position.
	 */
	private String afterCaret;

	/**
	 * Cached value representing whether <code>beforeCaret</code> contains
	 * one or more newlines.
	 */
	private transient int firstBeforeNewline;

	/**
	 * Cached value representing whether <code>afterCaret</code> contains
	 * one or more newlines.
	 */
	private transient int firstAfterNewline;

	private static final String EMPTY_STRING		= "";


	/**
	 * Constructor.  This constructor only exists to support persistance
	 * through serialization.
	 */
	public StaticCodeTemplate() {
	}


	/**
	 * Constructor.
	 *
	 * @param id The ID of this code template.
	 * @param beforeCaret The text to place before the caret.
	 * @param afterCaret The text to place after the caret.
	 */
	public StaticCodeTemplate(String id, String beforeCaret, String afterCaret){
		super(id);
		setBeforeCaretText(beforeCaret);
		setAfterCaretText(afterCaret);
	}


	/**
	 * Returns the text that will be placed after the caret.
	 *
	 * @return The text.
	 * @see #setAfterCaretText
	 */
	public String getAfterCaretText() {
		return afterCaret;
	}


	/**
	 * Returns the text that will be placed before the caret.
	 *
	 * @return The text.
	 * @see #setBeforeCaretText
	 */
	public String getBeforeCaretText() {
		return beforeCaret;
	}


	/**
	 * Returns the "after caret" text, with each new line indented by
	 * the specified amount.
	 *
	 * @param indent The amount to indent.
	 * @return The "after caret" text.
	 */
	private String getAfterTextIndented(String indent) {
		return getTextIndented(getAfterCaretText(), firstAfterNewline, indent);
	}


	/**
	 * Returns the "before caret" text, with each new line indented by
	 * the specified amount.
	 *
	 * @param indent The amount to indent.
	 * @return The "before caret" text.
	 */
	private String getBeforeTextIndented(String indent) {
		return getTextIndented(getBeforeCaretText(),firstBeforeNewline,indent);
	}


	/**
	 * Returns text with newlines indented by the specifed amount.
	 *
	 * @param text The original text.
	 * @param firstNewline The index of the first '\n' character.
	 * @param indent The amount to indent.
	 * @return The indented text.
	 */
	private String getTextIndented(String text,int firstNewline,String indent) {
		if (firstNewline==-1) {
			return text;
		}
		int pos = 0;
		int old = firstNewline+1;
		StringBuffer sb = new StringBuffer(text.substring(0, old));
		sb.append(indent);
		while ((pos=text.indexOf('\n', old))>-1) {
			sb.append(text.substring(old, pos+1));
			sb.append(indent);
			old = pos+1;
		}
		if (old<text.length()) {
			sb.append(text.substring(old));
		}
		return sb.toString();
	}


	/**
	 * Invokes this code template.  The changes are made to the given text
	 * area.
	 *
	 * @param textArea The text area to operate on.
	 * @throws BadLocationException If something bad happens.
	 */
	public void invoke(RSyntaxTextArea textArea) throws BadLocationException {

		Caret c = textArea.getCaret();
		int dot = c.getDot();
		int mark = c.getMark();
		int p0 = Math.min(dot, mark);
		int p1 = Math.max(dot, mark);
		RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
		Element map = doc.getDefaultRootElement();

		int lineNum = map.getElementIndex(dot);
		Element line = map.getElement(lineNum);
		int start = line.getStartOffset();
		int end = line.getEndOffset()-1; // Why always "-1"?
		String s = textArea.getText(start,end-start);
		int len = s.length();

		// endWS is the end of the leading whitespace
		// of the current line.
		int endWS = 0;
		while (endWS<len && RSyntaxUtilities.isWhitespace(s.charAt(endWS))) {
			endWS++;
		}
		s = s.substring(0, endWS);
		p0 -= getID().length();
		String beforeText = getBeforeTextIndented(s);
		String afterText = getAfterTextIndented(s);
		doc.replace(p0,p1-p0, beforeText+afterText, null);
		textArea.setCaretPosition(p0+beforeText.length());

	}


	/**
	 * Called when reading a serialized version of this document.  This is
	 * overridden to initialize the transient members of this class.
	 *
	 * @param in The input stream to read from.
	 * @throws ClassNotFoundException Never.
	 * @throws IOException If an IO error occurs.
	 */
	private void readObject(ObjectInputStream in) throws ClassNotFoundException,
											IOException  {
		in.defaultReadObject();
		// "Resetting" before and after text to the same values will replace
		// nulls with empty srings, and set transient "first*Newline" values.
		setBeforeCaretText(this.beforeCaret);
		setAfterCaretText(this.afterCaret);
	}


	/**
	 * Sets the text to place after the caret.
	 *
	 * @param afterCaret The text.
	 * @see #getAfterCaretText()
	 */
	public void setAfterCaretText(String afterCaret) {
		this.afterCaret = afterCaret==null ? EMPTY_STRING : afterCaret;
		firstAfterNewline = this.afterCaret.indexOf('\n');
	}


	/**
	 * Sets the text to place before the caret.
	 *
	 * @param beforeCaret The text.
	 * @see #getBeforeCaretText()
	 */
	public void setBeforeCaretText(String beforeCaret) {
		this.beforeCaret = beforeCaret==null ? EMPTY_STRING : beforeCaret;
		firstBeforeNewline = this.beforeCaret.indexOf('\n');
	}


	/**
	 * Returns a string representation of this template for debugging
	 * purposes.
	 *
	 * @return A string representation of this template.
	 */
	public String toString() {
		return "[StaticCodeTemplate: id=" + getID() +
			", text=" + getBeforeCaretText() + "|" + getAfterCaretText() + "]";
	}


}