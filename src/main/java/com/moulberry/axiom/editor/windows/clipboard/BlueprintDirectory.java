package com.moulberry.axiom.editor.windows.clipboard;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import org.jetbrains.annotations.Nullable;

public final class BlueprintDirectory {
   private PathWrapper path;
   private final String dirName;
   private BlueprintDirectory parent;
   private final List<BlueprintDirectory> children;
   private Map<PathWrapper, BlueprintOrDirectory> blueprints;
   private boolean orderingChanged = false;
   private String lastSearch = "";
   private Set<String> lastFilterTags = new HashSet<>();
   private Map<PathWrapper, BlueprintOrDirectory> searchedBlueprints;
   private BlueprintOrDirectory head = null;
   private BlueprintOrDirectory tail = null;
   private BlueprintOrDirectory searchHead = null;
   private BlueprintOrDirectory searchVisibleStart = null;
   private int searchVisibleStartIndex = 0;
   private BlueprintOrDirectory searchTail = null;
   private final Map<String, Integer> tagCounts = new TreeMap<>();

   public BlueprintDirectory(PathWrapper path, String dirName) {
      this.path = path;
      this.dirName = dirName;
      this.children = new ArrayList<>();
      this.blueprints = new HashMap<>();
      this.searchedBlueprints = this.blueprints;
   }

   public PathWrapper path() {
      return this.path;
   }

   public String dirName() {
      return this.dirName;
   }

   public BlueprintDirectory parent() {
      return this.parent;
   }

   public Map<String, Integer> tagsWithCount() {
      return this.tagCounts;
   }

   public Set<String> tags() {
      return this.tagCounts.keySet();
   }

   @Nullable
   public BlueprintOrDirectory head() {
      return this.head;
   }

   public List<BlueprintDirectory> children() {
      return this.children;
   }

   public Map<PathWrapper, BlueprintOrDirectory> blueprints() {
      return this.blueprints;
   }

   public Map<PathWrapper, BlueprintOrDirectory> searchedBlueprints() {
      return this.searchedBlueprints;
   }

   public BlueprintOrDirectory setSearchVisibleStart(int index) {
      if (index < 0) {
         throw new IllegalArgumentException();
      } else if (index >= this.searchedBlueprints.size()) {
         return null;
      } else {
         if (this.searchVisibleStart == null || index != this.searchVisibleStartIndex) {
            int distanceToSearchVisibleStart = Math.abs(index - this.searchVisibleStartIndex);
            int distanceToHead = Math.abs(index);
            int distanceToTail = Math.abs(index - (this.searchedBlueprints.size() - 1));
            if (this.searchVisibleStart != null && distanceToSearchVisibleStart < distanceToHead && distanceToSearchVisibleStart < distanceToTail) {
               if (index < this.searchVisibleStartIndex) {
                  for (int i = 0; i < distanceToSearchVisibleStart; i++) {
                     BlueprintOrDirectory prev = this.searchVisibleStart.prevSearchNode;
                     if (prev == null) {
                        break;
                     }

                     this.searchVisibleStart = prev;
                  }
               } else {
                  for (int i = 0; i < distanceToSearchVisibleStart; i++) {
                     BlueprintOrDirectory next = this.searchVisibleStart.nextSearchNode;
                     if (next == null) {
                        break;
                     }

                     this.searchVisibleStart = next;
                  }
               }
            } else if (distanceToHead < distanceToTail) {
               this.searchVisibleStart = this.searchHead;

               for (int i = 0; i < distanceToHead; i++) {
                  this.searchVisibleStart = this.searchVisibleStart.nextSearchNode;
               }
            } else {
               this.searchVisibleStart = this.searchTail;

               for (int i = 0; i < distanceToTail; i++) {
                  this.searchVisibleStart = this.searchVisibleStart.prevSearchNode;
               }
            }

            this.searchVisibleStartIndex = index;
         }

         return this.searchVisibleStart;
      }
   }

