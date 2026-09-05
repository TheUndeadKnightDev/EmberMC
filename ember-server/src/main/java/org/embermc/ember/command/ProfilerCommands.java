package org.embermc.ember.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.embermc.ember.config.EmberConfigurations;
import org.embermc.ember.profiler.EmberMetrics;
import org.embermc.ember.profiler.EmberProfiler;
import org.embermc.ember.profiler.Phase;
import org.embermc.ember.profiler.PluginTimes;
import org.embermc.ember.profiler.TimeRing;
import org.jspecify.annotations.NullMarked;

import static net.kyori.adventure.text.Component.text;

/**
 * The read-outs of the profiler: {@code /ember profiler|plugins|worlds|entities|chunks|metrics}.
 *
 * <p>Every view is computed when asked and never cached, so the cost lands on
 * the command, not on the tick. Where a number can mislead - plugin attribution
 * most of all - the view says what it does and does not measure.
 */
@NullMarked
final class ProfilerCommands {

    private static final TextColor EMBER = TextColor.color(0xFF8F00);
    private static final TextColor SPARK = TextColor.color(0xFFCA28);
    private static final TextColor ASH = TextColor.color(0x9E9E9E);
    private static final TextColor WHITE = NamedTextColor.WHITE;

    private ProfilerCommands() {
    }

    /* ---- /ember profiler [start [seconds] | stop] ----------------------- */

