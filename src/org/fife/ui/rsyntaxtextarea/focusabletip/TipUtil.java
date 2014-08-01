/*
 * 08/13/2009
 *
 * TipUtil.java - Utility methods for homemade tool tips.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.focusabletip;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.text.html.HTMLDocument;


/**
 * Static utility methods for focusable tips.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class TipUtil {


	private TipUtil() {
	}


	/**
	 * Returns a hex string for the specified color, suitable for HTML.
	 *
	 * @param c The color.
	 * @return The string representation, in the form "<code>#rrggbb</code>",
	 *         or <code>null</code> if <code>c</code> is <code>null</code>.
	 */
	private static final String getHexString(Color c) {

		if (c==null) {
			return null;
		}

		StringBuffer sb = new StringBuffer("#");
		int r = c.getRed();
		if (r<16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(r));
		int g = c.getGreen();
		if (g<16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(g));
		int b = c.getBlue();
		if (b<16) {
			sb.append('0');
		}
		sb.append(Integer.toHexString(b));

		return sb.toString();

	}


	/**
	 * Returns the screen coordinates for the monitor that contains the
	 * specified point.  This is useful for setups with multiple monitors,
	 * to ensure that popup windows are positioned properly.
	 *
	 * @param x The x-coordinate, in screen coordinates.
	 * @param y The y-coordinate, in screen coordinates.
	 * @return The bounds of the monitor that contains the specified point.
	 */
	public static Rectangle getScreenBoundsForPoint(int x, int y) {
		GraphicsEnvironment env = GraphicsEnvironment.
										getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = env.getScreenDevices();
		for (int i=0; i<devices.length; i++) {
			GraphicsConfiguration[] configs = devices[i].getConfigurations();
			for (int j=0; j<configs.length; j++) {
				Rectangle gcBounds = configs[j].getBounds();
				if (gcBounds.contains(x, y)) {
					return gcBounds;
				}
			}
		}
		// If point is outside all monitors, default to default monitor (?)
		return env.getMaximumWindowBounds();
	}


	/**
	 * Returns the default background color to use for tool tip windows.
	 *
	 * @return The default background color.
	 */
	public static Color getToolTipBackground() {

		Color c = UIManager.getColor("ToolTip.background");

		// Tooltip.background is wrong color on Nimbus (!)
		boolean isNimbus = isNimbusLookAndFeel();
		if (c==null || isNimbus) {
			c = UIManager.getColor("info"); // Used by Nimbus (and others)
			if (c==null || (isNimbus && isDerivedColor(c))) {
				c = SystemColor.info; // System default
			}
		}

		// Workaround for a bug (?) with Nimbus - calling JLabel.setBackground()
		// with a ColorUIResource does nothing, must be a normal Color
		if (c instanceof ColorUIResource) {
			c = new Color(c.getRGB());
		}

		return c;

	}


	/**
	 * Returns the border used by tool tips in this look and feel.
	 *
	 * @return The border.
	 */
	public static Border getToolTipBorder() {

		Border border = UIManager.getBorder("ToolTip.border");

		if (border==null || isNimbusLookAndFeel()) {
			border = UIManager.getBorder("nimbusBorder");
			if (border==null) {
				border = BorderFactory.createLineBorder(SystemColor.controlDkShadow);
			}
		}

		return border;

	}


	/**
	 * Returns whether a color is a Nimbus DerivedColor, which is troublesome
	 * in that it doesn't use its RGB values (uses HSB instead?) and so
	 * querying them is useless.
	 *
	 * @param c The color to check.
	 * @return Whether it is a DerivedColor
	 */
	private static final boolean isDerivedColor(Color c) {
		return c!=null && c.getClass().getName().endsWith(".DerivedColor");
	}


	/**
	 * Returns whether the Nimbus Look and Feel is installed.
	 *
	 * @return Whether the current LAF is Nimbus.
	 */
	private static final boolean isNimbusLookAndFeel() {
		return UIManager.getLookAndFeel().getName().equals("Nimbus");
	}


	/**
	 * Tweaks a <code>JEditorPane</code> so it can be used to render the
	 * content in a focusable pseudo-tool tip.  It is assumed that the editor
	 * pane is using an <code>HTMLDocument</code>.
	 *
	 * @param textArea The editor pane to tweak.
	 */
	public static void tweakTipEditorPane(JEditorPane textArea) {

		// Jump through a few hoops to get things looking nice in Nimbus
		boolean isNimbus = isNimbusLookAndFeel();
		if (isNimbus) {
			Color selBG = textArea.getSelectionColor();
			Color selFG = textArea.getSelectedTextColor();
			textArea.setUI(new javax.swing.plaf.basic.BasicEditorPaneUI());
			textArea.setSelectedTextColor(selFG);
			textArea.setSelectionColor(selBG);
		}

		textArea.setEditable(false); // Required for links to work!
		textArea.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

		// Make selection visible even though we are not (initially) focusable.
		textArea.getCaret().setSelectionVisible(true);

		// Set the foreground color.  Important because when rendering HTML,
		// default foreground becomes black, which may not match all LAF's
		// (e.g. Substance).
		Color fg = UIManager.getColor("Label.foreground");
		if (fg==null || (isNimbus && isDerivedColor(fg))) {
			fg = SystemColor.textText;
		}
		textArea.setForeground(fg);

		// Make it use the "tool tip" background color.
		textArea.setBackground(TipUtil.getToolTipBackground());

		// Force JEditorPane to use a certain font even in HTML.
		// All standard LookAndFeels, even Nimbus (!), define Label.font.
		Font font = UIManager.getFont("Label.font");
		if (font == null) { // Try to make a sensible default
			font = new Font("SansSerif", Font.PLAIN, 12);
		}
		HTMLDocument doc = (HTMLDocument) textArea.getDocument();
		doc.getStyleSheet().addRule(
				"body { font-family: " + font.getFamily() +
						"; font-size: " + font.getSize() + "pt" +
						"; color: " + getHexString(fg) + "; }");

	}


}