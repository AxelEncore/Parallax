package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.world_modification.BiomeBuffer;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class SetBiomeOperation {
   public static void setBiome(ResourceKey<Biome> biome, boolean fillVertically) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         setBiomeAABB(aabb, biome, fillVertically);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         setBiomeSet(set, biome, fillVertically);
      }
   }

   private static void setBiomeAABB(SelectionBuffer.AABB aabb, ResourceKey<Biome> biome, boolean fillVertically) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BiomeBuffer setOperation = new BiomeBuffer();
         BiomeBuffer previousBiomesForUndo = new BiomeBuffer();
         int minX = aabb.min().getX() >> 2;
         int minZ = aabb.min().getZ() >> 2;
         int maxX = aabb.max().getX() >> 2;
         int maxZ = aabb.max().getZ() >> 2;
         if (fillVertically) {
            for (int bx = minX; bx <= maxX; bx++) {
               for (int bz = minZ; bz <= maxZ; bz++) {
                  LevelChunk chunk = (LevelChunk)world.getChunk(bx >> 2, bz >> 2, ChunkStatus.FULL, false);
                  if (chunk != null) {
                     int sectionCount = chunk.getSectionsCount();
                     int minSection = chunk.getMinSection();

                     for (int i = 0; i < sectionCount; i++) {
                        LevelChunkSection section = chunk.getSection(i);

                        for (int y = 0; y < 4; y++) {
                           if (section.getNoiseBiome(bx & 3, y, bz & 3) instanceof Reference<Biome> reference) {
                              ResourceKey<Biome> oldBiomeKey = reference.key();
                              if (oldBiomeKey != biome) {
                                 int by = (i + minSection << 2) + y;
                                 setOperation.set(bx, by, bz, biome);
                                 previousBiomesForUndo.set(bx, by, bz, oldBiomeKey);
                              }
                           }
                        }
                     }
                  }
               }
            }
         } else {
            int minY = aabb.min().getY() >> 2;
            int maxY = aabb.max().getY() >> 2;

            for (int bx = minX; bx <= maxX; bx++) {
               for (int by = minY; by <= maxY; by++) {
                  for (int bzx = minZ; bzx <= maxZ; bzx++) {
                     LevelChunk chunk = (LevelChunk)world.getChunk(bx >> 2, bzx >> 2, ChunkStatus.FULL, false);
                     if (chunk != null) {
                        int sectionIndex = world.getSectionIndexFromSectionY(by >> 2);
                        if (sectionIndex >= 0 && sectionIndex < chunk.getSectionsCount()) {
                           LevelChunkSection section = chunk.getSection(sectionIndex);
                           if (section.getNoiseBiome(bx & 3, by & 3, bzx & 3) instanceof Reference<Biome> referencex) {
                              ResourceKey<Biome> oldBiomeKey = referencex.key();
                              if (oldBiomeKey != biome) {
                                 setOperation.set(bx, by, bzx, biome);
                                 previousBiomesForUndo.set(bx, by, bzx, oldBiomeKey);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         String historyDescription = "Set biome to " + biome.location();
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBiomesForUndo, aabb.center(), historyDescription, 0));
      }
   }

   private static void setBiomeSet(SelectionBuffer.Set set, ResourceKey<Biome> biome, boolean fillVertically) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         PositionSet alreadyChecked = new PositionSet();
         BiomeBuffer setOperation = new BiomeBuffer();
         BiomeBuffer previousBiomesForUndo = new BiomeBuffer();
         if (fillVertically) {
            set.selectionRegion.forEach((x, y, z) -> {
               int bx = x >> 2;
               int bz = z >> 2;
               if (alreadyChecked.add(bx, 0, bz)) {
                  LevelChunk chunk = (LevelChunk)world.getChunk(bx >> 2, bz >> 2, ChunkStatus.FULL, false);
                  if (chunk == null) {
                     return;
                  }

                  int sectionCount = chunk.getSectionsCount();
                  int minSection = chunk.getMinSection();

                  for (int i = 0; i < sectionCount; i++) {
                     LevelChunkSection section = chunk.getSection(i);

                     for (int ly = 0; ly < 4; ly++) {
                        if (section.getNoiseBiome(bx & 3, ly, bz & 3) instanceof Reference<Biome> reference) {
                           ResourceKey<Biome> oldBiomeKey = reference.key();
                           if (oldBiomeKey != biome) {
                              int by = (i + minSection << 2) + ly;
                              setOperation.set(bx, by, bz, biome);
                              previousBiomesForUndo.set(bx, by, bz, oldBiomeKey);
                           }
                        }
                     }
                  }
               }
            });
         } else {
            set.selectionRegion.forEach((x, y, z) -> {
               int bx = x >> 2;
               int by = y >> 2;
               int bz = z >> 2;
               if (alreadyChecked.add(bx, by, bz)) {
                  LevelChunk chunk = (LevelChunk)world.getChunk(bx >> 2, bz >> 2, ChunkStatus.FULL, false);
                  if (chunk == null) {
                     return;
                  }

                  int sectionIndex = world.getSectionIndexFromSectionY(by >> 2);
                  if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) {
                     return;
                  }

                  LevelChunkSection section = chunk.getSection(sectionIndex);
                  if (section.getNoiseBiome(bx & 3, by & 3, bz & 3) instanceof Reference<Biome> reference) {
                     ResourceKey<Biome> oldBiomeKey = reference.key();
                     if (oldBiomeKey != biome) {
                        setOperation.set(bx, by, bz, biome);
                        previousBiomesForUndo.set(bx, by, bz, oldBiomeKey);
                     }
                  }
               }
            });
         }

         String historyDescription = "Set biome to " + biome.location();
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBiomesForUndo, set.selectionRegion.getCenter(), historyDescription, 0));
      }
   }
}
