package org.embermc.ember.bench;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.embermc.ember.adaptive.AdaptiveRuntime;
import org.embermc.ember.entity.EntityTiers;
import org.embermc.ember.profiler.EmberProfiler;
import org.embermc.ember.profiler.Phase;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A reproducible, in-server benchmark of the entity tiers, on the load the
 * server is carrying right now.
 *
 * <p>It holds each tier steady for a settle window and then measures the entity
 * tick phase over a fixed window, cycling vanilla, balanced, performance and
 * extreme, and prints the entity tick time at each with the saving against
 * vanilla. Nothing is spawned and nothing in the world is changed: the tiers are
 * a decision about how often existing mobs tick, so switching between them and
 * reading the profiler is the whole measurement. This is the number in the
 * listing, made repeatable by anyone on their own server.
 *
 * <p>Driven from the profiler tick, one run at a time. The adaptive engine is
 * suspended for the duration so it cannot move the tier mid-measurement, and the
 * original state is restored when the run ends, including if a world unloads.
 */
@NullMarked
public final class Bench {

    private static final EntityTiers.Level[] SEQUENCE = {
        EntityTiers.Level.VANILLA, EntityTiers.Level.BALANCED,
        EntityTiers.Level.PERFORMANCE, EntityTiers.Level.EXTREME,
    };

    private static volatile boolean running;
    private static @Nullable CommandSender sender;
    private static int index;
    private static long phaseStartTick;
    private static long settleTicks;
    private static long measureTicks;
    private static final double[] RESULTS = new double[SEQUENCE.length];

    private Bench() {
    }

    public static boolean running() {
        return running;
    }

    /** Begin a run. settleSeconds lets a tier bed in; measureSeconds is the window read. */
    public static boolean start(final CommandSender who, final int settleSeconds, final int measureSeconds) {
        if (running) {
            return false;
        }
        running = true;
        sender = who;
        index = 0;
        settleTicks = Math.max(20L, settleSeconds * 20L);
        measureTicks = Math.min(1000L, Math.max(40L, measureSeconds * 20L));
        AdaptiveRuntime.suspend(true);
        long entities = 0;
        for (final World w : Bukkit.getWorlds()) {
            entities += w.getEntityCount();
        }
        who.sendMessage(Component.text("Ember benchmark: entity tiers, on the current load ("
            + entities + " entities). About "
            + ((settleTicks + measureTicks) / 20L * SEQUENCE.length) + "s.", NamedTextColor.AQUA));
        if (entities < 200) {
            who.sendMessage(Component.text("Note: few entities loaded, so the difference will be small. "
                + "Run this with players online and mobs about for a meaningful number.", NamedTextColor.GRAY));
        }
        beginPhase(EmberProfiler.tickCount());
        return true;
    }

    private static void beginPhase(final long tickCount) {
        phaseStartTick = tickCount;
        EntityTiers.force(SEQUENCE[index]);
        // Neutralise any load scaling so the measured number is the pure tier.
        EntityTiers.setLoadResponse(1.0, 1);
    }

    /** Called every tick from the profiler. Cheap: two comparisons unless a run is active. */
    public static void tick(final long tickCount) {
        if (!running) {
            return;
        }
        if (tickCount - phaseStartTick < settleTicks + measureTicks) {
            return;
        }
        // Window complete: read the entity phase mean over just the measure window.
        RESULTS[index] = EmberProfiler.ring(Phase.ENTITIES).stats((int) measureTicks).meanMs();
        final CommandSender s = sender;
        if (s != null) {
            s.sendMessage(Component.text("  " + name(SEQUENCE[index]) + ": "
                + String.format(Locale.ROOT, "%.2f ms", RESULTS[index]), NamedTextColor.WHITE));
        }
        index++;
        if (index >= SEQUENCE.length) {
            finish();
        } else {
            beginPhase(tickCount);
        }
    }

    private static void finish() {
        EntityTiers.force(null);
        AdaptiveRuntime.suspend(false);
        EntityTiers.setLoadResponse(1.0, 1);
        final CommandSender s = sender;
        running = false;
        sender = null;
        if (s == null) {
            return;
        }
        final double base = RESULTS[0];
        s.sendMessage(Component.text("Ember benchmark result (entity tick, vanilla = baseline):", NamedTextColor.AQUA));
        for (int i = 0; i < SEQUENCE.length; i++) {
            final String delta;
            if (i == 0) {
                delta = "baseline";
            } else if (base < 0.05) {
                // Baseline is essentially zero (no real entity load); a percentage
                // here is jitter, not a saving. Say so instead of printing noise.
                delta = "no load to measure";
            } else {
                final double pct = (base - RESULTS[i]) / base * 100.0;
                delta = String.format(Locale.ROOT, "%.0f%% lower", pct);
            }
            s.sendMessage(Component.text(String.format(Locale.ROOT, "  %-12s %6.2f ms   %s",
                name(SEQUENCE[i]), RESULTS[i], delta),
                i == 0 ? NamedTextColor.GRAY : NamedTextColor.GREEN));
        }
        s.sendMessage(Component.text("Measured on your own load; nothing was spawned or changed. "
            + "The tier is back to your configured setting.", NamedTextColor.GRAY));
    }

    private static String name(final EntityTiers.Level level) {
        return level.name().toLowerCase(Locale.ROOT);
    }
}
