package com.moulberry.axiom.world_modification.undo;

import com.moulberry.axiom.packets.AxiomServerboundDeleteEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DeleteEntityAdditionalUndoOperation(List<UUID> entitiesToDelete) implements AdditionalUndoOperation {
   @Override
   public void perform() {
      new AxiomServerboundDeleteEntity(new ArrayList<>(this.entitiesToDelete())).send();
      this.entitiesToDelete().clear();
   }
}
