package com.moulberry.axiom.render.annotations;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.annotations.AnnotationHistoryElement;
import com.moulberry.axiom.annotations.AnnotationUpdateAction;
import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.core_rendering.AxiomBlending;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.windows.TextAnnotationListWindow;
import com.moulberry.axiom.packets.AxiomServerboundAnnotationUpdate;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.annotation.AnnotationTool;
import com.moulberry.axiom.utils.FramebufferUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL31C;

public class Annotations {
   private static final Map<UUID, Annotation> annotationsByUUID = new LinkedHashMap<>();
   private static final Map<UUID, Annotation> visibleAnnotationsByUUID = new LinkedHashMap<>();
   private static SectionPos lastSectionPos = null;
   private static Vector4f pendingErase = null;
   private static final List<AnnotationHistoryElement> history = new ArrayList<>();
   private static int historyPosition = 0;
   public static boolean hasVisibleOutlineAnnotation = false;
   private static RenderTarget renderTarget = null;

   public static Collection<Entry<UUID, Annotation>> allAnnotations() {
      return annotationsByUUID.entrySet();
   }

   public static Set<Entry<UUID, Annotation>> visibleAnnotations() {
      return visibleAnnotationsByUUID.entrySet();
   }

   public static int totalCount() {
      return annotationsByUUID.size();
   }

   public static void push(AnnotationHistoryElement annotationHistoryElement) {
      while (history.size() > historyPosition) {
         history.remove(history.size() - 1);
      }

      new AxiomServerboundAnnotationUpdate(annotationHistoryElement.redo()).send();
      if (!history.isEmpty() && annotationHistoryElement.singleUndo() instanceof AnnotationUpdateAction.MoveAnnotation undoMove) {
         AnnotationHistoryElement last = history.get(history.size() - 1);
         if (last.singleRedo() instanceof AnnotationUpdateAction.MoveAnnotation redoMove
            && undoMove.uuid().equals(redoMove.uuid())
            && undoMove.to().equals(redoMove.to())) {
            history.remove(history.size() - 1);
            history.add(new AnnotationHistoryElement(last.undo(), annotationHistoryElement.redo()));
            historyPosition = history.size();
            return;
         }
      }

      history.add(annotationHistoryElement);
      historyPosition = history.size();
   }

   public static UUID pushCreateAnnotation(AnnotationData annotation) {
      UUID uuid = UUID.randomUUID();
      push(
         new AnnotationHistoryElement(
            List.of(new AnnotationUpdateAction.DeleteAnnotation(uuid)), List.of(new AnnotationUpdateAction.CreateAnnotation(uuid, annotation))
         )
      );
      return uuid;
   }

   public static void pushUpdateAnnotation(UUID uuid, AnnotationData oldData, AnnotationData newData) {
      push(
         new AnnotationHistoryElement(
            List.of(new AnnotationUpdateAction.CreateAnnotation(uuid, oldData)), List.of(new AnnotationUpdateAction.CreateAnnotation(uuid, newData))
         )
      );
   }

   public static void undo() {
      if (historyPosition != 0) {
         historyPosition--;
         AnnotationHistoryElement element = history.get(historyPosition);
         new AxiomServerboundAnnotationUpdate(element.undo()).send();
      }
   }

   public static void redo() {
      if (historyPosition != history.size()) {
         AnnotationHistoryElement element = history.get(historyPosition);
         new AxiomServerboundAnnotationUpdate(element.redo()).send();
         historyPosition++;
      }
   }

   public static void add(UUID uuid, AnnotationData annotationData) {
      Annotation annotation = Annotation.fromData(annotationData);
      if (annotation != null) {
         if (ToolManager.isToolActive()
            && ToolManager.getCurrentTool() instanceof AnnotationTool annotationTool
            && uuid.equals(annotationTool.justPlacedAnnotation)) {
            annotationTool.selectedGizmo = annotation.getGizmo();
            annotationTool.selectedAnnotation = uuid;
            annotationTool.lastSelectedAnnotation = null;
            annotationTool.justPlacedAnnotation = null;
         }

         Annotation oldAnnotation = annotationsByUUID.put(uuid, annotation);
         if (oldAnnotation != null) {
            oldAnnotation.close();
         }

         if (annotation instanceof TextAnnotation) {
            TextAnnotationListWindow.invalidateTextAnnotations();
         }

         lastSectionPos = null;
      }
   }

