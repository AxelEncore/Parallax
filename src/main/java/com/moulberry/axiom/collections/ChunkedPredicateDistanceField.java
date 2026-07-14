package com.moulberry.axiom.collections;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.utils.BlockCondition;
import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

public class ChunkedPredicateDistanceField {
   private static final int CHAMFER_A = 108;
   private static final int CHAMFER_B = 153;
   private static final int CHAMFER_C = 187;
   private static final int MAX_VALUE = 2147483460;
   private final Long2ByteOpenHashMap sectionFlags = new Long2ByteOpenHashMap();
   private final Long2ObjectMap<int[]> map = new Long2ObjectOpenHashMap();
   private final ChunkedPredicateDistanceField.SectionProvider sectionProvider;
   private final BlockCondition predicate;
   private static final int FLAG_PROPAGATE_EDGE_X = 1;
   private static final int FLAG_PROPAGATE_EDGE_Y = 2;
   private static final int FLAG_PROPAGATE_EDGE_Z = 4;
   private static final int FLAG_PROPAGATE_FACE_X = 14;
   private static final int FLAG_PROPAGATE_FACE_Y = 21;
   private static final int FLAG_PROPAGATE_FACE_Z = 35;
   private static final int FLAG_PROPAGATE_ALL = 127;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private int[] lastChunk = null;

   public ChunkedPredicateDistanceField(ChunkedPredicateDistanceField.SectionProvider sectionProvider, BlockCondition predicate) {
      this.sectionProvider = sectionProvider;
      this.predicate = predicate;
   }

   private int[] computeCorner(int cx, int cy, int cz) {
      long key = BlockPos.asLong(cx, cy, cz);
      if (this.map.containsKey(key)) {
         return (int[])this.map.get(key);
      } else {
         int[] distances = new int[5832];
         Arrays.fill(distances, 2147483460);
         PalettedContainer<BlockState> container = this.sectionProvider.get(cx, cy, cz);
         if (container != null) {
            this.propagateInitial(distances, container);
         }

         this.map.put(key, distances);
         return distances;
      }
   }

   private int[] computeEdge(int cx, int cy, int cz, int[] p, int[] n, Axis axis) {
      int aStep;
      int bStep;
      int cStep;
      int propagateFlag;
      switch (axis) {
         case X:
            aStep = 1;
            bStep = 18;
            cStep = 324;
            propagateFlag = 1;
            break;
         case Y:
            aStep = 18;
            bStep = 1;
            cStep = 324;
            propagateFlag = 2;
            break;
         case Z:
            aStep = 324;
            bStep = 1;
            cStep = 18;
            propagateFlag = 4;
            break;
         default:
            throw new FaultyImplementationError();
      }

      long key = BlockPos.asLong(cx, cy, cz);
      int[] distances;
      boolean initial;
      if (this.map.containsKey(key)) {
         distances = (int[])this.map.get(key);
         if ((this.sectionFlags.get(key) & propagateFlag) == propagateFlag) {
            return distances;
         }

         initial = false;
      } else {
         distances = new int[5832];
         Arrays.fill(distances, 2147483460);
         initial = true;
      }

      for (int b = 0; b < 16; b++) {
         for (int c = 0; c < 16; c++) {
            distances[16 * aStep + b * bStep + c * cStep + 343] = p[0 * aStep + b * bStep + c * cStep + 343];
            distances[-1 * aStep + b * bStep + c * cStep + 343] = n[15 * aStep + b * bStep + c * cStep + 343];
         }
      }

      if (initial) {
         PalettedContainer<BlockState> container = this.sectionProvider.get(cx, cy, cz);
         if (container != null) {
            this.propagateInitial(distances, container);
         } else {
            this.propagate(distances);
         }

         this.map.put(key, distances);
      } else {
         this.propagate(distances);
      }

      this.sectionFlags.put(key, (byte)(this.sectionFlags.get(key) | propagateFlag));
      return distances;
   }

