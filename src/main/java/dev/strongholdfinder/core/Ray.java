package dev.strongholdfinder.core;

/** A ray on the map, whatever it was built from. */
public sealed interface Ray permits AngleRay, TwoPointRay {
    Point origin();

    /** Unit vector along the ray. */
    Point direction();

    default RayEquation equation() {
        return new RayEquation(origin(), direction());
    }

    /** Game yaw of the direction: 0 is south, 90 is west. */
    default double yaw() {
        Point direction = direction();
        return Geometry.normYaw(Math.toDegrees(Math.atan2(-direction.x(), direction.z())));
    }
}
