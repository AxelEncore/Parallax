package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.ChatUtils;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;

public class AxiomServerboundDeleteEntity implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:delete_entity");
   private final List<UUID> delete;
   private static final Component UNSUPPORTED_MESSAGE = Component.literal(AxiomI18n.get("axiom.hardcoded.srv_no_delete_entities"))
      .withStyle(ChatFormatting.RED);

   public AxiomServerboundDeleteEntity(List<UUID> delete) {
      this.delete = List.copyOf(delete);
   }

   public AxiomServerboundDeleteEntity(FriendlyByteBuf friendlyByteBuf) {
      this.delete = friendlyByteBuf.readList(buf -> buf.readUUID());
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeCollection(this.delete, (buf, uuid) -> buf.writeUUID(uuid));
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.ENTITY_DELETE)) {
         ServerLevel serverLevel = player.serverLevel();

         for (UUID uuid : this.delete) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity != null && !(entity instanceof Player) && !entity.hasPassenger(e -> e instanceof Player)) {
               entity.remove(RemovalReason.DISCARDED);
            }
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundDeleteEntity::new);
   }

   @Override
   public void send() {
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.DELETE_ENTITY)) {
         ChatUtils.error(UNSUPPORTED_MESSAGE);
      } else {
         AxiomServerboundPacket.super.send();
      }
   }
}
