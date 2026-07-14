package com.moulberry.axiom.world_modification.undo;

import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.path.PathTool;
import com.moulberry.axiom.tools.path.PointConfig;
import java.util.List;
import net.minecraft.core.BlockPos;

public record CreatePathAdditionalUndoOperation(
   List<BlockPos> points,
   List<PointConfig> pointConfigs,
   int curveType,
   boolean looped,
   boolean useStairsAndSlabs,
   int shape,
   int radius,
   int depth,
   int endRadius,
   boolean inverted,
   int slack,
   boolean keepExisting,
   boolean extendToGround
) implements AdditionalUndoOperation {
   @Override
   public void perform() {
      if (ToolManager.isToolActive() && ToolManager.getCurrentTool() instanceof PathTool pathTool) {
         pathTool.restore(
            this.points,
            this.pointConfigs,
            this.curveType,
            this.looped,
            this.useStairsAndSlabs,
            this.shape,
            this.radius,
            this.endRadius,
            this.depth,
            this.inverted,
            this.slack,
            this.keepExisting,
            this.extendToGround
         );
      }
   }
}
