/*
 * 08/13/2004
 *
 * RTextAreaEditorKit.java - The editor kit used by RTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.BreakIterator;
import java.text.DateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.text.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;


/**
 * An extension of <code>DefaultEditorKit</code> that adds functionality found
 * in <code>RTextArea</code>.
 *
 * @author Robert Futrell
 * @version 0.1
 */
// FIXME:  Replace Utilities calls with custom versions (in RSyntaxUtilities) to
// cut down on all of the modelToViews, as each call causes
// a getTokenList => expensive!
public class RTextAreaEditorKit extends DefaultEditorKit {

	/**
	 * The name of the action that begins recording a macro.
	 */
	public static final String rtaBeginRecordingMacroAction	= "RTA.BeginRecordingMacroAction";

	/**
	 * The name of the action to decrease the font size.
	 */
	public static final String rtaDecreaseFontSizeAction		= "RTA.DecreaseFontSizeAction";

	/**
	 * The name of the action that deletes the current line.
	 */
	public static final String rtaDeleteLineAction			= "RTA.DeleteLineAction";

	/**
	 * The name of the action to delete the word before the caret.
	 */
	public static final String rtaDeletePrevWordAction		= "RTA.DeletePrevWordAction";

	/**
	 * The name of the action taken to delete the remainder of the line (from
	 * the caret position to the end of the line).
	 */
	public static final String rtaDeleteRestOfLineAction		= "RTA.DeleteRestOfLineAction";

	/**
	 * The name of the action that completes the word at the caret position
	 * with the last word in the document that starts with the text up to the
	 * caret.
	 */
	public static final String rtaDumbCompleteWordAction		= "RTA.DumbCompleteWordAction";

	/**
	 * The name of the action that ends recording a macro.
	 */
	public static final String rtaEndRecordingMacroAction		= "RTA.EndRecordingMacroAction";

	/**
	 * The name of the action to increase the font size.
	 */
	public static final String rtaIncreaseFontSizeAction		= "RTA.IncreaseFontSizeAction";

	/**
	 * The name of the action that inverts the case of the current selection.
	 */
	public static final String rtaInvertSelectionCaseAction	= "RTA.InvertCaseAction";

	/**
	 * The name of the action to join two lines.
	 */
	public static final String rtaJoinLinesAction			= "RTA.JoinLinesAction";

	/**
	 * Action to move a line down.
	 */
	public static final String rtaLineDownAction				= "RTA.LineDownAction";

	/**
	 * Action to move a line up.
	 */
	public static final String rtaLineUpAction				= "RTA.LineUpAction";

	/**
	 * The name of the action to make the current selection lower-case.
	 */
	public static final String rtaLowerSelectionCaseAction		= "RTA.LowerCaseAction";

	/**
	 * Action to select the next occurrence of the selected text.
	 */
	public static final String rtaNextOccurrenceAction		= "RTA.NextOccurrenceAction";

	/**
     * Action to select the previous occurrence of the selected text.
	 */
	public static final String rtaPrevOccurrenceAction		= "RTA.PrevOccurrenceAction";

	/**
	 * Action to jump to the next bookmark.
	 */
	public static final String rtaNextBookmarkAction		= "RTA.NextBookmarkAction";

	/**
	 * Action to jump to the previous bookmark.
	 */
	public static final String rtaPrevBookmarkAction		= "RTA.PrevBookmarkAction";

	/**
	 * The name of the action that "plays back" the last macro.
	 */
	public static final String rtaPlaybackLastMacroAction		= "RTA.PlaybackLastMacroAction";

	/**
	 * The name of the action for "redoing" the last action undone.
	 */
	public static final String rtaRedoAction				= "RTA.RedoAction";

	/**
	 * The name of the action to scroll the text area down one line
	 * without changing the caret's position.
	 */
	public static final String rtaScrollDownAction			= "RTA.ScrollDownAction";

	/**
	 * The name of the action to scroll the text area up one line
	 * without changing the caret's position.
	 */
	public static final String rtaScrollUpAction				= "RTA.ScrollUpAction";

	/**
	 * The name of the action for "paging up" with the selection.
	 */
	public static final String rtaSelectionPageUpAction		= "RTA.SelectionPageUpAction";

	/**
	 * The name of the action for "paging down" with the selection.
	 */
	public static final String rtaSelectionPageDownAction		= "RTA.SelectionPageDownAction";

	/**
	 * The name of the action for "paging left" with the selection.
	 */
	public static final String rtaSelectionPageLeftAction		= "RTA.SelectionPageLeftAction";

	/**
	 * The name of the action for "paging right" with the selection.
	 */
	public static final String rtaSelectionPageRightAction		= "RTA.SelectionPageRightAction";

	/**
	 * The name of the action for inserting a time/date stamp.
	 */
	public static final String rtaTimeDateAction				= "RTA.TimeDateAction";

	/**
	 * Toggles whether the current line has a bookmark, if this text area
	 * is in an {@link RTextScrollPane}.
	 */
	public static final String rtaToggleBookmarkAction		= "RTA.ToggleBookmarkAction";

	/**
	 * The name of the action taken when the user hits the Insert key (thus
	 * toggling between insert and overwrite modes).
	 */
	public static final String rtaToggleTextModeAction		= "RTA.ToggleTextModeAction";

	/**
	 * The name of the action for "undoing" the last action done.
	 */
	public static final String rtaUndoAction				= "RTA.UndoAction";

	/**
	 * The name of the action for unselecting any selected text in the text
	 * area.
	 */
	public static final String rtaUnselectAction				= "RTA.UnselectAction";

	/**
	 * The name of the action for making the current selection upper-case.
	 */
	public static final String rtaUpperSelectionCaseAction		= "RTA.UpperCaseAction";

