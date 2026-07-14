package com.moulberry.axiom;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.annotation.AnnotationTool;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;

public class RayCaster {
   public static RayCaster.RaycastResult raycast(Level level, Vec3 from, Vec3 direction, boolean includeSelection, boolean includeFluids) {
      return raycast(level, new Vector3d(from.x, from.y, from.z), new Vector3d(direction.x, direction.y, direction.z), includeSelection, includeFluids);
   }

   public static RayCaster.RaycastResult raycast(Level level, Vector3d from, Vector3d direction, boolean includeSelection, boolean includeFluids) {
      return raycast(level, from, direction, includeSelection, includeFluids, true);
   }

   public static RayCaster.RaycastResult raycast(
      Level level, Vec3 from, Vec3 direction, boolean includeSelection, boolean includeFluids, boolean includeNonSolid
   ) {
      return raycast(
         level, new Vector3d(from.x, from.y, from.z), new Vector3d(direction.x, direction.y, direction.z), includeSelection, includeFluids, includeNonSolid
      );
   }

   public static RayCaster.RaycastResult raycast(
      Level level, Vector3d from, Vector3d direction, boolean includeSelection, boolean includeFluids, boolean includeNonSolid
   ) {
      if (Selection.getSelectionBuffer().isEmpty()) {
         includeSelection = false;
      }

      direction.normalize();
      if (from.y < level.getMinBuildHeight()) {
         double yTarget = level.getMinBuildHeight() + 0.01;
         if (direction.y <= 0.0) {
            return null;
         }

         double steps = (yTarget - from.y) / direction.y;
         from.x = from.x + direction.x * steps;
         from.y = yTarget;
         from.z = from.z + direction.z * steps;
      } else if (from.y >= level.getMaxBuildHeight() - 1 + 1) {
         double yTarget = level.getMaxBuildHeight() - 1 + 1 - 0.01;
         if (direction.y >= 0.0) {
            return null;
         }

         double steps = (yTarget - from.y) / direction.y;
         from.x = from.x + direction.x * steps;
         from.y = yTarget;
         from.z = from.z + direction.z * steps;
      }

      Vec3 fromVec3 = new Vec3(from.x, from.y, from.z);
      int mapX = (int)Math.floor(from.x);
      int mapY = (int)Math.floor(from.y);
      int mapZ = (int)Math.floor(from.z);
      double deltaDistX = Math.abs(1.0 / direction.x);
      double deltaDistY = Math.abs(1.0 / direction.y);
      double deltaDistZ = Math.abs(1.0 / direction.z);
      int stepX = direction.x < 0.0 ? -1 : 1;
      int stepY = direction.y < 0.0 ? -1 : 1;
      int stepZ = direction.z < 0.0 ? -1 : 1;
      double sideDistX = (direction.x > 0.0 ? 1.0 - from.x + mapX : from.x - mapX) * deltaDistX;
      double sideDistY = (direction.y > 0.0 ? 1.0 - from.y + mapY : from.y - mapY) * deltaDistY;
      double sideDistZ = (direction.z > 0.0 ? 1.0 - from.z + mapZ : from.z - mapZ) * deltaDistZ;
      if (Double.isNaN(sideDistX)) {
         sideDistX = Double.POSITIVE_INFINITY;
      }

      if (Double.isNaN(sideDistY)) {
         sideDistY = Double.POSITIVE_INFINITY;
      }

      if (Double.isNaN(sideDistZ)) {
         sideDistZ = Double.POSITIVE_INFINITY;
      }

      int oldChunkX = mapX >> 4;
      int oldChunkY = mapY >> 4;
      int oldChunkZ = mapZ >> 4;
      LevelChunk chunk = (LevelChunk)level.getChunk(oldChunkX, oldChunkZ, ChunkStatus.FULL, false);
      if (chunk == null) {
         return null;
      } else {
         int sectionIndex = chunk.getSectionIndexFromSectionY(oldChunkY);
         LevelChunkSection section = chunk.getSection(sectionIndex);
         PalettedContainer<BlockState> container = section.getStates();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         LocalPlayer player = Minecraft.getInstance().player;
         CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
         BlockState currentBlock = (BlockState)container.get(mapX & 15, mapY & 15, mapZ & 15);
         boolean isInsideBlock = currentBlock.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
         Vector3d exitPosition = null;
         int limit = 0;

         while (limit++ < 3000) {
            if (sideDistZ < sideDistX && sideDistZ < sideDistY) {
               sideDistZ += deltaDistZ;
               mapZ += stepZ;
            } else if (sideDistX < sideDistY) {
               sideDistX += deltaDistX;
               mapX += stepX;
            } else {
               sideDistY += deltaDistY;
               mapY += stepY;
            }

            int newChunkX = mapX >> 4;
            int newChunkY = mapY >> 4;
            int newChunkZ = mapZ >> 4;
            boolean updateSection = newChunkY != oldChunkY;
            if (newChunkX != oldChunkX || newChunkZ != oldChunkZ) {
               oldChunkX = newChunkX;
               oldChunkZ = newChunkZ;
               chunk = (LevelChunk)level.getChunk(newChunkX, newChunkZ, ChunkStatus.FULL, false);
               if (chunk == null) {
                  return null;
               }

               updateSection = true;
            }

            if (updateSection) {
               oldChunkY = newChunkY;
               if (newChunkY < chunk.getMinSection() || newChunkY > chunk.getMaxSection() - 1) {
                  return null;
               }

               sectionIndex = chunk.getSectionIndexFromSectionY(newChunkY);
               section = chunk.getSection(sectionIndex);
               container = section.getStates();
            }

            if (includeSelection && Selection.contains(mapX, mapY, mapZ)) {
               return new RayCaster.RaycastResult(new BlockPos(mapX, mapY, mapZ), Vec3.atCenterOf(new BlockPos(mapX, mapY, mapZ)), Direction.UP, true);
            }

            BlockState blockState = (BlockState)container.get(mapX & 15, mapY & 15, mapZ & 15);
            if (blockState.isAir()) {
               isInsideBlock = false;
            } else {
               mutableBlockPos.set(mapX, mapY, mapZ);
               Vec3 toVec3 = fromVec3.add(direction.x * (limit + 5), direction.y * (limit + 5), direction.z * (limit + 5));
               BlockHitResult result = null;
               boolean clipBlockShape = includeNonSolid || blockState.blocksMotion();
               if (!clipBlockShape
                  && ToolManager.isToolActive()
                  && ToolManager.getCurrentTool() instanceof AnnotationTool
                  && blockState.hasProperty(BlockStateProperties.LAYERS)) {
                  clipBlockShape = true;
               }

               if (clipBlockShape) {
                  VoxelShape voxelShape = blockState.getShape(level, mutableBlockPos, collisionContext);
                  if (voxelShape != Shapes.empty() && !voxelShape.isEmpty()) {
                     result = AABB.clip(voxelShape.toAabbs(), fromVec3, toVec3, mutableBlockPos);
                     if (result != null && isInsideBlock) {
                        Vec3 enter = result.getLocation();
                        if (exitPosition != null && exitPosition.distanceSquared(enter.x, enter.y, enter.z) > 0.00390625) {
                           isInsideBlock = false;
                        } else {
                           BlockHitResult reverseResult = AABB.clip(voxelShape.toAabbs(), toVec3, fromVec3, mutableBlockPos);
                           if (reverseResult != null) {
                              Vec3 exit = reverseResult.getLocation();
                              if (exitPosition == null) {
                                 exitPosition = new Vector3d(exit.x, exit.y, exit.z);
                              } else {
                                 exitPosition.set(exit.x, exit.y, exit.z);
                              }

                              result = null;
                           } else {
                              isInsideBlock = false;
                           }
                        }
                     }
                  } else {
                     isInsideBlock = false;
                  }
               }

               if (includeFluids) {
                  FluidState fluidState = blockState.getFluidState();
                  if (!fluidState.isEmpty()) {
                     VoxelShape fluidVoxelShape = fluidState.getShape(level, mutableBlockPos);
                     if (fluidVoxelShape != Shapes.empty() && !fluidVoxelShape.isEmpty()) {
                        isInsideBlock = false;
                        BlockHitResult fluidBlockHitResult = AABB.clip(fluidVoxelShape.toAabbs(), fromVec3, toVec3, mutableBlockPos);
                        if (result == null || result.getType() != Type.BLOCK) {
                           result = fluidBlockHitResult;
                        } else if (fluidBlockHitResult != null && fluidBlockHitResult.getType() == Type.BLOCK) {
                           double blockDistance = fromVec3.distanceToSqr(result.getLocation());
                           double fluidDistance = fromVec3.distanceToSqr(fluidBlockHitResult.getLocation());
                           if (fluidDistance < blockDistance) {
                              result = fluidBlockHitResult;
                           }
                        }
                     }
                  }
               }

               if (result != null && result.getType() == Type.BLOCK) {
                  return new RayCaster.RaycastResult(result.getBlockPos(), result.getLocation(), result.getDirection(), false);
               }
            }
         }

         return null;
      }
   }

