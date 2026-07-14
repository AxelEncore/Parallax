package com.moulberry.axiom.packets;

import com.moulberry.axiom.tools.ServerHeightmaps;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class AxiomClientboundAddServerHeightmap implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:add_server_heightmap");
   private final String name;
   private final BufferedImage bufferedImage;

   public AxiomClientboundAddServerHeightmap(String name, BufferedImage bufferedImage) {
      this.name = name;
      this.bufferedImage = bufferedImage;
   }

   public AxiomClientboundAddServerHeightmap(FriendlyByteBuf friendlyByteBuf) {
      this.name = friendlyByteBuf.readUtf();
      byte[] pngData = friendlyByteBuf.readByteArray();

      try {
         this.bufferedImage = ImageIO.read(new ByteArrayInputStream(pngData));
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeUtf(this.name);

      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ImageIO.write(this.bufferedImage, "PNG", baos);
         friendlyByteBuf.writeByteArray(baos.toByteArray());
      } catch (Exception var3) {
         throw new RuntimeException(var3);
      }
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      ServerHeightmaps.add(this.name, this.bufferedImage);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundAddServerHeightmap::new);
   }
}
