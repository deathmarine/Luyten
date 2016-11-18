package us.deathmarine.luyten;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Drag-Drop (only MainWindow should be called from here)
 */
public class DropListener implements DropTargetListener {
	private MainWindow mainWindow;

	public DropListener(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void drop(DropTargetDropEvent event) {
		event.acceptDrop(DnDConstants.ACTION_COPY);
		Transferable transferable = event.getTransferable();
		if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			for (DataFlavor flavor : flavors) {
				try {
					if (flavor.isFlavorJavaFileListType()) {
						List<File> files = (List<File>) transferable.getTransferData(flavor);
						if (files.size() > 1) {
							event.rejectDrop();
							return;
						}
						if (files.size() == 1) {
							mainWindow.onFileDropped(files.get(0));
						}
					}
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			}
			event.dropComplete(true);
		} else {
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			boolean handled = false;
			for (int zz = 0; zz < flavors.length; zz++) {
				if (flavors[zz].isRepresentationClassReader()) {
					try {
						Reader reader = flavors[zz].getReaderForText(transferable);
						BufferedReader br = new BufferedReader(reader);
						List<File> list = new ArrayList<File>();
						String line = null;
						while ((line = br.readLine()) != null) {
							try {
								if (new String("" + (char) 0).equals(line))
									continue;
								File file = new File(new URI(line));
								list.add(file);
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						if (list.size() > 1) {
							event.rejectDrop();
							return;
						}
						if (list.size() == 1) {
							mainWindow.onFileDropped(list.get(0));
						}
						event.getDropTargetContext().dropComplete(true);
						handled = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				}
			}
			if (!handled) {
				event.rejectDrop();
			}
		}

	}

	@Override
	public void dragEnter(DropTargetDragEvent arg0) {
	}

	@Override
	public void dragExit(DropTargetEvent arg0) {
	}

	@Override
	public void dragOver(DropTargetDragEvent arg0) {
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent arg0) {
	}
}
