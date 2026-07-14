package com.moulberry.axiom.packets;

import com.moulberry.axiom.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundRedoHandshake implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:redo_handshake");

   public AxiomClientboundRedoHandshake() {
   }

   public AxiomClientboundRedoHandshake(FriendlyByteBuf friendlyByteBuf) {
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      ClientEvents.handshake();
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundRedoHandshake::new);
   }
}
