package com.moulberry.axiom.tools.roughen;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
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
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class RoughenTool implements Tool {
   private final ChunkedBooleanRegion surfaceBlockHighlight = new ChunkedBooleanRegion();
   private AsyncToolPathProvider pathProvider = null;
   private boolean usingTool = false;
   private final BrushWidget brushWidget = new BrushWidget();
   private final int[] minFaces = new int[]{1};
   private final float[] ratio = new float[]{0.2F};
   private boolean addBlocks = true;
   private boolean removeBlocks = true;
   private final LongList surfaceBlocks = new LongArrayList();
   private final PresetWidget presetWidget = new PresetWidget(this, "roughen");
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();

   @Override
   public void reset() {
      this.usingTool = false;
      this.surfaceBlocks.clear();
      this.surfaceBlockHighlight.clear();
      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            } else {
               this.reset();
               if (!this.addBlocks && !this.removeBlocks) {
                  return UserAction.ActionResult.USED_STOP;
               }

               this.usingTool = true;
               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               MaskElement maskElement = MaskManager.getSourceMask();
               MaskContext maskContext = new MaskContext(level);
               this.pathProvider = new AsyncToolPathProvider(new AsyncToolPatherUnique(this.brushWidget.getBrushShape(), (x, y, z) -> {
                  if (isOnSurface(x, y, z, mutableBlockPos, level) && maskElement.test(maskContext.reset(), x, y, z)) {
                     this.surfaceBlocks.add(BlockPos.asLong(x, y, z));
                     this.surfaceBlockHighlight.add(x, y, z);
                  }
               }));
               return UserAction.ActionResult.USED_STOP;
            }
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
      if (!this.usingTool) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            Selection.render(rc, 7);
            return;
         }

         Selection.render(rc, 4);
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            this.reset();
            return;
         }

         this.pathProvider.finish();
         Selection.render(rc, 4);
         LongList airBlocks = new LongArrayList();
         PositionSet airBlockSet = new PositionSet();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         int surfaceBlocksSize = this.surfaceBlocks.size();

         for (int i = 0; i < surfaceBlocksSize; i++) {
            long position = this.surfaceBlocks.getLong(i);
            int x = BlockPos.getX(position);
            int y = BlockPos.getY(position);
            int z = BlockPos.getZ(position);

            for (Direction direction : Direction.values()) {
               mutableBlockPos.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
               BlockState blockState = level.getBlockState(mutableBlockPos);
               if (!blockState.blocksMotion() && airBlockSet.add(mutableBlockPos.getX(), mutableBlockPos.getY(), mutableBlockPos.getZ())) {
                  airBlocks.add(mutableBlockPos.asLong());
               }
            }
         }

         int totalSolidBlocks = this.surfaceBlocks.size();
         int totalAirBlocks = airBlocks.size();
         float solidRatio = 1.0F - this.ratio[0] * (this.addBlocks ? 0.5F : 1.0F);
         float airRatio = 1.0F - this.ratio[0] * (this.removeBlocks ? 0.5F : 1.0F);
         int solidBlocksDesiredCount = (int)(totalSolidBlocks * solidRatio);
         int airBlocksDesiredCount = (int)(totalAirBlocks * airRatio);
         ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
         int minFaces = Math.max(0, this.minFaces[0] - 1);
         Random random = ThreadLocalRandom.current();

         boolean finished;
         do {
            finished = true;
            if (this.removeBlocks && solidBlocksDesiredCount < this.surfaceBlocks.size()) {
               finished = false;
               int size = this.surfaceBlocks.size();
               int index = random.nextInt(size);
               long position;
               if (index < size - 1) {
                  long tail = this.surfaceBlocks.removeLong(size - 1);
                  position = this.surfaceBlocks.set(index, tail);
               } else {
                  position = this.surfaceBlocks.removeLong(size - 1);
               }

               int x = BlockPos.getX(position);
               int y = BlockPos.getY(position);
               int z = BlockPos.getZ(position);
               int airNeighborCount = this.airNeighborCount(x, y, z, blockRegion, mutableBlockPos, level);
               if (airNeighborCount <= minFaces) {
                  solidBlocksDesiredCount = (int)(--totalSolidBlocks * solidRatio);
               } else if (airNeighborCount * airNeighborCount <= random.nextInt(36)) {
                  this.surfaceBlocks.add(position);
               } else {
                  blockRegion.addBlock(x, y, z, AIR);
               }
            }

            if (this.addBlocks && airBlocksDesiredCount < airBlocks.size()) {
               finished = false;
               int sizex = airBlocks.size();
               int indexx = random.nextInt(sizex);
               long positionx;
               if (indexx < sizex - 1) {
                  long tail = airBlocks.removeLong(sizex - 1);
                  positionx = airBlocks.set(indexx, tail);
               } else {
                  positionx = airBlocks.removeLong(sizex - 1);
               }

               int x = BlockPos.getX(positionx);
               int y = BlockPos.getY(positionx);
               int z = BlockPos.getZ(positionx);
               int solidNeighborCount = this.solidNeighborCount(x, y, z, blockRegion, mutableBlockPos, level);
               if (solidNeighborCount <= minFaces) {
                  airBlocksDesiredCount = (int)(--totalAirBlocks * airRatio);
               } else if (solidNeighborCount * solidNeighborCount <= random.nextInt(36)) {
                  airBlocks.add(positionx);
               } else {
                  List<BlockState> possibleBlocks = new ArrayList<>();

                  for (Direction directionx : Direction.values()) {
                     mutableBlockPos.set(x + directionx.getStepX(), y + directionx.getStepY(), z + directionx.getStepZ());
                     BlockState blockState = blockRegion.getBlockStateOrDelegate(mutableBlockPos, level);
                     if (blockState.blocksMotion()) {
                        possibleBlocks.add(blockState);
                     }
                  }

                  if (BuildConfig.DEBUG && possibleBlocks.size() != solidNeighborCount) {
                     throw new FaultyImplementationError();
                  }

                  BlockState blockState = possibleBlocks.get(random.nextInt(possibleBlocks.size()));
                  blockRegion.addBlock(x, y, z, blockState);
               }
            }
         } while (!finished);

         String countString = NumberFormat.getInstance().format((long)blockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.roughen_tool", countString);
         RegionHelper.pushBlockRegionChange(blockRegion, historyDescription);
         this.reset();
      } else {
         ClientLevel levelx = Minecraft.getInstance().level;
         if (levelx == null) {
            return;
         }

         this.pathProvider.update();
         this.surfaceBlockHighlight.render(rc, Vec3.ZERO, 3);
      }
   }

   private static boolean isOnSurface(int x, int y, int z, MutableBlockPos mutableBlockPos, ClientLevel level) {
      mutableBlockPos.set(x, y, z);
      BlockState blockState = level.getBlockState(mutableBlockPos);
      if (!blockState.blocksMotion()) {
         return false;
      } else {
         for (Direction direction : Direction.values()) {
            mutableBlockPos.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
            blockState = level.getBlockState(mutableBlockPos);
            if (!blockState.blocksMotion()) {
               return true;
            }
         }

         return false;
      }
   }

   private int airNeighborCount(int x, int y, int z, ChunkedBlockRegion blockRegion, MutableBlockPos mutableBlockPos, ClientLevel level) {
      int adjacent = 0;

      for (Direction direction : Direction.values()) {
         mutableBlockPos.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
         BlockState blockState = blockRegion.getBlockStateOrDelegate(mutableBlockPos, level);
         if (!blockState.blocksMotion()) {
            adjacent++;
         }
      }

      return adjacent;
   }

   private int solidNeighborCount(int x, int y, int z, ChunkedBlockRegion blockRegion, MutableBlockPos mutableBlockPos, ClientLevel level) {
      int adjacent = 0;

      for (Direction direction : Direction.values()) {
         mutableBlockPos.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
         BlockState blockState = blockRegion.getBlockStateOrDelegate(mutableBlockPos, level);
         if (blockState.blocksMotion()) {
            adjacent++;
         }
      }

      return adjacent;
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = this.brushWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.roughen"));
      changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.roughen.ratio"), this.ratio, 0.0F, 1.0F);
      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.roughen.min_faces"), this.minFaces, 1, 4);
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.roughen.mode_add"), this.addBlocks)) {
         this.addBlocks = !this.addBlocks;
         changed = true;
      }

      ImGui.sameLine();
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.roughen.mode_remove"), this.removeBlocks)) {
         this.removeBlocks = !this.removeBlocks;
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(changed);
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
      return AxiomI18n.get("axiom.tool.roughen");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putInt("MinFaces", this.minFaces[0]);
      tag.putFloat("Ratio", this.ratio[0]);
      tag.putBoolean("AddBlocks", this.addBlocks);
      tag.putBoolean("RemoveBlocks", this.removeBlocks);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.minFaces[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "MinFaces", 1);
      this.ratio[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Ratio", 0.2F);
      this.addBlocks = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "AddBlocks", true);
      this.removeBlocks = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "RemoveBlocks", true);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue916';
   }

   @Override
   public String keybindId() {
      return "roughen";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_ROUGHEN, AxiomPermission.BUILD_SECTION);
   }
}
