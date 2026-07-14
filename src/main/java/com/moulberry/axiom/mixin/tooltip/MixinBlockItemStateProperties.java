package com.moulberry.axiom.mixin.tooltip;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.hooks.BlockItemStatePropertiesExt;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
   targets = {"net/minecraft/world/item/component/BlockItemStateProperties"}
)
public abstract class MixinBlockItemStateProperties implements BlockItemStatePropertiesExt {
   @Unique
   private List<Component> cachedTooltipLines = null;

   @Shadow
   public abstract Map<String, String> properties();

   @Override
   public void axiom$addToTooltip(Consumer<Component> consumer, Set<String> ignore) {
      if (Axiom.configuration.visuals.showBlockStateTooltip) {
         if (this.cachedTooltipLines == null) {
            this.cachedTooltipLines = new ArrayList<>();
            ArrayList<Entry<String, String>> properties = new ArrayList<>(this.properties().entrySet());
            properties.sort(Entry.comparingByKey());

            for (Entry<String, String> entry : properties) {
               if (!ignore.contains(entry.getKey())) {
                  String value = entry.getValue();
                  if (!value.isBlank()) {
                     MutableComponent component = Component.literal(entry.getKey() + ": ").withStyle(ChatFormatting.GRAY);
                     boolean isNumber = true;
                     int length = value.length();

                     for (int i = 0; i < length; i++) {
                        char c = value.charAt(i);
                        if (c < '0' || c > '9') {
                           isNumber = false;
                           break;
                        }
                     }

                     if (isNumber) {
                        component = component.append(Component.literal(value).withStyle(ChatFormatting.BLUE));
                        this.cachedTooltipLines.add(component);
                     } else {
                        String lower = value.toLowerCase(Locale.ROOT);
                        if (lower.equals("true")) {
                           component = component.append(Component.literal(value).withStyle(ChatFormatting.GREEN));
                           this.cachedTooltipLines.add(component);
                        } else if (lower.equals("false")) {
                           component = component.append(Component.literal(value).withStyle(ChatFormatting.RED));
                           this.cachedTooltipLines.add(component);
                        } else {
                           component = component.append(Component.literal(value).withStyle(ChatFormatting.AQUA));
                           this.cachedTooltipLines.add(component);
                        }
                     }
                  }
               }
            }
         }

         this.cachedTooltipLines.forEach(consumer);
      }
   }
}
