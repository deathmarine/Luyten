package com.modcrafting.luyten;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.JarTypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

/**
 * Performs Save and Save All
 */
public class FileSaver {

	private JProgressBar bar;
	private JLabel label;

	public FileSaver(JProgressBar bar, JLabel label) {
		this.bar = bar;
		this.label = label;
	}

	public void saveText(final String text, final File file) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try (FileWriter fw = new FileWriter(file);
						BufferedWriter bw = new BufferedWriter(fw);) {
					label.setText("Extracting: " + file.getName());
					bar.setVisible(true);
					bw.write(text);
					bw.flush();
					label.setText("Complete");
				} catch (Exception e1) {
					label.setText("Cannot save file: " + file.getName());
					e1.printStackTrace();
					JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
				} finally {
					bar.setVisible(false);
				}
			}
		}).start();
	}

	public void saveAllDecompiled(final File inFile, final File outFile) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					bar.setVisible(true);
					label.setText("Extracting: " + outFile.getName());
					String inFileName = inFile.getName().toLowerCase();

					if (inFileName.endsWith(".jar") || inFileName.endsWith(".zip")) {
						doSaveJarDecompiled(inFile, outFile);
					} else if (inFileName.endsWith(".class")) {
						doSaveClassDecompiled(inFile, outFile);
					} else {
						doSaveUnknownFile(inFile, outFile);
					}

					label.setText("Complete");
				} catch (Exception e1) {
					e1.printStackTrace();
					label.setText("Cannot save file: " + outFile.getName());
					JOptionPane.showMessageDialog(null, e1.toString(), "Error!", JOptionPane.ERROR_MESSAGE);
				} finally {
					bar.setVisible(false);
				}
			}
		}).start();
	}

	private void doSaveJarDecompiled(File inFile, File outFile) throws Exception {
		try (JarFile jfile = new JarFile(inFile);
				FileOutputStream dest = new FileOutputStream(outFile);
				BufferedOutputStream buffDest = new BufferedOutputStream(dest);
				ZipOutputStream out = new ZipOutputStream(buffDest);) {

			byte data[] = new byte[1024];
			DecompilerSettings settings = cloneSettings();
			LuytenTypeLoader typeLoader = new LuytenTypeLoader();
			MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
			ITypeLoader jarLoader = new JarTypeLoader(jfile);
			typeLoader.getTypeLoaders().add(jarLoader);

			DecompilationOptions decompilationOptions = new DecompilationOptions();
			decompilationOptions.setSettings(settings);
			decompilationOptions.setFullDecompilation(true);

			List<String> mass = null;
			JarEntryFilter jarEntryFilter = new JarEntryFilter(jfile);
			LuytenPreferences luytenPrefs = ConfigSaver.getLoadedInstance().getLuytenPreferences();
			if (luytenPrefs.isFilterOutInnerClassEntries()) {
				mass = jarEntryFilter.getEntriesWithoutInnerClasses();
			} else {
				mass = jarEntryFilter.getAllEntriesFromJar();
			}

			Enumeration<JarEntry> ent = jfile.entries();
			while (ent.hasMoreElements()) {
				JarEntry entry = ent.nextElement();
				if (!mass.contains(entry.getName()))
					continue;
				label.setText("Extracting: " + entry.getName());
				bar.setVisible(true);
				if (entry.getName().endsWith(".class")) {
					JarEntry etn = new JarEntry(entry.getName().replace(".class", ".java"));
					label.setText("Extracting: " + etn.getName());
					out.putNextEntry(etn);
					try {
						String internalName = StringUtilities.removeRight(entry.getName(), ".class");
						TypeReference type = metadataSystem.lookupType(internalName);
						TypeDefinition resolvedType = null;
						if ((type == null) || ((resolvedType = type.resolve()) == null)) {
							throw new Exception("Unable to resolve type.");
						}
						Writer writer = new OutputStreamWriter(out);
						settings.getLanguage().decompileType(resolvedType,
								new PlainTextOutput(writer), decompilationOptions);
						writer.flush();
					} finally {
						out.closeEntry();
					}
				} else {
					try {
						JarEntry etn = new JarEntry(entry.getName());
						out.putNextEntry(etn);
						try {
							InputStream in = jfile.getInputStream(entry);
							if (in != null) {
								try {
									int count;
									while ((count = in.read(data, 0, 1024)) != -1) {
										out.write(data, 0, count);
									}
								} finally {
									in.close();
								}
							}
						} finally {
							out.closeEntry();
						}
					} catch (ZipException ze) {
						// some jar-s contain duplicate pom.xml entries: ignore it
						if (!ze.getMessage().contains("duplicate")) {
							throw ze;
						}
					}
				}
			}

		}
	}

	private void doSaveClassDecompiled(File inFile, File outFile) throws Exception {
		DecompilerSettings settings = cloneSettings();
		LuytenTypeLoader typeLoader = new LuytenTypeLoader();
		MetadataSystem metadataSystem = new MetadataSystem(typeLoader);
		TypeReference type = metadataSystem.lookupType(inFile.getCanonicalPath());

		DecompilationOptions decompilationOptions = new DecompilationOptions();
		decompilationOptions.setSettings(settings);
		decompilationOptions.setFullDecompilation(true);

		TypeDefinition resolvedType = null;
		if (type == null || ((resolvedType = type.resolve()) == null)) {
			throw new Exception("Unable to resolve type.");
		}
		StringWriter stringwriter = new StringWriter();
		settings.getLanguage().decompileType(resolvedType,
				new PlainTextOutput(stringwriter), decompilationOptions);
		String decompiledSource = stringwriter.toString();

		try (FileWriter fw = new FileWriter(outFile);
				BufferedWriter bw = new BufferedWriter(fw);) {
			bw.write(decompiledSource);
			bw.flush();
		}
	}

	private void doSaveUnknownFile(File inFile, File outFile) throws Exception {
		try (FileInputStream in = new FileInputStream(inFile);
				FileOutputStream out = new FileOutputStream(outFile);) {

			byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				out.write(data, 0, count);
			}
		}
	}

	private DecompilerSettings cloneSettings() {
		DecompilerSettings settings = ConfigSaver.getLoadedInstance().getDecompilerSettings();
		DecompilerSettings newSettings = new DecompilerSettings();
		if (newSettings.getFormattingOptions() == null) {
			newSettings.setFormattingOptions(JavaFormattingOptions.createDefault());
		}
		// synchronized: against main menu changes
		synchronized (settings) {
			newSettings.setExcludeNestedTypes(settings.getExcludeNestedTypes());
			newSettings.setFlattenSwitchBlocks(settings.getFlattenSwitchBlocks());
			newSettings.setForceExplicitImports(settings.getForceExplicitImports());
			newSettings.setForceExplicitTypeArguments(settings.getForceExplicitTypeArguments());
			newSettings.setOutputFileHeaderText(settings.getOutputFileHeaderText());
			newSettings.setLanguage(settings.getLanguage());
			newSettings.setShowSyntheticMembers(settings.getShowSyntheticMembers());
			newSettings.setAlwaysGenerateExceptionVariableForCatchBlocks(settings
					.getAlwaysGenerateExceptionVariableForCatchBlocks());
			newSettings.setOutputDirectory(settings.getOutputDirectory());
			newSettings.setRetainRedundantCasts(settings.getRetainRedundantCasts());
			newSettings.setIncludeErrorDiagnostics(settings.getIncludeErrorDiagnostics());
			newSettings.setIncludeLineNumbersInBytecode(settings.getIncludeLineNumbersInBytecode());
			newSettings.setRetainPointlessSwitches(settings.getRetainPointlessSwitches());
			newSettings.setUnicodeOutputEnabled(settings.isUnicodeOutputEnabled());
			newSettings.setMergeVariables(settings.getMergeVariables());
			newSettings.setShowDebugLineNumbers(settings.getShowDebugLineNumbers());
		}
		return newSettings;
	}
}
