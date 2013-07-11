/*
 * 12/06/2005
 *
 * PythonTokenMaker.java - Token maker for the Python programming language.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.modes;

import java.io.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractJFlexTokenMaker;
import org.fife.ui.rsyntaxtextarea.DefaultToken;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMaker;


/**
 * Scanner for the Python programming language.
 *
 * @author Robert Futrell
 * @version 0.3
 */
%%

%public
%class PythonTokenMaker
%extends AbstractJFlexTokenMaker
%implements TokenMaker
%unicode
%type org.fife.ui.rsyntaxtextarea.Token


%{


	/**
	 * Constructor.  This must be here because JFlex does not generate a
	 * no-parameter constructor.
	 */
	public PythonTokenMaker() {
		super();
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 */
	private void addToken(int tokenType) {
		addToken(zzStartRead, zzMarkedPos-1, tokenType);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 */
	private void addToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param array The character array.
	 * @param start The starting offset in the array.
	 * @param end The ending offset in the array.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *                    occurs.
	 */
	public void addToken(char[] array, int start, int end, int tokenType, int startOffset) {
		super.addToken(array, start,end, tokenType, startOffset);
		zzStartRead = zzMarkedPos;
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
	 * Returns the first token in the linked list of tokens generated
	 * from <code>text</code>.  This method must be implemented by
	 * subclasses so they can correctly implement syntax highlighting.
	 *
	 * @param text The text from which to get tokens.
	 * @param initialTokenType The token type we should start with.
	 * @param startOffset The offset into the document at which
	 *        <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		int state = Token.NULL;
		switch (initialTokenType) {
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
				state = LONG_STRING_2;
				break;
			case Token.LITERAL_CHAR:
				state = LONG_STRING_1;
				break;
			default:
				state = Token.NULL;
		}

		s = text;
		try {
			yyreset(zzReader);
			yybegin(state);
			return yylex();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return new DefaultToken();
		}

	}


	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream 
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>YY_INITIAL</tt>.
	 *
	 * @param reader   the new input stream 
	 */
	public final void yyreset(java.io.Reader reader) throws java.io.IOException {
		// 's' has been updated.
		zzBuffer = s.array;
		/*
		 * We replaced the line below with the two below it because zzRefill
		 * no longer "refills" the buffer (since the way we do it, it's always
		 * "full" the first time through, since it points to the segment's
		 * array).  So, we assign zzEndRead here.
		 */
		//zzStartRead = zzEndRead = s.offset;
		zzStartRead = s.offset;
		zzEndRead = zzStartRead + s.count - 1;
		zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;
		zzLexicalState = YYINITIAL;
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
	}


	/**
	 * Refills the input buffer.
	 *
	 * @return      <code>true</code> if EOF was reached, otherwise
	 *              <code>false</code>.
	 * @exception   IOException  if any I/O-Error occurs.
	 */
	private boolean zzRefill() throws java.io.IOException {
		return zzCurrentPos>=s.offset+s.count;
	}


%}

/* This part is taken from http://www.python.org/doc/2.2.3/ref/grammar.txt */
identifier		= (({letter}|"_")({letter}|{digit}|"_")*)
letter			= ({lowercase}|{uppercase})
lowercase			= ([a-z])
uppercase			= ([A-Z])
digit			= ([0-9])
stringliteral		= ({stringprefix}?{shortstring})
stringprefix		= ("r"|"u"[rR]?|"R"|"U"[rR]?)
shortstring1		= ([\']{shortstring1item}*[\']?)
shortstring2		= ([\"]{shortstring2item}*[\"]?)
shortstring		= ({shortstring1}|{shortstring2})
shortstring1item	= ({shortstring1char}|{escapeseq})
shortstring2item	= ({shortstring2char}|{escapeseq})
shortstring1char	= ([^\\\n\'])
shortstring2char	= ([^\\\n\"])
escapeseq			= ([\\].)
longinteger		= ({integer}[lL])
integer			= ({decimalinteger}|{octinteger}|{hexinteger})
decimalinteger		= ({nonzerodigit}{digit}*|"0")
octinteger		= ("0"{octdigit}+)
hexinteger		= ("0"[xX]{hexdigit}+)
nonzerodigit		= ([1-9])
octdigit			= ([0-7])
hexdigit			= ({digit}|[a-f]|[A-F])
floatnumber		= ({pointfloat}|{exponentfloat})
pointfloat		= ({intpart}?{fraction}|{intpart}".")
exponentfloat		= (({intpart}|{pointfloat}){exponent})
intpart			= ({digit}+)
fraction			= ("."{digit}+)
exponent			= ([eE][\+\-]?{digit}+)
imagnumber		= (({floatnumber}|{intpart})[jJ])

ErrorNumberFormat	= ({digit}{NonSeparator}+)
NonSeparator		= ([^\t\f\r\n\ \(\)\{\}\[\]\;\,\.\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\']|"#")

LongStringStart1	= ({stringprefix}?\'\'\')
LongStringStart2	= ({stringprefix}?\"\"\")

LineTerminator		= (\n)
WhiteSpace		= ([ \t\f])

LineComment		= ("#".*)


%state LONG_STRING_1
%state LONG_STRING_2


%%

/* Keywords */
<YYINITIAL> "and"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "as"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "assert"				{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "break"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "class"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "continue"				{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "def"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "del"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "elif"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "else"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "except"				{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "exec"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "finally"				{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "for"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "from"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "global"				{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "if"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "import"				{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "in"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "is"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "lambda"				{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "not"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "or"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "pass"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "print"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "raise"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "return"				{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "try"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "while"					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> "yield"					{ addToken(Token.RESERVED_WORD); }

/* Data types. */
<YYINITIAL> "char"					{ addToken(Token.DATA_TYPE); }
<YYINITIAL> "double"				{ addToken(Token.DATA_TYPE); }
<YYINITIAL> "float"					{ addToken(Token.DATA_TYPE); }
<YYINITIAL> "int"					{ addToken(Token.DATA_TYPE); }
<YYINITIAL> "long"					{ addToken(Token.DATA_TYPE); }
<YYINITIAL> "short"					{ addToken(Token.DATA_TYPE); }
<YYINITIAL> "signed"				{ addToken(Token.DATA_TYPE); }
<YYINITIAL> "unsigned"				{ addToken(Token.DATA_TYPE); }
<YYINITIAL> "void"					{ addToken(Token.DATA_TYPE); }

/* Standard functions */
<YYINITIAL> "abs"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "apply"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "bool"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "buffer"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "callable"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "chr"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "classmethod"			{ addToken(Token.FUNCTION); }
<YYINITIAL> "cmp"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "coerce"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "compile"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "complex"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "delattr"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "dict"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "dir"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "divmod"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "enumerate"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "eval"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "execfile"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "file"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "filter"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "float"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "getattr"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "globals"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "hasattr"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "hash"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "hex"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "id"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "input"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "int"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "intern"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "isinstance"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "issubclass"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "iter"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "len"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "list"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "locals"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "long"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "map"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "max"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "min"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "object"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "oct"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "open"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "ord"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "pow"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "property"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "range"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "raw_input"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "reduce"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "reload"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "repr"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "round"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "setattr"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "slice"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "staticmethod"			{ addToken(Token.FUNCTION); }
<YYINITIAL> "str"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "sum"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "super"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "tuple"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "type"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "unichr"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "unicode"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "vars"					{ addToken(Token.FUNCTION); }
<YYINITIAL> "xrange"				{ addToken(Token.FUNCTION); }
<YYINITIAL> "zip"					{ addToken(Token.FUNCTION); }


<YYINITIAL> {

	{LineTerminator}				{ addNullToken(); return firstToken; }

	{identifier}					{ addToken(Token.IDENTIFIER); }

	{WhiteSpace}+					{ addToken(Token.WHITESPACE); }

	/* String/Character Literals. */
	{stringliteral}				{ addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
	{LongStringStart1}				{ yybegin(LONG_STRING_1); addToken(Token.LITERAL_CHAR); }
	{LongStringStart2}				{ yybegin(LONG_STRING_2); addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }

	/* Comment Literals. */
	{LineComment}					{ addToken(Token.COMMENT_EOL); }

	/* Separators. */
	"("							{ addToken(Token.SEPARATOR); }
	")"							{ addToken(Token.SEPARATOR); }
	"["							{ addToken(Token.SEPARATOR); }
	"]"							{ addToken(Token.SEPARATOR); }
	"{"							{ addToken(Token.SEPARATOR); }
	"}"							{ addToken(Token.SEPARATOR); }

	/* Operators. */
	"="							{ addToken(Token.OPERATOR); }
	"+"							{ addToken(Token.OPERATOR); }
	"-"							{ addToken(Token.OPERATOR); }
	"*"							{ addToken(Token.OPERATOR); }
	"/"							{ addToken(Token.OPERATOR); }
	"%"							{ addToken(Token.OPERATOR); }
	"**"							{ addToken(Token.OPERATOR); }
	"~"							{ addToken(Token.OPERATOR); }
	"<"							{ addToken(Token.OPERATOR); }
	">"							{ addToken(Token.OPERATOR); }
	"<<"							{ addToken(Token.OPERATOR); }
	">>"							{ addToken(Token.OPERATOR); }
	"=="							{ addToken(Token.OPERATOR); }
	"+="							{ addToken(Token.OPERATOR); }
	"-="							{ addToken(Token.OPERATOR); }
	"*="							{ addToken(Token.OPERATOR); }
	"/="							{ addToken(Token.OPERATOR); }
	"%="							{ addToken(Token.OPERATOR); }
	">>="						{ addToken(Token.OPERATOR); }
	"<<="						{ addToken(Token.OPERATOR); }
	"^"							{ addToken(Token.OPERATOR); }
	"&"							{ addToken(Token.OPERATOR); }
	"&&"							{ addToken(Token.OPERATOR); }
	"|"							{ addToken(Token.OPERATOR); }
	"||"							{ addToken(Token.OPERATOR); }
	"?"							{ addToken(Token.OPERATOR); }
	":"							{ addToken(Token.OPERATOR); }
	","							{ addToken(Token.OPERATOR); }
	"!"							{ addToken(Token.OPERATOR); }
	"++"							{ addToken(Token.OPERATOR); }
	"--"							{ addToken(Token.OPERATOR); }
	"."							{ addToken(Token.OPERATOR); }
	","							{ addToken(Token.OPERATOR); }

	/* Numbers */
	{longinteger}|{integer}			{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{floatnumber}|{imagnumber}		{ addToken(Token.LITERAL_NUMBER_FLOAT); }
	{ErrorNumberFormat}				{ addToken(Token.ERROR_NUMBER_FORMAT); }

	/* Other punctuation, we'll highlight it as "identifiers." */
	"@"							{ addToken(Token.IDENTIFIER); }
	";"							{ addToken(Token.IDENTIFIER); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters and flag them as bad. */
	.							{ addToken(Token.ERROR_IDENTIFIER); }

}

<LONG_STRING_1> {
	[^\']+						{ addToken(Token.LITERAL_CHAR); }
	"'''"						{ yybegin(YYINITIAL); addToken(Token.LITERAL_CHAR); }
	"'"							{ addToken(Token.LITERAL_CHAR); }
	<<EOF>>						{
									if (firstToken==null) {
										addToken(Token.LITERAL_CHAR); 
									}
									return firstToken;
								}
}

<LONG_STRING_2> {
	[^\"]+						{ addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
	\"\"\"						{ yybegin(YYINITIAL); addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
	\"							{ addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
	<<EOF>>						{
									if (firstToken==null) {
										addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); 
									}
									return firstToken;
								}
}
