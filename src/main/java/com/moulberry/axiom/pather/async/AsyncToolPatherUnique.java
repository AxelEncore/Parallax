package com.moulberry.axiom.pather.async;

import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.utils.Box;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;

public class AsyncToolPatherUnique implements AsyncToolPather {
   private final int[] sphere;
   private final int[][] cardinal;
   private final PositionSet positions;
   private final TriIntConsumer consumer;
   private final MutableBlockPos lastPosition = new MutableBlockPos();
   protected final ArrayBlockingQueue<long[]> outputPositions = new ArrayBlockingQueue<>(128);

   public AsyncToolPatherUnique(BrushShape brushShape, TriIntConsumer consumer) {
      this.positions = new PositionSet();
      this.consumer = consumer;
      IntList sphere = new IntArrayList();
      List<IntList> cardinal = List.of(new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList());
      Box bounding = brushShape.boundingBox();
      int minX = bounding.pos1().getX() - 1;
      int minY = bounding.pos1().getY() - 1;
      int minZ = bounding.pos1().getZ() - 1;
      int maxX = bounding.pos2().getX() + 1;
      int maxY = bounding.pos2().getY() + 1;
      int maxZ = bounding.pos2().getZ() + 1;

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               if (brushShape.isInsideShape(x, y, z)) {
                  sphere.add(x);
                  sphere.add(y);
                  sphere.add(z);
               } else {
                  for (Direction direction : Direction.values()) {
                     int x2 = x - direction.getStepX();
                     int y2 = y - direction.getStepY();
                     int z2 = z - direction.getStepZ();
                     if (brushShape.isInsideShape(x2, y2, z2)) {
                        IntList list = cardinal.get(direction.get3DDataValue());
                        list.add(x2);
                        list.add(y2);
                        list.add(z2);
                     }
                  }
               }
            }
         }
      }

      this.sphere = sphere.toIntArray();
      this.cardinal = new int[6][];

      for (int i = 0; i < 6; i++) {
         this.cardinal[i] = cardinal.get(i).toIntArray();
      }
   }

   @Override
   public void update() {
      while (true) {
         long[] positions = this.outputPositions.poll();
         if (positions == null) {
            return;
         }

         for (long position : positions) {
            this.consumer.accept(BlockPos.getX(position), BlockPos.getY(position), BlockPos.getZ(position));
         }
      }
   }

   @Override
   public void acceptInitial(long position) {
      int x = BlockPos.getX(position);
      int y = BlockPos.getY(position);
      int z = BlockPos.getZ(position);
      this.lastPosition.set(x, y, z);
      LongArrayList outputs = new LongArrayList();

      for (int i = 0; i < this.sphere.length; i += 3) {
         int xo = this.sphere[i] + x;
         int yo = this.sphere[i + 1] + y;
         int zo = this.sphere[i + 2] + z;
         if (this.positions.add(xo, yo, zo)) {
            outputs.add(BlockPos.asLong(xo, yo, zo));
         }
      }

      if (!outputs.isEmpty()) {
         try {
            this.outputPositions.put(outputs.toLongArray());
         } catch (InterruptedException var11) {
            throw new RuntimeException(var11);
         }
      }
   }

   @Override
   public void accept(long[] positions) {
      int lastX = this.lastPosition.getX();
      int lastY = this.lastPosition.getY();
      int lastZ = this.lastPosition.getZ();
      LongArrayList outputs = new LongArrayList();

      for (long position : positions) {
         int x = BlockPos.getX(position);
         int y = BlockPos.getY(position);
         int z = BlockPos.getZ(position);
         int dx = x - lastX;
         int dy = y - lastY;
         int dz = z - lastZ;
         int[] offsets;
         if (dx == 0) {
            if (dy == 0) {
               if (dz == 1) {
                  offsets = this.cardinal[3];
               } else {
                  if (dz != -1) {
                     throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                  }

                  offsets = this.cardinal[2];
               }
            } else {
               if (dz != 0) {
                  throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
               }

               if (dy == 1) {
                  offsets = this.cardinal[1];
               } else {
                  if (dy != -1) {
                     throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                  }

                  offsets = this.cardinal[0];
               }
            }
         } else {
            if (dy != 0 || dz != 0) {
               throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
            }

            if (dx == 1) {
               offsets = this.cardinal[5];
            } else {
               if (dx != -1) {
                  throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
               }

               offsets = this.cardinal[4];
            }
         }

         for (int j = 0; j < offsets.length; j += 3) {
            int offsetX = offsets[j] + x;
            int offsetY = offsets[j + 1] + y;
            int offsetZ = offsets[j + 2] + z;
            if (this.positions.add(offsetX, offsetY, offsetZ)) {
               outputs.add(BlockPos.asLong(offsetX, offsetY, offsetZ));
            }
         }

         lastX = x;
         lastY = y;
         lastZ = z;
      }

      if (!outputs.isEmpty()) {
         try {
            this.outputPositions.put(outputs.toLongArray());
         } catch (InterruptedException var22) {
            throw new RuntimeException(var22);
         }
      }

      this.lastPosition.set(lastX, lastY, lastZ);
   }
}
