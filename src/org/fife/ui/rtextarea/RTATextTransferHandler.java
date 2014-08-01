/*
 * 07/29/2004
 *
 * RTATextTransferHandler.java - Handles the transfer of data to/from an
 * RTextArea via drag-and-drop.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.datatransfer.*;
import java.awt.im.InputContext;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import javax.swing.*;
import javax.swing.text.*;


/**
 * Handles the transfer of data to/from an <code>RTextArea</code> via
 * drag-and-drop.  This class is pretty much ripped off from a subclass of
 * <code>BasicTextUI</code>.  In the future, it will include the ability to
 * drag-and-drop files into <code>RTextArea</code>s (i.e., the text will be
 * inserted into the text area).<p>
 * 
 * The main reason this class is kept around is so we can subclass it.
 *
 * @author Robert Futrell
 * @version 0.1
 */
public class RTATextTransferHandler extends TransferHandler {

	private JTextComponent exportComp;
	private boolean shouldRemove;
	private int p0;
	private int p1;
	private boolean withinSameComponent;


	/**
	 * Try to find a flavor that can be used to import a Transferable to a
	 * specified text component.  
	 * The set of usable flavors are tried in the following order:
	 * <ol>
	 *     <li>First, an attempt is made to find a flavor matching the content
	 *         tyep of the EditorKit for the component.
	 *     <li>Second, an attempt to find a text/plain flavor is made.
	 *     <li>Third, an attempt to find a flavor representing a String
	 *         reference in the same VM is made.
	 *     <li>Lastly, DataFlavor.stringFlavor is searched for.
	 * </ol>
	 *
	 * @param flavors The flavors to check if c will accept them.
	 * @param c The text component to see whether it will accept any of the
	 *        specified data flavors as input.
	 */
	 protected DataFlavor getImportFlavor(DataFlavor[] flavors,
	 								JTextComponent c) {

		DataFlavor refFlavor = null;
		DataFlavor stringFlavor = null;
            
		for (int i = 0; i < flavors.length; i++) {

			String mime = flavors[i].getMimeType();
			if (mime.startsWith("text/plain")) {
				return flavors[i];
			}
			else if (refFlavor == null
				&& mime.startsWith("application/x-java-jvm-local-objectref")
				&& flavors[i].getRepresentationClass() == String.class)
			{
				refFlavor = flavors[i];
			}
			else if (stringFlavor==null &&
				flavors[i].equals(DataFlavor.stringFlavor))
			{
				stringFlavor = flavors[i];
			}

		}

		if (refFlavor != null)
			return refFlavor;
		else if (stringFlavor != null)
			return stringFlavor;

		return null;

	}


	/**
	 * Import the given stream data into the text component.
	 */
	protected void handleReaderImport(Reader in, JTextComponent c)
							throws BadLocationException, IOException {

		char[] buff = new char[1024];
		int nch;
		boolean lastWasCR = false;
		int last;
		StringBuffer sbuff = null;
                
		// Read in a block at a time, mapping \r\n to \n, as well as single
		// \r to \n.
		while ((nch = in.read(buff, 0, buff.length)) != -1) {

			if (sbuff == null) {
				sbuff = new StringBuffer(nch);
                   }
			last = 0;

			for (int counter = 0; counter < nch; counter++) {

				switch (buff[counter]) {
					case '\r':
						if (lastWasCR) {
							if (counter == 0)
								sbuff.append('\n');
							else
								buff[counter - 1] = '\n';
						}
						else
							lastWasCR = true;
						break;
					case '\n':
						if (lastWasCR) {
							if (counter > (last + 1))
								sbuff.append(buff, last, counter - last - 1);
							// else nothing to do, can skip \r, next write will
							// write \n
							lastWasCR = false;
							last = counter;
						}
						break;
					default:
						if (lastWasCR) {
							if (counter == 0)
								sbuff.append('\n');
							else
								buff[counter - 1] = '\n';
							lastWasCR = false;
						}
						break;

				} // End fo switch (buff[counter]).

			} // End of for (int counter = 0; counter < nch; counter++).

			if (last < nch) {
				if (lastWasCR) {
					if (last < (nch - 1))
						sbuff.append(buff, last, nch - last - 1);
				}
					else
					sbuff.append(buff, last, nch - last);
			}

		} // End of while ((nch = in.read(buff, 0, buff.length)) != -1).

		if (withinSameComponent) {
			((RTextArea)c).beginAtomicEdit();
		}

		if (lastWasCR)
			sbuff.append('\n');
		c.replaceSelection(sbuff != null ? sbuff.toString() : "");

	}


