/*
 * 10/23/2011
 *
 * FoldCollapser.java - Goes through an RSTA instance and collapses folds of
 * specific types, such as comments.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;

import java.util.ArrayList;
import java.util.List;


/**
 * Collapses folds based on their type.  You can create an instance of this
 * class to collapse all comment blocks when opening a new file, for example.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class FoldCollapser {

	private List typesToCollapse;


	/**
	 * Creates an instance that collapses all comment blocks.
	 */
	public FoldCollapser() {
		this(FoldType.COMMENT);
	}


	/**
	 * Creates an instance that collapses all blocks of the specified
	 * type.
	 *
	 * @param typeToCollapse The type to collapse.
	 * @see FoldType
	 */
	public FoldCollapser(int typeToCollapse) {
		typesToCollapse = new ArrayList();
		addTypeToCollapse(typeToCollapse);
	}


	/**
	 * Adds a type of fold to collapse.
	 *
	 * @param typeToCollapse The type of fold to collapse.
	 */
	public void addTypeToCollapse(int typeToCollapse) {
		typesToCollapse.add(new Integer(typeToCollapse));
	}


	/**
	 * Collapses any relevant folds known by the fold manager.
	 *
	 * @param fm The fold manager.
	 */
	public void collapseFolds(FoldManager fm) {
		for (int i=0; i<fm.getFoldCount(); i++) {
			Fold fold = fm.getFold(i);
			collapseImpl(fold);
		}
	}


	/**
	 * Collapses the specified fold, and any of its child folds, as
	 * appropriate.
	 *
	 * @param fold The fold to examine.
	 * @see #getShouldCollapse(Fold)
	 */
	protected void collapseImpl(Fold fold) {
		if (getShouldCollapse(fold)) {
			fold.setCollapsed(true);
		}
		for (int i=0; i<fold.getChildCount(); i++) {
			collapseImpl(fold.getChild(i));
		}
	}


	/**
	 * Returns whether a specific fold should be collapsed.
	 *
	 * @param fold The fold to examine.
	 * @return Whether the fold should be collapsed.
	 */
	public boolean getShouldCollapse(Fold fold) {
		int type = fold.getFoldType();
		for (int i=0; i<typesToCollapse.size(); i++) {
			if (type==((Integer)typesToCollapse.get(i)).intValue()) {
				return true;
			}
		}
		return false;
	}


}