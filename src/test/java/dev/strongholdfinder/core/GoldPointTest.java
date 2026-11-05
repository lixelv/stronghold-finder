package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GoldPointTest {
    private static Solution withPoint() {
        Point target = new Point(500, 900);
        Point a = new Point(0, 0);
        Point b = new Point(400, 0);
        Solution solution = Solver.solve(List.of(
                new Throw(1, new AngleRay(a, Geometry.yawTo(a, target)), true),
                new Throw(2, new AngleRay(b, Geometry.yawTo(b, target)), true)), Map.of());
        assertNotNull(solution.mean());
        return solution;
    }

    private static Solution withoutPoint() {
        Solution solution = Solver.solve(List.of(), Map.of());
        assertNull(solution.mean());
        return solution;
    }

    @Test
    void aNewPointIsCreated() {
        assertEquals(GoldPointAction.CREATE, GoldPointAction.of(withPoint(), false));
    }

    @Test
    void anExistingPointIsMovedNotCreatedAgain() {
        assertEquals(GoldPointAction.MOVE, GoldPointAction.of(withPoint(), true));
    }

    @Test
    void aPointWithoutAResultIsRemoved() {
        assertEquals(GoldPointAction.REMOVE, GoldPointAction.of(withoutPoint(), true));
    }

    @Test
    void nothingHappensWithoutAResultOrAPoint() {
        assertEquals(GoldPointAction.NONE, GoldPointAction.of(withoutPoint(), false));
    }

    @Test
    void nameFollowsTheLanguageAndRoundsTheErrorUp() {
        assertEquals("Stronghold ±15", GoldPointName.of(false, 14.2));
        assertEquals("Крепость ±15", GoldPointName.of(true, 14.2));
    }

    @Test
    void zeroErrorHasNoMinus() {
        assertEquals("Stronghold ±0", GoldPointName.of(false, 0));
        assertEquals("Stronghold ±0", GoldPointName.of(false, -0.0));
    }

    @Test
    void bothLanguagesAreRecognized() {
        assertTrue(GoldPointName.isOurs(GoldPointName.of(false, 3)));
        assertTrue(GoldPointName.isOurs(GoldPointName.of(true, 3)));
        assertFalse(GoldPointName.isOurs("Home"));
        assertFalse(GoldPointName.isOurs(null));
    }
}
