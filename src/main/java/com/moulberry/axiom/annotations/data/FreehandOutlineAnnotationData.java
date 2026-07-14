package com.moulberry.axiom.annotations.data;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record FreehandOutlineAnnotationData(BlockPos start, byte[] offsets, int offsetCount, int colour) implements OutlineAnnotationData {
   public FreehandOutlineAnnotationData(BlockPos start, byte[] offsets, int offsetCount, int colour) {
      colour = 0xFF000000 | colour;
      this.start = start;
      this.offsets = offsets;
      this.offsetCount = offsetCount;
      this.colour = colour;
   }

   @Override
   public void setPosition(Vector3f position) {
   }

   @Override
   public void setRotation(Quaternionf rotation) {
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeByte(3);
      friendlyByteBuf.writeVarInt(this.start.getX());
      friendlyByteBuf.writeVarInt(this.start.getY());
      friendlyByteBuf.writeVarInt(this.start.getZ());
      friendlyByteBuf.writeVarInt(this.offsetCount);
      friendlyByteBuf.writeInt(this.colour);
      friendlyByteBuf.writeByteArray(this.offsets);
   }

   public static FreehandOutlineAnnotationData read(FriendlyByteBuf friendlyByteBuf) {
      int x = friendlyByteBuf.readVarInt();
      int y = friendlyByteBuf.readVarInt();
      int z = friendlyByteBuf.readVarInt();
      int offsetCount = friendlyByteBuf.readVarInt();
      int colour = friendlyByteBuf.readInt();
      byte[] offsets = friendlyByteBuf.readByteArray();
      return new FreehandOutlineAnnotationData(new BlockPos(x, y, z), offsets, offsetCount, colour);
   }

   @Override
   public int getColour() {
      return this.colour;
   }

   @Override
   public void iteratePositions(TriIntConsumer consumer) {
      MutableBlockPos position = new MutableBlockPos(this.start.getX(), this.start.getY(), this.start.getZ());
      PositionSet visited = new PositionSet();
      if (visited.add(position.getX(), position.getY(), position.getZ())) {
         consumer.accept(position.getX(), position.getY(), position.getZ());
      }

      for (int i = 0; i < this.offsetCount; i += 3) {
         int offset = this.offsets[i / 3] & 255;
         position.move(Direction.from3DDataValue(offset % 6));
         if (visited.add(position.getX(), position.getY(), position.getZ())) {
            consumer.accept(position.getX(), position.getY(), position.getZ());
         }

         if (i + 1 < this.offsetCount) {
            position.move(Direction.from3DDataValue(offset / 6 % 6));
            if (visited.add(position.getX(), position.getY(), position.getZ())) {
               consumer.accept(position.getX(), position.getY(), position.getZ());
            }
         }

         if (i + 2 < this.offsetCount) {
            position.move(Direction.from3DDataValue(offset / 36 % 6));
            if (visited.add(position.getX(), position.getY(), position.getZ())) {
               consumer.accept(position.getX(), position.getY(), position.getZ());
            }
         }
      }
   }
}
