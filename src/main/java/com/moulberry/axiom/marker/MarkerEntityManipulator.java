package com.moulberry.axiom.marker;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.packets.AxiomClientboundMarkerData;
import com.moulberry.axiom.packets.AxiomServerboundDeleteEntity;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.packets.AxiomServerboundMarkerNbtRequest;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.EffectRenderer;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.screen.EditMarkerScreen;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.InputHelper;
import com.moulberry.axiom.utils.ProjectedText;
import com.moulberry.axiom.utils.RenderHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarkerEntityManipulator {
   private static final Map<UUID, Gizmo> gizmos = new HashMap<>();
   private static final Map<UUID, MarkerData> markerDataMap = new HashMap<>();
   private static final Map<UUID, Vec3> pendingTeleports = new HashMap<>();
   private static MarkerData activeEntity = null;
   private static boolean hoveredGizmoPart = false;
   private static final List<Vec3> undoStates = new ArrayList<>();
   private static final List<Vec3> redoStates = new ArrayList<>();
   private static boolean movementDirty = false;
   private static int teleportTicks = 0;
   private static boolean copyMarkerEntity = false;

   public static void tick() {
      if (teleportTicks > 0) {
         teleportTicks--;
      }

      LocalPlayer player = Minecraft.getInstance().player;
      ClientLevel level = Minecraft.getInstance().level;
      if (player != null && level != null && AxiomClient.isAxiomActive() && Axiom.configuration.entityManipulation.showMarkerEntities) {
         if (!pendingTeleports.isEmpty()) {
            List<AxiomServerboundManipulateEntity.ManipulateEntry> entries = new ArrayList<>();

            for (Entry<UUID, Vec3> entry : pendingTeleports.entrySet()) {
               MarkerData markerData = markerDataMap.get(entry.getKey());
               CompoundTag mergeData = new CompoundTag();
               Vec3 delta = entry.getValue().subtract(markerData.position());
               if (!delta.equals(Vec3.ZERO)) {
                  CompoundTag merge = null;
                  if (!mergeData.isEmpty()) {
                     merge = new CompoundTag();
                     merge.put("data", mergeData);
                  }

                  entries.add(new AxiomServerboundManipulateEntity.ManipulateEntry(entry.getKey(), entry.getValue(), merge));
               }
            }

            new AxiomServerboundManipulateEntity(entries).send();
         }

         pendingTeleports.clear();
      } else {
         if (activeEntity != null) {
            Gizmo gizmo = gizmos.get(activeEntity.uuid());
            if (gizmo != null) {
               gizmo.enableAxes = false;
               gizmo.moveToVec(activeEntity.position());
            }

            teleportTicks = 0;
            activeEntity = null;
            movementDirty = false;
            undoStates.clear();
            redoStates.clear();
         }

         pendingTeleports.clear();
      }
   }

   public static void render(AxiomWorldRenderContext rc) {
      hoveredGizmoPart = false;
      if (Axiom.configuration.entityManipulation.showMarkerEntities) {
         boolean inEditor = EditorUI.isActive();
         Vec3 lookDirection = inEditor ? null : Tool.getLookDirection();
         boolean isLeftDown = !inEditor && (Axiom.configuration.entityManipulation.swapLeftRightClick ? Tool.isMouseDown(1) : Tool.isMouseDown(0));
         boolean isCtrlDown = !inEditor && InputHelper.isCtrlOrCmdDownRaw();
         int rangeSq = Axiom.configuration.entityManipulation.markerEntityRange * Axiom.configuration.entityManipulation.markerEntityRange;

         for (Entry<UUID, Gizmo> entry : gizmos.entrySet()) {
            UUID uuid = entry.getKey();
            Gizmo gizmo = entry.getValue();
            Vec3 before = gizmo.getTargetVec();
            boolean isActiveEntity = activeEntity != null && uuid.equals(activeEntity.uuid());
            if (!(before.distanceToSqr(rc.position()) > rangeSq) || isActiveEntity) {
               if (gizmo.enableAxes) {
                  gizmo.setAxisDirections(rc.x() > gizmo.getTargetVec().x, rc.y() > gizmo.getTargetVec().y, rc.z() > gizmo.getTargetVec().z);
               }

               gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, true);
               Vec3 position = gizmo.getTargetVec();
               if (!Minecraft.getInstance().options.hideGui) {
                  if (!inEditor) {
                     gizmo.render(rc, isCtrlDown);
                  }

                  MarkerData markerData = markerDataMap.get(uuid);
                  if (!gizmo.isGrabbed()) {
                     PoseStack poseStack = rc.poseStack();
                     poseStack.pushPose();
                     poseStack.translate(position.x - rc.x(), position.y - rc.y(), position.z - rc.z());
                     ProjectedText.setupProjectedText();
                     ProjectedText.renderProjectedText(
                        markerData.name() == null ? "Unnamed Marker" : markerData.name(), poseStack, rc.projection(), 0.0F, 0.15F, 0.0F
                     );
                     ProjectedText.finishProjectedText();
                     poseStack.popPose();
                  }

                  if ((isActiveEntity || activeEntity == null) && markerData.minRegion() != null && markerData.maxRegion() != null) {
                     Vec3 minRegion = markerData.minRegion();
                     Vec3 maxRegion = markerData.maxRegion();
                     boolean renderCustomLine = markerData.lineArgb() != 0;
                     boolean renderCustomFace = markerData.faceArgb() != 0;
                     if (!renderCustomLine) {
                        EffectRenderer.renderBoundingBox(rc, minRegion, maxRegion, 4);
                     }

                     if (renderCustomLine || renderCustomFace) {
                        double minX = Math.min(minRegion.x(), maxRegion.x()) - 1.0E-4;
                        double minY = Math.min(minRegion.y(), maxRegion.y()) - 1.0E-4;
                        double minZ = Math.min(minRegion.z(), maxRegion.z()) - 1.0E-4;
                        float sizeX = (float)(Math.abs(minRegion.x() - maxRegion.x()) + 2.0E-4);
                        float sizeY = (float)(Math.abs(minRegion.y() - maxRegion.y()) + 2.0E-4);
                        float sizeZ = (float)(Math.abs(minRegion.z() - maxRegion.z()) + 2.0E-4);
                        float thickness = markerData.lineThickness() < 1.0F ? 2.0F : markerData.lineThickness();
                        float actualLineWidth = RenderHelper.baseLineWidth * thickness / 2.0F;
                        AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
                        AxiomRenderer.setLineWidthLegacy(actualLineWidth);
                        PoseStack poseStack = rc.poseStack();
                        poseStack.pushPose();
                        poseStack.translate(minX - rc.x(), minY - rc.y(), minZ - rc.z());
                        RenderHelper.tryApplyModelViewMatrix();
                        VertexConsumerProvider provider = VertexConsumerProvider.shared();
                        if (renderCustomLine) {
                           int argb = markerData.lineArgb();
                           float alpha = (argb >> 24 & 0xFF) / 255.0F;
                           float red = (argb >> 16 & 0xFF) / 255.0F;
                           float green = (argb >> 8 & 0xFF) / 255.0F;
                           float blue = (argb & 0xFF) / 255.0F;
                           if (alpha < 0.1) {
                              alpha = 1.0F;
                           }

                           BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                           Shapes.lineBox(
                              poseStack, bufferBuilder, 0.0F, 0.0F, 0.0F, sizeX, sizeY, sizeZ, red, green, blue, alpha, red, green, blue, actualLineWidth
                           );
                           AxiomRenderPipelines.LINES_WITH_CUSTOM_WIDTH.render(provider.build());
                        }

                        if (renderCustomFace) {
                           int faceArgb = markerData.faceArgb();
                           if ((faceArgb >> 24 & 0xFF) == 0) {
                              faceArgb |= 1073741824;
                           }

                           Shapes.shadedBox(provider, poseStack.last().pose(), sizeX, sizeY, sizeZ, faceArgb);
                           AxiomRenderPipelines.POSITION_COLOR.render(provider.build());
                        }

                        poseStack.popPose();
                     }
                  }
               }

               if (gizmo.isHovered()) {
                  hoveredGizmoPart = true;
               }

               if (isActiveEntity) {
                  if (gizmo.isGrabbed()) {
                     movementDirty = true;
                  } else {
                     if (movementDirty) {
                        movementDirty = false;
                        teleportTicks = 10;
                        applyTeleport(uuid, position);
                        addTranslationUndoState(position);
                     }

                     if (teleportTicks == 0) {
                        gizmo.moveToVec(activeEntity.position());
                     }
                  }

                  if (!position.equals(before)) {
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
               }
            }
         }
      }
   }

   public static void clear() {
      teleportTicks = 0;
      activeEntity = null;
      movementDirty = false;
      gizmos.clear();
      markerDataMap.clear();
      undoStates.clear();
      redoStates.clear();
      pendingTeleports.clear();
   }

   public static void update(AxiomClientboundMarkerData markerData) {
      for (MarkerData entry : markerData.entries) {
         if (activeEntity != null && entry.uuid().equals(activeEntity.uuid())) {
            activeEntity = entry;
         }

         markerDataMap.put(entry.uuid(), entry);
         Gizmo gizmo = gizmos.get(entry.uuid());
         if (gizmo == null) {
            gizmo = new Gizmo(entry.position());
            gizmo.enableAxes = false;
            gizmo.minVisualScale = 2.0F;
            gizmo.translationSnapping = 16;
            gizmo.centerColour = 16576167;
            gizmos.put(entry.uuid(), gizmo);
         } else if (entry != activeEntity || teleportTicks == 0) {
            gizmo.moveToVec(entry.position());
         }
      }

      if (activeEntity != null && markerData.removedMarkers.contains(activeEntity.uuid())) {
         disableActive();
      }

      gizmos.keySet().removeAll(markerData.removedMarkers);
      markerDataMap.keySet().removeAll(markerData.removedMarkers);
   }

   private static void addTranslationUndoState(Vec3 position) {
      Vec3 first = undoStates.get(undoStates.size() - 1);
      if (!first.equals(position)) {
         if (undoStates.size() >= 2) {
            Vec3 second = undoStates.get(undoStates.size() - 2);
            boolean merge = false;
            if (first.x == position.x && first.y == position.y && second.x == position.x && second.y == position.y) {
               merge = Math.abs(first.z - position.z) < 1.0 && Math.abs(second.z - position.z) < 1.0;
            } else if (first.x == position.x && first.z == position.z && second.x == position.x && second.z == position.z) {
               merge = Math.abs(first.y - position.y) < 1.0 && Math.abs(second.y - position.y) < 1.0;
            } else if (first.y == position.y && first.z == position.z && second.y == position.y && second.z == position.z) {
               merge = Math.abs(first.x - position.x) < 1.0 && Math.abs(second.x - position.x) < 1.0;
            }

            if (merge) {
               undoStates.set(undoStates.size() - 1, position);
               redoStates.clear();
               return;
            }
         }

         undoStates.add(position);
         if (undoStates.size() > 256) {
            undoStates.remove(0);
         }

         redoStates.clear();
      }
   }

   public static void disableActive() {
      if (activeEntity != null) {
         Gizmo gizmo = gizmos.get(activeEntity.uuid());
         if (gizmo != null) {
            gizmo.enableAxes = false;
            gizmo.moveToVec(activeEntity.position());
         }

         teleportTicks = 0;
         activeEntity = null;
         movementDirty = false;
         undoStates.clear();
         redoStates.clear();
      }
   }

   public static void deleteActive() {
      if (activeEntity != null) {
         new AxiomServerboundDeleteEntity(List.of(activeEntity.uuid())).send();
      }

      disableActive();
   }

   @Nullable
   public static MarkerData getActiveMarkerData() {
      return activeEntity;
   }

   public static boolean hasActiveGizmo() {
      return activeEntity != null;
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

   private static void applyTeleport(UUID uuid, Vec3 position) {
      Gizmo gizmo = gizmos.get(uuid);
      if (gizmo != null) {
         gizmo.moveToVec(position);
      }

      pendingTeleports.put(uuid, position);
   }

   public static void spawnNewMarkerRegion(BlockPos first, BlockPos second) {
      double minX = Math.min(first.getX(), second.getX());
      double minY = Math.min(first.getY(), second.getY());
      double minZ = Math.min(first.getZ(), second.getZ());
      double maxX = Math.max(first.getX(), second.getX()) + 1;
      double maxY = Math.max(first.getY(), second.getY()) + 1;
      double maxZ = Math.max(first.getZ(), second.getZ()) + 1;
      Vec3 center = new Vec3((minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);
      ListTag min = new ListTag();
      min.add(StringTag.valueOf("~" + (minX - center.x)));
      min.add(StringTag.valueOf("~" + (minY - center.y)));
      min.add(StringTag.valueOf("~" + (minZ - center.z)));
      ListTag max = new ListTag();
      max.add(StringTag.valueOf("~" + (maxX - center.x)));
      max.add(StringTag.valueOf("~" + (maxY - center.y)));
      max.add(StringTag.valueOf("~" + (maxZ - center.z)));
      CompoundTag data = new CompoundTag();
      data.put("min", min);
      data.put("max", max);
      CompoundTag tag = new CompoundTag();
      tag.putString("id", "minecraft:marker");
      tag.put("data", data);
      new AxiomServerboundSpawnEntity(List.of(new AxiomServerboundSpawnEntity.SpawnEntry(UUID.randomUUID(), center, 0.0F, 0.0F, null, tag))).send();
   }

   public static List<String> getKeyHints() {
      List<String> keyHints = new ArrayList<>();
      keyHints.add("RMB - Edit/Move");
      keyHints.add("Scroll - Nudge");
      Gizmo gizmo = gizmos.get(activeEntity.uuid());
      if (gizmo != null && gizmo.isGrabbed()) {
         keyHints.add("Hold Ctrl - Inc Snapping");
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
      if (!EditorUI.isActive() && AxiomClient.isAxiomActive() && Axiom.configuration.entityManipulation.showMarkerEntities) {
         switch (userAction) {
            case COPY:
               if (activeEntity != null) {
                  if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.MARKER_NBT_REQUEST)) {
                     ChatUtils.error("Server doesn't support requesting marker nbt");
                  } else {
                     copyMarkerEntity = true;
                     new AxiomServerboundMarkerNbtRequest(activeEntity.uuid(), AxiomServerboundMarkerNbtRequest.REASON_COPY).send();
                  }

                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case UNDO:
               if (activeEntity != null) {
                  Gizmo gizmox = gizmos.get(activeEntity.uuid());
                  if (gizmox != null && gizmox.isGrabbed()) {
                     return UserAction.ActionResult.USED_STOP;
                  }

                  if (undoStates.size() >= 2) {
                     Vec3 current = undoStates.remove(undoStates.size() - 1);
                     redoStates.add(current);
                     Vec3 head = undoStates.get(undoStates.size() - 1);
                     applyTeleport(activeEntity.uuid(), head);
                     teleportTicks = 10;
                  }

                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case REDO:
               if (activeEntity != null) {
                  Gizmo gizmo = gizmos.get(activeEntity.uuid());
                  if (gizmo != null && gizmo.isGrabbed()) {
                     return UserAction.ActionResult.USED_STOP;
                  }

                  if (redoStates.size() >= 1) {
                     Vec3 head = redoStates.remove(redoStates.size() - 1);
                     undoStates.add(head);
                     applyTeleport(activeEntity.uuid(), head);
                     teleportTicks = 10;
                  }

                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case SCROLL:
               if (activeEntity != null) {
                  UserAction.ScrollAmount amount = (UserAction.ScrollAmount)object;
                  Gizmo gizmo = gizmos.get(activeEntity.uuid());
                  Vec3 look = Tool.getLookDirection();
                  if (gizmo != null && look != null) {
                     boolean isCtrlDown = InputHelper.isCtrlOrCmdDownRaw();
                     gizmo.handleScroll(amount.scrollX(), amount.scrollY(), isCtrlDown, look);
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
            default:
               return UserAction.ActionResult.NOT_HANDLED;
         }
      } else {
         return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @NotNull
   private static UserAction.ActionResult handleRightClick() {
      if (hoveredGizmoPart) {
         if (activeEntity != null) {
            Gizmo gizmo = gizmos.get(activeEntity.uuid());
            if (gizmo != null && gizmo.isHovered()) {
               if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.MARKER_NBT_REQUEST)) {
                  ChatUtils.error("Server doesn't support requesting marker nbt");
               } else {
                  copyMarkerEntity = false;
                  new AxiomServerboundMarkerNbtRequest(activeEntity.uuid(), AxiomServerboundMarkerNbtRequest.REASON_RIGHT_CLICK).send();
               }

               return UserAction.ActionResult.USED_STOP;
            }
         }

         if (Minecraft.getInstance().cameraEntity == null) {
            return UserAction.ActionResult.NOT_HANDLED;
         }

         Vec3 cameraPos = Minecraft.getInstance().cameraEntity.getEyePosition();
         int rangeSq = Axiom.configuration.entityManipulation.markerEntityRange * Axiom.configuration.entityManipulation.markerEntityRange;

         for (Entry<UUID, Gizmo> entry : gizmos.entrySet()) {
            UUID uuid = entry.getKey();
            Gizmo gizmo = entry.getValue();
            Vec3 before = gizmo.getTargetVec();
            boolean isActiveEntity = activeEntity != null && uuid.equals(activeEntity.uuid());
            if ((!(before.distanceToSqr(cameraPos) > rangeSq) || isActiveEntity) && gizmo.isHovered()) {
               if (activeEntity != null) {
                  Gizmo activeGizmo = gizmos.get(activeEntity.uuid());
                  if (activeGizmo != null) {
                     activeGizmo.enableAxes = false;
                     activeGizmo.moveToVec(activeEntity.position());
                  }

                  teleportTicks = 0;
                  activeEntity = null;
                  movementDirty = false;
                  undoStates.clear();
                  redoStates.clear();
               }

               gizmo.enableAxes = true;
               activeEntity = markerDataMap.get(uuid);
               movementDirty = false;
               undoStates.clear();
               redoStates.clear();
               undoStates.add(activeEntity.position());
               if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.MARKER_NBT_REQUEST)) {
                  ChatUtils.error("Server doesn't support requesting marker nbt");
               } else {
                  copyMarkerEntity = false;
                  new AxiomServerboundMarkerNbtRequest(activeEntity.uuid(), AxiomServerboundMarkerNbtRequest.REASON_RIGHT_CLICK).send();
               }

               return UserAction.ActionResult.USED_STOP;
            }
         }
      }

      if (activeEntity != null) {
         HitResult hitResult = Minecraft.getInstance().hitResult;
         if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == Type.BLOCK) {
            BlockPos position = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
            applyTeleport(activeEntity.uuid(), Vec3.atCenterOf(position));
            teleportTicks = 10;
         }

         return UserAction.ActionResult.USED_STOP;
      } else {
         return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   public static void receivedNbtData(UUID uuid, CompoundTag data) {
      if (activeEntity != null && activeEntity.uuid().equals(uuid)) {
         if (copyMarkerEntity) {
            CompoundTag markerTag = new CompoundTag();
            markerTag.put("data", data);
            String nbt = new SnbtPrinterTagVisitor("", 0, new ArrayList()).visit(markerTag);
            String command = "/summon marker ~ ~ ~ " + nbt;
            Minecraft.getInstance().keyboardHandler.setClipboard(command);
            ChatUtils.info("Copied summon command to clipboard (" + command.length() + " characters)");
            ChatUtils.info("You can use Ctrl+V to paste the entity");
         } else {
            Minecraft.getInstance().setScreen(new EditMarkerScreen(uuid, data));
         }
      }
   }

   @NotNull
   private static UserAction.ActionResult handleLeftClick() {
      boolean ret = false;
      if (activeEntity != null) {
         Gizmo gizmo = gizmos.get(activeEntity.uuid());
         if (gizmo != null) {
            if (gizmo.leftClick()) {
               return UserAction.ActionResult.USED_STOP;
            }

            gizmo.enableAxes = false;
            gizmo.moveToVec(activeEntity.position());
            ret = true;
         }

         teleportTicks = 0;
         activeEntity = null;
         movementDirty = false;
         undoStates.clear();
         redoStates.clear();
      }

      if (Minecraft.getInstance().cameraEntity == null) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         Vec3 cameraPos = Minecraft.getInstance().cameraEntity.getEyePosition();
         int rangeSq = Axiom.configuration.entityManipulation.markerEntityRange * Axiom.configuration.entityManipulation.markerEntityRange;

         for (Entry<UUID, Gizmo> entry : gizmos.entrySet()) {
            UUID uuid = entry.getKey();
            Gizmo gizmo = entry.getValue();
            Vec3 before = gizmo.getTargetVec();
            boolean isActiveEntity = activeEntity != null && uuid.equals(activeEntity.uuid());
            if ((!(before.distanceToSqr(cameraPos) > rangeSq) || isActiveEntity) && gizmo.leftClick()) {
               activeEntity = markerDataMap.get(uuid);
               movementDirty = false;
               undoStates.clear();
               redoStates.clear();
               undoStates.add(activeEntity.position());
               return UserAction.ActionResult.USED_STOP;
            }
         }

         return ret ? UserAction.ActionResult.USED_STOP : UserAction.ActionResult.NOT_HANDLED;
      }
   }
}
