package com.moulberry.axiom;

import com.mojang.blaze3d.platform.Window;
import com.moulberry.axiom.utils.RenderHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class Toasts {
   private static final List<Toasts.RenderingToast> renderingToasts = new ArrayList<>();

   public static void addToast(Toasts.Toast toast) {
      if (toast.toastId >= 0) {
         for (Toasts.RenderingToast renderingToast : renderingToasts) {
            if (renderingToast.toast.toastId == toast.toastId) {
               renderingToast.toast = toast;
               if (renderingToast.ticks < 10) {
                  renderingToast.ticks = 60 - renderingToast.ticks;
               } else if (renderingToast.ticks < 50) {
                  renderingToast.ticks = 50;
               }

               return;
            }
         }
      }

      renderingToasts.add(new Toasts.RenderingToast(toast, 60));
   }

   public static void tick() {
      if (!renderingToasts.isEmpty()) {
         Toasts.RenderingToast renderingToast = renderingToasts.get(0);
         int speed = Math.min(renderingToast.ticks, renderingToasts.size());
         renderingToast.ticks -= speed;
         if (renderingToast.ticks <= 0) {
            renderingToasts.remove(0);
         }
      }
   }

   public static boolean isRenderingToast() {
      return !renderingToasts.isEmpty();
   }

   public static void render(GuiGraphics guiGraphics, int scaledWidth, int scaledHeight, float tickDelta) {
      if (!renderingToasts.isEmpty()) {
         Toasts.RenderingToast toast = renderingToasts.get(0);
         Minecraft mc = Minecraft.getInstance();
         Window window = Minecraft.getInstance().getWindow();
         double otherScale = 4.0 / window.getGuiScale();
         int toastWidth = 160;
         int toastHeight = 40;
         int toastIconWidth = 32;
         int toastPadding = 8;
         int textWidth = 104;
         int x = scaledWidth - 160;
         int speed = Math.min(toast.ticks, renderingToasts.size());
         float toastAnimation = toast.ticks - tickDelta * speed;
         if (toastAnimation > 50.0F) {
            float amount = (toastAnimation - 50.0F) / 10.0F;
            amount = (float)Math.pow(amount, 4.0);
            x = Mth.lerpInt(amount, x, scaledWidth + 3);
         } else if (toastAnimation < 10.0F) {
            float amount = toastAnimation / 10.0F;
            amount = 1.0F - (float)Math.pow(1.0F - amount, 4.0);
            x = Mth.lerpInt(amount, scaledWidth + 3, x);
         }

         int y = scaledHeight - (int)(9.0 * otherScale) - 40 - 30;
         guiGraphics.fill(x, y, scaledWidth, y + 40, -1728053248);
         guiGraphics.fill(x - 2, y, x, y + 40, toast.toast.stripARGB);
         Font font = Minecraft.getInstance().font;
         List<FormattedCharSequence> split = font.split(toast.toast.text, 104);
         int lineCount = split.size();
         int textY = y + 20 - 9 * lineCount / 2;

         for (FormattedCharSequence formattedCharSequence : split) {
            guiGraphics.drawString(font, formattedCharSequence, x + 16 + 32, textY, -1, false);
            textY += 9;
         }

         Toasts.Toast actualToast = toast.toast;
         ResourceLocation icon = actualToast.icon;
         float iconTexX = actualToast.iconTexX;
         float iconTexY = actualToast.iconTexY;
         int iconTexW = actualToast.iconTexW;
         int iconTexH = actualToast.iconTexH;
         VersionUtilsClient.genericBlit(guiGraphics, Dummy.GUI_TEXTURED, icon, x + 8, y + 4, iconTexX, iconTexY, 32, 32, 16, 16, iconTexW, iconTexH, -1);
         RenderHelper.tryFlush(guiGraphics);
      }
   }

   public static final class RenderingToast {
      private Toasts.Toast toast;
      private int ticks;

      private RenderingToast(Toasts.Toast toast, int ticks) {
         this.toast = toast;
         this.ticks = ticks;
      }
   }

   public record Toast(Component text, ResourceLocation icon, int stripARGB, int toastId, int iconTexX, int iconTexY, int iconTexW, int iconTexH) {
   }
}
