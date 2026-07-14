package com.moulberry.axiom.mask;

import com.moulberry.axiom.editor.widgets.BlockConditionWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.AndMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.AngleMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BiomeMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockAboveMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockAdjacentMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockBelowMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockNearMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.BlockNeighborMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.CanSeeSkyMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.CoordMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.InSelectionMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.MaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.NotMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.OffsetMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.OrMaskWidget;
import com.moulberry.axiom.editor.windows.global_mask.visualcode.SurfaceMaskWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.elements.AllMaskElement;
import com.moulberry.axiom.mask.elements.AngleMaskElement;
import com.moulberry.axiom.mask.elements.AnyMaskElement;
import com.moulberry.axiom.mask.elements.BiomeConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockAboveConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockAdjacentConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockBelowConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockNearConditionMaskElement;
import com.moulberry.axiom.mask.elements.BlockNeighborConditionMaskElement;
import com.moulberry.axiom.mask.elements.BothMaskElement;
import com.moulberry.axiom.mask.elements.CanSeeSkyMaskElement;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.mask.elements.CoordMaskElement;
import com.moulberry.axiom.mask.elements.EitherMaskElement;
import com.moulberry.axiom.mask.elements.NotMaskElement;
import com.moulberry.axiom.mask.elements.OffsetMaskElement;
import com.moulberry.axiom.mask.elements.SelectedMaskElement;
import com.moulberry.axiom.mask.elements.SurfaceMaskElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class VisualCodeConverter {
   public static MaskElement fromVisualCode(MaskWidget maskWidget) {
      if (maskWidget instanceof AndMaskWidget andMaskWidget) {
         List<MaskWidget> children = andMaskWidget.getChildren();
         List<MaskElement> maskElements = new ArrayList<>();

         for (MaskWidget child : children) {
            MaskElement childElement = fromVisualCode(child);
            if (childElement != null) {
               maskElements.add(childElement);
            }
         }

         return maskElements.size() == 0 ? null : new AllMaskElement(maskElements.toArray(new MaskElement[0]));
      } else if (maskWidget instanceof OrMaskWidget orMaskWidget) {
         List<MaskWidget> children = orMaskWidget.getChildren();
         List<MaskElement> maskElements = new ArrayList<>();

         for (MaskWidget childx : children) {
            MaskElement childElement = fromVisualCode(childx);
            if (childElement != null) {
               maskElements.add(childElement);
            }
         }

         return maskElements.size() == 0 ? null : new AnyMaskElement(maskElements.toArray(new MaskElement[0]));
      } else if (maskWidget instanceof NotMaskWidget notMaskWidget) {
         if (notMaskWidget.getChildren().size() != 1) {
            return null;
         } else {
            MaskElement childElement = fromVisualCode(notMaskWidget.getChildren().get(0));
            return childElement == null ? null : new NotMaskElement(childElement);
         }
      } else if (maskWidget instanceof OffsetMaskWidget offsetMaskWidget) {
         if (offsetMaskWidget.getChildren().size() != 1) {
            return null;
         } else {
            MaskElement childElement = fromVisualCode(offsetMaskWidget.getChildren().get(0));
            return childElement == null
               ? null
               : new OffsetMaskElement(childElement, offsetMaskWidget.getOffsetX(), offsetMaskWidget.getOffsetY(), offsetMaskWidget.getOffsetZ());
         }
      } else if (maskWidget instanceof BiomeMaskWidget biomeMaskWidget) {
         return new BiomeConditionMaskElement(biomeMaskWidget.getBiome());
      } else if (maskWidget instanceof BlockMaskWidget blockMaskWidget) {
         return new BlockConditionMaskElement(blockMaskWidget.createCondition(), List.of(blockMaskWidget.createState()));
      } else if (maskWidget instanceof BlockAboveMaskWidget blockAboveMaskWidget) {
         return new BlockAboveConditionMaskElement(blockAboveMaskWidget.createCondition(), List.of(blockAboveMaskWidget.createState()));
      } else if (maskWidget instanceof BlockBelowMaskWidget blockBelowMaskWidget) {
         return new BlockBelowConditionMaskElement(blockBelowMaskWidget.createCondition(), List.of(blockBelowMaskWidget.createState()));
      } else if (maskWidget instanceof BlockNearMaskWidget blockNearMaskWidget) {
         return new BlockNearConditionMaskElement(
            blockNearMaskWidget.createCondition(), List.of(blockNearMaskWidget.createState()), blockNearMaskWidget.getRadius()
         );
      } else if (maskWidget instanceof BlockNeighborMaskWidget blockNeighborMaskWidget) {
         return new BlockNeighborConditionMaskElement(blockNeighborMaskWidget.createCondition(), List.of(blockNeighborMaskWidget.createState()));
      } else if (maskWidget instanceof BlockAdjacentMaskWidget blockAdjacentMaskWidget) {
         return new BlockAdjacentConditionMaskElement(blockAdjacentMaskWidget.createCondition(), List.of(blockAdjacentMaskWidget.createState()));
      } else if (maskWidget instanceof CoordMaskWidget coordMaskWidget) {
         return new CoordMaskElement(coordMaskWidget.getAxis(), coordMaskWidget.getYValue(), coordMaskWidget.getComparisonType());
      } else if (maskWidget instanceof AngleMaskWidget angleMaskWidget) {
         int angle = angleMaskWidget.getAngle();
         int range = angleMaskWidget.getRange();
         if (range == 0) {
            return new AngleMaskElement(angle, 0);
         } else {
            int minAngle = angle - range;
            int maxAngle = angle + range;
            if (minAngle < -90) {
               minAngle = -90;
            }

            if (maxAngle > 90) {
               maxAngle = 90;
            }

            if (minAngle > maxAngle) {
               return null;
            } else if (minAngle == -90) {
               return maxAngle == 90 ? null : new AngleMaskElement(maxAngle, 4);
            } else {
               return (MaskElement)(maxAngle == 90
                  ? new AngleMaskElement(minAngle, 5)
                  : new BothMaskElement(new AngleMaskElement(minAngle, 5), new AngleMaskElement(maxAngle, 4)));
            }
         }
      } else if (maskWidget instanceof InSelectionMaskWidget) {
         return new SelectedMaskElement();
      } else if (maskWidget instanceof CanSeeSkyMaskWidget) {
         return new CanSeeSkyMaskElement();
      } else if (maskWidget instanceof SurfaceMaskWidget) {
         return new SurfaceMaskElement();
      } else {
         throw new UnsupportedOperationException("Don't know how to convert " + maskWidget.getClass());
      }
   }

   public static MaskWidget toVisualCode(MaskElement maskElement) {
      if (maskElement instanceof BothMaskElement bothMaskElement) {
         if (bothMaskElement.getChild1() instanceof AngleMaskElement angle1
            && bothMaskElement.getChild2() instanceof AngleMaskElement angle2
            && angle1.getComparison() != 1
            && angle2.getComparison() != 1) {
            return combineAngles(List.of(angle1, angle2));
         } else {
            MaskWidget left = toVisualCode(bothMaskElement.getChild1());
            MaskWidget right = toVisualCode(bothMaskElement.getChild2());
            if (left == null) {
               return right;
            } else if (right == null) {
               return left;
            } else {
               AndMaskWidget maskWidget = new AndMaskWidget();
               maskWidget.addChild(-1, left);
               maskWidget.addChild(-1, right);
               return maskWidget;
            }
         }
      } else if (!(maskElement instanceof AllMaskElement allMaskElement)) {
         if (maskElement instanceof EitherMaskElement eitherMaskElement) {
            MaskWidget left = toVisualCode(eitherMaskElement.getChild1());
            MaskWidget right = toVisualCode(eitherMaskElement.getChild2());
            if (left == null) {
               return right;
            } else if (right == null) {
               return left;
            } else {
               OrMaskWidget maskWidget = new OrMaskWidget();
               maskWidget.addChild(-1, left);
               maskWidget.addChild(-1, right);
               return maskWidget;
            }
         } else if (maskElement instanceof AnyMaskElement anyMaskElement) {
            OrMaskWidget maskWidget = new OrMaskWidget();

            for (MaskElement child : anyMaskElement.getChildren()) {
               MaskWidget childWidget = toVisualCode(child);
               if (childWidget != null) {
                  maskWidget.addChild(-1, toVisualCode(child));
               }
            }

            return maskWidget;
         } else if (maskElement instanceof NotMaskElement notMaskElement) {
            MaskWidget childx = toVisualCode(notMaskElement.getChild());
            if (childx == null) {
               return null;
            } else {
               NotMaskWidget maskWidget = new NotMaskWidget();
               maskWidget.addChild(0, childx);
               return maskWidget;
            }
         } else if (maskElement instanceof OffsetMaskElement offsetMaskElement) {
            MaskWidget childx = toVisualCode(offsetMaskElement.getChild());
            if (childx == null) {
               return null;
            } else {
               OffsetMaskWidget maskWidget = new OffsetMaskWidget(
                  offsetMaskElement.getOffsetX(), offsetMaskElement.getOffsetY(), offsetMaskElement.getOffsetZ()
               );
               maskWidget.addChild(0, childx);
               return maskWidget;
            }
         } else if (maskElement instanceof BiomeConditionMaskElement biomeConditionMaskElement) {
            return new BiomeMaskWidget(biomeConditionMaskElement.getMatchBiome());
         } else if (maskElement instanceof BlockConditionMaskElement blockConditionMaskElement) {
            return toVisualCodeBlockCondition(blockConditionMaskElement.getConditionStates(), BlockMaskWidget::new);
         } else if (maskElement instanceof BlockAboveConditionMaskElement blockAboveConditionMaskElement) {
            return toVisualCodeBlockCondition(blockAboveConditionMaskElement.getConditionStates(), BlockAboveMaskWidget::new);
         } else if (maskElement instanceof BlockBelowConditionMaskElement blockBelowConditionMaskElement) {
            return toVisualCodeBlockCondition(blockBelowConditionMaskElement.getConditionStates(), BlockBelowMaskWidget::new);
         } else if (maskElement instanceof BlockNearConditionMaskElement blockNearConditionMaskElement) {
            return toVisualCodeBlockCondition(
               blockNearConditionMaskElement.getConditionStates(), condition -> new BlockNearMaskWidget(condition, blockNearConditionMaskElement.radius)
            );
         } else if (maskElement instanceof BlockNeighborConditionMaskElement blockNeighborConditionMaskElement) {
            return toVisualCodeBlockCondition(blockNeighborConditionMaskElement.getConditionStates(), BlockNeighborMaskWidget::new);
         } else if (maskElement instanceof BlockAdjacentConditionMaskElement blockAdjacentConditionMaskElement) {
            return toVisualCodeBlockCondition(blockAdjacentConditionMaskElement.getConditionStates(), BlockAdjacentMaskWidget::new);
         } else if (maskElement instanceof CoordMaskElement coordMaskElement) {
            return new CoordMaskWidget(coordMaskElement.getAxis(), coordMaskElement.getValue(), coordMaskElement.getComparison());
         } else if (maskElement instanceof AngleMaskElement angleMaskElement) {
            int angle = angleMaskElement.getAngle();
            switch (angleMaskElement.getComparison()) {
               case 0:
                  return new AngleMaskWidget(angle, 0);
               case 1:
                  NotMaskWidget not = new NotMaskWidget();
                  not.addChild(-1, new AngleMaskWidget(angle, 0));
                  return not;
               case 2:
               case 4:
                  if (angle < -90) {
                     return null;
                  }

                  int diff = angle + 90;
                  return new AngleMaskWidget(angle - diff / 2, diff / 2);
               case 3:
               case 5:
                  if (angle > 90) {
                     return null;
                  }

                  diff = 90 - angle;
                  return new AngleMaskWidget(angle + diff / 2, diff / 2);
               default:
                  throw new FaultyImplementationError();
            }
         } else if (maskElement instanceof SelectedMaskElement) {
            return new InSelectionMaskWidget();
         } else if (maskElement instanceof CanSeeSkyMaskElement) {
            return new CanSeeSkyMaskWidget();
         } else if (maskElement instanceof SurfaceMaskElement) {
            return new SurfaceMaskWidget();
         } else if (maskElement instanceof ConstantMaskElement) {
            return null;
         } else {
            throw new UnsupportedOperationException("Don't know how to convert " + maskElement.getClass());
         }
      } else {
         List<AngleMaskElement> angleMaskElements = new ArrayList<>();
         AndMaskWidget maskWidget = new AndMaskWidget();

         for (MaskElement childx : allMaskElement.getChildren()) {
            if (childx instanceof AngleMaskElement angle && angle.getComparison() != 1) {
               angleMaskElements.add(angle);
            } else {
               MaskWidget childWidget = toVisualCode(childx);
               if (childWidget != null) {
                  maskWidget.addChild(-1, childWidget);
               }
            }
         }

         if (!angleMaskElements.isEmpty()) {
            MaskWidget combinedAngleMask = combineAngles(angleMaskElements);
            if (combinedAngleMask != null) {
               maskWidget.addChild(-1, combinedAngleMask);
            }
         }

         return maskWidget;
      }
   }

   private static MaskWidget toVisualCodeBlockCondition(
      List<BlockConditionWidget.BlockConditionState> conditions, Function<BlockConditionWidget.BlockConditionState, MaskWidget> provider
   ) {
      if (conditions.size() == 1) {
         return provider.apply(conditions.get(0));
      } else {
         OrMaskWidget maskWidget = new OrMaskWidget();

         for (BlockConditionWidget.BlockConditionState condition : conditions) {
            maskWidget.addChild(-1, provider.apply(condition));
         }

         return maskWidget;
      }
   }

   private static MaskWidget combineAngles(List<AngleMaskElement> elements) {
      int minAngle = -90;
      int maxAngle = 90;

      for (AngleMaskElement element : elements) {
         int angle = element.getAngle();
         switch (element.getComparison()) {
            case 0:
               if (angle >= minAngle && angle <= maxAngle) {
                  minAngle = angle;
                  maxAngle = angle;
                  break;
               }

               return null;
            case 1:
            default:
               throw new FaultyImplementationError();
            case 2:
            case 4:
               if (minAngle > angle) {
                  return null;
               }

               maxAngle = angle;
               break;
            case 3:
            case 5:
               if (maxAngle < angle) {
                  return null;
               }

               minAngle = angle;
         }
      }

      int range = maxAngle - minAngle;
      return range < 0 ? null : new AngleMaskWidget(minAngle + range / 2, range / 2);
   }
}
