/*
 * 02/19/2006
 *
 * SearchEngine.java - Handles find/replace operations in an RTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.folding.FoldManager;


/**
 * A singleton class that can perform advanced find/replace operations
 * in an {@link RTextArea}.  Simply create a {@link SearchContext} and call
 * one of the following methods:
 * 
 * <ul>
 *    <li>{@link #find(JTextArea, SearchContext)}
 *    <li>{@link #replace(RTextArea, SearchContext)}
 *    <li>{@link #replaceAll(RTextArea, SearchContext)}
 * </ul>
 *
 * @author Robert Futrell
 * @version 1.0
 * @see SearchContext
 */
public class SearchEngine {


	/**
	 * Private constructor to prevent instantiation.
	 */
	private SearchEngine() {
	}


	/**
	 * Finds the next instance of the string/regular expression specified
	 * from the caret position.  If a match is found, it is selected in this
	 * text area.
	 *
	 * @param textArea The text area in which to search.
	 * @param context What to search for and all search options.
	 * @return Whether a match was found (and thus selected).
	 * @throws PatternSyntaxException If this is a regular expression search
	 *         but the search text is an invalid regular expression.
	 * @see #replace(RTextArea, SearchContext)
	 * @see #replaceAll(RTextArea, SearchContext)
	 */
	public static boolean find(JTextArea textArea, SearchContext context) {

		String text = context.getSearchFor();
		if (text==null || text.length()==0) {
			return false;
		}

		// Be smart about what position we're "starting" at.  We don't want
		// to find a match in the currently selected text (if any), so we
		// start searching AFTER the selection if searching forward, and
		// BEFORE the selection if searching backward.
		Caret c = textArea.getCaret();
		boolean forward = context.getSearchForward();
		int start = forward ? Math.max(c.getDot(), c.getMark()) :
						Math.min(c.getDot(), c.getMark());

		String findIn = getFindInText(textArea, start, forward);
		if (findIn==null || findIn.length()==0) return false;

		// Find the next location of the text we're searching for.
		if (!context.isRegularExpression()) {
			int pos = getNextMatchPos(text, findIn, forward,
								context.getMatchCase(), context.getWholeWord());
			findIn = null; // May help garbage collecting.
			if (pos!=-1) {
				// Without this, if JTextArea isn't in focus, selection
				// won't appear selected.
				c.setSelectionVisible(true);
				pos = forward ? start+pos : pos;
				selectAndPossiblyCenter(textArea, pos, pos+text.length());
				return true;
			}
		}
		else {
			// Regex matches can have varying widths.  The returned point's
			// x- and y-values represent the start and end indices of the
			// match in findIn.
			Point regExPos = getNextMatchPosRegEx(text, findIn, forward,
								context.getMatchCase(), context.getWholeWord());
			findIn = null; // May help garbage collecting.
			if (regExPos!=null) {
				// Without this, if JTextArea isn't in focus, selection
				// won't appear selected.
				c.setSelectionVisible(true);
				if (forward) {
					regExPos.translate(start, start);
				}
				selectAndPossiblyCenter(textArea, regExPos.x, regExPos.y);
				return true;
			}
		}

		// No match.
		return false;

	}


	/**
	 * Returns a <code>CharSequence</code> for a text area that doesn't make a
	 * copy of its contents for iteration.  This conserves memory but is likely
	 * just a tad slower.
	 *
	 * @param textArea The text area whose document is the basis for the
	 *        <code>CharSequence</code>.
	 * @param start The starting offset of the sequence (or ending offset if
	 *        <code>forward</code> is <code>false</code>).
	 * @param forward Whether we're searching forward or backward.
	 * @return The character sequence.
	 */
	private static CharSequence getFindInCharSequence(RTextArea textArea,
			int start, boolean forward) {
		RDocument doc = (RDocument)textArea.getDocument();
		int csStart = 0;
		int csEnd = 0;
		if (forward) {
			csStart = start;
			csEnd = doc.getLength();
		}
		else {
			csStart = 0;
			csEnd = start;
		}
		return new RDocumentCharSequence(doc, csStart, csEnd);
	}


