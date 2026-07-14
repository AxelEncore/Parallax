package com.moulberry.axiom.editor.windows.clipboard;

import com.moulberry.axiom.blueprint.BlueprintHeader;
import com.moulberry.axiom.blueprint.BlueprintIo;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class BlueprintDirectoryLoader {
   public static BlueprintDirectory loadDirectory(String name, Path path, WatchService watchService, Map<Path, BlueprintDirectory> dirStructureMap) {
      ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
      BlueprintOrDirectory blueprintOrDirectory = forkJoinPool.invoke(
         new BlueprintDirectoryLoader.LoadBlueprintOrDirectoryTask(name, path, watchService, Collections.synchronizedMap(dirStructureMap))
      );
      if (blueprintOrDirectory instanceof BlueprintOrDirectory.Dir dir) {
         return dir.blueprintDirectory;
      } else {
         throw new RuntimeException("Not a valid blueprint directory: " + path);
      }
   }

   public static BlueprintOrDirectory loadBlueprintOrDirectory(Path path, WatchService watchService, Map<Path, BlueprintDirectory> dirStructureMap) {
      if (Files.isDirectory(path)) {
         String name = path.getFileName().toString();
         ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
         return forkJoinPool.invoke(
            new BlueprintDirectoryLoader.LoadBlueprintOrDirectoryTask(name, path, watchService, Collections.synchronizedMap(dirStructureMap))
         );
      } else {
         return loadBpInner(path);
      }
   }

   private static ForkJoinTask<BlueprintOrDirectory> forkBlueprintOrDirectory(
      Path path, WatchService watchService, Map<Path, BlueprintDirectory> dirStructureMap
   ) {
      return new BlueprintDirectoryLoader.LoadBlueprintOrDirectoryTask(path.getFileName().toString(), path, watchService, dirStructureMap).fork();
   }

   private static BlueprintOrDirectory.Bp loadBpInner(Path path) {
      try {
         BlueprintOrDirectory.Bp var3;
         try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path), 2048)) {
            BlueprintHeader blueprintHeader = BlueprintIo.readHeader(inputStream);
            var3 = new BlueprintOrDirectory.Bp(new PathWrapper(path, null), blueprintHeader);
         }

         return var3;
      } catch (Exception var6) {
         return null;
      }
   }

   private static class LoadBlueprintOrDirectoryTask extends RecursiveTask<BlueprintOrDirectory> {
      private final String name;
      private final Path path;
      private final WatchService watchService;
      private final Map<Path, BlueprintDirectory> dirStructureMap;

      public LoadBlueprintOrDirectoryTask(String name, Path path, WatchService watchService, Map<Path, BlueprintDirectory> dirStructureMap) {
         this.name = name;
         this.path = path;
         this.watchService = watchService;
         this.dirStructureMap = dirStructureMap;
      }

      protected BlueprintOrDirectory compute() {
         if (Files.isDirectory(this.path)) {
            Set<Path> loadedPaths = new HashSet<>();
            BlueprintDirectory blueprintDirectory = new BlueprintDirectory(new PathWrapper(this.path, null), this.name);

            try {
               if (this.watchService != null) {
                  this.path
                     .register(
                        this.watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE
                     );
               }

               this.dirStructureMap.put(this.path, blueprintDirectory);
            } catch (IOException var14) {
               throw new UncheckedIOException(var14);
            }

            List<ForkJoinTask<BlueprintOrDirectory>> childrenTasks = new ArrayList<>();
            Path dotfile = this.path.resolve(".axiom_blueprint_ordering");
            if (Files.exists(dotfile)) {
               String line;
               try (BufferedReader reader = Files.newBufferedReader(dotfile)) {
                  while ((line = reader.readLine()) != null) {
                     Path subpath = this.path.resolve(line.replace("/", this.path.getFileSystem().getSeparator()));
                     if (loadedPaths.add(subpath)) {
                        childrenTasks.add(BlueprintDirectoryLoader.forkBlueprintOrDirectory(subpath, this.watchService, this.dirStructureMap));
                     }
                  }
               } catch (IOException var13) {
                  throw new UncheckedIOException(var13);
               }
            }

            try (DirectoryStream<Path> ds = Files.newDirectoryStream(this.path)) {
               for (Path subpath : ds) {
                  if (!loadedPaths.contains(subpath) && !subpath.getFileName().toString().startsWith(".")) {
                     childrenTasks.add(BlueprintDirectoryLoader.forkBlueprintOrDirectory(subpath, this.watchService, this.dirStructureMap));
                  }
               }
            } catch (IOException var11) {
               throw new UncheckedIOException(var11);
            }

            for (ForkJoinTask<BlueprintOrDirectory> task : childrenTasks) {
               BlueprintOrDirectory blueprintOrDirectory = task.join();
               if (blueprintOrDirectory != null) {
                  blueprintDirectory.addLast(blueprintOrDirectory);
               }
            }

            return new BlueprintOrDirectory.Dir(blueprintDirectory);
         } else {
            return BlueprintDirectoryLoader.loadBpInner(this.path);
         }
      }
   }
}
