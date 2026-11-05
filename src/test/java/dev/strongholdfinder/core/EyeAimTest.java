package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EyeAimTest {
    @Test
    void aimIsAllowedOnlyInPrivateSingleplayer() {
        assertTrue(EyeAim.isAllowed(true, false));
        assertFalse(EyeAim.isAllowed(true, true), "a world opened to LAN has other players");
        assertFalse(EyeAim.isAllowed(false, false), "a remote server");
    }

    @Test
    void aimsSouth() {
        Aim aim = EyeAim.compute(0, 64, 0, 0, 64, 10);
        assertEquals(0, aim.yaw(), 1e-9);
        assertEquals(0, aim.pitch(), 1e-9);
    }

    @Test
    void aimsWest() {
        assertEquals(90, EyeAim.compute(0, 64, 0, -10, 64, 0).yaw(), 1e-9);
    }

    @Test
    void aimsUp() {
        Aim aim = EyeAim.compute(0, 64, 0, 10, 74, 0);
        assertEquals(-90, aim.yaw(), 1e-9);
        assertEquals(-45, aim.pitch(), 1e-9);
    }

    @Test
    void aimsDown() {
        assertEquals(45, EyeAim.compute(0, 64, 0, 10, 54, 0).pitch(), 1e-9);
    }

    @Test
    void tooCloseHorizontallyIsNull() {
        assertNull(EyeAim.compute(5, 64, 5, 5.2, 70, 5.1));
    }

    @Test
    void picksNewestEye() {
        EyeCandidate newest = new EyeCandidate(1, 1, 1, 3);
        assertEquals(newest, EyeAim.pick(List.of(new EyeCandidate(0, 0, 0, 40), newest, new EyeCandidate(2, 2, 2, 12))));
    }

    @Test
    void pickOfNothingIsNull() {
        assertNull(EyeAim.pick(List.of()));
    }
}
