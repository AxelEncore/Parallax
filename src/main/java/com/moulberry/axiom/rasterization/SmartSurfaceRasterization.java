package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import it.unimi.dsi.fastutil.ints.Int2ByteMap;
import it.unimi.dsi.fastutil.ints.Int2ByteOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector4d;

public class SmartSurfaceRasterization {
   private static final BlockState[] VISUALIZATION_BLOCKS = new BlockState[]{
      Blocks.WHITE_WOOL.defaultBlockState(),
      Blocks.ORANGE_WOOL.defaultBlockState(),
      Blocks.MAGENTA_WOOL.defaultBlockState(),
      Blocks.LIGHT_BLUE_WOOL.defaultBlockState(),
      Blocks.YELLOW_WOOL.defaultBlockState(),
      Blocks.LIME_WOOL.defaultBlockState(),
      Blocks.PINK_WOOL.defaultBlockState(),
      Blocks.GRAY_WOOL.defaultBlockState(),
      Blocks.LIGHT_GRAY_WOOL.defaultBlockState(),
      Blocks.CYAN_WOOL.defaultBlockState(),
      Blocks.PURPLE_WOOL.defaultBlockState(),
      Blocks.BLUE_WOOL.defaultBlockState(),
      Blocks.BROWN_WOOL.defaultBlockState(),
      Blocks.GREEN_WOOL.defaultBlockState(),
      Blocks.RED_WOOL.defaultBlockState(),
      Blocks.BLACK_WOOL.defaultBlockState()
   };

