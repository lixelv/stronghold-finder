package dev.strongholdfinder.core;

/** Builds a ray from where an eye is on two neighboring ticks. */
public final class EyeRayCapture {
    /**
     * Measured on real eyes: up to here the step between ticks is within 0.05 degrees of the stronghold.
     * From tick 36 the eye overshoots its target and swings back, so a later step can point the wrong way.
     */
    public static final int LAST_TICK = 31;
    /** For its first ticks the eye stands still on the client. */
    private static final double MIN_STEP = 0.05;

    public enum Status { WAITING, DONE, LOST }

    private EyeSample first;
    private Ray ray;

    /** {@code sample} is null when the eye is gone. */
    public Status accept(EyeSample sample) {
        if (sample == null || sample.age() > LAST_TICK) {
            return Status.LOST;
        }
        if (first == null || Math.hypot(sample.x() - first.x(), sample.z() - first.z()) < MIN_STEP) {
            first = sample;
            return Status.WAITING;
        }
        ray = new TwoPointRay(new Point(first.x(), first.z()), new Point(sample.x(), sample.z()));
        return Status.DONE;
    }

    public Ray ray() {
        return ray;
    }
}
