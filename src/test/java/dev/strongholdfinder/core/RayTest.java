package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RayTest {
    private static void assertPoint(double x, double z, Point point) {
        assertEquals(x, point.x(), 1e-9);
        assertEquals(z, point.z(), 1e-9);
    }

    @Test
    void angleRayFollowsTheGameYaw() {
        assertPoint(0, 1, new AngleRay(new Point(1, 2), 0).direction());
        assertPoint(-1, 0, new AngleRay(new Point(1, 2), 90).direction());
    }

    @Test
    void angleRayNormalizesYaw() {
        assertEquals(5, new AngleRay(new Point(0, 0), 725).yaw(), 1e-9);
    }

    @Test
    void angleRayNeedsARealAngle() {
        assertThrows(IllegalArgumentException.class, () -> new AngleRay(new Point(0, 0), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new AngleRay(new Point(0, 0), Double.POSITIVE_INFINITY));
    }

    @Test
    void twoPointRayStartsAtTheFirstPointAndHasAUnitDirection() {
        Ray ray = new TwoPointRay(new Point(10, 20), new Point(13, 24));
        assertPoint(10, 20, ray.origin());
        assertPoint(0.6, 0.8, ray.direction());
    }

    @Test
    void twoPointRayNeedsTwoDifferentPoints() {
        assertThrows(IllegalArgumentException.class, () -> new TwoPointRay(new Point(1, 1), new Point(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new TwoPointRay(new Point(1, 1), new Point(Double.NaN, 1)));
    }

    @Test
    void equationGivesPointsAlongTheRay() {
        RayEquation equation = new TwoPointRay(new Point(10, 20), new Point(13, 24)).equation();
        assertPoint(10, 20, equation.at(0));
        assertPoint(16, 28, equation.at(10));
    }

    @Test
    void bothTypesOfTheSameRayAreInterchangeable() {
        Point from = new Point(-40, 75);
        Point through = new Point(-12, 19);
        Ray two = new TwoPointRay(from, through);
        Ray angle = new AngleRay(from, Geometry.yawTo(from, through));
        assertEquals(angle.yaw(), two.yaw(), 1e-9);
        assertPoint(angle.direction().x(), angle.direction().z(), two.direction());
        assertPoint(angle.equation().at(50).x(), angle.equation().at(50).z(), two.equation().at(50));
    }

    @Test
    void solverGivesTheSamePointForEitherType() {
        Point target = new Point(1000, 2000);
        Point a = new Point(0, 0);
        Point b = new Point(500, 0);
        Solution withAngles = Solver.solve(List.of(
                new Throw(1, new AngleRay(a, Geometry.yawTo(a, target)), true),
                new Throw(2, new AngleRay(b, Geometry.yawTo(b, target)), true)), Map.of());
        Solution mixed = Solver.solve(List.of(
                new Throw(1, new TwoPointRay(a, new Point(0.5, 1)), true),
                new Throw(2, new AngleRay(b, Geometry.yawTo(b, target)), true)), Map.of());
        assertEquals(target.x(), withAngles.mean().x(), 1e-6);
        assertEquals(target.z(), withAngles.mean().z(), 1e-6);
        assertEquals(target.x(), mixed.mean().x(), 1e-6);
        assertEquals(target.z(), mixed.mean().z(), 1e-6);
    }
}
