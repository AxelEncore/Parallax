package com.moulberry.axiom.buildertools;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiom.world_modification.undo.AdditionalUndoOperation;
import com.moulberry.axiom.world_modification.undo.DeleteEntityAdditionalUndoOperation;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class StackBuilderTool implements BuilderTool {
   private final BuilderToolSelectionState selectionState = new BuilderToolSelectionState();
   private SelectionBuffer selectionBuffer = null;
   private MutableBlockPos stackDirections = null;
   private final ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();

   @Override
   public void renderScreen(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      if (this.stackDirections != null) {
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.cancel"), Minecraft.getInstance().options.keyAttack, 0
         );
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.confirm"), Minecraft.getInstance().options.keyUse, 1
         );
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.stack.stack_to"), Minecraft.getInstance().options.keyPickItem, 2
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
         if (this.stackDirections == null) {
            this.selectionState.createSelectionBuffer().callDelete(HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME);
         }

         this.reset(false);
      }
   }

   @Override
   public void renderWorld(AxiomWorldRenderContext rc) {
      if (this.stackDirections != null) {
         this.blockRegion.render(rc, Vec3.ZERO, 0.8F, 0.0F);
         BlockPos min = this.selectionBuffer.min();
         BlockPos max = this.selectionBuffer.max();
         int minX = min.getX();
         int minY = min.getY();
         int minZ = min.getZ();
         int maxX = max.getX();
         int maxY = max.getY();
         int maxZ = max.getZ();
         int stackDX = maxX - minX + 1;
         int stackDY = maxY - minY + 1;
         int stackDZ = maxZ - minZ + 1;
         if (this.stackDirections.getX() < 0) {
            minX += stackDX * this.stackDirections.getX();
         } else {
            maxX += stackDX * this.stackDirections.getX();
         }

         if (this.stackDirections.getY() < 0) {
            minY += stackDY * this.stackDirections.getY();
         } else {
            maxY += stackDY * this.stackDirections.getY();
         }

         if (this.stackDirections.getZ() < 0) {
            minZ += stackDZ * this.stackDirections.getZ();
         } else {
            maxZ += stackDZ * this.stackDirections.getZ();
         }

         BuilderTool.renderBoxWithArrow(rc, BuilderTool.calculateDirection(), minX, minY, minZ, maxX, maxY, maxZ);
         this.selectionState.renderWorldMagicSelect(rc);
      } else {
         this.selectionState.renderWorld(rc);
      }
   }

   @Override
   public boolean setPos1(BlockPos position) {
      if (this.stackDirections == null) {
         this.selectionState.setPos1(position);
         this.showTextInActionBar();
      }

      return true;
   }

   @Override
   public boolean setPos2(BlockPos position) {
      if (this.stackDirections == null) {
         this.selectionState.setPos2(position);
         this.showTextInActionBar();
      }

      return true;
   }

   @Override
   public void leftClick(HitResult hitResult) {
      if (this.stackDirections != null) {
         this.reset(false);
      } else if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         this.selectionState.leftClick(blockHitResult);
         this.showTextInActionBar();
      }
   }

   @Override
   public void rightClick(HitResult hitResult) {
      if (this.stackDirections != null) {
         this.reset(true);
      } else if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         this.selectionState.rightClick(blockHitResult);
         this.showTextInActionBar();
      }
   }

   @Override
   public void middleClick(HitResult hitResult) {
      if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         BlockPos blockPos = blockHitResult.getBlockPos();
         if (this.stackDirections != null) {
            BlockPos newStackDirections = new BlockPos(
               this.calculateStackToForAxis(blockPos, Axis.X), this.calculateStackToForAxis(blockPos, Axis.Y), this.calculateStackToForAxis(blockPos, Axis.Z)
            );
            if (!this.stackDirections.equals(newStackDirections)) {
               this.stackDirections.set(newStackDirections);
               this.recalculate(Minecraft.getInstance().level);
            }

            return;
         }

         this.selectionState.middleClick(blockHitResult);
         this.showTextInActionBar();
      }
   }

   private int calculateStackToForAxis(BlockPos blockPos, Axis axis) {
      int min = this.selectionBuffer.min().get(axis);
      int max = this.selectionBuffer.max().get(axis);
      int size = max - min + 1;
      if (blockPos.get(axis) < min) {
         int delta = blockPos.get(axis) - min;
         return (delta - (size - 1)) / size;
      } else if (blockPos.get(axis) > max) {
         int delta = blockPos.get(axis) - max;
         return (delta + (size - 1)) / size;
      } else {
         return 0;
      }
   }

   private void apply() {
      if (this.stackDirections != null && (this.stackDirections.getX() != 0 || this.stackDirections.getY() != 0 || this.stackDirections.getZ() != 0)) {
         String description = AxiomI18n.get("axiom.history_description.stacked", NumberFormat.getInstance().format((long)this.blockRegion.count()));
         AdditionalUndoOperation additionalUndoOperation = null;
         if (this.blockRegion.isEmpty()) {
            return;
         }

         BlockPos pos1 = this.selectionBuffer.min();
         BlockPos pos2 = this.selectionBuffer.max();
         List<AxiomServerboundSpawnEntity.SpawnEntry> spawnEntries = new ArrayList<>();
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && ClientEvents.serverSupportsProtocol(SupportedProtocol.CREATE_ENTITY) && BuilderToolManager.copyEntities) {
            int posMinX = Math.min(pos1.getX(), pos2.getX());
            int posMinY = Math.min(pos1.getY(), pos2.getY());
            int posMinZ = Math.min(pos1.getZ(), pos2.getZ());
            int posMaxX = Math.max(pos1.getX(), pos2.getX());
            int posMaxY = Math.max(pos1.getY(), pos2.getY());
            int posMaxZ = Math.max(pos1.getZ(), pos2.getZ());
            AABB bounds = new AABB(new Vec3(posMinX, posMinY, posMinZ), new Vec3(posMaxX + 1, posMaxY + 1, posMaxZ + 1));
            List<Entity> entities = level.getEntities(Minecraft.getInstance().player, bounds, BuilderTool::shouldEntityByCopied);
            if (!entities.isEmpty()) {
               List<UUID> uuids = new ArrayList<>();
               CompoundTag emptyTag = new CompoundTag();
               int stackDX = (posMaxX - posMinX + 1) * (int)Math.signum((float)this.stackDirections.getX());
               int stackDY = (posMaxY - posMinY + 1) * (int)Math.signum((float)this.stackDirections.getY());
               int stackDZ = (posMaxZ - posMinZ + 1) * (int)Math.signum((float)this.stackDirections.getZ());
               int stackMaxX = Math.abs(this.stackDirections.getX());
               int stackMaxY = Math.abs(this.stackDirections.getY());
               int stackMaxZ = Math.abs(this.stackDirections.getZ());

               for (Entity entity : entities) {
                  Vec3 entityPos;
                  if (entity instanceof HangingEntity hangingEntity) {
                     entityPos = Vec3.atCenterOf(hangingEntity.getPos());
                  } else {
                     entityPos = entity.position();
                  }

                  double x = entityPos.x;
                  double y = entityPos.y;
                  double z = entityPos.z;

                  for (int stackX = 0; stackX <= stackMaxX; stackX++) {
                     for (int stackY = 0; stackY <= stackMaxY; stackY++) {
                        for (int stackZ = 0; stackZ <= stackMaxZ; stackZ++) {
                           if (stackX != 0 || stackY != 0 || stackZ != 0) {
                              Vec3 position = new Vec3(x + stackX * stackDX, y + stackY * stackDY, z + stackZ * stackDZ);
                              UUID uuid = UUID.randomUUID();
                              spawnEntries.add(
                                 new AxiomServerboundSpawnEntity.SpawnEntry(uuid, position, entity.getYRot(), entity.getXRot(), entity.getUUID(), emptyTag)
                              );
                              uuids.add(uuid);
                           }
                        }
                     }
                  }
               }

               additionalUndoOperation = new DeleteEntityAdditionalUndoOperation(uuids);
            }
         }

         int flags = HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME;
         if (BuilderToolManager.keepExisting) {
            flags |= HistoryEntry.MODIFIER_KEEP_EXISTING;
         }

         int minX = Math.min(pos1.getX(), pos2.getX());
         int minY = Math.min(pos1.getY(), pos2.getY());
         int minZ = Math.min(pos1.getZ(), pos2.getZ());
         int sizeX = Math.abs(pos1.getX() - pos2.getX()) + 1;
         int sizeY = Math.abs(pos1.getY() - pos2.getY()) + 1;
         int sizeZ = Math.abs(pos1.getZ() - pos2.getZ()) + 1;
         RegionHelper.pushBlockRegionChangeWithNBT(this.blockRegion, description, flags, pos -> {
            int xx = BlockPos.getX(pos);
            int yx = BlockPos.getY(pos);
            int zx = BlockPos.getZ(pos);
            xx -= minX;
            yx -= minY;
            zx -= minZ;
            xx %= sizeX;
            yx %= sizeY;
            zx %= sizeZ;
            if (xx < 0) {
               xx += sizeX;
            }

            if (yx < 0) {
               yx += sizeY;
            }

            if (zx < 0) {
               zx += sizeZ;
            }

            return BlockPos.asLong(xx + minX, yx + minY, zx + minZ);
         }, additionalUndoOperation);
         if (!spawnEntries.isEmpty()) {
            new AxiomServerboundSpawnEntity(spawnEntries).send();
         }
      }
   }

   @Override
   public boolean scroll(int scroll) {
      if (!this.selectionState.hasSelection()) {
         return false;
      } else {
         Level level = Minecraft.getInstance().level;
         if (level == null) {
            return false;
         } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
               return false;
            } else {
               Direction dir = BuilderTool.calculateDirection();
               if (this.stackDirections == null) {
                  if (ClientEvents.builderToolNudgeScrollKeyBind.isDown()) {
                     this.nudge(scroll);
                     return true;
                  }

                  this.stackDirections = new MutableBlockPos();
               }

               this.stackDirections.move(dir, scroll);
               this.showTextInActionBar();
               this.recalculate(level);
               return true;
            }
         }
      }
   }

   private void recalculate(Level level) {
      this.blockRegion.clear();
      if (this.selectionBuffer != null) {
         this.selectionBuffer.close();
      }

      this.selectionBuffer = this.selectionState.createSelectionBuffer();
      BlockPos min = this.selectionBuffer.min();
      BlockPos max = this.selectionBuffer.max();
      if (!this.selectionBuffer.isEmpty() && min != null && max != null) {
         int stackDX = (max.getX() - min.getX() + 1) * (int)Math.signum((float)this.stackDirections.getX());
         int stackDY = (max.getY() - min.getY() + 1) * (int)Math.signum((float)this.stackDirections.getY());
         int stackDZ = (max.getZ() - min.getZ() + 1) * (int)Math.signum((float)this.stackDirections.getZ());
         int stackMaxX = Math.abs(this.stackDirections.getX());
         int stackMaxY = Math.abs(this.stackDirections.getY());
         int stackMaxZ = Math.abs(this.stackDirections.getZ());
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         this.selectionBuffer.forEach((x, y, z) -> {
            BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
            if (BuilderToolManager.copyAir || !blockState.isAir()) {
               for (int stackX = 0; stackX <= stackMaxX; stackX++) {
                  for (int stackY = 0; stackY <= stackMaxY; stackY++) {
                     for (int stackZ = 0; stackZ <= stackMaxZ; stackZ++) {
                        if (stackX != 0 || stackY != 0 || stackZ != 0) {
                           this.blockRegion.addBlock(x + stackX * stackDX, y + stackY * stackDY, z + stackZ * stackDZ, blockState);
                        }
                     }
                  }
               }
            }
         });
      } else {
         this.selectionBuffer.close();
         this.selectionBuffer = null;
      }
   }

   private void nudge(int amount) {
      if (this.stackDirections == null) {
         this.selectionState.nudge(amount);
         this.showTextInActionBar();
      }
   }

   private void showTextInActionBar() {
      if (this.stackDirections != null) {
         ScreenRenderHook.setOverlayText(
            Component.literal(AxiomI18n.get("axiom.hardcoded.stacks_prefix"))
               .withStyle(ChatFormatting.YELLOW)
               .append(Component.literal(this.stackDirections.getX() + " ").withStyle(ChatFormatting.RED))
               .append(Component.literal(this.stackDirections.getY() + " ").withStyle(ChatFormatting.GREEN))
               .append(Component.literal(this.stackDirections.getZ() + "").withStyle(ChatFormatting.AQUA))
         );
      } else {
         this.selectionState.showTextInActionBar();
      }
   }

   @Override
   public boolean shouldRenderBlockOutline(BlockPos blockPos) {
      return this.stackDirections != null ? false : !this.selectionState.selectionContains(blockPos);
   }

   @Override
   public String getName() {
      return AxiomI18n.get("axiom.buildertool.stack");
   }

   @Override
   public boolean canBeReset() {
      return this.selectionState.hasSelection() || this.stackDirections != null;
   }

   @Override
   public void reset(boolean apply) {
      if (apply && this.selectionState.hasSelection() && this.stackDirections != null) {
         this.apply();
      }

      this.selectionState.resetSelection();
      this.blockRegion.clear();
      this.stackDirections = null;
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
      return EnumSet.of(AxiomPermission.BUILDERTOOL_STACK, AxiomPermission.BUILD_SECTION);
   }
}
