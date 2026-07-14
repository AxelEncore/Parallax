package com.moulberry.axiom.editor.widgets;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.StringProperty;
import com.moulberry.axiom.editor.BlockList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.StringUtils;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiViewport;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class BlockStateWidget {
   private CustomBlock block;
   private CustomBlockState blockState;
   private final Map<Property<?>, Comparable<?>> selectedProperties = new LinkedHashMap<>();
   private final Set<Property<?>> randomProperties = new HashSet<>();
   private float maxLabelWidth = 0.0F;
   private float maxControlWidth = 0.0F;
   private final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   public boolean allowRandomProperties = false;

   public BlockStateWidget(Block block) {
      this.switchBlock((CustomBlock)block);
   }

   public BlockStateWidget(CustomBlockState customBlockState) {
      this.setBlockState(customBlockState);
   }

   @Nullable
   public Set<Property<?>> getRandomProperties() {
      return this.randomProperties.isEmpty() ? null : Collections.unmodifiableSet(this.randomProperties);
   }

   public void setFilter(Predicate<CustomBlockState> filter) {
      this.selectBlockWidget.setFilter(filter);
   }

   public void renderBlockSwitcher(BlockList blockList, String id) {
      boolean clicked = ImGuiHelper.blockStateButton(this.blockState, ImGui.getID(id), (int)(52.0F * EditorUI.getUiScale()));
      CustomBlockState dragDropped = ImGuiHelper.blockStateDragDrop(this.blockState);
      if (dragDropped != null) {
         this.setBlockState(dragDropped);
      }

      if (clicked) {
         this.selectBlockWidget.open();
      }

      ImGuiViewport viewport = ImGui.getMainViewport();
      ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), 8, 0.5F, 0.5F);
      this.selectBlockWidget.render(AxiomI18n.get("axiom.widget.select_block"), blockList);
      CustomBlockState selectedBlock = this.selectBlockWidget.getResultState();
      if (selectedBlock != null) {
         this.switchBlock(selectedBlock.getCustomBlock());
         this.setBlockState(selectedBlock);
      }
   }

   public float renderPropertySettings(String id, boolean compressed) {
      boolean propertiesChanged = false;
      float maxX = 0.0F;
      float start = ImGui.getCursorPosX();
      if (compressed && this.maxLabelWidth + ImGui.getStyle().getCellPaddingX() + this.maxControlWidth + 8.0F <= ImGui.getContentRegionAvailX()) {
         compressed = false;
      }

      this.maxLabelWidth = 0.0F;
      this.maxControlWidth = 0.0F;
      if (!compressed) {
         ImGui.setCursorPosY(ImGui.getCursorPosY() - ImGui.getStyle().getCellPaddingY());
      }

      if (compressed || ImGui.beginTable("##PropertyTable" + id, 2, 73728)) {
         for (Entry<Property<?>, Comparable<?>> entry : this.selectedProperties.entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();
            String prettyName = StringUtils.convertSnakeToWords(property.getName());
            float offset = ImGui.getStyle().getFramePaddingY();
            if (!compressed) {
               ImGui.tableNextColumn();
            }

            ImGui.setCursorPosY(ImGui.getCursorPosY() + offset);
            ImGui.text(prettyName);
            ImGui.sameLine(0.0F, 0.0F);
            this.maxLabelWidth = Math.max(ImGui.getCursorPosX() - start, this.maxLabelWidth);
            ImGui.dummy(0.0F, 0.0F);
            if (compressed) {
               ImGui.sameLine();
               ImGui.setCursorPosY(ImGui.getCursorPosY() - offset);
            } else {
               ImGui.tableNextColumn();
            }

            float controlStart = ImGui.getCursorPosX();
            if (property instanceof EnumProperty<?> enumProperty) {
               String[] values = new String[enumProperty.getPossibleValues().size() + (this.allowRandomProperties ? 1 : 0)];
               int currentIndex = -1;
               Iterator<?> iterator = enumProperty.getPossibleValues().iterator();

               for (int i = 0; i < values.length; i++) {
                  if (this.allowRandomProperties && i == values.length - 1) {
                     values[i] = AxiomI18n.get("axiom.random");
                  } else {
                     Enum<?> enumVal = (Enum<?>)iterator.next();
                     values[i] = StringUtils.convertSnakeToWords(((StringRepresentable)enumVal).getSerializedName());
                     if (enumVal == value) {
                        currentIndex = i;
                     }
                  }
               }

               if (this.allowRandomProperties && this.randomProperties.contains(property)) {
                  currentIndex = values.length - 1;
               }

               if (BuildConfig.DEBUG && currentIndex == -1 && value != null) {
                  throw new FaultyImplementationError();
               }

               ImGui.setNextItemWidth(!compressed ? 128.0F : Math.min(ImGui.getContentRegionAvailX(), 128.0F));
               int[] array = new int[]{currentIndex};
               ImGuiHelper.combo("##Combo" + prettyName, array, values);
               if (array[0] != currentIndex) {
                  propertiesChanged = true;
                  if (this.allowRandomProperties && array[0] == values.length - 1) {
                     this.randomProperties.add(property);
                  } else {
                     iterator = enumProperty.getPossibleValues().iterator();
                     Enum<?> enumVal = null;

                     for (int ix = 0; ix <= array[0]; ix++) {
                        enumVal = (Enum<?>)iterator.next();
                     }

                     this.selectedProperties.put(property, enumVal);
                     this.randomProperties.remove(property);
                  }
               }
            } else if (property instanceof StringProperty stringProperty) {
               String[] values = new String[stringProperty.getPossibleValues().size() + (this.allowRandomProperties ? 1 : 0)];
               int currentIndex = -1;
               int index = 0;

               for (String stringValue : stringProperty.getPossibleValues()) {
                  if (this.allowRandomProperties && index == values.length - 1) {
                     values[index] = AxiomI18n.get("axiom.random");
                  } else {
                     values[index] = StringUtils.convertSnakeToWords(stringValue);
                     if (stringValue == value) {
                        currentIndex = index;
                     }
                  }

                  index++;
               }

               if (this.allowRandomProperties && this.randomProperties.contains(property)) {
                  currentIndex = values.length - 1;
               }

               if (BuildConfig.DEBUG && currentIndex == -1 && value != null) {
                  throw new FaultyImplementationError();
               }

               ImGui.setNextItemWidth(!compressed ? 128.0F : Math.min(ImGui.getContentRegionAvailX(), 128.0F));
               int[] array = new int[]{currentIndex};
               ImGuiHelper.combo("##Combo" + prettyName, array, values);
               if (array[0] != currentIndex) {
                  propertiesChanged = true;
                  if (this.allowRandomProperties && array[0] == values.length - 1) {
                     this.randomProperties.add(property);
                  } else {
                     Iterator<String> iterator = stringProperty.getPossibleValues().iterator();
                     String stringVal = null;

                     for (int ix = 0; ix <= array[0]; ix++) {
                        stringVal = iterator.next();
                     }

                     this.selectedProperties.put(property, stringVal);
                     this.randomProperties.remove(property);
                  }
               }
            } else if (property instanceof BooleanProperty) {
               if (this.allowRandomProperties || compressed && !(ImGui.getContentRegionAvailX() >= 80.0F + ImGui.getStyle().getItemSpacingX())) {
                  ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
                  int currentValue = (Boolean)value ? 0 : 1;
                  if (this.allowRandomProperties && this.randomProperties.contains(property)) {
                     currentValue = 2;
                  }

                  String[] values;
                  if (this.allowRandomProperties) {
                     values = new String[]{
                        AxiomI18n.get("axiom.widget.true_block_property"), AxiomI18n.get("axiom.widget.false_block_property"), AxiomI18n.get("axiom.random")
                     };
                  } else {
                     values = new String[]{AxiomI18n.get("axiom.widget.true_block_property"), AxiomI18n.get("axiom.widget.false_block_property")};
                  }

                  int[] array = new int[]{currentValue};
                  ImGuiHelper.combo("##BoolCombo" + prettyName, array, values);
                  if (array[0] != currentValue) {
                     propertiesChanged = true;
                     if (this.allowRandomProperties && array[0] == 2) {
                        this.randomProperties.add(property);
                     } else if (array[0] == 0) {
                        this.selectedProperties.put(property, true);
                        this.randomProperties.remove(property);
                     } else {
                        this.selectedProperties.put(property, false);
                        this.randomProperties.remove(property);
                     }
                  }
               } else {
                  if ((Boolean)value) {
                     ImGui.beginDisabled();
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.widget.true_block_property") + "##Bool" + prettyName)) {
                     propertiesChanged = true;
                     this.selectedProperties.put(property, true);
                  }

                  if ((Boolean)value) {
                     ImGui.endDisabled();
                  }

                  ImGui.sameLine();
                  if (!(Boolean)value) {
                     ImGui.beginDisabled();
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.widget.false_block_property") + "##Bool" + prettyName)) {
                     propertiesChanged = true;
                     this.selectedProperties.put(property, false);
                  }

                  if (!(Boolean)value) {
                     ImGui.endDisabled();
                  }
               }
            } else if (!(property instanceof IntegerProperty integerProperty)) {
               ImGui.text(String.valueOf(property.getClass()));
            } else {
               int currentValuex = (Integer)value;
               if (this.allowRandomProperties && this.randomProperties.contains(property)) {
                  currentValuex = integerProperty.max + 1;
               }

               int[] array = new int[]{currentValuex};
               ImGui.setNextItemWidth(!compressed ? 128.0F : Math.min(ImGui.getContentRegionAvailX(), 128.0F));
               if (this.allowRandomProperties && this.randomProperties.contains(property)) {
                  ImGui.sliderInt("##Slider" + prettyName, array, integerProperty.min, integerProperty.max + 1, AxiomI18n.get("axiom.random"));
               } else {
                  ImGui.sliderInt("##Slider" + prettyName, array, integerProperty.min, integerProperty.max + (this.allowRandomProperties ? 1 : 0));
               }

               if (array[0] != currentValuex) {
                  propertiesChanged = true;
                  if (this.allowRandomProperties && array[0] > integerProperty.max) {
                     this.randomProperties.add(property);
                  } else {
                     this.selectedProperties.put(property, Math.max(integerProperty.min, Math.min(integerProperty.max, array[0])));
                     this.randomProperties.remove(property);
                  }
               }
            }

            ImGui.sameLine(0.0F, 0.0F);
            maxX = Math.max(ImGui.getCursorPosX(), maxX);
            this.maxControlWidth = Math.max(ImGui.getCursorPosX() - controlStart, this.maxControlWidth);
            ImGui.dummy(0.0F, 0.0F);
         }

         if (!compressed) {
            ImGui.endTable();
         }
      }

      if (propertiesChanged) {
         this.blockState = this.block.axiom$defaultCustomState();

         for (Entry<Property<?>, Comparable<?>> entry : this.selectedProperties.entrySet()) {
            this.blockState = this.blockState.setPropertyUnsafe(entry.getKey(), entry.getValue());
         }
      }

      return maxX - start;
   }

   public boolean hasEditableProperties() {
      return !this.selectedProperties.isEmpty();
   }

   public CustomBlockState getBlockState() {
      return this.blockState;
   }

   public void setBlockState(CustomBlockState blockState) {
      if (this.blockState != blockState) {
         this.blockState = blockState;
         this.block = this.blockState.getCustomBlock();
         this.selectedProperties.clear();

         for (Property<?> property : this.blockState.getProperties()) {
            this.selectedProperties.put(property, this.blockState.getProperty((Property)property));
         }

         this.randomProperties.retainAll(this.selectedProperties.keySet());
      }
   }

   private void switchBlock(CustomBlock block) {
      this.blockState = block.axiom$defaultCustomState();
      if (this.block != null && this.block != block) {
         this.selectedProperties.keySet().retainAll(this.blockState.getProperties());

         for (Property<?> property : this.blockState.getProperties()) {
            if (!this.selectedProperties.containsKey(property)) {
               this.selectedProperties.put(property, this.blockState.getProperty((Property)property));
            }
         }

         this.randomProperties.retainAll(this.selectedProperties.keySet());

         for (Entry<Property<?>, Comparable<?>> entry : this.selectedProperties.entrySet()) {
            this.blockState = this.blockState.setPropertyUnsafe(entry.getKey(), entry.getValue());
         }
      } else {
         this.selectedProperties.clear();

         for (Property<?> propertyx : this.blockState.getProperties()) {
            this.selectedProperties.put(propertyx, this.blockState.getProperty((Property)propertyx));
         }

         this.randomProperties.retainAll(this.selectedProperties.keySet());
      }

      this.block = block;
   }

   public void setRandomizedProperties(@Nullable Set<Property<?>> randomProperties) {
      this.randomProperties.clear();
      if (randomProperties != null) {
         this.randomProperties.addAll(randomProperties);
         this.randomProperties.retainAll(this.selectedProperties.keySet());
      }
   }
}
