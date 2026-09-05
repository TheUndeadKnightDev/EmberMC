package org.embermc.ember.profiler;

import it.unimi.dsi.fastutil.longs.LongArrays;

/**
 * A fixed ring of per-tick durations with statistics computed only when asked.
 *
 * <p>Recording is one array store and one increment; nothing is sorted, summed
 * or allocated on the tick. Percentiles are computed on demand by copying the
 * window and sorting the copy, which is what a command can afford and a tick
 * cannot. Main-thread only for writes; readers on the main thread see a
 * consistent window.
 */
public final class TimeRing {

    /** Sixty seconds at twenty ticks per second. */
    public static final int CAPACITY = 1200;

    private final long[] values = new long[CAPACITY];
    private int head;
    private int size;

    public void record(final long nanos) {
        this.values[this.head] = nanos;
        this.head = (this.head + 1) % CAPACITY;
        if (this.size < CAPACITY) {
            this.size++;
        }
    }

    public int size() {
        return this.size;
    }

    /** Statistics over the most recent {@code ticks} samples (or fewer if not yet filled). */
    public Stats stats(final int ticks) {
        final int n = Math.min(ticks, this.size);
        if (n == 0) {
            return Stats.EMPTY;
        }
        final long[] window = new long[n];
        int idx = (this.head - n + CAPACITY) % CAPACITY;
        long sum = 0;
        long max = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            final long v = this.values[idx];
            window[i] = v;
            sum += v;
            if (v > max) {
                max = v;
            }
            idx = (idx + 1) % CAPACITY;
        }
        LongArrays.quickSort(window);
        return new Stats(n, sum / (double) n, window[(int) Math.min(n - 1, Math.floor(n * 0.95))],
            window[(int) Math.min(n - 1, Math.floor(n * 0.99))], max);
    }

    /** Sum over the most recent {@code ticks} samples, for cheap averages without a sort. */
    public long sum(final int ticks) {
        final int n = Math.min(ticks, this.size);
        long sum = 0;
        int idx = (this.head - n + CAPACITY) % CAPACITY;
        for (int i = 0; i < n; i++) {
            sum += this.values[idx];
            idx = (idx + 1) % CAPACITY;
        }
        return sum;
    }

    public record Stats(int samples, double meanNanos, long p95Nanos, long p99Nanos, long maxNanos) {
        public static final Stats EMPTY = new Stats(0, 0, 0, 0, 0);

        public double meanMs() {
            return this.meanNanos / 1_000_000.0;
        }

        public double p95Ms() {
            return this.p95Nanos / 1_000_000.0;
        }

        public double p99Ms() {
            return this.p99Nanos / 1_000_000.0;
        }

        public double maxMs() {
            return this.maxNanos / 1_000_000.0;
        }
    }
}
