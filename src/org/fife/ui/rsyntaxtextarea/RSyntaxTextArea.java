/*
 * 01/27/2004
 *
 * RSyntaxTextArea.java - An extension of RTextArea that adds
 * the ability to syntax highlight certain programming languages.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;

import org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip;
import org.fife.ui.rsyntaxtextarea.folding.Fold;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ToolTipInfo;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextAreaUI;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.RecordableTextAction;



/**
 * An extension of <code>RTextArea</code> that adds syntax highlighting
 * of certain programming languages to its list of features.  Languages
 * currently supported include:
 *
 * <table>
 *  <tr>
 *   <td style="vertical-align: top">
 *    <ul>
 *       <li>ActionScript
 *       <li>Assembler (X86)
 *       <li>BBCode
 *       <li>C
 *       <li>C++
 *       <li>CSS
 *       <li>C#
 *       <li>Clojure
 *       <li>Delphi
 *       <li>DTD
 *       <li>Fortran
 *       <li>Groovy
 *       <li>HTML
 *       <li>Java
 *       <li>JavaScript
 *       <li>JSP
 *    </ul>
 *   </td>
 *   <td style="vertical-align: top">
 *    <ul>
 *       <li>LaTeX
 *       <li>Lisp
 *       <li>Lua
 *       <li>Make
 *       <li>MXML
 *       <li>NSIS
 *       <li>Perl
 *       <li>PHP
 *       <li>Ruby
 *       <li>SAS
 *       <li>Scala
 *       <li>SQL
 *       <li>Tcl
 *       <li>UNIX shell scripts
 *       <li>Windows batch
 *       <li>XML files
 *    </ul>
 *   </td>
 *  </tr>
 * </table>
 *
 * Other added features include:
 * <ul style="columns: 2 12em; column-gap: 1em">
 *    <li>Code folding
 *    <li>Bracket matching
 *    <li>Auto-indentation
 *    <li>Copy as RTF
 *    <li>Clickable hyperlinks (if the language scanner being used supports it)
 *    <li>A pluggable "parser" system that can be used to implement syntax
 *        validation, spell checking, etc.
 * </ul>
 *
 * It is recommended that you use an instance of
 * {@link org.fife.ui.rtextarea.RTextScrollPane} instead of a regular
 * <code>JScrollPane</code> as this class allows you to add line numbers and
 * bookmarks easily to your text area.
 *
 * @author Robert Futrell
 * @version 2.0.5
 * @see TextEditorPane
 */
public class RSyntaxTextArea extends RTextArea implements SyntaxConstants {

	public static final String ANIMATE_BRACKET_MATCHING_PROPERTY		= "RSTA.animateBracketMatching";
	public static final String ANTIALIAS_PROPERTY						= "RSTA.antiAlias";
	public static final String AUTO_INDENT_PROPERTY						= "RSTA.autoIndent";
	public static final String BRACKET_MATCHING_PROPERTY				= "RSTA.bracketMatching";
	public static final String CLEAR_WHITESPACE_LINES_PROPERTY			= "RSTA.clearWhitespaceLines";
	public static final String CLOSE_CURLY_BRACES_PROPERTY				= "RSTA.closeCurlyBraces";
	public static final String CLOSE_MARKUP_TAGS_PROPERTY				= "RSTA.closeMarkupTags";
	public static final String CODE_FOLDING_PROPERTY					= "RSTA.codeFolding";
	public static final String EOL_VISIBLE_PROPERTY						= "RSTA.eolMarkersVisible";
	public static final String FOCUSABLE_TIPS_PROPERTY					= "RSTA.focusableTips";
	public static final String FRACTIONAL_FONTMETRICS_PROPERTY			= "RSTA.fractionalFontMetrics";
	public static final String HIGHLIGHT_SECONDARY_LANGUAGES_PROPERTY	= "RSTA.highlightSecondaryLanguages";
	public static final String HYPERLINKS_ENABLED_PROPERTY				= "RSTA.hyperlinksEnabled";
	public static final String MARK_OCCURRENCES_PROPERTY				= "RSTA.markOccurrences";
	public static final String MARKED_OCCURRENCES_CHANGED_PROPERTY		= "RSTA.markedOccurrencesChanged";
	public static final String PAINT_MATCHED_BRACKET_PAIR_PROPERTY		= "RSTA.paintMatchedBracketPair";
	public static final String PARSER_NOTICES_PROPERTY					= "RSTA.parserNotices";
	public static final String SYNTAX_SCHEME_PROPERTY					= "RSTA.syntaxScheme";
	public static final String SYNTAX_STYLE_PROPERTY					= "RSTA.syntaxStyle";
	public static final String TAB_LINE_COLOR_PROPERTY					= "RSTA.tabLineColor";
	public static final String TAB_LINES_PROPERTY						= "RSTA.tabLines";
	public static final String VISIBLE_WHITESPACE_PROPERTY				= "RSTA.visibleWhitespace";

	private static final Color DEFAULT_BRACKET_MATCH_BG_COLOR		= new Color(234,234,255);
	private static final Color DEFAULT_BRACKET_MATCH_BORDER_COLOR	= new Color(0,0,128);
	private static final Color DEFAULT_SELECTION_COLOR			= new Color(200,200,255);

	private static final String MSG	= "org.fife.ui.rsyntaxtextarea.RSyntaxTextArea";

	private JMenu foldingMenu;
	private static RecordableTextAction toggleCurrentFoldAction;
	private static RecordableTextAction collapseAllCommentFoldsAction;
	private static RecordableTextAction collapseAllFoldsAction;
	private static RecordableTextAction expandAllFoldsAction;

	/** The key for the syntax style to be highlighting. */
	private String syntaxStyleKey;

	/** The colors used for syntax highlighting. */
	private SyntaxScheme syntaxScheme;

	/** Handles code templates. */
	private static CodeTemplateManager codeTemplateManager;

	/** Whether or not templates are enabled. */
	private static boolean templatesEnabled;

	/**
	 * The rectangle surrounding the "matched bracket" if bracket matching
	 * is enabled.
	 */
	private Rectangle match;

	/**
	 * The rectangle surrounding the current offset if both bracket matching and
	 * "match both brackets" are enabled.
	 */
	private Rectangle dotRect;

	/**
	 * Colors used for the "matched bracket" if bracket matching is enabled.
	 */
	private Color matchedBracketBGColor;
	private Color matchedBracketBorderColor;

	/** The location of the last matched bracket. */
	private int lastBracketMatchPos;

	/** Whether or not bracket matching is enabled. */
	private boolean bracketMatchingEnabled;

	/** Whether or not bracket matching is animated. */
	private boolean animateBracketMatching;

	/** Whether <b>both</b> brackets are highlighted when bracket matching. */
	private boolean paintMatchedBracketPair;

	private BracketMatchingTimer bracketRepaintTimer;

	/**
	 * Whether or not auto-indent is on.
	 */
	private boolean autoIndentEnabled;

	/**
	 * Whether curly braces should be closed on Enter key presses, (if the
	 * current language supports it).
	 */
	private boolean closeCurlyBraces;

	/**
	 * Whether closing markup tags should be automatically completed when
	 * "<code>&lt;/</code>" is typed (if the current language is a markup
	 * language).
	 */
	private boolean closeMarkupTags;

	/**
	 * Whether or not lines with nothing but whitespace are "made empty."
	 */
	private boolean clearWhitespaceLines;

	/** Whether we are displaying visible whitespace (spaces and tabs). */
	private boolean whitespaceVisible;

	/** Whether EOL markers should be visible at the end of each line. */
	private boolean eolMarkersVisible;

	/** Whether tab lines are enabled. */
	private boolean paintTabLines;

	/** The color to use when painting tab lines. */
	private Color tabLineColor;

	/**
	 * Whether hyperlinks are enabled (must be supported by the syntax
	 * scheme being used).
	 */
	private boolean hyperlinksEnabled;

	/** The color to use when painting hyperlinks. */
	private Color hyperlinkFG;

	/**
	 * Mask used to determine if the correct key is being held down to scan
	 * for hyperlinks (ctrl, meta, etc.).
	 */
	private int linkScanningMask;

	/** Whether secondary languages have their backgrounds colored. */
	private boolean highlightSecondaryLanguages;

	/** Used during "Copy as RTF" operations. */
	private RtfGenerator rtfGenerator;

	/** Handles "mark occurrences" support. */
	private MarkOccurrencesSupport markOccurrencesSupport;

	/** The color used to render "marked occurrences." */
	private Color markOccurrencesColor;

	/** Whether a border should be painted around marked occurrences. */
	private boolean paintMarkOccurrencesBorder;

	/** Metrics of the text area's font. */
	private FontMetrics defaultFontMetrics;

	/** Manages running the parser. */
	private ParserManager parserManager;

	/**
	 * Whether the editor is currently scanning for hyperlinks on mouse
	 * movement.
	 */
	private boolean isScanningForLinks;

	private int hoveredOverLinkOffset;

	private FoldManager foldManager;

	/** Whether "focusable" tool tips are used instead of standard ones. */
	private boolean useFocusableTips;

	/** The last focusable tip displayed. */
	private FocusableTip focusableTip;

	/** Cached desktop anti-aliasing hints, if anti-aliasing is enabled. */
	private Map aaHints;

private int lineHeight;		// Height of a line of text; same for default, bold & italic.
private int maxAscent;
private boolean fractionalFontMetricsEnabled;

	private Color[] secondaryLanguageBackgrounds;


	/**
	 * Constructor.
	 */
	public RSyntaxTextArea() {
		init();
	}


	/**
	 * Constructor.
	 * 
	 * @param doc The document for the editor.
	 */
	public RSyntaxTextArea(RSyntaxDocument doc) {
		super(doc);
		init();
	}

	/**
	 * Constructor.
	 * 
	 * @param text The initial text to display.
	 */
	public RSyntaxTextArea(String text) {
		super(text);
		init();
	}


