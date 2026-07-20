package us.deathmarine.luyten;

import java.io.Serializable;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class FindAllLabeledProgressBar implements Serializable {

    private static final long serialVersionUID = 4599045134198840361L;

    private final JLabel statusLabel = new JLabel("");
    private final JProgressBar progressBar;

    public FindAllLabeledProgressBar(JProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    public void setStatus(String text) {
        if (text.length() > 25) {
            this.statusLabel.setText("Searching in file: ..." + text.substring(text.length() - 25));
        } else {
            this.statusLabel.setText("Searching in file: " + text);
        }

        progressBar.setValue(progressBar.getValue() + 1);
    }

    public void initProgressBar(Integer length) {
        progressBar.setMaximum(length);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

}
