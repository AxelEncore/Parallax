package com.moulberry.axiom.downgrade;

import java.util.Map;

public enum DowngradeVersion {
   v1_7("1.7", 90, 90, Map.of()),
   v1_8("1.8", 100, 100, Map.of(v1_7, "1.8_to_1.7")),
   v1_9("1.9", 184, 184, Map.of(v1_8, "1.9_to_1.8")),
   v1_10("1.10", 512, 512, Map.of(v1_9, "1.10_to_1.9")),
   v1_11("1.11", 922, 922, Map.of(v1_10, "1.11_to_1.10")),
   v1_12("1.12", 1139, 1139, Map.of(v1_11, "1.12_to_1.11")),
   v1_13_2("1.13.2", 1631, 1631, Map.of(v1_12, "1.13.2_to_1.12")),
   v1_14_4("1.14.4", 1976, 1976, Map.of(v1_13_2, "1.14.4_to_1.13.2")),
   v1_15_2("1.15.2", 2230, 2230, Map.of(v1_14_4, "1.15.2_to_1.14.4")),
   v1_16_5("1.16.5", 2586, 2586, Map.of(v1_15_2, "1.16.5_to_1.15.2")),
   v1_17("1.17", 2724, 2724, Map.of(v1_16_5, "1.17_to_1.16.5")),
   v1_18("1.18", 2860, 2860, Map.of(v1_17, "1.18_to_1.17")),
   v1_19("1.19", 3105, 3105, Map.of(v1_18, "1.19_to_1.18")),
   v1_19_3("1.19.3", 3218, 3218, Map.of(v1_19, "1.19.3_to_1.19")),
   v1_19_4("1.19.4", 3337, 3337, Map.of(v1_19_3, "1.19.4_to_1.19.3")),
   v1_20("1.20", 3463, 3465, Map.of(v1_19_4, "1.20_to_1.19.4")),
   v1_20_2("1.20.2", 3578, 3578, Map.of(v1_20, "1.20.2_to_1.20")),
   v1_20_4("1.20.4", 3698, 3700, Map.of(v1_20_2, "1.20.4_to_1.20.2")),
   v1_20_6("1.20.6", 3837, 3839, Map.of(v1_20_4, "1.20.6_to_1.20.4")),
   v1_21("1.21", 3953, 3955, Map.of(v1_20_6, "1.21_to_1.20.6")),
   v1_21_3("1.21.3", 4080, 4082, Map.of(v1_21, "1.21.3_to_1.21")),
   v1_21_4("1.21.4", 4189, 4189, Map.of(v1_21_3, "1.21.4_to_1.21.3")),
   v1_21_5("1.21.5", 4325, 4325, Map.of(v1_21_4, "1.21.5_to_1.21.4")),
   v1_21_8("1.21.8", 4435, 4440, Map.of(v1_21_5, "1.21.8_to_1.21.5")),
   v1_21_11("1.21.11", 4554, 4671, Map.of(v1_21_5, "1.21.8_to_1.21.5"));

   private final String versionString;
   private final int minDataVersion;
   private final int maxDataVersion;
   private final Map<DowngradeVersion, String> downgradeMap;

   private DowngradeVersion(String versionString, int minDataVersion, int maxDataVersion, Map<DowngradeVersion, String> downgradeMap) {
      this.versionString = versionString;
      this.minDataVersion = minDataVersion;
      this.maxDataVersion = maxDataVersion;
      this.downgradeMap = downgradeMap;
   }

   public String getVersionString() {
      return this.versionString;
   }

   public int getMinDataVersion() {
      return this.minDataVersion;
   }

   public int getMaxDataVersion() {
      return this.maxDataVersion;
   }

   public Map<DowngradeVersion, String> getDowngradeMap() {
      return this.downgradeMap;
   }
}
