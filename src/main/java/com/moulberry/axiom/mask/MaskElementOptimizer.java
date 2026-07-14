package com.moulberry.axiom.mask;

import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.elements.AllMaskElement;
import com.moulberry.axiom.mask.elements.AngleMaskElement;
import com.moulberry.axiom.mask.elements.AnyMaskElement;
import com.moulberry.axiom.mask.elements.BlockAboveConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockAdjacentConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockBelowConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockNearConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockNeighborConditionMaskElement;
import com.moulberry.axiom.mask.elements.BothMaskElement;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.mask.elements.CoordMaskElement;
import com.moulberry.axiom.mask.elements.EitherMaskElement;
import com.moulberry.axiom.mask.elements.GenericBlockConditionMaskElement;
import com.moulberry.axiom.mask.elements.NotMaskElement;
import com.moulberry.axiom.mask.elements.OffsetMaskElement;
import com.moulberry.axiom.utils.BlockCondition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MaskElementOptimizer {
   public static MaskElement optimizeStatic(MaskElement maskElement) {
      while (true) {
         MaskElement optimized = optimizeStaticInner(maskElement);
         if (optimized == null) {
            return maskElement;
         }

         maskElement = optimized;
      }
   }

   private static MaskElement optimizeStaticInner(MaskElement maskElement) {
      if (!(maskElement instanceof BothMaskElement) && !(maskElement instanceof AllMaskElement)) {
         if (!(maskElement instanceof EitherMaskElement) && !(maskElement instanceof AnyMaskElement)) {
            if (maskElement instanceof NotMaskElement notMaskElement) {
               MaskElement child = notMaskElement.getChild();
               if (child instanceof NotMaskElement childNot) {
                  return childNot.getChild();
               } else if (child instanceof AngleMaskElement angleMaskElement) {
                  return new AngleMaskElement(angleMaskElement.getAngle(), MaskElement.invertComparison(angleMaskElement.getComparison()));
               } else if (child instanceof CoordMaskElement coordMaskElement) {
                  return new CoordMaskElement(
                     coordMaskElement.getAxis(), coordMaskElement.getValue(), MaskElement.invertComparison(coordMaskElement.getComparison())
                  );
               } else {
                  MaskElement optimized = optimizeStaticInner(child);
                  return optimized != null ? new NotMaskElement(optimized) : null;
               }
            } else if (maskElement instanceof OffsetMaskElement offsetMaskElement) {
               MaskElement child = offsetMaskElement.getChild();
               MaskElement optimized = optimizeStaticInner(child);
               return optimized != null
                  ? new OffsetMaskElement(optimized, offsetMaskElement.getOffsetX(), offsetMaskElement.getOffsetY(), offsetMaskElement.getOffsetZ())
                  : null;
            } else {
               return null;
            }
         } else {
            List<MaskElement> children = new ArrayList<>();
            if (maskElement instanceof EitherMaskElement eitherMaskElement) {
               children.add(eitherMaskElement.getChild1());
               children.add(eitherMaskElement.getChild2());
            } else {
               MaskElement[] childArray = ((AnyMaskElement)maskElement).getChildren();
               children.addAll(Arrays.asList(childArray));
            }

            boolean changed = false;
            int i = 0;

            while (i < children.size()) {
               MaskElement child = children.remove(i);
               if (child instanceof EitherMaskElement eitherMaskElementChild) {
                  changed = true;
                  children.add(i, eitherMaskElementChild.getChild1());
                  children.add(i + 1, eitherMaskElementChild.getChild2());
               } else if (child instanceof AnyMaskElement anyMaskElementChild) {
                  changed = true;
                  MaskElement[] childChildren = anyMaskElementChild.getChildren();

                  for (int j = 0; j < childChildren.length; j++) {
                     children.add(i + j, childChildren[j]);
                  }
               } else if (child instanceof ConstantMaskElement constantMaskElement) {
                  if (constantMaskElement.getConstant()) {
                     return constantMaskElement;
                  }
               } else {
                  MaskElement optimized = optimizeStaticInner(child);
                  if (optimized != null) {
                     changed = true;
                     children.add(i, optimized);
                  } else {
                     children.add(i, child);
                     i++;
                  }
               }
            }

            for (int ix = 0; ix < children.size(); ix++) {
               MaskElement child = children.get(ix);
               if (child instanceof GenericBlockConditionMaskElement blockConditionMaskElement) {
                  List<GenericBlockConditionMaskElement> combineAll = new ArrayList<>();
                  combineAll.add(blockConditionMaskElement);

                  for (int j = ix + 1; j < children.size(); j++) {
                     MaskElement other = children.get(j);
                     if (other instanceof GenericBlockConditionMaskElement otherBlockConditionMaskElement
                        && blockConditionMaskElement.canCombine(otherBlockConditionMaskElement)) {
                        combineAll.add(otherBlockConditionMaskElement);
                        children.remove(j);
                        j--;
                     }
                  }

                  if (combineAll.size() >= 2) {
                     List<BlockCondition> blockConditions = new ArrayList<>();
                     List<BlockConditionWidget.BlockConditionState> blockConditionStates = new ArrayList<>();

                     for (GenericBlockConditionMaskElement combine : combineAll) {
                        if (combine.getBlockCondition() instanceof BlockCondition.AnyCondition anyCondition) {
                           blockConditions.addAll(anyCondition.conditions());
                        } else {
                           blockConditions.add(combine.getBlockCondition());
                        }

                        blockConditionStates.addAll(combine.getConditionStates());
                     }

                     BlockCondition any = new BlockCondition.AnyCondition(blockConditions);
                     if (child instanceof BlockConditionMaskElement) {
                        children.set(ix, new BlockConditionMaskElement(any, blockConditionStates));
                     } else if (child instanceof BlockAboveConditionMaskElement) {
                        children.set(ix, new BlockAboveConditionMaskElement(any, blockConditionStates));
                     } else if (child instanceof BlockBelowConditionMaskElement) {
                        children.set(ix, new BlockBelowConditionMaskElement(any, blockConditionStates));
                     } else if (child instanceof BlockNearConditionMaskElement near) {
                        children.set(ix, new BlockNearConditionMaskElement(any, blockConditionStates, near.radius));
                     } else if (child instanceof BlockNeighborConditionMaskElement) {
                        children.set(ix, new BlockNeighborConditionMaskElement(any, blockConditionStates));
                     } else {
                        if (!(child instanceof BlockAdjacentConditionMaskElement)) {
                           throw new FaultyImplementationError("Unknown block condition: " + child.getClass());
                        }

                        children.set(ix, new BlockAdjacentConditionMaskElement(any, blockConditionStates));
                     }
                  }
               }
            }

            if (children.size() == 0) {
               return new ConstantMaskElement(false);
            } else if (children.size() == 1) {
               return children.get(0);
            } else if (children.size() == 2 && (changed || !(maskElement instanceof EitherMaskElement))) {
               return new EitherMaskElement(children.get(0), children.get(1));
            } else if (!changed) {
               return null;
            } else {
               MaskElement[] childArray = children.toArray(new MaskElement[0]);
               return new AnyMaskElement(childArray);
            }
         }
      } else {
         List<MaskElement> childrenx = new ArrayList<>();
         if (maskElement instanceof BothMaskElement bothMaskElement) {
            childrenx.add(bothMaskElement.getChild1());
            childrenx.add(bothMaskElement.getChild2());
         } else {
            MaskElement[] childArray = ((AllMaskElement)maskElement).getChildren();
            childrenx.addAll(Arrays.asList(childArray));
         }

         boolean changed = false;
         int i = 0;

         while (i < childrenx.size()) {
            MaskElement child = childrenx.remove(i);
            if (child instanceof BothMaskElement bothMaskElementChild) {
               changed = true;
               childrenx.add(i, bothMaskElementChild.getChild1());
               childrenx.add(i + 1, bothMaskElementChild.getChild2());
            } else if (child instanceof AllMaskElement allMaskElementChild) {
               changed = true;
               MaskElement[] childChildren = allMaskElementChild.getChildren();

               for (int jx = 0; jx < childChildren.length; jx++) {
                  childrenx.add(i + jx, childChildren[jx]);
               }
            } else if (child instanceof ConstantMaskElement constantMaskElementx) {
               if (!constantMaskElementx.getConstant()) {
                  return constantMaskElementx;
               }
            } else {
               MaskElement optimized = optimizeStaticInner(child);
               if (optimized != null) {
                  changed = true;
                  childrenx.add(i, optimized);
               } else {
                  childrenx.add(i, child);
                  i++;
               }
            }
         }

         if (childrenx.size() == 0) {
            return new ConstantMaskElement(false);
         } else if (childrenx.size() == 1) {
            return childrenx.get(0);
         } else if (childrenx.size() == 2 && (changed || !(maskElement instanceof BothMaskElement))) {
            return new BothMaskElement(childrenx.get(0), childrenx.get(1));
         } else if (!changed) {
            return null;
         } else {
            MaskElement[] childArray = childrenx.toArray(new MaskElement[0]);
            return new AllMaskElement(childArray);
         }
      }
   }
}
