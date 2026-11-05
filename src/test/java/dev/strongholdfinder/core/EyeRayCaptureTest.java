package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class EyeRayCaptureTest {
    @Test
    void twoTicksGiveARayFromTheFirstPoint() {
        EyeRayCapture capture = new EyeRayCapture();
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(10, 20, 8)));
        assertEquals(EyeRayCapture.Status.DONE, capture.accept(new EyeSample(10.3, 20.4, 9)));
        Ray ray = assertInstanceOf(TwoPointRay.class, capture.ray());
        assertEquals(10, ray.origin().x(), 1e-9);
        assertEquals(20, ray.origin().z(), 1e-9);
        assertEquals(0.6, ray.direction().x(), 1e-9);
        assertEquals(0.8, ray.direction().z(), 1e-9);
    }

    @Test
    void anEyeThatHasNotStartedMovingIsWaitedFor() {
        EyeRayCapture capture = new EyeRayCapture();
        capture.accept(new EyeSample(5, 5, 0));
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(5, 5, 1)));
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(5, 5, 2)));
        assertEquals(EyeRayCapture.Status.DONE, capture.accept(new EyeSample(5, 5.2, 3)));
        assertEquals(5, capture.ray().origin().z(), 1e-9);
    }

    @Test
    void theEyeDisappearingBeforeTheSecondTickLosesIt() {
        EyeRayCapture capture = new EyeRayCapture();
        capture.accept(new EyeSample(1, 1, 10));
        assertEquals(EyeRayCapture.Status.LOST, capture.accept(null));
    }

    @Test
    void noEyeAtAllLosesIt() {
        assertEquals(EyeRayCapture.Status.LOST, new EyeRayCapture().accept(null));
    }

    @Test
    void anEyeThatMayAlreadyFlyBackIsLost() {
        assertEquals(EyeRayCapture.Status.LOST, new EyeRayCapture().accept(new EyeSample(1, 1, EyeRayCapture.LAST_TICK + 1)));
        EyeRayCapture capture = new EyeRayCapture();
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(1, 1, EyeRayCapture.LAST_TICK)));
        assertEquals(EyeRayCapture.Status.LOST, capture.accept(new EyeSample(1, 1.5, EyeRayCapture.LAST_TICK + 1)));
    }
}
