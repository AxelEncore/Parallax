package com.moulberry.axiom.downgrade;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.utils.DFUHelper;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class Downgrader {
   private final List<JsonObject> downgradeMaps = new ArrayList<>();

   public Downgrader(DowngradeVersion targetVersion) {
      int currentDataVersion = DFUHelper.DATA_VERSION;
      DowngradeVersion currentVersion = null;

      for (DowngradeVersion value : DowngradeVersion.values()) {
         if (value.getMinDataVersion() <= currentDataVersion && currentDataVersion <= value.getMaxDataVersion()) {
            currentVersion = value;
            break;
         }
      }

      if (currentVersion == null) {
         throw new FaultyImplementationError("Unable to find current version");
      } else {
         Gson gson = new Gson();

         while (currentVersion != targetVersion) {
            DowngradeVersion to = null;

            for (Entry<DowngradeVersion, String> entry : currentVersion.getDowngradeMap().entrySet()) {
               if (entry.getKey().getMaxDataVersion() >= targetVersion.getMaxDataVersion()
                  && (to == null || to.getMaxDataVersion() > entry.getKey().getMaxDataVersion())) {
                  to = entry.getKey();
               }
            }

            if (to == null) {
               throw new FaultyImplementationError("Unable to downgrade " + currentVersion.getVersionString() + " to " + targetVersion.getVersionString());
            }

            String downgradeFile = currentVersion.getDowngradeMap().get(to);

            try {
               URL url = Downgrader.class.getClassLoader().getResource("downgrade/" + downgradeFile + ".json");
               URLConnection connection = url.openConnection();
               connection.setUseCaches(false);

               try (InputStream is = connection.getInputStream()) {
                  byte[] bytes = is.readAllBytes();
                  this.downgradeMaps.add((JsonObject)gson.fromJson(new String(bytes), JsonObject.class));
               }
            } catch (Exception var14) {
               throw new RuntimeException(var14);
            }

            currentVersion = to;
         }
      }
   }

   public String downgrade(String state) {
      String block = state.split("\\[")[0];
      boolean bestEffort = false;
      Iterator var4 = this.downgradeMaps.iterator();

      while (true) {
         if (!var4.hasNext()) {
            if (bestEffort) {
               return "?" + state;
            }

            return state;
         }

         JsonObject downgradeMap = (JsonObject)var4.next();
         JsonElement downgradeOperation = downgradeMap.get(state);
         if (downgradeOperation == null && !state.equals(block)) {
            downgradeOperation = downgradeMap.get(block);
         }

         if (downgradeOperation == null) {
            return "";
         }

         if (!downgradeOperation.isJsonPrimitive()) {
            break;
         }

         JsonPrimitive primitive = downgradeOperation.getAsJsonPrimitive();
         if (primitive.isBoolean()) {
            if (!primitive.getAsBoolean()) {
               return null;
            }
         } else {
            if (!primitive.isString()) {
               break;
            }

            state = primitive.getAsString();
            if (state.startsWith("?")) {
               bestEffort = true;
               state = state.substring(1);
            }

            block = state.split("\\[")[0];
         }
      }

      throw new RuntimeException();
   }
}
