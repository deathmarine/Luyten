/*
 * 01/11/2011
 *
 * PopupWindowDecorator.java - Hook allowing hosting applications to decorate
 * JWindows created by the AutoComplete library.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import javax.swing.JWindow;


/**
 * A hook allowing hosting applications to decorate JWindows created by the
 * AutoComplete library.  For example, you could use the
 * <a href="http://jgoodies.com/">JGoodies</a> library to add drop shadows
 * to the windows. 
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class PopupWindowDecorator {

	/**
	 * The singleton instance of this class.
	 */
	private static PopupWindowDecorator decorator;


	/**
	 * Callback called whenever an appropriate JWindow is created by the
	 * AutoComplete library.  Implementations can decorate the window however
	 * they see fit.
	 *
	 * @param window The newly-created window.
	 */
	public abstract void decorate(JWindow window);


	/**
	 * Returns the singleton instance of this class.  This should only be
	 * called on the EDT.
	 *
	 * @return The singleton instance of this class, or <code>null</code>
	 *         for none.
	 * @see #set(PopupWindowDecorator)
	 */
	public static PopupWindowDecorator get() {
		return decorator;
	}


	/**
	 * Sets the singleton instance of this class.  This should only be called
	 * on the EDT.
	 *
	 * @param decorator The new instance of this class.  This may be
	 *        <code>null</code>.
	 * @see #get()
	 */
	public static void set(PopupWindowDecorator decorator) {
		PopupWindowDecorator.decorator = decorator;
	}


}