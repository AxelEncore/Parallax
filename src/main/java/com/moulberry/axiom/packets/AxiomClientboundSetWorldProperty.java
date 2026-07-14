package com.moulberry.axiom.packets;

import com.moulberry.axiom.world_properties.client.ClientWorldPropertiesRegistry;
import com.moulberry.axiom.world_properties.client.ClientWorldProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundSetWorldProperty implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:set_world_property");
   private final ResourceLocation id;
   private final int type;
   private final byte[] bytes;

   public AxiomClientboundSetWorldProperty(ResourceLocation id, int type, byte[] bytes) {
      this.id = id;
      this.type = type;
      this.bytes = bytes;
   }

   public AxiomClientboundSetWorldProperty(FriendlyByteBuf friendlyByteBuf) {
      this.id = friendlyByteBuf.readResourceLocation();
      this.type = friendlyByteBuf.readVarInt();
      this.bytes = friendlyByteBuf.readByteArray();
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeResourceLocation(this.id);
      friendlyByteBuf.writeVarInt(this.type);
      friendlyByteBuf.writeByteArray(this.bytes);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      ClientWorldProperty<?> worldProperty = ClientWorldPropertiesRegistry.PROPERTY_MAP.get(this.id);
      if (worldProperty != null && worldProperty.getType().getTypeId() == this.type) {
         worldProperty.setRemoteValue(this.bytes);
      }
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundSetWorldProperty::new);
   }
}