   @Nullable
   public static AnnotationData getData(UUID uuid) {
      Annotation annotation = annotationsByUUID.get(uuid);
      return annotation != null ? annotation.getData() : null;
   }

   public static void remove(UUID uuid) {
      Annotation removed = annotationsByUUID.remove(uuid);
      if (removed != null) {
         removed.close();
         if (removed instanceof TextAnnotation) {
            TextAnnotationListWindow.invalidateTextAnnotations();
         }
      }

      removed = visibleAnnotationsByUUID.remove(uuid);
      if (removed != null) {
         removed.close();
      }
   }

   public static void move(UUID uuid, Vector3f location) {
      Annotation annotation = annotationsByUUID.get(uuid);
      if (annotation != null) {
         annotation.getData().setPosition(location);
      }
   }

   public static void rotate(UUID uuid, Quaternionf rotation) {
      Annotation annotation = annotationsByUUID.get(uuid);
      if (annotation != null) {
         annotation.getData().setRotation(rotation);
      }
   }

   public static void clear() {
      for (Annotation value : annotationsByUUID.values()) {
         value.close();
      }

      for (Annotation value : visibleAnnotationsByUUID.values()) {
         value.close();
      }

      annotationsByUUID.clear();
      visibleAnnotationsByUUID.clear();
      history.clear();
      historyPosition = 0;
   }

   public static void erase(Vec2 pos1, Vec2 pos2) {
      pendingErase = new Vector4f(pos1.x, pos1.y, pos2.x, pos2.y);
   }

   public static boolean showAnnotations() {
      return Axiom.configuration.visuals.showAnnotations || ToolManager.isToolActive() && ToolManager.getCurrentTool() instanceof AnnotationTool;
   }

   public static void renderPre(AxiomWorldRenderContext rc) {
      if (showAnnotations() && !annotationsByUUID.isEmpty()) {
         RenderSystem.assertOnRenderThread();
         SectionPos sectionPos = SectionPos.of(rc.blockPosition());
         if (!Objects.equals(lastSectionPos, sectionPos)) {
            lastSectionPos = sectionPos;
            calculateVisibleAnnotations();
         }

         if (visibleAnnotationsByUUID.isEmpty()) {
            pendingErase = null;
         } else {
            BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            bufferSource.endBatch();

            for (Entry<UUID, Annotation> entry : visibleAnnotationsByUUID.entrySet()) {
               if (!entry.getValue().renderPost()) {
                  entry.getValue().render(rc, entry.getKey(), Minecraft.getInstance().getMainRenderTarget());
               }
            }

            bufferSource.endBatch();
            if (pendingErase != null) {
               eraseVisibleAnnotations(rc);
               pendingErase = null;
            }
         }
      } else {
         pendingErase = null;
      }
   }

