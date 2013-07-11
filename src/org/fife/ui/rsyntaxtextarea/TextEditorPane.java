/*
 * 11/25/2008
 *
 * TextEditorPane.java - A syntax highlighting text area that has knowledge of
 * the file it is editing on disk.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.fife.io.UnicodeReader;
import org.fife.io.UnicodeWriter;
import org.fife.ui.rtextarea.RTextAreaEditorKit;


/**
 * An extension of {@link org.fife.ui.rsyntaxtextarea.RSyntaxTextArea}
 * that adds information about the file being edited, such as:
 *
 * <ul>
 *   <li>Its name and location.
 *   <li>Is it dirty?
 *   <li>Is it read-only?
 *   <li>The last time it was loaded or saved to disk (local files only).
 *   <li>The file's encoding on disk.
 *   <li>Easy access to the line separator.
 * </ul>
 *
 * Loading and saving is also built into the editor.<p>
 * Both local and remote files (e.g. ftp) are supported.  See the
 * {@link FileLocation} class for more information.
 *
 * @author Robert Futrell
 * @version 1.0
 * @see FileLocation
 */
public class TextEditorPane extends RSyntaxTextArea implements
									DocumentListener {

	private static final long serialVersionUID = 1L;

	public static final String FULL_PATH_PROPERTY	= "TextEditorPane.fileFullPath";
	public static final String DIRTY_PROPERTY	= "TextEditorPane.dirty";
	public static final String READ_ONLY_PROPERTY	= "TextEditorPane.readOnly";

	/**
	 * The location of the file being edited.
	 */
	private FileLocation loc;

	/**
	 * The charset to use when reading or writing this file.
	 */
	private String charSet;

	/**
	 * Whether the file should be treated as read-only.
	 */
	private boolean readOnly;

	/**
	 * Whether the file is dirty.
	 */
	private boolean dirty;

	/**
	 * The last time this file was modified on disk, for local files.
	 * For remote files, this value should always be
	 * {@link #LAST_MODIFIED_UNKNOWN}.
	 */
	private long lastSaveOrLoadTime;

	/**
	 * The value returned by {@link #getLastSaveOrLoadTime()} for remote files.
	 */
	public static final long LAST_MODIFIED_UNKNOWN		= 0;

	/**
	 * The default name given to files if none is specified in a constructor.
	 */
	private static final String DEFAULT_FILE_NAME = "Untitled.txt";


	/**
	 * Constructor.  The file will be given a default name.
	 */
	public TextEditorPane() {
		this(INSERT_MODE);
	}


	/**
	 * Constructor.  The file will be given a default name.
	 *
	 * @param textMode Either <code>INSERT_MODE</code> or
	 *        <code>OVERWRITE_MODE</code>.
	 */
	public TextEditorPane(int textMode) {
		this(textMode, false);
	}


	/**
	 * Creates a new <code>TextEditorPane</code>.  The file will be given
	 * a default name.
	 *
	 * @param textMode Either <code>INSERT_MODE</code> or
	 *        <code>OVERWRITE_MODE</code>.
	 * @param wordWrapEnabled Whether or not to use word wrap in this pane.
	 */
	public TextEditorPane(int textMode, boolean wordWrapEnabled) {
		super(textMode);
		setLineWrap(wordWrapEnabled);
		try {
			init(null, null);
		} catch (IOException ioe) { // Never happens
			ioe.printStackTrace();
		}
	}


	/**
	 * Creates a new <code>TextEditorPane</code>.
	 *
	 * @param textMode Either <code>INSERT_MODE</code> or
	 *        <code>OVERWRITE_MODE</code>.
	 * @param wordWrapEnabled Whether or not to use word wrap in this pane.
	 * @param loc The location of the text file being edited.  If this value
	 *        is <code>null</code>, a file named "Untitled.txt" in the current
	 *        directory is used.
	 * @throws IOException If an IO error occurs reading the file at
	 *         <code>loc</code>.  This of course won't happen if
	 *         <code>loc</code> is <code>null</code>.
	 */
	public TextEditorPane(int textMode, boolean wordWrapEnabled,
							FileLocation loc) throws IOException {
		this(textMode, wordWrapEnabled, loc, null);
	}


	/**
	 * Creates a new <code>TextEditorPane</code>.
	 *
	 * @param textMode Either <code>INSERT_MODE</code> or
	 *        <code>OVERWRITE_MODE</code>.
	 * @param wordWrapEnabled Whether or not to use word wrap in this pane.
	 * @param loc The location of the text file being edited.  If this value
	 *        is <code>null</code>, a file named "Untitled.txt" in the current
	 *        directory is used.  This file is displayed as empty even if it
	 *        actually exists.
	 * @param defaultEnc The default encoding to use when opening the file,
	 *        if the file is not Unicode.  If this value is <code>null</code>,
	 *        a system default value is used.
	 * @throws IOException If an IO error occurs reading the file at
	 *         <code>loc</code>.  This of course won't happen if
	 *         <code>loc</code> is <code>null</code>.
	 */
	public TextEditorPane(int textMode, boolean wordWrapEnabled,
				FileLocation loc, String defaultEnc) throws IOException {
		super(textMode);
		setLineWrap(wordWrapEnabled);
		init(loc, defaultEnc);
	}


	/**
	 * Callback for when styles in the current document change.
	 * This method is never called.
	 *
	 * @param e The document event.
	 */
	public void changedUpdate(DocumentEvent e) {
	}



	/**
	 * Returns the default encoding for this operating system.
	 *
	 * @return The default encoding.
	 */
	private static final String getDefaultEncoding() {
		// TODO: Change to "Charset.defaultCharset().name()" when 1.4 support
		// is no longer needed.
		// NOTE:  The "file.encoding" property is not guaranteed to be set by
		// the spec, so we cannot rely on it.
		String encoding = System.getProperty("file.encoding");
		if (encoding==null) {
			try {
				File f = File.createTempFile("rsta", null);
				FileWriter w = new FileWriter(f);
				encoding = w.getEncoding();
				w.close();
				f.deleteOnExit();//delete();  Keep FindBugs happy
			} catch (IOException ioe) {
				encoding = "US-ASCII";
			}
		}
		return encoding;
	}


	/**
	 * Returns the encoding to use when reading or writing this file.
	 *
	 * @return The encoding.
	 * @see #setEncoding(String)
	 */
	public String getEncoding() {
		return charSet;
	}


	/**
	 * Returns the full path to this document.
	 *
	 * @return The full path to the document.
	 */
	public String getFileFullPath() {
		return loc==null ? null : loc.getFileFullPath();
	}


	/**
	 * Returns the file name of this document.
	 *
	 * @return The file name.
	 */
	public String getFileName() {
		return loc.getFileName();
	}


	/**
	 * Returns the timestamp for when this file was last loaded or saved
	 * <em>by this editor pane</em>.  If the file has been modified on disk by
	 * another process after it was loaded into this editor pane, this method
	 * will not return the actual file's last modified time.<p>
	 *
	 * For remote files, this method will always return
	 * {@link #LAST_MODIFIED_UNKNOWN}.
	 *
	 * @return The timestamp when this file was last loaded or saved by this
	 *         editor pane, if it is a local file, or
	 *         {@link #LAST_MODIFIED_UNKNOWN} if it is a remote file.
	 * @see #isModifiedOutsideEditor()
	 */
	public long getLastSaveOrLoadTime() {
		return lastSaveOrLoadTime;
	}


	/**
	 * Returns the line separator used when writing this file (e.g.
	 * "<code>\n</code>", "<code>\r\n</code>", or "<code>\r</code>").<p>
	 * 
	 * Note that this value is an <code>Object</code> and not a
	 * <code>String</code> as that is the way the {@link Document} interface
	 * defines its property values.  If you always use
	 * {@link #setLineSeparator(String)} to modify this value, then the value
	 * returned from this method will always be a <code>String</code>.
	 *
	 * @return The line separator.  If this value is <code>null</code>, then
	 *         the system default line separator is used (usually the value
	 *         of <code>System.getProperty("line.separator")</code>).
	 * @see #setLineSeparator(String)
	 * @see #setLineSeparator(String, boolean)
	 */
	public Object getLineSeparator() {
		return getDocument().getProperty(
							RTextAreaEditorKit.EndOfLineStringProperty);
	}


	/**
	 * Initializes this editor with the specified file location.
	 *
	 * @param loc The file location.  If this is <code>null</code>, a default
	 *        location is used and an empty file is displayed.
	 * @param defaultEnc The default encoding to use when opening the file,
	 *        if the file is not Unicode.  If this value is <code>null</code>,
	 *        a system default value is used.
	 * @throws IOException If an IO error occurs reading from <code>loc</code>.
	 *         If <code>loc</code> is <code>null</code>, this cannot happen.
	 */
	private void init(FileLocation loc, String defaultEnc) throws IOException {

		if (loc==null) {
			// Don't call load() just in case Untitled.txt actually exists,
			// just to ensure there is no chance of an IOException being thrown
			// in the default case.
			this.loc = FileLocation.create(DEFAULT_FILE_NAME);
			charSet = defaultEnc==null ? getDefaultEncoding() : defaultEnc;
			// Ensure that line separator always has a value, even if the file
			// does not exist (or is the "default" file).  This makes life
			// easier for host applications that want to display this value.
			setLineSeparator(System.getProperty("line.separator"));
		}
		else {
			load(loc, defaultEnc); // Sets this.loc
		}

		if (this.loc.isLocalAndExists()) {
			File file = new File(this.loc.getFileFullPath());
			lastSaveOrLoadTime = file.lastModified();
			setReadOnly(!file.canWrite());
		}
		else {
			lastSaveOrLoadTime = LAST_MODIFIED_UNKNOWN;
			setReadOnly(false);
		}

		setDirty(false);

	}


	/**
	 * Callback for when text is inserted into the document.
	 *
	 * @param e Information on the insertion.
	 */
	public void insertUpdate(DocumentEvent e) {
		if (!dirty) {
			setDirty(true);
		}
	}


	/**
	 * Returns whether or not the text in this editor has unsaved changes.
	 *
	 * @return Whether or not the text has unsaved changes.
	 * @see #setDirty(boolean)
	 */
	public boolean isDirty() {
		return dirty;
	}


	/**
	 * Returns whether this file is a local file.
	 *
	 * @return Whether this is a local file.
	 */
	public boolean isLocal() {
		return loc.isLocal();
	}


	/**
	 * Returns whether this is a local file that already exists.
	 *
	 * @return Whether this is a local file that already exists.
	 */
	public boolean isLocalAndExists() {
		return loc.isLocalAndExists();
	}


	/**
	 * Returns whether the text file has been modified outside of this editor
	 * since the last load or save operation.  Note that if this is a remote
	 * file, this method will always return <code>false</code>.<p>
	 *
	 * This method may be used by applications to implement a reloading
	 * feature, where the user is prompted to reload a file if it has been
	 * modified since their last open or save.
	 *
	 * @return Whether the text file has been modified outside of this
	 *         editor.
	 * @see #getLastSaveOrLoadTime()
	 */
	public boolean isModifiedOutsideEditor() {
		return loc.getActualLastModified()>getLastSaveOrLoadTime();
	}


	/**
	 * Returns whether or not the text area should be treated as read-only.
	 *
	 * @return Whether or not the text area should be treated as read-only.
	 * @see #setReadOnly(boolean)
	 */
	public boolean isReadOnly() {
		return readOnly;
	}


	/**
	 * Loads the specified file in this editor.  This method fires a property
	 * change event of type {@link #FULL_PATH_PROPERTY}.
	 *
	 * @param loc The location of the file to load.  This cannot be
	 *        <code>null</code>.
	 * @param defaultEnc The encoding to use when loading/saving the file.
	 *        This encoding will only be used if the file is not Unicode.
	 *        If this value is <code>null</code>, the system default encoding
	 *        is used.
	 * @throws IOException If an IO error occurs.
	 * @see #save()
	 * @see #saveAs(FileLocation)
	 */
	public void load(FileLocation loc, String defaultEnc) throws IOException {

		// For new local files, just go with it.
		if (loc.isLocal() && !loc.isLocalAndExists()) {
			this.charSet = defaultEnc!=null ? defaultEnc : getDefaultEncoding();
			this.loc = loc;
			setText(null);
			discardAllEdits();
			setDirty(false);
			return;
		}

		// Old local files and remote files, load 'em up.  UnicodeReader will
		// check for BOMs and handle them correctly in all cases, then pass
		// rest of stream down to InputStreamReader.
		UnicodeReader ur = new UnicodeReader(loc.getInputStream(), defaultEnc);

		// Remove listener so dirty flag doesn't get set when loading a file.
		Document doc = getDocument();
		doc.removeDocumentListener(this);
		BufferedReader r = new BufferedReader(ur);
		try {
			read(r, null);
		} finally {
			doc.addDocumentListener(this);
			r.close();
		}

		// No IOException thrown, so we can finally change the location.
		charSet = ur.getEncoding();
		String old = getFileFullPath();
		this.loc = loc;
		setDirty(false);
		setCaretPosition(0);
		firePropertyChange(FULL_PATH_PROPERTY, old, getFileFullPath());

	}


	/**
	 * Reloads this file from disk.  The file must exist for this operation
	 * to not throw an exception.<p>
	 * 
	 * The file's "dirty" state will be set to <code>false</code> after this
	 * operation.  If this is a local file, its "last modified" time is
	 * updated to reflect that of the actual file.<p>
	 *
	 * Note that if the file has been modified on disk, and is now a Unicode
	 * encoding when before it wasn't (or if it is a different Unicode now),
	 * this will cause this {@link TextEditorPane}'s encoding to change.
	 * Otherwise, the file's encoding will stay the same.
	 *
	 * @throws IOException If the file does not exist, or if an IO error
	 *         occurs reading the file.
	 * @see #isLocalAndExists()
	 */
	public void reload() throws IOException {
		String oldEncoding = getEncoding();
		UnicodeReader ur = new UnicodeReader(loc.getInputStream(), oldEncoding);
		String encoding = ur.getEncoding();
		BufferedReader r = new BufferedReader(ur);
		try {
			read(r, null); // Dumps old contents.
		} finally {
			r.close();
		}
		setEncoding(encoding);
		setDirty(false);
		syncLastSaveOrLoadTimeToActualFile();
		discardAllEdits(); // Prevent user from being able to undo the reload
	}


	/**
	 * Called whenever text is removed from this editor.
	 *
	 * @param e The document event.
	 */
	public void removeUpdate(DocumentEvent e) {
		if (!dirty) {
			setDirty(true);
		}
	}


	/**
	 * Saves the file in its current encoding.<p>
	 *
	 * The text area's "dirty" state is set to <code>false</code>, and if
	 * this is a local file, its "last modified" time is updated.
	 *
	 * @throws IOException If an IO error occurs.
	 * @see #saveAs(FileLocation)
	 * @see #load(FileLocation, String)
	 */
	public void save() throws IOException {
		saveImpl(loc);
		setDirty(false);
		syncLastSaveOrLoadTimeToActualFile();
	}


	/**
	 * Saves this file in a new local location.  This method fires a property
	 * change event of type {@link #FULL_PATH_PROPERTY}.
	 *
	 * @param loc The location to save to.
	 * @throws IOException If an IO error occurs.
	 * @see #save()
	 * @see #load(FileLocation, String)
	 */
	public void saveAs(FileLocation loc) throws IOException {
		saveImpl(loc);
		// No exception thrown - we can "rename" the file.
		String old = getFileFullPath();
		this.loc = loc;
		setDirty(false);
		lastSaveOrLoadTime = loc.getActualLastModified();
		firePropertyChange(FULL_PATH_PROPERTY, old, getFileFullPath());
	}


	/**
	 * Saves the text in this editor to the specified location.
	 *
	 * @param loc The location to save to.
	 * @throws IOException If an IO error occurs.
	 */
	private void saveImpl(FileLocation loc) throws IOException {
		OutputStream out = loc.getOutputStream();
		BufferedWriter w = new BufferedWriter(
				new UnicodeWriter(out, getEncoding()));
		try {
			write(w);
		} finally {
			w.close();
		}
	}


	/**
	 * Sets whether or not this text in this editor has unsaved changes.
	 * This fires a property change event of type {@link #DIRTY_PROPERTY}.<p>
	 *
	 * Applications will usually have no need to call this method directly; the
	 * only time you might have a need to call this method directly is if you
	 * have to initialize an instance of TextEditorPane with content that does
	 * not come from a file. <code>TextEditorPane</code> automatically sets its
	 * own dirty flag when its content is edited, when its encoding is changed,
	 * or when its line ending property is changed.  It is cleared whenever
	 * <code>load()</code>, <code>reload()</code>, <code>save()</code>, or
	 * <code>saveAs()</code> are called.
	 *
	 * @param dirty Whether or not the text has been modified.
	 * @see #isDirty()
	 */
	public void setDirty(boolean dirty) {
		if (this.dirty!=dirty) {
			this.dirty = dirty;
			firePropertyChange(DIRTY_PROPERTY, !dirty, dirty);
		}
	}


	/**
	 * Sets the document for this editor.
	 *
	 * @param doc The new document.
	 */
	public void setDocument(Document doc) {
		Document old = getDocument();
		if (old!=null) {
			old.removeDocumentListener(this);
		}
		super.setDocument(doc);
		doc.addDocumentListener(this);
	}


	/**
	 * Sets the encoding to use when reading or writing this file.  This
	 * method sets the editor's dirty flag when the encoding is changed.
	 *
	 * @param encoding The new encoding.
	 * @throws UnsupportedCharsetException If the encoding is not supported.
	 * @throws NullPointerException If <code>encoding</code> is
	 *         <code>null</code>.
	 * @see #getEncoding()
	 */
	public void setEncoding(String encoding) {
		if (encoding==null) {
			throw new NullPointerException("encoding cannot be null");
		}
		else if (!Charset.isSupported(encoding)) {
			throw new UnsupportedCharsetException(encoding);
		}
		if (charSet==null || !charSet.equals(encoding)) {
			charSet = encoding;
			setDirty(true);
		}
	}


	/**
	 * Sets the line separator sequence to use when this file is saved (e.g.
	 * "<code>\n</code>", "<code>\r\n</code>" or "<code>\r</code>").
	 *
	 * Besides parameter checking, this method is preferred over
	 * <code>getDocument().putProperty()</code> because it sets the editor's
	 * dirty flag when the line separator is changed.
	 *
	 * @param separator The new line separator.
	 * @throws NullPointerException If <code>separator</code> is
	 *         <code>null</code>.
	 * @throws IllegalArgumentException If <code>separator</code> is not one
	 *         of "<code>\n</code>", "<code>\r\n</code>" or "<code>\r</code>".
	 * @see #getLineSeparator()
	 */
	public void setLineSeparator(String separator) {
		setLineSeparator(separator, true);
	}


	/**
	 * Sets the line separator sequence to use when this file is saved (e.g.
	 * "<code>\n</code>", "<code>\r\n</code>" or "<code>\r</code>").
	 *
	 * Besides parameter checking, this method is preferred over
	 * <code>getDocument().putProperty()</code> because can set the editor's
	 * dirty flag when the line separator is changed.
	 *
	 * @param separator The new line separator.
	 * @param setDirty Whether the dirty flag should be set if the line
	 *        separator is changed.
	 * @throws NullPointerException If <code>separator</code> is
	 *         <code>null</code>.
	 * @throws IllegalArgumentException If <code>separator</code> is not one
	 *         of "<code>\n</code>", "<code>\r\n</code>" or "<code>\r</code>".
	 * @see #getLineSeparator()
	 */
	public void setLineSeparator(String separator, boolean setDirty) {
		if (separator==null) {
			throw new NullPointerException("terminator cannot be null");
		}
		if (!"\r\n".equals(separator) && !"\n".equals(separator) &&
				!"\r".equals(separator)) {
			throw new IllegalArgumentException("Invalid line terminator");
		}
		Document doc = getDocument();
		Object old = doc.getProperty(
						RTextAreaEditorKit.EndOfLineStringProperty);
		if (!separator.equals(old)) {
			doc.putProperty(RTextAreaEditorKit.EndOfLineStringProperty,
							separator);
			if (setDirty) {
				setDirty(true);
			}
		}
	}


	/**
	 * Sets whether or not this text area should be treated as read-only.
	 * This fires a property change event of type {@link #READ_ONLY_PROPERTY}.
	 *
	 * @param readOnly Whether or not the document is read-only.
	 * @see #isReadOnly()
	 */
	public void setReadOnly(boolean readOnly) {
		if (this.readOnly!=readOnly) {
			this.readOnly = readOnly;
			firePropertyChange(READ_ONLY_PROPERTY, !readOnly, readOnly);
		}
	}


	/**
	 * Syncs this text area's "last saved or loaded" time to that of the file
	 * being edited, if that file is local and exists.  If the file is
	 * remote or is local but does not yet exist, nothing happens.<p>
	 *
	 * You normally do not have to call this method, as the "last saved or
	 * loaded" time for {@link TextEditorPane}s is kept up-to-date internally
	 * during such operations as {@link #save()}, {@link #reload()}, etc.
	 *
	 * @see #getLastSaveOrLoadTime()
	 * @see #isModifiedOutsideEditor()
	 */
	public void syncLastSaveOrLoadTimeToActualFile() {
		if (loc.isLocalAndExists()) {
			lastSaveOrLoadTime = loc.getActualLastModified();
		}
	}


}