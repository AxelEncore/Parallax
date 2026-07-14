package com.moulberry.axiom.tools;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.utils.AsyncFileDialogs;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.Nullable;

public class ImageHeightmapProvider {
   public static Path getHeightmapDirectory() {
      return Axiom.getInstance().getConfigDirectory().resolve("heightmaps");
   }

   private static void loadDefaultFolder() {
      Path directory = getHeightmapDirectory();
      Path defaultMarker = directory.resolve(".created_default_heightmaps");

      try {
         Files.deleteIfExists(directory.resolve(".loaded_defaults"));
         Files.deleteIfExists(directory.resolve(".loaded_default_heightmaps"));
      } catch (IOException var9) {
      }

      boolean loadDefault = !Files.exists(directory) || !Files.exists(defaultMarker);
      if (loadDefault) {
         try {
            Files.createDirectories(directory);

            try {
               Files.createFile(defaultMarker);
            } catch (FileAlreadyExistsException var7) {
            }

            Path dunes = directory.resolve("dunes");
            Path geometric = directory.resolve("geometric");
            Path procedural = directory.resolve("procedural");
            Path realworld = directory.resolve("realworld");
            Files.createDirectories(dunes);
            Files.createDirectories(geometric);
            Files.createDirectories(procedural);
            Files.createDirectories(realworld);
            putDefaultFile(dunes.resolve("cross.png"), "/default_heightmaps/dunes/cross.png");
            putDefaultFile(dunes.resolve("simple_25.png"), "/default_heightmaps/dunes/simple_25.png");
            putDefaultFile(dunes.resolve("simple_50.png"), "/default_heightmaps/dunes/simple_50.png");
            putDefaultFile(dunes.resolve("simple_100.png"), "/default_heightmaps/dunes/simple_100.png");
            putDefaultFile(geometric.resolve("clover.png"), "/default_heightmaps/geometric/clover.png");
            putDefaultFile(geometric.resolve("semicircle.png"), "/default_heightmaps/geometric/semicircle.png");
            putDefaultFile(geometric.resolve("square.png"), "/default_heightmaps/geometric/square.png");
            putDefaultFile(geometric.resolve("triangle.png"), "/default_heightmaps/geometric/triangle.png");
            putDefaultFile(procedural.resolve("cells.png"), "/default_heightmaps/procedural/cells.png");
            putDefaultFile(procedural.resolve("cloudy.png"), "/default_heightmaps/procedural/cloudy.png");
            putDefaultFile(procedural.resolve("cubism.png"), "/default_heightmaps/procedural/cubism.png");
            putDefaultFile(procedural.resolve("rocky.png"), "/default_heightmaps/procedural/rocky.png");
            putDefaultFile(procedural.resolve("smoke.png"), "/default_heightmaps/procedural/smoke.png");
            putDefaultFile(procedural.resolve("swirl.png"), "/default_heightmaps/procedural/swirl.png");
            putDefaultFile(realworld.resolve("california_gulf.png"), "/default_heightmaps/realworld/california_gulf.png");
            putDefaultFile(realworld.resolve("cerro_nevado.png"), "/default_heightmaps/realworld/cerro_nevado.png");
            putDefaultFile(realworld.resolve("gunung_agung.png"), "/default_heightmaps/realworld/gunung_agung.png");
            putDefaultFile(realworld.resolve("hawassa.png"), "/default_heightmaps/realworld/hawassa.png");
            putDefaultFile(realworld.resolve("matterhorn.png"), "/default_heightmaps/realworld/matterhorn.png");
            putDefaultFile(realworld.resolve("pico_de_tancitaro.png"), "/default_heightmaps/realworld/pico_de_tancitaro.png");
         } catch (Exception var8) {
            var8.printStackTrace();
         }
      }
   }

   private static void putDefaultFile(Path target, String resource) {
      try {
         InputStream png = ImageHeightmapProvider.class.getResourceAsStream(resource);
         if (png != null) {
            Files.write(target, png.readAllBytes());
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   @Nullable
   public static CompletableFuture<BufferedImage> chooseHeightmap() {
      loadDefaultFolder();
      Path directory = getHeightmapDirectory();
      if (Files.exists(directory) && Files.isDirectory(directory)) {
         String defaultPath = directory.toString();
         String separator = directory.getFileSystem().getSeparator();
         if (!defaultPath.endsWith(separator)) {
            defaultPath = defaultPath + separator;
         }

         CompletableFuture<String> fileFuture = AsyncFileDialogs.openFileDialog(defaultPath, "Image", "png");
         return fileFuture.thenApply(pathStr -> {
            if (pathStr == null) {
               return null;
            } else {
               Path path = Path.of(pathStr);

               try {
                  return ImageIO.read(path.toFile());
               } catch (Throwable var3x) {
                  return null;
               }
            }
         });
      } else {
         return null;
      }
   }
}
