package us.deathmarine.luyten;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import org.fife.ui.rsyntaxtextarea.LinkGenerator;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Languages;

public class OpenFile implements SyntaxConstants {

	public static final HashSet<String> WELL_KNOWN_TEXT_FILE_EXTENSIONS = new HashSet<>(
			Arrays.asList(".java", ".xml", ".rss", ".project", ".classpath", ".h", ".sql", ".js", ".php", ".php5",
					".phtml", ".html", ".htm", ".xhtm", ".xhtml", ".lua", ".bat", ".pl", ".sh", ".css", ".json", ".txt",
					".rb", ".make", ".mak", ".py", ".properties", ".prop"));

	// navigation links
	private TreeMap<Selection, String> selectionToUniqueStrTreeMap = new TreeMap<>();
	private Map<String, Boolean> isNavigableCache = new ConcurrentHashMap<>();
	private Map<String, String> readableLinksCache = new ConcurrentHashMap<>();

	private volatile boolean isContentValid = false;
	private volatile boolean isNavigationLinksValid = false;
	private volatile boolean isWaitForLinksCursor = false;
	private volatile Double lastScrollPercent = null;

	private LinkProvider linkProvider;
	private String initialNavigationLink;
	private boolean isFirstTimeRun = true;

	MainWindow mainWindow;
	RTextScrollPane scrollPane;
	Panel image_pane;
	RSyntaxTextArea textArea;
	String name;
	String path;

	// decompiler and type references (not needed for text files)
	private MetadataSystem metadataSystem;
	private DecompilerSettings settings;
	private DecompilationOptions decompilationOptions;
	private TypeDefinition type;

