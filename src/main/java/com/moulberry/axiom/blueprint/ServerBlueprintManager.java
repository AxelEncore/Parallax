package com.moulberry.axiom.blueprint;

import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.packets.blueprint.AxiomClientboundBlueprintManifest;
import com.moulberry.axiom.restrictions.AxiomPermission;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

public class ServerBlueprintManager {
   private static ServerBlueprintRegistry registry = null;

   public static void initialize(Path blueprintDirectory) {
      Map<String, RawBlueprint> map = new HashMap<>();
      loadRegistryFromFolder(map, blueprintDirectory, "/");
      registry = new ServerBlueprintRegistry(map);
   }

   public static void sendManifest(List<ServerPlayer> serverPlayers) {
      if (registry != null) {
         List<ServerPlayer> sendTo = new ArrayList<>();

         for (ServerPlayer serverPlayer : serverPlayers) {
            if (AxiomServer.canUseAxiom(serverPlayer, AxiomPermission.BLUEPRINT_MANIFEST)) {
               sendTo.add(serverPlayer);
            }
         }

         AxiomClientboundBlueprintManifest.sendMulti(sendTo, registry);
      }
   }

   public static ServerBlueprintRegistry getRegistry() {
      return registry;
   }

   private static void loadRegistryFromFolder(Map<String, RawBlueprint> map, Path folder, String location) {
      if (Files.isDirectory(folder)) {
         try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder)) {
            for (Path path : directoryStream) {
               String filename = path.getFileName().toString();
               if (filename.endsWith(".bp")) {
                  try {
                     RawBlueprint rawBlueprint = BlueprintIo.readRawBlueprint(new BufferedInputStream(Files.newInputStream(path)));
                     String newLocation = location + filename.substring(0, filename.length() - 3);
                     map.put(newLocation, rawBlueprint);
                  } catch (Exception var10) {
                     var10.printStackTrace();
                  }
               } else if (Files.isDirectory(path)) {
                  String newLocation = location + filename + "/";
                  loadRegistryFromFolder(map, path, newLocation);
               }
            }
         } catch (IOException var12) {
         }
      }
   }
}