   public static void smartSurface(ChunkedBlockRegion region, BlockState block, BlockPos[] points, boolean visualizeMesh) {
      if (points.length != 0) {
         if (points.length == 1) {
            if (visualizeMesh) {
               region.addBlock(points[0], VISUALIZATION_BLOCKS[1]);
            } else {
               region.addBlock(points[0], block);
            }
         } else if (points.length == 2) {
            if (visualizeMesh) {
               Rasterization3D.bresenham(points[0], points[1], (x, y, z) -> region.addBlock(x, y, z, VISUALIZATION_BLOCKS[1]));
            } else {
               Rasterization3D.bresenham(points[0], points[1], (x, y, z) -> region.addBlock(x, y, z, block));
            }
         } else if (points.length == 3) {
            if (visualizeMesh) {
               Rasterization3D.triangle(points[0], points[1], points[2], (x, y, z) -> region.addBlock(x, y, z, VISUALIZATION_BLOCKS[1]));
            } else {
               Rasterization3D.triangle(points[0], points[1], points[2], (x, y, z) -> region.addBlock(x, y, z, block));
            }
         } else {
            PriorityQueue<SmartSurfaceRasterization.Triangle> triangles = new PriorityQueue<>(
               Comparator.comparingDouble(SmartSurfaceRasterization.Triangle::heuristic)
            );
            IntSet addedTriangles = new IntOpenHashSet();
            SmartSurfaceRasterization.Triangle minTriangle = null;

            for (int i = 0; i < points.length - 2; i++) {
               BlockPos one = points[i];

               for (int j = i + 1; j < points.length - 1; j++) {
                  BlockPos two = points[j];

                  for (int k = i + 2; k < points.length; k++) {
                     BlockPos three = points[k];
                     if (calculateNormal(one, two, three) != null) {
                        double heuristic = calculateHeuristic(one, two, three);
                        if (heuristic >= 0.0 && (minTriangle == null || heuristic < minTriangle.heuristic)) {
                           minTriangle = new SmartSurfaceRasterization.Triangle(i, j, k, heuristic);
                        }
                     }
                  }
               }
            }

            if (minTriangle != null) {
               triangles.add(minTriangle);
               addedTriangles.add(calculateTriangleId(minTriangle.v1, minTriangle.v2, minTriangle.v3, points.length));
               BitSet usedVertices = new BitSet();
               Int2ByteMap edgeConnections = new Int2ByteOpenHashMap();
               int remainingVertices = points.length;
               IntSet bannedTriangles = new IntOpenHashSet();
               int blockIndex = 0;
               boolean nearlyDone = false;
               List<SmartSurfaceRasterization.Triangle> failedNearlyDone = new ArrayList<>();

               while (!triangles.isEmpty()) {
                  SmartSurfaceRasterization.Triangle triangle = triangles.poll();
                  int thisTriangleId = calculateTriangleId(triangle.v1, triangle.v2, triangle.v3, points.length);
                  if (!bannedTriangles.contains(thisTriangleId)) {
                     BlockPos one = points[triangle.v1];
                     BlockPos two = points[triangle.v2];
                     BlockPos three = points[triangle.v3];
                     Vector3d normal = calculateNormal(one, two, three);
                     if (normal != null) {
                        int edge1 = triangle.v2 < triangle.v3 ? triangle.v2 + triangle.v3 * points.length : triangle.v3 + triangle.v2 * points.length;
                        int edge1Connections = edgeConnections.getOrDefault(edge1, (byte)0);
                        if (edge1Connections < 2) {
                           int edge2 = triangle.v1 < triangle.v3 ? triangle.v1 + triangle.v3 * points.length : triangle.v3 + triangle.v1 * points.length;
                           int edge2Connections = edgeConnections.getOrDefault(edge2, (byte)0);
                           if (edge2Connections < 2) {
                              int edge3 = triangle.v1 < triangle.v2 ? triangle.v1 + triangle.v2 * points.length : triangle.v2 + triangle.v1 * points.length;
                              int edge3Connections = edgeConnections.getOrDefault(edge3, (byte)0);
                              if (edge3Connections < 2) {
                                 boolean usedVert1 = usedVertices.get(triangle.v1);
                                 boolean usedVert2 = usedVertices.get(triangle.v2);
                                 boolean usedVert3 = usedVertices.get(triangle.v3);
                                 if (usedVert1 && edge2Connections == 0 && edge3Connections == 0) {
                                    failedNearlyDone.add(triangle);
                                 } else if (usedVert2 && edge1Connections == 0 && edge3Connections == 0) {
                                    failedNearlyDone.add(triangle);
                                 } else if (usedVert3 && edge1Connections == 0 && edge2Connections == 0) {
                                    failedNearlyDone.add(triangle);
                                 } else {
                                    if (nearlyDone) {
                                       boolean allow = false;
                                       if (edge1Connections + edge2Connections + edge3Connections == 2) {
                                          double angle;
                                          if (edge1Connections == 0) {
                                             angle = getAngle(one, two, three);
                                          } else if (edge2Connections == 0) {
                                             angle = getAngle(two, three, one);
                                          } else {
                                             if (edge3Connections != 0) {
                                                throw new FaultyImplementationError();
                                             }

                                             angle = getAngle(three, one, two);
                                          }

                                          if (Math.toDegrees(angle) <= 120.0) {
                                             allow = true;
                                          }
                                       }

                                       allow |= edge1Connections == 1 && edge2Connections == 1 && edge3Connections == 1;
                                       if (!allow) {
                                          failedNearlyDone.add(triangle);
                                          continue;
                                       }

                                       triangles.addAll(failedNearlyDone);
                                       failedNearlyDone.clear();
                                    }

                                    edgeConnections.put(edge1, (byte)(edge1Connections + 1));
                                    edgeConnections.put(edge2, (byte)(edge2Connections + 1));
                                    edgeConnections.put(edge3, (byte)(edge3Connections + 1));
                                    if (visualizeMesh) {
                                       if (++blockIndex >= VISUALIZATION_BLOCKS.length) {
                                          blockIndex = 0;
                                       }

                                       BlockState visualizationBlock = VISUALIZATION_BLOCKS[blockIndex];
                                       Rasterization3D.triangle(one, two, three, (x, y, z) -> region.addBlock(x, y, z, visualizationBlock));
                                    } else {
                                       Rasterization3D.triangle(one, two, three, (x, y, z) -> region.addBlock(x, y, z, block));
                                    }

                                    int best1 = -1;
                                    double heuristic1 = Double.MAX_VALUE;
                                    int best2 = -1;
                                    double heuristic2 = Double.MAX_VALUE;
                                    int best3 = -1;
                                    double heuristic3 = Double.MAX_VALUE;
                                    Vector4d edgeDirection1 = calculateEdgeDirection(two, three, one, normal);
                                    Vector4d edgeDirection2 = calculateEdgeDirection(one, three, two, normal);
                                    Vector4d edgeDirection3 = calculateEdgeDirection(one, two, three, normal);

                                    for (int kx = 0; kx < points.length; kx++) {
                                       BlockPos kPos = points[kx];
                                       if (kx != triangle.v2 && kx != triangle.v3) {
                                          int triangleId = calculateTriangleId(kx, triangle.v2, triangle.v3, points.length);
                                          if (kPos.getX() * edgeDirection1.x + kPos.getY() * edgeDirection1.y + kPos.getZ() * edgeDirection1.z
                                             < edgeDirection1.w) {
                                             bannedTriangles.add(triangleId);
                                          } else if (!nearlyDone && !addedTriangles.contains(triangleId) && calculateNormal(kPos, two, three) != null) {
                                             double heuristic = calculateHeuristic(kPos, two, three);
                                             if (heuristic >= 0.0 && heuristic < heuristic1) {
                                                heuristic1 = heuristic;
                                                best1 = kx;
                                             }
                                          }
                                       }

                                       if (kx != triangle.v1 && kx != triangle.v3) {
                                          int triangleId = calculateTriangleId(triangle.v1, kx, triangle.v3, points.length);
                                          if (kPos.getX() * edgeDirection2.x + kPos.getY() * edgeDirection2.y + kPos.getZ() * edgeDirection2.z
                                             < edgeDirection2.w) {
                                             bannedTriangles.add(triangleId);
                                          } else if (!nearlyDone && !addedTriangles.contains(triangleId) && calculateNormal(one, kPos, three) != null) {
                                             double heuristic = calculateHeuristic(one, kPos, three);
                                             if (heuristic >= 0.0 && heuristic < heuristic2) {
                                                heuristic2 = heuristic;
                                                best2 = kx;
                                             }
                                          }
                                       }

                                       if (kx != triangle.v1 && kx != triangle.v2) {
                                          int triangleId = calculateTriangleId(triangle.v1, triangle.v2, kx, points.length);
                                          if (kPos.getX() * edgeDirection3.x + kPos.getY() * edgeDirection3.y + kPos.getZ() * edgeDirection3.z
                                             < edgeDirection3.w) {
                                             bannedTriangles.add(triangleId);
                                          } else if (!nearlyDone && !addedTriangles.contains(triangleId) && calculateNormal(one, two, kPos) != null) {
                                             double heuristic = calculateHeuristic(one, two, kPos);
                                             if (heuristic >= 0.0 && heuristic < heuristic3) {
                                                heuristic3 = heuristic;
                                                best3 = kx;
                                             }
                                          }
                                       }
                                    }

                                    if (best1 >= 0) {
                                       triangles.add(new SmartSurfaceRasterization.Triangle(best1, triangle.v2, triangle.v3, heuristic1));
                                       addedTriangles.add(calculateTriangleId(best1, triangle.v2, triangle.v3, points.length));
                                    }

                                    if (best2 >= 0) {
                                       triangles.add(new SmartSurfaceRasterization.Triangle(triangle.v1, best2, triangle.v3, heuristic2));
                                       addedTriangles.add(calculateTriangleId(triangle.v1, best2, triangle.v3, points.length));
                                    }

                                    if (best3 >= 0) {
                                       triangles.add(new SmartSurfaceRasterization.Triangle(triangle.v1, triangle.v2, best3, heuristic3));
                                       addedTriangles.add(calculateTriangleId(triangle.v1, triangle.v2, best3, points.length));
                                    }

                                    if (!nearlyDone) {
                                       if (!usedVert1) {
                                          usedVertices.set(triangle.v1);
                                          remainingVertices--;
                                       }

                                       if (!usedVert2) {
                                          usedVertices.set(triangle.v2);
                                          remainingVertices--;
                                       }

                                       if (!usedVert3) {
                                          usedVertices.set(triangle.v3);
                                          remainingVertices--;
                                       }

                                       if (remainingVertices <= 0) {
                                          triangles.addAll(failedNearlyDone);
                                          nearlyDone = true;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static int calculateTriangleId(int v1, int v2, int v3, int numPoints) {
      int min1;
      int min2;
      int min3;
      if (v1 < v2 && v1 < v3) {
         min1 = v1;
         if (v2 < v3) {
            min2 = v2;
            min3 = v3;
         } else {
            min2 = v3;
            min3 = v2;
         }
      } else if (v2 < v3) {
         min1 = v2;
         if (v1 < v3) {
            min2 = v1;
            min3 = v3;
         } else {
            min2 = v3;
            min3 = v1;
         }
      } else {
         min1 = v3;
         if (v1 < v2) {
            min2 = v1;
            min3 = v2;
         } else {
            min2 = v2;
            min3 = v1;
         }
      }

      return min1 + min2 * numPoints + min3 * numPoints * numPoints;
   }

   private static double calculateHeuristic(BlockPos one, BlockPos two, BlockPos three) {
      int leftX = one.getX() - two.getX();
      int leftY = one.getY() - two.getY();
      int leftZ = one.getZ() - two.getZ();
      int rightX = one.getX() - three.getX();
      int rightY = one.getY() - three.getY();
      int rightZ = one.getZ() - three.getZ();
      double angle1 = getAngle(one, two, three);
      double angle2 = getAngle(three, one, two);
      double angle3 = getAngle(two, three, one);
      double maxAngle = Math.toDegrees(Math.max(angle1, Math.max(angle2, angle3)));
      if (maxAngle > 160.0) {
         return -1.0;
      } else {
         double multiplier;
         if (maxAngle <= 120.0) {
            multiplier = 1.0;
         } else {
            multiplier = 60.0 / (180.0 - maxAngle);
         }

         int otherX = two.getX() - three.getX();
         int otherY = two.getY() - three.getY();
         int otherZ = two.getZ() - three.getZ();
         double edgeLength1 = Math.sqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
         double edgeLength2 = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
         double edgeLength3 = Math.sqrt(otherX * otherX + otherY * otherY + otherZ * otherZ);
         double edgeLength = Math.max(edgeLength1, Math.max(edgeLength2, edgeLength3));
         return edgeLength * multiplier;
      }
   }

   private static double getAngle(BlockPos one, BlockPos two, BlockPos three) {
      int x1 = one.getX();
      int x2 = two.getX();
      int x3 = three.getX();
      int y1 = one.getY();
      int y2 = two.getY();
      int y3 = three.getY();
      int z1 = one.getZ();
      int z2 = two.getZ();
      int z3 = three.getZ();
      int num = (x2 - x1) * (x3 - x1) + (y2 - y1) * (y3 - y1) + (z2 - z1) * (z3 - z1);
      double den = Math.sqrt(Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0) + Math.pow(z2 - z1, 2.0))
         * Math.sqrt(Math.pow(x3 - x1, 2.0) + Math.pow(y3 - y1, 2.0) + Math.pow(z3 - z1, 2.0));
      return Math.acos(num / den);
   }

   @Nullable
   private static Vector3d calculateNormal(BlockPos one, BlockPos two, BlockPos three) {
      int leftX = one.getX() - two.getX();
      int leftY = one.getY() - two.getY();
      int leftZ = one.getZ() - two.getZ();
      int rightX = one.getX() - three.getX();
      int rightY = one.getY() - three.getY();
      int rightZ = one.getZ() - three.getZ();
      long normalX = (long)leftY * rightZ - (long)leftZ * rightY;
      long normalY = (long)leftZ * rightX - (long)leftX * rightZ;
      long normalZ = (long)leftX * rightY - (long)leftY * rightX;
      long normalLengthSq = normalX * normalX + normalY * normalY + normalZ * normalZ;
      if (normalLengthSq > 1.0E-5) {
         double normalLength = Math.sqrt(normalLengthSq);
         return new Vector3d(normalX / normalLength, normalY / normalLength, normalZ / normalLength);
      } else {
         return null;
      }
   }

   private static Vector4d calculateEdgeDirection(BlockPos one, BlockPos two, BlockPos three, Vector3d normal) {
      double leftX = one.getX() - two.getX();
      double leftY = one.getY() - two.getY();
      double leftZ = one.getZ() - two.getZ();
      double rightX = normal.x;
      double rightY = normal.y;
      double rightZ = normal.z;
      double crossX = leftY * rightZ - leftZ * rightY;
      double crossY = leftZ * rightX - leftX * rightZ;
      double crossZ = leftX * rightY - leftY * rightX;
      double k = one.getX() * crossX + one.getY() * crossY + one.getZ() * crossZ;
      if (three.getX() * crossX + three.getY() * crossY + three.getZ() * crossZ >= k) {
         crossX *= -1.0;
         crossY *= -1.0;
         crossZ *= -1.0;
         k = one.getX() * crossX + one.getY() * crossY + one.getZ() * crossZ;
      }

      return new Vector4d(crossX, crossY, crossZ, k);
   }

   record Triangle(int v1, int v2, int v3, double heuristic) {
   }
}
