package org.embermc.ember.security;

import java.util.concurrent.atomic.AtomicLongArray;
import org.embermc.ember.config.EmberConfigurations;
import org.embermc.ember.config.EmberGlobalConfiguration;
import org.jspecify.annotations.NullMarked;

/**
 * The Ember Packet Guard: one place for per-category inbound packet limits.
 *
 * <p>Each connection carries a {@link Session} — one {@link TokenBucket} per
 * {@link PacketCategory}, created lazily. On every serverbound packet the guard
 * classifies it, refills and spends a token, and if the bucket is dry applies
 * the category's {@link GuardAction}. Paper's own all-packets limiter still runs
 * ahead of this as the blunt backstop; this is the shaped layer on top.
 *
 * <p>Cost on the hot path: one array index for the category, one bucket refill
 * (arithmetic on two longs), and, only on a violation, one counter increment.
 * No allocation per packet after the first of each category on a connection.
 * The guard reads its limits from config through a snapshot refreshed once a
 * second, not per packet.
 *
 * <p>What it deliberately does not do: it never logs packet contents, IP beyond
 * what Paper already logs, or anything about authentication. Diagnostics are
 * counts per category, nothing more.
 */
@NullMarked
public final class PacketGuard {

    /** Server-wide diagnostics: allowed / throttled / kicked per category since start. */
    private static final AtomicLongArray ALLOWED = new AtomicLongArray(PacketCategory.VALUES.length);
    private static final AtomicLongArray BLOCKED = new AtomicLongArray(PacketCategory.VALUES.length);
    private static long lastWarnNanos;

    private PacketGuard() {
    }

    public static boolean enabled() {
        return EmberConfigurations.isInitialized() && EmberConfigurations.global().security.packetGuard.enabled;
    }

    /** Per-connection limiter state. One is held by each {@code Connection}. */
    public static final class Session {
        private final TokenBucket[] buckets = new TokenBucket[PacketCategory.VALUES.length];

        /**
         * @return true to let the packet through, false to drop it. A kick is
         *         requested via {@link #shouldKick}, checked by the caller after
         *         a false return, so this class never touches the connection.
         */
        public boolean allow(final PacketCategory category, final int payloadBytes, final long nowNanos) {
            final EmberGlobalConfiguration.Security.PacketGuard cfg = EmberConfigurations.global().security.packetGuard;
            final EmberGlobalConfiguration.Security.PacketGuard.Limit limit = cfg.limitFor(category);
            final GuardAction action = GuardAction.valueOf(limit.action.name());
            final int idx = category.ordinal();

            // Oversized payloads are a violation regardless of rate: a book or a
            // plugin message far past any legitimate size is an attack, not spam.
            final boolean oversized = limit.maxBytes > 0 && payloadBytes > limit.maxBytes;

            TokenBucket bucket = this.buckets[idx];
            if (bucket == null) {
                bucket = new TokenBucket(limit.perSecond, limit.burst, nowNanos);
                this.buckets[idx] = bucket;
            } else {
                bucket.reconfigure(limit.perSecond, limit.burst);
            }

            final boolean withinRate = bucket.tryConsume(nowNanos);
            if (withinRate && !oversized) {
                ALLOWED.incrementAndGet(idx);
                return true;
            }
            BLOCKED.incrementAndGet(idx);
            this.lastKick = action == GuardAction.KICK;
            maybeWarn(category, action, oversized);
            return switch (action) {
                case LOG, WARN -> true;          // observed, not enforced
                case THROTTLE, DROP, KICK -> false;
            };
        }

        private boolean lastKick;

        /** Whether the last {@link #allow} that returned false wants the connection closed. */
        public boolean shouldKick() {
            return this.lastKick;
        }
    }

    private static void maybeWarn(final PacketCategory category, final GuardAction action, final boolean oversized) {
        if (action != GuardAction.WARN && action != GuardAction.KICK) {
            return;
        }
        final long now = System.nanoTime();
        if (now - lastWarnNanos < 5_000_000_000L) {
            return;
        }
        lastWarnNanos = now;
        org.slf4j.LoggerFactory.getLogger("EmberMC").warn(
            "Packet Guard: {} over its {} limit ({}), action {}",
            category.name().toLowerCase(java.util.Locale.ROOT),
            oversized ? "payload-size" : "rate",
            "per-connection", action.name().toLowerCase(java.util.Locale.ROOT));
    }

    public static long allowed(final PacketCategory c) {
        return ALLOWED.get(c.ordinal());
    }

    public static long blocked(final PacketCategory c) {
        return BLOCKED.get(c.ordinal());
    }

    public static long totalBlocked() {
        long n = 0;
        for (int i = 0; i < BLOCKED.length(); i++) {
            n += BLOCKED.get(i);
        }
        return n;
    }
}
