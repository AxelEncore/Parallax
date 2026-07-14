package com.moulberry.axiom.tools.modelling;

import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.tools.path.CatmullRomSpline;
import com.moulberry.axiom.utils.BezierOperator;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;

public class GridSurface {
   private static int calculateMinSubdivisionCount(List<List<BlockPos>> grid) {
      int count = grid.size();

      for (List<BlockPos> line : grid) {
         count = Math.max(line.size(), count);
      }

      if (count <= 4) {
         return count;
      } else {
         double log = Math.log(count) / Math.log(2.0);
         return Math.max(4, (int)Math.ceil(log));
      }
   }

   public static void calculateBezier(List<List<BlockPos>> grid, TriIntConsumer consumer) {
      BezierOperator[] factorsU = new BezierOperator[grid.size()];
      BezierOperator[][] factorsV = new BezierOperator[grid.size()][];

      for (int i = 0; i < grid.size(); i++) {
         factorsU[i] = new BezierOperator(i, grid.size());
         int gizmoLineCount = grid.get(i).size();
         factorsV[i] = new BezierOperator[gizmoLineCount];

         for (int i1 = 0; i1 < gizmoLineCount; i1++) {
            factorsV[i][i1] = new BezierOperator(i1, gizmoLineCount);
         }
      }

      Vector3d pos00 = new Vector3d();
      Vector3d pos01 = new Vector3d();
      Vector3d pos10 = new Vector3d();
      Vector3d pos11 = new Vector3d();

      for (int i = 0; i < grid.size(); i++) {
         List<BlockPos> gizmoLine = grid.get(i);
         double factorU0 = factorsU[i].applyAsDouble(0.0);
         double factorU1 = factorsU[i].applyAsDouble(1.0);

         for (int j = 0; j < gizmoLine.size(); j++) {
            double factorV0 = factorsV[i][j].applyAsDouble(0.0);
            double factorV1 = factorsV[i][j].applyAsDouble(1.0);
            BlockPos pos = gizmoLine.get(j);
            pos00.add(pos.getX() * factorU0 * factorV0, pos.getY() * factorU0 * factorV0, pos.getZ() * factorU0 * factorV0);
            pos01.add(pos.getX() * factorU0 * factorV1, pos.getY() * factorU0 * factorV1, pos.getZ() * factorU0 * factorV1);
            pos10.add(pos.getX() * factorU1 * factorV0, pos.getY() * factorU1 * factorV0, pos.getZ() * factorU1 * factorV0);
            pos11.add(pos.getX() * factorU1 * factorV1, pos.getY() * factorU1 * factorV1, pos.getZ() * factorU1 * factorV1);
         }
      }

      int remainingSubdivisions = calculateMinSubdivisionCount(grid);
      surfaceBezier(pos00, pos01, pos10, pos11, 0.0, 1.0, 0.0, 1.0, factorsU, factorsV, grid, consumer, new Vector3d(), new Vector3d(), remainingSubdivisions);
   }

