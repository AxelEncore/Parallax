package com.moulberry.axiom.world_properties;

import com.moulberry.axiom.i18n.AxiomI18n;
import net.minecraft.network.FriendlyByteBuf;

public record WorldPropertyCategory(String name, boolean localizeName) {
   public String getLocalizedName() {
      return this.localizeName ? AxiomI18n.get(this.name) : this.name;
   }

   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeUtf(this.name);
      friendlyByteBuf.writeBoolean(this.localizeName);
   }

   public static WorldPropertyCategory read(FriendlyByteBuf friendlyByteBuf) {
      return new WorldPropertyCategory(friendlyByteBuf.readUtf(), friendlyByteBuf.readBoolean());
   }
}
