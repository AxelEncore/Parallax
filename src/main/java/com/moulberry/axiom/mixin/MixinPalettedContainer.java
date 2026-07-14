package com.moulberry.axiom.mixin;

import com.moulberry.axiom.hooks.PalettedContainerExt;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PalettedContainer.class})
public class MixinPalettedContainer implements PalettedContainerExt {
   @Unique
   private final ReentrantLock lock = new ReentrantLock();
   @Unique
   private volatile boolean locked = false;
   @Unique
   private volatile int acquired = 0;

   @Override
   public void axiom$lock() {
      this.locked = true;
      this.lock.lock();
      int acquired = this.acquired;

      while (acquired > 0) {
         acquired--;
         LockSupport.parkNanos("waiting for container to not be acquired", 100000L);
      }

      this.acquired = 0;
   }

   @Override
   public void axiom$unlock() {
      this.lock.unlock();
      this.locked = false;
   }

   @Inject(
      method = {"acquire"},
      at = {@At("HEAD")}
   )
   public void acquire(CallbackInfo ci) {
      if (this.locked) {
         this.acquired = 0;
         this.lock.lock();
         this.acquired = 10000;
         this.lock.unlock();
      } else {
         this.acquired = 10000;
      }
   }

   @Inject(
      method = {"release"},
      at = {@At("RETURN")}
   )
   public void release(CallbackInfo ci) {
      this.acquired = 0;
   }
}
