package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.collections.ChunkedPredicateDistanceField;
import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.utils.BlockCondition;
import com.moulberry.axiom.utils.IntegerDistanceFieldConstants;
import java.util.BitSet;
import java.util.List;

public class BlockNearConditionMaskElement implements MaskElement, GenericBlockConditionMaskElement {
   private final BlockCondition blockCondition;
   private final List<BlockConditionWidget.BlockConditionState> blockConditionStates;
   public final int radius;
   private final int distanceThreshold;
   private final int maxWhitelist;
   private final BitSet whitelist;

   public BlockNearConditionMaskElement(BlockCondition blockCondition, List<BlockConditionWidget.BlockConditionState> blockConditionStates, int radius) {
      this.blockCondition = blockCondition;
      this.blockConditionStates = blockConditionStates;
      this.radius = radius;
      int clampedRadius = Math.max(1, Math.min(16, this.radius));
      this.distanceThreshold = IntegerDistanceFieldConstants.DISTANCE_THRESHOLDS[clampedRadius];
      this.maxWhitelist = IntegerDistanceFieldConstants.MAX_WHITELIST[clampedRadius];
      this.whitelist = IntegerDistanceFieldConstants.WHITELIST[clampedRadius];
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      if (this.radius <= 1) {
         if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, -1, -1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, -1, 0))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, -1, 1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, 0, -1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, 0, 0))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, 0, 1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, 1, -1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, 1, 0))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, -1, 1, 1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, -1, -1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, -1, 0))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, -1, 1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, 0, -1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, 0, 0))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, 0, 1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, 1, -1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, 1, 0))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 0, 1, 1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 1, -1, -1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 1, -1, 0))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 1, -1, 1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 1, 0, -1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 1, 0, 0))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 1, 0, 1))) {
            return true;
         } else if (this.blockCondition.matches(context.getBlockState(x, y, z, 1, 1, -1))) {
            return true;
         } else {
            return this.blockCondition.matches(context.getBlockState(x, y, z, 1, 1, 0))
               ? true
               : this.blockCondition.matches(context.getBlockState(x, y, z, 1, 1, 1));
         }
      } else {
         ChunkedPredicateDistanceField distanceField = context.getPredicateDistanceField(this.blockCondition);
         int distance = distanceField.getDistance(x, y, z);
         if (distance <= this.distanceThreshold) {
            return true;
         } else {
            return distance > this.maxWhitelist ? false : this.whitelist.get(distance - this.distanceThreshold);
         }
      }
   }

   @Override
   public BlockCondition getBlockCondition() {
      return this.blockCondition;
   }

   @Override
   public List<BlockConditionWidget.BlockConditionState> getConditionStates() {
      return this.blockConditionStates;
   }

   @Override
   public String cmdStringName() {
      return this.radius <= 1 ? "near" : "near(" + this.radius + ")";
   }

   @Override
   public boolean canCombine(GenericBlockConditionMaskElement other) {
      return other instanceof BlockNearConditionMaskElement otherNear && otherNear.radius == this.radius;
   }
}