	/**
	 * The actions that <code>RTextAreaEditorKit</code> adds to those of
	 * the default editor kit.
	 */
	private static final RecordableTextAction[] defaultActions = {
		new BeginAction(beginAction, false), 
		new BeginAction(selectionBeginAction, true), 
		new BeginLineAction(beginLineAction, false),  
		new BeginLineAction(selectionBeginLineAction, true),  
		new BeginRecordingMacroAction(),
		new BeginWordAction(beginWordAction, false),
		new BeginWordAction(selectionBeginWordAction, true),
		new CopyAction(),
		new CutAction(),
		new DefaultKeyTypedAction(),
		new DeleteLineAction(),
		new DeleteNextCharAction(),
		new DeletePrevCharAction(),
		new DeletePrevWordAction(),
		new DeleteRestOfLineAction(),
		new DumbCompleteWordAction(),
		new EndAction(endAction, false),
		new EndAction(selectionEndAction, true),
		new EndLineAction(endLineAction, false),
		new EndLineAction(selectionEndLineAction, true),
		new EndRecordingMacroAction(),
		new EndWordAction(endWordAction, false),
		new EndWordAction(endWordAction, true),
		new InsertBreakAction(),
		new InsertContentAction(),
		new InsertTabAction(),
		new InvertSelectionCaseAction(),
		new JoinLinesAction(),
		new LowerSelectionCaseAction(),
		new LineMoveAction(rtaLineUpAction, -1),
		new LineMoveAction(rtaLineDownAction, 1),
		new NextBookmarkAction(rtaNextBookmarkAction, true),
		new NextBookmarkAction(rtaPrevBookmarkAction, false),
		new NextVisualPositionAction(forwardAction, false, SwingConstants.EAST),
		new NextVisualPositionAction(backwardAction, false, SwingConstants.WEST),
		new NextVisualPositionAction(selectionForwardAction, true, SwingConstants.EAST),
		new NextVisualPositionAction(selectionBackwardAction, true, SwingConstants.WEST),
		new NextVisualPositionAction(upAction, false, SwingConstants.NORTH),
		new NextVisualPositionAction(downAction, false, SwingConstants.SOUTH),
		new NextVisualPositionAction(selectionUpAction, true, SwingConstants.NORTH),
		new NextVisualPositionAction(selectionDownAction, true, SwingConstants.SOUTH),
        new NextOccurrenceAction(rtaNextOccurrenceAction),
        new PreviousOccurrenceAction(rtaPrevOccurrenceAction),
		new NextWordAction(nextWordAction, false),
		new NextWordAction(selectionNextWordAction, true),
		new PageAction(rtaSelectionPageLeftAction, true, true), 
		new PageAction(rtaSelectionPageRightAction, false, true),
		new PasteAction(),
		new PlaybackLastMacroAction(),
		new PreviousWordAction(previousWordAction, false),  
		new PreviousWordAction(selectionPreviousWordAction, true),
		new RedoAction(),
		new ScrollAction(rtaScrollUpAction, -1),
		new ScrollAction(rtaScrollDownAction, 1),
		new SelectAllAction(),
		new SelectLineAction(),
		new SelectWordAction(),
		new SetReadOnlyAction(),
		new SetWritableAction(),
		new ToggleBookmarkAction(),
		new ToggleTextModeAction(),
		new UndoAction(),
		new UnselectAction(),
		new UpperSelectionCaseAction(),
		new VerticalPageAction(pageUpAction, -1, false), 
		new VerticalPageAction(pageDownAction, 1, false),
		new VerticalPageAction(rtaSelectionPageUpAction, -1, true), 
		new VerticalPageAction(rtaSelectionPageDownAction, 1, true)
	};

	/**
	 * The amount of characters read at a time when reading a file.
	 */
	private static final int READBUFFER_SIZE	= 32768;


	/**
	 * Constructor.
	 */
	public RTextAreaEditorKit() {
		super();
	}


	/**
	 * Creates an icon row header to use in the gutter for a text area.
	 *
	 * @param textArea The text area.
	 * @return The icon row header.
	 */
	public IconRowHeader createIconRowHeader(RTextArea textArea) {
		return new IconRowHeader(textArea);
	}


	/**
	 * Creates a line number list to use in the gutter for a text area.
	 *
	 * @param textArea The text area.
	 * @return The line number list.
	 */
	public LineNumberList createLineNumberList(RTextArea textArea) {
		return new LineNumberList(textArea);
	}


	/**
	 * Fetches the set of commands that can be used
	 * on a text component that is using a model and
	 * view produced by this kit.
	 *
	 * @return the command list
	 */ 
	public Action[] getActions() {
		return defaultActions;
	}


	/**
	 * Inserts content from the given stream, which will be 
	 * treated as plain text.  This method is overridden merely
	 * so we can increase the number of characters read at a time.
	 * 
	 * @param in  The stream to read from
	 * @param doc The destination for the insertion.
	 * @param pos The location in the document to place the
	 *   content >= 0.
	 * @exception IOException on any I/O error
	 * @exception BadLocationException if pos represents an invalid
	 *   location within the document.
	*/
	public void read(Reader in, Document doc, int pos) 
				throws IOException, BadLocationException {

		char[] buff = new char[READBUFFER_SIZE];
		int nch;
		boolean lastWasCR = false;
		boolean isCRLF = false;
		boolean isCR = false;
		int last;
		boolean wasEmpty = (doc.getLength() == 0);

		// Read in a block at a time, mapping \r\n to \n, as well as single
		// \r's to \n's. If a \r\n is encountered, \r\n will be set as the
		// newline string for the document, if \r is encountered it will
		// be set as the newline character, otherwise the newline property
		// for the document will be removed.
		while ((nch = in.read(buff, 0, buff.length)) != -1) {
			last = 0;
			for (int counter = 0; counter < nch; counter++) {
				switch (buff[counter]) {
					case '\r':
						if (lastWasCR) {
							isCR = true;
							if (counter == 0) {
								doc.insertString(pos, "\n", null);
								pos++;
							}
							else {
								buff[counter - 1] = '\n';
							}
						}
						else {
							lastWasCR = true;
						}
						break;
					case '\n':
						if (lastWasCR) {
							if (counter > (last + 1)) {
								doc.insertString(pos, new String(buff, last,
												counter - last - 1), null);
								pos += (counter - last - 1);
							}
							// else nothing to do, can skip \r, next write will
							// write \n
							lastWasCR = false;
							last = counter;
							isCRLF = true;
						}
						break;
					default:
						if (lastWasCR) {
							isCR = true;
							if (counter == 0) {
								doc.insertString(pos, "\n", null);
								pos++;
							}
							else {
								buff[counter - 1] = '\n';
							}
							lastWasCR = false;
						}
						break;
				} // End of switch (buff[counter]).
			} // End of for (int counter = 0; counter < nch; counter++).

			if (last < nch) {
				if(lastWasCR) {
					if (last < (nch - 1)) {
						doc.insertString(pos, new String(buff, last,
										nch - last - 1), null);
						pos += (nch - last - 1);
					}
				}
				else {
					doc.insertString(pos, new String(buff, last,
									nch - last), null);
					pos += (nch - last);
				}
			}

		} // End of while ((nch = in.read(buff, 0, buff.length)) != -1).

		if (lastWasCR) {
			doc.insertString(pos, "\n", null);
			isCR = true;
		}

		if (wasEmpty) {
			if (isCRLF) {
				doc.putProperty(EndOfLineStringProperty, "\r\n");
			}
			else if (isCR) {
				doc.putProperty(EndOfLineStringProperty, "\r");
			}
			else {
				doc.putProperty(EndOfLineStringProperty, "\n");
			}
		}

	}


	/**
	 * Creates a beep.
	 */
	public static class BeepAction extends RecordableTextAction {

		public BeepAction() {
			super(beepAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			UIManager.getLookAndFeel().provideErrorFeedback(textArea);
		}

		public final String getMacroID() {
			return beepAction;
		}

	}


	/**
	 * Moves the caret to the beginning of the document.
	 */
	public static class BeginAction extends RecordableTextAction {

 		private boolean select;

