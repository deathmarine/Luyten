package us.deathmarine.luyten;

import java.io.File;
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * An OS X-specific initialization method for dragging/dropping
 */
public class LuytenOsx extends Luyten {
	public static void main(String[] args) {
		// Set a flag that says we are running in OS X
		System.setProperty("us.deathmarine.luyten.Luyten.running_in_osx", "true");

		// Add an adapter as the handler to a new instance of the application
		// class
		@SuppressWarnings("deprecation")
		Application app = new Application();
		app.addApplicationListener(new ApplicationAdapter() {
			public void handleOpenFile(ApplicationEvent e) {
				Luyten.openFileInInstance(new File(e.getFilename()));
			}

			public void handleQuit(ApplicationEvent e) {
				Luyten.quitInstance();
			}
		});

		// Call the superclass's main function
		Luyten.main(args);
	}
}
