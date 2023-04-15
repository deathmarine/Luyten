package us.deathmarine.luyten;

import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

/**
 * this is the Find All Dialog
 * <p>
 * Change with 1.1
 * Adjust the find all box width
 * </p>
 *
 * @author clevertension
 * @version 1.1
 */
public class FindAllBox extends JDialog {

    private static final long serialVersionUID = -4125409760166690462L;
    private static final int MIN_WIDTH = 640;

    private final JButton findButton;
    private final JTextField textField;
    private final JCheckBox mcase;
    private final JCheckBox regex;
    private final JCheckBox wholew;
    private final JCheckBox classname;
    private final JList<String> list;
    private final FindAllLabeledProgressBar labeledProgressBar;
    private boolean searching;
    boolean locked;

    private final DefaultListModel<String> classesList = new DefaultListModel<>();

    private Thread tmp_thread;

    private final MainWindow mainWindow;

    public FindAllBox(final MainWindow mainWindow) {
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.setHideOnEscapeButton();

        labeledProgressBar = new FindAllLabeledProgressBar(new JProgressBar(0, 100));
        this.mainWindow = mainWindow;

        JLabel label = new JLabel("Find What:");
        textField = new JTextField();
        findButton = new JButton("Find");
        findButton.addActionListener(new FindButton());

        mcase = new JCheckBox("Match Case");
        regex = new JCheckBox("Regex");
        wholew = new JCheckBox("Whole Words");
        classname = new JCheckBox("Classnames");

        this.getRootPane().setDefaultButton(findButton);

        list = new JList<>(classesList);
        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL_WRAP);
        list.setVisibleRowCount(-1);
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                @SuppressWarnings("unchecked")
                JList<String> list = (JList<String>) evt.getSource();
                if (evt.getClickCount() == 2) {
                    int index = list.locationToIndex(evt.getPoint());
                    String entryName = list.getModel().getElementAt(index);
                    String[] array = entryName.split("/");
                    if (entryName.toLowerCase().endsWith(".class")) {
                        String internalName = StringUtilities.removeRight(entryName, ".class");
                        TypeReference type = mainWindow.getSelectedModel().getMetadataSystem().lookupType(internalName);
                        try {
                            mainWindow.getSelectedModel().extractClassToTextPane(type, array[array.length - 1],
                                    entryName,
                                    null);
                        } catch (Exception ignored) {
                            for (Model m : mainWindow.getModels()) {
                                try {
                                    m.extractClassToTextPane(type, array[array.length - 1], entryName, null);
                                } catch (Exception ignored1) {
                                }
                            }
                        }

                    } else {
                        try {
                            JarFile jfile = new JarFile(mainWindow.getSelectedModel().getOpenedFile());
                            mainWindow.getSelectedModel().extractSimpleFileEntryToTextPane(
                                    jfile.getInputStream(jfile.getEntry(entryName)), array[array.length - 1],
                                    entryName);
                            jfile.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
        });
        list.setLayoutOrientation(JList.VERTICAL);
        JScrollPane listScroller = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) (screenSize.width * 0.35);
        if (width < MIN_WIDTH) {
            width = MIN_WIDTH;
        }
        final Dimension center = new Dimension(width, 500);
        final int x = (int) (center.width * 0.2);
        final int y = (int) (center.height * 0.2);
        this.setBounds(x, y, center.width, center.height);
        this.setResizable(false);

