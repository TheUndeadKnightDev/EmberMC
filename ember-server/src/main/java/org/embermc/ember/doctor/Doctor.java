package org.embermc.ember.doctor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NullMarked;

/**
 * Turns the numbers EmberMC already measures into a short, ranked list of things
 * worth doing something about, each with a plain recommendation.
 *
 * <p>The profiler, the entity engine, the chunk diagnostics and the adaptive
 * engine each show one slice. An administrator still has to know which slice
 * matters right now and what to change. This reads all of them at once and says
 * so: "your tick is over budget, entities are the biggest cost, and the tiers
 * are off, so turn them on". Nothing here acts; it only advises.
 *
 * <p>The assessment ({@link #assess}) is a pure function of a {@link Snapshot},
 * so every rule is unit-tested against fixed inputs. The command builds the
 * snapshot from the live server and prints the result.
 */
@NullMarked
public final class Doctor {

    private Doctor() {
    }

    public enum Severity {
        OK, NOTICE, WARN, CRITICAL
    }

    /** One thing worth knowing, with what to do about it. */
    public record Finding(Severity severity, String title, String advice) {
    }

    /** Everything the assessment reads, gathered once so the rules stay pure. */
    public record Snapshot(
        double mspt,
        double tps1m,
        String heaviestPhase,
        double heaviestPhaseMs,
        double tickMs,
        boolean tiersEnabled,
        long chunkPluginHeld,
        int chunkWarnThreshold,
        long forceLoaded,
        int adaptiveLevel,        // 0 = normal
        boolean adaptiveEnabled,
        long usedMb,
        long maxMb,
        boolean idleTrimEnabled,
        long onlinePlayers) {
    }

    public static List<Finding> assess(final Snapshot s) {
        final List<Finding> out = new ArrayList<>();

        // --- tick budget ---
        if (s.mspt() >= 45) {
            out.add(new Finding(Severity.CRITICAL,
                String.format(Locale.ROOT, "Tick is over budget: %.1f ms (20 TPS needs under 50).", s.mspt()),
                heaviest(s) + " Run /ember profiler for the full breakdown."));
        } else if (s.mspt() >= 40) {
            out.add(new Finding(Severity.WARN,
                String.format(Locale.ROOT, "Tick is close to budget: %.1f ms.", s.mspt()),
                heaviest(s) + " Headroom is thin; check /ember profiler before it tips over."));
        } else if (s.mspt() >= 30) {
            out.add(new Finding(Severity.NOTICE,
                String.format(Locale.ROOT, "Tick has some load: %.1f ms.", s.mspt()),
                heaviest(s)));
        }

        // --- entities as the dominant cost with tiers off ---
        final boolean entityHeavy = "entities".equals(s.heaviestPhase())
            && s.tickMs() > 0 && s.heaviestPhaseMs() >= s.tickMs() * 0.30;
        if (entityHeavy && !s.tiersEnabled()) {
            out.add(new Finding(s.mspt() >= 40 ? Severity.WARN : Severity.NOTICE,
                "Entities are the biggest tick cost and entity tiers are off.",
                "Turn on entities.tiers, or apply a performance profile with /ember tune. "
                    + "Run /ember bench to see the saving on your own load first."));
        } else if (entityHeavy && s.tiersEnabled() && s.mspt() >= 45) {
            out.add(new Finding(Severity.NOTICE,
                "Entities are still the biggest cost with tiers on.",
                "Try a more aggressive profile (performance or extreme), or check /ember entities "
                    + "for what is clustered."));
        }

        // --- chunk retention ---
        if (s.chunkWarnThreshold() > 0 && s.chunkPluginHeld() > s.chunkWarnThreshold()) {
            out.add(new Finding(Severity.WARN,
                "Plugins are holding " + s.chunkPluginHeld() + " chunks loaded.",
                "See /ember chunks for which plugin. A chunk kept loaded ticks and costs memory "
                    + "whether or not a player is near."));
        }
        if (s.forceLoaded() >= 200) {
            out.add(new Finding(Severity.NOTICE,
                s.forceLoaded() + " chunks are force-loaded (they persist in level.dat).",
                "Confirm these are intentional in /ember chunks; forgotten force-loads are a common "
                    + "memory drain."));
        }

        // --- heap ---
        if (s.maxMb() > 0) {
            final double pct = s.usedMb() * 100.0 / s.maxMb();
            if (pct >= 90) {
                out.add(new Finding(Severity.WARN,
                    String.format(Locale.ROOT, "Heap is high: %d of %d MB (%.0f%%).", s.usedMb(), s.maxMb(), pct),
                    "Raise -Xmx if the host has room, or reduce loaded chunks and entities. "
                        + "Used heap counts uncollected garbage, so check /ember status live heap too."));
            } else if (pct >= 80) {
                out.add(new Finding(Severity.NOTICE,
                    String.format(Locale.ROOT, "Heap is getting full: %.0f%%.", pct),
                    "Keep an eye on it; if it climbs, more -Xmx or less load."));
            }
            if (s.onlinePlayers() == 0 && pct < 40 && !s.idleTrimEnabled()) {
                out.add(new Finding(Severity.NOTICE,
                    "Server is empty and holding heap it is not using.",
                    "Turn on memory.idle-trim (with uncommit-friendly JVM flags) to hand it back to the OS."));
            }
        }

        // --- adaptive engine actively easing load ---
        if (s.adaptiveEnabled() && s.adaptiveLevel() > 0) {
            out.add(new Finding(Severity.NOTICE,
                "Adaptive engine is easing load (level " + s.adaptiveLevel() + ").",
                "The server has been over its p95 threshold; this is EmberMC protecting the tick. "
                    + "If it stays here, address the underlying cost above."));
        }

        if (out.isEmpty()) {
            out.add(new Finding(Severity.OK,
                String.format(Locale.ROOT, "Healthy: %.1f ms MSPT, %.1f TPS.", s.mspt(), s.tps1m()),
                "Nothing needs attention."));
        }
        out.sort((a, b) -> b.severity().compareTo(a.severity()));
        return out;
    }

    private static String heaviest(final Snapshot s) {
        if (s.heaviestPhase().isEmpty() || s.heaviestPhaseMs() <= 0) {
            return "";
        }
        return String.format(Locale.ROOT, "Heaviest phase: %s (%.1f ms).", s.heaviestPhase(), s.heaviestPhaseMs());
    }
}
