/*
 * 03/16/2004
 *
 * UnixShellTokenMaker.java - Scanner for UNIX shell scripts.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.modes;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * A token maker that turns text into a linked list of <code>Token</code>s
 * for syntax highlighting UNIX shell scripts.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class UnixShellTokenMaker extends AbstractTokenMaker {

	protected final String operators = "=|><&";
	protected final String separators = "()[]";
	protected final String separators2 = ".,;";			// Characters you don't want syntax highlighted but separate identifiers.
	protected final String shellVariables = "#-?$!*@_";	// Characters that are part of "$<char>" shell variables; e.g., "$_".


	private int currentTokenStart;
	private int currentTokenType;


	/**
	 * Constructor.
	 */
	public UnixShellTokenMaker() {
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
			// Since reserved words, functions, and data types are all passed into here
			// as "identifiers," we have to see what the token really is...
			case Token.IDENTIFIER:
				int value = wordsToHighlight.get(segment, start,end);
				if (value!=-1)
					tokenType = value;
				break;
			case Token.WHITESPACE:
			case Token.SEPARATOR:
			case Token.OPERATOR:
			case Token.LITERAL_NUMBER_DECIMAL_INT:
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
			case Token.LITERAL_CHAR:
			case Token.LITERAL_BACKQUOTE:
			case Token.COMMENT_EOL:
			case Token.PREPROCESSOR:
			case Token.VARIABLE:
				break;

			default:
				new Exception("Unknown tokenType: '" + tokenType + "'").
													printStackTrace();
				tokenType = Token.IDENTIFIER;
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
		return new String[] { "#", null };
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
	 * Returns the words to highlight for UNIX shell scripts.
	 *
	 * @return A <code>TokenMap</code> containing the words to highlight for
	 *         UNIX shell scripts.
	 * @see org.fife.ui.rsyntaxtextarea.AbstractTokenMaker#getWordsToHighlight
	 */
	public TokenMap getWordsToHighlight() {

		TokenMap tokenMap = new TokenMap();

		int reservedWord = Token.RESERVED_WORD;
		tokenMap.put("case",				reservedWord);
		tokenMap.put("do",				reservedWord);
		tokenMap.put("done",				reservedWord);
		tokenMap.put("elif",				reservedWord);
		tokenMap.put("else",				reservedWord);
		tokenMap.put("esac",				reservedWord);
		tokenMap.put("fi",				reservedWord);
		tokenMap.put("for",				reservedWord);
		tokenMap.put("if",				reservedWord);
		tokenMap.put("in",				reservedWord);
		tokenMap.put("select",			reservedWord);
		tokenMap.put("then",				reservedWord);
		tokenMap.put("until",			reservedWord);
		tokenMap.put("while",			reservedWord);

		int function = Token.FUNCTION;
		tokenMap.put("addbib",			function);
		tokenMap.put("admin",			function);
		tokenMap.put("alias",			function);
		tokenMap.put("apropos",			function);
		tokenMap.put("ar",			function);
		tokenMap.put("at",				function);
		tokenMap.put("awk",			function);
		tokenMap.put("banner",			function);
		tokenMap.put("basename",			function);
		tokenMap.put("batch",			function);
		tokenMap.put("bg",			function);
		tokenMap.put("biff",			function);
		tokenMap.put("bin-mail",			function);
		tokenMap.put("binmail",			function);
		tokenMap.put("break",			function);
		tokenMap.put("cal",			function);
		tokenMap.put("calendar",			function);
		tokenMap.put("cancel",			function);
		tokenMap.put("cat",			function);
		tokenMap.put("cb",			function);
		tokenMap.put("cc",			function);
		tokenMap.put("cd",			function);
		tokenMap.put("cdc",			function);
		tokenMap.put("chdir",			function);
		tokenMap.put("checkeq",		function);
		tokenMap.put("checknr",			function);
		tokenMap.put("chfn",			function);
		tokenMap.put("chgrp",			function);
		tokenMap.put("chmod",			function);
		tokenMap.put("chown",			function);
		tokenMap.put("chsh",			function);
		tokenMap.put("clear",			function);
		tokenMap.put("cmp",			function);
		tokenMap.put("colcrt",			function);
		tokenMap.put("comb",			function);
		tokenMap.put("comm",			function);
		tokenMap.put("command",			function);
		tokenMap.put("compress",			function);
		tokenMap.put("continue",			function);
		tokenMap.put("cp",			function);
		tokenMap.put("cpio",			function);
		tokenMap.put("cpp",			function);
		tokenMap.put("crontab",			function);
		tokenMap.put("csh",			function);
		tokenMap.put("ctags",			function);
		tokenMap.put("cut", 		function);
		tokenMap.put("cvs",			function);
		tokenMap.put("date",			function);
		tokenMap.put("dbx",			function);
		tokenMap.put("delta",			function);
		tokenMap.put("deroff",			function);
		tokenMap.put("df",			function);
		tokenMap.put("diff",			function);
		tokenMap.put("dtree",			function);
		tokenMap.put("du",			function);
		tokenMap.put("e",			function);
		tokenMap.put("echo",			function);
		tokenMap.put("ed",			function);
		tokenMap.put("edit",			function);
		tokenMap.put("enscript",		function);
		tokenMap.put("eqn",			function);
		tokenMap.put("error",		function);
		tokenMap.put("eval",		function);
		tokenMap.put("ex",			function);
		tokenMap.put("exec",			function);
		tokenMap.put("exit",			function);
		tokenMap.put("expand",		function);
		tokenMap.put("export",		function);
		tokenMap.put("expr",			function);
		tokenMap.put("false",			function);
		tokenMap.put("fc",			function);
		tokenMap.put("fg",			function);
		tokenMap.put("file",			function);
		tokenMap.put("find",			function);
		tokenMap.put("finger",			function);
		tokenMap.put("fmt",			function);
		tokenMap.put("fmt_mail",			function);
		tokenMap.put("fold",			function);
		tokenMap.put("ftp",			function);
		tokenMap.put("function",		function);
		tokenMap.put("gcore",			function);
		tokenMap.put("get",			function);
		tokenMap.put("getopts",			function);
		tokenMap.put("gprof",			function);
		tokenMap.put("grep",			function);
		tokenMap.put("groups",			function);
		tokenMap.put("gunzip",			function);
		tokenMap.put("gzip",			function);
		tokenMap.put("hashcheck",			function);
		tokenMap.put("hashmake",			function);
		tokenMap.put("head",			function);
		tokenMap.put("help",			function);
		tokenMap.put("history",			function);
		tokenMap.put("imake",			function);
		tokenMap.put("indent",			function);
		tokenMap.put("install",			function);
		tokenMap.put("jobs",			function);
		tokenMap.put("join",			function);
		tokenMap.put("kill",			function);
		tokenMap.put("last",			function);
		tokenMap.put("ld",			function);
		tokenMap.put("leave",			function);
		tokenMap.put("less",			function);
		tokenMap.put("let",			function);
		tokenMap.put("lex",			function);
		tokenMap.put("lint",			function);
		tokenMap.put("ln",			function);
		tokenMap.put("login",			function);
		tokenMap.put("look",			function);
		tokenMap.put("lookbib",			function);
		tokenMap.put("lorder",			function);
		tokenMap.put("lp",			function);
		tokenMap.put("lpq",			function);
		tokenMap.put("lpr",			function);
		tokenMap.put("lprm",			function);
		tokenMap.put("ls",			function);
		tokenMap.put("mail",			function);
		tokenMap.put("Mail",			function);
		tokenMap.put("make",			function);
		tokenMap.put("man",			function);
		tokenMap.put("md",			function);
		tokenMap.put("mesg",			function);
		tokenMap.put("mkdir",		function);
		tokenMap.put("mkstr",			function);
		tokenMap.put("more",			function);
		tokenMap.put("mount",			function);
		tokenMap.put("mv",			function);
		tokenMap.put("nawk",			function);
		tokenMap.put("neqn",			function);
		tokenMap.put("nice",			function);
		tokenMap.put("nm",			function);
		tokenMap.put("nroff",			function);
		tokenMap.put("od",			function);
		tokenMap.put("page",			function);
		tokenMap.put("passwd",			function);
		tokenMap.put("paste",			function);
		tokenMap.put("pr",			function);
		tokenMap.put("print",		function);
		tokenMap.put("printf",		function);
		tokenMap.put("printenv",			function);
		tokenMap.put("prof",			function);
		tokenMap.put("prs",			function);
		tokenMap.put("prt",			function);
		tokenMap.put("ps",			function);
		tokenMap.put("ptx",			function);
		tokenMap.put("pwd",			function);
		tokenMap.put("quota",			function);
		tokenMap.put("ranlib",			function);
		tokenMap.put("rcp",			function);
		tokenMap.put("rcs",			function);
		tokenMap.put("rcsdiff",			function);
		tokenMap.put("read",			function);
		tokenMap.put("readonly",			function);
		tokenMap.put("red",			function);
		tokenMap.put("return",		function);
		tokenMap.put("rev",			function);
		tokenMap.put("rlogin",			function);
		tokenMap.put("rm",			function);
		tokenMap.put("rmdel",			function);
		tokenMap.put("rmdir",			function);
		tokenMap.put("roffbib",			function);
		tokenMap.put("rsh",			function);
		tokenMap.put("rup",			function);
		tokenMap.put("ruptime",			function);
		tokenMap.put("rusers",			function);
		tokenMap.put("rwall",			function);
		tokenMap.put("rwho",			function);
		tokenMap.put("sact",			function);
		tokenMap.put("sccs",			function);
		tokenMap.put("sccsdiff",			function);
		tokenMap.put("script",			function);
		tokenMap.put("sed",			function);
		tokenMap.put("set",			function);
		tokenMap.put("setgroups",			function);
		tokenMap.put("setsenv",			function);
		tokenMap.put("sh",			function);
		tokenMap.put("shift",		function);
		tokenMap.put("size",			function);
		tokenMap.put("sleep",			function);
		tokenMap.put("sort",			function);
		tokenMap.put("sortbib",			function);
		tokenMap.put("spell",		function);
		tokenMap.put("split",			function);
		tokenMap.put("ssh",				function);
		tokenMap.put("strings",			function);
		tokenMap.put("strip",			function);
		tokenMap.put("stty",			function);
		tokenMap.put("su",			function);
		tokenMap.put("sudo",			function);
		tokenMap.put("symorder",			function);
		tokenMap.put("tabs",			function);
		tokenMap.put("tail",			function);
		tokenMap.put("talk",			function);
		tokenMap.put("tar",			function);
		tokenMap.put("tbl",			function);
		tokenMap.put("tee",			function);
		tokenMap.put("telnet",			function);
		tokenMap.put("test",			function);
		tokenMap.put("tftp",			function);
		tokenMap.put("time",			function);
		tokenMap.put("times",			function);
		tokenMap.put("touch",			function);
		tokenMap.put("trap",			function);
		tokenMap.put("troff",			function);
		tokenMap.put("true",			function);
		tokenMap.put("tsort",			function);
		tokenMap.put("tty",			function);
		tokenMap.put("type",			function);
		tokenMap.put("typeset",		function);
		tokenMap.put("ue",			function);
		tokenMap.put("ul",			function);
		tokenMap.put("ulimit",			function);
		tokenMap.put("umask",		function);
		tokenMap.put("unalias",			function);
		tokenMap.put("uncompress",			function);
		tokenMap.put("unexpand",		function);
		tokenMap.put("unget",			function);
		tokenMap.put("unifdef",			function);
		tokenMap.put("uniq",			function);
		tokenMap.put("units",			function);
		tokenMap.put("unset",			function);
		tokenMap.put("uptime",			function);
		tokenMap.put("users",			function);
		tokenMap.put("uucp",			function);
		tokenMap.put("uudecode",			function);
		tokenMap.put("uuencode",			function);
		tokenMap.put("uulog",			function);
		tokenMap.put("uuname",			function);
		tokenMap.put("uusend",			function);
		tokenMap.put("uux",			function);
		tokenMap.put("vacation",			function);
		tokenMap.put("val",			function);
		tokenMap.put("vedit",			function);
		tokenMap.put("vgrind",			function);
		tokenMap.put("vi",			function);
		tokenMap.put("view",			function);
		tokenMap.put("vtroff",			function);
		tokenMap.put("w",			function);
		tokenMap.put("wait",			function);
		tokenMap.put("wall",			function);
		tokenMap.put("wc",			function);
		tokenMap.put("wait",			function);
		tokenMap.put("what",			function);
		tokenMap.put("whatis",			function);
		tokenMap.put("whence",			function);
		tokenMap.put("whereis",			function);
		tokenMap.put("which",			function);
		tokenMap.put("who",			function);
		tokenMap.put("whoami",			function);
		tokenMap.put("write",			function);
		tokenMap.put("xargs",			function);
		tokenMap.put("xstr",			function);
		tokenMap.put("yacc",			function);
		tokenMap.put("yes",			function);
		tokenMap.put("zcat",			function);

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
		// 'newStartOffset+currentTokenStart' for one less subraction operation.
		int newStartOffset = startOffset - offset;

		currentTokenStart = offset;
		currentTokenType  = startTokenType;
		boolean backslash = false;

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

						case '`':
							if (backslash) { // Escaped back quote => call '`' an identifier..
								addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								backslash = false;
							}
							else {
								currentTokenType = Token.LITERAL_BACKQUOTE;
							}
							break;

						case '"':
							if (backslash) { // Escaped double quote => call '"' an identifier..
								addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								backslash = false;
							}
							else {
								currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							}
							break;

						case '\'':
							if (backslash) { // Escaped single quote => call '\'' an identifier.
								addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								backslash = false;
							}
							else {
								currentTokenType = Token.LITERAL_CHAR;
							}
							break;

						case '\\':
							addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenType = Token.NULL;
							backslash = !backslash;
							break;

						case '$':
							if (backslash) { // Escaped dollar sign => call '$' an identifier..
								addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								backslash = false;
							}
							else {
								currentTokenType = Token.VARIABLE;
							}
							break;

						case '#':
							backslash = false;
							currentTokenType = Token.COMMENT_EOL;
							break;

						default:
							if (RSyntaxUtilities.isDigit(c)) {
								currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
								break;
							}
							else if (RSyntaxUtilities.isLetter(c) || c=='/' || c=='_') {
								currentTokenType = Token.IDENTIFIER;
								break;
							}
							int indexOf = operators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i, Token.OPERATOR, newStartOffset+currentTokenStart);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i, Token.SEPARATOR, newStartOffset+currentTokenStart);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators2.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
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

						case '\\':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
							currentTokenType = Token.NULL;
							backslash = true; // Previous char whitespace => this must be first backslash.
							break;

						case '`': // Don't need to worry about backslashes as previous char is space.
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_BACKQUOTE;
							backslash = false;
							break;

						case '"': // Don't need to worry about backslashes as previous char is space.
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							backslash = false;
							break;

						case '\'': // Don't need to worry about backslashes as previous char is space.
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_CHAR;
							backslash = false;
							break;
						
						case '$': // Don't need to worry about backslashes as previous char is space.
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.VARIABLE;
							backslash = false;
							break;

						case '#':
							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.COMMENT_EOL;
							break;

						default:	// Add the whitespace token and start anew.

							addToken(text, currentTokenStart,i-1, Token.WHITESPACE, newStartOffset+currentTokenStart);
							currentTokenStart = i;

							if (RSyntaxUtilities.isDigit(c)) {
								currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
								break;
							}
							else if (RSyntaxUtilities.isLetter(c) || c=='/' || c=='_') {
								currentTokenType = Token.IDENTIFIER;
								break;
							}
							int indexOf = operators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, i,i, Token.SEPARATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators2.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
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
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.WHITESPACE;
							break;

						case '/': // Special-case to colorize commands like "echo" in "/bin/echo"
							addToken(text, currentTokenStart,i, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i+1;
							currentTokenType = Token.NULL;
							break;

						case '`': // Don't need to worry about backslashes as previous char is space.
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_BACKQUOTE;
							backslash = false;
							break;

						case '"': // Don't need to worry about backslashes as previous char is non-backslash.
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							backslash = false;
							break;

						case '\'': // Don't need to worry about backslashes as previous char is non-backslash.
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_CHAR;
							backslash = false;
							break;

						case '\\':
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
							currentTokenType = Token.NULL;
							backslash = true;
							break;

						case '$': // Don't need to worry about backslashes as previous char is non-backslash.
							addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.VARIABLE;
							backslash = false;
							break;
						
						case '=': // Special case here; when you have "identifier=<value>" in shell, "identifier" is a variable.
							addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
							currentTokenType = Token.NULL;
							break;

						default:
							if (RSyntaxUtilities.isLetterOrDigit(c) || c=='/' || c=='_') {
								break;	// Still an identifier of some type.
							}
							int indexOf = operators.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.SEPARATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators2.indexOf(c,0);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.IDENTIFIER, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							// Otherwise, we're still an identifier (?).

					} // End of switch (c).

					break;

				case Token.LITERAL_NUMBER_DECIMAL_INT:

					switch (c) {

						case ' ':
						case '\t':
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.WHITESPACE;
							break;

						case '`': // Don't need to worry about backslashes as previous char is space.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_BACKQUOTE;
							backslash = false;
							break;

						case '"': // Don't need to worry about backslashes as previous char is non-backslash.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
							backslash = false;
							break;

						case '\'': // Don't need to worry about backslashes as previous char is non-backslash.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.LITERAL_CHAR;
							backslash = false;
							break;

						case '$': // Don't need to worry about backslashes as previous char is non-backslash.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							currentTokenStart = i;
							currentTokenType = Token.VARIABLE;
							backslash = false;
							break;

						case '\\':
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
							currentTokenType = Token.NULL;
							backslash = true;
							break;

						default:

							if (RSyntaxUtilities.isDigit(c)) {
								break;	// Still a literal number.
							}
							int indexOf = operators.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.OPERATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.SEPARATOR, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}
							indexOf = separators2.indexOf(c);
							if (indexOf>-1) {
								addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
								addToken(text, i,i, Token.IDENTIFIER, newStartOffset+i);
								currentTokenType = Token.NULL;
								break;
							}

							// Otherwise, remember this was a number and start over.
							addToken(text, currentTokenStart,i-1, Token.LITERAL_NUMBER_DECIMAL_INT, newStartOffset+currentTokenStart);
							i--;
							currentTokenType = Token.NULL;

					} // End of switch (c).

					break;
				
				case Token.VARIABLE:

					// Note that we first arrive here AFTER the '$' character.
					// First check if the variable name is enclosed in '{' and '}' characters.
					if (c=='{') {
						while (++i<end) {
							if (array[i]=='}') {
								addToken(text, currentTokenStart,i, Token.VARIABLE, newStartOffset+currentTokenStart);
								currentTokenType = Token.NULL;
								break;
							}
						} // End of while (++i<end).
						if (i==end) { // Happens when '}' wasn't found...
							addToken(text, currentTokenStart,end-1, Token.VARIABLE, newStartOffset+currentTokenStart);
							currentTokenType = Token.NULL;
						}
						break;
					} // End of if (i<end-1 && array[i+1]=='{').

					// If we didn't find the '{' character, find the end of the variable...
					while (i<end) {
						c = array[i];	// Not needed the first iteration, but can't think of a better way to do it...
						if (!RSyntaxUtilities.isLetterOrDigit(c) && shellVariables.indexOf(c)==-1 && c!='_') {
							addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
							i--;
							currentTokenType = Token.NULL;
							break;
						}
						i++;
					}
								
					// This only happens if we never found the end of the variable in the loop above.
					if (i==end) {
						addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
						currentTokenType = Token.NULL;
					}
								
					break;

				case Token.COMMENT_EOL:
					// If we got here, then the line != "#" only, so check for "#!".
					if (c=='!')
						currentTokenType = Token.PREPROCESSOR;
					i = end - 1;
					addToken(text, currentTokenStart,i, currentTokenType, newStartOffset+currentTokenStart);
					// We need to set token type to null so at the bottom we don't add one more token.
					currentTokenType = Token.NULL;
					
					break;

				case Token.LITERAL_CHAR:

						if (c=='\\') {
							backslash = !backslash; // Okay because if we got in here, backslash was initially false.
						}
						else {
							if (c=='\'' && !backslash) {
								addToken(text, currentTokenStart,i, Token.LITERAL_CHAR, newStartOffset+currentTokenStart);
								currentTokenStart = i + 1;
								currentTokenType = Token.NULL;
								// backslash is definitely false when we leave.
							}

							backslash = false; // Need to set backslash to false here as a character was typed.

						}
						// Otherwise, we're still an unclosed char literal...

						break;

				case Token.LITERAL_BACKQUOTE:

						switch (c) {
								
							case '\\':
								backslash = !backslash;
								break;
								
							case '`':
								if (!backslash) {
									addToken(text, currentTokenStart,i, Token.LITERAL_BACKQUOTE, newStartOffset+currentTokenStart);
									currentTokenType = Token.NULL;
									// backslash is definitely false when we leave.
									break;
								}
								backslash = false;
								break;
							
							// Variable in the backquote string...
							case '$':

								if (backslash==true) {
									backslash = false;
									break;
								}
							
								// Add the string up-to the variable.
								addToken(text, currentTokenStart,i-1, Token.LITERAL_BACKQUOTE, newStartOffset+currentTokenStart);
								currentTokenType = Token.VARIABLE;
								currentTokenStart = i;
								
								// First check if the variable name is enclosed in '{' and '}' characters.
								if (i<end-1 && array[i+1]=='{') {
									i++; // Now we're on the '{' char.
									while (++i<end) {
										if (array[i]=='}') {
											addToken(text, currentTokenStart,i, Token.VARIABLE, newStartOffset+currentTokenStart);
											i++;
											if (i<end) {
												c = array[i];
												if (c=='`') { // The only rub - back quote right after variable.
													addToken(text, i,i, Token.LITERAL_BACKQUOTE, newStartOffset+i);
													currentTokenType = Token.NULL;
													break;
												}
												else { // Continue on with the string.
													currentTokenStart = i;
													currentTokenType = Token.LITERAL_BACKQUOTE;
													i--;
													break;
												}
											}
											else { // i==end = "trick" this method so that the string is continued to the next line.
												currentTokenStart = i;
												currentTokenType = Token.LITERAL_BACKQUOTE;
												break; // So we don't hit the condition below.
											}
										} // End of if (array[i]=='}').
									} // End of while (++i<end).
									if (i==end) { // Happens when '}' wasn't found...
										addToken(text, currentTokenStart,end-1, Token.VARIABLE, newStartOffset+currentTokenStart);
										currentTokenStart = end; // ???
										currentTokenType = Token.LITERAL_BACKQUOTE;
										break;
									}
								} // End of if (i<end-1 && array[i+1]=='{').
								
								// If we reached the end of the variable, get out.
								if (currentTokenType==Token.NULL || currentTokenType==Token.LITERAL_BACKQUOTE)
									break;

								// If we didn't find the '{' character, find the end of the variable...
								// Increment first to skip the '$'.
								while (++i<end) {
									c = array[i];
									if (!RSyntaxUtilities.isLetterOrDigit(c) && shellVariables.indexOf(c)==-1 && c!='_') {
										addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
										if (c=='`') { // The only rub.
											addToken(text, i,i, Token.LITERAL_BACKQUOTE, newStartOffset+i);
											currentTokenType = Token.NULL;
											break;
										}
										else {
											currentTokenStart = i;
											currentTokenType = Token.LITERAL_BACKQUOTE;
											i--;
											break;
										}
									}
								}
								
								// This only happens if we never found the end of the variable in the loop above.
								// We "trick" this method so that the backquote string token is at the end.
								if (i==end) {
									addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
									currentTokenStart = i;
									currentTokenType = Token.LITERAL_BACKQUOTE;
								}
								
								break;
								
							// Otherwise, we're still in an unclosed string...
							default:
								backslash = false; // Need to set backslash to false here as a character was typed.

						} // End of switch (c).
				
						break;

				case Token.LITERAL_STRING_DOUBLE_QUOTE:
				
						switch (c) {
								
							case '\\':
								backslash = !backslash;
								break;
								
							case '"':
								if (!backslash) {
									addToken(text, currentTokenStart,i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset+currentTokenStart);
									currentTokenType = Token.NULL;
									// backslash is definitely false when we leave.
									break;
								}
								backslash = false;
								break;
							
							// Variable in the double-quoted string...
							case '$':
							
								if (backslash==true) {
									backslash = false;
									break;
								}
							
								// Add the string up-to the variable.
								addToken(text, currentTokenStart,i-1, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset+currentTokenStart);
								currentTokenType = Token.VARIABLE;
								currentTokenStart = i;
								
								// First check if the variable name is enclosed in '{' and '}' characters.
								if (i<end-1 && array[i+1]=='{') {
									i++; // Now we're on the '{' char.
									while (++i<end) {
										if (array[i]=='}') {
											addToken(text, currentTokenStart,i, Token.VARIABLE, newStartOffset+currentTokenStart);
											i++;
											if (i<end) {
												c = array[i];
												if (c=='"') { // The only rub - double-quote right after variable.
													addToken(text, i,i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset+i);
													currentTokenType = Token.NULL;
													break;
												}
												else { // Continue on with the string.
													currentTokenStart = i;
													currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
													i--;
													break;
												}
											}
											else { // i==end = "trick" this method so that the string is continued to the next line.
												currentTokenStart = i;
												currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
												break; // So we don't hit the condition below.
											}
										} // End of if (array[i]=='}').
									} // End of while (++i<end).
									if (i==end) { // Happens when '}' wasn't found...
										addToken(text, currentTokenStart,end-1, Token.VARIABLE, newStartOffset+currentTokenStart);
										currentTokenStart = end; // ???
										currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
										break;
									}
								} // End of if (i<end-1 && array[i+1]=='{').
								
								// If we reached the end of the variable, get out.
								if (currentTokenType==Token.NULL || currentTokenType==Token.LITERAL_STRING_DOUBLE_QUOTE)
									break;

								// If we didn't find the '{' character, find the end of the variable...
								// Increment first to skip the '$'.
								while (++i<end) {
									c = array[i];
									if (!RSyntaxUtilities.isLetterOrDigit(c) && shellVariables.indexOf(c)==-1 && c!='_') {
										addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
										if (c=='"') { // The only rub.
											addToken(text, i,i, Token.LITERAL_STRING_DOUBLE_QUOTE, newStartOffset+i);
											currentTokenType = Token.NULL;
											break;
										}
										else {
											currentTokenStart = i;
											currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
											i--;
											break;
										}
									}
								}
								
								// This only happens if we never found the end of the variable in the loop above.
								// We "trick" this method so that the double-quote string token is at the end.
								if (i==end) {
									addToken(text, currentTokenStart,i-1, Token.VARIABLE, newStartOffset+currentTokenStart);
									currentTokenStart = i;
									currentTokenType = Token.LITERAL_STRING_DOUBLE_QUOTE;
								}
								
								break;
								
							// Otherwise, we're still in an unclosed string...
							default:
								backslash = false; // Need to set backslash to false here as a character was typed.

						} // End of switch (c).
				
						break;

			} // End of switch (currentTokenType).

		} // End of for (int i=offset; i<end; i++).

		switch (currentTokenType) {

			// Remember what token type to begin the next line with.
			case Token.LITERAL_BACKQUOTE:
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
			case Token.LITERAL_CHAR:
						addToken(text, currentTokenStart,end-1, currentTokenType, newStartOffset+currentTokenStart);
						break;

			// Do nothing if everything was okay.
			case Token.NULL:
						addNullToken();
						break;

			// All other token types don't continue to the next line...
			default:
						addToken(text, currentTokenStart,end-1, currentTokenType, newStartOffset+currentTokenStart);
						addNullToken();

		}

		// Return the first token in our linked list.
		return firstToken;

	}


}