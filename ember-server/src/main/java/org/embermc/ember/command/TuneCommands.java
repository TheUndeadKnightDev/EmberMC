package org.embermc.ember.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.embermc.ember.config.Preset;
import org.embermc.ember.tune.Setting;
import org.embermc.ember.tune.Tuner;
import org.jspecify.annotations.NullMarked;

import static net.kyori.adventure.text.Component.text;

/**
 * {@code /ember tune show|apply|revert [preset]}.
 *
 * <p>{@code show} prints the plan and writes nothing. {@code apply} writes it
 * after backing the files up and says, per file, whether {@code /paper reload}
 * or a restart brings it into effect. {@code revert} is {@code apply vanilla}.
 */
@NullMarked
final class TuneCommands {

    private static final TextColor EMBER = TextColor.color(0xFF8F00);
    private static final TextColor SPARK = TextColor.color(0xFFCA28);
    private static final TextColor ASH = TextColor.color(0x9E9E9E);
    private static final TextColor WHITE = NamedTextColor.WHITE;

    private TuneCommands() {
    }

    static void tune(final CommandSender sender, final String[] args) {
        if (args.length == 0) {
            usage(sender);
            return;
        }
        final String verb = args[0].toLowerCase(Locale.ROOT);
        switch (verb) {
            case "show" -> plan(sender, preset(sender, args, 1), false);
            case "apply" -> plan(sender, preset(sender, args, 1), true);
            case "revert" -> plan(sender, Preset.VANILLA, true);
            case "backups" -> backups(sender);
            case "restore" -> restore(sender, args.length > 1 ? args[1] : null);
            default -> usage(sender);
        }
    }

    private static void usage(final CommandSender sender) {
        sender.sendMessage(header("Tune"));
        sender.sendMessage(row("show <preset>", text("what the preset would change, without writing", WHITE)));
        sender.sendMessage(row("apply <preset>", text("write it, with backups; presets: vanilla, balanced, performance, extreme", WHITE)));
        sender.sendMessage(row("revert", text("write upstream defaults back (= apply vanilla)", WHITE)));
        sender.sendMessage(row("backups", text("list the backups every apply made", WHITE)));
        sender.sendMessage(row("restore <stamp>", text("put the files from that backup back exactly - the real undo", WHITE)));
        sender.sendMessage(text("  Sets Paper, Spigot and Bukkit's own keys. Paper's apply on /paper reload; the others need a restart.", ASH));
    }

