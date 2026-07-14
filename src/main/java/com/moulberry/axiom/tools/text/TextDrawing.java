package com.moulberry.axiom.tools.text;

import com.moulberry.axiom.block_maps.HDVoxelMap;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.BlockHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.MissingResourceException;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.stb.STBTTPackedchar.Buffer;

public class TextDrawing {
   @Nullable
   private static ByteBuffer loadFont(Path path) {
      byte[] bytes;
      if (path == null) {
         Optional<Resource> resource = Minecraft.getInstance()
            .getResourceManager()
            .getResource(ResourceLocation.fromNamespaceAndPath("axiom", "inter-medium.ttf"));
         if (resource.isEmpty()) {
            throw new MissingResourceException("Missing font: inter-medium.ttf", "Font", "");
         }

         try (InputStream is = resource.get().open()) {
            bytes = is.readAllBytes();
         } catch (IOException var9) {
            var9.printStackTrace();
            return null;
         }
      } else {
         if (!Files.exists(path)) {
            return null;
         }

         try {
            bytes = Files.readAllBytes(path);
         } catch (IOException var7) {
            var7.printStackTrace();
            return null;
         }
      }

      ByteBuffer data = BufferUtils.createByteBuffer(bytes.length);
      data.put(bytes);
      data.flip();
      return data;
   }

   public static void drawSimple(ChunkedBlockRegion chunkedBlockRegion, Path path, BlockState blockState, String text, int direction, float pixelHeight) {
      if (pixelHeight > 256.0F) {
         pixelHeight = 256.0F;
      }

      if (pixelHeight < 8.0F) {
         pixelHeight = 8.0F;
      }

      ByteBuffer data = loadFont(path);
      if (data != null) {
         STBTTFontinfo info = STBTTFontinfo.malloc();
         STBTruetype.stbtt_InitFont(info, data);
         data.rewind();

         record CharacterData(Buffer chardata, ByteBuffer bitmap) {
         }

         Int2ObjectMap<CharacterData> characterData = new Int2ObjectOpenHashMap();
         ByteBuffer currentBitmap = BufferUtils.createByteBuffer(1048576);
         STBTTPackContext packContext = STBTTPackContext.malloc();
         STBTruetype.stbtt_PackBegin(packContext, currentBitmap, 1024, 1024, 0, 1, 0L);
         int[] codepoint = new int[]{0};
         int i = 0;

         while (i < text.length()) {
            i += getCodepoint(text, i, codepoint);
            if (!characterData.containsKey(codepoint[0])) {
               Buffer packedchar = STBTTPackedchar.malloc(1);
               if (STBTruetype.stbtt_PackFontRange(packContext, data, 0, pixelHeight, codepoint[0], packedchar)) {
                  characterData.put(codepoint[0], new CharacterData(packedchar, currentBitmap));
               } else {
                  currentBitmap = BufferUtils.createByteBuffer(1048576);
                  STBTruetype.stbtt_PackEnd(packContext);
                  packContext.free();
                  packContext = STBTTPackContext.malloc();
                  STBTruetype.stbtt_PackBegin(packContext, currentBitmap, 1024, 1024, 0, 1, 0L);
                  if (!STBTruetype.stbtt_PackFontRange(packContext, data, 0, pixelHeight, codepoint[0], packedchar)) {
                     throw new Error("Failed to allocate a second time");
                  }

                  characterData.put(codepoint[0], new CharacterData(packedchar, currentBitmap));
               }
            }
         }

         STBTruetype.stbtt_PackEnd(packContext);
         packContext.free();
         STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();
         float scale = STBTruetype.stbtt_ScaleForPixelHeight(info, pixelHeight);
         float[] locx = new float[]{0.0F};
         float[] locy = new float[]{0.0F};
         int ix = 0;

         while (ix < text.length()) {
            ix += getCodepoint(text, ix, codepoint);
            CharacterData charData = (CharacterData)characterData.get(codepoint[0]);
            if (charData == null) {
               throw new FaultyImplementationError();
            }

            STBTruetype.stbtt_GetPackedQuad(charData.chardata, 1024, 1024, 0, locx, locy, quad, true);
            if (ix < text.length()) {
               int currentCodepoint = codepoint[0];
               getCodepoint(text, ix, codepoint);
               locx[0] += STBTruetype.stbtt_GetCodepointKernAdvance(info, currentCodepoint, codepoint[0]) * scale;
            }

            for (int x = (int)quad.x0(); x < (int)quad.x1(); x++) {
               for (int y = (int)quad.y0(); y < (int)quad.y1(); y++) {
                  float value = lookup(charData.bitmap, quad, x, y);
                  if (value > 0.5) {
                     switch (direction) {
                        case 1:
                           chunkedBlockRegion.addBlock(0, -y, x, blockState);
                           break;
                        case 2:
                           chunkedBlockRegion.addBlock(-x, -y, 0, blockState);
                           break;
                        case 3:
                           chunkedBlockRegion.addBlock(0, -y, -x, blockState);
                           break;
                        default:
                           chunkedBlockRegion.addBlock(x, -y, 0, blockState);
                     }
                  }
               }
            }
         }

         quad.free();
         info.free();
         ObjectIterator var23 = characterData.values().iterator();

         while (var23.hasNext()) {
            CharacterData value = (CharacterData)var23.next();
            value.chardata.free();
         }
      }
   }

