package com.moulberry.axiom.editor.tutorial;

import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.rock.RockTool;
import org.jetbrains.annotations.Nullable;

public class TutorialManager {
   private static TutorialStage currentStage = null;

   public static void reset(Tutorial tutorial) {
      tutorial.uncomplete();
      Tutorial.INTRODUCTION.initiateIfNotCompleted();
   }

   public static void skip() {
      for (Tutorial tutorial : Tutorial.values()) {
         tutorial.complete();
      }

      setStage(null);
   }

   public static void initialize() {
      Tutorial.INTRODUCTION.initiateIfNotCompleted();
   }

   public static void setStage(TutorialStage tutorialStage) {
      currentStage = tutorialStage;
   }

   @Nullable
   public static TutorialStage getCurrentStage() {
      return currentStage;
   }

   @Nullable
   public static EditorWindowType getCurrentStageLinkedWindow() {
      return currentStage == null ? null : currentStage.getLinkedWindow();
   }

   public static void nextTutorialStage() {
      TutorialStage nextStage = currentStage.getNextStage();
      if (nextStage == null) {
         currentStage.getTutorial().complete();
         currentStage = null;
      } else {
         currentStage = nextStage;
         if (currentStage == TutorialStage.TOOL_OPTIONS && !ToolManager.isToolActive()) {
            ToolManager.setTool(RockTool.class);
            ToolManager.setToolSelected(true);
         }
      }
   }
}
