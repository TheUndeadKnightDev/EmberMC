package org.embermc.ember.net;

import java.util.Locale;

import org.jspecify.annotations.NullMarked;

/**
 * Buckets an outbound (clientbound) packet by what it is, for the network
 * sampler. Classified by the packet class's simple name so no Minecraft type is
 * needed to test it, the same approach as {@link org.embermc.ember.security.PacketCategory}.
 *
 * <p>The buckets are chosen to answer "what is filling this player's downstream":
 * entity movement and metadata (the tracker's bulk), spawns and equipment, chunk
 * and light data, blocks, and the noisy extras (sound, particle, player-info).
 */
@NullMarked
public enum OutboundCategory {
    ENTITY_MOVE,
    ENTITY_METADATA,
    ENTITY_VELOCITY,
    ENTITY_EQUIPMENT,
    ENTITY_SPAWN,
    ENTITY_OTHER,
    CHUNK,
    LIGHT,
    BLOCK,
    SOUND,
    PARTICLE,
    PLAYER_INFO,
    OTHER;

    public static final OutboundCategory[] VALUES = values();

    /** The category for a clientbound packet class name (e.g. {@code "ClientboundMoveEntityPacket$Pos"}). */
    public static OutboundCategory of(final String simpleName) {
        final String n = simpleName.toLowerCase(Locale.ROOT);
        // Order matters: more specific entity checks before the generic entity catch.
        if (n.contains("setentitydata")) {
            return ENTITY_METADATA;
        }
        if (n.contains("setentitymotion") || n.contains("entityvelocity")) {
            return ENTITY_VELOCITY;
        }
        if (n.contains("setequipment")) {
            return ENTITY_EQUIPMENT;
        }
        if (n.contains("moveentity") || n.contains("entityposition") || n.contains("teleportentity")
            || n.contains("rotatehead") || n.contains("moveminecart")) {
            return ENTITY_MOVE;
        }
        if (n.contains("addentity") || n.contains("addplayer") || n.contains("addexperienceorb")) {
            return ENTITY_SPAWN;
        }
        if (n.contains("lightupdate")) {
            return LIGHT;
        }
        if (n.contains("levelchunk") || n.contains("chunkbatch") || n.contains("forgetlevelchunk")
            || n.contains("chunksbiomes")) {
            return CHUNK;
        }
        if (n.contains("blockupdate") || n.contains("blockchange") || n.contains("blockevent")
            || n.contains("blockdestruction") || n.contains("blockentitydata")) {
            return BLOCK;
        }
        if (n.contains("sound")) {
            return SOUND;
        }
        if (n.contains("particle") || n.contains("explode")) {
            return PARTICLE;
        }
        if (n.contains("playerinfo") || n.contains("tablist")) {
            return PLAYER_INFO;
        }
        // Generic entity packets that did not match a specific bucket above.
        if (n.contains("entity") || n.contains("removeentities") || n.contains("animate")
            || n.contains("takeitem") || n.contains("hurtanimation")) {
            return ENTITY_OTHER;
        }
        return OTHER;
    }
}
