package com.moulberry.axiom.world_modification;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.world_modification.undo.AdditionalUndoOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record HistoryEntry<T>(
   T forwards, T backwards, BlockPos origin, String description, int bytes, int modifiers, AdditionalUndoOperation additionalUndoOperation
) {
   public static int MODIFIER_PASTE = 1;
   public static int MODIFIER_CUT = 2;
   public static int MODIFIER_SELECT_ON_BACKSTEP = 4;
   public static int MODIFIER_CAN_BE_UNDONE_INGAME = 8;
   private static int MODIFIER_SAVE_WITH_VOID_AIR_AS_EMPTY = 16;
   public static int MODIFIER_KEEP_EXISTING = 65536;

   public HistoryEntry(T forwards, T backwards, BlockPos origin, String description, int modifiers) {
      this(forwards, backwards, origin, description, calculateBytes(forwards, backwards), modifiers, null);
   }

   public HistoryEntry(T forwards, T backwards, BlockPos origin, String description, int modifiers, AdditionalUndoOperation additionalUndoOperation) {
      this(forwards, backwards, origin, description, calculateBytes(forwards, backwards), modifiers, additionalUndoOperation);
   }

   public void save(FriendlyByteBuf out) {
      out.writeBlockPos(this.origin);
      out.writeUtf(this.description);
      out.writeByte(this.modifiers | MODIFIER_SAVE_WITH_VOID_AIR_AS_EMPTY);
      out.writeInt(DFUHelper.DATA_VERSION);
      if (this.forwards instanceof BlockBuffer blockBuffer) {
         out.writeByte(0);
         blockBuffer.saveNBT(out, Blocks.VOID_AIR.defaultBlockState());
         ((BlockBuffer)this.backwards).saveNBT(out, Blocks.VOID_AIR.defaultBlockState());
      } else {
         if (!(this.forwards instanceof BiomeBuffer biomeBuffer)) {
            throw new FaultyImplementationError("Unknown buffer type: " + this.forwards.getClass());
         }

         out.writeByte(1);
         biomeBuffer.save(out);
         ((BiomeBuffer)this.backwards).save(out);
      }
   }

   public static HistoryEntry<BlockOrBiomeBuffer> load(FriendlyByteBuf friendlyByteBuf) {
      BlockPos origin = friendlyByteBuf.readBlockPos();
      String description = friendlyByteBuf.readUtf();
      int modifiers = friendlyByteBuf.readByte();
      int dataVersion = friendlyByteBuf.readInt();
      BlockState emptyState = Blocks.STRUCTURE_VOID.defaultBlockState();
      if ((modifiers & MODIFIER_SAVE_WITH_VOID_AIR_AS_EMPTY) != 0) {
         modifiers &= ~MODIFIER_SAVE_WITH_VOID_AIR_AS_EMPTY;
         emptyState = Blocks.VOID_AIR.defaultBlockState();
      }

      int bufferType = friendlyByteBuf.readByte();
      switch (bufferType) {
         case 0: {
            BlockBuffer forwards = BlockBuffer.loadNBT(friendlyByteBuf, dataVersion, emptyState);
            BlockBuffer backwards = BlockBuffer.loadNBT(friendlyByteBuf, dataVersion, emptyState);
            return new HistoryEntry<>(forwards, backwards, origin, description, modifiers);
         }
         case 1: {
            BiomeBuffer forwards = BiomeBuffer.load(friendlyByteBuf);
            BiomeBuffer backwards = BiomeBuffer.load(friendlyByteBuf);
            return new HistoryEntry<>(forwards, backwards, origin, description, modifiers);
         }
         default:
            throw new FaultyImplementationError("Unknown buffer type: " + bufferType);
      }
   }

   public boolean hasModifier(int modifier) {
      return (this.modifiers & modifier) != 0;
   }

   private static <T> int calculateBytes(T forwards, T backwards) {
      return forwards instanceof BlockBuffer blockBuffer ? blockBuffer.estimateSizeInRAM() + ((BlockBuffer)backwards).estimateSizeInRAM() : 0;
   }
}
