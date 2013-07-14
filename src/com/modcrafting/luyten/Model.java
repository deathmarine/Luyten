package com.modcrafting.luyten;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.DefaultEditorKit;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.JarTypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerDriver;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

public class Model extends JFrame implements WindowListener{
	private static final long serialVersionUID = 6896857630400910200L;
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
	JRadioButtonMenuItem java;
	JRadioButtonMenuItem bytecode;
	JRadioButtonMenuItem bytecodeAST;
	JRadioButtonMenuItem bytecodeUn;
	HashSet<OpenFile> hmap = new HashSet<OpenFile>();
	boolean open = false;
    public static final String JENKINS_BUILD = "JENKINSBUILDNUMBER";
	public Model(){
		frame = this;
		setup();
	}
	public Model(String string) {
		frame = this;
		setup();
		try {
			file = new File(string);
			new FileLoad(false).loadFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setup(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension center = new Dimension((int) (screenSize.width * 0.75), (int) (screenSize.height * 0.75));
        final int x = (int) (center.width * 0.2);
        final int y = (int) (center.height * 0.2);
        this.setBounds(x, y, center.width, center.height);
        this.setTitle("Luyten");
        this.addWindowListener(this);
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
        
		tree = new JTree(new DefaultMutableTreeNode("No File"));
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
	    jsp =  new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panel2, panel);
	    jsp.setDividerLocation(250 % this.getWidth());
	    this.getContentPane().add(jsp);
	    

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
	    
        menuItem = new JMenuItem("Save...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		menuItem.setEnabled(false);
		//menuItem.addActionListener(new FileExtract());
		fileMenu.add(menuItem);
		
		menuItem = new JMenuItem("Save All...");
		menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
		menuItem.setEnabled(false);
		//menuItem.addActionListener(new FileExtract());
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
		menuItem.addActionListener(new ActionListener(){
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
		menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new FindBox(Model.frame);
			}
		});
		fileMenu.add(menuItem);
	    menuBar.add(fileMenu);
	    
	    fileMenu = new JMenu("Themes");
	    ButtonGroup group = new ButtonGroup();
	    JRadioButtonMenuItem a  = new JRadioButtonMenuItem(new ThemeAction("Default", "default.xml"));
	    group.add(a);
	    fileMenu.add(a);
	    a = new JRadioButtonMenuItem(new ThemeAction("Dark", "dark.xml"));
	    group.add(a);
	    fileMenu.add(a);
	    a = new JRadioButtonMenuItem(new ThemeAction("Eclipse", "eclipse.xml"));
	    a.setSelected(true);
	    group.add(a);
	    fileMenu.add(a);
	    a = new JRadioButtonMenuItem(new ThemeAction("Visual Studio", "vs.xml"));
	    group.add(a);
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
	    fileMenu.addSeparator();

	    group = new ButtonGroup();
	    java = new JRadioButtonMenuItem("Java");
	    java.setSelected(true);
	    group.add(java);
	    fileMenu.add(java);
	    bytecode = new JRadioButtonMenuItem("ByteCode");
	    group.add(bytecode);
	    fileMenu.add(bytecode);
	    bytecodeAST = new JRadioButtonMenuItem("ByteCode AST");
	    group.add(bytecodeAST);
	    fileMenu.add(bytecodeAST);
	    bytecodeUn = new JRadioButtonMenuItem("ByteCode AST Unoptimized");
	    group.add(bytecodeUn);
	    fileMenu.add(bytecodeUn);
	    menuBar.add(fileMenu);
	    

        fileMenu = new JMenu("Help");
        menuItem = new JMenuItem("Legal");
        menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event) {
				
				StringBuilder sb = new StringBuilder();
				try {
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(getClass().getResourceAsStream("/distfiles/Procyon.License.txt")));
					String line;
					while((line = reader.readLine())!=null)
						sb.append(line).append("\n");
					sb.append("\n\n\n\n\n");
					reader = new BufferedReader(
							new InputStreamReader(getClass().getResourceAsStream("/distfiles/RSyntaxTextArea.License.txt")));
					while((line = reader.readLine())!=null)
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
        menuItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event) {
				JOptionPane.showMessageDialog(null, "Luyten Gui v0.3 Build#"+JENKINS_BUILD+"\nby Deathmarine\n\n" +
						"Powered By\nProcyon v0.4\n(c)2013 Mike Strobel\n\nRSyntaxTextArea\n(c) 2012 Robert Futrell\nAll rights reserved.");
				
			}
        });
        fileMenu.add(menuItem);
	    menuBar.add(fileMenu);
	    
	    this.setJMenuBar(menuBar);
	    this.setVisible(true);
	    

		decompilationOptions = new DecompilationOptions();
	    decompilationOptions.setSettings(settings);
	    decompilationOptions.setFullDecompilation(true);
	}
	
	public static void main(final String[] args){
		SwingUtilities.invokeLater(new Runnable(){
			@SuppressWarnings("rawtypes")
			@Override
			public void run() {
				if(args.length>0){
					if(Arrays.asList(args).contains("--nogui")){
						List<String> list = new LinkedList(Arrays.asList(args));
						list.remove("--nogui");
						DecompilerDriver.main(list.toArray(new String[]{}));
					}else{
						new Model(args[0]);
					}
				}else{
					new Model();
				}
			}
		});
	}
	
	public void addTab(String title, RTextScrollPane rTextScrollPane){
		if(house.indexOfTab(title) < 0){
			house.addTab(title, rTextScrollPane);
			house.setSelectedIndex(house.indexOfTab(title));
			int index = house.indexOfTab(title);
			Tab ct = new Tab(title);
			ct.getButton().addMouseListener(new CloseTab(title));
			house.setTabComponentAt(index, ct);
		}else{
			house.setSelectedIndex(house.indexOfTab(title));
		}
	}
	
	public void closeOpenTab(int index) {
	    RTextScrollPane co = (RTextScrollPane) house.getComponentAt(index);
	    RSyntaxTextArea pane = (RSyntaxTextArea) co.getViewport().getView();
	    OpenFile open = null;
	    for(OpenFile file : hmap)
	    	if(pane.equals(file.textArea))
	    		open = file;
	    if(open != null && hmap.contains(open))
	    	hmap.remove(open);
	    house.remove(co);
	}
	
	private class TreeListener extends MouseAdapter{
        @Override
		public void mouseClicked(MouseEvent event) {
			TreePath trp = tree.getPathForLocation(event.getX(), event.getY());
			if(trp == null)
				return;
        	if(SwingUtilities.isLeftMouseButton(event) 
        			&& event.getClickCount() == 2){
        		String st = trp.toString().replace(file.getName(), "");
    			final String[] args = st.replace("[", "").replace("]", "").split(",");
				try {
	    			if(args.length>1){
	    				String name = new String();
	    				StringBuilder sb = new StringBuilder();
	    				for(int i=1;i<args.length;i++){
	    					if(i==args.length-1){
	    						name = args[i].trim();
	    					}else{
	        					sb.append(args[i].trim()).append("/");
	    					}
	    				} 			
	    				settings.setFlattenSwitchBlocks(flattenSwitchBlocks.isSelected());
	    				settings.setForceExplicitImports(forceExplicitImports.isSelected());
	    				settings.setShowSyntheticMembers(showSyntheticMembers.isSelected());
	    				settings.setShowNestedTypes(showNestedTypes.isSelected());
	    			    settings.setForceExplicitTypeArguments(forceExplicitTypes.isSelected());
	    			    settings.setRetainRedundantCasts(retainRedundantCasts.isSelected());
	    				settings.setAlwaysGenerateExceptionVariableForCatchBlocks(true);
	    				if(java.isSelected()){
	    					settings.setLanguage(Languages.java());
	    				}else if(bytecode.isSelected()){
	    					settings.setLanguage(Languages.bytecode());
	    				}else if(bytecodeAST.isSelected()){
	    					settings.setLanguage(Languages.bytecodeAst());
	    				}else if(bytecodeUn.isSelected()){
	    					settings.setLanguage(Languages.bytecodeAstUnoptimized());
	    				}
						if(file.getName().endsWith(".jar") || file.getName().endsWith(".zip")){
							try {
								JarFile jfile = new JarFile(file);
			    				JarEntry entry = jfile.getJarEntry(sb.toString().replace(".", "/")+name);
			    				if (entry.getName().endsWith(".class")){
			    					String internalName = StringUtilities.removeRight(entry.getName(), ".class");
		    					    MetadataSystem metadataSystem = new MetadataSystem(settings.getTypeLoader());
		    					    TypeReference type = metadataSystem.lookupType(internalName);
		    					    TypeDefinition resolvedType = null;
		    					    if ((type == null) || ((resolvedType = type.resolve()) == null)) {
		    					    	//Dialog end operation
		    					    	jfile.close();
								    	return;
		    					    }
		    					    StringWriter stringwriter = new StringWriter();
		    					    settings.getLanguage().decompileType(resolvedType, new PlainTextOutput(stringwriter), decompilationOptions);
		    					    OpenFile open = new OpenFile(name, stringwriter.getBuffer().toString(), theme);
		    					    hmap.add(open);
		    					    addTab(name, open.scrollPane);
		    					    stringwriter.close();
			    				}else{
									InputStream in = jfile.getInputStream(entry);
			    					StringBuilder sd = new StringBuilder();
									if(in != null){
										BufferedReader reader = new BufferedReader(
												new InputStreamReader(in));
				    					String line;
				    					while((line = reader.readLine())!=null)
				    						sd.append(line).append("\n");
				    					reader.close();
									}
			    					OpenFile open = new OpenFile(name, sd.toString(), theme);
			    					hmap.add(open);
		    					    addTab(name, open.scrollPane);
			    				}
				    		    jfile.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
	    			}else{
	    				String name = file.getName();
	    				if(name.endsWith(".class")){
						    MetadataSystem metadataSystem = new MetadataSystem(settings.getTypeLoader());
						    TypeReference type = metadataSystem.lookupType(file.getPath());
						    TypeDefinition resolvedType = null;
						    if ((type == null) || ((resolvedType = type.resolve()) == null)) {
						    	return;
						    }
						    StringWriter stringwriter = new StringWriter();
						    settings.getLanguage().decompileType(resolvedType, new PlainTextOutput(stringwriter), decompilationOptions);
						    OpenFile open = new OpenFile(name, stringwriter.getBuffer().toString(), theme);
						    hmap.add(open);
						    addTab(name, open.scrollPane);
						    stringwriter.close();
						}else{
	    					StringBuilder sd = new StringBuilder();
							BufferedReader reader = new BufferedReader(
									new InputStreamReader(new FileInputStream(file)));
	    					String line;
	    					while((line = reader.readLine())!=null)
	    						sd.append(line).append("\n");
	    					reader.close();
	    					OpenFile open = new OpenFile(name, sd.toString(), theme);
	    					hmap.add(open);
						    addTab(name, open.scrollPane);
						}
	    			}
				} catch (IOException e) {
					e.printStackTrace();
				}
    			
        	}else{
		        tree.getSelectionModel().setSelectionPath(trp);
        	}
			
        }
	}
	
	private class Tab extends JPanel{
		private static final long serialVersionUID = -514663009333644974L;
		private JLabel closeButton = new JLabel(new ImageIcon(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/icon_close.png"))));
		private JLabel tabTitle = new JLabel();
		private String title = "";
		
		
		public Tab(String t){
			super(new GridBagLayout());
			this.setOpaque(false);
			
			this.title = t;
			this.tabTitle = new JLabel(title);
			
			this.createTab();
		}
		
		public JLabel getButton(){
			return this.closeButton;
		}
		
		public void createTab(){
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

	private class CloseTab extends MouseAdapter{
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
	
	private class FileLoad implements ActionListener{
		JFileChooser fc;
		public FileLoad(boolean dialog){
			if(dialog){
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
	    			if(open)
	    				new FileClose().actionPerformed(e);
	    		    try {
	    		    	loadFile(file);
					} catch (IOException e1) {
						//JOptionPane.showMessageDialog(null, null);
					}
	    	}
		}
	    
	    public DefaultMutableTreeNode load(DefaultMutableTreeNode node, List<String> args){
	    	if(args.size() > 0){
	    		String name = args.remove(0);
	    		DefaultMutableTreeNode nod = getChild(node, name);
	    		if(nod == null)
	    			nod = new DefaultMutableTreeNode(name);
	    		node.add(load(nod, args));
	    	}	    		
	    	return node;
	    }

		@SuppressWarnings("unchecked")
	    public DefaultMutableTreeNode getChild(DefaultMutableTreeNode node, String name){
			Enumeration<DefaultMutableTreeNode> entry = node.children();
    		while(entry.hasMoreElements()){
    			DefaultMutableTreeNode nods = entry.nextElement();
    			if(nods.getUserObject().equals(name)){
    				return nods;
    			}
    		}
    		return null;
	    }
	    
	    public String getName(String path) {
	  		if(path==null)
	  			return "";
	  		int i = path.lastIndexOf("/");
	  		if(i==-1)
	  			i = path.lastIndexOf("\\");
	  		if(i!=-1)
	  			return path.substring(i+1);
	  		return path;
	  	}
		
	    public void loadFile(File file) throws IOException{
	    	if(file.getName().endsWith(".zip") || file.getName().endsWith(".jar")){
				JarFile jfile = new JarFile(file);
    		    Enumeration<JarEntry> entry = jfile.entries();
    		    DefaultMutableTreeNode top = new DefaultMutableTreeNode(getName(file.getName()));
    		    List<String> mass = new ArrayList<String>();
    		    while(entry.hasMoreElements())
    		    	mass.add(entry.nextElement().getName());
    		    Collections.sort(mass,String.CASE_INSENSITIVE_ORDER);
    		    for(String pack : mass){
    		    	LinkedList<String> list = new LinkedList<String>(Arrays.asList(pack.split("/")));
    		    	load(top, list);
    		    }
    		    tree.setModel(new DefaultTreeModel(top));
			    settings.setTypeLoader(new JarTypeLoader(jfile));
    		    open = true;
	    	}else{
    		    DefaultMutableTreeNode top = new DefaultMutableTreeNode(getName(file.getName()));
    		    tree.setModel(new DefaultTreeModel(top));
			    settings.setTypeLoader(new InputTypeLoader());
    		    open = true;
	    	}
	    }
	}
	
	public class FileClose implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent event) {
			for(OpenFile co : hmap){
				int pos = house.indexOfTab(co.name);
				if(pos > 0)
					house.remove(pos);
			}
			hmap.clear();
			tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("No File")));
			open = false;
		}
	}
	
	public class Quit implements ActionListener{
		@Override
		public void actionPerformed(ActionEvent e) {
			Model.frame.dispose();
			System.exit(0);			
		}
	}
	
    public class FileChooserFileFilter extends FileFilter{
    	String objType;
    	public FileChooserFileFilter(String string){
    		objType=string;
    	}
    	@Override
    	public boolean accept(File f) {
    		if(f.isDirectory())
    			return false;
    		return f.getName().toLowerCase().endsWith(objType.substring(1));
    	}

    	@Override
    	public String getDescription() {
    		return objType;
    	}

    }
	private class ThemeAction extends AbstractAction {

		private static final long serialVersionUID = -6618680171943723199L;
		private String xml;

		public ThemeAction(String name, String xml) {
			putValue(NAME, name);
			this.xml = "/themes/"+xml;
		}

		public void actionPerformed(ActionEvent e) {
			InputStream in = getClass().getResourceAsStream(xml);
			try {
				if(in!=null){
					theme = Theme.load(in);
					for(OpenFile f : hmap){
						theme.apply(f.textArea);
					}
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowClosed(WindowEvent event) {
		
	}
	@Override
	public void windowClosing(WindowEvent event) {
		new Quit().actionPerformed(null);
	}
	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
