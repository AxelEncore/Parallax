package com.moulberry.axiom.packets;

import com.moulberry.axiom.AxiomClient;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundIgnoreDisplayEntities implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:ignore_display_entities");
   private final Set<UUID> ignoredEntities;

   public AxiomClientboundIgnoreDisplayEntities(Set<UUID> ignoredEntities) {
      this.ignoredEntities = Set.copyOf(ignoredEntities);
   }

   public AxiomClientboundIgnoreDisplayEntities(FriendlyByteBuf friendlyByteBuf) {
      this.ignoredEntities = (Set<UUID>)friendlyByteBuf.readCollection(count -> new HashSet(Math.min(count, 65536)), buf -> buf.readUUID());
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeCollection(this.ignoredEntities, (buf, uuid) -> buf.writeUUID(uuid));
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      AxiomClient.ignoredDisplayEntities = this.ignoredEntities;
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundIgnoreDisplayEntities::new);
   }
}
