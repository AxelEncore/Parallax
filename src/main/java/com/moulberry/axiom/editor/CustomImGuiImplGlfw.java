package com.moulberry.axiom.editor;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.KeyPressOverlay;
import com.moulberry.axiom.Toasts;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.capabilities.BuildSymmetry;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.editor.keybinds.Keybind;
import com.moulberry.axiom.editor.keybinds.KeybindCategory;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.windows.clipboard.BlueprintBrowserWindow;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.utils.AsyncFileDialogs;
import com.moulberry.axiom.utils.PositionUtils;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiIO;
import imgui.moulberry92.ImGuiPlatformIO;
import imgui.moulberry92.ImGuiViewport;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.callback.ImPlatformFuncViewport;
import imgui.moulberry92.callback.ImPlatformFuncViewportFloat;
import imgui.moulberry92.callback.ImPlatformFuncViewportImVec2;
import imgui.moulberry92.callback.ImPlatformFuncViewportString;
import imgui.moulberry92.callback.ImPlatformFuncViewportSuppBoolean;
import imgui.moulberry92.callback.ImPlatformFuncViewportSuppImVec2;
import imgui.moulberry92.callback.ImStrConsumer;
import imgui.moulberry92.callback.ImStrSupplier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWCharModsCallback;
import org.lwjgl.glfw.GLFWCursorEnterCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMonitorCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowFocusCallback;

public class CustomImGuiImplGlfw {
   private static final String OS = System.getProperty("os.name", "generic").toLowerCase();
   protected static final boolean IS_WINDOWS = OS.contains("win");
   protected static final boolean IS_APPLE = OS.contains("mac") || OS.contains("darwin");
   private long mainWindowPtr;
   private boolean glfwHawWindowTopmost;
   private boolean glfwHasWindowAlpha;
   private boolean glfwHasPerMonitorDpi;
   private boolean glfwHasFocusWindow;
   private boolean glfwHasFocusOnShow;
   private boolean glfwHasMonitorWorkArea;
   private boolean glfwHasOsxWindowPosFix;
   private final int[] winWidth = new int[1];
   private final int[] winHeight = new int[1];
   private final int[] fbWidth = new int[1];
   private final int[] fbHeight = new int[1];
   private final float[] windowScaleX = new float[1];
   private final float[] windowScaleY = new float[1];
   private final long[] mouseCursors = new long[11];
   private final long[] keyOwnerWindows = new long[512];
   private final boolean[] keyPressedGame = new boolean[512];
   private final boolean[] mouseJustPressed = new boolean[5];
   private final ImVec2 mousePosBackup = new ImVec2();
   private final double[] mouseX = new double[1];
   private final double[] mouseY = new double[1];
   private final int[] windowX = new int[1];
   private final int[] windowY = new int[1];
   private final int[] monitorX = new int[1];
   private final int[] monitorY = new int[1];
   private final int[] monitorWorkAreaX = new int[1];
   private final int[] monitorWorkAreaY = new int[1];
   private final int[] monitorWorkAreaWidth = new int[1];
   private final int[] monitorWorkAreaHeight = new int[1];
   private final float[] monitorContentScaleX = new float[1];
   private final float[] monitorContentScaleY = new float[1];
   private GLFWWindowFocusCallback prevUserCallbackWindowFocus = null;
   private GLFWMouseButtonCallback prevUserCallbackMouseButton = null;
   private GLFWScrollCallback prevUserCallbackScroll = null;
   private GLFWCursorPosCallback prevUserCallbackCursorPos = null;
   private GLFWKeyCallback prevUserCallbackKey = null;
   private GLFWCharModsCallback prevUserCallbackCharMods = null;
   private GLFWCharCallback prevUserCallbackChar = null;
   private GLFWMonitorCallback prevUserCallbackMonitor = null;
   private GLFWCursorEnterCallback prevUserCallbackCursorEnter = null;
   private boolean callbacksInstalled = false;
   private boolean wantUpdateMonitors = true;
   private double time = 0.0;
   private long mouseWindowPtr;
   private CustomImGuiImplGlfw.MouseHandledBy grabbed = null;
   private int ignoreMouseMovements = 0;
   private boolean releasedAllKeysBecauseOfDialog = false;
   private boolean releasedAllKeysBecauseOfDisable = false;
   private final double[] grabbedOriginalMouseX = new double[1];
   private final double[] grabbedOriginalMouseY = new double[1];
   private int grabLinkedKey = -1;
   private List<Vec2> interframeMousePositions = new ArrayList<>();
   private List<Vec2> capturedInterframeMousePositions = new ArrayList<>();
   public float contentScale = 1.0F;
   private static double grabbedLastMouseX = 0.0;
   private static double grabbedLastMouseY = 0.0;
   private static double grabbedCurrMouseX = 0.0;
   private static double grabbedCurrMouseY = 0.0;
   private boolean viewportWindowsHidden = false;

   public CustomImGuiImplGlfw.MouseHandledBy getMouseHandledBy() {
      if (!EditorUI.isActive()) {
         return CustomImGuiImplGlfw.MouseHandledBy.GAME;
      } else if (this.grabbed != null) {
         return this.grabbed;
      } else {
         return EditorUI.getIO().getWantCaptureMouse() ? CustomImGuiImplGlfw.MouseHandledBy.IMGUI : CustomImGuiImplGlfw.MouseHandledBy.BOTH;
      }
   }

   public boolean isGrabbed() {
      return this.grabbed != null;
   }

   public void ungrab() {
      if (this.grabbed != null) {
         this.grabbed = null;
         this.grabLinkedKey = 0;
         GLFW.glfwSetInputMode(this.mainWindowPtr, 208897, 212993);
         GLFW.glfwSetCursorPos(this.mainWindowPtr, this.grabbedOriginalMouseX[0], this.grabbedOriginalMouseY[0]);
      }
   }

   public void setGrabbed(boolean passthroughToGame, int grabLinkedKey, double x, double y) {
      if (grabLinkedKey != 0) {
         if (grabLinkedKey < 0) {
            if (GLFW.glfwGetMouseButton(this.mainWindowPtr, -grabLinkedKey - 1) == 0) {
               this.ungrab();
               return;
            }
         } else if (GLFW.glfwGetKey(this.mainWindowPtr, grabLinkedKey) == 0) {
            this.ungrab();
            return;
         }
      }

      if (this.grabbed == null) {
         this.grabbed = passthroughToGame ? CustomImGuiImplGlfw.MouseHandledBy.GAME : CustomImGuiImplGlfw.MouseHandledBy.EDITOR_GRABBED;
         if (grabLinkedKey != 0) {
            this.grabLinkedKey = grabLinkedKey;
         }

         if (x >= 0.0 && y >= 0.0) {
            this.grabbedOriginalMouseX[0] = x;
            this.grabbedOriginalMouseY[0] = y;
         } else {
            GLFW.glfwGetCursorPos(this.mainWindowPtr, this.grabbedOriginalMouseX, this.grabbedOriginalMouseY);
         }

         GLFW.glfwSetInputMode(this.mainWindowPtr, 208897, 212995);
         this.ignoreMouseMovements = 2;
         Minecraft.getInstance().mouseHandler.setIgnoreFirstMove();
      }
   }

