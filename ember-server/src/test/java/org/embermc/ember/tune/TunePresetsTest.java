package org.embermc.ember.tune;

import java.util.HashSet;
import java.util.Set;
import org.embermc.ember.config.Preset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TunePresetsTest {

    @Test
    void everySettingHasAVanillaValueSoRevertIsAlwaysExact() {
        for (final Setting s : TunePresets.SETTINGS) {
            assertTrue(s.values().containsKey(Preset.VANILLA), s.path() + " has no vanilla (upstream default) value");
        }
    }

    @Test
    void everyPresetResolvesForEverySetting() {
        for (final Setting s : TunePresets.SETTINGS) {
            for (final Preset p : Preset.values()) {
                assertNotNull(s.value(p), s.path() + " resolves nothing for " + p);
            }
        }
    }

    @Test
    void pathsAreUniquePerFile() {
        final Set<String> seen = new HashSet<>();
        for (final Setting s : TunePresets.SETTINGS) {
            assertTrue(seen.add(s.file() + ":" + s.path()), "duplicate key " + s.path());
        }
    }

    @Test
    void presetsFallBackToTheGentlerOne() {
        final Setting s = new Setting(Setting.File.SPIGOT, "x", java.util.Map.of(Preset.VANILLA, 1, Preset.PERFORMANCE, 3), null, "why");
        assertEquals(1, s.value(Preset.VANILLA));
        assertEquals(1, s.value(Preset.BALANCED), "balanced inherits vanilla when unset");
        assertEquals(3, s.value(Preset.PERFORMANCE));
        assertEquals(3, s.value(Preset.EXTREME), "extreme inherits performance when unset");
    }

    @Test
    void everyChangedKeyExplainsWhatAPlayerCouldNotice() {
        for (final Setting s : TunePresets.SETTINGS) {
            final boolean changesSomething = Preset.values().length > 1
                && !s.value(Preset.EXTREME).equals(s.value(Preset.VANILLA));
            if (changesSomething) {
                assertNotNull(s.visible(), s.path() + " changes behaviour but says nothing about what is visible");
                assertFalse(s.visible().isBlank(), s.path());
            }
            assertFalse(s.why().isBlank(), s.path() + " has no reason");
        }
    }

    @Test
    void theOneMeasuredClaimIsTheRedstoneEngine() {
        final Setting redstone = TunePresets.SETTINGS.stream()
            .filter(s -> s.path().equals("misc.redstone-implementation")).findFirst().orElseThrow();
        assertEquals("ALTERNATE_CURRENT", redstone.value(Preset.BALANCED));
        assertTrue(redstone.why().contains("Measured"), "the redstone entry must say it was measured; nothing else may");
        for (final Setting s : TunePresets.SETTINGS) {
            if (s != redstone) {
                assertFalse(s.why().contains("Measured"), s.path() + " claims a measurement that was not made");
            }
        }
    }
}
