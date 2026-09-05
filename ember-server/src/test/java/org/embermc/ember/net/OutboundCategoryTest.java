package org.embermc.ember.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutboundCategoryTest {

    @Test
    void entityMovementAndMetadataAreDistinct() {
        assertEquals(OutboundCategory.ENTITY_MOVE, OutboundCategory.of("ClientboundMoveEntityPacket$Pos"));
        assertEquals(OutboundCategory.ENTITY_MOVE, OutboundCategory.of("ClientboundTeleportEntityPacket"));
        assertEquals(OutboundCategory.ENTITY_METADATA, OutboundCategory.of("ClientboundSetEntityDataPacket"));
        assertEquals(OutboundCategory.ENTITY_VELOCITY, OutboundCategory.of("ClientboundSetEntityMotionPacket"));
        assertEquals(OutboundCategory.ENTITY_EQUIPMENT, OutboundCategory.of("ClientboundSetEquipmentPacket"));
    }

    @Test
    void spawnsClassifyBeforeGenericEntity() {
        assertEquals(OutboundCategory.ENTITY_SPAWN, OutboundCategory.of("ClientboundAddEntityPacket"));
        assertEquals(OutboundCategory.ENTITY_SPAWN, OutboundCategory.of("ClientboundAddExperienceOrbPacket"));
    }

    @Test
    void worldDataBuckets() {
        assertEquals(OutboundCategory.CHUNK, OutboundCategory.of("ClientboundLevelChunkWithLightPacket"));
        assertEquals(OutboundCategory.CHUNK, OutboundCategory.of("ClientboundForgetLevelChunkPacket"));
        assertEquals(OutboundCategory.LIGHT, OutboundCategory.of("ClientboundLightUpdatePacket"));
        assertEquals(OutboundCategory.BLOCK, OutboundCategory.of("ClientboundBlockUpdatePacket"));
    }

    @Test
    void noisyExtras() {
        assertEquals(OutboundCategory.SOUND, OutboundCategory.of("ClientboundSoundPacket"));
        assertEquals(OutboundCategory.PARTICLE, OutboundCategory.of("ClientboundLevelParticlesPacket"));
        assertEquals(OutboundCategory.PARTICLE, OutboundCategory.of("ClientboundExplodePacket"));
        assertEquals(OutboundCategory.PLAYER_INFO, OutboundCategory.of("ClientboundPlayerInfoUpdatePacket"));
    }

    @Test
    void genericEntityAndUnknown() {
        assertEquals(OutboundCategory.ENTITY_OTHER, OutboundCategory.of("ClientboundRemoveEntitiesPacket"));
        assertEquals(OutboundCategory.ENTITY_OTHER, OutboundCategory.of("ClientboundAnimatePacket"));
        assertEquals(OutboundCategory.OTHER, OutboundCategory.of("ClientboundKeepAlivePacket"));
        assertEquals(OutboundCategory.OTHER, OutboundCategory.of("ClientboundSetTimePacket"));
    }
}
