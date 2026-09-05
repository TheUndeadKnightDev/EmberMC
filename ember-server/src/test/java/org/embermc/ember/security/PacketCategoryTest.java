package org.embermc.ember.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketCategoryTest {

    @Test
    void mojangServerboundNamesLandInTheRightCategory() {
        assertEquals(PacketCategory.MOVEMENT, PacketCategory.of("ServerboundMovePlayerPacket"));
        assertEquals(PacketCategory.MOVEMENT, PacketCategory.of("ServerboundMoveVehiclePacket"));
        assertEquals(PacketCategory.MOVEMENT, PacketCategory.of("ServerboundAcceptTeleportationPacket"));
        assertEquals(PacketCategory.ARM_SWING, PacketCategory.of("ServerboundSwingPacket"));
        assertEquals(PacketCategory.INTERACT, PacketCategory.of("ServerboundInteractPacket"));
        assertEquals(PacketCategory.INTERACT, PacketCategory.of("ServerboundPlayerActionPacket"));
        assertEquals(PacketCategory.INVENTORY, PacketCategory.of("ServerboundContainerClickPacket"));
        assertEquals(PacketCategory.BOOK_SIGN, PacketCategory.of("ServerboundEditBookPacket"));
        assertEquals(PacketCategory.BOOK_SIGN, PacketCategory.of("ServerboundSignUpdatePacket"));
        assertEquals(PacketCategory.COMMAND, PacketCategory.of("ServerboundChatCommandPacket"));
        assertEquals(PacketCategory.COMMAND, PacketCategory.of("ServerboundChatCommandSignedPacket"));
        assertEquals(PacketCategory.CHAT, PacketCategory.of("ServerboundChatPacket"));
        assertEquals(PacketCategory.TAB_COMPLETE, PacketCategory.of("ServerboundCommandSuggestionPacket"));
        assertEquals(PacketCategory.RECIPE, PacketCategory.of("ServerboundPlaceRecipePacket"));
        assertEquals(PacketCategory.CREATIVE, PacketCategory.of("ServerboundSetCreativeModeSlotPacket"));
    }

    @Test
    void commandBeatsChatForTheSignedCommandPacket() {
        // The signed command packet contains "chat" and "command"; it must be a command,
        // because command has the CPU cost and chat has the flood cost, and they get different limits.
        assertEquals(PacketCategory.COMMAND, PacketCategory.of("ServerboundChatCommandSignedPacket"));
    }

    @Test
    void unknownNamesAreOther() {
        assertEquals(PacketCategory.OTHER, PacketCategory.of("ServerboundKeepAlivePacket"));
        assertEquals(PacketCategory.OTHER, PacketCategory.of("SomeFutureThing"));
    }

    @Test
    void everyCategoryHasAName() {
        for (final PacketCategory c : PacketCategory.VALUES) {
            assertEquals(c, PacketCategory.valueOf(c.name()));
        }
    }
}
