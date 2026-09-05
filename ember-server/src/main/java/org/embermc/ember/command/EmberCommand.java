package org.embermc.ember.command;

import io.papermc.paper.ServerBuildInfo;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static net.kyori.adventure.text.Component.text;

/**
 * The {@code /ember} command: the administrator's window into the server.
 *
 * <p>Milestone 1 ships {@code status} and {@code version}. Later milestones add
 * the profiler, entity, chunk, network, security and plugin views described in
 * {@code docs/EMBER_ROADMAP.md}, each as its own subcommand behind its own
 * {@code ember.command.<name>} permission.
 *
 * <p>Everything here reads through Bukkit/Paper API on the main thread. Nothing
 * is cached, nothing is scheduled, and nothing touches world state, so the
 * command costs exactly what it displays and no more.
 */
@NullMarked
public final class EmberCommand extends Command {

    public static final String BASE_PERMISSION = "ember.command";
    private static final DecimalFormat ONE_DP = new DecimalFormat("0.0");
    private static final DecimalFormat TWO_DP = new DecimalFormat("0.00");

    private static final TextColor EMBER = TextColor.color(0xFF8F00);
    private static final TextColor ASH = TextColor.color(0x9E9E9E);
    private static final TextColor SPARK = TextColor.color(0xFFCA28);

    private static final List<String> SUBCOMMANDS = List.of("status", "version", "config", "reload");

    public EmberCommand(final String name) {
        super(name);
        this.description = "EmberMC server status and tools";
        this.usageMessage = "/ember [" + String.join(" | ", SUBCOMMANDS) + "]";
        this.setPermission(BASE_PERMISSION);

        final var pluginManager = Bukkit.getServer().getPluginManager();
        register(pluginManager, BASE_PERMISSION, PermissionDefault.OP);
        for (final String sub : SUBCOMMANDS) {
            register(pluginManager, BASE_PERMISSION + "." + sub, PermissionDefault.OP);
        }
    }

    private static void register(final org.bukkit.plugin.PluginManager pm, final String node, final PermissionDefault def) {
        if (pm.getPermission(node) == null) {
            pm.addPermission(new Permission(node, def));
        }
    }

