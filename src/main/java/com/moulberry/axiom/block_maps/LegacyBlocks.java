package com.moulberry.axiom.block_maps;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moulberry.axiom.downgrade.Downgrader;
import com.moulberry.axiom.utils.DFUHelper;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class LegacyBlocks {
   private static BlockState[] LEGACY_BLOCKS = null;

   public static BlockState[] getLegacyBlocks() {
      if (LEGACY_BLOCKS == null) {
         LEGACY_BLOCKS = new BlockState[4095];

         try {
            URL url = Downgrader.class.getClassLoader().getResource("legacy.json");
            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);

            try (InputStream is = connection.getInputStream()) {
               byte[] bytes = is.readAllBytes();
               JsonObject jsonObject = (JsonObject)new Gson().fromJson(new String(bytes), JsonObject.class);

               for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                  String key = entry.getKey();
                  String[] split = key.split(":");
                  int blockId = Integer.parseInt(split[0]);
                  int blockData = 0;
                  if (split.length >= 2) {
                     blockData = Integer.parseInt(split[1]);
                  }

                  String value = entry.getValue().getAsString();
                  CompoundTag blockTag = DFUHelper.createBlockTag(value);
                  BlockState blockState = DFUHelper.updateBlockState(blockTag, 1631).result().orElse(Blocks.AIR.defaultBlockState());
                  LEGACY_BLOCKS[blockId * 16 + blockData] = blockState;
               }
            }
         } catch (Exception var16) {
            throw new RuntimeException(var16);
         }
      }

      return LEGACY_BLOCKS;
   }
}
