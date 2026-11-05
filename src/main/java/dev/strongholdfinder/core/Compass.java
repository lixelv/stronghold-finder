package dev.strongholdfinder.core;

public enum Compass {
    SOUTH,
    SOUTH_WEST,
    WEST,
    NORTH_WEST,
    NORTH,
    NORTH_EAST,
    EAST,
    SOUTH_EAST;

    public String key() {
        return "compass." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
