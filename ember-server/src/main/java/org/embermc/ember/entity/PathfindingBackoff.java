package org.embermc.ember.entity;

import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.NullMarked;

/**
 * Generalises Paper's failed-pathfind backoff to every pathfind, not only the
 * follow-an-entity case.
 *
 * <p>Paper already stops a mob hammering the A* pathfinder when it repeatedly
 * fails to reach an <b>entity</b> target ({@code PathNavigation.moveTo(Entity)}):
 * after ten failures it skips pathfinding for forty ticks. But a mob failing to
 * reach a <b>position</b> - a wander target, a work site, a spot it is fleeing
 * toward - gets no such backoff, so it re-runs a full A* search every recompute
 * for as long as it stays stuck. A wall of mobs against a base perimeter, none
 * of them able to reach the players inside, is the case that hurts: each one
 * pays a real pathfind on its throttle, forever.
 *
 * <p>This applies the same proven, bounded rule at the one place every pathfind
 * funnels through: once a navigation has failed to reach the <i>same</i> target
 * {@code failuresBeforeBackoff} times in a row, further searches to that target
 * are skipped for {@code backoffTicks} ticks. A different target, or the target
 * becoming reachable, resets it immediately, so a mob is never stuck longer than
 * the backoff window (the same order as Paper's existing forty ticks) and only
 * ever on a target it has already proven it cannot reach.
 *
 * <p>The decision is pure and unit-tested; the caller ({@code PathNavigation})
 * holds the tiny per-navigation state and this class only decides and counts.
 */
@NullMarked
public final class PathfindingBackoff {

    private static final AtomicLong SKIPPED = new AtomicLong();

    private PathfindingBackoff() {
    }

    /**
     * Whether a pathfind to a target that has already failed {@code failures}
     * times in a row should be skipped.
     *
     * @param failures            consecutive failed searches to this same target
     * @param ticksSinceLastFail  ticks since the most recent failure
     * @param failuresBeforeBackoff how many failures arm the backoff
     * @param backoffTicks        how long the backoff lasts once armed
     */
    public static boolean shouldSkip(final int failures, final long ticksSinceLastFail,
                                     final int failuresBeforeBackoff, final int backoffTicks) {
        if (failuresBeforeBackoff <= 0 || backoffTicks <= 0) {
            return false;
        }
        return failures >= failuresBeforeBackoff
            && ticksSinceLastFail >= 0
            && ticksSinceLastFail < backoffTicks;
    }

    /** Count one skipped (avoided) A* search, for {@code ember_pathfinds_skipped}. */
    public static void recordSkip() {
        SKIPPED.incrementAndGet();
    }

    /** Total pathfinds skipped since start. */
    public static long skipped() {
        return SKIPPED.get();
    }
}