   public double getGrabbedMouseDeltaX() {
      double delta = grabbedCurrMouseX - grabbedLastMouseX;
      grabbedLastMouseX = grabbedCurrMouseX;
      return delta;
   }

   public double getGrabbedMouseDeltaY() {
      double delta = grabbedCurrMouseY - grabbedLastMouseY;
      grabbedLastMouseY = grabbedCurrMouseY;
      return delta;
   }

   public void mouseButtonCallback(long windowId, int button, int action, int mods) {
      if (!AsyncFileDialogs.hasDialog()) {
         if (action != 0) {
            KeyPressOverlay.addKey(-button - 1);
         } else {
            KeyPressOverlay.removeKey(-button - 1);
         }

         if (!EditorUI.isActive()) {
            if (this.prevUserCallbackMouseButton != null && windowId == this.mainWindowPtr) {
               this.prevUserCallbackMouseButton.invoke(windowId, button, action, mods);
            }
         } else {
            if (this.grabbed != null && action == 0 && this.grabLinkedKey < 0 && button == -this.grabLinkedKey - 1) {
               this.ungrab();
            }

            CustomImGuiImplGlfw.MouseHandledBy handledBy = this.getMouseHandledBy();
            this.updateKeyModifiers(EditorUI.getIO(), windowId);
            if (handledBy.allowGame() && this.prevUserCallbackMouseButton != null && windowId == this.mainWindowPtr) {
               this.prevUserCallbackMouseButton.invoke(windowId, button, action, mods);
            }

            if (handledBy.allowImgui() && action == 1 && button >= 0 && button < this.mouseJustPressed.length) {
               this.mouseJustPressed[button] = true;
            }
         }
      }
   }

   public void scrollCallback(long windowId, double xOffset, double yOffset) {
      if (!AsyncFileDialogs.hasDialog()) {
         if (yOffset != 0.0) {
            int id = yOffset < 0.0 ? -201 : -200;
            KeyPressOverlay.addKey(id);
            KeyPressOverlay.removeKey(id);
         }

         if (EditorUI.isActive()) {
            if (xOffset != 0.0) {
               int id = xOffset > 0.0 ? -202 : -203;
               KeyPressOverlay.addKey(id);
               KeyPressOverlay.removeKey(id);
            }

            ImGuiIO io = EditorUI.getIO();
            io.setMouseWheelH(io.getMouseWheelH() + (float)xOffset);
            io.setMouseWheel(io.getMouseWheel() + (float)yOffset);
            if (Minecraft.getInstance().screen == null || !EditorUI.isMainFrameHovered()) {
               return;
            }
         }

         if (this.prevUserCallbackScroll != null && windowId == this.mainWindowPtr) {
            this.prevUserCallbackScroll.invoke(windowId, xOffset, yOffset);
         }
      }
   }

