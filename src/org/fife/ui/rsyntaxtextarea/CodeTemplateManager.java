/*
 * 02/21/2005
 *
 * CodeTemplateManager.java - manages code templates.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.templates.CodeTemplate;


/**
 * Manages "code templates."<p>
 *
 * All methods in this class are synchronized for thread safety, but as a
 * best practice, you should probably only modify the templates known to a
 * <code>CodeTemplateManager</code> on the EDT.  Modifying a
 * <code>CodeTemplate</code> retrieved from a <code>CodeTemplateManager</code>
 * while <em>not</em> on the EDT could cause problems.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class CodeTemplateManager {

	private int maxTemplateIDLength;
	private List templates;

	private KeyStroke insertTrigger;
	private String insertTriggerString;
	private Segment s;
	private TemplateComparator comparator;
	private File directory;

	private static final int mask = InputEvent.CTRL_MASK|InputEvent.SHIFT_MASK;
	static final KeyStroke TEMPLATE_KEYSTROKE = KeyStroke.
								getKeyStroke(KeyEvent.VK_SPACE, mask);


	/**
	 * Constructor.
	 */
	public CodeTemplateManager() {

		// Default insert trigger is a space.
		// FIXME:  See notes in RSyntaxTextAreaDefaultInputMap.
		setInsertTrigger(TEMPLATE_KEYSTROKE);

		s = new Segment();
		comparator = new TemplateComparator();
		templates = new ArrayList();

	}


	/**
	 * Registers the specified template with this template manager.
	 *
	 * @param template The template to register.
	 * @throws IllegalArgumentException If <code>template</code> is
	 *         <code>null</code>.
	 * @see #removeTemplate(CodeTemplate)
	 * @see #removeTemplate(String)
	 */
	public synchronized void addTemplate(CodeTemplate template) {
		if (template==null) {
			throw new IllegalArgumentException("template cannot be null");
		}
		templates.add(template);
		sortTemplates();
	}


	/**
	 * Returns the keystroke that is the "insert trigger" for templates;
	 * that is, the character that, when inserted into an instance of
	 * <code>RSyntaxTextArea</code>, triggers the search for
	 * a template matching the token ending at the caret position.
	 *
	 * @return The insert trigger.
	 * @see #getInsertTriggerString()
	 * @see #setInsertTrigger(KeyStroke)
	 */
	/*
	 * FIXME:  This text IS what's inserted if the trigger character is pressed
	 * in a text area but no template matches, but it is NOT the trigger
	 * character used in the text areas.  This is because space (" ") is
	 * hard-coded into RSyntaxTextAreaDefaultInputMap.java.  We need to make
	 * this dynamic somehow.  See RSyntaxTextAreaDefaultInputMap.java.
	 */
	public KeyStroke getInsertTrigger() {
		return insertTrigger;
	}


	/**
	 * Returns the "insert trigger" for templates; that is, the character
	 * that, when inserted into an instance of <code>RSyntaxTextArea</code>,
	 * triggers the search for a template matching the token ending at the
	 * caret position.
	 *
	 * @return The insert trigger character.
	 * @see #getInsertTrigger()
	 * @see #setInsertTrigger(KeyStroke)
	 */
	/*
	 * FIXME:  This text IS what's inserted if the trigger character is pressed
	 * in a text area but no template matches, but it is NOT the trigger
	 * character used in the text areas.  This is because space (" ") is
	 * hard-coded into RSyntaxTextAreaDefaultInputMap.java.  We need to make
	 * this dynamic somehow.  See RSyntaxTextAreaDefaultInputMap.java.
	 */
	public String getInsertTriggerString() {
		return insertTriggerString;
	}


	/**
	 * Returns the template that should be inserted at the current caret
	 * position, assuming the trigger character was pressed.
	 *
	 * @param textArea The text area that's getting text inserted into it.
	 * @return A template that should be inserted, if appropriate, or
	 *         <code>null</code> if no template should be inserted.
	 */
	public synchronized CodeTemplate getTemplate(RSyntaxTextArea textArea) {
		int caretPos = textArea.getCaretPosition();
		int charsToGet = Math.min(caretPos, maxTemplateIDLength);
		try {
			Document doc = textArea.getDocument();
			doc.getText(caretPos-charsToGet, charsToGet, s);
			int index = Collections.binarySearch(templates, s, comparator);
			return index>=0 ? (CodeTemplate)templates.get(index) : null;
		} catch (BadLocationException ble) {
			ble.printStackTrace();
			throw new InternalError("Error in CodeTemplateManager");
		}
	}


	/**
	 * Returns the number of templates this manager knows about.
	 *
	 * @return The template count.
	 */
	public synchronized int getTemplateCount() {
		return templates.size();
	}


	/**
	 * Returns the templates currently available.
	 *
	 * @return The templates available.
	 */
	public synchronized CodeTemplate[] getTemplates() {
		CodeTemplate[] temp = new CodeTemplate[templates.size()];
		return (CodeTemplate[])templates.toArray(temp);
	}


	/**
	 * Returns whether the specified character is a valid character for a
	 * <code>CodeTemplate</code> id.
	 *
	 * @param ch The character to check.
	 * @return Whether the character is a valid template character.
	 */
	public static final boolean isValidChar(char ch) {
		return RSyntaxUtilities.isLetterOrDigit(ch) || ch=='_';
	}


	/**
	 * Returns the specified code template.
	 *
	 * @param template The template to remove.
	 * @return <code>true</code> if the template was removed, <code>false</code>
	 *         if the template was not in this template manager.
	 * @throws IllegalArgumentException If <code>template</code> is
	 *         <code>null</code>.
	 * @see #removeTemplate(String)
	 * @see #addTemplate(CodeTemplate)
	 */
	public synchronized boolean removeTemplate(CodeTemplate template) {

		if (template==null) {
			throw new IllegalArgumentException("template cannot be null");
		}

		// TODO: Do a binary search
		return templates.remove(template);

	}


	/**
	 * Returns the code template with the specified id.
	 *
	 * @param id The id to check for.
	 * @return The code template that was removed, or <code>null</code> if
	 *         there was no template with the specified ID.
	 * @throws IllegalArgumentException If <code>id</code> is <code>null</code>.
	 * @see #removeTemplate(CodeTemplate)
	 * @see #addTemplate(CodeTemplate)
	 */
	public synchronized CodeTemplate removeTemplate(String id) {

		if (id==null) {
			throw new IllegalArgumentException("id cannot be null");
		}

		// TODO: Do a binary search
		for (Iterator i=templates.iterator(); i.hasNext(); ) {
			CodeTemplate template = (CodeTemplate)i.next();
			if (id.equals(template.getID())) {
				i.remove();
				return template;
			}
		}

		return null;

	}


	/**
	 * Replaces the current set of available templates with the ones
	 * specified.
	 *
	 * @param newTemplates The new set of templates.  Note that we will
	 *        be taking a shallow copy of these and sorting them.
	 */
	public synchronized void replaceTemplates(CodeTemplate[] newTemplates) {
		templates.clear();
		if (newTemplates!=null) {
			for (int i=0; i<newTemplates.length; i++) {
				templates.add(newTemplates[i]);
			}
		}
		sortTemplates(); // Also recomputes maxTemplateIDLength.
	}


	/**
	 * Saves all templates as XML files in the current template directory.
	 *
	 * @return Whether or not the save was successful.
	 */
	public synchronized boolean saveTemplates() {

		if (templates==null)
			return true;
		if (directory==null || !directory.isDirectory())
			return false;

		// Blow away all old XML files to start anew, as some might be from
		// templates we're removed from the template manager.
		File[] oldXMLFiles = directory.listFiles(new XMLFileFilter());
		if (oldXMLFiles==null)
			return false; // Either an IOException or it isn't a directory.
		int count = oldXMLFiles.length;
		for (int i=0; i<count; i++) {
			/*boolean deleted = */oldXMLFiles[i].delete();
		}

		// Save all current templates as XML.
		boolean wasSuccessful = true;
		for (Iterator i=templates.iterator(); i.hasNext(); ) {
			CodeTemplate template = (CodeTemplate)i.next();
			File xmlFile = new File(directory, template.getID() + ".xml");
			try {
				XMLEncoder e = new XMLEncoder(new BufferedOutputStream(
										new FileOutputStream(xmlFile)));
				e.writeObject(template);
				e.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				wasSuccessful = false;
			}
		}

		return wasSuccessful;

	}


	/**
	 * Sets the "trigger" character for templates.
	 *
	 * @param trigger The trigger character to set for templates.  This means
	 *        that when this character is pressed in an
	 *        <code>RSyntaxTextArea</code>,  the last-typed token is found,
	 *        and is checked against all template ID's to see if a template
	 *        should be inserted.  If a template ID matches, that template is
	 *        inserted; if not, the trigger character is inserted.  If this
	 *        parameter is <code>null</code>, no change is made to the trigger
	 *        character.
	 * @see #getInsertTrigger()
	 * @see #getInsertTriggerString()
	 */
	/*
	 * FIXME:  The trigger set here IS inserted when no matching template
	 * is found, but a space character (" ") is always used as the "trigger"
	 * to look for templates.  This is because it is hard-coded in
	 * RSyntaxTextArea's input map this way.  We need to change this.
	 * See RSyntaxTextAreaDefaultInputMap.java.
	 */
	public void setInsertTrigger(KeyStroke trigger) {
		if (trigger!=null) {
			insertTrigger = trigger;
			insertTriggerString = Character.toString(trigger.getKeyChar());
		}
	}


	/**
	 * Sets the directory in which to look for templates.  Calling this
	 * method adds any new templates found in the specified directory to
	 * the templates already registered.
	 *
	 * @param dir The new directory in which to look for templates.
	 * @return The new number of templates in this template manager, or
	 *         <code>-1</code> if the specified directory does not exist.
	 */
	public synchronized int setTemplateDirectory(File dir) {

		if (dir!=null && dir.isDirectory()) {

			this.directory = dir;

			File[] files = dir.listFiles(new XMLFileFilter());
			int newCount = files==null ? 0 : files.length;
			int oldCount = templates.size();

			List temp = new ArrayList(oldCount+newCount);
			temp.addAll(templates);

			for (int i=0; i<newCount; i++) {
				try {
					XMLDecoder d = new XMLDecoder(new BufferedInputStream(
						new FileInputStream(files[i])));
					Object obj = d.readObject();
					if (!(obj instanceof CodeTemplate)) {
						throw new IOException("Not a CodeTemplate: " +
										files[i].getAbsolutePath());
					}
					temp.add(obj);
					d.close();
				} catch (/*IO, NoSuchElement*/Exception e) {
					// NoSuchElementException can be thrown when reading
					// an XML file not in the format expected by XMLDecoder.
					// (e.g. CodeTemplates in an old format).
					e.printStackTrace();
				}
			}
			templates = temp;
			sortTemplates();

			return getTemplateCount();

		}

		return -1;

	}


	/**
	 * Removes any null entries in the current set of templates (if
	 * any), sorts the remaining templates, and computes the new
	 * maximum template ID length.
	 */
	private synchronized void sortTemplates() {

		// Get the maximum length of a template ID.
		maxTemplateIDLength = 0;

		// Remove any null entries (should only happen because of
		// IOExceptions, etc. when loading from files), and sort
		// the remaining list.
		for (Iterator i=templates.iterator(); i.hasNext(); ) {
			CodeTemplate temp = (CodeTemplate)i.next();
			if (temp==null || temp.getID()==null) {
				i.remove();
			}
			else {
				maxTemplateIDLength = Math.max(maxTemplateIDLength,
										temp.getID().length());
			}
		}

		Collections.sort(templates);

	}


	/**
	 * A comparator that takes a <code>CodeTemplate</code> as its first
	 * parameter and a <code>Segment</code> as its second, and knows
	 * to compare the template's ID to the segment's text.
	 */
	private static class TemplateComparator implements Comparator, Serializable{

		public int compare(Object template, Object segment) {

			// Get template start index (0) and length.
			CodeTemplate t = (CodeTemplate)template;
			final char[] templateArray = t.getID().toCharArray();
			int i = 0;
			int len1 = templateArray.length;

			// Find "token" part of segment and get its offset and length.
			Segment s = (Segment)segment;
			char[] segArray = s.array;
			int len2 = s.count;
			int j = s.offset + len2 - 1;
			while (j>=s.offset && isValidChar(segArray[j])) {
				j--;
			}
			j++;
			int segShift = j - s.offset;
			len2 -= segShift;

			int n = Math.min(len1, len2);
			while (n-- != 0) {
				char c1 = templateArray[i++];
				char c2 = segArray[j++];
				if (c1 != c2)
					return c1 - c2;
			}
			return len1 - len2;

		}

	}


	/**
	 * A file filter for File.listFiles() (NOT for JFileChoosers!) that
	 * accepts only XML files.
	 */
	private static class XMLFileFilter implements FileFilter {
		public boolean accept(File f) {
			return f.getName().toLowerCase().endsWith(".xml");
		}
	}


}