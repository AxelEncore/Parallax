package com.moulberry.axiom.packets;

import com.moulberry.axiom.annotations.AnnotationUpdateAction;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundAnnotationUpdate implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:annotation_update");
   private final List<AnnotationUpdateAction> actions;

   public AxiomClientboundAnnotationUpdate(List<AnnotationUpdateAction> actions) {
      this.actions = List.copyOf(actions);
   }

   public AxiomClientboundAnnotationUpdate(FriendlyByteBuf friendlyByteBuf) {
      int length = friendlyByteBuf.readVarInt();
      this.actions = new ArrayList<>(Math.min(256, length));

      for (int i = 0; i < length; i++) {
         AnnotationUpdateAction action = AnnotationUpdateAction.read(friendlyByteBuf);
         if (action != null) {
            this.actions.add(action);
         }
      }
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeCollection(this.actions, (buffer, action) -> action.write(buffer));
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      for (AnnotationUpdateAction action : this.actions) {
         action.apply();
      }
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundAnnotationUpdate::new);
   }
}
