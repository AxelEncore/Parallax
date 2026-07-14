package com.moulberry.axiom.restrictions;

import com.moulberry.axiom.utils.Box;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class Restrictions {
   private static final Map<String, AxiomPermission> PERMISSION_BY_NAME = new LinkedHashMap<>();
   public EnumSet<AxiomPermission> allowedPermissions = EnumSet.of(AxiomPermission.DEFAULT);
   public EnumSet<AxiomPermission> deniedPermissions = EnumSet.noneOf(AxiomPermission.class);
   public int infiniteReachLimit = -1;
   public List<Box> bounds = new ArrayList<>();
   public Box globalBounds = null;

   public Restrictions() {
   }

   public Restrictions(FriendlyByteBuf friendlyByteBuf) {
      this.allowedPermissions = EnumSet.noneOf(AxiomPermission.class);
      int allowedPermissionCount = friendlyByteBuf.readVarInt();

      for (int i = 0; i < allowedPermissionCount; i++) {
         String name = friendlyByteBuf.readUtf();
         AxiomPermission permission = PERMISSION_BY_NAME.get(name);
         if (permission != null) {
            this.allowedPermissions.add(permission);
         }
      }

      this.deniedPermissions = EnumSet.noneOf(AxiomPermission.class);
      int disallowedPermissionCount = friendlyByteBuf.readVarInt();

      for (int ix = 0; ix < disallowedPermissionCount; ix++) {
         String name = friendlyByteBuf.readUtf();
         AxiomPermission permission = PERMISSION_BY_NAME.get(name);
         if (permission != null) {
            this.deniedPermissions.add(permission);
         }
      }

      this.infiniteReachLimit = friendlyByteBuf.readInt();
      int count = friendlyByteBuf.readVarInt();
      if (count <= 0) {
         this.bounds = List.of();
         this.globalBounds = null;
      } else {
         this.bounds = new ArrayList<>();
         BlockPos globalMin = null;
         BlockPos globalMax = null;

         for (int ixx = 0; ixx < count; ixx++) {
            BlockPos one = friendlyByteBuf.readBlockPos();
            BlockPos two = friendlyByteBuf.readBlockPos();
            int minX = Math.min(one.getX(), two.getX());
            int minY = Math.min(one.getY(), two.getY());
            int minZ = Math.min(one.getZ(), two.getZ());
            int maxX = Math.max(one.getX(), two.getX());
            int maxY = Math.max(one.getY(), two.getY());
            int maxZ = Math.max(one.getZ(), two.getZ());
            if (globalMin == null) {
               globalMin = new BlockPos(minX, minY, minZ);
               globalMax = new BlockPos(maxX, maxY, maxZ);
            } else {
               globalMin = new BlockPos(Math.min(minX, globalMin.getX()), Math.min(minY, globalMin.getY()), Math.min(minZ, globalMin.getZ()));
               globalMax = new BlockPos(Math.max(maxX, globalMax.getX()), Math.max(maxY, globalMax.getY()), Math.max(maxZ, globalMax.getZ()));
            }

            this.bounds.add(new Box(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ)));
         }

         this.globalBounds = new Box(globalMin, globalMax);
      }
   }

   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeVarInt(this.allowedPermissions.size());

      for (AxiomPermission allowedPermission : this.allowedPermissions) {
         friendlyByteBuf.writeUtf(allowedPermission.getInternalName());
      }

      friendlyByteBuf.writeVarInt(this.deniedPermissions.size());

      for (AxiomPermission disallowedPermission : this.deniedPermissions) {
         friendlyByteBuf.writeUtf(disallowedPermission.getInternalName());
      }

      friendlyByteBuf.writeInt(this.infiniteReachLimit);
      friendlyByteBuf.writeVarInt(this.bounds.size());

      for (Box bound : this.bounds) {
         friendlyByteBuf.writeBlockPos(bound.pos1());
         friendlyByteBuf.writeBlockPos(bound.pos2());
      }
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         Restrictions that = (Restrictions)o;
         return this.infiniteReachLimit == that.infiniteReachLimit
            && this.allowedPermissions.equals(that.allowedPermissions)
            && this.deniedPermissions.equals(that.deniedPermissions)
            && this.bounds.equals(that.bounds)
            && Objects.equals(this.globalBounds, that.globalBounds);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.allowedPermissions.hashCode();
      result = 31 * result + this.deniedPermissions.hashCode();
      result = 31 * result + this.infiniteReachLimit;
      result = 31 * result + this.bounds.hashCode();
      return 31 * result + Objects.hashCode(this.globalBounds);
   }

   static {
      for (AxiomPermission value : AxiomPermission.values()) {
         PERMISSION_BY_NAME.put(value.getInternalName(), value);
      }
   }
}
