package us.deathmarine.luyten;

import java.awt.BorderLayout;
import javax.swing.JRootPane;
import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class LuytenTabPane extends JRootPane {

    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private final ErrorStrip errorStrip;

    public LuytenTabPane(RSyntaxTextArea textArea) {
        this.textArea = textArea;

        scrollPane = new RTextScrollPane(textArea, true);
        errorStrip = new ErrorStrip(textArea);

        getContentPane().add(scrollPane);
        getContentPane().add(errorStrip, BorderLayout.LINE_END);
    }

    public RTextScrollPane getScrollPane() {
        return scrollPane;
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public ErrorStrip getErrorStrip() {
        return errorStrip;
    }

}
