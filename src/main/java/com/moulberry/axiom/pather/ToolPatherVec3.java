package com.moulberry.axiom.pather;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.editor.EditorUI;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ToolPatherVec3 {
   private Vec3 lastPosition;
   private Vec3 lastLookDirection = null;
   private Vec2 lastMousePosition = null;

   public void update(Consumer<RayCaster.RaycastResult> consumer) {
      Entity entity = Minecraft.getInstance().player;
      if (entity != null && entity == Minecraft.getInstance().cameraEntity) {
         if (EditorUI.isActive()) {
            if (!EditorUI.isMovingCamera()) {
               Vec3 currentLookDirection = EditorUI.getMouseLookVector();
               if (currentLookDirection != null) {
                  Vec3 start = entity.getEyePosition();
                  if (this.lastLookDirection != null && this.lastPosition != null) {
                     for (Vec2 mousePosition : EditorUI.imguiGlfw.getCapturedInterframeMousePositions()) {
                        float mouseDX = mousePosition.x - this.lastMousePosition.x;
                        float mouseDY = mousePosition.y - this.lastMousePosition.y;
                        if (mouseDX != 0.0F || mouseDY != 0.0F) {
                           float distance = (float)Math.sqrt(mouseDX * mouseDX + mouseDY * mouseDY);
                           int mouseSteps = (int)Math.max(1.0, Math.ceil(distance));

                           for (int j = 1; j <= mouseSteps; j++) {
                              float mouseX = this.lastMousePosition.x + mouseDX * j / mouseSteps;
                              float mouseY = this.lastMousePosition.y + mouseDY * j / mouseSteps;
                              Vec3 lookDirection = EditorUI.getMouseLookVector(mouseX, mouseY);
                              if (lookDirection != null) {
                                 double dot = this.lastLookDirection.dot(lookDirection);
                                 double angleChange = Math.toDegrees(Math.acos(dot));
                                 int steps = 1;
                                 if (angleChange > 1.0) {
                                    steps = (int)Math.ceil(angleChange);
                                 }

                                 for (int i = 1; i <= steps; i++) {
                                    double f = (float)i / steps;
                                    Vec3 look = this.lastLookDirection.lerp(lookDirection, f);
                                    RayCaster.RaycastResult raycastResult = RayCaster.raycast(entity.level(), start, look, false, true, false);
                                    if (raycastResult != null && !this.lastPosition.equals(raycastResult.getLocation())) {
                                       this.lastPosition = raycastResult.getLocation();
                                       consumer.accept(raycastResult);
                                    }
                                 }

                                 this.lastLookDirection = lookDirection;
                              }
                           }

                           this.lastMousePosition = mousePosition;
                        }
                     }
                  } else {
                     RayCaster.RaycastResult raycastResult = RayCaster.raycast(entity.level(), start, currentLookDirection, false, true, false);
                     if (raycastResult != null) {
                        this.lastLookDirection = currentLookDirection;
                        this.lastMousePosition = new Vec2(EditorUI.getIO().getMousePosX(), EditorUI.getIO().getMousePosY());
                        this.lastPosition = raycastResult.getLocation();
                        consumer.accept(raycastResult);
                     }
                  }
               }
            }
         }
      }
   }
}
