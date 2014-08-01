/*
 * 10/08/2011
 *
 * FoldManager.java - Manages code folding in an RSyntaxTextArea instance.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;
import org.fife.ui.rsyntaxtextarea.parser.Parser;


/**
 * Manages code folding in an instance of RSyntaxTextArea.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FoldManager {

	private RSyntaxTextArea textArea;
	private FoldParser parser;
	private List folds;
	private boolean codeFoldingEnabled;
	private PropertyChangeSupport support;


	/**
	 * Property fired when folds have been updated.
	 */
	public static final String PROPERTY_FOLDS_UPDATED = "FoldsUpdated";


	/**
	 * Constructor.
	 *
	 * @param textArea The text area whose folds we are managing.
	 */
	public FoldManager(RSyntaxTextArea textArea) {
		this.textArea = textArea;
		support = new PropertyChangeSupport(this);
		Listener l = new Listener();
		textArea.getDocument().addDocumentListener(l);
		textArea.addPropertyChangeListener(RSyntaxTextArea.SYNTAX_STYLE_PROPERTY, l);
		folds = new ArrayList();
		updateFoldParser(); 
	}


	/**
	 * Adds a property change listener to this fold manager.
	 *
	 * @param l The new listener.
	 * @see #removePropertyChangeListener(PropertyChangeListener)
	 */
	public void addPropertyChangeListener(PropertyChangeListener l) {
		support.addPropertyChangeListener(l);
	}


	/**
	 * Removes all folds.
	 */
	public void clear() {
		folds.clear();
	}


	/**
	 * Ensures that the specified offset is not hidden in a collapsed fold.
	 * Any folds containing this offset that are collapsed will be expanded.
	 *
	 * @param offs The offset.
	 * @return Whether any folds had to be opened.
	 * @see #getDeepestFoldContaining(int)
	 */
	public boolean ensureOffsetNotInClosedFold(int offs) {
		boolean foldsOpened = false;
		Fold fold = getDeepestFoldContaining(offs);
		while (fold!=null) {
			if (fold.isCollapsed()) {
				fold.setCollapsed(false);
				foldsOpened = true;
			}
			fold = fold.getParent();
		}
		return foldsOpened;
	}


	/**
	 * Returns the "deepest" nested fold containing the specified offset.
	 *
	 * @param offs The offset.
	 * @return The deepest fold containing the offset, or <code>null</code> if
	 *         no fold contains the offset.
	 */
	public Fold getDeepestFoldContaining(int offs) {
		Fold deepestFold = null;
		if (offs>-1) {
			for (int i=0; i<folds.size(); i++) {
				Fold fold = getFold(i);
				if (fold.containsOffset(offs)) {
					deepestFold = fold.getDeepestFoldContaining(offs);
					break;
				}
			}
		}
		return deepestFold;
	}


	/**
	 * Returns the "deepest" open fold containing the specified offset.
	 *
	 * @param offs The offset.
	 * @return The fold, or <code>null</code> if no open fold contains the
	 *         offset.
	 */
	public Fold getDeepestOpenFoldContaining(int offs) {

		Fold deepestFold = null;

		if (offs>-1) {
			for (int i=0; i<folds.size(); i++) {
				Fold fold = getFold(i);
				if (fold.containsOffset(offs)) {
					if (fold.isCollapsed()) {
						return null;
					}
					deepestFold = fold.getDeepestOpenFoldContaining(offs);
					break;
				}
			}
		}

		return deepestFold;

	}


	/**
	 * Returns a specific top-level fold, which may have child folds.
	 *
	 * @param index The index of the fold.
	 * @return The fold.
	 * @see #getFoldCount()
	 */
	public Fold getFold(int index) {
		return (Fold)folds.get(index);
	}


	/**
	 * Returns the number of top-level folds.
	 *
	 * @return The number of top-level folds.
	 * @see #getFold(int)
	 */
	public int getFoldCount() {
		return folds.size();
	}


	/**
	 * Returns the fold region that starts at the specified line.
	 *
	 * @param line The line number.
	 * @return The fold, or <code>null</code> if the line is not the start
	 *         of a fold region.
	 * @see #isFoldStartLine(int)
	 */
	public Fold getFoldForLine(int line) {
		return getFoldForLineImpl(null, folds, line);
	}


