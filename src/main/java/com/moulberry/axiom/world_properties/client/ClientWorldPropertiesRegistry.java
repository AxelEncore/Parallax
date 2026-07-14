package com.moulberry.axiom.world_properties.client;

import com.moulberry.axiom.world_properties.WorldPropertyCategory;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public class ClientWorldPropertiesRegistry {
   public static LinkedHashMap<WorldPropertyCategory, List<ClientWorldProperty<?>>> PROPERTY_LIST = new LinkedHashMap<>();
   public static final Map<ResourceLocation, ClientWorldProperty<?>> PROPERTY_MAP = new HashMap<>();

   public static void clear() {
      PROPERTY_MAP.clear();
      PROPERTY_LIST.clear();
   }

   public static void loadAll(LinkedHashMap<WorldPropertyCategory, List<ClientWorldProperty<?>>> properties) {
      PROPERTY_LIST = properties;
      PROPERTY_MAP.clear();

      for (List<ClientWorldProperty<?>> propertiesList : PROPERTY_LIST.values()) {
         for (ClientWorldProperty<?> property : propertiesList) {
            ResourceLocation id = property.getId();
            if (PROPERTY_MAP.containsKey(id)) {
               throw new RuntimeException("Duplicate property: " + id);
            }

            PROPERTY_MAP.put(id, property);
         }
      }
   }
}