   public static void ddaSkip(Vector3d from, Vector3d to, Level level, boolean skipNonSolid, TriIntConsumer consumer) {
      if (from.y < level.getMinBuildHeight()) {
         Vector3d direction = to.sub(from, new Vector3d()).normalize();
         double yTarget = level.getMinBuildHeight() + 0.01;
         if (direction.y <= 0.0) {
            return;
         }

         double steps = (yTarget - from.y) / direction.y;
         from.x = from.x + direction.x * steps;
         from.y = yTarget;
         from.z = from.z + direction.z * steps;
      } else if (from.y >= level.getMaxBuildHeight() - 1 + 1) {
         Vector3d direction = to.sub(from, new Vector3d()).normalize();
         double yTarget = level.getMaxBuildHeight() - 1 + 1 - 0.01;
         if (direction.y >= 0.0) {
            return;
         }

         double steps = (yTarget - from.y) / direction.y;
         from.x = from.x + direction.x * steps;
         from.y = yTarget;
         from.z = from.z + direction.z * steps;
      }

      int fromMapX = (int)Math.floor(from.x);
      int fromMapY = (int)Math.floor(from.y);
      int fromMapZ = (int)Math.floor(from.z);
      int toMapX = (int)Math.floor(to.x);
      int toMapY = (int)Math.floor(to.y);
      int toMapZ = (int)Math.floor(to.z);
      if (toMapX != fromMapX || toMapY != fromMapY || toMapZ != fromMapZ) {
         Vector3d ray = to.sub(from, new Vector3d()).normalize();
         int mapX = fromMapX;
         int mapY = fromMapY;
         int mapZ = fromMapZ;
         double deltaDistX = Math.abs(1.0 / ray.x);
         double deltaDistY = Math.abs(1.0 / ray.y);
         double deltaDistZ = Math.abs(1.0 / ray.z);
         int stepX = ray.x() < 0.0 ? -1 : 1;
         int stepY = ray.y() < 0.0 ? -1 : 1;
         int stepZ = ray.z() < 0.0 ? -1 : 1;
         double sideDistX = (ray.x > 0.0 ? 1.0 - from.x + fromMapX : from.x - fromMapX) * deltaDistX;
         double sideDistY = (ray.y > 0.0 ? 1.0 - from.y + fromMapY : from.y - fromMapY) * deltaDistY;
         double sideDistZ = (ray.z > 0.0 ? 1.0 - from.z + fromMapZ : from.z - fromMapZ) * deltaDistZ;
         if (Double.isNaN(sideDistX)) {
            sideDistX = Double.POSITIVE_INFINITY;
         }

         if (Double.isNaN(sideDistY)) {
            sideDistY = Double.POSITIVE_INFINITY;
         }

         if (Double.isNaN(sideDistZ)) {
            sideDistZ = Double.POSITIVE_INFINITY;
         }

         int oldChunkX = fromMapX >> 4;
         int oldChunkY = fromMapY >> 4;
         int oldChunkZ = fromMapZ >> 4;
         LevelChunk chunk = (LevelChunk)level.getChunk(oldChunkX, oldChunkZ, ChunkStatus.FULL, false);
         if (chunk != null) {
            int sectionIndex = chunk.getSectionIndexFromSectionY(oldChunkY);
            LevelChunkSection section = chunk.getSection(sectionIndex);
            PalettedContainer<BlockState> container = section.getStates();
            BlockState blockState = (BlockState)container.get(fromMapX & 15, fromMapY & 15, fromMapZ & 15);
            if (blockState.blocksMotion() == skipNonSolid) {
               consumer.accept(fromMapX, fromMapY, fromMapZ);
            }

            while (true) {
               if (sideDistZ < sideDistX && sideDistZ < sideDistY) {
                  sideDistZ += deltaDistZ;
                  mapZ += stepZ;
               } else if (sideDistX < sideDistY) {
                  sideDistX += deltaDistX;
                  mapX += stepX;
               } else {
                  sideDistY += deltaDistY;
                  mapY += stepY;
               }

               if (mapX * stepX > toMapX * stepX || mapY * stepY > toMapY * stepY || mapZ * stepZ > toMapZ * stepZ) {
                  return;
               }

               int newChunkX = mapX >> 4;
               int newChunkY = mapY >> 4;
               int newChunkZ = mapZ >> 4;
               boolean updateSection = newChunkY != oldChunkY;
               if (newChunkX != oldChunkX || newChunkZ != oldChunkZ) {
                  oldChunkX = newChunkX;
                  oldChunkZ = newChunkZ;
                  chunk = (LevelChunk)level.getChunk(newChunkX, newChunkZ, ChunkStatus.FULL, false);
                  if (chunk == null) {
                     return;
                  }

                  updateSection = true;
               }

               if (updateSection) {
                  oldChunkY = newChunkY;
                  if (newChunkY < chunk.getMinSection() || newChunkY > chunk.getMaxSection() - 1) {
                     return;
                  }

                  sectionIndex = chunk.getSectionIndexFromSectionY(newChunkY);
                  section = chunk.getSection(sectionIndex);
                  container = section.getStates();
               }

               blockState = (BlockState)container.get(mapX & 15, mapY & 15, mapZ & 15);
               if (blockState.blocksMotion() == skipNonSolid) {
                  consumer.accept(mapX, mapY, mapZ);
               }
            }
         }
      }
   }

   public record RaycastResult(BlockPos blockPos, Vec3 worldPos, Direction direction, boolean isSelection) {
      public RaycastResult(BlockPos blockPos, Vec3 worldPos, Direction direction, boolean isSelection) {
         blockPos = blockPos.immutable();
         this.blockPos = blockPos;
         this.worldPos = worldPos;
         this.direction = direction;
         this.isSelection = isSelection;
      }

      public Vec3 getPositionWithinBlock() {
         double x = Math.max(this.blockPos.getX() + 0.01, Math.min(this.blockPos.getX() + 0.99, this.worldPos.x));
         double y = Math.max(this.blockPos.getY() + 0.01, Math.min(this.blockPos.getY() + 0.99, this.worldPos.y));
         double z = Math.max(this.blockPos.getZ() + 0.01, Math.min(this.blockPos.getZ() + 0.99, this.worldPos.z));
         return new Vec3(x, y, z);
      }

      public BlockPos getBlockPos() {
         return this.blockPos();
      }

      public Direction getDirection() {
         return this.direction();
      }

      public Vec3 getLocation() {
         return this.worldPos();
      }
   }
}