   private int[] computeFace(int cx, int cy, int cz, int[] zp, int[] zn, int[] pz, int[] nz, Axis axis) {
      int aStep;
      int bStep;
      int cStep;
      int propagateFlag;
      switch (axis) {
         case X:
            aStep = 1;
            bStep = 18;
            cStep = 324;
            propagateFlag = 14;
            break;
         case Y:
            aStep = 18;
            bStep = 1;
            cStep = 324;
            propagateFlag = 21;
            break;
         case Z:
            aStep = 324;
            bStep = 1;
            cStep = 18;
            propagateFlag = 35;
            break;
         default:
            throw new FaultyImplementationError();
      }

      long key = BlockPos.asLong(cx, cy, cz);
      int[] distances;
      boolean initial;
      if (this.map.containsKey(key)) {
         distances = (int[])this.map.get(key);
         if ((this.sectionFlags.get(key) & propagateFlag) == propagateFlag) {
            return distances;
         }

         initial = false;
      } else {
         distances = new int[5832];
         Arrays.fill(distances, 2147483460);
         initial = true;
      }

      for (int a = 0; a < 16; a++) {
         for (int c = 0; c < 16; c++) {
            distances[a * aStep + 16 * bStep + c * cStep + 343] = pz[a * aStep + 0 * bStep + c * cStep + 343];
            distances[a * aStep + -1 * bStep + c * cStep + 343] = nz[a * aStep + 15 * bStep + c * cStep + 343];
            distances[a * aStep + c * bStep + 16 * cStep + 343] = zp[a * aStep + c * bStep + 0 * cStep + 343];
            distances[a * aStep + c * bStep + -1 * cStep + 343] = zn[a * aStep + c * bStep + 15 * cStep + 343];
         }
      }

      if (initial) {
         PalettedContainer<BlockState> container = this.sectionProvider.get(cx, cy, cz);
         if (container != null) {
            this.propagateInitial(distances, container);
         } else {
            this.propagate(distances);
         }

         this.map.put(key, distances);
      } else {
         this.propagate(distances);
      }

      this.sectionFlags.put(key, (byte)(this.sectionFlags.get(key) | propagateFlag));
      return distances;
   }

