package com.moulberry.axiom.editor.windows.clipboard;

import com.moulberry.axiom.blueprint.BlueprintHeader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

public abstract class BlueprintOrDirectory {
   BlueprintOrDirectory prevNode;
   BlueprintOrDirectory nextNode;
   BlueprintOrDirectory prevSearchNode;
   BlueprintOrDirectory nextSearchNode;

   public abstract PathWrapper path();

   public abstract void path(Path var1);

   public abstract boolean nameContainsLower(String var1);

   public abstract boolean containsAllTags(Collection<String> var1);

   public void unlink() {
      if (this.prevNode != null) {
         this.prevNode.nextNode = this.nextNode;
      }

      if (this.nextNode != null) {
         this.nextNode.prevNode = this.prevNode;
      }

      if (this.prevSearchNode != null) {
         this.prevSearchNode.nextSearchNode = this.nextSearchNode;
      }

      if (this.nextSearchNode != null) {
         this.nextSearchNode.prevSearchNode = this.prevSearchNode;
      }

      this.prevNode = null;
      this.nextNode = null;
      this.prevSearchNode = null;
      this.nextSearchNode = null;
   }

   public static final class Bp extends BlueprintOrDirectory {
      private PathWrapper path;
      public final BlueprintHeader blueprint;

      public Bp(PathWrapper path, BlueprintHeader blueprint) {
         this.path = path;
         this.blueprint = blueprint;
      }

      @Override
      public PathWrapper path() {
         return this.path;
      }

      @Override
      public void path(Path path) {
         this.path = new PathWrapper(path, null);
      }

      @Override
      public boolean nameContainsLower(String string) {
         return this.blueprint.name().toLowerCase(Locale.ROOT).contains(string);
      }

      @Override
      public boolean containsAllTags(Collection<String> tags) {
         return new HashSet<>(this.blueprint.tags()).containsAll(tags);
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else if (obj != null && obj.getClass() == this.getClass()) {
            BlueprintOrDirectory.Bp that = (BlueprintOrDirectory.Bp)obj;
            return Objects.equals(this.path, that.path);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.path.hashCode();
      }
   }

   public static final class Dir extends BlueprintOrDirectory {
      public final BlueprintDirectory blueprintDirectory;

      public Dir(BlueprintDirectory blueprintDirectory) {
         this.blueprintDirectory = blueprintDirectory;
      }

      @Override
      public PathWrapper path() {
         return this.blueprintDirectory.path();
      }

      @Override
      public void path(Path path) {
         this.blueprintDirectory.setPath(new PathWrapper(path, null));
      }

      @Override
      public boolean nameContainsLower(String string) {
         return this.blueprintDirectory.path().getFileName().toLowerCase(Locale.ROOT).contains(string);
      }

      @Override
      public boolean containsAllTags(Collection<String> tags) {
         return this.blueprintDirectory.tags().containsAll(tags);
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            BlueprintOrDirectory.Dir dir = (BlueprintOrDirectory.Dir)o;
            return this.blueprintDirectory.equals(dir.blueprintDirectory);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.blueprintDirectory.hashCode();
      }
   }
}
