package com.moulberry.axiom.packets;

import com.moulberry.axiom.restrictions.AxiomPermission;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AxiomServerboundSetFlySpeed implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:set_fly_speed");
   private final float flySpeed;

   public AxiomServerboundSetFlySpeed(float flySpeed) {
      this.flySpeed = flySpeed;
   }

   public AxiomServerboundSetFlySpeed(FriendlyByteBuf friendlyByteBuf) {
      this.flySpeed = friendlyByteBuf.readFloat();
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeFloat(this.flySpeed);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.PLAYER_SPEED)) {
         player.getAbilities().setFlyingSpeed(this.flySpeed);
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundSetFlySpeed::new);
   }
}
