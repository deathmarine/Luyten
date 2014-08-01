/*
 * 02/17/2012
 *
 * SearchContext.java - Container for options of a search/replace operation.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;


/**
 * Contains information about a find/replace operation.  Applications can
 * keep an instance of this class around and use it to maintain the user's
 * selection for options such as "match case," "regular expression," etc.,
 * between search operations.  They can then pass the instance as a parameter
 * to the public {@link SearchEngine} methods to do the actual searching.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see SearchEngine
 */
public class SearchContext {

	private String searchFor;

	private String replaceWith;

	private boolean forward;

	private boolean matchCase;

	private boolean wholeWord;

	private boolean regex;

	private boolean selectionOnly;


	/**
	 * Creates a new search context.  Specifies a forward search,
	 * case-insensitive, not whole-word, not a regular expression.
	 */
	public SearchContext() {
		this(null);
	}


	/**
	 * Creates a new search context.  Specifies a forward search,
	 * case-insensitive, not whole-word, not a regular expression.
	 *
	 * @param searchFor The text to search for.
	 */
	public SearchContext(String searchFor) {
		this.searchFor = searchFor;
		forward = true;
	}


	/**
	 * Returns whether case should be honored while searching.
	 *
	 * @return Whether case should be honored.
	 * @see #setMatchCase(boolean)
	 */
	public boolean getMatchCase() {
		return matchCase;
	}


	/**
	 * Returns the text to replace with, if doing a replace operation.
	 *
	 * @return The text to replace with.
	 * @see #setReplaceWith(String)
	 * @see #getSearchFor()
	 */
	public String getReplaceWith() {
		return replaceWith;
	}


	/**
	 * Returns the text to search for.
	 *
	 * @return The text to search for.
	 * @see #setSearchFor(String)
	 * @see #getReplaceWith()
	 */
	public String getSearchFor() {
		return searchFor;
	}


	/**
	 * Returns whether the search should be forward through the text (vs.
	 * backwards).
	 *
	 * @return Whether we should search forwards.
	 * @see #setSearchForward(boolean)
	 */
	public boolean getSearchForward() {
		return forward;
	}


	/**
	 * Returns whether the search should only be done in the selected text.
	 * This flag is currently not supported.
	 *
	 * @return Whether only the selected text should be searched.
	 * @see #setSearchSelectionOnly(boolean)
	 */
	public boolean getSearchSelectionOnly() {
		return selectionOnly;
	}


	/**
	 * Returns whether only "whole word" matches should be returned.  A match
	 * is considered to be "whole word" if the character on either side of the
	 * matched text is a non-word character, or if there is no character on
	 * one side of the word, such as when it's at the beginning or end of a
	 * line.
	 *
	 * @return Whether only "whole word" matches should be returned.
	 * @see #setWholeWord(boolean)
	 */
	public boolean getWholeWord() {
		return wholeWord;
	}


	/**
	 * Returns whether a regular expression search should be done.
	 *
	 * @return Whether a regular expression search should be done.
	 * @see #setRegularExpression(boolean)
	 */
	public boolean isRegularExpression() {
		return regex;
	}


	/**
	 * Sets whether case should be honored while searching.
	 *
	 * @param matchCase Whether case should be honored.
	 * @see #getMatchCase()
	 */
	public void setMatchCase(boolean matchCase) {
		this.matchCase = matchCase;
	}


	/**
	 * Sets whether a regular expression search should be done.
	 *
	 * @param regex Whether a regular expression search should be done.
	 * @see #isRegularExpression()
	 */
	public void setRegularExpression(boolean regex) {
		this.regex = regex;
	}


	/**
	 * Sets the text to replace with, if doing a replace operation.
	 *
	 * @param replaceWith The text to replace with.
	 * @see #getReplaceWith()
	 * @see #setSearchFor(String)
	 */
	public void setReplaceWith(String replaceWith) {
		this.replaceWith = replaceWith;
	}


	/**
	 * Sets the text to search for.
	 *
	 * @param searchFor The text to search for.
	 * @see #getSearchFor()
	 * @see #setReplaceWith(String)
	 */
	public void setSearchFor(String searchFor) {
		this.searchFor = searchFor;
	}


	/**
	 * Sets whether the search should be forward through the text (vs.
	 * backwards).
	 *
	 * @param forward Whether we should search forwards.
	 * @see #getSearchForward()
	 */
	public void setSearchForward(boolean forward) {
		this.forward = forward;
	}


	/**
	 * Sets whether only the selected text should be searched.
	 * This flag is currently not supported.
	 *
	 * @param selectionOnly Whether only selected text should be searched.
	 * @see #getSearchSelectionOnly()
	 */
	public void setSearchSelectionOnly(boolean selectionOnly) {
		this.selectionOnly = selectionOnly;
	}


	/**
	 * Sets whether only "whole word" matches should be returned.  A match
	 * is considered to be "whole word" if the character on either side of the
	 * matched text is a non-word character, or if there is no character on
	 * one side of the word, such as when it's at the beginning or end of a
	 * line.
	 *
	 * @param wholeWord Whether only "whole word" matches should be returned.
	 * @see #getWholeWord()
	 */
	public void setWholeWord(boolean wholeWord) {
		this.wholeWord = wholeWord;
	}


}