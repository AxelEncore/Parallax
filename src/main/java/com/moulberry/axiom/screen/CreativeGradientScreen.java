package com.moulberry.axiom.screen;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.CreativeModeInventoryScreenExt;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.gradient_painter.GradientPainterTool;
import com.moulberry.axiom.tools.painter.PainterTool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
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

public class CreativeGradientScreen extends AbstractContainerScreen<CreativeGradientScreen.ItemPickerMenu> {
   private static final SimpleContainer CONTAINER = new SimpleContainer(18);
   private static final int[] GRADIENT_REFRESH_COUNT = new int[]{-1, -1, -1, -1, -1, -1, -1, -1, -1};
   private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("axiom", "textures/gui/creative_tab_gradient_helper.png");
   private static final ResourceLocation INSET_CREATIVE_COLOUR_FLAG = ResourceLocation.parse("axiom:inset_creative_colour_flag_toggle");
   private static final ResourceLocation CREATIVE_GRADIENT_REFRESH = ResourceLocation.parse("axiom:creative_gradient_refresh");
   private static final ItemStack COPY_BUTTON_ICON = new ItemStack(Items.WRITABLE_BOOK);
   private static final ItemStack GRADIENT_PAINTER_ICON = new ItemStack(Items.MAGENTA_GLAZED_TERRACOTTA);
   private static final ItemStack GRADIENT_BRUSH_ICON = new ItemStack(Items.BRUSH);
   private final CreativeModeInventoryScreen creativeScreen;
   private boolean blockTakenFromGradientInput = false;
   private boolean isHoveringRefresh = false;

