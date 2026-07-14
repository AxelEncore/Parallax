package com.moulberry.axiom.mixin.block_attributes;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.StaticValues;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LightBlock.class})
public class MixinLightBlock extends Block {
   @Shadow
   @Final
   public static IntegerProperty LEVEL;
   @Unique
   private static final VoxelShape SHAPE = Block.box(2.0, 2.0, 2.0, 14.0, 14.0, 14.0);

   public MixinLightBlock(Properties properties) {
      super(properties);
   }

   @ModifyReturnValue(
      method = {"getRenderShape"},
      at = {@At("RETURN")}
   )
   public RenderShape getShape(RenderShape voxelShape) {
      return StaticValues.gameHasTicked && AxiomClient.isAxiomActive() && Axiom.configuration.blockAttributes.showLightBlocks ? RenderShape.MODEL : voxelShape;
   }

   @ModifyReturnValue(
      method = {"getShape"},
      at = {@At("RETURN")}
   )
   public VoxelShape getShape(VoxelShape voxelShape, @Local(argsOnly = true) CollisionContext context) {
      return StaticValues.gameHasTicked
            && AxiomClient.isAxiomActive()
            && Axiom.configuration.blockAttributes.showLightBlocks
            && context instanceof EntityCollisionContext entityCollisionContext
            && entityCollisionContext.getEntity() instanceof LocalPlayer
         ? SHAPE
         : voxelShape;
   }

   public boolean skipRendering(BlockState blockState, BlockState blockState2, Direction direction) {
      return blockState2.is(this) && ((Integer)blockState2.getValue(LEVEL)).equals(blockState.getValue(LEVEL));
   }
}
