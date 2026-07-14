package com.moulberry.axiom.tools.generator;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.phys.Vec3;

public class TreeGeneration {
   public static ChunkedBlockRegion generateCyprus(Level level, BlockPos pos) {
      Random random = ThreadLocalRandom.current();
      int height = 10 + random.nextInt(9);
      ChunkedBlockRegion region = new ChunkedBlockRegion();

      for (int y = 0; y < height; y++) {
         region.addBlockWithoutDirty(pos.getX(), pos.getY() + y, pos.getZ(), Blocks.STRIPPED_SPRUCE_WOOD.defaultBlockState());
      }

      int leafHeight = 23 + random.nextInt(-1, 2);

      for (int y = height; y < leafHeight; y++) {
         region.addBlockWithoutDirty(pos.getX(), pos.getY() + y, pos.getZ(), Blocks.AZALEA_LEAVES.defaultBlockState());
      }

      generateCyprusLeaves(pos.offset(0, 4, 0), height, region, Blocks.AZALEA_LEAVES.defaultBlockState());
      region.addBlockWithoutDirty(pos.getX() + 1, pos.getY() + 3, pos.getZ(), Blocks.AZALEA_LEAVES.defaultBlockState());
      region.addBlockWithoutDirty(pos.getX() - 1, pos.getY() + 3, pos.getZ(), Blocks.AZALEA_LEAVES.defaultBlockState());
      region.addBlockWithoutDirty(pos.getX(), pos.getY() + 3, pos.getZ() + 1, Blocks.AZALEA_LEAVES.defaultBlockState());
      region.addBlockWithoutDirty(pos.getX(), pos.getY() + 3, pos.getZ() - 1, Blocks.AZALEA_LEAVES.defaultBlockState());
      return region;
   }

   private static void generateCyprusLeaves(BlockPos pos, int height, ChunkedBlockRegion region, BlockState blockState) {
      BinaryHeap heap = new BinaryHeap();
      Node heapNode = new Node(pos.getX(), pos.getY(), pos.getZ());
      heap.insert(heapNode);
      PositionSet visited = new PositionSet();
      visited.add(pos.getX(), pos.getY(), pos.getZ());
      PositionSet reached = new PositionSet();
      int[] offsets = new int[]{0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, -1, 0, 0, 1, 0, 0, -1, -1, 0, 0, -1, 0, 1, -1, 0, -1};
      Random random = ThreadLocalRandom.current();

      for (int i = 0; i < 14 * height && !heap.isEmpty(); i++) {
         heapNode = heap.pop();
         reached.add(heapNode.x, heapNode.y, heapNode.z);

         for (int i1 = 0; i1 < offsets.length; i1 += 3) {
            int newX = heapNode.x + offsets[i1];
            int newY = heapNode.y + offsets[i1 + 1];
            int newZ = heapNode.z + offsets[i1 + 2];
            if (!visited.contains(newX, newY, newZ)) {
               visited.add(newX, newY, newZ);
               Node newNode = new Node(newX, newY, newZ);
               float dx = pos.getX() - newX;
               float dy;
               if (newY > pos.getY() + 15) {
                  dy = 4 + (newY - (pos.getY() + 15));
               } else {
                  dy = (pos.getY() - newY) / 15.0F * 4.0F;
               }

               float dz = pos.getZ() - newZ;
               if (offsets[i1 + 1] < 0) {
                  newNode.f += 4.0F;
               }

               newNode.f = (float)(newNode.f + Math.sqrt(dx * dx * 4.0F + dy * dy + dz * dz * 4.0F));
               heap.insert(newNode);
            }
         }
      }

      reached.forEach((x, y, z) -> {
         if ((x != pos.getX() || z != pos.getZ()) && y >= pos.getY()) {
            if (y < pos.getY() + height && Math.abs(x - pos.getX()) + Math.abs(z - pos.getZ()) <= 1) {
               region.addBlockIfNotPresent(x, y, z, blockState);
               if (random.nextFloat() < 0.5) {
                  region.addBlockIfNotPresent(x, y + 1, z, blockState);
               }
            } else {
               int neighbors = 0;
               if (reached.contains(x + 1, y, z)) {
                  neighbors++;
               }

               if (reached.contains(x - 1, y, z)) {
                  neighbors++;
               }

               if (reached.contains(x, y + 1, z)) {
                  neighbors++;
               }

               if (reached.contains(x, y - 1, z)) {
                  neighbors++;
               }

               if (reached.contains(x, y, z + 1)) {
                  neighbors++;
               }

               if (reached.contains(x, y, z - 1)) {
                  neighbors++;
               }

               if (random.nextFloat() <= 0.5 + 0.5 * neighbors * neighbors / 30.0) {
                  region.addBlockIfNotPresent(x, y, z, blockState);
                  if (random.nextFloat() < 0.5) {
                     region.addBlockIfNotPresent(x, y + 1, z, blockState);
                  }
               }
            }
         } else {
            region.addBlockIfNotPresent(x, y, z, blockState);
         }
      });
   }