   public void keyCallback(long windowId, int key, int scancode, int action, int mods) {
      if (!AsyncFileDialogs.hasDialog()) {
         if (key >= 32 && key <= 348) {
            boolean shiftMod = (mods & 1) != 0;
            boolean ctrlMod = (mods & 2) != 0;
            boolean altMod = (mods & 4) != 0;
            boolean superMod = (mods & 8) != 0;
            boolean keyInBoundsForGame = key >= 0 && key < this.keyPressedGame.length;
            if (action != 0) {
               KeyPressOverlay.addKey(key);
            } else {
               KeyPressOverlay.removeKey(key);
            }

            ImGuiIO io = EditorUI.getIO();
            if (this.grabbed != null && action == 0 && this.grabLinkedKey > 0 && key == this.grabLinkedKey) {
               this.ungrab();
            }

            if (EditorUI.isActive() && Minecraft.getInstance().screen == null) {
               if (action == 1 && key != 256 && ImGuiHelper.getEditingKeybind() != null) {
                  shiftMod &= key != 340 && key != 344;
                  ctrlMod &= key != 341 && key != 345;
                  altMod &= key != 342 && key != 346;
                  superMod &= key != 343 && key != 347;
                  if (Minecraft.ON_OSX) {
                     boolean temp = ctrlMod;
                     ctrlMod = superMod;
                     superMod = temp;
                  }

                  ImGuiHelper.getEditingKeybind().set(key, shiftMod, ctrlMod, altMod, superMod);
               } else {
                  if (action == 1 && ImGuiHelper.getWantsSpecialInput()) {
                     if (key == 259 && ImGuiHelper.backspaceInput(mods)) {
                        return;
                     }

                     if (key == 32) {
                        return;
                     }
                  }

                  if (EditorUI.getIO().getWantTextInput() || key != 256 || action == 0 || UserAction.ESCAPE.call(null) != UserAction.ActionResult.USED_STOP) {
                     if (action != 0 && GLFW.glfwGetKey(windowId, 292) != 0) {
                        this.prevUserCallbackKey.invoke(windowId, key, scancode, action, mods);
                        if (keyInBoundsForGame) {
                           this.keyPressedGame[key] = action != 0;
                        }
                     } else {
                        boolean passToMinecraft = (
                              EditorUI.allowGameInputWhileCaptureKeyboard()
                                 || EditorUI.isMainFrameHovered() && io.getMouseDown(0)
                                 || !io.getWantCaptureKeyboard()
                           )
                           && !io.getWantTextInput();
                        if ((key == 340 || key == 344) && BlueprintBrowserWindow.selectMultiple) {
                           passToMinecraft = false;
                        }

                        if (action == 0) {
                           if (keyInBoundsForGame && this.keyPressedGame[key]) {
                              passToMinecraft = true;
                           }
                        } else if (passToMinecraft) {
                           label314:
                           for (KeybindCategory category : Keybinds.categories) {
                              if (category.preventPassToGame()) {
                                 for (Keybind keybind : category.keybinds()) {
                                    if (keybind.wouldBePressed(key, shiftMod, ctrlMod, altMod, superMod)) {
                                       passToMinecraft = false;
                                       break label314;
                                    }
                                 }
                              }
                           }
                        }

                        if (passToMinecraft && this.prevUserCallbackKey != null && windowId == this.mainWindowPtr) {
                           this.prevUserCallbackKey.invoke(windowId, key, scancode, action, mods);
                           if (keyInBoundsForGame) {
                              this.keyPressedGame[key] = action != 0;
                           }
                        }

                        this.updateKeyModifiers(io, windowId);
                        int imguiKey = glfwKeyToImGuiKey(key);
                        if (key >= 0 && key < this.keyOwnerWindows.length) {
                           if (action != 1 && action != 2) {
                              if (action == 0) {
                                 if (imguiKey != 0) {
                                    io.addKeyEvent(imguiKey, false);
                                    io.setKeyEventNativeData(imguiKey, key, scancode);
                                 }

                                 this.keyOwnerWindows[key] = 0L;
                              }
                           } else {
                              if (imguiKey != 0) {
                                 io.addKeyEvent(imguiKey, true);
                                 io.setKeyEventNativeData(imguiKey, key, scancode);
                              }

                              this.keyOwnerWindows[key] = windowId;
                           }
                        }
                     }
                  }
               }
            } else {
               if (action == 0 && key >= 0 && key < this.keyOwnerWindows.length) {
                  int imguiKey = glfwKeyToImGuiKey(key);
                  if (imguiKey != 0) {
                     io.addKeyEvent(imguiKey, false);
                  }

                  this.keyOwnerWindows[key] = 0L;
               }

               if (this.prevUserCallbackKey != null && windowId == this.mainWindowPtr) {
                  boolean pressed = action != 0;
                  boolean passToMinecraftx = true;
                  if (Minecraft.getInstance().screen == null) {
                     if (Keybinds.ROTATE_PLACEMENT.wouldBePressed(key, shiftMod, ctrlMod, altMod, superMod)) {
                        if (BuildSymmetry.isActive()) {
                           if (pressed) {
                              if (BuildSymmetry.toggleRotY()) {
                                 Toasts.addToast(
                                    new Toasts.Toast(
                                       Component.literal(AxiomI18n.get("axiom.hardcoded.enabled_rot_sym")),
                                       ResourceLocation.parse("axiom:gui/tool_swapper.png"),
                                       -16711936,
                                       200,
                                       142,
                                       0,
                                       256,
                                       256
                                    )
                                 );
                              } else {
                                 Toasts.addToast(
                                    new Toasts.Toast(
                                       Component.literal(AxiomI18n.get("axiom.hardcoded.disabled_rot_sym")),
                                       ResourceLocation.parse("axiom:gui/tool_swapper.png"),
                                       -65536,
                                       200,
                                       142,
                                       0,
                                       256,
                                       256
                                    )
                                 );
                              }
                           }

                           passToMinecraftx = false;
                        }

                        if (BuilderToolManager.isToolSlotActive()) {
                           passToMinecraftx = false;
                        }
                     }

                     if (Keybinds.FLIP_PLACEMENT.wouldBePressed(key, shiftMod, ctrlMod, altMod, superMod)) {
                        if (BuildSymmetry.isActive()) {
                           if (pressed) {
                              Entity cameraEntity = Minecraft.getInstance().cameraEntity;
                              if (cameraEntity != null) {
                                 Vec3 lookDirection = cameraEntity.getLookAngle();
                                 Direction direction = PositionUtils.orderedByNearest(lookDirection)[0];
                                 Axis axis = direction.getAxis();

                                 boolean enabled = switch (axis) {
                                    case X -> BuildSymmetry.toggleFlipX();
                                    case Y -> BuildSymmetry.toggleFlipY();
                                    case Z -> BuildSymmetry.toggleFlipZ();
                                    default -> throw new IncompatibleClassChangeError();
                                 };
                                 if (enabled) {
                                    Toasts.addToast(
                                       new Toasts.Toast(
                                          Component.literal(AxiomI18n.get("axiom.hardcoded.enabled_sp") + axis.getName().toUpperCase(Locale.ROOT) + " symmetry"),
                                          ResourceLocation.parse("axiom:gui/tool_swapper.png"),
                                          -16711936,
                                          200,
                                          142,
                                          0,
                                          256,
                                          256
                                       )
                                    );
                                 } else {
                                    Toasts.addToast(
                                       new Toasts.Toast(
                                          Component.literal(AxiomI18n.get("axiom.hardcoded.disabled_sp") + axis.getName().toUpperCase(Locale.ROOT) + " symmetry"),
                                          ResourceLocation.parse("axiom:gui/tool_swapper.png"),
                                          -65536,
                                          200,
                                          142,
                                          0,
                                          256,
                                          256
                                       )
                                    );
                                 }
                              }
                           }

                           passToMinecraftx = false;
                        }

                        if (BuilderToolManager.isToolSlotActive()) {
                           passToMinecraftx = false;
                        }
                     }

                     if (BuildSymmetry.isActive() && (key == 261 || key == 259)) {
                        if (pressed) {
                           BuildSymmetry.clear();
                        }

                        passToMinecraftx = false;
                     }

                     if (DisplayEntityManipulator.hasActiveGizmo() && (key == 261 || key == 259)) {
                        if (pressed) {
                           DisplayEntityManipulator.deleteActive();
                        }

                        passToMinecraftx = false;
                     }

                     if (MarkerEntityManipulator.hasActiveGizmo() && (key == 261 || key == 259)) {
                        if (pressed) {
                           MarkerEntityManipulator.deleteActive();
                        }

                        passToMinecraftx = false;
                     }
                  }

                  if (passToMinecraftx || action == 0 && keyInBoundsForGame && !this.keyPressedGame[key]) {
                     if (keyInBoundsForGame) {
                        this.keyPressedGame[key] = action != 0;
                     }

                     this.prevUserCallbackKey.invoke(windowId, key, scancode, action, mods);
                  }
               }
            }
         } else {
            if (this.prevUserCallbackKey != null && windowId == this.mainWindowPtr) {
               this.prevUserCallbackKey.invoke(windowId, key, scancode, action, mods);
            }
         }
      }
   }

   protected void updateKeyModifiers(ImGuiIO io, long window) {
      io.addKeyEvent(4096, GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1);
      io.addKeyEvent(8192, GLFW.glfwGetKey(window, 340) == 1 || GLFW.glfwGetKey(window, 344) == 1);
      io.addKeyEvent(16384, GLFW.glfwGetKey(window, 342) == 1 || GLFW.glfwGetKey(window, 346) == 1);
      io.addKeyEvent(32768, GLFW.glfwGetKey(window, 343) == 1 || GLFW.glfwGetKey(window, 347) == 1);
   }

   public void cursorPosCallback(long windowId, double xpos, double ypos) {
      if (!AsyncFileDialogs.hasDialog()) {
         if (!EditorUI.isActive()) {
            if (this.prevUserCallbackCursorPos != null && windowId == this.mainWindowPtr) {
               this.prevUserCallbackCursorPos.invoke(windowId, xpos, ypos);
            }
         } else {
            CustomImGuiImplGlfw.MouseHandledBy handledBy = this.getMouseHandledBy();
            if (this.ignoreMouseMovements > 0) {
               grabbedCurrMouseX = xpos;
               grabbedCurrMouseY = ypos;
               grabbedLastMouseX = xpos;
               grabbedLastMouseY = ypos;
               this.ignoreMouseMovements--;
            } else {
               if (handledBy.allowGame() && this.prevUserCallbackCursorPos != null && windowId == this.mainWindowPtr) {
                  this.prevUserCallbackCursorPos.invoke(windowId, EditorUI.getNewMouseX(xpos), EditorUI.getNewMouseY(ypos));
               }

               if (handledBy == CustomImGuiImplGlfw.MouseHandledBy.EDITOR_GRABBED) {
                  grabbedCurrMouseX = xpos;
                  grabbedCurrMouseY = ypos;
               } else {
                  this.interframeMousePositions.add(new Vec2((float)xpos, (float)ypos));
               }
            }
         }
      }
   }

   public void windowFocusCallback(long windowId, boolean focused) {
      if (this.prevUserCallbackWindowFocus != null && windowId == this.mainWindowPtr) {
         this.prevUserCallbackWindowFocus.invoke(windowId, focused);
      }

      EditorUI.getIO().addFocusEvent(focused);
   }

