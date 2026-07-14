package com.moulberry.axiom.block_maps;

import com.moulberry.axiom.DefaultBlocks;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockColourMap {
   public static int FLAG_SOLID = 1;
   public static int FLAG_OPAQUE = 2;
   public static int FLAG_FULL_CUBE = 4;
   public static int FLAG_SAME_TEXTURE = 8;
   public static int FLAG_NO_ORES = 16;
   public static int FLAG_NO_GLAZED_TERRACOTTA = 32;
   public static int FLAG_NO_TILE_ENTITIES = 64;
   private static final SpaceLookup<BlockState> tree = new SpaceLookup<>();
   private static final Map<CustomBlock, Vec3> colourMap = new HashMap<>();
   private static final Set<CustomBlock> sameTextureSet = new HashSet<>();
   private static boolean coloursDirty = true;
   private static int lastTreeFlags = -1;
   private static final Direction[] DIRECTIONS = Direction.values();

   private static void recalculateColours() {
      if (coloursDirty) {
         coloursDirty = false;
         colourMap.clear();
         sameTextureSet.clear();
         LinkedHashSet<BlockState> blockStates = new LinkedHashSet<>();

         for (CustomBlock block : ServerCustomBlocks.customBlockMap.values()) {
            BlockState defaultState = block.axiom$defaultCustomState().getVanillaState();
            blockStates.add(defaultState);
         }

         for (Block block : BuiltInRegistries.BLOCK) {
            BlockState defaultState = DefaultBlocks.forBlock(block);
            blockStates.add(defaultState);
         }

         for (BlockColourComputation.BlockColourResult result : BlockColourComputation.calculateColours(blockStates)) {
            CustomBlock customBlock = ServerCustomBlocks.getCustomOrVanillaStateFor(result.blockState()).getCustomBlock();
            Vec3 lab = new Vec3(result.l(), result.a(), result.b());
            colourMap.put(customBlock, lab);
            if (result.numTextures() <= 1) {
               sameTextureSet.add(customBlock);
            }
         }
      }
   }

   private static void recalculateTree(int flags) {
      recalculateColours();
      if (lastTreeFlags != flags) {
         lastTreeFlags = flags;
         tree.clear();

         for (Entry<CustomBlock, Vec3> entry : colourMap.entrySet()) {
            CustomBlock block = entry.getKey();
            Vec3 lab = entry.getValue();
            BlockState defaultState = block.axiom$defaultCustomState().getVanillaState();
            if (defaultState.getRenderShape() == RenderShape.MODEL
               && !isHardIgnore(defaultState.getBlock())
               && ((flags & FLAG_SOLID) == 0 || isSolid(defaultState))
               && ((flags & FLAG_OPAQUE) == 0 || isOpaque(defaultState))
               && ((flags & FLAG_FULL_CUBE) == 0 || isFullCube(defaultState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)))
               && ((flags & FLAG_SAME_TEXTURE) == 0 || sameTextureSet.contains(block))
               && ((flags & FLAG_NO_ORES) == 0 || !isOre(defaultState))
               && ((flags & FLAG_NO_GLAZED_TERRACOTTA) == 0 || !isGlazedTerracotta(defaultState))
               && ((flags & FLAG_NO_TILE_ENTITIES) == 0 || !isTileEntity(defaultState))) {
               tree.edit(lab, old -> {
                  if (old != null) {
                     String oldName = old.getBlock().builtInRegistryHolder().key().location().getPath();
                     String newName = defaultState.getBlock().builtInRegistryHolder().key().location().getPath();
                     return oldName.length() <= newName.length() ? old : defaultState;
                  } else {
                     return defaultState;
                  }
               });
            }
         }
      }
   }

   public static void invalidateCache() {
      colourMap.clear();
      tree.clear();
      coloursDirty = true;
      lastTreeFlags = -1;
   }

   private static boolean isSolid(BlockState blockState) {
      return blockState.blocksMotion();
   }

   private static boolean isOpaque(BlockState blockState) {
      return (ItemBlockRenderTypes.getChunkRenderType(blockState) == RenderType.solid() || blockState.getBlock() instanceof GrassBlock)
         && !(blockState.getBlock() instanceof LeavesBlock);
   }

   private static boolean isFullCube(VoxelShape shape) {
      return shape == Shapes.block();
   }

   private static boolean isOre(BlockState blockState) {
      return blockState.getBlock().builtInRegistryHolder().key().location().getPath().contains("_ore");
   }

   private static boolean isGlazedTerracotta(BlockState blockState) {
      return blockState.getBlock() instanceof GlazedTerracottaBlock
         || blockState.getBlock().builtInRegistryHolder().key().location().getPath().contains("_glazed_terracotta");
   }

   private static boolean isTileEntity(BlockState blockState) {
      return blockState.hasBlockEntity() || blockState.getBlock() instanceof ObserverBlock;
   }

   public static boolean isHardIgnore(Block block) {
      return block instanceof InfestedBlock || block == Blocks.WARPED_NYLIUM;
   }

   public static boolean isFullSolidOpaque(BlockState blockState) {
      return blockState.getRenderShape() == RenderShape.MODEL
         && isFullCube(blockState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
         && isSolid(blockState)
         && isOpaque(blockState);
   }

   public static Vec3 getLab(Block block) {
      recalculateColours();
      return colourMap.get((CustomBlock)block);
   }

   public static Vec3 getLab(CustomBlock block) {
      recalculateColours();
      return colourMap.get(block);
   }

   public static BlockState getNearestLab(double l, double a, double b, int flags) {
      recalculateTree(flags);
      return tree.nearest(new Vec3(l, a, b));
   }

   public static List<BlockState> getNearestLabN(double l, double a, double b, int flags, int count) {
      recalculateTree(flags);
      return tree.nearestN(new Vec3(l, a, b), count);
   }
}
