package org.embermc.ember.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathfindingBackoffTest {

    @Test
    void noBackoffBeforeTheFailureThreshold() {
        // default: 8 failures arm, 40-tick window
        assertFalse(PathfindingBackoff.shouldSkip(0, 0, 8, 40));
        assertFalse(PathfindingBackoff.shouldSkip(7, 0, 8, 40));
    }

    @Test
    void backoffActiveInsideTheWindowOnceArmed() {
        assertTrue(PathfindingBackoff.shouldSkip(8, 0, 8, 40));
        assertTrue(PathfindingBackoff.shouldSkip(8, 39, 8, 40));
        assertTrue(PathfindingBackoff.shouldSkip(50, 20, 8, 40));
    }

    @Test
    void backoffExpiresAfterTheWindow() {
        assertFalse(PathfindingBackoff.shouldSkip(8, 40, 8, 40));
        assertFalse(PathfindingBackoff.shouldSkip(8, 999, 8, 40));
    }

    @Test
    void negativeElapsedIsNeverBackedOff() {
        // guards against a clock/reset race producing a nonsense elapsed
        assertFalse(PathfindingBackoff.shouldSkip(8, -1, 8, 40));
    }

    @Test
    void zeroOrNegativeConfigDisablesBackoff() {
        assertFalse(PathfindingBackoff.shouldSkip(100, 0, 0, 40));
        assertFalse(PathfindingBackoff.shouldSkip(100, 0, 8, 0));
    }
}
