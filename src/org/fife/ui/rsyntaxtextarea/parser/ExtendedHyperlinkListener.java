/*
 * 07/31/2009
 *
 * ExtendedHyeprlinkListener.java - A hyperlink event from a FocusableTip.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.parser;

import java.util.EventListener;
import javax.swing.event.HyperlinkEvent;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.focusabletip.FocusableTip;


/**
 * Listens for hyperlink events from {@link FocusableTip}s.  In addition to
 * the link event, the text area that the tip is for is also received, which
 * allows the listener to modify the displayed content, if desired.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface ExtendedHyperlinkListener extends EventListener {


	/**
	 * Called when a link in a {@link FocusableTip} is clicked.
	 *
	 * @param textArea The text area displaying the tip.
	 * @param e The event.
	 */
	public void linkClicked(RSyntaxTextArea textArea, HyperlinkEvent e);


}