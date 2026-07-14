package com.moulberry.axiom.world_properties;

import com.moulberry.axiom.world_properties.client.ClientWorldProperty;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertyButton;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertyButtonArray;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertyCheckbox;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertySlider;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertyTextbox;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertyTime;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

public interface WorldPropertyWidgetType<T> {
   WorldPropertyWidgetType<Boolean> CHECKBOX = new WorldPropertyWidgetType<Boolean>() {
      @Override
      public WorldPropertyDataType<Boolean> dataType() {
         return WorldPropertyDataType.BOOLEAN;
      }

      @Override
      public ClientWorldProperty<Boolean> create(ResourceLocation id, String name, boolean localizeName, byte[] data) {
         return new ClientWorldPropertyCheckbox(id, name, localizeName, this.dataType().deserialize(data));
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeVarInt(0);
      }
   };
   WorldPropertyWidgetType<String> TEXTBOX = new WorldPropertyWidgetType<String>() {
      @Override
      public WorldPropertyDataType<String> dataType() {
         return WorldPropertyDataType.STRING;
      }

      @Override
      public ClientWorldProperty<String> create(ResourceLocation id, String name, boolean localizeName, byte[] data) {
         return new ClientWorldPropertyTextbox(id, name, localizeName, this.dataType().deserialize(data));
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeVarInt(2);
      }
   };
   WorldPropertyWidgetType<Unit> TIME = new WorldPropertyWidgetType<Unit>() {
      @Override
      public WorldPropertyDataType<Unit> dataType() {
         return WorldPropertyDataType.EMPTY;
      }

      @Override
      public ClientWorldProperty<Unit> create(ResourceLocation id, String name, boolean localizeName, byte[] data) {
         return new ClientWorldPropertyTime(id, name, localizeName);
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeVarInt(3);
      }
   };
   WorldPropertyWidgetType<Unit> BUTTON = new WorldPropertyWidgetType<Unit>() {
      @Override
      public WorldPropertyDataType<Unit> dataType() {
         return WorldPropertyDataType.EMPTY;
      }

      @Override
      public ClientWorldProperty<Unit> create(ResourceLocation id, String name, boolean localizeName, byte[] data) {
         return new ClientWorldPropertyButton(id, name, localizeName);
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeVarInt(4);
      }
   };

   WorldPropertyDataType<T> dataType();

   ClientWorldProperty<T> create(ResourceLocation var1, String var2, boolean var3, byte[] var4);

   void write(FriendlyByteBuf var1);

   static WorldPropertyWidgetType<?> read(FriendlyByteBuf friendlyByteBuf) {
      int type = friendlyByteBuf.readVarInt();

      return (WorldPropertyWidgetType<?>)(switch (type) {
         case 0 -> CHECKBOX;
         case 1 -> new WorldPropertyWidgetType.Slider(friendlyByteBuf.readInt(), friendlyByteBuf.readInt());
         case 2 -> TEXTBOX;
         case 3 -> TIME;
         case 4 -> BUTTON;
         case 5 -> new WorldPropertyWidgetType.ButtonArray(friendlyByteBuf.readList(FriendlyByteBuf::readUtf));
         default -> throw new RuntimeException("Unknown widget type: " + type);
      });
   }

   public record ButtonArray(List<String> otherButtons) implements WorldPropertyWidgetType<Integer> {
      @Override
      public WorldPropertyDataType<Integer> dataType() {
         return WorldPropertyDataType.INTEGER;
      }

      @Override
      public ClientWorldProperty<Integer> create(ResourceLocation id, String name, boolean localizeName, byte[] data) {
         return new ClientWorldPropertyButtonArray(id, name, localizeName, this.otherButtons);
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeVarInt(5);
         friendlyByteBuf.writeCollection(this.otherButtons, FriendlyByteBuf::writeUtf);
      }
   }

   public record Slider(int min, int max) implements WorldPropertyWidgetType<Integer> {
      @Override
      public WorldPropertyDataType<Integer> dataType() {
         return WorldPropertyDataType.INTEGER;
      }

      @Override
      public ClientWorldProperty<Integer> create(ResourceLocation id, String name, boolean localizeName, byte[] data) {
         return new ClientWorldPropertySlider(id, name, localizeName, this.dataType().deserialize(data), this.min, this.max);
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeVarInt(1);
         friendlyByteBuf.writeInt(this.min);
         friendlyByteBuf.writeInt(this.max);
      }
   }
}
