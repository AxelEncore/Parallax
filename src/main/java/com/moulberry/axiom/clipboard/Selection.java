package com.moulberry.axiom.clipboard;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import com.moulberry.axiom.utils.EntityDataUtils;
import com.moulberry.axiom.world_modification.Dispatcher;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Selection {
   private static SelectionBuffer buffer = SelectionBuffer.EMPTY;
   private static boolean renderSelection = true;
   public static boolean copyWithAir = false;
   public static boolean copyWithEntities = false;
   public static boolean makeNextCopyIncludeAir = false;

   public static void render(AxiomWorldRenderContext rc, int effects) {
      if (renderSelection) {
         buffer.render(rc, effects);
      }
   }

   public static SelectionBuffer getSelectionBuffer() {
      return buffer;
   }

   public static boolean shouldRenderSelection() {
      return renderSelection;
   }

   public static void setShouldRenderSelection(boolean renderSelection) {
      Selection.renderSelection = renderSelection;
   }

   public static int selectedBlockCount() {
      return buffer.size();
   }

   public static boolean contains(int x, int y, int z) {
      return !EditorUI.isActive() ? true : buffer.contains(x, y, z);
   }

   public static void setBuffer(SelectionBuffer selectionBuffer) {
      selectionBuffer = selectionBuffer.optimize();
      selectionBuffer = ClientRestrictions.constrainSelection(selectionBuffer);
      if (buffer != selectionBuffer) {
         buffer.close();
         buffer = selectionBuffer;
      }
   }

   public static void setBufferWithHistory(SelectionBuffer selectionBuffer) {
      if (selectionBuffer.isEmpty()) {
         clearSelection();
      } else {
         BlockPos oldCenter = buffer.center();
         int oldCount = buffer.size();
         SelectionHistoryElement oldElement = buffer.createHistoryElement();
         setBuffer(selectionBuffer);
         buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
      }
   }

   public static void set(ChunkedBooleanRegion booleanRegion) {
      setBufferWithHistory(new SelectionBuffer.Set(booleanRegion));
   }

   public static void modify(UnaryOperator<ChunkedBooleanRegion> consumer) {
      setBuffer(buffer.modify(consumer, true));
   }

   public static void modifyWithHistory(UnaryOperator<ChunkedBooleanRegion> consumer, boolean history) {
      setBuffer(buffer.modify(consumer, history));
   }

   public static void move(int x, int y, int z, boolean history) {
      setBuffer(buffer.move(x, y, z, history));
   }

   public static void addAABB(BlockPos min, BlockPos max) {
      setBuffer(buffer.addAABB(min, max, true));
   }

   public static void subtractAABB(BlockPos min, BlockPos max) {
      setBuffer(buffer.subtractAABB(min, max, true));
   }

   public static void intersectAABB(BlockPos min, BlockPos max) {
      setBuffer(buffer.intersectAABB(min, max, true));
   }

   public static void addAABBWithHistory(BlockPos min, BlockPos max, boolean history) {
      setBuffer(buffer.addAABB(min, max, history));
   }

   public static void addSet(PositionSet positionSet) {
      if (!positionSet.isEmpty()) {
         setBuffer(buffer.addSet(positionSet, true));
      }
   }

   public static void subtractSet(PositionSet positionSet) {
      if (!positionSet.isEmpty()) {
         setBuffer(buffer.subtractSet(positionSet, true));
      }
   }

   public static void intersectSet(PositionSet positionSet) {
      setBuffer(buffer.intersectSet(positionSet, true));
   }

   public static void clearSelection() {
      Dispatcher.clearActiveSelectionHistory();
      setBuffer(SelectionBuffer.EMPTY);
   }

   public static void clearSelectionNoHistory() {
      setBuffer(SelectionBuffer.EMPTY);
   }

   public static UserAction.ActionResult callAction(UserAction action, Object object) {
      if (!EditorUI.isActive()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         switch (action) {
            case COPY:
               callCopy(makeNextCopyIncludeAir || copyWithAir, copyWithEntities);
               makeNextCopyIncludeAir = false;
               return UserAction.ActionResult.USED_STOP;
            case DELETE:
               callDelete();
               return UserAction.ActionResult.USED_STOP;
            case CUT:
               callCut(copyWithAir, copyWithEntities);
               return UserAction.ActionResult.USED_STOP;
            case DUPLICATE:
               callDuplicate(copyWithAir, copyWithEntities);
               return UserAction.ActionResult.USED_STOP;
            case ENTER:
               clearSelection();
               return UserAction.ActionResult.USED_STOP;
            default:
               return UserAction.ActionResult.NOT_HANDLED;
         }
      }
   }

   private static void callDelete() {
      buffer.callDelete();
      clearSelection();
   }

   private static void performCopyLikeAction(boolean copyAir, boolean copyEntities, boolean cut, boolean putInClipboard, boolean startPlacement) {
      copyEntities &= ClientEvents.serverSupportsProtocol(SupportedProtocol.REQUEST_ENTITY);
      BlockPos center = buffer.center();
      if (center != null) {
         if (Placement.INSTANCE.isPlacing()) {
            Placement.INSTANCE.stopPlacement();
         }

         float preferredYaw = 135.0F;
         Entity cameraEntity = Minecraft.getInstance().cameraEntity;
         if (cameraEntity != null) {
            double dx = cameraEntity.getX() - center.getX();
            double dz = cameraEntity.getZ() - center.getZ();
            preferredYaw = (float)(-Math.toDegrees(Math.atan2(dx, dz)));
         }

         float preferredYawFinal = preferredYaw;
         List<UUID> requestedEntities = new ArrayList<>();
         if (copyEntities) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
               for (Entity entity : level.entitiesForRendering()) {
                  if (entity != null
                     && !entity.isRemoved()
                     && entity.getType().canSerialize()
                     && buffer.contains(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ())) {
                     requestedEntities.add(entity.getUUID());
                  }
               }
            }
         }

         buffer.callCopy(cut, copyAir).thenAccept(copyResult -> {
            if (copyResult != null && !copyResult.chunkedBlockRegion().isEmpty()) {
               if (requestedEntities.isEmpty()) {
                  finalizeCopyLikeAction(copyResult, List.of(), copyAir, putInClipboard, startPlacement, preferredYawFinal, center);
               } else {
                  Dispatcher.requestEntityData(requestedEntities, entityData -> {
                     List<CompoundTag> entities = new ArrayList<>(entityData.values());

                     for (CompoundTag entityx : entities) {
                        EntityDataUtils.offsetEntityRecursive(entityx, Vec3.atLowerCornerOf(copyResult.realOffset()));
                     }

                     finalizeCopyLikeAction(copyResult, entities, copyAir, putInClipboard, startPlacement, preferredYawFinal, center);
                  });
               }
            }
         });
      }

      clearSelection();
   }

   private static void finalizeCopyLikeAction(
      SelectionBuffer.CopyResult copyResult,
      List<CompoundTag> entities,
      boolean includesAir,
      boolean putInClipboard,
      boolean startPlacement,
      float preferredYaw,
      BlockPos center
   ) {
      if (putInClipboard) {
         Clipboard.INSTANCE.setClipboard(copyResult.chunkedBlockRegion(), copyResult.blockEntities(), entities, "", preferredYaw, includesAir);
      }

      if (startPlacement) {
         ChunkedBlockRegion blockBuffer = copyResult.chunkedBlockRegion();
         BlockPos newCenter = center.offset(
            (blockBuffer.max().getX() + blockBuffer.min().getX()) / 2,
            (blockBuffer.max().getY() + blockBuffer.min().getY()) / 2,
            (blockBuffer.max().getZ() + blockBuffer.min().getZ()) / 2
         );
         String formattedCount = NumberFormat.getInstance().format((long)blockBuffer.count());
         String description = AxiomI18n.get("axiom.history_description.placed", formattedCount);
         Placement.INSTANCE.startPlacement(newCenter, blockBuffer, copyResult.blockEntities(), entities, includesAir, description);
      }
   }

   private static void callCut(boolean copyAir, boolean copyEntities) {
      performCopyLikeAction(copyAir, copyEntities, true, Axiom.configuration.internal.cutAlsoCopiesToClipboard, true);
   }

   public static void callCopy(boolean copyAir, boolean copyEntities) {
      performCopyLikeAction(copyAir, copyEntities, false, true, false);
   }

   private static void callDuplicate(boolean copyAir, boolean copyEntities) {
      performCopyLikeAction(copyAir, copyEntities, false, false, true);
   }
}
