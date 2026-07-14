package com.moulberry.axiom.i18n;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.StaticValues;
import com.moulberry.axiom.platform.AxiomPlatform;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Bidi;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

public class LocalizationLoader {
   private static final Gson GSON = new GsonBuilder().create();
   private static boolean attemptedLocalizationDownload = false;
   private static boolean appliedCachedTranslations = false;
   private static final Set<String> languages = Set.of(
      "af_za",
      "ar_sa",
      "ast_es",
      "az_az",
      "ba_ru",
      "bar",
      "be_by",
      "bg_bg",
      "br_fr",
      "brb",
      "bs_ba",
      "ca_es",
      "cs_cz",
      "cy_gb",
      "da_dk",
      "de_at",
      "de_ch",
      "de_de",
      "el_gr",
      "en_au",
      "en_ca",
      "en_gb",
      "en_nz",
      "en_pt",
      "en_ud",
      "en_us",
      "enp",
      "enws",
      "eo_uy",
      "es_ar",
      "es_cl",
      "es_ec",
      "es_es",
      "es_mx",
      "es_uy",
      "es_ve",
      "esan",
      "et_ee",
      "eu_es",
      "fa_ir",
      "fi_fi",
      "fil_ph",
      "fo_fo",
      "fr_ca",
      "fr_fr",
      "fra_de",
      "fur_it",
      "fy_nl",
      "ga_ie",
      "gd_gb",
      "gl_es",
      "haw_us",
      "he_il",
      "hi_in",
      "hr_hr",
      "hu_hu",
      "hy_am",
      "id_id",
      "ig_ng",
      "io_en",
      "is_is",
      "isv",
      "it_it",
      "ja_jp",
      "jbo_en",
      "ka_ge",
      "kk_kz",
      "kn_in",
      "ko_kr",
      "ksh",
      "kw_gb",
      "la_la",
      "lb_lu",
      "li_li",
      "lmo",
      "lo_la",
      "lol_us",
      "lt_lt",
      "lv_lv",
      "lzh",
      "mk_mk",
      "mn_mn",
      "ms_my",
      "mt_mt",
      "nah",
      "nds_de",
      "nl_be",
      "nl_nl",
      "nn_no",
      "no_no",
      "oc_fr",
      "ovd",
      "pl_pl",
      "pt_br",
      "pt_pt",
      "qya_aa",
      "ro_ro",
      "rpr",
      "ru_ru",
      "ry_ua",
      "sah_sah",
      "se_no",
      "sk_sk",
      "sl_si",
      "so_so",
      "sq_al",
      "sr_cs",
      "sr_sp",
      "sv_se",
      "sxu",
      "szl",
      "ta_in",
      "th_th",
      "tl_ph",
      "tlh_aa",
      "tok",
      "tr_tr",
      "tt_ru",
      "uk_ua",
      "val_es",
      "vec_it",
      "vi_vn",
      "yi_de",
      "yo_ng",
      "zh_cn",
      "zh_hk",
      "zh_tw",
      "zlm_arab"
   );