   public void cursorEnterCallback(long windowId, boolean entered) {
      if (this.prevUserCallbackCursorEnter != null && windowId == this.mainWindowPtr) {
         this.prevUserCallbackCursorEnter.invoke(windowId, entered);
      }

      if (entered) {
         this.mouseWindowPtr = windowId;
      }

      if (!entered && this.mouseWindowPtr == windowId) {
         this.mouseWindowPtr = 0L;
      }
   }

   public void charModsCallback(long windowId, int c, int mods) {
      if (!AsyncFileDialogs.hasDialog()) {
         if (!EditorUI.isActive()) {
            if (this.prevUserCallbackCharMods != null && windowId == this.mainWindowPtr) {
               this.prevUserCallbackCharMods.invoke(windowId, c, mods);
            }
         } else if (ImGuiHelper.getEditingKeybind() == null) {
            ImGuiIO io = EditorUI.getIO();
            if (!io.getWantCaptureKeyboard() && !io.getWantTextInput() && this.prevUserCallbackCharMods != null && windowId == this.mainWindowPtr) {
               this.prevUserCallbackCharMods.invoke(windowId, c, mods);
            }
         }
      }
   }

   public void charCallback(long windowId, int c) {
      if (!AsyncFileDialogs.hasDialog()) {
         if (!EditorUI.isActive()) {
            if (this.prevUserCallbackChar != null && windowId == this.mainWindowPtr) {
               this.prevUserCallbackChar.invoke(windowId, c);
            }
         } else if (ImGuiHelper.getEditingKeybind() == null) {
            ImGuiIO io = EditorUI.getIO();
            if (!io.getWantCaptureKeyboard() && !io.getWantTextInput() && this.prevUserCallbackChar != null && windowId == this.mainWindowPtr) {
               this.prevUserCallbackChar.invoke(windowId, c);
            }

            if (!ImGuiHelper.addInputCharacter((char)c)) {
               io.addInputCharacter(c);
            }
         }
      }
   }

   public void monitorCallback(long windowId, int event) {
      if (this.prevUserCallbackMonitor != null && windowId == this.mainWindowPtr) {
         this.prevUserCallbackMonitor.invoke(windowId, event);
      }

      this.wantUpdateMonitors = true;
   }

   public boolean init(long windowId, boolean installCallbacks) {
      this.mainWindowPtr = windowId;
      this.detectGlfwVersionAndEnabledFeatures();
      ImGuiIO io = EditorUI.getIO();
      io.addBackendFlags(2054);
      io.setBackendPlatformName("imgui_java_impl_glfw");
      io.setGetClipboardTextFn(new ImStrSupplier() {
         public String get() {
            String clipboardString = GLFW.glfwGetClipboardString(windowId);
            return clipboardString != null ? clipboardString : "";
         }
      });
      io.setSetClipboardTextFn(new ImStrConsumer() {
         public void accept(String str) {
            GLFW.glfwSetClipboardString(windowId, str);
         }
      });
      GLFWErrorCallback prevErrorCallback = GLFW.glfwSetErrorCallback(null);
      this.mouseCursors[0] = GLFW.glfwCreateStandardCursor(221185);
      this.mouseCursors[1] = GLFW.glfwCreateStandardCursor(221186);
      this.mouseCursors[2] = GLFW.glfwCreateStandardCursor(221193);
      this.mouseCursors[3] = GLFW.glfwCreateStandardCursor(221190);
      this.mouseCursors[4] = GLFW.glfwCreateStandardCursor(221189);
      this.mouseCursors[5] = GLFW.glfwCreateStandardCursor(221185);
      this.mouseCursors[6] = GLFW.glfwCreateStandardCursor(221185);
      this.mouseCursors[7] = GLFW.glfwCreateStandardCursor(221188);
      this.mouseCursors[10] = GLFW.glfwCreateStandardCursor(221185);
      GLFW.glfwSetErrorCallback(prevErrorCallback);
      if (installCallbacks) {
         this.callbacksInstalled = true;
         this.prevUserCallbackWindowFocus = GLFW.glfwSetWindowFocusCallback(windowId, this::windowFocusCallback);
         this.prevUserCallbackCursorEnter = GLFW.glfwSetCursorEnterCallback(windowId, this::cursorEnterCallback);
         this.prevUserCallbackMouseButton = GLFW.glfwSetMouseButtonCallback(windowId, this::mouseButtonCallback);
         this.prevUserCallbackScroll = GLFW.glfwSetScrollCallback(windowId, this::scrollCallback);
         this.prevUserCallbackCursorPos = GLFW.glfwSetCursorPosCallback(windowId, this::cursorPosCallback);
         this.prevUserCallbackKey = GLFW.glfwSetKeyCallback(windowId, this::keyCallback);
         this.prevUserCallbackCharMods = GLFW.glfwSetCharModsCallback(windowId, this::charModsCallback);
         this.prevUserCallbackChar = GLFW.glfwSetCharCallback(windowId, this::charCallback);
         this.prevUserCallbackMonitor = GLFW.glfwSetMonitorCallback(this::monitorCallback);
      }

      GLFW.glfwGetWindowSize(this.mainWindowPtr, this.winWidth, this.winHeight);
      GLFW.glfwGetFramebufferSize(this.mainWindowPtr, this.fbWidth, this.fbHeight);
      io.setDisplaySize(this.winWidth[0], this.winHeight[0]);
      if (this.winWidth[0] > 0 && this.winHeight[0] > 0) {
         float scaleX = (float)this.fbWidth[0] / this.winWidth[0];
         float scaleY = (float)this.fbHeight[0] / this.winHeight[0];
         io.setDisplayFramebufferScale(scaleX, scaleY);
         GLFW.glfwGetWindowContentScale(Minecraft.getInstance().getWindow().getWindow(), this.windowScaleX, this.windowScaleY);
         this.contentScale = Math.max(this.windowScaleX[0] / scaleX, this.windowScaleY[0] / scaleY);
      }

      this.updateMonitors();
      GLFW.glfwSetMonitorCallback(this::monitorCallback);
      ImGuiViewport mainViewport = ImGui.getMainViewport();
      mainViewport.setPlatformHandle(this.mainWindowPtr);
      if (IS_WINDOWS) {
         mainViewport.setPlatformHandleRaw(GLFWNativeWin32.glfwGetWin32Window(windowId));
      }

      if (io.hasConfigFlags(1024)) {
         this.initPlatformInterface();
      }

      return true;
   }

   public List<Vec2> getCapturedInterframeMousePositions() {
      return this.capturedInterframeMousePositions;
   }

