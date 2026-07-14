package com.moulberry.axiom.displayentity;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.mojang.brigadier.StringReader;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Transformation;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.packets.AxiomServerboundDeleteEntity;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.screen.EditDisplayEntityScreen;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.InputHelper;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.Display.GenericInterpolator;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.Display.RenderState;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

public class DisplayEntityManipulator {
   private static Map<Entity, Gizmo> gizmos = new HashMap<>();
   private static final Map<UUID, DisplayEntityManipulator.PendingUpdate> pendingUpdates = new HashMap<>();
   private static Entity activeEntity = null;
   private static boolean hoveredGizmoPart = false;
   private static Matrix4f overrideMatrix = null;
   private static int overrideMatrixTicks = 0;
   public static GizmoMode gizmoMode = GizmoMode.GLOBAL;
   private static final List<DisplayEntityManipulator.UndoState> undoStates = new ArrayList<>();
   private static final List<DisplayEntityManipulator.UndoState> redoStates = new ArrayList<>();
   private static boolean movementDirty = false;
   private static Matrix4f previewMultiplyTransformMatrix = null;

   public static void tick() {
      if (overrideMatrixTicks > 0) {
         overrideMatrixTicks--;
      }

      LocalPlayer player = Minecraft.getInstance().player;
      ClientLevel level = Minecraft.getInstance().level;
      if (player != null
         && level != null
         && !EditorUI.isActive()
         && AxiomClient.isAxiomActive()
         && ClientEvents.serverSupportsProtocol(SupportedProtocol.MANIPULATE_ENTITY)
         && Axiom.configuration.entityManipulation.showDisplayEntities) {
         if (!pendingUpdates.isEmpty()) {
            List<AxiomServerboundManipulateEntity.ManipulateEntry> entries = new ArrayList<>();

            for (Entry<UUID, DisplayEntityManipulator.PendingUpdate> entry : pendingUpdates.entrySet()) {
               entries.add(new AxiomServerboundManipulateEntity.ManipulateEntry(entry.getKey(), entry.getValue().position, entry.getValue().mergeNbt));
            }

            new AxiomServerboundManipulateEntity(entries).send();
         }

         pendingUpdates.clear();
         AABB aabb = player.getBoundingBox().inflate(Math.max(1, Axiom.configuration.entityManipulation.displayEntityRange));
         Map<Entity, Gizmo> newGizmos = new HashMap<>();

         for (Display display : level.getEntitiesOfClass(Display.class, aabb)) {
            if (!display.isPassenger()) {
               if (AxiomClient.ignoredDisplayEntities.contains(display.getUUID())) {
                  if (display == activeEntity) {
                     activeEntity = null;
                  }

                  gizmos.remove(display);
               } else {
                  Gizmo oldGizmo = gizmos.get(display);
                  if (oldGizmo != null) {
                     if (!oldGizmo.isGrabbed() && overrideMatrixTicks == 0) {
                        oldGizmo.moveToVec(display.position());
                     }

                     newGizmos.put(display, oldGizmo);
                  } else {
                     Gizmo gizmo = new Gizmo(display.position());
                     gizmo.enableAxes = false;
                     gizmo.enableRotation = true;
                     gizmo.enableScale = true;
                     gizmo.rotationSnapRadians = (float)Math.toRadians(1.0);
                     gizmo.minVisualScale = 2.0F;
                     gizmo.translationSnapping = 16;
                     newGizmos.put(display, gizmo);
                  }
               }
            }
         }

         for (Interaction interaction : level.getEntitiesOfClass(Interaction.class, aabb)) {
            if (!interaction.isPassenger()) {
               if (AxiomClient.ignoredDisplayEntities.contains(interaction.getUUID())) {
                  if (interaction == activeEntity) {
                     activeEntity = null;
                  }

                  gizmos.remove(interaction);
               } else {
                  Gizmo oldGizmo = gizmos.get(interaction);
                  if (oldGizmo != null) {
                     if (!oldGizmo.isGrabbed() && overrideMatrixTicks == 0) {
                        oldGizmo.moveToVec(interaction.position());
                     }

                     newGizmos.put(interaction, oldGizmo);
                  } else {
                     Gizmo gizmo = new Gizmo(interaction.position());
                     gizmo.enableAxes = false;
                     gizmo.enableRotation = false;
                     gizmo.enableScale = true;
                     gizmo.minVisualScale = 2.0F;
                     gizmo.translationSnapping = 16;
                     gizmo.centerColour = 11005692;
                     newGizmos.put(interaction, gizmo);
                  }
               }
            }
         }

         if (activeEntity != null) {
            if (activeEntity.isRemoved() || activeEntity.isPassenger()) {
               newGizmos.remove(activeEntity);
               activeEntity = null;
               movementDirty = false;
               undoStates.clear();
               redoStates.clear();
            } else if (!newGizmos.containsKey(activeEntity)) {
               Gizmo gizmo = gizmos.get(activeEntity);
               if (gizmo != null) {
                  newGizmos.put(activeEntity, gizmo);
               } else {
                  activeEntity = null;
                  movementDirty = false;
                  undoStates.clear();
                  redoStates.clear();
               }
            }
         }

         gizmos = newGizmos;
      } else {
         if (activeEntity != null) {
            if (activeEntity.isRemoved()) {
               gizmos.remove(activeEntity);
            } else {
               Gizmo gizmo = gizmos.get(activeEntity);
               if (gizmo != null) {
                  gizmo.enableAxes = false;
               }
            }

            activeEntity = null;
            movementDirty = false;
            undoStates.clear();
            redoStates.clear();
         }

         pendingUpdates.clear();
         gizmos.clear();
      }
   }

