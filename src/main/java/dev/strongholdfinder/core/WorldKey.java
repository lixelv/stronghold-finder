package dev.strongholdfinder.core;

public final class WorldKey {
    private static final String UNSAFE = "[^\\p{L}\\p{N}._ -]";

    private WorldKey() {
    }

    /** A safe file name; the hash keeps names apart when sanitising makes two of them equal. */
    public static String of(String raw) {
        String safe = raw.replaceAll(UNSAFE, "_").trim();
        if (safe.length() > 64) {
            safe = safe.substring(0, 64);
        }
        return safe + "-" + Integer.toHexString(raw.hashCode());
    }
}
