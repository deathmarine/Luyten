package us.deathmarine.luyten;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;

import com.strobel.Procyon;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;

/**
 * Main menu (only MainWindow should be called from here)
 */
public class MainMenuBar extends JMenuBar {
	private static final long serialVersionUID = -7949855817172562075L;
	private final MainWindow mainWindow;
	private final Map<String, Language> languageLookup = new HashMap<String, Language>();

	private JCheckBox flattenSwitchBlocks;
	private JCheckBox forceExplicitImports;
	private JCheckBox forceExplicitTypes;
	private JCheckBox showSyntheticMembers;
	private JCheckBox excludeNestedTypes;
	private JCheckBox retainRedundantCasts;
	private JCheckBox unicodeReplacement;
	private JCheckBox debugLineNumbers;
	private JCheckBox showDebugInfo;
	private JCheckBox bytecodeLineNumbers;
	private JRadioButtonMenuItem java;
	private JRadioButtonMenuItem bytecode;
	private JRadioButtonMenuItem bytecodeAST;
	private ButtonGroup languagesGroup;
	private ButtonGroup themesGroup;
	private JCheckBox packageExplorerStyle;
	private JCheckBox filterOutInnerClassEntries;
	private JCheckBox singleClickOpenEnabled;
	private JCheckBox exitByEscEnabled;
	private DecompilerSettings settings;
	private LuytenPreferences luytenPrefs;

	public MainMenuBar(MainWindow mainWnd) {
		this.mainWindow = mainWnd;
		final ConfigSaver configSaver = ConfigSaver.getLoadedInstance();
		settings = configSaver.getDecompilerSettings();
		luytenPrefs = configSaver.getLuytenPreferences();

		final JMenu fileMenu = new JMenu("File");
		fileMenu.add(new JMenuItem("..."));
		this.add(fileMenu);
		final JMenu editMenu = new JMenu("Edit");
		editMenu.add(new JMenuItem("..."));
		this.add(editMenu);
		final JMenu themesMenu = new JMenu("Themes");
		themesMenu.add(new JMenuItem("..."));
		this.add(themesMenu);
		final JMenu operationMenu = new JMenu("Operation");
		operationMenu.add(new JMenuItem("..."));
		this.add(operationMenu);
		final JMenu settingsMenu = new JMenu("Settings");
		settingsMenu.add(new JMenuItem("..."));
		this.add(settingsMenu);
		final JMenu helpMenu = new JMenu("Help");
		helpMenu.add(new JMenuItem("..."));
		this.add(helpMenu);

		// start quicker
		new Thread() {
			public void run() {
				try {
					// build menu later
					buildFileMenu(fileMenu);
					refreshMenuPopup(fileMenu);

					buildEditMenu(editMenu);
					refreshMenuPopup(editMenu);

					buildThemesMenu(themesMenu);
					refreshMenuPopup(themesMenu);

					buildOperationMenu(operationMenu);
					refreshMenuPopup(operationMenu);

					buildSettingsMenu(settingsMenu, configSaver);
					refreshMenuPopup(settingsMenu);

					buildHelpMenu(helpMenu);
					refreshMenuPopup(helpMenu);
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			}

			// refresh currently opened menu
			// (if user selected a menu before it was ready)
			private void refreshMenuPopup(JMenu menu) {
				try {
					if (menu.isPopupMenuVisible()) {
						menu.getPopupMenu().setVisible(false);
						menu.getPopupMenu().setVisible(true);
					}
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			}
		}.start();
	}

	private void buildFileMenu(final JMenu fileMenu) {
		fileMenu.removeAll();
		JMenuItem menuItem = new JMenuItem("Open File...");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onOpenFileMenu();
			}
		});
		fileMenu.add(menuItem);
		fileMenu.addSeparator();