	/**
	 * Constructor.
	 * 
	 * @param rows The number of rows to display.
	 * @param cols The number of columns to display.
	 * @throws IllegalArgumentException If either <code>rows</code> or
	 *         <code>cols</code> is negative.
	 */
	public RSyntaxTextArea(int rows, int cols) {
		super(rows, cols);
		init();
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
	public RSyntaxTextArea(String text, int rows, int cols) {
		super(text, rows, cols);
		init();
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
	public RSyntaxTextArea(RSyntaxDocument doc, String text,int rows,int cols) {
		super(doc, text, rows, cols);
		init();
	}


	/**
	 * Creates a new <code>RSyntaxTextArea</code>.
	 *
	 * @param textMode Either <code>INSERT_MODE</code> or
	 *        <code>OVERWRITE_MODE</code>.
	 */
	public RSyntaxTextArea(int textMode) {
		super(textMode);
		init();
	}


	/**
	 * Adds an "active line range" listener to this text area.
	 *
	 * @param l The listener to add.
	 * @see #removeActiveLineRangeListener(ActiveLineRangeListener)
	 */
	public void addActiveLineRangeListener(ActiveLineRangeListener l) {
		listenerList.add(ActiveLineRangeListener.class, l);
	}


	/**
	 * Adds a hyperlink listener to this text area.
	 *
	 * @param l The listener to add.
	 * @see #removeHyperlinkListener(HyperlinkListener)
	 */
	public void addHyperlinkListener(HyperlinkListener l) {
		listenerList.add(HyperlinkListener.class, l);
	}


	/**
	 * Updates the font metrics the first time we're displayed.
	 */
	public void addNotify() {

		super.addNotify();

		// We know we've just been connected to a screen resource (by
		// definition), so initialize our font metrics objects.
		refreshFontMetrics(getGraphics2D(getGraphics()));

		// Re-start parsing if we were removed from one container and added
		// to another
		if (parserManager!=null) {
			parserManager.restartParsing();
		}

	}


	/**
	 * Adds the parser to "validate" the source code in this text area.  This
	 * can be anything from a spell checker to a "compiler" that verifies
	 * source code.
	 *
	 * @param parser The new parser.  A value of <code>null</code> will
	 *        do nothing.
	 * @see #getParser(int)
	 * @see #getParserCount()
	 * @see #removeParser(Parser)
	 */
	public void addParser(Parser parser) {
		if (parserManager==null) {
			parserManager = new ParserManager(this);
		}
		parserManager.addParser(parser);
	}


	/**
	 * Recalculates the height of a line in this text area and the
	 * maximum ascent of all fonts displayed.
	 */
	private void calculateLineHeight() {

		lineHeight = maxAscent = 0;

		// Each token style.
		for (int i=0; i<syntaxScheme.getStyleCount(); i++) {
			Style ss = syntaxScheme.getStyle(i);
			if (ss!=null && ss.font!=null) {
				FontMetrics fm = getFontMetrics(ss.font);
				int height = fm.getHeight();
				if (height>lineHeight)
					lineHeight = height;
				int ascent = fm.getMaxAscent();
				if (ascent>maxAscent)
					maxAscent = ascent;
			}
		}

		// The text area's (default) font).
		Font temp = getFont();
		FontMetrics fm = getFontMetrics(temp);
		int height = fm.getHeight();
		if (height>lineHeight) {
			lineHeight = height;
		}
		int ascent = fm.getMaxAscent();
		if (ascent>maxAscent) {
			maxAscent = ascent;
		}

	}


	/**
	 * Removes all parsers from this text area.
	 *
	 * @see #removeParser(Parser)
	 */
	public void clearParsers() {
		if (parserManager!=null) {
			parserManager.clearParsers();
		}
	}


	/**
	 * Clones a token list.  This is necessary as tokens are reused in
	 * {@link RSyntaxDocument}, so we can't simply use the ones we
	 * are handed from it.
	 *
	 * @param t The token list to clone.
	 * @return The clone of the token list.
	 */
	private Token cloneTokenList(Token t) {

		if (t==null) {
			return null;
		}

		Token clone = new DefaultToken();
		clone.copyFrom(t);
		Token cloneEnd = clone;

		while ((t=t.getNextToken())!=null) {
			Token temp = new DefaultToken();
			temp.copyFrom(t);
			cloneEnd.setNextToken(temp);
			cloneEnd = temp;
		}

		return clone;

	}


	/**
	 * Overridden to toggle the enabled state of various
	 * RSyntaxTextArea-specific menu items.
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

		super.configurePopupMenu(popupMenu);

		// They may have overridden createPopupMenu()...
		if (popupMenu!=null && popupMenu.getComponentCount()>0 &&
				foldingMenu!=null) {
			foldingMenu.setEnabled(foldManager.
						isCodeFoldingSupportedAndEnabled());
		}
	}


	/**
	 * Copies the currently selected text to the system clipboard, with
	 * any necessary style information (font, foreground color and background
	 * color).  Does nothing for <code>null</code> selections.
	 */
	public void copyAsRtf() {

		int selStart = getSelectionStart();
		int selEnd = getSelectionEnd();
		if (selStart==selEnd) {
			return;
		}

		// Make sure there is a system clipboard, and that we can write
		// to it.
		SecurityManager sm = System.getSecurityManager();
		if (sm!=null) {
			try {
				sm.checkSystemClipboardAccess();
			} catch (SecurityException se) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
				return;
			}
		}
		Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();

		// Create the RTF selection.
		RtfGenerator gen = getRTFGenerator();
		Token tokenList = getTokenListFor(selStart, selEnd);
		for (Token t=tokenList; t!=null; t=t.getNextToken()) {
			if (t.isPaintable()) {
				if (t.textCount==1 && t.text[t.textOffset]=='\n') {
					gen.appendNewline();
				}
				else {
					Font font = getFontForTokenType(t.type);
					Color bg = getBackgroundForToken(t);
					boolean underline = getUnderlineForToken(t);
					// Small optimization - don't print fg color if this
					// is a whitespace color.  Saves on RTF size.
					if (t.isWhitespace()) {
						gen.appendToDocNoFG(t.getLexeme(), font, bg, underline);
					}
					else {
						Color fg = getForegroundForToken(t);
						gen.appendToDoc(t.getLexeme(), font, fg, bg, underline);
					}
				}
			}
		}

		// Set the system clipboard contents to the RTF selection.
		RtfTransferable contents = new RtfTransferable(gen.getRtf().getBytes());
		//System.out.println("*** " + new String(gen.getRtf().getBytes()));
		try {
			cb.setContents(contents, null);
		} catch (IllegalStateException ise) {
			UIManager.getLookAndFeel().provideErrorFeedback(null);
			return;
		}

	}


	/**
	 * Returns the document to use for an <code>RSyntaxTextArea</code>
	 *
	 * @return The document.
	 */
	protected Document createDefaultModel() {
		return new RSyntaxDocument(SYNTAX_STYLE_NONE);
	}


	/**
	 * Returns the caret event/mouse listener for <code>RTextArea</code>s.
	 *
	 * @return The caret event/mouse listener.
	 */
	protected RTAMouseListener createMouseListener() {
		return new RSyntaxTextAreaMutableCaretEvent(this);
	}


	/**
	 * Overridden to add menu items related to cold folding.
	 *
	 * @return The popup menu.
	 */
	protected JPopupMenu createPopupMenu() {

		JPopupMenu popup = super.createPopupMenu();
		popup.addSeparator();

		ResourceBundle bundle = ResourceBundle.getBundle(MSG);
		foldingMenu = new JMenu(bundle.getString("ContextMenu.Folding"));
		foldingMenu.add(createPopupMenuItem(toggleCurrentFoldAction));
		foldingMenu.add(createPopupMenuItem(collapseAllCommentFoldsAction));
		foldingMenu.add(createPopupMenuItem(collapseAllFoldsAction));
		foldingMenu.add(createPopupMenuItem(expandAllFoldsAction));
		popup.add(foldingMenu);

		return popup;

	}


	/**
	 * See createPopupMenuActions() in RTextArea.
	 * TODO: Remove these horrible hacks and move localizing of actions into
	 * the editor kits, where it should be!  The context menu should contain
	 * actions from the editor kits.
	 */
	private static void createRstaPopupMenuActions() {

		ResourceBundle msg = ResourceBundle.getBundle(MSG);

		toggleCurrentFoldAction = new RSyntaxTextAreaEditorKit.
				ToggleCurrentFoldAction();
		toggleCurrentFoldAction.setProperties(msg, "Action.ToggleCurrentFold");

		collapseAllCommentFoldsAction = new RSyntaxTextAreaEditorKit.
				CollapseAllCommentFoldsAction();
		collapseAllCommentFoldsAction.setProperties(msg, "Action.CollapseCommentFolds");

		collapseAllFoldsAction = new RSyntaxTextAreaEditorKit.CollapseAllFoldsAction(true);
		expandAllFoldsAction = new RSyntaxTextAreaEditorKit.ExpandAllFoldsAction(true);

	}


	/**
	 * Returns the a real UI to install on this text area.
	 *
	 * @return The UI.
	 */
	protected RTextAreaUI createRTextAreaUI() {
		return new RSyntaxTextAreaUI(this);
	}


	/**
	 * If the caret is on a bracket, this method finds the matching bracket,
	 * and if it exists, highlights it.
	 */
	protected final void doBracketMatching() {

		// We always need to repaint the "matched bracket" highlight if it
		// exists.
		if (match!=null) {
			repaint(match);
			if (dotRect!=null) {
				repaint(dotRect);
			}
		}

		// If a matching bracket is found, get its bounds and paint it!
		int pos = RSyntaxUtilities.getMatchingBracketPosition(this);
		if (pos>-1 && pos!=lastBracketMatchPos) {
			try {
				match = modelToView(pos);
				if (match!=null) { // Happens if we're not yet visible
					if (getPaintMatchedBracketPair()) {
						dotRect = modelToView(getCaretPosition()-1);
					}
					else {
						dotRect = null;
					}
					if (getAnimateBracketMatching()) {
						bracketRepaintTimer.restart();
					}
					repaint(match);
					if (dotRect!=null) {
						repaint(dotRect);
					}
				}
			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Shouldn't happen.
			}
		}
		else if (pos==-1) {
			// Set match to null so the old value isn't still repainted.
			match = null;
			dotRect = null;
			bracketRepaintTimer.stop();
		}
		lastBracketMatchPos = pos;

	}


	/**
	 * Notifies all listeners that a caret change has occurred.
	 *
	 * @param e The caret event.
	 */
	protected void fireCaretUpdate(CaretEvent e) {
		super.fireCaretUpdate(e);
		if (isBracketMatchingEnabled()) {
			doBracketMatching();
		}
	}


	/**
	 * Notifies all listeners that the active line range has changed.
	 *
	 * @param min The minimum "active" line, or <code>-1</code>.
	 * @param max The maximum "active" line, or <code>-1</code>.
	 */
	private void fireActiveLineRangeEvent(int min, int max) {
		ActiveLineRangeEvent e = null; // Lazily created
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==ActiveLineRangeListener.class) {
				if (e==null) {
					e = new ActiveLineRangeEvent(this, min, max);
				}
				((ActiveLineRangeListener)listeners[i+1]).activeLineRangeChanged(e);
			}
		}
	}


	/**
	 * Notifies all listeners that have registered interest for notification
	 * on this event type.  The listener list is processed last to first.
	 *
	 * @param e The event to fire.
	 * @see EventListenerList
	 */
	private void fireHyperlinkUpdate(HyperlinkEvent e) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length-2; i>=0; i-=2) {
			if (listeners[i]==HyperlinkListener.class) {
				((HyperlinkListener)listeners[i+1]).hyperlinkUpdate(e);
			}          
		}
	}


	/**
	 * Notifies listeners that the marked occurrences for this text area
	 * have changed.
	 */
	void fireMarkedOccurrencesChanged() {
		firePropertyChange(RSyntaxTextArea.MARKED_OCCURRENCES_CHANGED_PROPERTY,
							null, null);
	}


	/**
	 * Fires a notification that the parser notices for this text area have
	 * changed.
	 */
	void fireParserNoticesChange() {
		firePropertyChange(PARSER_NOTICES_PROPERTY, null, null);
	}


	/**
	 * Called whenever a fold is collapsed or expanded.  This causes the
	 * text editor to revalidate.  This method is here because of poor design
	 * and should be removed.
	 *
	 * @param fold The fold that was collapsed or expanded.
	 */
	public void foldToggled(Fold fold) {
		match = null; // TODO: Update the bracket rect rather than hide it
		dotRect = null;
		possiblyUpdateCurrentLineHighlightLocation();
		revalidate();
		repaint();
	}


	/**
	 * Forces the given {@link Parser} to re-parse the content of this text
	 * area.<p>
	 * 
	 * This method can be useful when a <code>Parser</code> can be configured
	 * as to what notices it returns.  For example, if a Java language parser
	 * can be configured to set whether no serialVersionUID is a warning,
	 * error, or ignored, this method can be called after changing the expected
	 * notice type to have the document re-parsed.
	 *
	 * @param parser The index of the <code>Parser</code> to re-run.
	 * @see #getParser(int)
	 */
	public void forceReparsing(int parser) {
		parserManager.forceReparsing(parser);
	}


	/**
	 * Forces re-parsing with a specific parser.  Note that if this parser is
	 * not installed on this text area, nothing will happen.
	 *
	 * @param parser The parser that should re-parse this text area's contents.
	 *        This should be installed on this text area.
	 * @return Whether the parser was installed on this text area.
	 * @see #forceReparsing(int)
	 */
	public boolean forceReparsing(Parser parser) {
		for (int i=0; i<getParserCount(); i++) {
			if (getParser(i)==parser) {
				forceReparsing(i);
				return true;
			}
		}
		return false;
	}


	/**
	 * Returns whether bracket matching should be animated.
	 *
	 * @return Whether bracket matching should be animated.
	 * @see #setAnimateBracketMatching(boolean)
	 */
	public boolean getAnimateBracketMatching() {
		return animateBracketMatching;
	}


	/**
	 * Returns whether anti-aliasing is enabled in this editor.
	 *
	 * @return Whether anti-aliasing is enabled in this editor.
	 * @see #setAntiAliasingEnabled(boolean)
	 * @see #getFractionalFontMetricsEnabled()
	 */
	public boolean getAntiAliasingEnabled() {
		return aaHints!=null;
	}


	/**
	 * Returns the background color for a token.
	 *
	 * @param token The token.
	 * @return The background color to use for that token.  If this value is
	 *         is <code>null</code> then this token has no special background
	 *         color.
	 * @see #getForegroundForToken(Token)
	 */
	public Color getBackgroundForToken(Token token) {
		Color c = null;
		if (getHighlightSecondaryLanguages()) {
			// 1-indexed, since 0 == main language.
			int languageIndex = token.getLanguageIndex() - 1;
			if (languageIndex>=0 &&
					languageIndex<secondaryLanguageBackgrounds.length) {
				c = secondaryLanguageBackgrounds[languageIndex];
			}
		}
		if (c==null) {
			c = syntaxScheme.getStyle(token.type).background;
		}
		// Don't default to this.getBackground(), as Tokens simply don't
		// paint a background if they get a null Color.
		return c;
	}


	/**
	 * Returns whether curly braces should be automatically closed when a
	 * newline is entered after an opening curly brace.  Note that this
	 * property is only honored for languages that use curly braces to denote
	 * code blocks.
	 *
	 * @return Whether curly braces should be automatically closed.
	 * @see #setCloseCurlyBraces(boolean)
	 */
	public boolean getCloseCurlyBraces() {
		return closeCurlyBraces;
	}


	/**
	 * Returns whether closing markup tags should be automatically completed
	 * when "<code>&lt;/</code>" is typed.  Note that this property is only
	 * honored for markup languages, such as HTML, XML and PHP.
	 *
	 * @return Whether closing markup tags should be automatically completed.
	 * @see #setCloseMarkupTags(boolean)
	 */
	public boolean getCloseMarkupTags() {
		return closeMarkupTags;
	}


	/**
	 * Returns the code template manager for all instances of
	 * <code>RSyntaxTextArea</code>.  The manager is lazily created.
	 *
	 * @return The code template manager.
	 * @see #setTemplatesEnabled(boolean)
	 */
	public static synchronized CodeTemplateManager getCodeTemplateManager() {
		if (codeTemplateManager==null) {
			codeTemplateManager = new CodeTemplateManager();
		}
		return codeTemplateManager;
	}


	/**
	 * Returns the default bracket-match background color.
	 *
	 * @return The color.
	 * @see #getDefaultBracketMatchBorderColor
	 */
	public static final Color getDefaultBracketMatchBGColor() {
		return DEFAULT_BRACKET_MATCH_BG_COLOR;
	}


	/**
	 * Returns the default bracket-match border color.
	 *
	 * @return The color.
	 * @see #getDefaultBracketMatchBGColor
	 */
	public static final Color getDefaultBracketMatchBorderColor() {
		return DEFAULT_BRACKET_MATCH_BORDER_COLOR;
	}


	/**
	 * Returns the default selection color for this text area.  This
	 * color was chosen because it's light and <code>RSyntaxTextArea</code>
	 * does not change text color between selected/unselected text for
	 * contrast like regular <code>JTextArea</code>s do.
	 *
	 * @return The default selection color.
	 */
	public static Color getDefaultSelectionColor() {
		return DEFAULT_SELECTION_COLOR;
	}


	/**
	 * Returns the "default" syntax highlighting color scheme.  The colors
	 * used are somewhat standard among syntax highlighting text editors.
	 *
	 * @return The default syntax highlighting color scheme.
	 * @see #restoreDefaultSyntaxScheme()
	 * @see #getSyntaxScheme()
	 * @see #setSyntaxScheme(SyntaxScheme)
	 */
	public SyntaxScheme getDefaultSyntaxScheme() {
		return new SyntaxScheme(getFont());
	}


	/**
	 * Returns whether an EOL marker should be drawn at the end of each line.
	 *
	 * @return Whether EOL markers should be visible.
	 * @see #setEOLMarkersVisible(boolean)
	 * @see #isWhitespaceVisible()
	 */
	public boolean getEOLMarkersVisible() {
		return eolMarkersVisible;
	}


	/**
	 * Returns the fold manager for this text area.
	 *
	 * @return The fold manager.
	 */
	public FoldManager getFoldManager() {
		return foldManager;
	}


	/**
	 * Returns the font for tokens of the specified type.
	 *
	 * @param type The type of token.
	 * @return The font to use for that token type.
	 * @see #getFontMetricsForTokenType(int)
	 */
	public Font getFontForTokenType(int type) {
		Font f = syntaxScheme.getStyle(type).font;
		return f!=null ? f : getFont();
	}


	/**
	 * Returns the font metrics for tokens of the specified type.
	 *
	 * @param type The type of token.
	 * @return The font metrics to use for that token type.
	 * @see #getFontForTokenType(int)
	 */
	public FontMetrics getFontMetricsForTokenType(int type) {
		FontMetrics fm = syntaxScheme.getStyle(type).fontMetrics;
		return fm!=null ? fm : defaultFontMetrics;
	}


	/**
	 * Returns the foreground color to use when painting a token.
	 *
	 * @param t The token.
	 * @return The foreground color to use for that token.  This
	 *         value is never <code>null</code>.
	 * @see #getBackgroundForToken(Token)
	 */
	public Color getForegroundForToken(Token t) {
		if (getHyperlinksEnabled() && t.isHyperlink() &&
				hoveredOverLinkOffset==t.offset) {
			return hyperlinkFG;
		}
		return getForegroundForTokenType(t.type);
	}


	/**
	 * Returns the foreground color to use when painting a token.  This does
	 * not take into account whether the token is a hyperlink.
	 *
	 * @param type The token type.
	 * @return The foreground color to use for that token.  This
	 *         value is never <code>null</code>.
	 * @see #getForegroundForToken(Token)
	 */
	public Color getForegroundForTokenType(int type) {
		Color fg = syntaxScheme.getStyle(type).foreground;
		return fg!=null ? fg : getForeground();
	}


	/**
	 * Returns whether fractional font metrics are enabled for this text area.
	 *
	 * @return Whether fractional font metrics are enabled.
	 * @see #setFractionalFontMetricsEnabled
	 * @see #getAntiAliasingEnabled()
	 */
	public boolean getFractionalFontMetricsEnabled() {
		return fractionalFontMetricsEnabled;
	}


	/**
	 * Returns a <code>Graphics2D</code> version of the specified graphics
	 * that has been initialized with the proper rendering hints.
	 *
	 * @param g The graphics context for which to get a
	 *        <code>Graphics2D</code>.
	 * @return The <code>Graphics2D</code>.
	 */
	private final Graphics2D getGraphics2D(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		if (aaHints!=null) {
			g2d.addRenderingHints(aaHints);
		}
		if (fractionalFontMetricsEnabled) {
			g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
							RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		}
		return g2d;
	}


	/**
	 * Returns whether "secondary" languages should have their backgrounds
	 * colored differently to visually differentiate them.  This feature
	 * imposes a fair performance penalty.
	 *
	 * @return Whether secondary languages have their backgrounds colored
	 *         differently.
	 * @see #setHighlightSecondaryLanguages(boolean)
	 * @see #getSecondaryLanguageBackground(int)
	 * @see #getSecondaryLanguageCount()
	 * @see #setSecondaryLanguageBackground(int, Color)
	 */
	public boolean getHighlightSecondaryLanguages() {
		return highlightSecondaryLanguages;
	}


	/**
	 * Returns the color to use when painting hyperlinks.
	 *
	 * @return The color to use when painting hyperlinks.
	 * @see #setHyperlinkForeground(Color)
	 * @see #getHyperlinksEnabled()
	 */
	public Color getHyperlinkForeground() {
		return hyperlinkFG;
	}


	/**
	 * Returns whether hyperlinks are enabled for this text area.
	 *
	 * @return Whether hyperlinks are enabled for this text area.
	 * @see #setHyperlinksEnabled(boolean)
	 */
	public boolean getHyperlinksEnabled() {
		return hyperlinksEnabled;
	}


	/**
	 * Returns the last visible offset in this text area.  This may not be the
	 * length of the document if code folding is enabled.
	 *
	 * @return The last visible offset in this text area.
	 */
	public int getLastVisibleOffset() {
		if (isCodeFoldingEnabled()) {
			int lastVisibleLine = foldManager.getLastVisibleLine();
			if (lastVisibleLine<getLineCount()-1) { // Not the last line
				try {
					return getLineEndOffset(lastVisibleLine) - 1;
				} catch (BadLocationException ble) { // Never happens
					ble.printStackTrace();
				}
			}
		}
		return getDocument().getLength();
	}


	/**
	 * Returns the height to use for a line of text in this text area.
	 *
	 * @return The height of a line of text in this text area.
	 */
	public int getLineHeight() {
		//System.err.println("... getLineHeight() returning " + lineHeight);
		return lineHeight;
	}


	/**
	 * Returns a list of "marked occurrences" in the text area.  If there are
	 * no marked occurrences, this will be an empty list.
	 *
	 * @return The list of marked occurrences.
	 */
	public List getMarkedOccurrences() {
		return ((RSyntaxTextAreaHighlighter)getHighlighter()).
											getMarkedOccurrences();
	}


	/**
	 * Returns whether "Mark Occurrences" is enabled.
	 *
	 * @return Whether "Mark Occurrences" is enabled.
	 * @see #setMarkOccurrences(boolean)
	 */
	public boolean getMarkOccurrences() {
		return markOccurrencesSupport!=null;
	}


	/**
	 * Returns the color used to "mark occurrences."
	 *
	 * @return The mark occurrences color.
	 * @see #setMarkOccurrencesColor(Color)
	 */
	public Color getMarkOccurrencesColor() {
		return markOccurrencesColor;
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
		RSyntaxDocument doc = (RSyntaxDocument)getDocument();
		return doc.getMarkOccurrencesOfTokenType(type);
	}


	/**
	 * Gets the color used as the background for a matched bracket.
	 *
	 * @return The color used.  If this is <code>null</code>, no special
	 *         background is painted behind a matched bracket.
	 * @see #setMatchedBracketBGColor
	 * @see #getMatchedBracketBorderColor
	 */
	public Color getMatchedBracketBGColor() {
		return matchedBracketBGColor;
	}


	/**
	 * Gets the color used as the border for a matched bracket.
	 *
	 * @return The color used.
	 * @see #setMatchedBracketBorderColor
	 * @see #getMatchedBracketBGColor
	 */
	public Color getMatchedBracketBorderColor() {
		return matchedBracketBorderColor;
	}


	/**
	 * Returns the caret's offset's rectangle, or <code>null</code> if there
	 * is currently no matched bracket, bracket matching is disabled, or "paint
	 * both matched brackets" is disabled.  This should never be called by the
	 * programmer directly.
	 *
	 * @return The rectangle surrounding the matched bracket.
	 * @see #getMatchRectangle()
	 */
	Rectangle getDotRectangle() {
		return dotRect;
	}


	/**
	 * Returns the matched bracket's rectangle, or <code>null</code> if there
	 * is currently no matched bracket.  This should never be called by the
	 * programmer directly.
	 *
	 * @return The rectangle surrounding the matched bracket.
	 * @see #getDotRectangle()
	 */
	Rectangle getMatchRectangle() {
		return match;
	}


	/**
	 * Overridden to return the max ascent for any font used in the editor.
	 *
	 * @return The max ascent value.
	 */
	public int getMaxAscent() {
		return maxAscent;
	}


	/**
	 * Returns whether the bracket at the caret position is painted as a
	 * "match" when a matched bracket is found.  Note that this property does
	 * nothing if {@link #isBracketMatchingEnabled()} returns
	 * <code>false</code>.
	 * 
	 * @return Whether both brackets in a bracket pair are highlighted when
	 *         bracket matching is enabled.
	 * @see #setPaintMatchedBracketPair(boolean)
	 * @see #isBracketMatchingEnabled()
	 * @see #setBracketMatchingEnabled(boolean)
	 */
	public boolean getPaintMatchedBracketPair() {
		return paintMatchedBracketPair;
	}


	/**
	 * Returns whether tab lines are painted.
	 *
	 * @return Whether tab lines are painted.
	 * @see #setPaintTabLines(boolean)
	 * @see #getTabLineColor()
	 */
	public boolean getPaintTabLines() {
		return paintTabLines;
	}


	/**
	 * Returns the specified parser.
	 *
	 * @param index The {@link Parser} to retrieve.
	 * @return The <code>Parser</code>.
	 * @see #getParserCount()
	 * @see #addParser(Parser)
	 */
	public Parser getParser(int index) {
		return parserManager.getParser(index);
	}


	/**
	 * Returns the number of parsers operating on this text area.
	 *
	 * @return The parser count.
	 * @see #addParser(Parser)
	 */
	public int getParserCount() {
		return parserManager==null ? 0 : parserManager.getParserCount();
	}


	/**
	 * Returns a list of the current parser notices for this text area.
	 * This method (like most Swing methods) should only be called on the
	 * EDT.
	 *
	 * @return The list of notices.  This will be an empty list if there are
	 *         none.
	 */
	public List getParserNotices() {
		return parserManager==null ? Collections.EMPTY_LIST :
									parserManager.getParserNotices();
	}


	/**
	 * Returns the RTF generator for this text area, lazily creating it
	 * if necessary.
	 *
	 * @return The RTF generator.
	 */
	private RtfGenerator getRTFGenerator() {
		if (rtfGenerator==null) {
			rtfGenerator = new RtfGenerator();
		}
		else {
			rtfGenerator.reset();
		}
		return rtfGenerator;
	}


	/**
	 * If auto-indent is enabled, this method returns whether a new line after
	 * this one should be indented (based on the standard indentation rules for
	 * the current programming language). For example, in Java, for a line
	 * containing:
	 * 
	 * <pre>
	 * for (int i=0; i<10; i++) {
	 * </pre>
	 * 
	 * the following line should be indented.
	 *
	 * @param line The line to check.
	 * @return Whether a line inserted after this one should be auto-indented.
	 *         If auto-indentation is disabled, this will always return
	 *         <code>false</code>.
	 * @see #isAutoIndentEnabled()
	 */
	public boolean getShouldIndentNextLine(int line) {
		if (isAutoIndentEnabled()) {
			RSyntaxDocument doc = (RSyntaxDocument)getDocument();
			return doc.getShouldIndentNextLine(line);
		}
		return false;
	}


	/**
	 * Returns what type of syntax highlighting this editor is doing.
	 *
	 * @return The style being used, such as
	 *         {@link SyntaxConstants#SYNTAX_STYLE_JAVA}.
	 * @see #setSyntaxEditingStyle(String)
	 * @see SyntaxConstants
	 */
	public String getSyntaxEditingStyle() {
		return syntaxStyleKey;
	}


	/**
	 * Returns all of the colors currently being used in syntax highlighting
	 * by this text component.
	 *
	 * @return An instance of <code>SyntaxScheme</code> that represents
	 *         the colors currently being used for syntax highlighting.
	 * @see #setSyntaxScheme(SyntaxScheme)
	 */
	public SyntaxScheme getSyntaxScheme() {
		return syntaxScheme;
	}


	/**
	 * Returns the color used to paint tab lines.
	 *
	 * @return The color used to paint tab lines.
	 * @see #setTabLineColor(Color)
	 * @see #getPaintTabLines()
	 * @see #setPaintTabLines(boolean)
	 */
	public Color getTabLineColor() {
		return tabLineColor;
	}


	/**
	 * Returns whether a border is painted around marked occurrences.
	 *
	 * @return Whether a border is painted.
	 * @see #setPaintMarkOccurrencesBorder(boolean)
	 * @see #getMarkOccurrencesColor()
	 * @see #getMarkOccurrences()
	 */
	public boolean getPaintMarkOccurrencesBorder() {
		return paintMarkOccurrencesBorder;
	}


	/**
	 * Returns the background color for the specified secondary language.
	 *
	 * @param index The language index.  Note that these are 1-based, not
	 *        0-based, and should be in the range
	 *        <code>1-getSecondaryLanguageCount()</code>, inclusive.
	 * @return The color, or <code>null</code> if none.
	 * @see #getSecondaryLanguageCount()
	 * @see #setSecondaryLanguageBackground(int, Color)
	 * @see #getHighlightSecondaryLanguages()
	 */
	public Color getSecondaryLanguageBackground(int index) {
		return secondaryLanguageBackgrounds[index];
	}


	/**
	 * Returns the number of secondary language backgrounds.
	 *
	 * @return The number of secondary language backgrounds.
	 * @see #getSecondaryLanguageBackground(int)
	 * @see #setSecondaryLanguageBackground(int, Color)
	 * @see #getHighlightSecondaryLanguages()
	 */
	public int getSecondaryLanguageCount() {
		return secondaryLanguageBackgrounds.length;
	}


	/**
	 * Returns whether or not templates are enabled for all instances
	 * of <code>RSyntaxTextArea</code>.
	 *
	 * @return Whether templates are enabled.
	 * @see #saveTemplates()
	 * @see #setTemplateDirectory(String)
	 * @see #setTemplatesEnabled(boolean)
	 */
	public static synchronized boolean getTemplatesEnabled() {
		return templatesEnabled;
	}


	/**
	 * Returns a token list for the given range in the document.
	 *
	 * @param startOffs The starting offset in the document.
	 * @param endOffs The end offset in the document.
	 * @return The first token in the token list.
	 */
	private Token getTokenListFor(int startOffs, int endOffs) {

		Token tokenList = null;
		Token lastToken = null;

		Element map = getDocument().getDefaultRootElement();
		int startLine = map.getElementIndex(startOffs);
		int endLine = map.getElementIndex(endOffs);

		for (int line=startLine; line<=endLine; line++) {
			Token t = getTokenListForLine(line);
			t = cloneTokenList(t);
			if (tokenList==null) {
				tokenList = t;
				lastToken = tokenList;
			}
			else {
				lastToken.setNextToken(t);
			}
			while (lastToken.getNextToken()!=null &&
					lastToken.getNextToken().isPaintable()) {
				lastToken = lastToken.getNextToken();
			}
			if (line<endLine) {
				// Document offset MUST be correct to prevent exceptions
				// in getTokenListFor()
				int docOffs = map.getElement(line).getEndOffset()-1;
				t = new DefaultToken(new char[] { '\n' }, 0,0, docOffs,
								Token.WHITESPACE);
				lastToken.setNextToken(t);
				lastToken = t;
			}
		}

		// Trim the beginning and end of the token list so that it starts
		// at startOffs and ends at endOffs.

		// Be careful and check that startOffs is actually in the list.
		// startOffs can be < the token list's start if the end "newline"
		// character of a line is the first character selected (the token
		// list returned for that line will be null, so the first token in
		// the final token list will be from the next line and have a
		// starting offset > startOffs?).
		if (startOffs>=tokenList.offset) {
			while (!tokenList.containsPosition(startOffs)) {
				tokenList = tokenList.getNextToken();
			}
			tokenList.makeStartAt(startOffs);
		}

		Token temp = tokenList;
		// Be careful to check temp for null here.  It is possible that no
		// token contains endOffs, if endOffs is at the end of a line.
		while (temp!=null && !temp.containsPosition(endOffs)) {
			temp = temp.getNextToken();
		}
		if (temp!=null) {
			temp.textCount = endOffs - temp.offset;
			temp.setNextToken(null);
		}

		return tokenList;

	}


	/**
	 * Returns a list of tokens representing the given line.
	 *
	 * @param line The line number to get tokens for.
	 * @return A linked list of tokens representing the line's text.
	 */
	public Token getTokenListForLine(int line) {
		return ((RSyntaxDocument)getDocument()).getTokenListForLine(line);
	}


	/**
	 * Returns the tool tip to display for a mouse event at the given
	 * location.  This method is overridden to give a registered parser a
	 * chance to display a tool tip (such as an error description when the
	 * mouse is over an error highlight).
	 *
	 * @param e The mouse event.
	 */
	public String getToolTipText(MouseEvent e) {

		// Check parsers for tool tips first.
		String text = null;
		URL imageBase = null;
		if (parserManager!=null) {
			ToolTipInfo info = parserManager.getToolTipText(e);
			if (info!=null) { // Should always be true
				text = info.getToolTipText(); // May be null
				imageBase = info.getImageBase(); // May be null
			}
		}
		if (text==null) {
			text = super.getToolTipText(e);
		}

		// Do we want to use "focusable" tips?
		if (getUseFocusableTips()) {
			if (text!=null) {
				if (focusableTip==null) {
					focusableTip = new FocusableTip(this, parserManager);
				}
				focusableTip.setImageBase(imageBase);
				focusableTip.toolTipRequested(e, text);
			}
			// No tool tip text at new location - hide tip window if one is
			// currently visible
			else if (focusableTip!=null) {
				focusableTip.possiblyDisposeOfTipWindow();
			}
			return null;
		}

		return text; // Standard tool tips

	}


	/**
	 * Returns whether the specified token should be underlined.
	 * A token is underlined if its syntax style includes underlining,
	 * or if it is a hyperlink and hyperlinks are enabled.
	 *
	 * @param t The token.
	 * @return Whether the specified token should be underlined.
	 */
	public boolean getUnderlineForToken(Token t) {
		return (t.isHyperlink() && getHyperlinksEnabled()) ||
				syntaxScheme.getStyle(t.type).underline;
	}


	/**
	 * Returns whether "focusable" tool tips are used instead of standard
	 * ones.  Focusable tool tips are tool tips that the user can click on,
	 * resize, copy from, and click links in.
	 *
	 * @return Whether to use focusable tool tips.
	 * @see #setUseFocusableTips(boolean)
	 * @see FocusableTip
	 */
	public boolean getUseFocusableTips() {
		return useFocusableTips;
	}


	/**
	 * Called by constructors to initialize common properties of the text
	 * editor.
	 */
	protected void init() {

		// NOTE: Our actions are created here instead of in a static block
		// so they are only created when the first RTextArea is instantiated,
		// not before.  There have been reports of users calling static getters
		// (e.g. RSyntaxTextArea.getDefaultBracketMatchBGColor()) which would
		// cause these actions to be created and (possibly) incorrectly
		// localized, if they were in a static block.
		if (toggleCurrentFoldAction==null) {
			createRstaPopupMenuActions();
		}

		// Set some RSyntaxTextArea default values.
		syntaxStyleKey = SYNTAX_STYLE_NONE;
		setMatchedBracketBGColor(getDefaultBracketMatchBGColor());
		setMatchedBracketBorderColor(getDefaultBracketMatchBorderColor());
		setBracketMatchingEnabled(true);
		setAnimateBracketMatching(true);
		lastBracketMatchPos = -1;
		setSelectionColor(getDefaultSelectionColor());
		setTabLineColor(null);
		setMarkOccurrencesColor(MarkOccurrencesSupport.DEFAULT_COLOR);

		foldManager = new FoldManager(this);

		// Set auto-indent related stuff.
		setAutoIndentEnabled(true);
		setCloseCurlyBraces(true);
		setCloseMarkupTags(true);
		setClearWhitespaceLinesEnabled(true);

		setHyperlinksEnabled(true);
		setLinkScanningMask(InputEvent.CTRL_DOWN_MASK);
		setHyperlinkForeground(Color.BLUE);
		isScanningForLinks = false;
		setUseFocusableTips(true);

		setAntiAliasingEnabled(true);
		restoreDefaultSyntaxScheme();

		setHighlightSecondaryLanguages(true);
		secondaryLanguageBackgrounds = new Color[3];
		secondaryLanguageBackgrounds[0] = new Color(0xfff0cc);
		secondaryLanguageBackgrounds[1] = new Color(0xdafeda);
		secondaryLanguageBackgrounds[2] = new Color(0xffe0f0);

	}


	/**
	 * Returns whether or not auto-indent is enabled.
	 *
	 * @return Whether or not auto-indent is enabled.
	 * @see #setAutoIndentEnabled(boolean)
	 */
	public boolean isAutoIndentEnabled() {
		return autoIndentEnabled;
	}


	/**
	 * Returns whether or not bracket matching is enabled.
	 *
	 * @return <code>true</code> iff bracket matching is enabled.
	 * @see #setBracketMatchingEnabled
	 */
	public final boolean isBracketMatchingEnabled() {
		return bracketMatchingEnabled;
	}


	/**
	 * Returns whether or not lines containing nothing but whitespace are made
	 * into blank lines when Enter is pressed in them.
	 *
	 * @return Whether or not whitespace-only lines are cleared when
	 *         the user presses Enter on them.
	 * @see #setClearWhitespaceLinesEnabled(boolean)
	 */
	public boolean isClearWhitespaceLinesEnabled() {
		return clearWhitespaceLines;
	}


	/**
	 * Returns whether code folding is enabled.  Note that only certain
	 * languages support code folding; those that do not will ignore this
	 * property.
	 *
	 * @return Whether code folding is enabled.
	 * @see #setCodeFoldingEnabled(boolean)
	 */
	public boolean isCodeFoldingEnabled() {
		return foldManager.isCodeFoldingEnabled();
	}


	/**
	 * Returns whether whitespace (spaces and tabs) is visible.
	 *
	 * @return Whether whitespace is visible.
	 * @see #setWhitespaceVisible(boolean)
	 * @see #getEOLMarkersVisible()
	 */
	public boolean isWhitespaceVisible() {
		return whitespaceVisible;
	}


	/**
	 * Returns the token at the specified position in the model.
	 *
	 * @param offs The position in the model.
	 * @return The token, or <code>null</code> if no token is at that
	 *         position.
	 * @see #viewToToken(Point)
	 */
	private Token modelToToken(int offs) {
		if (offs>=0) {
			try {
				int line = getLineOfOffset(offs);
				Token t = getTokenListForLine(line);
				while (t!=null && t.isPaintable()) {
					if (t.containsPosition(offs)) {
						return t;
					}
					t = t.getNextToken();
				}
			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}
		return null;
	}


	/**
	 * The <code>paintComponent</code> method is overridden so we
	 * apply any necessary rendering hints to the Graphics object.
	 */
	protected void paintComponent(Graphics g) {
		super.paintComponent(getGraphics2D(g));
	}


	private void refreshFontMetrics(Graphics2D g2d) {
		// It is assumed that any rendering hints are already applied to g2d.
		defaultFontMetrics = g2d.getFontMetrics(getFont());
		syntaxScheme.refreshFontMetrics(g2d);
		if (getLineWrap()==false) {
			// HORRIBLE HACK!  The un-wrapped view needs to refresh its cached
			// longest line information.
			SyntaxView sv = (SyntaxView)getUI().getRootView(this).getView(0);
			sv.calculateLongestLine();
		}
	}


	/**
	 * Removes an "active line range" listener from this text area.
	 *
	 * @param l The listener to remove.
	 * @see #removeActiveLineRangeListener(ActiveLineRangeListener)
	 */
	public void removeActiveLineRangeListener(ActiveLineRangeListener l) {
		listenerList.remove(ActiveLineRangeListener.class, l);
	}


	/**
	 * Removes a hyperlink listener from this text area.
	 *
	 * @param l The listener to remove.
	 * @see #addHyperlinkListener(HyperlinkListener)
	 */
	public void removeHyperlinkListener(HyperlinkListener l) {
		listenerList.remove(HyperlinkListener.class, l);
	}


	/**
	 * Overridden so we stop this text area's parsers, if any.
	 */
	public void removeNotify() {
		if (parserManager!=null) {
			parserManager.stopParsing();
		}
		super.removeNotify();
	}


	/**
	 * Removes a parser from this text area.
	 *
	 * @param parser The {@link Parser} to remove.
	 * @return Whether the parser was found and removed.
	 * @see #clearParsers()
	 * @see #addParser(Parser)
	 * @see #getParser(int)
	 */
	public boolean removeParser(Parser parser) {
		boolean removed = false;
		if (parserManager!=null) {
			removed = parserManager.removeParser(parser);
		}
		return removed;
	}


	/**
	 * Sets the colors used for syntax highlighting to their defaults.
	 *
	 * @see #setSyntaxScheme(SyntaxScheme)
	 * @see #getSyntaxScheme()
	 * @see #getDefaultSyntaxScheme()
	 */
	public void restoreDefaultSyntaxScheme() {
		setSyntaxScheme(getDefaultSyntaxScheme());
	}


	/**
	 * Attempts to save all currently-known templates to the current template
	 * directory, as set by <code>setTemplateDirectory</code>.  Templates
	 * will be saved as XML files with names equal to their abbreviations; for
	 * example, a template that expands on the word "forb" will be saved as
	 * <code>forb.xml</code>.
	 *
	 * @return Whether or not the save was successful.  The save will
	 *         be unsuccessful if the template directory does not exist or
	 *         if it has not been set (i.e., you have not yet called
	 *         <code>setTemplateDirectory</code>).
	 * @see #getTemplatesEnabled
	 * @see #setTemplateDirectory
	 * @see #setTemplatesEnabled
	 */
	public static synchronized boolean saveTemplates() {
		if (!getTemplatesEnabled()) {
			return false;
		}
		return getCodeTemplateManager().saveTemplates();
	}


	/**
	 * Sets the "active line range."  Note that this
	 * <code>RSyntaxTextArea</code> itself does nothing with this information,
	 * but if it is contained inside an {@link RTextScrollPane}, the active
	 * line range may be displayed in the icon area of the {@link Gutter}.<p>
	 * 
	 * Note that basic users of <code>RSyntaxTextArea</code> will not call this
	 * method directly; rather, it is usually called by instances of
	 * <code>LanguageSupport</code> in the <code>RSTALangaugeSupport</code>
	 * library.  See <a href="http://fifesoft.com">http://fifesoft.com</a>
	 * for more information about this library.
	 *
	 * @param min The "minimum" line in the active line range, or
	 *        <code>-1</code> if the range is being cleared.
	 * @param max The "maximum" line in the active line range, or
	 *        <code>-1</code> if the range is being cleared.
	 * @see #addActiveLineRangeListener(ActiveLineRangeListener)
	 */
	public void setActiveLineRange(int min, int max) {
		if (min==-1) {
			max = -1; // Force max to be -1 if min is.
		}
		fireActiveLineRangeEvent(min, max);
	}


	/**
	 * Sets whether bracket matching should be animated.  This fires a property
	 * change event of type {@link #ANIMATE_BRACKET_MATCHING_PROPERTY}.
	 *
	 * @param animate Whether to animate bracket matching.
	 * @see #getAnimateBracketMatching()
	 */
	public void setAnimateBracketMatching(boolean animate) {
		if (animate!=animateBracketMatching) {
			animateBracketMatching = animate;
			if (animate && bracketRepaintTimer==null) {
				bracketRepaintTimer = new BracketMatchingTimer();
			}
			firePropertyChange(ANIMATE_BRACKET_MATCHING_PROPERTY,
								!animate, animate);
		}
	}


	/**
	 * Sets whether anti-aliasing is enabled in this editor.  This method
	 * fires a property change event of type {@link #ANTIALIAS_PROPERTY}.
	 *
	 * @param enabled Whether anti-aliasing is enabled.
	 * @see #getAntiAliasingEnabled()
	 */
	public void setAntiAliasingEnabled(boolean enabled) {

		boolean currentlyEnabled = aaHints!=null;

		if (enabled!=currentlyEnabled) {

			if (enabled) {
				aaHints = RSyntaxUtilities.getDesktopAntiAliasHints();
				// If the desktop query method comes up empty, use the standard
				// Java2D greyscale method.  Note this will likely NOT be as
				// nice as what would be used if the getDesktopAntiAliasHints()
				// call worked.
				if (aaHints==null) {
					aaHints = new HashMap();
					aaHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
							RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				}
			}
			else {
				aaHints = null;
			}

			// We must be connected to a screen resource for our graphics
			// to be non-null.
			if (isDisplayable()) {
				refreshFontMetrics(getGraphics2D(getGraphics()));
			}
			firePropertyChange(ANTIALIAS_PROPERTY, !enabled, enabled);
			repaint();

		}

	}


	/**
	 * Sets whether or not auto-indent is enabled.  This fires a property
	 * change event of type {@link #AUTO_INDENT_PROPERTY}.
	 *
	 * @param enabled Whether or not auto-indent is enabled.
	 * @see #isAutoIndentEnabled()
	 */
	public void setAutoIndentEnabled(boolean enabled) {
		if (autoIndentEnabled!=enabled) {
			autoIndentEnabled = enabled;
			firePropertyChange(AUTO_INDENT_PROPERTY, !enabled, enabled);
		}
	}


	/**
	 * Sets whether bracket matching is enabled.  This fires a property change
	 * event of type {@link #BRACKET_MATCHING_PROPERTY}.
	 *
	 * @param enabled Whether or not bracket matching should be enabled.
	 * @see #isBracketMatchingEnabled()
	 */
	public void setBracketMatchingEnabled(boolean enabled) {
		if (enabled!=bracketMatchingEnabled) {
			bracketMatchingEnabled = enabled;
			repaint();
			firePropertyChange(BRACKET_MATCHING_PROPERTY, !enabled, enabled);
		}
	}


	/**
	 * Sets whether or not lines containing nothing but whitespace are made
	 * into blank lines when Enter is pressed in them.  This method fires
	 * a property change event of type {@link #CLEAR_WHITESPACE_LINES_PROPERTY}.
	 *
	 * @param enabled Whether or not whitespace-only lines are cleared when
	 *        the user presses Enter on them.
	 * @see #isClearWhitespaceLinesEnabled()
	 */
	public void setClearWhitespaceLinesEnabled(boolean enabled) {
		if (enabled!=clearWhitespaceLines) {
			clearWhitespaceLines = enabled;
			firePropertyChange(CLEAR_WHITESPACE_LINES_PROPERTY,
							!enabled, enabled);
		}
	}


	/**
	 * Toggles whether curly braces should be automatically closed when a
	 * newline is entered after an opening curly brace.  Note that this
	 * property is only honored for languages that use curly braces to denote
	 * code blocks.<p>
	 *
	 * This method fires a property change event of type
	 * {@link #CLOSE_CURLY_BRACES_PROPERTY}.
	 *
	 * @param close Whether curly braces should be automatically closed.
	 * @see #getCloseCurlyBraces()
	 */
	public void setCloseCurlyBraces(boolean close) {
		if (close!=closeCurlyBraces) {
			closeCurlyBraces = close;
			firePropertyChange(CLOSE_CURLY_BRACES_PROPERTY, !close, close);
		}
	}


	/**
	 * Sets whether closing markup tags should be automatically completed
	 * when "<code>&lt;/</code>" is typed.  Note that this property is only
	 * honored for markup languages, such as HTML, XML and PHP.<p>
	 *
	 * This method fires a property change event of type
	 * {@link #CLOSE_MARKUP_TAGS_PROPERTY}.
	 *
	 * @param close Whether closing markup tags should be automatically
	 *        completed.
	 * @see #getCloseMarkupTags()
	 */
	public void setCloseMarkupTags(boolean close) {
		if (close!=closeMarkupTags) {
			closeMarkupTags = close;
			firePropertyChange(CLOSE_MARKUP_TAGS_PROPERTY, !close, close);
		}
	}


	/**
	 * Sets whether code folding is enabled.  Note that only certain
	 * languages will support code folding out of the box.  Those languages
	 * which do not support folding will ignore this property.<p>
	 * This method fires a property change event of type
	 * {@link #CODE_FOLDING_PROPERTY}.
	 *
	 * @param enabled Whether code folding should be enabled.
	 * @see #isCodeFoldingEnabled()
	 */
	public void setCodeFoldingEnabled(boolean enabled) {
		if (enabled!=foldManager.isCodeFoldingEnabled()) {
			foldManager.setCodeFoldingEnabled(enabled);
			firePropertyChange(CODE_FOLDING_PROPERTY, !enabled, enabled);
		}
	}


	/**
	 * Sets the document used by this text area.  This is overridden so that
	 * only instances of {@link RSyntaxDocument} are accepted; for all
	 * others, an exception will be thrown.
	 *
	 * @param document The new document for this text area.
	 * @throws IllegalArgumentException If the document is not an
	 *         <code>RSyntaxDocument</code>.
	 */
	public void setDocument(Document document) {
		if (!(document instanceof RSyntaxDocument))
			throw new IllegalArgumentException("Documents for " +
					"RSyntaxTextArea must be instances of " +
					"RSyntaxDocument!");
		super.setDocument(document);
	}


	/**
	 * Sets whether EOL markers are visible at the end of each line.  This
	 * method fires a property change of type {@link #EOL_VISIBLE_PROPERTY}.
	 *
	 * @param visible Whether EOL markers are visible.
	 * @see #getEOLMarkersVisible()
	 * @see #setWhitespaceVisible(boolean)
	 */
	public void setEOLMarkersVisible(boolean visible) {
		if (visible!=eolMarkersVisible) {
			eolMarkersVisible = visible;
			repaint();
			firePropertyChange(EOL_VISIBLE_PROPERTY, !visible, visible);
		}
	}


	/**
	 * Sets the font used by this text area.  Note that if some token styles
	 * are using a different font, they will not be changed by calling this
	 * method.  To set different fonts on individual token types, use the
	 * text area's <code>SyntaxScheme</code>.
	 *
	 * @param font The font.
	 * @see SyntaxScheme#getStyle(int)
	 */
	public void setFont(Font font) {

		Font old = super.getFont();
		super.setFont(font); // Do this first.

		// Usually programmers keep a single font for all token types, but
		// may use bold or italic for styling some.
		SyntaxScheme scheme = getSyntaxScheme();
		if (scheme!=null && old!=null) {
			scheme.changeBaseFont(old, font);
			calculateLineHeight();
		}

		// We must be connected to a screen resource for our
		// graphics to be non-null.
		if (isDisplayable()) {
			refreshFontMetrics(getGraphics2D(getGraphics()));
			// Updates the margin line.
			updateMarginLineX();
			// Force the current line highlight to be repainted, even
			// though the caret's location hasn't changed.
			forceCurrentLineHighlightRepaint();
			// Get line number border in text area to repaint again
			// since line heights have updated.
			firePropertyChange("font", old, font);
			// So parent JScrollPane will have its scrollbars updated.
			revalidate();
		}

	}


	/**
	 * Sets whether fractional font metrics are enabled.  This method fires
	 * a property change event of type {@link #FRACTIONAL_FONTMETRICS_PROPERTY}.
	 *
	 * @param enabled Whether fractional font metrics are enabled.
	 * @see #getFractionalFontMetricsEnabled()
	 */
	public void setFractionalFontMetricsEnabled(boolean enabled) {
		if (fractionalFontMetricsEnabled!=enabled) {
			fractionalFontMetricsEnabled = enabled;
			// We must be connected to a screen resource for our graphics to be
			// non-null.
			if (isDisplayable()) {
				refreshFontMetrics(getGraphics2D(getGraphics()));
			}
			firePropertyChange(FRACTIONAL_FONTMETRICS_PROPERTY,
											!enabled, enabled);
		}
	}


	/**
	 * Sets the highlighter used by this text area.
	 *
	 * @param h The highlighter.
	 * @throws IllegalArgumentException If <code>h</code> is not an instance
	 *         of {@link RSyntaxTextAreaHighlighter}.
	 */
	public void setHighlighter(Highlighter h) {
		if (!(h instanceof RSyntaxTextAreaHighlighter)) {
			throw new IllegalArgumentException("RSyntaxTextArea requires " +
				"an RSyntaxTextAreaHighlighter for its Highlighter");
		}
		super.setHighlighter(h);
	}


	/**
	 * Sets whether "secondary" languages should have their backgrounds
	 * colored differently to visually differentiate them.  This feature
	 * imposes a fair performance penalty.  This method fires a property change
	 * event of type {@link #HIGHLIGHT_SECONDARY_LANGUAGES_PROPERTY}.
	 *
	 * @return Whether secondary languages have their backgrounds colored
	 *         differently.
	 * @see #getHighlightSecondaryLanguages()
	 * @see #setSecondaryLanguageBackground(int, Color)
	 * @see #getSecondaryLanguageCount()
	 */
	public void setHighlightSecondaryLanguages(boolean highlight) {
		if (this.highlightSecondaryLanguages!=highlight) {
			highlightSecondaryLanguages = highlight;
			repaint();
			firePropertyChange(HIGHLIGHT_SECONDARY_LANGUAGES_PROPERTY,
					!highlight, highlight);
		}
	}


	/**
	 * Sets the color to use when painting hyperlinks.
	 *
	 * @param fg The color to use when painting hyperlinks.
	 * @throws NullPointerException If <code>fg</code> is <code>null</code>.
	 * @see #getHyperlinkForeground()
	 * @see #setHyperlinksEnabled(boolean)
	 */
	public void setHyperlinkForeground(Color fg) {
		if (fg==null) {
			throw new NullPointerException("fg cannot be null");
		}
		hyperlinkFG = fg;
	}


	/**
	 * Sets whether hyperlinks are enabled for this text area.  This method
	 * fires a property change event of type
	 * {@link #HYPERLINKS_ENABLED_PROPERTY}.
	 *
	 * @param enabled Whether hyperlinks are enabled.
	 * @see #getHyperlinksEnabled()
	 */
	public void setHyperlinksEnabled(boolean enabled) {
		if (this.hyperlinksEnabled!=enabled) {
			this.hyperlinksEnabled = enabled;
			repaint();
			firePropertyChange(HYPERLINKS_ENABLED_PROPERTY, !enabled, enabled);
		}
	}


	/**
	 * Sets the mask for the key used to toggle whether we are scanning for
	 * hyperlinks with mouse hovering.
	 *
	 * @param mask The mask to use.  This should be a value such as
	 *        {@link InputEvent#CTRL_DOWN_MASK} or
	 *        {@link InputEvent#META_DOWN_MASK}.
	 *        For invalid values, behavior is undefined.
	 * @see InputEvent
	 */
	public void setLinkScanningMask(int mask) {
		if (mask==InputEvent.CTRL_DOWN_MASK ||
				mask==InputEvent.META_DOWN_MASK ||
				mask==InputEvent.ALT_DOWN_MASK ||
				mask==InputEvent.SHIFT_DOWN_MASK) {
			linkScanningMask = mask;
		}
	}


	/**
	 * Toggles whether "mark occurrences" is enabled.  This method fires a
	 * property change event of type {@link #MARK_OCCURRENCES_PROPERTY}.
	 *
	 * @param markOccurrences Whether "Mark Occurrences" should be enabled.
	 * @see #getMarkOccurrences()
	 * @see #setMarkOccurrencesColor(Color)
	 */
	public void setMarkOccurrences(boolean markOccurrences) {
		if (markOccurrences) {
			if (markOccurrencesSupport==null) {
				markOccurrencesSupport = new MarkOccurrencesSupport();
				markOccurrencesSupport.install(this);
				firePropertyChange(MARK_OCCURRENCES_PROPERTY, false, true);
			}
		}
		else {
			if (markOccurrencesSupport!=null) {
				markOccurrencesSupport.uninstall();
				markOccurrencesSupport = null;
				firePropertyChange(MARK_OCCURRENCES_PROPERTY, true, false);
			}
		}
	}


	/**
	 * Sets the "mark occurrences" color.
	 *
	 * @param color The new color.  This cannot be <code>null</code>.
	 * @see #getMarkOccurrencesColor()
	 * @see #setMarkOccurrences(boolean)
	 */
	public void setMarkOccurrencesColor(Color color) {
		markOccurrencesColor = color;
		if (markOccurrencesSupport!=null) {
			markOccurrencesSupport.setColor(color);
		}
	}


	/**
	 * Sets the color used as the background for a matched bracket.
	 *
	 * @param color The color to use.  If this is <code>null</code>, then no
	 *        special background is painted behind a matched bracket.
	 * @see #getMatchedBracketBGColor
	 * @see #setMatchedBracketBorderColor
	 * @see #setPaintMarkOccurrencesBorder(boolean)
	 */
	public void setMatchedBracketBGColor(Color color) {
		matchedBracketBGColor = color;
		if (match!=null) {
			repaint();
		}
	}


	/**
	 * Sets the color used as the border for a matched bracket.
	 *
	 * @param color The color to use.
	 * @see #getMatchedBracketBorderColor
	 * @see #setMatchedBracketBGColor
	 */
	public void setMatchedBracketBorderColor(Color color) {
		matchedBracketBorderColor = color;
		if (match!=null) {
			repaint();
		}
	}


	/**
	 * Toggles whether a border should be painted around marked occurrences.
	 *
	 * @param paintBorder Whether to paint a border.
	 * @see #getPaintMarkOccurrencesBorder()
	 * @see #setMarkOccurrencesColor(Color)
	 * @see #setMarkOccurrences(boolean)
	 */
	public void setPaintMarkOccurrencesBorder(boolean paintBorder) {
		paintMarkOccurrencesBorder = paintBorder;
		if (markOccurrencesSupport!=null) {
			markOccurrencesSupport.setPaintBorder(paintBorder);
		}
	}


	/**
	 * Sets whether the bracket at the caret position is painted as a "match"
	 * when a matched bracket is found.  Note that this property does nothing
	 * if {@link #isBracketMatchingEnabled()} returns <code>false</code>.<p>
	 *
	 * This method fires a property change event of type
	 * {@link #PAINT_MATCHED_BRACKET_PAIR_PROPERTY}.
	 * 
	 * @param paintPair Whether both brackets in a bracket pair should be
	 *        highlighted when bracket matching is enabled.
	 * @see #getPaintMatchedBracketPair()
	 * @see #isBracketMatchingEnabled()
	 * @see #setBracketMatchingEnabled(boolean)
	 */
	public void setPaintMatchedBracketPair(boolean paintPair) {
		if (paintPair!=paintMatchedBracketPair) {
			paintMatchedBracketPair = paintPair;
			doBracketMatching();
			repaint();
			firePropertyChange(PAINT_MATCHED_BRACKET_PAIR_PROPERTY,
					!paintMatchedBracketPair, paintMatchedBracketPair);
		}
	}


	/**
	 * Toggles whether tab lines are painted.  This method fires a property
	 * change event of type {@link #TAB_LINES_PROPERTY}.
	 *
	 * @param paint Whether tab lines are painted.
	 * @see #getPaintTabLines()
	 * @see #setTabLineColor(Color)
	 */
	public void setPaintTabLines(boolean paint) {
		if (paint!=paintTabLines) {
			paintTabLines = paint;
			repaint();
			firePropertyChange(TAB_LINES_PROPERTY, !paint, paint);
		}
	}


	/**
	 * Sets the background color to use for a secondary language.
	 *
	 * @param index The language index.  Note that these are 1-based, not
	 *        0-based, and should be in the range
	 *        <code>1-getSecondaryLanguageCount()</code>, inclusive.
	 * @param color The new color, or <code>null</code> for none.
	 * @see #getSecondaryLanguageBackground(int)
	 * @see #getSecondaryLanguageCount()
	 */
	public void setSecondaryLanguageBackground(int index, Color color) {
		index--;
		Color old = secondaryLanguageBackgrounds[index];
		if ((color==null && old!=null) || (color!=null && !color.equals(old))) {
			secondaryLanguageBackgrounds[index] = color;
			if (getHighlightSecondaryLanguages()) {
				repaint();
			}
		}
	}


	/**
	 * Sets what type of syntax highlighting this editor is doing.  This method
	 * fires a property change of type {@link #SYNTAX_STYLE_PROPERTY}.
	 *
	 * @param styleKey The syntax editing style to use, for example,
	 *        {@link SyntaxConstants#SYNTAX_STYLE_NONE} or
	 *        {@link SyntaxConstants#SYNTAX_STYLE_JAVA}.
	 * @see #getSyntaxEditingStyle()
	 * @see SyntaxConstants
	 */
	public void setSyntaxEditingStyle(String styleKey) {
		if (styleKey==null) {
			styleKey = SYNTAX_STYLE_NONE;
		}
		if (!styleKey.equals(syntaxStyleKey)) {
			String oldStyle = syntaxStyleKey;
			syntaxStyleKey = styleKey;
			((RSyntaxDocument)getDocument()).setSyntaxStyle(styleKey);
			firePropertyChange(SYNTAX_STYLE_PROPERTY, oldStyle, styleKey);
			setActiveLineRange(-1, -1);
		}

	}


	/**
	 * Sets all of the colors used in syntax highlighting to the colors
	 * specified.  This uses a shallow copy of the color scheme so that
	 * multiple text areas can share the same color scheme and have their
	 * properties changed simultaneously.<p>
	 *
	 * This method fires a property change event of type
	 * {@link #SYNTAX_SCHEME_PROPERTY}.
	 *
	 * @param scheme The instance of <code>SyntaxScheme</code> to use.
	 * @see #getSyntaxScheme()
	 */
	public void setSyntaxScheme(SyntaxScheme scheme) {

		// NOTE:  We don't check whether colorScheme is the same as the
		// current scheme because DecreaseFontSizeAction and
		// IncreaseFontSizeAction need it this way.
		// FIXME:  Find a way around this.

		SyntaxScheme old = this.syntaxScheme;
		this.syntaxScheme = scheme;

		// Recalculate the line height.  We do this here instead of in
		// refreshFontMetrics() as this method is called less often and we
		// don't need the rendering hints to get the font's height.
		calculateLineHeight();

		if (isDisplayable()) {
			refreshFontMetrics(getGraphics2D(getGraphics()));
		}

		// Updates the margin line.
		updateMarginLineX();

		// Force the current line highlight to be repainted, even though
		// the caret's location hasn't changed.
		forceCurrentLineHighlightRepaint();

		// So encompassing JScrollPane will have its scrollbars updated.
		revalidate();

		firePropertyChange(SYNTAX_SCHEME_PROPERTY, old, this.syntaxScheme);

	}


	/**
	 * If templates are enabled, all currently-known templates are forgotten
	 * and all templates are loaded from all files in the specified directory
	 * ending in "*.xml".  If templates aren't enabled, nothing happens.
	 *
	 * @param dir The directory containing files ending in extension
	 *        <code>.xml</code> that contain templates to load.
	 * @return <code>true</code> if the load was successful;
	 *         <code>false</code> if either templates aren't currently
	 *         enabled or the load failed somehow (most likely, the
	 *         directory doesn't exist).	 
	 * @see #getTemplatesEnabled
	 * @see #setTemplatesEnabled
	 * @see #saveTemplates
	 */
	public static synchronized boolean setTemplateDirectory(String dir) {
		if (getTemplatesEnabled() && dir!=null) {
			File directory = new File(dir);
			if (directory.isDirectory()) {
				return getCodeTemplateManager().
						setTemplateDirectory(directory)>-1;
			}
			boolean created = directory.mkdir();
			if (created) {
				return getCodeTemplateManager().
					setTemplateDirectory(directory)>-1;
			}
		}
		return false;
	}


	/**
	 * Enables or disables templates.<p>
	 *
	 * Templates are a set of "shorthand identifiers" that you can configure
	 * so that you only have to type a short identifier (such as "forb") to
	 * insert a larger amount of code into the document (such as:<p>
	 *
	 * <pre>
	 *   for (&lt;caret&gt;) {
	 *
	 *   }
	 * </pre>
	 *
	 * Templates are a shared resource among all instances of
	 * <code>RSyntaxTextArea</code>; that is, templates can only be
	 * enabled/disabled for all text areas globally, not individually, and
	 * all text areas have access of the same templates.  This should not
	 * be an issue; rather, it should be beneficial as it promotes
	 * uniformity among all text areas in an application.
	 *
	 * @param enabled Whether or not templates should be enabled.
	 * @see #getTemplatesEnabled()
	 */
	public static synchronized void setTemplatesEnabled(boolean enabled) {
		templatesEnabled = enabled;
	}


	/**
	 * Sets the color use to paint tab lines.  This method fires a property
	 * change event of type {@link #TAB_LINE_COLOR_PROPERTY}.
	 *
	 * @param c The color.  If this value is <code>null</code>, the default
	 *        (gray) is used.
	 * @see #getTabLineColor()
	 * @see #setPaintTabLines(boolean)
	 * @see #getPaintTabLines()
	 */
	public void setTabLineColor(Color c) {

		if (c==null) {
			c = Color.gray;
		}

		if (!c.equals(tabLineColor)) {
			Color old = tabLineColor;
			tabLineColor = c;
			if (getPaintTabLines()) {
				repaint();
			}
			firePropertyChange(TAB_LINE_COLOR_PROPERTY, old, tabLineColor);
		}

	}


	/**
	 * Sets whether "focusable" tool tips are used instead of standard ones.
	 * Focusable tool tips are tool tips that the user can click on,
	 * resize, copy from, and clink links in.  This method fires a property
	 * change event of type {@link #FOCUSABLE_TIPS_PROPERTY}.
	 *
	 * @param use Whether to use focusable tool tips.
	 * @see #getUseFocusableTips()
	 * @see FocusableTip
	 */
	public void setUseFocusableTips(boolean use) {
		if (use!=useFocusableTips) {
			useFocusableTips = use;
			firePropertyChange(FOCUSABLE_TIPS_PROPERTY, !use, use);
		}
	}


	/**
	 * Sets whether whitespace is visible.  This method fires a property change
	 * of type {@link #VISIBLE_WHITESPACE_PROPERTY}.
	 *
	 * @param visible Whether whitespace should be visible.
	 * @see #isWhitespaceVisible
	 */
	public void setWhitespaceVisible(boolean visible) {
		if (whitespaceVisible!=visible) {
			whitespaceVisible = visible;
			((RSyntaxDocument)getDocument()).setWhitespaceVisible(visible);
			repaint();
			firePropertyChange(VISIBLE_WHITESPACE_PROPERTY, !visible, visible);
		}
	}


	/**
	 * Resets the editor state after the user clicks on a hyperlink or releases
	 * the hyperlink modifier.
	 */
	private void stopScanningForLinks() {
		if (isScanningForLinks) {
			Cursor c = getCursor();
			isScanningForLinks = false;
			hoveredOverLinkOffset = -1;
			if (c!=null && c.getType()==Cursor.HAND_CURSOR) {
				setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
				repaint(); // TODO: Repaint just the affected line.
			}
		}
	}


	/**
	 * Returns the token at the specified position in the view.
	 *
	 * @param p The position in the view.
	 * @return The token, or <code>null</code> if no token is at that
	 *         position.
	 * @see #modelToToken(int)
	 */
	/*
	 * TODO: This is a little inefficient.  This should convert view
	 * coordinates to the underlying token (if any).  The way things currently
	 * are, we're calling getTokenListForLine() twice (once in viewToModel()
	 * and once here).
	 */
	private Token viewToToken(Point p) {
		return modelToToken(viewToModel(p));
	}


	/**
	 * A timer that animates the "bracket matching" animation.
	 */
	private class BracketMatchingTimer extends Timer implements ActionListener {

		private int pulseCount;

		public BracketMatchingTimer() {
			super(20, null);
			addActionListener(this);
			setCoalesce(false);
		}

		public void actionPerformed(ActionEvent e) {
			if (isBracketMatchingEnabled()) {
				if (match!=null) {
					updateAndInvalidate(match);
				}
				if (dotRect!=null && getPaintMatchedBracketPair()) {
					updateAndInvalidate(dotRect);
				}
				if (++pulseCount==8) {
					pulseCount = 0;
					stop();
				}
			}
		}

		private void init(Rectangle r) {
			r.x += 3;
			r.y += 3;
			r.width -= 6;
			r.height -= 6; // So animation can "grow" match
		}

		public void start() {
			init(match);
			if (dotRect!=null && getPaintMatchedBracketPair()) {
				init(dotRect);
			}
			pulseCount = 0;
			super.start();
		}

		private void updateAndInvalidate(Rectangle r) {
			if (pulseCount<5) {
				r.x--;
				r.y--;
				r.width += 2;
				r.height += 2;
				repaint(r.x,r.y, r.width,r.height);
			}
			else if (pulseCount<7) {
				r.x++;
				r.y++;
				r.width -= 2;
				r.height -= 2;
				repaint(r.x-2,r.y-2, r.width+5,r.height+5);
			}
		}

	}


	/**
	 * Handles hyperlinks.
	 */
	private class RSyntaxTextAreaMutableCaretEvent
					extends RTextAreaMutableCaretEvent {

		protected RSyntaxTextAreaMutableCaretEvent(RTextArea textArea) {
			super(textArea);
		}

		public void mouseClicked(MouseEvent e) {
			if (getHyperlinksEnabled() && isScanningForLinks &&
					hoveredOverLinkOffset>-1) {
				Token t = modelToToken(hoveredOverLinkOffset);
				URL url = null;
				String desc = null;
				try {
					String temp = t.getLexeme();
					// URI's need "http://" prefix for web URL's to work.
					if (temp.startsWith("www.")) {
						temp = "http://" + temp;
					}
					url = new URL(temp);
				} catch (MalformedURLException mue) {
					desc = mue.getMessage();
				}
				HyperlinkEvent he = new HyperlinkEvent(this,
						HyperlinkEvent.EventType.ACTIVATED,
						url, desc);
				fireHyperlinkUpdate(he);
				stopScanningForLinks();
			}
		}

		public void mouseMoved(MouseEvent e) {
			super.mouseMoved(e);
			if (getHyperlinksEnabled()) {
				if ((e.getModifiersEx()&linkScanningMask)!=0) {
					isScanningForLinks = true;
					Token t = viewToToken(e.getPoint());
					Cursor c2 = null;
					if (t!=null && t.isHyperlink()) {
						hoveredOverLinkOffset = t.offset;
						c2 = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
					}
					else {
						c2 = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
						hoveredOverLinkOffset = -1;
					}
					if (getCursor()!=c2) {
						setCursor(c2);
						// TODO: Repaint just the affected line(s).
						repaint(); // Link either left or went into.
					}
				}
				else {
					if (isScanningForLinks) {
						stopScanningForLinks();
					}
				}
			}
		}

	}


}