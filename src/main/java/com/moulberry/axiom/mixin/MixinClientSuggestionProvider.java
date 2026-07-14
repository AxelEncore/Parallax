package com.moulberry.axiom.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.marker.MarkerData;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ClientSuggestionProvider.class})
public class MixinClientSuggestionProvider {
   @ModifyReturnValue(
      method = {"getSelectedEntities"},
      at = {@At("RETURN")}
   )
   public Collection<String> getSelectedEntities(Collection<String> returnValue) {
      Display activeDisplayEntity = DisplayEntityManipulator.getActiveDisplayEntity();
      if (activeDisplayEntity != null) {
         Collection<String> newCollection = new ArrayList<>(returnValue);
         newCollection.add(activeDisplayEntity.getStringUUID());
         return newCollection;
      } else {
         MarkerData activeMarker = MarkerEntityManipulator.getActiveMarkerData();
         if (activeMarker != null) {
            Collection<String> newCollection = new ArrayList<>(returnValue);
            newCollection.add(activeMarker.uuid().toString());
            return newCollection;
         } else {
            return returnValue;
         }
      }
   }
}
