package com.moulberry.axiom.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

/**
 * Client-side tag resolution independent of server datapacks, reimplementing Fabric's
 * {@code ClientTags.getOrCreateLocalTag}. NeoForge has no equivalent, so this reads the tag JSON
 * bundled in the mod's own {@code data/<ns>/tags/<registry>/<path>.json} resources and resolves
 * entries recursively (including nested {@code #tag} references).
 */
public final class ClientTagsHelper {

    private ClientTagsHelper() {
    }

    public static Set<ResourceLocation> getOrCreateLocalTag(TagKey<?> tagKey) {
        Set<ResourceLocation> out = new HashSet<>();
        String registryDir = tagKey.registry().location().getPath();
        resolve(registryDir, tagKey.location(), out, new HashSet<>());
        return out;
    }

    private static void resolve(String registryDir, ResourceLocation tag, Set<ResourceLocation> out, Set<ResourceLocation> seen) {
        if (!seen.add(tag)) {
            return;
        }
        String path = "/data/" + tag.getNamespace() + "/tags/" + registryDir + "/" + tag.getPath() + ".json";
        try (InputStream is = ClientTagsHelper.class.getResourceAsStream(path)) {
            if (is == null) {
                return;
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray values = json.getAsJsonArray("values");
            if (values == null) {
                return;
            }
            for (JsonElement element : values) {
                String id = element.isJsonObject() ? element.getAsJsonObject().get("id").getAsString() : element.getAsString();
                if (id.startsWith("#")) {
                    resolve(registryDir, ResourceLocation.parse(id.substring(1)), out, seen);
                } else {
                    out.add(ResourceLocation.parse(id));
                }
            }
        } catch (Exception ignored) {
        }
    }
}
