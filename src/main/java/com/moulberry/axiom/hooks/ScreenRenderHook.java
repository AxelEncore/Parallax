package com.moulberry.axiom.hooks;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ContextMenuManager;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.FlightSpeedAdjustment;
import com.moulberry.axiom.KeyPressOverlay;
import com.moulberry.axiom.Toasts;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.capabilities.BuildSymmetry;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.render.BlockRenderCache;
import com.moulberry.axiom.screen.SwitchBuilderToolScreen;
import com.moulberry.axiom.utils.RenderHelper;
import com.moulberry.axiom.world_modification.Dispatcher;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

public class ScreenRenderHook {
   private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.parse("hud/hotbar_selection");
   private static final ResourceLocation HOTBAR_OFFHAND_LEFT_SPRITE = ResourceLocation.parse("hud/hotbar_offhand_left");
   private static final ResourceLocation HOTBAR_OFFHAND_RIGHT_SPRITE = ResourceLocation.parse("hud/hotbar_offhand_right");
   private static final ResourceLocation TOOL_SWAPPER_LOCATION = ResourceLocation.parse("axiom:gui/tool_swapper.png");
   private static int waitingForServerToGiveDispatchSendsTimer = 0;
   private static Component overlayText;
   private static int overlayTextTicks;

   public static void render(GuiGraphics guiGraphics, int scaledWidth, int scaledHeight, float tickDelta) {
      if (AxiomClient.isAxiomActive()) {
         Minecraft mc = Minecraft.getInstance();
         BlockRenderCache.renderTick(guiGraphics);
         if (!mc.options.hideGui) {
            if (mc.screen == null) {
               guiGraphics.pose().pushPose();
               guiGraphics.pose().translate(0.0F, 0.0F, 5000.0F);
               if (ContextMenuManager.getInstance().isActive()) {
                  ContextMenuManager.getInstance().render(guiGraphics, scaledWidth, scaledHeight, tickDelta);
               } else if (BuilderToolManager.isToolSlotActive()) {
                  BuilderToolManager.renderScreen(guiGraphics, scaledWidth, scaledHeight, tickDelta);
               } else if (FlightSpeedAdjustment.isEnabled()) {
                  FlightSpeedAdjustment.render(guiGraphics, scaledWidth, scaledHeight, tickDelta);
               } else if (BuildSymmetry.isActiveAndHasModifier() && Axiom.configuration.builderTools.showSymmetryEnabledIcon && !EditorUI.isActive()) {
                  VersionUtilsClient.genericBlit(
                     guiGraphics,
                     Dummy.GUI_TEXTURED,
                     TOOL_SWAPPER_LOCATION,
                     scaledWidth / 2 - 8,
                     scaledHeight / 2 + 24,
                     142.0F,
                     0.0F,
                     16,
                     16,
                     16,
                     16,
                     256,
                     256,
                     1090519039
                  );
                  RenderHelper.tryFlush(guiGraphics);
               }

               if (Axiom.configuration.visuals.showKeyHints) {
                  List<String> keyHints = List.of();
                  if (BuilderToolManager.isToolSlotActive()) {
                     keyHints = BuilderToolManager.getKeyHints();
                  } else if (DisplayEntityManipulator.hasActiveGizmo()) {
                     keyHints = DisplayEntityManipulator.getKeyHints();
                  } else if (MarkerEntityManipulator.hasActiveGizmo()) {
                     keyHints = MarkerEntityManipulator.getKeyHints();
                  }

                  if (!keyHints.isEmpty()) {
                     Font font = Minecraft.getInstance().font;
                     int[] widths = new int[keyHints.size()];
                     int maxWidth = 0;

                     for (int i = 0; i < keyHints.size(); i++) {
                        String keyHint = keyHints.get(i);
                        int width = font.width(keyHint);
                        if (width > maxWidth) {
                           maxWidth = width;
                        }

                        widths[i] = width;
                     }

                     Window window = Minecraft.getInstance().getWindow();
                     double otherScale = 4.0 / window.getGuiScale();
                     int hintHeight = 9 * keyHints.size();
                     int padding = 2;
                     int y = scaledHeight - (int)(9.0 * otherScale) - hintHeight - 30 - padding + 1;
                     if (Toasts.isRenderingToast()) {
                        y -= 43;
                     }

                     guiGraphics.fill(scaledWidth - maxWidth - padding * 2, y - padding, scaledWidth, y + hintHeight + padding - 1, -1728053248);

                     for (int i = 0; i < keyHints.size(); i++) {
                        String keyHint = keyHints.get(i);
                        guiGraphics.drawString(font, keyHint, scaledWidth - widths[i] - padding, y + 9 * i, -1);
                     }
                  }
               }

               guiGraphics.pose().popPose();
            }

            Toasts.render(guiGraphics, scaledWidth, scaledHeight, tickDelta);
            if (Axiom.configuration.visuals.keypressOverlay) {
               renderKeypressOverlay(guiGraphics, scaledWidth, scaledHeight, mc);
            }

            Font font = Minecraft.getInstance().font;
            if (waitingForServerToGiveDispatchSendsTimer > 10) {
               float timerWithPartial = waitingForServerToGiveDispatchSendsTimer + tickDelta;
               float alphaFactor = timerWithPartial >= 20.0F ? 1.0F : (timerWithPartial - 10.0F) / 10.0F;
               int alpha = (int)(255.0F * alphaFactor);
               guiGraphics.drawCenteredString(
                  font, "[Parallax] Waiting for server to process chunk changes...", scaledWidth / 2, scaledHeight - 90, alpha << 24 | 16777045
               );
            }

            if (overlayText != null && overlayTextTicks > 0) {
               float timerWithPartial = overlayTextTicks - tickDelta;
               float alphaFactor = timerWithPartial >= 10.0F ? 1.0F : timerWithPartial / 10.0F;
               int alpha = (int)(255.0F * alphaFactor);
               int width = font.width(overlayText);
               int x = scaledWidth / 2 - width / 2;
               int y = scaledHeight - 80;
               boolean background = Axiom.configuration.visuals.statusBackground;
               if (background) {
                  guiGraphics.fill(x - 2, y - 2, x + width + 1, y + 9 + 1, alpha / 2 << 24);
               }

               guiGraphics.drawString(font, overlayText, x, y, alpha << 24 | 16777215, !background);
            }

            RenderHelper.tryFlush(guiGraphics);
         }
      }
   }

