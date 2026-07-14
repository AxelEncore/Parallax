package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.hooks.MarkerEntityExt;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.ChatUtils;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Marker;

public class AxiomServerboundMarkerNbtRequest implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:marker_nbt_request");
   public static int REASON_COPY = 0;
   public static int REASON_RIGHT_CLICK = 1;
   private final UUID uuid;
   private final int reason;
   private static final Component UNSUPPORTED_MESSAGE = Component.literal(AxiomI18n.get("axiom.hardcoded.srv_no_manip_entities"))
      .withStyle(ChatFormatting.RED);

   public AxiomServerboundMarkerNbtRequest(UUID uuid, int reason) {
      this.uuid = uuid;
      this.reason = reason;
   }

   public AxiomServerboundMarkerNbtRequest(FriendlyByteBuf friendlyByteBuf) {
      this.uuid = friendlyByteBuf.readUUID();
      this.reason = friendlyByteBuf.readVarInt();
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeUUID(this.uuid);
      friendlyByteBuf.writeVarInt(this.reason);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.ENTITY_REQUESTDATA)) {
         ServerLevel serverLevel = player.serverLevel();
         if (serverLevel.getEntity(this.uuid) instanceof Marker marker) {
            new AxiomClientboundMarkerNbtResponse(this.uuid, ((MarkerEntityExt)marker).axiom$getData()).send(player);
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundMarkerNbtRequest::new);
   }

   @Override
   public void send() {
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.MANIPULATE_ENTITY)) {
         ChatUtils.error(UNSUPPORTED_MESSAGE);
      } else {
         AxiomServerboundPacket.super.send();
      }
   }
}
