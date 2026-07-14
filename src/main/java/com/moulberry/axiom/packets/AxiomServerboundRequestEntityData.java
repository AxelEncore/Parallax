package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.EntityDataUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AxiomServerboundRequestEntityData implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:request_entity_data");
   private final long id;
   private final List<UUID> request;
   private static final Component UNSUPPORTED_MESSAGE = Component.literal(AxiomI18n.get("axiom.hardcoded.srv_no_request_entity"))
      .withStyle(ChatFormatting.RED);

   public AxiomServerboundRequestEntityData(long id, List<UUID> request) {
      this.id = id;
      this.request = List.copyOf(request);
   }

   public AxiomServerboundRequestEntityData(FriendlyByteBuf friendlyByteBuf) {
      this.id = friendlyByteBuf.readLong();
      this.request = friendlyByteBuf.readList(buf -> buf.readUUID());
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeLong(this.id);
      friendlyByteBuf.writeCollection(this.request, (buf, uuid) -> buf.writeUUID(uuid));
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (!AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.ENTITY_REQUESTDATA)) {
         new AxiomClientboundResponseEntityData(this.id, true, Map.of()).send(player);
      } else {
         ServerLevel serverLevel = player.serverLevel();
         int maxPacketSize = 1048576;
         int remainingBytes = 1048576;
         Map<UUID, CompoundTag> entityData = new HashMap<>();
         Set<UUID> visitedEntities = new HashSet<>();

         for (UUID uuid : this.request) {
            if (visitedEntities.add(uuid)) {
               Entity entity = serverLevel.getEntity(uuid);
               if (entity != null && !(entity instanceof Player)) {
                  CompoundTag entityTag = EntityDataUtils.saveRoot(entity);
                  if (entityTag != null) {
                     int size = entityTag.sizeInBytes();
                     if (size >= 1048576) {
                        new AxiomClientboundResponseEntityData(this.id, false, Map.of(uuid, entityTag)).send(player);
                     } else {
                        if (remainingBytes - size < 0) {
                           new AxiomClientboundResponseEntityData(this.id, false, entityData).send(player);
                           entityData.clear();
                           remainingBytes = 1048576;
                        }

                        entityData.put(uuid, entityTag);
                        remainingBytes -= size;
                     }
                  }
               }
            }
         }

         new AxiomClientboundResponseEntityData(this.id, true, entityData).send(player);
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundRequestEntityData::new);
   }

   @Override
   public void send() {
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.REQUEST_ENTITY)) {
         ChatUtils.error(UNSUPPORTED_MESSAGE);
      } else {
         AxiomServerboundPacket.super.send();
      }
   }
}