    static void profiler(final CommandSender sender, final String[] args) {
        if (args.length >= 1) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "start" -> {
                    int seconds = EmberConfigurations.global().profiler.defaultSessionSeconds;
                    if (args.length >= 2) {
                        try {
                            seconds = Math.max(0, Integer.parseInt(args[1]));
                        } catch (final NumberFormatException ex) {
                            sender.sendMessage(text("'" + args[1] + "' is not a number of seconds.", NamedTextColor.RED));
                            return;
                        }
                    }
                    EmberProfiler.startSession(seconds);
                    sender.sendMessage(header("Profiler"));
                    sender.sendMessage(row("Session", text("started" + (seconds > 0 ? ", stops in " + seconds + " s" : ", until /ember profiler stop"), NamedTextColor.GREEN)));
                    sender.sendMessage(row("Now timing", text("event handlers and sync tasks per plugin", WHITE)
                        .append(text("  (two nanoTime calls per call; costs a little while on)", ASH))));
                    return;
                }
                case "stop" -> {
                    EmberProfiler.stopSession();
                    sender.sendMessage(header("Profiler"));
                    sender.sendMessage(row("Session", text("stopped after " + EmberProfiler.sessionTicks() + " ticks; /ember plugins keeps the results", WHITE)));
                    return;
                }
                default -> { }
            }
        }
        sender.sendMessage(header("Profiler"));
        sender.sendMessage(row("Attribution", EmberProfiler.attributing
            ? text("on", NamedTextColor.GREEN).append(text("  (" + EmberProfiler.sessionTicks() + " ticks so far)", ASH))
            : text("off", ASH).append(text("  /ember profiler start [seconds]", ASH))));
        sender.sendMessage(text("  Phase           mean 5s    p95 5s    p99 1m", ASH));
        for (final Phase p : Phase.VALUES) {
            final TimeRing.Stats five = EmberProfiler.ring(p).stats(100);
            final TimeRing.Stats minute = EmberProfiler.ring(p).stats(TimeRing.CAPACITY);
            sender.sendMessage(text()
                .append(text(String.format("  %-15s", p.label()), p == Phase.TICK ? WHITE : ASH))
                .append(text(String.format("%8.2f  ", five.meanMs()), colour(five.meanMs(), p)))
                .append(text(String.format("%8.2f  ", five.p95Ms()), colour(five.p95Ms(), p)))
                .append(text(String.format("%8.2f", minute.p99Ms()), colour(minute.p99Ms(), p)))
                .build());
        }
        sender.sendMessage(text("  Phases are always timed; the whole table costs about a microsecond per tick.", ASH));
    }

    /* ---- /ember plugins -------------------------------------------------- */

    static void plugins(final CommandSender sender) {
        sender.sendMessage(header("Plugins"));
        final List<PluginTimes.Row> rows = PluginTimes.rows();
        if (rows.isEmpty()) {
            sender.sendMessage(row("No data", text("run /ember profiler start, wait, then look again", WHITE)));
            return;
        }
        final long ticks = EmberProfiler.sessionTicks();
        sender.sendMessage(text("  Plugin                        ms/tick   events   tasks     (over " + ticks + " ticks" + (EmberProfiler.attributing ? ", running" : "") + ")", ASH));
        int shown = 0;
        for (final PluginTimes.Row r : rows) {
            if (shown++ == 15) {
                break;
            }
            final double perTick = r.totalNanos() / 1_000_000.0 / ticks;
            sender.sendMessage(text()
                .append(text(String.format("  %-28s", trim(r.plugin().getName(), 28)), WHITE))
                .append(text(String.format("%9.3f", perTick), perTick >= 5 ? NamedTextColor.RED : perTick >= 1 ? NamedTextColor.YELLOW : NamedTextColor.GREEN))
                .append(text(String.format("%9d%8d", r.eventCalls(), r.taskRuns()), ASH))
                .build());
        }
        sender.sendMessage(text("  Measures main-thread time inside event handlers and sync scheduler tasks only. It does not see", ASH));
        sender.sendMessage(text("  commands, packet listeners, entities a plugin spawned, or chunks it keeps loaded.", ASH));
    }

    /* ---- /ember worlds --------------------------------------------------- */

    static void worlds(final CommandSender sender) {
        sender.sendMessage(header("Worlds"));
        sender.sendMessage(text("  World                    tick ms  entities   chunks  block ent.  heaviest phase", ASH));
        for (final ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
            final World w = level.getWorld();
            final double tick = level.emberTimes.ring(Phase.TICK).stats(100).meanMs();
            Phase worst = Phase.WORLD_PHASES[0];
            double worstMs = -1;
            for (final Phase p : Phase.WORLD_PHASES) {
                final double m = level.emberTimes.ring(p).stats(100).meanMs();
                if (m > worstMs) {
                    worstMs = m;
                    worst = p;
                }
            }
            sender.sendMessage(text()
                .append(text(String.format("  %-24s", trim(w.getName(), 24)), WHITE))
                .append(text(String.format("%8.2f", tick), colour(tick, Phase.TICK)))
                .append(text(String.format("%10d%9d%12d", w.getEntityCount(), w.getChunkCount(), w.getTileEntityCount()), ASH))
                .append(text(String.format("  %s %.2f ms", worst.label(), worstMs), ASH))
                .build());
        }
        sender.sendMessage(text("  Means over the last 5 s. /ember profiler for percentiles.", ASH));
    }

    /* ---- /ember entities ------------------------------------------------- */

    static void entities(final CommandSender sender) {
        sender.sendMessage(header("Entities"));
        final Map<String, int[]> byType = new java.util.HashMap<>();
        int total = 0;
        for (final World w : Bukkit.getWorlds()) {
            for (final Entity e : w.getEntities()) {
                byType.computeIfAbsent(e.getType().key().value(), k -> new int[1])[0]++;
                total++;
            }
        }
        final List<Map.Entry<String, int[]>> sorted = new ArrayList<>(byType.entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<String, int[]> e) -> e.getValue()[0]).reversed());
        sender.sendMessage(row("Total", text(total + " across " + Bukkit.getWorlds().size() + " worlds", WHITE)));
        sender.sendMessage(row("Last tick", text(org.embermc.ember.entity.EntityTiers.fullLastTick() + " full, "
            + org.embermc.ember.entity.EntityTiers.reducedLastTick() + " reduced", WHITE)
            .append(text("  (reduced = outer ring, inactive-ticked this tick)", ASH))));
        if (org.embermc.ember.config.EmberConfigurations.global().entities.pathfinding.enabled) {
            sender.sendMessage(row("Pathfinds skipped", text(String.valueOf(org.embermc.ember.entity.PathfindingBackoff.skipped()), WHITE)
                .append(text("  (A* searches avoided to unreachable targets, since start)", ASH))));
        }
        for (final ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
            sender.sendMessage(text("  " + level.getWorld().getName() + ": tier " + level.emberTierLevel.name().toLowerCase(Locale.ROOT)
                + (level.emberTierLevel == org.embermc.ember.entity.EntityTiers.Level.VANILLA ? "" : ", full ring "
                + Math.round(org.embermc.ember.entity.EntityTiers.fullFraction(level.emberTierLevel) * 100) + "% of range, every " + level.emberTierInterval + " ticks outside"), ASH));
        }
        int shown = 0;
        for (final var e : sorted) {
            if (shown++ == 15) {
                break;
            }
            sender.sendMessage(text()
                .append(text(String.format("  %-24s", e.getKey()), WHITE))
                .append(text(String.format("%7d", e.getValue()[0]), ASH))
                .append(text(String.format("  %5.1f%%", total == 0 ? 0.0 : e.getValue()[0] * 100.0 / total), ASH))
                .build());
        }
    }

    /* ---- /ember chunks --------------------------------------------------- */

    static void chunks(final CommandSender sender) {
        sender.sendMessage(header("Chunks"));
        sender.sendMessage(text("  World                  loaded  ticking   forced  plugin  chunk ms", ASH));
        final boolean diag = EmberConfigurations.global().chunks.retentionDiagnostics;
        for (final ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
            final World w = level.getWorld();
            final var wc = org.embermc.ember.chunk.ChunkInsights.of(w);
            sender.sendMessage(text()
                .append(text(String.format("  %-22s", trim(w.getName(), 22)), WHITE))
                .append(text(String.format("%7d%9d", wc.loaded(), wc.ticking()), ASH))
                .append(text(String.format("%9d", wc.forceLoaded()), wc.forceLoaded() > 0 ? WHITE : ASH))
                .append(text(String.format("%8d", wc.pluginHeld()), wc.pluginHeld() > 0 ? NamedTextColor.YELLOW : ASH))
                .append(text(String.format("%10.2f", level.emberTimes.ring(Phase.CHUNKS).stats(100).meanMs()), ASH))
                .build());
        }
        sender.sendMessage(text("  loaded = all in memory, ticking = fully ticking, forced = /forceload, plugin = plugin chunk tickets.", ASH));
        if (diag) {
            boolean any = false;
            for (final ServerLevel level : MinecraftServer.getServer().getAllLevels()) {
                final World w = level.getWorld();
                final var holders = org.embermc.ember.chunk.ChunkInsights.topHolders(w, 5);
                if (holders.isEmpty()) {
                    continue;
                }
                any = true;
                sender.sendMessage(row("Held in " + w.getName(), text(holders.stream()
                    .map(e -> e.getKey().getName() + " (" + e.getValue() + ")")
                    .reduce((a, b) -> a + ", " + b).orElse("none"), WHITE)));
            }
            if (!any) {
                sender.sendMessage(row("Plugin-held", text("none - no plugin is keeping chunks loaded", ASH)));
            }
        }
    }

    /* ---- /ember doctor --------------------------------------------------- */

    static void doctor(final CommandSender sender) {
        final var cfg = EmberConfigurations.global();
        final double tickMs = EmberProfiler.ring(Phase.TICK).stats(100).meanMs();
        String heaviest = ""; double heaviestMs = 0;
        for (final Phase ph : Phase.VALUES) {
            if (ph == Phase.TICK || ph == Phase.OTHER) {
                continue;
            }
            final double m = EmberProfiler.ring(ph).stats(100).meanMs();
            if (m > heaviestMs) {
                heaviestMs = m; heaviest = ph.label();
            }
        }
        final Runtime rt = Runtime.getRuntime();
        final long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        final long maxMb = rt.maxMemory() / (1024 * 1024);
        final var snap = new org.embermc.ember.doctor.Doctor.Snapshot(
            Bukkit.getAverageTickTime(),
            Math.min(20.0, Bukkit.getTPS()[0]),
            heaviest, heaviestMs, tickMs,
            cfg.entities.tiers.enabled,
            org.embermc.ember.chunk.ChunkInsights.pluginHeldTotal(),
            cfg.chunks.warnThreshold,
            org.embermc.ember.chunk.ChunkInsights.forceLoadedTotal(),
            org.embermc.ember.adaptive.AdaptiveRuntime.level().ordinal(),
            cfg.adaptive.enabled,
            usedMb, maxMb,
            cfg.memory.idleTrim.enabled,
            Bukkit.getOnlinePlayers().size());
        final var findings = org.embermc.ember.doctor.Doctor.assess(snap);
        sender.sendMessage(header("Doctor"));
        for (final var f : findings) {
            final NamedTextColor c = switch (f.severity()) {
                case CRITICAL -> NamedTextColor.RED;
                case WARN -> NamedTextColor.YELLOW;
                case NOTICE -> NamedTextColor.AQUA;
                case OK -> NamedTextColor.GREEN;
            };
            final String tag = f.severity() == org.embermc.ember.doctor.Doctor.Severity.OK
                ? "OK" : f.severity().name();
            sender.sendMessage(text("  [" + tag + "] ", c).append(text(f.title(), WHITE)));
            if (!f.advice().isEmpty()) {
                sender.sendMessage(text("      " + f.advice(), ASH));
            }
        }
    }

    /* ---- /ember netstat -------------------------------------------------- */

    static void netstat(final CommandSender sender, final String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("start")) {
            int seconds = 15;
            if (args.length >= 2) {
                try {
                    seconds = Math.max(1, Math.min(600, Integer.parseInt(args[1])));
                } catch (final NumberFormatException ex) {
                    sender.sendMessage(text("Usage: /ember netstat start [seconds]", ASH));
                    return;
                }
            }
            org.embermc.ember.net.NetSampler.start(org.embermc.ember.profiler.EmberProfiler.tickCount(), seconds);
            sender.sendMessage(row("Network sample", text("started for " + seconds + "s", NamedTextColor.GREEN)
                .append(text("  run /ember netstat to read", ASH))));
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("stop")) {
            org.embermc.ember.net.NetSampler.stop(org.embermc.ember.profiler.EmberProfiler.tickCount());
            sender.sendMessage(row("Network sample", text("stopped", WHITE)));
            return;
        }
        sender.sendMessage(header("Network out"));
        final long total = org.embermc.ember.net.NetSampler.totalCount();
        if (total == 0) {
            sender.sendMessage(text("  No sample yet. Run /ember netstat start [seconds] to measure outbound traffic.", ASH));
            sender.sendMessage(text("  Zero cost when idle; it counts clientbound packets during the window only.", ASH));
            return;
        }
        final double secs = org.embermc.ember.net.NetSampler.windowSeconds();
        final long totalBytes = org.embermc.ember.net.NetSampler.totalBytes();
        sender.sendMessage(row(org.embermc.ember.net.NetSampler.isSampling() ? "Sampling" : "Window",
            text(String.format("%.1fs", secs), WHITE)
                .append(text(org.embermc.ember.net.NetSampler.isSampling() ? "  (still running)" : "  (last sample)", ASH))));
        sender.sendMessage(text("  Category            packets      pkt/s      bytes     KiB/s    share", ASH));
        final java.util.List<org.embermc.ember.net.OutboundCategory> cats =
            new ArrayList<>(java.util.Arrays.asList(org.embermc.ember.net.OutboundCategory.VALUES));
        cats.sort(Comparator.comparingLong((org.embermc.ember.net.OutboundCategory c) ->
            org.embermc.ember.net.NetSampler.bytes(c)).reversed());
        for (final org.embermc.ember.net.OutboundCategory c : cats) {
            final long cnt = org.embermc.ember.net.NetSampler.count(c);
            if (cnt == 0) {
                continue;
            }
            final long b = org.embermc.ember.net.NetSampler.bytes(c);
            sender.sendMessage(text()
                .append(text(String.format("  %-16s", c.name().toLowerCase(Locale.ROOT)), WHITE))
                .append(text(String.format("%10d%11.1f", cnt, cnt / secs), ASH))
                .append(text(String.format("%11s%10.1f", human(b), b / 1024.0 / secs), ASH))
                .append(text(String.format("%8.0f%%", totalBytes > 0 ? b * 100.0 / totalBytes : 0), ASH))
                .build());
        }
        sender.sendMessage(row("Total", text(total + " packets, " + human(totalBytes)
            + String.format(" (%.1f pkt/s, %.1f KiB/s)", total / secs, totalBytes / 1024.0 / secs), WHITE)));
        sender.sendMessage(text("  Outbound visibility. Metadata is already deduped by dirty-tracking upstream; nothing is dropped.", ASH));
    }

    private static String human(final long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KiB", bytes / 1024.0);
        }
        return String.format("%.1f MiB", bytes / 1024.0 / 1024.0);
    }

    /* ---- /ember metrics -------------------------------------------------- */

    static void metrics(final CommandSender sender) {
        sender.sendMessage(header("Metrics"));
        for (final var e : EmberMetrics.snapshot().entrySet()) {
            sender.sendMessage(text()
                .append(text(String.format("  %-40s", e.getKey()), ASH))
                .append(text(String.format("%12.3f", e.getValue()), WHITE))
                .build());
        }
    }

    /* ---- /ember security ------------------------------------------------- */

    static void security(final CommandSender sender) {
        sender.sendMessage(header("Security"));
        final var pg = EmberConfigurations.global().security.packetGuard;
        sender.sendMessage(row("Packet Guard", pg.enabled
            ? text("on", NamedTextColor.GREEN).append(text("  " + org.embermc.ember.security.PacketGuard.totalBlocked() + " packets blocked since start", ASH))
            : text("off", ASH)));
        sender.sendMessage(text("  Category        limit/s   burst   action     allowed    blocked", ASH));
        for (final org.embermc.ember.security.PacketCategory c : org.embermc.ember.security.PacketCategory.VALUES) {
            final var lim = pg.limitFor(c);
            final long blocked = org.embermc.ember.security.PacketGuard.blocked(c);
            sender.sendMessage(text()
                .append(text(String.format("  %-14s", c.name().toLowerCase(Locale.ROOT)), WHITE))
                .append(text(String.format("%8.0f%8.0f  ", lim.perSecond, lim.burst), ASH))
                .append(text(String.format("%-9s", lim.action.name().toLowerCase(Locale.ROOT)), ASH))
                .append(text(String.format("%9d", org.embermc.ember.security.PacketGuard.allowed(c)), ASH))
                .append(text(String.format("%11d", blocked), blocked > 0 ? NamedTextColor.YELLOW : ASH))
                .build());
        }
        sender.sendMessage(text("  Per connection. Paper's all-packets limiter runs ahead of this. Never logs contents or auth.", ASH));
        final var il = EmberConfigurations.global().entities.itemLimits;
        sender.sendMessage(row("Item limits", il.enabled
            ? text(il.maxPerChunk + " items/chunk", WHITE).append(text("  " + org.embermc.ember.entity.ItemLimits.removed() + " overflow items removed since start", ASH))
            : text("off", ASH).append(text("  (anti-dupe-flood backstop; entities.item-limits)", ASH))));
        final var xl = EmberConfigurations.global().entities.xpLimits;
        sender.sendMessage(row("XP limits", xl.enabled
            ? text(xl.maxPerChunk + " orbs/chunk", WHITE).append(text("  " + org.embermc.ember.entity.XpLimits.merged() + " overflow orbs folded into survivors (no XP lost)", ASH))
            : text("off", ASH).append(text("  (merges overflow XP orbs, keeps the XP; entities.xp-limits)", ASH))));
    }

    /* ---- presentation ---------------------------------------------------- */

    private static Component header(final String title) {
        return text().append(text("Ember", EMBER)).append(text("MC", SPARK))
            .append(text(" » ", ASH)).append(text(title, WHITE)).build();
    }

    private static Component row(final String label, final Component value) {
        return text().append(text("  " + label + ": ", ASH)).append(value).build();
    }

    private static TextColor colour(final double ms, final Phase phase) {
        if (phase == Phase.TICK) {
            return ms >= 50 ? NamedTextColor.RED : ms >= 40 ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
        }
        return ms >= 20 ? NamedTextColor.RED : ms >= 10 ? NamedTextColor.YELLOW : ASH;
    }

    private static String trim(final String s, final int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "…";
    }
}
