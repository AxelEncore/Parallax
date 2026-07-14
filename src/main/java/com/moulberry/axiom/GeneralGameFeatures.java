package com.moulberry.axiom;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.capabilities.Capability;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.MinecraftExt;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import com.moulberry.axiom.utils.PositionUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class GeneralGameFeatures {
   public static final int CUSTOM_CREATIVE_TAB_COLOUR = 0;
   public static final int CUSTOM_CREATIVE_TAB_GRADIENT = 1;
   public static int lastOpenedCustomCreativeTab = -1;

   public static UserAction.ActionResult callAction(UserAction userAction, Object object) {
      if (!EditorUI.isActive() && AxiomClient.isAxiomActive()) {
         if (userAction == UserAction.COPY) {
            HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult != null
               && hitResult.getType() == Type.BLOCK
               && hitResult instanceof BlockHitResult blockHitResult
               && tryAxiomPickBlock(Minecraft.getInstance().player, Minecraft.getInstance().level, blockHitResult.getBlockPos(), true, true, true)) {
               return UserAction.ActionResult.USED_STOP;
            }
         }

         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   public static InteractionResult handleUseCustomItemOn(LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult) {
      ItemStack heldItem = localPlayer.getItemInHand(interactionHand);
      CompoundTag entityPlacer = ItemStackDataHelper.getEntityPlacer(heldItem);
      if (entityPlacer == null) {
         return InteractionResult.PASS;
      } else {
         BlockPos blockPos = blockHitResult.getBlockPos();
         BlockPos offset = blockPos.relative(blockHitResult.getDirection());
         Vec3 spawnPosition = Vec3.atCenterOf(offset);
         Vec3 eyePosition = localPlayer.getEyePosition();
         double dx = eyePosition.x - spawnPosition.x;
         double dz = eyePosition.z - spawnPosition.z;
         float yaw;
         if (dx * dx + dz * dz < 0.25) {
            yaw = localPlayer.getYRot() + 180.0F;
         } else {
            yaw = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
         }

         if (localPlayer.isShiftKeyDown()) {
            yaw = Math.round(yaw);
         } else {
            yaw = Math.round(yaw / 90.0F) * 90;
         }

         new AxiomServerboundSpawnEntity(List.of(new AxiomServerboundSpawnEntity.SpawnEntry(UUID.randomUUID(), spawnPosition, yaw, 0.0F, null, entityPlacer)))
            .send();
         return InteractionResult.SUCCESS;
      }
   }

   public static boolean tryAxiomPickBlock(LocalPlayer player, Level level, BlockPos blockPos, boolean force, boolean includeData, boolean exactState) {
      if (!AxiomClient.isAxiomActive() || player == null || level == null) {
         return false;
      } else if (!player.hasInfiniteMaterials()) {
         return false;
      } else {
         BlockState blockState = level.getBlockState(blockPos);
         if (blockState.isAir()) {
            return false;
         } else {
            CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
            ItemStack customPickBlockState = customBlockState == null ? null : customBlockState.getCustomBlock().axiom$customPickBlockStack();
            boolean autoSwapToOtherHotbarWithItem = Axiom.configuration.contextMenu.autoSwapToOtherHotbarWithItem;
            boolean shouldCustomPickBecauseOfCustomBlock = customPickBlockState != null && !customBlockState.getCustomBlock().sendServerPickBlockIfPossible();
            boolean shouldCustomPick = force
               || shouldCustomPickBecauseOfCustomBlock
               || !player.canInteractWithBlock(blockPos, 0.5) && Axiom.configuration.capabilities.infiniteReach;
            if (!autoSwapToOtherHotbarWithItem && !shouldCustomPick) {
               return false;
            } else {
               ItemStack itemStack = customPickBlockState;
               if (customPickBlockState == null) {
                  itemStack = blockState.getBlock().getCloneItemStack(level, blockPos, blockState);
                  if (customBlockState != null) {
                     ItemStackDataHelper.setCustomBlockPlacer(itemStack, customBlockState.getCustomBlock().axiom$getIdentifier().toString());
                  }
               }

               if (itemStack.isEmpty()) {
                  return true;
               } else {
                  if (includeData && blockState.hasBlockEntity()) {
                     BlockEntity blockEntity = level.getBlockEntity(blockPos);
                     if (blockEntity != null) {
                        ((MinecraftExt)Minecraft.getInstance()).axiom$addCustomNbtData(itemStack, blockEntity, level.registryAccess());
                     }
                  }

                  if (exactState && !blockState.getProperties().isEmpty()) {
                     Map<String, String> properties = new HashMap<>();

                     for (Property<?> property : blockState.getProperties()) {
                        properties.put(property.getName(), serialize(blockState, property));
                     }

                     ItemStackDataHelper.setBlockStateTag(itemStack, properties);
                     itemStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                  }

                  if (autoSwapToOtherHotbarWithItem && AxiomClient.hasPermission(AxiomPermission.PLAYER_HOTBAR)) {
                     int matchingHotbar = HotbarManager.findHotbarWithMatchingItem(itemStack);
                     if (matchingHotbar >= 0) {
                        ScreenRenderHook.setOverlayText(Component.literal(AxiomI18n.get("axiom.hardcoded.switched_to_hotbar") + (matchingHotbar + 1)));
                        HotbarManager.setActiveHotbarIndex(matchingHotbar);
                        Inventory inventory = player.getInventory();
                        int matching = inventory.findSlotMatchingItem(itemStack);
                        if (matching != -1 && Inventory.isHotbarSlot(matching)) {
                           inventory.selected = matching;
                        }

                        return true;
                     }
                  }

                  if (!shouldCustomPick) {
                     return false;
                  } else {
                     Inventory inventory = player.getInventory();
                     int matching = inventory.findSlotMatchingItem(itemStack);
                     if (matching != -1) {
                        if (Inventory.isHotbarSlot(matching)) {
                           inventory.selected = matching;
                        } else {
                           inventory.pickSlot(matching);
                           Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(inventory.getItem(matching), 9 + matching);
                           Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inventory.selected);
                        }
                     } else {
                        inventory.setPickedItem(itemStack);
                        Minecraft.getInstance().gameMode.handleCreativeModeItemAdd(player.getItemInHand(InteractionHand.MAIN_HAND), 36 + inventory.selected);
                     }

                     return true;
                  }
               }
            }
         }
      }
   }

   private static <T extends Comparable<T>> String serialize(BlockState blockState, Property<T> property) {
      T comparable = (T)blockState.getValue(property);
      return property.getName(comparable);
   }

   public static void injectPickReturn(Minecraft minecraft, float partialTick, CallbackInfo ci) {
      if (AxiomClient.isAxiomActive()) {
         if (!BuilderToolManager.isToolSlotActive()
            && Capability.INFINITE_REACH.isEnabled()
            && minecraft.hitResult != null
            && minecraft.hitResult.getType() == Type.MISS) {
            Entity entity = minecraft.cameraEntity;
            if (entity == null) {
               return;
            }

            if (minecraft.level == null) {
               return;
            }

            int infiniteReachLimit = ClientRestrictions.getInfiniteReachLimit();
            if (infiniteReachLimit < 0) {
               Vec3 eye = entity.getEyePosition(partialTick);
               Vec3 view = entity.getViewVector(1.0F);
               RayCaster.RaycastResult result = RayCaster.raycast(minecraft.level, eye, view, false, false);
               if (result != null) {
                  minecraft.hitResult = new BlockHitResult(result.getLocation(), result.direction(), result.blockPos(), false);
               }
            } else {
               minecraft.hitResult = entity.pick(infiniteReachLimit, partialTick, false);
            }
         }
      }
   }

   public static void injectPickHead(Minecraft minecraft, float partialTick, CallbackInfo ci) {
      if (AxiomClient.isAxiomActive()) {
         if (BuilderToolManager.isToolSlotActive()) {
            Entity entity = minecraft.cameraEntity;
            if (entity == null) {
               return;
            }

            if (minecraft.level == null) {
               return;
            }

            Vec3 eye = entity.getEyePosition(partialTick);
            Vec3 view = entity.getViewVector(1.0F);
            RayCaster.RaycastResult result = RayCaster.raycast(minecraft.level, eye, view, false, false);
            if (result != null) {
               minecraft.hitResult = new BlockHitResult(result.getLocation(), result.direction(), result.blockPos(), false);
            } else {
               Vec3 to = eye.add(view.x * 64.0, view.y * 64.0, view.z * 64.0);
               minecraft.hitResult = BlockHitResult.miss(to, PositionUtils.getNearestDirection(view.x, view.y, view.z), BlockPos.containing(to));
            }

            ci.cancel();
         }
      }
   }
}
