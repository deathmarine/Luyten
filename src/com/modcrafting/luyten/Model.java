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
import java.awt.event.WindowAdapter;
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
import java.io.Writer;
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
import java.util.zip.ZipException;
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

	private static final long MAX_JAR_FILE_SIZE_BYTES = 1_000_000_000;
	private static final long MAX_UNPACKED_FILE_SIZE_BYTES = 1_000_000;
    
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
    JCheckBox excludeNestedTypes;
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
    private FindBox findBox;

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
            	try {
	                int pos = house.getSelectedIndex();
	    			if (pos < 0) {
	    				label.setText("No open tab");
	    				return;
	    			}
	                RTextScrollPane co = (RTextScrollPane) house.getComponentAt(pos);
	                RSyntaxTextArea pane = (RSyntaxTextArea) co.getViewport().getView();
	                pane.requestFocusInWindow();
	                pane.setSelectionStart(0);
	                pane.setSelectionEnd(pane.getText().length());
            	} catch (Exception e) {
            		e.printStackTrace();
            	}
            }
        });
        fileMenu.add(menuItem);
        fileMenu.addSeparator();

        menuItem = new JMenuItem("Find...");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					int pos = house.getSelectedIndex();
					if (pos >= 0) {
						if (findBox == null)
							findBox = new FindBox(Model.this);
						findBox.showFindBox();
					} else {
						label.setText("No open tab");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
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
        excludeNestedTypes = new JCheckBox("Exclude Nested Types");
        fileMenu.add(excludeNestedTypes);
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

                final StringBuilder sb = new StringBuilder();
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
				new Thread() {
					public void run() {
						try {
							bar.setVisible(true);
			                OpenFile open = new OpenFile("Legal", "*/Legal", sb.toString(), theme);
			                hmap.add(open);
			                addOrSwitchToTab(open);
						} finally {
							bar.setVisible(false);
						}
					}
				}.start();
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
        this.setHideFindBoxOnMainWindowFocus();
        this.setVisible(true);
        bar.setVisible(false);


        decompilationOptions = new DecompilationOptions();
        decompilationOptions.setSettings(settings);
        decompilationOptions.setFullDecompilation(true);
    }

	private void populateSettingsFromSettingsMenu() {
        settings.setFlattenSwitchBlocks(flattenSwitchBlocks.isSelected());
        settings.setForceExplicitImports(forceExplicitImports.isSelected());
        settings.setShowSyntheticMembers(showSyntheticMembers.isSelected());
        settings.setExcludeNestedTypes(excludeNestedTypes.isSelected());
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
	}
	
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            	new Model();
            }
        });
    }

	public void addOrSwitchToTab(OpenFile open) {
		String title = open.name;
		RTextScrollPane rTextScrollPane = open.scrollPane;
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
            final TreePath trp = tree.getPathForLocation(event.getX(), event.getY());
            if (trp == null)
                return;
            if (SwingUtilities.isLeftMouseButton(event) 
            		&& event.getClickCount() == 2) {
				new Thread() {
					public void run() {
						openEntryByTreePath(trp);
					}
				}.start();
            } else {
                tree.getSelectionModel().setSelectionPath(trp);
            }
        }
    }

	private void openEntryByTreePath(TreePath trp) {
        String st = trp.toString().replace(file.getName(), "");
        final String[] args = st.replace("[", "").replace("]", "").split(",");
        String name = "";
        String path = "";
        try {
        	bar.setVisible(true);
            if (args.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i == args.length - 1) {
                        name = args[i].trim();
                    } else {
                        sb.append(args[i].trim()).append("/");
                    }
                }
                path = sb.toString().replace(".", "/") + name;
                populateSettingsFromSettingsMenu();
                
                if (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")) {
                    if (state == null) {
                        JarFile jfile = new JarFile(file);
                        ITypeLoader jarLoader = new JarTypeLoader(jfile);

                        typeLoader.getTypeLoaders().add(jarLoader);
                        state = new State(file.getCanonicalPath(), file, jfile, jarLoader);
                    }

                    JarEntry entry = state.jarFile.getJarEntry(path);
					if (entry == null) {
						throw new FileEntryNotFoundException();
					}
                    if (entry.getSize() > MAX_UNPACKED_FILE_SIZE_BYTES) {
						throw new TooLargeFileException(entry.getSize());
					}

                    if (entry.getName().endsWith(".class")) {
                    	label.setText("Extracting: " + name);
                        String internalName = StringUtilities.removeRight(entry.getName(), ".class");
                        TypeReference type = metadataSystem.lookupType(internalName);
                        extractClassToTextPane(type, name, path);
                    } else {
                    	label.setText("Opening: " + name);
						try (InputStream in = state.jarFile.getInputStream(entry);) {
							extractSimpleFileEntryToTextPane(in, name, path);
						}
                    }
                }
            } else {
                name = file.getName();
                path = file.getPath().replaceAll("\\\\", "/");
				if (file.length() > MAX_UNPACKED_FILE_SIZE_BYTES) {
					throw new TooLargeFileException(file.length());
				}
                if (name.endsWith(".class")) {
                	label.setText("Extracting: " + name);
                    TypeReference type = metadataSystem.lookupType(path);
                    extractClassToTextPane(type, name, path);
                } else {
                	label.setText("Opening: " + name);
					try (InputStream in = new FileInputStream(file);) {
						extractSimpleFileEntryToTextPane(in, name, path);
					}
                }
            }
			label.setText("Complete");
		} catch (FileEntryNotFoundException e) {
			label.setText("File not found: " + name);
		} catch (FileIsBinaryException e) {
			label.setText("Binary resource: " + name);
		} catch (TooLargeFileException e) {
			label.setText("File is too large: " + name + " - size: " + e.getReadableFileSize());
		} catch (Exception e) {
			label.setText("Cannot open: " + name);
			e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
		} finally {
			bar.setVisible(false);
		}
	}

	private void extractClassToTextPane(TypeReference type, String tabTitle, String path) throws Exception {
		if (tabTitle == null || tabTitle.trim().length() < 1 || path == null) {
			throw new FileEntryNotFoundException();
		}
		OpenFile sameTitledOpen = null;
		for (OpenFile nextOpen : hmap) {
			if (tabTitle.equals(nextOpen.name)) {
				sameTitledOpen = nextOpen;
				break;
			}
		}
		if (sameTitledOpen != null && path.equals(sameTitledOpen.getPath())) {
			addOrSwitchToTab(sameTitledOpen);
			return;
		}
		
		// build tab content: do decompilation
		TypeDefinition resolvedType = null;
		if (type == null || ((resolvedType = type.resolve()) == null)) {
			throw new Exception("Unable to resolve type.");
		}
		StringWriter stringwriter = new StringWriter();
		settings.getLanguage().decompileType(resolvedType,
				new PlainTextOutput(stringwriter), decompilationOptions);
		String decompiledSource = stringwriter.toString();

		// open tab
		if (sameTitledOpen != null) {
			sameTitledOpen.setContent(decompiledSource);
			sameTitledOpen.setPath(path);
			addOrSwitchToTab(sameTitledOpen);
		} else {
			OpenFile open = new OpenFile(tabTitle, path, decompiledSource, theme);
			hmap.add(open);
			addOrSwitchToTab(open);
		}
	}

	private void extractSimpleFileEntryToTextPane(InputStream inputStream, String tabTitle, String path) throws Exception {
		if (inputStream == null || tabTitle == null || tabTitle.trim().length() < 1 || path == null) {
			throw new FileEntryNotFoundException();
		}
		OpenFile sameTitledOpen = null;
		for (OpenFile nextOpen : hmap) {
			if (tabTitle.equals(nextOpen.name)) {
				sameTitledOpen = nextOpen;
				break;
			}
		}
		if (sameTitledOpen != null && path.equals(sameTitledOpen.getPath())) {
			addOrSwitchToTab(sameTitledOpen);
			return;
		}

		// build tab content
		StringBuilder sb = new StringBuilder();
		long nonprintableCharactersCount = 0;
		try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
				BufferedReader reader = new BufferedReader(inputStreamReader);) {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");

				for (byte nextByte : line.getBytes()) {
					if (nextByte <= 0) {
						nonprintableCharactersCount++;
					}
				}

			}
		}
		
		// guess binary or text
		String extension = "." + tabTitle.replaceAll("^[^\\.]*$", "").replaceAll("[^\\.]*\\.", "");
		boolean isTextFile = (OpenFile.WELL_KNOWN_TEXT_FILE_EXTENSIONS.contains(extension) ||
				nonprintableCharactersCount < sb.length() / 5);
		if (!isTextFile) {
			throw new FileIsBinaryException();
		}
		
		// open tab
		if (sameTitledOpen != null) {
			sameTitledOpen.setContent(sb.toString());
			sameTitledOpen.setPath(path);
			addOrSwitchToTab(sameTitledOpen);
		} else {
			OpenFile open = new OpenFile(tabTitle, path, sb.toString(), theme);
			hmap.add(open);
			addOrSwitchToTab(open);
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
					try {
						if (file == null) {
							return;
						}
						if (file.length() > MAX_JAR_FILE_SIZE_BYTES) {
							throw new TooLargeFileException(file.length());
						}
				        if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
				            JarFile jfile;
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
		            		label.setText("Complete");
				        } else {
				            DefaultMutableTreeNode top = new DefaultMutableTreeNode(getName(file.getName()));
				            tree.setModel(new DefaultTreeModel(top));
				            settings.setTypeLoader(new InputTypeLoader());
				            open = true;
		            		label.setText("Complete");
				        }
					} catch (TooLargeFileException e) {
						label.setText("File is too large: " + file.getName() + " - size: " + e.getReadableFileSize());
					} catch (Exception e1) {
						label.setText("Cannot open: " + file.getName());
						e1.printStackTrace();
					} finally {
						bar.setVisible(false);
					}
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
			if (!open || file == null) {
				label.setText("No open file");
				return;
			}
			int index = house.getSelectedIndex();
			if (index < 0) {
				label.setText("No open tab");
				return;
			}
			RTextScrollPane co = (RTextScrollPane) house.getComponentAt(index);
			final RSyntaxTextArea pane = (RSyntaxTextArea) co.getViewport().getView();
			String title = house.getTitleAt(index);
			fc.setSelectedFile(new File(title.replace(".class", ".java")));
			int returnVal = fc.showSaveDialog(Model.frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						File fil = fc.getSelectedFile();
						try (FileWriter fw = new FileWriter(fil);
								BufferedWriter bw = new BufferedWriter(fw);) {
							label.setText("Extracting: " + fil.getName());
							bar.setVisible(true);
							bw.write(pane.getText());
							bw.flush();
							label.setText("Complete");
						} catch (Exception e1) {
							label.setText("Cannot save file: " + fil.getName());
							e1.printStackTrace();
							JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
						} finally {
							bar.setVisible(false);
						}
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
			if (!open || file == null) {
				label.setText("No open file");
				return;
			}
			populateSettingsFromSettingsMenu();
			
			String s = getName(file.getName());
			if (s.endsWith(".class")) {
				new FileExtractFile().actionPerformed(e);
				return;
			}
			if (s.toLowerCase().endsWith(".jar"))
				s = s.replaceAll("\\.[jJ][aA][rR]", ".zip");
			fc.setSelectedFile(new File("decompiled-" + s));
			int returnVal = fc.showSaveDialog(Model.frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						File fil = fc.getSelectedFile();
						try (FileOutputStream dest = new FileOutputStream(fil);
								BufferedOutputStream buffDest = new BufferedOutputStream(dest);
								ZipOutputStream out = new ZipOutputStream(buffDest);) {
							byte data[] = new byte[1024];
							if (state == null) {
								JarFile jfile = new JarFile(file);
								ITypeLoader jarLoader = new JarTypeLoader(jfile);
								typeLoader.getTypeLoaders().add(jarLoader);
								state = new State(file.getCanonicalPath(), file, jfile, jarLoader);
							}

							Enumeration<JarEntry> ent = state.jarFile.entries();
							while (ent.hasMoreElements()) {
								label.setText("Extracting: " + file.getName());
								bar.setVisible(true);
								JarEntry entry = ent.nextElement();
								if (entry.getName().endsWith(".class")) {
									JarEntry etn = new JarEntry(entry.getName().replace(".class", ".java"));
									label.setText("Extracting: " + etn.getName());
									out.putNextEntry(etn);
									try {
										String internalName = StringUtilities.removeRight(entry.getName(), ".class");
										TypeReference type = metadataSystem.lookupType(internalName);
										TypeDefinition resolvedType = null;
										if ((type == null) || ((resolvedType = type.resolve()) == null)) {
											new Exception("Unable to resolve type.").printStackTrace();
											JOptionPane.showMessageDialog(null, "Unable to resolve type.", "Error!",
													JOptionPane.ERROR_MESSAGE);
											return;
										}
										Writer writer = new OutputStreamWriter(out);
										settings.getLanguage().decompileType(resolvedType,
												new PlainTextOutput(writer), decompilationOptions);
										writer.flush();
									} finally {
										out.closeEntry();
									}
								} else {
									try {
										JarEntry etn = new JarEntry(entry.getName());
										label.setText("Extracting: " + etn.getName());
										out.putNextEntry(etn);
										try {
											InputStream in = state.jarFile.getInputStream(entry);
											if (in != null) {
												try {
													int count;
													while ((count = in.read(data, 0, 1024)) != -1) {
														out.write(data, 0, count);
													}
												} finally {
													in.close();
												}
											}
										} finally {
											out.closeEntry();
										}
									} catch (ZipException ze) {
										// some jar-s contain duplicate pom.xml entries: ignore it
										if (!ze.getMessage().contains("duplicate")) {
											throw ze;
										}
									}
								}
							}
							label.setText("Complete");
						} catch (Exception e1) {
							label.setText("Cannot save file: " + fil.getName());
							e1.printStackTrace();
							JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
						} finally {
							bar.setVisible(false);
						}
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
            	e1.printStackTrace();
                JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
            }
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
}
