package com.moulberry.axiom.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Transformation;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.StringProperty;
import com.moulberry.axiom.displayentity.DisplayEntityObject;
import com.moulberry.axiom.displayentity.ItemList;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.render.BlockRenderCache;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import com.moulberry.axiom.utils.StringUtils;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.CycleButton.Builder;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.TextDisplay.Align;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.apache.commons.lang3.text.WordUtils;
import org.joml.Matrix4f;

public class CreateDisplayEntityScreen extends Screen {
   private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.parse("container/creative_inventory/scroller");
   private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.parse("container/creative_inventory/scroller_disabled");
   private static final ResourceLocation[] UNSELECTED_TOP_TABS = new ResourceLocation[]{
      ResourceLocation.parse("container/creative_inventory/tab_top_unselected_1"),
      ResourceLocation.parse("container/creative_inventory/tab_top_unselected_2"),
      ResourceLocation.parse("container/creative_inventory/tab_top_unselected_3")
   };
   private static final ResourceLocation[] SELECTED_TOP_TABS = new ResourceLocation[]{
      ResourceLocation.parse("container/creative_inventory/tab_top_selected_1"),
      ResourceLocation.parse("container/creative_inventory/tab_top_selected_2"),
      ResourceLocation.parse("container/creative_inventory/tab_top_selected_3")
   };
   private static final ResourceLocation BACKGROUND = ResourceLocation.parse("axiom:background");
   private static final ResourceLocation SELECT_ITEM = ResourceLocation.parse("axiom:select_item");
   private static final ResourceLocation SELECT_BLOCK = ResourceLocation.parse("axiom:select_block");
   private static final ResourceLocation FILTER_OFF = ResourceLocation.parse("axiom:filter_off");
   private static final ResourceLocation SLOT = ResourceLocation.parse("axiom:slot");
   private static final int SCREEN_TYPE_ITEM = 0;
   private static final int SCREEN_TYPE_BLOCK = 1;
   private static final int SCREEN_TYPE_TEXT = 2;
   private static final int SCREEN_TYPE_SELECT_ITEM = 3;
   private static final int SCREEN_TYPE_SELECT_BLOCK = 4;
   private static final int SCREEN_TYPE_ADDITIONAL = 5;
   private int lastScreenType = 0;
   private int screenType = 0;
   private static final int HOVERED_ITEM_SLOT = 0;
   private static final int HOVERED_FILTER_CUSTOMMODELDATA_ITEMS = 1;
   private static final int HOVERED_SCROLLER = 2;
   private static final int HOVERED_ITEM_DISPLAY_TAB = 3;
   private static final int HOVERED_BLOCK_DISPLAY_TAB = 4;
   private static final int HOVERED_TEXT_DISPLAY_TAB = 5;
   private static final int HOVERED_BLOCK_SLOT = 6;
   private int hovered = -1;
   private EditBox searchBox = null;
   private float scrollOffset = 0.0F;
   private boolean scrolling = false;
   private boolean onlyCustomItems = false;
   private int leftPos;
   private int topPos;
   private int menuWidth;
   private int menuHeight;
   private ItemStack item = new ItemStack(Items.STONE);
   private ItemDisplayContext itemDisplayContext = ItemDisplayContext.NONE;
   private CustomBlockState blockState = (CustomBlockState)Blocks.STONE.defaultBlockState();
   private Align textAlignment = Align.CENTER;
   private String jsonText = "";
   private boolean defaultBackground = false;
   private int backgroundColour = 1073741824;
   private int lineWidth = 200;
   private boolean seeThrough = false;
   private boolean shadow = false;
   private int textOpacity = 255;
   private BillboardConstraints billboardConstraints = BillboardConstraints.FIXED;
   private Transformation defaultItemDisplayTransformation = null;
   private Brightness defaultItemDisplayBrightness = null;
   private boolean overrideBrightness = false;
   private int overrideBrightnessBlock = 0;
   private int overrideBrightnessSky = 0;
   private int glowColourOverride = -1;
   private float displayEntityHeight = 0.0F;
   private float displayEntityWidth = 0.0F;
   private int interpolationDuration = 0;
   private int teleportDuration = 0;
   private float shadowRadius = 0.0F;
   private float shadowStrength = 1.0F;
   private float viewRange = 1.0F;
   private boolean canSwitchType;
   private Consumer<DisplayEntityObject> callback;
   private boolean callbackOnClose;
   private static final ItemDisplayContext[] REORDERED_ITEM_DISPLAY_CONTEXT = new ItemDisplayContext[]{
      ItemDisplayContext.NONE,
      ItemDisplayContext.HEAD,
      ItemDisplayContext.GUI,
      ItemDisplayContext.GROUND,
      ItemDisplayContext.FIXED,
      ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
      ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
      ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
      ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
   };
   private static final ItemStack[] TAB_ITEM_STACKS = new ItemStack[]{
      new ItemStack(Items.ITEM_FRAME), new ItemStack(Items.STONE), new ItemStack(Items.WRITABLE_BOOK)
   };

   public CreateDisplayEntityScreen(DisplayEntityObject editing, Consumer<DisplayEntityObject> callback, boolean callbackOnClose) {
      super(Component.translatable("axiom.create_display_entity.title"));
      this.callbackOnClose = callbackOnClose;
      if (editing == null) {
         this.canSwitchType = true;
      } else {
         DisplayEntityObject.GenericDisplayEntityData genericData = editing.genericDisplayEntityData();
         this.billboardConstraints = genericData.billboardConstraints();
         this.overrideBrightness = genericData.overrideBrightness();
         this.overrideBrightnessBlock = genericData.overrideBrightnessBlock();
         this.overrideBrightnessSky = genericData.overrideBrightnessSky();
         this.glowColourOverride = genericData.glowColorOverride();
         this.displayEntityHeight = genericData.height();
         this.displayEntityWidth = genericData.width();
         this.interpolationDuration = genericData.interpolationDuration();
         this.teleportDuration = genericData.teleportDuration();
         this.shadowRadius = genericData.shadowRadius();
         this.shadowStrength = genericData.shadowStrength();
         this.viewRange = genericData.viewRange();
         if (editing instanceof DisplayEntityObject.DisplayEntityItemObject displayEntityItemObject) {
            this.canSwitchType = false;
            this.screenType = this.lastScreenType = 0;
            this.item = displayEntityItemObject.itemStack().copy();
            this.itemDisplayContext = displayEntityItemObject.itemDisplayContext();
         } else if (editing instanceof DisplayEntityObject.DisplayEntityBlockObject displayEntityBlockObject) {
            this.canSwitchType = false;
            this.screenType = this.lastScreenType = 1;
            this.blockState = (CustomBlockState)displayEntityBlockObject.blockState();
         } else {
            if (!(editing instanceof DisplayEntityObject.DisplayEntityTextObject displayEntityTextObject)) {
               throw new FaultyImplementationError();
            }

            this.canSwitchType = false;
            this.screenType = this.lastScreenType = 2;
            this.textAlignment = displayEntityTextObject.alignment();
            this.jsonText = displayEntityTextObject.text();
            this.defaultBackground = displayEntityTextObject.defaultBackground();
            this.backgroundColour = displayEntityTextObject.background();
            this.lineWidth = displayEntityTextObject.lineWidth();
            this.seeThrough = displayEntityTextObject.seeThrough();
            this.shadow = displayEntityTextObject.shadow();
            this.textOpacity = displayEntityTextObject.textOpacity();
         }
      }

      this.callback = callback;
   }

