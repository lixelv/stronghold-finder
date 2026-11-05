package dev.strongholdfinder.core;

/** {@code P(t) = origin + t * direction} for {@code t >= 0}, where {@code t} is in blocks. */
public record RayEquation(Point origin, Point direction) {
    public Point at(double t) {
        return new Point(origin.x() + t * direction.x(), origin.z() + t * direction.z());
    }
}
