package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LabelPlacerTest {
    private final LabelPlacer placer = new LabelPlacer(-100, -100, 100, 100);

    @Test
    void placesToTheRightWhenFree() {
        LabelPlacer.Box box = placer.place(0, 0, 20, 10, 5, false);
        assertEquals(5, box.left());
        assertEquals(-5, box.top());
    }

    @Test
    void movesAwayFromReservedBox() {
        placer.reserve(0, -5, 30, 5);
        LabelPlacer.Box box = placer.place(0, 0, 20, 10, 5, false);
        assertFalse(box.overlaps(new LabelPlacer.Box(0, -5, 30, 5)));
    }

    @Test
    void movesInsideTheBounds() {
        LabelPlacer.Box box = placer.place(95, 0, 20, 10, 5, false);
        assertTrue(box.right() <= 100);
    }

    @Test
    void hidesLabelWhenEverySpotIsTaken() {
        placer.reserve(-100, -100, 100, 100);
        assertNull(placer.place(0, 0, 20, 10, 5, false));
    }

    @Test
    void forcedLabelIsPlacedAnyway() {
        placer.reserve(-100, -100, 100, 100);
        assertNotNull(placer.place(0, 0, 20, 10, 5, true));
    }

    @Test
    void placedLabelsBlockLaterOnes() {
        LabelPlacer.Box first = placer.place(0, 0, 20, 10, 5, false);
        LabelPlacer.Box second = placer.place(0, 0, 20, 10, 5, false);
        assertFalse(first.overlaps(second));
    }
}
