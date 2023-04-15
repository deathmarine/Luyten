package us.deathmarine.luyten;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * Dispatcher
 */
public class MainWindow extends JFrame {

    private static final long serialVersionUID = 5265556630724988013L;

    private static final String TITLE = Luyten.VERSION;
    private static final String DEFAULT_TAB = "#DEFAULT";

    private final JProgressBar bar;
    private final JLabel label;
    FindBox findBox;
    private FindAllBox findAllBox;
    private final ConfigSaver configSaver;
    private final WindowPosition windowPosition;
    private final LuytenPreferences luytenPrefs;
    private final FileDialog fileDialog;
    private final FileSaver fileSaver;
    private final JTabbedPane jarsTabbedPane;
    private final Map<String, Model> jarModels;
    public MainMenuBar mainMenuBar;

    public MainWindow(File fileFromCommandLine) {
        configSaver = ConfigSaver.getLoadedInstance();
        windowPosition = configSaver.getMainWindowPosition();
        luytenPrefs = configSaver.getLuytenPreferences();

        jarModels = new HashMap<>();
        mainMenuBar = new MainMenuBar(this);
        this.setJMenuBar(mainMenuBar);

        this.adjustWindowPositionBySavedState();
        this.setHideFindBoxOnMainWindowFocus();
        this.setShowFindAllBoxOnMainWindowFocus();
        this.setQuitOnWindowClosing();
        this.setTitle(TITLE);
        this.setIconImage(new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/Luyten.png"))).getImage());

        JPanel panel1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        label = new JLabel();
        label.setHorizontalAlignment(JLabel.LEFT);
        panel1.setBorder(new BevelBorder(BevelBorder.LOWERED));
        panel1.setPreferredSize(new Dimension(this.getWidth() / 2, 20));
        panel1.add(label);

        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bar = new JProgressBar();

        bar.setStringPainted(true);
        bar.setOpaque(false);
        bar.setVisible(false);
        panel2.setPreferredSize(new Dimension(this.getWidth() / 3, 20));
        panel2.add(bar);

        jarsTabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        jarsTabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected int calculateTabAreaHeight(int tab_placement, int run_count, int max_tab_height) {
                if (!isDefaultTabLoaded())
                    return super.calculateTabAreaHeight(tab_placement, run_count, max_tab_height);
                else
                    return 0;
            }
        });
        createDefaultTab();
        this.getContentPane().add(jarsTabbedPane);