    @Override
    public boolean execute(final CommandSender sender, final String label, final String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        final String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> status(sender);
            case "version", "ver" -> version(sender);
            case "config" -> config(sender);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(text(this.usageMessage, NamedTextColor.RED));
        }
        return true;
    }

    /* ---- subcommands --------------------------------------------------- */

    private void status(final CommandSender sender) {
        if (!hasSub(sender, "status")) {
            return;
        }
        final double[] tps = Bukkit.getTPS();
        final double mspt = Bukkit.getAverageTickTime();

        int entities = 0;
        int chunks = 0;
        int blockEntities = 0;
        for (final World world : Bukkit.getWorlds()) {
            entities += world.getEntityCount();
            chunks += world.getChunkCount();
            blockEntities += world.getTileEntityCount();
        }

        final Runtime rt = Runtime.getRuntime();
        final long usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        final long maxMb = rt.maxMemory() / (1024 * 1024);

        // "Used" counts garbage the collector has not bothered with yet, and on a
        // server started with AlwaysPreTouch it looks alarming while meaning
        // nothing. Live-after-GC is the footprint. Read from the pools' post-GC
        // usage, which the JVM keeps for free; no collection is forced here.
        long liveMb = 0;
        for (final java.lang.management.MemoryPoolMXBean pool : java.lang.management.ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == java.lang.management.MemoryType.HEAP && pool.getCollectionUsage() != null) {
                liveMb += pool.getCollectionUsage().getUsed() / (1024 * 1024);
            }
        }

        sender.sendMessage(header("Status"));
        sender.sendMessage(row("TPS", text()
            .append(tpsPart(tps[0])).append(text(", ", ASH))
            .append(tpsPart(tps[1])).append(text(", ", ASH))
            .append(tpsPart(tps[2]))
            .append(text("  (1m, 5m, 15m)", ASH))
            .build()));
        sender.sendMessage(row("MSPT", text(ONE_DP.format(mspt) + " ms", msptColour(mspt))
            .append(text("  (5s average)", ASH))));
        sender.sendMessage(row("Players", text(Bukkit.getOnlinePlayers().size() + " / " + Bukkit.getMaxPlayers(), NamedTextColor.WHITE)));
        sender.sendMessage(row("Entities", text(String.valueOf(entities), NamedTextColor.WHITE)
            .append(text("  across " + Bukkit.getWorlds().size() + " worlds", ASH))));
        sender.sendMessage(row("Block entities", text(String.valueOf(blockEntities), NamedTextColor.WHITE)));
        sender.sendMessage(row("Loaded chunks", text(String.valueOf(chunks), NamedTextColor.WHITE)));
        sender.sendMessage(row("Heap", text(usedMb + " / " + maxMb + " MB used", NamedTextColor.WHITE)
            .append(text("  (" + TWO_DP.format(usedMb * 100.0 / Math.max(1, maxMb)) + "%)", ASH))));
        if (org.embermc.ember.config.EmberConfigurations.global().status.showLiveHeap) {
            sender.sendMessage(row("Live heap", text(liveMb + " MB after last GC", NamedTextColor.WHITE)
                .append(text("  (the real footprint)", ASH))));
        }
    }

    private void config(final CommandSender sender) {
        if (!hasSub(sender, "config")) {
            return;
        }
        final var global = org.embermc.ember.config.EmberConfigurations.global();
        sender.sendMessage(header("Config"));
        sender.sendMessage(row("Profile", text(global.profile.name().toLowerCase(Locale.ROOT), NamedTextColor.WHITE)));
        sender.sendMessage(row("Console", text("banner " + (global.console.banner ? "on" : "off") + ", colour "
            + global.console.colorLevel.name().toLowerCase(Locale.ROOT).replace('_', '-'), NamedTextColor.WHITE)));
        sender.sendMessage(row("Files", text("config/" + org.embermc.ember.config.EmberConfigurations.GLOBAL_FILE
            + ", config/" + org.embermc.ember.config.EmberConfigurations.WORLD_DEFAULTS_FILE
            + ", <world>/" + org.embermc.ember.config.EmberConfigurations.WORLD_FILE, ASH)));
        for (final World world : Bukkit.getWorlds()) {
            final var level = ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
            sender.sendMessage(row(world.getName(), text("entities.optimization = "
                + level.emberConfig().entities.optimization.name().toLowerCase(Locale.ROOT), NamedTextColor.WHITE)
                .append(text("  (read; applied from Milestone 4)", ASH))));
        }
    }

    private void reload(final CommandSender sender) {
        if (!hasSub(sender, "reload")) {
            return;
        }
        org.embermc.ember.config.EmberConfigurations.get().reload(net.minecraft.server.MinecraftServer.getServer());
        sender.sendMessage(header("Reload"));
        sender.sendMessage(row("Re-read", text("ember-global.yml, ember-world-defaults.yml and every world's ember-world.yml", NamedTextColor.WHITE)));
        sender.sendMessage(row("Applied now", text("status.*, update-checker.*, per-world entities.*", NamedTextColor.WHITE)));
        sender.sendMessage(row("Needs restart", text("profile, console.*", NamedTextColor.YELLOW)));
    }

    private void version(final CommandSender sender) {
        if (!hasSub(sender, "version")) {
            return;
        }
        final ServerBuildInfo info = ServerBuildInfo.buildInfo();
        sender.sendMessage(header("Version"));
        sender.sendMessage(row("Server", text(info.brandName() + " " + info.asString(ServerBuildInfo.StringRepresentation.VERSION_FULL), NamedTextColor.WHITE)));
        sender.sendMessage(row("Minecraft", text(info.minecraftVersionName(), NamedTextColor.WHITE)));
        sender.sendMessage(row("Bukkit API", text(Bukkit.getBukkitVersion(), NamedTextColor.WHITE)));
        sender.sendMessage(row("Java", text(Runtime.version().toString(), NamedTextColor.WHITE)));
        sender.sendMessage(row("Paper compatible", text(info.isBrandCompatible(ServerBuildInfo.BRAND_PAPER_ID) ? "yes" : "no", NamedTextColor.WHITE)));
    }

    /* ---- presentation -------------------------------------------------- */

    private static Component header(final String title) {
        return text().append(text("Ember", EMBER)).append(text("MC", SPARK))
            .append(text(" » ", ASH)).append(text(title, NamedTextColor.WHITE)).build();
    }

    private static Component row(final String label, final Component value) {
        return text().append(text("  " + label + ": ", ASH)).append(value).build();
    }

    private static Component tpsPart(final double tps) {
        final double shown = Math.min(20.0, tps);
        return text(ONE_DP.format(shown), shown >= 18 ? NamedTextColor.GREEN : shown >= 15 ? NamedTextColor.YELLOW : NamedTextColor.RED);
    }

    private static TextColor msptColour(final double mspt) {
        return mspt >= 50 ? NamedTextColor.RED : mspt >= 40 ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
    }

    private boolean hasSub(final CommandSender sender, final String sub) {
        if (sender.hasPermission(BASE_PERMISSION + "." + sub)) {
            return true;
        }
        sender.sendMessage(Bukkit.permissionMessage());
        return false;
    }

    /* ---- completion ---------------------------------------------------- */

    @Override
    public List<String> tabComplete(final CommandSender sender, final String alias, final String[] args, final @Nullable Location location) {
        if (args.length != 1) {
            return List.of();
        }
        final List<String> out = new ArrayList<>();
        final String partial = args[0].toLowerCase(Locale.ROOT);
        for (final String sub : SUBCOMMANDS) {
            if (sub.startsWith(partial) && sender.hasPermission(BASE_PERMISSION + "." + sub)) {
                out.add(sub);
            }
        }
        return out;
    }
}
