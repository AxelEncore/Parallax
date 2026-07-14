package com.moulberry.axiom.operations;

import com.moulberry.axiom.BlueNoiseArray;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.BlockWithFloat;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.HistoryEntry;
import java.text.NumberFormat;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class AutoshadeOperation {
   public static void autoshade(
      boolean sunShade,
      boolean ambientShade,
      AutoshadeShading shading,
      float globalIllumination,
      float dither,
      List<BlockWithFloat> customPalette,
      int paletteFlags
   ) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      globalIllumination = Math.max(0.0F, Math.min(1.0F, globalIllumination));
      dither = Math.max(0.0F, Math.min(1.0F, dither));
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         autoshadeAABB(aabb, sunShade, ambientShade, shading, globalIllumination, dither, customPalette, paletteFlags);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         autoshadeSet(set, sunShade, ambientShade, shading, globalIllumination, dither, customPalette, paletteFlags);
      }
   }

   private static void autoshadeSet(
      SelectionBuffer.Set set,
      boolean sunShade,
      boolean ambientShade,
      AutoshadeShading shading,
      float globalIllumination,
      float dither,
      List<BlockWithFloat> customPalette,
      int paletteFlags
   ) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         if (sunShade || ambientShade) {
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
            float customPaletteTotal = 0.0F;
            if (customPalette != null) {
               for (BlockWithFloat blockWithFloat : customPalette) {
                  customPaletteTotal += blockWithFloat.percentage()[0];
               }
            }

            float customPaletteTotalF = customPaletteTotal;
            set.selectionRegion
               .forEach(
                  (x, y, z) -> {
                     BlockState shadedBlock = getShadedBlock(
                        world,
                        x,
                        y,
                        z,
                        mutableBlockPos,
                        sunShade,
                        ambientShade,
                        shading,
                        globalIllumination,
                        dither,
                        customPalette,
                        customPaletteTotalF,
                        paletteFlags
                     );
                     if (shadedBlock != null) {
                        blockRegion.addBlock(x, y, z, shadedBlock);
                     }
                  }
               );
            String blockCountString = NumberFormat.getNumberInstance().format((long)blockRegion.count());
            RegionHelper.pushBlockRegionChange(blockRegion, "Autoshade (" + blockCountString + " blocks)", HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP);
         }
      }
   }

   private static void autoshadeAABB(
      SelectionBuffer.AABB aabb,
      boolean sunShade,
      boolean ambientShade,
      AutoshadeShading shading,
      float globalIllumination,
      float dither,
      List<BlockWithFloat> customPalette,
      int paletteFlags
   ) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         if (sunShade || ambientShade) {
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
            float customPaletteTotal = 0.0F;
            if (customPalette != null) {
               for (BlockWithFloat blockWithFloat : customPalette) {
                  customPaletteTotal += blockWithFloat.percentage()[0];
               }
            }

            int minX = aabb.min().getX();
            int minY = aabb.min().getY();
            int minZ = aabb.min().getZ();
            int maxX = aabb.max().getX();
            int maxY = aabb.max().getY();
            int maxZ = aabb.max().getZ();

            for (int x = minX; x <= maxX; x++) {
               for (int y = minY; y <= maxY; y++) {
                  for (int z = minZ; z <= maxZ; z++) {
                     BlockState shadedBlock = getShadedBlock(
                        world,
                        x,
                        y,
                        z,
                        mutableBlockPos,
                        sunShade,
                        ambientShade,
                        shading,
                        globalIllumination,
                        dither,
                        customPalette,
                        customPaletteTotal,
                        paletteFlags
                     );
                     if (shadedBlock != null) {
                        blockRegion.addBlock(x, y, z, shadedBlock);
                     }
                  }
               }
            }

            String blockCountString = NumberFormat.getNumberInstance().format((long)blockRegion.count());
            RegionHelper.pushBlockRegionChange(blockRegion, "Autoshade (" + blockCountString + " blocks)", HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP);
         }
      }
   }

   private static BlockState getShadedBlock(
      Level world,
      int x,
      int y,
      int z,
      MutableBlockPos mutableBlockPos,
      boolean sunShade,
      boolean ambientShade,
      AutoshadeShading shading,
      float globalIllumination,
      float dither,
      List<BlockWithFloat> customPalette,
      float customPaletteTotal,
      int paletteFlags
   ) {
      BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
      if (!block.blocksMotion()) {
         return null;
      } else {
         Vec3 lab = BlockColourMap.getLab(block.getBlock());
         if (lab == null) {
            return block;
         } else if (world.getBlockState(mutableBlockPos.set(x + 1, y, z)).blocksMotion()
            && world.getBlockState(mutableBlockPos.set(x - 1, y, z)).blocksMotion()
            && world.getBlockState(mutableBlockPos.set(x, y + 1, z)).blocksMotion()
            && world.getBlockState(mutableBlockPos.set(x, y - 1, z)).blocksMotion()
            && world.getBlockState(mutableBlockPos.set(x, y, z + 1)).blocksMotion()
            && world.getBlockState(mutableBlockPos.set(x, y, z - 1)).blocksMotion()) {
            return block;
         } else {
            float offsetX = 0.0F;
            float offsetY = 0.0F;
            float offsetZ = 0.0F;
            float filled = 0.0F;
            float total = 0.0F;

            for (int xo = -8; xo <= 8; xo++) {
               for (int yo = -8; yo <= 8; yo++) {
                  for (int zo = -8; zo <= 8; zo++) {
                     int distSq = xo * xo + yo * yo + zo * zo;
                     if (distSq <= 72) {
                        total++;
                        BlockState neighbor = world.getBlockState(mutableBlockPos.set(x + xo, y + yo, z + zo));
                        if (neighbor.blocksMotion()) {
                           float factor = 1.0F / (float)Math.max(1.0, Math.sqrt(distSq));
                           filled++;
                           offsetX -= xo * factor;
                           offsetY -= yo * factor;
                           offsetZ -= zo * factor;
                        }
                     }
                  }
               }
            }

            float shade = 1.0F;
            if (ambientShade) {
               float ambientFactor = Math.min(1.0F, Math.max(0.0F, 2.0F * (1.0F - filled / total)));
               shade *= Math.max(globalIllumination, ambientFactor * ambientFactor);
            }

            if (sunShade && (offsetX != 0.0F || offsetY != 0.0F || offsetZ != 0.0F)) {
               float invNormalLength = 1.0F / (float)Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
               float normalX = offsetX * invNormalLength;
               float normalY = offsetY * invNormalLength;
               float normalZ = offsetZ * invNormalLength;
               float sunDot = 0.0F;

               for (Vec3 vector : shading.vectors) {
                  float newSunDot = (float)vector.x * normalX + (float)vector.y * normalY + (float)vector.z * normalZ;
                  sunDot = Math.max(sunDot, newSunDot * 0.5F + 0.5F);
               }

               for (AutoshadeShading.PositionWithIntensity position : shading.positions) {
                  float deltaX = position.x() - (x + 0.5F);
                  float deltaY = position.y() - (y + 0.5F);
                  float deltaZ = position.z() - (z + 0.5F);
                  float deltaDist = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                  float newSunDot = deltaX / deltaDist * normalX + deltaY / deltaDist * normalY + deltaZ / deltaDist * normalZ;
                  sunDot = Math.max(sunDot, (newSunDot * 0.5F + 0.5F) * (float)Math.sqrt(position.intensity()));
               }

               shade *= Math.max(globalIllumination, sunDot);
            }

            if (shade < 0.0F) {
               shade = 0.0F;
            }

            if (shade > 1.0F) {
               shade = 1.0F;
            }

            if (dither > 0.01) {
               float noise = BlueNoiseArray.NOISE[(x & 31) + (y & 31) * 32 + (z & 31) * 32 * 32];
               shade += noise * dither - dither / 2.0F;
               if (shade < 0.0F) {
                  shade = 0.0F;
               }

               if (shade > 1.0F) {
                  shade = 1.0F;
               }
            }

            if (customPalette == null) {
               double lightness = lab.x * shade;
               return BlockColourMap.getNearestLab(lightness, lab.y, lab.z, paletteFlags);
            } else {
               float shadeTimesTotal = shade * customPaletteTotal;
               BlockState shadedBlock = null;

               for (BlockWithFloat blockWithFloat : customPalette) {
                  shadeTimesTotal -= blockWithFloat.percentage()[0];
                  if (shadeTimesTotal <= 0.0F) {
                     shadedBlock = blockWithFloat.blockState().getVanillaState();
                     break;
                  }
               }

               if (shadedBlock == null) {
                  shadedBlock = customPalette.get(customPalette.size() - 1).blockState().getVanillaState();
               }

               return shadedBlock;
            }
         }
      }
   }
}
