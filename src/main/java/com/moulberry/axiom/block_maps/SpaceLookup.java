package com.moulberry.axiom.block_maps;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import net.minecraft.world.phys.Vec3;

public class SpaceLookup<T> {
   private final Map<Vec3, T> items = new LinkedHashMap<>();

   public void clear() {
      this.items.clear();
   }

   public void edit(Vec3 position, UnaryOperator<T> op) {
      T existingItem = this.items.get(position);
      T newItem = op.apply(existingItem);
      if (newItem != existingItem) {
         this.items.put(position, newItem);
      }
   }

   public T nearest(Vec3 position) {
      T nearest = null;
      double nearestDistance = Double.POSITIVE_INFINITY;

      for (Entry<Vec3, T> entry : this.items.entrySet()) {
         double distance = entry.getKey().distanceToSqr(position);
         if (distance < nearestDistance) {
            nearestDistance = distance;
            nearest = entry.getValue();
         }
      }

      return nearest;
   }

   public List<T> nearestN(Vec3 position, int n) {
      n = Math.min(n, this.items.size());
      if (n < 1) {
         return List.of();
      } else if (n == 1) {
         return List.of(this.nearest(position));
      } else {
         DoubleList nearestDistances = new DoubleArrayList(n + 1);
         List<T> nearest = new ArrayList<>(n + 1);
         double lowestDistance = Double.POSITIVE_INFINITY;
         double highestDistance = Double.POSITIVE_INFINITY;

         for (Entry<Vec3, T> entry : this.items.entrySet()) {
            double distance = entry.getKey().distanceToSqr(position);
            if (distance < lowestDistance) {
               nearestDistances.add(0, distance);
               nearest.add(0, entry.getValue());
               if (nearest.size() > n) {
                  nearest.remove(n);
                  nearestDistances.removeDouble(n);
               }

               lowestDistance = distance;
               highestDistance = nearestDistances.getDouble(nearestDistances.size() - 1);
            } else if (distance > highestDistance) {
               if (nearest.size() < n) {
                  nearest.add(entry.getValue());
                  nearestDistances.add(distance);
                  highestDistance = distance;
               }
            } else {
               int i = 0;

               while (true) {
                  if (i < nearestDistances.size()) {
                     double otherDistance = nearestDistances.getDouble(i);
                     if (!(distance < otherDistance)) {
                        i++;
                        continue;
                     }

                     nearestDistances.add(i, distance);
                     nearest.add(i, entry.getValue());
                  }

                  if (nearest.size() > n) {
                     nearest.remove(n);
                     nearestDistances.removeDouble(n);
                  }

                  highestDistance = nearestDistances.getDouble(nearestDistances.size() - 1);
                  break;
               }
            }
         }

         return nearest;
      }
   }
}
