package org.embermc.ember.tune;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.embermc.ember.config.Preset;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Applies a {@link TunePresets preset} to the server's own configuration files.
 *
 * <p>Three rules make this safe to run on a live server:
 *
 * <ol>
 *   <li><b>Show before write.</b> {@link #plan} computes every change without
 *       touching disk; the command prints it, and {@link #apply} writes exactly
 *       that plan.</li>
 *   <li><b>Back up first.</b> Every file written is copied to
 *       {@code ember-backups/<name>.<timestamp>.bak} beforehand.</li>
 *   <li><b>Refuse what would break a plugin.</b> A change whose safety depends
 *       on the plugin set is checked against the live server and skipped, with
 *       the reason, rather than applied and regretted.</li>
 * </ol>
 *
 * <p>Applying {@link Preset#VANILLA} writes upstream defaults back, which is the
 * revert. Paper's world defaults take effect on {@code /paper reload};
 * {@code spigot.yml} and {@code bukkit.yml} are read at startup only, and the
 * plan says so per file.
 */
@NullMarked
public final class Tuner {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private Tuner() {
    }

    /** One line of a plan: where, what it is now, what it would become, and why it might be skipped. */
    public record Change(Setting setting, @Nullable Object current, Object target, @Nullable String skipped) {
        public boolean isNoop() {
            return this.skipped == null && Objects.equals(normalise(this.current), normalise(this.target));
        }
    }

    public record Plan(Preset preset, List<Change> changes) {
        public long applied() {
            return this.changes.stream().filter(c -> c.skipped() == null && !c.isNoop()).count();
        }

        public long skipped() {
            return this.changes.stream().filter(c -> c.skipped() != null).count();
        }

        public long unchanged() {
            return this.changes.stream().filter(Change::isNoop).count();
        }
    }

    /** Computes what {@code preset} would change. Reads files, writes nothing. */
    public static Plan plan(final Preset preset) {
        final Map<Setting.File, YamlConfiguration> loaded = load();
        final List<Change> changes = new ArrayList<>();
        for (final Setting s : TunePresets.SETTINGS) {
            final YamlConfiguration yaml = loaded.get(s.file());
            final Object current = yaml == null ? null : yaml.get(s.path());
            final Object target = s.value(preset);
            changes.add(new Change(s, current, target, yaml == null ? "file not found: " + s.file().relativePath : guard(s, target)));
        }
        return new Plan(preset, changes);
    }

    /** Writes a plan. Returns the backup files made. */
    public static List<Path> apply(final Plan plan) throws IOException {
        final Map<Setting.File, YamlConfiguration> loaded = load();
        final Map<Setting.File, Boolean> dirty = new EnumMap<>(Setting.File.class);
        for (final Change c : plan.changes()) {
            if (c.skipped() != null || c.isNoop()) {
                continue;
            }
            final YamlConfiguration yaml = loaded.get(c.setting().file());
            if (yaml != null) {
                yaml.set(c.setting().path(), c.target());
                dirty.put(c.setting().file(), true);
            }
        }
        final List<Path> backups = new ArrayList<>();
        for (final Setting.File f : dirty.keySet()) {
            final Path path = Path.of(f.relativePath);
            Files.createDirectories(BACKUP_DIR);
            final Path backup = BACKUP_DIR.resolve(path.getFileName() + "." + LocalDateTime.now().format(STAMP) + ".bak");
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            backups.add(backup);
            loaded.get(f).save(path.toFile());
        }
        return backups;
    }

    /* ---- backups ------------------------------------------------------------ */

    public static final Path BACKUP_DIR = Path.of("ember-backups");

    /** Backup timestamps present, newest first. A timestamp covers every file written in that apply. */
    public static List<String> backupStamps() throws IOException {
        if (!Files.isDirectory(BACKUP_DIR)) {
            return List.of();
        }
        final java.util.TreeSet<String> stamps = new java.util.TreeSet<>(java.util.Comparator.reverseOrder());
        try (var files = Files.list(BACKUP_DIR)) {
            files.map(pth -> pth.getFileName().toString())
                .filter(n -> n.endsWith(".bak") && n.contains(".yml."))
                .forEach(n -> stamps.add(n.substring(n.indexOf(".yml.") + 5, n.length() - 4)));
        }
        return new ArrayList<>(stamps);
    }

    /**
     * Puts back every file backed up at {@code stamp}. This is the true undo:
     * {@code revert} writes upstream defaults, which is not the same as what
     * the administrator had before an apply.
     */
    public static List<Path> restore(final String stamp) throws IOException {
        final List<Path> restored = new ArrayList<>();
        for (final Setting.File f : Setting.File.values()) {
            final Path target = Path.of(f.relativePath);
            final Path backup = BACKUP_DIR.resolve(target.getFileName() + "." + stamp + ".bak");
            if (Files.isRegularFile(backup)) {
                Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING);
                restored.add(target);
            }
        }
        return restored;
    }

    /* ---- guards: things that depend on the live server --------------------- */

    private static @Nullable String guard(final Setting s, final Object target) {
        if (s.path().equals("hopper.disable-move-event") && Boolean.TRUE.equals(target)) {
            final var listeners = InventoryMoveItemEvent.getHandlerList().getRegisteredListeners();
            if (listeners.length > 0) {
                final StringBuilder who = new StringBuilder();
                for (int i = 0; i < Math.min(3, listeners.length); i++) {
                    who.append(i > 0 ? ", " : "").append(listeners[i].getPlugin().getName());
                }
                if (listeners.length > 3) {
                    who.append(" +").append(listeners.length - 3);
                }
                return "kept: " + who + " listen for InventoryMoveItemEvent and would stop seeing hopper moves";
            }
        }
        return null;
    }

    /* ---- files -------------------------------------------------------------- */

    private static Map<Setting.File, YamlConfiguration> load() {
        final Map<Setting.File, YamlConfiguration> out = new EnumMap<>(Setting.File.class);
        for (final Setting.File f : Setting.File.values()) {
            final File file = new File(f.relativePath);
            if (file.isFile()) {
                out.put(f, YamlConfiguration.loadConfiguration(file));
            }
        }
        return out;
    }

    /** YAML gives back Integer for 8 and Double for 8.0; compare by meaning, not by class. */
    private static @Nullable Object normalise(final @Nullable Object o) {
        if (o instanceof Number n) {
            final double d = n.doubleValue();
            return d == Math.rint(d) ? (Object) (long) d : (Object) d;
        }
        if (o instanceof String s) {
            return s.trim();
        }
        return o;
    }
}
