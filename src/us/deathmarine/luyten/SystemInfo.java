package us.deathmarine.luyten;

import java.util.Locale;

public class SystemInfo {
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_NAME_LOWER = OS_NAME.toLowerCase(Locale.US);

    public static boolean IS_MAC = OS_NAME_LOWER.startsWith("mac");
}
