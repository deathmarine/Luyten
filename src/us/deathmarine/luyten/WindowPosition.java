package us.deathmarine.luyten;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JDialog;
import javax.swing.JFrame;

public class WindowPosition {

	private boolean isFullScreen;
	private int windowWidth;
	private int windowHeight;
	private int windowX;
	private int windowY;

	public void readPositionFromWindow(JFrame window) {
		isFullScreen = (window.getExtendedState() == JFrame.MAXIMIZED_BOTH);
		if (!isFullScreen) {
			this.readPositionFromComponent(window);
		}
	}

	public void readPositionFromDialog(JDialog dialog) {
		this.readPositionFromComponent(dialog);
	}

	private void readPositionFromComponent(Component component) {
		isFullScreen = false;
		windowWidth = component.getWidth();
		windowHeight = component.getHeight();
		windowX = component.getX();
		windowY = component.getY();
	}

	public boolean isSavedWindowPositionValid() {
		if (isFullScreen) {
			return true;
		}
		if (windowWidth < 100 || windowHeight < 100) {
			return false;
		}
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (windowWidth > screenSize.width + 50 || windowHeight > screenSize.height + 50) {
			return false;
		}
		if (windowY < -20 || windowY > screenSize.height - 50 || windowX < 50 - windowWidth
				|| windowX > screenSize.width - 50) {
			return false;
		}
		return true;
	}

	public boolean isFullScreen() {
		return isFullScreen;
	}

	public void setFullScreen(boolean isFullScreen) {
		this.isFullScreen = isFullScreen;
	}

	public int getWindowWidth() {
		return windowWidth;
	}

	public void setWindowWidth(int windowWidth) {
		this.windowWidth = windowWidth;
	}

	public int getWindowHeight() {
		return windowHeight;
	}

	public void setWindowHeight(int windowHeight) {
		this.windowHeight = windowHeight;
	}

	public int getWindowX() {
		return windowX;
	}

	public void setWindowX(int windowX) {
		this.windowX = windowX;
	}

	public int getWindowY() {
		return windowY;
	}

	public void setWindowY(int windowY) {
		this.windowY = windowY;
	}
}