        GroupLayout layout = new GroupLayout(getRootPane());
        getRootPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
                layout.createSequentialGroup().addComponent(label)
                        .addGroup(
                                layout.createParallelGroup(Alignment.LEADING)
                                        .addComponent(labeledProgressBar.getStatusLabel())
                                        .addComponent(textField)
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                                                        .addComponent(mcase))
                                                .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(wholew))
                                                .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(regex))
                                                .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(classname)))
                                        .addGroup(layout.createSequentialGroup()
                                                .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(listScroller)
                                                        .addComponent(labeledProgressBar.getProgressBar()))))
                        .addGroup(layout.createParallelGroup(Alignment.LEADING).addComponent(findButton))

        );

        layout.linkSize(SwingConstants.HORIZONTAL, findButton);
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(label).addComponent(textField)
                        .addComponent(findButton))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(mcase).addComponent(wholew)
                        .addComponent(regex).addComponent(classname))
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(Alignment.BASELINE).addComponent(listScroller))))
                .addGroup(layout.createParallelGroup(Alignment.LEADING))
                .addComponent(labeledProgressBar.getStatusLabel())
                .addComponent(labeledProgressBar.getProgressBar()));
        this.adjustWindowPositionBySavedState();
        this.setSaveWindowPositionOnClosing();

        this.setName("Find All");
        this.setTitle("Find All");
    }

    private class FindButton extends AbstractAction {
        private static final long serialVersionUID = 75954129199541874L;

        @Override
        public void actionPerformed(ActionEvent event) {
            tmp_thread = new Thread(() -> {
                if (findButton.getText().equals("Stop")) {
                    if (tmp_thread != null)
                        tmp_thread.interrupt();
                    setStatus("Stopped.");
                    findButton.setText("Find");
                    locked = false;
                } else {
                    File inFile = mainWindow.getSelectedModel().getOpenedFile();
                    if (inFile == null)
                        return;
                    findButton.setText("Stop");
                    classesList.clear();
                    ConfigSaver configSaver = ConfigSaver.getLoadedInstance();
                    DecompilerSettings settings = configSaver.getDecompilerSettings();
                    boolean filter = ConfigSaver.getLoadedInstance().getLuytenPreferences()
                            .isFilterOutInnerClassEntries();
                    try {
                        JarFile jarFile = new JarFile(inFile);
                        Enumeration<JarEntry> entLength = jarFile.entries();
                        initProgressBar(Collections.list(entLength).size());
                        Enumeration<JarEntry> ent = jarFile.entries();
                        while (ent.hasMoreElements() && findButton.getText().equals("Stop")) {
                            JarEntry entry = ent.nextElement();
                            String name = entry.getName();
                            setStatus(name);
                            if (filter && name.contains("$"))
                                continue;
                            if (locked || classname.isSelected()) {
                                locked = true;
                                if (search(entry.getName()))
                                    addClassName(entry.getName());
                            } else {
                                if (entry.getName().endsWith(".class")) {
                                    synchronized (settings) {
                                        String internalName = StringUtilities.removeRight(entry.getName(), ".class");
                                        try {
                                            TypeReference type =
                                                    mainWindow.getSelectedModel().getMetadataSystem().lookupType(internalName);
                                            TypeDefinition resolvedType;
                                            if (type != null && ((resolvedType = type.resolve()) != null)) {
                                                StringWriter stringwriter = new StringWriter();
                                                DecompilationOptions decompilationOptions;
                                                decompilationOptions = new DecompilationOptions();
                                                decompilationOptions.setSettings(settings);
                                                decompilationOptions.setFullDecompilation(true);
                                                PlainTextOutput plainTextOutput = new PlainTextOutput(stringwriter);
                                                plainTextOutput.setUnicodeOutputEnabled(
                                                        decompilationOptions.getSettings().isUnicodeOutputEnabled());
                                                settings.getLanguage().decompileType(resolvedType, plainTextOutput,
                                                        decompilationOptions);
                                                if (search(stringwriter.toString()))
                                                    addClassName(entry.getName());
                                            }
                                        } catch (IllegalStateException ise) {
                                            if (ise.getMessage().contains("Invalid BootstrapMethods attribute entry: "
                                                    + "2 additional arguments required for method "
                                                    + "java/lang/invoke/StringConcatFactory.makeConcatWithConstants, "
                                                    + "but only 1 specified.")) {
                                                // Known issue of Procyon <= 0.5.35 and fix not yet released, refer to
                                                // https://web.archive.org/web/20200722211732/https://bitbucket.org/mstrobel/procyon/issues/336/
                                                // Searching in a WAR or JAR file could pop-up a lot of error dialogs
                                                // for a lot of class files, we simply say nothing here
                                                addClassName(entry.getName() + "  (search failed due to known "
                                                        + "Exception in Procyon <= 0.5.35. Opening file will fail "
                                                        + "too)");
                                            } else {
                                                // all other IllegalStateException cases
                                                addClassName(entry.getName() + "  (search failed due to Exception. "
                                                        + "Opening file will fail too)");
                                                Luyten.showExceptionDialog("Caught Exception on: " + entry.getName(),
                                                        ise);
                                            }
                                        } catch (Exception e) {
                                            addClassName(entry.getName() + "  (search failed due to Exception. "
                                                    + "Opening file will fail too)");
                                            Luyten.showExceptionDialog("Caught Exception on: " + entry.getName(), e);
                                        }
                                    }
                                } else {

                                    StringBuilder sb = new StringBuilder();
                                    double ascii = 0;
                                    double other = 0;
                                    try (InputStreamReader inputStreamReader = new InputStreamReader(
                                            jarFile.getInputStream(entry));
                                         BufferedReader reader = new BufferedReader(inputStreamReader)) {
                                        String line;
                                        while ((line = reader.readLine()) != null) {
                                            sb.append(line).append("\n");
                                            // Source: https://stackoverflow.com/a/13533390/5894824
                                            for (byte b : line.getBytes()) {
                                                if (b == 0x09 || b == 0x0A || b == 0x0C || b == 0x0D || (b >= 0x20 && b <= 0x7E))
                                                    ascii++;
                                                else other++;
                                            }
                                        }
                                    }

                                    if ((other == 0 || ascii / (ascii + other) > 0.5) && search(sb.toString()))
                                        addClassName(entry.getName());
                                }
                            }
                        }
                        setSearching(false);
                        if (findButton.getText().equals("Stop")) {
                            setStatus("Done.");
                            findButton.setText("Find");
                            locked = false;
                        }
                        jarFile.close();
                        locked = false;
                    } catch (Exception e) {
                        Luyten.showExceptionDialog("Exception!", e);
                    }

                }
            });
            tmp_thread.start();

        }

    }

    private boolean search(String bulk) {
        String a = textField.getText();
        String b = bulk;
        if (regex.isSelected())
            return Pattern.matches(a, b);
        if (wholew.isSelected())
            a = " " + a + " ";
        if (!mcase.isSelected()) {
            a = a.toLowerCase();
            b = b.toLowerCase();
        }
        return b.contains(a);
    }

    private void setHideOnEscapeButton() {
        Action escapeAction = new AbstractAction() {
            private static final long serialVersionUID = 6846566740472934801L;

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

    public void showFindBox() {
        this.setVisible(true);
        this.textField.requestFocus();
    }

    public void hideFindBox() {
        this.setVisible(false);
    }

    public void setStatus(String text) {
        labeledProgressBar.setStatus(text);
    }

    public void addClassName(String className) {
        this.classesList.addElement(className);
    }

    public void initProgressBar(Integer length) {
        labeledProgressBar.initProgressBar(length);
    }

    public boolean isSearching() {
        return searching;
    }

    public void setSearching(boolean searching) {
        this.searching = searching;
    }

}
