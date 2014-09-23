package us.deathmarine.luyten;

import java.awt.Panel;
import java.util.Arrays;
import java.util.HashSet;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import com.strobel.assembler.metadata.TypeReference;

public class OpenFile implements SyntaxConstants {

	public static final HashSet<String> WELL_KNOWN_TEXT_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(
			".java", ".xml", ".rss", ".project", ".classpath", ".h", ".sql", ".js", ".php", ".php5",
			".phtml", ".html", ".htm", ".xhtm", ".xhtml", ".lua", ".bat", ".pl", ".sh", ".css",
			".json", ".txt", ".rb", ".make", ".mak", ".py", ".properties", ".prop"));

	RTextScrollPane scrollPane;
	Panel image_pane;
	RSyntaxTextArea textArea;
	String name;
	private String path;
	private TypeReference type = null;
	private boolean isContentValid = false;

	public OpenFile(TypeReference type, String name, String path, String content, Theme theme) {
		this(name, path, content, theme);
		this.type = type;
	}
	
	public OpenFile(String name, String path, String contents, Theme theme) {
		this.name = name;
		this.path = path;
		textArea = new RSyntaxTextArea(25, 70);
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		textArea.setEditable(false);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		if (name.toLowerCase().endsWith(".class")
				|| name.toLowerCase().endsWith(".java"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_JAVA);
		else if (name.toLowerCase().endsWith(".xml")
				|| name.toLowerCase().endsWith(".rss")
				|| name.toLowerCase().endsWith(".project")
				|| name.toLowerCase().endsWith(".classpath"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_XML);
		else if (name.toLowerCase().endsWith(".h"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_C);
		else if (name.toLowerCase().endsWith(".sql"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_SQL);
		else if (name.toLowerCase().endsWith(".js"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_JAVASCRIPT);
		else if (name.toLowerCase().endsWith(".php")
				|| name.toLowerCase().endsWith(".php5")
				|| name.toLowerCase().endsWith(".phtml"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_PHP);
		else if (name.toLowerCase().endsWith(".html")
				|| name.toLowerCase().endsWith(".htm")
				|| name.toLowerCase().endsWith(".xhtm")
				|| name.toLowerCase().endsWith(".xhtml"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_HTML);
		else if (name.toLowerCase().endsWith(".js"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_JAVASCRIPT);
		else if (name.toLowerCase().endsWith(".lua"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_LUA);
		else if (name.toLowerCase().endsWith(".bat"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_WINDOWS_BATCH);
		else if (name.toLowerCase().endsWith(".pl"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_PERL);
		else if (name.toLowerCase().endsWith(".sh"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_UNIX_SHELL);
		else if (name.toLowerCase().endsWith(".css"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_CSS);
		else if (name.toLowerCase().endsWith(".json"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_JSON);
		else if (name.toLowerCase().endsWith(".txt"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_NONE);
		else if (name.toLowerCase().endsWith(".rb"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_RUBY);
		else if (name.toLowerCase().endsWith(".make")
				|| name.toLowerCase().endsWith(".mak"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_MAKEFILE);
		else if (name.toLowerCase().endsWith(".py"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_PYTHON);
		else
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_PROPERTIES_FILE);
		scrollPane = new RTextScrollPane(textArea, true);
		scrollPane.setIconRowHeaderEnabled(true);
		textArea.setText(contents);
		theme.apply(textArea);
	}

	public void setContent(String content) {
		textArea.setText(content);
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public TypeReference getType() {
		return type;
	}

	public void setType(TypeReference type) {
		this.type = type;
	}

	public boolean isContentValid() {
		return isContentValid;
	}

	public void setContentValid(boolean isContentValid) {
		this.isContentValid = isContentValid;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpenFile other = (OpenFile) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
