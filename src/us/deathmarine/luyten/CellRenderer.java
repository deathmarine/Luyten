package us.deathmarine.luyten;

import java.awt.Component;
import java.awt.Toolkit;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class CellRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = -5691181006363313993L;
	Icon pack;
	Icon java_image;
	Icon yml_image;
	Icon file_image;

	public CellRenderer() {
		this.pack = new ImageIcon(
				Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/package_obj.png")));
		this.java_image = new ImageIcon(
				Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/java.png")));
		this.yml_image = new ImageIcon(
				Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/yml.png")));
		this.file_image = new ImageIcon(
				Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/resources/file.png")));
	}
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
			int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node.getChildCount() > 0) {
			setIcon(this.pack);
		} else if (getFileName(node).endsWith(".class") || getFileName(node).endsWith(".java")) {
			setIcon(this.java_image);
		} else if (getFileName(node).endsWith(".yml") || getFileName(node).endsWith(".yaml")) {
			setIcon(this.yml_image);
		} else {
			setIcon(this.file_image);
		}

		return this;
	}

	public String getFileName(DefaultMutableTreeNode node) {
		return ((TreeNodeUserObject) node.getUserObject()).getOriginalName();
	}

}
