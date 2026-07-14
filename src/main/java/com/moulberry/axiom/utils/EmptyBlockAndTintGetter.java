package com.moulberry.axiom.utils;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum EmptyBlockAndTintGetter implements BlockAndTintGetter {
   INSTANCE;

   @Nullable
   public BlockEntity getBlockEntity(BlockPos blockPos) {
      return null;
   }

   @NotNull
   public BlockState getBlockState(BlockPos blockPos) {
      return Blocks.AIR.defaultBlockState();
   }

   @NotNull
   public FluidState getFluidState(BlockPos blockPos) {
      return Fluids.EMPTY.defaultFluidState();
   }

   public int getMinBuildHeight() {
      return 0;
   }

   public int getHeight() {
      return 0;
   }

   public float getShade(Direction direction, boolean bl) {
      switch (direction) {
         case DOWN:
         case UP:
            return 0.9F;
         case NORTH:
         case SOUTH:
            return 0.8F;
         case WEST:
         case EAST:
            return 0.6F;
         default:
            return 1.0F;
      }
   }

   public LevelLightEngine getLightEngine() {
      throw new UnsupportedOperationException();
   }

   public int getBrightness(LightLayer type, BlockPos pos) {
      return 12;
   }

   public int getRawBrightness(BlockPos pos, int ambientDarkness) {
      return 12;
   }

   public boolean canSeeSky(BlockPos pos) {
      return false;
   }

   public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
      if (colorResolver == BiomeColors.WATER_COLOR_RESOLVER) {
         return -12618012;
      } else if (colorResolver == BiomeColors.GRASS_COLOR_RESOLVER) {
         return GrassColor.get(0.8F, 0.4F);
      } else {
         return colorResolver == BiomeColors.FOLIAGE_COLOR_RESOLVER ? FoliageColor.get(0.8F, 0.4F) : -1;
      }
   }
}