   public CreativeGradientScreen(LocalPlayer localPlayer, CreativeModeInventoryScreen creativeScreen) {
      super(new CreativeGradientScreen.ItemPickerMenu(localPlayer), localPlayer.getInventory(), Component.translatable("axiom.gradient_helper"));
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
         if (Axiom.configuration.creativeMenu.showColorPicker) {
            this.addRenderableWidget(
               new CustomCreativeTabButton(
                  this.leftPos - 36,
                  this.topPos + 30,
                  26,
                  26,
                  Component.translatable("axiom.color_picker"),
                  new ItemStack(Items.YELLOW_DYE),
                  () -> Minecraft.getInstance().setScreen(new CreativeColourScreen(this.minecraft.player, this.creativeScreen))
               )
            );
         }

         CustomCreativeTabButton button = new CustomCreativeTabButton(
            this.leftPos - 36,
            this.topPos + 30 + 36,
            26,
            26,
            Component.translatable("axiom.gradient_helper"),
            new ItemStack(Items.BRUSH),
            () -> Minecraft.getInstance().setScreen(new CreativeGradientScreen(this.minecraft.player, this.creativeScreen))
         );
         button.active = false;
         this.addRenderableWidget(button);
      }
   }

   public void resize(Minecraft minecraft, int width, int height) {
      this.init(minecraft, width, height);
   }

   private void updateGradient(boolean resetRefreshCount) {
      if (resetRefreshCount) {
         Arrays.fill(GRADIENT_REFRESH_COUNT, -1);
      }

      CustomBlock from = null;
      int fromIndex = -1;

      for (int i = 9; i < 18; i++) {
         CONTAINER.setItem(i, ItemStack.EMPTY);
      }

      double[][] labForBlocks = new double[9][];
      CustomBlock[] originalBlocksWithoutRefresh = new CustomBlock[9];

      for (int index = 0; index < 9; index++) {
         ItemStack itemStack = CONTAINER.getItem(index);
         CustomBlock block = ServerCustomBlocks.getFromItemStack(itemStack);
         if (block != Blocks.AIR) {
            CONTAINER.setItem(index + 9, itemStack);
            originalBlocksWithoutRefresh[index] = block;
            if (from != null) {
               int gradientSteps = index - fromIndex - 1;
               Vec3 fromLab = BlockColourMap.getLab(from);
               Vec3 toLab = BlockColourMap.getLab(block);
               if (fromLab != null && toLab != null) {
                  for (int step = 0; step < gradientSteps; step++) {
                     float partial = (float)(step + 1) / (gradientSteps + 1);
                     double l = fromLab.x + (toLab.x - fromLab.x) * partial;
                     double a = fromLab.y + (toLab.y - fromLab.y) * partial;
                     double b = fromLab.z + (toLab.z - fromLab.z) * partial;
                     int gradientIndex = fromIndex + step + 1;
                     int refreshCount = GRADIENT_REFRESH_COUNT[gradientIndex];
                     BlockState targetBlock;
                     if (refreshCount > 0) {
                        List<BlockState> targetBlocks = BlockColourMap.getNearestLabN(l, a, b, CreativeColourScreen.currentFlags, refreshCount + 1);
                        targetBlock = targetBlocks.get(targetBlocks.size() - 1);
                        originalBlocksWithoutRefresh[gradientIndex] = ServerCustomBlocks.getCustomOrVanillaStateFor(targetBlocks.get(0)).getCustomBlock();
                     } else {
                        targetBlock = BlockColourMap.getNearestLab(l, a, b, CreativeColourScreen.currentFlags);
                        originalBlocksWithoutRefresh[gradientIndex] = ServerCustomBlocks.getCustomOrVanillaStateFor(targetBlock).getCustomBlock();
                     }

                     labForBlocks[gradientIndex] = new double[]{l, a, b};
                     CONTAINER.setItem(gradientIndex + 9, ServerCustomBlocks.getCustomOrVanillaStateFor(targetBlock).getCustomBlock().axiom$asItemStack());
                     if (GRADIENT_REFRESH_COUNT[gradientIndex] < 0) {
                        GRADIENT_REFRESH_COUNT[gradientIndex] = 0;
                     }
                  }
               }
            }

            from = block;
            fromIndex = index;
         }
      }

      for (int i = 1; i < 8; i++) {
         double[] lab = labForBlocks[i];
         if (lab != null) {
            CustomBlock block = originalBlocksWithoutRefresh[i];
            if (block != Blocks.AIR) {
               CustomBlock leftBlock = originalBlocksWithoutRefresh[i - 1];
               if (leftBlock != Blocks.AIR) {
                  CustomBlock rightBlock = originalBlocksWithoutRefresh[i + 1];
                  if (rightBlock != Blocks.AIR && leftBlock != rightBlock && (block == leftBlock || block == rightBlock)) {
                     List<BlockState> targetBlocks = BlockColourMap.getNearestLabN(lab[0], lab[1], lab[2], CreativeColourScreen.currentFlags, 2);
                     CustomBlock targetBlockx = ServerCustomBlocks.getCustomOrVanillaStateFor(targetBlocks.get(targetBlocks.size() - 1)).getCustomBlock();
                     if (targetBlockx != leftBlock && targetBlockx != rightBlock) {
                        originalBlocksWithoutRefresh[i] = targetBlockx;
                        if (GRADIENT_REFRESH_COUNT[i] == 0) {
                           CONTAINER.setItem(i + 9, targetBlockx.axiom$asItemStack());
                           GRADIENT_REFRESH_COUNT[i] = 1;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean keyPressed(int i, int j, int k) {
      if (this.minecraft.options.keyChat.matches(i, j)) {
         ((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$selectTab(CreativeModeTabs.searchTab());
         this.minecraft.setScreen(this.creativeScreen);
         return true;
      } else {
         return super.keyPressed(i, j, k);
      }
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      for (CreativeModeTab creativeModeTab : CreativeModeTabs.tabs()) {
         ((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$renderTabButton(guiGraphics, mouseX, mouseY, creativeModeTab, true);
      }

      VersionUtilsClient.genericBlit(
         guiGraphics, Dummy.GUI_TEXTURED, BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256, -1
      );
      guiGraphics.renderFakeItem(COPY_BUTTON_ICON, this.leftPos + 173, this.topPos + 90);
      guiGraphics.renderFakeItem(GRADIENT_PAINTER_ICON, this.leftPos + 173, this.topPos + 72);
      guiGraphics.renderFakeItem(GRADIENT_BRUSH_ICON, this.leftPos + 173, this.topPos + 54);
      if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_FULL_CUBE) != 0) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, INSET_CREATIVE_COLOUR_FLAG, this.leftPos + 116, this.topPos + 39, 12, 12);
      }

      if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_SOLID) != 0) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, INSET_CREATIVE_COLOUR_FLAG, this.leftPos + 130, this.topPos + 39, 12, 12);
      }

      if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_OPAQUE) != 0) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, INSET_CREATIVE_COLOUR_FLAG, this.leftPos + 144, this.topPos + 39, 12, 12);
      }

      if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_SAME_TEXTURE) != 0) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, INSET_CREATIVE_COLOUR_FLAG, this.leftPos + 158, this.topPos + 39, 12, 12);
      }

      guiGraphics.drawCenteredString(this.font, Component.translatable("axiom.color_picker.full_cube_abbreviation"), this.leftPos + 122, this.topPos + 41, -1);
      guiGraphics.drawCenteredString(this.font, Component.translatable("axiom.color_picker.solid_abbreviation"), this.leftPos + 136, this.topPos + 41, -1);
      guiGraphics.drawCenteredString(this.font, Component.translatable("axiom.color_picker.opaque_abbreviation"), this.leftPos + 150, this.topPos + 41, -1);
      guiGraphics.drawCenteredString(
         this.font, Component.translatable("axiom.color_picker.same_texture_abbreviation"), this.leftPos + 164, this.topPos + 41, -1
      );
   }

   protected void renderLabels(GuiGraphics graphics, int i, int j) {
      graphics.drawString(this.font, this.title, 8, 6, -12566464, false);
      graphics.drawString(this.font, Component.translatable("axiom.gradient_helper.inputs"), 8, 42, -12566464, false);
      graphics.drawString(this.font, Component.translatable("axiom.gradient_helper.outputs"), 8, 78, -12566464, false);
   }

   protected boolean isHovering(int i, int j, int k, int l, double d, double e) {
      return this.isHoveringRefresh ? false : super.isHovering(i, j, k, l, d, e);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.isHoveringRefresh = false;

      for (int i = 0; i < 9; i++) {
         int refreshes = GRADIENT_REFRESH_COUNT[i];
         if (refreshes >= 0) {
            int x = this.leftPos + 17 + 18 * i;
            int y = this.topPos + 100;
            if (mouseX >= x && mouseX <= x + 9 && mouseY >= y && mouseY <= y + 7) {
               this.isHoveringRefresh = true;
               break;
            }
         }
      }

      super.render(guiGraphics, mouseX, mouseY, partialTick);
      PoseStack pose = guiGraphics.pose();
      pose.pushPose();
      pose.translate(0.0F, 0.0F, 266.0F);

      for (int ix = 0; ix < 9; ix++) {
         int refreshes = GRADIENT_REFRESH_COUNT[ix];
         if (refreshes >= 0) {
            int x = this.leftPos + 17 + 18 * ix;
            int y = this.topPos + 100;
            if (refreshes >= 32) {
               VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, CREATIVE_GRADIENT_REFRESH, x, y, 9, 7, -8355712);
            } else {
               VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, CREATIVE_GRADIENT_REFRESH, x, y, 9, 7);
            }

            if (this.isHoveringRefresh && mouseX >= x && mouseX <= x + 9 && mouseY >= y && mouseY <= y + 7) {
               List<Component> lines = List.of(
                  refreshes >= 32
                     ? Component.literal(AxiomI18n.get("axiom.hardcoded.refresh_max"))
                     : (refreshes == 0 ? Component.literal(AxiomI18n.get("axiom.hardcoded.refresh")) : Component.literal(AxiomI18n.get("axiom.hardcoded.refresh_paren") + refreshes + ")")),
                  Component.literal(AxiomI18n.get("axiom.hardcoded.left_click_next")),
                  Component.literal(AxiomI18n.get("axiom.hardcoded.right_click_prev"))
               );
               guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
            }
         }
      }

      pose.popPose();
      if (!this.isHoveringRefresh) {
         if (mouseY >= this.topPos + 39 && mouseY <= this.topPos + 51) {
            if (mouseX >= this.leftPos + 116 && mouseX <= this.leftPos + 128) {
               Component enabledOrDisabled;
               if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_FULL_CUBE) != 0) {
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

            if (mouseX >= this.leftPos + 130 && mouseX <= this.leftPos + 142) {
               Component enabledOrDisabled;
               if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_SOLID) != 0) {
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

            if (mouseX >= this.leftPos + 144 && mouseX <= this.leftPos + 156) {
               Component enabledOrDisabled;
               if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_OPAQUE) != 0) {
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

            if (mouseX >= this.leftPos + 158 && mouseX <= this.leftPos + 170) {
               Component enabledOrDisabled;
               if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_SAME_TEXTURE) != 0) {
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

         if (mouseX >= this.leftPos + 172 && mouseX <= this.leftPos + 190 && mouseY >= this.topPos + 90 && mouseY <= this.topPos + 108) {
            guiGraphics.renderTooltip(this.font, Component.literal(AxiomI18n.get("axiom.hardcoded.copy_to_hotbar")), mouseX, mouseY);
         }

         if (mouseX >= this.leftPos + 172 && mouseX <= this.leftPos + 190 && mouseY >= this.topPos + 72 && mouseY <= this.topPos + 90) {
            guiGraphics.renderTooltip(this.font, Component.literal(AxiomI18n.get("axiom.hardcoded.export_gradient_painter")), mouseX, mouseY);
         }

         if (mouseX >= this.leftPos + 172 && mouseX <= this.leftPos + 190 && mouseY >= this.topPos + 54 && mouseY <= this.topPos + 72) {
            guiGraphics.renderTooltip(this.font, Component.literal(AxiomI18n.get("axiom.hardcoded.export_gradient_brush")), mouseX, mouseY);
         }

         this.renderTooltip(guiGraphics, mouseX, mouseY);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
      this.clearFocus();
      if (mouseButton == 0 || mouseButton == 1) {
         int change = mouseButton == 0 ? 1 : -1;

         for (int i = 0; i < 9; i++) {
            int refreshes = GRADIENT_REFRESH_COUNT[i];
            if (refreshes >= 0 && refreshes + change >= 0 && refreshes + change <= 32) {
               int x = this.leftPos + 17 + 18 * i;
               int y = this.topPos + 100;
               if (mouseX >= x && mouseX <= x + 9 && mouseY >= y && mouseY <= y + 7) {
                  GRADIENT_REFRESH_COUNT[i] = GRADIENT_REFRESH_COUNT[i] + change;
                  this.updateGradient(false);
                  return true;
               }
            }
         }
      }

      if (mouseButton == 0) {
         double relX = mouseX - this.leftPos;
         double relY = mouseY - this.topPos;
         if (mouseY >= this.topPos + 39 && mouseY <= this.topPos + 51) {
            if (mouseX >= this.leftPos + 116 && mouseX <= this.leftPos + 128) {
               if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_FULL_CUBE) != 0) {
                  CreativeColourScreen.currentFlags = CreativeColourScreen.currentFlags & ~BlockColourMap.FLAG_FULL_CUBE;
               } else {
                  CreativeColourScreen.currentFlags = CreativeColourScreen.currentFlags | BlockColourMap.FLAG_FULL_CUBE;
               }

               this.updateGradient(false);
               return true;
            }

            if (mouseX >= this.leftPos + 130 && mouseX <= this.leftPos + 142) {
               if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_SOLID) != 0) {
                  CreativeColourScreen.currentFlags = CreativeColourScreen.currentFlags & ~BlockColourMap.FLAG_SOLID;
               } else {
                  CreativeColourScreen.currentFlags = CreativeColourScreen.currentFlags | BlockColourMap.FLAG_SOLID;
               }

               this.updateGradient(false);
               return true;
            }

            if (mouseX >= this.leftPos + 144 && mouseX <= this.leftPos + 156) {
               if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_OPAQUE) != 0) {
                  CreativeColourScreen.currentFlags = CreativeColourScreen.currentFlags & ~BlockColourMap.FLAG_OPAQUE;
               } else {
                  CreativeColourScreen.currentFlags = CreativeColourScreen.currentFlags | BlockColourMap.FLAG_OPAQUE;
               }

               this.updateGradient(false);
               return true;
            }

            if (mouseX >= this.leftPos + 158 && mouseX <= this.leftPos + 170) {
               if ((CreativeColourScreen.currentFlags & BlockColourMap.FLAG_SAME_TEXTURE) != 0) {
                  CreativeColourScreen.currentFlags = CreativeColourScreen.currentFlags & ~BlockColourMap.FLAG_SAME_TEXTURE;
               } else {
                  CreativeColourScreen.currentFlags = CreativeColourScreen.currentFlags | BlockColourMap.FLAG_SAME_TEXTURE;
               }

               this.updateGradient(false);
               return true;
            }
         }

         for (CreativeModeTab creativeModeTab : CreativeModeTabs.tabs()) {
            if (((CreativeModeInventoryScreenExt)this.creativeScreen).axiom$checkTabClicked(creativeModeTab, relX, relY)) {
               return true;
            }
         }

         if (mouseX >= this.leftPos + 172 && mouseX <= this.leftPos + 190) {
            if (mouseY >= this.topPos + 90 && mouseY <= this.topPos + 108) {
               for (int index = 0; index < 9; index++) {
                  ItemStack itemStack = CONTAINER.getItem(index + 9);
                  this.minecraft.player.getInventory().setItem(index, itemStack);
                  this.minecraft.gameMode.handleCreativeModeItemAdd(itemStack, 36 + index);
               }

               return true;
            }

            if (mouseY >= this.topPos + 72 && mouseY <= this.topPos + 90) {
               Minecraft.getInstance().setScreen(null);
               EditorUI.enable();
               ToolManager.setToolSelected(true);
               ToolManager.setTool(GradientPainterTool.class);
               if (ToolManager.getCurrentTool() instanceof GradientPainterTool gradientPainterTool) {
                  List<CustomBlockState> blocks = new ArrayList<>();

                  for (int index = 0; index < 9; index++) {
                     ItemStack itemStack = CONTAINER.getItem(index + 9);
                     if (!itemStack.isEmpty()) {
                        blocks.add(ServerCustomBlocks.getFromItemStack(itemStack).axiom$defaultCustomState());
                     }
                  }

                  gradientPainterTool.setBlocks(blocks);
               }

               return true;
            }

            if (mouseY >= this.topPos + 54 && mouseY <= this.topPos + 72) {
               Minecraft.getInstance().setScreen(null);
               EditorUI.enable();
               ToolManager.setToolSelected(true);
               ToolManager.setTool(PainterTool.class);
               if (ToolManager.getCurrentTool() instanceof PainterTool painterTool) {
                  List<CustomBlockState> blocks = new ArrayList<>();

                  for (int indexx = 0; indexx < 9; indexx++) {
                     ItemStack itemStack = CONTAINER.getItem(indexx + 9);
                     if (!itemStack.isEmpty()) {
                        blocks.add(ServerCustomBlocks.getFromItemStack(itemStack).axiom$defaultCustomState());
                     }
                  }

                  painterTool.setGradientBlocks(blocks);
               }

               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, mouseButton);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int mouseButton) {
      if (mouseButton == 0) {
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

      return super.mouseReleased(mouseX, mouseY, mouseButton);
   }

   protected void slotClicked(@Nullable Slot slot, int i, int j, ClickType clickType) {
      boolean isShiftClick = clickType == ClickType.QUICK_MOVE;
      clickType = i == -999 && clickType == ClickType.PICKUP ? ClickType.THROW : clickType;
      boolean wasTakenFromGradientInput = this.blockTakenFromGradientInput;
      this.blockTakenFromGradientInput = false;
      if (slot == null && clickType != ClickType.QUICK_CRAFT) {
         ((CreativeGradientScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
      } else if (slot == null || slot.mayPickup(this.minecraft.player)) {
         if (slot == ((CreativeGradientScreen.ItemPickerMenu)this.menu).destroyItemSlot && isShiftClick) {
            for (int slotIndex = 0; slotIndex < this.minecraft.player.inventoryMenu.getItems().size(); slotIndex++) {
               if (InventoryMenu.isHotbarSlot(slotIndex)) {
                  this.minecraft.player.inventoryMenu.getSlot(slotIndex).set(ItemStack.EMPTY);
                  this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, slotIndex);
               }
            }
         } else if (slot == ((CreativeGradientScreen.ItemPickerMenu)this.menu).destroyItemSlot) {
            ((CreativeGradientScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
         } else {
            if (clickType != ClickType.QUICK_CRAFT && slot.container == CONTAINER) {
               ItemStack carried = ((CreativeGradientScreen.ItemPickerMenu)this.menu).getCarried();
               ItemStack target = slot.getItem();
               if (clickType == ClickType.SWAP) {
                  if (!target.isEmpty()) {
                     this.minecraft.player.getInventory().setItem(j, target.copyWithCount(target.getMaxStackSize()));
                     ((CreativeGradientScreen.ItemPickerMenu)this.menu).broadcastChanges();
                     this.minecraft.player.inventoryMenu.broadcastChanges();
                  }

                  return;
               }

               if (clickType == ClickType.CLONE) {
                  if (((CreativeGradientScreen.ItemPickerMenu)this.menu).getCarried().isEmpty() && slot.hasItem()) {
                     ItemStack itemStack3 = slot.getItem();
                     ((CreativeGradientScreen.ItemPickerMenu)this.menu).setCarried(itemStack3.copyWithCount(itemStack3.getMaxStackSize()));
                  }

                  return;
               }

               if (!carried.isEmpty() && !target.isEmpty() && ItemStack.isSameItemSameComponents(carried, target)) {
                  if (slot.index < 9) {
                     slot.set(ItemStack.EMPTY);
                     this.blockTakenFromGradientInput = wasTakenFromGradientInput;
                     this.updateGradient(true);
                  } else if (j == 0) {
                     if (isShiftClick) {
                        carried.setCount(carried.getMaxStackSize());
                     } else if (carried.getCount() < carried.getMaxStackSize()) {
                        carried.grow(1);
                     }
                  } else {
                     carried.shrink(1);
                  }
               } else if (!target.isEmpty() && carried.isEmpty()) {
                  if (slot.index < 9) {
                     slot.set(ItemStack.EMPTY);
                     this.updateGradient(true);
                     if (!isShiftClick) {
                        this.blockTakenFromGradientInput = true;
                        ((CreativeGradientScreen.ItemPickerMenu)this.menu).setCarried(target.copy());
                     }
                  } else {
                     int count = isShiftClick ? target.getMaxStackSize() : target.getCount();
                     ((CreativeGradientScreen.ItemPickerMenu)this.menu).setCarried(target.copyWithCount(count));
                  }
               } else if (j == 0) {
                  if (slot.index < 9) {
                     slot.set(carried.copyWithCount(1));
                     if (wasTakenFromGradientInput) {
                        ((CreativeGradientScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
                     }

                     if (carried.getItem() != target.getItem()) {
                        this.updateGradient(true);
                     }
                  } else {
                     ((CreativeGradientScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
                  }
               } else if (!((CreativeGradientScreen.ItemPickerMenu)this.menu).getCarried().isEmpty()) {
                  if (slot.index < 9) {
                     slot.set(carried.copyWithCount(1));
                     if (wasTakenFromGradientInput) {
                        ((CreativeGradientScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
                     }

                     if (carried.getItem() != target.getItem()) {
                        this.updateGradient(true);
                     }
                  } else {
                     ((CreativeGradientScreen.ItemPickerMenu)this.menu).getCarried().shrink(1);
                  }
               }
            } else if (this.menu != null) {
               ((CreativeGradientScreen.ItemPickerMenu)this.menu).clicked(slot == null ? i : slot.index, j, clickType, this.minecraft.player);
               ((CreativeGradientScreen.ItemPickerMenu)this.menu).broadcastChanges();
               this.minecraft.player.inventoryMenu.broadcastChanges();
            }
         }
      }
   }
   public static class ItemPickerMenu extends AbstractContainerMenu {
      private final AbstractContainerMenu inventoryMenu;
      private final Slot destroyItemSlot;

      public ItemPickerMenu(Player player) {
         super(null, 0);
         this.inventoryMenu = player.inventoryMenu;
         Inventory inventory = player.getInventory();

         for (int i = 0; i < CreativeGradientScreen.CONTAINER.getContainerSize(); i++) {
            if (i < 9) {
               this.addSlot(new Slot(CreativeGradientScreen.CONTAINER, i, 9 + i * 18, 54));
            } else {
               this.addSlot(new Slot(CreativeGradientScreen.CONTAINER, i, 9 + (i - 9) * 18, 90));
            }
         }

         for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(inventory, k, 9 + k * 18, 112));
         }

         this.destroyItemSlot = new Slot(CreativeGradientScreen.CONTAINER, 999, 173, 112);
         this.addSlot(this.destroyItemSlot);
         this.setSynchronizer(new ContainerSynchronizer() {
            public void sendInitialData(AbstractContainerMenu abstractContainerMenu, NonNullList<ItemStack> nonNullList, ItemStack itemStack, int[] is) {
            }

            public void sendSlotChange(AbstractContainerMenu abstractContainerMenu, int index, ItemStack itemStack) {
               if (index >= CreativeGradientScreen.CONTAINER.getContainerSize() && index < CreativeGradientScreen.CONTAINER.getContainerSize() + 9) {
                  Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(itemStack, 36 + index - CreativeGradientScreen.CONTAINER.getContainerSize());
               }
            }

            public void sendCarriedChange(AbstractContainerMenu abstractContainerMenu, ItemStack itemStack) {
            }

            public void sendDataChange(AbstractContainerMenu abstractContainerMenu, int ix, int j) {
            }
         });
      }

      public boolean stillValid(Player player) {
         return true;
      }

      public ItemStack quickMoveStack(Player player, int i) {
         if (i >= this.slots.size() - 10 && i < this.slots.size() - 1) {
            Slot slot = (Slot)this.slots.get(i);
            if (slot != null && slot.hasItem()) {
               slot.setByPlayer(ItemStack.EMPTY);
            }
         }

         return ItemStack.EMPTY;
      }

      public boolean canTakeItemForPickAll(ItemStack itemStack, Slot slot) {
         return slot.container != CreativeGradientScreen.CONTAINER;
      }

      public boolean canDragTo(Slot slot) {
         return slot.container != CreativeGradientScreen.CONTAINER;
      }

      public ItemStack getCarried() {
         return this.inventoryMenu.getCarried();
      }

      public void setCarried(ItemStack itemStack) {
         this.inventoryMenu.setCarried(itemStack);
      }

      public void setItem(int index, int state, ItemStack itemStack) {
         if (index >= CreativeGradientScreen.CONTAINER.getContainerSize() && index < CreativeGradientScreen.CONTAINER.getContainerSize() + 9) {
            super.setItem(index, state, itemStack);
         }
      }
   }
}
