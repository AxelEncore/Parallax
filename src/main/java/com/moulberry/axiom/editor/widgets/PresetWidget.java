package com.moulberry.axiom.editor.widgets;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.Authorization;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.FileUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

public class PresetWidget {
   private final Consumer<CompoundTag> loadSettings;
   private final Consumer<CompoundTag> writeSettings;
   private final Path path;
   private final int[] activePreset = new int[]{0};
   private final ImString createPresetName = new ImString(64);
   private PresetWidget.NamedPreset[] loadedPresets = new PresetWidget.NamedPreset[0];
   private String[] presetNames = new String[]{AxiomI18n.getOrDefault("axiom.preset.default", "Default")};
   private final SearchableCombo searchableCombo = new SearchableCombo(new String[0]);
   private boolean loaded = false;
   private Map<String, CompoundTag> defaultPresets;

   public PresetWidget(Consumer<CompoundTag> loadSettings, Consumer<CompoundTag> writeSettings, String identifier, Map<String, CompoundTag> defaultPresets) {
      this.loadSettings = loadSettings;
      this.writeSettings = writeSettings;
      this.path = Axiom.getInstance().getConfigDirectory().resolve("tool_presets").resolve(identifier);
      this.activePreset[0] = 0;
      this.defaultPresets = defaultPresets;
   }

   public PresetWidget(Consumer<CompoundTag> loadSettings, Consumer<CompoundTag> writeSettings, String identifier) {
      this(loadSettings, writeSettings, identifier, null);
   }

   public PresetWidget(Tool tool, String identifier) {
      this(tool::loadSettings, tool::writeSettings, identifier);
   }

   private Set<String> getLoadedDefaults() {
      try {
         Set<String> defaults = new HashSet<>();

         try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.path)) {
            for (Path child : stream) {
               if (child.endsWith(".defaults")) {
                  defaults.addAll(Files.readAllLines(child));
               }
            }
         }

