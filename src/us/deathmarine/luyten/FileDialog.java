package us.deathmarine.luyten;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * FileChoosers for Open and Save
 */
public class FileDialog {
	private ConfigSaver configSaver;
	private LuytenPreferences luytenPrefs;
	private Component parent;
	private JFileChooser fcOpen;
	private JFileChooser fcSave;
	private JFileChooser fcSaveAll;

	public FileDialog(Component parent) {
		this.parent = parent;
		configSaver = ConfigSaver.getLoadedInstance();
		luytenPrefs = configSaver.getLuytenPreferences();

		new Thread() {
			public void run() {
				try {
					initOpenDialog();
					Thread.sleep(500);
					initSaveAllDialog();
					Thread.sleep(500);
					initSaveDialog();
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			};
		}.start();
	}

	public File doOpenDialog() {
		File selectedFile = null;
		initOpenDialog();

		retrieveOpenDialogDir(fcOpen);
		int returnVal = fcOpen.showOpenDialog(parent);
		saveOpenDialogDir(fcOpen);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selectedFile = fcOpen.getSelectedFile();
		}
		return selectedFile;
	}

	public File doSaveDialog(String recommendedFileName) {
		File selectedFile = null;
		initSaveDialog();

		retrieveSaveDialogDir(fcSave);
		fcSave.setSelectedFile(new File(recommendedFileName));
		int returnVal = fcSave.showSaveDialog(parent);
		saveSaveDialogDir(fcSave);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selectedFile = fcSave.getSelectedFile();
		}
		return selectedFile;
	}

	public File doSaveAllDialog(String recommendedFileName) {
		File selectedFile = null;
		initSaveAllDialog();

		retrieveSaveDialogDir(fcSaveAll);
		fcSaveAll.setSelectedFile(new File(recommendedFileName));
		int returnVal = fcSaveAll.showSaveDialog(parent);
		saveSaveDialogDir(fcSaveAll);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			selectedFile = fcSaveAll.getSelectedFile();
		}
		return selectedFile;
	}

	public synchronized void initOpenDialog() {
		if (fcOpen == null) {
			fcOpen = createFileChooser("*.jar", "*.zip", "*.class");
			retrieveOpenDialogDir(fcOpen);
		}
	}

	public synchronized void initSaveDialog() {
		if (fcSave == null) {
			fcSave = createFileChooser("*.txt", "*.java");
			retrieveSaveDialogDir(fcSave);
		}
	}

	public synchronized void initSaveAllDialog() {
		if (fcSaveAll == null) {
			fcSaveAll = createFileChooser("*.jar", "*.zip");
			retrieveSaveDialogDir(fcSaveAll);
		}
	}

	private JFileChooser createFileChooser(String... fileFilters) {
		JFileChooser fc = new JFileChooser();
		for (String fileFilter : fileFilters) {
			fc.addChoosableFileFilter(new FileChooserFileFilter(fileFilter));
		}
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		return fc;
	}

	public class FileChooserFileFilter extends FileFilter {
		String objType;

		public FileChooserFileFilter(String string) {
			objType = string;
		}

		@Override
		public boolean accept(File f) {
			if (f.isDirectory())
				return true;
			return f.getName().toLowerCase().endsWith(objType.substring(1));
		}

		@Override
		public String getDescription() {
			return objType;
		}
	}

	private void retrieveOpenDialogDir(JFileChooser fc) {
		try {
			String currentDirStr = luytenPrefs.getFileOpenCurrentDirectory();
			if (currentDirStr != null && currentDirStr.trim().length() > 0) {
				File currentDir = new File(currentDirStr);
				if (currentDir.exists() && currentDir.isDirectory()) {
					fc.setCurrentDirectory(currentDir);
				}
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void saveOpenDialogDir(JFileChooser fc) {
		try {
			File currentDir = fc.getCurrentDirectory();
			if (currentDir != null && currentDir.exists() && currentDir.isDirectory()) {
				luytenPrefs.setFileOpenCurrentDirectory(currentDir.getAbsolutePath());
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void retrieveSaveDialogDir(JFileChooser fc) {
		try {
			String currentDirStr = luytenPrefs.getFileSaveCurrentDirectory();
			if (currentDirStr != null && currentDirStr.trim().length() > 0) {
				File currentDir = new File(currentDirStr);
				if (currentDir.exists() && currentDir.isDirectory()) {
					fc.setCurrentDirectory(currentDir);
				}
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void saveSaveDialogDir(JFileChooser fc) {
		try {
			File currentDir = fc.getCurrentDirectory();
			if (currentDir != null && currentDir.exists() && currentDir.isDirectory()) {
				luytenPrefs.setFileSaveCurrentDirectory(currentDir.getAbsolutePath());
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}
}
