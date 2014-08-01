/*
 * 08/06/2004
 *
 * TokenOrientedView.java - An interface for the syntax-highlighting token-
 * oriented views for token-oriented methods.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;


/**
 * An interface for the syntax-highlighting token oriented views for
 * token-oriented methods.  This way callers won't need to know what specific
 * class a view is an instance of to access its tokens.<p>
 *
 * Currently, this interface is only useful for obtaining token lists for
 * "physical lines" (i.e., a word-wrapped view's logical lines may be
 * represented as several physical lines, thus getting the "physical line" above
 * a given position may prove complicated).
 *
 * @author Robert Futrell
 * @version 0.1
 */
public interface TokenOrientedView {


	/**
	 * Returns a token list for the <i>physical</i> line above the physical
	 * line containing the specified offset into the document.  Note that for
	 * a plain (non-wrapped) view, this is simply the token list for the
	 * logical line above the line containing <code>offset</code>, since lines
	 * are not wrapped.  For a wrapped view, this may or may not be tokens from
	 * the same line.
	 *
	 * @param offset The offset in question.
	 * @return A token list for the physical (and in this view, logical) line
	 *         before this one.  If no physical line is above the one
	 *         containing <code>offset</code>, <code>null</code> is returned.
	 */
	public Token getTokenListForPhysicalLineAbove(int offset);


	/**
	 * Returns a token list for the <i>physical</i> line below the physical
	 * line containing the specified offset into the document.  Note that for
	 * a plain (non-wrapped) view, this is simply the token list for the
	 * logical line below the line containing <code>offset</code>, since lines
	 * are not wrapped.  For a wrapped view, this may or may not be tokens from
	 * the same line.
	 *
	 * @param offset The offset in question.
	 * @return A token list for the physical (and in this view, logical) line
	 *         after this one.  If no physical line is after the one
	 *         containing <code>offset</code>, <code>null</code> is returned.
	 */
	public Token getTokenListForPhysicalLineBelow(int offset);


}