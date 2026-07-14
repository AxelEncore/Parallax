package com.moulberry.axiom.editor.tutorial;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.exceptions.FaultyImplementationError;

public enum Tutorial {
   INTRODUCTION("introduction", TutorialStage.TOOLS_WINDOW),
   SELECTION("selection", TutorialStage.SELECTION1),
   MAGIC_SELECT_TOOL("magic_select_tool", TutorialStage.MAGIC_SELECT_TOOL),
   BOX_SELECT_TOOL("box_select_tool", TutorialStage.BOX_SELECT_TOOL1),
   SLOPE_TOOL("slope_tool", TutorialStage.SLOPE_TOOL1),
   SCULPT_DRAW_TOOL("sculpt_draw_tool", TutorialStage.SCULPT_DRAW_TOOL);

   private final String id;
   private final TutorialStage firstStage;
   private boolean completed = false;
   private final int stageCount;

   private Tutorial(String id, TutorialStage firstStage) {
      this.id = id;
      this.firstStage = firstStage;
      int stageCount = 0;

      for (TutorialStage stage = firstStage; stage != null; stageCount++) {
         if (stage.tutorial != null) {
            throw new FaultyImplementationError("Duplicate stage in tutorials: " + firstStage);
         }

         stage.tutorial = this;
         stage.stageIndex = stageCount;
         stage = stage.getNextStage();
      }

      this.stageCount = stageCount;
   }

   public int stageCount() {
      return this.stageCount;
   }

   public void complete() {
      this.completed = true;
      Axiom.configuration.internal.completedTutorials.add(this.id);
   }

   public void uncomplete() {
      this.completed = false;
      Axiom.configuration.internal.completedTutorials.remove(this.id);
   }

   public void initiateIfNotCompleted() {
      if (!Axiom.configuration.internal.askedTutorialPreference) {
         return;
      }

      if (!this.completed) {
         if (TutorialManager.getCurrentStage() == null) {
            if (Axiom.configuration.internal.completedTutorials.contains(this.id)) {
               this.completed = true;
            } else {
               TutorialManager.setStage(this.firstStage);
            }
         }
      }
   }
}
