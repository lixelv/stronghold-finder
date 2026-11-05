package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumberTextTest {
    @Test
    void coordinatesKeepTwoDecimals() {
        assertEquals("-703.50", NumberText.coordinate(-703.5));
        assertEquals("288.61", NumberText.coordinate(288.61273));
    }

    @Test
    void anglesKeepThreeDecimals() {
        assertEquals("-47.300", NumberText.angle(-47.3));
        assertEquals("67.800", NumberText.angle(67.80007));
    }

    @Test
    void almostZeroHasNoMinus() {
        assertEquals("0.00", NumberText.coordinate(-0.001));
        assertEquals("0.000", NumberText.angle(-0.0001));
    }

    @Test
    void fewerDecimalsRoundAndKeepNoMinusOnZero() {
        assertEquals("-703.5", NumberText.fixed(-703.5, 1));
        assertEquals("0", NumberText.fixed(-0.2, 0));
    }

    @Test
    void outOfRangeYawKeepsItsDecimals() {
        assertEquals(10.123456789, Geometry.normYaw(370.123456789), 1e-9);
    }
}
