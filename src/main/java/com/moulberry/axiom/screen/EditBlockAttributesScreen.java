package com.moulberry.axiom.screen;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtilsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class EditBlockAttributesScreen extends Screen {
   private static final ResourceLocation BACKGROUND = ResourceLocation.parse("axiom:background");
   private int leftPos;
   private int topPos;
   private int menuWidth;
   private int menuHeight;
   private boolean reloadWorld = false;

   public EditBlockAttributesScreen() {
      super(Component.translatable("axiom.block_attributes.title"));
   }

   protected void init() {
      super.init();
      this.menuWidth = 195;
      this.menuHeight = 177;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      int x = this.leftPos + 8;
      int y = this.topPos + 17;
      AbstractWidget widget = (AbstractWidget)this.addRenderableWidget(
         CycleButton.onOffBuilder(Axiom.configuration.blockAttributes.showCollisionMesh)
            .create(
               x,
               y,
               179,
               20,
               Component.translatable("axiom.block_attributes.show_collision_mesh"),
               (button, bool) -> Axiom.configuration.blockAttributes.showCollisionMesh = bool
            )
      );
      widget.setTooltip(createTooltip("axiom.block_attributes.show_collision_mesh"));
      y += 22;
      widget = (AbstractWidget)this.addRenderableWidget(
         CycleButton.onOffBuilder(Axiom.configuration.blockAttributes.showLightBlocks)
            .create(x, y, 179, 20, Component.translatable("axiom.block_attributes.show_light_blocks"), (button, bool) -> {
               Axiom.configuration.blockAttributes.showLightBlocks = bool;
               this.reloadWorld = true;
            })
      );
      widget.setTooltip(createTooltip("axiom.block_attributes.show_light_blocks"));
      y += 22;
      widget = (AbstractWidget)this.addRenderableWidget(
         CycleButton.onOffBuilder(Axiom.configuration.blockAttributes.showStructureVoidBlocks)
            .create(x, y, 179, 20, Component.translatable("axiom.block_attributes.show_structure_void_blocks"), (button, bool) -> {
               Axiom.configuration.blockAttributes.showStructureVoidBlocks = bool;
               this.reloadWorld = true;
            })
      );
      widget.setTooltip(createTooltip("axiom.block_attributes.show_structure_void_blocks"));
      y += 22;
      widget = (AbstractWidget)this.addRenderableWidget(
         CycleButton.onOffBuilder(Axiom.configuration.blockAttributes.showMovingPistonBlocks)
            .create(x, y, 179, 20, Component.translatable("axiom.block_attributes.show_moving_piston_blocks"), (button, bool) -> {
               Axiom.configuration.blockAttributes.showMovingPistonBlocks = bool;
               this.reloadWorld = true;
            })
      );
      widget.setTooltip(createTooltip("axiom.block_attributes.show_moving_piston_blocks"));
      y += 22;
      widget = (AbstractWidget)this.addRenderableWidget(
         CycleButton.onOffBuilder(Axiom.configuration.blockAttributes.expandHitboxesToFullCube)
            .create(
               x,
               y,
               179,
               20,
               Component.translatable("axiom.block_attributes.expand_hitboxes_to_full_cube"),
               (button, bool) -> Axiom.configuration.blockAttributes.expandHitboxesToFullCube = bool
            )
      );
      widget.setTooltip(createTooltip("axiom.block_attributes.expand_hitboxes_to_full_cube"));
      y += 22;
      widget = (AbstractWidget)this.addRenderableWidget(
         CycleButton.onOffBuilder(Axiom.configuration.blockAttributes.makeFluidHitboxesSolid)
            .create(
               x,
               y,
               179,
               20,
               Component.translatable("axiom.block_attributes.make_fluid_hitboxes_solid"),
               (button, bool) -> Axiom.configuration.blockAttributes.makeFluidHitboxesSolid = bool
            )
      );
      widget.setTooltip(createTooltip("axiom.block_attributes.make_fluid_hitboxes_solid"));
      y += 22;
      widget = (AbstractWidget)this.addRenderableWidget(
         CycleButton.onOffBuilder(Axiom.configuration.blockAttributes.preventInteractions)
            .create(
               x,
               y,
               179,
               20,
               Component.translatable("axiom.block_attributes.prevent_interactions"),
               (button, bool) -> Axiom.configuration.blockAttributes.preventInteractions = bool
            )
      );
      widget.setTooltip(createTooltip("axiom.block_attributes.prevent_interactions"));
   }

   private static Tooltip createTooltip(String key) {
      return Tooltip.create(
         Component.empty()
            .append(Component.translatable(key).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal("\n"))
            .append(Component.translatable(key + ".description"))
      );
   }

   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, this.menuWidth, this.menuHeight);
   }

   public void render(GuiGraphics graphics, int i, int j, float f) {
      super.render(graphics, i, j, f);
      graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, -12566464, false);
   }

   public void onClose() {
      super.onClose();
      if (this.reloadWorld) {
         AxiomClient.updateItemBlockRenderTypes();
         Minecraft.getInstance().levelRenderer.allChanged();
      }
   }
}