        JSplitPane spt = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel1, panel2) {
            private static final long serialVersionUID = 2189946972124687305L;
            private final int location = 400;

            {
                setDividerLocation(location);
            }

            @Override
            public int getDividerLocation() {
                return location;
            }

            @Override
            public int getLastDividerLocation() {
                return location;
            }
        };
        spt.setBorder(new BevelBorder(BevelBorder.LOWERED));
        spt.setPreferredSize(new Dimension(this.getWidth(), 24));
        this.add(spt, BorderLayout.SOUTH);
        Model jarModel = null;
        if (fileFromCommandLine != null) {
            jarModel = loadNewFile(fileFromCommandLine);
        }

        try {
            DropTarget dt = new DropTarget();
            dt.addDropTargetListener(new DropListener(this));
            this.setDropTarget(dt);
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }

        fileDialog = new FileDialog(this);
        fileSaver = new FileSaver(bar, label);

        if (jarModel != null) {
            this.setExitOnEscWhenEnabled(jarModel);
        }

        if (jarModel != null && (fileFromCommandLine.getName().toLowerCase().endsWith(".jar")
                || fileFromCommandLine.getName().toLowerCase().endsWith(".zip"))) {
            jarModel.startWarmUpThread();
        }

        if (RecentFiles.load() > 0) mainMenuBar.updateRecentFiles();
    }

    private void createDefaultTab() {
        jarsTabbedPane.addTab(DEFAULT_TAB, new Model(this));
    }

    private void removeDefaultTab() {
        jarsTabbedPane.remove(jarsTabbedPane.indexOfTab(DEFAULT_TAB));
    }

    private boolean isDefaultTabLoaded() {
        return jarsTabbedPane.indexOfTab(DEFAULT_TAB) >= 0;
    }

    public void onOpenFileMenu() {
        File selectedFile = fileDialog.doOpenDialog();
        if (selectedFile != null) {
            System.out.println("[Open]: Opening " + selectedFile.getAbsolutePath());
            this.loadNewFile(selectedFile);
        }
    }

    public Model loadNewFile(final File file) {
        if (!file.isFile()) return null;

        // In case we open the same file again
        // we remove the old entry to force a refresh
        String absPath = file.getAbsolutePath();
        if (jarModels.containsKey(absPath)) {
            Model m = jarModels.remove(absPath);
            int index = jarsTabbedPane.indexOfComponent(m);
            jarsTabbedPane.remove(index);
        }

        final Model jarModel = new Model(this);
        jarModel.loadFile(file);
        jarModels.put(absPath, jarModel);
        jarsTabbedPane.addTab(file.getName(), jarModel);
        jarsTabbedPane.setSelectedComponent(jarModel);

        final String tabName = file.getName();
        int index = jarsTabbedPane.indexOfComponent(jarModel);
        Model.Tab tabUI = new Model.Tab(tabName, () -> {
            int index1 = jarsTabbedPane.indexOfComponent(jarModel);
            jarModels.remove(absPath);
            jarsTabbedPane.remove(index1);
            jarModel.closeFile();
            if (jarsTabbedPane.getTabCount() == 0) {
                createDefaultTab();
            }
        });
        jarsTabbedPane.setTabComponentAt(index, tabUI);
        if (isDefaultTabLoaded() && jarsTabbedPane.getTabCount() > 1) {
            removeDefaultTab();
        }
        return jarModel;
    }

    public void onCloseFileMenu() {
        this.getSelectedModel().closeFile();
        jarModels.remove(getSelectedModel());
    }

    public void onSaveAsMenu() {
        RSyntaxTextArea pane = this.getSelectedModel().getCurrentTextArea();
        if (pane == null)
            return;
        String tabTitle = this.getSelectedModel().getCurrentTabTitle();
        if (tabTitle == null)
            return;

        String recommendedFileName = tabTitle.replace(".class", ".java");
        File selectedFile = fileDialog.doSaveDialog(recommendedFileName);
        if (selectedFile != null) {
            fileSaver.saveText(pane.getText(), selectedFile);
        }
    }

    public void onSaveAllMenu() {
        File openedFile = this.getSelectedModel().getOpenedFile();
        if (openedFile == null)
            return;

        String fileName = openedFile.getName();
        if (fileName.endsWith(".class")) {
            fileName = fileName.replace(".class", ".java");
        } else if (fileName.toLowerCase().endsWith(".jar")) {
            fileName = "decompiled-" + fileName.replaceAll("\\.[jJ][aA][rR]", ".zip");
        } else {
            fileName = "saved-" + fileName;
        }

        File selectedFileToSave = fileDialog.doSaveAllDialog(fileName);
        if (selectedFileToSave != null) {
            fileSaver.saveAllDecompiled(openedFile, selectedFileToSave);
        }
    }

    public void onExitMenu() {
        quit();
    }

    public void onSelectAllMenu() {
        try {
            RSyntaxTextArea pane = this.getSelectedModel().getCurrentTextArea();
            if (pane != null) {
                pane.requestFocusInWindow();
                pane.setSelectionStart(0);
                pane.setSelectionEnd(pane.getText().length());
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    public void onFindMenu() {
        try {
            RSyntaxTextArea pane = this.getSelectedModel().getCurrentTextArea();
            if (pane != null) {
                if (findBox == null)
                    findBox = new FindBox(this);
                findBox.showFindBox();
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    public void onFindAllMenu() {
        try {
            if (findAllBox == null)
                findAllBox = new FindAllBox(this);
            findAllBox.showFindBox();

        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    public void onLegalMenu() {
        new Thread(() -> {
            try {
                bar.setVisible(true);
                bar.setIndeterminate(true);
                String legalStr = getLegalStr();
                getSelectedModel().showLegal(legalStr);
            } finally {
                bar.setIndeterminate(false);
                bar.setVisible(false);
            }
        }).start();
    }

    public void onListLoadedClasses() {
        try {
            StringBuilder sb = new StringBuilder();
            ClassLoader myCL = Thread.currentThread().getContextClassLoader();
            bar.setVisible(true);
            bar.setIndeterminate(true);
            while (myCL != null) {
                sb.append("ClassLoader: ").append(myCL).append("\n");
                Iterator<?> iter = list(myCL);
                while (iter != null && iter.hasNext()) {
                    sb.append("\t").append(iter.next()).append("\n");
                }
                myCL = myCL.getParent();
            }
            this.getSelectedModel().show("Debug", sb.toString());
        } finally {
            bar.setIndeterminate(false);
            bar.setVisible(false);
        }
    }

    private static Iterator<?> list(ClassLoader CL) {
        Class<?> CL_class = CL.getClass();
        while (CL_class != ClassLoader.class) {
            CL_class = CL_class.getSuperclass();
        }
        Field ClassLoader_classes_field;
        try {
            ClassLoader_classes_field = CL_class.getDeclaredField("classes");
            ClassLoader_classes_field.setAccessible(true);
            Iterable<?> classes = (Iterable<?>) ClassLoader_classes_field.get(CL);
            return classes.iterator();
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
        return null;
    }

    private String getLegalStr() {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream("/licenses/Procyon.License.txt")));
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");

            sb.append("\n\n\n\n\n");

            reader = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream("/licenses/RSyntaxTextArea.License.txt")));
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");

            sb.append("\n\n\n\n\n");

            reader = new BufferedReader(
                    new InputStreamReader(getClass().getResourceAsStream("/licenses/PicoCLI.License.txt")));
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
        } catch (IOException e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
        return sb.toString();
    }

    public void onThemesChanged() {
        for (Model jarModel : jarModels.values()) {
            jarModel.changeTheme(luytenPrefs.getThemeXml());
            luytenPrefs.setFont_size(jarModel.getTheme().baseFont.getSize());
        }
    }

    public void onSettingsChanged() {
        for (Model jarModel : jarModels.values()) {
            jarModel.updateOpenClasses();
        }
    }

    public void onTreeSettingsChanged() {
        for (Model jarModel : jarModels.values()) {
            jarModel.updateTree();
        }
    }

    public void onFilesDropped(List<File> files) {
        if (files != null) {
            for (File file : files) {
                this.loadNewFile(file);
            }
        }
    }

    public void onFileDropped(File file) {
        if (file != null) {
            this.loadNewFile(file);
        }
    }

    public void onFileLoadEnded() {
        try {
            Model model = getSelectedModel();
            if (model != null && model.getFileName() != null) {
                this.setTitle(TITLE + " - " + model.getFileName());
            } else {
                this.setTitle(TITLE);
            }
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        }
    }

    public void onNavigationRequest(String uniqueStr) {
        this.getSelectedModel().navigateTo(uniqueStr);
    }

    private void adjustWindowPositionBySavedState() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (!windowPosition.isSavedWindowPositionValid()) {
            final Dimension center = new Dimension((int) (screenSize.width * 0.75), (int) (screenSize.height * 0.75));
            final int x = (int) (center.width * 0.2);
            final int y = (int) (center.height * 0.2);
            this.setBounds(x, y, center.width, center.height);

        } else if (windowPosition.isFullScreen()) {
            int heightMinusTray = screenSize.height;
            if (screenSize.height > 30)
                heightMinusTray -= 30;
            this.setBounds(0, 0, screenSize.width, heightMinusTray);
            this.setExtendedState(JFrame.MAXIMIZED_BOTH);

            this.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (MainWindow.this.getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                        windowPosition.setFullScreen(false);
                        if (windowPosition.isSavedWindowPositionValid()) {
                            MainWindow.this.setBounds(windowPosition.getWindowX(), windowPosition.getWindowY(),
                                    windowPosition.getWindowWidth(), windowPosition.getWindowHeight());
                        }
                        MainWindow.this.removeComponentListener(this);
                    }
                }
            });

        } else {
            this.setBounds(windowPosition.getWindowX(), windowPosition.getWindowY(), windowPosition.getWindowWidth(),
                    windowPosition.getWindowHeight());
        }
    }

    private void setHideFindBoxOnMainWindowFocus() {
        this.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (findBox != null && findBox.isVisible()) {
                    findBox.setVisible(false);
                }
            }
        });
    }

    private void setShowFindAllBoxOnMainWindowFocus() {
        this.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                if (findAllBox != null && findAllBox.isVisible()) {
                    findAllBox.setVisible(false);
                }
            }
        });
    }

    private void setQuitOnWindowClosing() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
    }

    private void quit() {
        try {
            windowPosition.readPositionFromWindow(this);
            configSaver.saveConfig();
        } catch (Exception e) {
            Luyten.showExceptionDialog("Exception!", e);
        } finally {
            try {
                this.dispose();
            } finally {
                System.exit(0);
            }
        }
    }

    private void setExitOnEscWhenEnabled(JComponent mainComponent) {
        Action escapeAction = new AbstractAction() {
            private static final long serialVersionUID = -3460391555954575248L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (luytenPrefs.isExitByEscEnabled()) {
                    quit();
                }
            }
        };
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
        mainComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(escapeKeyStroke, "ESCAPE");
        mainComponent.getActionMap().put("ESCAPE", escapeAction);
    }

    public Model getSelectedModel() {
        return (Model) jarsTabbedPane.getSelectedComponent();
    }

    public Collection<Model> getModels() {
        return jarModels.values();
    }

    public JProgressBar getBar() {
        return bar;
    }

    public JLabel getLabel() {
        return label;
    }

}
