package com.moulberry.axiom.packets;

import com.moulberry.axiom.world_properties.client.ClientWorldPropertiesRegistry;
import com.moulberry.axiom.world_properties.client.ClientWorldProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundAckWorldProperties implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:ack_world_properties");
   private final int updateId;

   public AxiomClientboundAckWorldProperties(int updateId) {
      this.updateId = updateId;
   }

   public AxiomClientboundAckWorldProperties(FriendlyByteBuf friendlyByteBuf) {
      this.updateId = friendlyByteBuf.readVarInt();
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeVarInt(this.updateId);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      for (ClientWorldProperty<?> worldProperty : ClientWorldPropertiesRegistry.PROPERTY_MAP.values()) {
         worldProperty.ackChangesUpTo(this.updateId);
      }
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundAckWorldProperties::new);
   }
}
