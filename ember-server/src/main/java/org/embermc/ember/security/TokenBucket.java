package org.embermc.ember.security;

import org.jspecify.annotations.NullMarked;

/**
 * A token bucket: a rate limit that forgives bursts.
 *
 * <p>Tokens refill at {@code ratePerSecond} up to a ceiling of {@code burst}.
 * Each event tries to spend one token; if the bucket is dry the event is over
 * the limit. A player who opens a chest and shift-clicks a whole row spends a
 * burst of tokens at once and is fine; a bot sending a thousand packets a second
 * drains the bucket and stays dry. That is the difference between this and a
 * fixed per-second counter, which cannot tell the two apart.
 *
 * <p>Pure arithmetic on two longs and a double — no allocation, no clock of its
 * own (the caller passes {@code nowNanos}), so it is trivial to test and cheap
 * to run per packet. Not thread-safe; guard it the way the caller guards the
 * connection.
 */
@NullMarked
public final class TokenBucket {

    private double ratePerSecond;
    private double burst;
    private double tokens;
    private long lastNanos;

    public TokenBucket(final double ratePerSecond, final double burst, final long nowNanos) {
        this.ratePerSecond = ratePerSecond;
        this.burst = burst;
        this.tokens = burst;
        this.lastNanos = nowNanos;
    }

    /** Re-arms the limits without dropping the current fill (for a live config reload). */
    public void reconfigure(final double ratePerSecond, final double burst) {
        this.ratePerSecond = ratePerSecond;
        this.burst = burst;
        if (this.tokens > burst) {
            this.tokens = burst;
        }
    }

    /**
     * Refills for the elapsed time and tries to spend one token.
     *
     * @return true if a token was available (the event is within the limit)
     */
    public boolean tryConsume(final long nowNanos) {
        refill(nowNanos);
        if (this.tokens >= 1.0) {
            this.tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill(final long nowNanos) {
        final long elapsed = nowNanos - this.lastNanos;
        if (elapsed <= 0) {
            return;
        }
        this.lastNanos = nowNanos;
        this.tokens = Math.min(this.burst, this.tokens + elapsed / 1_000_000_000.0 * this.ratePerSecond);
    }

    /** Current fill, for diagnostics. */
    public double tokens() {
        return this.tokens;
    }
}
