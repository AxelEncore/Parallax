package com.moulberry.axiom.displayentity;

import net.minecraft.util.StringRepresentable;

public enum GizmoMode implements StringRepresentable {
   GLOBAL("global"),
   LOCAL("local");

   private final String name;

   private GizmoMode(String name) {
      this.name = name;
   }

   public String getSerializedName() {
      return this.name;
   }
}
