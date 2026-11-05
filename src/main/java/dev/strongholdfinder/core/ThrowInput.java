package dev.strongholdfinder.core;

public record ThrowInput(double x, double z, double yaw) {
    public Ray toRay() {
        return new AngleRay(new Point(x, z), yaw);
    }
}
