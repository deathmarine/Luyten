package us.deathmarine.luyten;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

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

    private String fontFamily = "";
    private float fontSize = 10;
    private int fontStyle = Font.PLAIN;
    private boolean fontKerning = false;
    private boolean fontLigatures = false;

    private boolean isPackageExplorerStyle = true;
    private boolean isFilterOutInnerClassEntries = true;
    private boolean isSingleClickOpenEnabled = true;
    private boolean isExitByEscEnabled = false;

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

    public boolean isExitByEscEnabled() {
        return isExitByEscEnabled;
    }

    public void setExitByEscEnabled(boolean isExitByEscEnabled) {
        this.isExitByEscEnabled = isExitByEscEnabled;
    }

    public Map<TextAttribute, Object> getFontAttributes() {
        Map<TextAttribute, Object> attrs = new HashMap<>();
        attrs.put(TextAttribute.FAMILY, fontFamily);
        attrs.put(TextAttribute.SIZE, fontSize);
        JFontChooser.addStyleToAttrs(fontStyle, attrs);
        if (fontKerning) attrs.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        if (fontLigatures) attrs.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        return attrs;
    }

    public void setFontAttributes(Map<TextAttribute, Object> attrs) {
        this.fontFamily = (String) attrs.getOrDefault(TextAttribute.FAMILY, Font.DIALOG);
        this.fontSize = (Float) attrs.getOrDefault(TextAttribute.SIZE, 10);
        this.fontStyle = JFontChooser.getStyleFromAttrs(attrs);
        this.fontKerning = attrs.getOrDefault(TextAttribute.KERNING, 0) == TextAttribute.KERNING_ON;
        this.fontLigatures = attrs.getOrDefault(TextAttribute.LIGATURES, 0) == TextAttribute.LIGATURES_ON;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

}
