package com.moulberry.axiom.packets;

import com.moulberry.axiom.world_properties.WorldPropertyCategory;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertiesRegistry;
import com.moulberry.axiom.world_properties.client.ClientWorldProperty;
import com.moulberry.axiom.world_properties.server.ServerWorldProperty;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundRegisterWorldProperties implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:register_world_properties");
   private final LinkedHashMap<WorldPropertyCategory, List<ClientWorldProperty<?>>> clientProperties;
   private final LinkedHashMap<WorldPropertyCategory, List<ServerWorldProperty<?>>> serverProperties;

   public AxiomClientboundRegisterWorldProperties(LinkedHashMap<WorldPropertyCategory, List<ServerWorldProperty<?>>> properties) {
      this.serverProperties = new LinkedHashMap<>(properties);
      this.clientProperties = null;
   }

   public AxiomClientboundRegisterWorldProperties(FriendlyByteBuf friendlyByteBuf) {
      int count = friendlyByteBuf.readVarInt();
      this.clientProperties = new LinkedHashMap<>();

      for (int i = 0; i < count; i++) {
         WorldPropertyCategory category = WorldPropertyCategory.read(friendlyByteBuf);
         List<ClientWorldProperty<?>> properties = friendlyByteBuf.readList(ClientWorldProperty::read);
         this.clientProperties.put(category, properties);
      }

      this.serverProperties = null;
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeVarInt(this.serverProperties.size());

      for (Entry<WorldPropertyCategory, List<ServerWorldProperty<?>>> entry : this.serverProperties.entrySet()) {
         entry.getKey().write(friendlyByteBuf);
         friendlyByteBuf.writeCollection(entry.getValue(), (buf, p) -> p.write(buf));
      }
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      ClientWorldPropertiesRegistry.loadAll(this.clientProperties);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundRegisterWorldProperties::new);
   }
}
