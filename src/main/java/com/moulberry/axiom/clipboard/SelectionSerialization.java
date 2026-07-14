package com.moulberry.axiom.clipboard;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.BooleanWrapper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import net.minecraft.core.BlockPos;

public class SelectionSerialization {
   public static void writeSelection(BufferedWriter writer, SelectionBuffer buffer) throws IOException {
      StringBuilder line = new StringBuilder();
      if (buffer instanceof SelectionBuffer.AABB aabb) {
         BlockPos min = aabb.min();
         BlockPos max = aabb.max();
         if (min == null || max == null) {
            return;
         }

         line.append(min.getX())
            .append(',')
            .append(min.getY())
            .append(',')
            .append(min.getZ())
            .append(',')
            .append(max.getX())
            .append(',')
            .append(max.getY())
            .append(',')
            .append(max.getZ());
         writer.write(line.toString());
      } else if (buffer instanceof SelectionBuffer.Set set) {
         Long2ObjectMap<short[]> map = set.selectionRegion.unsafeGetPositionSet().unsafeGetRawMap();
         ObjectIterator var26 = map.long2ObjectEntrySet().iterator();

         while (var26.hasNext()) {
            Entry<short[]> entry = (Entry<short[]>)var26.next();
            int cx = BlockPos.getX(entry.getLongKey());
            int cy = BlockPos.getY(entry.getLongKey());
            int cz = BlockPos.getZ(entry.getLongKey());
            boolean fullSection = true;

            for (short value : (short[])entry.getValue()) {
               if (value != -1) {
                  fullSection = false;
                  break;
               }
            }

            int minX = cx << 4;
            int minY = cy << 4;
            int minZ = cz << 4;
            if (fullSection) {
               line.setLength(0);
               line.append(minX)
                  .append(',')
                  .append(minY)
                  .append(',')
                  .append(minZ)
                  .append(',')
                  .append(minX + 15)
                  .append(',')
                  .append(minY + 15)
                  .append(',')
                  .append(minZ + 15);
               writer.write(line.append('\n').toString());
            } else {
               int index = 0;

               for (int z = 0; z < 16; z++) {
                  int minRowY = -1;
                  int rowMin = 0;
                  int rowMax = 0;

                  for (int y = 0; y < 16; y++) {
                     short v = ((short[])entry.getValue())[index++];
                     int low = Integer.numberOfTrailingZeros(v & '\uffff');
                     int high = 31 - Integer.numberOfLeadingZeros(v & '\uffff');
                     if (Integer.bitCount(v & '\uffff') == high - low + 1) {
                        if (minRowY == -1) {
                           minRowY = y;
                           rowMin = low;
                           rowMax = high;
                        } else if (rowMin != low || rowMax != high) {
                           line.setLength(0);
                           line.append(minX + rowMin)
                              .append(',')
                              .append(minY + minRowY)
                              .append(',')
                              .append(minZ + z)
                              .append(',')
                              .append(minX + rowMax)
                              .append(',')
                              .append(minY + y - 1)
                              .append(',')
                              .append(minZ + z);
                           writer.write(line.append('\n').toString());
                           minRowY = y;
                           rowMin = low;
                           rowMax = high;
                        }
                     } else {
                        if (minRowY != -1) {
                           line.setLength(0);
                           line.append(minX + rowMin)
                              .append(',')
                              .append(minY + minRowY)
                              .append(',')
                              .append(minZ + z)
                              .append(',')
                              .append(minX + rowMax)
                              .append(',')
                              .append(minY + y - 1)
                              .append(',')
                              .append(minZ + z);
                           writer.write(line.append('\n').toString());
                           minRowY = -1;
                        }

                        if (v != 0) {
                           for (int x = 0; x < 16; x++) {
                              if ((v & 1 << x) != 0) {
                                 line.setLength(0);
                                 line.append(minX + x).append(',').append(minY + y).append(',').append(minZ + z);
                                 writer.write(line.append('\n').toString());
                              }
                           }
                        }
                     }
                  }

                  if (minRowY != -1) {
                     line.setLength(0);
                     line.append(minX + rowMin)
                        .append(',')
                        .append(minY + minRowY)
                        .append(',')
                        .append(minZ + z)
                        .append(',')
                        .append(minX + rowMax)
                        .append(',')
                        .append(minY + 15)
                        .append(',')
                        .append(minZ + z);
                     writer.write(line.append('\n').toString());
                  }
               }
            }
         }
      }
   }

   public static SelectionBuffer loadSelection(BufferedReader reader, BooleanWrapper malformed) throws IOException {
      while (true) {
         String line = reader.readLine();
         if (line == null) {
            return SelectionBuffer.EMPTY;
         }

         if (!line.isEmpty()) {
            String[] split = line.split(",");
            int initialX1;
            int initialY1;
            int initialZ1;
            int initialX2;
            int initialY2;
            int initialZ2;
            if (split.length == 3) {
               initialX1 = initialX2 = Integer.parseInt(split[0]);
               initialY1 = initialY2 = Integer.parseInt(split[1]);
               initialZ1 = initialZ2 = Integer.parseInt(split[2]);
            } else {
               if (split.length != 6) {
                  malformed.value = true;
                  continue;
               }

               initialX1 = Integer.parseInt(split[0]);
               initialY1 = Integer.parseInt(split[1]);
               initialZ1 = Integer.parseInt(split[2]);
               initialX2 = Integer.parseInt(split[3]);
               initialY2 = Integer.parseInt(split[4]);
               initialZ2 = Integer.parseInt(split[5]);
            }

            PositionSet set = null;

            while (true) {
               line = reader.readLine();
               if (line == null) {
                  if (set == null) {
                     return new SelectionBuffer.AABB(new BlockPos(initialX1, initialY1, initialZ1), new BlockPos(initialX2, initialY2, initialZ2));
                  }

                  return new SelectionBuffer.Set(new ChunkedBooleanRegion(set));
               }

               if (!line.isEmpty()) {
                  if (set == null) {
                     set = new PositionSet();

                     for (int x = initialX1; x <= initialX2; x++) {
                        for (int y = initialY1; y <= initialY2; y++) {
                           for (int z = initialZ1; z <= initialZ2; z++) {
                              set.add(x, y, z);
                           }
                        }
                     }
                  }

                  String[] splitx = line.split(",");
                  if (splitx.length == 3) {
                     set.add(Integer.parseInt(splitx[0]), Integer.parseInt(splitx[1]), Integer.parseInt(splitx[2]));
                  } else if (splitx.length != 6) {
                     malformed.value = true;
                  } else {
                     int fromX = Integer.parseInt(splitx[0]);
                     int fromY = Integer.parseInt(splitx[1]);
                     int fromZ = Integer.parseInt(splitx[2]);
                     int toX = Integer.parseInt(splitx[3]);
                     int toY = Integer.parseInt(splitx[4]);
                     int toZ = Integer.parseInt(splitx[5]);

                     for (int x = fromX; x <= toX; x++) {
                        for (int y = fromY; y <= toY; y++) {
                           for (int z = fromZ; z <= toZ; z++) {
                              set.add(x, y, z);
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
