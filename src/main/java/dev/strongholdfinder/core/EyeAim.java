package dev.strongholdfinder.core;

import java.util.Comparator;
import java.util.List;

public final class EyeAim {
    /** A fresh eye hangs right above the thrower, where the direction to it means nothing. */
    private static final double MIN_HORIZONTAL = 0.5;

    private EyeAim() {
    }

    public static Aim compute(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double horizontal = Math.hypot(toX - fromX, toZ - fromZ);
        if (horizontal < MIN_HORIZONTAL) {
            return null;
        }
        double yaw = Geometry.yawTo(new Point(fromX, fromZ), new Point(toX, toZ));
        double pitch = -Math.toDegrees(Math.atan2(toY - fromY, horizontal));
        return new Aim(yaw, pitch);
    }

    public static EyeCandidate pick(List<EyeCandidate> eyes) {
        return eyes.stream().min(Comparator.comparingInt(EyeCandidate::age)).orElse(null);
    }
}
