/*
 * 07/28/2008
 *
 * RtfTransferable.java - Used during drag-and-drop to represent RTF text.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea;

import java.awt.datatransfer.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;


/**
 * Object used during copy/paste and DnD operations to represent RTF text.
 * It can return the text being moved as either RTF or plain text.  This
 * class is basically the same as
 * <code>java.awt.datatransfer.StringSelection</code>, except that it can also
 * return the text as RTF.
 *
 * @author Robert Futrell
 * @version 1.0
 */
class RtfTransferable implements Transferable {

	/**
	 * The RTF data, in bytes (the RTF is 7-bit ascii).
	 */
	private byte[] data;


	/**
	 * The "flavors" the text can be returned as.
	 */
	private final DataFlavor[] FLAVORS = {
		new DataFlavor("text/rtf", "RTF"),
		DataFlavor.stringFlavor,
		DataFlavor.plainTextFlavor // deprecated
	};


	/**
	 * Constructor.
	 *
	 * @param data The RTF data.
	 */
	public RtfTransferable(byte[] data) {
		this.data = data;
	}


	public Object getTransferData(DataFlavor flavor)
					throws UnsupportedFlavorException, IOException {
		if (flavor.equals(FLAVORS[0])) { // RTF
			return new ByteArrayInputStream(data==null ? new byte[0] : data);
		}
		else if (flavor.equals(FLAVORS[1])) { // stringFlavor
			return data==null ? "" : RtfToText.getPlainText(data);
		}
		else if (flavor.equals(FLAVORS[2])) { // plainTextFlavor (deprecated)
			String text = ""; // Valid if data==null
			if (data!=null) {
				text = RtfToText.getPlainText(data);
			}
			return new StringReader(text);
		}
		else {
			throw new UnsupportedFlavorException(flavor);
		}
	}


	public DataFlavor[] getTransferDataFlavors() {
		return (DataFlavor[])FLAVORS.clone();
	}


	public boolean isDataFlavorSupported(DataFlavor flavor) {
		for (int i=0; i<FLAVORS.length; i++) {
			if (flavor.equals(FLAVORS[i])) {
				return true;
			}
		}
		return false;
	}


}