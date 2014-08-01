/*
 * 10/08/2011
 *
 * FoldType.java - Types of folds found in many programming languages.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.folding;


/**
 * Constants representing the "type" of a folded region.  Implementations of
 * {@link FoldParser} can also define their own folded region types, provided
 * they give them values of at least {@link #FOLD_TYPE_USER_DEFINED_MIN}.  This
 * allows you to identify and auto-fold specific regions of source code when
 * opening files; for example, a Java editor could identify all import
 * statements in a file as a foldable region, and give it a user-defined value
 * for fold type.  Then, the UI could provide a means for the user to specify
 * that they always want the import region folded when opening a new file.<p>
 * 
 * The majority of the time, however, code editors won't need to be that fancy,
 * and can simply use the standard <code>CODE</code> and <code>COMMENT</code>
 * fold types.
 * 
 * @author Robert Futrell
 * @version 1.0
 * @see Fold
 */
public interface FoldType {

	/**
	 * Denotes a <code>Fold</code> as being a region of code.
	 */
	public static final int CODE							= 0;

	/**
	 * Denotes a <code>Fold</code> as being a multi-line comment.
	 */
	public static final int COMMENT							= 1;

	/**
	 * Denotes a <code>Fold</code> as being a section of import statements
	 * (Java), include statements (C), etc.
	 */
	public static final int IMPORTS							= 2;

	/**
	 * Users building advanced editors such as IDE's, that want to allow their
	 * users to auto-expand/collapse foldable regions of a specific type other
	 * than comments, should define their custom fold types using values
	 * <code>FOLD_TYPE_USER_DEFINED_MIN + <em>n</em></code>.  That way, if
	 * new default fold types are added to this interface in the future, your
	 * code won't suddenly break when upgrading to a new version of RSTA.
	 */
	public static final int FOLD_TYPE_USER_DEFINED_MIN		= 1000;


}