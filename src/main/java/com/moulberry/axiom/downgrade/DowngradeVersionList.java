package com.moulberry.axiom.downgrade;

import com.moulberry.axiom.utils.DFUHelper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class DowngradeVersionList {
   private static List<DowngradeVersion> versions = new ArrayList<>();
   private static String[] versionStrings = null;

   public static List<DowngradeVersion> getVersions() {
      return versions;
   }

   @Nullable
   public static String[] getVersionStrings() {
      return versionStrings;
   }

   static {
      int currentDataVersion = DFUHelper.DATA_VERSION;
      DowngradeVersion currentVersion = null;

      for (DowngradeVersion value : DowngradeVersion.values()) {
         if (value.getMinDataVersion() <= currentDataVersion && currentDataVersion <= value.getMaxDataVersion()) {
            currentVersion = value;
            break;
         }
      }

      if (currentVersion != null) {
         Set<DowngradeVersion> allVersions = new HashSet<>();
         Set<DowngradeVersion> newVersions = new HashSet<>();
         Set<DowngradeVersion> iterVersions = new HashSet<>();
         allVersions.add(currentVersion);
         newVersions.add(currentVersion);

         while (!newVersions.isEmpty()) {
            iterVersions.clear();
            iterVersions.addAll(newVersions);
            newVersions.clear();

            for (DowngradeVersion iterVersion : iterVersions) {
               for (DowngradeVersion v : iterVersion.getDowngradeMap().keySet()) {
                  if (allVersions.add(v)) {
                     newVersions.add(v);
                  }
               }
            }
         }

         versions.addAll(allVersions);
         versions.sort(Comparator.comparingInt(vx -> -vx.getMinDataVersion()));
         versionStrings = new String[versions.size()];

         for (int i = 0; i < versions.size(); i++) {
            versionStrings[i] = versions.get(i).getVersionString();
         }
      }
   }
}
