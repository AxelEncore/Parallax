package com.moulberry.axiom.tools.magic_select;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.IntWrapper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.shorts.ShortArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class MagicSelectionFast {
   private static MagicSelectionFast.CalculatedSection calculateSection(Level world, int cx, int cy, int cz, Block block) {
      LevelChunk chunk = world.getChunk(cx, cz);
      int sectionIndex = chunk.getSectionIndexFromSectionY(cy);
      if (sectionIndex >= 0 && sectionIndex < chunk.getSectionsCount()) {
         LevelChunkSection section = chunk.getSection(sectionIndex);
         if (section.hasOnlyAir()) {
            return new MagicSelectionFast.BooleanCalculatedSection(block == Blocks.AIR, cx << 4, cy << 4, cz << 4);
         } else if (!section.maybeHas(state -> state.getBlock() == block)) {
            return new MagicSelectionFast.BooleanCalculatedSection(false, cx << 4, cy << 4, cz << 4);
         } else {
            int index = 0;
            int count = 0;
            boolean[] contains = new boolean[4096];

            for (int z = 0; z < 16; z++) {
               for (int y = 0; y < 16; y++) {
                  for (int x = 0; x < 16; x++) {
                     BlockState blockState = section.getBlockState(x, y, z);
                     if (blockState.getBlock() == block) {
                        contains[index] = true;
                        count++;
                     }

                     index++;
                  }
               }
            }

            if (count == 0) {
               return new MagicSelectionFast.BooleanCalculatedSection(false, cx << 4, cy << 4, cz << 4);
            } else {
               return (MagicSelectionFast.CalculatedSection)(count == 4096
                  ? new MagicSelectionFast.BooleanCalculatedSection(true, cx << 4, cy << 4, cz << 4)
                  : new MagicSelectionFast.ArrayCalculatedSection(contains, cx << 4, cy << 4, cz << 4));
            }
         }
      } else {
         return new MagicSelectionFast.BooleanCalculatedSection(false, cx << 4, cy << 4, cz << 4);
      }
   }

   private record ArrayCalculatedSection(boolean[] contains, int chunkOriginX, int chunkOriginY, int chunkOriginZ)
      implements MagicSelectionFast.CalculatedSection {
      @Override
      public void check(
         ChunkedBooleanRegion region,
         IntWrapper count,
         int x,
         int y,
         int z,
         PositionSet alreadyChecked,
         LongArrayFIFOQueue toCheck,
         ShortArrayFIFOQueue sharedQueue
      ) {
         int localX = x & 15;
         int localY = y & 15;
         int localZ = z & 15;
         short index = (short)(localZ * 16 * 16 + localY * 16 + localX);
         if (this.contains[index]) {
            this.contains[index] = false;
            sharedQueue.enqueue(index);

            while (!sharedQueue.isEmpty()) {
               short packed = sharedQueue.dequeueShort();
               int locZ = packed >> 8 & 15;
               int locY = packed >> 4 & 15;
               int locX = packed & 15;
               region.add(this.chunkOriginX + locX, this.chunkOriginY + locY, this.chunkOriginZ + locZ);
               count.value++;

               for (int xo = -1; xo <= 1; xo++) {
                  for (int yo = -1; yo <= 1; yo++) {
                     for (int zo = -1; zo <= 1; zo++) {
                        int newLocX = locX + xo;
                        int newLocY = locY + yo;
                        int newLocZ = locZ + zo;
                        if (((newLocX | newLocY | newLocZ) & 16) != 0) {
                           if (alreadyChecked.add(this.chunkOriginX + newLocX, this.chunkOriginY + newLocY, this.chunkOriginZ + newLocZ)) {
                              toCheck.enqueue(BlockPos.asLong(this.chunkOriginX + newLocX, this.chunkOriginY + newLocY, this.chunkOriginZ + newLocZ));
                           }
                        } else {
                           short packedO = (short)(newLocZ * 16 * 16 + newLocY * 16 + newLocX);
                           if (this.contains[packedO]) {
                              this.contains[packedO] = false;
                              sharedQueue.enqueue(packedO);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static class BooleanCalculatedSection implements MagicSelectionFast.CalculatedSection {
      boolean contains;
      int chunkOriginX;
      int chunkOriginY;
      int chunkOriginZ;
      boolean alreadyChecked = false;

      public BooleanCalculatedSection(boolean contains, int chunkOriginX, int chunkOriginY, int chunkOriginZ) {
         this.contains = contains;
         this.chunkOriginX = chunkOriginX;
         this.chunkOriginY = chunkOriginY;
         this.chunkOriginZ = chunkOriginZ;
      }

      @Override
      public void check(
         ChunkedBooleanRegion region,
         IntWrapper count,
         int posX,
         int posY,
         int posZ,
         PositionSet alreadyChecked,
         LongArrayFIFOQueue toCheck,
         ShortArrayFIFOQueue sharedQueue
      ) {
         if (!this.alreadyChecked) {
            this.alreadyChecked = true;
            if (this.contains) {
               for (int x = this.chunkOriginX; x <= this.chunkOriginX + 15; x++) {
                  for (int y = this.chunkOriginY; y <= this.chunkOriginY + 15; y++) {
                     for (int z = this.chunkOriginZ; z <= this.chunkOriginZ + 15; z++) {
                        region.add(x, y, z);
                        alreadyChecked.add(x, y, z);
                     }
                  }
               }

               count.value += 4096;

               for (int y = this.chunkOriginY - 1; y <= this.chunkOriginY + 16; y++) {
                  for (int z = this.chunkOriginZ - 1; z <= this.chunkOriginZ + 16; z++) {
                     if (alreadyChecked.add(this.chunkOriginX - 1, y, z)) {
                        toCheck.enqueue(BlockPos.asLong(this.chunkOriginX - 1, y, z));
                     }

                     if (alreadyChecked.add(this.chunkOriginX + 16, y, z)) {
                        toCheck.enqueue(BlockPos.asLong(this.chunkOriginX + 16, y, z));
                     }
                  }
               }

               for (int x = this.chunkOriginX; x <= this.chunkOriginX + 15; x++) {
                  for (int z = this.chunkOriginZ - 1; z <= this.chunkOriginZ + 16; z++) {
                     if (alreadyChecked.add(x, this.chunkOriginY - 1, z)) {
                        toCheck.enqueue(BlockPos.asLong(x, this.chunkOriginY - 1, z));
                     }

                     if (alreadyChecked.add(x, this.chunkOriginY + 16, z)) {
                        toCheck.enqueue(BlockPos.asLong(x, this.chunkOriginY + 16, z));
                     }
                  }
               }

               for (int x = this.chunkOriginX; x <= this.chunkOriginX + 15; x++) {
                  for (int y = this.chunkOriginY; y <= this.chunkOriginY + 15; y++) {
                     if (alreadyChecked.add(x, y, this.chunkOriginZ - 1)) {
                        toCheck.enqueue(BlockPos.asLong(x, y, this.chunkOriginZ - 1));
                     }

                     if (alreadyChecked.add(x, y, this.chunkOriginZ - 1)) {
                        toCheck.enqueue(BlockPos.asLong(x, y, this.chunkOriginZ - 1));
                     }
                  }
               }
            }
         }
      }
   }

   private interface CalculatedSection {
      void check(ChunkedBooleanRegion var1, IntWrapper var2, int var3, int var4, int var5, PositionSet var6, LongArrayFIFOQueue var7, ShortArrayFIFOQueue var8);
   }

   public static class MagicSelectionTask {
      public Long2ObjectMap<MagicSelectionFast.CalculatedSection> calculatedSections;
      public PositionSet alreadyChecked = new PositionSet();
      public LongArrayFIFOQueue toCheck = new LongArrayFIFOQueue();
      public Level world;
      public ChunkedBooleanRegion region;
      public Block originalBlock;

      public MagicSelectionTask(ChunkedBooleanRegion region, Level world, BlockPos position) {
         this.calculatedSections = new Long2ObjectOpenHashMap();
         this.originalBlock = world.getBlockState(position).getBlock();
         this.region = region;
         this.world = world;
         this.toCheck.enqueue(BlockPos.asLong(position.getX(), position.getY(), position.getZ()));
         this.alreadyChecked.add(position.getX(), position.getY(), position.getZ());
      }

      public void fill(int until) {
         IntWrapper count = new IntWrapper();
         ShortArrayFIFOQueue sharedQueue = new ShortArrayFIFOQueue(64);

         while (!this.toCheck.isEmpty() && count.value < until) {
            long pos = this.toCheck.dequeueLong();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            int cx = x >> 4;
            int cy = y >> 4;
            int cz = z >> 4;
            MagicSelectionFast.CalculatedSection section = (MagicSelectionFast.CalculatedSection)this.calculatedSections
               .computeIfAbsent(BlockPos.asLong(cx, cy, cz), k -> MagicSelectionFast.calculateSection(this.world, cx, cy, cz, this.originalBlock));
            section.check(this.region, count, x, y, z, this.alreadyChecked, this.toCheck, sharedQueue);
         }
      }
   }
}