		menuItem = new JMenuItem("Close");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onCloseFileMenu();
			}
		});
		fileMenu.add(menuItem);
		fileMenu.addSeparator();

		menuItem = new JMenuItem("Save As...");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onSaveAsMenu();
			}
		});
		fileMenu.add(menuItem);

		menuItem = new JMenuItem("Save All...");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onSaveAllMenu();
			}
		});
		fileMenu.add(menuItem);
		fileMenu.addSeparator();

		menuItem = new JMenuItem("Recent Files");
		menuItem.setEnabled(false);
		fileMenu.add(menuItem);
		fileMenu.addSeparator();

		// Only add the exit command for non-OS X. OS X handles its close
		// automatically
		if (!("true".equals(System.getProperty("us.deathmarine.luyten.Luyten.running_in_osx")))) {
			menuItem = new JMenuItem("Exit");
			menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
			menuItem.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					mainWindow.onExitMenu();
				}
			});
			fileMenu.add(menuItem);
		}
	}

	private void buildEditMenu(JMenu editMenu) {
		editMenu.removeAll();
		JMenuItem menuItem = new JMenuItem("Cut");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.setEnabled(false);
		editMenu.add(menuItem);

		menuItem = new JMenuItem("Copy");
		menuItem.addActionListener(new DefaultEditorKit.CopyAction());
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		editMenu.add(menuItem);

		menuItem = new JMenuItem("Paste");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.setEnabled(false);
		editMenu.add(menuItem);

		editMenu.addSeparator();

		menuItem = new JMenuItem("Select All");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onSelectAllMenu();
			}
		});
		editMenu.add(menuItem);
		editMenu.addSeparator();

		menuItem = new JMenuItem("Find...");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onFindMenu();
			}
		});
		editMenu.add(menuItem);

		menuItem = new JMenuItem("Find All");
		menuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onFindAllMenu();

			}
		});
		editMenu.add(menuItem);
	}

	private void buildThemesMenu(JMenu themesMenu) {
		themesMenu.removeAll();
		themesGroup = new ButtonGroup();
		JRadioButtonMenuItem a = new JRadioButtonMenuItem(new ThemeAction("Default", "default.xml"));
		a.setSelected("default.xml".equals(luytenPrefs.getThemeXml()));
		themesGroup.add(a);
		themesMenu.add(a);

		a = new JRadioButtonMenuItem(new ThemeAction("Default-Alt", "default-alt.xml"));
		a.setSelected("default-alt.xml".equals(luytenPrefs.getThemeXml()));
		themesGroup.add(a);
		themesMenu.add(a);

		a = new JRadioButtonMenuItem(new ThemeAction("Dark", "dark.xml"));
		a.setSelected("dark.xml".equals(luytenPrefs.getThemeXml()));
		themesGroup.add(a);
		themesMenu.add(a);

		a = new JRadioButtonMenuItem(new ThemeAction("Eclipse", "eclipse.xml"));
		a.setSelected("eclipse.xml".equals(luytenPrefs.getThemeXml()));
		themesGroup.add(a);
		themesMenu.add(a);

		a = new JRadioButtonMenuItem(new ThemeAction("Visual Studio", "vs.xml"));
		a.setSelected("vs.xml".equals(luytenPrefs.getThemeXml()));
		themesGroup.add(a);
		themesMenu.add(a);

		a = new JRadioButtonMenuItem(new ThemeAction("IntelliJ", "idea.xml"));
		a.setSelected("idea.xml".equals(luytenPrefs.getThemeXml()));
		themesGroup.add(a);
		themesMenu.add(a);
	}

	private void buildOperationMenu(JMenu operationMenu) {
		operationMenu.removeAll();
		packageExplorerStyle = new JCheckBox("    Package Explorer Style");
		packageExplorerStyle.setSelected(luytenPrefs.isPackageExplorerStyle());
		packageExplorerStyle.setContentAreaFilled(false);
		packageExplorerStyle.setFocusable(false);
		packageExplorerStyle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				luytenPrefs.setPackageExplorerStyle(packageExplorerStyle.isSelected());
				mainWindow.onTreeSettingsChanged();
			}
		});
		operationMenu.add(packageExplorerStyle);

		filterOutInnerClassEntries = new JCheckBox("    Filter Out Inner Class Entries");
		filterOutInnerClassEntries.setSelected(luytenPrefs.isFilterOutInnerClassEntries());
		filterOutInnerClassEntries.setContentAreaFilled(false);
		filterOutInnerClassEntries.setFocusable(false);
		filterOutInnerClassEntries.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				luytenPrefs.setFilterOutInnerClassEntries(filterOutInnerClassEntries.isSelected());
				mainWindow.onTreeSettingsChanged();
			}
		});
		operationMenu.add(filterOutInnerClassEntries);

		singleClickOpenEnabled = new JCheckBox("    Single Click Open");
		singleClickOpenEnabled.setSelected(luytenPrefs.isSingleClickOpenEnabled());
		singleClickOpenEnabled.setContentAreaFilled(false);
		singleClickOpenEnabled.setFocusable(false);
		singleClickOpenEnabled.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				luytenPrefs.setSingleClickOpenEnabled(singleClickOpenEnabled.isSelected());
			}
		});
		operationMenu.add(singleClickOpenEnabled);

		exitByEscEnabled = new JCheckBox("    Exit By Esc");
		exitByEscEnabled.setSelected(luytenPrefs.isExitByEscEnabled());
		exitByEscEnabled.setContentAreaFilled(false);
		exitByEscEnabled.setFocusable(false);
		exitByEscEnabled.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				luytenPrefs.setExitByEscEnabled(exitByEscEnabled.isSelected());
			}
		});
		operationMenu.add(exitByEscEnabled);
	}

	private void buildSettingsMenu(JMenu settingsMenu, ConfigSaver configSaver) {
		settingsMenu.removeAll();
		ActionListener settingsChanged = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread() {
					@Override
					public void run() {
						populateSettingsFromSettingsMenu();
						mainWindow.onSettingsChanged();
					}
				}.start();
			}
		};
		flattenSwitchBlocks = new JCheckBox("    Flatten Switch Blocks");
		flattenSwitchBlocks.setSelected(settings.getFlattenSwitchBlocks());
		flattenSwitchBlocks.setContentAreaFilled(false);
		flattenSwitchBlocks.setFocusable(false);
		flattenSwitchBlocks.addActionListener(settingsChanged);
		settingsMenu.add(flattenSwitchBlocks);

		forceExplicitImports = new JCheckBox("    Force Explicit Imports");
		forceExplicitImports.setSelected(settings.getForceExplicitImports());
		forceExplicitImports.setContentAreaFilled(false);
		forceExplicitImports.setFocusable(false);
		forceExplicitImports.addActionListener(settingsChanged);
		settingsMenu.add(forceExplicitImports);

		forceExplicitTypes = new JCheckBox("    Force Explicit Types");
		forceExplicitTypes.setSelected(settings.getForceExplicitTypeArguments());
		forceExplicitTypes.setContentAreaFilled(false);
		forceExplicitTypes.setFocusable(false);
		forceExplicitTypes.addActionListener(settingsChanged);
		settingsMenu.add(forceExplicitTypes);

		showSyntheticMembers = new JCheckBox("    Show Synthetic Members");
		showSyntheticMembers.setSelected(settings.getShowSyntheticMembers());
		showSyntheticMembers.setContentAreaFilled(false);
		showSyntheticMembers.setFocusable(false);
		showSyntheticMembers.addActionListener(settingsChanged);
		settingsMenu.add(showSyntheticMembers);

		excludeNestedTypes = new JCheckBox("    Exclude Nested Types");
		excludeNestedTypes.setSelected(settings.getExcludeNestedTypes());
		excludeNestedTypes.setContentAreaFilled(false);
		excludeNestedTypes.setFocusable(false);
		excludeNestedTypes.addActionListener(settingsChanged);
		settingsMenu.add(excludeNestedTypes);

		retainRedundantCasts = new JCheckBox("    Retain Redundant Casts");
		retainRedundantCasts.setSelected(settings.getRetainRedundantCasts());
		retainRedundantCasts.setContentAreaFilled(false);
		retainRedundantCasts.setFocusable(false);
		retainRedundantCasts.addActionListener(settingsChanged);
		settingsMenu.add(retainRedundantCasts);

		unicodeReplacement = new JCheckBox("    Enable Unicode Replacement");
		unicodeReplacement.setSelected(settings.isUnicodeOutputEnabled());
		unicodeReplacement.setContentAreaFilled(false);
		unicodeReplacement.setFocusable(false);
		unicodeReplacement.addActionListener(settingsChanged);
		settingsMenu.add(unicodeReplacement);

		debugLineNumbers = new JCheckBox("    Show Debug Line Numbers");
		debugLineNumbers.setSelected(settings.getShowDebugLineNumbers());
		debugLineNumbers.setContentAreaFilled(false);
		debugLineNumbers.setFocusable(false);
		debugLineNumbers.addActionListener(settingsChanged);
		settingsMenu.add(debugLineNumbers);

		JMenu debugSettingsMenu = new JMenu("Debug Settings");
		showDebugInfo = new JCheckBox("    Include Error Diagnostics");
		showDebugInfo.setSelected(settings.getIncludeErrorDiagnostics());
		showDebugInfo.setContentAreaFilled(false);
		showDebugInfo.setFocusable(false);
		showDebugInfo.addActionListener(settingsChanged);

		debugSettingsMenu.add(showDebugInfo);
		settingsMenu.add(debugSettingsMenu);
		settingsMenu.addSeparator();

		languageLookup.put(Languages.java().getName(), Languages.java());
		languageLookup.put(Languages.bytecode().getName(), Languages.bytecode());
		languageLookup.put(Languages.bytecodeAst().getName(), Languages.bytecodeAst());

		languagesGroup = new ButtonGroup();
		java = new JRadioButtonMenuItem(Languages.java().getName());
		java.getModel().setActionCommand(Languages.java().getName());
		java.setSelected(Languages.java().getName().equals(settings.getLanguage().getName()));
		languagesGroup.add(java);
		settingsMenu.add(java);
		bytecode = new JRadioButtonMenuItem(Languages.bytecode().getName());
		bytecode.getModel().setActionCommand(Languages.bytecode().getName());
		bytecode.setSelected(Languages.bytecode().getName().equals(settings.getLanguage().getName()));
		languagesGroup.add(bytecode);
		settingsMenu.add(bytecode);
		bytecodeAST = new JRadioButtonMenuItem(Languages.bytecodeAst().getName());
		bytecodeAST.getModel().setActionCommand(Languages.bytecodeAst().getName());
		bytecodeAST.setSelected(Languages.bytecodeAst().getName().equals(settings.getLanguage().getName()));
		languagesGroup.add(bytecodeAST);
		settingsMenu.add(bytecodeAST);

		JMenu debugLanguagesMenu = new JMenu("Debug Languages");
		for (final Language language : Languages.debug()) {
			final JRadioButtonMenuItem m = new JRadioButtonMenuItem(language.getName());
			m.getModel().setActionCommand(language.getName());
			m.setSelected(language.getName().equals(settings.getLanguage().getName()));
			languagesGroup.add(m);
			debugLanguagesMenu.add(m);
			languageLookup.put(language.getName(), language);
		}
		for (AbstractButton button : Collections.list(languagesGroup.getElements())) {
			button.addActionListener(settingsChanged);
		}
		settingsMenu.add(debugLanguagesMenu);

		bytecodeLineNumbers = new JCheckBox("    Show Line Numbers In Bytecode");
		bytecodeLineNumbers.setSelected(settings.getIncludeLineNumbersInBytecode());
		bytecodeLineNumbers.setContentAreaFilled(false);
		bytecodeLineNumbers.setFocusable(false);
		bytecodeLineNumbers.addActionListener(settingsChanged);
		settingsMenu.add(bytecodeLineNumbers);
	}

	private void buildHelpMenu(JMenu helpMenu) {
		helpMenu.removeAll();
		JMenuItem menuItem = new JMenuItem("Legal");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onLegalMenu();
			}
		});
		helpMenu.add(menuItem);
		JMenu menuDebug = new JMenu("Debug");
		menuItem = new JMenuItem("List JVM Classes");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onListLoadedClasses();
			}
		});
		menuDebug.add(menuItem);
		helpMenu.add(menuDebug);
		menuItem = new JMenuItem("About");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JPanel pane = new JPanel();
				pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
				JLabel title = new JLabel("Luyten " + Luyten.getVersion());
				title.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
				pane.add(title);
				pane.add(new JLabel("by Deathmarine"));
				String project = "https://github.com/deathmarine/Luyten/";
				JLabel link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + project + "</U></FONT></HTML>");
				link.setCursor(new Cursor(Cursor.HAND_CURSOR));
				link.addMouseListener(new LinkListener(project, link));
				pane.add(link);
				pane.add(new JLabel("Contributions By:"));
				pane.add(new JLabel("zerdei, toonetown, dstmath"));
				pane.add(new JLabel("virustotalop, xtrafrancyz"));
				pane.add(new JLabel("mbax, quitten, and mstrobel"));
				pane.add(new JLabel(" "));
				pane.add(new JLabel("Powered By:"));
				String procyon = "https://bitbucket.org/mstrobel/procyon";
				link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + procyon + "</U></FONT></HTML>");
				link.setCursor(new Cursor(Cursor.HAND_CURSOR));
				link.addMouseListener(new LinkListener(procyon, link));
				pane.add(link);
				pane.add(new JLabel("Version: " + Procyon.version()));
				pane.add(new JLabel("(c) 2016 Mike Strobel"));
				String rsyntax = "https://github.com/bobbylight/RSyntaxTextArea";
				link = new JLabel("<HTML><FONT color=\"#000099\"><U>" + rsyntax + "</U></FONT></HTML>");
				link.setCursor(new Cursor(Cursor.HAND_CURSOR));
				link.addMouseListener(new LinkListener(rsyntax, link));
				pane.add(link);
				pane.add(new JLabel("Version: 2.6.0"));
				pane.add(new JLabel("(c) 2016 Robert Futrell"));
				pane.add(new JLabel(" "));
				JOptionPane.showMessageDialog(null, pane);
			}
		});
		helpMenu.add(menuItem);
	}

	private void populateSettingsFromSettingsMenu() {
		// synchronized: do not disturb decompiler at work (synchronize every
		// time before run decompiler)
		synchronized (settings) {
			settings.setFlattenSwitchBlocks(flattenSwitchBlocks.isSelected());
			settings.setForceExplicitImports(forceExplicitImports.isSelected());
			settings.setShowSyntheticMembers(showSyntheticMembers.isSelected());
			settings.setExcludeNestedTypes(excludeNestedTypes.isSelected());
			settings.setForceExplicitTypeArguments(forceExplicitTypes.isSelected());
			settings.setRetainRedundantCasts(retainRedundantCasts.isSelected());
			settings.setIncludeErrorDiagnostics(showDebugInfo.isSelected());
			settings.setUnicodeOutputEnabled(unicodeReplacement.isSelected());
			settings.setShowDebugLineNumbers(debugLineNumbers.isSelected());
			//
			// Note: You shouldn't ever need to set this. It's only for
			// languages that support catch
			// blocks without an exception variable. Java doesn't allow this. I
			// think Scala does.
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
			settings.setIncludeLineNumbersInBytecode(bytecodeLineNumbers.isSelected());
		}
	}

	private class ThemeAction extends AbstractAction {
		private static final long serialVersionUID = -6618680171943723199L;
		private String xml;

		public ThemeAction(String name, String xml) {
			putValue(NAME, name);
			this.xml = xml;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			luytenPrefs.setThemeXml(xml);
			mainWindow.onThemesChanged();
		}
	}

	private class LinkListener extends MouseAdapter {
		String link;
		JLabel label;

		public LinkListener(String link, JLabel label) {
			this.link = link;
			this.label = label;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			try {
				Desktop.getDesktop().browse(new URI(link));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			label.setText("<HTML><FONT color=\"#00aa99\"><U>" + link + "</U></FONT></HTML>");
		}

		@Override
		public void mouseExited(MouseEvent e) {
			label.setText("<HTML><FONT color=\"#000099\"><U>" + link + "</U></FONT></HTML>");
		}

	}
}
