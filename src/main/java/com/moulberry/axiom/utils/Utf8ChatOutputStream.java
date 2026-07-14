package com.moulberry.axiom.utils;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;

public class Utf8ChatOutputStream extends OutputStream {
   private final ByteArrayList byteList = new ByteArrayList();
   private final ChatComponent chat;
   private final ChatFormatting formatting;

   public Utf8ChatOutputStream(ChatFormatting formatting) {
      this.formatting = formatting;
      this.chat = Minecraft.getInstance().gui.getChat();
   }

   @Override
   public void write(int i) throws IOException {
      if (i == 10) {
         int size = this.byteList.size();
         byte[] bytes = new byte[size];
         this.byteList.getElements(0, bytes, 0, size);
         String content = new String(bytes, StandardCharsets.UTF_8);
         this.chat.addMessage(Component.literal(content).withStyle(this.formatting));
         this.byteList.clear();
      } else {
         byte value = (byte)i;
         if (value < 0 || value >= 32) {
            this.byteList.add((byte)i);
         }
      }
   }
}
