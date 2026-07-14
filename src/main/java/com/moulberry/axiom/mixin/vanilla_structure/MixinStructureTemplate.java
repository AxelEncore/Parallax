package com.moulberry.axiom.mixin.vanilla_structure;

import com.moulberry.axiom.block_maps.BlockEntityMap;
import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.hooks.StructureTemplateExt;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.Palette;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureEntityInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({StructureTemplate.class})
public abstract class MixinStructureTemplate implements StructureTemplateExt {
   @Shadow
   @Final
   private List<Palette> palettes;
   @Shadow
   @Final
   private List<StructureEntityInfo> entityInfoList;
   @Shadow
   private Vec3i size;

   @Shadow
   private static void addToLists(
      StructureBlockInfo structureBlockInfo, List<StructureBlockInfo> list, List<StructureBlockInfo> list2, List<StructureBlockInfo> list3
   ) {
   }

   @Shadow
   private static List<StructureBlockInfo> buildInfoList(List<StructureBlockInfo> list, List<StructureBlockInfo> list2, List<StructureBlockInfo> list3) {
      return null;
   }

   @Override
   public void axiom$fillFromClipboard(ClipboardObject clipboardObject) {
      ArrayList<StructureBlockInfo> list1 = new ArrayList<>();
      ArrayList<StructureBlockInfo> list2 = new ArrayList<>();
      ArrayList<StructureBlockInfo> list3 = new ArrayList<>();
      BlockPos min = clipboardObject.blockRegion().min();
      BlockPos max = clipboardObject.blockRegion().max();
      if (min != null && max != null) {
         this.size = new Vec3i(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1);
         clipboardObject.blockRegion().forEachEntry((x, y, z, blockState) -> {
            BlockPos relative = new BlockPos(x - min.getX(), y - min.getY(), z - min.getZ());
            CompressedBlockEntity compressedBlockEntity = (CompressedBlockEntity)clipboardObject.blockEntities().get(BlockPos.asLong(x, y, z));
            StructureBlockInfo structureBlockInfo;
            if (compressedBlockEntity != null) {
               CompoundTag blockEntityTag = compressedBlockEntity.decompress();
               BlockEntityType<?> blockEntityType = BlockEntityMap.get(blockState);
               if (blockEntityType != null) {
                  ResourceLocation resourceLocation = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntityType);
                  if (resourceLocation != null) {
                     blockEntityTag.putString("id", resourceLocation.toString());
                  }
               }

               structureBlockInfo = new StructureBlockInfo(relative, blockState, blockEntityTag);
            } else {
               structureBlockInfo = new StructureBlockInfo(relative, blockState, null);
            }

            addToLists(structureBlockInfo, list1, list2, list3);
         });
      }

      List<StructureBlockInfo> combined = buildInfoList(list1, list2, list3);
      this.palettes.clear();
      this.palettes.add(new Palette(combined));
      this.entityInfoList.clear();
   }
}
