package com.moulberry.axiom.annotations.data;

import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record LinesOutlineAnnotationData(long[] positions, int colour) implements OutlineAnnotationData {
   public LinesOutlineAnnotationData(long[] positions, int colour) {
      colour = 0xFF000000 | colour;
      this.positions = positions;
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
      friendlyByteBuf.writeByte(4);
      friendlyByteBuf.writeVarInt(this.positions.length);

      for (long position : this.positions) {
         friendlyByteBuf.writeLong(position);
      }

      friendlyByteBuf.writeInt(this.colour);
   }

   public static LinesOutlineAnnotationData read(FriendlyByteBuf friendlyByteBuf) {
      int positionCount = friendlyByteBuf.readVarInt();
      long[] positions = new long[positionCount];

      for (int i = 0; i < positionCount; i++) {
         positions[i] = friendlyByteBuf.readLong();
      }

      int colour = friendlyByteBuf.readInt();
      return new LinesOutlineAnnotationData(positions, colour);
   }

   @Override
   public int getColour() {
      return this.colour;
   }

   @Override
   public void iteratePositions(TriIntConsumer consumer) {
      MutableBlockPos from = new MutableBlockPos();
      MutableBlockPos to = null;

      for (long pos : this.positions) {
         int x = BlockPos.getX(pos);
         int y = BlockPos.getY(pos);
         int z = BlockPos.getZ(pos);
         if (to == null) {
            to = new MutableBlockPos(x, y, z);
            consumer.accept(x, y, z);
         } else {
            from.set(to);
            to.set(x, y, z);
            Rasterization3D.ddaSkipFrom(from, to, consumer);
         }
      }
   }
}
