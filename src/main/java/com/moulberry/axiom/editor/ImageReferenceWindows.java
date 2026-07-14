package com.moulberry.axiom.editor;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiViewport;
import imgui.moulberry92.ImVec2;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

public class ImageReferenceWindows {
   private static boolean defaultShowInGameUi = false;
   private static boolean defaultBorderless = false;
   private static final List<ImageReferenceWindows.Reference> references = new ArrayList<>();

   public static void add(NativeImage image) {
      references.add(
         new ImageReferenceWindows.Reference(
            VersionUtilsClient.helperCreateFromNativeImageDynamicTexture(null, image), 0.0F, defaultShowInGameUi, defaultBorderless
         )
      );
   }

   public static boolean hasReferenceInGameUI() {
      for (ImageReferenceWindows.Reference reference : references) {
         if (reference.showInGameUI) {
            return true;
         }
      }

      return false;
   }

   public static void render() {
      if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
         boolean isInEditor = EditorUI.isActive();
         boolean lastBorderless = false;
         boolean anyMouseDown = ImGui.isAnyMouseDown();

         for (int i = 0; i < references.size(); i++) {
            ImageReferenceWindows.Reference reference = references.get(i);
            if (isInEditor || reference.showInGameUI) {
               if (lastBorderless != reference.borderless) {
                  if (reference.borderless) {
                     ImGuiHelper.pushStyleVar(2, 0.0F, 0.0F);
                  } else {
                     ImGuiHelper.popStyleVar();
                  }

                  lastBorderless = reference.borderless;
               }

               ImGui.pushID(i);
               NativeImage nativeImage = reference.texture.getPixels();
               if (nativeImage == null) {
                  throw new IllegalArgumentException();
               }

               ImGuiViewport viewport = ImGui.getMainViewport();
               ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), 4, 0.5F, 0.5F);
               float lastWidth = reference.lastWidth;
               if (lastWidth != 0.0F) {
                  ImGui.setNextWindowSizeConstraints(
                     64.0F, lastWidth / nativeImage.getWidth() * nativeImage.getHeight(), 2048.0F, lastWidth / nativeImage.getWidth() * nativeImage.getHeight()
                  );
               }

               int flags = 257;
               if (reference.borderless) {
                  flags |= 128;
               }

               if (reference.hasCalculatedCorner && !anyMouseDown) {
                  float screenX = reference.cornerX;
                  float screenY = reference.cornerY;
                  if (EditorUI.isActive()) {
                     screenX += EditorUI.frameX + EditorUI.frameWidth * reference.pivotX;
                     screenY += EditorUI.frameY + EditorUI.frameHeight * reference.pivotY;
                  } else {
                     screenX += viewport.getSizeX() * reference.pivotX;
                     screenY += viewport.getSizeY() * reference.pivotY;
                  }

                  ImGui.setNextWindowPos(screenX, screenY, 1, reference.pivotX, reference.pivotY);
               }

               if (reference.opacity[0] < 0.99F) {
                  int col = ImGui.getColorU32(2);
                  int opacity = col >> 24 & 0xFF;
                  opacity *= (int)(255.0F * reference.opacity[0]);
                  opacity /= 255;
                  col &= 16777215;
                  col |= opacity << 24;
                  ImGuiHelper.pushStyleColor(2, col);
               }

               boolean visible = ImGui.begin("##Reference" + reference.uuid, flags);
               if (reference.opacity[0] < 0.99F) {
                  ImGuiHelper.popStyleColor();
               }

               if (visible) {
                  float windowWidth = ImGui.getWindowWidth();
                  float windowHeight = windowWidth / nativeImage.getWidth() * nativeImage.getHeight();
                  ImGui.setWindowSize(windowWidth, windowHeight);
                  ImVec2 windowPos = ImGui.getWindowPos();
                  if (EditorUI.isActive()) {
                     if (windowPos.x + windowWidth / 2.0F < EditorUI.frameX + EditorUI.frameWidth / 2.0F) {
                        reference.pivotX = 0.0F;
                     } else {
                        reference.pivotX = 1.0F;
                     }

                     if (windowPos.y + windowHeight / 2.0F < EditorUI.frameY + EditorUI.frameHeight / 2.0F) {
                        reference.pivotY = 0.0F;
                     } else {
                        reference.pivotY = 1.0F;
                     }

                     reference.cornerX = windowPos.x + windowWidth * reference.pivotX - (EditorUI.frameX + EditorUI.frameWidth * reference.pivotX);
                     reference.cornerY = windowPos.y + windowHeight * reference.pivotY - (EditorUI.frameY + EditorUI.frameHeight * reference.pivotY);
                     reference.hasCalculatedCorner = true;
                  }

                  reference.lastWidth = windowWidth;
                  float availX = ImGui.getContentRegionAvailX();
                  float availY = ImGui.getContentRegionAvailY();
                  ImGui.imageWithBg(
                     new AxiomGpuTexture(reference.texture.getId()).glId(),
                     availX,
                     availY,
                     0.0F,
                     0.0F,
                     1.0F,
                     1.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     0.0F,
                     1.0F,
                     1.0F,
                     1.0F,
                     reference.opacity[0]
                  );
                  String popupName = "##EditProperties" + reference.uuid;
                  if (ImGui.isItemClicked(1)) {
                     ImGui.openPopup(popupName);
                  }

                  if (ImGui.isPopupOpen(popupName)) {
                     boolean borderless = reference.borderless;
                     if (borderless) {
                        ImGuiHelper.popStyleVar();
                     }

                     if (ImGui.beginPopup(popupName)) {
                        if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.reference_image.close_window"))) {
                           EditorUI.deferredClose(references.remove(i).texture);
                           i--;
                        }

                        if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.reference_image.set_filtering_nearest"))) {
                           GlStateManager._bindTexture(new AxiomGpuTexture(reference.texture.getId()).glId());
                           GL11.glTexParameteri(3553, 10241, 9728);
                           GL11.glTexParameteri(3553, 10240, 9728);
                        }

                        if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.reference_image.set_filtering_linear"))) {
                           GlStateManager._bindTexture(new AxiomGpuTexture(reference.texture.getId()).glId());
                           GL11.glTexParameteri(3553, 10241, 9729);
                           GL11.glTexParameteri(3553, 10240, 9729);
                           GL11.glTexParameteri(3553, 10242, 33071);
                           GL11.glTexParameteri(3553, 10243, 33071);
                        }

                        if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.reference_image.show_in_game_ui"), reference.showInGameUI)) {
                           reference.showInGameUI = !reference.showInGameUI;
                           defaultShowInGameUi = reference.showInGameUI;
                        }

