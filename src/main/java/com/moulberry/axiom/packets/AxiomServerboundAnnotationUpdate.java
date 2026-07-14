package com.moulberry.axiom.packets;

import com.moulberry.axiom.annotations.AnnotationUpdateAction;
import com.moulberry.axiom.annotations.ServerAnnotations;
import com.moulberry.axiom.restrictions.AxiomPermission;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AxiomServerboundAnnotationUpdate implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:annotation_update");
   private final List<AnnotationUpdateAction> actions;

   public AxiomServerboundAnnotationUpdate(List<AnnotationUpdateAction> actions) {
      this.actions = List.copyOf(actions);
   }

   public AxiomServerboundAnnotationUpdate(FriendlyByteBuf friendlyByteBuf) {
      int length = friendlyByteBuf.readVarInt();
      this.actions = new ArrayList<>();

      for (int i = 0; i < length; i++) {
         AnnotationUpdateAction action = AnnotationUpdateAction.read(friendlyByteBuf);
         if (action != null) {
            this.actions.add(action);
         }
      }
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeCollection(this.actions, (buffer, action) -> action.write(buffer));
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.ANNOTATION_CREATE)) {
         ServerAnnotations.handleUpdates(player.serverLevel(), this.actions);
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundAnnotationUpdate::new);
   }
}
