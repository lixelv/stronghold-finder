package dev.strongholdfinder.xaero;

public sealed interface MapMark {
    double x();

    double z();

    record Probe(double x, double z) implements MapMark {
    }
}
