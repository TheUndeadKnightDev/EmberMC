package org.embermc.ember.adaptive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveResponderTest {

    @Test
    void normalLoadLeavesPathfindingUntouched() {
        assertEquals(8, AdaptiveRuntime.scaledPathfindFailures(8, AdaptiveEngine.LoadLevel.NORMAL));
        assertEquals(40, AdaptiveRuntime.scaledPathfindBackoff(40, AdaptiveEngine.LoadLevel.NORMAL));
    }

    @Test
    void higherLoadGivesUpSoonerAndStaysOffLonger() {
        int fN = AdaptiveRuntime.scaledPathfindFailures(8, AdaptiveEngine.LoadLevel.NORMAL);
        int fL = AdaptiveRuntime.scaledPathfindFailures(8, AdaptiveEngine.LoadLevel.LIGHT);
        int fM = AdaptiveRuntime.scaledPathfindFailures(8, AdaptiveEngine.LoadLevel.MODERATE);
        int fA = AdaptiveRuntime.scaledPathfindFailures(8, AdaptiveEngine.LoadLevel.AGGRESSIVE);
        assertTrue(fN >= fL && fL >= fM && fM >= fA, "failures shrink with load");

        int bN = AdaptiveRuntime.scaledPathfindBackoff(40, AdaptiveEngine.LoadLevel.NORMAL);
        int bA = AdaptiveRuntime.scaledPathfindBackoff(40, AdaptiveEngine.LoadLevel.AGGRESSIVE);
        assertTrue(bA > bN, "backoff grows with load");
    }

    @Test
    void boundsAreRespected() {
        // never below two failures even from a tiny base under max load
        assertEquals(2, AdaptiveRuntime.scaledPathfindFailures(1, AdaptiveEngine.LoadLevel.AGGRESSIVE));
        // backoff capped at 200 ticks
        assertEquals(200, AdaptiveRuntime.scaledPathfindBackoff(1000, AdaptiveEngine.LoadLevel.AGGRESSIVE));
    }
}
