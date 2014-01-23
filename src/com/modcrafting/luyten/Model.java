package com.modcrafting.luyten;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;

import java.net.URI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipOutputStream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.strobel.assembler.metadata.*;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.languages.Language;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

public class Model extends JFrame implements WindowListener {
    private static final long serialVersionUID = 6896857630400910200L;

    final LuytenTypeLoader typeLoader = new LuytenTypeLoader();
    final Map<String, Language> languageLookup = new HashMap<String, Language>();
    static File base;
    MetadataSystem metadataSystem = new MetadataSystem(typeLoader);

    JTree tree;
    JTabbedPane house;
    File file;
    JSplitPane jsp;
    DecompilerSettings settings;
    DecompilationOptions decompilationOptions;
    static Model frame;
    Theme theme;
    JCheckBox flattenSwitchBlocks;
    JCheckBox forceExplicitImports;
    JCheckBox forceExplicitTypes;
    JCheckBox showSyntheticMembers;
    JCheckBox showNestedTypes;
    JCheckBox retainRedundantCasts;
    JCheckBox showDebugInfo;
    JRadioButtonMenuItem java;
    JRadioButtonMenuItem bytecode;
    JRadioButtonMenuItem bytecodeAST;
    JProgressBar bar;
    JLabel label;
    HashSet<OpenFile> hmap = new HashSet<OpenFile>();
    boolean open = false;
    private ButtonGroup languagesGroup;
    private State state;

    public Model() {
        frame = this;
        setup();
    }

    public Model(String string) {
        frame = this;
        setup();
        file = new File(string);
		new FileLoad(false).loadFile(file);
    }

    public void setup() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension center = new Dimension((int) (screenSize.width * 0.75), (int) (screenSize.height * 0.75));
        final int x = (int) (center.width * 0.2);
        final int y = (int) (center.height * 0.2);
        this.setBounds(x, y, center.width, center.height);
        this.setTitle("Luyten");
        this.addWindowListener(this);
        DropTarget dt = new DropTarget();
        try {
            dt.addDropTargetListener(new DropListener());
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        }
        this.setDropTarget(dt);
        try {
            theme = Theme.load(getClass().getResourceAsStream("/themes/eclipse.xml"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        tree = new JTree();
        tree.setModel(new DefaultTreeModel(null));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new CellRenderer());
        TreeListener tl = new TreeListener();
        tree.addMouseListener(tl);

        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, 1));
        panel2.setBorder(BorderFactory.createTitledBorder("Structure"));
        panel2.add(new JScrollPane(tree));

