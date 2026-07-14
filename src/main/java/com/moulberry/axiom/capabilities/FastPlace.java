package com.moulberry.axiom.capabilities;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.hooks.MinecraftExt;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class FastPlace {
   private static Vec3 lastView;
   private static Object2IntMap<BlockPos> placedPositions = new Object2IntOpenHashMap();
   private static int fastPlaceDisabledTimer = 0;

   public static void tick() {
      if (!BuilderToolManager.isToolSlotActive()) {
         if (Capability.FAST_PLACE.isEnabled() && !Capability.REPLACE_MODE.isEnabled()) {
            if (fastPlaceDisabledTimer > 0) {
               fastPlaceDisabledTimer--;
            } else {
               Minecraft mc = Minecraft.getInstance();
               MinecraftExt ext = (MinecraftExt)mc;
               if (ext.axiom$getRightClickDelay() > 0) {
                  HitResult hitResult = Minecraft.getInstance().hitResult;
                  if (hitResult.getType() == Type.BLOCK) {
                     BlockPos blockPos = ((BlockHitResult)hitResult).getBlockPos();
                     if (!placedPositions.containsKey(blockPos)) {
                        ((MinecraftExt)mc).axiom$setRightClickDelay(0);
                     }
                  }
               }

               Object2IntMap<BlockPos> newPlacedPositions = new Object2IntOpenHashMap();
               ObjectIterator var6 = placedPositions.entrySet().iterator();

               while (var6.hasNext()) {
                  Entry<BlockPos, Integer> entry = (Entry<BlockPos, Integer>)var6.next();
                  if (entry.getValue() > 0) {
                     newPlacedPositions.put(entry.getKey(), entry.getValue() - 1);
                  }
               }

               placedPositions = newPlacedPositions;
            }
         } else {
            placedPositions.clear();
         }
      }
   }

   public static void afterUseItemOn(HitResult hitResult, boolean angel) {
      if (!BuilderToolManager.isToolSlotActive()) {
         if (Capability.FAST_PLACE.isEnabled() && (!Capability.REPLACE_MODE.isEnabled() || angel)) {
            if (hitResult.getType() == Type.BLOCK) {
               BlockHitResult blockHitResult = (BlockHitResult)hitResult;
               placedPositions.put(blockHitResult.getBlockPos(), 3);
               BlockPos placedAt = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
               placedPositions.put(placedAt, 3);
               ClientLevel level = Minecraft.getInstance().level;
               if (level != null) {
                  BlockState blockAt = level.getBlockState(placedAt);
                  if (blockAt.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                     && blockAt.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                     placedPositions.put(placedAt.above(), 3);
                  }
               }
            }
         }
      }
   }

   public static void disableTemporarily() {
      fastPlaceDisabledTimer = 5;
   }

   public static void resetPlacedPositions() {
      placedPositions.clear();
   }

   public static void setLastView(Vec3 lastView) {
      FastPlace.lastView = lastView;
   }

   public static boolean handleFastPlace(ClientLevel level, LocalPlayer localPlayer, MultiPlayerGameMode gameMode) {
      if (fastPlaceDisabledTimer > 0) {
         return false;
      } else if (Capability.FAST_PLACE.isEnabled() && !Capability.REPLACE_MODE.isEnabled()) {
         HitResult mcHitResult = Minecraft.getInstance().hitResult;
         if (mcHitResult != null && mcHitResult.getType() == Type.BLOCK) {
            float range = (float)localPlayer.blockInteractionRange();
            int infiniteReachLimit = ClientRestrictions.getInfiniteReachLimit();
            if (Capability.INFINITE_REACH.isEnabled() && infiniteReachLimit > range) {
               range = infiniteReachLimit;
            }

            boolean swung = false;

            for (int i = 0; i < 100; i++) {
               Vec3 eye = localPlayer.getEyePosition(i / 100.0F);
               Vec3 view = lastView.lerp(localPlayer.getViewVector(1.0F), i / 100.0F);
               Vec3 end = eye.add(view.x * range, view.y * range, view.z * range);
               BlockHitResult hitResult = level.clip(new ClipContext(eye, end, Block.OUTLINE, Fluid.NONE, localPlayer));
               if (hitResult.getType() == Type.MISS && Capability.INFINITE_REACH.isEnabled() && infiniteReachLimit < 0) {
                  RayCaster.RaycastResult result = RayCaster.raycast(level, eye, view, false, false);
                  if (result == null) {
                     continue;
                  }

                  hitResult = new BlockHitResult(result.getLocation(), result.direction(), result.blockPos(), false);
               }

               if (hitResult.getType() == Type.BLOCK && !placedPositions.containsKey(hitResult.getBlockPos())) {
                  placedPositions.put(hitResult.getBlockPos(), 3);

                  for (InteractionHand interactionHand : InteractionHand.values()) {
                     ItemStack itemInHand = localPlayer.getItemInHand(interactionHand);
                     if (!itemInHand.isEmpty() && !(itemInHand.getItem() instanceof BlockItem)) {
                        return false;
                     }

                     InteractionResult interactionResult = gameMode.useItemOn(localPlayer, interactionHand, hitResult);
                     if (interactionResult.consumesAction()) {
                        if (!swung && VersionUtils.shouldSwing(interactionResult)) {
                           swung = true;
                           localPlayer.swing(InteractionHand.MAIN_HAND);
                        }

                        if (fastPlaceDisabledTimer > 0) {
                           return true;
                        }
                        break;
                     }

                     if (fastPlaceDisabledTimer > 0) {
                        return true;
                     }
                  }
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
