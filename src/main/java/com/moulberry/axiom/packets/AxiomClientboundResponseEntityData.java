package com.moulberry.axiom.packets;

import com.moulberry.axiom.world_modification.Dispatcher;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundResponseEntityData implements AxiomClientboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:response_entity_data");
   private final long id;
   private final boolean finished;
   private final Map<UUID, CompoundTag> entityData;

   public AxiomClientboundResponseEntityData(long id, boolean finished, Map<UUID, CompoundTag> entityData) {
      this.id = id;
      this.finished = finished;
      this.entityData = Map.copyOf(entityData);
   }

   public static AxiomClientboundResponseEntityData read(FriendlyByteBuf friendlyByteBuf) {
      long id = friendlyByteBuf.readLong();
      boolean finished = friendlyByteBuf.readBoolean();
      Map<UUID, CompoundTag> entityData = friendlyByteBuf.readMap(buf -> buf.readUUID(), buf -> buf.readNbt());
      return new AxiomClientboundResponseEntityData(id, finished, entityData);
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeLong(this.id);
      friendlyByteBuf.writeBoolean(this.finished);
      friendlyByteBuf.writeMap(this.entityData, (buf, uuid) -> buf.writeUUID(uuid), (buf, nbt) -> buf.writeNbt(nbt));
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      Dispatcher.finishRequestEntityData(this.id, this.finished, this.entityData);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundResponseEntityData::read);
   }
}
