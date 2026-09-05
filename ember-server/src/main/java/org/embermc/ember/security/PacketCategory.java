package org.embermc.ember.security;

import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/**
 * Groups serverbound packets into the classes an administrator actually limits.
 *
 * <p>Paper's limiter is per exact packet type or all-packets; neither is the
 * granularity a server wants. "Movement" and "arm swings" are high-rate and
 * cheap; "book edits" and "sign updates" are low-rate and carry big payloads;
 * "commands" and "tab completion" are where spam and CPU abuse live. Limiting
 * by category lets each get a limit that fits it.
 *
 * <p>Classification is by the packet class's <em>simple name</em>, so this
 * class needs no Minecraft type on the classpath and is unit-tested with plain
 * strings. Mojang's serverbound names are stable enough (Serverbound…Packet)
 * that string matching is both clear and robust across versions; an unknown
 * name falls into {@link #OTHER}, which has a generous default.
 */
@NullMarked
public enum PacketCategory {
    MOVEMENT,
    ARM_SWING,
    INTERACT,
    INVENTORY,
    BOOK_SIGN,
    CHAT,
    COMMAND,
    TAB_COMPLETE,
    RECIPE,
    CREATIVE,
    PLUGIN_MESSAGE,
    OTHER;

    public static final PacketCategory[] VALUES = values();

    /** The category for a packet class name (e.g. {@code "ServerboundMovePlayerPacket"}). */
    public static PacketCategory of(final String simpleName) {
        final String n = simpleName.toLowerCase(Locale.ROOT);
        if (n.contains("moveplayer") || n.contains("movevehicle") || n.contains("playerinput")
            || n.contains("playerposition") || n.contains("playerrotation") || n.contains("acceptteleport")
            || n.contains("steervehicle") || n.contains("paddleboat")) {
            return MOVEMENT;
        }
        if (n.contains("swing")) {
            return ARM_SWING;
        }
        if (n.contains("interact") || n.contains("useitem") || n.contains("blockplace") || n.contains("playeraction")) {
            return INTERACT;
        }
        if (n.contains("containerclick") || n.contains("containerbutton") || n.contains("containerclose")
            || n.contains("carrieditem") || n.contains("pickitem") || n.contains("holditem")) {
            return INVENTORY;
        }
        if (n.contains("editbook") || n.contains("signupdate")) {
            return BOOK_SIGN;
        }
        if (n.contains("chatcommand") || n.contains("commandsigned")) {
            return COMMAND;
        }
        if (n.contains("chat")) {
            return CHAT;
        }
        if (n.contains("commandsuggestion") || n.contains("tabcomplete")) {
            return TAB_COMPLETE;
        }
        if (n.contains("placerecipe") || n.contains("recipe")) {
            return RECIPE;
        }
        if (n.contains("setcreativemodeslot") || n.contains("creativeinventory")) {
            return CREATIVE;
        }
        if (n.contains("custompayload") || n.contains("payload") || n.contains("cookieresponse")) {
            return PLUGIN_MESSAGE;
        }
        return OTHER;
    }
}
