package com.moulberry.axiom.mixin;

import com.moulberry.axiom.StaticValues;
import com.moulberry.axiom.displayentity.ItemList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.i18n.LocalizationLoader;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LanguageManager.class})
public class MixinLanguageManager {
   @Shadow
   private String currentCode;

   @Inject(
      method = {"onResourceManagerReload"},
      at = {@At("RETURN")}
   )
   public void onReload(ResourceManager resourceManager, CallbackInfo ci) {
      StaticValues.shouldReloadResourcesForLanguage = false;
      AxiomI18n.setLanguage(Language.getInstance());
      LocalizationLoader.languageChanged(this.currentCode);
      EditorUI.initFonts(this.currentCode);
      EditorUI.getBlockList().markNeedsReload();
      ItemList.INSTANCE.markDirty();
   }
}