   protected void init() {
      super.init();
      this.searchBox = null;
      this.clearWidgets();
      switch (this.screenType) {
         case 1:
            this.initBlock();
            break;
         case 2:
            this.initText();
            break;
         case 3:
            this.initSelectItem();
            break;
         case 4:
            this.initSelectBlock();
            break;
         case 5:
            this.initAdditional();
            break;
         default:
            this.initItem();
      }
   }

   private void initItem() {
      boolean hasDyeOptions = ItemStackDataHelper.canBeDyed(this.item);
      this.menuWidth = 195;
      this.menuHeight = 124;
      if (hasDyeOptions) {
         this.menuHeight += 32;
         this.menuHeight = this.menuHeight + (DyeColor.values().length + 3) / 4 * 22;
      }

      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      int y = this.topPos + 36;
      Builder<ItemDisplayContext> itemDisplayCycleButton = CycleButton.<ItemDisplayContext>builder(CreateDisplayEntityScreen::enumToComponent)
         .withInitialValue(this.itemDisplayContext);
      this.addRenderableWidget(
         itemDisplayCycleButton.withValues(REORDERED_ITEM_DISPLAY_CONTEXT)
            .create(
               this.leftPos + 8,
               y,
               179,
               20,
               Component.translatable("axiom.create_display_entity.item_display"),
               (button, value) -> this.itemDisplayContext = value
            )
      );
      if (hasDyeOptions) {
         int var14 = y + 22;
         Component dyeColorComponent = Component.translatable("axiom.create_display_entity.dye_color");
         this.addRenderableWidget(
            new CreateDisplayEntityScreen.BasicStringWidget(
               this.leftPos + 9, var14 + 1, 0, 0, dyeColorComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font
            )
         );
         y = var14 + 10;
         EditBox colourEditBox = new EditBox(this.font, this.leftPos + 8, y, 179, 20, dyeColorComponent);
         colourEditBox.setValue(String.format("%06x", ItemStackDataHelper.getDyeColor(this.item) & 16777215));
         colourEditBox.setResponder(string -> {
            try {
               int newDyedColour = (int)(Long.parseLong(string, 16) & 16777215L);
               ItemStackDataHelper.setDyeColor(this.item, newDyedColour);
               colourEditBox.setTextColor(-1);
            } catch (Exception var4x) {
               colourEditBox.setTextColor(-65536);
            }
         });
         this.addRenderableWidget(colourEditBox);
         int index = 0;

         for (DyeColor dyeColor : DyeColor.values()) {
            int xIndex = index % 4;
            if (xIndex == 0) {
               y += 22;
            }

            int x = this.leftPos + 8 + 45 * xIndex;
            String name = dyeColor.getName().replace("_", " ");
            name = WordUtils.capitalizeFully(name);
            this.addRenderableWidget(Button.builder(Component.literal(name), a -> {
               int rgb = dyeColor.getTextureDiffuseColor();
               colourEditBox.setValue(String.format("%06x", rgb & 16777215));
               colourEditBox.setTextColor(-1);
               ItemStackDataHelper.setDyeColor(this.item, rgb);
            }).bounds(x, y, 43, 20).build());
            index++;
         }
      }

      this.initGenericEnd(y, 179);
   }

   private void initBlock() {
      this.menuWidth = 195;
      this.menuHeight = 102 + this.blockState.getProperties().size() * 22;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      int y = this.topPos + 14;

      for (final Property<?> property : this.blockState.getProperties()) {
         y += 22;
         Comparable<?> propertyValue = this.blockState.getProperty((Property)property);
         final String propertyName = StringUtils.convertSnakeToWords(property.getName());
         if (property instanceof EnumProperty<?> enumProperty) {
            Collection<?> values = enumProperty.getPossibleValues();
            Builder<Object> cycleButton = CycleButton.builder(CreateDisplayEntityScreen::enumToComponent).withInitialValue(propertyValue);
            this.addRenderableWidget(
               cycleButton.withValues(values.toArray(new Object[0]))
                  .create(
                     this.leftPos + 8,
                     y,
                     179,
                     20,
                     Component.literal(propertyName),
                     (button, value) -> this.blockState = this.blockState.setPropertyUnsafe(property, (Comparable<?>)value)
                  )
            );
         } else if (property instanceof StringProperty stringProperty) {
            Collection<String> values = stringProperty.getPossibleValues();
            Builder<String> cycleButton = CycleButton.builder(CreateDisplayEntityScreen::stringToComponent).withInitialValue((String)propertyValue);
            this.addRenderableWidget(
               cycleButton.withValues((String[])values.toArray(new String[0]))
                  .create(
                     this.leftPos + 8,
                     y,
                     179,
                     20,
                     Component.literal(propertyName),
                     (button, value) -> this.blockState = this.blockState.setPropertyUnsafe(property, value)
                  )
            );
         } else if (property instanceof IntegerProperty integerProperty) {
            final int min = integerProperty.min;
            final int max = integerProperty.max;
            final IntWrapper current = new IntWrapper((Integer)propertyValue);
            this.addRenderableWidget(
               new AbstractSliderButton(
                  this.leftPos + 8, y, 179, 20, Component.literal(propertyName + ": " + current.value), (float)(current.value - min) / (max - min)
               ) {
                  protected void updateMessage() {
                     this.setMessage(Component.literal(propertyName + ": " + current.value));
                  }

                  protected void applyValue() {
                     current.value = (int)(this.value * (max - min) + min);
                     CreateDisplayEntityScreen.this.blockState = CreateDisplayEntityScreen.this.blockState.setPropertyUnsafe(property, current.value);
                  }
               }
            );
         } else if (property instanceof BooleanProperty) {
            Builder<Boolean> cycleButton = CycleButton.builder(CreateDisplayEntityScreen::booleanToComponent).withInitialValue((Boolean)propertyValue);
            this.addRenderableWidget(
               cycleButton.withValues(new Boolean[]{Boolean.TRUE, Boolean.FALSE})
                  .create(
                     this.leftPos + 8,
                     y,
                     179,
                     20,
                     Component.literal(propertyName),
                     (button, value) -> this.blockState = this.blockState.setPropertyUnsafe(property, value)
                  )
            );
         }
      }

      this.initGenericEnd(y, 179);
   }

