package com.moulberry.axiom.pather.async;

import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.funcinterfaces.IntIntIntFloatConsumer;
import com.moulberry.axiom.utils.Box;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;

public class AsyncToolPatherMinSDF implements AsyncToolPather {
   private final int[] sphere;
   private final int[][] cardinalHalfBalls;
   private final int[][][] bicardinalBallSlices;
   private final Position2FloatMap minDistances = new Position2FloatMap(Float.MAX_VALUE);
   private final IntIntIntFloatConsumer consumer;
   private final MutableBlockPos lastPosition = new MutableBlockPos();
   protected final ArrayBlockingQueue<int[]> outputData = new ArrayBlockingQueue<>(128);

   public AsyncToolPatherMinSDF(BrushShape brushShape, IntIntIntFloatConsumer consumer) {
      this.consumer = consumer;
      IntList sphere = new IntArrayList();
      List<IntList> cardinalHalfSpheres = List.of(
         new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList()
      );
      List<List<IntArrayList>> bicardinalSphereSlices = new ArrayList<>();

      for (int i = 0; i < Direction.values().length; i++) {
         List<IntArrayList> list = new ArrayList<>();

         for (int j = 0; j < Direction.values().length; j++) {
            list.add(new IntArrayList());
         }

         bicardinalSphereSlices.add(list);
      }

      Box bounding = brushShape.boundingBox();
      int minX = bounding.pos1().getX();
      int minY = bounding.pos1().getY();
      int minZ = bounding.pos1().getZ();
      int maxX = bounding.pos2().getX();
      int maxY = bounding.pos2().getY();
      int maxZ = bounding.pos2().getZ();

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               if (brushShape.isInsideShape(x, y, z)) {
                  float sdf = brushShape.sdf(x, y, z);
                  int encodedDistance = Float.floatToIntBits(sdf);
                  sphere.add(x);
                  sphere.add(y);
                  sphere.add(z);
                  sphere.add(encodedDistance);

                  for (Direction direction : Direction.values()) {
                     int x2 = x + direction.getStepX();
                     int y2 = y + direction.getStepY();
                     int z2 = z + direction.getStepZ();
                     float sdf2 = brushShape.sdf(x2, y2, z2);
                     if (!brushShape.isInsideShape(x2, y2, z2) || sdf2 >= sdf) {
                        IntList cardinalHalfSphere = cardinalHalfSpheres.get(direction.get3DDataValue());
                        cardinalHalfSphere.add(x);
                        cardinalHalfSphere.add(y);
                        cardinalHalfSphere.add(z);
                        cardinalHalfSphere.add(encodedDistance);
                        List<IntArrayList> cardinalSphereSlices = bicardinalSphereSlices.get(direction.get3DDataValue());

                        for (Direction direction2 : Direction.values()) {
                           int x3 = x - direction2.getStepX();
                           int y3 = y - direction2.getStepY();
                           int z3 = z - direction2.getStepZ();
                           float sdf3 = brushShape.sdf(x3, y3, z3);
                           if (!brushShape.isInsideShape(x3, y3, z3) || sdf3 >= sdf) {
                              IntList cardinalSphereSlice = (IntList)cardinalSphereSlices.get(direction2.get3DDataValue());
                              cardinalSphereSlice.add(x);
                              cardinalSphereSlice.add(y);
                              cardinalSphereSlice.add(z);
                              cardinalSphereSlice.add(encodedDistance);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      this.sphere = sphere.toIntArray();
      this.cardinalHalfBalls = new int[6][];

      for (int i = 0; i < 6; i++) {
         this.cardinalHalfBalls[i] = cardinalHalfSpheres.get(i).toIntArray();
      }

      this.bicardinalBallSlices = new int[6][6][];

      for (int i = 0; i < 6; i++) {
         for (int j = 0; j < 6; j++) {
            this.bicardinalBallSlices[i][j] = bicardinalSphereSlices.get(i).get(j).toIntArray();
         }
      }
   }

   @Override
   public void update() {
      while (true) {
         int[] positions = this.outputData.poll();
         if (positions == null) {
            return;
         }

         for (int i = 0; i < positions.length; i += 4) {
            this.consumer.accept(positions[i], positions[i + 1], positions[i + 2], Float.intBitsToFloat(positions[i + 3]));
         }
      }
   }

   @Override
   public void acceptInitial(long position) {
      int x = BlockPos.getX(position);
      int y = BlockPos.getY(position);
      int z = BlockPos.getZ(position);
      this.lastPosition.set(x, y, z);
      IntArrayList outputs = new IntArrayList();

      for (int i = 0; i < this.sphere.length; i += 4) {
         int xo = this.sphere[i] + x;
         int yo = this.sphere[i + 1] + y;
         int zo = this.sphere[i + 2] + z;
         float distance = Float.intBitsToFloat(this.sphere[i + 3]);
         if (this.minDistances.min(xo, yo, zo, distance)) {
            outputs.add(xo);
            outputs.add(yo);
            outputs.add(zo);
            outputs.add(this.sphere[i + 3]);
         }
      }

      if (!outputs.isEmpty()) {
         try {
            this.outputData.put(outputs.toIntArray());
         } catch (InterruptedException var12) {
            throw new RuntimeException(var12);
         }
      }
   }

   @Override
   public void accept(long[] positions) {
      if (positions.length != 0) {
         int lastX = this.lastPosition.getX();
         int lastY = this.lastPosition.getY();
         int lastZ = this.lastPosition.getZ();
         int currX = BlockPos.getX(positions[0]);
         int currY = BlockPos.getY(positions[0]);
         int currZ = BlockPos.getZ(positions[0]);
         Position2FloatMap newMinimums = new Position2FloatMap(Float.MAX_VALUE);

         for (int posIndex = 1; posIndex < positions.length; posIndex++) {
            long position = positions[posIndex];
            int nextX = BlockPos.getX(position);
            int nextY = BlockPos.getY(position);
            int nextZ = BlockPos.getZ(position);
            int dx = currX - lastX;
            int dy = currY - lastY;
            int dz = currZ - lastZ;
            int firstIndex = this.getDirectionFromDelta(dx, dy, dz);
            dx = nextX - currX;
            dy = nextY - currY;
            dz = nextZ - currZ;
            int secondIndex = this.getDirectionFromDelta(dx, dy, dz);
            int[] offsets = this.bicardinalBallSlices[firstIndex][secondIndex];

            for (int i = 0; i < offsets.length; i += 4) {
               int xo = offsets[i] + currX;
               int yo = offsets[i + 1] + currY;
               int zo = offsets[i + 2] + currZ;
               float distance = Float.intBitsToFloat(offsets[i + 3]);
               if (this.minDistances.min(xo, yo, zo, distance)) {
                  newMinimums.put(xo, yo, zo, distance);
               }
            }

            lastX = currX;
            lastY = currY;
            lastZ = currZ;
            currX = nextX;
            currY = nextY;
            currZ = nextZ;
         }

         int dx = currX - lastX;
         int dy = currY - lastY;
         int dz = currZ - lastZ;
         int directionIndex = this.getDirectionFromDelta(dx, dy, dz);
         int[] offsets = this.cardinalHalfBalls[directionIndex];

         for (int ix = 0; ix < offsets.length; ix += 4) {
            int xo = offsets[ix] + currX;
            int yo = offsets[ix + 1] + currY;
            int zo = offsets[ix + 2] + currZ;
            float distance = Float.intBitsToFloat(offsets[ix + 3]);
            if (this.minDistances.min(xo, yo, zo, distance)) {
               newMinimums.put(xo, yo, zo, distance);
            }
         }

         IntArrayList outputs = new IntArrayList();
         newMinimums.forEachEntry((x, y, z, distancex) -> {
            outputs.add(x);
            outputs.add(y);
            outputs.add(z);
            outputs.add(Float.floatToIntBits(distancex));
         });
         if (!outputs.isEmpty()) {
            try {
               this.outputData.put(outputs.toIntArray());
            } catch (InterruptedException var26) {
               throw new RuntimeException(var26);
            }
         }

         this.lastPosition.set(currX, currY, currZ);
      }
   }

   private int getDirectionFromDelta(int dx, int dy, int dz) {
      if (dx == 0) {
         if (dy == 0) {
            if (dz == 1) {
               return 3;
            } else if (dz == -1) {
               return 2;
            } else {
               throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
            }
         } else if (dz == 0) {
            if (dy == 1) {
               return 1;
            } else if (dy == -1) {
               return 0;
            } else {
               throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
            }
         } else {
            throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
         }
      } else if (dy != 0 || dz != 0) {
         throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
      } else if (dx == 1) {
         return 5;
      } else if (dx == -1) {
         return 4;
      } else {
         throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
      }
   }
}
