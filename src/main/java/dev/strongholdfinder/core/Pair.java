package dev.strongholdfinder.core;

/** {@code x} and {@code z} are NaN when the rays have no usable crossing. */
public record Pair(String key, Throw a, Throw b, PairStatus status, double angle, double x, double z, boolean usable, boolean used) {
}
