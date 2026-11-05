package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MapShapesTest {
    @Test
    void rayStartsAtZeroWhenThrowIsOnScreen() {
        MapShapes.RaySpan span = MapShapes.raySpan(100, 1000, 2);
        assertEquals(0, span.start());
        assertEquals(550, span.end());
    }

    @Test
    void rayStartsNearTheViewWhenThrowIsFarAway() {
        MapShapes.RaySpan span = MapShapes.raySpan(5000, 1000, 2);
        assertEquals(2000, span.start());
        assertEquals(3000, span.end());
    }

    @Test
    void absurdlyFarThrowHasNoSpanInsteadOfOverflowing() {
        assertNull(MapShapes.raySpan(1e12, 1000, 2));
        assertNull(MapShapes.raySpan(Double.POSITIVE_INFINITY, 1000, 2));
        assertNull(MapShapes.raySpan(Double.NaN, 1000, 2));
    }

    @Test
    void largestAllowedSpanStaysFarBelowIntOverflow() {
        MapShapes.RaySpan span = MapShapes.raySpan(1.9e8, 2000, 2);
        assertNotNull(span);
        assertTrue(span.end() < Integer.MAX_VALUE / 2);
    }

    @Test
    void circleUsesAtMostTheBandLimit() {
        assertTrue(MapShapes.circleBands(2000, 32).size() <= 32);
        assertTrue(MapShapes.circleBands(150, 32).size() <= 32);
    }

    @Test
    void bandsCoverTheWholeDiameterWithoutGaps() {
        List<MapShapes.Band> bands = MapShapes.circleBands(100, 32);
        assertEquals(-100, bands.get(0).top());
        assertEquals(100, bands.get(bands.size() - 1).bottom());
        for (int i = 1; i < bands.size(); i++) {
            assertEquals(bands.get(i - 1).bottom(), bands.get(i).top());
        }
    }

    @Test
    void bandsAreWidestInTheMiddle() {
        List<MapShapes.Band> bands = MapShapes.circleBands(100, 32);
        int middle = bands.get(bands.size() / 2).halfWidth();
        assertTrue(middle > bands.get(0).halfWidth());
        assertTrue(middle >= 95 && middle <= 100);
    }

    @Test
    void smallCircleKeepsOnePixelBands() {
        assertEquals(10, MapShapes.circleBands(5, 32).size());
    }
}
