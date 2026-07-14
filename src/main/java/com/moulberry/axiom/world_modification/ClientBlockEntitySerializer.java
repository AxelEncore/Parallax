package com.moulberry.axiom.world_modification;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.restrictions.AxiomPermission;
import java.util.Set;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public class ClientBlockEntitySerializer {
   private static final CompoundTag EMPTY = new CompoundTag();
   private static final Set<BlockEntityType<?>> NO_DATA = Set.of(
      BlockEntityType.ENDER_CHEST, BlockEntityType.END_PORTAL, BlockEntityType.DAYLIGHT_DETECTOR, BlockEntityType.BED, BlockEntityType.BELL
   );
   private static final Set<BlockEntityType<?>> CLIENT_DATA = Set.of(
      BlockEntityType.SIGN,
      BlockEntityType.HANGING_SIGN,
      BlockEntityType.BEACON,
      BlockEntityType.SKULL,
      BlockEntityType.BANNER,
      BlockEntityType.STRUCTURE_BLOCK,
      BlockEntityType.END_GATEWAY,
      BlockEntityType.CONDUIT,
      BlockEntityType.JIGSAW,
      BlockEntityType.BRUSHABLE_BLOCK
   );

   @Nullable
   public static CompoundTag serialize(BlockEntity blockEntity, Provider provider) {
      BlockEntityType<?> type = blockEntity.getType();
      if (NO_DATA.contains(type)) {
         return EMPTY;
      } else {
         return !CLIENT_DATA.contains(type)
               && AxiomClient.hasPermission(AxiomPermission.CHUNK_REQUESTBLOCKENTITY)
               && ClientEvents.serverSupportsProtocol(SupportedProtocol.REQUEST_CHUNK)
            ? null
            : blockEntity.saveWithoutMetadata(provider);
      }
   }
}
