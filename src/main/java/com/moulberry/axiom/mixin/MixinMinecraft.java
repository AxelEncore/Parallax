package com.moulberry.axiom.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ContextMenuManager;
import com.moulberry.axiom.GeneralGameFeatures;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.capabilities.AngelPlacement;
import com.moulberry.axiom.capabilities.Bulldozer;
import com.moulberry.axiom.capabilities.Capability;
import com.moulberry.axiom.capabilities.FastPlace;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.MinecraftExt;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.render.BiomeOverlayRenderer;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.CollisionMeshOverlayRenderer;
import com.moulberry.axiom.render.ShaderManager;
import com.moulberry.axiom.screen.CreativeColourScreen;
import com.moulberry.axiom.screen.CreativeGradientScreen;
import com.moulberry.axiom.tools.ToolManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {Minecraft.class},
   priority = 1001
)
public abstract class MixinMinecraft implements MinecraftExt {
   @Shadow
   public Screen screen;
   @Shadow
   private int rightClickDelay;
   @Shadow
   @Final
   private Window window;
   @Shadow
   @Final
   public Gui gui;
   @Shadow
   @Nullable
   public HitResult hitResult;
   @Shadow
   @Nullable
   public MultiPlayerGameMode gameMode;
   @Shadow
   @Nullable
   public LocalPlayer player;
   @Shadow
   @Nullable
   public ClientLevel level;
   @Shadow
   @Final
   public Options options;
   @Shadow
   public int missTime;
   @Shadow
   @Final
   @Mutable
   private RenderTarget mainRenderTarget;
   @Unique
   private RenderTarget overrideMainRenderTarget = null;

   @Shadow
   public abstract Entity getCameraEntity();

   @Shadow
   protected abstract void addCustomNbtData(ItemStack var1, BlockEntity var2, RegistryAccess var3);

   @Shadow
   public abstract RenderBuffers renderBuffers();

   @Shadow
   public abstract void openChatScreen(String var1);

