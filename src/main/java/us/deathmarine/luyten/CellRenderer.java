package us.deathmarine.luyten;

import java.awt.Component;
import java.awt.Toolkit;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public class CellRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = -5691181006363313993L;

    Icon pack;
    Icon java_image;
    Icon yml_image;
    Icon file_image;

    public CellRenderer() {
        this.pack = new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/package_obj.png")));
        this.java_image = new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/java.png")));
        this.yml_image = new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/yml.png")));
        this.file_image = new ImageIcon(
                Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/file.png")));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
                                                  int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        if (node.getChildCount() > 0) {
            setIcon(this.pack);
        } else {
            switch (FileUtil.getLanguage(getFileName(node))) {
            case SyntaxConstants.SYNTAX_STYLE_JAVA:
                setIcon(this.java_image);
                break;
            case SyntaxConstants.SYNTAX_STYLE_YAML:
                setIcon(this.yml_image);
                break;
            default:
                setIcon(this.file_image);
                break;
            }
        }

        return this;
    }

    public String getFileName(DefaultMutableTreeNode node) {
        return ((TreeNodeUserObject) node.getUserObject()).getOriginalName();
    }

}
