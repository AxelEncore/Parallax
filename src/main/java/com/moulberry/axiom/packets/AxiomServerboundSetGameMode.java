package com.moulberry.axiom.packets;

import com.moulberry.axiom.restrictions.AxiomPermission;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class AxiomServerboundSetGameMode implements AxiomServerboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:set_gamemode");
   private final GameType gameType;

   public AxiomServerboundSetGameMode(GameType gameType) {
      this.gameType = gameType;
   }

   public AxiomServerboundSetGameMode(FriendlyByteBuf friendlyByteBuf) {
      this.gameType = GameType.byId(friendlyByteBuf.readByte());
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeByte(this.gameType.getId());
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      AxiomPermission permission = switch (this.gameType) {
         case SURVIVAL -> AxiomPermission.PLAYER_GAMEMODE_SURVIVAL;
         case CREATIVE -> AxiomPermission.PLAYER_GAMEMODE_CREATIVE;
         case ADVENTURE -> AxiomPermission.PLAYER_GAMEMODE_ADVENTURE;
         case SPECTATOR -> AxiomPermission.PLAYER_GAMEMODE_SPECTATOR;
         default -> AxiomPermission.PLAYER_GAMEMODE;
      };
      if (AxiomServerboundPacket.canUseAxiom(player, permission)) {
         player.setGameMode(this.gameType);
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundSetGameMode::new);
   }
}
