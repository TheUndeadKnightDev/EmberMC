package org.embermc.ember.adaptive;

import org.embermc.ember.adaptive.AdaptiveEngine.LoadLevel;
import org.embermc.ember.adaptive.AdaptiveEngine.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdaptiveEngineTest {

    /** light > 35, moderate > 45, aggressive > 50; exit 5 below entry; 3 s to rise, 6 s to fall. */
    private static final Settings S = new Settings(35, 45, 50, 5, 3, 6, LoadLevel.AGGRESSIVE);

    private static LoadLevel feed(final AdaptiveEngine e, final double p95, final int seconds, final long[] clock) {
        LoadLevel l = e.level();
        for (int i = 0; i < seconds; i++) {
            l = e.observe(p95, clock[0]++);
        }
        return l;
    }

    @Test
    void staysNormalUnderTheLightThreshold() {
        final AdaptiveEngine e = new AdaptiveEngine(S);
        assertEquals(LoadLevel.NORMAL, feed(e, 30, 60, new long[]{0}));
        assertEquals(0, e.changes());
    }

    @Test
    void risesOnlyAfterTheHold() {
        final AdaptiveEngine e = new AdaptiveEngine(S);
        final long[] clock = {0};
        assertEquals(LoadLevel.NORMAL, feed(e, 40, 2, clock), "two seconds above is not enough");
        assertEquals(LoadLevel.LIGHT, feed(e, 40, 1, clock), "the third second trips it");
    }

    @Test
    void aBlipDoesNotTripIt() {
        final AdaptiveEngine e = new AdaptiveEngine(S);
        final long[] clock = {0};
        feed(e, 40, 2, clock);
        feed(e, 20, 1, clock); // one quiet second resets the climb
        assertEquals(LoadLevel.NORMAL, feed(e, 40, 2, clock));
    }

    @Test
    void climbsOneLevelAtATimeEvenUnderHeavyLoad() {
        final AdaptiveEngine e = new AdaptiveEngine(S);
        final long[] clock = {0};
        assertEquals(LoadLevel.LIGHT, feed(e, 90, 3, clock));
        assertEquals(LoadLevel.MODERATE, feed(e, 90, 3, clock));
        assertEquals(LoadLevel.AGGRESSIVE, feed(e, 90, 3, clock));
        assertEquals(LoadLevel.AGGRESSIVE, feed(e, 90, 30, clock), "there is nothing above aggressive");
        assertEquals(3, e.changes());
    }

    @Test
    void fallsSlowlyAndOnlyBelowTheExitMargin() {
        final AdaptiveEngine e = new AdaptiveEngine(S);
        final long[] clock = {0};
        feed(e, 90, 9, clock); // -> AGGRESSIVE
        // 47 is below aggressive's entry (50) but not below entry - margin (45): hold the level.
        assertEquals(LoadLevel.AGGRESSIVE, feed(e, 47, 30, clock));
        // 44 is below 45: after the down-hold, one level down.
        assertEquals(LoadLevel.AGGRESSIVE, feed(e, 44, 5, clock));
        assertEquals(LoadLevel.MODERATE, feed(e, 44, 1, clock));
        // 44 is above moderate's exit (45 - 5 = 40): stays moderate.
        assertEquals(LoadLevel.MODERATE, feed(e, 44, 30, clock));
        // Quiet: walks all the way back, six seconds per step.
        assertEquals(LoadLevel.NORMAL, feed(e, 10, 12, clock));
    }

    @Test
    void ceilingIsRespected() {
        final AdaptiveEngine e = new AdaptiveEngine(new Settings(35, 45, 50, 5, 1, 1, LoadLevel.LIGHT));
        assertEquals(LoadLevel.LIGHT, feed(e, 200, 30, new long[]{0}));
    }

    @Test
    void loweringTheCeilingClampsImmediately() {
        final AdaptiveEngine e = new AdaptiveEngine(S);
        feed(e, 90, 9, new long[]{0});
        assertEquals(LoadLevel.AGGRESSIVE, e.level());
        e.settings(new Settings(35, 45, 50, 5, 3, 6, LoadLevel.MODERATE));
        assertEquals(LoadLevel.MODERATE, e.level());
    }

    @Test
    void levelsScaleMonotonically() {
        LoadLevel prev = LoadLevel.NORMAL;
        for (final LoadLevel l : LoadLevel.values()) {
            if (l != prev) {
                org.junit.jupiter.api.Assertions.assertTrue(l.fullRingScale <= prev.fullRingScale && l.intervalScale >= prev.intervalScale,
                    l + " must be at least as gentle on the tick as " + prev);
            }
            prev = l;
        }
    }
}