   public void setPath(PathWrapper newPath) {
      if (this.path.real() != null && newPath.real() != null) {
         Path oldPath = this.path.real();
         this.path = newPath;
         Map<PathWrapper, BlueprintOrDirectory> blueprints = new HashMap<>();

         for (Entry<PathWrapper, BlueprintOrDirectory> entry : this.blueprints.entrySet()) {
            BlueprintOrDirectory blueprintOrDirectory = entry.getValue();
            Path relative = oldPath.relativize(entry.getKey().real());
            Path newAbsolute = newPath.real().resolve(relative);
            blueprintOrDirectory.path(newAbsolute);
            blueprints.put(new PathWrapper(newAbsolute, null), blueprintOrDirectory);
            blueprintOrDirectory.prevSearchNode = blueprintOrDirectory.prevNode;
            blueprintOrDirectory.nextSearchNode = blueprintOrDirectory.nextNode;
         }

         this.lastSearch = "";
         this.searchHead = this.head;
         this.searchTail = this.tail;
         this.searchedBlueprints = this.blueprints = blueprints;
      } else {
         throw new UnsupportedOperationException("Unsupported on server blueprints");
      }
   }

   public void sort(BlueprintDirectory.SortMode sortMode) {
      if (this.head != null) {
         List<BlueprintOrDirectory> allBlueprintsList = new ArrayList<>();

         for (BlueprintOrDirectory blueprintOrDirectory = this.head; blueprintOrDirectory != null; blueprintOrDirectory = blueprintOrDirectory.nextNode) {
            allBlueprintsList.add(blueprintOrDirectory);
         }

         allBlueprintsList.sort(sortMode.comparator);
         this.head = this.searchHead = allBlueprintsList.get(0);
         this.tail = this.searchTail = allBlueprintsList.get(allBlueprintsList.size() - 1);
         BlueprintOrDirectory last = null;

         for (BlueprintOrDirectory current : allBlueprintsList) {
            current.prevNode = current.prevSearchNode = last;
            if (last != null) {
               last.nextNode = last.nextSearchNode = current;
            }

            last = current;
         }

         last.nextNode = null;
         last.nextSearchNode = null;
         this.searchVisibleStart = null;
         this.searchVisibleStartIndex = -1;
         String lastSearch = this.lastSearch;
         this.lastSearch = "";
         this.search(lastSearch, this.lastFilterTags);
         this.orderingChanged = true;
         BlueprintBrowserWindow.anyOrderUpdated = true;
      }
   }

   public void addRecursiveSearch(String search, List<BlueprintOrDirectory.Bp> results, Set<String> filterTags) {
      List<BlueprintOrDirectory.Dir> children = new ArrayList<>();

      for (BlueprintOrDirectory blueprintOrDirectory = this.head; blueprintOrDirectory != null; blueprintOrDirectory = blueprintOrDirectory.nextNode) {
         if (blueprintOrDirectory.containsAllTags(filterTags)) {
            if (blueprintOrDirectory instanceof BlueprintOrDirectory.Dir dir) {
               children.add(dir);
            } else if (blueprintOrDirectory instanceof BlueprintOrDirectory.Bp bp && bp.nameContainsLower(search)) {
               results.add(bp);
            }
         }
      }

      for (BlueprintOrDirectory.Dir dir : children) {
         dir.blueprintDirectory.addRecursiveSearch(search, results, filterTags);
      }
   }

