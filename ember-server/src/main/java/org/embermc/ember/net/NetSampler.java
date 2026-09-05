package org.embermc.ember.net;

import java.util.concurrent.atomic.AtomicLongArray;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A deliberate, time-boxed sample of outbound (clientbound) network traffic, so
 * an administrator can see what is filling the pipe: the entity tracker's move
 * and metadata packets, chunk data, sounds, and so on.
 *
 * <p>It is off until {@code /ember netstat start} and stops itself after the
 * window. When it is off, the only thing on the send path is a single volatile
 * read that returns immediately, so it costs nothing in normal running; the
 * packet size estimate and the classification happen only while a sample is
 * active. It counts and measures, and changes nothing that is sent.
 *
 * <p>This is visibility, not deduplication. Vanilla and Paper already send only
 * changed entity data (dirty tracking), so there is no redundant metadata to
 * remove; what was missing was a way to see the volume and its shape.
 */
@NullMarked
public final class NetSampler {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmberMC");

    private static volatile boolean sampling;
    private static final AtomicLongArray COUNTS = new AtomicLongArray(OutboundCategory.VALUES.length);
    private static final AtomicLongArray BYTES = new AtomicLongArray(OutboundCategory.VALUES.length);
    private static long startTick;
    private static long endTick;
    private static double lastWindowSeconds;

    private NetSampler() {
    }

    /** True while a sample is running. The send path checks only this. */
    public static boolean isSampling() {
        return sampling;
    }

    /**
     * Record one outbound packet. Called from the connection send path, guarded
     * by {@link #isSampling()} so it is only reached during a sample.
     */
    public static void record(final String simpleName, final int bytes) {
        final int i = OutboundCategory.of(simpleName).ordinal();
        COUNTS.incrementAndGet(i);
        if (bytes > 0) {
            BYTES.addAndGet(i, bytes);
        }
    }

    /** Begin a sample for {@code seconds}, clearing previous counts. */
    public static void start(final long tickCount, final int seconds) {
        for (int i = 0; i < COUNTS.length(); i++) {
            COUNTS.set(i, 0);
            BYTES.set(i, 0);
        }
        startTick = tickCount;
        endTick = tickCount + (long) seconds * 20L;
        lastWindowSeconds = seconds;
        sampling = true;
        LOGGER.info("Network sample started for {}s. /ember netstat to read, /ember netstat stop to end early.", seconds);
    }

    /** End the sample now. Counts remain readable until the next start. */
    public static void stop(final long tickCount) {
        if (sampling) {
            sampling = false;
            lastWindowSeconds = Math.max(0.05, (tickCount - startTick) / 20.0);
            LOGGER.info("Network sample stopped after {}s.", String.format("%.1f", lastWindowSeconds));
        }
    }

    /** Called each tick from the profiler; ends the window when it elapses. */
    public static void tick(final long tickCount) {
        if (sampling && tickCount >= endTick) {
            stop(tickCount);
        }
    }

    public static long count(final OutboundCategory c) {
        return COUNTS.get(c.ordinal());
    }

    public static long bytes(final OutboundCategory c) {
        return BYTES.get(c.ordinal());
    }

    public static long totalCount() {
        long t = 0;
        for (int i = 0; i < COUNTS.length(); i++) {
            t += COUNTS.get(i);
        }
        return t;
    }

    public static long totalBytes() {
        long t = 0;
        for (int i = 0; i < BYTES.length(); i++) {
            t += BYTES.get(i);
        }
        return t;
    }

    /** Seconds the last (or current) window covers, for per-second rates. */
    public static double windowSeconds() {
        return Math.max(0.05, lastWindowSeconds);
    }
}
