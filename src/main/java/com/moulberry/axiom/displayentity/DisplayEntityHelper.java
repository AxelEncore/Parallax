package com.moulberry.axiom.displayentity;

import com.mojang.math.Transformation;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mixin.DisplayAccessor;
import com.moulberry.axiom.packets.AxiomServerboundDeleteEntity;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.utils.EntityDataUtils;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.utils.SerializationUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Display.TextDisplay.Align;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayEntityHelper {
   private static final Tag DEFAULT_TRANSFORMATION = (Tag)Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, Transformation.identity()).result().get();
   private static final Tag DEFAULT_BILLBOARD = (Tag)BillboardConstraints.CODEC.encodeStart(NbtOps.INSTANCE, BillboardConstraints.FIXED).result().get();
   private static final Tag DEFAULT_ITEM_DISPLAY = (Tag)ItemDisplayContext.CODEC.encodeStart(NbtOps.INSTANCE, ItemDisplayContext.NONE).result().get();
   private static final List<String> VALID_PASSENGER_IDS = List.of(
      EntityType.getKey(EntityType.ITEM_DISPLAY).toString(),
      EntityType.getKey(EntityType.BLOCK_DISPLAY).toString(),
      EntityType.getKey(EntityType.TEXT_DISPLAY).toString()
   );

   public static void spawnAtPlayer(DisplayEntityObject object, LocalPlayer player) {
      spawnAt(object, player.position().add(0.0, 0.5, 0.0));
   }

   public static void spawnAt(DisplayEntityObject object, Vec3 position) {
      CompoundTag tag = object.getNbt(false);
      tag.putString("id", object.entityType());
      if (object instanceof DisplayEntityObject.DisplayEntityBlockObject) {
         Transformation transformation = new Transformation(
            new Vector3f(new Vector3f(-0.5F, -0.5F, -0.5F)),
            new Quaternionf(new Quaternionf()),
            new Vector3f(new Vector3f(1.0F, 1.0F, 1.0F)),
            new Quaternionf(new Quaternionf())
         );
         Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().ifPresent(t -> tag.put("transformation", t));
      }

      new AxiomServerboundSpawnEntity(List.of(new AxiomServerboundSpawnEntity.SpawnEntry(UUID.randomUUID(), position, 0.0F, 0.0F, null, tag))).send();
   }

   public static void applyTransformation(Display display, Matrix4f matrix4f) {
      CompoundTag compoundTag = new CompoundTag();
      Transformation transformation = VersionUtils.helperTransformationNew(matrix4f);
      transformation.getLeftRotation();
      Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().ifPresent(tag -> compoundTag.put("transformation", tag));
      new AxiomServerboundManipulateEntity(List.of(new AxiomServerboundManipulateEntity.ManipulateEntry(display.getUUID(), null, compoundTag))).send();
   }

   public static void applyBrightness(Display display, @NotNull Brightness brightness) {
      CompoundTag compoundTag = new CompoundTag();
      Brightness.CODEC.encodeStart(NbtOps.INSTANCE, brightness).result().ifPresent(tag -> compoundTag.put("brightness", tag));
      new AxiomServerboundManipulateEntity(List.of(new AxiomServerboundManipulateEntity.ManipulateEntry(display.getUUID(), null, compoundTag))).send();
   }

   public static void spawnAtWithTransformation(DisplayEntityObject object, Vec3 position, Matrix4f matrix4f) {
      CompoundTag compoundTag = object.getNbt(false);
      compoundTag.putString("id", object.entityType());
      Transformation transformation = VersionUtils.helperTransformationNew(matrix4f);
      transformation.getLeftRotation();
      Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().ifPresent(tag -> compoundTag.put("transformation", tag));
      new AxiomServerboundSpawnEntity(List.of(new AxiomServerboundSpawnEntity.SpawnEntry(UUID.randomUUID(), position, 0.0F, 0.0F, null, compoundTag))).send();
   }

   public static void killRecursive(Display display) {
      if (!display.isRemoved()) {
         List<UUID> toKill = new ArrayList<>();
         addUuidOfSelfAndChildren(display, toKill);
         new AxiomServerboundDeleteEntity(toKill).send();
      }
   }

   private static void addUuidOfSelfAndChildren(Display display, List<UUID> uuids) {
      if (!display.isRemoved()) {
         uuids.add(display.getUUID());

         for (Entity passenger : display.getPassengers()) {
            if (passenger instanceof Display displayPassenger) {
               addUuidOfSelfAndChildren(displayPassenger, uuids);
            }
         }
      }
   }

   public static void applyDataTo(Display display, DisplayEntityObject object) {
      if (!display.isRemoved()) {
         new AxiomServerboundManipulateEntity(
               List.of(
                  new AxiomServerboundManipulateEntity.ManipulateEntry(
                     display.getUUID(), null, object.getNbt((Integer)display.getEntityData().get(Display.DATA_BRIGHTNESS_OVERRIDE_ID) != -1)
                  )
               )
            )
            .send();
      }
   }

   @Nullable
   public static CompoundTag getDisplayEntityTagWithId(Display display) {
      CompoundTag tag = EntityDataUtils.saveAsPassenger(display);
      if (tag == null) {
         return null;
      } else {
         processDisplayEntityTag(tag);
         return tag;
      }
   }

   public static String getSummonCommandForEntity(Entity entity) {
      CompoundTag tag = EntityDataUtils.saveWithoutId(entity);
      processDisplayEntityTag(tag);
      String nbt = new SnbtPrinterTagVisitor("", 0, new ArrayList()).visit(tag);
      return "/summon " + EntityType.getKey(entity.getType()) + " ~ ~ ~ " + nbt;
   }

   public static String getInterpolateCommandForDisplay(Display display) {
      CompoundTag tag = EntityDataUtils.saveWithoutId(display);
      CompoundTag transformation = tag.getCompound("transformation");
      CompoundTag interpolateTag = new CompoundTag();
      interpolateTag.put("start_interpolation", IntTag.valueOf(-1));
      interpolateTag.put("transformation", transformation);
      String nbt = new SnbtPrinterTagVisitor("", 0, new ArrayList()).visit(interpolateTag);
      return "/data merge entity " + display.getStringUUID() + " " + nbt;
   }

   private static void processDisplayEntityTag(CompoundTag tag) {
      tag.remove("Pos");
      tag.remove("UUID");
      removeIfMatches(tag, "Motion", newDoubleList(0.0, 0.0, 0.0));
      removeIfMatches(tag, "Rotation", newFloatList(0.0F, 0.0F));
      removeIfMatches(tag, "Invulnerable", ByteTag.valueOf(false));
      tag.remove("fall_distance");
      tag.remove("Fire");
      tag.remove("Air");
      tag.remove("OnGround");
      tag.remove("PortalCooldown");
      removeIfMatches(tag, "transformation", DEFAULT_TRANSFORMATION);
      removeIfMatches(tag, "billboard", DEFAULT_BILLBOARD);
      removeIfMatches(tag, "interpolation_duration", IntTag.valueOf(0));
      removeIfMatches(tag, "teleport_duration", IntTag.valueOf(0));
      removeIfMatches(tag, "view_range", FloatTag.valueOf(1.0F));
      removeIfMatches(tag, "shadow_radius", FloatTag.valueOf(0.0F));
      removeIfMatches(tag, "shadow_strength", FloatTag.valueOf(1.0F));
      removeIfMatches(tag, "width", FloatTag.valueOf(0.0F));
      removeIfMatches(tag, "height", FloatTag.valueOf(0.0F));
      removeIfMatches(tag, "glow_color_override", IntTag.valueOf(-1));
      removeIfMatches(tag, "item_display", DEFAULT_ITEM_DISPLAY);
      ListTag passengers = NbtHelper.getList(tag, "Passengers", 10);
      Iterator<Tag> iterator = passengers.iterator();

      while (iterator.hasNext()) {
         CompoundTag passenger = (CompoundTag)iterator.next();
         String id = VersionUtilsNbt.helperCompoundTagGetStringOr(passenger, "id", "");
         if (!id.isEmpty() && VALID_PASSENGER_IDS.contains(id)) {
            processDisplayEntityTag(passenger);
         } else {
            iterator.remove();
         }
      }
   }

   private static void removeIfMatches(CompoundTag tag, String key, Tag value) {
      Tag existing = tag.get(key);
      if (existing != null) {
         if (existing.equals(value)) {
            tag.remove(key);
         }
      }
   }

   private static ListTag newDoubleList(double... ds) {
      ListTag listTag = new ListTag();

      for (double d : ds) {
         listTag.add(DoubleTag.valueOf(d));
      }

      return listTag;
   }

   private static ListTag newFloatList(float... fs) {
      ListTag listTag = new ListTag();

      for (float f : fs) {
         listTag.add(FloatTag.valueOf(f));
      }

      return listTag;
   }

   public static DisplayEntityObject getObjectFromEntity(Display display) {
      SynchedEntityData data = display.getEntityData();
      int brightnessData = (Integer)data.get(Display.DATA_BRIGHTNESS_OVERRIDE_ID);
      Brightness brightness = brightnessData != -1 ? Brightness.unpack(brightnessData) : null;
      DisplayEntityObject.GenericDisplayEntityData genericDisplayEntityData = new DisplayEntityObject.GenericDisplayEntityData(
         (Float)data.get(Display.DATA_WIDTH_ID),
         (Float)data.get(Display.DATA_HEIGHT_ID),
         (Integer)data.get(DisplayAccessor.getDataTransformationInterpolationDurationId()),
         (Integer)data.get(DisplayAccessor.getDataPosRotInterpolationId()),
         (Integer)data.get(Display.DATA_GLOW_COLOR_OVERRIDE_ID),
         (BillboardConstraints)BillboardConstraints.BY_ID.apply((Byte)data.get(Display.DATA_BILLBOARD_RENDER_CONSTRAINTS_ID)),
         brightness != null,
         brightness == null ? 0 : brightness.block(),
         brightness == null ? 0 : brightness.sky(),
         (Float)data.get(Display.DATA_SHADOW_RADIUS_ID),
         (Float)data.get(Display.DATA_SHADOW_STRENGTH_ID),
         (Float)data.get(Display.DATA_VIEW_RANGE_ID)
      );
      if (display instanceof ItemDisplay) {
         return new DisplayEntityObject.DisplayEntityItemObject(
            (ItemStack)data.get(ItemDisplay.DATA_ITEM_STACK_ID),
            (ItemDisplayContext)ItemDisplayContext.BY_ID.apply((Byte)data.get(ItemDisplay.DATA_ITEM_DISPLAY_ID)),
            genericDisplayEntityData,
            null,
            null
         );
      } else if (display instanceof BlockDisplay) {
         return new DisplayEntityObject.DisplayEntityBlockObject((BlockState)data.get(BlockDisplay.DATA_BLOCK_STATE_ID), genericDisplayEntityData);
      } else if (display instanceof TextDisplay) {
         byte flags = (Byte)data.get(TextDisplay.DATA_STYLE_FLAGS_ID);
         boolean shadow = (flags & 1) != 0;
         boolean seeThrough = (flags & 2) != 0;
         boolean useDefaultBackground = (flags & 4) != 0;
         Align align = TextDisplay.getAlign(flags);
         Component component = (Component)data.get(TextDisplay.DATA_TEXT_ID);
         String collapsed = component.tryCollapseToString();
         String json;
         if (collapsed != null) {
            json = collapsed;
         } else {
            json = SerializationUtils.componentToJson((Component)data.get(TextDisplay.DATA_TEXT_ID), display.registryAccess());
         }

         return new DisplayEntityObject.DisplayEntityTextObject(
            json,
            useDefaultBackground,
            (Integer)data.get(TextDisplay.DATA_BACKGROUND_COLOR_ID),
            (Integer)data.get(TextDisplay.DATA_LINE_WIDTH_ID),
            align,
            seeThrough,
            shadow,
            (Byte)data.get(TextDisplay.DATA_TEXT_OPACITY_ID),
            genericDisplayEntityData
         );
      } else {
         throw new FaultyImplementationError();
      }
   }
}