   public void search(String search, Set<String> filterTags) {
      search = search.toLowerCase(Locale.ROOT);
      if (!search.equals(this.lastSearch) || !filterTags.equals(this.lastFilterTags)) {
         if (search.isBlank() && filterTags.isEmpty()) {
            boolean findingSearchVisible = this.searchVisibleStart != null;
            int searchVisibleRemaining = this.searchVisibleStartIndex;

            for (BlueprintOrDirectory blueprintOrDirectory = this.head; blueprintOrDirectory != null; blueprintOrDirectory = blueprintOrDirectory.nextNode) {
               blueprintOrDirectory.prevSearchNode = blueprintOrDirectory.prevNode;
               blueprintOrDirectory.nextSearchNode = blueprintOrDirectory.nextNode;
               if (findingSearchVisible) {
                  if (searchVisibleRemaining == 0) {
                     this.searchVisibleStart = blueprintOrDirectory;
                     findingSearchVisible = false;
                  } else {
                     searchVisibleRemaining--;
                  }
               }
            }

            this.searchHead = this.head;
            this.searchTail = this.tail;
            this.searchedBlueprints = this.blueprints;
         } else if (this.searchedBlueprints != this.blueprints && search.startsWith(this.lastSearch) && filterTags.containsAll(this.lastFilterTags)) {
            String searchF = search;
            var filterTagsF = filterTags;
            this.searchedBlueprints.values().removeIf(entry -> {
               if (entry.nameContainsLower(searchF) && entry.containsAllTags(filterTagsF)) {
                  return false;
               } else {
                  if (entry.prevSearchNode != null) {
                     entry.prevSearchNode.nextSearchNode = entry.nextSearchNode;
                  }

                  if (entry.nextSearchNode != null) {
                     entry.nextSearchNode.prevSearchNode = entry.prevSearchNode;
                  }

                  if (entry == this.searchHead) {
                     this.searchHead = entry.nextSearchNode;
                  }

                  if (entry == this.searchTail) {
                     this.searchTail = entry.prevSearchNode;
                  }

                  if (entry == this.searchVisibleStart) {
                     this.searchVisibleStart = entry.nextSearchNode;
                  }

                  entry.nextSearchNode = null;
                  entry.prevSearchNode = null;
                  return true;
               }
            });
         } else {
            Map<PathWrapper, BlueprintOrDirectory> searchedBlueprints = new HashMap<>();
            boolean findingSearchVisible = this.searchVisibleStart != null;
            int searchVisibleRemaining = this.searchVisibleStartIndex;
            BlueprintOrDirectory blueprintOrDirectoryx = this.head;

            BlueprintOrDirectory lastMatching;
            for (lastMatching = null; blueprintOrDirectoryx != null; blueprintOrDirectoryx = blueprintOrDirectoryx.nextNode) {
               blueprintOrDirectoryx.prevSearchNode = null;
               blueprintOrDirectoryx.nextSearchNode = null;
               if (blueprintOrDirectoryx.nameContainsLower(search) && blueprintOrDirectoryx.containsAllTags(filterTags)) {
                  if (lastMatching == null) {
                     this.searchHead = blueprintOrDirectoryx;
                  } else {
                     lastMatching.nextSearchNode = blueprintOrDirectoryx;
                     blueprintOrDirectoryx.prevSearchNode = lastMatching;
                  }

                  if (findingSearchVisible) {
                     if (searchVisibleRemaining == 0) {
                        this.searchVisibleStart = blueprintOrDirectoryx;
                        findingSearchVisible = false;
                     } else {
                        searchVisibleRemaining--;
                     }
                  }

                  searchedBlueprints.put(blueprintOrDirectoryx.path(), blueprintOrDirectoryx);
                  lastMatching = blueprintOrDirectoryx;
               }
            }

            this.searchedBlueprints = searchedBlueprints;
            this.searchTail = lastMatching;
         }

         this.lastSearch = search;
         this.lastFilterTags = new HashSet<>(filterTags);
      }
   }

   public void saveOrdering() {
      if (this.orderingChanged) {
         if (this.path.real() != null) {
            Path dotfile = this.path.real().resolve(".axiom_blueprint_ordering");
            if (this.blueprints.size() <= 1) {
               try {
                  Files.deleteIfExists(dotfile);
               } catch (IOException var7) {
               }
            } else {
               try (BufferedWriter writer = Files.newBufferedWriter(dotfile)) {
                  for (BlueprintOrDirectory blueprintOrDirectory = this.head;
                     blueprintOrDirectory != null;
                     blueprintOrDirectory = blueprintOrDirectory.nextNode
                  ) {
                     Path path = blueprintOrDirectory.path().real();
                     Path relative = this.path.real().relativize(path);
                     if (blueprintOrDirectory != this.head) {
                        writer.write(10);
                     }

                     writer.write(relative.toString().replace(path.getFileSystem().getSeparator(), "/"));
                  }
               } catch (IOException var9) {
                  throw new UncheckedIOException(var9);
               }

               this.orderingChanged = false;
            }
         }
      }
   }

