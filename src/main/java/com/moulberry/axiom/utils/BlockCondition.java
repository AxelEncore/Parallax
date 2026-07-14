package com.moulberry.axiom.utils;

import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.editor.BlockList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface BlockCondition {
   boolean matches(BlockState var1);

   @Nullable
   static BlockCondition fromMinecraftOrCustomTag(BlockList.MinecraftOrCustomTagSet tagSet) {
      if (tagSet == null) {
         return null;
      } else if (tagSet.minecraft() != null) {
         return (BlockCondition)(tagSet.customSet() != null
            ? new BlockCondition.AnyCondition(
               List.of(new BlockCondition.MatchesTag(tagSet.minecraft()), new BlockCondition.MatchesCustomTag(tagSet.customSet()))
            )
            : new BlockCondition.MatchesTag(tagSet.minecraft()));
      } else {
         return tagSet.customSet() != null ? new BlockCondition.MatchesCustomTag(tagSet.customSet()) : null;
      }
   }

   @Nullable
   static BlockCondition fromCustomBlock(CustomBlock customBlock) {
      return fromCustomBlocks(List.of(customBlock));
   }

   @Nullable
   static BlockCondition fromCustomBlocks(Collection<CustomBlock> customBlocks) {
      Set<Block> blocks = new HashSet<>();
      Set<BlockState> blockStates = new HashSet<>();

      for (CustomBlock fromBlock : customBlocks) {
         if (fromBlock instanceof Block block) {
            blocks.add(block);
         } else {
            for (CustomBlockState possibleCustomState : fromBlock.axiom$getPossibleCustomStates()) {
               blockStates.add(possibleCustomState.getVanillaState());
            }
         }
      }

      if (!blockStates.isEmpty()) {
         for (Block block : blocks) {
            blockStates.addAll(block.getStateDefinition().getPossibleStates());
         }

         return (BlockCondition)(blockStates.size() == 1
            ? new BlockCondition.SpecificState(blockStates.iterator().next())
            : new BlockCondition.StateSet(blockStates));
      } else if (blocks.isEmpty()) {
         return null;
      } else {
         return (BlockCondition)(blocks.size() == 1 ? new BlockCondition.AnyState(blocks.iterator().next()) : new BlockCondition.BlockSet(blocks));
      }
   }

   public record AnyCondition(List<BlockCondition> conditions) implements BlockCondition {
      @Override
      public boolean matches(BlockState blockState) {
         for (BlockCondition condition : this.conditions) {
            if (condition.matches(blockState)) {
               return true;
            }
         }

         return false;
      }
   }

   public record AnyState(Block block) implements BlockCondition {
      @Override
      public boolean matches(BlockState blockState) {
         return blockState.getBlock() == this.block;
      }
   }

   public record BlockSet(Set<Block> set) implements BlockCondition {
      @Override
      public boolean matches(BlockState blockState) {
         return this.set.contains(blockState.getBlock());
      }
   }

   public record MatchesCustomTag(Set<ResourceLocation> customTag) implements BlockCondition {
      @Override
      public boolean matches(BlockState blockState) {
         return this.customTag.contains(blockState.getBlock().builtInRegistryHolder().key().location());
      }
   }

   public record MatchesTag(HolderSet<Block> tag) implements BlockCondition {
      @Override
      public boolean matches(BlockState blockState) {
         return this.tag.contains(blockState.getBlockHolder());
      }
   }

   public record SpecificState(BlockState blockState) implements BlockCondition {
      @Override
      public boolean matches(BlockState blockState) {
         return blockState == this.blockState;
      }
   }

   public record StateSet(Set<BlockState> set) implements BlockCondition {
      @Override
      public boolean matches(BlockState blockState) {
         return this.set.contains(blockState);
      }
   }
}
