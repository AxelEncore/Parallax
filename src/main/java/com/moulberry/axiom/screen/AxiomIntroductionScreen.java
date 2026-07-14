package com.moulberry.axiom.screen;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.configuration.FlightDirection;
import com.moulberry.axiom.editor.keybinds.KeybindDefaults;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class AxiomIntroductionScreen extends Screen {
   private List<FormattedCharSequence> message = new ArrayList<>();
   private Button vanillaFlight = null;
   private Button enhancedFlight = null;

   public AxiomIntroductionScreen() {
      super(Component.literal(AxiomI18n.get("axiom.hardcoded.welcome_parallax")).withStyle(ChatFormatting.YELLOW));
   }

   protected void init() {
      super.init();
      this.initStage1();
   }

   protected void clearWidgets() {
      super.clearWidgets();
      this.vanillaFlight = null;
      this.enhancedFlight = null;
   }

   private Component keybind(String id) {
      return Component.empty().append("[").append(Component.keybind(id)).append("]").withStyle(ChatFormatting.GREEN);
   }

   private void initStage1() {
      this.clearWidgets();
      Component builderMode = Component.literal(AxiomI18n.get("axiom.hardcoded.builder_mode")).withStyle(ChatFormatting.LIGHT_PURPLE);
      Component editorMode = Component.literal(AxiomI18n.get("axiom.hardcoded.editor_mode")).withStyle(ChatFormatting.LIGHT_PURPLE);
      Component text = Component.empty()
         .append("It seems like this is your first time...\n\n")
         .append("To access the ")
         .append(builderMode)
         .append(" menu, hold ")
         .append(this.keybind("axiom.keybind.context"))
         .append("\n")
         .append("To open/close the ")
         .append(editorMode)
         .append(", press ")
         .append(this.keybind("axiom.keybind.toggle_editor_ui"))
         .append("\n\n")
         .append("To view this introduction again, run ")
         .append(Component.literal("/axiomintro").withStyle(ChatFormatting.YELLOW))
         .append("\n")
         .append("To view Parallax's web documentation, run ")
         .append(Component.literal("/axiomdocs").withStyle(ChatFormatting.YELLOW))
         .append("\n")
         .append("To open Parallax's config documentation, run ")
         .append(Component.literal("/axiomconfig").withStyle(ChatFormatting.YELLOW))
         .append("\n\n")
         .append("Would you like to run through a quick introduction?");
      this.message = this.font.split(text, this.width - 50);
      int buttonY = this.message.size() * 9;
      int clampedY = Mth.clamp(90 + buttonY + 12, this.height / 6 + 96, this.height - 24);
      OnPress cancel = button -> Minecraft.getInstance().setScreen(null);
      OnPress proceed = button -> this.initStage2();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, cancel).bounds((this.width - 150) / 2, clampedY, 74, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CONTINUE, proceed).bounds((this.width - 150) / 2 + 76, clampedY, 74, 20).build());
   }

   private void initStage2() {
      this.clearWidgets();
      Component editorMode = Component.literal(AxiomI18n.get("axiom.hardcoded.editor_mode")).withStyle(ChatFormatting.LIGHT_PURPLE);
      Component builderTools = Component.literal(AxiomI18n.get("axiom.hardcoded.builder_tools_lbl")).withStyle(ChatFormatting.GOLD);
      Component text = Component.empty()
         .append(builderTools)
         .append("\n\n")
         .append("These tools are simple and are intended to ease structural/small scale building where use of the more powerful ")
         .append(editorMode)
         .append(" is too slow\n\n")
         .append("To start using the tools, scroll to the 10th slot or press ")
         .append(this.keybind("axiom.keybind.builder_tool_slot"))
         .append("\n")
         .append("To swap to a different tool, hold ")
         .append(this.keybind("axiom.keybind.context"))
         .append(" and scroll");
      this.message = this.font.split(text, this.width - 50);
      int buttonY = this.message.size() * 9;
      int clampedY = Mth.clamp(90 + buttonY + 12, this.height / 6 + 96, this.height - 24);
      OnPress cancel = button -> this.initStage1();
      OnPress proceed = button -> this.initStage3();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, cancel).bounds((this.width - 150) / 2, clampedY, 74, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CONTINUE, proceed).bounds((this.width - 150) / 2 + 76, clampedY, 74, 20).build());
   }

   private void initStage3() {
      this.clearWidgets();
      Component builderMode = Component.literal(AxiomI18n.get("axiom.hardcoded.builder_mode")).withStyle(ChatFormatting.LIGHT_PURPLE);
      Component text = Component.empty()
         .append("Would you like to try Enhanced Flight?\n")
         .append("Enhanced Flight changes flight direction to be 3D and removes momentum\n")
         .append("You can always adjust the flight settings in the ")
         .append(builderMode)
         .append(" toolbox later");
      this.message = this.font.split(text, this.width - 50);
      int buttonY = this.message.size() * 9;
      int clampedY = Mth.clamp(90 + buttonY + 12, this.height / 6 + 96, this.height - 24);
      OnPress vanilla = button -> {
         Axiom.configuration.movement.flightMomentum = 100;
         Axiom.configuration.movement.flightDirection = FlightDirection.HORIZONTAL;
      };
      OnPress enhanced = button -> {
         Axiom.configuration.movement.flightMomentum = 0;
         Axiom.configuration.movement.flightDirection = FlightDirection.CAMERA;
      };
      this.vanillaFlight = (Button)this.addRenderableWidget(
         Button.builder(Component.literal(AxiomI18n.get("axiom.hardcoded.vanilla")), vanilla).bounds((this.width - 150) / 2, clampedY, 74, 20).build()
      );
      this.enhancedFlight = (Button)this.addRenderableWidget(
         Button.builder(Component.literal(AxiomI18n.get("axiom.hardcoded.enhanced")), enhanced).bounds((this.width - 150) / 2 + 76, clampedY, 74, 20).build()
      );
      OnPress back = button -> this.initStage2();
      OnPress proceed = button -> this.initStage4();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, back).bounds((this.width - 150) / 2, clampedY + 42, 74, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CONTINUE, proceed).bounds((this.width - 150) / 2 + 76, clampedY + 42, 74, 20).build());
   }

   private void initStage4() {
      this.clearWidgets();
      Component editorMode = Component.literal(AxiomI18n.get("axiom.hardcoded.editor_mode")).withStyle(ChatFormatting.LIGHT_PURPLE);
      Component text = Component.empty()
         .append(editorMode)
         .append(" default keybinds\n\n")
         .append("Would you like to use the default Parallax keybinds or Blender-like keybinds?\n")
         .append("You can always adjust the keybinds later in the ")
         .append(editorMode)
         .append(" through Help > Keybinds");
      this.message = this.font.split(text, this.width - 50);
      int buttonY = this.message.size() * 9;
      int clampedY = Mth.clamp(90 + buttonY + 12, this.height / 6 + 96, this.height - 24);
      OnPress axiomDefaults = button -> KeybindDefaults.loadAxiom();
      OnPress blenderLike = button -> KeybindDefaults.loadBlender();
      this.addRenderableWidget(Button.builder(Component.literal("Parallax Default"), axiomDefaults).bounds((this.width - 150) / 2, clampedY, 74, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal(AxiomI18n.get("axiom.hardcoded.blender_like")), blenderLike).bounds((this.width - 150) / 2 + 76, clampedY, 74, 20).build());
      OnPress back = button -> this.initStage3();
      OnPress proceed = button -> this.initStage5();
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, back).bounds((this.width - 150) / 2, clampedY + 42, 74, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CONTINUE, proceed).bounds((this.width - 150) / 2 + 76, clampedY + 42, 74, 20).build());
   }

   private void initStage5() {
      this.clearWidgets();
      Component text = Component.empty()
         .append(Component.literal(AxiomI18n.get("axiom.hardcoded.intro_done")).withStyle(ChatFormatting.GOLD))
         .append("\n\n")
         .append("I hope you enjoy using Parallax\n\n")
         .append("To view this introduction again, run ")
         .append(Component.literal("/axiomintro").withStyle(ChatFormatting.YELLOW))
         .append("\n")
         .append("To view Parallax's web documentation, run ")
         .append(Component.literal("/axiomdocs").withStyle(ChatFormatting.YELLOW));
      this.message = this.font.split(text, this.width - 50);
      int buttonY = this.message.size() * 9;
      int clampedY = Mth.clamp(90 + buttonY + 12, this.height / 6 + 96, this.height - 24);
      OnPress back = button -> this.initStage4();
      OnPress done = button -> Minecraft.getInstance().setScreen(null);
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, back).bounds((this.width - 150) / 2, clampedY, 74, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, done).bounds((this.width - 150) / 2 + 76, clampedY, 74, 20).build());
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      int flightMomentum = Axiom.configuration.movement.flightMomentum;
      FlightDirection flightDirection = Axiom.configuration.movement.flightDirection;
      if (this.vanillaFlight != null) {
         this.vanillaFlight.active = flightMomentum != 100 || flightDirection != FlightDirection.HORIZONTAL;
      }

      if (this.enhancedFlight != null) {
         this.enhancedFlight.active = flightMomentum != 0 || flightDirection != FlightDirection.CAMERA;
      }

      super.render(guiGraphics, mouseX, mouseY, partialTick);
      guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 70, -1);
      int y = 90;

      for (FormattedCharSequence line : this.message) {
         guiGraphics.drawCenteredString(this.font, line, this.width / 2, y, -1);
         y += 9;
      }
   }
}
