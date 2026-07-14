package com.moulberry.axiom.packets;

import com.mojang.math.Transformation;
import com.moulberry.axiom.displayentity.ItemList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Brightness;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class AxiomClientboundRegisterCustomItems implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:register_custom_items");
   private final List<ItemList.Entry> entries;

   public AxiomClientboundRegisterCustomItems(FriendlyByteBuf friendlyByteBuf) {
      List<ItemList.Entry> entries = new ArrayList<>();
      int count = friendlyByteBuf.readVarInt();

      for (int i = 0; i < count; i++) {
         ItemStack itemStack = (ItemStack)ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf)friendlyByteBuf);
         ResourceLocation location = friendlyByteBuf.readResourceLocation();
         String searchKey = friendlyByteBuf.readUtf();
         Transformation defaultTransformation = null;
         if (friendlyByteBuf.readBoolean()) {
            Vector3f translation = friendlyByteBuf.readVector3f();
            Quaternionf leftRotation = friendlyByteBuf.readQuaternion();
            Vector3f scale = friendlyByteBuf.readVector3f();
            Quaternionf rightRotation = friendlyByteBuf.readQuaternion();
            defaultTransformation = new Transformation(
               new Vector3f(translation), new Quaternionf(leftRotation), new Vector3f(scale), new Quaternionf(rightRotation)
            );
         }

         Brightness defaultBrightness = null;
         if (friendlyByteBuf.readBoolean()) {
            defaultBrightness = new Brightness(friendlyByteBuf.readByte(), friendlyByteBuf.readByte());
         }

         entries.add(new ItemList.Entry(itemStack, location, searchKey, defaultTransformation, defaultBrightness));
      }

      this.entries = entries;
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeVarInt(this.entries.size());

      for (ItemList.Entry entry : this.entries) {
         ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf)friendlyByteBuf, entry.itemStack());
         friendlyByteBuf.writeResourceLocation(entry.location());
         friendlyByteBuf.writeUtf(entry.searchKey());
         if (entry.defaultItemDisplayTransformation() != null) {
            friendlyByteBuf.writeBoolean(true);
            Transformation transformation = entry.defaultItemDisplayTransformation();
            friendlyByteBuf.writeVector3f(new Vector3f(transformation.getTranslation()));
            friendlyByteBuf.writeQuaternion(new Quaternionf(transformation.getLeftRotation()));
            friendlyByteBuf.writeVector3f(new Vector3f(transformation.getScale()));
            friendlyByteBuf.writeQuaternion(new Quaternionf(transformation.getRightRotation()));
         } else {
            friendlyByteBuf.writeBoolean(false);
         }

         if (entry.defaultItemDisplayBrightness() != null) {
            friendlyByteBuf.writeBoolean(true);
            friendlyByteBuf.writeByte(entry.defaultItemDisplayBrightness().block());
            friendlyByteBuf.writeByte(entry.defaultItemDisplayBrightness().sky());
         } else {
            friendlyByteBuf.writeBoolean(false);
         }
      }
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      ItemList.INSTANCE.customEntriesDefinedByPacket = new ArrayList<>(this.entries);
      ItemList.INSTANCE.markDirty();
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundRegisterCustomItems::new);
   }
}
