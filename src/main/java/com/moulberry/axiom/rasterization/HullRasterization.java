package com.moulberry.axiom.rasterization;

import com.github.quickhull3d.QuickHull3D;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2d;
import org.joml.Vector3d;

public class HullRasterization {
   public static void quickHullBlockRegion(ChunkedBlockRegion region, BlockState block, double[] points) {
      if (points.length % 3 != 0) {
         throw new IllegalArgumentException();
      } else if (points.length != 0) {
         if (points.length == 3) {
            region.addBlockWithoutDirty((int)Math.round(points[0]), (int)Math.round(points[1]), (int)Math.round(points[2]), block);
         } else if (points.length == 6) {
            BlockPos from = new BlockPos((int)Math.round(points[0]), (int)Math.round(points[1]), (int)Math.round(points[2]));
            BlockPos to = new BlockPos((int)Math.round(points[3]), (int)Math.round(points[4]), (int)Math.round(points[5]));
            Rasterization3D.bresenham(from, to, (xx, yx, zx) -> region.addBlockWithoutDirty(xx, yx, zx, block));
         } else if (points.length == 9) {
            if (!coplanarBlockRegion(region, block, points)) {
               BlockPos from = new BlockPos((int)Math.round(points[0]), (int)Math.round(points[1]), (int)Math.round(points[2]));

               for (int i = 3; i < points.length; i += 3) {
                  BlockPos to = new BlockPos((int)Math.round(points[i]), (int)Math.round(points[i + 1]), (int)Math.round(points[i + 2]));
                  Rasterization3D.bresenham(from, to, (xx, yx, zx) -> region.addBlockWithoutDirty(xx, yx, zx, block));
                  from = to;
               }
            }
         } else {
            QuickHull3D hull = new QuickHull3D();

            try {
               hull.build(points);
            } catch (IllegalArgumentException var33) {
               if (var33.getMessage().contains("coplanar") && coplanarBlockRegion(region, block, points)) {
                  return;
               }

               BlockPos from = new BlockPos((int)Math.round(points[0]), (int)Math.round(points[1]), (int)Math.round(points[2]));

               for (int i = 3; i < points.length; i += 3) {
                  BlockPos to = new BlockPos((int)Math.round(points[i]), (int)Math.round(points[i + 1]), (int)Math.round(points[i + 2]));
                  Rasterization3D.bresenham(from, to, (xx, yx, zx) -> region.addBlockWithoutDirty(xx, yx, zx, block));
                  from = to;
               }

               return;
            } catch (Exception var34) {
               return;
            }

            int numVertices = hull.getNumVertices();
            double[] vertices = new double[numVertices * 3];
            hull.getVertices(vertices);
            int[][] faceIndices = hull.getFaces();
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (int i = 0; i < numVertices; i++) {
               int x = (int)vertices[i * 3];
               int y = (int)vertices[i * 3 + 1];
               int z = (int)vertices[i * 3 + 2];
               minX = Math.min(minX, x);
               minY = Math.min(minY, y);
               minZ = Math.min(minZ, z);
               maxX = Math.max(maxX, x);
               maxY = Math.max(maxY, y);
               maxZ = Math.max(maxZ, z);
            }

            if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
               int offsetX = (maxX + minX) / 2;
               int offsetY = (maxY + minY) / 2;
               int offsetZ = (maxZ + minZ) / 2;
               minX -= offsetX;
               minY -= offsetY;
               minZ -= offsetZ;
               maxX -= offsetX;
               maxY -= offsetY;
               maxZ -= offsetZ;

               for (int i = 0; i < vertices.length; i += 3) {
                  vertices[i] -= offsetX;
                  vertices[i + 1] = vertices[i + 1] - offsetY;
                  vertices[i + 2] = vertices[i + 2] - offsetZ;
               }

               int numFaces = faceIndices.length;
               DoubleList planes = new DoubleArrayList(4 * numFaces);

               for (int i = 0; i < faceIndices.length; i++) {
                  int[] face = faceIndices[i];
                  int vert0 = face[0];
                  int vert1 = face[1];
                  int vert2 = face[2];
                  double x = vertices[vert0 * 3];
                  double y = vertices[vert0 * 3 + 1];
                  double z = vertices[vert0 * 3 + 2];
                  Vector3d vector1 = new Vector3d(x - vertices[vert1 * 3], y - vertices[vert1 * 3 + 1], z - vertices[vert1 * 3 + 2]);
                  Vector3d vector2 = new Vector3d(x - vertices[vert2 * 3], y - vertices[vert2 * 3 + 1], z - vertices[vert2 * 3 + 2]);
                  vector1.cross(vector2);
                  vector1.normalize();
                  double k = vector1.x * x + vector1.y * y + vector1.z * z;
                  planes.add(vector1.x);
                  planes.add(vector1.y);
                  planes.add(vector1.z);
                  planes.add(k);
               }

               add2dConstraints(planes, vertices, 0, true);
               add2dConstraints(planes, vertices, 1, true);
               add2dConstraints(planes, vertices, 2, true);
               double[] planesArray = planes.toDoubleArray();

               for (int x = minX; x <= maxX; x++) {
                  for (int y = minY; y <= maxY; y++) {
                     label110:
                     for (int z = minZ; z <= maxZ; z++) {
                        for (int j = 0; j < planesArray.length; j += 4) {
                           if (x * planesArray[j] + y * planesArray[j + 1] + z * planesArray[j + 2] > planesArray[j + 3] + 0.5) {
                              continue label110;
                           }
                        }

                        region.addBlockWithoutDirty(x + offsetX, y + offsetY, z + offsetZ, block);
                     }
                  }
               }
            }
         }
      }
   }

   private static void add2dConstraints(DoubleList doubles, double[] vertices, int ignoreAxis, boolean make3d) {
      Vector2d[] points2d = new Vector2d[vertices.length / 3];

      for (int i = 0; i < vertices.length; i += 3) {
         if (ignoreAxis == 0) {
            points2d[i / 3] = new Vector2d(vertices[i + 1], vertices[i + 2]);
         } else if (ignoreAxis == 1) {
            points2d[i / 3] = new Vector2d(vertices[i], vertices[i + 2]);
         } else {
            if (ignoreAxis != 2) {
               throw new FaultyImplementationError();
            }

            points2d[i / 3] = new Vector2d(vertices[i], vertices[i + 1]);
         }
      }

      Vector2d[] points = GrahamScan2d.getVertices(points2d);
      if (points != null && points.length >= 3) {
         for (int ix = 0; ix < points.length; ix++) {
            Vector2d point = points[ix];
            Vector2d nextPoint = ix == points.length - 1 ? points[0] : points[ix + 1];
            double dx = point.x - nextPoint.x;
            double dy = point.y - nextPoint.y;
            double normalX = -dy;
            double invLength = 1.0 / Math.sqrt(normalX * normalX + dx * dx);
            normalX *= invLength;
            double normalY = dx * invLength;
            double k = point.x * normalX + point.y * normalY;
            if (make3d && ignoreAxis == 0) {
               doubles.add(0.0);
            }

            doubles.add(normalX);
            if (make3d && ignoreAxis == 1) {
               doubles.add(0.0);
            }

            doubles.add(normalY);
            if (make3d && ignoreAxis == 2) {
               doubles.add(0.0);
            }

            doubles.add(k);
         }
      }
   }

   private static boolean coplanarBlockRegion(ChunkedBlockRegion region, BlockState block, double[] points) {
      double normalX = 0.0;
      double normalY = 0.0;
      double normalZ = 0.0;
      boolean foundNormal = false;

      label148:
      for (int i = 0; i < points.length - 6; i += 3) {
         double oneX = points[i];
         double oneY = points[i + 1];
         double oneZ = points[i + 2];

         for (int j = i + 3; j < points.length - 3; j += 3) {
            double leftX = oneX - points[j];
            double leftY = oneY - points[j + 1];
            double leftZ = oneZ - points[j + 2];

            for (int k = i + 6; k < points.length; k += 3) {
               double rightX = oneX - points[k];
               double rightY = oneY - points[k + 1];
               double rightZ = oneZ - points[k + 2];
               normalX = leftY * rightZ - leftZ * rightY;
               normalY = leftZ * rightX - leftX * rightZ;
               normalZ = leftX * rightY - leftY * rightX;
               double dx = normalX * normalX;
               double dy = normalY * normalY;
               double dz = normalZ * normalZ;
               double normalLengthSq = dx * dx + dy * dy + dz * dz;
               if (normalLengthSq > 1.0E-5) {
                  foundNormal = true;
                  break label148;
               }
            }
         }
      }

      if (!foundNormal) {
         return false;
      } else {
         double kx = -(points[0] * normalX + points[1] * normalY + points[2] * normalZ);
         double absNormalX = Math.abs(normalX);
         double absNormalY = Math.abs(normalY);
         double absNormalZ = Math.abs(normalZ);
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;

         for (int i = 0; i < points.length; i += 3) {
            int x = (int)points[i];
            int y = (int)points[i + 1];
            int z = (int)points[i + 2];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
         }

         if (absNormalX > absNormalY && absNormalX > absNormalZ) {
            DoubleList doubleList = new DoubleArrayList();
            add2dConstraints(doubleList, points, 0, false);
            double[] edges = doubleList.toDoubleArray();

            for (int y = minY; y <= maxY; y++) {
               label117:
               for (int z = minZ; z <= maxZ; z++) {
                  for (int j = 0; j < edges.length; j += 3) {
                     if (y * edges[j] + z * edges[j + 1] > edges[j + 2] + 0.5) {
                        continue label117;
                     }
                  }

                  int x = (int)Math.round(-(y * normalY + z * normalZ + kx) / normalX);
                  region.addBlockWithoutDirty(x, y, z, block);
               }
            }
         } else if (absNormalY > absNormalZ) {
            DoubleList doubleList = new DoubleArrayList();
            add2dConstraints(doubleList, points, 1, false);
            double[] edges = doubleList.toDoubleArray();

            for (int x = minX; x <= maxX; x++) {
               label97:
               for (int z = minZ; z <= maxZ; z++) {
                  for (int jx = 0; jx < edges.length; jx += 3) {
                     if (x * edges[jx] + z * edges[jx + 1] > edges[jx + 2] + 0.5) {
                        continue label97;
                     }
                  }

                  int y = (int)Math.round(-(x * normalX + z * normalZ + kx) / normalY);
                  region.addBlockWithoutDirty(x, y, z, block);
               }
            }
         } else {
            DoubleList doubleList = new DoubleArrayList();
            add2dConstraints(doubleList, points, 2, false);
            double[] edges = doubleList.toDoubleArray();

            for (int x = minX; x <= maxX; x++) {
               label78:
               for (int y = minY; y <= maxY; y++) {
                  for (int jxx = 0; jxx < edges.length; jxx += 3) {
                     if (x * edges[jxx] + y * edges[jxx + 1] > edges[jxx + 2] + 0.5) {
                        continue label78;
                     }
                  }

                  int z = (int)Math.round(-(x * normalX + y * normalY + kx) / normalZ);
                  region.addBlockWithoutDirty(x, y, z, block);
               }
            }
         }

         return true;
      }
   }

   public static void quickHullPositionSet(PositionSet set, double[] points) {
      if (points.length % 3 != 0) {
         throw new IllegalArgumentException();
      } else if (points.length != 0) {
         if (points.length == 3) {
            set.add((int)Math.round(points[0]), (int)Math.round(points[1]), (int)Math.round(points[2]));
         } else if (points.length == 6) {
            BlockPos from = new BlockPos((int)Math.round(points[0]), (int)Math.round(points[1]), (int)Math.round(points[2]));
            BlockPos to = new BlockPos((int)Math.round(points[3]), (int)Math.round(points[4]), (int)Math.round(points[5]));
            Rasterization3D.bresenham(from, to, (xx, yx, zx) -> set.add(xx, yx, zx));
         } else if (points.length == 9) {
            if (!coplanarPositionSet(set, points)) {
               BlockPos from = new BlockPos((int)Math.round(points[0]), (int)Math.round(points[1]), (int)Math.round(points[2]));

               for (int i = 3; i < points.length; i += 3) {
                  BlockPos to = new BlockPos((int)Math.round(points[i]), (int)Math.round(points[i + 1]), (int)Math.round(points[i + 2]));
                  Rasterization3D.bresenham(from, to, (xx, yx, zx) -> set.add(xx, yx, zx));
                  from = to;
               }
            }
         } else {
            QuickHull3D hull = new QuickHull3D();

            try {
               hull.build(points);
            } catch (IllegalArgumentException var32) {
               if (var32.getMessage().contains("coplanar") && coplanarPositionSet(set, points)) {
                  return;
               }

               BlockPos from = new BlockPos((int)Math.round(points[0]), (int)Math.round(points[1]), (int)Math.round(points[2]));

               for (int i = 3; i < points.length; i += 3) {
                  BlockPos to = new BlockPos((int)Math.round(points[i]), (int)Math.round(points[i + 1]), (int)Math.round(points[i + 2]));
                  Rasterization3D.bresenham(from, to, set::add);
                  from = to;
               }

               return;
            } catch (Exception var33) {
               return;
            }

            int numVertices = hull.getNumVertices();
            double[] vertices = new double[numVertices * 3];
            hull.getVertices(vertices);
            int[][] faceIndices = hull.getFaces();
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (int i = 0; i < numVertices; i++) {
               int x = (int)vertices[i * 3];
               int y = (int)vertices[i * 3 + 1];
               int z = (int)vertices[i * 3 + 2];
               minX = Math.min(minX, x);
               minY = Math.min(minY, y);
               minZ = Math.min(minZ, z);
               maxX = Math.max(maxX, x);
               maxY = Math.max(maxY, y);
               maxZ = Math.max(maxZ, z);
            }

            if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
               int offsetX = (maxX + minX) / 2;
               int offsetY = (maxY + minY) / 2;
               int offsetZ = (maxZ + minZ) / 2;
               minX -= offsetX;
               minY -= offsetY;
               minZ -= offsetZ;
               maxX -= offsetX;
               maxY -= offsetY;
               maxZ -= offsetZ;

               for (int i = 0; i < vertices.length; i += 3) {
                  vertices[i] -= offsetX;
                  vertices[i + 1] = vertices[i + 1] - offsetY;
                  vertices[i + 2] = vertices[i + 2] - offsetZ;
               }

               int numFaces = faceIndices.length;
               DoubleList planes = new DoubleArrayList(4 * numFaces);

               for (int i = 0; i < faceIndices.length; i++) {
                  int[] face = faceIndices[i];
                  int vert0 = face[0];
                  int vert1 = face[1];
                  int vert2 = face[2];
                  double x = vertices[vert0 * 3];
                  double y = vertices[vert0 * 3 + 1];
                  double z = vertices[vert0 * 3 + 2];
                  Vector3d vector1 = new Vector3d(x - vertices[vert1 * 3], y - vertices[vert1 * 3 + 1], z - vertices[vert1 * 3 + 2]);
                  Vector3d vector2 = new Vector3d(x - vertices[vert2 * 3], y - vertices[vert2 * 3 + 1], z - vertices[vert2 * 3 + 2]);
                  vector1.cross(vector2);
                  vector1.normalize();
                  double k = vector1.x * x + vector1.y * y + vector1.z * z;
                  planes.add(vector1.x);
                  planes.add(vector1.y);
                  planes.add(vector1.z);
                  planes.add(k);
               }

               add2dConstraints(planes, vertices, 0, true);
               add2dConstraints(planes, vertices, 1, true);
               add2dConstraints(planes, vertices, 2, true);
               double[] planesArray = planes.toDoubleArray();

               for (int x = minX; x <= maxX; x++) {
                  for (int y = minY; y <= maxY; y++) {
                     label110:
                     for (int z = minZ; z <= maxZ; z++) {
                        for (int j = 0; j < planesArray.length; j += 4) {
                           if (x * planesArray[j] + y * planesArray[j + 1] + z * planesArray[j + 2] > planesArray[j + 3] + 0.5) {
                              continue label110;
                           }
                        }

                        set.add(x + offsetX, y + offsetY, z + offsetZ);
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean coplanarPositionSet(PositionSet set, double[] points) {
      double normalX = 0.0;
      double normalY = 0.0;
      double normalZ = 0.0;
      boolean foundNormal = false;

      label148:
      for (int i = 0; i < points.length - 6; i += 3) {
         double oneX = points[i];
         double oneY = points[i + 1];
         double oneZ = points[i + 2];

         for (int j = i + 3; j < points.length - 3; j += 3) {
            double leftX = oneX - points[j];
            double leftY = oneY - points[j + 1];
            double leftZ = oneZ - points[j + 2];

            for (int k = i + 6; k < points.length; k += 3) {
               double rightX = oneX - points[k];
               double rightY = oneY - points[k + 1];
               double rightZ = oneZ - points[k + 2];
               normalX = leftY * rightZ - leftZ * rightY;
               normalY = leftZ * rightX - leftX * rightZ;
               normalZ = leftX * rightY - leftY * rightX;
               double dx = normalX * normalX;
               double dy = normalY * normalY;
               double dz = normalZ * normalZ;
               double normalLengthSq = dx * dx + dy * dy + dz * dz;
               if (normalLengthSq > 1.0E-5) {
                  foundNormal = true;
                  break label148;
               }
            }
         }
      }

      if (!foundNormal) {
         return false;
      } else {
         double kx = -(points[0] * normalX + points[1] * normalY + points[2] * normalZ);
         double absNormalX = Math.abs(normalX);
         double absNormalY = Math.abs(normalY);
         double absNormalZ = Math.abs(normalZ);
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;

         for (int i = 0; i < points.length; i += 3) {
            int x = (int)points[i];
            int y = (int)points[i + 1];
            int z = (int)points[i + 2];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
         }

         if (absNormalX > absNormalY && absNormalX > absNormalZ) {
            DoubleList doubleList = new DoubleArrayList();
            add2dConstraints(doubleList, points, 0, false);
            double[] edges = doubleList.toDoubleArray();

            for (int y = minY; y <= maxY; y++) {
               label117:
               for (int z = minZ; z <= maxZ; z++) {
                  for (int j = 0; j < edges.length; j += 3) {
                     if (y * edges[j] + z * edges[j + 1] > edges[j + 2] + 0.5) {
                        continue label117;
                     }
                  }

                  int x = (int)Math.round(-(y * normalY + z * normalZ + kx) / normalX);
                  set.add(x, y, z);
               }
            }
         } else if (absNormalY > absNormalZ) {
            DoubleList doubleList = new DoubleArrayList();
            add2dConstraints(doubleList, points, 1, false);
            double[] edges = doubleList.toDoubleArray();

            for (int x = minX; x <= maxX; x++) {
               label97:
               for (int z = minZ; z <= maxZ; z++) {
                  for (int jx = 0; jx < edges.length; jx += 3) {
                     if (x * edges[jx] + z * edges[jx + 1] > edges[jx + 2] + 0.5) {
                        continue label97;
                     }
                  }

                  int y = (int)Math.round(-(x * normalX + z * normalZ + kx) / normalY);
                  set.add(x, y, z);
               }
            }
         } else {
            DoubleList doubleList = new DoubleArrayList();
            add2dConstraints(doubleList, points, 2, false);
            double[] edges = doubleList.toDoubleArray();

            for (int x = minX; x <= maxX; x++) {
               label78:
               for (int y = minY; y <= maxY; y++) {
                  for (int jxx = 0; jxx < edges.length; jxx += 3) {
                     if (x * edges[jxx] + y * edges[jxx + 1] > edges[jxx + 2] + 0.5) {
                        continue label78;
                     }
                  }

                  int z = (int)Math.round(-(x * normalX + y * normalY + kx) / normalZ);
                  set.add(x, y, z);
               }
            }
         }

         return true;
      }
   }
}
