package dev.strongholdfinder.core;

public record Throw(int id, Ray ray, boolean on) {
    public Throw(int id, double x, double z, double yaw, boolean on) {
        this(id, new AngleRay(new Point(x, z), yaw), on);
    }

    public double x() {
        return ray.origin().x();
    }

    public double z() {
        return ray.origin().z();
    }

    public double yaw() {
        return ray.yaw();
    }

    public Throw withOn(boolean value) {
        return new Throw(id, ray, value);
    }
}
