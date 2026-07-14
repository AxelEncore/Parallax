package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.blueprint.ServerBlueprintManager;
import com.moulberry.axiom.editor.EditorWarningType;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.DFUHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

public class AxiomServerboundHello implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:hello");
   private final int apiVersion;
   private final int dataVersion;
   private final int protocolVersion;

   public AxiomServerboundHello(int apiVersion, int dataVersion, int protocolVersion) {
      this.apiVersion = apiVersion;
      this.dataVersion = dataVersion;
      this.protocolVersion = protocolVersion;
   }

   public AxiomServerboundHello(FriendlyByteBuf friendlyByteBuf) {
      this.apiVersion = friendlyByteBuf.readVarInt();
      if (this.apiVersion != 9) {
         this.dataVersion = 0;
         this.protocolVersion = 0;
         friendlyByteBuf.readerIndex(friendlyByteBuf.writerIndex());
      } else {
         this.dataVersion = friendlyByteBuf.readVarInt();
         this.protocolVersion = friendlyByteBuf.readVarInt();
      }
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeVarInt(this.apiVersion);
      friendlyByteBuf.writeVarInt(this.dataVersion);
      friendlyByteBuf.writeVarInt(this.protocolVersion);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (!AxiomServer.hasPermission(player, AxiomPermission.USE)) {
         AxiomServer.activeAxiomPlayers.remove(player.getUUID());
         AxiomServer.failedPermissionPlayers.add(player.getUUID());
      } else if (this.apiVersion != 9) {
         player.connection.disconnect(Component.literal(AxiomI18n.get("axiom.hardcoded.unsupported_api") + this.apiVersion));
      } else if (this.protocolVersion != SharedConstants.getProtocolVersion()) {
         int serverDataVersion = DFUHelper.DATA_VERSION;
         player.connection
            .disconnect(
               Component.literal(
                  "Axiom: Incompatible data version detected (client " + this.dataVersion + ", server " + serverDataVersion + "), are you using ViaVersion?"
               )
            );
      } else {
         boolean spectatorsGenerateChunks = server.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
         if (!spectatorsGenerateChunks) {
            new AxiomClientboundEditorWarning(EditorWarningType.SPECTATORS_GENERATE_CHUNKS, true).send(player);
         }

         new AxiomClientboundEnable().send(player);
         AxiomServer.onAxiomActive(player);
         if (AxiomServer.supportsServerBlueprints()) {
            ServerBlueprintManager.sendManifest(List.of(player));
         }

         if (AxiomServer.usingDeprecatedAxiomStarPermission(player)) {
            MutableComponent text = Component.literal(
               "Axiom: Using deprecated axiom.* permission. Please switch to axiom.default for public servers, or axiom.all for private servers"
            );
            player.sendSystemMessage(text.withStyle(ChatFormatting.YELLOW));
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundHello::new);
   }
}
