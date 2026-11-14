package dev.strongholdfinder;

import dev.strongholdfinder.core.Throw;

public final class RayColors {
    public static final int MUTED = 0xFF888888;
    /** Crossings outside every stronghold ring. They still count toward the mean. */
    public static final int OUT_OF_RING = 0xFF996666;
    /** The mean when it is outside every stronghold ring. */
    public static final int OUT_OF_RING_MEAN = 0xFFFF5555;

    private static final int[] COLORS = {
        0xFF55AAFF, 0xFFFF55AA, 0xFFFFAA00, 0xFFAA88FF, 0xFF55FFAA, 0xFFFF5555, 0xFFFF55FF, 0xFF00AA66,
    };

    private RayColors() {
    }

    /**
     * A ray has its own color only when it is on and its shown number is within the palette;
     * other rays are drawn muted and dashed. {@code number} is the shown number from {@code RayNumbers}.
     */
    public static boolean hasOwnColor(Throw source, int number) {
        return source.on() && number >= 1 && number <= COLORS.length;
    }

    public static int of(Throw source, int number) {
        return hasOwnColor(source, number) ? COLORS[number - 1] : MUTED;
    }
}
