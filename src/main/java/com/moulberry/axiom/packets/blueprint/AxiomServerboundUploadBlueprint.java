package com.moulberry.axiom.packets.blueprint;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.blueprint.BlueprintIo;
import com.moulberry.axiom.blueprint.RawBlueprint;
import com.moulberry.axiom.blueprint.ServerBlueprintManager;
import com.moulberry.axiom.blueprint.ServerBlueprintRegistry;
import com.moulberry.axiom.packets.AxiomServerboundPacket;
import com.moulberry.axiom.restrictions.AxiomPermission;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AxiomServerboundUploadBlueprint implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:upload_blueprint");
   private final String path;
   private final RawBlueprint rawBlueprint;

   public AxiomServerboundUploadBlueprint(String path, RawBlueprint rawBlueprint) {
      this.path = path;
      this.rawBlueprint = rawBlueprint;
   }

   public AxiomServerboundUploadBlueprint(FriendlyByteBuf friendlyByteBuf) {
      this.path = friendlyByteBuf.readUtf();
      this.rawBlueprint = RawBlueprint.read(friendlyByteBuf);
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeUtf(this.path);
      RawBlueprint.write(friendlyByteBuf, this.rawBlueprint);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServer.supportsServerBlueprints()) {
         if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.BLUEPRINT_UPLOAD)) {
            ServerBlueprintRegistry registry = ServerBlueprintManager.getRegistry();
            if (registry != null) {
               String pathStr = this.path;
               pathStr = pathStr.replace("\\", "/");
               if (pathStr.endsWith(".bp") && !pathStr.contains("..") && pathStr.startsWith("/")) {
                  pathStr = pathStr.substring(1);
                  Path relative = Path.of(pathStr).normalize();
                  if (!relative.isAbsolute()) {
                     Path path = Axiom.getInstance().getBlueprintDirectory().resolve(relative);

                     try {
                        Files.createDirectories(path.getParent());
                     } catch (IOException var13) {
                        return;
                     }

                     try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
                        BlueprintIo.writeRaw(outputStream, this.rawBlueprint);
                     } catch (IOException var12) {
                        return;
                     }

                     registry.blueprints().put("/" + pathStr.substring(0, pathStr.length() - 3), this.rawBlueprint);
                     ServerBlueprintManager.sendManifest(server.getPlayerList().getPlayers());
                  }
               }
            }
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundUploadBlueprint::new);
   }
}