   public void newFrame() {
      ImGuiIO io = EditorUI.getIO();
      if (this.interframeMousePositions.isEmpty()) {
         this.capturedInterframeMousePositions.clear();
      } else {
         this.capturedInterframeMousePositions = this.interframeMousePositions;
         this.interframeMousePositions = new ArrayList<>();
         if (io.hasConfigFlags(1024)) {
            long windowPtr = this.mouseWindowPtr;
            if (windowPtr == 0L) {
               windowPtr = this.mainWindowPtr;
            }

            GLFW.glfwGetWindowPos(windowPtr, this.windowX, this.windowY);
            List<Vec2> offset = new ArrayList<>();

            for (Vec2 pos : this.capturedInterframeMousePositions) {
               offset.add(new Vec2(pos.x + this.windowX[0], pos.y + this.windowY[0]));
            }

            this.capturedInterframeMousePositions = offset;
         }
      }

      GLFW.glfwGetWindowSize(this.mainWindowPtr, this.winWidth, this.winHeight);
      GLFW.glfwGetFramebufferSize(this.mainWindowPtr, this.fbWidth, this.fbHeight);
      io.setDisplaySize(this.winWidth[0], this.winHeight[0]);
      if (this.winWidth[0] > 0 && this.winHeight[0] > 0) {
         float scaleX = (float)this.fbWidth[0] / this.winWidth[0];
         float scaleY = (float)this.fbHeight[0] / this.winHeight[0];
         io.setDisplayFramebufferScale(scaleX, scaleY);
         GLFW.glfwGetWindowContentScale(Minecraft.getInstance().getWindow().getWindow(), this.windowScaleX, this.windowScaleY);
         this.contentScale = Math.max(this.windowScaleX[0] / scaleX, this.windowScaleY[0] / scaleY);
      }

      if (this.wantUpdateMonitors) {
         this.updateMonitors();
      }

      double currentTime = GLFW.glfwGetTime();
      io.setDeltaTime(this.time > 0.0 ? (float)(currentTime - this.time) : 0.016666668F);
      this.time = currentTime;
      if (AsyncFileDialogs.hasDialog()) {
         if (!this.releasedAllKeysBecauseOfDialog) {
            this.releasedAllKeysBecauseOfDialog = true;

            for (int key = 0; key < this.keyPressedGame.length; key++) {
               if (this.keyPressedGame[key]) {
                  int scancode = GLFW.glfwGetKeyScancode(key);
                  this.prevUserCallbackKey.invoke(this.mainWindowPtr, key, scancode, 0, 0);
               }
            }

            this.releaseAllImGuiKeys(io);
         }
      } else {
         this.releasedAllKeysBecauseOfDialog = false;
         boolean shiftDown = false;
         boolean ctrlDown = false;
         boolean altDown = false;
         boolean superDown = false;
         ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

         for (int n = 0; n < platformIO.getViewportsSize(); n++) {
            ImGuiViewport viewport = platformIO.getViewports(n);
            long windowPtr = viewport.getPlatformHandle();
            if (GLFW.glfwGetWindowAttrib(windowPtr, 131073) != 0) {
               shiftDown |= GLFW.glfwGetKey(windowPtr, 340) != 0 || GLFW.glfwGetKey(windowPtr, 344) != 0;
               ctrlDown |= GLFW.glfwGetKey(windowPtr, 341) != 0 || GLFW.glfwGetKey(windowPtr, 345) != 0;
               altDown |= GLFW.glfwGetKey(windowPtr, 342) != 0 || GLFW.glfwGetKey(windowPtr, 346) != 0;
               superDown |= GLFW.glfwGetKey(windowPtr, 343) != 0 || GLFW.glfwGetKey(windowPtr, 347) != 0;
            }
         }

         io.setKeyShift(shiftDown);
         io.setKeyCtrl(ctrlDown);
         io.setKeyAlt(altDown);
         io.setKeySuper(superDown);
         this.updateMousePosAndButtons();
         this.updateMouseCursor();
      }
   }

   private void releaseAllImGuiKeys(ImGuiIO io) {
      for (int key = 0; key < this.keyOwnerWindows.length; key++) {
         if (this.keyOwnerWindows[key] != 0L) {
            int imguiKey = glfwKeyToImGuiKey(key);
            if (imguiKey != 0) {
               io.addKeyEvent(imguiKey, false);
            }

            this.keyOwnerWindows[key] = 0L;
         }
      }

      io.setKeyCtrl(false);
      io.setKeyShift(false);
      io.setKeyAlt(false);
      io.setKeySuper(false);
   }

   public void updateReleaseAllKeys(boolean release) {
      if (release) {
         if (!this.releasedAllKeysBecauseOfDisable) {
            this.releasedAllKeysBecauseOfDisable = true;
            ImGuiIO io = EditorUI.getIO();
            this.releaseAllImGuiKeys(io);
            if (ImGui.getMouseCursor() != 0) {
               ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

               for (int n = 0; n < platformIO.getViewportsSize(); n++) {
                  long windowPtr = platformIO.getViewports(n).getPlatformHandle();
                  GLFW.glfwSetCursor(windowPtr, this.mouseCursors[0]);
               }
            }
         }
      } else {
         this.releasedAllKeysBecauseOfDisable = false;
      }
   }

   private void detectGlfwVersionAndEnabledFeatures() {
      int[] major = new int[1];
      int[] minor = new int[1];
      int[] rev = new int[1];
      GLFW.glfwGetVersion(major, minor, rev);
      int version = major[0] * 1000 + minor[0] * 100 + rev[0] * 10;
      this.glfwHawWindowTopmost = version >= 3200;
      this.glfwHasWindowAlpha = version >= 3300;
      this.glfwHasPerMonitorDpi = version >= 3300;
      this.glfwHasFocusWindow = version >= 3200;
      this.glfwHasFocusOnShow = version >= 3300;
      this.glfwHasMonitorWorkArea = version >= 3300;
   }

   private void updateMousePosAndButtons() {
      ImGuiIO io = EditorUI.getIO();
      CustomImGuiImplGlfw.MouseHandledBy mouseHandledBy = this.getMouseHandledBy();
      if (mouseHandledBy.allowImgui() && !AsyncFileDialogs.hasDialog()) {
         for (int i = 0; i < 5; i++) {
            io.setMouseDown(i, this.mouseJustPressed[i] || GLFW.glfwGetMouseButton(this.mainWindowPtr, i) != 0);
            this.mouseJustPressed[i] = false;
         }

         io.getMousePos(this.mousePosBackup);
         io.setMousePos(-Float.MAX_VALUE, -Float.MAX_VALUE);
         io.setMouseHoveredViewport(0);
         ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

         for (int n = 0; n < platformIO.getViewportsSize(); n++) {
            ImGuiViewport viewport = platformIO.getViewports(n);
            long windowPtr = viewport.getPlatformHandle();
            boolean focused = GLFW.glfwGetWindowAttrib(windowPtr, 131073) != 0;
            if (focused) {
               for (int i = 0; i < 5; i++) {
                  if (!io.getMouseDown(i)) {
                     io.setMouseDown(i, GLFW.glfwGetMouseButton(windowPtr, i) != 0);
                  }
               }
            }

            if (io.getWantSetMousePos() && focused) {
               GLFW.glfwSetCursorPos(windowPtr, this.mousePosBackup.x - viewport.getPosX(), this.mousePosBackup.y - viewport.getPosY());
            }

            if (this.mouseWindowPtr == windowPtr || focused) {
               GLFW.glfwGetCursorPos(windowPtr, this.mouseX, this.mouseY);
               if (io.hasConfigFlags(1024)) {
                  GLFW.glfwGetWindowPos(windowPtr, this.windowX, this.windowY);
                  io.setMousePos((float)this.mouseX[0] + this.windowX[0], (float)this.mouseY[0] + this.windowY[0]);
               } else {
                  io.setMousePos((float)this.mouseX[0], (float)this.mouseY[0]);
               }
            }
         }
      } else {
         for (int ix = 0; ix < 5; ix++) {
            io.setMouseDown(ix, false);
            this.mouseJustPressed[ix] = false;
         }
      }
   }

