package com.moulberry.axiom.tools.shape;

import com.moulberry.axiom.funcinterfaces.TriIntPredicate;
import java.util.Arrays;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TerrainDistanceField {
   private static final float SQRT_2 = (float)Math.sqrt(2.0);
   private static final float SQRT_3 = (float)Math.sqrt(3.0);

   public static void calculateChamferEuclideanTwo(
      TriIntPredicate first,
      TriIntPredicate second,
      int minX,
      int minY,
      int minZ,
      int maxX,
      int maxY,
      int maxZ,
      TerrainDistanceField.TwoDistanceConsumer consumer
   ) {
      int sizeX = maxX - minX + 1;
      int sizeY = maxY - minY + 1;
      int sizeZ = maxZ - minZ + 1;
      float[] distancesOne = new float[(sizeX + 2) * (sizeY + 2) * (sizeZ + 2)];
      Arrays.fill(distancesOne, Float.POSITIVE_INFINITY);
      float[] distancesTwo = new float[(sizeX + 2) * (sizeY + 2) * (sizeZ + 2)];
      Arrays.fill(distancesTwo, Float.POSITIVE_INFINITY);
      int xIndexOffset = (sizeY + 2) * (sizeZ + 2);
      int yIndexOffset = sizeZ + 2;
      int zIndexOffset = 1;

      for (int x = 0; x < sizeX; x++) {
         for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
               int index = (x + 1) * xIndexOffset + (y + 1) * yIndexOffset + (z + 1) * zIndexOffset;
               if (first.test(minX + x, minY + y, minZ + z)) {
                  distancesOne[index] = 0.0F;
               } else {
                  float least = Float.POSITIVE_INFINITY;
                  least = Math.min(least, distancesOne[index - xIndexOffset - yIndexOffset - zIndexOffset] + SQRT_3);
                  least = Math.min(least, distancesOne[index - xIndexOffset - yIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesOne[index - xIndexOffset - yIndexOffset + zIndexOffset] + SQRT_3);
                  least = Math.min(least, distancesOne[index - xIndexOffset - zIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesOne[index - xIndexOffset] + 1.0F);
                  least = Math.min(least, distancesOne[index - xIndexOffset + zIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesOne[index - xIndexOffset + yIndexOffset - zIndexOffset] + SQRT_3);
                  least = Math.min(least, distancesOne[index - xIndexOffset + yIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesOne[index - xIndexOffset + yIndexOffset + zIndexOffset] + SQRT_3);
                  least = Math.min(least, distancesOne[index - yIndexOffset - zIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesOne[index - yIndexOffset] + 1.0F);
                  least = Math.min(least, distancesOne[index - yIndexOffset + zIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesOne[index - zIndexOffset] + 1.0F);
                  distancesOne[index] = least;
               }

               if (second.test(minX + x, minY + y, minZ + z)) {
                  distancesTwo[index] = 0.0F;
               } else {
                  float least = Float.POSITIVE_INFINITY;
                  least = Math.min(least, distancesTwo[index - xIndexOffset - yIndexOffset - zIndexOffset] + SQRT_3);
                  least = Math.min(least, distancesTwo[index - xIndexOffset - yIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesTwo[index - xIndexOffset - yIndexOffset + zIndexOffset] + SQRT_3);
                  least = Math.min(least, distancesTwo[index - xIndexOffset - zIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesTwo[index - xIndexOffset] + 1.0F);
                  least = Math.min(least, distancesTwo[index - xIndexOffset + zIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesTwo[index - xIndexOffset + yIndexOffset - zIndexOffset] + SQRT_3);
                  least = Math.min(least, distancesTwo[index - xIndexOffset + yIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesTwo[index - xIndexOffset + yIndexOffset + zIndexOffset] + SQRT_3);
                  least = Math.min(least, distancesTwo[index - yIndexOffset - zIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesTwo[index - yIndexOffset] + 1.0F);
                  least = Math.min(least, distancesTwo[index - yIndexOffset + zIndexOffset] + SQRT_2);
                  least = Math.min(least, distancesTwo[index - zIndexOffset] + 1.0F);
                  distancesTwo[index] = least;
               }
            }
         }
      }

      for (int x = sizeX - 1; x >= 0; x--) {
         for (int y = sizeY - 1; y >= 0; y--) {
            for (int z = sizeZ - 1; z >= 0; z--) {
               int indexx = (x + 1) * xIndexOffset + (y + 1) * yIndexOffset + (z + 1) * zIndexOffset;
               float leastOne = distancesOne[indexx];
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset + yIndexOffset + zIndexOffset] + SQRT_3);
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset + yIndexOffset] + SQRT_2);
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset + yIndexOffset - zIndexOffset] + SQRT_3);
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset + zIndexOffset] + SQRT_2);
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset] + 1.0F);
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset - zIndexOffset] + SQRT_2);
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset - yIndexOffset + zIndexOffset] + SQRT_3);
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset - yIndexOffset] + SQRT_2);
               leastOne = Math.min(leastOne, distancesOne[indexx + xIndexOffset - yIndexOffset - zIndexOffset] + SQRT_3);
               leastOne = Math.min(leastOne, distancesOne[indexx + yIndexOffset + zIndexOffset] + SQRT_2);
               leastOne = Math.min(leastOne, distancesOne[indexx + yIndexOffset] + 1.0F);
               leastOne = Math.min(leastOne, distancesOne[indexx + yIndexOffset - zIndexOffset] + SQRT_2);
               leastOne = Math.min(leastOne, distancesOne[indexx + zIndexOffset] + 1.0F);
               float leastTwo = distancesTwo[indexx];
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset + yIndexOffset + zIndexOffset] + SQRT_3);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset + yIndexOffset] + SQRT_2);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset + yIndexOffset - zIndexOffset] + SQRT_3);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset + zIndexOffset] + SQRT_2);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset] + 1.0F);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset - zIndexOffset] + SQRT_2);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset - yIndexOffset + zIndexOffset] + SQRT_3);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset - yIndexOffset] + SQRT_2);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + xIndexOffset - yIndexOffset - zIndexOffset] + SQRT_3);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + yIndexOffset + zIndexOffset] + SQRT_2);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + yIndexOffset] + 1.0F);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + yIndexOffset - zIndexOffset] + SQRT_2);
               leastTwo = Math.min(leastTwo, distancesTwo[indexx + zIndexOffset] + 1.0F);
               consumer.accept(minX + x, minY + y, minZ + z, leastOne, leastTwo);
               distancesOne[indexx] = leastOne;
               distancesTwo[indexx] = leastTwo;
            }
         }
      }
   }

   public static float[] calculateChamferEuclidean(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      int sizeX = maxX - minX + 1;
      int sizeY = maxY - minY + 1;
      int sizeZ = maxZ - minZ + 1;
      float[] distances = new float[(sizeX + 2) * (sizeY + 2) * (sizeZ + 2)];
      Arrays.fill(distances, Float.POSITIVE_INFINITY);
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      int xIndexOffset = (sizeY + 2) * (sizeZ + 2);
      int yIndexOffset = sizeZ + 2;
      int zIndexOffset = 1;

      for (int x = 0; x < sizeX; x++) {
         for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
               int index = (x + 1) * xIndexOffset + (y + 1) * yIndexOffset + (z + 1) * zIndexOffset;
               mutableBlockPos.set(minX + x, minY + y, minZ + z);
               BlockState blockState = level.getBlockState(mutableBlockPos);
               if (blockState.blocksMotion()) {
                  distances[index] = 0.0F;
               } else {
                  float least = Float.POSITIVE_INFINITY;
                  least = Math.min(least, distances[index - xIndexOffset - yIndexOffset - zIndexOffset] + SQRT_3);
                  least = Math.min(least, distances[index - xIndexOffset - yIndexOffset] + SQRT_2);
                  least = Math.min(least, distances[index - xIndexOffset - yIndexOffset + zIndexOffset] + SQRT_3);
                  least = Math.min(least, distances[index - xIndexOffset - zIndexOffset] + SQRT_2);
                  least = Math.min(least, distances[index - xIndexOffset] + 1.0F);
                  least = Math.min(least, distances[index - xIndexOffset + zIndexOffset] + SQRT_2);
                  least = Math.min(least, distances[index - xIndexOffset + yIndexOffset - zIndexOffset] + SQRT_3);
                  least = Math.min(least, distances[index - xIndexOffset + yIndexOffset] + SQRT_2);
                  least = Math.min(least, distances[index - xIndexOffset + yIndexOffset + zIndexOffset] + SQRT_3);
                  least = Math.min(least, distances[index - yIndexOffset - zIndexOffset] + SQRT_2);
                  least = Math.min(least, distances[index - yIndexOffset] + 1.0F);
                  least = Math.min(least, distances[index - yIndexOffset + zIndexOffset] + SQRT_2);
                  least = Math.min(least, distances[index - zIndexOffset] + 1.0F);
                  distances[index] = least;
               }
            }
         }
      }

      for (int x = sizeX - 1; x >= 0; x--) {
         for (int y = sizeY - 1; y >= 0; y--) {
            for (int zx = sizeZ - 1; zx >= 0; zx--) {
               int index = (x + 1) * xIndexOffset + (y + 1) * yIndexOffset + (zx + 1) * zIndexOffset;
               float least = distances[index];
               least = Math.min(least, distances[index + xIndexOffset + yIndexOffset + zIndexOffset] + SQRT_3);
               least = Math.min(least, distances[index + xIndexOffset + yIndexOffset] + SQRT_2);
               least = Math.min(least, distances[index + xIndexOffset + yIndexOffset - zIndexOffset] + SQRT_3);
               least = Math.min(least, distances[index + xIndexOffset + zIndexOffset] + SQRT_2);
               least = Math.min(least, distances[index + xIndexOffset] + 1.0F);
               least = Math.min(least, distances[index + xIndexOffset - zIndexOffset] + SQRT_2);
               least = Math.min(least, distances[index + xIndexOffset - yIndexOffset + zIndexOffset] + SQRT_3);
               least = Math.min(least, distances[index + xIndexOffset - yIndexOffset] + SQRT_2);
               least = Math.min(least, distances[index + xIndexOffset - yIndexOffset - zIndexOffset] + SQRT_3);
               least = Math.min(least, distances[index + yIndexOffset + zIndexOffset] + SQRT_2);
               least = Math.min(least, distances[index + yIndexOffset] + 1.0F);
               least = Math.min(least, distances[index + yIndexOffset - zIndexOffset] + SQRT_2);
               least = Math.min(least, distances[index + zIndexOffset] + 1.0F);
               distances[index] = least;
            }
         }
      }

      return distances;
   }

   @FunctionalInterface
   public interface TwoDistanceConsumer {
      void accept(int var1, int var2, int var3, float var4, float var5);
   }
}
