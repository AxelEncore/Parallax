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
import com.moulberry.axiom.utils.BlockCondition;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.StringUtils;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiViewport;
import imgui.moulberry92.ImVec2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class BlockConditionWidget {
   private CustomBlock block = null;
   private CustomBlockState visualBlockState = null;
   private final Map<Property<?>, Comparable<?>> selectedProperties = new LinkedHashMap<>();
   private BlockList.MinecraftOrCustomTagSet tag = null;
   private final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(true);

   public BlockConditionWidget(CustomBlock block) {
      this.switchBlock(block);
   }

   public BlockConditionWidget(Block block) {
      this.switchBlock((CustomBlock)block);
   }

   public void setFilter(Predicate<CustomBlockState> filter) {
      this.selectBlockWidget.setFilter(filter);
   }

   public boolean renderBlockSwitcher(BlockList blockList, String id) {
      return this.renderBlockSwitcher(blockList, id, null);
   }

   public boolean renderBlockSwitcher(BlockList blockList, String id, BooleanWrapper allowDragDropTarget) {
      boolean changed = false;
      if (this.block != null) {
         int buttonSize = (int)(52.0F * EditorUI.getUiScale());
         ImVec2 pos = ImGui.getCursorScreenPos();
         boolean clicked = ImGuiHelper.blockStateButton(this.visualBlockState, ImGui.getID(id), buttonSize);
         ImGuiHelper.blockStateDragDropSource(this.visualBlockState);
         if (allowDragDropTarget == null || allowDragDropTarget.value) {
            CustomBlockState dragDropped;
            if (allowDragDropTarget != null) {
               dragDropped = ImGuiHelper.blockStateDragDropTarget(
                  () -> {
                     allowDragDropTarget.value = false;
                     ImGui.getWindowDrawList()
                        .addRect(pos.x - 3.0F, pos.y - 3.0F, pos.x + buttonSize + 3.0F, pos.y + buttonSize + 3.0F, ImGui.getColorU32(55), 0.0F, 0, 2.0F);
                  }
               );
            } else {
               dragDropped = ImGuiHelper.blockStateDragDropTarget();
            }

            if (dragDropped != null) {
               if (allowDragDropTarget != null) {
                  allowDragDropTarget.value = false;
               }

               if (dragDropped.getCustomBlock() != this.block) {
                  this.switchBlock(dragDropped.getCustomBlock());
                  changed = true;
               }
            }
         }

         if (clicked) {
            this.selectBlockWidget.open();
         }
      } else if (this.tag != null && ImGui.button("#" + this.tag.name() + "###TagButton" + id, 0.0F, 52.0F)) {
         this.selectBlockWidget.open();
      }

      ImGuiViewport viewport = ImGui.getMainViewport();
      ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), 8, 0.5F, 0.5F);
      this.selectBlockWidget.render(AxiomI18n.get("axiom.widget.select_block"), blockList);
      CustomBlockState selectedBlock = this.selectBlockWidget.getResultState();
      if (selectedBlock != null) {
         this.switchBlock(selectedBlock.getCustomBlock());
         changed = true;
      }

      BlockList.MinecraftOrCustomTagSet selectedTag = this.selectBlockWidget.getResultTag();
      if (selectedTag != null) {
         this.block = null;
         this.visualBlockState = null;
         this.selectedProperties.clear();
         this.tag = selectedTag;
         changed = true;
      }

      return changed;
   }

   public BlockConditionWidget.PropertySettingsResult renderPropertySettings(String id, boolean compressed) {
      boolean propertiesChanged = false;
      float maxX = 0.0F;
      float start = ImGui.getCursorPosX();
      if (compressed || ImGui.beginTable("##PropertyTable" + id, 2, 65536)) {
         for (Entry<Property<?>, Comparable<?>> entry : this.selectedProperties.entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();
            String prettyName = StringUtils.convertSnakeToWords(property.getName());
            if (!compressed) {
               ImGui.tableNextColumn();
            }

            ImGui.setCursorPosY(ImGui.getCursorPosY() + ImGui.getStyle().getFramePaddingY());
            ImGui.text(prettyName);
            if (compressed) {
               ImGui.sameLine();
            } else {
               ImGui.tableNextColumn();
            }

            if (property instanceof EnumProperty<?> enumProperty) {
               String[] values = new String[enumProperty.getPossibleValues().size() + 1];
               values[0] = AxiomI18n.get("axiom.widget.any_block_property");
               int currentIndex = 0;
               Iterator<?> iterator = enumProperty.getPossibleValues().iterator();

               for (int i = 1; i < values.length; i++) {
                  Enum<?> enumVal = (Enum<?>)iterator.next();
                  values[i] = StringUtils.convertSnakeToWords(((StringRepresentable)enumVal).getSerializedName());
                  if (enumVal == value) {
                     currentIndex = i;
                  }
               }

               if (BuildConfig.DEBUG && currentIndex == 0 && value != null) {
                  throw new FaultyImplementationError();
               }

               ImGui.setNextItemWidth(!compressed ? 128.0F : Math.min(ImGui.getContentRegionAvailX(), 128.0F));
               int[] array = new int[]{currentIndex};
               ImGuiHelper.combo("##Combo" + prettyName, array, values);
               if (array[0] != currentIndex) {
                  propertiesChanged = true;
                  if (array[0] == 0) {
                     this.selectedProperties.put(property, null);
                  } else {
                     iterator = enumProperty.getPossibleValues().iterator();
                     Enum<?> enumVal = null;

                     for (int ix = 0; ix < array[0]; ix++) {
                        enumVal = (Enum<?>)iterator.next();
                     }

                     this.selectedProperties.put(property, enumVal);
                  }
               }
            } else if (!(property instanceof StringProperty stringProperty)) {
               if (property instanceof BooleanProperty) {
                  if (compressed && !(ImGui.getContentRegionAvailX() >= 128.0F)) {
                     ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
                     int currentValue = value == null ? 0 : ((Boolean)value ? 1 : 2);
                     int[] array = new int[]{currentValue};
                     ImGuiHelper.combo(
                        "##BoolCombo" + prettyName,
                        new int[]{0},
                        new String[]{
                           AxiomI18n.get("axiom.widget.any_block_property"),
                           AxiomI18n.get("axiom.widget.true_block_property"),
                           AxiomI18n.get("axiom.widget.false_block_property")
                        }
                     );
                     if (array[0] != currentValue) {
                        propertiesChanged = true;
                        if (array[0] == 1) {
                           this.selectedProperties.put(property, true);
                        } else if (array[0] == 2) {
                           this.selectedProperties.put(property, false);
                        } else {
                           this.selectedProperties.put(property, null);
                        }
                     }
                  } else {
                     if (value != null && (Boolean)value) {
                        ImGui.beginDisabled();
                     }

                     if (ImGui.button(AxiomI18n.get("axiom.widget.true_block_property") + "##Bool" + prettyName)) {
                        propertiesChanged = true;
                        this.selectedProperties.put(property, true);
                     }

                     if (value != null && (Boolean)value) {
                        ImGui.endDisabled();
                     }

                     ImGui.sameLine();
                     if (value != null && !(Boolean)value) {
                        ImGui.beginDisabled();
                     }

                     if (ImGui.button(AxiomI18n.get("axiom.widget.false_block_property") + "##Bool" + prettyName)) {
                        propertiesChanged = true;
                        this.selectedProperties.put(property, false);
                     }

                     if (value != null && !(Boolean)value) {
                        ImGui.endDisabled();
                     }

                     ImGui.sameLine();
                     if (value == null) {
                        ImGui.beginDisabled();
                     }

                     if (ImGui.button(AxiomI18n.get("axiom.widget.any_block_property") + "##Bool" + prettyName)) {
                        propertiesChanged = true;
                        this.selectedProperties.put(property, null);
                     }

                     if (value == null) {
                        ImGui.endDisabled();
                     }
                  }
               } else if (property instanceof IntegerProperty integerProperty) {
                  if (value == null) {
                     int[] array = new int[]{integerProperty.min - 1};
                     ImGui.setNextItemWidth(!compressed ? 128.0F : Math.min(ImGui.getContentRegionAvailX(), 128.0F));
                     ImGui.sliderInt(
                        "##Slider" + prettyName, array, integerProperty.min - 1, integerProperty.max, AxiomI18n.get("axiom.widget.any_block_property")
                     );
                     if (array[0] >= integerProperty.min && array[0] <= integerProperty.max) {
                        propertiesChanged = true;
                        this.selectedProperties.put(property, array[0]);
                     }
                  } else {
                     int[] array = new int[]{(Integer)value};
                     ImGui.setNextItemWidth(!compressed ? 128.0F : Math.min(ImGui.getContentRegionAvailX(), 128.0F));
                     ImGui.sliderInt("##Slider" + prettyName, array, integerProperty.min - 1, integerProperty.max);
                     if (array[0] != (Integer)value) {
                        propertiesChanged = true;
                        if (array[0] >= integerProperty.min && array[0] <= integerProperty.max) {
                           this.selectedProperties.put(property, array[0]);
                        } else {
                           this.selectedProperties.put(property, null);
                        }
                     }
                  }
               } else {
                  ImGui.text(String.valueOf(property.getClass()));
               }
            } else {
               String[] values = new String[stringProperty.getPossibleValues().size() + 1];
               values[0] = AxiomI18n.get("axiom.widget.any_block_property");
               int currentIndex = 0;
               int index = 1;

               for (String stringValue : stringProperty.getPossibleValues()) {
                  values[index] = StringUtils.convertSnakeToWords(stringValue);
                  if (stringValue.equals(value)) {
                     currentIndex = index;
                  }

                  index++;
               }

               if (BuildConfig.DEBUG && currentIndex == 0 && value != null) {
                  throw new FaultyImplementationError();
               }

               ImGui.setNextItemWidth(!compressed ? 128.0F : Math.min(ImGui.getContentRegionAvailX(), 128.0F));
               int[] array = new int[]{currentIndex};
               ImGuiHelper.combo("##Combo" + prettyName, array, values);
               if (array[0] != currentIndex) {
                  propertiesChanged = true;
                  if (array[0] == 0) {
                     this.selectedProperties.put(property, null);
                  } else {
                     Iterator<String> iterator = stringProperty.getPossibleValues().iterator();
                     String newValue = null;

                     for (int ix = 0; ix < array[0]; ix++) {
                        newValue = iterator.next();
                     }

                     this.selectedProperties.put(property, newValue);
                  }
               }
            }

            ImGui.sameLine(0.0F, 0.0F);
            maxX = Math.max(ImGui.getCursorPosX(), maxX);
            ImGui.dummy(0.0F, 0.0F);
         }

         if (!compressed) {
            ImGui.endTable();
         }
      }

      if (propertiesChanged) {
         this.visualBlockState = this.block.axiom$defaultCustomState();

         for (Entry<Property<?>, Comparable<?>> entry : this.selectedProperties.entrySet()) {
            Comparable<?> valuex = entry.getValue();
            if (valuex != null) {
               this.visualBlockState = this.visualBlockState.setPropertyUnsafe(entry.getKey(), valuex);
            }
         }
      }

      return new BlockConditionWidget.PropertySettingsResult(maxX - start, propertiesChanged);
   }

   public boolean hasEditableProperties() {
      return !this.selectedProperties.isEmpty();
   }

   public BlockCondition createCondition() {
      return createCondition(this.block, this.selectedProperties, this.tag);
   }

   private static BlockCondition createCondition(
      CustomBlock customBlock, Map<Property<?>, Comparable<?>> selectedProperties, BlockList.MinecraftOrCustomTagSet tag
   ) {
      BlockCondition fromTag = BlockCondition.fromMinecraftOrCustomTag(tag);
      if (fromTag != null) {
         return fromTag;
      } else {
         List<CustomBlockState> possibleStates = customBlock.axiom$getPossibleCustomStates();
         if (possibleStates.size() == 1) {
            return new BlockCondition.SpecificState(possibleStates.get(0).getVanillaState());
         } else {
            List<CustomBlockState> var9 = new ArrayList<>(possibleStates);

            for (Entry<Property<?>, Comparable<?>> entry : selectedProperties.entrySet()) {
               Property<?> property = entry.getKey();
               Comparable<?> value = entry.getValue();
               if (value != null) {
                  var9.removeIf(state -> state.getProperty(property) != value);
               }
            }

            if (var9.size() == 0) {
               throw new FaultyImplementationError("No possible states");
            } else if (var9.size() == 1) {
               return new BlockCondition.SpecificState(((CustomBlockState)var9.get(0)).getVanillaState());
            } else {
               Set<BlockState> vanillaStateSet = new HashSet<>();

               for (CustomBlockState possibleState : var9) {
                  vanillaStateSet.add(possibleState.getVanillaState());
               }

               Block block = null;

               for (BlockState blockState : vanillaStateSet) {
                  if (block == null) {
                     block = blockState.getBlock();
                  } else if (block != blockState.getBlock()) {
                     block = null;
                     break;
                  }
               }

               return (BlockCondition)(block != null && block.getStateDefinition().getPossibleStates().size() == vanillaStateSet.size()
                  ? new BlockCondition.AnyState(block)
                  : new BlockCondition.StateSet(vanillaStateSet));
            }
         }
      }
   }

   public BlockConditionWidget.BlockConditionState createState() {
      return this.tag != null
         ? new BlockConditionWidget.BlockConditionState(null, null, this.tag)
         : new BlockConditionWidget.BlockConditionState(this.block, new HashMap<>(this.selectedProperties), null);
   }

   public void revertState(BlockConditionWidget.BlockConditionState blockConditionState) {
      if (blockConditionState.tag != null) {
         this.tag = blockConditionState.tag;
         this.block = null;
         this.visualBlockState = null;
         this.selectedProperties.clear();
      } else {
         this.tag = null;
         this.block = blockConditionState.block;
         this.visualBlockState = this.block.axiom$defaultCustomState();
         this.selectedProperties.clear();

         for (Entry<Property<?>, Comparable<?>> entry : blockConditionState.selectedProperties.entrySet()) {
            Comparable<?> value = entry.getValue();
            this.selectedProperties.put(entry.getKey(), value);
            if (value != null) {
               this.visualBlockState = this.visualBlockState.setPropertyUnsafe(entry.getKey(), value);
            }
         }
      }
   }

   @Nullable
   public CustomBlock getBlock() {
      return this.block;
   }

   @Nullable
   public BlockList.MinecraftOrCustomTagSet getTag() {
      return this.tag;
   }

   private void switchBlock(CustomBlock block) {
      this.tag = null;
      this.visualBlockState = block.axiom$defaultCustomState();
      if (this.block != null && this.block != block) {
         this.selectedProperties.keySet().retainAll(this.visualBlockState.getProperties());

         for (Property<?> property : this.visualBlockState.getProperties()) {
            if (!this.selectedProperties.containsKey(property)) {
               this.selectedProperties.put(property, null);
            }
         }

         for (Entry<Property<?>, Comparable<?>> entry : this.selectedProperties.entrySet()) {
            Comparable<?> value = entry.getValue();
            if (value != null) {
               this.visualBlockState = this.visualBlockState.setPropertyUnsafe(entry.getKey(), value);
            }
         }
      } else {
         this.selectedProperties.clear();

         for (Property<?> propertyx : this.visualBlockState.getProperties()) {
            this.selectedProperties.put(propertyx, null);
         }
      }

      this.block = block;
   }

   public record BlockConditionState(CustomBlock block, Map<Property<?>, Comparable<?>> selectedProperties, BlockList.MinecraftOrCustomTagSet tag) {
      public BlockCondition createCondition() {
         return BlockConditionWidget.createCondition(this.block, this.selectedProperties, this.tag);
      }
   }

   public record PropertySettingsResult(float width, boolean changed) {
   }
}
