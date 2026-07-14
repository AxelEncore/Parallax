package com.moulberry.axiom.world_modification;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.github.luben.zstd.Zstd;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class HistoryIO {
   private static final Lock lock = new ReentrantLock();
   private static final FriendlyByteBuf friendlyDirect = new FriendlyByteBuf(Unpooled.directBuffer());
   private static final String ENTRY_PREFIX = "entry";
   private static final String CHUNK_PREFIX = "chunk";

   public static HistoryBuffer<BlockOrBiomeBuffer> loadHistory(Path historyFolder) {
      lock.lock();

      HistoryBuffer var53;
      try {
         if (!Files.exists(historyFolder)) {
            return new HistoryBuffer<>();
         }

         Files.createDirectories(historyFolder);
         completePendingTransaction(historyFolder);
         boolean positionMissing = true;
         int position = -1;
         int size = 0;
         ByteBuffer readBuffer = ByteBuffer.allocateDirect(2048);
         Path positionPath = historyFolder.resolve("position");
         if (Files.exists(positionPath)) {
            try (SeekableByteChannel in = Files.newByteChannel(positionPath, StandardOpenOption.READ)) {
               in.read(readBuffer);
               readBuffer.flip();
               FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(readBuffer));
               position = byteBuf.readInt();
               size = byteBuf.readInt();
               positionMissing = false;
            } catch (Exception var44) {
            }
         }

         Int2ObjectMap<HistoryEntry<BlockOrBiomeBuffer>> historyEntries = new Int2ObjectOpenHashMap();
         Pattern pattern = Pattern.compile("(entry|chunk)(\\d+).hist");
         ByteBuffer decompressionBuffer = null;

         try (DirectoryStream<Path> ds = Files.newDirectoryStream(historyFolder)) {
            for (Path subpath : ds) {
               String filename = subpath.getFileName().toString();
               Matcher matcher = pattern.matcher(filename);
               if (matcher.matches()) {
                  int fileIndex = Integer.parseInt(matcher.group(2));
                  if (fileIndex < 0) {
                     throw new FaultyImplementationError();
                  }

                  String type = matcher.group(1);
                  if (type.equals("entry")) {
                     if (fileIndex + 1 > size) {
                        size = fileIndex + 1;
                     }

                     try (SeekableByteChannel in = Files.newByteChannel(subpath, StandardOpenOption.READ)) {
                        int fileSize = (int)in.size();
                        if (readBuffer.capacity() < fileSize) {
                           readBuffer = ByteBuffer.allocateDirect(fileSize);
                        } else {
                           readBuffer.clear();
                        }

                        in.read(readBuffer);
                        readBuffer.flip();
                        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(readBuffer));
                        historyEntries.put(fileIndex, HistoryEntry.load(byteBuf));
                     } catch (Exception var42) {
                        var42.printStackTrace();
                     }
                  } else {
                     if (!type.equals("chunk")) {
                        throw new FaultyImplementationError();
                     }

                     int maxPosition = fileIndex * 16 + 15;
                     if (maxPosition + 1 > size) {
                        size = maxPosition + 1;
                     }

                     try (SeekableByteChannel in = Files.newByteChannel(subpath, StandardOpenOption.READ)) {
                        int fileSize = (int)in.size();
                        if (readBuffer.capacity() < fileSize) {
                           readBuffer = ByteBuffer.allocateDirect(fileSize);
                        } else {
                           readBuffer.clear();
                        }

                        in.read(readBuffer);
                        readBuffer.flip();
                        int decompressedSize = (int)Zstd.decompressedSize(readBuffer);
                        if (decompressionBuffer != null && decompressionBuffer.capacity() >= decompressedSize) {
                           decompressionBuffer.clear();
                        } else {
                           decompressionBuffer = ByteBuffer.allocateDirect(decompressedSize);
                        }

                        Zstd.decompress(decompressionBuffer, readBuffer);
                        decompressionBuffer.flip();
                        FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(decompressionBuffer));

                        for (int i = 0; i <= 15; i++) {
                           HistoryEntry<BlockOrBiomeBuffer> historyEntry = HistoryEntry.load(byteBuf);
                           historyEntries.put(fileIndex * 16 + i, historyEntry);
                        }
                     } catch (Exception var46) {
                        var46.printStackTrace();
                     }
                  }
               }
            }
         }

         if (!historyEntries.isEmpty()) {
            IntList missingElements = new IntArrayList();
            DummyBuffer dummy = new DummyBuffer();
            HistoryBuffer<BlockOrBiomeBuffer> historyBuffer = new HistoryBuffer<>();

            for (int i = 0; i < size; i++) {
               HistoryEntry<BlockOrBiomeBuffer> entry = (HistoryEntry<BlockOrBiomeBuffer>)historyEntries.get(i);
               if (entry == null) {
                  missingElements.add(i);
                  historyBuffer.push(new HistoryEntry<>(dummy, dummy, BlockPos.ZERO, "~Corrupted~", 0));
               } else {
                  historyBuffer.push(entry);
               }
            }

            if (positionMissing) {
               displayWarning("Axiom History", "Error: position file has been corrupted or deleted", "Axiom will try to recover, however some data may be lost");
            } else {
               historyBuffer.unsafeSetPosition(position);
            }

            if (!missingElements.isEmpty()) {
               displayWarning(
                  "Axiom History",
                  "Error: " + missingElements.size() + " history entry file(s) have been corrupted or deleted",
                  "It is very likely data has been lost",
                  "Missing history elements: " + Arrays.toString(missingElements.toIntArray())
               );
            }

            return historyBuffer;
         }

         var53 = new HistoryBuffer();
      } catch (Exception var48) {
         displayException(var48);
         return new HistoryBuffer<>();
      } finally {
         lock.unlock();
      }

      return var53;
   }

   private static void completePendingTransaction(Path historyFolder) throws IOException {
      Path path = historyFolder.resolve("transaction");
      if (Files.exists(path)) {
         byte[] bytes = Files.readAllBytes(path);
         FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

         try {
            byte transactionType = friendlyByteBuf.readByte();
            switch (transactionType) {
               case 0: {
                  int position = friendlyByteBuf.readInt();
                  int oldSize = friendlyByteBuf.readInt();
                  doPushEntry(historyFolder, position, oldSize);
                  break;
               }
               case 1: {
                  int position = friendlyByteBuf.readInt();
                  int size = friendlyByteBuf.readInt();
                  doSetPosition(historyFolder, position, size);
                  break;
               }
               case 2:
                  doClear(historyFolder);
            }
         } catch (Exception var7) {
         }

         Files.delete(path);
      }

      Files.deleteIfExists(historyFolder.resolve("buffer.hist"));
   }

   public static void pushEntry(Path historyFolder, HistoryEntry<?> historyEntry, int position, int oldSize) {
      lock.lock();

      try {
         if (!Files.isDirectory(historyFolder)) {
            Files.deleteIfExists(historyFolder);
         }

         Files.createDirectories(historyFolder);
         completePendingTransaction(historyFolder);
         friendlyDirect.resetWriterIndex();
         historyEntry.save(friendlyDirect);
         ByteBuffer buffer = friendlyDirect.nioBuffer();
         Path bufferPath = historyFolder.resolve("buffer.hist");

         try (SeekableByteChannel out = Files.newByteChannel(
               bufferPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC
            )) {
            out.write(buffer);
         }

         int transactionIndex = friendlyDirect.writerIndex();
         friendlyDirect.writeByte(0);
         friendlyDirect.writeInt(position);
         friendlyDirect.writeInt(oldSize);
         beginTransaction(historyFolder, friendlyDirect.nioBuffer(transactionIndex, friendlyDirect.writerIndex() - transactionIndex));
         doPushEntry(historyFolder, position, oldSize);
         endTransaction(historyFolder);
      } catch (IOException var16) {
         displayException(var16);
      } finally {
         lock.unlock();
      }
   }

   private static void doPushEntry(Path historyFolder, int position, int oldSize) throws IOException {
      int chunk = position / 16;
      int minPosition = chunk * 16;
      int maxPosition = minPosition + 16;
      int positionWithinChunk = position - minPosition;
      ByteBuffer readBuffer = null;
      Path chunkFile = historyFolder.resolve("chunk" + chunk + ".hist");
      if (Files.exists(chunkFile)) {
         try (SeekableByteChannel in = Files.newByteChannel(chunkFile, StandardOpenOption.READ)) {
            int fileSize = (int)in.size();
            readBuffer = ByteBuffer.allocateDirect(fileSize);
            in.read(readBuffer);
            readBuffer.flip();
         }

         int decompressedSize = (int)Zstd.decompressedSize(readBuffer);
         ByteBuffer decompressionBuffer = ByteBuffer.allocateDirect(decompressedSize);
         Zstd.decompress(decompressionBuffer, readBuffer);
         decompressionBuffer.flip();
         FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(decompressionBuffer));

         for (int i = 0; i <= positionWithinChunk; i++) {
            try {
               HistoryEntry<BlockOrBiomeBuffer> historyEntry = HistoryEntry.load(byteBuf);
               friendlyDirect.resetWriterIndex();
               historyEntry.save(friendlyDirect);
               ByteBuffer buffer = friendlyDirect.nioBuffer();
               Path path = historyFolder.resolve("entry" + (minPosition + i) + ".hist");

               try (SeekableByteChannel out = Files.newByteChannel(
                     path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC
                  )) {
                  out.write(buffer);
               }
            } catch (Exception var31) {
               Axiom.LOGGER.error("Failed to load history", var31);
               break;
            }
         }

         Files.deleteIfExists(chunkFile);
      }

      Path bufferPath = historyFolder.resolve("buffer.hist");
      if (Files.exists(bufferPath)) {
         Path entryPath = historyFolder.resolve("entry" + position + ".hist");
         Files.move(bufferPath, entryPath, StandardCopyOption.REPLACE_EXISTING);
      }

      Path positionPath = historyFolder.resolve("position");

      try (DataOutputStream out = new DataOutputStream(
            Files.newOutputStream(
               positionPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC
            )
         )) {
         out.writeInt(position);
         out.writeInt(position + 1);
      }

      for (int var38 = position + 1; var38 < oldSize; var38++) {
         Files.deleteIfExists(historyFolder.resolve("entry" + var38 + ".hist"));
      }

      for (int c = position / 16 + 1; c < (oldSize + 15) / 16; c++) {
         Files.deleteIfExists(historyFolder.resolve("chunk" + c + ".hist"));
      }

      if (positionWithinChunk == 15) {
         ByteBuffer writeBuffer = ByteBuffer.allocateDirect(2048);

         for (int e = minPosition; e < maxPosition; e++) {
            Path histEntry = historyFolder.resolve("entry" + e + ".hist");
            if (!Files.exists(histEntry)) {
               return;
            }

            try (SeekableByteChannel in = Files.newByteChannel(histEntry, StandardOpenOption.READ)) {
               int fileSize = (int)in.size();
               if (readBuffer != null && readBuffer.capacity() >= fileSize) {
                  readBuffer.clear();
               } else {
                  readBuffer = ByteBuffer.allocateDirect(fileSize);
               }

               in.read(readBuffer);
               readBuffer.flip();
               FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(readBuffer));
               HistoryEntry.load(byteBuf);
               int need = writeBuffer.position() + readBuffer.limit();
               if (writeBuffer.capacity() <= need) {
                  int newCapacity = Math.max(writeBuffer.capacity() * 2, Mth.smallestEncompassingPowerOfTwo(need));
                  ByteBuffer newWriteBuffer = ByteBuffer.allocateDirect(newCapacity);
                  writeBuffer.flip();
                  newWriteBuffer.put(writeBuffer);
                  writeBuffer = newWriteBuffer;
               }

               writeBuffer.put(readBuffer);
            } catch (Exception var29) {
               var29.printStackTrace();
               return;
            }
         }

         writeBuffer.flip();
         ByteBuffer compressedBuffer = Zstd.compress(writeBuffer, Zstd.defaultCompressionLevel());

         try (SeekableByteChannel out = Files.newByteChannel(
               chunkFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC
            )) {
            out.write(compressedBuffer);
         }

         for (int e = minPosition; e < maxPosition; e++) {
            Files.deleteIfExists(historyFolder.resolve("entry" + e + ".hist"));
         }
      }
   }

   public static void setPosition(Path historyFolder, int position, int size) {
      lock.lock();

      try {
         if (!Files.isDirectory(historyFolder)) {
            Files.deleteIfExists(historyFolder);
         }

         Files.createDirectories(historyFolder);
         completePendingTransaction(historyFolder);
         friendlyDirect.resetWriterIndex();
         friendlyDirect.writeByte(1);
         friendlyDirect.writeInt(position);
         friendlyDirect.writeInt(size);
         beginTransaction(historyFolder, friendlyDirect.nioBuffer());
         doSetPosition(historyFolder, position, size);
         endTransaction(historyFolder);
      } catch (IOException var7) {
         displayException(var7);
      } finally {
         lock.unlock();
      }
   }

   private static void doSetPosition(Path historyFolder, int position, int size) throws IOException {
      Path positionPath = historyFolder.resolve("position");

      try (DataOutputStream out = new DataOutputStream(
            Files.newOutputStream(
               positionPath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC
            )
         )) {
         out.writeInt(position);
         out.writeInt(size);
      }
   }

   public static void clear(Path historyFolder) {
      lock.lock();

      try {
         if (Files.exists(historyFolder)) {
            if (!Files.isDirectory(historyFolder)) {
               Files.deleteIfExists(historyFolder);
            }

            Files.createDirectories(historyFolder);
            completePendingTransaction(historyFolder);
            friendlyDirect.resetWriterIndex();
            friendlyDirect.writeByte(2);
            beginTransaction(historyFolder, friendlyDirect.nioBuffer());
            doClear(historyFolder);
            endTransaction(historyFolder);
            return;
         }
      } catch (IOException var5) {
         displayException(var5);
         return;
      } finally {
         lock.unlock();
      }
   }

   private static void doClear(Path historyFolder) throws IOException {
      for (File file : historyFolder.toFile().listFiles()) {
         if (!file.isDirectory() && !file.getName().contains("transaction")) {
            file.delete();
         }
      }
   }

   private static void beginTransaction(Path historyFolder, ByteBuffer byteBuffer) throws IOException {
      Path path = historyFolder.resolve("transaction");

      try (SeekableByteChannel out = Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, StandardOpenOption.SYNC)) {
         out.write(byteBuffer);
      }
   }

   private static void endTransaction(Path historyFolder) throws IOException {
      Path path = historyFolder.resolve("transaction");
      Files.delete(path);
   }

   private static void displayException(Exception e) {
      StringBuilder builder = new StringBuilder();
      builder.append("An error occured while updating history").append("\n\n");
      builder.append(e);

      for (StackTraceElement element : e.getStackTrace()) {
         builder.append("\n\tat ").append(element);
      }

      Axiom.LOGGER.error("HistoryIO Exception: " + builder);
      ClientEvents.pendingAlertScreen = new AlertScreen(() -> {}, Component.literal(AxiomI18n.get("axiom.hardcoded.lbl_axiom")), Component.literal(builder.toString()));
   }

   private static void displayWarning(String... warning) {
      StringBuilder builder = new StringBuilder();

      for (String s : warning) {
         builder.append(s).append("\n");
      }

      Axiom.LOGGER.error("HistoryIO Warning: " + builder);
      ClientEvents.pendingAlertScreen = new AlertScreen(() -> {}, Component.literal(AxiomI18n.get("axiom.hardcoded.lbl_axiom")), Component.literal(builder.toString()));
   }
}
