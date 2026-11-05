package dev.strongholdfinder.core;

/** Turns key clicks into one action per press, since the OS repeats clicks while a key is held. */
public final class KeyEdge {
    private boolean wasDown;

    public boolean update(boolean clicked, boolean down) {
        boolean fire = clicked && !wasDown;
        wasDown = down;
        return fire;
    }
}
