package us.deathmarine.luyten;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Starter, the main class
 */
public class Luyten {
	
	private static final AtomicReference<MainWindow> mainWindowRef = new AtomicReference<>();
	private static final List<File> pendingFiles = new ArrayList<>();

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
				if (!mainWindowRef.compareAndSet(null, new MainWindow(fileFromCommandLine))) {
					// Already set - so add the files to open
					openFileInInstance(fileFromCommandLine);
				}
				processPendingFiles();
				mainWindowRef.get().setVisible(true);
			}
		});
	}
	
	// Private function which processes all pending files - synchronized on the list of pending files
	private static void processPendingFiles() {
		final MainWindow mainWindow = mainWindowRef.get();
		if (mainWindow != null) {
			synchronized(pendingFiles) {
				for (File f : pendingFiles) {
					mainWindow.getModel().loadFile(f);
				}
				pendingFiles.clear();
			}
		}
	}
	
	// Function which opens the given file in the instance, if it's running - and if not, it processes the files
	public static void openFileInInstance(File fileToOpen) {
		synchronized(pendingFiles) {
			if (fileToOpen != null) {
				pendingFiles.add(fileToOpen);
			}
		}
		processPendingFiles();
	}
    
    // Function which exits the application if it's running
    public static void quitInstance() {
        final MainWindow mainWindow = mainWindowRef.get();
        if (mainWindow != null) { mainWindow.onExitMenu(); }
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
