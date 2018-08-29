package us.deathmarine.luyten;

import javax.swing.*;
import java.io.Serializable;

class ExtractedFindAllBox implements Serializable {
    private JProgressBar progressBar;
    private JLabel statusLabel = new JLabel("");

    ExtractedFindAllBox() {
    }

    JProgressBar getProgressBar() {
        return progressBar;
    }

    void setProgressBar(JProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    JLabel getStatusLabel() {
        return statusLabel;
    }

    void setStatus(String text) {
        if (text.length() > 25) {
            this.statusLabel.setText("Searching in file: ..." + text.substring(text.length() - 25));
        } else {
            this.statusLabel.setText("Searching in file: " + text);
        }

        progressBar.setValue(progressBar.getValue() + 1);
    }

    void initProgressBar(Integer length) {
        progressBar.setMaximum(length);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
    }
}