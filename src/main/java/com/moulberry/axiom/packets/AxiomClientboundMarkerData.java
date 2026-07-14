package com.moulberry.axiom.packets;

import com.moulberry.axiom.marker.MarkerData;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundMarkerData implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:marker_data");
   public final List<MarkerData> entries;
   public final Set<UUID> removedMarkers;

   public AxiomClientboundMarkerData(List<MarkerData> entries, Set<UUID> removedMarkers) {
      this.entries = List.copyOf(entries);
      this.removedMarkers = Set.copyOf(removedMarkers);
   }

   public AxiomClientboundMarkerData(FriendlyByteBuf friendlyByteBuf) {
      this.entries = friendlyByteBuf.readList(MarkerData::read);
      this.removedMarkers = (Set<UUID>)friendlyByteBuf.readCollection(HashSet::new, buf -> buf.readUUID());
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeCollection(this.entries, MarkerData::write);
      friendlyByteBuf.writeCollection(this.removedMarkers, (buf, uuid) -> buf.writeUUID(uuid));
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      MarkerEntityManipulator.update(this);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundMarkerData::new);
   }
}
