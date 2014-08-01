/*
 * 02/06/2011
 *
 * ActiveLineRangeListener.java - Listens for "active line range" changes
 * in an RSyntaxTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.util.EventListener;


/**
 * Listens for "active line range" events from an <code>RSyntaxTextArea</code>.
 * If a text area contains some semantic knowledge of the programming language
 * being edited, it may broadcast {@link ActiveLineRangeEvent}s whenever the
 * caret moves into a new "block" of code.  Listeners can listen for these
 * events and respond accordingly.<p>
 * 
 * See the <code>RSTALanguageSupport</code> project at
 * <a href="http://fifesoft.com">http://fifesoft.com</a> for some
 * <code>LanguageSupport</code> implementations that may broadcast these
 * events.  Note that if an RSTA/LanguageSupport does not support broadcasting
 * these events, the listener will simply never receive any notifications.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface ActiveLineRangeListener extends EventListener {


	/**
	 * Called whenever the "active line range" changes.
	 *
	 * @param e Information about the line range change.  If there is no longer
	 *        an "active line range," the "minimum" and "maximum" line values
	 *        should both be <code>-1</code>.
	 */
	public void activeLineRangeChanged(ActiveLineRangeEvent e);


}