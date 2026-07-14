package com.moulberry.axiom.world_properties.server;

import com.moulberry.axiom.packets.AxiomClientboundRegisterWorldProperties;
import com.moulberry.axiom.world_properties.AxiomGameRules;
import com.moulberry.axiom.world_properties.DefaultServerWorldProperties;
import com.moulberry.axiom.world_properties.WorldPropertyCategory;
import com.moulberry.axiom.world_properties.WorldPropertyWidgetType;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.IntegerValue;

public class ServerWorldPropertiesRegistry {
   public final LinkedHashMap<WorldPropertyCategory, List<ServerWorldProperty<?>>> propertyList = new LinkedHashMap<>();
   public final Map<ResourceLocation, ServerWorldProperty<?>> propertyMap = new HashMap<>();

   public ServerWorldPropertiesRegistry(ServerLevel serverLevel) {
      this.registerDefault(serverLevel);
   }

   public void clear() {
      this.propertyMap.clear();
      this.propertyList.clear();
   }

   public void addCategory(WorldPropertyCategory category, List<ServerWorldProperty<?>> properties) {
      this.propertyList.put(category, properties);

      for (ServerWorldProperty<?> property : properties) {
         ResourceLocation id = property.getId();
         if (this.propertyMap.containsKey(id)) {
            throw new RuntimeException("Duplicate property: " + id);
         }

         this.propertyMap.put(id, property);
      }
   }

   public void registerFor(ServerPlayer serverPlayer) {
      new AxiomClientboundRegisterWorldProperties(this.propertyList).send(serverPlayer);
   }

