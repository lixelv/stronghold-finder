package dev.strongholdfinder.core;

import java.util.Locale;

/**
 * Which dimensions eye rays make sense in.
 *
 * <p>Servers often run the game in a custom dimension instead of {@code minecraft:overworld}
 * (for example {@code minecraft:game_world} from world-reset plugins), so the check goes by name:
 * every dimension counts as an overworld except a Nether or an End, whose coordinates do not match
 * the overworld's. Copies such as {@code game_world_nether} and {@code world_the_end} are caught by suffix.
 */
public final class Dimensions {
    private Dimensions() {
    }

    /** {@code id} is a dimension id such as {@code minecraft:the_nether}; a missing namespace is fine. */
    public static boolean hasRays(String id) {
        String path = id.toLowerCase(Locale.ROOT);
        path = path.substring(path.indexOf(':') + 1);
        return !(path.equals("the_nether") || path.equals("the_end")
                || path.endsWith("_nether") || path.endsWith("_the_end"));
    }
}
