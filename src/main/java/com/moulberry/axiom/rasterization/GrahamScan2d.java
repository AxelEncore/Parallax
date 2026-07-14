package com.moulberry.axiom.rasterization;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;
import org.joml.Vector2d;

public class GrahamScan2d {
   public static Vector2d[] getVertices(Vector2d[] a) {
      Stack<Vector2d> hull = new Stack<>();
      int n = a.length;
      Arrays.sort(a, new GrahamScan2d.YOrder());
      Arrays.sort(a, 1, n, new GrahamScan2d.PolarOrder(a[0]));
      hull.push(a[0]);
      int k1 = 1;

      while (k1 < n && a[0].equals(a[k1])) {
         k1++;
      }

      if (k1 == n) {
         return null;
      } else {
         int k2 = k1 + 1;

         while (k2 < n && ccw(a[0], a[k1], a[k2]) == 0) {
            k2++;
         }

         hull.push(a[k2 - 1]);

         for (int i = k2; i < n; i++) {
            Vector2d top = hull.pop();

            while (ccw(hull.peek(), top, a[i]) <= 0) {
               top = hull.pop();
            }

            hull.push(top);
            hull.push(a[i]);
         }

         return hull.toArray(new Vector2d[0]);
      }
   }

   public static int ccw(Vector2d a, Vector2d b, Vector2d c) {
      double area2 = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
      if (area2 < 0.0) {
         return -1;
      } else {
         return area2 > 0.0 ? 1 : 0;
      }
   }

   private static int lowestPolarAngle(Vector2d[] points, Vector2d lowestPoint) {
      int index = 0;
      double minorPolarAngle = 180.0;

      for (int i = 1; i < points.length; i++) {
         double angle = lowestPoint.angle(points[i]);
         if (angle < minorPolarAngle) {
            minorPolarAngle = angle;
            index = i;
         }
      }

      return index;
   }

   public static int lowestPoint(Vector2d[] points) {
      Vector2d lowest = points[0];
      int index = 0;

      for (int i = 1; i < points.length; i++) {
         if (points[i].y < lowest.y || points[i].y == lowest.y && points[i].x > lowest.x) {
            lowest = points[i];
            index = i;
         }
      }

      return index;
   }

   public static void swap(Vector2d[] points, int index0, int index1) {
      Vector2d temp = points[index0];
      points[index0] = points[index1];
      points[index1] = temp;
   }

   public static double angle(Vector2d first, Vector2d p1, Vector2d p2) {
      double x = first.x;
      double y = first.y;
      double ax = p1.x - x;
      double ay = p1.y - y;
      double bx = p2.x - x;
      double by = p2.y - y;
      double delta = (ax * bx + ay * by) / Math.sqrt((ax * ax + ay * ay) * (bx * bx + by * by));
      if (delta > 1.0) {
         return 0.0;
      } else {
         return delta < -1.0 ? 180.0 : Math.toDegrees(Math.acos(delta));
      }
   }

   private record PolarOrder(Vector2d main) implements Comparator<Vector2d> {
      public int compare(Vector2d q1, Vector2d q2) {
         double dx1 = q1.x - this.main.x;
         double dy1 = q1.y - this.main.y;
         double dx2 = q2.x - this.main.x;
         double dy2 = q2.y - this.main.y;
         if (dy1 >= 0.0 && dy2 < 0.0) {
            return -1;
         } else if (dy2 >= 0.0 && dy1 < 0.0) {
            return 1;
         } else if (dy1 != 0.0 || dy2 != 0.0) {
            return -GrahamScan2d.ccw(this.main, q1, q2);
         } else if (dx1 >= 0.0 && dx2 < 0.0) {
            return -1;
         } else {
            return dx2 >= 0.0 && dx1 < 0.0 ? 1 : 0;
         }
      }
   }

   private static class YOrder implements Comparator<Vector2d> {
      public int compare(Vector2d o1, Vector2d o2) {
         if (o1.y < o2.y) {
            return -1;
         } else if (o1.y > o2.y) {
            return 1;
         } else if (o1.x < o2.x) {
            return -1;
         } else {
            return o1.x > o2.x ? 1 : 0;
         }
      }
   }
}
