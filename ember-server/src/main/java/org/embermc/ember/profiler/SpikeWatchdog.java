package org.embermc.ember.profiler;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.embermc.ember.config.EmberConfigurations;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures what a bad tick was doing, at the moment it happens.
 *
 * <p>Nobody is watching when the spike hits; by the time an administrator opens
 * a profiler it is gone. This runs on every tick end, compares the tick against
 * a threshold, and when it trips, writes down the phase breakdown of <em>that
 * tick</em>, every world's share, entity and chunk counts, whether a garbage
 * collection happened during it, and - if a session is running - which plugins
 * were heaviest. One line to the console, the full report to a file.
 *
 * <p>Cost when nothing is wrong: one comparison plus a read of two GC counters,
 * on the order of a microsecond. Reports are rate-limited so a sustained
 * overload produces one file every cooldown, not thousands.
 */
@NullMarked
public final class SpikeWatchdog {

    private static final Logger LOGGER = LoggerFactory.getLogger("EmberMC");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final List<GarbageCollectorMXBean> GC = ManagementFactory.getGarbageCollectorMXBeans();

    private static long lastGcCount;
    private static long lastGcTimeMs;
    private static long lastReportNanos;

    private SpikeWatchdog() {
    }

    static void onTick(final long tickNanos, final long[] current) {
        // GC counters first, every tick, so a spike can be blamed on a collection
        // that happened inside it rather than on the phase it landed in.
        long gcCount = 0;
        long gcTime = 0;
        for (int i = 0; i < GC.size(); i++) {
            final GarbageCollectorMXBean bean = GC.get(i);
            gcCount += bean.getCollectionCount();
            gcTime += bean.getCollectionTime();
        }
        final long gcDeltaCount = gcCount - lastGcCount;
        final long gcDeltaMs = gcTime - lastGcTimeMs;
        lastGcCount = gcCount;
        lastGcTimeMs = gcTime;

        final var cfg = EmberConfigurations.global().profiler;
        if (tickNanos < cfg.spikeThresholdMs * 1_000_000L) {
            return;
        }
        final long now = System.nanoTime();
        if (now - lastReportNanos < cfg.spikeReportCooldownSeconds * 1_000_000_000L) {
            return;
        }
        lastReportNanos = now;

        final String report = build(tickNanos, current, gcDeltaCount, gcDeltaMs);
        final Phase worst = worstPhase(current);
        LOGGER.warn("Tick took {} ms (threshold {} ms); heaviest phase: {} {} ms{}. Report: {}",
            ms(tickNanos), cfg.spikeThresholdMs, worst.label(), ms(current[worst.ordinal()]),
            gcDeltaCount > 0 ? ", GC ran " + gcDeltaCount + "x (" + gcDeltaMs + " ms)" : "",
            write(report, cfg.spikeReportsDir, cfg.keepSpikeReports));
    }

    private static Phase worstPhase(final long[] current) {
        Phase worst = Phase.OTHER;
        for (final Phase p : Phase.VALUES) {
            if (p != Phase.TICK && current[p.ordinal()] > current[worst.ordinal()]) {
                worst = p;
            }
        }
        return worst;
    }

    private static String build(final long tickNanos, final long[] current, final long gcCount, final long gcMs) {
        final StringBuilder sb = new StringBuilder(2048);
        sb.append("EmberMC tick spike report\n");
        sb.append("time: ").append(LocalDateTime.now()).append('\n');
        sb.append("tick: ").append(ms(tickNanos)).append(" ms (tick #").append(EmberProfiler.tickCount()).append(")\n");
        sb.append("players online: ").append(Bukkit.getOnlinePlayers().size()).append('\n');
        sb.append("gc during tick: ").append(gcCount > 0 ? gcCount + " collection(s), " + gcMs + " ms" : "none").append('\n');
        sb.append('\n').append("phases (this tick):\n");
        for (final Phase p : Phase.VALUES) {
            if (p == Phase.TICK) {
                continue;
            }
            sb.append(String.format("  %-15s %9s ms  %5.1f%%%n", p.label(), ms(current[p.ordinal()]),
                tickNanos == 0 ? 0.0 : current[p.ordinal()] * 100.0 / tickNanos));
        }
        sb.append('\n').append("worlds (this tick):\n");
        for (final ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
            final long[] w = level.emberTimes.current;
            // endTick() has already filed this tick for the world, so read the most recent ring entry instead.
            sb.append(String.format("  %-24s tick %8s ms | entities %6d | chunks %6d | block entities %6d%n",
                level.dimension().identifier(), ms(level.emberTimes.ring(Phase.TICK).sum(1)),
                level.getWorld().getEntityCount(), level.getChunkSource().getFullChunksCount(),
                level.getWorld().getTileEntityCount()));
            for (final Phase p : Phase.WORLD_PHASES) {
                sb.append(String.format("      %-15s %9s ms%n", p.label(), ms(level.emberTimes.ring(p).sum(1))));
            }
        }
        sb.append('\n');
        if (EmberProfiler.attributing) {
            sb.append("plugins (this tick, event handlers + sync tasks):\n");
            for (final var e : PluginTimes.topThisTick(10)) {
                sb.append(String.format("  %-32s %9s ms%n", e.getKey().getName(), ms(e.getLongValue())));
            }
        } else {
            sb.append("plugins: no profiling session running. /ember profiler start attributes event and task time per plugin.\n");
        }
        sb.append('\n').append("recent context (last 5 s, mean / p95 / p99 ms):\n");
        for (final Phase p : Phase.VALUES) {
            final TimeRing.Stats s = EmberProfiler.ring(p).stats(100);
            sb.append(String.format("  %-15s %8.2f / %8.2f / %8.2f%n", p.label(), s.meanMs(), s.p95Ms(), s.p99Ms()));
        }
        return sb.toString();
    }

    /** Writes the report off the main thread and prunes old ones. Returns the intended path. */
    private static String write(final String report, final String dir, final int keep) {
        final Path folder = Path.of(dir);
        final Path file = folder.resolve("spike-" + LocalDateTime.now().format(STAMP) + ".txt");
        io.papermc.paper.util.MCUtil.ASYNC_EXECUTOR.execute(() -> {
            try {
                Files.createDirectories(folder);
                Files.writeString(file, report);
                try (Stream<Path> files = Files.list(folder)) {
                    final List<Path> reports = files.filter(p -> p.getFileName().toString().startsWith("spike-")).sorted().toList();
                    for (int i = 0; i < reports.size() - keep; i++) {
                        Files.deleteIfExists(reports.get(i));
                    }
                }
            } catch (final IOException ex) {
                LOGGER.warn("Could not write spike report {}", file, ex);
            }
        });
        return file.toString();
    }

    private static String ms(final long nanos) {
        return String.format("%.2f", nanos / 1_000_000.0);
    }
}
