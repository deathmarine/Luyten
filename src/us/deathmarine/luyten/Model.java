package us.deathmarine.luyten;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.JarTypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

/**
 * Jar-level model
 */
public class Model extends JSplitPane {
	private static final long serialVersionUID = 6896857630400910200L;

	private static final long MAX_JAR_FILE_SIZE_BYTES = 1_000_000_000;
	private static final long MAX_UNPACKED_FILE_SIZE_BYTES = 1_000_000;

    private static LuytenTypeLoader typeLoader = new LuytenTypeLoader();
    public static MetadataSystem metadataSystem = new MetadataSystem(typeLoader);

	private JTree tree;
	private JTabbedPane house;
	private File file;
	private DecompilerSettings settings;
	private DecompilationOptions decompilationOptions;
	private Theme theme;
	private MainWindow mainWindow;
	private JProgressBar bar;
	private JLabel label;
	private HashSet<OpenFile> hmap = new HashSet<OpenFile>();
	private Set<String> treeExpansionState;
	private boolean open = false;
	private State state;
	private ConfigSaver configSaver;
	private LuytenPreferences luytenPrefs;

	public Model(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.bar = mainWindow.getBar();
		this.setLabel(mainWindow.getLabel());

		configSaver = ConfigSaver.getLoadedInstance();
		settings = configSaver.getDecompilerSettings();
		luytenPrefs = configSaver.getLuytenPreferences();

		try {
			String themeXml = luytenPrefs.getThemeXml();
			theme = Theme.load(getClass().getResourceAsStream(LuytenPreferences.THEME_XML_PATH + themeXml));
		} catch (Exception e1) {
			try {
				e1.printStackTrace();
				String themeXml = LuytenPreferences.DEFAULT_THEME_XML;
				luytenPrefs.setThemeXml(themeXml);
				theme = Theme.load(getClass().getResourceAsStream(LuytenPreferences.THEME_XML_PATH + themeXml));
			} catch (Exception e2) {
				e2.printStackTrace();
			}
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
		house.addChangeListener(new TabChangeListener());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, 1));
		panel.setBorder(BorderFactory.createTitledBorder("Code"));
		panel.add(house);
		this.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		this.setDividerLocation(250 % mainWindow.getWidth());
		this.setLeftComponent(panel2);
		this.setRightComponent(panel);

