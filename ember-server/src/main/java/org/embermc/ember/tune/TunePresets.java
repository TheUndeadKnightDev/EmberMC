package org.embermc.ember.tune;

import java.util.List;
import java.util.Map;
import org.embermc.ember.config.Preset;
import org.jspecify.annotations.NullMarked;

import static org.embermc.ember.config.Preset.BALANCED;
import static org.embermc.ember.config.Preset.EXTREME;
import static org.embermc.ember.config.Preset.PERFORMANCE;
import static org.embermc.ember.config.Preset.VANILLA;
import static org.embermc.ember.tune.Setting.File.BUKKIT;
import static org.embermc.ember.tune.Setting.File.PAPER_WORLD_DEFAULTS;
import static org.embermc.ember.tune.Setting.File.SPIGOT;

/**
 * What each preset sets in Paper's, Spigot's and Bukkit's own configuration.
 *
 * <p>This is the "Paper optimisation guide" as a table instead of a blog post:
 * every key is one Paper already ships, named exactly as it appears in the
 * generated file, with the upstream default as {@code VANILLA} so the revert is
 * always exact. Each entry states what a player or plugin could notice; the
 * ones marked {@code visible} are the trade-offs, and the command shows them
 * before it writes anything.
 *
 * <p>Values here are choices, not measurements. The one that has been measured
 * on EmberMC's own profiler is the redstone engine (block-ticks phase on a
 * 961-dust plane with a clock: 2.20 ms mean on vanilla, 0.20 ms on Alternate
 * Current). The rest are Paper's documented behaviour and the settled practice
 * of large survival servers; a preset is where to start, not where to stop.
 */
@NullMarked
public final class TunePresets {

    private TunePresets() {
    }