   private void updateMouseCursor() {
      if (!AsyncFileDialogs.hasDialog()) {
         ImGuiIO io = EditorUI.getIO();
         boolean noCursorChange = io.hasConfigFlags(32);
         boolean cursorDisabled = GLFW.glfwGetInputMode(this.mainWindowPtr, 208897) == 212995;
         if (!noCursorChange && !cursorDisabled) {
            int imguiCursor = ImGui.getMouseCursor();
            ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

            for (int n = 0; n < platformIO.getViewportsSize(); n++) {
               long windowPtr = platformIO.getViewports(n).getPlatformHandle();
               if (imguiCursor != -1 && !io.getMouseDrawCursor()) {
                  GLFW.glfwSetCursor(windowPtr, this.mouseCursors[imguiCursor] != 0L ? this.mouseCursors[imguiCursor] : this.mouseCursors[0]);
                  GLFW.glfwSetInputMode(windowPtr, 208897, 212993);
               } else {
                  GLFW.glfwSetInputMode(windowPtr, 208897, 212994);
               }
            }
         }
      }
   }

   private void updateMonitors() {
      ImGuiPlatformIO platformIO = ImGui.getPlatformIO();
      PointerBuffer monitors = GLFW.glfwGetMonitors();
      platformIO.resizeMonitors(0);
      if (monitors != null) {
         for (int n = 0; n < monitors.limit(); n++) {
            long monitor = monitors.get(n);
            GLFW.glfwGetMonitorPos(monitor, this.monitorX, this.monitorY);
            GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
            float mainPosX = this.monitorX[0];
            float mainPosY = this.monitorY[0];
            float mainSizeX = vidMode.width();
            float mainSizeY = vidMode.height();
            if (this.glfwHasMonitorWorkArea) {
               GLFW.glfwGetMonitorWorkarea(monitor, this.monitorWorkAreaX, this.monitorWorkAreaY, this.monitorWorkAreaWidth, this.monitorWorkAreaHeight);
            }

            float workPosX = 0.0F;
            float workPosY = 0.0F;
            float workSizeX = 0.0F;
            float workSizeY = 0.0F;
            if (this.glfwHasMonitorWorkArea && this.monitorWorkAreaWidth[0] > 0 && this.monitorWorkAreaHeight[0] > 0) {
               workPosX = this.monitorWorkAreaX[0];
               workPosY = this.monitorWorkAreaY[0];
               workSizeX = this.monitorWorkAreaWidth[0];
               workSizeY = this.monitorWorkAreaHeight[0];
            }

            if (this.glfwHasPerMonitorDpi) {
               GLFW.glfwGetMonitorContentScale(monitor, this.monitorContentScaleX, this.monitorContentScaleY);
            }

            float dpiScale = this.monitorContentScaleX[0];
            platformIO.pushMonitors(monitor, mainPosX, mainPosY, mainSizeX, mainSizeY, workPosX, workPosY, workSizeX, workSizeY, dpiScale);
         }

         this.wantUpdateMonitors = false;
      }
   }

   public void setViewportWindowsHidden(boolean viewportWindowsHidden) {
      if (this.viewportWindowsHidden != viewportWindowsHidden) {
         if (!this.viewportWindowsHidden) {
            this.viewportWindowsHidden = true;
            ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

            for (int n = 0; n < platformIO.getViewportsSize(); n++) {
               long windowPtr = platformIO.getViewports(n).getPlatformHandle();
               if (windowPtr != this.mainWindowPtr) {
                  GLFW.glfwHideWindow(windowPtr);
               }
            }
         } else {
            this.viewportWindowsHidden = false;
            ImGuiPlatformIO platformIO = ImGui.getPlatformIO();

            for (int nx = 0; nx < platformIO.getViewportsSize(); nx++) {
               long windowPtr = platformIO.getViewports(nx).getPlatformHandle();
               if (windowPtr != this.mainWindowPtr) {
                  GLFW.glfwShowWindow(windowPtr);
               }
            }
         }
      }
   }

   private void windowCloseCallback(long windowId) {
      ImGuiViewport vp = ImGui.findViewportByPlatformHandle(windowId);
      vp.setPlatformRequestClose(true);
   }

   private void windowPosCallback(long windowId, int xPos, int yPos) {
      ImGuiViewport vp = ImGui.findViewportByPlatformHandle(windowId);
      CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
      boolean ignoreEvent = ImGui.getFrameCount() <= data.ignoreWindowPosEventFrame + 1;
      if (!ignoreEvent) {
         vp.setPlatformRequestMove(true);
      }
   }

   private void windowSizeCallback(long windowId, int width, int height) {
      ImGuiViewport vp = ImGui.findViewportByPlatformHandle(windowId);
      CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
      boolean ignoreEvent = ImGui.getFrameCount() <= data.ignoreWindowSizeEventFrame + 1;
      if (!ignoreEvent) {
         vp.setPlatformRequestResize(true);
      }
   }

   private void initPlatformInterface() {
      ImGuiPlatformIO platformIO = ImGui.getPlatformIO();
      platformIO.setPlatformCreateWindow(new CustomImGuiImplGlfw.CreateWindowFunction());
      platformIO.setPlatformDestroyWindow(new CustomImGuiImplGlfw.DestroyWindowFunction());
      platformIO.setPlatformShowWindow(new CustomImGuiImplGlfw.ShowWindowFunction());
      platformIO.setPlatformGetWindowPos(new CustomImGuiImplGlfw.GetWindowPosFunction());
      platformIO.setPlatformSetWindowPos(new CustomImGuiImplGlfw.SetWindowPosFunction());
      platformIO.setPlatformGetWindowSize(new CustomImGuiImplGlfw.GetWindowSizeFunction());
      platformIO.setPlatformSetWindowSize(new CustomImGuiImplGlfw.SetWindowSizeFunction());
      platformIO.setPlatformSetWindowTitle(new CustomImGuiImplGlfw.SetWindowTitleFunction());
      platformIO.setPlatformSetWindowFocus(new CustomImGuiImplGlfw.SetWindowFocusFunction());
      platformIO.setPlatformGetWindowFocus(new CustomImGuiImplGlfw.GetWindowFocusFunction());
      platformIO.setPlatformGetWindowMinimized(new CustomImGuiImplGlfw.GetWindowMinimizedFunction());
      platformIO.setPlatformSetWindowAlpha(new CustomImGuiImplGlfw.SetWindowAlphaFunction());
      platformIO.setPlatformRenderWindow(new CustomImGuiImplGlfw.RenderWindowFunction());
      platformIO.setPlatformSwapBuffers(new CustomImGuiImplGlfw.SwapBuffersFunction());
      ImGuiViewport mainViewport = ImGui.getMainViewport();
      CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = new CustomImGuiImplGlfw.ImGuiViewportDataGlfw();
      data.window = this.mainWindowPtr;
      data.windowOwned = false;
      mainViewport.setPlatformUserData(data);
   }

