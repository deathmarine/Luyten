package us.deathmarine.luyten;

/**
 * Do not instantiate this class, get the instance from ConfigSaver. All
 * not-static fields will be saved automatically named by the field's java
 * variable name. (Watch for collisions with existing IDs defined in
 * ConfigSaver.) Only String, boolean and int fields are supported. Write
 * default values into the field declarations.
 */
public class LuytenPreferences {
	public static final String THEME_XML_PATH = "/org/fife/ui/rsyntaxtextarea/themes/";
	public static final String DEFAULT_THEME_XML = "eclipse.xml";

	private String themeXml = DEFAULT_THEME_XML;
	private String fileOpenCurrentDirectory = "";
	private String fileSaveCurrentDirectory = "";
	private int font_size = 10;

	private boolean isPackageExplorerStyle = true;
	private boolean isFilterOutInnerClassEntries = true;
	private boolean isSingleClickOpenEnabled = true;
	private boolean isExitByEscEnabled = false;
	private boolean isDiscordIntegrationEnabled = false;
	private boolean isFollowEmbeddedJarFile = true;
	private boolean isPrepareWarFile = false;


	public String getThemeXml() {
		return themeXml;
	}

	public void setThemeXml(String themeXml) {
		this.themeXml = themeXml;
	}

	public String getFileOpenCurrentDirectory() {
		return fileOpenCurrentDirectory;
	}

	public void setFileOpenCurrentDirectory(String fileOpenCurrentDirectory) {
		this.fileOpenCurrentDirectory = fileOpenCurrentDirectory;
	}

	public String getFileSaveCurrentDirectory() {
		return fileSaveCurrentDirectory;
	}

	public void setFileSaveCurrentDirectory(String fileSaveCurrentDirectory) {
		this.fileSaveCurrentDirectory = fileSaveCurrentDirectory;
	}

	public boolean isPackageExplorerStyle() {
		return isPackageExplorerStyle;
	}

	public void setPackageExplorerStyle(boolean isPackageExplorerStyle) {
		this.isPackageExplorerStyle = isPackageExplorerStyle;
	}

	public void setDiscordIntegration(boolean isDiscordIntegrationEnabled) {
		this.isDiscordIntegrationEnabled = isDiscordIntegrationEnabled;
	}

	public boolean isFilterOutInnerClassEntries() {
		return isFilterOutInnerClassEntries;
	}

	public void setFilterOutInnerClassEntries(boolean isFilterOutInnerClassEntries) {
		this.isFilterOutInnerClassEntries = isFilterOutInnerClassEntries;
	}

	public boolean isSingleClickOpenEnabled() {
		return isSingleClickOpenEnabled;
	}

	public void setSingleClickOpenEnabled(boolean isSingleClickOpenEnabled) {
		this.isSingleClickOpenEnabled = isSingleClickOpenEnabled;
	}

	public boolean isDiscordIntegrationEnabled() {
		return isDiscordIntegrationEnabled;
	}

	public boolean isExitByEscEnabled() {
		return isExitByEscEnabled;
	}

	public void setExitByEscEnabled(boolean isExitByEscEnabled) {
		this.isExitByEscEnabled = isExitByEscEnabled;
	}

	/*
	 * Maybe this makes sense for WAR files only (located in WEB-INF/lib)
	 * If opened input file is a JAR / ZIP or WAR file and the package contains a JAR file inside
	 * control what happens with that embedded JAR file
	 * should be unprocessed and displayed only or opened by accessing them
	 * isFollowEmbeddedJarFile:
	 * true  = by selecting the JAR file, it is opened as new input file
	 * false = do not follow that embedded JAR file, just display in the tree as JAR file and notify user as binary resource file
	 */
	public boolean isFollowEmbeddedJarFile() {
		return isFollowEmbeddedJarFile;
	}

	public void setFollowEmbeddedJarFile(boolean isFollowEmbeddedJarFile) {
		this.isFollowEmbeddedJarFile = isFollowEmbeddedJarFile;
	}

	/*
	 * For WAR files only: They contain embedded JAR files (located in WEB-INF/lib) which has to be decompiled as well
	 * This is great to get an overall view to the source code and call hierarchy. Turn the blackbox into a whitebox ;-)
	 * isPrepareWarFile:
	 * true = unzip the WAR file and _all_ embedded JAR files with its package file name into temporary directory and create an overall ZIP file from this temporary directory
	 *        At the end this new zip file is opened and will show _all_ CLASS Files at once
	 * false  = do not pre-process the WAR file, just display embedded JAR files in the tree as JAR file - in this case maybe check the option isFollowEmbeddedJarFile to go into that JAR file
	 */
	public boolean isPrepareWarFile() {
		return isPrepareWarFile;
	}

	public void setPrepareWarFile(boolean isPrepareWarFile) {
		this.isPrepareWarFile = isPrepareWarFile;
	}

	
	
	public int getFont_size() {
		return font_size;
	}

	public void setFont_size(int font_size) {
		this.font_size = font_size;
	}
}