   public static ChunkedBlockRegion generateAsh(Level level, BlockPos pos) {
      ChunkedBlockRegion region = new ChunkedBlockRegion();
      List<Vec3> attractionPoints = new ArrayList<>();
      Random random = ThreadLocalRandom.current();
      int height = 10 + random.nextInt(17);

      for (int i = 0; i < 1000; i++) {
         float angle = random.nextFloat() * 2.0F * (float) Math.PI;
         float angleX = (float)Math.sin(angle);
         float angleZ = (float)Math.cos(angle);
         double y = random.nextFloat() * (height - 5) + 5.0F;
         double spread = (height - y) / (height / 5.0F) + 3.0;
         double randomSpread = random.nextFloat() * spread;
         attractionPoints.add(new Vec3(pos.getX() + 0.5 + angleX * randomSpread, pos.getY() + 0.5 + y, pos.getZ() + 0.5 + angleZ * randomSpread));
      }

      SpaceColonization.TreeNode root = new SpaceColonization.TreeNode(Vec3.atCenterOf(pos));
      AtomicReference<SpaceColonization.TreeNode> last = new AtomicReference<>(root);
      BlockPos end = pos.offset(random.nextInt(-1, 2), height, random.nextInt(-1, 2));
      Rasterization3D.dda(pos, end, (x, yx, z) -> {
         SpaceColonization.TreeNode newNode = new SpaceColonization.TreeNode(new Vec3(x + 0.5, yx + 0.5, z + 0.5));
         last.get().children.add(newNode);
         last.set(newNode);
      });
      SpaceColonization.colonize(root, attractionPoints, 3.0F, 6.0F);
      root.generate(region, Blocks.DARK_OAK_WOOD.defaultBlockState());
      generateAshLeavesAtBranches(root, pos, height, region);
      generateAshLeaves(last.get(), pos, height, region);
      generateAshStump(level, pos, region, Blocks.DARK_OAK_WOOD.defaultBlockState());
      return region;
   }

   private static boolean removeBranchesBelowLength(SpaceColonization.TreeNode node, int currentLength, int threshold) {
      if (node.children.isEmpty()) {
         return currentLength < threshold;
      } else if (node.children.size() == 1) {
         return removeBranchesBelowLength(node.children.get(0), currentLength + 1, threshold);
      } else {
         node.children.removeIf(child -> removeBranchesBelowLength(child, 0, threshold));
         return false;
      }
   }

   private static void generateAshLeavesAtBranches(SpaceColonization.TreeNode node, BlockPos rootPosition, int height, ChunkedBlockRegion region) {
      if (node.children.isEmpty()) {
         generateAshLeaves(node, rootPosition, height, region);
      } else {
         for (SpaceColonization.TreeNode child : node.children) {
            generateAshLeavesAtBranches(child, rootPosition, height, region);
         }
      }
   }

