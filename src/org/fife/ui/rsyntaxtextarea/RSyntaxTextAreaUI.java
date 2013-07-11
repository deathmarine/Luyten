/*
 * 02/24/2004
 *
 * RSyntaxTextAreaUI.java - UI for an RSyntaxTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.InputMapUIResource;
import javax.swing.text.*;

import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextAreaUI;


/**
 * UI used by <code>RSyntaxTextArea</code>.  This allows us to implement
 * syntax highlighting.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class RSyntaxTextAreaUI extends RTextAreaUI {

	private static final String SHARED_ACTION_MAP_NAME	= "RSyntaxTextAreaUI.actionMap";
	private static final String SHARED_INPUT_MAP_NAME		= "RSyntaxTextAreaUI.inputMap";
	private static final EditorKit defaultKit			= new RSyntaxTextAreaEditorKit();


	public static ComponentUI createUI(JComponent ta) {
		return new RSyntaxTextAreaUI(ta);
	}


	/**
	 * Constructor.
	 */
	public RSyntaxTextAreaUI(JComponent rSyntaxTextArea) {
		super(rSyntaxTextArea);
	}


    /**
     * Creates the view for an element.
     *
     * @param elem The element.
     * @return The view.
     */
	public View create(Element elem) {
		RTextArea c = getRTextArea();
		if (c instanceof RSyntaxTextArea) {
			RSyntaxTextArea area = (RSyntaxTextArea) c;
			View v;
			if (area.getLineWrap())
				v = new WrappedSyntaxView(elem);
			else
				v = new SyntaxView(elem);
			return v;
		}
		return null;
	}


	/**
	 * Creates the highlighter to use for syntax text areas.
	 *
	 * @return The highlighter.
	 */
	protected Highlighter createHighlighter() {
		return new RSyntaxTextAreaHighlighter();
	}


	/**
	 * Returns the name to use to cache/fetch the shared action map.  This
	 * should be overridden by subclasses if the subclass has its own custom
	 * editor kit to install, so its actions get picked up.
	 *
	 * @return The name of the cached action map.
	 */
	protected String getActionMapName() {
		return SHARED_ACTION_MAP_NAME;
	}


	/**
	 * Fetches the EditorKit for the UI.
	 *
	 * @param tc The text component for which this UI is installed.
	 * @return The editor capabilities.
	 * @see javax.swing.plaf.TextUI#getEditorKit
	 */
	public EditorKit getEditorKit(JTextComponent tc) {
		return defaultKit;
	}


	/**
	 * Get the InputMap to use for the UI.<p>  
	 *
	 * This method is not named <code>getInputMap()</code> because there is
	 * a package-private method in <code>BasicTextAreaUI</code> with that name.
	 * Thus, creating a new method with that name causes certain compilers to
	 * issue warnings that you are not actually overriding the original method
	 * (since it is package-private).
	 */
	protected InputMap getRTextAreaInputMap() {
		InputMap map = new InputMapUIResource();
		InputMap shared = (InputMap)UIManager.get(SHARED_INPUT_MAP_NAME);
		if (shared==null) {
			shared = new RSyntaxTextAreaDefaultInputMap();
			UIManager.put(SHARED_INPUT_MAP_NAME, shared);
		}
		//KeyStroke[] keys = shared.allKeys();
		//for (int i=0; i<keys.length; i++)
		//	System.err.println(keys[i] + " -> " + shared.get(keys[i]));
		map.setParent(shared);
		return map;
	}


	/**
	 * Paints the text area's background.
	 *
	 * @param g The graphics component on which to paint.
	 */
	protected void paintBackground(Graphics g) {
		super.paintBackground(g);
		paintMatchedBracket(g);
	}


	/**
	 * Paints the "matched bracket", if any.
	 *
	 * @param g The graphics context.
	 */
	protected void paintMatchedBracket(Graphics g) {
		RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
		if (rsta.isBracketMatchingEnabled()) {
			Rectangle match = rsta.getMatchRectangle();
			if (match!=null) {
				paintMatchedBracketImpl(g, rsta, match);
			}
			if (rsta.getPaintMatchedBracketPair()) {
				Rectangle dotRect = rsta.getDotRectangle();
				if (dotRect!=null) { // should always be true
					paintMatchedBracketImpl(g, rsta, dotRect);
				}
			}
		}
	}


	private void paintMatchedBracketImpl(Graphics g, RSyntaxTextArea rsta,
			Rectangle r) {
		// We must add "-1" to the height because otherwise we'll paint below
		// the region that gets invalidated.
		if (rsta.getAnimateBracketMatching()) {
			Color bg = rsta.getMatchedBracketBGColor();
			if (bg!=null) {
				g.setColor(bg);
				g.fillRoundRect(r.x,r.y, r.width,r.height-1, 5,5);
			}
			g.setColor(rsta.getMatchedBracketBorderColor());
			g.drawRoundRect(r.x,r.y, r.width,r.height-1, 5,5);
		}
		else {
			Color bg = rsta.getMatchedBracketBGColor();
			if (bg!=null) {
				g.setColor(bg);
				g.fillRect(r.x,r.y, r.width,r.height-1);
			}
			g.setColor(rsta.getMatchedBracketBorderColor());
			g.drawRect(r.x,r.y, r.width,r.height-1);
		}
	}


	/**
	 * Gets called whenever a bound property is changed on this UI's
	 * <code>RSyntaxTextArea</code>.
	 *
	 * @param e The property change event.
	 */
	protected void propertyChange(PropertyChangeEvent e) {

		String name = e.getPropertyName();

		// If they change the syntax scheme, we must do this so that
		// WrappedSyntaxView(_TEST) updates its child views properly.
		if (name.equals(RSyntaxTextArea.SYNTAX_SCHEME_PROPERTY)) {
			modelChanged();
		}

		// Everything else is general to all RTextAreas.
		else {
			super.propertyChange(e);
		}

	}


	/**
	 * Updates the view.  This should be called when the underlying
	 * <code>RSyntaxTextArea</code> changes its syntax editing style.
	 */
	public void refreshSyntaxHighlighting() {
		modelChanged();
	}


	/**
	 * Returns the y-coordinate of the specified line.<p>
	 *
	 * This method is quicker than using traditional
	 * <code>modelToView(int)</code> calls, as the entire bounding box isn't
	 * computed.
	 */
	public int yForLine(int line) throws BadLocationException {
		Rectangle alloc = getVisibleEditorRect();
		if (alloc!=null) {
			RSTAView view = (RSTAView)getRootView(textArea).getView(0);
			return view.yForLine(alloc, line);
		}
		return -1;
	}


	/**
	 * Returns the y-coordinate of the line containing a specified offset.<p>
	 *
	 * This is faster than calling <code>modelToView(offs).y</code>, so it is
	 * preferred if you do not need the actual bounding box.
	 */
	public int yForLineContaining(int offs) throws BadLocationException {
		Rectangle alloc = getVisibleEditorRect();
		if (alloc!=null) {
			RSTAView view = (RSTAView)getRootView(textArea).getView(0);
			return view.yForLineContaining(alloc, offs);
		}
		return -1;
	}


}