   public void reposition(BlueprintOrDirectory blueprintOrDirectory, BlueprintOrDirectory reference, boolean before) {
      if (blueprintOrDirectory != reference) {
         if (this.head == blueprintOrDirectory) {
            this.head = blueprintOrDirectory.nextNode;
         }

         if (this.tail == blueprintOrDirectory) {
            this.tail = blueprintOrDirectory.prevNode;
         }

         if (this.searchHead == blueprintOrDirectory) {
            this.searchHead = blueprintOrDirectory.nextSearchNode;
         }

         if (this.searchTail == blueprintOrDirectory) {
            this.searchTail = blueprintOrDirectory.prevSearchNode;
         }

         if (this.searchVisibleStart == blueprintOrDirectory) {
            this.searchVisibleStart = blueprintOrDirectory.nextSearchNode;
         }

         blueprintOrDirectory.unlink();
         if (before) {
            if (blueprintOrDirectory == reference.prevNode) {
               return;
            }

            if (blueprintOrDirectory.nextNode == reference) {
               return;
            }

            if (this.searchVisibleStart == reference) {
               this.searchVisibleStart = blueprintOrDirectory;
            }

            if (this.head == reference) {
               this.head = blueprintOrDirectory;
            }

            if (reference.prevNode != null) {
               reference.prevNode.nextNode = blueprintOrDirectory;
            }

            blueprintOrDirectory.prevNode = reference.prevNode;
            reference.prevNode = blueprintOrDirectory;
            blueprintOrDirectory.nextNode = reference;
            if (this.searchHead == reference) {
               this.searchHead = blueprintOrDirectory;
            }

            if (reference.prevSearchNode != null) {
               reference.prevSearchNode.nextSearchNode = blueprintOrDirectory;
            }

            blueprintOrDirectory.prevSearchNode = reference.prevSearchNode;
            reference.prevSearchNode = blueprintOrDirectory;
            blueprintOrDirectory.nextSearchNode = reference;
         } else {
            if (blueprintOrDirectory == reference.nextNode) {
               return;
            }

            if (blueprintOrDirectory.prevNode == reference) {
               return;
            }

            if (this.tail == reference) {
               this.tail = blueprintOrDirectory;
            }

            if (reference.nextNode != null) {
               reference.nextNode.prevNode = blueprintOrDirectory;
            }

            blueprintOrDirectory.nextNode = reference.nextNode;
            reference.nextNode = blueprintOrDirectory;
            blueprintOrDirectory.prevNode = reference;
            if (this.searchTail == reference) {
               this.searchTail = blueprintOrDirectory;
            }

            if (reference.nextSearchNode != null) {
               reference.nextSearchNode.prevSearchNode = blueprintOrDirectory;
            }

            blueprintOrDirectory.nextSearchNode = reference.nextSearchNode;
            reference.nextSearchNode = blueprintOrDirectory;
            blueprintOrDirectory.prevSearchNode = reference;
         }

         this.orderingChanged = true;
         BlueprintBrowserWindow.anyOrderUpdated = true;
      }
   }

   public void changeChildPath(Path from, Path to) {
      if (this.path.real() == null) {
         throw new UnsupportedOperationException("Unsupported on server blueprints");
      } else if (!from.getParent().equals(this.path.real())) {
         throw new FaultyImplementationError();
      } else if (!to.getParent().equals(this.path.real())) {
         throw new FaultyImplementationError();
      } else {
         BlueprintOrDirectory item = this.blueprints.remove(new PathWrapper(from, null));
         if (item != null) {
            item.path(to);
            this.blueprints.put(new PathWrapper(to, null), item);
            if (this.searchedBlueprints != this.blueprints && item.nameContainsLower(this.lastSearch)) {
               BlueprintOrDirectory searchedItem = this.searchedBlueprints.remove(new PathWrapper(from, null));
               if (searchedItem != null) {
                  this.searchedBlueprints.put(new PathWrapper(to, null), searchedItem);
               }
            }

            this.orderingChanged = true;
            BlueprintBrowserWindow.anyOrderUpdated = true;
         }
      }
   }

