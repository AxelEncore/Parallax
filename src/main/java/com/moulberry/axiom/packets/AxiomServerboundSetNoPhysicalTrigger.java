package com.moulberry.axiom.packets;

import com.moulberry.axiom.hooks.ServerPlayerExt;
import com.moulberry.axiom.restrictions.AxiomPermission;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AxiomServerboundSetNoPhysicalTrigger implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:set_no_physical_trigger");
   private final boolean noPhysicalTrigger;

   public AxiomServerboundSetNoPhysicalTrigger(boolean noPhysicalTrigger) {
      this.noPhysicalTrigger = noPhysicalTrigger;
   }

   public AxiomServerboundSetNoPhysicalTrigger(FriendlyByteBuf friendlyByteBuf) {
      this.noPhysicalTrigger = friendlyByteBuf.readBoolean();
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeBoolean(this.noPhysicalTrigger);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.PLAYER_SETNOPHYSICALTRIGGER)) {
         ((ServerPlayerExt)player).axiom$setNoPhysicalTrigger(this.noPhysicalTrigger);
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundSetNoPhysicalTrigger::new);
   }
}
