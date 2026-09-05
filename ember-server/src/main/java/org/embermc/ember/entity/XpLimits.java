package org.embermc.ember.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;
import org.embermc.ember.config.EmberConfigurations;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A live ceiling on how many experience-orb entities a single chunk may hold,
 * without throwing away any experience.
 *
 * <p>Paper already merges nearby orbs pairwise up to a value cap
 * ({@code experience-merge-max-value}), but it does not bound the <em>count</em>
 * of orbs in a loaded chunk. A big grinder or an XP-dupe can still pile up
 * thousands of orb entities faster than the pairwise merge clears them, and each
 * one is an entity that ticks and is tracked to every nearby player.
 *
 * <p>This caps the count per loaded chunk. When a chunk is over the limit, the
 * overflow orbs' experience is folded into the orbs that remain and the now-empty
 * overflow entities are removed, so the total experience on the ground is
 * unchanged - only the entity count drops. That is the difference from
 * {@link ItemLimits}, which discards: experience is a player's, so it is merged,
 * never deleted.
 *
 * <p>Off by default. Removals fire Bukkit's {@code EntityRemoveEvent} and are
 * counted for {@code /ember security}. Cost: one pass over each world's orb
 * entities every {@code sweep-seconds}, bucketed by chunk; nothing per tick.
 * Main thread.
 */
@NullMarked
public final class XpLimits {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmberMC");
    private static long merged;
    private static long lastSweepTick = Long.MIN_VALUE;

    private XpLimits() {
    }

    public static void tick(final long tickCount) {
        if (!EmberConfigurations.isInitialized()) {
            return;
        }
        final var cfg = EmberConfigurations.global().entities.xpLimits;
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
        int collapsed = 0;
        for (final World world : org.bukkit.Bukkit.getWorlds()) {
            final Long2ObjectMap<List<ExperienceOrb>> byChunk = new Long2ObjectOpenHashMap<>();
            for (final ExperienceOrb orb : world.getEntitiesByClass(ExperienceOrb.class)) {
                final long key = chunkKey(orb.getLocation().getBlockX() >> 4, orb.getLocation().getBlockZ() >> 4);
                byChunk.computeIfAbsent(key, k -> new ArrayList<>()).add(orb);
            }
            for (final List<ExperienceOrb> orbs : byChunk.values()) {
                if (orbs.size() <= cap) {
                    continue;
                }
                // Youngest kept (a player's freshest orbs survive intact); the
                // oldest overflow is folded into the youngest survivor so no XP is lost.
                orbs.sort((a, b) -> Integer.compare(a.getTicksLived(), b.getTicksLived()));
                final int toRemove = ItemLimits.overflow(orbs.size(), cap);
                if (toRemove <= 0) {
                    continue;
                }
                final ExperienceOrb survivor = orbs.get(0);
                long folded = survivor.getExperience();
                for (int i = orbs.size() - toRemove; i < orbs.size(); i++) {
                    folded += orbs.get(i).getExperience();
                    orbs.get(i).remove();
                    collapsed++;
                }
                survivor.setExperience((int) Math.min(Integer.MAX_VALUE, folded));
            }
        }
        if (collapsed > 0) {
            merged += collapsed;
            LOGGER.info("XP limits: folded {} overflow orb(s) above {}/chunk into survivors this sweep ({} total since start); no experience lost.",
                collapsed, cap, merged);
        }
    }

    private static long chunkKey(final int x, final int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    /** Orbs collapsed (merged away) since start, for the metric and /ember security. */
    public static long merged() {
        return merged;
    }
}
