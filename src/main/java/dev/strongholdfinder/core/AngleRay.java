package dev.strongholdfinder.core;

/** A point and a game yaw: a look direction or typed numbers. */
public record AngleRay(Point origin, double yaw) implements Ray {
    public AngleRay {
        if (!Double.isFinite(yaw)) {
            throw new IllegalArgumentException("A ray needs a real angle");
        }
        yaw = Geometry.normYaw(yaw);
    }

    @Override
    public Point direction() {
        return Geometry.dir(yaw);
    }
}
