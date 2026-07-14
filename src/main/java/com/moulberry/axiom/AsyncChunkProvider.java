package com.moulberry.axiom;

import com.moulberry.axiom.hooks.PalettedContainerExt;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public class AsyncChunkProvider {
   private static final BlockState DEFAULT_STATE = Blocks.VOID_AIR.defaultBlockState();
   private final Long2ObjectMap<PalettedContainer<BlockState>> containerMap = new Long2ObjectOpenHashMap();
   private final Long2ObjectMap<PalettedContainerRO<Holder<Biome>>> biomeContainerMap = new Long2ObjectOpenHashMap();
   private final Level level;
   private final int minSectionY;
   private final int maxSectionY;
   private final int maxWorldY;

   public AsyncChunkProvider(Level level) {
      this.level = level;
      this.minSectionY = level.getMinSection();
      this.maxSectionY = level.getMaxSection() - 1;
      this.maxWorldY = level.getMaxBuildHeight() - 1;
   }

   public int getMaxY() {
      return this.maxWorldY;
   }

   public BlockState getBlockState(BlockPos blockPos) {
      return this.get(blockPos.getX(), blockPos.getY(), blockPos.getZ());
   }

   @Nullable
   public PalettedContainer<BlockState> getSection(int sectionX, int sectionY, int sectionZ) {
      if (sectionY >= this.minSectionY && sectionY <= this.maxSectionY) {
         long pos = BlockPos.asLong(sectionX, sectionY, sectionZ);
         return (PalettedContainer<BlockState>)this.containerMap.computeIfAbsent(pos, k -> {
            LevelChunk chunk = (LevelChunk)this.level.getChunk(sectionX, sectionZ, ChunkStatus.FULL, false);
            if (chunk == null) {
               return null;
            } else {
               LevelChunkSection levelChunkSection = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
               PalettedContainer<BlockState> container = levelChunkSection.getStates();
               ((PalettedContainerExt)container).axiom$lock();

               PalettedContainer var9;
               try {
                  var9 = container.copy();
               } finally {
                  ((PalettedContainerExt)container).axiom$unlock();
               }

               return var9;
            }
         });
      } else {
         return null;
      }
   }

   public BlockState get(int x, int y, int z) {
      int sectionY = y >> 4;
      if (sectionY >= this.minSectionY && sectionY <= this.maxSectionY) {
         PalettedContainer<BlockState> section = this.getSection(x >> 4, sectionY, z >> 4);
         return section == null ? DEFAULT_STATE : (BlockState)section.get(x & 15, y & 15, z & 15);
      } else {
         return DEFAULT_STATE;
      }
   }

   @Nullable
   public PalettedContainerRO<Holder<Biome>> getBiomeSection(int sectionX, int sectionY, int sectionZ) {
      if (sectionY >= this.minSectionY && sectionY <= this.maxSectionY) {
         long pos = BlockPos.asLong(sectionX, sectionY, sectionZ);
         return (PalettedContainerRO<Holder<Biome>>)this.biomeContainerMap.computeIfAbsent(pos, k -> {
            LevelChunk chunk = (LevelChunk)this.level.getChunk(sectionX, sectionZ, ChunkStatus.FULL, false);
            if (chunk == null) {
               return null;
            } else {
               LevelChunkSection levelChunkSection = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
               return levelChunkSection.getBiomes();
            }
         });
      } else {
         return null;
      }
   }

   @Nullable
   public Holder<Biome> getBiome(int quartX, int quartY, int quartZ) {
      int sectionY = quartY >> 2;
      if (sectionY >= this.minSectionY && sectionY <= this.maxSectionY) {
         PalettedContainerRO<Holder<Biome>> section = this.getBiomeSection(quartX >> 2, quartY >> 2, quartZ >> 2);
         return section == null ? null : (Holder)section.get(quartX & 3, quartY & 3, quartZ & 3);
      } else {
         return null;
      }
   }
}
