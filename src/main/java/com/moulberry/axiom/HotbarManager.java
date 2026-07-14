package com.moulberry.axiom;

import com.mojang.serialization.Dynamic;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import com.moulberry.axiom.utils.StringUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HotbarManager {
   private static int activeHotbarIndex = 0;
   private static Int2ObjectMap<ItemStack> items = new Int2ObjectOpenHashMap();
   private static String location = null;
   private static String altLocation = null;
   private static int saveTimer = 0;
   private static ReentrantLock lock = new ReentrantLock();

   public static void clearPage() {
      lock.lock();

      try {
         int page = activeHotbarIndex / 9;
         int itemIndexOffset = page * 81;

         for (int i = 0; i < 81; i++) {
            items.remove(i + itemIndexOffset);
         }

         activeHotbarIndex = page * 9;
         if (saveTimer <= 0) {
            saveTimer = 200;
         }
      } finally {
         lock.unlock();
      }
   }

   public static void clearAll() {
      lock.lock();

      try {
         activeHotbarIndex = 0;
         items.clear();
         saveTimer = 0;
      } finally {
         lock.unlock();
      }
   }

   public static void tickSaving() {
      lock.lock();

      try {
         if (Objects.equals(location, "__global__") != Axiom.configuration.contextMenu.globalHotbars) {
            updateLocation();
            return;
         }

         LocalPlayer player = Minecraft.getInstance().player;
         if (location == null || player == null) {
            saveTimer = 0;
            return;
         }

         if (saveTimer > 0) {
            saveTimer--;
            if (saveTimer == 0) {
               trySave(player, player.registryAccess(), true);
            }
         }
      } finally {
         lock.unlock();
      }
   }

   public static void trySave(@Nullable LocalPlayer player, @NotNull RegistryAccess registryAccess, boolean force) {
      lock.lock();

      try {
         boolean isGlobal = Objects.equals(location, "__global__");
         if (!force) {
            boolean hasChanges = saveTimer > 0;
            if (player != null && isGlobal) {
               for (int i = 0; i < 9; i++) {
                  ItemStack itemStack = player.getInventory().getItem(i);
                  ItemStack current = (ItemStack)items.get(activeHotbarIndex * 9 + i);
                  boolean sameItem;
                  if (itemStack.isEmpty()) {
                     sameItem = current == null || current.isEmpty();
                  } else {
                     sameItem = current != null && ItemStack.matches(itemStack, current);
                  }

                  if (!sameItem) {
                     hasChanges = true;
                     if (itemStack.isEmpty()) {
                        items.remove(activeHotbarIndex * 9 + i);
                     } else {
                        items.put(activeHotbarIndex * 9 + i, itemStack);
                     }
                  }
               }
            }

            if (!hasChanges) {
               return;
            }
         }

         saveTimer = 0;
         if (location == null) {
            return;
         }

         try {
            Path hotbarDataPath = Axiom.getInstance().getConfigDirectory().resolve("hotbars").resolve(location);
            Path altPath = altLocation == null ? null : Axiom.getInstance().getConfigDirectory().resolve("hotbars").resolve(altLocation);
            if (altPath != null && !Files.exists(hotbarDataPath) && Files.exists(altPath)) {
               hotbarDataPath = altPath;
            }

            if (activeHotbarIndex != 0 || !items.isEmpty()) {
               CompoundTag compoundTag = new CompoundTag();
               if (isGlobal) {
                  compoundTag.putBoolean("AutomaticHotbarIndex", true);
               } else {
                  compoundTag.putInt("ActiveHotbarIndex", activeHotbarIndex);
               }

               compoundTag.putInt("DataVersion", DFUHelper.DATA_VERSION);
               CompoundTag itemsTag = new CompoundTag();
               ObjectIterator var23 = items.int2ObjectEntrySet().iterator();

               while (var23.hasNext()) {
                  Entry<ItemStack> entry = (Entry<ItemStack>)var23.next();
                  int key = entry.getIntKey();
                  ItemStack itemStackx = (ItemStack)entry.getValue();
                  if (!itemStackx.isEmpty()) {
                     itemsTag.put(key + "", ItemStackDataHelper.save(itemStackx, registryAccess));
                  }
               }

               compoundTag.put("Items", itemsTag);
               Files.createDirectories(hotbarDataPath.getParent());
               NbtIo.writeCompressed(compoundTag, hotbarDataPath);
               if (altPath != null && altPath != hotbarDataPath && !Files.exists(altPath)) {
                  try {
                     Files.createLink(altPath, hotbarDataPath);
                  } catch (Exception var16) {
                  }

                  return;
               }

               return;
            }

            Files.deleteIfExists(hotbarDataPath);
            if (altPath != null) {
               Files.deleteIfExists(altPath);
            }
         } catch (Exception var17) {
            Axiom.LOGGER.error("Error saving hotbars", var17);
            return;
         }
      } finally {
         lock.unlock();
      }
   }

   public static void unload(RegistryAccess registryAccess) {
      lock.lock();

      try {
         LocalPlayer player = Minecraft.getInstance().player;
         if (location != null) {
            trySave(player, registryAccess, true);
         }

         location = null;
         altLocation = null;
         clearAll();
      } finally {
         lock.unlock();
      }
   }

   public static void updateLocation() {
      lock.lock();

      try {
         LocalPlayer player = Minecraft.getInstance().player;
         if (location != null && player != null) {
            trySave(player, player.registryAccess(), false);
         }

         String oldLocation = location;
         location = null;
         altLocation = null;
         if (player == null) {
            clearAll();
         } else {
            IntegratedServer integratedServer = Minecraft.getInstance().getSingleplayerServer();
            ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (Axiom.configuration.contextMenu.globalHotbars) {
               location = "__global__";
            } else if (integratedServer != null) {
               location = integratedServer.storageSource.getLevelId();
            } else {
               if (connection == null) {
                  clearAll();
                  return;
               }

               SocketAddress address = connection.getConnection().getRemoteAddress();
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
               loadFromDisk(player);
            }
         }
      } finally {
         lock.unlock();
      }
   }

   private static void loadFromDisk(LocalPlayer player) {
      clearAll();
      if (location != null) {
         Path hotbarDataPath = Axiom.getInstance().getConfigDirectory().resolve("hotbars").resolve(location);
         if (!Files.exists(hotbarDataPath)) {
            if (altLocation == null) {
               return;
            }

            hotbarDataPath = Axiom.getInstance().getConfigDirectory().resolve("hotbars").resolve(altLocation);
            if (!Files.exists(hotbarDataPath)) {
               return;
            }
         }

         try {
            CompoundTag compoundTag = NbtIo.readCompressed(hotbarDataPath, NbtAccounter.unlimitedHeap());
            int currentDataVersion = DFUHelper.DATA_VERSION;
            boolean automaticHotbarIndex = VersionUtilsNbt.helperCompoundTagGetBooleanOr(compoundTag, "AutomaticHotbarIndex", false);
            activeHotbarIndex = Math.max(0, VersionUtilsNbt.helperCompoundTagGetIntOr(compoundTag, "ActiveHotbarIndex", 0));
            int dataVersion = VersionUtilsNbt.helperCompoundTagGetIntOr(compoundTag, "DataVersion", currentDataVersion);
            if (dataVersion > currentDataVersion) {
               Axiom.LOGGER.info("Refusing to load hotbars since dataVersion (" + dataVersion + ") > currentDataVersion (" + currentDataVersion + ")");
            } else {
               CompoundTag itemsTag = compoundTag.getCompound("Items");

               for (String key : itemsTag.getAllKeys()) {
                  int keyInt = -1;

                  try {
                     keyInt = Integer.parseInt(key);
                  } catch (Exception var15) {
                  }

                  if (keyInt >= 0) {
                     CompoundTag itemCompound = itemsTag.getCompound(key);
                     if (dataVersion != currentDataVersion) {
                        try {
                           Dynamic<Tag> dynamic = new Dynamic(NbtOps.INSTANCE, itemCompound);
                           Dynamic<Tag> output = DataFixers.getDataFixer().update(References.ITEM_STACK, dynamic, dataVersion, currentDataVersion);
                           itemCompound = (CompoundTag)output.getValue();
                        } catch (Exception var14) {
                           Axiom.LOGGER.error("Failed to data fix item stack for hotbar", var14);
                        }
                     }

                     ItemStack itemStack = ItemStackDataHelper.loadOrEmpty(itemCompound, player.registryAccess());
                     if (!itemStack.isEmpty()) {
                        items.put(keyInt, itemStack);
                     }
                  }
               }

               if (dataVersion != currentDataVersion) {
                  trySave(player, player.registryAccess(), true);
               }

               if (automaticHotbarIndex) {
                  ItemStack[] hotbarItems = new ItemStack[9];

                  for (int i = 0; i < 9; i++) {
                     hotbarItems[i] = player.getInventory().getItem(i);
                  }

                  IntRBTreeSet hotbarIndexes = new IntRBTreeSet();
                  IntIterator intIterator = items.keySet().intIterator();

                  while (intIterator.hasNext()) {
                     hotbarIndexes.add(intIterator.nextInt() / 9);
                  }

                  intIterator = hotbarIndexes.intIterator();

                  while (intIterator.hasNext()) {
                     int candidateHotbarIndex = intIterator.nextInt();
                     boolean matches = true;

                     for (int i = 0; i < 9; i++) {
                        ItemStack storedItemStack = (ItemStack)items.get(candidateHotbarIndex * 9 + i);
                        if (hotbarItems[i].isEmpty()) {
                           if (storedItemStack != null && !storedItemStack.isEmpty()) {
                              matches = false;
                              break;
                           }
                        } else if (storedItemStack == null || !ItemStack.matches(hotbarItems[i], storedItemStack)) {
                           matches = false;
                           break;
                        }
                     }

                     if (matches) {
                        activeHotbarIndex = candidateHotbarIndex;
                        break;
                     }
                  }
               }
            }
         } catch (Exception var16) {
            Axiom.LOGGER.error("Failed to load hotbars", var16);
         }
      }
   }

   public static int findHotbarWithMatchingItem(ItemStack matching) {
      lock.lock();

      try {
         ObjectIterator var1 = items.int2ObjectEntrySet().iterator();

         while (var1.hasNext()) {
            Entry<ItemStack> entry = (Entry<ItemStack>)var1.next();
            ItemStack itemStack = (ItemStack)entry.getValue();
            if (itemStack != null && !itemStack.isEmpty()) {
               int hotbarIndex = entry.getIntKey() / 9;
               if (hotbarIndex != activeHotbarIndex && ItemStack.isSameItemSameComponents(matching, itemStack)) {
                  return hotbarIndex;
               }
            }
         }

         return -1;
      } finally {
         lock.unlock();
      }
   }

   public static int getActiveHotbarIndex() {
      return activeHotbarIndex;
   }

   public static void setActiveHotbarIndex(int activeHotbarIndex) {
      lock.lock();

      try {
         if (activeHotbarIndex == HotbarManager.activeHotbarIndex) {
            return;
         }

         MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
         if (gameMode == null) {
            return;
         }

         if (!Minecraft.getInstance().player.hasInfiniteMaterials()) {
            return;
         }

         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            for (int i = 0; i < 9; i++) {
               ItemStack oldStack = player.getInventory().getItem(i);
               if (oldStack.isEmpty()) {
                  items.remove(HotbarManager.activeHotbarIndex * 9 + i);
               } else {
                  items.put(HotbarManager.activeHotbarIndex * 9 + i, oldStack);
               }

               ItemStack itemStack = getItemStack(activeHotbarIndex, i);
               player.getInventory().setItem(i, itemStack);
               gameMode.handleCreativeModeItemAdd(itemStack, 36 + i);
            }

            HotbarManager.activeHotbarIndex = activeHotbarIndex;
            player.inventoryMenu.broadcastChanges();
            if (saveTimer <= 0) {
               saveTimer = 200;
            }

            return;
         }
      } finally {
         lock.unlock();
      }
   }

   public static void setItemStack(int hotbarIndex, int itemIndex, ItemStack itemStack) {
      lock.lock();

      try {
         if (hotbarIndex != activeHotbarIndex) {
            if (itemStack.isEmpty()) {
               items.remove(hotbarIndex * 9 + itemIndex);
            } else {
               items.put(hotbarIndex * 9 + itemIndex, itemStack.copy());
            }

            if (saveTimer <= 0) {
               saveTimer = 200;
            }

            return;
         }

         MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
         if (gameMode == null) {
            return;
         }

         if (!Minecraft.getInstance().player.hasInfiniteMaterials()) {
            return;
         }

         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            player.getInventory().setItem(itemIndex, itemStack);
            gameMode.handleCreativeModeItemAdd(itemStack, 36 + itemIndex);
            player.inventoryMenu.broadcastChanges();
            return;
         }
      } finally {
         lock.unlock();
      }
   }

   public static ItemStack getItemStack(int hotbarIndex, int itemIndex) {
      lock.lock();

      ItemStack var2;
      try {
         if (hotbarIndex != activeHotbarIndex) {
            return ((ItemStack)Objects.requireNonNullElse((ItemStack)items.get(hotbarIndex * 9 + itemIndex), ItemStack.EMPTY)).copy();
         }

         var2 = Minecraft.getInstance().player.getInventory().getItem(itemIndex).copy();
      } finally {
         lock.unlock();
      }

      return var2;
   }
}
