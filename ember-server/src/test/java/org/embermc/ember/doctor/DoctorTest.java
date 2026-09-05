package org.embermc.ember.doctor;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorTest {

    private static Doctor.Snapshot healthy() {
        // 8ms tick, entities small, tiers on, no retention, low heap, 4 players
        return new Doctor.Snapshot(8.0, 20.0, "entities", 2.0, 8.0,
            true, 0, 400, 0, 0, true, 2000, 8000, true, 4);
    }

    private static boolean has(List<Doctor.Finding> f, Doctor.Severity sev, String needle) {
        return f.stream().anyMatch(x -> x.severity() == sev
            && (x.title().toLowerCase().contains(needle) || x.advice().toLowerCase().contains(needle)));
    }

    @Test
    void healthyServerReportsOkOnly() {
        List<Doctor.Finding> f = Doctor.assess(healthy());
        assertEquals(1, f.size());
        assertEquals(Doctor.Severity.OK, f.get(0).severity());
    }

    @Test
    void overBudgetTickIsCriticalAndNamesHeaviestPhase() {
        var s = new Doctor.Snapshot(52.0, 12.0, "entities", 30.0, 52.0,
            true, 0, 400, 0, 2, true, 3000, 8000, true, 30);
        var f = Doctor.assess(s);
        assertEquals(Doctor.Severity.CRITICAL, f.get(0).severity());
        assertTrue(has(f, Doctor.Severity.CRITICAL, "entities"));
    }

    @Test
    void entitiesHeavyWithTiersOffAdvisesTurningThemOn() {
        var s = new Doctor.Snapshot(38.0, 18.0, "entities", 20.0, 38.0,
            false, 0, 400, 0, 0, true, 3000, 8000, true, 20);
        var f = Doctor.assess(s);
        assertTrue(has(f, Doctor.Severity.NOTICE, "tiers"));
    }

    @Test
    void pluginHeldChunksOverThresholdWarns() {
        var s = new Doctor.Snapshot(20.0, 20.0, "chunks", 5.0, 20.0,
            true, 900, 400, 0, 0, true, 3000, 8000, true, 10);
        var f = Doctor.assess(s);
        assertTrue(has(f, Doctor.Severity.WARN, "holding"));
    }

    @Test
    void highHeapWarns() {
        var s = new Doctor.Snapshot(20.0, 20.0, "entities", 3.0, 20.0,
            true, 0, 400, 0, 0, true, 7400, 8000, true, 10);
        var f = Doctor.assess(s);
        assertTrue(has(f, Doctor.Severity.WARN, "heap"));
    }

    @Test
    void emptyServerWithoutIdleTrimIsFlagged() {
        var s = new Doctor.Snapshot(4.0, 20.0, "entities", 1.0, 4.0,
            true, 0, 400, 0, 0, false, 1000, 8000, false, 0);
        var f = Doctor.assess(s);
        assertTrue(has(f, Doctor.Severity.NOTICE, "idle-trim"));
    }

    @Test
    void findingsAreSortedMostSevereFirst() {
        var s = new Doctor.Snapshot(52.0, 10.0, "entities", 30.0, 52.0,
            false, 900, 400, 300, 3, true, 7600, 8000, true, 25);
        var f = Doctor.assess(s);
        for (int i = 1; i < f.size(); i++) {
            assertTrue(f.get(i - 1).severity().compareTo(f.get(i).severity()) >= 0);
        }
        assertEquals(Doctor.Severity.CRITICAL, f.get(0).severity());
        assertFalse(f.isEmpty());
    }
}
