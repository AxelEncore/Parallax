package com.moulberry.axiom.tools.ruler;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.moulberry.axiom.collections.Position2dSet;
import com.moulberry.axiom.funcinterfaces.BiIntConsumer;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public record RulerCircle(Gizmo center, Vec3i offset, Axis axis) {
   private static final Int2IntMap blockCountCache = new Int2IntOpenHashMap();

   public RulerCircle(BlockPos center, BlockPos edge, Axis axis) {
      this(new Gizmo(center), edge.subtract(center), axis);
   }

   public int radius() {
      int dx = this.offset.getX();
      int dy = this.offset.getY();
      int dz = this.offset.getZ();
      return (int)(Math.sqrt(dx * dx + dy * dy + dz * dz) + 0.35F);
   }

   public int blockCount() {
      int r = this.radius();
      if (blockCountCache.containsKey(r)) {
         return blockCountCache.get(r);
      } else {
         Position2dSet set = new Position2dSet();
         forEachBlock(r, set::add);
         int count = set.count();
         blockCountCache.put(r, count);
         return count;
      }
   }

   public void drawLineFromCenterToEdge(BufferBuilder bufferBuilder, Pose pose, Vec3 origin) {
      Vec3 from = this.center().getTargetVec();
      Vec3 to = from.add(this.offset().getX(), this.offset().getY(), this.offset().getZ());
      Shapes.line(bufferBuilder, pose, 1.0F, 1.0F, 1.0F, from.subtract(origin), to.subtract(origin));
   }

   public void drawCircle(AxiomWorldRenderContext rc, ClientLevel level, BufferBuilder bufferBuilder, int colour, Vec3 origin) {
      BlockPos centerPos = this.center().getTargetPosition();
      drawBlockOutline(rc, level, bufferBuilder, centerPos, colour, origin);
      MutableBlockPos point = new MutableBlockPos();
      forEachBlock(this.radius(), (x, y) -> {
         switch (this.axis()) {
            case X:
               point.set(centerPos.getX(), centerPos.getY() + x, centerPos.getZ() + y);
               break;
            case Y:
               point.set(centerPos.getX() + x, centerPos.getY(), centerPos.getZ() + y);
               break;
            case Z:
               point.set(centerPos.getX() + x, centerPos.getY() + y, centerPos.getZ());
         }

         drawBlockOutline(rc, level, bufferBuilder, point, colour, origin);
      });
   }

   private static void drawBlockOutline(AxiomWorldRenderContext rc, ClientLevel level, BufferBuilder bufferBuilder, BlockPos point, int colour, Vec3 origin) {
      BlockState blockState = level.getBlockState(point);
      VoxelShape voxelShape = blockState.getShape(level, point, CollisionContext.empty());
      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      matrices.translate(point.getX() - origin.x, point.getY() - origin.y, point.getZ() - origin.z);
      Shapes.blockOutline(bufferBuilder, matrices.last(), voxelShape, colour);
      matrices.popPose();
   }

   private static void forEachBlock(int r, BiIntConsumer consumer) {
      int t1 = r / 16;
      int x = r;
      int y = 0;

      while (x >= y) {
         consumer.accept(x, y);
         consumer.accept(-x, y);
         consumer.accept(x, -y);
         consumer.accept(-x, -y);
         consumer.accept(y, x);
         consumer.accept(-y, x);
         consumer.accept(y, -x);
         consumer.accept(-y, -x);
         t1 += ++y;
         int t2 = t1 - x;
         if (t2 >= 0) {
            t1 = t2;
            x--;
         }
      }
   }
}