	/**
	 * Returns the text in which to search, as a string.  This is used
	 * internally to grab the smallest buffer possible in which to search.
	 */
	private static String getFindInText(JTextArea textArea, int start,
									boolean forward) {

		// Be smart about the text we grab to search in.  We grab more than
		// a single line because our searches can return multi-line results.
		// We copy only the chars that will be searched through.
		String findIn = null;
		try {
			if (forward) {
				findIn = textArea.getText(start,
							textArea.getDocument().getLength()-start);
			}
			else { // backward
				findIn = textArea.getText(0, start);
			}
		} catch (BadLocationException ble) {
			// Never happens; findIn will be null anyway.
			ble.printStackTrace();
		}

		return findIn;

	}


	/**
	 * This method is called internally by
	 * <code>getNextMatchPosRegExImpl</code> and is used to get the locations
	 * of all regular-expression matches, and possibly their replacement
	 * strings.<p>
	 *
	 * Returns either:
	 * <ul>
	 *   <li>A list of points representing the starting and ending positions
	 *       of all matches returned by the specified matcher, or
	 *   <li>A list of <code>RegExReplaceInfo</code>s describing the matches
	 *       found by the matcher and the replacement strings for each.
	 * </ul>
	 *
	 * If <code>replacement</code> is <code>null</code>, this method call is
	 * assumed to be part of a "find" operation and points are returned.  If
	 * if is non-<code>null</code>, it is assumed to be part of a "replace"
	 * operation and the <code>RegExReplaceInfo</code>s are returned.<p>
	 *
	 * @param m The matcher.
	 * @param replaceStr The string to replace matches with.  This is a
	 *        "template" string and can contain captured group references in
	 *        the form "<code>${digit}</code>".
	 * @return A list of result objects.
	 * @throws IndexOutOfBoundsException If <code>replaceStr</code> references
	 *         an invalid group (less than zero or greater than the number of
	 *         groups matched).
	 */
	private static List getMatches(Matcher m, String replaceStr) {
		ArrayList matches = new ArrayList();
		while (m.find()) {
			Point loc = new Point(m.start(), m.end());
			if (replaceStr==null) { // Find, not replace.
				matches.add(loc);
			}
			else { // Replace.
				matches.add(new RegExReplaceInfo(m.group(0), loc.x, loc.y,
								getReplacementText(m, replaceStr)));
			}
		}
		return matches;
	}


	/**
	 * Searches <code>searchIn</code> for an occurrence of
	 * <code>searchFor</code> either forwards or backwards, matching
	 * case or not.<p>
	 *
	 * Most clients will have no need to call this method directly.
	 *
	 * @param searchFor The string to look for.
	 * @param searchIn The string to search in.
	 * @param forward Whether to search forward or backward in
	 *        <code>searchIn</code>.
	 * @param matchCase If <code>true</code>, do a case-sensitive search for
	 *        <code>searchFor</code>.
	 * @param wholeWord If <code>true</code>, <code>searchFor</code>
	 *        occurrences embedded in longer words in <code>searchIn</code>
	 *        don't count as matches.
	 * @return The starting position of a match, or <code>-1</code> if no
	 *         match was found.
	 */
	public static final int getNextMatchPos(String searchFor, String searchIn,
								boolean forward, boolean matchCase,
								boolean wholeWord) {

		// Make our variables lower case if we're ignoring case.
		if (!matchCase) {
			return getNextMatchPosImpl(searchFor.toLowerCase(),
								searchIn.toLowerCase(), forward,
								matchCase, wholeWord);
		}

		return getNextMatchPosImpl(searchFor, searchIn, forward,
								matchCase, wholeWord);

	}


	/**
	 * Actually does the work of matching; assumes searchFor and searchIn
	 * are already upper/lower-cased appropriately.<br>
	 * The reason this method is here is to attempt to speed up
	 * <code>FindInFilesDialog</code>; since it repeatedly calls
	 * this method instead of <code>getNextMatchPos</code>, it gets better
	 * performance as it no longer has to allocate a lower-cased string for
	 * every call.
	 *
	 * @param searchFor The string to search for.
	 * @param searchIn The string to search in.
	 * @param goForward Whether the search is forward or backward.
	 * @param matchCase Whether the search is case-sensitive.
	 * @param wholeWord Whether only whole words should be matched.
	 * @return The location of the next match, or <code>-1</code> if no
	 *         match was found.
	 */
	private static final int getNextMatchPosImpl(String searchFor,
								String searchIn, boolean goForward,
								boolean matchCase, boolean wholeWord) {

		if (wholeWord) {
			int len = searchFor.length();
			int temp = goForward ? 0 : searchIn.length();
			int tempChange = goForward ? 1 : -1;
			while (true) {
				if (goForward)
					temp = searchIn.indexOf(searchFor, temp);
				else
					temp = searchIn.lastIndexOf(searchFor, temp);
				if (temp!=-1) {
					if (isWholeWord(searchIn, temp, len)) {
						return temp;
					}
					else {
						temp += tempChange;
						continue;
					}
				}
				return temp; // Always -1.
			}
		}
		else {
			return goForward ? searchIn.indexOf(searchFor) :
							searchIn.lastIndexOf(searchFor);
		}

	}


