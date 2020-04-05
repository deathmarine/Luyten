package us.deathmarine.luyten;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.text.DefaultEditorKit;

/**
 * Starter, the main class
 */
public class Luyten {

	private static final AtomicReference<MainWindow> mainWindowRef = new AtomicReference<>();
	private static final List<File> pendingFiles = new ArrayList<>();
	private static ServerSocket lockSocket = null;

	public static void main(final String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (lockSocket != null) {
						lockSocket.close();
					}
				} catch (IOException e) {
				}
			}
		}));

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// for TotalCommander External Viewer setting:
		// javaw -jar "c:\Program Files\Luyten\luyten.jar"
		// (TC will not complain about temporary file when opening .class from
		// .zip or .jar)
		final File fileFromCommandLine = getFileFromCommandLine(args);

		try {
			launchMainInstance(fileFromCommandLine);
		} catch (Exception e) {
			// Instance already exists. Open new file in running instance
			try {
				Socket socket = new Socket("localhost", 3456);
				DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
				dos.writeUTF(args[0]);
				dos.flush();
				dos.close();
				socket.close();
			} catch (IOException ex) {
				showExceptionDialog("Exception", e);
			}
		}
	}

	private static void launchMainInstance(final File fileFromCommandLine) throws IOException {
		lockSocket = new ServerSocket(3456);
		launchSession(fileFromCommandLine);
		new Thread(new Runnable() {
			@Override
			public void run() {
				launchServer();
			}
		}).start();
	}

	private static void launchSession(final File fileFromCommandLine) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!mainWindowRef.compareAndSet(null, new MainWindow(fileFromCommandLine))) {
					// Already set - so add the files to open
					addToPendingFiles(fileFromCommandLine);
				}
				processPendingFiles();
				mainWindowRef.get().setVisible(true);
			}
		});
	}

	private static void launchServer() {
		try { // Server
			while (true) {
				Socket socket = lockSocket.accept();
				DataInputStream dis = new DataInputStream(socket.getInputStream());
				addToPendingFiles(getFileFromCommandLine(dis.readUTF()));
				processPendingFiles();
				dis.close();
				socket.close();
			}
		} catch (IOException e) { // Client
			showExceptionDialog("Exception", e);
		}
	}

	// Private function which processes all pending files - synchronized on the
	// list of pending files
	public static void processPendingFiles() {
		final MainWindow mainWindow = mainWindowRef.get();
		if (mainWindow != null) {
			synchronized (pendingFiles) {
				for (File f : pendingFiles) {
					mainWindow.loadNewFile(f);
				}
				pendingFiles.clear();
			}
		}
	}

	// Function which opens the given file in the instance, if it's running -
	// and if not, it processes the files
	public static void addToPendingFiles(File fileToOpen) {
		synchronized (pendingFiles) {
			if (fileToOpen != null) {
				pendingFiles.add(fileToOpen);
			}
		}
	}

	// Function which exits the application if it's running
	public static void quitInstance() {
		final MainWindow mainWindow = mainWindowRef.get();
		if (mainWindow != null) {
			mainWindow.onExitMenu();
		}
	}

	public static File getFileFromCommandLine(String... args) {
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

	public static String getVersion() {
		String result = "";
		try {
			String line;
			BufferedReader br = new BufferedReader(new InputStreamReader(
					ClassLoader.getSystemResourceAsStream("META-INF/maven/us.deathmarine/luyten/pom.properties")));
			while ((line = br.readLine()) != null) {
				if (line.contains("version"))
					result = line.split("=")[1];
			}
			br.close();
		} catch (Exception e) {
			return result;
		}
		return result;

	}

	/**
	 * Method allows for users to copy the stacktrace for reporting any issues.
	 * Add Cool Hyperlink Enhanced for mouse users.
	 * 
	 * @param message
	 * @param e
	 */
	public static void showExceptionDialog(String message, Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String stacktrace = sw.toString();
		try {
			sw.close();
			pw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println(stacktrace);

		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		if (message.contains("\n")) {
			for (String s : message.split("\n")) {
				pane.add(new JLabel(s));
			}
		} else {
			pane.add(new JLabel(message));
		}
		pane.add(new JLabel(" \n")); // Whitespace
		final JTextArea exception = new JTextArea(25, 100);
		exception.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		exception.setText(stacktrace);
		exception.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					new JPopupMenu() {
						{
							JMenuItem menuitem = new JMenuItem("Select All");
							menuitem.addActionListener(new ActionListener() {
								@Override
								public void actionPerformed(ActionEvent e) {
									exception.requestFocus();
									exception.selectAll();
								}
							});
							this.add(menuitem);
							menuitem = new JMenuItem("Copy");
							menuitem.addActionListener(new DefaultEditorKit.CopyAction());
							this.add(menuitem);
						}

						private static final long serialVersionUID = 562054483562666832L;
					}.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		JScrollPane scroll = new JScrollPane(exception);
		scroll.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Stacktrace"),
				new BevelBorder(BevelBorder.LOWERED)));
		pane.add(scroll);
		final String issue = "https://github.com/deathmarine/Luyten/issues";
		final JLabel link = new JLabel("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(issue));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				link.setText("<HTML>Submit to <FONT color=\"#00aa99\"><U>" + issue + "</U></FONT></HTML>");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				link.setText("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
			}
		});
		pane.add(link);
		JOptionPane.showMessageDialog(null, pane, "Error!", JOptionPane.ERROR_MESSAGE);
	}
}
