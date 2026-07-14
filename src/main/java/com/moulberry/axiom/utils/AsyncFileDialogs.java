package com.moulberry.axiom.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.Util;
import net.minecraft.Util.OS;
import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.nfd.NFDFilterItem;
import org.lwjgl.util.nfd.NativeFileDialog;
import org.lwjgl.util.nfd.NFDFilterItem.Buffer;

public class AsyncFileDialogs {
   private static CompletableFuture<String> currentSaveOrOpenFileDialog = null;
   private static final ExecutorService dialogThread = Executors.newSingleThreadExecutor();
   private static boolean initializedNfd = false;

   public static boolean hasDialog() {
      return currentSaveOrOpenFileDialog != null;
   }

   public static CompletableFuture<String> saveFileDialog(String defaultPath, String defaultName, String filterDescription, String... filters) {
      if (hasDialog()) {
         return CompletableFuture.completedFuture(null);
      } else {
         currentSaveOrOpenFileDialog = new CompletableFuture<>();
         CompletableFuture<String> future = currentSaveOrOpenFileDialog;
         boolean initializedNfd = AsyncFileDialogs.initializedNfd;
         AsyncFileDialogs.initializedNfd = true;
         Runnable runnable = () -> {
            if (!initializedNfd) {
               NativeFileDialog.NFD_Init();
            }

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  PointerBuffer out = stack.callocPointer(1);
                  StringBuilder filterBuilder = new StringBuilder();

                  for (String filter : filters) {
                     if (!filterBuilder.isEmpty()) {
                        filterBuilder.append(",");
                     }

                     filterBuilder.append(filter(filter));
                  }

                  Buffer filtersBuffer = NFDFilterItem.malloc(1);
                  ((NFDFilterItem)filtersBuffer.get(0)).name(stack.UTF8(filter(filterDescription))).spec(stack.UTF8(filterBuilder.toString()));
                  int result = NativeFileDialog.NFD_SaveDialog(out, filtersBuffer, filter(defaultPath), filter(defaultName));
                  if (result != 1) {
                     currentSaveOrOpenFileDialog.complete(null);
                     currentSaveOrOpenFileDialog = null;
                  } else {
                     currentSaveOrOpenFileDialog.complete(out.getStringUTF8(0));
                     currentSaveOrOpenFileDialog = null;
                     NativeFileDialog.NFD_FreePath(out.get(0));
                  }
               } catch (Throwable var18) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Throwable var19) {
               var19.printStackTrace();
            } finally {
               if (currentSaveOrOpenFileDialog != null) {
                  currentSaveOrOpenFileDialog.complete(null);
                  currentSaveOrOpenFileDialog = null;
               }
            }
         };
         if (Util.getPlatform() == OS.OSX) {
            Minecraft.getInstance().submit(runnable);
         } else {
            dialogThread.submit(runnable);
         }

         return future;
      }
   }

   public static CompletableFuture<String> openFileDialog(String defaultPath, String filterDescription, String... filters) {
      if (hasDialog()) {
         return CompletableFuture.completedFuture(null);
      } else {
         currentSaveOrOpenFileDialog = new CompletableFuture<>();
         CompletableFuture<String> future = currentSaveOrOpenFileDialog;
         boolean initializedNfd = AsyncFileDialogs.initializedNfd;
         AsyncFileDialogs.initializedNfd = true;
         Runnable runnable = () -> {
            if (!initializedNfd) {
               NativeFileDialog.NFD_Init();
            }

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  PointerBuffer out = stack.callocPointer(1);
                  StringBuilder filterBuilder = new StringBuilder();

                  for (String filter : filters) {
                     if (!filterBuilder.isEmpty()) {
                        filterBuilder.append(",");
                     }

                     filterBuilder.append(filter(filter));
                  }

                  Buffer filtersBuffer = NFDFilterItem.malloc(1);
                  ((NFDFilterItem)filtersBuffer.get(0)).name(stack.UTF8(filter(filterDescription))).spec(stack.UTF8(filterBuilder.toString()));
                  int result = NativeFileDialog.NFD_OpenDialog(out, filtersBuffer, filter(defaultPath));
                  if (result != 1) {
                     currentSaveOrOpenFileDialog.complete(null);
                     currentSaveOrOpenFileDialog = null;
                  } else {
                     currentSaveOrOpenFileDialog.complete(out.getStringUTF8(0));
                     currentSaveOrOpenFileDialog = null;
                     NativeFileDialog.NFD_FreePath(out.get(0));
                  }
               } catch (Throwable var17) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }
                  }

                  throw var17;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Throwable var18) {
               var18.printStackTrace();
            } finally {
               if (currentSaveOrOpenFileDialog != null) {
                  currentSaveOrOpenFileDialog.complete(null);
                  currentSaveOrOpenFileDialog = null;
               }
            }
         };
         if (Util.getPlatform() == OS.OSX) {
            Minecraft.getInstance().submit(runnable);
         } else {
            dialogThread.submit(runnable);
         }

         return future;
      }
   }

   public static CompletableFuture<String> openFolderDialog(String defaultPath) {
      if (hasDialog()) {
         return CompletableFuture.completedFuture(null);
      } else {
         currentSaveOrOpenFileDialog = new CompletableFuture<>();
         CompletableFuture<String> future = currentSaveOrOpenFileDialog;
         boolean initializedNfd = AsyncFileDialogs.initializedNfd;
         AsyncFileDialogs.initializedNfd = true;
         Runnable runnable = () -> {
            if (!initializedNfd) {
               NativeFileDialog.NFD_Init();
            }

            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  PointerBuffer out = stack.callocPointer(1);
                  int result = NativeFileDialog.NFD_PickFolder(out, filter(defaultPath));
                  if (result != 1) {
                     currentSaveOrOpenFileDialog.complete(null);
                     currentSaveOrOpenFileDialog = null;
                  } else {
                     currentSaveOrOpenFileDialog.complete(out.getStringUTF8(0));
                     currentSaveOrOpenFileDialog = null;
                     NativeFileDialog.NFD_FreePath(out.get(0));
                  }
               } catch (Throwable var11) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (Throwable var12) {
               var12.printStackTrace();
            } finally {
               if (currentSaveOrOpenFileDialog != null) {
                  currentSaveOrOpenFileDialog.complete(null);
                  currentSaveOrOpenFileDialog = null;
               }
            }
         };
         if (Util.getPlatform() == OS.OSX) {
            Minecraft.getInstance().submit(runnable);
         } else {
            dialogThread.submit(runnable);
         }

         return future;
      }
   }

   public static String filter(CharSequence in) {
      return filterLT20(in.toString().replace("'", "").replace("\"", "").replace("$", "").replace("`", ""));
   }

   public static String filterLT20(CharSequence in) {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i < in.length(); i++) {
         char c = in.charAt(i);
         if (c >= ' ' || c == '\n') {
            builder.append(c);
         }
      }

      return builder.toString();
   }
}
