package com.moulberry.axiom.screen;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.buildertools.BuilderTool;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.configuration.BuilderToolMiddleClick;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.versioning.input_events.LegacyMouseButtonEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.CycleButton.Builder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

public class SwitchBuilderToolScreen extends Screen {
   public static final ResourceLocation TOOL_SWAPPER_LOCATION = ResourceLocation.parse("axiom:gui/tool_swapper.png");
   private double accumulatedScroll = 0.0;

   public SwitchBuilderToolScreen() {
      super(Component.literal(AxiomI18n.get("axiom.hardcoded.switch_tool")));
   }

   public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
   }

   protected void init() {
      super.init();
      Component copyAirText = Component.literal(AxiomI18n.get("axiom.hardcoded.comp_copy_air"));
      this.addRenderableWidget(
         CycleButton.onOffBuilder(BuilderToolManager.copyAir)
            .create(this.width / 2 - 91, this.height - 44, 182, 20, copyAirText, (button, bool) -> BuilderToolManager.copyAir = bool)
      );
      Component copyEntitiesText = Component.literal(AxiomI18n.get("axiom.hardcoded.comp_copy_entities"));
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.REQUEST_ENTITY)) {
         CycleButton<Boolean> widget = (CycleButton<Boolean>)this.addRenderableWidget(
            CycleButton.onOffBuilder(false).create(this.width / 2 - 91, this.height - 66, 182, 20, copyEntitiesText, (button, bool) -> {})
         );
         widget.active = false;
      } else {
         this.addRenderableWidget(
            CycleButton.onOffBuilder(BuilderToolManager.copyEntities)
               .create(this.width / 2 - 91, this.height - 66, 182, 20, copyEntitiesText, (button, bool) -> BuilderToolManager.copyEntities = bool)
         );
      }

      Component keepExistingText = Component.literal(AxiomI18n.get("axiom.hardcoded.keep_existing"));
      this.addRenderableWidget(
         CycleButton.onOffBuilder(BuilderToolManager.keepExisting)
            .create(this.width / 2 - 91, this.height - 88, 182, 20, keepExistingText, (button, bool) -> BuilderToolManager.keepExisting = bool)
      );
      Builder<Object> middleClickButton = CycleButton.builder(
            v -> (Boolean)v
               ? Component.translatable("axiom.config.builder_tools.middle_click.extend_select")
               : Component.translatable("axiom.config.builder_tools.middle_click.magic_select")
         )
         .withInitialValue(Axiom.configuration.builderTools.middleClick == BuilderToolMiddleClick.EXTEND_SELECT);
      this.addRenderableWidget(
         middleClickButton.withValues(new Object[]{true, false})
            .create(this.width / 2 - 91, this.height - 110, 182, 20, Component.translatable("axiom.config.builder_tools.middle_click"), (button, value) -> {
               if ((Boolean)value) {
                  Axiom.configuration.builderTools.middleClick = BuilderToolMiddleClick.EXTEND_SELECT;
               } else {
                  Axiom.configuration.builderTools.middleClick = BuilderToolMiddleClick.MAGIC_SELECT;
               }
            })
      );
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      if (Minecraft.getInstance().cameraEntity instanceof Player player) {
         int var15 = this.width / 2;
         int slotCount = BuilderToolManager.getToolCount();
         int slot = BuilderToolManager.getToolSlotSelected();
         String string = BuilderToolManager.getToolName(slot);
         if (player.getMainArm() == HumanoidArm.LEFT) {
            VersionUtilsClient.blit256(guiGraphics, TOOL_SWAPPER_LOCATION, var15 - 91 - 29, this.height - 2 - slotCount * 20, 0, 0, 22, 2 + slotCount * 20);
            VersionUtilsClient.blit256(guiGraphics, TOOL_SWAPPER_LOCATION, var15 - 91 - 30, this.height - 23 - 20 * slot, 22, 0, 24, 24);
            int width = this.font.width(string);
            guiGraphics.drawString(this.font, string, var15 - 91 - 31 - width, this.height - 15 - 20 * slot, -1);

            for (int i = 0; i < slotCount; i++) {
               int x = var15 - 91 - 26;
               int y = this.height - 19 - 20 * i;
               VersionUtilsClient.blit256(guiGraphics, TOOL_SWAPPER_LOCATION, x, y, 46 + 16 * i, 0, 16, 16);
               BuilderTool tool = BuilderToolManager.getToolForIndex(i);
               if (!AxiomClient.hasPermissions(tool.requiredPermissions())) {
                  guiGraphics.fill(x, y, x + 16, y + 16, -1442840576);
               }
            }
         } else {
            VersionUtilsClient.blit256(guiGraphics, TOOL_SWAPPER_LOCATION, var15 + 98, this.height - 2 - slotCount * 20, 0, 0, 22, 2 + slotCount * 20);
            VersionUtilsClient.blit256(guiGraphics, TOOL_SWAPPER_LOCATION, var15 + 98 - 1, this.height - 23 - 20 * slot, 22, 0, 24, 24);
            guiGraphics.drawString(this.font, string, var15 + 98 + 24, this.height - 15 - 20 * slot, -1);

            for (int ix = 0; ix < slotCount; ix++) {
               int x = var15 + 101;
               int y = this.height - 19 - 20 * ix;
               VersionUtilsClient.blit256(guiGraphics, TOOL_SWAPPER_LOCATION, x, y, 46 + 16 * ix, 0, 16, 16);
               BuilderTool tool = BuilderToolManager.getToolForIndex(ix);
               if (!AxiomClient.hasPermissions(tool.requiredPermissions())) {
                  guiGraphics.fill(x, y, x + 16, y + 16, -1442840576);
               }
            }
         }
      }
   }

   public boolean mouseReleased(double x, double y, int button) {
      boolean ret = super.mouseReleased(x, y, button);
      this.clearFocus();
      return ret;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.mouseClicked(new LegacyMouseButtonEvent(mouseX, mouseY, button), false);
   }

   public boolean mouseClicked(LegacyMouseButtonEvent event, boolean doubleClicked) {
      if (Minecraft.getInstance().cameraEntity instanceof Player player) {
         double mouseX = event.x();
         double mouseY = event.y();
         int mid = this.width / 2;
         if (player.getMainArm() == HumanoidArm.LEFT) {
            if (mouseX >= mid - 91 - 29 && mouseX <= mid - 91 - 7) {
               int slot = (int)(this.height - mouseY - 1.0) / 20;
               if (slot >= 0 && slot < BuilderToolManager.getToolCount()) {
                  BuilderToolManager.setToolSlotSelected(slot);
               }
            }
         } else if (mouseX >= mid + 98 && mouseX <= mid + 120) {
            int slot = (int)(this.height - mouseY - 1.0) / 20;
            if (slot >= 0 && slot < BuilderToolManager.getToolCount()) {
               BuilderToolManager.setToolSlotSelected(slot);
            }
         }
      }

      return super.mouseClicked(event.x(), event.y(), event.button());
   }

   public void slotSelected(int slot) {
      if (slot < BuilderToolManager.getToolCount()) {
         BuilderToolManager.setToolSlotSelected(slot);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.accumulatedScroll != 0.0 && Math.signum(scrollY) != Math.signum(this.accumulatedScroll)) {
         this.accumulatedScroll = 0.0;
      }

      this.accumulatedScroll += scrollY;
      int i = (int)this.accumulatedScroll;
      if (i == 0) {
         return true;
      } else {
         int slot = BuilderToolManager.getToolSlotSelected();
         int count = BuilderToolManager.getToolCount();
         int start = slot;

         while (true) {
            slot = (int)(slot + Math.signum((float)i));
            if (slot < 0) {
               slot += count;
            }

            if (slot >= count) {
               slot -= count;
            }

            if (slot == start) {
               break;
            }

            if (AxiomClient.hasPermissions(BuilderToolManager.getToolForIndex(slot).requiredPermissions())) {
               BuilderToolManager.setToolSlotSelected(slot);
               break;
            }
         }

         return true;
      }
   }
}
