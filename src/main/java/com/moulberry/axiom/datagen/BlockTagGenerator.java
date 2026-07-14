package com.moulberry.axiom.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.BlockTagProvider;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BlockTagGenerator extends BlockTagProvider {
   private static final TagKey<Block> EXISTING = TagKey.create(Registries.BLOCK, ResourceLocation.parse("axiom:existing"));
   private static final TagKey<Block> SOLID = TagKey.create(Registries.BLOCK, ResourceLocation.parse("axiom:solid"));
   private static final TagKey<Block> NONSOLID = TagKey.create(Registries.BLOCK, ResourceLocation.parse("axiom:nonsolid"));
   private static final TagKey<Block> CAN_BE_WATERLOGGED = TagKey.create(Registries.BLOCK, ResourceLocation.parse("axiom:can_be_waterlogged"));
   private static final TagKey<Block> PLANTS = TagKey.create(Registries.BLOCK, ResourceLocation.parse("axiom:plants"));
   private static final TagKey<Block> UNDERWATER_PLANTS = TagKey.create(Registries.BLOCK, ResourceLocation.parse("axiom:underwater_plants"));
   private static final TagKey<Block> FALLING_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.parse("axiom:falling_blocks"));
   private static final TagKey<Block> STAINED_GLASS = TagKey.create(Registries.BLOCK, ResourceLocation.parse("axiom:stained_glass"));

   public BlockTagGenerator(FabricDataOutput output, CompletableFuture<Provider> registriesFuture) {
      super(output, registriesFuture);
   }

   protected void addTags(Provider arg) {
      AxiomTagCreator<Block> tagCreator = tag -> {
         final FabricTagProvider<Block>.FabricTagBuilder builder = this.getOrCreateTagBuilder(tag);
         return new AxiomTagAppender<Block>() {
            @Override
            public void addTag(TagKey<Block> tag) {
               builder.addTag(tag);
            }

            public void add(Block tag) {
               builder.add(tag);
            }
         };
      };
      MaterialBlockTagGenerator.addTags(arg, tagCreator);
      ColourBlockTagGenerator.addTags(arg, tagCreator);
      AxiomTagAppender<Block> existing = tagCreator.create(EXISTING);
      AxiomTagAppender<Block> solid = tagCreator.create(SOLID);
      AxiomTagAppender<Block> nonsolid = tagCreator.create(NONSOLID);
      AxiomTagAppender<Block> canBeWaterlogged = tagCreator.create(CAN_BE_WATERLOGGED);
      AxiomTagAppender<Block> plants = tagCreator.create(PLANTS);
      AxiomTagAppender<Block> underwaterPlants = tagCreator.create(UNDERWATER_PLANTS);
      AxiomTagAppender<Block> fallingBlocks = tagCreator.create(FALLING_BLOCKS);
      AxiomTagAppender<Block> stainedGlass = tagCreator.create(STAINED_GLASS);

      for (Block block : BuiltInRegistries.BLOCK) {
         BlockState defaultState = block.defaultBlockState();
         if (!defaultState.isAir()) {
            existing.add(block);
            if (defaultState.isSolid() && !block.isPossibleToRespawnInThis(defaultState)) {
               solid.add(block);
            } else {
               nonsolid.add(block);
            }

            if (defaultState.hasProperty(BlockStateProperties.WATERLOGGED)) {
               canBeWaterlogged.add(block);
            }

            if (block instanceof BushBlock || block instanceof GrowingPlantBlock || block instanceof ChorusPlantBlock) {
               plants.add(block);
               if (!defaultState.hasProperty(BlockStateProperties.WATERLOGGED) && !defaultState.getFluidState().isEmpty()) {
                  underwaterPlants.add(block);
               }
            }

            if (block instanceof StainedGlassBlock || block instanceof StainedGlassPaneBlock) {
               stainedGlass.add(block);
            }

            if (!(block instanceof FallingBlock) && !(block instanceof BrushableBlock)) {
               if (block instanceof Fallable && !(block instanceof PointedDripstoneBlock)) {
                  throw new RuntimeException(block + " is fallable but isn't included... care to explain?");
               }
            } else {
               fallingBlocks.add(block);
            }
         }
      }
   }
}
