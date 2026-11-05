package dev.strongholdfinder.core;

import java.math.BigDecimal;

public final class Geometry {
    /** Each row is {inner radius, outer radius, stronghold count}. */
    public static final int[][] RINGS = {
        {1280, 2816, 3},
        {4352, 5888, 6},
        {7424, 8960, 10},
        {10496, 12032, 15},
        {13568, 15104, 21},
        {16640, 18176, 28},
        {19712, 21248, 36},
        {22784, 24320, 9},
    };

    private Geometry() {
    }

    public static double normYaw(double yaw) {
        if ((yaw > -180 && yaw <= 180) || !Double.isFinite(yaw)) {
            return yaw;
        }
        // In decimal, so wrapping 312.7 gives exactly -47.3: nothing is rounded and no float noise appears.
        BigDecimal turn = BigDecimal.valueOf(360);
        BigDecimal half = BigDecimal.valueOf(180);
        BigDecimal shifted = BigDecimal.valueOf(yaw).add(half).remainder(turn);
        BigDecimal wrapped = (shifted.signum() < 0 ? shifted.add(turn) : shifted).subtract(half);
        return wrapped.compareTo(half.negate()) == 0 ? 180 : wrapped.doubleValue();
    }

    public static Point dir(double yaw) {
        double radians = Math.toRadians(yaw);
        return new Point(-Math.sin(radians), Math.cos(radians));
    }

    public static double yawTo(Point from, Point to) {
        return normYaw(Math.toDegrees(Math.atan2(-(to.x() - from.x()), to.z() - from.z())));
    }

    public static Compass compass(double yaw) {
        int index = (int) Math.round(normYaw(yaw) / 45);
        return Compass.values()[(index % 8 + 8) % 8];
    }

    public static Ring ringOf(double distance, double tolerance) {
        for (int i = 0; i < RINGS.length; i++) {
            if (distance >= RINGS[i][0] && distance <= RINGS[i][1]) {
                return new Ring(i + 1, true, 0);
            }
        }
        for (int i = 0; i < RINGS.length; i++) {
            if (distance >= RINGS[i][0] - tolerance && distance <= RINGS[i][1] + tolerance) {
                return new Ring(i + 1, true, 0);
            }
        }
        int best = 0;
        double gap = Double.MAX_VALUE;
        for (int i = 0; i < RINGS.length; i++) {
            double g = distance < RINGS[i][0] ? RINGS[i][0] - distance : distance - RINGS[i][1];
            if (g < gap) {
                gap = g;
                best = i;
            }
        }
        return new Ring(best + 1, false, gap);
    }
}
