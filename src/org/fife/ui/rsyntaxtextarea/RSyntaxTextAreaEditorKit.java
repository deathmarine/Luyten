/*
 * 08/29/2004
 *
 * RSyntaxTextAreaEditorKit.java - The editor kit used by RSyntaxTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.util.Stack;
import javax.swing.*;
import javax.swing.text.*;

import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldCollapser;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rsyntaxtextarea.templates.CodeTemplate;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.IconRowHeader;
import org.fife.ui.rtextarea.RecordableTextAction;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextAreaEditorKit;


/**
 * An extension of <code>RTextAreaEditorKit</code> that adds functionality for
 * programming-specific stuff.  There are currently subclasses to handle:
 *
 * <ul>
 *   <li>Toggling code folds.</li>
 *   <li>Aligning "closing" curly braces with their matches, if the current
 *       programming language uses curly braces to identify code blocks.</li>
 *   <li>Copying the current selection as RTF.</li>
 *   <li>Block indentation (increasing the indent of one or multiple lines)</li>
 *   <li>Block un-indentation (decreasing the indent of one or multiple lines)
 *       </li>
 *   <li>Inserting a "code template" when a configurable key (e.g. a space) is
 *       pressed</li>
 *   <li>Decreasing the point size of all fonts in the text area</li>
 *   <li>Increasing the point size of all fonts in the text area</li>
 *   <li>Moving the caret to the "matching bracket" of the one at the current
 *       caret position</li>
 *   <li>Toggling whether the currently selected lines are commented out.</li>
 *   <li>Better selection of "words" on mouse double-clicks for programming
 *       languages.</li>
 *   <li>Better keyboard navigation via Ctrl+arrow keys for programming
 *       languages.</li>
 * </ul>
 *
 * @author Robert Futrell
 * @version 0.5
 */
public class RSyntaxTextAreaEditorKit extends RTextAreaEditorKit {

	private static final long serialVersionUID = 1L;

	public static final String rstaCloseCurlyBraceAction	= "RSTA.CloseCurlyBraceAction";
	public static final String rstaCloseMarkupTagAction		= "RSTA.CloseMarkupTagAction";
	public static final String rstaCollapseAllFoldsAction	= "RSTA.CollapseAllFoldsAction";
	public static final String rstaCollapseAllCommentFoldsAction = "RSTA.CollapseAllCommentFoldsAction";
	public static final String rstaCollapseFoldAction		= "RSTA.CollapseFoldAction";
	public static final String rstaCopyAsRtfAction			= "RSTA.CopyAsRtfAction";
	public static final String rstaDecreaseIndentAction		= "RSTA.DecreaseIndentAction";
	public static final String rstaExpandAllFoldsAction		= "RSTA.ExpandAllFoldsAction";
	public static final String rstaExpandFoldAction			= "RSTA.ExpandFoldAction";
	public static final String rstaGoToMatchingBracketAction	= "RSTA.GoToMatchingBracketAction";
	public static final String rstaPossiblyInsertTemplateAction = "RSTA.TemplateAction";
	public static final String rstaToggleCommentAction 		= "RSTA.ToggleCommentAction";
	public static final String rstaToggleCurrentFoldAction	= "RSTA.ToggleCurrentFoldAction";

	private static final String MSG	= "org.fife.ui.rsyntaxtextarea.RSyntaxTextArea";
	private static final ResourceBundle msg = ResourceBundle.getBundle(MSG);


	/**
	 * The actions that <code>RSyntaxTextAreaEditorKit</code> adds to those of
	 * <code>RTextAreaEditorKit</code>.
	 */
	private static final Action[] defaultActions = {
		new CloseCurlyBraceAction(),
		new CloseMarkupTagAction(),
		new BeginWordAction(beginWordAction, false),
		new BeginWordAction(selectionBeginWordAction, true),
		new ChangeFoldStateAction(rstaCollapseFoldAction, true),
		new ChangeFoldStateAction(rstaExpandFoldAction, false),
		new CollapseAllFoldsAction(),
		new CopyAsRtfAction(),
		//new DecreaseFontSizeAction(),
		new DecreaseIndentAction(),
		new DeletePrevWordAction(),
		new EndAction(endAction, false),
		new EndAction(selectionEndAction, true),
		new EndWordAction(endWordAction, false),
		new EndWordAction(endWordAction, true),
		new ExpandAllFoldsAction(),
		new GoToMatchingBracketAction(),
		new InsertBreakAction(),
		//new IncreaseFontSizeAction(),
		new InsertTabAction(),
		new NextWordAction(nextWordAction, false),
		new NextWordAction(selectionNextWordAction, true),
		new PossiblyInsertTemplateAction(),
		new PreviousWordAction(previousWordAction, false),  
		new PreviousWordAction(selectionPreviousWordAction, true),
		new SelectWordAction(),
		new ToggleCommentAction(),
	};


	/**
	 * Constructor.
	 */
	public RSyntaxTextAreaEditorKit() {
	}


	/**
	 * Returns the default document used by <code>RSyntaxTextArea</code>s.
	 *
	 * @return The document.
	 */
	public Document createDefaultDocument() {
		return new RSyntaxDocument(SyntaxConstants.SYNTAX_STYLE_NONE);
	}


	/**
	 * Overridden to return a row header that is aware of folding.
	 *
	 * @param textArea The text area.
	 * @return The icon row header.
	 */
	public IconRowHeader createIconRowHeader(RTextArea textArea) {
		return new FoldingAwareIconRowHeader((RSyntaxTextArea)textArea);
	}


	/**
	 * Fetches the set of commands that can be used
	 * on a text component that is using a model and
	 * view produced by this kit.
	 *
	 * @return the command list
	 */ 
	public Action[] getActions() {
		return TextAction.augmentList(super.getActions(),
							RSyntaxTextAreaEditorKit.defaultActions);
	}


	/**
	 * Returns localized text for an action.  There's definitely a better place
	 * for this functionality.
	 *
	 * @param key The key into the action resource bundle.
	 * @return The localized text.
	 */
	public static String getString(String key) {
		return msg.getString(key);
	}


