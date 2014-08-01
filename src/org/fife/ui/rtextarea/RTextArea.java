/*
 * 11/14/2003
 *
 * RTextArea.java - An extension of JTextArea that adds many features.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.plaf.TextUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.fife.print.RPrintUtilities;
import org.fife.ui.rtextarea.Macro.MacroRecord;


/**
 * An extension of <code>JTextArea</code> that adds the following features:
 * <ul>
 *    <li>Insert/Overwrite modes (can be toggled via the Insert key)
 *    <li>A right-click popup menu with standard editing options
 *    <li>Macro support
 *    <li>"Mark all" functionality.
 *    <li>A way to change the background to an image (gif/png/jpg)
 *    <li>Highlight the current line (can be toggled)
 *    <li>An easy way to print its text (implements Printable)
 *    <li>Hard/soft (emulated with spaces) tabs
 *    <li>Fixes a bug with setTabSize
 *    <li>Other handy new methods
 * </ul>
 * NOTE:  If the background for an <code>RTextArea</code> is set to a color,
 * its opaque property is set to <code>true</code> for performance reasons.  If
 * the background is set to an image, then the opaque property is set to
 * <code>false</code>.  This slows things down a little, but if it didn't happen
 * then we would see garbage on-screen when the user scrolled through a document
 * using the arrow keys (not the page-up/down keys though).  You should never
 * have to set the opaque property yourself; it is always done for you.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RTextArea extends RTextAreaBase
								implements Printable, Serializable {

	/**
	 * Constant representing insert mode.
	 *
	 * @see #setCaretStyle(int, int)
	 */
	public static final int INSERT_MODE				= 0;

	/**
	 * Constant representing overwrite mode.
	 *
	 * @see #setCaretStyle(int, int)
	 */
	public static final int OVERWRITE_MODE				= 1;

	/**
	 * The property fired when the "mark all" color changes.
	 */
	public static final String MARK_ALL_COLOR_PROPERTY	= "RTA.markAllColor";

	/*
	 * Constants for all actions.
	 */
	private static final int MIN_ACTION_CONSTANT	= 0;
	public static final int COPY_ACTION				= 0;
	public static final int CUT_ACTION				= 1;
	public static final int DELETE_ACTION			= 2;
	public static final int PASTE_ACTION			= 3;
	public static final int REDO_ACTION				= 4;
	public static final int SELECT_ALL_ACTION		= 5;
	public static final int UNDO_ACTION				= 6;
	private static final int MAX_ACTION_CONSTANT	= 6;

	private static final Color DEFAULT_MARK_ALL_COLOR		= Color.ORANGE;

	/**
	 * The current text mode ({@link #INSERT_MODE} or {@link #OVERWRITE_MODE}).
	 */
	private int textMode;

	// All macros are shared across all RTextAreas.
	private static boolean recordingMacro;		// Whether we're recording a macro.
	private static Macro currentMacro;

	/**
	 * This text area's popup menu.
	 */
	private JPopupMenu popupMenu;

	private JMenuItem undoMenuItem;
	private JMenuItem redoMenuItem;
	private JMenuItem cutMenuItem;
	private JMenuItem pasteMenuItem;
	private JMenuItem deleteMenuItem;

	/**
	 * Whether the popup menu has been created.
	 */
	private boolean popupMenuCreated;

	/**
	 * The text last searched for via Ctrl+K or Ctrl+Shift+K.
	 */
	private static String selectedOccurrenceText;

	/**
	 * Can return tool tips for this text area.  Subclasses can install a
	 * supplier as a means of adding custom tool tips without subclassing
	 * <tt>RTextArea</tt>.  {@link #getToolTipText()} checks this supplier
	 * before calling the super class's version.
	 */
	private ToolTipSupplier toolTipSupplier;

	private static RecordableTextAction cutAction;
	private static RecordableTextAction copyAction;
	private static RecordableTextAction pasteAction;
	private static RecordableTextAction deleteAction;
	private static RecordableTextAction undoAction;
	private static RecordableTextAction redoAction;
	private static RecordableTextAction selectAllAction;

	private static IconGroup iconGroup;		// Info on icons for actions.

	private transient RUndoManager undoManager;

	private transient LineHighlightManager lineHighlightManager;

	private ArrayList markAllHighlights;		// Highlights from "mark all".
	private String markedWord;				// Expression marked in "mark all."
	private ChangeableHighlightPainter markAllHighlightPainter;

	private int[] carets;		// Index 0=>insert caret, 1=>overwrite.

	private static final String MSG	= "org.fife.ui.rtextarea.RTextArea";


	/**
	 * Constructor.
	 */
	public RTextArea() {
		init(INSERT_MODE);
	}


	/**
	 * Constructor.
	 *
	 * @param doc The document for the editor.
	 */
	public RTextArea(AbstractDocument doc) {
		super(doc);
		init(INSERT_MODE);
	}


	/**
	 * Constructor.
	 *
	 * @param text The initial text to display.
	 */
	public RTextArea(String text) {
		super(text);
		init(INSERT_MODE);
	}


	/**
	 * Constructor.
	 *
	 * @param rows The number of rows to display.
	 * @param cols The number of columns to display.
	 * @throws IllegalArgumentException If either <code>rows</code> or
	 *         <code>cols</code> is negative.
	 */
	public RTextArea(int rows, int cols) {
		super(rows, cols);
		init(INSERT_MODE);
	}


	/**
	 * Constructor.
	 *
	 * @param text The initial text to display.
	 * @param rows The number of rows to display.
	 * @param cols The number of columns to display.
	 * @throws IllegalArgumentException If either <code>rows</code> or
	 *         <code>cols</code> is negative.
	 */
	public RTextArea(String text, int rows, int cols) {
		super(text, rows, cols);
		init(INSERT_MODE);
	}


	/**
	 * Constructor.
	 *
	 * @param doc The document for the editor.
	 * @param text The initial text to display.
	 * @param rows The number of rows to display.
	 * @param cols The number of columns to display.
	 * @throws IllegalArgumentException If either <code>rows</code> or
	 *         <code>cols</code> is negative.
	 */
	public RTextArea(AbstractDocument doc, String text, int rows, int cols) {
		super(doc, text, rows, cols);
		init(INSERT_MODE);
	}


	/**
	 * Creates a new <code>RTextArea</code>.
	 *
	 * @param textMode Either <code>INSERT_MODE</code> or
	 *        <code>OVERWRITE_MODE</code>.
	 */
	public RTextArea(int textMode) {
		init(textMode);
	}


	/**
	 * Adds an action event to the current macro.  This shouldn't be called
	 * directly, as it is called by the actions themselves.
	 *
	 * @param id The ID of the recordable text action.
	 * @param actionCommand The "command" of the action event passed to it.
	 */
	static synchronized void addToCurrentMacro(String id,
											String actionCommand) {
		currentMacro.addMacroRecord(new Macro.MacroRecord(id, actionCommand));
	}


	/**
	 * Adds a line highlight.
	 *
	 * @param line The line to highlight.  This is zero-based.
	 * @param color The color to highlight the line with.
	 * @throws BadLocationException If <code>line</code> is an invalid line
	 *         number.
	 * @see #removeLineHighlight(Object)
	 * @see #removeAllLineHighlights()
	 */
	public Object addLineHighlight(int line, Color color)
										throws BadLocationException {
		if (lineHighlightManager==null) {
			lineHighlightManager = new LineHighlightManager(this);
		}
		return lineHighlightManager.addLineHighlight(line, color);
	}


	/**
	 * Begins an "atomic edit."  All text editing operations between this call
	 * and the next call to <tt>endAtomicEdit()</tt> will be treated as a
	 * single operation by the undo manager.<p>
	 *
	 * Using this method should be done with great care.  You should probably
	 * wrap the call to <tt>endAtomicEdit()</tt> in a <tt>finally</tt> block:
	 *
	 * <pre>
	 * textArea.beginAtomicEdit();
	 * try {
	 *    // Do editing
	 * } finally {
	 *    textArea.endAtomicEdit();
	 * }
	 * </pre>
	 *
	 * @see #endAtomicEdit()
	 */
	public void beginAtomicEdit() {
		undoManager.beginInternalAtomicEdit();
	}


	/**
	 * Begins recording a macro.  After this method is called, all input/caret
	 * events, etc. are recorded until <code>endMacroRecording</code> is
	 * called.  If this method is called but the text component is already
	 * recording a macro, nothing happens (but the macro keeps recording).
	 *
	 * @see #isRecordingMacro()
	 * @see #endRecordingMacro()
	 */
	public static synchronized void beginRecordingMacro() {
		if (isRecordingMacro()) {
			//System.err.println("Macro already being recorded!");
			return;
		}
		//JOptionPane.showMessageDialog(this, "Now recording a macro");
		if (currentMacro!=null)
			currentMacro = null; // May help gc?
		currentMacro = new Macro();
		recordingMacro = true;
	}


	/**
	 * Tells whether an undo is possible
	 * 
	 * @see #canRedo()
	 * @see #undoLastAction()
	 */
	public boolean canUndo() {
		return undoManager.canUndo();
	}


	/**
	 * Tells whether a redo is possible
	 * 
	 * @see #canUndo()
	 * @see #redoLastAction()
	 */
	public boolean canRedo() {
		return undoManager.canRedo();
	}


	/**
	 * Clears any "mark all" highlights, if any.
	 *
	 * @see #markAll
	 * @see #getMarkAllHighlightColor
	 * @see #setMarkAllHighlightColor
	 */
	public void clearMarkAllHighlights() {
		Highlighter h = getHighlighter();
		if (h!=null && markAllHighlights!=null) {
			int count = markAllHighlights.size();
			for (int i=0; i<count; i++)
				h.removeHighlight(markAllHighlights.get(i));
			markAllHighlights.clear();
		}
		markedWord = null;
		repaint();
	}


	/**
	 * Configures the popup menu for this text area.  This method is called
	 * right before it is displayed, so a hosting application can do any
	 * custom configuration (configuring actions, adding/removing items, etc.).
	 * <p>
	 *
	 * The default implementation does nothing.<p>
	 * 
	 * If you set the popup menu via {@link #setPopupMenu(JPopupMenu)}, you
	 * will want to override this method, especially if you removed any of the
	 * menu items in the default popup menu.
	 *
	 * @param popupMenu The popup menu.  This will never be <code>null</code>.
	 * @see #createPopupMenu()
	 * @see #setPopupMenu(JPopupMenu)
	 */
	protected void configurePopupMenu(JPopupMenu popupMenu) {

		boolean canType = isEditable() && isEnabled();

		// Since the user can customize the popup menu, these actions may not
		// have been created.
		if (undoMenuItem!=null) {
			undoMenuItem.setEnabled(undoAction.isEnabled() && canType);
			redoMenuItem.setEnabled(redoAction.isEnabled() && canType);
			cutMenuItem.setEnabled(cutAction.isEnabled() && canType);
			pasteMenuItem.setEnabled(pasteAction.isEnabled() && canType);
			deleteMenuItem.setEnabled(deleteAction.isEnabled() && canType);
		}

	}


	/**
	 * Creates the default implementation of the model to be used at
	 * construction if one isn't explicitly given. A new instance of RDocument
	 * is returned.
	 *
	 * @return The default document.
	 */
	protected Document createDefaultModel() {
		return new RDocument();
	}

	/**
	 * Returns the caret event/mouse listener for <code>RTextArea</code>s.
	 *
	 * @return The caret event/mouse listener.
	 */
	protected RTAMouseListener createMouseListener() {
		return new RTextAreaMutableCaretEvent(this);
	}


	/**
	 * Creates the right-click popup menu. Subclasses can override this method
	 * to replace or augment the popup menu returned.
	 *
	 * @return The popup menu.
	 * @see #setPopupMenu(JPopupMenu)
	 * @see #configurePopupMenu(JPopupMenu)
	 * @see #createPopupMenuItem(Action)
	 */
	protected JPopupMenu createPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(undoMenuItem = createPopupMenuItem(undoAction));
		menu.add(redoMenuItem = createPopupMenuItem(redoAction));
		menu.addSeparator();
		menu.add(cutMenuItem = createPopupMenuItem(cutAction));
		menu.add(createPopupMenuItem(copyAction));
		menu.add(pasteMenuItem = createPopupMenuItem(pasteAction));
		menu.add(deleteMenuItem = createPopupMenuItem(deleteAction));
		menu.addSeparator();
		menu.add(createPopupMenuItem(selectAllAction));
		return menu;
	}


	/**
	 * Creates the actions used in the popup menu and retrievable by
	 * {@link #getAction(int)}.
	 * TODO: Remove these horrible hacks and move localizing of actions into
	 * the editor kits, where it should be!  The context menu should contain
	 * actions from the editor kits.
	 */
	private static void createPopupMenuActions() {

		// Create actions for right-click popup menu.
		// 1.5.2004/pwy: Replaced the CTRL_MASK with the cross-platform version...
		int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		ResourceBundle msg = ResourceBundle.getBundle(MSG);

		cutAction = new RTextAreaEditorKit.CutAction();
		cutAction.setProperties(msg, "Action.Cut");
		cutAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, mod));
		copyAction = new RTextAreaEditorKit.CopyAction();
		copyAction.setProperties(msg, "Action.Copy");
		copyAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, mod));
		pasteAction = new RTextAreaEditorKit.PasteAction();
		pasteAction.setProperties(msg, "Action.Paste");
		pasteAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, mod));
		deleteAction = new RTextAreaEditorKit.DeleteNextCharAction();
		deleteAction.setProperties(msg, "Action.Delete");
		deleteAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
		undoAction = new RTextAreaEditorKit.UndoAction();
		undoAction.setProperties(msg, "Action.Undo");
		undoAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, mod));
		redoAction = new RTextAreaEditorKit.RedoAction();
		redoAction.setProperties(msg, "Action.Redo");
		redoAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, mod));
		selectAllAction = new RTextAreaEditorKit.SelectAllAction();
		selectAllAction.setProperties(msg, "Action.SelectAll");
		selectAllAction.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, mod));

	}


	/**
	 * Creates and configures a menu item for used in the popup menu.
	 *
	 * @param a The action for the menu item.
	 * @return The menu item.
	 * @see #createPopupMenu()
	 */
	protected JMenuItem createPopupMenuItem(Action a) {
		JMenuItem item = new JMenuItem(a) {
			public void setToolTipText(String text) {
				// Ignore!  Actions (e.g. undo/redo) set this when changing
				// their text due to changing enabled state.
			}
		};
		item.setAccelerator(null);
		return item;
	}


	/**
	 * Returns the a real UI to install on this text area.
	 *
	 * @return The UI.
	 */
	protected RTextAreaUI createRTextAreaUI() {
		return new RTextAreaUI(this);
	}


	/**
	 * Creates an undo manager for use in this text area.
	 *
	 * @return The undo manager.
	 */
	protected RUndoManager createUndoManager() {
		return new RUndoManager(this);
	}


	/**
	 * Removes all undoable edits from this document's undo manager.  This
	 * method also makes the undo/redo actions disabled.
	 */
	/*
	 * NOTE:  For some reason, it appears I have to create an entirely new
	 *        <code>undoManager</code> for undo/redo to continue functioning
	 *        properly; if I don't, it only ever lets you do one undo.  Not
	 *        too sure why this is...
	 */
	public void discardAllEdits() {
		undoManager.discardAllEdits();
		getDocument().removeUndoableEditListener(undoManager);
		undoManager = createUndoManager();
		getDocument().addUndoableEditListener(undoManager);
		undoManager.updateActions();
	}


	/**
	 * Completes an "atomic" edit.
	 *
	 * @see #beginAtomicEdit()
	 */
	public void endAtomicEdit() {
		undoManager.endInternalAtomicEdit();
	}


	/**
	 * Ends recording a macro.  If this method is called but the text component
	 * is not recording a macro, nothing happens.
	 *
	 * @see #isRecordingMacro()
	 * @see #beginRecordingMacro()
	 */
	/*
	 * FIXME:  This should throw an exception if we're not recording a macro.
	 */
	public static synchronized void endRecordingMacro() {
		if (!isRecordingMacro()) {
			//System.err.println("Not recording a macro!");
			return;
		}
		recordingMacro = false;
	}


	/**
	 * Notifies all listeners that a caret change has occurred.
	 *
	 * @param e The caret event.
	 */
	protected void fireCaretUpdate(CaretEvent e) {

		// Decide whether we need to repaint the current line background.
		possiblyUpdateCurrentLineHighlightLocation();

		// Now, if there is a highlighted region of text, allow them to cut
		// and copy.
		if (e!=null && e.getDot()!=e.getMark()) {// && !cutAction.isEnabled()) {
			cutAction.setEnabled(true);
			copyAction.setEnabled(true);
		}

		// Otherwise, if there is no highlighted region, don't let them cut
		// or copy.  The condition here should speed things up, because this
		// way, we will only enable the actions the first time the selection
		// becomes nothing.
		else if (cutAction.isEnabled()) {
			cutAction.setEnabled(false);
			copyAction.setEnabled(false);
		}

		super.fireCaretUpdate(e);

	}


	/**
	 * Removes the "Ctrl+H <=> Backspace" behavior that Java shows, for some
	 * odd reason...
	 */
	private void fixCtrlH() {
		InputMap inputMap = getInputMap();
		KeyStroke char010 = KeyStroke.getKeyStroke("typed \010");
		InputMap parent = inputMap;
		while (parent != null) {
			parent.remove(char010);
			parent = parent.getParent();
		}
		KeyStroke backspace = KeyStroke.getKeyStroke("BACK_SPACE");
		inputMap.put(backspace, DefaultEditorKit.deletePrevCharAction);
	}


	/**
	 * Provides a way to gain access to the editor actions on the right-click
	 * popup menu.  This way you can make toolbar/menu bar items use the actual
	 * actions used by all <code>RTextArea</code>s, so that icons stay
	 * synchronized and you don't have to worry about enabling/disabling them
	 * yourself.<p>
	 * Keep in mind that these actions are shared across all instances of
	 * <code>RTextArea</code>, so a change to any action returned by this
	 * method is global across all <code>RTextArea</code> editors in your
	 * application.
	 *
	 * @param action The action to retrieve, such as {@link #CUT_ACTION}.
	 *        If the action name is invalid, <code>null</code> is returned.
	 * @return The action, or <code>null</code> if an invalid action is
	 *         requested.
	 */
	public static RecordableTextAction getAction(int action) {
		if (action<MIN_ACTION_CONSTANT || action>MAX_ACTION_CONSTANT)
			return null;
		switch (action) {
			case COPY_ACTION:
				return copyAction;
			case CUT_ACTION:
				return cutAction;
			case DELETE_ACTION:
				return deleteAction;
			case PASTE_ACTION:
				return pasteAction;
			case REDO_ACTION:
				return redoAction;
			case SELECT_ALL_ACTION:
				return selectAllAction;
			case UNDO_ACTION:
				return undoAction;
		}
		return null;
	}


	/**
	 * Returns the macro currently stored in this <code>RTextArea</code>.
	 * Since macros are shared, all <code>RTextArea</code>s in the currently-
	 * running application are using this macro.
	 *
	 * @return The current macro, or <code>null</code> if no macro has been
	 *         recorded/loaded.
	 * @see #loadMacro(Macro)
	 */
	public static synchronized Macro getCurrentMacro() {
		return currentMacro;
	}


	/**
	 * Returns the default color used for "mark all."
	 *
	 * @return The color.
	 * @see #getMarkAllHighlightColor()
	 * @see #setMarkAllHighlightColor(Color)
	 */
	public static final Color getDefaultMarkAllHighlightColor() {
		return DEFAULT_MARK_ALL_COLOR;
	}


	/**
	 * Returns the icon group being used for the actions of this text area.
	 *
	 * @return The icon group.
	 * @see #setIconGroup(IconGroup)
	 */
	public static IconGroup getIconGroup() {
		return iconGroup;
	}


	/**
	 * Returns the line highlight manager.
	 *
	 * @return The line highlight manager.  This may be <code>null</code>.
	 */
	LineHighlightManager getLineHighlightManager() {
		return lineHighlightManager;
	}


	/**
	 * Returns the color used in "mark all."
	 *
	 * @return The color.
	 * @see #setMarkAllHighlightColor(Color)
	 */
	public Color getMarkAllHighlightColor() {
		return (Color)markAllHighlightPainter.getPaint();
	}


	/**
	 * Returns the maximum ascent of all fonts used in this text area.  In
	 * the case of a standard <code>RTextArea</code>, this is simply the
	 * ascent of the current font.<p>
	 *
	 * This value could be useful, for example, to implement a line-numbering
	 * scheme.
	 *
	 * @return The ascent of the current font.
	 */
	public int getMaxAscent() {
		return getFontMetrics(getFont()).getAscent();
	}


	/**
	 * Returns the popup menu for this component, lazily creating it if
	 * necessary.
	 *
	 * @return The popup menu.
	 * @see #createPopupMenu()
	 * @see #setPopupMenu(JPopupMenu)
	 */
	public JPopupMenu getPopupMenu() {
		if (!popupMenuCreated) {
			popupMenu = createPopupMenu();
			if (popupMenu!=null) {
				ComponentOrientation orientation = ComponentOrientation.
										getOrientation(Locale.getDefault());
				popupMenu.applyComponentOrientation(orientation);
			}
			popupMenuCreated = true;
		}
		return popupMenu;
	}


	/**
	 * Returns the text last selected and used in a Ctrl+K operation.
	 *
	 * @return The text, or <code>null</code> if none.
	 * @see #setSelectedOccurrenceText(String)
	 */
	public static String getSelectedOccurrenceText() {
		return selectedOccurrenceText;
	}


	/**
	 * Returns the text mode this editor pane is currently in.
	 *
	 * @return Either {@link #INSERT_MODE} or {@link #OVERWRITE_MODE}.
	 * @see #setTextMode(int)
	 */
	public final int getTextMode() {
		return textMode;
	}


	/**
	 * Returns the tool tip supplier.
	 *
	 * @return The tool tip supplier, or <code>null</code> if one isn't
	 *         installed.
	 * @see #setToolTipSupplier(ToolTipSupplier)
	 */
	public ToolTipSupplier getToolTipSupplier() {
		return toolTipSupplier;
	}


	/**
	 * Returns the tooltip to display for a mouse event at the given
	 * location.  This method is overridden to check for a
	 * {@link ToolTipSupplier}; if there is one installed, it is queried for
	 * tool tip text before using the super class's implementation of this
	 * method.
	 *
	 * @param e The mouse event.
	 * @return The tool tip text, or <code>null</code> if none.
	 * @see #getToolTipSupplier()
	 * @see #setToolTipSupplier(ToolTipSupplier)
	 */
	public String getToolTipText(MouseEvent e) {
		String tip = null;
		if (getToolTipSupplier()!=null) {
			tip = getToolTipSupplier().getToolTipText(this, e);
		}
		return tip!=null ? tip : super.getToolTipText();
	}


	/**
	 * Does the actual dirty-work of replacing the selected text in this
	 * text area (i.e., in its document).  This method provides a hook for
	 * subclasses to handle this in a different way.
	 *
	 * @param content The content to add.
	 */
	protected void handleReplaceSelection(String content) {
		// Call into super to handle composed text (1.5+ only though).
		super.replaceSelection(content);
	}


	/**
	 * Initializes this text area.
	 *
	 * @param textMode The text mode.
	 */
	private void init(int textMode) {

		// NOTE: Our actions are created here instead of in a static block
		// so they are only created when the first RTextArea is instantiated,
		// not before.  There have been reports of users calling static getters
		// (e.g. RSyntaxTextArea.getDefaultBracketMatchBGColor()) which would
		// cause these actions to be created and (possibly) incorrectly
		// localized, if they were in a static block.
		if (cutAction==null) {
			createPopupMenuActions();
		}

		// Install the undo manager.
		undoManager = createUndoManager();
		getDocument().addUndoableEditListener(undoManager);

		// Set the defaults for various stuff.
		Color markAllHighlightColor = getDefaultMarkAllHighlightColor();
		markAllHighlightPainter = new ChangeableHighlightPainter(
										markAllHighlightColor);
		setMarkAllHighlightColor(markAllHighlightColor);
		carets = new int[2];
		setCaretStyle(INSERT_MODE, ConfigurableCaret.THICK_VERTICAL_LINE_STYLE);
		setCaretStyle(OVERWRITE_MODE, ConfigurableCaret.BLOCK_STYLE);
		setDragEnabled(true);			// Enable drag-and-drop.

		// Set values for stuff the user passed in.
		setTextMode(textMode); // carets array must be initialized first!

		// Fix the odd "Ctrl+H <=> Backspace" Java behavior.
		fixCtrlH();

	}


	/**
	 * Returns whether or not a macro is being recorded.
	 *
	 * @return Whether or not a macro is being recorded.
	 * @see #beginRecordingMacro()
	 * @see #endRecordingMacro()
	 */
	public static synchronized boolean isRecordingMacro() {
		return recordingMacro;
	}


	/**
	 * Loads a macro to be used by all <code>RTextArea</code>s in the current
	 * application.
	 *
	 * @param macro The macro to load.
	 * @see #getCurrentMacro()
	 */
	public static synchronized void loadMacro(Macro macro) {
		currentMacro = macro;
	}


	/**
	 * Marks all instances of the specified text in this text area.
	 *
	 * @param toMark The text to mark.
	 * @param matchCase Whether the match should be case-sensitive.
	 * @param wholeWord Whether the matches should be surrounded by spaces
	 *        or tabs.
	 * @param regex Whether <code>toMark</code> is a Java regular expression.
	 * @return The number of matches marked.
	 * @see #clearMarkAllHighlights
	 * @see #getMarkAllHighlightColor
	 * @see #setMarkAllHighlightColor
	 */
	public int markAll(String toMark, boolean matchCase, boolean wholeWord,
					boolean regex) {
		Highlighter h = getHighlighter();
		int numMarked = 0;
		if (toMark!=null && !toMark.equals(markedWord) && h!=null) {
			if (markAllHighlights!=null)
				clearMarkAllHighlights();
			else
				markAllHighlights = new ArrayList(10);
			int caretPos = getCaretPosition();
			markedWord = toMark;
			setCaretPosition(0);
			SearchContext context = new SearchContext();
			context.setSearchFor(toMark);
			context.setMatchCase(matchCase);
			context.setRegularExpression(regex);
			context.setSearchForward(true);
			context.setWholeWord(wholeWord);
			boolean found = SearchEngine.find(this, context);
			while (found) {
				int start = getSelectionStart();
				int end = getSelectionEnd();
				try {
					markAllHighlights.add(h.addHighlight(start, end,
										markAllHighlightPainter));
				} catch (BadLocationException ble) {
					ble.printStackTrace();
				}
				numMarked++;
				found = SearchEngine.find(this, context);
			}
			setCaretPosition(caretPos);
			repaint();
		}
		return numMarked;
	}


	/**
	 * {@inheritDoc}
	 */
	public void paste() {
		// Treat paste operations as atomic, otherwise the removal and
		// insertion are treated as two separate undo-able operations.
		beginAtomicEdit();
		try {
			super.paste();
		} finally {
			endAtomicEdit();
		}
	}


	/**
	 * "Plays back" the last recorded macro in this text area.
	 */
	public synchronized void playbackLastMacro() {
		if (currentMacro!=null) {
			Action[] actions = getActions();
			int numActions = actions.length;
			List macroRecords = currentMacro.getMacroRecords();
			int num = macroRecords.size();
			if (num>0) {
				undoManager.beginInternalAtomicEdit();
				try {
					for (int i=0; i<num; i++) {
						MacroRecord record = (MacroRecord)macroRecords.get(i);
						for (int j=0; j<numActions; j++) {
							if ((actions[j] instanceof RecordableTextAction) &&
								record.id.equals(
								((RecordableTextAction)actions[j]).getMacroID())) {
								actions[j].actionPerformed(
									new ActionEvent(this,
												ActionEvent.ACTION_PERFORMED,
												record.actionCommand));
								break;
							}
						}
					}
				} finally {
					undoManager.endInternalAtomicEdit();
				}
			}
		}
	}


	/**
	 * Method called when it's time to print this badboy (the old-school,
	 * AWT way).
	 *
	 * @param g The context into which the page is drawn.
	 * @param pageFormat The size and orientation of the page being drawn.
	 * @param pageIndex The zero based index of the page to be drawn.
	 */
	public int print(Graphics g, PageFormat pageFormat, int pageIndex) {
		return RPrintUtilities.printDocumentWordWrap(g, this, getFont(), pageIndex, pageFormat, getTabSize());
	}


	/**
	 * We override this method because the super version gives us an entirely
	 * new <code>Document</code>, thus requiring us to re-attach our Undo
	 * manager.  With this version we just replace the text.
	 */
	public void read(Reader in, Object desc) throws IOException {

		RTextAreaEditorKit kit = (RTextAreaEditorKit)getUI().getEditorKit(this);
		setText(null);
		Document doc = getDocument();
		if (desc != null)
			doc.putProperty(Document.StreamDescriptionProperty, desc);
		try {
			// NOTE:  Resets the "line separator" property.
			kit.read(in, doc, 0);
		} catch (BadLocationException e) {
			throw new IOException(e.getMessage());
		}

	}


	/**
	 * De-serializes a text area.
	 *
	 * @param s The stream to read from.
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void readObject(ObjectInputStream s)
						throws ClassNotFoundException, IOException {

		s.defaultReadObject();

		// UndoManagers cannot be serialized without Exceptions.  See
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4275892
		undoManager = createUndoManager();
		getDocument().addUndoableEditListener(undoManager);

		lineHighlightManager = null; // Keep FindBugs happy.

	}


	/**
	 * Attempt to redo the last action.
	 *
	 * @see #undoLastAction()
	 */
	public void redoLastAction() {
		// NOTE:  The try/catch block shouldn't be necessary...
		try {
			if (undoManager.canRedo())
				undoManager.redo();
		} catch (CannotRedoException cre) {
			cre.printStackTrace();
		}
	}


	/**
	 * Removes all line highlights.
	 *
	 * @see #removeLineHighlight(Object)
	 */
	public void removeAllLineHighlights() {
		if (lineHighlightManager!=null) {
			lineHighlightManager.removeAllLineHighlights();
		}
	}


	/**
	 * Removes a line highlight.
	 *
	 * @param tag The tag of the line highlight to remove.
	 * @see #removeAllLineHighlights()
	 * @see #addLineHighlight(int, Color)
	 */
	public void removeLineHighlight(Object tag) {
		if (lineHighlightManager!=null) {
			lineHighlightManager.removeLineHighlight(tag);
		}
	}


	/**
	 * Replaces text from the indicated start to end position with the
	 * new text specified.  Does nothing if the model is null.  Simply
	 * does a delete if the new string is null or empty.
	 * <p>
	 * This method is thread safe, although most Swing methods
	 * are not.<p>
	 * This method is overridden so that our Undo manager remembers it as a
	 * single operation (it has trouble with this, especially for
	 * <code>RSyntaxTextArea</code> and the "auto-indent" feature).
	 *
	 * @param str the text to use as the replacement
	 * @param start the start position >= 0
	 * @param end the end position >= start
	 * @exception IllegalArgumentException  if part of the range is an
	 *  invalid position in the model
	 * @see #insert(String, int)
	 * @see #replaceRange(String, int, int)
	 */
	public void replaceRange(String str, int start, int end) {
		if (end < start)
			throw new IllegalArgumentException("end before start");
		Document doc = getDocument();
		if (doc != null) {
			try {
				// Without this, in some cases we'll have to do two undos
				// for one logical operation (for example, try editing a
				// Java source file in an RSyntaxTextArea, and moving a line
				// with text already on it down via Enter.  Without this
				// line, doing a single "undo" moves all later text up,
				// but the first line moved down isn't there!  Doing a
				// second undo puts it back.
				undoManager.beginInternalAtomicEdit();
				((AbstractDocument)doc).replace(start, end - start,
                               		                     str, null);
			} catch (BadLocationException e) {
				throw new IllegalArgumentException(e.getMessage());
			} finally {
				undoManager.endInternalAtomicEdit();
			}
		}
    }


	/**
	 * This method overrides <code>JTextComponent</code>'s
	 * <code>replaceSelection</code>, so that if <code>textMode</code> is
	 * {@link #OVERWRITE_MODE}, it actually overwrites.
	 *
	 * @param text The content to replace the selection with.
	 */
	public void replaceSelection(String text) {

		// It's legal for null to be used here...
		if (text==null) {
			handleReplaceSelection(text);
			return;
		}

		if (getTabsEmulated() && text.indexOf('\t')>-1) {
			text = replaceTabsWithSpaces(text, getTabSize());
		}

		// If the user wants to overwrite text...
		if (textMode==OVERWRITE_MODE && !"\n".equals(text)) {

			Caret caret = getCaret();
			int caretPos = caret.getDot();
			Document doc = getDocument();
			Element map = doc.getDefaultRootElement();
			int curLine = map.getElementIndex(caretPos);
			int lastLine = map.getElementCount() - 1;

			try {

				// If we're not at the end of a line, select the characters
				// that will be overwritten (otherwise JTextArea will simply
				// insert in front of them).
				int curLineEnd = getLineEndOffset(curLine);
				if (caretPos==caret.getMark() && caretPos!=curLineEnd) {
					if (curLine==lastLine)
						caretPos = Math.min(caretPos+text.length(), curLineEnd);
					else
						caretPos = Math.min(caretPos+text.length(), curLineEnd-1);
					caret.moveDot(caretPos);//moveCaretPosition(caretPos);
				}

			} catch (BadLocationException ble) { // Never happens
				UIManager.getLookAndFeel().provideErrorFeedback(this);
				ble.printStackTrace();
			}

		} // End of if (textMode==OVERWRITE_MODE).

		// Now, actually do the inserting/replacing.  Our undoManager will
		// take care of remembering the remove/insert as atomic if we are in
		// overwrite mode.
		handleReplaceSelection(text);

	}


	private static StringBuffer repTabsSB;
	/**
	 * Replaces all instances of the tab character in <code>text</code> with
	 * the number of spaces equivalent to a tab in this text area.<p>
	 *
	 * This method should only be called from thread-safe methods, such as
	 * {@link #replaceSelection(String)}.
	 *
	 * @param text The <code>java.lang.String</code> in which to replace tabs
	 *        with spaces.  This has already been verified to have at least
	 *        one tab character in it.
	 * @return A <code>java.lang.String</code> just like <code>text</code>,
	 *         but with spaces instead of tabs.
	 */
	public static final String replaceTabsWithSpaces(String text,
			int tabSize) {

		String tabText = "";
		for (int i=0; i<tabSize; i++) {
			tabText += ' ';
		}

		// Common case: User's entering a single tab (pressed the tab key).
		if (text.length()==1) {
			return tabText;
		}

		// Otherwise, there may be more than one tab.  Manually search for
		// tabs for performance, as opposed to using String#replaceAll().
		// This method is called for each character inserted when "replace
		// tabs with spaces" is enabled, so we need to be quick.

		//return text.replaceAll("\t", tabText);
		if (repTabsSB==null) {
			repTabsSB = new StringBuffer();
		}
		repTabsSB.setLength(0);
		char[] array = text.toCharArray(); // Wouldn't be needed in 1.5!
		int oldPos = 0;
		int pos = 0;
		while ((pos=text.indexOf('\t', oldPos))>-1) {
			//repTabsSB.append(text, oldPos, pos); // Added in Java 1.5
			if (pos>oldPos) {
				repTabsSB.append(array, oldPos, pos-oldPos);
			}
			repTabsSB.append(tabText);
			oldPos = pos + 1;
		}
		if (oldPos<array.length) {
			repTabsSB.append(array, oldPos, array.length-oldPos);
		}

		return repTabsSB.toString();

	}



	/**
	 * Sets the properties of one of the actions this text area owns.
	 *
	 * @param action The action to modify; for example, {@link #CUT_ACTION}.
	 * @param name The new name for the action.
	 * @param mnemonic The new mnemonic for the action.
	 * @param accelerator The new accelerator key for the action.
	 */
	public static void setActionProperties(int action, String name,
							char mnemonic, KeyStroke accelerator) {
		setActionProperties(action, name, new Integer(mnemonic), accelerator);
	}


	/**
	 * Sets the properties of one of the actions this text area owns.
	 *
	 * @param action The action to modify; for example, {@link #CUT_ACTION}.
	 * @param name The new name for the action.
	 * @param mnemonic The new mnemonic for the action.
	 * @param accelerator The new accelerator key for the action.
	 */
	public static void setActionProperties(int action, String name,
							Integer mnemonic, KeyStroke accelerator) {

		Action tempAction = null;

		switch (action) {
			case CUT_ACTION:
				tempAction = cutAction;
				break;
			case COPY_ACTION:
				tempAction = copyAction;
				break;
			case PASTE_ACTION:
				tempAction = pasteAction;
				break;
			case DELETE_ACTION:
				tempAction = deleteAction;
				break;
			case SELECT_ALL_ACTION:
				tempAction = selectAllAction;
				break;
			case UNDO_ACTION:
			case REDO_ACTION:
			default:
				return;
		}

		tempAction.putValue(Action.NAME, name);
		tempAction.putValue(Action.SHORT_DESCRIPTION, name);
		tempAction.putValue(Action.ACCELERATOR_KEY, accelerator);
		tempAction.putValue(Action.MNEMONIC_KEY, mnemonic);

	}


	/**
	 * Sets the caret to use in this text area.  It is strongly encouraged to
	 * use {@link ConfigurableCaret}s (which is used by default), or a
	 * subclass, since they know how to render themselves differently when the
	 * user toggles between insert and overwrite modes.
	 *
	 * @param caret The caret to use.  If this is not an instance of
	 *        <code>ConfigurableCaret</code>, an exception is thrown.
	 * @throws IllegalArgumentException If the specified caret is not an
	 *         <code>ConfigurableCaret</code>.
	 * @see #setCaretStyle(int, int)
	 */
	public void setCaret(Caret caret) {
		super.setCaret(caret);
		if (carets!=null && // Called by setUI() before carets is initialized
				caret instanceof ConfigurableCaret) {
			((ConfigurableCaret)caret).setStyle(carets[getTextMode()]);
		}
	}


	/**
	 * Sets the style of caret used when in insert or overwrite mode.
	 *
	 * @param mode Either {@link #INSERT_MODE} or {@link #OVERWRITE_MODE}.
	 * @param style The style for the caret (such as
	 *        {@link ConfigurableCaret#VERTICAL_LINE_STYLE}).
	 * @see org.fife.ui.rtextarea.ConfigurableCaret
	 */
	public void setCaretStyle(int mode, int style) {
		style = (style>=ConfigurableCaret.MIN_STYLE &&
					style<=ConfigurableCaret.MAX_STYLE ?
						style : ConfigurableCaret.THICK_VERTICAL_LINE_STYLE);
		carets[mode] = style;
		if (mode==getTextMode() && getCaret() instanceof ConfigurableCaret) {
			// Will repaint the caret if necessary.
			((ConfigurableCaret)getCaret()).setStyle(style);
		}
	}


	/**
	 * Sets the document used by this text area.
	 *
	 * @param document The new document to use.
	 * @throws IllegalArgumentException If the document is not an instance of
	 *         {@link RDocument}.
	 */
	public void setDocument(Document document) {
		if (!(document instanceof RDocument)) {
			throw new IllegalArgumentException("RTextArea requires " +
				"instances of RDocument for its document");
		}
		if (undoManager!=null) { // First time through, undoManager==null
			Document old = getDocument();
			if (old!=null) {
				old.removeUndoableEditListener(undoManager);
			}
		}
		super.setDocument(document);
		if (undoManager!=null) {
			document.addUndoableEditListener(undoManager);
			discardAllEdits();
		}
	}


	/**
	 * Sets the path in which to find images to associate with the editor's
	 * actions.  The path MUST contain the following images (with the
	 * appropriate extension as defined by the icon group):<br>
	 * <ul>
	 *   <li>cut</li>
	 *   <li>copy</li>
	 *   <li>paste</li>
	 *   <li>delete</li>
	 *   <li>undo</li>
	 *   <li>redo</li>
	 *   <li>selectall</li>
	 * </ul>
	 * If any of the above images don't exist, the corresponding action will
	 * not have an icon.
	 *
	 * @param group The icon group to load.
	 * @see #getIconGroup()
	 */
	public static synchronized void setIconGroup(IconGroup group) {
		Icon icon = group.getIcon("cut");
		cutAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("copy");
		copyAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("paste");
		pasteAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("delete");
		deleteAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("undo");
		undoAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("redo");
		redoAction.putValue(Action.SMALL_ICON, icon);
		icon = group.getIcon("selectall");
		selectAllAction.putValue(Action.SMALL_ICON, icon);
		iconGroup = group;
	}


	/**
	 * Sets the color used for "mark all."  This fires a property change of
	 * type {@link #MARK_ALL_COLOR_PROPERTY}.
	 *
	 * @param color The color to use for "mark all."
	 * @see #getMarkAllHighlightColor()
	 */
	public void setMarkAllHighlightColor(Color color) {
		Color old = (Color)markAllHighlightPainter.getPaint();
		if (old!=null && !old.equals(color)) {
			markAllHighlightPainter.setPaint(color);
			if (markedWord!=null)
				repaint();	// Repaint if words are highlighted.
			firePropertyChange(MARK_ALL_COLOR_PROPERTY, old, color);
		}
	}


	/**
	 * Sets the popup menu used by this text area.<p>
	 * 
	 * If you set the popup menu with this method, you'll want to consider also
	 * overriding {@link #configurePopupMenu(JPopupMenu)}, especially if you
	 * removed any of the default menu items.
	 *
	 * @param popupMenu The popup menu.  If this is <code>null</code>, no
	 *        popup menu will be displayed.
	 * @see #getPopupMenu()
	 * @see #configurePopupMenu(JPopupMenu)
	 */
	public void setPopupMenu(JPopupMenu popupMenu) {
		this.popupMenu = popupMenu;
		popupMenuCreated = true;
	}


	/**
	 * {@inheritDoc}
	 */
	public void setRoundedSelectionEdges(boolean rounded) {
		if (getRoundedSelectionEdges()!=rounded) {
			markAllHighlightPainter.setRoundedEdges(rounded);
			super.setRoundedSelectionEdges(rounded); // Fires event.
		}
	}


	/**
	 * Sets the text last selected/Ctrl+K'd in an <code>RTextArea</code>.
	 * This text will be searched for in subsequent Ctrl+K/Ctrl+Shift+K
	 * actions (Cmd+K on OS X).<p>
	 *
	 * Since the selected occurrence actions are built into RTextArea,
	 * applications usually do not have to call this method directly, but can
	 * choose to do so if they wish (for example, if they wish to set this
	 * value when the user does a search via a Find dialog).
	 *
	 * @param text The selected text.
	 * @see #getSelectedOccurrenceText()
	 */
	public static void setSelectedOccurrenceText(String text) {
		selectedOccurrenceText = text;
	}


	/**
	 * Sets the text mode for this editor pane.  If the currently installed
	 * caret is an instance of {@link ConfigurableCaret}, it will be
	 * automatically updated to render itself appropriately for the new text
	 * mode.
	 *
	 * @param mode Either {@link #INSERT_MODE} or {@link #OVERWRITE_MODE}.
	 * @see #getTextMode()
	 */
	public void setTextMode(int mode) {

		if (mode!=INSERT_MODE && mode!=OVERWRITE_MODE)
			mode = INSERT_MODE;

		if (textMode != mode) {
			Caret caret = getCaret();
			if (caret instanceof ConfigurableCaret) {
				((ConfigurableCaret)caret).setStyle(carets[mode]);
			}
			textMode = mode;
		}

	}


	/**
	 * Sets the tool tip supplier.
	 *
	 * @param supplier The new tool tip supplier, or <code>null</code> if
	 *        there is to be no supplier.
	 * @see #getToolTipSupplier()
	 */
	public void setToolTipSupplier(ToolTipSupplier supplier) {
		this.toolTipSupplier = supplier;
	}


	/**
	 * Sets the UI used by this text area.  This is overridden so only the
	 * right-click popup menu's UI is updated.  The look and feel of an
	 * <code>RTextArea</code> is independent of the Java Look and Feel, and so
	 * this method does not change the text area itself.  Subclasses (such as
	 * <code>RSyntaxTextArea</code> can call <code>setRTextAreaUI</code> if
	 * they wish to install a new UI.
	 *
	 * @param ui This parameter is ignored.
	 */
	public final void setUI(TextUI ui) {

		// Update the popup menu's ui.
		if (popupMenu!=null) {
			SwingUtilities.updateComponentTreeUI(popupMenu);
		}

		// Set things like selection color, selected text color, etc. to
		// laf defaults (if values are null or UIResource instances).
		RTextAreaUI rtaui = (RTextAreaUI)getUI();
		if (rtaui!=null) {
			rtaui.installDefaults();
		}

	}


	/**
	 * Attempt to undo an "action" done in this text area.
	 *
	 * @see #redoLastAction()
	 */
	public void undoLastAction() {
		// NOTE: that the try/catch block shouldn't be necessary...
		try {
			if (undoManager.canUndo())
				undoManager.undo();
		}
		catch (CannotUndoException cre) {
			cre.printStackTrace();
		}
	}

	/**
	 * Serializes this text area.
	 *
	 * @param s The stream to write to.
	 * @throws IOException If an IO error occurs.
	 */
	private void writeObject(ObjectOutputStream s) throws IOException {

		// UndoManagers cannot be serialized without Exceptions.  See
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4275892
		getDocument().removeUndoableEditListener(undoManager);
		s.defaultWriteObject();
		getDocument().addUndoableEditListener(undoManager);

	}


	/**
	 * Modified from <code>MutableCaretEvent</code> in
	 * <code>JTextComponent</code> so that mouse events get fired when the user
	 * is selecting text with the mouse as well.  This class also displays the
	 * popup menu when the user right-clicks in the text area.
	 */
	protected class RTextAreaMutableCaretEvent extends RTAMouseListener {

		protected RTextAreaMutableCaretEvent(RTextArea textArea) {
			super(textArea);
		}

		public void focusGained(FocusEvent e) {
			Caret c = getCaret();
			boolean enabled = c.getDot()!=c.getMark();
			cutAction.setEnabled(enabled);
			copyAction.setEnabled(enabled);
			undoManager.updateActions(); // To reflect this text area.
		}

		public void focusLost(FocusEvent e) {
		}

		public void mouseDragged(MouseEvent e) {
			if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
				Caret caret = getCaret();
				dot = caret.getDot();
				mark = caret.getMark();
				fireCaretUpdate(this);
			}
		}

		public void mousePressed(MouseEvent e) {
			// WORKAROUND:  Since JTextComponent only updates the caret
			// location on mouse clicked and released, we'll do it on dragged
			// events when the left mouse button is clicked.
			if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
				Caret caret = getCaret();
				dot = caret.getDot();
				mark = caret.getMark();
				fireCaretUpdate(this);
			}
		}

		public void mouseReleased(MouseEvent e) {
			if ((e.getModifiers()&MouseEvent.BUTTON3_MASK)!=0)
				showPopup(e);
		}

		/**
		 * Shows a popup menu with cut, copy, paste, etc. options if the
		 * user clicked the right button.
		 *
		 * @param e The mouse event that caused this method to be called.
		 */
		private void showPopup(MouseEvent e) {
			JPopupMenu popupMenu = getPopupMenu();
			if (popupMenu!=null) {
				configurePopupMenu(popupMenu);
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

	}


}