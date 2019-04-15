package us.deathmarine.luyten;

import javax.swing.*;
import java.io.File;

class DirPreferences {
    private LuytenPreferences luytenPrefs;

    public DirPreferences(LuytenPreferences luytenPrefs) {
        this.luytenPrefs = luytenPrefs;
    }

    void retrieveOpenDialogDir(JFileChooser fc) {
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

    void saveOpenDialogDir(JFileChooser fc) {
        try {
            File currentDir = fc.getCurrentDirectory();
            if (currentDir != null && currentDir.exists() && currentDir.isDirectory()) {
                luytenPrefs.setFileOpenCurrentDirectory(currentDir.getAbsolutePath());
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    void retrieveSaveDialogDir(JFileChooser fc) {
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

    void saveSaveDialogDir(JFileChooser fc) {
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