   public void addTagCounts(Map<String, Integer> counts, boolean subtract) {
      if (subtract) {
         for (Entry<String, Integer> tag : counts.entrySet()) {
            this.tagCounts.computeIfPresent(tag.getKey(), (tag1, count) -> {
               count = count - tag.getValue();
               return count <= 0 ? null : count;
            });
         }
      } else {
         for (Entry<String, Integer> tag : counts.entrySet()) {
            this.tagCounts.compute(tag.getKey(), (tag1, count) -> count == null ? tag.getValue() : count + tag.getValue());
         }
      }

      if (this.parent != null) {
         this.parent.addTagCounts(counts, subtract);
      }
   }

   public boolean remove(PathWrapper path) {
      if (this.path.real() != null && path.real() != null) {
         BlueprintOrDirectory blueprintOrDirectory = this.blueprints.remove(path);
         if (blueprintOrDirectory == null) {
            return false;
         } else {
            if (this.searchedBlueprints != this.blueprints) {
               this.searchedBlueprints.remove(path);
            }

            if (this.head == blueprintOrDirectory) {
               this.head = blueprintOrDirectory.nextNode;
            }

            if (this.tail == blueprintOrDirectory) {
               this.tail = blueprintOrDirectory.prevNode;
            }

            if (this.searchHead == blueprintOrDirectory) {
               this.searchHead = blueprintOrDirectory.nextSearchNode;
            }

            if (this.searchTail == blueprintOrDirectory) {
               this.searchTail = blueprintOrDirectory.prevSearchNode;
            }

            if (this.searchVisibleStart == blueprintOrDirectory) {
               this.searchVisibleStart = blueprintOrDirectory.nextSearchNode;
            }

            blueprintOrDirectory.unlink();
            if (blueprintOrDirectory instanceof BlueprintOrDirectory.Dir dir) {
               this.children.remove(dir.blueprintDirectory);
               dir.blueprintDirectory.parent = null;
               if (dir.blueprintDirectory == BlueprintBrowserWindow.pendingSelectDirectory) {
                  BlueprintBrowserWindow.pendingSelectDirectory = null;
               }

               this.addTagCounts(dir.blueprintDirectory.tagCounts, true);
            } else if (blueprintOrDirectory instanceof BlueprintOrDirectory.Bp bp) {
               Map<String, Integer> map = new HashMap<>();

               for (String tag : bp.blueprint.tags()) {
                  map.put(tag, 1);
               }

               this.addTagCounts(map, true);
            }

            this.orderingChanged = true;
            BlueprintBrowserWindow.anyOrderUpdated = true;
            return true;
         }
      } else {
         throw new UnsupportedOperationException("Unsupported on server blueprints");
      }
   }

