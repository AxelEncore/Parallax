package com.moulberry.axiom.marker;

import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.hooks.MarkerEntityExt;
import com.moulberry.axiom.utils.NbtHelper;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record MarkerData(
   UUID uuid, Vec3 position, @Nullable String name, @Nullable Vec3 minRegion, @Nullable Vec3 maxRegion, int lineArgb, float lineThickness, int faceArgb
) {
   public static MarkerData read(FriendlyByteBuf friendlyByteBuf) {
      UUID uuid = friendlyByteBuf.readUUID();
      Vec3 position = new Vec3(friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble());
      String name = (String)friendlyByteBuf.readNullable(FriendlyByteBuf::readUtf);
      Vec3 minRegion = null;
      Vec3 maxRegion = null;
      int lineArgb = 0;
      float lineThickness = 0.0F;
      int faceArgb = 0;
      byte flags = friendlyByteBuf.readByte();
      if (flags != 0) {
         minRegion = new Vec3(friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble());
         maxRegion = new Vec3(friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble(), friendlyByteBuf.readDouble());
         if ((flags & 2) != 0) {
            lineArgb = friendlyByteBuf.readInt();
         }

         if ((flags & 4) != 0) {
            lineThickness = friendlyByteBuf.readFloat();
         }

         if ((flags & 8) != 0) {
            faceArgb = friendlyByteBuf.readInt();
         }
      }

      return new MarkerData(uuid, position, name, minRegion, maxRegion, lineArgb, lineThickness, faceArgb);
   }

   public static void write(FriendlyByteBuf friendlyByteBuf, MarkerData markerData) {
      friendlyByteBuf.writeUUID(markerData.uuid);
      friendlyByteBuf.writeDouble(markerData.position.x);
      friendlyByteBuf.writeDouble(markerData.position.y);
      friendlyByteBuf.writeDouble(markerData.position.z);
      friendlyByteBuf.writeNullable(markerData.name, FriendlyByteBuf::writeUtf);
      if (markerData.minRegion != null && markerData.maxRegion != null) {
         byte flags = 1;
         if (markerData.lineArgb != 0) {
            flags = (byte)(flags | 2);
         }

         if (markerData.lineThickness != 0.0F) {
            flags = (byte)(flags | 4);
         }

         if (markerData.faceArgb != 0) {
            flags = (byte)(flags | 8);
         }

         friendlyByteBuf.writeByte(flags);
         friendlyByteBuf.writeDouble(markerData.minRegion.x);
         friendlyByteBuf.writeDouble(markerData.minRegion.y);
         friendlyByteBuf.writeDouble(markerData.minRegion.z);
         friendlyByteBuf.writeDouble(markerData.maxRegion.x);
         friendlyByteBuf.writeDouble(markerData.maxRegion.y);
         friendlyByteBuf.writeDouble(markerData.maxRegion.z);
         if (markerData.lineArgb != 0) {
            friendlyByteBuf.writeInt(markerData.lineArgb);
         }

         if (markerData.lineThickness != 0.0F) {
            friendlyByteBuf.writeFloat(markerData.lineThickness);
         }

         if (markerData.faceArgb != 0) {
            friendlyByteBuf.writeInt(markerData.faceArgb);
         }
      } else {
         friendlyByteBuf.writeByte(0);
      }
   }

   public static MarkerData createFrom(Marker marker) {
      Vec3 position = marker.position();
      CompoundTag data = ((MarkerEntityExt)marker).axiom$getData();
      String name = VersionUtilsNbt.helperCompoundTagGetString(data, "name").orElse("").trim();
      if (name.isEmpty()) {
         name = null;
      }

      Vec3 minRegion = null;
      Vec3 maxRegion = null;
      int lineArgb = 0;
      float lineThickness = 0.0F;
      int faceArgb = 0;
      if (data.contains("min") && data.contains("max")) {
         ListTag min = NbtHelper.getList(data, "min", 6);
         if (min.size() == 3) {
            double minX = VersionUtilsNbt.helperListTagGetDoubleOr(min, 0, 0.0);
            double minY = VersionUtilsNbt.helperListTagGetDoubleOr(min, 1, 0.0);
            double minZ = VersionUtilsNbt.helperListTagGetDoubleOr(min, 2, 0.0);
            minRegion = new Vec3(minX, minY, minZ);
         }

         ListTag max = NbtHelper.getList(data, "max", 6);
         if (max.size() == 3) {
            double maxX = VersionUtilsNbt.helperListTagGetDoubleOr(max, 0, 0.0);
            double maxY = VersionUtilsNbt.helperListTagGetDoubleOr(max, 1, 0.0);
            double maxZ = VersionUtilsNbt.helperListTagGetDoubleOr(max, 2, 0.0);
            maxRegion = new Vec3(maxX, maxY, maxZ);
         }

         if (minRegion == null) {
            min = NbtHelper.getList(data, "min", 8);
            if (min.size() == 3) {
               double minX = calculateCoordinate(VersionUtilsNbt.helperListTagGetStringOr(min, 0, ""), position.x);
               double minY = calculateCoordinate(VersionUtilsNbt.helperListTagGetStringOr(min, 1, ""), position.y);
               double minZ = calculateCoordinate(VersionUtilsNbt.helperListTagGetStringOr(min, 2, ""), position.z);
               if (Double.isFinite(minX) && Double.isFinite(minY) && Double.isFinite(minZ)) {
                  minRegion = new Vec3(minX, minY, minZ);
               }
            }
         }

         if (maxRegion == null) {
            max = NbtHelper.getList(data, "max", 8);
            if (max.size() == 3) {
               double maxX = calculateCoordinate(VersionUtilsNbt.helperListTagGetStringOr(max, 0, ""), position.x);
               double maxY = calculateCoordinate(VersionUtilsNbt.helperListTagGetStringOr(max, 1, ""), position.y);
               double maxZ = calculateCoordinate(VersionUtilsNbt.helperListTagGetStringOr(max, 2, ""), position.z);
               if (Double.isFinite(maxX) && Double.isFinite(maxY) && Double.isFinite(maxZ)) {
                  maxRegion = new Vec3(maxX, maxY, maxZ);
               }
            }
         }

         if (minRegion != null && maxRegion != null) {
            lineArgb = VersionUtilsNbt.helperCompoundTagGetIntOr(data, "line_argb", 0);
            lineThickness = VersionUtilsNbt.helperCompoundTagGetIntOr(data, "line_thickness", 0);
            faceArgb = VersionUtilsNbt.helperCompoundTagGetIntOr(data, "face_argb", 0);
         }
      }

      return new MarkerData(marker.getUUID(), position, name, minRegion, maxRegion, lineArgb, lineThickness, faceArgb);
   }

   private static double calculateCoordinate(String coordinate, double position) {
      coordinate = coordinate.trim();
      boolean relative = coordinate.startsWith("~");
      if (relative) {
         coordinate = coordinate.substring(1).trim();
      }

      try {
         double value = Double.parseDouble(coordinate);
         return relative ? position + value : value;
      } catch (NumberFormatException var6) {
         return Double.NaN;
      }
   }
}
