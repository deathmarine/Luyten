package com.modcrafting.luyten;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

public class FindBox extends JDialog {
	private static final long serialVersionUID = -4125409760166690462L;

	private JCheckBox mcase;
	private JCheckBox regex;
	private JCheckBox wholew;
	private JCheckBox reverse;
	private JButton findButton;
	private JTextField textField;
	private MainWindow mainWindow;

	public void showFindBox() {
		this.setVisible(true);
		this.textField.requestFocus();
	}

	public void hideFindBox() {
		this.setVisible(false);
	}

	public FindBox(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.setHideOnEscapeButton();

		JLabel label = new JLabel("Find What:");
		textField = new JTextField();

		RSyntaxTextArea pane = mainWindow.getModel().getCurrentTextArea();
		if (pane != null) {
			textField.setText(pane.getSelectedText());
		}
		mcase = new JCheckBox("Match Case");
		regex = new JCheckBox("Regex");
		wholew = new JCheckBox("Whole Words");
		reverse = new JCheckBox("Search Backwards");

		findButton = new JButton("Find");
		findButton.addActionListener(new FindButton());
		this.getRootPane().setDefaultButton(findButton);

		mcase.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		regex.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		wholew.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		reverse.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final Dimension center = new Dimension((int) (screenSize.width * 0.35),
				Math.min((int) (screenSize.height * 0.20), 200));
		final int x = (int) (center.width * 0.2);
		final int y = (int) (center.height * 0.2);
		this.setBounds(x, y, center.width, center.height);
		this.setResizable(false);

		GroupLayout layout = new GroupLayout(getRootPane());
		getRootPane().setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(label)
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(textField)
						.addGroup(layout.createSequentialGroup()
								.addGroup(layout.createParallelGroup(Alignment.LEADING)
										.addComponent(mcase)
										.addComponent(wholew))
								.addGroup(layout.createParallelGroup(Alignment.LEADING)
										.addComponent(regex)
										.addComponent(reverse))))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(findButton))
				);

		layout.linkSize(SwingConstants.HORIZONTAL, findButton);
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
						.addComponent(label)
						.addComponent(textField)
						.addComponent(findButton))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addGroup(layout.createSequentialGroup()
								.addGroup(layout.createParallelGroup(Alignment.BASELINE)
										.addComponent(mcase)
										.addComponent(regex))
								.addGroup(layout.createParallelGroup(Alignment.BASELINE)
										.addComponent(wholew)
										.addComponent(reverse))))
				);

		this.adjustWindowPositionBySavedState();
		this.setSaveWindowPositionOnClosing();

		this.setName("Find");
		this.setTitle("Find");
		this.setVisible(true);
	}

	private class FindButton extends AbstractAction {
		private static final long serialVersionUID = 75954129199541874L;

		@Override
		public void actionPerformed(ActionEvent event) {
			if (textField.getText().length() == 0)
				return;

			RSyntaxTextArea pane = mainWindow.getModel().getCurrentTextArea();
			if (pane == null)
				return;

			SearchContext context = new SearchContext();
			context.setSearchFor(textField.getText());
			context.setMatchCase(mcase.isSelected());
			context.setRegularExpression(regex.isSelected());
			context.setSearchForward(!reverse.isSelected());
			context.setWholeWord(wholew.isSelected());

			if (SearchEngine.find(pane, context) == null) {
				pane.setSelectionStart(0);
				pane.setSelectionEnd(0);
			}
		}

	}

	private void setHideOnEscapeButton() {
		Action escapeAction = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				FindBox.this.setVisible(false);
			}
		};

		KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
		this.getRootPane().getActionMap().put("ESCAPE", escapeAction);
	}

	private void adjustWindowPositionBySavedState() {
		WindowPosition windowPosition = ConfigSaver.getLoadedInstance().getFindWindowPosition();

		if (windowPosition.isSavedWindowPositionValid()) {
			this.setLocation(windowPosition.getWindowX(), windowPosition.getWindowY());
		}
	}

	private void setSaveWindowPositionOnClosing() {
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDeactivated(WindowEvent e) {
				WindowPosition windowPosition = ConfigSaver.getLoadedInstance().getFindWindowPosition();
				windowPosition.readPositionFromDialog(FindBox.this);
			}
		});
	}
}
