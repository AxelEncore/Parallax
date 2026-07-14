package com.moulberry.axiom.editor.windows.clipboard;

import java.nio.file.Path;
import java.util.Objects;

public record PathWrapper(Path real, String fakePath) {
   public String getFileName() {
      if (this.real != null) {
         return this.real.getFileName().toString();
      } else {
         String[] split = this.fakePath.split("/");
         return split[split.length - 1];
      }
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         PathWrapper that = (PathWrapper)o;
         return !Objects.equals(this.real, that.real) ? false : Objects.equals(this.fakePath, that.fakePath);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.real != null ? this.real.hashCode() : 0;
      return 31 * result + (this.fakePath != null ? this.fakePath.hashCode() : 0);
   }
}
