package com.moulberry.axiom.collections;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SimpleBlockWeightTracker {
   private BlockState currentBlock = null;
   private float currentWeight = 0.0F;
   private float totalWeight = 0.0F;

   public static Position2ObjectMap<SimpleBlockWeightTracker> createMap() {
      return new Position2ObjectMap<>(k -> {
         SimpleBlockWeightTracker[] array = new SimpleBlockWeightTracker[4096];

         for (int i = 0; i < 4096; i++) {
            array[i] = new SimpleBlockWeightTracker();
         }

         return array;
      });
   }

   public void add(SimpleBlockWeightTracker other) {
      this.totalWeight = this.totalWeight + other.totalWeight;
      if (this.currentBlock == other.currentBlock) {
         this.currentWeight = this.currentWeight + other.currentWeight;
      } else {
         this.currentWeight = this.currentWeight - other.currentWeight;
         if (this.currentWeight <= 0.0F) {
            this.currentBlock = other.currentBlock;
            this.currentWeight = -this.currentWeight;
         }
      }
   }

   public void add(SimpleBlockWeightTracker other, float multiplier) {
      this.totalWeight = this.totalWeight + other.totalWeight * multiplier;
      if (this.currentBlock == other.currentBlock) {
         this.currentWeight = this.currentWeight + other.currentWeight * multiplier;
      } else {
         this.currentWeight = this.currentWeight - other.currentWeight * multiplier;
         if (this.currentWeight <= 0.0F) {
            this.currentBlock = other.currentBlock;
            this.currentWeight = -this.currentWeight;
         }
      }
   }

   public void addWeight(BlockState block, float weight) {
      this.totalWeight += weight;
      if (this.currentBlock == block) {
         this.currentWeight += weight;
      } else {
         this.currentWeight -= weight;
         if (this.currentWeight <= 0.0F) {
            this.currentBlock = block;
            this.currentWeight = -this.currentWeight;
         }
      }
   }

   public void addWeight(BlockState block, float weight, float blockWeight) {
      this.totalWeight += weight;
      if (this.currentBlock == block) {
         this.currentWeight += blockWeight;
      } else {
         this.currentWeight -= blockWeight;
         if (this.currentWeight <= 0.0F) {
            this.currentBlock = block;
            this.currentWeight = -this.currentWeight;
         }
      }
   }

   public void weight(float weight) {
      this.totalWeight = weight;
   }

   @Nullable
   public BlockState block() {
      return this.currentBlock;
   }

   public float totalWeight() {
      return this.totalWeight;
   }

   public float currentWeight() {
      return this.currentWeight;
   }

   @Override
   public String toString() {
      return "SimpleBlockWeightTracker[currentBlock=" + this.currentBlock + ", currentWeight=" + this.currentWeight + ", totalWeight=" + this.totalWeight + "]";
   }
}