	/**
	 * Positions the caret at the beginning of the word.  This class is here
	 * to better handle finding the "beginning of the word" for programming
	 * languages.
	 */
	protected static class BeginWordAction
							extends RTextAreaEditorKit.BeginWordAction {

		private Segment seg;

		protected BeginWordAction(String name, boolean select) {
			super(name, select);
			seg = new Segment();
		}

		protected int getWordStart(RTextArea textArea, int offs)
										throws BadLocationException {

			if (offs==0) {
				return offs;
			}

			RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
			int line = textArea.getLineOfOffset(offs);
			int start = textArea.getLineStartOffset(line);
			if (offs==start) {
				return start;
			}
			int end = textArea.getLineEndOffset(line);
			if (line!=textArea.getLineCount()-1) {
				end--;
			}
			doc.getText(start, end-start, seg);

			// Determine the "type" of char at offs - lower case, upper case,
			// whitespace or other.  We take special care here as we're starting
			// in the middle of the Segment to check whether we're already at
			// the "beginning" of a word.
			int firstIndex = seg.getBeginIndex() + (offs-start) - 1;
			seg.setIndex(firstIndex);
			char ch = seg.current();
			char nextCh = offs==end ? 0 : seg.array[seg.getIndex() + 1];

			// The "word" is a group of letters and/or digits
			if (Character.isLetterOrDigit(ch)) {
				if (offs!=end && !Character.isLetterOrDigit(nextCh)) {
					return offs;
				}
				do {
					ch = seg.previous();
				} while (Character.isLetterOrDigit(ch));
			}

			// The "word" is whitespace
			else if (Character.isWhitespace(ch)) {
				if (offs!=end && !Character.isWhitespace(nextCh)) {
					return offs;
				}
				do {
					ch = seg.previous();
				} while (Character.isWhitespace(ch));
			}

			// Otherwise, the "word" a single "something else" char (operator,
			// etc.).

			offs -= firstIndex - seg.getIndex() + 1;//seg.getEndIndex() - seg.getIndex();
			if (ch!=Segment.DONE && nextCh!='\n') {
				offs++;
			}

			return offs;

		}

	}


	/**
	 * Expands or collapses the nearest fold.
	 */
	public static class ChangeFoldStateAction extends FoldRelatedAction {

		private boolean collapse;

		public ChangeFoldStateAction(String name, boolean collapse) {
			super(name);
			this.collapse = collapse;
		}

		public ChangeFoldStateAction(String name, Icon icon,
				String desc, Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			if (rsta.isCodeFoldingEnabled()) {
				Fold fold = getClosestFold(rsta);
				if (fold!=null) {
					fold.setCollapsed(collapse);
				}
				possiblyRepaintGutter(textArea);
			}
			else {
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
			}
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action that (optionally) aligns a closing curly brace with the line
	 * containing its matching opening curly brace.
	 */
	public static class CloseCurlyBraceAction extends RecordableTextAction {
		
		private static final long serialVersionUID = 1L;

		private Segment seg;

		public CloseCurlyBraceAction() {
			super(rstaCloseCurlyBraceAction);
			seg = new Segment();
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			RSyntaxDocument doc = (RSyntaxDocument)rsta.getDocument();
			boolean alignCurlyBraces = rsta.isAutoIndentEnabled() &&
										doc.getCurlyBracesDenoteCodeBlocks();

			if (alignCurlyBraces) {
				textArea.beginAtomicEdit();
			}

			try {

				textArea.replaceSelection("}");

				// If the user wants to align curly braces...
				if (alignCurlyBraces) {

					Element root = doc.getDefaultRootElement();
					int dot = rsta.getCaretPosition() - 1; // Start before '{'
					int line = root.getElementIndex(dot);
					Element elem = root.getElement(line);
					int start = elem.getStartOffset();

					// Get the current line's text up to the '}' entered.
					try {
						doc.getText(start, dot-start, seg);
					} catch (BadLocationException ble) { // Never happens
						ble.printStackTrace();
						return;
					}

					// Only attempt to align if there's only whitespace up to
					// the '}' entered.
					for (int i=0; i<seg.count; i++) {
						char ch = seg.array[seg.offset+i];
						if (!Character.isWhitespace(ch)) {
							return;
						}
					}

					// Locate the matching '{' bracket, and replace the leading
					// whitespace for the '}' to match that of the '{' char's line.
					int match = RSyntaxUtilities.getMatchingBracketPosition(rsta);
					if (match>-1) {
						try {
							String ws = RSyntaxUtilities.getLeadingWhitespace(doc, match);
							rsta.replaceRange(ws, start, dot);
						} catch (BadLocationException ble) {
							ble.printStackTrace();
							return;
						}
					}

				}

			} finally {
				if (alignCurlyBraces) {
					textArea.endAtomicEdit();
				}
			}

		}

		public final String getMacroID() {
			return rstaCloseCurlyBraceAction;
		}

	}


	/**
	 * (Optionally) completes a closing markup tag.
	 */
	public static class CloseMarkupTagAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public CloseMarkupTagAction() {
			super(rstaCloseMarkupTagAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			RSyntaxDocument doc = (RSyntaxDocument)rsta.getDocument();

			Caret c = rsta.getCaret();
			boolean selection = c.getDot()!=c.getMark();
			rsta.replaceSelection("/");

			// Don't automatically complete a tag if there was a selection
			int dot = c.getDot();

			if (doc.getLanguageIsMarkup() && 
					doc.getCompleteMarkupCloseTags() &&
					!selection && rsta.getCloseMarkupTags() && dot>1) {

				try {

					// Check actual char before token type, since it's quicker
					char ch = doc.charAt(dot-2);
					if (ch=='<' || ch=='[') {

						Token t = doc.getTokenListForLine(
											rsta.getCaretLineNumber());
						t = RSyntaxUtilities.getTokenAtOffset(t, dot-1);
						if (t!=null && t.type==Token.MARKUP_TAG_DELIMITER) {
							//System.out.println("Huzzah - closing tag!");
							String tagName = discoverTagName(doc, dot);
							if (tagName!=null) {
								rsta.replaceSelection(tagName + (char)(ch+2));
							}
						}

					}

				} catch (BadLocationException ble) { // Never happens
					UIManager.getLookAndFeel().provideErrorFeedback(rsta);
					ble.printStackTrace();
				}

			}

		}

		/**
		 * Discovers the name of the tag being closed.  Assumes standard
		 * SGML-style markup tags.
		 *
		 * @param doc The document to parse.
		 * @param dot The location of the caret.  This should be right after
		 *        the start of a closing tag token (e.g. "<code>&lt;/</code>"
		 *        or "<code>[</code>" in the case of BBCode).
		 * @return The name of the tag to close, or <code>null</code> if it
		 *         could not be determined.
		 */
		private String discoverTagName(RSyntaxDocument doc, int dot) {

			Stack stack = new Stack();

			Element root = doc.getDefaultRootElement();
			int curLine = root.getElementIndex(dot);

			for (int i=0; i<=curLine; i++) {

				Token t = doc.getTokenListForLine(i);
				while (t!=null && t.isPaintable()) {

					if (t.type==Token.MARKUP_TAG_DELIMITER) {
						if (t.isSingleChar('<') || t.isSingleChar('[')) {
							t = t.getNextToken();
							while (t!=null && t.isPaintable()) {
								if (t.type==Token.MARKUP_TAG_NAME ||
										// Being lenient here and also checking
										// for attributes, in case they
										// (incorrectly) have whitespace between
										// the '<' char and the element name.
										t.type==Token.MARKUP_TAG_ATTRIBUTE) {
									stack.push(t.getLexeme());
									break;
								}
								t = t.getNextToken();
							}
						}
						else if (t.textCount==2 && t.text[t.textOffset]=='/' &&
								(t.text[t.textOffset+1]=='>' ||
										t.text[t.textOffset+1]==']')) {
							if (!stack.isEmpty()) { // Always true for valid XML
								stack.pop();
							}
						}
						else if (t.textCount==2 && 
								(t.text[t.textOffset]=='<' || t.text[t.textOffset]=='[') &&
								t.text[t.textOffset+1]=='/') {
							String tagName = null;
							if (!stack.isEmpty()) { // Always true for valid XML
								tagName = (String)stack.pop();
							}
							if (t.offset+t.textCount>=dot) {
								return tagName;
							}
						}
					}

					t = t.getNextToken();

				}

			}

			return null; // Should never happen

		}

		public String getMacroID() {
			return getName();
		}

	}


	/**
	 * Collapses all comment folds.
	 */
	public static class CollapseAllCommentFoldsAction extends FoldRelatedAction{

		private static final long serialVersionUID = 1L;

		public CollapseAllCommentFoldsAction() {
			super(rstaCollapseAllCommentFoldsAction);
			setProperties(msg, "Action.CollapseCommentFolds");
		}

		public CollapseAllCommentFoldsAction(String name, Icon icon,
				String desc, Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			if (rsta.isCodeFoldingEnabled()) {
				FoldCollapser collapser = new FoldCollapser();
				collapser.collapseFolds(rsta.getFoldManager());
				possiblyRepaintGutter(textArea);
			}
			else {
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
			}
		}

		public final String getMacroID() {
			return rstaCollapseAllCommentFoldsAction;
		}

	}


	/**
	 * Collapses all folds.
	 */
	public static class CollapseAllFoldsAction extends FoldRelatedAction {

		private static final long serialVersionUID = 1L;

		public CollapseAllFoldsAction() {
			this(false);
		}

		public CollapseAllFoldsAction(boolean localizedName) {
			super(rstaCollapseAllFoldsAction);
			if (localizedName) {
				setProperties(msg, "Action.CollapseAllFolds");
			}
		}

		public CollapseAllFoldsAction(String name, Icon icon,
				String desc, Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			if (rsta.isCodeFoldingEnabled()) {
				FoldCollapser collapser = new FoldCollapser() {
					public boolean getShouldCollapse(Fold fold) {
						return true;
					}
				};
				collapser.collapseFolds(rsta.getFoldManager());
				possiblyRepaintGutter(textArea);
			}
			else {
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
			}
		}

		public final String getMacroID() {
			return rstaCollapseAllFoldsAction;
		}

	}


	/**
	 * Action for copying text as RTF.
	 */
	public static class CopyAsRtfAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public CopyAsRtfAction() {
			super(rstaCopyAsRtfAction);
		}

		public CopyAsRtfAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			((RSyntaxTextArea)textArea).copyAsRtf();
			textArea.requestFocusInWindow();
		}

