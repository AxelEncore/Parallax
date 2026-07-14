package com.moulberry.axiom.custom_blocks;

import com.mojang.brigadier.StringReader;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.custom_blocks.update.AxisCustomBlockPlacementLogic;
import com.moulberry.axiom.custom_blocks.update.CustomBlockPlacementLogic;
import com.moulberry.axiom.custom_blocks.update.FacingClickedCustomBlockPlacementLogic;
import com.moulberry.axiom.custom_blocks.update.FacingClickedOppositeCustomBlockPlacementLogic;
import com.moulberry.axiom.custom_blocks.update.FacingCustomBlockPlacementLogic;
import com.moulberry.axiom.custom_blocks.update.FacingOppositeCustomBlockPlacementLogic;
import com.moulberry.axiom.custom_blocks.update.HalfCustomBlockPlacementLogic;
import com.moulberry.axiom.custom_blocks.update.WaterloggedCustomBlockPlacementLogic;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.utils.BlockHelper;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class ServerCustomBlocks {
   public static final Map<ResourceLocation, CustomBlock> customBlockMap = new HashMap<>();
   private static final Map<BlockState, CustomBlockState> vanillaStateToCustomBlockState = new HashMap<>();
   private static final Map<ResourceLocation, CustomBlockPlacementLogic> placementLogicMap = new HashMap<>();
   private static final List<Direction> HORIZONTAL_DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

   public static void clearRegisteredCustomBlocks() {
      if (!customBlockMap.isEmpty() || !vanillaStateToCustomBlockState.isEmpty()) {
         customBlockMap.clear();
         vanillaStateToCustomBlockState.clear();
         BlockHelper.customRotateY.clear();
      }
   }

   public static void registerCustomBlock(
      ResourceLocation resourceLocation,
      String translationKey,
      List<Property<?>> properties,
      List<BlockState> vanillaStates,
      @Nullable ItemStack pickBlockItemStack,
      boolean sendServerPickBlockIfPossible,
      boolean preventRightClickInteraction,
      boolean preventShapeUpdates,
      List<ResourceLocation> placementLogicNames,
      boolean automaticRotationAndMirroring,
      Map<BlockState, BlockState> rotateYMappings,
      Map<BlockState, BlockState> flipXMappings,
      Map<BlockState, BlockState> flipYMappings,
      Map<BlockState, BlockState> flipZMappings
   ) {
      List<CustomBlockPlacementLogic> placementLogics = new ArrayList<>(placementLogicNames.size());

      for (ResourceLocation updater : placementLogicNames) {
         CustomBlockPlacementLogic placementLogic = placementLogicMap.get(updater);
         if (placementLogic == null) {
            throw new RuntimeException(resourceLocation + " - unknown custom block updater: " + updater);
         }

         placementLogics.add(placementLogic);
      }

      if (customBlockMap.containsKey(resourceLocation)) {
         throw new RuntimeException(resourceLocation + " - duplicate custom block registered: " + resourceLocation);
      } else {
         if (pickBlockItemStack != null && !pickBlockItemStack.isEmpty()) {
            ItemStackDataHelper.setCustomBlockPlacer(pickBlockItemStack, resourceLocation.toString());
         }

         CustomBlock customBlock = new CustomBlockImplementation(
            resourceLocation,
            translationKey,
            properties,
            vanillaStates,
            pickBlockItemStack,
            sendServerPickBlockIfPossible,
            preventRightClickInteraction,
            preventShapeUpdates,
            placementLogics
         );
         Set<BlockState> vanillaStatesSet = new HashSet<>(vanillaStates);
         checkMappingContainedInSet(rotateYMappings, vanillaStatesSet, "customRotateY");
         checkMappingContainedInSet(flipXMappings, vanillaStatesSet, "customFlipX");
         checkMappingContainedInSet(flipYMappings, vanillaStatesSet, "customFlipY");
         checkMappingContainedInSet(flipZMappings, vanillaStatesSet, "customFlipZ");

         for (CustomBlockState possibleCustomState : customBlock.axiom$getPossibleCustomStates()) {
            BlockState vanillaState = possibleCustomState.getVanillaState();
            if (vanillaStateToCustomBlockState.containsKey(vanillaState)) {
               throw new RuntimeException(resourceLocation + " - duplicate vanilla state registered: " + vanillaState.toString());
            }
         }

         for (CustomBlockState possibleCustomStatex : customBlock.axiom$getPossibleCustomStates()) {
            BlockState vanillaState = possibleCustomStatex.getVanillaState();
            vanillaStateToCustomBlockState.put(vanillaState, possibleCustomStatex);
            BlockHelper.customRotateY.put(vanillaState, vanillaState);
            BlockHelper.customFlipX.put(vanillaState, vanillaState);
            BlockHelper.customFlipY.put(vanillaState, vanillaState);
            BlockHelper.customFlipZ.put(vanillaState, vanillaState);
         }

         if (automaticRotationAndMirroring) {
            applyAutomaticRotationAndMirroring(properties, customBlock);
         }

         BlockHelper.customRotateY.putAll(rotateYMappings);
         BlockHelper.customFlipX.putAll(flipXMappings);
         BlockHelper.customFlipY.putAll(flipYMappings);
         BlockHelper.customFlipZ.putAll(flipZMappings);
         customBlockMap.put(resourceLocation, customBlock);
         update();
      }
   }

   private static void checkMappingContainedInSet(Map<BlockState, BlockState> mappings, Set<BlockState> vanillaStatesSet, String name) {
      for (Entry<BlockState, BlockState> entry : mappings.entrySet()) {
         if (!vanillaStatesSet.contains(entry.getKey()) && !vanillaStatesSet.contains(entry.getValue())) {
            throw new RuntimeException(name + " mapping either key or value must be part of custom block");
         }
      }
   }

   private static void applyAutomaticRotationAndMirroring(List<Property<?>> properties, CustomBlock customBlock) {
      Direction[] directions = Direction.values();
      EnumProperty<Direction> facingProperty = CustomBlockPlacementLogic.findFacingProperty(properties);
      EnumProperty<Axis> axisProperty = CustomBlockPlacementLogic.findAxisProperty(properties);
      EnumMap<Direction, Property<?>> directionalProperties = findSharedDirectionalProperties(properties, directions);
      Set<Direction> facingValues = facingProperty == null ? Set.of() : Set.copyOf(facingProperty.getPossibleValues());
      Set<Axis> axisValues = axisProperty == null ? Set.of() : Set.copyOf(axisProperty.getPossibleValues());
      boolean doClockwiseFacingRotate = facingProperty != null && facingValues.containsAll(HORIZONTAL_DIRECTIONS);
      boolean doClockwiseDirectionalRotate = directionalProperties != null && directionalProperties.keySet().containsAll(HORIZONTAL_DIRECTIONS);
      boolean doDirectionalFlipX = directionalProperties != null
         && directionalProperties.containsKey(Direction.EAST)
         && directionalProperties.containsKey(Direction.WEST);
      boolean doDirectionalFlipY = directionalProperties != null
         && directionalProperties.containsKey(Direction.UP)
         && directionalProperties.containsKey(Direction.DOWN);
      boolean doDirectionalFlipZ = directionalProperties != null
         && directionalProperties.containsKey(Direction.NORTH)
         && directionalProperties.containsKey(Direction.SOUTH);

      for (CustomBlockState possibleCustomState : customBlock.axiom$getPossibleCustomStates()) {
         BlockState vanillaState = possibleCustomState.getVanillaState();
         CustomBlockState rotated = possibleCustomState;
         CustomBlockState flippedX = possibleCustomState;
         CustomBlockState flippedY = possibleCustomState;
         CustomBlockState flippedZ = possibleCustomState;
         if (facingProperty != null) {
            Direction direction = possibleCustomState.getProperty(facingProperty);
            if (doClockwiseFacingRotate && direction.getAxis() != Axis.Y) {
               rotated = possibleCustomState.setPropertyUnsafe(facingProperty, direction.getClockWise());
            }

            if (facingValues.contains(direction.getOpposite())) {
               switch (direction.getAxis()) {
                  case X:
                     flippedX = possibleCustomState.setPropertyUnsafe(facingProperty, direction.getOpposite());
                     break;
                  case Y:
                     flippedY = possibleCustomState.setPropertyUnsafe(facingProperty, direction.getOpposite());
                     break;
                  case Z:
                     flippedZ = possibleCustomState.setPropertyUnsafe(facingProperty, direction.getOpposite());
               }
            }
         }

         if (axisProperty != null) {
            Axis axis = possibleCustomState.getProperty(axisProperty);
            if (axis == Axis.X) {
               if (axisValues.contains(Axis.Z)) {
                  rotated = rotated.setPropertyUnsafe(axisProperty, Axis.Z);
               }
            } else if (axis == Axis.Z && axisValues.contains(Axis.X)) {
               rotated = rotated.setPropertyUnsafe(axisProperty, Axis.X);
            }
         }

         if (doClockwiseDirectionalRotate) {
            for (Direction directionx : HORIZONTAL_DIRECTIONS) {
               Comparable<?> propertyValue = possibleCustomState.getProperty((Property)directionalProperties.get(directionx));
               rotated = rotated.setPropertyUnsafe(directionalProperties.get(directionx.getClockWise()), propertyValue);
            }
         }

         if (doDirectionalFlipX) {
            flippedX = flippedX.setPropertyUnsafe(
               directionalProperties.get(Direction.EAST), possibleCustomState.getProperty((Property)directionalProperties.get(Direction.WEST))
            );
            flippedX = flippedX.setPropertyUnsafe(
               directionalProperties.get(Direction.WEST), possibleCustomState.getProperty((Property)directionalProperties.get(Direction.EAST))
            );
         }

         if (doDirectionalFlipY) {
            flippedY = flippedY.setPropertyUnsafe(
               directionalProperties.get(Direction.UP), possibleCustomState.getProperty((Property)directionalProperties.get(Direction.DOWN))
            );
            flippedY = flippedY.setPropertyUnsafe(
               directionalProperties.get(Direction.DOWN), possibleCustomState.getProperty((Property)directionalProperties.get(Direction.UP))
            );
         }

         if (doDirectionalFlipZ) {
            flippedZ = flippedZ.setPropertyUnsafe(
               directionalProperties.get(Direction.NORTH), possibleCustomState.getProperty((Property)directionalProperties.get(Direction.SOUTH))
            );
            flippedZ = flippedZ.setPropertyUnsafe(
               directionalProperties.get(Direction.SOUTH), possibleCustomState.getProperty((Property)directionalProperties.get(Direction.NORTH))
            );
         }

         BlockHelper.customRotateY.put(vanillaState, rotated.getVanillaState());
         BlockHelper.customFlipX.put(vanillaState, flippedX.getVanillaState());
         BlockHelper.customFlipY.put(vanillaState, flippedY.getVanillaState());
         BlockHelper.customFlipZ.put(vanillaState, flippedZ.getVanillaState());
      }
   }

   @Nullable
   private static EnumMap<Direction, Property<?>> findSharedDirectionalProperties(List<Property<?>> properties, Direction[] directions) {
      EnumMap<Direction, Property<?>> directionalProperties = new EnumMap<>(Direction.class);
      Class<?> valueClass = null;
      List possibleValues = null;

      label37:
      for (Property<?> property : properties) {
         for (Direction value : directions) {
            if (property.getName().equals(value.getSerializedName())) {
               directionalProperties.put((Direction)value, property);
               if (valueClass == null) {
                  valueClass = property.getValueClass();
                  possibleValues = VersionUtils.helperGetPossibleValues(property);
                  break;
               }

               if (valueClass == property.getValueClass() && possibleValues.equals(VersionUtils.helperGetPossibleValues(property))) {
                  break;
               }

               directionalProperties = null;
               break label37;
            }
         }
      }

      if (directionalProperties != null && directionalProperties.size() < 2) {
         directionalProperties = null;
      }

      return directionalProperties;
   }

   public static void update() {
      EditorUI.getBlockList().markNeedsReload();
      if (Axiom.configuration.internal.rootEditorPalette != null) {
         Axiom.configuration.internal.rootEditorPalette.markNeedsReload();
      }

      BlockColourMap.invalidateCache();
   }

   @Nullable
   public static CustomBlock getCustomBlock(ResourceLocation resourceLocation) {
      return customBlockMap.get(resourceLocation);
   }

   @Nullable
   public static CustomBlockState getCustomStateFor(BlockState blockState) {
      return vanillaStateToCustomBlockState.get(blockState);
   }

   public static CustomBlockState getCustomOrVanillaStateFor(BlockState blockState) {
      CustomBlockState customBlockState = vanillaStateToCustomBlockState.get(blockState);
      return customBlockState == null ? (CustomBlockState)blockState : customBlockState;
   }

   public static CustomBlock getFromItemStack(ItemStack itemStack) {
      String customBlockPlacer = ItemStackDataHelper.getCustomBlockPlacer(itemStack);
      if (customBlockPlacer != null) {
         CustomBlock customBlock = getCustomBlock(ResourceLocation.parse(customBlockPlacer));
         if (customBlock != null) {
            return customBlock;
         }
      }

      return (CustomBlock)Block.byItem(itemStack.getItem());
   }

   public static void registerUpdater(ResourceLocation resourceLocation, CustomBlockPlacementLogic customBlockUpdater) {
      placementLogicMap.put(resourceLocation, customBlockUpdater);
   }

   public static String serialize(CustomBlockState customBlockState) {
      if (customBlockState instanceof BlockState blockState) {
         return BlockStateParser.serialize(blockState);
      } else if (!(customBlockState instanceof CustomBlockStateImplementation state)) {
         throw new FaultyImplementationError();
      } else {
         StringBuilder stringBuilder = new StringBuilder(state.getCustomBlock().axiom$getIdentifier().toString());
         if (!state.getProperties().isEmpty()) {
            stringBuilder.append('[');
            boolean addComma = false;

            for (Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
               if (addComma) {
                  stringBuilder.append(',');
               }

               appendProperty(stringBuilder, entry.getKey(), entry.getValue());
               addComma = true;
            }

            stringBuilder.append(']');
         }

         return stringBuilder.toString();
      }
   }

   private static <T extends Comparable<T>> void appendProperty(StringBuilder stringBuilder, Property<T> property, Comparable<?> comparable) {
      stringBuilder.append(property.getName());
      stringBuilder.append('=');
      stringBuilder.append(property.getName((T)comparable));
   }

   public static CustomBlockState deserialize(String input) {
      return deserialize(input, null);
   }

   public static CustomBlockState deserialize(String input, Set<String> nonDefaultProperties) {
      try {
         BlockResult result = BlockStateParser.parseForBlock(VersionUtils.createLookup(BuiltInRegistries.BLOCK), input, false);
         if (nonDefaultProperties != null) {
            for (Property<?> property : result.properties().keySet()) {
               nonDefaultProperties.add(property.getName());
            }
         }

         return (CustomBlockState)result.blockState();
      } catch (Exception var11) {
         try {
            StringReader reader = new StringReader(input);
            ResourceLocation resourceLocation = ResourceLocation.read(reader);
            CustomBlock customBlock = getCustomBlock(resourceLocation);
            if (customBlock == null) {
               return null;
            } else {
               CustomBlockImplementation customBlockImplementation = (CustomBlockImplementation)customBlock;
               StateDefinition<CustomBlock, CustomBlockStateImplementation> definition = customBlockImplementation.getStateDefinition();
               CustomBlockState customBlockState = customBlockImplementation.axiom$defaultCustomState();
               if (reader.canRead()) {
                  if (reader.peek() != '[') {
                     return null;
                  }

                  reader.skip();
                  reader.skipWhitespace();

                  while (reader.canRead() && reader.peek() != ']') {
                     reader.skipWhitespace();
                     String string = reader.readString();
                     Property<?> property = definition.getProperty(string);
                     if (property == null) {
                        return null;
                     }

                     reader.skipWhitespace();
                     if (!reader.canRead() || reader.peek() != '=') {
                        return null;
                     }

                     reader.skip();
                     reader.skipWhitespace();
                     if (nonDefaultProperties != null) {
                        nonDefaultProperties.add(property.getName());
                     }

                     customBlockState = setValue(customBlockState, property, reader.readString());
                     if (customBlockState == null) {
                        return null;
                     }

                     reader.skipWhitespace();
                     if (reader.canRead()) {
                        if (reader.peek() != ',') {
                           if (reader.peek() != ']') {
                              return null;
                           }
                           break;
                        }

                        reader.skip();
                     }
                  }
               }

               return customBlockState;
            }
         } catch (Exception var10) {
            return null;
         }
      }
   }

   private static <T extends Comparable<T>> CustomBlockState setValue(CustomBlockState customBlockState, Property<T> property, String string) {
      Optional<T> optional = property.getValue(string);
      return optional.isEmpty() ? null : customBlockState.setPropertyUnsafe(property, optional.get());
   }

   private static void registerDefaultUpdaters() {
      registerUpdater(ResourceLocation.parse("axiom:axis"), new AxisCustomBlockPlacementLogic());
      registerUpdater(ResourceLocation.parse("axiom:facing"), new FacingCustomBlockPlacementLogic());
      registerUpdater(ResourceLocation.parse("axiom:facing_opposite"), new FacingOppositeCustomBlockPlacementLogic());
      registerUpdater(ResourceLocation.parse("axiom:facing_clicked"), new FacingClickedCustomBlockPlacementLogic());
      registerUpdater(ResourceLocation.parse("axiom:facing_clicked_opposite"), new FacingClickedOppositeCustomBlockPlacementLogic());
      registerUpdater(ResourceLocation.parse("axiom:waterlogged"), new WaterloggedCustomBlockPlacementLogic());
      registerUpdater(ResourceLocation.parse("axiom:half"), new HalfCustomBlockPlacementLogic());
   }

   static {
      registerDefaultUpdaters();
   }
}
