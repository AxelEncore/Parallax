package com.moulberry.axiom.packets.blueprint;

import com.moulberry.axiom.blueprint.BlueprintIo;
import com.moulberry.axiom.blueprint.RawBlueprint;
import com.moulberry.axiom.editor.windows.clipboard.BlueprintBrowserWindow;
import com.moulberry.axiom.packets.AxiomClientboundPacket;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundResponseBlueprint implements AxiomClientboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:response_blueprint");
   private final String path;
   private final RawBlueprint blueprint;

   public AxiomClientboundResponseBlueprint(String path, RawBlueprint blueprint) {
      this.path = path;
      this.blueprint = blueprint;
   }

   public AxiomClientboundResponseBlueprint(FriendlyByteBuf friendlyByteBuf) {
      this.path = friendlyByteBuf.readUtf();
      this.blueprint = RawBlueprint.read(friendlyByteBuf);
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeUtf(this.path);
      RawBlueprint.write(friendlyByteBuf, this.blueprint);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      if (this.path.equals(BlueprintBrowserWindow.pendingServerBlueprintDownload)) {
         try {
            BlueprintBrowserWindow.pendingServerBlueprintResult = BlueprintIo.createFromRaw(this.blueprint);
         } catch (IOException var4) {
            BlueprintBrowserWindow.pendingServerBlueprintDownload = null;
         }
      }
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundResponseBlueprint::new);
   }
}
