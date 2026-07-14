package com.moulberry.axiom.utils;

import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

public class EntityDataUtils {
   public static CompoundTag saveWithoutId(Entity entity) {
      return entity.saveWithoutId(new CompoundTag());
   }

   @Nullable
   public static CompoundTag saveRoot(Entity entity) {
      CompoundTag tag = new CompoundTag();
      return entity.save(tag) ? tag : null;
   }

   @Nullable
   public static CompoundTag saveAsPassenger(Entity entity) {
      CompoundTag tag = new CompoundTag();
      return entity.saveAsPassenger(tag) ? tag : null;
   }

   public static void load(Entity entity, CompoundTag compoundTag) {
      entity.load(compoundTag);
   }

   @Nullable
   public static Vec3 getEntityPosition(CompoundTag entity) {
      if (entity.contains("TileX") && entity.contains("TileY") && entity.contains("TileZ")) {
         int tileX = VersionUtilsNbt.helperCompoundTagGetIntOr(entity, "TileX", 0);
         int tileY = VersionUtilsNbt.helperCompoundTagGetIntOr(entity, "TileY", 0);
         int tileZ = VersionUtilsNbt.helperCompoundTagGetIntOr(entity, "TileZ", 0);
         return new Vec3(tileX + 0.5, tileY + 0.5, tileZ + 0.5);
      } else {
         if (entity.contains("Pos")) {
            ListTag listTag = NbtHelper.getList(entity, "Pos", 6);
            if (listTag.size() == 3) {
               return new Vec3(
                  VersionUtilsNbt.helperListTagGetDoubleOr(listTag, 0, 0.0),
                  VersionUtilsNbt.helperListTagGetDoubleOr(listTag, 1, 0.0),
                  VersionUtilsNbt.helperListTagGetDoubleOr(listTag, 2, 0.0)
               );
            }
         }

         return null;
      }
   }

   public static void setEntityPosition(CompoundTag entity, Vec3 vec3) {
      if (entity.contains("TileX") && entity.contains("TileY") && entity.contains("TileZ")) {
         entity.putInt("TileX", (int)Math.floor(vec3.x));
         entity.putInt("TileY", (int)Math.floor(vec3.y));
         entity.putInt("TileZ", (int)Math.floor(vec3.z));
      } else {
         ListTag position = new ListTag();
         position.add(DoubleTag.valueOf(vec3.x));
         position.add(DoubleTag.valueOf(vec3.y));
         position.add(DoubleTag.valueOf(vec3.z));
         entity.put("Pos", position);
      }
   }

   public static boolean cloneEntity(
      UUID newUuid,
      CompoundTag entity,
      Vec3 translation,
      IntMatrix rotationMatrix,
      List<AxiomServerboundSpawnEntity.SpawnEntry> spawnEntries,
      List<AxiomServerboundManipulateEntity.ManipulateEntry> manipulateEntries,
      List<UUID> allUuids
   ) {
      Vec3 position = getEntityPosition(entity);
      if (position == null) {
         return false;
      } else {
         double x = position.x - 0.5;
         double y = position.y - 0.5;
         double z = position.z - 0.5;
         Vector2f rotation = getEntityRotation(entity);
         float yaw = rotation == null ? 0.0F : rotation.x;
         float pitch = rotation == null ? 0.0F : rotation.y;
         if (rotationMatrix != null && !rotationMatrix.isIdentity()) {
            double nx = rotationMatrix.transformDoubleX(x, y, z);
            double ny = rotationMatrix.transformDoubleY(x, y, z);
            double nz = rotationMatrix.transformDoubleZ(x, y, z);
            position = new Vec3(nx + translation.x + 0.5, ny + translation.y + 0.5, nz + translation.z + 0.5);
            double pitchRadians = Math.toRadians(pitch);
            double yawRadians = Math.toRadians(-yaw);
            double cosYaw = Math.cos(yawRadians);
            double sinYaw = Math.sin(yawRadians);
            double cosPitch = Math.cos(pitchRadians);
            double sinPitch = Math.sin(pitchRadians);
            Vec3 viewVector = new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
            double viewX = rotationMatrix.transformDoubleX(viewVector.x, viewVector.y, viewVector.z);
            double viewY = rotationMatrix.transformDoubleY(viewVector.x, viewVector.y, viewVector.z);
            double viewZ = rotationMatrix.transformDoubleZ(viewVector.x, viewVector.y, viewVector.z);
            double viewXZ = Math.sqrt(viewX * viewX + viewZ * viewZ);
            pitch = Mth.wrapDegrees((float)(-(Mth.atan2(viewY, viewXZ) * 180.0F / (float)Math.PI)));
            yaw = Mth.wrapDegrees((float)(Mth.atan2(viewZ, viewX) * 180.0F / (float)Math.PI) - 90.0F);
            pitch = Math.round(pitch * 256.0F) / 256.0F;
            yaw = Math.round(yaw * 256.0F) / 256.0F;
         } else {
            position = new Vec3(x + translation.x + 0.5, y + translation.y + 0.5, z + translation.z + 0.5);
         }

         List<UUID> childrenUuids = new ArrayList<>();

         for (CompoundTag child : splitChildren(entity)) {
            UUID childUuid = UUID.randomUUID();
            if (cloneEntity(childUuid, child, translation, rotationMatrix, spawnEntries, manipulateEntries, allUuids)) {
               childrenUuids.add(childUuid);
            }
         }

         spawnEntries.add(new AxiomServerboundSpawnEntity.SpawnEntry(newUuid, position, yaw, pitch, null, entity));
         if (!childrenUuids.isEmpty()) {
            manipulateEntries.add(
               new AxiomServerboundManipulateEntity.ManipulateEntry(newUuid, AxiomServerboundManipulateEntity.PassengerManipulation.ADD_LIST, childrenUuids)
            );
         }

         allUuids.add(newUuid);
         return true;
      }
   }

