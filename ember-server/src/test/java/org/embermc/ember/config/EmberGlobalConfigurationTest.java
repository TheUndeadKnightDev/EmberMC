package org.embermc.ember.config;

import org.embermc.ember.security.PacketCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The config classes are plain serializable POJOs, so their defaults and their
 * internal mappings can be checked without a running server. The full Configurate
 * parse / merge / migration round-trip needs the server's registry and is
 * exercised live ({@code /ember reload} on the box); what is pinned here is the
 * structure and the one mapping a new category can silently break.
 */
class EmberGlobalConfigurationTest {

    @Test
    void everyPacketCategoryHasAGuardLimit() {
        // The classic bug: add a PacketCategory, forget the limitFor switch case.
        // A missing case returns null and NPEs the guard at runtime; catch it here.
        var pg = new EmberGlobalConfiguration().security.packetGuard;
        for (PacketCategory c : PacketCategory.VALUES) {
            assertNotNull(pg.limitFor(c), "no guard limit mapped for category " + c);
        }
    }

    @Test
    void defaultsAreSaneAndSectionsPresent() {
        var cfg = new EmberGlobalConfiguration();
        assertNotNull(cfg.entities);
        assertNotNull(cfg.entities.tiers);
        assertNotNull(cfg.entities.itemLimits);
        assertNotNull(cfg.entities.xpLimits);
        assertNotNull(cfg.entities.pathfinding);
        assertNotNull(cfg.security.packetGuard);
        assertNotNull(cfg.chunks);
        assertNotNull(cfg.memory.idleTrim);
        assertNotNull(cfg.adaptive);
        assertNotNull(cfg.profiler);

        // Anti-abuse features default OFF (they change gameplay); optimisations default ON.
        assertTrue(cfg.entities.tiers.enabled, "tiers on by default");
        assertFalse(cfg.entities.itemLimits.enabled, "item limits off by default");
        assertFalse(cfg.entities.xpLimits.enabled, "xp limits off by default");
        assertTrue(cfg.entities.pathfinding.enabled, "pathfinding backoff on by default");
        assertTrue(cfg.security.packetGuard.enabled, "packet guard on by default");
        assertEquals(Preset.BALANCED, cfg.profile, "balanced profile default");
    }

    @Test
    void constraintBoundsAreCoherent() {
        var cfg = new EmberGlobalConfiguration();
        assertTrue(cfg.entities.pathfinding.failuresBeforeBackoff >= 1);
        assertTrue(cfg.entities.pathfinding.backoffTicks >= 1);
        assertTrue(cfg.chunks.warnThreshold >= 0);
        assertTrue(cfg.memory.idleTrim.afterMinutes >= 1);
        assertTrue(EmberGlobalConfiguration.CURRENT_VERSION >= 1);
    }
}
