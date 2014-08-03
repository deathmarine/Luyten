package com.modcrafting.luyten;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

public class FindAllBox extends JDialog {
    private static final long serialVersionUID = -4125409760166690462L;

    private JButton findButton;
    private JTextField textField;
    private MainWindow mainWindow;
    JProgressBar progressBar;
    private DefaultListModel classesList = new DefaultListModel();
    private JLabel statusLabel = new JLabel("");

    public void showFindBox() {
        this.setVisible(true);
        this.textField.requestFocus();
    }

    public void hideFindBox() {
        this.setVisible(false);
    }

    public void setStatus(String text)
    {
        if (text.length() > 51)
        {
            this.statusLabel.setText("Searching in file: " + text.substring(text.length()-51));
        }
        else
        {
            this.statusLabel.setText("Searching in file: " + text);
        }

        progressBar.setValue(progressBar.getValue()+1);
    }

    public void addClassName(String className)
    {
        this.classesList.addElement(className);
    }

    public void initProgressBar(Integer length)
    {
        progressBar.setMaximum(length);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
    }

    public FindAllBox(MainWindow mainWindow) {
        progressBar = new JProgressBar(0, 100);
//test
        JLabel label = new JLabel("Find What:");
        textField = new JTextField();

        findButton = new JButton("Find");
        findButton.addActionListener(new FindButton());
        this.getRootPane().setDefaultButton(findButton);

        JList list = new JList(classesList);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        list.setVisibleRowCount(-1);
        JScrollPane listScroller = new JScrollPane(list);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension center = new Dimension((int) (screenSize.width * 0.35),500);
        final int x = (int) (center.width * 0.2);
        final int y = (int) (center.height * 0.2);
        this.setBounds(x, y, center.width, center.height);
        this.setResizable(false);

        GroupLayout layout = new GroupLayout(getRootPane());
        getRootPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
                        .addComponent(label)
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(statusLabel)
                                .addComponent(textField)
                                .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                        .addComponent(listScroller)
                                                        .addComponent(progressBar)
                                        )))
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(findButton))

        );

        layout.linkSize(SwingConstants.HORIZONTAL, findButton);
        layout.setVerticalGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(label)
                                .addComponent(textField)
                                .addComponent(findButton))
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                                        .addComponent(listScroller)
                                        )))
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                .addComponent(statusLabel))
                        .addComponent(progressBar)
        );

        this.mainWindow = mainWindow;
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.setHideOnEscapeButton();
        this.adjustWindowPositionBySavedState();
        this.setSaveWindowPositionOnClosing();
        this.setName("Find All");
        this.setTitle("Find All");
    }

    private class FindButton extends AbstractAction {
        private static final long serialVersionUID = 75954129199541874L;

        @Override
        public void actionPerformed(ActionEvent event) {
            classesList.clear();
            MainMenuBar.searchall(textField.getText(),FindAllBox.this);
        }

    }

    private void setHideOnEscapeButton() {
        Action escapeAction = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                FindAllBox.this.setVisible(false);
            }
        };

        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", escapeAction);
    }

    private void adjustWindowPositionBySavedState() {
        WindowPosition windowPosition = ConfigSaver.getLoadedInstance().getFindWindowPosition();

        if (windowPosition.isSavedWindowPositionValid()) {
            this.setLocation(windowPosition.getWindowX(), windowPosition.getWindowY());
        }
    }

    private void setSaveWindowPositionOnClosing() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDeactivated(WindowEvent e) {
                WindowPosition windowPosition = ConfigSaver.getLoadedInstance().getFindWindowPosition();
                windowPosition.readPositionFromDialog(FindAllBox.this);
            }
        });
    }
}
