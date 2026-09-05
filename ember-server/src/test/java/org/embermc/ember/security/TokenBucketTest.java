package org.embermc.ember.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketTest {

    private static final long S = 1_000_000_000L;

    @Test
    void aFreshBucketAllowsAFullBurstThenStops() {
        final TokenBucket b = new TokenBucket(10, 20, 0);
        for (int i = 0; i < 20; i++) {
            assertTrue(b.tryConsume(0), "burst token " + i + " should be available");
        }
        assertFalse(b.tryConsume(0), "the 21st packet in the same instant is over the burst");
    }

    @Test
    void refillsAtTheConfiguredRate() {
        final TokenBucket b = new TokenBucket(10, 20, 0);
        for (int i = 0; i < 20; i++) {
            b.tryConsume(0);
        }
        assertFalse(b.tryConsume(0));
        assertTrue(b.tryConsume(S / 10), "after 1/10 s at 10/s, one token is back");
        assertFalse(b.tryConsume(S / 10), "but only one");
    }

    @Test
    void neverRefillsAboveBurst() {
        final TokenBucket b = new TokenBucket(10, 20, 0);
        for (int i = 0; i < 20; i++) {
            b.tryConsume(0);
        }
        // Idle for an hour, then the burst is 20 again, not more.
        final long later = 3600L * S;
        for (int i = 0; i < 20; i++) {
            assertTrue(b.tryConsume(later), "token " + i + " after long idle");
        }
        assertFalse(b.tryConsume(later), "cap is still the burst, no matter how long idle");
    }

    @Test
    void sustainedRateAtTheLimitIsAllowed() {
        final TokenBucket b = new TokenBucket(100, 100, 0);
        b.tryConsume(0); // drain nothing meaningful; start the clock
        int allowed = 0;
        // One packet every 10 ms for a second = 100 packets at exactly the rate.
        for (int i = 1; i <= 100; i++) {
            if (b.tryConsume(i * (S / 100))) {
                allowed++;
            }
        }
        assertTrue(allowed >= 99, "a client exactly at the sustained rate is not blocked (was " + allowed + ")");
    }

    @Test
    void reconfigureCapsExistingFillDown() {
        final TokenBucket b = new TokenBucket(1000, 1000, 0);
        b.reconfigure(10, 5);
        int allowed = 0;
        for (int i = 0; i < 20; i++) {
            if (b.tryConsume(0)) {
                allowed++;
            }
        }
        assertTrue(allowed <= 5, "after shrinking the burst to 5, no more than 5 pass at once (was " + allowed + ")");
    }
}
