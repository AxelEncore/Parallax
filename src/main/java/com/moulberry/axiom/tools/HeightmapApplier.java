package com.moulberry.axiom.tools;

import com.moulberry.axiom.collections.Position2dToIntMap;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class HeightmapApplier {
   public ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
   public ChunkedBooleanRegion removeRegion = new ChunkedBooleanRegion();
   private final Position2dToIntMap originalHeights = new Position2dToIntMap(Integer.MIN_VALUE);
   private final Position2dToIntMap modifiedHeights = new Position2dToIntMap(Integer.MIN_VALUE);
   private Position2dToIntMap constrainedHeights = new Position2dToIntMap(Integer.MIN_VALUE);
   private final LongSet dirtyChunks = new LongOpenHashSet();
   private float smoothing;

   public void reset(float smoothing) {
      this.blockRegion.clear();
      this.removeRegion.clear();
      this.originalHeights.clear();
      this.modifiedHeights.clear();
      this.constrainedHeights.clear();
      this.smoothing = smoothing;
   }

   public void setOriginalY(int x, int z, int y) {
      this.originalHeights.put(x, z, y);
   }

   public void setModifiedY(int x, int z, int y) {
      this.modifiedHeights.put(x, z, y);
      this.dirtyChunks.add(ChunkPos.asLong(x >> 4, z >> 4));
   }

   public int getOriginalY(int x, int z) {
      return this.originalHeights.get(x, z);
   }

   public int getModifiedY(int x, int z) {
      return this.modifiedHeights.get(x, z);
   }

   public int getConstrainedY(int x, int z) {
      return this.constrainedHeights.get(x, z);
   }

   public void iterateOriginalY(TriIntConsumer consumer) {
      this.originalHeights.forEachEntry(consumer);
   }

   public void update() {
      if (!this.dirtyChunks.isEmpty()) {
         LongSet dirtyChunksAndNeighbors = new LongOpenHashSet();
         LongIterator longIterator = this.dirtyChunks.longIterator();

         while (longIterator.hasNext()) {
            long pos = longIterator.nextLong();
            int chunkX = ChunkPos.getX(pos);
            int chunkZ = ChunkPos.getZ(pos);
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX - 1, chunkZ - 1));
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX, chunkZ - 1));
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX + 1, chunkZ - 1));
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX - 1, chunkZ));
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX, chunkZ));
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX + 1, chunkZ));
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX - 1, chunkZ + 1));
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX, chunkZ + 1));
            dirtyChunksAndNeighbors.add(ChunkPos.asLong(chunkX + 1, chunkZ + 1));
         }

         this.dirtyChunks.clear();
         if (this.smoothing < 0.01) {
            this.updateRegion(dirtyChunksAndNeighbors, this.modifiedHeights);
         } else {
            Long2ObjectMap<int[]> constrainedHeightsMap = new Long2ObjectOpenHashMap();
            Long2ObjectMap<int[]> baseHeightsMap = new Long2ObjectOpenHashMap();
            Long2ObjectMap<int[]> oldModifiedHeightsMap = this.modifiedHeights.unsafeGetMap();
            Long2ObjectMap<int[]> oldOriginalHeightsMap = this.originalHeights.unsafeGetMap();
            longIterator = dirtyChunksAndNeighbors.longIterator();

            while (longIterator.hasNext()) {
               long pos = longIterator.nextLong();
               int[] modifiedValues = (int[])oldModifiedHeightsMap.get(pos);
               int[] originalValues = (int[])oldOriginalHeightsMap.get(pos);
               if (modifiedValues != null && originalValues != null) {
                  constrainedHeightsMap.put(pos, Arrays.copyOf(modifiedValues, modifiedValues.length));
                  baseHeightsMap.put(pos, Arrays.copyOf(originalValues, originalValues.length));
               }
            }

            Long2ObjectMap<int[]> oldConstrainedHeightsMap = this.constrainedHeights.unsafeGetMap();
            ObjectIterator newConstrainedHeights = oldConstrainedHeightsMap.long2ObjectEntrySet().iterator();

            while (newConstrainedHeights.hasNext()) {
               Entry<int[]> entry = (Entry<int[]>)newConstrainedHeights.next();
               if (!constrainedHeightsMap.containsKey(entry.getLongKey())) {
                  constrainedHeightsMap.put(entry.getLongKey(), Arrays.copyOf((int[])entry.getValue(), ((int[])entry.getValue()).length));
               }
            }

            Position2dToIntMap newConstrainedHeightsx = new Position2dToIntMap(Integer.MIN_VALUE, constrainedHeightsMap);
            Position2dToIntMap baseHeights = new Position2dToIntMap(Integer.MIN_VALUE, baseHeightsMap);
            int errorThreshold = 15 - (int)(this.smoothing * 15.0F);
            BooleanWrapper continueConstraining = new BooleanWrapper(true);

            while (continueConstraining.value) {
               continueConstraining.value = false;
               baseHeights.removeIf((x, z, baseHeight) -> {
                  int modifiedHeight = newConstrainedHeightsx.get(x, z);
                  if (baseHeight == modifiedHeight) {
                     return true;
                  } else {
                     int constrainedHeight = baseHeight < modifiedHeight ? modifiedHeight - 1 : modifiedHeight + 1;
                     int height1 = newConstrainedHeightsx.get(x + 1, z);
                     int height2 = newConstrainedHeightsx.get(x - 1, z);
                     int height3 = newConstrainedHeightsx.get(x, z + 1);
                     int height4 = newConstrainedHeightsx.get(x, z - 1);
                     int origErr1 = height1 - constrainedHeight;
                     int origErr2 = height2 - constrainedHeight;
                     int origErr3 = height3 - constrainedHeight;
                     int origErr4 = height4 - constrainedHeight;
                     int newError = origErr1 * origErr1 + origErr2 * origErr2 + origErr3 * origErr3 + origErr4 * origErr4;
                     int modErr1 = height1 - modifiedHeight;
                     int modErr2 = height2 - modifiedHeight;
                     int modErr3 = height3 - modifiedHeight;
                     int modErr4 = height4 - modifiedHeight;
                     int oldError = modErr1 * modErr1 + modErr2 * modErr2 + modErr3 * modErr3 + modErr4 * modErr4;
                     if (newError + errorThreshold < oldError) {
                        newConstrainedHeightsx.put(x, z, constrainedHeight);
                        continueConstraining.value = true;
                        return constrainedHeight == baseHeight;
                     } else {
                        return false;
                     }
                  }
               });
            }

            this.constrainedHeights = newConstrainedHeightsx;
            this.updateRegion(dirtyChunksAndNeighbors, this.constrainedHeights);
         }
      }
   }

   private void updateRegion(LongSet dirtyChunksAndNeighbors, Position2dToIntMap constrained) {
      BlockState dirt = Blocks.DIRT.defaultBlockState();
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         MaskElement destMask = MaskManager.getDestMask();
         MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         LongIterator longIterator = dirtyChunksAndNeighbors.longIterator();

         while (longIterator.hasNext()) {
            long pos = longIterator.nextLong();
            int[] heights = this.originalHeights.getChunk(pos);
            if (heights != null) {
               int cx = ChunkPos.getX(pos);
               int cz = ChunkPos.getZ(pos);
               int wcx = cx * 16;
               int wcz = cz * 16;
               ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
               if (chunk != null) {
                  int index = 0;

                  for (int zo = 0; zo < 16; zo++) {
                     for (int xo = 0; xo < 16; xo++) {
                        int originalHeight = heights[index++];
                        if (originalHeight != Integer.MIN_VALUE) {
                           int x = wcx + xo;
                           int z = wcz + zo;
                           int modifiedHeight = constrained.get(x, z);
                           if (modifiedHeight < originalHeight) {
                              int y;
                              for (y = originalHeight; y > modifiedHeight && destMask.test(maskContext.reset(), x, y, z); y--) {
                                 this.blockRegion.addBlock(x, y, z, Blocks.AIR.defaultBlockState());
                                 ChunkRenderOverrider.setBlock(x, y, z, Blocks.AIR.defaultBlockState());
                                 this.removeRegion.add(x, y, z);
                              }

                              for (int i = 0; i < 8; i++) {
                                 BlockState aboveState = chunk.getBlockState(mutableBlockPos.set(x, originalHeight + i + 1, z));
                                 if (aboveState.isAir() || aboveState.blocksMotion() || !aboveState.getFluidState().isEmpty()) {
                                    break;
                                 }

                                 this.blockRegion.addBlock(x, originalHeight + i + 1, z, Blocks.AIR.defaultBlockState());
                                 ChunkRenderOverrider.setBlock(x, originalHeight + i + 1, z, Blocks.AIR.defaultBlockState());
                                 this.removeRegion.add(x, originalHeight + i + 1, z);
                                 this.blockRegion.addBlock(x, y + 1 + i, z, aboveState);
                              }
                           } else if (modifiedHeight > originalHeight) {
                              int y = originalHeight;
                              BlockState blockState = chunk.getBlockState(mutableBlockPos.set(x, originalHeight, z));
                              if (blockState.getBlock() != Blocks.GRASS_BLOCK) {
                                 while (y <= modifiedHeight && destMask.test(maskContext.reset(), x, y, z)) {
                                    this.blockRegion.addBlock(x, y, z, blockState);
                                    y++;
                                 }
                              } else {
                                 while (y <= modifiedHeight && destMask.test(maskContext.reset(), x, y, z)) {
                                    this.blockRegion.addBlock(x, y, z, dirt);
                                    y++;
                                 }

                                 if (y > originalHeight) {
                                    this.blockRegion.addBlock(x, y - 1, z, Blocks.GRASS_BLOCK.defaultBlockState());
                                 }
                              }

                              for (int i = 0; i < 8; i++) {
                                 BlockState aboveState = chunk.getBlockState(mutableBlockPos.set(x, originalHeight + i + 1, z));
                                 if (aboveState.isAir()
                                    || aboveState.blocksMotion()
                                    || !aboveState.getFluidState().isEmpty()
                                    || !destMask.test(maskContext.reset(), x, y + i, z)) {
                                    break;
                                 }

                                 this.blockRegion.addBlock(x, y + i, z, aboveState);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
