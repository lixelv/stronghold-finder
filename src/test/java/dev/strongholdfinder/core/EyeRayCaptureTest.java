package dev.strongholdfinder.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

class EyeRayCaptureTest {
    private static final int LAST = EyeRayCapture.LAST_TICK;

    @Test
    void theRayRunsFromTheFirstPointToTheLastTrustedTick() {
        EyeRayCapture capture = new EyeRayCapture();
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(10, 20, 8)));
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(10.3, 20.4, 9)));
        assertEquals(EyeRayCapture.Status.DONE, capture.accept(new EyeSample(13, 24, LAST)));
        TwoPointRay ray = assertInstanceOf(TwoPointRay.class, capture.ray());
        assertEquals(new Point(10, 20), ray.from());
        assertEquals(new Point(13, 24), ray.through());
    }

    @Test
    void anEyeThatHasNotStartedMovingIsWaitedFor() {
        EyeRayCapture capture = new EyeRayCapture();
        capture.accept(new EyeSample(5, 5, 0));
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(5, 5, 1)));
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(5, 5.2, 4)));
        assertEquals(EyeRayCapture.Status.DONE, capture.accept(new EyeSample(5, 15, LAST)));
        assertEquals(new Point(5, 5), capture.ray().origin());
    }

    @Test
    void anEyeThatIsGoneMidwayStillGivesTheRaySeenSoFar() {
        EyeRayCapture capture = new EyeRayCapture();
        capture.accept(new EyeSample(1, 1, 10));
        capture.accept(new EyeSample(1, 2, 11));
        assertEquals(EyeRayCapture.Status.DONE, capture.accept(null));
        assertEquals(new Point(1, 1), capture.ray().origin());
    }

    @Test
    void theEyeDisappearingBeforeTheSecondPointLosesIt() {
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
        assertEquals(EyeRayCapture.Status.LOST, new EyeRayCapture().accept(new EyeSample(1, 1, LAST + 1)));
        EyeRayCapture capture = new EyeRayCapture();
        assertEquals(EyeRayCapture.Status.WAITING, capture.accept(new EyeSample(1, 1, LAST)));
        assertEquals(EyeRayCapture.Status.LOST, capture.accept(new EyeSample(1, 1.5, LAST + 1)));
    }

    @Test
    void anEyeThatNeverMovesIsLost() {
        EyeRayCapture capture = new EyeRayCapture();
        capture.accept(new EyeSample(3, 3, 20));
        assertEquals(EyeRayCapture.Status.LOST, capture.accept(new EyeSample(3, 3, LAST)));
    }
}
