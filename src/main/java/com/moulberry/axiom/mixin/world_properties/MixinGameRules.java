package com.moulberry.axiom.mixin.world_properties;

import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.editor.EditorWarningType;
import com.moulberry.axiom.packets.AxiomClientboundEditorWarning;
import com.moulberry.axiom.packets.AxiomClientboundSetWorldProperty;
import com.moulberry.axiom.world_properties.DefaultServerWorldProperties;
import com.moulberry.axiom.world_properties.WorldPropertyDataType;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.GameRules.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRules.class})
public class MixinGameRules {
   @Shadow
   @Final
   private static Map<Key<?>, Type<?>> GAME_RULE_TYPES;

   @Inject(
      method = {"<clinit>"},
      at = {@At("RETURN")}
   )
   private static void staticInitEnd(CallbackInfo ci) {
      for (Entry<Key<?>, Type<?>> entry : GAME_RULE_TYPES.entrySet()) {
         if (entry.getKey() == GameRules.RULE_DOMOBSPAWNING) {
            Type<BooleanValue> type = (Type<BooleanValue>)entry.getValue();
            BiConsumer<MinecraftServer, BooleanValue> callback = type.callback;
            type.callback = (server, booleanValue) -> {
               callback.accept(server, booleanValue);
               AxiomClientboundSetWorldProperty packet = new AxiomClientboundSetWorldProperty(
                  DefaultServerWorldProperties.MOB_SPAWNING,
                  WorldPropertyDataType.BOOLEAN.getTypeId(),
                  WorldPropertyDataType.BOOLEAN.serialize(booleanValue.get())
               );

               for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                  if (AxiomServer.canUseAxiom(player)) {
                     packet.send(player);
                  }
               }
            };
         } else if (entry.getKey() == GameRules.RULE_DOFIRETICK) {
            Type<BooleanValue> type = (Type<BooleanValue>)entry.getValue();
            BiConsumer<MinecraftServer, BooleanValue> callback = type.callback;
            type.callback = (server, booleanValue) -> {
               callback.accept(server, booleanValue);
               AxiomClientboundSetWorldProperty packet = new AxiomClientboundSetWorldProperty(
                  DefaultServerWorldProperties.FIRE_TICK,
                  WorldPropertyDataType.BOOLEAN.getTypeId(),
                  WorldPropertyDataType.BOOLEAN.serialize(booleanValue.get())
               );

               for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                  if (AxiomServer.canUseAxiom(player)) {
                     packet.send(player);
                  }
               }
            };
         } else if (entry.getKey() == GameRules.RULE_RANDOMTICKING) {
            Type<IntegerValue> type = (Type<IntegerValue>)entry.getValue();
            BiConsumer<MinecraftServer, IntegerValue> callback = type.callback;
            type.callback = (server, integerValue) -> {
               callback.accept(server, integerValue);
               AxiomClientboundSetWorldProperty packet = new AxiomClientboundSetWorldProperty(
                  DefaultServerWorldProperties.RANDOM_TICK_SPEED,
                  WorldPropertyDataType.INTEGER.getTypeId(),
                  WorldPropertyDataType.INTEGER.serialize(integerValue.get())
               );

               for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                  if (AxiomServer.canUseAxiom(player)) {
                     packet.send(player);
                  }
               }
            };
         } else if (entry.getKey() == GameRules.RULE_WEATHER_CYCLE) {
            Type<BooleanValue> type = (Type<BooleanValue>)entry.getValue();
            BiConsumer<MinecraftServer, BooleanValue> callback = type.callback;
            type.callback = (server, booleanValue) -> {
               callback.accept(server, booleanValue);
               AxiomClientboundSetWorldProperty packet = new AxiomClientboundSetWorldProperty(
                  DefaultServerWorldProperties.PAUSE_WEATHER,
                  WorldPropertyDataType.BOOLEAN.getTypeId(),
                  WorldPropertyDataType.BOOLEAN.serialize(!booleanValue.get())
               );

               for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                  if (AxiomServer.canUseAxiom(player)) {
                     packet.send(player);
                  }
               }
            };
         } else if (entry.getKey() == GameRules.RULE_SPECTATORSGENERATECHUNKS) {
            Type<BooleanValue> type = (Type<BooleanValue>)entry.getValue();
            BiConsumer<MinecraftServer, BooleanValue> callback = type.callback;
            type.callback = (server, booleanValue) -> {
               callback.accept(server, booleanValue);
               AxiomClientboundEditorWarning packet = new AxiomClientboundEditorWarning(EditorWarningType.SPECTATORS_GENERATE_CHUNKS, !booleanValue.get());

               for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                  if (AxiomServer.canUseAxiom(player)) {
                     packet.send(player);
                  }
               }
            };
         }
      }
   }
}
