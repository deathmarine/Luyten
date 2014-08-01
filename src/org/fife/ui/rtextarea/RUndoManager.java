/*
 * 12/06/2008
 *
 * RUndoManager.java - Handles undo/redo behavior for RTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;


/**
 * This class manages undos/redos for a particular editor pane.  It groups
 * all undos that occur one character position apart together, to avoid
 * Java's horrible "one character at a time" undo behavior.  It also
 * recognizes "replace" actions (i.e., text is selected, then the user
 * types), and treats it as a single action, instead of a remove/insert
 * action pair.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class RUndoManager extends UndoManager {

	private RCompoundEdit compoundEdit;
	private RTextArea textArea;
	private int lastOffset;
	private String cantUndoText;
	private String cantRedoText;

	private int internalAtomicEditDepth;

	private static final String MSG	= "org.fife.ui.rtextarea.RTextArea";


	/**
	 * Constructor.
	 *
	 * @param textArea The parent text area.
	 */
	public RUndoManager(RTextArea textArea) {
		this.textArea = textArea;
		ResourceBundle msg = ResourceBundle.getBundle(MSG);
		cantUndoText = msg.getString("Action.CantUndo.Name");
		cantRedoText = msg.getString("Action.CantRedo.Name");
	}


	/**
	 * Begins an "atomic" edit.  This method is called when RTextArea
	 * KNOWS that some edits should be compound automatically, such as
	 * when the user is typing in overwrite mode (the deletion of the
	 * current char + insertion of the new one) or the playing back of a
	 * macro.
	 *
	 * @see #endInternalAtomicEdit()
	 */
	public void beginInternalAtomicEdit() {
		if (++internalAtomicEditDepth==1) {
			if (compoundEdit!=null)
				compoundEdit.end();
			compoundEdit = new RCompoundEdit();
		}
	}


	/**
	 * Ends an "atomic" edit.
	 *
	 * @see #beginInternalAtomicEdit()
	 */
	public void endInternalAtomicEdit() {
		if (internalAtomicEditDepth>0 && --internalAtomicEditDepth==0) {
			addEdit(compoundEdit);
			compoundEdit.end();
			compoundEdit = null;
			updateActions();	// Needed to show the new display name.
		}
	}


	/**
	 * Returns the localized "Can't Redo" string.
	 *
	 * @return The localized "Can't Redo" string.
	 * @see #getCantUndoText()
	 */
	public String getCantRedoText() {
		return cantRedoText;
	}


	/**
	 * Returns the localized "Can't Undo" string.
	 *
	 * @return The localized "Can't Undo" string.
	 * @see #getCantRedoText()
	 */
	public String getCantUndoText() {
		return cantUndoText;
	}


	/**
	 * {@inheritDoc}
	 */
	public void redo() throws CannotRedoException {
		super.redo();
		updateActions();
	}


	private RCompoundEdit startCompoundEdit(UndoableEdit edit) {
		lastOffset = textArea.getCaretPosition();
		compoundEdit = new RCompoundEdit();
		compoundEdit.addEdit(edit);
		addEdit(compoundEdit);
		return compoundEdit;
	}


	/**
	 * {@inheritDoc}
	 */
	public void undo() throws CannotUndoException {
		super.undo();
		updateActions();
	}


	public void undoableEditHappened(UndoableEditEvent e) {

		// This happens when the first undoable edit occurs, and
		// just after an undo.  So, we need to update our actions.
		if (compoundEdit==null) {
			compoundEdit = startCompoundEdit(e.getEdit());
			updateActions();
			return;
		}

		else if (internalAtomicEditDepth>0) {
			compoundEdit.addEdit(e.getEdit());
			return;
		}

		// This happens when there's already an undo that has occurred.
		// Test to see if these undos are on back-to-back characters,
		// and if they are, group them as a single edit.  Since an
		// undo has already occurred, there is no need to update our
		// actions here.
		int diff = textArea.getCaretPosition() - lastOffset;
		// "<=1" allows contiguous "overwrite mode" key presses to be
		// grouped together.
		if (Math.abs(diff)<=1) {//==1) {
			compoundEdit.addEdit(e.getEdit());
			lastOffset += diff;
			//updateActions();
			return;
		}

		// This happens when this UndoableEdit didn't occur at the
		// character just after the previous undlabeledit.  Since an
		// undo has already occurred, there is no need to update our
		// actions here either.
		compoundEdit.end();
		compoundEdit = startCompoundEdit(e.getEdit());
		//updateActions();

	}


	/**
	 * Ensures that undo/redo actions are enabled appropriately and have
	 * descriptive text at all times.
	 */
	public void updateActions() {

		String text;

		Action a = RTextArea.getAction(RTextArea.UNDO_ACTION);
		if (canUndo()) {
			a.setEnabled(true);
			text = getUndoPresentationName();
			a.putValue(Action.NAME, text);
			a.putValue(Action.SHORT_DESCRIPTION, text);
		}
		else {
			if (a.isEnabled()) {
				a.setEnabled(false);
				text = cantUndoText;
				a.putValue(Action.NAME, text);
				a.putValue(Action.SHORT_DESCRIPTION, text);
			}
		}

		a = RTextArea.getAction(RTextArea.REDO_ACTION);
		if (canRedo()) {
			a.setEnabled(true);
			text = getRedoPresentationName();
			a.putValue(Action.NAME, text);
			a.putValue(Action.SHORT_DESCRIPTION, text);
		}
		else {
			if (a.isEnabled()) {
				a.setEnabled(false);
				text = cantRedoText;
				a.putValue(Action.NAME, text);
				a.putValue(Action.SHORT_DESCRIPTION, text);
			}
		}

	}

	/**
	 * The edit used by {@link RUndoManager}.
	 */
	class RCompoundEdit extends CompoundEdit {

		public String getUndoPresentationName() {
			return UIManager.getString("AbstractUndoableEdit.undoText");
		}

		public String getRedoPresentationName() {
			return UIManager.getString("AbstractUndoableEdit.redoText");
		}

		public boolean isInProgress() {
			return false;
		}

		public void undo() throws CannotUndoException {
			if (compoundEdit!=null)
				compoundEdit.end();
			super.undo();
			compoundEdit = null;
		}

	}


}