                        if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.reference_image.borderless"), reference.borderless)) {
                           reference.borderless = !reference.borderless;
                           defaultBorderless = reference.borderless;
                        }

                        ImGui.sliderFloat("Opacity", reference.opacity, 0.0F, 1.0F);
                        ImGui.endPopup();
                     }

                     if (borderless) {
                        ImGuiHelper.pushStyleVar(2, 0.0F, 0.0F);
                     }
                  }
               } else {
                  EditorUI.deferredClose(references.remove(i).texture);
                  i--;
               }

               ImGui.end();
               ImGui.popID();
            }
         }

         if (lastBorderless) {
            ImGuiHelper.popStyleVar();
         }
      }
   }

   private static final class Reference {
      private final UUID uuid = UUID.randomUUID();
      private final DynamicTexture texture;
      private float lastWidth;
      private boolean showInGameUI;
      private boolean borderless;
      private float[] opacity = new float[]{1.0F};
      private boolean hasCalculatedCorner = false;
      private float cornerX;
      private float cornerY;
      private float pivotX;
      private float pivotY;

      private Reference(DynamicTexture texture, float lastWidth, boolean showInGameUI, boolean borderless) {
         this.texture = texture;
         this.lastWidth = lastWidth;
         this.showInGameUI = showInGameUI;
         this.borderless = borderless;
      }
   }
}