         return defaults;
      } catch (Exception var7) {
         return Set.of();
      }
   }

   private void loadPresetsFromDisk(@Nullable Path select) {
      try {
         List<PresetWidget.NamedPreset> namedPresets = new ArrayList<>();

         try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.path)) {
            for (Path child : stream) {
               if (child.toString().endsWith(".nbt")) {
                  CompoundTag preset = NbtIo.readCompressed(child, NbtAccounter.unlimitedHeap());
                  Optional<String> nameOptional = VersionUtilsNbt.helperCompoundTagGetString(preset, "Name");
                  if (!nameOptional.isEmpty()) {
                     Optional<CompoundTag> settingsOptional = VersionUtilsNbt.helperCompoundTagGetCompound(preset, "Settings");
                     if (!settingsOptional.isEmpty()) {
                        String name = nameOptional.get();
                        CompoundTag settings = settingsOptional.get();
                        namedPresets.add(new PresetWidget.NamedPreset(name, child, settings));
                     }
                  }
               }
            }
         }

         namedPresets.sort(
            Comparator.<PresetWidget.NamedPreset, String>comparing(presetx -> ((PresetWidget.NamedPreset)presetx).name(), String::compareToIgnoreCase)
               .thenComparing(presetx -> ((PresetWidget.NamedPreset)presetx).path().toString(), String::compareToIgnoreCase)
         );
         List<String> presetNames = new ArrayList<>();
         presetNames.add(AxiomI18n.getOrDefault("axiom.preset.default", "Default"));
         int selectedIndex = -1;

         for (int i = 0; i < namedPresets.size(); i++) {
            PresetWidget.NamedPreset preset = namedPresets.get(i);
            presetNames.add(preset.name());
            if (preset.path.equals(select)) {
               selectedIndex = i + 1;
            }
         }

         this.activePreset[0] = selectedIndex;
         this.presetNames = presetNames.toArray(new String[0]);
         this.searchableCombo.setElements(this.presetNames);
         this.loadedPresets = namedPresets.toArray(new PresetWidget.NamedPreset[0]);
      } catch (Exception var13) {
         var13.printStackTrace();
         this.activePreset[0] = -1;
         this.presetNames = new String[]{AxiomI18n.getOrDefault("axiom.preset.default", "Default")};
         this.searchableCombo.setElements(this.presetNames);
         this.loadedPresets = new PresetWidget.NamedPreset[0];
      }
   }

   public int getPresetCount() {
      this.loadIfNeeded();
      return this.loadedPresets.length;
   }

   public void setDefault() {
      this.activePreset[0] = 0;
   }

   public void displayImgui(boolean changed) {
      this.displayImgui(changed, true);
   }

   public void displayImgui(boolean changed, boolean canCreatePreset) {
      this.loadIfNeeded();
      if (changed || this.activePreset[0] >= this.presetNames.length) {
         this.activePreset[0] = -1;
      }

      if (this.searchableCombo.render("##PresetCombo", this.activePreset)) {
         if (this.activePreset[0] == 0) {
            this.loadSettings.accept(new CompoundTag());
         } else if (this.activePreset[0] > 0) {
            this.loadSettings.accept(this.loadedPresets[this.activePreset[0] - 1].settings);
         }
      }

      if (ImGuiHelper.beginPopup("CreatePreset")) {
         ImGui.setNextItemWidth(160.0F);
         ImGui.inputText(AxiomI18n.get("axiom.preset.name"), this.createPresetName);
         if (ImGui.button(AxiomI18n.get("axiom.preset.save"))) {
            String name = ImGuiHelper.getString(this.createPresetName);
            Path saveTo = null;

            try {
               String filename = FileUtil.findAvailableName(this.path, name.toLowerCase(Locale.ROOT).replace(" ", "_"), ".nbt");
               CompoundTag settings = new CompoundTag();
               this.writeSettings.accept(settings);
               CompoundTag preset = new CompoundTag();
               preset.putString("Name", name);
               preset.putString("Version", Authorization.getUserAgent());
               preset.put("Settings", settings);
               saveTo = this.path.resolve(filename);
               NbtIo.writeCompressed(preset, saveTo);
            } catch (Exception var9) {
               var9.printStackTrace();
            }

            this.loadPresetsFromDisk(saveTo);
            ImGui.closeCurrentPopup();
         }

         ImGui.endPopup();
      }

      if (this.activePreset[0] < 0) {
         if (canCreatePreset) {
            ImGui.sameLine();
            if (ImGui.button("+")) {
               ImGui.openPopup("CreatePreset");
               this.createPresetName.set("", false);
            }

            ImGuiHelper.tooltip(AxiomI18n.get("axiom.preset.create_new_preset"));
         }
      } else if (this.activePreset[0] > 0) {
         String tooltip = AxiomI18n.get("axiom.preset.delete", this.presetNames[this.activePreset[0]]);
         ImGui.sameLine();
         if (ImGui.button("X")) {
            try {
               PresetWidget.NamedPreset preset = this.loadedPresets[this.activePreset[0] - 1];
               Files.delete(preset.path);
            } catch (Exception var8) {
               var8.printStackTrace();
            }

            this.loadPresetsFromDisk(null);
         }

         ImGuiHelper.tooltip(tooltip);
      }
   }

   private void loadIfNeeded() {
      if (!this.loaded) {
         this.loaded = true;

         try {
            Files.createDirectories(this.path);
         } catch (Exception var12) {
            var12.printStackTrace();
         }

         this.loadPresetsFromDisk(null);
         boolean reloadFromDisk = false;
         if (this.defaultPresets != null) {
            Set<String> alreadyLoaded = this.getLoadedDefaults();
            if (!alreadyLoaded.containsAll(this.defaultPresets.keySet())) {
               StringBuilder defaultFile = new StringBuilder();

               label63:
               for (Entry<String, CompoundTag> entry : this.defaultPresets.entrySet()) {
                  defaultFile.append(entry.getKey());
                  defaultFile.append("\n");
                  if (!alreadyLoaded.contains(entry.getKey())) {
                     for (PresetWidget.NamedPreset loadedPreset : this.loadedPresets) {
                        if (loadedPreset.name.trim().equalsIgnoreCase(entry.getKey().trim())) {
                           continue label63;
                        }
                     }

                     String name = entry.getKey();
                     CompoundTag settings = entry.getValue();

                     try {
                        String filename = FileUtil.findAvailableName(this.path, name.toLowerCase(Locale.ROOT).replace(" ", "_"), ".nbt");
                        CompoundTag preset = new CompoundTag();
                        preset.putString("Name", name);
                        preset.putString("Version", Authorization.getUserAgent());
                        preset.put("Settings", settings);
                        NbtIo.writeCompressed(preset, this.path.resolve(filename));
                        reloadFromDisk = true;
                     } catch (Exception var11) {
                        var11.printStackTrace();
                     }
                  }
               }

               try {
                  Files.writeString(this.path.resolve(".defaults"), defaultFile.toString());
               } catch (Exception var10) {
               }
            }
         }

         if (reloadFromDisk) {
            this.loadPresetsFromDisk(null);
         }

         this.activePreset[0] = 0;
      }
   }

   private record NamedPreset(String name, Path path, CompoundTag settings) {
   }
}
