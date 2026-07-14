package com.moulberry.axiom.screen;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.hooks.CreativeModeInventoryScreenExt;
import com.moulberry.axiom.utils.AutoCleaningDynamicTexture;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.utils.OkLabColourUtils;
import com.moulberry.axiom.versioning.input_events.LegacyKeyEvent;
import com.moulberry.axiom.versioning.input_events.LegacyMouseButtonEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class CreativeColourScreen extends AbstractContainerScreen<CreativeColourScreen.ItemPickerMenu> {
   private static final SimpleContainer CONTAINER = new SimpleContainer(10);
   private static int scrollAmount = 0;
   private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("axiom", "textures/gui/creative_tab_colour_picker.png");
   private static final ResourceLocation DYNAMIC_TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath("axiom", "dynamic/creative_colour_screen");
   private static final ResourceLocation INSET_CREATIVE_COLOUR_FLAG = ResourceLocation.parse("axiom:inset_creative_colour_flag_toggle");
   private final CreativeModeInventoryScreen creativeScreen;
   private AutoCleaningDynamicTexture dynamicTexture = null;
   public static int currentFlags = BlockColourMap.FLAG_SOLID | BlockColourMap.FLAG_OPAQUE | BlockColourMap.FLAG_FULL_CUBE;
   private static float currentHue = 0.0F;
   private static float currentSaturation = 1.0F;
   private static float currentBrightness = 1.0F;
   private float lastHue = currentHue;
   private int lastScale = 1;
   private float lastSaturation = currentSaturation;
   private float lastBrightness = currentBrightness;
   private int draggingElement = -1;
   private static int DRAGGING_SATVAL = 0;
   private static int DRAGGING_LARGE_HUE = 1;
   private static int DRAGGING_RGB_OR_HSV_START = 2;
   private final EditBox[] rgbHsbInputs = new EditBox[6];
   private EditBox hexEditBox = null;
   private boolean settingEditBoxes = false;

   public CreativeColourScreen(LocalPlayer localPlayer, CreativeModeInventoryScreen creativeScreen) {
      super(new CreativeColourScreen.ItemPickerMenu(localPlayer), localPlayer.getInventory(), Component.translatable("axiom.color_picker"));
      localPlayer.containerMenu = this.menu;
      this.creativeScreen = creativeScreen;
      this.imageWidth = 195;
      this.imageHeight = 136;
   }

   protected void init() {
      if (this.minecraft != null && !this.minecraft.player.hasInfiniteMaterials()) {
         this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
      } else {
         super.init();
         this.creativeScreen.init(this.minecraft, this.width, this.height);
         CustomCreativeTabButton button = new CustomCreativeTabButton(
            this.leftPos - 36,
            this.topPos + 30,
            26,
            26,
            Component.translatable("axiom.color_picker"),
            new ItemStack(Items.YELLOW_DYE),
            () -> Minecraft.getInstance().setScreen(new CreativeColourScreen(this.minecraft.player, this.creativeScreen))
         );
         button.active = false;
         this.addRenderableWidget(button);
         if (Axiom.configuration.creativeMenu.showGradientHelper) {
            this.addRenderableWidget(
               new CustomCreativeTabButton(
                  this.leftPos - 36,
                  this.topPos + 30 + 36,
                  26,
                  26,
                  Component.translatable("axiom.gradient_helper"),
                  new ItemStack(Items.BRUSH),
                  () -> Minecraft.getInstance().setScreen(new CreativeGradientScreen(this.minecraft.player, this.creativeScreen))
               )
            );
         }

         for (int i = 0; i < 6; i++) {
            int y = this.topPos + 19 + 11 * i;
            if (i >= 3) {
               y += 6;
            }

            EditBox editBox = new EditBox(this.font, this.leftPos + 156, y, 31, 9, CommonComponents.EMPTY);
            editBox.setMaxLength(6);
            editBox.setBordered(false);
            editBox.setVisible(true);
            editBox.setTextColor(-1);
            int editBoxType = i;
            editBox.setResponder(input -> {
               if (!this.settingEditBoxes) {
                  editBox.setTextColor(-65536);

                  try {
                     float value = Float.parseFloat(input);
                     switch (editBoxType) {
                        case 0:
                           this.setRGB(Math.max(0.0F, Math.min(255.0F, value)), -1.0F, -1.0F, true, false);
                           break;
                        case 1:
                           this.setRGB(-1.0F, Math.max(0.0F, Math.min(255.0F, value)), -1.0F, true, false);
                           break;
                        case 2:
                           this.setRGB(-1.0F, -1.0F, Math.max(0.0F, Math.min(255.0F, value)), true, false);
                           break;
                        case 3:
                           this.setHSB(Math.max(0.0F, Math.min(360.0F, value)) / 360.0F, -1.0F, -1.0F, true);
                           break;
                        case 4:
                           this.setHSB(-1.0F, Math.max(0.0F, Math.min(100.0F, value)) / 100.0F, -1.0F, true);
                           break;
                        case 5:
                           this.setHSB(-1.0F, -1.0F, Math.max(0.0F, Math.min(100.0F, value) / 100.0F), true);
                     }

                     editBox.setTextColor(-1);
                  } catch (NumberFormatException var5x) {
                  }
               }
            });
            this.addRenderableWidget(editBox);
            this.rgbHsbInputs[i] = editBox;
         }

         this.hexEditBox = new EditBox(this.font, this.leftPos + 128, this.topPos + 94, 60, 9, CommonComponents.EMPTY);
         this.hexEditBox.setMaxLength(10);
         this.hexEditBox.setBordered(false);
         this.hexEditBox.setTextColor(-1);
         this.hexEditBox.setHint(Component.literal("#FF0000"));
         this.hexEditBox.setResponder(input -> {
            if (!this.settingEditBoxes) {
               this.hexEditBox.setTextColor(-65536);
               if (input.startsWith("0x")) {
                  input = input.substring(2);
               }

               String replaced = input.trim().toLowerCase(Locale.ROOT).replaceAll("[^0-9a-f]", "");
               if (!replaced.isEmpty()) {
                  if (replaced.length() > 6) {
                     replaced = replaced.substring(0, 6);
                  } else if (replaced.length() < 6) {
                     char lastChar = replaced.charAt(replaced.length() - 1);
                     StringBuilder builder = new StringBuilder(replaced);

                     while (builder.length() < 6) {
                        builder.append(lastChar);
                     }

                     replaced = builder.toString();
                  }

                  try {
                     int hex = (int)Long.parseLong(replaced, 16);
                     int red = hex >> 16 & 0xFF;
                     int green = hex >> 8 & 0xFF;
                     int blue = hex & 0xFF;
                     this.setRGB(red, green, blue, false, true);
                     this.hexEditBox.setTextColor(-1);
                  } catch (NumberFormatException var7) {
                  }
               }
            }
         });
         this.addRenderableWidget(this.hexEditBox);
         float[] rgb = new float[3];
         ColourUtils.HSBtoRGB(currentHue, currentSaturation, currentBrightness, rgb);
         this.settingEditBoxes = true;
         this.rgbHsbInputs[0].setValue(String.format("%.1f", rgb[0]));
         this.rgbHsbInputs[1].setValue(String.format("%.1f", rgb[1]));
         this.rgbHsbInputs[2].setValue(String.format("%.1f", rgb[2]));
         this.rgbHsbInputs[3].setValue(String.format("%.1f", currentHue * 360.0F));
         this.rgbHsbInputs[4].setValue(String.format("%.1f", currentSaturation * 100.0F));
         this.rgbHsbInputs[5].setValue(String.format("%.1f", currentBrightness * 100.0F));
         this.hexEditBox.setValue(String.format("#%06x", (int)rgb[0] << 16 | (int)rgb[1] << 8 | (int)rgb[2]));
         this.settingEditBoxes = false;
      }
   }

   public void resize(Minecraft minecraft, int width, int height) {
      this.init(minecraft, width, height);
   }

   public boolean charTyped(char character, int modifiers) {
      for (EditBox rgbHsbInput : this.rgbHsbInputs) {
         if (rgbHsbInput.charTyped(character, modifiers)) {
            return true;
         }
      }

      return this.hexEditBox.charTyped(character, modifiers);
   }

   public boolean keyPressed(int key, int scancode, int modifiers) {
      return this.keyPressed(new LegacyKeyEvent(key, scancode, modifiers));
   }

   public boolean keyPressed(LegacyKeyEvent keyEvent) {
      boolean hasFocused = false;

      for (EditBox rgbHsbInput : this.rgbHsbInputs) {
         if (VersionUtilsClient.legacyGuiEventListenerKeyPressed(rgbHsbInput, keyEvent)) {
            return true;
         }

         if (rgbHsbInput.isFocused() && keyEvent.key() == 256) {
            rgbHsbInput.setFocused(false);
            return true;
         }

         hasFocused |= rgbHsbInput.isFocused();
      }

      if (VersionUtilsClient.legacyGuiEventListenerKeyPressed(this.hexEditBox, keyEvent)) {
         return true;
      } else if (this.hexEditBox.isFocused() && keyEvent.key() == 256) {
         this.hexEditBox.setFocused(false);
         return true;
      } else {
         hasFocused |= this.hexEditBox.isFocused();
         if (hasFocused) {
            return true;
         } else if (VersionUtilsClient.legacyKeyMappingMatches(this.minecraft.options.keyChat, keyEvent)) {
            ((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$selectTab(CreativeModeTabs.searchTab());
            this.minecraft.setScreen(this.creativeScreen);
            return true;
         } else {
            return super.keyPressed(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers());
         }
      }
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      for (CreativeModeTab creativeModeTab : CreativeModeTabs.tabs()) {
         ((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$renderTabButton(guiGraphics, mouseX, mouseY, creativeModeTab, true);
      }

      VersionUtilsClient.genericBlit(
         guiGraphics, Dummy.GUI_TEXTURED, BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256, -1
      );
      int sliderHeight = 11;

      for (int i = 0; i < 6; i++) {
         Component letter = switch (i) {
            case 0 -> Component.translatable("axiom.color_picker.red_abbreviation");
            case 1 -> Component.translatable("axiom.color_picker.green_abbreviation");
            case 2 -> Component.translatable("axiom.color_picker.blue_abbreviation");
            case 3 -> Component.translatable("axiom.color_picker.hue_abbreviation");
            case 4 -> Component.translatable("axiom.color_picker.saturation_abbreviation");
            case 5 -> Component.translatable("axiom.color_picker.value_abbreviation");
            default -> throw new FaultyImplementationError();
         };
         int y = this.topPos + 19 + sliderHeight * i;
         if (i >= 3) {
            y += 6;
         }

         guiGraphics.drawString(this.font, letter, this.leftPos + 104, y, -12566464, false);
      }

      guiGraphics.drawString(this.font, Component.translatable("axiom.color_picker.hex"), this.leftPos + 103, this.topPos + 94, -12566464, false);
      if ((currentFlags & BlockColourMap.FLAG_FULL_CUBE) != 0) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, INSET_CREATIVE_COLOUR_FLAG, this.leftPos + 46, this.topPos + 92, 12, 12);
      }

      if ((currentFlags & BlockColourMap.FLAG_SOLID) != 0) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, INSET_CREATIVE_COLOUR_FLAG, this.leftPos + 60, this.topPos + 92, 12, 12);
      }

      if ((currentFlags & BlockColourMap.FLAG_OPAQUE) != 0) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, INSET_CREATIVE_COLOUR_FLAG, this.leftPos + 74, this.topPos + 92, 12, 12);
      }

      if ((currentFlags & BlockColourMap.FLAG_SAME_TEXTURE) != 0) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, INSET_CREATIVE_COLOUR_FLAG, this.leftPos + 88, this.topPos + 92, 12, 12);
      }

      guiGraphics.drawCenteredString(this.font, Component.translatable("axiom.color_picker.full_cube_abbreviation"), this.leftPos + 52, this.topPos + 94, -1);
      guiGraphics.drawCenteredString(this.font, Component.translatable("axiom.color_picker.solid_abbreviation"), this.leftPos + 66, this.topPos + 94, -1);
      guiGraphics.drawCenteredString(this.font, Component.translatable("axiom.color_picker.opaque_abbreviation"), this.leftPos + 80, this.topPos + 94, -1);
      guiGraphics.drawCenteredString(this.font, Component.translatable("axiom.color_picker.same_texture_abbreviation"), this.leftPos + 94, this.topPos + 94, -1);
      int scale = Math.max(1, (int)Minecraft.getInstance().getWindow().getGuiScale());
      if (this.dynamicTexture == null
         || this.lastHue != currentHue
         || this.lastSaturation != currentSaturation
         || this.lastBrightness != currentBrightness
         || this.lastScale != scale) {
         this.lastHue = currentHue;
         this.lastSaturation = currentSaturation;
         this.lastBrightness = currentBrightness;
         if (this.dynamicTexture == null) {
            this.dynamicTexture = new AutoCleaningDynamicTexture(107 * scale, 70 * scale, true);
            this.lastScale = scale;
         } else if (this.lastScale != scale) {
            this.dynamicTexture.close();
            this.dynamicTexture = new AutoCleaningDynamicTexture(107 * scale, 70 * scale, true);
            this.lastScale = scale;
         }

         NativeImage image = this.dynamicTexture.getPixels();
         int currentHueH = (int)(currentHue * (52 * scale - 1));
         float[] rgb = new float[3];

         for (int h = 0; h < 52 * scale; h++) {
            int currentHueDelta = Math.abs(currentHueH - h);
            int argb;
            if (currentHueDelta == 0) {
               argb = -16777216;
            } else if (currentHueDelta <= 1) {
               argb = -1;
            } else {
               float hue = h / (52.0F * scale - 1.0F);
               ColourUtils.HSBtoRGB(hue, 1.0F, 1.0F, rgb);
               argb = 0xFF000000 | (int)rgb[0] << 16 | (int)rgb[1] << 8 | (int)rgb[2];
            }

            for (int x = 45 * scale; x < 54 * scale; x++) {
               image.setPixelRGBA(x, h, ColourUtils.argbToAbgr(argb));
            }
         }

         int currentSaturationX = (int)(currentSaturation * (43 * scale - 1));
         int currentBrightnessY = (int)((1.0F - currentBrightness) * (52 * scale - 1));

         for (int x = 0; x < 43 * scale; x++) {
            for (int y = 0; y < 52 * scale; y++) {
               float saturation = x / (43.0F * scale - 1.0F);
               float brightness = 1.0F - y / (52.0F * scale - 1.0F);
               int currentSaturationDelta = Math.abs(currentSaturationX - x);
               int currentBrightnessDelta = Math.abs(currentBrightnessY - y);
               int currentDelta = Math.min(currentSaturationDelta, currentBrightnessDelta);
               int argb;
               if (currentDelta == 0) {
                  argb = -16777216;
               } else if (Math.abs(currentDelta) <= 1) {
                  argb = -1;
               } else {
                  ColourUtils.HSBtoRGB(currentHue, saturation, brightness, rgb);
                  argb = 0xFF000000 | (int)rgb[0] << 16 | (int)rgb[1] << 8 | (int)rgb[2];
               }

               image.setPixelRGBA(x, y, ColourUtils.argbToAbgr(argb));
            }
         }

         ColourUtils.HSBtoRGB(currentHue, currentSaturation, currentBrightness, rgb);
         int argb = 0xFF000000 | (int)rgb[0] << 16 | (int)rgb[1] << 8 | (int)rgb[2];

         for (int x = 0; x < 36 * scale; x++) {
            for (int y = 54 * scale; y < 70 * scale; y++) {
               image.setPixelRGBA(x, y, ColourUtils.argbToAbgr(argb));
            }
         }

         float currentRed = rgb[0];
         float currentGreen = rgb[1];
         float currentBlue = rgb[2];
         int currentRedX = (int)(currentRed / 255.0F * (42 * scale - 1));
         int currentGreenX = (int)(currentGreen / 255.0F * (42 * scale - 1));
         int currentBlueX = (int)(currentBlue / 255.0F * (42 * scale - 1));
         int currentHX = (int)(currentHue * (42 * scale - 1));
         int currentSX = (int)(currentSaturation * (42 * scale - 1));
         int currentBX = (int)(currentBrightness * (42 * scale - 1));

         for (int i = 0; i < 6; i++) {
            if (i < 3) {
               for (int x = 0; x < 42 * scale; x++) {
                  int currentDelta;
                  if (i == 0) {
                     currentDelta = Math.abs(currentRedX - x);
                  } else if (i == 1) {
                     currentDelta = Math.abs(currentGreenX - x);
                  } else {
                     currentDelta = Math.abs(currentBlueX - x);
                  }

                  if (currentDelta == 0) {
                     argb = -16777216;
                  } else if (Math.abs(currentDelta) <= 1) {
                     argb = -1;
                  } else {
                     float red = i == 0 ? x / (42.0F * scale - 1.0F) * 255.0F : currentRed;
                     float green = i == 1 ? x / (42.0F * scale - 1.0F) * 255.0F : currentGreen;
                     float blue = i == 2 ? x / (42.0F * scale - 1.0F) * 255.0F : currentBlue;
                     argb = 0xFF000000 | (int)red << 16 | (int)green << 8 | (int)blue;
                  }

                  for (int y = 0; y < 9 * scale; y++) {
                     image.setPixelRGBA(x + 65 * scale, sliderHeight * i * scale + y, ColourUtils.argbToAbgr(argb));
                  }
               }
            } else {
               for (int x = 0; x < 42 * scale; x++) {
                  int currentDeltax;
                  if (i == 3) {
                     currentDeltax = Math.abs(currentHX - x);
                  } else if (i == 4) {
                     currentDeltax = Math.abs(currentSX - x);
                  } else {
                     currentDeltax = Math.abs(currentBX - x);
                  }

                  if (currentDeltax == 0) {
                     argb = -16777216;
                  } else if (Math.abs(currentDeltax) <= 1) {
                     argb = -1;
                  } else {
                     float h = i == 3 ? x / (42.0F * scale - 1.0F) : currentHue;
                     float s = i == 4 ? x / (42.0F * scale - 1.0F) : currentSaturation;
                     float b = i == 5 ? x / (42.0F * scale - 1.0F) : currentBrightness;
                     ColourUtils.HSBtoRGB(h, s, b, rgb);
                     argb = 0xFF000000 | (int)rgb[0] << 16 | (int)rgb[1] << 8 | (int)rgb[2];
                  }

                  for (int y = 0; y < 9 * scale; y++) {
                     image.setPixelRGBA(x + 65 * scale, (6 + sliderHeight * i) * scale + y, ColourUtils.argbToAbgr(argb));
                  }
               }
            }
         }

         this.dynamicTexture.upload();
         this.updateNearestBlocks(true);
      }

      Minecraft.getInstance().getTextureManager().register(DYNAMIC_TEXTURE_LOCATION, this.dynamicTexture);
      VersionUtilsClient.genericBlit(
         guiGraphics,
         Dummy.GUI_TEXTURED,
         DYNAMIC_TEXTURE_LOCATION,
         this.leftPos + 46,
         this.topPos + 18,
         0.0F,
         0.0F,
         107,
         70,
         107 * scale,
         70 * scale,
         107 * scale,
         70 * scale,
         -1
      );
   }

   private void updateNearestBlocks(boolean resetScroll) {
      if (resetScroll) {
         scrollAmount = 0;
      }

      float[] rgb = new float[3];
      ColourUtils.HSBtoRGB(currentHue, currentSaturation, currentBrightness, rgb);
      double[] lab = new double[3];
      OkLabColourUtils.rgb2lab(rgb[0], rgb[1], rgb[2], lab);
      List<BlockState> blocks = BlockColourMap.getNearestLabN(lab[0], lab[1], lab[2], currentFlags, 20 + scrollAmount);
      CONTAINER.clearContent();
      int toSkip = scrollAmount;
      int index = 0;
      Set<ItemStack> addedItems = new HashSet<>();

      for (BlockState blockState : blocks) {
         ItemStack itemStack = ServerCustomBlocks.getCustomOrVanillaStateFor(blockState).getCustomBlock().axiom$asItemStack();
         if (!itemStack.isEmpty() && addedItems.add(itemStack)) {
            if (toSkip > 0) {
               toSkip--;
            } else {
               CONTAINER.setItem(index, itemStack);
               if (++index >= 10) {
                  break;
               }
            }
         }
      }
   }

   protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
      guiGraphics.drawString(this.font, this.title, 8, 6, -12566464, false);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      if (this.draggingElement == -1) {
         if (mouseX >= this.leftPos + 8 && mouseX <= this.leftPos + 44 && mouseY >= this.topPos + 16 && mouseY <= this.topPos + 16 + 90) {
            guiGraphics.pose().pushPose();
            int scale = Math.max(1, (int)Minecraft.getInstance().getWindow().getGuiScale());
            float offset = (float)Math.floor(scale / 2.0F + 0.4F) / scale;
            guiGraphics.pose().translate(offset, offset, 266.0F);

            for (int i = 0; i < 10; i++) {
               int x = 17 + i % 2 * 18;
               int y = 22 + i / 2 * 18;
               guiGraphics.drawCenteredString(this.font, "#" + (i + 1 + scrollAmount), this.leftPos + x, this.topPos + y, 1090519039);
            }

            guiGraphics.pose().popPose();
         }

         if (mouseY >= this.topPos + 92 && mouseY <= this.topPos + 104) {
            if (mouseX >= this.leftPos + 46 && mouseX <= this.leftPos + 58) {
               Component enabledOrDisabled;
               if ((currentFlags & BlockColourMap.FLAG_FULL_CUBE) != 0) {
                  enabledOrDisabled = Component.literal(AxiomI18n.get("axiom.hardcoded.br_enabled")).withStyle(ChatFormatting.GREEN);
               } else {
                  enabledOrDisabled = Component.literal(AxiomI18n.get("axiom.hardcoded.br_disabled")).withStyle(ChatFormatting.RED);
               }

               guiGraphics.renderTooltip(
                  this.font,
                  List.of(Component.literal(AxiomI18n.get("axiom.hardcoded.full_cube")).withStyle(ChatFormatting.YELLOW), Component.literal(AxiomI18n.get("axiom.hardcoded.must_full_cube")), enabledOrDisabled),
                  Optional.empty(),
                  mouseX,
                  mouseY
               );
               return;
            }

            if (mouseX >= this.leftPos + 60 && mouseX <= this.leftPos + 72) {
               Component enabledOrDisabled;
               if ((currentFlags & BlockColourMap.FLAG_SOLID) != 0) {
                  enabledOrDisabled = Component.literal(AxiomI18n.get("axiom.hardcoded.br_enabled")).withStyle(ChatFormatting.GREEN);
               } else {
                  enabledOrDisabled = Component.literal(AxiomI18n.get("axiom.hardcoded.br_disabled")).withStyle(ChatFormatting.RED);
               }

               guiGraphics.renderTooltip(
                  this.font,
                  List.of(Component.literal(AxiomI18n.get("axiom.hardcoded.solid")).withStyle(ChatFormatting.YELLOW), Component.literal(AxiomI18n.get("axiom.hardcoded.must_collidable")), enabledOrDisabled),
                  Optional.empty(),
                  mouseX,
                  mouseY
               );
               return;
            }

            if (mouseX >= this.leftPos + 74 && mouseX <= this.leftPos + 86) {
               Component enabledOrDisabled;
               if ((currentFlags & BlockColourMap.FLAG_OPAQUE) != 0) {
                  enabledOrDisabled = Component.literal(AxiomI18n.get("axiom.hardcoded.br_enabled")).withStyle(ChatFormatting.GREEN);
               } else {
                  enabledOrDisabled = Component.literal(AxiomI18n.get("axiom.hardcoded.br_disabled")).withStyle(ChatFormatting.RED);
               }

               guiGraphics.renderTooltip(
                  this.font,
                  List.of(Component.literal(AxiomI18n.get("axiom.hardcoded.opaque")).withStyle(ChatFormatting.YELLOW), Component.literal(AxiomI18n.get("axiom.hardcoded.must_not_transparent")), enabledOrDisabled),
                  Optional.empty(),
                  mouseX,
                  mouseY
               );
               return;
            }

            if (mouseX >= this.leftPos + 88 && mouseX <= this.leftPos + 100) {
               Component enabledOrDisabled;
               if ((currentFlags & BlockColourMap.FLAG_SAME_TEXTURE) != 0) {
                  enabledOrDisabled = Component.literal(AxiomI18n.get("axiom.hardcoded.br_enabled")).withStyle(ChatFormatting.GREEN);
               } else {
                  enabledOrDisabled = Component.literal(AxiomI18n.get("axiom.hardcoded.br_disabled")).withStyle(ChatFormatting.RED);
               }

               guiGraphics.renderTooltip(
                  this.font,
                  List.of(
                     Component.literal(AxiomI18n.get("axiom.hardcoded.same_texture")).withStyle(ChatFormatting.YELLOW),
                     Component.literal(AxiomI18n.get("axiom.hardcoded.must_same_texture")),
                     enabledOrDisabled
                  ),
                  Optional.empty(),
                  mouseX,
                  mouseY
               );
               return;
            }
         }

         for (CreativeModeTab creativeModeTab : CreativeModeTabs.tabs()) {
            if (((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$checkTabHovering(guiGraphics, creativeModeTab, mouseX, mouseY)) {
               break;
            }
         }

         this.renderTooltip(guiGraphics, mouseX, mouseY);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      return this.mouseDragged(new LegacyMouseButtonEvent(mouseX, mouseY, button), deltaX, deltaY);
   }

   public boolean mouseDragged(LegacyMouseButtonEvent event, double deltaX, double deltaY) {
      if (this.draggingElement != -1) {
         float relX = (float)event.x() - this.leftPos;
         float relY = (float)event.y() - this.topPos;
         if (this.draggingElement == DRAGGING_SATVAL) {
            float s = Math.max(0.0F, Math.min(1.0F, (relX - 46.0F) / 43.0F));
            float b = 1.0F - Math.max(0.0F, Math.min(1.0F, (relY - 18.0F) / 52.0F));
            this.setHSB(-1.0F, s, b, false);
         } else if (this.draggingElement == DRAGGING_LARGE_HUE) {
            float h = Math.max(0.0F, Math.min(1.0F, (relY - 18.0F) / 52.0F));
            this.setHSB(h, -1.0F, -1.0F, false);
         } else {
            float value = Math.max(0.0F, Math.min(1.0F, (relX - 111.0F) / 42.0F));
            switch (this.draggingElement) {
               case 2:
                  this.setRGB(value * 255.0F, -1.0F, -1.0F, false, false);
                  break;
               case 3:
                  this.setRGB(-1.0F, value * 255.0F, -1.0F, false, false);
                  break;
               case 4:
                  this.setRGB(-1.0F, -1.0F, value * 255.0F, false, false);
                  break;
               case 5:
                  this.setHSB(value, -1.0F, -1.0F, false);
                  break;
               case 6:
                  this.setHSB(-1.0F, value, -1.0F, false);
                  break;
               case 7:
                  this.setHSB(-1.0F, -1.0F, value, false);
            }
         }

         return true;
      } else {
         return super.mouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY);
      }
   }

   private void setRGB(float r, float g, float b, boolean skipSameEditBox, boolean skipHex) {
      float[] rgb = new float[3];
      ColourUtils.HSBtoRGB(currentHue, currentSaturation, currentBrightness, rgb);
      if (r >= 0.0F) {
         rgb[0] = r;
      }

      if (g >= 0.0F) {
         rgb[1] = g;
      }

      if (b >= 0.0F) {
         rgb[2] = b;
      }

      float[] hsb = new float[3];
      ColourUtils.RGBtoHSB(rgb[0], rgb[1], rgb[2], hsb);
      currentHue = hsb[0];
      currentSaturation = hsb[1];
      currentBrightness = hsb[2];
      this.settingEditBoxes = true;
      if (!skipSameEditBox || r < 0.0F) {
         setValueIfDifferent(this.rgbHsbInputs[0], String.format("%.1f", rgb[0]));
      }

      if (!skipSameEditBox || g < 0.0F) {
         setValueIfDifferent(this.rgbHsbInputs[1], String.format("%.1f", rgb[1]));
      }

      if (!skipSameEditBox || b < 0.0F) {
         setValueIfDifferent(this.rgbHsbInputs[2], String.format("%.1f", rgb[2]));
      }

      setValueIfDifferent(this.rgbHsbInputs[3], String.format("%.1f", currentHue * 360.0F));
      setValueIfDifferent(this.rgbHsbInputs[4], String.format("%.1f", currentSaturation * 100.0F));
      setValueIfDifferent(this.rgbHsbInputs[5], String.format("%.1f", currentBrightness * 100.0F));
      if (!skipHex) {
         setValueIfDifferent(this.hexEditBox, String.format("#%06x", (int)rgb[0] << 16 | (int)rgb[1] << 8 | (int)rgb[2]));
      }

      this.settingEditBoxes = false;
   }

   private void setHSB(float h, float s, float b, boolean skipSameEditBox) {
      if (h >= 0.0F) {
         currentHue = h;
      }

      if (s >= 0.0F) {
         currentSaturation = s;
      }

      if (b >= 0.0F) {
         currentBrightness = b;
      }

      float[] rgb = new float[3];
      ColourUtils.HSBtoRGB(currentHue, currentSaturation, currentBrightness, rgb);
      this.settingEditBoxes = true;
      setValueIfDifferent(this.rgbHsbInputs[0], String.format("%.1f", rgb[0]));
      setValueIfDifferent(this.rgbHsbInputs[1], String.format("%.1f", rgb[1]));
      setValueIfDifferent(this.rgbHsbInputs[2], String.format("%.1f", rgb[2]));
      if (!skipSameEditBox || h < 0.0F) {
         setValueIfDifferent(this.rgbHsbInputs[3], String.format("%.1f", currentHue * 360.0F));
      }

      if (!skipSameEditBox || s < 0.0F) {
         setValueIfDifferent(this.rgbHsbInputs[4], String.format("%.1f", currentSaturation * 100.0F));
      }

      if (!skipSameEditBox || b < 0.0F) {
         setValueIfDifferent(this.rgbHsbInputs[5], String.format("%.1f", currentBrightness * 100.0F));
      }

      setValueIfDifferent(this.hexEditBox, String.format("#%06x", (int)rgb[0] << 16 | (int)rgb[1] << 8 | (int)rgb[2]));
      this.settingEditBoxes = false;
   }

   private static void setValueIfDifferent(EditBox editBox, String value) {
      if (!editBox.getValue().equals(value)) {
         editBox.setValue(value);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.mouseClicked(new LegacyMouseButtonEvent(mouseX, mouseY, button), false);
   }

   public boolean mouseClicked(LegacyMouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
      this.clearFocus();
      double mouseX = mouseButtonEvent.x();
      double mouseY = mouseButtonEvent.y();
      if (mouseButtonEvent.button() == 0) {
         this.draggingElement = -1;
         double relX = mouseButtonEvent.x() - this.leftPos;
         double relY = mouseButtonEvent.y() - this.topPos;
         if (mouseY >= this.topPos + 92 && mouseY <= this.topPos + 104) {
            if (mouseX >= this.leftPos + 46 && mouseX <= this.leftPos + 58) {
               if ((currentFlags & BlockColourMap.FLAG_FULL_CUBE) != 0) {
                  currentFlags = currentFlags & ~BlockColourMap.FLAG_FULL_CUBE;
               } else {
                  currentFlags = currentFlags | BlockColourMap.FLAG_FULL_CUBE;
               }

               this.updateNearestBlocks(true);
               return true;
            }

            if (mouseX >= this.leftPos + 60 && mouseX <= this.leftPos + 72) {
               if ((currentFlags & BlockColourMap.FLAG_SOLID) != 0) {
                  currentFlags = currentFlags & ~BlockColourMap.FLAG_SOLID;
               } else {
                  currentFlags = currentFlags | BlockColourMap.FLAG_SOLID;
               }

               this.updateNearestBlocks(true);
               return true;
            }

            if (mouseX >= this.leftPos + 74 && mouseX <= this.leftPos + 86) {
               if ((currentFlags & BlockColourMap.FLAG_OPAQUE) != 0) {
                  currentFlags = currentFlags & ~BlockColourMap.FLAG_OPAQUE;
               } else {
                  currentFlags = currentFlags | BlockColourMap.FLAG_OPAQUE;
               }

               this.updateNearestBlocks(true);
               return true;
            }

            if (mouseX >= this.leftPos + 88 && mouseX <= this.leftPos + 100) {
               if ((currentFlags & BlockColourMap.FLAG_SAME_TEXTURE) != 0) {
                  currentFlags = currentFlags & ~BlockColourMap.FLAG_SAME_TEXTURE;
               } else {
                  currentFlags = currentFlags | BlockColourMap.FLAG_SAME_TEXTURE;
               }

               this.updateNearestBlocks(true);
               return true;
            }
         }

         for (CreativeModeTab creativeModeTab : CreativeModeTabs.tabs()) {
            if (((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$checkTabClicked(creativeModeTab, relX, relY)) {
               return true;
            }
         }

         if (relX >= 46.0 && relX <= 89.0 && relY >= 18.0 && relY <= 70.0) {
            this.draggingElement = DRAGGING_SATVAL;
         } else if (relX >= 91.0 && relX <= 100.0 && relY >= 18.0 && relY <= 70.0) {
            this.draggingElement = DRAGGING_LARGE_HUE;
         } else if (relX >= 111.0 && relX <= 153.0) {
            for (int i = 0; i < 6; i++) {
               int y = 18 + 11 * i;
               if (i >= 3) {
                  y += 6;
               }

               if (relY >= y && relY <= y + 9) {
                  this.draggingElement = DRAGGING_RGB_OR_HSV_START + i;
               }
            }
         }

         if (this.draggingElement != -1) {
            Window window = Minecraft.getInstance().getWindow();
            double[] xpos = new double[1];
            double[] ypos = new double[1];
            GLFW.glfwGetCursorPos(window.getWindow(), xpos, ypos);
            double mouseXD = xpos[0] * window.getGuiScaledWidth() / window.getScreenWidth();
            double mouseYD = ypos[0] * window.getGuiScaledHeight() / window.getScreenHeight();
            this.mouseDragged(new LegacyMouseButtonEvent(mouseXD, mouseYD, mouseButtonEvent.button()), 0.0, 0.0);
            return true;
         }
      }

      return super.mouseClicked(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      return this.mouseReleased(new LegacyMouseButtonEvent(mouseX, mouseY, button));
   }

   public boolean mouseReleased(LegacyMouseButtonEvent event) {
      int mouseButton = event.button();
      double mouseX = event.x();
      double mouseY = event.y();
      if (mouseButton == 0) {
         if (this.draggingElement != -1) {
            this.draggingElement = -1;
            return true;
         }

         double relX = mouseX - this.leftPos;
         double relY = mouseY - this.topPos;

         for (CreativeModeTab creativeModeTab : CreativeModeTabs.tabs()) {
            if (((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$checkTabClicked(creativeModeTab, relX, relY)) {
               ((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$selectTab(creativeModeTab);
               Minecraft.getInstance().setScreen(this.creativeScreen);
               return true;
            }
         }
      }

      return super.mouseReleased(event.x(), event.y(), event.button());
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double f, double scroll) {
      if (super.mouseScrolled(mouseX, mouseY, f, scroll)) {
         return true;
      } else {
         if (mouseX >= this.leftPos + 8 && mouseX <= this.leftPos + 44 && mouseY >= this.topPos + 16 && mouseY <= this.topPos + 16 + 90) {
            int newScrollAmount = Math.max(0, Math.min(90, scrollAmount - (int)Math.signum(scroll) * 2));
            if (newScrollAmount != scrollAmount) {
               scrollAmount = newScrollAmount;
               this.updateNearestBlocks(false);
               return true;
            }
         }

         return false;
      }
   }

   protected void slotClicked(@Nullable Slot slot, int i, int j, ClickType clickType) {
      boolean isShiftClick = clickType == ClickType.QUICK_MOVE;
      clickType = i == -999 && clickType == ClickType.PICKUP ? ClickType.THROW : clickType;
      if (slot == null && clickType != ClickType.QUICK_CRAFT) {
         ((CreativeColourScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
      } else if (slot == null || slot.mayPickup(this.minecraft.player)) {
         if (slot == ((CreativeColourScreen.ItemPickerMenu)this.menu).pickColourItemSlot) {
            ItemStack carried = ((CreativeColourScreen.ItemPickerMenu)this.menu).getCarried();
            CustomBlock block = ServerCustomBlocks.getFromItemStack(carried);
            if (block != null && block != Blocks.AIR) {
               Vec3 lab = BlockColourMap.getLab(block);
               if (lab != null) {
                  int rgb = OkLabColourUtils.lab2rgb(lab.x, lab.y, lab.z);
                  float[] hsb = new float[3];
                  ColourUtils.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, hsb);
                  this.setHSB(hsb[0], hsb[1], hsb[2], false);
               }
            }
         } else if (slot == ((CreativeColourScreen.ItemPickerMenu)this.menu).destroyItemSlot && isShiftClick) {
            for (int slotIndex = 0; slotIndex < this.minecraft.player.inventoryMenu.getItems().size(); slotIndex++) {
               if (InventoryMenu.isHotbarSlot(slotIndex)) {
                  this.minecraft.player.inventoryMenu.getSlot(slotIndex).set(ItemStack.EMPTY);
                  this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, slotIndex);
               }
            }
         } else if (slot == ((CreativeColourScreen.ItemPickerMenu)this.menu).destroyItemSlot) {
            ((CreativeColourScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
         } else {
            if (clickType != ClickType.QUICK_CRAFT && slot.container == CONTAINER) {
               ItemStack itemStack = ((CreativeColourScreen.ItemPickerMenu)this.menu).getCarried();
               ItemStack itemStack2 = slot.getItem();
               if (clickType == ClickType.SWAP) {
                  if (!itemStack2.isEmpty()) {
                     this.minecraft.player.getInventory().setItem(j, itemStack2.copyWithCount(itemStack2.getMaxStackSize()));
                     ((CreativeColourScreen.ItemPickerMenu)this.menu).broadcastChanges();
                     this.minecraft.player.inventoryMenu.broadcastChanges();
                  }

                  return;
               }

               if (clickType == ClickType.CLONE) {
                  if (((CreativeColourScreen.ItemPickerMenu)this.menu).getCarried().isEmpty() && slot.hasItem()) {
                     ItemStack itemStack3 = slot.getItem();
                     ((CreativeColourScreen.ItemPickerMenu)this.menu).setCarried(itemStack3.copyWithCount(itemStack3.getMaxStackSize()));
                  }

                  return;
               }

               if (!itemStack.isEmpty() && !itemStack2.isEmpty() && ItemStack.isSameItemSameComponents(itemStack, itemStack2)) {
                  if (j == 0) {
                     if (isShiftClick) {
                        itemStack.setCount(itemStack.getMaxStackSize());
                     } else if (itemStack.getCount() < itemStack.getMaxStackSize()) {
                        itemStack.grow(1);
                     }
                  } else {
                     itemStack.shrink(1);
                  }
               } else if (!itemStack2.isEmpty() && itemStack.isEmpty()) {
                  int l = isShiftClick ? itemStack2.getMaxStackSize() : itemStack2.getCount();
                  ((CreativeColourScreen.ItemPickerMenu)this.menu).setCarried(itemStack2.copyWithCount(l));
               } else if (j == 0) {
                  ((CreativeColourScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
               } else if (!((CreativeColourScreen.ItemPickerMenu)this.menu).getCarried().isEmpty()) {
                  ((CreativeColourScreen.ItemPickerMenu)this.menu).getCarried().shrink(1);
               }
            } else if (this.menu != null) {
               ((CreativeColourScreen.ItemPickerMenu)this.menu).clicked(slot == null ? i : slot.index, j, clickType, this.minecraft.player);
               ((CreativeColourScreen.ItemPickerMenu)this.menu).broadcastChanges();
               this.minecraft.player.inventoryMenu.broadcastChanges();
            }
         }
      }
   }
   public static class ItemPickerMenu extends AbstractContainerMenu {
      private final AbstractContainerMenu inventoryMenu;
      private final Slot destroyItemSlot;
      private final Slot pickColourItemSlot;

      public ItemPickerMenu(Player player) {
         super(null, 0);
         this.inventoryMenu = player.inventoryMenu;
         Inventory inventory = player.getInventory();

         for (int i = 0; i < CreativeColourScreen.CONTAINER.getContainerSize(); i++) {
            this.addSlot(new Slot(CreativeColourScreen.CONTAINER, i, 9 + i % 2 * 18, 18 + i / 2 * 18));
         }

         for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(inventory, k, 9 + k * 18, 112));
         }

         this.pickColourItemSlot = new Slot(CreativeColourScreen.CONTAINER, 999, 84, 72);
         this.addSlot(this.pickColourItemSlot);
         this.destroyItemSlot = new Slot(CreativeColourScreen.CONTAINER, 999, 173, 112);
         this.addSlot(this.destroyItemSlot);
         this.setSynchronizer(new ContainerSynchronizer() {
            public void sendInitialData(AbstractContainerMenu abstractContainerMenu, NonNullList<ItemStack> nonNullList, ItemStack itemStack, int[] is) {
            }

            public void sendSlotChange(AbstractContainerMenu abstractContainerMenu, int index, ItemStack itemStack) {
               if (index >= CreativeColourScreen.CONTAINER.getContainerSize() && index < CreativeColourScreen.CONTAINER.getContainerSize() + 9) {
                  Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(itemStack, 36 + index - CreativeColourScreen.CONTAINER.getContainerSize());
               }
            }

            public void sendCarriedChange(AbstractContainerMenu abstractContainerMenu, ItemStack itemStack) {
            }

            public void sendDataChange(AbstractContainerMenu abstractContainerMenu, int i, int j) {
            }
         });
      }

      public boolean stillValid(Player player) {
         return true;
      }

      public ItemStack quickMoveStack(Player player, int i) {
         if (i >= this.slots.size() - 11 && i < this.slots.size() - 2) {
            Slot slot = (Slot)this.slots.get(i);
            if (slot != null && slot.hasItem()) {
               slot.setByPlayer(ItemStack.EMPTY);
            }
         }

         return ItemStack.EMPTY;
      }

      public boolean canTakeItemForPickAll(ItemStack itemStack, Slot slot) {
         return slot.container != CreativeColourScreen.CONTAINER;
      }

      public boolean canDragTo(Slot slot) {
         return slot.container != CreativeColourScreen.CONTAINER;
      }

      public ItemStack getCarried() {
         return this.inventoryMenu.getCarried();
      }

      public void setCarried(ItemStack itemStack) {
         this.inventoryMenu.setCarried(itemStack);
      }

      public void setItem(int index, int state, ItemStack itemStack) {
         if (index >= CreativeColourScreen.CONTAINER.getContainerSize() && index < CreativeColourScreen.CONTAINER.getContainerSize() + 9) {
            super.setItem(index, state, itemStack);
         }
      }
   }
}
