package com.moulberry.axiom.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.moulberry.axiom.platform.AxiomPlatform;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * Commercial-license verification, ported verbatim from the original mod.
 *
 * <p>This is the genuine licensing check: it contacts {@code axiom.moulberry.com} and verifies a
 * signed JWT against Moulberry's public key, caching it under {@code <config>/axiom/.license}. It
 * grants commercial features only to accounts that actually own a commercial license — it is not a
 * bypass. The original hid this class behind a homoglyph package name for anti-tamper reasons; the
 * port keeps it in the normal {@code com.moulberry.axiom.utils} package.
 */
public class Authorization {

    private static final String PUBKEY =
            "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAvvwXcWKKx5ee1lrpIsVF"
            + "5lRNBpB+0Krlc8HNcahvTRu7IOF9mz68ATSqf94lJbvU0jVPg1I4bvAAuniDTRI2"
            + "28WeoQpNPM6iSdcIc7hLbThDX9iWYqne+YBve0NwGkFpxHZTrwFSRmIixg/0nTQZ"
            + "S00xhCGynVJgOMbMVUaD4v/5Tsj9s0KxSss2mZLMpMCUEMrB3P6AGQ8abr6hPJXM"
            + "k8v5+BbfTljpv8GX6OQIWKmktNMWca0Zj69Ixf1llog9bgTxn4LyCR1zWnGYxaj3"
            + "qe8N/MY9zKNamV6eL+JNTnlXSauLnwBjENGG6nZuPzRTPbw/VhH+gBvgZFhiOUUu"
            + "dfkOhDltnP7viJKAM3mtvU6GpntzeWuoji2FnsXKmj7qkCi8lWEl6Ah5l0YVxm75"
            + "C+7Uz4gndd4sJGpuzH8w798ifWiknaIfgP5qvwmUdcIrotZc67WXE1Apmgvn+oNO"
            + "mru3HZJg14oto0FYyVfVMzvNjKcn4P3vD6MkxlecqsMCVKlkZ5MaM7NeSv+q1q/f"
            + "s072mHPiXxc/cr7C/1ZdlGcEv44L8yQTJf1glS8fzKNNrQaFvonJPMb0VMO8rP+N"
            + "PrjTmUMlSjzDwDQAu3bzFUWnWwsL3T5CgNdnNcNpyU4XxtIUNbOcMD1FeI49breb"
            + "PnT3DkeYK/iYneS6JW2pqiECAwEAAQ==";

    private static boolean hasCommercialLicense;
    public static boolean hasServerCommercialLicense;

    public static String getUserAgent() {
        return "Axiom/" + AxiomPlatform.modVersion();
    }

