package com.moulberry.axiom.mask;

import com.google.common.collect.UnmodifiableIterator;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.elements.AllMaskElement;
import com.moulberry.axiom.mask.elements.AnyMaskElement;
import com.moulberry.axiom.mask.elements.BlockConditionMaskElement;
import com.moulberry.axiom.mask.elements.BothMaskElement;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.mask.elements.EitherMaskElement;
import com.moulberry.axiom.mask.elements.LuaMaskElement;
import com.moulberry.axiom.mask.elements.NotMaskElement;
import com.moulberry.axiom.mask.elements.SelectedMaskElement;
import com.moulberry.axiom.mask.elements.SolidMaskElement;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.elevation.ElevationTool;
import com.moulberry.axiom.tools.modelling.ModellingTool;
import com.moulberry.axiom.tools.path.PathTool;
import com.moulberry.axiom.tools.shape.ShapeTool;
import com.moulberry.axiom.utils.BlockCondition;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MaskManager {
   private static MaskElement configuredMask = null;
   public static int[] mode = new int[]{0};
   public static boolean useLuaScript = false;
   public static String luaScript = null;
   private static final Set<BlockState> SOLID_BLOCKS = new HashSet<>();
   private static boolean calculatedSolidBlocks = false;

   private static void calculateSolidBlockSet() {
      if (!calculatedSolidBlocks) {
         calculatedSolidBlocks = true;
         SOLID_BLOCKS.clear();

         for (Block block : BuiltInRegistries.BLOCK) {
            UnmodifiableIterator var2 = block.getStateDefinition().getPossibleStates().iterator();

            while (var2.hasNext()) {
               BlockState possibleState = (BlockState)var2.next();
               if (possibleState.blocksMotion()) {
                  SOLID_BLOCKS.add(possibleState);
               }
            }
         }
      }
   }

   public static MaskElement createSolidSourceMask() {
      return createSolidMask(2);
   }

   public static MaskElement createSolidDestMask() {
      return createSolidMask(1);
   }

   public static MaskElement getSourceMask() {
      return getMask(2, true);
   }

   public static MaskElement getDestMask() {
      return getMask(1, true);
   }

   public static MaskElement getDestMaskWithoutDefaultSelection() {
      return getMask(1, false);
   }

   private static MaskElement createSolidMask(int exclude) {
      if (!EditorUI.isActive()) {
         return new SolidMaskElement();
      } else if (mode[0] != exclude && EditorWindowType.TOOL_MASKS.isOpen()) {
         if (useLuaScript && luaScript != null) {
            return getLuaMask(true);
         } else if (configuredMask == null) {
            return new BothMaskElement(new SelectedMaskElement(), new SolidMaskElement());
         } else {
            boolean addSelection = !Selection.getSelectionBuffer().isEmpty() && !doesMaskContainSelection(configuredMask);
            MaskManager.MaybeNegatedSet possibleStates = possibleBlocks(configuredMask);
            boolean addSolid;
            if (possibleStates == null) {
               addSolid = true;
            } else if (possibleStates.negated) {
               calculateSolidBlockSet();
               addSolid = !possibleStates.set.containsAll(SOLID_BLOCKS);
            } else {
               addSolid = false;

               for (BlockState blockState : possibleStates.set) {
                  if (blockState.blocksMotion()) {
                     addSolid = true;
                     break;
                  }
               }
            }

            if (addSolid && addSelection) {
               return new AllMaskElement(new SelectedMaskElement(), new SolidMaskElement(), configuredMask);
            } else if (addSelection) {
               return new BothMaskElement(new SelectedMaskElement(), configuredMask);
            } else {
               return (MaskElement)(addSolid ? new BothMaskElement(new SolidMaskElement(), configuredMask) : configuredMask);
            }
         }
      } else {
         return (MaskElement)(Selection.getSelectionBuffer().isEmpty()
            ? new SolidMaskElement()
            : new BothMaskElement(new SelectedMaskElement(), new SolidMaskElement()));
      }
   }

   public static MaskElement getSelectionMask() {
      if (!EditorUI.isActive()) {
         return new ConstantMaskElement(true);
      } else if (!EditorWindowType.TOOL_MASKS.isOpen()) {
         return new ConstantMaskElement(true);
      } else if (useLuaScript && luaScript != null) {
         return getLuaMask(false);
      } else {
         return (MaskElement)(configuredMask == null ? new ConstantMaskElement(true) : configuredMask);
      }
   }

   private static MaskElement getMask(int exclude, boolean addDefaultSelection) {
      if (!EditorUI.isActive()) {
         return new ConstantMaskElement(true);
      } else if (mode[0] != exclude && EditorWindowType.TOOL_MASKS.isOpen()) {
         if (useLuaScript && luaScript != null) {
            return getLuaMask(false);
         } else if (configuredMask == null) {
            return (MaskElement)(addDefaultSelection && !Selection.getSelectionBuffer().isEmpty() ? new SelectedMaskElement() : new ConstantMaskElement(true));
         } else {
            return (MaskElement)(addDefaultSelection && !Selection.getSelectionBuffer().isEmpty() && !doesMaskContainSelection(configuredMask)
               ? new BothMaskElement(new SelectedMaskElement(), configuredMask)
               : configuredMask);
         }
      } else {
         return (MaskElement)(addDefaultSelection && !Selection.getSelectionBuffer().isEmpty() ? new SelectedMaskElement() : new ConstantMaskElement(true));
      }
   }

   private static MaskElement getLuaMask(boolean solid) {
      LuaMaskElement luaMaskElement = new LuaMaskElement(luaScript);
      if (Selection.getSelectionBuffer().isEmpty()) {
         return (MaskElement)(solid ? new BothMaskElement(new SolidMaskElement(), luaMaskElement) : luaMaskElement);
      } else {
         return (MaskElement)(solid
            ? new AllMaskElement(new SelectedMaskElement(), new SolidMaskElement(), luaMaskElement)
            : new BothMaskElement(new SelectedMaskElement(), luaMaskElement));
      }
   }

   public static boolean hasConfiguredMask() {
      return configuredMask != null;
   }

   public static void setConfiguredMask(MaskElement maskElement, boolean updateCmdString, boolean updateVisualCode) {
      if (maskElement != null) {
         maskElement = MaskElementOptimizer.optimizeStatic(maskElement);
      }

      configuredMask = maskElement;
      if (updateCmdString || updateVisualCode) {
         ToolMaskWindow.onMaskUpdated(maskElement, updateCmdString, updateVisualCode);
      }

      if (ToolManager.getCurrentTool() instanceof PathTool pathTool) {
         pathTool.markDirty();
      }

      if (ToolManager.getCurrentTool() instanceof ShapeTool shapeTool) {
         shapeTool.markDirty();
      }

      if (ToolManager.getCurrentTool() instanceof ModellingTool modellingTool) {
         modellingTool.markDirty();
      }

      if (ToolManager.getCurrentTool() instanceof ElevationTool elevationTool) {
         elevationTool.cachedSourceMask = null;
      }
   }

   private static boolean doesMaskContainSelection(MaskElement maskElement) {
      if (maskElement instanceof SelectedMaskElement) {
         return true;
      } else if (maskElement instanceof NotMaskElement notMaskElement) {
         return doesMaskContainSelection(notMaskElement.getChild());
      } else if (maskElement instanceof BothMaskElement bothMaskElement) {
         return doesMaskContainSelection(bothMaskElement.getChild1()) || doesMaskContainSelection(bothMaskElement.getChild2());
      } else {
         if (maskElement instanceof AllMaskElement allMaskElement) {
            for (MaskElement child : allMaskElement.getChildren()) {
               if (doesMaskContainSelection(child)) {
                  return true;
               }
            }
         } else {
            if (maskElement instanceof EitherMaskElement eitherMaskElement) {
               return doesMaskContainSelection(eitherMaskElement.getChild1()) || doesMaskContainSelection(eitherMaskElement.getChild2());
            }

            if (maskElement instanceof AnyMaskElement anyMaskElement) {
               for (MaskElement childx : anyMaskElement.getChildren()) {
                  if (doesMaskContainSelection(childx)) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   @Nullable
   private static MaskManager.MaybeNegatedSet possibleBlocks(MaskElement maskElement) {
      if (maskElement instanceof BothMaskElement bothMaskElement) {
         MaskManager.MaybeNegatedSet set1 = possibleBlocks(bothMaskElement.getChild1());
         MaskManager.MaybeNegatedSet set2 = possibleBlocks(bothMaskElement.getChild2());
         if (set1 == null) {
            return set2;
         } else if (set2 == null) {
            return set1;
         } else if (!set1.negated) {
            if (!set2.negated) {
               set1.set.retainAll(set2.set);
               return set1;
            } else {
               set1.set.removeAll(set2.set);
               return set1;
            }
         } else if (!set2.negated) {
            set2.set.removeAll(set1.set);
            return set2;
         } else {
            set1.set.addAll(set2.set);
            return set1;
         }
      } else if (maskElement instanceof AllMaskElement allMaskElement) {
         MaskManager.MaybeNegatedSet set1 = null;

         for (MaskElement child : allMaskElement.getChildren()) {
            MaskManager.MaybeNegatedSet set2 = possibleBlocks(child);
            if (set2 != null) {
               if (set1 == null) {
                  set1 = set2;
               } else if (!set1.negated) {
                  if (!set2.negated) {
                     set1.set.retainAll(set2.set);
                  } else {
                     set1.set.removeAll(set2.set);
                  }
               } else if (!set2.negated) {
                  set2.set.removeAll(set1.set);
                  set1 = set2;
               } else {
                  set1.set.addAll(set2.set);
               }
            }
         }

         return set1;
      } else if (maskElement instanceof EitherMaskElement eitherMaskElement) {
         MaskManager.MaybeNegatedSet set1 = possibleBlocks(eitherMaskElement.getChild1());
         MaskManager.MaybeNegatedSet set2 = possibleBlocks(eitherMaskElement.getChild2());
         if (set1 == null) {
            return set2;
         } else if (set2 == null) {
            return set1;
         } else if (!set1.negated) {
            if (!set2.negated) {
               set1.set.addAll(set2.set);
               return set1;
            } else {
               set2.set.removeAll(set1.set);
               return set2;
            }
         } else if (!set2.negated) {
            set1.set.removeAll(set2.set);
            return set1;
         } else {
            set1.set.retainAll(set2.set);
            return set1;
         }
      } else if (!(maskElement instanceof AnyMaskElement anyMaskElement)) {
         if (maskElement instanceof NotMaskElement notMaskElement) {
            MaskManager.MaybeNegatedSet set = possibleBlocks(notMaskElement.getChild());
            if (set != null) {
               set = new MaskManager.MaybeNegatedSet(set.set, !set.negated);
            }

            return set;
         } else {
            return maskElement instanceof BlockConditionMaskElement blockConditionMaskElement ? createSet(blockConditionMaskElement.getBlockCondition()) : null;
         }
      } else {
         MaskManager.MaybeNegatedSet set1 = null;

         for (MaskElement childx : anyMaskElement.getChildren()) {
            set1 = union(set1, possibleBlocks(childx));
         }

         return set1;
      }
   }

   private static MaskManager.MaybeNegatedSet createSet(BlockCondition blockCondition) {
      if (blockCondition instanceof BlockCondition.MatchesTag matchesTag) {
         Set<BlockState> possibleStates = new HashSet<>();

         for (Holder<Block> blockHolder : matchesTag.tag()) {
            possibleStates.addAll(((Block)blockHolder.value()).getStateDefinition().getPossibleStates());
         }

         return new MaskManager.MaybeNegatedSet(possibleStates, false);
      } else if (blockCondition instanceof BlockCondition.MatchesCustomTag customTag) {
         Set<BlockState> possibleStates = new HashSet<>();

         for (ResourceLocation resourceLocation : customTag.customTag()) {
            Optional<Block> optional = BuiltInRegistries.BLOCK.getOptional(resourceLocation);
            if (optional.isPresent()) {
               possibleStates.addAll(optional.get().getStateDefinition().getPossibleStates());
            }
         }

         return new MaskManager.MaybeNegatedSet(possibleStates, false);
      } else if (blockCondition instanceof BlockCondition.AnyState anyState) {
         Set<BlockState> possibleStates = new HashSet<>(anyState.block().getStateDefinition().getPossibleStates());
         return new MaskManager.MaybeNegatedSet(possibleStates, false);
      } else if (blockCondition instanceof BlockCondition.SpecificState specificState) {
         Set<BlockState> possibleStates = new HashSet<>();
         possibleStates.add(specificState.blockState());
         return new MaskManager.MaybeNegatedSet(possibleStates, false);
      } else if (blockCondition instanceof BlockCondition.BlockSet blockSet) {
         Set<BlockState> possibleStates = new HashSet<>();

         for (Block block : blockSet.set()) {
            possibleStates.addAll(block.getStateDefinition().getPossibleStates());
         }

         return new MaskManager.MaybeNegatedSet(possibleStates, false);
      } else if (blockCondition instanceof BlockCondition.StateSet stateSet) {
         return new MaskManager.MaybeNegatedSet(new HashSet<>(stateSet.set()), false);
      } else if (!(blockCondition instanceof BlockCondition.AnyCondition anyCondition)) {
         throw new FaultyImplementationError("Don't know how to convert: " + blockCondition.getClass());
      } else {
         MaskManager.MaybeNegatedSet set1 = null;

         for (BlockCondition condition : anyCondition.conditions()) {
            set1 = union(set1, createSet(condition));
         }

         return set1;
      }
   }

   private static MaskManager.MaybeNegatedSet union(MaskManager.MaybeNegatedSet set1, MaskManager.MaybeNegatedSet set2) {
      if (set2 == null) {
         return set1;
      } else if (set1 == null) {
         return set2;
      } else if (!set1.negated) {
         if (!set2.negated) {
            set1.set.addAll(set2.set);
            return set1;
         } else {
            set2.set.removeAll(set1.set);
            return set2;
         }
      } else if (!set2.negated) {
         set1.set.removeAll(set2.set);
         return set1;
      } else {
         set1.set.retainAll(set2.set);
         return set1;
      }
   }

   private record MaybeNegatedSet(Set<BlockState> set, boolean negated) {
   }
}
