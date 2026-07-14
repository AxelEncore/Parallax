package com.moulberry.axiom.editor.windows.save_world;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixUtils;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.BlockEntityMap;
import com.moulberry.axiom.capabilities.ReplaceMode;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.downgrade.DowngradeVersion;
import com.moulberry.axiom.downgrade.Downgrader;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.utils.Authorization;
import imgui.moulberry92.ImGui;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class SaveSchematicAction {
   private final Path path;
   private final ChunkedBlockRegion blockRegion;
   private final Long2ObjectMap<CompressedBlockEntity> blockEntities;
   private final int minX;
   private final int minY;
   private final int minZ;
   private final int maxX;
   private final int maxY;
   private final int maxZ;
   private final String serializedAir;
   private final String name;
   private final String author;
   @Nullable
   private final CompoundTag additionalSchematicData;
   private boolean finished = false;
   private int index;
   private final FriendlyByteBuf blockData;
   private final Object2IntMap<String> paletteMap;
   private final List<String> paletteList;
   private final byte[] legacyBlockIds;
   private final byte[] legacyBlockData;
   private final Map<BlockState, String> serializedMap = new HashMap<>();
   private final Downgrader downgrader;
   private final DowngradeVersion downgradeVersion;
   private final boolean isLegacy;
   private final Map<Block, ConflictResolution> conflictResolutionForBlock = new HashMap<>();
   private final Map<Block, CustomBlock> otherBlockForBlock = new HashMap<>();
   private ConflictResolution doesntExistResolution = null;
   private ConflictResolution substituteResolution = null;
   private final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   private boolean doThisForAllConflicts = false;
   private BlockState promptBlockState = null;
   private Set<BlockState> promptWriteSerialized = new HashSet<>();
   private String promptDowngrade = null;
   private int promptType = -1;
   private static final Map<String, String> REVERSE_BLOCK_ENTITY_ID_FIX_MAP = (Map<String, String>)DataFixUtils.make(Maps.<String, String>newHashMap(), hashMap -> {
      hashMap.put("minecraft:end_portal", "Airportal");
      hashMap.put("minecraft:banner", "Banner");
      hashMap.put("minecraft:beacon", "Beacon");
      hashMap.put("minecraft:brewing_stand", "Cauldron");
      hashMap.put("minecraft:chest", "Chest");
      hashMap.put("minecraft:comparator", "Comparator");
      hashMap.put("minecraft:command_block", "Control");
      hashMap.put("minecraft:daylight_detector", "DLDetector");
      hashMap.put("minecraft:dropper", "Dropper");
      hashMap.put("minecraft:enchanting_table", "EnchantTable");
      hashMap.put("minecraft:end_gateway", "EndGateway");
      hashMap.put("minecraft:ender_chest", "EnderChest");
      hashMap.put("minecraft:flower_pot", "FlowerPot");
      hashMap.put("minecraft:furnace", "Furnace");
      hashMap.put("minecraft:hopper", "Hopper");
      hashMap.put("minecraft:mob_spawner", "MobSpawner");
      hashMap.put("minecraft:noteblock", "Music");
      hashMap.put("minecraft:piston", "Piston");
      hashMap.put("minecraft:jukebox", "RecordPlayer");
      hashMap.put("minecraft:sign", "Sign");
      hashMap.put("minecraft:skull", "Skull");
      hashMap.put("minecraft:structure_block", "Structure");
      hashMap.put("minecraft:dispenser", "Trap");
   });

   public SaveSchematicAction(
      Path path,
      ChunkedBlockRegion blockRegion,
      Long2ObjectMap<CompressedBlockEntity> blockEntities,
      DowngradeVersion downgradeVersion,
      String name,
      String author,
      @Nullable CompoundTag additionalSchematicData
   ) {
      this.path = path;
      this.blockRegion = blockRegion;
      this.blockEntities = blockEntities;
      this.name = name;
      this.author = author;
      this.additionalSchematicData = additionalSchematicData;
      this.minX = this.blockRegion.min().getX();
      this.minY = this.blockRegion.min().getY();
      this.minZ = this.blockRegion.min().getZ();
      this.maxX = this.blockRegion.max().getX();
      this.maxY = this.blockRegion.max().getY();
      this.maxZ = this.blockRegion.max().getZ();
      int currentDataVersion = DFUHelper.DATA_VERSION;
      if (downgradeVersion != null && (downgradeVersion.getMinDataVersion() > currentDataVersion || currentDataVersion > downgradeVersion.getMaxDataVersion())) {
         this.downgrader = new Downgrader(downgradeVersion);
         this.downgradeVersion = downgradeVersion;
         String serializedAir = this.downgrader.downgrade(BlockStateParser.serialize(Blocks.AIR.defaultBlockState()));
         if (serializedAir != null && serializedAir.startsWith("?")) {
            serializedAir = serializedAir.substring(1);
         }

         if (serializedAir == null || serializedAir.isEmpty()) {
            if (this.downgradeVersion.getMaxDataVersion() >= 1631) {
               serializedAir = BlockStateParser.serialize(Blocks.AIR.defaultBlockState());
            } else {
               serializedAir = "0";
            }
         }

         this.serializedAir = serializedAir;
      } else {
         this.downgrader = null;
         this.downgradeVersion = null;
         this.serializedAir = BlockStateParser.serialize(Blocks.AIR.defaultBlockState());
      }

      if (this.downgradeVersion != null && this.downgradeVersion.getMaxDataVersion() < 1631) {
         this.isLegacy = true;
         this.blockData = null;
         this.paletteMap = null;
         this.paletteList = null;
         int sizeX = this.maxX - this.minX + 1;
         int sizeY = this.maxY - this.minY + 1;
         int sizeZ = this.maxZ - this.minZ + 1;
         int count = sizeX * sizeY * sizeZ;
         this.legacyBlockIds = new byte[count];
         this.legacyBlockData = new byte[count];
      } else {
         this.isLegacy = false;
         this.blockData = new FriendlyByteBuf(Unpooled.buffer());
         this.paletteMap = new Object2IntOpenHashMap();
         this.paletteList = new ArrayList<>();
         this.legacyBlockIds = null;
         this.legacyBlockData = null;
      }
   }

   public boolean render() {
      if (this.finished) {
         throw new FaultyImplementationError();
      } else {
         if (this.promptType >= 0) {
            if (!ImGui.isPopupOpen("###ExportPrompt")) {
               ImGui.openPopup("###ExportPrompt");
            }

            String text = this.promptType == 1
               ? "Don't know how to downgrade this block to " + this.downgradeVersion.getVersionString()
               : "Block doesn't exist in " + this.downgradeVersion.getVersionString();
            if (ImGuiHelper.beginPopupModal(text + "###ExportPrompt", 64)) {
               ConflictResolution conflictResolution = null;
               if (this.promptType == 0 || this.promptType == 2) {
                  ImGuiHelper.blockStateButton((CustomBlockState)this.promptBlockState, 0, 64);
                  if (this.promptType == 2) {
                     ImGuiHelper.separatorWithText("Substitute");
                     ImGui.text(this.promptDowngrade);
                  }

                  ImGuiHelper.separatorWithText("Actions");
                  if (this.promptType == 2 && ImGui.button(AxiomI18n.get("axiom.hardcoded.replace_with_substitute"))) {
                     conflictResolution = ConflictResolution.REPLACE_WITH_SUBSTITUTE;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.replace_with_air"))) {
                     conflictResolution = ConflictResolution.REPLACE_WITH_AIR;
                  }

                  if (!this.isLegacy && ImGui.button(AxiomI18n.get("axiom.hardcoded.force_write_anyways"))) {
                     conflictResolution = ConflictResolution.FORCE_WRITE_ANYWAYS;
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.replace_with_other_block"))) {
                     this.selectBlockWidget.open();
                  }

                  this.selectBlockWidget.render(AxiomI18n.get("axiom.widget.select_block"), EditorUI.getBlockList());
                  CustomBlockState blockState = this.selectBlockWidget.getResultState();
                  if (blockState != null) {
                     if (!this.promptWriteSerialized.contains(this.promptBlockState)) {
                        throw new FaultyImplementationError();
                     }

                     for (BlockState write : this.promptWriteSerialized) {
                        String fromKey = write.getBlock().builtInRegistryHolder().key().location().toString();
                        String toValue = blockState.getVanillaState().getBlock().builtInRegistryHolder().key().location().toString();
                        Axiom.configuration.internal.customDowngradeSuggestions.put(fromKey, toValue);
                        this.conflictResolutionForBlock.put(write.getBlock(), ConflictResolution.REPLACE_WITH_OTHER_BLOCK);
                        this.otherBlockForBlock.put(write.getBlock(), blockState.getCustomBlock());
                        BlockState newState = blockState.getVanillaState();

                        for (Property<?> property : write.getProperties()) {
                           if (newState.hasProperty(property)) {
                              newState = ReplaceMode.copyProperty((CustomBlockState)write, newState, property);
                           }
                        }

                        if (this.serializedMap.containsKey(newState)) {
                           String serialized = this.serializedMap.get(newState);
                           if (newState != write) {
                              this.serializedMap.put(write, serialized);
                           }
                        } else {
                           String serialized = this.tryDowngrade(newState);
                           if (serialized == null) {
                              ImGui.endPopup();
                              return false;
                           }

                           this.serializedMap.put(newState, serialized);
                           if (newState != write) {
                              this.serializedMap.put(write, serialized);
                           }
                        }
                     }

                     this.promptType = -1;
                     this.promptWriteSerialized.clear();
                     this.promptBlockState = null;
                     this.promptDowngrade = null;
                     ImGui.endPopup();
                     return this.run();
                  }

                  if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.do_for_all_conflicts"), this.doThisForAllConflicts)) {
                     this.doThisForAllConflicts = !this.doThisForAllConflicts;
                  }

                  ImGui.sameLine();
               } else if (this.promptType == 1) {
                  String namespace = this.promptBlockState.getBlock().builtInRegistryHolder().key().location().getNamespace();
                  if (!namespace.equals("minecraft")) {
                     ImGui.text(AxiomI18n.get("axiom.hardcoded.block_seems_modded") + namespace + ").");
                  } else {
                     ImGui.text(AxiomI18n.get("axiom.hardcoded.likely_axiom_issue"));
                  }

                  ImGuiHelper.blockStateButton((CustomBlockState)this.promptBlockState, 0, 64);
                  ImGuiHelper.separatorWithText("Actions");
                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.replace_with_air"))) {
                     conflictResolution = ConflictResolution.REPLACE_WITH_AIR;
                  }

                  if (!this.isLegacy && ImGui.button(AxiomI18n.get("axiom.hardcoded.force_write_anyways"))) {
                     conflictResolution = ConflictResolution.FORCE_WRITE_ANYWAYS;
                  }

                  if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.do_for_all_unknown_blocks"), this.doThisForAllConflicts)) {
                     this.doThisForAllConflicts = !this.doThisForAllConflicts;
                  }

                  ImGui.sameLine();
               }

               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel_export"))) {
                  ImGui.endPopup();
                  return true;
               }

               ImGui.endPopup();
               if (conflictResolution == null) {
                  return false;
               }
               String resolved = switch (conflictResolution) {
                  case REPLACE_WITH_SUBSTITUTE -> this.promptDowngrade;
                  case REPLACE_WITH_AIR -> this.serializedAir;
                  case FORCE_WRITE_ANYWAYS -> BlockStateParser.serialize(this.promptBlockState);
                  case REPLACE_WITH_OTHER_BLOCK -> throw new FaultyImplementationError();
               };
               if (!this.promptWriteSerialized.contains(this.promptBlockState)) {
                  throw new FaultyImplementationError();
               }

               for (BlockState write : this.promptWriteSerialized) {
                  this.serializedMap.put(write, resolved);
                  this.conflictResolutionForBlock.put(write.getBlock(), conflictResolution);
               }

               if (this.doThisForAllConflicts) {
                  this.doThisForAllConflicts = false;
                  if (this.promptType != 0 && this.promptType != 1) {
                     this.substituteResolution = conflictResolution;
                  } else {
                     this.doesntExistResolution = conflictResolution;
                  }
               }
            }
         } else {
            if (!ImGui.isPopupOpen("###ExportPrompt")) {
               ImGui.openPopup("###ExportPrompt");
            }

            if (ImGuiHelper.beginPopupModal("Exporting...###ExportPrompt", 64)) {
               ImGui.text(AxiomI18n.get("axiom.hardcoded.exporting"));
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
                  return true;
               }

               ImGui.endPopup();
            }
         }

         this.promptType = -1;
         this.promptWriteSerialized.clear();
         this.promptBlockState = null;
         this.promptDowngrade = null;
         return this.run();
      }
   }

   private String tryDowngrade(BlockState blockState) {
      String originalSerialized = BlockStateParser.serialize(blockState);
      if (this.downgrader == null) {
         return originalSerialized;
      } else {
         String downgraded = this.downgrader.downgrade(originalSerialized);
         if (downgraded == null || downgraded.isEmpty() || downgraded.startsWith("?")) {
            String fromKey = blockState.getBlock().builtInRegistryHolder().key().location().toString();
            String customSuggestion = Axiom.configuration.internal.customDowngradeSuggestions.get(fromKey);
            if (customSuggestion != null) {
               try {
                  ResourceLocation resourceLocation = ResourceLocation.parse(customSuggestion);
                  Block block = (Block)BuiltInRegistries.BLOCK.getOptional(resourceLocation).orElse(null);
                  if (block != null) {
                     BlockState newState = block.defaultBlockState();

                     for (Property<?> property : blockState.getProperties()) {
                        if (newState.hasProperty(property)) {
                           newState = ReplaceMode.copyProperty((CustomBlockState)blockState, newState, property);
                        }
                     }

                     String newDowngraded = this.downgrader.downgrade(BlockStateParser.serialize(newState));
                     if (newDowngraded != null && !newDowngraded.isEmpty()) {
                        if (!newDowngraded.startsWith("?")) {
                           downgraded = "?" + newDowngraded;
                        } else {
                           downgraded = newDowngraded;
                        }
                     }
                  }
               } catch (Exception var11) {
               }
            }
         }

         if (downgraded == null || downgraded.isEmpty()) {
            ConflictResolution conflictResolution = this.conflictResolutionForBlock.get(blockState.getBlock());
            if (conflictResolution == null) {
               conflictResolution = this.doesntExistResolution;
            }

            if (conflictResolution == ConflictResolution.REPLACE_WITH_OTHER_BLOCK && this.otherBlockForBlock.containsKey(blockState.getBlock())) {
               CustomBlock otherBlock = this.otherBlockForBlock.get(blockState.getBlock());
               BlockState newState = otherBlock.axiom$defaultCustomState().getVanillaState();

               for (Property<?> propertyx : blockState.getProperties()) {
                  if (newState.hasProperty(propertyx)) {
                     newState = ReplaceMode.copyProperty((CustomBlockState)blockState, newState, propertyx);
                  }
               }

               if (this.serializedMap.containsKey(newState)) {
                  String newDowngraded = this.serializedMap.get(newState);
                  if (newState != blockState) {
                     this.serializedMap.put(blockState, newDowngraded);
                  }

                  return newDowngraded;
               } else {
                  String newDowngraded = this.tryDowngrade(newState);
                  if (newDowngraded == null) {
                     return null;
                  } else {
                     this.serializedMap.put(newState, newDowngraded);
                     if (newState != blockState) {
                        this.serializedMap.put(blockState, newDowngraded);
                     }

                     return newDowngraded;
                  }
               }
            } else if (conflictResolution == ConflictResolution.REPLACE_WITH_AIR) {
               this.serializedMap.put(blockState, this.serializedAir);
               return this.serializedAir;
            } else if (conflictResolution == ConflictResolution.FORCE_WRITE_ANYWAYS) {
               this.serializedMap.put(blockState, originalSerialized);
               return originalSerialized;
            } else {
               if (downgraded == null) {
                  this.promptType = 0;
               } else {
                  this.promptType = 1;
               }

               this.promptBlockState = blockState;
               this.promptWriteSerialized.add(blockState);
               this.promptDowngrade = null;
               return null;
            }
         } else if (downgraded.startsWith("?")) {
            String substitute = downgraded.substring(1);
            ConflictResolution conflictResolutionx = this.conflictResolutionForBlock.get(blockState.getBlock());
            if (conflictResolutionx == null) {
               conflictResolutionx = this.substituteResolution;
            }

            if (conflictResolutionx == ConflictResolution.REPLACE_WITH_OTHER_BLOCK && this.otherBlockForBlock.containsKey(blockState.getBlock())) {
               CustomBlock otherBlock = this.otherBlockForBlock.get(blockState.getBlock());
               BlockState newState = otherBlock.axiom$defaultCustomState().getVanillaState();

               for (Property<?> propertyxx : blockState.getProperties()) {
                  if (newState.hasProperty(propertyxx)) {
                     newState = ReplaceMode.copyProperty((CustomBlockState)blockState, newState, propertyxx);
                  }
               }

               if (this.serializedMap.containsKey(newState)) {
                  String newDowngraded = this.serializedMap.get(newState);
                  if (newState != blockState) {
                     this.serializedMap.put(blockState, newDowngraded);
                  }

                  return newDowngraded;
               } else {
                  String newDowngraded = this.tryDowngrade(newState);
                  if (newDowngraded == null) {
                     return null;
                  } else {
                     this.serializedMap.put(newState, newDowngraded);
                     if (newState != blockState) {
                        this.serializedMap.put(blockState, newDowngraded);
                     }

                     return newDowngraded;
                  }
               }
            } else if (conflictResolutionx == ConflictResolution.REPLACE_WITH_SUBSTITUTE) {
               this.serializedMap.put(blockState, substitute);
               return substitute;
            } else if (conflictResolutionx == ConflictResolution.REPLACE_WITH_AIR) {
               this.serializedMap.put(blockState, this.serializedAir);
               return this.serializedAir;
            } else if (conflictResolutionx == ConflictResolution.FORCE_WRITE_ANYWAYS) {
               this.serializedMap.put(blockState, originalSerialized);
               return originalSerialized;
            } else {
               this.promptType = 2;
               this.promptBlockState = blockState;
               this.promptWriteSerialized.add(blockState);
               this.promptDowngrade = substitute;
               return null;
            }
         } else {
            this.serializedMap.put(blockState, downgraded);
            return downgraded;
         }
      }
   }

   public boolean run() {
      return !this.isLegacy ? this.runSpongeV2() : this.runLegacy();
   }

   private boolean runSpongeV2() {
      if (this.finished) {
         throw new FaultyImplementationError();
      } else {
         int width = this.maxX - this.minX + 1;
         int height = this.maxY - this.minY + 1;
         int length = this.maxZ - this.minZ + 1;

         for (int maxIndex = width * height * length; this.index < maxIndex; this.index++) {
            int x = this.index % width + this.minX;
            int z = this.index % (width * length) / width + this.minZ;
            int y = this.index / (width * length) + this.minY;
            BlockState blockState = this.blockRegion.getBlockStateOrAir(x, y, z);
            String serialized;
            if (this.serializedMap.containsKey(blockState)) {
               serialized = this.serializedMap.get(blockState);
            } else {
               serialized = this.tryDowngrade(blockState);
               if (serialized == null) {
                  return false;
               }
            }

            int entry;
            if (this.paletteMap.containsKey(serialized)) {
               entry = this.paletteMap.getInt(serialized);
            } else {
               entry = this.paletteMap.size();
               this.paletteMap.put(serialized, entry);
               this.paletteList.add(serialized);
            }

            this.blockData.writeVarInt(entry);
         }

         CompoundTag schematic = new CompoundTag();
         schematic.putInt("Version", 2);
         if (this.downgradeVersion == null) {
            schematic.putInt("DataVersion", DFUHelper.DATA_VERSION);
         } else {
            schematic.putInt("DataVersion", this.downgradeVersion.getMinDataVersion());
         }

         schematic.putShort("Width", (short)(width & 65535));
         schematic.putShort("Height", (short)(height & 65535));
         schematic.putShort("Length", (short)(length & 65535));
         CompoundTag metadata = new CompoundTag();
         metadata.putString("Name", this.name);
         metadata.putString("Author", this.author);
         metadata.putString("CreatedBy", Authorization.getUserAgent());
         metadata.putLong("Date", System.currentTimeMillis());
         schematic.put("Metadata", metadata);
         schematic.putIntArray("Offset", new int[]{0, 0, 0});
         byte[] bytes = new byte[this.blockData.writerIndex()];
         this.blockData.getBytes(0, bytes);
         schematic.put("BlockData", new ByteArrayTag(bytes));
         ListTag blockEntities = new ListTag();
         this.blockEntities.forEach((pos, compressedBlockEntity) -> {
            int xx = BlockPos.getX(pos);
            int yx = BlockPos.getY(pos);
            int zx = BlockPos.getZ(pos);
            BlockState blockStatex = this.blockRegion.getBlockStateOrAir(xx, yx, zx);
            BlockEntityType<?> type = BlockEntityMap.get(blockStatex);
            ResourceLocation resourceLocation = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            if (resourceLocation != null) {
               CompoundTag tag = compressedBlockEntity.decompress();
               tag.putIntArray("Pos", new int[]{xx - this.minX, yx - this.minY, zx - this.minZ});
               tag.putString("Id", resourceLocation.toString());
               blockEntities.add(tag);
            }
         });
         schematic.put("BlockEntities", blockEntities);
         CompoundTag palette = new CompoundTag();

         for (int i = 0; i < this.paletteList.size(); i++) {
            palette.putInt(this.paletteList.get(i), i);
         }

         schematic.put("Palette", palette);
         schematic.putInt("PaletteMax", this.paletteList.size());
         this.putAdditional(schematic, false);

         try (
            OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(this.path));
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(outputStream)));
         ) {
            dataOutputStream.writeByte(schematic.getId());
            dataOutputStream.writeUTF("Schematic");
            schematic.write(dataOutputStream);
         } catch (IOException var18) {
            var18.printStackTrace();
         }

         this.finished = true;
         return true;
      }
   }

   private void putAdditional(CompoundTag schematic, boolean legacy) {
      if (this.additionalSchematicData != null) {
         for (String key : this.additionalSchematicData.getAllKeys()) {
            Tag tag = this.additionalSchematicData.get(key);
            if (tag != null) {
               if (legacy) {
                  if (key.equals("Metadata") && tag instanceof CompoundTag compound) {
                     Tag worldEditOffsetX = compound.get("WEOffsetX");
                     if (worldEditOffsetX != null) {
                        schematic.put("WEOffsetX", worldEditOffsetX);
                        compound.remove("WEOffsetX");
                     }

                     Tag worldEditOffsetY = compound.get("WEOffsetY");
                     if (worldEditOffsetY != null) {
                        schematic.put("WEOffsetY", worldEditOffsetY);
                        compound.remove("WEOffsetY");
                     }

                     Tag worldEditOffsetZ = compound.get("WEOffsetZ");
                     if (worldEditOffsetZ != null) {
                        schematic.put("WEOffsetZ", worldEditOffsetZ);
                        compound.remove("WEOffsetZ");
                     }
                  } else {
                     if (key.equals("Offset") && tag instanceof IntArrayTag intArrayTag && intArrayTag.size() == 3) {
                        schematic.put("WEOriginX", intArrayTag.get(0));
                        schematic.put("WEOriginY", intArrayTag.get(1));
                        schematic.put("WEOriginZ", intArrayTag.get(2));
                        continue;
                     }

                     if (key.equals("Version")) {
                        continue;
                     }
                  }
               }

               if (!(tag instanceof CompoundTag compoundTag && compoundTag.isEmpty()) && !(tag instanceof ListTag listTag && listTag.isEmpty())) {
                  this.merge(schematic, key, tag);
               }
            }
         }
      }
   }

   private void merge(CompoundTag into, String key, Tag tag) {
      if (!into.contains(key)) {
         into.put(key, tag);
      } else {
         Optional<CompoundTag> existingOptional = VersionUtilsNbt.helperCompoundTagGetCompound(into, key);
         if (existingOptional.isPresent() && tag instanceof CompoundTag compoundTag) {
            CompoundTag existing = existingOptional.get();

            for (String subkey : compoundTag.getAllKeys()) {
               Tag subtag = compoundTag.get(subkey);
               if (subtag != null) {
                  this.merge(existing, subkey, subtag);
               }
            }
         }
      }
   }

   private boolean runLegacy() {
      if (this.finished) {
         throw new FaultyImplementationError();
      } else {
         int width = this.maxX - this.minX + 1;
         int height = this.maxY - this.minY + 1;
         int length = this.maxZ - this.minZ + 1;

         for (int maxIndex = width * height * length; this.index < maxIndex; this.index++) {
            int x = this.index % width + this.minX;
            int z = this.index % (width * length) / width + this.minZ;
            int y = this.index / (width * length) + this.minY;
            BlockState blockState = this.blockRegion.getBlockStateOrAir(x, y, z);
            String serialized;
            if (this.serializedMap.containsKey(blockState)) {
               serialized = this.serializedMap.get(blockState);
            } else {
               serialized = this.tryDowngrade(blockState);
               if (serialized == null) {
                  return false;
               }
            }

            byte blockId;
            byte blockData;
            if (serialized.contains(":")) {
               String[] split = serialized.split(":");
               blockId = (byte)Integer.parseInt(split[0]);
               blockData = Byte.parseByte(split[1]);
            } else {
               blockId = (byte)Integer.parseInt(serialized);
               blockData = 0;
            }

            this.legacyBlockIds[this.index] = blockId;
            this.legacyBlockData[this.index] = blockData;
         }

         CompoundTag schematic = new CompoundTag();
         schematic.putString("Materials", "Alpha");
         schematic.putShort("Width", (short)(width & 65535));
         schematic.putShort("Height", (short)(height & 65535));
         schematic.putShort("Length", (short)(length & 65535));
         schematic.putByteArray("Blocks", this.legacyBlockIds);
         schematic.putByteArray("Data", this.legacyBlockData);
         ListTag blockEntities = new ListTag();
         this.blockEntities.forEach((pos, compressedBlockEntity) -> {
            int xx = BlockPos.getX(pos);
            int yx = BlockPos.getY(pos);
            int zx = BlockPos.getZ(pos);
            BlockState blockStatex = this.blockRegion.getBlockStateOrAir(xx, yx, zx);
            BlockEntityType<?> type = BlockEntityMap.get(blockStatex);
            ResourceLocation resourceLocation = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            if (resourceLocation != null) {
               CompoundTag tag = compressedBlockEntity.decompress();
               String id = resourceLocation.toString();
               if (this.downgradeVersion.getMinDataVersion() <= 704) {
                  id = REVERSE_BLOCK_ENTITY_ID_FIX_MAP.get(id);
                  if (id == null) {
                     return;
                  }
               }

               tag.putString("id", id);
               tag.putInt("x", xx - this.minX);
               tag.putInt("y", yx - this.minY);
               tag.putInt("z", zx - this.minZ);
               blockEntities.add(tag);
            }
         });
         schematic.put("TileEntities", blockEntities);
         schematic.put("Entities", new ListTag());
         this.putAdditional(schematic, true);

         try (
            OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(this.path));
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(outputStream)));
         ) {
            dataOutputStream.writeByte(schematic.getId());
            dataOutputStream.writeUTF("Schematic");
            schematic.write(dataOutputStream);
         } catch (IOException var17) {
            var17.printStackTrace();
         }

         this.finished = true;
         return true;
      }
   }
}
