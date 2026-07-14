package com.moulberry.axiom.tools.fluidball;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2ByteMap;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.text.NumberFormat;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class FluidBall implements Tool {
   private final ChunkedBooleanRegion chunkedBooleanRegion = new ChunkedBooleanRegion();
   private boolean usingTool = false;
   private AsyncToolPathProvider pathProvider = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private final int[] quality = new int[]{10};
   private final int[] flowLength = new int[]{8};
   private boolean removeFluidsWithoutSource = true;
   private boolean fillEdges = true;
   private final int[] fluidType = new int[]{0};
   private boolean infiniteSources = true;
   private boolean requireSupportToFlow = false;
   private FluidBall.FluidSimulation currentJob = null;

   @Override
   public void reset() {
      this.usingTool = false;
      this.currentJob = null;
      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }

      this.chunkedBooleanRegion.clear();
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.reset();
            this.usingTool = true;
            BrushShape brushShape = this.brushWidget.getBrushShape();
            MaskElement maskElement = MaskManager.getDestMask();
            MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
            this.pathProvider = new AsyncToolPathProvider(new AsyncToolPatherUnique(brushShape, (x, y, z) -> {
               maskContext.reset();
               BlockState blockState = maskContext.getBlockState(x, y, z);
               if (blockState.canBeReplaced() && maskElement.test(maskContext, x, y, z)) {
                  this.chunkedBooleanRegion.add(x, y, z);
               }
            }));
            this.pathProvider.includeFluids = true;
            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
            if (this.usingTool) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (this.currentJob != null) {
         this.runFluidSimulationJob(250L, false);
      } else {
         if (!this.usingTool) {
            RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
            if (result == null) {
               Selection.render(rc, 7);
               return;
            }

            Selection.render(rc, 4);
            this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 1);
         } else if (Tool.cancelUsing()) {
            this.reset();
         } else if (!Tool.isMouseDown(1)) {
            this.pathProvider.finish();
            FluidBall.FluidSimulation job = this.createFluidSimulationJob();
            this.reset();
            this.currentJob = job;
            this.runFluidSimulationJob(1000L, false);
         } else {
            Selection.render(rc, 4);
            this.pathProvider.update();
            this.chunkedBooleanRegion.render(rc, Vec3.ZERO, 1);
         }
      }
   }

   private Position2FloatMap createInitialLevelMap() {
      Position2FloatMap levelMap = new Position2FloatMap();
      this.chunkedBooleanRegion.forEach((x, y, z) -> levelMap.put(x, y, z, 8.0F));
      return levelMap;
   }

   private FluidBall.FluidSimulation createFluidSimulationJob() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         PositionSet checkedExistingWater = new PositionSet();
         Position2ByteMap worldStateMap = createWorldStateMap(level, this.fluidType[0]);
         this.chunkedBooleanRegion.forEach((x, y, z) -> {
            checkedExistingWater.add(x, y, z);
            worldStateMap.put(x, y, z, (byte)0);
         });
         float minDelta = 8.0F / Math.max(1, Math.min(32, this.flowLength[0]));
         return new FluidBall.FluidSimulation(
            this.createInitialLevelMap(), checkedExistingWater, worldStateMap, minDelta, this.infiniteSources, this.requireSupportToFlow
         );
      }
   }

   private void runFluidSimulationJob(long millis, boolean forceFinish) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         this.currentJob = null;
      } else {
         int maxIterations = this.quality[0] * 1000;
         this.currentJob.runUntil(maxIterations, millis);
         if (this.currentJob.iterations >= maxIterations || this.currentJob.isFinished() || forceFinish) {
            Position2FloatMap levelMap = this.currentJob.levelMap;
            Position2ByteMap worldStateMap = this.currentJob.worldStateMap;
            PositionSet checkedExistingWater = this.currentJob.checkedExistingWater;
            float minDelta = this.currentJob.minDelta;
            BooleanWrapper booleanWrapper = new BooleanWrapper(true);

            while (booleanWrapper.value) {
               booleanWrapper.value = false;
               Position2FloatMap newLevelMap = new Position2FloatMap();
               Position2FloatMap levelMapF = levelMap;
               levelMap.forEachEntry((xx, yx, zx, f) -> {
                  byte belowState = worldStateMap.getOrCreate(xx, yx - 1, zx);
                  if (belowState >= 0) {
                     float belowLevel;
                     if (belowState >= 1 && checkedExistingWater.add(xx, yx - 1, zx)) {
                        belowLevel = newLevelMap.add(xx, yx - 1, zx, belowState);
                        booleanWrapper.value = true;
                     } else {
                        belowLevel = levelMapF.get(xx, yx - 1, zx);
                     }

                     if (belowLevel < 8.0F) {
                        float transferred = Math.min(f, 8.0F - belowLevel);
                        newLevelMap.add(xx, yx - 1, zx, transferred);
                        f -= transferred;
                        booleanWrapper.value = true;
                     }
                  }

                  if (f > 8.0F) {
                     newLevelMap.add(xx, yx, zx, 8.0F);
                     newLevelMap.add(xx, yx + 1, zx, f - 8.0F);
                     booleanWrapper.value = true;
                  } else {
                     newLevelMap.add(xx, yx, zx, f);
                  }
               });
               levelMap = newLevelMap;
            }

            if (this.removeFluidsWithoutSource) {
               LongSet check = new LongOpenHashSet();
               Position2FloatMap newLevelMap = new Position2FloatMap();
               levelMap.forEachEntry((xx, yx, zx, f) -> {
                  if (Math.round(f) >= 8) {
                     newLevelMap.put(xx, yx, zx, 8.0F);
                     check.add(BlockPos.asLong(xx, yx, zx));
                  }
               });
               LongSet currentCheck = check;

               while (!currentCheck.isEmpty()) {
                  LongSet newCheck = new LongOpenHashSet();
                  LongIterator longIterator = currentCheck.longIterator();

                  while (longIterator.hasNext()) {
                     long pos = longIterator.nextLong();
                     int x = BlockPos.getX(pos);
                     int y = BlockPos.getY(pos);
                     int z = BlockPos.getZ(pos);
                     float threshold = newLevelMap.get(x, y, z);
                     if (Math.round(threshold) > 0) {
                        float plusX = levelMap.get(x + 1, y, z);
                        if (Math.round(plusX) > 0 && newLevelMap.max(x + 1, y, z, Math.min(plusX, threshold - minDelta))) {
                           newCheck.add(BlockPos.asLong(x + 1, y, z));
                        }

                        float minusX = levelMap.get(x - 1, y, z);
                        if (Math.round(minusX) > 0 && newLevelMap.max(x - 1, y, z, Math.min(minusX, threshold - minDelta))) {
                           newCheck.add(BlockPos.asLong(x - 1, y, z));
                        }

                        float plusZ = levelMap.get(x, y, z + 1);
                        if (Math.round(plusZ) > 0 && newLevelMap.max(x, y, z + 1, Math.min(plusZ, threshold - minDelta))) {
                           newCheck.add(BlockPos.asLong(x, y, z + 1));
                        }

                        float minusZ = levelMap.get(x, y, z - 1);
                        if (Math.round(minusZ) > 0 && newLevelMap.max(x, y, z - 1, Math.min(minusZ, threshold - minDelta))) {
                           newCheck.add(BlockPos.asLong(x, y, z - 1));
                        }
                     }
                  }

                  currentCheck = newCheck;
               }

               levelMap = newLevelMap;
            }

            if (this.fillEdges) {
               Position2FloatMap newLevelMap = new Position2FloatMap();
               Position2FloatMap oldLevelMap = levelMap;
               int[] offsets = new int[]{1, 0, -1, 0, 0, 1, 0, -1};
               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               levelMap.forEachEntry(
                  (xx, yx, zx, f) -> {
                     newLevelMap.max(xx, yx, zx, f);
                     if (Math.round(f) > 0) {
                        BlockState below = level.getBlockState(mutableBlockPos.set(xx, yx - 1, zx));
                        if (!below.canBeReplaced()) {
                           for (int offsetIndex = 0; offsetIndex < offsets.length; offsetIndex += 2) {
                              int offsetX = offsets[offsetIndex];
                              int offsetZ = offsets[offsetIndex + 1];
                              if (!(oldLevelMap.get(xx + offsetX, yx, zx + offsetZ) > 0.5F)
                                 && level.getBlockState(mutableBlockPos.set(xx + offsetX, yx, zx + offsetZ)).canBeReplaced()) {
                                 BlockState down = level.getBlockState(mutableBlockPos.set(xx + offsetX, yx - 1, zx + offsetZ));
                                 if (down.canBeReplaced() && !(newLevelMap.get(xx + offsetX, yx, zx + offsetZ) > 1.0F)) {
                                    boolean foundFluid = false;

                                    int ny;
                                    for (ny = yx - 1;
                                       down.canBeReplaced() && down.getBlock() != Blocks.VOID_AIR;
                                       down = level.getBlockState(mutableBlockPos.set(xx + offsetX, --ny, zx + offsetZ))
                                    ) {
                                       if (oldLevelMap.get(xx + offsetX, ny, zx + offsetZ) > 0.5F) {
                                          foundFluid = true;
                                          break;
                                       }
                                    }

                                    if (foundFluid) {
                                       newLevelMap.put(xx + offsetX, yx, zx + offsetZ, 1.0F);

                                       for (int yo = ny; yo < yx; yo++) {
                                          newLevelMap.put(xx + offsetX, yo, zx + offsetZ, 8.0F);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               );
               levelMap = newLevelMap;
            }

            ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            BlockState fluid;
            IntegerProperty property;
            if (this.fluidType[0] == 1) {
               fluid = Blocks.LAVA.defaultBlockState();
               property = BlockStateProperties.LEVEL;
            } else if (this.fluidType[0] == 2) {
               fluid = (BlockState)Blocks.SNOW.defaultBlockState().setValue(BlockStateProperties.LAYERS, 8);
               property = BlockStateProperties.LAYERS;
            } else {
               fluid = Blocks.WATER.defaultBlockState();
               property = BlockStateProperties.LEVEL;
            }

            Position2FloatMap finalLevelMap = levelMap;
            checkedExistingWater.forEach((xx, yx, zx) -> {
               BlockState existing = level.getBlockState(mutableBlockPos.set(xx, yx, zx));
               if (existing.getBlock() != Blocks.VOID_AIR) {
                  if (!(finalLevelMap.get(xx, yx, zx) > 0.0F)) {
                     if (this.fluidType[0] == 0 && existing.hasProperty(BlockStateProperties.WATERLOGGED)) {
                        chunkedBlockRegion.addBlockWithoutDirty(xx, yx, zx, (BlockState)existing.setValue(BlockStateProperties.WATERLOGGED, false));
                     } else {
                        chunkedBlockRegion.addBlockWithoutDirty(xx, yx, zx, Blocks.AIR.defaultBlockState());
                     }
                  }
               }
            });
            levelMap.forEachEntry((xx, yx, zx, f) -> {
               BlockState existing = level.getBlockState(mutableBlockPos.set(xx, yx, zx));
               if (existing.getBlock() != Blocks.VOID_AIR) {
                  int roundedLevel = Math.round(f);
                  if (this.fluidType[0] == 0 && existing.getBlock() != fluid.getBlock()) {
                     if (existing.hasProperty(BlockStateProperties.WATERLOGGED)) {
                        chunkedBlockRegion.addBlockWithoutDirty(xx, yx, zx, (BlockState)existing.setValue(BlockStateProperties.WATERLOGGED, roundedLevel > 0));
                        return;
                     }

                     if (roundedLevel > 0) {
                        FluidState fluidState = existing.getFluidState();
                        if (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER)) {
                           return;
                        }
                     }
                  }

                  if (roundedLevel >= 8) {
                     chunkedBlockRegion.addBlockWithoutDirty(xx, yx, zx, fluid);
                  } else if (roundedLevel > 0) {
                     BlockState blockState;
                     if (property == BlockStateProperties.LEVEL) {
                        blockState = (BlockState)fluid.setValue(BlockStateProperties.LEVEL, 8 - roundedLevel);
                     } else {
                        blockState = (BlockState)fluid.setValue(property, roundedLevel);
                     }

                     chunkedBlockRegion.addBlockWithoutDirty(xx, yx, zx, blockState);
                  } else {
                     chunkedBlockRegion.addBlockWithoutDirty(xx, yx, zx, Blocks.AIR.defaultBlockState());
                  }
               }
            });
            String countString = NumberFormat.getInstance().format((long)chunkedBlockRegion.count());
            String historyDescription = AxiomI18n.get("axiom.history_description.drew", countString);
            RegionHelper.pushBlockRegionChange(chunkedBlockRegion, historyDescription);
            this.currentJob = null;
            this.reset();
         }
      }
   }

   private static float getSpreadAmount(byte neighborState, int x, int y, int z, float f, Position2FloatMap oldLevelMap) {
      if (neighborState >= 0 && neighborState < 8) {
         if (neighborState >= 1 && f - neighborState <= 0.0) {
            return 0.0F;
         } else {
            float neighborLevel = oldLevelMap.get(x, y, z);
            return Math.max(0.0F, f - neighborLevel);
         }
      } else {
         return 0.0F;
      }
   }

   private static byte getSpreadCount(
      Position2ByteMap worldStateMap, Position2ByteMap spreadFromMap, int x, int y, int z, float minDelta, Position2FloatMap oldLevelMap
   ) {
      byte spreadFrom = spreadFromMap.get(x, y, z);
      if (spreadFrom == -1) {
         float centerLevel = oldLevelMap.get(x, y, z);
         byte centerState = worldStateMap.getOrCreate(x, y, z);
         if (centerState < 0 || centerState >= 8) {
            spreadFromMap.put(x, y, z, (byte)0);
            return 0;
         }

         spreadFrom = 4;
         float plusX = oldLevelMap.get(x + 1, y, z);
         if (plusX - minDelta - centerLevel < 0.05) {
            spreadFrom--;
         }

         float minusX = oldLevelMap.get(x - 1, y, z);
         if (minusX - minDelta - centerLevel < 0.05) {
            spreadFrom--;
         }

         float plusZ = oldLevelMap.get(x, y, z + 1);
         if (plusZ - minDelta - centerLevel < 0.05) {
            spreadFrom--;
         }

         float minusZ = oldLevelMap.get(x, y, z - 1);
         if (minusZ - minDelta - centerLevel < 0.05) {
            spreadFrom--;
         }

         spreadFromMap.put(x, y, z, spreadFrom);
      }

      return spreadFrom;
   }

   @NotNull
   private static Position2ByteMap createWorldStateMap(ClientLevel level, int fluidType) {
      MutableBlockPos mutableBlockPosForWorldState = new MutableBlockPos();
      return new Position2ByteMap((byte)-1, k -> {
         byte[] values = new byte[4096];
         int cx = BlockPos.getX(k) * 16;
         int cy = BlockPos.getY(k) * 16;
         int cz = BlockPos.getZ(k) * 16;

         for (int index = 0; index < 4096; index++) {
            int x = cx + (index & 15);
            int y = cy + (index >> 4 & 15);
            int z = cz + (index >> 8 & 15);
            BlockState blockState = level.getBlockState(mutableBlockPosForWorldState.set(x, y, z));
            FluidState fluidState = blockState.getFluidState();
            if (fluidType == 1) {
               if (fluidState.is(Fluids.LAVA) || fluidState.is(Fluids.FLOWING_LAVA)) {
                  values[index] = (byte)fluidState.getAmount();
                  continue;
               }
            } else if (fluidType == 2) {
               if (blockState.getBlock() == Blocks.SNOW_BLOCK) {
                  values[index] = 8;
                  continue;
               }

               if (blockState.getBlock() == Blocks.SNOW) {
                  values[index] = (byte)((Integer)blockState.getValue(BlockStateProperties.LAYERS)).intValue();
                  continue;
               }
            } else {
               if (fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER)) {
                  values[index] = (byte)fluidState.getAmount();
                  continue;
               }

               if (blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                  if ((Boolean)blockState.getValue(BlockStateProperties.WATERLOGGED)) {
                     values[index] = 8;
                  } else {
                     values[index] = 0;
                  }
                  continue;
               }
            }

            if (!fluidState.isEmpty()) {
               values[index] = -1;
            } else if (blockState.canBeReplaced() && blockState.getBlock() != Blocks.VOID_AIR) {
               values[index] = 0;
            } else {
               values[index] = -1;
            }
         }

         return values;
      });
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      this.brushWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.fluid_ball"));
      ImGuiHelper.combo("Fluid Type", this.fluidType, new String[]{"Water", "Lava", "Snow"});
      ImGui.sliderInt("Quality", this.quality, 0, 100);
      ImGui.sliderInt("Flow Length", this.flowLength, 2, 16);
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.remove_fluids_without_sources"), this.removeFluidsWithoutSource)) {
         this.removeFluidsWithoutSource = !this.removeFluidsWithoutSource;
      }

      ImGuiHelper.tooltip("Removes extraneous fluids at the end of the operation if it isn't connected to a source block");
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.fill_edges"), this.fillEdges)) {
         this.fillEdges = !this.fillEdges;
      }

      ImGuiHelper.tooltip("Adds fluid around the edge of blocks to make it look like it's flowing over the edge");
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.infinite_sources"), this.infiniteSources)) {
         this.infiniteSources = !this.infiniteSources;
      }

      ImGuiHelper.tooltip("Turns the initial region into infinite sources, making them able to propagate water without being drained");
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.require_support_to_flow"), this.requireSupportToFlow)) {
         this.requireSupportToFlow = !this.requireSupportToFlow;
      }

      ImGuiHelper.tooltip("Only propagates water sideways if the block below is solid");
      if (this.currentJob != null) {
         if (!ImGui.isPopupOpen("Fluid Simulation")) {
            ImGui.openPopup("Fluid Simulation");
         }

         if (ImGuiHelper.beginPopupModal("Fluid Simulation", 64)) {
            int maxIterations = this.quality[0] * 1000;
            int percentage = this.currentJob.iterations * 100 / maxIterations;
            ImGui.text(AxiomI18n.get("axiom.hardcoded.progress") + percentage + "%");
            long currentTime = System.currentTimeMillis();
            if (currentTime < this.currentJob.startTime) {
               this.currentJob.startTime = 0L;
            }

            if (this.currentJob.startTime > 0L) {
               long duration = (currentTime - this.currentJob.startTime) / 1000L;
               String durationString = String.format("Elapsed Time: %02d:%02d", duration / 60L, duration % 60L);
               ImGui.text(durationString);
            }

            if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
               this.currentJob = null;
               this.reset();
            }

            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.hardcoded.finish_early"))) {
               this.runFluidSimulationJob(250L, true);
               this.reset();
            }

            ImGui.endPopup();
         }
      }
   }

   @Override
   public String listenForEsc() {
      return !this.usingTool ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public boolean initiateAdjustment() {
      return this.brushWidget.initiateAdjustment();
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      return this.brushWidget.renderAdjustment(mouseX, mouseY, mouseDelta);
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.fluid_ball");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue920';
   }

   @Override
   public String keybindId() {
      return "fluid_ball";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_FLUIDBALL, AxiomPermission.BUILD_SECTION);
   }

   private static class FluidSimulation {
      private float minDelta;
      private final boolean requireSupportToFlow;
      private long startTime = System.currentTimeMillis();
      private Position2FloatMap levelMap;
      private PositionSet shouldPropagate = null;
      private final PositionSet checkedExistingWater;
      private final PositionSet initialWater;
      private final Position2ByteMap worldStateMap;
      private int iterations = 0;

      public FluidSimulation(
         Position2FloatMap levelMap,
         PositionSet checkedExistingWater,
         Position2ByteMap worldStateMap,
         float minDelta,
         boolean infiniteSources,
         boolean requireSupportToFlow
      ) {
         this.levelMap = levelMap;
         this.checkedExistingWater = checkedExistingWater;
         this.initialWater = infiniteSources ? checkedExistingWater.copy() : new PositionSet();
         this.worldStateMap = worldStateMap;
         this.minDelta = minDelta;
         this.requireSupportToFlow = requireSupportToFlow;
      }

      private boolean isFinished() {
         return this.shouldPropagate != null && this.shouldPropagate.isEmpty();
      }

      private void runUntil(int maxIterations, long maxTime) {
         long start = System.currentTimeMillis();

         while (!this.isFinished() && System.currentTimeMillis() - start < maxTime && this.iterations < maxIterations) {
            this.iterations++;
            Position2FloatMap newLevelMap = new Position2FloatMap();
            Position2ByteMap spreadFromMap = new Position2ByteMap((byte)-1);
            Position2FloatMap oldLevelMap = this.levelMap;
            LongSet newlyUsedWater = new LongOpenHashSet();
            PositionSet newShouldPropagate = new PositionSet();
            LongList directCopy = new LongArrayList();
            LongSet propagatedChunks = this.shouldPropagate == null ? null : this.shouldPropagate.chunkKeySet();
            ObjectIterator directCopyIterator = this.levelMap.unsafeGetRawMap().long2ObjectEntrySet().iterator();

            while (directCopyIterator.hasNext()) {
               Entry<float[]> entry = (Entry<float[]>)directCopyIterator.next();
               if (propagatedChunks != null && !propagatedChunks.contains(entry.getLongKey())) {
                  directCopy.add(entry.getLongKey());
               } else {
                  int cx = BlockPos.getX(entry.getLongKey()) * 16;
                  int cy = BlockPos.getY(entry.getLongKey()) * 16;
                  int cz = BlockPos.getZ(entry.getLongKey()) * 16;
                  float[] array = (float[])entry.getValue();
                  int index = 0;

                  for (int zo = 0; zo < 16; zo++) {
                     int z = cz + zo;

                     for (int yo = 0; yo < 16; yo++) {
                        int y = cy + yo;
                        if (this.shouldPropagate != null && !this.shouldPropagate.containsInXRow(cx, y, z)) {
                           for (int xo = 0; xo < 16; xo++) {
                              float f = array[index++];
                              if (f > 0.0) {
                                 newLevelMap.add(cx + xo, y, z, f);
                              }
                           }
                        } else {
                           for (int xox = 0; xox < 16; xox++) {
                              float f = array[index++];
                              if (f != 0.0) {
                                 boolean propagatedThis = false;
                                 int x = cx + xox;
                                 if (this.shouldPropagate != null && !this.shouldPropagate.contains(x, y, z)) {
                                    newLevelMap.add(x, y, z, f);
                                 } else {
                                    byte belowState = this.worldStateMap.getOrCreate(x, y - 1, z);
                                    if (belowState >= 0) {
                                       if (belowState >= 1 && this.checkedExistingWater.add(x, y - 1, z)) {
                                          newlyUsedWater.add(BlockPos.asLong(x, y - 1, z));
                                          newLevelMap.add(x, y - 1, z, belowState);
                                       }

                                       float belowLevel = oldLevelMap.get(x, y - 1, z) + belowState;
                                       if (belowLevel < 8.0F) {
                                          float transferred = Math.min(f, 8.0F - belowLevel);
                                          newLevelMap.add(x, y - 1, z, transferred);
                                          f -= transferred;
                                          propagatedThis = true;
                                       }
                                    }

                                    byte aboveState = this.worldStateMap.getOrCreate(x, y + 1, z);
                                    if (aboveState >= 1 && this.checkedExistingWater.add(x, y + 1, z)) {
                                       newlyUsedWater.add(BlockPos.asLong(x, y + 1, z));
                                       newLevelMap.add(x, y + 1, z, aboveState);
                                    }

                                    if (Math.round(f) > 1 && (!this.requireSupportToFlow || belowState < 0 || this.initialWater.contains(x, y - 1, z))) {
                                       float currentLevel = f;
                                       float cappedCurrentLevel = Math.min(f, 8.0F) - this.minDelta;
                                       byte statePX = this.worldStateMap.getOrCreate(x + 1, y, z);
                                       byte stateNX = this.worldStateMap.getOrCreate(x - 1, y, z);
                                       byte statePZ = this.worldStateMap.getOrCreate(x, y, z + 1);
                                       byte stateNZ = this.worldStateMap.getOrCreate(x, y, z - 1);
                                       float spreadPX = FluidBall.getSpreadAmount(statePX, x + 1, y, z, cappedCurrentLevel, oldLevelMap);
                                       float spreadNX = FluidBall.getSpreadAmount(stateNX, x - 1, y, z, cappedCurrentLevel, oldLevelMap);
                                       float spreadPZ = FluidBall.getSpreadAmount(statePZ, x, y, z + 1, cappedCurrentLevel, oldLevelMap);
                                       float spreadNZ = FluidBall.getSpreadAmount(stateNZ, x, y, z - 1, cappedCurrentLevel, oldLevelMap);
                                       int spreadableNeighbors = 0;
                                       if (spreadPX > 0.05) {
                                          spreadableNeighbors++;
                                       }

                                       if (spreadNX > 0.05) {
                                          spreadableNeighbors++;
                                       }

                                       if (spreadPZ > 0.05) {
                                          spreadableNeighbors++;
                                       }

                                       if (spreadNZ > 0.05) {
                                          spreadableNeighbors++;
                                       }

                                       if (spreadableNeighbors > 0) {
                                          if (spreadPX > 0.05) {
                                             if (statePX > 0) {
                                                if (this.checkedExistingWater.add(x + 1, y, z)) {
                                                   newlyUsedWater.add(BlockPos.asLong(x + 1, y, z));
                                                   newLevelMap.add(x + 1, y, z, statePX);
                                                }
                                             } else {
                                                byte spreadableFrom = FluidBall.getSpreadCount(
                                                   this.worldStateMap, spreadFromMap, x + 1, y, z, this.minDelta, oldLevelMap
                                                );
                                                float transferred = spreadPX / (spreadableNeighbors + spreadableFrom);
                                                newLevelMap.add(x + 1, y, z, transferred);
                                                currentLevel = f - transferred;
                                             }
                                          }

                                          if (spreadNX > 0.05) {
                                             if (stateNX > 0) {
                                                if (this.checkedExistingWater.add(x - 1, y, z)) {
                                                   newlyUsedWater.add(BlockPos.asLong(x - 1, y, z));
                                                   newLevelMap.add(x - 1, y, z, stateNX);
                                                }
                                             } else {
                                                byte spreadableFrom = FluidBall.getSpreadCount(
                                                   this.worldStateMap, spreadFromMap, x - 1, y, z, this.minDelta, oldLevelMap
                                                );
                                                float transferred = spreadNX / (spreadableNeighbors + spreadableFrom);
                                                newLevelMap.add(x - 1, y, z, transferred);
                                                currentLevel -= transferred;
                                             }
                                          }

                                          if (spreadPZ > 0.05) {
                                             if (statePZ > 0) {
                                                if (this.checkedExistingWater.add(x, y, z + 1)) {
                                                   newlyUsedWater.add(BlockPos.asLong(x, y, z + 1));
                                                   newLevelMap.add(x, y, z + 1, statePZ);
                                                }
                                             } else {
                                                byte spreadableFrom = FluidBall.getSpreadCount(
                                                   this.worldStateMap, spreadFromMap, x, y, z + 1, this.minDelta, oldLevelMap
                                                );
                                                float transferred = spreadPZ / (spreadableNeighbors + spreadableFrom);
                                                newLevelMap.add(x, y, z + 1, transferred);
                                                currentLevel -= transferred;
                                             }
                                          }

                                          if (spreadNZ > 0.05) {
                                             if (stateNZ > 0) {
                                                if (this.checkedExistingWater.add(x, y, z - 1)) {
                                                   newlyUsedWater.add(BlockPos.asLong(x, y, z - 1));
                                                   newLevelMap.add(x, y, z - 1, stateNZ);
                                                }
                                             } else {
                                                byte spreadableFrom = FluidBall.getSpreadCount(
                                                   this.worldStateMap, spreadFromMap, x, y, z - 1, this.minDelta, oldLevelMap
                                                );
                                                float transferred = spreadNZ / (spreadableNeighbors + spreadableFrom);
                                                newLevelMap.add(x, y, z - 1, transferred);
                                                currentLevel -= transferred;
                                             }
                                          }

                                          propagatedThis = true;
                                       }

                                       if (currentLevel > 8.0F && aboveState >= 0) {
                                          newLevelMap.add(x, y, z, 8.0F);
                                          newLevelMap.add(x, y + 1, z, currentLevel - 8.0F);
                                          propagatedThis = true;
                                       } else {
                                          newLevelMap.add(x, y, z, this.initialWater.contains(x, y, z) ? 8.0F : Math.max(0.01F, currentLevel));
                                       }
                                    } else {
                                       newLevelMap.add(x, y, z, this.initialWater.contains(x, y, z) ? 8.0F : f);
                                    }

                                    if (propagatedThis) {
                                       newShouldPropagate.add(x, y, z);
                                       newShouldPropagate.add(x + 1, y, z);
                                       newShouldPropagate.add(x - 1, y, z);
                                       newShouldPropagate.add(x, y + 1, z);
                                       newShouldPropagate.add(x, y - 1, z);
                                       newShouldPropagate.add(x, y, z + 1);
                                       newShouldPropagate.add(x, y, z - 1);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            newlyUsedWater.forEach(value -> {
               int xx = BlockPos.getX(value);
               int yx = BlockPos.getY(value);
               int zx = BlockPos.getZ(value);
               this.worldStateMap.put(xx, yx, zx, (byte)0);
            });
            LongIterator directCopyIteratorx = directCopy.longIterator();

            while (directCopyIteratorx.hasNext()) {
               long pos = directCopyIteratorx.nextLong();
               float[] oldChunk = this.levelMap.getChunk(pos);
               float[] newChunk = newLevelMap.getChunk(pos);
               if (newChunk == null) {
                  newLevelMap.unsafeGetRawMap().put(pos, oldChunk);
               } else {
                  int length = oldChunk.length;

                  for (int index = 0; index < length; index++) {
                     newChunk[index] += oldChunk[index];
                  }
               }
            }

            this.levelMap = newLevelMap;
            this.shouldPropagate = newShouldPropagate;
         }
      }
   }
}
