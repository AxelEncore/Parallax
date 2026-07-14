package com.moulberry.axiom.packets;

import com.moulberry.axiom.packets.blueprint.AxiomServerboundRequestBlueprint;
import com.moulberry.axiom.packets.blueprint.AxiomServerboundUploadBlueprint;
import com.moulberry.axiom.restrictions.AxiomPermission;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public enum SupportedProtocol {
   HELLO(AxiomServerboundHello.IDENTIFIER, null),
   SET_FLY_SPEED(AxiomServerboundSetFlySpeed.IDENTIFIER, AxiomPermission.PLAYER_SPEED),
   TELEPORT(AxiomServerboundTeleport.IDENTIFIER, AxiomPermission.PLAYER_TELEPORT),
   SET_WORLD_TIME(AxiomServerboundSetTime.IDENTIFIER, AxiomPermission.WORLD_TIME),
   CREATE_ENTITY(AxiomServerboundSpawnEntity.IDENTIFIER, AxiomPermission.ENTITY_SPAWN),
   MANIPULATE_ENTITY(AxiomServerboundManipulateEntity.IDENTIFIER, AxiomPermission.ENTITY_MANIPULATE),
   DELETE_ENTITY(AxiomServerboundDeleteEntity.IDENTIFIER, AxiomPermission.ENTITY_DELETE),
   REQUEST_ENTITY(AxiomServerboundRequestEntityData.IDENTIFIER, AxiomPermission.ENTITY_REQUESTDATA),
   REQUEST_CHUNK(AxiomServerboundRequestChunkData.IDENTIFIER, AxiomPermission.CHUNK_REQUEST),
   MARKER_NBT_REQUEST(AxiomServerboundMarkerNbtRequest.IDENTIFIER, AxiomPermission.ENTITY_REQUESTDATA),
   ANNOTATION_UPDATE(AxiomServerboundAnnotationUpdate.IDENTIFIER, AxiomPermission.ANNOTATION_CREATE),
   BLUEPRINT_UPLOAD(AxiomServerboundUploadBlueprint.IDENTIFIER, AxiomPermission.BLUEPRINT_UPLOAD),
   BLUEPRINT_REQUEST(AxiomServerboundRequestBlueprint.IDENTIFIER, AxiomPermission.BLUEPRINT_REQUEST),
   TICK_BLOCKS(AxiomServerboundTickBlocks.IDENTIFIER, AxiomPermission.BUILD_DANGEROUS_TICK),
   SET_NO_PHYSICAL_TRIGGER(AxiomServerboundSetNoPhysicalTrigger.IDENTIFIER, AxiomPermission.PLAYER_SETNOPHYSICALTRIGGER);

   public final ResourceLocation identifier;
   @Nullable
   public final AxiomPermission relatedPermission;

   private SupportedProtocol(ResourceLocation identifier, @Nullable AxiomPermission relatedPermission) {
      this.identifier = identifier;
      this.relatedPermission = relatedPermission;
   }
}
