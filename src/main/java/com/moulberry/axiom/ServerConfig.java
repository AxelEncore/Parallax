package com.moulberry.axiom;

import com.moulberry.axiom.utils.NetworkHelper;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;

public record ServerConfig(int setBufferMaxSize, int blueprintVersion, Set<Block> blocksWithCustomData, Set<Block> ignoreRotationSet) {
   public static ServerConfig createDefault() {
      return new ServerConfig(1048576, 2, Set.of(), Set.of());
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeInt(this.setBufferMaxSize);
      buf.writeVarInt(this.blueprintVersion);
      buf.writeCollection(this.blocksWithCustomData, NetworkHelper::writeBlock);
      buf.writeCollection(this.ignoreRotationSet, NetworkHelper::writeBlock);
   }

   public static ServerConfig read(FriendlyByteBuf buf) {
      int setBufferMaxSize = buf.readInt();
      int blueprintVersion = buf.readVarInt();
      Set<Block> blockWithCustomData = (Set<Block>)buf.readCollection(HashSet::new, NetworkHelper::readBlock);
      Set<Block> ignoreRotationSet = (Set<Block>)buf.readCollection(HashSet::new, NetworkHelper::readBlock);
      return new ServerConfig(setBufferMaxSize, blueprintVersion, blockWithCustomData, ignoreRotationSet);
   }
}