   public static void render(AxiomWorldRenderContext rc) {
      previewMultiplyTransformMatrix = null;
      hoveredGizmoPart = false;
      if (Axiom.configuration.entityManipulation.showDisplayEntities) {
         if (!EditorUI.isActive()) {
            Vec3 lookDirection = Tool.getLookDirection();
            boolean isLeftDown = Axiom.configuration.entityManipulation.swapLeftRightClick ? Tool.isMouseDown(1) : Tool.isMouseDown(0);
            boolean isCtrlDown = InputHelper.isCtrlOrCmdDownRaw();

            for (Entry<Entity, Gizmo> entry : gizmos.entrySet()) {
               Entity entity = entry.getKey();
               Gizmo gizmo = entry.getValue();
               Vec3 before = gizmo.getTargetVec();
               if (gizmo.enableAxes) {
                  if (entity instanceof Display display && gizmoMode == GizmoMode.LOCAL) {
                     Matrix4f matrix4f = getTransformationMatrix(display);
                     Quaternionf orientation = calculateOrientation(display.renderState(), display, new Quaternionf());
                     matrix4f.rotateLocal(orientation);
                     float f = 1.0F / matrix4f.m33();
                     Triple<Quaternionf, Vector3f, Quaternionf> triple = MatrixUtil.svdDecompose(new Matrix3f(matrix4f).scale(f));
                     gizmo.localRotation = ((Quaternionf)triple.getLeft()).mul((Quaternionfc)triple.getRight());
                     gizmo.setAxisDirections(true, true, true);
                  } else {
                     gizmo.localRotation = null;
                     gizmo.setAxisDirections(rc.x() > gizmo.getTargetVec().x, rc.y() > gizmo.getTargetVec().y, rc.z() > gizmo.getTargetVec().z);
                  }
               }

               gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, true);
               if (!Minecraft.getInstance().options.hideGui) {
                  gizmo.render(rc, isCtrlDown);
               }

               if (gizmo.isHovered()) {
                  hoveredGizmoPart = true;
               }

               if (entity == activeEntity) {
                  if (gizmo.isGrabbed()) {
                     movementDirty = true;
                  }

                  Gizmo.GizmoRotation gizmoRotation = gizmo.peekGizmoRotation();
                  if (gizmoRotation != null && entity instanceof Display display) {
                     Matrix4f matrix4f = getTransformationMatrix(display);
                     Quaternionf orientation = calculateOrientation(display.renderState(), display, new Quaternionf());
                     matrix4f.rotateLocal(orientation);
                     applyGizmoRotation(matrix4f, gizmoRotation);
                     matrix4f.rotateLocal(orientation.invert());
                     float f = 1.0F / matrix4f.m33();
                     Triple<Quaternionf, Vector3f, Quaternionf> triple = MatrixUtil.svdDecompose(new Matrix3f(matrix4f).scale(f));
                     Quaternionf quaternionf = ((Quaternionf)triple.getLeft()).mul((Quaternionfc)triple.getRight());
                     Vector3f eulerAngles = new Vector3f();
                     float asinFactor = quaternionf.y * quaternionf.z - quaternionf.w * quaternionf.x;
                     if (asinFactor >= 0.49999997F) {
                        eulerAngles.x = -90.0F;
                     } else if (asinFactor <= -0.49999997F) {
                        eulerAngles.x = 90.0F;
                     } else {
                        eulerAngles.x = (float)Math.asin(-2.0F * asinFactor);
                        eulerAngles.x = (float)Math.toDegrees(eulerAngles.x);
                     }

                     eulerAngles.y = (float)Math.atan2(
                        quaternionf.x * quaternionf.z + quaternionf.y * quaternionf.w, 0.5F - quaternionf.y * quaternionf.y - quaternionf.x * quaternionf.x
                     );
                     eulerAngles.z = (float)Math.atan2(
                        quaternionf.y * quaternionf.x + quaternionf.w * quaternionf.z, 0.5F - quaternionf.x * quaternionf.x - quaternionf.z * quaternionf.z
                     );
                     eulerAngles.y = (float)Math.toDegrees(eulerAngles.y);
                     eulerAngles.z = (float)Math.toDegrees(eulerAngles.z);
                     if (eulerAngles.x <= 0.0F && eulerAngles.x > -0.01) {
                        eulerAngles.x = 0.0F;
                     }

                     if (eulerAngles.y <= 0.0F && eulerAngles.y > -0.01) {
                        eulerAngles.y = 0.0F;
                     }

                     if (eulerAngles.z <= 0.0F && eulerAngles.z > -0.01) {
                        eulerAngles.z = 0.0F;
                     }

                     String paddedRotationX = String.format("%.2f", eulerAngles.x);
                     String paddedRotationY = String.format("%.2f", eulerAngles.y);
                     String paddedRotationZ = String.format("%.2f", eulerAngles.z);
                     ScreenRenderHook.setOverlayText(
                        Component.literal(AxiomI18n.get("axiom.hardcoded.rotation_prefix"))
                           .withStyle(ChatFormatting.YELLOW)
                           .append(Component.literal(paddedRotationX + " ").withStyle(ChatFormatting.RED))
                           .append(Component.literal(paddedRotationY + " ").withStyle(ChatFormatting.GREEN))
                           .append(Component.literal(paddedRotationZ).withStyle(ChatFormatting.AQUA))
                     );
                  }

                  gizmoRotation = gizmo.popRotation();
                  if (gizmoRotation != null && entity instanceof Display display) {
                     Map<UUID, Matrix4fc> transforms = new HashMap<>();
                     applyGizmoRotationToEntityRecursive(display, gizmoRotation, transforms);
                     movementDirty = false;
                     undoStates.add(new DisplayEntityManipulator.UndoState(gizmo.getTargetVec(), transforms, null, null));
                     if (undoStates.size() > 256) {
                        undoStates.remove(0);
                     }

                     redoStates.clear();
                  }

                  Gizmo.GizmoScale gizmoScale = gizmo.peekScale();
                  if (gizmoScale != null) {
                     if (entity instanceof Display display) {
                        Matrix4f matrix4fx = getTransformationMatrix(display);
                        Quaternionf orientationx = calculateOrientation(display.renderState(), display, new Quaternionf());
                        matrix4fx.rotateLocal(orientationx);
                        applyGizmoScale(matrix4fx, gizmoScale);
                        matrix4fx.rotateLocal(orientationx.invert());
                        float fx = 1.0F / matrix4fx.m33();
                        Triple<Quaternionf, Vector3f, Quaternionf> triplex = MatrixUtil.svdDecompose(new Matrix3f(matrix4fx).scale(fx));
                        Vector3f scale = (Vector3f)triplex.getMiddle();
                        if (scale.x <= 0.0F && scale.x > -1.0E-4) {
                           scale.x = 0.0F;
                        }

                        if (scale.y <= 0.0F && scale.y > -1.0E-4) {
                           scale.y = 0.0F;
                        }

                        if (scale.z <= 0.0F && scale.z > -1.0E-4) {
                           scale.z = 0.0F;
                        }

                        String paddedScaleX = String.format("%.3f", scale.x);
                        String paddedScaleY = String.format("%.3f", scale.y);
                        String paddedScaleZ = String.format("%.3f", scale.z);
                        ScreenRenderHook.setOverlayText(
                           Component.literal(AxiomI18n.get("axiom.hardcoded.scale_prefix"))
                              .withStyle(ChatFormatting.YELLOW)
                              .append(Component.literal(paddedScaleX + " ").withStyle(ChatFormatting.RED))
                              .append(Component.literal(paddedScaleY + " ").withStyle(ChatFormatting.GREEN))
                              .append(Component.literal(paddedScaleZ).withStyle(ChatFormatting.AQUA))
                        );
                     } else if (entity instanceof Interaction interaction) {
                        float width = interaction.getWidth() * gizmoScale.getScaleXZ();
                        float height = interaction.getHeight() * gizmoScale.getScaleY();
                        if (!isShiftDown()) {
                           if (gizmoScale.getScaleXZ() != 1.0F) {
                              width = Math.round(width * 16.0F) / 16.0F;
                           }

                           if (gizmoScale.getScaleY() != 1.0F) {
                              height = Math.round(height * 16.0F) / 16.0F;
                           }
                        }

                        if (width <= 0.0F && width > -1.0E-4) {
                           width = 0.0F;
                        }

                        if (height <= 0.0F && height > -1.0E-4) {
                           height = 0.0F;
                        }

                        String paddedWidth = String.format("%.3f", width);
                        String paddedHeight = String.format("%.3f", height);
                        ScreenRenderHook.setOverlayText(
                           Component.literal(AxiomI18n.get("axiom.hardcoded.size_prefix"))
                              .withStyle(ChatFormatting.YELLOW)
                              .append(Component.literal(paddedWidth + " ").withStyle(ChatFormatting.RED))
                              .append(Component.literal(paddedHeight + " ").withStyle(ChatFormatting.GREEN))
                        );
                     }
                  }

                  gizmoScale = gizmo.popScale();
                  if (gizmoScale != null && !gizmoScale.isIdentity()) {
                     if (entity instanceof Display display) {
                        Map<UUID, Matrix4fc> transforms = new HashMap<>();
                        applyGizmoScaleToEntityRecursive(display, gizmoScale, transforms);
                        movementDirty = false;
                        undoStates.add(new DisplayEntityManipulator.UndoState(gizmo.getTargetVec(), transforms, null, null));
                        if (undoStates.size() > 256) {
                           undoStates.remove(0);
                        }

                        redoStates.clear();
                     } else if (entity instanceof Interaction interaction) {
                        CompoundTag root = new CompoundTag();
                        float widthx = interaction.getWidth() * gizmoScale.getScaleXZ();
                        float heightx = interaction.getHeight() * gizmoScale.getScaleY();
                        if (!isShiftDown()) {
                           if (gizmoScale.getScaleXZ() != 1.0F) {
                              widthx = Math.round(widthx * 16.0F) / 16.0F;
                           }

                           if (gizmoScale.getScaleY() != 1.0F) {
                              heightx = Math.round(heightx * 16.0F) / 16.0F;
                           }
                        }

                        root.putFloat("width", widthx);
                        root.putFloat("height", heightx);
                        DisplayEntityManipulator.PendingUpdate pendingUpdate = pendingUpdates.computeIfAbsent(
                           interaction.getUUID(), k -> new DisplayEntityManipulator.PendingUpdate()
                        );
                        pendingUpdate.mergeNbt = pendingUpdate.mergeNbt.merge(root);
                        movementDirty = false;
                        undoStates.add(new DisplayEntityManipulator.UndoState(gizmo.getTargetVec(), new HashMap<>(), widthx, heightx));
                        if (undoStates.size() > 256) {
                           undoStates.remove(0);
                        }

                        redoStates.clear();
                     }
                  }

                  Vec3 position = gizmo.getTargetVec();
                  if (!position.equals(before)) {
                     applyTeleport(entity, position);
                     String paddedPositionX = String.format("%.4f", position.x);
                     String paddedPositionY = String.format("%.4f", position.y);
                     String paddedPositionZ = String.format("%.4f", position.z);
                     ScreenRenderHook.setOverlayText(
                        Component.literal(AxiomI18n.get("axiom.hardcoded.position_prefix"))
                           .withStyle(ChatFormatting.YELLOW)
                           .append(Component.literal(paddedPositionX + " ").withStyle(ChatFormatting.RED))
                           .append(Component.literal(paddedPositionY + " ").withStyle(ChatFormatting.GREEN))
                           .append(Component.literal(paddedPositionZ).withStyle(ChatFormatting.AQUA))
                     );
                  }

                  if (movementDirty && !gizmo.isGrabbed()) {
                     movementDirty = false;
                     addTranslationUndoState(position);
                  }
               }
            }
         }
      }
   }

   private static void addTranslationUndoState(Vec3 position) {
      DisplayEntityManipulator.UndoState first = undoStates.get(undoStates.size() - 1);
      if (!first.position.equals(position)) {
         if (undoStates.size() >= 2) {
            DisplayEntityManipulator.UndoState second = undoStates.get(undoStates.size() - 2);
            boolean merge = false;
            if (first.position.x == position.x && first.position.y == position.y && second.position.x == position.x && second.position.y == position.y) {
               merge = Math.abs(first.position.z - position.z) < 1.0 && Math.abs(second.position.z - position.z) < 1.0;
            } else if (first.position.x == position.x && first.position.z == position.z && second.position.x == position.x && second.position.z == position.z) {
               merge = Math.abs(first.position.y - position.y) < 1.0 && Math.abs(second.position.y - position.y) < 1.0;
            } else if (first.position.y == position.y && first.position.z == position.z && second.position.y == position.y && second.position.z == position.z) {
               merge = Math.abs(first.position.x - position.x) < 1.0 && Math.abs(second.position.x - position.x) < 1.0;
            }

            if (merge) {
               undoStates.set(
                  undoStates.size() - 1,
                  new DisplayEntityManipulator.UndoState(position, first.transformations, first.interactionWidth, first.interactionHeight)
               );
               redoStates.clear();
               return;
            }
         }

         undoStates.add(new DisplayEntityManipulator.UndoState(position, first.transformations, first.interactionWidth, first.interactionHeight));
         if (undoStates.size() > 256) {
            undoStates.remove(0);
         }

         redoStates.clear();
      }
   }

   public static void disableActive() {
      if (activeEntity != null) {
         Gizmo gizmo = gizmos.get(activeEntity);
         if (gizmo != null) {
            gizmo.enableAxes = false;
         }

         activeEntity = null;
         movementDirty = false;
         undoStates.clear();
         redoStates.clear();
      }
   }

   public static void deleteActive() {
      if (activeEntity instanceof Display display) {
         DisplayEntityHelper.killRecursive(display);
      } else if (activeEntity != null) {
         new AxiomServerboundDeleteEntity(List.of(activeEntity.getUUID())).send();
      }

      disableActive();
   }

   public static void onEntityRemoved(Entity entity) {
      if (activeEntity == entity) {
         disableActive();
      }
   }

   @Nullable
   public static Display getActiveDisplayEntity() {
      return activeEntity instanceof Display display ? display : null;
   }

   public static boolean hasActiveGizmo() {
      return activeEntity != null;
   }

   public static boolean hasGrabbedNotCenterGizmo() {
      if (activeEntity == null) {
         return false;
      } else {
         Gizmo gizmo = gizmos.get(activeEntity);
         return gizmo == null ? false : gizmo.isGrabbed() && !gizmo.isCenterGrabbed();
      }
   }

   public static boolean hoveredGizmoPart() {
      return hoveredGizmoPart;
   }

   public static boolean canUndo() {
      return undoStates.size() >= 2;
   }

   public static boolean canRedo() {
      return redoStates.size() >= 1;
   }

   private static Matrix4f getPreviewMultiplyTransformMatrix() {
      if (previewMultiplyTransformMatrix != null) {
         return previewMultiplyTransformMatrix;
      } else if (activeEntity instanceof Display activeDisplay) {
         RenderState var9 = activeDisplay.renderState();
         Gizmo gizmo = gizmos.get(activeEntity);
         if (gizmo != null && gizmo.isGrabbed()) {
            Matrix4f oldMatrix = new Matrix4f(((Transformation)var9.transformation().get(1.0F)).getMatrix());
            Matrix4f matrix4f = new Matrix4f(oldMatrix);
            Quaternionf orientation = calculateOrientation(var9, activeDisplay, new Quaternionf());
            Quaternionf invertedOrientation = new Quaternionf(orientation).invert();
            Gizmo.GizmoRotation peekedRotation = gizmo.peekGizmoRotation();
            if (peekedRotation != null) {
               matrix4f.rotateLocal(orientation);
               applyGizmoRotation(matrix4f, peekedRotation);
               matrix4f.rotateLocal(invertedOrientation);
            }

            Gizmo.GizmoScale peekedScale = gizmo.peekScale();
            if (peekedScale != null) {
               matrix4f.rotateLocal(orientation);
               applyGizmoScale(matrix4f, peekedScale);
               matrix4f.rotateLocal(invertedOrientation);
            }

            overrideMatrixTicks = 10;
            overrideMatrix = new Matrix4f(matrix4f);
            previewMultiplyTransformMatrix = matrix4f.mul(oldMatrix.invert());
            return previewMultiplyTransformMatrix;
         } else if (overrideMatrixTicks > 0) {
            Matrix4f oldMatrixx = new Matrix4f(((Transformation)var9.transformation().get(1.0F)).getMatrix());
            Matrix4f newMatrix = overrideMatrix == null
               ? new Matrix4f(((Transformation)var9.transformation().get(1.0F)).getMatrix())
               : new Matrix4f(overrideMatrix);
            previewMultiplyTransformMatrix = newMatrix.mul(oldMatrixx.invert());
            return previewMultiplyTransformMatrix;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static RenderState getModifiedRenderState(Display display) {
      if (activeEntity instanceof Display activeDisplay) {
         if (activeDisplay.renderState() == null) {
            return display.renderState();
         } else {
            Entity root = display.getRootVehicle();
            if (root != activeEntity) {
               return display.renderState();
            } else {
               RenderState oldRenderState = display.renderState();
               if (oldRenderState == null) {
                  return null;
               } else {
                  Matrix4f transformBy = getPreviewMultiplyTransformMatrix();
                  if (transformBy == null) {
                     return oldRenderState;
                  } else {
                     Matrix4f matrix4f = new Matrix4f(((Transformation)oldRenderState.transformation().get(1.0F)).getMatrix());
                     Quaternionf parentOrientation = calculateOrientation(activeDisplay.renderState(), activeDisplay, new Quaternionf());
                     Quaternionf parentInvertedOrientation = new Quaternionf(parentOrientation).invert();
                     Quaternionf orientation = calculateOrientation(oldRenderState, display, new Quaternionf());
                     Quaternionf invertedOrientation = new Quaternionf(orientation).invert();
                     matrix4f.rotateLocal(orientation);
                     matrix4f.rotateLocal(parentInvertedOrientation);
                     matrix4f.mulLocal(transformBy);
                     matrix4f.rotateLocal(parentOrientation);
                     matrix4f.rotateLocal(invertedOrientation);
                     Gizmo gizmo = gizmos.get(activeEntity);
                     if (gizmo != null) {
                        float partial = VersionUtilsClient.getPartialTick(Minecraft.getInstance());
                        double x = Mth.lerp(partial, display.xOld, display.getX());
                        double y = Mth.lerp(partial, display.yOld, display.getY());
                        double z = Mth.lerp(partial, display.zOld, display.getZ());
                        Vec3 interpPosition = gizmo.getInterpPosition();
                        matrix4f.rotateLocal(orientation);
                        matrix4f.translateLocal((float)(interpPosition.x - x), (float)(interpPosition.y - y), (float)(interpPosition.z - z));
                        matrix4f.rotateLocal(invertedOrientation);
                     }

                     GenericInterpolator<Transformation> transformation = GenericInterpolator.constant(VersionUtils.helperTransformationNew(matrix4f));
                     return new RenderState(
                        transformation,
                        oldRenderState.billboardConstraints(),
                        oldRenderState.brightnessOverride(),
                        oldRenderState.shadowRadius(),
                        oldRenderState.shadowStrength(),
                        oldRenderState.glowColorOverride()
                     );
                  }
               }
            }
         }
      } else {
         return display.renderState();
      }
   }

   private static Quaternionf calculateOrientation(RenderState renderState, Display display, Quaternionf quaternionf) {
      return renderState == null ? quaternionf : calculateOrientation(renderState.billboardConstraints(), display, quaternionf);
   }

   private static Quaternionf calculateOrientation(BillboardConstraints billboardConstraints, Display display, Quaternionf quaternionf) {
      if (billboardConstraints == null) {
         return quaternionf;
      } else {
         Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;

         return switch (billboardConstraints) {
            case FIXED -> quaternionf.rotationYXZ((float) (-Math.PI / 180.0) * display.getYRot(), (float) (Math.PI / 180.0) * display.getXRot(), 0.0F);
            case HORIZONTAL -> quaternionf.rotationYXZ((float) (-Math.PI / 180.0) * display.getYRot(), (float) (Math.PI / 180.0) * -camera.getXRot(), 0.0F);
            case VERTICAL -> quaternionf.rotationYXZ(
               (float) (-Math.PI / 180.0) * (camera.getYRot() - 180.0F), (float) (Math.PI / 180.0) * display.getXRot(), 0.0F
            );
            case CENTER -> quaternionf.rotationYXZ(
               (float) (-Math.PI / 180.0) * (camera.getYRot() - 180.0F), (float) (Math.PI / 180.0) * -camera.getXRot(), 0.0F
            );
            default -> throw new IncompatibleClassChangeError();
         };
      }
   }

   private static void applyGizmoRotationToEntityRecursive(Display display, Gizmo.GizmoRotation gizmoRotation, Map<UUID, Matrix4fc> transforms) {
      if (!display.isRemoved()) {
         Matrix4f originalMatrix = getTransformationMatrix(display);
         Matrix4f newMatrix = new Matrix4f(originalMatrix);
         int billboardId = (Byte)display.getEntityData().get(Display.DATA_BILLBOARD_RENDER_CONSTRAINTS_ID);
         BillboardConstraints billboardConstraints = (BillboardConstraints)BillboardConstraints.BY_ID.apply(billboardId);
         Quaternionf orientation = calculateOrientation(billboardConstraints, display, new Quaternionf());
         Quaternionf invertedOrientation = new Quaternionf(orientation).invert();
         newMatrix.rotateLocal(orientation);
         applyGizmoRotation(newMatrix, gizmoRotation);
         newMatrix.rotateLocal(invertedOrientation);
         transforms.put(display.getUUID(), new Matrix4f(newMatrix));
         Transformation transformation = VersionUtils.helperTransformationNew(newMatrix);
         transformation.getLeftRotation();
         Transformation.CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().ifPresent(createTagConsumer(display.getUUID()));
         Matrix4f childTransform = newMatrix.mul(originalMatrix.invert());

         for (Entity passenger : display.getPassengers()) {
            if (passenger instanceof Display display1) {
               transformSelfAndChildren(display1, childTransform, orientation, invertedOrientation, transforms);
            }
         }
      }
   }

   private static Consumer<Tag> createTagConsumer(UUID uuid) {
      return tag -> {
         CompoundTag root = new CompoundTag();
         root.put("transformation", tag);
         DisplayEntityManipulator.PendingUpdate pendingUpdate = pendingUpdates.computeIfAbsent(uuid, k -> new DisplayEntityManipulator.PendingUpdate());
         pendingUpdate.mergeNbt = pendingUpdate.mergeNbt.merge(root);
      };
   }

   private static void applyGizmoScaleToEntityRecursive(Display display, Gizmo.GizmoScale gizmoScale, Map<UUID, Matrix4fc> transforms) {
      if (!display.isRemoved()) {
         Matrix4f originalMatrix = getTransformationMatrix(display);
         Matrix4f newMatrix = new Matrix4f(originalMatrix);
         int billboardId = (Byte)display.getEntityData().get(Display.DATA_BILLBOARD_RENDER_CONSTRAINTS_ID);
         BillboardConstraints billboardConstraints = (BillboardConstraints)BillboardConstraints.BY_ID.apply(billboardId);
         Quaternionf orientation = calculateOrientation(billboardConstraints, display, new Quaternionf());
         Quaternionf invertedOrientation = new Quaternionf(orientation).invert();
         newMatrix.rotateLocal(orientation);
         applyGizmoScale(newMatrix, gizmoScale);
         newMatrix.rotateLocal(invertedOrientation);
         transforms.put(display.getUUID(), new Matrix4f(newMatrix));
         Transformation transformation = VersionUtils.helperTransformationNew(newMatrix);
         transformation.getLeftRotation();
         Transformation.CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().ifPresent(createTagConsumer(display.getUUID()));
         Matrix4f childTransform = newMatrix.mul(originalMatrix.invert());

         for (Entity passenger : display.getPassengers()) {
            if (passenger instanceof Display display1) {
               transformSelfAndChildren(display1, childTransform, orientation, invertedOrientation, transforms);
            }
         }
      }
   }

   private static void transformSelfAndChildren(
      Display display, Matrix4f transform, Quaternionf parentOrientation, Quaternionf parentInvertedOrientation, Map<UUID, Matrix4fc> transforms
   ) {
      for (Entity passenger : display.getPassengers()) {
         if (passenger instanceof Display display1) {
            transformSelfAndChildren(display1, transform, parentOrientation, parentInvertedOrientation, transforms);
         }
      }

      if (!display.isRemoved()) {
         Matrix4f matrix4f = getTransformationMatrix(display);
         int billboardId = (Byte)display.getEntityData().get(Display.DATA_BILLBOARD_RENDER_CONSTRAINTS_ID);
         BillboardConstraints billboardConstraints = (BillboardConstraints)BillboardConstraints.BY_ID.apply(billboardId);
         Quaternionf orientation = calculateOrientation(billboardConstraints, display, new Quaternionf());
         Quaternionf invertedOrientation = new Quaternionf(orientation).invert();
         matrix4f.rotateLocal(orientation);
         matrix4f.rotateLocal(parentInvertedOrientation);
         matrix4f.mulLocal(transform);
         matrix4f.rotateLocal(parentOrientation);
         matrix4f.rotateLocal(invertedOrientation);
         transforms.put(display.getUUID(), new Matrix4f(matrix4f));
         Transformation transformation = VersionUtils.helperTransformationNew(matrix4f);
         transformation.getLeftRotation();
         Transformation.CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().ifPresent(createTagConsumer(display.getUUID()));
      }
   }

   private static void transformSelfAndChildren(Display display, Map<UUID, Matrix4fc> transforms) {
      for (Entity passenger : display.getPassengers()) {
         if (passenger instanceof Display display1) {
            transformSelfAndChildren(display1, transforms);
         }
      }

      if (!display.isRemoved()) {
         Matrix4fc matrix4f = transforms.get(display.getUUID());
         if (matrix4f == null) {
            return;
         }

         Transformation transformation = VersionUtils.helperTransformationNew(matrix4f);
         transformation.getLeftRotation();
         Transformation.CODEC.encodeStart(NbtOps.INSTANCE, transformation).result().ifPresent(createTagConsumer(display.getUUID()));
      }
   }

   public static Matrix4f getTransformationMatrix(Display display) {
      Vector3fc translation = (Vector3fc)display.getEntityData().get(Display.DATA_TRANSLATION_ID);
      Quaternionfc left = (Quaternionfc)display.getEntityData().get(Display.DATA_LEFT_ROTATION_ID);
      Vector3fc scale = (Vector3fc)display.getEntityData().get(Display.DATA_SCALE_ID);
      Quaternionfc right = (Quaternionfc)display.getEntityData().get(Display.DATA_RIGHT_ROTATION_ID);
      Matrix4f matrix = new Matrix4f();
      matrix.translate(translation);
      matrix.rotate(left);
      matrix.scale(scale);
      matrix.rotate(right);
      return matrix;
   }

   private static void applyGizmoRotation(Matrix4f matrix4f, Gizmo.GizmoRotation gizmoRotation) {
      float f = 1.0F / matrix4f.m33();
      Triple<Quaternionf, Vector3f, Quaternionf> triple = MatrixUtil.svdDecompose(new Matrix3f(matrix4f).scale(f));
      Vector3f translation = matrix4f.getTranslation(new Vector3f()).mul(f);
      Quaternionf leftRotation = (Quaternionf)triple.getLeft();
      Vector3f scale = (Vector3f)triple.getMiddle();
      Quaternionf rightRotation = (Quaternionf)triple.getRight();
      Quaternionf totalRotation = new Quaternionf();
      totalRotation.mul(leftRotation);
      totalRotation.mul(rightRotation);
      Quaternionf oldRotation = new Quaternionf(totalRotation);
      if (gizmoMode == GizmoMode.LOCAL) {
         switch (gizmoRotation.axis()) {
            case X:
               totalRotation.rotateX(gizmoRotation.radians());
               break;
            case Y:
               totalRotation.rotateY(gizmoRotation.radians());
               break;
            case Z:
               totalRotation.rotateZ(gizmoRotation.radians());
         }
      } else {
         switch (gizmoRotation.axis()) {
            case X:
               totalRotation.rotateLocalX(gizmoRotation.radians());
               break;
            case Y:
               totalRotation.rotateLocalY(gizmoRotation.radians());
               break;
            case Z:
               totalRotation.rotateLocalZ(gizmoRotation.radians());
         }
      }

      if (!isShiftDown()) {
         float sinX = -2.0F * (totalRotation.y * totalRotation.z - totalRotation.w * totalRotation.x);
         boolean useAlternative = sinX <= -0.999 || sinX >= 0.999;
         Vector3f eulerAngles = new Vector3f();
         if (useAlternative) {
            totalRotation.getEulerAnglesXYZ(eulerAngles);
         } else {
            totalRotation.getEulerAnglesYXZ(eulerAngles);
         }

         eulerAngles.x = (float)Math.toRadians((float)Math.round(Math.toDegrees(eulerAngles.x) * 10.0) / 10.0F);
         eulerAngles.y = (float)Math.toRadians((float)Math.round(Math.toDegrees(eulerAngles.y) * 10.0) / 10.0F);
         eulerAngles.z = (float)Math.toRadians((float)Math.round(Math.toDegrees(eulerAngles.z) * 10.0) / 10.0F);
         totalRotation.identity();
         if (useAlternative) {
            totalRotation.rotateXYZ(eulerAngles.x, eulerAngles.y, eulerAngles.z);
         } else {
            totalRotation.rotateYXZ(eulerAngles.y, eulerAngles.x, eulerAngles.z);
         }
      }

      oldRotation.transformInverse(translation);
      totalRotation.transform(translation);
      Quaternionf newLeftRotation = totalRotation.div(rightRotation);
      Matrix4f newMatrix = new Matrix4f();
      newMatrix.translate(translation);
      newMatrix.rotate(newLeftRotation);
      newMatrix.scale(scale);
      newMatrix.rotate(rightRotation);
      matrix4f.set(newMatrix);
   }

   private static void applyTeleport(Entity entity, Vec3 position) {
      Gizmo gizmo = gizmos.get(entity);
      if (gizmo != null) {
         gizmo.moveToVec(position);
      }

      DisplayEntityManipulator.PendingUpdate pendingUpdate = pendingUpdates.computeIfAbsent(entity.getUUID(), k -> new DisplayEntityManipulator.PendingUpdate());
      pendingUpdate.position = position;
   }

   private static boolean isShiftDown() {
      long window = Minecraft.getInstance().getWindow().getWindow();
      return GLFW.glfwGetKey(window, 340) != 0 || GLFW.glfwGetKey(window, 344) != 0;
   }

   private static void applyGizmoScale(Matrix4f matrix4f, Gizmo.GizmoScale gizmoScale) {
      boolean doHinting = !isShiftDown();
      if (gizmoScale.scaleAll()) {
         if (gizmoMode == GizmoMode.LOCAL) {
            float axis = switch (gizmoScale.axis()) {
               case X -> (float)Math.sqrt(matrix4f.m00() * matrix4f.m00() + matrix4f.m01() * matrix4f.m01() + matrix4f.m02() * matrix4f.m02());
               case Y -> (float)Math.sqrt(matrix4f.m10() * matrix4f.m10() + matrix4f.m11() * matrix4f.m11() + matrix4f.m12() * matrix4f.m12());
               case Z -> (float)Math.sqrt(matrix4f.m20() * matrix4f.m20() + matrix4f.m21() * matrix4f.m21() + matrix4f.m22() * matrix4f.m22());
               default -> throw new FaultyImplementationError();
            };
            if (axis == 0.0F) {
               axis = 1.0F;
            }

            float scale;
            if (axis < 1.0F && gizmoScale.scale() > 1.0F) {
               scale = (gizmoScale.scale() - 1.0F) / axis + 1.0F;
            } else {
               scale = gizmoScale.scale();
            }

            if (doHinting) {
               scale = Math.round(axis * scale * 16.0F) / 16.0F / axis;
            }

            if (scale < 1.0E-4 / axis) {
               scale = 1.0E-4F / axis;
            }

            matrix4f.scale(scale, scale, scale);
         } else {
            float var9 = switch (gizmoScale.axis()) {
               case X -> (float)Math.sqrt(matrix4f.m00() * matrix4f.m00() + matrix4f.m10() * matrix4f.m10() + matrix4f.m20() * matrix4f.m20());
               case Y -> (float)Math.sqrt(matrix4f.m01() * matrix4f.m01() + matrix4f.m11() * matrix4f.m11() + matrix4f.m21() * matrix4f.m21());
               case Z -> (float)Math.sqrt(matrix4f.m02() * matrix4f.m02() + matrix4f.m12() * matrix4f.m12() + matrix4f.m22() * matrix4f.m22());
               default -> throw new FaultyImplementationError();
            };
            if (var9 == 0.0F) {
               var9 = 1.0F;
            }

            float scalex;
            if (var9 < 1.0F && gizmoScale.scale() > 1.0F) {
               scalex = (gizmoScale.scale() - 1.0F) / var9 + 1.0F;
            } else {
               scalex = gizmoScale.scale();
            }

            if (doHinting) {
               scalex = Math.round(var9 * scalex * 16.0F) / 16.0F / var9;
            }

            if (scalex < 1.0E-4 / var9) {
               scalex = 1.0E-4F / var9;
            }

            matrix4f.scaleLocal(scalex, scalex, scalex);
         }
      } else {
         Axis axisx = gizmoScale.axis();
         if (gizmoMode == GizmoMode.LOCAL) {
            float a = matrix4f.get(axisx.ordinal(), 0);
            float b = matrix4f.get(axisx.ordinal(), 1);
            float c = matrix4f.get(axisx.ordinal(), 2);
            float oldScale = (float)Math.sqrt(a * a + b * b + c * c);
            if (oldScale == 0.0F) {
               oldScale = 1.0F;
            }

            float scalexx;
            if (oldScale < 1.0F && gizmoScale.scale() > 1.0F) {
               scalexx = (gizmoScale.scale() - 1.0F) / oldScale + 1.0F;
            } else {
               scalexx = gizmoScale.scale();
            }

            if (doHinting) {
               scalexx = Math.round(oldScale * scalexx * 16.0F) / 16.0F / oldScale;
            }

            if (scalexx < 1.0E-4 / oldScale) {
               scalexx = 1.0E-4F / oldScale;
            }

            matrix4f.scale((float)axisx.choose(scalexx, 1.0, 1.0), (float)axisx.choose(1.0, scalexx, 1.0), (float)axisx.choose(1.0, 1.0, scalexx));
         } else {
            float ax = matrix4f.get(0, axisx.ordinal());
            float bx = matrix4f.get(1, axisx.ordinal());
            float cx = matrix4f.get(2, axisx.ordinal());
            float oldScalex = (float)Math.sqrt(ax * ax + bx * bx + cx * cx);
            if (oldScalex == 0.0F) {
               oldScalex = 1.0F;
            }

            float scalexxx;
            if (oldScalex < 1.0F && gizmoScale.scale() > 1.0F) {
               scalexxx = (gizmoScale.scale() - 1.0F) / oldScalex + 1.0F;
            } else {
               scalexxx = gizmoScale.scale();
            }

            if (doHinting) {
               scalexxx = Math.round(oldScalex * scalexxx * 16.0F) / 16.0F / oldScalex;
            }

            if (scalexxx < 1.0E-4 / oldScalex) {
               scalexxx = 1.0E-4F / oldScalex;
            }

            matrix4f.scaleLocal((float)axisx.choose(scalexxx, 1.0, 1.0), (float)axisx.choose(1.0, scalexxx, 1.0), (float)axisx.choose(1.0, 1.0, scalexxx));
         }
      }
   }

   public static List<String> getKeyHints() {
      List<String> keyHints = new ArrayList<>();
      keyHints.add("RMB - Edit/Move");
      keyHints.add("Scroll - Nudge");
      Gizmo gizmo = gizmos.get(activeEntity);
      if (gizmo != null && gizmo.isGrabbed()) {
         if (gizmo.isScaleGrabbed()) {
            keyHints.add("Hold Ctrl - Scale All");
         } else {
            keyHints.add("Hold Ctrl - Inc Snapping");
         }

         keyHints.add("Hold Shift - No Snapping");
      }

      keyHints.add(Keybinds.COPY_INGAME.shortKeyIdentifier() + " - Copy");
      keyHints.add("Delete - Remove");
      boolean canUndo = canUndo();
      boolean canRedo = canRedo();
      if (canUndo && canRedo) {
         keyHints.add("Ctrl+Z/Y - Undo/Redo");
      } else if (canUndo) {
         keyHints.add("Ctrl+Z - Undo");
      } else if (canRedo) {
         keyHints.add("Ctrl+Y - Redo");
      }

      return keyHints;
   }

   public static UserAction.ActionResult callAction(UserAction userAction, Object object) {
      if (EditorUI.isActive() || !AxiomClient.isAxiomActive()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else if (userAction == UserAction.PASTE) {
         String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
         if (clipboard != null && clipboard.startsWith("/summon")) {
            StringReader stringReader = new StringReader(clipboard.substring("/summon".length()).trim());
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            LocalPlayer player = Minecraft.getInstance().player;
            if (connection != null && player != null) {
               CommandBuildContext buildContext = CommandBuildContext.simple(connection.registryAccess(), connection.enabledFeatures());

               try {
                  stringReader.skipWhitespace();
                  ResourceArgument<EntityType<?>> resourceArgument = ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE);
                  Reference<EntityType<?>> entity = resourceArgument.parse(stringReader);
                  stringReader.skipWhitespace();
                  Vec3 position;
                  if (stringReader.canRead()) {
                     Coordinates coordinates = Vec3Argument.vec3().parse(stringReader);
                     int permissionSet = 2;
                     CommandSourceStack commandSourceStack = new CommandSourceStack(
                        null, player.position(), player.getRotationVector(), null, permissionSet, null, CommonComponents.EMPTY, null, player
                     );
                     position = coordinates.getPosition(commandSourceStack);
                  } else {
                     position = player.position();
                  }

                  stringReader.skipWhitespace();
                  CompoundTag compoundTag;
                  if (stringReader.canRead()) {
                     compoundTag = CompoundTagArgument.compoundTag().parse(stringReader);
                  } else {
                     compoundTag = new CompoundTag();
                  }

                  compoundTag.putString("id", entity.key().location().toString());
                  new AxiomServerboundSpawnEntity(
                        List.of(new AxiomServerboundSpawnEntity.SpawnEntry(UUID.randomUUID(), position, 0.0F, 0.0F, null, compoundTag))
                     )
                     .send();
                  ChatUtils.info("Pasted entity");
                  disableActive();
                  return UserAction.ActionResult.USED_STOP;
               } catch (Exception var13) {
                  var13.printStackTrace();
                  return UserAction.ActionResult.NOT_HANDLED;
               }
            } else {
               return UserAction.ActionResult.NOT_HANDLED;
            }
         } else {
            return UserAction.ActionResult.NOT_HANDLED;
         }
      } else if (!Axiom.configuration.entityManipulation.showDisplayEntities) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         switch (userAction) {
            case COPY:
               if (tryCopyToClipboard()) {
                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case UNDO:
               if (activeEntity != null) {
                  Gizmo gizmox = gizmos.get(activeEntity);
                  if (gizmox != null && gizmox.isGrabbed()) {
                     return UserAction.ActionResult.USED_STOP;
                  }

                  if (undoStates.size() >= 2) {
                     DisplayEntityManipulator.UndoState current = undoStates.remove(undoStates.size() - 1);
                     redoStates.add(current);
                     DisplayEntityManipulator.UndoState head = undoStates.get(undoStates.size() - 1);
                     applyUndoStateToActiveEntity(head, current);
                     overrideMatrixTicks = 10;
                     overrideMatrix = null;
                  }

                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case REDO:
               if (activeEntity != null) {
                  Gizmo gizmo = gizmos.get(activeEntity);
                  if (gizmo != null && gizmo.isGrabbed()) {
                     return UserAction.ActionResult.USED_STOP;
                  }

                  if (redoStates.size() >= 1) {
                     DisplayEntityManipulator.UndoState current = undoStates.get(undoStates.size() - 1);
                     DisplayEntityManipulator.UndoState head = redoStates.remove(redoStates.size() - 1);
                     undoStates.add(head);
                     applyUndoStateToActiveEntity(head, current);
                     overrideMatrixTicks = 10;
                     overrideMatrix = null;
                  }

                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case SCROLL:
               if (activeEntity != null) {
                  UserAction.ScrollAmount amount = (UserAction.ScrollAmount)object;
                  Gizmo gizmo = gizmos.get(activeEntity);
                  Vec3 look = Tool.getLookDirection();
                  if (gizmo != null && look != null) {
                     boolean isCtrlDown = InputHelper.isCtrlOrCmdDownRaw();
                     gizmo.handleScroll(amount.scrollX(), amount.scrollY(), isCtrlDown, look);
                     applyTeleport(activeEntity, gizmo.getTargetVec());
                     movementDirty = true;
                     String paddedPositionX = String.format("%.4f", gizmo.getTargetVec().x);
                     String paddedPositionY = String.format("%.4f", gizmo.getTargetVec().y);
                     String paddedPositionZ = String.format("%.4f", gizmo.getTargetVec().z);
                     ScreenRenderHook.setOverlayText(
                        Component.literal(AxiomI18n.get("axiom.hardcoded.position_prefix"))
                           .withStyle(ChatFormatting.YELLOW)
                           .append(Component.literal(paddedPositionX + " ").withStyle(ChatFormatting.RED))
                           .append(Component.literal(paddedPositionY + " ").withStyle(ChatFormatting.GREEN))
                           .append(Component.literal(paddedPositionZ).withStyle(ChatFormatting.AQUA))
                     );
                     overrideMatrixTicks = 10;
                     overrideMatrix = null;
                  }

                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case LEFT_MOUSE:
               if (Axiom.configuration.entityManipulation.swapLeftRightClick) {
                  return handleRightClick();
               }

               return handleLeftClick();
            case RIGHT_MOUSE:
               if (Axiom.configuration.entityManipulation.swapLeftRightClick) {
                  return handleLeftClick();
               }

               return handleRightClick();
            case MIDDLE_MOUSE:
               return handleMiddleClick();
            default:
               return UserAction.ActionResult.NOT_HANDLED;
         }
      }
   }

   private static void applyUndoStateToActiveEntity(DisplayEntityManipulator.UndoState head, DisplayEntityManipulator.UndoState current) {
      applyTeleport(activeEntity, head.position);
      if (activeEntity instanceof Display activeDisplay) {
         if (!Objects.equals(current.transformations, head.transformations)) {
            transformSelfAndChildren(activeDisplay, head.transformations);
         }
      } else if (activeEntity instanceof Interaction interaction) {
         CompoundTag root = new CompoundTag();
         if (head.interactionWidth != null && !Objects.equals(current.interactionWidth, head.interactionWidth)) {
            root.putFloat("width", head.interactionWidth);
         }

         if (head.interactionHeight != null && !Objects.equals(current.interactionHeight, head.interactionHeight)) {
            root.putFloat("height", head.interactionHeight);
         }

         if (!root.isEmpty()) {
            DisplayEntityManipulator.PendingUpdate pendingUpdate = pendingUpdates.computeIfAbsent(
               interaction.getUUID(), k -> new DisplayEntityManipulator.PendingUpdate()
            );
            pendingUpdate.mergeNbt = pendingUpdate.mergeNbt.merge(root);
         }
      }
   }

   public static boolean tryCopyToClipboard() {
      if (activeEntity != null) {
         String command = DisplayEntityHelper.getSummonCommandForEntity(activeEntity);
         Minecraft.getInstance().keyboardHandler.setClipboard(command);
         ChatUtils.info("Copied summon command to clipboard (" + command.length() + " characters)");
         ChatUtils.info("You can use Ctrl+V to paste the entity");
         return true;
      } else {
         return false;
      }
   }

   public static void tryCopyInterpolateToClipboard() {
      if (activeEntity instanceof Display activeDisplay) {
         String command = DisplayEntityHelper.getInterpolateCommandForDisplay(activeDisplay);
         Minecraft.getInstance().keyboardHandler.setClipboard(command);
         ChatUtils.info("Copied interpolate command to clipboard (" + command.length() + " characters)");
      }
   }

   @NotNull
   private static UserAction.ActionResult handleRightClick() {
      if (hoveredGizmoPart) {
         if (activeEntity != null) {
            Gizmo gizmo = gizmos.get(activeEntity);
            if (gizmo != null && gizmo.isHovered() && activeEntity instanceof Display) {
               Minecraft.getInstance().setScreen(new EditDisplayEntityScreen(activeEntity.getUUID()));
               return UserAction.ActionResult.USED_STOP;
            }
         }

         for (Entry<Entity, Gizmo> entry : gizmos.entrySet()) {
            if (entry.getValue().isHovered() && entry.getKey() instanceof Display) {
               if (activeEntity != null) {
                  Gizmo gizmo = gizmos.get(activeEntity);
                  if (gizmo != null) {
                     gizmo.enableAxes = false;
                  }

                  activeEntity = null;
                  movementDirty = false;
                  undoStates.clear();
                  redoStates.clear();
               }

               entry.getValue().enableAxes = true;
               activeEntity = entry.getKey();
               Map<UUID, Matrix4fc> transforms = new HashMap<>();
               putTransformRecursive((Display)activeEntity, transforms);
               movementDirty = false;
               undoStates.clear();
               redoStates.clear();
               undoStates.add(new DisplayEntityManipulator.UndoState(activeEntity.position(), transforms, null, null));
               Minecraft.getInstance().setScreen(new EditDisplayEntityScreen(activeEntity.getUUID()));
               return UserAction.ActionResult.USED_STOP;
            }
         }
      }

      if (activeEntity != null) {
         HitResult hitResult = Minecraft.getInstance().hitResult;
         if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == Type.BLOCK) {
            BlockPos position = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
            Vec3 center;
            if (activeEntity instanceof Display) {
               center = Vec3.atCenterOf(position);
            } else {
               center = Vec3.atBottomCenterOf(position);
            }

            applyTeleport(activeEntity, center);
         }

         return UserAction.ActionResult.USED_STOP;
      } else {
         return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   private static UserAction.ActionResult handleMiddleClick() {
      if (activeEntity != null) {
         Gizmo gizmo = gizmos.get(activeEntity);
         if (gizmo != null && gizmo.isCenterHovered()) {
            if (activeEntity instanceof Display display) {
               handleMiddleClick(display);
            }

            disableActive();
            return UserAction.ActionResult.USED_STOP;
         }
      }

      for (Entry<Entity, Gizmo> entry : gizmos.entrySet()) {
         if (entry.getValue().isCenterHovered()) {
            if (entry.getKey() instanceof Display display) {
               handleMiddleClick(display);
            }

            return UserAction.ActionResult.USED_STOP;
         }
      }

      return UserAction.ActionResult.NOT_HANDLED;
   }

   private static void handleMiddleClick(Display display) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         CompoundTag displayNbt = DisplayEntityHelper.getDisplayEntityTagWithId(display);
         ItemStack placerItemStack;
         if (display instanceof ItemDisplay itemDisplay) {
            placerItemStack = ((ItemStack)itemDisplay.getEntityData().get(ItemDisplay.DATA_ITEM_STACK_ID)).copy();
         } else if (display instanceof BlockDisplay blockDisplay) {
            BlockState blockState = (BlockState)blockDisplay.getEntityData().get(BlockDisplay.DATA_BLOCK_STATE_ID);
            CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
            if (customBlockState != null) {
               placerItemStack = customBlockState.getCustomBlock().axiom$asItemStack();
            } else {
               placerItemStack = new ItemStack(blockState.getBlock().asItem());
            }
         } else if (display instanceof TextDisplay) {
            placerItemStack = new ItemStack(Items.OAK_SIGN);
         } else {
            placerItemStack = new ItemStack(Items.ARMOR_STAND);
         }

         if (placerItemStack.isEmpty()) {
            placerItemStack = new ItemStack(Items.ARMOR_STAND);
         }

         ItemStackDataHelper.setEntityPlacer(placerItemStack, displayNbt);
         ItemStackDataHelper.setName(placerItemStack, display.getName().copy().withStyle(ChatFormatting.YELLOW));
         ItemStackDataHelper.setLore(
            placerItemStack,
            List.of(
               Component.literal(AxiomI18n.get("axiom.hardcoded.rc_place_90")).setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.GRAY)),
               Component.literal(AxiomI18n.get("axiom.hardcoded.sneak_rc_place_1"))
                  .setStyle(Style.EMPTY.withItalic(false).applyFormat(ChatFormatting.GRAY))
            )
         );
         Inventory inventory = player.getInventory();
         inventory.setPickedItem(placerItemStack);
         Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inventory.selected);
      }
   }

   @NotNull
   private static UserAction.ActionResult handleLeftClick() {
      boolean ret = false;
      if (activeEntity != null) {
         Gizmo gizmo = gizmos.get(activeEntity);
         if (gizmo != null) {
            if (gizmo.leftClick()) {
               return UserAction.ActionResult.USED_STOP;
            }

            gizmo.enableAxes = false;
            ret = true;
         }

         activeEntity = null;
         movementDirty = false;
         undoStates.clear();
         redoStates.clear();
      }

      for (Entry<Entity, Gizmo> entry : gizmos.entrySet()) {
         if (entry.getValue().leftClick()) {
            activeEntity = entry.getKey();
            Map<UUID, Matrix4fc> transforms = new HashMap<>();
            if (activeEntity instanceof Display activeDisplay) {
               putTransformRecursive(activeDisplay, transforms);
            }

            Float interactionWidth = null;
            Float interactionHeight = null;
            if (activeEntity instanceof Interaction interaction) {
               interactionWidth = interaction.getWidth();
               interactionHeight = interaction.getHeight();
            }

            movementDirty = false;
            undoStates.clear();
            redoStates.clear();
            undoStates.add(new DisplayEntityManipulator.UndoState(activeEntity.position(), transforms, interactionWidth, interactionHeight));
            return UserAction.ActionResult.USED_STOP;
         }
      }

      return ret ? UserAction.ActionResult.USED_STOP : UserAction.ActionResult.NOT_HANDLED;
   }

   public static void trySelectDisplayEntity(Display display) {
      disableActive();
      Gizmo gizmo = gizmos.get(display);
      if (gizmo != null) {
         gizmo.enableAxes = true;
         activeEntity = display;
         Map<UUID, Matrix4fc> transforms = new HashMap<>();
         putTransformRecursive((Display)activeEntity, transforms);
         movementDirty = false;
         undoStates.clear();
         redoStates.clear();
         undoStates.add(new DisplayEntityManipulator.UndoState(activeEntity.position(), transforms, null, null));
      }
   }

   private static void putTransformRecursive(Display display, Map<UUID, Matrix4fc> transforms) {
      Vector3fc vector3f = (Vector3fc)display.getEntityData().get(Display.DATA_TRANSLATION_ID);
      Quaternionfc quaternionf = (Quaternionfc)display.getEntityData().get(Display.DATA_LEFT_ROTATION_ID);
      Vector3fc vector3f2 = (Vector3fc)display.getEntityData().get(Display.DATA_SCALE_ID);
      Quaternionfc quaternionf2 = (Quaternionfc)display.getEntityData().get(Display.DATA_RIGHT_ROTATION_ID);
      Matrix4fc transform = new Transformation(new Vector3f(vector3f), new Quaternionf(quaternionf), new Vector3f(vector3f2), new Quaternionf(quaternionf2))
         .getMatrix();
      transforms.put(display.getUUID(), transform);

      for (Entity passenger : display.getPassengers()) {
         if (passenger instanceof Display displayPassenger) {
            putTransformRecursive(displayPassenger, transforms);
         }
      }
   }

   private static class PendingUpdate {
      private Vec3 position;
      private CompoundTag mergeNbt = new CompoundTag();
   }

   private record UndoState(Vec3 position, Map<UUID, Matrix4fc> transformations, Float interactionWidth, Float interactionHeight) {
   }
}
