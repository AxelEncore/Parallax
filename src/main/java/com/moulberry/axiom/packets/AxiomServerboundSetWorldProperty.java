package com.moulberry.axiom.packets;

import com.moulberry.axiom.hooks.ServerGamePacketListenerImplExt;
import com.moulberry.axiom.hooks.ServerLevelExt;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.world_properties.server.ServerWorldPropertiesRegistry;
import com.moulberry.axiom.world_properties.server.ServerWorldProperty;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class AxiomServerboundSetWorldProperty implements AxiomServerboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:set_world_property");
   private final ResourceLocation id;
   private final int type;
   private final byte[] bytes;
   private final int updateId;

   public AxiomServerboundSetWorldProperty(ResourceLocation id, int type, byte[] bytes, int updateId) {
      this.id = id;
      this.type = type;
      this.bytes = bytes;
      this.updateId = updateId;
   }

   public AxiomServerboundSetWorldProperty(FriendlyByteBuf friendlyByteBuf) {
      this.id = friendlyByteBuf.readResourceLocation();
      this.type = friendlyByteBuf.readVarInt();
      this.bytes = friendlyByteBuf.readByteArray();
      this.updateId = friendlyByteBuf.readVarInt();
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeResourceLocation(this.id);
      friendlyByteBuf.writeVarInt(this.type);
      friendlyByteBuf.writeByteArray(this.bytes);
      friendlyByteBuf.writeVarInt(this.updateId);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.WORLD_PROPERTY)) {
         ServerLevel serverLevel = player.serverLevel();
         ServerWorldPropertiesRegistry properties = ((ServerLevelExt)serverLevel).axiom$getWorldProperties();
         ServerWorldProperty<?> property = properties.propertyMap.get(this.id);
         if (property != null && property.getType().getTypeId() == this.type) {
            property.update(serverLevel, this.bytes);
         }

         ((ServerGamePacketListenerImplExt)player.connection).ackWorldPropertiesUpTo(this.updateId);
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundSetWorldProperty::new);
   }
}
