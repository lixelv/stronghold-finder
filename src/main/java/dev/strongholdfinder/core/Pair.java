package dev.strongholdfinder.core;

/**
 * {@code x} and {@code z} are NaN when the rays have no usable crossing.
 * {@code inRing} is true for a usable crossing within a stronghold ring (with a margin); it does not affect {@code used}.
 */
public record Pair(String key, Throw a, Throw b, PairStatus status, double angle, double x, double z, boolean usable, boolean used,
        boolean inRing) {
}
