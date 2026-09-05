package org.embermc.ember.profiler;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * Main-thread time spent inside each plugin's event handlers and sync tasks.
 *
 * <p>Only collected while a profiling session is running, because collecting it
 * costs two {@code nanoTime} calls per handler call. Only main-thread work is
 * counted: async events and async tasks run off the tick and cannot lag it,
 * and attributing them would need synchronisation this class deliberately
 * avoids.
 *
 * <p>What this measures is honest but partial: time a plugin spends in an
 * event handler or a scheduled task. It does not see a plugin's cost inside a
 * command it registered, a packet listener, a ticking entity it spawned, or a
 * chunk it forced to stay loaded. The command view says so.
 */
@NullMarked
public final class PluginTimes {

    private static final Object2LongOpenHashMap<Plugin> EVENT_NANOS = new Object2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<Plugin> TASK_NANOS = new Object2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<Plugin> EVENT_CALLS = new Object2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<Plugin> TASK_RUNS = new Object2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<Plugin> TICK_NANOS = new Object2LongOpenHashMap<>();

    private PluginTimes() {
    }

    public static void event(final Plugin plugin, final long nanos) {
        EVENT_NANOS.addTo(plugin, nanos);
        EVENT_CALLS.addTo(plugin, 1);
        TICK_NANOS.addTo(plugin, nanos);
    }

    public static void task(final Plugin plugin, final long nanos) {
        TASK_NANOS.addTo(plugin, nanos);
        TASK_RUNS.addTo(plugin, 1);
        TICK_NANOS.addTo(plugin, nanos);
    }

    static void reset() {
        EVENT_NANOS.clear();
        TASK_NANOS.clear();
        EVENT_CALLS.clear();
        TASK_RUNS.clear();
        TICK_NANOS.clear();
    }

    static void tickReset() {
        TICK_NANOS.clear();
    }

    public record Row(Plugin plugin, long eventNanos, long taskNanos, long eventCalls, long taskRuns) {
        public long totalNanos() {
            return this.eventNanos + this.taskNanos;
        }
    }

    /** Session totals, heaviest first. */
    public static List<Row> rows() {
        final List<Row> rows = new ArrayList<>();
        final java.util.Set<Plugin> plugins = new java.util.HashSet<>(EVENT_NANOS.keySet());
        plugins.addAll(TASK_NANOS.keySet());
        for (final Plugin p : plugins) {
            rows.add(new Row(p, EVENT_NANOS.getLong(p), TASK_NANOS.getLong(p), EVENT_CALLS.getLong(p), TASK_RUNS.getLong(p)));
        }
        rows.sort(Comparator.comparingLong(Row::totalNanos).reversed());
        return rows;
    }

    /** Plugins that cost the most during the tick in progress (for spike reports). */
    public static List<Object2LongMap.Entry<Plugin>> topThisTick(final int n) {
        final List<Object2LongMap.Entry<Plugin>> entries = new ArrayList<>(TICK_NANOS.object2LongEntrySet());
        entries.sort((a, b) -> Long.compare(b.getLongValue(), a.getLongValue()));
        return entries.size() > n ? entries.subList(0, n) : entries;
    }
}
