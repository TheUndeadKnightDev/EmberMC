package org.embermc.ember.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkInsightsTest {

    @Test
    void warnsOnlyAboveTheThreshold() {
        // threshold 400, cooldown elapsed
        assertFalse(ChunkInsights.shouldWarn(400, 400, 10_000, 6000));
        assertFalse(ChunkInsights.shouldWarn(399, 400, 10_000, 6000));
        assertTrue(ChunkInsights.shouldWarn(401, 400, 10_000, 6000));
    }

    @Test
    void respectsTheCooldown() {
        assertFalse(ChunkInsights.shouldWarn(1000, 400, 5999, 6000));
        assertTrue(ChunkInsights.shouldWarn(1000, 400, 6000, 6000));
    }

    @Test
    void zeroThresholdDisablesTheWarning() {
        assertFalse(ChunkInsights.shouldWarn(100_000, 0, 999_999, 6000));
    }
}
