package com.moulberry.axiom.integration;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.packets.AxiomServerboundSetFlySpeed;
import com.moulberry.axiom.packets.AxiomServerboundSetGameMode;
import com.moulberry.axiom.packets.AxiomServerboundSetTime;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.restrictions.AxiomPermission;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.GameType;

public class ServerIntegration {
   private static Float pendingFlySpeed = null;
   private static GameType pendingGameType = null;
   private static Integer pendingTime = null;
   private static Boolean pendingTimeFrozen = null;
   private static List<Packet<?>> pendingPackets = new ArrayList<>();
   private static int suppressTimeUpdateTicks = 0;

   public static void changeFlySpeed(float flySpeed) {
      if (ClientEvents.serverSupportsProtocol(SupportedProtocol.SET_FLY_SPEED)) {
         flySpeed = Math.max(-1.0F, Math.min(1.0F, flySpeed));
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            Abilities abilities = player.getAbilities();
            abilities.setFlyingSpeed(flySpeed);
            pendingFlySpeed = flySpeed;
         }
      }
   }

   public static void syncFlySpeed() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         Abilities abilities = player.getAbilities();
         changeFlySpeed(abilities.getFlyingSpeed());
      }
   }

   public static void changeGameMode(GameType gameType) {
      AxiomPermission permission = switch (gameType) {
         case SURVIVAL -> AxiomPermission.PLAYER_GAMEMODE_SURVIVAL;
         case CREATIVE -> AxiomPermission.PLAYER_GAMEMODE_CREATIVE;
         case ADVENTURE -> AxiomPermission.PLAYER_GAMEMODE_ADVENTURE;
         case SPECTATOR -> AxiomPermission.PLAYER_GAMEMODE_SPECTATOR;
         default -> AxiomPermission.PLAYER_GAMEMODE;
      };
      if (AxiomClient.hasPermission(permission)) {
         MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
         if (gameMode != null) {
            gameMode.setLocalMode(gameType);
         }

         pendingGameType = gameType;
      }
   }

   public static void sendPacketAfterUpdates(Packet<?> packet) {
      pendingPackets.add(packet);
   }

   public static GameType getGameType() {
      MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
      return gameMode != null ? gameMode.getPlayerMode() : null;
   }

   public static void changeTime(int time) {
      if (ClientEvents.serverSupportsProtocol(SupportedProtocol.SET_WORLD_TIME)) {
         suppressTimeUpdateTicks = 20;
         pendingTime = time;
      }
   }

   public static void changeTimeFrozen(boolean frozen) {
      if (ClientEvents.serverSupportsProtocol(SupportedProtocol.SET_WORLD_TIME)) {
         suppressTimeUpdateTicks = 20;
         pendingTimeFrozen = frozen;
      }
   }

   public static boolean shouldSuppressTimeUpdates() {
      return suppressTimeUpdateTicks > 0;
   }

   public static void sendChangeGameModeImmediately(GameType gameType) {
      AxiomPermission permission = switch (gameType) {
         case SURVIVAL -> AxiomPermission.PLAYER_GAMEMODE_SURVIVAL;
         case CREATIVE -> AxiomPermission.PLAYER_GAMEMODE_CREATIVE;
         case ADVENTURE -> AxiomPermission.PLAYER_GAMEMODE_ADVENTURE;
         case SPECTATOR -> AxiomPermission.PLAYER_GAMEMODE_SPECTATOR;
         default -> AxiomPermission.PLAYER_GAMEMODE;
      };
      if (AxiomClient.hasPermission(permission)) {
         new AxiomServerboundSetGameMode(gameType).send();
      }
   }

   public static void sendPendingUpdates() {
      if (suppressTimeUpdateTicks > 0) {
         suppressTimeUpdateTicks--;
      }

      LocalPlayer player = Minecraft.getInstance().player;
      ClientLevel level = Minecraft.getInstance().level;
      if (player != null && level != null) {
         if (pendingGameType != null) {
            new AxiomServerboundSetGameMode(pendingGameType).send();
         }

         if (pendingFlySpeed != null) {
            new AxiomServerboundSetFlySpeed(pendingFlySpeed).send();
         }

         if (pendingTime != null) {
            new AxiomServerboundSetTime(level.dimension(), pendingTime, pendingTimeFrozen).send();
         } else if (pendingTimeFrozen != null && pendingTimeFrozen) {
            int currentTime = (int)(level.getDayTime() % 24000L);
            new AxiomServerboundSetTime(level.dimension(), currentTime, pendingTimeFrozen).send();
         } else if (pendingTimeFrozen != null && !pendingTimeFrozen) {
            new AxiomServerboundSetTime(level.dimension(), null, pendingTimeFrozen).send();
         }

         for (Packet<?> packet : pendingPackets) {
            player.connection.send(packet);
         }

         pendingGameType = null;
         pendingFlySpeed = null;
         pendingTime = null;
         pendingTimeFrozen = null;
         pendingPackets.clear();
      } else {
         pendingGameType = null;
         pendingFlySpeed = null;
         pendingTime = null;
         pendingTimeFrozen = null;
         pendingPackets.clear();
      }
   }
}
