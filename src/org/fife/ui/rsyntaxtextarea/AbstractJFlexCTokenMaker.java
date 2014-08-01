/*
 * 01/25/2009
 *
 * AbstractJFlexCTokenMaker.java - Base class for token makers that use curly
 * braces to denote code blocks, such as C, C++, Java, Perl, etc.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.event.ActionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;

import org.fife.ui.rtextarea.RTextArea;



/**
 * Base class for JFlex-based token makers using C-style syntax.  This class
 * knows how to auto-indent after opening braces and parens.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class AbstractJFlexCTokenMaker extends AbstractJFlexTokenMaker {

	protected static final Action INSERT_BREAK_ACTION = new InsertBreakAction();


	/**
	 * Returns <code>true</code> always as C-style languages use curly braces
	 * to denote code blocks.
	 *
	 * @return <code>true</code> always.
	 */
	public boolean getCurlyBracesDenoteCodeBlocks() {
		return true;
	}


	/**
	 * Returns an action to handle "insert break" key presses (i.e. Enter).
	 * An action is returned that handles newlines differently in multi-line
	 * comments.
	 *
	 * @return The action.
	 */
	public Action getInsertBreakAction() {
		return INSERT_BREAK_ACTION;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean getMarkOccurrencesOfTokenType(int type) {
		return type==Token.IDENTIFIER || type==Token.FUNCTION;
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean getShouldIndentNextLineAfter(Token t) {
		if (t!=null && t.textCount==1) {
			char ch = t.text[t.textOffset];
			return ch=='{' || ch=='(';
		}
		return false;
	}


	/**
	 * Action that knows how to special-case inserting a newline in a
	 * multi-line comment for languages like C and Java.
	 */
	private static class InsertBreakAction extends
							RSyntaxTextAreaEditorKit.InsertBreakAction {

		private static final Pattern p =
							Pattern.compile("([ \\t]*)(/?[\\*]+)([ \\t]*)");

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			RSyntaxTextArea rsta = (RSyntaxTextArea)getTextComponent(e);
			RSyntaxDocument doc = (RSyntaxDocument)rsta.getDocument();

			int line = textArea.getCaretLineNumber();
			int type = doc.getLastTokenTypeOnLine(line);

			// Only in MLC's should we try this
			if (type==Token.COMMENT_DOCUMENTATION ||
					type==Token.COMMENT_MULTILINE) {
				insertBreakInMLC(e, rsta, line);
			}
			else {
				handleInsertBreak(rsta, true);
			}

		}


		/**
		 * Returns whether the MLC token containing <code>offs</code> appears
		 * to have a "nested" comment (i.e., contains "<code>/*</code>"
		 * somewhere inside of it).  This implies that it is likely a "new" MLC
		 * and needs to be closed.  While not foolproof, this is usually good
		 * enough of a sign.
		 *
		 * @param textArea
		 * @param line
		 * @param offs
		 * @return Whether a comment appears to be nested inside this one.
		 */
		private boolean appearsNested(RSyntaxTextArea textArea,
						int line, int offs) {

			final int firstLine = line; // Remember the line we start at.

			while (line<textArea.getLineCount()) {
				Token t = textArea.getTokenListForLine(line);
				int i = 0;
				// If examining the first line, start at offs.
				if (line++==firstLine) {
					t = RSyntaxUtilities.getTokenAtOffset(t, offs);
					if (t==null) { // offs was at end of the line
						continue;
					}
					i = t.documentToToken(offs);
				}
				else {
					i = t.textOffset;
				}
				while (i<t.textOffset+t.textCount-1) {
					if (t.text[i]=='/' && t.text[i+1]=='*') {
						return true;
					}
					i++;
				}
				// If tokens come after this one on this line, our MLC ended.
				if (t.getNextToken()!=null) {
					return false;
				}
			}

			return true; // No match - MLC goes to the end of the file

		}

		private void insertBreakInMLC(ActionEvent e, RSyntaxTextArea textArea,
										int line) {

			Matcher m = null;
			int start = -1;
			int end = -1;
			try {
				start = textArea.getLineStartOffset(line);
				end = textArea.getLineEndOffset(line);
				String text = textArea.getText(start, end-start);
				m = p.matcher(text);
			} catch (BadLocationException ble) { // Never happens
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				ble.printStackTrace();
				return;
			}

			if (m.lookingAt()) {

				String leadingWS = m.group(1);
				String mlcMarker = m.group(2);

				// If the caret is "inside" any leading whitespace or MLC
				// marker, move it to the end of the line.
				int dot = textArea.getCaretPosition();
				if (dot>=start &&
						dot<start+leadingWS.length()+mlcMarker.length()) {
					// If we're in the whitespace before the very start of the
					// MLC though, just insert a normal newline
					if (mlcMarker.charAt(0)=='/') {
						handleInsertBreak(textArea, true);
						return;
					}
					textArea.setCaretPosition(end-1);
				}

				boolean firstMlcLine = mlcMarker.charAt(0)=='/';
				boolean nested = appearsNested(textArea, line,
												start+leadingWS.length()+2);
				String header = leadingWS +
						(firstMlcLine ? " * " : "*") +
						m.group(3);
				textArea.replaceSelection("\n" + header);
				if (nested) {
					dot = textArea.getCaretPosition(); // Has changed
					textArea.insert("\n" + leadingWS + " */", dot);
					textArea.setCaretPosition(dot);
				}

			}
			else {
				handleInsertBreak(textArea, true);
			}

		}

	}


}