		public BeginAction(String name, boolean select) {
			super(name);
			this.select = select;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (select)
				textArea.moveCaretPosition(0);
			else
				textArea.setCaretPosition(0);
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Toggles the position of the caret between the beginning of the line,
	 * and the first non-whitespace character on the line.
	 */
	public static class BeginLineAction extends RecordableTextAction {

 		private Segment currentLine = new Segment(); // For speed.
 		private boolean select;

		public BeginLineAction(String name, boolean select) {
			super(name);
			this.select = select;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			int newPos = 0;

			try {

				// Is line wrap enabled?
				if (textArea.getLineWrap()) {
					int offs = textArea.getCaretPosition();
					// TODO:  Replace Utilities call with custom version
					// to cut down on all of the modelToViews, as each call
					// causes TokenList => expensive!
					int begOffs = Utilities.getRowStart(textArea, offs);
					// TODO: line wrap doesn't currently toggle between
					// the first non-whitespace char and the actual start
					// of the line line the no-line-wrap version does.
					newPos = begOffs;
				}

				// No line wrap - optimized for performance!
				else {

					// We use the elements instead of calling
					// getLineOfOffset(), etc. to speed things up just a
					// tad (i.e. micro-optimize).
					int caretPosition = textArea.getCaretPosition();
					Document document = textArea.getDocument();
					Element map = document.getDefaultRootElement();
					int currentLineNum = map.getElementIndex(caretPosition);
					Element currentLineElement = map.getElement(currentLineNum);
					int currentLineStart = currentLineElement.getStartOffset();
					int currentLineEnd = currentLineElement.getEndOffset();
					int count = currentLineEnd - currentLineStart;
					if (count>0) { // If there are chars in the line...
						document.getText(currentLineStart, count, currentLine);
						int firstNonWhitespace = getFirstNonWhitespacePos();
						firstNonWhitespace = currentLineStart +
								(firstNonWhitespace - currentLine.offset);
						if (caretPosition!=firstNonWhitespace) {
							newPos = firstNonWhitespace;
						}
						else {
							newPos = currentLineStart;
						}
					}
					else { // Empty line (at end of the document only).
						newPos = currentLineStart;
					}

				}

				if (select) {
					textArea.moveCaretPosition(newPos);
				}
				else {
					textArea.setCaretPosition(newPos);
				}
				//e.consume();

			} catch (BadLocationException ble) {
				/* Shouldn't ever happen. */
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				ble.printStackTrace();
			}

		}

		private final int getFirstNonWhitespacePos() {
			int offset = currentLine.offset;
			int end = offset + currentLine.count - 1;
			int pos = offset;
			char[] array = currentLine.array;
			char currentChar = array[pos];
			while ((currentChar=='\t' || currentChar==' ') && (++pos<end))
				currentChar = array[pos];
			return pos;
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action that begins recording a macro.
	 */
	public static class BeginRecordingMacroAction extends RecordableTextAction {

 
		public BeginRecordingMacroAction() {
			super(rtaBeginRecordingMacroAction);
		}

		public BeginRecordingMacroAction(String name, Icon icon,
					String desc, Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			RTextArea.beginRecordingMacro();
		}

		public boolean isRecordable() {
			return false; // Never record the recording of a macro!
		}

		public final String getMacroID() {
			return rtaBeginRecordingMacroAction;
		}

	}


	/**
	 * Positions the caret at the beginning of the word.
	 */
	protected static class BeginWordAction extends RecordableTextAction {

 		private boolean select;

		protected BeginWordAction(String name, boolean select) {
			super(name);
			this.select = select;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			try {
				int offs = textArea.getCaretPosition();
				int begOffs = getWordStart(textArea, offs);
				if (select)
					textArea.moveCaretPosition(begOffs);
				else
					textArea.setCaretPosition(begOffs);
			} catch (BadLocationException ble) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		public final String getMacroID() {
			return getName();
		}

		protected int getWordStart(RTextArea textArea, int offs)
										throws BadLocationException {
			return Utilities.getWordStart(textArea, offs);
		}

	}


	/**
	 * Action for copying text.
	 */
	public static class CopyAction extends RecordableTextAction {

 
		public CopyAction() {
			super(DefaultEditorKit.copyAction);
		}

		public CopyAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			textArea.copy();
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return DefaultEditorKit.copyAction;
		}

	}


	/**
	 * Action for cutting text.
	 */
	public static class CutAction extends RecordableTextAction {

		public CutAction() {
			super(DefaultEditorKit.cutAction);
		}

		public CutAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			textArea.cut();
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return DefaultEditorKit.cutAction;
		}

	}


	/**
	 * Action for decreasing the font size.
	 */
	public static class DecreaseFontSizeAction extends RecordableTextAction {

 
		protected float decreaseAmount;

		protected static final float MINIMUM_SIZE	= 2.0f;

		public DecreaseFontSizeAction() {
			super(rtaDecreaseFontSizeAction);
			initialize();
		}

		public DecreaseFontSizeAction(String name, Icon icon, String desc,
							Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
			initialize();
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			Font font = textArea.getFont();
			float oldSize = font.getSize2D();
			float newSize = oldSize - decreaseAmount;
			if (newSize>=MINIMUM_SIZE) {
				// Shrink by decreaseAmount.
				font = font.deriveFont(newSize);
				textArea.setFont(font);
			}
			else if (oldSize>MINIMUM_SIZE) {
				// Can't shrink by full decreaseAmount, but can shrink a
				// little bit.
				font = font.deriveFont(MINIMUM_SIZE);
				textArea.setFont(font);
			}
			else {
				// Our font size must be at or below MINIMUM_SIZE.
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return rtaDecreaseFontSizeAction;
		}

		protected void initialize() {
			decreaseAmount = 1.0f;
		}

	}


	/**
	 * The action to use when no actions in the input/action map meet the key
	 * pressed.  This is actually called from the keymap I believe.
	 */
	public static class DefaultKeyTypedAction extends RecordableTextAction {

		private Action delegate;

		public DefaultKeyTypedAction() {
			super(DefaultEditorKit.defaultKeyTypedAction, null, null, null,
					null);
			delegate = new DefaultEditorKit.DefaultKeyTypedAction();
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			// DefaultKeyTypedAction *is* different across different JVM's
			// (at least the OSX implementation must be different - Alt+Numbers
			// inputs symbols such as '[', '{', etc., which is a *required*
			// feature on MacBooks running with non-English input, such as
			// German or Swedish Pro).  So we can't just copy the
			// implementation, we must delegate to it.
			delegate.actionPerformed(e);
		}

		public final String getMacroID() {
			return DefaultEditorKit.defaultKeyTypedAction;
		}

	}


	/**
	 * Deletes the current line(s).
	 */
	public static class DeleteLineAction extends RecordableTextAction {

		public DeleteLineAction() {
			super(RTextAreaEditorKit.rtaDeleteLineAction, null, null, null,
						null);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			int selStart = textArea.getSelectionStart();
			int selEnd   = textArea.getSelectionEnd();

			try {

				int line1     = textArea.getLineOfOffset(selStart);
				int startOffs = textArea.getLineStartOffset(line1);
				int line2     = textArea.getLineOfOffset(selEnd);
				int endOffs   = textArea.getLineEndOffset(line2);

				// Don't remove the last line if no actual chars are selected
				if (line2>line1) {
					if (selEnd==textArea.getLineStartOffset(line2)) {
						endOffs = selEnd;
					}
				}

				textArea.replaceRange(null, startOffs, endOffs);

			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}

		}

		public final String getMacroID() {
			return RTextAreaEditorKit.rtaDeleteLineAction;
		}

	}


	/**
	 * Deletes the character of content that follows the current caret
	 * position.
	 */
	public static class DeleteNextCharAction extends RecordableTextAction {

		public DeleteNextCharAction() {
			super(DefaultEditorKit.deleteNextCharAction, null, null,
												null, null);
		}

		public DeleteNextCharAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			boolean beep = true;
			if ((textArea != null) && (textArea.isEditable())) {
				try {
					Document doc = textArea.getDocument();
					Caret caret = textArea.getCaret();
					int dot = caret.getDot();
					int mark = caret.getMark();
					if (dot != mark) {
						doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
						beep = false;
					}
					else if (dot < doc.getLength()) {
						int delChars = 1;
						if (dot < doc.getLength() - 1) {
							String dotChars = doc.getText(dot, 2);
							char c0 = dotChars.charAt(0);
							char c1 = dotChars.charAt(1);
							if (c0 >= '\uD800' && c0 <= '\uDBFF' &&
								c1 >= '\uDC00' && c1 <= '\uDFFF') {
								delChars = 2;
							}
						}
						doc.remove(dot, delChars);
						beep = false;
					}
				} catch (BadLocationException bl) {
				}
			}

			if (beep)
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);

			textArea.requestFocusInWindow();

		}

		public final String getMacroID() {
			return DefaultEditorKit.deleteNextCharAction;
		}

	}


	/**
	 * Deletes the character of content that precedes the current caret
	 * position.
	 */
	public static class DeletePrevCharAction extends RecordableTextAction {
 
		public DeletePrevCharAction() {
			super(deletePrevCharAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			boolean beep = true;
			if ((textArea != null) && (textArea.isEditable())) {
				try {
					Document doc = textArea.getDocument();
					Caret caret = textArea.getCaret();
					int dot = caret.getDot();
					int mark = caret.getMark();
					if (dot != mark) {
						doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
						beep = false;
					}
					else if (dot > 0) {
						int delChars = 1;
						if (dot > 1) {
							String dotChars = doc.getText(dot - 2, 2);
							char c0 = dotChars.charAt(0);
							char c1 = dotChars.charAt(1);
							if (c0 >= '\uD800' && c0 <= '\uDBFF' &&
								c1 >= '\uDC00' && c1 <= '\uDFFF') {
								delChars = 2;
							}
						}
						doc.remove(dot - delChars, delChars);
						beep = false;
					}
				} catch (BadLocationException bl) {
				}
			}

			if (beep)
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);

		}

		public final String getMacroID() {
			return DefaultEditorKit.deletePrevCharAction;
		}

	}


	/**
	 * Action that deletes the previous word in the text area.
	 */
	public static class DeletePrevWordAction extends RecordableTextAction {

		public DeletePrevWordAction() {
			super(rtaDeletePrevWordAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			try {
				int end = textArea.getSelectionStart();
				int start = getPreviousWordStart(textArea, end);
				if (end>start) {
					textArea.getDocument().remove(start, end-start);
				}
			} catch (BadLocationException ex) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		public String getMacroID() {
			return rtaDeletePrevWordAction;
		}

		/**
		 * Returns the starting offset to delete.  Exists so subclasses can
		 * override.
		 */
		protected int getPreviousWordStart(RTextArea textArea, int end)
				throws BadLocationException {
			return Utilities.getPreviousWord(textArea, end);
		}

	}


	/**
	 * Action that deletes all text from the caret position to the end of the
	 * caret's line.
	 */
	public static class DeleteRestOfLineAction extends RecordableTextAction {

 
		public DeleteRestOfLineAction() {
			super(rtaDeleteRestOfLineAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			try {

				// We use the elements instead of calling getLineOfOffset(),
				// etc. to speed things up just a tad (i.e. micro-optimize).
				Document document = textArea.getDocument();
				int caretPosition = textArea.getCaretPosition();
				Element map = document.getDefaultRootElement();
				int currentLineNum = map.getElementIndex(caretPosition);
				Element currentLineElement = map.getElement(currentLineNum);
				// Always take -1 as we don't want to remove the newline.
				int currentLineEnd = currentLineElement.getEndOffset()-1;
				if (caretPosition<currentLineEnd) {
					document.remove(caretPosition,
									currentLineEnd-caretPosition);
				}

			} catch (BadLocationException ble) {
				ble.printStackTrace();
			}

		}

		public final String getMacroID() {
			return rtaDeleteRestOfLineAction;
		}

	}


	/**
	 * Finds the most recent word in the document that matches the "word" up
	 * to the current caret position, and auto-completes the rest.  Repeatedly
	 * calling this action at the same location in the document goes one
	 * match back each time it is called.
	 */
	public static class DumbCompleteWordAction extends RecordableTextAction {

		private int lastWordStart;
		private int lastDot;
		private int searchOffs;
		private String lastPrefix;

		public DumbCompleteWordAction() {
			super(rtaDumbCompleteWordAction);
			lastWordStart = searchOffs = lastDot = -1;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				return;
			}

			try {

				int dot = textArea.getCaretPosition();
				if (dot == 0) {
					return;
				}

				int curWordStart = Utilities.getWordStart(textArea, dot - 1);

				if (lastWordStart!=curWordStart || dot!=lastDot) {
					lastPrefix = textArea.getText(curWordStart,dot-curWordStart);
					// Utilities.getWordStart() treats spans of whitespace and
					// single non-letter chars as "words."
					if (lastPrefix.length()==0 ||
							!Character.isLetter(lastPrefix.charAt(lastPrefix.length()-1))) {
						UIManager.getLookAndFeel().provideErrorFeedback(textArea);
						return;
					}
					lastWordStart = dot - lastPrefix.length();
					searchOffs = lastWordStart;
				}

				while (searchOffs > 0) {
					int wordStart = 0;
					try {
						wordStart = Utilities.getPreviousWord(textArea,
							searchOffs);
					} catch (BadLocationException ble) {
						// No more words.  Sometimes happens for example if the
						// document starts off with whitespace - then searchOffs
						// is > 0 but there are no more words
						wordStart = BreakIterator.DONE;
					}
					if (wordStart==BreakIterator.DONE) {
						UIManager.getLookAndFeel().provideErrorFeedback(
								textArea);
						break;
					}
					int end = Utilities.getWordEnd(textArea, wordStart);
					String word = textArea.getText(wordStart, end - wordStart);
					searchOffs = wordStart;
					if (word.startsWith(lastPrefix)) {
						textArea.replaceRange(word, lastWordStart, dot);
						lastDot = textArea.getCaretPosition(); // Maybe shifted
						break;
					}
				}

			} catch (BadLocationException ble) { // Never happens
				ble.printStackTrace();
			}

		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Moves the caret to the end of the document.
	 */
	public static class EndAction extends RecordableTextAction {

 		private boolean select;

		public EndAction(String name, boolean select) {
			super(name);
			this.select = select;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			int dot = getVisibleEnd(textArea);
			if (select)
				textArea.moveCaretPosition(dot);
			else
				textArea.setCaretPosition(dot);
		}

		public final String getMacroID() {
			return getName();
		}

		protected int getVisibleEnd(RTextArea textArea) {
			return textArea.getDocument().getLength();
		}

	}


	/**
	 * Positions the caret at the end of the line.
	 */
	public static class EndLineAction extends RecordableTextAction {

 		private boolean select;

		public EndLineAction(String name, boolean select) {
			super(name);
			this.select = select;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			int offs = textArea.getCaretPosition();
			int endOffs = 0;
			try {
				if (textArea.getLineWrap()) {
					// Must check per character, since one logical line may be
					// many physical lines.
					// FIXME:  Replace Utilities call with custom version to
					// cut down on all of the modelToViews, as each call causes
					// a getTokenList => expensive!
					endOffs = Utilities.getRowEnd(textArea, offs);
				}
				else {
					Element root = textArea.getDocument().getDefaultRootElement();
					int line = root.getElementIndex(offs);
					endOffs = root.getElement(line).getEndOffset() - 1;
				}
				if (select) {
					textArea.moveCaretPosition(endOffs);
				}
				else {
					textArea.setCaretPosition(endOffs);
				}
			} catch (Exception ex) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action that ends recording a macro.
	 */
	public static class EndRecordingMacroAction extends RecordableTextAction {

 
		public EndRecordingMacroAction() {
			super(rtaEndRecordingMacroAction);
		}

		public EndRecordingMacroAction(String name, Icon icon,
					String desc, Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			RTextArea.endRecordingMacro();
		}

		public final String getMacroID() {
			return rtaEndRecordingMacroAction;
		}

		public boolean isRecordable() {
			return false; // Never record the recording of a macro!
		}

	}


	/**
	 * Positions the caret at the end of the word.
	 */
	protected static class EndWordAction extends RecordableTextAction {

 		private boolean select;

		protected EndWordAction(String name, boolean select) {
			super(name);
			this.select = select;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			try {
				int offs = textArea.getCaretPosition();
				int endOffs = getWordEnd(textArea, offs);
				if (select)
					textArea.moveCaretPosition(endOffs);
				else
					textArea.setCaretPosition(endOffs);
			} catch (BadLocationException ble) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
		}

		public final String getMacroID() {
			return getName();
		}

		protected int getWordEnd(RTextArea textArea, int offs)
									throws BadLocationException {
			return Utilities.getWordEnd(textArea, offs);
		}

	}


	/**
	 * Action for increasing the font size.
	 */
	public static class IncreaseFontSizeAction extends RecordableTextAction {

 
		protected float increaseAmount;

		protected static final float MAXIMUM_SIZE	= 40.0f;

		public IncreaseFontSizeAction() {
			super(rtaIncreaseFontSizeAction);
			initialize();
		}

		public IncreaseFontSizeAction(String name, Icon icon, String desc,
							Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
			initialize();
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			Font font = textArea.getFont();
			float oldSize = font.getSize2D();
			float newSize = oldSize + increaseAmount;
			if (newSize<=MAXIMUM_SIZE) {
				// Grow by increaseAmount.
				font = font.deriveFont(newSize);
				textArea.setFont(font);
			}
			else if (oldSize<MAXIMUM_SIZE) {
				// Can't grow by full increaseAmount, but can grow a
				// little bit.
				font = font.deriveFont(MAXIMUM_SIZE);
				textArea.setFont(font);
			}
			else {
				// Our font size must be at or bigger than MAXIMUM_SIZE.
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return rtaIncreaseFontSizeAction;
		}

		protected void initialize() {
			increaseAmount = 1.0f;
		}

	}


	/**
	 * Action for when the user presses the Enter key.
	 */
	public static class InsertBreakAction extends RecordableTextAction {

 
		public InsertBreakAction() {
			super(DefaultEditorKit.insertBreakAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			textArea.replaceSelection("\n");
		}

		public final String getMacroID() {
			return DefaultEditorKit.insertBreakAction;
		}

		/*
		 * Overridden for Sun bug 4515750.  Sun fixed this in a more complicated
		 * way, but I'm not sure why.  See BasicTextUI#getActionMap() and
		 * BasicTextUI.TextActionWrapper.
		 */
		public boolean isEnabled() {
			JTextComponent tc = getTextComponent(null);
			return (tc==null || tc.isEditable()) ? super.isEnabled() : false;
		}

	}


	/**
	 * Action taken when content is to be inserted.
	 */
	public static class InsertContentAction extends RecordableTextAction {

 
		public InsertContentAction() {
			super(DefaultEditorKit.insertContentAction, null, null, null,
					null);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			String content = e.getActionCommand();
			if (content != null)
				textArea.replaceSelection(content);
			else
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
		}

		public final String getMacroID() {
			return DefaultEditorKit.insertContentAction;
		}

	}


	/**
	 * Places a tab character into the document. If there is a selection, it
	 * is removed before the tab is added.
	 */
	public static class InsertTabAction extends RecordableTextAction {

 
		public InsertTabAction() {
			super(insertTabAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			textArea.replaceSelection("\t");
		}

		public final String getMacroID() {
			return DefaultEditorKit.insertTabAction;
		}

	}


	/**
	 * Action to invert the selection's case.
	 */
	public static class InvertSelectionCaseAction extends RecordableTextAction {

 
		public InvertSelectionCaseAction() {
			super(rtaInvertSelectionCaseAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			String selection = textArea.getSelectedText();
			if (selection!=null) {
				StringBuffer buffer = new StringBuffer(selection);
				int length = buffer.length();
				for (int i=0; i<length; i++) {
					char c = buffer.charAt(i);
					if (Character.isUpperCase(c))
						buffer.setCharAt(i, Character.toLowerCase(c));
					else if (Character.isLowerCase(c))
						buffer.setCharAt(i, Character.toUpperCase(c));
				}
				textArea.replaceSelection(buffer.toString());
			}
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action to join the current line and the following line.
	 */
	public static class JoinLinesAction extends RecordableTextAction {

		public JoinLinesAction() {
			super(rtaJoinLinesAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			try {
				Caret c = textArea.getCaret();
				int caretPos = c.getDot();
				Document doc = textArea.getDocument();
				Element map = doc.getDefaultRootElement();
				int lineCount = map.getElementCount();
				int line = map.getElementIndex(caretPos);
				if (line==lineCount-1) {
					UIManager.getLookAndFeel().
								provideErrorFeedback(textArea);
					return;
				}
				Element lineElem = map.getElement(line);
				caretPos = lineElem.getEndOffset() - 1;
				c.setDot(caretPos);		// Gets rid of any selection.
				doc.remove(caretPos, 1);	// Should be '\n'.
			} catch (BadLocationException ble) {
				/* Shouldn't ever happen. */
				ble.printStackTrace();
			}
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action that moves a line up or down.
	 */
	public static class LineMoveAction extends RecordableTextAction {

		private int moveAmt;

		public LineMoveAction(String name, int moveAmt) {
			super(name);
			this.moveAmt = moveAmt;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			try {
				int caret = textArea.getCaretPosition();
				Document doc = textArea.getDocument();
				Element root = doc.getDefaultRootElement();
				int line = root.getElementIndex(caret);
				if (moveAmt==-1 && line>0) {
					moveLineUp(textArea, line);
				}
				else if (moveAmt==1 && line<root.getElementCount()-1) {
					moveLineDown(textArea, line);
				}
				else {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					return;
				}
			} catch (BadLocationException ble) {
				// Never happens.
				ble.printStackTrace();
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
		}

		public final String getMacroID() {
			return getName();
		}

		private final void moveLineDown(RTextArea textArea, int line)
									throws BadLocationException {
			Document doc = textArea.getDocument();
			Element root = doc.getDefaultRootElement();
			Element elem = root.getElement(line);
			int start = elem.getStartOffset();
			int end = elem.getEndOffset();
			int caret = textArea.getCaretPosition();
			int caretOffset = caret - start;
			String text = doc.getText(start, end-start);
			doc.remove(start, end-start);
			Element elem2 = root.getElement(line); // not "line+1" - removed.
			//int start2 = elem2.getStartOffset();
			int end2 = elem2.getEndOffset();
			doc.insertString(end2, text, null);
			elem = root.getElement(line+1);
			textArea.setCaretPosition(elem.getStartOffset()+caretOffset);
		}

		private final void moveLineUp(RTextArea textArea, int line)
									throws BadLocationException {
			Document doc = textArea.getDocument();
			Element root = doc.getDefaultRootElement();
			int lineCount = root.getElementCount();
			Element elem = root.getElement(line);
			int start = elem.getStartOffset();
			int end = line==lineCount-1 ? elem.getEndOffset()-1 :
									elem.getEndOffset();
			int caret = textArea.getCaretPosition();
			int caretOffset = caret - start;
			String text = doc.getText(start, end-start);
			if (line==lineCount-1) {
				start--; // Remove previous line's ending \n
			}
			doc.remove(start, end-start);
			Element elem2 = root.getElement(line-1);
			int start2 = elem2.getStartOffset();
			//int end2 = elem2.getEndOffset();
			if (line==lineCount-1) {
				text += '\n';
			}
			doc.insertString(start2, text, null);
			//caretOffset = Math.min(start2+caretOffset, end2-1);
			textArea.setCaretPosition(start2+caretOffset);
		}

	}


	/**
	 * Action to make the selection lower-case.
	 */
	public static class LowerSelectionCaseAction extends RecordableTextAction {

		public LowerSelectionCaseAction() {
			super(rtaLowerSelectionCaseAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			String selection = textArea.getSelectedText();
			if (selection!=null)
				textArea.replaceSelection(selection.toLowerCase());
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action that moves the caret to the next (or previous) bookmark.
	 */
	public static class NextBookmarkAction extends RecordableTextAction {

		private boolean forward;

		public NextBookmarkAction(String name, boolean forward) {
			super(name);
			this.forward = forward;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			Gutter gutter = RSyntaxUtilities.getGutter(textArea);
			if (gutter!=null) {

				try {

					GutterIconInfo[] bookmarks = gutter.getBookmarks();
					if (bookmarks.length==0) {
						UIManager.getLookAndFeel().
									provideErrorFeedback(textArea);
						return;
					}

					GutterIconInfo moveTo = null;
					int curLine = textArea.getCaretLineNumber();

					if (forward) {
						for (int i=0; i<bookmarks.length; i++) {
							GutterIconInfo bookmark = bookmarks[i];
							int offs = bookmark.getMarkedOffset();
							int line = textArea.getLineOfOffset(offs);
							if (line>curLine) {
								moveTo = bookmark;
								break;
							}
						}
						if (moveTo==null) { // Loop back to beginning
							moveTo = bookmarks[0];
						}
					}
					else {
						for (int i=bookmarks.length-1; i>=0; i--) {
							GutterIconInfo bookmark = bookmarks[i];
							int offs = bookmark.getMarkedOffset();
							int line = textArea.getLineOfOffset(offs);
							if (line<curLine) {
								moveTo = bookmark;
								break;
							}
						}
						if (moveTo==null) { // Loop back to end
							moveTo = bookmarks[bookmarks.length-1];
						}
					}

					int offs = moveTo.getMarkedOffset();
					if (textArea instanceof RSyntaxTextArea) {
						RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
						if (rsta.isCodeFoldingEnabled()) {
							rsta.getFoldManager().
										ensureOffsetNotInClosedFold(offs);
						}
					}
					int line = textArea.getLineOfOffset(offs);
					offs = textArea.getLineStartOffset(line);
					textArea.setCaretPosition(offs);

				} catch (BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().
								provideErrorFeedback(textArea);
					ble.printStackTrace();
				}
			}

		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Selects the next occurrence of the text last selected.
	 */
	public static class NextOccurrenceAction extends RecordableTextAction {

		public NextOccurrenceAction(String name) {
			super(name);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			String selectedText = textArea.getSelectedText();
			if (selectedText == null || selectedText.length() == 0) {
				selectedText = RTextArea.getSelectedOccurrenceText();
				if (selectedText == null || selectedText.length() == 0) {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					return;
				}
			}
			SearchContext context = new SearchContext(selectedText);
			if (!SearchEngine.find(textArea, context)) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
			RTextArea.setSelectedOccurrenceText(selectedText);
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action to move the selection and/or caret. Constructor indicates
	 * direction to use.
	 */
	public static class NextVisualPositionAction extends RecordableTextAction {

		private boolean select;
		private int direction;

		public NextVisualPositionAction(String nm, boolean select, int dir) {
			super(nm);
			this.select = select;
			this.direction = dir;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			Caret caret = textArea.getCaret();
			int dot = caret.getDot();

			/*
			 * Move to the beginning/end of selection on a "non-shifted"
			 * left- or right-keypress.  We shouldn't have to worry about
			 * navigation filters as, if one is being used, it let us get
			 * to that position before.
			 */
			if (!select) {
				switch (direction) {
					case SwingConstants.EAST:
						int mark = caret.getMark();
						if (dot!=mark) {
							caret.setDot(Math.max(dot, mark));
							return;
						}
						break;
					case SwingConstants.WEST:
						mark = caret.getMark();
						if (dot!=mark) {
							caret.setDot(Math.min(dot, mark));
							return;
						}
						break;
					default:
				}
			}

			Position.Bias[] bias = new Position.Bias[1];
			Point magicPosition = caret.getMagicCaretPosition();

			try {

				if(magicPosition == null &&
					(direction == SwingConstants.NORTH ||
					direction == SwingConstants.SOUTH)) {
					Rectangle r = textArea.modelToView(dot);
					magicPosition = new Point(r.x, r.y);
				}

				NavigationFilter filter = textArea.getNavigationFilter();

				if (filter != null) {
					dot = filter.getNextVisualPositionFrom(textArea, dot,
								Position.Bias.Forward, direction, bias);
				}
				else {
					dot = textArea.getUI().getNextVisualPositionFrom(
								textArea, dot,
								Position.Bias.Forward, direction, bias);
				}
				if (select)
					caret.moveDot(dot);
				else
					caret.setDot(dot);

				if(magicPosition != null &&
					(direction == SwingConstants.NORTH ||
					direction == SwingConstants.SOUTH)) {
						caret.setMagicCaretPosition(magicPosition);
				}

			} catch (BadLocationException ble) {
				ble.printStackTrace();
			}

		}

		public final String getMacroID() {
			return getName();
		}

    }


	/**
	 * Positions the caret at the next word.
	 */
	public static class NextWordAction extends RecordableTextAction {

 		private boolean select;

		public NextWordAction(String name, boolean select) {
			super(name);
			this.select = select;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			int offs = textArea.getCaretPosition();
			int oldOffs = offs;
			Element curPara = Utilities.getParagraphElement(textArea, offs);

			try {
				offs = getNextWord(textArea, offs);
				if(offs >= curPara.getEndOffset() &&
							oldOffs != curPara.getEndOffset() - 1) {
					// we should first move to the end of current paragraph
					// http://bugs.sun.com/view_bug.do?bug_id=4278839
					offs = curPara.getEndOffset() - 1;
				}
			} catch (BadLocationException ble) {
				int end = textArea.getDocument().getLength();
				if (offs != end) {
					if(oldOffs != curPara.getEndOffset() - 1)
						offs = curPara.getEndOffset() - 1;
					else
						offs = end;
				}
			}

			if (select)
				textArea.moveCaretPosition(offs);
			else
				textArea.setCaretPosition(offs);

		}

		public final String getMacroID() {
			return getName();
		}

		protected int getNextWord(RTextArea textArea, int offs)
									throws BadLocationException {
			return Utilities.getNextWord(textArea, offs);
		}

	}


	/**
	 * Pages one view to the left or right.
	 */
	static class PageAction extends RecordableTextAction {

 
		private boolean select;
		private boolean left;

		public PageAction(String name, boolean left, boolean select) {
			super(name);
			this.select = select;
			this.left = left;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			int selectedIndex;
			Rectangle visible = new Rectangle();
			textArea.computeVisibleRect(visible);
			if (left)
				visible.x = Math.max(0, visible.x - visible.width);
			else
				visible.x += visible.width;
		
			selectedIndex = textArea.getCaretPosition();
			if(selectedIndex != -1) {
				if (left) {
					selectedIndex = textArea.viewToModel(
									new Point(visible.x, visible.y));
				}
				else {
					selectedIndex = textArea.viewToModel(
							new Point(visible.x + visible.width - 1,
									visible.y + visible.height - 1));
				}
				Document doc = textArea.getDocument();
				if ((selectedIndex != 0) && 
					(selectedIndex  > (doc.getLength()-1))) {
					selectedIndex = doc.getLength()-1;
				}
				else if(selectedIndex  < 0) {
					selectedIndex = 0;
				}
				if (select)
					textArea.moveCaretPosition(selectedIndex);
				else
					textArea.setCaretPosition(selectedIndex);
			}

		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action for pasting text.
	 */
	public static class PasteAction extends RecordableTextAction {

 
		public PasteAction() {
			super(DefaultEditorKit.pasteAction);
		}

		public PasteAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			textArea.paste();
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return DefaultEditorKit.pasteAction;
		}

	}


	/**
	 * "Plays back" the last macro recorded.
	 */
	public static class PlaybackLastMacroAction extends RecordableTextAction {

 
		public PlaybackLastMacroAction() {
			super(rtaPlaybackLastMacroAction);
		}

		public PlaybackLastMacroAction(String name, Icon icon,
					String desc, Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			textArea.playbackLastMacro();
		}

		public boolean isRecordable() {
			return false; // Don't record macro playbacks.
		}

		public final String getMacroID() {
			return rtaPlaybackLastMacroAction;
		}

	}


	/**
	 * Select the previous occurrence of the text last selected.
	 */
	public static class PreviousOccurrenceAction extends RecordableTextAction {

		public PreviousOccurrenceAction(String name) {
			super(name);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			String selectedText = textArea.getSelectedText();
			if (selectedText == null || selectedText.length() == 0) {
				selectedText = RTextArea.getSelectedOccurrenceText();
				if (selectedText == null || selectedText.length() == 0) {
					UIManager.getLookAndFeel().provideErrorFeedback(textArea);
					return;
				}
			}
			SearchContext context = new SearchContext(selectedText);
			context.setSearchForward(false);
			if (!SearchEngine.find(textArea, context)) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			}
			RTextArea.setSelectedOccurrenceText(selectedText);
		}

		public final String getMacroID() {
			return getName();
		}

	}


    /**
     * Positions the caret at the beginning of the previous word.
     */
    public static class PreviousWordAction extends RecordableTextAction {

 		private boolean select;

		public PreviousWordAction(String name, boolean select) {
			super(name);
			this.select = select;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			int offs = textArea.getCaretPosition();
			boolean failed = false;
			try {

				Element curPara = Utilities.getParagraphElement(textArea, offs);
				offs = getPreviousWord(textArea, offs);
				if(offs < curPara.getStartOffset()) {
					offs = Utilities.getParagraphElement(textArea, offs).
												getEndOffset() - 1;
				}

			} catch (BadLocationException bl) {
				if (offs != 0)
					offs = 0;
				else
					failed = true;
			}

			if (!failed) {
				if (select)
					textArea.moveCaretPosition(offs);
				else
					textArea.setCaretPosition(offs);
			}
			else
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);

		}

		public final String getMacroID() {
			return getName();
		}

		protected int getPreviousWord(RTextArea textArea, int offs)
										throws BadLocationException {
			return Utilities.getPreviousWord(textArea, offs);
		}

	}


	/**
	 * Re-does the last action undone.
	 */
	public static class RedoAction extends RecordableTextAction {

 
		public RedoAction() {
			super(rtaRedoAction);
		}

		public RedoAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (textArea.isEnabled() && textArea.isEditable()) {
				textArea.redoLastAction();
				textArea.requestFocusInWindow();
			}
		}

		public final String getMacroID() {
			return rtaRedoAction;
		}

	}


	/**
	 * Scrolls the text area one line up or down, without changing
	 * the caret position.
	 */
	public static class ScrollAction extends RecordableTextAction {

		private int delta;

		public ScrollAction(String name, int delta) {
			super(name);
			this.delta = delta;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			Container parent = textArea.getParent();
			if (parent instanceof JViewport) {
				JViewport viewport = (JViewport)parent;
				Point p = viewport.getViewPosition();
				p.y += delta*textArea.getLineHeight();
				if (p.y<0) {
					p.y = 0;
				}
				else {
					Rectangle viewRect = viewport.getViewRect();
					int visibleEnd = p.y + viewRect.height;
					if (visibleEnd>=textArea.getHeight()) {
						p.y = textArea.getHeight() - viewRect.height;
					}
				}
				viewport.setViewPosition(p);
			}
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Selects the entire document.
	 */
	public static class SelectAllAction extends RecordableTextAction {

 
		public SelectAllAction() {
			super(selectAllAction);
		}

		public SelectAllAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			Document doc = textArea.getDocument();
			textArea.setCaretPosition(0);
			textArea.moveCaretPosition(doc.getLength());
		}

		public final String getMacroID() {
			return DefaultEditorKit.selectAllAction;
		}

	}


	/**
	 * Selects the line around the caret.
	 */
	public static class SelectLineAction extends RecordableTextAction {

 		private Action start;
		private Action end;

		public SelectLineAction() {
			super(selectLineAction);
			start = new BeginLineAction("pigdog", false);
			end = new EndLineAction("pigdog", true);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			start.actionPerformed(e);
			end.actionPerformed(e);
		}

		public final String getMacroID() {
			return DefaultEditorKit.selectLineAction;
		}

	}


	/**
	 * Selects the word around the caret.
	 */
	public static class SelectWordAction extends RecordableTextAction {

 		protected Action start;
		protected Action end;

		public SelectWordAction() {
			super(selectWordAction);
			createActions();
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			start.actionPerformed(e);
			end.actionPerformed(e);
		}

		protected void createActions() {
			start = new BeginWordAction("pigdog", false);
			end = new EndWordAction("pigdog", true);
		}

		public final String getMacroID() {
			return DefaultEditorKit.selectWordAction;
		}

	}


	/**
	 * Puts the text area into read-only mode.
	 */
	public static class SetReadOnlyAction extends RecordableTextAction {

 
		public SetReadOnlyAction() {
			super(readOnlyAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			textArea.setEditable(false);
		}

		public final String getMacroID() {
			return DefaultEditorKit.readOnlyAction;
		}

		public boolean isRecordable() {
			return false; // Why would you want to record this?
		}

	}


	/**
	 * Puts the text area into writable (from read-only) mode.
	 */
	public static class SetWritableAction extends RecordableTextAction {

 
		public SetWritableAction() {
			super(writableAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			textArea.setEditable(true);
		}

		public final String getMacroID() {
			return DefaultEditorKit.writableAction;
		}

		public boolean isRecordable() {
			return false; // Why would you want to record this?
		}

	}


	/**
	 * The action for inserting a time/date stamp.
	 */
	public static class TimeDateAction extends RecordableTextAction {

 
		public TimeDateAction() {
			super(rtaTimeDateAction);
		}

		public TimeDateAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			Date today = new Date();
			DateFormat timeDateStamp = DateFormat.getDateTimeInstance();
			String dateString = timeDateStamp.format(today);
			textArea.replaceSelection(dateString);
		}

		public final String getMacroID() {
			return rtaTimeDateAction;
		}

	}


	/**
	 * Toggles whether the current line has a bookmark.
	 */
	public static class ToggleBookmarkAction extends RecordableTextAction {
 
		public ToggleBookmarkAction() {
			super(rtaToggleBookmarkAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			Gutter gutter = RSyntaxUtilities.getGutter(textArea);
			if (gutter!=null) {
				int line = textArea.getCaretLineNumber();
				try {
					gutter.toggleBookmark(line);
				} catch (BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().
								provideErrorFeedback(textArea);
					ble.printStackTrace();
				}
			}
		}

		public final String getMacroID() {
			return rtaToggleBookmarkAction;
		}

	}


	/**
	 * The action for the insert key toggling insert/overwrite modes.
	 */
	public static class ToggleTextModeAction extends RecordableTextAction {
 
		public ToggleTextModeAction() {
			super(rtaToggleTextModeAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			int textMode = textArea.getTextMode();
			if (textMode==RTextArea.INSERT_MODE)
				textArea.setTextMode(RTextArea.OVERWRITE_MODE);
			else
				textArea.setTextMode(RTextArea.INSERT_MODE);
		}

		public final String getMacroID() {
			return rtaToggleTextModeAction;
		}

	}


	/**
	 * Undoes the last action done.
	 */
	public static class UndoAction extends RecordableTextAction {

 		public UndoAction() {
			super(rtaUndoAction);
		}

		public UndoAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (textArea.isEnabled() && textArea.isEditable()) {
				textArea.undoLastAction();
				textArea.requestFocusInWindow();
			}
		}

		public final String getMacroID() {
			return rtaUndoAction;
		}

	}


	/**
	 * Removes the selection, if any.
	 */
	public static class UnselectAction extends RecordableTextAction {

 
		public UnselectAction() {
			super(rtaUnselectAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			textArea.setCaretPosition(textArea.getCaretPosition());
		}

		public final String getMacroID() {
			return rtaUnselectAction;
		}

	}

 
	/**
	 * Action to make the selection upper-case.
	 */
	public static class UpperSelectionCaseAction extends RecordableTextAction {

 
		public UpperSelectionCaseAction() {
			super(rtaUpperSelectionCaseAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}
			String selection = textArea.getSelectedText();
			if (selection!=null)
				textArea.replaceSelection(selection.toUpperCase());
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Scrolls up/down vertically.  The select version of this action extends
	 * the selection, instead of simply moving the caret.
	 */
	public static class VerticalPageAction extends RecordableTextAction {

		private boolean select;
		private int direction;

		public VerticalPageAction(String name, int direction, boolean select) {
			super(name);
			this.select = select;
			this.direction = direction;
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			Rectangle visible = textArea.getVisibleRect();
			Rectangle newVis = new Rectangle(visible);
			int selectedIndex = textArea.getCaretPosition();
			int scrollAmount = textArea.getScrollableBlockIncrement(
							visible, SwingConstants.VERTICAL, direction); 
			int initialY = visible.y;
			Caret caret = textArea.getCaret();
			Point magicPosition = caret.getMagicCaretPosition();
			int yOffset;   

			if (selectedIndex!=-1) {

				try {

					Rectangle dotBounds = textArea.modelToView(selectedIndex);
					int x = (magicPosition != null) ? magicPosition.x :
												dotBounds.x;
					int h = dotBounds.height;
					yOffset = direction *
							((int)Math.ceil(scrollAmount/(double)h)-1)*h; 
					newVis.y = constrainY(textArea, initialY+yOffset, yOffset, visible.height);                        
					int newIndex;

					if (visible.contains(dotBounds.x, dotBounds.y)) {
						// Dot is currently visible, base the new
						// location off the old, or
						newIndex = textArea.viewToModel(
									new Point(x, constrainY(textArea,
										dotBounds.y + yOffset, 0, 0)));
										}
					else {
						// Dot isn't visible, choose the top or the bottom
						// for the new location.
						if (direction == -1) {
							newIndex = textArea.viewToModel(new Point(
													x, newVis.y));
						}
						else {
							newIndex = textArea.viewToModel(new Point(
									x, newVis.y + visible.height));
						}
					}
					newIndex = constrainOffset(textArea, newIndex);
					if (newIndex != selectedIndex) {
						// Make sure the new visible location contains
						// the location of dot, otherwise Caret will
						// cause an additional scroll.
						adjustScrollIfNecessary(textArea, newVis, initialY,
											newIndex);
						if (select)
							textArea.moveCaretPosition(newIndex);
						else
							textArea.setCaretPosition(newIndex);
					}

				} catch (BadLocationException ble) { }

			} // End of if (selectedIndex!=-1).

			else {
				yOffset = direction * scrollAmount;
				newVis.y = constrainY(textArea, initialY + yOffset, yOffset, visible.height);
			}

			if (magicPosition != null)
				caret.setMagicCaretPosition(magicPosition);

			textArea.scrollRectToVisible(newVis);
		}

		private int constrainY(JTextComponent textArea, int y, int vis, int screenHeight) {
			if (y < 0)
				y = 0;
			else if (y + vis > textArea.getHeight()) {
				//y = Math.max(0, textArea.getHeight() - vis);
				y = Math.max(0, textArea.getHeight()-screenHeight);
			}
			return y;
		}

		private int constrainOffset(JTextComponent text, int offset) {
			Document doc = text.getDocument();
			if ((offset != 0) && (offset > doc.getLength()))
				offset = doc.getLength();
			if (offset  < 0)
				offset = 0;
			return offset;
		}

		private void adjustScrollIfNecessary(JTextComponent text,
									Rectangle visible, int initialY,
									int index) {
			try {
				Rectangle dotBounds = text.modelToView(index);
	                if (dotBounds.y < visible.y ||
					(dotBounds.y > (visible.y + visible.height)) ||
					(dotBounds.y + dotBounds.height) >
					(visible.y + visible.height)) {
					int y;
					if (dotBounds.y < visible.y)
						y = dotBounds.y;
					else
						y = dotBounds.y + dotBounds.height - visible.height;
					if ((direction == -1 && y < initialY) ||
						(direction == 1 && y > initialY))
						// Only adjust if won't cause scrolling upward.
						visible.y = y;
				}
			} catch (BadLocationException ble) {}
		}

		public final String getMacroID() {
			return getName();
		}

	}


}