        house = new JTabbedPane();
        house.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, 1));
        panel.setBorder(BorderFactory.createTitledBorder("Code"));
        panel.add(house);
        jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel2, panel);
        jsp.setDividerLocation(250 % this.getWidth());
        this.getContentPane().add(jsp);
        JPanel pane = new JPanel();
        pane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        pane.setPreferredSize(new Dimension(frame.getWidth(), 24));
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
        
        JPanel panel1 = new JPanel();
        label = new JLabel(" ");
        label.setHorizontalAlignment(JLabel.LEFT);
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        panel1.setBorder(new BevelBorder(BevelBorder.LOWERED));
        panel1.setPreferredSize(new Dimension(frame.getWidth()/2, 20));
        panel1.add(label);
        pane.add(panel1);
        
        panel1 = new JPanel();
        bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setOpaque(false);
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        panel1.setPreferredSize(new Dimension(frame.getWidth()/2, 20));
        panel1.add(bar);
        pane.add(panel1);
        this.add(pane, BorderLayout.SOUTH);
        
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem menuItem = new JMenuItem("Open File...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new FileLoad(true));
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("Close");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new FileClose());
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("Save As...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new FileExtractFile());
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Save All...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
        //menuItem.setEnabled(false);
        menuItem.addActionListener(new FileExtractJar());
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("Recent Files");
        menuItem.setEnabled(false);
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("Exit");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        menuItem.addActionListener(new Quit());
        fileMenu.add(menuItem);

        menuBar.add(fileMenu);


        fileMenu = new JMenu("Edit");

        menuItem = new JMenuItem("Cut");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        menuItem.setEnabled(false);
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Copy");
        menuItem.addActionListener(new DefaultEditorKit.CopyAction());
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        fileMenu.add(menuItem);

        menuItem = new JMenuItem("Paste");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        menuItem.setEnabled(false);
        fileMenu.add(menuItem);

        fileMenu.addSeparator();

        menuItem = new JMenuItem("Select All");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int pos = house.getSelectedIndex();
                RTextScrollPane co = (RTextScrollPane) house.getComponentAt(pos);
                RSyntaxTextArea pane = (RSyntaxTextArea) co.getViewport().getView();
                pane.setSelectionStart(0);
                pane.setSelectionEnd(pane.getText().length());
            }
        });
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("Find...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                new FindBox(Model.frame);
            }
        });
        fileMenu.add(menuItem);
        menuBar.add(fileMenu);

        fileMenu = new JMenu("Themes");
        languagesGroup = new ButtonGroup();
        JRadioButtonMenuItem a = new JRadioButtonMenuItem(new ThemeAction("Default", "default.xml"));
        languagesGroup.add(a);
        fileMenu.add(a);
        a = new JRadioButtonMenuItem(new ThemeAction("Dark", "dark.xml"));
        languagesGroup.add(a);
        fileMenu.add(a);
        a = new JRadioButtonMenuItem(new ThemeAction("Eclipse", "eclipse.xml"));
        a.setSelected(true);
        languagesGroup.add(a);
        fileMenu.add(a);
        a = new JRadioButtonMenuItem(new ThemeAction("Visual Studio", "vs.xml"));
        languagesGroup.add(a);
        fileMenu.add(a);
        menuBar.add(fileMenu);

        settings = new DecompilerSettings();
        if (settings.getFormattingOptions() == null)
            settings.setFormattingOptions(JavaFormattingOptions.createDefault());
        fileMenu = new JMenu("Settings");
        flattenSwitchBlocks = new JCheckBox("Flatten Switch Blocks");
        fileMenu.add(flattenSwitchBlocks);
        forceExplicitImports = new JCheckBox("Force Explicit Imports");
        fileMenu.add(forceExplicitImports);
        forceExplicitTypes = new JCheckBox("Force Explicit Types");
        fileMenu.add(forceExplicitTypes);
        showSyntheticMembers = new JCheckBox("Show Synthetic Members");
        fileMenu.add(showSyntheticMembers);
        showNestedTypes = new JCheckBox("Show Nested Types");
        fileMenu.add(showSyntheticMembers);
        retainRedundantCasts = new JCheckBox("Retain Redundant Casts");
        fileMenu.add(retainRedundantCasts);
        JMenu debugSettingsMenu = new JMenu("Debug Settings");
        showDebugInfo = new JCheckBox("Include Error Diagnostics");
        debugSettingsMenu.add(showDebugInfo);
        fileMenu.add(debugSettingsMenu);
        fileMenu.addSeparator();

        languageLookup.put(Languages.java().getName(), Languages.java());
        languageLookup.put(Languages.bytecode().getName(), Languages.bytecode());
        languageLookup.put(Languages.bytecodeAst().getName(), Languages.bytecodeAst());

        languagesGroup = new ButtonGroup();
        java = new JRadioButtonMenuItem(Languages.java().getName());
        java.getModel().setActionCommand(Languages.java().getName());
        java.setSelected(true);
        languagesGroup.add(java);
        fileMenu.add(java);
        bytecode = new JRadioButtonMenuItem(Languages.bytecode().getName());
        bytecode.getModel().setActionCommand(Languages.bytecode().getName());
        languagesGroup.add(bytecode);
        fileMenu.add(bytecode);
        bytecodeAST = new JRadioButtonMenuItem(Languages.bytecodeAst().getName());
        bytecodeAST.getModel().setActionCommand(Languages.bytecodeAst().getName());
        languagesGroup.add(bytecodeAST);
        fileMenu.add(bytecodeAST);
        
        JMenu debugLanguagesMenu = new JMenu("Debug Languages");
        for (final Language language : Languages.debug()) {
            final JRadioButtonMenuItem m = new JRadioButtonMenuItem(language.getName());
            m.getModel().setActionCommand(language.getName());
            languagesGroup.add(m);
            debugLanguagesMenu.add(m);
            languageLookup.put(language.getName(), language);
        }
        fileMenu.add(debugLanguagesMenu);
        menuBar.add(fileMenu);
        fileMenu = new JMenu("Help");
        menuItem = new JMenuItem("Legal");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {

                StringBuilder sb = new StringBuilder();
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(getClass().getResourceAsStream("/distfiles/Procyon.License.txt")));
                    String line;
                    while ((line = reader.readLine()) != null)
                        sb.append(line).append("\n");
                    sb.append("\n\n\n\n\n");
                    reader = new BufferedReader(
                            new InputStreamReader(getClass().getResourceAsStream("/distfiles/RSyntaxTextArea.License.txt")));
                    while ((line = reader.readLine()) != null)
                        sb.append(line).append("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                OpenFile open = new OpenFile("Legal", sb.toString(), theme);
                hmap.add(open);
                addTab("Legal", open.scrollPane);

            }
        });
        fileMenu.add(menuItem);
        menuItem = new JMenuItem("About");
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                JOptionPane.showMessageDialog(null, 
                		"Luyten Gui \n" +
                		"by Deathmarine\n\n" +
                        "Powered By\nProcyon\n" +
                        "(c) 2013 Mike Strobel\n\n" +
                        "RSyntaxTextArea\n" +
                        "(c) 2012 Robert Futrell\n" +
                        "All rights reserved.");
            }
        });
        fileMenu.add(menuItem);
        menuBar.add(fileMenu);

        this.setJMenuBar(menuBar);
        this.setVisible(true);
        bar.setVisible(false);


        decompilationOptions = new DecompilationOptions();
        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	new Model();
            }
        });
    }

    public void addTab(String title, RTextScrollPane rTextScrollPane) {
        if (house.indexOfTab(title) < 0) {
            house.addTab(title, rTextScrollPane);
            house.setSelectedIndex(house.indexOfTab(title));
            int index = house.indexOfTab(title);
            Tab ct = new Tab(title);
            ct.getButton().addMouseListener(new CloseTab(title));
            house.setTabComponentAt(index, ct);
        } else {
            house.setSelectedIndex(house.indexOfTab(title));
        }
    }

    public void closeOpenTab(int index) {
        RTextScrollPane co = (RTextScrollPane) house.getComponentAt(index);
        RSyntaxTextArea pane = (RSyntaxTextArea) co.getViewport().getView();
        OpenFile open = null;
        for (OpenFile file : hmap)
            if (pane.equals(file.textArea))
                open = file;
        if (open != null && hmap.contains(open))
            hmap.remove(open);
        house.remove(co);
    }

    public String getName(String path) {
        if (path == null)
            return "";
        int i = path.lastIndexOf("/");
        if (i == -1)
            i = path.lastIndexOf("\\");
        if (i != -1)
            return path.substring(i + 1);
        return path;
    }

    @Override
    public void windowActivated(WindowEvent arg0) {}

    @Override
    public void windowClosed(WindowEvent event) {}

    @Override
    public void windowClosing(WindowEvent event) {
        new Quit().actionPerformed(null);
    }

    @Override
    public void windowDeactivated(WindowEvent arg0) {}

    @Override
    public void windowDeiconified(WindowEvent arg0) {}

    @Override
    public void windowIconified(WindowEvent arg0) {}

    @Override
    public void windowOpened(WindowEvent arg0) {}

    private class TreeListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            TreePath trp = tree.getPathForLocation(event.getX(), event.getY());
            if (trp == null)
                return;
            if (SwingUtilities.isLeftMouseButton(event)
                    && event.getClickCount() == 2) {
                String st = trp.toString().replace(file.getName(), "");
                final String[] args = st.replace("[", "").replace("]", "").split(",");
                try {
                    if (args.length > 1) {
                        String name = new String();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < args.length; i++) {
                            if (i == args.length - 1) {
                                name = args[i].trim();
                            } else {
                                sb.append(args[i].trim()).append("/");
                            }
                        }
                        settings.setFlattenSwitchBlocks(flattenSwitchBlocks.isSelected());
                        settings.setForceExplicitImports(forceExplicitImports.isSelected());
                        settings.setShowSyntheticMembers(showSyntheticMembers.isSelected());
                        settings.setExcludeNestedTypes(showNestedTypes.isSelected());
                        settings.setForceExplicitTypeArguments(forceExplicitTypes.isSelected());
                        settings.setRetainRedundantCasts(retainRedundantCasts.isSelected());
                        settings.setIncludeErrorDiagnostics(showDebugInfo.isSelected());
                        //
                        // Note: You shouldn't ever need to set this.  It's only for languages that support catch
                        //       blocks without an exception variable.  Java doesn't allow this.  I think Scala does.
                        //
                        // settings.setAlwaysGenerateExceptionVariableForCatchBlocks(true);
                        //

                        final ButtonModel selectedLanguage = languagesGroup.getSelection();
                        if (selectedLanguage != null) {
                            final Language language = languageLookup.get(selectedLanguage.getActionCommand());

                            if (language != null)
                                settings.setLanguage(language);
                        }

                        if (java.isSelected()) {
                            settings.setLanguage(Languages.java());
                        } else if (bytecode.isSelected()) {
                            settings.setLanguage(Languages.bytecode());
                        } else if (bytecodeAST.isSelected()) {
                            settings.setLanguage(Languages.bytecodeAst());
                        }
                        if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
                            try {
                                if (state == null) {
                                    JarFile jfile = new JarFile(file);
                                    ITypeLoader jarLoader = new JarTypeLoader(jfile);

                                    typeLoader.getTypeLoaders().add(jarLoader);
                                    state = new State(file.getCanonicalPath(), file, jfile, jarLoader);
                                }

                                JarEntry entry = state.jarFile.getJarEntry(sb.toString().replace(".", "/") + name);

                                if (entry.getName().endsWith(".class")) {
                                    String internalName = StringUtilities.removeRight(entry.getName(), ".class");
                                    TypeReference type = metadataSystem.lookupType(internalName);
                                    TypeDefinition resolvedType = null;
                                    if ((type == null) || ((resolvedType = type.resolve()) == null)) {
                                        JOptionPane.showMessageDialog(null, "Unable to resolve type.", "Error!", JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }
                                    StringWriter stringwriter = new StringWriter();
                                    settings.getLanguage().decompileType(resolvedType, new PlainTextOutput(stringwriter), decompilationOptions);
                                    OpenFile open = new OpenFile(name, stringwriter.getBuffer().toString(), theme);
                                    hmap.add(open);
                                    addTab(name, open.scrollPane);
                                    stringwriter.close();
                                } else {
                                    InputStream in = state.jarFile.getInputStream(entry);
                                    StringBuilder sd = new StringBuilder();
                                    if (in != null) {
                                        BufferedReader reader = new BufferedReader(
                                                new InputStreamReader(in));
                                        String line;
                                        while ((line = reader.readLine()) != null)
                                            sd.append(line).append("\n");
                                        reader.close();
                                    }
                                    OpenFile open = new OpenFile(name, sd.toString(), theme);
                                    hmap.add(open);
                                    addTab(name, open.scrollPane);
                                }
                            } catch (IOException e1) {
                                JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } else {
                        String name = file.getName();
                        if (name.endsWith(".class")) {
                            TypeReference type = metadataSystem.lookupType(file.getPath());
                            TypeDefinition resolvedType = null;
                            if ((type == null) || ((resolvedType = type.resolve()) == null)) {
                                JOptionPane.showMessageDialog(null, "Unable to resolve type.", "Error!", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            StringWriter stringwriter = new StringWriter();
                            settings.getLanguage().decompileType(resolvedType, new PlainTextOutput(stringwriter), decompilationOptions);
                            OpenFile open = new OpenFile(name, stringwriter.getBuffer().toString(), theme);
                            hmap.add(open);
                            addTab(name, open.scrollPane);
                            stringwriter.close();
                        } else {
                            StringBuilder sd = new StringBuilder();
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(new FileInputStream(file)));
                            String line;
                            while ((line = reader.readLine()) != null)
                                sd.append(line).append("\n");
                            reader.close();
                            OpenFile open = new OpenFile(name, sd.toString(), theme);
                            hmap.add(open);
                            addTab(name, open.scrollPane);
                        }
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, e.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
                }

            } else {
                tree.getSelectionModel().setSelectionPath(trp);
            }
        }
    }

    private final class State implements AutoCloseable {
        private final String key;
        private final File file;
        final JarFile jarFile;
        final ITypeLoader typeLoader;

        private State(String key, File file, JarFile jarFile, ITypeLoader typeLoader) {
            this.key = VerifyArgument.notNull(key, "key");
            this.file = VerifyArgument.notNull(file, "file");
            this.jarFile = jarFile;
            this.typeLoader = typeLoader;
        }

        @Override
        public void close() {
            if (typeLoader != null) {
                Model.this.typeLoader.getTypeLoaders().remove(typeLoader);
            }
            Closer.tryClose(jarFile);
        }

		public File getFile() {
			return file;
		}

		public String getKey() {
			return key;
		}
    }

    private class Tab extends JPanel {
        private static final long serialVersionUID = -514663009333644974L;
        private JLabel closeButton = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/icon_close.png"))));
        private JLabel tabTitle = new JLabel();
        private String title = "";


        public Tab(String t) {
            super(new GridBagLayout());
            this.setOpaque(false);

            this.title = t;
            this.tabTitle = new JLabel(title);

            this.createTab();
        }

        public JLabel getButton() {
            return this.closeButton;
        }

        public void createTab() {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            this.add(tabTitle, gbc);
            gbc.gridx++;
            gbc.insets = new Insets(0, 5, 0, 0);
            gbc.anchor = GridBagConstraints.EAST;
            this.add(closeButton, gbc);
        }
    }

    private class CloseTab extends MouseAdapter {
        String title;

        public CloseTab(String title) {
            this.title = title;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            int index = house.indexOfTab(title);
            closeOpenTab(index);
        }
    }

    private class FileLoad implements ActionListener {
        JFileChooser fc;

        public FileLoad(boolean dialog) {
            if (dialog) {
                fc = new JFileChooser();
                fc.addChoosableFileFilter(new FileChooserFileFilter("*.jar"));
                fc.addChoosableFileFilter(new FileChooserFileFilter("*.zip"));
                fc.addChoosableFileFilter(new FileChooserFileFilter("*.class"));
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setMultiSelectionEnabled(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int returnVal = fc.showOpenDialog(Model.frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                file = fc.getSelectedFile();
                if (open)
                    new FileClose().actionPerformed(e);
                loadFile(file);
            }
        }

        public DefaultMutableTreeNode load(DefaultMutableTreeNode node, List<String> args) {
            if (args.size() > 0) {
                String name = args.remove(0);
                DefaultMutableTreeNode nod = getChild(node, name);
                if (nod == null)
                    nod = new DefaultMutableTreeNode(name);
                node.add(load(nod, args));
            }
            return node;
        }

        @SuppressWarnings("unchecked")
        public DefaultMutableTreeNode getChild(DefaultMutableTreeNode node, String name) {
            Enumeration<DefaultMutableTreeNode> entry = node.children();
            while (entry.hasMoreElements()) {
                DefaultMutableTreeNode nods = entry.nextElement();
                if (nods.getUserObject().equals(name)) {
                    return nods;
                }
            }
            return null;
        }

        public void loadFile(final File file) {
        	new Thread(new Runnable(){
				@Override
				public void run() {
			        if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
			            JarFile jfile;
						try {
							jfile = new JarFile(file); 
			                label.setText("Loading: "+jfile.getName());
			                bar.setVisible(true);
							Enumeration<JarEntry> entry = jfile.entries();
				            DefaultMutableTreeNode top = new DefaultMutableTreeNode(getName(file.getName()));
				            List<String> mass = new ArrayList<String>();
				            while (entry.hasMoreElements()){
				            	JarEntry e = entry.nextElement();
				            	if(!e.isDirectory())
				                    mass.add(e.getName());
				      
				            }
				            List<String> sort = new ArrayList<String>();
				            Collections.sort(mass, String.CASE_INSENSITIVE_ORDER);
				            for(String m : mass)
				            	if(m.contains("META-INF") && !sort.contains(m))
				            		sort.add(m);
				            Set<String> set = new HashSet<String>();
				            for(String m : mass){
				            	if(m.contains("/")){
				                	set.add(m.substring(0, m.lastIndexOf("/")+1));
				            	}
				            }
				            List<String> packs = Arrays.asList(set.toArray(new String[]{}));
				            Collections.sort(packs, String.CASE_INSENSITIVE_ORDER);                
				            Collections.sort(packs, new Comparator<String>(){
				            	public int compare(String o1, String o2) {
				            		return o2.split("/").length - o1.split("/").length;
				            	}
				            });
				            for(String pack : packs)
				            	for(String m : mass)
				            		if(!m.contains("META-INF") && m.contains(pack) && !m.replace(pack, "").contains("/"))
				            			sort.add(m);
				            for(String m : mass)
				            	if(!m.contains("META-INF") && !m.contains("/") && !sort.contains(m))
				            		sort.add(m);
				            for (String pack : sort) {
				                LinkedList<String> list = new LinkedList<String>(Arrays.asList(pack.split("/")));
				                load(top, list);
				            }
				            tree.setModel(new DefaultTreeModel(top));
				            if (state == null) {
				                ITypeLoader jarLoader = new JarTypeLoader(jfile);
				                typeLoader.getTypeLoaders().add(jarLoader);
				                state = new State(file.getCanonicalPath(), file, jfile, jarLoader);
				            }
				            open = true;
						} catch (IOException e1) {
							e1.printStackTrace();
						}
			           
			        } else {
			            DefaultMutableTreeNode top = new DefaultMutableTreeNode(getName(file.getName()));
			            tree.setModel(new DefaultTreeModel(top));
			            settings.setTypeLoader(new InputTypeLoader());
			            open = true;
			        }
            		label.setText("Complete");
    				bar.setVisible(false);				
				}

        	}).start();
        }
    }
    
    public class FileClose implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            for (OpenFile co : hmap) {
                int pos = house.indexOfTab(co.name);
                if (pos >= 0)
                    house.remove(pos);
            }

            final State oldState = state;
            Model.this.state = null;
            if (oldState != null) {
                Closer.tryClose(oldState);
            }

            hmap.clear();
            tree.setModel(new DefaultTreeModel(null));
            open = false;
            metadataSystem = new MetadataSystem(typeLoader);
        }
    }

    private class FileExtractFile implements ActionListener{
        JFileChooser fc;
    	public FileExtractFile(){
    		fc = new JFileChooser();
            fc.addChoosableFileFilter(new FileChooserFileFilter("*.txt"));
            fc.addChoosableFileFilter(new FileChooserFileFilter("*.java"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
    	}
		@Override
		public void actionPerformed(ActionEvent event) {
			int index = house.getSelectedIndex();
			if(index < 0)
				return;
            RTextScrollPane co = (RTextScrollPane) house.getComponentAt(index);
            final RSyntaxTextArea pane = (RSyntaxTextArea) co.getViewport().getView();
            String title = house.getTitleAt(index);
			fc.setSelectedFile(new File(title.replace(".class",".java")));
            int returnVal = fc.showSaveDialog(Model.frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
            	new Thread(new Runnable(){
					@Override
					public void run() {
		                File fil = fc.getSelectedFile();
		                label.setText("Extracting: "+fil.getName());
		                bar.setVisible(true);
		                try {
		                    BufferedWriter bw = new BufferedWriter(new FileWriter(fil));
		                    bw.write(pane.getText());
		                    bw.flush();
		                    bw.close();
		                } catch (Exception e1) {
		                    JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
		                }
                		label.setText("Complete");
                		bar.setVisible(false);				
					}
    		
            	}).start();
            }
			
		}
    	
    }

    private class FileExtractJar implements ActionListener{
        JFileChooser fc;
    	public FileExtractJar(){
    		fc = new JFileChooser();
            fc.addChoosableFileFilter(new FileChooserFileFilter("*.jar"));
            fc.addChoosableFileFilter(new FileChooserFileFilter("*.zip"));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setMultiSelectionEnabled(false);
    	}
		@Override
		public void actionPerformed(ActionEvent e) {
			String s = getName(file.getName());
			if(s.endsWith(".class")){
				new FileExtractFile().actionPerformed(e);
				return;
			}
			if(s.toLowerCase().endsWith(".jar"))
				s = s.replaceAll("\\.[jJ][aA][rR]", ".zip");
			fc.setSelectedFile(new File("decompiled-"+s));
            int returnVal = fc.showSaveDialog(Model.frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
            	new Thread(new Runnable(){
					@Override
					public void run() {
		                label.setText("Extracting: "+file.getName());
		                bar.setVisible(true);
		                File fil = fc.getSelectedFile();
		                try {
		                    FileOutputStream dest = new FileOutputStream(fil);
		                    ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
		                    byte data[] = new byte[1024];
		                    if (state == null) {
		                        JarFile jfile = new JarFile(file);
		                        ITypeLoader jarLoader = new JarTypeLoader(jfile);
		                        typeLoader.getTypeLoaders().add(jarLoader);
		                        state = new State(file.getCanonicalPath(), file, jfile, jarLoader);
		                    }
		                    Enumeration<JarEntry> ent = state.jarFile.entries();
		                    while(ent.hasMoreElements()){
		                    	JarEntry entry = ent.nextElement();
		                        if (entry.getName().endsWith(".class")) {
		                        	JarEntry etn = new JarEntry(entry.getName().replace(".class", ".java"));
		                            label.setText("Extracting: "+etn.getName());
		                            out.putNextEntry(etn);
		                            String internalName = StringUtilities.removeRight(entry.getName(), ".class");
		                            TypeReference type = metadataSystem.lookupType(internalName);
		                            TypeDefinition resolvedType = null;
		                            if ((type == null) || ((resolvedType = type.resolve()) == null)) {
		                                JOptionPane.showMessageDialog(null, "Unable to resolve type.", "Error!", JOptionPane.ERROR_MESSAGE);
		                                return;
		                            }
		                            settings.getLanguage().decompileType(resolvedType, new PlainTextOutput(new OutputStreamWriter(out)), decompilationOptions);
		                            out.closeEntry();
		                        } else {
		                        	JarEntry etn = new JarEntry(entry.getName());
		                            label.setText("Extracting: "+etn.getName());
		                            out.putNextEntry(etn);
		                            InputStream in = state.jarFile.getInputStream(entry);
		                            if (in != null) {
		                                int count;
		                                while((count = in.read(data, 0, 1024)) != -1) {
		                                	out.write(data, 0, count);
		                                }
		                                in.close();
		                            }
		                            out.closeEntry();
		                        }
		                    }
		                    out.close();
		                } catch (Exception e1) {
		                    JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
		                    e1.printStackTrace();
		                }
		                label.setText("Complete");
		                bar.setVisible(false);						
					}
            		
            	}).start();
            }
		}
    	
    }
    
    public class Quit implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            Model.frame.dispose();
            System.exit(0);
        }
    }

    public class FileChooserFileFilter extends FileFilter {
        String objType;

        public FileChooserFileFilter(String string) {
            objType = string;
        }

        @Override
        public boolean accept(File f) {
            if (f.isDirectory())
                return false;
            return f.getName().toLowerCase().endsWith(objType.substring(1));
        }

        @Override
        public String getDescription() {
            return objType;
        }

    }

    public class DropListener implements DropTargetListener {

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
                            List<File> files = (List<File>) transferable
                                    .getTransferData(flavor);
                            if (files.size() > 1) {
                                event.rejectDrop();
                                return;
                            }
                            if (files.size() == 1) {
                                if (open)
                                    new FileClose().actionPerformed(null);
                                file = files.get(0);
                                new FileLoad(false).loadFile(file);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
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
                                    if (new String("" + (char) 0).equals(line)) continue;
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
                                if (open)
                                    new FileClose().actionPerformed(null);
                                file = list.get(0);
                                new FileLoad(false).loadFile(file);
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

    private class ThemeAction extends AbstractAction {

        private static final long serialVersionUID = -6618680171943723199L;
        private String xml;

        public ThemeAction(String name, String xml) {
            putValue(NAME, name);
            this.xml = "/themes/" + xml;
        }

        public void actionPerformed(ActionEvent e) {
            InputStream in = getClass().getResourceAsStream(xml);
            try {
                if (in != null) {
                    theme = Theme.load(in);
                    for (OpenFile f : hmap) {
                        theme.apply(f.textArea);
                    }
                }
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
}
