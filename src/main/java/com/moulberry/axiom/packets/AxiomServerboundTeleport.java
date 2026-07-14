package com.moulberry.axiom.packets;

import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.restrictions.AxiomPermission;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class AxiomServerboundTeleport implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:teleport");
   private final ResourceKey<Level> world;
   private final double x;
   private final double y;
   private final double z;
   private final float yRot;
   private final float xRot;

   public AxiomServerboundTeleport(ResourceKey<Level> world, double x, double y, double z, float yRot, float xRot) {
      this.world = world;
      this.x = x;
      this.y = y;
      this.z = z;
      this.yRot = yRot;
      this.xRot = xRot;
   }

   public AxiomServerboundTeleport(FriendlyByteBuf friendlyByteBuf) {
      this.world = friendlyByteBuf.readResourceKey(Registries.DIMENSION);
      this.x = friendlyByteBuf.readDouble();
      this.y = friendlyByteBuf.readDouble();
      this.z = friendlyByteBuf.readDouble();
      this.yRot = friendlyByteBuf.readFloat();
      this.xRot = friendlyByteBuf.readFloat();
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeResourceKey(this.world);
      friendlyByteBuf.writeDouble(this.x);
      friendlyByteBuf.writeDouble(this.y);
      friendlyByteBuf.writeDouble(this.z);
      friendlyByteBuf.writeFloat(this.yRot);
      friendlyByteBuf.writeFloat(this.xRot);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.PLAYER_TELEPORT)) {
         ServerLevel level = server.getLevel(this.world);
         if (level != null) {
            VersionUtils.genericTeleportTo(player, level, this.x, this.y, this.z, Set.of(), this.yRot, this.xRot, true);
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundTeleport::new);
   }
}
