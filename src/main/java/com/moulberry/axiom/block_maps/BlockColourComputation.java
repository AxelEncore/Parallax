package com.moulberry.axiom.block_maps;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.utils.OkLabColourUtils;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class BlockColourComputation {
   private static final Set<String> ERRORED_MODS = new HashSet<>();
   private static final Direction[] MODEL_DIRECTIONS = new Direction[]{
      Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN, null
   };

   public static List<BlockColourComputation.BlockColourResult> calculateColours(Collection<BlockState> blockStates) {
      Map<Object, Object2IntOpenHashMap<BlockState>> spriteTargetMap = new LinkedHashMap<>();
      Map<Object, ForkJoinTask<double[]>> calculateLabTasks = new LinkedHashMap<>();
      Set<BlockState> ignore = new HashSet<>();

      for (BlockState blockState : blockStates) {
         extractSpriteTargets(blockState, spriteTargetMap, calculateLabTasks, ignore);
      }

      Map<BlockState, BlockColourComputation.BlockInformation> infoForBlockState = new LinkedHashMap<>();

      for (Entry<Object, ForkJoinTask<double[]>> taskEntry : calculateLabTasks.entrySet()) {
         Object sprite = taskEntry.getKey();
         double[] lab = taskEntry.getValue().join();
         if (lab != null && !(lab[3] <= 0.0)) {
            ObjectIterator information = spriteTargetMap.get(sprite).object2IntEntrySet().iterator();

            while (information.hasNext()) {
               it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<BlockState> spriteEntry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<BlockState>)information.next();
               BlockState blockState = (BlockState)spriteEntry.getKey();
               int countMultiplier = spriteEntry.getIntValue();
               BlockColourComputation.BlockInformation info = infoForBlockState.computeIfAbsent(blockState, k -> new BlockColourComputation.BlockInformation());
               info.l = info.l + lab[0] * countMultiplier;
               info.a = info.a + lab[1] * countMultiplier;
               info.b = info.b + lab[2] * countMultiplier;
               info.alpha = info.alpha + lab[3] * countMultiplier;
               if (sprite instanceof SpriteContents spriteContents) {
                  info.sprites.add(spriteContents);
               } else {
                  if (!(sprite instanceof BlockColourComputation.SpriteWithTint spriteWithTint)) {
                     throw new FaultyImplementationError();
                  }

                  info.sprites.add(spriteWithTint.spriteContents());
               }
            }
         }
      }

      List<BlockColourComputation.BlockColourResult> results = new ArrayList<>();

      for (Entry<BlockState, BlockColourComputation.BlockInformation> entry : infoForBlockState.entrySet()) {
         BlockState blockState = entry.getKey();
         BlockColourComputation.BlockInformation information = entry.getValue();
         if (!ignore.contains(blockState) && !(information.alpha < 1.0)) {
            results.add(
               new BlockColourComputation.BlockColourResult(
                  blockState,
                  information.l / information.alpha,
                  information.a / information.alpha,
                  information.b / information.alpha,
                  information.sprites.size()
               )
            );
         }
      }

      return results;
   }

   private static void extractSpriteTargets(
      BlockState blockState,
      Map<Object, Object2IntOpenHashMap<BlockState>> spriteTargetMap,
      Map<Object, ForkJoinTask<double[]>> calculateLabTasks,
      Set<BlockState> ignore
   ) {
      try {
         RandomSource rand = RandomSource.create(42L);
         BakedModel bakedModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
         List<BakedQuad> quads = new ArrayList<>();

         for (Direction direction : MODEL_DIRECTIONS) {
            quads.addAll(bakedModel.getQuads(blockState, direction, rand));
         }

         Int2IntOpenHashMap cachedTints = new Int2IntOpenHashMap();

         for (BakedQuad quad : quads) {
            if (quad != null) {
               TextureAtlasSprite sprite = quad.getSprite();
               if (sprite != null) {
                  int tintColour = 16777215;
                  boolean isTinted = quad.isTinted();
                  if (isTinted) {
                     try {
                        int tintIndex = quad.getTintIndex();
                        if (cachedTints.containsKey(tintIndex)) {
                           tintColour = cachedTints.get(tintIndex);
                        } else {
                           tintColour = Minecraft.getInstance().getBlockColors().getColor(blockState, null, null, tintIndex);
                           cachedTints.put(tintIndex, tintColour);
                        }
                     } catch (Exception var17) {
                        ResourceLocation location = blockState.getBlock().builtInRegistryHolder().key().location();
                        String mod = location.getNamespace();
                        if (!mod.equals("minecraft") && ERRORED_MODS.add(mod)) {
                           Axiom.LOGGER
                              .warn(
                                 "Mod {} threw an exception when asked for the color of {}. This is usually because the mod doesn't correctly handle null values for BlockAndTintGetter/BlockPos. The block has been excluded from the colour-related features of Axiom",
                                 new Object[]{mod, location, var17}
                              );
                        }

                        ignore.add(blockState);
                        return;
                     }
                  }

                  tintColour &= 16777215;
                  SpriteContents spriteContents = sprite.contents();
                  NativeImage image = spriteContents.byMipLevel[0];
                  if (tintColour == 16777215) {
                     calculateLabTasks.put(spriteContents, ForkJoinPool.commonPool().submit(new BlockColourComputation.CalculateLabTask(image)));
                     Object2IntOpenHashMap<BlockState> count = spriteTargetMap.computeIfAbsent(spriteContents, k -> new Object2IntOpenHashMap());
                     count.put(blockState, count.getOrDefault(blockState, 0) + 1);
                  } else {
                     BlockColourComputation.SpriteWithTint spriteWithTint = new BlockColourComputation.SpriteWithTint(spriteContents, tintColour);
                     calculateLabTasks.put(
                        spriteWithTint, ForkJoinPool.commonPool().submit(new BlockColourComputation.CalculateLabWithTintTask(image, tintColour))
                     );
                     Object2IntOpenHashMap<BlockState> count = spriteTargetMap.computeIfAbsent(spriteWithTint, k -> new Object2IntOpenHashMap());
                     count.put(blockState, count.getOrDefault(blockState, 0) + 1);
                  }
               }
            }
         }
      } catch (Exception var18) {
         ResourceLocation location = blockState.getBlock().builtInRegistryHolder().key().location();
         String mod = location.getNamespace();
         if (!mod.equals("minecraft") && ERRORED_MODS.add(mod)) {
            Axiom.LOGGER
               .warn(
                  "Mod {} threw an exception when trying to calculate the colour of {}. The block has been excluded from the colour-related features of Axiom",
                  new Object[]{mod, location, var18}
               );
         }

         ignore.add(blockState);
      }
   }

   public record BlockColourResult(BlockState blockState, double l, double a, double b, int numTextures) {
   }

   private static final class BlockInformation {
      private double l = 0.0;
      private double a = 0.0;
      private double b = 0.0;
      private double alpha = 0.0;
      private final Set<SpriteContents> sprites = new HashSet<>();
   }

   private static final class CalculateLabTask implements Callable<double[]> {
      private final NativeImage image;

      private CalculateLabTask(NativeImage image) {
         this.image = image;
      }

      public double[] call() {
         double[] lab = new double[4];
         double totalL = 0.0;
         double totalA = 0.0;
         double totalB = 0.0;
         double totalAlpha = 0.0;
         int width = this.image.getWidth();
         int height = this.image.getHeight();
         int lastArgb = 0;

         for (int u = 0; u < width; u++) {
            for (int v = 0; v < height; v++) {
               int argb = ColourUtils.abgrToArgb(this.image.getPixelRGBA(u, v));
               int alpha = argb >> 24 & 0xFF;
               if (alpha != 0) {
                  if (argb != lastArgb) {
                     lastArgb = argb;
                     int red = argb >> 16 & 0xFF;
                     int green = argb >> 8 & 0xFF;
                     int blue = argb & 0xFF;
                     OkLabColourUtils.rgb2lab(red, green, blue, lab);
                  }

                  float scale = alpha / 255.0F;
                  totalL += lab[0] * scale;
                  totalA += lab[1] * scale;
                  totalB += lab[2] * scale;
                  totalAlpha += scale;
               }
            }
         }

         lab[0] = totalL;
         lab[1] = totalA;
         lab[2] = totalB;
         lab[3] = totalAlpha;
         return lab;
      }
   }

   private static final class CalculateLabWithTintTask implements Callable<double[]> {
      private final NativeImage image;
      private final int tint;

      private CalculateLabWithTintTask(NativeImage image, int tint) {
         this.image = image;
         this.tint = tint;
      }

      public double[] call() {
         double[] lab = new double[4];
         float redMult = (this.tint >> 16 & 0xFF) / 255.0F;
         float greenMult = (this.tint >> 8 & 0xFF) / 255.0F;
         float blueMult = (this.tint & 0xFF) / 255.0F;
         double totalL = 0.0;
         double totalA = 0.0;
         double totalB = 0.0;
         double totalAlpha = 0.0;
         int width = this.image.getWidth();
         int height = this.image.getHeight();
         int lastArgb = 0;

         for (int u = 0; u < width; u++) {
            for (int v = 0; v < height; v++) {
               int argb = ColourUtils.abgrToArgb(this.image.getPixelRGBA(u, v));
               int alpha = argb >> 24 & 0xFF;
               if (alpha != 0) {
                  if (argb != lastArgb) {
                     lastArgb = argb;
                     int red = (int)((argb >> 16 & 0xFF) * redMult);
                     int green = (int)((argb >> 8 & 0xFF) * greenMult);
                     int blue = (int)((argb & 0xFF) * blueMult);
                     OkLabColourUtils.rgb2lab(red, green, blue, lab);
                  }

                  float scale = alpha / 255.0F;
                  totalL += lab[0] * scale;
                  totalA += lab[1] * scale;
                  totalB += lab[2] * scale;
                  totalAlpha += scale;
               }
            }
         }

         lab[0] = totalL;
         lab[1] = totalA;
         lab[2] = totalB;
         lab[3] = totalAlpha;
         return lab;
      }
   }

   private record SpriteWithTint(SpriteContents spriteContents, int tint) {
   }
}
