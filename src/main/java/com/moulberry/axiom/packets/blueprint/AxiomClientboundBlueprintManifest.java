package com.moulberry.axiom.packets.blueprint;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ServerConfig;
import com.moulberry.axiom.blueprint.RawBlueprint;
import com.moulberry.axiom.blueprint.ServerBlueprintRegistry;
import com.moulberry.axiom.editor.windows.clipboard.BlueprintBrowserWindow;
import com.moulberry.axiom.packets.AxiomClientboundEnable;
import com.moulberry.axiom.packets.AxiomClientboundPacket;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class AxiomClientboundBlueprintManifest implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:blueprint_manifest");
   private final boolean replace;
   private final ServerBlueprintRegistry registry;
   private final FriendlyByteBuf rawByteBuf;
   private static final int MAX_SIZE = 1000000;

   public AxiomClientboundBlueprintManifest(boolean replace, ServerBlueprintRegistry registry) {
      this.replace = replace;
      this.registry = registry;
      this.rawByteBuf = null;
   }

   public AxiomClientboundBlueprintManifest(FriendlyByteBuf rawByteBuf) {
      this.replace = false;
      this.registry = null;
      this.rawByteBuf = rawByteBuf;
   }

   public static AxiomClientboundBlueprintManifest read(FriendlyByteBuf friendlyByteBuf) {
      ServerConfig serverConfig = Axiom.getInstance().serverConfig;
      if (serverConfig == null) {
         serverConfig = AxiomClientboundEnable.lastReadServerConfig;
      }

      if (serverConfig != null && serverConfig.blueprintVersion() == 2) {
         boolean replace = friendlyByteBuf.readBoolean();
         ServerBlueprintRegistry registry = ServerBlueprintRegistry.readManifest(friendlyByteBuf);
         return new AxiomClientboundBlueprintManifest(replace, registry);
      } else {
         friendlyByteBuf.readerIndex(friendlyByteBuf.writerIndex());
         return new AxiomClientboundBlueprintManifest(true, new ServerBlueprintRegistry(new HashMap<>()));
      }
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      if (this.rawByteBuf != null) {
         friendlyByteBuf.writeBytes(this.rawByteBuf);
      } else {
         friendlyByteBuf.writeBoolean(this.replace);
         this.registry.writeManifest(friendlyByteBuf);
      }
   }

   public static void sendMulti(List<ServerPlayer> serverPlayers, ServerBlueprintRegistry registry) {
      FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
      buf.writeBoolean(true);

      for (Entry<String, RawBlueprint> entry : registry.blueprints().entrySet()) {
         buf.writeUtf(entry.getKey());
         RawBlueprint.writeHeader(buf, entry.getValue());
         if (buf.writerIndex() > 1000000) {
            buf.writeUtf("");
            AxiomClientboundBlueprintManifest packet = new AxiomClientboundBlueprintManifest(buf);

            for (ServerPlayer player : serverPlayers) {
               packet.send(player);
            }

            buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(false);
         }
      }

      buf.writeUtf("");
      AxiomClientboundBlueprintManifest packet = new AxiomClientboundBlueprintManifest(buf);

      for (ServerPlayer player : serverPlayers) {
         packet.send(player);
      }
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      BlueprintBrowserWindow.setServerBlueprintRegistry(this.replace, this.registry);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundBlueprintManifest::read);
   }
}
