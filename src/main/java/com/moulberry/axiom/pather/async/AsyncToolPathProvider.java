package com.moulberry.axiom.pather.async;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.GlobalCleaner;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.pather.ToolPatherPoint;
import com.moulberry.axiom.tools.Tool;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

public class AsyncToolPathProvider {
   private Vec3 lastLookDirection = null;
   private Vec3 lastPosition = null;
   private final DoubleList lastPositionsForSmoothing = new DoubleArrayList();
   private final int smoothingAmount;
   private final ArrayBlockingQueue<long[]> inputPositions;
   private final AsyncToolPather pather;
   private final AtomicBoolean stopTask;
   private final AtomicBoolean stopTaskWhenInputEmpty;
   private final AtomicBoolean stoppedTask;
   private final GlobalCleaner.LeakChecker leakChecker;
   private boolean includeNonSolid = true;
   public boolean includeFluids = Tool.defaultIncludeFluids();
   public static final int SMOOTH_DIVISIONS = 10;
   private static final double SMOOTH_DISTANCE = 0.1;

   public AsyncToolPathProvider(@NotNull AsyncToolPather pather) {
      this.inputPositions = new ArrayBlockingQueue<>(128);
      this.stopTask = new AtomicBoolean(false);
      this.stopTaskWhenInputEmpty = new AtomicBoolean(false);
      this.stoppedTask = new AtomicBoolean(false);
      this.smoothingAmount = (int)(Axiom.configuration.editor.toolStabilization * 10.0F);
      Tool.sharedPoolThreadExecutor
         .submit(new AsyncToolPathProvider.AsyncTask(this.inputPositions, this.stopTask, this.stopTaskWhenInputEmpty, this.stoppedTask, pather));
      this.pather = pather;
      this.leakChecker = GlobalCleaner.createLeakChecker(this, "AsyncToolPathProvider");
   }

   public AsyncToolPathProvider includeNonSolid(boolean includeNonSolid) {
      this.includeNonSolid = includeNonSolid;
      return this;
   }

   public void close() {
      this.stopTask.set(true);
      this.leakChecker.disarm();
   }

   public void finish() {
      if (!this.stopTask.get()) {
         Entity entity = Minecraft.getInstance().player;
         if (entity != null && entity == Minecraft.getInstance().cameraEntity) {
            if (EditorUI.isActive()) {
               if (!EditorUI.isMovingCamera()) {
                  Vec3 start = entity.getEyePosition();
                  this.update(false);
                  if (this.smoothingAmount > 1) {
                     LongArrayList positions;
                     for (positions = new LongArrayList(); !this.lastPositionsForSmoothing.isEmpty(); this.lastPositionsForSmoothing.removeElements(0, 3)) {
                        double sumX = 0.0;
                        double sumY = 0.0;
                        double sumZ = 0.0;
                        int count = 0;

                        for (int j = 0; j < this.lastPositionsForSmoothing.size(); j += 3) {
                           double targetX = this.lastPositionsForSmoothing.getDouble(j);
                           double targetY = this.lastPositionsForSmoothing.getDouble(j + 1);
                           double targetZ = this.lastPositionsForSmoothing.getDouble(j + 2);
                           double deltaX = targetX - start.x;
                           double deltaY = targetY - start.y;
                           double deltaZ = targetZ - start.z;
                           Vec3 normalizedLook = new Vec3(deltaX, deltaY, deltaZ).normalize();
                           if (!normalizedLook.equals(Vec3.ZERO)) {
                              sumX += normalizedLook.x;
                              sumY += normalizedLook.y;
                              sumZ += normalizedLook.z;
                              count++;
                           }
                        }

                        if (count == 0) {
                           break;
                        }

                        Vector3d smoothedLook = new Vector3d(sumX / count, sumY / count, sumZ / count);
                        RayCaster.RaycastResult raycastResult = RayCaster.raycast(
                           entity.level(), new Vector3d(start.x, start.y, start.z), smoothedLook, false, this.includeFluids, this.includeNonSolid
                        );
                        if (raycastResult != null) {
                           this.updateFromRaycastResult(raycastResult, entity, positions);
                        }
                     }

                     if (!positions.isEmpty()) {
                        try {
                           this.inputPositions.put(positions.toLongArray());
                        } catch (InterruptedException var25) {
                           throw new RuntimeException(var25);
                        }
                     }
                  }

                  this.stopTaskWhenInputEmpty.set(true);
                  long waitingSince = System.currentTimeMillis();

                  while (!this.stoppedTask.get()) {
                     LockSupport.parkNanos("waiting for tool async path to finish task", 100000L);
                     long now = System.currentTimeMillis();
                     if (now < waitingSince || now > waitingSince + 5000L) {
                        break;
                     }
                  }

                  this.leakChecker.disarm();
                  this.pather.update();
               }
            }
         }
      }
   }

