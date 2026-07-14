package com.moulberry.axiom.mixin;

import com.moulberry.axiom.hooks.ThreadedLevelLightEngineExt;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.ThreadedLevelLightEngine.TaskType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({ThreadedLevelLightEngine.class})
public abstract class MixinThreadedLevelLightEngine extends LevelLightEngine implements ThreadedLevelLightEngineExt {
   public MixinThreadedLevelLightEngine(LightChunkGetter lightChunkGetter, boolean bl, boolean bl2) {
      super(lightChunkGetter, bl, bl2);
   }

   @Shadow
   protected abstract void addTask(int var1, int var2, TaskType var3, Runnable var4);

   @Shadow
   public abstract void updateSectionStatus(SectionPos var1, boolean var2);

   @Shadow
   public abstract CompletableFuture<ChunkAccess> lightChunk(ChunkAccess var1, boolean var2);

   @Shadow
   protected abstract void runUpdate();

   @Override
   public void axiom$checkSectionBlocks(int cx, int cy, int cz, short[] array) {
      this.addTask(cx, cz, TaskType.PRE_UPDATE, () -> {
         MutableBlockPos mutable = new MutableBlockPos();
         int baseX = cx * 16;
         int baseY = cy * 16;
         int baseZ = cz * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               short v = array[index++];
               if (v != 0) {
                  for (int x = 0; x < 16; x++) {
                     if ((v & 1 << x) != 0) {
                        super.checkBlock(mutable.set(baseX + x, baseY + y, baseZ + z));
                     }
                  }
               }
            }
         }
      });
   }

   @Override
   public CompletableFuture<?> axiom$waitForPendingTasks(int cx, int cz) {
      return CompletableFuture.runAsync(() -> {}, runnable -> this.addTask(cx, cz, TaskType.POST_UPDATE, runnable));
   }
}
