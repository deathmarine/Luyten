/*
 * 03/07/2004
 *
 * WindowsBatchTokenMaker.java - Scanner for Windows batch files.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.modes;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * A token maker that turns text into a linked list of
 * <code>Token</code>s for syntax highlighting Microsoft
 * Windows batch files.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class WindowsBatchTokenMaker extends AbstractTokenMaker {

	protected final String operators = "@:*<>=?";

	private int currentTokenStart;
	private int currentTokenType;

	private boolean bracketVariable;			// Whether a variable is of the format %{...}


	/**
	 * Constructor.
	 */
	public WindowsBatchTokenMaker() {
		super();	// Initializes tokensToHighlight.
	}


	/**
	 * Checks the token to give it the exact ID it deserves before
	 * being passed up to the super method.
	 *
	 * @param segment <code>Segment</code> to get text from.
	 * @param start Start offset in <code>segment</code> of token.
	 * @param end End offset in <code>segment</code> of token.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which the token occurs.
	 */
	public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {

		switch (tokenType) {
			// Since reserved words, functions, and data types are all passed
			// into here as "identifiers," we have to see what the token
			// really is...
			case Token.IDENTIFIER:
				int value = wordsToHighlight.get(segment, start,end);
				if (value!=-1)
					tokenType = value;
				break;
		}

		super.addToken(segment, start, end, tokenType, startOffset);

	}


	/**
	 * Returns the text to place at the beginning and end of a
	 * line to "comment" it in a this programming language.
	 *
	 * @return The start and end strings to add to a line to "comment"
	 *         it out.
	 */
	public String[] getLineCommentStartAndEnd() {
		return new String[] { "rem ", null };
	}


	/**
	 * Returns whether tokens of the specified type should have "mark
	 * occurrences" enabled for the current programming language.
	 *
	 * @param type The token type.
	 * @return Whether tokens of this type should have "mark occurrences"
	 *         enabled.
	 */
	public boolean getMarkOccurrencesOfTokenType(int type) {
		return type==Token.IDENTIFIER || type==Token.VARIABLE;
	}


	/**
	 * Returns the words to highlight for Windows batch files.
	 *
	 * @return A <code>TokenMap</code> containing the words to highlight for
	 *         Windows batch files.
	 * @see org.fife.ui.rsyntaxtextarea.AbstractTokenMaker#getWordsToHighlight
	 */
	public TokenMap getWordsToHighlight() {

		TokenMap tokenMap = new TokenMap(true); // Ignore case.

		int reservedWord = Token.RESERVED_WORD;
		tokenMap.put("call",			reservedWord);
		tokenMap.put("choice",		reservedWord);
		tokenMap.put("cls",			reservedWord);
		tokenMap.put("echo",			reservedWord);
		tokenMap.put("exit",			reservedWord);
		tokenMap.put("goto",			reservedWord);
		tokenMap.put("if",			reservedWord);
		tokenMap.put("pause",		reservedWord);
		tokenMap.put("shift",		reservedWord);
		tokenMap.put("start",		reservedWord);

		tokenMap.put("ansi.sys",		reservedWord);
		tokenMap.put("append",		reservedWord);
		tokenMap.put("arp",			reservedWord);
		tokenMap.put("assign",		reservedWord);
		tokenMap.put("assoc",		reservedWord);
		tokenMap.put("at",			reservedWord);
		tokenMap.put("attrib",		reservedWord);
		tokenMap.put("break",		reservedWord);
		tokenMap.put("cacls",		reservedWord);
		tokenMap.put("call",			reservedWord);
		tokenMap.put("cd",			reservedWord);
		tokenMap.put("chcp",			reservedWord);
		tokenMap.put("chdir",		reservedWord);
		tokenMap.put("chkdsk",		reservedWord);
		tokenMap.put("chknfts",		reservedWord);
		tokenMap.put("choice",		reservedWord);
		tokenMap.put("cls",			reservedWord);
		tokenMap.put("cmd",			reservedWord);
		tokenMap.put("color",		reservedWord);
		tokenMap.put("comp",			reservedWord);
		tokenMap.put("compact",		reservedWord);
		tokenMap.put("control",		reservedWord);
		tokenMap.put("convert",		reservedWord);
		tokenMap.put("copy",			reservedWord);
		tokenMap.put("ctty",			reservedWord);
		tokenMap.put("date",			reservedWord);
		tokenMap.put("debug",		reservedWord);
		tokenMap.put("defrag",		reservedWord);
		tokenMap.put("del",			reservedWord);
		tokenMap.put("deltree",		reservedWord);
		tokenMap.put("dir",			reservedWord);
		tokenMap.put("diskcomp",		reservedWord);
		tokenMap.put("diskcopy",		reservedWord);
		tokenMap.put("do",				reservedWord);
		tokenMap.put("doskey",		reservedWord);
		tokenMap.put("dosshell",		reservedWord);
		tokenMap.put("drivparm",		reservedWord);
		tokenMap.put("echo",			reservedWord);
		tokenMap.put("edit",			reservedWord);
		tokenMap.put("edlin",		reservedWord);
		tokenMap.put("emm386",		reservedWord);
		tokenMap.put("erase",		reservedWord);
		tokenMap.put("exist",		reservedWord);
		tokenMap.put("exit",			reservedWord);
		tokenMap.put("expand",		reservedWord);
		tokenMap.put("extract",		reservedWord);
		tokenMap.put("fasthelp",		reservedWord);
		tokenMap.put("fc",			reservedWord);
		tokenMap.put("fdisk",		reservedWord);
		tokenMap.put("find",			reservedWord);
		tokenMap.put("for",			reservedWord);
		tokenMap.put("format",		reservedWord);
		tokenMap.put("ftp",			reservedWord);
		tokenMap.put("graftabl",		reservedWord);
		tokenMap.put("help",			reservedWord);
		tokenMap.put("ifshlp.sys",	reservedWord);
		tokenMap.put("in",			reservedWord);
		tokenMap.put("ipconfig",		reservedWord);
		tokenMap.put("keyb",			reservedWord);
		tokenMap.put("label",		reservedWord);
		tokenMap.put("lh",			reservedWord);
		tokenMap.put("loadfix",		reservedWord);
		tokenMap.put("loadhigh",		reservedWord);
		tokenMap.put("lock",			reservedWord);
		tokenMap.put("md",			reservedWord);
		tokenMap.put("mem",			reservedWord);
		tokenMap.put("mkdir",		reservedWord);
		tokenMap.put("mode",			reservedWord);
		tokenMap.put("more",			reservedWord);
		tokenMap.put("move",			reservedWord);
		tokenMap.put("msav",			reservedWord);
		tokenMap.put("msd",			reservedWord);
		tokenMap.put("mscdex",		reservedWord);
		tokenMap.put("nbtstat",		reservedWord);
		tokenMap.put("net",			reservedWord);
		tokenMap.put("netstat",		reservedWord);
		tokenMap.put("nlsfunc",		reservedWord);
		tokenMap.put("not",			reservedWord);
		tokenMap.put("nslookup",		reservedWord);
		tokenMap.put("path",			reservedWord);
		tokenMap.put("pathping",		reservedWord);
		tokenMap.put("pause",		reservedWord);
		tokenMap.put("ping",			reservedWord);
		tokenMap.put("power",		reservedWord);
		tokenMap.put("print",		reservedWord);
		tokenMap.put("prompt",		reservedWord);
		tokenMap.put("qbasic",		reservedWord);
		tokenMap.put("rd",			reservedWord);
		tokenMap.put("ren",			reservedWord);
		tokenMap.put("rename",		reservedWord);
		tokenMap.put("rmdir",		reservedWord);
		tokenMap.put("route",		reservedWord);
		tokenMap.put("sc",			reservedWord);
		tokenMap.put("scandisk",		reservedWord);
		tokenMap.put("scandreg",		reservedWord);
		tokenMap.put("set",			reservedWord);
		tokenMap.put("setx",			reservedWord);
		tokenMap.put("setver",		reservedWord);
		tokenMap.put("share",		reservedWord);
		tokenMap.put("shutdown",		reservedWord);
		tokenMap.put("smartdrv",		reservedWord);
		tokenMap.put("sort",			reservedWord);
		tokenMap.put("subset",		reservedWord);
		tokenMap.put("switches",		reservedWord);
		tokenMap.put("sys",			reservedWord);
		tokenMap.put("time",			reservedWord);
		tokenMap.put("tracert",		reservedWord);
		tokenMap.put("tree",			reservedWord);
		tokenMap.put("type",			reservedWord);
		tokenMap.put("undelete",		reservedWord);
		tokenMap.put("unformat",		reservedWord);
		tokenMap.put("unlock",		reservedWord);
		tokenMap.put("ver",			reservedWord);
		tokenMap.put("verify",		reservedWord);
		tokenMap.put("vol",			reservedWord);
		tokenMap.put("xcopy",		reservedWord);

		return tokenMap;

	}


	/**
	 * Returns a list of tokens representing the given text.
	 *
	 * @param text The text to break into tokens.
	 * @param startTokenType The token with which to start tokenizing.
	 * @param startOffset The offset at which the line of tokens begins.
	 * @return A linked list of tokens representing <code>text</code>.
	 */
	public Token getTokenList(Segment text, int startTokenType, final int startOffset) {

		resetTokenList();

		char[] array = text.array;
		int offset = text.offset;
		int count = text.count;
		int end = offset + count;

		// See, when we find a token, its starting position is always of the form:
		// 'startOffset + (currentTokenStart-offset)'; but since startOffset and
		// offset are constant, tokens' starting positions become:
		// 'newStartOffset+currentTokenStart' for one less subtraction operation.
		int newStartOffset = startOffset - offset;

		currentTokenStart = offset;
		currentTokenType  = startTokenType;

//beginning:
		for (int i=offset; i<end; i++) {

			char c = array[i];

			switch (currentTokenType) {

				case Token.NULL:

					currentTokenStart = i;	// Starting a new token here.

					switch (c) {

						case ' ':
						case '\t':
							currentTokenType = Token.WHITESPACE;
							break;

						case '"':
							currentTokenType = Token.ERROR_STRING_DOUBLE;
							break;

						case '%':
							currentTokenType = Token.VARIABLE;
							break;

						// The "separators".
						case '(':
						case ')':
							addToken(text, currentTokenStart,i, Token.SEPARATOR, newStartOffset+currentTokenStart);
							currentTokenType = Token.NULL;
							break;

						// The "separators2".
						case ',':
						case ';':
							addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenType = Token.NULL;
							break;

						// Newer version of EOL comments, or a label
						case ':':
							// If this will be the first token added, it is
							// a new-style comment or a label
							if (firstToken==null) {
								if (i<end-1 && array[i+1]==':') { // new-style comment
									currentTokenType = Token.COMMENT_EOL;
								}
								else { // Label
									currentTokenType = Token.PREPROCESSOR;
								}
							}
							else { // Just a colon
								currentTokenType = Token.IDENTIFIER;
							}
							break;

						default:

							// Just to speed things up a tad, as this will usually be the case (if spaces above failed).
							if (RSyntaxUtilities.isLetterOrDigit(c) || c=='\\') {
								currentTokenType = Token.IDENTIFIER;
								break;
							}

							int indexOf = operators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i, Token.OPERATOR, newStartOffset+currentTokenStart);
								currentTokenType = Token.NULL;
								break;
							}
							else {
								currentTokenType = Token.IDENTIFIER;
								break;
							}

					} // End of switch (c).

					break;

				case Token.WHITESPACE:

					switch (c) {

						case ' ':
						case '\t':
							break;	// Still whitespace.

						case '"':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.ERROR_STRING_DOUBLE;
							break;

						case '%':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.VARIABLE;
							break;

						// The "separators".
						case '(':
						case ')':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.SEPARATOR, newStartOffset+i);
							currentTokenType = Token.NULL;
							break;

						// The "separators2".
						case ',':
						case ';':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
							currentTokenType = Token.NULL;
							break;

						// Newer version of EOL comments, or a label
						case ':':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							// If the previous (whitespace) token was the first token
							// added, this is a new-style comment or a label
							if (firstToken.getNextToken()==null) {
								if (i<end-1 && array[i+1]==':') { // new-style comment
									currentTokenType = Token.COMMENT_EOL;
								}
								else { // Label
									currentTokenType = Token.PREPROCESSOR;
								}
							}
							else { // Just a colon
								currentTokenType = Token.IDENTIFIER;
							}
							break;

						default:	// Add the whitespace token and start anew.

							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;

							// Just to speed things up a tad, as this will usually be the case (if spaces above failed).
							if (RSyntaxUtilities.isLetterOrDigit(c) || c=='\\') {
								currentTokenType = Token.IDENTIFIER;
								break;
							}

							int indexOf = operators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i, Token.OPERATOR, newStartOffset+currentTokenStart);
								currentTokenType = Token.NULL;
								break;
							}
							else {
								currentTokenType = Token.IDENTIFIER;
							}

					} // End of switch (c).

					break;

				default: // Should never happen
				case Token.IDENTIFIER:

					switch (c) {

						case ' ':
						case '\t':
							// Check for REM comments.
							if (i-currentTokenStart==3 &&
								(array[i-3]=='r' || array[i-3]=='R') &&
								(array[i-2]=='e' || array[i-2]=='E') &&
								(array[i-1]=='m' || array[i-1]=='M')) {
									currentTokenType = Token.COMMENT_EOL;
									break;
							}
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.WHITESPACE;
							break;

						case '"':
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.ERROR_STRING_DOUBLE;
							break;

						case '%':
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.VARIABLE;
							break;

						// Should be part of identifiers, but not at end of "REM".
						case '\\':
							// Check for REM comments.
							if (i-currentTokenStart==3 &&
								(array[i-3]=='r' || array[i-3]=='R') &&
								(array[i-2]=='e' || array[i-2]=='E') &&
								(array[i-1]=='m' || array[i-1]=='M')) {
									currentTokenType = Token.COMMENT_EOL;
							}
							break;

						case '.':
						case '_':
							break;	// Characters good for identifiers.

						// The "separators".
						case '(':
						case ')':
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.SEPARATOR, newStartOffset+i);
							currentTokenType = Token.NULL;
							break;

						// The "separators2".
						case ',':
						case ';':
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
							currentTokenType = Token.NULL;
							break;

						default:

							// Just to speed things up a tad, as this will usually be the case.
							if (RSyntaxUtilities.isLetterOrDigit(c) || c=='\\') {
								break;
							}

							int indexOf = operators.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}

							// Otherwise, fall through and assume we're still okay as an IDENTIFIER...

					} // End of switch (c).

					break;

				case Token.COMMENT_EOL:
					i = end - 1;
					addToken(text, currentTokenStart,i, Token.COMMENT_EOL, newStartOffset+currentTokenStart);
					// We need to set token type to null so at the bottom we don't add one more token.
					currentTokenType = Token.NULL;
					break;

				case Token.PREPROCESSOR: // Used for labels
					i = end - 1;
					addToken(text, currentTokenStart,i, Token.PREPROCESSOR, newStartOffset+currentTokenStart);
					// We need to set token type to null so at the bottom we don't add one more token.
					currentTokenType = Token.NULL;
					break;

				case Token.ERROR_STRING_DOUBLE:

					if (c=='"') {
						addToken(text, currentTokenStart,i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset+currentTokenStart);
						currentTokenStart = i + 1;
						currentTokenType = Token.NULL;
					}
					// Otherwise, we're still an unclosed string...

					break;

				case Token.VARIABLE:

						if (i==currentTokenStart+1) { // first character after '%'.
							bracketVariable = false;
							switch (c) {
								case '{':
									bracketVariable = true;
									break;
								default:
									if (RSyntaxUtilities.isLetter(c) || c==' ') { // No tab, just space; spaces are okay in variable names.
										break;
									}
									else if (RSyntaxUtilities.isDigit(c)) { // Single-digit command-line argument ("%1").
										addToken(text, currentTokenStart,i, Token.VARIABLE, newStartOffset+currentTokenStart);
										currentTokenType = Token.NULL;
										break;
									}
									else { // Anything else, ???.
										addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart); // ???
										i--;
										currentTokenType = Token.NULL;
										break;
									}
							} // End of switch (c).
						}
						else { // Character other than first after the '%'.
							if (bracketVariable==true) {
								if (c=='}') {
									addToken(text, currentTokenStart,i, Token.VARIABLE, newStartOffset+currentTokenStart);
									currentTokenType = Token.NULL;
								}
							}
							else {
								if (c=='%') {
									addToken(text, currentTokenStart,i, Token.VARIABLE, newStartOffset+currentTokenStart);
									currentTokenType = Token.NULL;
								}
							}
							break;
						}
						break;

			} // End of switch (currentTokenType).

		} // End of for (int i=offset; i<end; i++).

		// Deal with the (possibly there) last token.
		if (currentTokenType != Token.NULL) {

				// Check for REM comments.
				if (end-currentTokenStart==3 &&
					(array[end-3]=='r' || array[end-3]=='R') &&
					(array[end-2]=='e' || array[end-2]=='E') &&
					(array[end-1]=='m' || array[end-1]=='M')) {
						currentTokenType = Token.COMMENT_EOL;
				}

				addToken(text, currentTokenStart,end-1, currentTokenType, newStartOffset+currentTokenStart);
		}

		addNullToken();

		// Return the first token in our linked list.
		return firstToken;

	}


}