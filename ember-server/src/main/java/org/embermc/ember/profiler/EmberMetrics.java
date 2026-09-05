package org.embermc.ember.profiler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

/**
 * Named, read-on-demand gauges.
 *
 * <p>A metric here is a supplier, not a stored value: nothing is sampled or
 * pushed on the tick, so the registry costs nothing until something reads it.
 * {@code /ember metrics} reads it; a Prometheus-style endpoint (opt-in, later
 * milestone) will read the same suppliers. Names follow the {@code ember_}
 * prefix so they never collide with anything a plugin exports.
 */
@NullMarked
public final class EmberMetrics {

    private static final Map<String, DoubleSupplier> GAUGES = new LinkedHashMap<>();

    static {
        register("ember_tps_1m", () -> Math.min(20.0, Bukkit.getTPS()[0]));
        register("ember_mspt_5s", Bukkit::getAverageTickTime);
        register("ember_players", () -> Bukkit.getOnlinePlayers().size());
        register("ember_entities", () -> sum(World::getEntityCount));
        register("ember_block_entities", () -> sum(World::getTileEntityCount));
        register("ember_chunks_loaded", () -> sum(World::getChunkCount));
        register("ember_profiler_attributing", () -> EmberProfiler.attributing ? 1 : 0);
        register("ember_packets_blocked", org.embermc.ember.security.PacketGuard::totalBlocked);
        register("ember_adaptive_level", () -> org.embermc.ember.adaptive.AdaptiveRuntime.level().ordinal());
        register("ember_adaptive_changes", org.embermc.ember.adaptive.AdaptiveRuntime::changes);
        register("ember_entities_full_tick", org.embermc.ember.entity.EntityTiers::fullLastTick);
        register("ember_entities_reduced_tick", org.embermc.ember.entity.EntityTiers::reducedLastTick);
        for (final Phase p : Phase.VALUES) {
            register("ember_tick_" + p.name().toLowerCase(java.util.Locale.ROOT) + "_ms_mean_5s",
                () -> EmberProfiler.ring(p).stats(100).meanMs());
        }
        register("ember_tick_p99_ms_1m", () -> EmberProfiler.ring(Phase.TICK).stats(TimeRing.CAPACITY).p99Ms());
    }

    private EmberMetrics() {
    }

    public static void register(final String name, final DoubleSupplier gauge) {
        GAUGES.put(name, gauge);
    }

    /** Current value of every gauge, in registration order. */
    public static Map<String, Double> snapshot() {
        final Map<String, Double> out = new LinkedHashMap<>();
        for (final var e : GAUGES.entrySet()) {
            out.put(e.getKey(), e.getValue().getAsDouble());
        }
        return out;
    }

    private static double sum(final java.util.function.ToIntFunction<World> f) {
        int n = 0;
        for (final World w : Bukkit.getWorlds()) {
            n += f.applyAsInt(w);
        }
        return n;
    }
}