   public static void applyMatrixToEntity(CompoundTag entity, IntMatrix matrix) {
      if (matrix != null && !matrix.isIdentity()) {
         Vec3 position = getEntityPosition(entity);
         if (position != null) {
            double x = position.x - 0.5;
            double y = position.y - 0.5;
            double z = position.z - 0.5;
            Vector2f rotation = getEntityRotation(entity);
            float yaw = rotation == null ? 0.0F : rotation.x;
            float pitch = rotation == null ? 0.0F : rotation.y;
            double nx = matrix.transformDoubleX(x, y, z);
            double ny = matrix.transformDoubleY(x, y, z);
            double nz = matrix.transformDoubleZ(x, y, z);
            position = new Vec3(nx + 0.5, ny + 0.5, nz + 0.5);
            double pitchRadians = Math.toRadians(pitch);
            double yawRadians = Math.toRadians(-yaw);
            double cosYaw = Math.cos(yawRadians);
            double sinYaw = Math.sin(yawRadians);
            double cosPitch = Math.cos(pitchRadians);
            double sinPitch = Math.sin(pitchRadians);
            Vec3 viewVector = new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
            double viewX = matrix.transformDoubleX(viewVector.x, viewVector.y, viewVector.z);
            double viewY = matrix.transformDoubleY(viewVector.x, viewVector.y, viewVector.z);
            double viewZ = matrix.transformDoubleZ(viewVector.x, viewVector.y, viewVector.z);
            double viewXZ = Math.sqrt(viewX * viewX + viewZ * viewZ);
            pitch = Mth.wrapDegrees((float)(-(Mth.atan2(viewY, viewXZ) * 180.0F / (float)Math.PI)));
            yaw = Mth.wrapDegrees((float)(Mth.atan2(viewZ, viewX) * 180.0F / (float)Math.PI) - 90.0F);
            pitch = Math.round(pitch * 256.0F) / 256.0F;
            yaw = Math.round(yaw * 256.0F) / 256.0F;
            setEntityPosition(entity, position);
            setEntityRotation(entity, new Vector2f(yaw, pitch));

            for (CompoundTag child : getChildren(entity)) {
               applyMatrixToEntity(child, matrix);
            }
         }
      }
   }

   public static List<CompoundTag> splitChildren(CompoundTag entity) {
      if (entity.contains("Passengers")) {
         ListTag listTag = NbtHelper.getList(entity, "Passengers", 10);
         if (!listTag.isEmpty()) {
            List<CompoundTag> children = new ArrayList<>();

            for (Tag tag : listTag) {
               children.add((CompoundTag)tag);
            }

            entity.remove("Passengers");
            return children;
         }
      }

      return List.of();
   }

   public static List<CompoundTag> getChildren(CompoundTag entity) {
      if (entity.contains("Passengers")) {
         ListTag listTag = NbtHelper.getList(entity, "Passengers", 10);
         if (!listTag.isEmpty()) {
            List<CompoundTag> children = new ArrayList<>();

            for (Tag tag : listTag) {
               children.add((CompoundTag)tag);
            }

            return children;
         }
      }

      return List.of();
   }

   @Nullable
   public static Vector2f getEntityRotation(CompoundTag entity) {
      if (entity.contains("Rotation")) {
         ListTag listTag = NbtHelper.getList(entity, "Rotation", 5);
         if (listTag.size() == 2) {
            return new Vector2f(VersionUtilsNbt.helperListTagGetFloatOr(listTag, 0, 0.0F), VersionUtilsNbt.helperListTagGetFloatOr(listTag, 1, 0.0F));
         }
      }

      return null;
   }

   public static void setEntityRotation(CompoundTag entity, Vector2f vector2f) {
      ListTag rotation = new ListTag();
      rotation.add(FloatTag.valueOf(vector2f.x));
      rotation.add(FloatTag.valueOf(vector2f.y));
      entity.put("Rotation", rotation);
   }

   public static void offsetEntityRecursive(CompoundTag entity, Vec3 offset) {
      if (entity.contains("Pos")) {
         ListTag listTag = NbtHelper.getList(entity, "Pos", 6);
         if (listTag.size() == 3) {
            listTag.setTag(0, DoubleTag.valueOf(VersionUtilsNbt.helperListTagGetDoubleOr(listTag, 0, 0.0) + offset.x));
            listTag.setTag(1, DoubleTag.valueOf(VersionUtilsNbt.helperListTagGetDoubleOr(listTag, 1, 0.0) + offset.y));
            listTag.setTag(2, DoubleTag.valueOf(VersionUtilsNbt.helperListTagGetDoubleOr(listTag, 2, 0.0) + offset.z));
         }
      }

      if (entity.contains("TileX")) {
         entity.putInt("TileX", VersionUtilsNbt.helperCompoundTagGetIntOr(entity, "TileX", 0) + (int)Math.round(offset.x));
      }

      if (entity.contains("TileY")) {
         entity.putInt("TileY", VersionUtilsNbt.helperCompoundTagGetIntOr(entity, "TileY", 0) + (int)Math.round(offset.y));
      }

      if (entity.contains("TileZ")) {
         entity.putInt("TileZ", VersionUtilsNbt.helperCompoundTagGetIntOr(entity, "TileZ", 0) + (int)Math.round(offset.z));
      }

      if (entity.contains("Passengers")) {
         for (Tag tag : NbtHelper.getList(entity, "Passengers", 10)) {
            offsetEntityRecursive((CompoundTag)tag, offset);
         }
      }
   }
}
