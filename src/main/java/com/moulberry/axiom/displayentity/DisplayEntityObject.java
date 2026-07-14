package com.moulberry.axiom.displayentity;

import com.mojang.math.Transformation;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.TextDisplay.Align;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface DisplayEntityObject {
   String entityType();

   CompoundTag getNbt(boolean var1);

   DisplayEntityObject.GenericDisplayEntityData genericDisplayEntityData();

   public record DisplayEntityBlockObject(BlockState blockState, DisplayEntityObject.GenericDisplayEntityData genericDisplayEntityData)
      implements DisplayEntityObject {
      @Override
      public String entityType() {
         return "block_display";
      }

      @Override
      public CompoundTag getNbt(boolean entityHasBrightness) {
         CompoundTag compoundTag = new CompoundTag();
         compoundTag.put("block_state", NbtUtils.writeBlockState(this.blockState));
         this.genericDisplayEntityData.write(compoundTag, entityHasBrightness);
         return compoundTag;
      }
   }

   public record DisplayEntityItemObject(
      ItemStack itemStack,
      ItemDisplayContext itemDisplayContext,
      DisplayEntityObject.GenericDisplayEntityData genericDisplayEntityData,
      @Nullable Transformation transformation,
      @Nullable Brightness brightness
   ) implements DisplayEntityObject {
      @Override
      public String entityType() {
         return "item_display";
      }

      @Override
      public CompoundTag getNbt(boolean entityHasBrightness) {
         CompoundTag compoundTag = new CompoundTag();
         Tag item = ItemStackDataHelper.save(this.itemStack, Minecraft.getInstance().player.registryAccess());
         compoundTag.put("item", item);
         compoundTag.putString("item_display", this.itemDisplayContext.getSerializedName());
         this.genericDisplayEntityData.write(compoundTag, entityHasBrightness);
         if (this.transformation != null) {
            Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, this.transformation).result().ifPresent(tag -> compoundTag.put("transformation", tag));
         }

         if (this.brightness != null) {
            Brightness.CODEC
               .encodeStart(NbtOps.INSTANCE, new Brightness(this.brightness.block(), this.brightness.sky()))
               .result()
               .ifPresent(tag -> compoundTag.put("brightness", tag));
         }

         return compoundTag;
      }
   }

   public record DisplayEntityTextObject(
      String text,
      boolean defaultBackground,
      int background,
      int lineWidth,
      Align alignment,
      boolean seeThrough,
      boolean shadow,
      int textOpacity,
      DisplayEntityObject.GenericDisplayEntityData genericDisplayEntityData
   ) implements DisplayEntityObject {
      @Override
      public String entityType() {
         return "text_display";
      }

      @Override
      public CompoundTag getNbt(boolean entityHasBrightness) {
         CompoundTag compoundTag = new CompoundTag();

         try {
            Serializer.fromJson(this.text, Minecraft.getInstance().player.registryAccess());
            compoundTag.putString("text", this.text);
         } catch (Exception var4) {
            compoundTag.putString("text", "{\"text\": \"" + this.text.replace("\"", "\\\"") + "\"}");
         }

         compoundTag.putInt("line_width", this.lineWidth);
         compoundTag.putInt("background", this.background);
         compoundTag.putByte("text_opacity", (byte)this.textOpacity);
         compoundTag.putBoolean("shadow", this.shadow);
         compoundTag.putBoolean("see_through", this.seeThrough);
         compoundTag.putBoolean("default_background", this.defaultBackground);
         Align.CODEC.encodeStart(NbtOps.INSTANCE, this.alignment).result().ifPresent(tag -> compoundTag.put("alignment", tag));
         this.genericDisplayEntityData.write(compoundTag, entityHasBrightness);
         return compoundTag;
      }
   }

   public record GenericDisplayEntityData(
      float width,
      float height,
      int interpolationDuration,
      int teleportDuration,
      int glowColorOverride,
      BillboardConstraints billboardConstraints,
      boolean overrideBrightness,
      int overrideBrightnessBlock,
      int overrideBrightnessSky,
      float shadowRadius,
      float shadowStrength,
      float viewRange
   ) {
      public void write(CompoundTag compoundTag, boolean entityHasBrightness) {
         BillboardConstraints.CODEC.encodeStart(NbtOps.INSTANCE, this.billboardConstraints).result().ifPresent(tag -> compoundTag.put("billboard", tag));
         compoundTag.putInt("interpolation_duration", this.interpolationDuration);
         compoundTag.putInt("teleport_duration", this.teleportDuration);
         compoundTag.putFloat("view_range", this.viewRange);
         compoundTag.putFloat("shadow_radius", this.shadowRadius);
         compoundTag.putFloat("shadow_strength", this.shadowStrength);
         compoundTag.putFloat("width", this.width);
         compoundTag.putFloat("height", this.height);
         compoundTag.putInt("glow_color_override", this.glowColorOverride);
         if (this.overrideBrightness) {
            Brightness.CODEC
               .encodeStart(NbtOps.INSTANCE, new Brightness(this.overrideBrightnessBlock, this.overrideBrightnessSky))
               .result()
               .ifPresent(tag -> compoundTag.put("brightness", tag));
         } else if (entityHasBrightness) {
            compoundTag.put("brightness", new CompoundTag());
         }
      }
   }
}
