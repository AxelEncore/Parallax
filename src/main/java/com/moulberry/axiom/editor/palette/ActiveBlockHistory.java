package com.moulberry.axiom.editor.palette;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.level.block.Blocks;

public class ActiveBlockHistory {
   private final List<CustomBlockState> blocks = new ArrayList<>();

   public ActiveBlockHistory() {
      this.blocks.add((CustomBlockState)Blocks.STONE.defaultBlockState());
   }

   public void clear() {
      this.blocks.clear();
      this.blocks.add((CustomBlockState)Blocks.STONE.defaultBlockState());
   }

   public List<CustomBlockState> getRecentlyUsed() {
      return Collections.unmodifiableList(this.blocks);
   }

   public CustomBlockState getActive() {
      return this.blocks.get(0);
   }

   public void setActive(CustomBlockState newBlock) {
      this.blocks.removeIf(block -> block == newBlock);
      if (this.blocks.size() > 0 && this.blocks.get(0).getCustomBlock() == newBlock.getCustomBlock()) {
         this.blocks.remove(0);
      }

      while (this.blocks.size() > 32) {
         this.blocks.remove(this.blocks.size() - 1);
      }

      this.blocks.add(0, newBlock);
   }
}