    private static @org.jspecify.annotations.Nullable Preset preset(final CommandSender sender, final String[] args, final int idx) {
        if (args.length <= idx) {
            return org.embermc.ember.config.EmberConfigurations.global().profile;
        }
        try {
            return Preset.valueOf(args[idx].toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ex) {
            sender.sendMessage(text("Unknown preset '" + args[idx] + "'. Use vanilla, balanced, performance or extreme.", NamedTextColor.RED));
            return null;
        }
    }

    private static void plan(final CommandSender sender, final @org.jspecify.annotations.Nullable Preset preset, final boolean write) {
        if (preset == null) {
            return;
        }
        final Tuner.Plan plan = Tuner.plan(preset);
        sender.sendMessage(header("Tune » " + preset.name().toLowerCase(Locale.ROOT) + (write ? "" : " (preview)")));

        Setting.File lastFile = null;
        for (final Tuner.Change c : plan.changes()) {
            if (c.isNoop()) {
                continue;
            }
            if (c.setting().file() != lastFile) {
                lastFile = c.setting().file();
                sender.sendMessage(text("  " + lastFile.relativePath + "  ", WHITE).append(text("(" + lastFile.takesEffect + ")", ASH)));
            }
            final Component line = text()
                .append(text("    " + c.setting().path() + ": ", ASH))
                .append(text(String.valueOf(c.current()), ASH))
                .append(text(" → ", ASH))
                .append(text(String.valueOf(c.target()), c.skipped() == null ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                .build();
            sender.sendMessage(line);
            if (c.skipped() != null) {
                sender.sendMessage(text("      " + c.skipped(), NamedTextColor.YELLOW));
            } else if (c.setting().visible() != null) {
                sender.sendMessage(text("      you may notice: " + c.setting().visible(), ASH));
            }
        }

        sender.sendMessage(row("Summary", text(plan.applied() + " to change, " + plan.unchanged() + " already set, " + plan.skipped() + " kept for safety", WHITE)));

        if (!write) {
            sender.sendMessage(text("  Nothing written. /ember tune apply " + preset.name().toLowerCase(Locale.ROOT) + " to write it.", ASH));
            return;
        }
        if (plan.applied() == 0) {
            sender.sendMessage(text("  Nothing to write.", ASH));
            return;
        }
        try {
            final List<Path> backups = Tuner.apply(plan);
            sender.sendMessage(row("Written", text(plan.applied() + " change(s)", NamedTextColor.GREEN)));
            for (final Path b : backups) {
                sender.sendMessage(row("Backup", text(b.toString(), ASH)));
            }
            final Map<Setting.File, Boolean> touched = new EnumMap<>(Setting.File.class);
            plan.changes().stream().filter(c -> c.skipped() == null && !c.isNoop()).forEach(c -> touched.put(c.setting().file(), true));
            if (touched.containsKey(Setting.File.PAPER_WORLD_DEFAULTS)) {
                sender.sendMessage(row("Next", text("/paper reload applies the Paper changes now", WHITE)));
            }
            if (touched.containsKey(Setting.File.SPIGOT) || touched.containsKey(Setting.File.BUKKIT)) {
                sender.sendMessage(row("Next", text("spigot.yml / bukkit.yml changes apply at the next restart", NamedTextColor.YELLOW)));
            }
        } catch (final IOException ex) {
            sender.sendMessage(text("Could not write: " + ex.getMessage(), NamedTextColor.RED));
        }
    }

    private static void backups(final CommandSender sender) {
        sender.sendMessage(header("Tune » backups"));
        try {
            final List<String> stamps = Tuner.backupStamps();
            if (stamps.isEmpty()) {
                sender.sendMessage(text("  None yet. Every /ember tune apply makes one.", ASH));
                return;
            }
            for (final String st : stamps) {
                sender.sendMessage(text("  " + st, WHITE).append(text("   /ember tune restore " + st, ASH)));
            }
        } catch (final IOException ex) {
            sender.sendMessage(text("Could not list backups: " + ex.getMessage(), NamedTextColor.RED));
        }
    }

    private static void restore(final CommandSender sender, final @org.jspecify.annotations.Nullable String stamp) {
        sender.sendMessage(header("Tune » restore"));
        try {
            final List<String> stamps = Tuner.backupStamps();
            if (stamp == null) {
                sender.sendMessage(text("  Which one? /ember tune backups lists them; the oldest is usually the state before you started.", ASH));
                return;
            }
            if (!stamps.contains(stamp)) {
                sender.sendMessage(text("  No backup with stamp '" + stamp + "'. /ember tune backups lists them.", NamedTextColor.RED));
                return;
            }
            final List<Path> restored = Tuner.restore(stamp);
            for (final Path pth : restored) {
                sender.sendMessage(row("Restored", text(pth.toString(), NamedTextColor.GREEN)));
            }
            sender.sendMessage(row("Next", text("/paper reload for Paper's file; restart for spigot.yml / bukkit.yml", WHITE)));
        } catch (final IOException ex) {
            sender.sendMessage(text("Could not restore: " + ex.getMessage(), NamedTextColor.RED));
        }
    }

    private static Component header(final String title) {
        return text().append(text("Ember", EMBER)).append(text("MC", SPARK))
            .append(text(" » ", ASH)).append(text(title, WHITE)).build();
    }

    private static Component row(final String label, final Component value) {
        return text().append(text("  " + label + ": ", ASH)).append(value).build();
    }
}