   public static synchronized void applyCachedTranslations() {
      try {
         String languageCode = Minecraft.getInstance().options.languageCode;
         if (!languageCode.startsWith("en")) {
            Path path = AxiomPlatform.configDir().resolve("axiom").resolve("translations.zip");
            if (Files.exists(path)) {
               Axiom.dbg("Using cached localizations...");
               byte[] cachedBytes = Files.readAllBytes(path);
               Map<String, byte[]> localizations = createLocalizations(cachedBytes);
               LocalizationSource.INSTANCE.setResources(new LoadedPackResources("axiom_translations", Component.literal("Parallax Translations"), localizations));
               appliedCachedTranslations = true;
            }
         }
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   public static synchronized void languageChanged(String languageCode) {
      if (!attemptedLocalizationDownload) {
         if (!languageCode.startsWith("en")) {
            attemptedLocalizationDownload = true;
            ForkJoinPool.commonPool()
               .submit(
                  () -> {
                     try {
                        Path path = AxiomPlatform.configDir().resolve("axiom").resolve("translations.zip");
                        boolean wantDownload = true;
                        boolean canDownload = true;
                        boolean canUseCached = true;
                        if (!Files.exists(path)) {
                           canUseCached = false;
                        }

                        int remoteCount = 0;

                        try {
                           remoteCount = fetchUpdateCount();
                           Axiom.dbg("Localization update count: remote=" + remoteCount + ", last=" + Axiom.configuration.internal.lastTranslationCount);
                           if (Axiom.configuration.internal.lastTranslationCount == remoteCount) {
                              wantDownload = false;
                           }
                        } catch (Exception var11) {
                           canDownload = false;
                           wantDownload = false;
                        }

                        if (canUseCached && !wantDownload) {
                           if (appliedCachedTranslations) {
                              return;
                           }

                           Axiom.dbg("Using cached localizations...");
                           byte[] cachedBytes = Files.readAllBytes(path);

                           try {
                              Map<String, byte[]> localizations = createLocalizations(cachedBytes);
                              LocalizationSource.INSTANCE
                                 .setResources(new LoadedPackResources("axiom_translations", Component.literal("Parallax Translations"), localizations));
                              StaticValues.shouldReloadResourcesForLanguage = true;
                              appliedCachedTranslations = true;
                              return;
                           } catch (Exception var13) {
                              var13.printStackTrace();
                           }
                        }

                        if (canDownload) {
                           Axiom.dbg("Downloading localizations...");

                           try {
                              URL url = new URL("https://axiom.moulberry.com/weblate/api/components/axiom/mod/file/");
                              HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                              conn.setConnectTimeout(5000);
                              conn.setRequestMethod("GET");
                              conn.setRequestProperty("Authorization", "Token wlp_tWKISyESQ0OAAmYj7MqvJEjDZAZvHWtju1WW");
                              conn.setRequestProperty("Accept", "*/*");
                              InputStream inputStream = conn.getInputStream();
                              byte[] allBytes = inputStream.readAllBytes();
                              Files.write(path, allBytes);
                              Axiom.configuration.internal.lastTranslationCount = remoteCount;
                              Map<String, byte[]> localizations = createLocalizations(allBytes);
                              LocalizationSource.INSTANCE
                                 .setResources(new LoadedPackResources("axiom_translations", Component.literal("Parallax Translations"), localizations));
                              StaticValues.shouldReloadResourcesForLanguage = true;
                              return;
                           } catch (Exception var12) {
                              var12.printStackTrace();
                           }
                        }

                        if (canUseCached) {
                           if (appliedCachedTranslations) {
                              return;
                           }

                           Axiom.dbg("Using cached localizations after download failed...");
                           byte[] cachedBytes = Files.readAllBytes(path);

                           try {
                              Map<String, byte[]> localizations = createLocalizations(cachedBytes);
                              LocalizationSource.INSTANCE
                                 .setResources(new LoadedPackResources("axiom_translations", Component.literal("Parallax Translations"), localizations));
                              StaticValues.shouldReloadResourcesForLanguage = true;
                              appliedCachedTranslations = true;
                           } catch (Exception var10) {
                           }
                        }
                     } catch (Exception var14) {
                        var14.printStackTrace();
                     }
                  }
               );
         }
      }
   }

   private static int fetchUpdateCount() throws IOException {
      URL url = new URL("https://axiom.moulberry.com/weblate/api/components/axiom/mod/changes/");
      HttpURLConnection conn = (HttpURLConnection)url.openConnection();
      conn.setConnectTimeout(5000);
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", "Token wlp_tWKISyESQ0OAAmYj7MqvJEjDZAZvHWtju1WW");
      conn.setRequestProperty("Accept", "application/json");
      InputStream inputStream = conn.getInputStream();
      String input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      conn.disconnect();
      JsonObject jsonObject = (JsonObject)GSON.fromJson(input, JsonObject.class);
      return jsonObject.get("count").getAsInt();
   }

   private static Map<String, byte[]> createLocalizations(byte[] translationsZip) throws IOException {
      ZipFile zipFile = new ZipFile(new SeekableInMemoryByteChannel(translationsZip));
      Map<String, byte[]> data = new HashMap<>();
      Enumeration<ZipArchiveEntry> enumeration = zipFile.getEntries();

      while (enumeration.hasMoreElements()) {
         ZipArchiveEntry entry = enumeration.nextElement();
         String name = entry.getName();
         String[] split = name.split("/");
         String lang = split[split.length - 1].replace(".json", "").toLowerCase(Locale.ROOT);

         lang = switch (lang) {
            case "ru" -> "ru_ru";
            case "cs" -> "cs_cz";
            case "de" -> "de_de";
            case "en" -> "en_us";
            case "es" -> "es_es";
            case "fr" -> "fr_fr";
            case "ja" -> "ja_jp";
            case "nl" -> "nl_nl";
            case "uk" -> "uk_ua";
            case "tr" -> "tr_tr";
            case "zh_hans" -> "zh_cn";
            case "zh_hant" -> "zh_tw";
            case "pl" -> "pl_pl";
            case "da" -> "da_dk";
            case "sv" -> "sv_se";
            case "af" -> "af_za";
            case "ko" -> "ko_kr";
            default -> lang;
         };
         if (!languages.contains(lang)) {
            if (languages.contains(lang + "_" + lang)) {
               lang = lang + "_" + lang;
            } else {
               for (String language : languages) {
                  if (language.startsWith(lang + "_")) {
                     lang = language;
                     break;
                  }
               }
            }
         }

         if (!languages.contains(lang)) {
            Axiom.LOGGER.error("Unknown language, need alias: {}", lang);
         } else {
            byte[] bytes = zipFile.getInputStream(entry).readAllBytes();
            if (lang.startsWith("he")) {
               JsonObject object = (JsonObject)GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
               JsonObject newObject = new JsonObject();

               for (Entry<String, JsonElement> jsonEntry : object.entrySet()) {
                  String value = convertBidi(jsonEntry.getValue().getAsString());
                  value = value.replace("s%", "%s");
                  value = value.replace("s$1%", "%1$s");
                  value = value.replace("s$2%", "%2$s");
                  value = value.replace("s$3%", "%3$s");
                  newObject.addProperty(jsonEntry.getKey(), value);
               }

               bytes = GSON.toJson(newObject).getBytes(StandardCharsets.UTF_8);
            }

            data.put("lang/" + lang + ".json", bytes);
            if (lang.equals("zh_tw")) {
               data.put("lang/zh_hk.json", bytes);
            }
         }
      }

      for (String languageKey : languages) {
         if (!data.containsKey("lang/" + languageKey + ".json")) {
            String code = languageKey.split("_")[0];

            for (Entry<String, byte[]> entry : data.entrySet()) {
               if (entry.getKey().startsWith("lang/" + code + "_")) {
                  data.put("lang/" + languageKey + ".json", entry.getValue());
                  break;
               }
            }
         }
      }

      return data;
   }

   private static String convertBidi(String input) {
      Bidi bidi = new Bidi(input, -1);
      int runCount = bidi.getRunCount();
      StringBuilder builder = new StringBuilder();
      if (bidi.baseIsLeftToRight()) {
         for (int i = 0; i < runCount; i++) {
            String subString = input.substring(bidi.getRunStart(i), bidi.getRunLimit(i));
            if (bidi.getRunLevel(i) % 2 != 0) {
               for (int j = 0; j < subString.length(); j++) {
                  char c = subString.charAt(j);
                  if (c == '(') {
                     builder.insert(0, ')');
                  } else if (c == ')') {
                     builder.insert(0, '(');
                  } else {
                     builder.insert(0, c);
                  }
               }
            } else {
               builder.append(subString);
            }
         }
      } else {
         for (int ix = 0; ix < runCount; ix++) {
            String subString = input.substring(bidi.getRunStart(ix), bidi.getRunLimit(ix));
            if (bidi.getRunLevel(ix) % 2 != 0) {
               for (int jx = 0; jx < subString.length(); jx++) {
                  char c = subString.charAt(jx);
                  if (c == '(') {
                     builder.insert(0, ')');
                  } else if (c == ')') {
                     builder.insert(0, '(');
                  } else {
                     builder.insert(0, c);
                  }
               }
            } else {
               builder.insert(0, subString);
            }
         }
      }

      return builder.toString();
   }
}
