/*
 * 09/05/2004
 *
 * IconGroup.java - Class encapsulating images used for RTextArea actions.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.AccessControlException;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;


/**
 * This class encapsulates the location, properties, etc. of an icon set used
 * for an instance of <code>RTextArea</code>.  If the location of the icon
 * group is invalid in any way, any attempt to retrieve icons from an icon
 * group will return <code>null</code>.
 *
 * @author Robert Futrell
 * @version 0.5
 */
public class IconGroup {

	private String path;
	private boolean separateLargeIcons;
	private String largeIconSubDir;
	private String extension;
	private String name;
	private String jarFile;

	private static final String DEFAULT_EXTENSION	= "gif";


	/**
	 * Creates an icon set without "large versions" of the icons.
	 *
	 * @param name The name of the icon group.
	 * @param path The directory containing the icon group.
	 */
	public IconGroup(String name, String path) {
		this(name, path, null);
	}


	/**
	 * Constructor.
	 *
	 * @param name The name of the icon group.
	 * @param path The directory containing the icon group.
	 * @param largeIconSubDir The subdirectory containing "large versions" of
	 *        the icons.  If no subdirectory exists, pass in <code>null</code>.
	 */
	public IconGroup(String name, String path, String largeIconSubDir) {
		this(name, path, largeIconSubDir, DEFAULT_EXTENSION);
	}


	/**
	 * Constructor.
	 *
	 * @param name The name of the icon group.
	 * @param path The directory containing the icon group.
	 * @param largeIconSubDir The subdirectory containing "large versions" of
	 *        the icons.  If no subdirectory exists, pass in <code>null</code>.
	 * @param extension The extension of the icons (one of <code>gif</code>,
	 *        <code>jpg</code>, or <code>png</code>).
	 */
	public IconGroup(String name, String path, String largeIconSubDir,
											String extension) {
		this(name, path, largeIconSubDir, extension, null);
	}


	/**
	 * Constructor.
	 *
	 * @param name The name of the icon group.
	 * @param path The directory containing the icon group.
	 * @param largeIconSubDir The subdirectory containing "large versions" of
	 *        the icons.  If no subdirectory exists, pass in <code>null</code>.
	 * @param extension The extension of the icons (one of <code>gif</code>,
	 *        <code>jpg</code>, or <code>png</code>).
	 * @param jar The Jar file containing the icons, or <code>null</code> if
	 *        the icons are on the local file system.  If a Jar is specified,
	 *        the value of <code>path</code> must be a path in the Jar file.
	 *        If this is not a valid Jar file, then no Jar file will be used,
	 *        meaning all icons returned from this icon group will be
	 *        <code>null</code>.
	 */
	public IconGroup(String name, String path, String largeIconSubDir,
					String extension, String jar) {
		this.name = name;
		this.path = path;
		if (path!=null && path.length()>0 && !path.endsWith("/")) {
			this.path += "/";
		}
		this.separateLargeIcons = (largeIconSubDir!=null);
		this.largeIconSubDir = largeIconSubDir;
		this.extension = extension!=null ? extension : DEFAULT_EXTENSION;
		this.jarFile = jar;
	}


	/**
	 * Returns whether two icon groups are equal.
	 *
	 * @param o2 The object to check against.
	 * @return Whether <code>o2</code> represents the same icons as this icon
	 *         group.
	 */
	public boolean equals(Object o2) {
		if (o2!=null && o2 instanceof IconGroup) {
			IconGroup ig2 = (IconGroup)o2;
			if (ig2.getName().equals(getName()) &&
					separateLargeIcons==ig2.hasSeparateLargeIcons()) {
				if (separateLargeIcons) {
					if (!largeIconSubDir.equals(ig2.largeIconSubDir))
						return false;
				}
				return path.equals(ig2.path);
			}
			// If we got here, separateLargeIcons values weren't equal.
		}
		return false;
	}


	/**
	 * Returns the icon from this icon group with the specified name.
	 *
	 * @param name The name of the icon.  For example, if you want the icon
	 * specified in <code>new.gif</code>, this value should be
	 * <code>new</code>.
	 * @return The icon, or <code>null</code> if it could not be found or
	 *         loaded.
	 * @see #getLargeIcon
	 */
	public Icon getIcon(String name) {
		Icon icon = getIconImpl(path + name + "." + extension);
		// JDK 6.0 b74 returns icons with width/height==-1 in certain error
		// cases (new ImageIcon(url) where url is not resolved?).  We'll
		// just return null in this case as Swing AbstractButtons throw
		// exceptions when expected to paint an icon with width or height
		// is less than 1.
		if (icon!=null && (icon.getIconWidth()<1 || icon.getIconHeight()<1)) {
			icon = null;
		}
		return icon;
	}


	/**
	 * Does the dirty work of loading an image.
	 *
	 * @param iconFullPath The full path to the icon, either on the local
	 *        file system or in the Jar file, if this icon group represents
	 *        icons in a Jar file.
	 * @return The icon.
	 */
	private Icon getIconImpl(String iconFullPath) {
		try {
			if (jarFile==null) {
				// First see if it's on our classpath (e.g. an icon in
				// RText.jar, so we'd need to use the class loader).
				URL url = getClass().getClassLoader().
										getResource(iconFullPath);
				if (url!=null)
					return new ImageIcon(url);
				// If not, see if it's a plain file on disk.
				BufferedImage image = ImageIO.read(new File(iconFullPath));
				return image!=null ? new ImageIcon(image) : null;
			}
			else { // If it's in a Jar, create a URL and grab it.
				URL url = new URL("jar:file:///" +
									jarFile + "!/" + iconFullPath);
				//System.err.println("***** " + url.toString());
				return new ImageIcon(url);
			}
		} catch (AccessControlException ace) {
			return null; // Likely in an applet or WebStart
		} catch (IOException ioe) {
			return null;
		}
	}


	/**
	 * Returns the large icon from this icon group with the specified name.
	 * If this icon group does not have large icons, <code>null</code> is
	 * returned.
	 *
	 * @param name The name of the icon.  For example, if you want the icon
	 *        specified in <code>new.gif</code>, this value should be
	 *        <code>new</code>.
	 * @return The icon, or <code>null</code> if it could not be found or
	 *         loaded.
	 * @see #getLargeIcon
	 */
	public Icon getLargeIcon(String name) {
		return getIconImpl(path + largeIconSubDir + "/" +
						name + "." + extension);
	}


	/**
	 * Returns the name of this icon group.
	 *
	 * @return This icon group's name.
	 */
	public String getName() {
		return name;
	}


	/**
	 * Returns whether a separate directory for the large icons exists.
	 *
	 * @return Whether a directory containing "large versions" ov the icons
	 *         exists.
	 * @see #getLargeIcon(String)
	 */
	public boolean hasSeparateLargeIcons() {
		return separateLargeIcons;
	}


	/**
	 * Overridden since we also override {@link #equals(Object)}, to honor
	 * the invariant that equal objects must have equal hashcodes.  This also
	 * keeps FindBugs happy.
	 */
	public int hashCode() {
		return getName().hashCode();
	}


}