   public static void setOverlayText(Component component) {
      overlayText = component;
      overlayTextTicks = 60;
   }

   public static void tick() {
      if (Dispatcher.exhaustedDispatchSends()) {
         if (waitingForServerToGiveDispatchSendsTimer < 30) {
            waitingForServerToGiveDispatchSendsTimer++;
         }
      } else if (waitingForServerToGiveDispatchSendsTimer > 0) {
         waitingForServerToGiveDispatchSendsTimer--;
      }

      if (overlayTextTicks > 0) {
         overlayTextTicks--;
      }
   }

   private static void renderKeypressOverlay(GuiGraphics guiGraphics, int screenWidth, int screenHeight, Minecraft mc) {
      List<KeyPressOverlay.StringWithOpacity> strings = KeyPressOverlay.getStrings(mc.font);
      if (!strings.isEmpty()) {
         Window window = Minecraft.getInstance().getWindow();
         float scale = 4.0F / (float)window.getGuiScale();
         int width = 0;

         for (KeyPressOverlay.StringWithOpacity string : strings) {
            width += string.width();
         }

         int scaledWidth = (int)Math.ceil(width * scale);
         int hotbarRight = screenWidth / 2 + 120;
         int centerX = (hotbarRight + screenWidth) / 2;
         if (centerX + scaledWidth / 2 > screenWidth) {
            centerX = screenWidth - scaledWidth / 2;
         }

         int baseY = screenHeight - 24;
         PoseStack pose = guiGraphics.pose();
         pose.pushPose();
         pose.translate(centerX, baseY, 0.0F);
         pose.scale(scale, scale, scale);
         pose.translate(-centerX, -baseY, 0.0F);
         int x = centerX - width / 2;
         int y = baseY - 9;
         guiGraphics.fill(x - 1, y - 1, x + width + 1, y + 9, -1728053248);

         for (KeyPressOverlay.StringWithOpacity string : strings) {
            String str = string.string();
            int opacity = string.opacity();
            guiGraphics.drawString(mc.font, str, x, y, 16777215 | opacity << 24);
            x += string.width();
         }

         pose.popPose();
      }
   }

   public static void renderHotbar(GuiGraphics guiGraphics, int scaledWidth, int scaledHeight, float tickDelta) {
      if (Minecraft.getInstance().cameraEntity instanceof Player player) {
         if (!(ContextMenuManager.getInstance().getActiveScreen() instanceof SwitchBuilderToolScreen)) {
            if (BuilderToolManager.isToolSlotActive() || Axiom.configuration.builderTools.showBuilderToolSlot) {
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               int mid = scaledWidth / 2;
               HumanoidArm humanoidArm = player.getMainArm();
               if (humanoidArm == HumanoidArm.LEFT) {
                  VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, mid - 91 - 29, scaledHeight - 23, 29, 24);
                  if (BuilderToolManager.isToolSlotActive()) {
                     VersionUtilsClient.genericBlitSprite(
                        guiGraphics, Dummy.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, mid - 91 - 30, scaledHeight - 23 - 1, 24, 24
                     );
                  }

                  VersionUtilsClient.blit256(
                     guiGraphics, TOOL_SWAPPER_LOCATION, mid - 91 - 26, scaledHeight - 19, 46 + 16 * BuilderToolManager.getToolSlotSelected(), 0, 16, 16
                  );
                  if (!BuilderToolManager.canUseBuilderTools()) {
                     guiGraphics.fill(mid - 91 - 26, scaledHeight - 19, mid - 91 - 26 + 16, scaledHeight - 19 + 16, -1442840576);
                  }
               } else {
                  VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, HOTBAR_OFFHAND_RIGHT_SPRITE, mid + 91, scaledHeight - 23, 29, 24);
                  if (BuilderToolManager.isToolSlotActive()) {
                     VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, mid + 91 + 6, scaledHeight - 23 - 1, 24, 24);
                  }

                  VersionUtilsClient.blit256(
                     guiGraphics, TOOL_SWAPPER_LOCATION, mid + 101, scaledHeight - 19, 46 + 16 * BuilderToolManager.getToolSlotSelected(), 0, 16, 16
                  );
                  if (!BuilderToolManager.canUseBuilderTools()) {
                     guiGraphics.fill(mid + 101, scaledHeight - 19, mid + 101 + 16, scaledHeight - 19 + 16, -1442840576);
                  }
               }
            }
         }
      }
   }
}