   public static void drawFancy(
      ChunkedBlockRegion chunkedBlockRegion, Path path, HDVoxelMap.HDVoxelBaseBlocks blocks, String text, int direction, float pixelHeight
   ) {
      if (pixelHeight > 256.0F) {
         pixelHeight = 256.0F;
      }

      if (pixelHeight < 8.0F) {
         pixelHeight = 8.0F;
      }

      ByteBuffer data = loadFont(path);
      if (data != null) {
         STBTTFontinfo info = STBTTFontinfo.malloc();
         STBTruetype.stbtt_InitFont(info, data);
         data.rewind();

         record CharacterData(Buffer chardata, ByteBuffer bitmap) {
         }

         Int2ObjectMap<CharacterData> characterData = new Int2ObjectOpenHashMap();
         ByteBuffer currentBitmap = BufferUtils.createByteBuffer(1048576);
         STBTTPackContext packContext = STBTTPackContext.malloc();
         STBTruetype.stbtt_PackBegin(packContext, currentBitmap, 1024, 1024, 0, 1, 0L);
         int[] codepoint = new int[]{0};
         int i = 0;

         while (i < text.length()) {
            i += getCodepoint(text, i, codepoint);
            if (!characterData.containsKey(codepoint[0])) {
               Buffer packedchar = STBTTPackedchar.malloc(1);
               if (STBTruetype.stbtt_PackFontRange(packContext, data, 0, pixelHeight, codepoint[0], packedchar)) {
                  characterData.put(codepoint[0], new CharacterData(packedchar, currentBitmap));
               } else {
                  currentBitmap = BufferUtils.createByteBuffer(1048576);
                  STBTruetype.stbtt_PackEnd(packContext);
                  packContext.free();
                  packContext = STBTTPackContext.malloc();
                  STBTruetype.stbtt_PackBegin(packContext, currentBitmap, 1024, 1024, 0, 1, 0L);
                  if (!STBTruetype.stbtt_PackFontRange(packContext, data, 0, pixelHeight, codepoint[0], packedchar)) {
                     throw new Error("Failed to allocate a second time");
                  }

                  characterData.put(codepoint[0], new CharacterData(packedchar, currentBitmap));
               }
            }
         }

         STBTruetype.stbtt_PackEnd(packContext);
         packContext.free();
         STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();
         float scale = STBTruetype.stbtt_ScaleForPixelHeight(info, pixelHeight);
         float[] locx = new float[]{0.0F};
         float[] locy = new float[]{0.0F};
         int lastXEnd = 0;
         int ix = 0;

         while (ix < text.length()) {
            ix += getCodepoint(text, ix, codepoint);
            float lastLocX = locx[0];
            CharacterData charData = (CharacterData)characterData.get(codepoint[0]);
            if (charData == null) {
               throw new FaultyImplementationError();
            }

            STBTruetype.stbtt_GetPackedQuad(charData.chardata, 1024, 1024, 0, locx, locy, quad, true);
            if (ix < text.length()) {
               int currentCodepoint = codepoint[0];
               getCodepoint(text, ix, codepoint);
               locx[0] += STBTruetype.stbtt_GetCodepointKernAdvance(info, currentCodepoint, codepoint[0]) * scale;
            }

            TextDrawing.RenderedGlyph first = renderFancy(charData.bitmap, blocks, quad, 0);
            TextDrawing.RenderedGlyph second = renderFancy(charData.bitmap, blocks, quad, 1);
            TextDrawing.RenderedGlyph renderedGlyph = first;
            if (second.error < first.error) {
               renderedGlyph = second;
            }

            int x0 = (int)quad.x0();
            int y0 = (int)quad.y0();
            int width = (int)Math.ceil((quad.x1() - quad.x0()) / 2.0F);
            int height = (int)Math.ceil(((int)quad.y1() - quad.y0()) / 2.0F);
            if (lastLocX < locx[0] && x0 / 2 <= lastXEnd + 1) {
               x0 += 2;
               locx[0] += 2.0F;
            }

            for (int x = 0; x < width; x++) {
               for (int y = 0; y < height; y++) {
                  BlockState block = renderedGlyph.blockStates()[x + y * width];
                  if (block != null) {
                     switch (direction) {
                        case 1:
                           chunkedBlockRegion.addBlock(0, -(y + y0 / 2), x + x0 / 2, BlockHelper.rotateY(block, Rotation.CLOCKWISE_90));
                           break;
                        case 2:
                           chunkedBlockRegion.addBlock(-(x + x0 / 2), -(y + y0 / 2), 0, BlockHelper.rotateY(block, Rotation.CLOCKWISE_180));
                           break;
                        case 3:
                           chunkedBlockRegion.addBlock(0, -(y + y0 / 2), -(x + x0 / 2), BlockHelper.rotateY(block, Rotation.COUNTERCLOCKWISE_90));
                           break;
                        default:
                           chunkedBlockRegion.addBlock(x + x0 / 2, -(y + y0 / 2), 0, block);
                     }

                     lastXEnd = x + x0 / 2;
                  }
               }
            }
         }

         quad.free();
         info.free();
         ObjectIterator var32 = characterData.values().iterator();

         while (var32.hasNext()) {
            CharacterData value = (CharacterData)var32.next();
            value.chardata.free();
         }
      }
   }

