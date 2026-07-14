package com.moulberry.axiom.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.BiomeDataManager;
import com.moulberry.axiom.core_rendering.AxiomBufferUsage;
import com.moulberry.axiom.core_rendering.AxiomDraw;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.biome_painter.BiomePainterTool;
import com.moulberry.axiom.utils.ARGB32;
import com.moulberry.axiom.utils.AxiomVertexFormats;
import com.moulberry.axiom.utils.FramebufferUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.joml.Matrix4f;

public enum BiomeOverlayRenderer {
   INSTANCE;

   private final Long2ObjectMap<BiomeOverlayRenderer.ChunkData> chunkDataMap = new Long2ObjectOpenHashMap();
   private LongSet tickDirtyChunkSet = new LongOpenHashSet();
   private LongSet forgetChunkSet = new LongOpenHashSet();
   private boolean clearChunkData = false;
   private RenderTarget renderTarget;

   public void render(AxiomWorldRenderContext rc) {
      if (this.clearChunkData) {
         this.clearChunkData = false;
         this.chunkDataMap.values().forEach(BiomeOverlayRenderer.ChunkData::close);
         this.chunkDataMap.clear();
      }

      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         boolean biomeToolActive = false;
         if (EditorUI.isActive() && ToolManager.isToolActive() && ToolManager.getCurrentTool() instanceof BiomePainterTool biomePainterTool) {
            biomeToolActive = biomePainterTool.shouldRenderBiomeOverlay();
         }

         boolean setBiomeWindowIsActive = EditorUI.isActive() && EditorWindowType.SET_BIOME.isOpenAndActive();
         if (Axiom.configuration.visuals.showBiomes || setBiomeWindowIsActive || biomeToolActive) {
            this.uploadDirty();
            float offsetY = level.getMinBuildHeight();
            int mainWidth = Minecraft.getInstance().getMainRenderTarget().width;
            int mainHeight = Minecraft.getInstance().getMainRenderTarget().height;
            this.renderTarget = FramebufferUtils.resizeOrCreateFramebuffer(this.renderTarget, mainWidth, mainHeight);
            FramebufferUtils.clear(this.renderTarget, 0);
            FramebufferUtils.copyDepth(Minecraft.getInstance().getMainRenderTarget(), this.renderTarget);
            List<AxiomDraw> axiomDraws = new ArrayList<>();
            Matrix4f modelViewMatrix = rc.poseStack().last().pose();
            ObjectIterator var10 = this.chunkDataMap.values().iterator();

            while (var10.hasNext()) {
               BiomeOverlayRenderer.ChunkData chunkData = (BiomeOverlayRenderer.ChunkData)var10.next();
               Matrix4f translated = new Matrix4f(modelViewMatrix);
               translated.translate((float)(chunkData.offsetX - rc.x()), (float)(offsetY - rc.y()), (float)(chunkData.offsetZ - rc.z()));
               axiomDraws.add(new AxiomDraw(chunkData.vertexBuffer, translated, null, null));
            }

            AxiomRenderer.renderPipeline(AxiomRenderPipelines.BIOME_OVERLAY_PIPELINE, this.renderTarget, axiomDraws);
            FramebufferUtils.blitToMainBlend(this.renderTarget, mainWidth, mainHeight);
         }
      }
   }

   public void uploadDirty() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         if (!this.forgetChunkSet.isEmpty()) {
            LongSet forgetChunkSet = this.forgetChunkSet;
            LongIterator longIterator = forgetChunkSet.longIterator();

            while (longIterator.hasNext()) {
               long pos = longIterator.nextLong();
               BiomeOverlayRenderer.ChunkData data = (BiomeOverlayRenderer.ChunkData)this.chunkDataMap.remove(pos);
               if (data != null) {
                  data.close();
               }
            }

            this.forgetChunkSet = new LongOpenHashSet();
         }

         if (!this.tickDirtyChunkSet.isEmpty()) {
            LongSet dirtyChunkSet = this.tickDirtyChunkSet;
            this.tickDirtyChunkSet = new LongOpenHashSet();
            BiomeDataManager biomeDataManager = BiomeDataManager.get();
            LongIterator longIterator = dirtyChunkSet.longIterator();

            while (longIterator.hasNext()) {
               long pos = longIterator.nextLong();
               int chunkX = ChunkPos.getX(pos);
               int chunkZ = ChunkPos.getZ(pos);
               LevelChunk chunk = (LevelChunk)level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
               if (chunk != null) {
                  BiomeOverlayRenderer.ChunkData chunkData = (BiomeOverlayRenderer.ChunkData)this.chunkDataMap.get(pos);
                  if (chunkData == null) {
                     chunkData = new BiomeOverlayRenderer.ChunkData(chunkX * 16, chunkZ * 16);
                     this.chunkDataMap.put(pos, chunkData);
                  }

                  if (chunkData.vertexBuffer == null) {
                     chunkData.vertexBuffer = new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE);
                  }

                  VertexConsumerProvider provider = VertexConsumerProvider.shared();
                  renderChunk(biomeDataManager, level, chunk, chunkX, chunkZ, provider);
                  chunkData.vertexBuffer.upload(provider.build());
               }
            }

            VertexBuffer.unbind();
         }
      }
   }

   private static void renderChunk(
      BiomeDataManager biomeDataManager, ClientLevel level, LevelChunk chunk, int chunkX, int chunkZ, VertexConsumerProvider provider
   ) {
      BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, AxiomVertexFormats.BORDER_VERTEX_FORMAT);
      int alpha = 160;
      int minSection = chunk.getMinSection();
      int maxSection = chunk.getMaxSection() - 1;
      LevelChunk northChunk = (LevelChunk)level.getChunk(chunkX, chunkZ - 1, ChunkStatus.FULL, false);
      LevelChunk westChunk = (LevelChunk)level.getChunk(chunkX - 1, chunkZ, ChunkStatus.FULL, false);
      PalettedContainerRO<Holder<Biome>> aboveSection = chunk.getSection(0).getBiomes();

      for (int sectionY = minSection; sectionY <= maxSection; sectionY++) {
         PalettedContainerRO<Holder<Biome>> section = aboveSection;
         aboveSection = sectionY == maxSection ? null : chunk.getSection(sectionY - minSection + 1).getBiomes();
         PalettedContainerRO<Holder<Biome>> northSection = northChunk == null ? null : northChunk.getSection(sectionY - minSection).getBiomes();
         PalettedContainerRO<Holder<Biome>> westSection = westChunk == null ? null : westChunk.getSection(sectionY - minSection).getBiomes();
         int offsetY = (sectionY - minSection) * 16;

         for (int x = 0; x < 16; x += 4) {
            for (int y = 0; y < 16; y += 4) {
               for (int z = 0; z < 16; z += 4) {
                  int bx = x >> 2;
                  int by = y >> 2;
                  int bz = z >> 2;
                  Reference<Biome> biome = (Reference<Biome>)section.get(bx, by, bz);
                  int argb = biomeDataManager.getData(biome.key()).colour();
                  int red = ARGB32.red(argb);
                  int green = ARGB32.green(argb);
                  int blue = ARGB32.blue(argb);
                  Reference<Biome> aboveBiome;
                  if (by == 3) {
                     aboveBiome = aboveSection == null ? null : (Reference)aboveSection.get(bx, 0, bz);
                  } else {
                     aboveBiome = (Reference<Biome>)section.get(bx, by + 1, bz);
                  }

                  if (biome != aboveBiome) {
                     bufferBuilder.addVertex(x, offsetY + y + 4, z).setColor(red, green, blue, alpha).setUv2(0, 0);
                     bufferBuilder.addVertex(x, offsetY + y + 4, z + 4).setColor(red, green, blue, alpha).setUv2(0, 1);
                     bufferBuilder.addVertex(x + 4, offsetY + y + 4, z + 4).setColor(red, green, blue, alpha).setUv2(1, 1);
                     bufferBuilder.addVertex(x + 4, offsetY + y + 4, z).setColor(red, green, blue, alpha).setUv2(1, 0);
                     if (aboveBiome != null) {
                        int invArgb = biomeDataManager.getData(aboveBiome.key()).colour();
                        int invRed = ARGB32.red(invArgb);
                        int invGreen = ARGB32.green(invArgb);
                        int invBlue = ARGB32.blue(invArgb);
                        bufferBuilder.addVertex(x + 4, offsetY + y + 4, z).setColor(invRed, invGreen, invBlue, alpha).setUv2(1, 0);
                        bufferBuilder.addVertex(x + 4, offsetY + y + 4, z + 4).setColor(invRed, invGreen, invBlue, alpha).setUv2(1, 1);
                        bufferBuilder.addVertex(x, offsetY + y + 4, z + 4).setColor(invRed, invGreen, invBlue, alpha).setUv2(0, 1);
                        bufferBuilder.addVertex(x, offsetY + y + 4, z).setColor(invRed, invGreen, invBlue, alpha).setUv2(0, 0);
                     }
                  }

                  Reference<Biome> northBiome;
                  if (bz == 0) {
                     northBiome = northSection == null ? null : (Reference)northSection.get(bx, by, 3);
                  } else {
                     northBiome = (Reference<Biome>)section.get(bx, by, bz - 1);
                  }

                  if (northBiome != null && biome != northBiome) {
                     bufferBuilder.addVertex(x, offsetY + y + 4, z).setColor(red, green, blue, alpha).setUv2(1, 0);
                     bufferBuilder.addVertex(x + 4, offsetY + y + 4, z).setColor(red, green, blue, alpha).setUv2(1, 1);
                     bufferBuilder.addVertex(x + 4, offsetY + y, z).setColor(red, green, blue, alpha).setUv2(0, 1);
                     bufferBuilder.addVertex(x, offsetY + y, z).setColor(red, green, blue, alpha).setUv2(0, 0);
                     int invArgb = biomeDataManager.getData(northBiome.key()).colour();
                     int invRed = ARGB32.red(invArgb);
                     int invGreen = ARGB32.green(invArgb);
                     int invBlue = ARGB32.blue(invArgb);
                     bufferBuilder.addVertex(x, offsetY + y, z).setColor(invRed, invGreen, invBlue, alpha).setUv2(0, 0);
                     bufferBuilder.addVertex(x + 4, offsetY + y, z).setColor(invRed, invGreen, invBlue, alpha).setUv2(0, 1);
                     bufferBuilder.addVertex(x + 4, offsetY + y + 4, z).setColor(invRed, invGreen, invBlue, alpha).setUv2(1, 1);
                     bufferBuilder.addVertex(x, offsetY + y + 4, z).setColor(invRed, invGreen, invBlue, alpha).setUv2(1, 0);
                  }

                  Reference<Biome> westBiome;
                  if (bx == 0) {
                     westBiome = westSection == null ? null : (Reference)westSection.get(3, by, bz);
                  } else {
                     westBiome = (Reference<Biome>)section.get(bx - 1, by, bz);
                  }

                  if (westBiome != null && biome != westBiome) {
                     bufferBuilder.addVertex(x, offsetY + y, z).setColor(red, green, blue, alpha).setUv2(0, 0);
                     bufferBuilder.addVertex(x, offsetY + y, z + 4).setColor(red, green, blue, alpha).setUv2(0, 1);
                     bufferBuilder.addVertex(x, offsetY + y + 4, z + 4).setColor(red, green, blue, alpha).setUv2(1, 1);
                     bufferBuilder.addVertex(x, offsetY + y + 4, z).setColor(red, green, blue, alpha).setUv2(1, 0);
                     int invArgb = biomeDataManager.getData(westBiome.key()).colour();
                     int invRed = ARGB32.red(invArgb);
                     int invGreen = ARGB32.green(invArgb);
                     int invBlue = ARGB32.blue(invArgb);
                     bufferBuilder.addVertex(x, offsetY + y + 4, z).setColor(invRed, invGreen, invBlue, alpha).setUv2(1, 0);
                     bufferBuilder.addVertex(x, offsetY + y + 4, z + 4).setColor(invRed, invGreen, invBlue, alpha).setUv2(1, 1);
                     bufferBuilder.addVertex(x, offsetY + y, z + 4).setColor(invRed, invGreen, invBlue, alpha).setUv2(0, 1);
                     bufferBuilder.addVertex(x, offsetY + y, z).setColor(invRed, invGreen, invBlue, alpha).setUv2(0, 0);
                  }

                  if (by == 0 && sectionY == minSection) {
                     bufferBuilder.addVertex(x + 4, offsetY + y, z).setColor(red, green, blue, alpha).setUv2(1, 0);
                     bufferBuilder.addVertex(x + 4, offsetY + y, z + 4).setColor(red, green, blue, alpha).setUv2(1, 1);
                     bufferBuilder.addVertex(x, offsetY + y, z + 4).setColor(red, green, blue, alpha).setUv2(0, 1);
                     bufferBuilder.addVertex(x, offsetY + y, z).setColor(red, green, blue, alpha).setUv2(0, 0);
                  }
               }
            }
         }
      }
   }

   public void clear() {
      this.tickDirtyChunkSet.clear();
      this.forgetChunkSet.clear();
      this.clearChunkData = true;
   }

   public void markDirty(int chunkX, int chunkZ) {
      long pos = ChunkPos.asLong(chunkX, chunkZ);
      this.tickDirtyChunkSet.add(pos);
   }

   public void forgetChunk(int chunkX, int chunkZ) {
      long pos = ChunkPos.asLong(chunkX, chunkZ);
      this.forgetChunkSet.add(pos);
      this.tickDirtyChunkSet.remove(pos);
   }

   private static class ChunkData {
      private AxiomDrawBuffer vertexBuffer = new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE);
      private final int offsetX;
      private final int offsetZ;

      public ChunkData(int offsetX, int offsetZ) {
         this.offsetX = offsetX;
         this.offsetZ = offsetZ;
      }

      public void close() {
         this.vertexBuffer.close();
         this.vertexBuffer = null;
      }
   }
}
