package com.moulberry.axiom.operations;

import com.moulberry.axiom.DefaultBlocks;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.OkLabColourUtils;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.HistoryEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class GenerateColourFieldOperation {
   public static void generateColourField() {
      if (Selection.getSelectionBuffer() instanceof SelectionBuffer.AABB aabb) {
         generateColourFieldAABB(aabb);
      }
   }

   private static void generateColourFieldAABB(SelectionBuffer.AABB aabb) {
      int minX = aabb.min().getX();
      int minY = aabb.min().getY();
      int minZ = aabb.min().getZ();
      int maxX = aabb.max().getX();
      int maxY = aabb.max().getY();
      int maxZ = aabb.max().getZ();
      ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               blockRegion.addBlock(x, y, z, Blocks.AIR.defaultBlockState());
            }
         }
      }

      int collisions = 0;
      int missing = 0;
      Map<CustomBlock, Vec3> labValues = new HashMap<>();
      Int2ObjectMap<List<CustomBlock>> blocksByColour = new Int2ObjectOpenHashMap();
      double minL = Float.MAX_VALUE;
      double maxL = Float.MIN_VALUE;
      double minA = Float.MAX_VALUE;
      double maxA = Float.MIN_VALUE;
      double minB = Float.MAX_VALUE;
      double maxB = Float.MIN_VALUE;
      LinkedHashSet<BlockState> blockStates = new LinkedHashSet<>();

      for (CustomBlock value : ServerCustomBlocks.customBlockMap.values()) {
         blockStates.add(value.axiom$defaultCustomState().getVanillaState());
      }

      for (Block block : BuiltInRegistries.BLOCK) {
         if (!BlockColourMap.isHardIgnore(block)) {
            blockStates.add(DefaultBlocks.forBlock(block));
         }
      }

      Iterator var58 = blockStates.iterator();

      while (true) {
         CustomBlock customBlock;
         Vec3 lab;
         label144:
         while (true) {
            if (!var58.hasNext()) {
               for (Entry<CustomBlock, Vec3> entry : labValues.entrySet()) {
                  customBlock = entry.getKey();
                  lab = entry.getValue();
                  double doubleX;
                  double doubleZ;
                  if (maxX - minX > maxZ - minZ) {
                     doubleX = (lab.x - minL) / (maxL - minL) * (maxX - minX) + minX;
                     doubleZ = (lab.z - minB) / (maxB - minB) * (maxZ - minZ) + minZ;
                  } else {
                     doubleX = (lab.z - minB) / (maxB - minB) * (maxX - minX) + minX;
                     doubleZ = (lab.x - minL) / (maxL - minL) * (maxZ - minZ) + minZ;
                  }

                  double doubleY = (lab.y - minA) / (maxA - minA) * (maxY - minY) + minY;
                  int x = (int)Math.round(doubleX);
                  int y = (int)Math.round(doubleY);
                  int z = (int)Math.round(doubleZ);
                  if (blockRegion.getBlockStateOrAir(x, y, z).isAir()) {
                     blockRegion.addBlock(x, y, z, customBlock.axiom$defaultCustomState().getVanillaState());
                  } else {
                     collisions++;
                     double minDistanceSq = Double.MAX_VALUE;
                     int closestX = 0;
                     int closestY = 0;
                     int closestZ = 0;

                     for (int xo = -3; xo <= 3; xo++) {
                        for (int yo = -3; yo <= 3; yo++) {
                           for (int zo = -3; zo <= 3; zo++) {
                              double dx = doubleX - (x + xo);
                              double dy = doubleY - (y + yo);
                              double dz = doubleZ - (z + zo);
                              double distanceSq = dx * dx + dy * dy + dz * dz;
                              if (!(distanceSq > minDistanceSq)
                                 && x + xo >= minX
                                 && x + xo <= maxX
                                 && y + yo >= minY
                                 && y + yo <= maxY
                                 && z + zo >= minZ
                                 && z + zo <= maxZ
                                 && blockRegion.getBlockStateOrAir(x + xo, y + yo, z + zo).isAir()) {
                                 minDistanceSq = distanceSq;
                                 closestX = xo;
                                 closestY = yo;
                                 closestZ = zo;
                              }
                           }
                        }
                     }

                     if (closestX == 0 && closestY == 0 && closestZ == 0) {
                        missing++;
                     } else {
                        blockRegion.addBlock(x + closestX, y + closestY, z + closestZ, customBlock.axiom$defaultCustomState().getVanillaState());
                     }
                  }
               }

               if (collisions > 0) {
                  ChatUtils.warning("Encountered " + collisions + " collisions, tried to adjust positions to include every block");
               }

               if (missing > 0) {
                  ChatUtils.error("Missing " + missing + " blocks due to insufficient space");
               }

               String blockCountString = NumberFormat.getNumberInstance().format((long)blockRegion.count());
               RegionHelper.pushBlockRegionChange(blockRegion, "Colour Field (" + blockCountString + " blocks)", HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP);
               return;
            }

            BlockState blockState = (BlockState)var58.next();
            if (BlockColourMap.isFullSolidOpaque(blockState)) {
               customBlock = ServerCustomBlocks.getCustomOrVanillaStateFor(blockState).getCustomBlock();
               lab = BlockColourMap.getLab(customBlock);
               if (lab != null) {
                  int rgb = OkLabColourUtils.lab2rgb(lab.x, lab.y, lab.z);
                  List<CustomBlock> existingBlocks = (List<CustomBlock>)blocksByColour.get(rgb);
                  if (existingBlocks != null) {
                     ResourceLocation current = customBlock.axiom$getIdentifier();
                     Iterator<CustomBlock> existingBlockIterator = existingBlocks.iterator();

                     while (existingBlockIterator.hasNext()) {
                        CustomBlock existingBlock = existingBlockIterator.next();
                        ResourceLocation other = existingBlock.axiom$getIdentifier();
                        if (current.getNamespace().equals(other.getNamespace())) {
                           if (current.getPath().endsWith(other.getPath())) {
                              continue label144;
                           }

                           if (other.getPath().endsWith(current.getPath())) {
                              existingBlockIterator.remove();
                              labValues.remove(existingBlock);
                           }
                        }
                     }

                     existingBlocks.add(customBlock);
                     break;
                  }

                  List<CustomBlock> var67 = new ArrayList();
                  var67.add(customBlock);
                  blocksByColour.put(rgb, var67);
                  break;
               }
            }
         }

         labValues.put(customBlock, lab);
         minL = Math.min(minL, lab.x);
         maxL = Math.max(maxL, lab.x);
         minA = Math.min(minA, lab.y);
         maxA = Math.max(maxA, lab.y);
         minB = Math.min(minB, lab.z);
         maxB = Math.max(maxB, lab.z);
      }
   }
}
