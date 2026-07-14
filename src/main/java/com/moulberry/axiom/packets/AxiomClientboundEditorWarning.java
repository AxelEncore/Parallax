package com.moulberry.axiom.packets;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWarningType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundEditorWarning implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:editor_warning");
   private final EditorWarningType warningType;
   private final boolean enabled;

   public AxiomClientboundEditorWarning(EditorWarningType warningType, boolean enabled) {
      this.warningType = warningType;
      this.enabled = enabled;
   }

   public AxiomClientboundEditorWarning(FriendlyByteBuf friendlyByteBuf) {
      this.warningType = (EditorWarningType)friendlyByteBuf.readEnum(EditorWarningType.class);
      this.enabled = friendlyByteBuf.readBoolean();
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeEnum(this.warningType);
      friendlyByteBuf.writeBoolean(this.enabled);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      if (this.enabled) {
         EditorUI.warnings.add(this.warningType);
      } else {
         EditorUI.warnings.remove(this.warningType);
      }
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundEditorWarning::new);
   }
}
