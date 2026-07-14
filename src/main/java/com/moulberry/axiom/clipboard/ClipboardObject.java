package com.moulberry.axiom.clipboard;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.blueprint.Blueprint;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.editor.BlueprintPreview;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.AutoCleaningDynamicTexture;
import com.moulberry.axiom.utils.DynamicTextureSupplier;
import com.moulberry.axiom.utils.EntityDataUtils;
import com.moulberry.axiom.utils.IntMatrix;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public interface ClipboardObject {
   ChunkedBlockRegion blockRegion();

   Long2ObjectMap<CompressedBlockEntity> blockEntities();

   List<CompoundTag> entities();

   String name();

   float preferredYaw();

   boolean containsAir();

   String placementDescription();

   int thumbnailTextureId();

   static ClipboardObject rotate(ClipboardObject clipboardObject, Axis axis, int amount) {
      if (clipboardObject == null) {
         return null;
      } else {
         ChunkedBlockRegion chunkedBlockRegion = clipboardObject.blockRegion().rotate(axis, amount);
         IntMatrix matrix = new IntMatrix();
         matrix.rotate(axis, amount);
         Long2ObjectMap<CompressedBlockEntity> blockEntities = applyMatrixToBlockEntities(matrix, clipboardObject.blockEntities());
         List<CompoundTag> entities = applyMatrixToEntities(matrix, clipboardObject.entities());
         return new ClipboardObject.Anonymous(
            chunkedBlockRegion, blockEntities, entities, clipboardObject.name(), clipboardObject.preferredYaw(), clipboardObject.containsAir()
         );
      }
   }

   static ClipboardObject flip(ClipboardObject clipboardObject, Axis axis) {
      if (clipboardObject == null) {
         return null;
      } else {
         ChunkedBlockRegion chunkedBlockRegion = clipboardObject.blockRegion().flip(axis);
         IntMatrix matrix = new IntMatrix();
         matrix.flip(axis);
         Long2ObjectMap<CompressedBlockEntity> blockEntities = applyMatrixToBlockEntities(matrix, clipboardObject.blockEntities());
         List<CompoundTag> entities = applyMatrixToEntities(matrix, clipboardObject.entities());
         return new ClipboardObject.Anonymous(
            chunkedBlockRegion, blockEntities, entities, clipboardObject.name(), clipboardObject.preferredYaw(), clipboardObject.containsAir()
         );
      }
   }

   private static Long2ObjectMap<CompressedBlockEntity> applyMatrixToBlockEntities(IntMatrix matrix, Long2ObjectMap<CompressedBlockEntity> blockEntities) {
      Long2ObjectMap<CompressedBlockEntity> rotatedBlockEntites = new Long2ObjectOpenHashMap();
      if (blockEntities != null && !blockEntities.isEmpty()) {
         ObjectIterator var3 = blockEntities.long2ObjectEntrySet().iterator();

         while (var3.hasNext()) {
            Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var3.next();
            long pos = entry.getLongKey();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            int nx = matrix.transformX(x, y, z);
            int ny = matrix.transformY(x, y, z);
            int nz = matrix.transformZ(x, y, z);
            long newPos = BlockPos.asLong(nx, ny, nz);
            rotatedBlockEntites.put(newPos, (CompressedBlockEntity)entry.getValue());
         }

         return rotatedBlockEntites;
      } else {
         return rotatedBlockEntites;
      }
   }

   private static List<CompoundTag> applyMatrixToEntities(IntMatrix matrix, List<CompoundTag> entities) {
      List<CompoundTag> rotatedEntities = new ArrayList<>();
      if (entities != null && !entities.isEmpty()) {
         for (CompoundTag entity : entities) {
            CompoundTag copied = entity.copy();
            EntityDataUtils.applyMatrixToEntity(copied, matrix);
            rotatedEntities.add(copied);
         }

         return rotatedEntities;
      } else {
         return rotatedEntities;
      }
   }

   public static class Anonymous implements ClipboardObject {
      private final ChunkedBlockRegion blockRegion;
      private final Long2ObjectMap<CompressedBlockEntity> blockEntities;
      private final List<CompoundTag> entities;
      private final String name;
      private final float preferredYaw;
      private final boolean containsAir;
      private CompletableFuture<NativeImage> future = null;
      public AutoCleaningDynamicTexture texture = null;
      @Nullable
      private final CompoundTag additionalSchematicData;

      public Anonymous(
         ChunkedBlockRegion blockRegion,
         Long2ObjectMap<CompressedBlockEntity> blockEntities,
         List<CompoundTag> entities,
         String name,
         float preferredYaw,
         boolean containsAir
      ) {
         this(blockRegion, blockEntities, entities, name, preferredYaw, containsAir, null);
      }

      public Anonymous(
         ChunkedBlockRegion blockRegion,
         Long2ObjectMap<CompressedBlockEntity> blockEntities,
         List<CompoundTag> entities,
         String name,
         float preferredYaw,
         boolean containsAir,
         @Nullable CompoundTag additionalSchematicData
      ) {
         this.blockRegion = blockRegion;
         this.blockEntities = blockEntities;
         this.entities = entities;
         this.name = name;
         this.preferredYaw = preferredYaw;
         this.additionalSchematicData = additionalSchematicData;
         this.containsAir = containsAir;
      }

      @Override
      public ChunkedBlockRegion blockRegion() {
         return this.blockRegion;
      }

      @Override
      public Long2ObjectMap<CompressedBlockEntity> blockEntities() {
         return this.blockEntities;
      }

      @Override
      public List<CompoundTag> entities() {
         return this.entities;
      }

      @Override
      public String name() {
         return this.name;
      }

      @Override
      public float preferredYaw() {
         return this.preferredYaw;
      }

      @Override
      public boolean containsAir() {
         return this.containsAir;
      }

      @Nullable
      public CompoundTag additionalSchematicData() {
         return this.additionalSchematicData;
      }

      @Override
      public String placementDescription() {
         String formattedCount = NumberFormat.getInstance().format((long)this.blockRegion.count());
         return AxiomI18n.get("axiom.history_description.placed", formattedCount);
      }

      @Override
      public int thumbnailTextureId() {
         if (this.blockRegion.count() >= 16777216) {
            return -1;
         } else {
            if (this.texture == null && this.future == null) {
               BlueprintPreview blueprintPreview = new BlueprintPreview();
               blueprintPreview.setBlockRegion(this.blockRegion);
               blueprintPreview.setYaw(this.preferredYaw, true);
               blueprintPreview.render(960, false, false);
               this.future = blueprintPreview.toNativeImage(96, true);
            }

            if (this.future != null) {
               if (this.future.isDone()) {
                  NativeImage nativeImage = this.future.join();
                  this.future = null;
                  this.texture = new AutoCleaningDynamicTexture(nativeImage);
                  return new AxiomGpuTexture(this.texture.getId()).glId();
               } else {
                  return -1;
               }
            } else {
               return new AxiomGpuTexture(this.texture.getId()).glId();
            }
         }
      }
   }

   public record FromBlueprint(Blueprint blueprint) implements ClipboardObject {
      @Override
      public ChunkedBlockRegion blockRegion() {
         return this.blueprint.blockRegion();
      }

      @Override
      public Long2ObjectMap<CompressedBlockEntity> blockEntities() {
         return this.blueprint.blockEntities();
      }

      @Override
      public List<CompoundTag> entities() {
         return this.blueprint.entities();
      }

      @Override
      public String name() {
         return this.blueprint.header().name();
      }

      @Override
      public float preferredYaw() {
         return this.blueprint.header().thumbnailYaw();
      }

      @Override
      public boolean containsAir() {
         return this.blueprint.header().containsAir();
      }

      @Override
      public String placementDescription() {
         return AxiomI18n.get("axiom.history_description.placed_blueprint", this.name());
      }

      @Override
      public int thumbnailTextureId() {
         DynamicTextureSupplier supplier = this.blueprint.thumbnail();
         if (supplier == null) {
            return 0;
         } else {
            DynamicTexture texture = supplier.get();
            return texture == null ? 0 : new AxiomGpuTexture(texture.getId()).glId();
         }
      }
   }
}