   public static int glfwKeyToImGuiKey(int glfwKey) {
      switch (glfwKey) {
         case 32:
            return 524;
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 40:
         case 41:
         case 42:
         case 43:
         case 58:
         case 60:
         case 62:
         case 63:
         case 64:
         case 94:
         case 95:
         case 97:
         case 98:
         case 99:
         case 100:
         case 101:
         case 102:
         case 103:
         case 104:
         case 105:
         case 106:
         case 107:
         case 108:
         case 109:
         case 110:
         case 111:
         case 112:
         case 113:
         case 114:
         case 115:
         case 116:
         case 117:
         case 118:
         case 119:
         case 120:
         case 121:
         case 122:
         case 123:
         case 124:
         case 125:
         case 126:
         case 127:
         case 128:
         case 129:
         case 130:
         case 131:
         case 132:
         case 133:
         case 134:
         case 135:
         case 136:
         case 137:
         case 138:
         case 139:
         case 140:
         case 141:
         case 142:
         case 143:
         case 144:
         case 145:
         case 146:
         case 147:
         case 148:
         case 149:
         case 150:
         case 151:
         case 152:
         case 153:
         case 154:
         case 155:
         case 156:
         case 157:
         case 158:
         case 159:
         case 160:
         case 161:
         case 162:
         case 163:
         case 164:
         case 165:
         case 166:
         case 167:
         case 168:
         case 169:
         case 170:
         case 171:
         case 172:
         case 173:
         case 174:
         case 175:
         case 176:
         case 177:
         case 178:
         case 179:
         case 180:
         case 181:
         case 182:
         case 183:
         case 184:
         case 185:
         case 186:
         case 187:
         case 188:
         case 189:
         case 190:
         case 191:
         case 192:
         case 193:
         case 194:
         case 195:
         case 196:
         case 197:
         case 198:
         case 199:
         case 200:
         case 201:
         case 202:
         case 203:
         case 204:
         case 205:
         case 206:
         case 207:
         case 208:
         case 209:
         case 210:
         case 211:
         case 212:
         case 213:
         case 214:
         case 215:
         case 216:
         case 217:
         case 218:
         case 219:
         case 220:
         case 221:
         case 222:
         case 223:
         case 224:
         case 225:
         case 226:
         case 227:
         case 228:
         case 229:
         case 230:
         case 231:
         case 232:
         case 233:
         case 234:
         case 235:
         case 236:
         case 237:
         case 238:
         case 239:
         case 240:
         case 241:
         case 242:
         case 243:
         case 244:
         case 245:
         case 246:
         case 247:
         case 248:
         case 249:
         case 250:
         case 251:
         case 252:
         case 253:
         case 254:
         case 255:
         case 270:
         case 271:
         case 272:
         case 273:
         case 274:
         case 275:
         case 276:
         case 277:
         case 278:
         case 279:
         case 285:
         case 286:
         case 287:
         case 288:
         case 289:
         case 314:
         case 315:
         case 316:
         case 317:
         case 318:
         case 319:
         case 337:
         case 338:
         case 339:
         default:
            return 0;
         case 39:
            return 596;
         case 44:
            return 597;
         case 45:
            return 598;
         case 46:
            return 599;
         case 47:
            return 600;
         case 48:
            return 536;
         case 49:
            return 537;
         case 50:
            return 538;
         case 51:
            return 539;
         case 52:
            return 540;
         case 53:
            return 541;
         case 54:
            return 542;
         case 55:
            return 543;
         case 56:
            return 544;
         case 57:
            return 545;
         case 59:
            return 601;
         case 61:
            return 602;
         case 65:
            return 546;
         case 66:
            return 547;
         case 67:
            return 548;
         case 68:
            return 549;
         case 69:
            return 550;
         case 70:
            return 551;
         case 71:
            return 552;
         case 72:
            return 553;
         case 73:
            return 554;
         case 74:
            return 555;
         case 75:
            return 556;
         case 76:
            return 557;
         case 77:
            return 558;
         case 78:
            return 559;
         case 79:
            return 560;
         case 80:
            return 561;
         case 81:
            return 562;
         case 82:
            return 563;
         case 83:
            return 564;
         case 84:
            return 565;
         case 85:
            return 566;
         case 86:
            return 567;
         case 87:
            return 568;
         case 88:
            return 569;
         case 89:
            return 570;
         case 90:
            return 571;
         case 91:
            return 603;
         case 92:
            return 604;
         case 93:
            return 605;
         case 96:
            return 606;
         case 256:
            return 526;
         case 257:
            return 525;
         case 258:
            return 512;
         case 259:
            return 523;
         case 260:
            return 521;
         case 261:
            return 522;
         case 262:
            return 514;
         case 263:
            return 513;
         case 264:
            return 516;
         case 265:
            return 515;
         case 266:
            return 517;
         case 267:
            return 518;
         case 268:
            return 519;
         case 269:
            return 520;
         case 280:
            return 607;
         case 281:
            return 608;
         case 282:
            return 609;
         case 283:
            return 610;
         case 284:
            return 611;
         case 290:
            return 572;
         case 291:
            return 573;
         case 292:
            return 574;
         case 293:
            return 575;
         case 294:
            return 576;
         case 295:
            return 577;
         case 296:
            return 578;
         case 297:
            return 579;
         case 298:
            return 580;
         case 299:
            return 581;
         case 300:
            return 582;
         case 301:
            return 583;
         case 302:
            return 584;
         case 303:
            return 585;
         case 304:
            return 586;
         case 305:
            return 587;
         case 306:
            return 588;
         case 307:
            return 589;
         case 308:
            return 590;
         case 309:
            return 591;
         case 310:
            return 592;
         case 311:
            return 593;
         case 312:
            return 594;
         case 313:
            return 595;
         case 320:
            return 612;
         case 321:
            return 613;
         case 322:
            return 614;
         case 323:
            return 615;
         case 324:
            return 616;
         case 325:
            return 617;
         case 326:
            return 618;
         case 327:
            return 619;
         case 328:
            return 620;
         case 329:
            return 621;
         case 330:
            return 622;
         case 331:
            return 623;
         case 332:
            return 624;
         case 333:
            return 625;
         case 334:
            return 626;
         case 335:
            return 627;
         case 336:
            return 628;
         case 340:
            return 528;
         case 341:
            return 527;
         case 342:
            return 529;
         case 343:
            return 530;
         case 344:
            return 532;
         case 345:
            return 531;
         case 346:
            return 533;
         case 347:
            return 534;
         case 348:
            return 535;
      }
   }

   private final class CreateWindowFunction extends ImPlatformFuncViewport {
      public void accept(ImGuiViewport vp) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = new CustomImGuiImplGlfw.ImGuiViewportDataGlfw();
         vp.setPlatformUserData(data);
         GLFW.glfwWindowHint(131076, 0);
         GLFW.glfwWindowHint(131073, 0);
         if (CustomImGuiImplGlfw.this.glfwHasFocusOnShow) {
            GLFW.glfwWindowHint(131084, 0);
         }

         GLFW.glfwWindowHint(131077, vp.hasFlags(8) ? 0 : 1);
         if (CustomImGuiImplGlfw.this.glfwHawWindowTopmost) {
            GLFW.glfwWindowHint(131079, vp.hasFlags(1024) ? 1 : 0);
         }

