package org.embermc.ember;

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.util.JarManifests;
import java.util.Optional;
import java.util.jar.Manifest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.ansi.ColorLevel;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.craftbukkit.CraftServer;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;

import static net.kyori.adventure.text.Component.text;

/**
 * The few lines EmberMC prints at startup, and nothing more.
 *
 * <p>Printed once from {@code DedicatedServer.initServer}, immediately after
 * vanilla announces its own version, so the two sit together in the log and a
 * pasted log identifies the server at a glance.
 *
 * <p>Colour goes through Adventure's ANSI serializer, the same route Paper uses
 * for console messages: the console appender passes ANSI through and the file
 * appender strips it, so {@code latest.log} stays plain. Set
 * {@code -Dember.plainBanner=true} to print without colour anywhere.
 *
 * <p>Later milestones add their one status line here (profile, Packet Guard,
 * Adaptive Engine) rather than logging separately, so the console stays quiet.
 * Nothing is listed until it exists.
 */
@NullMarked
public final class EmberBanner {

    private static final TextColor EMBER = TextColor.color(0xFF8F00);
    private static final TextColor SPARK = TextColor.color(0xFFCA28);
    private static final TextColor ASH = TextColor.color(0x8A8A8A);
    private static final TextColor RULE = TextColor.color(0x5A5A5A);
    private static final TextColor VALUE = NamedTextColor.WHITE;

    private static final String ATTRIBUTE_UPSTREAM = "Upstream-Paper-Commit";

    /**
     * Explicit true colour. Inside a container {@code COLORTERM} is unset, so the
     * serializer would otherwise guess a 16-colour terminal and fold orange and
     * amber into the same yellow. Panel consoles render true colour; a terminal
     * that cannot can set {@code -Dember.colorLevel=indexed16}.
     */
    /** console.color-level from ember-global.yml; -Dember.colorLevel overrides it for one run. */
    private static ColorLevel colourLevel() {
        final String prop = System.getProperty("ember.colorLevel");
        if (prop != null) {
            return switch (prop.toLowerCase(java.util.Locale.ROOT)) {
                case "indexed16", "16" -> ColorLevel.INDEXED_16;
                case "indexed256", "256" -> ColorLevel.INDEXED_256;
                case "none" -> ColorLevel.NONE;
                default -> ColorLevel.TRUE_COLOR;
            };
        }
        if (org.embermc.ember.config.EmberConfigurations.isInitialized()) {
            return switch (org.embermc.ember.config.EmberConfigurations.global().console.colorLevel) {
                case INDEXED_16 -> ColorLevel.INDEXED_16;
                case INDEXED_256 -> ColorLevel.INDEXED_256;
                case NONE -> ColorLevel.NONE;
                case TRUECOLOR -> ColorLevel.TRUE_COLOR;
            };
        }
        return ColorLevel.TRUE_COLOR;
    }
    private static final int WIDTH = 60;

    private EmberBanner() {
    }

    public static void print(final Logger logger) {
        if (org.embermc.ember.config.EmberConfigurations.isInitialized()
            && !org.embermc.ember.config.EmberConfigurations.global().console.banner) {
            return;
        }
        final ServerBuildInfo info = ServerBuildInfo.buildInfo();
        final String upstream = upstreamCommit().map(c -> "Paper " + c.substring(0, Math.min(7, c.length())))
            .orElse("Paper");

        final boolean colour = !Boolean.getBoolean("ember.plainBanner")
            && !"false".equalsIgnoreCase(System.getProperty("terminal.ansi", "true"))
            && colourLevel() != ColorLevel.NONE;

        final Component top = text().append(text("╭", RULE)).append(text("─".repeat(WIDTH), RULE)).append(text("╮", RULE)).build();
        final Component bottom = text().append(text("╰", RULE)).append(text("─".repeat(WIDTH), RULE)).append(text("╯", RULE)).build();

        final Component[] lines = {
            top,
            row(text().append(text("◆ ", EMBER)).append(text("Ember", EMBER)).append(text("MC", SPARK))
                .append(text("  Ignite better performance.", ASH)).build()),
            row(Component.empty()),
            row(field("Version", info.asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE))),
            row(field("Minecraft", info.minecraftVersionName())),
            row(field("Upstream", upstream + " (compatible)")),
            row(field("Java", Runtime.version().toString())),
            row(field("Profiler", "phases timed · plugins off (/ember profiler start)")),
            row(field("Security", org.embermc.ember.config.EmberConfigurations.isInitialized()
                && org.embermc.ember.config.EmberConfigurations.global().security.packetGuard.enabled
                ? "Packet Guard on · per-category limits" : "Packet Guard off")),
            row(field("Adaptive", org.embermc.ember.config.EmberConfigurations.isInitialized()
                && org.embermc.ember.config.EmberConfigurations.global().adaptive.enabled
                ? "on · light/moderate/aggressive at p95 "
                    + (int) org.embermc.ember.config.EmberConfigurations.global().adaptive.lightAboveMs + "/"
                    + (int) org.embermc.ember.config.EmberConfigurations.global().adaptive.moderateAboveMs + "/"
                    + (int) org.embermc.ember.config.EmberConfigurations.global().adaptive.aggressiveAboveMs + " ms"
                : "off")),
            row(field("Entities", org.embermc.ember.config.EmberConfigurations.isInitialized()
                && org.embermc.ember.config.EmberConfigurations.global().entities.tiers.enabled
                ? "tiered ticking on · profile " + org.embermc.ember.config.EmberConfigurations.global().profile.name().toLowerCase(java.util.Locale.ROOT)
                : "tiered ticking off")),
            bottom,
        };

        for (final Component line : lines) {
            logger.info(colour ? ANSIComponentSerializer.builder().colorLevel(colourLevel()).build().serialize(line)
                : PlainTextComponentSerializer.plainText().serialize(line));
        }
    }

    /** One boxed line: left rule, content padded to the box width, right rule. */
    private static Component row(final Component content) {
        final int used = PlainTextComponentSerializer.plainText().serialize(content).length();
        final int pad = Math.max(0, WIDTH - 2 - used);
        return text().append(text("│ ", RULE)).append(content).append(text(" ".repeat(pad), RULE)).append(text(" │", RULE)).build();
    }

    private static TextComponent field(final String label, final String value) {
        return text().append(text(String.format("%-10s", label), ASH)).append(text(value, VALUE)).build();
    }

    private static Optional<String> upstreamCommit() {
        final Manifest manifest = JarManifests.manifest(CraftServer.class);
        if (manifest == null) {
            return Optional.empty();
        }
        final String v = manifest.getMainAttributes().getValue(ATTRIBUTE_UPSTREAM);
        return v == null || v.isBlank() ? Optional.empty() : Optional.of(v);
    }
}
