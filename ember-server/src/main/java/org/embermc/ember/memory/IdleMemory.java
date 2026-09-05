package org.embermc.ember.memory;

import org.bukkit.Bukkit;
import org.embermc.ember.config.EmberConfigurations;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns idle heap to the operating system.
 *
 * <p>A running server holds far more committed heap than it is using, and with
 * no players online it holds it for no reason. When the server has been empty
 * for a while this asks the JVM for one collection so the garbage collector can
 * hand unused regions back to the OS, shrinking the process's real footprint.
 *
 * <p>It fires only when <b>no players are online</b>, so the pause has nobody to
 * affect, and at most once per idle stretch: once it has trimmed it waits until
 * a player has come and gone again before it will trim a second time. When
 * anyone is online it does nothing at all.
 *
 * <p>This only reduces the footprint the OS sees if the JVM is allowed to
 * uncommit heap. The common performance flag set (Xms equal to Xmx, plus
 * {@code AlwaysPreTouch}) deliberately pins the whole heap and will not give it
 * back; the trim then costs nothing and returns nothing. See
 * {@code docs/optimisations/idle-memory.md} for the flags that let it work.
 */
@NullMarked
public final class IdleMemory {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmberMC");
    private static long emptySinceTick = -1;
    private static boolean trimmedThisIdle;
    private static long lastTrimFreedMb;

    private IdleMemory() {
    }

    /** Called once per tick from the profiler. Cheap: a player count and two comparisons. */
    public static void tick(final long tickCount) {
        if (!EmberConfigurations.isInitialized()) {
            return;
        }
        final var cfg = EmberConfigurations.global().memory.idleTrim;
        if (!cfg.enabled) {
            return;
        }
        final boolean empty = Bukkit.getOnlinePlayers().isEmpty();
        if (!empty) {
            // Someone is on: reset, and re-arm so the next empty stretch can trim again.
            emptySinceTick = -1;
            trimmedThisIdle = false;
            return;
        }
        if (emptySinceTick < 0) {
            emptySinceTick = tickCount;
            return;
        }
        if (trimmedThisIdle) {
            return;
        }
        final long idleTicks = tickCount - emptySinceTick;
        if (dueForTrim(idleTicks, cfg.afterMinutes)) {
            trim();
            trimmedThisIdle = true;
        }
    }

    /**
     * Pure decision, split out so it can be tested without a running server: an
     * idle stretch of {@code idleTicks} (at 20 ticks per second) is due for a
     * trim once it has lasted at least {@code afterMinutes}.
     */
    public static boolean dueForTrim(final long idleTicks, final int afterMinutes) {
        return idleTicks >= (long) afterMinutes * 60L * 20L;
    }

    private static void trim() {
        final Runtime rt = Runtime.getRuntime();
        final long usedBefore = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        final long committedBefore = rt.totalMemory() / (1024 * 1024);
        // One collection. With G1 and uncommit-capable flags this lets unused
        // regions return to the OS. Only ever runs with zero players, so the
        // stop-the-world portion affects no one.
        System.gc();
        final long usedAfter = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        final long committedAfter = rt.totalMemory() / (1024 * 1024);
        lastTrimFreedMb = Math.max(0, committedBefore - committedAfter);
        LOGGER.info("Idle memory trim (server empty): used heap {} MB -> {} MB, committed {} MB -> {} MB ({} MB returned to OS).",
            usedBefore, usedAfter, committedBefore, committedAfter, lastTrimFreedMb);
    }

    /** MB the last trim returned to the OS (0 if the JVM flags pin the heap). */
    public static long lastTrimFreedMb() {
        return lastTrimFreedMb;
    }
}
