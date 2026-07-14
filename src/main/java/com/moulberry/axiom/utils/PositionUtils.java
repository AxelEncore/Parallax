package com.moulberry.axiom.utils;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class PositionUtils {
   public static final long MIN_POSITION_LONG = BlockPos.asLong(-33554432, -2048, -33554432);

   public static Direction getNearestDirection(double x, double y, double z) {
      Direction direction = Direction.NORTH;
      double largestDot = Float.MIN_VALUE;

      for (Direction candidate : Direction.values()) {
         double dot = x * candidate.getStepX() + y * candidate.getStepY() + z * candidate.getStepZ();
         if (dot > largestDot) {
            largestDot = dot;
            direction = candidate;
         }
      }

      return direction;
   }

   public static Direction[] orderedByNearest(Vec3 look) {
      double absX = Math.abs(look.x);
      double absY = Math.abs(look.y);
      double absZ = Math.abs(look.z);
      Direction directionX = look.x > 0.0 ? Direction.EAST : Direction.WEST;
      Direction directionY = look.y > 0.0 ? Direction.UP : Direction.DOWN;
      Direction directionZ = look.z > 0.0 ? Direction.SOUTH : Direction.NORTH;
      if (absX > absZ) {
         if (absY > absX) {
            return new Direction[]{directionY, directionX, directionZ, directionZ.getOpposite(), directionX.getOpposite(), directionY.getOpposite()};
         } else {
            return absZ > absY
               ? new Direction[]{directionX, directionZ, directionY, directionY.getOpposite(), directionZ.getOpposite(), directionX.getOpposite()}
               : new Direction[]{directionX, directionY, directionZ, directionZ.getOpposite(), directionY.getOpposite(), directionX.getOpposite()};
         }
      } else if (absY > absZ) {
         return new Direction[]{directionY, directionZ, directionX, directionX.getOpposite(), directionZ.getOpposite(), directionY.getOpposite()};
      } else {
         return absX > absY
            ? new Direction[]{directionZ, directionX, directionY, directionY.getOpposite(), directionX.getOpposite(), directionZ.getOpposite()}
            : new Direction[]{directionZ, directionY, directionX, directionX.getOpposite(), directionY.getOpposite(), directionZ.getOpposite()};
      }
   }

   public static Direction directionFromDelta(int x, int y, int z) {
      if (x == 0) {
         if (y == 0) {
            if (z > 0) {
               return Direction.SOUTH;
            }

            if (z < 0) {
               return Direction.NORTH;
            }
         } else if (z == 0) {
            if (y > 0) {
               return Direction.UP;
            }

            return Direction.DOWN;
         }
      } else if (y == 0 && z == 0) {
         if (x > 0) {
            return Direction.EAST;
         }

         return Direction.WEST;
      }

      return null;
   }

   public static BlockPos findClosestPoint(BlockPos min, BlockPos max, BlockPos point) {
      return new BlockPos(
         Math.max(min.getX(), Math.min(max.getX(), point.getX())),
         Math.max(min.getY(), Math.min(max.getY(), point.getY())),
         Math.max(min.getZ(), Math.min(max.getZ(), point.getZ()))
      );
   }

   public static BlockPos findFurthestPoint(BlockPos min, BlockPos max, BlockPos point) {
      BlockPos furthest = null;
      int furthestDistanceSq = Integer.MIN_VALUE;

      for (int i = 0; i < 8; i++) {
         int x = (i & 1) == 0 ? min.getX() : max.getX();
         int y = (i & 2) == 0 ? min.getY() : max.getY();
         int z = (i & 4) == 0 ? min.getZ() : max.getZ();
         int dx = x - point.getX();
         int dy = y - point.getY();
         int dz = z - point.getZ();
         int distanceSq = dx * dx + dy * dy + dz * dz;
         if (furthest == null || distanceSq > furthestDistanceSq) {
            furthest = new BlockPos(x, y, z);
            furthestDistanceSq = distanceSq;
         }
      }

      return furthest;
   }

   static {
      if (MIN_POSITION_LONG != -9223371899415820288L) {
         throw new FaultyImplementationError("BlockPos representation changed!");
      }
   }
}
