package com.moulberry.axiom.services;

import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiomclientapi.CustomTool;
import java.util.EnumSet;
import net.minecraft.nbt.CompoundTag;
import org.joml.Matrix4f;

public class ToolRegistryService implements com.moulberry.axiomclientapi.service.ToolRegistryService {
   public void register(CustomTool customTool) {
      ToolManager.addTool(new ToolRegistryService.WrappedCustomTool(customTool));
   }

   private record WrappedCustomTool(CustomTool customTool) implements Tool {
      @Override
      public UserAction.ActionResult callAction(UserAction action, Object object) {
         switch (action) {
            case RIGHT_MOUSE:
               if (this.customTool.callUseTool()) {
                  return UserAction.ActionResult.USED_STOP;
               }
               break;
            case ENTER:
               if (this.customTool.callConfirm()) {
                  return UserAction.ActionResult.USED_STOP;
               }
               break;
            case DELETE:
               if (this.customTool.callDelete()) {
                  return UserAction.ActionResult.USED_STOP;
               }
         }

         return UserAction.ActionResult.NOT_HANDLED;
      }

      @Override
      public void displayImguiOptions() {
         this.customTool.displayImguiOptions();
      }

      @Override
      public void reset() {
         this.customTool.reset();
      }

      @Override
      public void render(AxiomWorldRenderContext rc) {
         this.customTool.render(rc.rawCameraDontUse(), rc.partialTick(), rc.nanos(), rc.poseStack(), new Matrix4f(rc.projection()));
      }

      @Override
      public String name() {
         return this.customTool.name();
      }

      @Override
      public void writeSettings(CompoundTag tag) {
         throw new UnsupportedOperationException();
      }

      @Override
      public void loadSettings(CompoundTag tag) {
      }

      @Override
      public char iconChar() {
         return '\ue912';
      }

      @Override
      public String keybindId() {
         return null;
      }

      @Override
      public EnumSet<AxiomPermission> requiredPermissions() {
         return EnumSet.of(AxiomPermission.TOOL);
      }
   }
}
