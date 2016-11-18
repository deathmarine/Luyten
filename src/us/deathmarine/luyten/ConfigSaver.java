package us.deathmarine.luyten;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
	private static final String UNICODE_REPLACE_ENABLED_ID = "unicodeReplaceEnabled";
	private static final String LANGUAGE_NAME_ID = "languageName";

	private static final String MAIN_WINDOW_ID_PREFIX = "main";
	private static final String FIND_WINDOW_ID_PREFIX = "find";
	private static final String WINDOW_IS_FULL_SCREEN_ID = "WindowIsFullScreen";
	private static final String WINDOW_WIDTH_ID = "WindowWidth";
	private static final String WINDOW_HEIGHT_ID = "WindowHeight";
	private static final String WINDOW_X_ID = "WindowX";
	private static final String WINDOW_Y_ID = "WindowY";

	private DecompilerSettings decompilerSettings;
	private WindowPosition mainWindowPosition;
	private WindowPosition findWindowPosition;
	private LuytenPreferences luytenPreferences;

	private static ConfigSaver theLoadedInstance;

	/**
	 * Do not instantiate, get the loaded instance
	 */
	private ConfigSaver() {
	}

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
		if (decompilerSettings.getJavaFormattingOptions() == null) {
			decompilerSettings.setJavaFormattingOptions(JavaFormattingOptions.createDefault());
		}
		luytenPreferences = new LuytenPreferences();
		mainWindowPosition = new WindowPosition();
		findWindowPosition = new WindowPosition();
		try {
			Preferences prefs = Preferences.userNodeForPackage(ConfigSaver.class);
			if (!prefs.get(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName())
					.equals(decompilerSettings.getLanguage().getName()))
				prefs.put(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName());

			decompilerSettings.setFlattenSwitchBlocks(
					prefs.getBoolean(FLATTEN_SWITCH_BLOCKS_ID, decompilerSettings.getFlattenSwitchBlocks()));
			decompilerSettings.setForceExplicitImports(
					prefs.getBoolean(FORCE_EXPLICIT_IMPORTS_ID, decompilerSettings.getForceExplicitImports()));
			decompilerSettings.setShowSyntheticMembers(
					prefs.getBoolean(SHOW_SYNTHETIC_MEMBERS_ID, decompilerSettings.getShowSyntheticMembers()));
			decompilerSettings.setExcludeNestedTypes(
					prefs.getBoolean(EXCLUDE_NESTED_TYPES_ID, decompilerSettings.getExcludeNestedTypes()));
			decompilerSettings.setForceExplicitTypeArguments(prefs.getBoolean(FORCE_EXPLICIT_TYPE_ARGUMENTS_ID,
					decompilerSettings.getForceExplicitTypeArguments()));
			decompilerSettings.setRetainRedundantCasts(
					prefs.getBoolean(RETAIN_REDUNDANT_CASTS_ID, decompilerSettings.getRetainRedundantCasts()));
			decompilerSettings.setIncludeErrorDiagnostics(
					prefs.getBoolean(INCLUDE_ERROR_DIAGNOSTICS_ID, decompilerSettings.getIncludeErrorDiagnostics()));
			decompilerSettings.setLanguage(
					findLanguageByName(prefs.get(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName())));
			decompilerSettings.setUnicodeOutputEnabled(prefs.getBoolean(UNICODE_REPLACE_ENABLED_ID, false));

			mainWindowPosition = loadWindowPosition(prefs, MAIN_WINDOW_ID_PREFIX);
			findWindowPosition = loadWindowPosition(prefs, FIND_WINDOW_ID_PREFIX);
			luytenPreferences = loadLuytenPreferences(prefs);
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private WindowPosition loadWindowPosition(Preferences prefs, String windowIdPrefix) {
		WindowPosition windowPosition = new WindowPosition();
		windowPosition.setFullScreen(prefs.getBoolean(windowIdPrefix + WINDOW_IS_FULL_SCREEN_ID, false));
		windowPosition.setWindowWidth(prefs.getInt(windowIdPrefix + WINDOW_WIDTH_ID, 0));
		windowPosition.setWindowHeight(prefs.getInt(windowIdPrefix + WINDOW_HEIGHT_ID, 0));
		windowPosition.setWindowX(prefs.getInt(windowIdPrefix + WINDOW_X_ID, 0));
		windowPosition.setWindowY(prefs.getInt(windowIdPrefix + WINDOW_Y_ID, 0));
		return windowPosition;
	}

	// load preferences by their java variable names
	private LuytenPreferences loadLuytenPreferences(Preferences prefs) throws Exception {
		LuytenPreferences newLuytenPrefs = new LuytenPreferences();
		for (Field field : LuytenPreferences.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			field.setAccessible(true);
			String prefId = field.getName();
			Object defaultVal = field.get(newLuytenPrefs);

			if (field.getType() == String.class) {
				String defaultStr = (String) (defaultVal == null ? "" : defaultVal);
				field.set(newLuytenPrefs, prefs.get(prefId, defaultStr));

			} else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
				Boolean defaultBool = (Boolean) (defaultVal == null ? new Boolean(false) : defaultVal);
				field.setBoolean(newLuytenPrefs, prefs.getBoolean(prefId, defaultBool));

			} else if (field.getType() == Integer.class || field.getType() == int.class) {
				Integer defaultInt = (Integer) (defaultVal == null ? new Integer(0) : defaultVal);
				field.setInt(newLuytenPrefs, prefs.getInt(prefId, defaultInt));
			}
		}
		return newLuytenPrefs;
	}

	public void saveConfig() {
		// Registry path on Windows Xp:
		// HKEY_CURRENT_USER/Software/JavaSoft/Prefs/us/deathmarine/luyten
		try {
			Preferences prefs = Preferences.userNodeForPackage(ConfigSaver.class);

			prefs.putBoolean(FLATTEN_SWITCH_BLOCKS_ID, decompilerSettings.getFlattenSwitchBlocks());
			prefs.putBoolean(FORCE_EXPLICIT_IMPORTS_ID, decompilerSettings.getForceExplicitImports());
			prefs.putBoolean(SHOW_SYNTHETIC_MEMBERS_ID, decompilerSettings.getShowSyntheticMembers());
			prefs.putBoolean(EXCLUDE_NESTED_TYPES_ID, decompilerSettings.getExcludeNestedTypes());
			prefs.putBoolean(FORCE_EXPLICIT_TYPE_ARGUMENTS_ID, decompilerSettings.getForceExplicitTypeArguments());
			prefs.putBoolean(RETAIN_REDUNDANT_CASTS_ID, decompilerSettings.getRetainRedundantCasts());
			prefs.putBoolean(INCLUDE_ERROR_DIAGNOSTICS_ID, decompilerSettings.getIncludeErrorDiagnostics());
			prefs.putBoolean(UNICODE_REPLACE_ENABLED_ID, decompilerSettings.isUnicodeOutputEnabled());
			prefs.put(LANGUAGE_NAME_ID, decompilerSettings.getLanguage().getName());

			saveWindowPosition(prefs, MAIN_WINDOW_ID_PREFIX, mainWindowPosition);
			saveWindowPosition(prefs, FIND_WINDOW_ID_PREFIX, findWindowPosition);
			saveLuytenPreferences(prefs);
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void saveWindowPosition(Preferences prefs, String windowIdPrefix, WindowPosition windowPosition) {
		prefs.putBoolean(windowIdPrefix + WINDOW_IS_FULL_SCREEN_ID, windowPosition.isFullScreen());
		prefs.putInt(windowIdPrefix + WINDOW_WIDTH_ID, windowPosition.getWindowWidth());
		prefs.putInt(windowIdPrefix + WINDOW_HEIGHT_ID, windowPosition.getWindowHeight());
		prefs.putInt(windowIdPrefix + WINDOW_X_ID, windowPosition.getWindowX());
		prefs.putInt(windowIdPrefix + WINDOW_Y_ID, windowPosition.getWindowY());
	}

	// save preferences by their java variable names
	private void saveLuytenPreferences(Preferences prefs) throws Exception {
		for (Field field : LuytenPreferences.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			field.setAccessible(true);
			String prefId = field.getName();
			Object value = field.get(luytenPreferences);

			if (field.getType() == String.class) {
				prefs.put(prefId, (String) (value == null ? "" : value));

			} else if (field.getType() == Boolean.class || field.getType() == boolean.class) {
				prefs.putBoolean(prefId, (Boolean) (value == null ? new Boolean(false) : value));

			} else if (field.getType() == Integer.class || field.getType() == int.class) {
				prefs.putInt(prefId, (Integer) (value == null ? new Integer(0) : value));
			}
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

	public WindowPosition getMainWindowPosition() {
		return mainWindowPosition;
	}

	public WindowPosition getFindWindowPosition() {
		return findWindowPosition;
	}

	public LuytenPreferences getLuytenPreferences() {
		return luytenPreferences;
	}
}
