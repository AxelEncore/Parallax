package com.moulberry.axiom.tools.floodfill;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.magic_select.MagicSelectionTask;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class FloodfillTool implements Tool {
   private ChunkedBooleanRegion region = new ChunkedBooleanRegion();
   private BlockPos lastFloodedBlockPos = null;
   private BlockPos justAppliedBlockPos = null;
   private final float[] limit = new float[]{100000.0F};
   private boolean propagateUp = false;
   private boolean propagateDown = true;
   private boolean propagateHorizontal = true;
   private boolean propagateCorners = false;
   private Future<FloodfillTask> livePreviewFuture = null;

   @Override
   public void reset() {
      this.lastFloodedBlockPos = null;
      this.region.clear();
      if (this.livePreviewFuture != null) {
         this.livePreviewFuture.cancel(true);
         this.livePreviewFuture = null;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.rightClick();
            return UserAction.ActionResult.USED_STOP;
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   public void rightClick() {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result != null) {
         BlockPos offset = result.getBlockPos().relative(result.getDirection());
         if (!offset.equals(this.justAppliedBlockPos)) {
            this.justAppliedBlockPos = offset;
            BlockState activeBlock = Tool.getActiveBlock();
            boolean isWater = activeBlock == Blocks.WATER.defaultBlockState();
            FloodfillTask task;
            if (isWater) {
               task = new FloodfillWaterTask(new PositionSet(), Minecraft.getInstance().level, offset, this.getPropagationFlags());
            } else {
               task = new FloodfillTask(new PositionSet(), Minecraft.getInstance().level, offset, this.getPropagationFlags());
            }

            task.fill((int)this.limit[0]);
            PositionSet positionSet = task.positionSet;
            if (positionSet.count() > 0) {
               ClientLevel world = Minecraft.getInstance().level;
               if (world != null) {
                  BlockBuffer setOperation = new BlockBuffer();
                  BlockBuffer previousBlocksForUndo = new BlockBuffer();
                  IntWrapper count = new IntWrapper();
                  positionSet.forEachChunk((cx, cy, cz, data) -> {
                     if (cy >= world.getMinSection() && cy <= world.getMaxSection() - 1) {
                        LevelChunk chunk = (LevelChunk)world.getChunk(cx, cz, ChunkStatus.FULL, false);
                        if (chunk != null) {
                           LevelChunkSection section = chunk.getSection(world.getSectionIndexFromSectionY(cy));
                           PalettedContainer<BlockState> container = section.getStates();
                           int wcx = cx * 16;
                           int wcy = cy * 16;
                           int wcz = cz * 16;
                           int index = 0;

                           for (int z = 0; z < 16; z++) {
                              for (int y = 0; y < 16; y++) {
                                 short line = data[index++];
                                 if (line != 0) {
                                    for (int x = 0; x < 16; x++) {
                                       if ((line & 1 << x) != 0) {
                                          BlockState oldBlock = (BlockState)container.get(x, y, z);
                                          if (oldBlock.getBlock() != Blocks.VOID_AIR && (!isWater || !oldBlock.getFluidState().is(Fluids.WATER))) {
                                             BlockState to;
                                             if (isWater && oldBlock.hasProperty(BlockStateProperties.WATERLOGGED)) {
                                                to = (BlockState)oldBlock.setValue(BlockStateProperties.WATERLOGGED, true);
                                             } else {
                                                to = activeBlock;
                                             }

                                             if (to != oldBlock) {
                                                count.value++;
                                                setOperation.set(wcx + x, wcy + y, wcz + z, to);
                                                previousBlocksForUndo.set(wcx + x, wcy + y, wcz + z, oldBlock);
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  });
                  if (count.value != 0) {
                     String countString = NumberFormat.getInstance().format((long)count.value);
                     String historyDescription = AxiomI18n.get("axiom.history_description.floodfilled", countString);
                     Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, offset, historyDescription, 0));
                  }
               }
            }
         }
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (Tool.getLookDirection() == null) {
         Selection.render(rc, 7);
      } else {
         Selection.render(rc, 4);
         if (this.livePreviewFuture != null && this.livePreviewFuture.isDone()) {
            try {
               FloodfillTask task = this.livePreviewFuture.get();
               this.region.clear();
               task.positionSet.forEach(this.region::add);
            } catch (ExecutionException | InterruptedException var5) {
               throw new RuntimeException(var5);
            }

            this.livePreviewFuture = null;
         }

         if (this.livePreviewFuture == null) {
            RayCaster.RaycastResult result = Tool.raycastBlock();
            if (result == null) {
               return;
            }

            BlockPos offset = result.getBlockPos().relative(result.getDirection());
            if (offset.equals(this.justAppliedBlockPos)) {
               return;
            }

            this.justAppliedBlockPos = null;
            if (!offset.equals(this.lastFloodedBlockPos)) {
               this.lastFloodedBlockPos = offset;
               FloodfillTask task;
               if (Tool.getActiveBlock() == Blocks.WATER.defaultBlockState()) {
                  task = new FloodfillWaterTask(new PositionSet(), Minecraft.getInstance().level, offset, this.getPropagationFlags());
               } else {
                  task = new FloodfillTask(new PositionSet(), Minecraft.getInstance().level, offset, this.getPropagationFlags());
               }

               this.livePreviewFuture = Tool.sharedPoolThreadExecutor.submit(() -> {
                  task.fill((int)this.limit[0]);
                  return task;
               });
            }
         }

         this.region.render(rc, Vec3.ZERO, 1);
      }
   }

   private int getPropagationFlags() {
      int flags = 0;
      if (this.propagateUp) {
         flags |= MagicSelectionTask.PROPAGATION_FLAG_UP;
      }

      if (this.propagateDown) {
         flags |= MagicSelectionTask.PROPAGATION_FLAG_DOWN;
      }

      if (this.propagateHorizontal) {
         flags |= MagicSelectionTask.PROPAGATION_FLAG_HORIZONTAL;
      }

      if (this.propagateCorners) {
         flags |= MagicSelectionTask.PROPAGATION_FLAG_CORNERS;
      }

      return flags;
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.floodfill"));
      ImGui.sliderFloat(AxiomI18n.get("axiom.tool.generic.limit"), this.limit, 2.0F, 1.0E7F, "%'.0f", 32);
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.up"), this.propagateUp)) {
         this.propagateUp = !this.propagateUp;
      }

      ImGui.sameLine();
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.down"), this.propagateDown)) {
         this.propagateDown = !this.propagateDown;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.horizontal"), this.propagateHorizontal)) {
         this.propagateHorizontal = !this.propagateHorizontal;
      }

      if (!this.propagateHorizontal) {
         this.propagateCorners = false;
      } else {
         ImGui.sameLine();
         if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.corners"), this.propagateCorners)) {
            this.propagateCorners = !this.propagateCorners;
         }
      }
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.floodfill");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putInt("Limit", (int)this.limit[0]);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.limit[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Limit", 100000);
   }

   @Override
   public char iconChar() {
      return '\ue919';
   }

   @Override
   public String keybindId() {
      return "floodfill";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_FLOODFILL, AxiomPermission.BUILD_SECTION);
   }
}