   private int[] computeFull(int cx, int cy, int cz) {
      int[] ppp = this.computeCorner(cx + 1, cy + 1, cz + 1);
      int[] npp = this.computeCorner(cx - 1, cy + 1, cz + 1);
      int[] pnp = this.computeCorner(cx + 1, cy - 1, cz + 1);
      int[] nnp = this.computeCorner(cx - 1, cy - 1, cz + 1);
      int[] ppn = this.computeCorner(cx + 1, cy + 1, cz - 1);
      int[] npn = this.computeCorner(cx - 1, cy + 1, cz - 1);
      int[] pnn = this.computeCorner(cx + 1, cy - 1, cz - 1);
      int[] nnn = this.computeCorner(cx - 1, cy - 1, cz - 1);
      int[] zpp = this.computeEdge(cx, cy + 1, cz + 1, ppp, npp, Axis.X);
      int[] znp = this.computeEdge(cx, cy - 1, cz + 1, pnp, nnp, Axis.X);
      int[] zpn = this.computeEdge(cx, cy + 1, cz - 1, ppn, npn, Axis.X);
      int[] znn = this.computeEdge(cx, cy - 1, cz - 1, pnn, nnn, Axis.X);
      int[] pzp = this.computeEdge(cx + 1, cy, cz + 1, ppp, pnp, Axis.Y);
      int[] nzp = this.computeEdge(cx - 1, cy, cz + 1, npp, nnp, Axis.Y);
      int[] pzn = this.computeEdge(cx + 1, cy, cz - 1, ppn, pnn, Axis.Y);
      int[] nzn = this.computeEdge(cx - 1, cy, cz - 1, npn, nnn, Axis.Y);
      int[] ppz = this.computeEdge(cx + 1, cy + 1, cz, ppp, ppn, Axis.Z);
      int[] npz = this.computeEdge(cx - 1, cy + 1, cz, npp, npn, Axis.Z);
      int[] pnz = this.computeEdge(cx + 1, cy - 1, cz, pnp, pnn, Axis.Z);
      int[] nnz = this.computeEdge(cx - 1, cy - 1, cz, nnp, nnn, Axis.Z);
      int[] pzz = this.computeFace(cx + 1, cy, cz, pzp, pzn, ppz, pnz, Axis.X);
      int[] nzz = this.computeFace(cx - 1, cy, cz, nzp, nzn, npz, nnz, Axis.X);
      int[] zpz = this.computeFace(cx, cy + 1, cz, zpp, zpn, ppz, npz, Axis.Y);
      int[] znz = this.computeFace(cx, cy - 1, cz, znp, znn, pnz, nnz, Axis.Y);
      int[] zzp = this.computeFace(cx, cy, cz + 1, zpp, znp, pzp, nzp, Axis.Z);
      int[] zzn = this.computeFace(cx, cy, cz - 1, zpn, znn, pzn, nzn, Axis.Z);
      long key = BlockPos.asLong(cx, cy, cz);
      int[] distances;
      boolean initial;
      if (this.map.containsKey(key)) {
         distances = (int[])this.map.get(key);
         initial = false;
      } else {
         distances = new int[5832];
         Arrays.fill(distances, 2147483460);
         initial = true;
      }

      int xStep = 1;
      int yStep = 18;
      int zStep = 324;

      for (int a = 0; a < 16; a++) {
         for (int b = 0; b < 16; b++) {
            distances[16 + a * 18 + b * 324 + 343] = pzz[0 + a * 18 + b * 324 + 343];
            distances[-1 + a * 18 + b * 324 + 343] = nzz[15 + a * 18 + b * 324 + 343];
            distances[a * 1 + 288 + b * 324 + 343] = zpz[a * 1 + 0 + b * 324 + 343];
            distances[a * 1 + -18 + b * 324 + 343] = znz[a * 1 + 270 + b * 324 + 343];
            distances[a * 1 + b * 18 + 5184 + 343] = zzp[a * 1 + b * 18 + 0 + 343];
            distances[a * 1 + b * 18 + -324 + 343] = zzn[a * 1 + b * 18 + 4860 + 343];
         }
      }

      if (initial) {
         PalettedContainer<BlockState> container = this.sectionProvider.get(cx, cy, cz);
         if (container != null) {
            this.propagateInitial(distances, container);
         } else {
            this.propagate(distances);
         }

         this.map.put(key, distances);
      } else {
         this.propagate(distances);
      }

      this.sectionFlags.put(BlockPos.asLong(cx, cy, cz), (byte)127);
      return distances;
   }

