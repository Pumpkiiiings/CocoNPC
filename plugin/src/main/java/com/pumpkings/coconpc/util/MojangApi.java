package com.pumpkings.coconpc.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class MojangApi {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson GSON = new Gson();
    private final Map<String, String> uuidCache = new HashMap<>();

    public String fetchMojangSkinUrl(String playerName) {
        String uuid = resolvePlayerUuid(playerName);
        if (uuid == null) {
            return null;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            JsonObject profile = GSON.fromJson(response.body(), JsonObject.class);
            JsonArray properties = profile.getAsJsonArray("properties");
            if (properties == null) {
                return null;
            }

            for (int i = 0; i < properties.size(); i++) {
                JsonObject prop = properties.get(i).getAsJsonObject();
                if ("textures".equals(prop.get("name").getAsString())) {
                    String base64Value = prop.get("value").getAsString();
                    String jsonTextures = new String(Base64.getDecoder().decode(base64Value));
                    JsonObject texObj = GSON.fromJson(jsonTextures, JsonObject.class);
                    return texObj.getAsJsonObject("textures")
                            .getAsJsonObject("SKIN")
                            .get("url").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        
        return null;
    }

    private String resolvePlayerUuid(String playerName) {
        if (uuidCache.containsKey(playerName)) {
            return uuidCache.get(playerName);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonObject obj = GSON.fromJson(response.body(), JsonObject.class);
                if (obj != null && obj.has("id")) {
                    String uuid = obj.get("id").getAsString();
                    uuidCache.put(playerName, uuid);
                    return uuid;
                }
            }
        } catch (Exception ignored) {
        }
        
        uuidCache.put(playerName, null);
        return null;
    }
}
