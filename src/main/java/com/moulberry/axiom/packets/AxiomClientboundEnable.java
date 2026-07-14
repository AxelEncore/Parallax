package com.moulberry.axiom.packets;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.HotbarManager;
import com.moulberry.axiom.ServerConfig;
import com.moulberry.axiom.editor.views.ViewManager;
import com.moulberry.axiom.utils.BlockHelper;
import java.util.Set;
import com.moulberry.axiom.platform.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundEnable implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:enable");
   private boolean enabled;
   private ServerConfig config;
   public static ServerConfig lastReadServerConfig = null;

   public AxiomClientboundEnable() {
      this(true, ServerConfig.createDefault());
   }

   public AxiomClientboundEnable(boolean enabled, ServerConfig config) {
      this.enabled = enabled;
      this.config = config;
   }

   public AxiomClientboundEnable(FriendlyByteBuf friendlyByteBuf) {
      try {
         this.enabled = friendlyByteBuf.readBoolean();
         if (this.enabled) {
            this.config = ServerConfig.read(friendlyByteBuf);
            if (this.config.setBufferMaxSize() < 30000) {
               throw new RuntimeException("Max Buffer Size is too low, got " + this.config.setBufferMaxSize() + ", minimum 30000");
            }

            lastReadServerConfig = this.config;
         } else {
            this.config = null;
         }
      } catch (Exception var3) {
         Axiom.LOGGER.error("Error while reading AxiomClientboundEnable", var3);
         this.enabled = false;
         this.config = null;
      }

      if (friendlyByteBuf.readerIndex() < friendlyByteBuf.writerIndex()) {
         friendlyByteBuf.readerIndex(friendlyByteBuf.writerIndex());
      }
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeBoolean(this.enabled);
      if (this.enabled) {
         this.config.write(friendlyByteBuf);
      }
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      boolean wasNotEnabled = Axiom.getInstance().serverConfig == null;
      Axiom.getInstance().serverConfig = this.config;
      if (wasNotEnabled && this.config != null) {
         AxiomClient.onAxiomEnabled(client);
      }

      if (this.config != null) {
         BlockHelper.ignoreRotation = this.config.ignoreRotationSet();
      } else {
         BlockHelper.ignoreRotation = Set.of();
      }

      ClientEvents.setNoPhysicalTrigger = TriState.DEFAULT;
      HotbarManager.updateLocation();
      ViewManager.updateLocation();
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundEnable::new);
   }
}