	/**
	 * This is the type of transfer actions supported by the source.  Some
	 * models are not mutable, so a transfer operation of COPY only should
	 * be advertised in that case.
	 * 
	 * @param c  The component holding the data to be transfered.  This
	 *  argument is provided to enable sharing of TransferHandlers by
	 *  multiple components.
	 * @return If the text component is editable, COPY_OR_MOVE is returned,
	 *         otherwise just COPY is allowed.
	 */
	public int getSourceActions(JComponent c) {
		if (((JTextComponent)c).isEditable())
			return COPY_OR_MOVE;
		else
			return COPY;
	}


	/**
	 * Create a Transferable to use as the source for a data transfer.
	 *
	 * @param comp  The component holding the data to be transfered.  This
	 *  argument is provided to enable sharing of TransferHandlers by
	 *  multiple components.
	 * @return  The representation of the data to be transfered. 
	 *  
	 */
	protected Transferable createTransferable(JComponent comp) {
		exportComp = (JTextComponent)comp;
		shouldRemove = true;
		p0 = exportComp.getSelectionStart();
		p1 = exportComp.getSelectionEnd();
		return (p0 != p1) ? (new TextTransferable(exportComp, p0, p1)) : null;
	}


	/**
	 * This method is called after data has been exported.  This method should
	 * remove the data that was transfered if the action was MOVE.
	 *
	 * @param source The component that was the source of the data.
	 * @param data   The data that was transferred or possibly null
     *               if the action is <code>NONE</code>.
	 * @param action The actual action that was performed.  
	 */
	protected void exportDone(JComponent source, Transferable data, int action) {
		// only remove the text if shouldRemove has not been set to
		// false by importData and only if the action is a move
		if (shouldRemove && action == MOVE) {
			TextTransferable t = (TextTransferable)data;
			t.removeText();
			if (withinSameComponent) {
				((RTextArea)source).endAtomicEdit();
				withinSameComponent = false;
			}
		}
		exportComp = null;
	}


