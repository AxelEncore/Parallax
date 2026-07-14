package com.moulberry.axiom.editor.views;

import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.packets.AxiomServerboundTeleport;
import com.moulberry.axiom.utils.NbtHelper;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class View {
   public String name;
   public final UUID uuid;
   public boolean pinLevel = false;
   public boolean pinLocation = false;
   private ResourceKey<Level> level = null;
   private Vec3 position = null;
   private float yaw;
   private float pitch;
   private boolean active = false;

   public View(String name, UUID uuid, @Nullable Player player) {
      this.name = name;
      this.uuid = uuid;
      if (player != null) {
         this.position = player.position();
         this.yaw = player.getYRot();
         this.pitch = player.getXRot();
         this.level = player.level().dimension();
      }
   }

   public void teleportPinned(Player player) {
      if (player != null && this.level == null) {
         this.level = player.level().dimension();
      }

      if (this.position != null && this.level != null && this.pinLevel && this.pinLocation) {
         new AxiomServerboundTeleport(this.level, this.position.x, this.position.y, this.position.z, this.yaw, this.pitch).send();
      }
   }

   public void markInactive() {
      if (this.active) {
         this.active = false;
      }
   }

   public void markActive(Player player) {
      if (this.active) {
         if (!this.uuid.equals(ViewManager.activeView)) {
            ViewManager.activeView = this.uuid;
            ViewManager.activeFrames = 0;
            ViewManager.dirty();
         } else if (ViewManager.activeFrames < 5) {
            ViewManager.activeFrames++;
         } else {
            if (player == null) {
               return;
            }

            ResourceKey<Level> currentDimension = player.level().dimension();
            if (!this.pinLevel || this.level == null) {
               this.level = currentDimension;
            }

            if ((!this.pinLevel || !this.pinLocation || this.position == null) && (currentDimension.equals(this.level) || this.position == null)) {
               this.position = player.position();
               this.yaw = player.getYRot();
               this.pitch = player.getXRot();
            }
         }
      } else {
         this.active = true;
         if (!this.uuid.equals(ViewManager.activeView)) {
            ViewManager.activeView = this.uuid;
            ViewManager.activeFrames = 0;
            ViewManager.dirty();
            if (player != null && this.level == null) {
               this.level = player.level().dimension();
            }

            if (this.position != null && this.level != null) {
               new AxiomServerboundTeleport(this.level, this.position.x, this.position.y, this.position.z, this.yaw, this.pitch).send();
            }
         }
      }
   }

   public CompoundTag save() {
      CompoundTag tag = new CompoundTag();
      tag.putString("Name", this.name);
      NbtHelper.putUUID(tag, "UUID", this.uuid);
      tag.putBoolean("PinLevel", this.pinLevel);
      if (this.pinLevel && this.level != null) {
         tag.putString("Level", this.level.location().toString());
      }

      tag.putBoolean("PinLocation", this.pinLocation);
      if (this.position != null) {
         tag.putDouble("X", this.position.x);
         tag.putDouble("Y", this.position.y);
         tag.putDouble("Z", this.position.z);
         tag.putDouble("Yaw", this.yaw);
         tag.putDouble("Pitch", this.pitch);
      }

      return tag;
   }

   public static View load(CompoundTag tag) {
      String name = VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "Name", "");
      UUID uuid = NbtHelper.getUUID(tag, "UUID");
      View view = new View(name, uuid, null);
      view.pinLevel = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "PinLevel", false);
      String level = VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "Level", "");
      if (level.isEmpty()) {
         view.level = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(level));
      }

      view.pinLocation = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "PinLocation", false);
      if (tag.contains("X") && tag.contains("Y") && tag.contains("Z")) {
         double x = VersionUtilsNbt.helperCompoundTagGetDoubleOr(tag, "X", 0.0);
         double y = VersionUtilsNbt.helperCompoundTagGetDoubleOr(tag, "Y", 0.0);
         double z = VersionUtilsNbt.helperCompoundTagGetDoubleOr(tag, "Z", 0.0);
         view.position = new Vec3(x, y, z);
         view.yaw = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Yaw", 0.0F);
         view.pitch = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Pitch", 0.0F);
      }

      return view;
   }
}
