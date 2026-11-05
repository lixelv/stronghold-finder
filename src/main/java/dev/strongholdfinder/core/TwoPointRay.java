package dev.strongholdfinder.core;

/** Starts at {@code from} and passes through {@code through}: an eye seen on two ticks. */
public record TwoPointRay(Point from, Point through) implements Ray {
    public TwoPointRay {
        double length = Math.hypot(through.x() - from.x(), through.z() - from.z());
        if (!(length > 0) || Double.isInfinite(length)) {
            throw new IllegalArgumentException("A ray needs two different points");
        }
    }

    @Override
    public Point origin() {
        return from;
    }

    @Override
    public Point direction() {
        double dx = through.x() - from.x();
        double dz = through.z() - from.z();
        double length = Math.hypot(dx, dz);
        return new Point(dx / length, dz / length);
    }
}
