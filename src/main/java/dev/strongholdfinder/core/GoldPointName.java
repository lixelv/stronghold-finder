package dev.strongholdfinder.core;

/** The waypoint is found again by its name, so both languages count as ours. */
public final class GoldPointName {
    private static final String ENGLISH = "Stronghold";
    private static final String RUSSIAN = "Крепость";

    private GoldPointName() {
    }

    public static String of(boolean russian) {
        return russian ? RUSSIAN : ENGLISH;
    }

    public static boolean isOurs(String name) {
        return name != null && (name.startsWith(ENGLISH) || name.startsWith(RUSSIAN));
    }
}
