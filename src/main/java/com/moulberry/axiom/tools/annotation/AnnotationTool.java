package com.moulberry.axiom.tools.annotation;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.annotations.AnnotationHistoryElement;
import com.moulberry.axiom.annotations.AnnotationUpdateAction;
import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.annotations.data.BoxOutlineAnnotationData;
import com.moulberry.axiom.annotations.data.FreehandOutlineAnnotationData;
import com.moulberry.axiom.annotations.data.ImageAnnotationData;
import com.moulberry.axiom.annotations.data.LineAnnotationData;
import com.moulberry.axiom.annotations.data.LinesOutlineAnnotationData;
import com.moulberry.axiom.annotations.data.TextAnnotationData;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.packets.AxiomServerboundAnnotationUpdate;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.pather.ToolPatherPoint;
import com.moulberry.axiom.pather.ToolPatherVec3;
import com.moulberry.axiom.rasterization.Rasterization2D;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.annotations.Annotation;
import com.moulberry.axiom.render.annotations.Annotations;
import com.moulberry.axiom.render.annotations.LineAnnotation;
import com.moulberry.axiom.render.annotations.OutlineAnnotation;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.PositionUtils;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class AnnotationTool implements Tool {
   public static EnumSet<AnnotationsDisabled> annotationsDisabled = EnumSet.noneOf(AnnotationsDisabled.class);
   private boolean raycastFluids = true;
   @Nullable
   private ToolPatherVec3 linePather = null;
   private Vec3i startQuantized = null;
   private Vec3i lastQuantized = null;
   private Direction currentDirection = null;
   private Direction lastUsedDirection = null;
   private Vec3 lastRaycastPosition = null;
   private final List<Vec3> raycastPositions = new ArrayList<>();
   private final ByteList quantizedLineOffsets = new ByteArrayList();
   private final int[] outlineMode = new int[]{0};
   private static final int OUTLINE_MODE_FREEHAND = 0;
   private static final int OUTLINE_MODE_LINES = 1;
   private static final int OUTLINE_MODE_BOX = 2;
   private ToolPatherPoint outlinePather = null;
   private BlockPos startOutline = null;
   private final MutableBlockPos lastOutlinePos = new MutableBlockPos();
   private int outlineOffsetIndex = 0;
   private final ByteArrayList outlineOffsets = new ByteArrayList();
   private final LongArrayList outlineLines = new LongArrayList();
   private BlockPos outlineBoxStart = null;
   private boolean maxBoxSizeReached = false;
   private Vec2 deleteMouseDragStart = null;
   public Gizmo selectedGizmo = null;
   public UUID lastSelectedAnnotation = null;
   public UUID selectedAnnotation = null;
   public UUID justPlacedAnnotation = null;
   private final int[] tool = new int[]{0};
   private static final int TOOL_DRAW = 0;
   private static final int TOOL_OUTLINE = 1;
   private static final int TOOL_TEXT = 2;
   private static final int TOOL_IMAGE = 3;
   private static final int TOOL_ERASE = 4;
   private static final int TOOL_MOVE = 5;
   private static final String[] TOOL_NAMES = new String[]{"Draw", "Outline", "Text", "Image", "Erase", "Move"};
   private static final String[] TOOL_ICONS = new String[]{"\ue924", "\ue927", "\ue925", "\ue926", "\ue922", "\ue923"};
   private boolean textShadow = true;
   private final int[] textScale = new int[]{1};
   private final int[] billboardMode = new int[]{0};
   private final ImString textContent = new ImString();
   private final float[] colour = new float[]{1.0F, 1.0F, 1.0F};
   private final float[] opacity = new float[]{1.0F};
   private final float[] lineWidth = new float[]{2.0F};
   private final int[] imageWidth = new int[]{8};
   private final ImString imageUrl = new ImString();

   public AnnotationTool() {
      this.textContent.inputData.isResizable = true;
      this.imageUrl.inputData.isResizable = true;
   }

   @Override
   public void reset() {
      this.linePather = null;
      this.startQuantized = null;
      this.lastQuantized = null;
      this.currentDirection = null;
      this.lastUsedDirection = null;
      this.lastRaycastPosition = null;
      this.raycastPositions.clear();
      this.quantizedLineOffsets.clear();
      this.outlinePather = null;
      this.startOutline = null;
      this.outlineOffsetIndex = 0;
      this.outlineOffsets.clear();
      this.outlineBoxStart = null;
      this.maxBoxSizeReached = false;
      this.outlineLines.clear();
      this.deleteMouseDragStart = null;
      this.lastRaycastPosition = null;
      this.raycastPositions.clear();
      this.selectedGizmo = null;
      this.selectedAnnotation = null;
      this.lastSelectedAnnotation = null;
      this.justPlacedAnnotation = null;
   }

   private static boolean compatibleSign(int a, int b) {
      return Math.abs(a) > 1 && Math.abs(b) > 0 ? a > 0 == b > 0 : true;
   }

   public static boolean leftQuantizedCloserToEdge(int a, int b) {
      a %= 16;
      if (a > 8) {
         a -= 16;
      }

      if (a < -8) {
         a += 16;
      }

      b %= 16;
      if (b > 8) {
         b -= 16;
      }

      if (b < -8) {
         b += 16;
      }

      return Math.abs(a) < Math.abs(b);
   }

   private static Vec3i quantize(Vec3 vec3) {
      return new Vec3i((int)Math.round(vec3.x * 16.0), (int)Math.round(vec3.y * 16.0), (int)Math.round(vec3.z * 16.0));
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      if (!annotationsDisabled.isEmpty()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         switch (action) {
            case RIGHT_MOUSE:
               if (this.tool[0] != 1) {
                  this.reset();
               } else {
                  switch (this.outlineMode[0]) {
                     case 0:
                        this.reset();
                        break;
                     case 1:
                        if (this.outlineLines.isEmpty()) {
                           this.reset();
                        }
                        break;
                     case 2:
                        if (this.outlineBoxStart == null) {
                           this.reset();
                        }
                  }
               }

               switch (this.tool[0]) {
                  case 0:
                     RayCaster.RaycastResult resultx = Tool.raycastBlock(false, false, this.raycastFluids);
                     if (resultx != null) {
                        this.linePather = new ToolPatherVec3();
                        this.startQuantized = quantize(resultx.getLocation());
                        this.lastRaycastPosition = resultx.getLocation();
                        this.lastUsedDirection = resultx.getDirection();
                        this.lastQuantized = this.startQuantized;
                     }
                     break;
                  case 1:
                     RayCaster.RaycastResult result = Tool.raycastBlock(false, false, this.raycastFluids);
                     if (result != null) {
                        switch (this.outlineMode[0]) {
                           case 0:
                              this.outlinePather = new ToolPatherPoint(false);
                              this.outlinePather.includeFluids = this.raycastFluids;
                              this.outlineOffsetIndex = 0;
                              this.outlineOffsets.clear();
                              break;
                           case 1:
                              if (this.outlineLines.isEmpty()) {
                                 this.outlineLines.add(result.blockPos().asLong());
                              } else {
                                 long lastPos = this.outlineLines.getLong(this.outlineLines.size() - 1);
                                 BlockPos from = BlockPos.of(lastPos);
                                 BlockPos to = result.blockPos();
                                 if (!from.equals(to)) {
                                    this.outlineLines.add(to.asLong());
                                 }
                              }
                              break;
                           case 2:
                              if (this.outlineBoxStart == null) {
                                 this.outlineBoxStart = result.blockPos();
                                 this.maxBoxSizeReached = false;
                              } else {
                                 int minX = Math.min(this.outlineBoxStart.getX(), result.blockPos().getX());
                                 int minY = Math.min(this.outlineBoxStart.getY(), result.blockPos().getY());
                                 int minZ = Math.min(this.outlineBoxStart.getZ(), result.blockPos().getZ());
                                 int maxX = Math.max(this.outlineBoxStart.getX(), result.blockPos().getX());
                                 int maxY = Math.max(this.outlineBoxStart.getY(), result.blockPos().getY());
                                 int maxZ = Math.max(this.outlineBoxStart.getZ(), result.blockPos().getZ());
                                 int size = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
                                 if (size <= 1000000) {
                                    BoxOutlineAnnotationData annotation = new BoxOutlineAnnotationData(
                                       minX, minY, minZ, maxX, maxY, maxZ, colourFromFloat(this.colour[0], this.colour[1], this.colour[2])
                                    );
                                    Annotations.pushCreateAnnotation(annotation);
                                 }

                                 this.reset();
                              }
                        }
                     }
                     break;
                  case 2:
                     RayCaster.RaycastResult resultxxx = Tool.raycastBlock(false, false, this.raycastFluids);
                     if (resultxxx != null) {
                        TextAnnotationData data = this.createTextAnnotationData(
                           resultxxx.getLocation().toVector3f(), calculateFallbackYaw(), resultxxx.direction()
                        );
                        if (data != null) {
                           this.justPlacedAnnotation = Annotations.pushCreateAnnotation(data);
                           if (this.billboardMode[0] == 0 || this.billboardMode[0] == 2 && resultxxx.direction().getAxis() == Axis.Y) {
                              this.justPlacedAnnotation = null;
                           }
                        }
                     }
                     break;
                  case 3:
                     RayCaster.RaycastResult resultxx = Tool.raycastBlock(false, false, this.raycastFluids);
                     if (resultxx != null) {
                        ImageAnnotationData annotation = this.createImageAnnotationData(
                           resultxx.getLocation().toVector3f(), calculateFallbackYaw(), resultxx.direction()
                        );
                        this.justPlacedAnnotation = Annotations.pushCreateAnnotation(annotation);
                        if (this.billboardMode[0] == 0 || this.billboardMode[0] == 2 && resultxx.direction().getAxis() == Axis.Y) {
                           this.justPlacedAnnotation = null;
                        }
                     }
                     break;
                  case 4:
                     this.deleteMouseDragStart = new Vec2(EditorUI.getIO().getMousePosX(), EditorUI.getIO().getMousePosY());
               }

               return UserAction.ActionResult.USED_STOP;
            case UNDO:
               if (this.canBeResetByUndo()) {
                  this.reset();
               } else if (!this.outlineLines.isEmpty()) {
                  this.outlineLines.removeLong(this.outlineLines.size() - 1);
                  if (this.outlineLines.isEmpty()) {
                     this.reset();
                  }
               } else {
                  Annotations.undo();
               }

               return UserAction.ActionResult.USED_STOP;
            case REDO:
               if (!this.canBeResetByUndo() && this.outlineLines.isEmpty()) {
                  Annotations.redo();
                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.USED_STOP;
            case LEFT_MOUSE:
               if (this.tool[0] == 5 && !EditorUI.isCtrlOrCmdDown()) {
                  for (Entry<UUID, Annotation> entryx : Annotations.visibleAnnotations()) {
                     Annotation annotation = entryx.getValue();
                     Gizmo gizmo = annotation.getGizmo();
                     if (gizmo != null && gizmo.leftClick()) {
                        this.selectedGizmo = gizmo;
                        this.selectedAnnotation = entryx.getKey();
                        this.lastSelectedAnnotation = null;
                        return UserAction.ActionResult.USED_STOP;
                     }
                  }
               } else if (this.selectedGizmo != null && this.selectedGizmo.leftClick()) {
                  return UserAction.ActionResult.USED_STOP;
               }

               this.selectedGizmo = null;
               this.selectedAnnotation = null;
               this.lastSelectedAnnotation = null;
            default:
               return UserAction.ActionResult.NOT_HANDLED;
            case DELETE:
               if (this.selectedGizmo != null) {
                  for (Entry<UUID, Annotation> entry : Annotations.visibleAnnotations()) {
                     Annotation annotation = entry.getValue();
                     if (this.selectedGizmo == annotation.getGizmo()) {
                        Annotations.push(AnnotationHistoryElement.makeDeleteAnnotation(entry.getKey(), entry.getValue().getData()));
                        return UserAction.ActionResult.USED_STOP;
                     }
                  }
               }

               this.reset();
               return UserAction.ActionResult.USED_STOP;
         }
      }
   }

   private static float calculateFallbackYaw() {
      float yaw = Minecraft.getInstance().player.getYRot();
      float roundedYaw = Math.round(yaw / 90.0F) * 90;
      float difference = Math.abs(Mth.wrapDegrees(yaw - roundedYaw));
      return difference < 7.5 ? roundedYaw : yaw;
   }

   private TextAnnotationData createTextAnnotationData(Vector3f position, float fallbackYaw, Direction direction) {
      String textContent = ImGuiHelper.getString(this.textContent);
      if (!textContent.isBlank()) {
         int colour = colourFromFloat(this.colour[0], this.colour[1], this.colour[2]) & 16777215;
         colour |= (int)(this.opacity[0] * 255.0F) << 24;
         return new TextAnnotationData(
            textContent, position, new Quaternionf(), direction, fallbackYaw, this.textScale[0], this.billboardMode[0], colour, this.textShadow
         );
      } else {
         return null;
      }
   }

   private ImageAnnotationData createImageAnnotationData(Vector3f position, float fallbackYaw, Direction direction) {
      return new ImageAnnotationData(
         ImGuiHelper.getString(this.imageUrl), position, new Quaternionf(), direction, fallbackYaw, this.imageWidth[0], this.opacity[0], this.billboardMode[0]
      );
   }

   private boolean canBeResetByUndo() {
      return this.deleteMouseDragStart != null || this.outlineBoxStart != null || this.outlinePather != null || this.linePather != null;
   }

   private static int colourFromFloat(float red, float green, float blue) {
      int redI = (int)(red * 255.0F);
      int greenI = (int)(green * 255.0F);
      int blueI = (int)(blue * 255.0F);
      return 0xFF000000 | redI << 16 | greenI << 8 | blueI;
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (annotationsDisabled.isEmpty()) {
         this.update(rc.position());
         if (this.startQuantized != null && !this.quantizedLineOffsets.isEmpty()) {
            LineAnnotation.drawStatic(
               rc, this.startQuantized, this.quantizedLineOffsets, this.lineWidth[0], colourFromFloat(this.colour[0], this.colour[1], this.colour[2])
            );
         }

         if (this.startOutline != null) {
            FreehandOutlineAnnotationData data = new FreehandOutlineAnnotationData(
               this.startOutline, this.outlineOffsets.elements(), this.outlineOffsetIndex, colourFromFloat(this.colour[0], this.colour[1], this.colour[2])
            );
            OutlineAnnotation.drawStatic(rc, Minecraft.getInstance().level, data);
         }

         if (!this.outlineLines.isEmpty()) {
            RayCaster.RaycastResult result = Tool.raycastBlock(false, false, this.raycastFluids);
            long[] positions;
            if (result != null) {
               positions = new long[this.outlineLines.size() + 1];
               this.outlineLines.toArray(positions);
               positions[positions.length - 1] = result.blockPos().asLong();
            } else {
               positions = this.outlineLines.toLongArray();
            }

            LinesOutlineAnnotationData data = new LinesOutlineAnnotationData(positions, colourFromFloat(this.colour[0], this.colour[1], this.colour[2]));
            OutlineAnnotation.drawStatic(rc, Minecraft.getInstance().level, data);
         }

         if (this.outlineBoxStart != null) {
            RayCaster.RaycastResult result = Tool.raycastBlock(false, false, this.raycastFluids);
            if (result != null) {
               int minX = Math.min(this.outlineBoxStart.getX(), result.blockPos().getX());
               int minY = Math.min(this.outlineBoxStart.getY(), result.blockPos().getY());
               int minZ = Math.min(this.outlineBoxStart.getZ(), result.blockPos().getZ());
               int maxX = Math.max(this.outlineBoxStart.getX(), result.blockPos().getX());
               int maxY = Math.max(this.outlineBoxStart.getY(), result.blockPos().getY());
               int maxZ = Math.max(this.outlineBoxStart.getZ(), result.blockPos().getZ());
               int size = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
               this.maxBoxSizeReached = size > 1000000;
               if (!this.maxBoxSizeReached) {
                  BoxOutlineAnnotationData data = new BoxOutlineAnnotationData(
                     minX, minY, minZ, maxX, maxY, maxZ, colourFromFloat(this.colour[0], this.colour[1], this.colour[2])
                  );
                  OutlineAnnotation.drawStatic(rc, Minecraft.getInstance().level, data);
               }
            }
         }

         boolean showedSelectedGizmo = false;
         if (this.tool[0] == 5) {
            Vec3 lookDirection = Tool.getLookDirection();
            boolean isLeftDown = Tool.isMouseDown(0);
            boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();
            boolean showGizmos = !isCtrlDown;

            for (Entry<UUID, Annotation> entry : Annotations.visibleAnnotations()) {
               Annotation annotation = entry.getValue();
               Gizmo gizmo = annotation.getGizmo();
               if (gizmo != null) {
                  gizmo.enableAxes = gizmo == this.selectedGizmo;
                  if (gizmo == this.selectedGizmo) {
                     showedSelectedGizmo = true;
                  }

                  boolean wasGrabbed = gizmo.isGrabbed();
                  Vec3 previousTarget = gizmo.getTargetVec();
                  gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, true);
                  Vec3 currentTarget = gizmo.getTargetVec();
                  if (!previousTarget.equals(currentTarget)) {
                     Annotations.push(
                        new AnnotationHistoryElement(
                           new AnnotationUpdateAction.MoveAnnotation(entry.getKey(), previousTarget.toVector3f()),
                           new AnnotationUpdateAction.MoveAnnotation(entry.getKey(), currentTarget.toVector3f())
                        )
                     );
                  }

                  if (wasGrabbed && gizmo.isGrabbed()) {
                     gizmo.render(rc, isCtrlDown);
                     showGizmos = false;
                  }
               }
            }

            if (showGizmos) {
               for (Entry<UUID, Annotation> entryx : Annotations.visibleAnnotations()) {
                  Annotation annotation = entryx.getValue();
                  Gizmo gizmo = annotation.getGizmo();
                  if (gizmo != null) {
                     gizmo.render(rc, false);
                  }
               }
            }
         } else if (this.selectedGizmo != null) {
            boolean isLeftDown = Tool.isMouseDown(0);
            boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();

            for (Entry<UUID, Annotation> entryxx : Annotations.visibleAnnotations()) {
               Annotation annotation = entryxx.getValue();
               Gizmo gizmo = annotation.getGizmo();
               if (gizmo != null && gizmo == this.selectedGizmo) {
                  gizmo.enableAxes = true;
                  showedSelectedGizmo = true;
                  boolean wasGrabbedx = gizmo.isGrabbed();
                  Vec3 previousTargetx = gizmo.getTargetVec();
                  gizmo.update(rc.nanos(), Tool.getLookDirection(), isLeftDown, isCtrlDown, true);
                  Vec3 currentTargetx = gizmo.getTargetVec();
                  if (!previousTargetx.equals(currentTargetx)) {
                     Annotations.push(
                        new AnnotationHistoryElement(
                           new AnnotationUpdateAction.MoveAnnotation(entryxx.getKey(), previousTargetx.toVector3f()),
                           new AnnotationUpdateAction.MoveAnnotation(entryxx.getKey(), currentTargetx.toVector3f())
                        )
                     );
                  }

                  if (wasGrabbedx && gizmo.isGrabbed()) {
                     gizmo.render(rc, isCtrlDown);
                  } else if (!isCtrlDown) {
                     gizmo.render(rc, false);
                  }
               }
            }
         }

         if (!showedSelectedGizmo) {
            this.selectedGizmo = null;
            this.selectedAnnotation = null;
            this.lastSelectedAnnotation = null;
         }
      }
   }

   private void update(Vec3 cameraPos) {
      if (Tool.cancelUsing()) {
         this.reset();
      }

      this.updateErase();
      this.updateOutline();
      this.updateDraw(cameraPos);
   }

   private void updateErase() {
      if (this.deleteMouseDragStart != null && !Tool.isMouseDown(1)) {
         Vec2 end = new Vec2(ImGui.getMousePosX(), ImGui.getMousePosY());
         Annotations.erase(this.deleteMouseDragStart, end);
         this.reset();
      }
   }

   private void updateOutline() {
      if (this.outlinePather != null) {
         this.outlinePather.update((x, y, z) -> {
            if (this.startOutline == null) {
               this.startOutline = new BlockPos(x, y, z);
               this.lastOutlinePos.set(x, y, z);
            } else {
               int dx = x - this.lastOutlinePos.getX();
               int dy = y - this.lastOutlinePos.getY();
               int dz = z - this.lastOutlinePos.getZ();
               if (dx != 0 || dy != 0 || dz != 0) {
                  Direction direction = PositionUtils.directionFromDelta(dx, dy, dz);
                  Objects.requireNonNull(direction);
                  byte directionValue = (byte)direction.get3DDataValue();
                  int indexModulo = this.outlineOffsetIndex % 3;
                  if (indexModulo == 0) {
                     this.outlineOffsets.add(directionValue);
                  } else {
                     byte last = this.outlineOffsets.getByte(this.outlineOffsets.size() - 1);
                     if (indexModulo == 1) {
                        this.outlineOffsets.set(this.outlineOffsets.size() - 1, (byte)(last + directionValue * 6));
                     } else {
                        this.outlineOffsets.set(this.outlineOffsets.size() - 1, (byte)(last + directionValue * 6 * 6));
                     }
                  }

                  this.outlineOffsetIndex++;
                  this.lastOutlinePos.set(x, y, z);
               }
            }
         });
         if (!Tool.isMouseDown(1)) {
            FreehandOutlineAnnotationData annotation = new FreehandOutlineAnnotationData(
               this.startOutline, this.outlineOffsets.toByteArray(), this.outlineOffsetIndex, colourFromFloat(this.colour[0], this.colour[1], this.colour[2])
            );
            Annotations.pushCreateAnnotation(annotation);
            this.reset();
         }
      }
   }

   private void updateDraw(Vec3 cameraPos) {
      if (this.linePather != null) {
         Vec3i cameraPosition = quantize(cameraPos);
         double distanceSq = cameraPos.distanceToSqr(this.lastRaycastPosition);
         double distance = Math.sqrt(distanceSq);
         int levelOfDetail = (int)Math.max(1.0, Math.ceil(distance / 16.0));
         double threshold = levelOfDetail / 16.0;
         double thresholdSq = threshold * threshold;
         this.linePather.update(raycastResult -> {
            boolean emitPoint = this.lastRaycastPosition.distanceToSqr(raycastResult.getLocation()) > thresholdSq;
            emitPoint |= this.currentDirection == null || this.currentDirection != raycastResult.direction();
            if (emitPoint && !this.raycastPositions.isEmpty()) {
               double sumX = 0.0;
               double sumY = 0.0;
               double sumZ = 0.0;
               int sumCount = 0;

               for (Vec3 raycastPosition : this.raycastPositions) {
                  sumX += raycastPosition.x;
                  sumY += raycastPosition.y;
                  sumZ += raycastPosition.z;
                  sumCount++;
               }

               Vec3 position = new Vec3(sumX / sumCount, sumY / sumCount, sumZ / sumCount);
               Vec3i quantized = quantize(position);
               if (!quantized.equals(this.lastQuantized)) {
                  this.handleDrawFromTo(cameraPosition, this.lastQuantized, quantized, this.lastUsedDirection.getAxis(), this.currentDirection.getAxis());
                  this.lastQuantized = quantized;
                  this.lastUsedDirection = this.currentDirection;
                  this.lastRaycastPosition = position;
               }

               this.raycastPositions.clear();
            }

            this.currentDirection = raycastResult.direction();
            this.raycastPositions.add(raycastResult.getLocation());
         });
         if (!Tool.isMouseDown(1)) {
            if (!this.quantizedLineOffsets.isEmpty()) {
               LineAnnotationData annotation = new LineAnnotationData(
                  this.startQuantized,
                  this.quantizedLineOffsets.toByteArray(),
                  this.lineWidth[0],
                  colourFromFloat(this.colour[0], this.colour[1], this.colour[2])
               );
               Annotations.pushCreateAnnotation(annotation);
            }

            this.reset();
         }
      }
   }

   private void handleDrawFromTo(Vec3i cameraPosition, Vec3i from, Vec3i to, Axis fromAxis, Axis toAxis) {
      if (!from.equals(to)) {
         if (fromAxis != toAxis) {
            Vec3i middle = switch (fromAxis) {
               case X -> new Vec3i(from.getX(), to.getY(), to.getZ());
               case Y -> new Vec3i(to.getX(), from.getY(), to.getZ());
               case Z -> new Vec3i(to.getX(), to.getY(), from.getZ());
               default -> throw new IncompatibleClassChangeError();
            };
            this.handleDrawFromTo(cameraPosition, from, middle, fromAxis, fromAxis);
            this.handleDrawFromTo(cameraPosition, middle, to, toAxis, toAxis);
         } else {
            if (fromAxis.choose(from.getX(), from.getY(), from.getZ()) != fromAxis.choose(to.getX(), to.getY(), to.getZ())) {
               switch (fromAxis) {
                  case X:
                     Vector2i from2dxx = new Vector2i(from.getY(), from.getZ());
                     Vector2i to2dxx = new Vector2i(to.getY(), to.getZ());
                     if (cameraPosition.distSqr(from) > cameraPosition.distSqr(to)) {
                        Vector2i temp = from2dxx;
                        from2dxx = to2dxx;
                        to2dxx = temp;
                     }

                     Vector2i middle = Rasterization2D.bresenhamReturn(from2dxx, to2dxx, (y, z) -> y % 16 != 0 && z % 16 != 0 ? null : new Vector2i(y, z));
                     if (middle == null) {
                        middle = new Vector2i((to.getY() + from.getY()) / 2, (to.getZ() + from.getZ()) / 2);
                     }

                     Axis axis = leftQuantizedCloserToEdge(middle.x, middle.y) ? Axis.Y : Axis.Z;
                     this.handleDrawFromTo(cameraPosition, from, new Vec3i(from.getX(), middle.x, middle.y), fromAxis, fromAxis);
                     this.handleDrawFromTo(cameraPosition, new Vec3i(from.getX(), middle.x, middle.y), new Vec3i(to.getX(), middle.x, middle.y), axis, axis);
                     this.handleDrawFromTo(cameraPosition, new Vec3i(to.getX(), middle.x, middle.y), to, toAxis, toAxis);
                     return;
                  case Y:
                     Vector2i from2dx = new Vector2i(from.getX(), from.getZ());
                     Vector2i to2dx = new Vector2i(to.getX(), to.getZ());
                     if (cameraPosition.distSqr(from) > cameraPosition.distSqr(to)) {
                        Vector2i temp = from2dx;
                        from2dx = to2dx;
                        to2dx = temp;
                     }

                     middle = Rasterization2D.bresenhamReturn(from2dx, to2dx, (x, z) -> x % 16 != 0 && z % 16 != 0 ? null : new Vector2i(x, z));
                     if (middle == null) {
                        middle = new Vector2i((to.getX() + from.getX()) / 2, (to.getZ() + from.getZ()) / 2);
                     }

                     axis = leftQuantizedCloserToEdge(middle.x, middle.y) ? Axis.X : Axis.Z;
                     this.handleDrawFromTo(cameraPosition, from, new Vec3i(middle.x, from.getY(), middle.y), fromAxis, fromAxis);
                     this.handleDrawFromTo(cameraPosition, new Vec3i(middle.x, from.getY(), middle.y), new Vec3i(middle.x, to.getY(), middle.y), axis, axis);
                     this.handleDrawFromTo(cameraPosition, new Vec3i(middle.x, to.getY(), middle.y), to, toAxis, toAxis);
                     return;
                  case Z:
                     Vector2i from2d = new Vector2i(from.getX(), from.getY());
                     Vector2i to2d = new Vector2i(to.getX(), to.getY());
                     if (cameraPosition.distSqr(from) > cameraPosition.distSqr(to)) {
                        Vector2i temp = from2d;
                        from2d = to2d;
                        to2d = temp;
                     }

                     middle = Rasterization2D.bresenhamReturn(from2d, to2d, (x, y) -> x % 16 != 0 && y % 16 != 0 ? null : new Vector2i(x, y));
                     if (middle == null) {
                        middle = new Vector2i((to.getX() + from.getX()) / 2, (to.getY() + from.getY()) / 2);
                     }

                     axis = leftQuantizedCloserToEdge(middle.x, middle.y) ? Axis.X : Axis.Y;
                     this.handleDrawFromTo(cameraPosition, from, new Vec3i(middle.x, middle.y, from.getZ()), fromAxis, fromAxis);
                     this.handleDrawFromTo(cameraPosition, new Vec3i(middle.x, middle.y, from.getZ()), new Vec3i(middle.x, middle.y, to.getZ()), axis, axis);
                     this.handleDrawFromTo(cameraPosition, new Vec3i(middle.x, middle.y, to.getZ()), to, toAxis, toAxis);
                     return;
               }
            }

            MutableBlockPos last = new MutableBlockPos(from.getX(), from.getY(), from.getZ());
            Rasterization3D.bresenhamSkipFrom(
               from,
               to,
               (x1, y1, z1) -> {
                  int dx = x1 - last.getX();
                  int dy = y1 - last.getY();
                  int dz = z1 - last.getZ();

                  while (!this.quantizedLineOffsets.isEmpty()) {
                     byte lastOffsetId = this.quantizedLineOffsets.getByte(this.quantizedLineOffsets.size() - 1);
                     AnnotationByteOffset lastOffset = AnnotationByteOffset.idToOffset(lastOffsetId);
                     if (lastOffset.axis() != fromAxis
                        || !compatibleSign(lastOffset.dx(), dx)
                        || !compatibleSign(lastOffset.dy(), dy)
                        || !compatibleSign(lastOffset.dz(), dz)) {
                        break;
                     }

                     int combinedOffsetId = AnnotationByteOffset.offsetToId(lastOffset.dx() + dx, lastOffset.dy() + dy, lastOffset.dz() + dz, lastOffset.axis());
                     if (combinedOffsetId < 0) {
                        break;
                     }

                     this.quantizedLineOffsets.removeByte(this.quantizedLineOffsets.size() - 1);
                     dx += lastOffset.dx();
                     dy += lastOffset.dy();
                     dz += lastOffset.dz();
                  }

                  if (dx != 0 || dy != 0 || dz != 0) {
                     int offset = AnnotationByteOffset.offsetToId(dx, dy, dz, fromAxis);
                     if (offset == -1) {
                        throw new FaultyImplementationError();
                     }

                     this.quantizedLineOffsets.add((byte)offset);
                  }

                  last.set(x1, y1, z1);
               }
            );
         }
      }
   }

   @Override
   public void displayImguiOptions() {
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.ANNOTATION_UPDATE)) {
         ImGui.textWrapped("⚠ The server does not allow creating annotations");
      } else if (annotationsDisabled.contains(AnnotationsDisabled.UNAVAILABLE)) {
         ImGui.textWrapped("⚠ Annotations in Multiplayer is a Commercial License feature. Click for more information");
         ImGuiHelper.openCommercialLicenseOnClick();
      } else if (!this.outlineLines.isEmpty()) {
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.finish_lines"))) {
            LinesOutlineAnnotationData annotation = new LinesOutlineAnnotationData(
               this.outlineLines.toLongArray(), colourFromFloat(this.colour[0], this.colour[1], this.colour[2])
            );
            Annotations.pushCreateAnnotation(annotation);
            this.reset();
         }
      } else {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.annotation"));
         float toolButtonSizeX = (32.0F + ImGui.getStyle().getFramePaddingX() * 2.0F) * EditorUI.getUiScale();
         float toolButtonSizeY = (32.0F + ImGui.getStyle().getFramePaddingY() * 2.0F) * EditorUI.getUiScale();
         float strideX = toolButtonSizeX + ImGui.getStyle().getItemInnerSpacingX();
         float strideY = toolButtonSizeY + ImGui.getStyle().getItemInnerSpacingY();
         int rows = Math.max(1, (int)Math.ceil(strideX * TOOL_NAMES.length / (ImGui.getContentRegionAvailX() + toolButtonSizeX / 3.0F)));
         int buttonsPerRow = (int)Math.ceil((float)TOOL_NAMES.length / rows);
         float currentX = ImGui.getCursorPosX();
         float currentY = ImGui.getCursorPosY();
         float originalX = currentX;
         int buttonActiveCol = ImGui.getColorU32(23);
         ImGui.pushFont(EditorUI.icons, EditorUI.icons.getLegacySize());
         int activeTool = this.tool[0];

         for (int i = 0; i <= 5; i++) {
            if (i > 0 && i % buttonsPerRow == 0) {
               currentX = originalX;
               currentY += strideY;
            }

            ImGui.setCursorPos(currentX, currentY);
            if (i == activeTool) {
               ImGuiHelper.pushStyleColor(21, buttonActiveCol);
            }

            ImGui.pushID(i);
            if (ImGui.button(TOOL_ICONS[i], toolButtonSizeX, toolButtonSizeY) && i != activeTool) {
               this.tool[0] = i;
               this.reset();
            }

            ImGui.popID();
            if (i == activeTool) {
               ImGuiHelper.popStyleColor();
            }

            if (ImGui.isItemHovered()) {
               ImGui.popFont();
               ImGui.beginTooltip();
               ImGui.text(TOOL_NAMES[i]);
               ImGui.endTooltip();
               ImGui.pushFont(EditorUI.icons, EditorUI.icons.getLegacySize());
            }

            currentX += strideX;
         }

         ImGui.popFont();
         if (this.deleteMouseDragStart != null) {
            float mouseX = ImGui.getMousePosX();
            float mouseY = ImGui.getMousePosY();
            float minX = Math.min(this.deleteMouseDragStart.x, mouseX);
            float maxX = Math.max(this.deleteMouseDragStart.x, mouseX);
            float minY = Math.min(this.deleteMouseDragStart.y, mouseY);
            float maxY = Math.max(this.deleteMouseDragStart.y, mouseY);
            ImGui.getForegroundDrawList().addRectFilled(minX, minY, maxX, maxY, -2147483393, 4);
            ImGui.getForegroundDrawList().addRect(minX, minY, maxX, maxY, -16776961);
         }

         if (this.tool[0] == 0) {
            ImGuiHelper.separatorWithText("Options");
            ImGui.sliderFloat("Line Width", this.lineWidth, 1.0F, 8.0F);
            ImGuiHelper.colorPicker3WithBlockDrop("Color", this.colour);
         } else if (this.tool[0] == 1) {
            ImGuiHelper.separatorWithText("Options");
            ImGuiHelper.combo("Mode", this.outlineMode, new String[]{"Freehand", "Lines", "Box"});
            if (this.outlineMode[0] == 2 && this.maxBoxSizeReached) {
               ImGui.textWrapped("⚠ Maximum outline box size reached. Boxes with a volume of >1,000,000 are disallowed for performance reasons");
            } else {
               ImGuiHelper.colorPicker3WithBlockDrop("Color", this.colour);
            }
         } else if (this.tool[0] == 2) {
            ImGuiHelper.separatorWithText("Options");
            this.renderTextOptions();
         } else if (this.tool[0] == 3) {
            ImGuiHelper.separatorWithText("Options");
            this.renderImageOptions();
         } else if (this.tool[0] == 5 && this.selectedAnnotation != null) {
            AnnotationData annotationData = Annotations.getData(this.selectedAnnotation);
            if (!Objects.equals(this.lastSelectedAnnotation, this.selectedAnnotation)) {
               this.lastSelectedAnnotation = this.selectedAnnotation;
               if (annotationData instanceof TextAnnotationData textAnnotationData) {
                  this.textShadow = textAnnotationData.shadow();
                  this.textScale[0] = Math.round(textAnnotationData.scale());
                  this.billboardMode[0] = textAnnotationData.billboardMode();
                  this.textContent.set(textAnnotationData.text());
                  this.colour[0] = (textAnnotationData.colour() >> 16 & 0xFF) / 255.0F;
                  this.colour[1] = (textAnnotationData.colour() >> 8 & 0xFF) / 255.0F;
                  this.colour[2] = (textAnnotationData.colour() & 0xFF) / 255.0F;
                  this.opacity[0] = (textAnnotationData.colour() >> 24 & 0xFF) / 255.0F;
               } else if (annotationData instanceof ImageAnnotationData imageAnnotationData) {
                  this.imageWidth[0] = Math.round(imageAnnotationData.width());
                  this.billboardMode[0] = imageAnnotationData.billboardMode();
                  this.imageUrl.set(imageAnnotationData.imageUrl());
                  this.opacity[0] = imageAnnotationData.opacity();
               }
            }

            if (annotationData instanceof TextAnnotationData oldData) {
               ImGuiHelper.separatorWithText("Options");
               this.renderTextOptions();
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.update_lbl"))) {
                  TextAnnotationData newData = this.createTextAnnotationData(oldData.position(), oldData.fallbackYaw(), oldData.direction());
                  if (!Objects.equals(newData, oldData)) {
                     Annotations.pushUpdateAnnotation(this.selectedAnnotation, oldData, newData);
                     this.justPlacedAnnotation = this.selectedAnnotation;
                  }
               }
            } else if (annotationData instanceof ImageAnnotationData oldDatax) {
               ImGuiHelper.separatorWithText("Options");
               this.renderImageOptions();
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.update_lbl"))) {
                  ImageAnnotationData newData = this.createImageAnnotationData(oldDatax.position(), oldDatax.fallbackYaw(), oldDatax.direction());
                  if (!Objects.equals(newData, oldDatax)) {
                     Annotations.pushUpdateAnnotation(this.selectedAnnotation, oldDatax, newData);
                     this.justPlacedAnnotation = this.selectedAnnotation;
                  }
               }
            }
         }

         if (this.tool[0] != 5 && this.tool[0] != 4) {
            ImGui.separator();
            if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.raycast_fluids"), this.raycastFluids)) {
               this.raycastFluids = !this.raycastFluids;
            }
         }

         ImGui.separator();
         int annotationCount = Annotations.totalCount();
         int visibleAnnotationCount = Annotations.visibleAnnotations().size();
         ImGui.text(AxiomI18n.get("axiom.hardcoded.total_annotations") + annotationCount);
         ImGui.text(AxiomI18n.get("axiom.hardcoded.annotations_in_range") + visibleAnnotationCount);
         if (this.tool[0] == 2 && ImGui.button(AxiomI18n.get("axiom.hardcoded.view_all_text_annotations"))) {
            EditorWindowType.TEXT_ANNOTATIONS.setOpen(true);
         }

         if (this.tool[0] == 4 && annotationCount > 0) {
            boolean allowedToClearAll = AxiomClient.hasPermission(AxiomPermission.ANNOTATION_CLEARALL);
            if (!allowedToClearAll) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.hardcoded.clear_all_annotations")) && allowedToClearAll) {
               ImGui.openPopup("##ConfirmClear");
            }

            if (!allowedToClearAll) {
               ImGui.endDisabled();
               ImGuiHelper.tooltip("Server has not given you permission to clear all annotations", 1024);
            }

            if (ImGui.beginPopup("##ConfirmClear") && allowedToClearAll) {
               ImGui.pushTextWrapPos(300.0F);
               ImGui.textWrapped(AxiomI18n.get("axiom.hardcoded.clear_annotations_confirm"));
               ImGui.popTextWrapPos();
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.im_sure_delete_them"))) {
                  new AxiomServerboundAnnotationUpdate(List.of(new AnnotationUpdateAction.ClearAllAnnotations())).send();
                  Annotations.clear();
                  ImGui.closeCurrentPopup();
               }

               ImGui.sameLine();
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
                  ImGui.closeCurrentPopup();
               }

               ImGui.endPopup();
            }
         }
      }
   }

   private void renderImageOptions() {
      ImGui.sliderInt("Width", this.imageWidth, 1, 64);
      ImGuiHelper.combo("Billboard", this.billboardMode, new String[]{"Fixed", "Horizontal", "Vertical", "Center"});
      ImGui.inputText("Image URL", this.imageUrl);
      ImGui.sliderFloat("Opacity", this.opacity, 0.2F, 1.0F);
   }

   private void renderTextOptions() {
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.shadow"), this.textShadow)) {
         this.textShadow = !this.textShadow;
      }

      ImGui.sliderInt("Scale", this.textScale, 1, 32);
      ImGuiHelper.combo("Billboard", this.billboardMode, new String[]{"Fixed", "Horizontal", "Vertical", "Center"});
      ImGui.inputTextMultiline("Text", this.textContent);
      char[] charPresets = new char[]{'✔', '✖', '☠', '☢', '❤', '⬆', '⬇', '➡', '⬅', '✏'};

      for (int i = 0; i < charPresets.length; i++) {
         if (i > 0 && i % 5 != 0) {
            ImGui.sameLine();
         }

         if (ImGui.button(String.valueOf(charPresets[i]))) {
            this.textContent.set(String.valueOf(charPresets[i]));
         }
      }

      ImGuiHelper.colorPicker3WithBlockDrop("Color", this.colour);
      ImGui.sliderFloat("Opacity", this.opacity, 0.2F, 1.0F);
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.annotation");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
   }

   @Override
   public void loadSettings(CompoundTag tag) {
   }

   @Override
   public char iconChar() {
      return '\ue921';
   }

   @Override
   public String keybindId() {
      return "annotation";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_ANNOTATION);
   }
}
