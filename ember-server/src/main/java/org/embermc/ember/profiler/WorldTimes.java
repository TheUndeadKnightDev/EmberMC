package org.embermc.ember.profiler;

/**
 * One world's share of the tick: what its phases cost this tick, and the ring
 * of what the whole world tick has cost recently.
 *
 * <p>Held as a field on {@code ServerLevel} so recording is a field access, not
 * a map lookup. Main-thread only.
 */
public final class WorldTimes {

    /** Nanos per phase for the tick in progress. {@link Phase#TICK} is the whole world tick. */
    public final long[] current = new long[Phase.COUNT];

    private final TimeRing[] rings = new TimeRing[Phase.COUNT];

    public WorldTimes() {
        for (int i = 0; i < Phase.COUNT; i++) {
            this.rings[i] = new TimeRing();
        }
    }

    public TimeRing ring(final Phase phase) {
        return this.rings[phase.ordinal()];
    }

    /** Called once per tick after the world has ticked: files this tick and clears the slate. */
    void endTick() {
        for (int i = 0; i < Phase.COUNT; i++) {
            this.rings[i].record(this.current[i]);
            this.current[i] = 0;
        }
    }
}