	/**
	 * Searches <code>searchIn</code> for an occurrence of <code>regEx</code>
	 * either forwards or backwards, matching case or not.
	 *
	 * @param regEx The regular expression to look for.
	 * @param searchIn The string to search in.
	 * @param goForward Whether to search forward.  If <code>false</code>,
	 *        search backward.
	 * @param matchCase Whether or not to do a case-sensitive search for
	 *        <code>regEx</code>.
	 * @param wholeWord If <code>true</code>, <code>regEx</code>
	 *        occurrences embedded in longer words in <code>searchIn</code>
	 *        don't count as matches.
	 * @return A <code>Point</code> representing the starting and ending
	 *         position of the match, or <code>null</code> if no match was
	 *         found.
	 * @throws PatternSyntaxException If <code>regEx</code> is an invalid
	 *         regular expression.
	 * @see #getNextMatchPos
	 */
	private static Point getNextMatchPosRegEx(String regEx,
							CharSequence searchIn, boolean goForward,
							boolean matchCase, boolean wholeWord) {
		return (Point)getNextMatchPosRegExImpl(regEx, searchIn, goForward,
									matchCase, wholeWord, null);
	}


	/**
	 * Searches <code>searchIn</code> for an occurrence of <code>regEx</code>
	 * either forwards or backwards, matching case or not.
	 *
	 * @param regEx The regular expression to look for.
	 * @param searchIn The string to search in.
	 * @param goForward Whether to search forward.  If <code>false</code>,
	 *        search backward.
	 * @param matchCase Whether or not to do a case-sensitive search for
	 *        <code>regEx</code>.
	 * @param wholeWord If <code>true</code>, <code>regEx</code>
	 *        occurrences embedded in longer words in <code>searchIn</code>
	 *        don't count as matches.
	 * @param replaceStr The string that will replace the match found (if
	 *        a match is found).  The object returned will contain the
	 *        replacement string with matched groups substituted.  If this
	 *        value is <code>null</code>, it is assumed this call is part of a
	 *        "find" instead of a "replace" operation.
	 * @return If <code>replaceStr</code> is <code>null</code>, a
	 *         <code>Point</code> representing the starting and ending points
	 *         of the match.  If it is non-<code>null</code>, an object with
	 *         information about the match and the morphed string to replace
	 *         it with.  If no match is found, <code>null</code> is returned.
	 * @throws PatternSyntaxException If <code>regEx</code> is an invalid
	 *         regular expression.
	 * @throws IndexOutOfBoundsException If <code>replaceStr</code> references
	 *         an invalid group (less than zero or greater than the number of
	 *         groups matched).
	 * @see #getNextMatchPos
	 */
	private static Object getNextMatchPosRegExImpl(String regEx,
							CharSequence searchIn, boolean goForward,
							boolean matchCase, boolean wholeWord,
							String replaceStr) {

		if (wholeWord) {
			regEx = "\\b" + regEx + "\\b";
		}

		// Make a pattern that takes into account whether or not to match case.
		int flags = Pattern.MULTILINE; // '^' and '$' are done per line.
		flags |= matchCase ? 0 : (Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
		Pattern pattern = Pattern.compile(regEx, flags);

		// Make a Matcher to find the regEx instances.
		Matcher m = pattern.matcher(searchIn);

		// Search forwards
		if (goForward) {
			if (m.find()) {
				if (replaceStr==null) { // Find, not replace.
					return new Point(m.start(), m.end());
				}
				// Otherwise, replace
				return new RegExReplaceInfo(m.group(0),
						m.start(), m.end(),
						getReplacementText(m, replaceStr));
			}
		}

		// Search backwards
		else {
			List matches = getMatches(m, replaceStr);
			if (!matches.isEmpty()) {
				return matches.get(matches.size()-1);
			}
		}

		return null; // No match found

	}


	/**
	 * Returns information on how to implement a regular expression "replace"
	 * action in the specified text with the specified replacement string.
	 *
	 * @param searchIn The string to search in.
	 * @param context The search options.
	 * @return A <code>RegExReplaceInfo</code> object describing how to
	 *         implement the replace.
	 * @throws PatternSyntaxException If the search text is an invalid regular
	 *         expression.
	 * @throws IndexOutOfBoundsException If the replacement text references an
	 *         invalid group (less than zero or greater than the number of
	 *         groups matched).
	 * @see #getNextMatchPos
	 */
	private static RegExReplaceInfo getRegExReplaceInfo(CharSequence searchIn,
										SearchContext context) {
		// Can't pass null to getNextMatchPosRegExImpl or it'll think
		// you're doing a "find" operation instead of "replace, and return a
		// Point.
		String replacement = context.getReplaceWith();
		if (replacement==null) {
			replacement = "";
		}
		String regex = context.getSearchFor();
		boolean goForward = context.getSearchForward();
		boolean matchCase = context.getMatchCase();
		boolean wholeWord = context.getWholeWord();
		return (RegExReplaceInfo)getNextMatchPosRegExImpl(regex, searchIn,
						goForward, matchCase, wholeWord, replacement);
	}


	/**
	 * Called internally by <code>getMatches()</code>.  This method assumes
	 * that the specified matcher has just found a match, and that you want
	 * to get the string with which to replace that match.<p>
	 *
	 * Escapes simply insert the escaped character, except for <code>\n</code>
	 * and <code>\t</code>, which insert a newline and tab respectively.
	 * Substrings of the form <code>$\d+</code> are considered to be matched
	 * groups.  To include a literal dollar sign in your template, escape it
	 * (i.e. <code>\$</code>).<p>
	 *
	 * Most clients will have no need to call this method directly.
	 *
	 * @param m The matcher.
	 * @param template The template for the replacement string.  For example,
	 *        "<code>foo</code>" would yield the replacement string
	 *        "<code>foo</code>", while "<code>$1 is the greatest</code>"
	 *        would yield different values depending on the value of the first
	 *        captured group in the match.
	 * @return The string to replace the match with.
	 * @throws IndexOutOfBoundsException If <code>template</code> references
	 *         an invalid group (less than zero or greater than the number of
	 *         groups matched).
	 */
	public static String getReplacementText(Matcher m, CharSequence template) {

		// NOTE: This code was mostly ripped off from J2SE's Matcher
		// class.

		// Process substitution string to replace group references with groups
		int cursor = 0;
		StringBuffer result = new StringBuffer();

		while (cursor < template.length()) {
	
			char nextChar = template.charAt(cursor);
	
			if (nextChar == '\\') { // Escape character.
				nextChar = template.charAt(++cursor);
				switch (nextChar) { // Special cases.
					case 'n':
						nextChar = '\n';
						break;
					case 't':
						nextChar = '\t';
						break;
				}
				result.append(nextChar);
				cursor++;
			}
			else if (nextChar == '$') { // Group reference.

				cursor++; // Skip the '$'.

				// The first number is always a group
				int refNum = template.charAt(cursor) - '0';
				if ((refNum < 0)||(refNum > 9)) {
					// This should really be an IllegalArgumentException,
					// but we cheat to keep all "group" errors throwing
					// the same exception type.
					throw new IndexOutOfBoundsException(
								"No group " + template.charAt(cursor));
				}
				cursor++;

				// Capture the largest legal group string
				boolean done = false;
				while (!done) {
					if (cursor >= template.length()) {
						break;
					}
					int nextDigit = template.charAt(cursor) - '0';
					if ((nextDigit < 0)||(nextDigit > 9)) { // not a number
						break;
					}
					int newRefNum = (refNum * 10) + nextDigit;
					if (m.groupCount() < newRefNum) {
						done = true;
					}
					else {
						refNum = newRefNum;
						cursor++;
					}
				}

				// Append group
				if (m.group(refNum) != null)
					result.append(m.group(refNum));

			}

			else {
				result.append(nextChar);
				cursor++;
			}

		}

		return result.toString();

	}


	/**
	 * Returns whether the characters on either side of
	 * <code>substr(searchIn, startPos, startPos+searchStringLength)</code>
	 * are <em>not</em> letters or digits.
	 */
	private static final boolean isWholeWord(CharSequence searchIn,
											int offset, int len) {

		boolean wsBefore, wsAfter;

		try {
			wsBefore = !Character.isLetterOrDigit(searchIn.charAt(offset - 1));
		} catch (IndexOutOfBoundsException e) { wsBefore = true; }
		try {
			wsAfter  = !Character.isLetterOrDigit(searchIn.charAt(offset + len));
		} catch (IndexOutOfBoundsException e) { wsAfter = true; }

		return wsBefore && wsAfter;

	}


	/**
	 * Makes the caret's dot and mark the same location so that, for the
	 * next search in the specified direction, a match will be found even
	 * if it was within the original dot and mark's selection.
	 *
	 * @param textArea The text area.
	 * @param forward Whether the search will be forward through the
	 *        document (<code>false</code> means backward).
	 * @return The new dot and mark position.
	 */
	private static int makeMarkAndDotEqual(JTextArea textArea,
										boolean forward) {
		Caret c = textArea.getCaret();
		int val = forward ? Math.min(c.getDot(), c.getMark()) :
						Math.max(c.getDot(), c.getMark());
		c.setDot(val);
		return val;
	}


	/**
	 * Finds the next instance of the regular expression specified from
	 * the caret position.  If a match is found, it is replaced with
	 * the specified replacement string.
	 *
	 * @param textArea The text area in which to search.
	 * @param context What to search for and all search options.
	 * @return Whether a match was found (and thus replaced).
	 * @throws PatternSyntaxException If this is a regular expression search
	 *         but the search text is an invalid regular expression.
	 * @throws IndexOutOfBoundsException If this is a regular expression search
	 *         but the replacement text references an invalid group (less than
	 *         zero or greater than the number of groups matched).
	 * @see #replace(RTextArea, SearchContext)
	 * @see #find(JTextArea, SearchContext)
	 */
	private static boolean regexReplace(RTextArea textArea,
			SearchContext context) throws PatternSyntaxException {

		// Be smart about what position we're "starting" at.  For example,
		// if they are searching backwards and there is a selection such that
		// the dot is past the mark, and the selection is the text for which
		// you're searching, this search will find and return the current
		// selection.  So, in that case we start at the beginning of the
		// selection.
		Caret c = textArea.getCaret();
		boolean forward = context.getSearchForward();
		int start = makeMarkAndDotEqual(textArea, forward);

		CharSequence findIn = getFindInCharSequence(textArea, start, forward);
		if (findIn==null) return false;

		// Find the next location of the text we're searching for.
		RegExReplaceInfo info = getRegExReplaceInfo(findIn, context);

		findIn = null; // May help garbage collecting.

		// If a match was found, do the replace and return!
		if (info!=null) {

			// Without this, if JTextArea isn't in focus, selection won't
			// appear selected.
			c.setSelectionVisible(true);

			int matchStart = info.getStartIndex();
			int matchEnd = info.getEndIndex();
			if (forward) {
				matchStart += start;
				matchEnd += start;
			}
			selectAndPossiblyCenter(textArea, matchStart, matchEnd);
			textArea.replaceSelection(info.getReplacement());

			return true;

		}

		// No match.
		return false;

	}


	/**
	 * Finds the next instance of the text/regular expression specified from
	 * the caret position.  If a match is found, it is replaced with the
	 * specified replacement string.
	 *
	 * @param textArea The text area in which to search.
	 * @param context What to search for and all search options.
	 * @return Whether a match was found (and thus replaced).
	 * @throws PatternSyntaxException If this is a regular expression search
	 *         but the search text is an invalid regular expression.
	 * @throws IndexOutOfBoundsException If this is a regular expression search
	 *         but the replacement text references an invalid group (less than
	 *         zero or greater than the number of groups matched).
	 * @see #replaceAll(RTextArea, SearchContext)
	 * @see #find(JTextArea, SearchContext)
	 */
	public static boolean replace(RTextArea textArea, SearchContext context)
									throws PatternSyntaxException {

		String toFind = context.getSearchFor();
		if (toFind==null || toFind.length()==0) {
			return false;
		}

		textArea.beginAtomicEdit();
		try {

			// Regular expression replacements have their own method.
			if (context.isRegularExpression()) {
				return regexReplace(textArea, context);
			}

			// Plain text search.  If we find it, replace it!
			// First make the dot and mark equal (get rid of any selection), as
			// a common use-case is the user will use "Find" to select the text
			// to replace, then click "Replace" to replace the current
			// selection. Since our find() method searches from an endpoint of
			// the selection, we must remove the selection to work properly.
			makeMarkAndDotEqual(textArea, context.getSearchForward());
			if (find(textArea, context)) {
				textArea.replaceSelection(context.getReplaceWith());
				return true;
			}

		} finally {
			textArea.endAtomicEdit();
		}

		return false;

	}


	/**
	 * Replaces all instances of the text/regular expression specified in
	 * the specified document with the specified replacement.
	 *
	 * @param textArea The text area in which to search.
	 * @param context What to search for and all search options.
	 * @return The number of replacements done.
	 * @throws PatternSyntaxException If this is a regular expression search
	 *         but the replacement text is an invalid regular expression.
	 * @throws IndexOutOfBoundsException If this is a regular expression search
	 *         but the replacement text references an invalid group (less than
	 *         zero or greater than the number of groups matched).
	 * @see #replace(RTextArea, SearchContext)
	 * @see #find(JTextArea, SearchContext)
	 */
	public static int replaceAll(RTextArea textArea, SearchContext context)
									throws PatternSyntaxException {

		context.setSearchForward(true); // Replace all always searches forward
		String toFind = context.getSearchFor();
		if (toFind==null || toFind.length()==0) {
			return 0;
		}

		int count = 0;

		textArea.beginAtomicEdit();
		try {
			int oldOffs = textArea.getCaretPosition();
			textArea.setCaretPosition(0);
			while (SearchEngine.replace(textArea, context)) {
				count++;
			}
			if (count==0) { // If nothing was found, don't move the caret.
				textArea.setCaretPosition(oldOffs);
			}
		} finally {
			textArea.endAtomicEdit();
		}

		return count;

	}


	/**
	 * Selects a range of text in a text component.  If the new selection is
	 * outside of the previous viewable rectangle, then the view is centered
	 * around the new selection.
	 *
	 * @param textArea The text component whose selection is to be centered.
	 * @param start The start of the range to select.
	 * @param end The end of the range to select.
	 */
	private static void selectAndPossiblyCenter(JTextArea textArea, int start,
												int end) {

		boolean foldsExpanded = false;
		if (textArea instanceof RSyntaxTextArea) {
			RSyntaxTextArea rsta = (RSyntaxTextArea)textArea;
			FoldManager fm = rsta.getFoldManager();
			if (fm.isCodeFoldingSupportedAndEnabled()) {
				foldsExpanded = fm.ensureOffsetNotInClosedFold(start);
				foldsExpanded |= fm.ensureOffsetNotInClosedFold(end);
			}
		}

		textArea.setSelectionStart(start);
		textArea.setSelectionEnd(end);

		Rectangle r = null;
		try {
			r = textArea.modelToView(start);
			if (r==null) { // Not yet visible; i.e. JUnit tests
				return;
			}
			if (end!=start) {
				r = r.union(textArea.modelToView(end));
			}
		} catch (BadLocationException ble) { // Never happens
			ble.printStackTrace();
			textArea.setSelectionStart(start);
			textArea.setSelectionEnd(end);
			return;
		}

		Rectangle visible = textArea.getVisibleRect();

		// If the new selection is already in the view, don't scroll,
		// as that is visually jarring.
		if (!foldsExpanded && visible.contains(r)) {
			textArea.setSelectionStart(start);
			textArea.setSelectionEnd(end);
			return;
		}

		visible.x = r.x - (visible.width - r.width) / 2;
		visible.y = r.y - (visible.height - r.height) / 2;

		Rectangle bounds = textArea.getBounds();
		Insets i = textArea.getInsets();
		bounds.x = i.left;
		bounds.y = i.top;
		bounds.width -= i.left + i.right;
		bounds.height -= i.top + i.bottom;

		if (visible.x < bounds.x) {
			visible.x = bounds.x;
		}

		if (visible.x + visible.width > bounds.x + bounds.width) {
			visible.x = bounds.x + bounds.width - visible.width;
		}

		if (visible.y < bounds.y) {
			visible.y = bounds.y;
		}

		if (visible.y + visible.height > bounds.y + bounds.height) {
			visible.y = bounds.y + bounds.height - visible.height;
		}

		textArea.scrollRectToVisible(visible);

	}


}