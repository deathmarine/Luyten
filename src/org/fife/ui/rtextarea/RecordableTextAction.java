/*
 * 08/19/2004
 *
 * RecordableTextAction.java - An action that can be recorded and replayed
 * in an RTextArea macro.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;


/**
 * The base action used by the actions defined in
 * {@link RTextAreaEditorKit}.  This action is what allows instances of
 * <code>RTextArea</code> to record keystrokes into "macros;" if an action is
 * recordable and occurs while the user is recording a macro, it adds itself to
 * the currently-being-recorded macro.
 *
 * @author Robert Futrell
 * @version 0.5
 */
public abstract class RecordableTextAction extends TextAction {

	/**
	 * Whether or not this text action should be recorded in a macro.
	 */
	private boolean isRecordable;


	/**
	 * Constructor.
	 *
	 * @param text The text (name) associated with the action.
	 */
	public RecordableTextAction(String text) {
		this(text, null, null, null, null);
	}


	/**
	 * Constructor.
	 *
	 * @param text The text (name) associated with the action.
	 * @param icon The icon associated with the action.
	 * @param desc The description of the action.
	 * @param mnemonic The mnemonic for the action.
	 * @param accelerator The accelerator key for the action.
	 */
	public RecordableTextAction(String text, Icon icon, String desc,
					Integer mnemonic, KeyStroke accelerator) {
		super(text);
		putValue(SMALL_ICON, icon);
		putValue(SHORT_DESCRIPTION, desc);
		putValue(ACCELERATOR_KEY, accelerator);
		putValue(MNEMONIC_KEY, mnemonic);
		setRecordable(true);
	}


	/**
	 * This method is final so that you cannot override it and mess up the
	 * macro-recording part of it.  The actual meat of the action is in
	 * <code>actionPerformedImpl</code>.
	 *
	 * @param e The action being performed.
	 * @see #actionPerformedImpl
	 */
	public final void actionPerformed(ActionEvent e) {

		JTextComponent textComponent = getTextComponent(e);
		if (textComponent instanceof RTextArea) {
			RTextArea textArea = (RTextArea)textComponent;
			//System.err.println("Recordable action: " + getMacroID());
			if (RTextArea.isRecordingMacro() && isRecordable()) {
				int mod = e.getModifiers();
				// We ignore keypresses involving ctrl/alt/meta if they are
				// "default" keypresses - i.e., they aren't some special
				// action like paste (e.g., paste would return Ctrl+V, but
				// its action would be "paste-action").
				String macroID = getMacroID();
//System.err.println(macroID);
//System.err.println("... " + (mod&ActionEvent.ALT_MASK));
//System.err.println("... " + (mod&ActionEvent.CTRL_MASK));
//System.err.println("... " + (mod&ActionEvent.META_MASK));
				if (!DefaultEditorKit.defaultKeyTypedAction.equals(macroID)
					|| (
						(mod&ActionEvent.ALT_MASK)==0 &&
						(mod&ActionEvent.CTRL_MASK)==0 &&
						(mod&ActionEvent.META_MASK)==0)
					) {
					String command = e.getActionCommand();
					RTextArea.addToCurrentMacro(macroID, command);
					//System.err.println("... recording it!");
				}
			}
			actionPerformedImpl(e, textArea);
		}

	}


	/**
	 * The actual meat of the action.  If you wish to subclass this action
	 * and modify its behavior, this is the method to override.
	 *
	 * @param e The action being performed.
	 * @param textArea The text area "receiving" the action.
	 * @see #actionPerformed
	 */
	public abstract void actionPerformedImpl(ActionEvent e, RTextArea textArea);


	/**
	 * Returns the accelerator for this action.
	 *
	 * @return The accelerator.
	 * @see #setAccelerator(KeyStroke)
	 */
	public KeyStroke getAccelerator() {
		return (KeyStroke)getValue(ACCELERATOR_KEY);
	}


