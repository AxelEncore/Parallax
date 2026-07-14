package com.moulberry.axiom.world_properties.client;

import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.packets.AxiomServerboundSetWorldProperty;
import com.moulberry.axiom.world_properties.WorldPropertyDataType;
import com.moulberry.axiom.world_properties.WorldPropertyWidgetType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public abstract class ClientWorldProperty<T> {
   private static int GLOBAL_UPDATE_ID = 0;
   private final ResourceLocation id;
   private final String name;
   protected final boolean localizeName;
   protected int updateId = -1;
   protected T value;
   protected T serverValue;

   public ClientWorldProperty(ResourceLocation id, String name, boolean localizeName, T initialValue) {
      this.id = id;
      this.name = name;
      this.localizeName = localizeName;
      this.value = this.serverValue = initialValue;
   }

   public abstract void renderImgui();

   public abstract WorldPropertyDataType<T> getType();

   public ResourceLocation getId() {
      return this.id;
   }

   public String getLocalizedName() {
      return this.localizeName ? AxiomI18n.get(this.name) : this.name;
   }

   public T getLocalValue() {
      return this.value;
   }

   public void setRemoteValue(byte[] bytes) {
      this.serverValue = this.getType().deserialize(bytes);
      if (this.updateId < 0) {
         this.value = this.serverValue;
      }
   }

   public void changeLocalValue(T value) {
      GLOBAL_UPDATE_ID++;
      if (GLOBAL_UPDATE_ID < 0) {
         GLOBAL_UPDATE_ID = 0;
      }

      this.updateId = GLOBAL_UPDATE_ID;
      this.value = value;
      new AxiomServerboundSetWorldProperty(this.getId(), this.getType().getTypeId(), this.getType().serialize(this.value), this.updateId).send();
   }

   public void ackChangesUpTo(int updateId) {
      if (this.updateId >= 0) {
         if (this.updateId <= updateId) {
            this.updateId = -1;
            this.value = this.serverValue;
         }
      }
   }

   public static ClientWorldProperty<?> read(FriendlyByteBuf friendlyByteBuf) {
      ResourceLocation id = friendlyByteBuf.readResourceLocation();
      String name = friendlyByteBuf.readUtf();
      boolean localizeName = friendlyByteBuf.readBoolean();
      WorldPropertyWidgetType<?> widgetType = WorldPropertyWidgetType.read(friendlyByteBuf);
      byte[] data = friendlyByteBuf.readByteArray();
      return widgetType.create(id, name, localizeName, data);
   }
}
