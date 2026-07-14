package com.moulberry.axiom;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Transformation;
import com.mojang.serialization.DataResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class VersionUtils {
   public static Vector3fc Transformation_translation(Transformation transformation) {
      return transformation.getTranslation();
   }

   public static Quaternionfc Transformation_leftRotation(Transformation transformation) {
      return transformation.getLeftRotation();
   }

   public static Vector3fc Transformation_scale(Transformation transformation) {
      return transformation.getScale();
   }

   public static Quaternionfc Transformation_rightRotation(Transformation transformation) {
      return transformation.getRightRotation();
   }

   public static void ChatComponent_addClientSystemMessage(ChatComponent chatComponent, Component component) {
      chatComponent.addMessage(component);
   }

   public static ChunkPos ChunkPos_unpack(long value) {
      return new ChunkPos(value);
   }

   public static long ChunkPos_instance_pack(ChunkPos chunkPos) {
      return chunkPos.toLong();
   }

   public static long ChunkPos_static_pack(int x, int z) {
      return ChunkPos.asLong(x, z);
   }

   public static int ChunkPos_x(ChunkPos chunkPos) {
      return chunkPos.x;
   }

   public static int ChunkPos_z(ChunkPos chunkPos) {
      return chunkPos.z;
   }

   public static AABB AABB_encapsulatingFullBlocks(BlockPos first, BlockPos second) {
      return AABB.encapsulatingFullBlocks(first, second);
   }

   public static AABB legacyEncapsulatingFullBlocks(BlockPos first, BlockPos second) {
      return new AABB(
         Math.min(first.getX(), second.getX()),
         Math.min(first.getY(), second.getY()),
         Math.min(first.getZ(), second.getZ()),
         Math.max(first.getX(), second.getX()) + 1,
         Math.max(first.getY(), second.getY()) + 1,
         Math.max(first.getZ(), second.getZ()) + 1
      );
   }

   public static Transformation Transformation_new(Vector3fc translation, Quaternionfc leftrot, Vector3fc scale, Quaternionfc rightrot) {
      return new Transformation(new Vector3f(translation), new Quaternionf(leftrot), new Vector3f(scale), new Quaternionf(rightrot));
   }

   public static ResourceLocation ResourceKey_identifier(ResourceKey resourceKey) {
      return resourceKey.location();
   }

   public static Key GameRules_ADVANCE_WEATHER() {
      return GameRules.RULE_WEATHER_CYCLE;
   }

   public static Key GameRules_BLOCK_DROPS() {
      return GameRules.RULE_DOBLOCKDROPS;
   }

   public static Key GameRules_SPAWN_MOBS() {
      return GameRules.RULE_DOMOBSPAWNING;
   }

   public static Key GameRules_RANDOM_TICK_SPEED() {
      return GameRules.RULE_RANDOMTICKING;
   }

   public static Key GameRules_ADVANCE_TIME() {
      return GameRules.RULE_DAYLIGHT;
   }

   public static Boolean GameRules_getBool(GameRules gameRules, Key<BooleanValue> rule) {
      return ((BooleanValue)gameRules.getRule(rule)).get();
   }

   public static void GameRules_setBool(GameRules gameRules, Key<BooleanValue> rule, Boolean value, MinecraftServer minecraftServer) {
      ((BooleanValue)gameRules.getRule(rule)).set(value, minecraftServer);
   }

   public static Integer GameRules_getInt(GameRules gameRules, Key<IntegerValue> rule) {
      return ((IntegerValue)gameRules.getRule(rule)).get();
   }

   public static void GameRules_setInt(GameRules gameRules, Key<IntegerValue> rule, Integer value, MinecraftServer minecraftServer) {
      ((IntegerValue)gameRules.getRule(rule)).set(value, minecraftServer);
   }

   public static boolean isAlwaysFlying(MultiPlayerGameMode gameMode) {
      return gameMode == null ? false : gameMode.isAlwaysFlying();
   }

   public static boolean Level_isClientSide(Level level) {
      return level.isClientSide;
   }

   public static UUID GameProfile_id(GameProfile gameProfile) {
      return gameProfile.getId();
   }

   public static String GameProfile_name(GameProfile gameProfile) {
      return gameProfile.getName();
   }

   public static <T extends Comparable<T>> List<T> helperGetPossibleValues(Property<T> property) {
      Collection<T> possibleValues = property.getPossibleValues();
      return possibleValues instanceof List list ? list : List.copyOf(possibleValues);
   }

   public static boolean isWithinBlockInteractionRange(Player player, BlockPos blockPos, double leniency) {
      return player.canInteractWithBlock(blockPos, leniency);
   }

   public static boolean legacyCanInteractWithBlock(Player player, BlockPos blockPos, double leniency) {
      double distance = player.blockInteractionRange() + leniency;
      return new AABB(blockPos).distanceToSqr(player.getEyePosition()) < distance * distance;
   }

   public static String Component_tryCollapseToString(Component component) {
      return component.tryCollapseToString();
   }

   public static String legacyTryCollapseToString(Component component) {
      return component.tryCollapseToString();
   }

   public static boolean Player_hasInfiniteMaterials(Player player) {
      return player.hasInfiniteMaterials();
   }

   public static Transformation Transformation_new(Matrix4fc matrix4fc) throws CommandSyntaxException {
      return helperTransformationNew(matrix4fc);
   }

   public static Transformation helperTransformationNew(Matrix4fc matrix4fc) {
      return matrix4fc instanceof Matrix4f matrix ? new Transformation(matrix) : new Transformation(new Matrix4f(matrix4fc));
   }

   public static CompoundTag TagParser_parseCompoundFully(String compound) throws CommandSyntaxException {
      return TagParser.parseTag(compound);
   }

   public static int Inventory_getSelectedSlot(Inventory inventory) {
      return inventory.selected;
   }

   public static Block DefaultedRegistry_getBlock(DefaultedRegistry<Block> registry, ResourceLocation resourceLocation) {
      return (Block)registry.get(resourceLocation);
   }

   public static void Inventory_setSelectedSlot(Inventory inventory, int slot) {
      inventory.selected = slot;
   }

   public static void Inventory_addAndPickItem(Inventory inventory, ItemStack itemStack) {
      inventory.setPickedItem(itemStack);
   }

   public static Vec3 BlockState_getOffset(BlockState blockState, BlockPos blockPos) {
      return blockState.getOffset(EmptyBlockGetter.INSTANCE, blockPos);
   }

   public static VoxelShape BlockState_getFaceOcclusionShape(BlockState blockState, Direction direction) {
      return blockState.getFaceOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction);
   }

   public static Entity genericLoadEntityRecursive(CompoundTag tag, Level level, MobSpawnType spawnReason, VersionUtils.LegacyEntityProcessor function) {
      return EntityType.loadEntityRecursive(tag, level, function::process);
   }

   private static void schedule(BlockableEventLoop blockableEventLoop, Runnable runnable) {
      blockableEventLoop.tell(runnable);
   }

   private static Optional<Named> Registry_getTag(Registry registry, TagKey key) {
      return registry.getTag(key);
   }

   private static Optional<Reference> Registry_getReference(Registry registry, ResourceKey key) {
      return registry.getHolder(key);
   }

   private static Optional<Registry> RegistryAccess_lookup(RegistryAccess registryAccess, ResourceKey key) {
      return registryAccess.registry(key);
   }

   private static boolean BlockState_isSolidRender(BlockState blockState) {
      return blockState.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
   }

   public static void LevelChunk_markUnsaved(LevelChunk levelChunk) {
      levelChunk.setUnsaved(true);
   }

   public static <T> HolderLookup<T> createLookup(Registry<T> registry) {
      return registry.asLookup();
   }

   public static boolean hasDifferentLightProperties(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, BlockState blockState2) {
      return LightEngine.hasDifferentLightProperties(blockGetter, blockPos, blockState, blockState2);
   }

   public static boolean shouldSwing(InteractionResult interactionResult) {
      return interactionResult.shouldSwing();
   }

   public static boolean shouldRenderFace(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction, BlockPos neighborPosition) {
      return Block.shouldRenderFace(blockState, blockGetter, blockPos, direction, neighborPosition);
   }

   public void Entity_teleportTo(
      Entity entity, ServerLevel serverLevel, double x, double y, double z, Set<RelativeMovement> set, float yaw, float pitch, boolean stopSpectating
   ) {
      genericTeleportTo(entity, serverLevel, x, y, z, set, yaw, pitch, stopSpectating);
   }

   public static void genericTeleportTo(
      Entity entity, ServerLevel serverLevel, double x, double y, double z, Set<RelativeMovement> set, float yaw, float pitch, boolean stopSpectating
   ) {
      entity.teleportTo(serverLevel, x, y, z, set, yaw, pitch);
   }

   private static int LevelHeightAccessor_getMinSectionY(LevelHeightAccessor levelAccessor) {
      return levelAccessor.getMinSection();
   }

   private static int LevelHeightAccessor_getMaxSectionY(LevelHeightAccessor levelAccessor) {
      return levelAccessor.getMaxSection() - 1;
   }

   private static int LevelHeightAccessor_getMinY(LevelHeightAccessor levelAccessor) {
      return levelAccessor.getMinBuildHeight();
   }

   private static int LevelHeightAccessor_getMaxY(LevelHeightAccessor levelAccessor) {
      return levelAccessor.getMaxBuildHeight() - 1;
   }

   private static int LevelReader_getMinY(LevelReader levelReader) {
      return levelReader.getMinBuildHeight();
   }

   private static ResourceLocation Identifier_fromNamespaceAndPath(String namespace, String path) {
      return ResourceLocation.fromNamespaceAndPath(namespace, path);
   }

   private static ResourceLocation Identifier_parse(String string) {
      return ResourceLocation.parse(string);
   }

   private static double Player_blockInteractionRange(Player player) {
      return player.blockInteractionRange();
   }

   private static RegistryAccess Entity_registryAccess(Entity entity) {
      return entity.registryAccess();
   }

   public static void helperLoadBlockEntity(BlockEntity blockEntity, CompoundTag tag, Provider provider) {
      throw new UnsupportedOperationException();
   }

   private static CompoundTag BlockEntity_saveWithoutMetadata(BlockEntity blockEntity, Provider provider) {
      return blockEntity.saveWithoutMetadata(provider);
   }

   public static <R> R getOrThrow(DataResult<R> dataResult) {
      return (R)dataResult.getOrThrow();
   }

   public static <T> T wasteFirst(Object ignore, T object) {
      return object;
   }

   public static <T> T wasteSecond(T object, Object ignore) {
      return object;
   }

   private static String Display_TAG_POS_ROT_INTERPOLATION_DURATION() {
      return "teleport_duration";
   }

   private static String Display_TAG_TRANSFORMATION_INTERPOLATION_DURATION() {
      return "interpolation_duration";
   }

   private static CompoundTag readCompressedNbt(Path path) throws IOException {
      return NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
   }

   private static CompoundTag readCompressedNbt(InputStream inputStream) throws IOException {
      return NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
   }

   private static void writeCompressedNbt(CompoundTag tag, Path path) throws IOException {
      NbtIo.writeCompressed(tag, path);
   }

   private static Block Block_SHORT_GRASS() {
      return Blocks.SHORT_GRASS;
   }

   @FunctionalInterface
   public interface LegacyEntityProcessor {
      Entity process(Entity var1);
   }
}
