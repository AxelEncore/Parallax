package com.moulberry.axiom.packets;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.utils.NetworkHelper;
import java.util.List;
import java.util.Map;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class AxiomClientboundRegisterCustomBlockV2 implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:register_custom_block_v2");
   private final ResourceLocation id;
   private final String translationKey;
   private final List<Property<?>> properties;
   private final List<BlockState> blockStates;
   @Nullable
   private final ItemStack pickBlockItemStack;
   private final boolean sendServerPickBlockIfPossible;
   private final boolean preventRightClickInteraction;
   private final boolean preventShapeUpdates;
   private final List<ResourceLocation> placementLogics;
   private final boolean automaticRotationAndMirroring;
   private final Map<BlockState, BlockState> rotateYMappings;
   private final Map<BlockState, BlockState> flipXMappings;
   private final Map<BlockState, BlockState> flipYMappings;
   private final Map<BlockState, BlockState> flipZMappings;

   public AxiomClientboundRegisterCustomBlockV2(
      ResourceLocation id,
      String translationKey,
      List<Property<?>> properties,
      List<BlockState> blockStates,
      @Nullable ItemStack pickBlockItemStack,
      boolean sendServerPickBlockIfPossible,
      boolean preventRightClickInteraction,
      boolean preventShapeUpdates,
      List<ResourceLocation> placementLogics,
      boolean automaticRotationAndMirroring,
      Map<BlockState, BlockState> rotateYMappings,
      Map<BlockState, BlockState> flipXMappings,
      Map<BlockState, BlockState> flipYMappings,
      Map<BlockState, BlockState> flipZMappings
   ) {
      this.id = id;
      this.translationKey = translationKey;
      this.properties = properties;
      this.blockStates = blockStates;
      this.pickBlockItemStack = pickBlockItemStack;
      this.sendServerPickBlockIfPossible = sendServerPickBlockIfPossible;
      this.preventRightClickInteraction = preventRightClickInteraction;
      this.preventShapeUpdates = preventShapeUpdates;
      this.placementLogics = placementLogics;
      this.automaticRotationAndMirroring = automaticRotationAndMirroring;
      this.rotateYMappings = rotateYMappings;
      this.flipXMappings = flipXMappings;
      this.flipYMappings = flipYMappings;
      this.flipZMappings = flipZMappings;
   }

   public AxiomClientboundRegisterCustomBlockV2(FriendlyByteBuf friendlyByteBuf) {
      this.id = friendlyByteBuf.readResourceLocation();
      this.translationKey = friendlyByteBuf.readUtf();
      this.properties = friendlyByteBuf.readList(AxiomClientboundCustomBlocks::readProperty);
      this.blockStates = friendlyByteBuf.readList(NetworkHelper::readBlockStateByIdOrString);
      if (friendlyByteBuf.readBoolean()) {
         this.pickBlockItemStack = NetworkHelper.readItemStack(friendlyByteBuf);
      } else {
         this.pickBlockItemStack = null;
      }

      this.sendServerPickBlockIfPossible = friendlyByteBuf.readBoolean();
      this.preventRightClickInteraction = friendlyByteBuf.readBoolean();
      this.preventShapeUpdates = friendlyByteBuf.readBoolean();
      this.placementLogics = friendlyByteBuf.readList(FriendlyByteBuf::readResourceLocation);
      this.automaticRotationAndMirroring = friendlyByteBuf.readBoolean();
      this.rotateYMappings = friendlyByteBuf.readMap(NetworkHelper::readBlockStateByIdOrString, NetworkHelper::readBlockStateByIdOrString);
      this.flipXMappings = friendlyByteBuf.readMap(NetworkHelper::readBlockStateByIdOrString, NetworkHelper::readBlockStateByIdOrString);
      this.flipYMappings = friendlyByteBuf.readMap(NetworkHelper::readBlockStateByIdOrString, NetworkHelper::readBlockStateByIdOrString);
      this.flipZMappings = friendlyByteBuf.readMap(NetworkHelper::readBlockStateByIdOrString, NetworkHelper::readBlockStateByIdOrString);
      int version = friendlyByteBuf.readVarInt();
      if (version > 0) {
         friendlyByteBuf.readerIndex(friendlyByteBuf.writerIndex());
      }
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeResourceLocation(this.id);
      friendlyByteBuf.writeUtf(this.translationKey);
      friendlyByteBuf.writeCollection(this.properties, AxiomClientboundCustomBlocks::writeProperty);
      friendlyByteBuf.writeCollection(this.blockStates, NetworkHelper::writeBlockState);
      if (this.pickBlockItemStack != null) {
         friendlyByteBuf.writeBoolean(true);
         NetworkHelper.writeItemStack(friendlyByteBuf, this.pickBlockItemStack);
      } else {
         friendlyByteBuf.writeBoolean(false);
      }

      friendlyByteBuf.writeBoolean(this.sendServerPickBlockIfPossible);
      friendlyByteBuf.writeBoolean(this.preventRightClickInteraction);
      friendlyByteBuf.writeBoolean(this.preventShapeUpdates);
      friendlyByteBuf.writeCollection(this.placementLogics, FriendlyByteBuf::writeResourceLocation);
      friendlyByteBuf.writeBoolean(this.automaticRotationAndMirroring);
      friendlyByteBuf.writeMap(this.rotateYMappings, NetworkHelper::writeBlockState, NetworkHelper::writeBlockState);
      friendlyByteBuf.writeMap(this.flipXMappings, NetworkHelper::writeBlockState, NetworkHelper::writeBlockState);
      friendlyByteBuf.writeMap(this.flipYMappings, NetworkHelper::writeBlockState, NetworkHelper::writeBlockState);
      friendlyByteBuf.writeMap(this.flipZMappings, NetworkHelper::writeBlockState, NetworkHelper::writeBlockState);
      friendlyByteBuf.writeVarInt(0);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      try {
         ServerCustomBlocks.registerCustomBlock(
            this.id,
            this.translationKey,
            this.properties,
            this.blockStates,
            this.pickBlockItemStack,
            this.sendServerPickBlockIfPossible,
            this.preventRightClickInteraction,
            this.preventShapeUpdates,
            this.placementLogics,
            this.automaticRotationAndMirroring,
            this.rotateYMappings,
            this.flipXMappings,
            this.flipYMappings,
            this.flipZMappings
         );
      } catch (Exception var4) {
         if (AxiomPlatform.isDevelopment()) {
            throw var4;
         }

         Axiom.LOGGER.error("Failed to register custom block", var4);
      }
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundRegisterCustomBlockV2::new);
   }
}