private Fold getFoldForLineImpl(Fold parent, List folds, int line) {

	int low = 0;
	int high = folds.size() - 1;

	while (low <= high) {
		int mid = (low + high) >> 1;
		Fold midFold = (Fold)folds.get(mid);
		int startLine = midFold.getStartLine();
		if (line==startLine) {
			return midFold;
		}
		else if (line<startLine) {
			high = mid - 1;
		}
		else {
			int endLine = midFold.getEndLine();
			if (line>=endLine) {
				low = mid + 1;
			}
			else { // line>startLine && line<=endLine
				List children = midFold.getChildren();
				return children!=null ? getFoldForLineImpl(midFold, children, line) : null;
			}
		}
	}

	return null; // No fold for this line
}


	/**
	 * Returns the total number of hidden (folded) lines.
	 *
	 * @return The total number of hidden (folded) lines.
	 * @see #getHiddenLineCountAbove(int)
	 */
	public int getHiddenLineCount() {
		int count = 0;
		for (int i=0; i<folds.size(); i++) {
			count += ((Fold)folds.get(i)).getCollapsedLineCount();
		}
		return count;
	}


	/**
	 * Returns the number of lines "hidden" by collapsed folds above the
	 * specified line.
	 *
	 * @param line The line.  This is the line number for a logical line.
	 *        For the line number of a physical line (i.e. visible, not folded),
	 *        use <code>getHiddenLineCountAbove(int, true)</code>.
	 * @return The number of lines hidden in folds above <code>line</code>.
	 * @see #getHiddenLineCountAbove(int, boolean)
	 */
	public int getHiddenLineCountAbove(int line) {
		return getHiddenLineCountAbove(line, false);
	}
	

	/**
	 * Returns the number of lines "hidden" by collapsed folds above the
	 * specified line.
	 *
	 * @param line The line.
	 * @param physical Whether <code>line</code> is the number of a physical
	 *        line (i.e. visible, not code-folded), or a logical one (i.e. any
	 *        line from the model).  If <code>line</code> was determined by a
	 *        raw line calculation (i.e. <code>(visibleTopY / lineHeight)</code>),
	 *        this value should be <code>true</code>.  It should be
	 *        <code>false</code> when it was calculated from an offset in the
	 *        document (for example).
	 * @return The number of lines hidden in folds above <code>line</code>.
	 */
	public int getHiddenLineCountAbove(int line, boolean physical) {

		int count = 0;

		for (int i=0; i<folds.size(); i++) {
			Fold fold = (Fold)folds.get(i);
			int comp = physical ? line+count : line;
			if (fold.getStartLine()>=comp) {
				break;
			}
			count += getHiddenLineCountAboveImpl(fold, comp, physical);
		}

		return count;

	}


	/**
	 * Returns the number of lines "hidden" by collapsed folds above the
	 * specified line.
	 *
	 * @param fold The current fold in the recursive algorithm.  It and its
	 *        children are examined.
	 * @param line The line.
	 * @param physical Whether <code>line</code> is the number of a physical
	 *        line (i.e. visible, not code-folded), or a logical one (i.e. any
	 *        line from the model).  If <code>line</code> was determined by a
	 *        raw line calculation (i.e. <code>(visibleTopY / lineHeight)</code>),
	 *        this value should be <code>true</code>.  It should be
	 *        <code>false</code> when it was calculated from an offset in the
	 *        document (for example).
	 * @return The number of lines hidden in folds that are descendants of
	 *         <code>fold</code>, or <code>fold</code> itself, above
	 *         <code>line</code>.
	 */
	private int getHiddenLineCountAboveImpl(Fold fold, int line, boolean physical) {

		int count = 0;

		if (fold.getEndLine()<line ||
				(fold.isCollapsed() && fold.getStartLine()<line)) {
			count = fold.getCollapsedLineCount();
		}
		else {
			int childCount = fold.getChildCount();
			for (int i=0; i<childCount; i++) {
				Fold child = fold.getChild(i);
				int comp = physical ? line+count : line;
				if (child.getStartLine()>=comp) {
					break;
				}
				count += getHiddenLineCountAboveImpl(child, comp, physical);
			}
		}

		return count;

	}


	/**
	 * Returns the last visible line in the text area, taking into account
	 * folds.
	 *
	 * @return The last visible line.
	 */
	public int getLastVisibleLine() {

		int lastLine = textArea.getLineCount() - 1;

		if (isCodeFoldingSupportedAndEnabled()) {
			int foldCount = getFoldCount();
			if (foldCount>0) {
				Fold lastFold = getFold(foldCount-1);
				if (lastFold.containsLine(lastLine)) {
					if (lastFold.isCollapsed()) {
						lastLine = lastFold.getStartLine();
					}
					else { // Child fold may end on the same line as parent
						while (lastFold.getHasChildFolds()) {
							lastFold = lastFold.getLastChild();
							if (lastFold.containsLine(lastLine)) {
								if (lastFold.isCollapsed()) {
									lastLine = lastFold.getStartLine();
									break;
								}
							}
							else { // Higher up
								break;
							}
						}
					}
				}
			}
		}

		return lastLine;

	}


	public int getVisibleLineAbove(int line) {

		if (line<=0 || line>=textArea.getLineCount()) {
			return -1;
		}

		do {
			line--;
		} while (line>=0 && isLineHidden(line));

		return line;

	}


	public int getVisibleLineBelow(int line) {

		int lineCount = textArea.getLineCount();
		if (line<0 || line>=lineCount-1) {
			return -1;
		}

		do {
			line++;
		} while (line<lineCount && isLineHidden(line));

		return line==lineCount ? -1 : line;

	}


