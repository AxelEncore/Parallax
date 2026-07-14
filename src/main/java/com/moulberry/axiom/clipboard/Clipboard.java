package com.moulberry.axiom.clipboard;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.blueprint.Blueprint;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.windows.BlueprintCreateWindow;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public enum Clipboard {
   INSTANCE;

   private ClipboardObject clipboardObject;
   private AtomicInteger clipboardId = new AtomicInteger();

   public int setClipboard(
      ChunkedBlockRegion blockBuffer,
      Long2ObjectMap<CompressedBlockEntity> blockEntities,
      List<CompoundTag> entities,
      String name,
      float preferredYaw,
      boolean containsAir
   ) {
      if (blockBuffer.isEmpty()) {
         throw new IllegalArgumentException("blockBuffer must not be empty");
      } else {
         return this.setClipboard(new ClipboardObject.Anonymous(blockBuffer, blockEntities, entities, name, preferredYaw, containsAir));
      }
   }

   public int setClipboard(Blueprint blueprint) {
      if (!AxiomClient.hasPermission(AxiomPermission.CAN_IMPORT_BLOCKS)) {
         ChatUtils.error("The server has disallowed the use of blueprints");
         return -1;
      } else if (blueprint.blockRegion().isEmpty()) {
         throw new IllegalArgumentException("blockBuffer must not be empty");
      } else {
         return this.setClipboard(new ClipboardObject.FromBlueprint(blueprint));
      }
   }

   public int setClipboard(ClipboardObject clipboardObject) {
      this.clipboardObject = clipboardObject;
      return this.clipboardId.incrementAndGet();
   }

   public void clearClipboard() {
      this.clipboardObject = null;
   }

   public boolean hasClipboardId(int id) {
      return this.clipboardObject == null ? false : this.clipboardId.get() == id;
   }

   public boolean isEmpty() {
      return this.clipboardObject == null || this.clipboardObject.blockRegion().isEmpty();
   }

   @Nullable
   public ClipboardObject getClipboard() {
      return this.clipboardObject;
   }

   public void rotate(Axis axis, int amount) {
      this.setClipboard(ClipboardObject.rotate(this.clipboardObject, axis, amount));
   }

   public void flip(Axis axis) {
      this.setClipboard(ClipboardObject.flip(this.clipboardObject, axis));
   }

   public UserAction.ActionResult callAction(UserAction action, Object object) {
      if (!EditorUI.isActive()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else if (this.clipboardObject != null && !this.clipboardObject.blockRegion().isEmpty()) {
         switch (action) {
            case PASTE:
               RayCaster.RaycastResult raycast = Tool.raycastBlock();
               if (raycast != null) {
                  ChunkedBlockRegion blockRegion = this.clipboardObject.blockRegion();
                  Axis axis = raycast.getDirection().getAxis();
                  int max = blockRegion.max().get(axis);
                  int min = blockRegion.min().get(axis);
                  int count = (max - min + 1) / 2 + 1;
                  if (Math.abs(max - min) % 2 == 1 && raycast.getDirection().getAxisDirection() == AxisDirection.POSITIVE) {
                     count--;
                  }

                  BlockPos offset = raycast.getBlockPos().relative(raycast.getDirection(), count);
                  Placement.INSTANCE
                     .startPlacement(
                        offset,
                        this.clipboardObject.blockRegion(),
                        this.clipboardObject.blockEntities(),
                        this.clipboardObject.entities(),
                        this.clipboardObject.containsAir(),
                        this.clipboardObject.placementDescription()
                     );
                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case SAVE:
               BlueprintCreateWindow.open(
                  this.clipboardObject.blockRegion(), this.clipboardObject.blockEntities(), this.clipboardObject.entities(), this.clipboardObject.containsAir()
               );
               return UserAction.ActionResult.USED_STOP;
            default:
               return UserAction.ActionResult.NOT_HANDLED;
         }
      } else {
         return UserAction.ActionResult.NOT_HANDLED;
      }
   }
}
