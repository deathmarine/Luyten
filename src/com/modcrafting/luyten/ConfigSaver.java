package com.modcrafting.luyten;

import java.util.prefs.Preferences;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.Language;
import com.strobel.decompiler.languages.Languages;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;

public class ConfigSaver {

	private static final String FLATTEN_SWITCH_BLOCKS_ID = "flattenSwitchBlocks";
	private static final String FORCE_EXPLICIT_IMPORTS_ID = "forceExplicitImports";
	private static final String SHOW_SYNTHETIC_MEMBERS_ID = "showSyntheticMembers";
	private static final String EXCLUDE_NESTED_TYPES_ID = "excludeNestedTypes";
	private static final String FORCE_EXPLICIT_TYPE_ARGUMENTS_ID = "forceExplicitTypeArguments";
	private static final String RETAIN_REDUNDANT_CASTS_ID = "retainRedundantCasts";
	private static final String INCLUDE_ERROR_DIAGNOSTICS_ID = "includeErrorDiagnostics";
	private static final String LANGUAGE_NAME_ID = "languageName";

	private DecompilerSettings decompilerSettings;

	private static ConfigSaver theLoadedInstance;

	/**
	 * Do not instantiate, get the loaded instance
	 */
	private ConfigSaver() {}

	public static ConfigSaver getLoadedInstance() {
		if (theLoadedInstance == null) {
			synchronized (ConfigSaver.class) {
				if (theLoadedInstance == null) {
					theLoadedInstance = new ConfigSaver();
					theLoadedInstance.loadConfig();
				}
			}
		}
		return theLoadedInstance;
	}

	/**
	 * Do not load, get the loaded instance
	 */
	private void loadConfig() {
		decompilerSettings = new DecompilerSettings();
		if (decompilerSettings.getFormattingOptions() == null) {
			decompilerSettings.setFormattingOptions(JavaFormattingOptions.createDefault());
		}
		try {
			Preferences prefs = Preferences.userNodeForPackage(ConfigSaver.class);

			decompilerSettings.setFlattenSwitchBlocks(prefs.getBoolean(FLATTEN_SWITCH_BLOCKS_ID,
					decompilerSettings.getFlattenSwitchBlocks()));
			decompilerSettings.setForceExplicitImports(prefs.getBoolean(FORCE_EXPLICIT_IMPORTS_ID,
					decompilerSettings.getForceExplicitImports()));
			decompilerSettings.setShowSyntheticMembers(prefs.getBoolean(SHOW_SYNTHETIC_MEMBERS_ID,
					decompilerSettings.getShowSyntheticMembers()));
			decompilerSettings.setExcludeNestedTypes(prefs.getBoolean(EXCLUDE_NESTED_TYPES_ID,
					decompilerSettings.getExcludeNestedTypes()));
			decompilerSettings.setForceExplicitTypeArguments(prefs.getBoolean(FORCE_EXPLICIT_TYPE_ARGUMENTS_ID,
					decompilerSettings.getForceExplicitTypeArguments()));
			decompilerSettings.setRetainRedundantCasts(prefs.getBoolean(RETAIN_REDUNDANT_CASTS_ID,
					decompilerSettings.getRetainRedundantCasts()));
			decompilerSettings.setIncludeErrorDiagnostics(prefs.getBoolean(INCLUDE_ERROR_DIAGNOSTICS_ID,
					decompilerSettings.getIncludeErrorDiagnostics()));
			decompilerSettings.setLanguage(findLanguageByName(prefs.get(LANGUAGE_NAME_ID,
					decompilerSettings.getLanguage().getName())));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveConfig() {
		// Registry path on Windows Xp:
		// HKEY_CURRENT_USER\Software\JavaSoft\Prefs\com\modcrafting\luyten
		try {
			Preferences prefs = Preferences.userNodeForPackage(ConfigSaver.class);

			prefs.putBoolean(FLATTEN_SWITCH_BLOCKS_ID, decompilerSettings.getFlattenSwitchBlocks());
			prefs.putBoolean(FORCE_EXPLICIT_IMPORTS_ID, decompilerSettings.getForceExplicitImports());
			prefs.putBoolean(SHOW_SYNTHETIC_MEMBERS_ID, decompilerSettings.getShowSyntheticMembers());
			prefs.putBoolean(EXCLUDE_NESTED_TYPES_ID, decompilerSettings.getExcludeNestedTypes());
			prefs.putBoolean(FORCE_EXPLICIT_TYPE_ARGUMENTS_ID, decompilerSettings.getForceExplicitTypeArguments());
			prefs.putBoolean(RETAIN_REDUNDANT_CASTS_ID, decompilerSettings.getRetainRedundantCasts());
			prefs.putBoolean(INCLUDE_ERROR_DIAGNOSTICS_ID, decompilerSettings.getIncludeErrorDiagnostics());
			prefs.put(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Language findLanguageByName(String languageName) {
		if (languageName != null) {

			if (languageName.equals(Languages.java().getName())) {
				return Languages.java();
			} else if (languageName.equals(Languages.bytecode().getName())) {
				return Languages.bytecode();
			} else if (languageName.equals(Languages.bytecodeAst().getName())) {
				return Languages.bytecodeAst();
			}

			for (Language language : Languages.debug()) {
				if (languageName.equals(language.getName())) {
					return language;
				}
			}
		}
		return Languages.java();
	}

	public DecompilerSettings getDecompilerSettings() {
		return decompilerSettings;
	}
}
