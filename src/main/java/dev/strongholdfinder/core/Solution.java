package dev.strongholdfinder.core;

import java.util.List;

/** {@code mean}, {@code lsq} and {@code ring} are null while the rays give no point. */
public record Solution(
        List<Throw> on,
        List<Pair> pairs,
        List<Pair> used,
        boolean weakFallback,
        Point mean,
        double rms,
        double theory,
        double err,
        Lsq lsq,
        Throw last,
        double toLast,
        double yawNext,
        double origin,
        Ring ring,
        double minAngle) {
}
