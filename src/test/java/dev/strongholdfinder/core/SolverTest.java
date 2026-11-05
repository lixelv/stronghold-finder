package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SolverTest {
    private static final Point T = new Point(1832, -1240);

    private static Throw aim(int id, double x, double z, double noise) {
        return new Throw(id, x, z, Geometry.yawTo(new Point(x, z), T) + noise, true);
    }

    private static Throw aim(int id, double x, double z) {
        return aim(id, x, z, 0);
    }

    private static Throw raw(int id, double x, double z, double yaw) {
        return new Throw(id, x, z, yaw, true);
    }

    private static Pair pair(Solution s, String key) {
        return s.pairs().stream().filter(p -> p.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void compassMatchesMinecraftDirections() {
        assertEquals(Compass.SOUTH, Geometry.compass(0));
        assertEquals(Compass.WEST, Geometry.compass(90));
        assertEquals(Compass.NORTH, Geometry.compass(180));
        assertEquals(Compass.NORTH, Geometry.compass(-180));
        assertEquals(Compass.EAST, Geometry.compass(-90));
        assertEquals(Compass.SOUTH_EAST, Geometry.compass(-45));
        assertEquals(Compass.NORTH_WEST, Geometry.compass(135));
    }

    @Test
    void yawZeroPointsToPositiveZAndNinetyToNegativeX() {
        assertEquals(1, Geometry.dir(0).z(), 1e-9);
        assertEquals(-1, Geometry.dir(90).x(), 1e-9);
        assertEquals(1, Geometry.dir(-90).x(), 1e-9);
        assertEquals(-1, Geometry.dir(180).z(), 1e-9);
    }

    @Test
    void yawToInvertsDir() {
        for (double y : new double[] {-179, -120, -47.3, 0, 12.5, 90, 179}) {
            Point d = Geometry.dir(y);
            assertEquals(y, Geometry.yawTo(new Point(5, 7), new Point(5 + d.x() * 100, 7 + d.z() * 100)), 1e-8);
        }
    }

    @Test
    void normYawWrapsWithoutFloatNoise() {
        assertEquals(10, Geometry.normYaw(370), 1e-9);
        assertEquals(170, Geometry.normYaw(-190), 1e-9);
        assertEquals(180, Geometry.normYaw(180));
        assertEquals(180, Geometry.normYaw(-180));
        assertEquals(-47.3, Geometry.normYaw(-47.3));
    }

    @Test
    void recoversTargetFromTwoExactThrows() {
        Solution r = Solver.solve(List.of(aim(1, 100, 250), aim(2, 780, 420)), Map.of());
        assertEquals(T.x(), r.mean().x(), 1e-5);
        assertEquals(T.z(), r.mean().z(), 1e-5);
        assertEquals(1, r.used().size());
        assertEquals(1, r.ring().n());
        assertTrue(r.ring().inside());
    }

    @Test
    void averagesAllPairwiseIntersections() {
        List<Throw> ts = List.of(aim(1, 100, 250, 0.12), aim(2, 780, 420, -0.09), aim(3, 1500, 200, 0.1));
        Solution r = Solver.solve(ts, Map.of());
        assertEquals(3, r.used().size());
        double x = r.used().stream().mapToDouble(Pair::x).average().orElseThrow();
        double z = r.used().stream().mapToDouble(Pair::z).average().orElseThrow();
        assertEquals(x, r.mean().x(), 1e-9);
        assertEquals(z, r.mean().z(), 1e-9);
        assertTrue(Math.abs(r.mean().x() - T.x()) < 40);
        assertNotNull(r.lsq());
    }

    @Test
    void twoRayUncertaintyMatchesClosedForm() {
        Throw a = aim(1, 100, 250);
        Throw b = aim(2, 780, 420);
        Solution r = Solver.solve(List.of(a, b), Map.of());
        double d1 = Math.hypot(T.x() - a.x(), T.z() - a.z());
        double d2 = Math.hypot(T.x() - b.x(), T.z() - b.z());
        double expected = Math.toRadians(0.2) * Math.sqrt(d1 * d1 + d2 * d2) / Math.sin(Math.toRadians(r.used().get(0).angle()));
        assertEquals(expected, r.theory(), 1e-5);
    }

    @Test
    void rejectsParallelAndDivergingRays() {
        Solution parallel = Solver.solve(List.of(raw(1, 0, 0, 90), raw(2, 100, 0, -90)), Map.of());
        assertEquals(PairStatus.PARALLEL, parallel.pairs().get(0).status());
        Solution behind = Solver.solve(List.of(raw(1, 0, 0, 45), raw(2, 100, 0, -45)), Map.of());
        assertEquals(PairStatus.BEHIND, behind.pairs().get(0).status());
        assertNull(behind.mean());
    }

    @Test
    void usesWeakPairOnlyWhenNothingBetterOrForced() {
        Throw a = aim(1, 100, 250);
        Throw b = aim(2, 130, 260);
        Solution solo = Solver.solve(List.of(a, b), Map.of());
        assertEquals(PairStatus.WEAK, solo.pairs().get(0).status());
        assertTrue(solo.weakFallback());
        assertNotNull(solo.mean());
        Throw c = aim(3, 1120, -160, 0.5);
        Solution r = Solver.solve(List.of(a, b, c), Map.of());
        assertFalse(pair(r, "1-2").used());
        assertEquals(2, r.used().size());
        assertEquals(3, Solver.solve(List.of(a, b, c), Map.of("1-2", true)).used().size());
    }

    @Test
    void honoursOverridesAndDisabledThrows() {
        Throw a = aim(1, 100, 250);
        Throw b = aim(2, 780, 420);
        Throw c = aim(3, 1120, -160, 3);
        assertEquals(1, Solver.solve(List.of(a, b, c), Map.of("1-3", false, "2-3", false)).used().size());
        Solution r = Solver.solve(List.of(a, b, new Throw(3, c.x(), c.z(), c.yaw(), false)), Map.of());
        assertEquals(1, r.pairs().size());
        assertEquals(2, r.last().id());
    }

    @Test
    void doesNotPairThrowsFromOneSpot() {
        Solution r = Solver.solve(List.of(raw(1, 0, 0, 10), raw(2, 0, 0, 40)), Map.of());
        assertEquals(PairStatus.SAME, r.pairs().get(0).status());
        assertNull(r.mean());
    }

    @Test
    void handlesZeroAndOneThrow() {
        assertNull(Solver.solve(List.of(), Map.of()).mean());
        assertNull(Solver.solve(List.of(aim(1, 0, 0)), Map.of()).mean());
    }

    @Test
    void ringsCover128Strongholds() {
        assertEquals(128, Arrays.stream(Geometry.RINGS).mapToInt(r -> r[2]).sum());
    }

    @Test
    void ringOfClassifiesDistances() {
        assertEquals(1, Geometry.ringOf(2000, 0).n());
        assertEquals(2, Geometry.ringOf(5000, 0).n());
        assertFalse(Geometry.ringOf(3500, 0).inside());
        assertTrue(Geometry.ringOf(2900, 100).inside());
        assertEquals(780, Geometry.ringOf(500, 10).gap(), 1e-9);
    }
}
