package com.moulberry.axiom.editor;

import com.mojang.datafixers.util.Pair;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.DefaultBlocks;
import com.moulberry.axiom.collections.JoinedList;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.datagen.MaterialBlockTagGenerator;
import com.moulberry.axiom.editor.palette.CustomBlockStateOrTombstone;
import com.moulberry.axiom.editor.palette.EditorPalette;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.ImportantBlocks;
import com.moulberry.axiom.utils.WeirdTags;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import com.moulberry.axiom.utils.ClientTagsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.SessionSearchTrees;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlockList {
   private boolean needsReload = true;
   private String lastNameSearch = "";
   private Set<String> lastFilteredTags = Set.of();
   private Set<String> lastEffectiveFilteredTags = Set.of();
   private Set<String> lastFilteredNamespaces = Set.of();
   private String lastSearchRaw = "";
   private Predicate<CustomBlockState> lastFilter = null;
   private List<BlockList.Entry> searchedBlocks;
   private List<BlockList.TagEntry> searchedTags;
   private final List<BlockList.Entry> blocksStartsWith = new ArrayList<>();
   private final List<BlockList.Entry> blocksContains = new ArrayList<>();
   private final List<BlockList.TagEntry> tagsStartsWith = new ArrayList<>();
   private final List<BlockList.TagEntry> tagsContains = new ArrayList<>();
   private List<BlockList.Entry> blocksAll;
   private List<BlockList.Entry> blocksAllLengthSorted;
   private List<BlockList.TagEntry> tagsAll;
   private List<BlockList.TagEntry> tagsAllLengthSorted;
   private static final Set<ResourceLocation> BUILTIN_AXIOM_TAGS = Set.of(
      ResourceLocation.parse("axiom:concrete"),
      ResourceLocation.parse("axiom:concrete_powder"),
      ResourceLocation.parse("axiom:grayscale_blocks"),
      ResourceLocation.parse("axiom:infested_blocks"),
      ResourceLocation.parse("axiom:ore_blocks"),
      ResourceLocation.parse("axiom:precious_materials"),
      ResourceLocation.parse("axiom:tree_blocks"),
      ResourceLocation.parse("axiom:can_be_waterlogged"),
      ResourceLocation.parse("axiom:colored_blocks"),
      ResourceLocation.parse("axiom:existing"),
      ResourceLocation.parse("axiom:falling_blocks"),
      ResourceLocation.parse("axiom:nonsolid"),
      ResourceLocation.parse("axiom:plants"),
      ResourceLocation.parse("axiom:solid"),
      ResourceLocation.parse("axiom:stained_glass"),
      ResourceLocation.parse("axiom:underwater_plants")
   );

   private static List<ResourceLocation> sortedList(Set<ResourceLocation> set) {
      if (set == null) {
         return null;
      } else {
         List<ResourceLocation> list = new ArrayList<>(set);
         list.sort(ResourceLocation::compareTo);
         return list;
      }
   }

   public BlockList() {
      this.searchedBlocks = this.blocksAll = List.of();
      this.blocksAllLengthSorted = List.of();
      this.searchedTags = this.tagsAll = List.of();
      this.tagsAllLengthSorted = List.of();
      NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> {
         if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) {
            this.markNeedsReload();
         }
      });
   }

   public void markNeedsReload() {
      this.needsReload = true;
   }

   private void doReload() {
      this.needsReload = false;
      ArrayList<BlockList.Entry> blocksAll = new ArrayList<>();
      Set<Block> processedBlocks = new HashSet<>();
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         boolean result = CreativeModeTabs.tryRebuildTabContents(
            player.connection.enabledFeatures(),
            player.canUseGameMasterBlocks() && (Boolean)Minecraft.getInstance().options.operatorItemsTab().get(),
            player.level().registryAccess()
         );
         if (result) {
            SessionSearchTrees sessionSearchTrees = player.connection.searchTrees();
            List<ItemStack> list = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
            sessionSearchTrees.updateCreativeTooltips(player.level().registryAccess(), list);
            sessionSearchTrees.updateCreativeTags(list);
         }
      }

      for (Block importantBlock : ImportantBlocks.IMPORTANT_BLOCKS) {
         addBlockToAll(blocksAll, processedBlocks, importantBlock, true);
      }

      for (Map.Entry<ResourceLocation, CustomBlock> mapEntry : ServerCustomBlocks.customBlockMap.entrySet()) {
         CustomBlockState state = mapEntry.getValue().axiom$defaultCustomState();
         String name = AxiomI18n.get(mapEntry.getValue().axiom$translationKey());
         BlockList.Entry entry = new BlockList.Entry(
            state, mapEntry.getKey(), mapEntry.getKey().getPath(), createSearchKey(name), mapEntry.getValue().axiom$translationKey()
         );
         blocksAll.add(entry);
      }

      for (ItemStack stack : CreativeModeTabs.searchTab().getSearchTabDisplayItems()) {
         Block block = Block.byItem(stack.getItem());
         addBlockToAll(blocksAll, processedBlocks, block, false);
      }

      for (Block block : BuiltInRegistries.BLOCK) {
         addBlockToAll(blocksAll, processedBlocks, block, false);
      }

      this.searchedBlocks = this.blocksAll = Collections.unmodifiableList(blocksAll);
      ArrayList<BlockList.Entry> blocksAllLengthSorted = new ArrayList<>(blocksAll);
      blocksAllLengthSorted.sort(Comparator.comparingInt(entry -> entry.location.getPath().length()));
      this.blocksAllLengthSorted = Collections.unmodifiableList(blocksAllLengthSorted);
      List<BlockList.TagEntry> tagsAll = BuiltInRegistries.BLOCK
         .getTags()
         .sorted(
            Comparator.<Pair, Boolean>comparing(o -> WeirdTags.isWeird((TagKey<Block>)o.getFirst()))
               .thenComparing(o -> !((TagKey)o.getFirst()).location().getNamespace().equals("axiom"))
               .thenComparingInt(o -> -((Named)o.getSecond()).size())
               .thenComparing(o -> ((TagKey)o.getFirst()).location().getPath())
         )
         .map(
            e -> new BlockList.TagEntry(
               new BlockList.MinecraftOrCustomTagSet(((TagKey)e.getFirst()).location(), (HolderSet<Block>)e.getSecond(), null),
               ((TagKey)e.getFirst()).location(),
               createSearchKey(((TagKey)e.getFirst()).location().toString())
            )
         )
         .toList();
      if (Axiom.configuration.internal.rootEditorPalette != null) {
         List<BlockList.TagEntry> customTags = new ArrayList<>();
         addCustomTags("", Axiom.configuration.internal.rootEditorPalette, customTags);
         customTags.addAll(tagsAll);
         tagsAll = Collections.unmodifiableList(customTags);
      }

      List<ResourceLocation> builtinTagNames = new ArrayList<>(BUILTIN_AXIOM_TAGS);

      for (DyeColor dyeColor : DyeColor.values()) {
         builtinTagNames.add(ResourceLocation.parse("color:" + dyeColor.getName()));
      }

      for (String value : MaterialBlockTagGenerator.blockToMaterialMap.values()) {
         builtinTagNames.add(ResourceLocation.parse("material:" + value));
      }

      for (String value : MaterialBlockTagGenerator.setTypeToMaterialMap.values()) {
         builtinTagNames.add(ResourceLocation.parse("material:" + value));
      }

      for (String value : MaterialBlockTagGenerator.woodTypeToMaterialMap.values()) {
         builtinTagNames.add(ResourceLocation.parse("material:" + value));
      }

      List<BlockList.TagEntry> builtinTags = new ArrayList<>();

      label68:
      for (ResourceLocation builtinAxiomTag : builtinTagNames) {
         for (BlockList.TagEntry tagEntry : tagsAll) {
            if (tagEntry.tag.name.equals(builtinAxiomTag)) {
               continue label68;
            }
         }

         Set<ResourceLocation> clientTag = ClientTagsHelper.getOrCreateLocalTag(TagKey.create(Registries.BLOCK, builtinAxiomTag));
         if (clientTag != null && !clientTag.isEmpty()) {
            String searchKey = createSearchKey(builtinAxiomTag.toString());
            builtinTags.add(new BlockList.TagEntry(new BlockList.MinecraftOrCustomTagSet(builtinAxiomTag, null, clientTag), builtinAxiomTag, searchKey));
         }
      }

      if (!builtinTags.isEmpty()) {
         builtinTags.addAll(tagsAll);
         tagsAll = Collections.unmodifiableList(builtinTags);
      }

      this.searchedTags = this.tagsAll = tagsAll;
      ArrayList<BlockList.TagEntry> tagsAllLengthSorted = new ArrayList<>(tagsAll);
      tagsAllLengthSorted.sort(Comparator.comparingInt(entry -> entry.tag.name.getPath().length()));
      this.tagsAllLengthSorted = Collections.unmodifiableList(tagsAllLengthSorted);
      this.lastNameSearch = "";
      this.lastFilteredTags = Set.of();
      this.lastFilteredNamespaces = Set.of();
      this.lastSearchRaw = "";
      this.lastFilter = null;
   }

   private static void addCustomTags(String prefix, EditorPalette editorPalette, List<BlockList.TagEntry> tags) {
      for (EditorPalette subcategory : editorPalette.getSubcategories()) {
         String name = subcategory.getName().toLowerCase(Locale.ROOT);
         StringBuilder pathBuilder = new StringBuilder();

         for (char c : name.toCharArray()) {
            if (ResourceLocation.validPathChar(c)) {
               pathBuilder.append(c);
            }
         }

         String currentPath = pathBuilder.toString();
         if (currentPath.isEmpty()) {
            currentPath = "unknown";
         }

         currentPath = prefix + currentPath;
         if (!subcategory.getBlocks().isEmpty()) {
            Set<ResourceLocation> blocks = new HashSet<>();

            for (CustomBlockStateOrTombstone block : subcategory.getBlocks()) {
               if (block instanceof CustomBlockState customBlockState) {
                  blocks.add(customBlockState.getVanillaState().getBlock().builtInRegistryHolder().key().location());
               }
            }

            if (!blocks.isEmpty()) {
               ResourceLocation location = ResourceLocation.fromNamespaceAndPath("custom", currentPath);
               String searchKey = createSearchKey(location.toString());
               tags.add(new BlockList.TagEntry(new BlockList.MinecraftOrCustomTagSet(location, null, blocks), location, searchKey));
            }
         }

         addCustomTags(prefix + currentPath + "/", subcategory, tags);
      }
   }

   private static void addBlockToAll(ArrayList<BlockList.Entry> all, Set<Block> processedBlocks, Block block, boolean allowAir) {
      if (!processedBlocks.contains(block)) {
         if (allowAir || !block.defaultBlockState().isAir()) {
            if (!(block instanceof MovingPistonBlock)) {
               BlockList.Entry entry = createEntry(block.defaultBlockState());
               all.add(entry);
               processedBlocks.add(block);
            }
         }
      }
   }

   public static BlockList.Entry createEntry(BlockState blockState) {
      Block block = blockState.getBlock();
      ResourceLocation location = BuiltInRegistries.BLOCK.getKey(block);
      CustomBlockState state = (CustomBlockState)DefaultBlocks.applyDefaultProperties(blockState);
      String name = AxiomI18n.get(block.getDescriptionId());
      return new BlockList.Entry(state, location, location.getPath(), createSearchKey(name), block.getDescriptionId());
   }

   private static String createSearchKey(String string) {
      StringBuilder searchKey = new StringBuilder();

      for (char c : string.toLowerCase(Locale.ROOT).toCharArray()) {
         if (!Character.isWhitespace(c)) {
            searchKey.append(c);
         }
      }

      return searchKey.toString();
   }

   public void search(String search) {
      this.search(search, null);
   }

   public void search(String search, Predicate<CustomBlockState> filter) {
      this.search(search, filter, false);
   }

   public void search(String search, Predicate<CustomBlockState> filter, boolean force) {
      if (this.needsReload) {
         this.doReload();
         force = true;
      }

      search = search.toLowerCase(Locale.ROOT);
      if (force || !search.equals(this.lastSearchRaw) || filter != this.lastFilter) {
         this.lastSearchRaw = search;
         Set<String> filteredTags = new HashSet<>();
         Set<String> filteredNamespaces = new HashSet<>();
         StringBuilder searchBuilder = new StringBuilder();
         String[] split = search.split(" ");

         for (String s : split) {
            if (s.startsWith("#")) {
               String tag = s.substring(1);
               if (!tag.isEmpty()) {
                  filteredTags.add(tag);
               }
            } else if (s.startsWith("@")) {
               String namespace = s.substring(1);
               if (!namespace.isEmpty()) {
                  filteredNamespaces.add(namespace);
               }
            } else {
               for (char c : s.toCharArray()) {
                  if (!Character.isWhitespace(c)) {
                     searchBuilder.append(c);
                  }
               }
            }
         }

         search = searchBuilder.toString();
         boolean forceBlock = force || !filteredTags.equals(this.lastFilteredTags) || !filteredNamespaces.equals(this.lastFilteredNamespaces);
         if (forceBlock || !search.equals(this.lastNameSearch) || filter != this.lastFilter) {
            this.searchTags(search, filteredTags, force);
            Set<Block> tagFilteredBlocks = filteredTags.isEmpty() ? null : new HashSet<>();
            Set<ResourceLocation> tagFilteredLocations = filteredTags.isEmpty() ? null : new HashSet<>();
            if (!filteredTags.isEmpty()) {
               for (BlockList.TagEntry searchedTag : this.searchedTags) {
                  HolderSet<Block> minecraft = searchedTag.tag.minecraft;
                  if (minecraft != null) {
                     for (Holder<Block> blockHolder : minecraft) {
                        tagFilteredBlocks.add((Block)blockHolder.value());
                     }
                  }

                  Set<ResourceLocation> custom = searchedTag.tag.customSet;
                  if (custom != null) {
                     tagFilteredLocations.addAll(custom);
                  }
               }
            }

            if (search.isBlank()) {
               this.doBlockSearchBlank(filter, tagFilteredBlocks, tagFilteredLocations, filteredNamespaces);
            } else {
               this.doBlockSearchByName(search, filter, tagFilteredBlocks, tagFilteredLocations, filteredNamespaces, forceBlock);
            }

            this.lastNameSearch = search;
            this.lastFilteredNamespaces = filteredNamespaces;
            this.lastFilteredTags = filteredTags;
            this.lastFilter = filter;
         }
      }
   }

   private boolean shouldSearchTagsFromScratch(Set<String> filteredTags) {
      if (this.searchedTags instanceof JoinedList<BlockList.TagEntry> joined && joined.first() == this.tagsStartsWith && joined.second() == this.tagsContains) {
         for (String filteredTag : filteredTags) {
            boolean hasStartsWith = false;

            for (String lastEffectiveFilteredTag : this.lastEffectiveFilteredTags) {
               if (filteredTag.startsWith(lastEffectiveFilteredTag)) {
                  hasStartsWith = true;
                  break;
               }
            }

            if (!hasStartsWith) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private void searchTags(String search, Set<String> filteredTags, boolean force) {
      if (filteredTags.isEmpty()) {
         if (search.isBlank()) {
            this.searchedTags = this.tagsAll;
            return;
         }

         filteredTags = Set.of(search);
      }

      if (!force && !this.shouldSearchTagsFromScratch(filteredTags)) {
         Set<String> filteredTagsF = filteredTags;
         this.tagsContains.removeIf(entryx -> {
            for (String filteredTagx : filteredTagsF) {
               if (entryx.searchKey.contains(filteredTagx)) {
                  return false;
               }
            }

            return true;
         });
         this.tagsStartsWith.removeIf(entryx -> {
            boolean containsx = false;

            for (String filteredTagx : filteredTagsF) {
               if (entryx.location.getPath().startsWith(filteredTagx)) {
                  return false;
               }

               if (entryx.searchKey.contains(filteredTagx)) {
                  containsx = true;
               }
            }

            if (containsx) {
               this.tagsContains.add(entryx);
            }

            return true;
         });
      } else {
         this.tagsStartsWith.clear();
         this.tagsContains.clear();

         for (BlockList.TagEntry entry : this.tagsAllLengthSorted) {
            boolean contains = false;

            for (String filteredTag : filteredTags) {
               if (entry.location.getPath().startsWith(filteredTag)) {
                  this.tagsStartsWith.add(entry);
                  contains = false;
                  break;
               }

               if (entry.searchKey.contains(filteredTag)) {
                  contains = true;
               }
            }

            if (contains) {
               this.tagsContains.add(entry);
            }
         }

         this.searchedTags = new JoinedList<>(this.tagsStartsWith, this.tagsContains);
      }

      this.lastEffectiveFilteredTags = filteredTags;
   }

   private void doBlockSearchBlank(
      Predicate<CustomBlockState> filter, Set<Block> tagFilteredBlocks, Set<ResourceLocation> tagFilteredLocations, Set<String> filteredNamespaces
   ) {
      boolean hasTagFiltered = tagFilteredBlocks != null || tagFilteredLocations != null;
      if (hasTagFiltered) {
         if (tagFilteredBlocks == null) {
            tagFilteredBlocks = Set.of();
         }

         if (tagFilteredLocations == null) {
            tagFilteredLocations = Set.of();
         }
      }

      if (filter == null && !hasTagFiltered && filteredNamespaces.isEmpty()) {
         this.searchedBlocks = this.blocksAll;
      } else {
         this.searchedBlocks = new ArrayList<>();
         if (!hasTagFiltered || !tagFilteredBlocks.isEmpty() || !tagFilteredLocations.isEmpty()) {
            Iterator var6 = this.blocksAll.iterator();

            while (true) {
               BlockList.Entry entry;
               while (true) {
                  if (!var6.hasNext()) {
                     return;
                  }

                  entry = (BlockList.Entry)var6.next();
                  if (filter == null || filter.test(entry.state)) {
                     if (filteredNamespaces.isEmpty()) {
                        break;
                     }

                     boolean namespaceMatch = false;

                     for (String filteredNamespace : filteredNamespaces) {
                        if (entry.location.getNamespace().startsWith(filteredNamespace)) {
                           namespaceMatch = true;
                           break;
                        }
                     }

                     if (namespaceMatch) {
                        break;
                     }
                  }
               }

               if (hasTagFiltered) {
                  boolean matchesTagFilter = false;
                  if (entry.state instanceof BlockState blockState && tagFilteredBlocks.contains(blockState.getBlock())) {
                     matchesTagFilter = true;
                  }

                  if (!matchesTagFilter && tagFilteredLocations.contains(entry.location)) {
                     matchesTagFilter = true;
                  }

                  if (!matchesTagFilter) {
                     continue;
                  }
               }

               this.searchedBlocks.add(entry);
            }
         }
      }
   }

   private void doBlockSearchByName(
      String search,
      Predicate<CustomBlockState> filter,
      Set<Block> tagFilteredBlocks,
      Set<ResourceLocation> tagFilteredLocations,
      Set<String> filteredNamespaces,
      boolean force
   ) {
      boolean hasTagFiltered = tagFilteredBlocks != null || tagFilteredLocations != null;
      if (hasTagFiltered) {
         if (tagFilteredBlocks == null) {
            tagFilteredBlocks = Set.of();
         }

         if (tagFilteredLocations == null) {
            tagFilteredLocations = Set.of();
         }
      }

      if (!force
         && this.searchedBlocks instanceof JoinedList<BlockList.Entry> joined
         && joined.first() == this.blocksStartsWith
         && joined.second() == this.blocksContains
         && search.startsWith(this.lastNameSearch)
         && filter == this.lastFilter) {
         String searchF = search;
         this.blocksContains.removeIf(entryx -> !entryx.searchKeyId.contains(searchF) && !entryx.searchKeyLocalized.contains(searchF));
         this.blocksStartsWith.removeIf(entryx -> {
            if (!entryx.searchKeyId.startsWith(searchF) && !entryx.searchKeyLocalized.startsWith(searchF)) {
               if (entryx.searchKeyId.contains(searchF) || entryx.searchKeyLocalized.contains(searchF)) {
                  this.blocksContains.add(entryx);
               }

               return true;
            } else {
               return false;
            }
         });
      } else {
         this.blocksStartsWith.clear();
         this.blocksContains.clear();
         if (!hasTagFiltered || !tagFilteredBlocks.isEmpty() || !tagFilteredLocations.isEmpty()) {
            Iterator var14 = this.blocksAllLengthSorted.iterator();

            while (true) {
               BlockList.Entry entry;
               while (true) {
                  if (!var14.hasNext()) {
                     this.searchedBlocks = new JoinedList<>(this.blocksStartsWith, this.blocksContains);
                     return;
                  }

                  entry = (BlockList.Entry)var14.next();
                  if (filter == null || filter.test(entry.state)) {
                     if (filteredNamespaces.isEmpty()) {
                        break;
                     }

                     boolean namespaceMatch = false;

                     for (String filteredNamespace : filteredNamespaces) {
                        if (entry.location.getNamespace().startsWith(filteredNamespace)) {
                           namespaceMatch = true;
                           break;
                        }
                     }

                     if (namespaceMatch) {
                        break;
                     }
                  }
               }

               if (hasTagFiltered) {
                  boolean matchesTagFilter = false;
                  if (entry.state instanceof BlockState blockState && tagFilteredBlocks.contains(blockState.getBlock())) {
                     matchesTagFilter = true;
                  }

                  if (!matchesTagFilter && tagFilteredLocations.contains(entry.location)) {
                     matchesTagFilter = true;
                  }

                  if (!matchesTagFilter) {
                     continue;
                  }
               }

               if (entry.searchKeyId.startsWith(search) || entry.searchKeyLocalized.startsWith(search)) {
                  this.blocksStartsWith.add(entry);
               } else if (entry.searchKeyId.contains(search) || entry.searchKeyLocalized.contains(search)) {
                  this.blocksContains.add(entry);
               }
            }
         }
      }
   }

   public String getLastSearchRaw() {
      return this.lastSearchRaw;
   }

   public List<BlockList.Entry> getBlocks() {
      if (this.needsReload) {
         this.doReload();
      }

      return this.searchedBlocks;
   }

   public List<BlockList.TagEntry> getTags() {
      if (this.needsReload) {
         this.doReload();
      }

      return this.searchedTags;
   }

   public record Entry(CustomBlockState state, ResourceLocation location, String searchKeyId, String searchKeyLocalized, String translationKey) {
   }

   public record MinecraftOrCustomTagSet(
      ResourceLocation name, @Nullable HolderSet<Block> minecraft, @Nullable List<ResourceLocation> customList, @Nullable Set<ResourceLocation> customSet
   ) {
      public MinecraftOrCustomTagSet(ResourceLocation name, @Nullable HolderSet<Block> minecraft, @Nullable Set<ResourceLocation> customSet) {
         this(name, minecraft, BlockList.sortedList(customSet), customSet);
      }
   }

   public record TagEntry(BlockList.MinecraftOrCustomTagSet tag, ResourceLocation location, String searchKey) {
   }
}
