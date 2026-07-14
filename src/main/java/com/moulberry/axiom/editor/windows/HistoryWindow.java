package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.editor.tutorial.TutorialStage;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiom.world_modification.UndoRedoTracer;
import com.moulberry.axiom.utils.Authorization;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiListClipper;
import imgui.moulberry92.callback.ImListClipperCallback;
import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;

public class HistoryWindow {
   private static int lastHistoryPosition = -1;
   private static int historyPopupPosition = -1;
   private static boolean hiddenHistoryIdentifier = false;
   private static boolean obscuredHistoryIdentifier = true;
   private static boolean revertHistoryIKnowWhatImDoing = false;
   private static boolean bigUndoIKnowWhatImDoing = false;

   public static void render() {
      boolean showTutorial = TutorialManager.getCurrentStage() == TutorialStage.HISTORY;
      if (EditorWindowType.HISTORY.isOpen() || showTutorial) {
         if (EditorWindowType.HISTORY.begin("###History", true)) {
            if (!hiddenHistoryIdentifier) {
               if (Dispatcher.historyIdentifier != null) {
                  if (obscuredHistoryIdentifier) {
                     String[] split = Dispatcher.historyIdentifier.split("/");
                     if (split.length >= 3) {
                        StringBuilder builder = new StringBuilder();
                        boolean first = true;

                        for (int i = 2; i < split.length; i++) {
                           if (first) {
                              first = false;
                           } else {
                              builder.append(",");
                           }

                           builder.append(split[i]);
                        }

                        ImGui.text(split[0] + "/(Hidden)/" + builder);
                        if (ImGui.isItemClicked()) {
                           obscuredHistoryIdentifier = false;
                        }

                        ImGuiHelper.tooltip("Click to show");
                     } else {
                        ImGui.text(Dispatcher.historyIdentifier);
                     }
                  } else {
                     ImGui.text(Dispatcher.historyIdentifier);
                  }
               } else {
                  ImGui.text("⚠ History is not being saved");
                  if (!Authorization.hasCommercialLicense()) {
                     ImGuiHelper.tooltip("Automatic history persistence is a Commercial License feature. Click for more information");
                     ImGuiHelper.openCommercialLicenseOnClick();
                  }
               }

               ImGui.sameLine();
               if (ImGui.smallButton(AxiomI18n.get("axiom.hardcoded.hide"))) {
                  hiddenHistoryIdentifier = true;
               }

               ImGui.separator();
            }

            long historyBytes = Dispatcher.getHistoryBytes();
            String bytesString;
            if (historyBytes < 1000L) {
               bytesString = NumberFormat.getInstance().format(historyBytes) + " bytes";
            } else if (historyBytes < 1000000L) {
               bytesString = NumberFormat.getInstance().format(historyBytes / 1000L) + "kB";
            } else {
               bytesString = NumberFormat.getInstance().format(historyBytes / 1000L / 1000L) + "MB";
            }

            ImGui.text(AxiomI18n.get("axiom.editorui.window.history.history_size", bytesString));
            ImGui.sameLine();
            if (ImGui.smallButton(AxiomI18n.get("axiom.editorui.window.history.clear") + "##ClearHistory")) {
               Dispatcher.clear();
            }

            ImGui.separator();
            if (Dispatcher.getHistoryDataCount() == 0) {
               ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.history.no_history"));
               if (showTutorial) {
                  TutorialStage.HISTORY.render(ImGui.getWindowPos(), ImGui.getWindowSize());
               }

               EditorWindowType.HISTORY.end();
               return;
            }

            if (ImGui.beginChild("##Scroller")) {
               final int position = Dispatcher.getHistoryPosition(true);
               boolean historyPositionChanged = position != lastHistoryPosition;
               if (historyPositionChanged) {
                  lastHistoryPosition = position;
                  float textLineHeightWithSpacing = ImGui.getTextLineHeightWithSpacing();
                  float localY = ImGui.getCursorStartPosY() + textLineHeightWithSpacing * position;
                  float windowHeight = ImGui.getWindowHeight();
                  if (ImGui.getWindowHeight() < textLineHeightWithSpacing * 4.0F) {
                     if (localY < textLineHeightWithSpacing) {
                        ImGui.setScrollFromPosY(ImGui.getCursorStartPosY() + textLineHeightWithSpacing * position, 0.0F);
                     } else if (localY > windowHeight - textLineHeightWithSpacing) {
                        ImGui.setScrollFromPosY(ImGui.getCursorStartPosY() + textLineHeightWithSpacing * position + textLineHeightWithSpacing, 1.0F);
                     }
                  } else if (localY < textLineHeightWithSpacing * 2.0F) {
                     ImGui.setScrollFromPosY(ImGui.getCursorStartPosY() + textLineHeightWithSpacing * position - textLineHeightWithSpacing, 0.0F);
                  } else if (localY > windowHeight - textLineHeightWithSpacing * 2.0F) {
                     ImGui.setScrollFromPosY(ImGui.getCursorStartPosY() + textLineHeightWithSpacing * position + textLineHeightWithSpacing * 2.0F, 1.0F);
                  }
               }

               final AtomicBoolean disabledAlpha = new AtomicBoolean(false);
               final AtomicInteger setHistoryPosition = new AtomicInteger(-1);
               final AtomicInteger revertHistoryPosition = new AtomicInteger(-1);
               final int historyCount = Dispatcher.getHistoryDataCount();
               final int historyPosition = Dispatcher.getHistoryPosition(true);
               ImGuiListClipper.forEach(
                  historyCount,
                  (int)ImGui.getTextLineHeightWithSpacing(),
                  new ImListClipperCallback() {
                     public void accept(int index) {
                        Dispatcher.HistoryData historyData = Dispatcher.getHistoryData(index);
                        if (historyData != null) {
                           ImGui.pushID(index);
                           boolean shouldUseDisabledAlpha = index != position;
                           if (disabledAlpha.get() != shouldUseDisabledAlpha) {
                              disabledAlpha.set(shouldUseDisabledAlpha);
                              if (shouldUseDisabledAlpha) {
                                 ImGuiHelper.pushStyleVar(0, ImGui.getStyle().getAlpha() * ImGui.getStyle().getDisabledAlpha());
                              } else {
                                 ImGuiHelper.popStyleVar();
                              }
                           }

                           if (ImGui.menuItem("(" + historyData.getIndexIdentifier() + ") " + historyData.entry().description())) {
                              ImGui.openPopup("##HistoryItemPopup");
                              HistoryWindow.historyPopupPosition = index;
                           }

                           if (ImGui.isItemClicked(1)) {
                              ImGui.openPopup("##HistoryItemPopup");
                              HistoryWindow.historyPopupPosition = index;
                           }

                           if (HistoryWindow.historyPopupPosition == index) {
                              if (disabledAlpha.get()) {
                                 ImGuiHelper.popStyleVar();
                              }

                              boolean openUndoWarningPopup = false;
                              boolean openRevertPopup = false;
                              if (ImGui.beginPopup("##HistoryItemPopup", 64)) {
                                 String message = HistoryWindow.historyPopupPosition <= historyPosition ? "Undo to this point" : "Redo to this point";
                                 if (HistoryWindow.historyPopupPosition == historyPosition) {
                                    ImGui.beginDisabled();
                                 }

                                 if (ImGui.menuItem(message)) {
                                    if (Math.abs(HistoryWindow.historyPopupPosition - historyPosition) >= 10) {
                                       openUndoWarningPopup = true;
                                    } else {
                                       setHistoryPosition.set(HistoryWindow.historyPopupPosition);
                                    }
                                 }

                                 if (HistoryWindow.historyPopupPosition == historyPosition) {
                                    ImGui.endDisabled();
                                 }

                                 if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.locate_change"))) {
                                    Minecraft.getInstance().player.lookAt(Anchor.EYES, historyData.entry().origin().getCenter());
                                    Dispatcher.addTracer(new UndoRedoTracer(historyData.entry().origin().getCenter(), -256, false));
                                 }

                                 if (historyData.entry().backwards() instanceof BlockBuffer
                                    && HistoryWindow.historyPopupPosition <= historyPosition - 1
                                    && historyPosition >= historyCount - 2
                                    && ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.revert_without_undo"))) {
                                    openRevertPopup = true;
                                 }

                                 ImGui.endPopup();
                              }

                              if (openUndoWarningPopup) {
                                 ImGui.openPopup("Undo/Redo Warning!##HistoryUndoWarning");
                              }

                              if (ImGuiHelper.beginPopupModalCloseable("Undo/Redo Warning!##HistoryUndoWarning", 64)) {
                                 int changeCount = Math.abs(HistoryWindow.historyPopupPosition - historyPosition);
                                 String warning = HistoryWindow.historyPopupPosition <= historyPosition ? "Undoing" : "Redoing";
                                 warning = warning
                                    + " to this point will result in a lot of changes. If you have modified the world by hand or by using a non-Axiom tool, these changes may get overriden and may be impossible to recover!";
                                 ImGui.pushTextWrapPos(500.0F);
                                 ImGui.textWrapped(warning);
                                 ImGui.popTextWrapPos();
                                 if (!HistoryWindow.bigUndoIKnowWhatImDoing) {
                                    if (ImGui.button(AxiomI18n.get("axiom.hardcoded.i_know_what_im_doing"))) {
                                       HistoryWindow.bigUndoIKnowWhatImDoing = true;
                                    }
                                 } else {
                                    if (ImGui.button(
                                       HistoryWindow.historyPopupPosition <= historyPosition
                                          ? "Undo " + changeCount + " changes"
                                          : "Redo " + changeCount + " changes"
                                    )) {
                                       setHistoryPosition.set(HistoryWindow.historyPopupPosition);
                                       ImGui.closeCurrentPopup();
                                    }

                                    ImGui.sameLine();
                                    if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
                                       ImGui.closeCurrentPopup();
                                    }
                                 }

                                 ImGuiHelper.endPopupModalCloseable();
                              }

                              if (openRevertPopup) {
                                 ImGui.openPopup("Revert Change##RevertWarning");
                              }

                              if (ImGuiHelper.beginPopupModalCloseable("Revert Change##RevertWarning")) {
                                 String explanation = "This function will revert the history change without undoing to that point. It will create a new history element. This is an advanced feature and should not be used unless you know what you're doing!";
                                 ImGui.pushTextWrapPos(500.0F);
                                 ImGui.textWrapped(explanation);
                                 ImGui.popTextWrapPos();
                                 if (!HistoryWindow.revertHistoryIKnowWhatImDoing) {
                                    if (ImGui.button(AxiomI18n.get("axiom.hardcoded.i_know_what_im_doing"))) {
                                       HistoryWindow.revertHistoryIKnowWhatImDoing = true;
                                    }
                                 } else {
                                    if (ImGui.button(AxiomI18n.get("axiom.hardcoded.revert_quote") + historyData.entry().description() + "'")) {
                                       revertHistoryPosition.set(HistoryWindow.historyPopupPosition);
                                       ImGui.closeCurrentPopup();
                                    }

                                    ImGui.sameLine();
                                    if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
                                       ImGui.closeCurrentPopup();
                                    }
                                 }

                                 ImGuiHelper.endPopupModalCloseable();
                              }

                              if (disabledAlpha.get()) {
                                 ImGuiHelper.pushStyleVar(0, ImGui.getStyle().getAlpha() * ImGui.getStyle().getDisabledAlpha());
                              }
                           }

                           ImGui.popID();
                        }
                     }
                  }
               );
               if (setHistoryPosition.get() != -1) {
                  Dispatcher.setHistoryPosition(setHistoryPosition.get());
               }

               if (revertHistoryPosition.get() != -1) {
                  Dispatcher.HistoryData history = Dispatcher.getHistoryData(revertHistoryPosition.get());
                  HistoryEntry<?> entry = history.entry();
                  if (entry.backwards() instanceof BlockBuffer blockBuffer) {
                     String description = "Revert '" + entry.description() + "'";
                     RegionHelper.pushBlockBufferChange(blockBuffer, entry.origin(), description, null);
                  }
               }

               if (disabledAlpha.get()) {
                  ImGuiHelper.popStyleVar();
               }
            }

            ImGui.endChild();
            if (showTutorial) {
               TutorialStage.HISTORY.render(ImGui.getWindowPos(), ImGui.getWindowSize());
            }
         } else if (showTutorial) {
            TutorialManager.nextTutorialStage();
         }

         EditorWindowType.HISTORY.end();
      }
   }

   private static String formatBytes(int bytes) {
      String[] shorthand = new String[]{"b", "kB", "MB", "GB"};

      int exp;
      for (exp = 0; bytes >= 10000 && exp < shorthand.length - 1; exp++) {
         bytes = (bytes + 512) / 1024;
      }

      return NumberFormat.getInstance().format((long)bytes) + shorthand[exp];
   }
}
