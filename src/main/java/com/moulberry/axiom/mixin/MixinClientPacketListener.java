package com.moulberry.axiom.mixin;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.HotbarManager;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.capabilities.NoClip;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.integration.ServerIntegration;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.render.BiomeOverlayRenderer;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.CollisionMeshOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess.Frozen;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket.ChunkBiomeData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPacketListener.class})
public abstract class MixinClientPacketListener extends ClientCommonPacketListenerImpl {
   @Shadow
   private ClientLevel level;

   @Shadow
   public abstract Frozen registryAccess();

   protected MixinClientPacketListener(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
      super(minecraft, connection, commonListenerCookie);
   }

   @Inject(
      method = {"handleConfigurationStart"},
      at = {@At("HEAD")}
   )
   public void handleConfigurationStart(CallbackInfo ci) {
      if (this.minecraft.isSameThread()) {
         HotbarManager.unload(this.registryAccess());
      }
   }

   @Inject(
      method = {"handleSetCarriedItem"},
      at = {@At("RETURN")}
   )
   public void handleSetCarriedItem(ClientboundSetCarriedItemPacket clientboundSetCarriedItemPacket, CallbackInfo ci) {
      BuilderToolManager.setToolSlotActive(false);
      DisplayEntityManipulator.disableActive();
      MarkerEntityManipulator.disableActive();
   }

   @Inject(
      method = {"handleSetTime"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void handleSetTime(ClientboundSetTimePacket clientboundSetTimePacket, CallbackInfo ci) {
      if (ServerIntegration.shouldSuppressTimeUpdates()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"handleSetEntityData"},
      at = {@At("RETURN")}
   )
   public void handleSetEntityData(ClientboundSetEntityDataPacket clientboundSetEntityDataPacket, CallbackInfo ci) {
      LocalPlayer player = this.minecraft.player;
      if (player.getId() == clientboundSetEntityDataPacket.id() && NoClip.canNoClip(player) && player.getPose() != Pose.STANDING) {
         player.setPose(Pose.STANDING);
      }
   }

   @Inject(
      method = {"handleRespawn"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Minecraft;setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/gui/screens/ReceivingLevelScreen$Reason;)V",
         shift = Shift.AFTER
      )}
   )
   public void handleRespawn(ClientboundRespawnPacket clientboundRespawnPacket, CallbackInfo ci) {
      ResourceKey<Level> resourceKey = clientboundRespawnPacket.commonPlayerSpawnInfo().dimension();
      Axiom.getInstance().dimensionChanged(resourceKey);
   }

   @Inject(
      method = {"handleLogin"},
      at = {@At("RETURN")}
   )
   public void handleLogin(ClientboundLoginPacket clientboundLoginPacket, CallbackInfo ci) {
      ResourceKey<Level> resourceKey = clientboundLoginPacket.commonPlayerSpawnInfo().dimension();
      Axiom.getInstance().dimensionChanged(resourceKey);
   }

   @Inject(
      method = {"handleLevelChunkWithLight"},
      at = {@At("RETURN")}
   )
   public void handleLevelChunkWithLight(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
      int minSection = this.level.getMinSection();
      int maxSection = this.level.getMaxSection() - 1;

      for (int y = minSection; y <= maxSection; y++) {
         ChunkRenderOverrider.invalidateChunkSection(packet.getX(), y, packet.getZ());
      }

      BiomeOverlayRenderer.INSTANCE.markDirty(packet.getX(), packet.getZ());
      BiomeOverlayRenderer.INSTANCE.markDirty(packet.getX() + 1, packet.getZ());
      BiomeOverlayRenderer.INSTANCE.markDirty(packet.getX() - 1, packet.getZ());
      BiomeOverlayRenderer.INSTANCE.markDirty(packet.getX(), packet.getZ() + 1);
      BiomeOverlayRenderer.INSTANCE.markDirty(packet.getX(), packet.getZ() - 1);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(packet.getX(), packet.getZ());
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(packet.getX() + 1, packet.getZ());
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(packet.getX() - 1, packet.getZ());
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(packet.getX(), packet.getZ() + 1);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(packet.getX(), packet.getZ() - 1);
   }

   @Inject(
      method = {"handleChunksBiomes"},
      at = {@At("RETURN")}
   )
   public void handleChunksBiomes(ClientboundChunksBiomesPacket packet, CallbackInfo ci) {
      for (ChunkBiomeData chunkBiomeDatum : packet.chunkBiomeData()) {
         ChunkPos pos = chunkBiomeDatum.pos();
         BiomeOverlayRenderer.INSTANCE.markDirty(pos.x, pos.z);
         BiomeOverlayRenderer.INSTANCE.markDirty(pos.x + 1, pos.z);
         BiomeOverlayRenderer.INSTANCE.markDirty(pos.x - 1, pos.z);
         BiomeOverlayRenderer.INSTANCE.markDirty(pos.x, pos.z + 1);
         BiomeOverlayRenderer.INSTANCE.markDirty(pos.x, pos.z - 1);
      }
   }

   @Inject(
      method = {"handleForgetLevelChunk"},
      at = {@At("RETURN")}
   )
   public void handleForgetLevelChunk(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
      BiomeOverlayRenderer.INSTANCE.forgetChunk(packet.pos().x, packet.pos().z);
      CollisionMeshOverlayRenderer.INSTANCE.forgetChunk(packet.pos().x, packet.pos().z);
   }

   @Inject(
      method = {"handleBlockUpdate"},
      at = {@At("RETURN")}
   )
   public void handleBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
      ChunkRenderOverrider.invalidateChunkSection(packet.getPos().getX() >> 4, packet.getPos().getY() >> 4, packet.getPos().getZ() >> 4);
      int chunkX = packet.getPos().getX() >> 4;
      int chunkZ = packet.getPos().getZ() >> 4;
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX, chunkZ);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX + 1, chunkZ);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX - 1, chunkZ);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX, chunkZ + 1);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX, chunkZ - 1);
   }

   @Inject(
      method = {"handleChunkBlocksUpdate"},
      at = {@At("RETURN")}
   )
   public void handleChunkBlocksUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
      ChunkRenderOverrider.invalidateChunkSection(packet.sectionPos.x(), packet.sectionPos.y(), packet.sectionPos.z());
      int chunkX = packet.sectionPos.x();
      int chunkZ = packet.sectionPos.z();
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX, chunkZ);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX + 1, chunkZ);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX - 1, chunkZ);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX, chunkZ + 1);
      CollisionMeshOverlayRenderer.INSTANCE.markDirty(chunkX, chunkZ - 1);
   }
}
