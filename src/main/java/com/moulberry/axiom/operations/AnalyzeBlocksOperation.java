package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.utils.IntWrapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AnalyzeBlocksOperation {
   public static AnalyzeBlocksOperation.Information analyze(boolean filterBlockEntities) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         return analyze(aabb, filterBlockEntities);
      } else {
         return selectionBuffer instanceof SelectionBuffer.Set set ? analyze(set, filterBlockEntities) : null;
      }
   }

   private static AnalyzeBlocksOperation.Information analyze(SelectionBuffer.AABB aabb, boolean filterBlockEntities) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world == null) {
         return null;
      } else {
         int minX = aabb.min().getX();
         int minY = aabb.min().getY();
         int minZ = aabb.min().getZ();
         int maxX = aabb.max().getX();
         int maxY = aabb.max().getY();
         int maxZ = aabb.max().getZ();
         minY = Math.max(world.getMinBuildHeight(), minY);
         maxY = Math.min(world.getMaxBuildHeight() - 1, maxY);
         Map<CustomBlock, IntWrapper> map = new HashMap<>();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  BlockState blockState = world.getBlockState(mutableBlockPos.set(x, y, z));
                  if (blockState.getBlock() != Blocks.VOID_AIR && (!filterBlockEntities || blockState.hasBlockEntity())) {
                     CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
                     if (customBlockState != null) {
                        map.computeIfAbsent(customBlockState.getCustomBlock(), k -> new IntWrapper()).value++;
                     } else {
                        map.computeIfAbsent((CustomBlock)blockState.getBlock(), k -> new IntWrapper()).value++;
                     }
                  }
               }
            }
         }

         return new AnalyzeBlocksOperation.Information(map);
      }
   }

   private static AnalyzeBlocksOperation.Information analyze(SelectionBuffer.Set set, boolean filterBlockEntities) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world == null) {
         return null;
      } else {
         Map<CustomBlock, IntWrapper> map = new HashMap<>();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         set.selectionRegion.forEach((x, y, z) -> {
            BlockState blockState = world.getBlockState(mutableBlockPos.set(x, y, z));
            if (blockState.getBlock() != Blocks.VOID_AIR) {
               if (!filterBlockEntities || blockState.hasBlockEntity()) {
                  CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
                  if (customBlockState != null) {
                     map.computeIfAbsent(customBlockState.getCustomBlock(), k -> new IntWrapper()).value++;
                  } else {
                     map.computeIfAbsent((CustomBlock)blockState.getBlock(), k -> new IntWrapper()).value++;
                  }
               }
            }
         });
         return new AnalyzeBlocksOperation.Information(map);
      }
   }

   public record Information(List<AnalyzeBlocksOperation.InformationEntry> list, int total) {
      public Information(Map<CustomBlock, IntWrapper> map) {
         this(sort(map));
      }

      public Information(List<AnalyzeBlocksOperation.InformationEntry> list) {
         this(list, count(list));
      }

      private static int count(List<AnalyzeBlocksOperation.InformationEntry> list) {
         int count = 0;

         for (AnalyzeBlocksOperation.InformationEntry informationEntry : list) {
            count += informationEntry.count();
         }

         return count;
      }

      private static List<AnalyzeBlocksOperation.InformationEntry> sort(Map<CustomBlock, IntWrapper> map) {
         List<AnalyzeBlocksOperation.InformationEntry> list = new ArrayList<>();

         for (Entry<CustomBlock, IntWrapper> entry : map.entrySet()) {
            list.add(new AnalyzeBlocksOperation.InformationEntry(entry.getKey(), entry.getValue().value));
         }

         list.sort(Comparator.comparingInt(k -> -k.count));
         return list;
      }
   }

   public record InformationEntry(CustomBlock block, int count) {
   }
}
