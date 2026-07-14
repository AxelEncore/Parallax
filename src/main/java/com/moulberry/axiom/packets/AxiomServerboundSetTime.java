package com.moulberry.axiom.packets;

import com.moulberry.axiom.restrictions.AxiomPermission;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules.BooleanValue;

public class AxiomServerboundSetTime implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:set_world_time");
   private final ResourceKey<Level> world;
   private final Integer time;
   private final Boolean freezeTime;

   public AxiomServerboundSetTime(ResourceKey<Level> world, Integer time, Boolean freezeTime) {
      this.world = world;
      this.time = time;
      this.freezeTime = freezeTime;
   }

   public AxiomServerboundSetTime(FriendlyByteBuf friendlyByteBuf) {
      this.world = friendlyByteBuf.readResourceKey(Registries.DIMENSION);
      this.time = (Integer)friendlyByteBuf.readNullable(FriendlyByteBuf::readInt);
      this.freezeTime = (Boolean)friendlyByteBuf.readNullable(FriendlyByteBuf::readBoolean);
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeResourceKey(this.world);
      friendlyByteBuf.writeNullable(this.time, FriendlyByteBuf::writeInt);
      friendlyByteBuf.writeNullable(this.freezeTime, FriendlyByteBuf::writeBoolean);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.WORLD_TIME)) {
         if (this.time != null || this.freezeTime != null) {
            try {
               ServerLevel level = server.getLevel(this.world);
               if (level != null) {
                  if (this.time != null) {
                     level.setDayTime(this.time.intValue());
                  }

                  if (this.freezeTime != null) {
                     ((BooleanValue)level.getGameRules().getRule(GameRules.RULE_DAYLIGHT)).set(!this.freezeTime, server);
                  }
               }
            } catch (Exception var4) {
               var4.printStackTrace();
            }
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundSetTime::new);
   }
}
