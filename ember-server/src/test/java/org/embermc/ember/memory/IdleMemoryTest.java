package org.embermc.ember.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdleMemoryTest {

    @Test
    void notDueBeforeTheThreshold() {
        // 5 minutes = 6000 ticks.
        assertFalse(IdleMemory.dueForTrim(0, 5));
        assertFalse(IdleMemory.dueForTrim(5999, 5));
    }

    @Test
    void dueAtAndPastTheThreshold() {
        assertTrue(IdleMemory.dueForTrim(6000, 5));
        assertTrue(IdleMemory.dueForTrim(100000, 5));
    }

    @Test
    void thresholdScalesWithMinutes() {
        assertFalse(IdleMemory.dueForTrim(1199, 1));
        assertTrue(IdleMemory.dueForTrim(1200, 1));
        assertFalse(IdleMemory.dueForTrim(35999, 30));
        assertTrue(IdleMemory.dueForTrim(36000, 30));
    }
}