	/**
	 * This method causes a transfer to a component from a clipboard or a 
	 * DND drop operation.  The Transferable represents the data to be
	 * imported into the component.  
	 *
	 * @param comp  The component to receive the transfer.  This
	 *  argument is provided to enable sharing of TransferHandlers by
	 *  multiple components.
	 * @param t The data to import
	 * @return <code>true</code> iff the data was inserted into the component.
	 */
	public boolean importData(JComponent comp, Transferable t) {

		JTextComponent c = (JTextComponent)comp;
		withinSameComponent = c==exportComp;

		// if we are importing to the same component that we exported from
		// then don't actually do anything if the drop location is inside
		// the drag location and set shouldRemove to false so that exportDone
		// knows not to remove any data
		if (withinSameComponent && c.getCaretPosition()>=p0 && c.getCaretPosition()<=p1) {
			shouldRemove = false;
			return true;
		}

		boolean imported = false;
		DataFlavor importFlavor = getImportFlavor(t.getTransferDataFlavors(), c);
		if (importFlavor != null) {
			try {
				InputContext ic = c.getInputContext();
				if (ic != null)
					ic.endComposition();
				Reader r = importFlavor.getReaderForText(t);
				handleReaderImport(r, c);
				imported = true;
			} catch (UnsupportedFlavorException ufe) {
				ufe.printStackTrace();
			} catch (BadLocationException ble) {
				ble.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

		return imported;

	}

	/**
	 * This method indicates if a component would accept an import of the
	 * given set of data flavors prior to actually attempting to import it. 
	 *
	 * @param comp The component to receive the transfer.  This argument is
	 *        provided to enable sharing of TransferHandlers by multiple
	 *        components.
	 * @param flavors The data formats available.
	 * @return <code>true</code> iff the data can be inserted.
	 */
	public boolean canImport(JComponent comp, DataFlavor[] flavors) {
		JTextComponent c = (JTextComponent)comp;
		if (!(c.isEditable() && c.isEnabled()))
			return false;
		return (getImportFlavor(flavors, c) != null);
	}


	/**
	 * A possible implementation of the Transferable interface for RTextAreas.
	 */
	static class TextTransferable implements Transferable {

		Position p0;
		Position p1;
		JTextComponent c;

		protected String plainData;

		private static DataFlavor[] stringFlavors;
		private static DataFlavor[] plainFlavors;

		TextTransferable(JTextComponent c, int start, int end) {
			this.c = c;
			Document doc = c.getDocument();
			try {
				p0 = doc.createPosition(start);
				p1 = doc.createPosition(end);
				plainData = c.getSelectedText();
			} catch (BadLocationException ble) {
			}
		}

		/**
		 * Fetch the data in a text/plain format.
		 */
		protected String getPlainData() {
			return plainData;
		}

		/**
		 * Returns an object which represents the data to be transferred.  The class 
		 * of the object returned is defined by the representation class of the flavor.
		 *
		 * @param flavor the requested flavor for the data
		 * @see DataFlavor#getRepresentationClass
		 * @exception IOException                if the data is no longer available
		 *              in the requested flavor.
		 * @exception UnsupportedFlavorException if the requested data flavor is
		 *              not supported.
		 */
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			if (isPlainFlavor(flavor)) {
				String data = getPlainData();
				data = (data == null) ? "" : data;
				if (String.class.equals(flavor.getRepresentationClass())) {
					return data;
				} else if (Reader.class.equals(flavor.getRepresentationClass())) {
					return new StringReader(data);
				} else if (InputStream.class.equals(flavor.getRepresentationClass())) {
					return new StringBufferInputStream(data);
				}
				// fall through to unsupported
			} else if (isStringFlavor(flavor)) {
				String data = getPlainData();
				data = (data == null) ? "" : data;
				return data;
			}
			throw new UnsupportedFlavorException(flavor);
		}

		/**
		 * Returns an array of DataFlavor objects indicating the flavors the data 
		 * can be provided in.  The array should be ordered according to preference
		 * for providing the data (from most richly descriptive to least descriptive).
		 *
		 * @return an array of data flavors in which this data can be transferred
		 */
		public DataFlavor[] getTransferDataFlavors() {

			int plainCount = (isPlainSupported()) ? plainFlavors.length: 0;
			int stringCount = (isPlainSupported()) ? stringFlavors.length : 0;
			int totalCount = plainCount + stringCount;
			DataFlavor[] flavors = new DataFlavor[totalCount];
	
			// fill in the array
			int pos = 0;
			if (plainCount > 0) {
				System.arraycopy(plainFlavors, 0, flavors, pos, plainCount);
				pos += plainCount;
			}
			if (stringCount > 0) {
				System.arraycopy(stringFlavors, 0, flavors, pos, stringCount);
				//pos += stringCount;
			}

			return flavors;

		}

		/**
		 * Returns whether or not the specified data flavor is supported for
		 * this object.
		 * @param flavor the requested flavor for the data
		 * @return boolean indicating whether or not the data flavor is supported
		 */
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			DataFlavor[] flavors = getTransferDataFlavors();
			for (int i = 0; i < flavors.length; i++) {
				if (flavors[i].equals(flavor))
					return true;
			}
			return false;
		}

		/**
		 * Returns whether or not the specified data flavor is an plain flavor that
		 * is supported.
		 * @param flavor the requested flavor for the data
		 * @return boolean indicating whether or not the data flavor is supported
		 */
		protected boolean isPlainFlavor(DataFlavor flavor) {
			DataFlavor[] flavors = plainFlavors;
			for (int i = 0; i < flavors.length; i++) {
				if (flavors[i].equals(flavor))
					return true;
			}
			return false;
		}

		/**
		 * Should the plain text flavors be offered?  If so, the method
		 * getPlainData should be implemented to provide something reasonable.
		 */
		protected boolean isPlainSupported() {
			return plainData != null;
		}

		/**
		 * Returns whether or not the specified data flavor is a String flavor that
		 * is supported.
		 * @param flavor the requested flavor for the data
		 * @return boolean indicating whether or not the data flavor is supported
		 */
		protected boolean isStringFlavor(DataFlavor flavor) {
			DataFlavor[] flavors = stringFlavors;
			for (int i = 0; i < flavors.length; i++) {
				if (flavors[i].equals(flavor))
					return true;
			}
			return false;
		}

		void removeText() {
			if ((p0 != null) && (p1 != null) && (p0.getOffset() != p1.getOffset())) {
				try {
					Document doc = c.getDocument();
					doc.remove(p0.getOffset(), p1.getOffset() - p0.getOffset());
				} catch (BadLocationException e) {
				}
			}
		}

		// Initialization of supported flavors.
		static {
			try {

				plainFlavors = new DataFlavor[3];
				plainFlavors[0] = new DataFlavor("text/plain;class=java.lang.String");
				plainFlavors[1] = new DataFlavor("text/plain;class=java.io.Reader");
				plainFlavors[2] = new DataFlavor("text/plain;charset=unicode;class=java.io.InputStream");

				stringFlavors = new DataFlavor[2];
				stringFlavors[0] = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType+";class=java.lang.String");
				stringFlavors[1] = DataFlavor.stringFlavor;
 
			} catch (ClassNotFoundException cle) {
				System.err.println("Error initializing org.fife.ui.RTATextTransferHandler");
			}
		}

	}


}