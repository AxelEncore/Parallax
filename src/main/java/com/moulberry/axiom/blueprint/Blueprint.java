package com.moulberry.axiom.blueprint;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.DynamicTextureSupplier;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;

public class Blueprint implements AutoCloseable {
   private final BlueprintHeader header;
   private final DynamicTextureSupplier thumbnail;
   private final ChunkedBlockRegion blockRegion;
   private final Long2ObjectMap<CompressedBlockEntity> blockEntities;
   private final List<CompoundTag> entities;

   public Blueprint(
      BlueprintHeader header,
      DynamicTextureSupplier thumbnail,
      ChunkedBlockRegion blockRegion,
      Long2ObjectMap<CompressedBlockEntity> blockEntities,
      List<CompoundTag> entities
   ) {
      this.header = header;
      this.thumbnail = thumbnail;
      this.blockRegion = blockRegion;
      this.blockEntities = blockEntities;
      this.entities = entities;
   }

   @Override
   public void close() {
      EditorUI.deferredClose(this.thumbnail);
   }

   public BlueprintHeader header() {
      return this.header;
   }

   public DynamicTextureSupplier thumbnail() {
      return this.thumbnail;
   }

   public ChunkedBlockRegion blockRegion() {
      return this.blockRegion;
   }

   public List<CompoundTag> entities() {
      return this.entities;
   }

   public Long2ObjectMap<CompressedBlockEntity> blockEntities() {
      return this.blockEntities;
   }
}
