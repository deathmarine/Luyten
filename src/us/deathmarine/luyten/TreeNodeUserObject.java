package us.deathmarine.luyten;

public class TreeNodeUserObject {

	private String originalName;
	private String displayName;

	public TreeNodeUserObject(String name) {
		this(name, name);
	}

	public TreeNodeUserObject(String originalName, String displayName) {
		this.originalName = originalName;
		this.displayName = displayName;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String originalName) {
		this.originalName = originalName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}
}
