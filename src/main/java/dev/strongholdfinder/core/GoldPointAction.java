package dev.strongholdfinder.core;

/** What to do with the gold waypoint after the throws change. */
public enum GoldPointAction {
    NONE, CREATE, MOVE, REMOVE;

    public static GoldPointAction of(Solution solution, boolean exists) {
        if (solution.mean() != null) {
            return exists ? MOVE : CREATE;
        }
        return exists ? REMOVE : NONE;
    }
}
