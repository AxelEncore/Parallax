package com.moulberry.axiom.annotations;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.packets.AxiomClientboundAnnotationUpdate;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class ServerAnnotations extends SavedData {
   private final LinkedHashMap<UUID, AnnotationData> annotations = new LinkedHashMap<>();
   private static final Factory<ServerAnnotations> FACTORY = new Factory<ServerAnnotations>(ServerAnnotations::new, ServerAnnotations::read, null);

   private static ServerAnnotations read(CompoundTag tag, Provider provider) {
      ServerAnnotations serverAnnotations = new ServerAnnotations();

      for (String key : tag.getAllKeys()) {
         try {
            UUID uuid = UUID.fromString(key);
            byte[] bytes = VersionUtilsNbt.helperCompoundTagGetByteArray(tag, key).get();
            AnnotationData annotation = AnnotationData.read(new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
            serverAnnotations.annotations.put(uuid, annotation);
         } catch (Exception var8) {
            Axiom.LOGGER.error("Error reading annotation", var8);
         }
      }

      return serverAnnotations;
   }

   public CompoundTag save(CompoundTag compoundTag, Provider provider) {
      FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());

      for (Entry<UUID, AnnotationData> entry : this.annotations.entrySet()) {
         try {
            friendlyByteBuf.writerIndex(0);
            entry.getValue().write(friendlyByteBuf);
            byte[] bytes = new byte[friendlyByteBuf.writerIndex()];
            friendlyByteBuf.getBytes(0, bytes);
            compoundTag.putByteArray(entry.getKey().toString(), bytes);
         } catch (Exception var7) {
            Axiom.LOGGER.error("Error writing annotation", var7);
         }
      }

      return compoundTag;
   }

   public static void sendAll(ServerLevel serverLevel, ServerPlayer player) {
      ServerAnnotations serverAnnotations = (ServerAnnotations)serverLevel.getChunkSource().getDataStorage().get(FACTORY, "axiom_annotations");
      List<AnnotationUpdateAction> actions = new ArrayList<>();
      actions.add(new AnnotationUpdateAction.ClearAllAnnotations());
      if (serverAnnotations != null) {
         for (Entry<UUID, AnnotationData> entry : serverAnnotations.annotations.entrySet()) {
            actions.add(new AnnotationUpdateAction.CreateAnnotation(entry.getKey(), entry.getValue()));
         }
      }

      new AxiomClientboundAnnotationUpdate(actions).send(player);
   }

   public static void handleUpdates(ServerLevel serverLevel, List<AnnotationUpdateAction> actions) {
      ServerAnnotations serverAnnotations = (ServerAnnotations)serverLevel.getChunkSource().getDataStorage().computeIfAbsent(FACTORY, "axiom_annotations");

      for (AnnotationUpdateAction action : actions) {
         if (action instanceof AnnotationUpdateAction.CreateAnnotation create) {
            serverAnnotations.annotations.put(create.uuid(), create.annotationData());
            serverAnnotations.setDirty();
         } else if (action instanceof AnnotationUpdateAction.DeleteAnnotation delete) {
            AnnotationData removed = serverAnnotations.annotations.remove(delete.uuid());
            if (removed != null) {
               serverAnnotations.setDirty();
            }
         } else if (action instanceof AnnotationUpdateAction.MoveAnnotation move) {
            AnnotationData annotation = serverAnnotations.annotations.get(move.uuid());
            if (annotation != null) {
               annotation.setPosition(move.to());
               serverAnnotations.setDirty();
            }
         } else if (action instanceof AnnotationUpdateAction.ClearAllAnnotations) {
            if (!serverAnnotations.annotations.isEmpty()) {
               serverAnnotations.annotations.clear();
               serverAnnotations.setDirty();
            }
         } else {
            if (!(action instanceof AnnotationUpdateAction.RotateAnnotation rotate)) {
               throw new UnsupportedOperationException("Unknown action: " + action.getClass());
            }

            AnnotationData annotation = serverAnnotations.annotations.get(rotate.uuid());
            if (annotation != null) {
               annotation.setRotation(rotate.to());
               serverAnnotations.setDirty();
            }
         }
      }

      AxiomClientboundAnnotationUpdate packet = new AxiomClientboundAnnotationUpdate(actions);

      for (ServerPlayer player : serverLevel.players()) {
         packet.send(player);
      }
   }
}
