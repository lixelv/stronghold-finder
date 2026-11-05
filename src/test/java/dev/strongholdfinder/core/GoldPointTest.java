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
    void nameFollowsTheLanguage() {
        assertEquals("Stronghold", GoldPointName.of(false));
        assertEquals("Крепость", GoldPointName.of(true));
    }

    @Test
    void waypointsNamedByOlderVersionsAreStillOurs() {
        assertTrue(GoldPointName.isOurs("Stronghold ±15"));
        assertTrue(GoldPointName.isOurs("Крепость ±15"));
    }

    @Test
    void bothLanguagesAreRecognized() {
        assertTrue(GoldPointName.isOurs(GoldPointName.of(false)));
        assertTrue(GoldPointName.isOurs(GoldPointName.of(true)));
        assertFalse(GoldPointName.isOurs("Home"));
        assertFalse(GoldPointName.isOurs(null));
    }
}
