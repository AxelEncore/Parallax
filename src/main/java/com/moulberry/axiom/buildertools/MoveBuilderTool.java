package com.moulberry.axiom.buildertools;

import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.IntMatrix;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiom.world_modification.undo.AdditionalUndoOperation;
import com.moulberry.axiom.world_modification.undo.MoveEntityAdditionalUndoOperation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class MoveBuilderTool implements BuilderTool {
   private final BuilderToolSelectionState selectionState = new BuilderToolSelectionState();
   private boolean overridingChunkRender = false;
   private final IntMatrix transformMatrix = new IntMatrix();
   private MutableBlockPos regionOffset = null;
   private SelectionBuffer selectionBuffer = null;
   private ChunkedBlockRegion blockRegion = null;
   private Long2ObjectMap<CompressedBlockEntity> blockEntities = null;
   private final PendingAction pendingAction = new PendingAction();
   private int pendingCopyId = 0;

   @Override
   public void renderScreen(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      if (this.blockRegion != null) {
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.cancel"), Minecraft.getInstance().options.keyAttack, 0
         );
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.confirm"), Minecraft.getInstance().options.keyUse, 1
         );
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.move_to"), Minecraft.getInstance().options.keyPickItem, 2
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
         if (this.selectionBuffer != null) {
            this.selectionBuffer.callDelete(HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME);
         } else {
            this.selectionState.createSelectionBuffer().callDelete(HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME);
         }

         this.reset(false);
      }
   }

   @Override
   public void renderWorld(AxiomWorldRenderContext rc) {
      if (this.regionOffset != null) {
         boolean rotatePressed = Keybinds.ROTATE_PLACEMENT.isPressed(false);
         boolean flipPressed = Keybinds.FLIP_PLACEMENT.isPressed(false);
         this.blockRegion = BuilderTool.handleMoveableBlockBuffer(rc, this.blockRegion, this.regionOffset, this.transformMatrix, rotatePressed, flipPressed);
      } else {
         if (this.selectionState.hasSelection()) {
            boolean rotatePressed = Keybinds.ROTATE_PLACEMENT.isPressed(false);
            boolean flipPressed = Keybinds.FLIP_PLACEMENT.isPressed(false);
            if (rotatePressed || flipPressed) {
               this.initiateMovement();
            }

            if (this.blockRegion != null) {
               if (rotatePressed) {
                  this.blockRegion = this.blockRegion.rotate(Axis.Y, -1);
                  this.transformMatrix.rotateY(-1);
               }

               if (flipPressed) {
                  Direction direction = BuilderTool.calculateDirection();
                  this.blockRegion = this.blockRegion.flip(direction.getAxis());
                  this.transformMatrix.flip(direction.getAxis());
               }
            } else {
               if (rotatePressed) {
                  this.pendingAction.rotateY(-1);
               }

               if (flipPressed) {
                  Direction direction = BuilderTool.calculateDirection();
                  switch (direction.getAxis()) {
                     case X:
                        this.pendingAction.flipX();
                        break;
                     case Y:
                        this.pendingAction.flipY();
                        break;
                     case Z:
                        this.pendingAction.flipZ();
                  }
               }
            }
         }

         this.selectionState.renderWorld(rc);
      }
   }

   @Override
   public boolean setPos1(BlockPos position) {
      if (this.regionOffset == null) {
         this.selectionState.setPos1(position);
         this.showTextInActionBar();
      }

      return true;
   }

   @Override
   public boolean setPos2(BlockPos position) {
      if (this.regionOffset == null) {
         this.selectionState.setPos2(position);
         this.showTextInActionBar();
      }

      return true;
   }

   @Override
   public void leftClick(HitResult hitResult) {
      if (this.regionOffset != null) {
         this.reset(false);
      } else if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         this.selectionState.leftClick(blockHitResult);
         this.showTextInActionBar();
      }
   }

   @Override
   public void rightClick(HitResult hitResult) {
      if (this.regionOffset != null) {
         this.reset(true);
      } else if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         this.selectionState.rightClick(blockHitResult);
         this.showTextInActionBar();
      }
   }

   @Override
   public void middleClick(HitResult hitResult) {
      if (hitResult.getType() == Type.MISS && this.regionOffset != null && Minecraft.getInstance().player != null) {
         this.regionOffset.set(Minecraft.getInstance().player.blockPosition());
         this.showTextInActionBar();
      } else if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         if (this.regionOffset != null) {
            BuilderTool.setOffsetFromBlock(blockHitResult, this.regionOffset, this.selectionBuffer.min(), this.selectionBuffer.max());
         } else {
            this.selectionState.middleClick(blockHitResult);
         }

         this.showTextInActionBar();
      }
   }

   private void apply() {
      if (this.regionOffset != null) {
         BlockPos offset = this.regionOffset.immutable();
         ChunkedBlockRegion blockRegion = this.blockRegion;
         BlockPos min = blockRegion.min();
         BlockPos max = blockRegion.max();
         int minX = Math.min(min.getX(), max.getX());
         int minY = Math.min(min.getY(), max.getY());
         int minZ = Math.min(min.getZ(), max.getZ());
         int maxX = Math.max(min.getX(), max.getX());
         int maxY = Math.max(min.getY(), max.getY());
         int maxZ = Math.max(min.getZ(), max.getZ());
         float partialX = (maxX - minX) % 2 / 2.0F;
         float partialY = (maxY - minY) % 2 / 2.0F;
         float partialZ = (maxZ - minZ) % 2 / 2.0F;
         int shiftX = (int)(-Math.floor(this.transformMatrix.transformDoubleX(partialX, partialY, partialZ)));
         int shiftY = (int)(-Math.floor(this.transformMatrix.transformDoubleY(partialX, partialY, partialZ)));
         int shiftZ = (int)(-Math.floor(this.transformMatrix.transformDoubleZ(partialX, partialY, partialZ)));
         offset = offset.offset(shiftX, shiftY, shiftZ);
         String description = AxiomI18n.get("axiom.history_description.moved", NumberFormat.getInstance().format((long)blockRegion.count()));
         if (BuilderToolManager.keepExisting) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
               blockRegion = new ChunkedBlockRegion();
               int offsetX = offset.getX();
               int offsetY = offset.getY();
               int offsetZ = offset.getZ();
               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               ChunkedBlockRegion region = blockRegion;
               this.blockRegion.forEachEntry((xx, yx, zx, blockState) -> {
                  if (this.selectionBuffer.contains(xx + offsetX, yx + offsetY, zx + offsetZ)) {
                     region.addBlock(xx, yx, zx, blockState);
                  } else {
                     BlockState existingState = level.getBlockState(mutableBlockPos.set(xx + offsetX, yx + offsetY, zx + offsetZ));
                     if (existingState.isAir()) {
                        region.addBlock(xx, yx, zx, blockState);
                     }
                  }
               });
            }
         }

         ChunkedBlockRegion blockRegionF = blockRegion;
         int offsetX = offset.getX();
         int offsetY = offset.getY();
         int offsetZ = offset.getZ();
         this.selectionBuffer
            .forEach((xx, yx, zx) -> blockRegionF.addBlockIfNotPresent(xx - offsetX, yx - offsetY, zx - offsetZ, Blocks.AIR.defaultBlockState()));
         Long2ObjectMap<CompressedBlockEntity> blockEntities;
         if (!this.blockEntities.isEmpty() && !this.transformMatrix.isIdentity()) {
            blockEntities = new Long2ObjectOpenHashMap();
            ObjectIterator var65 = this.blockEntities.long2ObjectEntrySet().iterator();

            while (var65.hasNext()) {
               Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var65.next();
               long pos = entry.getLongKey();
               int x = BlockPos.getX(pos);
               int y = BlockPos.getY(pos);
               int z = BlockPos.getZ(pos);
               int nx = this.transformMatrix.transformX(x, y, z);
               int ny = this.transformMatrix.transformY(x, y, z);
               int nz = this.transformMatrix.transformZ(x, y, z);
               if (!blockRegion.getBlockStateOrAir(nx, ny, nz).isAir()) {
                  blockEntities.put(BlockPos.asLong(nx, ny, nz), (CompressedBlockEntity)entry.getValue());
               }
            }
         } else {
            blockEntities = this.blockEntities;
         }

         if (blockRegion.isEmpty()) {
            return;
         }

         AdditionalUndoOperation additionalUndoOperation = null;
         List<AxiomServerboundManipulateEntity.ManipulateEntry> manipulateEntries = new ArrayList<>();
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && ClientEvents.serverSupportsProtocol(SupportedProtocol.MANIPULATE_ENTITY) && BuilderToolManager.copyEntities) {
            AABB bounds = AABB.encapsulatingFullBlocks(this.selectionBuffer.min(), this.selectionBuffer.max());
            BlockPos selectionBufferCenter = this.selectionBuffer.center();
            int originalOffsetX = selectionBufferCenter.getX();
            int originalOffsetY = selectionBufferCenter.getY();
            int originalOffsetZ = selectionBufferCenter.getZ();
            List<Entity> entities = level.getEntities(Minecraft.getInstance().player, bounds, BuilderTool::shouldEntityByCopied);
            if (!entities.isEmpty()) {
               List<MoveEntityAdditionalUndoOperation.UUIDAndPosition> positions = new ArrayList<>();

               for (Entity entity : entities) {
                  Vec3 entityPos;
                  if (entity instanceof HangingEntity hangingEntity) {
                     entityPos = Vec3.atCenterOf(hangingEntity.getPos());
                  } else {
                     entityPos = entity.position();
                  }

                  double x = entityPos.x - originalOffsetX - 0.5;
                  double y = entityPos.y - originalOffsetY - 0.5;
                  double z = entityPos.z - originalOffsetZ - 0.5;
                  Vec3 position;
                  float newYaw;
                  float newPitch;
                  if (this.transformMatrix.isIdentity()) {
                     position = new Vec3(x + offset.getX() + 0.5, y + offset.getY() + 0.5, z + offset.getZ() + 0.5);
                     newYaw = entity.getYRot();
                     newPitch = entity.getXRot();
                  } else {
                     double nx = this.transformMatrix.transformDoubleX(x, y, z);
                     double ny = this.transformMatrix.transformDoubleY(x, y, z);
                     double nz = this.transformMatrix.transformDoubleZ(x, y, z);
                     position = new Vec3(nx + offset.getX() + 0.5, ny + offset.getY() + 0.5, nz + offset.getZ() + 0.5);
                     Vec3 viewVector = entity.getViewVector(1.0F);
                     double viewX = this.transformMatrix.transformDoubleX(viewVector.x, viewVector.y, viewVector.z);
                     double viewY = this.transformMatrix.transformDoubleY(viewVector.x, viewVector.y, viewVector.z);
                     double viewZ = this.transformMatrix.transformDoubleZ(viewVector.x, viewVector.y, viewVector.z);
                     double viewXZ = Math.sqrt(viewX * viewX + viewZ * viewZ);
                     newPitch = Mth.wrapDegrees((float)(-(Mth.atan2(viewY, viewXZ) * 180.0F / (float)Math.PI)));
                     newYaw = Mth.wrapDegrees((float)(Mth.atan2(viewZ, viewX) * 180.0F / (float)Math.PI) - 90.0F);
                     newPitch = Math.round(newPitch * 256.0F) / 256.0F;
                     newYaw = Math.round(newYaw * 256.0F) / 256.0F;
                  }

                  manipulateEntries.add(new AxiomServerboundManipulateEntity.ManipulateEntry(entity.getUUID(), Set.of(), position, newYaw, newPitch, null));
                  positions.add(new MoveEntityAdditionalUndoOperation.UUIDAndPosition(entity.getUUID(), entityPos, entity.getYRot(), entity.getXRot()));
               }

               additionalUndoOperation = new MoveEntityAdditionalUndoOperation(positions);
            }
         }

         RegionHelper.pushBlockRegionChangeOffset(
            blockRegion, blockEntities, offset, description, HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME, additionalUndoOperation
         );
         if (!manipulateEntries.isEmpty()) {
            new AxiomServerboundManipulateEntity(manipulateEntries).send();
         }
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
            if (this.regionOffset == null) {
               if (ClientEvents.builderToolNudgeScrollKeyBind.isDown()) {
                  this.nudge(scroll);
                  return true;
               }

               this.initiateMovement();
            }

            if (this.regionOffset != null) {
               this.regionOffset.move(BuilderTool.calculateDirection(), scroll);
               this.showTextInActionBar();
            } else {
               this.pendingAction.move(BuilderTool.calculateDirection(), scroll);
            }

            return true;
         }
      }
   }

   private void nudge(int amount) {
      if (this.regionOffset == null) {
         this.selectionState.nudge(amount);
         this.showTextInActionBar();
      }
   }

   private void showTextInActionBar() {
      if (this.regionOffset != null) {
         BlockPos center = this.selectionBuffer.center();
         ScreenRenderHook.setOverlayText(
            Component.literal(AxiomI18n.get("axiom.hardcoded.offset_prefix"))
               .withStyle(ChatFormatting.YELLOW)
               .append(Component.literal(this.regionOffset.getX() - center.getX() + " ").withStyle(ChatFormatting.RED))
               .append(Component.literal(this.regionOffset.getY() - center.getY() + " ").withStyle(ChatFormatting.GREEN))
               .append(Component.literal(this.regionOffset.getZ() - center.getZ() + "").withStyle(ChatFormatting.AQUA))
         );
      } else {
         this.selectionState.showTextInActionBar();
      }
   }

   private void initiateMovement() {
      if (this.pendingCopyId == 0) {
         SelectionBuffer selection = this.selectionState.createSelectionBuffer();
         BlockPos selectionOffset = selection.center();
         if (!selection.isEmpty() && selectionOffset != null) {
            int offsetX = selectionOffset.getX();
            int offsetY = selectionOffset.getY();
            int offsetZ = selectionOffset.getZ();
            int randomId = ThreadLocalRandom.current().nextInt();
            int copyId = randomId == 0 ? 1 : randomId;
            this.pendingCopyId = copyId;
            this.pendingAction.reset();
            selection.callCopy(false, BuilderToolManager.copyAir)
               .thenAccept(
                  copyResult -> {
                     if (this.pendingCopyId == copyId) {
                        this.pendingCopyId = 0;
                        if (copyResult.chunkedBlockRegion().isEmpty()) {
                           this.reset(false);
                        } else {
                           this.transformMatrix.identity();
                           this.blockRegion = copyResult.chunkedBlockRegion();
                           this.blockEntities = copyResult.blockEntities();
                           if (!this.overridingChunkRender) {
                              ChunkRenderOverrider.acquire("MoveBuilderTool");
                              this.overridingChunkRender = true;
                           }

                           this.blockRegion
                              .forEachEntry(
                                 (x, y, z, block) -> ChunkRenderOverrider.setBlock(
                                    x + offsetX, y + offsetY, z + offsetZ, Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState()
                                 )
                              );
                           if (this.selectionBuffer != null) {
                              this.selectionBuffer.close();
                           }

                           this.selectionBuffer = selection;
                           this.regionOffset = new MutableBlockPos(offsetX, offsetY, offsetZ);
                           this.regionOffset.move(this.pendingAction.moveX, this.pendingAction.moveY, this.pendingAction.moveZ);
                           this.showTextInActionBar();
                           this.blockRegion = this.blockRegion.rotate(Axis.Y, this.pendingAction.rotateY);
                           this.transformMatrix.rotateY(this.pendingAction.rotateY);
                           if (this.pendingAction.flipX) {
                              this.blockRegion = this.blockRegion.flip(Axis.X);
                              this.transformMatrix.flip(Axis.X);
                           }

                           if (this.pendingAction.flipY) {
                              this.blockRegion = this.blockRegion.flip(Axis.Y);
                              this.transformMatrix.flip(Axis.Y);
                           }

                           if (this.pendingAction.flipZ) {
                              this.blockRegion = this.blockRegion.flip(Axis.Z);
                              this.transformMatrix.flip(Axis.Z);
                           }

                           this.pendingAction.reset();
                        }
                     }
                  }
               );
         }
      }
   }

   @Override
   public boolean shouldRenderBlockOutline(BlockPos blockPos) {
      return this.regionOffset != null ? false : !this.selectionState.selectionContains(blockPos);
   }

   @Override
   public String getName() {
      return AxiomI18n.get("axiom.buildertool.move");
   }

   @Override
   public List<String> getKeyHints() {
      return this.regionOffset != null ? List.of("Ctrl+R - Rotate", "Ctrl+F - Flip") : List.of();
   }

   @Override
   public boolean canBeReset() {
      return this.selectionState.hasSelection() || this.regionOffset != null;
   }

   @Override
   public void reset(boolean apply) {
      if (apply && this.selectionState.hasSelection() && this.regionOffset != null) {
         this.apply();
      }

      this.selectionState.resetSelection();
      this.pendingCopyId = 0;
      this.pendingAction.reset();
      this.blockRegion = null;
      this.transformMatrix.identity();
      this.regionOffset = null;
      if (this.selectionBuffer != null) {
         this.selectionBuffer.close();
         this.selectionBuffer = null;
      }

      if (this.overridingChunkRender) {
         ChunkRenderOverrider.release("MoveBuilderTool");
         this.overridingChunkRender = false;
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
      return EnumSet.of(AxiomPermission.BUILDERTOOL_MOVE, AxiomPermission.BUILD_SECTION);
   }
}