   public static void renderPost(AxiomWorldRenderContext rc) {
      if (showAnnotations() && !visibleAnnotationsByUUID.isEmpty()) {
         RenderSystem.assertOnRenderThread();
         BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
         bufferSource.endBatch();
         boolean renderedAnything = false;
         int mainWidth = Minecraft.getInstance().getMainRenderTarget().width;
         int mainHeight = Minecraft.getInstance().getMainRenderTarget().height;

         for (Entry<UUID, Annotation> entry : visibleAnnotationsByUUID.entrySet()) {
            if (entry.getValue().renderPost()) {
               if (!renderedAnything) {
                  renderedAnything = true;
                  renderTarget = FramebufferUtils.resizeOrCreateFramebuffer(renderTarget, mainWidth, mainHeight);
                  FramebufferUtils.clear(renderTarget, 0);
                  FramebufferUtils.copyDepth(Minecraft.getInstance().getMainRenderTarget(), renderTarget);
               }

               entry.getValue().render(rc, entry.getKey(), renderTarget);
            }
         }

         bufferSource.endBatch();
         if (renderedAnything) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            FramebufferUtils.blitToMainBlend(renderTarget, mainWidth, mainHeight, AxiomBlending.ONE_ONE_MINUS_SRC_ALPHA);
         }
      }
   }

   private static void calculateVisibleAnnotations() {
      for (Annotation value : visibleAnnotationsByUUID.values()) {
         value.sectionChanged();
      }

      visibleAnnotationsByUUID.clear();
      hasVisibleOutlineAnnotation = false;
      int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();

      for (Entry<UUID, Annotation> entry : annotationsByUUID.entrySet()) {
         SectionPos minSectionPos = entry.getValue().getMinSectionY();
         SectionPos maxSectionPos = entry.getValue().getMaxSection();
         int deltaSectionX = Math.abs(Math.max(minSectionPos.x(), Math.min(maxSectionPos.x(), lastSectionPos.x())) - lastSectionPos.x());
         int deltaSectionY = Math.abs(Math.max(minSectionPos.y(), Math.min(maxSectionPos.y(), lastSectionPos.y())) - lastSectionPos.y());
         int deltaSectionZ = Math.abs(Math.max(minSectionPos.z(), Math.min(maxSectionPos.z(), lastSectionPos.z())) - lastSectionPos.z());
         if (deltaSectionX * deltaSectionX + deltaSectionZ * deltaSectionZ <= renderDistance * renderDistance && deltaSectionY <= renderDistance) {
            visibleAnnotationsByUUID.put(entry.getKey(), entry.getValue());
            if (entry.getValue() instanceof OutlineAnnotation) {
               hasVisibleOutlineAnnotation = true;
            }
         }
      }
   }

   private static void eraseVisibleAnnotations(AxiomWorldRenderContext rc) {
      Window window = Minecraft.getInstance().getWindow();
      float minX = (Math.min(pendingErase.x, pendingErase.z) - EditorUI.frameX) / EditorUI.frameWidth;
      float maxX = (Math.max(pendingErase.x, pendingErase.z) - EditorUI.frameX) / EditorUI.frameWidth;
      float minY = (Math.min(pendingErase.y, pendingErase.w) - EditorUI.frameY) / EditorUI.frameHeight;
      float maxY = (Math.max(pendingErase.y, pendingErase.w) - EditorUI.frameY) / EditorUI.frameHeight;
      minX = Math.max(0.0F, Math.min(1.0F, minX));
      maxX = Math.max(0.0F, Math.min(1.0F, maxX));
      minY = Math.max(0.0F, Math.min(1.0F, minY));
      maxY = Math.max(0.0F, Math.min(1.0F, maxY));
      float left = window.getWidth() * minX;
      float bottom = window.getHeight() - window.getHeight() * maxY;
      float width = window.getWidth() * (maxX - minX);
      float height = window.getHeight() * (maxY - minY);
      AxiomRenderer.enableScissor((int)left, (int)bottom, (int)width, (int)height);
      List<AnnotationUpdateAction> undo = new ArrayList<>();
      List<AnnotationUpdateAction> redo = new ArrayList<>();
      BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
      bufferSource.endBatch();

      for (Entry<UUID, Annotation> entry : visibleAnnotationsByUUID.entrySet()) {
         Annotation annotation = entry.getValue();
         int query = GL31C.glGenQueries();
         GL31C.glBeginQuery(35092, query);
         annotation.render(rc, entry.getKey(), Minecraft.getInstance().getMainRenderTarget());
         bufferSource.endBatch();
         GL31C.glEndQuery(35092);
         if (GL31C.glGetQueryObjecti(query, 34918) > 0) {
            undo.add(new AnnotationUpdateAction.CreateAnnotation(entry.getKey(), annotation.getData()));
            redo.add(new AnnotationUpdateAction.DeleteAnnotation(entry.getKey()));
         }

         GL31C.glDeleteQueries(query);
      }

      push(new AnnotationHistoryElement(undo, redo));
      AxiomRenderer.disableScissor();
   }
}
