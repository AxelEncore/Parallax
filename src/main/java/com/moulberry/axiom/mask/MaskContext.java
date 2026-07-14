package com.moulberry.axiom.mask;

import com.moulberry.axiom.AsyncChunkProvider;
import com.moulberry.axiom.collections.ChunkedPredicateDistanceField;
import com.moulberry.axiom.collections.Position2dToIntMap;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.funcinterfaces.TriIntFunction;
import com.moulberry.axiom.mask.elements.LuaMaskElement;
import com.moulberry.axiom.utils.BlockCondition;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

public class MaskContext {
   public static int frame = 0;
   private int lastFrame = 0;
   private final BlockState[] blocks = new BlockState[27];
   private Holder<Biome> biome = null;
   private Vec3 angleVector = null;
   private final MutableBlockPos mutableBlockPos = new MutableBlockPos();
   private final Function<BlockPos, BlockState> blockGetter;
   private final TriIntFunction<Holder<Biome>> biomeGetter;
   private final ChunkedPredicateDistanceField.SectionProvider sectionGetter;
   private final Position2dToIntMap heightmap = new Position2dToIntMap(Integer.MIN_VALUE);
   private final int maximumY;
   private final Map<BlockCondition, ChunkedPredicateDistanceField> distanceFieldMap = new HashMap<>();
   private boolean triedToCompileLuaScript = false;
   private LuaFunction loadedScript = null;
   private Globals luaGlobals = null;

   public MaskContext(Level level) {
      this.blockGetter = level::getBlockState;
      this.biomeGetter = level::getNoiseBiome;
      this.sectionGetter = (cx, cy, cz) -> {
         LevelChunk chunk = (LevelChunk)level.getChunk(cx, cz, ChunkStatus.FULL, false);
         if (chunk == null) {
            return null;
         } else {
            int sectionY = chunk.getSectionIndexFromSectionY(cy);
            if (sectionY >= 0 && sectionY < chunk.getSectionsCount()) {
               LevelChunkSection section = chunk.getSection(sectionY);
               return section.getStates();
            } else {
               return null;
            }
         }
      };
      this.maximumY = level.getMaxBuildHeight() - 1;
   }

   public MaskContext(AsyncChunkProvider provider) {
      this.blockGetter = provider::getBlockState;
      this.biomeGetter = provider::getBiome;
      this.sectionGetter = provider::getSection;
      this.maximumY = provider.getMaxY();
   }

   public MaskContext reset() {
      if (this.lastFrame != frame && this.luaGlobals != null) {
         this.lastFrame = frame;
         LuaHelper.updateExtraVariables(this.luaGlobals);
      }

      Arrays.fill(this.blocks, null);
      this.biome = null;
      this.angleVector = null;
      return this;
   }

   public ChunkedPredicateDistanceField getPredicateDistanceField(BlockCondition blockCondition) {
      return this.distanceFieldMap.computeIfAbsent(blockCondition, cond -> new ChunkedPredicateDistanceField(this.sectionGetter, blockCondition));
   }

   public boolean runScript(LuaMaskElement luaMaskElement, int x, int y, int z) {
      if (!this.triedToCompileLuaScript) {
         this.triedToCompileLuaScript = true;
         String script = luaMaskElement.getScript();
         this.luaGlobals = LuaHelper.createSandboxed();
         LuaHelper.initializeMask(this.luaGlobals, this, x, y, z);

         try {
            this.loadedScript = LuaHelper.compile(script, this.luaGlobals);
            ToolMaskWindow.luaExecutionError = null;
         } catch (LuaError var10) {
            String message = var10.getMessage();
            String[] splitMessage = message.split(":");
            ToolMaskWindow.luaExecutionError = splitMessage[splitMessage.length - 1];
         }
      }

      if (this.loadedScript != null) {
         LuaHelper.setPosition(this.luaGlobals, x, y, z);

         try {
            LuaValue value = this.loadedScript.call();
            if (value.isboolean()) {
               return value.toboolean();
            }

            ToolMaskWindow.luaExecutionError = "expected boolean output, got " + value.typename() + " instead";
            this.loadedScript = null;
         } catch (LuaError var9) {
            String message = var9.getMessage();
            String[] splitMessage = message.split(":");
            ToolMaskWindow.luaExecutionError = splitMessage[splitMessage.length - 1];
            this.loadedScript = null;
         }
      }

      return false;
   }

   public int getHighestBlock(int x, int z) {
      int currentValue = this.heightmap.get(x, z);
      if (currentValue != Integer.MIN_VALUE) {
         return currentValue;
      } else {
         int y = this.maximumY;

         while (true) {
            BlockState blockState = this.blockGetter.apply(this.mutableBlockPos.set(x, y, z));
            if (blockState.getBlock() == Blocks.VOID_AIR || blockState.blocksMotion()) {
               this.heightmap.put(x, z, y);
               return y;
            }

            y--;
         }
      }
   }

   public Vec3 getAngleVector(int x, int y, int z) {
      if (this.angleVector != null) {
         return this.angleVector;
      } else {
         int dirX = 0;
         int dirY = 0;
         int dirZ = 0;

         for (int xo = -2; xo <= 2; xo++) {
            for (int yo = -2; yo <= 2; yo++) {
               for (int zo = -2; zo <= 2; zo++) {
                  BlockState block = this.blockGetter.apply(this.mutableBlockPos.set(x + xo, y + yo, z + zo));
                  if (block.blocksMotion()) {
                     dirX -= xo;
                     dirY -= yo;
                     dirZ -= zo;
                  }
               }
            }
         }

         int dirSq = dirX * dirX + dirY * dirY + dirZ * dirZ;
         if (dirSq == 0) {
            this.angleVector = Vec3.ZERO;
            return this.angleVector;
         } else {
            double scale = 1.0 / Math.sqrt(dirSq);
            this.angleVector = new Vec3(dirX * scale, dirY * scale, dirZ * scale);
            return this.angleVector;
         }
      }
   }

   public Holder<Biome> getBiomeAt(int x, int y, int z) {
      if (this.biome != null) {
         return this.biome;
      } else {
         this.biome = this.biomeGetter.get(x >> 2, y >> 2, z >> 2);
         return this.biome;
      }
   }

   public BlockState getBlockStateAt(int x, int y, int z) {
      return this.blockGetter.apply(this.mutableBlockPos.set(x, y, z));
   }

   public BlockState getBlockState(int x, int y, int z) {
      BlockState blockState = this.blocks[13];
      if (blockState == null) {
         this.blocks[13] = blockState = this.blockGetter.apply(this.mutableBlockPos.set(x, y, z));
      }

      return blockState;
   }

   public BlockState getBlockState(int x, int y, int z, int xo, int yo, int zo) {
      int index = 13 + xo * 9 + yo * 3 + zo;
      BlockState blockState = this.blocks[index];
      if (blockState == null) {
         this.blocks[index] = blockState = this.blockGetter.apply(this.mutableBlockPos.set(x + xo, y + yo, z + zo));
      }

      return blockState;
   }
}
