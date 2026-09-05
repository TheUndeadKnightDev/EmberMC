package org.embermc.ember.adaptive;

import org.embermc.ember.config.EmberConfigurations;
import org.embermc.ember.config.EmberGlobalConfiguration;
import org.embermc.ember.entity.EntityTiers;
import org.embermc.ember.profiler.EmberProfiler;
import org.embermc.ember.profiler.Phase;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires the {@link AdaptiveEngine} to the server: feeds it the tick p95 once a
 * second, pushes the resulting level into the systems that respond, and says
 * so - once per change - in the console.
 *
 * <p>Every response is bounded by the responder, not by this class: the entity
 * tiers clamp their own fraction and interval, so no level can ever push a mob
 * below what a player would notice. Turning the engine off returns every
 * responder to its preset instantly.
 */
@NullMarked
public final class AdaptiveRuntime {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmberMC");
    private static final AdaptiveEngine ENGINE = new AdaptiveEngine(AdaptiveEngine.Settings.DEFAULT);
    private static long seconds;
    private static AdaptiveEngine.LoadLevel lastApplied = AdaptiveEngine.LoadLevel.NORMAL;
    private static double lastP95;

    private AdaptiveRuntime() {
    }

    /** Called every tick from the profiler; does its work every 20th. */
    public static void tick(final long tickCount) {
        if (tickCount % 20 != 0) {
            return;
        }
        seconds++;
        final EmberGlobalConfiguration.Adaptive cfg = EmberConfigurations.global().adaptive;
        if (!cfg.enabled) {
            if (lastApplied != AdaptiveEngine.LoadLevel.NORMAL) {
                ENGINE.force(AdaptiveEngine.LoadLevel.NORMAL, seconds);
                apply(AdaptiveEngine.LoadLevel.NORMAL, "adaptive engine disabled");
            }
            return;
        }
        ENGINE.settings(new AdaptiveEngine.Settings(cfg.lightAboveMs, cfg.moderateAboveMs, cfg.aggressiveAboveMs,
            cfg.exitMarginMs, cfg.holdUpSeconds, cfg.holdDownSeconds, ceiling(cfg.ceiling)));
        lastP95 = EmberProfiler.ring(Phase.TICK).stats(100).p95Ms();
        final AdaptiveEngine.LoadLevel level = ENGINE.observe(lastP95, seconds);
        if (level != lastApplied) {
            apply(level, String.format("tick p95 %.1f ms over the last 5 s", lastP95));
        }
    }

    private static void apply(final AdaptiveEngine.LoadLevel level, final String why) {
        EntityTiers.setLoadResponse(level.fullRingScale, level.intervalScale);
        if (level.compareTo(lastApplied) > 0) {
            LOGGER.warn("Load {}: {}. Entity outer ring now ticks 1/{} of preset rate, full ring x{}.",
                level.name().toLowerCase(java.util.Locale.ROOT), why, level.intervalScale, level.fullRingScale);
        } else {
            LOGGER.info("Load back to {}: {}.", level.name().toLowerCase(java.util.Locale.ROOT), why);
        }
        lastApplied = level;
    }

    private static AdaptiveEngine.LoadLevel ceiling(final String name) {
        try {
            return AdaptiveEngine.LoadLevel.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            return AdaptiveEngine.LoadLevel.AGGRESSIVE;
        }
    }

    public static AdaptiveEngine.LoadLevel level() {
        return ENGINE.level();
    }

    public static double lastP95() {
        return lastP95;
    }

    public static long changes() {
        return ENGINE.changes();
    }

    /** Seconds since the level last changed, or -1 if it never has. */
    public static long secondsSinceChange() {
        return ENGINE.lastChangeSecond() < 0 ? -1 : seconds - ENGINE.lastChangeSecond();
    }
}
