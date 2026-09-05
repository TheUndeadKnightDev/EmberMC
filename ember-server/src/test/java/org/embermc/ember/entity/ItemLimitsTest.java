package org.embermc.ember.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemLimitsTest {

    @Test
    void underOrAtTheCapRemovesNothing() {
        assertEquals(0, ItemLimits.overflow(0, 300));
        assertEquals(0, ItemLimits.overflow(300, 300));
        assertEquals(0, ItemLimits.overflow(1, 300));
    }

    @Test
    void overTheCapRemovesTheExcess() {
        assertEquals(1, ItemLimits.overflow(301, 300));
        assertEquals(700, ItemLimits.overflow(1000, 300));
        assertEquals(4700, ItemLimits.overflow(5000, 300));
    }

    @Test
    void aZeroOrNegativeCapDisablesRemoval() {
        assertEquals(0, ItemLimits.overflow(5000, 0));
        assertEquals(0, ItemLimits.overflow(5000, -1));
    }
}
