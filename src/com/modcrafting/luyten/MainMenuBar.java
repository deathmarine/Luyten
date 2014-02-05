package com.modcrafting.luyten;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;

/**
 * Main menu (only MainWindow should be called from here)
 */
public class MainMenuBar extends JMenuBar {
	private static final long serialVersionUID = 1L;

	private final MainWindow mainWindow;
	private final Map<String, Language> languageLookup = new HashMap<String, Language>();

	private JCheckBox flattenSwitchBlocks;
	private JCheckBox forceExplicitImports;
	private JCheckBox forceExplicitTypes;
	private JCheckBox showSyntheticMembers;
	private JCheckBox excludeNestedTypes;
	private JCheckBox retainRedundantCasts;
	private JCheckBox showDebugInfo;
	private JRadioButtonMenuItem java;
	private JRadioButtonMenuItem bytecode;
	private JRadioButtonMenuItem bytecodeAST;
	private ButtonGroup languagesGroup;
	private DecompilerSettings settings;
	private LuytenPreferences luytenPrefs;

	public MainMenuBar(MainWindow mainWnd) {
		this.mainWindow = mainWnd;
		ConfigSaver configSaver = ConfigSaver.getLoadedInstance();
		settings = configSaver.getDecompilerSettings();
		luytenPrefs = configSaver.getLuytenPreferences();

		JMenu fileMenu = new JMenu("File");
		JMenuItem menuItem = new JMenuItem("Open File...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onOpenFileMenu();
			}
		});
		fileMenu.add(menuItem);
		fileMenu.addSeparator();

		menuItem = new JMenuItem("Close");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onCloseFileMenu();
			}
		});
		fileMenu.add(menuItem);
		fileMenu.addSeparator();

		menuItem = new JMenuItem("Save As...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onSaveAsMenu();
			}
		});
		fileMenu.add(menuItem);

		menuItem = new JMenuItem("Save All...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
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

		menuItem = new JMenuItem("Exit");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onExitMenu();
			}
		});
		fileMenu.add(menuItem);
		this.add(fileMenu);

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
			public void actionPerformed(ActionEvent e) {
				mainWindow.onSelectAllMenu();
			}
		});
		fileMenu.add(menuItem);
		fileMenu.addSeparator();

		menuItem = new JMenuItem("Find...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onFindMenu();
			}
		});
		fileMenu.add(menuItem);
		this.add(fileMenu);

		fileMenu = new JMenu("Themes");
		languagesGroup = new ButtonGroup();
		JRadioButtonMenuItem a = new JRadioButtonMenuItem(new ThemeAction("Default", "default.xml"));
		a.setSelected("default.xml".equals(luytenPrefs.getThemeXml()));
		languagesGroup.add(a);
		fileMenu.add(a);
		a = new JRadioButtonMenuItem(new ThemeAction("Dark", "dark.xml"));
		a.setSelected("dark.xml".equals(luytenPrefs.getThemeXml()));
		languagesGroup.add(a);
		fileMenu.add(a);
		a = new JRadioButtonMenuItem(new ThemeAction("Eclipse", "eclipse.xml"));
		a.setSelected("eclipse.xml".equals(luytenPrefs.getThemeXml()));
		languagesGroup.add(a);
		fileMenu.add(a);
		a = new JRadioButtonMenuItem(new ThemeAction("Visual Studio", "vs.xml"));
		a.setSelected("vs.xml".equals(luytenPrefs.getThemeXml()));
		languagesGroup.add(a);
		fileMenu.add(a);
		this.add(fileMenu);

		fileMenu = new JMenu("Settings");
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
		flattenSwitchBlocks = new JCheckBox("Flatten Switch Blocks");
		flattenSwitchBlocks.setSelected(settings.getFlattenSwitchBlocks());
		flattenSwitchBlocks.addActionListener(settingsChanged);
		fileMenu.add(flattenSwitchBlocks);
		forceExplicitImports = new JCheckBox("Force Explicit Imports");
		forceExplicitImports.setSelected(settings.getForceExplicitImports());
		forceExplicitImports.addActionListener(settingsChanged);
		fileMenu.add(forceExplicitImports);
		forceExplicitTypes = new JCheckBox("Force Explicit Types");
		forceExplicitTypes.setSelected(settings.getForceExplicitTypeArguments());
		forceExplicitTypes.addActionListener(settingsChanged);
		fileMenu.add(forceExplicitTypes);
		showSyntheticMembers = new JCheckBox("Show Synthetic Members");
		showSyntheticMembers.setSelected(settings.getShowSyntheticMembers());
		showSyntheticMembers.addActionListener(settingsChanged);
		fileMenu.add(showSyntheticMembers);
		excludeNestedTypes = new JCheckBox("Exclude Nested Types");
		excludeNestedTypes.setSelected(settings.getExcludeNestedTypes());
		excludeNestedTypes.addActionListener(settingsChanged);
		fileMenu.add(excludeNestedTypes);
		retainRedundantCasts = new JCheckBox("Retain Redundant Casts");
		retainRedundantCasts.setSelected(settings.getRetainRedundantCasts());
		retainRedundantCasts.addActionListener(settingsChanged);
		fileMenu.add(retainRedundantCasts);
		JMenu debugSettingsMenu = new JMenu("Debug Settings");
		showDebugInfo = new JCheckBox("Include Error Diagnostics");
		showDebugInfo.setSelected(settings.getIncludeErrorDiagnostics());
		showDebugInfo.addActionListener(settingsChanged);

		debugSettingsMenu.add(showDebugInfo);
		fileMenu.add(debugSettingsMenu);
		fileMenu.addSeparator();

		languageLookup.put(Languages.java().getName(), Languages.java());
		languageLookup.put(Languages.bytecode().getName(), Languages.bytecode());
		languageLookup.put(Languages.bytecodeAst().getName(), Languages.bytecodeAst());

		languagesGroup = new ButtonGroup();
		java = new JRadioButtonMenuItem(Languages.java().getName());
		java.getModel().setActionCommand(Languages.java().getName());
		java.setSelected(Languages.java().getName().equals(settings.getLanguage().getName()));
		languagesGroup.add(java);
		fileMenu.add(java);
		bytecode = new JRadioButtonMenuItem(Languages.bytecode().getName());
		bytecode.getModel().setActionCommand(Languages.bytecode().getName());
		bytecode.setSelected(Languages.bytecode().getName().equals(settings.getLanguage().getName()));
		languagesGroup.add(bytecode);
		fileMenu.add(bytecode);
		bytecodeAST = new JRadioButtonMenuItem(Languages.bytecodeAst().getName());
		bytecodeAST.getModel().setActionCommand(Languages.bytecodeAst().getName());
		bytecodeAST.setSelected(Languages.bytecodeAst().getName().equals(settings.getLanguage().getName()));
		languagesGroup.add(bytecodeAST);
		fileMenu.add(bytecodeAST);

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
		fileMenu.add(debugLanguagesMenu);
		this.add(fileMenu);
		fileMenu = new JMenu("Help");
		menuItem = new JMenuItem("Legal");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainWindow.onLegalMenu();
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
		this.add(fileMenu);
	}

	private void populateSettingsFromSettingsMenu() {
		// synchronized: do not disturb decompiler at work (synchronize every time before run decompiler)
		synchronized (settings) {
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
}
