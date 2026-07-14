package com.moulberry.axiom.render.cache;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;
import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.hooks.BufferBuilderExt;
import com.moulberry.axiom.render.BlockTessellator;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.AxiomVertexFormats;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.MemoryUtil.MemoryAllocator;

public class TessellatedBlockCache {
   private static final MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);
   private BlockState blockState = Blocks.AIR.defaultBlockState();
   private final VertexConsumerProvider provider = VertexConsumerProvider.owned(1024);
   private final BlockTessellator blockTessellator = new BlockTessellator(true, true);
   private final Int2LongOpenHashMap offsets = new Int2LongOpenHashMap();
   private final Int2LongOpenHashMap mod16Delta = new Int2LongOpenHashMap();
   private ByteBuffer vertexInformation;
   private VertexFormat format = null;
   private int formatStride = 0;
   private int formatPositionOffset = 0;
   private List<Object> reusableList;

   public TessellatedBlockCache() {
      long pointer = ALLOCATOR.malloc(1024L);
      if (pointer == 0L) {
         throw new OutOfMemoryError("Failed to allocate 1024 bytes");
      } else {
         this.vertexInformation = MemoryUtil.memByteBuffer(pointer, 1024);
      }
   }

   public void clear() {
      this.offsets.clear();
      this.mod16Delta.clear();
      this.vertexInformation.clear();
      this.format = null;
      this.formatStride = 0;
      this.formatPositionOffset = 0;
      this.blockTessellator.reset();
   }

   public void setBlockState(BlockState blockState) {
      if (this.blockState != blockState) {
         this.blockState = blockState;
         this.clear();
      } else {
         this.blockTessellator.reset();
      }
   }

   public void close() {
      long address = MemoryUtil.memAddress0(this.vertexInformation);
      this.vertexInformation = null;
      if (address != 0L) {
         ALLOCATOR.free(address);
      }

      this.provider.close();
   }

   public void setVertexFormat(VertexFormat vertexFormat) {
      if (this.format != vertexFormat) {
         this.clear();
         this.format = vertexFormat;
         this.formatStride = this.format.getVertexSize();
         this.formatPositionOffset = 0;

         for (VertexFormatElement element : this.format.getElements()) {
            if (element.usage() == Usage.POSITION && element.type() == Type.FLOAT && element.count() == 3) {
               break;
            }

            this.formatPositionOffset = this.formatPositionOffset + element.byteSize();
         }

         if (this.formatPositionOffset >= this.formatStride) {
            this.formatPositionOffset = -1;
         }
      }
   }

   public void renderBlock(BufferBuilder blockBuilder, ChunkedBlockRegion region, BlockPos blockPos) {
      if (BuildConfig.DEBUG && blockBuilder.format != this.format) {
         throw new FaultyImplementationError();
      } else {
         int wx = blockPos.getX();
         int wy = blockPos.getY();
         int wz = blockPos.getZ();
         int neighborInfo = 0;
         if (region.getBlockStateOrNull(wx, wy, wz - 1) != null) {
            neighborInfo |= 32;
         }

         if (region.getBlockStateOrNull(wx, wy - 1, wz) != null) {
            neighborInfo |= 2048;
         }

         if (region.getBlockStateOrNull(wx - 1, wy, wz) != null) {
            neighborInfo |= 8192;
         }

         if (region.getBlockStateOrNull(wx + 1, wy, wz) != null) {
            neighborInfo |= 32768;
         }

         if (region.getBlockStateOrNull(wx, wy + 1, wz) != null) {
            neighborInfo |= 131072;
         }

         if (region.getBlockStateOrNull(wx, wy, wz + 1) != null) {
            neighborInfo |= 8388608;
         }

         if (neighborInfo != 8562720) {
            if (region.getBlockStateOrNull(wx - 1, wy - 1, wz - 1) != null) {
               neighborInfo |= 2;
            }

            if (region.getBlockStateOrNull(wx, wy - 1, wz - 1) != null) {
               neighborInfo |= 4;
            }

            if (region.getBlockStateOrNull(wx + 1, wy - 1, wz - 1) != null) {
               neighborInfo |= 8;
            }

            if (region.getBlockStateOrNull(wx - 1, wy, wz - 1) != null) {
               neighborInfo |= 16;
            }

            if (region.getBlockStateOrNull(wx + 1, wy, wz - 1) != null) {
               neighborInfo |= 64;
            }

            if (region.getBlockStateOrNull(wx - 1, wy + 1, wz - 1) != null) {
               neighborInfo |= 128;
            }

            if (region.getBlockStateOrNull(wx, wy + 1, wz - 1) != null) {
               neighborInfo |= 256;
            }

            if (region.getBlockStateOrNull(wx + 1, wy + 1, wz - 1) != null) {
               neighborInfo |= 512;
            }

            if (region.getBlockStateOrNull(wx - 1, wy - 1, wz) != null) {
               neighborInfo |= 1024;
            }

            if (region.getBlockStateOrNull(wx + 1, wy - 1, wz) != null) {
               neighborInfo |= 4096;
            }

            if (region.getBlockStateOrNull(wx - 1, wy + 1, wz) != null) {
               neighborInfo |= 65536;
            }

            if (region.getBlockStateOrNull(wx + 1, wy + 1, wz) != null) {
               neighborInfo |= 262144;
            }

            if (region.getBlockStateOrNull(wx - 1, wy - 1, wz + 1) != null) {
               neighborInfo |= 524288;
            }

            if (region.getBlockStateOrNull(wx, wy - 1, wz + 1) != null) {
               neighborInfo |= 1048576;
            }

            if (region.getBlockStateOrNull(wx + 1, wy - 1, wz + 1) != null) {
               neighborInfo |= 2097152;
            }

            if (region.getBlockStateOrNull(wx - 1, wy, wz + 1) != null) {
               neighborInfo |= 4194304;
            }

            if (region.getBlockStateOrNull(wx + 1, wy, wz + 1) != null) {
               neighborInfo |= 16777216;
            }

            if (region.getBlockStateOrNull(wx - 1, wy + 1, wz + 1) != null) {
               neighborInfo |= 33554432;
            }

            if (region.getBlockStateOrNull(wx, wy + 1, wz + 1) != null) {
               neighborInfo |= 67108864;
            }

            if (region.getBlockStateOrNull(wx + 1, wy + 1, wz + 1) != null) {
               neighborInfo |= 134217728;
            }
         }

         int position;
         int vertexCount;
         if (!this.offsets.containsKey(neighborInfo)) {
            BufferBuilder builder = this.provider.begin(Mode.QUADS, AxiomVertexFormats.AXIOM_BLOCK);
            this.blockTessellator.tessellateBlockAndLiquidOffsetMod16(builder, blockPos, region, this.blockState);
            this.mod16Delta.put(neighborInfo, BlockPos.asLong(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15));
            MeshData buffer = this.provider.build();
            if (buffer == null) {
               this.offsets.put(neighborInfo, 0L);
               position = 0;
               vertexCount = 0;
            } else {
               ByteBuffer data = buffer.vertexBuffer();
               if (data.limit() % this.formatStride != 0) {
                  throw new FaultyImplementationError();
               }

               int needed = this.vertexInformation.position() + data.limit() + this.formatStride;
               if (needed >= this.vertexInformation.capacity()) {
                  int newCapacity = Mth.smallestEncompassingPowerOfTwo(needed);
                  long l = ALLOCATOR.realloc(MemoryUtil.memAddress0(this.vertexInformation), newCapacity);
                  if (l == 0L) {
                     throw new OutOfMemoryError("Failed to resize buffer from " + this.vertexInformation.capacity() + " bytes to " + newCapacity + " bytes");
                  }

                  ByteBuffer newBuffer = MemoryUtil.memByteBuffer(l, newCapacity);
                  newBuffer.position(this.vertexInformation.position());
                  this.vertexInformation = newBuffer;
               }

               MemoryUtil.memCopy(MemoryUtil.memAddress(data), MemoryUtil.memAddress(this.vertexInformation), data.limit());
               position = this.vertexInformation.position();
               vertexCount = data.limit() / this.formatStride;
               long info = position & 4294967295L | (vertexCount & 4294967295L) << 32;
               this.offsets.put(neighborInfo, info);
               this.vertexInformation.position(this.vertexInformation.position() + data.limit());
               buffer.close();
            }

            this.provider.clear();
         } else {
            long info = this.offsets.get(neighborInfo);
            position = (int)(info & 4294967295L);
            vertexCount = (int)(info >>> 32 & 4294967295L);
         }

         if (vertexCount > 0) {
            int numBytes = vertexCount * this.formatStride;
            long ptr = ((BufferBuilderExt)blockBuilder).axiom$reserve(numBytes);
            MemoryUtil.memCopy(MemoryUtil.memAddress(this.vertexInformation, position), ptr, numBytes);
            if (this.formatPositionOffset >= 0) {
               long liquidPositionDelta = this.mod16Delta.getOrDefault(neighborInfo, 0L);
               int x = (blockPos.getX() & 15) - BlockPos.getX(liquidPositionDelta);
               int y = (blockPos.getY() & 15) - BlockPos.getY(liquidPositionDelta);
               int z = (blockPos.getZ() & 15) - BlockPos.getZ(liquidPositionDelta);
               long addr = ptr + this.formatPositionOffset;

               for (int i = 0; i < vertexCount; i++) {
                  MemoryUtil.memPutFloat(addr, MemoryUtil.memGetFloat(addr) + x);
                  MemoryUtil.memPutFloat(addr + 4L, MemoryUtil.memGetFloat(addr + 4L) + y);
                  MemoryUtil.memPutFloat(addr + 8L, MemoryUtil.memGetFloat(addr + 8L) + z);
                  addr += this.formatStride;
               }
            }

            blockBuilder.vertices += vertexCount;
         }
      }
   }
}
