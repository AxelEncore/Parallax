package com.moulberry.axiom.world_modification;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictCompress;
import com.github.luben.zstd.ZstdDictDecompress;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.FriendlyByteBuf;

public record CompressedBlockEntity(int originalSize, byte compressionDict, byte[] compressed) {
   private static final ZstdDictCompress zstdDictCompress;
   private static final ZstdDictDecompress zstdDictDecompress;

   public static CompressedBlockEntity compress(CompoundTag tag, ByteArrayOutputStream baos) {
      try {
         baos.reset();
         DataOutputStream dos = new DataOutputStream(baos);
         NbtIo.write(tag, dos);
         byte[] uncompressed = baos.toByteArray();
         byte[] compressed = Zstd.compress(uncompressed, zstdDictCompress);
         return new CompressedBlockEntity(uncompressed.length, (byte)0, compressed);
      } catch (IOException var5) {
         throw new RuntimeException(var5);
      }
   }

   public CompoundTag decompress() {
      if (this.compressionDict != 0) {
         throw new UnsupportedOperationException("Unknown compression dict: " + this.compressionDict);
      } else {
         try {
            byte[] nbt = Zstd.decompress(this.compressed, zstdDictDecompress, this.originalSize);
            return NbtIo.read(new DataInputStream(new ByteArrayInputStream(nbt)));
         } catch (IOException var2) {
            throw new RuntimeException(var2);
         }
      }
   }

   public static CompressedBlockEntity read(FriendlyByteBuf friendlyByteBuf) {
      int originalSize = friendlyByteBuf.readVarInt();
      byte compressionDict = friendlyByteBuf.readByte();
      byte[] compressed = friendlyByteBuf.readByteArray();
      return new CompressedBlockEntity(originalSize, compressionDict, compressed);
   }

   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeVarInt(this.originalSize);
      friendlyByteBuf.writeByte(this.compressionDict);
      friendlyByteBuf.writeByteArray(this.compressed);
   }

   static {
      try {
         URL url = CompressedBlockEntity.class.getClassLoader().getResource("zstd_dictionaries/block_entities_v1.dict");
         URLConnection connection = url.openConnection();
         connection.setUseCaches(false);

         try (InputStream is = connection.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            zstdDictCompress = new ZstdDictCompress(bytes, Zstd.defaultCompressionLevel());
            zstdDictDecompress = new ZstdDictDecompress(bytes);
         }
      } catch (Exception var7) {
         throw new RuntimeException(var7);
      }
   }
}