    public static final List<Setting> SETTINGS = List.of(

        // ---- redstone -------------------------------------------------------
        new Setting(PAPER_WORLD_DEFAULTS, "misc.redstone-implementation",
            Map.of(VANILLA, "VANILLA", BALANCED, "ALTERNATE_CURRENT"),
            "Update order can differ from vanilla in rare edge-case contraptions; Alternate Current aims for parity and documents the exceptions.",
            "Alternate Current recomputes redstone dust networks as a graph instead of block by block. Measured on this profiler: 11x cheaper block-ticks phase on a dust plane."),

        // ---- pathfinding and AI ---------------------------------------------
        new Setting(PAPER_WORLD_DEFAULTS, "misc.update-pathfinding-on-block-update",
            Map.of(VANILLA, true, PERFORMANCE, false),
            "A mob that was walking towards a spot may take a moment longer to notice a block placed in its path.",
            "Every block change otherwise re-paths every nearby mob immediately."),
        new Setting(PAPER_WORLD_DEFAULTS, "tick-rates.behavior.villager.validatenearbypoi",
            Map.of(VANILLA, -1, PERFORMANCE, 60, EXTREME, 120),
            "Villagers confirm their bed and workstation less often.",
            "POI validation is one of the most expensive villager behaviours."),
        new Setting(PAPER_WORLD_DEFAULTS, "tick-rates.sensor.villager.secondarypoisensor",
            Map.of(VANILLA, 40, PERFORMANCE, 80, EXTREME, 120),
            "Villagers look for a second workstation less often.",
            "Sensor runs a POI search."),

        // ---- entities -------------------------------------------------------
        new Setting(PAPER_WORLD_DEFAULTS, "entities.armor-stands.do-collision-entity-lookups",
            Map.of(VANILLA, true, BALANCED, false),
            "Armor stands are no longer pushed by entities walking into them.",
            "Each armor stand otherwise searches for colliding entities every tick."),
        new Setting(PAPER_WORLD_DEFAULTS, "entities.armor-stands.tick",
            Map.of(VANILLA, true, EXTREME, false),
            "Armor stands do not fall, get pushed by water, or take fire tick; decorative stands only.",
            "Stops ticking every armor stand."),
        new Setting(PAPER_WORLD_DEFAULTS, "collisions.max-entity-collisions",
            Map.of(VANILLA, 8, PERFORMANCE, 4, EXTREME, 2),
            "Entities in a dense crowd push fewer neighbours per tick.",
            "Collision checks scale with the square of the crowd."),
        new Setting(PAPER_WORLD_DEFAULTS, "chunks.entity-per-chunk-save-limit.arrow", Map.of(VANILLA, -1, BALANCED, 16), "Arrows beyond 16 in a chunk are dropped when it unloads.", "Unbounded projectile piles saved to disk."),
        new Setting(PAPER_WORLD_DEFAULTS, "chunks.entity-per-chunk-save-limit.ender_pearl", Map.of(VANILLA, -1, BALANCED, 16), "Ender pearls beyond 16 in a chunk are dropped when it unloads.", "Same as arrows."),
        new Setting(PAPER_WORLD_DEFAULTS, "chunks.entity-per-chunk-save-limit.experience_orb", Map.of(VANILLA, -1, BALANCED, 16), "XP orbs beyond 16 in a chunk are dropped when it unloads.", "Same as arrows."),
        new Setting(PAPER_WORLD_DEFAULTS, "chunks.entity-per-chunk-save-limit.snowball", Map.of(VANILLA, -1, BALANCED, 16), "Snowballs beyond 16 in a chunk are dropped when it unloads.", "Same as arrows."),
        new Setting(PAPER_WORLD_DEFAULTS, "chunks.entity-per-chunk-save-limit.fireball", Map.of(VANILLA, -1, BALANCED, 16), "Fireballs beyond 16 in a chunk are dropped when it unloads.", "Same as arrows."),
        new Setting(PAPER_WORLD_DEFAULTS, "chunks.entity-per-chunk-save-limit.small_fireball", Map.of(VANILLA, -1, BALANCED, 16), "Small fireballs beyond 16 in a chunk are dropped when it unloads.", "Same as arrows."),

        // ---- explosions and hoppers ----------------------------------------
        new Setting(PAPER_WORLD_DEFAULTS, "environment.optimize-explosions",
            Map.of(VANILLA, false, BALANCED, true),
            "Explosion damage uses a faster density calculation; results differ very slightly from vanilla.",
            "Vanilla explosion damage samples exposure per entity per block."),
        new Setting(PAPER_WORLD_DEFAULTS, "hopper.ignore-occluding-blocks",
            Map.of(VANILLA, false, PERFORMANCE, true),
            "Hoppers no longer pull from minecarts or containers inside full blocks.",
            "Skips an inventory search through occluding blocks every hopper tick."),
        new Setting(PAPER_WORLD_DEFAULTS, "hopper.disable-move-event",
            Map.of(VANILLA, false, EXTREME, true),
            "InventoryMoveItemEvent is not fired for hopper transfers. Refused automatically while any plugin listens for it.",
            "Firing the event for every hopper move is the single biggest hopper cost on plugin-heavy servers."),

        // ---- tick rates -----------------------------------------------------
        new Setting(PAPER_WORLD_DEFAULTS, "tick-rates.mob-spawner",
            Map.of(VANILLA, 1, PERFORMANCE, 2, EXTREME, 4),
            "Spawners run their spawn logic every N ticks; spawner farms produce a little slower.",
            "Spawner ticks do a spawn-condition search each run."),
        new Setting(PAPER_WORLD_DEFAULTS, "tick-rates.container-update",
            Map.of(VANILLA, 1, PERFORMANCE, 3),
            "Open container screens refresh every 3 ticks instead of every tick; barely perceptible.",
            "Container sync packets every tick for every open inventory."),
        new Setting(PAPER_WORLD_DEFAULTS, "tick-rates.grass-spread",
            Map.of(VANILLA, 1, PERFORMANCE, 4),
            "Grass and mycelium spread more slowly.",
            "Random-tick spread checks neighbours."),

        // ---- chunks ---------------------------------------------------------
        new Setting(PAPER_WORLD_DEFAULTS, "chunks.prevent-moving-into-unloaded-chunks",
            Map.of(VANILLA, false, BALANCED, true),
            "A player cannot walk into a chunk that has not loaded yet; they wait at the edge for a moment.",
            "Stops movement from forcing synchronous chunk loads."),
        new Setting(PAPER_WORLD_DEFAULTS, "chunks.max-auto-save-chunks-per-tick",
            Map.of(VANILLA, 24, PERFORMANCE, 8),
            "Auto-save spreads over more ticks.",
            "Smooths the save spike."),

        // ---- spigot: activation and tracking --------------------------------
        new Setting(SPIGOT, "world-settings.default.entity-activation-range.animals", Map.of(VANILLA, 32, PERFORMANCE, 24, EXTREME, 16), "Animals further than this from a player tick less.", "Activation range is the cheapest lever there is."),
        new Setting(SPIGOT, "world-settings.default.entity-activation-range.monsters", Map.of(VANILLA, 32, PERFORMANCE, 24, EXTREME, 20), "Monsters further than this from a player tick less.", "Same."),
        new Setting(SPIGOT, "world-settings.default.entity-activation-range.raiders", Map.of(VANILLA, 64, PERFORMANCE, 48, EXTREME, 32), "Raid mobs further than this tick less; raids still progress.", "Same."),
        new Setting(SPIGOT, "world-settings.default.entity-activation-range.misc", Map.of(VANILLA, 16, PERFORMANCE, 8, EXTREME, 4), "Items, XP and other misc entities far from players tick less.", "Same."),
        new Setting(SPIGOT, "world-settings.default.entity-activation-range.water", Map.of(VANILLA, 16, PERFORMANCE, 8), "Fish and squid far from players tick less.", "Same."),
        new Setting(SPIGOT, "world-settings.default.entity-activation-range.villagers", Map.of(VANILLA, 32, PERFORMANCE, 24, EXTREME, 16), "Villagers far from players tick less; trading halls still work when you are there.", "Same."),
        new Setting(SPIGOT, "world-settings.default.entity-activation-range.flying-monsters", Map.of(VANILLA, 32, PERFORMANCE, 24, EXTREME, 16), "Phantoms and ghasts far from players tick less.", "Same."),
        new Setting(SPIGOT, "world-settings.default.entity-activation-range.tick-inactive-villagers", Map.of(VANILLA, true, PERFORMANCE, false), "Villagers outside activation range stop restocking and pathing until a player is near.", "Inactive villagers otherwise still run their brain."),
        new Setting(SPIGOT, "world-settings.default.entity-tracking-range.animals", Map.of(VANILLA, 96, PERFORMANCE, 48), "Animals appear to clients from 48 blocks instead of 96.", "Tracking range decides how many entities every client is sent."),
        new Setting(SPIGOT, "world-settings.default.entity-tracking-range.monsters", Map.of(VANILLA, 96, PERFORMANCE, 64, EXTREME, 48), "Monsters appear to clients from closer.", "Same."),
        new Setting(SPIGOT, "world-settings.default.entity-tracking-range.misc", Map.of(VANILLA, 96, PERFORMANCE, 32), "Items and other misc entities appear to clients from closer.", "Same."),
        new Setting(SPIGOT, "world-settings.default.mob-spawn-range", Map.of(VANILLA, 8, PERFORMANCE, 6, EXTREME, 4), "Mobs spawn within fewer chunks of each player; total mob count is governed by spawn limits either way.", "Smaller spawn area, fewer spawn attempts."),
        new Setting(SPIGOT, "world-settings.default.merge-radius.item", Map.of(VANILLA, 0.5, PERFORMANCE, 3.5), "Dropped items merge from further apart; piles look tidier.", "Fewer item entities."),
        new Setting(SPIGOT, "world-settings.default.merge-radius.exp", Map.of(VANILLA, -1.0, PERFORMANCE, 4.0, EXTREME, 6.0), "XP orbs merge from further apart.", "Fewer orb entities; mending still works."),
        new Setting(SPIGOT, "world-settings.default.arrow-despawn-rate", Map.of(VANILLA, 1200, PERFORMANCE, 300), "Stuck arrows vanish after 15 s instead of 60 s.", "Fewer lingering projectiles."),
        new Setting(SPIGOT, "world-settings.default.nerf-spawner-mobs", Map.of(VANILLA, false, EXTREME, true), "Mobs from spawners have no AI; spawner farms still work, mob grinders that rely on AI do not.", "AI is the cost."),
        new Setting(SPIGOT, "world-settings.default.ticks-per.hopper-check", Map.of(VANILLA, 1, PERFORMANCE, 4, EXTREME, 8), "An empty hopper looks for items every 4 ticks instead of every tick; throughput of a moving hopper line is unchanged.", "Idle hoppers otherwise search every tick."),

        // ---- bukkit: spawn limits -------------------------------------------
        new Setting(BUKKIT, "spawn-limits.monsters", Map.of(VANILLA, 70, PERFORMANCE, 50, EXTREME, 40), "Fewer hostile mobs per player.", "Mob count is the entity-tick budget."),
        new Setting(BUKKIT, "spawn-limits.animals", Map.of(VANILLA, 10, PERFORMANCE, 8), "Fewer passive mobs per player.", "Same."),
        new Setting(BUKKIT, "spawn-limits.water-animals", Map.of(VANILLA, 5, PERFORMANCE, 3), "Fewer water mobs per player.", "Same."),
        new Setting(BUKKIT, "spawn-limits.ambient", Map.of(VANILLA, 15, PERFORMANCE, 8), "Fewer bats.", "Same."),
        new Setting(BUKKIT, "ticks-per.monster-spawns", Map.of(VANILLA, 1, PERFORMANCE, 2, EXTREME, 4), "Hostile spawn attempts every 2 ticks; caps are reached a little slower.", "Spawn attempts are a chunk scan.")
    );
}
