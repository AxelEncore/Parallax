package com.moulberry.axiom.tools;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.utils.AutoCleaningDynamicTexture;
import com.moulberry.axiom.utils.ColourUtils;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiListClipper;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.callback.ImListClipperCallback;
import imgui.moulberry92.type.ImString;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.Nullable;

public class ServerHeightmaps {
   private static String lastSearch = "";
   private static final ImString search = ImGuiHelper.createResizableString(32);
   private static final List<ServerHeightmaps.NamedImage> allHeightmaps = new ArrayList<>();
   private static List<ServerHeightmaps.NamedImage> filteredHeightmaps = new ArrayList<>();
   private static boolean addedNewHeightmap = false;
   private static final int ICON_SIZE = 100;

   public static void clear() {
      allHeightmaps.clear();
      filteredHeightmaps.clear();
      search.clear();
      lastSearch = "";
   }

   public static boolean hasHeightmaps() {
      return !allHeightmaps.isEmpty();
   }

   public static void add(String name, BufferedImage image) {
      name = name.trim();
      if (!name.isEmpty()) {
         String lowerName = name.toLowerCase(Locale.ROOT);
         allHeightmaps.removeIf(namedImage -> namedImage.name.toLowerCase(Locale.ROOT).equals(lowerName));
         allHeightmaps.add(new ServerHeightmaps.NamedImage(name, image));
         addedNewHeightmap = true;
      }
   }

   public static void openHeightmapModal() {
      ImGui.openPopup("Server Heightmaps##ChooseServerHeightmap");
   }

   @Nullable
   public static BufferedImage showHeightmapModalIfOpen() {
      final AtomicReference<BufferedImage> result = new AtomicReference<>();
      ImGui.setNextWindowSizeConstraints(510.0F, 350.0F, 5000.0F, 3000.0F);
      if (ImGuiHelper.beginPopupModalCloseable("Server Heightmaps##ChooseServerHeightmap")) {
         ImGui.inputText("Search", search);
         String newSearch = ImGuiHelper.getString(search).toLowerCase(Locale.ROOT);
         if (newSearch.isBlank()) {
            lastSearch = newSearch;
            filteredHeightmaps = allHeightmaps;
         } else if (!newSearch.equals(lastSearch) || addedNewHeightmap) {
            lastSearch = newSearch;
            filteredHeightmaps = new ArrayList<>();
            List<ServerHeightmaps.NamedImage> contains = new ArrayList<>();

            for (ServerHeightmaps.NamedImage namedImage : allHeightmaps) {
               String lowerName = namedImage.name.toLowerCase(Locale.ROOT);
               if (lowerName.startsWith(newSearch)) {
                  filteredHeightmaps.add(namedImage);
               } else if (lowerName.contains(newSearch)) {
                  contains.add(namedImage);
               }
            }

            filteredHeightmaps.addAll(contains);
         }

         addedNewHeightmap = false;
         if (ImGui.beginChild("Browser")) {
            final int countPerRow = Math.max(
               1, (int)Math.floor((ImGui.getContentRegionAvailX() + ImGui.getStyle().getItemSpacingX() + 2.0F) / (100.0F + ImGui.getStyle().getItemSpacingX()))
            );
            float itemHeightWithSpacing = 100.0F + ImGui.getTextLineHeightWithSpacing() * 2.0F + 7.0F + ImGui.getStyle().getItemSpacingY();
            int vcount = (int)Math.ceil((float)filteredHeightmaps.size() / countPerRow);
            ImGuiListClipper.forEach(vcount, (int)itemHeightWithSpacing, new ImListClipperCallback() {
               public void accept(int index) {
                  int from = index * countPerRow;
                  int to = Math.min(from + countPerRow, ServerHeightmaps.filteredHeightmaps.size());

                  for (int i = from; i < to; i++) {
                     ServerHeightmaps.NamedImage namedImagex = ServerHeightmaps.filteredHeightmaps.get(i);
                     ImGui.pushID(i);
                     if (ServerHeightmaps.renderItem(namedImagex, i == to - 1)) {
                        result.set(namedImagex.bufferedImage);
                     }

                     ImGui.popID();
                     if (i < to - 1) {
                        ImGui.sameLine();
                     }
                  }
               }
            });
         }

         ImGui.endChild();
         if (result.get() != null) {
            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }

      return result.get();
   }

   private static boolean renderItem(ServerHeightmaps.NamedImage namedImage, boolean last) {
      String textToRender = namedImage.name.trim();
      if (textToRender.isBlank()) {
         textToRender = "Unnamed Heightmap";
      }

      ImVec2 textSize = new ImVec2();
      ImGui.calcTextSize(textSize, textToRender, 100.0F);
      float childHeight = 100.0F + ImGui.getTextLineHeightWithSpacing() * 2.0F + 7.0F;
      boolean opened = false;
      if (ImGui.beginChild("", 100.0F, childHeight, false, 59)) {
         ImGuiHelper.pushStyleVar(11, 0.0F, 0.0F);
         ImGui.pushStyleVar(11, 2.0F, 2.0F);
         opened = ImGui.imageButton("##Heightmap", namedImage.texture.getId(), 96.0F, 96.0F, 0.0F, 0.0F, 1.0F, 1.0F);
         ImGui.popStyleVar();
         ImVec2 cursorPos = ImGui.getCursorPos();
         ImGui.setCursorPos(cursorPos.x + (100.0F - textSize.x) / 2.0F, cursorPos.y);
         ImGui.pushTextWrapPos(100.0F);
         ImGui.text(textToRender);
         ImGui.popTextWrapPos();
         if (textSize.y > ImGui.getTextLineHeight() + ImGui.getTextLineHeightWithSpacing() && ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.pushTextWrapPos(ImGui.getFontSize() * 20.0F);
            ImGui.textUnformatted(textToRender);
            ImGui.popTextWrapPos();
            ImGui.endTooltip();
         }

         ImGuiHelper.popStyleVar();
      }

      ImGui.endChild();
      return opened;
   }

   private record NamedImage(String name, BufferedImage bufferedImage, AutoCleaningDynamicTexture texture) {
      public NamedImage(String name, BufferedImage bufferedImage) {
         this(name, bufferedImage, createFromBufferedImage(bufferedImage));
      }

      private static AutoCleaningDynamicTexture createFromBufferedImage(BufferedImage bufferedImage) {
         int width = bufferedImage.getWidth();
         int height = bufferedImage.getHeight();
         NativeImage nativeImage = new NativeImage(width, height, true);

         for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
               int argb = bufferedImage.getRGB(x, y);
               nativeImage.setPixelRGBA(x, y, ColourUtils.argbToAbgr(argb));
            }
         }

         return new AutoCleaningDynamicTexture(nativeImage);
      }
   }
}
