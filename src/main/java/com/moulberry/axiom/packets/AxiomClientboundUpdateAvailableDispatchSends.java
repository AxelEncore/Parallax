package com.moulberry.axiom.packets;

import com.moulberry.axiom.world_modification.Dispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundUpdateAvailableDispatchSends implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:update_available_dispatch_sends");
   private final int add;
   private final int max;

   public AxiomClientboundUpdateAvailableDispatchSends(int add, int max) {
      this.add = add;
      this.max = max;
   }

   public AxiomClientboundUpdateAvailableDispatchSends(FriendlyByteBuf friendlyByteBuf) {
      this.add = friendlyByteBuf.readVarInt();
      this.max = friendlyByteBuf.readVarInt();
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeVarInt(this.add);
      friendlyByteBuf.writeVarInt(this.max);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      Dispatcher.updateAvailableDispatchSends(this.add, this.max);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundUpdateAvailableDispatchSends::new);
   }
}
