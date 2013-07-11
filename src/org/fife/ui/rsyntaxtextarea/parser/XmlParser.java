/*
 * 08/16/2008
 *
 * XMLParser.java - Simple XML parser.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.parser;

import java.io.IOException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import org.fife.io.DocumentReader;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;


/**
 * A parser for XML documents.  Adds squiggle underlines for any XML errors
 * found (though most XML parsers don't really have error recovery and so only
 * can find one error at a time).<p>
 *
 * This class isn't actually used by RSyntaxTextArea anywhere, but you can
 * install and use it yourself.  Doing so is as simple as:
 * 
 * <pre>
 * XmlParser xmlParser = new XmlParser();
 * textArea.addParser(xmlParser);
 * </pre>
 * 
 * To support DTD validation, specify an entity resolver when creating the
 * parser, and enable validation like so:
 * 
 * <pre>
 * XmlParser xmlParser = new XmlParser(new MyEntityResolver());
 * xmlParser.setValidating(true);
 * textArea.addParser(xmlParser);
 * </pre>
 * 
 * Also note that a single instance of this class can be installed on
 * multiple instances of <code>RSyntaxTextArea</code>.<p>
 *
 * For a more complete XML parsing solution, see the
 * <a href="http://svn.fifesoft.com/viewvc-1.0.5/bin/cgi/viewvc.cgi/RSTALanguageSupport/trunk/?root=RSyntaxTextArea">RSTALanguageSupport
 * project</a>'s <code>XmlLanguageSupport</code> class.
 * 
 * @author Robert Futrell
 * @version 1.1
 */
public class XmlParser extends AbstractParser {

	private SAXParserFactory spf;
	private DefaultParseResult result;
	private EntityResolver entityResolver;


	public XmlParser() {
		this(null);
	}


	/**
	 * Constructor allowing DTD validation of documents.
	 * 
	 * @param resolver An entity resolver to use if validation is enabled.
	 * @see #setValidating(boolean)
	 */
	public XmlParser(EntityResolver resolver) {
		this.entityResolver = resolver;
		result = new DefaultParseResult(this);
		try {
			spf = SAXParserFactory.newInstance();
		} catch (FactoryConfigurationError fce) {
			fce.printStackTrace();
		}
	}


	/**
	 * Returns whether this parser does DTD validation.
	 *
	 * @return Whether this parser does DTD validation.
	 * @see #setValidating(boolean)
	 */
	public boolean isValidating() {
		return spf.isValidating();
	}


	/**
	 * {@inheritDoc}
	 */
	public ParseResult parse(RSyntaxDocument doc, String style) {

		result.clearNotices();
		Element root = doc.getDefaultRootElement();
		result.setParsedLines(0, root.getElementCount()-1);

		if (spf==null || doc.getLength()==0) {
			return result;
		}

		try {
			SAXParser sp = spf.newSAXParser();
			Handler handler = new Handler(doc);
			DocumentReader r = new DocumentReader(doc);
			InputSource input = new InputSource(r);
			sp.parse(input, handler);
			r.close();
		} catch (SAXParseException spe) {
			// A fatal parse error - ignore; a ParserNotice was already created.
		} catch (Exception e) {
			//e.printStackTrace(); // Will print if DTD specified and can't be found
			result.addNotice(new DefaultParserNotice(this,
					"Error parsing XML: " + e.getMessage(), 0, -1, -1));
		}

		return result;

	}


	/**
	 * Sets whether this parser will use DTD validation if required.
	 *
	 * @param validating Whether DTD validation should be enabled.  If this is
	 *        <code>true</code>, documents must specify a DOCTYPE, and you
	 *        should have used the constructor specifying an entity resolver.
	 * @see #isValidating()
	 */
	public void setValidating(boolean validating) {
		spf.setValidating(validating);
	}

/*
	public static void main(String[] args) {
		javax.swing.JFrame frame = new javax.swing.JFrame();
		org.fife.ui.rsyntaxtextarea.RSyntaxTextArea textArea = new
			org.fife.ui.rsyntaxtextarea.RSyntaxTextArea(25, 40);
		textArea.setSyntaxEditingStyle("text/xml");
		XmlParser parser = new XmlParser(new EntityResolver() {
			public InputSource resolveEntity(String publicId, String systemId)
					throws SAXException, IOException {
		    	if ("http://fifesoft.com/rsyntaxtextarea/theme.dtd".equals(systemId)) {
		    		return new org.xml.sax.InputSource(getClass().getResourceAsStream("/theme.dtd"));
		    	}
		    	return null;
			}
		});
		parser.setValidating(true);
		textArea.addParser(parser);
		try {
			textArea.read(new java.io.BufferedReader(new java.io.FileReader("C:/temp/test.xml")), null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		frame.setContentPane(new org.fife.ui.rtextarea.RTextScrollPane(textArea));
		frame.pack();
		frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
*/

	/**
	 * Callback notified when errors are found in the XML document.  Adds a
	 * notice to be squiggle-underlined.
	 */
	private class Handler extends DefaultHandler {

		private Document doc;

		private Handler(Document doc) {
			this.doc = doc;
		}

		private void doError(SAXParseException e, int level) {
			int line = e.getLineNumber() - 1;
			Element root = doc.getDefaultRootElement();
			Element elem = root.getElement(line);
			int offs = elem.getStartOffset();
			int len = elem.getEndOffset() - offs;
			if (line==root.getElementCount()-1) {
				len++;
			}
			DefaultParserNotice pn = new DefaultParserNotice(XmlParser.this,
											e.getMessage(), line, offs, len);
			pn.setLevel(level);
			result.addNotice(pn);
		}

		public void error(SAXParseException e) {
			doError(e, ParserNotice.ERROR);
		}

		public void fatalError(SAXParseException e) {
			doError(e, ParserNotice.ERROR);
		}

		// NOTE: If you compile with Java 5+, you must add IOException to the
		// throws clause of this method.  The "official" release is built with
		// Java 1.4.
		public InputSource resolveEntity(String publicId, String systemId)
								throws SAXException, IOException {
			if (entityResolver!=null) {
				try {
					return entityResolver.resolveEntity(publicId, systemId);
				} catch (IOException ioe) {
					// TODO: Remove when removing 1.4.2-compatibility, as
					// IOExceptions are thrown then
					ioe.printStackTrace();
				}
			}
			return super.resolveEntity(publicId, systemId);
		}

		public void warning(SAXParseException e) {
			doError(e, ParserNotice.WARNING);
		}

	}


}