	public OpenFile(String name, String path, Theme theme, final MainWindow mainWindow) {
		this.name = name;
		this.path = path;
		this.mainWindow = mainWindow;
		textArea = new RSyntaxTextArea(25, 70);
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		textArea.setEditable(false);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);
		if (name.toLowerCase().endsWith(".class") || name.toLowerCase().endsWith(".java"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_JAVA);
		else if (name.toLowerCase().endsWith(".xml") || name.toLowerCase().endsWith(".rss")
				|| name.toLowerCase().endsWith(".project") || name.toLowerCase().endsWith(".classpath"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_XML);
		else if (name.toLowerCase().endsWith(".h"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_C);
		else if (name.toLowerCase().endsWith(".sql"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_SQL);
		else if (name.toLowerCase().endsWith(".js"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_JAVASCRIPT);
		else if (name.toLowerCase().endsWith(".php") || name.toLowerCase().endsWith(".php5")
				|| name.toLowerCase().endsWith(".phtml"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_PHP);
		else if (name.toLowerCase().endsWith(".html") || name.toLowerCase().endsWith(".htm")
				|| name.toLowerCase().endsWith(".xhtm") || name.toLowerCase().endsWith(".xhtml"))
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
		else if (name.toLowerCase().endsWith(".make") || name.toLowerCase().endsWith(".mak"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_MAKEFILE);
		else if (name.toLowerCase().endsWith(".py"))
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_PYTHON);
		else
			textArea.setSyntaxEditingStyle(SYNTAX_STYLE_PROPERTIES_FILE);
		scrollPane = new RTextScrollPane(textArea, true);

		scrollPane.setIconRowHeaderEnabled(true);
		textArea.setText("");

		// Edit RTextArea's PopupMenu
		JPopupMenu pop = textArea.getPopupMenu();
		pop.addSeparator();
		JMenuItem item = new JMenuItem("Font");
		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFontChooser fontChooser = new JFontChooser();
				fontChooser.setSelectedFont(textArea.getFont());
				fontChooser.setSelectedFontSize(textArea.getFont().getSize());
				int result = fontChooser.showDialog(mainWindow);
				if (result == JFontChooser.OK_OPTION)
					textArea.setFont(fontChooser.getSelectedFont());
			}
		});
		pop.add(item);
		textArea.setPopupMenu(pop);

		theme.apply(textArea);

		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		final JScrollBar verticalScrollbar = scrollPane.getVerticalScrollBar();
		if (verticalScrollbar != null) {
			verticalScrollbar.addAdjustmentListener(new AdjustmentListener() {
				@Override
				public void adjustmentValueChanged(AdjustmentEvent e) {
					String content = textArea.getText();
					if (content == null || content.length() == 0)
						return;
					int scrollValue = verticalScrollbar.getValue() - verticalScrollbar.getMinimum();
					int scrollMax = verticalScrollbar.getMaximum() - verticalScrollbar.getMinimum();
					if (scrollMax < 1 || scrollValue < 0 || scrollValue > scrollMax)
						return;
					lastScrollPercent = (((double) scrollValue) / ((double) scrollMax));
				}
			});
		}

		textArea.setHyperlinksEnabled(true);
		textArea.setLinkScanningMask(InputEvent.CTRL_DOWN_MASK);

		textArea.setLinkGenerator(new LinkGenerator() {
			@Override
			public LinkGeneratorResult isLinkAtOffset(RSyntaxTextArea textArea, final int offs) {
				final String uniqueStr = getUniqueStrForOffset(offs);
				final Integer selectionFrom = getSelectionFromForOffset(offs);
				if (uniqueStr != null && selectionFrom != null) {
					return new LinkGeneratorResult() {
						@Override
						public HyperlinkEvent execute() {
							if (isNavigationLinksValid)
								onNavigationClicked(uniqueStr);
							return null;
						}

						@Override
						public int getSourceOffset() {
							if (isNavigationLinksValid)
								return selectionFrom;
							return offs;
						}
					};
				}
				return null;
			}
		});

		/*
		 * Add Ctrl+Wheel Zoom for Text Size Removes all standard listeners and
		 * writes new listeners for wheelscroll movement.
		 */
		for (MouseWheelListener listeners : scrollPane.getMouseWheelListeners()) {
			scrollPane.removeMouseWheelListener(listeners);
		}
		;
		scrollPane.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {

				if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
					Font font = textArea.getFont();
					int size = font.getSize();
					if (e.getWheelRotation() > 0) {
						textArea.setFont(new Font(font.getName(), font.getStyle(), --size >= 8 ? --size : 8));
					} else {
						textArea.setFont(new Font(font.getName(), font.getStyle(), ++size));
					}
				} else {
					if (scrollPane.isWheelScrollingEnabled() && e.getWheelRotation() != 0) {
						JScrollBar toScroll = scrollPane.getVerticalScrollBar();
						int direction = e.getWheelRotation() < 0 ? -1 : 1;
						int orientation = SwingConstants.VERTICAL;
						if (toScroll == null || !toScroll.isVisible()) {
							toScroll = scrollPane.getHorizontalScrollBar();
							if (toScroll == null || !toScroll.isVisible()) {
								return;
							}
							orientation = SwingConstants.HORIZONTAL;
						}
						e.consume();

						if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
							JViewport vp = scrollPane.getViewport();
							if (vp == null) {
								return;
							}
							Component comp = vp.getView();
							int units = Math.abs(e.getUnitsToScroll());
							boolean limitScroll = Math.abs(e.getWheelRotation()) == 1;
							Object fastWheelScroll = toScroll.getClientProperty("JScrollBar.fastWheelScrolling");
							if (Boolean.TRUE == fastWheelScroll && comp instanceof Scrollable) {
								Scrollable scrollComp = (Scrollable) comp;
								Rectangle viewRect = vp.getViewRect();
								int startingX = viewRect.x;
								boolean leftToRight = comp.getComponentOrientation().isLeftToRight();
								int scrollMin = toScroll.getMinimum();
								int scrollMax = toScroll.getMaximum() - toScroll.getModel().getExtent();

								if (limitScroll) {
									int blockIncr = scrollComp.getScrollableBlockIncrement(viewRect, orientation,
											direction);
									if (direction < 0) {
										scrollMin = Math.max(scrollMin, toScroll.getValue() - blockIncr);
									} else {
										scrollMax = Math.min(scrollMax, toScroll.getValue() + blockIncr);
									}
								}

								for (int i = 0; i < units; i++) {
									int unitIncr = scrollComp.getScrollableUnitIncrement(viewRect, orientation,
											direction);
									if (orientation == SwingConstants.VERTICAL) {
										if (direction < 0) {
											viewRect.y -= unitIncr;
											if (viewRect.y <= scrollMin) {
												viewRect.y = scrollMin;
												break;
											}
										} else { // (direction > 0
											viewRect.y += unitIncr;
											if (viewRect.y >= scrollMax) {
												viewRect.y = scrollMax;
												break;
											}
										}
									} else {
										if ((leftToRight && direction < 0) || (!leftToRight && direction > 0)) {
											viewRect.x -= unitIncr;
											if (leftToRight) {
												if (viewRect.x < scrollMin) {
													viewRect.x = scrollMin;
													break;
												}
											}
										} else if ((leftToRight && direction > 0) || (!leftToRight && direction < 0)) {
											viewRect.x += unitIncr;
											if (leftToRight) {
												if (viewRect.x > scrollMax) {
													viewRect.x = scrollMax;
													break;
												}
											}
										} else {
											assert false : "Non-sensical ComponentOrientation / scroll direction";
										}
									}
								}
								if (orientation == SwingConstants.VERTICAL) {
									toScroll.setValue(viewRect.y);
								} else {
									if (leftToRight) {
										toScroll.setValue(viewRect.x);
									} else {
										int newPos = toScroll.getValue() - (viewRect.x - startingX);
										if (newPos < scrollMin) {
											newPos = scrollMin;
										} else if (newPos > scrollMax) {
											newPos = scrollMax;
										}
										toScroll.setValue(newPos);
									}
								}
							} else {
								int delta;
								int limit = -1;

								if (limitScroll) {
									if (direction < 0) {
										limit = toScroll.getValue() - toScroll.getBlockIncrement(direction);
									} else {
										limit = toScroll.getValue() + toScroll.getBlockIncrement(direction);
									}
								}

								for (int i = 0; i < units; i++) {
									if (direction > 0) {
										delta = toScroll.getUnitIncrement(direction);
									} else {
										delta = -toScroll.getUnitIncrement(direction);
									}
									int oldValue = toScroll.getValue();
									int newValue = oldValue + delta;
									if (delta > 0 && newValue < oldValue) {
										newValue = toScroll.getMaximum();
									} else if (delta < 0 && newValue > oldValue) {
										newValue = toScroll.getMinimum();
									}
									if (oldValue == newValue) {
										break;
									}
									if (limitScroll && i > 0) {
										assert limit != -1;
										if ((direction < 0 && newValue < limit)
												|| (direction > 0 && newValue > limit)) {
											break;
										}
									}
									toScroll.setValue(newValue);
								}

							}
						} else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
							int oldValue = toScroll.getValue();
							int blockIncrement = toScroll.getBlockIncrement(direction);
							int delta = blockIncrement * ((direction > 0) ? +1 : -1);
							int newValue = oldValue + delta;
							if (delta > 0 && newValue < oldValue) {
								newValue = toScroll.getMaximum();
							} else if (delta < 0 && newValue > oldValue) {
								newValue = toScroll.getMinimum();
							}
							toScroll.setValue(newValue);
						}
					}
				}
				e.consume();
			}
		});

		textArea.addMouseMotionListener(new MouseMotionAdapter() {
			private boolean isLinkLabelPrev = false;
			private String prevLinkText = null;

			@Override
			public synchronized void mouseMoved(MouseEvent e) {
				String linkText = null;
				boolean isLinkLabel = false;
				boolean isCtrlDown = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
				if (isCtrlDown) {
					linkText = createLinkLabel(e);
					isLinkLabel = linkText != null;
				}
				if (isCtrlDown && isWaitForLinksCursor) {
					textArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				} else if (textArea.getCursor().getType() == Cursor.WAIT_CURSOR) {
					textArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}

				JLabel label = OpenFile.this.mainWindow.getLabel();

				if (isLinkLabel && isLinkLabelPrev) {
					if (!linkText.equals(prevLinkText)) {
						setLinkLabel(label, linkText);
					}
				} else if (isLinkLabel && !isLinkLabelPrev) {
					setLinkLabel(label, linkText);

				} else if (!isLinkLabel && isLinkLabelPrev) {
					setLinkLabel(label, null);
				}
				isLinkLabelPrev = isLinkLabel;
				prevLinkText = linkText;
			}

			private void setLinkLabel(JLabel label, String text) {
				String current = label.getText();
				if (text == null && current != null)
					if (current.startsWith("Navigating:") || current.startsWith("Cannot navigate:"))
						return;
				label.setText(text != null ? text : "Complete");
			}

			private String createLinkLabel(MouseEvent e) {
				int offs = textArea.viewToModel(e.getPoint());
				if (isNavigationLinksValid) {
					return getLinkDescriptionForOffset(offs);
				}
				return null;
			}
		});
	}

	public void setContent(String content) {
		textArea.setText(content);
	}

	public void decompile() {
		this.invalidateContent();
		// synchronized: do not accept changes from menu while running
		synchronized (settings) {
			if (Languages.java().getName().equals(settings.getLanguage().getName())) {
				decompileWithNavigationLinks();
			} else {
				decompileWithoutLinks();
			}
		}
	}

	private void decompileWithoutLinks() {
		this.invalidateContent();
		isNavigationLinksValid = false;
		textArea.setHyperlinksEnabled(false);

		StringWriter stringwriter = new StringWriter();
		PlainTextOutput plainTextOutput = new PlainTextOutput(stringwriter);
		plainTextOutput.setUnicodeOutputEnabled(decompilationOptions.getSettings().isUnicodeOutputEnabled());
		settings.getLanguage().decompileType(type, plainTextOutput, decompilationOptions);
		setContentPreserveLastScrollPosition(stringwriter.toString());
		this.isContentValid = true;
	}

	private void decompileWithNavigationLinks() {
		this.invalidateContent();
		DecompilerLinkProvider newLinkProvider = new DecompilerLinkProvider();
		newLinkProvider.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
		newLinkProvider.setType(type);
		linkProvider = newLinkProvider;

		linkProvider.generateContent();
		setContentPreserveLastScrollPosition(linkProvider.getTextContent());
		this.isContentValid = true;
		enableLinks();
	}

	private void setContentPreserveLastScrollPosition(final String content) {
		final Double scrollPercent = lastScrollPercent;
		if (scrollPercent != null && initialNavigationLink == null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					textArea.setText(content);
					restoreScrollPosition(scrollPercent);
				}
			});
		} else {
			textArea.setText(content);
		}
	}

	private void restoreScrollPosition(final double position) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JScrollBar verticalScrollbar = scrollPane.getVerticalScrollBar();
				if (verticalScrollbar == null)
					return;
				int scrollMax = verticalScrollbar.getMaximum() - verticalScrollbar.getMinimum();
				long newScrollValue = Math.round(position * scrollMax) + verticalScrollbar.getMinimum();
				if (newScrollValue < verticalScrollbar.getMinimum())
					newScrollValue = verticalScrollbar.getMinimum();
				if (newScrollValue > verticalScrollbar.getMaximum())
					newScrollValue = verticalScrollbar.getMaximum();
				verticalScrollbar.setValue((int) newScrollValue);
			}
		});
	}

	private void enableLinks() {
		if (initialNavigationLink != null) {
			doEnableLinks();
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						isWaitForLinksCursor = true;
						doEnableLinks();
					} finally {
						isWaitForLinksCursor = false;
						resetCursor();
					}
				}
			}).start();
		}
	}

	private void resetCursor() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				textArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
	}

	private void doEnableLinks() {
		isNavigationLinksValid = false;
		linkProvider.processLinks();
		buildSelectionToUniqueStrTreeMap();
		clearLinksCache();
		isNavigationLinksValid = true;
		textArea.setHyperlinksEnabled(true);
		warmUpWithFirstLink();
	}

	private void warmUpWithFirstLink() {
		if (selectionToUniqueStrTreeMap.keySet().size() > 0) {
			Selection selection = selectionToUniqueStrTreeMap.keySet().iterator().next();
			getLinkDescriptionForOffset(selection.from);
		}
	}

	public void clearLinksCache() {
		try {
			isNavigableCache.clear();
			readableLinksCache.clear();
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void buildSelectionToUniqueStrTreeMap() {
		TreeMap<Selection, String> treeMap = new TreeMap<>();
		Map<String, Selection> definitionToSelectionMap = linkProvider.getDefinitionToSelectionMap();
		Map<String, Set<Selection>> referenceToSelectionsMap = linkProvider.getReferenceToSelectionsMap();

		for (String key : definitionToSelectionMap.keySet()) {
			Selection selection = definitionToSelectionMap.get(key);
			treeMap.put(selection, key);
		}
		for (String key : referenceToSelectionsMap.keySet()) {
			for (Selection selection : referenceToSelectionsMap.get(key)) {
				treeMap.put(selection, key);
			}
		}
		selectionToUniqueStrTreeMap = treeMap;
	}

	private Selection getSelectionForOffset(int offset) {
		if (isNavigationLinksValid) {
			Selection offsetSelection = new Selection(offset, offset);
			Selection floorSelection = selectionToUniqueStrTreeMap.floorKey(offsetSelection);
			if (floorSelection != null && floorSelection.from <= offset && floorSelection.to > offset) {
				return floorSelection;
			}
		}
		return null;
	}

	private String getUniqueStrForOffset(int offset) {
		Selection selection = getSelectionForOffset(offset);
		if (selection != null) {
			String uniqueStr = selectionToUniqueStrTreeMap.get(selection);
			if (this.isLinkNavigable(uniqueStr) && this.getLinkDescription(uniqueStr) != null) {
				return uniqueStr;
			}
		}
		return null;
	}

	private Integer getSelectionFromForOffset(int offset) {
		Selection selection = getSelectionForOffset(offset);
		if (selection != null) {
			return selection.from;
		}
		return null;
	}

	private String getLinkDescriptionForOffset(int offset) {
		String uniqueStr = getUniqueStrForOffset(offset);
		if (uniqueStr != null) {
			String description = this.getLinkDescription(uniqueStr);
			if (description != null) {
				return description;
			}
		}
		return null;
	}

	private boolean isLinkNavigable(String uniqueStr) {
		try {
			Boolean isNavigableCached = isNavigableCache.get(uniqueStr);
			if (isNavigableCached != null)
				return isNavigableCached;

			boolean isNavigable = linkProvider.isLinkNavigable(uniqueStr);
			isNavigableCache.put(uniqueStr, isNavigable);
			return isNavigable;
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
		return false;
	}

	private String getLinkDescription(String uniqueStr) {
		try {
			String descriptionCached = readableLinksCache.get(uniqueStr);
			if (descriptionCached != null)
				return descriptionCached;

			String description = linkProvider.getLinkDescription(uniqueStr);
			if (description != null && description.trim().length() > 0) {
				readableLinksCache.put(uniqueStr, description);
				return description;
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
		return null;
	}

	private void onNavigationClicked(String clickedReferenceUniqueStr) {
		if (isLocallyNavigable(clickedReferenceUniqueStr)) {
			onLocalNavigationRequest(clickedReferenceUniqueStr);
		} else if (linkProvider.isLinkNavigable(clickedReferenceUniqueStr)) {
			onOutboundNavigationRequest(clickedReferenceUniqueStr);
		} else {
			JLabel label = this.mainWindow.getLabel();
			if (label == null)
				return;
			String[] linkParts = clickedReferenceUniqueStr.split("\\|");
			if (linkParts.length <= 1) {
				label.setText("Cannot navigate: " + clickedReferenceUniqueStr);
				return;
			}
			String destinationTypeStr = linkParts[1];
			label.setText("Cannot navigate: " + destinationTypeStr.replaceAll("/", "."));
		}
	}

	private boolean isLocallyNavigable(String uniqueStr) {
		return linkProvider.getDefinitionToSelectionMap().keySet().contains(uniqueStr);
	}

	private void onLocalNavigationRequest(String uniqueStr) {
		try {
			Selection selection = linkProvider.getDefinitionToSelectionMap().get(uniqueStr);
			doLocalNavigation(selection);
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void doLocalNavigation(Selection selection) {
		try {
			textArea.requestFocusInWindow();
			if (selection != null) {
				textArea.setSelectionStart(selection.from);
				textArea.setSelectionEnd(selection.to);
				scrollToSelection(selection.from);
			} else {
				textArea.setSelectionStart(0);
				textArea.setSelectionEnd(0);
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void scrollToSelection(final int selectionBeginningOffset) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					int fullHeight = textArea.getBounds().height;
					int viewportHeight = textArea.getVisibleRect().height;
					int viewportLineCount = viewportHeight / textArea.getLineHeight();
					int selectionLineNum = textArea.getLineOfOffset(selectionBeginningOffset);
					int upperMarginToScroll = Math.round(viewportLineCount * 0.29f);
					int upperLineToSet = selectionLineNum - upperMarginToScroll;
					int currentUpperLine = textArea.getVisibleRect().y / textArea.getLineHeight();

					if (selectionLineNum <= currentUpperLine + 2
							|| selectionLineNum >= currentUpperLine + viewportLineCount - 4) {
						Rectangle rectToScroll = new Rectangle();
						rectToScroll.x = 0;
						rectToScroll.width = 1;
						rectToScroll.y = Math.max(upperLineToSet * textArea.getLineHeight(), 0);
						rectToScroll.height = Math.min(viewportHeight, fullHeight - rectToScroll.y);
						textArea.scrollRectToVisible(rectToScroll);
					}
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			}
		});
	}

	private void onOutboundNavigationRequest(String uniqueStr) {
		mainWindow.onNavigationRequest(uniqueStr);
	}

	public void setDecompilerReferences(MetadataSystem metadataSystem, DecompilerSettings settings,
			DecompilationOptions decompilationOptions) {
		this.metadataSystem = metadataSystem;
		this.settings = settings;
		this.decompilationOptions = decompilationOptions;
	}

	public TypeDefinition getType() {
		return type;
	}

	public void setType(TypeDefinition type) {
		this.type = type;
	}

	public boolean isContentValid() {
		return isContentValid;
	}

	public void invalidateContent() {
		try {
			this.setContent("");
		} finally {
			this.isContentValid = false;
			this.isNavigationLinksValid = false;
		}
	}

	public void resetScrollPosition() {
		lastScrollPercent = null;
	}

	public void setInitialNavigationLink(String initialNavigationLink) {
		this.initialNavigationLink = initialNavigationLink;
	}

	public void onAddedToScreen() {
		try {
			if (initialNavigationLink != null) {
				onLocalNavigationRequest(initialNavigationLink);
			} else if (isFirstTimeRun) {
				// warm up scrolling
				isFirstTimeRun = false;
				doLocalNavigation(new Selection(0, 0));
			}
		} finally {
			initialNavigationLink = null;
		}
	}

	/**
	 * sun.swing.CachedPainter holds on OpenFile for a while even after
	 * JTabbedPane.remove(component)
	 */
	public void close() {
		linkProvider = null;
		type = null;
		invalidateContent();
		clearLinksCache();
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
