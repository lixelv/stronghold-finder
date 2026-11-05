package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdviceTest {
    private static final Point T = new Point(1832, -1240);

    private static Throw aim(int id, double x, double z, double noise) {
        return new Throw(id, x, z, Geometry.yawTo(new Point(x, z), T) + noise, true);
    }

    private static List<String> keys(List<Throw> throwsList) {
        return Advice.of(Solver.solve(throwsList, Map.of())).stream().map(AdviceLine::key).toList();
    }

    @Test
    void noThrowsAsksForFirstThrow() {
        assertEquals(List.of("advice.first_throw"), keys(List.of()));
    }

    @Test
    void oneThrowAsksToMoveAside() {
        List<AdviceLine> lines = Advice.of(Solver.solve(List.of(new Throw(1, 0, 0, 0, true)), Map.of()));
        assertEquals("advice.move_aside", lines.get(0).key());
        assertEquals(Compass.WEST, lines.get(0).args()[0]);
        assertEquals(Compass.EAST, lines.get(0).args()[1]);
    }

    @Test
    void preciseResultSaysGo() {
        assertEquals(List.of("advice.go"), keys(List.of(aim(1, 100, 250, 0), aim(2, 900, 900, 0), aim(3, 1500, 200, 0))));
    }

    @Test
    void roughResultAsksToRefine() {
        List<String> keys = keys(List.of(aim(1, 100, 250, 0), aim(2, 300, 250, 0)));
        assertTrue(keys.contains("advice.refine"));
    }

    @Test
    void divergingRaysWithoutResultWarn() {
        List<AdviceLine> lines = Advice.of(Solver.solve(List.of(new Throw(1, 0, 0, 45, true), new Throw(2, 100, 0, -45, true)), Map.of()));
        assertEquals("advice.behind", lines.get(0).key());
        assertTrue(lines.get(0).warn());
    }

    @Test
    void someDivergingRaysWarnNextToResult() {
        Throw bad = new Throw(3, 1500, 200, 13, true);
        assertTrue(keys(List.of(aim(1, 100, 250, 0), aim(2, 780, 420, 0), bad)).contains("advice.behind_some"));
    }

    @Test
    void parallelRaysWarn() {
        assertEquals(List.of("advice.parallel"), keys(List.of(new Throw(1, 0, 0, 90, true), new Throw(2, 100, 0, -90, true))));
    }

    @Test
    void weakFallbackWarns() {
        assertTrue(keys(List.of(aim(1, 100, 250, 0), aim(2, 130, 260, 0))).contains("advice.weak"));
    }

    @Test
    void pointOutsideRingsWarns() {
        Point near = new Point(300, 0);
        Throw a = new Throw(1, 0, -300, Geometry.yawTo(new Point(0, -300), near), true);
        Throw b = new Throw(2, 0, 300, Geometry.yawTo(new Point(0, 300), near), true);
        List<AdviceLine> lines = Advice.of(Solver.solve(List.of(a, b), Map.of()));
        assertEquals("advice.outside_rings", lines.get(0).key());
        assertTrue(lines.get(0).warn());
        assertFalse(lines.stream().anyMatch(l -> l.key().equals("advice.go")));
    }
}
