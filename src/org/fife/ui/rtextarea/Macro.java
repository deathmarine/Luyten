/*
 * 09/16/2004
 *
 * Macro.java - A macro as recorded/played back by an RTextArea.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import org.fife.io.UnicodeReader;


/**
 * A macro as recorded/played back by an <code>RTextArea</code>.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class Macro {

	private String name;
	private ArrayList macroRecords;

	private static final String ROOT_ELEMENT			= "macro";
	private static final String MACRO_NAME				= "macroName";
	private static final String ACTION					= "action";
	private static final String ID					= "id";

	private static final String UNTITLED_MACRO_NAME		= "<Untitled>";

	private static final String FILE_ENCODING			= "UTF-8";


	/**
	 * Constructor.
	 */
	public Macro() {
		this(UNTITLED_MACRO_NAME);
	}


	/**
	 * Loads a macro from a file on disk.
	 *
	 * @param file The file from which to load the macro.
	 * @throws java.io.EOFException If an EOF is reached unexpectedly (i.e.,
	 *         the file is corrupt).
	 * @throws FileNotFoundException If the specified file does not exist, is
	 *         a directory instead of a regular file, or otherwise cannot be
	 *         opened.
	 * @throws IOException If an I/O exception occurs while reading the file.
	 * @see #saveToFile
	 */
	public Macro(File file) throws EOFException, FileNotFoundException,
								IOException {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document doc = null;
		try {
			db = dbf.newDocumentBuilder();
			//InputSource is = new InputSource(new FileReader(file));
			InputSource is = new InputSource(new UnicodeReader(
								new FileInputStream(file), FILE_ENCODING));
			is.setEncoding(FILE_ENCODING);
			doc = db.parse(is);//db.parse(file);
		} catch (Exception e) {
			e.printStackTrace();
			String desc = e.getMessage();
			if (desc==null) {
				desc = e.toString();
			}
			throw new IOException("Error parsing XML: " + desc);
		}

		macroRecords = new ArrayList();

		// Traverse the XML tree.
		boolean parsedOK = initializeFromXMLFile(doc.getDocumentElement());
		if (parsedOK==false) {
			name = null;
			macroRecords.clear();
			macroRecords = null;
			throw new IOException("Error parsing XML!");
		}

	}


	/**
	 * Constructor.
	 *
	 * @param name The name of the macro.
	 */
	public Macro(String name) {
		this(name, null);
	}


	/**
	 * Constructor.
	 *
	 * @param name The name of the macro.
	 * @param records The initial records of the macro.
	 */
	public Macro(String name, List records) {
		
		this.name = name;

		if (records!=null) {
			macroRecords = new ArrayList(records.size());
			Iterator i = records.iterator();
			while (i.hasNext()) {
				MacroRecord record = (MacroRecord)i.next();
				macroRecords.add(record);
			}
		}
		else {
			macroRecords = new ArrayList(10);
		}
	
	}


	/**
	 * Adds a macro record to this macro.
	 *
	 * @param record The record to add.  If <code>null</code>, nothing happens.
	 * @see #getMacroRecords
	 */
	public void addMacroRecord(MacroRecord record) {
		if (record!=null)
			macroRecords.add(record);
	}


	/**
	 * Returns the macro records that make up this macro.
	 *
	 * @return The macro records.
	 * @see #addMacroRecord
	 */
	public List getMacroRecords() {
		return macroRecords;
	}


	/**
	 * Returns the name of this macro.
	 *
	 * @return The macro's name.
	 * @see #setName
	 */
	public String getName() {
		return name;
	}


	/**
	 * Used in parsing an XML document containing a macro.  This method
	 * initializes this macro with the data contained in the passed-in node.
	 *
	 * @param node The root node of the parsed XML document.
	 * @return <code>true</code> if the macro initialization went okay;
	 *         <code>false</code> if an error occurred.
	 */
	private boolean initializeFromXMLFile(Element root) {

		/*
		 * This method expects the XML document to be in the following format:
		 *
		 * <?xml version="1.0" encoding="UTF-8" ?>
		 * <macro>
		 *    <macroName>test</macroName>
		 *    <action id="default-typed">abcdefg</action>
		 *    [<action id=...>...</action>]
		 *    ...
		 * </macro>
		 *
		 */

		NodeList childNodes = root.getChildNodes();
		int count = childNodes.getLength();

		for (int i=0; i<count; i++) {

			Node node = childNodes.item(i);
			int type = node.getNodeType();
			switch (type) {

				// Handle element nodes.
				case Node.ELEMENT_NODE:

					String nodeName = node.getNodeName();

					if (nodeName.equals(MACRO_NAME)) {
						NodeList childNodes2 = node.getChildNodes();
						name = UNTITLED_MACRO_NAME;
						if (childNodes2.getLength()>0) {
							node = childNodes2.item(0);
							int type2 = node.getNodeType();
							if (type2!=Node.CDATA_SECTION_NODE &&
									type2!=Node.TEXT_NODE) {
								return false;
							}
							name = node.getNodeValue().trim();
						}
						//System.err.println("Macro name==" + name);
					}

					else if (nodeName.equals(ACTION)) {
						NamedNodeMap attributes = node.getAttributes();
						if (attributes==null || attributes.getLength()!=1)
							return false;
						Node node2 = attributes.item(0);
						MacroRecord macroRecord = new MacroRecord();
						if (!node2.getNodeName().equals(ID)) {
							return false;
						}
						macroRecord.id = node2.getNodeValue();
						NodeList childNodes2 = node.getChildNodes();
						int length = childNodes2.getLength();
						if (length==0) { // Could be empty "" command.
							//System.err.println("... empty actionCommand");
							macroRecord.actionCommand = "";
							//System.err.println("... adding action: " + macroRecord);
							macroRecords.add(macroRecord);
							break;
						}
						else {
							node = childNodes2.item(0);
							int type2 = node.getNodeType();
							if (type2!=Node.CDATA_SECTION_NODE &&
									type2!=Node.TEXT_NODE) {
								return false;
							}
							macroRecord.actionCommand = node.getNodeValue();
							macroRecords.add(macroRecord);
						}

					}
					break;

				default:
					break; // Skip whitespace nodes, etc.

			}

		}

		// Everything went okay.
		return true;

	}


	/**
	 * Saves this macro to a text file.  This file can later be read in by
	 * the constructor taking a <code>File</code> parameter; this is the
	 * mechanism for saving macros.
	 *
	 * @param fileName The name of the file in which to save the macro.
	 * @throws IOException If an error occurs while generating the XML for
	 *         the output file.
	 */
	public void saveToFile(String fileName) throws IOException {

		/*
		 * This method writes the XML document in the following format:
		 *
		 * <?xml version="1.0" encoding="UTF-8" ?>
		 * <macro>
		 *    <macroName>test</macroName>
		 *    <action id="default-typed">abcdefg</action>
		 *    [<action id=...>...</action>]
		 *    ...
		 * </macro>
		 *
		 */

		try {

			DocumentBuilder db = DocumentBuilderFactory.newInstance().
											newDocumentBuilder();
			DOMImplementation impl = db.getDOMImplementation();

			Document doc = impl.createDocument(null, ROOT_ELEMENT, null);
			Element rootElement = doc.getDocumentElement();

			// Write the name of the macro.
			Element nameElement = doc.createElement(MACRO_NAME);
			rootElement.appendChild(nameElement);

			// Write all actions (the meat) in the macro.
			int numActions = macroRecords.size();
			for (int i=0; i<numActions; i++) {
				MacroRecord record = (MacroRecord)macroRecords.get(i);
				Element actionElement = doc.createElement(ACTION);
				actionElement.setAttribute(ID, record.id);
				if (record.actionCommand!=null &&
						record.actionCommand.length()>0) {
					// Remove illegal characters.  I'm no XML expert, but
					// I'm not sure what I'm doing wrong.  If we don't
					// strip out chars with Unicode value < 32, our
					// generator will insert '&#<value>', which will cause
					// our parser to barf when reading the macro back in
					// (it says "Invalid XML character").  But why doesn't
					// our generator tell us the character is invalid too?
					String command = record.actionCommand;
					for (int j=0; j<command.length(); j++) {
						if (command.charAt(j)<32) {
							command = command.substring(0,j);
							if (j<command.length()-1)
								command += command.substring(j+1);
						}
					}
					Node n = doc.createCDATASection(command);
					actionElement.appendChild(n);
				}
				rootElement.appendChild(actionElement);
			}

			// Dump the XML out to the file.
			StreamResult result = new StreamResult(new File(fileName));
			DOMSource source = new DOMSource(doc);
			TransformerFactory transFac = TransformerFactory.newInstance();
			Transformer transformer = transFac.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, FILE_ENCODING);
			transformer.transform(source, result);

		} catch (RuntimeException re) {
			throw re; // Keep FindBugs happy.
		} catch (Exception e) {
			throw new IOException("Error generating XML!");
		}

	}


	/**
	 * Sets the name of this macro.
	 *
	 * @param name The new name for the macro.
	 * @see #getName
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * A "record" of a macro is a single action in the macro (corresponding to
	 * a key type and some action in the editor, such as a letter inserted into
	 * the document, scrolling one page down, selecting the current line,
	 * etc.).
	 */
	static class MacroRecord {

		public String id;
		public String actionCommand;

		public MacroRecord() {
			this(null, null);
		}

		public MacroRecord(String id, String actionCommand) {
			this.id = id;
			this.actionCommand = actionCommand;
		}

	}


}