   private void propagateInitial(int[] distances, PalettedContainer<BlockState> container) {
      int xIndexOffset = 1;
      int yIndexOffset = 18;
      int zIndexOffset = 324;

      for (int x = 0; x < 16; x++) {
         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               int index = (x + 1) * 1 + (y + 1) * 18 + (z + 1) * 324;
               BlockState blockState = (BlockState)container.get(x, y, z);
               if (this.predicate.matches(blockState)) {
                  distances[index] = 0;
               } else {
                  int least = 2147483460;
                  least = Math.min(least, distances[index - 1 - 18 - 324] + 187);
                  least = Math.min(least, distances[index - 1 - 18] + 153);
                  least = Math.min(least, distances[index - 1 - 18 + 324] + 187);
                  least = Math.min(least, distances[index - 1 - 324] + 153);
                  least = Math.min(least, distances[index - 1] + 108);
                  least = Math.min(least, distances[index - 1 + 324] + 153);
                  least = Math.min(least, distances[index - 1 + 18 - 324] + 187);
                  least = Math.min(least, distances[index - 1 + 18] + 153);
                  least = Math.min(least, distances[index - 1 + 18 + 324] + 187);
                  least = Math.min(least, distances[index - 18 - 324] + 153);
                  least = Math.min(least, distances[index - 18] + 108);
                  least = Math.min(least, distances[index - 18 + 324] + 153);
                  least = Math.min(least, distances[index - 324] + 108);
                  distances[index] = least;
               }
            }
         }
      }

      for (int x = 15; x >= 0; x--) {
         for (int y = 15; y >= 0; y--) {
            for (int zx = 15; zx >= 0; zx--) {
               int index = (x + 1) * 1 + (y + 1) * 18 + (zx + 1) * 324;
               int least = distances[index];
               least = Math.min(least, distances[index + 1 + 18 + 324] + 187);
               least = Math.min(least, distances[index + 1 + 18] + 153);
               least = Math.min(least, distances[index + 1 + 18 - 324] + 187);
               least = Math.min(least, distances[index + 1 + 324] + 153);
               least = Math.min(least, distances[index + 1] + 108);
               least = Math.min(least, distances[index + 1 - 324] + 153);
               least = Math.min(least, distances[index + 1 - 18 + 324] + 187);
               least = Math.min(least, distances[index + 1 - 18] + 153);
               least = Math.min(least, distances[index + 1 - 18 - 324] + 187);
               least = Math.min(least, distances[index + 18 + 324] + 153);
               least = Math.min(least, distances[index + 18] + 108);
               least = Math.min(least, distances[index + 18 - 324] + 153);
               least = Math.min(least, distances[index + 324] + 108);
               distances[index] = least;
            }
         }
      }
   }

   private void propagate(int[] distances) {
      int xIndexOffset = 1;
      int yIndexOffset = 18;
      int zIndexOffset = 324;

      for (int x = 0; x < 16; x++) {
         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               int index = (x + 1) * 1 + (y + 1) * 18 + (z + 1) * 324;
               int least = distances[index];
               least = Math.min(least, distances[index - 1 - 18 - 324] + 187);
               least = Math.min(least, distances[index - 1 - 18] + 153);
               least = Math.min(least, distances[index - 1 - 18 + 324] + 187);
               least = Math.min(least, distances[index - 1 - 324] + 153);
               least = Math.min(least, distances[index - 1] + 108);
               least = Math.min(least, distances[index - 1 + 324] + 153);
               least = Math.min(least, distances[index - 1 + 18 - 324] + 187);
               least = Math.min(least, distances[index - 1 + 18] + 153);
               least = Math.min(least, distances[index - 1 + 18 + 324] + 187);
               least = Math.min(least, distances[index - 18 - 324] + 153);
               least = Math.min(least, distances[index - 18] + 108);
               least = Math.min(least, distances[index - 18 + 324] + 153);
               least = Math.min(least, distances[index - 324] + 108);
               distances[index] = least;
            }
         }
      }

      for (int x = 15; x >= 0; x--) {
         for (int y = 15; y >= 0; y--) {
            for (int z = 15; z >= 0; z--) {
               int index = (x + 1) * 1 + (y + 1) * 18 + (z + 1) * 324;
               int least = distances[index];
               least = Math.min(least, distances[index + 1 + 18 + 324] + 187);
               least = Math.min(least, distances[index + 1 + 18] + 153);
               least = Math.min(least, distances[index + 1 + 18 - 324] + 187);
               least = Math.min(least, distances[index + 1 + 324] + 153);
               least = Math.min(least, distances[index + 1] + 108);
               least = Math.min(least, distances[index + 1 - 324] + 153);
               least = Math.min(least, distances[index + 1 - 18 + 324] + 187);
               least = Math.min(least, distances[index + 1 - 18] + 153);
               least = Math.min(least, distances[index + 1 - 18 - 324] + 187);
               least = Math.min(least, distances[index + 18 + 324] + 153);
               least = Math.min(least, distances[index + 18] + 108);
               least = Math.min(least, distances[index + 18 - 324] + 153);
               least = Math.min(least, distances[index + 324] + 108);
               distances[index] = least;
            }
         }
      }
   }

   public void reset() {
      this.sectionFlags.clear();
      this.map.clear();
      this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
      this.lastChunk = null;
   }

   public int getDistance(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      int[] array = this.getOrCreateChunk(xC, yC, zC);
      return array[(x & 15) + (y & 15) * 18 + (z & 15) * 18 * 18 + 343];
   }

   public int[] getOrCreateChunk(int cx, int cy, int cz) {
      long pos = BlockPos.asLong(cx, cy, cz);
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         int[] chunk;
         if (this.sectionFlags.get(pos) == 127) {
            chunk = (int[])this.map.get(pos);
         } else {
            chunk = this.computeFull(cx, cy, cz);
         }

         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   @FunctionalInterface
   public interface SectionProvider {
      PalettedContainer<BlockState> get(int var1, int var2, int var3);
   }
}
