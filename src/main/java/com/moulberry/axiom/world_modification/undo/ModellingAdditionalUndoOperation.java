package com.moulberry.axiom.world_modification.undo;

import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.modelling.ModellingTool;
import java.util.List;
import net.minecraft.core.BlockPos;

public record ModellingAdditionalUndoOperation(List<List<BlockPos>> positions, int mode, int thickness, boolean keepExisting, boolean extendToGround)
   implements AdditionalUndoOperation {
   @Override
   public void perform() {
      if (ToolManager.isToolActive() && ToolManager.getCurrentTool() instanceof ModellingTool modellingTool) {
         modellingTool.restore(this.positions, this.mode, this.thickness, this.keepExisting, this.extendToGround);
      }
   }
}
