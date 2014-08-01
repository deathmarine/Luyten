/*
 * 02/17/2009
 *
 * AbstractGutterComponent.java - A component that can be displayed in a Gutter.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Shape;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.text.View;


/**
 * A component that can be displayed in a {@link Gutter}.
 *
 * @author Robert Futrell
 * @version 1.0
 */
abstract class AbstractGutterComponent extends JPanel {

	/**
	 * The text area whose lines we are marking with icons.
	 */
	protected RTextArea textArea;

	/**
	 * The number of lines in the text area.
	 */
	protected int currentLineCount;


	/**
	 * Constructor.
	 *
	 * @param textArea The text area.
	 */
	public AbstractGutterComponent(RTextArea textArea) {
		setTextArea(textArea);
	}


	/**
	 * Returns the bounds of a child view as a rectangle, since
	 * <code>View</code>s tend to use <code>Shape</code>.
	 *
	 * @param parent The parent view of the child whose bounds we're getting.
	 * @param line The index of the child view.
	 * @param editorRect Returned from the text area's
	 *        <code>getVisibleEditorRect</code> method.
	 * @return The child view's bounds.
	 */
	protected static final Rectangle getChildViewBounds(View parent, int line,
										Rectangle editorRect) {
		Shape alloc = parent.getChildAllocation(line, editorRect);
		if (alloc==null) {
			// WrappedSyntaxView can have this when made so small it's
			// no longer visible
			return new Rectangle();
		}
		return alloc instanceof Rectangle ? (Rectangle)alloc :
										alloc.getBounds();
	}


	/**
	 * Returns the parent <code>Gutter</code> component.
	 *
	 * @return The parent <code>Gutter</code>.
	 */
	protected Gutter getGutter() {
		Container parent = getParent();
		return (parent instanceof Gutter) ? (Gutter)parent : null;
	}


	/**
	 * Called when text is inserted to or removed from the text area.
	 * Implementations can take this opportunity to repaint, revalidate, etc.
	 *
	 * @param e The document event.
	 */
	abstract void handleDocumentEvent(DocumentEvent e);


	/**
	 * Called when the line heights of the text area change.  This is usually
	 * the result of one or more of the fonts in the editor changing.
	 */
	abstract void lineHeightsChanged();


	/**
	 * Sets the text area being displayed.  Subclasses can override, but
	 * should call the super implementation.
	 *
	 * @param textArea The text area.
	 */
	public void setTextArea(RTextArea textArea) {
		this.textArea = textArea;
		int lineCount = textArea==null ? 0 : textArea.getLineCount();
		if (currentLineCount!=lineCount) {
			currentLineCount = lineCount;
			repaint();
		}
	}


}