         data.window = GLFW.glfwCreateWindow((int)vp.getSizeX(), (int)vp.getSizeY(), "No Title Yet", 0L, CustomImGuiImplGlfw.this.mainWindowPtr);
         data.windowOwned = true;
         vp.setPlatformHandle(data.window);
         if (CustomImGuiImplGlfw.IS_WINDOWS) {
            vp.setPlatformHandleRaw(GLFWNativeWin32.glfwGetWin32Window(data.window));
         }

         GLFW.glfwSetWindowPos(data.window, (int)vp.getPosX(), (int)vp.getPosY());
         GLFW.glfwSetMouseButtonCallback(data.window, CustomImGuiImplGlfw.this::mouseButtonCallback);
         GLFW.glfwSetScrollCallback(data.window, CustomImGuiImplGlfw.this::scrollCallback);
         GLFW.glfwSetKeyCallback(data.window, CustomImGuiImplGlfw.this::keyCallback);
         GLFW.glfwSetCharModsCallback(data.window, CustomImGuiImplGlfw.this::charModsCallback);
         GLFW.glfwSetCharCallback(data.window, CustomImGuiImplGlfw.this::charCallback);
         GLFW.glfwSetWindowCloseCallback(data.window, CustomImGuiImplGlfw.this::windowCloseCallback);
         GLFW.glfwSetWindowPosCallback(data.window, CustomImGuiImplGlfw.this::windowPosCallback);
         GLFW.glfwSetWindowSizeCallback(data.window, CustomImGuiImplGlfw.this::windowSizeCallback);
         GLFW.glfwMakeContextCurrent(data.window);
         GLFW.glfwSwapInterval(0);
      }
   }

   private final class DestroyWindowFunction extends ImPlatformFuncViewport {
      public void accept(ImGuiViewport vp) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         if (data != null && data.windowOwned) {
            for (int i = 0; i < CustomImGuiImplGlfw.this.keyOwnerWindows.length; i++) {
               if (CustomImGuiImplGlfw.this.keyOwnerWindows[i] == data.window) {
                  CustomImGuiImplGlfw.this.keyCallback(data.window, i, 0, 0, 0);
               }
            }

            GLFW.glfwDestroyWindow(data.window);
         }

         vp.setPlatformUserData(null);
         vp.setPlatformHandle(0L);
      }
   }

   private static final class GetWindowFocusFunction extends ImPlatformFuncViewportSuppBoolean {
      public boolean get(ImGuiViewport vp) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         return GLFW.glfwGetWindowAttrib(data.window, 131073) != 0;
      }
   }

   private static final class GetWindowMinimizedFunction extends ImPlatformFuncViewportSuppBoolean {
      public boolean get(ImGuiViewport vp) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         return GLFW.glfwGetWindowAttrib(data.window, 131074) != 0;
      }
   }

   private static final class GetWindowPosFunction extends ImPlatformFuncViewportSuppImVec2 {
      private final int[] posX = new int[1];
      private final int[] posY = new int[1];

      public void get(ImGuiViewport vp, ImVec2 dstImVec2) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         GLFW.glfwGetWindowPos(data.window, this.posX, this.posY);
         dstImVec2.x = this.posX[0];
         dstImVec2.y = this.posY[0];
      }
   }

   private static final class GetWindowSizeFunction extends ImPlatformFuncViewportSuppImVec2 {
      private final int[] width = new int[1];
      private final int[] height = new int[1];

      public void get(ImGuiViewport vp, ImVec2 dstImVec2) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         GLFW.glfwGetWindowSize(data.window, this.width, this.height);
         dstImVec2.x = this.width[0];
         dstImVec2.y = this.height[0];
      }
   }

   private static final class ImGuiViewportDataGlfw {
      long window;
      boolean windowOwned = false;
      int ignoreWindowPosEventFrame = -1;
      int ignoreWindowSizeEventFrame = -1;
   }

   public static enum MouseHandledBy {
      EDITOR_GRABBED,
      IMGUI,
      GAME,
      BOTH;

      public boolean allowImgui() {
         return this == IMGUI || this == BOTH;
      }

      public boolean allowGame() {
         return this == GAME || this == BOTH;
      }
   }

   private static final class RenderWindowFunction extends ImPlatformFuncViewport {
      public void accept(ImGuiViewport vp) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         GLFW.glfwMakeContextCurrent(data.window);
      }
   }

   private final class SetWindowAlphaFunction extends ImPlatformFuncViewportFloat {
      public void accept(ImGuiViewport vp, float f) {
         if (CustomImGuiImplGlfw.this.glfwHasWindowAlpha) {
            CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
            GLFW.glfwSetWindowOpacity(data.window, f);
         }
      }
   }

   private final class SetWindowFocusFunction extends ImPlatformFuncViewport {
      public void accept(ImGuiViewport vp) {
         if (CustomImGuiImplGlfw.this.glfwHasFocusWindow) {
            CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
            GLFW.glfwFocusWindow(data.window);
         }
      }
   }

   private static final class SetWindowPosFunction extends ImPlatformFuncViewportImVec2 {
      public void accept(ImGuiViewport vp, ImVec2 imVec2) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         data.ignoreWindowPosEventFrame = ImGui.getFrameCount();
         GLFW.glfwSetWindowPos(data.window, (int)imVec2.x, (int)imVec2.y);
      }
   }

   private final class SetWindowSizeFunction extends ImPlatformFuncViewportImVec2 {
      private final int[] x = new int[1];
      private final int[] y = new int[1];
      private final int[] width = new int[1];
      private final int[] height = new int[1];

      public void accept(ImGuiViewport vp, ImVec2 imVec2) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         if (CustomImGuiImplGlfw.IS_APPLE && !CustomImGuiImplGlfw.this.glfwHasOsxWindowPosFix) {
            GLFW.glfwGetWindowPos(data.window, this.x, this.y);
            GLFW.glfwGetWindowSize(data.window, this.width, this.height);
            GLFW.glfwSetWindowPos(data.window, this.x[0], this.y[0] - this.height[0] + (int)imVec2.y);
         }

         data.ignoreWindowSizeEventFrame = ImGui.getFrameCount();
         GLFW.glfwSetWindowSize(data.window, (int)imVec2.x, (int)imVec2.y);
      }
   }

   private static final class SetWindowTitleFunction extends ImPlatformFuncViewportString {
      public void accept(ImGuiViewport vp, String str) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         GLFW.glfwSetWindowTitle(data.window, str);
      }
   }

   private final class ShowWindowFunction extends ImPlatformFuncViewport {
      public void accept(ImGuiViewport vp) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         if (!CustomImGuiImplGlfw.this.viewportWindowsHidden) {
            GLFW.glfwShowWindow(data.window);
         }
      }
   }

   private static final class SwapBuffersFunction extends ImPlatformFuncViewport {
      public void accept(ImGuiViewport vp) {
         CustomImGuiImplGlfw.ImGuiViewportDataGlfw data = (CustomImGuiImplGlfw.ImGuiViewportDataGlfw)vp.getPlatformUserData();
         GLFW.glfwMakeContextCurrent(data.window);
         GLFW.glfwSwapBuffers(data.window);
      }
   }
}
