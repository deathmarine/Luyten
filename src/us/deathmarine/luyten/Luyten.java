package us.deathmarine.luyten;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Starter, the main class
 */
public class Luyten {

	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// for TotalCommander External Viewer setting:
		// javaw -jar "c:\Program Files\Luyten\luyten.jar"
		// (TC will not complain about temporary file when opening .class from .zip or .jar)
		final File fileFromCommandLine = getFileFromCommandLine(args);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				MainWindow mainWindow = new MainWindow(fileFromCommandLine);
				mainWindow.setVisible(true);
			}
		});
	}

	public static File getFileFromCommandLine(String[] args) {
		File fileFromCommandLine = null;
		try {
			if (args.length > 0) {
				String realFileName = new File(args[0]).getCanonicalPath();
				fileFromCommandLine = new File(realFileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileFromCommandLine;
	}
	
	public static String getVersion(){
		String result = "";
		try {
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("META-INF/maven/us.deathmarine/luyten/pom.properties")));
			while((line = br.readLine())!= null){
				if(line.contains("version")) 
					result = line.split("=")[1];
			}
			br.close();
		} catch (Exception e) {
			return result;
		}
		return result;
		
	}
}
