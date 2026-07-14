package com.moulberry.axiom.scaling;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import net.minecraft.world.level.block.state.BlockState;

public class Scale2x {
   private static boolean isSolid(BlockState blockState) {
      return !blockState.canBeReplaced();
   }

   private static boolean isSolidOrNonSolid(BlockState blockState, boolean solid) {
      return !blockState.isAir() && blockState.canBeReplaced() != solid;
   }

   public static ChunkedBlockRegion scale2x(ChunkedBlockRegion in, boolean postProcessing) {
      PositionSet solid = new PositionSet();
      PositionSet nonSolid = new PositionSet();
      in.forEachEntry(
         (x, y, z, block) -> {
            if (!block.isAir()) {
               boolean isSolid = isSolid(block);
               PositionSet set = isSolid ? solid : nonSolid;
               boolean plusXAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x + 1, y, z), isSolid);
               boolean plusYAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x, y + 1, z), isSolid);
               boolean plusZAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x, y, z + 1), isSolid);
               boolean minusXAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x - 1, y, z), isSolid);
               boolean minusYAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x, y - 1, z), isSolid);
               boolean minusZAir = !isSolidOrNonSolid(in.getBlockStateOrAir(x, y, z - 1), isSolid);
               set.add(x * 2, y * 2, z * 2);
               set.add(x * 2 + 1, y * 2, z * 2);
               set.add(x * 2, y * 2 + 1, z * 2);
               set.add(x * 2 + 1, y * 2 + 1, z * 2);
               set.add(x * 2, y * 2, z * 2 + 1);
               set.add(x * 2 + 1, y * 2, z * 2 + 1);
               set.add(x * 2, y * 2 + 1, z * 2 + 1);
               set.add(x * 2 + 1, y * 2 + 1, z * 2 + 1);
               if (plusXAir || plusYAir || plusZAir || minusXAir || minusYAir || minusZAir) {
                  for (int i = 0; i <= 8; i++) {
                     int xo = i & 1;
                     int yo = (i & 2) >> 1;
                     int zo = (i & 4) >> 2;
                     int bxo = xo * 2 - 1;
                     int byo = yo * 2 - 1;
                     int bzo = zo * 2 - 1;
                     boolean xAir = xo == 0 ? minusXAir : plusXAir;
                     boolean yAir = yo == 0 ? minusYAir : plusYAir;
                     boolean zAir = zo == 0 ? minusZAir : plusZAir;
                     if (xAir
                        && yAir
                        && zAir
                        && isSolidOrNonSolid(in.getBlockStateOrAir(x + bxo, y + byo, z + bzo), isSolid)
                        && !isSolidOrNonSolid(in.getBlockStateOrAir(x, y + byo, z + bzo), isSolid)
                        && !isSolidOrNonSolid(in.getBlockStateOrAir(x + bxo, y, z + bzo), isSolid)
                        && !isSolidOrNonSolid(in.getBlockStateOrAir(x + bxo, y + byo, z), isSolid)) {
                        set.add(x * 2 + xo + bxo, y * 2 + yo, z * 2 + zo);
                        set.add(x * 2 + xo, y * 2 + yo + byo, z * 2 + zo);
                        set.add(x * 2 + xo, y * 2 + yo, z * 2 + zo + bzo);
                        set.add(x * 2 + xo + bxo, y * 2 + yo + byo, z * 2 + zo);
                        set.add(x * 2 + xo + bxo, y * 2 + yo, z * 2 + zo + bzo);
                        set.add(x * 2 + xo, y * 2 + yo + byo, z * 2 + zo + bzo);
                     }
                  }
               }
            }
         }
      );
      PositionSet newSolid = new PositionSet();
      PositionSet newNonSolid = new PositionSet();
      in.forEachChunk((cx, cy, cz, blocks) -> {
         cx <<= 4;
         cy <<= 4;
         cz <<= 4;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  BlockState t = blocks[index++];
                  if (t == null || t.isAir()) {
                     processOutside(solid, newSolid, cx + x, cy + y, cz + z);
                     processOutside(nonSolid, newNonSolid, cx + x, cy + y, cz + z);
                  } else if (!isSolid(t)) {
                     processOutside(solid, newSolid, cx + x, cy + y, cz + z);
                  } else {
                     processOutside(nonSolid, newNonSolid, cx + x, cy + y, cz + z);
                  }
               }
            }
         }
      });
      newSolid.forEach(solid::add);
      newNonSolid.forEach(nonSolid::add);
      ChunkedBlockRegion out = new ChunkedBlockRegion();
      if (!nonSolid.isEmpty()) {
         applyToRegion(in, nonSolid, out, false);
      }

      if (!solid.isEmpty()) {
         applyToRegion(in, solid, out, true);
      }

      out.dirtyAll();
      return out;
   }

   private static void processOutside(PositionSet set, PositionSet newSet, int nx, int ny, int nz) {
      for (int i = 0; i <= 8; i++) {
         int xo = i & 1;
         int yo = (i & 2) >> 1;
         int zo = (i & 4) >> 2;
         boolean neighborXIsSolid = set.contains(nx * 2 + xo * 3 - 1, ny * 2 + yo, nz * 2 + zo);
         boolean neighborYIsSolid = set.contains(nx * 2 + xo, ny * 2 + yo * 3 - 1, nz * 2 + zo);
         boolean neighborZIsSolid = set.contains(nx * 2 + xo, ny * 2 + yo, nz * 2 + zo * 3 - 1);
         boolean oppositeXIsAir = !set.contains(nx * 2 - xo * 3 + 2, ny * 2 + yo, nz * 2 + zo);
         boolean oppositeYIsAir = !set.contains(nx * 2 + xo, ny * 2 - yo * 3 + 2, nz * 2 + zo);
         boolean oppositeZIsAir = !set.contains(nx * 2 + xo, ny * 2 + yo, nz * 2 - zo * 3 + 2);
         int oppositeAirCount = 0;
         if (oppositeXIsAir) {
            oppositeAirCount++;
         }

         if (oppositeYIsAir) {
            oppositeAirCount++;
         }

         if (oppositeZIsAir) {
            oppositeAirCount++;
         }

         if (neighborXIsSolid && neighborYIsSolid && neighborZIsSolid && oppositeAirCount >= 2) {
            newSet.add(nx * 2 + xo, ny * 2 + yo, nz * 2 + zo);
         } else if (neighborYIsSolid
            && neighborZIsSolid
            && oppositeYIsAir
            && oppositeZIsAir
            && set.contains(nx * 2 + xo, ny * 2 + yo * 3 - 1, nz * 2 + (1 - zo))
            && set.contains(nx * 2 + xo, ny * 2 + (1 - yo), nz * 2 + zo * 3 - 1)
            && set.contains(nx * 2 + (1 - xo), ny * 2 + yo * 3 - 1, nz * 2 + zo)
            && set.contains(nx * 2 + (1 - xo), ny * 2, nz * 2 + zo * 3 - 1)) {
            if (set.contains(nx * 2 + xo * 3 - 1, ny * 2 + yo * 3 - 1, nz * 2 + zo) && set.contains(nx * 2 + xo * 3 - 1, ny * 2 + yo, nz * 2 + zo * 3 - 1)) {
               newSet.add(nx * 2 + xo, ny * 2 + yo, nz * 2 + zo);
            } else if (!set.contains(nx * 2 - xo * 3 + 2, ny * 2 + yo * 3 - 1, nz * 2 + zo)
               || !set.contains(nx * 2 - xo * 3 + 2, ny * 2 + yo, nz * 2 + zo * 3 - 1)) {
               newSet.add(nx * 2 + xo, ny * 2 + yo, nz * 2 + zo);
               newSet.add(nx * 2 + xo, ny * 2 + yo * 3 - 1, nz * 2 + zo * 3 - 1);
            }
         } else if (neighborXIsSolid
            && neighborZIsSolid
            && oppositeXIsAir
            && oppositeZIsAir
            && set.contains(nx * 2 + xo * 3 - 1, ny * 2 + yo, nz * 2 + (1 - zo))
            && set.contains(nx * 2 + (1 - xo), ny * 2 + yo, nz * 2 + zo * 3 - 1)
            && set.contains(nx * 2 + xo * 3 - 1, ny * 2 + (1 - yo), nz * 2 + zo)
            && set.contains(nx * 2 + xo, ny * 2 + (1 - yo), nz * 2 + zo * 3 - 1)) {
            if (set.contains(nx * 2 + xo * 3 - 1, ny * 2 + yo * 3 - 1, nz * 2 + zo) && set.contains(nx * 2 + xo, ny * 2 + yo * 3 - 1, nz * 2 + zo * 3 - 1)) {
               newSet.add(nx * 2 + xo, ny * 2 + yo, nz * 2 + zo);
            } else if (!set.contains(nx * 2 + xo * 3 - 1, ny * 2 - yo * 3 + 2, nz * 2 + zo)
               || !set.contains(nx * 2 + xo, ny * 2 - yo * 3 + 2, nz * 2 + zo * 3 - 1)) {
               newSet.add(nx * 2 + xo, ny * 2 + yo, nz * 2 + zo);
               newSet.add(nx * 2 + xo * 3 - 1, ny * 2 + yo, nz * 2 + zo * 3 - 1);
            }
         } else if (neighborXIsSolid
            && neighborYIsSolid
            && oppositeXIsAir
            && oppositeYIsAir
            && set.contains(nx * 2 + xo * 3 - 1, ny * 2 + (1 - yo), nz * 2 + zo)
            && set.contains(nx * 2 + (1 - xo), ny * 2 + yo * 3 - 1, nz * 2 + zo)
            && set.contains(nx * 2 + xo * 3 - 1, ny * 2 + yo, nz * 2 + (1 - zo))
            && set.contains(nx * 2 + xo, ny * 2 + yo * 3 - 1, nz * 2 + (1 - zo))) {
            if (set.contains(nx * 2 + xo * 3 - 1, ny * 2 + yo, nz * 2 + zo * 3 - 1) && set.contains(nx * 2 + xo, ny * 2 + yo * 3 - 1, nz * 2 + zo * 3 - 1)) {
               newSet.add(nx * 2 + xo, ny * 2 + yo, nz * 2 + zo);
            } else if (!set.contains(nx * 2 + xo * 3 - 1, ny * 2 + yo, nz * 2 - zo * 3 + 2)
               || !set.contains(nx * 2 + xo, ny * 2 + yo * 3 - 1, nz * 2 - zo * 3 + 2)) {
               newSet.add(nx * 2 + xo, ny * 2 + yo, nz * 2 + zo);
               newSet.add(nx * 2 + xo * 3 - 1, ny * 2 + yo * 3 - 1, nz * 2 + zo);
            }
         }
      }
   }

   private static void applyToRegion(ChunkedBlockRegion in, PositionSet set, ChunkedBlockRegion out, boolean solid) {
      PositionSet ignore = new PositionSet();
      PositionSet tooMuchIgnore = new PositionSet();
      set.forEach((x, y, z) -> {
         int xo = (x & 1) * 2 - 1;
         int yo = (y & 1) * 2 - 1;
         int zo = (z & 1) * 2 - 1;
         if (!set.contains(x + xo, y + yo, z + zo)) {
            if (!set.contains(x, y + yo, z + zo)) {
               if (!set.contains(x + xo, y, z + zo)) {
                  if (!set.contains(x + xo, y + yo, z)) {
                     if (!set.contains(x + xo, y, z)) {
                        if (!set.contains(x, y + yo, z)) {
                           if (!set.contains(x, y, z + zo)) {
                              if (set.contains(x - xo, y, z)) {
                                 if (set.contains(x, y - yo, z)) {
                                    if (set.contains(x, y, z - zo)) {
                                       if (!set.contains(x + xo, y - yo, z)) {
                                          if (!set.contains(x + xo, y, z - zo)) {
                                             if (!set.contains(x - xo, y + yo, z)) {
                                                if (!set.contains(x, y + yo, z - zo)) {
                                                   if (!set.contains(x - xo, y, z + zo)) {
                                                      if (!set.contains(x, y - yo, z + zo)) {
                                                         if (!set.contains(x - xo, y + yo, z + zo)) {
                                                            if (!set.contains(x + xo, y - yo, z + zo)) {
                                                               if (!set.contains(x + xo, y + yo, z - zo)) {
                                                                  ignore.add(x, y, z);
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
                        }
                     }
                  }
               }
            }
         }
      });
      ignore.forEach((x, y, z) -> {
         int neighbors = 0;
         if (ignore.contains(x + 1, y, z)) {
            neighbors++;
         }

         if (ignore.contains(x - 1, y, z)) {
            neighbors++;
         }

         if (ignore.contains(x, y + 1, z)) {
            neighbors++;
         }

         if (ignore.contains(x, y - 1, z)) {
            neighbors++;
         }

         if (ignore.contains(x, y, z + 1)) {
            neighbors++;
         }

         if (ignore.contains(x, y, z - 1)) {
            neighbors++;
         }

         if (neighbors >= 2) {
            tooMuchIgnore.add(x, y, z);
         }
      });
      set.forEach(
         (x, y, z) -> {
            if (!ignore.contains(x, y, z)
               || tooMuchIgnore.contains(x + 1, y, z)
               || tooMuchIgnore.contains(x - 1, y, z)
               || tooMuchIgnore.contains(x, y + 1, z)
               || tooMuchIgnore.contains(x, y - 1, z)
               || tooMuchIgnore.contains(x, y, z + 1)
               || tooMuchIgnore.contains(x, y, z - 1)) {
               int bx = x >> 1;
               int by = y >> 1;
               int bz = z >> 1;
               BlockState center = in.getBlockStateOrAir(bx, by, bz);
               BlockState neighborX = in.getBlockStateOrAir(bx + (x & 1) * 2 - 1, by, bz);
               BlockState neighborY = in.getBlockStateOrAir(bx, by + (y & 1) * 2 - 1, bz);
               BlockState neighborZ = in.getBlockStateOrAir(bx, by, bz + (z & 1) * 2 - 1);
               if (isSolidOrNonSolid(center, solid)) {
                  if (center == in.getBlockStateOrAir(bx + (x & 1) * 2 - 1, by + (y & 1) * 2 - 1, bz + (z & 1) * 2 - 1)) {
                     out.addBlockWithoutDirty(x, y, z, center);
                     return;
                  }

                  if (center == in.getBlockStateOrAir(bx, by + (y & 1) * 2 - 1, bz + (z & 1) * 2 - 1)) {
                     out.addBlockWithoutDirty(x, y, z, center);
                     return;
                  }

                  if (center == in.getBlockStateOrAir(bx + (x & 1) * 2 - 1, by, bz + (z & 1) * 2 - 1)) {
                     out.addBlockWithoutDirty(x, y, z, center);
                     return;
                  }

                  if (center == in.getBlockStateOrAir(bx + (x & 1) * 2 - 1, by + (y & 1) * 2 - 1, bz)) {
                     out.addBlockWithoutDirty(x, y, z, center);
                     return;
                  }

                  int x1 = x <= 0 ? x - 1 : x;
                  int y1 = y <= 0 ? y - 1 : y;
                  int z1 = z <= 0 ? z - 1 : z;
                  int ring;
                  if (!isSolidOrNonSolid(neighborY, solid)) {
                     ring = Math.max(Math.abs(x1), Math.abs(z1));
                  } else if (!isSolidOrNonSolid(neighborX, solid)) {
                     ring = Math.max(Math.abs(y1), Math.abs(z1));
                  } else if (!isSolidOrNonSolid(neighborZ, solid)) {
                     ring = Math.max(Math.abs(x1), Math.abs(y1));
                  } else {
                     ring = 0;
                  }

                  if ((ring & 1) == 1) {
                     out.addBlockWithoutDirty(x, y, z, center);
                     return;
                  }
               }

               if (isSolidOrNonSolid(center, solid)) {
                  if (center == neighborY || center == neighborX || center == neighborZ) {
                     out.addBlockWithoutDirty(x, y, z, center);
                  } else if (isSolidOrNonSolid(neighborY, solid)) {
                     if (isSolidOrNonSolid(neighborX, solid) && neighborX == neighborZ) {
                        out.addBlockWithoutDirty(x, y, z, neighborX);
                     } else if (neighborY != neighborX && neighborY != neighborZ) {
                        out.addBlockWithoutDirty(x, y, z, center);
                     } else {
                        out.addBlockWithoutDirty(x, y, z, neighborY);
                     }
                  } else if (isSolidOrNonSolid(neighborX, solid)) {
                     if (neighborX == neighborZ) {
                        out.addBlockWithoutDirty(x, y, z, neighborX);
                     } else {
                        out.addBlockWithoutDirty(x, y, z, center);
                     }
                  } else {
                     out.addBlockWithoutDirty(x, y, z, center);
                  }
               } else if (isSolidOrNonSolid(neighborY, solid)) {
                  if (isSolidOrNonSolid(neighborX, solid) && neighborX == neighborZ) {
                     out.addBlockWithoutDirty(x, y, z, neighborX);
                  } else {
                     out.addBlockWithoutDirty(x, y, z, neighborY);
                  }
               } else if (isSolidOrNonSolid(neighborX, solid)) {
                  out.addBlockWithoutDirty(x, y, z, neighborX);
               } else if (isSolidOrNonSolid(neighborZ, solid)) {
                  out.addBlockWithoutDirty(x, y, z, neighborZ);
               }
            }
         }
      );
   }
}
