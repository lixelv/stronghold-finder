package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumberTextTest {
    @Test
    void coordinatesKeepThreeDecimals() {
        assertEquals("-703.500", NumberText.coordinate(-703.5));
        assertEquals("288.613", NumberText.coordinate(288.61273));
    }

    @Test
    void anglesKeepSixDecimals() {
        assertEquals("-47.300000", NumberText.angle(-47.3));
        assertEquals("-113.949860", NumberText.angle(-113.94986041416706));
    }

    @Test
    void almostZeroHasNoMinus() {
        assertEquals("0.000", NumberText.coordinate(-0.0001));
        assertEquals("0.000000", NumberText.angle(-0.0000001));
    }

    @Test
    void fewerDecimalsRoundAndKeepNoMinusOnZero() {
        assertEquals("-703.5", NumberText.fixed(-703.5, 1));
        assertEquals("0", NumberText.fixed(-0.2, 0));
    }

    @Test
    void exactKeepsEveryDigit() {
        assertEquals("-113.94986041416706", NumberText.exact(-113.94986041416706));
        assertEquals("0.00001", NumberText.exact(0.00001));
    }

    @Test
    void wrappingAYawLosesNothing() {
        assertEquals(10.123456789012, Geometry.normYaw(370.123456789012));
        assertEquals(-47.3, Geometry.normYaw(312.7));
        assertEquals(170.25, Geometry.normYaw(-189.75));
        assertEquals(180, Geometry.normYaw(-540));
    }
}
