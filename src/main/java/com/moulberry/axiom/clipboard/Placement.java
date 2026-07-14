package com.moulberry.axiom.clipboard;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.ServerConfig;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.Position2dSet;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.scaling.RotSprite;
import com.moulberry.axiom.scaling.Scale3x;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.utils.BlockVoxelShapeUtils;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.EntityDataUtils;
import com.moulberry.axiom.utils.IntMatrix;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiom.utils.RenderHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.ClientBlockEntitySerializer;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiom.world_modification.undo.AdditionalUndoOperation;
import com.moulberry.axiom.world_modification.undo.DeleteEntityAdditionalUndoOperation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public enum Placement {
   INSTANCE;

   private static final int CUTOUT_LIMIT = 65536;
   private ChunkedBlockRegion blockRegion;
   private ChunkedBlockRegion scaled3xBlockRegion;
   private Matrix4f partialRotationMatrix;
   private Long2ObjectMap<CompressedBlockEntity> blockEntities = null;
   public List<CompoundTag> entities = null;
   private IntMatrix entityRotationMatrix = null;
   public boolean containsAir = false;
   private BlockPos lastTargetPosition;
   private Gizmo gizmo;
   private PositionSet cutoutPositionSet = null;
   private String placementDescription = null;
   private final AtomicInteger placementId = new AtomicInteger();
   public boolean keepExisting = false;
   private boolean lastKeepExisting = false;
   public boolean prioritizeFullBlocks = false;
   private boolean lastPrioritizeFullBlocks = false;
   public boolean pasteAir = true;
   private boolean lastPasteAir = true;
   public boolean pasteEntities = true;
   public boolean unlockRotation = false;
   public boolean applyToolMask = false;
   public boolean reposition = true;
   private boolean hasAppliedCutout = false;
   public int pasteModifiers = HistoryEntry.MODIFIER_PASTE;
   private boolean acquiredChunkRenderOverrider = false;

   public void replacePlacement(ChunkedBlockRegion blockRegion) {
      this.replacePlacement(blockRegion, this.placementDescription);
   }

   public void replacePlacement(ChunkedBlockRegion blockRegion, String placementDescription) {
      if (this.isPlacing() && !blockRegion.isEmpty()) {
         if (ToolManager.isToolActive()) {
            ToolManager.getCurrentTool().reset();
            ToolManager.setToolSelected(false);
         }

         if (this.gizmo != null) {
            this.gizmo.enableRotation = true;
         }

         this.placementDescription = placementDescription;
         this.blockRegion = blockRegion;
         this.scaled3xBlockRegion = null;
         this.partialRotationMatrix = null;
         this.blockEntities = null;
         this.entities = null;
         this.entityRotationMatrix = null;
         this.containsAir = false;
         this.cutoutPositionSet = null;
         this.reposition = true;
         this.pasteModifiers = HistoryEntry.MODIFIER_PASTE;
         this.updateCutout();
      }
   }

   public int startPlacement(
      BlockPos target,
      ChunkedBlockRegion blockRegion,
      Long2ObjectMap<CompressedBlockEntity> blockEntities,
      List<CompoundTag> entities,
      boolean containsAir,
      String placementDescription
   ) {
      if (this.isPlacing()) {
         this.stopPlacement();
      }

      if (ToolManager.isToolActive()) {
         ToolManager.getCurrentTool().reset();
         ToolManager.setToolSelected(false);
      }

      if (!AxiomClient.hasPermission(AxiomPermission.BUILD_SECTION)) {
         ChatUtils.error("Server hasn't given you permission to place blocks with Axiom");
         return this.placementId.incrementAndGet();
      } else {
         this.lastTargetPosition = target;
         this.placementDescription = placementDescription;
         this.gizmo = new Gizmo(target);
         this.gizmo.enableRotation = true;
         this.blockRegion = blockRegion;
         this.scaled3xBlockRegion = null;
         this.partialRotationMatrix = null;
         this.blockEntities = blockEntities;
         this.entities = entities != null && !entities.isEmpty() ? entities : null;
         this.entityRotationMatrix = new IntMatrix();
         this.containsAir = containsAir;
         this.cutoutPositionSet = null;
         this.updateCutout();
         this.reposition = true;
         this.pasteModifiers = HistoryEntry.MODIFIER_PASTE;
         return this.placementId.incrementAndGet();
      }
   }

   public void startPlacement(BlockPos target, BlockBuffer blockBuffer, String placementDescription) {
      ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
      Long2ObjectMap<CompressedBlockEntity> blockEntities = new Long2ObjectOpenHashMap();
      boolean containsAir = false;
      ObjectIterator var7 = blockBuffer.entrySet().iterator();

      while (var7.hasNext()) {
         Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)var7.next();
         int cx = BlockPos.getX(entry.getLongKey()) * 16 - target.getX();
         int cy = BlockPos.getY(entry.getLongKey()) * 16 - target.getY();
         int cz = BlockPos.getZ(entry.getLongKey()) * 16 - target.getZ();
         Short2ObjectMap<CompressedBlockEntity> chunkBlockEntities = blockBuffer.getBlockEntityChunkMap(entry.getLongKey());

         for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  BlockState block = (BlockState)((PalettedContainer)entry.getValue()).get(x, y, z);
                  if (block != BlockBuffer.EMPTY_STATE) {
                     blockRegion.addBlock(cx + x, cy + y, cz + z, block);
                     containsAir |= block.isAir();
                     if (block.hasBlockEntity() && chunkBlockEntities != null) {
                        short key = (short)(x | y << 4 | z << 8);
                        CompressedBlockEntity blockEntity = (CompressedBlockEntity)chunkBlockEntities.get(key);
                        if (blockEntity != null) {
                           blockEntities.put(BlockPos.asLong(cx + x, cy + y, cz + z), blockEntity);
                        }
                     }
                  }
               }
            }
         }
      }

      if (!blockRegion.isEmpty()) {
         this.startPlacement(target, blockRegion, blockEntities, List.of(), containsAir, placementDescription);
      }
   }

   public ChunkedBlockRegion getBlockRegion() {
      return this.blockRegion;
   }

   private boolean pasteAir() {
      return this.containsAir && this.pasteAir;
   }

   private void tryReleaseChunkRenderOverrider() {
      if (this.acquiredChunkRenderOverrider) {
         ChunkRenderOverrider.release("placement");
         this.acquiredChunkRenderOverrider = false;
         this.hasAppliedCutout = false;
         this.cutoutPositionSet = null;
      }
   }

   public void updateCutout() {
      if (EditorUI.isActive() && this.isPlacing() && this.lastTargetPosition != null && !ToolManager.isToolActive()) {
         this.hasAppliedCutout = false;
         boolean pasteAir = this.pasteAir();
         boolean optionsChanged = pasteAir != this.lastPasteAir
            || this.keepExisting != this.lastKeepExisting
            || this.prioritizeFullBlocks != this.lastPrioritizeFullBlocks;
         this.lastPasteAir = pasteAir;
         this.lastKeepExisting = this.keepExisting;
         this.lastPrioritizeFullBlocks = this.prioritizeFullBlocks;
         if (this.blockRegion.count() <= 65536 && !this.blockRegion.isEmpty()) {
            if (!this.acquiredChunkRenderOverrider) {
               ChunkRenderOverrider.acquire("placement");
               this.acquiredChunkRenderOverrider = true;
            }

            if (this.cutoutPositionSet == null || optionsChanged || this.keepExisting || this.prioritizeFullBlocks) {
               this.cutoutPositionSet = new PositionSet();
               int xo = this.reposition ? -(this.blockRegion.max().getX() + this.blockRegion.min().getX()) / 2 : 0;
               int yo = this.reposition ? -(this.blockRegion.max().getY() + this.blockRegion.min().getY()) / 2 : 0;
               int zo = this.reposition ? -(this.blockRegion.max().getZ() + this.blockRegion.min().getZ()) / 2 : 0;
               if (this.keepExisting || this.prioritizeFullBlocks) {
                  ClientLevel level = Minecraft.getInstance().level;
                  MutableBlockPos mutableBlockPos = new MutableBlockPos();
                  int tx = this.lastTargetPosition.getX();
                  int ty = this.lastTargetPosition.getY();
                  int tz = this.lastTargetPosition.getZ();
                  if (pasteAir) {
                     this.blockRegion
                        .forEachEntry(
                           (x, y, z, block) -> {
                              if (block.isAir()
                                 || shouldReplaceExisting(
                                    this.keepExisting,
                                    this.prioritizeFullBlocks,
                                    level.getBlockState(mutableBlockPos.set(x + xo + tx, y + yo + ty, z + zo + tz)),
                                    block
                                 )) {
                                 this.cutoutPositionSet.add(x + xo, y + yo, z + zo);
                              }
                           }
                        );
                  } else {
                     this.blockRegion
                        .forEachEntry(
                           (x, y, z, block) -> {
                              if (!block.isAir()
                                 && shouldReplaceExisting(
                                    this.keepExisting,
                                    this.prioritizeFullBlocks,
                                    level.getBlockState(mutableBlockPos.set(x + xo + tx, y + yo + ty, z + zo + tz)),
                                    block
                                 )) {
                                 this.cutoutPositionSet.add(x + xo, y + yo, z + zo);
                              }
                           }
                        );
                  }
               } else if (pasteAir) {
                  this.blockRegion.forEachEntry((x, y, z, block) -> this.cutoutPositionSet.add(x + xo, y + yo, z + zo));
               } else {
                  this.blockRegion.forEachEntry((x, y, z, block) -> {
                     if (!block.isAir()) {
                        this.cutoutPositionSet.add(x + xo, y + yo, z + zo);
                     }
                  });
               }
            }

            if (this.cutoutPositionSet.isEmpty()) {
               ChunkRenderOverrider.clear();
            } else {
               ChunkRenderOverrider.cutoutBoolean(this.cutoutPositionSet, this.lastTargetPosition);
            }

            this.hasAppliedCutout = true;
         } else {
            this.tryReleaseChunkRenderOverrider();
         }
      } else {
         this.tryReleaseChunkRenderOverrider();
      }
   }

   public UserAction.ActionResult callAction(UserAction action, Object object) {
      if (!EditorUI.isActive()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else if (!this.isPlacing()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else if (ToolManager.isToolActive()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         switch (action) {
            case ROTATE_PLACEMENT:
               this.rotate(Axis.Y, (float) (-Math.PI / 2));
               this.cutoutPositionSet = null;
               this.updateCutout();
               break;
            case FLIP_PLACEMENT:
               Vec3 lookDirection = Tool.getLookDirection();
               if (lookDirection == null) {
                  lookDirection = Minecraft.getInstance().cameraEntity.getLookAngle();
               }

               Direction direction = PositionUtils.orderedByNearest(lookDirection)[0];
               this.flip(direction.getAxis());
               break;
            case LEFT_MOUSE:
               if (!this.leftClick()) {
                  return UserAction.ActionResult.NOT_HANDLED;
               }
               break;
            case RIGHT_MOUSE:
               if (this.gizmo == null) {
                  RayCaster.RaycastResult result = Tool.raycastBlock();
                  if (result != null) {
                     this.gizmo = new Gizmo(result.blockPos());
                     this.gizmo.enableRotation = true;
                  }
               }
               break;
            case SCROLL:
               UserAction.ScrollAmount scrollAmount = (UserAction.ScrollAmount)object;
               if (!this.handleScroll(scrollAmount.scrollX(), scrollAmount.scrollY())) {
                  return UserAction.ActionResult.NOT_HANDLED;
               }
               break;
            case UNDO:
            case REDO:
               return Dispatcher.callAction(action, object);
            case SAVE:
            case ESCAPE:
               return UserAction.ActionResult.NOT_HANDLED;
            case DELETE:
               this.stopPlacement();
               break;
            case ENTER:
            case PASTE:
               this.pastePlacement();
         }

         return UserAction.ActionResult.USED_STOP;
      }
   }

   public void flip(Axis axis) {
      this.scaled3xBlockRegion = null;
      this.partialRotationMatrix = null;
      this.blockRegion = this.blockRegion.flip(axis);
      if (this.entityRotationMatrix != null) {
         this.entityRotationMatrix.flip(axis);
      }

      this.cutoutPositionSet = null;
      this.updateCutout();
   }

   public void pastePlacement() {
      this.pastePlacement(false);
   }

   public void pastePlacement(boolean select) {
      this.pastePlacementWithoutStopping(select);
      this.stopPlacement();
   }

   public void pastePlacementWithoutStopping(boolean select) {
      BlockPos targetPos;
      if (this.gizmo != null) {
         targetPos = this.gizmo.getTargetPosition();
      } else {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            this.stopPlacement();
            return;
         }

         targetPos = result.blockPos();
      }

      int minX = this.blockRegion.min().getX();
      int minY = this.blockRegion.min().getY();
      int minZ = this.blockRegion.min().getZ();
      int maxX = this.blockRegion.max().getX();
      int maxY = this.blockRegion.max().getY();
      int maxZ = this.blockRegion.max().getZ();
      BlockPos originPos;
      if (this.reposition) {
         originPos = targetPos.offset(-(maxX + minX) / 2, -(maxY + minY) / 2, -(maxZ + minZ) / 2);
      } else {
         originPos = targetPos;
      }

      Dispatcher.resetShouldRemovePlacementOnStep();
      if (select) {
         PositionSet selection = new PositionSet();
         this.blockRegion.forEachEntry((x, y, z, blockState) -> {
            if (!blockState.isAir()) {
               selection.add(originPos.getX() + x, originPos.getY() + y, originPos.getZ() + z);
            }
         });
         Selection.clearSelection();
         Selection.addSet(selection);
      }

      LongSet needNbt = new LongOpenHashSet();
      LongSet needChunks = new LongOpenHashSet();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      BlockBuffer forwards = new BlockBuffer();
      BlockBuffer backwards = new BlockBuffer();
      ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
      MutableBlockPos mutable = new MutableBlockPos();
      int worldMinX = minX + originPos.getX();
      int worldMinY = minY + originPos.getY();
      int worldMinZ = minZ + originPos.getZ();
      int worldMaxX = maxX + originPos.getX();
      int worldMaxY = maxY + originPos.getY();
      int worldMaxZ = maxZ + originPos.getZ();
      ServerConfig serverConfig = Axiom.getInstance().serverConfig;
      Long2ObjectMap<PalettedContainer<BlockState>> containers = new Long2ObjectOpenHashMap();
      int chunkOffsetX = originPos.getX() >> 4;
      int chunkOffsetY = originPos.getY() >> 4;
      int chunkOffsetZ = originPos.getZ() >> 4;
      MaskElement maskElement = this.applyToolMask && EditorWindowType.TOOL_MASKS.isOpen() && MaskManager.hasConfiguredMask()
         ? MaskManager.getDestMask()
         : null;
      MaskContext maskContext = maskElement == null ? null : new MaskContext(Minecraft.getInstance().level);
      if (this.blockEntities != null && !this.blockEntities.isEmpty()) {
         if (this.entityRotationMatrix != null && !this.entityRotationMatrix.isIdentity()) {
            ObjectIterator var53 = this.blockEntities.long2ObjectEntrySet().iterator();

            while (var53.hasNext()) {
               Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var53.next();
               long pos = entry.getLongKey();
               int x = BlockPos.getX(pos);
               int y = BlockPos.getY(pos);
               int z = BlockPos.getZ(pos);
               int nx = this.entityRotationMatrix.transformX(x, y, z) + originPos.getX();
               int ny = this.entityRotationMatrix.transformY(x, y, z) + originPos.getY();
               int nz = this.entityRotationMatrix.transformZ(x, y, z) + originPos.getZ();
               if (maskElement == null || maskElement.test(maskContext.reset(), nx, ny, nz)) {
                  forwards.putBlockEntity(nx, ny, nz, (CompressedBlockEntity)entry.getValue());
               }
            }
         } else {
            ObjectIterator spawnEntries = this.blockEntities.long2ObjectEntrySet().iterator();

            while (spawnEntries.hasNext()) {
               Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)spawnEntries.next();
               long pos = entry.getLongKey();
               int x = BlockPos.getX(pos) + originPos.getX();
               int y = BlockPos.getY(pos) + originPos.getY();
               int z = BlockPos.getZ(pos) + originPos.getZ();
               if (maskElement == null || maskElement.test(maskContext.reset(), x, y, z)) {
                  forwards.putBlockEntity(x, y, z, (CompressedBlockEntity)entry.getValue());
               }
            }
         }
      }

      List<AxiomServerboundSpawnEntity.SpawnEntry> spawnEntries = new ArrayList<>();
      List<AxiomServerboundManipulateEntity.ManipulateEntry> manipulateEntries = new ArrayList<>();
      AdditionalUndoOperation additionalUndoOperation = null;
      if (this.pasteEntities && this.entities != null && !this.entities.isEmpty()) {
         List<UUID> uuids = new ArrayList<>();

         for (CompoundTag entity : this.entities) {
            EntityDataUtils.cloneEntity(
               UUID.randomUUID(), entity.copy(), Vec3.atLowerCornerOf(originPos), this.entityRotationMatrix, spawnEntries, manipulateEntries, uuids
            );
         }

         if (!uuids.isEmpty()) {
            additionalUndoOperation = new DeleteEntityAdditionalUndoOperation(uuids);
         }
      }

      AdditionalUndoOperation additionalUndoOperationF = additionalUndoOperation;
      LongIterator longIterator = this.blockRegion.chunkKeySet().longIterator();

      while (longIterator.hasNext()) {
         long regionPos = longIterator.nextLong();
         int cx = BlockPos.getX(regionPos) + chunkOffsetX;
         int cy = BlockPos.getY(regionPos) + chunkOffsetY;
         int cz = BlockPos.getZ(regionPos) + chunkOffsetZ;

         for (int cxo = 0; cxo <= 1; cxo++) {
            for (int cyo = 0; cyo <= 1; cyo++) {
               for (int czo = 0; czo <= 1; czo++) {
                  int ncx = cx + cxo;
                  int ncy = cy + cyo;
                  int ncz = cz + czo;
                  long chunkPos = BlockPos.asLong(ncx, ncy, ncz);
                  if (!containers.containsKey(chunkPos)
                     && !needChunks.contains(chunkPos)
                     && ncx * 16 + 15 >= worldMinX
                     && ncy * 16 + 15 >= worldMinY
                     && ncz * 16 + 15 >= worldMinZ
                     && ncx * 16 <= worldMaxX
                     && ncy * 16 <= worldMaxY
                     && ncz * 16 <= worldMaxZ) {
                     int sectionIndex = world.getSectionIndexFromSectionY(ncy);
                     if (sectionIndex >= 0 && sectionIndex < world.getSectionsCount()) {
                        LevelChunk chunk = (LevelChunk)world.getChunk(ncx, ncz, ChunkStatus.FULL, false);
                        if (chunk == null) {
                           needChunks.add(chunkPos);
                        } else {
                           LevelChunkSection section = chunk.getSection(sectionIndex);
                           containers.put(chunkPos, section.getStates());
                        }
                     }
                  }
               }
            }
         }
      }

      boolean pasteAir = this.pasteAir();
      containers.forEach(
         (cpos, container) -> {
            int cx = BlockPos.getX(cpos);
            int cy = BlockPos.getY(cpos);
            int cz = BlockPos.getZ(cpos);
            int xo = cx * 16 - originPos.getX();
            int yo = cy * 16 - originPos.getY();
            int zo = cz * 16 - originPos.getZ();
            PalettedContainer<BlockState> forwardsContainer = forwards.getOrCreateSection(cpos);
            PalettedContainer<BlockState> backwardsContainer = backwards.getOrCreateSection(cpos);

            for (int z = 0; z < 16; z++) {
               for (int y = 0; y < 16; y++) {
                  for (int x = 0; x < 16; x++) {
                     BlockState pasteState = this.blockRegion.getBlockStateOrNull(x + xo, y + yo, z + zo);
                     if (pasteState != null) {
                        BlockState existingState = (BlockState)container.get(x, y, z);
                        if ((maskElement == null || maskElement.test(maskContext.reset(), x + cx * 16, y + cy * 16, z + cz * 16))
                           && (
                              pasteState.isAir()
                                 ? pasteAir && !existingState.isAir()
                                 : shouldReplaceExisting(this.keepExisting, this.prioritizeFullBlocks, existingState, pasteState)
                           )) {
                           int toX = x + cx * 16;
                           int toY = y + cy * 16;
                           int toZ = z + cz * 16;
                           forwardsContainer.getAndSetUnchecked(x, y, z, pasteState);
                           backwardsContainer.getAndSetUnchecked(x, y, z, existingState);
                           if (serverConfig.blocksWithCustomData().contains(existingState.getBlock())) {
                              needNbt.add(BlockPos.asLong(toX, toY, toZ));
                           } else if (existingState.hasBlockEntity()) {
                              BlockEntity blockEntity = world.getBlockEntity(mutable.set(toX, toY, toZ));
                              if (blockEntity != null) {
                                 CompoundTag nbt = ClientBlockEntitySerializer.serialize(blockEntity, world.registryAccess());
                                 if (nbt != null) {
                                    if (!nbt.isEmpty()) {
                                       CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(nbt, baos);
                                       backwards.putBlockEntity(toX, toY, toZ, compressedBlockEntity);
                                    }
                                 } else {
                                    needNbt.add(mutable.asLong());
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      );
      if (needNbt.isEmpty() && needChunks.isEmpty()) {
         Dispatcher.push(new HistoryEntry<>(forwards, backwards, targetPos, this.placementDescription, this.pasteModifiers, additionalUndoOperation));
         if (!spawnEntries.isEmpty()) {
            new AxiomServerboundSpawnEntity(spawnEntries).send();
         }

         if (!manipulateEntries.isEmpty()) {
            new AxiomServerboundManipulateEntity(manipulateEntries).send();
         }
      } else {
         String description = this.placementDescription;
         int modifiers = this.pasteModifiers;
         boolean keepExisting = this.keepExisting;
         boolean prioritizeFullBlocks = this.prioritizeFullBlocks;
         Position2ObjectMap<BlockState> copiedBlocks = this.blockRegion.copyBlockData();
         Dispatcher.requestChunkData(
            needNbt,
            needChunks,
            true,
            (compressedBlockEntities, chunkSections) -> {
               BlockState air = Blocks.AIR.defaultBlockState();
               chunkSections.forEach(
                  (cpos, container) -> {
                     int cx = BlockPos.getX(cpos);
                     int cy = BlockPos.getY(cpos);
                     int cz = BlockPos.getZ(cpos);
                     int xo = cx * 16 - originPos.getX();
                     int yo = cy * 16 - originPos.getY();
                     int zo = cz * 16 - originPos.getZ();
                     PalettedContainer<BlockState> forwardsContainer = forwards.getOrCreateSection(cpos);
                     PalettedContainer<BlockState> backwardsContainer = backwards.getOrCreateSection(cpos);

                     for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 16; y++) {
                           for (int x = 0; x < 16; x++) {
                              BlockState pasteState = copiedBlocks.get(x + xo, y + yo, z + zo);
                              if (pasteState != null) {
                                 BlockState existingState = container == null ? air : (BlockState)container.get(x, y, z);
                                 if ((maskElement == null || maskElement.test(maskContext.reset(), x + cx * 16, y + cy * 16, z + cz * 16))
                                    && (
                                       pasteState.isAir()
                                          ? pasteAir && !existingState.isAir()
                                          : shouldReplaceExisting(keepExisting, prioritizeFullBlocks, existingState, pasteState)
                                    )) {
                                    forwardsContainer.getAndSetUnchecked(x, y, z, pasteState);
                                    backwardsContainer.getAndSetUnchecked(x, y, z, existingState);
                                 }
                              }
                           }
                        }
                     }
                  }
               );
               compressedBlockEntities.forEach((pos, compressedBlockEntity) -> {
                  int x = BlockPos.getX(pos);
                  int y = BlockPos.getY(pos);
                  int z = BlockPos.getZ(pos);
                  backwards.putBlockEntity(x, y, z, compressedBlockEntity);
               });
               Dispatcher.push(new HistoryEntry<>(forwards, backwards, targetPos, description, modifiers, additionalUndoOperationF));
               if (!spawnEntries.isEmpty()) {
                  new AxiomServerboundSpawnEntity(spawnEntries).send();
               }

               if (!manipulateEntries.isEmpty()) {
                  new AxiomServerboundManipulateEntity(manipulateEntries).send();
               }
            }
         );
      }
   }

   private static boolean shouldReplaceExisting(boolean keepExisting, boolean prioritizeFullBlocks, BlockState existingState, BlockState pasteState) {
      if (existingState.isAir()) {
         return !pasteState.isAir();
      } else if (existingState.canBeReplaced() && !pasteState.canBeReplaced()) {
         return true;
      } else if (keepExisting) {
         if (prioritizeFullBlocks) {
            VoxelShape existingShape = existingState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            VoxelShape pasteShape = pasteState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            return existingShape == pasteShape ? false : !BlockVoxelShapeUtils.firstCompletelyOverlapsSecond(existingShape, pasteShape);
         } else {
            return false;
         }
      } else if (prioritizeFullBlocks) {
         VoxelShape existingShape = existingState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
         VoxelShape pasteShape = pasteState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
         return existingShape == pasteShape ? true : BlockVoxelShapeUtils.firstCompletelyOverlapsSecond(pasteShape, existingShape);
      } else {
         return true;
      }
   }

   public void stopPlacement() {
      if (this.blockRegion != null) {
         this.tryReleaseChunkRenderOverrider();
         Dispatcher.resetShouldRemovePlacementOnStep();
         this.cutoutPositionSet = null;
         this.blockRegion = null;
         this.scaled3xBlockRegion = null;
         this.partialRotationMatrix = null;
         this.gizmo = null;
      }
   }

   public boolean isPlacing() {
      return this.blockRegion != null;
   }

   private boolean leftClick() {
      return this.gizmo != null && (!EditorUI.isCtrlOrCmdDown() || this.gizmo.isGrabbed()) ? this.gizmo.leftClick() : false;
   }

   public boolean handleScroll(int xScroll, int yScroll) {
      if (this.gizmo != null && (!EditorUI.isCtrlOrCmdDown() || this.gizmo.isGrabbed())) {
         Vec3 look = Tool.getLookDirection();
         if (look != null) {
            this.gizmo.handleScroll(xScroll, yScroll, EditorUI.isCtrlOrCmdDown(), look);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void render(AxiomWorldRenderContext rc) {
      if (this.isPlacing()) {
         boolean showGizmo = false;
         boolean doCutout = false;
         Vec3 translation;
         if (EditorUI.isActive() && !ToolManager.isToolActive()) {
            if (this.gizmo == null) {
               RayCaster.RaycastResult result = Tool.raycastBlock();
               if (result == null) {
                  return;
               }

               translation = Vec3.atCenterOf(result.blockPos());
               if (!result.blockPos().equals(this.lastTargetPosition)) {
                  this.lastTargetPosition = result.blockPos();
                  this.updateCutout();
               }
            } else {
               Vec3 lookDirection = Tool.getLookDirection();
               boolean isLeftDown = Tool.isMouseDown(0);
               boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();
               showGizmo = !EditorUI.isActive() || !isCtrlDown;
               this.gizmo.rotationSnapRadians = this.unlockRotation ? (float)Math.toRadians(1.0) : (float) (Math.PI / 2);
               if (lookDirection != null) {
                  this.gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
                  this.gizmo
                     .setAxisDirections(
                        rc.x() > this.gizmo.getTargetPosition().getX(),
                        rc.y() > this.gizmo.getTargetPosition().getY(),
                        rc.z() > this.gizmo.getTargetPosition().getZ()
                     );
               }

               if (!this.gizmo.getTargetPosition().equals(this.lastTargetPosition)) {
                  this.lastTargetPosition = this.gizmo.getTargetPosition();
                  doCutout = true;
               }

               if (!this.gizmo.isGrabbed()) {
                  Gizmo.GizmoRotation rotation = this.gizmo.popRotation();
                  if (rotation != null) {
                     if (this.unlockRotation && this.partialRotationMatrix == null) {
                        this.partialRotationMatrix = new Matrix4f();
                     }

                     this.rotate(rotation.axis(), rotation.radians());
                     this.cutoutPositionSet = null;
                     doCutout = true;
                  }
               }

               translation = this.gizmo.getInterpPosition();
            }

            if (doCutout
               || this.lastPasteAir != this.pasteAir()
               || this.lastKeepExisting != this.keepExisting
               || this.lastPrioritizeFullBlocks != this.prioritizeFullBlocks) {
               this.updateCutout();
            }
         } else {
            this.tryReleaseChunkRenderOverrider();
            if (this.gizmo == null) {
               return;
            }

            translation = this.gizmo.getInterpPosition();
         }

         Vector3f vector3f = new Vector3f(-0.5F, -0.5F, -0.5F);
         if (this.reposition) {
            vector3f.set(
               -(this.blockRegion.max().getX() + this.blockRegion.min().getX()) / 2 - 0.5F,
               -(this.blockRegion.max().getY() + this.blockRegion.min().getY()) / 2 - 0.5F,
               -(this.blockRegion.max().getZ() + this.blockRegion.min().getZ()) / 2 - 0.5F
            );
         }

         Quaternionf peekRotation = this.gizmo == null ? null : this.gizmo.peekRotation();
         if (peekRotation != null) {
            vector3f.rotate(peekRotation);
         }

         translation = translation.add(vector3f.x, vector3f.y, vector3f.z);
         int blockRegionCount = this.blockRegion.count();
         if (blockRegionCount < 16777216) {
            float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
            float blockOpacity = this.blockRegion.count() > 131072 ? 1.0F : 0.75F + opacity * 0.225F;
            this.blockRegion.render(rc, translation, peekRotation, blockOpacity, 0.1F - opacity * 0.1F, !this.hasAppliedCutout);
         }

         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(-rc.x() + translation.x, -rc.y() + translation.y, -rc.z() + translation.z);
         if (peekRotation != null) {
            matrices.mulPose(peekRotation);
         }

         this.renderBoundingBox(matrices);
         matrices.popPose();
         if (this.gizmo != null && (this.gizmo.isGrabbed() || showGizmo)) {
            this.gizmo.render(rc, EditorUI.isCtrlOrCmdDown());
         }
      }
   }

   private void rotate(Axis axis, float radians) {
      if (this.partialRotationMatrix != null) {
         switch (axis) {
            case X:
               this.partialRotationMatrix.rotateLocalX(radians);
               break;
            case Y:
               this.partialRotationMatrix.rotateLocalY(radians);
               break;
            case Z:
               this.partialRotationMatrix.rotateLocalZ(radians);
         }

         if (this.scaled3xBlockRegion == null) {
            this.scaled3xBlockRegion = Scale3x.scale3x(this.blockRegion, false);
         }

         this.blockRegion = RotSprite.rotateCached(this.scaled3xBlockRegion, this.partialRotationMatrix);
         this.blockEntities = null;
         this.entities = null;
         this.entityRotationMatrix = null;
      } else {
         int rotations = Math.round((float)Math.toDegrees(radians) / 90.0F);
         this.blockRegion = this.blockRegion.rotate(axis, rotations);
         if (this.entityRotationMatrix != null) {
            this.entityRotationMatrix.rotate(axis, rotations);
         }
      }
   }

   public void snapToGround() {
      BlockPos min = this.blockRegion.min();
      BlockPos max = this.blockRegion.max();
      if (this.blockRegion != null && this.gizmo != null && !this.blockRegion.isEmpty() && min != null && max != null) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            Position2dSet insideGroundPositions = new Position2dSet();
            Position2dSet aboveGroundPositions = new Position2dSet();
            int minX = min.getX();
            int minY = min.getY();
            int minZ = min.getZ();
            int maxX = max.getX();
            int maxY = max.getY();
            int maxZ = max.getZ();
            BlockPos originPos;
            if (this.reposition) {
               originPos = this.gizmo.getTargetPosition().offset(-(maxX + minX) / 2, -(maxY + minY) / 2, -(maxZ + minZ) / 2);
            } else {
               originPos = this.gizmo.getTargetPosition();
            }

            boolean hasOnGround = false;
            MutableBlockPos mutableBlockPos = new MutableBlockPos();

            for (int x = minX; x <= maxX; x++) {
               for (int z = minZ; z <= maxZ; z++) {
                  BlockState blockState = this.blockRegion.getBlockStateOrNull(x, minY, z);
                  if (blockState != null) {
                     BlockState existing = level.getBlockState(mutableBlockPos.setWithOffset(originPos, x, minY, z));
                     if (existing.getBlock() != Blocks.VOID_AIR) {
                        if (!existing.canBeReplaced()) {
                           insideGroundPositions.add(x, z);
                        } else {
                           BlockState below = level.getBlockState(mutableBlockPos.setWithOffset(originPos, x, minY - 1, z));
                           if (below.getBlock() != Blocks.VOID_AIR) {
                              if (below.canBeReplaced()) {
                                 aboveGroundPositions.add(x, z);
                              } else {
                                 hasOnGround = true;
                              }
                           }
                        }
                     }
                  }
               }
            }

            if (!aboveGroundPositions.isEmpty()) {
               for (int i = 0; i < 64; i++) {
                  int fi = i;
                  aboveGroundPositions.removeIf((x, zx) -> {
                     BlockState belowx = level.getBlockState(mutableBlockPos.setWithOffset(originPos, x, minY - fi - 1, zx));
                     return belowx.getBlock() == Blocks.VOID_AIR || !belowx.canBeReplaced();
                  });
                  if (aboveGroundPositions.isEmpty()) {
                     this.gizmo.moveToInstantly(this.gizmo.getTargetPosition().offset(0, -i, 0));
                     return;
                  }
               }
            }

            if (!insideGroundPositions.isEmpty() && !hasOnGround) {
               BooleanWrapper found = new BooleanWrapper(false);

               for (int ix = 1; ix < 64; ix++) {
                  int finalI = ix;
                  insideGroundPositions.forEach((x, zx) -> {
                     BlockState existingx = level.getBlockState(mutableBlockPos.setWithOffset(originPos, x, minY + finalI, zx));
                     if (existingx.canBeReplaced()) {
                        found.value = true;
                     }
                  });
                  if (found.value) {
                     this.gizmo.moveToInstantly(this.gizmo.getTargetPosition().offset(0, ix, 0));
                     return;
                  }
               }
            }
         }
      }
   }

   private void renderBoundingBox(PoseStack matrices) {
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      Shapes.lineBox(
         matrices,
         bufferBuilder,
         this.blockRegion.min().getX(),
         this.blockRegion.min().getY(),
         this.blockRegion.min().getZ(),
         this.blockRegion.max().getX() + 1,
         this.blockRegion.max().getY() + 1,
         this.blockRegion.max().getZ() + 1,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         0.0F,
         0.0F,
         0.0F,
         RenderHelper.baseLineWidth
      );
      MeshData meshData = provider.build();
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 0.4F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 0.1F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_IGNORE_DEPTH, null, meshData, true);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
   }
}
