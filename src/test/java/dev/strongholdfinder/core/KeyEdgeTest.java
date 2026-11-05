package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyEdgeTest {
    @Test
    void firstPressFires() {
        assertTrue(new KeyEdge().update(true, true));
    }

    @Test
    void holdingTheKeyDoesNotFireAgainOnOsRepeat() {
        KeyEdge edge = new KeyEdge();
        edge.update(true, true);
        assertFalse(edge.update(true, true));
        assertFalse(edge.update(false, true));
        assertFalse(edge.update(true, true));
    }

    @Test
    void pressAfterReleaseFires() {
        KeyEdge edge = new KeyEdge();
        edge.update(true, true);
        edge.update(false, false);
        assertTrue(edge.update(true, true));
    }

    @Test
    void tapWithinOneTickFires() {
        KeyEdge edge = new KeyEdge();
        assertTrue(edge.update(true, false));
        assertTrue(edge.update(true, false));
    }

    @Test
    void noClickDoesNotFire() {
        assertFalse(new KeyEdge().update(false, false));
    }
}
