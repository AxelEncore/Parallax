package com.moulberry.axiom.rasterization;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.funcinterfaces.BiIntConsumer;
import com.moulberry.axiom.funcinterfaces.BiIntFunction;
import com.moulberry.axiom.funcinterfaces.IntIntFloatConsumer;
import java.util.Arrays;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class Rasterization2D {
   public static void triangle(Vector2i one, Vector2i two, Vector2i three, BiIntConsumer consumer) {
      triangle(one.x, one.y, two.x, two.y, three.x, three.y, consumer);
   }

   public static void triangle(int x1, int y1, int x2, int y2, int x3, int y3, BiIntConsumer consumer) {
      Vector2i[] extremeIfColinear = getExtremePointsIfColinear(x1, y1, x2, y2, x3, y3);
      if (extremeIfColinear != null) {
         bresenham(extremeIfColinear[0], extremeIfColinear[1], consumer);
      } else {
         int minY = Math.min(y1, Math.min(y2, y3));
         int maxY = Math.max(y1, Math.max(y2, y3));
         if (new Vector3f(x2 - x1, y2 - y1, 0.0F).cross(x3 - x1, y3 - y1, 0.0F).z < 0.0F) {
            int temp = x2;
            x2 = x3;
            x3 = temp;
            temp = y2;
            y2 = y3;
            y3 = temp;
         }

         Vector2f center = new Vector2f((x1 + x2 + x3) / 3.0F + 0.5F, (y1 + y2 + y3) / 3.0F + 0.5F);
         int[] leftx = new int[maxY - minY + 1];
         Arrays.fill(leftx, Integer.MAX_VALUE);
         int[] rightx = new int[maxY - minY + 1];
         Arrays.fill(rightx, Integer.MIN_VALUE);
         BiIntConsumer bresenhamConsumer = (xx, yx) -> {
            if (xx < leftx[yx - minY]) {
               leftx[yx - minY] = xx;
            }

            if (xx > rightx[yx - minY]) {
               rightx[yx - minY] = xx;
            }
         };
         bresenham(new Vector2i(x1, y1), new Vector2i(x2, y2), bresenhamConsumer, bresenhamBias(new Vector2i(x1, y1), new Vector2i(x2, y2), center));
         bresenham(new Vector2i(x2, y2), new Vector2i(x3, y3), bresenhamConsumer, bresenhamBias(new Vector2i(x2, y2), new Vector2i(x3, y3), center));
         bresenham(new Vector2i(x3, y3), new Vector2i(x1, y1), bresenhamConsumer, bresenhamBias(new Vector2i(x3, y3), new Vector2i(x1, y1), center));

         for (int y = minY; y <= maxY; y++) {
            int left = leftx[y - minY];
            int right = rightx[y - minY];

            for (int x = left; x <= right; x++) {
               consumer.accept(x, y);
            }
         }
      }
   }

   private static Vector2i[] getExtremePointsIfColinear(int x1, int y1, int x2, int y2, int x3, int y3) {
      boolean colinear = (x2 - x1) * (y3 - y1) == (x3 - x1) * (y2 - y1);
      if (!colinear) {
         return null;
      } else {
         int firstX;
         int firstY;
         int secondX;
         int secondY;
         if (x1 == x2 && x2 == x3) {
            if (y1 <= y2 && y1 <= y3) {
               firstX = x1;
               firstY = y1;
               if (y2 >= y3) {
                  secondX = x2;
                  secondY = y2;
               } else {
                  secondX = x3;
                  secondY = y3;
               }
            } else if (y2 <= y3) {
               if (y1 >= y3) {
                  firstX = x1;
                  firstY = y1;
                  secondX = x2;
                  secondY = y2;
               } else {
                  firstX = x2;
                  firstY = y2;
                  secondX = x3;
                  secondY = y3;
               }
            } else {
               secondX = x3;
               secondY = y3;
               if (y1 >= y2) {
                  firstX = x1;
                  firstY = y1;
               } else {
                  firstX = x2;
                  firstY = y2;
               }
            }
         } else if (x1 <= x2 && x1 <= x3) {
            firstX = x1;
            firstY = y1;
            if (x2 >= x3) {
               secondX = x2;
               secondY = y2;
            } else {
               secondX = x3;
               secondY = y3;
            }
         } else if (x2 <= x3) {
            if (x1 >= x3) {
               firstX = x1;
               firstY = y1;
               secondX = x2;
               secondY = y2;
            } else {
               firstX = x2;
               firstY = y2;
               secondX = x3;
               secondY = y3;
            }
         } else {
            secondX = x3;
            secondY = y3;
            if (x1 >= x2) {
               firstX = x1;
               firstY = y1;
            } else {
               firstX = x2;
               firstY = y2;
            }
         }

         return new Vector2i[]{new Vector2i(firstX, firstY), new Vector2i(secondX, secondY)};
      }
   }

   public static void ddaCircle(int radius, BiIntConsumer consumer) {
      int x = radius;
      int y = 0;
      int dy = -2;
      int dx = 4 * radius - 4;
      int d = 2 * radius - 1;

      while (y <= x) {
         consumer.accept(x, y);
         consumer.accept(-x, y);
         consumer.accept(x, -y);
         consumer.accept(-x, -y);
         consumer.accept(y, x);
         consumer.accept(-y, x);
         consumer.accept(y, -x);
         consumer.accept(-y, -x);
         d += dy;
         dy -= 4;
         y++;
         if (d < 0) {
            if (y <= x) {
               consumer.accept(x, y);
               consumer.accept(-x, y);
               consumer.accept(x, -y);
               consumer.accept(-x, -y);
               consumer.accept(y, x);
               consumer.accept(-y, x);
               consumer.accept(y, -x);
               consumer.accept(-y, -x);
            }

            d += dx;
            dx -= 4;
            x--;
         }
      }
   }

   public static boolean bresenhamBias(Vector2i from, Vector2i to, Vector2f biasTowards) {
      boolean bias = (to.x - from.x) * (biasTowards.y - (from.y + 0.5)) - (to.y - from.y) * (biasTowards.x - (from.x + 0.5)) > 0.0;
      if (to.x < from.x) {
         bias = !bias;
      }

      if (to.y < from.y) {
         bias = !bias;
      }

      if (Math.abs(to.y - from.y) < Math.abs(to.x - from.x)) {
         bias = !bias;
      }

      return bias;
   }

   public static void bresenham(Vector2i from, Vector2i to, BiIntConsumer consumer) {
      bresenham(from, to, consumer, false);
   }

   public static void bresenham(Vector2i from, Vector2i to, BiIntConsumer consumer, boolean bias) {
      consumer.accept(from.x, from.y);
      bresenhamSkipFrom(from, to, consumer, bias);
   }

   public static void bresenhamSkipFrom(Vector2i from, Vector2i to, BiIntConsumer consumer, boolean bias) {
      if (!to.equals(from)) {
         int x = from.x;
         int y = from.y;
         int dx = Math.abs(to.x - x);
         int dy = Math.abs(to.y - y);
         int xs = to.x > x ? 1 : -1;
         int ys = to.y > y ? 1 : -1;
         int threshold = bias ? 1 : 0;
         if (dx >= dy) {
            int p1 = 2 * dy - dx;

            while (x != to.x) {
               x += xs;
               if (p1 >= threshold) {
                  y += ys;
                  p1 -= 2 * dx;
               }

               p1 += 2 * dy;
               consumer.accept(x, y);
            }
         } else {
            int p1 = 2 * dx - dy;

            while (y != to.y) {
               y += ys;
               if (p1 >= threshold) {
                  x += xs;
                  p1 -= 2 * dy;
               }

               p1 += 2 * dx;
               consumer.accept(x, y);
            }
         }
      }
   }

   @Nullable
   public static <T> T bresenhamReturn(Vector2i from, Vector2i to, BiIntFunction<T> consumer) {
      T initial = consumer.apply(from.x, from.y);
      if (initial != null) {
         return initial;
      } else if (to.equals(from)) {
         return null;
      } else {
         int x = from.x;
         int y = from.y;
         int dx = Math.abs(to.x - x);
         int dy = Math.abs(to.y - y);
         int xs = to.x > x ? 1 : -1;
         int ys = to.y > y ? 1 : -1;
         int threshold = 0;
         if (dx >= dy) {
            int p1 = 2 * dy - dx;

            while (x != to.x) {
               x += xs;
               if (p1 >= threshold) {
                  y += ys;
                  p1 -= 2 * dx;
               }

               p1 += 2 * dy;
               T value = consumer.apply(x, y);
               if (value != null) {
                  return value;
               }
            }
         } else {
            int p1 = 2 * dx - dy;

            while (y != to.y) {
               y += ys;
               if (p1 >= threshold) {
                  x += xs;
                  p1 -= 2 * dy;
               }

               p1 += 2 * dx;
               T value = consumer.apply(x, y);
               if (value != null) {
                  return value;
               }
            }
         }

         return null;
      }
   }

   private static float fpart(float f) {
      return f - (float)Math.floor(f);
   }

   private static float rfpart(float f) {
      return 1.0F - fpart(f);
   }

   public static void xiaolinWu(Vector2f from, Vector2f to, IntIntFloatConsumer consumer) {
      float x0 = from.x - 0.5F;
      float y0 = from.y - 0.5F;
      float x1 = to.x - 0.5F;
      float y1 = to.y - 0.5F;
      int bx0 = Math.round(x0);
      int by0 = Math.round(y0);
      int bx1 = Math.round(x1);
      int by1 = Math.round(y1);
      if (bx0 == bx1 && by0 == by1) {
         consumer.accept(bx0, by0, Math.max(Math.abs(x0 - x1), Math.abs(y0 - y1)));
      } else {
         boolean steep = Math.abs(by0 - by1) > Math.abs(bx0 - bx1);
         if (steep) {
            float temp = x0;
            x0 = y0;
            y0 = temp;
            temp = x1;
            x1 = y1;
            y1 = temp;
         }

         if (x0 > x1) {
            float temp = x0;
            x0 = x1;
            x1 = temp;
            temp = y0;
            y0 = y1;
            y1 = temp;
         }

         bx0 = Math.round(x0);
         bx1 = Math.round(x1);
         if (bx0 == bx1) {
            by0 = Math.round(y0);
            if (steep) {
               consumer.accept(by0, bx0, Math.abs(x0 - x1));
            } else {
               consumer.accept(bx0, by0, Math.abs(x0 - x1));
            }
         } else {
            float dx = x1 - x0;
            float dy = y1 - y0;
            float gradient = 1.0F;
            if (dx != 0.0) {
               gradient = dy / dx;
            }

            float xgap = rfpart(x0 + 0.5F);
            float yend = y0 + gradient * (bx0 - x0);
            int ypxl1 = (int)Math.floor(yend);
            if (steep) {
               consumer.accept(ypxl1, bx0, rfpart(yend) * xgap);
               consumer.accept(ypxl1 + 1, bx0, fpart(yend) * xgap);
            } else {
               consumer.accept(bx0, ypxl1, rfpart(yend) * xgap);
               consumer.accept(bx0, ypxl1 + 1, fpart(yend) * xgap);
            }

            float intery = yend + gradient;
            xgap = fpart(x1 + 0.5F);
            yend = y1 + gradient * (bx1 - x1);
            int ypxl2 = (int)Math.floor(yend);
            if (steep) {
               consumer.accept(ypxl2, bx1, rfpart(yend) * xgap);
               consumer.accept(ypxl2 + 1, bx1, fpart(yend) * xgap);
            } else {
               consumer.accept(bx1, ypxl2, rfpart(yend) * xgap);
               consumer.accept(bx1, ypxl2 + 1, fpart(yend) * xgap);
            }

            if (steep) {
               for (int x = bx0 + 1; x <= bx1 - 1; x++) {
                  int y = (int)Math.floor(intery);
                  consumer.accept(y, x, rfpart(intery));
                  consumer.accept(y + 1, x, fpart(intery));
                  intery += gradient;
               }
            } else {
               for (int x = bx0 + 1; x <= bx1 - 1; x++) {
                  int y = (int)Math.floor(intery);
                  consumer.accept(x, y, rfpart(intery));
                  consumer.accept(x, y + 1, fpart(intery));
                  intery += gradient;
               }
            }
         }
      }
   }

   public static void dda(Vector2i from, Vector2i to, BiIntConsumer consumer) {
      consumer.accept(from.x, from.y);
      if (!to.equals(from)) {
         Vector2f ray = new Vector2f(to.x - from.x, to.y - from.y).normalize();
         int mapX = from.x;
         int mapY = from.y;
         double deltaDistX = Math.abs(1.0F / ray.x);
         double deltaDistY = Math.abs(1.0F / ray.y);
         int stepX = ray.x < 0.0F ? -1 : 1;
         int stepY = ray.y < 0.0F ? -1 : 1;
         double sideDistX = 0.5 * deltaDistX;
         double sideDistY = 0.5 * deltaDistY;
         int limit = 0;

         while (!BuildConfig.DEBUG || limit++ < 100000) {
            if (sideDistX < sideDistY) {
               sideDistX += deltaDistX;
               mapX += stepX;
            } else {
               sideDistY += deltaDistY;
               mapY += stepY;
            }

            consumer.accept(mapX, mapY);
            if (mapX == to.x && mapY == to.y) {
               return;
            }
         }

         throw new FaultyImplementationError("100,000 iterations. Infinite loop?");
      }
   }
}
