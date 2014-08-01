/*
 * 08/19/2004
 *
 * RTADefaultInputMap.java - The default input map for RTextAreas.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;


/**
 * The default input map for an <code>RTextArea</code>.  For the most part it is
 * exactly that the one for a <code>JTextArea</code>, but it adds a few things.
 * Currently, the new key bindings include:
 * <ul>
 *   <li>HOME key toggles between first character on line and first non-
 *       whitespace character on line.
 *   <li>INSERT key toggles between insert and overwrite modes.
 *   <li>Ctrl+DELETE key deletes all text between the caret and the end of the
 *       current line.
 *   <li>Ctrl+Shift+Up and Ctrl+Shift+Down move the current line up and
 *       down, respectively.
 *   <li>Ctrl+J joins lines.
 *   <li>Ctrl+Z is undo and Ctrl+Y is redo.
 *   <li>Ctrl+Up and Ctrl+Down shift the visible area of the text area up and
 *       down one line, respectively.
 *   <li>F2 and Shift+F2 moves to the next and previous bookmarks,
 *       respectively.
 *   <li>Ctrl+F2 toggles whether a bookmark is on the current line.
 *   <li>etc.
 * </ul>
 */
public class RTADefaultInputMap extends InputMap {


	/**
	 * Constructs the default input map for an <code>RTextArea</code>.
	 */
	public RTADefaultInputMap() {

		super();

		int defaultModifier = getDefaultModifier();
		//int ctrl = InputEvent.CTRL_MASK;
		int alt = InputEvent.ALT_MASK;
		int shift = InputEvent.SHIFT_MASK;

		put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME,   0),					DefaultEditorKit.beginLineAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME,   shift),					DefaultEditorKit.selectionBeginLineAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME,   defaultModifier),			DefaultEditorKit.beginAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME,   defaultModifier|shift),	DefaultEditorKit.selectionBeginAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_END,    0),					DefaultEditorKit.endLineAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_END,    shift),					DefaultEditorKit.selectionEndLineAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_END,    defaultModifier),			DefaultEditorKit.endAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_END,    defaultModifier|shift),	DefaultEditorKit.selectionEndAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,   0),					DefaultEditorKit.backwardAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,   shift),					DefaultEditorKit.selectionBackwardAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,   defaultModifier),			DefaultEditorKit.previousWordAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,   defaultModifier|shift),	DefaultEditorKit.selectionPreviousWordAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,   0),					DefaultEditorKit.downAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,   shift),					DefaultEditorKit.selectionDownAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,   defaultModifier),			RTextAreaEditorKit.rtaScrollDownAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,   alt),	RTextAreaEditorKit.rtaLineDownAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,  0),					DefaultEditorKit.forwardAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,  shift),					DefaultEditorKit.selectionForwardAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,  defaultModifier),			DefaultEditorKit.nextWordAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,  defaultModifier|shift),	DefaultEditorKit.selectionNextWordAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,     0),					DefaultEditorKit.upAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,     shift),					DefaultEditorKit.selectionUpAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,     defaultModifier),			RTextAreaEditorKit.rtaScrollUpAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,     alt),	RTextAreaEditorKit.rtaLineUpAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,0),					DefaultEditorKit.pageUpAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,shift),					RTextAreaEditorKit.rtaSelectionPageUpAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP,defaultModifier|shift), 	RTextAreaEditorKit.rtaSelectionPageLeftAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,0),					DefaultEditorKit.pageDownAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,shift),				RTextAreaEditorKit.rtaSelectionPageDownAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN,defaultModifier|shift),	RTextAreaEditorKit.rtaSelectionPageRightAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_CUT,    0),					DefaultEditorKit.cutAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_COPY,   0),					DefaultEditorKit.copyAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_PASTE,  0),					DefaultEditorKit.pasteAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_X,      defaultModifier),			DefaultEditorKit.cutAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_C,      defaultModifier),			DefaultEditorKit.copyAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_V,      defaultModifier),			DefaultEditorKit.pasteAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),					DefaultEditorKit.deleteNextCharAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, shift),					DefaultEditorKit.cutAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, defaultModifier),			RTextAreaEditorKit.rtaDeleteRestOfLineAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),					RTextAreaEditorKit.rtaToggleTextModeAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, shift),					DefaultEditorKit.pasteAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, defaultModifier),			DefaultEditorKit.copyAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_A,      defaultModifier),			DefaultEditorKit.selectAllAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_D,      defaultModifier),			RTextAreaEditorKit.rtaDeleteLineAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_J,      defaultModifier),			RTextAreaEditorKit.rtaJoinLinesAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, shift),				DefaultEditorKit.deletePrevCharAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, defaultModifier),		RTextAreaEditorKit.rtaDeletePrevWordAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,    0),					DefaultEditorKit.insertTabAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,  0),					DefaultEditorKit.insertBreakAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,  shift),					DefaultEditorKit.insertBreakAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, defaultModifier),		RTextAreaEditorKit.rtaDumbCompleteWordAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_Z,      defaultModifier),			RTextAreaEditorKit.rtaUndoAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_Y,      defaultModifier),			RTextAreaEditorKit.rtaRedoAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_F2,		0),							RTextAreaEditorKit.rtaNextBookmarkAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_F2,		shift),						RTextAreaEditorKit.rtaPrevBookmarkAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_F2,		defaultModifier),			RTextAreaEditorKit.rtaToggleBookmarkAction);

		put(KeyStroke.getKeyStroke(KeyEvent.VK_K,		defaultModifier|shift),		RTextAreaEditorKit.rtaPrevOccurrenceAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_K,		defaultModifier),			RTextAreaEditorKit.rtaNextOccurrenceAction);

		/* NOTE:  Currently, macros aren't part of the default input map for */
		/* RTextArea, as they display their own popup windows, etc. which    */
		/* may or may not clash with the application in which the RTextArea  */
		/* resides.  You can add the macro actions yourself into an          */
		/* application if you want.  They may become standard in the future  */
		/* if I can find a way to implement them that I like.                */
		/*
		put(KeyStroke.getKeyStroke(KeyEvent.VK_R,      defaultModifier|shift),	RTextAreaEditorKit.rtaBeginRecordingMacroAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_S,      defaultModifier|shift),	RTextAreaEditorKit.rtaEndRecordingMacroAction);
		put(KeyStroke.getKeyStroke(KeyEvent.VK_M,      defaultModifier|shift),	RTextAreaEditorKit.rtaPlaybackLastMacroAction);
		*/

	}


	/**
	 * Returns the default modifier key for a system.  For example, on Windows
	 * this would be the CTRL key (<code>InputEvent.CTRL_MASK</code>).
	 *
	 * @return The default modifier key.
	 */
	protected static final int getDefaultModifier() {
		return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	}



}