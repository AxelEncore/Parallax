package com.moulberry.axiom.tools.lasso_select;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.rasterization.Rasterization2D;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.AutoCleaningDynamicTexture;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.utils.FramebufferUtils;
import imgui.moulberry92.ImGui;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2i;

public class LassoSelect implements Tool {
   private byte[] points = null;
   private AutoCleaningDynamicTexture lassoTexture = null;
   private Vector2i nextScreenPos = null;
   private Vector2i lastScreenPos = null;
   private Vector2i startScreenPos = null;
   public static List<PendingLassoSelect> pendingDepthBuffer = new ArrayList<>();
   private final int[] mode = new int[]{1};
   private final int[] depth = new int[]{1};
   private boolean includeNonSolid = false;

   @Override
   public void reset() {
      if (this.lassoTexture != null) {
         this.lassoTexture.close();
         this.lassoTexture = null;
      }

      this.lastScreenPos = null;
      this.startScreenPos = null;
      this.points = null;
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            if (this.points != null) {
               return UserAction.ActionResult.USED_STOP;
            } else {
               this.reset();
               RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
               int width = target.width;
               int height = target.height;
               if (width > 0 && height > 0) {
                  this.points = new byte[width * height];
                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.USED_STOP;
            }
         case ENTER:
            if (this.points == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.confirm();
            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
            if (this.points != null) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   private void confirm() {
      RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
      int width = target.width;
      int height = target.height;
      if (this.points.length == width * height && this.depth[0] != 0) {
         if (this.lastScreenPos != null && this.startScreenPos != null) {
            if (this.nextScreenPos != null && !this.nextScreenPos.equals(this.lastScreenPos) && !this.nextScreenPos.equals(this.startScreenPos)) {
               drawLine(this.points, width, new Vector2i(this.lastScreenPos), new Vector2i(this.nextScreenPos));
               drawLine(this.points, width, new Vector2i(this.nextScreenPos), new Vector2i(this.startScreenPos));
            } else {
               drawLine(this.points, width, new Vector2i(this.lastScreenPos), new Vector2i(this.startScreenPos));
            }
         }

         PendingLassoSelect pendingLassoSelect = new PendingLassoSelect(
            this.points, width, height, this.depth[0], this.includeNonSolid || this.depth[0] < 0, this.mode[0]
         );
         pendingDepthBuffer.add(pendingLassoSelect);
      }

      this.reset();
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (this.points == null) {
         Selection.render(rc, 7);
      } else {
         Selection.render(rc, 4);
         if (Tool.cancelUsing()) {
            this.reset();
         } else {
            RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
            int width = target.width;
            int height = target.height;
            if (this.points.length != width * height) {
               this.reset();
            } else {
               boolean changed = false;
               Vec2 currentMousePosition = new Vec2(EditorUI.getIO().getMousePosX(), EditorUI.getIO().getMousePosY());
               if (Tool.isMouseDown(1)) {
                  List<Vec2> mousePositions = new ArrayList<>(EditorUI.imguiGlfw.getCapturedInterframeMousePositions());
                  mousePositions.add(currentMousePosition);

                  for (Vec2 mousePosition : mousePositions) {
                     Vec2 screenPos = EditorUI.getMouseViewportFraction(mousePosition.x, mousePosition.y);
                     int x = (int)(screenPos.x * width);
                     int y = (int)(screenPos.y * height);
                     if (x < 0) {
                        x = 0;
                     }

                     if (y < 0) {
                        y = 0;
                     }

                     if (x >= width) {
                        x = width - 1;
                     }

                     if (y >= height) {
                        y = height - 1;
                     }

                     Vector2i screenPosInt = new Vector2i(x, y);
                     if (this.startScreenPos == null) {
                        this.startScreenPos = new Vector2i(screenPosInt);
                     }

                     if (this.lastScreenPos != null) {
                        if (this.lastScreenPos.x != screenPosInt.x || this.lastScreenPos.y != screenPosInt.y) {
                           drawLine(this.points, width, new Vector2i(this.lastScreenPos), new Vector2i(screenPosInt));
                           this.lastScreenPos = new Vector2i(screenPosInt);
                           changed = true;
                        }
                     } else {
                        this.points[x + y * width] = -1;
                        this.lastScreenPos = new Vector2i(screenPosInt);
                        changed = true;
                     }
                  }

                  this.nextScreenPos = null;
               } else {
                  Vec2 screenPosx = EditorUI.getMouseViewportFraction(currentMousePosition.x, currentMousePosition.y);
                  int xx = (int)(screenPosx.x * width);
                  int yx = (int)(screenPosx.y * height);
                  if (xx > 0 && yx > 0 && xx < width - 1 && yx < height - 1) {
                     Vector2i nextScreenPos = new Vector2i(xx, yx);
                     if (this.nextScreenPos == null || this.nextScreenPos.x != nextScreenPos.x || this.nextScreenPos.y != nextScreenPos.y) {
                        this.nextScreenPos = nextScreenPos;
                        changed = true;
                     }
                  } else if (this.nextScreenPos != null) {
                     this.nextScreenPos = null;
                     changed = true;
                  }
               }

               if (this.lassoTexture == null) {
                  this.lassoTexture = new AutoCleaningDynamicTexture(width, height, true);
                  changed = true;
               }

               NativeImage pixels = this.lassoTexture.getPixels();
               if (pixels == null || pixels.getWidth() != width || pixels.getHeight() != height) {
                  this.lassoTexture.close();
                  this.lassoTexture = new AutoCleaningDynamicTexture(width, height, true);
                  pixels = Objects.requireNonNull(this.lassoTexture.getPixels());
                  changed = true;
               }

               if (changed) {
                  byte[] data;
                  if (this.lastScreenPos != null && this.startScreenPos != null) {
                     data = Arrays.copyOf(this.points, this.points.length);
                     if (this.nextScreenPos != null && !this.nextScreenPos.equals(this.lastScreenPos) && !this.nextScreenPos.equals(this.startScreenPos)) {
                        drawLine(data, width, new Vector2i(this.lastScreenPos), new Vector2i(this.nextScreenPos));
                        drawLine(data, width, new Vector2i(this.nextScreenPos), new Vector2i(this.startScreenPos));
                     } else {
                        drawLine(data, width, new Vector2i(this.lastScreenPos), new Vector2i(this.startScreenPos));
                     }
                  } else {
                     data = this.points;
                  }

                  for (int yxx = 0; yxx < height; yxx++) {
                     boolean inside = false;
                     int countUp = 0;
                     int countDown = 0;

                     for (int xxx = 0; xxx < width; xxx++) {
                        byte value = data[xxx + yxx * width];
                        if (value != 0) {
                           if (this.points[xxx + yxx * width] != 0) {
                              pixels.setPixelRGBA(xxx, height - yxx - 1, ColourUtils.argbToAbgr(-6882));
                           } else {
                              pixels.setPixelRGBA(xxx, height - yxx - 1, ColourUtils.argbToAbgr(-2130713314));
                           }

                           if (value != -1) {
                              countDown += value & 15;
                              countUp += value >> 4 & 15;
                              if (countUp > 0 && countDown > 0) {
                                 int min = Math.min(countUp, countDown);
                                 if ((min & 1) != 0) {
                                    inside = !inside;
                                 }

                                 countUp -= min;
                                 countDown -= min;
                              }
                           }
                        } else if (inside) {
                           pixels.setPixelRGBA(xxx, height - yxx - 1, ColourUtils.argbToAbgr(-2130713314));
                        } else {
                           pixels.setPixelRGBA(xxx, height - yxx - 1, ColourUtils.argbToAbgr(0));
                        }
                     }
                  }

                  this.lassoTexture.upload();
               }

               FramebufferUtils.blitToMainBlend(new AxiomGpuTexture(this.lassoTexture.getId()), width, height);
            }
         }
      }
   }

   private static void drawLine(byte[] data, int stride, Vector2i from, Vector2i to) {
      Rasterization2D.dda(from, to, (x1, y1) -> {
         if (x1 != from.x || y1 != from.y) {
            int lastIndex = from.x + from.y * stride;
            int index = x1 + y1 * stride;
            byte lastValue = data[lastIndex];
            byte value = data[index];
            if (from.y < y1) {
               if (value != 0 && value != -1) {
                  int down = (value & 15) + 1;
                  int up = value >> 4 & 15;
                  data[index] = (byte)((up & 15) << 4 | down & 15);
               } else {
                  data[index] = 1;
               }

               if (lastValue != 0 && lastValue != -1) {
                  int down = lastValue & 15;
                  int up = (lastValue >> 4 & 15) + 1;
                  data[lastIndex] = (byte)((up & 15) << 4 | down & 15);
               } else {
                  data[lastIndex] = 16;
               }
            } else if (from.y > y1) {
               if (value != 0 && value != -1) {
                  int down = value & 15;
                  int up = (value >> 4 & 15) + 1;
                  data[index] = (byte)((up & 15) << 4 | down & 15);
               } else {
                  data[index] = 16;
               }

               if (lastValue != 0 && lastValue != -1) {
                  int down = (lastValue & 15) + 1;
                  int up = lastValue >> 4 & 15;
                  data[lastIndex] = (byte)((up & 15) << 4 | down & 15);
               } else {
                  data[lastIndex] = 1;
               }
            } else if (value == 0) {
               data[index] = -1;
            }

            from.x = x1;
            from.y = y1;
         }
      });
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.lasso_select"));
      ImGui.sliderInt(AxiomI18n.get("axiom.tool.lasso_select.depth"), this.depth, -64, 64);
      ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
      if (this.depth[0] > 0 && ImGui.checkbox(AxiomI18n.get("axiom.tool.lasso_select.include_non_solid"), this.includeNonSolid)) {
         this.includeNonSolid = !this.includeNonSolid;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.selection"));
      ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.selection.mode"),
         this.mode,
         new String[]{
            AxiomI18n.get("axiom.tool.selection.replace"),
            AxiomI18n.get("axiom.tool.selection.add"),
            AxiomI18n.get("axiom.tool.selection.subtract"),
            AxiomI18n.get("axiom.tool.selection.intersect")
         }
      );
      if (this.points != null) {
         ImGui.separator();
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.confirm_paren") + Keybinds.CONFIRM.longKeyIdentifier() + ")")) {
            this.confirm();
         }
      }
   }

   @Override
   public String listenForEsc() {
      return this.points != null ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.lasso_select");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
   }

   @Override
   public void loadSettings(CompoundTag tag) {
   }

   @Override
   public char iconChar() {
      return '\ue91d';
   }

   @Override
   public String keybindId() {
      return "lasso_select";
   }

   @Override
   public int defaultKeybind() {
      return 76;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_LASSOSELECT);
   }
}
