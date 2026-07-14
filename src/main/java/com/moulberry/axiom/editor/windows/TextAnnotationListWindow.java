package com.moulberry.axiom.editor.windows;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.annotations.AnnotationHistoryElement;
import com.moulberry.axiom.annotations.data.TextAnnotationData;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.packets.AxiomServerboundTeleport;
import com.moulberry.axiom.render.annotations.Annotation;
import com.moulberry.axiom.render.annotations.Annotations;
import com.moulberry.axiom.render.annotations.TextAnnotation;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiListClipper;
import imgui.moulberry92.callback.ImListClipperCallback;
import imgui.moulberry92.type.ImString;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public class TextAnnotationListWindow {
   private static List<TextAnnotationListWindow.TextAnnotationEntry> textAnnotations = null;
   private static final ImString searchString = ImGuiHelper.createResizableString(32);
   private static String lastSearch = null;

   public static void invalidateTextAnnotations() {
      textAnnotations = null;
   }

   private static List<TextAnnotationListWindow.TextAnnotationEntry> getTextAnnotations() {
      String search = ImGuiHelper.getString(searchString).toLowerCase(Locale.ROOT);
      if (textAnnotations == null || lastSearch == null || !Objects.equals(lastSearch, search)) {
         if (textAnnotations == null) {
            textAnnotations = new ArrayList<>();
         } else {
            textAnnotations.clear();
         }

         lastSearch = search;
         if (!search.isEmpty()) {
            List<TextAnnotationListWindow.TextAnnotationEntry> textAnnotationContains = new ArrayList<>();
            int index = 0;

            for (Entry<UUID, Annotation> entry : Annotations.allAnnotations()) {
               if (entry.getValue() instanceof TextAnnotation textAnnotation) {
                  String text = ((TextAnnotationData)textAnnotation.getData()).text();
                  String textLower = text.toLowerCase(Locale.ROOT);
                  if (textLower.startsWith(search)) {
                     textAnnotations.add(new TextAnnotationListWindow.TextAnnotationEntry(entry.getKey(), textAnnotation, index));
                  } else if (textLower.contains(search)) {
                     textAnnotationContains.add(new TextAnnotationListWindow.TextAnnotationEntry(entry.getKey(), textAnnotation, index));
                  }

                  index++;
               }
            }

            textAnnotations.addAll(textAnnotationContains);
         } else {
            int index = 0;

            for (Entry<UUID, Annotation> entryx : Annotations.allAnnotations()) {
               if (entryx.getValue() instanceof TextAnnotation textAnnotation) {
                  textAnnotations.add(new TextAnnotationListWindow.TextAnnotationEntry(entryx.getKey(), textAnnotation, index));
                  index++;
               }
            }
         }
      }

      return textAnnotations;
   }

   public static void render() {
      if (EditorWindowType.TEXT_ANNOTATIONS.isOpen()) {
         Level level = Minecraft.getInstance().level;
         if (level != null) {
            Entity entity = Minecraft.getInstance().cameraEntity;
            if (entity != null) {
               if (EditorWindowType.TEXT_ANNOTATIONS.begin("###TextAnnotationList", true)) {
                  ImGui.inputText("Search", searchString);
                  if (ImGui.beginChild("##TextAnnotations", 0.0F, 0.0F, true)) {
                     final List<TextAnnotationListWindow.TextAnnotationEntry> textAnnotations = getTextAnnotations();
                     if (textAnnotations.isEmpty()) {
                        ImGui.text(AxiomI18n.get("axiom.hardcoded.no_text_annotations"));
                     } else {
                        ImGuiListClipper.forEach(
                           textAnnotations.size(),
                           new ImListClipperCallback() {
                              public void accept(int index) {
                                 TextAnnotationListWindow.TextAnnotationEntry entry = textAnnotations.get(index);
                                 Annotation annotation = entry.textAnnotation();
                                 ImGui.pushID(entry.index);
                                 ImGuiHelper.separatorWithText("#" + (entry.index + 1));
                                 TextAnnotationData textAnnotationData = (TextAnnotationData)annotation.getData();
                                 Vector3f position = textAnnotationData.position();
                                 ImString textInput = new ImString(textAnnotationData.text());
                                 textInput.inputData.isResizable = true;
                                 ImGui.inputTextMultiline("##Text", textInput, -1.0F, 100.0F, 0);
                                 if (ImGui.isItemDeactivatedAfterEdit()) {
                                    String newText = ImGuiHelper.getString(textInput);
                                    if (!newText.isBlank() && !newText.equals(textAnnotationData.text())) {
                                       TextAnnotationData newData = textAnnotationData.withText(newText);
                                       Annotations.pushUpdateAnnotation(entry.uuid(), textAnnotationData, newData);
                                    }
                                 }

                                 StringBuilder teleport = new StringBuilder();
                                 teleport.append("Teleport (");
                                 teleport.append((int)Math.floor(position.x()));
                                 teleport.append(", ");
                                 teleport.append((int)Math.floor(position.y()));
                                 teleport.append(", ");
                                 teleport.append((int)Math.floor(position.z()));
                                 teleport.append(")");
                                 if (ImGui.smallButton(teleport.toString())) {
                                    LocalPlayer player = Minecraft.getInstance().player;
                                    new AxiomServerboundTeleport(
                                          player.level().dimension(), position.x, position.y, position.z, player.getYRot(), player.getXRot()
                                       )
                                       .send();
                                 }

                                 ImGui.sameLine();
                                 if (ImGui.smallButton(AxiomI18n.get("axiom.hardcoded.remove"))) {
                                    Annotations.push(AnnotationHistoryElement.makeDeleteAnnotation(entry.uuid(), textAnnotationData));
                                 }

                                 ImGui.popID();
                              }
                           }
                        );
                     }
                  }

                  ImGui.endChild();
               }

               EditorWindowType.TEXT_ANNOTATIONS.end();
            }
         }
      }
   }

   private record TextAnnotationEntry(UUID uuid, TextAnnotation textAnnotation, int index) {
   }
}
