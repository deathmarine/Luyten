/*
 * 01/06/2009
 *
 * MarkOccurrencesSupport.java - Handles marking all occurrences of the
 * currently selected identifier in a text area.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;



/**
 * Marks all occurrences of the token at the current caret position, if it is
 * an identifier.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class MarkOccurrencesSupport implements CaretListener, ActionListener {

	private RSyntaxTextArea textArea;
	private Timer timer;
	private MarkOccurrencesHighlightPainter p;

	/**
	 * The default color used to mark occurrences.
	 */
	public static final Color DEFAULT_COLOR = new Color(224, 224, 224);

	/**
	 * The default delay.
	 */
	private static final int DEFAULT_DELAY_MS = 1000;


	/**
	 * Constructor.  Creates a listener with a 1 second delay.
	 */
	public MarkOccurrencesSupport() {
		this(DEFAULT_DELAY_MS);
	}


	/**
	 * Constructor.
	 *
	 * @param delay The delay between when the caret last moves and when the
	 *        text should be scanned for matching occurrences.  This should
	 *        be in milliseconds.
	 */
	public MarkOccurrencesSupport(int delay) {
		this(delay, DEFAULT_COLOR);
	}


	/**
	 * Constructor.
	 *
	 * @param delay The delay between when the caret last moves and when the
	 *        text should be scanned for matching occurrences.  This should
	 *        be in milliseconds.
	 * @param color The color to use to mark the occurrences.  This cannot be
	 *        <code>null</code>.
	 */
	public MarkOccurrencesSupport(int delay, Color color) {
		timer = new Timer(delay, this);
		timer.setRepeats(false);
		p = new MarkOccurrencesHighlightPainter();
		setColor(color);
	}


	/**
	 * Called after the caret has been moved and a fixed time delay has
	 * elapsed.  This locates and highlights all occurrences of the identifier
	 * at the caret position, if any.
	 *
	 * @param e The event.
	 */
	public void actionPerformed(ActionEvent e) {

		// Don't do anything if they are selecting text.
		Caret c = textArea.getCaret();
		if (c.getDot()!=c.getMark()) {
			return;
		}

		RSyntaxDocument doc = (RSyntaxDocument)textArea.getDocument();
		//long time = System.currentTimeMillis();
		doc.readLock();
		try {

			// Get the token at the caret position.
			int line = textArea.getCaretLineNumber();
			Token tokenList = textArea.getTokenListForLine(line);
			int dot = c.getDot();
			Token t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
			if (t==null /* EOL */ || !isValidType(t) || isNonWordChar(t)) {
				// Try to the "left" of the caret.
				dot--;
				try {
					if (dot>=textArea.getLineStartOffset(line)) {
						t = RSyntaxUtilities.getTokenAtOffset(tokenList, dot);
					}
				} catch (BadLocationException ble) {
					ble.printStackTrace(); // Never happens
				}
			}

			// Add new highlights if an identifier is selected.
			if (t!=null && isValidType(t) && !isNonWordChar(t)) {
				removeHighlights();
				RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter)
													textArea.getHighlighter();
				char[] lexeme = t.getLexeme().toCharArray();
				int type = t.type;
				for (int i=0; i<textArea.getLineCount(); i++) {
					Token temp = textArea.getTokenListForLine(i);
					while (temp!=null && temp.isPaintable()) {
						if (temp.is(type, lexeme)) {
							try {
								int end = temp.offset + temp.textCount;
								h.addMarkedOccurrenceHighlight(temp.offset, end, p);
							} catch (BadLocationException ble) {
								ble.printStackTrace(); // Never happens
							}
						}
						temp = temp.getNextToken();
					}
				}
//textArea.repaint();
// TODO: Do a textArea.repaint() instead of repainting each marker as it's added if count is huge
			}

		} finally {
			doc.readUnlock();
			//time = System.currentTimeMillis() - time;
			//System.out.println("MarkOccurrencesSupport took: " + time + " ms");
		}

		textArea.fireMarkedOccurrencesChanged();

	}


	/**
	 * Called when the caret moves in the text area.
	 *
	 * @param e The event.
	 */
	public void caretUpdate(CaretEvent e) {
		timer.restart();
	}


	/**
	 * Returns the color being used to mark occurrences.
	 *
	 * @return The color being used.
	 * @see #setColor(Paint)
	 */
	public Color getColor() {
		return p.getColor();
	}


	/**
	 * Returns the delay, in milliseconds.
	 *
	 * @return The delay.
	 * @see #setDelay(int)
	 */
	public int getDelay() {
		return timer.getDelay();
	}


	/**
	 * Returns whether a border is painted around marked occurrences.
	 *
	 * @return Whether a border is painted.
	 * @see #setPaintBorder(boolean)
	 * @see #getColor()
	 */
	public boolean getPaintBorder() {
		return p.getPaintBorder();
	}


	/**
	 * Installs this listener on a text area.  If it is already installed on
	 * another text area, it is uninstalled first.
	 *
	 * @param textArea The text area to install on.
	 */
	public void install(RSyntaxTextArea textArea) {
		if (this.textArea!=null) {
			uninstall();
		}
		this.textArea = textArea;
		textArea.addCaretListener(this);
		if (textArea.getMarkOccurrencesColor()!=null) {
			setColor(textArea.getMarkOccurrencesColor());
		}
	}


	/**
	 * Returns whether the specified token is a single non-word char (e.g. not
	 * in <tt>[A-Za-z]</tt>.  This is a HACK to work around the fact that many
	 * standard token makers return things like semicolons and periods as
	 * {@link Token#IDENTIFIER}s just to make the syntax highlighting coloring
	 * look a little better.
	 * 
	 * @param t The token to check.  This cannot be <tt>null</tt>.
	 * @return Whether the token is a single non-word char.
	 */
	private static final boolean isNonWordChar(Token t) {
		return t.textCount==1 &&
				!RSyntaxUtilities.isLetter(t.text[t.textOffset]);
	}


	/**
	 * Returns whether the specified token is a type that we can do a
	 * "mark occurrences" on.
	 *
	 * @param t The token.
	 * @return Whether we should mark all occurrences of this token.
	 */
	private boolean isValidType(Token t) {
		return textArea.getMarkOccurrencesOfTokenType(t.type);
	}


	/**
	 * Removes all highlights added to the text area by this listener.
	 */
	private void removeHighlights() {
		if (textArea!=null) {
			RSyntaxTextAreaHighlighter h = (RSyntaxTextAreaHighlighter)
													textArea.getHighlighter();
			h.clearMarkOccurrencesHighlights();
		}
	}


	/**
	 * Sets the color to use when marking occurrences.
	 *
	 * @param color The color to use.
	 * @see #getColor()
	 * @see #setPaintBorder(boolean)
	 */
	public void setColor(Color color) {
		p.setColor(color);
		if (textArea!=null) {
			removeHighlights();
			caretUpdate(null); // Force a highlight repaint.
		}
	}


	/**
	 * Sets the delay between the last caret position change and when the
	 * text is scanned for matching identifiers.  A delay is needed to prevent
	 * repeated scanning while the user is typing.
	 *
	 * @param delay The new delay.
	 * @see #getDelay()
	 */
	public void setDelay(int delay) {
		timer.setDelay(delay);
	}


	/**
	 * Toggles whether a border is painted around marked highlights.
	 *
	 * @param paint Whether to paint a border.
	 * @see #getPaintBorder()
	 * @see #setColor(Color)
	 */
	public void setPaintBorder(boolean paint) {
		if (paint!=p.getPaintBorder()) {
			p.setPaintBorder(paint);
			if (textArea!=null) {
				textArea.repaint();
			}
		}
	}


	/**
	 * Uninstalls this listener from the current text area.  Does nothing if
	 * it not currently installed on any text area.
	 *
	 * @see #install(RSyntaxTextArea)
	 */
	public void uninstall() {
		if (textArea!=null) {
			removeHighlights();
			textArea.removeCaretListener(this);
		}
	}


}