   public void addLast(BlueprintOrDirectory blueprintOrDirectory) {
      if (blueprintOrDirectory.prevNode == null
         && blueprintOrDirectory.nextNode == null
         && blueprintOrDirectory.prevSearchNode == null
         && blueprintOrDirectory.nextSearchNode == null) {
         BlueprintOrDirectory previous = this.blueprints.put(blueprintOrDirectory.path(), blueprintOrDirectory);
         if (this.searchedBlueprints != this.blueprints && blueprintOrDirectory.nameContainsLower(this.lastSearch)) {
            this.searchedBlueprints.put(blueprintOrDirectory.path(), blueprintOrDirectory);
         }

         if (previous != null) {
            if (previous instanceof BlueprintOrDirectory.Dir dir) {
               this.children.remove(dir.blueprintDirectory);
               this.addTagCounts(dir.blueprintDirectory.tagCounts, true);
            } else if (previous instanceof BlueprintOrDirectory.Bp bp) {
               Map<String, Integer> map = new HashMap<>();

               for (String tag : bp.blueprint.tags()) {
                  map.put(tag, 1);
               }

               this.addTagCounts(map, true);
            }

            if (this.head == previous) {
               this.head = blueprintOrDirectory;
            }

            if (this.tail == previous) {
               this.tail = blueprintOrDirectory;
            }

            if (this.searchHead == previous) {
               this.searchHead = blueprintOrDirectory;
            }

            if (this.searchTail == previous) {
               this.searchTail = blueprintOrDirectory;
            }

            if (this.searchVisibleStart == previous) {
               this.searchVisibleStart = blueprintOrDirectory;
            }

            if (previous.prevNode != null) {
               previous.prevNode.nextNode = blueprintOrDirectory;
            }

            if (previous.nextNode != null) {
               previous.nextNode.prevNode = blueprintOrDirectory;
            }

            if (previous.prevSearchNode != null) {
               previous.prevSearchNode.nextSearchNode = blueprintOrDirectory;
            }

            if (previous.nextSearchNode != null) {
               previous.nextSearchNode.prevSearchNode = blueprintOrDirectory;
            }

            blueprintOrDirectory.prevNode = previous.prevNode;
            blueprintOrDirectory.nextNode = previous.nextNode;
            blueprintOrDirectory.prevSearchNode = previous.prevSearchNode;
            blueprintOrDirectory.nextSearchNode = previous.nextSearchNode;
            previous.prevNode = null;
            previous.nextNode = null;
            previous.prevSearchNode = null;
            previous.nextSearchNode = null;
         } else {
            if (this.head == null) {
               this.head = blueprintOrDirectory;
            }

            if (this.tail != null) {
               this.tail.nextNode = blueprintOrDirectory;
            }

            blueprintOrDirectory.prevNode = this.tail;
            this.tail = blueprintOrDirectory;
            if (this.searchHead == null) {
               this.searchHead = blueprintOrDirectory;
            }

            if (this.searchTail != null) {
               this.searchTail.nextSearchNode = blueprintOrDirectory;
            }

            blueprintOrDirectory.prevSearchNode = this.searchTail;
            this.searchTail = blueprintOrDirectory;
         }

         if (blueprintOrDirectory instanceof BlueprintOrDirectory.Dir dir) {
            this.children.add(dir.blueprintDirectory);
            dir.blueprintDirectory.parent = this;
            this.addTagCounts(dir.blueprintDirectory.tagCounts, false);
         } else if (blueprintOrDirectory instanceof BlueprintOrDirectory.Bp bp) {
            Map<String, Integer> map = new HashMap<>();

            for (String tag : bp.blueprint.tags()) {
               map.put(tag, 1);
            }

            this.addTagCounts(map, false);
         }

         this.orderingChanged = true;
         BlueprintBrowserWindow.anyOrderUpdated = true;
      } else {
         throw new FaultyImplementationError("Can't add node which still has links!");
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (obj != null && obj.getClass() == this.getClass()) {
         BlueprintDirectory that = (BlueprintDirectory)obj;
         return Objects.equals(this.path, that.path);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.path.hashCode();
   }

   public static enum SortMode {
      NAME(
         Comparator.comparing(
            k -> k instanceof BlueprintOrDirectory.Bp bp ? bp.blueprint.name().toLowerCase(Locale.ROOT) : k.path().getFileName().toLowerCase(Locale.ROOT),
            new NaturalOrderComparator()
         )
      ),
      BLOCK_COUNT_ASC(Comparator.comparingInt(k -> k instanceof BlueprintOrDirectory.Bp bp ? bp.blueprint.blockCount() : 0)),
      BLOCK_COUNT_DESC(Comparator.comparingInt(k -> k instanceof BlueprintOrDirectory.Bp bp ? Integer.MAX_VALUE - bp.blueprint.blockCount() : 0));

      private final Comparator<BlueprintOrDirectory> comparator;

      private SortMode(Comparator<BlueprintOrDirectory> comparator) {
         this.comparator = comparator;
      }
   }
}
