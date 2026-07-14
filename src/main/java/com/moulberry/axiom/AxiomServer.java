package com.moulberry.axiom;

import com.moulberry.axiom.hooks.ServerPlayerExt;
import com.moulberry.axiom.packets.AxiomClientboundEnable;
import com.moulberry.axiom.packets.AxiomClientboundRedoHandshake;
import com.moulberry.axiom.packets.AxiomClientboundRestrictions;
import com.moulberry.axiom.packets.AxiomClientboundUpdateAvailableDispatchSends;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.restrictions.AxiomPermissionSet;
import com.moulberry.axiom.restrictions.Restrictions;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import com.moulberry.axiom.platform.TriState;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class AxiomServer {
   private static final int ALLOWED_DISPATCH_SENDS_PER_SECOND = 1024;
   public static final Set<UUID> activeAxiomPlayers = new HashSet<>();
   public static final Set<UUID> failedPermissionPlayers = new HashSet<>();
   private static final Map<UUID, Restrictions> playerRestrictions = new HashMap<>();
   private static final Map<UUID, AxiomPermissionSet> playerPermissions = new HashMap<>();
   private static final Object2IntOpenHashMap<UUID> availableDispatchSends = new Object2IntOpenHashMap();
   private static boolean checkedForFabricPermissionAPI = false;
   private static Method permissionCheckMethod = null;

   public static void register() {
      NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
         MinecraftServer minecraftServer = event.getServer();
         Set<UUID> stillActiveAxiomPlayers = new HashSet<>();
         Set<UUID> stillFailedAxiomPlayers = new HashSet<>();
         playerPermissions.clear();

         for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            if (activeAxiomPlayers.contains(player.getUUID())) {
               if (!hasPermission(player, AxiomPermission.USE)) {
                  new AxiomClientboundEnable(false, null).send(player);
                  failedPermissionPlayers.add(player.getUUID());
               } else {
                  stillActiveAxiomPlayers.add(player.getUUID());
                  tickPlayer(player);
               }
            } else if (failedPermissionPlayers.contains(player.getUUID())) {
               if (hasPermission(player, AxiomPermission.USE)) {
                  new AxiomClientboundRedoHandshake().send(player);
                  failedPermissionPlayers.remove(player.getUUID());
               } else {
                  stillFailedAxiomPlayers.add(player.getUUID());
               }
            }
         }

         activeAxiomPlayers.retainAll(stillActiveAxiomPlayers);
         availableDispatchSends.keySet().retainAll(stillActiveAxiomPlayers);
         playerRestrictions.keySet().retainAll(stillActiveAxiomPlayers);
         failedPermissionPlayers.retainAll(stillFailedAxiomPlayers);
      });
      NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
         Player player = event.getEntity();
         playerPermissions.remove(player.getUUID());
         playerRestrictions.remove(player.getUUID());
         availableDispatchSends.removeInt(player.getUUID());
      });
   }

   public static boolean supportsServerBlueprints() {
      return AxiomPlatform.isDedicatedServer();
   }

   public static boolean consumeDispatchSends(ServerPlayer player, int sends, int clientAvailableDispatchSends) {
      int currentSends = availableDispatchSends.getOrDefault(player.getUUID(), 20480);
      currentSends -= sends * 20;
      currentSends = Math.min(currentSends, clientAvailableDispatchSends * 20);
      availableDispatchSends.put(player.getUUID(), currentSends);
      if (currentSends < -20480) {
         player.connection.disconnect(Component.literal("You are sending updates too fast!"));
         return false;
      } else {
         return true;
      }
   }

   public static void onAxiomActive(ServerPlayer player) {
      activeAxiomPlayers.add(player.getUUID());
      failedPermissionPlayers.remove(player.getUUID());
      playerPermissions.remove(player.getUUID());
      playerRestrictions.remove(player.getUUID());
      tickPlayer(player);
   }

   private static void tickPlayer(ServerPlayer player) {
      if (!availableDispatchSends.containsKey(player.getUUID())) {
         availableDispatchSends.put(player.getUUID(), 20480);
         new AxiomClientboundUpdateAvailableDispatchSends(1024, 1024).send(player);
      } else {
         int previousAllowed20 = availableDispatchSends.getInt(player.getUUID());
         int newAllowed20 = Math.min(20480, previousAllowed20 + 1024);
         availableDispatchSends.put(player.getUUID(), newAllowed20);
         int previousAllowed = previousAllowed20 / 20;
         int newAllowed = newAllowed20 / 20;
         if (previousAllowed != newAllowed) {
            new AxiomClientboundUpdateAvailableDispatchSends(newAllowed - previousAllowed, 1024).send(player);
         }
      }

      Restrictions restrictions = calculateRestrictions(player);
      boolean restrictionsChanged;
      if (playerRestrictions.containsKey(player.getUUID())) {
         Restrictions oldRestrictions = playerRestrictions.get(player.getUUID());
         restrictionsChanged = !Objects.equals(restrictions, oldRestrictions);
      } else {
         restrictionsChanged = true;
      }

      if (restrictionsChanged) {
         new AxiomClientboundRestrictions(restrictions).send(player);
         playerRestrictions.put(player.getUUID(), restrictions);
      }
   }

   private static boolean isOp(ServerPlayer serverPlayer) {
      return serverPlayer.hasPermissions(2);
   }

   private static Restrictions calculateRestrictions(ServerPlayer player) {
      if (isOp(player)) {
         Restrictions restrictions = new Restrictions();
         restrictions.allowedPermissions = EnumSet.of(AxiomPermission.ALL);
         return restrictions;
      } else {
         AxiomPermissionSet permissionSet = getPermissions(player);
         if (permissionSet.contains(AxiomPermission.ALL)) {
            Restrictions restrictions = new Restrictions();
            restrictions.allowedPermissions = EnumSet.of(AxiomPermission.ALL);
            return restrictions;
         } else {
            EnumSet<AxiomPermission> allowed = EnumSet.noneOf(AxiomPermission.class);
            EnumSet<AxiomPermission> denied = EnumSet.noneOf(AxiomPermission.class);

            for (AxiomPermission permission : permissionSet.explicitlyAllowed) {
               if (permission.parent == null || !permissionSet.explicitlyAllowed.contains(permission.parent)) {
                  allowed.add(permission);
               }
            }

            for (AxiomPermission permissionx : permissionSet.explicitlyDenied) {
               if (permissionx.parent == null || !permissionSet.explicitlyDenied.contains(permissionx.parent)) {
                  denied.add(permissionx);
               }
            }

            Restrictions restrictions = new Restrictions();
            restrictions.allowedPermissions = allowed;
            restrictions.deniedPermissions = denied;
            return restrictions;
         }
      }
   }

   private static AxiomPermissionSet calculatePermissions(ServerPlayer player) {
      if (isOp(player)) {
         return AxiomPermissionSet.ALL;
      } else {
         checkForFabricPermissionAPI();
         if (permissionCheckMethod == null) {
            return AxiomPermissionSet.NONE;
         } else {
            try {
               EnumSet<AxiomPermission> allowed = EnumSet.noneOf(AxiomPermission.class);
               EnumSet<AxiomPermission> denied = EnumSet.noneOf(AxiomPermission.class);

               for (AxiomPermission permission : AxiomPermission.values()) {
                  TriState value = (TriState)permissionCheckMethod.invoke(null, player, permission.getPermissionNode());
                  switch (value) {
                     case FALSE:
                        denied.add(permission);
                     case DEFAULT:
                     default:
                        break;
                     case TRUE:
                        allowed.add(permission);
                  }
               }

               return new AxiomPermissionSet(allowed, denied);
            } catch (Throwable var8) {
               Axiom.LOGGER.error("Error when trying to check permission using fabric permission api", var8);
               permissionCheckMethod = null;
               return AxiomPermissionSet.NONE;
            }
         }
      }
   }

   public static boolean usingDeprecatedAxiomStarPermission(ServerPlayer player) {
      checkForFabricPermissionAPI();
      if (permissionCheckMethod == null) {
         return false;
      } else if (isOp(player)) {
         return false;
      } else {
         try {
            TriState value = (TriState)permissionCheckMethod.invoke(null, player, "*");
            if (value == TriState.TRUE) {
               return false;
            } else {
               value = (TriState)permissionCheckMethod.invoke(null, player, "axiom.*");
               return value == TriState.TRUE;
            }
         } catch (Throwable var2) {
            Axiom.LOGGER.error("Error when trying to check permission using fabric permission api", var2);
            permissionCheckMethod = null;
            return false;
         }
      }
   }

   public static boolean isNoPhysicalTrigger(ServerPlayer serverPlayer) {
      return canUseAxiom(serverPlayer, AxiomPermission.PLAYER_SETNOPHYSICALTRIGGER) && ((ServerPlayerExt)serverPlayer).axiom$isNoPhysicalTrigger();
   }

   public static boolean canUseAxiom(ServerPlayer player) {
      return activeAxiomPlayers.contains(player.getUUID());
   }

   public static boolean canUseAxiom(ServerPlayer player, AxiomPermission axiomPermission) {
      return activeAxiomPlayers.contains(player.getUUID()) && hasPermission(player, axiomPermission);
   }

   private static void checkForFabricPermissionAPI() {
      if (!checkedForFabricPermissionAPI) {
         checkedForFabricPermissionAPI = true;

         try {
            Class<?> clazz = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
            permissionCheckMethod = clazz.getMethod("getPermissionValue", Entity.class, String.class);
         } catch (Throwable var1) {
         }
      }
   }

   public static AxiomPermissionSet getPermissions(ServerPlayer player) {
      return playerPermissions.computeIfAbsent(player.getUUID(), uuid -> calculatePermissions(player));
   }

   public static boolean hasPermission(ServerPlayer player, AxiomPermission axiomPermission) {
      return isOp(player) ? true : getPermissions(player).contains(axiomPermission);
   }
}
