package com.moulberry.axiom.world_properties;

import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.hooks.ServerLevelExt;
import com.moulberry.axiom.packets.AxiomClientboundSetWorldProperty;
import com.moulberry.axiom.world_properties.server.ServerWorldPropertiesRegistry;
import com.moulberry.axiom.world_properties.server.ServerWorldProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Type;

public class AxiomGameRules {
   public static Key<BooleanValue> RULE_DOBLOCKDROPS;
   public static Key<BooleanValue> RULE_DOBLOCKGRAVITY;
   public static Key<BooleanValue> RULE_DOTRAMPLEFARMLAND;
   public static Key<BooleanValue> RULE_PLAYERINVULNERABILITY;

   public static void register() {
      Type<BooleanValue> doBlockDrops = GameRules.BooleanValue.create(true, (minecraftServer, booleanValue) -> {
         if (minecraftServer != null) {
            setBoolean(minecraftServer, DefaultServerWorldProperties.BLOCK_DROPS, booleanValue.get());
         }
      });
      RULE_DOBLOCKDROPS = GameRules.register("axiomDoBlockDrops", Category.DROPS, doBlockDrops);
      Type<BooleanValue> doBlockGravity = GameRules.BooleanValue.create(true, (minecraftServer, booleanValue) -> {
         if (minecraftServer != null) {
            setBoolean(minecraftServer, DefaultServerWorldProperties.BLOCK_GRAVITY, booleanValue.get());
         }
      });
      RULE_DOBLOCKGRAVITY = GameRules.register("axiomDoBlockGravity", Category.UPDATES, doBlockGravity);
      Type<BooleanValue> doTrampleFarmland = GameRules.BooleanValue.create(true, (minecraftServer, booleanValue) -> {
         if (minecraftServer != null) {
            setBoolean(minecraftServer, DefaultServerWorldProperties.TRAMPLE_FARMLAND, booleanValue.get());
         }
      });
      RULE_DOTRAMPLEFARMLAND = GameRules.register("axiomDoTrampleFarmland", Category.UPDATES, doTrampleFarmland);
      Type<BooleanValue> playerInvulnerability = GameRules.BooleanValue.create(false, (minecraftServer, booleanValue) -> {
         if (minecraftServer != null) {
            setBoolean(minecraftServer, DefaultServerWorldProperties.PLAYER_INVULNERABILITY, booleanValue.get());
         }
      });
      RULE_PLAYERINVULNERABILITY = GameRules.register("axiomPlayerInvulnerability", Category.PLAYER, playerInvulnerability);
   }

   private static void setBoolean(MinecraftServer minecraftServer, ResourceLocation resourceLocation, boolean value) {
      for (ServerLevel level : minecraftServer.getAllLevels()) {
         ServerWorldPropertiesRegistry propertiesRegistry = ((ServerLevelExt)level).axiom$getWorldProperties();
         ServerWorldProperty<?> property = propertiesRegistry.propertyMap.get(resourceLocation);
         ((ServerWorldProperty<Boolean>)property).setValue(value);
      }

      AxiomClientboundSetWorldProperty packet = new AxiomClientboundSetWorldProperty(
         resourceLocation, WorldPropertyDataType.BOOLEAN.getTypeId(), WorldPropertyDataType.BOOLEAN.serialize(value)
      );

      for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
         if (AxiomServer.canUseAxiom(player)) {
            packet.send(player);
         }
      }
   }
}
