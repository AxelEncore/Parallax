package com.moulberry.axiom.packets.blueprint;

import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.blueprint.RawBlueprint;
import com.moulberry.axiom.blueprint.ServerBlueprintManager;
import com.moulberry.axiom.blueprint.ServerBlueprintRegistry;
import com.moulberry.axiom.packets.AxiomServerboundPacket;
import com.moulberry.axiom.restrictions.AxiomPermission;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AxiomServerboundRequestBlueprint implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:request_blueprint");
   private final String path;

   public AxiomServerboundRequestBlueprint(String path) {
      this.path = path;
   }

   public AxiomServerboundRequestBlueprint(FriendlyByteBuf friendlyByteBuf) {
      this.path = friendlyByteBuf.readUtf();
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeUtf(this.path);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (!AxiomServer.supportsServerBlueprints()) {
         if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.BLUEPRINT_REQUEST)) {
            ServerBlueprintRegistry registry = ServerBlueprintManager.getRegistry();
            if (registry != null) {
               RawBlueprint rawBlueprint = registry.blueprints().get(this.path);
               if (rawBlueprint != null) {
                  new AxiomClientboundResponseBlueprint(this.path, rawBlueprint).send(player);
               }
            }
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundRequestBlueprint::new);
   }
}
