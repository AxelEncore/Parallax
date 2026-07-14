package com.moulberry.axiom.tools.stamp;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.IntMatrix;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;

public class TransformedBlockRegions {
   private final ChunkedBlockRegion[] regions = new ChunkedBlockRegion[16];
   private final Long2ObjectMap<CompressedBlockEntity>[] blockEntities = new Long2ObjectMap[16];

   public TransformedBlockRegions(ChunkedBlockRegion base, Long2ObjectMap<CompressedBlockEntity> blockEntities) {
      this.regions[0] = base;
      this.blockEntities[0] = blockEntities;
   }

   public ChunkedBlockRegion getBlocks(int rotation, boolean flipX, boolean flipZ) {
      int index = rotation % 4;
      if (index < 0) {
         index += 4;
      }

      if (flipX) {
         index += 4;
      }

      if (flipZ) {
         index += 8;
      }

      ChunkedBlockRegion region = this.regions[index];
      if (region == null) {
         region = this.regions[0];
         if (flipX) {
            region = region.flip(Axis.X);
         }

         if (flipZ) {
            region = region.flip(Axis.Z);
         }

         region = region.rotate(Axis.Y, rotation);
         this.regions[index] = region;
      }

      return region;
   }

   public Long2ObjectMap<CompressedBlockEntity> getBlockEntities(int rotation, boolean flipX, boolean flipZ) {
      int index = rotation % 4;
      if (index < 0) {
         index += 4;
      }

      if (flipX) {
         index += 4;
      }

      if (flipZ) {
         index += 8;
      }

      Long2ObjectMap<CompressedBlockEntity> blockEntities = this.blockEntities[index];
      if (blockEntities == null) {
         IntMatrix matrix = new IntMatrix();
         if (flipX) {
            matrix.flip(Axis.X);
         }

         if (flipZ) {
            matrix.flip(Axis.Z);
         }

         matrix.rotate(Axis.Y, rotation);
         blockEntities = new Long2ObjectOpenHashMap();
         ObjectIterator var7 = this.blockEntities[0].long2ObjectEntrySet().iterator();

         while (var7.hasNext()) {
            Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var7.next();
            long pos = entry.getLongKey();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            int nx = matrix.transformX(x, y, z);
            int ny = matrix.transformY(x, y, z);
            int nz = matrix.transformZ(x, y, z);
            blockEntities.put(BlockPos.asLong(nx, ny, nz), (CompressedBlockEntity)entry.getValue());
         }

         this.blockEntities[index] = blockEntities;
      }

      return blockEntities;
   }
}