   @Inject(
      method = {"pickBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void pickBlockInfinite(CallbackInfo ci) {
      if (this.hitResult == null || this.hitResult.getType() == Type.MISS) {
         if (Capability.INFINITE_REACH.isEnabled()) {
            Vec3 eye = this.player.getEyePosition(1.0F);
            Vec3 view = this.player.getViewVector(1.0F);
            RayCaster.RaycastResult result = RayCaster.raycast(this.level, eye, view, false, false);
            if (result != null
               && GeneralGameFeatures.tryAxiomPickBlock(
                  this.player,
                  this.level,
                  result.blockPos(),
                  true,
                  VersionUtilsClient.wasteFirstBoolean(Minecraft.getInstance(), Screen.hasControlDown()),
                  false
               )) {
               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"startUseItem"},
      at = {@At(
         value = "HEAD",
         target = "Lnet/minecraft/client/Minecraft;startUseItem()V"
      )},
      cancellable = true
   )
   public void startUseItem(CallbackInfo ci) {
      if (AxiomClient.isAxiomActive()) {
         if (!this.gameMode.isDestroying() && !this.player.isHandsBusy()) {
            if (ContextMenuManager.getInstance().isActive()) {
               this.rightClickDelay = 4;
               ci.cancel();
            } else if (UserAction.RIGHT_MOUSE.call(null) == UserAction.ActionResult.USED_STOP) {
               this.rightClickDelay = 4;
               ci.cancel();
            } else if (FastPlace.handleFastPlace(this.level, this.player, this.gameMode)) {
               this.rightClickDelay = 4;
               ci.cancel();
            } else {
               for (InteractionHand hand : InteractionHand.values()) {
                  ItemStack heldItem = this.player.getItemInHand(hand);
                  if (heldItem.getItem() instanceof BlockItem) {
                     if (AngelPlacement.handlePlace(this.level, this.player, hand, heldItem)) {
                        this.rightClickDelay = 4;
                        ci.cancel();
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   @Override
   public void axiom$pushMainRenderTarget(RenderTarget renderTarget) {
      this.overrideMainRenderTarget = renderTarget;
   }

   @Override
   public void axiom$popMainRenderTarget() {
      this.overrideMainRenderTarget = null;
   }

   @Inject(
      method = {"getMainRenderTarget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
      if (this.overrideMainRenderTarget != null) {
         cir.setReturnValue(this.overrideMainRenderTarget);
      }
   }

   @Override
   public void axiom$setRightClickDelay(int rightClickDelay) {
      this.rightClickDelay = rightClickDelay;
   }

   @Override
   public int axiom$getRightClickDelay() {
      return this.rightClickDelay;
   }

   @Inject(
      method = {"resizeDisplay"},
      at = {@At("RETURN")}
   )
   public void resizeDisplay(CallbackInfo ci) {
      ShaderManager.INSTANCE.onResolutionChanged(this.window.getWidth(), this.window.getHeight());
   }

   @Inject(
      method = {"runTick"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V",
         shift = Shift.AFTER
      )}
   )
   public void afterMainBlit(CallbackInfo ci) {
      if (RenderSystem.isOnRenderThread()) {
         EditorUI.drawOverlay();
      }
   }

   @Inject(
      method = {"continueAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void continueAttack(boolean attacking, CallbackInfo ci) {
      if (AxiomClient.isAxiomActive()) {
         if (!BuilderToolManager.isToolSlotActive()
            && (!EditorUI.isActive() || !Placement.INSTANCE.isPlacing())
            && !ToolManager.isToolActive()
            && !DisplayEntityManipulator.hasActiveGizmo()
            && !MarkerEntityManipulator.hasActiveGizmo()) {
            Entity entity = this.getCameraEntity();
            if (entity == null || entity != this.player) {
               Bulldozer.resetInstabreakCountdown();
            } else if (!attacking) {
               Bulldozer.increaseInstabreakCountdown();
            } else if (Capability.BULLDOZER.isEnabled()) {
               if (this.hitResult != null && this.hitResult.getType() == Type.BLOCK) {
                  if (this.level != null && this.gameMode != null && this.gameMode.getPlayerMode().isCreative()) {
                     if (Bulldozer.handleInstabreak(this.level, this.player, this.gameMode)) {
                        ci.cancel();
                     }
                  }
               }
            }
         } else {
            Bulldozer.resetInstabreakCountdown();
            ci.cancel();
            this.missTime = 5;
         }
      }
   }

   @Inject(
      method = {"startAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void startAttack(CallbackInfoReturnable<Boolean> ci) {
      if (AxiomClient.isAxiomActive()) {
         if (UserAction.LEFT_MOUSE.call(null) == UserAction.ActionResult.USED_STOP) {
            ci.setReturnValue(false);
            this.missTime = 5;
         }
      }
   }

   @Inject(
      method = {"setLevel"},
      at = {@At("HEAD")}
   )
   public void setLevel(CallbackInfo ci) {
      ChunkRenderOverrider.clear();
      BiomeOverlayRenderer.INSTANCE.clear();
      CollisionMeshOverlayRenderer.INSTANCE.clear();
   }

   @Inject(
      method = {"handleKeybinds"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Minecraft;startUseItem()V",
         ordinal = 0,
         shift = Shift.BEFORE
      )}
   )
   public void onRightClick(CallbackInfo ci) {
      FastPlace.resetPlacedPositions();
   }

   @Redirect(
      method = {"handleKeybinds"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/entity/player/Inventory;selected:I",
         opcode = 181
      )
   )
   public void updateSelectedSlot(Inventory instance, int value) {
      if (ContextMenuManager.getInstance().isActive()) {
         ContextMenuManager.getInstance().slotSelected(value);
      } else {
         BuilderToolManager.setToolSlotActive(false);
         DisplayEntityManipulator.disableActive();
         MarkerEntityManipulator.disableActive();
         this.player.getInventory().selected = value;
      }
   }

   @Inject(
      method = {"setScreen"},
      at = {@At("HEAD")},
      require = 0
   )
   public void setScreen(Screen screen, CallbackInfo ci) {
      if (screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen) {
         GeneralGameFeatures.lastOpenedCustomCreativeTab = -1;
      } else if (screen instanceof CreativeColourScreen) {
         GeneralGameFeatures.lastOpenedCustomCreativeTab = 0;
      } else if (screen instanceof CreativeGradientScreen) {
         GeneralGameFeatures.lastOpenedCustomCreativeTab = 1;
      }
   }

   @WrapOperation(
      method = {"handleKeybinds"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"
      )},
      require = 0
   )
   public void handleKeybinds_setScreen(Minecraft instance, Screen screen, Operation<Void> original) {
      if (GeneralGameFeatures.lastOpenedCustomCreativeTab >= 0 && instance.player != null) {
         CreativeModeInventoryScreen creativeModeInventoryScreen = null;
         if (screen instanceof InventoryScreen) {
            creativeModeInventoryScreen = new CreativeModeInventoryScreen(
               instance.player, instance.player.connection.enabledFeatures(), (Boolean)instance.options.operatorItemsTab().get()
            );
         } else if (screen instanceof CreativeModeInventoryScreen creativeScreen) {
            creativeModeInventoryScreen = creativeScreen;
         }

         if (creativeModeInventoryScreen != null) {
            if (GeneralGameFeatures.lastOpenedCustomCreativeTab == 0) {
               original.call(new Object[]{instance, new CreativeColourScreen(instance.player, creativeModeInventoryScreen)});
               return;
            }

            if (GeneralGameFeatures.lastOpenedCustomCreativeTab == 1) {
               original.call(new Object[]{instance, new CreativeGradientScreen(instance.player, creativeModeInventoryScreen)});
               return;
            }
         }
      }

      original.call(new Object[]{instance, screen});
   }

   @WrapWithCondition(
      method = {"handleKeybinds"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
      )}
   )
   public boolean handleKeybindsSendPacket(ClientPacketListener instance, Packet<?> packet) {
      return packet instanceof ServerboundPlayerActionPacket serverboundPlayerActionPacket
            && serverboundPlayerActionPacket.getAction() == Action.SWAP_ITEM_WITH_OFFHAND
         ? !BuilderToolManager.isToolSlotActive()
         : true;
   }

   @Inject(
      method = {"handleKeybinds"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void handleKeyboards(CallbackInfo ci) {
      if (EditorUI.isActive()) {
         if (this.options.keyChat.consumeClick()) {
            this.openChatScreen("");
         }

         if (this.options.keyCommand.consumeClick()) {
            this.openChatScreen("/");
         }

         while (this.options.keyTogglePerspective.consumeClick()) {
         }

         while (this.options.keySmoothCamera.consumeClick()) {
         }

         for (int i = 0; i < 9; i++) {
            while (this.options.keyHotbarSlots[i].consumeClick()) {
            }
         }

         while (this.options.keySocialInteractions.consumeClick()) {
         }

         while (this.options.keyInventory.consumeClick()) {
         }

         while (this.options.keyAdvancements.consumeClick()) {
         }

         while (this.options.keySwapOffhand.consumeClick()) {
         }

         while (this.options.keyDrop.consumeClick()) {
         }

         while (this.options.keyChat.consumeClick()) {
         }

         while (this.options.keyCommand.consumeClick()) {
         }

         while (this.options.keyAttack.consumeClick()) {
         }

         while (this.options.keyUse.consumeClick()) {
         }

         while (this.options.keyPickItem.consumeClick()) {
         }

         ci.cancel();
      } else if (BuilderToolManager.isToolSlotActive()) {
         while (this.options.keySwapOffhand.consumeClick()) {
         }

         while (this.options.keyDrop.consumeClick()) {
         }
      }
   }

   @Override
   public void axiom$addCustomNbtData(ItemStack itemStack, BlockEntity blockEntity, RegistryAccess registryAccess) {
      this.addCustomNbtData(itemStack, blockEntity, registryAccess);
   }
}
