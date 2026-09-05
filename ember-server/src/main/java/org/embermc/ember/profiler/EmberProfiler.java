package org.embermc.ember.profiler;

import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.NullMarked;

/**
 * Where the tick is timed.
 *
 * <p>Two layers, priced differently on purpose.
 *
 * <p><b>Phase timing is always on.</b> Each hook is one {@code System.nanoTime()}
 * and one addition into a {@code long[]}; a tick with three worlds makes about
 * forty of them, around a microsecond in total. That is cheap enough to leave
 * on, and leaving it on is what lets the spike watchdog explain a bad tick that
 * nobody was watching for.
 *
 * <p><b>Plugin attribution is opt-in.</b> Timing every event handler and every
 * scheduler task means two {@code nanoTime} calls per call, and a busy server
 * fires thousands of events a tick. That can cost real fractions of a
 * millisecond, so it runs only during a session started with
 * {@code /ember profiler start}, guarded by one volatile read per call site.
 *
 * <p>Everything here is main-thread only; the tick is single-threaded and so is
 * this. Nothing allocates per tick.
 */
@NullMarked
public final class EmberProfiler {

    /** Read at every attribution hook. Volatile so a start/stop from a command thread is seen promptly. */
    public static volatile boolean attributing;

    private static final long[] CURRENT = new long[Phase.COUNT];
    private static final TimeRing[] RINGS = new TimeRing[Phase.COUNT];
    private static long tickStartNanos;
    private static long tickCount;
    private static long sessionStartTick;
    private static long sessionEndTick;

    static {
        for (int i = 0; i < Phase.COUNT; i++) {
            RINGS[i] = new TimeRing();
        }
    }

    private EmberProfiler() {
    }

    /* ---- tick hooks ---------------------------------------------------- */

    public static void tickStart() {
        tickStartNanos = System.nanoTime();
        if (attributing) {
            PluginTimes.tickReset();
        }
    }

    /** Take a start mark. Pair with {@link #end} or {@link #worldEnd}. */
    public static long start() {
        return System.nanoTime();
    }

    public static void end(final Phase phase, final long start) {
        CURRENT[phase.ordinal()] += System.nanoTime() - start;
    }

    /** A phase that belongs to one world: counted against the world and the tick. */
    public static void worldEnd(final ServerLevel level, final Phase phase, final long start) {
        final long d = System.nanoTime() - start;
        CURRENT[phase.ordinal()] += d;
        level.emberTimes.current[phase.ordinal()] += d;
    }

    /** The whole of one world's tick. Files the world's tick and clears it. */
    public static void worldTickEnd(final ServerLevel level, final long start) {
        level.emberTimes.current[Phase.TICK.ordinal()] = System.nanoTime() - start;
        level.emberTimes.endTick();
    }

    public static void tickEnd() {
        final long tick = System.nanoTime() - tickStartNanos;
        CURRENT[Phase.TICK.ordinal()] = tick;
        long covered = 0;
        for (final Phase p : Phase.VALUES) {
            if (p != Phase.TICK && p != Phase.OTHER) {
                covered += CURRENT[p.ordinal()];
            }
        }
        CURRENT[Phase.OTHER.ordinal()] = Math.max(0, tick - covered);

        SpikeWatchdog.onTick(tick, CURRENT);
        org.embermc.ember.entity.EntityTiers.endTick();
        org.embermc.ember.adaptive.AdaptiveRuntime.tick(tickCount);
        org.embermc.ember.entity.ItemLimits.tick(tickCount);
        org.embermc.ember.memory.IdleMemory.tick(tickCount);
        org.embermc.ember.chunk.ChunkInsights.tick(tickCount);
        org.embermc.ember.net.NetSampler.tick(tickCount);

        for (int i = 0; i < Phase.COUNT; i++) {
            RINGS[i].record(CURRENT[i]);
            CURRENT[i] = 0;
        }
        tickCount++;

        if (attributing && sessionEndTick > 0 && tickCount >= sessionEndTick) {
            stopSession();
        }
    }

    /* ---- sessions ------------------------------------------------------ */

    /** Starts plugin attribution for {@code seconds} (0 = until stopped). */
    public static void startSession(final int seconds) {
        PluginTimes.reset();
        sessionStartTick = tickCount;
        sessionEndTick = seconds > 0 ? tickCount + seconds * 20L : 0;
        attributing = true;
    }

    public static void stopSession() {
        attributing = false;
        sessionEndTick = 0;
    }

    /** Ticks the current or last session has covered. */
    public static long sessionTicks() {
        return Math.max(1, tickCount - sessionStartTick);
    }

    /* ---- reading ------------------------------------------------------- */

    public static TimeRing ring(final Phase phase) {
        return RINGS[phase.ordinal()];
    }

    public static long tickCount() {
        return tickCount;
    }
}
