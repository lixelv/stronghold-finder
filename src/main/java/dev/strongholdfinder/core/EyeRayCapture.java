package dev.strongholdfinder.core;

/**
 * Builds a ray from two points of one eye's flight: where it was when first seen and where it is as late as possible.
 * A longer stretch gives a better angle. On real eyes neighboring ticks were up to 0.05 degrees off the stronghold,
 * while the whole flight was within 0.001 degrees.
 */
public final class EyeRayCapture {
    /** From tick 36 the eye overshoots its target and swings back, so the flight is only trusted up to here. */
    public static final int LAST_TICK = 31;
    /** For its first ticks the eye stands still on the client, and a shorter stretch has no usable direction. */
    private static final double MIN_STRETCH = 0.05;

    public enum Status { WAITING, DONE, LOST }

    private EyeSample first;
    private EyeSample last;
    private Ray ray;

    /** {@code sample} is null when the eye is gone. */
    public Status accept(EyeSample sample) {
        if (sample == null || sample.age() > LAST_TICK) {
            return finish();
        }
        if (first == null) {
            first = sample;
            return Status.WAITING;
        }
        last = sample;
        return sample.age() == LAST_TICK ? finish() : Status.WAITING;
    }

    private Status finish() {
        if (first == null || last == null || Math.hypot(last.x() - first.x(), last.z() - first.z()) < MIN_STRETCH) {
            return Status.LOST;
        }
        ray = new TwoPointRay(new Point(first.x(), first.z()), new Point(last.x(), last.z()));
        return Status.DONE;
    }

    public Ray ray() {
        return ray;
    }
}
