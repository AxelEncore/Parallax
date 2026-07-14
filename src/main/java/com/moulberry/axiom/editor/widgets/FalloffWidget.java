package com.moulberry.axiom.editor.widgets;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.funcinterfaces.BiIntConsumer;
import com.moulberry.axiom.rasterization.Rasterization2D;
import com.moulberry.axiom.utils.ColourUtils;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector2i;

public class FalloffWidget {
   private static final FloatUnaryOperator[] falloffFunctions = new FloatUnaryOperator[]{
      x -> 1.0F,
      x -> (float)Math.sqrt(1.0F - x * x),
      x -> 1.0F - x,
      x -> 1.0F - x * x * x * x,
      x -> x < 0.5 ? 1.0F - 2.0F * x * x : (float)Math.pow(-2.0F * x + 2.0F, 2.0) / 2.0F,
      x -> (1.0F - x) * (1.0F - x) * (1.0F - x)
   };
   private final DynamicTexture[] falloffDisplay = new DynamicTexture[6];
   private int falloff = 1;

   public FloatUnaryOperator getFalloffFunction() {
      return falloffFunctions[this.falloff];
   }

   public void unload() {
      for (int i = 0; i < 6; i++) {
         DynamicTexture falloffDisplay = this.falloffDisplay[i];
         if (falloffDisplay != null) {
            falloffDisplay.close();
            this.falloffDisplay[i] = null;
         }
      }
   }

   public boolean displayImgui() {
      int sizeH = (int)((256.0F * EditorUI.getUiScale() - ImGui.getStyle().getItemSpacingX() * 2.0F - ImGui.getStyle().getFramePaddingX() * 6.0F) / 3.0F);
      int sizeV = (int)(37.0F * EditorUI.getUiScale());
      this.uploadFalloffPreviews(sizeH, sizeV);
      boolean changed = false;

      for (int i = 0; i < 6; i++) {
         ImGui.pushID(i);
         boolean selected = this.falloff == i;
         if (selected) {
            ImGuiHelper.pushStyleColor(21, ImGui.getColorU32(23));
         }

         if (ImGui.imageButton("##FalloffDisplay", new AxiomGpuTexture(this.falloffDisplay[i].getId()).glId(), sizeH, sizeV) && !selected) {
            this.falloff = i;
            changed = true;
         }

         if (selected) {
            ImGuiHelper.popStyleColor();
         }

         if (i % 3 != 2) {
            ImGui.sameLine();
         }

         ImGui.popID();
      }

      return changed;
   }

   public void writeSettings(CompoundTag tag) {
      tag.putInt("FalloffType", this.falloff);
   }

   public void loadSettings(CompoundTag tag) {
      this.falloff = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "FalloffType", 1);
   }

   private void uploadFalloffPreviews(int width, int height) {
      for (int i = 0; i < 6; i++) {
         DynamicTexture dynamicTexture = this.falloffDisplay[i];
         NativeImage pixels;
         if (dynamicTexture == null) {
            dynamicTexture = VersionUtilsClient.helperCreateNewWithSizeDynamicTexture((String)null, width, height, true);
            pixels = dynamicTexture.getPixels();
            this.falloffDisplay[i] = dynamicTexture;
         } else {
            NativeImage oldPixels = dynamicTexture.getPixels();
            if (oldPixels != null && oldPixels.getWidth() == width && oldPixels.getHeight() == height) {
               continue;
            }

            dynamicTexture.close();
            dynamicTexture = VersionUtilsClient.helperCreateNewWithSizeDynamicTexture((String)null, width, height, true);
            pixels = dynamicTexture.getPixels();
            this.falloffDisplay[i] = dynamicTexture;
         }

         if (pixels != null) {
            FloatUnaryOperator operator = falloffFunctions[i];
            int windowBg = ImGui.getColorU32(2);

            for (int x = 0; x < width; x++) {
               for (int y = 0; y < height; y++) {
                  pixels.setPixelRGBA(x, y, ColourUtils.argbToAbgr(ColourUtils.abgrToArgb(windowBg)));
               }
            }

            BiIntConsumer consumer = (xx, y) -> {
               for (int xo = -1; xo <= 1; xo++) {
                  for (int yo = -1; yo <= 1; yo++) {
                     int radiusSq = xo * xo + yo * yo;
                     if (radiusSq <= 1) {
                        int nx = xx + xo;
                        int ny = y + yo;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                           int existing = ColourUtils.abgrToArgb(pixels.getPixelRGBA(nx, ny));
                           int alpha = 255 - radiusSq * 255 / 2;
                           int result = ColourUtils.blendARGB(existing, alpha << 24 | 16777215);
                           pixels.setPixelRGBA(nx, ny, ColourUtils.argbToAbgr(result));
                        }
                     }
                  }
               }
            };
            float lastH = (1.0F - operator.apply(1.0F)) * (height - 3) + 1.0F;

            for (int x = 2; x < width - 1; x++) {
               float xf = (float)(x - 1) / (width - 3);
               float h = (1.0F - operator.apply(Math.abs(2.0F * xf - 1.0F))) * (height - 3) + 1.0F;
               if (x == 2) {
                  Rasterization2D.bresenham(new Vector2i(x - 1, Math.round(lastH)), new Vector2i(x, Math.round(h)), consumer, false);
               } else {
                  Rasterization2D.bresenhamSkipFrom(new Vector2i(x - 1, Math.round(lastH)), new Vector2i(x, Math.round(h)), consumer, false);
               }

               lastH = h;
            }

            dynamicTexture.upload();
         }
      }
   }
}
