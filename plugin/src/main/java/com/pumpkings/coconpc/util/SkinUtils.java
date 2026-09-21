package com.pumpkings.coconpc.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

public class SkinUtils {

    public static String urlToBase64(String url) {
        byte[] urlBytes = url.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(urlBytes);
    }

    public static String base64ToUrl(String base64) {
        byte[] urlBytes = Base64.getDecoder().decode(base64);
        return new String(urlBytes, StandardCharsets.UTF_8);
    }

    public static ItemStack getHeadUrl(String texture) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "");
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        PlayerTextures textures = profile.getTextures();

        try {
            URL url = new URL(texture);
            textures.setSkin(url);
            profile.setTextures(textures);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack getHead(String value, Logger logger) {
        String url = extractSkinUrl(value);
        if (url == null) {
            if (logger != null) logger.warning("Could not read a skin URL out of the supplied texture value.");
            return new ItemStack(Material.PLAYER_HEAD);
        }
        return getHeadUrl(url);
    }

    public static String extractSkinUrl(String base64Value) {
        try {
            String json = new String(Base64.getDecoder().decode(base64Value), StandardCharsets.UTF_8);
            JsonObject root = new Gson().fromJson(json, JsonObject.class);
            return root.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url")
                    .getAsString();
        } catch (Exception ex) {
            return null;
        }
    }
}
