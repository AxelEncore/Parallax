package com.moulberry.axiom.packets;

import com.moulberry.axiom.marker.MarkerEntityManipulator;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundMarkerNbtResponse implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:marker_nbt_response");
   public final UUID uuid;
   public final CompoundTag data;

   public AxiomClientboundMarkerNbtResponse(UUID uuid, CompoundTag data) {
      this.uuid = uuid;
      this.data = data.copy();
   }

   public AxiomClientboundMarkerNbtResponse(FriendlyByteBuf friendlyByteBuf) {
      this.uuid = friendlyByteBuf.readUUID();
      this.data = friendlyByteBuf.readNbt();
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeUUID(this.uuid);
      friendlyByteBuf.writeNbt(this.data);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      MarkerEntityManipulator.receivedNbtData(this.uuid, this.data);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundMarkerNbtResponse::new);
   }
}