//	private static int binaryFindFoldContainingLine(int line) {
//
//List allFolds;
//
//		int low = 0;
//		int high = allFolds.size() - 1;
//
//		while (low <= high) {
//			int mid = (low + high) >> 1;
//			Fold midVal = (Fold)allFolds.get(mid);
//			if (midVal.containsLine(line)) {
//				return mid;
//			}
//			if (line<=midVal.getStartLine()) {
//				high = mid - 1;
//			}
//			else { // line > midVal.getEndLine()
//				low = mid + 1;
//			}
//		}
//
//		return -(low + 1); // key not found
//
//	}


    /**
	 * Returns whether code folding is enabled.  Note that only certain
	 * languages support code folding; those that do not will ignore this
	 * property.
	 *
	 * @return Whether code folding is enabled.
	 * @see #setCodeFoldingEnabled(boolean)
	 */
	public boolean isCodeFoldingEnabled() {
		return codeFoldingEnabled;
	}


	/**
	 * Returns <code>true</code> if and only if code folding is enabled for
	 * this text area, AND folding is supported for the language it is editing.
	 * Whether or not folding is supported for a language depends on whether
	 * a fold parser is registered for that language with the
	 * <code>FoldParserManager</code>.
	 *
	 * @return Whether folding is supported and enabled for this text area.
	 * @see FoldParserManager
	 */
	public boolean isCodeFoldingSupportedAndEnabled() {
		return codeFoldingEnabled && parser!=null;
	}


	/**
	 * Returns whether the specified line contains the start of a fold region.
	 *
	 * @param line The line.
	 * @return Whether the line contains the start of a fold region.
	 * @see #getFoldForLine(int)
	 */
	public boolean isFoldStartLine(int line) {
		return getFoldForLine(line)!=null;
	}


	/**
	 * Returns whether a line is hidden in a collapsed fold.
	 *
	 * @param line The line to check.
	 * @return Whether the line is hidden in a collapsed fold.
	 */
	public boolean isLineHidden(int line) {
		for (int i=0; i<folds.size(); i++) {
			Fold fold = (Fold)folds.get(i);
			if (fold.containsLine(line)) {
				if (fold.isCollapsed()) {
					return true;
				}
				else {
					return isLineHiddenImpl(fold, line);
				}
			}
		}
		return false;
	}


	private boolean isLineHiddenImpl(Fold parent, int line) {
		for (int i=0; i<parent.getChildCount(); i++) {
			Fold child = parent.getChild(i);
			if (child.containsLine(line)) {
				if (child.isCollapsed()) {
					return true;
				}
				else {
					return isLineHiddenImpl(child, line);
				}
			}
		}
		return false;
	}


	/**
	 * Checks whether a single fold was there in the "old" set of folds.  If
	 * it was, its collapsed state is preserved.
	 *
	 * @param newFold The "new" fold to check for.
	 * @param oldFolds The previous folds before an edit occurred.
	 */
	private void keepFoldState(Fold newFold, List oldFolds) {
		int previousLoc = Collections.binarySearch(oldFolds, newFold);
		//System.out.println(newFold + " => " + previousLoc);
		if (previousLoc>=0) {
			Fold prevFold = (Fold)oldFolds.get(previousLoc);
			newFold.setCollapsed(prevFold.isCollapsed());
		}
		else {
			//previousLoc = -(insertion point) - 1;
			int insertionPoint = -(previousLoc + 1);
			if (insertionPoint>0) {
				Fold possibleParentFold = (Fold)oldFolds.get(insertionPoint-1);
				if (possibleParentFold.containsOffset(
						newFold.getStartOffset())) {
					List children = possibleParentFold.getChildren();
					if (children!=null) {
						keepFoldState(newFold, children);
					}
				}
			}
		}
	}


	/**
	 * Called when new folds come in from the fold parser.  Checks whether any
	 * folds from the "old" fold list are still in the "new" list; if so, their
	 * collapsed state is preserved.
	 *
	 * @param newFolds The "new" folds after an edit occurred.  This cannot be
	 *        <code>null</code>.
	 * @param oldFolds The previous folds before the edit occurred.
	 */
	private void keepFoldStates(List newFolds, List oldFolds) {
		for (int i=0; i<newFolds.size(); i++) {
			Fold newFold = (Fold)newFolds.get(i);
			keepFoldState(newFold, folds);
			List newChildFolds = newFold.getChildren();
			if (newChildFolds!=null) {
				keepFoldStates(newChildFolds, oldFolds);
			}
		}
	}


	/**
	 * Removes a property change listener from this fold manager.
	 *
	 * @param l The listener to remove.
	 * @see #addPropertyChangeListener(PropertyChangeListener)
	 */
	public void removePropertyChangeListener(PropertyChangeListener l) {
		support.removePropertyChangeListener(l);
	}


	/**
	 * Forces an immediate reparsing for folds, if folding is enabled.  This
	 * usually does not need to be called by the programmer, since fold
	 * parsing is done automatically by RSTA.
	 */
	public void reparse() {

		if (codeFoldingEnabled && parser!=null) {

			// Re-calculate folds.  Keep the fold state of folds that are
			// still around.
			List newFolds = parser.getFolds(textArea);
			if (newFolds==null) {
				newFolds = Collections.EMPTY_LIST;
			}
			else {
				keepFoldStates(newFolds, folds);
			}
			folds = newFolds;

			// Let folks (gutter, etc.) know that folds have been updated.
			support.firePropertyChange(PROPERTY_FOLDS_UPDATED, null, folds);
			textArea.repaint();

		}
		else {
			folds.clear();
		}

	}


	/**
	 * Sets whether code folding is enabled.  Note that only certain
	 * languages will support code folding out of the box.  Those languages
	 * which do not support folding will ignore this property.
	 *
	 * @param enabled Whether code folding should be enabled.
	 * @see #isCodeFoldingEnabled()
	 */
	public void setCodeFoldingEnabled(boolean enabled) {
		if (enabled!=codeFoldingEnabled) {
			codeFoldingEnabled = enabled;
			if (tempParser!=null) {
				textArea.removeParser(tempParser);
			}
			if (enabled) {
				tempParser = new AbstractParser() {
					public ParseResult parse(RSyntaxDocument doc, String style) {
						reparse();
						return new DefaultParseResult(this);
					}
				};
				textArea.addParser(tempParser);
				support.firePropertyChange(PROPERTY_FOLDS_UPDATED, null, null);
				//reparse();
			}
			else {
				folds = Collections.EMPTY_LIST;
				textArea.repaint();
				support.firePropertyChange(PROPERTY_FOLDS_UPDATED, null, null);
			}
		}
	}