   public void update() {
      this.update(true);
   }

   public void update(boolean callUpdate) {
      if (!this.stopTask.get()) {
         if (callUpdate) {
            this.pather.update();
         }

         Entity entity = Minecraft.getInstance().player;
         if (entity != null && entity == Minecraft.getInstance().cameraEntity) {
            if (EditorUI.isActive()) {
               if (!EditorUI.isMovingCamera()) {
                  Vec3 currentLookDirection = EditorUI.getMouseLookVector();
                  if (currentLookDirection != null) {
                     Vec3 start = entity.getEyePosition();
                     if (this.lastLookDirection != null && this.lastPosition != null) {
                        List<Vec2> mousePositions = EditorUI.imguiGlfw.getCapturedInterframeMousePositions();
                        LongArrayList positions = new LongArrayList();

                        for (Vec2 mousePosition : mousePositions) {
                           Vec3 lookDirection = EditorUI.getMouseLookVector(mousePosition.x, mousePosition.y);
                           if (lookDirection != null) {
                              double dot = this.lastLookDirection.dot(lookDirection);
                              double angleChange = Math.toDegrees(Math.acos(dot));
                              int steps = 1;
                              if (angleChange > 1.0) {
                                 steps = (int)Math.ceil(angleChange);
                              }

                              for (int i = 1; i <= steps; i++) {
                                 float f = (float)i / steps;
                                 Vec3 look = this.lastLookDirection.lerp(lookDirection, f);
                                 RayCaster.RaycastResult raycastResult = RayCaster.raycast(
                                    entity.level(), start, look, false, this.includeFluids, this.includeNonSolid
                                 );
                                 if (raycastResult != null) {
                                    if (this.smoothingAmount <= 1) {
                                       this.updateFromRaycastResult(raycastResult, entity, positions);
                                    } else {
                                       Vec3 target = raycastResult.getLocation();

                                       for (int c = 0; c < this.smoothingAmount; c++) {
                                          int size = this.lastPositionsForSmoothing.size();
                                          double lastX = this.lastPositionsForSmoothing.getDouble(size - 3);
                                          double lastY = this.lastPositionsForSmoothing.getDouble(size - 2);
                                          double lastZ = this.lastPositionsForSmoothing.getDouble(size - 1);
                                          double dx = target.x - lastX;
                                          double dy = target.y - lastY;
                                          double dz = target.z - lastZ;
                                          double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                                          if (distance < 0.1) {
                                             break;
                                          }

                                          double newX = lastX + dx * 0.1 / distance;
                                          double newY = lastY + dy * 0.1 / distance;
                                          double newZ = lastZ + dz * 0.1 / distance;
                                          this.lastPositionsForSmoothing.add(newX);
                                          this.lastPositionsForSmoothing.add(newY);
                                          this.lastPositionsForSmoothing.add(newZ);
                                          if (this.lastPositionsForSmoothing.size() > this.smoothingAmount * 3) {
                                             this.lastPositionsForSmoothing.removeElements(0, 3);
                                          }

                                          if (this.lastPositionsForSmoothing.size() >= this.smoothingAmount * 3) {
                                             double sumX = 0.0;
                                             double sumY = 0.0;
                                             double sumZ = 0.0;
                                             int count = 0;

                                             for (int j = 0; j < this.lastPositionsForSmoothing.size(); j += 3) {
                                                double targetX = this.lastPositionsForSmoothing.getDouble(j);
                                                double targetY = this.lastPositionsForSmoothing.getDouble(j + 1);
                                                double targetZ = this.lastPositionsForSmoothing.getDouble(j + 2);
                                                double deltaX = targetX - start.x;
                                                double deltaY = targetY - start.y;
                                                double deltaZ = targetZ - start.z;
                                                Vec3 normalizedLook = new Vec3(deltaX, deltaY, deltaZ).normalize();
                                                if (!normalizedLook.equals(Vec3.ZERO)) {
                                                   sumX += normalizedLook.x;
                                                   sumY += normalizedLook.y;
                                                   sumZ += normalizedLook.z;
                                                   count++;
                                                }
                                             }

                                             if (count != 0) {
                                                Vector3d smoothedLook = new Vector3d(sumX / count, sumY / count, sumZ / count);
                                                raycastResult = RayCaster.raycast(
                                                   entity.level(),
                                                   new Vector3d(start.x, start.y, start.z),
                                                   smoothedLook,
                                                   false,
                                                   this.includeFluids,
                                                   this.includeNonSolid
                                                );
                                                if (raycastResult != null) {
                                                   this.updateFromRaycastResult(raycastResult, entity, positions);
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }

                              this.lastLookDirection = lookDirection;
                           }
                        }

                        if (!positions.isEmpty()) {
                           try {
                              this.inputPositions.put(positions.toLongArray());
                           } catch (InterruptedException var63) {
                              throw new RuntimeException(var63);
                           }
                        }
                     } else {
                        RayCaster.RaycastResult raycastResult = RayCaster.raycast(
                           entity.level(), start, currentLookDirection, false, this.includeFluids, this.includeNonSolid
                        );
                        if (raycastResult != null) {
                           this.lastLookDirection = currentLookDirection;
                           this.lastPosition = raycastResult.getPositionWithinBlock();

                           try {
                              this.inputPositions.put(new long[]{raycastResult.blockPos().asLong()});
                              if (this.smoothingAmount > 1) {
                                 this.lastPositionsForSmoothing.add(raycastResult.getLocation().x);
                                 this.lastPositionsForSmoothing.add(raycastResult.getLocation().y);
                                 this.lastPositionsForSmoothing.add(raycastResult.getLocation().z);
                              }
                           } catch (InterruptedException var64) {
                              throw new RuntimeException(var64);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void updateFromRaycastResult(RayCaster.RaycastResult raycastResult, Entity entity, LongArrayList positions) {
      Vec3 withinBlock = raycastResult.getPositionWithinBlock();
      ToolPatherPoint.smartDDASkipFrom(
         entity.level(), this.lastPosition, withinBlock, this.includeNonSolid, (x, y, z) -> positions.add(BlockPos.asLong(x, y, z))
      );
      this.lastPosition = withinBlock;
   }

   public record AsyncTask(
      ArrayBlockingQueue<long[]> in, AtomicBoolean stopTask, AtomicBoolean stopTaskWhenInputEmpty, AtomicBoolean stoppedTask, AsyncToolPather pather
   ) implements Runnable {
      @Override
      public void run() {
         try {
            while (!this.stopTask.get()) {
               long[] firstPosition = this.in.poll(10L, TimeUnit.MILLISECONDS);
               if (firstPosition == null) {
                  if (!this.stopTaskWhenInputEmpty.get()) {
                     continue;
                  }

                  return;
               }

               if (firstPosition.length != 1) {
                  throw new FaultyImplementationError();
               }

               this.pather.acceptInitial(firstPosition[0]);
               break;
            }

            while (!this.stopTask.get()) {
               long[] polled = this.in.poll(10L, TimeUnit.MILLISECONDS);
               if (polled != null) {
                  this.pather.accept(polled);
               } else if (this.stopTaskWhenInputEmpty.get()) {
                  return;
               }
            }
         } catch (Throwable var5) {
            Axiom.LOGGER.error("Error inside AsyncToolPathProvider#AsyncTask", var5);
         } finally {
            this.stopTask.set(true);
            this.stoppedTask.set(true);
            this.stopTaskWhenInputEmpty.set(true);
         }
      }
   }
}
