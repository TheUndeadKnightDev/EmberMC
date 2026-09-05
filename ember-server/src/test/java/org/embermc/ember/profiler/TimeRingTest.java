package org.embermc.ember.profiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeRingTest {

    @Test
    void emptyRingHasEmptyStats() {
        final TimeRing ring = new TimeRing();
        assertSame(TimeRing.Stats.EMPTY, ring.stats(100));
        assertEquals(0, ring.sum(100));
    }

    @Test
    void meanAndPercentilesOverAKnownWindow() {
        final TimeRing ring = new TimeRing();
        for (int i = 1; i <= 100; i++) {
            ring.record(i * 1_000_000L); // 1 ms .. 100 ms
        }
        final TimeRing.Stats s = ring.stats(100);
        assertEquals(100, s.samples());
        assertEquals(50.5, s.meanMs(), 1e-9);
        assertEquals(96.0, s.p95Ms(), 1e-9);   // index floor(100*0.95)=95 -> 96th value
        assertEquals(100.0, s.p99Ms(), 1e-9);  // index 99 -> 100th value
        assertEquals(100.0, s.maxMs(), 1e-9);
    }

    @Test
    void windowIsTheMostRecentSamplesOnly() {
        final TimeRing ring = new TimeRing();
        for (int i = 0; i < 50; i++) {
            ring.record(1_000_000_000L); // a second each: old noise
        }
        for (int i = 0; i < 20; i++) {
            ring.record(2_000_000L); // 2 ms each: the recent window
        }
        assertEquals(2.0, ring.stats(20).meanMs(), 1e-9);
        assertEquals(40_000_000L, ring.sum(20));
        assertTrue(ring.stats(70).meanMs() > 500.0, "the wider window still sees the old samples");
    }

    @Test
    void wrapsAroundWithoutLosingOrder() {
        final TimeRing ring = new TimeRing();
        for (int i = 0; i < TimeRing.CAPACITY + 10; i++) {
            ring.record(i);
        }
        assertEquals(TimeRing.CAPACITY, ring.size());
        // The five most recent are the five highest values written.
        final long expected = (long) (TimeRing.CAPACITY + 9) + (TimeRing.CAPACITY + 8) + (TimeRing.CAPACITY + 7) + (TimeRing.CAPACITY + 6) + (TimeRing.CAPACITY + 5);
        assertEquals(expected, ring.sum(5));
    }

    @Test
    void askingForMoreThanRecordedClampsToWhatExists() {
        final TimeRing ring = new TimeRing();
        ring.record(3_000_000L);
        ring.record(5_000_000L);
        final TimeRing.Stats s = ring.stats(1000);
        assertEquals(2, s.samples());
        assertEquals(4.0, s.meanMs(), 1e-9);
    }
}