private Parser tempParser;


	/**
	 * Sets the folds for this fold manager.
	 *
	 * @param folds The new folds.  This should not be <code>null</code>.
	 */
	public void setFolds(List folds) {
		this.folds = folds;
	}


	/**
	 * Updates the fold parser to be the one appropriate for the language
	 * currently being highlighted.
	 */
	private void updateFoldParser() {
		parser = FoldParserManager.get().getFoldParser(
											textArea.getSyntaxEditingStyle());
	}


	/**
	 * Listens for events in the text editor.
	 */
	private class Listener implements DocumentListener, PropertyChangeListener {

		public void changedUpdate(DocumentEvent e) {
		}

		public void insertUpdate(DocumentEvent e) {
			// Adding text containing a newline to the visible line of a folded
			// Fold causes that Fold to unfold.  Check only start offset of
			// insertion since that's the line that was "modified".
			int startOffs = e.getOffset();
			int endOffs = startOffs + e.getLength();
			Document doc = e.getDocument();
			Element root = doc.getDefaultRootElement();
			int startLine = root.getElementIndex(startOffs);
			int endLine = root.getElementIndex(endOffs);
			if (startLine!=endLine) { // Inserted text covering > 1 line...
				Fold fold = getFoldForLine(startLine);
				if (fold!=null && fold.isCollapsed()) {
					fold.toggleCollapsedState();
				}
			}
		}

		public void propertyChange(PropertyChangeEvent e) {
			// Syntax style changed in editor.
			updateFoldParser();
			reparse(); // Even if no fold parser change, highlighting did
		}
		public void removeUpdate(DocumentEvent e) {
			// Removing text from the visible line of a folded Fold causes that
			// Fold to unfold.  We only need to check the removal offset since
			// that's the new caret position.
			int offs = e.getOffset();
			try {
				int lastLineModified = textArea.getLineOfOffset(offs);
				//System.out.println(">>> " + lastLineModified);
				Fold fold = getFoldForLine(lastLineModified);
				//System.out.println("&&& " + fold);
				if (fold!=null && fold.isCollapsed()) {
					fold.toggleCollapsedState();
				}
			} catch (BadLocationException ble) {
				ble.printStackTrace(); // Never happens
			}
		}

	}


}