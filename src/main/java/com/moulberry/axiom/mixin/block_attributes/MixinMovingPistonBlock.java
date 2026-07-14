package com.moulberry.axiom.mixin.block_attributes;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.StaticValues;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({MovingPistonBlock.class})
public abstract class MixinMovingPistonBlock extends BaseEntityBlock {
   protected MixinMovingPistonBlock(Properties properties) {
      super(properties);
   }

   public RenderShape getRenderShape(BlockState blockState) {
      return StaticValues.gameHasTicked && AxiomClient.isAxiomActive() && Axiom.configuration.blockAttributes.showMovingPistonBlocks
         ? RenderShape.MODEL
         : super.getRenderShape(blockState);
   }
}
