package com.moulberry.axiom.scaling;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import net.minecraft.world.level.block.state.BlockState;

public class Scale3x {
   private static boolean isSolid(BlockState blockState) {
      return !blockState.canBeReplaced();
   }

   private static boolean isSolidOrNonSolid(BlockState blockState, boolean solid) {
      return !blockState.isAir() && blockState.canBeReplaced() != solid;
   }

   public static ChunkedBlockRegion scale3x(ChunkedBlockRegion in, boolean postProcessing) {
      ChunkedBlockRegion out = new ChunkedBlockRegion();
      PositionSet solid = new PositionSet();
      PositionSet nonSolid = new PositionSet();
      in.forEachEntry((x, y, z, block) -> {
         if (!block.isAir()) {
            boolean isSolid = isSolid(block);
            PositionSet set = isSolid ? solid : nonSolid;
            boolean plusXAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x + 1, y, z), isSolid);
            boolean plusYAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x, y + 1, z), isSolid);
            boolean plusZAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x, y, z + 1), isSolid);
            boolean minusXAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x - 1, y, z), isSolid);
            boolean minusYAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x, y - 1, z), isSolid);
            boolean minusZAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x, y, z - 1), isSolid);

            for (int xo = 0; xo < 3; xo++) {
               for (int yo = 0; yo < 3; yo++) {
                  for (int zo = 0; zo < 3; zo++) {
                     set.add(x * 3 + xo, y * 3 + yo, z * 3 + zo);
                  }
               }
            }

            removeEdge(in, set, x, y, z, -1, 0, 0, 0, -1, 0, 0, 0, 1, isSolid);
            removeEdge(in, set, x, y, z, 1, 0, 0, 0, -1, 0, 0, 0, 1, isSolid);
            removeEdge(in, set, x, y, z, -1, 0, 0, 0, 1, 0, 0, 0, 1, isSolid);
            removeEdge(in, set, x, y, z, 1, 0, 0, 0, 1, 0, 0, 0, 1, isSolid);
            removeEdge(in, set, x, y, z, -1, 0, 0, 0, 0, -1, 0, 1, 0, isSolid);
            removeEdge(in, set, x, y, z, 1, 0, 0, 0, 0, -1, 0, 1, 0, isSolid);
            removeEdge(in, set, x, y, z, -1, 0, 0, 0, 0, 1, 0, 1, 0, isSolid);
            removeEdge(in, set, x, y, z, 1, 0, 0, 0, 0, 1, 0, 1, 0, isSolid);
            removeEdge(in, set, x, y, z, 0, -1, 0, 0, 0, -1, 1, 0, 0, isSolid);
            removeEdge(in, set, x, y, z, 0, 1, 0, 0, 0, -1, 1, 0, 0, isSolid);
            removeEdge(in, set, x, y, z, 0, -1, 0, 0, 0, 1, 1, 0, 0, isSolid);
            removeEdge(in, set, x, y, z, 0, 1, 0, 0, 0, 1, 1, 0, 0, isSolid);
            handleCorner(in, set, x, y, z, plusXAir, plusYAir, plusZAir, 1, 1, 1, isSolid);
            handleCorner(in, set, x, y, z, minusXAir, plusYAir, plusZAir, -1, 1, 1, isSolid);
            handleCorner(in, set, x, y, z, plusXAir, minusYAir, plusZAir, 1, -1, 1, isSolid);
            handleCorner(in, set, x, y, z, minusXAir, minusYAir, plusZAir, -1, -1, 1, isSolid);
            handleCorner(in, set, x, y, z, plusXAir, plusYAir, minusZAir, 1, 1, -1, isSolid);
            handleCorner(in, set, x, y, z, minusXAir, plusYAir, minusZAir, -1, 1, -1, isSolid);
            handleCorner(in, set, x, y, z, plusXAir, minusYAir, minusZAir, 1, -1, -1, isSolid);
            handleCorner(in, set, x, y, z, minusXAir, minusYAir, minusZAir, -1, -1, -1, isSolid);
         }
      });
      PositionSet newSolid = new PositionSet();
      PositionSet newNonSolid = new PositionSet();
      in.forEachChunk((cx, cy, cz, blocks) -> {
         cx <<= 4;
         cy <<= 4;
         cz <<= 4;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  BlockState t = blocks[index++];
                  if (t == null || t.isAir()) {
                     addEdges(in, newSolid, cx + x, cy + y, cz + z, true);
                     addEdges(in, newNonSolid, cx + x, cy + y, cz + z, false);
                  } else if (!isSolid(t)) {
                     addEdges(in, newSolid, cx + x, cy + y, cz + z, true);
                  } else {
                     addEdges(in, newNonSolid, cx + x, cy + y, cz + z, false);
                  }
               }
            }
         }
      });
      newSolid.forEach(solid::add);
      newNonSolid.forEach(nonSolid::add);
      nonSolid.forEach((x, y, z) -> {
         BlockState blockState = getBlockState(in, x, y, z, false);
         if (!blockState.isAir()) {
            out.addBlockWithoutDirty(x, y, z, blockState);
         }
      });
      solid.forEach((x, y, z) -> {
         BlockState blockState = getBlockState(in, x, y, z, true);
         if (!blockState.isAir()) {
            out.addBlockWithoutDirty(x, y, z, blockState);
         }
      });
      out.dirtyAll();
      return out;
   }

   private static void addEdges(ChunkedBlockRegion in, PositionSet newSolid, int nx, int ny, int nz, boolean isSolid) {
      addEdge(in, newSolid, nx, ny, nz, -1, 0, 0, 0, -1, 0, 0, 0, 1, isSolid);
      addEdge(in, newSolid, nx, ny, nz, 1, 0, 0, 0, -1, 0, 0, 0, 1, isSolid);
      addEdge(in, newSolid, nx, ny, nz, -1, 0, 0, 0, 1, 0, 0, 0, 1, isSolid);
      addEdge(in, newSolid, nx, ny, nz, 1, 0, 0, 0, 1, 0, 0, 0, 1, isSolid);
      addEdge(in, newSolid, nx, ny, nz, -1, 0, 0, 0, 0, -1, 0, 1, 0, isSolid);
      addEdge(in, newSolid, nx, ny, nz, 1, 0, 0, 0, 0, -1, 0, 1, 0, isSolid);
      addEdge(in, newSolid, nx, ny, nz, -1, 0, 0, 0, 0, 1, 0, 1, 0, isSolid);
      addEdge(in, newSolid, nx, ny, nz, 1, 0, 0, 0, 0, 1, 0, 1, 0, isSolid);
      addEdge(in, newSolid, nx, ny, nz, 0, -1, 0, 0, 0, -1, 1, 0, 0, isSolid);
      addEdge(in, newSolid, nx, ny, nz, 0, 1, 0, 0, 0, -1, 1, 0, 0, isSolid);
      addEdge(in, newSolid, nx, ny, nz, 0, -1, 0, 0, 0, 1, 1, 0, 0, isSolid);
      addEdge(in, newSolid, nx, ny, nz, 0, 1, 0, 0, 0, 1, 1, 0, 0, isSolid);
   }

   private static BlockState getBlockState(ChunkedBlockRegion in, int x, int y, int z, boolean isSolid) {
      int bx = Math.floorDiv(x, 3);
      int by = Math.floorDiv(y, 3);
      int bz = Math.floorDiv(z, 3);
      int xo = x - bx * 3 - 1;
      int yo = y - by * 3 - 1;
      int zo = z - bz * 3 - 1;
      BlockState center = in.getBlockStateOrAir(bx, by, bz);
      int nonZero = Math.abs(xo) + Math.abs(yo) + Math.abs(zo);
      if (center.isAir()) {
         if (nonZero == 1) {
            BlockState state = in.getBlockStateOrAir(bx + xo, by + yo, bz + zo);
            if (isSolidOrNonSolid(state, isSolid)) {
               return state;
            }
         } else if (nonZero == 2) {
            BlockState faceTwo;
            BlockState edge;
            BlockState faceOne;
            if (xo == 0) {
               faceOne = in.getBlockStateOrAir(bx, by + yo, bz);
               faceTwo = in.getBlockStateOrAir(bx, by, bz + zo);
               edge = in.getBlockStateOrAir(bx, by + yo, bz + zo);
            } else if (yo == 0) {
               faceOne = in.getBlockStateOrAir(bx + xo, by, bz);
               faceTwo = in.getBlockStateOrAir(bx, by, bz + zo);
               edge = in.getBlockStateOrAir(bx + xo, by, bz + zo);
            } else {
               faceOne = in.getBlockStateOrAir(bx, by + yo, bz);
               faceTwo = in.getBlockStateOrAir(bx + zo, by, bz);
               edge = in.getBlockStateOrAir(bx + zo, by + yo, bz);
            }

            if (isSolidOrNonSolid(faceOne, isSolid) && faceOne == edge) {
               return faceOne;
            }

            if (isSolidOrNonSolid(faceTwo, isSolid)) {
               return faceTwo;
            }

            if (isSolidOrNonSolid(edge, isSolid)) {
               return edge;
            }
         } else if (nonZero == 3) {
            BlockState neighborX = in.getBlockStateOrAir(bx + xo, by, bz);
            BlockState neighborY = in.getBlockStateOrAir(bx, by + yo, bz);
            BlockState neighborZ = in.getBlockStateOrAir(bx, by, bz + zo);
            if (isSolidOrNonSolid(neighborY, isSolid)) {
               if (isSolidOrNonSolid(neighborX, isSolid) && neighborX == neighborZ) {
                  return neighborX;
               }

               return neighborY;
            }

            if (isSolidOrNonSolid(neighborX, isSolid)) {
               return neighborX;
            }

            if (isSolidOrNonSolid(neighborZ, isSolid)) {
               return neighborZ;
            }
         }
      } else if (nonZero == 2) {
         BlockState faceOnex;
         BlockState faceTwox;
         BlockState edgex;
         if (xo == 0) {
            faceOnex = in.getBlockStateOrAir(bx, by + yo, bz);
            faceTwox = in.getBlockStateOrAir(bx, by, bz + zo);
            edgex = in.getBlockStateOrAir(bx, by + yo, bz + zo);
         } else if (yo == 0) {
            faceOnex = in.getBlockStateOrAir(bx + xo, by, bz);
            faceTwox = in.getBlockStateOrAir(bx, by, bz + zo);
            edgex = in.getBlockStateOrAir(bx + xo, by, bz + zo);
         } else {
            faceOnex = in.getBlockStateOrAir(bx, by + yo, bz);
            faceTwox = in.getBlockStateOrAir(bx + zo, by, bz);
            edgex = in.getBlockStateOrAir(bx + zo, by + yo, bz);
         }

         if (center == edgex) {
            return center;
         }

         if (isSolidOrNonSolid(faceOnex, isSolid) && faceOnex == faceTwox) {
            return faceOnex;
         }
      } else if (nonZero == 3) {
         BlockState neighborXx = in.getBlockStateOrAir(bx + xo, by, bz);
         BlockState neighborYx = in.getBlockStateOrAir(bx, by + yo, bz);
         BlockState neighborZx = in.getBlockStateOrAir(bx, by, bz + zo);
         if (center == neighborYx || center == neighborXx || center == neighborZx) {
            return center;
         }

         if (isSolidOrNonSolid(neighborYx, isSolid)) {
            if (isSolidOrNonSolid(neighborXx, isSolid) && neighborXx == neighborZx) {
               return neighborXx;
            }

            if (neighborYx == neighborXx || neighborYx == neighborZx) {
               return neighborYx;
            }
         } else if (isSolidOrNonSolid(neighborXx, isSolid) && neighborXx == neighborZx) {
            return neighborXx;
         }
      }

      if (center.isAir()) {
         BlockState state = in.getBlockStateOrAir(bx, by + 1, bz);
         if (isSolidOrNonSolid(state, isSolid)) {
            return state;
         }

         state = in.getBlockStateOrAir(bx, by - 1, bz);
         if (isSolidOrNonSolid(state, isSolid)) {
            return state;
         }

         state = in.getBlockStateOrAir(bx + 1, by, bz);
         if (isSolidOrNonSolid(state, isSolid)) {
            return state;
         }

         state = in.getBlockStateOrAir(bx - 1, by, bz);
         if (isSolidOrNonSolid(state, isSolid)) {
            return state;
         }

         state = in.getBlockStateOrAir(bx, by, bz + 1);
         if (isSolidOrNonSolid(state, isSolid)) {
            return state;
         }

         state = in.getBlockStateOrAir(bx, by, bz - 1);
         if (isSolidOrNonSolid(state, isSolid)) {
            return state;
         }
      }

      return center;
   }

   private static void handleCorner(
      ChunkedBlockRegion in, PositionSet set, int x, int y, int z, boolean airX, boolean airY, boolean airZ, int dirX, int dirY, int dirZ, boolean isSolid
   ) {
      if (airX
         && airY
         && airZ
         && !isSolidOrNonSolid(in.getBlockStateOrAir(x, y + dirY, z + dirZ), isSolid)
         && !isSolidOrNonSolid(in.getBlockStateOrAir(x + dirX, y, z + dirZ), isSolid)
         && !isSolidOrNonSolid(in.getBlockStateOrAir(x + dirX, y + dirY, z), isSolid)) {
         if (!isSolidOrNonSolid(in.getBlockStateOrAir(x + dirX, y + dirY, z + dirZ), isSolid)) {
            set.remove(x * 3 + 1 + dirX, y * 3 + 1 + dirY, z * 3 + 1 + dirZ);
         } else {
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX + 1, y * 3 + 1 + dirY, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX - 1, y * 3 + 1 + dirY, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY + 1, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY - 1, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY, z * 3 + 1 + dirZ + 1);
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY, z * 3 + 1 + dirZ - 1);
            set.add(x * 3 + 1 + dirX + 1, y * 3 + 1 + dirY + 1, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX + 1, y * 3 + 1 + dirY - 1, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX - 1, y * 3 + 1 + dirY + 1, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX - 1, y * 3 + 1 + dirY - 1, z * 3 + 1 + dirZ);
            set.add(x * 3 + 1 + dirX + 1, y * 3 + 1 + dirY, z * 3 + 1 + dirZ + 1);
            set.add(x * 3 + 1 + dirX + 1, y * 3 + 1 + dirY, z * 3 + 1 + dirZ - 1);
            set.add(x * 3 + 1 + dirX - 1, y * 3 + 1 + dirY, z * 3 + 1 + dirZ + 1);
            set.add(x * 3 + 1 + dirX - 1, y * 3 + 1 + dirY, z * 3 + 1 + dirZ - 1);
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY + 1, z * 3 + 1 + dirZ + 1);
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY + 1, z * 3 + 1 + dirZ - 1);
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY - 1, z * 3 + 1 + dirZ + 1);
            set.add(x * 3 + 1 + dirX, y * 3 + 1 + dirY - 1, z * 3 + 1 + dirZ - 1);
            set.add(x * 3 + 1 + 1, y * 3 + 1 + 1, z * 3 + 1);
            set.add(x * 3 + 1 + 1, y * 3 + 1 - 1, z * 3 + 1);
            set.add(x * 3 + 1 - 1, y * 3 + 1 + 1, z * 3 + 1);
            set.add(x * 3 + 1 - 1, y * 3 + 1 - 1, z * 3 + 1);
            set.add(x * 3 + 1 + 1, y * 3 + 1, z * 3 + 1 + 1);
            set.add(x * 3 + 1 + 1, y * 3 + 1, z * 3 + 1 - 1);
            set.add(x * 3 + 1 - 1, y * 3 + 1, z * 3 + 1 + 1);
            set.add(x * 3 + 1 - 1, y * 3 + 1, z * 3 + 1 - 1);
            set.add(x * 3 + 1, y * 3 + 1 + 1, z * 3 + 1 + 1);
            set.add(x * 3 + 1, y * 3 + 1 + 1, z * 3 + 1 - 1);
            set.add(x * 3 + 1, y * 3 + 1 - 1, z * 3 + 1 + 1);
            set.add(x * 3 + 1, y * 3 + 1 - 1, z * 3 + 1 - 1);
         }
      }
   }

   private static void addEdge(
      ChunkedBlockRegion in,
      PositionSet newSet,
      int nx,
      int ny,
      int nz,
      int edgeX1,
      int edgeY1,
      int edgeZ1,
      int edgeX2,
      int edgeY2,
      int edgeZ2,
      int edgeX3,
      int edgeY3,
      int edgeZ3,
      boolean isSolid
   ) {
      if (isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1, ny + edgeY1, nz + edgeZ1), isSolid)
         && isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX2, ny + edgeY2, nz + edgeZ2), isSolid)) {
         boolean minus1 = isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1 - edgeX3, ny + edgeY1 - edgeY3, nz + edgeZ1 - edgeZ3), isSolid);
         boolean minus2 = isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX2 - edgeX3, ny + edgeY2 - edgeY3, nz + edgeZ2 - edgeZ3), isSolid);
         boolean plus1 = isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1 + edgeX3, ny + edgeY1 + edgeY3, nz + edgeZ1 + edgeZ3), isSolid);
         boolean plus2 = isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX2 + edgeX3, ny + edgeY2 + edgeY3, nz + edgeZ2 + edgeZ3), isSolid);
         if ((!minus1 || !minus2) && (!plus1 || !plus2)) {
            boolean minusBoth = isSolidOrNonSolid(
               in.getBlockStateOrAir(nx + edgeX1 + edgeX2 - edgeX3, ny + edgeY1 + edgeY2 - edgeY3, nz + edgeZ1 + edgeZ2 - edgeZ3), isSolid
            );
            boolean plusBoth = isSolidOrNonSolid(
               in.getBlockStateOrAir(nx + edgeX1 + edgeX2 + edgeX3, ny + edgeY1 + edgeY2 + edgeY3, nz + edgeZ1 + edgeZ2 + edgeZ3), isSolid
            );
            if (!minusBoth && !plusBoth) {
               newSet.add(nx * 3 + 1 + edgeX1 + edgeX2 - edgeX3, ny * 3 + 1 + edgeY1 + edgeY2 - edgeY3, nz * 3 + 1 + edgeZ1 + edgeZ2 - edgeZ3);
               newSet.add(nx * 3 + 1 + edgeX1 + edgeX2, ny * 3 + 1 + edgeY1 + edgeY2, nz * 3 + 1 + edgeZ1 + edgeZ2);
               newSet.add(nx * 3 + 1 + edgeX1 + edgeX2 + edgeX3, ny * 3 + 1 + edgeY1 + edgeY2 + edgeY3, nz * 3 + 1 + edgeZ1 + edgeZ2 + edgeZ3);
               newSet.add(nx * 3 + 1 + edgeX1, ny * 3 + 1 + edgeY1, nz * 3 + 1 + edgeZ1);
               newSet.add(nx * 3 + 1 + edgeX2, ny * 3 + 1 + edgeY2, nz * 3 + 1 + edgeZ2);
               if (!minus1 && !plus1) {
                  newSet.add(nx * 3 + 1 + edgeX1 * 2 - edgeX3, ny * 3 + 1 + edgeY1 * 2 - edgeY3, nz * 3 + 1 + edgeZ1 * 2 - edgeZ3);
                  newSet.add(nx * 3 + 1 + edgeX1 * 2 + edgeX3, ny * 3 + 1 + edgeY1 * 2 + edgeY3, nz * 3 + 1 + edgeZ1 * 2 + edgeZ3);
                  newSet.add(nx * 3 + 1 + edgeX1 * 2 - edgeX2, ny * 3 + 1 + edgeY1 * 2 - edgeY2, nz * 3 + 1 + edgeZ1 * 2 - edgeZ2);
               }

               if (!minus2 && !plus2) {
                  newSet.add(nx * 3 + 1 + edgeX2 * 2 - edgeX3, ny * 3 + 1 + edgeY2 * 2 - edgeY3, nz * 3 + 1 + edgeZ2 * 2 - edgeZ3);
                  newSet.add(nx * 3 + 1 + edgeX2 * 2 + edgeX3, ny * 3 + 1 + edgeY2 * 2 + edgeY3, nz * 3 + 1 + edgeZ2 * 2 + edgeZ3);
                  newSet.add(nx * 3 + 1 + edgeX2 * 2 - edgeX1, ny * 3 + 1 + edgeY2 * 2 - edgeY1, nz * 3 + 1 + edgeZ2 * 2 - edgeZ1);
               }

               return;
            }
         }

         if (minus1 && minus2) {
            newSet.add(nx * 3 + 1 + edgeX1 + edgeX2 - edgeX3, ny * 3 + 1 + edgeY1 + edgeY2 - edgeY3, nz * 3 + 1 + edgeZ1 + edgeZ2 - edgeZ3);
         }

         newSet.add(nx * 3 + 1 + edgeX1 + edgeX2, ny * 3 + 1 + edgeY1 + edgeY2, nz * 3 + 1 + edgeZ1 + edgeZ2);
         if (plus1 && plus2) {
            newSet.add(nx * 3 + 1 + edgeX1 + edgeX2 + edgeX3, ny * 3 + 1 + edgeY1 + edgeY2 + edgeY3, nz * 3 + 1 + edgeZ1 + edgeZ2 + edgeZ3);
         }
      }
   }

   private static void removeEdge(
      ChunkedBlockRegion in,
      PositionSet set,
      int nx,
      int ny,
      int nz,
      int edgeX1,
      int edgeY1,
      int edgeZ1,
      int edgeX2,
      int edgeY2,
      int edgeZ2,
      int edgeX3,
      int edgeY3,
      int edgeZ3,
      boolean isSolid
   ) {
      if (!isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1, ny + edgeY1, nz + edgeZ1), isSolid)
         && !isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX2, ny + edgeY2, nz + edgeZ2), isSolid)
         && !isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1 + edgeX2, ny + edgeY1 + edgeY2, nz + edgeZ1 + edgeZ2), isSolid)) {
         if (!isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1 - edgeX3, ny + edgeY1 - edgeY3, nz + edgeZ1 - edgeZ3), isSolid)
            && !isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX2 - edgeX3, ny + edgeY2 - edgeY3, nz + edgeZ2 - edgeZ3), isSolid)
            && !isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1 + edgeX2 - edgeX3, ny + edgeY1 + edgeY2 - edgeY3, nz + edgeZ1 + edgeZ2 - edgeZ3), isSolid)) {
            set.remove(nx * 3 + 1 + edgeX1 + edgeX2 - edgeX3, ny * 3 + 1 + edgeY1 + edgeY2 - edgeY3, nz * 3 + 1 + edgeZ1 + edgeZ2 - edgeZ3);
         }

         set.remove(nx * 3 + 1 + edgeX1 + edgeX2, ny * 3 + 1 + edgeY1 + edgeY2, nz * 3 + 1 + edgeZ1 + edgeZ2);
         if (!isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1 + edgeX3, ny + edgeY1 + edgeY3, nz + edgeZ1 + edgeZ3), isSolid)
            && !isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX2 + edgeX3, ny + edgeY2 + edgeY3, nz + edgeZ2 + edgeZ3), isSolid)
            && !isSolidOrNonSolid(in.getBlockStateOrAir(nx + edgeX1 + edgeX2 + edgeX3, ny + edgeY1 + edgeY2 + edgeY3, nz + edgeZ1 + edgeZ2 + edgeZ3), isSolid)) {
            set.remove(nx * 3 + 1 + edgeX1 + edgeX2 + edgeX3, ny * 3 + 1 + edgeY1 + edgeY2 + edgeY3, nz * 3 + 1 + edgeZ1 + edgeZ2 + edgeZ3);
         }
      }
   }
}
