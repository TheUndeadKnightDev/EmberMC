package org.embermc.ember.entity;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityTiersTypeTest {

    @Test
    void emptyListPinsNothing() {
        assertFalse(EntityTiers.alwaysFullContains(List.of(), "villager"));
    }

    @Test
    void matchesArePathBasedAndCaseInsensitive() {
        var cfg = List.of("Villager", " piglin ", "ALLAY");
        assertTrue(EntityTiers.alwaysFullContains(cfg, "villager"));
        assertTrue(EntityTiers.alwaysFullContains(cfg, "piglin"));
        assertTrue(EntityTiers.alwaysFullContains(cfg, "allay"));
        assertFalse(EntityTiers.alwaysFullContains(cfg, "zombie"));
    }
}
