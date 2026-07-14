package com.moulberry.axiom.network;

import com.moulberry.axiom.packets.*;
import com.moulberry.axiom.packets.blueprint.AxiomClientboundBlueprintManifest;
import com.moulberry.axiom.packets.blueprint.AxiomClientboundResponseBlueprint;
import com.moulberry.axiom.packets.blueprint.AxiomServerboundRequestBlueprint;
import com.moulberry.axiom.packets.blueprint.AxiomServerboundUploadBlueprint;
import net.minecraft.resources.ResourceLocation;

/**
 * All Axiom protocol packet registrations, invoked from {@link AxiomNetworking#onRegister} during
 * NeoForge's {@code RegisterPayloadHandlersEvent}. Extracted verbatim from the original
 * {@code Axiom.onInitialize()} so protocol coverage stays 1:1.
 */
public final class AxiomPackets {

    private AxiomPackets() {
    }

    public static void registerAll() {
        // Clientbound (S2C)
        AxiomClientboundAckWorldProperties.register();
        AxiomClientboundEnable.register();
        AxiomClientboundCustomBlocks.register();
        AxiomClientboundRegisterCustomBlockV2.register();
        AxiomClientboundEditorWarning.register();
        AxiomClientboundIgnoreDisplayEntities.register();
        AxiomClientboundRegisterCustomItems.register();
        AxiomClientboundRedoHandshake.register();
        AxiomClientboundAddServerHeightmap.register();
        AxiomClientboundResponseChunkData.register();
        AxiomClientboundRegisterWorldProperties.register();
        AxiomClientboundRestrictions.register();
        AxiomClientboundMarkerData.register();
        AxiomClientboundMarkerNbtResponse.register();
        AxiomClientboundBlueprintManifest.register();
        AxiomClientboundResponseBlueprint.register();
        AxiomClientboundResponseEntityData.register();
        AxiomClientboundUpdateAvailableDispatchSends.register();

        // Serverbound (C2S)
        AxiomServerboundHello.register();
        AxiomServerboundSetTime.register();
        AxiomServerboundSetGameMode.register();
        AxiomServerboundSetNoPhysicalTrigger.register();
        AxiomServerboundSetFlySpeed.register();
        AxiomServerboundTeleport.register();
        AxiomServerboundSetBlock.register();
        AxiomServerboundSetBuffer.register();
        AxiomServerboundRequestChunkData.register();
        AxiomServerboundManipulateEntity.register();
        AxiomServerboundSpawnEntity.register();
        AxiomServerboundDeleteEntity.register();
        AxiomServerboundMarkerNbtRequest.register();
        AxiomServerboundRequestBlueprint.register();
        AxiomServerboundUploadBlueprint.register();
        AxiomServerboundRequestEntityData.register();
        AxiomServerboundTickBlocks.register();
        AxiomServerboundFixArea.register();

        // Bidirectional channels: same id used for a clientbound and a serverbound message. Fabric has
        // per-direction payload registries; NeoForge keys payloads globally by id, so each is registered
        // once over raw bytes and decoded per PacketFlow (see AxiomRawPayload / AxiomNetworking).
        AxiomNetworking.registerBidirectional(ResourceLocation.parse("axiom:annotation_update"),
                AxiomClientboundAnnotationUpdate::new, AxiomServerboundAnnotationUpdate::new);
        AxiomNetworking.registerBidirectional(ResourceLocation.parse("axiom:set_world_property"),
                AxiomClientboundSetWorldProperty::new, AxiomServerboundSetWorldProperty::new);
    }
}
