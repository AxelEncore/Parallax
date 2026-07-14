package com.moulberry.axiom.annotations;

import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.render.annotations.Annotations;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface AnnotationUpdateAction {
   void apply();

   void write(FriendlyByteBuf var1);

   static AnnotationUpdateAction read(FriendlyByteBuf friendlyByteBuf) {
      byte type = friendlyByteBuf.readByte();
      if (type == 0) {
         UUID uuid = friendlyByteBuf.readUUID();
         AnnotationData annotationData = AnnotationData.read(friendlyByteBuf);
         return annotationData == null ? null : new AnnotationUpdateAction.CreateAnnotation(uuid, annotationData);
      } else if (type == 1) {
         return new AnnotationUpdateAction.DeleteAnnotation(friendlyByteBuf.readUUID());
      } else if (type == 2) {
         return new AnnotationUpdateAction.MoveAnnotation(
            friendlyByteBuf.readUUID(), new Vector3f(friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat())
         );
      } else if (type == 3) {
         return new AnnotationUpdateAction.ClearAllAnnotations();
      } else {
         return type == 4
            ? new AnnotationUpdateAction.RotateAnnotation(
               friendlyByteBuf.readUUID(),
               new Quaternionf(friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat(), friendlyByteBuf.readFloat())
            )
            : null;
      }
   }

   public record ClearAllAnnotations() implements AnnotationUpdateAction {
      @Override
      public void apply() {
         Annotations.clear();
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeByte(3);
      }
   }

   public record CreateAnnotation(UUID uuid, AnnotationData annotationData) implements AnnotationUpdateAction {
      @Override
      public void apply() {
         Annotations.add(this.uuid, this.annotationData);
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeByte(0);
         friendlyByteBuf.writeUUID(this.uuid);
         this.annotationData.write(friendlyByteBuf);
      }
   }

   public record DeleteAnnotation(UUID uuid) implements AnnotationUpdateAction {
      @Override
      public void apply() {
         Annotations.remove(this.uuid);
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeByte(1);
         friendlyByteBuf.writeUUID(this.uuid);
      }
   }

   public record MoveAnnotation(UUID uuid, Vector3f to) implements AnnotationUpdateAction {
      @Override
      public void apply() {
         Annotations.move(this.uuid, this.to);
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeByte(2);
         friendlyByteBuf.writeUUID(this.uuid);
         friendlyByteBuf.writeFloat(this.to.x);
         friendlyByteBuf.writeFloat(this.to.y);
         friendlyByteBuf.writeFloat(this.to.z);
      }
   }

   public record RotateAnnotation(UUID uuid, Quaternionf to) implements AnnotationUpdateAction {
      @Override
      public void apply() {
         Annotations.rotate(this.uuid, this.to);
      }

      @Override
      public void write(FriendlyByteBuf friendlyByteBuf) {
         friendlyByteBuf.writeByte(4);
         friendlyByteBuf.writeUUID(this.uuid);
         friendlyByteBuf.writeFloat(this.to.x);
         friendlyByteBuf.writeFloat(this.to.y);
         friendlyByteBuf.writeFloat(this.to.z);
         friendlyByteBuf.writeFloat(this.to.w);
      }
   }
}
