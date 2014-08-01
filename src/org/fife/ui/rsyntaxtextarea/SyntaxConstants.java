/*
 * 03/08/2004
 *
 * SyntaxConstants.java - Constants used by RSyntaxTextArea and friends.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;


/**
 * Constants that define the different programming languages understood by
 * <code>RSyntaxTextArea</code>.  These constants are the values you can pass
 * to {@link RSyntaxTextArea#setSyntaxEditingStyle(String)} to get syntax
 * highlighting.<p>
 *
 * By default, all <code>RSyntaxTextArea</code>s can render all of these
 * languages, but this can be changed (the list can be augmented or completely
 * overwritten) on a per-text area basis.  What languages can be rendered is
 * actually managed by the {@link TokenMakerFactory} installed on the text
 * area's {@link RSyntaxDocument}.  By default, all
 * <code>RSyntaxDocumenet</code>s have a factory installed capable of handling
 * all of these languages.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public interface SyntaxConstants {

	/**
	 * Style meaning don't syntax highlight anything.
	 */
	public static final String SYNTAX_STYLE_NONE			= "text/plain";


	/**
	 * Style for highlighting ActionScript.
	 */
	public static final String SYNTAX_STYLE_ACTIONSCRIPT	= "text/actionscript";


	/**
	 * Style for highlighting x86 assembler.
	 */
	public static final String SYNTAX_STYLE_ASSEMBLER_X86	= "text/asm";


	/**
	 * Style for highlighting BBCode.
	 */
	public static final String SYNTAX_STYLE_BBCODE			= "text/bbcode";


	/**
	 * Style for highlighting C.
	 */
	public static final String SYNTAX_STYLE_C				= "text/c";


	/**
	 * Style for highlighting Clojure.
	 */
	public static final String SYNTAX_STYLE_CLOJURE			= "text/clojure";


	/**
	 * Style for highlighting C++.
	 */
	public static final String SYNTAX_STYLE_CPLUSPLUS		= "text/cpp";


	/**
	 * Style for highlighting C#.
	 */
	public static final String SYNTAX_STYLE_CSHARP			= "text/cs";


	/**
	 * Style for highlighting CSS.
	 */
	public static final String SYNTAX_STYLE_CSS			= "text/css";


	/**
	 * Style for highlighting Delphi/Pascal.
	 */
	public static final String SYNTAX_STYLE_DELPHI			= "text/delphi";


	/**
	 * Style for highlighting DTD files.
	 */
	public static final String SYNTAX_STYLE_DTD			= "text/dtd";


	/**
	 * Style for highlighting Fortran.
	 */
	public static final String SYNTAX_STYLE_FORTRAN			= "text/fortran";


	/**
	 * Style for highlighting Groovy.
	 */
	public static final String SYNTAX_STYLE_GROOVY			= "text/groovy";


	/**
	 * Style for highlighting HTML.
	 */
	public static final String SYNTAX_STYLE_HTML			= "text/html";


	/**
	 * Style for highlighting Java.
	 */
	public static final String SYNTAX_STYLE_JAVA			= "text/java";


	/**
	 * Style for highlighting JavaScript.
	 */
	public static final String SYNTAX_STYLE_JAVASCRIPT		= "text/javascript";


	/**
	 * Style for highlighting JSON.
	 */
	public static final String SYNTAX_STYLE_JSON		= "text/json";


	/**
	 * Style for highlighting JSP.
	 */
	public static final String SYNTAX_STYLE_JSP			= "text/jsp";


	/**
	 * Style for highlighting LaTeX.
	 */
	public static final String SYNTAX_STYLE_LATEX		= "text/latex";


	/**
	 * Style for highlighting Lisp.
	 */
	public static final String SYNTAX_STYLE_LISP		= "text/lisp";


	/**
	 * Style for highlighting Lua.
	 */
	public static final String SYNTAX_STYLE_LUA			= "text/lua";


	/**
	 * Style for highlighting makefiles.
	 */
	public static final String SYNTAX_STYLE_MAKEFILE		= "text/makefile";


	/**
	 * Style for highlighting MXML.
	 */
	public static final String SYNTAX_STYLE_MXML			= "text/mxml";


	/**
	 * Style for highlighting NSIS install scripts.
	 */
	public static final String SYNTAX_STYLE_NSIS			= "text/nsis";


	/**
	 * Style for highlighting Perl.
	 */
	public static final String SYNTAX_STYLE_PERL			= "text/perl";


	/**
	 * Style for highlighting PHP.
	 */
	public static final String SYNTAX_STYLE_PHP				= "text/php";


	/**
	 * Style for highlighting properties files.
	 */
	public static final String SYNTAX_STYLE_PROPERTIES_FILE	= "text/properties";


	/**
	 * Style for highlighting Python.
	 */
	public static final String SYNTAX_STYLE_PYTHON			= "text/python";


	/**
	 * Style for highlighting Ruby.
	 */
	public static final String SYNTAX_STYLE_RUBY			= "text/ruby";


	/**
	 * Style for highlighting SAS keywords.
	 */
	public static final String SYNTAX_STYLE_SAS			= "text/sas";


	/**
	 * Style for highlighting Scala.
	 */
	public static final String SYNTAX_STYLE_SCALA		= "text/scala";


	/**
	 * Style for highlighting SQL.
	 */
	public static final String SYNTAX_STYLE_SQL			= "text/sql";


	/**
	 * Style for highlighting Tcl.
	 */
	public static final String SYNTAX_STYLE_TCL			= "text/tcl";


	/**
	 * Style for highlighting UNIX shell keywords.
	 */
	public static final String SYNTAX_STYLE_UNIX_SHELL		= "text/unix";


	/**
	 * Style for highlighting Windows batch files.
	 */
	public static final String SYNTAX_STYLE_WINDOWS_BATCH	= "text/bat";


	/**
	 * Style for highlighting XML.
	 */
	public static final String SYNTAX_STYLE_XML			= "text/xml";


}