	/**
	 * Returns the description for this action.
	 *
	 * @return The description.
	 */
	public String getDescription() {
		return (String)getValue(SHORT_DESCRIPTION);
	}


	/**
	 * Returns the icon for this action.
	 *
	 * @return The icon.
	 */
	public Icon getIcon() {
		return (Icon)getValue(SMALL_ICON);
	}


	/**
	 * Returns the identifier for this macro.  This method makes it so that you
	 * can create an instance of the <code>RTextAreaEditorKit.CutAction</code>
	 * action, for example, rename it to "Remove", and it will still be
	 * recorded as a "cut" action.  Subclasses should return a unique string
	 * from this method; preferably the name of the action.<p>
	 * If you subclass a <code>RecordableTextAction</code>, you should NOT
	 * override this method; if you do, the action may not be properly
	 * recorded in a macro.
	 *
	 * @return The internally-used macro ID.
	 */
	public abstract String getMacroID();


	/**
	 * Returns the mnemonic for this action.
	 *
	 * @return The mnemonic, or <code>-1</code> if not defined.
	 * @see #setMnemonic(char)
	 * @see #setMnemonic(Integer)
	 */
	public int getMnemonic() {
		Integer i = (Integer)getValue(MNEMONIC_KEY);
		return i!=null ? i.intValue() : -1;
	}


	/**
	 * Returns the name of this action.
	 *
	 * @return The name of this action.
	 * @see #setName(String)
	 */
	public String getName() {
		return (String)getValue(NAME);
	}


	/**
	 * Returns whether or not this action will be recorded and replayed in
	 * a macro.
	 *
	 * @return Whether or not this action will be recorded and replayed.
	 * @see #setRecordable
	 */
	public boolean isRecordable() {
		return isRecordable;
	}


	/**
	 * Sets the accelerator for this action.
	 *
	 * @param accelerator The new accelerator.
	 * @see #getAccelerator()
	 */
	public void setAccelerator(KeyStroke accelerator) {
		putValue(ACCELERATOR_KEY, accelerator);
	}


	/**
	 * Sets the mnemonic for this action.
	 *
	 * @param mnemonic The new mnemonic.
	 * @see #setMnemonic(Integer)
	 * @see #getMnemonic()
	 */
	public void setMnemonic(char mnemonic) {
		setMnemonic(new Integer(mnemonic));
	}


	/**
	 * Sets the mnemonic for this action.
	 *
	 * @param mnemonic The new mnemonic.
	 * @see #setMnemonic(char)
	 * @see #getMnemonic()
	 */
	public void setMnemonic(Integer mnemonic) {
		putValue(MNEMONIC_KEY, mnemonic);
	}


	/**
	 * Sets the name of this action.
	 *
	 * @param name The new name.
	 * @see #getName()
	 */
	public void setName(String name) {
		putValue(NAME, name);
	}


	/**
	 * Sets the name, mnemonic, and description of this action.
	 *
	 * @param msg The resource bundle.
	 * @param keyRoot The root of the keys for the properties.
	 *        "<code>.Name</code>", "<code>.Mnemonic</code>", and
	 *        "<code>.Desc</code>" will be appended to create the key for each
	 *        property.
	 */
	public void setProperties(ResourceBundle msg, String keyRoot) {
		setName(msg.getString(keyRoot + ".Name"));
		setMnemonic(msg.getString(keyRoot + ".Mnemonic").charAt(0));
		setShortDescription(msg.getString(keyRoot + ".Desc"));
	}


	/**
	 * Sets whether or not this action will be recorded and replayed in
	 * a macro.
	 *
	 * @param recordable Whether or not this action should be recorded
	 *        and replayed.
	 * @see #isRecordable
	 */
	public void setRecordable(boolean recordable) {
		isRecordable = recordable;
	}


	/**
	 * Sets the short description for this action.
	 *
	 * @param shortDesc The short description for this action.
	 */
	public void setShortDescription(String shortDesc) {
		putValue(SHORT_DESCRIPTION, shortDesc);
	}


}