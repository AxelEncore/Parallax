package com.moulberry.axiom.buildertools;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.HistoryEntry;
import java.text.NumberFormat;
import java.util.EnumSet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class SmearBuilderTool implements BuilderTool {
   private final BuilderToolSelectionState selectionState = new BuilderToolSelectionState();
   private SelectionBuffer selectionBuffer = null;
   private MutableBlockPos smearDirections = null;
   private final ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();

   @Override
   public void renderScreen(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      if (this.smearDirections != null) {
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.cancel"), Minecraft.getInstance().options.keyAttack, 0
         );
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.confirm"), Minecraft.getInstance().options.keyUse, 1
         );
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.smear.smear_to"), Minecraft.getInstance().options.keyPickItem, 2
         );
      } else {
         this.selectionState.renderScreen(guiGraphics, screenWidth, screenHeight);
      }
   }

   @Override
   public void handleInput(boolean nudgeForwards, boolean nudgeBackwards, boolean delete) {
      if (nudgeForwards) {
         this.nudge(1);
      }

      if (nudgeBackwards) {
         this.nudge(-1);
      }

      if (delete) {
         if (this.smearDirections == null) {
            this.selectionState.createSelectionBuffer().callDelete(HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME);
         }

         this.reset(false);
      }
   }

   @Override
   public void renderWorld(AxiomWorldRenderContext rc) {
      if (this.smearDirections != null) {
         this.blockRegion.render(rc, Vec3.ZERO, 0.8F, 0.0F);
      }

      this.selectionState.renderWorld(rc);
   }

   @Override
   public boolean setPos1(BlockPos position) {
      if (this.smearDirections == null) {
         this.selectionState.setPos1(position);
         this.showTextInActionBar();
      }

      return true;
   }

   @Override
   public boolean setPos2(BlockPos position) {
      if (this.smearDirections == null) {
         this.selectionState.setPos2(position);
         this.showTextInActionBar();
      }

      return true;
   }

   @Override
   public void leftClick(HitResult hitResult) {
      if (this.smearDirections != null) {
         this.reset(false);
      } else if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         this.selectionState.leftClick(blockHitResult);
         this.showTextInActionBar();
      }
   }

   @Override
   public void rightClick(HitResult hitResult) {
      if (this.smearDirections != null) {
         this.reset(true);
      } else if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         this.selectionState.rightClick(blockHitResult);
         this.showTextInActionBar();
      }
   }

   @Override
   public void middleClick(HitResult hitResult) {
      if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         if (this.smearDirections != null) {
            BlockPos min = this.selectionBuffer.min();
            BlockPos max = this.selectionBuffer.max();
            if (min == null || max == null) {
               return;
            }

            BlockPos target = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
            if (target.getX() < min.getX()) {
               this.smearDirections.setX(target.getX() - min.getX());
            } else if (target.getX() > max.getX()) {
               this.smearDirections.setX(target.getX() - max.getX());
            } else {
               this.smearDirections.setX(0);
            }

            if (target.getY() < min.getY()) {
               this.smearDirections.setY(target.getY() - min.getY());
            } else if (target.getY() > max.getY()) {
               this.smearDirections.setY(target.getY() - max.getY());
            } else {
               this.smearDirections.setY(0);
            }

            if (target.getZ() < min.getZ()) {
               this.smearDirections.setZ(target.getZ() - min.getZ());
            } else if (target.getZ() > max.getZ()) {
               this.smearDirections.setZ(target.getZ() - max.getZ());
            } else {
               this.smearDirections.setZ(0);
            }

            this.updateSmear();
         } else {
            this.selectionState.middleClick(blockHitResult);
         }

         this.showTextInActionBar();
      }
   }

   private void apply() {
      if (this.smearDirections != null && (this.smearDirections.getX() != 0 || this.smearDirections.getY() != 0 || this.smearDirections.getZ() != 0)) {
         String description = AxiomI18n.get("axiom.history_description.smeared", NumberFormat.getInstance().format((long)this.blockRegion.count()));
         int flags = HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME;
         if (BuilderToolManager.keepExisting) {
            flags |= HistoryEntry.MODIFIER_KEEP_EXISTING;
         }

         RegionHelper.pushBlockRegionChangeWithNBT(this.blockRegion, description, flags, null);
      }
   }

   @Override
   public boolean scroll(int scroll) {
      if (!this.selectionState.hasSelection()) {
         return false;
      } else {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player == null) {
            return false;
         } else {
            Direction dir = BuilderTool.calculateDirection();
            if (this.smearDirections == null) {
               if (ClientEvents.builderToolNudgeScrollKeyBind.isDown()) {
                  this.nudge(scroll);
                  return true;
               }

               this.smearDirections = new MutableBlockPos();
            }

            this.smearDirections.move(dir, scroll);
            this.showTextInActionBar();
            this.updateSmear();
            return true;
         }
      }
   }

   private void updateSmear() {
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         this.blockRegion.clear();
         if (this.selectionBuffer != null) {
            this.selectionBuffer.close();
         }

         this.selectionBuffer = this.selectionState.createSelectionBuffer();
         if (this.selectionBuffer.isEmpty()) {
            this.selectionBuffer.close();
            this.selectionBuffer = null;
         } else {
            int smearX = this.smearDirections.getX();
            int smearY = this.smearDirections.getY();
            int smearZ = this.smearDirections.getZ();
            int maxSmear = Math.max(Math.abs(smearX), Math.max(Math.abs(smearY), Math.abs(smearZ)));
            if (maxSmear != 0) {
               record BlockStateWithDistance(BlockState blockState, int distanceSq) {
               }

               Position2ObjectMap<BlockStateWithDistance> blockMap = new Position2ObjectMap<>(k -> new BlockStateWithDistance[4096]);
               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               this.selectionBuffer.forEach((x, y, z) -> {
                  BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                  if (BuilderToolManager.copyAir || !blockState.isAir()) {
                     for (int driver = 1; driver <= maxSmear; driver++) {
                        int nx = x + Math.round((float)(driver * smearX) / maxSmear);
                        int ny = y + Math.round((float)(driver * smearY) / maxSmear);
                        int nz = z + Math.round((float)(driver * smearZ) / maxSmear);
                        if (BuilderToolManager.copyAir) {
                           if (!blockState.isAir()) {
                              if (this.selectionBuffer.contains(nx, ny, nz)) {
                                 continue;
                              }

                              if (!level.getBlockState(mutableBlockPos.set(nx, ny, nz)).canBeReplaced()) {
                                 break;
                              }
                           }
                        } else if (!level.getBlockState(mutableBlockPos.set(nx, ny, nz)).canBeReplaced()) {
                           if (!this.selectionBuffer.contains(nx, ny, nz)) {
                              break;
                           }
                           continue;
                        }

                        BlockStateWithDistance bswd = blockMap.get(nx, ny, nz);
                        int distanceSq = (nx - x) * (nx - x) + (ny - y) * (ny - y) + (nz - z) * (nz - z);
                        if (bswd == null || distanceSq < bswd.distanceSq) {
                           blockMap.put(nx, ny, nz, new BlockStateWithDistance(blockState, distanceSq));
                        }
                     }
                  }
               });
               blockMap.forEachEntry((x, y, z, bswd) -> this.blockRegion.addBlock(x, y, z, bswd.blockState));
            }
         }
      }
   }

   private void nudge(int amount) {
      if (this.smearDirections == null) {
         this.selectionState.nudge(amount);
         this.showTextInActionBar();
      }
   }

   private void showTextInActionBar() {
      if (this.smearDirections != null) {
         ScreenRenderHook.setOverlayText(
            Component.literal(AxiomI18n.get("axiom.hardcoded.smear_offset_prefix"))
               .withStyle(ChatFormatting.YELLOW)
               .append(Component.literal(this.smearDirections.getX() + " ").withStyle(ChatFormatting.RED))
               .append(Component.literal(this.smearDirections.getY() + " ").withStyle(ChatFormatting.GREEN))
               .append(Component.literal(this.smearDirections.getZ() + "").withStyle(ChatFormatting.AQUA))
         );
      } else {
         this.selectionState.showTextInActionBar();
      }
   }

   @Override
   public boolean shouldRenderBlockOutline(BlockPos blockPos) {
      return this.smearDirections != null ? false : !this.selectionState.selectionContains(blockPos);
   }

   @Override
   public String getName() {
      return AxiomI18n.get("axiom.buildertool.smear");
   }

   @Override
   public boolean canBeReset() {
      return this.selectionState.hasSelection() || this.smearDirections != null;
   }

   @Override
   public void reset(boolean apply) {
      if (apply && this.selectionState.hasSelection() && this.smearDirections != null) {
         this.apply();
      }

      this.selectionState.resetSelection();
      this.blockRegion.clear();
      this.smearDirections = null;
      if (this.selectionBuffer != null) {
         this.selectionBuffer.close();
         this.selectionBuffer = null;
      }
   }

   @Override
   public BuilderToolSelectionState.Restore getSelectionRestore() {
      return this.selectionState.getSelectionRestore();
   }

   @Override
   public void applySelectionRestore(BuilderToolSelectionState.Restore restore) {
      this.selectionState.restoreFrom(restore);
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.BUILDERTOOL_SMEAR, AxiomPermission.BUILD_SECTION);
   }
}