   private static void surfaceBezier(
      Vector3d pos00,
      Vector3d pos01,
      Vector3d pos10,
      Vector3d pos11,
      double u0,
      double u1,
      double v0,
      double v1,
      BezierOperator[] factorsU,
      BezierOperator[][] factorsV,
      List<List<BlockPos>> grid,
      TriIntConsumer consumer,
      Vector3d from,
      Vector3d to,
      int remainingSubdivisions
   ) {
      if (remainingSubdivisions <= 0) {
         double maximumDistance = 0.0;
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos01));
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos10));
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos11));
         maximumDistance = Math.max(maximumDistance, pos11.distanceSquared(pos10));
         maximumDistance = Math.max(maximumDistance, pos11.distanceSquared(pos01));
         maximumDistance = Math.max(maximumDistance, pos10.distanceSquared(pos01));
         if (maximumDistance <= 4.0) {
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos10).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos11).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos11).add(0.5, 0.5, 0.5), to.set(pos10).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos11).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos10).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            return;
         }
      } else {
         remainingSubdivisions--;
      }

      double midU = (u0 + u1) / 2.0;
      double midV = (v0 + v1) / 2.0;
      Vector3d zeroUMidV = new Vector3d();
      Vector3d midUZeroV = new Vector3d();
      Vector3d oneUMidV = new Vector3d();
      Vector3d midUoneV = new Vector3d();
      Vector3d midUmidV = new Vector3d();

      for (int i = 0; i < grid.size(); i++) {
         List<BlockPos> gizmoLine = grid.get(i);
         double factorU0 = factorsU[i].applyAsDouble(u0);
         double factorMidU = factorsU[i].applyAsDouble(midU);
         double factorU1 = factorsU[i].applyAsDouble(u1);

         for (int j = 0; j < gizmoLine.size(); j++) {
            double factorV0 = factorsV[i][j].applyAsDouble(v0);
            double factorMidV = factorsV[i][j].applyAsDouble(midV);
            double factorV1 = factorsV[i][j].applyAsDouble(v1);
            BlockPos pos = gizmoLine.get(j);
            zeroUMidV.add(pos.getX() * factorU0 * factorMidV, pos.getY() * factorU0 * factorMidV, pos.getZ() * factorU0 * factorMidV);
            midUZeroV.add(pos.getX() * factorMidU * factorV0, pos.getY() * factorMidU * factorV0, pos.getZ() * factorMidU * factorV0);
            oneUMidV.add(pos.getX() * factorU1 * factorMidV, pos.getY() * factorU1 * factorMidV, pos.getZ() * factorU1 * factorMidV);
            midUoneV.add(pos.getX() * factorMidU * factorV1, pos.getY() * factorMidU * factorV1, pos.getZ() * factorMidU * factorV1);
            midUmidV.add(pos.getX() * factorMidU * factorMidV, pos.getY() * factorMidU * factorMidV, pos.getZ() * factorMidU * factorMidV);
         }
      }

      surfaceBezier(pos00, zeroUMidV, midUZeroV, midUmidV, u0, midU, v0, midV, factorsU, factorsV, grid, consumer, from, to, remainingSubdivisions);
      surfaceBezier(midUZeroV, midUmidV, pos10, oneUMidV, midU, u1, v0, midV, factorsU, factorsV, grid, consumer, from, to, remainingSubdivisions);
      surfaceBezier(zeroUMidV, pos01, midUmidV, midUoneV, u0, midU, midV, v1, factorsU, factorsV, grid, consumer, from, to, remainingSubdivisions);
      surfaceBezier(midUmidV, midUoneV, oneUMidV, pos11, midU, u1, midV, v1, factorsU, factorsV, grid, consumer, from, to, remainingSubdivisions);
   }

   public static void calculateCatmullRom(List<List<BlockPos>> grid, TriIntConsumer consumer) {
      Vector3d pos00 = catmullRom2D(0.0, 0.0, grid);
      Vector3d pos01 = catmullRom2D(0.0, 1.0, grid);
      Vector3d pos10 = catmullRom2D(1.0, 0.0, grid);
      Vector3d pos11 = catmullRom2D(1.0, 1.0, grid);
      int remainingSubdivisions = calculateMinSubdivisionCount(grid);
      surfaceCatmullRom(pos00, pos01, pos10, pos11, 0.0, 1.0, 0.0, 1.0, grid, consumer, new Vector3d(), new Vector3d(), remainingSubdivisions);
   }

   private static Vector3d catmullRom2D(double u, double v, List<List<BlockPos>> grid) {
      double partialV = v * (grid.size() - 1);
      int floorPartialV = (int)partialV;
      double remainderPartialV = partialV - floorPartialV;
      int indexV0 = Math.max(0, floorPartialV - 1);
      int indexV2 = Math.min(grid.size() - 1, floorPartialV + 1);
      int indexV3 = Math.min(grid.size() - 1, floorPartialV + 2);
      if (floorPartialV == indexV2) {
         return catmullRom1D(u, grid.get(floorPartialV));
      } else {
         Vector3d position1 = catmullRom1D(u, grid.get(floorPartialV));
         Vector3d position2 = catmullRom1D(u, grid.get(indexV2));
         Vector3d position0 = indexV0 == floorPartialV ? position1 : catmullRom1D(u, grid.get(indexV0));
         Vector3d position3 = indexV3 == indexV2 ? position2 : catmullRom1D(u, grid.get(indexV3));
         return CatmullRomSpline.position(position0, position1, position2, position3, (float)remainderPartialV);
      }
   }

   private static Vector3d catmullRom1D(double u, List<BlockPos> line) {
      double partialU = u * (line.size() - 1);
      int floorPartialU = (int)partialU;
      double remainderPartialU = partialU - floorPartialU;
      int indexU0 = Math.max(0, floorPartialU - 1);
      int indexU2 = Math.min(line.size() - 1, floorPartialU + 1);
      int indexU3 = Math.min(line.size() - 1, floorPartialU + 2);
      BlockPos blockPos1 = line.get(floorPartialU);
      Vector3d position1 = new Vector3d(blockPos1.getX(), blockPos1.getY(), blockPos1.getZ());
      if (floorPartialU == indexU2) {
         return position1;
      } else {
         BlockPos blockPos0 = line.get(indexU0);
         Vector3d position0 = new Vector3d(blockPos0.getX(), blockPos0.getY(), blockPos0.getZ());
         BlockPos blockPos2 = line.get(indexU2);
         Vector3d position2 = new Vector3d(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ());
         BlockPos blockPos3 = line.get(indexU3);
         Vector3d position3 = new Vector3d(blockPos3.getX(), blockPos3.getY(), blockPos3.getZ());
         return CatmullRomSpline.position(position0, position1, position2, position3, (float)remainderPartialU);
      }
   }

   private static void surfaceCatmullRom(
      Vector3d pos00,
      Vector3d pos01,
      Vector3d pos10,
      Vector3d pos11,
      double u0,
      double u1,
      double v0,
      double v1,
      List<List<BlockPos>> grid,
      TriIntConsumer consumer,
      Vector3d from,
      Vector3d to,
      int remainingSubdivisions
   ) {
      if (remainingSubdivisions <= 0) {
         double maximumDistance = 0.0;
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos01));
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos10));
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos11));
         maximumDistance = Math.max(maximumDistance, pos11.distanceSquared(pos10));
         maximumDistance = Math.max(maximumDistance, pos11.distanceSquared(pos01));
         maximumDistance = Math.max(maximumDistance, pos10.distanceSquared(pos01));
         if (maximumDistance <= 4.0) {
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos10).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos11).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos11).add(0.5, 0.5, 0.5), to.set(pos10).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos11).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos10).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            return;
         }
      } else {
         remainingSubdivisions--;
      }

      double midU = (u0 + u1) / 2.0;
      double midV = (v0 + v1) / 2.0;
      Vector3d zeroUMidV = catmullRom2D(u0, midV, grid);
      Vector3d midUZeroV = catmullRom2D(midU, v0, grid);
      Vector3d oneUMidV = catmullRom2D(u1, midV, grid);
      Vector3d midUoneV = catmullRom2D(midU, v1, grid);
      Vector3d midUmidV = catmullRom2D(midU, midV, grid);
      surfaceCatmullRom(pos00, zeroUMidV, midUZeroV, midUmidV, u0, midU, v0, midV, grid, consumer, from, to, remainingSubdivisions);
      surfaceCatmullRom(midUZeroV, midUmidV, pos10, oneUMidV, midU, u1, v0, midV, grid, consumer, from, to, remainingSubdivisions);
      surfaceCatmullRom(zeroUMidV, pos01, midUmidV, midUoneV, u0, midU, midV, v1, grid, consumer, from, to, remainingSubdivisions);
      surfaceCatmullRom(midUmidV, midUoneV, oneUMidV, pos11, midU, u1, midV, v1, grid, consumer, from, to, remainingSubdivisions);
   }

   public static void calculateFlat(List<List<BlockPos>> grid, TriIntConsumer consumer) {
      Vector3d pos00 = flat2D(0.0, 0.0, grid);
      Vector3d pos01 = flat2D(0.0, 1.0, grid);
      Vector3d pos10 = flat2D(1.0, 0.0, grid);
      Vector3d pos11 = flat2D(1.0, 1.0, grid);
      int remainingSubdivisions = calculateMinSubdivisionCount(grid);
      surfaceFlat(pos00, pos01, pos10, pos11, 0.0, 1.0, 0.0, 1.0, grid, consumer, new Vector3d(), new Vector3d(), remainingSubdivisions);
   }

   private static Vector3d flat2D(double u, double v, List<List<BlockPos>> grid) {
      double partialV = v * (grid.size() - 1);
      int floorPartialV = (int)partialV;
      double remainderPartialV = partialV - floorPartialV;
      int indexV2 = Math.min(grid.size() - 1, floorPartialV + 1);
      if (floorPartialV == indexV2) {
         return flat1D(u, grid.get(floorPartialV));
      } else {
         Vector3d position1 = flat1D(u, grid.get(floorPartialV));
         Vector3d position2 = flat1D(u, grid.get(indexV2));
         return position1.lerp(position2, (float)remainderPartialV);
      }
   }

   private static Vector3d flat1D(double u, List<BlockPos> line) {
      double partialU = u * (line.size() - 1);
      int floorPartialU = (int)partialU;
      double remainderPartialU = partialU - floorPartialU;
      int indexU2 = Math.min(line.size() - 1, floorPartialU + 1);
      BlockPos blockPos1 = line.get(floorPartialU);
      Vector3d position1 = new Vector3d(blockPos1.getX(), blockPos1.getY(), blockPos1.getZ());
      if (floorPartialU == indexU2) {
         return position1;
      } else {
         BlockPos blockPos2 = line.get(indexU2);
         Vector3d position2 = new Vector3d(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ());
         return position1.lerp(position2, (float)remainderPartialU);
      }
   }

   private static void surfaceFlat(
      Vector3d pos00,
      Vector3d pos01,
      Vector3d pos10,
      Vector3d pos11,
      double u0,
      double u1,
      double v0,
      double v1,
      List<List<BlockPos>> grid,
      TriIntConsumer consumer,
      Vector3d from,
      Vector3d to,
      int remainingSubdivisions
   ) {
      if (remainingSubdivisions <= 0) {
         double maximumDistance = 0.0;
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos01));
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos10));
         maximumDistance = Math.max(maximumDistance, pos00.distanceSquared(pos11));
         maximumDistance = Math.max(maximumDistance, pos11.distanceSquared(pos10));
         maximumDistance = Math.max(maximumDistance, pos11.distanceSquared(pos01));
         maximumDistance = Math.max(maximumDistance, pos10.distanceSquared(pos01));
         if (maximumDistance <= 4.0) {
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos10).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos00).add(0.5, 0.5, 0.5), to.set(pos11).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos11).add(0.5, 0.5, 0.5), to.set(pos10).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos11).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            Rasterization3D.dda(from.set(pos10).add(0.5, 0.5, 0.5), to.set(pos01).add(0.5, 0.5, 0.5), consumer);
            return;
         }
      } else {
         remainingSubdivisions--;
      }

      double midU = (u0 + u1) / 2.0;
      double midV = (v0 + v1) / 2.0;
      Vector3d zeroUMidV = flat2D(u0, midV, grid);
      Vector3d midUZeroV = flat2D(midU, v0, grid);
      Vector3d oneUMidV = flat2D(u1, midV, grid);
      Vector3d midUoneV = flat2D(midU, v1, grid);
      Vector3d midUmidV = flat2D(midU, midV, grid);
      surfaceFlat(pos00, zeroUMidV, midUZeroV, midUmidV, u0, midU, v0, midV, grid, consumer, from, to, remainingSubdivisions);
      surfaceFlat(midUZeroV, midUmidV, pos10, oneUMidV, midU, u1, v0, midV, grid, consumer, from, to, remainingSubdivisions);
      surfaceFlat(zeroUMidV, pos01, midUmidV, midUoneV, u0, midU, midV, v1, grid, consumer, from, to, remainingSubdivisions);
      surfaceFlat(midUmidV, midUoneV, oneUMidV, pos11, midU, u1, midV, v1, grid, consumer, from, to, remainingSubdivisions);
   }
}
