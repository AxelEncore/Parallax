package com.moulberry.axiom.packets;

import com.moulberry.axiom.restrictions.ClientRestrictions;
import com.moulberry.axiom.restrictions.Restrictions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundRestrictions implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:restrictions");
   public final Restrictions restrictions;

   public AxiomClientboundRestrictions(Restrictions restrictions) {
      this.restrictions = restrictions;
   }

   public AxiomClientboundRestrictions(FriendlyByteBuf friendlyByteBuf) {
      this.restrictions = new Restrictions(friendlyByteBuf);
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      this.restrictions.write(friendlyByteBuf);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      ClientRestrictions.update(this.restrictions);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundRestrictions::new);
   }
}