   private static void generateAshStump(Level level, BlockPos pos, ChunkedBlockRegion region, BlockState blockState) {
      BinaryHeap heap = new BinaryHeap();
      Node heapNode = new Node(pos.getX(), pos.getY(), pos.getZ());
      heap.insert(heapNode);
      PositionSet visited = new PositionSet();
      visited.add(pos.getX(), pos.getY(), pos.getZ());
      int[] offsets = new int[]{0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, -1, 0, 0, 1, 0, 0, -1, -1, 0, 0, -1, 0, 1, -1, 0, -1};
      Random random = ThreadLocalRandom.current();

      for (int i = 0; i < 8 && !heap.isEmpty(); i++) {
         heapNode = heap.pop();
         region.addBlockIfNotPresent(heapNode.x, heapNode.y, heapNode.z, blockState);

         for (int i1 = 0; i1 < offsets.length; i1 += 3) {
            int newX = heapNode.x + offsets[i1];
            int newY = heapNode.y + offsets[i1 + 1];
            int newZ = heapNode.z + offsets[i1 + 2];
            if ((offsets[i1 + 1] != 0 || level.getBlockState(new BlockPos(newX, newY - 1, newZ)).blocksMotion()) && !visited.contains(newX, newY, newZ)) {
               visited.add(newX, newY, newZ);
               Node newNode = new Node(newX, newY, newZ);
               float dx = pos.getX() - newX;
               float dy = pos.getY() - newY;
               float dz = pos.getZ() - newZ;
               newNode.f = (float)(newNode.f + Math.sqrt(dx * dx * 4.0F + dy * dy + dz * dz * 4.0F));
               newNode.f = (float)(newNode.f + random.nextGaussian());
               heap.insert(newNode);
            }
         }
      }
   }

   private static void generateAshLeaves(SpaceColonization.TreeNode node, BlockPos rootPosition, int height, ChunkedBlockRegion region) {
      BinaryHeap heap = new BinaryHeap();
      BlockPos pos = BlockPos.containing(node.position);
      Node leafNode = new Node(pos.getX(), pos.getY(), pos.getZ());
      heap.insert(leafNode);
      PositionSet visited = new PositionSet();
      visited.add(pos.getX(), pos.getY(), pos.getZ());
      Direction[] directions = Direction.values();
      Random random = ThreadLocalRandom.current();
      int leaves = 32 + random.nextInt(32);
      leaves += 32 * (height - (pos.getY() - rootPosition.getY())) / height;

      for (int i = 0; i < leaves && !heap.isEmpty(); i++) {
         leafNode = heap.pop();
         region.addBlockIfNotPresent(leafNode.x, leafNode.y, leafNode.z, Blocks.OAK_LEAVES.defaultBlockState());

         for (Direction direction : directions) {
            int newX = leafNode.x + direction.getStepX();
            int newY = leafNode.y + direction.getStepY();
            int newZ = leafNode.z + direction.getStepZ();
            if (!visited.contains(newX, newY, newZ)) {
               visited.add(newX, newY, newZ);
               Node newNode = new Node(newX, newY, newZ);
               float dx = pos.getX() - newX;
               float dy = pos.getY() - newY;
               float dz = pos.getZ() - newZ;
               newNode.f = (float)(newNode.f + Math.sqrt(dx * dx + dy * dy * 3.0F + dz * dz));
               newNode.f = (float)(newNode.f + random.nextGaussian() * 0.75);
               int y = leafNode.y - rootPosition.getY();
               double spread = (height - y) / (height / 5.0F) + 3.0F;
               double distanceToCenterX = leafNode.x - rootPosition.getX();
               double distanceToCenterZ = leafNode.z - rootPosition.getZ();
               double distanceToCenter = Math.sqrt(distanceToCenterX * distanceToCenterX + distanceToCenterZ * distanceToCenterZ);
               newNode.f = (float)(newNode.f + Math.abs(distanceToCenter - spread) * (random.nextFloat() + 0.5));
               heap.insert(newNode);
            }
         }
      }
   }
}
