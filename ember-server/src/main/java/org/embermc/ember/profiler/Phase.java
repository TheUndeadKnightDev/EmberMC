package org.embermc.ember.profiler;

/**
 * The slices of a server tick EmberMC times.
 *
 * <p>Chosen to cover the tick without overlapping: every nanosecond of the tick
 * belongs to exactly one phase or to {@link #OTHER}, so the breakdown in a
 * report always adds up to the tick. World phases are recorded both against the
 * world they happened in and against the tick as a whole.
 */
public enum Phase {
    /** The whole tick, first {@code nanoTime} to last. */
    TICK("tick"),
    /** Bukkit scheduler main-thread heartbeat: every plugin's sync tasks. */
    SCHEDULER("scheduler"),
    /** Block and fluid scheduled ticks. */
    BLOCK_TICKS("block ticks"),
    /** Chunk source tick: loading, unloading, random ticks, mob spawning. */
    CHUNKS("chunks"),
    /** Queued block events (pistons, note blocks, etc.). */
    BLOCK_EVENTS("block events"),
    /** The entity tick loop, including AI and pathfinding. */
    ENTITIES("entities"),
    /** Block entity ticks: hoppers, furnaces, spawners, and the rest. */
    BLOCK_ENTITIES("block entities"),
    /** Network tick: inbound packets handled and outbound flushed for every connection, including commands players ran. */
    CONNECTIONS("connections"),
    /** Commands typed at the console, executed on the main thread after the network tick. */
    COMMANDS("console commands"),
    /** Player, level and chunk saving at the end of the tick: incremental every tick, a full save every autosave period. */
    AUTOSAVE("autosave"),
    /** Everything in the tick not covered above. Derived, never timed directly. */
    OTHER("other");

    public static final Phase[] VALUES = values();
    public static final int COUNT = VALUES.length;

    /** Phases that are measured per world as well as per tick. */
    public static final Phase[] WORLD_PHASES = {BLOCK_TICKS, CHUNKS, BLOCK_EVENTS, ENTITIES, BLOCK_ENTITIES};

    private final String label;

    Phase(final String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }
}
