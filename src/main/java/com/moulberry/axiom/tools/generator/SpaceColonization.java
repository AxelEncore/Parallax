package com.moulberry.axiom.tools.generator;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SpaceColonization {
   public static void colonize(SpaceColonization.TreeNode startingNode, List<Vec3> attractionPoints, float kill, float influence) {
      List<Vec3> var22 = new ArrayList<>(attractionPoints);
      List<SpaceColonization.TreeNode> all = new ArrayList<>();
      startingNode.addSelfAndChildrenToList(all);

      for (int iter = 0; iter < 1000; iter++) {
         boolean hasDirection = false;

         for (SpaceColonization.TreeNode node : all) {
            node.direction = null;
         }

         Iterator<Vec3> iterator = var22.iterator();

         while (iterator.hasNext()) {
            Vec3 attractionPoint = iterator.next();
            SpaceColonization.TreeNode closestNode = null;
            double closestDistanceSq = Float.MAX_VALUE;

            for (int i = 0; i < all.size(); i++) {
               SpaceColonization.TreeNode node = all.get(i);
               double dx = node.position.x - attractionPoint.x;
               double dy = node.position.y - attractionPoint.y;
               double dz = node.position.z - attractionPoint.z;
               double distanceSq = dx * dx + dy * dy + dz * dz;
               if (distanceSq < kill * kill) {
                  iterator.remove();
                  closestNode = null;
                  break;
               }

               if (distanceSq < influence * influence && distanceSq < closestDistanceSq) {
                  closestNode = node;
                  closestDistanceSq = distanceSq;
               }
            }

            if (closestNode != null && (closestNode.parent == null || closestNode.children.isEmpty())) {
               float dxx = (float)(attractionPoint.x - closestNode.position.x);
               float dyx = (float)(attractionPoint.y - closestNode.position.y);
               float dzx = (float)(attractionPoint.z - closestNode.position.z);
               float invLength = 1.0F / (float)Math.sqrt(dxx * dxx + dyx * dyx + dzx * dzx);
               dxx *= invLength;
               dyx *= invLength;
               dzx *= invLength;
               if (closestNode.parent != null) {
                  Vec3 subtract = closestNode.position.subtract(closestNode.parent.position).normalize();
                  double dot = subtract.dot(new Vec3(dxx, dyx, dzx));
                  if (dot < 0.3) {
                     continue;
                  }
               }

               hasDirection = true;
               if (closestNode.direction == null) {
                  closestNode.direction = new Vector3f(dxx, dyx, dzx);
               } else {
                  closestNode.direction.x += dxx;
                  closestNode.direction.y += dyx;
                  closestNode.direction.z += dzx;
               }
            }
         }

         if (!hasDirection) {
            break;
         }

         boolean hasValidDirection = false;

         for (SpaceColonization.TreeNode nodex : all) {
            if (nodex.direction != null && nodex.direction.lengthSquared() > 1.0E-5) {
               hasValidDirection = true;
               break;
            }
         }

         if (!hasValidDirection) {
            break;
         }

         int j = 0;

         while (j < 1000 && !startingNode.grow(1.0F, all)) {
            j++;
         }
      }
   }

   public static class TreeNode {
      private SpaceColonization.TreeNode parent;
      public Vec3 position;
      private float area;
      private float growth;
      private Vector3f direction;
      public List<SpaceColonization.TreeNode> children;

      public TreeNode(Vec3 position) {
         this.position = position;
         this.area = 0.1F;
         this.growth = 0.0F;
         this.direction = null;
         this.children = new ArrayList<>();
      }

      public boolean grow(float feed, List<SpaceColonization.TreeNode> all) {
         boolean branch = false;
         boolean childBranched = false;
         boolean validDirection = this.direction != null && this.direction.lengthSquared() > 1.0E-5;
         if (this.children.isEmpty()) {
            if (!validDirection) {
               return false;
            }

            this.growth += feed;
            if (this.growth >= 1.0F) {
               branch = true;
               this.growth--;
            }
         } else {
            int children = this.children.size();
            if (validDirection) {
               children++;
            }

            float perChild = feed / children;
            if (validDirection) {
               this.growth += perChild;
               if (this.growth >= 1.0F) {
                  branch = true;
                  this.growth--;
               }
            }

            for (SpaceColonization.TreeNode child : this.children) {
               childBranched |= child.grow(perChild, all);
            }
         }

         if (branch) {
            this.direction.normalize();
            if (this.parent != null) {
               Vec3 subtract = this.position.subtract(this.parent.position).normalize();
               this.direction.mul(0.1F).add((float)subtract.x * 0.9F, 0.25F, (float)subtract.z * 0.9F);
               this.direction.normalize();
            }

            Vec3 offset = this.position.add(this.direction.x, this.direction.y, this.direction.z);
            SpaceColonization.TreeNode child = new SpaceColonization.TreeNode(offset);
            this.children.add(child);
            child.parent = this;
            all.add(child);
            return true;
         } else {
            return childBranched;
         }
      }

      public void addSelfAndChildrenToList(List<SpaceColonization.TreeNode> list) {
         list.add(this);

         for (SpaceColonization.TreeNode child : this.children) {
            child.addSelfAndChildrenToList(list);
         }
      }

      public void generate(ChunkedBlockRegion region, BlockState blockState) {
         region.addBlockWithoutDirty((int)Math.floor(this.position.x), (int)Math.floor(this.position.y), (int)Math.floor(this.position.z), blockState);

         for (SpaceColonization.TreeNode child : this.children) {
            child.generate(region, blockState);
         }
      }
   }
}