		decompilationOptions = new DecompilationOptions();
		decompilationOptions.setSettings(settings);
		decompilationOptions.setFullDecompilation(true);
	}

	public void showLegal(String legalStr) {
		show("Legal", legalStr);
	}

	public void show(String name, String contents) {
		OpenFile open = new OpenFile(name, "*/"+name, theme, mainWindow);
		open.setContent(contents);
		hmap.add(open);
		addOrSwitchToTab(open);
	}

	private void addOrSwitchToTab(final OpenFile open) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					final String title = open.name;
					RTextScrollPane rTextScrollPane = open.scrollPane;
					if (house.indexOfTab(title) < 0) {
						house.addTab(title, rTextScrollPane);
						house.setSelectedIndex(house.indexOfTab(title));
						int index = house.indexOfTab(title);
						Tab ct = new Tab(title);
						ct.getButton().addMouseListener(new CloseTab(title));
						ct.addMouseListener(new MouseAdapter(){
							@Override
							public void mouseClicked(MouseEvent e) {
								if(SwingUtilities.isMiddleMouseButton(e)){
									int index = house.indexOfTab(title);
									closeOpenTab(index);
								}
							}
						});
						house.setTabComponentAt(index, ct);
					} else {
						house.setSelectedIndex(house.indexOfTab(title));
					}
					open.onAddedToScreen();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void closeOpenTab(int index) {
		RTextScrollPane co = (RTextScrollPane) house.getComponentAt(index);
		RSyntaxTextArea pane = (RSyntaxTextArea) co.getViewport().getView();
		OpenFile open = null;
		for (OpenFile file : hmap)
			if (pane.equals(file.textArea))
				open = file;
		if (open != null && hmap.contains(open))
			hmap.remove(open);
		house.remove(co);
		if (open != null)
			open.close();
	}

	private String getName(String path) {
		if (path == null)
			return "";
		int i = path.lastIndexOf("/");
		if (i == -1)
			i = path.lastIndexOf("\\");
		if (i != -1)
			return path.substring(i + 1);
		return path;
	}

	private class TreeListener extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent event) {
			boolean isClickCountMatches = (event.getClickCount() == 1 && luytenPrefs.isSingleClickOpenEnabled())
					|| (event.getClickCount() == 2 && !luytenPrefs.isSingleClickOpenEnabled());
			if (!isClickCountMatches)
				return;

			if (!SwingUtilities.isLeftMouseButton(event))
				return;

			final TreePath trp = tree.getPathForLocation(event.getX(), event.getY());
			if (trp == null)
				return;

			Object lastPathComponent = trp.getLastPathComponent();
			boolean isLeaf = (lastPathComponent instanceof TreeNode && ((TreeNode) lastPathComponent).isLeaf());
			if (!isLeaf)
				return;

			new Thread() {
				public void run() {
					openEntryByTreePath(trp);
				}
			}.start();
		}
	}

	public void openEntryByTreePath(TreePath trp) {
		String name = "";
		String path = "";
		try {
			bar.setVisible(true);
			if (trp.getPathCount() > 1) {
				for (int i = 1; i < trp.getPathCount(); i++) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) trp.getPathComponent(i);
					TreeNodeUserObject userObject = (TreeNodeUserObject) node.getUserObject();
					if (i == trp.getPathCount() - 1) {
						name = userObject.getOriginalName();
					} else {
						path = path + userObject.getOriginalName() + "/";
					}
				}
				path = path + name;

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
					String entryName = entry.getName();
					if (entryName.endsWith(".class")) {
						getLabel().setText("Extracting: " + name);
						String internalName = StringUtilities.removeRight(entryName, ".class");
						TypeReference type = metadataSystem.lookupType(internalName);
						extractClassToTextPane(type, name, path, null);
					} else {
						getLabel().setText("Opening: " + name);
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
					getLabel().setText("Extracting: " + name);
					TypeReference type = metadataSystem.lookupType(path);
					extractClassToTextPane(type, name, path, null);
				} else {
					getLabel().setText("Opening: " + name);
					try (InputStream in = new FileInputStream(file);) {
						extractSimpleFileEntryToTextPane(in, name, path);
					}
				}
			}
				
			getLabel().setText("Complete");
		} catch (FileEntryNotFoundException e) {
			getLabel().setText("File not found: " + name);
		} catch (FileIsBinaryException e) {
			getLabel().setText("Binary resource: " + name);
		} catch (TooLargeFileException e) {
			getLabel().setText("File is too large: " + name + " - size: " + e.getReadableFileSize());
		} catch (Exception e) {
			getLabel().setText("Cannot open: " + name);
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
		} finally {
			bar.setVisible(false);
		}
	}

	void extractClassToTextPane(TypeReference type, String tabTitle, String path,
			String navigatonLink) throws Exception {
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
		if (sameTitledOpen != null && path.equals(sameTitledOpen.path) &&
				type.equals(sameTitledOpen.getType()) && sameTitledOpen.isContentValid()) {
			sameTitledOpen.setInitialNavigationLink(navigatonLink);
			addOrSwitchToTab(sameTitledOpen);
			return;
		}

		// resolve TypeDefinition
		TypeDefinition resolvedType = null;
		if (type == null || ((resolvedType = type.resolve()) == null)) {
			throw new Exception("Unable to resolve type.");
		}

		// open tab, store type information, start decompilation
		if (sameTitledOpen != null) {
			sameTitledOpen.path = path;
			sameTitledOpen.invalidateContent();
			sameTitledOpen.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
			sameTitledOpen.setType(resolvedType);
			sameTitledOpen.setInitialNavigationLink(navigatonLink);
			sameTitledOpen.resetScrollPosition();
			sameTitledOpen.decompile();
			addOrSwitchToTab(sameTitledOpen);
		} else {
			OpenFile open = new OpenFile(tabTitle, path, theme, mainWindow);
			open.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
			open.setType(resolvedType);
			open.setInitialNavigationLink(navigatonLink);
			open.decompile();
			hmap.add(open);
			addOrSwitchToTab(open);
		}
	}

	public void extractSimpleFileEntryToTextPane(InputStream inputStream, String tabTitle, String path)
			throws Exception {
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
		if (sameTitledOpen != null && path.equals(sameTitledOpen.path)) {
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
			sameTitledOpen.path = path;
			sameTitledOpen.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
			sameTitledOpen.resetScrollPosition();
			sameTitledOpen.setContent(sb.toString());
			addOrSwitchToTab(sameTitledOpen);
		} else {
			OpenFile open = new OpenFile(tabTitle, path, theme, mainWindow);
			open.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
			open.setContent(sb.toString());
			hmap.add(open);
			addOrSwitchToTab(open);
		}
	}

	private class TabChangeListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			int selectedIndex = house.getSelectedIndex();
			if (selectedIndex < 0) {
				return;
			}
			for (OpenFile open : hmap) {
				if (house.indexOfTab(open.name) == selectedIndex) {

					if (open.getType() != null && !open.isContentValid()) {
						updateOpenClass(open);
						break;
					}

				}
			}
		}
	}
	
	public void updateOpenClasses() {
		// invalidate all open classes (update will hapen at tab change)
		for (OpenFile open : hmap) {
			if (open.getType() != null) {
				open.invalidateContent();
			}
		}
		// update the current open tab - if it is a class
		for (OpenFile open : hmap) {
			if (open.getType() != null && isTabInForeground(open)) {
				updateOpenClass(open);
				break;
			}
		}
	}

	private void updateOpenClass(final OpenFile open) {
		if (open.getType() == null) {
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					bar.setVisible(true);
					getLabel().setText("Extracting: " + open.name);
					open.invalidateContent();
					open.decompile();
					getLabel().setText("Complete");
				} catch (Exception e) {
					getLabel().setText("Error, cannot update: " + open.name);
				} finally {
					bar.setVisible(false);
				}
			}
		}).start();
	}

	private boolean isTabInForeground(OpenFile open) {
		String title = open.name;
		int selectedIndex = house.getSelectedIndex();
		return (selectedIndex >= 0 && selectedIndex == house.indexOfTab(title));
	}

	final class State implements AutoCloseable {
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
				Model.typeLoader.getTypeLoaders().remove(typeLoader);
			}
			Closer.tryClose(jarFile);
		}

		@SuppressWarnings("unused")
		public File getFile() {
			return file;
		}

		@SuppressWarnings("unused")
		public String getKey() {
			return key;
		}
	}

	private class Tab extends JPanel {
		private static final long serialVersionUID = -514663009333644974L;
		private JLabel closeButton = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
				this.getClass().getResource("/resources/icon_close.png"))));
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

	public DefaultMutableTreeNode loadNodesByNames(DefaultMutableTreeNode node, List<String> originalNames) {
		List<TreeNodeUserObject> args = new ArrayList<>();
		for (String originalName : originalNames) {
			args.add(new TreeNodeUserObject(originalName));
		}
		return loadNodesByUserObj(node, args);
	}

	public DefaultMutableTreeNode loadNodesByUserObj(DefaultMutableTreeNode node, List<TreeNodeUserObject> args) {
		if (args.size() > 0) {
			TreeNodeUserObject name = args.remove(0);
			DefaultMutableTreeNode nod = getChild(node, name);
			if (nod == null)
				nod = new DefaultMutableTreeNode(name);
			node.add(loadNodesByUserObj(nod, args));
		}
		return node;
	}

	@SuppressWarnings("unchecked")
	public DefaultMutableTreeNode getChild(DefaultMutableTreeNode node, TreeNodeUserObject name) {
		Enumeration<DefaultMutableTreeNode> entry = node.children();
		while (entry.hasMoreElements()) {
			DefaultMutableTreeNode nods = entry.nextElement();
			if (((TreeNodeUserObject) nods.getUserObject()).getOriginalName().equals(name.getOriginalName())) {
				return nods;
			}
		}
		return null;
	}

	public void loadFile(File file) {
		if (open)
			closeFile();
		this.file = file;
		loadTree();
	}

	public void updateTree() {
		TreeUtil treeUtil = new TreeUtil(tree);
		treeExpansionState = treeUtil.getExpansionState();
		loadTree();
	}

	public void loadTree() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (file == null) {
						return;
					}
					tree.setModel(new DefaultTreeModel(null));

					if (file.length() > MAX_JAR_FILE_SIZE_BYTES) {
						throw new TooLargeFileException(file.length());
					}
					if (file.getName().endsWith(".zip") || file.getName().endsWith(".jar")) {
						JarFile jfile;
						jfile = new JarFile(file);
						getLabel().setText("Loading: " + jfile.getName());
						bar.setVisible(true);

						JarEntryFilter jarEntryFilter = new JarEntryFilter(jfile);
						List<String> mass = null;
						if (luytenPrefs.isFilterOutInnerClassEntries()) {
							mass = jarEntryFilter.getEntriesWithoutInnerClasses();
						} else {
							mass = jarEntryFilter.getAllEntriesFromJar();
						}
						buildTreeFromMass(mass);

						if (state == null) {
							ITypeLoader jarLoader = new JarTypeLoader(jfile);
							typeLoader.getTypeLoaders().add(jarLoader);
							state = new State(file.getCanonicalPath(), file, jfile, jarLoader);
						}
						open = true;
						getLabel().setText("Complete");
					} else {
						TreeNodeUserObject topNodeUserObject = new TreeNodeUserObject(getName(file.getName()));
						final DefaultMutableTreeNode top = new DefaultMutableTreeNode(topNodeUserObject);
						tree.setModel(new DefaultTreeModel(top));
						settings.setTypeLoader(new InputTypeLoader());
						open = true;
						getLabel().setText("Complete");

						// open it automatically
						new Thread() {
							public void run() {
								TreePath trp = new TreePath(top.getPath());
								openEntryByTreePath(trp);
							};
						}.start();
					}

					if (treeExpansionState != null) {
						try {
							TreeUtil treeUtil = new TreeUtil(tree);
							treeUtil.restoreExpanstionState(treeExpansionState);
						} catch (Exception exc) {
							exc.printStackTrace();
						}
					}
				} catch (TooLargeFileException e) {
					getLabel().setText("File is too large: " + file.getName() + " - size: " + e.getReadableFileSize());
					closeFile();
				} catch (Exception e1) {
					e1.printStackTrace();
					getLabel().setText("Cannot open: " + file.getName());
					closeFile();
				} finally {
					mainWindow.onFileLoadEnded(file, open);
					bar.setVisible(false);
				}
			}

		}).start();
	}

	private void buildTreeFromMass(List<String> mass) {
		if (luytenPrefs.isPackageExplorerStyle()) {
			buildFlatTreeFromMass(mass);
		} else {
			buildDirectoryTreeFromMass(mass);
		}
	}

	private void buildDirectoryTreeFromMass(List<String> mass) {
		TreeNodeUserObject topNodeUserObject = new TreeNodeUserObject(getName(file.getName()));
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(topNodeUserObject);
		List<String> sort = new ArrayList<String>();
		Collections.sort(mass, String.CASE_INSENSITIVE_ORDER);
		for (String m : mass)
			if (m.contains("META-INF") && !sort.contains(m))
				sort.add(m);
		Set<String> set = new HashSet<String>();
		for (String m : mass) {
			if (m.contains("/")) {
				set.add(m.substring(0, m.lastIndexOf("/") + 1));
			}
		}
		List<String> packs = Arrays.asList(set.toArray(new String[] {}));
		Collections.sort(packs, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(packs, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return o2.split("/").length - o1.split("/").length;
			}
		});
		for (String pack : packs)
			for (String m : mass)
				if (!m.contains("META-INF") && m.contains(pack)
						&& !m.replace(pack, "").contains("/"))
					sort.add(m);
		for (String m : mass)
			if (!m.contains("META-INF") && !m.contains("/") && !sort.contains(m))
				sort.add(m);
		for (String pack : sort) {
			LinkedList<String> list = new LinkedList<String>(Arrays.asList(pack.split("/")));
			loadNodesByNames(top, list);
		}
		tree.setModel(new DefaultTreeModel(top));
	}

	private void buildFlatTreeFromMass(List<String> mass) {
		TreeNodeUserObject topNodeUserObject = new TreeNodeUserObject(getName(file.getName()));
		DefaultMutableTreeNode top = new DefaultMutableTreeNode(topNodeUserObject);

		TreeMap<String, TreeSet<String>> packages = new TreeMap<>();
		HashSet<String> classContainingPackageRoots = new HashSet<>();

		Comparator<String> sortByFileExtensionsComparator = new Comparator<String>() {
			// (assertion: mass does not contain null elements)
			@Override
			public int compare(String o1, String o2) {
				int comp = o1.replaceAll("[^\\.]*\\.", "").compareTo(o2.replaceAll("[^\\.]*\\.", ""));
				if (comp != 0)
					return comp;
				return o1.compareTo(o2);
			}
		};

		for (String entry : mass) {
			String packagePath = "";
			String packageRoot = "";
			if (entry.contains("/")) {
				packagePath = entry.replaceAll("/[^/]*$", "");
				packageRoot = entry.replaceAll("/.*$", "");
			}
			String packageEntry = entry.replace(packagePath + "/", "");
			if (!packages.containsKey(packagePath)) {
				packages.put(packagePath, new TreeSet<String>(sortByFileExtensionsComparator));
			}
			packages.get(packagePath).add(packageEntry);
			if (!entry.startsWith("META-INF") && packageRoot.trim().length() > 0 &&
					entry.matches(".*\\.(class|java|prop|properties)$")) {
				classContainingPackageRoots.add(packageRoot);
			}
		}

		// META-INF comes first -> not flat
		for (String packagePath : packages.keySet()) {
			if (packagePath.startsWith("META-INF")) {
				List<String> packagePathElements = Arrays.asList(packagePath.split("/"));
				for (String entry : packages.get(packagePath)) {
					ArrayList<String> list = new ArrayList<>(packagePathElements);
					list.add(entry);
					loadNodesByNames(top, list);
				}
			}
		}

		// real packages: path starts with a classContainingPackageRoot -> flat
		for (String packagePath : packages.keySet()) {
			String packageRoot = packagePath.replaceAll("/.*$", "");
			if (classContainingPackageRoots.contains(packageRoot)) {
				for (String entry : packages.get(packagePath)) {
					ArrayList<TreeNodeUserObject> list = new ArrayList<>();
					list.add(new TreeNodeUserObject(packagePath, packagePath.replaceAll("/", ".")));
					list.add(new TreeNodeUserObject(entry));
					loadNodesByUserObj(top, list);
				}
			}
		}

		// the rest, not real packages but directories -> not flat
		for (String packagePath : packages.keySet()) {
			String packageRoot = packagePath.replaceAll("/.*$", "");
			if (!classContainingPackageRoots.contains(packageRoot) &&
					!packagePath.startsWith("META-INF") && packagePath.length() > 0) {
				List<String> packagePathElements = Arrays.asList(packagePath.split("/"));
				for (String entry : packages.get(packagePath)) {
					ArrayList<String> list = new ArrayList<>(packagePathElements);
					list.add(entry);
					loadNodesByNames(top, list);
				}
			}
		}

		// the default package -> not flat
		String packagePath = "";
		if (packages.containsKey(packagePath)) {
			for (String entry : packages.get(packagePath)) {
				ArrayList<String> list = new ArrayList<>();
				list.add(entry);
				loadNodesByNames(top, list);
			}
		}
		tree.setModel(new DefaultTreeModel(top));
	}

	public void closeFile() {
		for (OpenFile co : hmap) {
			int pos = house.indexOfTab(co.name);
			if (pos >= 0)
				house.remove(pos);
			co.close();
		}

		final State oldState = state;
		Model.this.state = null;
		if (oldState != null) {
			Closer.tryClose(oldState);
		}

		hmap.clear();
		tree.setModel(new DefaultTreeModel(null));
		metadataSystem = new MetadataSystem(typeLoader);
		file = null;
		treeExpansionState = null;
		open = false;
		mainWindow.onFileLoadEnded(file, open);
	}

	public void changeTheme(String xml) {
		InputStream in = getClass().getResourceAsStream(LuytenPreferences.THEME_XML_PATH + xml);
		try {
			if (in != null) {
				theme = Theme.load(in);
				for (OpenFile f : hmap) {
					theme.apply(f.textArea);
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
		}
	}

	public File getOpenedFile() {
		File openedFile = null;
		if (file != null && open) {
			openedFile = file;
		}
		if (openedFile == null) {
			getLabel().setText("No open file");
		}
		return openedFile;
	}

	public String getCurrentTabTitle() {
		String tabTitle = null;
		try {
			int pos = house.getSelectedIndex();
			if (pos >= 0) {
				tabTitle = house.getTitleAt(pos);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if (tabTitle == null) {
			getLabel().setText("No open tab");
		}
		return tabTitle;
	}

	public RSyntaxTextArea getCurrentTextArea() {
		RSyntaxTextArea currentTextArea = null;
		try {
			int pos = house.getSelectedIndex();
			if (pos >= 0) {
				RTextScrollPane co = (RTextScrollPane) house.getComponentAt(pos);
				currentTextArea = (RSyntaxTextArea) co.getViewport().getView();
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		if (currentTextArea == null) {
			getLabel().setText("No open tab");
		}
		return currentTextArea;
	}

	public void startWarmUpThread() {
		new Thread() {
			public void run() {
				try {
					Thread.sleep(500);
					String internalName = FindBox.class.getName();
					TypeReference type = metadataSystem.lookupType(internalName);
					TypeDefinition resolvedType = null;
					if ((type == null) || ((resolvedType = type.resolve()) == null)) {
						return;
					}
					StringWriter stringwriter = new StringWriter();
					PlainTextOutput plainTextOutput = new PlainTextOutput(stringwriter);
					plainTextOutput.setUnicodeOutputEnabled(decompilationOptions.getSettings().isUnicodeOutputEnabled());
					settings.getLanguage().decompileType(resolvedType, plainTextOutput, decompilationOptions);
					String decompiledSource = stringwriter.toString();
					OpenFile open = new OpenFile(internalName, "*/" + internalName, theme, mainWindow);
					open.setContent(decompiledSource);
					JTabbedPane pane = new JTabbedPane();
					pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
					pane.addTab("title", open.scrollPane);
					pane.setSelectedIndex(pane.indexOfTab("title"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	public void navigateTo(final String uniqueStr) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (uniqueStr == null)
					return;
				String[] linkParts = uniqueStr.split("\\|");
				if (linkParts.length <= 1)
					return;
				String destinationTypeStr = linkParts[1];
				try {
					bar.setVisible(true);
					getLabel().setText("Navigating: " + destinationTypeStr.replaceAll("/", "."));

					TypeReference type = metadataSystem.lookupType(destinationTypeStr);
					if (type == null)
						throw new RuntimeException("Cannot lookup type: " + destinationTypeStr);
					TypeDefinition typeDef = type.resolve();
					if (typeDef == null)
						throw new RuntimeException("Cannot resolve type: " + destinationTypeStr);

					String tabTitle = typeDef.getName() + ".class";
					extractClassToTextPane(typeDef, tabTitle, destinationTypeStr, uniqueStr);

					getLabel().setText("Complete");
				} catch (Exception e) {
					getLabel().setText("Cannot navigate: " + destinationTypeStr.replaceAll("/", "."));
					e.printStackTrace();
				} finally {
					bar.setVisible(false);
				}
			}
		}).start();
	}

	public JLabel getLabel() {
		return label;
	}

	public void setLabel(JLabel label) {
		this.label = label;
	}
	
	public State getState(){
		return state;
	}
	
	
}