    private static RSAPublicKey loadPublicKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(PUBKEY);
        X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(ks);
    }

    public static CompletableFuture<Meta> getMeta() {
        CompletableFuture<Meta> future = new CompletableFuture<>();
        ForkJoinPool.commonPool().submit(() -> {
            try {
                URL url = new URL("https://axiom.moulberry.com/api/mcauth/meta");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", getUserAgent());
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                InputStream inputStream = conn.getInputStream();
                String input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                conn.disconnect();
                Gson gson = new GsonBuilder().create();
                JsonObject jsonObject = gson.fromJson(input, JsonObject.class);
                String latestModVersion = null;
                if (jsonObject.get("latest_mod_version") instanceof JsonPrimitive jsonPrimitive) {
                    latestModVersion = jsonPrimitive.getAsString();
                }

                String modDisabled = null;
                if (jsonObject.get("mod_disabled") instanceof JsonPrimitive jsonPrimitive) {
                    modDisabled = jsonPrimitive.getAsString();
                }

                List<String> latestChangelog = null;
                if (jsonObject.get("latest_changelog") instanceof JsonArray jsonArray) {
                    latestChangelog = new ArrayList<>();
                    for (JsonElement jsonElement : jsonArray) {
                        latestChangelog.add(jsonElement.getAsString());
                    }
                }

                boolean test = jsonObject.has("test") && jsonObject.get("test").getAsBoolean();
                future.complete(new Meta(latestModVersion, latestChangelog, modDisabled, test));
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(null);
            }
        });
        return future;
    }

    public static CompletableFuture<Boolean> checkCommercial(UUID uuid) {
        RSAPublicKey publicKey;
        try {
            publicKey = loadPublicKey();
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }

        if (uuid == null) {
            return CompletableFuture.completedFuture(false);
        }

        Algorithm algorithm = Algorithm.RSA256(publicKey);
        JWTVerifier verifier = JWT.require(algorithm)
                .withSubject(uuid.toString())
                .acceptNotBefore(86400L)
                .acceptExpiresAt(86400L)
                .ignoreIssuedAt()
                .build();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ForkJoinPool.commonPool().submit(() -> {
            try {
                Path path = AxiomPlatform.axiomConfigDir().resolve(".license");
                HttpURLConnection conn = null;
                try {
                    String endpoint = "https://axiom.moulberry.com/api/mcauth/has_commercial_license?uuid=";
                    URL url = new URL(endpoint + uuid);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", getUserAgent());
                    conn.setConnectTimeout(5000);
                    conn.setRequestMethod("GET");
                    if (conn.getResponseCode() == 401) {
                        if (Files.exists(path)) {
                            Files.delete(path);
                        }
                        future.complete(false);
                        return;
                    }

                    if (conn.getResponseCode() != 200) {
                        future.complete(false);
                        return;
                    }

                    String jwt = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    try {
                        verifier.verify(jwt);
                        Files.writeString(path, jwt, StandardCharsets.UTF_8);
                        hasCommercialLicense = true;
                        future.complete(true);
                        return;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                }

                if (Files.exists(path)) {
                    try {
                        String jwt = Files.readString(path, StandardCharsets.UTF_8);
                        verifier.verify(jwt);
                        hasCommercialLicense = true;
                        future.complete(true);
                        return;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                future.complete(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        return future;
    }

    public static CompletableFuture<ServerAuthorization> checkServer(String server, String host, UUID uuid) {
        RSAPublicKey publicKey;
        try {
            publicKey = loadPublicKey();
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(ServerAuthorization.NO);
        }

        if (uuid == null) {
            return CompletableFuture.completedFuture(ServerAuthorization.NO);
        }

        Algorithm algorithm = Algorithm.RSA256(publicKey);
        JWTVerifier verifier = JWT.require(algorithm)
                .withSubject(uuid + "/" + server)
                .acceptNotBefore(86400L)
                .acceptExpiresAt(86400L)
                .ignoreIssuedAt()
                .build();
        CompletableFuture<ServerAuthorization> future = new CompletableFuture<>();
        ForkJoinPool.commonPool().submit(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL("https://axiom.moulberry.com/api/mcauth/connect?uuid=" + uuid + "&server=" + server + "&host=" + host);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", getUserAgent());
                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == 200) {
                    String jwt = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    DecodedJWT decoded = verifier.verify(jwt);
                    Claim commercialClaim = decoded.getClaim("commercial");
                    if (commercialClaim != null && Boolean.TRUE.equals(commercialClaim.asBoolean())) {
                        hasServerCommercialLicense = true;
                        future.complete(ServerAuthorization.COMMERCIAL);
                        return;
                    }
                    future.complete(ServerAuthorization.YES);
                    return;
                }
                future.complete(ServerAuthorization.NO);
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(ServerAuthorization.YES);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
        return future;
    }

    public static boolean hasCommercialLicense() {
        return hasCommercialLicense || hasServerCommercialLicense;
    }

    public record Meta(String latestModVersion, List<String> latestChangelog, String modDisabled, boolean test) {
    }

    public enum ServerAuthorization {
        YES,
        NO,
        COMMERCIAL
    }
}
