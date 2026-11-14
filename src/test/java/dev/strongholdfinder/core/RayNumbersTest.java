package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RayNumbersTest {
    private static Throw t(int id, boolean on) {
        return new Throw(id, 0, 0, 0, on);
    }

    @Test
    void numbersFollowListOrder() {
        RayNumbers n = RayNumbers.of(List.of(t(1, true), t(2, true), t(3, true)));
        assertEquals(1, n.of(1));
        assertEquals(2, n.of(2));
        assertEquals(3, n.of(3));
    }

    @Test
    void deleteShiftsLaterRaysOnly() {
        RayNumbers firstGone = RayNumbers.of(List.of(t(2, true), t(3, true), t(5, true)));
        assertEquals(1, firstGone.of(2));
        assertEquals(2, firstGone.of(3));
        assertEquals(3, firstGone.of(5));

        RayNumbers middleGone = RayNumbers.of(List.of(t(1, true), t(3, true)));
        assertEquals(1, middleGone.of(1));
        assertEquals(2, middleGone.of(t(3, true)));
    }

    @Test
    void switchedOffRayKeepsNumber() {
        RayNumbers n = RayNumbers.of(List.of(t(1, true), t(2, false), t(4, true)));
        assertEquals(2, n.of(2));
        assertEquals(3, n.of(4));
    }

    @Test
    void unknownIdIsZero() {
        assertEquals(0, RayNumbers.of(List.of(t(1, true))).of(7));
        assertEquals(0, RayNumbers.of(List.of()).of(1));
    }
}
