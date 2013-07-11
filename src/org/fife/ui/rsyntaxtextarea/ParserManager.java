/*
 * 09/26/2005
 *
 * ParserManager.java - Manages the parsing of an RSyntaxTextArea's document,
 * if necessary.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;

import org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ToolTipInfo;



/**
 * Manages running a parser object for an <code>RSyntaxTextArea</code>.
 *
 * @author Robert Futrell
 * @version 0.9
 */
class ParserManager implements DocumentListener, ActionListener,
								HyperlinkListener {

	private RSyntaxTextArea textArea;
	private List parsers;
	private Timer timer;
	private boolean running;
	private Parser parserForTip;
	private Position firstOffsetModded;
	private Position lastOffsetModded;

	/**
	 * Mapping of notices to their highlights in the editor.  Can't use a Map
	 * since parsers could return two <code>ParserNotice</code>s that compare
	 * equally via <code>equals()</code>.  Real-world example:  The Perl
	 * compiler will return 2+ identical error messages if the same error is
	 * committed in a single line more than once.
	 */
	private List noticeHighlightPairs;

	/**
	 * Painter used to underline errors.
	 */
	private SquiggleUnderlineHighlightPainter parserErrorHighlightPainter =
						new SquiggleUnderlineHighlightPainter(Color.RED);

	/**
	 * If this system property is set to <code>true</code>, debug messages
	 * will be printed to stdout to help diagnose parsing issues.
	 */
	private static final String PROPERTY_DEBUG_PARSING = "rsta.debugParsing";

	/**
	 * Whether to print debug messages while running parsers.
	 */
	private static final boolean DEBUG_PARSING;

	/**
	 * The default delay between the last key press and when the document
	 * is parsed, in milliseconds.
	 */
	private static final int DEFAULT_DELAY_MS		= 1250;


	/**
	 * Constructor.
	 *
	 * @param textArea The text area whose document the parser will be
	 *        parsing.
	 */
	public ParserManager(RSyntaxTextArea textArea) {
		this(DEFAULT_DELAY_MS, textArea);
	}


	/**
	 * Constructor.
	 *
	 * @param delay The delay between the last key press and when the document
	 *        is parsed.
	 * @param textArea The text area whose document the parser will be
	 *        parsing.
	 */
	public ParserManager(int delay, RSyntaxTextArea textArea) {
		this.textArea = textArea;
		textArea.getDocument().addDocumentListener(this);
		parsers = new ArrayList(1); // Usually small
		timer = new Timer(delay, this);
		timer.setRepeats(false);
		running = true;
	}


	/**
	 * Called when the timer fires (e.g. it's time to parse the document).
	 * 
	 * @param e The event.
	 */
	public void actionPerformed(ActionEvent e) {

		// Sanity check - should have >1 parser if event is fired.
		int parserCount = getParserCount();
		if (parserCount==0) {
			return;
		}

		long begin = 0;
		if (DEBUG_PARSING) {
			begin = System.currentTimeMillis();
		}

		RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();

		Element root = doc.getDefaultRootElement();
		int firstLine = firstOffsetModded==null ? 0 : root.getElementIndex(firstOffsetModded.getOffset());
		int lastLine = lastOffsetModded==null ? root.getElementCount()-1 : root.getElementIndex(lastOffsetModded.getOffset());
		firstOffsetModded = lastOffsetModded = null;
		if (DEBUG_PARSING) {
			System.out.println("[DEBUG]: Minimum lines to parse: " + firstLine + "-" + lastLine);
		}

		String style = textArea.getSyntaxEditingStyle();
		doc.readLock();
		try {
			for (int i=0; i<parserCount; i++) {
				Parser parser = getParser(i);
				if (parser.isEnabled()) {
					ParseResult res = parser.parse(doc, style);
					addParserNoticeHighlights(res);
				}
				else {
					clearParserNoticeHighlights(parser);
				}
			}
			textArea.fireParserNoticesChange();
		} finally {
			doc.readUnlock();
		}

		if (DEBUG_PARSING) {
			float time = (System.currentTimeMillis()-begin)/1000f;
			System.out.println("Total parsing time: " + time + " seconds");
		}

	}


	/**
	 * Adds a parser for the text area.
	 *
	 * @param parser The new parser.  If this is <code>null</code>, nothing
	 *        happens.
	 * @see #getParser(int)
	 * @see #removeParser(Parser)
	 */
	public void addParser(Parser parser) {
		if (parser!=null && !parsers.contains(parser)) {
			if (running) {
				timer.stop();
			}
			parsers.add(parser);
			if (parsers.size()==1) {
				// Okay to call more than once.
				ToolTipManager.sharedInstance().registerComponent(textArea);
			}
			if (running) {
				timer.restart();
			}
		}
	}


	/**
	 * Adds highlights for a list of parser notices.  Any current notices
	 * from the same Parser, in the same parsed range, are removed.
	 *
	 * @param res The result of a parsing.
	 * @see #clearParserNoticeHighlights()
	 */
	private void addParserNoticeHighlights(ParseResult res) {

		// Parsers are supposed to return at least empty ParseResults, but
		// we'll be defensive here.
		if (res==null) {
			return;
		}

		if (DEBUG_PARSING) {
			System.out.println("[DEBUG]: Adding parser notices from " +
								res.getParser());
		}

		if (noticeHighlightPairs==null) {
			noticeHighlightPairs = new ArrayList();
		}

		removeParserNotices(res);

		List notices = res.getNotices();
		if (notices.size()>0) { // Guaranteed non-null

			RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter)
													textArea.getHighlighter();

			for (Iterator i=notices.iterator(); i.hasNext(); ) {
				ParserNotice notice = (ParserNotice)i.next();
				if (DEBUG_PARSING) {
					System.out.println("[DEBUG]: ... adding: " + notice);
				}
				try {
					Object highlight = null;
					if (notice.getShowInEditor()) {
						highlight = h.addParserHighlight(notice,
											parserErrorHighlightPainter);
					}
					noticeHighlightPairs.add(new NoticeHighlightPair(notice, highlight));
				} catch (BadLocationException ble) { // Never happens
					ble.printStackTrace();
				}
			}

		}

		if (DEBUG_PARSING) {
			System.out.println("[DEBUG]: Done adding parser notices from " +
								res.getParser());
		}

	}


	/**
	 * Called when the document is modified.
	 *
	 * @param e The document event.
	 */
	public void changedUpdate(DocumentEvent e) {
	}


	private void clearParserNoticeHighlights() {
		RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter)
											textArea.getHighlighter();
		if (h!=null) {
			h.clearParserHighlights();
		}
		if (noticeHighlightPairs!=null) {
			noticeHighlightPairs.clear();
		}
	}


	/**
	 * Removes all parser notice highlights for a specific parser.
	 *
	 * @param parser The parser whose highlights to remove.
	 */
	private void clearParserNoticeHighlights(Parser parser) {
		RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter)
											textArea.getHighlighter();
		if (h!=null) {
			h.clearParserHighlights(parser);
		}
		if (noticeHighlightPairs!=null) {
			for (Iterator i=noticeHighlightPairs.iterator(); i.hasNext(); ) {
				NoticeHighlightPair pair = (NoticeHighlightPair)i.next();
				if (pair.notice.getParser()==parser) {
					i.remove();
				}
			}
		}
	}


	/**
	 * Removes all parsers and any highlights they have created.
	 *
	 * @see #addParser(Parser)
	 */
	public void clearParsers() {
		timer.stop();
		clearParserNoticeHighlights();
		parsers.clear();
		textArea.fireParserNoticesChange();
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
		Parser p = getParser(parser);
		RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
		String style = textArea.getSyntaxEditingStyle();
		doc.readLock();
		try {
			if (p.isEnabled()) {
				ParseResult res = p.parse(doc, style);
				addParserNoticeHighlights(res);
			}
			else {
				clearParserNoticeHighlights(p);
			}
			textArea.fireParserNoticesChange();
		} finally {
			doc.readUnlock();
		}
	}


	/**
	 * Returns the delay between the last "concurrent" edit and when the
	 * document is re-parsed.
	 *
	 * @return The delay, in milliseconds.
	 * @see #setDelay(int)
	 */
	public int getDelay() {
		return timer.getDelay();
	}


	/**
	 * Returns the specified parser.
	 *
	 * @param index The index of the parser.
	 * @return The parser.
	 * @see #getParserCount()
	 * @see #addParser(Parser)
	 * @see #removeParser(Parser)
	 */
	public Parser getParser(int index) {
		return (Parser)parsers.get(index);
	}


	/**
	 * Returns the number of registered parsers.
	 *
	 * @return The number of registered parsers.
	 */
	public int getParserCount() {
		return parsers.size();
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
		List notices = new ArrayList();
		if (noticeHighlightPairs!=null) {
			for (Iterator i=noticeHighlightPairs.iterator(); i.hasNext(); ) {
				NoticeHighlightPair pair = (NoticeHighlightPair)i.next();
				notices.add(pair.notice);
			}
		}
		return notices;
	}


	/**
	 * Returns the tool tip to display for a mouse event at the given
	 * location.  This method is overridden to give a registered parser a
	 * chance to display a tool tip (such as an error description when the
	 * mouse is over an error highlight).
	 *
	 * @param e The mouse event.
	 * @return The tool tip to display, and possibly a hyperlink event handler. 
	 */
	public ToolTipInfo getToolTipText(MouseEvent e) {

		String tip = null;
		HyperlinkListener listener = null;
		parserForTip = null;

//		try {
			int pos = textArea.viewToModel(e.getPoint());
			/*
			Highlighter.Highlight[] highlights = textArea.getHighlighter().
												getHighlights();
			for (int i=0; i<highlights.length; i++) {
				Highlighter.Highlight h = highlights[i];
				//if (h instanceof ParserNoticeHighlight) {
				//	ParserNoticeHighlight pnh = (ParserNoticeHighlight)h;
					int start = h.getStartOffset();
					int end = h.getEndOffset();
					if (start<=pos && end>=pos) {
						//return pnh.getMessage();
						return textArea.getText(start, end-start);
					}
				//}
			}
			*/
			if (noticeHighlightPairs!=null) {
				for (int j=0; j<noticeHighlightPairs.size(); j++) {
					NoticeHighlightPair pair =
						(NoticeHighlightPair)noticeHighlightPairs.get(j);
					ParserNotice notice = pair.notice;
					if (notice.containsPosition(pos)) {
						tip = notice.getToolTipText();
						parserForTip = notice.getParser();
						if (parserForTip instanceof HyperlinkListener) {
							listener = (HyperlinkListener)parserForTip;
						}
						break;
					}
				}
			}
//		} catch (BadLocationException ble) {
//			ble.printStackTrace();	// Should never happen.
//		}

		URL imageBase = parserForTip==null ? null : parserForTip.getImageBase();
		return new ToolTipInfo(tip, listener, imageBase);

	}


	/**
	 * Called when the document is modified.
	 *
	 * @param e The document event.
	 */
	public void handleDocumentEvent(DocumentEvent e) {
		if (running && parsers.size()>0) {
			timer.restart();
		}
	}


	/**
	 * Called when the user clicks a hyperlink in a {@link FocusableTip}.
	 *
	 * @param e The event.
	 */
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (parserForTip!=null && parserForTip.getHyperlinkListener()!=null) {
			parserForTip.getHyperlinkListener().linkClicked(textArea, e);
		}
	}


	/**
	 * Called when the document is modified.
	 *
	 * @param e The document event.
	 */
	public void insertUpdate(DocumentEvent e) {

		// Keep track of the first and last offset modified.  Some parsers are
		// smart and will only re-parse this section of the file.
		try {
			int offs = e.getOffset();
			if (firstOffsetModded==null || offs<firstOffsetModded.getOffset()) {
				firstOffsetModded = e.getDocument().createPosition(offs);
			}
			offs = e.getOffset() + e.getLength();
			if (lastOffsetModded==null || offs>lastOffsetModded.getOffset()) {
				lastOffsetModded = e.getDocument().createPosition(offs);
			}
		} catch (BadLocationException ble) {
			ble.printStackTrace(); // Shouldn't happen
		}

		handleDocumentEvent(e);

	}


	/**
	 * Removes a parser.
	 *
	 * @param parser The parser to remove.
	 * @return Whether the parser was found.
	 * @see #addParser(Parser)
	 * @see #getParser(int)
	 */
	public boolean removeParser(Parser parser) {
		removeParserNotices(parser);
		boolean removed = parsers.remove(parser);
		if (removed) {
			textArea.fireParserNoticesChange();
		}
		return removed;
	}


	/**
	 * Removes all parser notices (and clears highlights in the editor) from
	 * a particular parser.
	 *
	 * @param parser The parser.
	 */
	private void removeParserNotices(Parser parser) {
		if (noticeHighlightPairs!=null) {
			RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter)
												textArea.getHighlighter();
			for (Iterator i=noticeHighlightPairs.iterator(); i.hasNext(); ) {
				NoticeHighlightPair pair = (NoticeHighlightPair)i.next();
				if (pair.notice.getParser()==parser && pair.highlight!=null) {
					h.removeParserHighlight(pair.highlight);
					i.remove();
				}
			}
		}
	}


	/**
	 * Removes any currently stored notices (and the corresponding highlights
	 * from the editor) from the same Parser, and in the given line range,
	 * as in the results.
	 *
	 * @param res The results.
	 */
	private void removeParserNotices(ParseResult res) {
		if (noticeHighlightPairs!=null) {
			RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter)
												textArea.getHighlighter();
			for (Iterator i=noticeHighlightPairs.iterator(); i.hasNext(); ) {
				NoticeHighlightPair pair = (NoticeHighlightPair)i.next();
				boolean removed = false;
				if (shouldRemoveNotice(pair.notice, res)) {
					if (pair.highlight!=null) {
						h.removeParserHighlight(pair.highlight);
					}
					i.remove();
					removed = true;
				}
				if (DEBUG_PARSING) {
					String text = removed ? "[DEBUG]: ... notice removed: " :
											"[DEBUG]: ... notice not removed: ";
					System.out.println(text + pair.notice);
				}
			}

		}

	}


	/**
	 * Called when the document is modified.
	 *
	 * @param e The document event.
	 */
	public void removeUpdate(DocumentEvent e) {

		// Keep track of the first and last offset modified.  Some parsers are
		// smart and will only re-parse this section of the file.  Note that
		// for removals, only the line at the removal start needs to be
		// re-parsed.
		try {
			int offs = e.getOffset();
			if (firstOffsetModded==null || offs<firstOffsetModded.getOffset()) {
				firstOffsetModded = e.getDocument().createPosition(offs);
			}
			if (lastOffsetModded==null || offs>lastOffsetModded.getOffset()) {
				lastOffsetModded = e.getDocument().createPosition(offs);
			}
		} catch (BadLocationException ble) { // Never happens
			ble.printStackTrace();
		}

		handleDocumentEvent(e);

	}


	/**
	 * Restarts parsing the document.
	 *
	 * @see #stopParsing()
	 */
	public void restartParsing() {
		timer.restart();
		running = true;
	}


	/**
	 * Sets the delay between the last "concurrent" edit and when the document
	 * is re-parsed.
	 *
	 * @param millis The new delay, in milliseconds.  This must be greater
	 *        than <code>0</code>.
	 * @see #getDelay()
	 */
	public void setDelay(int millis) {
		if (running) {
			timer.stop();
		}
		timer.setDelay(millis);
		if (running) {
			timer.start();
		}
	}


	/**
	 * Returns whether a parser notice should be removed, based on a parse
	 * result.
	 *
	 * @param notice The notice in question.
	 * @param res The result.
	 * @return Whether the notice should be removed.
	 */
	private final boolean shouldRemoveNotice(ParserNotice notice,
											ParseResult res) {

		if (DEBUG_PARSING) {
			System.out.println("[DEBUG]: ... ... shouldRemoveNotice " +
					notice + ": " + (notice.getParser()==res.getParser()));
		}

		// NOTE: We must currently remove all notices for the parser.  Parser
		// implementors are required to parse the entire document each parsing
		// request, as RSTA is not yet sophisticated enough to determine the
		// minimum range of text to parse (and ParserNotices' locations aren't
		// updated when the Document is mutated, which would be a requirement
		// for this as well).
		// return same_parser && (in_reparsed_range || in_deleted_end_of_doc)
		return notice.getParser()==res.getParser();

	}


	/**
	 * Stops parsing the document.
	 *
	 * @see #restartParsing()
	 */
	public void stopParsing() {
		timer.stop();
		running = false;
	}


	/**
	 * Mapping of a parser notice to its highlight in the editor.
	 */
	private static class NoticeHighlightPair {

		public ParserNotice notice;
		public Object highlight;

		public NoticeHighlightPair(ParserNotice notice, Object highlight) {
			this.notice = notice;
			this.highlight = highlight;
		}

	}


	static {
		boolean debugParsing = false;
		try {
			debugParsing = Boolean.getBoolean(PROPERTY_DEBUG_PARSING);
		} catch (AccessControlException ace) {
			// Likely an applet's security manager.
			debugParsing = false; // FindBugs
		}
		DEBUG_PARSING = debugParsing;
	}


}