   public void registerDefault(ServerLevel serverLevel) {
      GameRules gameRules = serverLevel.getGameRules();
      WorldPropertyCategory timeCategory = new WorldPropertyCategory("axiom.editorui.window.world_properties.time", true);
      ServerWorldProperty<Unit> time = new ServerWorldProperty<>(
         ResourceLocation.parse("axiom:time"), "axiom.editorui.window.world_properties.time", true, WorldPropertyWidgetType.TIME, Unit.INSTANCE, unit -> false
      );
      this.addCategory(timeCategory, List.of(time));
      WorldPropertyCategory weatherCategory = new WorldPropertyCategory("axiom.editorui.window.world_properties.weather", true);
      ServerWorldProperty<Boolean> pauseWeather = new ServerWorldProperty<>(
         DefaultServerWorldProperties.PAUSE_WEATHER,
         "axiom.editorui.window.world_properties.pause_weather",
         true,
         WorldPropertyWidgetType.CHECKBOX,
         !((BooleanValue)gameRules.getRule(GameRules.RULE_WEATHER_CYCLE)).get(),
         bool -> {
            ((BooleanValue)gameRules.getRule(GameRules.RULE_WEATHER_CYCLE)).set(!bool, serverLevel.getServer());
            return false;
         }
      );
      ServerWorldProperty<Integer> weatherType = new ServerWorldProperty<>(
         DefaultServerWorldProperties.WEATHER_TYPE,
         "axiom.editorui.window.world_properties.clear_weather",
         true,
         new WorldPropertyWidgetType.ButtonArray(
            List.of("axiom.editorui.window.world_properties.rain_weather", "axiom.editorui.window.world_properties.thunder_weather")
         ),
         0,
         index -> {
            if (index == 0) {
               serverLevel.setWeatherParameters(ServerLevel.RAIN_DELAY.sample(serverLevel.getRandom()), 0, false, false);
            } else if (index == 1) {
               serverLevel.setWeatherParameters(0, ServerLevel.RAIN_DURATION.sample(serverLevel.getRandom()), true, false);
            } else if (index == 2) {
               serverLevel.setWeatherParameters(0, ServerLevel.THUNDER_DURATION.sample(serverLevel.getRandom()), true, true);
            }

            return false;
         }
      );
      this.addCategory(weatherCategory, List.of(pauseWeather, weatherType));
      WorldPropertyCategory playerCategory = new WorldPropertyCategory("axiom.editorui.window.world_properties.player", true);
      ServerWorldProperty<Boolean> playerInvulnerability = new ServerWorldProperty<>(
         DefaultServerWorldProperties.PLAYER_INVULNERABILITY,
         "axiom.editorui.window.world_properties.player_invulnerability",
         true,
         WorldPropertyWidgetType.CHECKBOX,
         ((BooleanValue)gameRules.getRule(AxiomGameRules.RULE_PLAYERINVULNERABILITY)).get(),
         bool -> {
            ((BooleanValue)gameRules.getRule(AxiomGameRules.RULE_PLAYERINVULNERABILITY)).set(bool, serverLevel.getServer());
            return false;
         }
      );
      ServerWorldProperty<Boolean> trampleFarmland = new ServerWorldProperty<>(
         DefaultServerWorldProperties.TRAMPLE_FARMLAND,
         "axiom.editorui.window.world_properties.trample_farmland",
         true,
         WorldPropertyWidgetType.CHECKBOX,
         ((BooleanValue)gameRules.getRule(AxiomGameRules.RULE_DOTRAMPLEFARMLAND)).get(),
         bool -> {
            ((BooleanValue)gameRules.getRule(AxiomGameRules.RULE_DOTRAMPLEFARMLAND)).set(bool, serverLevel.getServer());
            return false;
         }
      );
      ServerWorldProperty<Boolean> mobSpawning = new ServerWorldProperty<>(
         DefaultServerWorldProperties.MOB_SPAWNING,
         "axiom.editorui.window.world_properties.mob_spawning",
         true,
         WorldPropertyWidgetType.CHECKBOX,
         ((BooleanValue)gameRules.getRule(GameRules.RULE_DOMOBSPAWNING)).get(),
         bool -> {
            ((BooleanValue)gameRules.getRule(GameRules.RULE_DOMOBSPAWNING)).set(bool, serverLevel.getServer());
            return false;
         }
      );
      this.addCategory(playerCategory, List.of(playerInvulnerability, trampleFarmland, mobSpawning));
      WorldPropertyCategory blocksCategory = new WorldPropertyCategory("axiom.editorui.window.world_properties.blocks", true);
      ServerWorldProperty<Boolean> blockDrops = new ServerWorldProperty<>(
         DefaultServerWorldProperties.BLOCK_DROPS,
         "axiom.editorui.window.world_properties.block_drops",
         true,
         WorldPropertyWidgetType.CHECKBOX,
         ((BooleanValue)gameRules.getRule(AxiomGameRules.RULE_DOBLOCKDROPS)).get(),
         bool -> {
            ((BooleanValue)gameRules.getRule(AxiomGameRules.RULE_DOBLOCKDROPS)).set(bool, serverLevel.getServer());
            ((BooleanValue)gameRules.getRule(GameRules.RULE_DOBLOCKDROPS)).set(bool, serverLevel.getServer());
            return false;
         }
      );
      ServerWorldProperty<Boolean> blockGravity = new ServerWorldProperty<>(
         DefaultServerWorldProperties.BLOCK_GRAVITY,
         "axiom.editorui.window.world_properties.block_gravity",
         true,
         WorldPropertyWidgetType.CHECKBOX,
         ((BooleanValue)gameRules.getRule(AxiomGameRules.RULE_DOBLOCKGRAVITY)).get(),
         bool -> {
            ((BooleanValue)gameRules.getRule(AxiomGameRules.RULE_DOBLOCKGRAVITY)).set(bool, serverLevel.getServer());
            return false;
         }
      );
      ServerWorldProperty<Boolean> fireTick = new ServerWorldProperty<>(
         DefaultServerWorldProperties.FIRE_TICK,
         "axiom.editorui.window.world_properties.fire_tick",
         true,
         WorldPropertyWidgetType.CHECKBOX,
         gameRules.getBoolean(GameRules.RULE_DOFIRETICK),
         bool -> {
            ((BooleanValue)gameRules.getRule(GameRules.RULE_DOFIRETICK)).set(bool, serverLevel.getServer());
            return false;
         }
      );
      ServerWorldProperty<Integer> randomTickSpeed = new ServerWorldProperty<>(
         DefaultServerWorldProperties.RANDOM_TICK_SPEED,
         "axiom.editorui.window.world_properties.random_tick_speed",
         true,
         new WorldPropertyWidgetType.Slider(0, 50),
         ((IntegerValue)gameRules.getRule(GameRules.RULE_RANDOMTICKING)).get(),
         integer -> {
            ((IntegerValue)gameRules.getRule(GameRules.RULE_RANDOMTICKING)).set(integer, serverLevel.getServer());
            return false;
         }
      );
      this.addCategory(blocksCategory, List.of(blockDrops, blockGravity, fireTick, randomTickSpeed));
   }
}
