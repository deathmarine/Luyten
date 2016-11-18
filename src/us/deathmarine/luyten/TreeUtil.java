package us.deathmarine.luyten;

import java.util.HashSet;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class TreeUtil {

	private JTree tree;

	public TreeUtil() {
	}

	public TreeUtil(JTree tree) {
		this.tree = tree;
	}

	public Set<String> getExpansionState() {
		Set<String> openedSet = new HashSet<>();
		if (tree != null) {
			int rowCount = tree.getRowCount();
			for (int i = 0; i < rowCount; i++) {
				TreePath path = tree.getPathForRow(i);
				if (tree.isExpanded(path)) {
					String rowPathStr = getRowPathStr(path);
					// for switching Package Explorer on/off
					openedSet.addAll(getAllParentPathsStr(rowPathStr));
				}
			}
		}
		return openedSet;
	}

	private Set<String> getAllParentPathsStr(String rowPathStr) {
		Set<String> parents = new HashSet<>();
		parents.add(rowPathStr);
		if (rowPathStr.contains("/")) {
			String[] pathElements = rowPathStr.split("/");
			String path = "";
			for (String pathElement : pathElements) {
				path = path + pathElement + "/";
				parents.add(path);
			}
		}
		return parents;
	}

	public void restoreExpanstionState(Set<String> expansionState) {
		if (tree != null && expansionState != null) {
			// tree.getRowCount() changes at tree.expandRow()
			for (int i = 0; i < tree.getRowCount(); i++) {
				TreePath path = tree.getPathForRow(i);
				if (expansionState.contains(getRowPathStr(path))) {
					tree.expandRow(i);
				}
			}
		}
	}

	private String getRowPathStr(TreePath trp) {
		String pathStr = "";
		if (trp.getPathCount() > 1) {
			for (int i = 1; i < trp.getPathCount(); i++) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) trp.getPathComponent(i);
				TreeNodeUserObject userObject = (TreeNodeUserObject) node.getUserObject();
				pathStr = pathStr + userObject.getOriginalName() + "/";
			}
		}
		return pathStr;
	}

	public JTree getTree() {
		return tree;
	}

	public void setTree(JTree tree) {
		this.tree = tree;
	}
}
