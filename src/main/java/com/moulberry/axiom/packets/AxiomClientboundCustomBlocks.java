package com.moulberry.axiom.packets;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.custom_blocks.StringProperty;
import com.moulberry.axiom.utils.NetworkHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class AxiomClientboundCustomBlocks implements AxiomClientboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:custom_blocks");
   private final List<AxiomClientboundCustomBlocks.CustomBlockEntry> customBlockEntries;
   private static Map<String, Map<List<String>, EnumProperty<?>>> allEnumProperties = null;

   public AxiomClientboundCustomBlocks(List<AxiomClientboundCustomBlocks.CustomBlockEntry> customBlockEntries) {
      this.customBlockEntries = List.copyOf(customBlockEntries);
   }

   public AxiomClientboundCustomBlocks(FriendlyByteBuf friendlyByteBuf) {
      this.customBlockEntries = friendlyByteBuf.readList(AxiomClientboundCustomBlocks.CustomBlockEntry::read);
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeCollection(this.customBlockEntries, AxiomClientboundCustomBlocks.CustomBlockEntry::write);
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      ServerCustomBlocks.clearRegisteredCustomBlocks();

      for (AxiomClientboundCustomBlocks.CustomBlockEntry customBlockEntry : this.customBlockEntries) {
         try {
            ServerCustomBlocks.registerCustomBlock(
               customBlockEntry.id,
               customBlockEntry.translationKey,
               customBlockEntry.properties,
               customBlockEntry.blockStates,
               customBlockEntry.pickBlockStack,
               false,
               !customBlockEntry.doNormalInteractions,
               true,
               customBlockEntry.updaters,
               true,
               Map.of(),
               Map.of(),
               Map.of(),
               Map.of()
            );
         } catch (Exception var6) {
            if (AxiomPlatform.isDevelopment()) {
               throw var6;
            }

            Axiom.LOGGER.error("Failed to register custom block", var6);
         }
      }

      ServerCustomBlocks.update();
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundCustomBlocks::new);
   }

   private static EnumProperty<?> findEnumProperty(String name, List<String> values) {
      if (allEnumProperties == null) {
         allEnumProperties = new LinkedHashMap<>();

         for (Block block : BuiltInRegistries.BLOCK) {
            label48:
            for (Property<?> property : block.getStateDefinition().getProperties()) {
               try {
                  if (property instanceof EnumProperty<?> enumProperty) {
                     Map<List<String>, EnumProperty<?>> map = allEnumProperties.computeIfAbsent(enumProperty.getName(), k -> new HashMap<>());
                     Collection<Enum<?>> possibleValues = (Collection<Enum<?>>)(Collection)enumProperty.getPossibleValues();
                     List<String> allKeys = new ArrayList<>(possibleValues.size());

                     for (Enum<?> possibleValue : possibleValues) {
                        if (!(possibleValue instanceof StringRepresentable stringRepresentable)) {
                           continue label48;
                        }

                        allKeys.add(stringRepresentable.getSerializedName());
                     }

                     map.put(allKeys, enumProperty);
                  }
               } catch (Exception var13) {
               }
            }
         }
      }

      Map<List<String>, EnumProperty<?>> properties = allEnumProperties.get(name);
      return properties == null ? null : properties.get(values);
   }

   public static Property<?> readProperty(FriendlyByteBuf friendlyByteBuf) {
      byte type = friendlyByteBuf.readByte();

      return (Property<?>)(switch (type) {
         case 0 -> {
            String name = friendlyByteBuf.readUtf();
            yield BooleanProperty.create(name);
         }
         case 1 -> {
            String name = friendlyByteBuf.readUtf();
            int min = friendlyByteBuf.readInt();
            int max = friendlyByteBuf.readInt();
            yield IntegerProperty.create(name, min, max);
         }
         case 2 -> {
            String name = friendlyByteBuf.readUtf();
            List<String> values = friendlyByteBuf.readList(FriendlyByteBuf::readUtf);
            if (values.size() < 2) {
               throw new IllegalArgumentException("Size of list must be greater than 1");
            }

            Set<String> seenValues = new HashSet<>();

            for (int i = 0; i < values.size(); i++) {
               String value = values.get(i).toLowerCase(Locale.ROOT);
               values.set(i, value);
               if (seenValues.contains(value)) {
                  throw new IllegalArgumentException("Duplicate property name: " + value);
               }

               seenValues.add(value);
            }

            EnumProperty<?> namedProperty = findEnumProperty(name, values);
            yield namedProperty != null ? namedProperty : new StringProperty(name, values);
         }
         case 3 -> BlockStateProperties.AXIS;
         case 4 -> BlockStateProperties.HORIZONTAL_AXIS;
         case 5 -> BlockStateProperties.FACING;
         case 6 -> BlockStateProperties.HORIZONTAL_FACING;
         case 7 -> BlockStateProperties.UP;
         case 8 -> BlockStateProperties.DOWN;
         case 9 -> BlockStateProperties.NORTH;
         case 10 -> BlockStateProperties.EAST;
         case 11 -> BlockStateProperties.SOUTH;
         case 12 -> BlockStateProperties.WEST;
         case 13 -> BlockStateProperties.WATERLOGGED;
         case 14 -> BlockStateProperties.HALF;
         case 15 -> BlockStateProperties.VERTICAL_DIRECTION;
         default -> throw new RuntimeException("Unknown property type: " + type);
      });
   }

   public static void writeProperty(FriendlyByteBuf friendlyByteBuf, Property<?> property) {
      if (property == BlockStateProperties.AXIS) {
         friendlyByteBuf.writeByte(3);
      } else if (property == BlockStateProperties.HORIZONTAL_AXIS) {
         friendlyByteBuf.writeByte(4);
      } else if (property == BlockStateProperties.FACING) {
         friendlyByteBuf.writeByte(5);
      } else if (property == BlockStateProperties.HORIZONTAL_FACING) {
         friendlyByteBuf.writeByte(6);
      } else if (property == BlockStateProperties.UP) {
         friendlyByteBuf.writeByte(7);
      } else if (property == BlockStateProperties.DOWN) {
         friendlyByteBuf.writeByte(8);
      } else if (property == BlockStateProperties.NORTH) {
         friendlyByteBuf.writeByte(9);
      } else if (property == BlockStateProperties.EAST) {
         friendlyByteBuf.writeByte(10);
      } else if (property == BlockStateProperties.SOUTH) {
         friendlyByteBuf.writeByte(11);
      } else if (property == BlockStateProperties.WEST) {
         friendlyByteBuf.writeByte(12);
      } else if (property == BlockStateProperties.WATERLOGGED) {
         friendlyByteBuf.writeByte(13);
      } else if (property == BlockStateProperties.HALF) {
         friendlyByteBuf.writeByte(14);
      } else if (property == BlockStateProperties.VERTICAL_DIRECTION) {
         friendlyByteBuf.writeByte(15);
      } else if (property instanceof BooleanProperty) {
         friendlyByteBuf.writeByte(0);
         friendlyByteBuf.writeUtf(property.getName());
      } else if (property instanceof IntegerProperty integerProperty) {
         friendlyByteBuf.writeByte(1);
         friendlyByteBuf.writeUtf(property.getName());
         friendlyByteBuf.writeInt(integerProperty.min);
         friendlyByteBuf.writeInt(integerProperty.max);
      } else if (property instanceof StringProperty stringProperty) {
         friendlyByteBuf.writeByte(2);
         friendlyByteBuf.writeUtf(property.getName());
         friendlyByteBuf.writeCollection(stringProperty.getPossibleValues(), FriendlyByteBuf::writeUtf);
      } else {
         if (!(property instanceof EnumProperty<?> enumProperty)) {
            throw new UnsupportedOperationException("Unknown property type: " + property.getClass());
         }

         friendlyByteBuf.writeByte(2);
         friendlyByteBuf.writeUtf(property.getName());
         friendlyByteBuf.writeCollection(enumProperty.getPossibleValues(), (buf, e) -> buf.writeUtf(((StringRepresentable)e).getSerializedName()));
      }
   }

   public record CustomBlockEntry(
      ResourceLocation id,
      String translationKey,
      ItemStack pickBlockStack,
      boolean doNormalInteractions,
      List<Property<?>> properties,
      List<ResourceLocation> updaters,
      List<BlockState> blockStates
   ) {
      public static AxiomClientboundCustomBlocks.CustomBlockEntry read(FriendlyByteBuf friendlyByteBuf) {
         ResourceLocation id = friendlyByteBuf.readResourceLocation();
         String translationKey = friendlyByteBuf.readUtf();
         ItemStack pickBlockStack = NetworkHelper.readItemStack(friendlyByteBuf);
         boolean doNormalInteractions = friendlyByteBuf.readBoolean();
         List<Property<?>> properties = friendlyByteBuf.readList(AxiomClientboundCustomBlocks::readProperty);
         List<ResourceLocation> updaters = friendlyByteBuf.readList(FriendlyByteBuf::readResourceLocation);
         List<BlockState> blockStates = friendlyByteBuf.readList(NetworkHelper::readBlockState);
         return new AxiomClientboundCustomBlocks.CustomBlockEntry(id, translationKey, pickBlockStack, doNormalInteractions, properties, updaters, blockStates);
      }

      public static void write(FriendlyByteBuf friendlyByteBuf, AxiomClientboundCustomBlocks.CustomBlockEntry entry) {
         friendlyByteBuf.writeResourceLocation(entry.id);
         friendlyByteBuf.writeUtf(entry.translationKey);
         NetworkHelper.writeItemStack(friendlyByteBuf, entry.pickBlockStack);
         friendlyByteBuf.writeBoolean(entry.doNormalInteractions);
         friendlyByteBuf.writeCollection(entry.properties, AxiomClientboundCustomBlocks::writeProperty);
         friendlyByteBuf.writeCollection(entry.updaters, FriendlyByteBuf::writeResourceLocation);
         friendlyByteBuf.writeCollection(entry.blockStates, NetworkHelper::writeBlockState);
      }
   }
}
