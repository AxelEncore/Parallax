package com.moulberry.axiom.editor.views;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.utils.StringUtils;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

public class ViewManager {
   public static final int MAX_VIEWS = 16;
   private static final List<View> views = new ArrayList<>();
   static UUID activeView = null;
   static int activeFrames = 0;
   private static String location = null;
   private static String altLocation = null;
   private static int saveTimer = 0;

   public static List<View> getViews() {
      return views;
   }

   public static void clear() {
      views.clear();
      addMainView();
   }

   public static void tickSaving() {
      if (location == null) {
         saveTimer = 0;
      } else {
         if (saveTimer > 0) {
            saveTimer--;
            if (saveTimer == 0) {
               trySave(true);
            }
         }
      }
   }

   public static void trySave(boolean force) {
      if (force || saveTimer > 0) {
         saveTimer = 0;
         if (location != null) {
            try {
               Path viewDataPath = Axiom.getInstance().getConfigDirectory().resolve("views").resolve(location);
               Path altPath = altLocation == null ? null : Axiom.getInstance().getConfigDirectory().resolve("views").resolve(altLocation);
               if (altPath != null && !Files.exists(viewDataPath) && Files.exists(altPath)) {
                  viewDataPath = altPath;
               }

               if (areViewsEmpty()) {
                  Files.deleteIfExists(viewDataPath);
                  if (altPath != null) {
                     Files.deleteIfExists(altPath);
                  }

                  return;
               }

               CompoundTag compoundTag = new CompoundTag();
               if (activeView != null) {
                  NbtHelper.putUUID(compoundTag, "ActiveView", activeView);
               }

               ListTag viewsTag = new ListTag();

               for (View view : views) {
                  viewsTag.add(view.save());
               }

               compoundTag.put("Views", viewsTag);
               Files.createDirectories(viewDataPath.getParent());
               NbtIo.writeCompressed(compoundTag, viewDataPath);
               if (altPath != null && altPath != viewDataPath && !Files.exists(altPath)) {
                  try {
                     Files.createLink(altPath, viewDataPath);
                  } catch (Exception var7) {
                  }
               }
            } catch (Exception var8) {
               Axiom.LOGGER.error("Error saving views", var8);
            }
         }
      }
   }

   private static boolean areViewsEmpty() {
      if (views.isEmpty()) {
         return true;
      } else if (views.size() != 1) {
         return false;
      } else {
         View view = views.get(0);
         return !view.pinLocation && !view.pinLevel;
      }
   }

   public static void updateLocation() {
      if (location != null) {
         trySave(false);
      }

      String oldLocation = location;
      location = null;
      altLocation = null;
      if (Minecraft.getInstance().hasSingleplayerServer()) {
         location = Minecraft.getInstance().getSingleplayerServer().storageSource.getLevelId();
      } else {
         SocketAddress address = Minecraft.getInstance().getConnection().getConnection().getRemoteAddress();
         if (address instanceof InetSocketAddress inetSocketAddress) {
            location = inetSocketAddress.getAddress().getHostAddress();
         } else {
            location = address.toString();
         }

         if (ClientEvents.lastServerAddress != null) {
            altLocation = ClientEvents.lastServerAddress.getHost();
            altLocation = StringUtils.sanitizePath(altLocation);
         }
      }

      location = StringUtils.sanitizePath(location);
      if (!Objects.equals(oldLocation, location)) {
         views.clear();
         if (location != null) {
            Path viewDataPath = Axiom.getInstance().getConfigDirectory().resolve("views").resolve(location);
            if (!Files.exists(viewDataPath)) {
               if (altLocation == null) {
                  return;
               }

               viewDataPath = Axiom.getInstance().getConfigDirectory().resolve("views").resolve(altLocation);
               if (!Files.exists(viewDataPath)) {
                  return;
               }
            }

            try {
               CompoundTag compoundTag = NbtIo.readCompressed(viewDataPath, NbtAccounter.unlimitedHeap());
               if (compoundTag.contains("ActiveView")) {
                  activeView = NbtHelper.getUUID(compoundTag, "ActiveView");
               }

               for (Tag viewTag : NbtHelper.getList(compoundTag, "Views", 10)) {
                  if (viewTag instanceof CompoundTag viewCompoundTag) {
                     views.add(View.load(viewCompoundTag));
                  }
               }
            } catch (Exception var7) {
               Axiom.LOGGER.error("Failed to load views", var7);
            }
         }

         if (views.isEmpty()) {
            addMainView();
         }

         boolean activeViewIsValid = false;

         for (View view : views) {
            if (view.uuid.equals(activeView)) {
               activeViewIsValid = true;
               break;
            }
         }

         if (!activeViewIsValid) {
            activeView = views.get(0).uuid;
         }
      }
   }

   public static void addNewView() {
      if (AxiomClient.hasPermission(AxiomPermission.EDITOR_VIEWS) && views.size() < 16) {
         views.add(new View(AxiomI18n.get("axiom.editorui.view.new_view_name"), UUID.randomUUID(), Minecraft.getInstance().player));
         dirty();
      }
   }

   private static void addMainView() {
      if (views.isEmpty() || AxiomClient.hasPermission(AxiomPermission.EDITOR_VIEWS) && views.size() < 16) {
         UUID uuid = UUID.randomUUID();
         views.add(new View(AxiomI18n.get("axiom.editorui.view.main_name"), uuid, Minecraft.getInstance().player));
         activeView = uuid;
         dirty();
      }
   }

   public static void dirty() {
      if (saveTimer <= 0) {
         saveTimer = 200;
      }
   }

   static {
      addMainView();
   }
}
