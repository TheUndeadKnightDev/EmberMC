package org.embermc.ember.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.embermc.ember.config.EmberConfigurations;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A live ceiling on how many dropped-item entities a single chunk may hold.
 *
 * <p>Paper limits items only at save time ({@code entity-per-chunk-save-limit}):
 * a chunk can hold tens of thousands of items while it is loaded, and only
 * sheds them when it unloads. That loaded pile is what an item-dupe machine or
 * a mob grinder gone wrong turns into lag, and it is what an economy server
 * bleeds ticks to. This caps it while the chunk is <em>loaded</em>: once a
 * chunk is over the limit, the oldest excess items are removed on the next
 * sweep, so a flood is bounded in seconds instead of persisting until unload.
 *
 * <p>Off by default, because removing items changes gameplay; an administrator
 * turns it on with a generous cap as an anti-abuse backstop, not a farm nerf.
 * Removals fire Bukkit's {@code EntityRemoveEvent} with the {@code OUT_OF_WORLD}
 * cause and are counted for {@code /ember security}. The oldest items go first,
 * so what a player just dropped survives and only long-settled overflow is
 * culled.
 *
 * <p>Cost: one pass over each world's item entities every {@code sweep-seconds}
 * (default 10), bucketed by chunk; nothing per tick, nothing per item outside
 * the sweep. Main thread.
 */
@NullMarked
public final class ItemLimits {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmberMC");
    private static long removed;
    private static long lastSweepTick = Long.MIN_VALUE;

    private ItemLimits() {
    }

    /** Called every tick; sweeps at most once per configured interval. */
    public static void tick(final long tickCount) {
        if (!EmberConfigurations.isInitialized()) {
            return;
        }
        final var cfg = EmberConfigurations.global().entities.itemLimits;
        if (!cfg.enabled || cfg.maxPerChunk <= 0) {
            return;
        }
        final long everyTicks = Math.max(20L, cfg.sweepSeconds * 20L);
        if (tickCount - lastSweepTick < everyTicks) {
            return;
        }
        lastSweepTick = tickCount;
        sweep(cfg.maxPerChunk);
    }

    private static void sweep(final int cap) {
        int culled = 0;
        for (final World world : org.bukkit.Bukkit.getWorlds()) {
            final Long2ObjectMap<List<Item>> byChunk = new Long2ObjectOpenHashMap<>();
            for (final Item item : world.getEntitiesByClass(Item.class)) {
                final long key = chunkKey(item.getLocation().getBlockX() >> 4, item.getLocation().getBlockZ() >> 4);
                byChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
            }
            for (final List<Item> items : byChunk.values()) {
                if (items.size() <= cap) {
                    continue;
                }
                // Oldest first: a longer-lived item is more likely settled overflow,
                // and a player's fresh drop is the youngest, so it is the last to go.
                items.sort((a, b) -> Integer.compare(b.getTicksLived(), a.getTicksLived()));
                final int toRemove = overflow(items.size(), cap);
                for (int i = 0; i < toRemove; i++) {
                    items.get(i).remove();
                    culled++;
                }
            }
        }
        if (culled > 0) {
            removed += culled;
            LOGGER.info("Item limits: removed {} overflow item(s) above {}/chunk this sweep ({} total since start).", culled, cap, removed);
        }
    }

    /** How many items to remove from a chunk holding {@code size} of them, capped at {@code cap}. Pure; unit-tested. */
    public static int overflow(final int size, final int cap) {
        if (cap <= 0) {
            return 0;
        }
        return Math.max(0, size - cap);
    }

    private static long chunkKey(final int x, final int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public static long removed() {
        return removed;
    }
}
