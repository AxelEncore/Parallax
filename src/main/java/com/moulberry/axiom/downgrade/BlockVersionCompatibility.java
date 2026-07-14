package com.moulberry.axiom.downgrade;

import com.moulberry.axiom.utils.DFUHelper;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class BlockVersionCompatibility {
   private static DowngradeVersion latestVersion;
   private static DowngradeVersion compatibilityLevel;
   private static Set<Block> currentInvalidBlocks;

   public static boolean isCompatible(Block block) {
      return compatibilityLevel == latestVersion ? true : !currentInvalidBlocks.contains(block);
   }

   @Nullable
   public static DowngradeVersion getCompatibilityLevel() {
      return compatibilityLevel;
   }

   public static void setCompatibilityLevel(DowngradeVersion downgradeVersion) {
      compatibilityLevel = downgradeVersion;
      Downgrader downgrader = new Downgrader(downgradeVersion);
      currentInvalidBlocks = new HashSet<>();

      for (Block block : BuiltInRegistries.BLOCK) {
         String serialized = BlockStateParser.serialize(block.defaultBlockState());
         String downgraded = downgrader.downgrade(serialized);
         if (downgraded == null || downgraded.startsWith("?")) {
            currentInvalidBlocks.add(block);
         }
      }
   }

   static {
      DowngradeVersion lastVersion = DowngradeVersion.values()[DowngradeVersion.values().length - 1];
      if (lastVersion.getMaxDataVersion() < DFUHelper.DATA_VERSION) {
         latestVersion = null;
      } else {
         latestVersion = lastVersion;
      }

      compatibilityLevel = latestVersion;
      currentInvalidBlocks = Set.of();
   }
}