   private static TextDrawing.RenderedGlyph renderFancy(ByteBuffer bitmap, HDVoxelMap.HDVoxelBaseBlocks blocks, STBTTAlignedQuad quad, int xOffset) {
      int x0 = (int)quad.x0() + xOffset;
      int y0 = (int)quad.y0();
      int width = (int)Math.ceil((quad.x1() - quad.x0()) / 2.0F);
      int height = (int)Math.ceil(((int)quad.y1() - quad.y0()) / 2.0F);
      float[] errors = new float[width * height * 4];
      boolean[] pixels = new boolean[width * height * 4];

      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            float one = lookup(bitmap, quad, x * 2 + x0, y * 2 + y0);
            float two = lookup(bitmap, quad, x * 2 + x0 + 1, y * 2 + y0);
            float three = lookup(bitmap, quad, x * 2 + x0, y * 2 + y0 + 1);
            float four = lookup(bitmap, quad, x * 2 + x0 + 1, y * 2 + y0 + 1);
            if (one > 0.5) {
               one = 1.0F;
            }

            if (two > 0.5) {
               two = 1.0F;
            }

            if (three > 0.5) {
               three = 1.0F;
            }

            if (four > 0.5) {
               four = 1.0F;
            }

            float lowestError = one * one + two * two + three * three + four * four;
            int lowestErrorPixels = 0;
            float fullBlockError = (1.0F - one) * (1.0F - one) + (1.0F - two) * (1.0F - two) + (1.0F - three) * (1.0F - three) + (1.0F - four) * (1.0F - four);
            if (fullBlockError < lowestError) {
               lowestError = fullBlockError;
               lowestErrorPixels = 15;
            }

            float topSlabError = (1.0F - one) * (1.0F - one) + (1.0F - two) * (1.0F - two) + three * three + four * four;
            if (topSlabError < lowestError) {
               lowestError = topSlabError;
               lowestErrorPixels = 12;
            }

            float bottomSlabError = (1.0F - three) * (1.0F - three) + (1.0F - four) * (1.0F - four) + one * one + two * two;
            if (bottomSlabError < lowestError) {
               lowestError = bottomSlabError;
               lowestErrorPixels = 3;
            }

            float topWestStairError = (1.0F - one) * (1.0F - one) + (1.0F - two) * (1.0F - two) + (1.0F - three) * (1.0F - three) + four * four;
            if (topWestStairError < lowestError) {
               lowestError = topWestStairError;
               lowestErrorPixels = 14;
            }

            float botWestStairError = (1.0F - one) * (1.0F - one) + (1.0F - three) * (1.0F - three) + (1.0F - four) * (1.0F - four) + two * two;
            if (botWestStairError < lowestError) {
               lowestError = botWestStairError;
               lowestErrorPixels = 11;
            }

            float topEastStairError = (1.0F - one) * (1.0F - one) + (1.0F - two) * (1.0F - two) + (1.0F - four) * (1.0F - four) + three * three;
            if (topEastStairError < lowestError) {
               lowestError = topEastStairError;
               lowestErrorPixels = 13;
            }

            float botEastStairError = (1.0F - two) * (1.0F - two) + (1.0F - three) * (1.0F - three) + (1.0F - four) * (1.0F - four) + one * one;
            if (botEastStairError < lowestError) {
               lowestErrorPixels = 7;
            }

            int oneIdx = x * 2 + y * 2 * width * 2;
            if ((lowestErrorPixels & 8) != 0) {
               pixels[oneIdx] = true;
               errors[oneIdx] = 1.0F - one;
            } else {
               errors[oneIdx] = one;
            }

            int twoIdx = x * 2 + 1 + y * 2 * width * 2;
            if ((lowestErrorPixels & 4) != 0) {
               pixels[twoIdx] = true;
               errors[twoIdx] = 1.0F - two;
            } else {
               errors[twoIdx] = two;
            }

            int threeIdx = x * 2 + (y * 2 + 1) * width * 2;
            if ((lowestErrorPixels & 2) != 0) {
               pixels[threeIdx] = true;
               errors[threeIdx] = 1.0F - three;
            } else {
               errors[threeIdx] = three;
            }

            int fourIdx = x * 2 + 1 + (y * 2 + 1) * width * 2;
            if ((lowestErrorPixels & 1) != 0) {
               pixels[fourIdx] = true;
               errors[fourIdx] = 1.0F - four;
            } else {
               errors[fourIdx] = four;
            }
         }
      }

      for (int x = 0; x < width * 2; x++) {
         for (int y = 2; y < height * 2 - 2; y += 2) {
            int otherSideOffsetX = 1 - x % 2 * 2;
            boolean top = pixels[x + y * width * 2];
            boolean bottom = pixels[x + (y + 1) * width * 2];
            if (top != bottom
               && errors[x + y * width * 2] == 1.0F - errors[x + (y + 1) * width * 2]
               && pixels[x + otherSideOffsetX + y * width * 2]
               && pixels[x + otherSideOffsetX + (y + 1) * width * 2]) {
               boolean topEmpty = !pixels[x + otherSideOffsetX + (y - 1) * width * 2] && !pixels[x + (y - 1) * width * 2];
               boolean bottomEmpty = !pixels[x + otherSideOffsetX + (y + 2) * width * 2] && !pixels[x + (y + 2) * width * 2];
               if (topEmpty || bottomEmpty) {
                  boolean halfTop;
                  if (topEmpty && bottomEmpty) {
                     halfTop = y > height / 2;
                  } else if (topEmpty) {
                     halfTop = false;
                  } else {
                     halfTop = true;
                  }

                  if (halfTop != top) {
                     pixels[x + y * width * 2] = !top;
                     errors[x + y * width * 2] = 1.0F - errors[x + y * width * 2];
                     pixels[x + (y + 1) * width * 2] = !bottom;
                     errors[x + (y + 1) * width * 2] = 1.0F - errors[x + (y + 1) * width * 2];
                  }
               }
            }
         }
      }

      for (int x = 0; x < width * 2; x++) {
         for (int yx = 0; yx < height * 2 - 2; yx++) {
            if ((x > 0 && pixels[x - 1 + (yx + 1) * width * 2] || x < width * 2 - 1 && pixels[x + 1 + (yx + 1) * width * 2])
               && pixels[x + yx * width * 2]
               && !pixels[x + (yx + 1) * width * 2]
               && pixels[x + (yx + 2) * width * 2]
               && (errors[x + yx * width * 2] >= 0.5 || errors[x + (yx + 2) * width * 2] >= 0.5)) {
               pixels[x + (yx + 1) * width * 2] = true;
               errors[x + (yx + 1) * width * 2] = 1.0F - errors[x + (yx + 1) * width * 2];
            }
         }
      }

      BlockState[] blockArray = new BlockState[width * height];
      float totalError = 0.0F;

      for (int x = 0; x < width; x++) {
         for (int yxx = 0; yxx < height; yxx++) {
            totalError += errors[x * 2 + yxx * 2 * width * 2] * errors[x * 2 + yxx * 2 * width * 2];
            totalError += errors[x * 2 + 1 + yxx * 2 * width * 2] * errors[x * 2 + 1 + yxx * 2 * width * 2];
            totalError += errors[x * 2 + (yxx * 2 + 1) * width * 2] * errors[x * 2 + (yxx * 2 + 1) * width * 2];
            totalError += errors[x * 2 + 1 + (yxx * 2 + 1) * width * 2] * errors[x * 2 + 1 + (yxx * 2 + 1) * width * 2];
            int b = 0;
            if (pixels[x * 2 + yxx * 2 * width * 2]) {
               b |= 8;
            }

            if (pixels[x * 2 + 1 + yxx * 2 * width * 2]) {
               b |= 4;
            }

            if (pixels[x * 2 + (yxx * 2 + 1) * width * 2]) {
               b |= 2;
            }

            if (pixels[x * 2 + 1 + (yxx * 2 + 1) * width * 2]) {
               b |= 1;
            }
            blockArray[x + yxx * width] = switch (b) {
               case 0 -> null;
               default -> throw new FaultyImplementationError("Unknown: " + b);
               case 3 -> (BlockState)blocks.slab().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
               case 7 -> (BlockState)((BlockState)blocks.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST))
                  .setValue(StairBlock.HALF, Half.BOTTOM);
               case 11 -> (BlockState)((BlockState)blocks.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.WEST))
                  .setValue(StairBlock.HALF, Half.BOTTOM);
               case 12 -> (BlockState)blocks.slab().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
               case 13 -> (BlockState)((BlockState)blocks.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST))
                  .setValue(StairBlock.HALF, Half.TOP);
               case 14 -> (BlockState)((BlockState)blocks.stair().defaultBlockState().setValue(StairBlock.FACING, Direction.WEST))
                  .setValue(StairBlock.HALF, Half.TOP);
               case 15 -> blocks.full().defaultBlockState();
            };
         }
      }

      return new TextDrawing.RenderedGlyph(totalError, blockArray);
   }

   private static float lookup(ByteBuffer bitmap, STBTTAlignedQuad quad, int x, int y) {
      if (x >= quad.x1()) {
         return 0.0F;
      } else if (y >= quad.y1()) {
         return 0.0F;
      } else if (x < quad.x0()) {
         return 0.0F;
      } else if (y < quad.y0()) {
         return 0.0F;
      } else {
         float sf = (quad.x1() - x) / (quad.x1() - quad.x0());
         float s = (quad.s0() * sf + quad.s1() * (1.0F - sf)) * 1023.0F;
         float tf = (quad.y1() - y) / (quad.y1() - quad.y0());
         float t = (quad.t0() * tf + quad.t1() * (1.0F - tf)) * 1023.0F;
         float sFrac = s - (float)Math.floor(s);
         float tFrac = t - (float)Math.floor(t);
         float total = 0.0F;
         total += (bitmap.get((int)Math.floor(s) + (int)Math.floor(t) * 1024) & 255) * (1.0F - sFrac) * (1.0F - tFrac);
         total += (bitmap.get((int)Math.floor(s) + (int)Math.ceil(t) * 1024) & 255) * (1.0F - sFrac) * tFrac;
         total += (bitmap.get((int)Math.ceil(s) + (int)Math.floor(t) * 1024) & 255) * sFrac * (1.0F - tFrac);
         total += (bitmap.get((int)Math.ceil(s) + (int)Math.ceil(t) * 1024) & 255) * sFrac * tFrac;
         return total / 255.0F;
      }
   }

   private static int getCodepoint(String text, int i, int[] out) {
      char c1 = text.charAt(i);
      if (Character.isHighSurrogate(c1) && i + 1 < text.length()) {
         char c2 = text.charAt(i + 1);
         if (Character.isLowSurrogate(c2)) {
            out[0] = Character.toCodePoint(c1, c2);
            return 2;
         }
      }

      out[0] = c1;
      return 1;
   }

   private record RenderedGlyph(float error, BlockState[] blockStates) {
   }
}