   private void initText() {
      this.menuWidth = 320;
      this.menuHeight = 198;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      int itemWidth = 150;
      Component name = Component.translatable("axiom.create_display_entity.text");
      MultiLineEditBox textEditBox = new MultiLineEditBox(this.font, this.leftPos + 8, this.topPos + 19, itemWidth, 110, name, name);
      textEditBox.setValue(this.jsonText);
      textEditBox.setValueListener(string -> this.jsonText = string);
      this.addRenderableWidget(textEditBox);
      int x = this.leftPos + 8 + 160 - 6;
      int y = this.topPos + 19;
      Component backgroundColorComponent = Component.translatable("axiom.create_display_entity.background_color");
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, backgroundColorComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font
         )
      );
      y += 10;
      this.addRenderableWidget(
         CycleButton.onOffBuilder(this.defaultBackground)
            .create(x, y, itemWidth, 20, Component.translatable("axiom.create_display_entity.default_background"), (button, bool) -> {
               this.defaultBackground = bool;
               this.init();
            })
      );
      if (!this.defaultBackground) {
         y += 20;
         EditBox backgroundColourEditBox = new EditBox(this.font, x, y, itemWidth, 20, backgroundColorComponent);
         backgroundColourEditBox.setValue(ColourUtils.formatHex(this.backgroundColour));
         backgroundColourEditBox.setResponder(string -> {
            try {
               this.backgroundColour = (int)Long.parseLong(string, 16);
               backgroundColourEditBox.setTextColor(-1);
            } catch (Exception var4x) {
               this.backgroundColour = 1073741824;
               backgroundColourEditBox.setTextColor(-65536);
            }
         });
         this.addRenderableWidget(backgroundColourEditBox);
      }

      y += 22;
      Component lineWidthComponent = Component.translatable("axiom.create_display_entity.line_width");
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(x + 1, y + 1, 0, 0, lineWidthComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font)
      );
      y += 10;
      EditBox lineWidthEditBox = new EditBox(this.font, x, y, itemWidth, 20, lineWidthComponent);
      lineWidthEditBox.setValue(Long.toString(this.lineWidth & 4294967295L));
      lineWidthEditBox.setResponder(string -> {
         try {
            this.lineWidth = (int)Long.parseLong(string);
            lineWidthEditBox.setTextColor(-1);
         } catch (Exception var4x) {
            this.lineWidth = 200;
            lineWidthEditBox.setTextColor(-65536);
         }
      });
      this.addRenderableWidget(lineWidthEditBox);
      y += 22;
      Builder<StringRepresentable> alignmentButtonBuilder = CycleButton.builder(CreateDisplayEntityScreen::toComponent).withInitialValue(this.textAlignment);
      CycleButton<?> alignmentButton = alignmentButtonBuilder.withValues(Align.values())
         .create(x, y, itemWidth, 20, Component.translatable("axiom.create_display_entity.alignment"), (button, value) -> this.textAlignment = (Align)value);
      this.addRenderableWidget(alignmentButton);
      y += 22;
      this.addRenderableWidget(
         CycleButton.onOffBuilder(this.seeThrough)
            .create(x, y, itemWidth, 20, Component.translatable("axiom.create_display_entity.see_through"), (button, bool) -> this.seeThrough = bool)
      );
      y += 22;
      this.addRenderableWidget(
         CycleButton.onOffBuilder(this.shadow)
            .create(x, y, itemWidth, 20, Component.translatable("axiom.create_display_entity.shadow"), (button, bool) -> this.shadow = bool)
      );
      y += 22;
      Component initialTextOpacityTitle = Component.translatable(
         "axiom.create_display_entity.text_opacity", new Object[]{Component.literal(String.valueOf(this.textOpacity))}
      );
      this.addRenderableWidget(
         new AbstractSliderButton(x, y, itemWidth, 20, initialTextOpacityTitle, this.textOpacity / 255.0F) {
            protected void updateMessage() {
               Component title = Component.translatable(
                  "axiom.create_display_entity.text_opacity", new Object[]{Component.literal(String.valueOf(CreateDisplayEntityScreen.this.textOpacity))}
               );
               this.setMessage(title);
            }

            protected void applyValue() {
               CreateDisplayEntityScreen.this.textOpacity = Mth.clamp((int)(this.value * 255.0), 0, 255);
            }
         }
      );
      y -= 60;
      this.initGenericEnd(y, itemWidth);
   }

   private void initGenericEnd(int y, int width) {
      y += 30;
      this.addRenderableWidget(Button.builder(Component.translatable("axiom.create_display_entity.additional_settings"), button -> {
         this.lastScreenType = this.screenType;
         this.screenType = 5;
         this.init();
      }).bounds(this.leftPos + 8, y, width, 20).build());
      y += 30;
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_CANCEL, button -> Minecraft.getInstance().setScreen(null))
            .bounds(this.leftPos + 8, y, (width - 1) / 2, 20)
            .build()
      );
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
         Minecraft.getInstance().setScreen(null);
         this.callback.accept(this.createDisplayEntityObject());
      }).bounds(this.leftPos + 8 + (width + 1) / 2, y, (width - 1) / 2, 20).build());
   }

   private DisplayEntityObject createDisplayEntityObject() {
      DisplayEntityObject.GenericDisplayEntityData genericDisplayEntityData = new DisplayEntityObject.GenericDisplayEntityData(
         this.displayEntityWidth,
         this.displayEntityHeight,
         this.interpolationDuration,
         this.teleportDuration,
         this.glowColourOverride,
         this.billboardConstraints,
         this.overrideBrightness,
         this.overrideBrightnessBlock,
         this.overrideBrightnessSky,
         this.shadowRadius,
         this.shadowStrength,
         this.viewRange
      );
      if (this.screenType == 0) {
         return new DisplayEntityObject.DisplayEntityItemObject(
            this.item, this.itemDisplayContext, genericDisplayEntityData, this.defaultItemDisplayTransformation, this.defaultItemDisplayBrightness
         );
      } else if (this.screenType == 1) {
         return new DisplayEntityObject.DisplayEntityBlockObject(this.blockState.getVanillaState(), genericDisplayEntityData);
      } else {
         return this.screenType == 2
            ? new DisplayEntityObject.DisplayEntityTextObject(
               this.jsonText,
               this.defaultBackground,
               this.backgroundColour,
               this.lineWidth,
               this.textAlignment,
               this.seeThrough,
               this.shadow,
               this.textOpacity,
               genericDisplayEntityData
            )
            : null;
      }
   }

   private void initSelectItem() {
      this.menuWidth = 195;
      this.menuHeight = 151;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      ItemList.INSTANCE.search("", this.onlyCustomItems);
      this.searchBox = new EditBox(this.font, this.leftPos + 82, this.topPos + 6, 80, 9, Component.translatable("itemGroup.search"));
      this.searchBox.setMaxLength(50);
      this.searchBox.setBordered(false);
      this.searchBox.setTextColor(-1);
      this.searchBox.setVisible(true);
      this.searchBox.setCanLoseFocus(false);
      this.searchBox.setFocused(true);
      this.addWidget(this.searchBox);
   }

   private void initSelectBlock() {
      this.menuWidth = 195;
      this.menuHeight = 136;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      EditorUI.getBlockList().search("");
      this.searchBox = new EditBox(this.font, this.leftPos + 87, this.topPos + 6, 75, 9, Component.translatable("itemGroup.search"));
      this.searchBox.setMaxLength(50);
      this.searchBox.setBordered(false);
      this.searchBox.setTextColor(-1);
      this.searchBox.setVisible(true);
      this.searchBox.setCanLoseFocus(false);
      this.searchBox.setFocused(true);
      this.addWidget(this.searchBox);
   }

   public boolean charTyped(char character, int modifiers) {
      if (this.searchBox != null) {
         String string = this.searchBox.getValue();
         if (this.searchBox.charTyped(character, modifiers)) {
            if (!Objects.equals(string, this.searchBox.getValue())) {
               if (this.screenType == 3) {
                  ItemList.INSTANCE.search(this.searchBox.getValue(), this.onlyCustomItems);
               } else {
                  EditorUI.getBlockList().search(this.searchBox.getValue());
               }
            }

            return true;
         }
      }

      return super.charTyped(character, modifiers);
   }

   public boolean keyPressed(int key, int scancode, int modifiers) {
      if (this.searchBox != null) {
         String string = this.searchBox.getValue();
         if (this.searchBox.keyPressed(key, scancode, modifiers)) {
            if (!Objects.equals(string, this.searchBox.getValue())) {
               if (this.screenType == 3) {
                  ItemList.INSTANCE.search(this.searchBox.getValue(), this.onlyCustomItems);
               } else {
                  EditorUI.getBlockList().search(this.searchBox.getValue());
               }
            }

            return true;
         }

         if (this.searchBox.isFocused() && this.searchBox.isVisible() && key != 256) {
            return true;
         }
      }

      return super.keyPressed(key, scancode, modifiers);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.screenType == 3) {
         int rows = (int)Math.ceil(ItemList.INSTANCE.getItems().size() / 9.0F) - 5;
         this.scrollOffset = Mth.clamp(this.scrollOffset - (float)(scrollY / rows), 0.0F, 1.0F);
      } else if (this.screenType == 4) {
         int rows = (int)Math.ceil(EditorUI.getBlockList().getBlocks().size() / 9.0F) - 5;
         this.scrollOffset = Mth.clamp(this.scrollOffset - (float)(scrollY / rows), 0.0F, 1.0F);
      }

      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   private void initAdditional() {
      this.menuWidth = 320;
      this.menuHeight = 194;
      this.leftPos = (this.width - this.menuWidth) / 2;
      this.topPos = (this.height - this.menuHeight) / 2;
      int itemWidth = 150;
      int x = this.leftPos + 8;
      int y = this.topPos + 6;
      Component cullingWidthComponent = Component.translatable("axiom.create_display_entity.culling_width");
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, cullingWidthComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font
         )
      );
      y += 10;
      EditBox widthEditBox = new EditBox(this.font, x, y, itemWidth, 20, cullingWidthComponent);
      widthEditBox.setValue(String.format(Locale.ROOT, "%.2f", this.displayEntityWidth));
      widthEditBox.setResponder(string -> {
         try {
            this.displayEntityWidth = Float.parseFloat(string);
            widthEditBox.setTextColor(-1);
         } catch (Exception var4x) {
            this.displayEntityWidth = 0.0F;
            widthEditBox.setTextColor(-65536);
         }
      });
      this.addRenderableWidget(widthEditBox);
      y += 22;
      Component cullingHeightComponent = Component.translatable("axiom.create_display_entity.culling_height");
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, cullingHeightComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font
         )
      );
      y += 10;
      EditBox heightEditBox = new EditBox(this.font, x, y, itemWidth, 20, cullingHeightComponent);
      heightEditBox.setValue(String.format(Locale.ROOT, "%.2f", this.displayEntityHeight));
      heightEditBox.setResponder(string -> {
         try {
            this.displayEntityHeight = Float.parseFloat(string);
            heightEditBox.setTextColor(-1);
         } catch (Exception var4x) {
            this.displayEntityHeight = 0.0F;
            heightEditBox.setTextColor(-65536);
         }
      });
      this.addRenderableWidget(heightEditBox);
      y += 22;
      Component interpolationTicksComponent = Component.translatable("axiom.create_display_entity.interpolation_ticks");
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, interpolationTicksComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font
         )
      );
      y += 10;
      EditBox interpolationDurationEditBox = new EditBox(this.font, x, y, itemWidth, 20, interpolationTicksComponent);
      interpolationDurationEditBox.setValue(Long.toString(this.interpolationDuration & 4294967295L));
      interpolationDurationEditBox.setResponder(string -> {
         try {
            this.interpolationDuration = (int)Long.parseLong(string);
            interpolationDurationEditBox.setTextColor(-1);
         } catch (Exception var4x) {
            this.interpolationDuration = 0;
            interpolationDurationEditBox.setTextColor(-65536);
         }
      });
      this.addRenderableWidget(interpolationDurationEditBox);
      y += 22;
      Component teleportTicksComponent = Component.translatable("axiom.create_display_entity.teleport_ticks");
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, teleportTicksComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font
         )
      );
      y += 10;
      EditBox teleportDurationEditBox = new EditBox(this.font, x, y, itemWidth, 20, teleportTicksComponent);
      teleportDurationEditBox.setValue(Long.toString(this.teleportDuration & 4294967295L));
      teleportDurationEditBox.setResponder(string -> {
         try {
            this.teleportDuration = (int)Long.parseLong(string);
            teleportDurationEditBox.setTextColor(-1);
         } catch (Exception var4x) {
            this.teleportDuration = 0;
            teleportDurationEditBox.setTextColor(-65536);
         }
      });
      this.addRenderableWidget(teleportDurationEditBox);
      y += 22;
      Component glowColorOverrideComponent = Component.translatable("axiom.create_display_entity.glow_color_override");
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, glowColorOverrideComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font
         )
      );
      y += 10;
      EditBox glowColorOverrideEditBox = new EditBox(this.font, x, y, itemWidth, 20, glowColorOverrideComponent);
      glowColorOverrideEditBox.setValue(ColourUtils.formatHex(this.glowColourOverride));
      glowColorOverrideEditBox.setResponder(string -> {
         try {
            this.glowColourOverride = (int)Long.parseLong(string, 16);
            glowColorOverrideEditBox.setTextColor(-1);
         } catch (Exception var4x) {
            this.glowColourOverride = 0;
            glowColorOverrideEditBox.setTextColor(-65536);
         }
      });
      this.addRenderableWidget(glowColorOverrideEditBox);
      x = this.leftPos + 8 + 160 - 6;
      y = this.topPos + 8;
      Builder<StringRepresentable> billboardButtonBuilder = CycleButton.builder(CreateDisplayEntityScreen::toComponent)
         .withInitialValue(this.billboardConstraints);
      CycleButton<?> billboardButton = billboardButtonBuilder.withValues(BillboardConstraints.values())
         .create(
            x,
            y,
            itemWidth,
            20,
            Component.translatable("axiom.create_display_entity.billboard"),
            (button, value) -> this.billboardConstraints = (BillboardConstraints)value
         );
      this.addRenderableWidget(billboardButton);
      y += 22;
      this.addRenderableWidget(
         CycleButton.onOffBuilder(this.overrideBrightness)
            .create(x, y, itemWidth, 20, Component.translatable("axiom.create_display_entity.override_brightness"), (button, bool) -> {
               this.overrideBrightness = bool;
               this.init();
            })
      );
      if (this.overrideBrightness) {
         int var32 = y + 19;
         Component initialBlockBrightnessTitle = Component.translatable(
            "axiom.create_display_entity.block_brightness", new Object[]{Component.literal(String.valueOf(this.overrideBrightnessBlock))}
         );
         this.addRenderableWidget(
            new AbstractSliderButton(x, var32, itemWidth, 20, initialBlockBrightnessTitle, this.overrideBrightnessBlock / 15.0F) {
               protected void updateMessage() {
                  Component title = Component.translatable(
                     "axiom.create_display_entity.block_brightness",
                     new Object[]{Component.literal(String.valueOf(CreateDisplayEntityScreen.this.overrideBrightnessBlock))}
                  );
                  this.setMessage(title);
               }

               protected void applyValue() {
                  CreateDisplayEntityScreen.this.overrideBrightnessBlock = Mth.clamp((int)(this.value * 15.0), 0, 15);
               }
            }
         );
         y = var32 + 19;
         Component initialSkyBrightnessTitle = Component.translatable(
            "axiom.create_display_entity.sky_brightness", new Object[]{Component.literal(String.valueOf(this.overrideBrightnessSky))}
         );
         this.addRenderableWidget(
            new AbstractSliderButton(x, y, itemWidth, 20, initialSkyBrightnessTitle, this.overrideBrightnessSky / 15.0F) {
               protected void updateMessage() {
                  Component title = Component.translatable(
                     "axiom.create_display_entity.sky_brightness",
                     new Object[]{Component.literal(String.valueOf(CreateDisplayEntityScreen.this.overrideBrightnessSky))}
                  );
                  this.setMessage(title);
               }

               protected void applyValue() {
                  CreateDisplayEntityScreen.this.overrideBrightnessSky = Mth.clamp((int)(this.value * 15.0), 0, 15);
               }
            }
         );
      }

      y += 22;
      Component shadowRadiusComponent = Component.translatable("axiom.create_display_entity.shadow_radius");
      this.addRenderableWidget(
         new CreateDisplayEntityScreen.BasicStringWidget(
            x + 1, y + 1, 0, 0, shadowRadiusComponent.copy().withStyle(Style.EMPTY.withColor(-12566464)), this.font
         )
      );
      y += 10;
      EditBox shadowRadiusEditBox = new EditBox(this.font, x, y, itemWidth, 20, shadowRadiusComponent);
      shadowRadiusEditBox.setValue(String.format(Locale.ROOT, "%.2f", this.shadowRadius));
      shadowRadiusEditBox.setResponder(string -> {
         try {
            this.shadowRadius = Float.parseFloat(string);
            shadowRadiusEditBox.setTextColor(-1);
         } catch (Exception var4x) {
            this.shadowRadius = 0.0F;
            shadowRadiusEditBox.setTextColor(-65536);
         }
      });
      this.addRenderableWidget(shadowRadiusEditBox);
      y += 22;
      Component initialShadowStrengthTitle = Component.translatable(
         "axiom.create_display_entity.shadow_strength", new Object[]{Component.literal(String.format("%.2f", this.shadowStrength))}
      );
      this.addRenderableWidget(
         new AbstractSliderButton(x, y, itemWidth, 20, initialShadowStrengthTitle, this.shadowStrength / 2.0F) {
            protected void updateMessage() {
               Component title = Component.translatable(
                  "axiom.create_display_entity.shadow_strength",
                  new Object[]{Component.literal(String.format("%.2f", CreateDisplayEntityScreen.this.shadowStrength))}
               );
               this.setMessage(title);
            }

            protected void applyValue() {
               CreateDisplayEntityScreen.this.shadowStrength = (float)this.value * 2.0F;
            }
         }
      );
      y += 22;
      Component initialViewRangeTitle = Component.translatable(
         "axiom.create_display_entity.view_range", new Object[]{Component.literal(String.format("%.2f", this.viewRange))}
      );
      this.addRenderableWidget(
         new AbstractSliderButton(x, y, itemWidth, 20, initialViewRangeTitle, this.viewRange / 2.0F) {
            protected void updateMessage() {
               Component title = Component.translatable(
                  "axiom.create_display_entity.view_range", new Object[]{Component.literal(String.format("%.2f", CreateDisplayEntityScreen.this.viewRange))}
               );
               this.setMessage(title);
            }

            protected void applyValue() {
               CreateDisplayEntityScreen.this.viewRange = (float)this.value * 2.0F;
            }
         }
      );
      y += 22;
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
         this.screenType = this.lastScreenType;
         this.init();
      }).bounds(this.leftPos + 8, this.topPos + this.menuHeight - 28, this.menuWidth - 16, 20).build());
   }

   private static Component booleanToComponent(Boolean bool) {
      return Component.literal(bool.toString().toUpperCase(Locale.ROOT));
   }

   private static Component stringToComponent(String string) {
      return Component.literal(string.toUpperCase(Locale.ROOT));
   }

   private static Component enumToComponent(Object object) {
      return toComponent((StringRepresentable)object);
   }

   public static Component toComponent(StringRepresentable stringRepresentable) {
      return Component.literal(stringRepresentable.getSerializedName().toUpperCase(Locale.ROOT));
   }

   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      if (this.canSwitchType && (this.screenType == 0 || this.screenType == 1 || this.screenType == 2)) {
         for (int i = 0; i < 3; i++) {
            if (i != this.screenType) {
               ResourceLocation resourceLocation = UNSELECTED_TOP_TABS[i];
               VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, resourceLocation, this.leftPos + 27 * i, this.topPos - 28, 26, 32);
            }
         }
      }

      if (this.screenType == 3) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, SELECT_ITEM, this.leftPos, this.topPos, this.menuWidth, this.menuHeight);
      } else if (this.screenType == 4) {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, SELECT_BLOCK, this.leftPos, this.topPos, this.menuWidth, this.menuHeight);
      } else {
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, BACKGROUND, this.leftPos, this.topPos, this.menuWidth, this.menuHeight);
      }
   }

   public void onClose() {
      switch (this.screenType) {
         case 3:
            this.screenType = 0;
            this.init();
            break;
         case 4:
            this.screenType = 1;
            this.init();
            break;
         case 5:
            this.screenType = this.lastScreenType;
            this.init();
            break;
         default:
            super.onClose();
            if (this.callbackOnClose) {
               this.callback.accept(this.createDisplayEntityObject());
            }
      }
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.updateHovered(mouseX, mouseY);
      if (this.searchBox != null) {
         this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
      }

      Component itemDisplay = Component.translatable("axiom.create_display_entity.item_display");
      Component blockDisplay = Component.translatable("axiom.create_display_entity.block_display");
      Component textDisplay = Component.translatable("axiom.create_display_entity.text_display");
      if (this.canSwitchType && (this.screenType == 0 || this.screenType == 1 || this.screenType == 2)) {
         for (int i = 0; i < 3; i++) {
            if (i == this.screenType) {
               ResourceLocation resourceLocation = SELECTED_TOP_TABS[this.screenType];
               VersionUtilsClient.genericBlitSprite(
                  guiGraphics, Dummy.GUI_TEXTURED, resourceLocation, this.leftPos + 27 * this.screenType, this.topPos - 28, 26, 32
               );
            }

            guiGraphics.renderItem(TAB_ITEM_STACKS[i], this.leftPos + 27 * i + 5, this.topPos - 28 + 8);
         }

         if (this.hovered == 3) {
            guiGraphics.renderTooltip(this.font, itemDisplay, mouseX, mouseY);
         } else if (this.hovered == 4) {
            guiGraphics.renderTooltip(this.font, blockDisplay, mouseX, mouseY);
         } else if (this.hovered == 5) {
            guiGraphics.renderTooltip(this.font, textDisplay, mouseX, mouseY);
         }
      }

      int y = this.topPos + 6;
      if (this.screenType == 0) {
         guiGraphics.drawString(this.font, itemDisplay, this.leftPos + 8, y, -12566464, false);
         y += 11;
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, SLOT, this.leftPos + 8, y, 18, 18);
         guiGraphics.drawString(this.font, Component.translatable("axiom.create_display_entity.item"), this.leftPos + 28, y + 5, -12566464, false);
         guiGraphics.renderItem(this.item, this.leftPos + 9, y + 1);
         if (this.hovered == 0) {
            int x = this.leftPos + 9;
            guiGraphics.fill(x, y + 1, x + 16, y + 1 + 16, -2130706433);
         }
      } else if (this.screenType == 1) {
         guiGraphics.drawString(this.font, blockDisplay, this.leftPos + 8, y, -12566464, false);
         y += 11;
         VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, SLOT, this.leftPos + 8, y, 18, 18);
         guiGraphics.drawString(this.font, Component.translatable("axiom.create_display_entity.block"), this.leftPos + 28, y + 5, -12566464, false);
         double size = 16.0 * Minecraft.getInstance().getWindow().getGuiScale();
         AxiomGpuTexture img = BlockRenderCache.requestTexture(this.blockState, (int)Math.ceil(size), (int)Math.ceil(size), true);
         if (img != null) {
            int left = this.leftPos + 9;
            int top = y + 1;
            renderFlippedImage(guiGraphics, img, left, top);
         }

         if (this.hovered == 6) {
            int x = this.leftPos + 9;
            guiGraphics.fill(x, y + 1, x + 16, y + 1 + 16, -2130706433);
         }
      } else if (this.screenType == 2) {
         guiGraphics.drawString(this.font, textDisplay, this.leftPos + 8, y, -12566464, false);
         y += 11;
      } else if (this.screenType == 3) {
         guiGraphics.drawString(this.font, Component.translatable("itemGroup.search"), this.leftPos + 8, y, -12566464, false);
         List<ItemList.Entry> items = ItemList.INSTANCE.getItems();
         int rows = (int)Math.ceil(items.size() / 9.0F) - 5;
         if (rows <= 0) {
            this.scrollOffset = 0.0F;
         }

         ResourceLocation resourceLocation = rows > 0 ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
         VersionUtilsClient.genericBlitSprite(
            guiGraphics, Dummy.GUI_TEXTURED, resourceLocation, this.leftPos + 175, this.topPos + 33 + (int)(95.0F * this.scrollOffset), 12, 15
         );
         y += 12;
         if (!this.onlyCustomItems) {
            VersionUtilsClient.genericBlitSprite(guiGraphics, Dummy.GUI_TEXTURED, FILTER_OFF, this.leftPos + 10, y, 13, 12);
         }

         y += 2;
         guiGraphics.drawString(this.font, Component.translatable("axiom.create_display_entity.filter_custom_items"), this.leftPos + 28, y, -12566464, false);
         int rowOffset = Math.round(this.scrollOffset * rows);
         if (rowOffset < 0) {
            rowOffset = 0;
         }

         for (int gridX = 0; gridX < 9; gridX++) {
            for (int gridY = 0; gridY < 5; gridY++) {
               int index = gridX + gridY * 9 + rowOffset * 9;
               if (index >= items.size()) {
                  break;
               }

               ItemStack itemStack = items.get(index).itemStack();
               int left = this.leftPos + 9 + gridX * 18;
               int top = this.topPos + 33 + gridY * 18;
               guiGraphics.renderItem(itemStack, left, top);
               if (mouseX >= left - 1 && mouseX < left + 17 && mouseY >= top - 1 && mouseY < top + 17) {
                  guiGraphics.fill(left, top, left + 16, top + 16, -2130706433);
                  guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY);
               }
            }
         }

         LocalPlayer player = Minecraft.getInstance().player;
         Inventory inventory = player.getInventory();

         for (int invX = 0; invX < 9; invX++) {
            ItemStack itemStack = inventory.getItem(invX);
            if (!itemStack.isEmpty()) {
               int left = this.leftPos + 9 + invX * 18;
               int top = this.topPos + 127;
               guiGraphics.renderItem(itemStack, left, top);
               if (mouseX >= left - 1 && mouseX < left + 17 && mouseY >= top - 1 && mouseY < top + 17) {
                  guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY);
               }
            }
         }
      } else if (this.screenType == 4) {
         guiGraphics.drawString(this.font, Component.translatable("axiom.create_display_entity.search_blocks"), this.leftPos + 8, y, -12566464, false);
         List<BlockList.Entry> blocks = EditorUI.getBlockList().getBlocks();
         int rowsx = (int)Math.ceil(blocks.size() / 9.0F) - 5;
         if (rowsx <= 0) {
            this.scrollOffset = 0.0F;
         }

         ResourceLocation resourceLocationx = rowsx > 0 ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
         VersionUtilsClient.genericBlitSprite(
            guiGraphics, Dummy.GUI_TEXTURED, resourceLocationx, this.leftPos + 175, this.topPos + 18 + (int)(95.0F * this.scrollOffset), 12, 15
         );
         int rowOffset = Math.round(this.scrollOffset * rowsx);
         if (rowOffset < 0) {
            rowOffset = 0;
         }

         for (int gridX = 0; gridX < 9; gridX++) {
            for (int gridY = 0; gridY < 5; gridY++) {
               int indexx = gridX + gridY * 9 + rowOffset * 9;
               if (indexx >= blocks.size()) {
                  break;
               }

               CustomBlockState customBlockState = blocks.get(indexx).state();
               int left = this.leftPos + 9 + gridX * 18;
               int top = this.topPos + 18 + gridY * 18;
               double sizex = 16.0 * Minecraft.getInstance().getWindow().getGuiScale();
               AxiomGpuTexture imgx = BlockRenderCache.requestTexture(customBlockState, (int)Math.ceil(sizex), (int)Math.ceil(sizex), false);
               if (imgx != null) {
                  renderFlippedImage(guiGraphics, imgx, left, top);
               }

               if (mouseX >= left - 1 && mouseX < left + 17 && mouseY >= top - 1 && mouseY < top + 17) {
                  guiGraphics.fill(left, top, left + 16, top + 16, -2130706433);
                  guiGraphics.renderTooltip(this.font, Component.translatable(customBlockState.getCustomBlock().axiom$translationKey()), mouseX, mouseY);
               }
            }
         }

         LocalPlayer player = Minecraft.getInstance().player;
         Inventory inventory = player.getInventory();

         for (int invXx = 0; invXx < 9; invXx++) {
            ItemStack itemStack = inventory.getItem(invXx);
            if (!itemStack.isEmpty()) {
               int leftx = this.leftPos + 9 + invXx * 18;
               int topx = this.topPos + 112;
               guiGraphics.renderItem(itemStack, leftx, topx);
               if (mouseX >= leftx - 1 && mouseX < leftx + 17 && mouseY >= topx - 1 && mouseY < topx + 17) {
                  guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY);
               }
            }
         }
      }
   }

   private static void renderFlippedImage(GuiGraphics guiGraphics, AxiomGpuTexture img, int left, int top) {
      RenderSystem.setShaderTexture(0, img.glId());
      Matrix4f matrix = guiGraphics.pose().last().pose();
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      bufferBuilder.addVertex(matrix, left, top, 0.0F).setUv(0.0F, 1.0F);
      bufferBuilder.addVertex(matrix, left, top + 16, 0.0F).setUv(0.0F, 0.0F);
      bufferBuilder.addVertex(matrix, left + 16, top + 16, 0.0F).setUv(1.0F, 0.0F);
      bufferBuilder.addVertex(matrix, left + 16, top, 0.0F).setUv(1.0F, 1.0F);
      AxiomRenderPipelines.POSITION_TEX.render(provider.build());
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.scrolling = false;
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
      if (this.scrolling) {
         int j = this.topPos + (this.screenType == 0 ? 33 : 18);
         int k = j + 112;
         this.scrollOffset = ((float)mouseY - j - 7.5F) / (k - j - 15.0F);
         this.scrollOffset = Mth.clamp(this.scrollOffset, 0.0F, 1.0F);
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dx, dy);
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.screenType == 3) {
         int gridLeft = this.leftPos + 9;
         int gridRight = gridLeft + 162;
         int gridTop = this.topPos + 33;
         int gridBottom = gridTop + 90;
         if (mouseX >= gridLeft && mouseX < gridRight) {
            int gridX = (int)((mouseX - gridLeft) / (gridRight - gridLeft) * 9.0);
            if (mouseY >= gridTop && mouseY < gridBottom) {
               int gridY = (int)((mouseY - gridTop) / (gridBottom - gridTop) * 5.0);
               List<ItemList.Entry> items = ItemList.INSTANCE.getItems();
               int rows = (int)Math.ceil(items.size() / 9.0F) - 5;
               int rowOffset = Math.round(this.scrollOffset * rows);
               if (rowOffset < 0) {
                  rowOffset = 0;
               }

               int index = gridX + gridY * 9 + rowOffset * 9;
               if (index < items.size()) {
                  this.item = items.get(index).itemStack();
                  this.defaultItemDisplayTransformation = items.get(index).defaultItemDisplayTransformation();
                  this.defaultItemDisplayBrightness = items.get(index).defaultItemDisplayBrightness();
                  this.screenType = 0;
                  this.init();
                  return true;
               }
            } else if (mouseY >= this.topPos + 127 && mouseY < this.topPos + 127 + 18) {
               LocalPlayer player = Minecraft.getInstance().player;
               Inventory inventory = player.getInventory();
               ItemStack itemStack = inventory.getItem(gridX);
               if (itemStack.isEmpty()) {
                  return true;
               }

               this.item = itemStack;
               this.screenType = 0;
               this.init();
               return true;
            }
         }
      } else if (this.screenType == 4) {
         int gridLeft = this.leftPos + 9;
         int gridRight = gridLeft + 162;
         int gridTop = this.topPos + 18;
         int gridBottom = gridTop + 90;
         if (mouseX >= gridLeft && mouseX < gridRight) {
            int gridX = (int)((mouseX - gridLeft) / (gridRight - gridLeft) * 9.0);
            if (mouseY >= gridTop && mouseY < gridBottom) {
               int gridYx = (int)((mouseY - gridTop) / (gridBottom - gridTop) * 5.0);
               List<BlockList.Entry> blocks = EditorUI.getBlockList().getBlocks();
               int rowsx = (int)Math.ceil(blocks.size() / 9.0F) - 5;
               int rowOffsetx = Math.round(this.scrollOffset * rowsx);
               if (rowOffsetx < 0) {
                  rowOffsetx = 0;
               }

               int index = gridX + gridYx * 9 + rowOffsetx * 9;
               if (index < blocks.size()) {
                  this.blockState = blocks.get(index).state();
                  this.screenType = 1;
                  this.init();
                  return true;
               }
            } else if (mouseY >= this.topPos + 112 && mouseY < this.topPos + 112 + 18) {
               LocalPlayer player = Minecraft.getInstance().player;
               Inventory inventory = player.getInventory();
               ItemStack itemStack = inventory.getItem(gridX);
               if (itemStack.isEmpty()) {
                  return true;
               }

               this.blockState = (CustomBlockState)Block.byItem(itemStack.getItem()).defaultBlockState();
               this.screenType = 1;
               this.init();
               return true;
            }
         }
      }

      this.updateHovered((int)mouseX, (int)mouseY);
      switch (this.hovered) {
         case 0:
            this.screenType = 3;
            this.init();
            return true;
         case 1:
            this.onlyCustomItems = !this.onlyCustomItems;
            ItemList.INSTANCE.search(this.searchBox.getValue(), this.onlyCustomItems);
            return true;
         case 2:
            this.scrolling = true;
            return true;
         case 3:
            if (this.screenType != 0) {
               this.screenType = 0;
               this.init();
               return true;
            }

            return false;
         case 4:
            if (this.screenType != 1) {
               this.screenType = 1;
               this.init();
               return true;
            }

            return false;
         case 5:
            if (this.screenType != 2) {
               this.screenType = 2;
               this.init();
               return true;
            }

            return false;
         case 6:
            this.screenType = 4;
            this.init();
            return true;
         default:
            boolean superResult = super.mouseClicked(mouseX, mouseY, button);
            if (superResult) {
               return true;
            } else {
               ComponentPath componentPath = this.getCurrentFocusPath();
               if (componentPath != null) {
                  componentPath.applyFocus(false);
               }

               return false;
            }
      }
   }

   private void updateHovered(int mouseX, int mouseY) {
      this.hovered = -1;
      if (this.canSwitchType && (this.screenType == 0 || this.screenType == 1 || this.screenType == 2) && mouseY >= this.topPos - 28 && mouseY < this.topPos) {
         if (mouseX >= this.leftPos && mouseX <= this.leftPos + 26) {
            this.hovered = 3;
            return;
         }

         if (mouseX >= this.leftPos + 27 && mouseX <= this.leftPos + 26 + 27) {
            this.hovered = 4;
            return;
         }

         if (mouseX >= this.leftPos + 54 && mouseX <= this.leftPos + 26 + 54) {
            this.hovered = 5;
            return;
         }
      }

      if (this.screenType == 3) {
         if (mouseX >= this.leftPos + 10 && mouseX <= this.leftPos + 23 && mouseY >= this.topPos + 18 && mouseY <= this.topPos + 30) {
            this.hovered = 1;
            return;
         }

         List<ItemList.Entry> items = ItemList.INSTANCE.getItems();
         int rows = (int)Math.ceil(items.size() / 9.0F) - 5;
         if (rows > 0) {
            int scrollerY = this.topPos + 33 + (int)(95.0F * this.scrollOffset);
            if (mouseX >= this.leftPos + 175 && mouseX <= this.leftPos + 175 + 12 && mouseY >= scrollerY && mouseY <= scrollerY + 15) {
               this.hovered = 2;
            }
         }
      } else if (this.screenType == 4) {
         List<BlockList.Entry> blocks = EditorUI.getBlockList().getBlocks();
         int rows = (int)Math.ceil(blocks.size() / 9.0F) - 5;
         if (rows > 0) {
            int scrollerY = this.topPos + 18 + (int)(95.0F * this.scrollOffset);
            if (mouseX >= this.leftPos + 175 && mouseX <= this.leftPos + 175 + 12 && mouseY >= scrollerY && mouseY <= scrollerY + 15) {
               this.hovered = 2;
            }
         }
      } else if (this.screenType == 0) {
         if (mouseX >= this.leftPos + 8 && mouseX <= this.leftPos + 26 && mouseY >= this.topPos + 17 && mouseY <= this.topPos + 35) {
            this.hovered = 0;
         }
      } else if (this.screenType == 1 && mouseX >= this.leftPos + 8 && mouseX <= this.leftPos + 26 && mouseY >= this.topPos + 17 && mouseY <= this.topPos + 35) {
         this.hovered = 6;
      }
   }

   public static final class BasicStringWidget extends AbstractWidget {
      private final Font font;

      public BasicStringWidget(int i, int j, int k, int l, Component component, Font font) {
         super(i, j, k, l, component);
         this.font = font;
      }

      protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float a) {
         guiGraphics.drawString(this.font, this.getMessage().getVisualOrderText(), this.getX(), this.getY(), -1, false);
      }

      protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
      }
   }
}
