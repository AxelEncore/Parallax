package com.moulberry.axiom.displayentity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.math.Transformation;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.collections.JoinedList;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Brightness;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ItemList {
   public static ItemList INSTANCE = new ItemList();
   public List<ItemList.Entry> customEntriesDefinedByPacket = new ArrayList<>();
   private String lastSearch = "";
   private boolean lastOnlyCustomModels = false;
   private List<ItemList.Entry> searchedItems;
   private final List<ItemList.Entry> itemsStartsWith = new ArrayList<>();
   private final List<ItemList.Entry> itemsContains = new ArrayList<>();
   private List<ItemList.Entry> itemsAll;
   private List<ItemList.Entry> itemsAllLengthSorted;
   private List<ItemList.Entry> itemsCustom;
   private List<ItemList.Entry> itemsCustomLengthSorted;
   private boolean isDirty = false;

   private ItemList() {
      this.searchedItems = this.itemsAll = List.of();
      this.itemsAllLengthSorted = List.of();
      NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> {
         if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            this.isDirty = true;
         }
      });
   }

   public void markDirty() {
      this.isDirty = true;
   }

   private void reload() {
      this.isDirty = false;
      ArrayList<ItemList.Entry> itemsAll = new ArrayList<>();
      ArrayList<ItemList.Entry> itemsCustom = new ArrayList<>();
      ArrayList<ItemList.Entry> itemsCustomImportant = new ArrayList<>();
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         CreativeModeTabs.tryRebuildTabContents(
            player.connection.enabledFeatures(),
            player.canUseGameMasterBlocks() && (Boolean)Minecraft.getInstance().options.operatorItemsTab().get(),
            player.level().registryAccess()
         );
      }

      ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
      Vector3f defaultTranslation = new Vector3f();
      Quaternionf defaultLeftRotation = new Quaternionf();
      Vector3f defaultScale = new Vector3f(1.0F, 1.0F, 1.0F);
      Quaternionf defaultRightRotation = new Quaternionf();
      HashSet<String> registeredModels = new HashSet<>();
      boolean loggedError = false;

      for (ItemStack stack : CreativeModeTabs.searchTab().getSearchTabDisplayItems()) {
         if (!stack.isEmpty()) {
            ResourceLocation location = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String name = AxiomI18n.get(stack.getItem().getDescriptionId());
            ItemList.Entry defaultItemEntry = new ItemList.Entry(stack, location, createSearchKey(name), null, null);
            itemsAll.add(defaultItemEntry);
            List<Resource> resourceStack = resourceManager.getResourceStack(
               ResourceLocation.fromNamespaceAndPath(location.getNamespace(), "models/item/" + location.getPath() + ".json")
            );
            Gson gson = new Gson();

            for (Resource resource : resourceStack) {
               try (BufferedReader bufferedReader = resource.openAsReader()) {
                  JsonObject object = (JsonObject)GsonHelper.fromJson(gson, bufferedReader, JsonObject.class);
                  registerAdditionalCustomModelsLegacy(
                     stack,
                     object,
                     registeredModels,
                     defaultTranslation,
                     defaultLeftRotation,
                     defaultScale,
                     defaultRightRotation,
                     location,
                     itemsCustomImportant,
                     itemsCustom
                  );
               } catch (Exception var26) {
                  if (!loggedError) {
                     Axiom.LOGGER.error("Error finding custom model data in resourcepack", var26);
                     loggedError = true;
                  }
               }
            }
         }
      }

      itemsCustom.addAll(0, itemsCustomImportant);
      itemsCustom.addAll(0, this.customEntriesDefinedByPacket);
      itemsAll.addAll(0, itemsCustom);
      this.searchedItems = this.itemsAll = Collections.unmodifiableList(itemsAll);
      this.itemsCustom = Collections.unmodifiableList(itemsCustom);
      ArrayList<ItemList.Entry> itemsAllLengthSorted = new ArrayList<>(itemsAll);
      itemsAllLengthSorted.sort(Comparator.comparingInt(entry -> entry.location.getPath().length()));
      this.itemsAllLengthSorted = Collections.unmodifiableList(itemsAllLengthSorted);
      ArrayList<ItemList.Entry> itemsCustomLengthSorted = new ArrayList<>(itemsCustom);
      itemsCustomLengthSorted.sort(Comparator.comparingInt(entry -> entry.location.getPath().length()));
      this.itemsCustomLengthSorted = Collections.unmodifiableList(itemsCustomLengthSorted);
      this.lastSearch = "";
      this.lastOnlyCustomModels = false;
   }

   private static void registerAdditionalCustomModelsLegacy(
      ItemStack stack,
      JsonObject object,
      HashSet<String> registeredModels,
      Vector3f defaultTranslation,
      Quaternionf defaultLeftRotation,
      Vector3f defaultScale,
      Quaternionf defaultRightRotation,
      ResourceLocation location,
      ArrayList<ItemList.Entry> itemsCustomImportant,
      ArrayList<ItemList.Entry> itemsCustom
   ) {
      JsonArray overrides = object.getAsJsonArray("overrides");
      if (overrides != null) {
         for (JsonElement override : overrides) {
            try {
               JsonObject overrideObj = override.getAsJsonObject();
               JsonObject predicate = overrideObj.getAsJsonObject("predicate");
               JsonElement customModelData = predicate.get("custom_model_data");
               if (customModelData != null) {
                  int customModelDataInt = customModelData.getAsInt();
                  ItemStack customStack = stack.copy();
                  String model = overrideObj.get("model").getAsString();
                  if (!(overrideObj.get("axiom:hide") instanceof JsonPrimitive jsonPrimitive && jsonPrimitive.getAsBoolean()) && registeredModels.add(model)) {
                     ItemStackDataHelper.setCustomModelData(customStack, customModelDataInt);
                     ItemStackDataHelper.setHoverName(
                        customStack, Component.literal(model).setStyle(Style.EMPTY.withItalic(false).withColor(ChatFormatting.YELLOW))
                     );
                     Transformation defaultItemDisplayTransformation = null;
                     if (overrideObj.get("axiom:default_transform") instanceof JsonObject defaultTransform) {
                        Vector3f translation = defaultTranslation;
                        Quaternionf leftRotation = defaultLeftRotation;
                        Vector3f scale = defaultScale;
                        Quaternionf rightRotation = defaultRightRotation;
                        if (defaultTransform.get("translation") instanceof JsonArray jsonArray && jsonArray.size() == 3) {
                           translation = new Vector3f(jsonArray.get(0).getAsFloat(), jsonArray.get(1).getAsFloat(), jsonArray.get(2).getAsFloat());
                        }

                        if (defaultTransform.get("left_rotation") instanceof JsonArray jsonArray && jsonArray.size() == 4) {
                           leftRotation = new Quaternionf(
                              jsonArray.get(0).getAsFloat(), jsonArray.get(1).getAsFloat(), jsonArray.get(2).getAsFloat(), jsonArray.get(3).getAsFloat()
                           );
                        }

                        if (defaultTransform.get("scale") instanceof JsonArray jsonArray && jsonArray.size() == 3) {
                           scale = new Vector3f(jsonArray.get(0).getAsFloat(), jsonArray.get(1).getAsFloat(), jsonArray.get(2).getAsFloat());
                        }

                        if (defaultTransform.get("right_rotation") instanceof JsonArray jsonArray && jsonArray.size() == 4) {
                           rightRotation = new Quaternionf(
                              jsonArray.get(0).getAsFloat(), jsonArray.get(1).getAsFloat(), jsonArray.get(2).getAsFloat(), jsonArray.get(3).getAsFloat()
                           );
                        }

                        defaultItemDisplayTransformation = new Transformation(
                           new Vector3f(translation), new Quaternionf(leftRotation), new Vector3f(scale), new Quaternionf(rightRotation)
                        );
                     }

                     Brightness defaultItemDisplayBrightness = null;
                     if (overrideObj.get("axiom:default_brightness") instanceof JsonObject jsonObject) {
                        defaultItemDisplayBrightness = new Brightness(jsonObject.get("block").getAsInt(), jsonObject.get("sky").getAsInt());
                     }

                     String searchKey = model.replaceAll("[^a-zA-Z]", "");
                     if (searchKey.isEmpty()) {
                        searchKey = AxiomI18n.get(stack.getItem().getDescriptionId());
                     }

                     ItemList.Entry entry = new ItemList.Entry(
                        customStack, location, createSearchKey(searchKey), defaultItemDisplayTransformation, defaultItemDisplayBrightness
                     );
                     if (overrideObj.get("axiom:important") instanceof JsonPrimitive jsonPrimitivex && jsonPrimitivex.getAsBoolean()) {
                        itemsCustomImportant.add(entry);
                     } else {
                        itemsCustom.add(entry);
                     }
                  }
               }
            } catch (Exception var27) {
            }
         }
      }
   }

   public static String createSearchKey(String string) {
      StringBuilder searchKey = new StringBuilder();

      for (char c : string.toLowerCase(Locale.ROOT).toCharArray()) {
         if (!Character.isWhitespace(c)) {
            searchKey.append(c);
         }
      }

      return searchKey.toString();
   }

   public void search(String search, boolean onlyCustomModels) {
      if (this.isDirty) {
         this.reload();
      }

      search = createSearchKey(search);
      if (!search.equals(this.lastSearch) || this.lastOnlyCustomModels != onlyCustomModels) {
         if (search.isBlank()) {
            if (onlyCustomModels) {
               this.searchedItems = this.itemsCustom;
            } else {
               this.searchedItems = this.itemsAll;
            }

            this.lastOnlyCustomModels = onlyCustomModels;
            this.lastSearch = search;
         } else {
            if (this.searchedItems instanceof JoinedList && search.startsWith(this.lastSearch) && this.lastOnlyCustomModels == onlyCustomModels) {
               String searchFinal = search;
               this.itemsContains.removeIf(entryx -> !entryx.searchKey.contains(searchFinal));
               this.itemsStartsWith.removeIf(entryx -> {
                  if (entryx.searchKey.startsWith(searchFinal)) {
                     return false;
                  } else {
                     if (entryx.searchKey.contains(searchFinal)) {
                        this.itemsContains.add(entryx);
                     }

                     return true;
                  }
               });
            } else {
               this.itemsStartsWith.clear();
               this.itemsContains.clear();

               for (ItemList.Entry entry : onlyCustomModels ? this.itemsCustomLengthSorted : this.itemsAllLengthSorted) {
                  if (entry.searchKey.startsWith(search)) {
                     this.itemsStartsWith.add(entry);
                  } else if (entry.searchKey.contains(search)) {
                     this.itemsContains.add(entry);
                  }
               }

               this.searchedItems = new JoinedList<>(this.itemsStartsWith, this.itemsContains);
            }

            this.lastSearch = search;
            this.lastOnlyCustomModels = onlyCustomModels;
         }
      }
   }

   public String getLastSearch() {
      return this.lastSearch;
   }

   public List<ItemList.Entry> getItems() {
      if (this.isDirty) {
         this.reload();
      }

      return this.searchedItems;
   }

   public record Entry(
      ItemStack itemStack,
      ResourceLocation location,
      String searchKey,
      @Nullable Transformation defaultItemDisplayTransformation,
      @Nullable Brightness defaultItemDisplayBrightness
   ) {
   }
}
