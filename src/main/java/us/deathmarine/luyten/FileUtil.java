package us.deathmarine.luyten;

import java.io.File;
import org.fife.ui.rsyntaxtextarea.FileTypeUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public final class FileUtil {

    private static final FileTypeUtil FILE_TYPE_UTIL = FileTypeUtil.get();

    private FileUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getLanguage(String fileName) {
        // Hard code .class -> Java mapping
        if (fileName.toLowerCase().endsWith(".class")) {
            return SyntaxConstants.SYNTAX_STYLE_JAVA;
        }
        // Otherwise, let RSTA do the job
        return FILE_TYPE_UTIL.guessContentType(new File(fileName));
    }

    public static void setLanguage(RSyntaxTextArea area, String fileName) {
        String type = getLanguage(fileName);
        if (type == null || type.equals(SyntaxConstants.SYNTAX_STYLE_NONE)) {
            type = FILE_TYPE_UTIL.guessContentType(area);
        }
        area.setSyntaxEditingStyle(type);
    }

}
