package com.moulberry.axiom.utils;

import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.collections.SimpleBlockWeightTracker;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class GaussianBlurTable {
   private final float[] table;

   public GaussianBlurTable(float stddev, float threshold) {
      if (stddev <= 0.0F) {
         this.table = new float[]{1.0F};
      } else {
         stddev = Math.min(10.0F, stddev);
         threshold = Math.max(0.0F, Math.min(1.0F, threshold));
         float onePercentX = (float)Math.sqrt(-(2.0F * stddev * stddev) * Math.log(threshold * (stddev * Math.sqrt(Math.PI * 2))));
         int radius = (int)Math.floor(onePercentX);
         radius = Math.min(16, radius);
         if (radius <= 0) {
            this.table = new float[]{1.0F};
         } else {
            this.table = new float[radius * 2 + 1];

            for (int i = -radius; i <= radius; i++) {
               this.table[i + radius] = (float)(Math.exp(-i * i / (2.0F * stddev * stddev)) / (stddev * Math.sqrt(Math.PI * 2)));
            }

            float sum = 0.0F;

            for (float f : this.table) {
               sum += f;
            }

            for (int i = 0; i < this.table.length; i++) {
               this.table[i] = this.table[i] / sum;
            }
         }
      }
   }

   public float[] getTable() {
      return this.table;
   }

   public boolean isSingleValued() {
      return this.table.length == 1;
   }

   public void applyBlur(PositionSet input, Position2FloatMap output) {
      if (this.isSingleValued()) {
         input.forEach((x, y, z) -> output.add(x, y, z, 1.0F));
      } else {
         int radius = this.table.length / 2;
         if (radius == 0) {
            throw new FaultyImplementationError();
         } else {
            Position2FloatMap smoothedX = new Position2FloatMap();
            input.forEach((x, y, z) -> {
               for (int i = -radius; i <= radius; i++) {
                  smoothedX.add(x + i, y, z, this.table[i + radius]);
               }
            });
            Position2FloatMap smoothedZ = new Position2FloatMap();
            smoothedX.forEachEntry((x, y, z, v) -> {
               for (int i = -radius; i <= radius; i++) {
                  smoothedZ.add(x, y, z + i, v * this.table[i + radius]);
               }
            });
            smoothedZ.forEachEntry((x, y, z, v) -> {
               for (int i = -radius; i <= radius; i++) {
                  output.add(x, y + i, z, v * this.table[i + radius]);
               }
            });
         }
      }
   }

   public void applyBlur(Position2FloatMap input, Position2FloatMap output) {
      if (this.isSingleValued()) {
         input.forEachEntry(output::add);
      } else {
         int radius = this.table.length / 2;
         if (radius == 0) {
            throw new FaultyImplementationError();
         } else {
            Position2FloatMap smoothedX = new Position2FloatMap();
            input.forEachEntry((x, y, z, v) -> {
               for (int i = -radius; i <= radius; i++) {
                  smoothedX.add(x + i, y, z, v * this.table[i + radius]);
               }
            });
            Position2FloatMap smoothedZ = new Position2FloatMap();
            smoothedX.forEachEntry((x, y, z, v) -> {
               for (int i = -radius; i <= radius; i++) {
                  smoothedZ.add(x, y, z + i, v * this.table[i + radius]);
               }
            });
            smoothedZ.forEachEntry((x, y, z, v) -> {
               for (int i = -radius; i <= radius; i++) {
                  output.add(x, y + i, z, v * this.table[i + radius]);
               }
            });
         }
      }
   }

   public void applyBlurCheckThreshold(Position2FloatMap input, Position2FloatMap output, float threshold, TriIntConsumer consumer) {
      if (this.isSingleValued()) {
         input.forEachEntry((x, y, z, v) -> {
            float currentValue = output.get(x, y, z);
            if (!(currentValue >= threshold)) {
               float newValue = output.add(x, y, z, v);
               if (newValue >= threshold) {
                  consumer.accept(x, y, z);
               }
            }
         });
      } else {
         int radius = this.table.length / 2;
         if (radius == 0) {
            throw new FaultyImplementationError();
         } else {
            Position2FloatMap smoothedX = new Position2FloatMap();
            input.forEachEntry((x, y, z, v) -> {
               for (int i = -radius; i <= radius; i++) {
                  smoothedX.add(x + i, y, z, v * this.table[i + radius]);
               }
            });
            Position2FloatMap smoothedZ = new Position2FloatMap();
            smoothedX.forEachEntry((x, y, z, v) -> {
               for (int i = -radius; i <= radius; i++) {
                  smoothedZ.add(x, y, z + i, v * this.table[i + radius]);
               }
            });
            smoothedZ.forEachEntry((x, y, z, v) -> {
               for (int i = -radius; i <= radius; i++) {
                  float s = v * this.table[i + radius];
                  float newValue = output.add(x, y + i, z, s);
                  if (newValue >= threshold && newValue - s < threshold) {
                     consumer.accept(x, y + i, z);
                  }
               }
            });
         }
      }
   }

   public void applyBlurCheckThreshold(
      Position2ObjectMap<SimpleBlockWeightTracker> input,
      Position2ObjectMap<SimpleBlockWeightTracker> output,
      float threshold,
      GaussianBlurTable.BlockBlurConsumer consumer
   ) {
      MutableBlockPos mutable = new MutableBlockPos();
      if (this.isSingleValued()) {
         input.forEachEntry((x, y, z, v) -> {
            if (v.block() != null) {
               SimpleBlockWeightTracker currentValue = output.getOrCreate(x, y, z);
               float oldWeight = currentValue.totalWeight();
               BlockState oldBlock = currentValue.block();
               currentValue.add(v);
               if (currentValue.totalWeight() >= threshold && (oldWeight < threshold || oldBlock != currentValue.block())) {
                  consumer.accept(mutable.set(x, y, z), currentValue.block());
               }
            }
         });
      } else {
         int radius = this.table.length / 2;
         if (radius == 0) {
            throw new FaultyImplementationError();
         } else {
            Position2ObjectMap<SimpleBlockWeightTracker> smoothedX = SimpleBlockWeightTracker.createMap();
            input.forEachEntry((x, y, z, v) -> {
               if (v.block() != null) {
                  for (int i = -radius; i <= radius; i++) {
                     smoothedX.getOrCreate(x + i, y, z).add(v, this.table[i + radius]);
                  }
               }
            });
            Position2ObjectMap<SimpleBlockWeightTracker> smoothedZ = SimpleBlockWeightTracker.createMap();
            smoothedX.forEachEntry((x, y, z, v) -> {
               if (v.block() != null) {
                  for (int i = -radius; i <= radius; i++) {
                     smoothedZ.getOrCreate(x, y, z + i).add(v, this.table[i + radius]);
                  }
               }
            });
            smoothedZ.forEachEntry((x, y, z, v) -> {
               if (v.block() != null) {
                  for (int i = -radius; i <= radius; i++) {
                     SimpleBlockWeightTracker currentValue = output.getOrCreate(x, y + i, z);
                     float oldWeight = currentValue.totalWeight();
                     BlockState oldBlock = currentValue.block();
                     currentValue.add(v, this.table[i + radius]);
                     if (currentValue.totalWeight() >= threshold && (oldWeight < threshold || oldBlock != currentValue.block())) {
                        consumer.accept(mutable.set(x, y + i, z), currentValue.block());
                     }
                  }
               }
            });
         }
      }
   }

   @Override
   public String toString() {
      return "GaussianBlurTable" + Arrays.toString(this.table);
   }

   public interface BlockBlurConsumer {
      void accept(BlockPos var1, BlockState var2);
   }
}
