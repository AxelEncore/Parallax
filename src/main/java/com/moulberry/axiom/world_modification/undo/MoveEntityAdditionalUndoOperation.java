package com.moulberry.axiom.world_modification.undo;

import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record MoveEntityAdditionalUndoOperation(List<MoveEntityAdditionalUndoOperation.UUIDAndPosition> entitiesToMove) implements AdditionalUndoOperation {
   @Override
   public void perform() {
      List<AxiomServerboundManipulateEntity.ManipulateEntry> entries = new ArrayList<>();

      for (MoveEntityAdditionalUndoOperation.UUIDAndPosition uuidAndPosition : this.entitiesToMove()) {
         entries.add(
            new AxiomServerboundManipulateEntity.ManipulateEntry(
               uuidAndPosition.uuid(), Set.of(), uuidAndPosition.position(), uuidAndPosition.yaw(), uuidAndPosition.pitch(), null
            )
         );
      }

      new AxiomServerboundManipulateEntity(entries).send();
      this.entitiesToMove().clear();
   }

   public record UUIDAndPosition(UUID uuid, Vec3 position, float yaw, float pitch) {
   }
}
