package com.moulberry.axiom.editor.palette;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class EditorPalette {
   public static int OFFSET_PREVIOUS = 0;
   public static int OFFSET_NEXT = 1;
   public static int OFFSET_AS_LAST_CHILD = 2;
   public static int OFFSET_AS_FIRST_CHILD = 3;
   private final Map<String, EditorPalette> subcategories;
   @Nullable
   private EditorPalette parent = null;
   @Nullable
   private EditorPalette childHead = null;
   @Nullable
   private EditorPalette childTail = null;
   @Nullable
   private EditorPalette siblingNext = null;
   @Nullable
   private EditorPalette siblingPrev = null;
   private List<CustomBlockStateOrTombstone> blocks = new ArrayList<>();
   private boolean isOpen = false;
   private String name;
   private boolean needsReload = false;

   public EditorPalette(String name) {
      this.name = name.replace("#", "");
      this.subcategories = new HashMap<>();
   }

   public static EditorPalette loadFromLegacyConfig(String name, ConfigurationNode node) throws SerializationException {
      EditorPalette palette = new EditorPalette(name);
      ConfigurationNode blocks = node.node(new Object[]{"blocks"});
      List<String> blocksString = blocks.getList(String.class);
      if (blocksString != null) {
         for (String key : blocksString) {
            CustomBlockState customBlockState = ServerCustomBlocks.deserialize(key);
            if (customBlockState != null) {
               palette.addBlock(customBlockState);
            } else {
               palette.addTombstone(key);
            }
         }
      }

      for (Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
         String subname = entry.getKey().toString();
         if (subname.startsWith("c_")) {
            subname = subname.substring(2);
            palette.addSubcategory(loadFromLegacyConfig(subname, entry.getValue()));
         }
      }

      return palette;
   }

   public void markNeedsReload() {
      this.needsReload = true;
   }

   public void reloadIfNeeded() {
      if (this.needsReload) {
         this.reload();
      }
   }

   private void reload() {
      this.needsReload = false;
      List<CustomBlockStateOrTombstone> blocks = this.blocks;
      this.blocks = new ArrayList<>();

      for (CustomBlockStateOrTombstone block : blocks) {
         if (block instanceof BlockState blockState) {
            this.addBlock((CustomBlockState)blockState);
         } else {
            String serialized;
            if (block instanceof CustomBlockState customBlockState) {
               serialized = ServerCustomBlocks.serialize(customBlockState);
            } else {
               if (!(block instanceof EditorPaletteTombstone tombstone)) {
                  throw new FaultyImplementationError();
               }

               serialized = tombstone.key();
            }

            CustomBlockState customBlockState = ServerCustomBlocks.deserialize(serialized);
            if (customBlockState != null) {
               this.addBlock(customBlockState);
            } else {
               this.addTombstone(serialized);
            }
         }
      }

      for (EditorPalette child : this.subcategories.values()) {
         child.reload();
      }
   }

   public EditorPalette createSubcategory(String name) {
      return this.createSubcategory(name, OFFSET_AS_LAST_CHILD);
   }

   public EditorPalette createSubcategory(String name, int offset) {
      this.onChanged();
      name = name.replace("#", "");
      if (offset != OFFSET_AS_LAST_CHILD && offset != OFFSET_AS_FIRST_CHILD) {
         if (offset != OFFSET_PREVIOUS && offset != OFFSET_NEXT) {
            throw new IllegalArgumentException(offset + " is not a valid offset");
         } else if (this.parent == null) {
            return null;
         } else if (this.parent.subcategories.containsKey(name)) {
            return null;
         } else {
            EditorPalette subcategory = new EditorPalette(name);
            subcategory.parent = this.parent;
            if (offset == OFFSET_PREVIOUS) {
               if (this.parent.childHead == this) {
                  this.parent.childHead = subcategory;
               }

               if (this.siblingPrev != null) {
                  subcategory.siblingPrev = this.siblingPrev;
                  this.siblingPrev.siblingNext = subcategory;
               }

               this.siblingPrev = subcategory;
               subcategory.siblingNext = this;
            } else if (offset == OFFSET_NEXT) {
               if (this.parent.childTail == this) {
                  this.parent.childTail = subcategory;
               }

               if (this.siblingNext != null) {
                  subcategory.siblingNext = this.siblingNext;
                  this.siblingNext.siblingPrev = subcategory;
               }

               this.siblingNext = subcategory;
               subcategory.siblingPrev = this;
            }

            this.parent.subcategories.put(name, subcategory);
            return subcategory;
         }
      } else if (this.subcategories.containsKey(name)) {
         return null;
      } else {
         EditorPalette subcategory = new EditorPalette(name);
         subcategory.parent = this;
         if (this.childHead == null || this.childTail == null) {
            this.childHead = this.childTail = subcategory;
         } else if (offset == OFFSET_AS_LAST_CHILD) {
            this.childTail.siblingNext = subcategory;
            subcategory.siblingPrev = this.childTail;
            this.childTail = subcategory;
         } else if (offset == OFFSET_AS_FIRST_CHILD) {
            this.childHead.siblingPrev = subcategory;
            subcategory.siblingNext = this.childHead;
            this.childHead = subcategory;
         }

         this.subcategories.put(name, subcategory);
         return subcategory;
      }
   }

   public boolean isParentOf(EditorPalette child) {
      if (child.parent == this) {
         return true;
      } else {
         for (EditorPalette subcategory : this.subcategories.values()) {
            if (subcategory == child) {
               return true;
            }

            if (subcategory.isParentOf(child)) {
               return true;
            }
         }

         return false;
      }
   }

   public void addSubcategory(EditorPalette subcategory) {
      this.addSubcategory(subcategory, OFFSET_AS_LAST_CHILD);
   }

   public void addSubcategory(EditorPalette subcategory, int offset) {
      this.onChanged();
      if (subcategory == this) {
         throw new IllegalStateException("Cannot add palette to itself");
      } else if (subcategory.parent != null) {
         throw new IllegalArgumentException("Subcategory must not already have a parent");
      } else if (subcategory.siblingNext == null && subcategory.siblingPrev == null) {
         String name = subcategory.getName();
         String originalName = subcategory.getName();
         int duplicateIndex = 1;
         if (offset != OFFSET_AS_LAST_CHILD && offset != OFFSET_AS_FIRST_CHILD) {
            if (offset != OFFSET_PREVIOUS && offset != OFFSET_NEXT) {
               throw new IllegalArgumentException(offset + " is not a valid offset");
            }

            if (this.parent == null) {
               return;
            }

            while (this.parent.subcategories.containsKey(name)) {
               String end = " (" + duplicateIndex + ")";
               name = originalName.substring(0, Math.min(originalName.length(), 32 - end.length())) + end;
               duplicateIndex++;
            }

            if (!name.equals(originalName) && !subcategory.rename(name)) {
               throw new RuntimeException("Unable to rename, even though we checked collisions");
            }

            subcategory.parent = this.parent;
            if (offset == OFFSET_PREVIOUS) {
               if (this.parent.childHead == this) {
                  this.parent.childHead = subcategory;
               }

               if (this.siblingPrev != null) {
                  subcategory.siblingPrev = this.siblingPrev;
                  this.siblingPrev.siblingNext = subcategory;
               }

               this.siblingPrev = subcategory;
               subcategory.siblingNext = this;
            } else if (offset == OFFSET_NEXT) {
               if (this.parent.childTail == this) {
                  this.parent.childTail = subcategory;
               }

               if (this.siblingNext != null) {
                  subcategory.siblingNext = this.siblingNext;
                  this.siblingNext.siblingPrev = subcategory;
               }

               this.siblingNext = subcategory;
               subcategory.siblingPrev = this;
            }

            this.parent.subcategories.put(name, subcategory);
         } else {
            while (this.subcategories.containsKey(name)) {
               String end = " (" + duplicateIndex + ")";
               name = originalName.substring(0, Math.min(originalName.length(), 32 - end.length())) + end;
               duplicateIndex++;
            }

            if (!name.equals(originalName) && !subcategory.rename(name)) {
               throw new RuntimeException("Unable to rename, even though we checked collisions");
            }

            subcategory.parent = this;
            if (this.childHead == null || this.childTail == null) {
               this.childHead = this.childTail = subcategory;
            } else if (offset == OFFSET_AS_LAST_CHILD) {
               this.childTail.siblingNext = subcategory;
               subcategory.siblingPrev = this.childTail;
               this.childTail = subcategory;
            } else if (offset == OFFSET_AS_FIRST_CHILD) {
               this.childHead.siblingPrev = subcategory;
               subcategory.siblingNext = this.childHead;
               this.childHead = subcategory;
            }

            this.subcategories.put(name, subcategory);
         }
      } else {
         throw new IllegalArgumentException("Subcategory must not already have a sibling");
      }
   }

   public void remove() {
      if (this.parent == null) {
         throw new IllegalStateException();
      } else {
         EditorPalette removed = this.parent.subcategories.remove(this.name);
         if (removed != this) {
            throw new IllegalStateException();
         } else {
            if (this.parent.childHead == this) {
               this.parent.childHead = this.siblingNext;
            }

            if (this.parent.childTail == this) {
               this.parent.childTail = this.siblingPrev;
            }

            if (this.siblingPrev != null) {
               this.siblingPrev.siblingNext = this.siblingNext;
            }

            if (this.siblingNext != null) {
               this.siblingNext.siblingPrev = this.siblingPrev;
            }

            this.parent = this.siblingNext = this.siblingPrev = null;
            this.onChanged();
         }
      }
   }

   public boolean rename(String name) {
      Preconditions.checkArgument(name.length() <= 32);
      name = name.replace("#", "");
      if (this.parent != null) {
         if (this.parent.subcategories.containsKey(name)) {
            return false;
         }

         EditorPalette removed = this.parent.subcategories.remove(this.name);
         if (removed != this) {
            throw new IllegalStateException();
         }

         this.parent.subcategories.put(name, this);
      }

      this.name = name;
      this.onChanged();
      return true;
   }

   private void onChanged() {
      EditorUI.getBlockList().markNeedsReload();
   }

   public void removeBlock(int index) {
      this.blocks.remove(index);
      this.onChanged();
   }

   public void addBlock(int index, CustomBlockState block) {
      this.blocks.add(index, block);
      this.onChanged();
   }

   public void addBlock(CustomBlockState block) {
      this.blocks.add(block);
      this.onChanged();
   }

   public void addTombstone(String key) {
      this.blocks.add(new EditorPaletteTombstone(key));
   }

   public List<CustomBlockStateOrTombstone> getBlocks() {
      return Collections.unmodifiableList(this.blocks);
   }

   public int getSubcategoryCount() {
      return this.subcategories.size();
   }

   public boolean isOpen() {
      return this.isOpen;
   }

   public void setOpen(boolean open) {
      this.isOpen = open;
   }

   public List<EditorPalette> getSubcategories() {
      if (this.childHead == null) {
         return List.of();
      } else {
         List<EditorPalette> list = new ArrayList<>(this.subcategories.size());

         for (EditorPalette node = this.childHead; node != null; node = node.siblingNext) {
            list.add(node);
         }

         return list;
      }
   }

   public String getName() {
      return this.name;
   }

   @FunctionalInterface
   public interface EditorPaletteConsumer {
      void accept(String var1, List<CustomBlockStateOrTombstone> var2);
   }

   public static class TypeAdapter implements JsonSerializer<EditorPalette>, JsonDeserializer<EditorPalette> {
      public EditorPalette deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         JsonObject editorPaletteJson = json.getAsJsonObject();
         Map<String, EditorPalette> subcategories = (Map<String, EditorPalette>)context.deserialize(
            editorPaletteJson.get("subcategories"), TypeToken.getParameterized(Map.class, new Type[]{String.class, EditorPalette.class}).getType()
         );
         EditorPalette palette = new EditorPalette("");

         for (Entry<String, EditorPalette> entry : subcategories.entrySet()) {
            entry.getValue().name = entry.getKey();
            palette.addSubcategory(entry.getValue());
         }

         for (String key : (List<String>)context.deserialize(
            editorPaletteJson.get("blocks"), TypeToken.getParameterized(List.class, new Type[]{String.class}).getType()
         )) {
            CustomBlockState customBlockState = ServerCustomBlocks.deserialize(key);
            if (customBlockState != null) {
               palette.addBlock(customBlockState);
            } else {
               palette.addTombstone(key);
            }
         }

         return palette;
      }

      public JsonElement serialize(EditorPalette src, Type typeOfSrc, JsonSerializationContext context) {
         JsonObject editorPaletteJson = new JsonObject();
         List<String> blocksString = new ArrayList<>();

         for (CustomBlockStateOrTombstone block : src.blocks) {
            if (block instanceof CustomBlockState customBlockState) {
               blocksString.add(ServerCustomBlocks.serialize(customBlockState));
            } else {
               if (!(block instanceof EditorPaletteTombstone tombstone)) {
                  throw new FaultyImplementationError();
               }

               blocksString.add(tombstone.key());
            }
         }

         editorPaletteJson.add("blocks", context.serialize(blocksString));
         editorPaletteJson.add("subcategories", context.serialize(src.subcategories));
         return editorPaletteJson;
      }
   }
}
