package org.embermc.ember.chunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.embermc.ember.config.EmberConfigurations;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes chunk retention visible.
 *
 * <p>Paper's chunk system already handles the performance of loading, generating
 * and saving chunks: saves run off the main thread, each player has a bounded
 * number of concurrent loads and generates, and unloads are delayed so a player
 * pacing a border does not thrash. What it does not surface is <b>why</b> a chunk
 * is still loaded. The common answer on a heavy, never-shrinking world is a
 * plugin holding chunk tickets (a claim protector, a spawner manager, a shop
 * region) or an admin force-load, and there is no convenient view of that.
 *
 * <p>This reads the counts Bukkit already exposes - loaded, force-loaded, and the
 * per-plugin ticket map - plus the server's own ticking-chunk count, and it
 * warns (once per cooldown) when one world's plugin-held chunks pass a threshold,
 * naming the plugins responsible. It never unloads anything: a chunk a plugin
 * asked to keep is the plugin's call, not ours.
 */
@NullMarked
public final class ChunkInsights {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmberMC");
    private static long lastWarnTick = Long.MIN_VALUE;

    private ChunkInsights() {
    }

    /** One world's chunk picture, all from cheap already-tracked counts. */
    public record WorldChunks(String world, int loaded, int ticking, int forceLoaded, int pluginHeld) {
    }

    public static WorldChunks of(final World world) {
        int ticking = 0;
        try {
            ticking = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle()
                .getChunkSource().getFullChunksCount();
        } catch (final Throwable ignored) {
            // Only used for display; a mapping change must never break the read-out.
        }
        final int force = world.getForceLoadedChunks().size();
        return new WorldChunks(world.getName(), world.getChunkCount(), ticking, force,
            pluginHeld(world));
    }

    private static int pluginHeld(final World world) {
        int held = 0;
        for (final var v : world.getPluginChunkTickets().values()) {
            held += v.size();
        }
        return held;
    }

    /** Plugins holding chunk tickets in a world, most chunks first. */
    public static List<Map.Entry<Plugin, Integer>> topHolders(final World world, final int limit) {
        final List<Map.Entry<Plugin, Integer>> holders = new ArrayList<>();
        for (final var e : world.getPluginChunkTickets().entrySet()) {
            holders.add(Map.entry(e.getKey(), e.getValue().size()));
        }
        holders.sort(Comparator.comparingInt((Map.Entry<Plugin, Integer> e) -> e.getValue()).reversed());
        return holders.size() > limit ? holders.subList(0, limit) : holders;
    }

    /** Total plugin-ticket-held chunks across all worlds, for the metric. */
    public static long pluginHeldTotal() {
        long total = 0;
        for (final World w : Bukkit.getWorlds()) {
            total += pluginHeld(w);
        }
        return total;
    }

    /** Total admin force-loaded chunks across all worlds, for the metric. */
    public static long forceLoadedTotal() {
        long total = 0;
        for (final World w : Bukkit.getWorlds()) {
            total += w.getForceLoadedChunks().size();
        }
        return total;
    }

    /**
     * Pure decision: should a retention warning fire now? Split out for testing.
     *
     * @param worstPluginHeld    the highest plugin-held count across worlds
     * @param threshold          the configured warn threshold (0 disables)
     * @param ticksSinceLastWarn ticks since the last warning
     * @param cooldownTicks      minimum ticks between warnings
     */
    public static boolean shouldWarn(final int worstPluginHeld, final int threshold,
                                     final long ticksSinceLastWarn, final long cooldownTicks) {
        if (threshold <= 0) {
            return false;
        }
        return worstPluginHeld > threshold && ticksSinceLastWarn >= cooldownTicks;
    }

    /** Called on a throttle from the profiler; cheap unless it actually warns. */
    public static void tick(final long tickCount) {
        if (!EmberConfigurations.isInitialized()) {
            return;
        }
        final var cfg = EmberConfigurations.global().chunks;
        if (!cfg.retentionDiagnostics || cfg.warnThreshold <= 0) {
            return;
        }
        // Only look every 20 ticks; the ticket map is cheap but not free.
        if (tickCount % 20L != 0L) {
            return;
        }
        World worst = null;
        int worstHeld = 0;
        for (final World w : Bukkit.getWorlds()) {
            final int held = pluginHeld(w);
            if (held > worstHeld) {
                worstHeld = held;
                worst = w;
            }
        }
        final long cooldownTicks = cfg.warnCooldownSeconds * 20L;
        if (worst != null && shouldWarn(worstHeld, cfg.warnThreshold,
                tickCount - lastWarnTick, cooldownTicks)) {
            lastWarnTick = tickCount;
            final StringBuilder who = new StringBuilder();
            for (final var e : topHolders(worst, 3)) {
                if (who.length() > 0) {
                    who.append(", ");
                }
                who.append(e.getKey().getName()).append(' ').append(e.getValue());
            }
            LOGGER.warn("Chunk retention: world '{}' has {} chunks held loaded by plugins (threshold {}). "
                    + "Top: {}. See /ember chunks. EmberMC does not unload these; this is a heads-up.",
                worst.getName(), worstHeld, cfg.warnThreshold, who.toString());
        }
    }
}
