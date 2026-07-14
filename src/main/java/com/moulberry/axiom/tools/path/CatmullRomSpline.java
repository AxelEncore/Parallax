package com.moulberry.axiom.tools.path;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class CatmullRomSpline {
   private static float tj(float ti, Vector3f p1, Vector3f p2, float alpha) {
      float dx = p2.x - p1.x;
      float dy = p2.y - p1.y;
      float dz = p2.z - p1.z;
      return (float)Math.pow(Math.sqrt(dx * dx + dy * dy + dz * dz), alpha) + ti;
   }

   private static FloatList linspace(float t1, float t2, int points) {
      FloatList list = new FloatArrayList(points);
      float delta = t2 - t1;

      for (int i = 0; i < points; i++) {
         list.add(t1 + delta * ((float)i / (points - 1)));
      }

      return list;
   }

   private static List<Vector3f> catmullInterp(FloatList list, Vector3f p1, Vector3f p2, float t1, float t0) {
      List<Vector3f> newList = new ArrayList<>();

      for (int i = 0; i < list.size(); i++) {
         float t = list.getFloat(i);
         Vector3f p1_2 = p1.mul((t1 - t) / (t1 - t0), new Vector3f());
         Vector3f p2_2 = p2.mul((t - t0) / (t1 - t0), new Vector3f());
         newList.add(p1_2.add(p2_2));
      }

      return newList;
   }

   private static List<Vector3f> catmullInterp(FloatList list, List<Vector3f> p1, List<Vector3f> p2, float t1, float t0) {
      List<Vector3f> newList = new ArrayList<>();

      for (int i = 0; i < list.size(); i++) {
         float t = list.getFloat(i);
         Vector3f p1_2 = p1.get(i).mul((t1 - t) / (t1 - t0), new Vector3f());
         Vector3f p2_2 = p2.get(i).mul((t - t0) / (t1 - t0), new Vector3f());
         newList.add(p1_2.add(p2_2));
      }

      return newList;
   }

   public static List<Vector3f> createCatmullRomSpline(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, int pointNum, float alpha) {
      float t0 = 0.0F;
      float t1 = tj(t0, p1, p2, alpha);
      float t2 = tj(t1, p2, p3, alpha);
      float t3 = tj(t2, p3, p4, alpha);
      FloatList points = linspace(t1, t2, pointNum);
      List<Vector3f> A1 = catmullInterp(points, p1, p2, t1, t0);
      List<Vector3f> A2 = catmullInterp(points, p2, p3, t2, t1);
      List<Vector3f> A3 = catmullInterp(points, p3, p4, t3, t2);
      List<Vector3f> B1 = catmullInterp(points, A1, A2, t2, t0);
      List<Vector3f> B2 = catmullInterp(points, A2, A3, t3, t1);
      return catmullInterp(points, B1, B2, t2, t1);
   }

   public static List<Vector4f> createCatmullRomSplineWithPartial(Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, int pointNum, float alpha) {
      float t0 = 0.0F;
      float t1 = tj(t0, p1, p2, alpha);
      float t2 = tj(t1, p2, p3, alpha);
      float t3 = tj(t2, p3, p4, alpha);
      FloatList points = linspace(t1, t2, pointNum);
      List<Vector3f> A1 = catmullInterp(points, p1, p2, t1, t0);
      List<Vector3f> A2 = catmullInterp(points, p2, p3, t2, t1);
      List<Vector3f> A3 = catmullInterp(points, p3, p4, t3, t2);
      List<Vector3f> B1 = catmullInterp(points, A1, A2, t2, t0);
      List<Vector3f> B2 = catmullInterp(points, A2, A3, t3, t1);
      List<Vector3f> C = catmullInterp(points, B1, B2, t2, t1);
      List<Vector4f> result = new ArrayList<>();

      for (int i = 0; i < C.size(); i++) {
         Vector3f vector3f = C.get(i);
         result.add(new Vector4f(vector3f.x, vector3f.y, vector3f.z, points.getFloat(i) - 1.0F));
      }

      return result;
   }

   private static double positionCentripetalTj(Vector3d p1, Vector3d p2) {
      double dx = p2.x - p1.x;
      double dy = p2.y - p1.y;
      double dz = p2.z - p1.z;
      return Math.pow(dx * dx + dy * dy + dz * dz, 0.25);
   }

   public static Vector3d position(Vector3d p0, Vector3d p1, Vector3d p2, Vector3d p3, float amount) {
      double tj1 = positionCentripetalTj(p0, p1);
      double tj2 = positionCentripetalTj(p1, p2);
      double tj3 = positionCentripetalTj(p2, p3);
      double t0 = 0.0;
      double t1 = t0 + tj1;
      double t2 = t1 + tj2;
      double t3 = t2 + tj3;
      double t = t1 + (t2 - t1) * amount;
      Vector3d a1;
      if (t0 == t1) {
         a1 = p0.lerp(p1, 0.5, new Vector3d());
      } else {
         a1 = p0.lerp(p1, (t - t0) / (t1 - t0), new Vector3d());
      }

      Vector3d a2;
      if (t1 == t2) {
         a2 = p1.lerp(p2, 0.5, new Vector3d());
      } else {
         a2 = p1.lerp(p2, (t - t1) / (t2 - t1), new Vector3d());
      }

      Vector3d a3;
      if (t2 == t3) {
         a3 = p2.lerp(p3, 0.5, new Vector3d());
      } else {
         a3 = p2.lerp(p3, (t - t2) / (t3 - t2), new Vector3d());
      }

      Vector3d b1;
      if (t0 == t2) {
         b1 = a1.lerp(a2, 0.5, new Vector3d());
      } else {
         b1 = a1.lerp(a2, (t - t0) / (t2 - t0), new Vector3d());
      }

      Vector3d b2;
      if (t1 == t3) {
         b2 = a2.lerp(a3, 0.5, new Vector3d());
      } else {
         b2 = a2.lerp(a3, (t - t1) / (t3 - t1), new Vector3d());
      }

      return t1 == t2 ? b1.lerp(b2, 0.5, new Vector3d()) : b1.lerp(b2, (t - t1) / (t2 - t1), new Vector3d());
   }
}
