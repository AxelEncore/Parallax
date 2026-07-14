package com.moulberry.axiom.tools.stamp;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public record StampPlacement(
   ChunkedBlockRegion blockRegion,
   Long2ObjectMap<CompressedBlockEntity> blockEntities,
   TransformedBlockRegions originalSet,
   int originalRotations,
   boolean originalFlipX,
   boolean originalFlipZ,
   int x,
   int y,
   int z,
   int baseX,
   int baseY,
   int baseZ
) {
   public StampPlacement withBasePosition(int baseX, int baseY, int baseZ) {
      int relX = this.x - this.baseX;
      int relY = this.y - this.baseY;
      int relZ = this.z - this.baseZ;
      return new StampPlacement(
         this.blockRegion,
         this.blockEntities,
         this.originalSet,
         this.originalRotations,
         this.originalFlipX,
         this.originalFlipZ,
         baseX + relX,
         baseY + relY,
         baseZ + relZ,
         baseX,
         baseY,
         baseZ
      );
   }

   public StampPlacement rotate(int rotations) {
      int newRotations = (this.originalRotations + rotations) % 4;
      return new StampPlacement(
         this.originalSet.getBlocks(newRotations, this.originalFlipX, this.originalFlipZ),
         this.originalSet.getBlockEntities(newRotations, this.originalFlipX, this.originalFlipZ),
         this.originalSet,
         newRotations,
         this.originalFlipX,
         this.originalFlipZ,
         this.x,
         this.y,
         this.z,
         this.baseX,
         this.baseY,
         this.baseZ
      );
   }

   public void pasteInto(ChunkedBlockRegion into, Long2ObjectMap<CompressedBlockEntity> blockEntities, MaskContext maskContext, MaskElement maskElement) {
      int minX = this.blockRegion.min().getX();
      int minY = this.blockRegion.min().getY();
      int minZ = this.blockRegion.min().getZ();
      int maxX = this.blockRegion.max().getX();
      int maxY = this.blockRegion.max().getY();
      int maxZ = this.blockRegion.max().getZ();
      int xo = this.x - (maxX + minX) / 2;
      int yo = this.y - (maxY + minY) / 2;
      int zo = this.z - (maxZ + minZ) / 2;
      this.blockRegion.forEachEntry((x, y, z, state) -> {
         if (maskElement.test(maskContext.reset(), x + xo, y + yo, z + zo)) {
            CompressedBlockEntity blockEntity = (CompressedBlockEntity)this.blockEntities.get(BlockPos.asLong(x, y, z));
            into.addBlock(x + xo, y + yo, z + zo, state);
            if (blockEntity != null) {
               blockEntities.put(BlockPos.asLong(x + xo, y + yo, z + zo), blockEntity);
            }
         }
      });
   }

   public void pasteIntoExtendToGround(
      ChunkedBlockRegion into, Long2ObjectMap<CompressedBlockEntity> blockEntities, MaskContext maskContext, MaskElement maskElement
   ) {
      int minX = this.blockRegion.min().getX();
      int minY = this.blockRegion.min().getY();
      int minZ = this.blockRegion.min().getZ();
      int maxX = this.blockRegion.max().getX();
      int maxY = this.blockRegion.max().getY();
      int maxZ = this.blockRegion.max().getZ();
      int xo = this.x - (maxX + minX) / 2;
      int yo = this.y - (maxY + minY) / 2;
      int zo = this.z - (maxZ + minZ) / 2;
      this.blockRegion.forEachEntry((x, y, z, state) -> {
         if (maskElement.test(maskContext.reset(), x + xo, y + yo, z + zo)) {
            CompressedBlockEntity blockEntity = (CompressedBlockEntity)this.blockEntities.get(BlockPos.asLong(x, y, z));
            into.addBlock(x + xo, y + yo, z + zo, state);
            if (blockEntity != null) {
               blockEntities.put(BlockPos.asLong(x + xo, y + yo, z + zo), blockEntity);
            }

            if (y == minY && state.blocksMotion()) {
               for (int y2 = y - 1; y2 > y - 33; y2--) {
                  if (!maskElement.test(maskContext.reset(), x + xo, y2 + yo, z + zo)) {
                     return;
                  }

                  BlockState blockState = maskContext.getBlockState(x + xo, y2 + yo, z + zo);
                  if (blockState.blocksMotion() || blockState.getBlock() == Blocks.VOID_AIR) {
                     break;
                  }

                  into.addBlock(x + xo, y2 + yo, z + zo, state);
                  if (blockEntity != null) {
                     blockEntities.put(BlockPos.asLong(x + xo, y2 + yo, z + zo), blockEntity);
                  }
               }
            }
         }
      });
   }

   public void pasteIntoApplyGravity(
      ChunkedBlockRegion into, Long2ObjectMap<CompressedBlockEntity> blockEntities, Level level, MaskContext maskContext, MaskElement maskElement
   ) {
      int minX = this.blockRegion.min().getX();
      int minY = this.blockRegion.min().getY();
      int minZ = this.blockRegion.min().getZ();
      int maxX = this.blockRegion.max().getX();
      int maxY = this.blockRegion.max().getY();
      int maxZ = this.blockRegion.max().getZ();
      int xo = this.x - (maxX + minX) / 2;
      int yo = this.y - (maxY + minY) / 2;
      int zo = this.z - (maxZ + minZ) / 2;
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      this.blockRegion.forEachEntryLowestFirst((x, y, z, state) -> {
         for (int y2 = y - 1; y2 > y - 33; y2--) {
            mutableBlockPos.set(x + xo, y2 + yo, z + zo);
            BlockState existing = into.getBlockStateOrDelegate(mutableBlockPos, level);
            if (!existing.isAir()) {
               if (!maskElement.test(maskContext.reset(), x + xo, y2 + yo + 1, z + zo)) {
                  return;
               }

               into.addBlock(x + xo, y2 + yo + 1, z + zo, state);
               long pos = BlockPos.asLong(x, y, z);
               CompressedBlockEntity blockEntity = (CompressedBlockEntity)this.blockEntities.get(pos);
               if (blockEntity != null) {
                  blockEntities.put(BlockPos.asLong(x + xo, y2 + yo + 1, z + zo), blockEntity);
               }

               return;
            }
         }
      });
   }

   public Vec3 getRenderOffset() {
      int minX = this.blockRegion.min().getX();
      int minY = this.blockRegion.min().getY();
      int minZ = this.blockRegion.min().getZ();
      int maxX = this.blockRegion.max().getX();
      int maxY = this.blockRegion.max().getY();
      int maxZ = this.blockRegion.max().getZ();
      int x = this.x - (maxX + minX) / 2;
      int y = this.y - (maxY + minY) / 2;
      int z = this.z - (maxZ + minZ) / 2;
      return new Vec3(x, y, z);
   }

   public void applyFailingDestinationMaskPreview(ChunkedBooleanRegion previewFailingDestinationMask, MaskContext maskContext, MaskElement maskElement) {
      int minX = this.blockRegion.min().getX();
      int minY = this.blockRegion.min().getY();
      int minZ = this.blockRegion.min().getZ();
      int maxX = this.blockRegion.max().getX();
      int maxY = this.blockRegion.max().getY();
      int maxZ = this.blockRegion.max().getZ();
      int xo = this.x - (maxX + minX) / 2;
      int yo = this.y - (maxY + minY) / 2;
      int zo = this.z - (maxZ + minZ) / 2;
      this.blockRegion.forEachEntry((x, y, z, state) -> {
         if (!maskElement.test(maskContext.reset(), x + xo, y + yo, z + zo)) {
            previewFailingDestinationMask.add(x + xo, y + yo, z + zo);
         }
      });
   }
}
