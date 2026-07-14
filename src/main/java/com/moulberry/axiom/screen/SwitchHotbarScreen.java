package com.moulberry.axiom.screen;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.mojang.blaze3d.systems.RenderSystem;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.ContextMenuManager;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.HotbarManager;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.capabilities.Capability;
import com.moulberry.axiom.configuration.FlightDirection;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.displayentity.DisplayEntityHelper;
import com.moulberry.axiom.integration.ServerIntegration;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.ShaderManager;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.versioning.input_events.LegacyMouseButtonEvent;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.CycleButton.Builder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public class SwitchHotbarScreen extends Screen {
   private static final ResourceLocation HOTBAR_OFFHAND_LEFT_SPRITE = ResourceLocation.parse("hud/hotbar_offhand_left");
   private static final ResourceLocation HOTBAR_OFFHAND_RIGHT_SPRITE = ResourceLocation.parse("hud/hotbar_offhand_right");
   private static final ResourceLocation HOTBAR_SWAPPER_LOCATION = ResourceLocation.parse("axiom:gui/hotbar_swapper.png");
   private static final int GRABBED_FROM_OFFHAND = 1000;
   private int selectedHotbarIndex;
   private double accumulatedScroll = 0.0;
   private int grabbedFrom = -1;
   private ItemStack grabbedItemStack = ItemStack.EMPTY;
   private int clickedElementType = -1;
   private long clickedElementTime = -1L;
   private int clickedButton = -1;
   private int lastMouseX = 0;
   private int lastMouseY = 0;
   private long lastMouseMoveTime = -1L;
   private int hoveredCapability = -1;
   private boolean preventHover = false;
   private boolean optionsOpen = false;
   private Button updateButton = null;
   private final List<AbstractWidget> mainWidgets = new ArrayList<>();
   private final List<AbstractWidget> optionsWidgets = new ArrayList<>();
   private AbstractWidget selectedHotbarPageDownButton = null;

   public SwitchHotbarScreen() {
      super(Component.literal(AxiomI18n.get("axiom.hardcoded.switch_toolbar")));
      this.selectedHotbarIndex = HotbarManager.getActiveHotbarIndex();
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
         if (AxiomClient.hasPermission(AxiomPermission.PLAYER_HOTBAR)) {
            this.selectedHotbarIndex = (int)(this.selectedHotbarIndex + Math.signum((float)i));
            if (this.selectedHotbarIndex < 0) {
               this.selectedHotbarIndex = 0;
            }
         }

         return true;
      }
   }

   protected void init() {
      super.init();
      Button survivalWidget = (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("selectWorld.gameMode.survival"), button -> ServerIntegration.changeGameMode(GameType.SURVIVAL))
            .bounds(this.width / 2 - 91, this.height - 182 - 21, 60, 20)
            .build()
      );
      if (!AxiomClient.hasPermission(AxiomPermission.PLAYER_GAMEMODE_SURVIVAL)) {
         survivalWidget.active = false;
      }

      Button spectatorWidget = (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("selectWorld.gameMode.spectator"), button -> ServerIntegration.changeGameMode(GameType.SPECTATOR))
            .bounds(this.width / 2 - 30, this.height - 182 - 21, 60, 20)
            .build()
      );
      if (!AxiomClient.hasPermission(AxiomPermission.PLAYER_GAMEMODE_SPECTATOR)) {
         spectatorWidget.active = false;
      }

      Button adventureWidget = (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("selectWorld.gameMode.adventure"), button -> ServerIntegration.changeGameMode(GameType.ADVENTURE))
            .bounds(this.width / 2 + 31, this.height - 182 - 21, 60, 20)
            .build()
      );
      if (!AxiomClient.hasPermission(AxiomPermission.PLAYER_GAMEMODE_ADVENTURE)) {
         adventureWidget.active = false;
      }

      this.mainWidgets.add(survivalWidget);
      this.mainWidgets.add(spectatorWidget);
      this.mainWidgets.add(adventureWidget);
      this.mainWidgets
         .add((AbstractWidget)this.addRenderableWidget(Button.builder(Component.translatable("axiom.contextmenu.edit_block_attributes"), button -> {
            ContextMenuManager.getInstance().close();
            Minecraft.getInstance().setScreen(new EditBlockAttributesScreen());
         }).bounds(this.width / 2 - 91, this.height - 182 - 42, 182, 20).build()));
      Button createDisplayEntityWidget = (Button)this.addRenderableWidget(
         Button.builder(
               Component.translatable("axiom.contextmenu.create_display_entity"),
               button -> {
                  ContextMenuManager.getInstance().close();
                  Minecraft.getInstance()
                     .setScreen(new CreateDisplayEntityScreen(null, object -> DisplayEntityHelper.spawnAtPlayer(object, Minecraft.getInstance().player), false));
               }
            )
            .bounds(this.width / 2 - 91, this.height - 182 - 63, 182, 20)
            .build()
      );
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.CREATE_ENTITY)) {
         createDisplayEntityWidget.active = false;
      }

      this.mainWidgets.add(createDisplayEntityWidget);
      if (ClientEvents.updateMessage != null) {
         this.mainWidgets
            .add((AbstractWidget)this.addRenderableWidget(this.updateButton = Button.builder(Component.literal("Parallax Update Available!"), button -> {
               try {
                  Util.getPlatform().openUri(new URI("https://axiom.moulberry.com/download"));
               } catch (Exception var2x) {
               }

               ClientEvents.updateMessage = null;
               Axiom.configuration.internal.nextUpdateNag = System.currentTimeMillis() + ChronoUnit.DAYS.getDuration().getSeconds() * 3000L;
            }).bounds(this.width / 2 - 91, this.height - 182 - 84, 182, 20).build()));
      }

      int hotbarPageX = this.width / 2 + 91 + 5;
      if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainArm() == HumanoidArm.LEFT) {
         hotbarPageX = this.width / 2 - 91 - 5 - 26;
      }

      Button selectedHotbarPageUpButton = (Button)this.addRenderableWidget(
         Button.builder(Component.literal("⬆"), button -> this.selectedHotbarIndex = (this.selectedHotbarIndex / 9 + 1) * 9)
            .bounds(hotbarPageX, this.height - 182 + 14, 12, 12)
            .build()
      );
      if (!AxiomClient.hasPermission(AxiomPermission.PLAYER_HOTBAR)) {
         selectedHotbarPageUpButton.active = false;
      }

      this.mainWidgets.add(selectedHotbarPageUpButton);
      this.selectedHotbarPageDownButton = Button.builder(
            Component.literal("⬇"), button -> this.selectedHotbarIndex = Math.max(0, this.selectedHotbarIndex / 9 - 1) * 9
         )
         .bounds(hotbarPageX + 14, this.height - 182 + 14, 12, 12)
         .build();
      if (!AxiomClient.hasPermission(AxiomPermission.PLAYER_HOTBAR)) {
         this.selectedHotbarPageDownButton.active = false;
      }

      this.mainWidgets.add((AbstractWidget)this.addRenderableWidget(this.selectedHotbarPageDownButton));
      float momentum = Axiom.configuration.movement.flightMomentum;
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               new AbstractSliderButton(
                  this.width / 2 - 91,
                  this.height - 22,
                  182,
                  20,
                  Component.translatable("axiom.contextmenu.flight_momentum", new Object[]{momentum}),
                  momentum / 100.0F
               ) {
                  protected void updateMessage() {
                     float momentumx = Axiom.configuration.movement.flightMomentum;
                     this.setMessage(Component.translatable("axiom.contextmenu.flight_momentum", new Object[]{momentumx}));
                  }

                  protected void applyValue() {
                     Axiom.configuration.movement.flightMomentum = (int)(Mth.clamp(this.value, 0.0, 1.0) * 100.0);
                  }
               }
            )
         );
      Builder<Object> flightDirectionButton = CycleButton.builder(
            v -> (Boolean)v
               ? Component.translatable("axiom.contextmenu.flight_direction.camera")
               : Component.translatable("axiom.contextmenu.flight_direction.horizontal")
         )
         .withInitialValue(Axiom.configuration.movement.flightDirection == FlightDirection.CAMERA);
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               flightDirectionButton.withValues(new Object[]{true, false})
                  .create(this.width / 2 - 91, this.height - 44, 182, 20, Component.translatable("axiom.contextmenu.flight_direction"), (button, value) -> {
                     if ((Boolean)value) {
                        Axiom.configuration.movement.flightDirection = FlightDirection.CAMERA;
                     } else {
                        Axiom.configuration.movement.flightDirection = FlightDirection.HORIZONTAL;
                     }
                  })
            )
         );
      boolean typeReplace = Axiom.configuration.capabilities.typeReplace;
      Component typeReplaceText = Component.translatable("axiom.contextmenu.type_replace");
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               CycleButton.onOffBuilder(typeReplace)
                  .create(
                     this.width / 2 - 91, this.height - 66, 182, 20, typeReplaceText, (button, bool) -> Axiom.configuration.capabilities.typeReplace = bool
                  )
            )
         );
      float minBrightness = Axiom.configuration.visuals.minBrightness;
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               new AbstractSliderButton(
                  this.width / 2 - 91,
                  this.height - 88,
                  182,
                  20,
                  Component.translatable("axiom.contextmenu.min_brightness", new Object[]{minBrightness}),
                  minBrightness / 100.0F
               ) {
                  protected void updateMessage() {
                     float minBrightnessx = Axiom.configuration.visuals.minBrightness;
                     this.setMessage(Component.translatable("axiom.contextmenu.min_brightness", new Object[]{minBrightnessx}));
                  }

                  protected void applyValue() {
                     Axiom.configuration.visuals.minBrightness = (int)(Mth.clamp(this.value, 0.0, 1.0) * 100.0);
                  }
               }
            )
         );
      float liquidOpacity = Axiom.configuration.visuals.liquidOpacity;
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               new AbstractSliderButton(
                  this.width / 2 - 91,
                  this.height - 110,
                  182,
                  20,
                  Component.translatable("axiom.contextmenu.liquid_opacity", new Object[]{liquidOpacity}),
                  liquidOpacity / 100.0F
               ) {
                  protected void updateMessage() {
                     float liquidOpacityx = Axiom.configuration.visuals.liquidOpacity;
                     this.setMessage(Component.translatable("axiom.contextmenu.liquid_opacity", new Object[]{liquidOpacityx}));
                  }

                  protected void applyValue() {
                     Axiom.configuration.visuals.liquidOpacity = (int)(Mth.clamp(this.value, 0.0, 1.0) * 100.0);
                  }
               }
            )
         );
      boolean keypressOverlay = Axiom.configuration.visuals.keypressOverlay;
      Component showKeyPressesText = Component.translatable("axiom.editorui.mainmenu.view.show_key_presses");
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               CycleButton.onOffBuilder(keypressOverlay)
                  .create(
                     this.width / 2 - 91, this.height - 132, 182, 20, showKeyPressesText, (button, bool) -> Axiom.configuration.visuals.keypressOverlay = bool
                  )
            )
         );
      float infiniteReachLimit = Axiom.configuration.capabilities.infiniteReachLimit;
      if (infiniteReachLimit < 5.0F) {
         infiniteReachLimit = 1.0F;
      } else {
         infiniteReachLimit = (infiniteReachLimit - 5.0F) / 96.0F;
      }

      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               new AbstractSliderButton(this.width / 2 - 91, this.height - 154, 182, 20, CommonComponents.EMPTY, infiniteReachLimit) {
                  {
                     this.updateMessage();
                  }

                  protected void updateMessage() {
                     int infiniteReachLimitx = Axiom.configuration.capabilities.infiniteReachLimit;
                     if (infiniteReachLimitx < 0) {
                        this.setMessage(Component.translatable("axiom.contextmenu.infinite_reach_limit_none"));
                     } else {
                        this.setMessage(Component.translatable("axiom.contextmenu.infinite_reach_limit", new Object[]{infiniteReachLimitx}));
                     }
                  }

                  protected void applyValue() {
                     int value = (int)(this.value * 96.0 + 5.0);
                     if (value < 5) {
                        value = 5;
                     }

                     if (value > 100) {
                        Axiom.configuration.capabilities.infiniteReachLimit = -1;
                     } else {
                        Axiom.configuration.capabilities.infiniteReachLimit = value;
                     }
                  }
               }
            )
         );
      boolean showKeyHints = Axiom.configuration.visuals.showKeyHints;
      Component showKeyHintsText = Component.translatable("axiom.contextmenu.show_key_hints");
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               CycleButton.onOffBuilder(showKeyHints)
                  .create(this.width / 2 - 91, this.height - 176, 182, 20, showKeyHintsText, (button, bool) -> Axiom.configuration.visuals.showKeyHints = bool)
            )
         );
      boolean showDisplayEntities = Axiom.configuration.entityManipulation.showDisplayEntities;
      Component showDisplayEntitiesText = Component.translatable("axiom.contextmenu.show_display_entity_gizmos");
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               CycleButton.onOffBuilder(showDisplayEntities)
                  .create(
                     this.width / 2 - 91,
                     this.height - 198,
                     182,
                     20,
                     showDisplayEntitiesText,
                     (button, bool) -> Axiom.configuration.entityManipulation.showDisplayEntities = bool
                  )
            )
         );
      boolean showMarkerEntities = Axiom.configuration.entityManipulation.showMarkerEntities;
      Component showMarkerEntitiesText = Component.translatable("axiom.contextmenu.show_marker_entity_gizmos");
      this.optionsWidgets
         .add(
            (AbstractWidget)this.addRenderableWidget(
               CycleButton.onOffBuilder(showMarkerEntities)
                  .create(
                     this.width / 2 - 91,
                     this.height - 220,
                     182,
                     20,
                     showMarkerEntitiesText,
                     (button, bool) -> Axiom.configuration.entityManipulation.showMarkerEntities = bool
                  )
            )
         );
      this.mainWidgets.forEach(widget -> widget.visible = !this.optionsOpen);
      this.optionsWidgets.forEach(widget -> widget.visible = this.optionsOpen);
   }

   public void onClose() {
      HotbarManager.setActiveHotbarIndex(this.selectedHotbarIndex);
      if (!this.grabbedItemStack.isEmpty()) {
         if (this.grabbedFrom == 1000) {
            if (this.minecraft != null && this.minecraft.player != null) {
               this.minecraft.player.setItemSlot(EquipmentSlot.OFFHAND, this.grabbedItemStack);
            }
         } else if (this.grabbedFrom >= 0) {
            HotbarManager.setItemStack(this.grabbedFrom / 9 + this.selectedHotbarIndex / 9 * 9, this.grabbedFrom % 9, this.grabbedItemStack);
         }
      }

      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         HotbarManager.trySave(player, player.registryAccess(), false);
      }
   }

   public void slotSelected(int index) {
      if (AxiomClient.hasPermission(AxiomPermission.PLAYER_HOTBAR)) {
         this.selectedHotbarIndex = this.selectedHotbarIndex / 9 * 9 + index;
      }
   }

   public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
      super.render(guiGraphics, mouseX, mouseY, f);
      if (this.selectedHotbarPageDownButton != null) {
         this.selectedHotbarPageDownButton.active = this.selectedHotbarIndex >= 9;
      }

      if (this.lastMouseX != mouseX || this.lastMouseY != mouseY) {
         this.lastMouseX = mouseX;
         this.lastMouseY = mouseY;
         this.lastMouseMoveTime = System.currentTimeMillis();
         this.preventHover = false;
      }

      if (this.minecraft != null) {
         Player player = this.minecraft.player;
         if (player != null) {
            List<Component> tooltipToRender = null;
            AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
            HumanoidArm humanoidArm = player.getMainArm();
            int centerX = this.width / 2;
            int mainX;
            int offX;
            if (humanoidArm == HumanoidArm.LEFT) {
               mainX = centerX - 109;
               offX = centerX + 109;
            } else {
               mainX = centerX + 109;
               offX = centerX - 109;
            }

            int flySpeedControlLeft = mainX - 8;
            int flySpeedControlTop = this.height - 138;
            boolean canAdjustSpeed = ClientEvents.serverSupportsProtocol(SupportedProtocol.SET_FLY_SPEED);
            boolean flySpeedHoveredX = canAdjustSpeed && mouseX >= flySpeedControlLeft && mouseX <= flySpeedControlLeft + 16;
            boolean increaseHovered = flySpeedHoveredX && mouseY >= flySpeedControlTop && mouseY <= flySpeedControlTop + 14;
            boolean decreaseHovered = flySpeedHoveredX && mouseY >= flySpeedControlTop + 78 && mouseY <= flySpeedControlTop + 78 + 14;
            Abilities abilities = player.getAbilities();
            if (canAdjustSpeed) {
               if (increaseHovered && this.clickedElementType == 1 && System.currentTimeMillis() - this.clickedElementTime > 300L) {
                  if (abilities.getFlyingSpeed() < 0.5F) {
                     ServerIntegration.changeFlySpeed(Math.min(0.5F, abilities.getFlyingSpeed() + (this.clickedButton == 1 ? 0.005F : 5.0E-4F)));
                  }
               } else if (decreaseHovered && this.clickedElementType == 2 && System.currentTimeMillis() - this.clickedElementTime > 300L) {
                  if (abilities.getFlyingSpeed() > 0.05F) {
                     ServerIntegration.changeFlySpeed(Math.max(0.05F, abilities.getFlyingSpeed() - (this.clickedButton == 1 ? 0.005F : 5.0E-4F)));
                  }
               } else if (this.clickedElementType == 3) {
                  float mouseYF = (float)this.minecraft.mouseHandler.ypos()
                     * this.minecraft.getWindow().getGuiScaledHeight()
                     / this.minecraft.getWindow().getScreenHeight();
                  float barAmount = (flySpeedControlTop + 78 - mouseYF) / 64.0F;
                  barAmount = Math.max(0.0F, Math.min(1.0F, barAmount));
                  float flyingSpeed = 1.0F + 8.99F * barAmount * barAmount;
                  ServerIntegration.changeFlySpeed(flyingSpeed * 0.05F);
               }
            }

            float flySpeed = abilities.getFlyingSpeed() / 0.05F;
            float flyAmount;
            if (flySpeed <= 1.0F) {
               flyAmount = 0.0F;
            } else {
               flyAmount = (float)Math.sqrt((flySpeed - 1.0F) / 8.99F);
               if (flyAmount > 1.0F) {
                  flyAmount = 1.0F;
               }
            }

            String textDisplay;
            if (flySpeed > 9.99999 && flySpeed < 10.00001) {
               textDisplay = "999%";
            } else {
               textDisplay = Math.round(flySpeed * 100.0F) + "%";
            }

            guiGraphics.drawString(
               this.minecraft.font, textDisplay, flySpeedControlLeft + 9 - this.minecraft.font.width(textDisplay) / 2, flySpeedControlTop - 10, -1
            );
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int increaseOffset = this.clickedElementType == 3 ? 0 : (flyAmount >= 0.99999 ? 16 : (increaseHovered ? 32 : 0));
            VersionUtilsClient.blit256(guiGraphics, HOTBAR_SWAPPER_LOCATION, flySpeedControlLeft, flySpeedControlTop, increaseOffset, 164, 16, 14);
            int decreaseOffset = this.clickedElementType == 3 ? 16 : (flyAmount <= 1.0E-5 ? 0 : (decreaseHovered ? 32 : 16));
            VersionUtilsClient.blit256(guiGraphics, HOTBAR_SWAPPER_LOCATION, flySpeedControlLeft, flySpeedControlTop + 78, decreaseOffset, 242, 16, 14);
            int flyAmountI = 64 - (int)Math.floor(flyAmount * 64.0F);
            VersionUtilsClient.blit256(guiGraphics, HOTBAR_SWAPPER_LOCATION, flySpeedControlLeft, flySpeedControlTop + 14, 0, 177, 16, flyAmountI);
            VersionUtilsClient.blit256(
               guiGraphics, HOTBAR_SWAPPER_LOCATION, flySpeedControlLeft, flySpeedControlTop + 14 + flyAmountI, 16, 177 + flyAmountI, 16, 64 - flyAmountI
            );
            if (!canAdjustSpeed) {
               guiGraphics.fill(flySpeedControlLeft, flySpeedControlTop, flySpeedControlLeft + 16, flySpeedControlTop + 92, -1442840576);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            boolean toolboxHovered = mouseX >= mainX - 10 && mouseX <= mainX + 10 && mouseY >= this.height - 44 && mouseY <= this.height - 24;
            VersionUtilsClient.blit256(
               guiGraphics, HOTBAR_SWAPPER_LOCATION, mainX - 10, this.height - 44, this.optionsOpen ? 236 : 216, toolboxHovered ? 20 : 0, 20, 20
            );
            VersionUtilsClient.blit256(guiGraphics, HOTBAR_SWAPPER_LOCATION, mainX - 8, this.height - 42, 0, 104, 16, 16);
            int index = 0;

            for (Capability capability : Capability.values()) {
               boolean hovered = mouseX >= offX - 10
                  && mouseX <= offX + 10
                  && mouseY >= this.height - 44 - 22 * index
                  && mouseY <= this.height - 24 - 22 * index;
               boolean active = capability.isEnabled();
               if (this.hoveredCapability == index) {
                  if (!hovered || this.preventHover) {
                     this.hoveredCapability = -1;
                  } else if (!capability.allowedByServer()) {
                     tooltipToRender = List.of(Component.literal(AxiomI18n.get("axiom.hardcoded.capability_disabled_srv")).withStyle(ChatFormatting.RED));
                  } else {
                     tooltipToRender = List.of(capability.title, capability.description);
                  }
               } else if (hovered && System.currentTimeMillis() - this.lastMouseMoveTime > 300L && !this.preventHover) {
                  this.hoveredCapability = index;
               }

               VersionUtilsClient.blit256(
                  guiGraphics, HOTBAR_SWAPPER_LOCATION, offX - 10, this.height - 44 - 22 * index, active ? 236 : 216, hovered ? 20 : 0, 20, 20
               );
               VersionUtilsClient.blit256(guiGraphics, HOTBAR_SWAPPER_LOCATION, offX - 8, this.height - 42 - 22 * index, 16 * index, active ? 24 : 40, 16, 16);
               index++;
            }

            if (!this.optionsOpen) {
               guiGraphics.pose().pushPose();
               guiGraphics.pose().translate(0.0F, 0.0F, -90.0F);
               VersionUtilsClient.blit256(guiGraphics, HOTBAR_SWAPPER_LOCATION, centerX - 91, this.height - 182, 74, 74, 182, 182);
               guiGraphics.pose().popPose();
               VersionUtilsClient.blit256(
                  guiGraphics, HOTBAR_SWAPPER_LOCATION, centerX - 91 - 1, this.height - 22 - 1 - 20 * (this.selectedHotbarIndex % 9), 0, 0, 184, 24
               );
               if (!AxiomClient.hasPermission(AxiomPermission.PLAYER_HOTBAR)) {
                  guiGraphics.fill(centerX - 91, this.height - 182, centerX + 91, this.height - 23, -1442840576);
               }
            }

            String pageString = "Page " + (this.selectedHotbarIndex / 9 + 1);
            if (humanoidArm == HumanoidArm.LEFT) {
               int pageStringWidth = this.minecraft.font.width(pageString);
               guiGraphics.drawString(this.minecraft.font, pageString, this.width / 2 - 91 - 2 - pageStringWidth, this.height - 182 + 4, -1);
            } else {
               guiGraphics.drawString(this.minecraft.font, pageString, this.width / 2 + 91 + 2, this.height - 182 + 4, -1);
            }

            if (!this.grabbedItemStack.isEmpty() && mouseX >= mainX - 11 && mouseX <= mainX + 13 && mouseY >= this.height - 22 && mouseY <= this.height) {
               VersionUtilsClient.blit256(guiGraphics, HOTBAR_SWAPPER_LOCATION, mainX - 11, this.height - 22, 22, 142, 22, 22);
            } else {
               VersionUtilsClient.blit256(guiGraphics, HOTBAR_SWAPPER_LOCATION, mainX - 11, this.height - 22, 0, 142, 22, 22);
            }

            ItemStack offHandItem = player.getOffhandItem();
            if (humanoidArm == HumanoidArm.LEFT) {
               VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, offX - 11, this.height - 23, 29, 24);
            } else {
               VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, HOTBAR_OFFHAND_RIGHT_SPRITE, offX - 18, this.height - 23, 29, 24);
            }

            if (this.grabbedItemStack.isEmpty() || this.grabbedFrom != 1000) {
               this.renderSlot(guiGraphics, offX - 8, this.height - 19, player, offHandItem, 0);
            }

            if (mouseX >= offX - 8
               && mouseX <= offX + 8
               && mouseY >= this.height - 19
               && mouseY <= this.height - 3
               && (!this.grabbedItemStack.isEmpty() || !offHandItem.isEmpty())) {
               this.renderSlotHover(guiGraphics, offX - 8, this.height - 19);
            }

            if (!this.optionsOpen) {
               int hoveredHotbar = Math.floorDiv(this.height - mouseY, 20);
               int hoveredSlot = Math.floorDiv(mouseX - (centerX - 90), 20);
               if (!AxiomClient.hasPermission(AxiomPermission.PLAYER_HOTBAR) && hoveredHotbar != this.selectedHotbarIndex % 9) {
                  hoveredHotbar = -1;
               }

               for (int hotbar = 8; hotbar >= 0; hotbar--) {
                  for (int item = 0; item < 9; item++) {
                     int x = centerX - 90 + item * 20 + 2;
                     int y = this.height - 16 - 3 - 20 * hotbar;
                     ItemStack itemStack = HotbarManager.getItemStack(hotbar + this.selectedHotbarIndex / 9 * 9, item);
                     if (this.grabbedItemStack.isEmpty() || this.grabbedFrom != hotbar * 9 + item) {
                        this.renderSlot(guiGraphics, x, y, player, itemStack, 0);
                     }

                     if (hotbar == hoveredHotbar && item == hoveredSlot && (!this.grabbedItemStack.isEmpty() || !itemStack.isEmpty())) {
                        this.renderSlotHover(guiGraphics, x, y);
                     }
                  }
               }
            }

            if (!this.grabbedItemStack.isEmpty()) {
               this.renderSlot(guiGraphics, mouseX - 8, mouseY - 8, player, this.grabbedItemStack, 500);
            }

            if (mouseX >= mainX - 11 && mouseX <= mainX + 13 && mouseY >= this.height - 22 && mouseY <= this.height) {
               tooltipToRender = List.of(Component.translatable("inventory.binSlot"));
            }

            if (tooltipToRender != null) {
               List<FormattedCharSequence> sequences = new ArrayList<>();

               for (Component text : tooltipToRender) {
                  sequences.addAll(this.minecraft.font.split(text, Math.max(this.width / 2, 200)));
               }

               guiGraphics.renderTooltip(this.font, sequences, mouseX, mouseY);
            } else if (ClientEvents.updateMessage != null && this.updateButton != null && this.updateButton.isHovered()) {
               List<FormattedCharSequence> sequences = new ArrayList<>();

               for (String text : ClientEvents.updateMessage.split("\n")) {
                  sequences.addAll(this.minecraft.font.split(Component.literal(text), Math.max(this.width / 2, 200)));
               }

               guiGraphics.renderTooltip(this.font, sequences, mouseX, mouseY);
            }

            RenderSystem.disableBlend();
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return this.mouseClicked(new LegacyMouseButtonEvent(mouseX, mouseY, button), false);
   }

   public boolean mouseClicked(LegacyMouseButtonEvent mouseButtonEvent, boolean doubleClicked) {
      int mouseButton = mouseButtonEvent.button();
      double mouseX = mouseButtonEvent.x();
      double mouseY = mouseButtonEvent.y();
      this.clickedElementType = 0;
      this.clickedElementTime = System.currentTimeMillis();
      this.clickedButton = mouseButton;
      if (mouseButton == 0) {
         this.preventHover = true;
      }

      if (this.minecraft == null) {
         return false;
      } else {
         Player player = this.minecraft.player;
         if (player == null) {
            return false;
         } else {
            int centerX = this.width / 2;
            int mainX;
            int offX;
            if (player.getMainArm() == HumanoidArm.LEFT) {
               mainX = centerX - 109;
               offX = centerX + 109;
            } else {
               mainX = centerX + 109;
               offX = centerX - 109;
            }

            int flySpeedControlLeft = mainX - 8;
            int flySpeedControlTop = this.height - 138;
            if (ClientEvents.serverSupportsProtocol(SupportedProtocol.SET_FLY_SPEED) && mouseX >= flySpeedControlLeft && mouseX <= flySpeedControlLeft + 16) {
               Abilities abilities = Minecraft.getInstance().player.getAbilities();
               float flyingSpeed = abilities.getFlyingSpeed();
               float amount = mouseButton == 1 ? 0.05F : 0.005F;
               if ((int)mouseY >= flySpeedControlTop && (int)mouseY <= flySpeedControlTop + 14) {
                  if (flyingSpeed < 1.0F) {
                     float adjustedAmount;
                     if (flyingSpeed < 0.00499F) {
                        adjustedAmount = amount / 10.0F;
                     } else if (flyingSpeed < 0.02499F) {
                        adjustedAmount = amount / 2.0F;
                     } else {
                        adjustedAmount = amount;
                     }

                     Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                     ServerIntegration.changeFlySpeed(Math.min(1.0F, Math.round(flyingSpeed / adjustedAmount + 1.0F) * adjustedAmount));
                  }

                  this.clickedElementType = 1;
                  return true;
               }

               if ((int)mouseY >= flySpeedControlTop + 78 && (int)mouseY <= flySpeedControlTop + 78 + 14) {
                  if (flyingSpeed > 5.0E-4F) {
                     float adjustedAmount;
                     if (flyingSpeed <= 0.00501F) {
                        adjustedAmount = amount / 10.0F;
                     } else if (flyingSpeed <= 0.02501F) {
                        adjustedAmount = amount / 2.0F;
                     } else {
                        adjustedAmount = amount;
                     }

                     Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                     ServerIntegration.changeFlySpeed(Math.max(5.0E-4F, Math.round(flyingSpeed / adjustedAmount - 1.0F) * adjustedAmount));
                  }

                  this.clickedElementType = 2;
                  return true;
               }

               if (mouseY > flySpeedControlTop + 14 && mouseY < flySpeedControlTop + 78) {
                  this.clickedElementType = 3;
                  return true;
               }
            }

            boolean toolboxHovered = mouseX >= mainX - 10 && mouseX <= mainX + 10 && mouseY >= this.height - 44 && mouseY <= this.height - 24;
            if (toolboxHovered) {
               this.optionsOpen = !this.optionsOpen;
               this.mainWidgets.forEach(widget -> widget.visible = !this.optionsOpen);
               this.optionsWidgets.forEach(widget -> widget.visible = this.optionsOpen);
            }

            if (mouseButton == 0) {
               int index = 0;

               for (Capability capability : Capability.values()) {
                  boolean hovered = mouseX >= offX - 10
                     && mouseX <= offX + 10
                     && mouseY >= this.height - 44 - 22 * index
                     && mouseY <= this.height - 24 - 22 * index;
                  if (hovered) {
                     capability.toggle();
                     Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                     return true;
                  }

                  index++;
               }
            }

            if (mouseX >= mainX - 11 && mouseX <= mainX + 13 && mouseY >= this.height - 22 && mouseY <= this.height) {
               if (!this.grabbedItemStack.isEmpty()) {
                  if (this.grabbedFrom == 1000) {
                     player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                     Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, 45);
                     player.inventoryMenu.broadcastChanges();
                  } else {
                     HotbarManager.setItemStack(this.grabbedFrom / 9 + this.selectedHotbarIndex / 9 * 9, this.grabbedFrom % 9, ItemStack.EMPTY);
                  }

                  this.grabbedFrom = -1;
                  this.grabbedItemStack = ItemStack.EMPTY;
               } else if (VersionUtilsClient.wasteFirstBoolean(this.minecraft, Screen.hasShiftDown())) {
                  for (int i = 0; i < 9; i++) {
                     player.getInventory().setItem(i, ItemStack.EMPTY);
                     Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, 36 + i);
                  }

                  player.inventoryMenu.broadcastChanges();
                  HotbarManager.clearPage();
                  this.selectedHotbarIndex = HotbarManager.getActiveHotbarIndex();
               }

               return true;
            } else if (mouseX >= offX - 8 && mouseX <= offX + 8 && mouseY >= this.height - 19 && mouseY <= this.height - 3) {
               ItemStack oldItemStack = player.getOffhandItem();
               if (!this.grabbedItemStack.isEmpty()) {
                  player.setItemSlot(EquipmentSlot.OFFHAND, this.grabbedItemStack);
                  Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(this.grabbedItemStack, 45);
                  if (this.grabbedFrom == 1000) {
                     this.grabbedFrom = -1;
                     this.grabbedItemStack = ItemStack.EMPTY;
                  } else {
                     this.grabbedItemStack = oldItemStack;
                  }
               } else if (!oldItemStack.isEmpty()) {
                  this.grabbedFrom = 1000;
                  this.grabbedItemStack = oldItemStack;
                  player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                  Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, 45);
               }

               player.inventoryMenu.broadcastChanges();
               return true;
            } else {
               if (!this.optionsOpen) {
                  int hoveredHotbar = Math.floorDiv(this.height - (int)mouseY, 20);
                  int hoveredSlot = Math.floorDiv((int)mouseX - (centerX - 90), 20);
                  if ((AxiomClient.hasPermission(AxiomPermission.PLAYER_HOTBAR) || hoveredHotbar == this.selectedHotbarIndex % 9)
                     && hoveredHotbar >= 0
                     && hoveredHotbar <= 8
                     && hoveredSlot >= 0
                     && hoveredSlot <= 8) {
                     ItemStack oldItemStack = HotbarManager.getItemStack(hoveredHotbar + this.selectedHotbarIndex / 9 * 9, hoveredSlot);
                     if (!this.grabbedItemStack.isEmpty()) {
                        if (this.grabbedFrom == hoveredHotbar * 9 + hoveredSlot) {
                           this.grabbedFrom = -1;
                           HotbarManager.setItemStack(hoveredHotbar + this.selectedHotbarIndex / 9 * 9, hoveredSlot, this.grabbedItemStack);
                           this.grabbedItemStack = ItemStack.EMPTY;
                        } else {
                           HotbarManager.setItemStack(hoveredHotbar + this.selectedHotbarIndex / 9 * 9, hoveredSlot, this.grabbedItemStack);
                           this.grabbedItemStack = oldItemStack;
                        }
                     } else if (!oldItemStack.isEmpty()) {
                        this.grabbedFrom = hoveredHotbar * 9 + hoveredSlot;
                        this.grabbedItemStack = oldItemStack;
                        HotbarManager.setItemStack(hoveredHotbar + this.selectedHotbarIndex / 9 * 9, hoveredSlot, ItemStack.EMPTY);
                     } else {
                        this.selectedHotbarIndex = this.selectedHotbarIndex / 9 * 9 + hoveredHotbar;
                     }

                     return true;
                  }
               }

               return super.mouseClicked(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
            }
         }
      }
   }

   public boolean mouseReleased(double x, double y, int button) {
      this.clickedElementType = -1;
      this.clickedButton = -1;
      this.mainWidgets.forEach(widget -> widget.setFocused(false));
      this.optionsWidgets.forEach(widget -> widget.setFocused(false));
      return super.mouseReleased(x, y, button);
   }

   private void renderSlotHover(GuiGraphics guiGraphics, int x, int y) {
      RenderSystem.disableDepthTest();
      RenderSystem.colorMask(true, true, true, false);
      guiGraphics.fill(x, y, x + 16, y + 16, -2130706433);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableDepthTest();
   }

   private void renderSlot(GuiGraphics guiGraphics, int x, int y, Player player, ItemStack itemStack, int offset) {
      if (!itemStack.isEmpty() && this.minecraft != null) {
         if (offset != 0) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, offset);
         }

         guiGraphics.renderItem(player, itemStack, x, y, offset);
         ShaderManager.enablePositionColorShader();
         guiGraphics.renderItemDecorations(this.minecraft.font, itemStack, x, y);
         if (offset != 0) {
            guiGraphics.pose().popPose();
         }
      }
   }
}