		public final String getMacroID() {
			return getName();
		}

	}


	/**
	 * Action for decreasing the font size of all fonts in the text area.
	 */
	public static class DecreaseFontSizeAction
					extends RTextAreaEditorKit.DecreaseFontSizeAction {

		private static final long serialVersionUID = 1L;

		public DecreaseFontSizeAction() {
			super();
		}

		public DecreaseFontSizeAction(String name, Icon icon, String desc,
							Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			SyntaxScheme scheme = rsta.getSyntaxScheme();

			// All we need to do is update all of the fonts in syntax
			// schemes, then call setSyntaxHighlightingColorScheme with the
			// same scheme already being used.  This relies on the fact that
			// that method does not check whether the new scheme is different
			// from the old scheme before updating.

			boolean changed = false;
			int count = scheme.getStyleCount();
			for (int i=0; i<count; i++) {
				Style ss = scheme.getStyle(i);
				if (ss!=null) {
					Font font = ss.font;
					if (font!=null) {
						float oldSize = font.getSize2D();
						float newSize = oldSize - decreaseAmount;
						if (newSize>=MINIMUM_SIZE) {
							// Shrink by decreaseAmount.
							ss.font = font.deriveFont(newSize);
							changed = true;
						}
						else if (oldSize>MINIMUM_SIZE) {
							// Can't shrink by full decreaseAmount, but
							// can shrink a little bit.
							ss.font = font.deriveFont(MINIMUM_SIZE);
							changed = true;
						}
					}
				}
			}

			// Do the text area's font also.
			Font font = rsta.getFont();
			float oldSize = font.getSize2D();
			float newSize = oldSize - decreaseAmount;
			if (newSize>=MINIMUM_SIZE) {
				// Shrink by decreaseAmount.
				rsta.setFont(font.deriveFont(newSize));
				changed = true;
			}
			else if (oldSize>MINIMUM_SIZE) {
				// Can't shrink by full decreaseAmount, but
				// can shrink a little bit.
				rsta.setFont(font.deriveFont(MINIMUM_SIZE));
				changed = true;
			}

			// If we updated at least one font, update the screen.  If
			// all of the fonts were already the minimum size, beep.
			if (changed) {
				rsta.setSyntaxScheme(scheme);
				// NOTE:  This is a hack to get an encompassing
				// RTextScrollPane to repaint its line numbers to account
				// for a change in line height due to a font change.  I'm
				// not sure why we need to do this here but not when we
				// change the syntax highlighting color scheme via the
				// Options dialog... setSyntaxHighlightingColorScheme()
				// calls revalidate() which won't repaint the scroll pane
				// if scrollbars don't change, which is why we need this.
				Component parent = rsta.getParent();
				if (parent instanceof javax.swing.JViewport) {
					parent = parent.getParent();
					if (parent instanceof JScrollPane) {
						parent.repaint();
					}
				}
			}
			else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);

		}

	}


	/**
	 * Action for when un-indenting lines (either the current line if there is
	 * selection, or all selected lines if there is one).
	 */
	public static class DecreaseIndentAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;
	
		private Segment s;

		public DecreaseIndentAction() {
			this(rstaDecreaseIndentAction);
		}

		public DecreaseIndentAction(String name) {
			super(name);
			s = new Segment();
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			Document document = textArea.getDocument();
			Element map = document.getDefaultRootElement();
			Caret c = textArea.getCaret();
			int dot = c.getDot();
			int mark = c.getMark();
			int line1 = map.getElementIndex(dot);
			int tabSize = textArea.getTabSize();

			// If there is a selection, indent all lines in the selection.
			// Otherwise, indent the line the caret is on.
			if (dot!=mark) {
				// Note that we cheaply reuse variables here, so don't
				// take their names to mean what they are.
				int line2 = map.getElementIndex(mark);
				dot = Math.min(line1, line2);
				mark = Math.max(line1, line2);
				Element elem;
				try {
					for (line1=dot; line1<mark; line1++) {
						elem = map.getElement(line1);
						handleDecreaseIndent(elem, document, tabSize);
					}
					// Don't do the last line if the caret is at its
					// beginning.  We must call getDot() again and not just
					// use 'dot' as the caret's position may have changed
					// due to the insertion of the tabs above.
					elem = map.getElement(mark);
					int start = elem.getStartOffset();
					if (Math.max(c.getDot(),c.getMark())!=start) {
						handleDecreaseIndent(elem, document, tabSize);
					}
				} catch (BadLocationException ble) {
					ble.printStackTrace();
					UIManager.getLookAndFeel().
								provideErrorFeedback(textArea);
				}
			}
			else {
				Element elem = map.getElement(line1);
				try {
					handleDecreaseIndent(elem, document, tabSize);
				} catch (BadLocationException ble) {
					ble.printStackTrace();
					UIManager.getLookAndFeel().
								provideErrorFeedback(textArea);
				}
			}

		}

		public final String getMacroID() {
			return rstaDecreaseIndentAction;
		}

		/**
		 * Actually does the "de-indentation."  This method finds where the
		 * given element's leading whitespace ends, then, if there is indeed
		 * leading whitespace, removes either the last char in it (if it is a
		 * tab), or removes up to the number of spaces equal to a tab in the
		 * specified document (i.e., if the tab size was 5 and there were 3
		 * spaces at the end of the leading whitespace, the three will be
		 * removed; if there were 8 spaces, only the first 5 would be
		 * removed).
		 *
		 * @param elem The element to "de-indent."
		 * @param doc The document containing the specified element.
		 * @param tabSize The size of a tab, in spaces.
		 */
		private final void handleDecreaseIndent(Element elem, Document doc,
									int tabSize)
									throws BadLocationException {
			int start = elem.getStartOffset();
			int end = elem.getEndOffset() - 1; // Why always true??
			doc.getText(start,end-start, s);
			int i = s.offset;
			end = i+s.count;
			if (end>i) {
				// If the first character is a tab, remove it.
				if (s.array[i]=='\t') {
					doc.remove(start, 1);
				}
				// Otherwise, see if the first character is a space.  If it
				// is, remove all contiguous whitespaces at the beginning of
				// this line, up to the tab size.
				else if (s.array[i]==' ') {
					i++;
					int toRemove = 1;
					while (i<end && s.array[i]==' ' && toRemove<tabSize) {
						i++;
						toRemove++;
					}
					doc.remove(start, toRemove);
				}
			}
		}

	}


	/**
	 * Deletes the previous word, but differentiates symbols from "words" to
	 * match the behavior of code editors.
	 */
	public static class DeletePrevWordAction
			extends RTextAreaEditorKit.DeletePrevWordAction {

		private Segment seg = new Segment();

		protected int getPreviousWordStart(RTextArea textArea, int offs)
				throws BadLocationException {

			if (offs==0) {
				return offs;
			}

			RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
			int line = textArea.getLineOfOffset(offs);
			int start = textArea.getLineStartOffset(line);
			if (offs==start) {
				return start-1; // Just delete the newline
			}
			int end = textArea.getLineEndOffset(line);
			if (line!=textArea.getLineCount()-1) {
				end--;
			}
			doc.getText(start, end-start, seg);

			// Determine the "type" of char at offs - lower case, upper case,
			// whitespace or other.  We take special care here as we're starting
			// in the middle of the Segment to check whether we're already at
			// the "beginning" of a word.
			int firstIndex = seg.getBeginIndex() + (offs-start) - 1;
			seg.setIndex(firstIndex);
			char ch = seg.current();

			// Always strip off whitespace first
			if (Character.isWhitespace(ch)) {
				do {
					ch = seg.previous();
				} while (Character.isWhitespace(ch));
			}

			// The "word" is a group of letters and/or digits
			if (Character.isLetterOrDigit(ch)) {
				do {
					ch = seg.previous();
				} while (Character.isLetterOrDigit(ch));
			}

			// The "word" is a series of symbols.
			else {
				while (!Character.isWhitespace(ch) &&
						!Character.isLetterOrDigit(ch)
						&& ch!=Segment.DONE) {
					ch = seg.previous();
				}
			}

			if (ch==Segment.DONE) {
				return start; // Removed last "token" of the line
			}
			offs -= firstIndex - seg.getIndex();
			return offs;

		}

	}


	/**
	 * Moves the caret to the end of the document, taking into account code
	 * folding.
	 */
	public static class EndAction extends RTextAreaEditorKit.EndAction {

		public EndAction(String name, boolean select) {
			super(name, select);
		}

		protected int getVisibleEnd(RTextArea textArea) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			return rsta.getLastVisibleOffset();
		}

	}


	/**
	 * Positions the caret at the end of the word.  This class is here to
	 * better handle finding the "end of the word" in programming languages.
	 */
	protected static class EndWordAction
							extends RTextAreaEditorKit.EndWordAction {

 		private Segment seg;

		protected EndWordAction(String name, boolean select) {
			super(name, select);
			seg = new Segment();
		}

		protected int getWordEnd(RTextArea textArea, int offs)
									throws BadLocationException {

			RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
			if (offs==doc.getLength()) {
				return offs;
			}

			int line = textArea.getLineOfOffset(offs);
			int end = textArea.getLineEndOffset(line);
			if (line!=textArea.getLineCount()-1) {
				end--; // Hide newline
			}
			if (offs==end) {
				return end;
			}
			doc.getText(offs, end-offs, seg);

			// Determine the "type" of char at offs - letter/digit,
			// whitespace or other
			char ch = seg.first();

			// The "word" is a group of letters and/or digits
			if (Character.isLetterOrDigit(ch)) {
				do {
					ch = seg.next();
				} while (Character.isLetterOrDigit(ch));
			}

			// The "word" is whitespace.
			else if (Character.isWhitespace(ch)) {

				do {
					ch = seg.next();
				} while (Character.isWhitespace(ch));
			}

			// Otherwise, the "word" is a single character of some other type
			// (operator, etc.).

			offs += seg.getIndex() - seg.getBeginIndex();
			return offs;

		}

	}


	/**
	 * Expands all folds.
	 */
	public static class ExpandAllFoldsAction extends FoldRelatedAction {

		private static final long serialVersionUID = 1L;

		public ExpandAllFoldsAction() {
			this(false);
		}

		public ExpandAllFoldsAction(boolean localizedName) {
			super(rstaExpandAllFoldsAction);
			if (localizedName) {
				setProperties(msg, "Action.ExpandAllFolds");
			}
		}

		public ExpandAllFoldsAction(String name, Icon icon,
				String desc, Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			if (rsta.isCodeFoldingEnabled()) {
				FoldManager fm = rsta.getFoldManager();
				for (int i=0; i<fm.getFoldCount(); i++) {
					expand(fm.getFold(i));
				}
				possiblyRepaintGutter(rsta);
			}
			else {
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
			}
		}

		private void expand(Fold fold) {
			fold.setCollapsed(false);
			for (int i=0; i<fold.getChildCount(); i++) {
				expand(fold.getChild(i));
			}
		}

		public final String getMacroID() {
			return rstaExpandAllFoldsAction;
		}

	}


	/**
	 * Base class for folding-related actions.
	 */
	static abstract class FoldRelatedAction extends RecordableTextAction {

		public FoldRelatedAction(String name) {
			super(name);
		}

		public FoldRelatedAction(String name, Icon icon,
				String desc, Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		protected Fold getClosestFold(RSyntaxTextArea textArea) {
			int offs = textArea.getCaretPosition();
			int line = textArea.getCaretLineNumber();
			FoldManager fm = textArea.getFoldManager();
			Fold fold = fm.getFoldForLine(line);
			if (fold==null) {
				fold = fm.getDeepestOpenFoldContaining(offs);
			}
			return fold;
		}

		/**
		 * Repaints the gutter in a text area's scroll pane, if necessary.
		 *
		 * @param textArea The text area.
		 */
		protected void possiblyRepaintGutter(RTextArea textArea) {
			Gutter gutter = RSyntaxUtilities.getGutter(textArea);
			if (gutter!=null) {
				gutter.repaint();
			}
		}

	}


	/**
	 * Action for moving the caret to the "matching bracket" of the bracket
	 * at the caret position (either before or after).
	 */
	public static class GoToMatchingBracketAction
									extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public GoToMatchingBracketAction() {
			super(rstaGoToMatchingBracketAction);
		}

		public GoToMatchingBracketAction(String name, Icon icon, String desc,
							Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			int pos = RSyntaxUtilities.getMatchingBracketPosition(rsta);
			if (pos>-1) {
				// Go to the position AFTER the bracket so the previous
				// bracket (which we were just on) is highlighted.
				rsta.setCaretPosition(pos+1);
			}
			else {
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
			}
		}

		public final String getMacroID() {
			return rstaGoToMatchingBracketAction;
		}

	}


	/**
	 * Action for increasing the font size of all fonts in the text area.
	 */
	public static class IncreaseFontSizeAction
					extends RTextAreaEditorKit.IncreaseFontSizeAction {

		private static final long serialVersionUID = 1L;

		public IncreaseFontSizeAction() {
			super();
		}

		public IncreaseFontSizeAction(String name, Icon icon, String desc,
							Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			SyntaxScheme scheme = rsta.getSyntaxScheme();

			// All we need to do is update all of the fonts in syntax
			// schemes, then call setSyntaxHighlightingColorScheme with the
			// same scheme already being used.  This relies on the fact that
			// that method does not check whether the new scheme is different
			// from the old scheme before updating.

			boolean changed = false;
			int count = scheme.getStyleCount();
			for (int i=0; i<count; i++) {
				Style ss = scheme.getStyle(i);
				if (ss!=null) {
					Font font = ss.font;
					if (font!=null) {
						float oldSize = font.getSize2D();
						float newSize = oldSize + increaseAmount;
						if (newSize<=MAXIMUM_SIZE) {
							// Grow by increaseAmount.
							ss.font = font.deriveFont(newSize);
							changed = true;
						}
						else if (oldSize<MAXIMUM_SIZE) {
							// Can't grow by full increaseAmount, but
							// can grow a little bit.
							ss.font = font.deriveFont(MAXIMUM_SIZE);
							changed = true;
						}
					}
				}
			}

			// Do the text area's font also.
			Font font = rsta.getFont();
			float oldSize = font.getSize2D();
			float newSize = oldSize + increaseAmount;
			if (newSize<=MAXIMUM_SIZE) {
				// Grow by increaseAmount.
				rsta.setFont(font.deriveFont(newSize));
				changed = true;
			}
			else if (oldSize<MAXIMUM_SIZE) {
				// Can't grow by full increaseAmount, but
				// can grow a little bit.
				rsta.setFont(font.deriveFont(MAXIMUM_SIZE));
				changed = true;
			}

			// If we updated at least one font, update the screen.  If
			// all of the fonts were already the minimum size, beep.
			if (changed) {
				rsta.setSyntaxScheme(scheme);
				// NOTE:  This is a hack to get an encompassing
				// RTextScrollPane to repaint its line numbers to account
				// for a change in line height due to a font change.  I'm
				// not sure why we need to do this here but not when we
				// change the syntax highlighting color scheme via the
				// Options dialog... setSyntaxHighlightingColorScheme()
				// calls revalidate() which won't repaint the scroll pane
				// if scrollbars don't change, which is why we need this.
				Component parent = rsta.getParent();
				if (parent instanceof javax.swing.JViewport) {
					parent = parent.getParent();
					if (parent instanceof JScrollPane) {
						parent.repaint();
					}
				}
			}
			else
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);

		}

	}


	/**
	 * Action for when the user presses the Enter key.  This is here so we can
	 * be smart and "auto-indent" for programming languages.
	 */
	public static class InsertBreakAction
							extends RTextAreaEditorKit.InsertBreakAction {

		private static final long serialVersionUID = 1L;

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			RSyntaxTextArea sta = (RSyntaxTextArea)textArea;
			boolean noSelection= sta.getSelectionStart()==sta.getSelectionEnd();

			// First, see if this language wants to handle inserting newlines
			// itself.
			boolean handled = false;
			if (noSelection) {
				RSyntaxDocument doc = (RSyntaxDocument)sta.getDocument();
				handled = doc.insertBreakSpecialHandling(e);
			}

			// If not...
			if (!handled) {
				handleInsertBreak(sta, noSelection);
			}

		}

		/**
		 * @return The first location in the string past <code>pos</code> that
		 *         is NOT a whitespace char, or <code>-1</code> if only
		 *         whitespace chars follow <code>pos</code> (or it is the end
		 *         position in the string).
		 */
		private static final int atEndOfLine(int pos, String s, int sLen) {
			for (int i=pos; i<sLen; i++) {
				if (!RSyntaxUtilities.isWhitespace(s.charAt(i)))
					return i;
			}
			return -1;
		}

		private static final int getOpenBraceCount(RSyntaxDocument doc) {
			int openCount = 0;
			Element root = doc.getDefaultRootElement();
			int lineCount = root.getElementCount();
			for (int i=0; i<lineCount; i++) {
				Token t = doc.getTokenListForLine(i);
				while (t!=null && t.isPaintable()) {
					if (t.type==Token.SEPARATOR && t.textCount==1) {
						char ch = t.text[t.textOffset];
						if (ch=='{') {
							openCount++;
						}
						else if (ch=='}') {
							openCount--;
						}
					}
					t = t.getNextToken();
				}
			}
			return openCount;
		}

		/**
		 * Actually inserts the newline into the document, and auto-indents
		 * if appropriate.  This method can be called by token makers who
		 * implement a custom action for inserting newlines.
		 *
		 * @param textArea
		 * @param noSelection Whether there is no selection.
		 */
		protected void handleInsertBreak(RSyntaxTextArea textArea,
										boolean noSelection) {
			// If we're auto-indenting...
			if (noSelection && textArea.isAutoIndentEnabled()) {
				insertNewlineWithAutoIndent(textArea);
			}
			else {
				textArea.replaceSelection("\n");
				if (noSelection) {
					possiblyCloseCurlyBrace(textArea, null);
				}
			}
		}

		private void insertNewlineWithAutoIndent(RSyntaxTextArea sta) {

			try {

				int caretPos = sta.getCaretPosition();
				Document doc = sta.getDocument();
				Element map = doc.getDefaultRootElement();
				int lineNum = map.getElementIndex(caretPos);
				Element line = map.getElement(lineNum);
				int start = line.getStartOffset();
				int end = line.getEndOffset()-1; // Why always "-1"?
				int len = end-start;
				String s = doc.getText(start, len);

				// endWS is the end of the leading whitespace of the
				// current line.
				String leadingWS = RSyntaxUtilities.getLeadingWhitespace(s);
				StringBuffer sb = new StringBuffer("\n");
				sb.append(leadingWS);

				// If there is only whitespace between the caret and
				// the EOL, pressing Enter auto-indents the new line to
				// the same place as the previous line.
				int nonWhitespacePos = atEndOfLine(caretPos-start, s, len);
				if (nonWhitespacePos==-1) {
					if (leadingWS.length()==len &&
							sta.isClearWhitespaceLinesEnabled()) {
						// If the line was nothing but whitespace, select it
						// so its contents get removed.
						sta.setSelectionStart(start);
						sta.setSelectionEnd(end);
					}
					sta.replaceSelection(sb.toString());
				}

				// If there is non-whitespace between the caret and the
				// EOL, pressing Enter takes that text to the next line
				// and auto-indents it to the same place as the last
				// line.
				else {
					sb.append(s.substring(nonWhitespacePos));
					sta.replaceRange(sb.toString(), caretPos, end);
					sta.setCaretPosition(caretPos + leadingWS.length()+1);
				}

				// Must do it after everything else, as the "smart indent"
				// calculation depends on the previous line's state
				// AFTER the Enter press (stuff may have been moved down).
				if (sta.getShouldIndentNextLine(lineNum)) {
					sta.replaceSelection("\t");
				}

				possiblyCloseCurlyBrace(sta, leadingWS);

			} catch (BadLocationException ble) { // Never happens
				sta.replaceSelection("\n");
				ble.printStackTrace();
			}

		}

		private void possiblyCloseCurlyBrace(RSyntaxTextArea textArea,
											String leadingWS) {

			RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();

			if (textArea.getCloseCurlyBraces() &&
					doc.getCurlyBracesDenoteCodeBlocks()) {

				int line = textArea.getCaretLineNumber();
				Token t = doc.getTokenListForLine(line-1);
				t = t.getLastNonCommentNonWhitespaceToken();

				if (t!=null && t.isLeftCurly()) {

					if (getOpenBraceCount(doc)>0) {
						StringBuffer sb = new StringBuffer();
						if (line==textArea.getLineCount()-1) {
							sb.append('\n');
						}
						if (leadingWS!=null) {
							sb.append(leadingWS);
						}
						sb.append("}\n");
						int dot = textArea.getCaretPosition();
						int end = textArea.getLineEndOffsetOfCurrentLine();
						// Insert at end of line, not at dot: they may have
						// pressed Enter in the middle of the line and brought
						// some text (though it must be whitespace and/or
						// comments) down onto the new line.
						textArea.insert(sb.toString(), end);
						textArea.setCaretPosition(dot); // Caret may have moved
					}

				}

			}

		}

	}


	/**
	 * Action for inserting tabs.  This is extended to "block indent" a
	 * group of contiguous lines if they are selected.
	 */
	public static class InsertTabAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public InsertTabAction() {
			super(insertTabAction);
		}

		public InsertTabAction(String name) {
			super(name);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			Document document = textArea.getDocument();
			Element map = document.getDefaultRootElement();
			Caret c = textArea.getCaret();
			int dot = c.getDot();
			int mark = c.getMark();
			int dotLine = map.getElementIndex(dot);
			int markLine = map.getElementIndex(mark);

			// If there is a multi-line selection, indent all lines in
			// the selection.
			if (dotLine!=markLine) {
				int first = Math.min(dotLine, markLine);
				int last = Math.max(dotLine, markLine);
				Element elem; int start;

				// Since we're using Document.insertString(), we must mimic the
				// soft tab behavior provided by RTextArea.replaceSelection().
				String replacement = "\t";
				if (textArea.getTabsEmulated()) {
					StringBuffer sb = new StringBuffer();
					int temp = textArea.getTabSize();
					for (int i=0; i<temp; i++) {
						sb.append(' ');
					}
					replacement = sb.toString();
				}

				textArea.beginAtomicEdit();
				try {
					for (int i=first; i<last; i++) {
						elem = map.getElement(i);
						start = elem.getStartOffset();
						document.insertString(start, replacement, null);
					}
					// Don't do the last line if the caret is at its
					// beginning.  We must call getDot() again and not just
					// use 'dot' as the caret's position may have changed
					// due to the insertion of the tabs above.
					elem = map.getElement(last);
					start = elem.getStartOffset();
					if (Math.max(c.getDot(), c.getMark())!=start) {
						document.insertString(start, replacement, null);
					}
				} catch (BadLocationException ble) { // Never happens.
					ble.printStackTrace();
					UIManager.getLookAndFeel().
									provideErrorFeedback(textArea);
				} finally {
					textArea.endAtomicEdit();
				}
			}
			else {
				textArea.replaceSelection("\t");
			}

		}

		public final String getMacroID() {
			return insertTabAction;
		}

	}


	/**
	 * Action to move the selection and/or caret. Constructor indicates
	 * direction to use.  This class overrides the behavior defined in
	 * {@link RTextAreaEditorKit} to better skip "words" in source code.
	 */
	public static class NextWordAction
						extends RTextAreaEditorKit.NextWordAction {

		private Segment seg;

		public NextWordAction(String nm, boolean select) {
			super(nm, select);
			seg = new Segment();
		}

		/**
		 * Overridden to do better with skipping "words" in code.
		 */
		protected int getNextWord(RTextArea textArea, int offs)
									throws BadLocationException {

			RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
			if (offs==doc.getLength()) {
				return offs;
			}

			Element root = doc.getDefaultRootElement();
			int line = root.getElementIndex(offs);
			int end = root.getElement(line).getEndOffset() - 1;
			if (offs==end) {// If we're already at the end of the line...
				RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
				if (rsta.isCodeFoldingEnabled()) { // Start of next visible line
					FoldManager fm = rsta.getFoldManager();
					int lineCount = root.getElementCount();
					while (++line<lineCount && fm.isLineHidden(line));
					if (line<lineCount) { // Found a lower visible line
						offs = root.getElement(line).getStartOffset();
					}
					// No lower visible line - we're already at last visible offset
					return offs;
				}
				else {
					return offs+1; // Start of next line.
				}
			}
			doc.getText(offs, end-offs, seg);

			// Determine the "type" of char at offs - letter/digit,
			// whitespace or other
			char ch = seg.first();

			// Skip the group of letters and/or digits
			if (Character.isLetterOrDigit(ch)) {
				do {
					ch = seg.next();
				} while (Character.isLetterOrDigit(ch));
			}

			// Skip groups of "anything else" (operators, etc.).
			else if (!Character.isWhitespace(ch)) {
				do {
					ch = seg.next();
				} while (ch!=Segment.DONE &&
						!(Character.isLetterOrDigit(ch) ||
								Character.isWhitespace(ch)));
			}

			// Skip any trailing whitespace
			while (Character.isWhitespace(ch)) {
				ch = seg.next();
			}

			offs += seg.getIndex() - seg.getBeginIndex();
			return offs;

		}

	}


	/**
	 * Action for when the user tries to insert a template (that is,
	 * they've typed a template ID and pressed the trigger character
	 * (a space) in an attempt to do the substitution).
	 */
	public static class PossiblyInsertTemplateAction extends RecordableTextAction {

		private static final long serialVersionUID = 1L;

		public PossiblyInsertTemplateAction() {
			super(rstaPossiblyInsertTemplateAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled())
				return;

			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;

			if (RSyntaxTextArea.getTemplatesEnabled()) {

				Document doc = textArea.getDocument();
				if (doc != null) {

					try {

						CodeTemplateManager manager = RSyntaxTextArea.
											getCodeTemplateManager();
						CodeTemplate template =  manager==null ? null :
													manager.getTemplate(rsta);

						// A non-null template means modify the text to insert!
						if (template!=null) {
							template.invoke(rsta);
						}

						// No template - insert default text.  This is
						// exactly what DefaultKeyTypedAction does.
						else {
							doDefaultInsert(rsta);
						}

					} catch (BadLocationException ble) {
						UIManager.getLookAndFeel().
								provideErrorFeedback(textArea);
					}


				} // End of if (doc!=null).

			} // End of if (textArea.getTemplatesEnabled()).

			// If templates aren't enabled, just insert the text as usual.
			else {
				doDefaultInsert(rsta);
			}

		}

		private final void doDefaultInsert(RTextArea textArea) {
			// FIXME:  We need a way to get the "trigger string" (i.e.,
			// the text that was just typed); however, the text area's
			// template manager might be null (if templates are disabled).
			// Also, the manager's trigger string doesn't yet match up with
			// that defined in RSyntaxTextAreaEditorKit.java (which is
			// hardcoded as a space)...
			//String str = manager.getInsertTriggerString();
			//int mod = manager.getInsertTrigger().getModifiers();
			//if (str!=null && str.length()>0 &&
			//	((mod&ActionEvent.ALT_MASK)==(mod&ActionEvent.CTRL_MASK))) {
			//	char ch = str.charAt(0);
			//	if (ch>=0x20 && ch!=0x7F)
			//		textArea.replaceSelection(str);
			//}
			textArea.replaceSelection(" ");
		}

		public final String getMacroID() {
			return rstaPossiblyInsertTemplateAction;
		}

	}


	/**
	 * Action to move the selection and/or caret. Constructor indicates
	 * direction to use.  This class overrides the behavior defined in
	 * {@link RTextAreaEditorKit} to better skip "words" in source code.
	 */
	public static class PreviousWordAction
						extends RTextAreaEditorKit.PreviousWordAction {

		private Segment seg;

		public PreviousWordAction(String nm, boolean select) {
			super(nm, select);
			seg = new Segment();
		}

		/**
		 * Overridden to do better with skipping "words" in code.
		 */
		protected int getPreviousWord(RTextArea textArea, int offs)
										throws BadLocationException {

			if (offs==0) {
				return offs;
			}

			RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
			Element root = doc.getDefaultRootElement();
			int line = root.getElementIndex(offs);
			int start = root.getElement(line).getStartOffset();
			if (offs==start) {// If we're already at the start of the line...
				RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
				if (rsta.isCodeFoldingEnabled()) { // End of next visible line
					FoldManager fm = rsta.getFoldManager();
					while (--line>=0 && fm.isLineHidden(line));
					if (line>=0) { // Found an earlier visible line
						offs = root.getElement(line).getEndOffset() - 1;
					}
					// No earlier visible line - we must be at offs==0...
					return offs;
				}
				else {
					return start-1; // End of previous line.
				}
			}
			doc.getText(start, offs-start, seg);

			// Determine the "type" of char at offs - lower case, upper case,
			// whitespace or other
			char ch = seg.last();

			// Skip any "leading" whitespace
			while (Character.isWhitespace(ch)) {
				ch = seg.previous();
			}

			// Skip the group of letters and/or digits
			if (Character.isLetterOrDigit(ch)) {
				do {
					ch = seg.previous();
				} while (Character.isLetterOrDigit(ch));
			}

			// Skip groups of "anything else" (operators, etc.).
			else if (!Character.isWhitespace(ch)) {
				do {
					ch = seg.previous();
				} while (ch!=Segment.DONE &&
						!(Character.isLetterOrDigit(ch) ||
								Character.isWhitespace(ch)));
			}

			offs -= seg.getEndIndex() - seg.getIndex();
			if (ch!=Segment.DONE) {
				offs++;
			}

			return offs;

		}

	}


	/**
	 * Selects the word around the caret.  This class is here to better
	 * handle selecting "words" in programming languages.
	 */
	public static class SelectWordAction
					extends RTextAreaEditorKit.SelectWordAction {

		protected void createActions() {
			start = new BeginWordAction("pigdog", false);
			end = new EndWordAction("pigdog", true);
		}

	}


	/**
	 * Action that toggles whether the currently selected lines are
	 * commented.
	 */
	public static class ToggleCommentAction extends RecordableTextAction {

		public ToggleCommentAction() {
			super(rstaToggleCommentAction);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {

			if (!textArea.isEditable() || !textArea.isEnabled()) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
			String[] startEnd = doc.getLineCommentStartAndEnd();

			if (startEnd==null) {
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
				return;
			}

			Element map = doc.getDefaultRootElement();
			Caret c = textArea.getCaret();
			int dot = c.getDot();
			int mark = c.getMark();
			int line1 = map.getElementIndex(dot);
			int line2 = map.getElementIndex(mark);
			int start = Math.min(line1, line2);
			int end   = Math.max(line1, line2);

			// Don't toggle comment on last line if there is no
			// text selected on it.
			if (start!=end) {
				Element elem = map.getElement(end);
				if (Math.max(dot, mark)==elem.getStartOffset()) {
					end--;
				}
			}

			textArea.beginAtomicEdit();
			try {
				boolean add = getDoAdd(doc,map, start,end, startEnd);
				for (line1=start; line1<=end; line1++) {
					Element elem = map.getElement(line1);
					handleToggleComment(elem, doc, startEnd, add);
				}
			} catch (BadLocationException ble) {
				ble.printStackTrace();
				UIManager.getLookAndFeel().provideErrorFeedback(textArea);
			} finally {
				textArea.endAtomicEdit();
			}

		}

		private boolean getDoAdd(Document doc, Element map, int startLine,
							int endLine, String[] startEnd)
								throws BadLocationException {
			boolean doAdd = false;
			for (int i=startLine; i<=endLine; i++) {
				Element elem = map.getElement(i);
				int start = elem.getStartOffset();
				String t = doc.getText(start, elem.getEndOffset()-start-1);
				if (!t.startsWith(startEnd[0]) ||
						(startEnd[1]!=null && !t.endsWith(startEnd[1]))) {
					doAdd = true;
					break;
				}
			}
			return doAdd;
		}

		private void handleToggleComment(Element elem, Document doc,
			String[] startEnd, boolean add) throws BadLocationException {
			int start = elem.getStartOffset();
			int end = elem.getEndOffset() - 1;
			if (add) {
				doc.insertString(start, startEnd[0], null);
				if (startEnd[1]!=null) {
					doc.insertString(end+startEnd[0].length(), startEnd[1],
									null);
				}
			}
			else {
				doc.remove(start, startEnd[0].length());
				if (startEnd[1]!=null) {
					int temp = startEnd[1].length();
					doc.remove(end-startEnd[0].length()-temp, temp);
				}
			}
		}

		public final String getMacroID() {
			return rstaToggleCommentAction;
		}

	}


	/**
	 * Toggles the fold at the current caret position or line.
	 */
	public static class ToggleCurrentFoldAction extends FoldRelatedAction {

		private static final long serialVersionUID = 1L;

		public ToggleCurrentFoldAction() {
			super(rstaToggleCurrentFoldAction);
			setProperties(msg, "Action.ToggleCurrentFold");
		}

		public ToggleCurrentFoldAction(String name, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
			super(name, icon, desc, mnemonic, accelerator);
		}

		public void actionPerformedImpl(ActionEvent e, RTextArea textArea) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			if (rsta.isCodeFoldingEnabled()) {
				Fold fold = getClosestFold(rsta);
				if (fold!=null) {
					fold.toggleCollapsedState();
				}
				possiblyRepaintGutter(textArea);
			}
			else {
				UIManager.getLookAndFeel().provideErrorFeedback(rsta);
			}
		}

		public final String getMacroID() {
			return rstaToggleCurrentFoldAction;
		}

	}


}